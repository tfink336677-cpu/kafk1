package com.kafk1.addon;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.ChatFormatting;

public class KafkImplantItem extends Item {
    public KafkImplantItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (KafkImplant.isInstalled(player)) {
                player.displayClientMessage(Component.translatable("message.kafk1.already"), true);
                return InteractionResultHolder.fail(stack);
            }
            KafkImplant.setInstalled(player, true);
            stack.consume(1, player);
            level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8f, 1.4f);
            player.displayClientMessage(Component.translatable("message.kafk1.installed"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.kafk1.kafk1_implant.tooltip1").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.kafk1.kafk1_implant.tooltip2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.kafk1.kafk1_implant.tooltip3").withStyle(ChatFormatting.RED));
    }
}
