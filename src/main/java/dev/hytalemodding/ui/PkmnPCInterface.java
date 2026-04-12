package dev.hytalemodding.ui;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;


/**
 * DO NOT USE<br>
 * <br>
 * very not finished<br>
 * <br>
 * is it even worth doing UI stuff if the whole system is changing soon?<br>
 */
public class PkmnPCInterface extends CustomUIHud {
    
    public PkmnPCInterface(PlayerRef playerRef) {
        super(playerRef);
    }

    // public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
    //     uiCommandBuilder.append("test.ui");
    //     uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#MyInput", EventData.of("@MyInput", "#MyInput.Value"), false);
    // }

    @Override
    protected void build(UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("PkmnPCInterface.ui");
        // uiCommandBuilder.set("#PkmnName.TextSpans",Message.raw("pizza"));
        // uiCommandBuilder.set("#PkmnHealthBar.Value",0.75);
    }

    // public void updateLevel(String newText) {
    //     UICommandBuilder uiCommandBuilder = new UICommandBuilder();
    //     uiCommandBuilder.set("#PkmnName.TextSpans", Message.raw(newText));
    //     uiCommandBuilder.set("#PkmnName.TextSpans", Message.raw(newText));
    //     update(false, uiCommandBuilder); // false = don't clear existing UI
    // }

    // public void updateHealth(String newText) {
    //     UICommandBuilder uiCommandBuilder = new UICommandBuilder();
    //     uiCommandBuilder.set("#PkmnHp.TextSpans", Message.raw(newText));
    //     uiCommandBuilder.set("#PkmnHealthBar.Value",0.75);
    //     update(false, uiCommandBuilder); // false = don't clear existing UI
    // }

}



// Showing & Hiding UI
// ────────────────────────────
// You can get the HudManager using Player#getHudManager.
//     Use HudManager#setCustomHud to show UI.
//     Use HudManager#hideHudComponents to hide default Hytale UI.
//     Set a custom hud with an empty build method to hide custom UI, or use MultipleHUD
