/**
 *  Copyright (C) 2002-2016   The FreeCol Team
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.sf.freecol.common.util.CachingFunction;


/**
 * Collection of small static helper methods using Collections.
 */
public class CollectionUtils {

    /** Trivial integer accumulator. */
    public static final BinaryOperator<Integer> integerAccumulator
        = (i1, i2) -> i1 + i2;

    /** Trivial double accumulator. */
    public static final BinaryOperator<Double> doubleAccumulator
        = (d1, d2) -> d1 + d2;

    /** Useful comparators for mapEntriesBy* */
    public static final Comparator<Integer> ascendingIntegerComparator
        = Comparator.comparingInt(i -> i);
    public static final Comparator<Integer> descendingIntegerComparator
        = ascendingIntegerComparator.reversed();
    public static final Comparator<Double> ascendingDoubleComparator
        = Comparator.comparingDouble(d -> d);
    public static final Comparator<Double> descendingDoubleComparator
        = ascendingDoubleComparator.reversed();
    public static final Comparator<List<?>> ascendingListLengthComparator
        = Comparator.comparingInt(l -> l.size());
    public static final Comparator<List<?>> descendingListLengthComparator
        = ascendingListLengthComparator.reversed();

    
    /**
     * Make an unmodifiable set with specified members.
     *
     * @param <T> The type of the set members.
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
     * @param <T> The type of the list members.
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
     * @param <T> The map value collection member type.
     * @param <K> The map key type.
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

    public static <K,V> void accumulateToMap(Map<K,V> map, K key, V value,
                                             BinaryOperator<V> accumulator) {
        if (map.containsKey(key)) {
            map.put(key, accumulator.apply(map.get(key), value));
        } else {
            map.put(key, value);
        }
    }

    public static <K,V> void accumulateMap(Map<K,V> map1, Map<K,V> map2,
                                           BinaryOperator<V> accumulator) {
        for (Entry<K,V> e : map2.entrySet()) {
            accumulateToMap(map1, e.getKey(), e.getValue(), accumulator);
        }
    }

    /**
     * Increment the count in an integer valued map for a given key.
     *
     * @param <K> The map key type.
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
     * @param <T> The list member type.
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
     * @param <T> The collection member type.
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
     * @param <T> The list member type.
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
     * @param <T> The list member type.
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
     * @param <T> The list member type.
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
     * @param <K> The map key type.
     * @param <V> The map value type.
     * @param map The <code>Map</code> to extract entries from.
     * @return A list of entries from the map sorted by key.
     */
    public static <K extends Comparable<? super K>,V> List<Entry<K,V>>
        mapEntriesByKey(Map<K, V> map) {
        return toSortedList(map.entrySet().stream(),
                            Comparator.comparing(Entry::getKey));
    }

    /**
     * Get the entries in a map in a sorted order.
     *
     * @param <K> The map key type.
     * @param <V> The map value type.
     * @param map The <code>Map</code> to extract entries from.
     * @param comparator A <code>Comparator</code> for the values.
     * @return A list of entries from the map sorted by key.
     */
    public static <K,V> List<Entry<K,V>>
        mapEntriesByKey(Map<K, V> map, final Comparator<K> comparator) {
        return toSortedList(map.entrySet().stream(),
                            Comparator.comparing(Entry::getKey, comparator));
    }

    /**
     * Get the entries in a map in a sorted order.
     *
     * @param <K> The map key type.
     * @param <V> The map value type.
     * @param map The <code>Map</code> to extract entries from.
     * @return A list of entries from the map sorted by key.
     */
    public static <K,V extends Comparable<? super V>> List<Entry<K,V>>
        mapEntriesByValue(Map<K, V> map) {
        return toSortedList(map.entrySet().stream(),
                            Comparator.comparing(Entry::getValue));
    }

    /**
     * Get the entries in a map in a sorted order.
     *
     * @param <K> The map key type.
     * @param <V> The map value type.
     * @param map The <code>Map</code> to extract entries from.
     * @param comparator A <code>Comparator</code> for the values.
     * @return A list of entries from the map sorted by value.
     */
    public static <K,V> List<Entry<K,V>>
        mapEntriesByValue(Map<K, V> map, final Comparator<V> comparator) {
        return toSortedList(map.entrySet().stream(),
                            Comparator.comparing(Entry::getValue, comparator));
    }

    public static <T> boolean all(T[] array, Predicate<T> predicate) {
        return all(Arrays.stream(array), predicate);
    }

    public static <T> boolean all(Collection<T> c, Predicate<T> predicate) {
        return all(c.stream(), predicate);
    }

    public static <T> boolean all(Stream<T> stream, Predicate<T> predicate) {
        return stream.allMatch(predicate);
    }

    public static <T> boolean any(T[] array, Predicate<T> predicate) {
        return any(Arrays.stream(array), predicate);
    }

    public static <T> boolean any(Collection<T> c, Predicate<T> predicate) {
        return any(c.stream(), predicate);
    }

