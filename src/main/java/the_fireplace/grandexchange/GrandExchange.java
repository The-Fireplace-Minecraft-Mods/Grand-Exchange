package the_fireplace.grandexchange;

import net.minecraft.command.ICommandManager;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import the_fireplace.grandexchange.commands.CommandGe;
import the_fireplace.grandexchange.util.TransactionDatabase;

@SuppressWarnings("WeakerAccess")
@Mod(modid = GrandExchange.MODID, name = GrandExchange.MODNAME, version = GrandExchange.VERSION, acceptedMinecraftVersions = "[1.12,1.13)", acceptableRemoteVersions = "*", dependencies="required-after:grandeconomy@[1.3.1,)")
public final class GrandExchange {
    public static final String MODID = "grandexchange";
    public static final String MODNAME = "Grand Exchange";
    public static final String VERSION = "${version}";

    @Mod.EventHandler
    public void onServerStart(FMLServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        ICommandManager command = server.getCommandManager();
        ServerCommandManager manager = (ServerCommandManager) command;
        manager.registerCommand(new CommandGe());
        //TODO Remove this old code when porting to 1.14+
        TransactionDatabase.getInstance();
    }

    @Config(modid=MODID, name=MODNAME)
    public static class cfg {
        @Config.Comment("Server locale - the client's locale takes precedence if Grand Exchange is installed there.")
        public static String locale = "en_us";
    }
}
