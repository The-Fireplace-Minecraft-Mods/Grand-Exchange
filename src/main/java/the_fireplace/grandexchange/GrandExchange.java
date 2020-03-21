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

@SuppressWarnings("WeakerAccess")
@Mod(modid = GrandExchange.MODID, name = GrandExchange.MODNAME, version = GrandExchange.VERSION, acceptedMinecraftVersions = "[1.12,1.13)", acceptableRemoteVersions = "*", dependencies="required-after:grandeconomy@[1.3.1,);after:clans")
public final class GrandExchange {
    public static final String MODID = "grandexchange";
    public static final String MODNAME = "Grand Exchange";
    public static final String VERSION = "${version}";
    public static final Logger LOGGER = LogManager.getLogger(MODID);
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

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if(Loader.isModLoaded("clans") && cfg.taxToClans)
            taxDistributor = new ClansTaxDistributor();
        else
            taxDistributor = new DummyTaxDistributor();
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
        getDatabase().onServerStop();
    }

    @Config(modid=MODID)
    public static class cfg {
        @Config.Comment("Server locale - the client's locale takes precedence if Grand Exchange is installed there.")
        public static String locale = "en_us";
        @Config.Comment("Tax for selling items on the Grand Exchange. This tax is non-refundable and taken up front, to prevent players from using the Grand Exchange as free storage. Negative numbers indicate a flat tax, positive numbers ingicate a percent of the total price.")
        public static int sellingTax = 3;
        @Config.Comment("Should the tax be contributed to the player's clans' balances if Clans is loaded?")
        public static boolean taxToClans = true;
    }
}
