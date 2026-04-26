package dev.hytalemodding.interactions;

import com.hypixel.hytale.builtin.adventure.farming.states.CoopBlock;
import com.hypixel.hytale.builtin.tagset.TagSetPlugin;
import com.hypixel.hytale.builtin.tagset.config.NPCGroup;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.function.consumer.TriConsumer;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockFace;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.metadata.CapturedNPCMetadata;
import dev.hytalemodding.components.PkmnCaptureMetadata;
import dev.hytalemodding.components.PkmnStatsComponent;
import dev.hytalemodding.util.PkmnBaseStatList;
import dev.hytalemodding.util.PkmnStatUtils;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bson.BsonDocument;

/**
 * UseCaptureOrbInteraction<br>
 *<br>
 * Extended version of UseCaptureCrate Interaction that:<br>
 *  - Can be triggered either by a direct entity interaction or by a projectile hit.<br>
 *  - Preserves more of the NPC stats<br>
 *  - Includes additional stats added by this mod<br>
 * <br>
 *<br>
 *  Included metadata:<br>
 *      - "CapturedEntity"   → CapturedNPCMetadata  (vanilla)<br>
 *      - "PkmnCapture"      → PkmnCaptureMetadata<br>
 */
public class UseCaptureOrbInteraction extends SimpleBlockInteraction {

    @Nonnull
    public static final BuilderCodec<UseCaptureOrbInteraction> CODEC;

    protected String[] acceptedNpcGroupIds;
    protected int[]    acceptedNpcGroupIndexes;
    protected String   fullIcon;
    protected String captureItemId;

    // TODO: use captureItemId/species combination to choose icon

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public UseCaptureOrbInteraction() {}

