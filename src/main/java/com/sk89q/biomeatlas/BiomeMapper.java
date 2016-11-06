package com.sk89q.biomeatlas;

import com.google.common.collect.Lists;
import com.sk89q.biomeatlas.config.BiomeColorConfig;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jfree.graphics2d.svg.*;

import java.awt.*;
import java.awt.geom.Area;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class BiomeMapper
{
	private World _worldServer;
	private ChunkProviderServer _cps;

	private boolean _startGeneration;
	private boolean _pendingGeneration;

	private int _resolution;
	private int _minBlockX;
	private int _currentBlockX;
	private int _maxBlockX;
	private int _minBlockZ;
	private int _currentBlockZ;
	private int _maxBlockZ;

	private int _mapImageLength;
	private int _totalBlocks;
	private int _totalCompletedBlocks;

	private Map<Biome, Area> _mapBiomes;

	BiomeMapper(WorldServer worldServer, int centerX, int centerZ, int apothem, int resolution)
	{
		_startGeneration = false;
		_pendingGeneration = false;
		_mapBiomes = new TreeMap<Biome, Area>(new Comparator<Biome>()
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

		setData(worldServer, centerX, centerZ, apothem, resolution);
	}

	public void setData(WorldServer worldServer, int centerX, int centerZ, int apothem, int resolution)
	{
		checkNotNull(worldServer);
		checkArgument(apothem >= 100, "apothem >= 100");
		checkArgument(resolution >= 1, "resolution >= 1");

		_worldServer = worldServer;
		_cps = worldServer.getChunkProvider();

		_resolution = resolution;
		_minBlockX = centerX - apothem;
		_maxBlockX = centerX + apothem;
		_minBlockZ = centerZ - apothem;
		_maxBlockZ = centerZ + apothem;

		_currentBlockX = 0;
		_currentBlockZ = 0;

		_mapImageLength = (int) Math.ceil((apothem * 2) / (double) _resolution);
		_totalBlocks = _mapImageLength * _mapImageLength;
	}

	public void startGeneration()
	{
		_totalCompletedBlocks = 0;
		_currentBlockX = _minBlockX;
		_currentBlockZ = _minBlockZ;
		_mapBiomes.clear();

		_startGeneration = true;
	}

	public void stopGeneration()
	{
		_startGeneration = false;
	}

	public String getProgress()
	{
		return String.format("%d/%d", _totalCompletedBlocks, _totalBlocks);
	}

	public String getProgressInPercent()
	{
		return String.format("%.2f%%", (_totalCompletedBlocks / (double) _totalBlocks * 100));
	}

	public boolean isPendingGenerationExist()
	{
		return _pendingGeneration;
	}

	public boolean isGenerationStarted()
	{
		return _startGeneration;
	}

	public boolean isAnalyzaCompleted()
	{
		return ((_currentBlockX + _resolution) >= _maxBlockX) && (_currentBlockZ >= _maxBlockZ);
	}

	public void analyzeRegion(int maxAnalyzedBlocks)
	{
		_pendingGeneration = true;
		try
		{
			int completedBlocks = 0;
			for (int blockX = _currentBlockX; blockX < _maxBlockX; blockX += _resolution)
			{
				_currentBlockX = blockX;
				int chunkX = blockX >> 4;
				int x = (blockX - _minBlockX) / _resolution;
				for (int blockZ = _currentBlockZ; blockZ < _maxBlockZ; blockZ += _resolution)
				{
					_currentBlockZ = blockZ + _resolution;
					int chunkZ = blockZ >> 4;
					int y = (blockZ - _minBlockZ) / _resolution;
					Rectangle rect = new Rectangle(x, y, 1, 1);

					if(!_cps.chunkExists(chunkX, chunkZ))
					{
						_cps.loadChunk(chunkX, chunkZ);
					}

					Biome biome = _worldServer.getBiomeGenForCoords(new BlockPos(blockX, 50, blockZ));

					if(_mapBiomes.containsKey(biome))
					{
						_mapBiomes.get(biome).add(new Area(rect));
					}
					else
					{
						Area ar = new Area();
						ar.add(new Area(rect));

						_mapBiomes.put(biome, ar);
					}

					completedBlocks++;
					_totalCompletedBlocks++;

					if (completedBlocks >= maxAnalyzedBlocks)
					{
						_cps.unloadAllChunks();
						return;
					}
				}

				_cps.unloadAllChunks();

				if((_currentBlockX + _resolution) < _maxBlockX)
				{
					_currentBlockZ = _minBlockZ;
				}
			}
		}
		finally
		{
			_pendingGeneration = false;
		}
	}

	public void generateFile() throws IOException
	{
		SVGGraphics2D g2 = new SVGGraphics2D(_mapImageLength, _mapImageLength);

		try
		{
			String header = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" +
					"<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"fr\" lang=\"fr\">\n" +
					"\t<head>\n" +
					"\t<title>" + _worldServer.getSeed() + "</title>\n" +
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
					"svg.addEventListener('mousemove',function(evt){ var loc = cursorPoint(evt); document.getElementById('biome_coord').innerHTML = \"x: \"+ ((Math.ceil(loc.x) * " + _resolution + ") + (" + _minBlockX + ")) + \" y:\" + ((Math.ceil(loc.y) * " + _resolution + ") + (" + _minBlockZ + ")); },false);" +
					"</script>\t\n</body>\n</html>\n";

			StringBuilder sbBiome = new StringBuilder();
			for (Biome b : _mapBiomes.keySet())
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
				g2.fill(_mapBiomes.get(b));
			}

			String fileContent = header +
					g2.getSVGElement("biomeatlas", null, null, new ViewBox(0, 0, _mapImageLength, _mapImageLength), PreserveAspectRatio.XMAX_YMAX, MeetOrSlice.MEET) +
					biomeList +
					sbBiome.toString() +
					footer;

			FileUtils.writeStringToFile(new File("biomeatlas_" + _worldServer.getSeed() + ".html"), fileContent);
		}
		finally
		{
			g2.dispose();
			_startGeneration = false;
		}
	}

	private static int getBiomeRGB(Biome biome)
	{
		Biome.TempCategory tempCat = biome.getTempCategory();

		List<Type> biomeTypes = Lists.newArrayList();
		Collections.addAll(biomeTypes, BiomeDictionary.getTypesForBiome(biome));

		if(BiomeColorConfig.biomeColorMap.containsKey(biome.getBiomeName().toLowerCase()))
		{
			return BiomeColorConfig.biomeColorMap.get(biome.getBiomeName().toLowerCase());
		}

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