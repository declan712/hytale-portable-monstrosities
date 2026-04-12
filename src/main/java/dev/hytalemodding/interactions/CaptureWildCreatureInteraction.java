package dev.hytalemodding.interactions;

import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.metadata.CapturedNPCMetadata;

import dev.hytalemodding.components.PkmnCaptureMetadata;
import dev.hytalemodding.components.PkmnStatsComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * CaptureWildCreatureInteraction<br>
 *<br>
 * Capture wild creature into item, converting it to the _Tamed variant.<br>
 * Assigns owner <br>
 *<br>
 * CODEC fields:<br>
 *   CaptureItemId  String  Item ID of the ball to create (default "Pokeball").<br>
 *   FullIcon       String  Fallback full-ball icon path.<br>
 *<br>
 *  TODO: combine naming/icon logic of this+UseCaptureOrbInteraction<br>
 */
public class CaptureWildCreatureInteraction extends SimpleInstantInteraction {

    protected String captureItemId = "Pokeball";
    protected String fullIcon      = "Icons/Items/Pokeball_Full.png";

    private static final String SPECIES_ICON_PREFIX = "Icons/Items/Pokeball/";
    private static final String SPECIES_ICON_SUFFIX = ".png";

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public CaptureWildCreatureInteraction() {}

    public static final BuilderCodec<CaptureWildCreatureInteraction> CODEC = BuilderCodec.builder(
        CaptureWildCreatureInteraction.class,
        CaptureWildCreatureInteraction::new,
        SimpleInstantInteraction.CODEC
    )
    .appendInherited(
        new KeyedCodec<>("CaptureItemId", Codec.STRING),
        (o, v) -> o.captureItemId = v,
        o      -> o.captureItemId,
        (o, p) -> o.captureItemId = p.captureItemId
    ).add()
    .appendInherited(
        new KeyedCodec<>("FullIcon", Codec.STRING),
        (o, v) -> o.fullIcon = v,
        o      -> o.fullIcon,
        (o, p) -> o.fullIcon = p.fullIcon
    ).add()
    .build();

