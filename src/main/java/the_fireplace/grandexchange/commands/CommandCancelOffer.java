package the_fireplace.grandexchange.commands;

import com.google.common.collect.Lists;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import the_fireplace.grandexchange.util.TransactionDatabase;
import the_fireplace.grandexchange.market.BuyOffer;
import the_fireplace.grandexchange.market.SellOffer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class CommandCancelOffer extends CommandBase {
    @Override
    @Nonnull
    public String getName() {
        return "canceloffer";
    }

    @Override
    @Nonnull
    public String getUsage(@Nullable ICommandSender sender) {
        return "/ge canceloffer <offer number>";
    }

    @SuppressWarnings("Duplicates")
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

            int cancelIndex = 0;
            if (args != null && args.length == 1)
                try {
                    cancelIndex = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    throw new CommandException("Invalid offer number!");
                }
            int curIndex = 0;
            for (BuyOffer offer : buyOffers) {
                if(curIndex++ == cancelIndex) {
                    TransactionDatabase.getInstance().cancelOffer(offer);
                    sender.sendMessage(new TextComponentString("Offer cancelled."));
                    break;
                }
            }

            for (SellOffer offer : sellOffers) {
                if(curIndex++ == cancelIndex) {
                    TransactionDatabase.getInstance().cancelOffer(offer);
                    sender.sendMessage(new TextComponentString("Offer cancelled. You can collect your items with /ge collect"));
                    break;
                }
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
