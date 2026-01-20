package studio.fantasyit.maid_rpg_task.behavior;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.phys.AABB;

import java.util.*;

public class TankAggroBehavior extends Behavior<EntityMaid> {
    private static class AggroMaidGoal extends TargetGoal {
        private final Mob mob;
        private final EntityMaid maid;
        private final List<Goal> originalGoals;

        public AggroMaidGoal(Mob mob, EntityMaid maid, List<Goal> originalGoals) {
            super(mob, false);
            this.mob = mob;
            this.maid = maid;
            this.originalGoals = originalGoals;
        }

        @Override
        public boolean canUse() {
            return maid.isAlive() && mob.canAttack(maid) && maid.level() == mob.level();
        }

        @Override
        public boolean canContinueToUse() {
            return maid.isAlive() && mob.canAttack(maid) && maid.level() == mob.level();
        }

        @Override
        public void start() {
            mob.setTarget(maid);
        }

        @Override
        public void tick() {
            if (!maid.isAlive() || maid.level() != mob.level()) {
                cleanup();
                return;
            }
            if (mob.getTarget() != maid) {
                mob.setTarget(maid);
            }
        }

        private void cleanup() {
            mob.setTarget(null);
            mob.targetSelector.removeGoal(this);
            for (Goal goal : originalGoals) {
                mob.targetSelector.addGoal(1, goal);
            }
        }

        public boolean isSameMaid(EntityMaid otherMaid) {
            return this.maid.getUUID().equals(otherMaid.getUUID());
        }
    }

    private final Set<UUID> aggroEntities = new HashSet<>();
    private final Map<UUID, List<Goal>> originalGoalsMap = new HashMap<>();

    public TankAggroBehavior() {
        super(Map.of(), 200);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        return true;
    }

    @Override
    protected void start(ServerLevel level, EntityMaid maid, long gameTime) {
        redirectAggro(level, maid);
    }

    @Override
    protected void tick(ServerLevel level, EntityMaid maid, long gameTime) {
        if (gameTime % 20 == 0) {
            redirectAggro(level, maid);
        }
    }

    @Override
    protected void stop(ServerLevel level, EntityMaid maid, long gameTime) {
        for (UUID uuid : aggroEntities) {
            Entity entity = level.getEntity(uuid);
            if (entity instanceof Mob mob && mob.isAlive()) {
                mob.targetSelector.removeAllGoals(goal -> goal instanceof AggroMaidGoal g && g.isSameMaid(maid));
                List<Goal> originalGoals = originalGoalsMap.get(mob.getUUID());
                if (originalGoals != null) {
                    for (Goal goal : originalGoals) {
                        mob.targetSelector.addGoal(1, goal);
                    }
                }
            }
        }
        aggroEntities.clear();
        originalGoalsMap.clear();
    }

    @Override
    protected boolean canStillUse(ServerLevel level, EntityMaid maid, long gameTime) {
        return true;
    }

    private void redirectAggro(ServerLevel level, EntityMaid maid) {
        UUID ownerId = maid.getOwnerUUID();
        if (ownerId == null) return;

        AABB area = AABB.ofSize(maid.position(), 16, 8, 16);

        List<TamableAnimal> pets = level.getEntitiesOfClass(TamableAnimal.class, area,
                p -> p.isAlive() && Objects.equals(p.getOwnerUUID(), ownerId));

        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, area, mob -> {
            Entity target = mob.getTarget();
            if (target == null) return false;
            if (target.equals(maid)) return true;
            if (maid.getOwner() != null && target.equals(maid.getOwner())) return true;
            for (TamableAnimal pet : pets) {
                if (target.equals(pet)) return true;
            }
            return false;
        });

        for (Mob mob : mobs) {
            if (!aggroEntities.contains(mob.getUUID())) {
                List<Goal> originalGoals = new ArrayList<>();
                List<WrappedGoal> toRemove = new ArrayList<>();

                for (WrappedGoal wrapped : mob.targetSelector.getAvailableGoals()) {
                    Goal goal = wrapped.getGoal();
                    if (goal instanceof TargetGoal) {
                        originalGoals.add(goal);
                        toRemove.add(wrapped);
                    }
                }
                for (WrappedGoal wrapped : toRemove) {
                    mob.targetSelector.removeGoal(wrapped.getGoal());
                }

                AggroMaidGoal aggroGoal = new AggroMaidGoal(mob, maid, originalGoals);
                mob.targetSelector.addGoal(0, aggroGoal);
                aggroEntities.add(mob.getUUID());
                originalGoalsMap.put(mob.getUUID(), originalGoals);
            }
        }
    }
}
