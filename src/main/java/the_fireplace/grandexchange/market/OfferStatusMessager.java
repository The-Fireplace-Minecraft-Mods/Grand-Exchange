package the_fireplace.grandexchange.market;

import com.google.common.collect.Maps;
import net.minecraft.entity.player.EntityPlayerMP;
import org.apache.commons.lang3.tuple.Pair;
import the_fireplace.grandexchange.util.TextStyles;
import the_fireplace.grandexchange.util.translation.TranslationUtil;

import java.util.Map;
import java.util.UUID;

public class OfferStatusMessager {
    private static Map<UUID, Map<Long, Pair<String, Object[]>>> offerStatusMessages = Maps.newHashMap();

    public static String getFormatted(String item, int meta) {
        return String.format("%s:%s", item, meta);
    }

    public static void updateStatus(UUID player, long offerId, String unlocalizedMessage, Object... args) {
        offerStatusMessages.putIfAbsent(player, Maps.newHashMap());
        offerStatusMessages.get(player).put(offerId, Pair.of(unlocalizedMessage, args));
    }

    public static void sendStatusUpdates(EntityPlayerMP player) {
        if(!offerStatusMessages.containsKey(player.getUniqueID()))
            return;
        for(Map.Entry<Long, Pair<String, Object[]>> entry: offerStatusMessages.remove(player.getUniqueID()).entrySet()) {
            player.sendMessage(TranslationUtil.getTranslation(player.getUniqueID(), entry.getValue().getLeft(), entry.getValue().getRight()).setStyle(TextStyles.BLUE));
        }
    }

    //TODO save and load
}
