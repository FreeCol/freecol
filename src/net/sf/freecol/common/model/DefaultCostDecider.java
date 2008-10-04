/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

import net.sf.freecol.common.model.Unit.MoveType;

/**
 * Class for determining the cost of a single move.
 * 
 * <br /><br />
 * 
 * This {@link CostDecider} is used as a default by
 * {@link Map#findPath(Unit, Tile, Tile) findPath} and 
 * {@link Map#search(Unit, Tile, GoalDecider, CostDecider, int, Unit) search} 
 * if no other <code>CostDecider</code> has been specified.
 */
public class DefaultCostDecider implements CostDecider {
    

    private int movesLeft;
    private boolean newTurn;
    
    /**
     * Determines the cost of a single move.
     * 
     * @param unit The <code>Unit</code> making the move.
     * @param oldTile The <code>Tile</code> we are moving from.
     * @param newTile The <code>Tile</code> we are moving to.
     * @param ml The remaining moves left.
     * @return The cost of moving the given unit from the
     *      <code>oldTile</code> to the <code>newTile</code>.
     */    
    public int getCost(Unit unit, Tile oldTile, Tile newTile, int ml, int turns) {
        movesLeft = ml;
        newTurn = false;
        
        if (!newTile.isExplored()) {
            // Not allowed to use an unexplored tile for a path:
            return ILLEGAL_MOVE;
        } else if (newTile.isLand() && unit.isNaval() && (newTile.getSettlement() == null
                || newTile.getSettlement().getOwner() != unit.getOwner())) {
            // Not allowed to move a naval unit on land:
            return ILLEGAL_MOVE;
        } else if (!newTile.isLand() && !unit.isNaval()) {
            // Not allowed to move a land unit on water:
            return ILLEGAL_MOVE;
        } else {
            int mc = unit.getMoveCost(oldTile, newTile, ml);
            MoveType moveType = unit.getMoveType(oldTile, newTile, ml);
            Unit defender = newTile.getFirstUnit();
            
            if (newTile.getSettlement() != null
                    && newTile.getSettlement().getOwner() != unit.getOwner()) {
                // A settlement is blocking the path:   
                switch (moveType) {
                    case ENTER_SETTLEMENT_WITH_CARRIER_AND_GOODS:
                    case ENTER_INDIAN_VILLAGE_WITH_FREE_COLONIST:
                    case ENTER_INDIAN_VILLAGE_WITH_MISSIONARY:
                    case ENTER_INDIAN_VILLAGE_WITH_SCOUT:
                    case ENTER_FOREIGN_COLONY_WITH_SCOUT:
                    case ATTACK:
                        if (unit.getDestination() == null ||
                                unit.getDestination().getTile() != newTile) {
                            movesLeft = 0;
                            return ml + unit.getInitialMovesLeft() * 5;
                        }
                        break;
                    case ILLEGAL_MOVE:
                        if (ml > 0) {
                            movesLeft = 0;
                            return ml + unit.getInitialMovesLeft() * 5;
                        }
                        break;
                }
            } else if (defender != null && defender.getOwner() != unit.getOwner()) {
                // A unit is blocking the path:                
                if (moveType != MoveType.ATTACK || unit.getDestination() == null ||
                    unit.getDestination().getTile() != newTile) {
                    mc += Math.max(0, 20 - turns * 4);
                }
            } else if (newTile.isLand() && newTile.getFirstUnit() != null &&
                    newTile.getFirstUnit().isNaval() &&
                    newTile.getFirstUnit().getOwner() != unit.getOwner()) {
                // An enemy ship in land tile without a settlement is blocking the path:                
                mc += Math.max(0, 20 - turns * 4); 
            }

            if (mc <= ml) {
                movesLeft -= mc;
                return mc;
            } else {
                // This move takes an extra turn to complete:
                mc = getCost(unit, oldTile, newTile, unit.getInitialMovesLeft(), turns+1);
                newTurn = true;
                return ml + mc;
            }
        }
    }
    
    /**
     * Gets the number of moves left. This method should be
     * called after invoking {@link #getCost}.
     * 
     * @return The number og moves left.
     */
    public int getMovesLeft() {
        return movesLeft;
    }
    
    /**
     * Checks if a new turn is needed in order to make the
     * move. This method should be called after invoking 
     * {@link #getCost}.
     * 
     * @return <code>true</code> if the move requires a
     *      new turn and <code>false</code> otherwise.
     */      
    public boolean isNewTurn() {
        return newTurn;
    }
}    
