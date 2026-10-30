// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gametest;

import java.util.ArrayDeque;
import java.util.Deque;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;

/**
 * The sounds the server played, as NeoForge's {@link PlayLevelSoundEvent} posts each one before it is
 * sent to the players in range: what a test can hear without a client. Each is kept with where it
 * played, and a test asks for those in its own structure, so tests running side by side don't hear
 * each other. Only the most recent are kept.
 */
final class HeardSounds {

    private record Heard(Level level, SoundEvent sound, Vec3 at) {
    }

    private static final int KEPT = 1024;
    private static final Deque<Heard> RECENT = new ArrayDeque<>();

    private HeardSounds() {
    }

    /** Starts listening: called as the game tests hook in, which only the Module's own runs do. */
    static void listen() {
        NeoForge.EVENT_BUS.addListener(HeardSounds::atPosition);
        NeoForge.EVENT_BUS.addListener(HeardSounds::atEntity);
    }

    private static void atPosition(PlayLevelSoundEvent.AtPosition event) {
        heard(event.getLevel(), event.getSound().value(), event.getPosition());
    }

    private static void atEntity(PlayLevelSoundEvent.AtEntity event) {
        heard(event.getLevel(), event.getSound().value(), event.getEntity().position());
    }

    private static synchronized void heard(Level level, SoundEvent sound, Vec3 at) {
        if (level.isClientSide()) {
            return;
        }
        RECENT.addLast(new Heard(level, sound, at));
        while (RECENT.size() > KEPT) {
            RECENT.removeFirst();
        }
    }

    /** How many times {@code sound} has played in {@code helper}'s structure. */
    static synchronized long count(GameTestHelper helper, SoundEvent sound) {
        var bounds = helper.getBounds();
        return RECENT.stream()
                .filter(heard -> heard.level() == helper.getLevel() && heard.sound() == sound && bounds.contains(heard.at()))
                .count();
    }
}
