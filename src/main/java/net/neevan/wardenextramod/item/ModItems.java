package net.neevan.wardenextramod.item;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.neevan.wardenextramod.WardenExtraMod;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, WardenExtraMod.MODID);

    public static final RegistryObject<Item> MOVEMENT_WAND = ITEMS.register("movement_wand",
            () -> new MovementWandItem(new Item.Properties()));
    public static final RegistryObject<Item> INTERACT_WAND = ITEMS.register("interact_wand",
            () -> new InteractWandItem(new Item.Properties()));

    public static final RegistryObject<Item> FIGHT_WAND = ITEMS.register("fight_wand",
            () -> new FightWandItem(new Item.Properties()));
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
