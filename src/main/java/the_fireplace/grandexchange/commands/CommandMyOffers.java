package the_fireplace.grandexchange.commands;

import com.google.common.collect.Lists;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import org.apache.commons.lang3.tuple.Pair;
import the_fireplace.grandeconomy.api.GrandEconomyApi;
import the_fireplace.grandexchange.TransactionDatabase;
import the_fireplace.grandexchange.market.BuyOffer;
import the_fireplace.grandexchange.market.SellOffer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class CommandMyOffers extends CommandBase {
    private static final String blue = "§3";
    private static final String purple = "§5";
    private static final String yellow = "§e";
    @Override
    @Nonnull
    public String getName() {
        return "myoffers";
    }

    @Override
    @Nonnull
    public String getUsage(@Nullable ICommandSender sender) {
        return "/ge myoffers [page]";
    }

    @Override
    public void execute(@Nullable MinecraftServer server, @Nonnull ICommandSender sender, @Nullable String[] args) throws CommandException {
        if(sender instanceof EntityPlayerMP) {
            List<BuyOffer> buyOffers = Lists.newArrayList();
            for (List<BuyOffer> offerList : TransactionDatabase.getBuyOffers().values())
                buyOffers.addAll(offerList);
            List<SellOffer> sellOffers = Lists.newArrayList();
            for (List<SellOffer> offerList : TransactionDatabase.getSellOffers().values())
                sellOffers.addAll(offerList);
            buyOffers.removeIf(offer -> !offer.getOwner().equals(((EntityPlayerMP) sender).getUniqueID()));
            sellOffers.removeIf(offer -> !offer.getOwner().equals(((EntityPlayerMP) sender).getUniqueID()));

            int page = 1;
            if (args != null && args.length == 1)
                try {
                    page = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    throw new CommandException("Invalid page number!");
                }
            //Expand page to be the first entry on the page
            page *= 50;
            //Subtract 50 because the first page starts with entry 0
            page -= 50;
            int orderIndex = page;
            int termLength = 50;
            for (BuyOffer offer : buyOffers) {
                if (page-- > 0)
                    continue;
                if (termLength-- <= 0)
                    break;
                sender.sendMessage(new TextComponentString(yellow + orderIndex++ + ". " + blue + offer.getAmount() + ' ' + offer.getItemResourceName() + ' ' + offer.getItemMeta() + " wanted for " + offer.getPrice() + ' ' + GrandEconomyApi.getCurrencyName(offer.getPrice()) + " each"));
            }

            for (SellOffer offer : sellOffers) {
                if (page-- > 0)
                    continue;
                if (termLength-- <= 0)
                    break;
                sender.sendMessage(new TextComponentString(yellow + orderIndex++ + ". " + purple + offer.getAmount() + ' ' + offer.getItemResourceName() + ' ' + offer.getItemMeta() + " being sold for " + offer.getPrice() + ' ' + GrandEconomyApi.getCurrencyName(offer.getPrice()) + " each"));
            }

            if(buyOffers.isEmpty() && sellOffers.isEmpty())
                sender.sendMessage(new TextComponentString("You are not buying or selling anything."));
        }
        //noinspection RedundantArrayCreation
        throw new WrongUsageException("/ge myoffers [page]", new Object[0]);
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }
}
