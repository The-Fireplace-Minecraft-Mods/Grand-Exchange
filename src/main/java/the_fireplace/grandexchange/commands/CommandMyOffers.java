package the_fireplace.grandexchange.commands;

import com.google.common.collect.Lists;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import the_fireplace.grandexchange.market.ExchangeManager;
import the_fireplace.grandexchange.market.Offer;
import the_fireplace.grandexchange.market.OfferType;
import the_fireplace.grandexchange.util.ChatPageUtil;
import the_fireplace.grandexchange.util.TextStyles;
import the_fireplace.grandexchange.util.Utils;
import the_fireplace.grandexchange.util.translation.TranslationUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CommandMyOffers extends CommandBase {
    @Override
    @Nonnull
    public String getName() {
        return "myoffers";
    }

    @Override
    @Nonnull
    public String getUsage(@Nullable ICommandSender sender) {
        return TranslationUtil.getRawTranslationString(sender, "commands.ge.myoffers.usage");
    }

    @Override
    public void execute(@Nullable MinecraftServer server, @Nonnull ICommandSender sender, @Nullable String[] args) throws CommandException {
        if(sender instanceof EntityPlayerMP) {
            List<Offer> buyOffers = Lists.newArrayList(ExchangeManager.getOffers(OfferType.BUY, ((EntityPlayerMP) sender).getUniqueID()));
            List<Offer> sellOffers = Lists.newArrayList(ExchangeManager.getOffers(OfferType.SELL, ((EntityPlayerMP) sender).getUniqueID()));

            int page = 1;
            if (args != null && args.length == 2)
                try {
                    page = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    throw new CommandException(TranslationUtil.getRawTranslationString(sender, "commands.ge.common.invalid_page"));
                }
            
            String search;
            if(args != null && args.length >= 1){
            	search = args[0];
                if(search.matches("^[a-zA-Z_]*$")) search = "minecraft:"+ search;
                else if(search.equals("any") || search.equals("*")) search = ".*";
            } else {
            	search = ".*";
            }

            ArrayList<ITextComponent> messages = Lists.newArrayList();

            final List<String> buyresults = Utils.getListOfStringsMatchingString(search, Utils.getOfferNames(buyOffers));
            buyOffers.removeIf(offer -> !buyresults.contains(offer.getItemResourceName()));

            for (Offer offer : buyOffers) {
                messages.add(offer.getOfferChatMessage(sender));
            }
            
            if(buyOffers.isEmpty())
            	sender.sendMessage(TranslationUtil.getTranslation(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.myoffers.no_buy_results").setStyle(TextStyles.RED));

            final List<String> sellresults = Utils.getListOfStringsMatchingString(search, Utils.getOfferNames(sellOffers));
            sellOffers.removeIf(offer -> !sellresults.contains(offer.getItemResourceName()));

            for (Offer offer : sellOffers) {
                messages.add(offer.getOfferChatMessage(sender));
            }
            if(sellOffers.isEmpty())
                sender.sendMessage(TranslationUtil.getTranslation(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.myoffers.no_sell_results").setStyle(TextStyles.RED));

            if(buyOffers.isEmpty() && sellOffers.isEmpty())
                sender.sendMessage(TranslationUtil.getTranslation(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.common.not_buying_or_selling"));
            else
                ChatPageUtil.showPaginatedChat(sender, "/ge myoffers " + search + " %s", messages, page);
        } else
            throw new CommandException(TranslationUtil.getRawTranslationString(sender, "commands.ge.common.not_player"));
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }
}
