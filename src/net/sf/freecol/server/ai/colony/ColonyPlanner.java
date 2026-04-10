/**
 *  Copyright (C) 2002-2024   The FreeCol Team
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

package net.sf.freecol.server.ai.colony;

import net.sf.freecol.common.model.Tile;

/**
 * Creates a ColonyPlan for an ideal utilization of a colony on the given tile.
 * 
 * Assumes perfect conditions like all bonus effect (SoL 100%, Henry Hudson etc)
 */
public interface ColonyPlanner {

    /**
     * Creates a new colony plan.
     * 
     * @param centerTile The <code>Tile</code> where the colony is/will be located.
     * @return An idealized plan that does not take into account the current state
     *      of the colony. 
     */
    ColonyPlan createPlan(Tile centerTile);
    
}
