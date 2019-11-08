/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

import java.util.Collection;
import java.util.Random;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.RandomUtils.*;


/**
 * A class to provide flat and weighted random selection from a collection.
 */
public class RandomChoice<T> {

    private int probability;
    private final T object;


    public RandomChoice(T object, int probability) {
        this.probability = probability;
        this.object = object;
    }

    public int getProbability() {
        return probability;
    }

    public T getObject() {
        return object;
    }


    private static <T> T select(Collection<RandomChoice<T>> input,
                                int probability) {
        if (input.isEmpty()) return null;

        int total = 0;
        for (RandomChoice<T> choice : input) {
            total += choice.getProbability();
            if (probability < total) return choice.getObject();
        }
        return first(input).getObject();
    }

    public static <T> int getTotalProbability(Collection<RandomChoice<T>> input) {
        return sum(input, RandomChoice::getProbability);
    }

    public static <T> T getWeightedRandom(Logger logger, String logMe,
                                          Collection<RandomChoice<T>> input,
                                          Random random) {
        int n;
        return (input == null || input.isEmpty()
            || (n = getTotalProbability(input)) <= 0) ? null
            : (input.size() == 1) ? first(input).getObject()
            : select(input, randomInt(logger, logMe, random, n));
    }

    public static <T> T getWeightedRandom(Logger logger, String logMe,
                                          Stream<RandomChoice<T>> input,
                                          Random random) {
        return (input == null) ? null
            : getWeightedRandom(logger, logMe, toList(input), random);
    }

    public static <T> void normalize(Collection<RandomChoice<T>> input,
                                     int expectedTotal) {
        int n;
        if (input == null || input.isEmpty()
            || (n = getTotalProbability(input)) <= 0) return;
        final double mult = (double)expectedTotal / n;
        for (RandomChoice<T> choice : input) {
            choice.probability = (int)Math.round(mult * choice.probability);
        }
    }
}