    //                              Entry point
    // ██████████████████████████████████████████████████████████████████████████
    @Override
    protected void tick0(
        boolean firstRun,
        float time,
        @Nonnull InteractionType type,
        @Nonnull InteractionContext context,
        @Nonnull CooldownHandler cooldownHandler
    ) {
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        assert commandBuffer != null;


        // -- Projectile-hit ----------
        if(type == InteractionType.ProjectileHit){
            World world = commandBuffer.getExternalData().getWorld();
            Ref<EntityStore> ownerRef = context.getOwningEntity();
            if(ownerRef==null){
                LOGGER.atInfo().log("ownerRef null");
                fail(context);
                super.tick0(firstRun, time, type, context, cooldownHandler);
                return;
            }
            Entity owner = EntityUtils.getEntity(ownerRef, commandBuffer);
            if (!(owner instanceof LivingEntity)) {
                fail(context);
                super.tick0(firstRun, time, type, context, cooldownHandler);
                return;
            }

            Ref<EntityStore> targetRef = context.getTargetEntity();
            if (targetRef == null) {
                fail(context);
                super.tick0(firstRun, time, type, context, cooldownHandler);
                return;
            }

            if (captureItemId == null) {
                fail(context);
                super.tick0(firstRun, time, type, context, cooldownHandler);
                return;
            }
            ItemStack captureItem = new ItemStack(captureItemId, 1);

            Store<EntityStore> store = commandBuffer.getExternalData().getStore();
            if (store == null) return;
            TransformComponent targetTransform = store.getComponent(targetRef, EntityModule.get().getTransformComponentType());
            Vector3d targetPosition = targetTransform.getPosition();

            ItemStack capturedBall = tryCaptureIntoItem(commandBuffer, captureItem, targetRef, ownerRef);
            if (capturedBall == null) {
                fail(context);
                super.tick0(firstRun, time, type, context, cooldownHandler);
                return;
            }

            if (targetPosition != null) {
                dropAsItem(world, commandBuffer, targetPosition, capturedBall);
            }
            return;
        }

        ItemStack item = context.getHeldItem();
        if (item == null) {
            fail(context);
            super.tick0(firstRun, time, type, context, cooldownHandler);
            return;
        }

        // -- Direct interact ----------
        Ref<EntityStore> ref = context.getEntity();
        Entity caster = EntityUtils.getEntity(ref, commandBuffer);
        if (!(caster instanceof LivingEntity)) {
            LOGGER.atInfo().log("caster not LivingEntity");
            fail(context);
            super.tick0(firstRun, time, type, context, cooldownHandler);
            return;
        }

        LivingEntity livingEntity  = (LivingEntity) caster;
        Inventory    inventory     = livingEntity.getInventory();
        byte         hotbarSlot    = inventory.getActiveHotbarSlot();
        ItemStack    inHandItem    = inventory.getActiveHotbarItem();
        if (inHandItem == null) {
            LOGGER.atInfo().log("inHandItem NULL");
            fail(context);
            super.tick0(firstRun, time, type, context, cooldownHandler);
            return;
        }

        CapturedNPCMetadata existingMeta = (CapturedNPCMetadata)
            item.getFromMetadataOrNull("CapturedEntity", CapturedNPCMetadata.CODEC);
        if (existingMeta != null) {
            LOGGER.atInfo().log("CapturedNPCMetadata != NULL");

            if (context.getTargetBlock() == null) { 
                context.getState().state = InteractionState.Finished;
                return;
            }
            super.tick0(firstRun, time, type, context, cooldownHandler);
            return;
        }

        Ref<EntityStore> targetRef = context.getTargetEntity();
        if (targetRef == null) {
            LOGGER.atInfo().log("targetRef NULL");
            fail(context);
            super.tick0(firstRun, time, type, context, cooldownHandler);
            return;
        }

        ItemStack singleBall = inHandItem.withQuantity(1);
        ItemStack capturedBall = tryCaptureIntoItem(commandBuffer, singleBall, targetRef, ref);
        if (capturedBall == null) {
            LOGGER.atInfo().log("capturedBall NULL");
            fail(context);
            super.tick0(firstRun, time, type, context, cooldownHandler);
            return;
        }
        ItemStack remainder = inHandItem.withQuantity(inHandItem.getQuantity() - 1);
        if(remainder == null){
            inventory.getHotbar().replaceItemStackInSlot((short) hotbarSlot, inHandItem, capturedBall);
        } else if (remainder.getQuantity() > 0) {
            inventory.getHotbar().replaceItemStackInSlot((short) hotbarSlot, inHandItem, remainder);
            inventory.getStorage().addItemStack(capturedBall);
        } else {
            inventory.getHotbar().replaceItemStackInSlot((short) hotbarSlot, inHandItem, capturedBall);
        }
        context.getState().state = InteractionState.Finished;
        return;
    }

    //                              Block-interaction (Release)
    // ██████████████████████████████████████████████████████████████████████████

