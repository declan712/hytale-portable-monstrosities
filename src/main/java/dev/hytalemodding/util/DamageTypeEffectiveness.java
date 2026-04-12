package dev.hytalemodding.util;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * DamageTypeEffectiveness<br>
 *<br>
 *<br>
 * Multipliers:<br>
 *   0.0  = immune (no damage)<br>
 *   0.5  = not very effective<br>
 *   1.0  = neutral (default – omitted from table)<br>
 *   2.0  = super effective<br>
 *<br>
 * TODO: decided to use EntityEffects for each type instead<br>
 *      each effect gives weakness/resistance instead.<br>
 *      doesnt stack multiplicitively though => problems<br>
 */
public final class DamageTypeEffectiveness {

    // Attacking.put("Defending", mult);
    private static final Map<String, Map<String, Float>> CHART = new HashMap<>();

    static {
        // Normal
        Map<String, Float> normal = new HashMap<>();
        normal.put("Rock",   0.5f);
        normal.put("Ghost",  0.0f);
        normal.put("Steel",  0.5f);
        CHART.put("Normal",   normal);
        CHART.put("Physical", normal);

        // Fire
        Map<String, Float> fire = new HashMap<>();
        fire.put("Fire",   0.5f);
        fire.put("Water",  0.5f);
        fire.put("Grass",  2.0f);
        fire.put("Ice",    2.0f);
        fire.put("Bug",    2.0f);
        fire.put("Rock",   0.5f);
        fire.put("Dragon", 0.5f);
        fire.put("Steel",  2.0f);
        CHART.put("Fire", fire);

        // Water
        Map<String, Float> water = new HashMap<>();
        water.put("Fire",   2.0f);
        water.put("Water",  0.5f);
        water.put("Grass",  0.5f);
        water.put("Ground", 2.0f);
        water.put("Rock",   2.0f);
        water.put("Dragon", 0.5f);
        CHART.put("Water", water);

        // Grass
        Map<String, Float> grass = new HashMap<>();
        grass.put("Fire",    0.5f);
        grass.put("Water",   2.0f);
        grass.put("Grass",   0.5f);
        grass.put("Poison",  0.5f);
        grass.put("Ground",  2.0f);
        grass.put("Flying",  0.5f);
        grass.put("Bug",     0.5f);
        grass.put("Rock",    2.0f);
        grass.put("Dragon",  0.5f);
        grass.put("Steel",   0.5f);
        CHART.put("Grass", grass);

        // Electric
        Map<String, Float> electric = new HashMap<>();
        electric.put("Water",    2.0f);
        electric.put("Electric", 0.5f);
        electric.put("Grass",    0.5f);
        electric.put("Ground",   0.0f);
        electric.put("Flying",   2.0f);
        electric.put("Dragon",   0.5f);
        CHART.put("Electric", electric);

        // Ice
        Map<String, Float> ice = new HashMap<>();
        ice.put("Water",  0.5f);
        ice.put("Grass",  2.0f);
        ice.put("Ice",    0.5f);
        ice.put("Ground", 2.0f);
        ice.put("Flying", 2.0f);
        ice.put("Dragon", 2.0f);
        ice.put("Steel",  0.5f);
        ice.put("Fire",   0.5f);
        CHART.put("Ice", ice);

        // Fighting
        Map<String, Float> fighting = new HashMap<>();
        fighting.put("Normal",   2.0f);
        fighting.put("Ice",      2.0f);
        fighting.put("Poison",   0.5f);
        fighting.put("Flying",   0.5f);
        fighting.put("Psychic",  0.5f);
        fighting.put("Bug",      0.5f);
        fighting.put("Rock",     2.0f);
        fighting.put("Ghost",    0.0f);
        fighting.put("Dark",     2.0f);
        fighting.put("Steel",    2.0f);
        CHART.put("Fighting", fighting);

        // Poison
        Map<String, Float> poison = new HashMap<>();
        poison.put("Grass",  2.0f);
        poison.put("Poison", 0.5f);
        poison.put("Ground", 0.5f);
        poison.put("Bug",    2.0f);
        poison.put("Rock",   0.5f);
        poison.put("Ghost",  0.5f);
        CHART.put("Poison", poison);

        // Ground
        Map<String, Float> ground = new HashMap<>();
        ground.put("Fire",     2.0f);
        ground.put("Electric", 2.0f);
        ground.put("Grass",    0.5f);
        ground.put("Poison",   2.0f);
        ground.put("Flying",   0.0f);
        ground.put("Bug",      0.5f);
        ground.put("Rock",     2.0f);
        ground.put("Steel",    2.0f);
        CHART.put("Ground", ground);

        // Flying
        Map<String, Float> flying = new HashMap<>();
        flying.put("Electric", 0.5f);
        flying.put("Grass",    2.0f);
        flying.put("Fighting", 2.0f);
        flying.put("Bug",      2.0f);
        flying.put("Rock",     0.5f);
        flying.put("Steel",    0.5f);
        CHART.put("Flying", flying);

        // Psychic 
        Map<String, Float> psychic = new HashMap<>();
        psychic.put("Fighting", 2.0f);
        psychic.put("Poison",   2.0f);
        psychic.put("Psychic",  0.5f);
        // Ghost supposed to be immune in Gen I
        psychic.put("Ghost",    0.0f);
        psychic.put("Dark",     0.0f);
        psychic.put("Steel",    0.5f);
        CHART.put("Psychic", psychic);

        // Bug
        Map<String, Float> bug = new HashMap<>();
        bug.put("Fire",     0.5f);
        bug.put("Grass",    2.0f);
        bug.put("Fighting", 0.5f);
        bug.put("Flying",   0.5f);
        bug.put("Psychic",  2.0f);
        bug.put("Ghost",    0.5f);// ?
        // poison in gen 1 ?
        bug.put("Dark",     2.0f);
        bug.put("Steel",    0.5f);
        CHART.put("Bug", bug);

        // Rock
        Map<String, Float> rock = new HashMap<>();
        rock.put("Fire",     2.0f);
        rock.put("Ice",      2.0f);
        rock.put("Fighting", 0.5f);
        rock.put("Ground",   0.5f);
        rock.put("Flying",   2.0f);
        rock.put("Bug",      2.0f);
        rock.put("Steel",    0.5f);
        CHART.put("Rock", rock);

        // Ghost
        Map<String, Float> ghost = new HashMap<>();
        ghost.put("Normal",  0.0f);
        ghost.put("Psychic", 2.0f); 
        ghost.put("Ghost",   2.0f);
        ghost.put("Dark",    0.5f);
        CHART.put("Ghost", ghost);

        // Dragon
        Map<String, Float> dragon = new HashMap<>();
        dragon.put("Dragon", 2.0f);
        dragon.put("Steel",  0.5f);
        CHART.put("Dragon", dragon);

        // Steel
        Map<String, Float> steel = new HashMap<>();
        steel.put("Ice",      2.0f);
        steel.put("Rock",     2.0f);
        steel.put("Fire",     0.5f);
        steel.put("Water",    0.5f);
        steel.put("Electric", 0.5f);
        steel.put("Steel",    0.5f);
        CHART.put("Steel", steel);

        // Dark
        Map<String, Float> dark = new HashMap<>();
        dark.put("Psychic",  2.0f);
        dark.put("Ghost",    2.0f);
        dark.put("Fighting", 0.5f);
        dark.put("Dark",     0.5f);
        CHART.put("Dark", dark);
    }

    private DamageTypeEffectiveness() {}


    public static float getSingleMatchup(String attackKey, String defendingType) {
        if (attackKey == null || defendingType == null) return 1.0f;
        Map<String, Float> row = CHART.get(attackKey);
        if (row == null) return 1.0f;
        return row.getOrDefault(defendingType, 1.0f);
    }

    public static float getMultiplier(String attackKey, String type1, @Nullable String type2) {
        float mult = getSingleMatchup(attackKey, type1);
        if (type2 != null && !type2.isEmpty()) {
            mult *= getSingleMatchup(attackKey, type2);
        }
        return mult;
    }

    public static float getMultiplier(String attackKey,
            dev.hytalemodding.components.PkmnStatsComponent stats) {
        if (stats == null) return 1.0f;
        return getMultiplier(attackKey, stats.getType1(), stats.getType2());
    }
}
