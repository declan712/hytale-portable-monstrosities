package dev.hytalemodding.interactions;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3i;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Rotation3f;
// import com.hypixel.hytale.math.vector.Vector3d;
// import com.hypixel.hytale.math.vector.Vector3f;
// import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.InventoryComponent.Backpack;
import com.hypixel.hytale.server.core.inventory.InventoryComponent.Hotbar;
import com.hypixel.hytale.server.core.inventory.InventoryComponent.Storage;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.modules.interaction.Interactions;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.metadata.CapturedNPCMetadata;

import dev.hytalemodding.components.FaintedPkmnComponent;
import dev.hytalemodding.components.PkmnCaptureMetadata;
import dev.hytalemodding.components.PkmnStatsComponent;
import dev.hytalemodding.util.PkmnStatUtils;


/// get entity location
/// place block in location
/// give block entityCaptureMetadata
/// allow recovery with pokeball


public class SpawnPkmnTombstoneInteraction extends SimpleInteraction {
    public  static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    // private String tombstoneItemId = "Pkmn_Tombstone";
    private String tombstoneModelId = "Fainted_Pkmn";
    private String faintedIcon = "Icons/ModelsGenerated/Fainted_Pkmn.png";
    private static final List<String> validItems = List.of(
        "Pokeball",
        "Pokeball_Apricorn",
        "Pokeball_Great",
        "Pokeball_Ultra",

        "*Pokeball_State_Active",
        "*Pokeball_Apricorn_State_Active",
        "*Pokeball_Great_State_Active",
        "*Pokeball_Ultra_State_Active"
    );

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

        Ref<EntityStore> ref = context.getOwningEntity();
        if(ref == null) { fail(context); return; }

        Store<EntityStore> store = commandBuffer.getExternalData().getStore();
        if (store == null) { fail(context); return; }

        NPCEntity npcEntity = commandBuffer.getComponent(ref, NPCEntity.getComponentType());
        if (npcEntity == null) { fail(context); return; }

        String roleId = npcEntity.getRoleName();

        DeathComponent deathComponent = store.getComponent(ref, DeathComponent.getComponentType());
        if (deathComponent == null) { 
            // fail(context); return; 
        }

        PkmnStatsComponent pkmnStats = store.getComponent(ref, PkmnStatsComponent.getComponentType());
        UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
        if(uuidComponent==null){ fail(context); return; }
        String entityId = uuidComponent.getUuid().toString();

        PkmnCaptureMetadata pkmnMeta = PkmnStatUtils.captureMetadata(commandBuffer,ref);
            pkmnMeta.setNpcStatus("Fainted");
            pkmnMeta.setNpcEntityUuid(entityId);
            pkmnMeta.setCurrentHp(0);
        
        String ownerUsername =  pkmnMeta.getOwner();
        if(ownerUsername!=null){
            PlayerRef playerRef = PkmnStatUtils.findOwner(world, ownerUsername);
            if(playerRef != null){
                Ref<EntityStore> ownerRef = playerRef.getReference();
                if(ownerRef!=null && ownerRef.isValid()){
                    boolean result = returnToPlayer(
                        context, 
                        store,
                        ref,
                        world,
                        roleId,
                        entityId,
                        pkmnStats,
                        pkmnMeta,
                        ownerRef
                    );
                    if(result) return;
                }
            }


            // LOGGER.atInfo().log("Unable to return to owner");
        }else{
            // LOGGER.atInfo().log("no OwnerUUID, spawn tombstone instead");
        }
        spawnAsTombstone(
            context, 
            store,
            ref,
            world,
            roleId,
            entityId,
            pkmnStats
        );

