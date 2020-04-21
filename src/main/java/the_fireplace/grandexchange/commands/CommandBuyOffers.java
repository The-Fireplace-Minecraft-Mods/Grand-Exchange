package the_fireplace.grandexchange.commands;

import com.google.common.collect.Lists;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import the_fireplace.grandexchange.market.ExchangeManager;
import the_fireplace.grandexchange.market.Offer;
import the_fireplace.grandexchange.market.OfferType;
import the_fireplace.grandexchange.util.ChatPageUtil;
import the_fireplace.grandexchange.util.TextStyles;
import the_fireplace.grandexchange.util.Utils;
import the_fireplace.grandexchange.util.translation.TranslationUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CommandBuyOffers extends CommandBase {

    @Override
    public String getName() {
        return "buyoffers";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return TranslationUtil.getRawTranslationString(sender, "commands.ge.buyoffers.usage");
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if(args.length <= 2) {
            List<Offer> offers = Lists.newArrayList(ExchangeManager.getOffers(OfferType.BUY));
            int page = 1;
            if (args.length == 2)
                try {
                    page = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    throw new CommandException(TranslationUtil.getRawTranslationString(sender, "commands.ge.common.invalid_page"));
                }
            
            String buysearch;
            if(args.length >= 1){
                buysearch = args[0]==null ? ".*" : args[0];
                if(buysearch.matches("^[a-zA-Z_]*$")) buysearch = "minecraft:"+ buysearch;
                else if(buysearch.equals("any") || buysearch.equals("*")) buysearch = ".*";
                final List<String> buyResults = Utils.getListOfStringsMatchingString(buysearch, Utils.getOfferNames(offers));

                offers.removeIf(offer -> !buyResults.contains(offer.getItemResourceName()));
            } else {
            	buysearch = ".*";
            }

            ArrayList<ITextComponent> messages = Lists.newArrayList();
            for (Offer offer : offers)
                messages.add(offer.getOfferChatMessage(sender));
            if(offers.isEmpty())
            	sender.sendMessage(TranslationUtil.getTranslation(sender, "commands.ge.common.no_results").setStyle(TextStyles.RED));
            else
                ChatPageUtil.showPaginatedChat(sender, "/ge buyoffers " + buysearch + " %s", messages, page);
        } else
            throw new WrongUsageException(getUsage(sender));
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }
}
