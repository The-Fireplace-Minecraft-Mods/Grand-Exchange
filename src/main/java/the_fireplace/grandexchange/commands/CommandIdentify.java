package the_fireplace.grandexchange.commands;

import net.minecraft.item.ItemStack;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import the_fireplace.grandexchange.TransactionDatabase;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CommandIdentify extends CommandBase {
    @Override
    @Nonnull
    public String getName() {
        return "identify";
    }

    @Override
    @Nonnull
    public String getUsage(@Nullable ICommandSender sender) {
        return "/identify";
    }

    @Override
    public void execute(@Nullable MinecraftServer server, @Nonnull ICommandSender sender, @Nullable String[] args) throws CommandException {
        if (sender instanceof EntityPlayer) {
            ItemStack held = ((EntityPlayer) sender).getHeldItemMainhand();
            if(!held.isEmpty()) {
                if(TransactionDatabase.canTransactItem(held)){
                    @SuppressWarnings("ConstantConditions")
                    String regName = held.getItem().getRegistryName().toString();
                    if(regName.startsWith("minecraft:"))
                        regName = regName.substring(10);
                    notifyCommandListener(sender, this, "This item is: %s", regName+' '+held.getMetadata());
                } else {
                    notifyCommandListener(sender, this, "This item cannot be traded on the Grand Exchange.");
                }
            } else {
                notifyCommandListener(sender, this, "You aren't holding anything in your main hand!");
            }
            return;
        }
        //noinspection RedundantArrayCreation
        throw new WrongUsageException("/identify", new Object[0]);
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }
}