    public static <T> boolean any(Stream<T> stream, Predicate<T> predicate) {
        return stream.anyMatch(predicate);
    }

    public static <T> boolean none(T[] array, Predicate<T> predicate) {
        return none(Arrays.stream(array), predicate);
    }

    public static <T> boolean none(Collection<T> c, Predicate<T> predicate) {
        return none(c.stream(), predicate);
    }

    public static <T> boolean none(Stream<T> stream, Predicate<T> predicate) {
        return stream.noneMatch(predicate);
    }

    /**
     * Helper to create a caching ToIntFunction.
     *
     * @param <T> The argument type to be converted to int.
     * @param f The integer valued function to cache.
     * @return A caching <code>ToIntFunction</code>.
     */
    public static <T> ToIntFunction<T> cacheInt(Function<T, Integer> f) {
        return t -> new CachingFunction<T, Integer>(f).apply(t);
    }

    /**
     * Helper to create a caching comparator.
     *
     * @param <T> The argument type to be converted to int.
     * @param f The integer valued function to use in comparison.
     * @return A caching <code>Comparator</code>.
     */
    public static <T> Comparator<T> cachingIntComparator(Function<T, Integer> f) {
        return Comparator.comparingInt(cacheInt(f));
    }

    /**
     * Helper to create a caching ToDoubleFunction.
     *
     * @param <T> The argument type to be converted to double.
     * @param f The double valued function to cache.
     * @return A caching <code>ToDoubleFunction</code>.
     */
    public static <T> ToDoubleFunction<T> cacheDouble(Function<T, Double> f) {
        return t -> new CachingFunction<T, Double>(f).apply(t);
    }

    /**
     * Helper to create a caching comparator.
     *
     * @param <T> The argument type to be converted to double.
     * @param f The double valued function to use in comparison.
     * @return A caching <code>Comparator</code>.
     */
    public static <T> Comparator<T> cachingDoubleComparator(Function<T, Double> f) {
        return Comparator.comparingDouble(cacheDouble(f));
    }

    /**
     * Does a collection contain at least one element that matches a predicate?
     *
     * @param <T> The collection member type.
     * @param c The <code>Collection</code> to search.
     * @param predicate A <code>Predicate</code> to test with.
     * @return True if the predicate ever succeeds.
     */
    public static <T> boolean contains(Collection<T> c,
                                       Predicate<T> predicate) {
        return c.stream().filter(predicate).findFirst().isPresent();
    }

    /**
     * Count the number of members of an array that match a predicate.
     *
     * @param <T> The array member type.
     * @param array The array to check.
     * @param predicate A <code>Predicate</code> to test with.
     * @return The number of items that matched.
     */
    public static <T> int count(T[] array, Predicate<T> predicate) {
        return count(Arrays.stream(array), predicate);
    }
        
    /**
     * Count the number of members of a collection that match a predicate.
     *
     * @param <T> The collection member type.
     * @param c The <code>Collection</code> to check.
     * @param predicate A <code>Predicate</code> to test with.
     * @return The number of items that matched.
     */
    public static <T> int count(Collection<T> c, Predicate<T> predicate) {
        return count(c.stream(), predicate);
    }
        
    /**
     * Count the number of members of a stream that match a predicate.
     *
     * @param <T> The stream member type.
     * @param stream The <code>Stream</code> to check.
     * @param predicate A <code>Predicate</code> to test with.
     * @return The number of items that matched.
     */
    public static <T> int count(Stream<T> stream, Predicate<T> predicate) {
        return (int)stream.filter(predicate).count();
    }
        
    /**
     * Simple stream search for the first item that matches a predicate.
     *
     * @param <T> The array member type.
     * @param array The array to search.
     * @param predicate A <code>Predicate</code> to match with.
     * @param fail The result to return on failure.
     * @return The item found, or fail if not found.
     */
    public static <T> T find(T[] array, Predicate<T> predicate, T fail) {
        return find(Arrays.stream(array), predicate, fail);
    }

    /**
     * Simple stream search for the first item that matches a predicate.
     *
     * @param <T> The collection member type.
     * @param c The <code>Collection</code> to search.
     * @param predicate A <code>Predicate</code> to match with.
     * @return The item found, or fail if not found.
     */
    public static <T> T find(Collection<T> c, Predicate<T> predicate) {
        return find(c, predicate, (T)null);
    }

    /**
     * Simple stream search for the first item that matches a predicate.
     *
     * @param <T> The collection member type.
     * @param collection The <code>Collection</code> to search.
     * @param predicate A <code>Predicate</code> to match with.
     * @param fail The value to return if nothing is found.
     * @return The item found, or fail if not found.
     */
    public static <T> T find(Collection<T> collection, Predicate<T> predicate,
                             T fail) {
        return find(collection.stream(), predicate, fail);
    }

    /**
     * Simple stream search for the first item that matches a predicate.
     *
     * @param <T> The stream member type.
     * @param stream A <code>Stream</code> to search.
     * @param predicate A <code>Predicate</code> to match with.
     * @return The item found, or null if not found.
     */
    public static <T> T find(Stream<T> stream, Predicate<T> predicate) {
        return find(stream, predicate, null);
    }

