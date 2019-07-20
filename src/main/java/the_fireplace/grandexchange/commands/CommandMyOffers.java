package the_fireplace.grandexchange.commands;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.Lists;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import the_fireplace.grandeconomy.api.GrandEconomyApi;
import the_fireplace.grandexchange.market.BuyOffer;
import the_fireplace.grandexchange.market.Offer;
import the_fireplace.grandexchange.market.SellOffer;
import the_fireplace.grandexchange.util.MinecraftColors;
import the_fireplace.grandexchange.util.TransactionDatabase;
import the_fireplace.grandexchange.util.Utils;

public class CommandMyOffers extends CommandBase {
    @Override
    @Nonnull
    public String getName() {
        return "myoffers";
    }

    @Override
    @Nonnull
    public String getUsage(@Nullable ICommandSender sender) {
        return "/ge myoffers [filter] [page]";
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
            if (args != null && args.length == 2)
                try {
                    page = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    throw new CommandException("Invalid page number!");
                }

            List<String> buyresults = Lists.newArrayList();
            if(args != null && args.length >= 1){
                String buysearch = args[0];
                buyresults = Utils.getListOfStringsMatchingString(buysearch, Utils.getBuyNames(buyOffers));
            }

            //Expand page to be the first entry on the page
            page *= 50;
            //Subtract 50 because the first page starts with entry 0
            page -= 50;
            int orderIndex = page;
            int termLength = 50;
            boolean buyresult = false;
            for (Offer offer : buyOffers) {
                if (page-- > 0)
                    continue;
                if (termLength-- <= 0)
                    break;

                if(args.length >= 1)
                {
                    if(buyresults.contains(offer.getItemResourceName())){
                    	buyresult=true;
                        sender.sendMessage(new TextComponentString(MinecraftColors.BLUE + offer.getAmount() + ' ' + offer.getItemResourceName() + ' ' + offer.getItemMeta() + (offer.getNbt() != null ? " with NBT "+offer.getNbt() : "") + " wanted for " + offer.getPrice() + ' ' + GrandEconomyApi.getCurrencyName(offer.getPrice()) + " each"));
                    }
                    
                } else {
                    sender.sendMessage(new TextComponentString(MinecraftColors.YELLOW + orderIndex++ + ". " + MinecraftColors.BLUE + offer.getAmount() + ' ' + offer.getItemResourceName() + ' ' + offer.getItemMeta() + (offer.getNbt() != null ? " with NBT "+offer.getNbt() : "") + " wanted for " + offer.getPrice() + ' ' + GrandEconomyApi.getCurrencyName(offer.getPrice()) + " each"));
                }
            }
            if(!buyresult && args.length >= 1){
            	sender.sendMessage(new TextComponentString(MinecraftColors.RED + "No buy results found"));
            }

            List<String> sellresults = Lists.newArrayList();
            if(args != null && args.length >= 1){
                String sellsearch = args[0];
                sellresults = Utils.getListOfStringsMatchingString(sellsearch, Utils.getSellNames(sellOffers));
            }

            boolean sellresult=false;
            for (SellOffer offer : sellOffers) {
                if (page-- > 0)
                    continue;
                if (termLength-- <= 0)
                    break;
                if(args.length >= 1)
                {
                    if(sellresults.contains(offer.getItemResourceName())){
                    	sellresult=true;
                        sender.sendMessage(new TextComponentString(MinecraftColors.PURPLE + offer.getAmount() + ' ' + offer.getItemResourceName() + ' ' + offer.getItemMeta() + (offer.getNbt() != null ? " with NBT "+offer.getNbt() : "") + " being sold for " + offer.getPrice() + ' ' + GrandEconomyApi.getCurrencyName(offer.getPrice()) + " each"));
                    }
                } else {
                    sender.sendMessage(new TextComponentString(MinecraftColors.YELLOW + orderIndex++ + ". " + MinecraftColors.PURPLE + offer.getAmount() + ' ' + offer.getItemResourceName() + ' ' + offer.getItemMeta() + (offer.getNbt() != null ? " with NBT "+offer.getNbt() : "") + " being sold for " + offer.getPrice() + ' ' + GrandEconomyApi.getCurrencyName(offer.getPrice()) + " each"));
                }
            }
            if(!sellresult && args.length >= 1){
            	sender.sendMessage(new TextComponentString(MinecraftColors.RED + "No sell results found"));
            }

            if(buyOffers.isEmpty() && sellOffers.isEmpty())
                sender.sendMessage(new TextComponentString("You are not buying or selling anything."));

            return;
        }
        throw new WrongUsageException(getUsage(sender));
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }
}
