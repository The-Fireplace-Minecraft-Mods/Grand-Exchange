package the_fireplace.grandexchange.market;

import com.google.common.collect.Lists;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;
import the_fireplace.grandeconomy.api.GrandEconomyApi;
import the_fireplace.grandexchange.GrandExchange;
import the_fireplace.grandexchange.util.SerializationUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static the_fireplace.grandexchange.GrandExchange.getDatabase;

public final class ExchangeManager {
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
        int amountUnfulfilled = tryFulfillOffer(type, item, meta, amount, price, owner, nbt);
        if(amountUnfulfilled > 0) {
            long newOfferId = getDatabase().addOffer(type, item, meta, amountUnfulfilled, price, owner, nbt);
            //Notify the user if it immediately gets partially fulfilled
            if(amountUnfulfilled < amount) {
                OfferStatusMessager.updateStatusPartial(owner, newOfferId);
            }
        }
        else if(amountUnfulfilled < 0)
            GrandExchange.LOGGER.error("Amount unfulfilled was {}! This is not good.", amountUnfulfilled);
        return amountUnfulfilled <= 0;
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
    public static NewOffer getOffer(long offerId) {
        return getDatabase().getOffer(offerId);
    }

    /**
     * Get all offers of a type for an item with the specified minimum or maximum price
     * @param type
     * The offer type to retrieve
     * @param itemPair
     * A pair with the item id and metadata
     * @param minMaxPrice
     * The minimum price when looking for buy offers, or the maximum price when looking for sell offers
     * @param nbt
     * A NBT tag to search for, if any. Null should return any NBT, not just offers without NBT.
     * @return
     * A collection of offers matching the criteria
     */
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

    /**
     * Attempts to fulfill an offer meeting the criteria and returns the remaining amount after fulfilling as much as possible
     */
    private static int tryFulfillOffer(OfferType type, String item, int meta, int amount, long price, UUID owner, @Nullable String nbt) {
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
        return amount;
    }