    /**
     * Simple stream search for the first item that matches a predicate.
     *
     * @param <T> The stream member type.
     * @param stream A <code>Stream</code> to search.
     * @param predicate A <code>Predicate</code> to match with.
     * @param fail The value to return if nothing is found.
     * @return The item found, or fail if not found.
     */
    public static <T> T find(Stream<T> stream, Predicate<T> predicate, T fail) {
        return stream.filter(predicate).findFirst().orElse(fail);
    }

    /**
     * Flatten an array into a stream derived from component collections.
     *
     * @param <T> The array member type.
     * @param <R> The resulting stream member type.
     * @param array The array to flatten.
     * @param mapper A mapping <code>Function</code> to apply.
     * @return A stream of the mapped collection.
     */
    public static <T, R> Stream<R> flatten(T[] array,
        Function<? super T, Collection<? extends R>> mapper) {
        final Predicate<T> alwaysTrue = t -> true;
        return flatten(Arrays.stream(array), alwaysTrue, mapper);
    }

    /**
     * Flatten an array into a stream derived from component collections.
     *
     * @param <T> The array member type.
     * @param <R> The resulting stream member type.
     * @param array The array to flatten.
     * @param predicate A <code>Predicate</code> to filter the collection with.
     * @param mapper A mapping <code>Function</code> to apply.
     * @return A stream of the mapped collection.
     */
    public static <T, R> Stream<R> flatten(T[] array,
        Predicate<T> predicate,
        Function<? super T, Collection<? extends R>> mapper) {
        return flatten(Arrays.stream(array), predicate, mapper);
    }

    /**
     * Flatten a collection into a stream derived from component collections.
     *
     * @param <T> The collection member type.
     * @param <R> The resulting stream member type.
     * @param collection The <code>Collection</code> to flatten.
     * @param mapper A mapping <code>Function</code> to apply.
     * @return A stream of the mapped collection.
     */
    public static <T, R> Stream<R> flatten(Collection<T> collection,
        Function<? super T, Collection<? extends R>> mapper) {
        final Predicate<T> alwaysTrue = t -> true;
        return flatten(collection.stream(), alwaysTrue, mapper);
    }

    /**
     * Flatten a collection into a stream derived from component collections.
     *
     * @param <T> The collection member type.
     * @param <R> The resulting stream member type.
     * @param collection The <code>Collection</code> to flatten.
     * @param predicate A <code>Predicate</code> to filter the collection with.
     * @param mapper A mapping <code>Function</code> to apply.
     * @return A stream of the mapped collection.
     */
    public static <T, R> Stream<R> flatten(Collection<T> collection,
        Predicate<T> predicate,
        Function<? super T, Collection<? extends R>> mapper) {
        return flatten(collection.stream(), predicate, mapper);
    }

    /**
     * Flatten the members of a stream.
     *
     * @param <T> The stream member type.
     * @param <R> The resulting stream member type.
     * @param stream The <code>Stream</code> to flatten.
     * @param mapper A mapping <code>Function</code> to apply.
     * @return A stream of the mapped stream.
     */
    public static <T, R> Stream<R> flatten(Stream<T> stream,
        Function<? super T, Collection<? extends R>> mapper) {
        final Predicate<T> alwaysTrue = t -> true;
        return flatten(stream, alwaysTrue, mapper);
    }

    /**
     * Flatten the members of a stream.
     *
     * @param <T> The stream member type.
     * @param <R> The resulting stream member type.
     * @param stream The <code>Stream</code> to flatten.
     * @param predicate A <code>Predicate</code> to filter the collection with.
     * @param mapper A mapping <code>Function</code> to apply.
     * @return A stream of the mapped stream.
     */
    public static <T, R> Stream<R> flatten(Stream<T> stream,
        Predicate<T> predicate,
        Function<? super T, Collection<? extends R>> mapper) {
        return stream.filter(predicate).map(mapper).flatMap(r -> r.stream());
    }
    
    /**
     * Create a stream from an array and an immediate mapping transform.
     *
     * @param <T> The array member type.
     * @param <R> The resulting stream member type.
     * @param array The array to search.
     * @param mapper A mapping <code>Function</code> to apply.
     * @return The resulting <code>Stream</code>.
     */
    public static <T,R> Stream<R> map(T[] array,
                                      Function<? super T,? extends R> mapper) {
        return Arrays.stream(array).map(mapper);
    }

    /**
     * Create a stream from a collection and an immediate mapping transform.
     *
     * @param <T> The collection member type.
     * @param <R> The resulting stream member type.
     * @param collection The <code>Collection</code> to search.
     * @param mapper A mapping <code>Function</code> to apply.
     * @return The resulting <code>Stream</code>.
     */
    public static <T,R> Stream<R> map(Collection<T> collection,
                                      Function<? super T,? extends R> mapper) {
        return collection.stream().map(mapper);
    }

