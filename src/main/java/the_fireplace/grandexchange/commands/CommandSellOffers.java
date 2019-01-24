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
import the_fireplace.grandexchange.MinecraftColors;
import the_fireplace.grandexchange.TransactionDatabase;
import the_fireplace.grandexchange.market.SellOffer;

import javax.annotation.ParametersAreNonnullByDefault;
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
        return "/ge selloffers [page]";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if(args.length <= 1) {
            List<SellOffer> offers = Lists.newArrayList();
            for (List<SellOffer> offerList : TransactionDatabase.getSellOffers().values())
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
            for (SellOffer offer : offers) {
                if (page-- > 0)
                    continue;
                if (termLength-- <= 0)
                    break;
                sender.sendMessage(new TextComponentString(MinecraftColors.PURPLE + offer.getAmount() + ' ' + offer.getItemResourceName() + ' ' + offer.getItemMeta() + " being sold for " + offer.getPrice() + ' ' + GrandEconomyApi.getCurrencyName(offer.getAmount()) + " each"));
            }
            if(offers.isEmpty())
                sender.sendMessage(new TextComponentString("Nobody is selling anything."));
        } else
            throw new WrongUsageException("/ge selloffers [page]");
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }
}