    @Override
    protected void interactWithBlock(
        @Nonnull World world,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull InteractionType type,
        @Nonnull InteractionContext context,
        @Nullable ItemStack itemInHand,
        @Nonnull Vector3i targetBlock,
        @Nonnull CooldownHandler cooldownHandler
    ) {
        ItemStack item = context.getHeldItem();
        if (item == null) { 
            LOGGER.atInfo().log("ItemStack NULL");
            fail(context); 
            return; 
        }

        Ref<EntityStore> ref    = context.getEntity();
        Entity           caster = EntityUtils.getEntity(ref, commandBuffer);
        if (!(caster instanceof LivingEntity)) { 
            LOGGER.atInfo().log("Caster not LivingEntity");
            fail(context); 
            return; 
        }

        LivingEntity livingEntity = (LivingEntity) caster;
        Inventory    inventory    = livingEntity.getInventory();
        byte         hotbarSlot   = inventory.getActiveHotbarSlot();

        CapturedNPCMetadata existingMeta = (CapturedNPCMetadata)
            item.getFromMetadataOrNull("CapturedEntity", CapturedNPCMetadata.CODEC);
        if (existingMeta == null) { 
            LOGGER.atInfo().log("CapturedNPCMetadata NULL");
            fail(context); 
            return; 
        }

        BlockPosition pos = context.getTargetBlock();
        if (pos == null) { 
            LOGGER.atInfo().log("BlockPosition NULL");
            fail(context); 
            return;
        }

        long       chunkIndex = ChunkUtil.indexChunkFromBlock(pos.x, pos.z);
        WorldChunk worldChunk = world.getChunk(chunkIndex);
        if (worldChunk == null) { 
            LOGGER.atInfo().log("WorldChunk NULL");
            fail(context); 
            return; 
        }

        Ref<ChunkStore> blockRef = worldChunk.getBlockComponentEntity(pos.x, pos.y, pos.z);
        if (blockRef == null || !blockRef.isValid()) {
            blockRef = BlockModule.ensureBlockEntity(worldChunk, pos.x, pos.y, pos.z);
        }

        // CapturedNPCMetadata existingMeta = (CapturedNPCMetadata)
        //             item.getFromMetadataOrNull("CapturedEntity", CapturedNPCMetadata.CODEC);

        // ItemStack withNpc      = ball.withMetadata(CapturedNPCMetadata.KEYED_CODEC,  npcMeta);
        // ItemStack capturedBall = withNpc.withMetadata(PkmnCaptureMetadata.KEYED_CODEC, captureMeta);

        PkmnCaptureMetadata existingCaptureMeta = item.getFromMetadataOrNull("PkmnCapture", PkmnCaptureMetadata.CODEC);
            LOGGER.atInfo().log("NPC IS not?");
        if(existingCaptureMeta == null ){
            fail(context);
            return;
        }
        String npcStatus = existingCaptureMeta.getNpcStatus();
        if (npcStatus==null) npcStatus="Healthy";
        if(npcStatus == "Fainted"){
            LOGGER.atInfo().log("NPC IS DEAD?");
            fail(context);
            return;
        }
        if(npcStatus == "Active"){
            LOGGER.atInfo().log("NPC not currently in ball?");
            fail(context);
            return;
        }
        LOGGER.atInfo().log("NPC state: "+npcStatus);
        
        existingCaptureMeta.setNpcStatus("Active");
        UUIDComponent ownerUuidComponent = commandBuffer.getComponent(ref, UUIDComponent.getComponentType());
        if (ownerUuidComponent != null) existingCaptureMeta.setOwnerUuid(ownerUuidComponent.getUuid().toString());
        

        ItemStack blankBall = item.withMetadata((BsonDocument) null);
        ItemStack emptyBall = blankBall
                .withMetadata(PkmnCaptureMetadata.KEYED_CODEC, existingCaptureMeta)
                .withState("Active");

        if (blockRef != null && blockRef.isValid()) {
            Store<ChunkStore> chunkStore         = world.getChunkStore().getStore();
            CoopBlock         coopBlock = (CoopBlock) chunkStore.getComponent(blockRef, CoopBlock.getComponentType());
            if (coopBlock != null) {
                WorldTimeResource worldTime = (WorldTimeResource)
                    commandBuffer.getResource(WorldTimeResource.getResourceType());
                if (coopBlock.tryPutResident(existingMeta, worldTime)) {
                    world.execute(() -> coopBlock.ensureSpawnResidentsInWorld(
                        world,
                        world.getEntityStore().getStore(),
                        new Vector3d(pos.x, pos.y, pos.z),
                        new Vector3d().assign(Vector3d.FORWARD)
                    ));
                    inventory.getHotbar().replaceItemStackInSlot((short) hotbarSlot, item, emptyBall);
                    context.getState().state = InteractionState.Finished;
                } else {
                    LOGGER.atInfo().log("Unable to add new resident to coop");
                    fail(context);
                }
                return;
            }
        }
        
        spawnCapturedCreature(
            world, 
            commandBuffer, 
            context, 
            pos, 
            existingMeta, 
            item.withMetadata(PkmnCaptureMetadata.KEYED_CODEC, existingCaptureMeta),
            inventory,
            hotbarSlot, 
            item, 
            emptyBall
        );
        // inventory.getHotbar().replaceItemStackInSlot((short) hotbarSlot, item, emptyBall);
    }

