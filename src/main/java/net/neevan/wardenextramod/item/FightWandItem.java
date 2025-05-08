package net.neevan.wardenextramod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.warden.WardenAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neevan.wardenextramod.capability.ModCapabilities;

import java.util.UUID;

public class FightWandItem extends Item {
    public FightWandItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) return InteractionResultHolder.pass(player.getItemInHand(hand));

        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle().scale(50);
        Vec3 end = eye.add(look);

        AABB aabb = player.getBoundingBox().expandTowards(look).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, aabb,
                e -> e instanceof LivingEntity && !(e instanceof Player), look.lengthSqr());

        if (hit != null && hit.getEntity() instanceof LivingEntity target) {
            return setSonicTarget(player, target);
        }

        player.displayClientMessage(Component.literal("No valid target found."), true);
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    private InteractionResultHolder<ItemStack> setSonicTarget(Player player, LivingEntity target) {
        player.getCapability(ModCapabilities.WARDEN_CONTROLLER).ifPresent(controller -> {
            UUID uuid = controller.getWardenUUID();
            if (uuid == null) {
                player.displayClientMessage(Component.literal("No Warden selected."), true);
                return;
            }

            Entity entity = ((ServerLevel) player.level()).getEntity(uuid);
            if (!(entity instanceof Warden warden)) {
                player.displayClientMessage(Component.literal("Warden not found."), true);
                return;
            }

            if (!warden.canTargetEntity(target)) {
                player.displayClientMessage(Component.literal("Warden can't target that entity."), true);
                return;
            }

            // Set memory for sonic boom targeting
            Brain<?> brain = warden.getBrain();
            brain.setMemory(MemoryModuleType.ATTACK_TARGET, target);
            brain.eraseMemory(MemoryModuleType.WALK_TARGET);
            brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
            brain.eraseMemory(MemoryModuleType.SONIC_BOOM_COOLDOWN);
            brain.eraseMemory(MemoryModuleType.SONIC_BOOM_SOUND_DELAY);
            brain.eraseMemory(MemoryModuleType.SONIC_BOOM_SOUND_COOLDOWN);

            // Optional: clear anger to prevent melee pursuit
            warden.clearAnger(target);

            // Force activity update so AI reconsiders sonic boom
            WardenAi.updateActivity(warden);


            player.displayClientMessage(Component.literal("Warden is now using sonic boom on: " + target.getName().getString()), true);
        });

        return InteractionResultHolder.success(player.getItemInHand(InteractionHand.MAIN_HAND));
    }
}
