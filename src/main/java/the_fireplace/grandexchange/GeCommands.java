package the_fireplace.grandexchange;

import com.google.common.collect.Lists;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.registries.ForgeRegistries;
import the_fireplace.grandeconomy.TextStyles;
import the_fireplace.grandeconomy.api.GrandEconomyApi;
import the_fireplace.grandeconomy.api.InsufficientCreditException;
import the_fireplace.grandeconomy.economy.Account;
import the_fireplace.grandexchange.market.BuyOffer;
import the_fireplace.grandexchange.market.SellOffer;
import the_fireplace.grandexchange.util.SerializationUtils;
import the_fireplace.grandexchange.util.TransactionDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class GeCommands {
    private static ArrayList<ResourceLocation> itemResources;
    @SuppressWarnings("Duplicates")
    static void register(CommandDispatcher<CommandSource> commandDispatcher) {
        itemResources = Lists.newArrayList();
        itemResources.addAll(ForgeRegistries.BLOCKS.getKeys());
        itemResources.addAll(ForgeRegistries.ITEMS.getKeys());
        LiteralArgumentBuilder<CommandSource> geCommand = Commands.literal("ge").requires((iCommandSender) -> iCommandSender.getEntity() instanceof EntityPlayerMP);

        ArgumentBuilder<CommandSource, ?> buyArgs =
                      Commands.argument("item", StringArgumentType.word())
                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64*37))
                .then(Commands.argument("price", IntegerArgumentType.integer(1))
                .executes(buyCommand)
                .then(Commands.argument("nbt", StringArgumentType.greedyString())
                .executes(buyCommand)
                )));
        ArgumentBuilder<CommandSource, ?> sellArgs =
                      Commands.argument("item", StringArgumentType.word())
                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64*37))
                .then(Commands.argument("price", IntegerArgumentType.integer(1))
                .executes(sellCommand)
                .then(Commands.argument("nbt", StringArgumentType.greedyString())
                .executes(sellCommand)
                )));
        ArgumentBuilder<CommandSource, ?> sellThisArgs =
                      Commands.argument("amount", IntegerArgumentType.integer(1, 64*37))
                .then(Commands.argument("price", IntegerArgumentType.integer(1))
                .executes(sellThisCommand)
                );

        geCommand.then(Commands.literal("buy").then(buyArgs));
        geCommand.then(Commands.literal("b").then(buyArgs));
        geCommand.then(Commands.literal("sell").then(sellArgs));
        geCommand.then(Commands.literal("s").then(sellArgs));
        geCommand.then(Commands.literal("sellthis").then(sellThisArgs));
        geCommand.then(Commands.literal("st").then(sellThisArgs));
        geCommand.then(Commands.literal("identify").executes(identifyCommand));
        geCommand.then(Commands.literal("i").executes(identifyCommand));
        geCommand.then(Commands.literal("collect").executes(collectCommand));
        geCommand.then(Commands.literal("c").executes(collectCommand));
        geCommand.then(Commands.literal("myoffers").executes(myOffersCommand).then(Commands.argument("page", IntegerArgumentType.integer(1)).executes(myOffersCommand)));
        geCommand.then(Commands.literal("mo").executes(myOffersCommand).then(Commands.argument("page", IntegerArgumentType.integer(1)).executes(myOffersCommand)));
        geCommand.then(Commands.literal("m").executes(myOffersCommand).then(Commands.argument("page", IntegerArgumentType.integer(1)).executes(myOffersCommand)));
        geCommand.then(Commands.literal("buyoffers").executes(buyOffersCommand).then(Commands.argument("page", IntegerArgumentType.integer(1)).executes(buyOffersCommand)));
        geCommand.then(Commands.literal("bo").executes(buyOffersCommand).then(Commands.argument("page", IntegerArgumentType.integer(1)).executes(buyOffersCommand)));
        geCommand.then(Commands.literal("selloffers").executes(sellOffersCommand).then(Commands.argument("page", IntegerArgumentType.integer(1)).executes(sellOffersCommand)));
        geCommand.then(Commands.literal("so").executes(sellOffersCommand).then(Commands.argument("page", IntegerArgumentType.integer(1)).executes(sellOffersCommand)));
        geCommand.then(Commands.literal("canceloffer").then(Commands.argument("number", IntegerArgumentType.integer(1)).executes(cancelOfferCommand)));
        geCommand.then(Commands.literal("co").then(Commands.argument("number", IntegerArgumentType.integer(1)).executes(cancelOfferCommand)));
        geCommand.then(Commands.literal("help").executes(context -> {context.getSource().sendFeedback(new TextComponentString("/ge commands:\n" +
                "buy\n" +
                "sell\n" +
                "sellthis\n" +
                "identify\n" +
                "collect\n" +
                "buyoffers\n" +
                "selloffers\n" +
                "myoffers\n" +
                "canceloffer\n" +
                "help"), false);return 1;}));

        commandDispatcher.register(geCommand);
    }

    private static final Command<CommandSource> buyCommand = context -> {
        EntityPlayerMP sender = (EntityPlayerMP) Objects.requireNonNull(context.getSource().getEntity());
        ResourceLocation offerResource = new ResourceLocation(context.getArgument("item", String.class));
        boolean isValidRequest = itemResources.contains(offerResource);
        if(!isValidRequest)
            throw new CommandException(new TextComponentString("Error: Item not found"));
        int amount = context.getArgument("amount", Integer.class);
        if(amount <= 0)
            throw new CommandException(new TextComponentString("Error: Amount cannot be less than 1"));
        int price = context.getArgument("price", Integer.class);
        if (price < 0)
            throw new CommandException(new TextComponentString("You cannot pay someone negative amount. That would be rude."));
        String tag;
        try {
            tag = context.getArgument("nbt", String.class);
        } catch(IllegalArgumentException e) {
            tag = null;
        }
        Account senderAccount = Account.get(sender);
        if (senderAccount.getBalance() < price*amount)
            throw new InsufficientCreditException();
        boolean madePurchase = TransactionDatabase.getInstance().makeOffer(new BuyOffer(offerResource.toString(), amount, price, sender.getUniqueID(), tag));
        senderAccount.addBalance(-price*amount, false);

        if(madePurchase)
            sender.sendMessage(new TextComponentTranslation("Purchase succeeded! Your balance is now: %s. You can collect your items with /ge collect", senderAccount.getBalance()));
        else
            sender.sendMessage(new TextComponentTranslation("Offer succeeded! Your balance is now: %s", senderAccount.getBalance()));
        return 1;
    };

    private static final Command<CommandSource> sellCommand = context -> {
        EntityPlayerMP sender = (EntityPlayerMP) Objects.requireNonNull(context.getSource().getEntity());
        ResourceLocation offerResource = new ResourceLocation(context.getArgument("item", String.class));
        boolean isValidRequest = itemResources.contains(offerResource);
        if(!isValidRequest)
            throw new CommandException(new TextComponentString("Error: Item not found"));
        int amount = context.getArgument("amount", Integer.class);
        if(amount <= 0)
            throw new CommandException(new TextComponentString("Error: Amount cannot be less than 1"));
        int price = context.getArgument("price", Integer.class);
        if (price < 0)
            throw new CommandException(new TextComponentString("You cannot pay someone negative amount. That would be rude."));
        String tag;
        try {
            tag = context.getArgument("nbt", String.class);
        } catch(IllegalArgumentException e) {
            tag = null;
        }
        NBTTagCompound nbt;
        try {
            nbt = JsonToNBT.getTagFromJson(tag);
        } catch(CommandSyntaxException e) {
            nbt = null;
        }
        int itemCount = 0;
        for(ItemStack stack: sender.inventory.mainInventory) {
            if(!stack.isEmpty() && Objects.equals(stack.getItem().getRegistryName(), offerResource) && Objects.equals(nbt, stack.getTag()) && TransactionDatabase.canTransactItem(stack)){
                if(stack.getCount() + itemCount >= amount)
                    itemCount = amount;
                else
                    itemCount += stack.getCount();
            }
        }
        if(itemCount < amount)
            throw new CommandException(new TextComponentString("Error: You do not have enough of that item in your inventory to make this offer."));
        int i = 0;
        for(ItemStack stack: sender.inventory.mainInventory) {
            while(!stack.isEmpty() && Objects.equals(stack.getItem().getRegistryName(), offerResource) && Objects.equals(nbt, stack.getTag()) && itemCount > 0 && TransactionDatabase.canTransactItem(stack)){
                if(stack.getCount() > 1)
                    stack.setCount(stack.getCount() - 1);
                else
                    sender.inventory.mainInventory.set(i, ItemStack.EMPTY);
                itemCount--;
            }
            i++;
        }
        if(itemCount > 0)
            throw new CommandException(new TextComponentString("Error: Something went wrong when removing items from your inventory."));

        boolean madePurchase = TransactionDatabase.getInstance().makeOffer(new SellOffer(offerResource.toString(), amount, price, sender.getUniqueID(), tag));

        Account senderAccount = Account.get(sender);
        if(madePurchase)
            sender.sendMessage(new TextComponentTranslation("Offer completed! Your balance is now: %s", senderAccount.getBalance()));
        else
            sender.sendMessage(new TextComponentTranslation("Offer succeeded!"));
        return 1;
    };


    private static final Command<CommandSource> sellThisCommand = context -> {
        EntityPlayerMP sender = (EntityPlayerMP) Objects.requireNonNull(context.getSource().getEntity());
        boolean isValidRequest = !sender.getHeldItemMainhand().isEmpty() || !sender.getHeldItemOffhand().isEmpty();
        if(!isValidRequest)
            throw new CommandException(new TextComponentString("Error: You aren't holding anything"));
        ItemStack selling = sender.getHeldItemMainhand().isEmpty() ? sender.getHeldItemOffhand() : sender.getHeldItemMainhand();
        int amount = context.getArgument("amount", Integer.class);
        if(amount <= 0)
            throw new CommandException(new TextComponentString("Error: Amount cannot be less than 1"));
        long price = context.getArgument("price", Integer.class);
        if (price < 0)
            throw new CommandException(new TextComponentString("You cannot pay someone negative amount. That would be rude."));
        String tag;
        try {
            tag = context.getArgument("nbt", String.class);
        } catch(IllegalArgumentException e) {
            tag = null;
        }
        NBTTagCompound nbt;
        try {
            nbt = JsonToNBT.getTagFromJson(tag);
        } catch(CommandSyntaxException e) {
            nbt = null;
        }
        int itemCount = 0;
        for(ItemStack stack: sender.inventory.mainInventory) {
            if(!stack.isEmpty() && Objects.equals(stack.getItem().getRegistryName(), selling.getItem().getRegistryName()) && Objects.equals(nbt, stack.getTag()) && TransactionDatabase.canTransactItem(stack)){
                if(stack.getCount() + itemCount >= amount)
                    itemCount = amount;
                else
                    itemCount += stack.getCount();
            }
        }
        if(itemCount < amount)
            throw new CommandException(new TextComponentString("Error: You do not have enough of that item in your inventory to make this offer."));
        int i = 0;
        for(ItemStack stack: sender.inventory.mainInventory) {
            while(!stack.isEmpty() && Objects.equals(stack.getItem().getRegistryName(), selling.getItem().getRegistryName()) && Objects.equals(nbt, stack.getTag()) && itemCount > 0 && TransactionDatabase.canTransactItem(stack)){
                if(stack.getCount() > 1)
                    stack.setCount(stack.getCount() - 1);
                else
                    sender.inventory.mainInventory.set(i, ItemStack.EMPTY);
                itemCount--;
            }
            i++;
        }
        if(itemCount > 0)
            throw new CommandException(new TextComponentString("Error: Something went wrong when removing items from your inventory."));

        boolean madePurchase = TransactionDatabase.getInstance().makeOffer(new SellOffer(selling.getItem().getRegistryName().toString(), amount, price, sender.getUniqueID(), tag));

        Account senderAccount = Account.get(sender);
        if(madePurchase)
            sender.sendMessage(new TextComponentTranslation("Offer completed! Your balance is now: %s", senderAccount.getBalance()));
        else
            sender.sendMessage(new TextComponentTranslation("Offer succeeded!"));
        return 1;
    };

    private static final Command<CommandSource> identifyCommand = context -> {
        EntityPlayerMP sender = (EntityPlayerMP) Objects.requireNonNull(context.getSource().getEntity());
        ItemStack held = sender.getHeldItemMainhand().isEmpty() ? sender.getHeldItemOffhand() : sender.getHeldItemMainhand();
        if(!held.isEmpty()) {
            if(TransactionDatabase.canTransactItem(held)){
                @SuppressWarnings("ConstantConditions")
                String regName = held.getItem().getRegistryName().toString();
                if(regName.startsWith("minecraft:"))
                    regName = regName.substring(10);
                context.getSource().sendFeedback(new TextComponentString("This item is: " + regName + (held.getTag() != null ? held.getTag().toString() : "")), false);
            } else {
                context.getSource().sendFeedback(new TextComponentString("This item cannot be traded on the Grand Exchange."), false);
            }
        } else {
            context.getSource().sendFeedback(new TextComponentString("You are not holding anything."), false);
        }
        return 1;
    };

    private static final Command<CommandSource> collectCommand = context -> {
        EntityPlayerMP sender = (EntityPlayerMP) Objects.requireNonNull(context.getSource().getEntity());
        if(TransactionDatabase.hasPayout(sender.getUniqueID())){
            List<String> removeItems = Lists.newArrayList();
            for(String stackStr: TransactionDatabase.getPayout(sender.getUniqueID())) {
                ItemStack stack = SerializationUtils.stackFromString(stackStr);
                if(stack != null && sender.addItemStackToInventory(stack))
                    removeItems.add(stackStr);
            }
            TransactionDatabase.getInstance().removePayouts(sender.getUniqueID(), removeItems);
            if(TransactionDatabase.hasPayout(sender.getUniqueID()))
                sender.sendMessage(new TextComponentString("You have run out of room for collection. Make room in your inventory and try again."));
            else
                sender.sendMessage(new TextComponentString("Collection successful."));
        } else {
            sender.sendMessage(new TextComponentString("You don't have anything to collect."));
        }
        return 1;
    };

    private static final Command<CommandSource> myOffersCommand = context -> {
        EntityPlayerMP sender = (EntityPlayerMP) Objects.requireNonNull(context.getSource().getEntity());
        List<BuyOffer> buyOffers = Lists.newArrayList();
        for (List<BuyOffer> offerList : TransactionDatabase.getBuyOffers().values())
            buyOffers.addAll(offerList);
        List<SellOffer> sellOffers = Lists.newArrayList();
        for (List<SellOffer> offerList : TransactionDatabase.getSellOffers().values())
            sellOffers.addAll(offerList);
        buyOffers.removeIf(offer -> !offer.getOwner().equals(sender.getUniqueID()));
        sellOffers.removeIf(offer -> !offer.getOwner().equals(sender.getUniqueID()));

        int page;
        try {
            page = context.getArgument("page", Integer.class);
        } catch (CommandException e) {
            page = 1;
        }
        //Expand page to be the first entry on the page
        page *= 50;
        //Subtract 49 because the first page starts with entry 1
        page -= 49;
        int orderIndex = page;
        int termLength = 50;
        for (BuyOffer offer : buyOffers) {
            if (page-- > 0)
                continue;
            if (termLength-- <= 0)
                break;
            sender.sendMessage(new TextComponentString(orderIndex++ + ". ").setStyle(TextStyles.YELLOW).appendSibling(new TextComponentString(offer.getAmount() + ' ' + offer.getItemResourceName() + (offer.getNbt() != null ? " with NBT "+offer.getNbt() : "") + " wanted for " + offer.getPrice() + ' ' + GrandEconomyApi.getCurrencyName(offer.getPrice()) + " each").setStyle(TextStyles.BLUE)));
        }

        for (SellOffer offer : sellOffers) {
            if (page-- > 0)
                continue;
            if (termLength-- <= 0)
                break;
            sender.sendMessage(new TextComponentString(orderIndex++ + ". ").setStyle(TextStyles.YELLOW).appendSibling(new TextComponentString(offer.getAmount() + ' ' + offer.getItemResourceName() + (offer.getNbt() != null ? " with NBT "+offer.getNbt() : "") + " being sold for " + offer.getPrice() + ' ' + GrandEconomyApi.getCurrencyName(offer.getPrice()) + " each").setStyle(TextStyles.PURPLE)));
        }

        if(buyOffers.isEmpty() && sellOffers.isEmpty())
            sender.sendMessage(new TextComponentString("You are not buying or selling anything."));
        return 1;
    };

    private static final Command<CommandSource> cancelOfferCommand = context -> {
        EntityPlayerMP sender = (EntityPlayerMP) Objects.requireNonNull(context.getSource().getEntity());
        List<BuyOffer> buyOffers = Lists.newArrayList();
        for (List<BuyOffer> offerList : TransactionDatabase.getBuyOffers().values())
            buyOffers.addAll(offerList);
        List<SellOffer> sellOffers = Lists.newArrayList();
        for (List<SellOffer> offerList : TransactionDatabase.getSellOffers().values())
            sellOffers.addAll(offerList);
        buyOffers.removeIf(offer -> !offer.getOwner().equals(sender.getUniqueID()));
        sellOffers.removeIf(offer -> !offer.getOwner().equals(sender.getUniqueID()));

        int cancelIndex = context.getArgument("number", Integer.class);
        int curIndex = 0;
        for (BuyOffer offer : buyOffers) {
            if(++curIndex == cancelIndex) {
                TransactionDatabase.getInstance().cancelOffer(offer);
                sender.sendMessage(new TextComponentString("Offer cancelled."));
                break;
            }
        }

        for (SellOffer offer : sellOffers) {
            if(++curIndex == cancelIndex) {
                TransactionDatabase.getInstance().cancelOffer(offer);
                sender.sendMessage(new TextComponentString("Offer cancelled. You can collect your items with /ge collect"));
                break;
            }
        }

        if(buyOffers.isEmpty() && sellOffers.isEmpty())
            sender.sendMessage(new TextComponentString("You are not buying or selling anything."));
        return 1;
    };

    private static final Command<CommandSource> buyOffersCommand = context -> {
        EntityPlayerMP sender = (EntityPlayerMP) Objects.requireNonNull(context.getSource().getEntity());
        List<BuyOffer> buyOffers = Lists.newArrayList();
        for (List<BuyOffer> offerList : TransactionDatabase.getBuyOffers().values())
            buyOffers.addAll(offerList);
        buyOffers.removeIf(offer -> offer.getOwner().equals(sender.getUniqueID()));

        int page;
        try {
            page = context.getArgument("page", Integer.class);
        } catch (CommandException e) {
            page = 1;
        }
        //Expand page to be the first entry on the page
        page *= 50;
        //Subtract 50 because the first page starts with entry 0
        page -= 50;
        int orderIndex = page;
        int termLength = 50;
        for (BuyOffer offer : buyOffers) {
            if (page-- > 0)
                continue;
            if (termLength-- <= 0)
                break;
            sender.sendMessage(new TextComponentString(orderIndex++ + ". ").setStyle(TextStyles.YELLOW).appendSibling(new TextComponentString(offer.getAmount() + ' ' + offer.getItemResourceName() + (offer.getNbt() != null ? " with NBT "+offer.getNbt() : "") + " wanted for " + offer.getPrice() + ' ' + GrandEconomyApi.getCurrencyName(offer.getPrice()) + " each").setStyle(TextStyles.BLUE)));
        }

        if(buyOffers.isEmpty())
            sender.sendMessage(new TextComponentString("Nobody is buying anything."));
        return 1;
    };

    private static final Command<CommandSource> sellOffersCommand = context -> {
        EntityPlayerMP sender = (EntityPlayerMP) Objects.requireNonNull(context.getSource().getEntity());
        List<SellOffer> sellOffers = Lists.newArrayList();
        for (List<SellOffer> offerList : TransactionDatabase.getSellOffers().values())
            sellOffers.addAll(offerList);
        sellOffers.removeIf(offer -> offer.getOwner().equals(sender.getUniqueID()));

        int page;
        try {
            page = context.getArgument("page", Integer.class);
        } catch (CommandException e) {
            page = 1;
        }
        //Expand page to be the first entry on the page
        page *= 50;
        //Subtract 50 because the first page starts with entry 0
        page -= 50;
        int orderIndex = page;
        int termLength = 50;
        for (SellOffer offer : sellOffers) {
            if (page-- > 0)
                continue;
            if (termLength-- <= 0)
                break;
            sender.sendMessage(new TextComponentString(orderIndex++ + ". ").setStyle(TextStyles.YELLOW).appendSibling(new TextComponentString(offer.getAmount() + ' ' + offer.getItemResourceName() + (offer.getNbt() != null ? " with NBT "+offer.getNbt() : "") + " being sold for " + offer.getPrice() + ' ' + GrandEconomyApi.getCurrencyName(offer.getPrice()) + " each").setStyle(TextStyles.PURPLE)));
        }

        if(sellOffers.isEmpty())
            sender.sendMessage(new TextComponentString("Nobody is selling anything."));
        return 1;
    };
}
