package studio.fantasyit.maid_rpg_task.behavior;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import studio.fantasyit.maid_rpg_task.task.MaidTankTask;

public class TankRedirectHelper {
    private static EntityMaid globalTankMaid = null;

    public static void updateTankMaid(EntityMaid maid) {
        if (maid != null && maid.getTask() != null &&
                maid.getTask().getUid().equals(MaidTankTask.UID)) {
            globalTankMaid = maid;
        } else if (globalTankMaid == maid) {
            globalTankMaid = null;
        }
    }

    public static EntityMaid getGlobalTankMaid() {
        return globalTankMaid;
    }
}
