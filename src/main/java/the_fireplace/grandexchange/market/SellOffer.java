package the_fireplace.grandexchange.market;

import java.io.Serializable;
import java.util.UUID;

public class SellOffer extends Offer implements Serializable {
	public SellOffer(String item, int meta, int amount, long price, UUID owner) {
		super("sell", item, meta, amount, price, owner);
	}
}
