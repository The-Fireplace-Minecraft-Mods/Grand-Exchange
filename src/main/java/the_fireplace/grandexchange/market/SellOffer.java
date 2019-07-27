package the_fireplace.grandexchange.market;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import the_fireplace.grandeconomy.api.GrandEconomyApi;
import the_fireplace.grandexchange.util.MinecraftColors;
import the_fireplace.grandexchange.util.TextStyles;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.UUID;

public class SellOffer extends Offer implements Serializable {
	private static final long serialVersionUID = 0x42069;
	public SellOffer(String item, int meta, int amount, long price, UUID owner, @Nullable String nbt) {
		super("sell", item, meta, amount, price, owner, nbt);
	}

	@Override
	public ITextComponent getOfferChatMessage(ICommandSender sender) {
		return new TextComponentString(getAmount() + ' ' + getItemResourceName() + ' ' + getItemMeta() + (getNbt() != null ? " with NBT "+getNbt() : "") + " being sold for " + getPrice() + ' ' + GrandEconomyApi.getCurrencyName(getPrice()) + " each").setStyle(TextStyles.DARK_PURPLE);
	}
}
