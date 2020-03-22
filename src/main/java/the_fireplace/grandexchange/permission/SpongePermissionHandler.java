package the_fireplace.grandexchange.permission;

import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import the_fireplace.grandexchange.GrandExchange;
import the_fireplace.grandexchange.commands.CommandGe;

import static the_fireplace.grandexchange.permission.PermissionManager.GE_COMMAND_PREFIX;

public class SpongePermissionHandler implements IPermissionHandler {

    private PermissionService permissionService;

    public SpongePermissionHandler() {
        if(!Sponge.getServiceManager().provide(PermissionService.class).isPresent())
            return;
        permissionService = Sponge.getServiceManager().provide(PermissionService.class).get();

        for(String subcommand: CommandGe.commands.keySet())
            registerPermission(GE_COMMAND_PREFIX+subcommand, PermissionDescription.ROLE_USER, "");
        for(String subcommand: CommandGe.opcommands.keySet())
            registerPermission(GE_COMMAND_PREFIX+subcommand, PermissionDescription.ROLE_ADMIN, "");

        registerPermission(GE_COMMAND_PREFIX+"canceloffer.op", PermissionDescription.ROLE_ADMIN, "");
    }

    @Override
    public boolean hasPermission(EntityPlayerMP player, String permissionName) {
        if(permissionManagementExists() && player instanceof Subject)
            return ((Subject) player).hasPermission(permissionName);
        return true;
    }

    @Override
    public void registerPermission(String permissionName, Object permissionLevel, String permissionDescription) {
        permissionService
                .newDescriptionBuilder(GrandExchange.instance)
                .id(permissionName)
                .description(Text.of(permissionDescription))
                .assign(((String)permissionLevel).isEmpty() ? PermissionDescription.ROLE_USER : (String)permissionLevel, !((String) permissionLevel).isEmpty())
                .register();
    }

    @Override
    public boolean permissionManagementExists() {
        return Sponge.getServiceManager().isRegistered(PermissionService.class);
    }
}
