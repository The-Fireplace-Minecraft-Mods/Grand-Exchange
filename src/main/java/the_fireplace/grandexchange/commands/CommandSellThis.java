package the_fireplace.grandexchange.commands;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import the_fireplace.grandeconomy.economy.Account;
import the_fireplace.grandexchange.market.SellOffer;
import the_fireplace.grandexchange.util.SerializationUtils;
import the_fireplace.grandexchange.util.TransactionDatabase;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CommandSellThis extends CommandBase {
    @Override
    public String getName() {
        return "sell";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/ge sellthis <price> <amount>";
    }

    @SuppressWarnings("Duplicates")
    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 2) {
            if(sender instanceof EntityPlayerMP) {
                boolean isValidRequest = !((EntityPlayerMP) sender).getHeldItemMainhand().isEmpty() || !((EntityPlayerMP) sender).getHeldItemOffhand().isEmpty();
                if(!isValidRequest)
                    throw new CommandException("Error: You aren't holding anything");
                ItemStack selling = ((EntityPlayerMP) sender).getHeldItemMainhand().isEmpty() ? ((EntityPlayerMP) sender).getHeldItemOffhand() : ((EntityPlayerMP) sender).getHeldItemMainhand();
                int amount = parseInt(args[1]);
                if(amount <= 0)
                    throw new CommandException("Error: Amount cannot be less than 1");
                long price = parseLong(args[0]);
                if (price < 0)
                    throw new CommandException("You cannot pay someone negative amount. That would be rude.");
                int itemCount = 0;
                for(ItemStack stack: ((EntityPlayerMP) sender).inventory.mainInventory) {
                    //noinspection ConstantConditions
                    if(!stack.isEmpty() && stack.getItem().getRegistryName().equals(selling.getItem().getRegistryName()) && stack.getMetadata() == selling.getMetadata() && ((!stack.hasTagCompound() && !selling.hasTagCompound()) || stack.getTagCompound().toString().equals(selling.getTagCompound().toString())) && TransactionDatabase.canTransactItem(stack)){
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
                    while(!stack.isEmpty() && stack.getItem().getRegistryName().equals(selling.getItem().getRegistryName()) && stack.getMetadata() == selling.getMetadata() && ((!stack.hasTagCompound() && !selling.hasTagCompound()) || stack.getTagCompound().toString().equals(selling.getTagCompound().toString())) && itemCount > 0 && TransactionDatabase.canTransactItem(stack)){
                        if(stack.getCount() > 1)
                            stack.setCount(stack.getCount() - 1);
                        else
                            ((EntityPlayerMP) sender).inventory.mainInventory.set(i, ItemStack.EMPTY);
                        itemCount--;
                    }
                    i++;
                }
                if(itemCount > 0)
                    throw new CommandException("Error: Something went wrong when removing items from your inventory.");

                boolean madePurchase = TransactionDatabase.getInstance().makeOffer(new SellOffer(selling.getItem().getRegistryName().toString(), selling.getMetadata(), amount, price, ((EntityPlayerMP) sender).getUniqueID(), selling.hasTagCompound() ? Objects.requireNonNull(selling.getTagCompound()).toString() : null));

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
