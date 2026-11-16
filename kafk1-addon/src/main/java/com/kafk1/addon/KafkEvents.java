package com.kafk1.addon;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Kafk1Mod.MODID)
public class KafkEvents {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            KafkAbility.tick(sp);
        }
    }

    /** После прыжка имплантом урон от падения не получаем. */
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && KafkAbility.isAirborne(sp)) {
            event.setDamageMultiplier(0f);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        KafkAbility.clear(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) Network.sendInstalled(sp);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) Network.sendInstalled(sp);
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) Network.sendInstalled(sp);
    }

    /** Имплант сохраняется после смерти. */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        boolean had = event.getOriginal().getPersistentData().getBoolean(KafkImplant.TAG);
        event.getEntity().getPersistentData().putBoolean(KafkImplant.TAG, had);
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("kafk1")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("install").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    KafkImplant.setInstalled(p, true);
                    ctx.getSource().sendSuccess(() -> Component.translatable("message.kafk1.installed"), false);
                    return 1;
                }))
                .then(Commands.literal("uninstall").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    KafkImplant.setInstalled(p, false);
                    ctx.getSource().sendSuccess(() -> Component.translatable("message.kafk1.removed"), false);
                    return 1;
                })));
    }
}
