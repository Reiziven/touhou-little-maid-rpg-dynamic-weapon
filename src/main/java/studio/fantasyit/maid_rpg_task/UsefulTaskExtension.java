package studio.fantasyit.maid_rpg_task;

import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.LittleMaidExtension;
import com.github.tartaricacid.touhoulittlemaid.entity.data.TaskDataRegister;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;
import studio.fantasyit.maid_rpg_task.compat.PlayerRevive;
import studio.fantasyit.maid_rpg_task.data.MaidConfigKeys;
import studio.fantasyit.maid_rpg_task.data.MaidReviveConfig;
import studio.fantasyit.maid_rpg_task.task.MaidDPSTask;
import studio.fantasyit.maid_rpg_task.task.MaidMageTask;
import studio.fantasyit.maid_rpg_task.task.MaidMasterTask;
import studio.fantasyit.maid_rpg_task.task.MaidSupportTask;
import studio.fantasyit.maid_rpg_task.task.MaidTankTask;

@LittleMaidExtension
public class UsefulTaskExtension implements ILittleMaid {

    @Override
    public void addMaidTask(TaskManager manager) {
        ILittleMaid.super.addMaidTask(manager);

        // Sempre disponíveis
        manager.add(new MaidDPSTask());
        manager.add(new MaidTankTask());
        manager.add(new MaidMageTask());
        // Master task controlada por config

        if (Config.enableMasterTask) {
            manager.add(new MaidMasterTask());
        }
        // Revive task depende da config + compat ativa
        if (Config.enableReviveTask) {
            manager.add(new MaidSupportTask());
        }
    }

    @Override
    public void registerTaskData(TaskDataRegister register) {
        MaidConfigKeys.addKey(
                MaidReviveConfig.LOCATION,
                MaidReviveConfig.KEY = register.register(new MaidReviveConfig()),
                MaidReviveConfig.Data::getDefault
        );
    }
}
