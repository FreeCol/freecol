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

/**
 * An object that can be put in a <code>Location</code>.
 */
public interface Locatable {

    /**
     * Sets the location for this <code>Locatable</code>.
     * @param newLocation The new <code>Location</code> for the <code>Locatable</code>.
     */
    public void setLocation(Location newLocation);


    /**
     * Gets the location of this <code>Locatable</code>.
     * @return The location of this <code>Locatable</code>.
     */
    public Location getLocation();


    /**
     * Returns the <code>Tile</code> where this <code>Locatable</code> is located,
     * or <code>null</code> if it's location is <code>Europe</code>.
     *
     * @return The Tile where this Unit is located. Or null if
     * its location is Europe.
     */
    public Tile getTile();


    /**
     * Gets the amount of space this <code>Locatable</code> take.
     * @return The space it takes on a carrier.
     */
    public int getSpaceTaken();

}
