package the_fireplace.grandexchange.db;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.*;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.lang3.tuple.Pair;
import the_fireplace.grandexchange.market.NewOffer;
import the_fireplace.grandexchange.market.OfferType;
import the_fireplace.grandexchange.util.SerializationUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

@SuppressWarnings("WeakerAccess")
public class JsonDatabase implements IDatabaseHandler {
    private File exchangeDataFile;
    private boolean isChanged;

    private long nextIdentifier = 0;

    private HashMap<Long, NewOffer> offers = Maps.newHashMap();
    private HashMap<Pair<String, Integer>, List<NewOffer>> buyOffers = Maps.newHashMap();
    private HashMap<Pair<String, Integer>, List<NewOffer>> sellOffers = Maps.newHashMap();
    private HashMap<UUID, List<ItemStack>> payouts = Maps.newHashMap();

    public JsonDatabase() {
        exchangeDataFile = new File(FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0).getSaveHandler().getWorldDirectory(), "exchange_database.json");
        isChanged = false;
        load();
    }

    public long getNewIdentifier() {
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
        offers.get(offerId).setAmount(newAmount);
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
                if(offer.getPrice() >= minMaxPrice && (nbt == null || nbt.equals(offer.getNbt())))
                    resultList.add(offer.copy());
        } else if(type.equals(OfferType.SELL)) {
            for(NewOffer offer: sellOffers.get(itemPair))
                if(offer.getPrice() <= minMaxPrice && (nbt == null || nbt.equals(offer.getNbt())))
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
    public NewOffer getOffer(long offerId) {
        return offers.get(offerId);
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

                nextIdentifier = db.get("next_identifier").getAsLong();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else
            markChanged();
    }

    public void save() {
        if(!isChanged)
            return;
        JsonObject db = new JsonObject();

        JsonArray offers = new JsonArray();
        for(NewOffer offer: this.offers.values())
            offers.add(offer.toJsonObject());
        db.add("offers", offers);

        JsonArray payouts = new JsonArray();
        for(Map.Entry<UUID, List<ItemStack>> entry: this.payouts.entrySet()) {
            JsonObject element = new JsonObject();
            element.addProperty("user", entry.getKey().toString());
            JsonArray items = new JsonArray();
            for(ItemStack stack: entry.getValue())
                items.add(SerializationUtils.stackToString(stack));
            element.add("items", items);
            payouts.add(element);
        }
        db.add("payouts", payouts);

        db.addProperty("next_identifier", nextIdentifier);

        try {
            FileWriter file = new FileWriter(exchangeDataFile);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(db);
            file.write(json);
            file.close();
        } catch(IOException e) {
            e.printStackTrace();
            return;
        }
        isChanged = false;
    }

    private short tickCount = 3;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent e) {
        //TODO Potentially monitor average load on the server and wait for points of lower load to do the save?
        if(tickCount++ % (20 * 60 * 2 + 29) == 0) {//Check if save needed once every 2 minutes 29 seconds assuming a server is running at full speed. The number is that specific only to help offset these saves from those done by other things.
            tickCount = 0;
            save();
        }
    }

    public void onServerStop() {
        save();
    }
}
