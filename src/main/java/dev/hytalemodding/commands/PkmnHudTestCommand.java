package dev.hytalemodding.commands;

import java.util.List;
import java.util.Set;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import com.buuz135.mhud.MultipleHUD;
import com.buuz135.mhud.testing.TestUIHUD;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.HudComponent;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractTargetEntityCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import dev.hytalemodding.components.PkmnCaptureMetadata;
import dev.hytalemodding.ui.PkmnHealthBarHUD;
import dev.hytalemodding.util.PkmnStatUtils;

public class PkmnHudTestCommand extends AbstractTargetEntityCommand {


    public PkmnHudTestCommand(String name, String description) {
        super(name,description);
    }

    @Override
    protected void execute(@NonNullDecl CommandContext context,
            @NonNullDecl List<Ref<EntityStore>> refList, 
            @NonNullDecl World world,
            @NonNullDecl Store<EntityStore> store
            // @NonNullDecl Store<EntityStore> store,
            // @NonNullDecl Ref<EntityStore> ref,
            // @NonNullDecl PlayerRef playerRef,
            // @NonNullDecl World world
        ) {
            Player player = context.senderAs(Player.class);
            Ref<EntityStore> refPlayer = player.getReference();
            PlayerRef playerRef = store.getComponent(refPlayer,PlayerRef.getComponentType());

            Ref<EntityStore> ref = refList.getFirst();
            if (ref == null) {
                context.sendMessage(Message.raw("No entity found"));
                hidePkmnHud(player);
                return;
            }

            NPCEntity npcEntity = store.getComponent(ref,NPCEntity.getComponentType());
            if(npcEntity==null){
                context.sendMessage(Message.raw("Target not an NPC"));
                hidePkmnHud(player);
                return;
            }

            String roleName = npcEntity.getRoleName();

            String npcName = PkmnStatUtils.displayNameOf(roleName);

            PkmnCaptureMetadata captureMetadata = PkmnStatUtils.captureMetadata(store,ref);
            
            PkmnHealthBarHUD pkmnHud = new PkmnHealthBarHUD(
                playerRef,
                npcName,
                (int) Math.floor(captureMetadata.getLevel()),
                (int) captureMetadata.getCurrentHp(),
                (int) captureMetadata.getMaxHp()
            );
            MultipleHUD.getInstance().setCustomHud(player, playerRef, "PkmnHealth", pkmnHud); 

            // 

            // HudManager hudManager = player.getHudManager();
            // if(hudManager==null) return;
            // CustomUIHud currentHud = player.getHudManager().getCustomHud();
            // Set<HudComponent> visible = hudManager.getVisibleHudComponents();
            // if(!visible.isEmpty()){
            //     context.sendMessage(Message.raw("Visible hud components:"));
            //     visible.forEach(item -> context.sendMessage(Message.raw(" - "+item.toString())));
            // }

        }

        private void hidePkmnHud(Player player){
            MultipleHUD.getInstance().hideCustomHud(player, "PkmnHealth");
        }
}
