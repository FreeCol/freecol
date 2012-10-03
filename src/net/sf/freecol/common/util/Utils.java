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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Collection of small static helper methods.
 */
public class Utils {

    private static final Logger logger = Logger.getLogger(Utils.class.getName());

    /** Hex constant digits for get/restoreRandomState. */
    private static final String HEX_DIGITS = "0123456789ABCDEF";


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
     * @return True if the arguments are either both null or equal in the
     *     sense of their equals() method.
     */
    public static boolean equals(Object one, Object two) {
        return (one == null) ? (two == null) : one.equals(two);
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
     * Appends a value to a list member of a map with a given key.
     *
     * @param map The <code>Map</code> to add to.
     * @param key The key with which to look up the list in the map.
     * @param value The value to append.
     */
    public static <T,K> void appendToMapList(Map<K, List<T>> map,
                                             K key, T value) {
        List<T> l = map.get(key);
        if (l == null) {
            l = new ArrayList<T>();
            l.add(value);
            map.put(key, l);
        } else if (!l.contains(value)) {
            l.add(value);
        }
    }

    /**
     * Given a list, return an iterable that yields all permutations
     * of the original list.
     *
     * Obviously combinatorial explosion will occur, so use with
     * caution only on lists that are known to be short.
     *
     * @param l The original list.
     * @return A iterable yielding all the permutations of the original list.
     */
    public static <T> Iterable<List<T>> getPermutations(final List<T> l) {
        if (l == null) return null;
        return new Iterable<List<T>>() {
            public Iterator<List<T>> iterator() {
                return new Iterator<List<T>>() {
                    private final List<T> original = new ArrayList<T>(l);
                    private final int n = l.size();
                    private final int np = factorial(n);
                    private int index = 0;

                    private int factorial(int n) {
                        int total = n;
                        while (--n > 1) total *= n;
                        return total;
                    }

                    public boolean hasNext() {
                        return index < np;
                    }

                    // TODO: see if we can do it with one array:-)
                    public List<T> next() {
                        List<T> pick = new ArrayList<T>(original);
                        List<T> result = new ArrayList<T>();
                        int current = index++;
                        int divisor = np;
                        for (int i = n; i > 0; i--) {
                            divisor /= i;
                            int j = current / divisor;
                            result.add(pick.remove(j));
                            current -= j * divisor;
                        }
                        return result;
                    }

                    public void remove() {
                        throw new RuntimeException("remove() not implemented");
                    }
                };
            }
        };
    }

    /**
     * Gets the user home directory name.
     *
     * @return The name of the user home directory.
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
        if (logger != null) {
            String msg = logMe + " random(" + range + ") = [";
            for (int i = 0; i < n; i++) msg += " " + Integer.toString(ret[i]);
            msg += " ]";
            logger.finest(msg);
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

    /**
     * Get the internal state of a random number generator as a
     * string.  It would have been more convenient to simply return
     * the current seed, but unfortunately it is private.
     *
     * @return A <code>String</code> encapsulating the object state.
     */
    public static synchronized String getRandomState(Random random) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(random);
            oos.flush();
        } catch (IOException e) {
            throw new IllegalStateException("IO exception in memory!?", e);
        }
        byte[] bytes = bos.toByteArray();
        StringBuffer sb = new StringBuffer(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(HEX_DIGITS.charAt((b >> 4) & 0x0F));
            sb.append(HEX_DIGITS.charAt(b & 0x0F));
        }
        return sb.toString();
    }

    /**
     * Restore a previously saved state.
     *
     * @param state The saved state (@see #getRandomState()).
     * @return The restored <code>Random</code>.
     */
    public static synchronized Random restoreRandomState(String state) {
        if (state == null || state.length() == 0) return null;
        byte[] bytes = new byte[state.length() / 2];
        int pos = 0;
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) HEX_DIGITS.indexOf(state.charAt(pos++));
            bytes[i] <<= 4;
            bytes[i] |= (byte) HEX_DIGITS.indexOf(state.charAt(pos++));
        }
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        try {
            ObjectInputStream ois = new ObjectInputStream(bis);
            return (Random) ois.readObject();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to restore random state.", e);
        }
        return null;
    }
}
