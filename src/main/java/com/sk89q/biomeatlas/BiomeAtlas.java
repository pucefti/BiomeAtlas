package com.sk89q.biomeatlas;

import com.google.common.base.Predicate;
import com.sk89q.biomeatlas.command.CommandBiomeAtlas;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

@Mod(modid = BiomeAtlas.MOD_ID, version = BiomeAtlas.MOD_VERSION, name = BiomeAtlas.MOD_NAME, dependencies = "required-after:Forge@[12.18.1.2039,)")
public class BiomeAtlas
{

	public static final String MOD_NAME = "Biome Atlas";
	public static final String MOD_ID = "BiomeAtlas";
	public static final String MOD_VERSION = "@MOD_VERSION@";

	@Mod.Instance(MOD_ID)
	public static BiomeAtlas instance;

	public static Logger logger = LogManager.getLogger(MOD_ID);

	@EventHandler
	public void serverStarting(FMLServerStartingEvent evt)
	{
		evt.registerServerCommand(new CommandBiomeAtlas());
	}

	@EventHandler
	public void serverStarted(FMLServerStartedEvent evt)
	{
		if (System.getProperty("biomeAtlas.mapOnStartup", "false").equals("true"))
		{
			int apothem = Integer.parseInt(System.getProperty("biomeAtlas.apothem", "250"));
			int dimension = Integer.parseInt(System.getProperty("biomeAtlas.mapDimension", "0"));
			int centerX = Integer.parseInt(System.getProperty("biomeAtlas.centerX", "0"));
			int centerZ = Integer.parseInt(System.getProperty("biomeAtlas.centerZ", "0"));
			int resolution = Integer.parseInt(System.getProperty("biomeAtlas.resolution", "16"));

			if (apothem > 0 && resolution >= 1)
			{
				World world = FMLCommonHandler.instance().getMinecraftServerInstance().worldServerForDimension(dimension);

				BiomeMapper mapper = new BiomeMapper();
				mapper.setResolution(resolution);
				mapper.setMessageRate(2000);
				mapper.getListeners().add(new LoggerObserver());
				mapper.generate(world, centerX, centerZ, apothem, new File("biomeatlas_" + world.getSeed() + ".html"));

				if (System.getProperty("biomeAtlas.exitOnFinish", "false").equals("true"))
				{
					logger.info("BiomeAtlas finished generating! Now exiting Java as enabled.");
					FMLCommonHandler.instance().exitJava(0, false);
				}
			}
		}
	}

	private static class LoggerObserver implements Predicate<String>
	{
		@Override
		public boolean apply(String input)
		{
			logger.info(input);
			return false;
		}
	}

}
