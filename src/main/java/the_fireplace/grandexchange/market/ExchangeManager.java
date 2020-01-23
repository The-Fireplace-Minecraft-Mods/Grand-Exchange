package the_fireplace.grandexchange.market;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import the_fireplace.grandeconomy.api.GrandEconomyApi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
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
        getDatabase().addOffer(type, item, meta, amount, price, owner, nbt);
        return false;
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
}
