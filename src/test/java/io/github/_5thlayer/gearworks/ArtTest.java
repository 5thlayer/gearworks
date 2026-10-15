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
 * Every suit, Bore Lance and upgrade is drawn in Gearworks' own art: an icon of its own, for a suit a
 * worn look of its own and for a lance a sprite of its own in the hand, each a texture the Module ships
 * rather than one borrowed from vanilla.
 */
class ArtTest {

    private static final Path ASSETS = Path.of("src/main/resources/assets/gearworks");
    private static final List<String> SUITS = List.of("iron_gear", "diamond_gear", "netherite_gear");
    private static final List<String> LANCES = List.of("iron_bore_lance", "diamond_bore_lance", "netherite_bore_lance");
    private static final List<String> UPGRADES = List.of(
            "battery_upgrade", "solar_upgrade", "speed_upgrade", "shield_upgrade", "jump_upgrade", "night_vision_upgrade",
            "flight_upgrade", "boost_upgrade");

    @ParameterizedTest
    @MethodSource("items")
    void eachItemHasAnIconOfItsOwn(String item) throws IOException {
        assertEquals("{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"gearworks:item/" + item + "\"}}",
                json(ASSETS.resolve("models/item/" + item + ".json")));
        var icon = image(ASSETS.resolve("textures/item/" + item + ".png"));
        assertEquals(16, icon.getWidth());
        assertEquals(0, icon.getHeight() % 16, "an icon's frames are 16x16");
    }

    @ParameterizedTest
    @MethodSource("suits")
    void eachSuitsIconPulses(String suit) throws IOException {
        assertTrue(image(ASSETS.resolve("textures/item/" + suit + ".png")).getHeight() > 16, suit + "'s icon has one frame");
        assertTrue(json(ASSETS.resolve("textures/item/" + suit + ".png.mcmeta")).contains("\"frametime\":"), suit + "'s icon is not animated");
    }

    @ParameterizedTest
    @MethodSource("upgrades")
    void noUpgradesIconIsAnimated(String upgrade) throws IOException {
        assertEquals(16, image(ASSETS.resolve("textures/item/" + upgrade + ".png")).getHeight());
    }

    @ParameterizedTest
    @MethodSource("suits")
    void theGlowLightsEachWornGem(String suit) throws IOException {
        var glow = image(ASSETS.resolve("textures/entity/gear_glow.png"));
        var worn = image(ASSETS.resolve("textures/entity/equipment/humanoid/" + suit + ".png"));
        assertEquals(worn.getWidth(), glow.getWidth());
        assertEquals(worn.getHeight(), glow.getHeight());
        int gem = 0;
        for (int y = 0; y < glow.getHeight(); y++) {
            for (int x = 0; x < glow.getWidth(); x++) {
                // The halo is faint; the gem's own glow is opaque, and lies on the worn gem's orange.
                if (glow.getRGB(x, y) >>> 24 == 0xFF) {
                    int under = worn.getRGB(x, y);
                    assertTrue((under >> 16 & 0xFF) - (under & 0xFF) > 0x30, suit + " is not orange under the glow at " + x + ", " + y);
                    gem++;
                }
            }
        }
        assertTrue(gem > 0, "the glow lights nothing");
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

    @ParameterizedTest
    @MethodSource("lances")
    void eachLanceIsHeldAsASpearInASpriteOfItsOwn(String lance) throws IOException {
        assertEquals("{\"parent\":\"minecraft:item/spear_in_hand\",\"textures\":{\"layer0\":\"gearworks:item/" + lance + "_in_hand\"}}",
                json(ASSETS.resolve("models/item/" + lance + "_in_hand.json")));
        assertTrue(json(ASSETS.resolve("items/" + lance + ".json")).contains("\"fallback\":{\"type\":\"minecraft:model\",\"model\":\"gearworks:item/"
                + lance + "_in_hand\"}"), lance + " is not held in its in-hand model");
        var held = image(ASSETS.resolve("textures/item/" + lance + "_in_hand.png"));
        assertEquals(32, held.getWidth());
        assertEquals(32, held.getHeight());
    }

    @Test
    void noTwoHeldLancesAreAlike() throws IOException {
        assertAllDistinct(LANCES, lance -> ASSETS.resolve("textures/item/" + lance + "_in_hand.png"));
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

    private static List<String> lances() {
        return LANCES;
    }

    private static List<String> upgrades() {
        return UPGRADES;
    }

    private static List<String> items() {
        var items = new ArrayList<>(SUITS);
        items.addAll(LANCES);
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
