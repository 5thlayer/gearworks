// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gametest;

import java.util.List;
import java.util.function.Consumer;

import com.mojang.serialization.MapCodec;
import io.github._5thlayer.gearworks.Gearworks;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Module's game tests, run by the {@code gameTestServer} Gradle run: the Minecraft-integration
 * seam, with a real player on a real server. Each test stands on the {@code gametest/platform}
 * structure, a stone floor that {@code scripts/build-gametest-structures.py} writes, and sets up
 * what it needs itself, so the setup is in the diff. It lives in the {@code gametest} source set, which
 * only the Module's own runs load, and hooks itself in, so the {@code @Mod} class names no game-test
 * code and nothing gates it (libworks ADR 0011).
 */
@EventBusSubscriber(modid = Gearworks.MOD_ID)
public final class GearworksGameTests {

    private static final Identifier PLATFORM = id("gametest/platform");

    private static final DeferredRegister<MapCodec<? extends GameTestInstance>> TEST_TYPES =
            DeferredRegister.create(Registries.TEST_INSTANCE_TYPE, Gearworks.MOD_ID);

    private static final DeferredRegister<MapCodec<? extends TestEnvironmentDefinition<?>>> ENVIRONMENT_TYPES =
            DeferredRegister.create(Registries.TEST_ENVIRONMENT_DEFINITION_TYPE, Gearworks.MOD_ID);

    static {
        TEST_TYPES.register("code", () -> CodeGameTest.CODEC);
        ENVIRONMENT_TYPES.register("day_time", () -> DayTime.CODEC);
    }

    private GearworksGameTests() {
    }

    @SubscribeEvent
    static void construct(FMLConstructModEvent event) {
        register(event.getContainer().getEventBus());
    }

    public static void register(IEventBus modBus) {
        TEST_TYPES.register(modBus);
        ENVIRONMENT_TYPES.register(modBus);
        // Posted only when game tests are enabled, which a production game never is.
        modBus.addListener(GearworksGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        // Registered rather than borrowed, since the event hands out no lookup for vanilla's.
        var environment = event.registerEnvironment(id("default"), new TestEnvironmentDefinition.AllOf(List.of()));
        var tests = new Registrar(event, environment);
        LoadTests.register(tests);
        SuitTests.register(tests);
        TierTests.register(tests);
        StatTests.register(tests);
        ReachTests.register(tests);
        FlightTests.register(tests);
        BoostTests.register(tests);
        GridTests.register(tests);
        SolarTests.register(tests);
        BenchTests.register(tests);
        CreativeTabTests.register(tests);
        LanceTests.register(tests);
        LungeBoringTests.register(tests);
        NetheriteLanceTests.register(tests);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Gearworks.MOD_ID, path);
    }

    /** What a test class is handed: a name, a tick budget and a body per test. */
    record Registrar(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {

        void test(String name, int maxTicks, Consumer<GameTestHelper> body) {
            test(name, maxTicks, environment, body);
        }

        /** A test in an environment of its own; tests sharing one run in a batch, apart from the rest. */
        void test(String name, int maxTicks, Holder<TestEnvironmentDefinition<?>> environment, Consumer<GameTestHelper> body) {
            var id = id(name);
            CodeGameTest.define(id, body);
            event.registerTest(id, new CodeGameTest(id, new TestData<>(environment, PLATFORM, maxTicks, 0, true, Rotation.NONE)));
        }

        Holder<TestEnvironmentDefinition<?>> environment(String name, TestEnvironmentDefinition<?>... definitions) {
            return event.registerEnvironment(id(name), definitions);
        }
    }
}
