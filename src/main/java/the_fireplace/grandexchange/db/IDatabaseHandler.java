package the_fireplace.grandexchange.db;

import net.minecraft.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;
import the_fireplace.grandexchange.market.NewOffer;
import the_fireplace.grandexchange.market.OfferStatusMessager;
import the_fireplace.grandexchange.market.OfferType;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface IDatabaseHandler {
    /**
     * Add an offer to the database
     * @return the offer id
     */
    long addOffer(OfferType type, String item, int meta, @Nullable Integer amount, long price, @Nullable UUID owner, @Nullable String nbt);

    /**
     * Remove the offer with the matching ID from the database.
     * @param offerId
     * The ID of the offer to be removed
     * @return
     * The offer that was removed, or null if not found.
     */
    @Nullable
    NewOffer removeOffer(long offerId);
    void addPayout(UUID player, ItemStack payout);
    void removePayout(UUID player, ItemStack payout);
    Collection<ItemStack> getPayouts(UUID player);
    int countPayouts(UUID player);
    void updateCount(long offerId, int newAmount);
    int getCount(long offerId);

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
    Collection<NewOffer> getOffers(OfferType type, Pair<String, Integer> itemPair, long minMaxPrice, @Nullable String nbt);

    /**
     * Gets all offers of a type with the specified owner
     */
    Collection<NewOffer> getOffers(OfferType type, UUID owner);
    Collection<NewOffer> getOffers(OfferType type);
    NewOffer getOffer(long offerId);

    void updateOfferStatusPartial(UUID player, long offerId);
    void removeOfferStatusPartial(UUID player, long offerId);
    void updateOfferStatusComplete(UUID player, long offerId, String message, int amount, String name, long price, @Nullable String nbt);
    void removeOfferStatusComplete(UUID player, long offerId);
    boolean hasPartialOfferUpdates(UUID player);
    boolean hasCompleteOfferUpdates(UUID player);
    List<Long> getPartialOfferUpdates(UUID player);
    List<OfferStatusMessager.MessageObj> getCompleteOfferUpdates(UUID player);

    void onServerStop();
}
