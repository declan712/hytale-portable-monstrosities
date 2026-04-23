package dev.hytalemodding.interactions;

import java.util.ArrayList;
import javax.annotation.Nonnull;
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
import com.hypixel.hytale.server.core.modules.entity.damage.*;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.DamageEntityInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.combat.DamageCalculator;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.combat.DamageEffects;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.components.PkmnStatsComponent;
import dev.hytalemodding.components.PkmnStatsComponent.PkmnStat;
import dev.hytalemodding.util.PkmnStatUtils;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;

public class DamagePkmnEntityInteraction extends SimpleInstantInteraction{

    protected DamageCalculator damageCalculator;
    protected DamageEffects damageEffects;
    protected boolean isPhysical;


    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final BuilderCodec<DamagePkmnEntityInteraction> CODEC = BuilderCodec.builder(
        DamagePkmnEntityInteraction.class, 
        DamagePkmnEntityInteraction::new, 
        SimpleInstantInteraction.CODEC
    )
    .appendInherited(
        new KeyedCodec<>("DamageCalculator", DamageCalculator.CODEC),
        (o, v) -> o.damageCalculator = v,
        (o)    -> o.damageCalculator,
        (o, p) -> o.damageCalculator = p.damageCalculator
    )
    .add()
    .appendInherited(
        new KeyedCodec<>("DamageEffects", DamageEffects.CODEC),
        (o, v) -> o.damageEffects = v,
        (o)    -> o.damageEffects,
        (o, p) -> o.damageEffects = p.damageEffects
    )
    .add()
    .appendInherited(new KeyedCodec<>("IsPhysical", Codec.BOOLEAN),
        (o, v) -> o.isPhysical = v,
        (o)    -> o.isPhysical,
        (o, p) -> o.isPhysical = p.isPhysical
    )
    .add()
    .build();

    // static {
    //     CODEC = BuilderCodec.builder(
    //         DamagePkmnEntityInteraction.class, 
    //         DamagePkmnEntityInteraction::new, 
    //         SimpleInstantInteraction.CODEC
    //     )
    //         .appendInherited(
    //             new KeyedCodec<>("AcceptedNpcGroups", NPCGroup.CHILD_ASSET_CODEC_ARRAY),
    //             (o, v) -> o.acceptedNpcGroupIds = v,
    //             (o)    -> o.acceptedNpcGroupIds,
    //             (o, p) -> o.acceptedNpcGroupIds = p.acceptedNpcGroupIds
    //         )
    //         .addValidator(NPCGroup.VALIDATOR_CACHE.getArrayValidator())
    //         .add()
    //         .appendInherited(
    //             new KeyedCodec<>("FullIcon", Codec.STRING),
    //             (o, v) -> o.fullIcon = v,
    //             (o)    -> o.fullIcon,
    //             (o, p) -> o.fullIcon = p.fullIcon
    //         )
    //         .add()
    //         .appendInherited(
    //             new KeyedCodec<>("CaptureItemId", Codec.STRING),
    //             (o, v) -> o.captureItemId = v,
    //             (o)    -> o.captureItemId,
    //             (o, p) -> o.captureItemId = p.captureItemId
    //         )
    //         .add()
    //         .afterDecode(captureData -> {
    //             if (captureData.acceptedNpcGroupIds != null) {
    //                 captureData.acceptedNpcGroupIndexes = new int[captureData.acceptedNpcGroupIds.length];
    //                 for (int i = 0; i < captureData.acceptedNpcGroupIds.length; i++) {
    //                     int assetIdx = NPCGroup.getAssetMap().getIndex(captureData.acceptedNpcGroupIds[i]);
    //                     captureData.acceptedNpcGroupIndexes[i] = assetIdx;
    //                 }
    //             }
    //         })
    //         .build();
    // }






