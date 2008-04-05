/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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


package net.sf.freecol.server.model;

import java.awt.Rectangle;

import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Region.RegionType;
import net.sf.freecol.common.model.Tile;


public class ServerRegion extends Region {

    public static final ServerRegion PACIFIC =
        new ServerRegion("model.region.pacific", RegionType.OCEAN, true);
    public static final ServerRegion NORTH_PACIFIC =
        new ServerRegion("model.region.northPacific", RegionType.OCEAN, PACIFIC, true);
    public static final ServerRegion SOUTH_PACIFIC =
        new ServerRegion("model.region.southPacific", RegionType.OCEAN, PACIFIC, true);
    public static final ServerRegion ATLANTIC =
        new ServerRegion("model.region.atlantic", RegionType.OCEAN, true);
    public static final ServerRegion NORTH_ATLANTIC =
        new ServerRegion("model.region.northAtlantic", RegionType.OCEAN, ATLANTIC, true);
    public static final ServerRegion SOUTH_ATLANTIC =
        new ServerRegion("model.region.southAtlantic", RegionType.OCEAN, ATLANTIC, true);

    public static final ServerRegion CENTER =
        new ServerRegion("model.region.center", RegionType.LAND);
    public static final ServerRegion NORTH =
        new ServerRegion("model.region.north", RegionType.LAND);
    public static final ServerRegion SOUTH =
        new ServerRegion("model.region.south", RegionType.LAND);
    public static final ServerRegion EAST =
        new ServerRegion("model.region.east", RegionType.LAND);
    public static final ServerRegion WEST =
        new ServerRegion("model.region.west", RegionType.LAND);
    public static final ServerRegion NORTH_EAST =
        new ServerRegion("model.region.northEast", RegionType.LAND);
    public static final ServerRegion NORTH_WEST =
        new ServerRegion("model.region.northWest", RegionType.LAND);
    public static final ServerRegion SOUTH_EAST =
        new ServerRegion("model.region.southEast", RegionType.LAND);
    public static final ServerRegion SOUTH_WEST =
        new ServerRegion("model.region.southWest", RegionType.LAND);

    public static final ServerRegion[] PREDEFINED_REGIONS = new ServerRegion[] {
        PACIFIC, NORTH_PACIFIC, SOUTH_PACIFIC,
        ATLANTIC, NORTH_ATLANTIC, SOUTH_ATLANTIC,
        NORTH_WEST, NORTH, NORTH_EAST,
        WEST, CENTER, EAST,
        SOUTH_WEST, SOUTH, SOUTH_WEST
    };

    /**
     * The size of this Region (number of Tiles).
     */
    private int size;

    /**
     * A Rectangle that contains all points of the Region.
     */
    private Rectangle bounds = new Rectangle();


    public ServerRegion(String id, RegionType type) {
        this(id, type, null, false);
    }

    public ServerRegion(String id, RegionType type, Region parent) {
        this(id, type, parent, false);
    }

    public ServerRegion(String id, RegionType type, boolean prediscovered) {
        this(id, type, null, prediscovered);
    }

    public ServerRegion(String id, RegionType type, Region parent, boolean prediscovered) {
        super(id, type, parent);
        setPrediscovered(prediscovered);
    }

    /**
     * Get the <code>Size</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getSize() {
        return size;
    }

    /**
     * Set the <code>Size</code> value.
     *
     * @param newSize The new Size value.
     */
    public final void setSize(final int newSize) {
        this.size = newSize;
    }

    /**
     * Get the <code>Bounds</code> value.
     *
     * @return a <code>Rectangle</code> value
     */
    public final Rectangle getBounds() {
        return bounds;
    }

    /**
     * Set the <code>Bounds</code> value.
     *
     * @param newBounds The new Bounds value.
     */
    public final void setBounds(final Rectangle newBounds) {
        this.bounds = newBounds;
    }

    /**
     * Add the given Tile to this Region.
     *
     * @param tile a <code>Tile</code> value
     */
    public void addTile(Tile tile) {
        tile.setRegion(this);
        bounds.add(tile.getX(), tile.getY());
        size++;
    }

    /**
     * Return the center of the Region's bounding box.
     *
     * @return a <code>Position</code> value
     */
    public Position getCenter() {
        return new Position(bounds.x + bounds.width/2, bounds.y + bounds.height/2);
    }


}