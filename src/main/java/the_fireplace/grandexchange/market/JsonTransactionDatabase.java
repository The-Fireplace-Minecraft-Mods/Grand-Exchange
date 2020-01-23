package the_fireplace.grandexchange.market;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonParser;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;
import the_fireplace.grandeconomy.api.GrandEconomyApi;
import the_fireplace.grandexchange.util.SerializationUtils;
import the_fireplace.grandexchange.util.TextStyles;

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
        if(!payouts.containsKey(player))
            payouts.put(player, Collections.emptyList());
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

    public boolean makeOffer(NewOffer offer){
        boolean offerFulfilled = tryFulfillOffer(offer);
        if(!offerFulfilled){
            if(offer.isBuyOffer()){
                if(!buyOffers.containsKey(offer.getItemPair()))
                    buyOffers.put(offer.getItemPair(), Lists.newArrayList());
                buyOffers.get(offer.getItemPair()).add(offer);
            } else {
                if(!sellOffers.containsKey(offer.getItemPair()))
                    sellOffers.put(offer.getItemPair(), Lists.newArrayList());
                sellOffers.get(offer.getItemPair()).add(offer);
            }
        }
        return offerFulfilled;
    }

    @SuppressWarnings("Duplicates")
    private boolean tryFulfillOffer(NewOffer offer) {
        if(!payouts.containsKey(offer.getOwner()))
            payouts.put(offer.getOwner(), Lists.newArrayList());
        ResourceLocation offerResource = new ResourceLocation(offer.getItemResourceName());
        boolean isOfferBlock = ForgeRegistries.BLOCKS.containsKey(offerResource);
        @SuppressWarnings("ConstantConditions")
        ItemStack sizeCheckStack = isOfferBlock ? new ItemStack(ForgeRegistries.BLOCKS.getValue(offerResource), 1, offer.getItemMeta()) : new ItemStack(ForgeRegistries.ITEMS.getValue(offerResource), 1, offer.getItemMeta());
        int maxStackSize = sizeCheckStack.getMaxStackSize();
        sortOffers();
        if(offer.isBuyOffer()){
            return tryFulfillBuyOffer(offer, offerResource, isOfferBlock, maxStackSize);
        } else if(offer.isSellOffer()) {
            return tryFulfillSellOffer(offer, offerResource, isOfferBlock, maxStackSize);
        }
        return false;
    }

    private boolean tryFulfillSellOffer(NewOffer offer, ResourceLocation offerResource, boolean isOfferBlock, int maxStackSize) {
        if(buyOffers.containsKey(offer.getItemPair()) && !buyOffers.get(offer.getItemPair()).isEmpty()){
            List<NewOffer> possibleBuyOffers = buyOffers.get(offer.getItemPair());
            possibleBuyOffers.removeIf(o1 -> o1.getPrice() < offer.getPrice());
            List<NewOffer> removeOffers = Lists.newArrayList();
            boolean offerComplete = false;
            for(NewOffer buyOffer: possibleBuyOffers){
                Entity buyer = FMLCommonHandler.instance().getMinecraftServerInstance().getEntityFromUuid(buyOffer.getOwner());
                if(!payouts.containsKey(buyOffer.getOwner()))
                    payouts.put(buyOffer.getOwner(), Lists.newArrayList());
                if(offer.getAmount() > buyOffer.getAmount()){
                    int givingAmount = buyOffer.getAmount();
                    GrandEconomyApi.addToBalance(offer.getOwner(), givingAmount*buyOffer.getPrice(), true);
                    while(givingAmount > maxStackSize) {
                        payouts.get(buyOffer.getOwner()).add(getStack(offerResource, isOfferBlock, maxStackSize, offer));
                        givingAmount -= maxStackSize;
                    }
                    payouts.get(buyOffer.getOwner()).add(getStack(offerResource, isOfferBlock, givingAmount, offer));
                    offer.decrementAmount(buyOffer.getAmount());
                    removeOffers.add(buyOffer);
                    if(buyer != null)
                        buyer.sendMessage(new TextComponentTranslation("ge.buyoffer.fulfilled", buyOffer.getAmount(), buyOffer.getItemResourceName(), buyOffer.getItemMeta() + (buyOffer.getNbt() != null ? " with NBT "+buyOffer.getNbt() : ""), buyOffer.getPrice()).setStyle(TextStyles.BLUE));
                } else {
                    int givingAmount = offer.getAmount();
                    GrandEconomyApi.addToBalance(offer.getOwner(), givingAmount*buyOffer.getPrice(), true);
                    while(givingAmount > maxStackSize) {
                        payouts.get(buyOffer.getOwner()).add(getStack(offerResource, isOfferBlock, maxStackSize, offer));
                        givingAmount -= maxStackSize;
                    }
                    payouts.get(buyOffer.getOwner()).add(getStack(offerResource, isOfferBlock, givingAmount, offer));
                    if(offer.getAmount() == buyOffer.getAmount()) {
                        removeOffers.add(buyOffer);
                        if(buyer != null)
                            buyer.sendMessage(new TextComponentTranslation("ge.buyoffer.fulfilled", buyOffer.getAmount(), buyOffer.getItemResourceName(), buyOffer.getItemMeta() + (buyOffer.getNbt() != null ? " with NBT "+buyOffer.getNbt() : ""), buyOffer.getPrice()).setStyle(TextStyles.BLUE));
                    } else {
                        buyOffer.decrementAmount(offer.getAmount());
                        if(buyer != null)
                            buyer.sendMessage(new TextComponentTranslation("ge.buyoffer.fulfilled_partial", offer.getAmount(), buyOffer.getItemResourceName(), buyOffer.getItemMeta() + (buyOffer.getNbt() != null ? " with NBT "+buyOffer.getNbt() : ""), buyOffer.getPrice()).setStyle(TextStyles.BLUE));
                    }
                    offerComplete = true;
                    break;
                }
            }
            buyOffers.get(offer.getItemPair()).removeAll(removeOffers);
            return offerComplete;
        }
        return false;
    }

    private boolean tryFulfillBuyOffer(NewOffer offer, ResourceLocation offerResource, boolean isOfferBlock, int maxStackSize) {
        if(sellOffers.containsKey(offer.getItemPair()) && !sellOffers.get(offer.getItemPair()).isEmpty()){
            List<NewOffer> possibleSellOffers = sellOffers.get(offer.getItemPair());
            possibleSellOffers.removeIf(o1 -> o1.getPrice() > offer.getPrice());
            List<NewOffer> removeOffers = Lists.newArrayList();
            boolean offerComplete = false;
            for(NewOffer sellOffer: possibleSellOffers){
                Entity seller = FMLCommonHandler.instance().getMinecraftServerInstance().getEntityFromUuid(sellOffer.getOwner());
                if(offer.getAmount() > sellOffer.getAmount()){
                    int givingAmount = sellOffer.getAmount();
                    GrandEconomyApi.addToBalance(sellOffer.getOwner(), givingAmount*sellOffer.getPrice(), true);
                    if(seller != null)
                        seller.sendMessage(new TextComponentTranslation("ge.selloffer.fulfilled", givingAmount, offer.getItemResourceName(), offer.getItemMeta(), offer.getPrice()).setStyle(TextStyles.DARK_PURPLE));
                    while(givingAmount > maxStackSize) {
                        payouts.get(offer.getOwner()).add(getStack(offerResource, isOfferBlock, maxStackSize, offer));
                        givingAmount -= maxStackSize;
                    }
                    payouts.get(offer.getOwner()).add(getStack(offerResource, isOfferBlock, givingAmount, offer));
                    offer.decrementAmount(sellOffer.getAmount());
                    removeOffers.add(sellOffer);
                } else {
                    int givingAmount = offer.getAmount();
                    GrandEconomyApi.addToBalance(sellOffer.getOwner(), givingAmount*sellOffer.getPrice(), true);
                    while(givingAmount > maxStackSize) {
                        payouts.get(offer.getOwner()).add(getStack(offerResource, isOfferBlock, maxStackSize, offer));
                        givingAmount -= maxStackSize;
                    }
                    payouts.get(offer.getOwner()).add(getStack(offerResource, isOfferBlock, givingAmount, offer));
                    if(offer.getAmount() == sellOffer.getAmount()) {
                        removeOffers.add(sellOffer);
                        if(seller != null)
                            seller.sendMessage(new TextComponentTranslation("ge.selloffer.fulfilled", offer.getAmount(), offer.getItemResourceName(), offer.getItemMeta() + (offer.getNbt() != null ? " with NBT "+offer.getNbt() : ""), sellOffer.getPrice()).setStyle(TextStyles.DARK_PURPLE));
                    } else {
                        sellOffer.decrementAmount(offer.getAmount());
                        if(seller != null)
                            seller.sendMessage(new TextComponentTranslation("ge.selloffer.fulfilled_partial", offer.getAmount(), offer.getItemResourceName(), offer.getItemMeta() + (offer.getNbt() != null ? " with NBT "+offer.getNbt() : ""), sellOffer.getPrice()).setStyle(TextStyles.DARK_PURPLE));
                    }
                    offerComplete = true;
                    break;
                }
            }
            sellOffers.get(offer.getItemPair()).removeAll(removeOffers);
            return offerComplete;
        }
        return false;
    }

    private static ItemStack getStack(ResourceLocation offerResource, boolean isOfferBlock, int amount, Offer offer) {
        //noinspection ConstantConditions
        ItemStack stack =  isOfferBlock ? new ItemStack(Item.getItemFromBlock(ForgeRegistries.BLOCKS.getValue(offerResource)), amount, offer.getItemMeta()) : new ItemStack(ForgeRegistries.ITEMS.getValue(offerResource), amount, offer.getItemMeta());
        if(offer.getNbt() != null)
            stack.setTagCompound(SerializationUtils.getNbt(offer.getNbt()));
        return stack;
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
