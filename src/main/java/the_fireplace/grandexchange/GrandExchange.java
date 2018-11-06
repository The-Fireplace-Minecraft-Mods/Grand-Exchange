package the_fireplace.grandexchange;

import net.minecraft.command.ICommandManager;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import the_fireplace.grandexchange.commands.CommandCollect;
import the_fireplace.grandexchange.commands.CommandIdentify;
import the_fireplace.grandexchange.commands.CommandBuy;
import the_fireplace.grandexchange.commands.CommandSell;

@Mod(modid = GrandExchange.MODID, name = GrandExchange.MODNAME, version = GrandExchange.VERSION, acceptedMinecraftVersions = "[1.12,1.13)", serverSideOnly = true, acceptableRemoteVersions = "*")
public class GrandExchange {
    public static final String MODID = "grandexchange";
    public static final String MODNAME = "Grand Exchange";
    public static final String VERSION = "${version}";
    @Mod.Instance(MODID)
    @SuppressWarnings("unused")
    public static GrandExchange instance;

    @Mod.EventHandler
    @SuppressWarnings("unused")
    public void onServerStart(FMLServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        ICommandManager command = server.getCommandManager();
        ServerCommandManager manager = (ServerCommandManager) command;
        manager.registerCommand(new CommandBuy());
        manager.registerCommand(new CommandSell());
        manager.registerCommand(new CommandIdentify());
        manager.registerCommand(new CommandCollect());
    }
}
