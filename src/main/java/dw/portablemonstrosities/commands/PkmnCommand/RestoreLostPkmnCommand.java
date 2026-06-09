package dw.portablemonstrosities.commands.PkmnCommand;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent.Hotbar;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.metadata.CapturedNPCMetadata;

import dw.portablemonstrosities.components.PkmnCaptureMetadata;

public class RestoreLostPkmnCommand extends AbstractPlayerCommand{

    public RestoreLostPkmnCommand(String name, String description) {
        super(name, description);
    }

    @Override
    protected void execute(
        @Nonnull CommandContext context, 
        @Nonnull Store<EntityStore> store, 
        @Nonnull Ref<EntityStore> ref, 
        @Nonnull PlayerRef playerRef,
        @Nonnull World world
    ) {
        Hotbar hotbar = store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
        if(hotbar==null) return;
        ItemStack  heldItem = hotbar.getActiveItem();
        byte  slot = hotbar.getActiveSlot();
        if(heldItem==null) return;
        PkmnCaptureMetadata pkmnMeta = heldItem.getFromMetadataOrNull("PkmnCapture", PkmnCaptureMetadata.CODEC);
        CapturedNPCMetadata npcMeta = heldItem.getFromMetadataOrNull("CapturedEntity", CapturedNPCMetadata.CODEC);
        if(npcMeta!=null) return;
        if(pkmnMeta==null) return;
        CapturedNPCMetadata newMeta = new CapturedNPCMetadata();
        newMeta.setNpcNameKey(pkmnMeta.getRoleId());
        pkmnMeta.setNpcStatus("Healthy");
        ItemStack newItem = heldItem
                .withMetadata("CapturedEntity", CapturedNPCMetadata.CODEC,newMeta)
                .withMetadata("PkmnCapture", PkmnCaptureMetadata.CODEC,pkmnMeta);
        world.execute(()->{
            hotbar.getInventory()
            .replaceItemStackInSlot(slot,heldItem,newItem);
        });

    }
    
}
