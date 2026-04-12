package dev.hytalemodding.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
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
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import dev.hytalemodding.components.PkmnCaptureMetadata;
import dev.hytalemodding.components.PkmnStatsComponent;;


public class PkmnStatUtils {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();


    /**
     * Create {@link PkmnCaptureMetadata} for given {@code Ref<EntityStore>}<br>
     * 
     * @param commandBuffer
     * @param targetRef
     * @return [PkmnCaptureMetadata]
     */
    @Nullable
    public static PkmnCaptureMetadata captureMetadata(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> targetRef
    ) {
        // LOGGER.atInfo().log("GetPkmnStats.read");
        Store<EntityStore> store = commandBuffer.getExternalData().getStore();
        if (store == null) return null;

        NPCEntity npcComponent = (NPCEntity)
            commandBuffer.getComponent(targetRef, NPCEntity.getComponentType());
        if (npcComponent == null) return null;

        DeathComponent deathComponent = (DeathComponent)
            commandBuffer.getComponent(targetRef, DeathComponent.getComponentType());
        if (deathComponent != null) return null;


        // -- Get HP ----------
        EntityStatMap stats = store.getComponent(targetRef, EntityStatMap.getComponentType());
        if (stats == null) return null;
        IndexedLookupTableAssetMap<String, EntityStatType> assetMap = EntityStatType.getAssetMap();
        int             lvlIdx = assetMap.getIndex("Lvl");
        int             expIdx = assetMap.getIndex("Exp");
        int             healthIdx = DefaultEntityStatTypes.getHealth();
        EntityStatValue health    = stats.get(healthIdx);
        EntityStatValue lvl       = stats.get(lvlIdx);
        EntityStatValue exp       = stats.get(expIdx);
        if (health == null) return null;
        float currentHp = health.get();
        float maxHp     = health.getMax();
        float currentExp = exp.get();
        float currentLvl = lvl.get();

        

        // -- Get Scale ----------
        float scale = 1f;
        EntityScaleComponent scaleComponent = store.getComponent(
            targetRef, EntityModule.get().getEntityScaleComponentType());
        if (scaleComponent != null) scale = scaleComponent.getScale();
        // TransformComponent targetTransform = store.getComponent(targetRef, EntityModule.get().getTransformComponentType());
        // Vector3d targetPosition = targetTransform.getPosition();

        // -- Get PkmnStatsComponent ----------
        PkmnStatsComponent pokemonStats = (PkmnStatsComponent)
            commandBuffer.getComponent(targetRef, PkmnStatsComponent.getComponentType());

        String roleId = NPCPlugin.get().getName(npcComponent.getRoleIndex());
        if(pokemonStats == null) {
            pokemonStats = new PkmnStatsComponent();
            String species = displayNameOf(roleId);
            pokemonStats.setBaseStats(PkmnBaseStatList.fromMap(species));
            // IVs are randomised by default in PokemonStatsComponent field initialiser
        }

        // -- Get PkmnCaptureMetadata ----------
        PkmnCaptureMetadata captureMetadata = new PkmnCaptureMetadata();
        captureMetadata.setCurrentHp(currentHp);
        captureMetadata.setMaxHp(maxHp);
        captureMetadata.setModelScale(scale);
        captureMetadata.setLevel((int)(currentLvl));
        captureMetadata.setExperience((long)(currentExp));
        if (pokemonStats != null) {
            captureMetadata.setBaseStats(pokemonStats.getBaseStats());
            captureMetadata.setEvs(pokemonStats.getEvs());
            captureMetadata.setIvs(pokemonStats.getIvs());
            captureMetadata.setNature(pokemonStats.getNature());
            captureMetadata.setNickname(pokemonStats.getNickname());
            captureMetadata.setOwnerUuid(pokemonStats.getOwnerUuid());
        }
        // LOGGER.atInfo().log("GetPkmnStats.captureMetadata: currentExp="+String.valueOf(currentExp));
        // LOGGER.atInfo().log("GetPkmnStats.captureMetadata: currentLvl="+String.valueOf(currentLvl));
        // LOGGER.atInfo().log("GetPkmnStats.captureMetadata: Lvl="+String.valueOf(captureMetadata.getLevel()));
        return captureMetadata;
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
        _applyToStatMap(stats, pkmnStats);
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
        EntityStatMap stats = store.getComponent(entityRef, EntityStatMap.getComponentType());
        if (stats == null) return;
        _applyToStatMap(stats, pkmnStats);
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
        @Nonnull PkmnStatsComponent pkmnStats
    ) {
        IndexedLookupTableAssetMap<String, EntityStatType> assetMap = EntityStatType.getAssetMap();
        int healthIdx  = DefaultEntityStatTypes.getHealth();
        int expIdx     = assetMap.getIndex("Exp");
        int atkIdx     = assetMap.getIndex("Atk");
        int defIdx     = assetMap.getIndex("Def");
        int spAtkIdx   = assetMap.getIndex("SpAtk");
        int spDefIdx   = assetMap.getIndex("SpDef");
        int speIdx     = assetMap.getIndex("Spd");

        int calcHp = pkmnStats.calcEffectiveStat(0);
        stats.putModifier(healthIdx, "NPC_Max",
            new StaticModifier(ModifierTarget.MAX, CalculationType.ADDITIVE, calcHp - 100));

        if (atkIdx   >= 0) stats.setStatValue(atkIdx,   (float) pkmnStats.calcEffectiveStat(1));
        if (defIdx   >= 0) stats.setStatValue(defIdx,   (float) pkmnStats.calcEffectiveStat(2));
        if (spAtkIdx >= 0) stats.setStatValue(spAtkIdx, (float) pkmnStats.calcEffectiveStat(3));
        if (spDefIdx >= 0) stats.setStatValue(spDefIdx, (float) pkmnStats.calcEffectiveStat(4));
        if (speIdx   >= 0) stats.setStatValue(speIdx,   (float) pkmnStats.calcEffectiveStat(5));

        int level = pkmnStats.getLevel();
        int expMax = level * level * level;
        if (expIdx >= 0) stats.putModifier(expIdx, "Pkmn_Exp_Max",
            new StaticModifier(ModifierTarget.MAX, CalculationType.ADDITIVE, expMax - 1000));
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
}
