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
import the_fireplace.grandeconomy.api.GrandEconomyApi;
import the_fireplace.grandeconomy.econhandlers.ge.InsufficientCreditException;
import the_fireplace.grandexchange.market.ExchangeManager;
import the_fireplace.grandexchange.market.OfferType;
import the_fireplace.grandexchange.util.SerializationUtils;
import the_fireplace.grandexchange.util.translation.TranslationUtil;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CommandBuy extends CommandBase {
    @Override
    public String getName() {
        return "buy";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return TranslationUtil.getRawTranslationString(sender, "commands.ge.buy.usage");
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
                if (GrandEconomyApi.getBalance(((EntityPlayerMP) sender).getUniqueID(), true) < price*amount)
                    throw new InsufficientCreditException();

                boolean madePurchase = ExchangeManager.makeOffer(OfferType.BUY, offerResource, meta, amount, price, ((EntityPlayerMP) sender).getUniqueID(), args.length == 5 ? args[4] : null);
                GrandEconomyApi.takeFromBalance(((EntityPlayerMP) sender).getUniqueID(), price*amount, true);

                if(madePurchase)
                    sender.sendMessage(TranslationUtil.getTranslation(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.buy.success_completed", GrandEconomyApi.toString(GrandEconomyApi.getBalance(((EntityPlayerMP) sender).getUniqueID(), true))));
                else
                    sender.sendMessage(TranslationUtil.getTranslation(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.common.offer_made_balance", GrandEconomyApi.toString(GrandEconomyApi.getBalance(((EntityPlayerMP) sender).getUniqueID(), true))));
                return;
            } else {
                throw new CommandException(TranslationUtil.getRawTranslationString(sender, "commands.ge.common.not_player"));
            }
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
