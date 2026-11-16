package com.kafk1.addon;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class Network {
    private Network() {}

    // ---------- Клиент -> Сервер: зажат ли пробел ----------
    public record HoldPayload(boolean holding) implements CustomPacketPayload {
        public static final Type<HoldPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Kafk1Mod.MODID, "hold"));
        public static final StreamCodec<ByteBuf, HoldPayload> CODEC =
                ByteBufCodecs.BOOL.map(HoldPayload::new, HoldPayload::holding);

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ---------- Сервер -> Клиент: идёт ли зарядка (принудительное приседание) ----------
    public record ChargePayload(boolean charging) implements CustomPacketPayload {
        public static final Type<ChargePayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Kafk1Mod.MODID, "charge"));
        public static final StreamCodec<ByteBuf, ChargePayload> CODEC =
                ByteBufCodecs.BOOL.map(ChargePayload::new, ChargePayload::charging);

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ---------- Сервер -> Клиент: тряска камеры ----------
    public record ShakePayload(float intensity, int ticks) implements CustomPacketPayload {
        public static final Type<ShakePayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Kafk1Mod.MODID, "shake"));
        public static final StreamCodec<ByteBuf, ShakePayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT, ShakePayload::intensity,
                ByteBufCodecs.VAR_INT, ShakePayload::ticks,
                ShakePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ---------- Сервер -> Клиент: установлен ли имплант ----------
    public record InstalledPayload(boolean installed) implements CustomPacketPayload {
        public static final Type<InstalledPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Kafk1Mod.MODID, "installed"));
        public static final StreamCodec<ByteBuf, InstalledPayload> CODEC =
                ByteBufCodecs.BOOL.map(InstalledPayload::new, InstalledPayload::installed);

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar("1");
        reg.playToServer(HoldPayload.TYPE, HoldPayload.CODEC, Network::handleHold);
        reg.playToClient(ChargePayload.TYPE, ChargePayload.CODEC, Network::handleCharge);
        reg.playToClient(ShakePayload.TYPE, ShakePayload.CODEC, Network::handleShake);
        reg.playToClient(InstalledPayload.TYPE, InstalledPayload.CODEC, Network::handleInstalled);
    }

    // ---------- Обработчики ----------
    private static void handleHold(HoldPayload p, IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer sp) {
            KafkAbility.onHold(sp, p.holding());
        }
    }

    private static void handleCharge(ChargePayload p, IPayloadContext ctx) {
        ClientState.setCharging(p.charging());
    }

    private static void handleShake(ShakePayload p, IPayloadContext ctx) {
        ClientState.shake(p.intensity(), p.ticks());
    }

    private static void handleInstalled(InstalledPayload p, IPayloadContext ctx) {
        ClientState.installed = p.installed();
    }

    // ---------- Отправка ----------
    public static void sendCharging(ServerPlayer player, boolean charging) {
        PacketDistributor.sendToPlayer(player, new ChargePayload(charging));
    }

    public static void sendShake(ServerPlayer player, float intensity, int ticks) {
        PacketDistributor.sendToPlayer(player, new ShakePayload(intensity, ticks));
    }

    public static void sendInstalled(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new InstalledPayload(KafkImplant.isInstalled(player)));
    }
}
