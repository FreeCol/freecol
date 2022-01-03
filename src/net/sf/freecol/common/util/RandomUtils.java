/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Collection of small static helper routines for logged pseudo-random
 * number generation.
 */
public class RandomUtils {

    public static class RandomIntCache {

        private final Logger logger;
        private final String logMe;
        private final Random random;
        private final int range;
        private final int size;
        private int generation;
        private int[] cache;
        private int index;

        public RandomIntCache(Logger logger, String logMe, Random random,
                              int range, int size) {
            this.logger = logger;
            this.logMe = logMe;
            this.random = random;
            this.range = range;
            this.size = size;
            this.generation = 0;
            refill();
        }

        private void refill() {
            this.cache = this.random.ints(this.size, 0, this.range).toArray();
            this.index = 0;
            RandomUtils.logArray(logger,
                logMe + "/" + String.valueOf(generation), this.cache);
            this.generation++;
        }

        public int nextInt() {
            if (this.index >= this.size) refill();
            return this.cache[this.index++];
        }

        public int nextInt(int tighterRange) {
            return nextInt() % tighterRange;
        }
    }
    
    /**
     * Convenience to aid logging uses of Randoms.
     *
     * @param logger The {@code Logger} to log to.
     * @param logMe A string to log with the result.
     * @param random A pseudo-{@code Random} number source.
     * @param range The exclusive maximum integer to return.
     * @return A pseudo-random integer r, 0 &le; r &lt; range.
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
     * Log an array of ints at Level.FINEST.
     *
     * @param logger The {@code Logger} to log to.
     * @param logMe A string to log with the result.
     * @param arr The array of ints to log.
     */
    public static void logArray(Logger logger, String logMe, int[] arr) {
        if (logger != null && logger.isLoggable(Level.FINEST)) {
            StringBuilder sb = new StringBuilder(64);
            sb.append(logMe).append(" random").append(" = [");
            for (int i = 0; i < arr.length; i++) sb.append(' ').append(arr[i]);
            sb.append(" ]");
            logger.finest(sb.toString());
        }
    }

    /**
     * Convenience to aid logging uses of Randoms.
     *
     * @param logger The {@code Logger} to log to.
     * @param logMe A string to log with the result.
     * @param random A pseudo-{@code Random} number source.
     * @param range The exclusive maximum integer to return.
     * @param n The number of randoms.
     * @return A vector of pseudo-random integers r, 0 &le; r &lt; range.
     */
    public static int[] randomInts(Logger logger, String logMe, Random random,
                                   int range, int n) {
        int[] ret = random.ints(n, 0, range).toArray();
        logArray(logger, logMe, ret);
        return ret;
    }

    /**
     * Convenience to aid logging uses of Randoms.
     *
     * @param logger The {@code Logger} to log to.
     * @param logMe A string to log with the result.
     * @param random A pseudo-{@code Random} number source.
     * @return A pseudo-random double r, 0 &le; r &lt; 1.0.
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
     * @param logger The {@code Logger} to log to.
     * @param logMe A string to log with the result.
     * @param random A pseudo-{@code Random} number source.
     * @return A pseudo-random double r, 0 &le; r &lt; 1.0.
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
     * @param <T> The list member type.
     * @param logger The {@code Logger} to log to.
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
     * Gets a random member of a stream.
     *
     * @param <T> The list member type.
     * @param logger The {@code Logger} to log to.
     * @param logMe A string to log with the result.
     * @param stream The {@code Stream} to pick from.
     * @param random A random number source.
     * @return A random member from the stream.
     */
    public static <T> T getRandomMember(Logger logger, String logMe,
                                        Stream<T> stream, Random random) {
        return (stream == null) ? null
            : getRandomMember(logger, logMe,
                              stream.collect(Collectors.toList()), random);
    }

    /**
     * Shuffle a list.
     *
     * @param logger The {@code Logger} to log to.
     * @param logMe A string to log with the result.
     * @param list The list.
     * @param random A random number source.
     */
    public static void randomShuffle(Logger logger, String logMe,
                                     List<?> list, Random random) {
        if (list.size() <= 1 || random == null) return;
        if (logger != null && logger.isLoggable(Level.FINEST)) {
            logger.finest(logMe + " shuffle.");
        }
        Collections.shuffle(list, random);
    }
}
