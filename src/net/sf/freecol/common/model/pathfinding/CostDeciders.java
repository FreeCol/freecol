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

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;


/**
 * Cost deciders to be used while finding paths.
 */
public final class CostDeciders {

    /**
     * A <code>CostDecider</code> that costs unit moves normally.
     */
    private static final CostDecider avoidIllegalCostDecider
        = new BaseCostDecider();


    /**
     * A trivial <code>CostDecider</code> that only considers the
     * number of locations visited when determining cost.  Totally ignores
     * the legality of the move.
     */
    private static final CostDecider trivialCostDecider = new CostDecider() {
            @Override
            public int getCost(Unit unit, Location oldLocation,
                               Location newLocation, int movesLeft) {
                return (newLocation == null) ? ILLEGAL_MOVE
                    : (newLocation instanceof Europe) ? 1
                    : (newLocation.getTile() == null) ? ILLEGAL_MOVE
                    : 1;
            }
            @Override
            public int getMovesLeft() { return 0; }
            @Override
            public int getNewTurns() { return 0; }
        };


    /**
     * A <code>CostDecider</code> that only considers the number of
     * tiles visited when determining the cost, but differs from the
     * trivialCostDecider in checking the legality of the move.
     */
    private static final CostDecider tileCostDecider = new CostDecider() {
            @Override
            public int getCost(Unit unit, Location oldLocation,
                               Location newLocation, int movesLeft) {
                return (newLocation == null) ? ILLEGAL_MOVE
                    : (newLocation instanceof Europe) ? 1
                    : (newLocation.getTile() == null) ? ILLEGAL_MOVE
                    : (unit.isTileAccessible(newLocation.getTile())) ? 1
                    : ILLEGAL_MOVE;
            }
            @Override
            public int getMovesLeft() { return 0; }
            @Override
            public int getNewTurns() { return 1; }
        };


    /**
     * A <code>CostDecider</code> that uses server-side knowledge of where
     * a player has explored to limit searches.
     */
    private static class ServerBaseCostDecider extends BaseCostDecider {
        @Override
        public int getCost(Unit unit, Location oldLocation,
                           Location newLocation, int movesLeft) {
            int cost = super.getCost(unit, oldLocation, newLocation, movesLeft);
            if (cost != ILLEGAL_MOVE && cost != Map.INFINITY) {
                if (newLocation instanceof Europe) {
                    ; // ok
                } else if (!newLocation.getTile().isExploredBy(unit.getOwner())) {
                    return ILLEGAL_MOVE;
                }
            }
            return cost;
        }
    };
    /**
     * A server-side <code>CostDecider</code> that costs unit moves normally.
     */
    private static final CostDecider
        serverAvoidIllegalCostDecider = new ServerBaseCostDecider();


    /**
     * A <code>CostDecider</code> that costs unit moves normally while
     * avoiding other player settlements.
     */
    private static class AvoidSettlementsCostDecider extends BaseCostDecider {
        @Override
        public int getCost(Unit unit, Location oldLocation,
                           Location newLocation, int movesLeft) {
            int cost = super.getCost(unit, oldLocation, newLocation, movesLeft);
            if (cost != ILLEGAL_MOVE && cost != Map.INFINITY) {
                Settlement settlement = newLocation.getSettlement();
                if (settlement != null
                    && settlement.getOwner() != unit.getOwner()) {
                    return ILLEGAL_MOVE;
                }
            }
            return cost;
        }
    };
    /**
     * An instance of the cost decider for avoiding settlements.
     */
    private static final AvoidSettlementsCostDecider
        avoidSettlementsCostDecider = new AvoidSettlementsCostDecider();


    /**
     * A <code>CostDecider</code> that costs unit moves normally while
     * avoiding other player settlements and units, and does not explore
     * if it is trading.
     */
    private static class AvoidSettlementsAndBlockingUnitsCostDecider
        extends AvoidSettlementsCostDecider {
        @Override
        public int getCost(Unit unit, Location oldLocation,
                           Location newLocation, int movesLeft) {
            int cost = super.getCost(unit, oldLocation, newLocation, movesLeft);
            Tile tile = newLocation.getTile();
            if (cost != ILLEGAL_MOVE && cost != Map.INFINITY
                && tile != null) {
                final Unit defender = tile.getFirstUnit();
                if (defender != null
                    && defender.getOwner() != unit.getOwner()) {
                    return ILLEGAL_MOVE;
                } else if (unit.getTradeRoute() != null
                    && tile.hasLostCityRumour()) {
                    return ILLEGAL_MOVE;
                }
            }
            return cost;
        }
    };

    /**
     * An instance of the settlement+unit avoiding cost decider.
     */
    private static final AvoidSettlementsAndBlockingUnitsCostDecider
        avoidSettlementsAndBlockingUnitsCostDecider
        = new AvoidSettlementsAndBlockingUnitsCostDecider();


