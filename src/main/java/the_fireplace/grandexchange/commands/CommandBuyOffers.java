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
import net.minecraft.util.text.TextComponentString;
import the_fireplace.grandexchange.market.BuyOffer;
import the_fireplace.grandexchange.util.*;

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
            
            String buysearch;
            List<String> buyresults = Lists.newArrayList();
            if(args.length >= 1){
                buysearch = args[0]==null ? ".*" : args[0];
                if(buysearch.matches("^[a-zA-Z_]*$")) buysearch = "minecraft:"+ buysearch;
                else if(buysearch.equals("any") || buysearch.equals("*")) buysearch = ".*";
                buyresults = Utils.getListOfStringsMatchingString(buysearch, Utils.getBuyNames(offers));
                final List<String> finalBuyResults = buyresults;

                offers.removeIf(offer -> !finalBuyResults.contains(offer.getItemResourceName()));
            } else {
            	buysearch = ".*";
            }

            ArrayList<ITextComponent> messages = Lists.newArrayList();
            boolean result=false;
            for (BuyOffer offer : offers) {
                if(args.length >= 1)
                {
                    if(buyresults.contains(offer.getItemResourceName())){
                    	result=true;
                        messages.add(offer.getOfferChatMessage(sender));
                    }
                } else {
                    messages.add(offer.getOfferChatMessage(sender));
                }
            }
            if(!result && args.length >= 1 && !offers.isEmpty())
            	sender.sendMessage(new TextComponentString("No results found").setStyle(TextStyles.RED));
            else if(offers.isEmpty())
                sender.sendMessage(new TextComponentString("Nobody is buying anything."));
            else
                ChatPageUtil.showPaginatedChat(sender, "/ge buyoffers " + buysearch + " %s", messages, page);
        } else
            throw new WrongUsageException(getUsage(sender));
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }
}
