package dev.hytalemodding.interactions;

import java.util.UUID;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.BlockEntity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
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
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
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


public class SpawnPkmnTombstoneInteration extends SimpleInstantInteraction {
    public  static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private String tombstoneItemId = "Pkmn_Tombstone";
    private String tombstoneModelId = "Fainted_Pkmn";

    @Override
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

        Store<EntityStore> store = commandBuffer.getExternalData().getStore();
        if (store == null) { fail(context); return; }

        NPCEntity npcEntity = commandBuffer.getComponent(ownerRef, NPCEntity.getComponentType());
        if (npcEntity == null) { fail(context); return; }

        String roleId = npcEntity.getRoleName();

        DeathComponent deathComponent = store.getComponent(ownerRef, DeathComponent.getComponentType());
        if (deathComponent == null) { 
            // fail(context); return; 
        }

        // item drop pos
        TransformComponent transform = store.getComponent(
            ownerRef, EntityModule.get().getTransformComponentType());
        if (transform == null) { fail(context); return; }
        Vector3d npcPos = transform.getPosition();

        Vector3i pos = new Vector3i(
            (int) npcPos.x,
            (int) npcPos.y,
            (int) npcPos.z
        );

        PkmnStatsComponent pkmnStats = store.getComponent(
            ownerRef, PkmnStatsComponent.getComponentType());

        UUIDComponent uuidComponent = store.getComponent(ownerRef, UUIDComponent.getComponentType());
        String entityId = uuidComponent.getUuid().toString();

        PkmnCaptureMetadata pkmnMeta = PkmnStatUtils.captureMetadata(commandBuffer,ownerRef);
            pkmnMeta.setNpcStatus("Fainted");
            pkmnMeta.setNpcEntityUuid(entityId);

        CapturedNPCMetadata npcMeta = 
            PkmnStatUtils.getNpcMetadata(commandBuffer,ownerRef,null,null);

        ItemStack tombstone = new ItemStack(tombstoneItemId, 1)
                .withMetadata(CapturedNPCMetadata.KEYED_CODEC, npcMeta)
                .withMetadata(PkmnCaptureMetadata.KEYED_CODEC, pkmnMeta);


        FaintedPkmnComponent faintedPkmnComponent = new FaintedPkmnComponent();
        faintedPkmnComponent.setNpcRoleId(roleId);
        faintedPkmnComponent.setNpcUuid(entityId);
        // faintedPkmnComponent.setPkmnStats(pkmnStats);


        long       chunkIndex = ChunkUtil.indexChunkFromBlock(pos.x, pos.z);
        WorldChunk worldChunk = world.getChunk(chunkIndex);
        if (worldChunk == null) { 
            fail(context); 
            return; 
        }
        


        world.execute(() -> {

            Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
            ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(tombstoneModelId);
            Model model = Model.createScaledModel(modelAsset, 0.85f);

            holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(npcPos, new Vector3f(0, 0, 0)));
            holder.addComponent(PersistentModel.getComponentType(), new PersistentModel(model.toReference()));
            holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
            holder.addComponent(BoundingBox.getComponentType(), new BoundingBox(model.getBoundingBox()));
            holder.addComponent(NetworkId.getComponentType(), new NetworkId(store.getExternalData().takeNextNetworkId()));

            Interactions tombstoneInteractions = new Interactions();
            tombstoneInteractions.setInteractionId(InteractionType.Use,"Root_Pkmn_Fainted_Return");
            tombstoneInteractions.setInteractionHint("Press [{key}] while holding a pokeball to pick up");

            holder.addComponent(PkmnStatsComponent.getComponentType(), pkmnStats);
            holder.addComponent(FaintedPkmnComponent.getComponentType(), faintedPkmnComponent);
            holder.addComponent(Interactions.getComponentType(), tombstoneInteractions); // you need to add interactions here if you want your entity to be interactable
            holder.ensureComponent(UUIDComponent.getComponentType());
            holder.ensureComponent(Interactable.getComponentType()); // if you want your entity to be interactable

            if(ownerRef.isValid()){
                store.addEntity(holder, AddReason.SPAWN);
                store.removeEntity(ownerRef, RemoveReason.REMOVE);
                // commandBuffer.removeEntity(ownerRef, RemoveReason.REMOVE);
            }
            // commandBuffer.tryRemoveEntity(ownerRef, RemoveReason.REMOVE);
        });




        context.getState().state = InteractionState.Finished;
    }

    private static void fail(@Nonnull InteractionContext context) {
        context.getState().state = InteractionState.Failed;
    }

    public static final BuilderCodec<SpawnPkmnTombstoneInteration> CODEC = BuilderCodec.builder(
        SpawnPkmnTombstoneInteration.class,
        SpawnPkmnTombstoneInteration::new,
        SimpleInstantInteraction.CODEC
    )
    .build();
    
}
