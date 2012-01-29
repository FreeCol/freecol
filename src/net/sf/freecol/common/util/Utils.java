/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import java.util.Random;
import java.util.logging.Logger;


/**
 * Collection of small static helper methods.
 */
public class Utils {

    /**
     * Joins the given strings.
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
     * Will check if both objects are equal but also checks for null.
     * 
     * @param one First object to compare
     * @param two Second object to compare
     * @return <code>(one == null && two != null) || (one != null && one.equals(two))</code>
     */
    public static boolean equals(Object one, Object two) {
        return one == null ? two == null : one.equals(two);
    }

    /**
     * Generalize this method instead of calling it directly elsewhere.
     * 
     * @return			String
     */
    public static String getUserDirectory() {
    	return System.getProperty("user.home");
    }

    /**
     * Convenience to aid logging uses of Randoms.
     *
     * @param logger The <code>Logger</code> to log to.
     * @param logMe A string to log with the result.
     * @param random A pseudo-<code>Random</code> number source.
     * @param n The exclusive maximum integer to return.
     * @return A pseudo-random integer r, 0 <= r < n.
     */
    public static int randomInt(Logger logger, String logMe, Random random,
                                int n) {
        int ret = random.nextInt(n);
        if (logger != null) {
            logger.finest(logMe + " random(" + n + ") = " + ret);
        }
        return ret;
    }

    /**
     * Convenience to aid logging uses of Randoms.
     *
     * @param logger The <code>Logger</code> to log to.
     * @param logMe A string to log with the result.
     * @param random A pseudo-<code>Random</code> number source.
     * @return A pseudo-random double r, 0 <= r < 1.0.
     */
    public static double randomDouble(Logger logger, String logMe,
                                      Random random) {
        double ret = random.nextDouble();
        if (logger != null) {
            logger.finest(logMe + " random(1.0) = " + ret);
        }
        return ret;
    }

    /**
     * Gets a random member of a list.
     *
     * @param logger The <code>Logger</code> to log to.
     * @param logMe A string to log with the result.
     * @param list The list.
     * @param random A random number source.
     * @return A random member from the list.
     */
    public static <T> T getRandomMember(Logger logger, String logMe,
                                        List<T> list, Random random) {
        return list.get(randomInt(logger, logMe, random, list.size()));
    }
}
