package the_fireplace.grandexchange.permission;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import the_fireplace.grandexchange.GrandExchange;

public final class PermissionManager {

    public static final String GE_COMMAND_PREFIX = "command.grandexchange.ge.";

    public static boolean hasPermission(EntityPlayerMP player, String permissionKey) {
        if(GrandExchange.getPermissionHandler() != null)
            return GrandExchange.getPermissionHandler().hasPermission(player, permissionKey);
        else
            return true;
    }

    public static boolean hasPermission(ICommandSender sender, String permissionKey) {
        if(sender instanceof EntityPlayerMP)
            return hasPermission((EntityPlayerMP)sender, permissionKey);
        return true;
    }

    public static boolean permissionManagementExists() {
        return GrandExchange.getPermissionHandler().permissionManagementExists();
    }
}
