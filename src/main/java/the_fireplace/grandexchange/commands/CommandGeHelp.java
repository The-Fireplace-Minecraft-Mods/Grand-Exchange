package the_fireplace.grandexchange.commands;

import com.google.common.collect.Lists;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import the_fireplace.grandexchange.util.ChatPageUtil;
import the_fireplace.grandexchange.util.translation.TranslationUtil;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CommandGeHelp extends CommandBase {
    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return TranslationUtil.getRawTranslationString(sender, "commands.ge.help.usage");
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, @Nullable String[] args) throws CommandException {
        if(args == null || args.length == 0 || args[0].matches("\\d+")) {
            int page = args == null || args.length < 1 ? 1 : parseInt(args[0]);
            List<ITextComponent> helps = Lists.newArrayList();
            for (Map.Entry<String, CommandBase> command : CommandGe.commands.entrySet())
                helps.add(TranslationUtil.getTranslation(sender, "commands.ge.help.format",
                        TranslationUtil.getStringTranslation(sender, "commands.ge." + command.getKey() + ".usage"),
                        TranslationUtil.getStringTranslation(sender, "commands.ge." + command.getKey() + ".description")));
            helps.sort(Comparator.comparing(ITextComponent::getUnformattedText));

            ChatPageUtil.showPaginatedChat(sender, "/clan help %s", helps, page);
        } else if(CommandGe.aliases.containsKey(args[0]) || CommandGe.commands.containsKey(args[0])) {
            sender.sendMessage(TranslationUtil.getTranslation(sender, "commands.ge.help.format",
                    TranslationUtil.getStringTranslation(sender, "commands.ge." + CommandGe.processAlias(args[0]) + ".usage"),
                    TranslationUtil.getStringTranslation(sender, "commands.ge." + CommandGe.processAlias(args[0]) + ".description")));
        } else {
            sender.sendMessage(TranslationUtil.getTranslation(sender, "commands.ge.help.invalid", args[0]));
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        List<String> comp = Lists.newArrayList();
        if(args.length != 1)
            return comp;
        for(int i=1;i<(CommandGe.commands.size()+CommandGe.opcommands.size()+CommandGe.aliases.size())/ChatPageUtil.RESULTS_PER_PAGE;i++)
            comp.add(String.valueOf(i));
        comp.addAll(CommandGe.aliases.keySet());
        comp.addAll(CommandGe.commands.keySet());
        return comp;
    }
}
