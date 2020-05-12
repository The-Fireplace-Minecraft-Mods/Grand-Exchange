package the_fireplace.grandexchange.commands;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.ArrayUtils;
import the_fireplace.grandexchange.permission.PermissionManager;
import the_fireplace.grandexchange.util.translation.TranslationUtil;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CommandGe extends CommandBase {
    public static final HashMap<String, CommandBase> commands = new HashMap<String, CommandBase>() {
        {
            this.put("buy", new CommandBuy());
            this.put("sell", new CommandSell());
            this.put("sellthis", new CommandSellThis());
            this.put("identify", new CommandIdentify());
            this.put("collect", new CommandCollect());
            this.put("buyoffers", new CommandBuyOffers());
            this.put("selloffers", new CommandSellOffers());
            this.put("myoffers", new CommandMyOffers());
            this.put("canceloffer", new CommandCancelOffer());
            this.put("help", new CommandGeHelp());
        }
    };
    public static final HashMap<String, CommandBase> opcommands = new HashMap<String, CommandBase>() {
        {
            this.put("opbuy", new CommandOpBuy());
            this.put("opsell", new CommandOpSell());
            this.put("opsellthis", new CommandOpSellThis());
            this.put("opoffers", new CommandOpOffers());
        }
    };
    public static final Map<String, String> aliases = Maps.newHashMap();

    static {
        aliases.put("b", "buy");
        aliases.put("s", "sell");
        aliases.put("st", "sellthis");
        aliases.put("i", "identify");
        aliases.put("c", "collect");
        aliases.put("bo", "buyoffers");
        aliases.put("so", "selloffers");
        aliases.put("m", "myoffers");
        aliases.put("mo", "myoffers");
        aliases.put("co", "canceloffer");

        aliases.put("ob", "opbuy");
        aliases.put("os", "opsell");
        aliases.put("ost", "opsellthis");
        aliases.put("oo", "opoffers");
    }

    public static String processAlias(String subCommand) {
        return aliases.getOrDefault(subCommand, subCommand);
    }

    @Override
    public String getName() {
        return "ge";
    }

    @Override
    public List<String> getAliases() {
        return Lists.newArrayList("grandexchange", "gex", "exchange");
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
        //Check permissions and run command
        if(!PermissionManager.permissionManagementExists() || PermissionManager.hasPermission(sender, PermissionManager.GE_COMMAND_PREFIX+processAlias(tag))) {
            String alias = processAlias(tag);
            if(commands.containsKey(alias))
                commands.get(alias).execute(server, sender, args);
            else if(opcommands.containsKey(alias))
                opcommands.get(alias).execute(server, sender, args);
            else
                throw new WrongUsageException(getUsage(sender));
            return;
        } else if(commands.containsKey(tag) || opcommands.containsKey(tag) || aliases.containsKey(tag))
            throw new CommandException("commands.generic.permission");
        throw new WrongUsageException(getUsage(sender));
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos)
    {
        if(args.length == 1){
            Collection<String> ret = Lists.newArrayList(commands.keySet());
            if(!(sender instanceof EntityPlayerMP) || ArrayUtils.contains(server.getPlayerList().getOppedPlayerNames(), sender.getName()))
                ret.addAll(opcommands.keySet());
            return getListOfStringsMatchingLastWord(args, ret);
        }
        return Collections.emptyList();
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
