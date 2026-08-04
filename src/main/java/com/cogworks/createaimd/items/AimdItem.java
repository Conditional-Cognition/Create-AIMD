package com.cogworks.createaimd.items;

import cattodream.createnucleartech.Config;
import cattodream.createnucleartech.explosion.NuclearBombEntity;
import cattodream.createnucleartech.radiation.RadiationEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import com.cogworks.createaimd.CreateAIMD;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@EventBusSubscriber(modid = CreateAIMD.MODID)
public class AimdItem extends Item {

    private static final int FUSE_TICKS = 200; // 10 seconds
    private static final int FULL_CRATER_RADIUS = 128;
    private static final int FULL_SHOCKWAVE_RADIUS = 256;
    private static final int FULL_THERMAL_RADIUS = 384;

    private static int[] savedConfig = null;
    private static int restoreCountdown = -1;

    public AimdItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            overrideToFullPower();

            BlockPos pos = player.blockPosition();
            NuclearBombEntity.spawnPrimed(serverLevel, pos, player.getDirection(), FUSE_TICKS, 0.0, player);
            restoreCountdown = FUSE_TICKS + 5;

            killByRadiation(player);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static void overrideToFullPower() {
        savedConfig = new int[] {
                Config.nuclearBombCraterRadius,
                Config.nuclearBombShockwaveRadius,
                Config.nuclearBombThermalRadius
        };
        Config.nuclearBombCraterRadius = FULL_CRATER_RADIUS;
        Config.nuclearBombShockwaveRadius = FULL_SHOCKWAVE_RADIUS;
        Config.nuclearBombThermalRadius = FULL_THERMAL_RADIUS;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (restoreCountdown > 0) {
            restoreCountdown--;
            if (restoreCountdown == 0 && savedConfig != null) {
                Config.nuclearBombCraterRadius = savedConfig[0];
                Config.nuclearBombShockwaveRadius = savedConfig[1];
                Config.nuclearBombThermalRadius = savedConfig[2];
                savedConfig = null;
            }
        }
    }

    private static void killByRadiation(Player player) {
        player.getPersistentData().putDouble(RadiationEvents.RADIATION_LEVEL_KEY, 999999.0);
        player.hurt(player.damageSources().magic(), 10000.0f);
        if (player.isAlive()) {
            player.setHealth(0.0f);
        }
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable(this.getDescriptionId() + ".desc").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}