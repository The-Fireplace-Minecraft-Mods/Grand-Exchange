package the_fireplace.grandexchange;

import net.minecraft.command.ICommandManager;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import the_fireplace.grandexchange.commands.CommandGe;
import the_fireplace.grandexchange.compat.ClansTaxDistributor;
import the_fireplace.grandexchange.compat.DummyTaxDistributor;
import the_fireplace.grandexchange.compat.TaxDistributor;
import the_fireplace.grandexchange.db.IDatabaseHandler;
import the_fireplace.grandexchange.db.JsonDatabase;
import the_fireplace.grandexchange.permission.ForgePermissionHandler;
import the_fireplace.grandexchange.permission.IPermissionHandler;
import the_fireplace.grandexchange.permission.SpongePermissionHandler;

@SuppressWarnings("WeakerAccess")
@Mod(modid = GrandExchange.MODID, name = GrandExchange.MODNAME, version = GrandExchange.VERSION, acceptedMinecraftVersions = "[1.12,1.13)", acceptableRemoteVersions = "*", dependencies="required-after:grandeconomy@[2.0.0,);after:clans")
public final class GrandExchange {
    public static final String MODID = "grandexchange";
    public static final String MODNAME = "Grand Exchange";
    public static final String VERSION = "${version}";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    @Mod.Instance(MODID)
    public static GrandExchange instance;

    private static IDatabaseHandler db = null;
    public static IDatabaseHandler getDatabase() {
        if(db == null)//TODO Check config for database type once implemented
            db = new JsonDatabase();
        return db;
    }

    private static TaxDistributor taxDistributor;
    public static TaxDistributor getTaxDistributor() {
        return taxDistributor;
    }
    private static IPermissionHandler permissionHandler;
    public static IPermissionHandler getPermissionHandler() {
        return permissionHandler;
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if(Loader.isModLoaded("clans") && cfg.taxToClans)
            taxDistributor = new ClansTaxDistributor();
        else
            taxDistributor = new DummyTaxDistributor();
        if(Loader.isModLoaded("spongeapi") && !cfg.forgePermissionPrecedence)
            permissionHandler = new SpongePermissionHandler();
        else
            permissionHandler = new ForgePermissionHandler();
    }

    @Mod.EventHandler
    public void onServerStart(FMLServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        ICommandManager command = server.getCommandManager();
        ServerCommandManager manager = (ServerCommandManager) command;
        manager.registerCommand(new CommandGe());
    }

    @Mod.EventHandler
    public void onServerStop(FMLServerStoppingEvent event) {
        getDatabase().manualSave();
    }

    @Config(modid=MODID)
    public static class cfg {
        @Config.Comment("Server locale - the client's locale takes precedence if Grand Exchange is installed there.")
        public static String locale = "en_us";
        @Config.Comment("Flat tax for selling items on the Grand Exchange. This tax is non-refundable and taken up front, to prevent players from using the Grand Exchange as free storage.")
        @Config.RangeDouble(min = 0)
        public static double flatTax = 0;
        @Config.Comment("Percentage tax for selling items on the Grand Exchange. This tax is non-refundable and taken up front, to prevent players from using the Grand Exchange as free storage.")
        @Config.RangeDouble(min = 0, max = 100)
        public static double percentTax = 3;
        @Config.Comment("Should the tax be contributed to the player's clans' balances if Clans is loaded?")
        public static boolean taxToClans = true;
        @Config.Comment("Whether Forge takes precedence over Sponge when finding permissions. Set this to true if your permissions manager uses Forge.")
        public static boolean forgePermissionPrecedence = false;
    }
}
