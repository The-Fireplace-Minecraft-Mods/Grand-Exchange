package the_fireplace.grandexchange.market;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.commons.lang3.tuple.Pair;
import the_fireplace.grandexchange.util.SerializationUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileReader;
import java.util.*;

@SuppressWarnings("WeakerAccess")
public class JsonTransactionDatabase implements ITransactionDatabase {
    private File exchangeDataFile;
    private boolean isChanged;

    private long nextIdentifier = 0;

    private HashMap<Long, NewOffer> offers = Maps.newHashMap();
    private HashMap<Pair<String, Integer>, List<NewOffer>> buyOffers = Maps.newHashMap();
    private HashMap<Pair<String, Integer>, List<NewOffer>> sellOffers = Maps.newHashMap();
    private HashMap<UUID, List<ItemStack>> payouts = Maps.newHashMap();

    protected JsonTransactionDatabase() {
        exchangeDataFile = new File(FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0).getSaveHandler().getWorldDirectory(), "exchange_database.json");
        isChanged = false;
        load();
    }

    protected long getNewIdentifier() {
        return nextIdentifier++;
    }

    private void markChanged() {
        isChanged = true;
    }

    @Override
    public void addPayout(UUID player, ItemStack payout){
        payouts.putIfAbsent(player, Lists.newArrayList());
        payouts.get(player).add(payout);
    }

    @Override
    public void removePayout(UUID player, ItemStack payout) {
        payouts.getOrDefault(player, Collections.emptyList()).remove(payout);
    }

    @Override
    public Collection<ItemStack> getPayouts(UUID player) {
        return payouts.getOrDefault(player, Collections.emptyList());
    }

    @Override
    public int countPayouts(UUID player) {
        return payouts.getOrDefault(player, Collections.emptyList()).size();
    }

    @Override
    public void updateCount(long offerId, int newAmount) {
        offers.get(offerId).amount = newAmount;
    }

    @Override
    public int getCount(long offerId) {
        return offers.get(offerId).getAmount();
    }

    @Override
    public Collection<NewOffer> getOffers(OfferType type, Pair<String, Integer> itemPair, long minMaxPrice, @Nullable String nbt) {
        List<NewOffer> resultList = Lists.newArrayList();
        if(type.equals(OfferType.BUY)) {
            for(NewOffer offer: buyOffers.get(itemPair))
                if(offer.getPrice() >= minMaxPrice && (nbt == null || nbt.equals(offer.nbt)))
                    resultList.add(offer.copy());
        } else if(type.equals(OfferType.SELL)) {
            for(NewOffer offer: sellOffers.get(itemPair))
                if(offer.getPrice() <= minMaxPrice && (nbt == null || nbt.equals(offer.nbt)))
                    resultList.add(offer.copy());
        }
        resultList.sort(Comparator.comparing(NewOffer::getTimestamp));
        return resultList;
    }

    @Override
    public Collection<NewOffer> getOffers(OfferType type, UUID owner) {
        List<NewOffer> resultList = Lists.newArrayList();
        for(NewOffer offer: offers.values())
            if(offer.getOwner().equals(owner))
                resultList.add(offer.copy());
        return resultList;
    }

    @Override
    public long addOffer(OfferType type, String item, int meta, int amount, long price, UUID owner, @Nullable String nbt) {
        long id = getNewIdentifier();
        NewOffer offer = new NewOffer(id, type.toString().toLowerCase(), item, meta, amount, price, owner, nbt);
        offers.put(id, offer);
        if(offer.isBuyOffer()) {
            buyOffers.putIfAbsent(offer.getItemPair(), Lists.newArrayList());
            buyOffers.get(offer.getItemPair()).add(offer);
        } else if(offer.isSellOffer()) {
            sellOffers.putIfAbsent(offer.getItemPair(), Lists.newArrayList());
            sellOffers.get(offer.getItemPair()).add(offer);
        }
        markChanged();
        return id;
    }

    @Override
    public NewOffer removeOffer(long offerId) {
        NewOffer offer = offers.remove(offerId);
        if(offer != null) {
            if(offer.isBuyOffer())
                for (Pair<String, Integer> key: buyOffers.keySet())
                    buyOffers.get(key).remove(offer);
            else if(offer.isSellOffer())
                for (Pair<String, Integer> key: sellOffers.keySet())
                    sellOffers.get(key).remove(offer);
            markChanged();
        }
        return offer;
    }

    private void load() {
        JsonParser jsonParser = new JsonParser();
        if(exchangeDataFile.exists()) {
            try {
                FileReader reader = new FileReader(exchangeDataFile);
                JsonObject db = (JsonObject)jsonParser.parse(reader);
                reader.close();

                JsonArray offers = db.getAsJsonArray("offers");
                for(JsonElement e: offers) {
                    NewOffer offer = new NewOffer(e.getAsJsonObject());
                    this.offers.put(offer.getIdentifier(), offer);
                    if(offer.isBuyOffer()) {
                        this.buyOffers.putIfAbsent(offer.getItemPair(), Lists.newArrayList());
                        this.buyOffers.get(offer.getItemPair()).add(offer);
                    } else if(offer.isSellOffer()) {
                        this.sellOffers.putIfAbsent(offer.getItemPair(), Lists.newArrayList());
                        this.sellOffers.get(offer.getItemPair()).add(offer);
                    }
                }

                JsonArray payouts = db.getAsJsonArray("payouts");
                for(JsonElement e: payouts) {
                    JsonObject obj = e.getAsJsonObject();
                    List<ItemStack> userPayouts = Lists.newArrayList();
                    for(JsonElement stackElement: obj.getAsJsonArray("items"))
                        userPayouts.add(SerializationUtils.stackFromString(stackElement.getAsString()));
                    this.payouts.putIfAbsent(UUID.fromString(obj.get("user").getAsString()), Lists.newArrayList());
                    this.payouts.get(UUID.fromString(obj.get("user").getAsString())).addAll(userPayouts);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else
            markChanged();
    }

    public void save() {
        if(!isChanged)
            return;
        //TODO write db
    }
}
