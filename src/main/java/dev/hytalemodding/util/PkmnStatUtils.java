package dev.hytalemodding.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
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
import com.hypixel.hytale.server.npc.metadata.CapturedNPCMetadata;

import dev.hytalemodding.components.PkmnCaptureMetadata;
import dev.hytalemodding.components.PkmnStatsComponent;;


public class PkmnStatUtils {
    private static final String SPECIES_ICON_PREFIX = "Icons/Items/Pokeball/";
    private static final String SPECIES_ICON_SUFFIX = ".png";
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();


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


    public static PkmnCaptureMetadata captureMetadata(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> targetRef
    ){
        Store<EntityStore> store = commandBuffer.getExternalData().getStore();
        if (store == null) return null;

        NPCEntity npcComponent = (NPCEntity)
            commandBuffer.getComponent(targetRef, NPCEntity.getComponentType());
        if (npcComponent == null) return null;

        DeathComponent deathComponent = (DeathComponent)
            commandBuffer.getComponent(targetRef, DeathComponent.getComponentType());
        if (deathComponent != null) return null;

        EntityStatMap stats = store.getComponent(targetRef, EntityStatMap.getComponentType());
        if (stats == null) return null;
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
        float currentHp = health.get();
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
            commandBuffer.getComponent(targetRef, PkmnStatsComponent.getComponentType());

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
        return captureMetadata;
    }

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
        String speciesIcon = (roleId != null)
            ? SPECIES_ICON_PREFIX + roleId + SPECIES_ICON_SUFFIX
            : null;

