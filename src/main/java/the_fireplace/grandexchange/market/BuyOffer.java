package the_fireplace.grandexchange.market;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.UUID;

public class BuyOffer extends Offer implements Serializable {
	private static final long serialVersionUID = 0x5EED1355;
	public BuyOffer(String item, int amount, long price, UUID owner, @Nullable String nbt) {
		super("buy", item, amount, price, owner, nbt);
	}
}