        context.getState().state = InteractionState.Finished;
    }

    
    private void spawnAsTombstone(
        InteractionContext context, 
        Store<EntityStore> store,
        Ref<EntityStore> ref,
        World world,
        String roleId,
        String entityId,
        PkmnStatsComponent pkmnStats
    ){
        // item drop pos
        TransformComponent transform = store.getComponent(ref, EntityModule.get().getTransformComponentType());
        if (transform == null) { fail(context); return; }
        Vector3d npcPos = transform.getPosition();
        Vector3i pos = new Vector3i(
            (int) npcPos.x,
            (int) npcPos.y,
            (int) npcPos.z
        );

        long       chunkIndex = ChunkUtil.indexChunkFromBlock(pos.x, pos.z);
        WorldChunk worldChunk = world.getChunk(chunkIndex);
        if (worldChunk == null) { 
            fail(context); 
            return; 
        }

        FaintedPkmnComponent faintedPkmnComponent = new FaintedPkmnComponent();
        faintedPkmnComponent.setNpcRoleId(roleId);
        faintedPkmnComponent.setNpcUuid(entityId);

        world.execute(() -> {

            Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
            ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(tombstoneModelId);
            Model model = Model.createScaledModel(modelAsset, 0.85f);

            holder.addComponent(TransformComponent.getComponentType(), new TransformComponent((Vector3dc) npcPos,Rotation3f.ZERO));
            holder.addComponent(PersistentModel.getComponentType(), new PersistentModel(model.toReference()));
            holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
            holder.addComponent(BoundingBox.getComponentType(), new BoundingBox(model.getBoundingBox()));
            holder.addComponent(NetworkId.getComponentType(), new NetworkId(store.getExternalData().takeNextNetworkId()));

            Interactions tombstoneInteractions = new Interactions();
            tombstoneInteractions.setInteractionId(InteractionType.Use,"Root_Pkmn_Fainted_Return");
            tombstoneInteractions.setInteractionHint("Press [F] while holding a pokeball to pick up");

            holder.addComponent(PkmnStatsComponent.getComponentType(), pkmnStats);
            holder.addComponent(FaintedPkmnComponent.getComponentType(), faintedPkmnComponent);
            holder.addComponent(Interactions.getComponentType(), tombstoneInteractions); // you need to add interactions here if you want your entity to be interactable
            holder.ensureComponent(UUIDComponent.getComponentType());
            holder.ensureComponent(Interactable.getComponentType()); // if you want your entity to be interactable

            if(ref.isValid()){
                store.addEntity(holder, AddReason.SPAWN);
                store.removeEntity(ref, RemoveReason.REMOVE);
                // commandBuffer.removeEntity(ownerRef, RemoveReason.REMOVE);
            }
            // commandBuffer.tryRemoveEntity(ownerRef, RemoveReason.REMOVE);
        });

    }

    private boolean returnToPlayer(        
        InteractionContext context, 
        Store<EntityStore> store,
        Ref<EntityStore> ref,
        World world,
        String roleId,
        String entityId,
        PkmnStatsComponent pkmnStats,
        PkmnCaptureMetadata pkmnMeta,
        Ref<EntityStore> ownerRef
    ){
        PlayerRef playerRef = store.getComponent(ownerRef, PlayerRef.getComponentType());
        if(playerRef==null) return false;
        // LOGGER.atInfo().log("owners name is: "+playerRef.getUsername());
        InventorySlotItem ballInSlot = findMatchingBall(store,ref,entityId,ownerRef);
        if(ballInSlot==null){
            // LOGGER.atInfo().log("no ball found");
            return false;
        }
        // LOGGER.atInfo().log("Found the right ball");

        Short slot = ballInSlot.slot;
        ItemStack ball = ballInSlot.itemstack;
        ItemContainer inventoryContainer = ballInSlot.inventory.getInventory();
        CapturedNPCMetadata npcMeta = new CapturedNPCMetadata();
            npcMeta.setNpcNameKey(roleId);
            npcMeta.setFullItemIcon(faintedIcon);
            npcMeta.setIconPath(faintedIcon);

        ItemStack capturedBall = ball
            .withMetadata(CapturedNPCMetadata.KEYED_CODEC,npcMeta)
            .withMetadata(PkmnCaptureMetadata.KEYED_CODEC,pkmnMeta)
            .withState("Fainted");

        inventoryContainer.replaceItemStackInSlot(slot, ball, capturedBall);

        return true;
    }




    public record InventorySlotItem(Short slot, ItemStack itemstack, InventoryComponent inventory) {}

    private InventorySlotItem findMatchingBall(        
        Store<EntityStore> store,
        Ref<EntityStore> ref,
        String entityId,
        Ref<EntityStore> ownerRef
    ){
        Hotbar hotbar = store.getComponent(ownerRef,InventoryComponent.Hotbar.getComponentType());
        Storage storage = store.getComponent(ownerRef,InventoryComponent.Storage.getComponentType());
        Backpack backpack = store.getComponent(ownerRef,InventoryComponent.Backpack.getComponentType());

        
        List<InventorySlotItem> matches = new ArrayList<>();
        
        if(hotbar!= null) {
            ItemContainer  hotbarContainer = hotbar.getInventory();
            hotbarContainer.forEach((slot, itemStack) -> {
                if(matches.size()>0) return;
                if(validItems.contains(itemStack.getItemId())){
                    PkmnCaptureMetadata captureMetadata = itemStack.getFromMetadataOrNull("PkmnCapture", PkmnCaptureMetadata.CODEC);
                    if(captureMetadata!=null){
                        // LOGGER.atInfo().log("Possible item: "+itemStack.getItemId()+" with data: "+captureMetadata.toString());
                        // var npcEntityUuid = captureMetadata.getNpcEntityUuid();
                        // var roleId = captureMetadata.getRoleId();
                        // var level = captureMetadata.getLevel();
                        // LOGGER.atInfo().log("EntityID = "+npcEntityUuid);
                        // LOGGER.atInfo().log("needs to be: "+entityId);
                        // LOGGER.atInfo().log("roleId = "+roleId+" ["+String.valueOf(level)+"]");
                        String itemEntityUUID = captureMetadata.getNpcEntityUuid();
                        if (entityId.equals(itemEntityUUID)){
                            // LOGGER.atInfo().log("found matching item");
                            matches.add(new InventorySlotItem(slot,itemStack,hotbar));
                            return;
                        }
                    }
                }
            });
        }
        if(matches.size()>0) return matches.get(0);
        if(storage!= null) {
            ItemContainer  storageContainer = storage.getInventory();
            storageContainer.forEach((slot, itemStack) -> {
                if(matches.size()>0) return;
                if(validItems.contains(itemStack.getItemId())){
                    PkmnCaptureMetadata captureMetadata = itemStack.getFromMetadataOrNull("PkmnCapture", PkmnCaptureMetadata.CODEC);
                    if(captureMetadata!=null){
                        String itemEntityUUID = captureMetadata.getNpcEntityUuid();
                        if (entityId.equals(itemEntityUUID)){
                            // LOGGER.atInfo().log("found matching item");
                            matches.add(new InventorySlotItem(slot,itemStack,storage));
                            return;
                        }
                    }
                }
            });
        }
        if(matches.size()>0) return matches.get(0);
        if(backpack!= null) {
            ItemContainer  backpackContainer = backpack.getInventory();
            backpackContainer.forEach((slot, itemStack) -> {
                if(matches.size()>0) return;
                if(validItems.contains(itemStack.getItemId())){
                    PkmnCaptureMetadata captureMetadata = itemStack.getFromMetadataOrNull("PkmnCapture", PkmnCaptureMetadata.CODEC);
                    if(captureMetadata!=null){
                        String itemEntityUUID = captureMetadata.getNpcEntityUuid();
                        if (entityId.equals(itemEntityUUID)){
                            // LOGGER.atInfo().log("found matching item");
                            matches.add(new InventorySlotItem(slot,itemStack,backpack));
                            return;
                        }
                    }
                }
            });
        }
        if(matches.size()>0) return matches.get(0);
        return null;
    }

    private static void fail(@Nonnull InteractionContext context) {
        context.getState().state = InteractionState.Failed;
    }

    public static final BuilderCodec<SpawnPkmnTombstoneInteraction> CODEC = BuilderCodec.builder(
        SpawnPkmnTombstoneInteraction.class,
        SpawnPkmnTombstoneInteraction::new,
        SimpleInteraction.CODEC
    )
    .build();


    
}
