package dev.hytalemodding.commands.PkmnCommand;

import java.util.List;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractTargetEntityCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import dev.hytalemodding.components.PkmnStatsComponent;
import dev.hytalemodding.util.PkmnStatUtils;

public class ClaimOwnershipCommand extends AbstractTargetEntityCommand{

    public ClaimOwnershipCommand(String name, String description) {
        super(name,description);
    }

    @Override
    protected void execute(
        CommandContext context, 
        List<Ref<EntityStore>> refs, 
        World world, 
        Store<EntityStore> store
    ) {
        Ref<EntityStore> sender = context.senderAsPlayerRef();
        if(sender==null) return;

        PlayerRef playerRef = store.getComponent(sender,PlayerRef.getComponentType());
        if(playerRef==null) return;
        
        Ref<EntityStore> target = refs.getFirst();
        if(target==null || !target.isValid()){
            playerRef.sendMessage(Message.raw("Must target valid entity"));
            return;
        }
        NPCEntity npc = store.getComponent(target, NPCEntity.getComponentType());
        if(npc==null) {
            playerRef.sendMessage(Message.raw("target is not an NPC"));
            return;
        }

        PkmnStatsComponent pkmnStats = store.getComponent(target, PkmnStatsComponent.getComponentType());
        if(pkmnStats==null) pkmnStats = new PkmnStatsComponent();
        if(PkmnStatUtils.hasOtherOwner(pkmnStats.getOwner(),playerRef)){
            playerRef.sendMessage(Message.raw("Claiming ownership of "+PkmnStatUtils.toDisplayName(npc.getRoleName())));
            pkmnStats.setOwner(playerRef.getUsername());
            store.putComponent(target, PkmnStatsComponent.getComponentType(), pkmnStats);
        }

        // String activeMotionController = npc.getActiveMotionControllerName();
        // playerRef.sendMessage(Message.raw("current: "+activeMotionController));
        // npc.setActiveMotionControllerName("Dive");
        
    }
    
}