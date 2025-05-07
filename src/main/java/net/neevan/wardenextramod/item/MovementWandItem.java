package net.neevan.wardenextramod.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.neevan.wardenextramod.capability.ModCapabilities;

import java.util.UUID;

public class MovementWandItem extends Item {
    public MovementWandItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) return InteractionResultHolder.pass(player.getItemInHand(hand));

        ItemStack stack = player.getItemInHand(hand);

        // Perform raycast to check for hit
        double reach = 100.0D;
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getLookAngle().scale(reach);
        Vec3 targetVec = eyePos.add(lookVec);

        // Block raycast
        BlockHitResult blockHit = level.clip(new ClipContext(eyePos, targetVec, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));

        // Entity raycast
        AABB aabb = player.getBoundingBox().expandTowards(lookVec).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, eyePos, targetVec, aabb, e ->
                !e.isSpectator() && e.isPickable() && e instanceof LivingEntity, reach * reach
        );

        if (entityHit != null && entityHit.getEntity() instanceof LivingEntity livingTarget) {
            return handleInteraction(player, livingTarget, hand);
        }

        if (blockHit.getType() == HitResult.Type.BLOCK) {
            return handleBlockTarget(player, blockHit.getBlockPos());
        }

        player.displayClientMessage(Component.literal("No target found."), true);
        return InteractionResultHolder.pass(stack);
    }


    @Override
    public InteractionResult interactLivingEntity(ItemStack pStack, Player pPlayer, LivingEntity pInteractionTarget, InteractionHand pUsedHand) {
        return handleInteraction(pPlayer, pInteractionTarget, pUsedHand).getResult();
    }

    private InteractionResultHolder<ItemStack> handleInteraction(Player player, LivingEntity target, InteractionHand hand) {
        player.getCapability(ModCapabilities.WARDEN_CONTROLLER).ifPresent(controller -> {
            UUID wardenUUID = controller.getWardenUUID();
            if (wardenUUID == null) {
                player.displayClientMessage(Component.literal("No Warden selected."), true);
                return;
            }

            Entity maybeWarden = ((ServerLevel) player.level()).getEntity(wardenUUID);
            if (!(maybeWarden instanceof Warden warden)) {
                player.displayClientMessage(Component.literal("Warden not found or invalid."), true);
                return;
            }

            if (warden.canTargetEntity(target)) {
                warden.getBrain().eraseMemory(MemoryModuleType.ROAR_TARGET);
                warden.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
                warden.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
                warden.increaseAngerAt(target);
                player.displayClientMessage(Component.literal("Warden is now attacking: " + target.getName().getString()), true);
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING,120*20));
            }
        });

        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    private InteractionResultHolder<ItemStack> handleBlockTarget(Player player, BlockPos pos) {
        player.getCapability(ModCapabilities.WARDEN_CONTROLLER).ifPresent(controller -> {
            UUID wardenUUID = controller.getWardenUUID();
            //UUID targetUUID = controller.getTargetUUID();
            if (wardenUUID == null) {
                player.displayClientMessage(Component.literal("No Warden selected."), true);
                return;
            }

            Entity maybeWarden = ((ServerLevel) player.level()).getEntity(wardenUUID);
            if (!(maybeWarden instanceof Warden warden)) {
                player.displayClientMessage(Component.literal("Warden not found or invalid."), true);
                return;
            }

            if(warden.getTarget() != null){
                warden.clearAnger(warden.getTarget());
                player.displayClientMessage(Component.literal("Anger cleared from previous entity"), true);
            }

            WalkTarget walkTarget = new WalkTarget(pos, 1.2F, 0);
            warden.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            warden.getBrain().setMemory(MemoryModuleType.WALK_TARGET, walkTarget);
            player.displayClientMessage(Component.literal("Warden walking to block at " + pos.toShortString()), true);
        });

        return InteractionResultHolder.success(player.getItemInHand(InteractionHand.MAIN_HAND));
    }
}
