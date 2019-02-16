package the_fireplace.grandexchange;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.EntityPlayer;

public class GeCommands {
    @SuppressWarnings("Duplicates")
    public static void register(CommandDispatcher<CommandSource> commandDispatcher) {
        LiteralArgumentBuilder<CommandSource> geCommand = Commands.literal("ge").requires((iCommandSender) -> iCommandSender.getEntity() instanceof EntityPlayer);



        commandDispatcher.register(geCommand);
    }
}