    @Override
    protected void firstRun(
        @Nonnull InteractionType type,
        @Nonnull InteractionContext context,
        @Nonnull CooldownHandler cooldownHandler
    ) {
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        if (commandBuffer == null) { fail(context); return; }

        Ref<EntityStore> targetRef  = context.getTargetEntity();
        Ref<EntityStore> throwerRef = context.getOwningEntity();
        if (targetRef == null) { fail(context); return; }

        Store<EntityStore> store = commandBuffer.getExternalData().getStore();
        World world = commandBuffer.getExternalData().getWorld();
        if (store == null || world == null) { fail(context); return; }

        // Get tamed role
        NPCEntity npcEntity = store.getComponent(targetRef, NPCEntity.getComponentType());
        if (npcEntity == null) { fail(context); return; }

        String wildRoleId = NPCPlugin.get().getName(npcEntity.getRoleIndex());
        String tameRoleId = resolveTameRole(wildRoleId);

        // Get stats
        // TODO: use util method isntead
        EntityStatMap statMap = store.getComponent(targetRef, EntityStatMap.getComponentType());
        if (statMap == null) { fail(context); return; }

        IndexedLookupTableAssetMap<String, EntityStatType> assetMap = EntityStatType.getAssetMap();
        EntityStatValue healthStat = statMap.get(DefaultEntityStatTypes.getHealth());
        if (healthStat == null) { fail(context); return; }

        float currentHp  = healthStat.get();
        float maxHp      = healthStat.getMax();
        EntityStatValue lvlV = statMap.get(assetMap.getIndex("Lvl"));
        EntityStatValue expV = statMap.get(assetMap.getIndex("Exp"));
        float currentLvl = (lvlV != null) ? lvlV.get() : 1f;
        float currentExp = (expV != null) ? expV.get() : 0f;

        // scale
        float scale = 1f;
        EntityScaleComponent scaleComp = store.getComponent(
            targetRef, EntityModule.get().getEntityScaleComponentType());
        if (scaleComp != null) scale = scaleComp.getScale();

        // item drop pos
        TransformComponent transform = store.getComponent(
            targetRef, EntityModule.get().getTransformComponentType());
        if (transform == null) { fail(context); return; }
        Vector3d pos = transform.getPosition();

        // set owner
        String ownerUuid = null;
        if (throwerRef != null) {
            Player player = store.getComponent(throwerRef, Player.getComponentType());
            if (player != null) ownerUuid = player.getUuid().toString();
        }

        PkmnStatsComponent pkmnStats = store.getComponent(
            targetRef, PkmnStatsComponent.getComponentType());

        PkmnCaptureMetadata captureMeta = new PkmnCaptureMetadata();
        captureMeta.setCurrentHp(currentHp);
        captureMeta.setMaxHp(maxHp);
        captureMeta.setModelScale(scale);
        captureMeta.setLevel((int) currentLvl);
        captureMeta.setExperience((long) currentExp);
        captureMeta.setOwnerUuid(ownerUuid);
        if (pkmnStats != null) {
            captureMeta.setBaseStats(pkmnStats.getBaseStats());
            captureMeta.setEvs(pkmnStats.getEvs());
            captureMeta.setIvs(pkmnStats.getIvs());
            if (pkmnStats.getNature()   != null) captureMeta.setNature(pkmnStats.getNature());
            if (pkmnStats.getNickname() != null) captureMeta.setNickname(pkmnStats.getNickname());
        }

        // vanilla metadata
        // TODO: combine with UseCaptureOrbInteraction logic, move to utils
        CapturedNPCMetadata npcMeta = new CapturedNPCMetadata();
        npcMeta.setNpcNameKey(tameRoleId);

        PersistentModel persistentModel = store.getComponent(
            targetRef, PersistentModel.getComponentType());
        if (persistentModel != null) {
            ModelAsset modelAsset = (ModelAsset) ModelAsset.getAssetMap().getAsset(
                persistentModel.getModelReference().getModelAssetId());
            if (modelAsset != null) npcMeta.setIconPath(modelAsset.getIcon());
        }
        String speciesIcon = SPECIES_ICON_PREFIX + tameRoleId + SPECIES_ICON_SUFFIX;
        npcMeta.setFullItemIcon(speciesIcon);

        // set owned to prevent a second projectile hit from triggering again
        if (pkmnStats == null) pkmnStats = new PkmnStatsComponent();
        pkmnStats.setOwnerUuid(ownerUuid);
        commandBuffer.putComponent(
            targetRef, PkmnStatsComponent.getComponentType(), pkmnStats);

        ItemStack ball         = new ItemStack(captureItemId, 1);
        ItemStack withNpc      = ball.withMetadata(CapturedNPCMetadata.KEYED_CODEC,  npcMeta);
        ItemStack capturedBall = withNpc.withMetadata(PkmnCaptureMetadata.KEYED_CODEC, captureMeta);

        // drop as
        final Vector3d dropPos   = pos;
        final ItemStack finalBall = capturedBall;
        commandBuffer.run(runStore -> {
            List<ItemStack> items = new ArrayList<>();
            items.add(finalBall);
            Holder<EntityStore>[] drops = ItemComponent.generateItemDrops(
                runStore, items, dropPos, new Vector3f());
            commandBuffer.addEntities(drops, AddReason.SPAWN);
        });

        LOGGER.atInfo().log(String.format(
            "PkmnCaptureFinalize: %s lv%.0f => tamed as %s, owner=%s",
            wildRoleId, currentLvl, tameRoleId, ownerUuid));

        context.getState().state = InteractionState.Finished;
    }

    @Nonnull
    private static String resolveTameRole(@Nullable String wildRoleId) {
        if (wildRoleId == null || wildRoleId.isBlank()) return "";
        return wildRoleId.endsWith("_Tamed") ? wildRoleId : wildRoleId + "_Tamed";
    }

    private static void fail(@Nonnull InteractionContext context) {
        context.getState().state = InteractionState.Failed;
    }
}
