package dw.portablemonstrosities.interactions;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;

public class ChanceInteraction extends SimpleInteraction {
    protected float chance=0f;

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final BuilderCodec<ChanceInteraction> CODEC = BuilderCodec.builder(
        ChanceInteraction.class, 
        ChanceInteraction::new, 
        SimpleInteraction.CODEC
    )
    .appendInherited(
        new KeyedCodec<>("Chance", Codec.FLOAT),
        (o, v) -> o.chance = v,
        (o)    -> o.chance,
        (o, p) -> o.chance = p.chance
    )
    .add()
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
            this.firstRun(context);
            super.tick0(firstRun, time, type, context, cooldownHandler);
        }
    }

    protected void firstRun(
        @Nonnull InteractionContext interactionContext
    ) {
        double rand = Math.random();
        if(chance>rand){
            next(interactionContext);
            return;
        }else{
            fail(interactionContext);
            return;
        }
    }

    private void next(@Nonnull InteractionContext interactionContext){
        interactionContext.getState().state = InteractionState.NotFinished;
    }

    private void fail(@Nonnull InteractionContext interactionContext){
        interactionContext.getState().state = InteractionState.Failed;
    }


}