    @Override
    protected void firstRun(
        @Nonnull InteractionType interactionType,
        @Nonnull InteractionContext context,
        @Nonnull CooldownHandler cooldownHandler
    ) {
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();

        if (commandBuffer == null) {
            // LOGGER.atInfo().log("CommandBuffer is null");
            fail(context);
            return;
        }

        World world = commandBuffer.getExternalData().getWorld();
        Store<EntityStore> store = commandBuffer.getExternalData().getStore();

        Ref<EntityStore> ownerRef   = context.getOwningEntity();
        Ref<EntityStore> targetRef  = context.getTargetEntity();
        Ref<EntityStore> thirdRef   = context.getEntity();

        if(ownerRef == null)  { 
            // LOGGER.atInfo().log("ownerRef is NULL"); 
            fail(context); 
            return; 
        }
        if(targetRef == null) { 
            // LOGGER.atInfo().log("targetRef is NULL"); 
            fail(context); 
            return; 
        }

        // PkmnStatsComponent ownerStats = store.getComponent(ownerRef, PkmnStatsComponent.getComponentType());
        // PkmnStatsComponent targetStats = store.getComponent(targetRef, PkmnStatsComponent.getComponentType());

        PkmnStatsComponent ownerStats = PkmnStatUtils.getPkmnStatsComponent(commandBuffer, ownerRef);
        commandBuffer.putComponent(ownerRef, PkmnStatsComponent.getComponentType(), ownerStats);
        PkmnStatsComponent targetStats = PkmnStatUtils.getPkmnStatsComponent(commandBuffer, targetRef);
        commandBuffer.putComponent(targetRef, PkmnStatsComponent.getComponentType(), targetStats);

        int[] ownerStatArray = PkmnStatUtils.getCurrentStats(commandBuffer, ownerRef);
        int[] targetStatArray = PkmnStatUtils.getCurrentStats(commandBuffer, targetRef);

        if(ownerStatArray == null)  { 
            // LOGGER.atInfo().log("ownerStatArray is NULL");  
            fail(context); 
            return; 
        }
        if(targetStatArray == null) { 
            // LOGGER.atInfo().log("targetStatArray is NULL"); 
            fail(context); 
            return; 
        }
        // NPCEntity attacker = store.getComponent(ownerRef,NPCEntity.getComponentType());
        // if(attacker != null) {
        //     String attackerRole = attacker.getRoleName();
        //     LOGGER.atInfo().log("ATTACKER: "+attackerRole);
        // }
        // NPCEntity defender = store.getComponent(targetRef,NPCEntity.getComponentType());
        // if(defender != null) {
        //     String defenderRole = defender.getRoleName();
        //     LOGGER.atInfo().log("DEFENDER: "+defenderRole);
        // }

        int lvl = ownerStats.getLevel();
        // int ownerAtk = ownerStats.calcEffectiveStat(PkmnStat.ATK.index);
        // int ownerSpAtk = ownerStats.calcEffectiveStat(PkmnStat.SPATK.index);

        // int targetDef = targetStats.calcEffectiveStat(PkmnStat.DEF.index);
        // int targetSpDef = targetStats.calcEffectiveStat(PkmnStat.SPDEF.index);
        int ownerAtk = ownerStatArray[PkmnStat.ATK.index];
        int ownerSpAtk = ownerStatArray[PkmnStat.SPATK.index];


        int targetDef = targetStatArray[PkmnStat.DEF.index];
        int targetSpDef = targetStatArray[PkmnStat.SPDEF.index];

        int atk = isPhysical ? ownerAtk : ownerSpAtk;
        int def = isPhysical ? targetDef : targetSpDef;

        ArrayList<String> ownerActiveEffects = PkmnStatUtils.activeEffects(store, ownerRef);

        Object2FloatMap<DamageCause> calculatedDamage = damageCalculator.calculateDamage(horizontalSpeedMultiplier);
        Damage.EntitySource source = new Damage.EntitySource(ownerRef);

        

        calculatedDamage.forEach((DamageCause cause, Float power) ->{
            boolean stab = PkmnStatUtils.hasSTAB(ownerActiveEffects, cause);
            Integer attackDamage = PkmnStatUtils.damageFormula(
                lvl,atk,def,power,false,false,1.0f,stab,false);
                // LOGGER.atInfo().log("ATTACK: ("+power+" "+cause.getId()+") => "+String.valueOf(attackDamage));
            Damage damage = new Damage(source, cause, attackDamage);
            damageEffects.addToDamage(damage);
            DamageSystems.executeDamage(targetRef, commandBuffer, damage);
        });
        
        context.getState().state = InteractionState.Finished;
        return;
    }


    private void fail(@Nonnull InteractionContext interactionContext){
        interactionContext.getState().state = InteractionState.Failed;
    }

}