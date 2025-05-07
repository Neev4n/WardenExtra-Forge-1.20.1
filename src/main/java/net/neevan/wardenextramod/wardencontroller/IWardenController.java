package net.neevan.wardenextramod.wardencontroller;


import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.UUID;

public interface IWardenController extends INBTSerializable<CompoundTag> {
    void setWardenUUID(UUID uuid);
    UUID getWardenUUID();
}
