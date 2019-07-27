package the_fireplace.grandexchange.market;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.UUID;

@SuppressWarnings("WeakerAccess")
public abstract class Offer implements Serializable {
    private static final long serialVersionUID = 0x42069;

	protected String item, offertype, nbt;
	protected int amount, meta;
	protected long price;
	protected UUID owner;
	protected long timestamp = System.currentTimeMillis();
	protected Offer(String offertype, String item, int meta, int amount, long price, UUID owner, @Nullable String nbt){
		this.offertype = offertype;
		this.item = item;
		this.meta = meta;
		this.amount = amount;
		this.price = price;
		this.owner = owner;
		this.nbt = nbt;
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
	public final String getNbt(){
		return nbt;
	}

	public void decrementAmount(int reduceBy){
		timestamp = System.currentTimeMillis();
		amount -= reduceBy;
	}

	public abstract ITextComponent getOfferChatMessage(ICommandSender sender);
}
