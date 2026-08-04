package com.cogworks.createaimd.registry;

import com.cogworks.createaimd.CreateAIMD;
import com.cogworks.createaimd.items.AimdItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateAIMD.MODID);

    public static final DeferredItem<Item> AIMD = ITEMS.register(
            "aimd_nuclear_launcher_are_you_sure_you_want_to_craft_this_broski",
            () -> new AimdItem(new Item.Properties()));

    public static final DeferredItem<Item> DESTROYED_AIMD = ITEMS.register(
            "destroyed_aimd_nuclear_launcher_elements",
            () -> new AimdItem(new Item.Properties()));
}