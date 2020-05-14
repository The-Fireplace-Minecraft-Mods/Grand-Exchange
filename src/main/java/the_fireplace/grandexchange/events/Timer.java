package the_fireplace.grandexchange.events;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import the_fireplace.grandexchange.GrandExchange;

@Mod.EventBusSubscriber(modid = GrandExchange.MODID)
public class Timer {
    private static int fiveMinuteCounter = 0;
    private static boolean executing = false;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if(!executing) {
            if(++fiveMinuteCounter >= 20*60*5) {
                executing = true;
                fiveMinuteCounter -= 20*60*5;
                GrandExchange.getDatabase().manualSave();
                executing = false;
            }
        }
    }
}
