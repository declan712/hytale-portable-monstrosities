package dw.portablemonstrosities.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent.Backpack;
import com.hypixel.hytale.server.core.inventory.InventoryComponent.Hotbar;
import com.hypixel.hytale.server.core.inventory.InventoryComponent.Storage;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import au.ellie.hyui.builders.HudBuilder;
import au.ellie.hyui.builders.HyUIHud;

import dw.portablemonstrosities.components.PkmnCaptureMetadata;
import dw.portablemonstrosities.ui.PkmnPartySlot;

/**
 * Handles all UI-related logic for the Pokémon party HUD.
 * Separated from interaction logic for better maintainability.
 */
public class PkmnPartyHUD {

    // ── Layout constants ─────────────────────────────────────────────────────

    private static final String UI_FILE          = "PkmnParty.ui";
    private static final int    MAX_SLOTS        = 6;
    private static final String SLOT_HEALTHY     = "UI/Custom/Pages/Pkmn/Pokeball.png";
    private static final String SLOT_ACTIVE      = "UI/Custom/Pages/Pkmn/Pokeball_Grey.png";
    private static final String SLOT_DEAD        = "UI/Custom/Pages/Pkmn/Pokeball_Dead.png";
    private static final String SLOT_EMPTY       = "UI/Custom/Pages/Pkmn/Pokeball_None.png";
    private static final int    ROW_HEIGHT        = 48;
    private static final int    PADDING           = 52;
    private static final int    HUD_BASE_HEIGHT   = 340;

    // ── Shared per-player state ──────────────────────────────────────────────

    /** Last-known party snapshot per player key, used for diffing on refresh. */
    public static final Map<String, List<PkmnPartySlot>> partyCache   = new HashMap<>();
    /** Live HUD handle per player key – present means HUD is currently open. */
    public static final Map<String, HyUIHud>             hudCache     = new HashMap<>();
    /** Builder per player key – retained so editElement calls accumulate. */
    public static final Map<String, HudBuilder>          builderCache = new HashMap<>();

    // ── Entry point: create and show HUD ─────────────────────────────────────

