package studio.fantasyit.maid_rpg_task.compat;

import net.minecraftforge.fml.ModList;

public class PlayerRevive {
    public static boolean isEnable(){
        return ModList.get().isLoaded("playerrevive");
    }
}
