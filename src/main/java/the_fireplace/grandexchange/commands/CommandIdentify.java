package the_fireplace.grandexchange.commands;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.item.ItemStack;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import the_fireplace.grandexchange.util.TransactionDatabase;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CommandIdentify extends CommandBase {
    @Override
    public String getName() {
        return "identify";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/ge identify";
    }

    @Override
    public void execute(@Nullable MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (sender instanceof EntityPlayer) {
            ItemStack held = ((EntityPlayer) sender).getHeldItemMainhand();
            if(!held.isEmpty()) {
                if(TransactionDatabase.canTransactItem(held)){
                    @SuppressWarnings("ConstantConditions")
                    String regName = held.getItem().getRegistryName().toString();
                    if(regName.startsWith("minecraft:"))
                        regName = regName.substring(10);
                    notifyCommandListener(sender, this, "This item is: %s", regName+' '+held.getMetadata()+' '+(held.hasTagCompound() ? Objects.requireNonNull(held.getTagCompound()).toString() : ""));
                } else {
                    notifyCommandListener(sender, this, "This item cannot be traded on the Grand Exchange.");
                }
            } else {
                notifyCommandListener(sender, this, "You aren't holding anything in your main hand!");
            }
            return;
        }
        throw new WrongUsageException(getUsage(sender));
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }
}
