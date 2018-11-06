package the_fireplace.grandexchange.market;

import java.util.UUID;

public class BuyOffer extends Offer {
	BuyOffer(String item, int amount, int price, UUID owner) {
		super("buy", item, amount, price, owner);
	}
}
