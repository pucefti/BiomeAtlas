package com.sk89q.biomeatlas;

import com.sk89q.biomeatlas.command.CommandBiomeAtlas;
import com.sk89q.biomeatlas.config.BiomeColorConfig;
import com.sk89q.biomeatlas.config.BiomeMainConfig;
import com.sk89q.biomeatlas.handler.ServerTickHandler;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Locale;

@Mod(modid = BiomeAtlas.MOD_ID, version = BiomeAtlas.MOD_VERSION, name = BiomeAtlas.MOD_NAME, dependencies = "required-after:Forge@[12.18.1.2039,)", acceptableRemoteVersions = "*")
public class BiomeAtlas
{

	public static final String MOD_NAME = "Biome Atlas";
	public static final String MOD_ID = "biomeatlas";
	public static final String MOD_VERSION = "@MOD_VERSION@";

	public static Logger logger = LogManager.getLogger(MOD_ID);

	@Mod.Instance(MOD_ID)
	public static BiomeAtlas instance;

	public static MinecraftServer getServerInstance()
	{
		return FMLCommonHandler.instance().getMinecraftServerInstance();
	}

	private File _configDirectory;
	private BiomeMapper _mapper;

	public BiomeMapper getMapper()
	{
		return _mapper;
	}

	@EventHandler
	public void preInit(FMLPreInitializationEvent event)
	{
		_configDirectory = new File(event.getModConfigurationDirectory(), "biomeatlas");

		Locale.setDefault(Locale.ENGLISH);
		BiomeMainConfig.init(new File(_configDirectory, "biome.cfg"));
		BiomeColorConfig.init(new File(_configDirectory, "biomecolor.cfg"));

		MinecraftForge.EVENT_BUS.register(new BiomeMainConfig());
		MinecraftForge.EVENT_BUS.register(new ServerTickHandler());
	}

	@EventHandler
	public void serverStarting(FMLServerStartingEvent evt)
	{
		evt.registerServerCommand(new CommandBiomeAtlas());
	}

	@EventHandler
	public void serverStarted(FMLServerStartedEvent evt)
	{
		_mapper = new BiomeMapper(
				getServerInstance().worldServerForDimension(Integer.parseInt(System.getProperty("biomeAtlas.mapDimension", "0"))), //WorldServer
				Integer.parseInt(System.getProperty("biomeAtlas.centerX", "0")), //CenterX
				Integer.parseInt(System.getProperty("biomeAtlas.centerZ", "0")), //CenterZ
				Integer.parseInt(System.getProperty("biomeAtlas.apothem", "250")), //Apothem
				Integer.parseInt(System.getProperty("biomeAtlas.resolution", "16"))); //resolution

		if (System.getProperty("biomeAtlas.mapOnStartup", "false").equals("true"))
		{
			_mapper.startGeneration(System.getProperty("biomeAtlas.exitOnFinish", "false").equals("true"));
		}
	}

	public static void sendMessage(String msg, TextFormatting color)
	{
		TextComponentString message = new TextComponentString(msg);
		message.getStyle().setColor(color);
		getServerInstance().addChatMessage(message);
		logger.info(message);

		if(BiomeMainConfig.BroadcastPlayers)
		{
			for (EntityPlayerMP player : getServerInstance().getPlayerList().getPlayerList())
			{
				player.addChatMessage(message);
			}
		}
	}
}