    /**
     * Creates and shows the Pokémon party HUD for a player.
     * @param context The interaction context containing player and world references
     * @param hotbar The player's hotbar inventory component
     * @param storage The player's storage inventory component
     * @param backpack The player's backpack inventory component
     * @return The created HyUIHud instance, or null if creation failed
     */
    public static HyUIHud createAndShowHud(
        @Nonnull InteractionContext context,
        Hotbar hotbar,
        Storage storage,
        Backpack backpack
    ) {
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        if (commandBuffer == null) { return null; }

        Ref<EntityStore> ref = context.getOwningEntity();
        if (ref == null) { return null; }

        Store<EntityStore> store = commandBuffer.getExternalData().getStore();
        World world = commandBuffer.getExternalData().getWorld();
        if (store == null || world == null) { return null; }

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) { return null; }

        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) { return null; }

        final String playerKey = playerRef.toString();

        // Build the initial party snapshot before showing the HUD
        List<PkmnPartySlot> initialParty = buildPartyFromInventory(
            store, ref, world, hotbar, storage, backpack);

        // Build and show the HUD
        HudBuilder hud = HudBuilder.hudForPlayer(playerRef)
            .fromUIFile("Pages/" + UI_FILE)
            .enablePersistentElementEdits(true)
            .withRefreshRate(1000);

        setSlots(hud, initialParty);

        hud.onRefresh(hyUIHud -> {
            world.execute(() -> {
                List<PkmnPartySlot> oldParty = partyCache.getOrDefault(playerKey, new ArrayList<>());
                List<PkmnPartySlot> newParty = buildPartyFromInventory(store, ref, world, hotbar, storage, backpack);
                updateSlots(hud, newParty, oldParty, playerKey);
                hyUIHud.update(hud);

                UICommandBuilder uiCommandBuilder = new UICommandBuilder();
                hyUIHud.build(uiCommandBuilder);
                hyUIHud.update(true, uiCommandBuilder);
            });
        });

        builderCache.put(playerKey, hud);
        HyUIHud liveHud = hud.show();
        hudCache.put(playerKey, liveHud);

        return liveHud;
    }

    /**
     * Removes and closes the HUD for a player.
     * @param context The interaction context containing player reference
     */
    public static void closeHud(@Nonnull InteractionContext context) {
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        if (commandBuffer == null) { return; }

        Ref<EntityStore> ref = context.getOwningEntity();
        if (ref == null) { return; }

        Store<EntityStore> store = commandBuffer.getExternalData().getStore();
        World world = commandBuffer.getExternalData().getWorld();
        if (store == null || world == null) { return; }

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) { return; }

        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) { return; }

        final String playerKey = playerRef.toString();

        HyUIHud existingHud = hudCache.getOrDefault(playerKey, null);
        if (existingHud != null) {
            existingHud.remove();
            hudCache.put(playerKey, null);
        }
    }

    // ── Slot rendering helpers ────────────────────────────────────────────────

    /**
     * Full (re-)render of all slots – used when the HUD is first shown.
     */
    private static void setSlots(
        @Nonnull HudBuilder hud,
        @Nonnull List<PkmnPartySlot> party
    ) {
        for (int i = 1; i <= MAX_SLOTS; i++) {
            final int fi = i;

            final String idPkmn   = "#Pkmn" + fi + ".Visible";
            final String idName   = "#Pkmn" + fi + " #PkmnName.Text";
            final String idLevel  = "#Pkmn" + fi + " #PkmnLevel.Text";
            final String idHpBar  = "#Pkmn" + fi + " #HealthBar.Value";
            final String idHpText = "#Pkmn" + fi + " #HealthText.Text";
            final String idIcon   = "#Pkmn" + fi + " #PkmnIcon.AssetPath";
            final String idBall   = "#Balls[" + (fi - 1) + "].AssetPath";

            if (party.size() < fi) {
                hud.editElement(cmds -> cmds.set(idPkmn, false))
                    .editElement(cmds -> cmds.set(idBall, SLOT_EMPTY));
                continue;
            }

            hud.editElement(cmds -> cmds.set(idPkmn, true));

            PkmnPartySlot pkmn = party.get(fi - 1);

            final String name     = pkmn.name;
            final String level    = "Lv." + pkmn.level;
            final float  pctHp    = Math.max(0f, Math.min(1f,
                (float) pkmn.currentHp / (float) pkmn.maxHp));
            final String hpText   = pkmn.currentHp + "/" + pkmn.maxHp;
            final String icon     = pkmn.iconPath;

            if(pkmn.fainted){       hud.editElement(cmds -> cmds.set(idBall, SLOT_DEAD));}
            else if (pkmn.active){  hud.editElement(cmds -> cmds.set(idBall, SLOT_ACTIVE));}
            else {                  hud.editElement(cmds -> cmds.set(idBall, SLOT_HEALTHY));}

            hud.editElement(cmds -> cmds.set(idName,   name))
                .editElement(cmds -> cmds.set(idLevel,  level))
                .editElement(cmds -> cmds.set(idHpBar,  pctHp))
                .editElement(cmds -> cmds.set(idHpText, hpText))
                .editElement(cmds -> cmds.set(idIcon,   icon));
        }
    }

    /**
     * Differential update – only pushes changes since the last refresh.
     */
    public static void updateSlots(
        @Nonnull HudBuilder hud,
        @Nonnull List<PkmnPartySlot> party,
        @Nonnull List<PkmnPartySlot> oldParty,
        @Nonnull String playerKey
    ) {
        if (partiesEqual(oldParty, party)) return;

        final int partySize    = party.size();
        final int oldPartySize = oldParty.size();

        if (partySize != oldPartySize) {
            final int resize = PADDING + partySize * ROW_HEIGHT;
            hud.editElement(cmds -> cmds.set(
                "#Container.ResizerSize", HUD_BASE_HEIGHT - resize));
        }

        for (int i = 1; i <= MAX_SLOTS; i++) {
            final int fi = i;

            final String idPkmn   = "#Pkmn" + fi + ".Visible";
            final String idName   = "#Pkmn" + fi + " #PkmnName.Text";
            final String idLevel  = "#Pkmn" + fi + " #PkmnLevel.Text";
            final String idHpBar  = "#Pkmn" + fi + " #HealthBar.Value";
            final String idHpText = "#Pkmn" + fi + " #HealthText.Text";
            final String idIcon   = "#Pkmn" + fi + " #PkmnIcon.AssetPath";
            final String idBall   = "#Balls[" + (fi - 1) + "].AssetPath";

            if (partySize < fi) {
                if (oldPartySize >= fi) {
                    hud.editElement(cmds -> cmds.set(idPkmn, false))
                       .editElement(cmds -> cmds.set(idBall, SLOT_EMPTY));
                }
                continue;
            } else if (oldPartySize < fi) {
                hud.editElement(cmds -> cmds.set(idPkmn, true));
            }

            final PkmnPartySlot pkmn = party.get(fi - 1);
            final PkmnPartySlot prev = (oldPartySize >= fi)
                ? oldParty.get(fi - 1)
                : PkmnPartySlot.of("", 0, 0, 0, "", "");

            if (pkmn.equals(prev)) continue;

            if(pkmn.fainted && !prev.fainted){
                hud.editElement(cmds -> cmds.set(idBall, SLOT_DEAD));}
            else if (pkmn.active && !prev.active){
                hud.editElement(cmds -> cmds.set(idBall, SLOT_ACTIVE));}
            else if (!pkmn.fainted && !pkmn.active && (prev.active || prev.fainted)){ 
                hud.editElement(cmds -> cmds.set(idBall, SLOT_HEALTHY));
            }

            if (!pkmn.name.equals(prev.name))
                hud.editElement(cmds -> cmds.set(idName, pkmn.name));

            if (pkmn.level != prev.level) {
                final String lvlStr = "Lv." + pkmn.level;
                hud.editElement(cmds -> cmds.set(idLevel, lvlStr));
            }

            if (pkmn.currentHp != prev.currentHp || pkmn.maxHp != prev.maxHp) {
                final float  pctHp  = Math.max(0f, Math.min(1f,
                    (float) pkmn.currentHp / (float) pkmn.maxHp));
                final String hpText = pkmn.currentHp + "/" + pkmn.maxHp;
                hud.editElement(cmds -> cmds.set(idHpBar,  pctHp))
                   .editElement(cmds -> cmds.set(idHpText, hpText));
            }

            if (!pkmn.iconPath.equals(prev.iconPath))
                hud.editElement(cmds -> cmds.set(idIcon, pkmn.iconPath));
        }
        partyCache.put(playerKey, party);
    }

    // ── Inventory scanning delegation ─────────────────────────────────────────

    /**
     * Builds a party list from the player's inventory.
     * @param store The player's entity store
     * @param ref The player's entity reference
     * @param world The current world
     * @param hotbar The hotbar inventory component
     * @param storage The storage inventory component
     * @param backpack The backpack inventory component
     * @return A list of PkmnPartySlot representing the party (max 6)
     */
    public static List<PkmnPartySlot> buildPartyFromInventory(
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull World world,
        Hotbar hotbar,
        Storage storage,
        Backpack backpack
    ) {
        List<PkmnPartySlot> party = new ArrayList<>();
        if (hotbar   != null)                     collectFromContainer(hotbar.getInventory(),   party, world);
        if (storage  != null && party.size() < 6) collectFromContainer(storage.getInventory(),  party, world);
        if (backpack != null && party.size() < 6) collectFromContainer(backpack.getInventory(), party, world);
        return party;
    }

    /**
     * Collects party members from a single inventory container.
     */
    public static void collectFromContainer(
        @Nonnull ItemContainer container,
        @Nonnull List<PkmnPartySlot> party,
        @Nonnull World world
    ) {
        container.forEach((slot, itemStack) -> {
            if (party.size() >= 6) return;
            if (PkmnPartyUtil.isValidPartyItem(itemStack.getItemId())) {
                PkmnPartySlot s = PkmnPartyUtil.tryExtractSlot(itemStack, world);
                if (s != null) party.add(s);
            }
        });
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    public static boolean partiesEqual(List<PkmnPartySlot> a, List<PkmnPartySlot> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).equals(b.get(i))) return false;
        }
        return true;
    }
}