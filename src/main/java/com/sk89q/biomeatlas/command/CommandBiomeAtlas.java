package com.sk89q.biomeatlas.command;

import com.google.common.base.Predicate;
import com.sk89q.biomeatlas.BiomeAtlas;
import com.sk89q.biomeatlas.BiomeMapper;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.io.File;
import java.util.List;

public class CommandBiomeAtlas extends CommandBase {

    public CommandBiomeAtlas()
    {

    }

    @Override
    public String getCommandName()
    {
        return "biomeatlas";
    }

    @Override
    public String getCommandUsage(ICommandSender sender)
    {
        return "commands.biomeatlas.usage";
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 4;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if (args.length >= 1 && args.length <= 2)
        {
            //BiomeAtlas.serverInstance = server;
            EntityPlayerMP player = getCommandSenderAsPlayer(sender);
            int apothem;
            int resolution = 16;

            try
            {
                apothem = Integer.parseInt(args[0]);
                if (apothem < 0)
                {
                    throw new WrongUsageException("Apothem should be >= 1");
                }
            } catch (NumberFormatException e)
            {
                throw new WrongUsageException(this.getCommandUsage(sender));
            }

            if (args.length >= 2)
            {
                try
                {
                    resolution = Integer.parseInt(args[1]);
                    if (resolution < 1)
                    {
                        throw new WrongUsageException("Resolution should be >= 1");
                    }
                }
                catch (NumberFormatException e)
                {
                    throw new WrongUsageException(this.getCommandUsage(sender));
                }
            }

            //World world = player.getEntityWorld();
            int centerX = (int) player.posX;
            int centerZ = (int) player.posZ;

            BiomeAtlas.instance.getMapper().setData(BiomeAtlas.getServerInstance().worldServerForDimension(player.dimension), centerX, centerZ, apothem, resolution);
            BiomeAtlas.instance.getMapper().startGeneration();

           // mapper.setResolution(resolution);
            //mapper.setMessageRate(1000);
            //mapper.getListeners().add(new BroadcastObserver(server));
            //mapper.generate(world, sender, centerX, centerZ, apothem, 1000);
        }
        else
        {
            throw new WrongUsageException(this.getCommandUsage(sender));
        }
    }

    @Override
    public List<String> getTabCompletionOptions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos)
    {
        return null;
    }

    /*private static class BroadcastObserver implements Predicate<String>
    {
        private MinecraftServer _server = null;

        public BroadcastObserver(MinecraftServer server)
        {
            _server = server;
        }

        @Override
        public boolean apply(String input)
        {
            TextComponentString message = new TextComponentString(input);
            message.getStyle().setColor(TextFormatting.DARK_GRAY);
            _server.addChatMessage(message);
            BiomeAtlas.logger.info(message);

            for (EntityPlayerMP player : _server.getPlayerList().getPlayerList())
            {
                player.addChatMessage(message);
            }

            return false;
        }
    }*/
}