    @Override
    protected void simulateInteractWithBlock(
        @Nonnull InteractionType type,
        @Nonnull InteractionContext context,
        @Nullable ItemStack itemInHand,
        @Nonnull World world,
        @Nonnull Vector3i targetBlock
    ) {
        LOGGER.atInfo().log("simulateInteractWithBlock?");
    }


    //                              Projectile-hit
    // ██████████████████████████████████████████████████████████████████████████

    public boolean captureFromProjectileHit(
        @Nonnull World world,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> throwerRef,
        @Nonnull Ref<EntityStore> targetRef,
        @Nonnull ItemStack thrownItem
    ) {
        // Capture item must be empty
        CapturedNPCMetadata existingMeta = (CapturedNPCMetadata)
            thrownItem.getFromMetadataOrNull("CapturedEntity", CapturedNPCMetadata.CODEC);
        if (existingMeta != null){ 
            return false;
        }

        Entity thrower = EntityUtils.getEntity(throwerRef, commandBuffer);
        if (!(thrower instanceof LivingEntity)) return false;

        LivingEntity livingEntity = (LivingEntity) thrower;
        Inventory    inventory    = livingEntity.getInventory();
        ItemStack    inHandItem   = inventory.getActiveHotbarItem();
        if (inHandItem == null) return false;

        ItemStack singleBall = inHandItem.withQuantity(1);
        ItemStack capturedBall = tryCaptureIntoItem(commandBuffer, singleBall, targetRef, throwerRef);

        if (capturedBall == null) return false;
        inventory.getStorage().addItemStack(capturedBall);
        return true;
    }

    //                              Capture
    // ██████████████████████████████████████████████████████████████████████████

    /**
     * Validates the target and returns ItemStack with both <br>
     *  CapturedNPCMetadata and PkmnCaptureMetadata.<br>
     * Target entity is removed on success.<br>
     * Returns null if capture should not proceed.<br>
     *<br>
     */
    @Nullable
    private ItemStack tryCaptureIntoItem(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull ItemStack sourceItem,
        @Nonnull Ref<EntityStore> targetRef,
        @Nonnull Ref<EntityStore> catcherRef
    ) {
        LOGGER.atInfo().log("tryCaptureIntoItem");
        Store<EntityStore> store = commandBuffer.getExternalData().getStore();
        if (store == null) return null;

        NPCEntity npcComponent = (NPCEntity)
            commandBuffer.getComponent(targetRef, NPCEntity.getComponentType());
        if (npcComponent == null) return null;

        DeathComponent deathComponent = (DeathComponent)
            commandBuffer.getComponent(targetRef, DeathComponent.getComponentType());
        if (deathComponent != null) return null;

        // Filter NPC groups
        // TODO: remove?
        TagSetPlugin.TagSetLookup tagSetPlugin = TagSetPlugin.get(NPCGroup.class);
        boolean tagFound = false;
        for (int group : this.acceptedNpcGroupIndexes) {
            if (tagSetPlugin.tagInSet(group, npcComponent.getRoleIndex())) {
                tagFound = true;
                break;
            }
        }
        if (!tagFound) return null;


        // PkmnStatsComponent
        // TODO: use method from utils instead
        PkmnStatsComponent pkmnStats = (PkmnStatsComponent)
            commandBuffer.getComponent(targetRef, PkmnStatsComponent.getComponentType());

        if (pkmnStats != null) {
            String owner  = pkmnStats.getOwnerUuid();
            Player player = commandBuffer.getComponent(catcherRef, Player.getComponentType());
            if (PkmnStatUtils.hasOtherOwner(owner, player)) {
                LOGGER.atInfo().log("Creature is owned by someone else");
                return null;
            }
        }

        String roleId = NPCPlugin.get().getName(npcComponent.getRoleIndex());
        int[] baseStats = PkmnBaseStatList.fromMap(PkmnStatUtils.toDisplayName(roleId));

        if(pkmnStats == null) {
            pkmnStats = new PkmnStatsComponent();
            String species = PkmnStatUtils.toDisplayName(roleId);
            pkmnStats.setBaseStats(PkmnBaseStatList.fromMap(species));
        }

        PkmnCaptureMetadata captureMetadata = 
            PkmnStatUtils.captureMetadata(commandBuffer, targetRef);

        CapturedNPCMetadata npcMeta = 
            PkmnStatUtils.getNpcMetadata(commandBuffer,targetRef,sourceItem,this.fullIcon);



        String npcStatus = captureMetadata.getNpcStatus();
        if (npcStatus==null) npcStatus="Healthy";

        if(npcStatus == "Fainted"){
            LOGGER.atInfo().log("NPC IS DEAD?");;
        }
        if(npcStatus == "Active"){
            LOGGER.atInfo().log("NPC not currently in ball?");
            captureMetadata.setNpcStatus("Healthy");
        }
        LOGGER.atInfo().log("NPC state: "+npcStatus);

        // String ballEntityId = existingCaptureMeta.getballEntityUuid();
        // if(ballEntityId==null || ballEntityId.isBlank()){
        //     String randomId = UUIDGenerator.randomUUID().toString();
        //     // existingCaptureMeta.setnpcEntityUuid(randomId);
        //     existingCaptureMeta.setballEntityUuid(randomId);
        // }


        // Write metadata
        ItemStack withNpc = sourceItem.withMetadata(CapturedNPCMetadata.KEYED_CODEC, npcMeta);
        ItemStack withAll = withNpc
                .withMetadata(PkmnCaptureMetadata.KEYED_CODEC, captureMetadata)
                .withState("Full");
        // Remove NPC from world
        commandBuffer.removeEntity(targetRef, RemoveReason.REMOVE);
        return withAll;
    }

