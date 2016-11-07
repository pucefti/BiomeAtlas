package com.sk89q.biomeatlas.config;

import com.sk89q.biomeatlas.BiomeAtlas;
import net.minecraftforge.common.config.Configuration;

import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ben on 28/10/2016.
 * Color config
 */
public class BiomeColorConfig
{
	public static Map<String, Integer> biomeColorMap;
	private static Configuration config;
	private static Map<String, Color> biomeDefaultColorMap;

	public static void init(File configFile)
	{
		if (config == null)
		{
			config = new Configuration(configFile);
			biomeColorMap = new HashMap<String, Integer>();
			biomeDefaultColorMap = new HashMap<String, Color>();
			defaultConfiguration();
			loadConfiguration();
		}
	}

	static void loadConfiguration()
	{
		try
		{
			biomeColorMap.clear();
			for (String biomeName: config.getCategoryNames())
			{
				Color biomeColor = new Color(127, 127, 127);
				if(biomeDefaultColorMap.containsKey(biomeName))
				{
					biomeColor = biomeDefaultColorMap.get(biomeName);
				}

				int r = config.getInt("r", biomeName, biomeColor.getRed(), 0, 255, "Red component");
				int g = config.getInt("g", biomeName, biomeColor.getRed(), 0, 255, "Green component");
				int b = config.getInt("b", biomeName, biomeColor.getRed(), 0, 255, "Blue component");
				Color color = new Color(r, g, b);

				biomeColorMap.put(biomeName.toLowerCase(), color.getRGB());

				BiomeAtlas.logger.debug("Add " + biomeName + " with color: " + color);
			}

			for (String biomeName: biomeDefaultColorMap.keySet())
			{
				if(!biomeColorMap.containsKey(biomeName))
				{
					Color biomeColor = biomeDefaultColorMap.get(biomeName);

					int r = config.getInt("r", biomeName, biomeColor.getRed(), 0, 255, "Red component");
					int g = config.getInt("g", biomeName, biomeColor.getGreen(), 0, 255, "Green component");
					int b = config.getInt("b", biomeName, biomeColor.getBlue(), 0, 255, "Blue component");
					Color color = new Color(r, g, b);

					biomeColorMap.put(biomeName.toLowerCase(), color.getRGB());

					BiomeAtlas.logger.debug("Add " + biomeName + " with color: " + color);
				}
			}
		}
		catch (Exception e)
		{
			BiomeAtlas.logger.error("BiomeAtlas has encountered a problem loading biomecolor.cfg", e);
		}
		finally
		{
			if (config.hasChanged()) config.save();
		}
	}

