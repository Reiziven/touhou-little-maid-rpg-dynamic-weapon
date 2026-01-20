package studio.fantasyit.maid_rpg_task;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = MaidRpgTask.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue ENABLE_REVIVE = BUILDER
            .define("functions.revive", true);
    private static final ForgeConfigSpec.BooleanValue ENABLE_REVIVE_AGGRO = BUILDER
            .define("revive.aggro", true);
    private static final ForgeConfigSpec.BooleanValue ENABLE_REVIVE_TOTEM = BUILDER
            .define("revive.totem", true);
    private static final ForgeConfigSpec.BooleanValue ENABLE_MASTER= BUILDER
            .define("functions.master", false);
    static final ForgeConfigSpec SPEC = BUILDER.build();
;
    public static boolean enableReviveTask = false;


    public static boolean enableReviveAggro = false;
    public static boolean enableReviveTotem = false;
    public static boolean enableMasterTask = false;


    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        enableReviveTask = ENABLE_REVIVE.get();
        enableReviveAggro = ENABLE_REVIVE_AGGRO.get();
        enableReviveTotem = ENABLE_REVIVE_TOTEM.get();
        enableMasterTask = ENABLE_MASTER.get();
    }
}