    //                              Drop Item
    // ██████████████████████████████████████████████████████████████████████████
    //  - Generate item drop with NPC metadata (used after projectile capture)

    private static void dropAsItem(
        @Nonnull World world,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Vector3d impactPos,
        @Nonnull ItemStack capturedBall
    ) {
        Store<EntityStore> store = commandBuffer.getExternalData().getStore();
        if (store == null) return;
        List<ItemStack> itemsToDrop = new ArrayList<>();
        itemsToDrop.add(capturedBall);
        Vector3f rotation = new Vector3f();
        Holder<EntityStore>[] drops = ItemComponent.generateItemDrops(store,itemsToDrop,impactPos,rotation);
        commandBuffer.addEntities(drops, AddReason.SPAWN);
        return;
    }


    //                              Spawning
    // ██████████████████████████████████████████████████████████████████████████
    // - restores captured stats on release

    /**
     * Spawns the captured NPC at the targeted block face, restoring its
     * vanilla stats and PkmnStatsComponent from the stored metadata.
     */
    private static void spawnCapturedCreature(
        @Nonnull World world,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull InteractionContext context,
        @Nonnull BlockPosition pos,
        @Nonnull CapturedNPCMetadata existingMeta,
        @Nonnull ItemStack item,
        @Nonnull Inventory  inventory,
        @Nonnull byte  hotbarSlot, 
        @Nonnull ItemStack itemToReplace, 
        @Nonnull ItemStack replacementItem
    ) {

        LOGGER.atInfo().log("spawnCapturedCreature");
        Vector3d spawnPos = new Vector3d(pos.x + 0.5, pos.y, pos.z + 0.5);
        if (context.getClientState() != null) {
            BlockFace face = BlockFace.fromProtocolFace(context.getClientState().blockFace);
            if (face != null) spawnPos.add(face.getDirection());
        }

        String roleId    = existingMeta.getNpcNameKey();
        int    roleIndex = NPCPlugin.get().getIndex(roleId);

        PkmnCaptureMetadata captureMetadata = (PkmnCaptureMetadata)
            item.getFromMetadataOrNull("PkmnCapture", PkmnCaptureMetadata.CODEC);

        commandBuffer.run(store -> {
            var pair = NPCPlugin.get().spawnEntity(
                store, roleIndex, spawnPos, Vector3f.ZERO, 
                (Model)  null,
                (TriConsumer<NPCEntity, Ref<EntityStore>, Store<EntityStore>>) null
            );
            if(pair==null) return;
            Ref<EntityStore> newEntityRef = pair.first();
            if (newEntityRef == null || !newEntityRef.isValid() || captureMetadata == null) return;

            PkmnStatsComponent pkmnStats = PkmnStatUtils.fromMetadata(captureMetadata);

            int[] ivs = captureMetadata.getIvs();
            boolean ivsAreZero = true;
            for (int iv : ivs) { if (iv != 0) { ivsAreZero = false; break; } }
            if (ivsAreZero) pkmnStats.setIvs(pkmnStats.randomIVs());
            else pkmnStats.setIvs(ivs);

            PkmnStatUtils.apply(store, commandBuffer, newEntityRef, pkmnStats);

            float currentHealth = captureMetadata.getCurrentHp();
            PkmnStatUtils.setCurrentHp(commandBuffer,newEntityRef,currentHealth);


            EntityScaleComponent scaleComponent  = store.getComponent(
                newEntityRef, EntityModule.get().getEntityScaleComponentType());
            if(scaleComponent != null) {
                scaleComponent.setScale(captureMetadata.getModelScale());
                commandBuffer.putComponent(
                    newEntityRef, EntityModule.get().getEntityScaleComponentType(), scaleComponent);
            }
            

            commandBuffer.putComponent(newEntityRef,PkmnStatsComponent.getComponentType(),pkmnStats);

            PkmnStatUtils.setPkmnNameplate(commandBuffer, newEntityRef, roleId, pkmnStats);

            ItemStack modifiedBall = PkmnStatUtils.linkNpcWithBall(commandBuffer, newEntityRef,replacementItem);

            inventory.getHotbar().replaceItemStackInSlot((short) hotbarSlot, itemToReplace, modifiedBall);

            // String nameplateText = PkmnStatUtils.buildNamplateString(roleId, pkmnStats, null);
            // commandBuffer.putComponent(newEntityRef,Nameplate.getComponentType(),new Nameplate(nameplateText));
        });
    }


