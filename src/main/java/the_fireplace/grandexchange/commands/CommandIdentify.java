package the_fireplace.grandexchange.commands;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import the_fireplace.grandexchange.market.ExchangeManager;
import the_fireplace.grandexchange.util.translation.TranslationUtil;

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
        return TranslationUtil.getRawTranslationString(sender, "commands.ge.identify.usage");
    }

    @Override
    public void execute(@Nullable MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (sender instanceof EntityPlayer) {
            boolean isValidRequest = !((EntityPlayer) sender).getHeldItemMainhand().isEmpty() || !((EntityPlayer) sender).getHeldItemOffhand().isEmpty();
            if(!isValidRequest)
                throw new CommandException(TranslationUtil.getRawTranslationString(((EntityPlayer) sender).getUniqueID(), "commands.ge.common.not_holding_anything"));
            ItemStack held = ((EntityPlayer) sender).getHeldItemMainhand().isEmpty() ? ((EntityPlayer) sender).getHeldItemOffhand() : ((EntityPlayer) sender).getHeldItemMainhand();
            if(ExchangeManager.canTransactItem(held)){
                @SuppressWarnings("ConstantConditions")
                String regName = held.getItem().getRegistryName().toString();
                if(regName.startsWith("minecraft:"))
                    regName = regName.substring(10);
                sender.sendMessage(TranslationUtil.getTranslation(((EntityPlayer) sender).getUniqueID(), "commands.ge.identify.success", regName+' '+held.getMetadata()+' '+(held.hasTagCompound() ? Objects.requireNonNull(held.getTagCompound()).toString() : "")));
            } else
                sender.sendMessage(TranslationUtil.getTranslation(((EntityPlayer) sender).getUniqueID(), "commands.ge.common.untradeable"));
        } else
            throw new CommandException(TranslationUtil.getRawTranslationString(sender, "commands.ge.common.not_player"));
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }
}
