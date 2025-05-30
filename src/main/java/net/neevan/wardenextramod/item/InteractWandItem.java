package net.neevan.wardenextramod.item;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.neevan.wardenextramod.WardenExtraMod;
import net.neevan.wardenextramod.capability.ModCapabilities;

import java.util.List;
import java.util.UUID;

public class InteractWandItem extends Item {
    public InteractWandItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) return InteractionResultHolder.pass(player.getItemInHand(hand));

        ItemStack stack = player.getItemInHand(hand);

        ItemStack offhand = player.getOffhandItem();

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

        if (blockHit.getType() == HitResult.Type.BLOCK) {
            return handleBlockTarget(player, blockHit.getBlockPos(), blockHit);
        }

        player.displayClientMessage(Component.literal("No target found."), true);
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        // Always return SUCCESS on client side to allow server execution
        if (player.level().isClientSide) {
            WardenExtraMod.LOGGER.info("is clientside");
            return InteractionResult.SUCCESS;

        }

        // Check if it's a Warden
        if (target instanceof Warden) {
            Warden warden = (Warden) target;
            WardenExtraMod.LOGGER.info("is warden");
            //warden.addEffect(new MobEffectInstance(MobEffects.GLOWING,10000));
            player.getCapability(ModCapabilities.WARDEN_CONTROLLER).ifPresent(controller -> {
                controller.setWardenUUID(warden.getUUID());
                player.displayClientMessage(
                        Component.literal("Warden selected! UUID: " + warden.getUUID()),
                        true
                );
            });


            return InteractionResult.CONSUME; // Better than SUCCESS for interaction handling
        } else {
            player.displayClientMessage(
                    Component.literal("This is not a Warden."),
                    true
            );
            WardenExtraMod.LOGGER.info("not warden");
            return InteractionResult.PASS;
        }
    }

    private InteractionResultHolder<ItemStack> handleBlockTarget(Player player, BlockPos pos, BlockHitResult blockHitResult) {
        player.getCapability(ModCapabilities.WARDEN_CONTROLLER).ifPresent(controller -> {
            UUID wardenUUID = controller.getWardenUUID();
            ItemStack offhand = player.getOffhandItem();
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

            if (warden.blockPosition().closerThan(pos, 3.5)) {

                if (offhand.isEmpty()) {
                    BlockState state = player.level().getBlockState(pos);
                    BlockEntity blockEntity = player.level().getBlockEntity(pos);

                    // Only break if the block isn't air or indestructible
                    if (!state.isAir() && state.getDestroySpeed(player.level(), pos) >= 0) {
                        // Get drops
                        List<ItemStack> drops = Block.getDrops(state, (ServerLevel) player.level(), pos, blockEntity, player, ItemStack.EMPTY);
                        for (ItemStack drop : drops) {
                            player.getInventory().placeItemBackInInventory(drop);
                        }

                        // Play sound and break
                        player.level().destroyBlock(pos, false);
                        player.displayClientMessage(Component.literal("Warden broke the block for you."), true);
                    } else {
                        player.displayClientMessage(Component.literal("Block is not breakable."), true);
                    }

                    return;
                } else {
                    // Clone the UseOnContext for the offhand item
                    UseOnContext offhandContext = new UseOnContext(
                            player.level(),
                            player,
                            InteractionHand.OFF_HAND,
                            offhand,
                            blockHitResult
                    );

                    // Use the offhand item's behavior
                    InteractionResult result = offhand.getItem().useOn(offhandContext);

                    if (result.consumesAction()) {
                        // Apply durability loss or count reduction
                        if (offhand.isDamageableItem()) {
                            offhand.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(InteractionHand.OFF_HAND));
                        } else if (!player.getAbilities().instabuild) {
                            // Not in Creative – Minecraft handles this
                        } else if (offhand.getCount() > 1) {
                            offhand.shrink(1); // Reduce stack size by 1
                        } else if (offhand.getCount() == 1 && !offhand.isDamageableItem()) {
                            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                        }

                        player.displayClientMessage(Component.literal("Warden used " + offhand.getDisplayName().getString()), true);
                    } else {
                        player.displayClientMessage(Component.literal("Use failed or had no effect."), true);
                    }

                    // Warden faces the block
                    warden.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(pos));

                    // Play Warden attack animation (byte 4)
                    warden.level().broadcastEntityEvent(warden, (byte) 4);

                    // Optional: play impact sound
                    warden.playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 5.0F, 1.0F);
                }

            } else {
                player.displayClientMessage(Component.literal("Warden is too far from the block."), true);

            }
        });

        return InteractionResultHolder.success(player.getItemInHand(InteractionHand.MAIN_HAND));
    }


}
