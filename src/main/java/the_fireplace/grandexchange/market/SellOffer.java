package the_fireplace.grandexchange.market;

import java.util.UUID;

public class SellOffer extends Offer {
	public SellOffer(String item, int meta, int amount, long price, UUID owner) {
		super("sell", item, meta, amount, price, owner);
	}
}