        if (speciesIcon != null) {
            npcMeta.setFullItemIcon(speciesIcon);
        } else if (fullIcon != null) {
            npcMeta.setFullItemIcon(fullIcon);
        }
        return npcMeta;
    }


    // /**
    //  * Create {@link PkmnCaptureMetadata} for given {@code Ref<EntityStore>}<br>
    //  * 
    //  * @param commandBuffer
    //  * @param targetRef
    //  * @return [PkmnCaptureMetadata]
    //  */
    // @Nullable
    // public static PkmnCaptureMetadata captureMetadata(
    //     @Nonnull CommandBuffer<EntityStore> commandBuffer,
    //     @Nonnull Ref<EntityStore> targetRef
    // ) {
    //     // LOGGER.atInfo().log("GetPkmnStats.read");
    //     Store<EntityStore> store = commandBuffer.getExternalData().getStore();
    //     if (store == null) return null;

    //     NPCEntity npcComponent = (NPCEntity)
    //         commandBuffer.getComponent(targetRef, NPCEntity.getComponentType());
    //     if (npcComponent == null) return null;

    //     DeathComponent deathComponent = (DeathComponent)
    //         commandBuffer.getComponent(targetRef, DeathComponent.getComponentType());
    //     if (deathComponent != null) return null;


    //     // -- Get HP ----------
    //     EntityStatMap stats = store.getComponent(targetRef, EntityStatMap.getComponentType());
    //     if (stats == null) return null;
    //     IndexedLookupTableAssetMap<String, EntityStatType> assetMap = EntityStatType.getAssetMap();
    //     int             lvlIdx = assetMap.getIndex("Lvl");
    //     int             expIdx = assetMap.getIndex("Exp");
    //     int             healthIdx = DefaultEntityStatTypes.getHealth();
    //     EntityStatValue health    = stats.get(healthIdx);
    //     EntityStatValue lvl       = stats.get(lvlIdx);
    //     EntityStatValue exp       = stats.get(expIdx);
    //     if (health == null) return null;
    //     float currentHp = health.get();
    //     float maxHp     = health.getMax();
    //     float currentExp = exp.get();
    //     float currentLvl = lvl.get();

        

    //     // -- Get Scale ----------
    //     float scale = 1f;
    //     EntityScaleComponent scaleComponent = store.getComponent(
    //         targetRef, EntityModule.get().getEntityScaleComponentType());
    //     if (scaleComponent != null) scale = scaleComponent.getScale();
    //     // TransformComponent targetTransform = store.getComponent(targetRef, EntityModule.get().getTransformComponentType());
    //     // Vector3d targetPosition = targetTransform.getPosition();

    //     // -- Get PkmnStatsComponent ----------
    //     PkmnStatsComponent pokemonStats = (PkmnStatsComponent)
    //         commandBuffer.getComponent(targetRef, PkmnStatsComponent.getComponentType());

    //     String roleId = NPCPlugin.get().getName(npcComponent.getRoleIndex());
    //     if(pokemonStats == null) {
    //         pokemonStats = new PkmnStatsComponent();
    //         String species = displayNameOf(roleId);
    //         pokemonStats.setBaseStats(PkmnBaseStatList.fromMap(species));
    //         // IVs are randomised by default in PokemonStatsComponent field initialiser
    //     }

    //     // -- Get PkmnCaptureMetadata ----------
    //     PkmnCaptureMetadata captureMetadata = new PkmnCaptureMetadata();
    //     captureMetadata.setCurrentHp(currentHp);
    //     captureMetadata.setMaxHp(maxHp);
    //     captureMetadata.setModelScale(scale);
    //     captureMetadata.setLevel((int)(currentLvl));
    //     captureMetadata.setExperience((long)(currentExp));
    //     if (pokemonStats != null) {
    //         captureMetadata.setBaseStats(pokemonStats.getBaseStats());
    //         captureMetadata.setEvs(pokemonStats.getEvs());
    //         captureMetadata.setIvs(pokemonStats.getIvs());
    //         captureMetadata.setNature(pokemonStats.getNature());
    //         captureMetadata.setNickname(pokemonStats.getNickname());
    //         captureMetadata.setOwnerUuid(pokemonStats.getOwnerUuid());
    //     }
    //     // LOGGER.atInfo().log("GetPkmnStats.captureMetadata: currentExp="+String.valueOf(currentExp));
    //     // LOGGER.atInfo().log("GetPkmnStats.captureMetadata: currentLvl="+String.valueOf(currentLvl));
    //     // LOGGER.atInfo().log("GetPkmnStats.captureMetadata: Lvl="+String.valueOf(captureMetadata.getLevel()));
    //     return captureMetadata;
    // }
    

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
        int lvlIdx     = assetMap.getIndex("Lvl");
        int expIdx     = assetMap.getIndex("Exp");
        int atkIdx     = assetMap.getIndex("Atk");
        int defIdx     = assetMap.getIndex("Def");
        int spAtkIdx   = assetMap.getIndex("SpAtk");
        int spDefIdx   = assetMap.getIndex("SpDef");
        int speIdx     = assetMap.getIndex("Spd");

        int calcHp = pkmnStats.calcEffectiveStat(0);
        stats.putModifier(healthIdx, "NPC_Max",
            new StaticModifier(ModifierTarget.MAX, CalculationType.ADDITIVE, calcHp - 100));

        int level = pkmnStats.getLevel();
        int expMax = level * level * level;

        if (atkIdx   >= 0) stats.setStatValue(atkIdx,   (float) pkmnStats.calcEffectiveStat(1));
        if (defIdx   >= 0) stats.setStatValue(defIdx,   (float) pkmnStats.calcEffectiveStat(2));
        if (spAtkIdx >= 0) stats.setStatValue(spAtkIdx, (float) pkmnStats.calcEffectiveStat(3));
        if (spDefIdx >= 0) stats.setStatValue(spDefIdx, (float) pkmnStats.calcEffectiveStat(4));
        if (speIdx   >= 0) stats.setStatValue(speIdx,   (float) pkmnStats.calcEffectiveStat(5));
        
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

    public static boolean isTamed(@Nonnull String npcRoleId){
        return npcRoleId.endsWith("_Tamed");
    }

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

    @Nonnull
    public static String resolveTameRole(@Nullable String wildRoleId) {
        if (wildRoleId == null || wildRoleId.isBlank()) return "";
        return wildRoleId.endsWith("_Tamed") ? wildRoleId : wildRoleId + "_Tamed";
    }
}
