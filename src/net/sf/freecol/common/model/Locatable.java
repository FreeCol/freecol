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

/**
 * An object that can be put in a {@code Location}.
 */
public interface Locatable {

    /**
     * Gets the location of this {@code Locatable}.
     *
     * @return The location of this {@code Locatable}.
     */
    public Location getLocation();

    /**
     * Sets the location for this {@code Locatable}.
     *
     * @param newLocation The new {@code Location} for the
     *     {@code Locatable}.
     * @return True if the location change succeeds.
     */
    public boolean setLocation(Location newLocation);

    /**
     * Is this locatable in Europe.
     *
     * @return True if the {@code Locatable} is in {@code Europe}.
     */
    public boolean isInEurope();

    /**
     * Get the {@code Tile} where this {@code Locatable} is
     * located, or {@code null} if it is in {@code Europe}.
     *
     * @return The {@code Tile} where this {@code Locatable}
     *     is located, if any.
     */
    public Tile getTile();

    /**
     * Gets the number of cargo slots consumed when this
     * {@code Locatable} if put onto a carrier.
     *
     * @return The number of cargo slots required.
     */
    public int getSpaceTaken();
}
