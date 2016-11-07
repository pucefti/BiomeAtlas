package com.sk89q.biomeatlas.handler;

import com.sk89q.biomeatlas.BiomeAtlas;
import com.sk89q.biomeatlas.config.BiomeMainConfig;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Created by Ben on 05/11/2016.
 * Tick event handler
 */
public class ServerTickHandler
{
	private int _totalCompletedBlocks;
	private long _totalTimeFroCompletion;

	public ServerTickHandler()
	{
		_totalCompletedBlocks = 0;
	}

	@SubscribeEvent
	public void onServerTick(TickEvent.ServerTickEvent event)
	{
		if(BiomeAtlas.instance.getMapper() != null && BiomeAtlas.instance.getMapper().isGenerationStarted() && !BiomeAtlas.instance.getMapper().isPendingGenerationExist())
		{
			try
			{
				long start =  System.currentTimeMillis();
				_totalCompletedBlocks += BiomeAtlas.instance.getMapper().analyzeRegion(BiomeMainConfig.NbBlockTick);
				BiomeAtlas.sendMessage(String.format("BiomeAtlas Analyze: %s (%s)", getProgress(), getProgressInPercent()), TextFormatting.DARK_GRAY);
				_totalTimeFroCompletion += System.currentTimeMillis() - start;

				if (BiomeAtlas.instance.getMapper().isAnalyzaCompleted())
				{
					BiomeAtlas.sendMessage(String.format("BiomeAtlas Analyze terminated in %dms.", _totalTimeFroCompletion), TextFormatting.DARK_GRAY);
					BiomeAtlas.sendMessage("BiomeAtlas generation: Creating file...", TextFormatting.DARK_GRAY);
					BiomeAtlas.instance.getMapper().generateFile();
					BiomeAtlas.sendMessage("BiomeAtlas generation: done", TextFormatting.DARK_GRAY);

					BiomeAtlas.instance.getMapper().stopGeneration();

					if(BiomeAtlas.instance.getMapper().stopOnCompletion())
					{
						BiomeAtlas.getServerInstance().initiateShutdown();
					}
				}
			}
			catch (Exception ex)
			{
				BiomeAtlas.sendMessage(ex.getMessage(), TextFormatting.RED);
			}
		}
	}

	private String getProgress()
	{
		return String.format("%d/%d", _totalCompletedBlocks, BiomeAtlas.instance.getMapper().getTotalBlock());
	}

	private String getProgressInPercent()
	{
		return String.format("%.2f%%", (_totalCompletedBlocks / (double) BiomeAtlas.instance.getMapper().getTotalBlock() * 100));
	}
}
