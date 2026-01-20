package studio.fantasyit.maid_rpg_task.behavior;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

public class ExtendedMeleeAttackBehavior extends Behavior<EntityMaid> {
    private static final double CUSTOM_ATTACK_RANGE = 20.0; // Custom attack range
    private static final int COOLDOWN_TICKS = 20;
    private int cooldown = 0;

    @Override
    protected boolean canStillUse(ServerLevel p_22545_, EntityMaid p_22546_, long p_22547_) {
        return super.canStillUse(p_22545_, p_22546_, p_22547_);
    }

    public ExtendedMeleeAttackBehavior() {
        super(Map.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        return getTarget(maid).filter(LivingEntity::isAlive).isPresent();
    }

    @Override
    protected void start(ServerLevel level, EntityMaid maid, long gameTime) {
        // Optional: set custom attack distance (if needed for other parts of mod logic)
        try {
            Field attackDistanceField = EntityMaid.class.getDeclaredField("attackDistance");
            attackDistanceField.setAccessible(true);
            attackDistanceField.setInt(maid, (int) CUSTOM_ATTACK_RANGE);
        } catch (Exception ignored) {
            // If field is final/private and can't be accessed, just fallback to range check
        }
    }

    @Override
    protected void tick(ServerLevel level, EntityMaid maid, long gameTime) {
        getTarget(maid).ifPresent(target -> {
            if (maid.distanceTo(target) <= CUSTOM_ATTACK_RANGE && cooldown <= 0) {
                maid.swing(maid.getUsedItemHand());
                maid.doHurtTarget(target);
                cooldown = COOLDOWN_TICKS;
            } else {
                if (cooldown > 0) cooldown--;
            }
        });
    }

    private Optional<LivingEntity> getTarget(EntityMaid maid) {
        return maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
    }
}