    /**
     * Find the maximum int value in a collection.
     *
     * @param <T> The collection member type.
     * @param c The <code>Collection</code> to check.
     * @param predicate A <code>Predicate</code> to match with.
     * @param tif A <code>ToIntFunction</code> to map the stream to int with.
     * @return The maximum value found, or zero if the input is empty.
     */
    public static <T> int max(Collection<T> c, Predicate<T> predicate,
                              ToIntFunction<T> tif) {
        return max(c.stream(), predicate, tif);
    }

    /**
     * Find the maximum int value in a stream.
     *
     * @param <T> The stream member type.
     * @param stream The <code>Stream</code> to check.
     * @param predicate A <code>Predicate</code> to match with.
     * @param tif A <code>ToIntFunction</code> to map the stream to int with.
     * @return The maximum value found, or zero if the input is empty.
     */
    public static <T> int max(Stream<T> stream, Predicate<T> predicate,
                              ToIntFunction<T> tif) {
        return stream.filter(predicate).mapToInt(tif).max().orElse(0);
    }

    /**
     * Find the selected member of a collection that maximizes according
     * to a given comparison.
     *
     * @param <T> The collection member type.
     * @param c The <code>Collection</code> to maximize from.
     * @param comparator A <code>Comparator</code> to compare with.
     * @return The maximal value found, or null if none present.
     */
    public static <T> T maximize(Collection<T> c, Comparator<T> comparator) {
        return maximize(c.stream(), p -> true, comparator);
    }

    /**
     * Find the selected member of a collection that maximizes according
     * to a given comparison.
     *
     * @param <T> The collection member type.
     * @param c The <code>Collection</code> to maximize from.
     * @param predicate A <code>Predicate</code> to match with.
     * @param comparator A <code>Comparator</code> to compare with.
     * @return The maximal value found, or null if none present.
     */
    public static <T> T maximize(Collection<T> c, Predicate<T> predicate,
                                 Comparator<T> comparator) {
        return maximize(c.stream(), predicate, comparator);
    }

    /**
     * Find the selected member of a stream that maximizes according
     * to a given comparison.
     *
     * @param <T> The stream member type.
     * @param stream The <code>Stream</code> to maximize from.
     * @param comparator A <code>Comparator</code> to compare with.
     * @return The maximal value found, or null if none present.
     */
    public static <T> T maximize(Stream<T> stream, Comparator<T> comparator) {
        return maximize(stream, p -> true, comparator);
    }

    /**
     * Find the selected member of a stream that maximizes according
     * to a given comparison.
     *
     * @param <T> The collection member type.
     * @param stream The <code>Stream</code> to maximize from.
     * @param predicate A <code>Predicate</code> to match with.
     * @param comparator A <code>Comparator</code> to compare with.
     * @return The maximal value found, or null if none present.
     */
    public static <T> T maximize(Stream<T> stream, Predicate<T> predicate,
                                 Comparator<T> comparator) {
        return stream.filter(predicate).collect(Collectors.maxBy(comparator))
            .orElse(null);
    }

    /**
     * Find the selected member of a collection that minimizes according
     * to a given comparison.
     *
     * @param <T> The collection member type.
     * @param c The <code>Collection</code> to minimize from.
     * @param comparator A <code>Comparator</code> to compare with.
     * @return The minimal value found, or null if none present.
     */
    public static <T> T minimize(Collection<T> c, Comparator<T> comparator) {
        return minimize(c.stream(), t -> true, comparator);
    }

    /**
     * Find the selected member of a collection that minimizes according
     * to a given comparison.
     *
     * @param <T> The collection member type.
     * @param c The <code>Collection</code> to minimize from.
     * @param predicate A <code>Predicate</code> to match with.
     * @param comparator A <code>Comparator</code> to compare with.
     * @return The minimal value found, or null if none present.
     */
    public static <T> T minimize(Collection<T> c, Predicate<T> predicate,
                                 Comparator<T> comparator) {
        return minimize(c.stream(), predicate, comparator);
    }

    /**
     * Find the selected member of a stream that minimizes according
     * to a given comparison.
     *
     * @param <T> The stream member type.
     * @param stream The <code>Stream</code> to minimize from.
     * @param comparator A <code>Comparator</code> to compare with.
     * @return The minimal value found, or null if none present.
     */
    public static <T> T minimize(Stream<T> stream, Comparator<T> comparator) {
        return minimize(stream, t -> true, comparator);
    }

    /**
     * Find the selected member of a stream that minimizes according
     * to a given comparison.
     *
     * @param <T> The stream member type.
     * @param stream The <code>Stream</code> to minimize from.
     * @param predicate A <code>Predicate</code> to match with.
     * @param comparator A <code>Comparator</code> to compare with.
     * @return The minimal value found, or null if none present.
     */
    public static <T> T minimize(Stream<T> stream, Predicate<T> predicate,
                                 Comparator<T> comparator) {
        return stream.filter(predicate).collect(Collectors.minBy(comparator))
            .orElse(null);
    }

