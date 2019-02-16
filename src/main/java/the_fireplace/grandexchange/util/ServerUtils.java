package the_fireplace.grandexchange.util;

import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import the_fireplace.grandexchange.GrandExchange;

import java.util.UUID;

public class ServerUtils {
    public static Entity getEntityFromUUID(UUID entityId) {
        MinecraftServer server = GrandExchange.server;
        for(WorldServer w : server.func_212370_w()) {
            Entity we = w.getEntityFromUuid(entityId);
            if(we != null)
                return we;
        }
        return null;
    }
}
