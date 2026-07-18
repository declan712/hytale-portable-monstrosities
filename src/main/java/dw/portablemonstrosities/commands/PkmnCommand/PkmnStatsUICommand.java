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
import dw.portablemonstrosities.ui.PkmnStatsPage;
import dw.portablemonstrosities.util.PkmnStatUtils;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;

//*
// Get current held item,
// IF item is ball with pkmn, display stats page
// 
//  */
public class PkmnStatsUICommand extends AbstractPlayerCommand {

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

        PkmnStatsPage page = new PkmnStatsPage();
        boolean res = page.fill(store,ref,playerRef,world);
        if (!res) {
            playerRef.sendMessage(Message.raw("Unable to populate stats page"));
            return;
        }
        page.open(playerRef, store);
    }

}
