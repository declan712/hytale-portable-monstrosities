package dev.hytalemodding.systems;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.HolderSystem;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import dev.hytalemodding.components.FaintedPkmnComponent;
import dev.hytalemodding.components.PkmnStatsComponent;

public class AddNetworkIdToMyEntitySystem extends HolderSystem<EntityStore> {

    private final ComponentType<EntityStore, PkmnStatsComponent> pkmnStatsComponentType = PkmnStatsComponent.getComponentType();
    private final ComponentType<EntityStore, FaintedPkmnComponent> faintedPkmnComponentType = FaintedPkmnComponent.getComponentType();
    private final ComponentType<EntityStore, NetworkId> networkIdComponentType = NetworkId.getComponentType();
    private final Query<EntityStore> query = Query.and(this.faintedPkmnComponentType, Query.not(this.networkIdComponentType));

    @Override
    public void onEntityAdd(
        @Nonnull Holder<EntityStore> holder, 
        @Nonnull AddReason reason, 
        @Nonnull Store<EntityStore> store
    ) {
        if (!holder.getArchetype().contains(NetworkId.getComponentType())) {
            holder.addComponent(NetworkId.getComponentType(), new NetworkId(store.getExternalData().takeNextNetworkId()));
        }
    }

    @Override
    public void onEntityRemoved(@Nonnull Holder<EntityStore> holder, @Nonnull RemoveReason reason, @Nonnull Store<EntityStore> store) {}

    @Override
    public @Nullable Query<EntityStore> getQuery() {
        return query;
    }
}