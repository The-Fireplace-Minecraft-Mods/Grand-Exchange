package the_fireplace.grandexchange.market;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.text.ITextComponent;
import the_fireplace.grandeconomy.api.GrandEconomyApi;
import the_fireplace.grandexchange.util.TextStyles;
import the_fireplace.grandexchange.util.translation.TranslationUtil;

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
		if(getNbt() != null)
			return TranslationUtil.getTranslation(sender, "ge.selloffer_nbt", getAmount(), getItemResourceName(), getItemMeta(), getNbt(), GrandEconomyApi.toString(getPrice())).setStyle(TextStyles.DARK_PURPLE);
		else
			return TranslationUtil.getTranslation(sender, "ge.selloffer", getAmount(), getItemResourceName(), getItemMeta(), GrandEconomyApi.toString(getPrice())).setStyle(TextStyles.DARK_PURPLE);
	}
}
