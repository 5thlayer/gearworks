// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.client;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * FE as a language writes it, with its digits grouped. It touches no Minecraft class, so the plain
 * JVM tests reach it; {@link FeText} gives it the client's language.
 */
final class FeFormat {

    private FeFormat() {
    }

    static String format(long fe, Locale locale) {
        return NumberFormat.getIntegerInstance(locale).format(fe);
    }

    /** The locale of a Minecraft language code, such as {@code it_it}. */
    static Locale locale(String language) {
        return Locale.forLanguageTag(language.replace('_', '-'));
    }
}
