// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.lance;

import io.github._5thlayer.gearworks.Gearworks;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The key that steps the Bore Lance in the main hand on to its next Bore. */
public record CycleBorePayload() implements CustomPacketPayload {

    public static final CycleBorePayload INSTANCE = new CycleBorePayload();

    public static final Type<CycleBorePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Gearworks.MOD_ID, "cycle_bore"));

    public static final StreamCodec<ByteBuf, CycleBorePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CycleBorePayload payload, IPayloadContext context) {
        cycle(context.player());
    }

    /**
     * Steps the lance in the player's main hand on, and when that changes its Bore says on the action bar
     * which it now breaks; a tier with one Bore stays on it and says nothing.
     */
    public static void cycle(Player player) {
        var lance = player.getMainHandItem();
        if (!BoreLance.is(lance)) {
            return;
        }
        var before = BoreLance.bore(lance);
        var after = BoreLance.cycle(lance);
        if (after != before) {
            player.sendOverlayMessage(Component.translatable("message.gearworks.bore", after.size()));
        }
    }
}
