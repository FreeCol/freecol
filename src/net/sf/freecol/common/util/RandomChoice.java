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

import java.util.Collection;
import java.util.Random;
import java.util.logging.Logger;

import net.sf.freecol.common.util.Utils;


public class RandomChoice<T> {

    private final int probability;
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


    public static <T> T getWeightedRandom(Logger logger, String logMe,
                                          Random random,
                                          Collection<RandomChoice<T>> input) {
        int n;
        if (input == null || input.isEmpty()
            || (n = getTotalProbability(input)) <= 0) {
            return null;
        } else if (input.size() == 1) {
            return input.iterator().next().getObject();
        } else {
            return select(input, Utils.randomInt(logger, logMe, random, n));
        }
    }

    public static <T> int getTotalProbability(Collection<RandomChoice<T>> input) {
        int total = 0;
        for (RandomChoice<T> choice : input) {
            total += choice.getProbability();
        }
        return total;
    }

    public static <T> T select(Collection<RandomChoice<T>> input, int probability) {
        if (input.isEmpty()) {
            return null;
        } else {
            int total = 0;
            for (RandomChoice<T> choice : input) {
                total += choice.getProbability();
                if (probability < total) {
                    return choice.getObject();
                }
            }
            return input.iterator().next().getObject();
        }
    }

}
