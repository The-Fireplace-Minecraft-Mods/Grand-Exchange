package the_fireplace.grandexchange.commands;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import the_fireplace.grandexchange.util.translation.TranslationUtil;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CommandGe extends CommandBase {
    private static final CommandBase buy = new CommandBuy();
    private static final CommandBase sell = new CommandSell();
    private static final CommandBase sellthis = new CommandSellThis();
    private static final CommandBase identify = new CommandIdentify();
    private static final CommandBase collect = new CommandCollect();
    private static final CommandBase buyoffers = new CommandBuyOffers();
    private static final CommandBase selloffers = new CommandSellOffers();
    private static final CommandBase myoffers = new CommandMyOffers();
    private static final CommandBase canceloffer = new CommandCancelOffer();

    private static final CommandBase opbuy = new CommandOpBuy();
    private static final CommandBase opsell = new CommandOpSell();
    private static final CommandBase opsellthis = new CommandOpSellThis();
    private static final CommandBase opoffers = new CommandOpOffers();

    @Override
    public String getName() {
        return "ge";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return TranslationUtil.getRawTranslationString(sender, "commands.ge.usage");
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if(args.length <= 0)
            throw new WrongUsageException(getUsage(sender));
        String tag = args[0];
        if(args.length > 1)
            args = Arrays.copyOfRange(args, 1, args.length);
        else
            args = new String[]{};
        switch(tag){//TODO migrate these to maps like in Clans
            case "buy":
            case "b":
                buy.execute(server, sender, args);
                return;
            case "sell":
            case "s":
                sell.execute(server, sender, args);
                return;
            case "sellthis":
            case "st":
                sellthis.execute(server, sender, args);
                return;
            case "identify":
            case "i":
                identify.execute(server, sender, args);
                return;
            case "collect":
            case "c":
                collect.execute(server, sender, args);
                return;
            case "buyoffers":
            case "bo":
                buyoffers.execute(server, sender, args);
                return;
            case "selloffers":
            case "so":
                selloffers.execute(server, sender, args);
                return;
            case "myoffers":
            case "m":
            case "mo":
                myoffers.execute(server, sender, args);
                return;
            case "canceloffer":
            case "co":
                canceloffer.execute(server, sender, args);
                return;
            case "opbuy":
            case "ob":
                opbuy.execute(server, sender, args);
                return;
            case "opsell":
            case "os":
                opsell.execute(server, sender, args);
                return;
            case "opsellthis":
            case "ost":
                opsellthis.execute(server, sender, args);
                return;
            case "opoffers":
            case "oo":
                opoffers.execute(server, sender, args);
                return;
            case "help":
            case "h":
                sender.sendMessage(new TextComponentString("/ge commands:\n" +
                        "buy\n" +
                        "sell\n" +
                        "sellthis\n" +
                        "identify\n" +
                        "collect\n" +
                        "buyoffers\n" +
                        "selloffers\n" +
                        "myoffers\n" +
                        "canceloffer\n" +
                        "opbuy\n" +
                        "opsell\n" +
                        "opsellthis\n" +
                        "help"));
                return;
        }
        throw new WrongUsageException(getUsage(sender));
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos)
    {
        if(args.length == 1){
            return getListOfStringsMatchingLastWord(args, "buy","sell","sellthis","identify","collect","buyoffers","selloffers","myoffers","canceloffer","help");
        }
        return Collections.emptyList();
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
