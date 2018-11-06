package the_fireplace.grandexchange.commands;

import com.google.common.collect.Lists;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import the_fireplace.grandexchange.TransactionDatabase;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class CommandCollect extends CommandBase {
    @Override
    @Nonnull
    public String getName() {
        return "collect";
    }

    @Override
    @Nonnull
    public String getUsage(@Nullable ICommandSender sender) {
        return "/collect";
    }

    @Override
    public void execute(@Nullable MinecraftServer server, @Nonnull ICommandSender sender, @Nullable String[] args) throws CommandException {
        if (sender instanceof EntityPlayer) {
            if(TransactionDatabase.hasPayout(((EntityPlayer) sender).getUniqueID())){
                List<ItemStack> removeItems = Lists.newArrayList();
                for(ItemStack stack: TransactionDatabase.getPayout(((EntityPlayer) sender).getUniqueID())) {
                    if(((EntityPlayer) sender).addItemStackToInventory(stack))
                        removeItems.add(stack);
                }
                TransactionDatabase.removePayouts(((EntityPlayer) sender).getUniqueID(), removeItems);
            } else {
                throw new CommandException("You don't have anything to collect.");
            }
            return;
        }
        //noinspection RedundantArrayCreation
        throw new WrongUsageException("/collect", new Object[0]);
    }

    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }
}
