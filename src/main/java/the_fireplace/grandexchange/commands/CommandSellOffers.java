package the_fireplace.grandexchange.commands;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.Lists;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import the_fireplace.grandeconomy.api.GrandEconomyApi;
import the_fireplace.grandexchange.market.SellOffer;
import the_fireplace.grandexchange.util.MinecraftColors;
import the_fireplace.grandexchange.util.TransactionDatabase;
import the_fireplace.grandexchange.util.Utils;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CommandSellOffers extends CommandBase {
    @Override
    public String getName() {
        return "selloffers";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/ge selloffers [filter] [page]";
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
                    throw new CommandException("Invalid page number!");
                }
            //Expand page to be the first entry on the page
            page *= 50;
            //Subtract 50 because the first page starts with entry 0
            page -= 50;
            int termLength = 50;
            List<String> sellresults = Lists.newArrayList();
            if(args.length >= 1){
                String sellsearch = args[0];
                if(sellsearch.matches("^[a-zA-Z_]*$")) sellsearch = "minecraft:"+ sellsearch;
                else if(sellsearch.equals("any") || sellsearch.equals("*")) sellsearch = ".*";
                sellresults = Utils.getListOfStringsMatchingString(sellsearch, Utils.getSellNames(offers));
            }

            boolean result = false;
            for (SellOffer offer : offers) {
                if (page-- > 0)
                    continue;
                if (termLength-- <= 0)
                    break;
                if(args.length >= 1)
                {
                    if(sellresults.contains(offer.getItemResourceName())){
                    	result=true;
                        sender.sendMessage(offer.getOfferChatMessage(sender));
                    }
                } else {
                    sender.sendMessage(offer.getOfferChatMessage(sender));
                }
            }
            if(!result && args.length >= 1)
            	sender.sendMessage(new TextComponentString(MinecraftColors.RED + "No results found"));
            if(offers.isEmpty())
                sender.sendMessage(new TextComponentString("Nobody is selling anything."));
        } else
            throw new WrongUsageException(getUsage(sender));
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }
}
