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

import java.util.EnumMap;
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

    private String style;

    private String mask;

    private Map<Direction, Integer> connections =
        new EnumMap<Direction, Integer>(Direction.class);

    private static final Map<String, TileImprovementStyle> cache =
        new HashMap<String, TileImprovementStyle>();


    private TileImprovementStyle(String input) {

        Direction[] directions = (input.length() < 8)
            ? Direction.longSides : Direction.values();

        // @compat 0.10.5
        if (input.length() < 4) {
            // must be an old style
            style = new String();
            int value = Integer.parseInt(input);
            for (int index = 0; index < 4; index++) {
                int magnitude = value % 3;
                style = style.concat(Integer.toString(magnitude, Character.MAX_RADIX));
                connections.put(directions[index], magnitude);
                value /= 3;
            }
            // end @compat
        } else {
            style = input;
            char[] chars = new char[directions.length];
            for (int index = 0; index < directions.length; index++) {
                int magnitude = Integer.parseInt(style.substring(index, index + 1),
                                                 Character.MAX_RADIX);
                connections.put(directions[index], magnitude);
            }
        }
        this.mask = new String();
        for (Direction direction : directions) {
            mask = mask.concat((getConnection(direction) == 0) ? "0" : "1");
        }
    }


    /**
     * Return the instance identified by the given string.
     *
     * @param key a <code>String</code> value
     * @return a <code>TileImprovementStyle</code> value
     */
    public static TileImprovementStyle getInstance(String key) {
        if (key == null || "".equals(key)) {
            return null;
        } else {
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
    }


    /**
     * Return a string suitable for looking up an appropriate tile
     * improvement image.
     *
     * @return a <code>String</code> value
     */
    public String getString() {
        return style;
    }

    public String toString() {
        return style;
    }

    /**
     * Return a string suitable for looking up an appropriate overlay
     * (forest) image.
     *
     * @return a <code>String</code> value
     */
    public String getMask() {
        return mask;
    }

    public Map<Direction, Integer> getConnections() {
        return new EnumMap<Direction, Integer>(connections);
    }

    /**
     * Return the magnitude of the TileImprovement in the given
     * direction.
     *
     * @param direction a <code>Direction</code> value
     * @return an <code>int</code> value
     */
    public int getConnection(Direction direction) {
        if (connections.containsKey(direction)) {
            return connections.get(direction);
        } else {
            return 0;
        }
    }

    /**
     * Return <code>true</code> if the tile improvement is connected
     * to a similar improvement on the given tile.
     *
     * @param direction a <code>Direction</code> value
     * @return a <code>boolean</code> value
     */
    public boolean isConnectedTo(Direction direction) {
        return getConnection(direction) > 0;
    }

}