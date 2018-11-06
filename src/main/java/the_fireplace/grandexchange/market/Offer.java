package the_fireplace.grandexchange.market;

import org.apache.commons.lang3.tuple.Pair;

import java.util.UUID;

@SuppressWarnings("WeakerAccess")
public abstract class Offer {
	protected String item, offertype;
	protected int amount, meta;
	protected long price;
	protected UUID owner;
	protected long timestamp = System.currentTimeMillis();
	protected Offer(String offertype, String item, int meta, int amount, long price, UUID owner){
		this.offertype = offertype;
		this.item = item;
		this.meta = meta;
		this.amount = amount;
		this.price = price;
		this.owner = owner;
	}

	public final Pair<String, Integer> getItemPair(){
		return Pair.of(item, meta);
	}
	public final String getItemResourceName(){
		return item;
	}
	public final int getAmount(){
		return amount;
	}
	public final int getItemMeta(){
		return meta;
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

	public void decrementAmount(int reduceBy){
		timestamp = System.currentTimeMillis();
		amount -= reduceBy;
	}
}
