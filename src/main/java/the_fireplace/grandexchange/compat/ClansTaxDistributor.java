package the_fireplace.grandexchange.compat;

import the_fireplace.clans.clan.membership.PlayerClans;
import the_fireplace.clans.economy.Economy;

import java.util.Collection;
import java.util.UUID;

public class ClansTaxDistributor implements TaxDistributor {
    @Override
    public void distributeTax(UUID player, double amount) {
        Collection<UUID> playerClans = PlayerClans.getClansPlayerIsIn(player);
        if (!playerClans.isEmpty()) {
            double dividedAmount = amount / playerClans.size();
            for (UUID clan : playerClans) {
                Economy.addAmount(dividedAmount, clan);
            }
        }
    }
}
