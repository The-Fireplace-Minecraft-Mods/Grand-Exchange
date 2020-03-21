package the_fireplace.grandexchange.commands;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import the_fireplace.grandexchange.market.ExchangeManager;
import the_fireplace.grandexchange.market.OfferType;
import the_fireplace.grandexchange.util.translation.TranslationUtil;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CommandOpSellThis extends CommandBase {
    @Override
    public String getName() {
        return "opsellthis";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return TranslationUtil.getRawTranslationString(sender, "commands.ge.opsellthis.usage");
    }

    @SuppressWarnings("Duplicates")
    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 1) {
            if(sender instanceof EntityPlayerMP) {
                boolean isValidRequest = !((EntityPlayerMP) sender).getHeldItemMainhand().isEmpty() || !((EntityPlayerMP) sender).getHeldItemOffhand().isEmpty();
                if(!isValidRequest)
                    throw new CommandException(TranslationUtil.getRawTranslationString(((EntityPlayer) sender).getUniqueID(), "commands.ge.common.not_holding_anything"));
                ItemStack selling = ((EntityPlayerMP) sender).getHeldItemMainhand().isEmpty() ? ((EntityPlayerMP) sender).getHeldItemOffhand() : ((EntityPlayerMP) sender).getHeldItemMainhand();
                long price = parseLong(args[0]);
                if (price < 0)
                    throw new CommandException(TranslationUtil.getRawTranslationString(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.common.invalid_price"));

                ExchangeManager.makeOpOffer(OfferType.SELL, selling.getItem().getRegistryName().toString(), selling.getMetadata(), price, selling.hasTagCompound() ? Objects.requireNonNull(selling.getTagCompound()).toString() : null);

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
        return sender instanceof EntityPlayerMP;
    }
}
