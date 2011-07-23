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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;


/**
 * A representation of the "New World" intended to be used as a
 * destination only.
 *
 * @see Locatable
 */
public class NewWorld implements Location {


    /**
     * Returns <code>null</code>.
     *
     * @return <code>null</code>
     */
    public Tile getTile() {
        return null;
    }

    /**
     * Returns the name of this location.
     *
     * @return The name of this location.
     */
    public StringTemplate getLocationName() {
        return StringTemplate.key("NewWorld");
    }

    /**
     * Returns the name of this location for a particular player.
     *
     * @param player The <code>Player</code> to return the name for.
     * @return The name of this location.
     */
    public StringTemplate getLocationNameFor(Player player) {
        return StringTemplate.name(player.getNewLandName());
    }

    /**
     * Adds a <code>Locatable</code> to this Location. It the given
     * Locatable is a Unit, its location is set to its entry location,
     * otherwise nothing happens.
     *
     * @param locatable
     *            The <code>Locatable</code> to add to this Location.
     */
    public void add(Locatable locatable) {
        if (locatable instanceof Unit) {
            Unit unit = (Unit) locatable;
            unit.setLocation(unit.getEntryLocation());
        }
    }

    /**
     * Removes a <code>Locatable</code> from this Location.
     *
     * @param locatable
     *            The <code>Locatable</code> to remove from this Location.
     */
    public void remove(Locatable locatable) {
        if (locatable instanceof Unit) {
            Tile tile = ((Unit) locatable).getTile();
            if (tile != null) {
                tile.remove(locatable);
            }
        }
    }

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
    public boolean contains(Locatable locatable) {
        if (locatable.getLocation() == null) {
            return false;
        } else if (locatable.getLocation().getTile() == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Checks whether or not the specified locatable may be added to this
     * <code>Location</code>.
     *
     * @param locatable
     *            The <code>Locatable</code> to add.
     * @return The result.
     */
    public boolean canAdd(Locatable locatable) {
        return (locatable instanceof Unit);
    }

    /**
     * Returns <code>-1</code>
     *
     * @return <code>-1</code>
     */
    public int getUnitCount() {
        return -1;
    }

    /**
     * Returns an empty list.
     *
     * @return an empty list
     */
    public List<Unit> getUnitList() {
        return Collections.emptyList();
    }

    /**
     * Returns an <code>Iterator</code> for an empty list.
     *
     * @return The <code>Iterator</code>.
     */
    public Iterator<Unit> getUnitIterator() {
        return getUnitList().iterator();
    }

    /**
     * Returns <code>NO_ID</code>.
     *
     * @return The ID.
     * @see FreeColGameObject#getId
     */
    public String getId() {
        return FreeColObject.NO_ID;
    }

    /**
     * Gets the <code>GoodsContainer</code> this <code>Location</code> use
     * for storing it's goods.
     *
     * @return The <code>GoodsContainer</code> or <code>null</code> if the
     *         <code>Location</code> cannot store any goods.
     */
    public GoodsContainer getGoodsContainer() {
        return null;
    }

    /**
     * Returns <code>null</code>.
     *
     * @return <code>null</code>
     */
    public Settlement getSettlement() {
        return null;
    }

    /**
     * Returns <code>null</code>.
     *
     * @return <code>null</code>
     */
    public Colony getColony() {
        return null;
    }

}
