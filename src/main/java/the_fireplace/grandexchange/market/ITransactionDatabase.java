package the_fireplace.grandexchange.market;

import net.minecraft.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.UUID;

public interface ITransactionDatabase {
    /**
     * Add an offer to the database
     * @return the offer id
     */
    long addOffer(OfferType type, String item, int meta, int amount, long price, UUID owner, @Nullable String nbt);

    /**
     * Remove the offer with the matching ID from the database.
     * @param offerId
     * The ID of the offer to be removed
     * @return
     * The offer that was removed, or null if not found.
     */
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
}
