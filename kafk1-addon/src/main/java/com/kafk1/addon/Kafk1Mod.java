package com.kafk1.addon;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(Kafk1Mod.MODID)
public class Kafk1Mod {
    public static final String MODID = "kafk1";

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    /** Предмет-имплант «КАФК1» */
    public static final DeferredItem<Item> KAFK1_IMPLANT = ITEMS.register("kafk1_implant",
            () -> new KafkImplantItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public Kafk1Mod(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(this::addCreative);
        modBus.addListener(Network::register);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(KAFK1_IMPLANT);
        }
    }
}
