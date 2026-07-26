package dw.portablemonstrosities.actions;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
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
        LOGGER.atInfo().log("InitPkmnAction");
        if(ref == null || !ref.isValid()) return false;
      
        // Initialize Pokemon stats component
        // CommandBuffer<EntityStore> commandBuffer = null;

        NPCEntity npcEntity = store.getComponent(ref, NPCEntity.getComponentType());
        // PkmnStatsComponent pkmnStats = store.getComponent(ref, PkmnStatsComponent.getComponentType());
        // if (pkmnStats == null) pkmnStats = new PkmnStatsComponent();

        if(npcEntity != null){
            PkmnCaptureMetadata metadata = PkmnStatUtils.captureMetadata(store, ref);
            PkmnStatsComponent pkmnStats = PkmnStatUtils.fromMetadata(metadata);
            if (pkmnStats == null) pkmnStats = new PkmnStatsComponent();

            String roleName = role.getRoleName();
            LOGGER.atInfo().log(roleName);
            boolean isPkmn = PkmnStatUtils.filterByRoleName(roleName);
            if(!isPkmn) { 
                LOGGER.atInfo().log("not a pkmn");
                int initialMaxHealth = role.getInitialMaxHealth();
                int equivLevel = (int) (initialMaxHealth*10)/100 ;
                pkmnStats.setLevel(equivLevel);
            }

            pkmnStats.setShiny(shiny);
            int[] baseStats = {hp,atk,def,spAtk,spDef,spd};

            if (baseStats != null){
                LOGGER.atInfo().log("baseStats = ["
                + String.valueOf(hp)+", "
                + String.valueOf(atk)+", "
                + String.valueOf(def)+", "
                + String.valueOf(spAtk)+", "
                + String.valueOf(spDef)+", "
                + String.valueOf(spd)+"]");
                pkmnStats.setBaseStats(baseStats);
            }
            if (isValidPkmnType(type1)) {
                LOGGER.atInfo().log("type1 = "+type1.toString());
            }
            if (isValidPkmnType(type2)) {
                LOGGER.atInfo().log("type2 = "+type2.toString());
            }

            if (baseStats != null) { pkmnStats.setBaseStats(baseStats); }
            if (isValidPkmnType(type1)) { pkmnStats.setType1(type1); }
            if (isValidPkmnType(type2)) { pkmnStats.setType2(type2); }

            String nature = pkmnStats.getNature();
            if(nature == null || nature.isEmpty()){
                PkmnNature newNature = PkmnNature.random();
                LOGGER.atInfo().log("New Nature: "+newNature.name);
                pkmnStats.setNature(newNature.name);
            }

            PkmnStatUtils.apply(store, ref, pkmnStats);
            if(isPkmn) PkmnStatUtils.setPkmnNameplate(store,ref,roleName,pkmnStats);
            return true;
        }
        LOGGER.atInfo().log("NPC NULL");
        return false;
   }

   private boolean isValidPkmnType(String type) { 
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
