/**
 *  Copyright (C) 2002-2009  The FreeCol Team
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

import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;

/**
 * A <code>CostDecider</code> avoid units blocking the path.
 */
class AvoidBlockingUnitsCostDecider extends BaseCostDecider {
    
    /**
     * Determines the cost of a single move.
     * 
     * @param unit The <code>Unit</code> making the move.
     * @param oldTile The <code>Tile</code> we are moving from.
     * @param newTile The <code>Tile</code> we are moving to.
     * @param movesLeftBefore The remaining moves left.
     * @return The cost of moving the given unit from the
     *      <code>oldTile</code> to the <code>newTile</code>.
     */
    @Override
    public int getCost(final Unit unit,
            final Tile oldTile,
            final Tile newTile,
            final int movesLeftBefore,
            final int turns) {
        final int normalCost = super.getCost(unit, oldTile, newTile, movesLeftBefore, turns);
        if (normalCost == ILLEGAL_MOVE
                || normalCost == Map.COST_INFINITY) {
            return normalCost;
        }
        
        int extraCost = 0;
        final Unit defender = newTile.getFirstUnit();
        if (defender != null && defender.getOwner() != unit.getOwner()) {
            if (turns == 0) {
                return ILLEGAL_MOVE;
            } else {
                extraCost += Math.max(0, 20 - turns * 4);
            }
        } else if (newTile.isLand()
                && newTile.getFirstUnit() != null
                && newTile.getFirstUnit().isNaval()
                && newTile.getFirstUnit().getOwner() != unit.getOwner()) {
            // An enemy ship in land tile without a settlement is blocking the path:                
            extraCost += Math.max(0, 20 - turns * 4); 
        }
        return normalCost + extraCost;
    }
}    
