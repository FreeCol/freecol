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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


/**
 * Collection of small static helper methods using Collections.
 */
public class CollectionUtils {

    /** Useful comparators for mapEntriesBy* */
    public static final Comparator<Integer> descendingIntegerComparator
        = new Comparator<Integer>() {
            public int compare(Integer i1, Integer i2) {
                return i2 - i1;
            }
        };
    public static final Comparator<Integer> ascendingIntegerComparator
        = new Comparator<Integer>() {
            public int compare(Integer i1, Integer i2) {
                return i1 - i2;
            }
        };
    public static final Comparator<Double> descendingDoubleComparator
        = new Comparator<Double>() {
            public int compare(Double d1, Double d2) {
                return (d2 > d1) ? 1 : (d2 < d1) ? -1 : 0;
            }
        };
    public static final Comparator<Double> ascendingDoubleComparator
        = new Comparator<Double>() {
            public int compare(Double d1, Double d2) {
                return (d1 > d2) ? 1 : (d1 < d2) ? -1 : 0;
            }
        };

    /** Comparator to order lists by decreasing length. */
    public static final Comparator<List<?>> listLengthComparator
        = new Comparator<List<?>>() {
            @Override
            public int compare(List<?> l1, List<?> l2) {
                return l2.size() - l1.size();
            }
        };

    /**
     * Make an unmodifiable set with specified members.
     *
     * @param members The set members.
     * @return An unmodifiable set containing the members.
     */
    @SafeVarargs
    public static <T> Set<T> makeUnmodifiableSet(T... members) {
        Set<T> tmp = new HashSet<>();
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

    public abstract static class Accumulator<T> {
        public abstract T accumulate(T t1, T t2);
    };

    /** Trivial integer accumulator. */
    public static Accumulator<Integer> integerAccumulator
        = new Accumulator<Integer>() {
            public Integer accumulate(Integer i1, Integer i2) {
                return i1 + i2;
            }
        };
    /** Trivial double accumulator. */
    public static Accumulator<Double> doubleAccumulator
        = new Accumulator<Double>() {
            public Double accumulate(Double d1, Double d2) {
                return d1 + d2;
            }
        };

    public static <K,V> void accumulateToMap(Map<K,V> map, K key, V value,
                                             Accumulator<V> accumulator) {
        if (map.containsKey(key)) {
            map.put(key, accumulator.accumulate(map.get(key), value));
        } else {
            map.put(key, value);
        }
    }

    public static <K,V> void accumulateMap(Map<K,V> map1, Map<K,V> map2,
                                           Accumulator<V> accumulator) {
        for (Entry<K,V> e : map2.entrySet()) {
            accumulateToMap(map1, e.getKey(), e.getValue(), accumulator);
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
            @Override
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

                    @Override
                    public boolean hasNext() {
                        return index < np;
                    }

                    // FIXME: see if we can do it with one array:-)
                    @Override
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

                    @Override
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

    /**
     * Rotate a list by N places.
     *
     * @param list The <code>List</code> to rotate.
     * @param n The number of places to rotate by (positive or negative).
     */
    public static <T> void rotate(final List<T> list, int n) {
        final int len = list.size();
        if (len <= 0 || n == 0) return;
        n %= len;
        if (n > 0) {
            for (; n > 0; n--) {
                T t = list.remove(0);
                list.add(t);
            }
        } else {
            for (; n < 0; n++) {
                T t = list.remove(n-1);
                list.add(0, t);
            }
        }
    }

    /**
     * Reverse a list.
     *
     * @param list The <code>List</code> to reverse.
     */
    public static <T> void reverse(final List<T> list) {
        final int len = list.size();
        if (len <= 0) return;
        for (int i = 0, j = len-1; i < j; i++, j--) {
            T t = list.get(i);
            list.set(i, list.get(j));
            list.set(j, t);
        }
    }

    /**
     * Check if two lists contents are equal but also checks for null.
     *
     * @param one First list to compare
     * @param two Second list to compare
     * @return True if the list contents are all either both null or
     *     equal in the sense of their equals() method.
     */
    public static <T> boolean listEquals(List<T> one, List<T> two) {
        if (one == null) return two == null;
        if (two == null) return false;
        
        Iterator<T> oneI = one.iterator();
        Iterator<T> twoI = two.iterator();
        for (;;) {
            if (oneI.hasNext()) {
                if (twoI.hasNext()) {
                    if (!Utils.equals(oneI.next(), twoI.next())) break;
                } else {
                    break;
                }
            } else {
                return !twoI.hasNext();
            }
        }
        return false;
    }                

    /**
     * Get the entries in a map in a sorted order.
     *
     * @param map The <code>Map</code> to extract entries from.
     * @return A list of entries from the map sorted by key.
     */
    public static <K extends Comparable<? super K>,V> List<Entry<K,V>>
        mapEntriesByKey(Map<K, V> map) {
        List<Entry<K,V>> result = new ArrayList<>();
        result.addAll(map.entrySet());
        Collections.sort(result, new Comparator<Entry<K,V>>() {
                public int compare(Entry<K,V> e1, Entry<K,V> e2) {
                    return e1.getKey().compareTo(e2.getKey());
                }
            });
        return result;
    }

    /**
     * Get the entries in a map in a sorted order.
     *
     * @param map The <code>Map</code> to extract entries from.
     * @param comparator A <code>Comparator</code> for the values.
     * @return A list of entries from the map sorted by key.
     */
    public static <K,V> List<Entry<K,V>>
        mapEntriesByKey(Map<K, V> map, final Comparator<K> comparator) {
        List<Entry<K,V>> result = new ArrayList<>();
        result.addAll(map.entrySet());
        Collections.sort(result, new Comparator<Entry<K,V>>() {
                public int compare(Entry<K,V> e1, Entry<K,V> e2) {
                    return comparator.compare(e1.getKey(), e2.getKey());
                }
            });
        return result;
    }

    /**
     * Get the entries in a map in a sorted order.
     *
     * @param map The <code>Map</code> to extract entries from.
     * @return A list of entries from the map sorted by key.
     */
    public static <K,V extends Comparable<? super V>> List<Entry<K,V>>
        mapEntriesByValue(Map<K, V> map) {
        List<Entry<K,V>> result = new ArrayList<>();
        result.addAll(map.entrySet());
        Collections.sort(result, new Comparator<Entry<K,V>>() {
                public int compare(Entry<K,V> e1, Entry<K,V> e2) {
                    return e1.getValue().compareTo(e2.getValue());
                }
            });
        return result;
    }

    /**
     * Get the entries in a map in a sorted order.
     *
     * @param map The <code>Map</code> to extract entries from.
     * @param comparator A <code>Comparator</code> for the values.
     * @return A list of entries from the map sorted by value.
     */
    public static <K,V> List<Entry<K,V>>
        mapEntriesByValue(Map<K, V> map, final Comparator<V> comparator) {
        List<Entry<K,V>> result = new ArrayList<>();
        result.addAll(map.entrySet());
        Collections.sort(result, new Comparator<Entry<K,V>>() {
                public int compare(Entry<K,V> e1, Entry<K,V> e2) {
                    return comparator.compare(e1.getValue(), e2.getValue());
                }
            });
        return result;
    }
}
