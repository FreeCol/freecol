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

/**
 * An object that can be put in a <code>Location</code>.
 */
public interface Locatable {

    /**
     * Gets the location of this <code>Locatable</code>.
     *
     * @return The location of this <code>Locatable</code>.
     */
    public Location getLocation();

    /**
     * Sets the location for this <code>Locatable</code>.
     *
     * @param newLocation The new <code>Location</code> for the
     *     <code>Locatable</code>.
     * @return True if the location change succeeds.
     */
    public boolean setLocation(Location newLocation);

    /**
     * Is this locatable in Europe.
     *
     * @return True if the <code>Locatable</code> is in <code>Europe</code>.
     */
    public boolean isInEurope();

    /**
     * Get the <code>Tile</code> where this <code>Locatable</code> is
     * located, or <code>null</code> if it is in <code>Europe</code>.
     *
     * @return The <code>Tile</code> where this <code>Locatable</code>
     *     is located, if any.
     */
    public Tile getTile();

    /**
     * Gets the number of cargo slots consumed when this
     * <code>Locatable</code> if put onto a carrier.
     *
     * @return The number of cargo slots required.
     */
    public int getSpaceTaken();
}
