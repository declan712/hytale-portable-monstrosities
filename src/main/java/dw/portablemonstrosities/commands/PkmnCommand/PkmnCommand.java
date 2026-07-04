package dw.portablemonstrosities.commands.PkmnCommand;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

// Server Collection here
public class PkmnCommand extends AbstractCommandCollection {
    public PkmnCommand () {
        super("pkmn", "Pkmn commands");
        addSubCommand(new PkmnHelpUICommand("help",
            "Display help screen"));
        addSubCommand(new ClaimOwnershipCommand("claim",
            "Set the player as the owner of the target NPC"));
        addSubCommand(new RestoreLostPkmnCommand("restore",
            "Returns the Pokemon to the currently held ball, even if it has despawned"));
        addSubCommand(new AlterPkmnCommand("alter",
            "Alter the metadata of the target Pokemon"));
        addSubCommand(new PkmnStatsUICommand("stats",
            "View Pokemon stats UI"));
    }
}