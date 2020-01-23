package the_fireplace.grandexchange.market;

import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.UUID;

public interface ITransactionDatabase {
    /**
     * Add an offer to the database
     */
    void addOffer(OfferType type, String item, int meta, int amount, long price, UUID owner, @Nullable String nbt);

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
}
