package dw.portablemonstrosities.interactions;

import java.util.List;

import javax.annotation.Nonnull;

import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import dw.portablemonstrosities.components.PkmnCaptureMetadata;
import dw.portablemonstrosities.components.PkmnStatsComponent;
import dw.portablemonstrosities.util.PkmnStatUtils;

public class InitPkmnInteraction extends SimpleInteraction {
    protected int level = 5;
    protected String type1 = "NormalType";
    protected String type2 = null;
    // protected int[] baseStats = null;
    protected int hp = 40; //"Hp"
    protected int atk = 40; //"Atk"
    protected int def = 40; //"Def"
    protected int spAtk = 40; //"SpAtk"
    protected int spDef = 40; //"SpDef"
    protected int spd = 40; //"Spd"
    protected boolean shiny = false;

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

    // to set:
    // evs
    // ivs
    // capture diff
    // types
    // moves
    // shiny
    // nature

    
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();


    // public static void validatePkmnType(String type,ValidationResults res){
    //     if(!PKMN_TYPES.contains(type)){
    //         res.fail("Invalid Pkmn Type");
    //     }
    // }
    public static boolean isValidPkmnType(String type) { 
        if(type == null) return false;
        return PKMN_TYPES.contains(type); 
    }

    public static final BuilderCodec<InitPkmnInteraction> CODEC = BuilderCodec.builder(
        InitPkmnInteraction.class, 
        InitPkmnInteraction::new, 
        SimpleInteraction.CODEC
    )
    .appendInherited(
        new KeyedCodec<>("Type1", Codec.STRING),
        (o, v) -> o.type1 = v,
        (o)    -> o.type1,
        (o, p) -> o.type1 = p.type1
    )
    // .addValidator((arg0, arg1) -> validatePkmnType(arg0,arg1))
    .add()
    .appendInherited(
        new KeyedCodec<>("Type2", Codec.STRING),
        (o, v) -> o.type2 = v,
        (o)    -> o.type2,
        (o, p) -> o.type2 = p.type2
    )
    .add()
    .appendInherited(
        new KeyedCodec<>("Shiny", Codec.BOOLEAN),
        (o, v) -> o.shiny = v,
        (o)    -> o.shiny,
        (o, p) -> o.shiny = p.shiny
    )
    .add()
    .appendInherited(
        new KeyedCodec<>("Level", Codec.INTEGER),
        (o, v) -> o.level = v,
        (o)    -> o.level,
        (o, p) -> o.level = p.level
    )
    .add()
    .appendInherited(
        new KeyedCodec<>("Hp", Codec.INTEGER),
        (o, v) -> o.hp = v,
        (o)    -> o.hp,
        (o, p) -> o.hp = p.hp
    )
    .add()
    .appendInherited(
        new KeyedCodec<>("Atk", Codec.INTEGER),
        (o, v) -> o.atk = v,
        (o)    -> o.atk,
        (o, p) -> o.atk = p.atk
    )
    .add()
    .appendInherited(
        new KeyedCodec<>("Def", Codec.INTEGER),
        (o, v) -> o.def = v,
        (o)    -> o.def,
        (o, p) -> o.def = p.def
    )
    .add()
    .appendInherited(
        new KeyedCodec<>("SpAtk", Codec.INTEGER),
        (o, v) -> o.spAtk = v,
        (o)    -> o.spAtk,
        (o, p) -> o.spAtk = p.spAtk
    )
    .add()
    .appendInherited(
        new KeyedCodec<>("SpDef", Codec.INTEGER),
        (o, v) -> o.spDef = v,
        (o)    -> o.spDef,
        (o, p) -> o.spDef = p.spDef
    )
    .add()
    .appendInherited(
        new KeyedCodec<>("Spd", Codec.INTEGER),
        (o, v) -> o.spd = v,
        (o)    -> o.spd,
        (o, p) -> o.spd = p.spd
    )
    .add()
    // .appendInherited(
    //     new KeyedCodec<>("BaseStats", Codec.INT_ARRAY),
    //     (o, v) -> o.baseStats = v,
    //     (o)    -> o.baseStats,
    //     (o, p) -> o.baseStats = p.baseStats
    // )
    // .add()
    .build();
     

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
        @Nonnull InteractionType interactionType,
        @Nonnull InteractionContext interactionContext,
        @Nonnull CooldownHandler cooldownHandler
    ) {
        CommandBuffer<EntityStore> commandBuffer = interactionContext.getCommandBuffer();
        if (commandBuffer == null) {
            fail(interactionContext);
            return;
        }
        Store<EntityStore> store = commandBuffer.getExternalData().getStore();
        Ref<EntityStore> ref = interactionContext.getEntity();
        if (ref == null) {
            fail(interactionContext);
            return;
        }

        Player player = commandBuffer.getComponent(ref, Player.getComponentType());

        if(player != null){
            // ── PLAYER ────
            LOGGER.atFinest().log("Running for player");
            // triggered by player level up, not naming NPC
            PkmnStatsComponent pkmnStats = commandBuffer.getComponent(ref, PkmnStatsComponent.getComponentType());
            if (pkmnStats == null) {
                LOGGER.atFinest().log("Current stats NULL");
                pkmnStats = new PkmnStatsComponent();
            }
            if(pkmnStats.getBaseStats() != PkmnStatUtils.PLAYER_BASE_STATS){ 
                LOGGER.atFinest().log("Setting base stats");
                pkmnStats.setBaseStats(PkmnStatUtils.PLAYER_BASE_STATS);
            }
            EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());
            IndexedLookupTableAssetMap<String, EntityStatType> assetMap = EntityStatType.getAssetMap();
            int lvlIdx = assetMap.getIndex("Lvl");
            int expIdx = assetMap.getIndex("Exp");
            EntityStatValue lvl = stats.get(lvlIdx);
            EntityStatValue exp = stats.get(expIdx);
            float currentExp = exp.get();
            float currentLvl = lvl.get();
            pkmnStats.setLevel((int)currentLvl);
            pkmnStats.setExperience((int)currentExp);
            PkmnStatUtils.apply(store,commandBuffer,ref,pkmnStats);
            commandBuffer.putComponent(ref, PkmnStatsComponent.getComponentType(), pkmnStats);
            next(interactionContext);
            return; 
        }

        NPCEntity npcEntity = commandBuffer.getComponent(ref,NPCEntity.getComponentType());
        if(npcEntity==null){ fail(interactionContext); return; }

        // ── NPC ────
        String roleName = npcEntity.getRoleName();
        if(!PkmnStatUtils.filterByRoleName(roleName)) { fail(interactionContext); return; }

        PkmnCaptureMetadata metadata = PkmnStatUtils.captureMetadata(commandBuffer,ref);
        PkmnStatsComponent pkmnStats = PkmnStatUtils.fromMetadata(metadata);

        // ActionApplyEntityEffect

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
            pkmnStats.setType1(type1);
        }
        if (isValidPkmnType(type2)) {
            LOGGER.atInfo().log("type2 = "+type2.toString());
            pkmnStats.setType1(type2);
        }

        PkmnStatUtils.apply(store, commandBuffer, ref, pkmnStats);

        PkmnStatUtils.setPkmnNameplate(commandBuffer,ref,roleName,pkmnStats);
        next(interactionContext);
        return;
    }

    private void next(@Nonnull InteractionContext interactionContext){
        interactionContext.getState().state = InteractionState.NotFinished;
    }

    private void fail(@Nonnull InteractionContext interactionContext){
        interactionContext.getState().state = InteractionState.Failed;
    }
}
