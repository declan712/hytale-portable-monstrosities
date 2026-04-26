package dev.hytalemodding.interactions;

import java.util.ArrayList;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.hitdetection.HitDetectionBuffer;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector4d;
import com.hypixel.hytale.protocol.CombatTextUpdate;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.hypixel.hytale.server.core.modules.entity.damage.*;
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems;
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems.EntityViewer;
import com.hypixel.hytale.server.core.modules.entityui.asset.CombatTextUIComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.none.SelectInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.DamageEntityInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.combat.DamageCalculator;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.combat.DamageEffects;
import com.hypixel.hytale.server.core.universe.PlayerRef;
// import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import dev.hytalemodding.components.PkmnStatsComponent;
import dev.hytalemodding.components.PkmnStatsComponent.PkmnStat;
import dev.hytalemodding.util.PkmnStatUtils;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;

public class DamagePkmnEntityInteraction extends SimpleInteraction{

    protected DamageCalculator damageCalculator;
    protected DamageEffects damageEffects;
    protected boolean isPhysical;


    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final BuilderCodec<DamagePkmnEntityInteraction> CODEC = BuilderCodec.builder(
        DamagePkmnEntityInteraction.class, 
        DamagePkmnEntityInteraction::new, 
        SimpleInteraction.CODEC
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

    @Override
    protected void tick0(
        boolean firstRun, 
        float time, 
        @Nonnull InteractionType type, 
        @Nonnull InteractionContext context, 
        @Nonnull CooldownHandler cooldownHandler
    ) {
        if (context.getState().state == InteractionState.Failed && context.hasLabels()) {
            context.jump(context.getLabel(0));
        }
        if(firstRun) { firstRun(type,context,cooldownHandler); }
    }


    @Override
    protected void simulateTick0(
        boolean firstRun, 
        float time, 
        @Nonnull InteractionType type, 
        @Nonnull InteractionContext context, 
        @Nonnull CooldownHandler cooldownHandler
    ) {
        // Intentionally empty — damage must only be applied on the server tick.
    }



    // @Override
    protected void firstRun(
        @Nonnull InteractionType interactionType,
        @Nonnull InteractionContext context,
        @Nonnull CooldownHandler cooldownHandler
    ) {
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        if (commandBuffer == null) { fail(context);return;}

        Store<EntityStore> store = commandBuffer.getExternalData().getStore();

        Vector4d hit = (Vector4d)context.getMetaStore().getMetaObject(Interaction.HIT_LOCATION);
        if (SelectInteraction.SHOW_VISUAL_DEBUG && hit != null) {
            DebugUtils.addSphere(((EntityStore)commandBuffer.getExternalData()).getWorld(), 
            new Vector3d(hit.x, hit.y, hit.z), 
            new Vector3f(1.0F, 0.0F, 0.0F), 
            (double)0.2F, 5.0F);
        }


        Ref<EntityStore> ownerRef   = context.getOwningEntity();
        Ref<EntityStore> targetRef  = context.getTargetEntity();

        if(ownerRef == null)  { fail(context); return; }
        if(targetRef == null) { fail(context); return; }

        PkmnStatsComponent ownerStats = PkmnStatUtils.getPkmnStatsComponent(commandBuffer, ownerRef);
        if (ownerStats == null) ownerStats = new PkmnStatsComponent();
        PkmnStatUtils.apply(store,commandBuffer,ownerRef,ownerStats);
        commandBuffer.putComponent(ownerRef, PkmnStatsComponent.getComponentType(), ownerStats);

        PkmnStatsComponent targetStats = PkmnStatUtils.getPkmnStatsComponent(commandBuffer, targetRef);
        if (targetStats == null) targetStats = new PkmnStatsComponent();
        PkmnStatUtils.apply(store,commandBuffer,targetRef,targetStats);
        commandBuffer.putComponent(targetRef, PkmnStatsComponent.getComponentType(), targetStats);

        int[] ownerStatArray = PkmnStatUtils.getCurrentStats(commandBuffer, ownerRef);
        int[] targetStatArray = PkmnStatUtils.getCurrentStats(commandBuffer, targetRef);

        if(ownerStatArray == null)  { fail(context); return; }
        if(targetStatArray == null) { fail(context); return; }

        int lvl = ownerStats.getLevel();

        int atk = isPhysical 
            ? ownerStatArray[PkmnStat.ATK.index]
            : ownerStatArray[PkmnStat.SPATK.index];
        int def = isPhysical 
            ? targetStatArray[PkmnStat.DEF.index]
            : targetStatArray[PkmnStat.SPDEF.index];

        ArrayList<String> ownerActiveEffects = PkmnStatUtils.activeEffects(store, ownerRef);

        Object2FloatMap<DamageCause> calculatedDamage = damageCalculator.calculateDamage(horizontalSpeedMultiplier);
        Damage.EntitySource source = new Damage.EntitySource(ownerRef);

        calculatedDamage.forEach((DamageCause cause, Float power) ->{
            boolean stab = PkmnStatUtils.hasSTAB(ownerActiveEffects, cause);
            Integer attackDamage = 
                PkmnStatUtils.damageFormula( lvl,atk,def,power,false,false,1.0f,stab,false);
            Damage damage = new Damage(source, cause, attackDamage);
            damageEffects.addToDamage(damage);
            DamageSystems.executeDamage(targetRef, commandBuffer, damage);
            // LOGGER.atInfo().log("executeDamage: "+String.valueOf(attackDamage));
            updateCombatText(targetRef,store,commandBuffer,damage);
        });
        
        context.getState().state = InteractionState.Finished;
        return;
    }

    private void fail(@Nonnull InteractionContext interactionContext){
        interactionContext.getState().state = InteractionState.Failed;
    }

    private void updateCombatText(
        @Nonnull Ref<EntityStore> targetRef, 
        @Nonnull Store<EntityStore> store, 
        @Nonnull CommandBuffer<EntityStore> commandBuffer, 
        @Nonnull Damage damage
    ) {
        // damage must be >0
        if (!(damage.getAmount() > 0.0F)) return;

        // Damage source must be entity
        Damage.Source damageSource = damage.getSource();
        if (!(damageSource instanceof Damage.EntitySource)) return;

        // Entity ref must be valid
        Damage.EntitySource entitySource = (Damage.EntitySource)damageSource;
        Ref<EntityStore> sourceRef = entitySource.getRef();
        if (!sourceRef.isValid()) return;

        // Entity must be player
        // TODO: show if either source or target is player or Tamed
        boolean shouldShowCombatText = false;

        PlayerRef sourcePlayerRef = (PlayerRef)commandBuffer.getComponent(sourceRef, PlayerRef.getComponentType());
        PlayerRef targetPlayerRef = (PlayerRef)commandBuffer.getComponent(targetRef, PlayerRef.getComponentType());

        boolean isPlayerSource = sourcePlayerRef!=null  && sourcePlayerRef.isValid();
        boolean isPlayerTarget = targetPlayerRef!=null  && targetPlayerRef.isValid();

        NPCEntity sourceNpc = commandBuffer.getComponent(sourceRef, NPCEntity.getComponentType());
        NPCEntity targetNpc = commandBuffer.getComponent(targetRef, NPCEntity.getComponentType());

        boolean isTamedSource = sourceNpc!=null && sourceNpc.getRoleName().endsWith("_Tamed");
        boolean isTamedTarget = targetNpc!=null && targetNpc.getRoleName().endsWith("_Tamed");

        shouldShowCombatText = isPlayerTarget || isTamedSource || isTamedTarget;


        if (!shouldShowCombatText) return;

        // player must have EntityViewer
        EntityTrackerSystems.EntityViewer entityViewerComponent;
        // if(isPlayerSource) {
        //     entityViewerComponent= commandBuffer.getComponent(sourceRef, EntityViewer.getComponentType());
        //     LOGGER.atInfo().log("use sources entityViewerComponent");
        // } else 
        if (isPlayerTarget) {
            entityViewerComponent = commandBuffer.getComponent(targetRef, EntityViewer.getComponentType());
            // LOGGER.atInfo().log("use targets entityViewerComponent");
            entityViewerComponent.visible.add(targetRef);
            commandBuffer.putComponent(targetRef, EntityViewer.getComponentType(),entityViewerComponent);

        } else if (isTamedSource){
            // LOGGER.atInfo().log("use sources owners entityViewerComponent");
            Ref<EntityStore> owner = getOwnerRef(sourceRef,store,commandBuffer);
            if (owner == null) return;

            entityViewerComponent = commandBuffer.getComponent(owner, EntityViewer.getComponentType());
            Player ownerPlayer = store.getComponent(owner, Player.getComponentType());
            if (ownerPlayer == null) return;

            String ownerName = ownerPlayer.getDisplayName();
            // LOGGER.atInfo().log("show damage text to owner: "+ownerName);

        } else if (isTamedTarget){
            // LOGGER.atInfo().log("use targets owners entityViewerComponent");
            Ref<EntityStore> owner = getOwnerRef(targetRef,store,commandBuffer);
            if (owner == null) return;

            entityViewerComponent = commandBuffer.getComponent(owner, EntityViewer.getComponentType());
            Player ownerPlayer = store.getComponent(owner, Player.getComponentType());
            if (ownerPlayer == null) return;

            String ownerName = ownerPlayer.getDisplayName();
            // LOGGER.atInfo().log("show damage text to owner: "+ownerName);
        } else {
            return;
        }

        if (entityViewerComponent == null) return;

        // Update combat text
        Float hitAngleDeg = damage.getIfPresentMetaObject(Damage.HIT_ANGLE);
        queueUpdateFor(targetRef, damage.getAmount(), hitAngleDeg, entityViewerComponent);
    }

    private static Ref<EntityStore> getOwnerRef(
        @Nonnull Ref<EntityStore> ref, 
        @Nonnull Store<EntityStore> store, 
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        PkmnStatsComponent pkmnStats = commandBuffer.getComponent(ref,PkmnStatsComponent.getComponentType());
        if (pkmnStats == null) { 
            // LOGGER.atInfo().log("pkmnStats NULL => no owner"); 
            return null;
        }
        String ownerId = pkmnStats.getOwnerUuid();
        if (ownerId == null)  { 
            // LOGGER.atInfo().log("ownerUuid NULL"); 
            return null;
        }
        Ref<EntityStore> ownerRef = store.getExternalData().getRefFromUUID(UUID.fromString(ownerId));
        if(ownerRef == null) {
            // LOGGER.atInfo().log("ownerREf NULL"); 
            return null;
        }
        Player ownerPlayer = store.getComponent(ownerRef, Player.getComponentType());
        if (ownerPlayer == null){
            // LOGGER.atInfo().log("Owner not a player"); 
            return null;
        }
        String ownerName = ownerPlayer.getDisplayName();
        // LOGGER.atInfo().log("Owner is "+ownerName); 
        return ownerRef;


    }


    private static void queueUpdateFor(
        @Nonnull Ref<EntityStore> ref, 
        float damageAmount, 
        @Nullable Float hitAngleDeg, 
        @Nonnull EntityTrackerSystems.EntityViewer viewer
    ) {
        CombatTextUIComponent combatText = new CombatTextUIComponent();
        CombatTextUpdate update = new CombatTextUpdate(hitAngleDeg == null ? 0.0F : hitAngleDeg, Integer.toString((int)Math.floor((double)damageAmount)));
        viewer.queueUpdate(ref, update);
    }



}



