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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;


/**
 * Handy library of GoalDeciders.
 */
public final class GoalDeciders {

    /**
     * Gets a composite goal decider composed of two or more individual
     * goal deciders.  The first one dominates the second etc.
     *
     * @param all If true, all deciders must succeed (and-semantics),
     *     if false, any decide may succeed (or-semantics).
     * @param gds A series (two minimum) of <code>GoalDecider</code>s
     *     to compose.
     * @return A new <code>GoalDecider</code> composed of the argument
     *     goal deciders.
     */
    public static GoalDecider getComposedGoalDecider(final boolean all,
        final GoalDecider... gds) {
        if (gds.length < 2) {
            throw new IllegalArgumentException("Short GoalDecider list");
        }

        return new GoalDecider() {
            private int winner = gds.length;
            private PathNode goal = null;

            @Override
            public PathNode getGoal() { return goal; }
            @Override
            public boolean hasSubGoals() {
                for (int i = 0; i < gds.length; i++) {
                    if (!all && i > this.winner) break;
                    if (gds[i].hasSubGoals()) {
                        if (!all) return true;
                    } else {
                        if (all) return false;
                    }
                }
                return !all;
            }
            @Override
            public boolean check(Unit u, PathNode path) {
                for (int i = 0; i < gds.length; i++) {
                    if (!all && i > this.winner) break;
                    if (gds[i].check(u, path)) {
                        if (!all) {
                            this.winner = i;
                            this.goal = path;
                            return true;
                        }
                    } else {
                        if (all) {
                            return false;
                        }
                    }
                }
                if (all) {
                    this.winner = 0;
                    this.goal = path;
                    return true;
                }
                return false;
            }
        };
    }

    /**
     * Gets a GoalDecider to find the `closest' settlement owned by the
     * searching unit player, with connected ports weighted double.
     *
     * @return The closest settlement goal decider.
     */
    public static GoalDecider getOurClosestSettlementGoalDecider() {
        return new GoalDecider() {
            private PathNode bestPath = null;
            private float bestValue = 0.0f;

            @Override
            public PathNode getGoal() { return bestPath; }
            @Override
            public boolean hasSubGoals() { return true; }
            @Override
            public boolean check(Unit u, PathNode path) {
                Location loc = path.getLastNode().getLocation();
                Settlement settlement = loc.getSettlement();
                if (settlement != null && settlement.getOwner().owns(u)) {
                    float value = ((settlement.isConnectedPort()) ? 2.0f
                        : 1.0f) / (path.getTotalTurns() + 1);
                    if (bestValue < value) {
                        bestValue = value;
                        bestPath = path;
                        return true;
                    }
                }
                return false;
            }
        };
    }

    /**
     * Gets a GoalDecider to find the closest high seas tile to a target.
     * Used when arriving on the map from Europe.
     *
     * @return The high seas goal decider.
     */
    public static GoalDecider getHighSeasGoalDecider() {
        return new GoalDecider() {
            private PathNode best = null;
            
            @Override
            public PathNode getGoal() { return best; }
            @Override
            public boolean hasSubGoals() { return false; }
            @Override
            public boolean check(Unit u, PathNode path) {
                Tile tile = path.getTile();
                if (tile != null
                    && tile.isExploredBy(u.getOwner())
                    && tile.isDirectlyHighSeasConnected()
                    && (tile.getFirstUnit() == null
                        || u.getOwner().owns(tile.getFirstUnit()))) {
                    if (best == null || path.getCost() < best.getCost()) {
                        best = path;
                        return true;
                    }
                }
                return false;
            }
        };
    }

    /**
     * Build a simple goal decider to find the first high seas tile
     * without using the unit parameter.
     *
     * @return A <code>GoalDecider</code> that finds the nearest high seas tile.
     */
    public static GoalDecider getSimpleHighSeasGoalDecider() {
        return new GoalDecider() {
            private PathNode first = null;

            @Override
            public PathNode getGoal() { return first; }
            @Override
            public boolean hasSubGoals() { return false; }
            @Override
            public boolean check(Unit u, PathNode path) {
                Tile tile = path.getTile();
                if (tile != null
                    && tile.isDirectlyHighSeasConnected()) {
                    first = path;
                    return true;
                }
                return false;
            }
        };
    }

