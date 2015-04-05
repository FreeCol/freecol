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

package net.sf.freecol.server.generator;

//import java.util.logging.Logger;

import java.util.EnumMap;

import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Tile;
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

    private static final char[] template = {
        '0', '1', '2', '3'
    };

    /**
     * River magnitude (size) for each direction toward the edges of the tile
     */
    private java.util.Map<Direction, Integer> branches
        = new EnumMap<>(Direction.class);

    /**
     * River magnitude (size) at the center of the tile
     */
    private int size = TileImprovement.SMALL_RIVER;

    /**
     * Direction the river is flowing toward, at the current section
     */
    public Direction direction;

    /**
     * Tile of the current river section
     */
    private Tile tile;


    /**
     * Creates a new RiverSection with the given branches. This
     * constructor is used by the MapEditor.
     *
     * @param branches The encoded style
     */
    public RiverSection(java.util.Map<Direction, Integer> branches) {
        this.branches = branches;
    }

    /**
     * Constructor used to automatically generate rivers.
     *
     * @param tile The map tile
     * @param direction The direction the river is flowing toward
     */
    public RiverSection(Tile tile, Direction direction) {
        this.tile = tile;
        this.direction = direction;
        setBranch(direction, TileImprovement.SMALL_RIVER);
    }

    /**
     * Get the section tile.
     *
     * @return The <code>Tile</code>.
     */
    public final Tile getTile() {
        return tile;
    }

    /**
     * Returns the size
     * @return size
     */
    public final int getSize() {
        return size;
    }

    /**
     * Sets the size of a branch
     */
    public final void setBranch(Direction direction, int size) {
        if (size != TileImprovement.SMALL_RIVER) {
            size = TileImprovement.LARGE_RIVER;
        }
        branches.put(direction, size);
    }

    /**
     * Gets the size of a branch
     */
    public final int getBranch(Direction direction) {
        if (branches.containsKey(direction)) {
            return branches.get(direction);
        } else {
            return TileImprovement.NO_RIVER;
        }
    }

    /**
     * Removes a branch
     */
    public final void removeBranch(Direction direction) {
        branches.remove(direction);
    }

    /**
     * Increases the size a branch
     */
    public final void growBranch(Direction direction, int increment) {
        int newSize = Math.min(TileImprovement.LARGE_RIVER,
                               Math.max(TileImprovement.NO_RIVER,
                                        getBranch(direction) + increment));
        setBranch(direction, newSize);
    }

    /**
     * Increases the size of this section by one.
     */
    public void grow() {
        this.size++;
        setBranch(direction, TileImprovement.LARGE_RIVER);
    }


    public String encodeStyle() {
        StringBuilder sb = new StringBuilder();
        for (Direction direction : Direction.longSides) {
            sb.append(Integer.toString(getBranch(direction), Character.MAX_RADIX));
        }
        return sb.toString();
    }
}
