package the_fireplace.grandexchange.market;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.UUID;

public class SellOffer extends Offer implements Serializable {
	private static final long serialVersionUID = 0x42069;
	public SellOffer(String item, int meta, int amount, long price, UUID owner, @Nullable String nbt) {
		super("sell", item, meta, amount, price, owner, nbt);
	}
}
