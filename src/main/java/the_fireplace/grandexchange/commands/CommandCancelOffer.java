package the_fireplace.grandexchange.commands;

import com.google.common.collect.Lists;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import the_fireplace.grandexchange.market.BuyOffer;
import the_fireplace.grandexchange.market.SellOffer;
import the_fireplace.grandexchange.util.TransactionDatabase;
import the_fireplace.grandexchange.util.translation.TranslationUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class CommandCancelOffer extends CommandBase {
    @Override
    @Nonnull
    public String getName() {
        return "canceloffer";
    }

    @Override
    @Nonnull
    public String getUsage(@Nullable ICommandSender sender) {
        return TranslationUtil.getRawTranslationString(sender, "commands.ge.canceloffer.usage");
    }

    @Override
    public void execute(@Nullable MinecraftServer server, @Nonnull ICommandSender sender, @Nullable String[] args) throws CommandException {
        if(sender instanceof EntityPlayerMP) {
            List<BuyOffer> buyOffers = Lists.newArrayList();
            for (List<BuyOffer> offerList : TransactionDatabase.getBuyOffers().values())
                buyOffers.addAll(offerList);
            List<SellOffer> sellOffers = Lists.newArrayList();
            for (List<SellOffer> offerList : TransactionDatabase.getSellOffers().values())
                sellOffers.addAll(offerList);
            buyOffers.removeIf(offer -> !offer.getOwner().equals(((EntityPlayerMP) sender).getUniqueID()));
            sellOffers.removeIf(offer -> !offer.getOwner().equals(((EntityPlayerMP) sender).getUniqueID()));

            if (args != null && args.length >= 2) {
                boolean enableBuySearch = false, enableSellSearch = false;
                String filter = args[1];
                Integer meta = null, price = null;
                switch(args[0].toLowerCase()) {
                    case "buy":
                    case "b":
                        enableBuySearch = true;
                        break;
                    case "sell":
                    case "s":
                        enableSellSearch = true;
                        break;
                    case "any":
                    case "a":
                    case "*":
                        enableBuySearch = enableSellSearch = true;
                        break;
                    default:
                        throw new WrongUsageException(getUsage(sender));
                }

                if(filter.matches("^[a-zA-Z_]*$")) filter = "minecraft:"+ filter;
                else if(filter.equals("any") || filter.equals("*")) filter = ".*";

                if(args.length >= 3) {
                    meta = parseInt(args[2]);
                    if(meta < 0)
                        throw new CommandException(TranslationUtil.getRawTranslationString(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.common.invalid_meta"));
                }

                if(args.length >= 4)
                    price = parseInt(args[3]);

                if(enableBuySearch)
                    for (BuyOffer offer : buyOffers) {
                        if (offer.getItemResourceName().matches(filter) && (meta == null || meta == offer.getItemMeta()) && (price == null || price == offer.getPrice())) {
                            TransactionDatabase.getInstance().cancelOffer(offer);
                            sender.sendMessage(TranslationUtil.getTranslation(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.canceloffer.success_buy"));
                        }
                    }

                if(enableSellSearch)
                    for (SellOffer offer : sellOffers) {
                        if (offer.getItemResourceName().matches(filter) && (meta == null || meta == offer.getItemMeta()) && (price == null || price == offer.getPrice())) {
                            TransactionDatabase.getInstance().cancelOffer(offer);
                            sender.sendMessage(TranslationUtil.getTranslation(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.canceloffer.success_sell"));
                        }
                    }

                if (buyOffers.isEmpty() && sellOffers.isEmpty())
                    sender.sendMessage(TranslationUtil.getTranslation(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.common.not_buying_or_selling"));
            } else
                throw new WrongUsageException(getUsage(sender));
        } else
            throw new CommandException(TranslationUtil.getRawTranslationString(sender, "commands.ge.common.not_player"));
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }
}
