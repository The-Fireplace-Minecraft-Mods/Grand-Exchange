package the_fireplace.grandexchange.commands;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import the_fireplace.grandeconomy.api.GrandEconomyApi;
import the_fireplace.grandexchange.GrandExchange;
import the_fireplace.grandexchange.market.ExchangeManager;
import the_fireplace.grandexchange.market.Offer;
import the_fireplace.grandexchange.market.OfferStatusMessager;
import the_fireplace.grandexchange.market.OfferType;
import the_fireplace.grandexchange.util.SerializationUtils;
import the_fireplace.grandexchange.util.TextStyles;
import the_fireplace.grandexchange.util.Utils;
import the_fireplace.grandexchange.util.translation.TranslationUtil;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CommandSell extends CommandBase {
    @Override
    public String getName() {
        return "sell";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return TranslationUtil.getRawTranslationString(sender, "commands.ge.sell.usage");
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
                //Parse if the command format is /ge sell domain:resource:meta amount price [nbt]
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
                if (price < 0)
                    throw new CommandException(TranslationUtil.getRawTranslationString(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.common.invalid_price"));
                if(nbt != null && !SerializationUtils.isValidNBT(nbt))
                    throw new CommandException(TranslationUtil.getRawTranslationString(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.common.invalid_nbt"));
                if(!hasEnoughItems((EntityPlayerMP) sender, offerResource, meta, amount, nbt))
                    throw new CommandException(TranslationUtil.getRawTranslationString(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.sell.not_enough_items"));
                long tax = Utils.calculateTax(amount * price);
                if(!GrandEconomyApi.takeFromBalance(((EntityPlayerMP) sender).getUniqueID(), tax, true)) {
                    sender.sendMessage(TranslationUtil.getTranslation(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.common.not_enough_tax", GrandEconomyApi.getCurrencyName(2), tax).setStyle(TextStyles.RED));
                    return;
                }
                GrandExchange.getTaxDistributor().distributeTax(((EntityPlayerMP) sender).getUniqueID(), tax);
                removeItems((EntityPlayerMP) sender, offerResource, meta, amount, nbt);

                boolean madePurchase = ExchangeManager.makeOffer(OfferType.SELL, offerResource.toString(), meta, amount, price, ((EntityPlayerMP) sender).getUniqueID(), nbt);

                if(madePurchase)
                    sender.sendMessage(TranslationUtil.getTranslation(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.common.offer_fulfilled_balance", GrandEconomyApi.toString(GrandEconomyApi.getBalance(((EntityPlayerMP) sender).getUniqueID(), true))));
                else
                    sender.sendMessage(TranslationUtil.getTranslation(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.common.offer_made"));
                return;
            } else if(args.length == 1 || args.length == 2) {
                long offerId = parseLong(args[0]);
                Integer amount = args.length == 2 ? parseInt(args[1]) : null;

                Offer offer = ExchangeManager.getOffer(offerId);
                if(offer != null) {
                    if(offer.isSellOffer()) {
                        sender.sendMessage(TranslationUtil.getTranslation(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.common.wrong_offer_type"));
                        return;
                    }
                    if(offer.getAmount() != null && (amount == null || amount > offer.getAmount()))
                        amount = offer.getAmount();
                    else if(amount == null)
                        amount = 1;
                    if(!hasEnoughItems((EntityPlayerMP) sender, new ResourceLocation(offer.getItemResourceName()), offer.getItemMeta(), amount, offer.getNbt()))
                        throw new CommandException(TranslationUtil.getRawTranslationString(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.sell.not_enough_items"));
                    removeItems((EntityPlayerMP) sender, new ResourceLocation(offer.getItemResourceName()), offer.getItemMeta(), amount, offer.getNbt());
                    if(Objects.equals(amount, offer.getAmount())) {
                        ExchangeManager.removeOffer(offerId);
                        OfferStatusMessager.updateStatusComplete(offer);
                    } else if(offer.getAmount() != null) {
                        ExchangeManager.updateCount(offerId, offer.getAmount() - amount);
                        OfferStatusMessager.updateStatusPartial(offer.getOwner(), offerId);
                    }

                    GrandEconomyApi.addToBalance(((EntityPlayerMP) sender).getUniqueID(), amount * offer.getPrice(), true);
                    if(offer.getOwner() != null)
                        ExchangeManager.addPayouts(offer.getOwner(), offer.getItemResourceName(), offer.getItemMeta(), amount, offer.getNbt());
                    sender.sendMessage(TranslationUtil.getTranslation(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.common.offer_fulfilled_balance", GrandEconomyApi.toString(GrandEconomyApi.getBalance(((EntityPlayerMP) sender).getUniqueID(), true))));
                } else
                    sender.sendMessage(TranslationUtil.getTranslation(((EntityPlayerMP) sender).getUniqueID(), "commands.ge.common.invalid_offer_number"));
                return;
            }
            throw new WrongUsageException(getUsage(sender));
        } else
            throw new CommandException(TranslationUtil.getRawTranslationString(sender, "commands.ge.common.not_player"));
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean hasEnoughItems(EntityPlayerMP sender, ResourceLocation offerResource, int meta, int amount, @Nullable String nbt) {
        int itemCount = 0;
        for(ItemStack stack: sender.inventory.mainInventory) {
            //noinspection ConstantConditions
            if(!stack.isEmpty() && stack.getItem().getRegistryName().equals(offerResource) && stack.getMetadata() == meta && ((!stack.hasTagCompound() && nbt == null) || stack.getTagCompound().toString().equals(nbt)) && ExchangeManager.canTransactItem(stack)){
                if(stack.getCount() + itemCount >= amount)
                    itemCount = amount;
                else
                    itemCount += stack.getCount();
            }
        }
        return itemCount >= amount;
    }

    public static void removeItems(EntityPlayerMP sender, ResourceLocation offerResource, int meta, int amount, @Nullable String nbt) throws CommandException {
        int slotIndex = 0;
        for(ItemStack stack: sender.inventory.mainInventory) {
            //noinspection ConstantConditions
            while(!stack.isEmpty() && stack.getItem().getRegistryName().equals(offerResource) && stack.getMetadata() == meta && ((!stack.hasTagCompound() && nbt == null) || stack.getTagCompound().toString().equals(nbt)) && amount > 0 && ExchangeManager.canTransactItem(stack)){
                amount--;
                if(stack.getCount() > 1)
                    stack.setCount(stack.getCount() - 1);
                else {
                    sender.inventory.mainInventory.set(slotIndex, ItemStack.EMPTY);
                    break;
                }
            }
            slotIndex++;
        }
        if(amount > 0)
            throw new CommandException(TranslationUtil.getRawTranslationString(sender.getUniqueID(), "commands.ge.sell.failed"));
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
