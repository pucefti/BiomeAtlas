package com.sk89q.biomeatlas;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.graphics2d.svg.*;

import java.awt.*;
import java.awt.geom.Area;
import java.io.File;
import java.util.*;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class BiomeMapper
{
	private static Logger logger = LogManager.getLogger("BiomeMapper");

	private final List<Predicate<String>> listeners = Lists.newArrayList();
	private int messageRate = 5000;
	private int resolution = 1;

	void setMessageRate(int messageRate)
	{
		checkArgument(messageRate >= 10, "messageRate >= 10");
		this.messageRate = messageRate;
	}

	public void setResolution(int resolution)
	{
		checkArgument(resolution >= 1, "resolution >= 1");
		this.resolution = resolution;
	}

	public List<Predicate<String>> getListeners()
	{
		return listeners;
	}

	public void generate(World world, int centerX, int centerZ, int apothem, File outputFile)
	{
		checkNotNull(outputFile, "outputFile");

		int minBlockX = centerX - apothem;
		int minBlockZ = centerZ - apothem;
		int maxBlockX = centerX + apothem;
		int maxBlockZ = centerZ + apothem;
		int worldLength = apothem * 2;
		int mapImageLength = (int) Math.ceil(worldLength / (double) resolution);

		SVGGraphics2D g2 = new SVGGraphics2D(mapImageLength, mapImageLength);

		Map<Biome, Area> mapBiomes = new TreeMap<Biome, Area>(new Comparator<Biome>()
		{
			public int compare(Biome biome1, Biome biome2)
			{
				if(biome1 == biome2)
				{
					return 0;
				}
				else if(biome1 == null)
				{
					return -1;
				}
				else if(biome2 == null)
				{
					return 1;
				}

				return biome1.getBiomeName().compareTo(biome2.getBiomeName());
			}
		});

		// Progress tracking
		int blockCount = mapImageLength * mapImageLength;
		int completedBlocks = 0;
		long lastMessageTime = System.currentTimeMillis();

		String header = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" +
				"<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"fr\" lang=\"fr\">\n" +
				"\t<head>\n" +
				"\t<title>" + world.getSeed() + "</title>\n" +
				"\t</head>\n" +
				"\t<style>" +
				"path:hover { fill-opacity: 0.7; stroke: black; stroke-opacity: 1.0; stroke-width: 1px; } " +
				"svg { border:3px ridge black; box-shadow: 4px 4px 0px gray; } " +
				"#map, #infobar { padding: 0; margin: 0; } " +
				"#main { display:table; margin:auto; } " +
				"#map  { display:table-cell; width:900px; background-color:#FFFFFF; } " +
				"#infobar { display:table-cell; vertical-align:top; padding-left: 10px; width:300px; background-color:#FFFFFF; } " +
				"#infomap { border:3px ridge black; box-shadow: 4px 4px 0px gray; padding: 5px; margin-bottom: 10px; } " +
				"#infobiome { border:3px ridge black; box-shadow: 4px 4px 0px gray; padding: 5px; } " +
				".biometext { padding: 2px 7px 2px 0; margin: 0; white-space: nowrap; } a { text-decoration: none; color: #000000; } a:hover { color: #FF0000; }" +
				"</style>\n" +
				"\t<script>" +
				"var colorOld; " +
				"function cursorPoint(evt) { pt.x = evt.clientX; pt.y = evt.clientY; return pt.matrixTransform(svg.getScreenCTM().inverse()); } " +
				"function displayName(name) { document.getElementById('biome_name').innerHTML = name; } " +
				"function highlightBiome(id) { svg.getElementById(id).style.stroke = \"1px\"; colorOld = svg.getElementById(id).style.fill; svg.getElementById(id).style.fill = \"#FF0000\"; } " +
				"function highlightBiomeOff(id) { svg.getElementById(id).style.stroke = \"none\"; svg.getElementById(id).style.fill = colorOld; }" +
				"</script>\n" +
				"\t<body>\n\t\t<div id=\"main\">\n\t\t\t<div id=\"map\">";

		String biomeList = "</div>\n\t\t\t<div id=\"infobar\">\n\t\t\t\t<div id=\"infomap\">Biome: <span id='biome_name'>&nbsp;</span><br/>Coordonn&eacute;e: <span id='biome_coord'>&nbsp;</span></div>\n\t\t\t\t<div id=\"infobiome\">\n";

		String footer = "\t\t\t\t</div>\n\t\t\t</div>\n\t\t</div>\n\t\t<script>var svg  = document.getElementById(\"biomeatlas\"); var pt   = svg.createSVGPoint(); " +
				"svg.addEventListener('mousemove',function(evt){ var loc = cursorPoint(evt); document.getElementById('biome_coord').innerHTML = \"x: \"+ ((Math.ceil(loc.x) * " + resolution + ") + (" + minBlockX + ")) + \" y:\" + ((Math.ceil(loc.y) * " + resolution + ") + (" + minBlockZ + ")); },false);" +
				"</script>\t\n</body>\n</html>\n";

		sendStatus("Generating map at (" + centerX + ", " + centerZ + ") spanning " + worldLength + ", " + worldLength + " at " + resolution + "x...");

		try
		{
			ChunkProviderServer cps = FMLCommonHandler.instance().getMinecraftServerInstance().worldServerForDimension(0).getChunkProvider();

			for (int blockX = minBlockX; blockX < maxBlockX; blockX += resolution)
			{
				int chunkX = blockX >> 4;
				int x = (blockX - minBlockX) / resolution;
				for (int blockZ = minBlockZ; blockZ < maxBlockZ; blockZ += resolution)
				{
					int chunkZ = blockZ >> 4;
					int y = (blockZ - minBlockZ) / resolution;
					Rectangle rect = new Rectangle(x, y, 1, 1);

					if(!cps.chunkExists(chunkX, chunkZ))
					{
						cps.loadChunk(chunkX, chunkZ);
					}

					Biome biome = world.getBiomeGenForCoords(new BlockPos(blockX, 50, blockZ));

					if(mapBiomes.containsKey(biome))
					{
						mapBiomes.get(biome).add(new Area(rect));
					}
					else
					{
						Area ar = new Area();
						ar.add(new Area(rect));

						mapBiomes.put(biome, ar);
					}

					completedBlocks++;

					long now = System.currentTimeMillis();
					if (now - lastMessageTime > messageRate)
					{
						sendStatus(String.format("BiomeAtlas thread: %d/%d (%f%%)", completedBlocks, blockCount, (completedBlocks / (double) blockCount * 100)));
						lastMessageTime = now;
					}
				}
			}

			sendStatus("Creating output file...");

			long startTimer = System.currentTimeMillis();
			StringBuilder sbBiome = new StringBuilder();
			for (Biome b : mapBiomes.keySet())
			{
				sbBiome.append("\t\t\t\t\t<span class=\"biometext\">|&nbsp;<a href=\"#\" onmouseover=\"highlightBiome('");
				sbBiome.append(b.getRegistryName().toString());
				sbBiome.append("')\" onmouseout=\"highlightBiomeOff('");
				sbBiome.append(b.getRegistryName().toString());
				sbBiome.append("')\">");
				sbBiome.append(b.getBiomeName());
				sbBiome.append("</a>&nbsp;|</span>\n");

				//Area area = new Area();
				g2.setPaint(new Color(getBiomeRGB(b)));
				g2.setRenderingHint(SVGHints.KEY_ELEMENT_ID, b.getRegistryName().toString());
				g2.setRenderingHint(SVGHints.KEY_ELEMENT_CUSTOM, "onmouseover=\"displayName('" + StringEscapeUtils.escapeHtml3(b.getBiomeName()) + "')\"");
				g2.fill(mapBiomes.get(b));
			}

			StringBuilder sb = new StringBuilder();

			sb.append(header);
			sb.append(g2.getSVGElement("biomeatlas", null, null, new ViewBox(0, 0, mapImageLength, mapImageLength), PreserveAspectRatio.XMAX_YMAX, MeetOrSlice.MEET));
			sb.append(biomeList);
			sb.append(sbBiome.toString());
			sb.append(footer);

			FileUtils.writeStringToFile(outputFile, sb.toString());
			sendStatus("Done in " + (System.currentTimeMillis() - startTimer) + " ms");
		} catch (Exception ex)
		{
			logger.error(ex);
			sendStatus("Error: " + ex.getMessage());
		}
		finally
		{
			g2.dispose();
		}
	}

	private void sendStatus(String message)
	{
		for (Predicate observer : listeners)
		{
			if (observer.apply(message))
			{
				return;
			}
		}
	}

	private static int getBiomeRGB(Biome biome)
	{
		Biome.TempCategory tempCat = biome.getTempCategory();

		List<Type> biomeTypes = Lists.newArrayList();
		Collections.addAll(biomeTypes, BiomeDictionary.getTypesForBiome(biome));

		if (biome.getBiomeName().equals("Deep Ocean"))
		{
			return 0x00177F; // HSV(229, 100, 50)
		} else if (biome.getBiomeName().equals("Ocean"))
		{
			return 0x002EFF; // HSV(229, 100, 100)
		} else if (biome.getBiomeName().equals("FrozenOcean"))
		{
			return 0xB7D0FF; // HSV(229, 100, 50)
		} else if (biomeTypes.size() <= 2 && biomeTypes.contains(Type.OCEAN))
		{
			int hash = Math.abs(biome.getBiomeName().hashCode());
			int hue = 207;
			int sat = (hash % 29) + 71;
			int lum = (hash % 17) + 83;
			//217 -> 247
			return HSVtoRGB(hue, sat, lum);
		} else if (biomeTypes.contains(Type.MUSHROOM))
		{
			int hash = Math.abs(biome.getBiomeName().hashCode());
			int hue = (hash % 20) + 340;
			int sat = (hash % 15) + 85;
			int lum = (hash % 10) + 90;
			return HSVtoRGB(hue, sat, lum);
		} else if (biomeTypes.contains(Type.MAGICAL))
		{
			int hash = Math.abs(biome.getBiomeName().hashCode());
			int hue = (hash % 20) + 300;
			int sat = (hash % 20) + 80;
			int lum = (hash % 20) + 80;
			return HSVtoRGB(hue, sat, lum);
		} else if (biomeTypes.contains(Type.DEAD))
		{
			int hash = Math.abs(biome.getBiomeName().hashCode());
			int hue = (hash % 30) + 10;
			int sat = (hash % 50) + 50;
			int lum = (hash % 10) + 30;
			return HSVtoRGB(hue, sat, lum);
		} else if (biomeTypes.contains(Type.SPOOKY))
		{
			int hash = Math.abs(biome.getBiomeName().hashCode());
			int hue = hash % 360;
			int sat = (hash % 30);
			int lum = (hash % 20);
			return HSVtoRGB(hue, sat, lum);
		} else if (biomeTypes.contains(Type.SNOWY))
		{
			int hash = Math.abs(biome.getBiomeName().hashCode());
			int hue = hash % 360;
			int sat = hash % 10;
			int lum = (hash % 20) + 80;
			return HSVtoRGB(hue, sat, lum);
		} else if (biomeTypes.contains(Type.MOUNTAIN))
		{
			int hash = Math.abs(biome.getBiomeName().hashCode());
			int hue = hash % 360;
			int sat = 0;
			int lum = (hash % 40) + 30;
			return HSVtoRGB(hue, sat, lum);
		} else if (biomeTypes.contains(Type.SWAMP))
		{
			int hash = Math.abs(biome.getBiomeName().hashCode());
			int hue = (hash % 15) + 255;
			int sat = (hash % 40) + 60;
			int lum = (hash % 20) + 40;
			//217 -> 247
			return HSVtoRGB(hue, sat, lum);
		} else if (biomeTypes.contains(Type.JUNGLE))
		{
			int hash = Math.abs(biome.getBiomeName().hashCode());
			int hue = (hash % 40) + 110;
			int sat = 100;
			int lum = (hash % 20) + 30;
			//217 -> 247
			return HSVtoRGB(hue, sat, lum);
		} else if (biomeTypes.contains(Type.MESA))
		{
			int hash = Math.abs(biome.getBiomeName().hashCode());
			int hue = 24;
			int sat = (hash % 20) + 80;
			int lum = (hash % 30) + 70;
			//217 -> 247
			return HSVtoRGB(hue, sat, lum);
		} else if (biomeTypes.contains(Type.SAVANNA))
		{
			int hash = Math.abs(biome.getBiomeName().hashCode());
			int hue = (hash % 20) + 60;
			int sat = (hash % 50) + 50;
			int lum = (hash % 40) + 30;
			//217 -> 247
			return HSVtoRGB(hue, sat, lum);
		} else if (biomeTypes.contains(Type.BEACH))
		{
			int hash = Math.abs(biome.getBiomeName().hashCode());
			int hue = (hash % 10) + 50;
			int sat = (hash % 30) + 70;
			int lum = (hash % 10) + 90;
			//217 -> 247
			return HSVtoRGB(hue, sat, lum);
		} else if (tempCat == Biome.TempCategory.WARM)
		{
			int hash = Math.abs(biome.getBiomeName().hashCode());
			int hue = (hash % 5) + 55;
			int sat = (hash % 7) + 93;
			int lum = (hash % 10) + 90;
			//217 -> 247
			return HSVtoRGB(hue, sat, lum);
		} else if (tempCat == Biome.TempCategory.COLD)
		{
			return 0x0000FF;
		} else
		{
			int hash = Math.abs(biome.getBiomeName().hashCode());
			int hue = (hash % 70) + 90;
			int sat = (hash % 20) + 80;
			int lum = (hash % 30) + 70;
			//217 -> 247
			return HSVtoRGB(hue, sat, lum);
		}
	}

	private static int HSVtoRGB(int hue, int sat, int lum)
	{
		// H is given on [0->6] or -1. S and V are given on [0->1].
		// RGB are each returned on [0->1].
		float h = hue / (float) 60;
		float s = sat / (float) 100;
		float v = lum / (float) 100;
		float m, n, f;
		int i;

		float[] hsv = new float[3];
		float[] rgb = new float[3];

		hsv[0] = h;
		hsv[1] = s;
		hsv[2] = v;

		if (hsv[0] == -1)
		{
			rgb[0] = rgb[1] = rgb[2] = hsv[2];
		} else
		{
			i = (int) (Math.floor(hsv[0]));
			f = hsv[0] - i;
			if (i % 2 == 0)
			{
				f = 1 - f; // if i is even
			}
			m = hsv[2] * (1 - hsv[1]);
			n = hsv[2] * (1 - hsv[1] * f);
			switch (i)
			{
				case 6:
				case 0:
					rgb[0] = hsv[2];
					rgb[1] = n;
					rgb[2] = m;
					break;
				case 1:
					rgb[0] = n;
					rgb[1] = hsv[2];
					rgb[2] = m;
					break;
				case 2:
					rgb[0] = m;
					rgb[1] = hsv[2];
					rgb[2] = n;
					break;
				case 3:
					rgb[0] = m;
					rgb[1] = n;
					rgb[2] = hsv[2];
					break;
				case 4:
					rgb[0] = n;
					rgb[1] = m;
					rgb[2] = hsv[2];
					break;
				case 5:
					rgb[0] = hsv[2];
					rgb[1] = m;
					rgb[2] = n;
					break;
			}
		}

		return (((int) (rgb[0] * 0xFF)) << 16) + (((int) (rgb[1] * 0xFF)) << 8) + (((int) (rgb[2] * 0xFF)));
	}
}