    /**
     * Builds a simple goal decider to find a single target location.
     *
     * @param target The target <code>Location</code>.
     * @return A <code>GoalDecider</code> that only succeeds for the
     *     target location.
     */
    public static GoalDecider getLocationGoalDecider(final Location target) {
        return new GoalDecider() {
            private PathNode best = null;
            private int bestCost = FreeColObject.INFINITY;

            @Override
            public PathNode getGoal() { return best; }
            @Override
            public boolean hasSubGoals() { return false; }
            @Override
            public boolean check(Unit u, PathNode path) {
                int cost;
                if (Map.isSameLocation(path.getLocation(), target)) {
                    if ((cost = path.getCost()) < bestCost) {
                        best = path;
                        bestCost = cost;
                    }
                    return true;
                }
                return false;
            }
        };
    }

    /**
     * Builds a goal decider to find an adjacent tile to a target location.
     *
     * @param target The target <code>Location</code>.
     * @return A <code>GoalDecider</code> that only succeeds for tiles
     *     adjacent to the target location.
     */
    public static GoalDecider getAdjacentLocationGoalDecider(Location target) {
        final Tile tile = target.getTile();
        if (tile == null) return null;

        return new GoalDecider() {
            private PathNode best = null;

            @Override
            public PathNode getGoal() { return best; }
            @Override
            public boolean hasSubGoals() { return false; }
            @Override
            public boolean check(Unit u, PathNode path) {
                Tile t = path.getTile();
                if (t != null && t.isAdjacent(tile)) {
                    best = path;
                    return true;
                }
                return false;
            }
        };
    }

    /**
     * Get a goal decider that succeeds for settlements owned by one
     * of a given list of enemies.
     *
     * @param enemies The list of enemy <code>Player</code>s.
     * @return A suitable <code>GoalDecider</code>.
     **/
    public static GoalDecider getEnemySettlementGoalDecider(final Collection<Player> enemies) {
        return new GoalDecider() {
            private PathNode best = null;
                        
            @Override
            public PathNode getGoal() { return best; }
            @Override
            public boolean hasSubGoals() { return false; }
            @Override
            public boolean check(Unit u, PathNode path) {
                Tile t = path.getTile();
                if (t == null || !t.isLand()) return false;
                Settlement s = t.getSettlement();
                if (s == null) return false;
                if (enemies.contains(s.getOwner())) {
                    best = path;
                    return true;
                }
                return false;
            }
        };
    }

    /**
     * Goal decider to find the best land tile to disembark a unit that
     * is planning to attack a given target.
     *
     * The result must be:
     * - Unoccupied
     * - Have at least one unoccupied high-seas-connected neighbour
     * - Favour the best natural defence of the alternatives
     * - Favour a short journey to the target
     * - Prioritize not landing next to a hostile fort/fortress.
     *
     * @param target The target <code>Tile</code>.
     * @return A suitable <code>GoalDecider</code>.
     */
    public static GoalDecider getDisembarkGoalDecider(final Tile target) {
        return new GoalDecider() {
            private double bestScore = -1.0;
            private boolean goalDangerous = true;
            private PathNode goal = null;

            @Override
            public PathNode getGoal() { return goal; }
            @Override
            public boolean hasSubGoals() { return true; }
            @Override
            public boolean check(Unit u, PathNode pathNode) {
                Tile tile = pathNode.getTile();
                if (tile == null || !tile.isLand() || !tile.isEmpty()
                    || tile.hasSettlement()) return false;
                final Player owner = u.getOwner();
                boolean found = false, danger = false;
                for (Tile t : pathNode.getTile().getSurroundingTiles(1)) {
                    if (!t.isHighSeasConnected() || t.isLand()) continue;
                    for (Tile t2 : t.getSurroundingTiles(1)) {
                        Settlement settlement = t2.getSettlement();
                        if (settlement != null
                            && !owner.owns(settlement)
                            && settlement.hasAbility(Ability.BOMBARD_SHIPS)
                            && (owner.atWarWith(settlement.getOwner())
                                || u.hasAbility(Ability.PIRACY))) {
                            danger = true;
                            break;
                        }
                    }
                    if (goalDangerous) {
                        found = true;
                        if (!danger) {
                            // Found first non-dangerous tile, reset scores
                            goalDangerous = false;
                            bestScore = -1.0f;
                        }
                    } else {
                        found = !danger; // Only accept non-dangerous tiles
                    }
                }
                if (found) {
                    double distance = 1.0 + u.getGame().getMap()
                        .getDistance(target, tile);
                    double score = tile.getDefenceValue() / distance;
                    if (bestScore < score) {
                        bestScore = score;
                        goal = pathNode;
                        return true;
                    }
                }
                return false;
            }
        };
    }
        
