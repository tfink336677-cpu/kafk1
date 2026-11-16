package com.kafk1.addon;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Состояние импланта у игрока.
 * Если решишь привязать имплант к слотам Chromeware, поменяй только isInstalled / setInstalled.
 */
public final class KafkImplant {
    private KafkImplant() {}

    public static final String TAG = "kafk1_installed";

    public static boolean isInstalled(Player player) {
        return player.getPersistentData().getBoolean(TAG);
    }

    public static void setInstalled(Player player, boolean value) {
        player.getPersistentData().putBoolean(TAG, value);
        if (player instanceof ServerPlayer sp) {
            Network.sendInstalled(sp);
        }
    }
}
