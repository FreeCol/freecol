/**
 *  Copyright (C) 2002-2014   The FreeCol Team
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

import java.util.List;


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
}
