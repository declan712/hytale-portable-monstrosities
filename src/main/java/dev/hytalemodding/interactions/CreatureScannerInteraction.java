package dev.hytalemodding.interactions;
import java.util.Collection;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.ItemWithAllMetadata;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.ActiveEntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import dev.hytalemodding.components.PkmnCaptureMetadata;
import dev.hytalemodding.components.PkmnStatsComponent;
import dev.hytalemodding.util.PkmnStatUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;



public class CreatureScannerInteraction extends SimpleInstantInteraction {

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final BuilderCodec<CreatureScannerInteraction> CODEC = BuilderCodec.builder(
        CreatureScannerInteraction.class, 
        CreatureScannerInteraction::new, 
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
        if(!PkmnStatUtils.filterByRoleName(roleName)) {
            player.sendMessage(Message.raw("That's not a pkmn, its a "+roleName));
            fail(interactionContext);
            return;
        }

        String pokemonSpecies = PkmnStatUtils.speciesFromRole(roleName);

        PkmnCaptureMetadata pkmnMeta = PkmnStatUtils.captureMetadata(commandBuffer,targetRef);
        PkmnStatsComponent pkmnStats = PkmnStatUtils.fromMetadata(pkmnMeta);

                
        player.sendMessage(Message.raw(pokemonSpecies+", the something Pkmn."));
        player.sendMessage(Message.raw("Lvl: "+String.valueOf((int) pkmnMeta.getLevel())));
        player.sendMessage(Message.raw("Exp: "+String.valueOf((int) pkmnMeta.getExperience())));
        // [0] HP
        // [1] Attack
        // [2] Defense
        // [3] Sp.Atk
        // [4] Sp.Def
        // [5] Speed
        String hp      = String.valueOf((int) pkmnMeta.getCurrentHp());
        String maxHp   = String.valueOf((int) pkmnMeta.getMaxHp());

        // var evs = pkmnStats.getEvs();
        var ivs = pkmnStats.getIvs();

        // var hp      = pkmnStats.calcEffectiveStat(0);
        var atk     = String.valueOf((int) pkmnStats.calcEffectiveStat(1) );
        var def     = String.valueOf((int) pkmnStats.calcEffectiveStat(2) );
        var spatk   = String.valueOf((int) pkmnStats.calcEffectiveStat(3) );
        var spdef   = String.valueOf((int) pkmnStats.calcEffectiveStat(4) );
        var spd     = String.valueOf((int) pkmnStats.calcEffectiveStat(5) );

        player.sendMessage(Message.raw("HP: "+hp+"/"+maxHp+"    ( +"+String.valueOf(ivs[0])+" )"  ));
        player.sendMessage(Message.raw("Atk: "+atk+"    ( +"+String.valueOf(ivs[1])+" )"  ));
        player.sendMessage(Message.raw("Def: "+def+"    ( +"+String.valueOf(ivs[2])+" )"  ));
        player.sendMessage(Message.raw("Sp.Atk: "+spatk+"   ( +"+String.valueOf(ivs[3])+" )"  ));
        player.sendMessage(Message.raw("Sp.Def: "+spdef+"   ( +"+String.valueOf(ivs[4])+" )"  ));
        player.sendMessage(Message.raw("Spd: "+spd+"    ( +"+String.valueOf(ivs[5])+" )"  ));

        // player.sendMessage(Message.raw("IVs: [a,b,c,d,e,f]"));
        // player.sendMessage(Message.raw("EVs: [a,b,c,d,e,f]"));
        // player.sendMessage(Message.raw("Base: [a,b,c,d,e,f]"));
        // player.sendMessage(Message.raw("Stat: [a,b,c,d,e,f]"));

        String ownerUuid = pkmnStats.getOwnerUuid();
        World world = store.getExternalData().getWorld();

        if (ownerUuid != null) {
            Ref<EntityStore> ownerRef = world.getEntityRef(UUID.fromString(ownerUuid));
            if(ownerRef != null){
                PlayerRef owner = store.getComponent(ownerRef, PlayerRef.getComponentType());
                if (owner != null) {
                    String username = owner.getUsername();
                    player.sendMessage(Message.raw("owned by: "+username));
                }
            }
        }

        // PlayerRef ownerRef;
        // for (int i=0;i<playerRefs.length;i++) {
        //     playerRefs[i];
        // }
        
        

        EffectControllerComponent effectControllerComponent = store.getComponent(targetRef,EffectControllerComponent.getComponentType());
        IndexedLookupTableAssetMap<String, EntityEffect> effectMap = EntityEffect.getAssetMap();
        Int2ObjectMap<ActiveEntityEffect> activeEffects = effectControllerComponent.getActiveEffects();
        if(activeEffects != null && !activeEffects.isEmpty()){
            player.sendMessage(Message.raw("Active effects:"));
            for(ActiveEntityEffect activeEffect : activeEffects.values()){
                int idx = activeEffect.getEntityEffectIndex();
                EntityEffect effect = effectMap.getAsset(idx);
                String effectId = effect.getId();
                player.sendMessage(Message.raw("  - "+effectId));
                // player.sendMessage(Message.raw("  - "+activeEffect.toString()));
            }
        }
        int[] effectIndexes = effectControllerComponent.getActiveEffectIndexes();
        if(effectIndexes != null){
        }

        // EntityEffect effect = (EntityEffect)effectMap.getAsset();



        String playerUuid = player.getUuid().toString();

        String displayName = PkmnStatUtils.displayNameOf(roleName);
        PkmnStatUtils.setPkmnNameplate(commandBuffer, targetRef, roleName, pkmnStats);


        String topText = displayName;
        String bottomText = "Lvl: "+pkmnStats.getLevel()+", IVs:"+pkmnStats.getIvs().toString()+", Base stats:"+pkmnStats.getBaseStats().toString();


        notifyPlayer(player.getPlayerRef(),topText,bottomText);
        next(interactionContext);
        return;
    }

    private void next(@Nonnull InteractionContext interactionContext){
        interactionContext.getState().state = InteractionState.NotFinished;
    }

    private void fail(@Nonnull InteractionContext interactionContext){
        interactionContext.getState().state = InteractionState.Failed;
    }
    
    // private String buildNamplateString(
    //     @Nonnull String npcRoleId,
    //     @Nonnull PkmnStatsComponent stats,
    //     @Nullable String playerUuid
    // ){
    //     String name = PkmnStatUtils.speciesFromRole(npcRoleId);
    //     var nickname = stats.getNickname();
    //     if(nickname != null && !nickname.isBlank()){
    //         name=nickname;
    //     }
    //     String lvl = String.valueOf(stats.getLevel());
    //     return name+" ["+lvl+"]";
    // }

    // private boolean filterByRoleName(String roleName) {
    //     if(roleName.startsWith("Pkmn_")) return true;
    //     // testing custom NPC/fakemon
    //     if(roleName.startsWith("Lizard_Ground_")) return true;
    //     return false;
    // }


    private void notifyPlayer(
        @NonNullDecl PlayerRef playerRef,
        @Nullable String topText,
        @Nullable String bottomText
    ) {
        String primaryText = "Custom data";
        String secondaryText = "";
        String primaryColour = "#a64cca";
        String secondaryColour = "#959097";
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