/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

package net.sf.freecol.server.generator;

//import java.util.logging.Logger;

import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.TileImprovement;


/**
 * This class facilitates building, editing the TileImprovement style
 * for rivers Rivers on the Map are composed of many individual
 * TileImprovements displayed on each Tile the river flows through The
 * river TileImprovement on a Tile has a style which represents the
 * inputs/outputs of water to/from neighboring Tiles This class allows
 * manipulation of individual stream(s) to neighboring Tiles (there
 * are many in case of confluence)
 */
public class RiverSection {

//    private static final Logger logger = Logger.getLogger(RiverImprovementBuilder.class.getName());

    /**
     * Base numbers used to encode/decode the river style
     */
    private static int[] base = {1, 3, 9, 27};

    /**
     * River magnitude (size) for each direction toward the edges of the tile
     */
    private int branch[] = {TileImprovement.NO_RIVER,
                            TileImprovement.NO_RIVER,
                            TileImprovement.NO_RIVER,
                            TileImprovement.NO_RIVER};

    /**
     * River magnitude (size) at the center of the tile
     */
    private int size = TileImprovement.SMALL_RIVER;

    /**
     * Direction the river is flowing toward, at the current section
     */
    public Direction direction;

    /**
     * Position of the current river section
     */
    private Map.Position position;

    /**
     * Constructor used by the MapEditor to encode/decode the style
     *
     * @param style The encoded style
     */
    public RiverSection(int style) {
        decodeStyle(style);
    }

    /**
     * Constructor used to automatically generate rivers.
     *
     * @param position The map position
     * @param direction The direction the river is flowing toward
     */
    public RiverSection(Map.Position position, Direction direction) {
        this.position = position;
        this.direction = direction;
        setBranch(direction, TileImprovement.SMALL_RIVER);
    }

    /**
     * Returns the position
     * @return position
     */
    public Map.Position getPosition() {
        return position;
    }

    /**
     * Returns the size
     * @return size
     */
    public int getSize() {
        return size;
    }

    /**
     * Decodes the style
     * @param style
     */
    public void decodeStyle(int style) {
        int tempStyle = style;
        for (int i = base.length - 1; i >= 0; i--) {
            if (tempStyle>0) {
                branch[i] = tempStyle / base[i];    // Get an integer value for a direction
                tempStyle -= branch[i] * base[i];   // Remove the component of this direction
            }
        }
    }

    /**
     * Encodes the style as a four-digit base-three number. The digits
     * correspond to the four directions valid for rivers, namely
     * north east, south east, south west and north west. Each digit
     * is either zero (no river), one (minor river) or two (major
     * river).
     *
     * @return style
     */
    public int encodeStyle() {
        int style = 0;
        for (int i = 0; i < base.length; i++) {
            style += base[i] * branch[i];
        }
        return style;
    }

    /**
     * Sets the size of a branch
     */
    public void setBranch(Direction direction, int size) {
        if (size != TileImprovement.SMALL_RIVER) {
            size = TileImprovement.LARGE_RIVER;
        }
        for (int i=0; i<Direction.longSides.length; i++) {
            if (Direction.longSides[i]==direction) {
                branch[i] = size;
                break;
            }
        }
    }

    /**
     * Gets the size of a branch
     */
    public int getBranch(Direction direction) {
        for (int i=0; i<Direction.longSides.length; i++) {
            if (Direction.longSides[i]==direction) {
                return branch[i];
            }
        }
        return TileImprovement.NO_RIVER;
    }

    /**
     * Removes a branch
     */
    public void removeBranch(Direction direction) {
        setBranch(direction, TileImprovement.NO_RIVER);
    }

    /**
     * Increases the size a branch
     */
    public void growBranch(Direction direction, int increment) {
        for (int i=0; i<Direction.longSides.length; i++) {
            if (Direction.longSides[i] == direction) {
                branch[i] = Math.min(TileImprovement.LARGE_RIVER,
                                     Math.max(TileImprovement.NO_RIVER,
                                              branch[i] + increment));
                break;
            }
        }
    }

    /**
     * Increases the size of this section by one.
     */
    public void grow() {
        this.size++;
        setBranch(direction, TileImprovement.LARGE_RIVER);
    }
}
