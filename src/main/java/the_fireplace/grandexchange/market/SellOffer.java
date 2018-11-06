package the_fireplace.grandexchange.market;

import java.util.UUID;

public class SellOffer extends Offer {
	SellOffer(String item, int amount, int price, UUID owner) {
		super("sell", item, amount, price, owner);
	}
}
