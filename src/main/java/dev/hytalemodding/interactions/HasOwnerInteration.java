package dev.hytalemodding.interactions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import dev.hytalemodding.components.PkmnStatsComponent;

import javax.annotation.Nonnull;

/**
 * HasOwnerInteration<br>
 *<br>
 * Use before selecting capture path<br>
 *<br>
 *   Finished → target has an ownerUuid<br>
 *   Failed   → target is wild creature<br>
 * <br>
 * TODO: check if _Tamed?<br>
 *<br>
 */
public class HasOwnerInteration extends SimpleInstantInteraction {

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final BuilderCodec<HasOwnerInteration> CODEC = BuilderCodec.builder(
        HasOwnerInteration.class,
        HasOwnerInteration::new,
        SimpleInstantInteraction.CODEC
    ).build();

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

        PkmnStatsComponent stats = store.getComponent(
            targetRef, PkmnStatsComponent.getComponentType());

        boolean owned = stats != null
            && stats.getOwnerUuid() != null
            && !stats.getOwnerUuid().isBlank();

        if (owned) {
            context.getState().state = InteractionState.Finished;
        } else {
            fail(context);
        }
    }

    private static void fail(@Nonnull InteractionContext context) {
        context.getState().state = InteractionState.Failed;
    }
}
