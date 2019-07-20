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
import the_fireplace.grandexchange.market.BuyOffer;
import the_fireplace.grandexchange.util.MinecraftColors;
import the_fireplace.grandexchange.util.TransactionDatabase;
import the_fireplace.grandexchange.util.Utils;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CommandBuyOffers extends CommandBase {

    @Override
    public String getName() {
        return "buyoffers";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/ge buyoffers [page] [filter]";
    }

    @SuppressWarnings("Duplicates")
    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if(args.length <= 2) {
            List<BuyOffer> offers = Lists.newArrayList();
            for (List<BuyOffer> offerList : TransactionDatabase.getBuyOffers().values())
                offers.addAll(offerList);
            int page = 1;
            if (args.length == 2)
                try {
                    page = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    throw new CommandException("Invalid page number!");
                }

            List<String> buyresults = Lists.newArrayList();
            if(args.length >= 1){
                String buysearch = args[0];
                if(!buysearch.contains(":")) buysearch = "minecraft:"+ buysearch;
                if(args[0].equals("*")) buysearch = "";
                buyresults = Utils.getListOfStringsMatchingString(buysearch, Utils.getBuyNames(offers));
            }

            //Expand page to be the first entry on the page
            page *= 50;
            //Subtract 50 because the first page starts with entry 0
            page -= 50;
            int termLength = 50;
            boolean result=false;
            for (BuyOffer offer : offers) {
                if (page-- > 0)
                    continue;
                if (termLength-- <= 0)
                    break;
                if(args.length >= 1)
                {
                    if(buyresults.contains(offer.getItemResourceName())){
                    	result=true;
                        sender.sendMessage(new TextComponentString(MinecraftColors.BLUE + offer.getAmount() + ' ' + offer.getItemResourceName() + ' ' + offer.getItemMeta() + (offer.getNbt() != null ? " with NBT "+offer.getNbt() : "") + " wanted for " + offer.getPrice() + ' ' + GrandEconomyApi.getCurrencyName(offer.getPrice()) + " each"));
                    }
                } else {
                    sender.sendMessage(new TextComponentString(MinecraftColors.BLUE + offer.getAmount() + ' ' + offer.getItemResourceName() + ' ' + offer.getItemMeta() + (offer.getNbt() != null ? " with NBT "+offer.getNbt() : "") + " wanted for " + offer.getPrice() + ' ' + GrandEconomyApi.getCurrencyName(offer.getAmount()) + " each"));
                }
            }
            if(!result && args.length >= 1)
            	sender.sendMessage(new TextComponentString(MinecraftColors.RED + "No results found"));
            if(offers.isEmpty())
                sender.sendMessage(new TextComponentString("Nobody is buying anything."));
        } else
            throw new WrongUsageException(getUsage(sender));
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }
}
