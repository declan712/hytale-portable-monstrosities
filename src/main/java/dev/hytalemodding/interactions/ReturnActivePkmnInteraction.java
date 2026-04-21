package dev.hytalemodding.interactions;

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
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.metadata.CapturedNPCMetadata;

import dev.hytalemodding.components.PkmnCaptureMetadata;
import dev.hytalemodding.util.PkmnStatUtils;

public class ReturnActivePkmnInteraction extends SimpleInstantInteraction {
    public  static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    protected void firstRun(
        InteractionType interactionType, 
        InteractionContext context, 
        CooldownHandler cooldownHandler
    ) {
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        if (commandBuffer == null) { fail(context); return; }

        World world = commandBuffer.getExternalData().getWorld();

        Ref<EntityStore> playerRef = context.getOwningEntity();
        if(playerRef == null) { fail(context); return; }

        Player player = commandBuffer.getComponent(playerRef, Player.getComponentType());
        if(player == null) { fail(context); return; }

        ItemStack ball = context.getHeldItem();
        if(ball == null) { fail(context); return; }

        Store<EntityStore> store = commandBuffer.getExternalData().getStore();
        if (store == null) { fail(context); return; }


        PkmnCaptureMetadata pkmnMetadata = ball.getFromMetadataOrNull("PkmnCapture", PkmnCaptureMetadata.CODEC);
        if (pkmnMetadata == null) { 
            player.sendMessage(Message.raw("Item missing capture metadata"));
            LOGGER.atInfo().log("Item has no Pkmn Metadata");
            fail(context); return; }
        
        String npcStatus = pkmnMetadata.getNpcStatus();
        if(npcStatus != "Active"){ fail(context); return; }
        
        String npcId = pkmnMetadata.getNpcEntityUuid();

        Ref<EntityStore> targetRef = world.getEntityRef(UUID.fromString(npcId));
        if (targetRef == null) { 
            player.sendMessage(Message.raw("Couldn't find entity :("));
            LOGGER.atInfo().log("Couldn't find entity");
            fail(context); 
            return; 
        }

        PkmnCaptureMetadata captureMeta = 
                PkmnStatUtils.captureMetadata(commandBuffer, targetRef);

        CapturedNPCMetadata npcMeta = 
            PkmnStatUtils.getNpcMetadata(commandBuffer,targetRef,ball,null);

        ItemStack modifiedBall = ball
                .withMetadata(CapturedNPCMetadata.KEYED_CODEC,npcMeta)
                .withMetadata(PkmnCaptureMetadata.KEYED_CODEC,captureMeta);

        if (captureMeta.getNpcStatus()=="Fainted"){
            modifiedBall=modifiedBall.withState("Fainted");
        } else {
            modifiedBall=modifiedBall.withState("Full");
        }


        var hotbar = store.getComponent(playerRef, InventoryComponent.Hotbar.getComponentType());
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
        SimpleInstantInteraction.CODEC
    )
    .build();
    
}
