package net.neevan.wardenextramod.event;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.neevan.wardenextramod.WardenExtraMod;
import net.neevan.wardenextramod.item.ModItems;

@Mod.EventBusSubscriber(modid = WardenExtraMod.MODID)
public class InteractionEventHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack mainHand = event.getItemStack();
        // If the player is holding the InteractWandItem
        if(mainHand.is(ModItems.INTERACT_WAND.get()) || mainHand.is(ModItems.MOVEMENT_WAND.get()) ){
            event.setCanceled(true); // Cancels entire pipeline
            event.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);
            event.setUseItem(net.minecraftforge.eventbus.api.Event.Result.DENY);
        }

    }
}
