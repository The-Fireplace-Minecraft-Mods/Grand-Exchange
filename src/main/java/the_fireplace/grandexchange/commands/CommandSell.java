package the_fireplace.grandexchange.commands;

import net.minecraft.command.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import the_fireplace.grandeconomy.economy.Account;
import the_fireplace.grandexchange.TransactionDatabase;
import the_fireplace.grandexchange.market.BuyOffer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class CommandSell extends CommandBase {
    @Override
    @Nonnull
    public String getName() {
        return "sell";
    }

    @Override
    @Nonnull
    public String getUsage(@Nullable ICommandSender sender) {
        return "/sell <item> <meta> <amount> <price>";
    }

    @Override
    public void execute(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull String[] args) throws CommandException {
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
                int itemCount = 0;
                for(ItemStack stack: ((EntityPlayerMP) sender).inventory.mainInventory) {
                    //noinspection ConstantConditions
                    if(!stack.isEmpty() && stack.getItem().getRegistryName().equals(offerResource) && TransactionDatabase.canTransactItem(stack)){
                        if(stack.getCount() + itemCount >= amount)
                            itemCount = amount;
                        else
                            itemCount += stack.getCount();
                    }
                }
                if(itemCount < amount)
                    //noinspection RedundantArrayCreation
                    throw new CommandException("Error: You do not have enough of that item in your inventory to make this offer.", new Object[0]);
                int i = 0;
                for(ItemStack stack: ((EntityPlayerMP) sender).inventory.mainInventory) {
                    //noinspection ConstantConditions
                    while(!stack.isEmpty() && stack.getItem().getRegistryName().equals(offerResource) && itemCount > 0 && TransactionDatabase.canTransactItem(stack)){
                        if(stack.getCount() > 1)
                            stack.setCount(stack.getCount() - 1);
                        else
                            ((EntityPlayerMP) sender).inventory.mainInventory.set(i, ItemStack.EMPTY);
                        itemCount--;
                    }
                    i++;
                }
                if(itemCount > 0)
                    //noinspection RedundantArrayCreation
                    throw new CommandException("Error: Something went wrong when removing items from your inventory.", new Object[0]);

                boolean madePurchase = TransactionDatabase.makeOffer(new BuyOffer(offerResource.toString(), meta, amount, price, ((EntityPlayerMP) sender).getUniqueID()));

                Account senderAccount = Account.get((EntityPlayerMP) sender);
                if(madePurchase)
                    sender.sendMessage(new TextComponentTranslation("Offer completed! Your balance is now: %s", senderAccount.getBalance()));
                else
                    sender.sendMessage(new TextComponentTranslation("Offer succeeded!"));
                return;
            } else {
                //noinspection RedundantArrayCreation
                throw new CommandException("You must be a player to do this", new Object[0]);
            }
        }
        //noinspection RedundantArrayCreation
        throw new WrongUsageException("/sell <item> <meta> <amount> <price>", new Object[0]);
    }

    @Override
    @Nonnull
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
