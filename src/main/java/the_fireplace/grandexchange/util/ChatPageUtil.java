package the_fireplace.grandexchange.util;

import com.google.common.collect.Lists;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.event.ClickEvent;

import java.util.List;

public class ChatPageUtil {

    public static void showPaginatedChat(ICommandSender sender, String command, List<ITextComponent> items, int page) {
        int resultsOnPage = 7;
        int totalPageCount = items.size() % resultsOnPage > 0 ? (items.size()/resultsOnPage)+1 : items.size()/resultsOnPage;

        ITextComponent counter = new TextComponentString("Page: " + page + "/" + totalPageCount);
        ITextComponent top = new TextComponentString(MinecraftColors.GREEN + "-----------------").appendSibling(counter).appendText(MinecraftColors.GREEN + "-------------------");

        //Expand page to be the first entry on the page
        page *= resultsOnPage;
        //Subtract result count because the first page starts with entry 0
        page -= resultsOnPage;
        int termLength = resultsOnPage;
        List<ITextComponent> printItems = Lists.newArrayList();

        for (ITextComponent item: items) {
            if (page-- > 0)
                continue;
            if (termLength-- <= 0)
                break;
            printItems.add(item);
        }

        ITextComponent nextButton = page < totalPageCount ? new TextComponentString("[Next]").setStyle(new Style().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format(command, page+1)))) : new TextComponentString("-----");
        ITextComponent prevButton = page > 1 ? new TextComponentString("[Previous]").setStyle(new Style().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format(command, page-1)))) : new TextComponentString("------");
        ITextComponent bottom = new TextComponentString(MinecraftColors.GREEN + "---------------").appendSibling(prevButton).appendText(MinecraftColors.GREEN + "---").appendSibling(nextButton).appendText(MinecraftColors.GREEN + "-------------");

        sender.sendMessage(top);

        for(ITextComponent item: printItems)
            sender.sendMessage(item);

        sender.sendMessage(bottom);
    }

    public static void showPaginatedChat(ICommandSender target, String command, List<ITextComponent> items) {
        showPaginatedChat(target, command, items, 1);
    }
}
