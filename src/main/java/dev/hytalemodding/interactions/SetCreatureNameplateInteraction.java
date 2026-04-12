package dev.hytalemodding.interactions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import dev.hytalemodding.components.PkmnCaptureMetadata;
import dev.hytalemodding.components.PkmnStatsComponent;
import dev.hytalemodding.util.PkmnStatUtils;


//TODO: clean up, use shared logic, fix lvl1 bug

public class SetCreatureNameplateInteraction extends SimpleInstantInteraction {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
        public static final BuilderCodec<SetCreatureNameplateInteraction> CODEC = BuilderCodec.builder(
        SetCreatureNameplateInteraction.class, 
        SetCreatureNameplateInteraction::new, 
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
            fail(interactionContext);
            return;
        }
        Store<EntityStore> store = commandBuffer.getExternalData().getStore();
        Ref<EntityStore> ref = interactionContext.getEntity();
        if (ref == null) {
            fail(interactionContext);
            return;
        }
        NPCEntity npcEntity = store.getComponent(ref,NPCEntity.getComponentType());
        if(npcEntity==null){
            fail(interactionContext);
            return;
        }
        String roleName = npcEntity.getRoleName();
        if(!filterByRoleName(roleName)) {
            fail(interactionContext);
            return;
        }

        String species = speciesFromRole(roleName);
        // LOGGER.atInfo().log("Add nameplate to: "+species);
        PkmnCaptureMetadata metadata = PkmnStatUtils.captureMetadata(commandBuffer,ref);
        PkmnStatsComponent pkmnStats = PkmnStatUtils.fromMetadata(metadata);

        PkmnStatUtils.apply(store, commandBuffer, ref, pkmnStats);

        String nameplateString = buildNamplateString(roleName,pkmnStats,null);
        commandBuffer.putComponent(ref,Nameplate.getComponentType(),new Nameplate(nameplateString));
        next(interactionContext);
        return;
    }

    private void next(@Nonnull InteractionContext interactionContext){
        interactionContext.getState().state = InteractionState.NotFinished;
    }

    private void fail(@Nonnull InteractionContext interactionContext){
        interactionContext.getState().state = InteractionState.Failed;
    }

    public static  String buildNamplateString(
        @Nonnull String npcRoleId,
        @Nonnull PkmnStatsComponent stats,
        @Nullable String playerUuid
    ){
        String name = speciesFromRole(npcRoleId);
        var nickname = stats.getNickname();
        if(nickname != null && !nickname.isBlank()){
            name=nickname;
        }
        if(isTamed(npcRoleId)){
            name = "(-o-) "+name;
        }
        String lvl = String.valueOf(stats.getLevel());
        return name+" ["+lvl+"]";
    }

    public static boolean isTamed(@Nonnull String npcRoleId){
        return npcRoleId.endsWith("_Tamed");
    }
    
    public static String speciesFromRole(@Nonnull String npcRoleId){
        String species = npcRoleId;
        if(npcRoleId.startsWith("Pkmn_")){
            species = npcRoleId.replaceFirst("Pkmn_", "");
        }
        if(isTamed(npcRoleId)){
            species = species.replaceFirst("_Tamed", "");
        }
        species = species.replaceAll("_"," ");
        return species;
    }
    
    public static boolean filterByRoleName(String roleName) {
        if(roleName.startsWith("Pkmn_")) return true;
        if(roleName.startsWith("Lizard_Ground_")) return true;
        return false;
    }

}
