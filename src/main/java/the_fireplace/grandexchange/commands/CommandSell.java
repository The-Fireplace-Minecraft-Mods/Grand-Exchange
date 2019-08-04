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
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import the_fireplace.grandeconomy.api.GrandEconomyApi;
import the_fireplace.grandexchange.market.SellOffer;
import the_fireplace.grandexchange.util.SerializationUtils;
import the_fireplace.grandexchange.util.TransactionDatabase;
import the_fireplace.grandexchange.util.translation.TranslationUtil;

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
        return TranslationUtil.getRawTranslationString(sender, "commands.ge.sell.usage");
    }

    @SuppressWarnings("Duplicates")
    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 4 || args.length == 5) {
            if(sender instanceof EntityPlayerMP) {
                ResourceLocation offerResource = new ResourceLocation(args[0]);
                boolean isValidRequest = ForgeRegistries.BLOCKS.containsKey(offerResource) || ForgeRegistries.ITEMS.containsKey(offerResource);
                if(!isValidRequest)
                    throw new CommandException(TranslationUtil.getRawTranslationString(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.common.invalid_item"));
                int meta = parseInt(args[1]);
                if(meta < 0)
                    throw new CommandException(TranslationUtil.getRawTranslationString(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.common.invalid_meta"));
                int amount = parseInt(args[2]);
                if(amount <= 0)
                    throw new CommandException(TranslationUtil.getRawTranslationString(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.common.invalid_amount"));
                long price = parseLong(args[3]);
                if (price < 0)
                    throw new CommandException(TranslationUtil.getRawTranslationString(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.common.invalid_price"));
                if(args.length == 5 && !args[4].isEmpty() && !SerializationUtils.isValidNBT(args[4]))
                    throw new CommandException(TranslationUtil.getRawTranslationString(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.common.invalid_nbt"));
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
                    throw new CommandException(TranslationUtil.getRawTranslationString(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.sell.not_enough_items"));
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
                    throw new CommandException(TranslationUtil.getRawTranslationString(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.sell.failed"));

                boolean madePurchase = TransactionDatabase.getInstance().makeOffer(new SellOffer(offerResource.toString(), meta, amount, price, ((EntityPlayerMP) sender).getUniqueID(), args.length == 5 ? args[4] : null));

                if(madePurchase)
                    sender.sendMessage(TranslationUtil.getTranslation(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.common.offer_fulfilled_balance", GrandEconomyApi.getBalance(((EntityPlayerMP) sender).getUniqueID())));
                else
                    sender.sendMessage(TranslationUtil.getTranslation(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.common.offer_made"));
                return;
            } else
                throw new CommandException(TranslationUtil.getRawTranslationString(sender, "commands.ge.common.not_player"));
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
