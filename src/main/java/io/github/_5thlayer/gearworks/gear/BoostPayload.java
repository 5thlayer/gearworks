// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gear;

import io.github._5thlayer.gearworks.Gearworks;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The key that fires the worn suit's Boost, which the server fires only when it would. */
public record BoostPayload() implements CustomPacketPayload {

    public static final BoostPayload INSTANCE = new BoostPayload();

    public static final Type<BoostPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Gearworks.MOD_ID, "boost"));

    public static final StreamCodec<ByteBuf, BoostPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(BoostPayload payload, IPayloadContext context) {
        SuitBoost.fire(context.player());
    }
}
