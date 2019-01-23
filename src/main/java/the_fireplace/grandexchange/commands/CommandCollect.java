package the_fireplace.grandexchange.commands;

import com.google.common.collect.Lists;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import the_fireplace.grandexchange.TransactionDatabase;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CommandCollect extends CommandBase {
    @Override
    public String getName() {
        return "ge collect";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/ge collect";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (sender instanceof EntityPlayer) {
            if(TransactionDatabase.hasPayout(((EntityPlayer) sender).getUniqueID())){
                List<ItemStack> removeItems = Lists.newArrayList();
                for(ItemStack stack: TransactionDatabase.getPayout(((EntityPlayer) sender).getUniqueID())) {
                    if(((EntityPlayer) sender).addItemStackToInventory(stack))
                        removeItems.add(stack);
                }
                TransactionDatabase.removePayouts(((EntityPlayer) sender).getUniqueID(), removeItems);
                if(TransactionDatabase.hasPayout(((EntityPlayer) sender).getUniqueID()))
                    sender.sendMessage(new TextComponentString("You have run out of room for collection. Make room in your inventory and try again."));
                else
                    sender.sendMessage(new TextComponentString("Collection successful."));
            } else {
                sender.sendMessage(new TextComponentString("You don't have anything to collect."));
            }
            return;
        }
        //noinspection RedundantArrayCreation
        throw new WrongUsageException("/ge collect", new Object[0]);
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }
}
