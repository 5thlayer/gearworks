// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gear;

import io.github._5thlayer.gearworks.Gearworks;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The key that opens the worn suit's Gear Grid. */
public record OpenGridPayload() implements CustomPacketPayload {

    public static final OpenGridPayload INSTANCE = new OpenGridPayload();

    public static final Type<OpenGridPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Gearworks.MOD_ID, "open_grid"));

    public static final StreamCodec<ByteBuf, OpenGridPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(OpenGridPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            GearGridMenu.open(player);
        }
    }
}
