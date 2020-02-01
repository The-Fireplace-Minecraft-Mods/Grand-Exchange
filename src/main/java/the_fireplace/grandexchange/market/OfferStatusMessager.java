package the_fireplace.grandexchange.market;

import com.google.gson.JsonObject;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import the_fireplace.grandexchange.util.TextStyles;
import the_fireplace.grandexchange.util.translation.TranslationUtil;

import javax.annotation.Nullable;
import java.util.UUID;

import static the_fireplace.grandexchange.GrandExchange.getDatabase;

@Mod.EventBusSubscriber
public class OfferStatusMessager {

    public static String getFormatted(String item, int meta) {
        return String.format("%s:%s", item, meta);
    }

    public static void updateStatusPartial(UUID player, long offerId) {
        //TODO immediately push to player if they are online
        getDatabase().updateOfferStatusPartial(player, offerId);
    }

    public static void updateStatusComplete(UUID player, long offerId, String message, int amount, String name, long price, @Nullable String nbt) {
        getDatabase().removeOfferStatusPartial(player, offerId);
        //TODO immediately push to player if they are online
        getDatabase().updateOfferStatusComplete(player, offerId, message, amount, name, price, nbt);
    }

    public static void sendStatusUpdates(EntityPlayerMP player) {
        boolean hasPartialOfferUpdates = getDatabase().hasPartialOfferUpdates(player.getUniqueID());
        boolean hasCompleteOfferUpdates = getDatabase().hasCompleteOfferUpdates(player.getUniqueID());
        if(!hasPartialOfferUpdates && !hasCompleteOfferUpdates)
            return;
        if(hasPartialOfferUpdates)
            for(long offerId: getDatabase().getPartialOfferUpdates(player.getUniqueID()))
                sendPartialStatusUpdate(player, offerId);
        if(hasCompleteOfferUpdates)
            for(MessageObj message: getDatabase().getCompleteOfferUpdates(player.getUniqueID()))
                sendCompleteStatusUpdate(player, message);
    }

    private static void sendCompleteStatusUpdate(EntityPlayerMP player, MessageObj message) {
        if(message.getNbt() == null)
            player.sendMessage(TranslationUtil.getTranslation(player.getUniqueID(), message.getMessage(), message.getAmount(), message.getName(), message.getPrice()).setStyle(TextStyles.BLUE));
        else
            player.sendMessage(TranslationUtil.getTranslation(player.getUniqueID(), message.getMessage(), message.getAmount(), message.getName(), message.getPrice(), message.getNbt()).setStyle(TextStyles.BLUE));
        getDatabase().removeOfferStatusComplete(player.getUniqueID(), message.offerId);
    }

    private static void sendPartialStatusUpdate(EntityPlayerMP player, long offerId) {
        NewOffer offer = ExchangeManager.getOffer(offerId);
        if(offer.getNbt() == null)
            player.sendMessage(TranslationUtil.getTranslation(player.getUniqueID(), "ge."+offer.getType().toString().toLowerCase()+"offer.fulfilled_partial", offer.getOriginalAmount()-offer.getAmount(), offer.getOriginalAmount(), OfferStatusMessager.getFormatted(offer.getItemResourceName(), offer.getItemMeta())).setStyle(TextStyles.BLUE));
        else
            player.sendMessage(TranslationUtil.getTranslation(player.getUniqueID(), "ge."+offer.getType().toString().toLowerCase()+"offer.fulfilled_partial_nbt", offer.getOriginalAmount()-offer.getAmount(), offer.getOriginalAmount(), OfferStatusMessager.getFormatted(offer.getItemResourceName(), offer.getItemMeta()), offer.getNbt()).setStyle(TextStyles.BLUE));
        getDatabase().removeOfferStatusPartial(player.getUniqueID(), offerId);
    }

    //TODO replace this with something that sends the update when they log in
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {//Send updates once every minute
        if(event.player.world.isRemote || event.player.ticksExisted % (20 * 60) > 0 || !(event.player instanceof EntityPlayerMP))
            return;
        sendStatusUpdates((EntityPlayerMP)event.player);
    }

    public static class MessageObj {
        private String message;
        private String name;
        private int amount;
        private long offerId;
        private long price;
        @Nullable
        private String nbt;

        public MessageObj(long offerId, String message, int amount, String name, long price, @Nullable String nbt) {
            this.offerId = offerId;
            this.message = message;
            this.amount = amount;
            this.name = name;
            this.price = price;
            this.nbt = nbt;
        }

        public MessageObj(JsonObject obj) {
            message = obj.get("message").getAsString();
            name = obj.get("name").getAsString();
            amount = obj.get("amount").getAsInt();
            offerId = obj.get("offerId").getAsLong();
            price = obj.get("price").getAsLong();
            nbt = obj.has("nbt") ? obj.get("nbt").getAsString() : null;
        }

        public JsonObject toJson() {
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

        public String getMessage() {
            return message;
        }

        public String getName() {
            return name;
        }

        public int getAmount() {
            return amount;
        }

        public long getOfferId() {
            return offerId;
        }

        public long getPrice() {
            return price;
        }

        @Nullable
        public String getNbt() {
            return nbt;
        }
    }
}
