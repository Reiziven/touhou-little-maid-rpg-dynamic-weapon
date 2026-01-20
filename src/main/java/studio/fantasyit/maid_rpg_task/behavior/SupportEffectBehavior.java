package studio.fantasyit.maid_rpg_task.behavior;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SupportEffectBehavior extends Behavior<EntityMaid> {
    public SupportEffectBehavior() {
        super(Map.of(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        if (maid.getExperience() <= 0) return false;
        LivingEntity player = maid.getOwner();
        if (player == null) return false;

        Optional<NearestVisibleLivingEntities> memory = maid.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
        return memory.map(list -> list
                .find(entity -> entity instanceof Mob)
                .map(Mob -> (Mob) Mob)
                .map(Mob::getTarget)
                .anyMatch(target -> target != null && (target.equals(player) || isTamedByPlayer(target, player)))
        ).orElse(false);
    }

    @Override
    protected void tick(ServerLevel level, EntityMaid maid, long gameTime) {
        super.tick(level, maid, gameTime);
        if (gameTime % 20 == 0) {
            maid.setExperience(maid.getExperience() - 1);
            reApplyEffects(level, maid);

            // Emit particle effect from maid
            level.sendParticles(
                    ParticleTypes.ENCHANT,
                    maid.getX(), maid.getY() + 1, maid.getZ(),
                    5, // count
                    0.3, 0.3, 0.3, // x, y, z offset
                    0.1 // speed
            );
        }
    }

    private void reApplyEffects(ServerLevel level, EntityMaid maid) {
        LivingEntity player = maid.getOwner();
        if (player == null) return;

        List<Entity> entities = level.getEntities(maid, AABB.ofSize(maid.position(), 16, 16, 16));

        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) continue;

            if (entity instanceof Mob mob && isTargetingPlayerOrAllies(mob, player)) {
                mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 0, false, true));
            }

            if (isAlly(living, player)) {
                living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 90, 0, false, false));
                living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 90, 1, false, false));
            }
        }
    }

    private boolean isTargetingPlayerOrAllies(Mob mob, LivingEntity player) {
        if (mob == null || !mob.isAlive()) return false;
        LivingEntity target = mob.getTarget();
        return target != null && (target.equals(player) || isTamedByPlayer(target, player) || isPlayerMaid(target, player));
    }

    private boolean isAlly(LivingEntity entity, LivingEntity player) {
        return entity.equals(player) || isTamedByPlayer(entity, player) || isPlayerMaid(entity, player);
    }

    private boolean isTamedByPlayer(LivingEntity entity, LivingEntity player) {
        return entity instanceof TamableAnimal animal && player.equals(animal.getOwner());
    }

    private boolean isPlayerMaid(LivingEntity entity, LivingEntity player) {
        return entity instanceof EntityMaid maid && player.equals(maid.getOwner());
    }

    @Override
    protected void stop(ServerLevel level, EntityMaid maid, long gameTime) {}

    @Override
    protected boolean canStillUse(ServerLevel level, EntityMaid maid, long gameTime) {
        return checkExtraStartConditions(level, maid);
    }

    @Override
    protected boolean timedOut(long time) {
        return false;
    }
}
