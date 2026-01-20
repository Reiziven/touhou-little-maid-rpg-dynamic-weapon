package studio.fantasyit.maid_rpg_task.behavior;

import com.github.tartaricacid.touhoulittlemaid.api.task.IMaidTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import studio.fantasyit.maid_rpg_task.task.MaidTankTask;

import java.util.Map;

public class TankRedirectBehavior extends Behavior<EntityMaid> {

    public TankRedirectBehavior() {
        super(Map.of());
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        IMaidTask current = maid.getTask();
        return current != null && current.getUid().equals(MaidTankTask.UID);
    }

    @Override
    protected void start(ServerLevel level, EntityMaid maid, long gameTime) {
        // No global static needed
    }

    @Override
    protected void stop(ServerLevel level, EntityMaid maid, long gameTime) {
        // Nothing to clear
    }

    @Override
    protected boolean canStillUse(ServerLevel level, EntityMaid maid, long gameTime) {
        IMaidTask current = maid.getTask();
        return current != null && current.getUid().equals(MaidTankTask.UID);
    }
}
