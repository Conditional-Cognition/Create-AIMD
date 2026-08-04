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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import com.cogworks.createaimd.CreateAIMD;
import com.cogworks.createaimd.registry.ModItems;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@EventBusSubscriber(modid = CreateAIMD.MODID)
public class AimdItem extends Item {

    private static final int FUSE_TICKS = 200; // 10 seconds
    private static final int FULL_CRATER_RADIUS = 128;
    private static final int FULL_SHOCKWAVE_RADIUS = 256;
    private static final int FULL_THERMAL_RADIUS = 384;
    private static final double THROW_SPEED = 1.5; // blocks/tick, same ballpark as a thrown snowball

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

            // spawnPrimed is void, so pull the entity it just created back out of the
            // world instead: freshly-spawned means tickCount == 0, which is enough to
            // pick it out of the handful of entities near the spawn block.
            NuclearBombEntity bomb = findFreshlySpawnedBomb(serverLevel, pos);
            if (bomb != null) {
                Vec3 look = player.getLookAngle();
                bomb.setDeltaMovement(look.scale(THROW_SPEED));
                bomb.hurtMarked = true; // forces an immediate velocity sync packet to clients
            }
            restoreCountdown = FUSE_TICKS + 5;

            killByRadiation(player);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static NuclearBombEntity findFreshlySpawnedBomb(ServerLevel serverLevel, BlockPos pos) {
        AABB searchBox = new AABB(pos).inflate(1.0);
        for (NuclearBombEntity candidate : serverLevel.getEntitiesOfClass(NuclearBombEntity.class, searchBox)) {
            if (candidate.tickCount == 0) {
                return candidate;
            }
        }
        return null;
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

    // Swaps any dropped AIM'D stacks for the destroyed/broken version on death,
    // so players can't just pick their launcher back up off their own corpse.
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        for (ItemEntity itemEntity : event.getDrops()) {
            ItemStack stack = itemEntity.getItem();
            if (stack.is(ModItems.AIMD.get())) {
                itemEntity.setItem(new ItemStack(ModItems.DESTROYED_AIMD.get(), stack.getCount()));
            }
        }
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable(this.getDescriptionId() + ".desc").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}