    //                              Utilities
    // ██████████████████████████████████████████████████████████████████████████

    private static void fail(@Nonnull InteractionContext context) {
        context.getState().state = InteractionState.Failed;
    }

    //                               CODEC
    // ██████████████████████████████████████████████████████████████████████████

    static {
        CODEC = BuilderCodec.builder(
            UseCaptureOrbInteraction.class, 
            UseCaptureOrbInteraction::new, 
            SimpleInteraction.CODEC
        )
            .appendInherited(
                new KeyedCodec<>("AcceptedNpcGroups", NPCGroup.CHILD_ASSET_CODEC_ARRAY),
                (o, v) -> o.acceptedNpcGroupIds = v,
                (o)    -> o.acceptedNpcGroupIds,
                (o, p) -> o.acceptedNpcGroupIds = p.acceptedNpcGroupIds
            )
            .addValidator(NPCGroup.VALIDATOR_CACHE.getArrayValidator())
            .add()
            .appendInherited(
                new KeyedCodec<>("FullIcon", Codec.STRING),
                (o, v) -> o.fullIcon = v,
                (o)    -> o.fullIcon,
                (o, p) -> o.fullIcon = p.fullIcon
            )
            .add()
            .appendInherited(
                new KeyedCodec<>("CaptureItemId", Codec.STRING),
                (o, v) -> o.captureItemId = v,
                (o)    -> o.captureItemId,
                (o, p) -> o.captureItemId = p.captureItemId
            )
            .add()
            .afterDecode(captureData -> {
                if (captureData.acceptedNpcGroupIds != null) {
                    captureData.acceptedNpcGroupIndexes = new int[captureData.acceptedNpcGroupIds.length];
                    for (int i = 0; i < captureData.acceptedNpcGroupIds.length; i++) {
                        int assetIdx = NPCGroup.getAssetMap().getIndex(captureData.acceptedNpcGroupIds[i]);
                        captureData.acceptedNpcGroupIndexes[i] = assetIdx;
                    }
                }
            })
            .build();
    }
}
