package the_fireplace.grandexchange.commands;

import com.google.common.collect.Lists;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import the_fireplace.grandexchange.market.BuyOffer;
import the_fireplace.grandexchange.market.Offer;
import the_fireplace.grandexchange.market.SellOffer;
import the_fireplace.grandexchange.util.ChatPageUtil;
import the_fireplace.grandexchange.util.TextStyles;
import the_fireplace.grandexchange.util.TransactionDatabase;
import the_fireplace.grandexchange.util.Utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

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
            
            String search;
            if(args != null && args.length >= 1){
            	search = args[0];
                if(search.matches("^[a-zA-Z_]*$")) search = "minecraft:"+ search;
                else if(search.equals("any") || search.equals("*")) search = ".*";
            } else {
            	search = ".*";
            }

            ArrayList<ITextComponent> messages = Lists.newArrayList();

            final List<String> buyresults = Utils.getListOfStringsMatchingString(search, Utils.getBuyNames(buyOffers));
            buyOffers.removeIf(offer -> !buyresults.contains(offer.getItemResourceName()));
            
            boolean buyresult = false;
            for (Offer offer : buyOffers) {
                if(args != null && args.length >= 1)
                {
                    if(buyresults.contains(offer.getItemResourceName())){
                    	buyresult=true;
                    	messages.add(offer.getOfferChatMessage(sender));
                    }
                } else {
                	messages.add(offer.getOfferChatMessage(sender));
                }
            }
            
            if(args != null && !buyresult && args.length >= 1){
            	sender.sendMessage(new TextComponentString("No buy results found").setStyle(TextStyles.RED));
            }

            final List<String> sellresults = Utils.getListOfStringsMatchingString(search, Utils.getSellNames(sellOffers));
            sellOffers.removeIf(offer -> !sellresults.contains(offer.getItemResourceName()));

            boolean sellresult=false;
            for (SellOffer offer : sellOffers) {
                if(args != null && args.length >= 1)
                {
                    if(sellresults.contains(offer.getItemResourceName())){
                    	sellresult=true;
                    	messages.add(offer.getOfferChatMessage(sender));
                    }
                } else {
                	messages.add(offer.getOfferChatMessage(sender));
                }
            }
            if(args != null && !sellresult && args.length >= 1){
            	sender.sendMessage(new TextComponentString("No sell results found").setStyle(TextStyles.RED));
            }
            

            if(buyOffers.isEmpty() && sellOffers.isEmpty())
                sender.sendMessage(new TextComponentString("You are not buying or selling anything."));
            else
                ChatPageUtil.showPaginatedChat(sender, "/ge myoffers " + search + " %s", messages, page);

 
            return;
        }
        throw new WrongUsageException(getUsage(sender));
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }
}