    /**
     * Take the sum of the members of a collection.
     *
     * @param <T> The collection member type.
     * @param c The <code>Collection</code> to sum.
     * @param tif A <code>ToIntFunction</code> to convert members to an int.
     * @return The sum of the values found.
     */
    public static <T> int sum(Collection<T> c, ToIntFunction<T> tif) {
        return sum(c.stream(), x -> true, tif);
    }

    /**
     * Take the sum of the members of a collection.
     *
     * @param <T> The collection member type.
     * @param c The <code>Collection</code> to sum.
     * @param tdf A <code>ToDoubleFunction</code> to convert members
     *     to a double.
     * @return The sum of the values found.
     */
    public static <T> double sumDouble(Collection<T> c,
                                       ToDoubleFunction<T> tdf) {
        return sumDouble(c.stream(), x -> true, tdf);
    }

    /**
     * Take the sum of the members of a collection.
     *
     * @param <T> The collection member type.
     * @param c The <code>Collection</code> to sum.
     * @param predicate A <code>Predicate</code> to match with.
     * @param tif A <code>ToIntFunction</code> to map the stream to int with.
     * @return The sum of the values found.
     */
    public static <T> int sum(Collection<T> c, Predicate<T> predicate,
                              ToIntFunction<T> tif) {
        return sum(c.stream(), predicate, tif);
    }

    /**
     * Take the sum of the members of a collection.
     *
     * @param <T> The collection member type.
     * @param c The <code>Collection</code> to sum.
     * @param predicate A <code>Predicate</code> to match with.
     * @param tdf A <code>ToDoubleFunction</code> to map the stream to
     *     double with.
     * @return The sum of the values found.
     */
    public static <T> double sumDouble(Collection<T> c, Predicate<T> predicate,
                                       ToDoubleFunction<T> tdf) {
        return sumDouble(c.stream(), predicate, tdf);
    }

    /**
     * Take the sum of the members of a stream.
     *
     * @param <T> The stream member type.
     * @param stream The <code>Stream</code> to sum.
     * @param tif A <code>ToIntFunction</code> to convert members to an int.
     * @return The sum of the values found.
     */
    public static <T> int sum(Stream<T> stream, ToIntFunction<T> tif) {
        return sum(stream, x -> true, tif);
    }

    /**
     * Take the sum of the members of a stream.
     *
     * @param <T> The stream member type.
     * @param stream The <code>Stream</code> to sum.
     * @param tdf A <code>ToDoubleFunction</code> to convert members
     *     to a double.
     * @return The sum of the values found.
     */
    public static <T> double sumDouble(Stream<T> stream,
                                       ToDoubleFunction<T> tdf) {
        return sumDouble(stream, x -> true, tdf);
    }

    /**
     * Take the sum of the members of a stream.
     *
     * @param <T> The stream member type.
     * @param stream The <code>Stream</code> to sum.
     * @param predicate A <code>Predicate</code> to select members.
     * @param tif A <code>ToIntFunction</code> to convert members to an int.
     * @return The sum of the values found.
     */
    public static <T> int sum(Stream<T> stream, Predicate<T> predicate,
                              ToIntFunction<T> tif) {
        return stream.filter(predicate).mapToInt(tif).sum();
    }

    /**
     * Take the sum of the members of a stream.
     *
     * @param <T> The stream member type.
     * @param stream The <code>Stream</code> to sum.
     * @param predicate A <code>Predicate</code> to select members.
     * @param tdf A <code>ToIntFunction</code> to convert members to a double.
     * @return The sum of the values found.
     */
    public static <T> double sumDouble(Stream<T> stream,
                                       Predicate<T> predicate,
                                       ToDoubleFunction<T> tdf) {
        return stream.filter(predicate).mapToDouble(tdf).sum();
    }

    /**
     * Convenience function to convert an array to a list.
     *
     * @param <T> The array member type.
     * @param array The array to convert.
     * @return A map of the stream contents.
     */
    public static <T> List<T> toList(T[] array) {
        return toList(Arrays.stream(array));
    }

    /**
     * Convenience function to convert a collection to a list.
     *
     * @param <T> The collection member type.
     * @param collection The <code>Collection</code> to convert.
     * @return A map of the stream contents.
     */
    public static <T> Collection<T> toList(Collection<T> collection) {
        return toList(collection.stream());
    }

    /**
     * Convenience function to collect a stream to a list.
     *
     * @param <T> The stream member type.
     * @param stream The <code>Stream</code> to collect.
     * @return A list of the stream contents.
     */
    public static <T> List<T> toList(Stream<T> stream) {
        return stream.collect(Collectors.toList());
    }

    /**
     * Convenience function to convert an array to a sorted list.
     *
     * @param <T> The array member type.
     * @param array The array to convert.
     * @return A list of the stream contents.
     */
    public static <T extends Comparable<? super T>> List<T>
        toSortedList(T[] array) {
        return toSortedList(Arrays.stream(array));
    }

