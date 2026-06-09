package dw.portablemonstrosities.interactions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent.Backpack;
import com.hypixel.hytale.server.core.inventory.InventoryComponent.Hotbar;
import com.hypixel.hytale.server.core.inventory.InventoryComponent.Storage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.metadata.CapturedNPCMetadata;

import au.ellie.hyui.builders.HudBuilder;
import au.ellie.hyui.builders.HyUIHud;
import dw.portablemonstrosities.components.PkmnCaptureMetadata;
import dw.portablemonstrosities.ui.PkmnPartySlot;

/**
 * TogglePkmnHUDInteraction<br>
 * <br>
 * Attach to an item's Primary or Secondary interaction to show/hide the
 * Pokémon party HUD. A second use of the same item closes the HUD.<br>
 * <br>
 * Mirrors the logic previously in {@code PkmnHudTestCommand} so it can be
 * driven from a normal item interaction instead of a chat command.
 */
public class TogglePkmnHUDInteraction extends SimpleInstantInteraction {

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // ── Shared per-player state (mirrors PkmnHudTestCommand statics) ──────────

    /** Last-known party snapshot per player key, used for diffing on refresh. */
    private static final Map<String, List<PkmnPartySlot>> partyCache   = new HashMap<>();
    /** Live HUD handle per player key – present means HUD is currently open. */
    private static final Map<String, HyUIHud>             hudCache     = new HashMap<>();
    /** Builder per player key – retained so editElement calls accumulate. */
    private static final Map<String, HudBuilder>          builderCache = new HashMap<>();

    // ── Layout constants (same as command) ───────────────────────────────────

    // private static final String HUD_KEY         = "PkmnParty";
    private static final int    MAX_SLOTS        = 6;
    private static final String UI_FILE          = "PkmnParty.ui";
    private static final String SLOT_HEALTHY     = "UI/Custom/Pages/Pokeball.png";
    private static final String SLOT_ACTIVE       = "UI/Custom/Pages/Pokeball_Grey.png";
    private static final String SLOT_DEAD       = "UI/Custom/Pages/Pokeball_Dead.png";
    private static final String SLOT_EMPTY       = "UI/Custom/Pages/Pokeball_None.png";
    private static final int    ROW_HEIGHT        = 48;
    private static final int    PADDING           = 52;
    private static final int    HUD_BASE_HEIGHT   = 340;

    private static final List<String> VALID_ITEMS = List.of(
        "*Pokeball_State_Full",
        "*Pokeball_Apricorn_State_Full",
        "*Pokeball_Great_State_Full",
        "*Pokeball_Ultra_State_Full",
        "*Pokeball_State_Active",
        "*Pokeball_Apricorn_State_Active",
        "*Pokeball_Great_State_Active",
        "*Pokeball_Ultra_State_Active",
        "*Pokeball_State_Fainted",
        "*Pokeball_Apricorn_State_Fainted",
        "*Pokeball_Great_State_Fainted",
        "*Pokeball_Ultra_State_Fainted"
    );

    // ── CODEC ─────────────────────────────────────────────────────────────────

    public static final BuilderCodec<TogglePkmnHUDInteraction> CODEC = BuilderCodec.builder(
        TogglePkmnHUDInteraction.class,
        TogglePkmnHUDInteraction::new,
        SimpleInstantInteraction.CODEC
    ).build();

    // ── Entry point ───────────────────────────────────────────────────────────

