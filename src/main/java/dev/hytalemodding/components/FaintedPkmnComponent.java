package dev.hytalemodding.components;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import dev.hytalemodding.PortableMonstrosities;

public class FaintedPkmnComponent implements Component<EntityStore> {

    private String npcUuid;
    private String npcRoleId;
    // private PkmnStatsComponent pkmnStats;

    public static ComponentType<EntityStore, FaintedPkmnComponent> getComponentType() {
        return PortableMonstrosities.instance().getFaintedPkmnComponent();
    }
    public FaintedPkmnComponent() {
    }

    public FaintedPkmnComponent(FaintedPkmnComponent clone) {
        npcUuid = clone.npcUuid;
        npcRoleId = clone.npcRoleId;
        // pkmnStats = clone.pkmnStats;
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new FaintedPkmnComponent(this);
    }

    public String getNpcUuid(){
        return this.npcUuid;
    }
    public void setNpcUuid(String id){
        this.npcUuid = id;
    }

    public String getNpcRoleId(){
        return this.npcRoleId;
    }
    public void setNpcRoleId(String id){
        this.npcRoleId = id;
    }

    // public PkmnStatsComponent getPkmnStats(){
    //     return this.pkmnStats;
    // }
    // public void setPkmnStats(PkmnStatsComponent stats){
    //     this.pkmnStats = stats;
    // }

    public static final BuilderCodec<FaintedPkmnComponent> CODEC = BuilderCodec
        .builder(FaintedPkmnComponent.class, FaintedPkmnComponent::new)
        .append(new KeyedCodec<>("NpcUuid", Codec.STRING),
                (data, value) -> data.npcUuid = value,
                data -> data.npcUuid)
        .add()
        .append(new KeyedCodec<>("RoleId", Codec.STRING),
                (data, value) -> data.npcRoleId = value,
                data -> data.npcRoleId)
        .add()
        // .append(new KeyedCodec<>("PkmnStats", PkmnStatsComponent.CODEC),
        //         (data, value) -> data.pkmnStats = value,
        //         data -> data.pkmnStats)
        // .add()
        .build();
}
