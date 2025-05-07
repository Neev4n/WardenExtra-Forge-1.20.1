package net.neevan.wardenextramod.wardencontroller;

import net.minecraft.nbt.CompoundTag;
import net.neevan.wardenextramod.WardenExtraMod;

import java.util.UUID;

public class WardenController implements IWardenController {
    private UUID wardenUUID;
    @Override
    public void setWardenUUID(UUID uuid) {
        //WardenExtraMod.LOGGER.info("warden uuid set");
        this.wardenUUID = uuid;
    }



    @Override
    public UUID getWardenUUID() {
        return this.wardenUUID;
    }



    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        if (wardenUUID != null) {
            tag.putUUID("WardenUUID", wardenUUID);
        }
        return tag;
    }


    public void deserializeNBT(CompoundTag tag) {
        if (tag.contains("WardenUUID")) {
            this.wardenUUID = tag.getUUID("WardenUUID");
        } else {
            this.wardenUUID = null;
        }
    }
}