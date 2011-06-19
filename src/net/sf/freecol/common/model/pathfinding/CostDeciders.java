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

package net.sf.freecol.common.model.pathfinding;

import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Unit;


/**
 * Cost deciders to be used while finding paths.
 */
public final class CostDeciders {

    /**
     * A <code>CostDecider</code> only considering the number of tiles
     * visited when determining the cost.
     */
    private static final CostDecider tileCostDecider
        = new CostDecider() {
            public int getCost(Unit unit, Tile oldTile, Tile newTile,
                               int movesLeft, int turns) {
                if (unit.isNaval()) {
                    if (!newTile.isLand()) return 1;
                    Settlement settlement = newTile.getSettlement();
                    return (settlement != null
                            && settlement.getOwner().equals(unit.getOwner())) ? 1
                        : ILLEGAL_MOVE;
                } else {
                    return (newTile.isLand()) ? 1 : ILLEGAL_MOVE;
                }
            }
            public int getMovesLeft() {
                return 0;
            }
            public boolean isNewTurn() {
                return false;
            }
        };


    /**
     * A <code>CostDecider</code> that costs unit moves normally.
     */
    private static final CostDecider
        avoidIllegalCostDecider = new BaseCostDecider();


    /**
     * A <code>CostDecider</code> that costs unit moves normally while
     * avoiding other player settlements.
     */
    private static class AvoidSettlementsCostDecider
        extends BaseCostDecider {
        @Override
        public int getCost(Unit unit, Tile oldTile, Tile newTile,
                           int movesLeft, int turns) {
            int cost = super.getCost(unit, oldTile, newTile,
                                     movesLeft, turns);
            if (cost != ILLEGAL_MOVE && cost != Map.COST_INFINITY) {
                Settlement settlement = newTile.getSettlement();
                if (settlement != null
                    && settlement.getOwner() != unit.getOwner()) {
                    return ILLEGAL_MOVE;
                }
            }
            return cost;
        }
    };
    private static final AvoidSettlementsCostDecider
        avoidSettlementsCostDecider = new AvoidSettlementsCostDecider();


    /**
     * A <code>CostDecider</code> that costs unit moves normally while
     * avoiding other player settlements and units, and does not explore
     * if it is trading.
     */
    private static class AvoidSettlementsAndBlockingUnitsCostDecider
        extends BaseCostDecider {
        @Override
        public int getCost(Unit unit, Tile oldTile, Tile newTile,
                               int movesLeft, int turns) {
            int cost = super.getCost(unit, oldTile, newTile,
                                     movesLeft, turns);
            if (cost != ILLEGAL_MOVE && cost != Map.COST_INFINITY) {
                Settlement settlement = newTile.getSettlement();
                if (settlement != null
                    && settlement.getOwner() != unit.getOwner()) {
                    return ILLEGAL_MOVE;
                }
                final Unit defender = newTile.getFirstUnit();
                if (defender != null
                    && defender.getOwner() != unit.getOwner()) {
                    if (turns == 0) {
                        return ILLEGAL_MOVE;
                    }
                    cost += Math.max(0, 20 - turns * 4);
                } else if (newTile.isLand()
                           && newTile.getFirstUnit() != null
                           && newTile.getFirstUnit().isNaval()
                           && newTile.getFirstUnit().getOwner() != unit.getOwner()) {
                    // An enemy ship in land tile without a settlement
                    // is blocking the path:
                    cost += Math.max(0, 20 - turns * 4);
                } else if (unit.getTradeRoute() != null
                           && unit.getMoveType(newTile)
                           == Unit.MoveType.EXPLORE_LOST_CITY_RUMOUR) {
                    return ILLEGAL_MOVE;
                }
            }
            return cost;
        }
    };
    private static final AvoidSettlementsAndBlockingUnitsCostDecider
        avoidSettlementsAndBlockingUnitsCostDecider
        = new AvoidSettlementsAndBlockingUnitsCostDecider();


    /**
     * Selects a default <code>CostDecider</code> for the given unit
     * depending on the owner of the unit (ai/human) and if the unit
     * can attack other units.
     * 
     * @param unit The <code>Unit</code> to choose a CostDecider for.
     * @return A suitable <code>CostDecider</code>.
     */
    public static CostDecider defaultCostDeciderFor(final Unit unit) {
        return (unit == null
                || !unit.getOwner().isAI()
                || !unit.isOffensiveUnit())
            ? avoidSettlementsAndBlockingUnits()
            : avoidSettlements();
    }

    /**
     * A <code>CostDecider</code> only considering the number of tiles
     * visited when determining the cost.
     * 
     * @return The <code>CostDecider</code>.
     */
    public static CostDecider numberOfTiles() {
        return tileCostDecider;
    }

    /**
     * A <code>CostDecider</code> that returns the cost of moving
     * across the terrain, excluding only illegal moves.
     *
     * @return The <code>CostDecider</code>.
     */
    public static CostDecider avoidIllegal() {
        return avoidIllegalCostDecider;
    }

    /**
     * A <code>CostDecider</code> returning only the cost of moving
     * across the terrain (no additional cost for blocking enemy units
     * etc) but excluding settlements.
     * 
     * @return The <code>CostDecider</code>.
     */
    public static CostDecider avoidSettlements() {
        return avoidSettlementsCostDecider;
    }
    
    /**
     * A <code>CostDecider</code> for avoiding using tiles which have
     * blocking enemy units on them. Paths containing an enemy
     * settlement are considered illegal, and so are paths where the
     * next move has an enemy unit on it.
     * 
     * @return The <code>CostDecider</code>.
     */
    public static CostDecider avoidSettlementsAndBlockingUnits() {
        return avoidSettlementsAndBlockingUnitsCostDecider;
    }

}
