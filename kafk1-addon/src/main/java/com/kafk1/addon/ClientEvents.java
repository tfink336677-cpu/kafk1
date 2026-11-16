package com.kafk1.addon;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Kafk1Mod.MODID, value = Dist.CLIENT)
public class ClientEvents {
    private static boolean lastHold = false;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientState.tick();

        if (mc.player == null || mc.level == null) {
            lastHold = false;
            ClientState.setCharging(false);
            return;
        }

        boolean hold = mc.screen == null && mc.options.keyJump.isDown();
        if (hold != lastHold) {
            lastHold = hold;
            PacketDistributor.sendToServer(new Network.HoldPayload(hold));

            if (hold && ClientState.installed && mc.player.onGround()) {
                // предсказание, чтобы игрок не успел обычно подпрыгнуть до ответа сервера
                ClientState.setCharging(true);
            } else if (!hold) {
                ClientState.setCharging(false);
            }
        }
    }

    /** Во время зарядки: принудительное приседание, стоим на месте, обычный прыжок отключён. */
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!ClientState.charging) return;
        Input in = event.getInput();
        in.shiftKeyDown = true;
        in.jumping = false;
        in.up = false;
        in.down = false;
        in.left = false;
        in.right = false;
        in.forwardImpulse = 0f;
        in.leftImpulse = 0f;
    }

    /** Тряска камеры. */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        float a = ClientState.amplitude();
        if (a <= 0.001f) return;
        event.setPitch(event.getPitch() + (float) ((Math.random() - 0.5) * 2.0 * a * 3.0));
        event.setYaw(event.getYaw() + (float) ((Math.random() - 0.5) * 2.0 * a * 3.0));
        event.setRoll(event.getRoll() + (float) ((Math.random() - 0.5) * 2.0 * a * 6.0));
    }
}
