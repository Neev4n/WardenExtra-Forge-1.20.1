package net.neevan.wardenextramod.capability;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.neevan.wardenextramod.wardencontroller.IWardenController;

public class ModCapabilities {
    public static Capability<IWardenController> WARDEN_CONTROLLER;

    public static void register() {
        WARDEN_CONTROLLER = CapabilityManager.get(new CapabilityToken<>() {});
    }
}

