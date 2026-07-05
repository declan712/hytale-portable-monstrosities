package dw.portablemonstrosities.interactions;

import java.util.ArrayList;
import java.util.List;

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
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import au.ellie.hyui.builders.HudBuilder;
import au.ellie.hyui.builders.HyUIHud;

import dw.portablemonstrosities.ui.PkmnPartyHUD;
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

    // ── CODEC ────────────────────────────────────────────────────────────────

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

        // ── Toggle: close HUD if already open ───────────────────────────────────
        HyUIHud existingHud = PkmnPartyHUD.hudCache.getOrDefault(playerRef.toString(), null);
        if (existingHud != null) {
            existingHud.remove();
            PkmnPartyHUD.hudCache.put(playerRef.toString(), null);
            context.getState().state = InteractionState.Finished;
            return;
        }

        // Gather inventory references for the refresh callback
        Hotbar   hotbar   = store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
        Storage  storage  = store.getComponent(ref, InventoryComponent.Storage.getComponentType());
        Backpack backpack = store.getComponent(ref, InventoryComponent.Backpack.getComponentType());

        // ── Build and show HUD via PkmnPartyHUD ───────────────────────────────
        
        HyUIHud hud = PkmnPartyHUD.createAndShowHud(
            context,
            hotbar,
            storage,
            backpack
        );
        
        // HudBuilder hud = HudBuilder.hudForPlayer(playerRef)
        //     .fromUIFile("Pages/PkmnParty.ui")
        //     .enablePersistentElementEdits(true)
        //     .withRefreshRate(500);

        // setSlots(hud, PkmnPartyHUD.buildPartyFromInventory(
        //     store, ref, world, hotbar, storage, backpack));

        // hud.onRefresh(hyUIHud -> {
        //     world.execute(() -> {
        //         List<PkmnPartySlot> oldParty = PkmnPartyHUD.partyCache.getOrDefault(playerRef.toString(), new ArrayList<>());
        //         List<PkmnPartySlot> newParty = PkmnPartyHUD.buildPartyFromInventory(
        //             store, ref, world, hotbar, storage, backpack);
        //         PkmnPartyHUD.updateSlots(hud, newParty, oldParty, playerRef.toString());
        //         hyUIHud.update(hud);

        //         UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        //         hyUIHud.build(uiCommandBuilder);
        //         hyUIHud.update(true, uiCommandBuilder);
        //     });
        // });

        // PkmnPartyHUD.builderCache.put(playerRef.toString(), hud);
        // HyUIHud liveHud = hud.show();
        // PkmnPartyHUD.hudCache.put(playerRef.toString(), liveHud);

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
        for (int i = 1; i <= 6; i++) {
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
                    .editElement(cmds -> cmds.set(idBall, "UUI/Custom/Pages/Pkmn/Pokeball_None.png"));
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

            if(pkmn.fainted){       hud.editElement(cmds -> cmds.set(idBall, "UI/Custom/Pages/Pkmn/Pokeball_Dead.png"));}
            else if (pkmn.active){  hud.editElement(cmds -> cmds.set(idBall, "UI/Custom/Pages/Pkmn/Pokeball_Grey.png"));}
            else {                  hud.editElement(cmds -> cmds.set(idBall, "UI/Custom/Pages/Pkmn/Pokeball.png"));}

            hud.editElement(cmds -> cmds.set(idName,   name))
                .editElement(cmds -> cmds.set(idLevel,  level))
                .editElement(cmds -> cmds.set(idHpBar,  pctHp))
                .editElement(cmds -> cmds.set(idHpText, hpText))
                .editElement(cmds -> cmds.set(idIcon,   icon));
        }
    }

    // ── Utilities ────────────────────────────────────────────────────────────

    private static void fail(@Nonnull InteractionContext context) {
        context.getState().state = InteractionState.Failed;
    }
}
