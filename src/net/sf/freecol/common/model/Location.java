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

import java.util.Iterator;
import java.util.List;


/**
 * A place where a <code>Locatable</code> can be put.
 *
 * @see Locatable
 */
public interface Location {

    /**
     * Gets the id of this <code>Location</code>.
     *
     * @return The id.
     * @see FreeColGameObject#getId
     */
    public String getId();

    /**
     * Gets the Tile associated with this Location.
     *
     * @return The Tile associated with this Location, or null if none found.
     */
    public Tile getTile();

    /**
     * Gets the name of this location.
     *
     * @return The name of this location.
     */
    public StringTemplate getLocationName();

    /**
     * Gets the name of this location for a particular player.
     *
     * @param player The <code>Player</code> to return the name for.
     * @return The name of this location.
     */
    public StringTemplate getLocationNameFor(Player player);

    /**
     * Adds a <code>Locatable</code> to this Location.
     *
     * @param locatable The <code>Locatable</code> to add to this Location.
     * @return True if the locatable was added.
     */
    public boolean add(Locatable locatable);

    /**
     * Removes a <code>Locatable</code> from this Location.
     *
     * @param locatable The <code>Locatable</code> to remove from this
     *     Location.
     * @return True if the locatable was removed.
     */
    public boolean remove(Locatable locatable);

    /**
     * Checks if this <code>Location</code> contains the specified
     * <code>Locatable</code>.
     *
     * @param locatable The <code>Locatable</code> to test the presence of.
     * @return True if the locatable is present at this location.
     */
    public boolean contains(Locatable locatable);

    /**
     * Checks whether or not the specified locatable may be added to this
     * <code>Location</code>.
     *
     * @param locatable The <code>Locatable</code> to add.
     * @return True if the locatable can be added to this location.
     */
    public boolean canAdd(Locatable locatable);

    /**
     * Gets the number of units at this Location.
     *
     * @return The number of units at this Location.
     */
    public int getUnitCount();

    /**
     * Gets a list of all the units present at this location.
     *
     * @return A list of all the units at this location.
     */
    public List<Unit> getUnitList();

    /**
     * Gets a <code>Iterator</code> of every <code>Unit</code> directly
     * located on this <code>Location</code>.
     *
     * @return A unit <code>Iterator</code>.
     */
    public Iterator<Unit> getUnitIterator();

    /**
     * Gets the <code>GoodsContainer</code> this <code>Location</code> use
     * for storing it's goods.
     *
     * @return The <code>GoodsContainer</code> or <code>null</code> if the
     *     <code>Location</code> cannot store any goods.
     */
    public GoodsContainer getGoodsContainer();

    /**
     * Gets the <code>Settlement</code> this <code>Location</code> is
     * located in.
     *
     * @return The associated <code>Settlement</code>, or null if none.
     */
    public Settlement getSettlement();

    /**
     * Gets the <code>Colony</code> this <code>Location</code> is
     * located in.
     *
     * @return The associated <code>Colony</code>, or null if none.
     */
    public Colony getColony();
}
