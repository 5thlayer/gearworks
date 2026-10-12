// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gametest;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;

/**
 * A test environment at that time of the test level's day, and the time it was put back after. 26.1's
 * own {@code clock_time} names its clock by a registry holder, which a mod's tests cannot reach when
 * they register, so this one sets the level's default clock instead.
 */
record DayTime(int time) implements TestEnvironmentDefinition<Long> {

    static final MapCodec<DayTime> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("time").forGetter(DayTime::time)
    ).apply(instance, DayTime::new));

    static final int NOON = 6000;
    static final int MIDNIGHT = 18000;

    @Override
    public Long setup(ServerLevel level) {
        var clock = level.dimensionType().defaultClock().orElseThrow();
        var clocks = level.getServer().clockManager();
        long previous = clocks.getTotalTicks(clock);
        clocks.setTotalTicks(clock, time);
        return previous;
    }

    @Override
    public void teardown(ServerLevel level, Long previous) {
        level.getServer().clockManager().setTotalTicks(level.dimensionType().defaultClock().orElseThrow(), previous);
    }

    @Override
    public MapCodec<DayTime> codec() {
        return CODEC;
    }
}
