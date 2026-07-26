package dw.portablemonstrosities.pkmn;

import com.google.crypto.tink.subtle.Random;

public class PkmnNature {
    public String name;
    public double[] statModifiers;
    //                                                                                HP    ATK     DEF     SPATK   SPDEF   SPD
    public static PkmnNature Adamant =  new PkmnNature("Adamant",  new double[] { 1,   1.1,      1,    0.9,      1,      1} ); // Adamant    + Attack     - SpAtk
    public static PkmnNature Bashful =  new PkmnNature("Bashful",  new double[] { 1,     1,      1,      1,      1,      1} ); // Bashful    + SpAtk 	    - SpAtk
    public static PkmnNature Bold =     new PkmnNature("Bold",     new double[] { 1,   0.9,    1.1,      1,      1,      1} ); // Bold 	   + Defense    - Attack
    public static PkmnNature Brave =    new PkmnNature("Brave",    new double[] { 1,   1.1,      1,      1,      1,    0.9} ); // Brave 	   + Attack     - Speed
    public static PkmnNature Calm =     new PkmnNature("Calm",     new double[] { 1,   0.9,      1,      1,    1.1,      1} ); // Calm 	   + SpDef 	    - Attack
    public static PkmnNature Careful =  new PkmnNature("Careful",  new double[] { 1,     1,      1,    0.9,    1.1,      1} ); // Careful    + SpDef 	    - SpAtk
    public static PkmnNature Docile =   new PkmnNature("Docile",   new double[] { 1,     1,      1,      1,      1,      1} ); // Docile 	   + Defense    - Defense
    public static PkmnNature Gentle =   new PkmnNature("Gentle",   new double[] { 1,     1,    0.9,      1,    1.1,      1} ); // Gentle 	   + SpDef 	    - Defense
    public static PkmnNature Hardy =    new PkmnNature("Hardy",    new double[] { 1,     1,      1,      1,      1,      1} ); // Hardy 	   + Attack     - Attack
    public static PkmnNature Hasty =    new PkmnNature("Hasty",    new double[] { 1,     1,    0.9,      1,      1,    1.1} ); // Hasty 	   + Speed 	    - Defense
    public static PkmnNature Impish =   new PkmnNature("Impish",   new double[] { 1,     1,    1.1,    0.9,      1,      1} ); // Impish 	   + Defense    - SpAtk
    public static PkmnNature Jolly =    new PkmnNature("Jolly",    new double[] { 1,     1,      1,    0.9,      1,    1.1} ); // Jolly 	   + Speed 	    - SpAtk
    public static PkmnNature Lax =      new PkmnNature("Lax",      new double[] { 1,     1,    1.1,      1,    0.9,      1} ); // Lax 	   + Defense    - SpDef
    public static PkmnNature Lonely =   new PkmnNature("Lonely",   new double[] { 1,   1.1,    0.9,      1,      1,      1} ); // Lonely 	   + Attack     - Defense
    public static PkmnNature Mild =     new PkmnNature("Mild",     new double[] { 1,     1,    0.9,    1.1,      1,      1} ); // Mild 	   + SpAtk 	    - Defense
    public static PkmnNature Modest =   new PkmnNature("Modest",   new double[] { 1,   0.9,      1,    1.1,      1,      1} ); // Modest 	   + SpAtk 	    - Attack
    public static PkmnNature Naive =    new PkmnNature("Naive",    new double[] { 1,     1,      1,      1,    0.9,    1.1} ); // Naive 	   + Speed 	    - SpDef
    public static PkmnNature Naughty =  new PkmnNature("Naughty",  new double[] { 1,   1.1,      1,      1,    0.9,      1} ); // Naughty    + Attack     - SpDef
    public static PkmnNature Quiet =    new PkmnNature("Quiet",    new double[] { 1,     1,      1,    1.1,      1,    0.9} ); // Quiet 	   + SpAtk 	    - Speed
    public static PkmnNature Quirky =   new PkmnNature("Quirky",   new double[] { 1,     1,      1,      1,      1,      1} ); // Quirky 	   + SpDef 	    - SpDef
    public static PkmnNature Rash =     new PkmnNature("Rash",     new double[] { 1,     1,      1,    1.1,    0.9,      1} ); // Rash 	   + SpAtk 	    - SpDef
    public static PkmnNature Relaxed =  new PkmnNature("Relaxed",  new double[] { 1,     1,    1.1,      1,      1,    0.9} ); // Relaxed    + Defense    - Speed
    public static PkmnNature Sassy =    new PkmnNature("Sassy",    new double[] { 1,     1,      1,      1,    1.1,    0.9} ); // Sassy 	   + SpDef 	    - Speed
    public static PkmnNature Serious =  new PkmnNature("Serious",  new double[] { 1,     1,      1,      1,      1,      1} ); // Serious    + Speed 	    - Speed
    public static PkmnNature Timid =    new PkmnNature("Timid",    new double[] { 1,   0.9,      1,      1,      1,    1.1} ); // Timid 	   + Speed 	    - Attack 

    public PkmnNature(
        String name,
        double[] statModifiers
    ){
        this.name = name;
        this.statModifiers = statModifiers;
    }

    public static PkmnNature[] list(){
        return new PkmnNature[]{
            Adamant,
            Bashful,
            Bold,
            Brave,
            Calm,
            Careful,
            Docile,
            Gentle,
            Hardy,
            Hasty,
            Impish,
            Jolly,
            Lax,
            Lonely,
            Mild,
            Modest,
            Naive,
            Naughty,
            Quiet,
            Quirky,
            Rash,
            Relaxed,
            Sassy,
            Serious,
            Timid, 
        };
    }

    public static PkmnNature random(){
        PkmnNature[] list = list();
        int index = Random.randInt(list.length-1);
        return list[index];
    }

    public static PkmnNature fromName(String nature){
        if(nature==null) return Quirky;
        
        switch(nature){
            case "Adamant":
                return Adamant;
            case "Bashful":
                return Bashful;
            case "Bold":
                return Bold;
            case "Brave":
                return Brave;
            case "Calm":
                return Calm;
            case "Careful":
                return Careful;
            case "Docile":
                return Docile;
            case "Gentle":
                return Gentle;
            case "Hardy":
                return Hardy;
            case "Hasty":
                return Hasty;
            case "Impish":
                return Impish;
            case "Jolly":
                return Jolly;
            case "Lax":
                return Lax;
            case "Lonely":
                return Lonely;
            case "Mild":
                return Mild;
            case "Modest":
                return Modest;
            case "Naive":
                return Naive;
            case "Naughty":
                return Naughty;
            case "Quiet":
                return Quiet;
            case "Quirky":
                return Quirky;
            case "Rash":
                return Rash;
            case "Relaxed":
                return Relaxed;
            case "Sassy":
                return Sassy;
            case "Serious":
                return Serious;
            case "Timid":
                return Timid;
            default:
                return Quirky;
        }

    }
}
