package com.kafk1.addon;

/** Все числа способности в одном месте. */
public final class KafkConfig {
    private KafkConfig() {}

    /** Время зарядки (приседания): 6 секунд */
    public static final int CHARGE_TICKS = 6 * 20;
    /** Перезарядка: 20 секунд (считается с момента приземления) */
    public static final int COOLDOWN_TICKS = 20 * 20;

    /** Радиус урона при приземлении (блоков) */
    public static final double RADIUS = 6.0;
    /** 20 сердец = 40 HP */
    public static final float DAMAGE = 40.0f;
    /** Бить ли мобов (игроков бьёт всегда) */
    public static final boolean DAMAGE_MOBS = true;

    /** Радиус, в котором игроки чувствуют тряску камеры */
    public static final double SHAKE_RADIUS = 24.0;

    /** Сила прыжка вверх (≈9 блоков) */
    public static final double JUMP_POWER = 1.45;
    /** Толчок вперёд по направлению взгляда */
    public static final double FORWARD_BOOST = 0.45;

    /** Разрушать слабые блоки вокруг места приземления */
    public static final boolean BREAK_BLOCKS = true;
    public static final int BREAK_RADIUS = 3;
    /** Максимальная твёрдость блока, который ломается (трава, листья, стекло, снег...) */
    public static final float BREAK_MAX_HARDNESS = 0.8f;
}
