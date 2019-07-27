package the_fireplace.grandexchange.market;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.UUID;

public class BuyOffer extends Offer implements Serializable {
	private static final long serialVersionUID = 0x42069;
	public BuyOffer(String item, int meta, int amount, long price, UUID owner, @Nullable String nbt) {
		super("buy", item, meta, amount, price, owner, nbt);
	}
}
