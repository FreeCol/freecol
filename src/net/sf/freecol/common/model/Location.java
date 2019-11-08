/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

import java.util.List;
import java.util.stream.Stream;

import javax.swing.ImageIcon;

import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.ObjectWithId;


/**
 * A place where a {@code Locatable} can be put.
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
     * Gets the identifier of this {@code Location}.
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
     * @param player The {@code Player} to return the name for.
     * @return A label for this location.
     */
    public StringTemplate getLocationLabelFor(Player player);

    /**
     * Adds a {@code Locatable} to this Location.
     *
     * @param locatable The {@code Locatable} to add to this Location.
     * @return True if the locatable was added.
     */
    public boolean add(Locatable locatable);

    /**
     * Removes a {@code Locatable} from this Location.
     *
     * @param locatable The {@code Locatable} to remove from this
     *     Location.
     * @return True if the locatable was removed.
     */
    public boolean remove(Locatable locatable);

    /**
     * Checks if this {@code Location} contains the specified
     * {@code Locatable}.
     *
     * @param locatable The {@code Locatable} to test the presence of.
     * @return True if the locatable is present at this location.
     */
    public boolean contains(Locatable locatable);

    /**
     * Checks whether or not the specified locatable may be added to this
     * {@code Location}.
     *
     * @param locatable The {@code Locatable} to add.
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
     * Gets a stream of all the units present at this location.
     *
     * @return A stream of all the units at this location.
     */
    public Stream<Unit> getUnits();

    /**
     * Gets a list of all the units present at this location.
     *
     * @return A list of all the units at this location.
     */
    public List<Unit> getUnitList();

    /**
     * Gets the {@code GoodsContainer} this {@code Location} use
     * for storing it's goods.
     *
     * @return The {@code GoodsContainer} or {@code null} if the
     *     {@code Location} cannot store any goods.
     */
    public GoodsContainer getGoodsContainer();

    /**
     * Gets the {@code Settlement} this {@code Location} is
     * located in.
     *
     * @return The associated {@code Settlement}, or null if none.
     */
    public Settlement getSettlement();

    /**
     * Get the colony at this location.
     *
     * @return A {@code Colony} at this location if any, or null
     *     if none found.
     */
    public Colony getColony();

    /**
     * Gets the native settlement at this location.
     *
     * @return The {@code IndianSettlement} at this location if
     *     any, or null if none found.
     */
    public IndianSettlement getIndianSettlement();

    /**
     * Promote this location to a more meaningful one if possible.
     *
     * For example: a settlement is more meaningful than the tile
     * it occupies.
     *
     * @return A more meaningful {@code Location}, or this one.
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
     * @param loc The {@code Location} to improve.
     * @return The improved {@code Location}.
     */
    public static Location upLoc(Location loc) {
        return (loc == null) ? null : loc.up();
    }

    /**
     * Static front end to getRank.
     *
     * @param loc A {@code Location} to check.
     * @return The integer rank of the given location.
     */
    public static int rankOf(Location loc) {
        return (loc == null) ? Location.LOCATION_RANK_NOWHERE : loc.getRank();
    }

    /**
     * Return an ImageIcon for a Location
     *
     * @param cellHeight The size of a destination cell, used by Europe
     * @param library The image library
     * @return The ImageIcon, null by default.
     */
    public default ImageIcon getLocationImage(final int cellHeight,
                                              final ImageLibrary library) {
        return null;
    }
}
