package studio.fantasyit.maid_rpg_task.behavior;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.*;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import com.github.tartaricacid.touhoulittlemaid.item.ItemHakureiGohei;

import java.util.Optional;
import java.util.function.Predicate;

public class MaidSwitchWeaponBehavior extends Behavior<EntityMaid> {
    private static final double RANGED_DISTANCE_THRESHOLD_SQR = 80.0;

    public MaidSwitchWeaponBehavior() {
        super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        Optional<LivingEntity> targetOpt = maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        if (targetOpt.isEmpty()) return false;

        LivingEntity target = targetOpt.get();
        double distanceSq = maid.distanceToSqr(target);
        ItemStack mainHand = maid.getMainHandItem();
        ItemStack offHand = maid.getOffhandItem();


        if (distanceSq > RANGED_DISTANCE_THRESHOLD_SQR) {
            // Distance > 8 blocks: Need Ranged
            if (isRangedWeapon(mainHand)) return false; // Already have it
            if (isRangedWeapon(offHand)) return true; // Can swap offhand to main
            return findWeaponSlot(maid, this::isRangedWeapon) != -1; // Only start if we have one
        } else {
            // Distance <= 8 blocks: Need Assault (Melee)
            if (isAssaultWeapon(mainHand)) return false; // Already have it
            if (isAssaultWeapon(offHand)) return true; // Can swap offhand to main
            return findWeaponSlot(maid, this::isAssaultWeapon) != -1; // Only start if we have one
        }
    }

    @Override
    protected void start(ServerLevel level, EntityMaid maid, long gameTime) {
        Optional<LivingEntity> targetOpt = maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        if (targetOpt.isEmpty()) return;

        LivingEntity target = targetOpt.get();
        double distanceSq = maid.distanceToSqr(target);

        if (distanceSq > RANGED_DISTANCE_THRESHOLD_SQR) {
            switchToWeapon(maid, this::isRangedWeapon);
        } else {
            switchToWeapon(maid, this::isAssaultWeapon);
        }
    }

    // We do NOT override canStillUse, so it defaults to false.
    // This ensures the behavior runs once (start -> stop) and then re-evaluates next tick via checkExtraStartConditions.

    private int findWeaponSlot(EntityMaid maid, Predicate<ItemStack> predicate) {
        CombinedInvWrapper handler = maid.getAvailableInv(true);
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (predicate.test(stack)) {
                return i;
            }
        }
        return -1;
    }

    private void switchToWeapon(EntityMaid maid, Predicate<ItemStack> predicate) {
        // Prefer offhand if it already has the desired weapon
        ItemStack off = maid.getOffhandItem();
        if (predicate.test(off)) {
            ItemStack current = maid.getMainHandItem();
            maid.setItemInHand(InteractionHand.MAIN_HAND, off.copy());
            maid.setItemInHand(InteractionHand.OFF_HAND, current.copy());
            return;
        }

        CombinedInvWrapper handler = maid.getAvailableInv(true);
        int bestSlot = findWeaponSlot(maid, predicate);

        if (bestSlot != -1) {
            ItemStack toEquip = handler.getStackInSlot(bestSlot);
            ItemStack current = maid.getMainHandItem();
            
            // Swap items
            handler.setStackInSlot(bestSlot, current.copy());
            maid.setItemInHand(InteractionHand.MAIN_HAND, toEquip.copy());
            
            // Play a sound or particles? Optional.
        }
    }

    private boolean isRangedWeapon(ItemStack stack) {
        return stack.getItem() instanceof BowItem
                || stack.getItem() instanceof CrossbowItem
                || ItemHakureiGohei.isGohei(stack);
    }

    private boolean isAssaultWeapon(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof SwordItem || item instanceof AxeItem || item instanceof TridentItem;
    }
}
