package dw.portablemonstrosities.interactions;

import java.util.UUID;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.metadata.CapturedNPCMetadata;

import dw.portablemonstrosities.components.PkmnCaptureMetadata;
import dw.portablemonstrosities.util.PkmnStatUtils;

public class ReturnActivePkmnInteraction extends SimpleInteraction {
    public  static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

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
        InteractionType interactionType, 
        InteractionContext context, 
        CooldownHandler cooldownHandler
    ) {
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        if (commandBuffer == null) { fail(context); return; }

        World world = commandBuffer.getExternalData().getWorld();

        Ref<EntityStore> ownerRef = context.getOwningEntity();
        if(ownerRef == null) { fail(context); return; }

        Player player = commandBuffer.getComponent(ownerRef, Player.getComponentType());
        if(player == null) { fail(context); return; }

        PlayerRef playerRef= commandBuffer.getComponent(ownerRef, PlayerRef.getComponentType());
        if(playerRef == null) { fail(context); return; }

        ItemStack ball = context.getHeldItem();
        if(ball == null) { fail(context); return; }

        Store<EntityStore> store = commandBuffer.getExternalData().getStore();
        if (store == null) { fail(context); return; }


        PkmnCaptureMetadata pkmnMetadata = ball.getFromMetadataOrNull("PkmnCapture", PkmnCaptureMetadata.CODEC);
        if (pkmnMetadata == null) { 
            playerRef.sendMessage(Message.raw("Item missing capture metadata"));
            // LOGGER.atInfo().log("Item has no Pkmn Metadata");
            fail(context); return; }
        
        String npcStatus = pkmnMetadata.getNpcStatus();
        if(npcStatus != "Active"){ fail(context); return; }
        
        String npcId = pkmnMetadata.getNpcEntityUuid();
        if (npcId == null) {
            LOGGER.atInfo().log("npcId is NULL");
            fail(context); 
            return; 
        }
        
        Ref<EntityStore> targetRef = world.getEntityRef(UUID.fromString(npcId));
        if (targetRef == null) { 
            playerRef.sendMessage(Message.raw("Couldn't find entity :("));
            // LOGGER.atInfo().log("Couldn't find entity");
            fail(context); 
            return; 
        }

        PkmnCaptureMetadata captureMeta =  PkmnStatUtils.captureMetadata(commandBuffer, targetRef);
        CapturedNPCMetadata npcMeta =  PkmnStatUtils.getNpcMetadataFromBall(commandBuffer,targetRef,ball,null);

        ItemStack modifiedBall = ball
                .withMetadata(CapturedNPCMetadata.KEYED_CODEC,npcMeta)
                .withMetadata(PkmnCaptureMetadata.KEYED_CODEC,captureMeta);

        if (captureMeta.getNpcStatus()=="Fainted"){
            modifiedBall=modifiedBall.withState("Fainted");
        } else {
            modifiedBall=modifiedBall.withState("Full");
        }


        var hotbar = store.getComponent(ownerRef, InventoryComponent.Hotbar.getComponentType());
        var slot = hotbar.getActiveSlot();
        var inventory = hotbar.getInventory();
        inventory.replaceItemStackInSlot(slot, ball, modifiedBall);

        commandBuffer.removeEntity(targetRef, RemoveReason.REMOVE);

        context.getState().state = InteractionState.Finished;
    }

    private static void fail(@Nonnull InteractionContext context) {
        context.getState().state = InteractionState.Failed;
    }

    public static final BuilderCodec<ReturnActivePkmnInteraction> CODEC = BuilderCodec.builder(
        ReturnActivePkmnInteraction.class,
        ReturnActivePkmnInteraction::new,
        SimpleInteraction.CODEC
    )
    .build();
    
}