	private static void defaultConfiguration()
	{
		biomeDefaultColorMap.clear();

		//Vanilla
		biomeDefaultColorMap.put("Ocean", new Color(0x2929ff));
		biomeDefaultColorMap.put("Plains", new Color(141, 179, 96));
		biomeDefaultColorMap.put("Desert", new Color(250, 148, 24));
		biomeDefaultColorMap.put("Extreme Hills", new Color(96, 96, 96));
		biomeDefaultColorMap.put("Forest", new Color(5, 102, 33));
		biomeDefaultColorMap.put("Taiga", new Color(11, 102, 89));
		biomeDefaultColorMap.put("Swampland", new Color(7, 249, 178));
		biomeDefaultColorMap.put("River", new Color(0x294cff));
		biomeDefaultColorMap.put("Hell", new Color(255, 0, 0));
		biomeDefaultColorMap.put("The End", new Color(128, 128, 255));
		biomeDefaultColorMap.put("Frozen Ocean", new Color(0xa6e5ff));
		biomeDefaultColorMap.put("Frozen River", new Color(0x97c3ff));
		biomeDefaultColorMap.put("Ice Plains", new Color(255, 255, 255));
		biomeDefaultColorMap.put("Ice Mountains", new Color(160, 160, 160));
		biomeDefaultColorMap.put("Mushroom Island", new Color(255, 0, 255));
		biomeDefaultColorMap.put("Mushroom Island Shore", new Color(160, 0, 255));
		biomeDefaultColorMap.put("Beach", new Color(250, 222, 85));
		biomeDefaultColorMap.put("Desert Hills", new Color(210, 95, 18));
		biomeDefaultColorMap.put("Forest Hills", new Color(34, 85, 28));
		biomeDefaultColorMap.put("Taiga Hills", new Color(22, 57, 51));
		biomeDefaultColorMap.put("Extreme Hills Edge", new Color(114, 120, 154));
		biomeDefaultColorMap.put("Jungle", new Color(83, 123, 9));
		biomeDefaultColorMap.put("Jungle Hills", new Color(44, 66, 5));
		biomeDefaultColorMap.put("Jungle Edge", new Color(98, 139, 23));
		biomeDefaultColorMap.put("Deep Ocean", new Color(0x0000b4));
		biomeDefaultColorMap.put("Stone Beach", new Color(162, 162, 132));
		biomeDefaultColorMap.put("Cold Beach", new Color(250, 240, 192));
		biomeDefaultColorMap.put("Birch Forest", new Color(48, 116, 68));
		biomeDefaultColorMap.put("Birch Forest Hills", new Color(31, 95, 50));
		biomeDefaultColorMap.put("Roofed Forest", new Color(64, 81, 26));
		biomeDefaultColorMap.put("Cold Taiga", new Color(49, 85, 74));
		biomeDefaultColorMap.put("Cold Taiga Hills", new Color(36, 63, 54));
		biomeDefaultColorMap.put("Mega Taiga", new Color(89, 102, 81));
		biomeDefaultColorMap.put("Mega Taiga Hills", new Color(69, 79, 62));
		biomeDefaultColorMap.put("Extreme Hills+", new Color(80, 112, 80));
		biomeDefaultColorMap.put("Savanna", new Color(189, 178, 95));
		biomeDefaultColorMap.put("Savanna Plateau", new Color(167, 157, 100));
		biomeDefaultColorMap.put("Mesa", new Color(217, 69, 21));
		biomeDefaultColorMap.put("Mesa Plateau F", new Color(176, 151, 101));
		biomeDefaultColorMap.put("Mesa Plateau", new Color(202, 140, 101));
		biomeDefaultColorMap.put("Ocean M", new Color(0x1717ff));
		biomeDefaultColorMap.put("Sunflower Plains", new Color(181, 219, 136));
		biomeDefaultColorMap.put("Desert M", new Color(255, 188, 64));
		biomeDefaultColorMap.put("Extreme Hills M", new Color(136, 136, 136));
		biomeDefaultColorMap.put("Flower Forest", new Color(45, 142, 73));
		biomeDefaultColorMap.put("Taiga M", new Color(51, 142, 129));
		biomeDefaultColorMap.put("Swampland M", new Color(47, 255, 218));
		biomeDefaultColorMap.put("River M", new Color(0x294cff));
		biomeDefaultColorMap.put("Hell M", new Color(255, 40, 40));
		biomeDefaultColorMap.put("Sky M", new Color(168, 168, 255));
		biomeDefaultColorMap.put("Frozen Ocean M", new Color(0xa6e5ff));
		biomeDefaultColorMap.put("Frozen River M", new Color(0x97c3ff));
		biomeDefaultColorMap.put("Ice Plains Spikes", new Color(180, 220, 220));
		biomeDefaultColorMap.put("Ice Mountains M", new Color(200, 200, 200));
		biomeDefaultColorMap.put("Mushroom Island M", new Color(255, 40, 255));
		biomeDefaultColorMap.put("Mushroom Island Shore M", new Color(200, 40, 255));
		biomeDefaultColorMap.put("Beach M", new Color(255, 255, 125));
		biomeDefaultColorMap.put("Desert Hills M", new Color(250, 135, 58));
		biomeDefaultColorMap.put("Forest Hills M", new Color(74, 125, 68));
		biomeDefaultColorMap.put("Taiga Hills M", new Color(62, 97, 91));
		biomeDefaultColorMap.put("Extreme Hills Edge M", new Color(154, 160, 194));
		biomeDefaultColorMap.put("Jungle M", new Color(123, 163, 49));
		biomeDefaultColorMap.put("Jungle Hills M", new Color(84, 106, 45));
		biomeDefaultColorMap.put("Jungle Edge M", new Color(138, 179, 63));
		biomeDefaultColorMap.put("Deep Ocean M", new Color(0x0000b0));
		biomeDefaultColorMap.put("Stone Beach M", new Color(202, 202, 172));
		biomeDefaultColorMap.put("Cold Beach M", new Color(255, 255, 232));
		biomeDefaultColorMap.put("Birch Forest M", new Color(88, 156, 108));
		biomeDefaultColorMap.put("Birch Forest Hills M", new Color(71, 135, 90));
		biomeDefaultColorMap.put("Roofed Forest M", new Color(104, 121, 66));
		biomeDefaultColorMap.put("Cold Taiga M", new Color(89, 125, 114));
		biomeDefaultColorMap.put("Cold Taiga Hills M", new Color(76, 103, 94));
		biomeDefaultColorMap.put("Mega Spruce Taiga", new Color(129, 142, 121));
		biomeDefaultColorMap.put("Mega Spruce Taiga (Hills)", new Color(109, 119, 102));
		biomeDefaultColorMap.put("Extreme Hills+ M", new Color(120, 152, 120));
		biomeDefaultColorMap.put("Savanna M", new Color(229, 218, 135));
		biomeDefaultColorMap.put("Savanna Plateau M", new Color(207, 197, 140));
		biomeDefaultColorMap.put("Mesa (Bryce)", new Color(255, 109, 61));
		biomeDefaultColorMap.put("Mesa Plateau F M", new Color(216, 191, 141));
		biomeDefaultColorMap.put("Mesa Plateau M", new Color(242, 180, 141));

		//Biome o Plenty
		biomeDefaultColorMap.put("Alps", new Color(13421772));
		biomeDefaultColorMap.put("Bamboo Forest", new Color(0xA3E053));
		biomeDefaultColorMap.put("Bayou", new Color(0x7DAD51));
		biomeDefaultColorMap.put("Bog", new Color(0xD8935F));
		biomeDefaultColorMap.put("Boreal Forest", new Color(0x9FB771));
		biomeDefaultColorMap.put("Brushland", new Color(0xC6C19B));
		biomeDefaultColorMap.put("Chaparral", new Color(0xC0D85D));
		biomeDefaultColorMap.put("Cherry Blossom Grove", new Color(0xF88F8F));
		biomeDefaultColorMap.put("Cold Desert", new Color(0xB3AF9B));
		biomeDefaultColorMap.put("Coniferous Forest (Snow)", new Color(0x528F60));
		biomeDefaultColorMap.put("Coniferous Forest", new Color(0x528F60));
		biomeDefaultColorMap.put("Coral Reef", new Color(0x5079ff));
		biomeDefaultColorMap.put("Crag", new Color(5209457));
		biomeDefaultColorMap.put("Dead Forest", new Color(0xBCA165));
		biomeDefaultColorMap.put("Dead Swamp", new Color(0x8BAF48));
		biomeDefaultColorMap.put("Dense Forest", new Color(0x006716));
		biomeDefaultColorMap.put("Eucalyptus Forest", new Color(0x9DCC70));
		biomeDefaultColorMap.put("Fen", new Color(0xBAC481));
		biomeDefaultColorMap.put("Flower Field", new Color(4044093));
		biomeDefaultColorMap.put("Flower Island", new Color(0x74D374));
		biomeDefaultColorMap.put("Glacier", new Color(11582425));
		biomeDefaultColorMap.put("Grassland", new Color(0x7FDB7D));
		biomeDefaultColorMap.put("Gravel Beach", new Color(0x908884));
		biomeDefaultColorMap.put("Grove", new Color(0x517F51));
		biomeDefaultColorMap.put("Heathland", new Color(0xADAE68));
		biomeDefaultColorMap.put("Highland", new Color(0x7CAD66));
		biomeDefaultColorMap.put("Kelp Forest", new Color(0x3f6ffa));
		biomeDefaultColorMap.put("Land of Lakes Marsh", new Color(0x66A06E));
		biomeDefaultColorMap.put("Land of Lakes", new Color(0x66A06E));
		biomeDefaultColorMap.put("Lavender Fields", new Color(189, 149, 194));
		biomeDefaultColorMap.put("Lush Desert", new Color(0x8AA92D));
		biomeDefaultColorMap.put("Lush River", new Color(0x294cff));
		biomeDefaultColorMap.put("Lush Swamp", new Color(0x57AE34));
		biomeDefaultColorMap.put("Mangrove", new Color(7251289));
		biomeDefaultColorMap.put("Maple Woods", new Color(97, 38, 38));
		biomeDefaultColorMap.put("Marsh", new Color(0x66A06E));
		biomeDefaultColorMap.put("Meadow", new Color(0x63B26D));
		biomeDefaultColorMap.put("Moor", new Color(0x619365));
		biomeDefaultColorMap.put("Mountain Foothills", new Color(0x717171));
		biomeDefaultColorMap.put("Mountain", new Color(0x8f8f8f));
		biomeDefaultColorMap.put("Mystic Grove", new Color(0x69CFDB));
		biomeDefaultColorMap.put("Oasis", new Color(7712283));
		biomeDefaultColorMap.put("Ominous Woods", new Color(0x3F4151));
		biomeDefaultColorMap.put("Orchard", new Color(14024557));
		biomeDefaultColorMap.put("Origin Valley", new Color(10341485));
		biomeDefaultColorMap.put("Outback", new Color(0xA57644));
		biomeDefaultColorMap.put("Overgrown Cliffs", new Color(8373350));
		biomeDefaultColorMap.put("Prairie", new Color(0xC8E580));
		biomeDefaultColorMap.put("Quagmire", new Color(0x503A2B));
		biomeDefaultColorMap.put("Rainforest", new Color(0x14E26F));
		biomeDefaultColorMap.put("Redwood Forest", new Color(0x6DAA3C));
		biomeDefaultColorMap.put("Sacred Springs", new Color(39259));
		biomeDefaultColorMap.put("Savanna Plateau (Sub-Biome)", new Color(167, 157, 100));
		biomeDefaultColorMap.put("Seasonal Forest", new Color(0xd26115));
		biomeDefaultColorMap.put("Seasonal Spruce Forest (Sub-Biome)", new Color(0xd26115));
		biomeDefaultColorMap.put("Shield", new Color(0x647F38));
		biomeDefaultColorMap.put("Shore", new Color(0xe7e89c));
		biomeDefaultColorMap.put("Shrubland", new Color(8168286));
		biomeDefaultColorMap.put("Sludgepit", new Color(0x637e28));
		biomeDefaultColorMap.put("Snowy Coniferous Forest", new Color(0xFFFFFF));
		biomeDefaultColorMap.put("Snowy Forest", new Color(0xABD6BC));
		biomeDefaultColorMap.put("Steppe", new Color(13413215));
		biomeDefaultColorMap.put("Temperate Rainforest", new Color(0xBBDD63));
		biomeDefaultColorMap.put("Tropical Islands", new Color(2211330));
		biomeDefaultColorMap.put("Tropical Rainforest", new Color(0x88E140));
		biomeDefaultColorMap.put("Tundra", new Color(0xA09456));
		biomeDefaultColorMap.put("Volcanic Island", new Color(6645093));
		biomeDefaultColorMap.put("Wasteland", new Color(0x5A5440));
		biomeDefaultColorMap.put("Wetland", new Color(0x4F9657));
		biomeDefaultColorMap.put("Woodland", new Color(0x84A92D));
		biomeDefaultColorMap.put("Xeric Shrubland", new Color(0xE2CDA5));

		//AbyssalCraft
		biomeDefaultColorMap.put("Darklands", new Color(90, 1, 147));
		biomeDefaultColorMap.put("Darklands Forest", new Color(99, 17, 129));
		biomeDefaultColorMap.put("Darklands Highland", new Color(90, 7, 117));
		biomeDefaultColorMap.put("Darklands Mountains", new Color(49, 0, 91));
		biomeDefaultColorMap.put("Darklands Plains", new Color(99, 17, 147));
		biomeDefaultColorMap.put("Coralium Infested", new Color(79, 2, 179));

		//BetterAgriculture
		biomeDefaultColorMap.put("FarmlandBiome", new Color(0x00d300));
	}
}
