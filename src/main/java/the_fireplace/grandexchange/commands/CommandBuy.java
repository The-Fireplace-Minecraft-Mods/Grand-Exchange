package the_fireplace.grandexchange.commands;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import the_fireplace.grandeconomy.economy.Account;
import net.minecraft.command.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import the_fireplace.grandexchange.TransactionDatabase;
import the_fireplace.grandexchange.market.BuyOffer;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CommandBuy extends CommandBase {
    @Override
    public String getName() {
        return "buy";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/ge buy <item> <meta> <amount> <price>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 4) {
            if(sender instanceof EntityPlayerMP) {
                ResourceLocation offerResource = new ResourceLocation(args[0]);
                boolean isValidRequest = ForgeRegistries.BLOCKS.containsKey(offerResource) || ForgeRegistries.ITEMS.containsKey(offerResource);
                if(!isValidRequest)
                    //noinspection RedundantArrayCreation
                    throw new CommandException("Error: Item not found", new Object[0]);
                int meta = parseInt(args[1]);
                if(meta < 0)
                    //noinspection RedundantArrayCreation
                    throw new CommandException("Error: Invalid meta", new Object[0]);
                int amount = parseInt(args[2]);
                if(amount <= 0)
                    //noinspection RedundantArrayCreation
                    throw new CommandException("Error: Amount cannot be less than 1", new Object[0]);
                long price = parseLong(args[3]);
                if (price < 0)
                    //noinspection RedundantArrayCreation
                    throw new CommandException("You cannot pay someone negative amount. That would be rude.", new Object[0]);
                Account senderAccount = Account.get((EntityPlayerMP) sender);
                if (senderAccount.getBalance() < price*amount)
                    throw new InsufficientCreditException();

                boolean madePurchase = TransactionDatabase.makeOffer(new BuyOffer(offerResource.toString(), meta, amount, price, ((EntityPlayerMP) sender).getUniqueID()));
                senderAccount.addBalance(-price*amount);

                if(madePurchase)
                    sender.sendMessage(new TextComponentTranslation("Purchase succeeded! Your balance is now: %s. You can collect your items with /ge collect", senderAccount.getBalance()));
                else
                    sender.sendMessage(new TextComponentTranslation("Offer succeeded! Your balance is now: %s", senderAccount.getBalance()));
                return;
            } else {
                //noinspection RedundantArrayCreation
                throw new CommandException("You must be a player to do this", new Object[0]);
            }
        }
        //noinspection RedundantArrayCreation
        throw new WrongUsageException("/ge buy <item> <meta> <amount> <price>", new Object[0]);
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        //TODO Tab completions
        return Collections.emptyList();
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 0;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }
}
