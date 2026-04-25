package dev.hytalemodding.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.components.PkmnStatsComponent;
import dev.hytalemodding.util.PkmnStatUtils;

public class InitPlayerStatsEvent {

    public static void onPlayerReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();
        player.sendMessage(Message.raw("Welcome " + player.getDisplayName()));
        assignPlayerPkmnStats(player);
    }

    public static void assignPlayerPkmnStats(Player player) {
        World world = player.getWorld();
        Store<EntityStore> store = world.getEntityStore().getStore();
        world.execute(() -> {
            Ref<EntityStore> playerRef = player.getReference();
            PkmnStatsComponent pkmnStats = store.getComponent(playerRef, PkmnStatsComponent.getComponentType());
            if (pkmnStats == null) pkmnStats = new PkmnStatsComponent();
            pkmnStats.setBaseStats(PkmnStatUtils.PLAYER_BASE_STATS);
            PkmnStatUtils.apply(store,playerRef,pkmnStats);
        });
    }

}