    /**
     * Convenience function to convert a collection to a sorted list.
     *
     * @param <T> The collection member type.
     * @param collection The <code>Collection</code> to convert.
     * @return A list of the stream contents.
     */
    public static <T extends Comparable<? super T>> List<T>
        toSortedList(Collection<T> collection) {
        return toSortedList(collection.stream());
    }

    /**
     * Convenience function to collect a stream to a list.
     *
     * @param <T> The stream member type.
     * @param stream The <code>Stream</code> to collect.
     * @return A list of the stream contents.
     */
    public static <T extends Comparable<? super T>> List<T>
        toSortedList(Stream<T> stream) {
        final Comparator<T> comparator = Comparator.naturalOrder();
        return toSortedList(stream, comparator);
    }

    /**
     * Convenience function to convert an array to a sorted list.
     *
     * @param <T> The array member type.
     * @param array The array to convert.
     * @param comparator A <code>Comparator</code> to sort with.
     * @return A list of the stream contents.
     */
    public static <T> List<T> toSortedList(T[] array,
                                           Comparator<T> comparator) {
        return toSortedList(Arrays.stream(array), comparator);
    }

    /**
     * Convenience function to convert a collection to a map.
     *
     * @param <T> The collection member type.
     * @param collection The <code>Collection</code> to convert.
     * @param comparator A <code>Comparator</code> to sort with.
     * @return A map of the stream contents.
     */
    public static <T> List<T> toSortedList(Collection<T> collection,
                                           Comparator<T> comparator) {
        return toSortedList(collection.stream(), comparator);
    }

    /**
     * Convenience function to collect a stream to a list.
     *
     * @param <T> The stream member type.
     * @param stream The <code>Stream</code> to collect.
     * @param comparator A <code>Comparator</code> to sort with.
     * @return A list of the stream contents.
     */
    public static <T> List<T> toSortedList(Stream<T> stream,
                                           Comparator<T> comparator) {
        return stream.sorted(comparator).collect(Collectors.toList());
    }

    /**
     * Convenience function to convert a collection to a map.
     *
     * @param <T> The collection member type.
     * @param <K> The key mapper function.
     * @param <V> The value mapper function.
     * @param collection The <code>Collection</code> to convert.
     * @param keyMapper A mapping function from datum to key.
     * @param valueMapper A mapping function from datum to value.
     * @return A map of the stream contents.
     */
    public static <T,K,V> Map<K,V> toMap(Collection<T> collection,
        Function<? super T,? extends K> keyMapper,
        Function<? super T,? extends V> valueMapper) {
        return toMap(collection.stream(), keyMapper, valueMapper);
    }

    /**
     * Convenience function to collect a stream to a map.
     *
     * @param <T> The stream member type.
     * @param <K> The key mapper function.
     * @param <V> The value mapper function.
     * @param stream The <code>Stream</code> to collect.
     * @param keyMapper A mapping function from datum to key.
     * @param valueMapper A mapping function from datum to value.
     * @return A map of the stream contents.
     */
    public static <T,K,V> Map<K,V> toMap(Stream<T> stream,
        Function<? super T,? extends K> keyMapper,
        Function<? super T,? extends V> valueMapper) {
        return stream.collect(Collectors.toMap(keyMapper, valueMapper));
    }

    /**
     * Convenience function to collect a stream to a set.
     *
     * @param <T> The stream member type.
     * @param stream The <code>Stream</code> to collect.
     * @return A set of the stream contents.
     */
    public static <T> Set<T> toSet(Stream<T> stream) {
        return stream.collect(Collectors.toSet());
    }

    /**
     * Transform the contents of an array.
     *
     * @param <T> The array member type.
     * @param <C> The resulting collection type.
     * @param array The array to transform.
     * @param predicate A <code>Predicate</code> to select the items.
     * @param collector A <code>Collector</code> to aggregate the results.
     * @return The result of collecting the predicate matches.
     */
    public static <T,C> C transform(T[] array, Predicate<T> predicate,
                                    Collector<T,?,C> collector) {
        return fmc(Arrays.stream(array), predicate, i -> i, collector);
    }

    /**
     * Transform the contents of an array.
     *
     * @param <T> The array member type.
     * @param <R> The resulting collection member type.
     * @param <C> The resulting collection type.
     * @param array The array to transform.
     * @param predicate A <code>Predicate</code> to select the items.
     * @param mapper A function to transform the selected items.
     * @param collector A <code>Collector</code> to aggregate the results.
     * @return The result of collecting the mapped predicate matches.
     */
    public static <T,R,C> C transform(T[] array, Predicate<T> predicate,
                                      Function<? super T, ? extends R> mapper,
                                      Collector<R,?,C> collector) {
        return fmc(Arrays.stream(array), predicate, mapper, collector);
    }

    /**
     * Transform the contents of a collection.
     *
     * @param <T> The collection member type.
     * @param <C> The resulting collection type.
     * @param collection The <code>Collection</code> to transform.
     * @param predicate A <code>Predicate</code> to select the items.
     * @param collector A <code>Collector</code> to aggregate the results.
     * @return The result of collecting the predicate matches.
     */
    public static <T,C> C transform(Collection<T> collection,
                                    Predicate<T> predicate,
                                    Collector<T,?,C> collector) {
        return fmc(collection.stream(), predicate, i -> i, collector);
    }

