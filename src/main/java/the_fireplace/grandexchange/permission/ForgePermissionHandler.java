package the_fireplace.grandexchange.permission;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import the_fireplace.grandexchange.commands.CommandGe;

import static the_fireplace.grandexchange.permission.PermissionManager.GE_COMMAND_PREFIX;

public class ForgePermissionHandler implements IPermissionHandler {

    public ForgePermissionHandler() {
        for(String subcommand: CommandGe.commands.keySet())
            registerPermission(GE_COMMAND_PREFIX+subcommand, DefaultPermissionLevel.ALL, "");
        for(String subcommand: CommandGe.opcommands.keySet())
            registerPermission(GE_COMMAND_PREFIX+subcommand, DefaultPermissionLevel.OP, "");
        registerPermission(GE_COMMAND_PREFIX+"canceloffer.op", DefaultPermissionLevel.OP, "");
    }

    @Override
    public boolean hasPermission(EntityPlayerMP player, String permissionName) {
        return PermissionAPI.hasPermission(player, permissionName);
    }

    @Override
    public void registerPermission(String permissionName, Object permissionLevel, String permissionDescription) {
        PermissionAPI.registerNode(permissionName, (DefaultPermissionLevel)permissionLevel, permissionDescription);
    }

    @Override
    public boolean permissionManagementExists() {
        return true;
    }
}
