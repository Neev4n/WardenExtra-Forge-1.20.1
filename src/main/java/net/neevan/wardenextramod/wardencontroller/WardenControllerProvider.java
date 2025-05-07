package net.neevan.wardenextramod.wardencontroller;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.neevan.wardenextramod.WardenExtraMod;
import net.neevan.wardenextramod.capability.ModCapabilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = "wardenextramod")
public class WardenControllerProvider implements ICapabilityProvider, ICapabilitySerializable<CompoundTag> {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(WardenExtraMod.MODID, "warden_controller");
    private final WardenController controller = new WardenController();
    private final LazyOptional<IWardenController> optional = LazyOptional.of(() -> controller);
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == ModCapabilities.WARDEN_CONTROLLER ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return controller.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        controller.deserializeNBT(nbt);
    }

    @SubscribeEvent
    public static void attachCapability(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(ID, new WardenControllerProvider());
            WardenExtraMod.LOGGER.info("Attached WardenController to a player entity."); // No .getName()
        }
    }


}
