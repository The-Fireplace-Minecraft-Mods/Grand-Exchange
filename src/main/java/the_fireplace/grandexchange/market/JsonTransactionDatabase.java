package the_fireplace.grandexchange.market;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonParser;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.commons.lang3.tuple.Pair;

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

    private void sortOffers(){
        for(List<NewOffer> buyOffer : buyOffers.values())
            buyOffer.sort(Comparator.comparing(NewOffer::getPrice).thenComparing(NewOffer::getTimestamp));
        for(List<NewOffer> sellOffer : sellOffers.values())
            sellOffer.sort(Comparator.comparing(NewOffer::getTimestamp));
    }

    @Override
    public void addOffer(OfferType type, String item, int meta, int amount, long price, UUID owner, @Nullable String nbt) {
        long id = getNewIdentifier();
        NewOffer offer = new NewOffer(id, type.toString().toLowerCase(), item, meta, amount, price, owner, nbt);
        offers.put(id, offer);
        if(offer.isBuyOffer()) {
            buyOffers.putIfAbsent(offer.getItemPair(), Lists.newArrayList());
            buyOffers.get(offer.getItemPair()).add(offer);
        }
        if(offer.isSellOffer()) {
            sellOffers.putIfAbsent(offer.getItemPair(), Lists.newArrayList());
            sellOffers.get(offer.getItemPair()).add(offer);
        }
        markChanged();
    }

    @Override
    public NewOffer removeOffer(long offerId) {
        NewOffer offer = offers.remove(offerId);
        if(offer != null) {
            if(offer.isBuyOffer())
                for (Pair<String, Integer> key: buyOffers.keySet())
                    buyOffers.get(key).remove(offer);
            if(offer.isSellOffer())
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
                Object db = jsonParser.parse(reader);
                reader.close();

                //TODO read db
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else
            markChanged();
    }

    public static void save() {
        //TODO write db
    }
}
