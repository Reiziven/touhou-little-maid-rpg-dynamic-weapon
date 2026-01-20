package studio.fantasyit.maid_rpg_task;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import studio.fantasyit.maid_rpg_task.registry.GuiRegistry;


// The value here should match an entry in the META-INF/mods.toml file
@Mod(MaidRpgTask.MODID)
public class MaidRpgTask {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "maid_rpg_task";

    @SuppressWarnings("removal")
    public MaidRpgTask() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        GuiRegistry.init(modEventBus);
    }
}