    @Override
    protected void firstRun(
        @Nonnull InteractionType type,
        @Nonnull InteractionContext context,
        @Nonnull CooldownHandler cooldownHandler
    ) {
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        if (commandBuffer == null) { fail(context); return; }

        // Owning entity must be a player
        Ref<EntityStore> ref = context.getOwningEntity();
        if (ref == null) { fail(context); return; }

        Store<EntityStore> store = commandBuffer.getExternalData().getStore();
        World world = commandBuffer.getExternalData().getWorld();
        if (store == null || world == null) { fail(context); return; }

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) { fail(context); return; }

        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) { fail(context); return; }

        final String playerKey = playerRef.toString();

        // ── Toggle: close HUD if already open ─────────────────────────────
        HyUIHud existingHud = hudCache.getOrDefault(playerKey, null);
        if (existingHud != null) {
            existingHud.remove();
            hudCache.put(playerKey, null);
            context.getState().state = InteractionState.Finished;
            return;
        }

        // ── Gather inventory references for the refresh callback ──────────
        Hotbar   hotbar   = store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
        Storage  storage  = store.getComponent(ref, InventoryComponent.Storage.getComponentType());
        Backpack backpack = store.getComponent(ref, InventoryComponent.Backpack.getComponentType());

        // Build the initial party snapshot before showing the HUD
        List<PkmnPartySlot> initialParty = buildPartyFromInventory(
            store, ref, world, hotbar, storage, backpack);

        // ── Build HUD ─────────────────────────────────────────────────────
        HudBuilder hud = HudBuilder.hudForPlayer(playerRef)
            .fromUIFile("Pages/" + UI_FILE)
            .enablePersistentElementEdits(true)
            .withRefreshRate(1000);

        setSlots(hud, initialParty);

        hud.onRefresh(hyUIHud -> {
            world.execute(() -> {
                List<PkmnPartySlot> oldParty = partyCache.getOrDefault(
                    playerKey, new ArrayList<>());
                List<PkmnPartySlot> newParty = buildPartyFromInventory(
                    store, ref, world, hotbar, storage, backpack);
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

        context.getState().state = InteractionState.Finished;
    }

    // ── Slot helpers (identical logic to PkmnHudTestCommand) ─────────────────

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
    private static void updateSlots(
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

            if(pkmn.fainted && !prev.fainted){       hud.editElement(cmds -> cmds.set(idBall, SLOT_DEAD));}
            else if (pkmn.active && !prev.active){  hud.editElement(cmds -> cmds.set(idBall, SLOT_ACTIVE));}
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

    // ── Inventory scanning (mirrors PkmnHudTestCommand statics) ──────────────

    private static List<PkmnPartySlot> buildPartyFromInventory(
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull World world,
        Hotbar hotbar,
        Storage storage,
        Backpack backpack
    ) {
        List<PkmnPartySlot> party = new ArrayList<>();
        if (hotbar   != null)                    collectFromContainer(hotbar.getInventory(),   party, world);
        if (storage  != null && party.size() < 6) collectFromContainer(storage.getInventory(),  party, world);
        if (backpack != null && party.size() < 6) collectFromContainer(backpack.getInventory(), party, world);
        return party;
    }

    private static void collectFromContainer(
        @Nonnull ItemContainer container,
        @Nonnull List<PkmnPartySlot> party,
        @Nonnull World world
    ) {
        container.forEach((slot, itemStack) -> {
            if (party.size() >= 6) return;
            if (VALID_ITEMS.contains(itemStack.getItemId())) {
                PkmnPartySlot s = tryExtractSlot(itemStack, world);
                if (s != null) party.add(s);
            }
        });
    }

    private static PkmnPartySlot tryExtractSlot(ItemStack item, @Nonnull World world) {
        if (item == null) return null;

        final PkmnCaptureMetadata captureMeta =
            item.getFromMetadataOrNull("PkmnCapture", PkmnCaptureMetadata.CODEC);
        if (captureMeta == null) return null;

        final CapturedNPCMetadata npcMeta =
            item.getFromMetadataOrNull("CapturedEntity", CapturedNPCMetadata.CODEC);

        final String status = captureMeta.getNpcStatus();
        if (status == null) return null;
        final boolean isActive  = "Active".equals(status);
        final boolean isFainted = "Fainted".equals(status);
        final boolean isHealthy = "Healthy".equals(status);
        if (!isActive && !isFainted && !isHealthy) return null;

        String roleId = isActive 
                ? captureMeta.getRoleId()
                : (npcMeta != null ? npcMeta.getNpcNameKey() : null);
        if (roleId == null) return null;

        String name = dw.portablemonstrosities.util.PkmnStatUtils.displayNameOf(roleId);
        final String nickname = captureMeta.getNickname();
        if (nickname != null && !nickname.isBlank()) name = nickname;

        String wildRole = roleId.replace("_Tamed", "").replace("Tamed_", "");
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

        Ref<EntityStore> ref = world.getEntityRef(java.util.UUID.fromString(npcUuid));
        if (ref == null || !ref.isValid()) return null;

        Store<EntityStore> store = ref.getStore();
        if (store == null || !store.isInThread()) return null;

        Map<String, Object> currentState = dw.portablemonstrosities.util.PkmnStatUtils.getState(store, ref);
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

    // ── Utilities ─────────────────────────────────────────────────────────────

    private static boolean partiesEqual(List<PkmnPartySlot> a, List<PkmnPartySlot> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).equals(b.get(i))) return false;
        }
        return true;
    }

    private static void fail(@Nonnull InteractionContext context) {
        context.getState().state = InteractionState.Failed;
    }
}