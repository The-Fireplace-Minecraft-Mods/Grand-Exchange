package the_fireplace.grandexchange.market;

import com.google.gson.JsonObject;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.Pair;
import the_fireplace.grandeconomy.api.GrandEconomyApi;
import the_fireplace.grandexchange.util.TextStyles;
import the_fireplace.grandexchange.util.translation.TranslationUtil;

import javax.annotation.Nullable;
import java.util.UUID;

public class Offer {
    protected String item;
    @Nullable
    protected String nbt;
    @Nullable
    protected Integer amount, originalAmount;
    protected int meta;
    protected double price;
    protected long identifier;
    @Nullable
    protected UUID owner;
    protected long timestamp = System.currentTimeMillis();
    private final OfferType type;

    public Offer(long id, OfferType offertype, String item, int meta, @Nullable Integer amount, double price, @Nullable UUID owner, @Nullable String nbt) {
        this.item = item;
        this.meta = meta;
        this.amount = amount;
        this.price = price;
        this.owner = owner;
        this.nbt = nbt;
        this.identifier = id;
        this.originalAmount = amount;
        this.type = offertype;
    }

    public ITextComponent getOfferChatMessage(ICommandSender sender) {
        if(isBuyOffer()) {
            if(getNbt() != null)
                return TranslationUtil.getTranslation(sender, "ge.buyoffer_nbt", getIdentifier(), getAmount() != null ? getAmount() : "\u221E", OfferStatusMessager.getFormatted(getItemResourceName(), getItemMeta()), getNbt(), GrandEconomyApi.getFormattedCurrency(getPrice())).setStyle(TextStyles.BLUE);
            else
                return TranslationUtil.getTranslation(sender, "ge.buyoffer", getIdentifier(), getAmount() != null ? getAmount() : "\u221E", OfferStatusMessager.getFormatted(getItemResourceName(), getItemMeta()), GrandEconomyApi.getFormattedCurrency(getPrice())).setStyle(TextStyles.BLUE);
        } else if(isSellOffer()) {
            if(getNbt() != null)
                return TranslationUtil.getTranslation(sender, "ge.selloffer_nbt", getIdentifier(), getAmount() != null ? getAmount() : "\u221E", OfferStatusMessager.getFormatted(getItemResourceName(), getItemMeta()), getNbt(), GrandEconomyApi.getFormattedCurrency(getPrice())).setStyle(TextStyles.DARK_PURPLE);
            else
                return TranslationUtil.getTranslation(sender, "ge.selloffer", getIdentifier(), getAmount() != null ? getAmount() : "\u221E", OfferStatusMessager.getFormatted(getItemResourceName(), getItemMeta()), GrandEconomyApi.getFormattedCurrency(getPrice())).setStyle(TextStyles.DARK_PURPLE);
        } else {
            if(getNbt() != null)
                return TranslationUtil.getTranslation(sender, "ge.invalidoffer_nbt", getAmount() != null ? getAmount() : "\u221E", OfferStatusMessager.getFormatted(getItemResourceName(), getItemMeta()), getNbt(), GrandEconomyApi.getFormattedCurrency(getPrice())).setStyle(TextStyles.RED);
            else
                return TranslationUtil.getTranslation(sender, "ge.invalidoffer", getAmount() != null ? getAmount() : "\u221E", OfferStatusMessager.getFormatted(getItemResourceName(), getItemMeta()), GrandEconomyApi.getFormattedCurrency(getPrice())).setStyle(TextStyles.RED);
        }
    }

    public Offer(JsonObject object) {
        item = object.get("item").getAsString();
        meta = object.get("meta").getAsInt();
        amount = object.has("amount") ? object.get("amount").getAsInt() : null;
        price = object.get("price").getAsInt();
        owner = object.has("owner") ? UUID.fromString(object.get("owner").getAsString()) : null;
        nbt = object.has("nbt") ? object.get("nbt").getAsString() : null;
        identifier = object.get("identifier").getAsLong();
        type = object.get("type").getAsString().equals("buy") ? OfferType.BUY : OfferType.SELL;
        originalAmount = object.has("original_amount") ? object.get("original_amount").getAsInt() : null;
    }

    public final Pair<String, Integer> getItemPair(){
        return Pair.of(item, meta);
    }
    public final String getItemResourceName(){
        return item;
    }
    @Nullable
    public final Integer getAmount(){
        return amount;
    }
    public final int getItemMeta(){
        return meta;
    }
    public final double getPrice(){
        return price;
    }
    @Nullable
    public final UUID getOwner(){
        return owner;
    }
    public final long getTimestamp(){
        return timestamp;
    }
    @Nullable
    public final String getNbt(){
        return nbt;
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

    @Nullable
    public Integer getOriginalAmount() {
        return originalAmount;
    }

    public void setAmount(int newAmount) {
        this.amount = newAmount;
    }

    public Offer copy() {
        //TODO do this more efficiently
        return new Offer(toJsonObject());
    }

    public JsonObject toJsonObject() {
        JsonObject ret = new JsonObject();
        ret.addProperty("item", item);
        ret.addProperty("type", type.toString().toLowerCase());
        if(nbt != null)
            ret.addProperty("nbt", nbt);
        if(amount != null)
            ret.addProperty("amount", amount);
        ret.addProperty("meta", meta);
        ret.addProperty("price", price);
        if(owner != null)
            ret.addProperty("owner", owner.toString());
        ret.addProperty("timestamp", timestamp);
        ret.addProperty("identifier", identifier);
        if(originalAmount != null)
            ret.addProperty("original_amount", originalAmount);
        return ret;
    }
}
