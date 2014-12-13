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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Collection of small static helper methods using Collections.
 */
public class CollectionUtils {

    /**
     * Make an unmodifiable set with specified members.
     *
     * @param members The set members.
     * @return An unmodifiable set containing the members.
     */
    @SafeVarargs
    public static <T> Set<T> makeUnmodifiableSet(T... members) {
        Set<T> tmp = new HashSet<T>();
        for (T t : members) tmp.add(t);
        return Collections.<T>unmodifiableSet(tmp);
    }

    /**
     * Make an unmodifiable list with specified members.
     *
     * @param members The list members.
     * @return An unmodifiable list containing the members.
     */
    @SafeVarargs
    public static <T> List<T> makeUnmodifiableList(T... members) {
        List<T> tmp = new ArrayList<>();
        for (T t : members) tmp.add(t);
        return Collections.<T>unmodifiableList(tmp);
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
            l = new ArrayList<>();
            l.add(value);
            map.put(key, l);
        } else if (!l.contains(value)) {
            l.add(value);
        }
    }

    /**
     * Increment the count in an integer valued map for a given key.
     *
     * @param map The map to increment within.
     * @param key The key to increment the value for.
     * @return The new count associated with the key.
     */
    public static <K> int incrementMapCount(Map<K, Integer> map, K key) {
        int count = map.containsKey(key) ? map.get(key) : 0;
        map.put(key, count+1);
        return count+1;
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
                    private final List<T> original = new ArrayList<>(l);
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

                    // FIXME: see if we can do it with one array:-)
                    public List<T> next() {
                        List<T> pick = new ArrayList<>(original);
                        List<T> result = new ArrayList<>();
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
     * Are all members of a collection the same (in the sense of ==).
     *
     * @param collection The <code>Collection</code> to examine.
     * @return True if all members are the same.
     */
    public static <T> boolean allSame(final Collection<T> collection) {
        T datum = null;
        boolean first = true;
        for (T t : collection) {
            if (first) datum = t; else if (t != datum) return false;
            first = false;
        }
        return true;
    }
}
