package the_fireplace.grandexchange.market;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import the_fireplace.grandexchange.util.TextStyles;
import the_fireplace.grandexchange.util.translation.TranslationUtil;

import javax.annotation.Nullable;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber
public class OfferStatusMessager {
    private static File offerStatusFile;
    private static boolean isChanged;

    private static Map<UUID, List<Long>> partialOfferStatusMessages = Maps.newHashMap();
    private static Map<UUID, List<MessageObj>> completeOfferStatusMessages = Maps.newHashMap();

    private static void markChanged() {
        if(!isChanged)
            isChanged = true;
    }

    public static String getFormatted(String item, int meta) {
        return String.format("%s:%s", item, meta);
    }

    public static void updateStatusPartial(UUID player, long offerId) {
        partialOfferStatusMessages.putIfAbsent(player, Lists.newArrayList());
        partialOfferStatusMessages.get(player).add(offerId);
        markChanged();
    }

    public static void updateStatusComplete(UUID player, long offerId, String message, int amount, String name, long price, @Nullable String nbt) {
        if(partialOfferStatusMessages.containsKey(player))
            partialOfferStatusMessages.get(player).remove(offerId);
        completeOfferStatusMessages.putIfAbsent(player, Lists.newArrayList());
        completeOfferStatusMessages.get(player).add(new MessageObj(offerId, message, amount, name, price, nbt));
        markChanged();
    }

    public static void sendStatusUpdates(EntityPlayerMP player) {
        if(!partialOfferStatusMessages.containsKey(player.getUniqueID()) && !completeOfferStatusMessages.containsKey(player.getUniqueID()))
            return;
        if(partialOfferStatusMessages.containsKey(player.getUniqueID()))
            for(long offerId: partialOfferStatusMessages.remove(player.getUniqueID())) {
                NewOffer offer = ExchangeManager.getOffer(offerId);
                if(offer.getNbt() == null)
                    player.sendMessage(TranslationUtil.getTranslation(player.getUniqueID(), "ge."+offer.getType().toString().toLowerCase()+"offer.fulfilled_partial", offer.getOriginalAmount()-offer.getAmount(), offer.getOriginalAmount(), OfferStatusMessager.getFormatted(offer.getItemResourceName(), offer.getItemMeta())).setStyle(TextStyles.BLUE));
                else
                    player.sendMessage(TranslationUtil.getTranslation(player.getUniqueID(), "ge."+offer.getType().toString().toLowerCase()+"offer.fulfilled_partial_nbt", offer.getOriginalAmount()-offer.getAmount(), offer.getOriginalAmount(), OfferStatusMessager.getFormatted(offer.getItemResourceName(), offer.getItemMeta()), offer.getNbt()).setStyle(TextStyles.BLUE));
                markChanged();
            }
        if(completeOfferStatusMessages.containsKey(player.getUniqueID()))
            for(MessageObj message: completeOfferStatusMessages.remove(player.getUniqueID())) {
                if(message.nbt == null)
                    player.sendMessage(TranslationUtil.getTranslation(player.getUniqueID(), message.message, message.amount, message.name, message.price).setStyle(TextStyles.BLUE));
                else
                    player.sendMessage(TranslationUtil.getTranslation(player.getUniqueID(), message.message, message.amount, message.name, message.price, message.nbt).setStyle(TextStyles.BLUE));
                markChanged();
            }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {//Send updates once every minute
        if(event.player.world.isRemote || event.player.ticksExisted % (20 * 60) > 0 || !(event.player instanceof EntityPlayerMP))
            return;
        sendStatusUpdates((EntityPlayerMP)event.player);
    }

    public static void save() {

    }

    public static void load() {

    }

    private static class MessageObj {
        private String message, name;
        private int amount;
        private long offerId, price;
        @Nullable
        private String nbt;
        private MessageObj(long offerId, String message, int amount, String name, long price, @Nullable String nbt) {
            this.offerId = offerId;
            this.message = message;
            this.amount = amount;
            this.name = name;
            this.price = price;
            this.nbt = nbt;
        }
        private MessageObj(JsonObject obj) {
            message = obj.get("message").getAsString();
            name = obj.get("name").getAsString();
            amount = obj.get("amount").getAsInt();
            offerId = obj.get("offerId").getAsLong();
            price = obj.get("price").getAsLong();
            nbt = obj.has("nbt") ? obj.get("nbt").getAsString() : null;
        }

        private JsonObject toJson() {
            JsonObject obj = new JsonObject();

            obj.addProperty("message", message);
            obj.addProperty("name", name);
            obj.addProperty("amount", amount);
            obj.addProperty("offerId", offerId);
            obj.addProperty("price", price);
            if(nbt != null)
                obj.addProperty("nbt", nbt);

            return obj;
        }
    }
}
