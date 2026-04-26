package dev.hytalemodding.util;

import java.util.ArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier.ModifierTarget;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier.CalculationType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.effect.ActiveEntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.metadata.CapturedNPCMetadata;

import dev.hytalemodding.components.PkmnCaptureMetadata;
import dev.hytalemodding.components.PkmnStatsComponent;
import dev.hytalemodding.components.PkmnStatsComponent.PkmnStat;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;;


public class PkmnStatUtils {
    private static final String SPECIES_ICON_PREFIX = "Icons/Items/Pokeball/";
    private static final String SPECIES_ICON_SUFFIX = ".png";
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final int[] PLAYER_BASE_STATS = {100,30,60,20,80,75};
    public static final float PLAYER_HP_BONUS_FACTOR = 0.5f;
    public static final int DEAULT_STAT_MAX = 50;

    /**
     * 
     * @param commandBuffer
     * @param ref
     * @param roleName
     * @param pkmnStats
     */
    public static void setPkmnNameplate(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull String roleName,
        @Nonnull PkmnStatsComponent pkmnStats
    ){
        String nameplateString = buildNamplateString(roleName,pkmnStats,null);
        commandBuffer.putComponent(ref,Nameplate.getComponentType(),new Nameplate(nameplateString));
    }

    /**
     * 
     * @param commandBuffer
     * @param targetRef
     * @param ball
     * @return
     */
    public static ItemStack linkNpcWithBall(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> targetRef,
        @Nonnull ItemStack ball
    ) {
        Store<EntityStore> store = commandBuffer.getExternalData().getStore();
        if (store == null) return ball;

        NPCEntity npcComponent = (NPCEntity)
        commandBuffer.getComponent(targetRef, NPCEntity.getComponentType());
        if (npcComponent == null) return ball;

        // DeathComponent deathComponent = (DeathComponent)
        // commandBuffer.getComponent(targetRef, DeathComponent.getComponentType());
        // if (deathComponent != null) return ball;

        PkmnCaptureMetadata captureMetadata = (PkmnCaptureMetadata)
            ball.getFromMetadataOrNull("PkmnCapture", PkmnCaptureMetadata.CODEC);

        UUIDComponent targetUuid = store.getComponent(targetRef, UUIDComponent.getComponentType());
        String entityId = targetUuid.getUuid().toString();
        captureMetadata.setNpcEntityUuid(entityId);

        LOGGER.atInfo().log("released NPC with uuid: "+entityId);

        return ball.withMetadata(PkmnCaptureMetadata.KEYED_CODEC, captureMetadata);
    }

    /**
     * 
     * @param commandBuffer
     * @param targetRef
     * @param pkmnMetadata
     */
    public static void applyMetadata(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> targetRef,
        @Nonnull PkmnCaptureMetadata pkmnMetadata
    ){
        Store<EntityStore> store = commandBuffer.getExternalData().getStore();
        if (store == null) return;

        NPCEntity npcComponent = (NPCEntity)
        commandBuffer.getComponent(targetRef, NPCEntity.getComponentType());
        if (npcComponent == null) return;

        DeathComponent deathComponent = (DeathComponent)
        commandBuffer.getComponent(targetRef, DeathComponent.getComponentType());
        if (deathComponent != null) return;

        EntityStatMap stats = store.getComponent(targetRef, EntityStatMap.getComponentType());
        if (stats == null) return;

        IndexedLookupTableAssetMap<String, EntityStatType> assetMap = EntityStatType.getAssetMap();
        int             lvlIdx = assetMap.getIndex("Lvl");
        int             expIdx = assetMap.getIndex("Exp");
        int             atkIdx = assetMap.getIndex("Atk");
        int             defIdx = assetMap.getIndex("Def");
        int             spAtkIdx = assetMap.getIndex("SpAtk");
        int             spDefIdx = assetMap.getIndex("SpDef");
        int             spdIdx = assetMap.getIndex("Spd");

        int             healthIdx = DefaultEntityStatTypes.getHealth();
        EntityStatValue health    = stats.get(healthIdx);
        EntityStatValue lvl       = stats.get(lvlIdx);
        EntityStatValue exp       = stats.get(expIdx);
        EntityStatValue atk       = stats.get(atkIdx);
        EntityStatValue def       = stats.get(defIdx);
        EntityStatValue spAtk     = stats.get(spAtkIdx);
        EntityStatValue spDef     = stats.get(spDefIdx);
        EntityStatValue spd       = stats.get(spdIdx);


        EntityScaleComponent scaleComponent = store.getComponent(
            targetRef, EntityModule.get().getEntityScaleComponentType());
        // if (scaleComponent != null) scale = scaleComponent.getScale();
        scaleComponent.setScale(pkmnMetadata.getModelScale());

        PkmnStatsComponent pkmnStats = fromMetadata(pkmnMetadata);

        String roleId = NPCPlugin.get().getName(npcComponent.getRoleIndex());
        int[] baseStats = pkmnStats.getBaseStats();
        if (baseStats == null || baseStats[0] == 0){
            String species = toDisplayName(roleId);
            pkmnStats.setBaseStats(PkmnBaseStatList.fromMap(species));
        }

        float currentHp = pkmnMetadata.getCurrentHp();

        apply(store,commandBuffer,targetRef,pkmnStats);
        stats.setStatValue(healthIdx,currentHp);
        
        commandBuffer.putComponent(targetRef, EntityModule.get().getEntityScaleComponentType(), scaleComponent);
        commandBuffer.putComponent(targetRef, EntityStatMap.getComponentType(), stats);
    }

