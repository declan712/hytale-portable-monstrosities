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
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.metadata.CapturedNPCMetadata;

import dev.hytalemodding.components.FaintedPkmnComponent;
import dev.hytalemodding.components.PkmnCaptureMetadata;
import dev.hytalemodding.components.PkmnStatsComponent;
import dev.hytalemodding.util.PkmnStatUtils;

public class ReturnFaintedPkmnInteraction extends SimpleInstantInteraction {
    public  static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private String faintedIcon = "Icons/ModelsGenerated/Fainted_Pkmn.png";
    private String[] acceptedItems = {
        "Pokeball",
        "Pokeball_Great",
        "Pokeball_Ultra"
    };

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

        Ref<EntityStore> targetRef = context.getTargetEntity();
        if(targetRef == null) { fail(context); return; }

        Player player = commandBuffer.getComponent(playerRef, Player.getComponentType());
        if(player == null) { fail(context); return; }

        ItemStack ball = context.getHeldItem();
        if(ball == null) { fail(context); return; }

        Store<EntityStore> store = commandBuffer.getExternalData().getStore();
        if (store == null) { fail(context); return; }


        FaintedPkmnComponent faintedPkmnComponent = commandBuffer.getComponent(targetRef,FaintedPkmnComponent.getComponentType());
        if (faintedPkmnComponent == null) {
            LOGGER.atInfo().log("Tombstone has no FaintedPkmnComponent");
            fail(context); return;
        }
        PkmnStatsComponent pkmnStats = commandBuffer.getComponent(targetRef,PkmnStatsComponent.getComponentType());
        if (pkmnStats == null) {
            LOGGER.atInfo().log("Tombstone has no PkmnStatsComponent");
            fail(context); return;
        }

        String heldItemId = ball.getItemId();
        
        boolean validItem = false;
        for (int i=0;i<acceptedItems.length;i++){
            if (acceptedItems[i].equals(heldItemId)) {
                validItem = true;
            }
        }
        if (!validItem){
            LOGGER.atInfo().log("Held item not valid: "+heldItemId);
            fail(context); 
            return;
        }
        String roleId = faintedPkmnComponent.getNpcRoleId();
        // PkmnStatsComponent  maybeStats =  faintedPkmnComponent.getPkmnStats();
        // if(maybeStats!=null){
        //     LOGGER.atInfo().log("faintedPkmnComponent.getPkmnStats not null");
        //     LOGGER.atInfo().log(" - LVL:" + String.valueOf(maybeStats.getLevel()));
        //     LOGGER.atInfo().log(" - EXP:" + String.valueOf(maybeStats.getExperience()));
        // }

        CapturedNPCMetadata npcMetadata = new CapturedNPCMetadata();
        PkmnCaptureMetadata pkmnMetadata = new PkmnCaptureMetadata();
        pkmnMetadata.setCurrentHp(0);
        pkmnMetadata.setBaseStats(pkmnStats.getBaseStats());
        pkmnMetadata.setEvs(pkmnStats.getEvs());
        pkmnMetadata.setIvs(pkmnStats.getIvs());
        pkmnMetadata.setExperience(pkmnStats.getExperience());
        pkmnMetadata.setLevel(pkmnMetadata.getLevel());
        pkmnMetadata.setNpcStatus("Fainted");
        pkmnMetadata.setOwnerUuid(pkmnStats.getOwnerUuid());

        pkmnMetadata.setNpcEntityUuid(faintedPkmnComponent.getNpcUuid());

        npcMetadata.setNpcNameKey(roleId);
        npcMetadata.setFullItemIcon(faintedIcon);
        npcMetadata.setIconPath(faintedIcon);

        ItemStack singleBall = ball.withQuantity(1);
        ItemStack capturedBall = singleBall
                .withMetadata(CapturedNPCMetadata.KEYED_CODEC,npcMetadata)
                .withMetadata(PkmnCaptureMetadata.KEYED_CODEC,pkmnMetadata)
                .withState("Fainted");


        var hotbar = store.getComponent(playerRef, InventoryComponent.Hotbar.getComponentType());
        var slot = hotbar.getActiveSlot();
        var inventory = hotbar.getInventory();

        ItemStack remainder = ball.withQuantity(ball.getQuantity() - 1);
        if(remainder == null){
            inventory.replaceItemStackInSlot((short) slot, ball, capturedBall);
        } else if (remainder.getQuantity() > 0) {
            inventory.replaceItemStackInSlot((short) slot, ball, remainder);
            inventory.addItemStack(capturedBall);
        } else {
            inventory.replaceItemStackInSlot((short) slot, ball, capturedBall);
        }
        commandBuffer.removeEntity(targetRef, RemoveReason.REMOVE);
        context.getState().state = InteractionState.Finished;
        return;
    }

    private static void fail(@Nonnull InteractionContext context) {
        context.getState().state = InteractionState.Failed;
    }

    public static final BuilderCodec<ReturnFaintedPkmnInteraction> CODEC = BuilderCodec.builder(
        ReturnFaintedPkmnInteraction.class,
        ReturnFaintedPkmnInteraction::new,
        SimpleInstantInteraction.CODEC
    )
    .build();
    
}
