// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Every suit and upgrade is drawn in Gearworks' own art: an icon of its own, and for a suit a worn look
 * of its own, each a texture the Library ships rather than one borrowed from vanilla.
 */
class ArtTest {

    private static final Path ASSETS = Path.of("src/main/resources/assets/gearworks");
    private static final List<String> SUITS = List.of("iron_gear", "diamond_gear", "netherite_gear");
    private static final List<String> UPGRADES = List.of(
            "battery_upgrade", "solar_upgrade", "speed_upgrade", "shield_upgrade", "jump_upgrade", "night_vision_upgrade");

    @ParameterizedTest
    @MethodSource("items")
    void eachItemHasAnIconOfItsOwn(String item) throws IOException {
        assertEquals("{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"gearworks:item/" + item + "\"}}",
                json(ASSETS.resolve("models/item/" + item + ".json")));
        var icon = image(ASSETS.resolve("textures/item/" + item + ".png"));
        assertEquals(16, icon.getWidth());
        assertEquals(16, icon.getHeight());
    }

    @ParameterizedTest
    @MethodSource("suits")
    void eachSuitIsWornInALookOfItsOwn(String suit) throws IOException {
        assertEquals("{\"layers\":{\"humanoid\":[{\"texture\":\"gearworks:" + suit + "\"}]}}",
                json(ASSETS.resolve("equipment/" + suit + ".json")));
        var worn = image(ASSETS.resolve("textures/entity/equipment/humanoid/" + suit + ".png"));
        assertEquals(64, worn.getWidth());
        assertEquals(32, worn.getHeight());
    }

    @Test
    void noTwoIconsAreAlike() throws IOException {
        assertAllDistinct(items(), item -> ASSETS.resolve("textures/item/" + item + ".png"));
    }

    @Test
    void noTwoWornLooksAreAlike() throws IOException {
        assertAllDistinct(SUITS, suit -> ASSETS.resolve("textures/entity/equipment/humanoid/" + suit + ".png"));
    }

    private static List<String> suits() {
        return SUITS;
    }

    private static List<String> items() {
        var items = new ArrayList<>(SUITS);
        items.addAll(UPGRADES);
        return items;
    }

    private static void assertAllDistinct(List<String> names, Function<String, Path> path) throws IOException {
        Map<String, String> seen = new HashMap<>();
        for (var name : names) {
            var pixels = Arrays.toString(pixels(image(path.apply(name))));
            var twin = seen.put(pixels, name);
            assertNull(twin, name + " is drawn the same as " + twin);
        }
    }

    private static int[] pixels(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }

    private static BufferedImage image(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " is missing");
        return ImageIO.read(path.toFile());
    }

    /** The file's JSON with its whitespace stripped, which none of these files holds inside a string. */
    private static String json(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " is missing");
        return Files.readString(path).replaceAll("\\s", "");
    }
}
