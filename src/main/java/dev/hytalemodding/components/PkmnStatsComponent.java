package dev.hytalemodding.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import dev.hytalemodding.PortableMonstrosities;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * PkmnStatsComponent<br>
 *<br>
 * An entity component attached to wild/owned pkmn NPCs that carries all<br>
 * pkmn-specific mutable data.<br>
 *<br>
 * All int[6] arrays follow the stat order:<br>
 * [0] HP<br>
 * [1] Attack<br>
 * [2] Defense<br>
 * [3] Sp.Atk<br>
 * [4] Sp.Def<br>
 * [5] Speed<br>
 * <br>
 * TODO: How much can be done though EntityStatTypes instead?<br>
 */
public class PkmnStatsComponent implements Component<EntityStore> {

    public static ComponentType<EntityStore, PkmnStatsComponent> getComponentType() {
        return PortableMonstrosities.instance().getPkmnStatsComponentType();
    }
    /** HP / Atk / Def / SpA / SpD / Spd */
    private int[] baseStats = new int[6];
    /** 0 - 252 */
    private int[] evs = new int[6];
    /** 0 - 31 */
    private int[] ivs = randomIVs();
    @Nullable
    private String nature;
    @Nullable
    private String nickname;
    /**
     * UUID string of the player who first captured this creature, or null if wild
     */
    @Nullable
    private String ownerUuid;
    private int level = 1;
    private long experience = 0L;
    /**
     * Primary elemental type (e.g. "Fire", "Water", "Grass"). <br>
     * default: "Normal"<br>
     */
    @Nonnull
    private String type1 = "Normal";
    /** Secondary type, can be null */
    @Nullable
    private String type2 = null;

    public PkmnStatsComponent() {
    }

    public PkmnStatsComponent(PkmnStatsComponent clone) {
        baseStats = clone.baseStats;
        evs = clone.evs;
        ivs = clone.ivs;
        nature = clone.nature;
        nickname = clone.nickname;
        ownerUuid = clone.ownerUuid;
        level = clone.level;
        experience = clone.experience;
        type1 = clone.type1;
        type2 = clone.type2;
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new PkmnStatsComponent(this);
    }

    public static final BuilderCodec<PkmnStatsComponent> CODEC = BuilderCodec
            .builder(PkmnStatsComponent.class, PkmnStatsComponent::new)
            .append(new KeyedCodec<>("Level", Codec.INTEGER),
                    (data, value) -> data.level = value,
                    data -> data.level)
            .add()
            .append(new KeyedCodec<>("BaseStats", Codec.INT_ARRAY),
                    (data, value) -> data.baseStats = value,
                    data -> data.baseStats)
            .add()
            .append(new KeyedCodec<>("Evs", Codec.INT_ARRAY),
                    (data, value) -> data.evs = value,
                    data -> data.evs)
            .add()
            .append(new KeyedCodec<>("Ivs", Codec.INT_ARRAY),
                    (data, value) -> data.ivs = value,
                    data -> data.ivs)
            .add()
            .append(new KeyedCodec<>("Nature", Codec.STRING),
                    (data, value) -> data.nature = value,
                    data -> data.nature)
            .add()
            .append(new KeyedCodec<>("Nickname", Codec.STRING),
                    (data, value) -> data.nickname = value,
                    data -> data.nickname)
            .add()
            .append(new KeyedCodec<>("Owner", Codec.STRING),
                    (data, value) -> data.ownerUuid = value,
                    data -> data.ownerUuid)
            .add()
            .append(new KeyedCodec<>("Exp", Codec.LONG),
                    (data, value) -> data.experience = value,
                    data -> data.experience)
            .add()
            .append(new KeyedCodec<>("Type1", Codec.STRING),
                    (data, value) -> data.type1 = (value != null && !value.isEmpty()) ? value : "Normal",
                    data -> data.type1)
            .add()
            .append(new KeyedCodec<>("Type2", Codec.STRING),
                    (data, value) -> data.type2 = (value != null && !value.isEmpty()) ? value : null,
                    data -> data.type2)
            .add()
            .build();


    // get/set

    public int[] getBaseStats() {
        return baseStats;
    }

    public void setBaseStats(int[] v) {
        this.baseStats = v != null ? v : new int[6];
    }

    public int[] getEvs() {
        return evs;
    }

    public void setEvs(int[] v) {
        this.evs = v != null ? v : new int[6];
    }

    public int[] getIvs() {
        return ivs;
    }

    public void setIvs(int[] v) {
        this.ivs = v != null ? v : randomIVs();
    }

    @Nullable
    public String getNature() {
        return nature;
    }

    public void setNature(String v) {
        this.nature = v;
    }

    @Nullable
    public String getNickname() {
        return nickname;
    }

    public void setNickname(String v) {
        this.nickname = v;
    }

    @Nullable
    public String getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(String v) {
        this.ownerUuid = v;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int v) {
        this.level = Math.max(1, v);
    }

    public long getExperience() {
        return experience;
    }

    public void setExperience(long v) {
        this.experience = Math.max(0L, v);
    }

    @Nonnull
    public String getType1() {
        return type1;
    }

    public void setType1(String v) {
        this.type1 = (v != null && !v.isEmpty()) ? v : "Normal";
    }

    @Nullable
    public String getType2() {
        return type2;
    }

    public void setType2(String v) {
        this.type2 = (v != null && !v.isEmpty()) ? v : null;
    }




    /**
    * Returns the effective stat value for the given index using the Gen-III formula.
    */
    public int calcEffectiveStat(int statIndex) {
        if (statIndex < 0 || statIndex >= 6)
            return 0;
        int base = baseStats[statIndex];
        int iv = ivs[statIndex];
        int ev = evs[statIndex];

        if (statIndex == 0) {
            // HP
            return 2 * (int) (((2.0 * base + iv + ev / 4.0) * level) / 100.0) + level + 10;
        } else {
            // Other stats
            return (int) ((((2.0 * base + iv + ev / 4.0) * level) / 100.0) + 5);
        }
    }

    public int[] randomIVs() {
        int[] ivs = {
            (int) Math.round(Math.random() * 31),
            (int) Math.round(Math.random() * 31),
            (int) Math.round(Math.random() * 31),
            (int) Math.round(Math.random() * 31),
            (int) Math.round(Math.random() * 31),
            (int) Math.round(Math.random() * 31)
        };
        return ivs;
    }

    public String toString() {
        return Map.of(
            "baseStats", baseStats,
            "evs", evs,
            "ivs", ivs,
            "nature", nature,
            "nickname", nickname,
            "ownerUuid", ownerUuid,
            "level", level,
            "experience", experience,
            "type1", type1,
            "type2", type2 != null ? type2 : "").toString();
    }
}
