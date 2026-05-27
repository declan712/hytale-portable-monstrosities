package dev.hytalemodding.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.components.PkmnStatsComponent;
import dev.hytalemodding.util.PkmnStatUtils;

public class InitPlayerStatsEvent {

    public static void onPlayerReady(PlayerReadyEvent event) {
        Ref<EntityStore> ref = event.getPlayerRef();
        // Player player = event.getPlayer();
        Store<EntityStore> store = ref.getStore();
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        assignPlayerPkmnStats(playerRef,store);
    }

    public static void assignPlayerPkmnStats(
        PlayerRef playerRef, 
        Store<EntityStore> store
    ) {
        World world = store.getExternalData().getWorld();
        // Store<EntityStore> store = world.getEntityStore().getStore();
        world.execute(() -> {
            Ref<EntityStore> ref = playerRef.getReference();
            PkmnStatsComponent pkmnStats = store.getComponent(ref, PkmnStatsComponent.getComponentType());
            if (pkmnStats == null) pkmnStats = new PkmnStatsComponent();
            pkmnStats.setBaseStats(PkmnStatUtils.PLAYER_BASE_STATS);
            PkmnStatUtils.apply(store,ref,pkmnStats);
            playerRef.sendMessage(Message.raw("Welcome " + playerRef.getUsername()));
            playerRef.sendMessage(Message.raw("Current Level: "+String.valueOf(pkmnStats.getLevel())));
        });
    }

}


