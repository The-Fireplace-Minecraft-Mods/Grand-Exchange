package the_fireplace.grandexchange;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.SaveHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.File;

@Mod(GrandExchange.MODID)
public final class GrandExchange {
    public static final String MODID = "grandexchange";
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    public static MinecraftServer server;
    public static File saveRoot;

    public GrandExchange() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStart(FMLServerStartingEvent event) {
        server = event.getServer();
        saveRoot = getWorldDir(server.getWorld(DimensionType.OVERWORLD));
        GeCommands.register(event.getCommandDispatcher());
    }

    @Nullable
    private File getWorldDir(World world) {
        ISaveHandler handler = world.getSaveHandler();
        if (!(handler instanceof SaveHandler))
            return null;
        return handler.getWorldDirectory();
    }
}
