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

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Collection of small static helper routines for logged pseudo-random
 * number generation.
 */
public class RandomUtils {

    /**
     * Convenience to aid logging uses of Randoms.
     *
     * @param logger The <code>Logger</code> to log to.
     * @param logMe A string to log with the result.
     * @param random A pseudo-<code>Random</code> number source.
     * @param range The exclusive maximum integer to return.
     * @return A pseudo-random integer r, 0 <= r < range.
     */
    public static int randomInt(Logger logger, String logMe, Random random,
                                int range) {
        int ret = random.nextInt(range);
        if (logger != null) {
            logger.finest(logMe + " random(" + range + ") = " + ret);
        }
        return ret;
    }

    /**
     * Convenience to aid logging uses of Randoms.
     *
     * @param logger The <code>Logger</code> to log to.
     * @param logMe A string to log with the result.
     * @param random A pseudo-<code>Random</code> number source.
     * @param range The exclusive maximum integer to return.
     * @param n The number of randoms.
     * @return A vector of pseudo-random integers r, 0 <= r < range.
     */
    public static int[] randomInts(Logger logger, String logMe, Random random,
                                   int range, int n) {
        int[] ret = new int[n];
        for (int i = 0; i < n; i++) ret[i] = random.nextInt(range);
        if (logger != null && logger.isLoggable(Level.FINEST)) {
            StringBuilder sb = new StringBuilder(64);
            sb.append(logMe).append(" random(").append(range).append(") = [");
            for (int i = 0; i < n; i++) sb.append(" ").append(ret[i]);
            sb.append(" ]");
            logger.finest(sb.toString());
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
    public static float randomFloat(Logger logger, String logMe,
                                    Random random) {
        float ret = random.nextFloat();
        if (logger != null && logger.isLoggable(Level.FINEST)) {
            logger.finest(logMe + " random(1.0f) = " + ret);
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
        if (logger != null && logger.isLoggable(Level.FINEST)) {
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
        switch (list.size()) {
        case 0:
            return null;
        case 1:
            return list.get(0);
        default:
            break;
        }
        return list.get(randomInt(logger, logMe, random, list.size()));
    }

    /**
     * Shuffle a list.
     *
     * @param logger The <code>Logger</code> to log to.
     * @param logMe A string to log with the result.
     * @param list The list.
     * @param random A random number source.
     */
    public static void randomShuffle(Logger logger, String logMe,
                                     List<?> list, Random random) {
        if (logger != null && logger.isLoggable(Level.FINEST)) {
            logger.finest(logMe + " shuffle.");
        }
        Collections.shuffle(list, random);
    }
}
