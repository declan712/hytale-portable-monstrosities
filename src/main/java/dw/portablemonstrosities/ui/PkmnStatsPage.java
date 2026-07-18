package dw.portablemonstrosities.ui;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.metadata.CapturedNPCMetadata;

import app.ultradev.hytaleuiparser.source.AssetSource;
import app.ultradev.hytaleuiparser.source.AssetSources;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.Options;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.InventoryComponent.Backpack;
import com.hypixel.hytale.server.core.inventory.InventoryComponent.Hotbar;
import com.hypixel.hytale.server.core.inventory.InventoryComponent.Storage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

import au.ellie.hyui.builders.HyUIPage;
import au.ellie.hyui.builders.NativeTabNavigationBuilder;
import au.ellie.hyui.builders.PageBuilder;
import au.ellie.hyui.builders.TabNavigationBuilder;
import au.ellie.hyui.builders.UIElementBuilder;
import au.ellie.hyui.uiparser.UIFileParser;
import au.ellie.hyui.uiparser.UIParseOptions;
import au.ellie.hyui.uiparser.UnifiedAssetSource;
import dw.portablemonstrosities.components.PkmnCaptureMetadata;
import dw.portablemonstrosities.components.PkmnStatsComponent;
import dw.portablemonstrosities.components.PkmnStatsComponent.PkmnStat;
import dw.portablemonstrosities.util.PkmnStatUtils;

import au.ellie.hyui.events.DynamicPageData;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import java.util.function.BiConsumer;

public class PkmnStatsPage {
    
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String HUD_KEY  = "PkmnParty";
    public static final int    MAX_SLOTS = 6;
    private static final String UI_FILE  = "PkmnStatsPage.ui";

    enum Tab{Overview,Stats,Moves,Other};

    private final String overviewTabId  = "#OverviewContent";
    private final String statsTabId     = "#StatsContent";
    private final String movesTabId     = "#MovesContent";
    
    private ItemStack               ball;
    private PkmnCaptureMetadata     pkmnMeta;
    private CapturedNPCMetadata     npcMeta;
    private PkmnStatsComponent      pkmnStats;

    private final String FALLBACK_ICON = "Icons/ModelsGenerated/Pkmn_Bulbasaur.png";

    private String nickname     = null;
    private String species      = null;
    private String icon         = FALLBACK_ICON;
    private int    lvl          = 1;
    private String type1        = null;
    private String type2        = null;
    private String status       = null;

    private CustomPageLifetime myLifetime = CustomPageLifetime.CanDismiss;
    private BiConsumer<DynamicPageData, HyUIPage> rawEventHandler;

    public static PkmnStatsPage detached() { return new PkmnStatsPage(); }

    public RawEventPageBuilder page;

    public PkmnStatsPage(){
        // page = PageBuilder.detachedPage()
        //     .enablePersistentElementEdits(true)
        //     .fromUIFile("Pages/"+UI_FILE,buildAssetOptions());
        page = (RawEventPageBuilder)RawEventPageBuilder.detached()
            .enablePersistentElementEdits(true)
            .fromFile("Pages/"+UI_FILE);

        page.onRawDataEvent((data, hyuiPage) -> {
            LOGGER.atInfo().log("onRawDataEvent");
            switch (data.action) {
                case "Overview" -> tabSelected("Overview");
                case "Stats"    -> tabSelected("Stats");
                case "Moves"    -> tabSelected("Moves");
            }
            // hyuiPage.triggerRefresh();
            hyuiPage.updatePage(false);
        });

        page.editElement((cmds,events)->{
            events.addEventBinding(CustomUIEventBindingType.Activating, 
                                    "#Overview",
                                    EventData.of("Action", "Overview"));
            events.addEventBinding(CustomUIEventBindingType.Activating, 
                                    "#Stats",
                                    EventData.of("Action", "Stats"));
            events.addEventBinding(CustomUIEventBindingType.Activating, 
                                    "#Moves",
                                    EventData.of("Action", "Moves"));
        });
    }


