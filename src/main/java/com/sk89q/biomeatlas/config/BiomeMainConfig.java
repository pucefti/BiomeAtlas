package com.sk89q.biomeatlas.config;

import com.sk89q.biomeatlas.BiomeAtlas;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;

/**
 * Created by bmi on 07/11/2016.
 * Main configuration
 */
public class BiomeMainConfig
{
	private static Configuration config;

	public static int NbBlockTick;
	public static boolean BroadcastPlayers;
	public static boolean ExitAfterCompletion;

	public static void init(File configFile)
	{
		if (config == null)
		{
			config = new Configuration(configFile);
			ExitAfterCompletion = false;

			loadConfiguration();
		}
	}

	private static void loadConfiguration()
	{
		try
		{
			NbBlockTick = config.getInt("BlockTick", "Main", 100, 1, 1000000, "Number of block analyze by server's tick.");
			BroadcastPlayers = config.getBoolean("Broadcast", "Main", false, "Broadcast all message to players.");

			if(NbBlockTick == 0)
			{
				NbBlockTick = 1;
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

	@SubscribeEvent
	public void onConfigurationChangedEvent(ConfigChangedEvent.OnConfigChangedEvent event)
	{
		if (event.getModID().equalsIgnoreCase(BiomeAtlas.MOD_ID))
		{
			BiomeMainConfig.loadConfiguration();
			BiomeColorConfig.loadConfiguration();
		}
	}
}
