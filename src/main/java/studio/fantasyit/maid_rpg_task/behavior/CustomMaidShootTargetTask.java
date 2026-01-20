package studio.fantasyit.maid_rpg_task.behavior;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import com.google.common.collect.ImmutableMap;

import java.util.Optional;
import java.util.Random;

public class CustomMaidShootTargetTask extends Behavior<EntityMaid> {

    private final int minTicks = 80;
    private final int maxTicks = 140;
    private int attackTime;
    private int seeTime;
    private final Random random = new Random();

    public CustomMaidShootTargetTask() {
        super(ImmutableMap.of(
                MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED,
                MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT
        ), 1200);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel worldIn, EntityMaid owner) {
        Optional<LivingEntity> memory = owner.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        if (memory.isEmpty()) return false;
        LivingEntity target = memory.get();
        return owner.isHolding(item -> item.getItem() instanceof BowItem) && owner.canSee(target);
    }

    @Override
    protected boolean canStillUse(ServerLevel worldIn, EntityMaid owner, long gameTimeIn) {
        return owner.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
                && this.checkExtraStartConditions(worldIn, owner);
    }

    @Override
    protected void start(ServerLevel worldIn, EntityMaid owner, long gameTimeIn) {
        owner.setSwingingArms(true);
        this.attackTime = minTicks + random.nextInt(maxTicks - minTicks + 1);
    }

    @Override
    protected void tick(ServerLevel worldIn, EntityMaid owner, long gameTime) {
        owner.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(target -> {
            owner.getLookControl().setLookAt(target.getX(), target.getY(), target.getZ());
            boolean canSee = owner.canSee(target);
            if (canSee != (this.seeTime > 0)) this.seeTime = 0;
            this.seeTime += canSee ? 1 : -1;
            boolean hasBow = owner.isHolding(item -> item.getItem() instanceof BowItem);
            if (!hasBow) return;
            if (owner.isUsingItem()) {
                int ticksUsingItem = owner.getTicksUsingItem();
                int threshold = 20 + attackTime;
                if (ticksUsingItem >= threshold) {
                    owner.performRangedAttack(target, BowItem.getPowerForTime(ticksUsingItem));
                    owner.stopUsingItem();
                    this.attackTime = minTicks + random.nextInt(maxTicks - minTicks + 1);
                }
            } else {
                if (--this.attackTime <= 0 && this.seeTime >= -60) {
                    owner.startUsingItem(InteractionHand.MAIN_HAND);
                }
            }
        });
    }

    @Override
    protected void stop(ServerLevel worldIn, EntityMaid owner, long gameTimeIn) {
        this.seeTime = 0;
        this.attackTime = -1;
        owner.stopUsingItem();
    }
}
