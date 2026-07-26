package dw.portablemonstrosities.interactions;

import javax.annotation.Nonnull;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import dw.portablemonstrosities.actions.InitPkmnAction;
import dw.portablemonstrosities.components.PkmnCaptureMetadata;
import dw.portablemonstrosities.components.PkmnStatsComponent;
import dw.portablemonstrosities.util.PkmnStatUtils;


public class SetCreatureNameplateInteraction extends SimpleInteraction {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final BuilderCodec<SetCreatureNameplateInteraction> CODEC = BuilderCodec.builder(
        SetCreatureNameplateInteraction.class, 
        SetCreatureNameplateInteraction::new, 
        SimpleInteraction.CODEC
    ).build();

    @Override
    protected final void tick0(
        boolean firstRun, 
        float time, 
        @Nonnull InteractionType type, 
        @Nonnull InteractionContext context, 
        @Nonnull CooldownHandler cooldownHandler
    ) {
        if (firstRun) {
            this.firstRun(type, context, cooldownHandler);
            super.tick0(firstRun, time, type, context, cooldownHandler);
        }
    }
    
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
        if(player !=null){
            // update Player Stats
            InitPkmnAction.initPlayer(ref,store,commandBuffer);
            next(interactionContext);
            return;
        }

        NPCEntity npcEntity = commandBuffer.getComponent(ref,NPCEntity.getComponentType());
        if(npcEntity==null){
            fail(interactionContext);
            return;
        }
        
        String roleName = npcEntity.getRoleName();
        if(!PkmnStatUtils.filterByRoleName(roleName)) { fail(interactionContext); return; }

        PkmnStatsComponent pkmnStats = PkmnStatUtils.getPkmnStatsComponent(commandBuffer, ref);
        // PkmnCaptureMetadata metadata = PkmnStatUtils.captureMetadata(commandBuffer,ref);
        // PkmnStatsComponent pkmnStats = PkmnStatUtils.fromMetadata(metadata);
        
        PkmnStatUtils.apply(store,commandBuffer, ref, pkmnStats);
        PkmnStatUtils.setPkmnNameplate(commandBuffer,ref,roleName,pkmnStats);
        next(interactionContext);
    }

    private void next(@Nonnull InteractionContext interactionContext){
        interactionContext.getState().state = InteractionState.NotFinished;
    }

    private void fail(@Nonnull InteractionContext interactionContext){
        interactionContext.getState().state = InteractionState.Failed;
    }

}
