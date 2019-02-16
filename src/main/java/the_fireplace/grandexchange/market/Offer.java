package the_fireplace.grandexchange.market;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.UUID;

@SuppressWarnings("WeakerAccess")
public abstract class Offer implements Serializable {
    private static final long serialVersionUID = 0x5EED1355;

	protected String item, offertype, nbt;
	protected int amount;
	protected long price;
	protected UUID owner;
	protected long timestamp = System.currentTimeMillis();
	protected Offer(String offertype, String item, int amount, long price, UUID owner, @Nullable String nbt){
		this.offertype = offertype;
		this.item = item;
		this.amount = amount;
		this.price = price;
		this.owner = owner;
		this.nbt = nbt;
	}

	public final String getItemResourceName(){
		return item;
	}
	public final int getAmount(){
		return amount;
	}
	public final long getPrice(){
		return price;
	}
	public final UUID getOwner(){
		return owner;
	}
	public final long getTimestamp(){
		return timestamp;
	}
	public final String getNbt(){
		return nbt;
	}

	public void decrementAmount(int reduceBy){
		timestamp = System.currentTimeMillis();
		amount -= reduceBy;
	}
}
