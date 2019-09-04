package the_fireplace.grandexchange.commands;

import com.google.common.collect.Lists;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import the_fireplace.grandexchange.market.SellOffer;
import the_fireplace.grandexchange.util.ChatPageUtil;
import the_fireplace.grandexchange.util.TextStyles;
import the_fireplace.grandexchange.util.TransactionDatabase;
import the_fireplace.grandexchange.util.Utils;
import the_fireplace.grandexchange.util.translation.TranslationUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CommandSellOffers extends CommandBase {
    @Override
    public String getName() {
        return "selloffers";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return TranslationUtil.getRawTranslationString(sender, "commands.ge.selloffers.usage");
    }

    @SuppressWarnings("Duplicates")
    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if(args.length <= 2) {
            List<SellOffer> offers = Lists.newArrayList();
            for (List<SellOffer> offerList : TransactionDatabase.getSellOffers().values())
                offers.addAll(offerList);
            int page = 1;
            if (args.length == 2)
                try {
                    page = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    throw new CommandException(TranslationUtil.getRawTranslationString(sender, "commands.ge.common.invalid_page"));
                }

            String sellsearch;
            if(args.length >= 1){
                sellsearch = args[0];
                if(sellsearch.matches("^[a-zA-Z_]*$")) sellsearch = "minecraft:"+ sellsearch;
                else if(sellsearch.equals("any") || sellsearch.equals("*")) sellsearch = ".*";
                final List<String> sellResults = Utils.getListOfStringsMatchingString(sellsearch, Utils.getSellNames(offers));

                offers.removeIf(offer -> !sellResults.contains(offer.getItemResourceName()));
            } else {
            	sellsearch = ".*";
            }

            ArrayList<ITextComponent> messages = Lists.newArrayList();

            for (SellOffer offer : offers)
                messages.add(offer.getOfferChatMessage(sender));

            if(offers.isEmpty())
                sender.sendMessage(TranslationUtil.getTranslation(sender, "commands.ge.selloffers.none"));
            else
                ChatPageUtil.showPaginatedChat(sender, "/ge selloffers " + sellsearch + " %s", messages, page);
        } else
            throw new WrongUsageException(getUsage(sender));
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }
}
