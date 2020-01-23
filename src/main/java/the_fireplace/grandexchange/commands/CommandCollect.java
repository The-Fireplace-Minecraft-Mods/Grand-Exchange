package the_fireplace.grandexchange.commands;

import com.google.common.collect.Lists;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import the_fireplace.grandexchange.market.ExchangeManager;
import the_fireplace.grandexchange.util.translation.TranslationUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CommandCollect extends CommandBase {
    @Override
    public String getName() {
        return "collect";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return TranslationUtil.getRawTranslationString(sender, "commands.ge.collect.usage");
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (sender instanceof EntityPlayer) {
            if(ExchangeManager.hasPayout(((EntityPlayer) sender).getUniqueID())) {
                List<ItemStack> removeItems = Lists.newArrayList();
                for(ItemStack stack: ExchangeManager.getPayout(((EntityPlayer) sender).getUniqueID())) {
                    if(stack != null && ((EntityPlayer) sender).addItemStackToInventory(stack.copy()))//Use a copy because in addItemStackToInventory, the stack's count gets set to 0 and that could be a problem when removing payouts
                        removeItems.add(stack);
                }
                ExchangeManager.removePayouts(((EntityPlayer) sender).getUniqueID(), removeItems.toArray(new ItemStack[]{}));
                if(ExchangeManager.hasPayout(((EntityPlayer) sender).getUniqueID()))
                    sender.sendMessage(TranslationUtil.getTranslation(((EntityPlayer) sender).getUniqueID(), "commands.ge.collect.no_more_room"));
                else
                    sender.sendMessage(TranslationUtil.getTranslation(((EntityPlayer) sender).getUniqueID(), "commands.ge.collect.success"));
            } else
                sender.sendMessage(TranslationUtil.getTranslation(((EntityPlayer) sender).getUniqueID(), "commands.ge.collect.nothing_to_collect"));
        } else
            throw new CommandException(TranslationUtil.getRawTranslationString(sender, "commands.ge.common.not_player"));
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }
}
