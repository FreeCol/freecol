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


package net.sf.freecol.common.model.pathfinding;

import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Unit;


/**
 * Determines the cost of a single move.
 * Used by {@link net.sf.freecol.common.model.Map#findPath}
 * and {@link net.sf.freecol.common.model.Map#search}.
 */
public interface CostDecider {

    public static final int ILLEGAL_MOVE = -1;
    
    /**
     * Determines the cost of a single move.
     * 
     * @param unit The <code>Unit</code> that will be used when
     *      determining the cost. This should be the same type
     *      of unit as the one following the path.
     * @param oldLocation The <code>Location</code> we are moving from.
     * @param newLocation The <code>Location</code> we are moving to.
     * @param movesLeftBefore The moves left to the unit prior to moving.
     * @return The cost of moving the given unit from the
     *     <code>oldLocation</code> to the <code>newLocation</code>.
     */
    public int getCost(Unit unit, Location oldLocation, Location newLocation,
                       int movesLeftBefore);
    
    /**
     * Gets the number of moves left. 
     * This method should be called after invoking {@link #getCost}.
     * 
     * @return The number of moves left.
     */
    public int getMovesLeft();

    /**
     * Gets the number of turns consumed.
     * This method should be called after invoking {@link #getCost}.
     * 
     * @return The number of turns consumed.
     */    
    public int getNewTurns();    
}
