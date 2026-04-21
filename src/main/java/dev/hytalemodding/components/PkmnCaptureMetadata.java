package dev.hytalemodding.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * PkmnCaptureMetadata<br>
 *<br>
 * Serialised into the Pokéball ItemStack under the key {@code "PkmnCapture"}.<br>
 * Carries both vanilla entity stats (health, scale) and mod-specific data<br>
 * (base stats, EVs, IVs, nature, nickname, owner, level, experience).<br>
 *<br>
 * All int[6] arrays follow the stat order:<br>
 *      [0] HP<br>
 *      [1] Attack<br>
 *      [2] Defense<br>
 *      [3] Sp.Atk<br>
 *      [4] Sp.Def<br>
 *      [5] Speed<br>
 */
public class PkmnCaptureMetadata {
    public static final String KEY = "PkmnCapture";
    public static final BuilderCodec<PkmnCaptureMetadata> CODEC;
    public static final KeyedCodec<PkmnCaptureMetadata> KEYED_CODEC;

    // vanilla state
    private float currentHp    = 0f;
    private float maxHp = 0f;
    private float modelScale  = 1f;

    private String npcEntityUuid;
    private String npcStatus;

    // mod-specific data
    @Nullable private int[]  baseStats;
    @Nullable private int[]  evs;
    @Nullable private int[]  ivs;
    @Nullable private String nature;
    @Nullable private String nickname;
    @Nullable private String ownerUuid;
    private int  level      = 1;
    private long experience = 0L;

    public PkmnCaptureMetadata() {
        int[] empty = {0,0,0,0,0,0};
        this.currentHp = 10f;
        this.maxHp = 10f;
        this.modelScale = 1f;
        this.baseStats=empty;
        this.evs=empty;
        this.ivs=empty;
        this.nature="";
        this.nickname="";
        this.ownerUuid="";
        this.level=1;
        this.experience=0;
    }

    // get/set
    public float getCurrentHp()             { return currentHp; }
    public void  setCurrentHp(float v)      { this.currentHp = v; }

    public float getMaxHp()          { return maxHp; }
    public void  setMaxHp(float v)   { this.maxHp = v; }

    public float getModelScale()           { return modelScale; }
    public void  setModelScale(float v)    { this.modelScale = v; }

    @Nullable public int[] getBaseStats()         { return baseStats; }
    public void             setBaseStats(int[] v) { this.baseStats = v; }

    @Nullable public int[] getEvs()               { return evs; }
    public void             setEvs(int[] v)        { this.evs = v; }

    @Nullable public int[] getIvs()               { return ivs; }
    public void             setIvs(int[] v)        { this.ivs = v; }

    @Nullable public String getNature()            { return nature; }
    public void              setNature(String v)   { this.nature = v; }

    @Nullable public String getNickname()          { return nickname; }
    public void              setNickname(String v) { this.nickname = v; }

    @Nullable public String getOwnerUuid()             { return ownerUuid; }
    public void              setOwnerUuid(String v)    { this.ownerUuid = v; }

    public int  getLevel()              { return level; }
    public void setLevel(int v)         { this.level = v; }

    public long getExperience()         { return experience; }
    public void setExperience(long v)   { this.experience = v; }

    @Nullable public String getNpcEntityUuid()      { return npcEntityUuid; }
    public void setNpcEntityUuid(String v)          { this.npcEntityUuid = v; }

    @Nullable public String getNpcStatus()           { return npcStatus; }
    public void setNpcStatus(String v)               { this.npcStatus = v; }


    public String toString(){
        return Map.of(
            "currentHp",     currentHp,
            // "maxHp",      maxHp,
            "modelScale",    modelScale,
            "baseStats",     baseStats,
            "evs",           evs,
            "ivs",           ivs,
            "nature",        nature,
            "nickname",      nickname,
            "ownerUuid",     ownerUuid,
            "level",         level,
            "experience",   experience
            // "ballId",        ballEntityUuid,
            // "npcId",         npcEntityUuid,
            // "npcStatus",      npcStatus
        ).toString();
    }

    static{
        CODEC = BuilderCodec.builder(PkmnCaptureMetadata.class, PkmnCaptureMetadata::new)
        // Vanilla stats
        .appendInherited(new KeyedCodec<>("Health",    Codec.FLOAT),
            (o, v) -> o.currentHp = v, 
            o -> o.currentHp,
            (o, p) -> o.currentHp= p.currentHp)
        .add()
        .appendInherited(new KeyedCodec<>("MaxHealth", Codec.FLOAT),
            (o, v) -> o.maxHp = v, 
            o -> o.maxHp, 
            (o, p) -> o.maxHp = p.maxHp)
        .add()
        .appendInherited(new KeyedCodec<>("ModelScale",  Codec.FLOAT),
            (o, v) -> o.modelScale  = v, 
            o -> o.modelScale,  
            (o, p) -> o.modelScale  = p.modelScale)
        .add()
        // Mod stats
        .appendInherited(new KeyedCodec<>("BaseStats", Codec.INT_ARRAY),
            (o, v) -> o.baseStats = v, 
            o -> o.baseStats,
            (o, p) -> o.baseStats = p.baseStats
        ).add()
        .appendInherited(new KeyedCodec<>("Evs", Codec.INT_ARRAY),
            (o, v) -> o.evs = v, 
            o -> o.evs,
            (o, p) -> o.evs = p.evs
        ).add()
        .appendInherited(new KeyedCodec<>("Ivs", Codec.INT_ARRAY),
            (o, v) -> o.ivs = v, 
            o -> o.ivs,
            (o, p) -> o.ivs = p.ivs
        ).add()
        .appendInherited(new KeyedCodec<>("Nature", Codec.STRING),
            (o, v) -> o.nature = v, 
            o -> o.nature,
            (o, p) -> o.nature = p.nature
        ).add()
        .appendInherited(new KeyedCodec<>("Nickname", Codec.STRING),
            (o, v) -> o.nickname = v, 
            o -> o.nickname,
            (o, p) -> o.nickname = p.nickname
        ).add()
        .appendInherited(new KeyedCodec<>("OwnerUuid", Codec.STRING),
            (o, v) -> o.ownerUuid = v,
            o -> o.ownerUuid,
            (o, p) -> o.ownerUuid = p.ownerUuid
        ).add()
        .appendInherited(new KeyedCodec<>("Level", Codec.INTEGER),
            (o, v) -> o.level = v, 
            o -> o.level,
            (o, p) -> o.level = p.level
        ).add()
        .appendInherited(new KeyedCodec<>("Experience", Codec.LONG),
            (o, v) -> o.experience = v, 
            o -> o.experience,
            (o, p) -> o.experience = p.experience
        ).add()
        .appendInherited(new KeyedCodec<>("NpcId", Codec.STRING),
            (o, v) -> o.npcEntityUuid = v,
            o -> o.npcEntityUuid,
            (o, p) -> o.npcEntityUuid = p.npcEntityUuid
        ).add()
        .appendInherited(new KeyedCodec<>("NpcStatus", Codec.STRING),
            (o, v) -> o.npcStatus = v,
            o -> o.npcStatus,
            (o, p) -> o.npcStatus = p.npcStatus
        ).add()
        .build();

        KEYED_CODEC = new KeyedCodec<>("PkmnCapture", CODEC);
    }
}
