package dev.hytalemodding.interactions;

import java.util.ArrayList;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
// import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent.Backpack;
import com.hypixel.hytale.server.core.inventory.InventoryComponent.Hotbar;
import com.hypixel.hytale.server.core.inventory.InventoryComponent.Storage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.metadata.CapturedNPCMetadata;

import dev.hytalemodding.components.PkmnCaptureMetadata;

public class HealAllPkmnInteraction extends SimpleInstantInteraction{
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    
    @Override
    protected void firstRun(
        InteractionType interactionType, 
        InteractionContext context, 
        CooldownHandler cooldownHandler
    ) {
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        if (commandBuffer == null) { fail(context, "No commandBuffer"); return; }

        World world = commandBuffer.getExternalData().getWorld();

        Ref<EntityStore> playerRef = context.getOwningEntity();
        if(playerRef == null) { fail(context, "No owning entity"); return; }

        // Ref<EntityStore> targetRef = context.getTargetEntity();
        // if(targetRef == null) { fail(context); return; }

        // Player player = commandBuffer.getComponent(playerRef, Player.getComponentType());
        // if(player == null) { fail(context, "owner not a player"); return; }

        // ItemStack ball = context.getHeldItem();
        // if(ball == null) { fail(context); return; }

        // Store<EntityStore> store = commandBuffer.getExternalData().getStore();
        // if (store == null) { fail(context); return; }

        // String heldItemId = ball.getItemId();
        Hotbar   playerHotbar   = commandBuffer.getComponent(playerRef, InventoryComponent.Hotbar.getComponentType());
        Storage  playerStorage  = commandBuffer.getComponent(playerRef, InventoryComponent.Storage.getComponentType());
        Backpack playerBackpack = commandBuffer.getComponent(playerRef, InventoryComponent.Backpack.getComponentType());

        ItemContainer playerHotbarInventory = null;
        ItemContainer playerStorageInventory = null;
        ItemContainer playerBackpackInventory = null;

        if(playerHotbar   != null) playerHotbarInventory    = playerHotbar.getInventory();
        if(playerStorage  != null) playerStorageInventory   = playerStorage.getInventory();
        if(playerBackpack != null) playerBackpackInventory  = playerBackpack.getInventory();

        if (playerHotbarInventory != null) {
            ArrayList<PendingItemReplacement> toReplace = new ArrayList<PendingItemReplacement>();
            playerHotbarInventory.forEach((i, itemstack) -> {
                if (hasCapturedPkmn(itemstack)) {
                    ItemStack healed = healCapturedPkmn(itemstack,commandBuffer);
                    toReplace.add(new PendingItemReplacement(i, itemstack, healed));
                }
            });
            for(int i=0; i<toReplace.size();i++){
                PendingItemReplacement replace = toReplace.get(i);
                playerHotbarInventory.replaceItemStackInSlot(replace.i, replace.before, replace.after);
            }
        }

        if (playerStorageInventory != null) {
            ArrayList<PendingItemReplacement> toReplace = new ArrayList<PendingItemReplacement>();
            playerStorageInventory.forEach((i, itemstack) -> {
                if (hasCapturedPkmn(itemstack)) {
                    ItemStack healed = healCapturedPkmn(itemstack,commandBuffer);
                    toReplace.add(new PendingItemReplacement(i, itemstack, healed));
                }
            });
            for(int i=0; i<toReplace.size();i++){
                PendingItemReplacement replace = toReplace.get(i);
                playerStorageInventory.replaceItemStackInSlot(replace.i, replace.before, replace.after);
            }
        }

        if (playerBackpackInventory != null) {
            ArrayList<PendingItemReplacement> toReplace = new ArrayList<PendingItemReplacement>();
            playerBackpackInventory.forEach((i, itemstack) -> {
                if (hasCapturedPkmn(itemstack)) {
                    ItemStack healed = healCapturedPkmn(itemstack,commandBuffer);
                    toReplace.add(new PendingItemReplacement(i, itemstack, healed));
                }
            });
            for(int i=0; i<toReplace.size();i++){
                PendingItemReplacement replace = toReplace.get(i);
                playerBackpackInventory.replaceItemStackInSlot(replace.i, replace.before, replace.after);
            }
        }
        context.getState().state = InteractionState.Finished;

    }

    public record PendingItemReplacement(short i, ItemStack before, ItemStack after) {}

    public static ItemStack healCapturedPkmn(
        @Nonnull ItemStack itemstack,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ){
        String itemId = itemstack.getItemId();
        PkmnCaptureMetadata captureMetadata = itemstack.getFromMetadataOrNull("PkmnCapture", PkmnCaptureMetadata.CODEC);
        if(captureMetadata==null) return itemstack;
        captureMetadata.setNpcStatus("Healthy");
        captureMetadata.setCurrentHp(captureMetadata.getMaxHp());

        CapturedNPCMetadata npcMetadata = itemstack.getFromMetadataOrDefault("CapturedEntity", CapturedNPCMetadata.CODEC);
            var npcRole =  npcMetadata.getNpcNameKey();
            // npcMetadata.getFullItemIcon();
            // npcMetadata.getIconPath();
                // "IconPath": "Icons/ModelsGenerated/Fainted_Pkmn.png",
                // "NpcNameKey": "Pkmn_Caterpie_Tamed",
                // "FullItemIcon": "Icons/ModelsGenerated/Fainted_Pkmn.png"
        // npcMetadata.setNpcNameKey(tameRoleId);
        // npcMetadata.setIconPath("Icons/Items/"+itemId+"_Full/Fainted_Pkmn.png");
        npcMetadata.setIconPath("Icons/Items/Pokeball/"+npcRole+".png");
        npcMetadata.setFullItemIcon("Icons/Items/Pokeball/"+npcRole+".png");

        ItemStack healedItem = itemstack
            .withMetadata(CapturedNPCMetadata.KEYED_CODEC, npcMetadata)
            .withMetadata(PkmnCaptureMetadata.KEYED_CODEC, captureMetadata)
            .withState("Full");
        return healedItem;
    }


    public boolean hasCapturedPkmn(
        @Nonnull ItemStack itemstack
    ){
        // String itemId = itemstack.getItemId();
        PkmnCaptureMetadata captureMetadata = itemstack.getFromMetadataOrNull("PkmnCapture", PkmnCaptureMetadata.CODEC);
        if (captureMetadata==null) return false;
        if (hasState(itemstack,"Active")) return false;
        // if (itemstack.getItemId() == itemstack.getItem().getItemIdForState("Active")) return false;
        if (captureMetadata.getNpcStatus() == "Active") return false;
        return true;
    }



    public static boolean hasState(
        @Nonnull ItemStack itemstack,
        @Nonnull String state
    ){
        String itemId = itemstack.getItemId();
        String statePrefix = "*";
        String stateSuffix = "_State_"+state;

        return (itemId.startsWith(statePrefix) && itemId.endsWith(stateSuffix));
    }

    private static void fail(@Nonnull InteractionContext context) {
        context.getState().state = InteractionState.Failed;
    }

        private static void fail(@Nonnull InteractionContext context, String logMessage) {
        // if(logMessage != null) {  LOGGER.atInfo().log(logMessage); }
        context.getState().state = InteractionState.Failed;
    }

    public static final BuilderCodec<HealAllPkmnInteraction> CODEC = BuilderCodec.builder(
        HealAllPkmnInteraction.class,
        HealAllPkmnInteraction::new,
        SimpleInstantInteraction.CODEC
    )
    .build();
    
}
