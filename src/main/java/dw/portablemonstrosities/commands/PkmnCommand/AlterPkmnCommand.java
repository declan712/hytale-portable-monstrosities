package dw.portablemonstrosities.commands.PkmnCommand;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import dw.portablemonstrosities.commands.PkmnCommand.alter.PkmnShinyCommand;

public class AlterPkmnCommand extends AbstractCommandCollection{
    public AlterPkmnCommand (String name, String description) {
        super(name, description);
        addSubCommand(new PkmnShinyCommand("shiny",
            "Set the player as the owner of the target NPC"));
        // addSubCommand(new RestoreLostPkmnCommand("restore",
        //     "Returns the Pokemon to the currently held ball, even if it has despawned"));
    }
    
}


