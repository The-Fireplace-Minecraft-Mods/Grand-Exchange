package the_fireplace.grandexchange.commands;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import the_fireplace.grandexchange.market.ExchangeManager;
import the_fireplace.grandexchange.market.OfferType;
import the_fireplace.grandexchange.util.SerializationUtils;
import the_fireplace.grandexchange.util.translation.TranslationUtil;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CommandOpSell extends CommandBase {
    @Override
    public String getName() {
        return "opsell";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return TranslationUtil.getRawTranslationString(sender, "commands.ge.opsell.usage");
    }

    @SuppressWarnings("Duplicates")
    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if(sender instanceof EntityPlayerMP) {
            if (args.length >= 2 && args.length <= 4) {
                String resourceName = args[0];
                int meta = parseInt(args[1]);
                long price = parseLong(args.length == 3 ? args[2] : "0");
                String nbt = args.length == 4 ? args[3] : null;
                if(args.length == 2 && resourceName.split(":").length < 3)
                    throw new WrongUsageException(getUsage(sender));
                //Parse if the command format is /ge opsell domain:resource:meta price [nbt]
                if(resourceName.split(":").length == 3) {
                    resourceName = resourceName.substring(0, resourceName.lastIndexOf(":"));
                    meta = parseInt(args[0].split(":")[2]);
                    price = parseLong(args[1]);
                    nbt = args.length == 3 ? args[2] : null;
                }
                ResourceLocation offerResource = new ResourceLocation(resourceName);
                boolean isValidRequest = ForgeRegistries.BLOCKS.containsKey(offerResource) || ForgeRegistries.ITEMS.containsKey(offerResource);
                if(!isValidRequest)
                    throw new CommandException(TranslationUtil.getRawTranslationString(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.common.invalid_item"));
                if(meta < 0)
                    throw new CommandException(TranslationUtil.getRawTranslationString(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.common.invalid_meta"));
                if (price < 0)
                    throw new CommandException(TranslationUtil.getRawTranslationString(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.common.invalid_price"));
                if(nbt != null && !SerializationUtils.isValidNBT(nbt))
                    throw new CommandException(TranslationUtil.getRawTranslationString(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.common.invalid_nbt"));

                ExchangeManager.makeOpOffer(OfferType.SELL, offerResource.toString(), meta, price, nbt);

                sender.sendMessage(TranslationUtil.getTranslation(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.common.offer_made"));
                return;
            }
            throw new WrongUsageException(getUsage(sender));
        } else
            throw new CommandException(TranslationUtil.getRawTranslationString(sender, "commands.ge.common.not_player"));
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
