package dev.hytalemodding.commands.PkmnCommand;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class RestoreLostPkmnCommand extends AbstractPlayerCommand{

    public RestoreLostPkmnCommand(String name, String description) {
        super(name, description);
        //TODO Auto-generated constructor stub
    }

    @Override
    protected void execute(
        CommandContext arg0, 
        Store<EntityStore> arg1, 
        Ref<EntityStore> arg2, 
        PlayerRef arg3,
        World arg4
    ) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'execute'");
    }
    
}
