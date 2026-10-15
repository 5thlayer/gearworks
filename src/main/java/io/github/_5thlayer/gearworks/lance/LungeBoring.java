// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.lance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import io.github._5thlayer.gearworks.gear.GearworksConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Lunge Boring: while Lunge carries a player with a Bore Lance forward, the lance breaks its Bore
 * through the rock ahead of them, laid on the face ahead of their head and three deep, each tick from
 * as far as they have come, up to a block per level of Lunge, about as far as a lunge on the ground
 * carries them. The tunnel runs straight on from where the lunge began, the way the player faced. The player's way, the two blocks they fill, is broken whatever the Bore. The
 * dash stops at the first block in their way the lance can't break, which they then run into, and the
 * lance breaks nothing past it; a block of the Bore off their way that it can't break, it leaves. Each
 * block is broken as the player breaking it and costs {@link GearworksConfig#lungeBoringCost()}
 * durability, so a lance that wears out or breaks partway stops the dash there.
 *
 * <p>The server bores the whole Bore ahead the tick the lunge fires, so the blocks a lunge carries the
 * player into are gone on the client before it moves them there.
 */
public final class LungeBoring {

    /** The longest a lunge carries the player: the grace vanilla gives an impulse. */
    public static final int DASH_TICKS = 10;

    private static final Map<Player, Dash> DASHES = new WeakHashMap<>();

    // The lance pays this dash's price for each block it breaks, not mining's.
    private static boolean breaking;

    private LungeBoring() {
    }

    /**
     * After a jab's enchantments ran: if they moved a player holding a Bore Lance, its Lunge fired, by
     * vanilla's rules, and the dash starts, boring at once.
     */
    public static void jabbed(LivingEntity user, Vec3 before) {
        if (!(user instanceof ServerPlayer player) || !BoreLance.is(player.getMainHandItem())
                || player.getDeltaMovement().subtract(before).horizontalDistanceSqr() == 0) {
            return;
        }
        var lance = player.getMainHandItem();
        int reach = ((BoreLanceItem) lance.getItem()).lunge();
        DASHES.put(player, new Dash(lance, player.blockPosition(), player.getDirection(), reach));
        tick(player);
    }

    /** Each tick of a dashing player's, their dash bores on. */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            tick(player);
        }
    }

    /** A tick of the player's dash, if they are dashing: the Bore ahead of them broken, until it stops or the lunge is spent. */
    public static void tick(ServerPlayer player) {
        var dash = DASHES.get(player);
        if (dash != null && (dash.ticks++ >= DASH_TICKS || !bore(player, dash))) {
            DASHES.remove(player);
        }
    }

    /** Whether the lance is breaking a block for Lunge Boring, and pays its price rather than mining's. */
    static boolean breaking() {
        return breaking;
    }

    /** Breaks the Bore ahead of the player, and whether the dash goes on: false once it meets what it can't break. */
    private static boolean bore(ServerPlayer player, Dash dash) {
        var lance = dash.lance;
        var level = player.level();
        breaking = true;
        try {
            for (var target : ahead(BoreLance.bore(lance), dash.reached(player), dash.direction)) {
                if (!BoreLance.lasts(lance, player)) {
                    return false;
                }
                var pos = target.pos();
                if (BoreLance.breaks(lance, level, pos) && player.gameMode.destroyBlock(pos)) {
                    continue;
                }
                if (target.inTheWay() && !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                    return false;
                }
            }
            return true;
        } finally {
            breaking = false;
        }
    }

    /**
     * What a dash that way breaks ahead of a player standing on those feet: layer by layer, the player's way
     * first, then the rest of the Bore, laid on the face ahead of their head.
     */
    private static List<Target> ahead(Bore bore, BlockPos feet, Direction direction) {
        var head = feet.above();
        var cells = BoreBreaker.blocks(bore, head.relative(direction), direction.getOpposite(), direction);
        int perLayer = cells.size() / Bore.DEPTH;
        var targets = new ArrayList<Target>();
        for (int in = 0; in < Bore.DEPTH; in++) {
            var way = List.of(feet.relative(direction, in + 1), head.relative(direction, in + 1));
            for (var pos : way) {
                targets.add(new Target(pos, true));
            }
            for (var pos : cells.subList(in * perLayer, (in + 1) * perLayer)) {
                if (!way.contains(pos)) {
                    targets.add(new Target(pos, false));
                }
            }
        }
        return targets;
    }

    /** A block a dash breaks, and whether it is in the player's way, where one it can't break stops the dash. */
    private record Target(BlockPos pos, boolean inTheWay) {
    }

    /**
     * A player's dash: the lance it bores with, the feet it began on, the way it goes, how many blocks
     * on it can go, and how many ticks it has run.
     */
    private static final class Dash {

        private final ItemStack lance;
        private final BlockPos start;
        private final Direction direction;
        private final int reach;
        private int ticks;

        private Dash(ItemStack lance, BlockPos start, Direction direction, int reach) {
            this.lance = lance;
            this.start = start;
            this.direction = direction;
            this.reach = reach;
        }

        /** Where the dash has carried the player's feet: as far on its way as they have come, up to its reach. */
        private BlockPos reached(Player player) {
            var moved = player.blockPosition().subtract(start);
            int along = moved.getX() * direction.getStepX() + moved.getZ() * direction.getStepZ();
            return start.relative(direction, Math.clamp(along, 0, reach));
        }
    }
}
