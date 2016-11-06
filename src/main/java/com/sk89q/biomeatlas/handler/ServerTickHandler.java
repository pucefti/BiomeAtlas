package com.sk89q.biomeatlas.handler;

import com.sk89q.biomeatlas.BiomeAtlas;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Created by Ben on 05/11/2016.
 */
public class ServerTickHandler
{
	private final int nbBlock = 10;

	@SubscribeEvent
	public void onServerTick(TickEvent.ServerTickEvent event)
	{
		if(BiomeAtlas.instance.getMapper() != null && BiomeAtlas.instance.getMapper().isGenerationStarted() && !BiomeAtlas.instance.getMapper().isPendingGenerationExist())
		{
			try
			{
				BiomeAtlas.instance.getMapper().analyzeRegion(nbBlock);
				BiomeAtlas.sendMessage(String.format("BiomeAtlas generation: %s (%s)", BiomeAtlas.instance.getMapper().getProgress(), BiomeAtlas.instance.getMapper().getProgressInPercent()), TextFormatting.DARK_GRAY);

				if (BiomeAtlas.instance.getMapper().isAnalyzaCompleted())
				{
					BiomeAtlas.sendMessage("BiomeAtlas generation: Creating file...", TextFormatting.DARK_GRAY);
					BiomeAtlas.instance.getMapper().generateFile();
					BiomeAtlas.sendMessage("BiomeAtlas generation: done", TextFormatting.DARK_GRAY);

					BiomeAtlas.instance.getMapper().stopGeneration();
				}
			}
			catch (Exception ex)
			{
				BiomeAtlas.sendMessage(ex.getMessage(), TextFormatting.RED);
			}
		}
	}
}
