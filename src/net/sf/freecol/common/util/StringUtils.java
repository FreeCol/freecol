/**
 *  Copyright (C) 2002-2015   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.util;

import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * Collection of small static helper methods using Strings.
 */
public class StringUtils {

    /**
     * Joins the given strings.
     *
     * In Java 8, we can use String.join.
     *
     * @param delimiter The delimiter to place between the individual strings.
     * @param strings The strings to be joined.
     * @return Each of the strings in the given array delimited by the given
     *         string.
     */
    public static String join(String delimiter, String... strings) {
        if (strings == null || strings.length == 0) {
            return null;
        } else {
            StringBuilder result = new StringBuilder(strings[0]);
            for (int i = 1; i < strings.length; i++) {
                result.append(delimiter);
                result.append(strings[i]);
            }
            return result.toString();
        }
    }

    /**
     * Joins the given strings.
     *
     * @param delimiter The delimiter to place between the individual strings.
     * @param strings The strings to be joined.
     * @return Each of the strings in the given array delimited by the given
     *         string.
     */
    public static String join(String delimiter, List<String> strings) {
        return join(delimiter, strings.toArray(new String[0]));
    }

    /**
     * Truncate a string to a maximum length.
     *
     * @param str The string to chop.
     * @param maxLength The maximum length.
     * @return A string not exceeding maxLength.
     */
    public static String chop(String str, int maxLength) {
        return (str.length() > maxLength) ? str.substring(0, maxLength) : str;
    }

    /**
     * Gets the last part of a string after a supplied delimiter.
     *
     * @param s The string to operate on.
     * @param delim The delimiter.
     * @return The last part of the string after the last instance of
     *     the delimiter, or the original string if the delimiter is
     *     not present.
     */
    public static String lastPart(String s, String delim) {
        int last = (s == null) ? -1 : s.lastIndexOf(delim);
        return (last > 0) ? s.substring(last+delim.length(), s.length())
            : s;
    }

    /**
     * Get the standard form an enum is used as a key.
     *
     * Convert to lower case, and remove underscores uppercasing the
     * next letter.
     *
     * @param value The enum value.
     * @return A suitable key.
     */
    public static String getEnumKey(Enum<?> value) {
        final String base = value.toString().toLowerCase(Locale.US);
        final int len = base.length();
        StringBuilder sb = new StringBuilder(len);
        int idx, from = 0;
        for (;;) {
            if ((idx = base.indexOf('_', from)) < 0) {
                sb.append(base.substring(from));
                break;
            }
            sb.append(base.substring(from, idx));
            from = idx+1;
            if (from >= len) break;
            char ch = base.charAt(from);
            if (Character.isLetter(ch)) {
                sb.append(Character.toUpperCase(ch));
                from++;
            }
        }
        return sb.toString();
    }

    /**
     * Find a breaking point in a line between two words.  The
     * breaking point is as close to the center as possible.
     *
     * @param string The line for which we should determine a breaking point.
     * @return The best breaking point or negative if none found.
     */
    public static int getBreakingPoint(String string) {
        return getBreakingPoint(string, " ");
    }

    /**
     * Find a breaking point in a line between two words.  The
     * breaking point is as close to the center as possible.
     *
     * @param string The line for which we should determine a breaking point.
     * @param delim Characters to consider as word delimiting.
     * @return The best breaking point or negative if none found.
     */
    public static int getBreakingPoint(String string, String delim) {
        int center = string.length() / 2;
        for (int offset = 0; offset < center; offset++) {
            if (delim.indexOf(string.charAt(center + offset)) >= 0) {
                return center + offset;
            } else if (delim.indexOf(string.charAt(center - offset)) >= 0) {
                return center - offset;
            }
        }
        return -1;
    }

    /**
     * Split some text at word boundaries into a list of strings that
     * take up no more than a given width.
     *
     * @param text The text to split.
     * @param delim Characters to consider as word delimiting.
     * @param fontMetrics <code>FontMetrics</code> used to calculate
     *     text width.
     * @param width The text width maximum.
     * @return A list of split text.
     */
    public static List<String> splitText(String text, String delim,
                                         FontMetrics fontMetrics, int width) {
        List<String> result = new ArrayList<>();
        final int len = text.length();
        int i = 0, start;
        String top = "";
        Character d = null;
        for (;;) {
            for (; i < len; i++) {
                if (delim.indexOf(text.charAt(i)) < 0) break;
            }
            if (i >= len) break;
            start = i;
            for (; i < len; i++) {
                if (delim.indexOf(text.charAt(i)) >= 0) break;
            }
            String s = text.substring(start, i);
            String t = (top.isEmpty()) ? s : top + d + s;
            if (fontMetrics.stringWidth(t) > width) {
                if (top.isEmpty()) {
                    result.add(s);
                } else {
                    result.add(top);
                    top = s;
                }
            } else {
                top = t;
            }
            if (i >= len) {
                if (!top.isEmpty()) result.add(top);
                break;
            }
            d = text.charAt(i);
        }
        return result;
    }
}
