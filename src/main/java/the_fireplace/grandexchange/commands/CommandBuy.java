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
import the_fireplace.grandexchange.market.NewOffer;
import the_fireplace.grandexchange.market.OfferStatusMessager;
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
        if(sender instanceof EntityPlayerMP) {
            if (args.length >= 3 && args.length <= 5) {
                String resourceName = args[0];
                int meta = parseInt(args[1]);
                int amount = parseInt(args[2]);
                long price = parseLong(args.length == 4 ? args[3] : "0");
                String nbt = args.length == 5 ? args[4] : null;
                if(args.length == 3 && resourceName.split(":").length < 3)
                    throw new WrongUsageException(getUsage(sender));
                //Parse if the command format is /ge buy domain:resource:meta amount price [nbt]
                if(resourceName.split(":").length == 3) {
                    resourceName = resourceName.substring(0, resourceName.lastIndexOf(":"));
                    meta = parseInt(args[0].split(":")[2]);
                    amount = parseInt(args[1]);
                    price = parseLong(args[2]);
                    nbt = args.length == 4 ? args[3] : null;
                }

                ResourceLocation offerResource = new ResourceLocation(resourceName);
                boolean isValidRequest = ForgeRegistries.BLOCKS.containsKey(offerResource) || ForgeRegistries.ITEMS.containsKey(offerResource);
                if(!isValidRequest)
                    throw new CommandException(TranslationUtil.getRawTranslationString(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.common.invalid_item"));
                if(meta < 0)
                    throw new CommandException(TranslationUtil.getRawTranslationString(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.common.invalid_meta"));
                if(amount <= 0)
                    throw new CommandException(TranslationUtil.getRawTranslationString(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.common.invalid_amount"));
                if(price < 0)
                    throw new CommandException(TranslationUtil.getRawTranslationString(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.common.invalid_price"));
                if(nbt != null && !SerializationUtils.isValidNBT(nbt))
                    throw new CommandException(TranslationUtil.getRawTranslationString(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.common.invalid_nbt"));
                if(GrandEconomyApi.getBalance(((EntityPlayerMP) sender).getUniqueID(), true) < price*amount)
                    throw new InsufficientCreditException(((EntityPlayerMP) sender).getUniqueID());

                boolean madePurchase = ExchangeManager.makeOffer(OfferType.BUY, offerResource, meta, amount, price, ((EntityPlayerMP) sender).getUniqueID(), nbt);
                GrandEconomyApi.takeFromBalance(((EntityPlayerMP) sender).getUniqueID(), price*amount, true);

                if(madePurchase)
                    sender.sendMessage(TranslationUtil.getTranslation(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.buy.success_completed", GrandEconomyApi.toString(GrandEconomyApi.getBalance(((EntityPlayerMP) sender).getUniqueID(), true))));
                else
                    sender.sendMessage(TranslationUtil.getTranslation(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.common.offer_made_balance", GrandEconomyApi.toString(GrandEconomyApi.getBalance(((EntityPlayerMP) sender).getUniqueID(), true))));
                return;
            } else if(args.length == 1 || args.length == 2) {
                long offerId = parseLong(args[0]);
                Integer amount = args.length == 2 ? parseInt(args[1]) : null;

                NewOffer offer = ExchangeManager.getOffer(offerId);
                if(offer != null) {
                    if(offer.isBuyOffer()) {
                        sender.sendMessage(TranslationUtil.getTranslation(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.common.wrong_offer_type"));
                        return;
                    }
                    if(amount == null || amount > offer.getAmount())
                        amount = offer.getAmount();
                    if(GrandEconomyApi.getBalance(((EntityPlayerMP) sender).getUniqueID(), true) < offer.getPrice()*amount)
                        throw new InsufficientCreditException(((EntityPlayerMP) sender).getUniqueID());
                    GrandEconomyApi.takeFromBalance(((EntityPlayerMP) sender).getUniqueID(), offer.getPrice() * amount, true);
                    if(amount == offer.getAmount()) {
                        ExchangeManager.removeOffer(offerId);
                        OfferStatusMessager.updateStatusComplete(offer);
                    } else {
                        ExchangeManager.updateCount(offerId, offer.getAmount() - amount);
                        OfferStatusMessager.updateStatusPartial(offer.getOwner(), offerId);
                    }
                    GrandEconomyApi.addToBalance(offer.getOwner(), amount * offer.getPrice(), true);
                    ExchangeManager.addPayouts(((EntityPlayerMP) sender).getUniqueID(), offer.getItemResourceName(), offer.getItemMeta(), amount, offer.getNbt());
                    sender.sendMessage(TranslationUtil.getTranslation(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.buy.success_completed", GrandEconomyApi.toString(GrandEconomyApi.getBalance(((EntityPlayerMP) sender).getUniqueID(), true))));
                } else
                    sender.sendMessage(TranslationUtil.getTranslation(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.common.invalid_offer_number"));
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
