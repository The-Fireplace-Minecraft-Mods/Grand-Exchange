package the_fireplace.grandexchange.commands;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.command.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import the_fireplace.grandeconomy.economy.Account;
import the_fireplace.grandexchange.util.SerializationUtils;
import the_fireplace.grandexchange.util.TransactionDatabase;
import the_fireplace.grandexchange.market.SellOffer;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CommandSell extends CommandBase {
    @Override
    public String getName() {
        return "sell";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/ge sell <item> <meta> <amount> <price> [nbt]";
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
                int itemCount = 0;
                for(ItemStack stack: ((EntityPlayerMP) sender).inventory.mainInventory) {
                    //noinspection ConstantConditions
                    if(!stack.isEmpty() && stack.getItem().getRegistryName().equals(offerResource) && stack.getMetadata() == meta && ((!stack.hasTagCompound() && args.length == 4) || stack.getTagCompound().toString().equals(args[4])) && TransactionDatabase.canTransactItem(stack)){
                        if(stack.getCount() + itemCount >= amount)
                            itemCount = amount;
                        else
                            itemCount += stack.getCount();
                    }
                }
                if(itemCount < amount)
                    throw new CommandException("Error: You do not have enough of that item in your inventory to make this offer.");
                int i = 0;
                for(ItemStack stack: ((EntityPlayerMP) sender).inventory.mainInventory) {
                    //noinspection ConstantConditions
                    while(!stack.isEmpty() && stack.getItem().getRegistryName().equals(offerResource) && stack.getMetadata() == meta && ((!stack.hasTagCompound() && args.length == 4) || stack.getTagCompound().toString().equals(args[4])) && itemCount > 0 && TransactionDatabase.canTransactItem(stack)){
                        itemCount--;
                        if(stack.getCount() > 1)
                            stack.setCount(stack.getCount() - 1);
                        else {
                            ((EntityPlayerMP) sender).inventory.mainInventory.set(i, ItemStack.EMPTY);
                            break;
                        }
                    }
                    i++;
                }
                if(itemCount > 0)
                    throw new CommandException("Error: Something went wrong when removing items from your inventory.");

                boolean madePurchase = TransactionDatabase.getInstance().makeOffer(new SellOffer(offerResource.toString(), meta, amount, price, ((EntityPlayerMP) sender).getUniqueID(), args.length == 5 ? args[4] : null));

                Account senderAccount = Account.get((EntityPlayerMP) sender);
                if(madePurchase)
                    sender.sendMessage(new TextComponentTranslation("Offer completed! Your balance is now: %s", senderAccount.getBalance()));
                else
                    sender.sendMessage(new TextComponentTranslation("Offer succeeded!"));
                return;
            } else
                throw new CommandException("You must be a player to do this");
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
