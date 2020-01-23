package the_fireplace.grandexchange.market;

import com.google.gson.JsonObject;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.text.ITextComponent;
import the_fireplace.grandeconomy.api.GrandEconomyApi;
import the_fireplace.grandexchange.util.TextStyles;
import the_fireplace.grandexchange.util.translation.TranslationUtil;

import javax.annotation.Nullable;
import java.util.UUID;

public class NewOffer extends Offer {
    private long identifier;
    private OfferType type;
    protected NewOffer(long id, String offertype, String item, int meta, int amount, long price, UUID owner, @Nullable String nbt) {
        super(offertype, item, meta, amount, price, owner, nbt);
        identifier = id;
        type = offertype.equals("buy") ? OfferType.BUY : OfferType.SELL;
    }

    @Override
    public ITextComponent getOfferChatMessage(ICommandSender sender) {
        if(isBuyOffer()) {
            if(getNbt() != null)
                return TranslationUtil.getTranslation(sender, "ge.buyoffer_nbt", getAmount(), getItemResourceName(), getItemMeta(), getNbt(), GrandEconomyApi.toString(getPrice())).setStyle(TextStyles.BLUE);
            else
                return TranslationUtil.getTranslation(sender, "ge.buyoffer", getAmount(), getItemResourceName(), getItemMeta(), GrandEconomyApi.toString(getPrice())).setStyle(TextStyles.BLUE);
        } else if(isSellOffer()) {
            if(getNbt() != null)
                return TranslationUtil.getTranslation(sender, "ge.selloffer_nbt", getAmount(), getItemResourceName(), getItemMeta(), getNbt(), GrandEconomyApi.toString(getPrice())).setStyle(TextStyles.DARK_PURPLE);
            else
                return TranslationUtil.getTranslation(sender, "ge.selloffer", getAmount(), getItemResourceName(), getItemMeta(), GrandEconomyApi.toString(getPrice())).setStyle(TextStyles.DARK_PURPLE);
        } else {
            if(getNbt() != null)
                return TranslationUtil.getTranslation(sender, "ge.invalidoffer_nbt", getAmount(), getItemResourceName(), getItemMeta(), getNbt(), GrandEconomyApi.toString(getPrice())).setStyle(TextStyles.RED);
            else
                return TranslationUtil.getTranslation(sender, "ge.invalidoffer", getAmount(), getItemResourceName(), getItemMeta(), GrandEconomyApi.toString(getPrice())).setStyle(TextStyles.RED);
        }
    }

    public NewOffer(JsonObject object) {
        super(object.get("offertype").getAsString(), object.get("item").getAsString(), object.get("meta").getAsInt(), object.get("amount").getAsInt(), object.get("price").getAsInt(), UUID.fromString(object.get("owner").getAsString()), object.has("nbt") ? object.get("nbt").getAsString() : null);
        identifier = object.has("identifier") ? object.get("identifier").getAsLong() : ((JsonTransactionDatabase)ExchangeManager.getDatabase()).getNewIdentifier();//TODO This usage of getNewIdentifier isn't long term, they will all have identifier by the time I update to have other database types and this will be removed then
        type = object.get("offertype").getAsString().equals("buy") ? OfferType.BUY : OfferType.SELL;
    }

    public boolean isBuyOffer() {
        return type.equals(OfferType.BUY);
    }

    public boolean isSellOffer() {
        return type.equals(OfferType.SELL);
    }

    public OfferType getType() {
        return type;
    }

    public long getIdentifier() {
        return identifier;
    }

    @Override
    public JsonObject toJsonObject() {
        JsonObject ret = super.toJsonObject();
        ret.addProperty("identifier", identifier);
        return ret;
    }
}