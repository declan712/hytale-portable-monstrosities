package dev.hytalemodding.ui;

import javax.annotation.Nullable;

/**
 * PkmnPartySlot<br>
 *<br>
 * Immutable snapshot of a single party member's display data.<br>
 * Pass a list of up to 6 of these to {@link PkmnPartyHUD}.<br>
 */
public final class PkmnPartySlot {

    public final String name;
    public final int    level;
    public final int    currentHp;
    public final int    maxHp;
    /** True if the Pokémon has fainted (hp == 0 or status == "Fainted"). */
    public final boolean fainted;
    public final String     iconPath;

    public PkmnPartySlot(
        @Nullable String name,
        int level,
        int currentHp,
        int maxHp,
        boolean fainted,
        String iconPath
    ) {
        this.name      = (name != null && !name.isBlank()) ? name : "???";
        this.level     = Math.max(1, level);
        this.currentHp = Math.max(0, currentHp);
        this.maxHp     = Math.max(1, maxHp);
        this.fainted   = fainted || currentHp <= 0;
        this.iconPath  = iconPath;
    }

    /** Convenience: build a slot from raw captured-metadata values. */
    public static PkmnPartySlot of(String name, int level, float currentHp, float maxHp, String npcStatus,String icon) {
        boolean fainted = "Fainted".equals(npcStatus) || currentHp <= 0;
        return new PkmnPartySlot(name, level, (int) currentHp, (int) maxHp, fainted, icon);
    }
    public boolean equals(PkmnPartySlot other){
        if(other==null)                             return false;
        if(this.currentHp != other.currentHp)       return false;
        if(this.level != other.level)               return false;
        if(this.maxHp != other.maxHp)               return false;
        if(this.fainted != other.fainted)           return false;
        if(!this.name.equals(other.name))           return false;
        if(!this.iconPath.equals(other.iconPath))   return false;
        return true;
    }
}