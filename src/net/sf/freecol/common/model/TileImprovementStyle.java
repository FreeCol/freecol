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

package net.sf.freecol.common.model;

import java.util.HashMap;
import java.util.Map;


/**
 * Represents the style of a tile improvement, such as a river or
 * road. Since TileImprovementStyles are immutable and some styles are
 * far more common than others (e.g. rivers with two branches are far
 * more common than rivers with a single branch, or three or four
 * branches), the class caches all styles actually used.
 *
 * As of 0.10.6 we use:
 *   - Four character encoded strings for rivers: a "0" for no connection,
 *     otherwise the string value of the integer magnitude of the river
 *     for each of Direction.longSides.
 *   - Eight character binary encoded strings for roads: a "0" or "1" for
 *     each of Direction.values()
 *   These are distinct so that the overlays can vary.
 */
public class TileImprovementStyle {

    /** Cache all TileImprovementStyles. */
    private static final Map<String, TileImprovementStyle> cache = new HashMap<>();

    /** A key for the tile improvement style. */
    private final String style;

    /** A key for the forest overlay, derived from the above. */
    private final String mask;


    /**
     * Private constructor, only called in getInstance() below.
     *
     * @param style The (decoded) style.
     */
    private TileImprovementStyle(String style) {
        this.style = style;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < style.length(); i++) {
            char c = style.charAt(i);
            if (Character.digit(c, Character.MAX_RADIX) < 0) break;
            sb.append((c == '0') ? "0" : "1");
        }
        this.mask = sb.toString();
    }


    /**
     * Gets the key suitable for looking up an appropriate tile
     * improvement image.
     *
     * @return The tile improvement lookup key.
     */
    public String getString() {
        return style;
    }

    /**
     * Gets the key suitable for looking up an appropriate overlay
     * (forest) image.
     *
     * @return The overlay lookup key.
     */
    public String getMask() {
        return mask;
    }

    /**
     * Gets the style corresponding to the given string.
     *
     * @param key The key to look up.
     * @return The corresponding {@code TileImprovementStyle}.
     */
    public static TileImprovementStyle getInstance(String key) {
        if (key == null || (key.length() != 4 && key.length() != 8))
            return null;

        TileImprovementStyle result = cache.get(key);
        if (result == null) {
            result = new TileImprovementStyle(key);
            cache.put(key, result);
            if (!result.getString().equals(key)) {
                cache.put(result.getString(), result);
            }
        }
        return result;
    }

        
    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return style;
    }
}