    public static int[] getCurrentStats(
        @Nonnull CommandBuffer<EntityStore>  commandBuffer, 
        @Nonnull Ref<EntityStore> ref
    ){
        Store<EntityStore> store = commandBuffer.getExternalData().getStore();
        if (store == null) return null;

        boolean isDead = store.getComponent(ref, DeathComponent.getComponentType()) != null;
        if (isDead) {
            LOGGER.atInfo().log("NPC is dead");
            return null;
        }

        EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());
        if (stats == null){ 
            LOGGER.atInfo().log("Entity has no EntityStatMap"); 
            return null;
        }

        // // Player
        // // TODO: check if pkmn or not?
        // Player player = store.getComponent(ref, Player.getComponentType());
        // boolean isPlayer = player != null;
        // if (isPlayer) {

        //     //playerBaseStats
        // }


        IndexedLookupTableAssetMap<String, EntityStatType> assetMap = EntityStatType.getAssetMap();
        int             lvlIdx = assetMap.getIndex("Lvl");
        int             expIdx = assetMap.getIndex("Exp");
        int             atkIdx = assetMap.getIndex("Atk");
        int             defIdx = assetMap.getIndex("Def");
        int             spAtkIdx = assetMap.getIndex("SpAtk");
        int             spDefIdx = assetMap.getIndex("SpDef");
        int             spdIdx = assetMap.getIndex("Spd");

        int             healthIdx = DefaultEntityStatTypes.getHealth();
        var health    = (int) stats.get(healthIdx).get();
        var lvl       = (int) stats.get(lvlIdx).get();
        var exp       = (int) stats.get(expIdx).get();
        var atk       = (int) stats.get(atkIdx).get();
        var def       = (int) stats.get(defIdx).get();
        var spAtk     = (int) stats.get(spAtkIdx).get();
        var spDef     = (int) stats.get(spDefIdx).get();
        var spd       = (int) stats.get(spdIdx).get();

