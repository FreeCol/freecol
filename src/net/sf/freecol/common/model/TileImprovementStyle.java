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

import java.util.EnumSet;
import java.util.Set;
import net.sf.freecol.common.model.Map.Direction;


/**
 * Represents the style of a tile improvement, such as a river or road.
 */
public class TileImprovementStyle {

    private static final char[] template = new char[] {
        '0', '1', '2'
    };

    private String style;

    private String mask;

    private Set<Direction> connections = EnumSet.noneOf(Direction.class);


    public TileImprovementStyle(String input) {

        this.style = input;

        // @compat 0.10.5
        if (style.length() < 4) {
            // must be an old style
            int value = Integer.parseInt(style);
            char[] chars = new char[4];
            for (int index = 0; index < 4; index++) {
                chars[index] = template[value % 3];
                value /= 3;
            }
            style = new String(chars);
        }
        // end @compat

        Direction[] directions = (style.length() < 8)
            ? Direction.longSides : Direction.values();

        char[] chars = new char[directions.length];
        for (int index = 0; index < directions.length; index++) {
            if (style.charAt(index) == '0') {
                chars[index] = '0';
            } else {
                chars[index] = '1';
                connections.add(directions[index]);
            }
        }
        this.mask = new String(chars);
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

    /**
     * Return a string suitable for looking up an appropriate overlay
     * (forest) image.
     *
     * @return a <code>String</code> value
     */
    public String getMask() {
        return mask;
    }

    /**
     * Return <code>true</code> if the tile improvement is connected
     * to a similar improvement on the given tile.
     *
     * @param direction a <code>Direction</code> value
     * @return a <code>boolean</code> value
     */
    public boolean isConnectedTo(Direction direction) {
        return connections.contains(direction);
    }

}