    /**
     * Transform the contents of a collection.
     *
     * @param <T> The collection member type.
     * @param <R> The resulting collection member type.
     * @param <C> The resulting collection type.
     * @param collection The <code>Collection</code> to transform.
     * @param predicate A <code>Predicate</code> to select the items.
     * @param mapper A function to transform the selected items.
     * @param collector A <code>Collector</code> to aggregate the results.
     * @return The result of collecting the mapped predicate matches.
     */
    public static <T,R,C> C transform(Collection<T> collection,
                                      Predicate<T> predicate,
                                      Function<? super T, ? extends R> mapper,
                                      Collector<R,?,C> collector) {
        return fmc(collection.stream(), predicate, mapper, collector);
    }

    /**
     * Transform the contents of a stream.
     *
     * @param <T> The stream type.
     * @param <C> The resulting collection type.
     * @param stream The <code>Stream</code> to transform.
     * @param predicate A <code>Predicate</code> to select the items.
     * @param collector A <code>Collector</code> to aggregate the results.
     * @return The result of collecting the predicate matches.
     */
    public static <T,C> C transform(Stream<T> stream, Predicate<T> predicate,
                                    Collector<T,?,C> collector) {
        return fmc(stream, predicate, i -> i, collector);
    }

    /**
     * Transform the contents of a stream.
     *
     * @param <T> The stream member type.
     * @param <R> The resulting collection member type.
     * @param <C> The resulting collection type.
     * @param stream The <code>Stream</code> to transform.
     * @param predicate A <code>Predicate</code> to select the items.
     * @param mapper A function to transform the selected items.
     * @param collector A <code>Collector</code> to aggregate the results.
     * @return The result of collecting the mapped predicate matches.
     */
    public static <T,R,C> C transform(Stream<T> stream, Predicate<T> predicate,
                                      Function<? super T, ? extends R> mapper,
                                      Collector<R,?,C> collector) {
        return fmc(stream, predicate, mapper, collector);
    }

    /**
     * Underlying implementation for the transform functions.
     *
     * @param <T> The stream member type.
     * @param <R> The resulting collection member type.
     * @param <C> The resulting collection type.
     * @param stream The <code>Stream</code> to transform.
     * @param predicate A <code>Predicate</code> to select the items.
     * @param mapper A function to transform the selected items.
     * @param collector A <code>Collector</code> to aggregate the results.
     * @return The result of collecting the mapped predicate matches.
     */
    private static <T,R,C> C fmc(Stream<T> stream, Predicate<T> predicate,
                                 Function<? super T, ? extends R> mapper,
                                 Collector<R,?,C> collector) {
        return stream.filter(predicate).map(mapper).collect(collector);
    }

    /**
     * Transform and sort the contents of an array.
     *
     * @param <T> The array member type.
     * @param <R> The resulting collection member type.
     * @param <C> The resulting collection type.
     * @param array The <code>Collection</code> to transform.
     * @param predicate A <code>Predicate</code> to select the items.
     * @param mapper A function to transform the selected items.
     * @param collector A <code>Collector</code> to aggregate the results.
     * @return The sorted result of collecting the mapped predicate matches.
     */
    public static <T,R extends Comparable<? super R>,C> C
        transformAndSort(T[] array, Predicate<T> predicate,
                         Function<? super T, ? extends R> mapper,
                         Collector<R,?,C> collector) {
        final Comparator<? super R> comparator = Comparator.naturalOrder();
        return fmcs(Arrays.stream(array), predicate, mapper, comparator,
                    collector);
    }

    /**
     * Transform and sort the contents of an array.
     *
     * @param <T> The array member type.
     * @param <R> The resulting collection member type.
     * @param <C> The resulting collection type.
     * @param array The <code>Collection</code> to transform.
     * @param predicate A <code>Predicate</code> to select the items.
     * @param mapper A function to transform the selected items.
     * @param comparator A <code>Comparator</code> to sort with.
     * @param collector A <code>Collector</code> to aggregate the results.
     * @return The sorted result of collecting the mapped predicate matches.
     */
    public static <T,R,C> C transformAndSort(T[] array, Predicate<T> predicate,
                                             Function<? super T, ? extends R> mapper,
                                             Comparator<? super R> comparator,
                                             Collector<R,?,C> collector) {
        return fmcs(Arrays.stream(array), predicate, mapper, comparator,
                    collector);
    }

