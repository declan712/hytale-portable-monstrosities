package dw.portablemonstrosities.ui;

import javax.annotation.Nonnull;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class PkmnHealthBarHUD extends CustomUIHud {
    private final String file="PkmnHealthBar.ui";
    private String pkmnName;
    private int pkmnLvl;
    private int pkmnHp;
    private int pkmnMaxHp;
//    public TestUIHUD(@Nonnull PlayerRef playerRef, String file) {
//       super(playerRef);
//       this.file = file;
//    }


    public PkmnHealthBarHUD(
        @Nonnull PlayerRef playerRef,
        @Nonnull String key,
        String name,
        int level,
        int currentHealth,
        int maxHealth
    ) {
        this.pkmnName = name;
        this.pkmnLvl = level;
        this.pkmnHp = currentHealth;
        this.pkmnMaxHp = maxHealth;
        super(playerRef,key);
        // this.file = file;
    }
    public PkmnHealthBarHUD(
        @Nonnull PlayerRef playerRef,
        @Nonnull String key
    ) {
        super(playerRef,key);
        // this.file = file;
    }



    // public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
    //     uiCommandBuilder.append("test.ui");
    //     uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#MyInput", EventData.of("@MyInput", "#MyInput.Value"), false);
    // }

//    protected void build(@Nonnull UICommandBuilder uiCommandBuilder) {
//       uiCommandBuilder.append("Pages/" + this.file);
//    }
    @Override
    protected void build(@Nonnull UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("Pages/" + this.file);

        if(pkmnName == null) pkmnName="pkmn";
        // if(pkmnLvl == null) pkmnLvl=1;
        // if(pkmnHp == null) pkmnHp=1;
        // if(pkmnMaxHp == null) pkmnMaxHp=1;

        // uiCommandBuilder.append("PkmnHealthBar.ui");

        uiCommandBuilder.set("#PkmnName.TextSpans",Message.raw(pkmnName));
        uiCommandBuilder.set("#PkmnLevel.TextSpans",Message.raw(String.valueOf(pkmnLvl)));
        uiCommandBuilder.set("#PkmnHp.TextSpans",Message.raw(String.valueOf(pkmnHp)+"/"+String.valueOf(pkmnMaxHp)));
    }


    public void updateName(String newText,@Nonnull UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.set("#PkmnName.TextSpans", Message.raw(newText));
        update(false, uiCommandBuilder); // false = don't clear existing UI
    }

    public void updateLevel(String newText,@Nonnull UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.set("#PkmnLevel.TextSpans", Message.raw(newText));
        update(false, uiCommandBuilder); // false = don't clear existing UI
    }

    public void updateHealth(String newText,@Nonnull UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.set("#PkmnHp.TextSpans", Message.raw(newText));
        update(false, uiCommandBuilder); // false = don't clear existing UI
    }

}

// Showing & Hiding UI
// ────────────────────────────

// You can get the HudManager using Player#getHudManager.

//     Use HudManager#setCustomHud to show UI.
//     Use HudManager#hideHudComponents to hide default Hytale UI.
//     Set a custom hud with an empty build method to hide custom UI, or use MultipleHUD






// import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
// import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
// import com.hypixel.hytale.server.core.universe.PlayerRef;
// import javax.annotation.Nonnull;

// public class TestUIHUD extends CustomUIHud {
//    private final String file;

//    public TestUIHUD(@Nonnull PlayerRef playerRef, String file) {
//       super(playerRef);
//       this.file = file;
//    }

//    protected void build(@Nonnull UICommandBuilder uiCommandBuilder) {
//       uiCommandBuilder.append("Pages/" + this.file);
//    }
// }