        int[] statArray = {health,atk,def,spAtk,spDef,spd};
        return statArray;
    }

    public static PkmnStatsComponent getPkmnStatsComponent(
        @Nonnull CommandBuffer<EntityStore>  commandBuffer, 
        @Nonnull Ref<EntityStore> ref
    ){
        Store<EntityStore> store = commandBuffer.getExternalData().getStore();
        if (store == null) return null;

        // NPCEntity npcComponent = (NPCEntity)
        // store.getComponent(ref, NPCEntity.getComponentType());
        // if (npcComponent == null) return null;

        boolean isDead = store.getComponent(ref, DeathComponent.getComponentType()) != null;
        if (isDead) LOGGER.atInfo().log("NPC is dead"); // return null;


        EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());
        if (stats == null){ 
            LOGGER.atInfo().log("Entity has no EntityStatMap"); 
            return null;
        }

        IndexedLookupTableAssetMap<String, EntityStatType> assetMap = EntityStatType.getAssetMap();
        int             lvlIdx = assetMap.getIndex("Lvl");
        int             expIdx = assetMap.getIndex("Exp");
        int             atkIdx = assetMap.getIndex("Atk");
        int             defIdx = assetMap.getIndex("Def");
        int             spAtkIdx = assetMap.getIndex("SpAtk");
        int             spDefIdx = assetMap.getIndex("SpDef");
        int             spdIdx = assetMap.getIndex("Spd");

        int             healthIdx = DefaultEntityStatTypes.getHealth();
        EntityStatValue health    = stats.get(healthIdx);
        EntityStatValue lvl       = stats.get(lvlIdx);
        EntityStatValue exp       = stats.get(expIdx);
        EntityStatValue atk       = stats.get(atkIdx);
        EntityStatValue def       = stats.get(defIdx);
        EntityStatValue spAtk     = stats.get(spAtkIdx);
        EntityStatValue spDef     = stats.get(spDefIdx);
        EntityStatValue spd       = stats.get(spdIdx);

        if (health == null) return null;
        float currentHp = isDead?0:health.get();
        float maxHp     = health.getMax();
        float currentExp = exp.get();
        float currentLvl = lvl.get();
        float currentAtk =   atk.get();
        float currentDef =   def.get();
        float currentSpAtk = spAtk.get();
        float currentSpDef = spDef.get();
        float currentSpd =   spd.get();

        PkmnStatsComponent pkmnStats = store.getComponent(ref, PkmnStatsComponent.getComponentType());
        if (pkmnStats == null) pkmnStats = new PkmnStatsComponent();

        pkmnStats.setLevel((int)currentLvl);
        pkmnStats.setExperience((long) currentExp);
        int[] baseStats = pkmnStats.getBaseStats();

        NPCEntity npcEntity = store.getComponent(ref, NPCEntity.getComponentType());
        if (npcEntity != null) {
            String roleName = npcEntity.getRoleName();
            String species = toDisplayName(roleName);
            var defaultBaseStats = PkmnBaseStatList.fromMap(species);
            if(baseStats!=defaultBaseStats) pkmnStats.setBaseStats(defaultBaseStats);
        }
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null && baseStats!=PLAYER_BASE_STATS) pkmnStats.setBaseStats(PLAYER_BASE_STATS);

        if ( baseStats[PkmnStat.ATK.index] == 0) { LOGGER.atInfo().log("Entity base ATK is 0"); }
        if ( baseStats[PkmnStat.DEF.index] == 0) { LOGGER.atInfo().log("Entity base DEF is 0"); }
        return pkmnStats;
    }


    /**
     * 
     * @param store
     * @param targetRef
     * @return
     */
    public static PkmnCaptureMetadata captureMetadata(
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> targetRef
    ){
        return _captureMetadata(store,targetRef);
    }

    /**
     * 
     * @param commandBuffer
     * @param targetRef
     * @return
     */
    public static PkmnCaptureMetadata captureMetadata(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> targetRef
    ){
        Store<EntityStore> store = commandBuffer.getExternalData().getStore();
        if (store == null) return null;
        return _captureMetadata(store,targetRef);
    }

    private static PkmnCaptureMetadata _captureMetadata(
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> targetRef
    ) {
        NPCEntity npcComponent = (NPCEntity)
        store.getComponent(targetRef, NPCEntity.getComponentType());
        if (npcComponent == null) return null;

        boolean isDead = store.getComponent(targetRef, DeathComponent.getComponentType()) != null;
        if (isDead) LOGGER.atInfo().log("NPC is dead"); // return null;

        EntityStatMap stats = store.getComponent(targetRef, EntityStatMap.getComponentType());
        if (stats == null){ 
            LOGGER.atInfo().log("NPC has no EntityStatMap"); 
            return null;
        }
        IndexedLookupTableAssetMap<String, EntityStatType> assetMap = EntityStatType.getAssetMap();
        int             lvlIdx = assetMap.getIndex("Lvl");
        int             expIdx = assetMap.getIndex("Exp");
        int             atkIdx = assetMap.getIndex("Atk");
        int             defIdx = assetMap.getIndex("Def");
        int             spAtkIdx = assetMap.getIndex("SpAtk");
        int             spDefIdx = assetMap.getIndex("SpDef");
        int             spdIdx = assetMap.getIndex("Spd");

        int             healthIdx = DefaultEntityStatTypes.getHealth();
        EntityStatValue health    = stats.get(healthIdx);
        EntityStatValue lvl       = stats.get(lvlIdx);
        EntityStatValue exp       = stats.get(expIdx);
        EntityStatValue atk       = stats.get(atkIdx);
        EntityStatValue def       = stats.get(defIdx);
        EntityStatValue spAtk     = stats.get(spAtkIdx);
        EntityStatValue spDef     = stats.get(spDefIdx);
        EntityStatValue spd       = stats.get(spdIdx);

        if (health == null) return null;
        float currentHp = isDead?0:health.get();
        float maxHp     = health.getMax();
        float currentExp = exp.get();
        float currentLvl = lvl.get();
        float currentAtk =   atk.get();
        float currentDef =   def.get();
        float currentSpAtk = spAtk.get();
        float currentSpDef = spDef.get();
        float currentSpd =   spd.get();

        // TODO: do something with size
        float scale = 1f;
        EntityScaleComponent scaleComponent = store.getComponent(
            targetRef, EntityModule.get().getEntityScaleComponentType());
        if (scaleComponent != null) scale = scaleComponent.getScale();


        String roleId = NPCPlugin.get().getName(npcComponent.getRoleIndex());
        int[] baseStats = PkmnBaseStatList.fromMap(toDisplayName(roleId));

        PkmnStatsComponent pkmnStats = (PkmnStatsComponent)
            store.getComponent(targetRef, PkmnStatsComponent.getComponentType());

        if(pkmnStats == null) {
            pkmnStats = new PkmnStatsComponent();
            String species = toDisplayName(roleId);
            pkmnStats.setBaseStats(PkmnBaseStatList.fromMap(species));
        }

        pkmnStats.setExperience((long)currentExp);
        pkmnStats.setLevel((int)currentLvl);

        PkmnCaptureMetadata captureMetadata = new PkmnCaptureMetadata();
        captureMetadata.setCurrentHp(currentHp);
        captureMetadata.setMaxHp(maxHp);
        captureMetadata.setModelScale(scale);
        captureMetadata.setLevel((int)(currentLvl));
        captureMetadata.setExperience((long)(currentExp));
        captureMetadata.setBaseStats(pkmnStats.getBaseStats());
        captureMetadata.setEvs(pkmnStats.getEvs());
        captureMetadata.setIvs(pkmnStats.getIvs());
        captureMetadata.setNature(pkmnStats.getNature());
        captureMetadata.setNickname(pkmnStats.getNickname());
        captureMetadata.setOwnerUuid(pkmnStats.getOwnerUuid());
        captureMetadata.setNpcStatus(isDead?"Fainted":"Healthy");
        return captureMetadata;
    }

    /**
     * 
     * @param commandBuffer
     * @param targetRef
     * @param sourceItem
     * @param fullIcon
     * @return
     */
    public static CapturedNPCMetadata getNpcMetadata(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> targetRef,
        ItemStack sourceItem,
        String fullIcon
    ){
        NPCEntity npcComponent = (NPCEntity)
            commandBuffer.getComponent(targetRef, NPCEntity.getComponentType());
        if (npcComponent == null) return null;
        
        String roleId = NPCPlugin.get().getName(npcComponent.getRoleIndex());
        String tameRoleId = resolveTameRole(roleId);

        CapturedNPCMetadata npcMeta = new CapturedNPCMetadata();
        if(sourceItem !=null){
        npcMeta = (CapturedNPCMetadata)
            sourceItem.getFromMetadataOrDefault("CapturedEntity", CapturedNPCMetadata.CODEC);
        }
        npcMeta.setNpcNameKey(tameRoleId);

        // Icon
        PersistentModel persistentModel = (PersistentModel)
            commandBuffer.getComponent(targetRef, PersistentModel.getComponentType());
        if (persistentModel == null){ 
            LOGGER.atInfo().log("persistentModel null");
            return null;
        }

        ModelAsset modelAsset = (ModelAsset) ModelAsset.getAssetMap().getAsset(
            persistentModel.getModelReference().getModelAssetId());
        if (modelAsset != null) {
            npcMeta.setIconPath(modelAsset.getIcon());
        }

        // Per-species full icon: Icons/Items/Pokeball/<roleId>.png
        // TODO: fallback to fullIcon from interaction config
        String speciesIcon = (tameRoleId != null)
            ? SPECIES_ICON_PREFIX + tameRoleId + SPECIES_ICON_SUFFIX
            : null;

        if (speciesIcon != null) {
            npcMeta.setFullItemIcon(speciesIcon);
        } else if (fullIcon != null) {
            npcMeta.setFullItemIcon(fullIcon);
        }
        return npcMeta;
    }

    /**
     * Converts {@link PkmnCaptureMetadata} to {@link PkmnStatsComponent}]<br>
     * 
     * @param metadata
     * @return 
     */
    public static PkmnStatsComponent fromMetadata(@Nonnull PkmnCaptureMetadata metadata){
        PkmnStatsComponent pkmnStats = new PkmnStatsComponent();
        var metaMaxHp           = metadata.getMaxHp();
        var metaCurrentHp       = metadata.getCurrentHp();
        var metaModelScale      = metadata.getModelScale();
        var metaEVs             = metadata.getEvs();
        var metaIVs             = metadata.getIvs();
        var metaNature          = metadata.getNature();
        var metaExperience      = metadata.getExperience();
        var metaLevel           = metadata.getLevel();
        var metaNickname        = metadata.getNickname();
        var metaOwnerUuid       = metadata.getOwnerUuid();
        var metaBaseStats       = metadata.getBaseStats();
        pkmnStats.setExperience(metaExperience);
        pkmnStats.setLevel(metaLevel);
        pkmnStats.setEvs(metaEVs);
        pkmnStats.setIvs(metaIVs);
        if(metaNature!=null && !metaNature.isBlank()) pkmnStats.setNature(metaNature);
        if(metaNickname!=null && !metaNickname.isBlank()) pkmnStats.setNickname(metaNickname);
        if(metaOwnerUuid!=null && !metaOwnerUuid.isBlank()) pkmnStats.setOwnerUuid(metaOwnerUuid);
        pkmnStats.setBaseStats(metaBaseStats);

        // LOGGER.atInfo().log("GetPkmnStats.fromMetadata: Lvl="+String.valueOf(metaLevel));
        // LOGGER.atInfo().log("GetPkmnStats.fromMetadata: Lvl="+String.valueOf(pkmnStats.getLevel()));
        return pkmnStats;
    }


    public static void setCurrentHp(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> entityRef,
        @Nonnull float hp
    ) {
        EntityStatMap stats = commandBuffer.getComponent(entityRef, EntityStatMap.getComponentType());
        if (stats == null) return;

        int healthIdx  = DefaultEntityStatTypes.getHealth();
        stats.setStatValue(healthIdx, (float) hp);

        commandBuffer.putComponent(entityRef, EntityStatMap.getComponentType(), stats);
    }

    /**
     * no CommandBuffer available, use store.putComponent instead<br>
     * 
     * @param store
     * @param entityRef
     * @param pkmnStats
     */
    public static void apply(
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> entityRef,
        @Nonnull PkmnStatsComponent pkmnStats
    ) {
        EntityStatMap stats = store.getComponent(entityRef, EntityStatMap.getComponentType());
        if (stats == null) return;
        boolean isPlayer = store.getComponent(entityRef,Player.getComponentType())!=null;
        NPCEntity npcEntity = store.getComponent(entityRef,NPCEntity.getComponentType());
        boolean isPkmn = npcEntity != null && filterByRoleName(npcEntity.getRoleName());
        _applyToStatMap(stats, pkmnStats, isPlayer, isPkmn);
        store.putComponent(entityRef, EntityStatMap.getComponentType(), stats);
    }

    /**
     * 
     * @param store
     * @param commandBuffer
     * @param entityRef
     * @param pkmnStats
     */
    public static void apply(
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull PkmnStatsComponent pkmnStats
    ) {
        EntityStatMap stats = commandBuffer.getComponent(entityRef, EntityStatMap.getComponentType());
        if (stats == null) return;
        boolean isPlayer = commandBuffer.getComponent(entityRef,Player.getComponentType())!=null;
        NPCEntity npcEntity = commandBuffer.getComponent(entityRef,NPCEntity.getComponentType());
        boolean isPkmn = npcEntity != null && filterByRoleName(npcEntity.getRoleName());
        _applyToStatMap(stats, pkmnStats, isPlayer, isPkmn);
        commandBuffer.putComponent(entityRef, EntityStatMap.getComponentType(), stats);
    }

    /**
     * Shared stat calculation<br>
     * 
     * @param stats
     * @param pkmnStats
     */
    private static void _applyToStatMap(
        @Nonnull EntityStatMap stats,
        @Nonnull PkmnStatsComponent pkmnStats,
        boolean isPlayer,
        boolean isPkmn
    ) {

        IndexedLookupTableAssetMap<String, EntityStatType> assetMap = EntityStatType.getAssetMap();
        int healthIdx  = DefaultEntityStatTypes.getHealth();
        int lvlIdx     = assetMap.getIndex("Lvl");
        int expIdx     = assetMap.getIndex("Exp");
        int atkIdx     = assetMap.getIndex("Atk");
        int defIdx     = assetMap.getIndex("Def");
        int spAtkIdx   = assetMap.getIndex("SpAtk");
        int spDefIdx   = assetMap.getIndex("SpDef");
        int speIdx     = assetMap.getIndex("Spd");

        if (isPlayer){
            int calcHp = pkmnStats.calcEffectiveStat(PkmnStat.HP.index);
            int hpBonus = Math.max(0, Math.round((calcHp - 100) * PLAYER_HP_BONUS_FACTOR));
            stats.putModifier(healthIdx, "NPC_Max",
                new StaticModifier(ModifierTarget.MAX, CalculationType.ADDITIVE, hpBonus));
        } else if (isPkmn) {
            int calcHp = pkmnStats.calcEffectiveStat(PkmnStat.HP.index);
            stats.putModifier(healthIdx, "NPC_Max",
                new StaticModifier(ModifierTarget.MAX, CalculationType.ADDITIVE, calcHp - 100));
        } else {
            // int calcHp = pkmnStats.calcEffectiveStat(PkmnStat.HP.index);
            // int hpBonus = Math.max(0, Math.round((calcHp - 100) * PLAYER_HP_BONUS_FACTOR));
            // stats.putModifier(healthIdx, "NPC_Max",
            //     new StaticModifier(ModifierTarget.MAX, CalculationType.ADDITIVE, hpBonus));
        }


        int level = pkmnStats.getLevel();
        int expMax = level * level * level;

        if (atkIdx   >= 0) {
            int calcStat = pkmnStats.calcEffectiveStat(PkmnStat.ATK.index);
            stats.putModifier(atkIdx, "NPC_Max", new StaticModifier(ModifierTarget.MAX, CalculationType.ADDITIVE, calcStat - DEAULT_STAT_MAX));
            stats.setStatValue(atkIdx, (float) calcStat);
        }
        if (defIdx   >= 0) {
            int calcStat = pkmnStats.calcEffectiveStat(PkmnStat.DEF.index);
            stats.putModifier(defIdx, "NPC_Max", new StaticModifier(ModifierTarget.MAX, CalculationType.ADDITIVE, calcStat - DEAULT_STAT_MAX));
            stats.setStatValue(defIdx, (float) calcStat);
        }
        if (spAtkIdx >= 0) {
            int calcStat = pkmnStats.calcEffectiveStat(PkmnStat.SPATK.index);
            stats.putModifier(spAtkIdx, "NPC_Max", new StaticModifier(ModifierTarget.MAX, CalculationType.ADDITIVE, calcStat - DEAULT_STAT_MAX));
            stats.setStatValue(spAtkIdx, (float) calcStat);
        }
        if (spDefIdx >= 0) {
            int calcStat = pkmnStats.calcEffectiveStat(PkmnStat.SPDEF.index);
            stats.putModifier(spDefIdx, "NPC_Max", new StaticModifier(ModifierTarget.MAX, CalculationType.ADDITIVE, calcStat - DEAULT_STAT_MAX));
            stats.setStatValue(spDefIdx, (float) calcStat);
        }
        if (speIdx   >= 0) {
            int calcStat = pkmnStats.calcEffectiveStat(PkmnStat.SPD.index);
            stats.putModifier(speIdx, "NPC_Max", new StaticModifier(ModifierTarget.MAX, CalculationType.ADDITIVE, calcStat - DEAULT_STAT_MAX));
            stats.setStatValue(speIdx, (float) calcStat);
        }
        if (level   >= 0) stats.setStatValue(lvlIdx,   (float) level);

        if (expIdx >= 0) {
            stats.putModifier(expIdx, "NPC_Max", new StaticModifier(ModifierTarget.MAX, CalculationType.ADDITIVE, expMax - 1000));
            stats.setStatValue(expIdx, pkmnStats.getExperience());
        }
    }

    /**
     * Remove prefix/suffix from roleId<br>
     * 
     * @param roleId
     * @return
     */
    @Nonnull
    public static String displayNameOf(@Nullable String roleId) {
        if (roleId == null || roleId.isEmpty()) return "";

        // Strip leading "Pkmn_" prefix (case-insensitive)
        if (roleId.regionMatches(true, 0, "Pkmn_", 0, 5)) {
            roleId = roleId.substring(5);
        }

        // Strip known trailing variant suffixes
        for (String suffix : new String[]{"_Tamed", "_Shiny"}) {
            if (roleId.endsWith(suffix)) {
                roleId = roleId.substring(0, roleId.length() - suffix.length());
                break;
            }
        }

        // Title-case each underscore-separated word
        String[] words = roleId.split("_");
        StringBuilder stringBuilder = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (stringBuilder.length() > 0) stringBuilder.append(' ');
            stringBuilder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) stringBuilder.append(word.substring(1).toLowerCase());
        }
        return stringBuilder.toString();
    }

    /**
     * Converts a role ID to the species display name.<br>
     * <br>
     * Strips Prefixes:<br>
     *      Pkmn_<br>
     * <br>
     * And Suffixes:<br>
     *      _Tamed<br>
     * <br>
     * <br>
    */
    @Nonnull
    public static String toDisplayName(@Nullable String roleId) {
        if (roleId == null || roleId.isEmpty()) return "";

        // Strip leading "Pkmn_" prefix (case-insensitive)
        String s = roleId;
        if (s.regionMatches(true, 0, "Pkmn_", 0, 5)) {
            s = s.substring(5);
        }

        // Strip known trailing variant suffixes
        for (String suffix : new String[]{"_Tamed", "_Shiny"}) {
            if (s.endsWith(suffix)) {
                s = s.substring(0, s.length() - suffix.length());
                break;
            }
        }

        // Title-case each underscore-separated word
        String[] words = s.split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) sb.append(word.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    /**
     * 
     * @param npcRoleId
     * @return
     */
    public static String speciesFromRole(@Nonnull String npcRoleId){
        String species = npcRoleId;
        if(npcRoleId.startsWith("Pkmn_")){
            species = npcRoleId.replaceFirst("Pkmn_", "");
        }
        if(isTamed(npcRoleId)){
            species = species.replaceFirst("_Tamed", "");
        }
        species = species.replaceAll("_"," ");
        return species;
    }

    /**
     * 
     * @param npcRoleId
     * @return
     */
    public static boolean isTamed(@Nonnull String npcRoleId){
        return npcRoleId.endsWith("_Tamed");
    }

    /**
     * 
     * @param owner
     * @param player
     * @return
     */
    public static boolean hasOtherOwner(
        @Nullable String owner,
        @Nullable Player player
    ){
        if (owner == null || owner.isBlank())   return false;
        if(player == null)                      return true;
        String playerId = player.getUuid().toString();
        if(playerId == null)                    return true;
        return !owner.equals(playerId);
    }

    /**
     * 
     * @param wildRoleId
     * @return
     */
    @Nonnull
    public static String resolveTameRole(@Nullable String wildRoleId) {
        if (wildRoleId == null || wildRoleId.isBlank()) return "";
        return wildRoleId.endsWith("_Tamed") ? wildRoleId : wildRoleId + "_Tamed";
    }

    /**
     * 
     * @param npcRoleId
     * @param stats
     * @param playerUuid
     * @return
     */
    public static String buildNamplateString(
        @Nonnull String npcRoleId,
        @Nonnull PkmnStatsComponent stats,
        @Nullable String playerUuid
    ){
        String name = speciesFromRole(npcRoleId);
        var nickname = stats.getNickname();
        if(nickname != null && !nickname.isBlank()){
            name=nickname;
        }
        if(isTamed(npcRoleId)){
            name = "(-o-) "+name;
        }
        String lvl = String.valueOf(stats.getLevel());
        return name+" ["+lvl+"]";
    }

    /**
     * 
     * @param roleName
     * @return
     */
    public static boolean filterByRoleName(String roleName) {
        if(roleName.startsWith("Pkmn_")) return true;
        if(roleName.startsWith("Lizard_Ground_")) return true;
        return false;
    }

    /**
     * Generation III damage formula
     * <br>
     * Damage=(((2×Level5+2)×Power×A/D50)×Burn×Screen×Targets×Weather×FF+2)×Stockpile×Critical×DoubleDmg×Charge×HH×STAB×Type1×Type2×random
     * <br>
     * <br>
     * where:
     * <br>
     * Level is the level of the attacking Pokémon. If the used move is Beat Up, L is instead the level of the Pokémon performing the strike.
     * <br> A is the effective Attack stat of the attacking Pokémon if the used move is a physical move, or the effective Special Attack stat of the attacking Pokémon if the used move is a special move (for a critical hit, negative Attack or Special Attack stat stages are ignored). If the used move is Beat Up, A is instead the base Attack of the Pokémon performing the strike.
     * <br> D is the effective Defense stat of the target if the used move is a physical move, or the effective Special Defense stat of the target if the used move is a special move (for a critical hit, positive Defense or Special Defense stat stages are ignored). If the used move is Beat Up, D is instead the base Defense of the target.
     * <br> Power is the effective power of the used move.
     * <br> Burn is 0.5 if the attacker is burned, its Ability is not Guts, and the used move is a physical move, and 1 otherwise.
     * <br> Screen is 0.5 if the used move is physical and Reflect is present on the target's side of the field, or special and Light Screen is present. For a Double Battle, Screen is instead 2/3, and 1 otherwise or if the used move lands a critical hit. However, if, in a Double Battle, when the move is executed, the only Pokémon on the target's side is the target, Screen remains as 0.5.
     * <br> Targets is 0.5 in Double Battles if the move targets both foes (unless it targets all other Pokémon, like Earthquake, and only if there is more than one such target when the move is executed, regardless of whether the move actually hits or can hit all the targets), and 1 otherwise.
     * <br> If the base damage after applying Targets is 0 and the move is physical the base damage is increased by one.[1]
     * <br> Weather is 1.5 if a Water-type move is being used during rain or a Fire-type move during harsh sunlight, and 0.5 if a Water-type move is used during harsh sunlight, any Fire-type move during rain, or SolarBeam during any non-clear weather besides harsh sunlight, and 1 otherwise or if any Pokémon on the field have the Ability Cloud Nine or Air Lock.
     * <br> FF is 1.5 if the used move is Fire-type, and the attacker's Ability is Flash Fire that has been activated by a Fire-type move, and 1 otherwise.
     * <br> Stockpile is 1, 2, or 3 if the used move is Spit Up, depending on how many Stockpiles have been used, or always 1 if the used move is not Spit Up.
     * <br> Critical is 2 for a critical hit, and 1 otherwise. It is always 1 if Future Sight, Doom Desire, or Spit Up is used, if the target's Ability is Battle Armor or Shell Armor, or if the battle is the first one against PoochyenaRS/ZigzagoonE or the capture tutorial where Wally catches a Ralts.
     * <br> DoubleDmg is 2 if the used move is (and 1 if the used move is not any of these moves): 
     * <br>     - Gust or Twister and the target is in the semi-invulnerable turn of Fly or Bounce.
     * <br>     - Stomp, Needle Arm, Astonish, or Extrasensory and the target has previously used Minimize.
     * <br>     - Surf or Whirlpool and the target is in the semi-invulnerable turn of Dive.
     * <br>     - Earthquake or Magnitude and the target is in the semi-invulnerable turn of Dig.
     * <br>     - Pursuit and the target is attempting to switch out.
     * <br>     - Facade and the user is poisoned, burned, or paralyzed.
     * <br>     - SmellingSalt and the target is paralyzed.
     * <br>     - Revenge and the attacker has been damaged by the target this turn.
     * <br>     - Weather Ball, there is non-clear weather, and no Pokémon on the field have the Ability Cloud Nine or Air Lock.
     * <br> Charge is 2 if the move is Electric-type and Charge takes effect, and 1 otherwise.
     * <br> HH is 1.5 if the attacker's ally in a Double Battle has used Helping Hand on it, and 1 otherwise.
     * <br> STAB is the same-type attack bonus. This is equal to 1.5 if the move's type matches any of the user's types and 1 if otherwise.
     * <br> Type1 is the type effectiveness of the used move against the target's first type (or only type, if it only has a single type). This can be 0.5 (not very effective), 1 (normally effective), or 2 (super effective). If the used move is Struggle, Future Sight, Beat Up, or Doom Desire, both Type1 and Type2 are always 1.
     * <br> Type2 is the type effectiveness of the used move against the target's second type. This can be 0.5 (not very effective), 1 (normally effective), or 2 (super effective). If the target only has a single type, Type2 is 1.
     * <br> random is realized as a multiplication by a random uniformly distributed integer between 85 and 100 (inclusive), followed by an integer division by 100. random is always 1 if Spit Up is used.
     * @return
     */
    public static Integer damageFormula(
        int level,
        int atk,
        int def,
        float power,
        boolean attackerIsBurned,
        boolean defenderhasScreen,
        float weather,
        boolean attackerSTAB,
        boolean isCrit
    ) {
        float burn = attackerIsBurned ? 0.5f : 1; //0.5 if attacker is burned
        float screen = defenderhasScreen ? 0.5f : 1; //0.5 if defender used reflect/light screen
        //float weather = 1; //1.5 if weather buff, 0.5 if weather debuff
        float crit = isCrit ? 2 : 1;//2 for crit, 3 for crit with sniper
        float item = 1;//1.3 with attack boost item
        double random = 0.85+Math.random()*0.15;// 0.85-1.00
        float stab = attackerSTAB ? 1.5f : 1.0f;//1.5 if matching type


        float raw = (2+2*level/5)*power*atk/def/50;
        float withStatus = raw*burn*screen*weather+2;
        float damage = withStatus*crit*item*(float)random*stab;

        // LOGGER.atInfo().log("POW: "+power+", ATK: "+String.valueOf(atk)+", DEF: "+String.valueOf(def)+", STAB: "+attackerSTAB+", crit: "+crit+", lvl: "+level);

        return (int) damage;
    }

    public static ArrayList<String>  activeEffects(
        @Nonnull Store<EntityStore> store,
        @Nonnull  Ref<EntityStore> ref
    ) {
        EffectControllerComponent effectControllerComponent = store.getComponent(ref,EffectControllerComponent.getComponentType());
        IndexedLookupTableAssetMap<String, EntityEffect> effectMap = EntityEffect.getAssetMap();
        Int2ObjectMap<ActiveEntityEffect> activeEffects = effectControllerComponent.getActiveEffects();
        ArrayList<String> effectIds = new ArrayList<>();

        if(activeEffects != null && !activeEffects.isEmpty()){
            for(ActiveEntityEffect activeEffect : activeEffects.values()){
                int idx = activeEffect.getEntityEffectIndex();
                EntityEffect effect = effectMap.getAsset(idx);
                String effectId = effect.getId();
                effectIds.add(effectId);
            }
        }
        return effectIds;
    }

    public static boolean hasSTAB(
        ArrayList<String> activeEFfects,
        DamageCause attack
    ){
        String attackType = attack.getId();
        for(int i=0; i<activeEFfects.size(); i++){
            if( activeEFfects.get(i).equals(attackType+"Type")) return true;
        }
        return false;
    }
}
