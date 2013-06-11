/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Tile;


/**
 * The server version of a region.
 */
public class ServerRegion extends Region {

    /**
     * The size of this Region (number of Tiles).
     */
    private int size = 0;

    /**
     * A Rectangle that contains all points of the Region.
     */
    private Rectangle bounds = new Rectangle();


    /**
     * Constructor for copying in a new region from an imported game.
     */
    public ServerRegion(Game game, Region region) {
        super(game);
        this.name = region.getName();
        this.nameKey = region.getNameKey();
        this.parent = null;
        this.claimable = region.isClaimable();
        this.discoverable = region.isDiscoverable();
        this.discoveredIn = region.getDiscoveredIn();
        this.discoveredBy = region.getDiscoveredBy();
        this.prediscovered = region.isPrediscovered();
        this.scoreValue = region.getScoreValue();
        this.type = region.getType();
    }

    /**
     * Creates a new server region.
     *
     * @param game The <code>Game</code> to create in.
     * @param nameKey The i18n-name of the region.
     * @param type The <code>RegionType</code> to use.
     * @param parent The <code>Region</code> to be the parent of this one.
     */
    public ServerRegion(Game game, String nameKey, RegionType type,
                        Region parent) {
        super(game);
        setNameKey(nameKey);
        setType(type);
        setParent(parent);
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
        size++;
        if (bounds.x == 0 && bounds.width == 0 ||
            bounds.y == 0 && bounds.height == 0) {
            bounds.setBounds(tile.getX(), tile.getY(), 0, 0);
        } else {
            bounds.add(tile.getX(), tile.getY());
        }
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
