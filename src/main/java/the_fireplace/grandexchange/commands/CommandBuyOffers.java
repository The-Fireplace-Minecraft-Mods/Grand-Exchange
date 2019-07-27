package the_fireplace.grandexchange.commands;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.Lists;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.event.ClickEvent;
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
        return "/ge buyoffers [filter] [page]";
    }

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
            
            int pageNum = page;
            
            String buysearch;
            List<String> buyresults = Lists.newArrayList();
            if(args.length >= 1){
                buysearch = args[0]==null ? ".*" : args[0];
                if(buysearch.matches("^[a-zA-Z_]*$")) buysearch = "minecraft:"+ buysearch;
                else if(buysearch.equals("any") || buysearch.equals("*")) buysearch = ".*";
                buyresults = Utils.getListOfStringsMatchingString(buysearch, Utils.getBuyNames(offers));
                
                
                List<BuyOffer> tmp = new ArrayList<BuyOffer>();
                for(BuyOffer offer : offers){
                	if(buyresults.contains(offer.getItemResourceName()))
                		tmp.add(offer);
                }
                offers = tmp;
                
            } else {
            	buysearch = ".*";
            }
            
            int resultsOnPage = 5;
            int total = offers.size()%resultsOnPage >0 ? (offers.size()/resultsOnPage)+1 : offers.size()/resultsOnPage;

            sender.sendMessage(new TextComponentString("/ge buyoffers " + buysearch + " " + (pageNum+1 > total ? pageNum : pageNum+1)));

            ITextComponent counter = new TextComponentString("Page: " + pageNum + "/" + total);
            ITextComponent top = new TextComponentString(MinecraftColors.GREEN + "-----------------").appendSibling(counter).appendText(MinecraftColors.GREEN + "-------------------");
            
            //Expand page to be the first entry on the page
            page *= resultsOnPage;
            //Subtract 50 because the first page starts with entry 0
            page -= resultsOnPage;
            int termLength = resultsOnPage;
            boolean result=false;
            sender.sendMessage(top);
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
            if(!result && args.length >= 1 && !offers.isEmpty())
            	sender.sendMessage(new TextComponentString(MinecraftColors.RED + "No results found"));
            if(offers.isEmpty())
                sender.sendMessage(new TextComponentString("Nobody is buying anything."));
            
            ITextComponent nextButton = new TextComponentString("[Next]").setStyle(new Style().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ge buyoffers " + buysearch + " " + (pageNum+1 > total ? pageNum : pageNum+1))));
            ITextComponent prevButton = new TextComponentString("[Previous]").setStyle(new Style().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ge buyoffers " +  buysearch + " " + (pageNum-1 < 1 ? pageNum : pageNum-1))));
            ITextComponent bottom = new TextComponentString(MinecraftColors.GREEN + "---------------").appendSibling(prevButton).appendText(MinecraftColors.GREEN + "---").appendSibling(nextButton).appendText(MinecraftColors.GREEN + "-------------");
            
            sender.sendMessage(bottom);
        } else
            throw new WrongUsageException(getUsage(sender));
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }
}
