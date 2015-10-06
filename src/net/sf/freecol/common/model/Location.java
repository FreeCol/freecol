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

package net.sf.freecol.common.model;

import java.util.Iterator;
import java.util.List;

import net.sf.freecol.common.ObjectWithId;


/**
 * A place where a <code>Locatable</code> can be put.
 *
 * @see Locatable
 */
public interface Location extends ObjectWithId {

    // "Rank" constants for location ordering.
    // Tile ranks are distinct and non-negative.
    // Other locations devolve to {europe,highseas,tile} rank.
    public static final int LOCATION_RANK_NOWHERE = -3;
    public static final int LOCATION_RANK_EUROPE = -2;
    public static final int LOCATION_RANK_HIGHSEAS = -1;
    
    /**
     * Gets the identifier of this <code>Location</code>.
     *
     * @return The object identifier.
     * @see FreeColGameObject#getId
     */
    @Override
    public String getId();

    /**
     * Gets the Tile associated with this Location.
     *
     * @return The Tile associated with this Location, or null if none found.
     */
    public Tile getTile();

    /**
     * Get a label for this location.
     *
     * @return A label for this location.
     */
    public StringTemplate getLocationLabel();

    /**
     * Get a label for this location for a particular player.
     *
     * @param player The <code>Player</code> to return the name for.
     * @return A label for this location.
     */
    public StringTemplate getLocationLabelFor(Player player);

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
     * Get the colony at this location.
     *
     * @return A <code>Colony</code> at this location if any, or null
     *     if none found.
     */
    public Colony getColony();

    /**
     * Gets the native settlement at this location.
     *
     * @return The <code>IndianSettlement</code> at this location if
     *     any, or null if none found.
     */
    public IndianSettlement getIndianSettlement();

    /**
     * Promote this location to a more meaningful one if possible.
     *
     * For example: a settlement is more meaningful than the tile
     * it occupies.
     *
     * @return A more meaningful <code>Location</code>, or this one.
     */
    public Location up();

    /**
     * Get a integer for this location, for the benefit of location
     * comparators.
     *
     * @return A suitable integer.
     */
    public int getRank();
        
    /**
     * Get a short description of this location.
     *
     * @return A short description.
     */
    public String toShortString();


    /**
     * Static frontend to up().
     *
     * @param loc The <code>Location</code> to improve.
     * @return The improved <code>Location</code>.
     */
    public static Location upLoc(Location loc) {
        return (loc == null) ? null : loc.up();
    }

    /**
     * Static front end to getRank.
     *
     * @param loc A <code>Location</code> to check.
     * @return The integer rank of the given location.
     */
    public static int getRank(Location loc) {
        return (loc == null) ? Location.LOCATION_RANK_NOWHERE : loc.getRank();
    }
}
