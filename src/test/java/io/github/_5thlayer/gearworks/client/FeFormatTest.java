// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.junit.jupiter.api.Test;

/** FE reads with its digits grouped the way the client's language groups them. */
class FeFormatTest {

    @Test
    void groupsDigitsByTheLocale() {
        assertEquals("31,400", FeFormat.format(31_400, Locale.US));
        assertEquals("31.400", FeFormat.format(31_400, Locale.ITALY));
    }

    @Test
    void leavesSmallNumbersAlone() {
        assertEquals("0", FeFormat.format(0, Locale.US));
        assertEquals("999", FeFormat.format(999, Locale.US));
    }

    @Test
    void readsAMinecraftLanguageCodeAsALocale() {
        assertEquals(Locale.forLanguageTag("it-IT"), FeFormat.locale("it_it"));
        assertEquals(Locale.forLanguageTag("en-US"), FeFormat.locale("en_us"));
    }
}
