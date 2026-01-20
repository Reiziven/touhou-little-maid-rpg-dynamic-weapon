package studio.fantasyit.maid_rpg_task.behavior;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.*;

public class StunBehavior extends Behavior<EntityMaid> {
    private static final int COOLDOWN_TICKS = 200; // ~10 seconds cooldown
    private static final int STUN_DURATION = 70;   // 3.5 seconds in ticks

    private long lastStunTime = 0;
    private boolean stunned = false;

    public StunBehavior() {
        super(Map.of(), COOLDOWN_TICKS);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        ItemStack offhand = maid.getOffhandItem();
        return offhand.getItem() == Items.SHIELD && (level.getGameTime() - lastStunTime) >= COOLDOWN_TICKS && !stunned;
    }

    @Override
    protected void start(ServerLevel level, EntityMaid maid, long gameTime) {
        AABB area = new AABB(maid.blockPosition()).inflate(6);
        UUID ownerId = maid.getOwnerUUID();

        List<TamableAnimal> pets = ownerId == null ? List.of() : level.getEntitiesOfClass(
                TamableAnimal.class, area,
                pet -> pet.isAlive() && Objects.equals(pet.getOwnerUUID(), ownerId)
        );

        List<Mob> allMobs = level.getEntitiesOfClass(Mob.class, area, mob -> {
            Entity target = mob.getTarget();
            if (target == null) return false;
            if (target.equals(maid)) return true;
            if (maid.getOwner() != null && target.equals(maid.getOwner())) return true;
            for (TamableAnimal pet : pets) {
                if (target.equals(pet)) return true;
            }
            return false;
        });

        for (Mob mob : allMobs) {
            mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, STUN_DURATION, 4, false, true));           // Level 5
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, STUN_DURATION, 254, false, true)); // Level 255
            mob.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, STUN_DURATION, 254, false, true));      // Level 255

            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    mob.getX(), mob.getY() + mob.getBbHeight() / 2.0, mob.getZ(),
                    10, 0.3, 0.3, 0.3, 0.02);
        }

        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                maid.getX(), maid.getY() + 1, maid.getZ(),
                20, 0.5, 0.5, 0.5, 0.05);

        lastStunTime = gameTime;
        stunned = true;
    }

    @Override
    protected void tick(ServerLevel level, EntityMaid maid, long gameTime) {
        // Clear mobs currently targeting the maid (defensive mechanic)
        AABB area = new AABB(maid.blockPosition()).inflate(8);
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, area, mob -> mob.getTarget() == maid);
        for (Mob mob : mobs) {
            mob.setTarget(null);
        }

        stunned = false; // End behavior after 1 tick
    }

    @Override
    protected boolean canStillUse(ServerLevel level, EntityMaid maid, long gameTime) {
        return stunned;
    }

    @Override
    protected void stop(ServerLevel level, EntityMaid maid, long gameTime) {
        stunned = false;
    }
}
