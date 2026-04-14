package dev.hytalemodding.interactions;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.ItemWithAllMetadata;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import dev.hytalemodding.components.PkmnCaptureMetadata;
import dev.hytalemodding.components.PkmnStatsComponent;
import dev.hytalemodding.util.PkmnStatUtils;



public class CreatureScannerInteration extends SimpleInstantInteraction {

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final BuilderCodec<CreatureScannerInteration> CODEC = BuilderCodec.builder(
        CreatureScannerInteration.class, 
        CreatureScannerInteration::new, 
        SimpleInstantInteraction.CODEC
    ).build();

    @Override
    protected void firstRun(
        @Nonnull InteractionType interactionType,
        @Nonnull InteractionContext interactionContext,
        @Nonnull CooldownHandler cooldownHandler
    ) {

        CommandBuffer<EntityStore> commandBuffer = interactionContext.getCommandBuffer();

        if (commandBuffer == null) {
            LOGGER.atInfo().log("CommandBuffer is null");
            fail(interactionContext);
            return;
        }

        // World world = commandBuffer.getExternalData().getWorld();
        Store<EntityStore> store = commandBuffer.getExternalData().getStore();


        Ref<EntityStore> playerRef = interactionContext.getEntity();
        Player player = commandBuffer.getComponent(playerRef, Player.getComponentType());

        if (player == null) {
            LOGGER.atInfo().log("Player is null");
            fail(interactionContext);
            return;
        }

        ItemStack itemStack = interactionContext.getHeldItem();
        if (itemStack == null) {
            // LOGGER.atInfo().log("ItemStack is null");
            // player.sendMessage(Message.raw("ItemStack is null"));
            fail(interactionContext);
            return;
        }

        Ref<EntityStore> targetRef = interactionContext.getTargetEntity();
        if (targetRef == null) {
            // LOGGER.atInfo().log("target ref null");
            player.sendMessage(Message.raw("No target"));
            fail(interactionContext);
            return;
        }
        // Check if target is a pkmn
        NPCEntity npcEntity = store.getComponent(targetRef,NPCEntity.getComponentType());
        if(npcEntity==null){
            player.sendMessage(Message.raw("That's not an NPC..."));
            fail(interactionContext);
            return;
        }
        String roleName = npcEntity.getRoleName();
        if(!filterByRoleName(roleName)) {
            player.sendMessage(Message.raw("That's not a pkmn, its a "+roleName));
            fail(interactionContext);
            return;
        }

        String pokemonSpecies = PkmnStatUtils.speciesFromRole(roleName);

        PkmnCaptureMetadata metadata = PkmnStatUtils.captureMetadata(commandBuffer,targetRef);
        PkmnStatsComponent pkmnStats = PkmnStatUtils.fromMetadata(metadata);
        
        player.sendMessage(Message.raw(pokemonSpecies+", the something Pkmn."));

        String playerUuid = player.getUuid().toString();
        String nameplateString = buildNamplateString(roleName,pkmnStats,playerUuid);

        commandBuffer.putComponent(targetRef,Nameplate.getComponentType(),new Nameplate(nameplateString));
        notifyPlayer(
            player.getPlayerRef(),
            nameplateString,
            PkmnStatUtils.speciesFromRole(roleName)
        );
        next(interactionContext);
        return;
    }

    private void next(@Nonnull InteractionContext interactionContext){
        interactionContext.getState().state = InteractionState.NotFinished;
    }

    private void fail(@Nonnull InteractionContext interactionContext){
        interactionContext.getState().state = InteractionState.Failed;
    }
    
    private String buildNamplateString(
        @Nonnull String npcRoleId,
        @Nonnull PkmnStatsComponent stats,
        @Nullable String playerUuid
    ){
        String name = PkmnStatUtils.speciesFromRole(npcRoleId);
        var nickname = stats.getNickname();
        if(nickname != null && !nickname.isBlank()){
            name=nickname;
        }
        String lvl = String.valueOf(stats.getLevel());
        return name+" ["+lvl+"]";
    }

    private boolean filterByRoleName(String roleName) {
        if(roleName.startsWith("Pkmn_")) return true;
        // testing custom NPC/fakemon
        if(roleName.startsWith("Lizard_Ground_")) return true;
        return false;
    }


    private void notifyPlayer(
        @NonNullDecl PlayerRef playerRef,
        @Nullable String topText,
        @Nullable String bottomText
    ) {
        String primaryText = "Custom data";
        String secondaryText = "";
        String primaryColour = "#00FF00";
        String secondaryColour = "#228B22";
        String iconItemId = "Pkmn_Pokedex";

        if(topText==null) topText = primaryText;
        if(bottomText==null) bottomText = secondaryText;

        var packetHandler = playerRef.getPacketHandler();
        var primaryMessage = Message.raw(topText).color(primaryColour);
        var secondaryMessage = Message.raw(bottomText).color(secondaryColour);
        var icon = new ItemStack(iconItemId, 1).toPacket();

        NotificationUtil.sendNotification(
            packetHandler,
            primaryMessage,
            secondaryMessage,
            (ItemWithAllMetadata) icon);
    }
        
}