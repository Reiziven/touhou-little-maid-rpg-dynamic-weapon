package studio.fantasyit.maid_rpg_task.registry;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import studio.fantasyit.maid_rpg_task.MaidRpgTask;
import studio.fantasyit.maid_rpg_task.menu.MaidReviveConfigGui;

public class GuiRegistry {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MaidRpgTask.MODID);
    public static final RegistryObject<MenuType<MaidReviveConfigGui.Container>> MAID_REVIVE_CONFIG_GUI = MENU_TYPES.register("maid_revive_config_gui",
            () -> IForgeMenuType.create((windowId, inv, data) -> new MaidReviveConfigGui.Container(windowId, inv, data.readInt())));

    public static void init(IEventBus modEventBus) {
        MENU_TYPES.register(modEventBus);
    }
}