    /**
     * A <code>CostDecider</code> to avoid naval danger.
     */
    private static class AvoidNavalDangerCostDecider
        extends AvoidSettlementsAndBlockingUnitsCostDecider {

        @Override
        public int getCost(Unit unit, Location oldLocation,
                           Location newLocation, int movesLeft) {
            int cost = super.getCost(unit, oldLocation, newLocation, movesLeft);
            Tile tile = newLocation.getTile();
            if (cost != ILLEGAL_MOVE && cost != Map.INFINITY && tile != null) {
                if (tile.isDangerousToShip(unit)) {
                    cost = ILLEGAL_MOVE;
                } else {
                    final Player owner = unit.getOwner();
                    tiles: for (Tile t : tile.getSurroundingTiles(1)) {
                        for (Unit u : t.getUnitList()) {
                            if (u.getOwner() == owner) break;
                            if (u.hasAbility(Ability.PIRACY)
                                || (u.getOwner().atWarWith(owner)
                                    && u.isOffensiveUnit())) {
                                this.movesLeft = 0;
                                this.newTurns++;
                                break tiles;
                            }
                        }
                    }
                }
            }
            return cost;
        }
    };


    // Public interface

    /**
     * Gets a composite cost decider composed of two or more
     * individual cost deciders.  The result/s are determined by the
     * cost decider which returns the highest cost, with an
     * ILLEGAL_MOVE result dominating.
     *
     * @param cds A series (two minimum) of <code>CostDecider</code>s
     *     to compose.
     * @return A new <code>CostDecider</code> composed of the argument
     *     cost deciders.
     */
    public static CostDecider getComposedCostDecider(final CostDecider... cds) {
        if (cds.length < 2) {
            throw new IllegalArgumentException("Short CostDecider list");
        }

        return new CostDecider() {

            private final CostDecider[] costDeciders = cds;
            private int ret = -1;
            private int index = -1;

            @Override
            public int getCost(Unit unit, Location oldLocation,
                               Location newLocation, int movesLeft) {
                for (int i = 0; i < costDeciders.length; i++) {
                    int cost = costDeciders[i].getCost(unit, oldLocation,
                                                       newLocation, movesLeft);
                    if (cost == ILLEGAL_MOVE || cost == Map.INFINITY) {
                        index = i;
                        return ILLEGAL_MOVE;
                    }
                    if (cost > ret) {
                        index = i;
                        ret = cost;
                    }
                }
                return ret;
            }

            @Override
            public int getMovesLeft() {
                return (index < 0) ? 0 : costDeciders[index].getMovesLeft();
            }

            @Override
            public int getNewTurns() {
                return (index < 0) ? 0 : costDeciders[index].getNewTurns();
            }
        };
    }

    /**
     * Selects a default <code>CostDecider</code> for the given unit
     * depending on the owner of the unit and if the unit can attack
     * other units.
     *
     * @param unit The <code>Unit</code> to choose a CostDecider for.
     * @return A suitable <code>CostDecider</code>.
     */
    public static CostDecider defaultCostDeciderFor(final Unit unit) {
        return (unit == null)
            ? avoidIllegal()
            : (unit.isNaval())
            ? avoidNavalDanger()
            : (unit.isOffensiveUnit())
            ? avoidSettlements()
            : avoidSettlementsAndBlockingUnits();
    }

    /**
     * The trivial <code>CostDecider</code>.
     *
     * @return The <code>CostDecider</code>.
     */
    public static CostDecider numberOfTiles() {
        return trivialCostDecider;
    }

    /**
     * A <code>CostDecider</code> only considering the number of tiles
     * visited when determining the cost.
     *
     * @return The <code>CostDecider</code>.
     */
    public static CostDecider numberOfLegalTiles() {
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
     * A <code>CostDecider</code> that returns the cost of moving
     * across the terrain, excluding only illegal moves, and works correctly
     * on the server side by refusing to consider locations unexplored by the
     * player.
     *
     * @return The <code>CostDecider</code>.
     */
    public static CostDecider serverAvoidIllegal() {
        return serverAvoidIllegalCostDecider;
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
     * A <code>CostDecider</code> for avoiding using locations which have
     * blocking enemy units on them. Paths containing an enemy
     * settlement are considered illegal, and so are paths where the
     * next move has an enemy unit on it.
     *
     * @return The <code>CostDecider</code>.
     */
    public static CostDecider avoidSettlementsAndBlockingUnits() {
        return avoidSettlementsAndBlockingUnitsCostDecider;
    }

    /**
     * A <code>CostDecider</code> for avoiding using locations which have
     * blocking enemy units or expose naval units to bombardment.
     *
     * @return The <code>CostDecider</code>.
     */
    public static CostDecider avoidNavalDanger() {
        return new AvoidNavalDangerCostDecider();
    }
}
