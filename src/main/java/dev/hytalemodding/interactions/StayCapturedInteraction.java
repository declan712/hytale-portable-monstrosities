package dev.hytalemodding.interactions;

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
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.Random;

/**
 * StayCapturedInteraction<br>
 *<br>
 * Single roll to prevent wild creature escaping capture<br>
 * Designed for maths to work if run 3 times<br>
 * <br>
 *   Failed     → creature escapes      → break from repeat loop<br>
 *   Finished   → creature contained    → repeat continues<br>
 *<br>
 * Codec fields:<br>
 *   BallBonus  float   Catch-rate multiplier. 1.0 Base / 1.5 Great / 2.0 Ultra.<br>
 *<br>
 * Capture formula:<br>
 *   catchValue  = ((3·maxHp − 2·currentHp) / 3·maxHp) · captureDifficulty · ballBonus<br>
 *   escapeProb  = (1 − catchValue) ^ (attemps * 3/16)<br>
 *      (where attempts = 3)<br>
 *<br>
 * captureDifficulty is read from the "CaptureDifficulty" EntityStat<br>
 * Defaults to 0.45 if not set<br>
 *<br>
 */
public class StayCapturedInteraction extends SimpleInstantInteraction {

    protected float ballBonus = 1.0f;

    private static final Random RNG = new Random();
    public  static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final int attempts = 3;

    public StayCapturedInteraction() {}

    public static final BuilderCodec<StayCapturedInteraction> CODEC = BuilderCodec.builder(
        StayCapturedInteraction.class,
        StayCapturedInteraction::new,
        SimpleInstantInteraction.CODEC
    )
    .appendInherited(
        new KeyedCodec<>("BallBonus", Codec.FLOAT),
        (o, v) -> o.ballBonus = v,
        o      -> o.ballBonus,
        (o, p) -> o.ballBonus = p.ballBonus
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

        Ref<EntityStore> targetRef = context.getTargetEntity();
        if (targetRef == null) { fail(context); return; }

        Store<EntityStore> store = commandBuffer.getExternalData().getStore();
        if (store == null) { fail(context); return; }

        EntityStatMap statMap = store.getComponent(targetRef, EntityStatMap.getComponentType());
        if (statMap == null) { fail(context); return; }

        EntityStatValue healthStat = statMap.get(DefaultEntityStatTypes.getHealth());
        if (healthStat == null) { fail(context); return; }

        float currentHp = healthStat.get();
        float maxHp     = Math.max(healthStat.getMax(), 1f);

        float difficulty = readCaptureDifficulty(statMap);
        float lvl = readLevel(statMap);
        float lvlMultiplier = lvl/100 + 0.5f;

        // Roll
        float escapeProb = lvlMultiplier * computeEscapeProb(currentHp, maxHp, difficulty, ballBonus);
        boolean escapes  = RNG.nextFloat() < escapeProb;

        LOGGER.atInfo().log(String.format(
            "Wobble: hp=%.0f/%.0f diff=%.2f bonus=%.2f escapeProb=%.3f => %s",
            currentHp, maxHp, difficulty, ballBonus, escapeProb,
            escapes ? "ESCAPED" : "CONTAINED"));

        if (escapes) {
            fail(context);
        } else {
            context.getState().state = InteractionState.Finished;
        }
    }

    // ── Formula ───────────────────────────────────────────────────────────────

    private static float computeEscapeProb(
        float currentHp, float maxHp, float difficulty, float ballBonus
    ) {
        float hpFactor   = (3f * maxHp - 2f * currentHp) / (3f * maxHp);
        float catchValue = Math.min(1f, hpFactor * difficulty * ballBonus);
        return (float) Math.pow(1.0 - catchValue, attempts * 3.0 / 16.0);
    }

    private static float readCaptureDifficulty(@Nonnull EntityStatMap statMap) {
        int idx = EntityStatType.getAssetMap().getIndex("CaptureDifficulty");
        if (idx < 0) return 0.45f;
        EntityStatValue v = statMap.get(idx);
        return (v != null) ? Math.min(1f, Math.max(0.01f, v.get())) : 0.45f;
    }

    private static float readLevel(@Nonnull EntityStatMap statMap) {
        int idx = EntityStatType.getAssetMap().getIndex("Lvl");
        if (idx < 0) return 100;
        EntityStatValue v = statMap.get(idx);
        return (v != null) ? Math.min(1, Math.max(1, v.get())) : 100;
    }

    private static void fail(@Nonnull InteractionContext context) {
        context.getState().state = InteractionState.Failed;
    }
}
