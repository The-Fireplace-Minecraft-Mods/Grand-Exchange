package the_fireplace.grandexchange;

import net.minecraft.network.NetHandlerPlayServer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;
import the_fireplace.grandexchange.util.translation.TranslationUtil;

import java.util.Map;

@Mod.EventBusSubscriber(modid= GrandExchange.MODID)
public class NetworkEvents {

    @SubscribeEvent
    public static void clientConnectToServer(FMLNetworkEvent.ServerConnectionFromClientEvent event) {
        Map<String, String> clientMods = NetworkDispatcher.get(event.getManager()).getModList();
        if(event.getHandler() instanceof NetHandlerPlayServer && ((NetHandlerPlayServer) event.getHandler()).player != null && clientMods.containsKey("grandexchange") && !clientMods.get("grandexchange").startsWith("1.0.") && !clientMods.get("grandexchange").startsWith("1.1."))
            TranslationUtil.grandExchangeClients.add(((NetHandlerPlayServer) event.getHandler()).player.getUniqueID());
    }
}
