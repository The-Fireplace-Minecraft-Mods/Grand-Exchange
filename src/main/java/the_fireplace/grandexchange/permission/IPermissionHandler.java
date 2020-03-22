package the_fireplace.grandexchange.permission;

import net.minecraft.entity.player.EntityPlayerMP;

public interface IPermissionHandler {
    boolean hasPermission(EntityPlayerMP player, String permissionName);
    void registerPermission(String permissionName, Object permissionLevel, String permissionDescription);
    boolean permissionManagementExists();
}
