package the_fireplace.grandexchange.compat;

import java.util.UUID;

public interface TaxDistributor {
    void distributeTax(UUID player, long amount);
}
