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

package net.sf.freecol.common.model;

import java.util.HashMap;
import java.util.Map;

import net.sf.freecol.common.model.Map.Direction;


/**
 * Represents the style of a tile improvement, such as a river or
 * road. Since TileImprovementStyles are immutable and some styles are
 * far more common than others (e.g. rivers with two branches are far
 * more common than rivers with a single branch, or three or four
 * branches), the class caches all styles actually used. This should
 * also speed up reading maps with old base-three styles.
 *
 * <p>Tile improvement styles should be encoded as a string of base-36
 * values, one for each of the directions the improvement might be
 * connected to (counting clockwise from N). In the case of rivers,
 * which can only be connected via the "long sides" (NE, SE, SW, NW),
 * possible values range from "0000" to "zzzz". Improvement styles
 * that use only four directions can add up to three characters of
 * arbitrary style information (such as "%$&", for example).
 * Improvement styles that use eight directions can add an arbitrary
 * amount of style information.
 */
public class TileImprovementStyle {

    /** Cache all TileImprovementStyles. */
    private static final Map<String, TileImprovementStyle> cache
        = new HashMap<String, TileImprovementStyle>();

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

        String s = new String();
        for (int i = 0; i < style.length(); i++) {
            char c = style.charAt(i);
            if (Character.digit(c, Character.MAX_RADIX) < 0) break;
            s = s.concat((c == '0') ? "0" : "1");
        }
        this.mask = s;
    }

    // @compat 0.10.5
    /**
     * Decode the old base-3 encoded style format.
     *
     * @param input An old style string.
     * @param allDirections If true extend the string to contain a value for
     *     all directions, if not, extend the string only for the long sides.
     * @return The style in the new format.
     */
    public static String decodeOldStyle(String input, boolean allDirections) {
        Direction[] directions = (allDirections) ? Direction.values()
            : Direction.longSides;

        String style = new String();
        try {
            int value = Integer.parseInt(input);
            for (int index = 0; index < 4; index++) {
                int magnitude = value % 3;
                style = style.concat(Integer.toString(magnitude,
                        Character.MAX_RADIX));
                value /= 3;
            }
        } catch (NumberFormatException nfe) {
            return null;
        }
        while (style.length() < directions.length) {
            style = style.concat("0");
        }
        return style;
    }
    // @end compatibility code

    /**
     * Gets the style corresponding to the given string.
     *
     * @param key The key to look up.
     * @return The corresponding <code>TileImprovementStyle</code>.
     */
    public static TileImprovementStyle getInstance(String key) {
        if (key == null || "".equals(key)) return null;

        TileImprovementStyle result = cache.get(key);
        if (result == null) {
            result = new TileImprovementStyle(key);
            cache.put(key, result);
            if (result.getString() != key) {
                cache.put(result.getString(), result);
            }
        }
        return result;
    }

    /**
     * Gets a new style derived from a base version but with an added
     * connection in a given direction.
     *
     * @param direction The new direction to connect to.
     * @param base The base style (may be null).
     * @param magnitude The magnitude of the new connection.
     * @param allDirections The style must include all directions.
     * @return A new style.
     */
    public static TileImprovementStyle getConnectedStyle(Direction direction,
        TileImprovementStyle base, int magnitude, boolean allDirections) {
        String style = (base == null) ? decodeOldStyle("0", allDirections)
            : base.getString();
        int index = direction.ordinal();
        if (style.length() < Direction.NUMBER_OF_DIRECTIONS) index /= 2;

        String result = ((index == 0) ? "" : style.substring(0, index-1))
            + Integer.toString(magnitude)
            + ((index+1 == style.length()) ? "" : style.substring(index+1));
        return getInstance(result);
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
     * {@inheritDoc}
     */
    public String toString() {
        return style;
    }
}
