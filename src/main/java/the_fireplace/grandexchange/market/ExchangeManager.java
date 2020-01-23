package the_fireplace.grandexchange.market;

import com.google.common.collect.Lists;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;
import the_fireplace.grandeconomy.api.GrandEconomyApi;
import the_fireplace.grandexchange.util.SerializationUtils;
import the_fireplace.grandexchange.util.TextStyles;
import the_fireplace.grandexchange.util.translation.TranslationUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ExchangeManager {
    private static ITransactionDatabase db = null;

    protected static ITransactionDatabase getDatabase() {
        if(db == null)
            db = new JsonTransactionDatabase();
        return db;
    }

    /**
     * Makes an offer and attempts to fulfill it
     * @param type
     * The type of offer to make
     * @param item
     * The item id being offered
     * @param meta
     * The item meta being offered
     * @param amount
     * The amount being offered
     * @param price
     * The price being offered
     * @param owner
     * The player making the offer
     * @param nbt
     * The String form of the item's NBT
     * @return
     * True if the offer was fulfilled, false otherwise.
     */
    public static boolean makeOffer(OfferType type, String item, int meta, int amount, long price, UUID owner, @Nullable String nbt) {
        boolean offerFulfilled = tryFulfillOffer(offer);
        if(!offerFulfilled)
            getDatabase().addOffer(type, item, meta, amount, price, owner, nbt);
        return offerFulfilled;
    }
    public static boolean makeOffer(OfferType type, ResourceLocation item, int meta, int amount, long price, UUID owner, @Nullable String nbt) {
        return makeOffer(type, item.toString(), meta, amount, price, owner, nbt);
    }
    public static boolean makeOffer(OfferType type, ResourceLocation item, int meta, int amount, long price, UUID owner, @Nonnull NBTTagCompound nbt) {
        return makeOffer(type, item.toString(), meta, amount, price, owner, nbt.toString());
    }
    public static boolean makeOffer(OfferType type, String item, int meta, int amount, long price, UUID owner) {
        return makeOffer(type, item, meta, amount, price, owner, null);
    }
    public static boolean makeOffer(OfferType type, ItemStack stack, int amount, long price, UUID owner) {
        return makeOffer(type, Objects.requireNonNull(stack.getItem().getRegistryName()).toString(), stack.getMetadata(), amount, price, owner, stack.hasTagCompound() ? Objects.requireNonNull(stack.getTagCompound()).toString() : null);
    }

    /**
     * Remove the offer with the matching ID from the database. If the offer is being cancelled, see also {@link ExchangeManager#returnInvestment(NewOffer)}.
     * @param offerId
     * The ID of the offer to be removed
     * @return
     * The offer that was removed, or null if not found.
     */
    public static NewOffer removeOffer(long offerId) {
        return getDatabase().removeOffer(offerId);
    }
    public static void addPayout(UUID player, ItemStack payout) {
        getDatabase().addPayout(player, payout);
    }
    public static void removePayout(UUID player, ItemStack payout) {
        getDatabase().removePayout(player, payout);
    }
    public static void removePayouts(UUID player, ItemStack... payouts) {
        for(ItemStack payout: payouts)
            removePayout(player, payout);
    }
    public static Collection<ItemStack> getPayout(UUID player) {
        return getDatabase().getPayouts(player);
    }
    public static boolean hasPayout(UUID player) {
        return getDatabase().countPayouts(player) > 0;
    }
    public static Collection<NewOffer> getOffers(OfferType type, Pair<String, Integer> itemPair, long minMaxPrice, @Nullable String nbt) {
        return getDatabase().getOffers(type, itemPair, minMaxPrice, nbt);
    }
    public static void updateCount(long offerId, int newAmount){
        getDatabase().updateCount(offerId, newAmount);
    }
    public static int getCount(long offerId) {
        return getDatabase().getCount(offerId);
    }

    public static boolean canTransactItem(ItemStack item){
        return !item.isEmpty();
    }

    /**
     * Returns the part of the offer that was not fulfilled. For sell offers, this returns the items. For buy offers, this returns the money.
     */
    public static void returnInvestment(NewOffer offer) {
        if(offer.isBuyOffer()) {
            GrandEconomyApi.addToBalance(offer.getOwner(), offer.getPrice()*offer.getAmount(), true);
        } else if(offer.isSellOffer()) {
            ResourceLocation offerRes = new ResourceLocation(offer.getItemResourceName());
            boolean isOfferBlock = ForgeRegistries.BLOCKS.containsKey(offerRes);
            ExchangeManager.addPayout(offer.getOwner(), isOfferBlock ? new ItemStack(Objects.requireNonNull(ForgeRegistries.BLOCKS.getValue(offerRes)), offer.getAmount(), offer.getItemMeta()) : new ItemStack(Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(offerRes)), offer.getAmount(), offer.getItemMeta()));
        }
    }

    private static boolean tryFulfillOffer(OfferType type, String item, int meta, int amount, long price, UUID owner, @Nullable String nbt) {
        ResourceLocation offerResource = new ResourceLocation(item);
        boolean isOfferBlock = !ForgeRegistries.ITEMS.containsKey(offerResource);
        @SuppressWarnings("ConstantConditions")
        ItemStack sizeCheckStack = isOfferBlock ? new ItemStack(ForgeRegistries.BLOCKS.getValue(offerResource), 1, meta) : new ItemStack(ForgeRegistries.ITEMS.getValue(offerResource), 1, meta);
        int maxStackSize = sizeCheckStack.getMaxStackSize();
        if(type.equals(OfferType.BUY)){
            return tryFulfillBuyOffer(item, meta, amount, price, owner, nbt, isOfferBlock, maxStackSize);
        } else if(type.equals(OfferType.SELL)) {
            return tryFulfillSellOffer(item, meta, amount, price, owner, nbt, isOfferBlock, maxStackSize);
        }
        return false;
    }

    private static boolean tryFulfillSellOffer(String item, int meta, int amount, long price, UUID owner, @Nullable String nbt, boolean isOfferBlock, int maxStackSize) {
        Collection<NewOffer> possibleBuyOffers = getOffers(OfferType.BUY, Pair.of(item, meta), price, nbt);
        if(!possibleBuyOffers.isEmpty()) {
            List<NewOffer> removeOffers = Lists.newArrayList();
            boolean offerComplete = false;
            for(NewOffer buyOffer: possibleBuyOffers) {
                Entity buyer = FMLCommonHandler.instance().getMinecraftServerInstance().getEntityFromUuid(buyOffer.getOwner());
                ResourceLocation offerResource = new ResourceLocation(item);
                if(amount > buyOffer.getAmount()){
                    int givingAmount = buyOffer.getAmount();
                    GrandEconomyApi.addToBalance(owner, givingAmount*buyOffer.getPrice(), true);
                    while(givingAmount > maxStackSize) {
                        addPayout(buyOffer.getOwner(), getStack(isOfferBlock, offerResource, maxStackSize, meta, nbt));
                        givingAmount -= maxStackSize;
                    }
                    addPayout(buyOffer.getOwner(), getStack(isOfferBlock, offerResource, givingAmount, meta, nbt));
                    amount -= buyOffer.getAmount();
                    removeOffers.add(buyOffer);
                    if(buyer != null) {
                        if(buyOffer.getNbt() == null)
                            buyer.sendMessage(TranslationUtil.getTranslation(buyOffer.getOwner(), "ge.buyoffer.fulfilled", buyOffer.getAmount(), buyOffer.getItemResourceName(), buyOffer.getItemMeta(), buyOffer.getPrice()).setStyle(TextStyles.BLUE));
                        else
                            buyer.sendMessage(TranslationUtil.getTranslation(buyOffer.getOwner(), "ge.buyoffer.fulfilled_nbt", buyOffer.getAmount(), buyOffer.getItemResourceName(), buyOffer.getItemMeta(), buyOffer.getNbt(), buyOffer.getPrice()).setStyle(TextStyles.BLUE));
                    }
                    //TODO if the offer gets partially fulfilled immediately, be sure to send a message saying how much got done
                } else {
                    int givingAmount = amount;
                    GrandEconomyApi.addToBalance(owner, givingAmount*buyOffer.getPrice(), true);
                    while(givingAmount > maxStackSize) {
                        addPayout(buyOffer.getOwner(), getStack(isOfferBlock, offerResource, maxStackSize, meta, nbt));
                        givingAmount -= maxStackSize;
                    }
                    addPayout(buyOffer.getOwner(), getStack(isOfferBlock, offerResource, givingAmount, meta, nbt));
                    if(amount == buyOffer.getAmount()) {
                        removeOffers.add(buyOffer);
                        if(buyer != null) {
                            if(buyOffer.getNbt() == null)
                                buyer.sendMessage(TranslationUtil.getTranslation(buyOffer.getOwner(), "ge.buyoffer.fulfilled", buyOffer.getAmount(), buyOffer.getItemResourceName(), buyOffer.getItemMeta(), buyOffer.getPrice()).setStyle(TextStyles.BLUE));
                            else
                                buyer.sendMessage(TranslationUtil.getTranslation(buyOffer.getOwner(), "ge.buyoffer.fulfilled_nbt", buyOffer.getAmount(), buyOffer.getItemResourceName(), buyOffer.getItemMeta(), buyOffer.getNbt(), buyOffer.getPrice()).setStyle(TextStyles.BLUE));
                        }
                    } else {
                        updateCount(buyOffer.getIdentifier(), buyOffer.getAmount() - amount);
                        if(buyer != null) {
                            if(buyOffer.getNbt() == null)
                                buyer.sendMessage(TranslationUtil.getTranslation(buyOffer.getOwner(), "ge.buyoffer.fulfilled_partial", amount, buyOffer.getItemResourceName(), buyOffer.getItemMeta(), buyOffer.getPrice()).setStyle(TextStyles.BLUE));
                            else
                                buyer.sendMessage(TranslationUtil.getTranslation(buyOffer.getOwner(), "ge.buyoffer.fulfilled_partial_nbt", amount, buyOffer.getItemResourceName(), buyOffer.getItemMeta(), buyOffer.getNbt(), buyOffer.getPrice()).setStyle(TextStyles.BLUE));
                        }
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

    private static boolean tryFulfillBuyOffer(String item, int meta, int amount, long price, UUID owner, @Nullable String nbt, boolean isOfferBlock, int maxStackSize) {
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

    private static ItemStack getStack(boolean isOfferBlock, ResourceLocation offerResource, int amount, int meta, @Nullable String nbt) {
        //noinspection ConstantConditions
        ItemStack stack =  isOfferBlock ? new ItemStack(Item.getItemFromBlock(ForgeRegistries.BLOCKS.getValue(offerResource)), amount, meta) : new ItemStack(ForgeRegistries.ITEMS.getValue(offerResource), amount, meta);
        if(nbt != null)
            stack.setTagCompound(SerializationUtils.getNbt(nbt));
        return stack;
    }
}
