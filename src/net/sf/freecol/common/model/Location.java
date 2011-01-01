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
     * Returns the Tile where this Location is located. Or null if no Tile
     * applies.
     * 
     * @return The Tile where this Location is located. Or null if no Tile
     *         applies.
     */
    public Tile getTile();

    /**
     * Returns the name of this location.
     * 
     * @return The name of this location.
     */
    public StringTemplate getLocationName();

    /**
     * Returns the name of this location for a particular player.
     *
     * @param player The <code>Player</code> to return the name for.
     * @return The name of this location.
     */
    public StringTemplate getLocationNameFor(Player player);

    /**
     * Adds a <code>Locatable</code> to this Location.
     * 
     * @param locatable
     *            The <code>Locatable</code> to add to this Location.
     */
    public void add(Locatable locatable);

    /**
     * Removes a <code>Locatable</code> from this Location.
     * 
     * @param locatable
     *            The <code>Locatable</code> to remove from this Location.
     */
    public void remove(Locatable locatable);

    /**
     * Checks if this <code>Location</code> contains the specified
     * <code>Locatable</code>.
     * 
     * @param locatable
     *            The <code>Locatable</code> to test the presence of.
     * @return
     *            <ul>
     *            <li><i>true</i> if the specified <code>Locatable</code> is
     *            on this <code>Location</code> and
     *            <li><i>false</i> otherwise.
     *            </ul>
     */
    public boolean contains(Locatable locatable);

    /**
     * Checks whether or not the specified locatable may be added to this
     * <code>Location</code>.
     * 
     * @param locatable
     *            The <code>Locatable</code> to add.
     * @return The result.
     */
    public boolean canAdd(Locatable locatable);

    /**
     * Returns the amount of Units at this Location.
     * 
     * @return The amount of Units at this Location.
     */
    public int getUnitCount();

    /**
     * Returns a list containing all the Units present at this Location.
     * 
     * @return a list containing the Units present at this location.
     */
    public List<Unit> getUnitList();

    /**
     * Gets a <code>Iterator</code> of every <code>Unit</code> directly
     * located on this <code>Location</code>.
     * 
     * @return The <code>Iterator</code>.
     */
    public Iterator<Unit> getUnitIterator();

    /**
     * Gets the ID of this <code>Location</code>.
     * 
     * @return The ID.
     * @see FreeColGameObject#getId
     */
    public String getId();

    /**
     * Gets the <code>GoodsContainer</code> this <code>Location</code> use
     * for storing it's goods.
     * 
     * @return The <code>GoodsContainer</code> or <code>null</code> if the
     *         <code>Location</code> cannot store any goods.
     */
    public GoodsContainer getGoodsContainer();

    /**
     * Returns the <code>Settlement</code> this <code>Location</code> is
     * located in.
     *
     * @return The current <code>Settlement</code> or null if none.
     */
    public Settlement getSettlement();

    /**
     * Returns the <code>Colony</code> this <code>Location</code> is
     * located in.
     *
     * @return The current <code>Colony</code> or null if none.
     */
    public Colony getColony();
}