    /**
     * Get a goal decider to find tiles that an enemy player can not see.
     *
     * @param enemy The enemy <code>Player</code> to avoid.
     * @return A suitable <code>GoalDecider</code>.
     */
    public static GoalDecider getStealthyGoalDecider(final Player enemy) {
        return new GoalDecider() {
            private PathNode goal = null;

            @Override
            public PathNode getGoal() { return goal; }
            @Override
            public boolean hasSubGoals() { return true; }
            @Override
            public boolean check(Unit u, PathNode pathNode) {
                Tile tile = pathNode.getTile();
                if (enemy.canSee(tile)) return false;
                this.goal = pathNode;
                return true;
            }
        };
    }

    /**
     * Get a goal decider to find tiles with a settlement with a lower
     * high seas count than a unit currently has.  Useful for tunnelling
     * out of intermittantly blocked rivers.
     *
     * @param unit The <code>Unit</code> to get the goal decider for.
     * @return A suitable <code>GoalDecider</code>.
     */
    public static GoalDecider getReduceHighSeasCountGoalDecider(final Unit unit) {
        return new GoalDecider() {
            private PathNode goal = null;
            private int score = unit.getTile().getHighSeasCount();

            @Override
            public PathNode getGoal() { return goal; }
            @Override
            public boolean hasSubGoals() { return true; }
            @Override
            public boolean check(Unit u, PathNode pathNode) {
                Tile tile = pathNode.getTile();
                if (tile.getHighSeasCount() < score) {
                    Settlement s = tile.getSettlement();
                    if (unit.getOwner().owns(s)) {
                        this.goal = pathNode;
                        this.score = tile.getHighSeasCount();
                        return true;
                    }
                }
                return false;
            }
        };
    }

    /**
     * Get a goal decider to find tiles on river corners, preferring ones
     * closest to the high seas.  This is useful when a unit is stuck on
     * a river.  By moving to a corner (i.e. where another unit can get
     * past it) the chance that the blockage clears is enhanced.
     *
     * @return A suitable goal decider.
     */
    public static GoalDecider getCornerGoalDecider() {
        return new GoalDecider() {
            private PathNode goal = null;
            private int score = Integer.MAX_VALUE;

            @Override
            public PathNode getGoal() { return goal; }
            @Override
            public boolean hasSubGoals() { return true; }
            @Override
            public boolean check(Unit u, PathNode pathNode) {
                Tile tile = pathNode.getTile();
                if (tile.getHighSeasCount() < score && tile.isRiverCorner()) {
                    score = tile.getHighSeasCount();
                    goal = pathNode;
                    return true;
                }
                return false;
            }
        };
    }

    /**
     * A class to wrap a goal decider that searches for paths to an
     * adjacent tile to a set of locations, and the results of such a
     * search.
     */
    public static class MultipleAdjacentDecider {

        private final GoalDecider gd;

        private final HashMap<Location, PathNode> results = new HashMap<>();


        /**
         * Create a multiple decider.
         *
         * @param locs The list of <code>Location</code>s to search for
         *     paths to an adjacent location for.
         */
        public MultipleAdjacentDecider(final List<Location> locs) {
            this.gd = new GoalDecider() {

                    @Override
                    public PathNode getGoal() { return null; }
                    @Override
                    public boolean hasSubGoals() { return true; }
                    @Override
                    public boolean check(Unit u, PathNode path) {
                        Tile tile = path.getTile();
                        if (tile == null) return false;
                        for (Location loc : locs) {
                            if (tile.isAdjacent(loc.getTile())) {
                                PathNode p = results.get(loc);
                                if (p == null
                                    || p.getCost() > path.getCost()) {
                                    results.put(loc, path);
                                }
                            }
                        }
                        return false;
                    }
                };
        }

        public GoalDecider getGoalDecider() {
            return gd;
        }

        public HashMap<Location, PathNode> getResults() {
            return results;
        }
    };
}
