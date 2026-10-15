// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gear;

import io.github._5thlayer.gearworks.Gearworks;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The server's word that a Boost fired, to the client of the player it throws, which moves them and
 * shows its Cooldown.
 *
 * @param strength the speed in blocks a tick the thrust pulls toward
 * @param ticks    how long it thrusts
 * @param cooldown how long until the next can fire
 */
public record BoostedPayload(double strength, int ticks, int cooldown) implements CustomPacketPayload {

    public static final Type<BoostedPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Gearworks.MOD_ID, "boosted"));

    public static final StreamCodec<ByteBuf, BoostedPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, BoostedPayload::strength,
            ByteBufCodecs.VAR_INT, BoostedPayload::ticks,
            ByteBufCodecs.VAR_INT, BoostedPayload::cooldown,
            BoostedPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(BoostedPayload payload, IPayloadContext context) {
        SuitBoost.of(context.player()).fire(payload.strength(), payload.ticks(), payload.cooldown());
    }
}
