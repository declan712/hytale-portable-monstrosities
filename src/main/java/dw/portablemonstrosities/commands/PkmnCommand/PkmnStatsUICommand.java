package dw.portablemonstrosities.commands.PkmnCommand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent.Backpack;
import com.hypixel.hytale.server.core.inventory.InventoryComponent.Hotbar;
import com.hypixel.hytale.server.core.inventory.InventoryComponent.Storage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
// import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.metadata.CapturedNPCMetadata;

import au.ellie.hyui.builders.HudBuilder;
import au.ellie.hyui.builders.HyUIHud;
import au.ellie.hyui.builders.PageBuilder;
import dw.portablemonstrosities.components.PkmnCaptureMetadata;
import dw.portablemonstrosities.ui.PkmnPartySlot;
import dw.portablemonstrosities.util.PkmnStatUtils;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;

//*
// Get current held item,
// IF item is ball with pkmn, display stats page
// 
//  */
public class PkmnStatsUICommand extends AbstractPlayerCommand {
    
    public static final String HUD_KEY  = "PkmnParty";
    public static final int    MAX_SLOTS = 6;
    private static final String UI_FILE  = "PkmnStatsPage.ui";

    private static final List<String> validItems = List.of(
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
    
    public PkmnStatsUICommand(String name, String description) {
        super(name,description);
    }

    @Override
    protected void execute(
        @Nonnull CommandContext context, 
        @Nonnull Store<EntityStore> store, 
        @Nonnull Ref<EntityStore> ref, 
        @Nonnull PlayerRef playerRef,
        @Nonnull World world
    ) {

    Hotbar   hotbar   = store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
    Storage  storage  = store.getComponent(ref, InventoryComponent.Storage.getComponentType());
    Backpack backpack = store.getComponent(ref, InventoryComponent.Backpack.getComponentType());
    // List<PkmnPartySlot> initialParty = buildPartyFromInventory(store,ref,world,hotbar,storage,backpack);

    PageBuilder page = PageBuilder.detachedPage()
    .fromUIFile("Pages/"+UI_FILE)
    .enablePersistentElementEdits(true);
    // .withRefreshRate(1000);


    page.open(playerRef, store);




    }

}
