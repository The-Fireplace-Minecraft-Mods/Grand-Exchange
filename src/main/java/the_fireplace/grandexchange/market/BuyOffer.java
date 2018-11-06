package the_fireplace.grandexchange.market;

import java.util.UUID;

public class BuyOffer extends Offer {
	public BuyOffer(String item, int meta, int amount, long price, UUID owner) {
		super("buy", item, meta, amount, price, owner);
	}
}
