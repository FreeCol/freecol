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
 * Interface for objects which can be owned by a {@code Player}.
 * @see Player
 */
public interface Ownable {

    /**
     * Gets the owner of this {@code Ownable}.
     *
     * @return The {@code Player} controlling this {@code Ownable}.
     */
    public Player getOwner();

    /**
     * Sets the owner of this {@code Ownable}.
     *
     * @param p The {@code Player} that should take ownership
     *     of this {@code Ownable}.
     * @exception UnsupportedOperationException if not implemented.
     */
    public void setOwner(Player p);
}
