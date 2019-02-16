package the_fireplace.grandexchange.market;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.UUID;

public class SellOffer extends Offer implements Serializable {
	private static final long serialVersionUID = 0x5EED1355;
	public SellOffer(String item, int amount, long price, UUID owner, @Nullable String nbt) {
		super("sell", item, amount, price, owner, nbt);
	}
}
