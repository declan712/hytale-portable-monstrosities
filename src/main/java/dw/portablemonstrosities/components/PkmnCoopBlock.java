package dw.portablemonstrosities.components;

import java.time.Instant;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3d;

// import com.hypixel.hytale.builtin.adventure.farming.FarmingPlugin;
import com.hypixel.hytale.builtin.adventure.farming.component.CoopResidentComponent;
import com.hypixel.hytale.builtin.adventure.farming.config.FarmingCoopAsset;
import com.hypixel.hytale.builtin.adventure.farming.states.CoopBlock;
// import com.hypixel.hytale.builtin.adventure.farming.states.CoopBlock.CoopResident;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.function.consumer.TriConsumer;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.math.vector.Vector3dUtil;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.reference.PersistentRef;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.EmptyItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.metadata.CapturedNPCMetadata;
import com.hypixel.hytale.server.spawning.ISpawnableWithModel;
import com.hypixel.hytale.server.spawning.SpawnTestResult;
import com.hypixel.hytale.server.spawning.SpawningContext;

import dw.portablemonstrosities.PortableMonstrosities;
import dw.portablemonstrosities.util.PkmnStatUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;




// import com.hypixel.hytale.builtin.adventure.farming.FarmingPlugin;
// import com.hypixel.hytale.builtin.adventure.farming.FarmingUtil;
// import com.hypixel.hytale.builtin.adventure.farming.component.CoopResidentComponent;
// import com.hypixel.hytale.builtin.adventure.farming.config.FarmingCoopAsset;
// import com.hypixel.hytale.builtin.tagset.TagSetPlugin;
// import com.hypixel.hytale.builtin.tagset.config.NPCGroup;
// import com.hypixel.hytale.codec.Codec;
// import com.hypixel.hytale.codec.KeyedCodec;
// import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
// import com.hypixel.hytale.component.AddReason;
// import com.hypixel.hytale.component.Component;
// import com.hypixel.hytale.component.ComponentType;
// import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
// import com.hypixel.hytale.component.Store;
// import com.hypixel.hytale.function.consumer.TriConsumer;
// import com.hypixel.hytale.logger.HytaleLogger;
// import com.hypixel.hytale.math.range.IntRange;
// import com.hypixel.hytale.math.util.MathUtil;
// import com.hypixel.hytale.math.vector.Rotation3f;
// import com.hypixel.hytale.math.vector.Vector3dUtil;
// import com.hypixel.hytale.math.vector.Vector3iUtil;
// import com.hypixel.hytale.server.core.asset.type.item.config.ItemDrop;
// import com.hypixel.hytale.server.core.asset.type.item.config.ItemDropList;
// import com.hypixel.hytale.server.core.asset.type.item.config.container.ItemDropContainer;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
// import com.hypixel.hytale.server.core.entity.UUIDComponent;
// import com.hypixel.hytale.server.core.entity.reference.PersistentRef;
// import com.hypixel.hytale.server.core.inventory.ItemStack;
// import com.hypixel.hytale.server.core.inventory.container.EmptyItemContainer;
// import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
// import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
// import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
// import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
// import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
// import com.hypixel.hytale.server.core.universe.world.World;
// import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
// import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
// import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
// import com.hypixel.hytale.server.npc.entities.NPCEntity;
// import com.hypixel.hytale.server.npc.metadata.CapturedNPCMetadata;
import com.hypixel.hytale.server.npc.role.Role;
// import com.hypixel.hytale.server.spawning.ISpawnableWithModel;
// import com.hypixel.hytale.server.spawning.SpawnTestResult;
// import com.hypixel.hytale.server.spawning.SpawningContext;
import it.unimi.dsi.fastutil.Pair;
// import it.unimi.dsi.fastutil.objects.ObjectArrayList;
// import it.unimi.dsi.fastutil.objects.ObjectListIterator;
// import java.time.Duration;
// import java.time.Instant;
// import java.time.LocalDateTime;
// import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
// import java.util.List;
// import java.util.Map;
// import java.util.Objects;
// import java.util.UUID;
// import java.util.concurrent.ThreadLocalRandom;
// import javax.annotation.Nonnull;
// import javax.annotation.Nullable;
// import org.joml.Vector3d;
// import org.joml.Vector3i;


public class PkmnCoopBlock extends CoopBlock {

    @Nonnull
    protected List<PkmnCoopResident> residents = new ObjectArrayList<>();
    
    // public static ComponentType<ChunkStore, CoopBlock> getComponentType() {
    //     return PortableMonstrosities.instance().getPkmnCoopBlockComponentType();
    // }
    public static ComponentType<ChunkStore, PkmnCoopBlock> getPkmnComponentType() {
        return PortableMonstrosities.instance().getPkmnCoopBlockComponentType();
    }

