package the_fireplace.grandexchange.market;

import java.util.UUID;

public abstract class Offer {
	String item, offertype;
	int amount, price;
	UUID owner;
	long timestamp = System.currentTimeMillis();
	Offer(String offertype, String item, int amount, int price, UUID owner){
		this.offertype = offertype;
		this.item = item;
		this.amount = amount;
		this.price = price;
		this.owner = owner;
	}

	public final String getItem(){
		return item;
	}
	public final int getAmount(){
		return amount;
	}
	public final int getPrice(){
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
