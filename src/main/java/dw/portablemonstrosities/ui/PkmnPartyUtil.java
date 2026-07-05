package dw.portablemonstrosities.ui;

import java.util.List;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.metadata.CapturedNPCMetadata;

import dw.portablemonstrosities.components.PkmnCaptureMetadata;
import dw.portablemonstrosities.util.PkmnStatUtils;

/**
 * Utility class for inventory-related operations for the Pokémon party HUD.
 * Separated from UI logic for better maintainability.
 */
public class PkmnPartyUtil {

    // ── Valid item patterns (same as TogglePkmnHUDInteraction) ───────────────

    private static final List<String> VALID_ITEMS = List.of(
        "*Pokeball_State_Full",
        "*Pokeball_Apricorn_State_Full",
        "*Pokeball_Great_State_Full",
        "*Pokeball_Ultra_State_Full",
        "*Pokeball_Master_State_Full",

        "*Pokeball_State_Active",
        "*Pokeball_Apricorn_State_Active",
        "*Pokeball_Great_State_Active",
        "*Pokeball_Ultra_State_Active",
        "*Pokeball_Master_State_Active",

        "*Pokeball_State_Fainted",
        "*Pokeball_Apricorn_State_Fainted",
        "*Pokeball_Great_State_Fainted",
        "*Pokeball_Ultra_State_Fainted",
        "*Pokeball_Master_State_Fainted"
    );

    // ── Entry point ──────────────────────────────────────────────────────────

    /**
     * Attempts to extract a party slot from an item stack.
     * @param item The item stack to check
     * @param world The current world (needed for active entity lookups)
     * @return A PkmnPartySlot if the item contains a valid Pokémon, null otherwise
     */
    public static PkmnPartySlot tryExtractSlot(ItemStack item, @Nonnull World world) {
        if (item == null) return null;

        final PkmnCaptureMetadata captureMeta = item.getFromMetadataOrNull("PkmnCapture", PkmnCaptureMetadata.CODEC);
        if (captureMeta == null) return null;
        final CapturedNPCMetadata npcMeta = item.getFromMetadataOrNull("CapturedEntity",CapturedNPCMetadata.CODEC);


        final String status = captureMeta.getNpcStatus();
        if (status == null) return null;
        
        final boolean isActive  = "Active".equals(status);
        final boolean isFainted = "Fainted".equals(status);
        final boolean isHealthy = "Healthy".equals(status);
        if (!isActive && !isFainted && !isHealthy) return null;

        String roleId = isActive 
                ? captureMeta.getRoleId()
                : null; // For fainted/healthy, we'll get name from captured entity metadata
        
        final String nickname = captureMeta.getNickname();

        if (roleId == null && npcMeta != null)  roleId = npcMeta.getNpcNameKey();
        if (roleId == null) return null;

        String name = PkmnStatUtils.displayNameOf(roleId);
        if (nickname != null && !nickname.isBlank()) name = nickname;
        
        String wildRole = PkmnStatUtils.getWildRole(roleId);// roleId.replace("_Tamed", "").replace("Tamed_", "");
        String icon     = "Icons/ModelsGenerated/" + wildRole + ".png";

        if (!isActive) {
            return PkmnPartySlot.of(
                name,
                captureMeta.getLevel(),
                captureMeta.getCurrentHp(),
                captureMeta.getMaxHp(),
                status,
                icon
            );
        }

        // Active – read live stats from the spawned entity
        final String npcUuid = captureMeta.getNpcEntityUuid();
        if (npcUuid == null) return null;

        Ref<EntityStore> ref = 
            world.getEntityRef(java.util.UUID.fromString(npcUuid));
        if (ref == null || !ref.isValid()) return null;

        com.hypixel.hytale.component.Store<EntityStore> store = ref.getStore();
        if (store == null || !store.isInThread()) return null;

        var currentState = PkmnStatUtils.getState(store, ref);
        if (currentState == null) return null;

        var level = currentState.getOrDefault("LVL", captureMeta.getLevel());
        var hp    = currentState.getOrDefault("HP",  (int) captureMeta.getCurrentHp());
        var maxHp = currentState.getOrDefault("HP_MAX", (int) captureMeta.getMaxHp());

        return PkmnPartySlot.of(
            name + "*",
            (int) level,
            (int) hp,
            (int) maxHp,
            status,
            icon
        );
    }

    // ── Validation helpers ───────────────────────────────────────────────────

    /**
     * Checks if an item ID represents a valid party member item.
     */
    public static boolean isValidPartyItem(String itemId) {
        return VALID_ITEMS.contains(itemId);
    }
}