package the_fireplace.grandexchange.commands;

import com.google.common.collect.Lists;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import the_fireplace.grandeconomy.api.GrandEconomyApi;
import the_fireplace.grandexchange.TransactionDatabase;
import the_fireplace.grandexchange.market.BuyOffer;
import the_fireplace.grandexchange.market.SellOffer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class CommandSellOffers extends CommandBase {
    private static final String purple = "§5";
    @Override
    @Nonnull
    public String getName() {
        return "selloffers";
    }

    @Override
    @Nonnull
    public String getUsage(@Nullable ICommandSender sender) {
        return "/selloffers [page]";
    }

    @Override
    public void execute(@Nullable MinecraftServer server, @Nonnull ICommandSender sender, @Nullable String[] args) throws CommandException {
        List<SellOffer> offers = Lists.newArrayList();
        for(List<SellOffer> offerList : TransactionDatabase.getSellOffers().values())
            offers.addAll(offerList);
        int page = 1;
        if(args != null && args.length == 1)
        try {
            page = Integer.parseInt(args[0]);
        } catch(NumberFormatException e) {
            throw new CommandException("Invalid page number!");
        }
        //Expand page to be the first entry on the page
        page *= 50;
        //Subtract 50 because the first page starts with entry 0
        page -= 50;
        int termLength = 50;
        for(SellOffer offer : offers) {
            if(page-- > 0)
                continue;
            if(termLength-- <= 0)
                break;
            sender.sendMessage(new TextComponentString(purple + offer.getAmount() + ' ' + offer.getItemResourceName() + ' ' + offer.getItemMeta() + " being sold for " + offer.getPrice() + ' ' + GrandEconomyApi.getCurrencyName(offer.getAmount()) + " each"));
        }
        //noinspection RedundantArrayCreation
        throw new WrongUsageException("/selloffers [page]", new Object[0]);
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }
}