    public boolean fill(
        @Nonnull Store<EntityStore> store, 
        @Nonnull Ref<EntityStore> ref, 
        @Nonnull PlayerRef playerRef,
        @Nonnull World world
    ){
        Hotbar   hotbar   = store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
        // Storage  storage  = store.getComponent(ref, InventoryComponent.Storage.getComponentType());
        // Backpack backpack = store.getComponent(ref, InventoryComponent.Backpack.getComponentType());
        ball = hotbar.getActiveItem();
        if (!PkmnPartyUtil.isValidPokeball(ball.getItemId())){
            playerRef.sendMessage(Message.raw("Must be holding valid item"));
            return false;
        }

        pkmnMeta = ball.getFromMetadataOrNull("PkmnCapture", PkmnCaptureMetadata.CODEC);
        if (pkmnMeta == null) {
            playerRef.sendMessage(Message.raw("Held item has no Pokemon data"));
            return false; 
        }
        
        npcMeta = ball.getFromMetadataOrNull("CapturedEntity",CapturedNPCMetadata.CODEC);
        if (npcMeta == null) {
            playerRef.sendMessage(Message.raw("Pokemon is not currently in the ball"));
        }

        final String status = pkmnMeta.getNpcStatus();
        if (status == null) {
            playerRef.sendMessage(Message.raw("Pokemon status: UNKNOWN"));
            return false;
        }
        
        pkmnStats = PkmnStatUtils.fromMetadata(pkmnMeta);
        if (pkmnStats == null) {
            playerRef.sendMessage(Message.raw("Unable to read Pokemon stats"));
            return false;
        }



        populateOverview(store,ref,pkmnStats);
        populateStats(store,ref,pkmnStats);
        populateMoves(store,ref,pkmnStats);

        return true;
    }


    public HyUIPage open(
        @Nonnull PlayerRef playerRef, 
        Store<EntityStore> store
    ){
        HyUIPage hyUIPage = page.open(playerRef,store);
        return hyUIPage;
    }

    private void tabSelected(String selectedTabId){
        LOGGER.atInfo().log("Tab selected: "+selectedTabId);
        switch(selectedTabId){
            case "Overview":
                switchTabOverview();
                break;
            case "Stats":
                switchTabStats();
                break;
            case "Moves":
                switchTabMoves();
                break;
            case "Other":
                switchTabOther();
                break;
        }
    }

    public void switchTab(Tab tab){
        switch(tab){
            case Tab.Overview:
                switchTabOverview();
                break;
            case Tab.Stats:
                switchTabStats();
                break;
            case Tab.Moves:
                switchTabMoves();
                break;
            case Tab.Other:
                switchTabOther();
                break;
        }

    }

    private void switchTabOverview(){
        LOGGER.atInfo().log("switchTabOverview");
        page.editElement(cmds->cmds.set(overviewTabId+".Visible", true))
            .editElement(cmds->cmds.set(statsTabId+".Visible", false))
            .editElement(cmds->cmds.set(movesTabId+".Visible", false))
            .editElement(cmds->cmds.set("#Tabs.SelectedTab", "overview"));
    }

    private void switchTabStats(){
        LOGGER.atInfo().log("switchTabStats");
        page.editElement(cmds->cmds.set(overviewTabId+".Visible", false))
            .editElement(cmds->cmds.set(statsTabId+".Visible", true))
            .editElement(cmds->cmds.set(movesTabId+".Visible", false))
            .editElement(cmds->cmds.set("#Tabs.SelectedTab", "stats"));
    }

    private void switchTabMoves(){
        LOGGER.atInfo().log("switchTabMoves");
        page.editElement(cmds->cmds.set(overviewTabId+".Visible", false))
            .editElement(cmds->cmds.set(statsTabId+".Visible", false))
            .editElement(cmds->cmds.set(movesTabId+".Visible", true))
            .editElement(cmds->cmds.set("#Tabs.SelectedTab", "moves"));
    }

    private void switchTabOther(){
        page.editElement(cmds->cmds.set(overviewTabId+".Visible", false))
            .editElement(cmds->cmds.set(statsTabId+".Visible", false))
            .editElement(cmds->cmds.set(movesTabId+".Visible", false))
            .editElement(cmds->cmds.set("#Tabs.SelectedTab", "other"));
    }

