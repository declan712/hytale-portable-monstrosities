package dw.portablemonstrosities.actions;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import dw.portablemonstrosities.components.PkmnCaptureMetadata;
import dw.portablemonstrosities.components.PkmnStatsComponent;
import dw.portablemonstrosities.pkmn.PkmnNature;
import dw.portablemonstrosities.util.PkmnStatUtils;

public class InitPkmnAction extends ActionBase {
//    protected final int entityEffectId;
//    protected final boolean useTarget;
   protected int level = 5;
   protected String type1 = "NormalType";
   protected String type2 = null;
   protected int hp = 40;
   protected int atk = 40;
   protected int def = 40;
   protected int spAtk = 40;
   protected int spDef = 40;
   protected int spd = 40;
   protected boolean shiny = false;

   public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // public InitPkmnAction(BuilderActionBase builderActionBase) {
    public InitPkmnAction(
        @Nonnull BuilderInitPkmnAction builder, 
        @Nonnull BuilderSupport support
    ) {
        super(builder);
        // this.entityEffectId =   builder.getEntityEffect(support);
        // this.useTarget =        builder.isUseTarget(support);
        this.level =            builder.getLevel(support);
        this.type1 =            builder.getType1(support);
        this.type2 =            builder.getType2(support);
        this.hp =               builder.getHp(support);
        this.atk =              builder.getAtk(support);
        this.def =              builder.getDef(support);
        this.spAtk =            builder.getSpAtk(support);
        this.spDef =            builder.getSpDef(support);
        this.spd =              builder.getSpd(support);
        this.shiny =            builder.isShiny(support);
    }

   public boolean canExecute(
        @Nonnull Ref<EntityStore> ref, 
        @Nonnull Role role, 
        @Nullable InfoProvider sensorInfo, 
        double dt, 
        @Nonnull Store<EntityStore> store
    ) {
        return super.canExecute(ref, role, sensorInfo, dt, store);
        // && 
            // (!this.useTarget || sensorInfo != null && sensorInfo.hasPosition());
    }

   public boolean execute(
        @Nonnull Ref<EntityStore> ref, 
        @Nonnull Role role, 
        @Nonnull InfoProvider sensorInfo, 
        double dt, 
        @Nonnull Store<EntityStore> store
    ) {
        super.execute(ref, role, sensorInfo, dt, store);

        if(ref == null || !ref.isValid()) return false;
      
        NPCEntity npcEntity = store.getComponent(ref, NPCEntity.getComponentType());
        if(npcEntity != null){

            int[] baseStats = {hp,atk,def,spAtk,spDef,spd};

            initPkmn(ref,store,role,type1,type2,baseStats,shiny);
            return true;
            // PkmnCaptureMetadata metadata = PkmnStatUtils.captureMetadata(store, ref);
            // PkmnStatsComponent pkmnStats = PkmnStatUtils.fromMetadata(metadata);
            // if (pkmnStats == null) pkmnStats = new PkmnStatsComponent();

            // String roleName = role.getRoleName();
            // boolean isPkmn = PkmnStatUtils.filterByRoleName(roleName);
            // if(!isPkmn) { 
            //     int initialMaxHealth = role.getInitialMaxHealth();
            //     int equivLevel = (int) (initialMaxHealth*10)/100 ;
            //     pkmnStats.setLevel(equivLevel);
            // }

            // pkmnStats.setShiny(shiny);
            // int[] baseStats = {hp,atk,def,spAtk,spDef,spd};

            // if (baseStats != null) pkmnStats.setBaseStats(baseStats);

            // if (isValidPkmnType(type1)) { pkmnStats.setType1(type1); }
            // if (isValidPkmnType(type2)) { pkmnStats.setType2(type2); }

            // String nature = pkmnStats.getNature();
            // if(nature == null || nature.isEmpty()){
            //     PkmnNature newNature = PkmnNature.random();
            //     pkmnStats.setNature(newNature.name);
            // }

            // PkmnStatUtils.apply(store, ref, pkmnStats);
            // if(isPkmn) PkmnStatUtils.setPkmnNameplate(store,ref,roleName,pkmnStats);
            // return true;
        }
        return false;
   }

   public static void initPkmn(
    @Nonnull Ref<EntityStore> ref, 
    @Nonnull Store<EntityStore> store,
    @Nonnull Role role,
    @Nullable String type1,
    @Nullable String type2,
    @Nullable int[] baseStats,
    @Nullable boolean shiny
   ){
        PkmnStatsComponent pkmnStats = PkmnStatUtils.getPkmnStatsComponent(store, ref);
        if (pkmnStats == null) pkmnStats = new PkmnStatsComponent();

        String roleName = role.getRoleName();
        boolean isPkmn = PkmnStatUtils.filterByRoleName(roleName);
        if(!isPkmn) { 
            int initialMaxHealth = role.getInitialMaxHealth();
            int equivLevel = (int) (initialMaxHealth*10)/100 ;
            pkmnStats.setLevel(equivLevel);
        }

        if(shiny) pkmnStats.setShiny(shiny);

        if (baseStats != null) pkmnStats.setBaseStats(baseStats);

        if (isValidPkmnType(type1)) { pkmnStats.setType1(type1); }
        if (isValidPkmnType(type2)) { pkmnStats.setType2(type2); }

        String nature = pkmnStats.getNature();
        if(nature == null || nature.isEmpty()){
            PkmnNature newNature = PkmnNature.random();
            pkmnStats.setNature(newNature.name);
        }

        PkmnStatUtils.apply(store, ref, pkmnStats);
        if(isPkmn) PkmnStatUtils.setPkmnNameplate(store,ref,roleName,pkmnStats);
   }

    public static void initPlayer(
        @Nonnull Ref<EntityStore> ref, 
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ){
        PkmnStatsComponent pkmnStats = PkmnStatUtils.getPkmnStatsComponent(commandBuffer, ref);
        if (pkmnStats == null) pkmnStats = new PkmnStatsComponent();
        
        PkmnStatUtils.apply(store, commandBuffer, ref, pkmnStats);
    }
   
    public static void initPlayer(
        @Nonnull Ref<EntityStore> ref, 
        @Nonnull Store<EntityStore> store
    ){
        PkmnStatsComponent pkmnStats = PkmnStatUtils.getPkmnStatsComponent(store, ref);
        if (pkmnStats == null) pkmnStats = new PkmnStatsComponent();
        
        PkmnStatUtils.apply(store, ref, pkmnStats);
    }

   public static boolean isValidPkmnType(String type) { 
       if(type == null) return false;
       return PKMN_TYPES.contains(type); 
   }

   public static List<String> PKMN_TYPES = List.of(
       "BugType",
       "DarkType",
       "DragonType",
       "ElectricType",
       "FightingType",
       "FireType",
       "FlyingType",
       "GhostType",
       "GrassType",
       "GroundType",
       "IceType",
       "NormalType",
       "PoisonType",
       "PsychicType",
       "RockType",
       "SteelType",
       "WaterType"
   );
}
