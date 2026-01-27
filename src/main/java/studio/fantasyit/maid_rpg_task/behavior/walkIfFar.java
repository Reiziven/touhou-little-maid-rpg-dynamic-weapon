package studio.fantasyit.maid_rpg_task.behavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class walkIfFar {

    BehaviorControl<Mob> walkIfFar =
            BehaviorBuilder.create(brain -> brain.group(
                    brain.present(MemoryModuleType.ATTACK_TARGET),
                    brain.registered(MemoryModuleType.WALK_TARGET)
            ).apply(brain, (targetMem, walkMem) -> (level, mob, time) -> {

                LivingEntity target = (LivingEntity) targetMem.value();



                // 👇 AQUI é o if
                if (mob.distanceToSqr(target) > 1.0D) {
                    walkMem.set(new WalkTarget(target, 0.6f, 0));
                    return true;
                }

                return false;
            }));

}
