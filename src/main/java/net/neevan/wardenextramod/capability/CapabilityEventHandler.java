package net.neevan.wardenextramod.capability;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.neevan.wardenextramod.WardenExtraMod;

@Mod.EventBusSubscriber(modid = "wardenextramod")
public class CapabilityEventHandler {

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        WardenExtraMod.LOGGER.info("Player cloned, transferring capability...");
        event.getOriginal().getCapability(ModCapabilities.WARDEN_CONTROLLER).ifPresent(oldCap -> {
            event.getEntity().getCapability(ModCapabilities.WARDEN_CONTROLLER).ifPresent(newCap -> {
                newCap.setWardenUUID(oldCap.getWardenUUID());
            });
        });
    }
}