    private static int tryFulfillSellOffer(String item, int meta, int amount, long price, UUID owner, @Nullable String nbt, boolean isOfferBlock, int maxStackSize) {
        Collection<NewOffer> possibleBuyOffers = getOffers(OfferType.BUY, Pair.of(item, meta), price, nbt);
        if(!possibleBuyOffers.isEmpty()) {
            List<Long> removeOfferIds = Lists.newArrayList();
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
                    removeOfferIds.add(buyOffer.getIdentifier());
                    if(buyOffer.getNbt() == null)
                        OfferStatusMessager.updateStatusComplete(buyOffer.getOwner(), buyOffer.getIdentifier(), "ge.buyoffer.fulfilled", buyOffer.getOriginalAmount(), OfferStatusMessager.getFormatted(buyOffer.getItemResourceName(), buyOffer.getItemMeta()), buyOffer.getPrice(), null);
                    else
                        OfferStatusMessager.updateStatusComplete(buyOffer.getOwner(), buyOffer.getIdentifier(), "ge.buyoffer.fulfilled_nbt", buyOffer.getOriginalAmount(), OfferStatusMessager.getFormatted(buyOffer.getItemResourceName(), buyOffer.getItemMeta()), buyOffer.getPrice(), buyOffer.getNbt());
                } else {
                    int givingAmount = amount;
                    GrandEconomyApi.addToBalance(owner, givingAmount*buyOffer.getPrice(), true);
                    while(givingAmount > maxStackSize) {
                        addPayout(buyOffer.getOwner(), getStack(isOfferBlock, offerResource, maxStackSize, meta, nbt));
                        givingAmount -= maxStackSize;
                    }
                    addPayout(buyOffer.getOwner(), getStack(isOfferBlock, offerResource, givingAmount, meta, nbt));
                    if(amount == buyOffer.getAmount()) {
                        removeOfferIds.add(buyOffer.getIdentifier());
                        if(buyOffer.getNbt() == null)
                            OfferStatusMessager.updateStatusComplete(buyOffer.getOwner(), buyOffer.getIdentifier(), "ge.buyoffer.fulfilled", buyOffer.getOriginalAmount(), OfferStatusMessager.getFormatted(buyOffer.getItemResourceName(), buyOffer.getItemMeta()), buyOffer.getPrice(), null);
                        else
                            OfferStatusMessager.updateStatusComplete(buyOffer.getOwner(), buyOffer.getIdentifier(), "ge.buyoffer.fulfilled_nbt", buyOffer.getOriginalAmount(), OfferStatusMessager.getFormatted(buyOffer.getItemResourceName(), buyOffer.getItemMeta()), buyOffer.getPrice(), buyOffer.getNbt());
                    } else {
                        updateCount(buyOffer.getIdentifier(), buyOffer.getAmount() - amount);
                        OfferStatusMessager.updateStatusPartial(buyOffer.getOwner(), buyOffer.getIdentifier());
                    }
                    amount = 0;
                    break;
                }
            }
            for(long offerId: removeOfferIds)
                removeOffer(offerId);
            return amount;
        }
        return amount;
    }

    private static int tryFulfillBuyOffer(String item, int meta, int amount, long price, UUID owner, @Nullable String nbt, boolean isOfferBlock, int maxStackSize) {
        Collection<NewOffer> possibleSellOffers = getOffers(OfferType.SELL, Pair.of(item, meta), price, nbt);
        if(!possibleSellOffers.isEmpty()) {
            List<Long> removeOfferIds = Lists.newArrayList();
            for(NewOffer sellOffer: possibleSellOffers){
                ResourceLocation offerResource = new ResourceLocation(item);
                if(amount > sellOffer.getAmount()){
                    int givingAmount = sellOffer.getAmount();
                    GrandEconomyApi.addToBalance(sellOffer.getOwner(), givingAmount*sellOffer.getPrice(), true);
                    if(nbt == null)
                        OfferStatusMessager.updateStatusComplete(sellOffer.getOwner(), sellOffer.getIdentifier(), "ge.selloffer.fulfilled", sellOffer.getOriginalAmount(), OfferStatusMessager.getFormatted(item, meta), price, null);
                    else
                        OfferStatusMessager.updateStatusComplete(sellOffer.getOwner(), sellOffer.getIdentifier(), "ge.selloffer.fulfilled_nbt", sellOffer.getOriginalAmount(), OfferStatusMessager.getFormatted(item, meta), price, nbt);
                    while(givingAmount > maxStackSize) {
                        addPayout(owner, getStack(isOfferBlock, offerResource, maxStackSize, meta, nbt));
                        givingAmount -= maxStackSize;
                    }
                    addPayout(owner, getStack(isOfferBlock, offerResource, givingAmount, meta, nbt));
                    amount -= sellOffer.getAmount();
                    removeOfferIds.add(sellOffer.getIdentifier());
                } else {
                    int givingAmount = amount;
                    GrandEconomyApi.addToBalance(sellOffer.getOwner(), givingAmount*sellOffer.getPrice(), true);
                    while(givingAmount > maxStackSize) {
                        addPayout(owner, getStack(isOfferBlock, offerResource, maxStackSize, meta, nbt));
                        givingAmount -= maxStackSize;
                    }
                    addPayout(owner, getStack(isOfferBlock, offerResource, givingAmount, meta, nbt));
                    if(amount == sellOffer.getAmount()) {
                        removeOfferIds.add(sellOffer.getIdentifier());
                        if(nbt == null)
                            OfferStatusMessager.updateStatusComplete(sellOffer.getOwner(), sellOffer.getIdentifier(), "ge.selloffer.fulfilled", sellOffer.getOriginalAmount(), OfferStatusMessager.getFormatted(item, meta), price, null);
                        else
                            OfferStatusMessager.updateStatusComplete(sellOffer.getOwner(), sellOffer.getIdentifier(), "ge.selloffer.fulfilled_nbt", sellOffer.getOriginalAmount(), OfferStatusMessager.getFormatted(item, meta), price, nbt);
                    } else {
                        updateCount(sellOffer.getIdentifier(), sellOffer.getAmount() - amount);
                        OfferStatusMessager.updateStatusPartial(sellOffer.getOwner(), sellOffer.getIdentifier());
                    }
                    amount = 0;
                    break;
                }
            }
            for(long offerId: removeOfferIds)
                removeOffer(offerId);
            return amount;
        }
        return amount;
    }

    private static ItemStack getStack(boolean isOfferBlock, ResourceLocation offerResource, int amount, int meta, @Nullable String nbt) {
        //noinspection ConstantConditions
        ItemStack stack =  isOfferBlock ? new ItemStack(Item.getItemFromBlock(ForgeRegistries.BLOCKS.getValue(offerResource)), amount, meta) : new ItemStack(ForgeRegistries.ITEMS.getValue(offerResource), amount, meta);
        if(nbt != null)
            stack.setTagCompound(SerializationUtils.getNbt(nbt));
        return stack;
    }
}
