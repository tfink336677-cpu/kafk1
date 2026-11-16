package com.kafk1.addon;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Серверная логика импланта КАФК1. */
public final class KafkAbility {
    private KafkAbility() {}

    private static final class State {
        boolean charging;
        int charge;
        boolean airborne;
        int airTicks;
        // расширяющееся кольцо после приземления
        int ring;
        Vec3 ringCenter = Vec3.ZERO;
    }

    private static final int RING_TICKS = 12;

    private static final Map<UUID, State> STATES = new HashMap<>();
    private static final Map<UUID, Long> COOLDOWN_END = new HashMap<>();

    public static void clear(UUID id) {
        STATES.remove(id);
    }

    public static boolean isAirborne(ServerPlayer p) {
        State s = STATES.get(p.getUUID());
        return s != null && s.airborne;
    }

    private static long cooldownLeft(ServerPlayer p) {
        long end = COOLDOWN_END.getOrDefault(p.getUUID(), 0L);
        return Math.max(0L, end - p.level().getGameTime());
    }

    // ------------------------------------------------------------------ ввод
    public static void onHold(ServerPlayer p, boolean holding) {
        State s = STATES.computeIfAbsent(p.getUUID(), k -> new State());

        if (!holding) {
            if (s.charging) cancel(p, s);
            return;
        }

        if (!KafkImplant.isInstalled(p) || s.charging || s.airborne) return;

        long left = cooldownLeft(p);
        if (left > 0) {
            p.displayClientMessage(Component.translatable("message.kafk1.cooldown",
                    String.format("%.1f", left / 20.0)), true);
            Network.sendCharging(p, false);
            return;
        }
        if (!p.onGround() || p.isPassenger() || p.isSpectator() || p.getAbilities().flying) {
            Network.sendCharging(p, false);
            return;
        }

        s.charging = true;
        s.charge = 0;
        Network.sendCharging(p, true);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS, 1.0f, 0.6f);
    }

    private static void cancel(ServerPlayer p, State s) {
        s.charging = false;
        s.charge = 0;
        Network.sendCharging(p, false);
    }

    // ------------------------------------------------------------------ тик
    public static void tick(ServerPlayer p) {
        State s = STATES.get(p.getUUID());
        if (s == null) return;
        ServerLevel level = p.serverLevel();

        // расширяющееся кольцо
        if (s.ring > 0) {
            tickRing(level, s);
        }

        if (s.charging) {
            if (!KafkImplant.isInstalled(p) || !p.isAlive() || !p.onGround() || p.isPassenger()) {
                cancel(p, s);
                return;
            }
            s.charge++;
            p.setShiftKeyDown(true);
            chargeEffects(p, level, s);
            if (s.charge >= KafkConfig.CHARGE_TICKS) {
                launch(p, level, s);
            }
            return;
        }

        if (s.airborne) {
            s.airTicks++;
            if (!p.isAlive()) {
                s.airborne = false;
                return;
            }
            level.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.1, p.getZ(), 3, 0.2, 0.05, 0.2, 0.01);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 0.5, p.getZ(), 2, 0.25, 0.3, 0.25, 0.05);

            boolean touched = p.onGround() || p.isInWater() || p.isInLava();
            if (s.airTicks > 5 && touched) {
                land(p, level, s);
            } else if (s.airTicks > 400) {
                s.airborne = false;
            }
        }
    }

    // ------------------------------------------------------------------ зарядка
    private static void chargeEffects(ServerPlayer p, ServerLevel level, State s) {
        float progress = s.charge / (float) KafkConfig.CHARGE_TICKS;
        BlockState ground = groundState(level, p);

        if (s.charge % 3 == 0) {
            double ang = level.random.nextDouble() * Math.PI * 2;
            double r = 1.2 + level.random.nextDouble() * 1.5;
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground),
                    p.getX() + Math.cos(ang) * r, p.getY() + 0.1, p.getZ() + Math.sin(ang) * r,
                    2, 0.1, 0.05, 0.1, 0.02);
        }
        if (s.charge % 5 == 0) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 0.2, p.getZ(),
                    2 + (int) (progress * 8), 0.5, 0.2, 0.5, 0.15);
        }
        if (s.charge % 20 == 0) {
            level.playSound(null, p.blockPosition(), SoundEvents.WARDEN_HEARTBEAT,
                    SoundSource.PLAYERS, 1.2f, 0.7f + progress * 0.8f);
        }
        if (s.charge % 4 == 0) {
            int total = 10;
            int filled = Math.round(progress * total);
            String bar = "\u25AE".repeat(filled) + "\u25AF".repeat(total - filled);
            p.displayClientMessage(Component.translatable("message.kafk1.charging", bar), true);
        }
    }

    // ------------------------------------------------------------------ прыжок
    private static void launch(ServerPlayer p, ServerLevel level, State s) {
        s.charging = false;
        s.charge = 0;
        s.airborne = true;
        s.airTicks = 0;
        Network.sendCharging(p, false);

        Vec3 look = p.getLookAngle();
        Vec3 horiz = new Vec3(look.x, 0, look.z);
        horiz = horiz.lengthSqr() > 1.0E-4 ? horiz.normalize().scale(KafkConfig.FORWARD_BOOST) : Vec3.ZERO;

        p.setShiftKeyDown(false);
        p.setDeltaMovement(horiz.x, KafkConfig.JUMP_POWER, horiz.z);
        p.hurtMarked = true;
        p.fallDistance = 0f;

        // эффекты прыжка
        double x = p.getX(), y = p.getY(), z = p.getZ();
        BlockState ground = groundState(level, p);
        level.sendParticles(ParticleTypes.EXPLOSION, x, y + 0.2, z, 4, 0.8, 0.1, 0.8, 0.0);
        for (int i = 0; i < 48; i++) {
            double a = i / 48.0 * Math.PI * 2;
            double dx = Math.cos(a), dz = Math.sin(a);
            level.sendParticles(ParticleTypes.CLOUD, x + dx, y + 0.1, z + dz, 0, dx, 0.06, dz, 0.5);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground),
                    x + dx * 1.5, y + 0.1, z + dz * 1.5, 3, 0.2, 0.1, 0.2, 0.25);
        }
        level.playSound(null, x, y, z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.7f, 1.3f);
        level.playSound(null, x, y, z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0f, 0.6f);

        shakeAround(level, p, 1.2f, 14, 12.0);
    }

    // ------------------------------------------------------------------ приземление
    private static void land(ServerPlayer p, ServerLevel level, State s) {
        s.airborne = false;
        p.fallDistance = 0f;

        COOLDOWN_END.put(p.getUUID(), level.getGameTime() + KafkConfig.COOLDOWN_TICKS);
        p.getCooldowns().addCooldown(Kafk1Mod.KAFK1_IMPLANT.get(), KafkConfig.COOLDOWN_TICKS);

        Vec3 pos = p.position();
        double x = pos.x, y = pos.y, z = pos.z;
        BlockState ground = groundState(level, p);

        // --- урон 20 сердец всем в радиусе ---
        AABB box = new AABB(pos, pos).inflate(KafkConfig.RADIUS, 3.0, KafkConfig.RADIUS);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, t -> t != p && t.isAlive())) {
            if (!(e instanceof ServerPlayer) && !KafkConfig.DAMAGE_MOBS) continue;
            if (e.position().distanceTo(pos) > KafkConfig.RADIUS) continue;

            Vec3 dir = e.position().subtract(pos).multiply(1, 0, 1);
            dir = dir.lengthSqr() > 1.0E-4 ? dir.normalize() : Vec3.ZERO;

            e.invulnerableTime = 0;
            // sonicBoom игнорирует броню и эффекты: честные 40 HP
            e.hurt(level.damageSources().sonicBoom(p), KafkConfig.DAMAGE);
            e.push(dir.x * 1.3, 0.7, dir.z * 1.3);
            e.hurtMarked = true;
        }

        // --- эффекты приземления ---
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y + 0.5, z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.SONIC_BOOM, x, y + 1.0, z, 1, 0, 0, 0, 0);
        for (int ring = 1; ring <= 3; ring++) {
            double r = ring * 2.0;
            int n = 24 * ring;
            for (int i = 0; i < n; i++) {
                double a = i / (double) n * Math.PI * 2;
                double dx = Math.cos(a), dz = Math.sin(a);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground),
                        x + dx * r, y + 0.15, z + dz * r, 3, 0.15, 0.1, 0.15, 0.35);
                level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x + dx * r, y + 0.2, z + dz * r,
                        0, dx, 0.05, dz, 0.25);
            }
        }
        level.playSound(null, x, y, z, SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, 2.0f, 0.7f);
        level.playSound(null, x, y, z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.5f, 0.6f);
        level.playSound(null, x, y, z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.5f, 0.5f);

        // --- разрушение слабых блоков ---
        if (KafkConfig.BREAK_BLOCKS && level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            breakBlocks(level, p.blockPosition());
        }

        // --- тряска камеры у всех рядом ---
        shakeAround(level, p, 3.0f, 30, KafkConfig.SHAKE_RADIUS);

        // --- расходящаяся волна трещин ---
        s.ring = RING_TICKS;
        s.ringCenter = pos;
    }

    private static void tickRing(ServerLevel level, State s) {
        s.ring--;
        double progress = 1.0 - s.ring / (double) RING_TICKS;
        double r = KafkConfig.RADIUS * progress;
        int n = (int) (12 + r * 8);
        Vec3 c = s.ringCenter;
        for (int i = 0; i < n; i++) {
            double a = i / (double) n * Math.PI * 2 + level.random.nextDouble() * 0.2;
            double px = c.x + Math.cos(a) * r;
            double pz = c.z + Math.sin(a) * r;
            BlockPos bp = BlockPos.containing(px, c.y - 0.5, pz);
            BlockState st = level.getBlockState(bp);
            if (st.isAir()) st = Blocks.DIRT.defaultBlockState();
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, st),
                    px, c.y + 0.1, pz, 2, 0.1, 0.2, 0.1, 0.15);
        }
    }

    private static void breakBlocks(ServerLevel level, BlockPos center) {
        int r = KafkConfig.BREAK_RADIUS;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, 0, -r), center.offset(r, 2, r))) {
            if (pos.distSqr(center) > r * r + 2) continue;
            BlockState st = level.getBlockState(pos);
            if (st.isAir() || level.getBlockEntity(pos) != null) continue;
            float hardness = st.getDestroySpeed(level, pos);
            if (hardness < 0 || hardness > KafkConfig.BREAK_MAX_HARDNESS) continue;
            level.destroyBlock(pos.immutable(), false);
        }
    }

    // ------------------------------------------------------------------ утилиты
    private static void shakeAround(ServerLevel level, ServerPlayer source, float maxIntensity, int ticks, double radius) {
        for (ServerPlayer other : level.players()) {
            double d = other.distanceTo(source);
            if (d > radius) continue;
            float k = (float) (1.0 - d / radius);
            Network.sendShake(other, Math.max(0.3f, maxIntensity * k), ticks);
        }
    }

    private static BlockState groundState(ServerLevel level, ServerPlayer p) {
        BlockState st = level.getBlockState(p.blockPosition().below());
        return st.isAir() ? Blocks.STONE.defaultBlockState() : st;
    }
}
