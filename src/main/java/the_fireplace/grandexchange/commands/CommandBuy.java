package the_fireplace.grandexchange.commands;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import the_fireplace.grandeconomy.api.GrandEconomyApi;
import the_fireplace.grandeconomy.econhandlers.ge.InsufficientCreditException;
import the_fireplace.grandexchange.market.BuyOffer;
import the_fireplace.grandexchange.util.SerializationUtils;
import the_fireplace.grandexchange.util.TransactionDatabase;

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
        return "/ge buy <item> <meta> <amount> <price> [nbt]";
    }

    @SuppressWarnings("Duplicates")
    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 4 || args.length == 5) {
            if(sender instanceof EntityPlayerMP) {
                ResourceLocation offerResource = new ResourceLocation(args[0]);
                boolean isValidRequest = ForgeRegistries.BLOCKS.containsKey(offerResource) || ForgeRegistries.ITEMS.containsKey(offerResource);
                if(!isValidRequest)
                    throw new CommandException("Error: Item not found");
                int meta = parseInt(args[1]);
                if(meta < 0)
                    throw new CommandException("Error: Invalid meta");
                int amount = parseInt(args[2]);
                if(amount <= 0)
                    throw new CommandException("Error: Amount cannot be less than 1");
                long price = parseLong(args[3]);
                if (price < 0)
                    throw new CommandException("You cannot pay someone negative amount. That would be rude.");
                if(args.length == 5 && !args[4].isEmpty() && !SerializationUtils.isValidNBT(args[4]))
                    throw new CommandException("Invalid NBT specified.");
                if (GrandEconomyApi.getBalance(((EntityPlayerMP) sender).getUniqueID()) < price*amount)
                    throw new InsufficientCreditException();

                boolean madePurchase = TransactionDatabase.getInstance().makeOffer(new BuyOffer(offerResource.toString(), meta, amount, price, ((EntityPlayerMP) sender).getUniqueID(), args.length == 5 ? args[4] : null));
                GrandEconomyApi.takeFromBalance(((EntityPlayerMP) sender).getUniqueID(), price*amount, false);

                if(madePurchase)
                    sender.sendMessage(new TextComponentTranslation("Purchase succeeded! Your balance is now: %s. You can collect your items with /ge collect", GrandEconomyApi.getBalance(((EntityPlayerMP) sender).getUniqueID())));
                else
                    sender.sendMessage(new TextComponentTranslation("Offer succeeded! Your balance is now: %s", GrandEconomyApi.getBalance(((EntityPlayerMP) sender).getUniqueID())));
                return;
            } else {
                throw new CommandException("You must be a player to do this");
            }
        }
        throw new WrongUsageException(getUsage(sender));
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
