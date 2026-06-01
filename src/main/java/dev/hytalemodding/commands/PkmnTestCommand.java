package dev.hytalemodding.commands;

import java.util.List;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractTargetEntityCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

public class PkmnTestCommand extends AbstractTargetEntityCommand{

    public PkmnTestCommand(String name, String description) {
        super(name,description);
    }

    @Override
    protected void execute(
        CommandContext ctx, 
        List<Ref<EntityStore>> refs, 
        World world, 
        Store<EntityStore> store
    ) {
        Ref<EntityStore> sender = ctx.senderAsPlayerRef();
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
        String activeMotionController = npc.getActiveMotionControllerName();
        playerRef.sendMessage(Message.raw("current: "+activeMotionController));
        npc.setActiveMotionControllerName("Dive");
        
    }
    
}
