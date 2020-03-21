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
import the_fireplace.grandexchange.market.OfferStatusMessager;
import the_fireplace.grandexchange.market.OfferType;
import the_fireplace.grandexchange.util.SerializationUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("WeakerAccess")
public class JsonDatabase implements IDatabaseHandler {
    private File exchangeDataFile;
    private boolean isChanged;

    private long nextIdentifier = 0;

    private HashMap<Long, NewOffer> offers = Maps.newHashMap();
    private HashMap<Pair<String, Integer>, List<NewOffer>> buyOffers = Maps.newHashMap();
    private HashMap<Pair<String, Integer>, List<NewOffer>> sellOffers = Maps.newHashMap();
    private HashMap<UUID, List<ItemStack>> payouts = Maps.newHashMap();

    private Map<UUID, List<Long>> partialOfferStatusMessages = Maps.newHashMap();
    private Map<UUID, List<OfferStatusMessager.MessageObj>> completeOfferStatusMessages = Maps.newHashMap();

    public JsonDatabase() {
        exchangeDataFile = new File(FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0).getSaveHandler().getWorldDirectory(), "exchange_database.json");
        isChanged = false;
        load();
    }

    public long getNewIdentifier() {
        return nextIdentifier++;
    }

    private void markChanged() {
        if(!isChanged)
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
        if(type.equals(OfferType.BUY) && buyOffers.containsKey(itemPair)) {
            for(NewOffer offer: buyOffers.get(itemPair))
                if(offer.getPrice() >= minMaxPrice && (nbt == null || nbt.equals(offer.getNbt())))
                    resultList.add(offer.copy());
        } else if(type.equals(OfferType.SELL) && sellOffers.containsKey(itemPair)) {
            for(NewOffer offer: sellOffers.get(itemPair))
                if(offer.getPrice() <= minMaxPrice && (nbt == null || nbt.equals(offer.getNbt())))
                    resultList.add(offer.copy());
        }
        resultList.sort(Comparator.comparing(NewOffer::getTimestamp));
        return Collections.unmodifiableList(resultList);
    }

    @Override
    public Collection<NewOffer> getOffers(OfferType type, UUID owner) {
        List<NewOffer> resultList = Lists.newArrayList();
        for(NewOffer offer: offers.values().stream().filter(o -> o.getType().equals(type)).collect(Collectors.toList()))
            if(offer.getOwner().equals(owner))
                resultList.add(offer.copy());
        return Collections.unmodifiableList(resultList);
    }

    @Override
    public Collection<NewOffer> getOffers(OfferType type) {
        return Collections.unmodifiableCollection(offers.values().stream().filter(o -> o.getType().equals(type)).collect(Collectors.toList()));
    }

    @Override
    public NewOffer getOffer(long offerId) {
        return offers.get(offerId);
    }

    @Override
    public void updateOfferStatusPartial(UUID player, long offerId) {
        partialOfferStatusMessages.putIfAbsent(player, Lists.newArrayList());
        if(!partialOfferStatusMessages.get(player).contains(offerId)) {
            partialOfferStatusMessages.get(player).add(offerId);
            markChanged();
        }
    }

    @Override
    public void removeOfferStatusPartial(UUID player, long offerId) {
        if(partialOfferStatusMessages.getOrDefault(player, Collections.emptyList()).remove(offerId))
            markChanged();
    }

    @Override
    public void updateOfferStatusComplete(UUID player, long offerId, String message, int amount, String name, long price, @Nullable String nbt) {
        completeOfferStatusMessages.putIfAbsent(player, Lists.newArrayList());
        completeOfferStatusMessages.get(player).add(new OfferStatusMessager.MessageObj(offerId, message, amount, name, price, nbt));
        markChanged();
    }

    @Override
    public void removeOfferStatusComplete(UUID player, long offerId) {
        if(completeOfferStatusMessages.getOrDefault(player, Collections.emptyList()).removeIf(messageObj -> messageObj.getOfferId() == offerId))
            markChanged();
    }

    @Override
    public boolean hasPartialOfferUpdates(UUID player) {
        return partialOfferStatusMessages.containsKey(player) && !partialOfferStatusMessages.get(player).isEmpty();
    }

    @Override
    public boolean hasCompleteOfferUpdates(UUID player) {
        return completeOfferStatusMessages.containsKey(player) && !completeOfferStatusMessages.get(player).isEmpty();
    }

    @Override
    public List<Long> getPartialOfferUpdates(UUID player) {
        return Collections.unmodifiableList(partialOfferStatusMessages.get(player));
    }

    @Override
    public List<OfferStatusMessager.MessageObj> getCompleteOfferUpdates(UUID player) {
        return Collections.unmodifiableList(completeOfferStatusMessages.get(player));
    }

    @Override
    public long addOffer(OfferType type, String item, int meta, @Nullable Integer amount, long price, @Nullable UUID owner, @Nullable String nbt) {
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
                    UUID user = UUID.fromString(obj.get("user").getAsString());
                    this.payouts.putIfAbsent(user, Lists.newArrayList());
                    this.payouts.get(user).addAll(userPayouts);
                }

                nextIdentifier = db.get("next_identifier").getAsLong();

                JsonArray partialOfferStatusUpdates = db.getAsJsonArray("partial_offer_status_updates");
                for(JsonElement e: partialOfferStatusUpdates) {
                    JsonObject obj = e.getAsJsonObject();
                    UUID user = UUID.fromString(obj.get("user").getAsString());
                    this.partialOfferStatusMessages.putIfAbsent(user, Lists.newArrayList());
                    for(JsonElement offerId: obj.getAsJsonArray("ids"))
                        this.partialOfferStatusMessages.get(user).add(offerId.getAsLong());
                }

                JsonArray completeOfferStatusUpdates = db.getAsJsonArray("complete_offer_status_updates");
                for(JsonElement e: completeOfferStatusUpdates) {
                    JsonObject obj = e.getAsJsonObject();
                    UUID user = UUID.fromString(obj.get("user").getAsString());
                    this.completeOfferStatusMessages.putIfAbsent(user, Lists.newArrayList());
                    for(JsonElement messageJson: obj.getAsJsonArray("messages"))
                        this.completeOfferStatusMessages.get(user).add(new OfferStatusMessager.MessageObj(messageJson.getAsJsonObject()));
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

        JsonArray partialOfferStatusUpdates = new JsonArray();
        for(Map.Entry<UUID, List<Long>> entry: this.partialOfferStatusMessages.entrySet()) {
            JsonObject element = new JsonObject();
            element.addProperty("user", entry.getKey().toString());
            JsonArray ids = new JsonArray();
            for(Long id: entry.getValue())
                ids.add(id);
            element.add("ids", ids);
            partialOfferStatusUpdates.add(element);
        }
        db.add("partial_offer_status_updates", partialOfferStatusUpdates);

        JsonArray completeOfferStatusUpdates = new JsonArray();
        for(Map.Entry<UUID, List<OfferStatusMessager.MessageObj>> entry: this.completeOfferStatusMessages.entrySet()) {
            JsonObject element = new JsonObject();
            element.addProperty("user", entry.getKey().toString());
            JsonArray messages = new JsonArray();
            for(OfferStatusMessager.MessageObj messageObj: entry.getValue())
                messages.add(messageObj.toJson());
            element.add("messages", messages);
            completeOfferStatusUpdates.add(element);
        }
        db.add("complete_offer_status_updates", completeOfferStatusUpdates);

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

    @Override
    public void onServerStop() {
        save();
    }
}
