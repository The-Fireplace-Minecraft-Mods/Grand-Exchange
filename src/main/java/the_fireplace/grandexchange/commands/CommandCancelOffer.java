package the_fireplace.grandexchange.commands;

import com.google.common.collect.Lists;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import the_fireplace.grandexchange.GrandExchange;
import the_fireplace.grandexchange.market.ExchangeManager;
import the_fireplace.grandexchange.market.Offer;
import the_fireplace.grandexchange.market.OfferType;
import the_fireplace.grandexchange.util.translation.TranslationUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import static the_fireplace.grandexchange.permission.PermissionManager.GE_COMMAND_PREFIX;

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
        if (args != null && args.length >= 2) {
            if(sender instanceof EntityPlayerMP) {
                List<Offer> buyOffers = Lists.newArrayList(ExchangeManager.getOffers(OfferType.BUY, ((EntityPlayerMP) sender).getUniqueID()));
                List<Offer> sellOffers = Lists.newArrayList(ExchangeManager.getOffers(OfferType.SELL, ((EntityPlayerMP) sender).getUniqueID()));
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

                try {
                    if(enableBuySearch)
                        for (Offer offer : buyOffers) {
                            if (offer.getItemResourceName().matches(filter) && (meta == null || meta == offer.getItemMeta()) && (price == null || price == offer.getPrice())) {
                                ExchangeManager.removeOffer(offer.getIdentifier());
                                ExchangeManager.returnInvestment(offer);
                                sender.sendMessage(TranslationUtil.getTranslation(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.canceloffer.success_buy", offer.getOfferChatMessage(sender.getServer()).getFormattedText()));
                            }
                        }

                    if(enableSellSearch)
                        for (Offer offer : sellOffers) {
                            if (offer.getItemResourceName().matches(filter) && (meta == null || meta == offer.getItemMeta()) && (price == null || price == offer.getPrice())) {
                                ExchangeManager.removeOffer(offer.getIdentifier());
                                ExchangeManager.returnInvestment(offer);
                                sender.sendMessage(TranslationUtil.getTranslation(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.canceloffer.success_sell", offer.getOfferChatMessage(sender.getServer()).getFormattedText()));
                            }
                        }
                } catch(PatternSyntaxException e) {
                    throw new CommandException(TranslationUtil.getStringTranslation("commands.ge.common.regex", e.getMessage()));
                }

                if (buyOffers.isEmpty() && sellOffers.isEmpty())
                    sender.sendMessage(TranslationUtil.getTranslation(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.common.not_buying_or_selling"));
            } else
                throw new WrongUsageException(getUsage(sender));
        } else if(args != null && args.length == 1) {
            Offer offer = ExchangeManager.getOffer(parseLong(args[0]));
            if(!(sender instanceof EntityPlayerMP)
                    || ((EntityPlayerMP) sender).getUniqueID().equals(offer.getOwner())
                    || (offer.getOwner() == null
                        && GrandExchange.getPermissionHandler().permissionManagementExists()
                        && GrandExchange.getPermissionHandler().hasPermission((EntityPlayerMP)sender, GE_COMMAND_PREFIX+"canceloffer.op"))) {
                Offer cancelled = ExchangeManager.removeOffer(parseLong(args[0]));
                if (cancelled != null) {
                    ExchangeManager.returnInvestment(cancelled);
                    sender.sendMessage(TranslationUtil.getTranslation(sender, "commands.ge.canceloffer.success_" + cancelled.getType().toString().toLowerCase(), cancelled.getOfferChatMessage(sender.getServer()).getFormattedText()));
                } else
                    sender.sendMessage(TranslationUtil.getTranslation(sender, "commands.ge.common.invalid_offer_number"));
            }
        } else
            throw new CommandException(TranslationUtil.getRawTranslationString(sender, "commands.ge.common.not_player"));
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }
}
