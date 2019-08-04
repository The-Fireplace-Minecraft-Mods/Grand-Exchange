package the_fireplace.grandexchange.market;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.text.ITextComponent;
import the_fireplace.grandeconomy.api.GrandEconomyApi;
import the_fireplace.grandexchange.util.TextStyles;
import the_fireplace.grandexchange.util.translation.TranslationUtil;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.UUID;

public class BuyOffer extends Offer implements Serializable {
	private static final long serialVersionUID = 0x42069;
	public BuyOffer(String item, int meta, int amount, long price, UUID owner, @Nullable String nbt) {
		super("buy", item, meta, amount, price, owner, nbt);
	}

	@Override
	public ITextComponent getOfferChatMessage(ICommandSender sender) {
		if(getNbt() != null)
			return TranslationUtil.getTranslation(sender, "ge.buyoffer_nbt", getAmount(), getItemResourceName(), getItemMeta(), getNbt(), getPrice(), GrandEconomyApi.getCurrencyName(getPrice())).setStyle(TextStyles.BLUE);
		else
			return TranslationUtil.getTranslation(sender, "ge.buyoffer", getAmount(), getItemResourceName(), getItemMeta(), getPrice(), GrandEconomyApi.getCurrencyName(getPrice())).setStyle(TextStyles.BLUE);
	}
}
