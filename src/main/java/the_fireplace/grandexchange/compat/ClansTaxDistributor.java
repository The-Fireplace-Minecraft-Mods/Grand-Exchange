package the_fireplace.grandexchange.compat;

import the_fireplace.clans.api.ClansAPI;
import the_fireplace.clans.cache.ClanCache;
import the_fireplace.clans.model.Clan;

import java.util.List;
import java.util.UUID;

public class ClansTaxDistributor implements TaxDistributor {
    @Override
    public void distributeTax(UUID player, double amount) {
        List<Clan> playerClans = ClanCache.getPlayerClans(player);
        if(!playerClans.isEmpty()) {
            double dividedAmount = amount / playerClans.size();
            for (Clan clan : playerClans)
                ClansAPI.getPaymentHandler().addAmount(dividedAmount, clan.getId());
        }
    }
}