    private void populateOverview(
        @Nonnull Store<EntityStore> store, 
        @Nonnull Ref<EntityStore> ref,
        @Nonnull PkmnStatsComponent pkmnStats
    ){
        // TITLE:  [T][T][T][T]  lvl name sex   [rename]

        // #OverviewContent
            // #PkmnIcon.AssetPath (Icon) ("Icons/ModelsGenerated/Pkmn_Bulbasaur.png")
            // #TopRight
                // #Row1 
                //      #DexNum.Text (number)
                // #Row2 
                //      #Name.Text (name)
                // #Row3 
                //      #Type1.AssetPath, #Type2.AssetPath (types) ("UI/Textures/Icons/Types/PoisonType.png")
                // #Row4 
                //      #Owner.Text (Owner)
                // #Row5 
                //      #ID.Text (ID)
                // #Row6 
                //      #HeldItem.Text (heldItem)
            // #Memo (memo)
                // #MemoLine1.Text
                // #MemoLine2.Text

    // private String nickname     = null;
    // private String species      = null;
    // private String icon         = FALLBACK_ICON;
    // private int    lvl          = 1;
    // private String type1        = null;
    // private String type2        = null;
    // private String status       = null;

        lvl = pkmnStats.getLevel();
        type1 = pkmnStats.getType1();
        type2 = pkmnStats.getType2();
        status = pkmnMeta.getNpcStatus();
        species = PkmnStatUtils.displayNameOf(pkmnMeta.getRoleId());
        nickname = pkmnStats.getNickname();
        String name = nickname!=null ? nickname : species;
        String nature = (pkmnStats.getNature() != null) ? pkmnStats.getNature() : "UKNOWN";
        var owner = pkmnStats.getOwner();

        page.editElement(cmds->cmds.set("#Level.Text", "Lvl."+String.valueOf(lvl)))
            .editElement(cmds->cmds.set("#Species.Text", species))
            .editElement(cmds->cmds.set("#Sex.Text", "N/A"))
            .editElement(cmds->cmds.set(overviewTabId+" #PkmnIcon.AssetPath", getIconPath()))
            .editElement(cmds->cmds.set(overviewTabId+" #DexNum.Text", "N/A"))
            .editElement(cmds->cmds.set(overviewTabId+" #TopRight #Name.Text", name))
            .editElement(cmds->cmds.set(overviewTabId+" #TopRight #Type1.AssetPath", typeIcon(type1)))
            .editElement(cmds->cmds.set(overviewTabId+" #TopRight #Type2.AssetPath", typeIcon(type2)))
            .editElement(cmds->cmds.set(overviewTabId+" #TopRight #Owner.Text", owner))
            .editElement(cmds->cmds.set(overviewTabId+" #TopRight #ID.Text", "N/A"))
            .editElement(cmds->cmds.set(overviewTabId+" #TopRight #HeldItem.Text", "N/A"))
            .editElement(cmds->cmds.set(overviewTabId+" #MemoLine1.Text", nature+" nature."))
            .editElement(cmds->cmds.set(overviewTabId+" #MemoLine2.Text", "N/A"));
    }

    private void populateStats(
        @Nonnull Store<EntityStore> store, 
        @Nonnull Ref<EntityStore> ref,
        @Nonnull PkmnStatsComponent pkmnStats
    ){
        var currentStats = PkmnStatUtils.getCurrentStats(store, ref);

        
        var bastStats = pkmnStats.getBaseStats();
        int atk = pkmnStats.calcEffectiveStat(PkmnStat.ATK.index);
        int def = pkmnStats.calcEffectiveStat(PkmnStat.DEF.index);
        int spatk = pkmnStats.calcEffectiveStat(PkmnStat.SPATK.index);
        int spdef = pkmnStats.calcEffectiveStat(PkmnStat.SPDEF.index);
        int speed = pkmnStats.calcEffectiveStat(PkmnStat.SPD.index);
        int hp = pkmnStats.calcEffectiveStat(PkmnStat.HP.index);
        long exp = pkmnStats.getExperience();
        var expMax = lvl*lvl*lvl;
        var currentHp = pkmnMeta.getCurrentHp();
        var currentExp = pkmnMeta.getExperience();
        var ivs = pkmnStats.getIvs();
        var evs = pkmnStats.getEvs();

        page.editElement(cmds->cmds.set(statsTabId+" #PkmnIcon.AssetPath", getIconPath()))
            .editElement(cmds->cmds.set(statsTabId+" #HealthBar.Value",    currentHp/hp))
            .editElement(cmds->cmds.set(statsTabId+" #HealthText.Text",    String.valueOf(currentHp)+"/"+String.valueOf(hp)))
            .editElement(cmds->cmds.set(statsTabId+" #ExpBar.Value",       currentExp/expMax))
            .editElement(cmds->cmds.set(statsTabId+" #ExpText.Text",       String.valueOf(currentExp)+"/"+String.valueOf(expMax)))
            .editElement(cmds->cmds.set(statsTabId+" #AtkValue.Text",      String.valueOf(atk)))
            .editElement(cmds->cmds.set(statsTabId+" #AtkIV.Text",         String.valueOf(ivs[PkmnStat.ATK.index])))
            .editElement(cmds->cmds.set(statsTabId+" #DefValue.Text",      String.valueOf(def)))
            .editElement(cmds->cmds.set(statsTabId+" #DefIV.Text",         String.valueOf(ivs[PkmnStat.DEF.index])))
            .editElement(cmds->cmds.set(statsTabId+" #SpAtkValue.Text",    String.valueOf(spatk)))
            .editElement(cmds->cmds.set(statsTabId+" #SpAtkIV.Text",       String.valueOf(ivs[PkmnStat.SPATK.index])))
            .editElement(cmds->cmds.set(statsTabId+" #SpDefValue.Text",    String.valueOf(spdef)))
            .editElement(cmds->cmds.set(statsTabId+" #SpDefIV.Text",       String.valueOf(ivs[PkmnStat.SPDEF.index])))
            .editElement(cmds->cmds.set(statsTabId+" #SpeedValue.Text",    String.valueOf(speed)))
            .editElement(cmds->cmds.set(statsTabId+" #SpeedIV.Text",       String.valueOf(ivs[PkmnStat.SPD.index])));
    }

