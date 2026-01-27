
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
import com.github.tartaricacid.touhoulittlemaid.item.ItemHakureiGohei;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

import java.util.Optional;

public class MasterWeaponSwitchBehavior extends Behavior<EntityMaid> {
    private static final double RANGED_DISTANCE_THRESHOLD = 8.0;
    private ItemStack lastMeleeWeapon = ItemStack.EMPTY;

    public MasterWeaponSwitchBehavior() {
        super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected void tick(ServerLevel level, EntityMaid maid, long gameTime) {
        Optional<LivingEntity> targetOpt = maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        if (targetOpt.isEmpty()) return;

        handleOffhandRanged(maid);

        LivingEntity target = targetOpt.get();
        double distanceSq = maid.distanceToSqr(target);
        ItemStack mainHand = maid.getMainHandItem();

        if (distanceSq > RANGED_DISTANCE_THRESHOLD * RANGED_DISTANCE_THRESHOLD) {
            // Distance > 8 blocks: Try to use ranged weapon
            if (!(mainHand.getItem() instanceof ProjectileWeaponItem)) {
                if (isMeleeWeapon(mainHand)) {
                    lastMeleeWeapon = mainHand.copy();
                }
                switchToRanged(maid);
            }
        } else {
            // Distance <= 8 blocks: Try to use melee weapon
            if (mainHand.getItem() instanceof ProjectileWeaponItem || mainHand.isEmpty()) {
                switchToMelee(maid);
            }
        }
    }

    private void switchToRanged(EntityMaid maid) {
        CombinedInvWrapper handler = maid.getAvailableInv(true);
        int bestSlot = -1;

        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.getItem() instanceof CrossbowItem) {
                bestSlot = i;
                break;
            } else if (stack.getItem() instanceof BowItem && bestSlot == -1) {
                bestSlot = i;
            }
        }

        if (bestSlot != -1) {
            swapWithMainHand(maid, bestSlot);
        }
    }

    private void switchToMelee(EntityMaid maid) {
        CombinedInvWrapper handler = maid.getAvailableInv(true);

        if (!lastMeleeWeapon.isEmpty()) {
            for (int i = 0; i < handler.getSlots(); i++) {
                if (ItemStack.isSameItemSameTags(handler.getStackInSlot(i), lastMeleeWeapon)) {
                    swapWithMainHand(maid, i);
                    return;
                }
            }
        }

        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (isMeleeWeapon(stack)) {
                swapWithMainHand(maid, i);
                return;
            }
        }
    }

    private void swapWithMainHand(EntityMaid maid, int slot) {
        CombinedInvWrapper handler = maid.getAvailableInv(true);
        ItemStack toEquip = handler.getStackInSlot(slot);
        ItemStack current = maid.getMainHandItem();

        handler.setStackInSlot(slot, current.copy());
        maid.setItemInHand(InteractionHand.MAIN_HAND, toEquip.copy());
    }

    private boolean isMeleeWeapon(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof SwordItem || item instanceof AxeItem || item instanceof TridentItem;
    }

    private void handleOffhandRanged(EntityMaid maid) {
        ItemStack off = maid.getOffhandItem();
        if (!off.isEmpty() && ((off.getItem() instanceof BowItem) || (off.getItem() instanceof CrossbowItem) || ItemHakureiGohei.isGohei(off))) {
            CombinedInvWrapper handler = maid.getAvailableInv(true);
            boolean moved = false;
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack slot = handler.getStackInSlot(i);
                if (slot.isEmpty()) {
                    handler.setStackInSlot(i, off.copy());
                    maid.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                    moved = true;
                    break;
                }
            }
            if (!moved) {
                ItemStack current = maid.getMainHandItem();
                maid.setItemInHand(InteractionHand.MAIN_HAND, off.copy());
                maid.setItemInHand(InteractionHand.OFF_HAND, current.copy());
            }
        }
    }
}
