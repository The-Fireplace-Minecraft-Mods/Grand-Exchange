package the_fireplace.grandexchange.compat;

import the_fireplace.clans.api.ClansAPI;
import the_fireplace.clans.cache.ClanCache;
import the_fireplace.clans.data.PlayerData;
import the_fireplace.clans.model.Clan;

import java.util.List;
import java.util.UUID;

public class ClansTaxDistributor implements TaxDistributor {
    @Override
    public void distributeTax(UUID player, long amount) {
        List<Clan> playerClans = ClanCache.getPlayerClans(player);
        long dividedAmount = amount / playerClans.size();
        long remainder = amount % playerClans.size();
        for(Clan clan: playerClans)
            ClansAPI.getPaymentHandler().addAmount(dividedAmount, clan.getId());
        ClansAPI.getPaymentHandler().addAmount(remainder, PlayerData.getDefaultClan(player));
    }
}
