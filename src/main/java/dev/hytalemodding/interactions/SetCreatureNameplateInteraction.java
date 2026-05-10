package dev.hytalemodding.interactions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import dev.hytalemodding.components.PkmnCaptureMetadata;
import dev.hytalemodding.components.PkmnStatsComponent;
import dev.hytalemodding.util.PkmnStatUtils;


public class SetCreatureNameplateInteraction extends SimpleInstantInteraction {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
        public static final BuilderCodec<SetCreatureNameplateInteraction> CODEC = BuilderCodec.builder(
        SetCreatureNameplateInteraction.class, 
        SetCreatureNameplateInteraction::new, 
        SimpleInstantInteraction.CODEC
    ).build();

    @Override
    protected void firstRun(
        @Nonnull InteractionType interactionType,
        @Nonnull InteractionContext interactionContext,
        @Nonnull CooldownHandler cooldownHandler
    ) {
        CommandBuffer<EntityStore> commandBuffer = interactionContext.getCommandBuffer();
        if (commandBuffer == null) {
            fail(interactionContext);
            return;
        }
        Store<EntityStore> store = commandBuffer.getExternalData().getStore();
        Ref<EntityStore> ref = interactionContext.getEntity();
        if (ref == null) {
            fail(interactionContext);
            return;
        }
        Player player = commandBuffer.getComponent(ref, Player.getComponentType());
        if(player != null){
            LOGGER.atInfo().log("Running for player");
            // triggered by player level up, not naming NPC
            PkmnStatsComponent pkmnStats = commandBuffer.getComponent(ref, PkmnStatsComponent.getComponentType());
            if (pkmnStats == null) {
                LOGGER.atInfo().log("Current stats NULL");
                pkmnStats = new PkmnStatsComponent();
            }
            if(pkmnStats.getBaseStats() != PkmnStatUtils.PLAYER_BASE_STATS){ 
                LOGGER.atInfo().log("Setting base stats");
                pkmnStats.setBaseStats(PkmnStatUtils.PLAYER_BASE_STATS);
            }
            EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());
            IndexedLookupTableAssetMap<String, EntityStatType> assetMap = EntityStatType.getAssetMap();
            int lvlIdx = assetMap.getIndex("Lvl");
            int expIdx = assetMap.getIndex("Exp");
            EntityStatValue lvl = stats.get(lvlIdx);
            EntityStatValue exp = stats.get(expIdx);
            float currentExp = exp.get();
            float currentLvl = lvl.get();
            pkmnStats.setLevel((int)currentLvl);
            pkmnStats.setExperience((int)currentExp);
            PkmnStatUtils.apply(store,commandBuffer,ref,pkmnStats);
            commandBuffer.putComponent(ref, PkmnStatsComponent.getComponentType(), pkmnStats);
            next(interactionContext);
            return; 
        }

        NPCEntity npcEntity = commandBuffer.getComponent(ref,NPCEntity.getComponentType());
        if(npcEntity==null){
            fail(interactionContext);
            return;
        }
        String roleName = npcEntity.getRoleName();
        if(!PkmnStatUtils.filterByRoleName(roleName)) {
            fail(interactionContext);
            return;
        }

        PkmnCaptureMetadata metadata = PkmnStatUtils.captureMetadata(commandBuffer,ref);
        PkmnStatsComponent pkmnStats = PkmnStatUtils.fromMetadata(metadata);

        PkmnStatUtils.apply(store, commandBuffer, ref, pkmnStats);

        PkmnStatUtils.setPkmnNameplate(commandBuffer,ref,roleName,pkmnStats);
        next(interactionContext);
        return;
    }

    private void next(@Nonnull InteractionContext interactionContext){
        interactionContext.getState().state = InteractionState.NotFinished;
    }

    private void fail(@Nonnull InteractionContext interactionContext){
        interactionContext.getState().state = InteractionState.Failed;
    }


}
