package com.cogworks.createaimd.registry;

import com.cogworks.createaimd.CreateAIMD;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateAIMD.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> AIMD_MOD_TAB = CREATIVE_MODE_TABS.register(
            "aimd_mod_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.createaimd"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.AIMD.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.AIMD.get());
                        output.accept(ModItems.DESTROYED_AIMD.get());
                    }).build());
}