    private void populateMoves(
        @Nonnull Store<EntityStore> store, 
        @Nonnull Ref<EntityStore> ref,
        @Nonnull PkmnStatsComponent pkmnStats
    ){

        // page.editElement(cmds->cmds.set(movesTabId+" #PkmnIcon.AssetPath", getIconPath()))
        //     .editElement(cmds->cmds.set(movesTabId+" #DexNum.Text", "151"))
        //     .editElement(cmds->cmds.set(movesTabId+" #TopRight #Name.Text", "nickname"))
        //     .editElement(cmds->cmds.set(movesTabId+" #TopRight #Type1.AssetPath", typeIcon("FireType")))
        //     .editElement(cmds->cmds.set(movesTabId+" #TopRight #Type2.AssetPath", typeIcon("WaterType")))
        //     .editElement(cmds->cmds.set(movesTabId+" #TopRight #ID.Text", "ID??"))
        //     .editElement(cmds->cmds.set(movesTabId+" #TopRight #HeldItem.Text", "These hands"))
        //     .editElement(cmds->cmds.set(movesTabId+" #MemoLine1.Text", "BRAT nature."))
        //     .editElement(cmds->cmds.set(movesTabId+" #MemoLine2.Text", "Met in Env_Zone1 at Lv 1"));
    }

    private String getIconPath(){
        if (pkmnMeta == null) { LOGGER.atInfo().log("getIconPath(): pkmnMeta null"); return FALLBACK_ICON; }
        if (npcMeta == null)  { LOGGER.atInfo().log("getIconPath(): npcMeta null");  return FALLBACK_ICON; }

        final String status = pkmnMeta.getNpcStatus();
        if (status == null)   { LOGGER.atInfo().log("getIconPath(): status null");   return FALLBACK_ICON; }

        final boolean isActive  = "Active".equals(status);
        final boolean isFainted = "Fainted".equals(status);
        final boolean isHealthy = "Healthy".equals(status);

        if (!isActive && !isFainted && !isHealthy) { LOGGER.atInfo().log("getIconPath(): status UNKNOWN"); return FALLBACK_ICON; }
        String roleId = isActive 
                ? pkmnMeta.getRoleId()
                : null; // For fainted/healthy, we'll get name from captured entity metadata
        
        // final String nickname = pkmnMeta.getNickname();

        if (roleId == null && npcMeta != null) { roleId = npcMeta.getNpcNameKey(); }
        if (roleId == null) { LOGGER.atInfo().log("getIconPath(): roleId null"); return FALLBACK_ICON; }

        // String name = PkmnStatUtils.displayNameOf(roleId);
        // if (nickname != null && !nickname.isBlank()) name = nickname;
        String wildRole = PkmnStatUtils.getWildRole(roleId);// roleId.replace("_Tamed", "").replace("Tamed_", "");
        String icon     = "Icons/ModelsGenerated/" + wildRole + ".png";

        return icon;
    }

    private String typeIcon(String type){
        final String typeIconDir = "UI/Textures/Icons/Types/";
        final String png = "Type.png";
        if(type==null || type.equals("")) { return ""; }
        return typeIconDir+type+png;
    }

}