    /**
     * Transform and sort the contents of a collection.
     *
     * @param <T> The collection member type.
     * @param <R> The resulting collection member type.
     * @param <C> The resulting collection type.
     * @param collection The <code>Collection</code> to transform.
     * @param predicate A <code>Predicate</code> to select the items.
     * @param mapper A function to transform the selected items.
     * @param collector A <code>Collector</code> to aggregate the results.
     * @return The sorted result of collecting the mapped predicate matches.
     */
    public static <T,R extends Comparable<? super R>,C> C
        transformAndSort(Collection<T> collection,
                         Predicate<T> predicate,
                         Function<? super T, ? extends R> mapper,
                         Collector<R,?,C> collector) {
        final Comparator<? super R> comparator = Comparator.naturalOrder();
        return fmcs(collection.stream(), predicate, mapper, comparator,
                    collector);
    }

    /**
     * Transform and sort the contents of a collection.
     *
     * @param <T> The collection member type.
     * @param <C> The resulting collection type.
     * @param collection The <code>Collection</code> to transform.
     * @param predicate A <code>Predicate</code> to select the items.
     * @param comparator A <code>Comparator</code> to sort with.
     * @param collector A <code>Collector</code> to aggregate the results.
     * @return The sorted result of collecting the mapped predicate matches.
     */
    public static <T extends Comparable<? super T>,C> C
        transformAndSort(Collection<T> collection,
                         Predicate<T> predicate,
                         Comparator<? super T> comparator,
                         Collector<T,?,C> collector) {
        return fmcs(collection.stream(), predicate, i -> i, comparator,
                    collector);
    }

    /**
     * Transform and sort the contents of a collection.
     *
     * @param <T> The collection member type.
     * @param <R> The resulting collection member type.
     * @param <C> The resulting collection type.
     * @param collection The <code>Collection</code> to transform.
     * @param predicate A <code>Predicate</code> to select the items.
     * @param mapper A function to transform the selected items.
     * @param comparator A <code>Comparator</code> to sort with.
     * @param collector A <code>Collector</code> to aggregate the results.
     * @return The sorted result of collecting the mapped predicate matches.
     */
    public static <T,R,C> C transformAndSort(Collection<T> collection,
                                             Predicate<T> predicate,
                                             Function<? super T, ? extends R> mapper,
                                             Comparator<? super R> comparator,
                                             Collector<R,?,C> collector) {
        return fmcs(collection.stream(), predicate, mapper, comparator,
                    collector);
    }

    /**
     * Underlying implementation for the sorted transform functions.
     *
     * @param <T> The stream member type.
     * @param <R> The resulting collection member type.
     * @param <C> The resulting collection type.
     * @param stream The <code>Stream</code> to transform.
     * @param predicate A <code>Predicate</code> to select the items.
     * @param mapper A function to transform the selected items.
     * @param comparator A <code>Comparator</code> to sort with.
     * @param collector A <code>Collector</code> to aggregate the results.
     * @return The sorted result of collecting the mapped predicate matches.
     */
    private static <T,R,C> C fmcs(Stream<T> stream, Predicate<T> predicate,
                                  Function<? super T, ? extends R> mapper,
                                  Comparator<? super R> comparator,
                                  Collector<R,?,C> collector) {
        return stream.filter(predicate).map(mapper).sorted(comparator)
            .collect(collector);
    }
    
    /**
     * Transform and return distinct items from a collection.
     *
     * @param <T> The collection member type.
     * @param <R> The resulting collection member type.
     * @param <C> The resulting collection type.
     * @param collection The <code>Collection</code> to transform.
     * @param predicate A <code>Predicate</code> to select the items.
     * @param mapper A function to transform the selected items.
     * @param collector A <code>Collector</code> to aggregate the results.
     * @return The result of collecting the mapped predicate matches and
     *     removing duplicates.
     */
    public static <T,R extends Comparable<? super R>,C> C
        transformDistinct(Collection<T> collection, Predicate<T> predicate,
                          Function<? super T, ? extends R> mapper,
                          Collector<R,?,C> collector) {
        final Comparator<? super R> comparator = Comparator.naturalOrder();
        return fmcd(collection.stream(), predicate, mapper, collector);
    }

    /**
     * Underlying implementation for the distinct transform functions.
     *
     * @param <T> The stream member type.
     * @param <R> The resulting collection member type.
     * @param <C> The resulting collection type.
     * @param stream The <code>Stream</code> to transform.
     * @param predicate A <code>Predicate</code> to select the items.
     * @param mapper A function to transform the selected items.
     * @param collector A <code>Collector</code> to aggregate the results.
     * @return The result of collecting the mapped predicate matches and
     *     removing duplicates.
     */
    private static <T,R,C> C fmcd(Stream<T> stream, Predicate<T> predicate,
                                  Function<? super T, ? extends R> mapper,
                                  Collector<R,?,C> collector) {
        return stream.filter(predicate).map(mapper).distinct()
            .collect(collector);
    }
    
    /**
     * Convenience function to convert a stream to an iterable.
     *
     * @param <T> The stream member type.
     * @param stream The <code>Stream</code> to convert.
     * @return The suitable <code>Iterable</code>.
     */
    public static <T> Iterable<T> iterable(final Stream<T> stream) {
        return new Iterable<T>() {
            public Iterator<T> iterator() { return stream.iterator(); }
        };
    }
}
