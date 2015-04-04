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

import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;


/**
 * Class for determining the cost of a single move.
 * 
 * This {@link CostDecider} is used as a default by
 * {@link net.sf.freecol.common.model.Map#findPath} and 
 * {@link net.sf.freecol.common.model.Map#search} 
 * if no other <code>CostDecider</code> has been specified.
 */
class BaseCostDecider implements CostDecider {

    /** The number of moves left following a proposed move. */
    protected int movesLeft;

    /** The number of turns consumed by the proposed move. */
    protected int newTurns;


    /**
     * Determines the cost of a single move.
     * 
     * @param unit The <code>Unit</code> making the move.
     * @param oldLocation The <code>Location</code> we are moving from.
     * @param newLocation The <code>Location</code> we are moving to.
     * @param movesLeftBefore The moves left before making the move.
     * @return The cost of moving the given unit from the
     *      <code>oldLocation</code> to the <code>newLocation</code>.
     */    
    @Override
    public int getCost(final Unit unit, final Location oldLocation,
                       final Location newLocation, int movesLeftBefore) {
        int cost = 0;
        newTurns = 0;
              
        Tile oldTile = oldLocation.getTile();
        Tile newTile = newLocation.getTile();
        if (oldLocation instanceof Europe) { // Coming from Europe
            if (newLocation instanceof Europe
                || newTile == null
                || !newTile.isDirectlyHighSeasConnected()
                || !unit.getType().canMoveToHighSeas()) return ILLEGAL_MOVE;
            newTurns = unit.getSailTurns();
            movesLeft = unit.getInitialMovesLeft();
            cost = newTurns * unit.getInitialMovesLeft();

        } else if (oldTile == null) {
            return ILLEGAL_MOVE;

        } else if (newLocation instanceof Europe) { // Going to Europe
            if (!unit.getType().canMoveToHighSeas()) return ILLEGAL_MOVE;
            newTurns = unit.getSailTurns();
            movesLeft = unit.getInitialMovesLeft();
            cost = newTurns * unit.getInitialMovesLeft();

        } else if (newTile == null || !newTile.isExplored()) {
            return ILLEGAL_MOVE;

        } else { // Moving between tiles
            // Disallow illegal moves.
            // Special moves and moving off a carrier consume a whole turn.
            boolean consumeMove = false;
            switch (unit.getSimpleMoveType(oldTile, newTile)) {
            case MOVE_HIGH_SEAS:
                break;
            case ATTACK_UNIT:
                // Ignore hostile units in the base case, treating attacks
                // as moves.
            case MOVE:
                if (!unit.isOnCarrier()) break; // Fall through if disembarking.
            case ATTACK_SETTLEMENT:
            case EXPLORE_LOST_CITY_RUMOUR:
            case EMBARK:
            case ENTER_INDIAN_SETTLEMENT_WITH_FREE_COLONIST:
            case ENTER_INDIAN_SETTLEMENT_WITH_SCOUT:
            case ENTER_INDIAN_SETTLEMENT_WITH_MISSIONARY:
            case ENTER_FOREIGN_COLONY_WITH_SCOUT:
            case ENTER_SETTLEMENT_WITH_CARRIER_AND_GOODS:
                consumeMove = true;
                break;
            default:
                return ILLEGAL_MOVE;
            }

            cost = unit.getMoveCost(oldTile, newTile, movesLeftBefore);
            if (cost <= movesLeftBefore) {
                movesLeft = movesLeftBefore - cost;
            } else { // This move takes an extra turn to complete:
                final int thisTurnMovesLeft = movesLeftBefore;
                int initialMoves = unit.getInitialMovesLeft();
                final int moveCostNextTurn = unit.getMoveCost(oldTile, newTile,
                                                              initialMoves);
                cost = thisTurnMovesLeft + moveCostNextTurn;
                movesLeft = initialMoves - moveCostNextTurn;
                newTurns++;
            }
            if (consumeMove) {
                cost += movesLeft;
                movesLeft = 0;
            }
        }
        return cost;
    }
    
    /**
     * Gets the number of moves left after the proposed move.
     * This method should be called after invoking {@link #getCost}.
     * 
     * @return The number of moves left.
     */
    @Override
    public int getMovesLeft() {
        return movesLeft;
    }
    
    /**
     * Gets the number of turns consumed by the proposed move.
     * This method should be called after invoking {@link #getCost}.
     * 
     * @return The number of turns consumed.
     */      
    @Override
    public int getNewTurns() {
        return newTurns;
    }
}
