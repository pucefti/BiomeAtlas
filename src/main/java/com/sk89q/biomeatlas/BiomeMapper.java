package com.sk89q.biomeatlas;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class BiomeMapper
{
	public static Logger logger = LogManager.getLogger("BiomeMapper");

	private final List<Predicate<String>> listeners = Lists.newArrayList();
	private int lineHeight = 8;
	private int statsLegendSpacing = lineHeight / 2;
	private int legendMapSpacing = 5;
	private int iconTextSpacing = 5;
	private int legendLabelSpacing = 200;
	private int messageRate = 5000;
	private int resolution = 1;

	public int getMessageRate()
	{
		return messageRate;
	}

	public void setMessageRate(int messageRate)
	{
		checkArgument(messageRate >= 10, "messageRate >= 10");
		this.messageRate = messageRate;
	}

	public int getResolution()
	{
		return resolution;
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

		BiomeProvider chunkManager = world.getBiomeProvider();

		int minBlockX = centerX - apothem;
		int minBlockZ = centerZ - apothem;
		int maxBlockX = centerX + apothem;
		int maxBlockZ = centerZ + apothem;
		int worldLength = apothem * 2;
		int mapImageLength = (int) Math.ceil(worldLength / (double) resolution);

		BufferedImage mapImage = new BufferedImage(mapImageLength, mapImageLength, BufferedImage.TYPE_INT_RGB);
		Set<Biome> seenBiomes = Sets.newHashSet();

		// Progress tracking
		int blockCount = mapImageLength * mapImageLength;
		int completedBlocks = 0;
		long lastMessageTime = System.currentTimeMillis();

		sendStatus("Generating map at (" + centerX + ", " + centerZ + ") spanning " + worldLength + ", " + worldLength + " at " + resolution + "x...");

		try
		{
			for (int blockX = minBlockX; blockX < maxBlockX; blockX += resolution)
			{
				for (int blockZ = minBlockZ; blockZ < maxBlockZ; blockZ += resolution)
				{
					Biome biome = chunkManager.getBiomesForGeneration(null, blockX, blockZ, 1, 1)[0];

					int x = (blockX - minBlockX) / resolution;
					int y = (blockZ - minBlockZ) / resolution;

					mapImage.setRGB(x, y, getBiomeRGB(biome));
					seenBiomes.add(biome);

					completedBlocks++;

					long now = System.currentTimeMillis();
					if (now - lastMessageTime > messageRate)
					{
						sendStatus(String.format("BiomeAtlas thread: %d/%d (%f%%)", completedBlocks, blockCount, (completedBlocks / (double) blockCount * 100)));
						lastMessageTime = now;
					}
				}
			}
		} catch (Exception ex)
		{
			logger.error(ex);
			sendStatus("Error: " + ex.getMessage());
			return;
		}

		sendStatus("Creating output image...");

		int legendHeight = seenBiomes.size() * lineHeight;
		int outputWidth = mapImage.getWidth() + legendLabelSpacing;
		int outputHeight = Math.max(mapImage.getHeight(), legendHeight + lineHeight + statsLegendSpacing);

		BufferedImage outputImage = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = (Graphics2D) outputImage.getGraphics();

		try
		{
			// Background
			g2d.setColor(Color.WHITE);
			g2d.fill(new Rectangle(0, 0, outputImage.getWidth(), outputImage.getHeight()));

			// Copy image to output image
			g2d.drawImage(mapImage, 0, 0, null);

			// Paint size
			g2d.setFont(new Font("Sans", 0, 9));
			FontMetrics fm = g2d.getFontMetrics();
			g2d.setPaint(Color.GRAY);
			g2d.drawString(String.format("%d x %d at %d, %d (%dx)", worldLength, worldLength, centerX, centerZ, resolution),
					mapImage.getWidth() + legendMapSpacing, outputHeight - fm.getHeight() / 2 + statsLegendSpacing);
		} finally
		{
			g2d.dispose();
		}

		// Paint legend
		//paintLegend(outputImage, seenBiomes, mapImage.getWidth() + legendMapSpacing, 0);

		try
		{
			ImageIO.write(outputImage, "png", outputFile);

			sendStatus("Written to: " + outputFile.getAbsolutePath());
		} catch (IOException e)
		{
			BiomeAtlas.logger.error("Failed to generate biome map", e);
			sendStatus("Map generation failed because the file couldn't be written! More details can be found in the log.");
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

	private void paintLegend(BufferedImage image, Set<Biome> biomes, int baseX, int baseY)
	{
		List<Biome> sortedBiomes = Lists.newArrayList(biomes);
		Collections.sort(sortedBiomes, new BiomeColorComparator());

		Graphics2D g2d = (Graphics2D) image.getGraphics();

		try
		{
			g2d.setFont(new Font("Sans", 0, 9));
			FontMetrics fm = g2d.getFontMetrics();

			int i = 0;
			for (Biome biome : sortedBiomes)
			{
				int y = lineHeight * i;
				g2d.setColor(new Color(getBiomeRGB(biome)));
				g2d.fill(new Rectangle(baseX, baseY + y, lineHeight, lineHeight));

				g2d.setPaint(Color.BLACK);
				g2d.drawString(biome.getBiomeName(), baseX + lineHeight + iconTextSpacing, baseY + y + fm.getHeight() / 2 + 1);

				i++;
			}
		} finally
		{
			g2d.dispose();
		}
	}

	private static int getBiomeRGB(Biome biome)
	{
		Biome.TempCategory tempCat = biome.getTempCategory();

		List<Type> biomeTypes = Lists.newArrayList();
		for (final Type type : BiomeDictionary.getTypesForBiome(biome))
		{
			biomeTypes.add(type);
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

	private static class BiomeColorComparator implements Comparator<Biome>
	{
		@Override
		public int compare(Biome biome1, Biome biome2)
		{
			Color c1 = new Color(getBiomeRGB(biome1));
			Color c2 = new Color(getBiomeRGB(biome2));
			float[] hsv1 = new float[3];
			float[] hsv2 = new float[3];
			Color.RGBtoHSB(c1.getRed(), c1.getGreen(), c1.getBlue(), hsv1);
			Color.RGBtoHSB(c2.getRed(), c2.getGreen(), c2.getBlue(), hsv2);
			if (hsv1[0] < hsv2[0])
			{
				return -1;
			} else if (hsv1[0] > hsv2[0])
			{
				return 1;
			} else
			{
				if (hsv1[1] < hsv2[1])
				{
					return -1;
				} else if (hsv1[1] > hsv2[1])
				{
					return 1;
				} else
				{
					return 0;
				}
			}

            /*if(biome1.getBiomeName().equals("Deep Ocean")
                    || biome1.getBiomeName().equals("Ocean")
                    || biome1.getBiomeName().equals("FrozenOcean")
                    || biome1.getBiomeName().equals("FrozenOcean")
                    || (biome1.size() <= 2 && biome1.contains(Type.OCEAN)))
            {
                return 0x00177F; // HSV(229, 100, 50)
            }

            else if(biome.getBiomeName().equals("Ocean"))
            {
                return 0x002EFF; // HSV(229, 100, 100)
            }
            else if(biome.getBiomeName().equals("FrozenOcean"))
            {
                return 0xB7D0FF; // HSV(229, 100, 50)
            }
            else if(biomeTypes.size() <= 2 && biomeTypes.contains(Type.OCEAN))
            {
                int hash = Math.abs(biome.getBiomeName().hashCode());
                int hue = 207;
                int sat = (hash % 29) + 71;
                int lum = (hash % 17) + 83;
                //217 -> 247
                return HSVtoRGB(hue, sat, lum);
            }
            else if(biomeTypes.contains(Type.MUSHROOM))
            {
                int hash = Math.abs(biome.getBiomeName().hashCode());
                int hue = (hash % 20) + 340;
                int sat = (hash % 15) + 85;
                int lum = (hash % 10) + 90;
                return HSVtoRGB(hue, sat, lum);
            }
            else if(biomeTypes.contains(Type.MAGICAL))
            {
                int hash = Math.abs(biome.getBiomeName().hashCode());
                int hue = (hash % 20) + 300;
                int sat = (hash % 20) + 80;
                int lum = (hash % 20) + 80;
                return HSVtoRGB(hue, sat, lum);
            }
            else if(biomeTypes.contains(Type.DEAD))
            {
                int hash = Math.abs(biome.getBiomeName().hashCode());
                int hue = (hash % 30) + 10;
                int sat = (hash % 50) + 50;
                int lum = (hash % 10) + 30;
                return HSVtoRGB(hue, sat, lum);
            }
            else if(biomeTypes.contains(Type.SPOOKY))
            {
                int hash = Math.abs(biome.getBiomeName().hashCode());
                int hue = hash % 360;
                int sat = (hash % 30);
                int lum = (hash % 20);
                return HSVtoRGB(hue, sat, lum);
            }
            else if(biomeTypes.contains(Type.SNOWY))
            {
                int hash = Math.abs(biome.getBiomeName().hashCode());
                int hue = hash % 360;
                int sat = hash % 10;
                int lum = (hash % 20) + 80;
                return HSVtoRGB(hue, sat, lum);
            }
            else if(biomeTypes.contains(Type.MOUNTAIN))
            {
                int hash = Math.abs(biome.getBiomeName().hashCode());
                int hue = hash % 360;
                int sat = 0;
                int lum = (hash % 40) + 30;
                return HSVtoRGB(hue, sat, lum);
            }
            else if(biomeTypes.contains(Type.SWAMP))
            {
                int hash = Math.abs(biome.getBiomeName().hashCode());
                int hue = (hash % 15) + 255;
                int sat = (hash % 40) + 60;
                int lum = (hash % 20) + 40;
                //217 -> 247
                return HSVtoRGB(hue, sat, lum);
            }
            else if(biomeTypes.contains(Type.JUNGLE))
            {
                int hash = Math.abs(biome.getBiomeName().hashCode());
                int hue = (hash % 40) + 110;
                int sat = 100;
                int lum = (hash % 20) + 30;
                //217 -> 247
                return HSVtoRGB(hue, sat, lum);
            }
            else if(biomeTypes.contains(Type.MESA))
            {
                int hash = Math.abs(biome.getBiomeName().hashCode());
                int hue = 24;
                int sat = (hash % 20) + 80;
                int lum = (hash % 30) + 70;
                //217 -> 247
                return HSVtoRGB(hue, sat, lum);
            }
            else if(biomeTypes.contains(Type.SAVANNA))
            {
                int hash = Math.abs(biome.getBiomeName().hashCode());
                int hue = (hash % 20) + 60;
                int sat = (hash % 50) + 50;
                int lum = (hash % 40) + 30;
                //217 -> 247
                return HSVtoRGB(hue, sat, lum);
            }
            else if(biomeTypes.contains(Type.BEACH))
            {
                int hash = Math.abs(biome.getBiomeName().hashCode());
                int hue = (hash % 10) + 50;
                int sat = (hash % 30) + 70;
                int lum = (hash % 10) + 90;
                //217 -> 247
                return HSVtoRGB(hue, sat, lum);
            }
            else if(tempCat == Biome.TempCategory.WARM)
            {
                int hash = Math.abs(biome.getBiomeName().hashCode());
                int hue = (hash % 5) + 55;
                int sat = (hash % 7) + 93;
                int lum = (hash % 10) + 90;
                //217 -> 247
                return HSVtoRGB(hue, sat, lum);
            }
            else if(tempCat == Biome.TempCategory.COLD)
            {
                return 0x0000FF;
            }
            else
            {
                int hash = Math.abs(biome.getBiomeName().hashCode());
                int hue = (hash % 70) + 90;
                int sat = (hash % 20) + 80;
                int lum = (hash % 30) + 70;
                //217 -> 247
                return HSVtoRGB(hue, sat, lum);
            }*/
		}
	}

	public static int HSVtoRGB(int hue, int sat, int lum)
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