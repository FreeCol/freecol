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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


/**
 * Wrapper for a Function that caches its results.
 */
public class CachingFunction<T, R> implements Function<T, R> {

    /** The function to be wrapped. */
    private final Function<T, R> wrapped;

    /** A cache of the wrapped values. */
    private final Map<T, R> cache = new HashMap<>();


    /**
     * Create a new caching function.
     *
     * @param wrapped The {@code Function} to be wrapped.
     */
    public CachingFunction(Function<T, R> wrapped) {
        this.wrapped = wrapped;
    }


    /**
     * {@inheritDoc}
     */
    @SuppressFBWarnings(value="MUI_CONTAINSKEY_BEFORE_GET",
                        justification="Deliberate to all null values")
    public R apply(T t) {
        R result;
        // Normally we would just use get(), but this is a general routine
        // so we want to distinguish the case where the failure return from
        // get (which is null) is being used as valid data.
        if (this.cache.containsKey(t)) {
            result = this.cache.get(t);
        } else {
            this.cache.put(t, result = this.wrapped.apply(t));
        }
        return result;
    }
}