    public PkmnCoopBlock() {
        this.itemContainer = EmptyItemContainer.INSTANCE;
        List<ItemStack> remainder = new ObjectArrayList<>();
        this.itemContainer = ItemContainer.ensureContainerCapacity(
            this.itemContainer, (short)5, SimpleItemContainer::new, remainder);
    }

    public PkmnCoopBlock(
        @Nonnull String farmingCoopId, 
        @Nonnull List<PkmnCoopResident> residents, 
        @Nonnull ItemContainer itemContainer
    ) {
        this.itemContainer = EmptyItemContainer.INSTANCE;
        this.coopAssetId = farmingCoopId;
        this.residents.addAll(residents);
        this.itemContainer = itemContainer.clone();
        List<ItemStack> remainder = new ObjectArrayList<>();
        this.itemContainer = ItemContainer.ensureContainerCapacity(this.itemContainer, (short)5, SimpleItemContainer::new, remainder);
    }

    public boolean tryPutResident(
        @Nonnull CapturedNPCMetadata metadata,
        @Nonnull PkmnCaptureMetadata pkmnMeta,
        @Nonnull WorldTimeResource worldTimeResource
    ) {
        FarmingCoopAsset coopAsset = this.getCoopAsset();
        if (coopAsset == null) {
            return false;
        } else if (this.residents.size() >= coopAsset.getMaxResidents()) {
            return false;
        } else if (!this.getCoopAcceptsNPC(metadata.getNpcNameKey())) {
            return false;
        } else {
            this.residents.add(new PkmnCoopResident(
                metadata, 
                pkmnMeta, 
                (PersistentRef)null, 
                worldTimeResource.getGameTime()
            ));
            return true;
        }
    }

    public Component<ChunkStore> clone() {
        return new PkmnCoopBlock(this.coopAssetId, this.residents, this.itemContainer);
    }

    
    public void ensureSpawnResidentsInWorld(
        @Nonnull World world, 
        @Nonnull Store<EntityStore> store, 
        @Nonnull Vector3d coopLocation, 
        @Nonnull Vector3d spawnOffset
    ) {
        FarmingCoopAsset coopAsset = this.getCoopAsset();
        if (coopAsset != null) {
            float radiansPerSpawn = ((float)Math.PI * 2F) / (float)coopAsset.getMaxResidents();
            Vector3d spawnOffsetIteration = spawnOffset;
            SpawningContext spawningContext = new SpawningContext();

            for(PkmnCoopResident resident : this.residents) {
                CapturedNPCMetadata residentMeta = resident.getMetadata();
                PkmnCaptureMetadata pkmnMeta = resident.getPkmnMetadata();
                String npcNameKey = residentMeta.getNpcNameKey();
                int npcRoleIndex = NPCPlugin.get().getIndex(npcNameKey);
                boolean residentDeployed = resident.getDeployedToWorld();
                PersistentRef residentEntityId = resident.getPersistentRef();
                if (!residentDeployed && residentEntityId == null) {
                Vector3d residentSpawnLocation = (new Vector3d()).set(coopLocation).add(spawnOffsetIteration);
                Builder<Role> roleBuilder = NPCPlugin.get().tryGetCachedValidRole(npcRoleIndex);
                if (roleBuilder != null) {
                    spawningContext.setSpawnable((ISpawnableWithModel)roleBuilder);
                    if (spawningContext.set(world, residentSpawnLocation.x, residentSpawnLocation.y, residentSpawnLocation.z) 
                            && spawningContext.canSpawn() == SpawnTestResult.TEST_OK) {
                        Pair<Ref<EntityStore>, NPCEntity> npcPair = NPCPlugin.get()
                        .spawnEntity(
                            store, npcRoleIndex, spawningContext.newPosition(), 
                            Rotation3f.IDENTITY, (Model)null, (TriConsumer)null
                        );
                        if (npcPair == null) {
                            resident.setPersistentRef((PersistentRef)null);
                            resident.setDeployedToWorld(false);
                        } else {
                            Ref<EntityStore> npcRef = (Ref)npcPair.first();
                            NPCEntity npcComponent = (NPCEntity)npcPair.second();
                            npcComponent.getLeashPoint().set(coopLocation);
                            if (npcRef != null && npcRef.isValid()) {
                                PkmnStatsComponent pkmnStats = PkmnStatUtils.fromMetadata(pkmnMeta);
                                PkmnStatUtils.apply(store,npcRef,pkmnStats);

                                UUIDComponent uuidComponent = (UUIDComponent)store.getComponent(npcRef, UUIDComponent.getComponentType());
                                if (uuidComponent == null) {
                                    resident.setPersistentRef((PersistentRef)null);
                                    resident.setDeployedToWorld(false);
                                } else {
                                    CoopResidentComponent coopResidentComponent = new CoopResidentComponent();
                                    coopResidentComponent.setCoopLocation(Vector3dUtil.toVector3i(coopLocation));
                                    store.addComponent(npcRef, CoopResidentComponent.getComponentType(), coopResidentComponent);
                                    PersistentRef persistentRef = new PersistentRef();
                                    persistentRef.setEntity(npcRef, uuidComponent.getUuid());
                                    resident.setPersistentRef(persistentRef);
                                    resident.setDeployedToWorld(true);
                                    spawnOffsetIteration = spawnOffsetIteration.rotateY((double)radiansPerSpawn);
                                }
                            } else {
                            resident.setPersistentRef((PersistentRef)null);
                            resident.setDeployedToWorld(false);
                            }
                        }
                    }
                }
                }
            }

        }
    }

