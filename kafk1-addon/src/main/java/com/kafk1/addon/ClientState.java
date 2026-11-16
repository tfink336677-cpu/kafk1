package com.kafk1.addon;

/** Клиентское состояние. Здесь нет клиентских классов Minecraft — безопасно для сервера. */
public final class ClientState {
    private ClientState() {}

    public static volatile boolean installed = false;
    public static volatile boolean charging = false;
    public static int chargeTicks = 0;

    public static float shakeIntensity = 0f;
    public static int shakeTicks = 0;
    public static int shakeTotal = 1;

    public static void setCharging(boolean value) {
        if (charging != value) {
            charging = value;
            chargeTicks = 0;
        }
    }

    public static void shake(float intensity, int ticks) {
        if (intensity >= amplitude() || shakeTicks <= 0) {
            shakeIntensity = intensity;
            shakeTicks = ticks;
            shakeTotal = Math.max(1, ticks);
        }
    }

    public static void tick() {
        if (charging) chargeTicks++;
        if (shakeTicks > 0) shakeTicks--;
    }

    /** Текущая амплитуда тряски камеры. */
    public static float amplitude() {
        float a = 0f;
        if (shakeTicks > 0) a = shakeIntensity * (shakeTicks / (float) shakeTotal);
        if (charging) {
            float progress = Math.min(1f, chargeTicks / (float) KafkConfig.CHARGE_TICKS);
            a = Math.max(a, 0.05f + 0.7f * progress * progress);
        }
        return a;
    }
}
