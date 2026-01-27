package studio.fantasyit.maid_rpg_task.behavior;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.phys.AABB;

import java.util.*;

public class TankAggroBehavior extends Behavior<EntityMaid> {

    private static class AggroMaidGoal extends TargetGoal {
        private final EntityMaid maid;

        public AggroMaidGoal(Mob mob, EntityMaid maid) {
            super(mob, true);
            this.maid = maid;
        }

        @Override
        public boolean canUse() {
            return maid.isAlive()
                    && maid.level() == mob.level()
                    && !mob.isDeadOrDying();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            if (!maid.isAlive() || maid.level() != mob.level()) return;
            mob.setTarget(maid);
        }

        @Override
        public void stop() {
            if (mob.getTarget() == maid) {
                mob.setTarget(null);
            }
        }
    }

    private final Map<UUID, AggroMaidGoal> activeAggroGoals = new HashMap<>();

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
        for (var entry : activeAggroGoals.entrySet()) {
            Entity e = level.getEntity(entry.getKey());
            if (e instanceof Mob mob) {
                mob.targetSelector.removeGoal(entry.getValue());
                if (mob.getTarget() == maid) {
                    mob.setTarget(null);
                }
            }
        }
        activeAggroGoals.clear();
    }

    @Override
    protected boolean canStillUse(ServerLevel level, EntityMaid maid, long gameTime) {
        return true;
    }

    private void redirectAggro(ServerLevel level, EntityMaid maid) {
        UUID ownerId = maid.getOwnerUUID();
        if (ownerId == null) return;

        AABB area = AABB.ofSize(maid.position(), 16, 8, 16);

        // Predicate SIMPLES e seguro
        List<Mob> mobs = level.getEntitiesOfClass(
                Mob.class,
                area,
                mob -> mob.isAlive() && !mob.isNoAi()
        );

        for (Mob mob : mobs) {
            if (activeAggroGoals.containsKey(mob.getUUID())) continue;

            // 👇 REGRA DE OURO: só puxa aggro se já existir alvo
            Entity currentTarget = mob.getTarget();
            if (currentTarget == null) continue;

            AggroMaidGoal goal = new AggroMaidGoal(mob, maid);
            mob.targetSelector.addGoal(0, goal);
            activeAggroGoals.put(mob.getUUID(), goal);
        }
    }
}
