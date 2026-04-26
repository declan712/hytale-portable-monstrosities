package dev.hytalemodding.interactions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;

public class FillFluidContainerInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<FillFluidContainerInteraction> CODEC = BuilderCodec.builder(
        FillFluidContainerInteraction.class, FillFluidContainerInteraction::new, SimpleInstantInteraction.CODEC
    ).build();

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    protected void firstRun(InteractionType interactionType, InteractionContext interactionContext, CooldownHandler cooldownHandler) {
        CommandBuffer<EntityStore> commandBuffer = interactionContext.getCommandBuffer();

        if (commandBuffer == null) {
            interactionContext.getState().state = InteractionState.Failed;
            // LOGGER.atInfo().log("CommandBuffer is null");
            return;
        }

        World world = commandBuffer.getExternalData().getWorld(); // just to show how to get the world if needed
        Store<EntityStore> store = commandBuffer.getExternalData().getStore(); // just to show how to get the store if needed

        Ref<EntityStore> ref = interactionContext.getEntity();
        Player player = commandBuffer.getComponent(ref, Player.getComponentType());
        if (player == null) {
            interactionContext.getState().state = InteractionState.Failed;
            // LOGGER.atInfo().log("Player is null");
            return;
        }

        ItemStack itemStack = interactionContext.getHeldItem();
        if (itemStack == null) {
            interactionContext.getState().state = InteractionState.Failed;
            // LOGGER.atInfo().log("ItemStack is null");
            return;
        }
        double  durability = itemStack.getDurability();
        double  maxDurability = itemStack.getMaxDurability();

        BlockPosition  targetPos = interactionContext.getTargetBlock();
        int targetBlock = world.getBlock(targetPos.x,targetPos.y,targetPos.z);
        var blockType = world.getBlockType(targetPos.x,targetPos.y,targetPos.z).getItem().getBlockId();
        var targetFluid = world.getFluidId(targetPos.x,targetPos.y,targetPos.z);

        int aboveTargetBlock = world.getBlock(targetPos.x,targetPos.y+1,targetPos.z);
        var aboveTargetFluid = world.getFluidId(targetPos.x,targetPos.y+1,targetPos.z);
        var aboveBlockType = world.getBlockType(targetPos.x,targetPos.y+1,targetPos.z).getId();

        
        TransformComponent playerTransformComponent = store.getComponent(player.getReference(), EntityModule.get().getTransformComponentType());
        var playerPos = playerTransformComponent.getPosition();
        // var targetLabel = interactionContext.getLabel(targetBlock);

    
        
        // 6 = lava_source
        // 7 = water source
        // 8 = water
        try{

            player.sendMessage(Message.raw("You have used the custom item: " + itemStack.getItemId() + " ("+durability+"/"+maxDurability+")"));
            player.sendMessage(Message.raw("Your target block is: "+blockType+"  (" + targetPos.x +","+ targetPos.y+","+ targetPos.z + ")"));
            player.sendMessage(Message.raw("above your target block is: "+aboveBlockType+"  (" + targetPos.x +","+ (targetPos.y+1)+","+ targetPos.z + ")"));
            player.sendMessage(Message.raw("Your target fluid is: "+targetFluid+"  (" + targetPos.x +","+ targetPos.y+","+ targetPos.z + ")"));
            player.sendMessage(Message.raw("above your target block is: "+aboveTargetFluid+"  (" + targetPos.x +","+ (targetPos.y+1)+","+ targetPos.z + ")"));
        }catch (Exception e) {
            // TODO: handle exception
            player.sendMessage(Message.raw(e.toString()));
        }



        try {
            var a = itemStack.getItemId().toString();
            var b = "*Container_Tank_State_Filled_Water";
            if(aboveTargetFluid!=7){ player.sendMessage(Message.raw(aboveTargetFluid+" != "+7)); }
            if(aboveTargetFluid==7 && a.equals(b)){
                player.sendMessage(Message.raw("Found water!"));
                // interactionContext.setHeldItem(itemStack.withDurability(durability+1));
                interactionContext.getState().state = InteractionState.NotFinished;
                return;
            }
        } catch (Exception e) {
            player.sendMessage(Message.raw(e.toString()));
        }
        // if(targetLabel != null){
        //     player.sendMessage(Message.raw("Your target block is: "+targetLabel.toString()));
        // }

        // var  targetEntity= interactionContext.getTargetEntity();
        // if(targetEntity!=null){
        //     player.sendMessage(Message.raw("Your target entity is: " + targetEntity));
        // }
        


        interactionContext.getState().state = InteractionState.Failed;
        return;


        
    }

    
}