package the_fireplace.grandexchange.events;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import the_fireplace.grandexchange.GrandExchange;

@Mod.EventBusSubscriber(value = Side.CLIENT, modid = GrandExchange.MODID)
public class ClientEvents {
    @SubscribeEvent
    public static void configChanged(ConfigChangedEvent event) {
        if (event.getModID().equals(GrandExchange.MODID))
            ConfigManager.sync(GrandExchange.MODID, Config.Type.INSTANCE);
    }
}