        @Nonnull
        public static final BuilderCodec<PkmnCoopBlock> CODEC = BuilderCodec
            .builder(PkmnCoopBlock.class, PkmnCoopBlock::new)
            .append(new KeyedCodec<>("FarmingCoopId", Codec.STRING, true), 
                (coop, s) -> coop.coopAssetId = s, 
                (coop) -> coop.coopAssetId)
            .add()
            .append(new KeyedCodec<>("Residents", 
                    new ArrayCodec<>(PkmnCoopBlock.PkmnCoopResident.CODEC, 
                    (x$0) -> new PkmnCoopResident[x$0])
                ), 
                (coop, residents) -> coop.residents = new ArrayList<>(Arrays.asList(residents)), 
                (coop) -> (PkmnCoopResident[])coop.residents.toArray((x$0) -> new PkmnCoopResident[x$0]))
            .add()
            .append(new KeyedCodec<>("Storage", ItemContainer.CODEC), 
                (coop, storage) -> coop.itemContainer = storage, 
                (coop) -> coop.itemContainer)
            .add()
            .build();
        
        
        // BuilderCodec
        //     .builder(PkmnCoopResident.class, PkmnCoopResident::new)
        //     .append(new KeyedCodec<>("Metadata", CapturedNPCMetadata.CODEC), 
        //         (coop, meta) -> coop.metadata = meta, 
        //         (coop) -> coop.metadata)
        //     .add()
        //     .append(new KeyedCodec<>("PkmnMeta", PkmnCaptureMetadata.CODEC), 
        //         (coop, meta) -> coop.pkmnMeta = meta, 
        //         (coop) -> coop.pkmnMeta)
        //     .add()
        //     .append(new KeyedCodec<>("PersistentRef", PersistentRef.CODEC), 
        //         (coop, persistentRef) -> coop.persistentRef = persistentRef, 
        //         (coop) -> coop.persistentRef)
        //     .add()
        //     .append(new KeyedCodec<>("DeployedToWorld", Codec.BOOLEAN), 
        //         (coop, deployedToWorld) -> coop.deployedToWorld = deployedToWorld, 
        //         (coop) -> coop.deployedToWorld)
        //     .add()
        //     .append(new KeyedCodec<>("LastHarvested", Codec.INSTANT), 
        //         (coop, instant) -> coop.lastProduced = instant, 
        //         (coop) -> coop.lastProduced)
        //     .add()
        //     .build();






    public static class PkmnCoopResident extends CoopResident {
        protected PkmnCaptureMetadata pkmnMeta;

        PkmnCoopResident(){}

        public PkmnCoopResident(
            CapturedNPCMetadata metadata, 
            PkmnCaptureMetadata pkmnMeta, 
            @Nullable PersistentRef persistentRef, 
            @Nonnull Instant lastProduced
        ) {
            this.metadata = metadata;
            this.pkmnMeta = pkmnMeta;
            this.persistentRef = persistentRef;
            this.lastProduced = lastProduced;
        }

        public PkmnCaptureMetadata getPkmnMetadata() {
            return this.pkmnMeta;
        }

        @Nonnull
        public static final BuilderCodec<PkmnCoopResident> CODEC = BuilderCodec
            .builder(PkmnCoopResident.class, PkmnCoopResident::new)
            .append(new KeyedCodec<>("Metadata", CapturedNPCMetadata.CODEC), 
                (coop, meta) -> coop.metadata = meta, 
                (coop) -> coop.metadata)
            .add()
            .append(new KeyedCodec<>("PkmnMeta", PkmnCaptureMetadata.CODEC), 
                (coop, meta) -> coop.pkmnMeta = meta, 
                (coop) -> coop.pkmnMeta)
            .add()
            .append(new KeyedCodec<>("PersistentRef", PersistentRef.CODEC), 
                (coop, persistentRef) -> coop.persistentRef = persistentRef, 
                (coop) -> coop.persistentRef)
            .add()
            .append(new KeyedCodec<>("DeployedToWorld", Codec.BOOLEAN), 
                (coop, deployedToWorld) -> coop.deployedToWorld = deployedToWorld, 
                (coop) -> coop.deployedToWorld)
            .add()
            .append(new KeyedCodec<>("LastHarvested", Codec.INSTANT), 
                (coop, instant) -> coop.lastProduced = instant, 
                (coop) -> coop.lastProduced)
            .add()
            .build();
        

    }
}
