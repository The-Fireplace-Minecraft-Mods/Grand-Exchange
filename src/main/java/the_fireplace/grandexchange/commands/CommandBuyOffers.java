package the_fireplace.grandexchange.commands;

import com.google.common.collect.Lists;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import the_fireplace.grandeconomy.api.GrandEconomyApi;
import the_fireplace.grandexchange.TransactionDatabase;
import the_fireplace.grandexchange.market.BuyOffer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CommandBuyOffers extends CommandBase {
    private static final String blue = "§3";
    @Override
    public String getName() {
        return "buyoffers";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/ge buyoffers [page]";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if(args.length <= 1) {
            List<BuyOffer> offers = Lists.newArrayList();
            for (List<BuyOffer> offerList : TransactionDatabase.getBuyOffers().values())
                offers.addAll(offerList);
            int page = 1;
            if (args.length == 1)
                try {
                    page = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    throw new CommandException("Invalid page number!");
                }
            //Expand page to be the first entry on the page
            page *= 50;
            //Subtract 50 because the first page starts with entry 0
            page -= 50;
            int termLength = 50;
            for (BuyOffer offer : offers) {
                if (page-- > 0)
                    continue;
                if (termLength-- <= 0)
                    break;
                sender.sendMessage(new TextComponentString(blue + offer.getAmount() + ' ' + offer.getItemResourceName() + ' ' + offer.getItemMeta() + " wanted for " + offer.getPrice() + ' ' + GrandEconomyApi.getCurrencyName(offer.getAmount()) + " each"));
            }
            if(offers.isEmpty())
                sender.sendMessage(new TextComponentString("Nobody is buying anything."));
        } else
            throw new WrongUsageException("/ge buyoffers [page]");
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }
}
