/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.sf.freecol.common.model.Europe;
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
     * @param gds A series (two minimum) of <code>GoalDecider</code>s
     *     to compose.
     * @return A new <code>GoalDecider</code> composed of the argument
     *     goal deciders.
     */
    public static GoalDecider getComposedGoalDecider(final GoalDecider... gds) {
        if (gds.length < 2) {
            throw new IllegalArgumentException("Short GoalDecider list");
        }

        return new GoalDecider() {
            private GoalDecider[] goalDeciders = gds;

            public PathNode getGoal() {
                for (int i = 0; i < goalDeciders.length; i++) {
                    PathNode path = goalDeciders[i].getGoal();
                    if (path != null) return path;
                }
                return null;
            }
            public boolean hasSubGoals() { return true; }
            public boolean check(Unit u, PathNode path) {
                boolean ret = false;
                for (int i = goalDeciders.length-1; i >= 0; i--) {
                    ret = goalDeciders[i].check(u, path);
                }
                return ret;
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

            public PathNode getGoal() { return bestPath; }
            public boolean hasSubGoals() { return true; }
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
            
            public PathNode getGoal() { return best; }
            public boolean hasSubGoals() { return false; }
            public boolean check(Unit u, PathNode path) {
                Tile tile = path.getTile();
                if (tile != null
                    && tile.isDirectlyHighSeasConnected()
                    && (tile.getFirstUnit() == null
                        || tile.getFirstUnit().getOwner() == u.getOwner())) {
                    if (best == null || path.getCost() < best.getCost()) {
                        best = path;
                    }
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

            public PathNode getGoal() { return best; }
            public boolean hasSubGoals() { return false; }
            public boolean check(Unit u, PathNode path) {
                if (Map.isSameLocation(path.getLocation(), target)) {
                    best = path;
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
     * @return A <code>GoalDecider</code> that only succeeds for tiles adjacent
     *     to the target location.
     */
    public static GoalDecider getAdjacentLocationGoalDecider(Location target) {
        final Tile tile = target.getTile();
        if (tile == null) return null;

        return new GoalDecider() {
            private PathNode best = null;

            public PathNode getGoal() { return best; }
            public boolean hasSubGoals() { return false; }
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
     * A class to wrap a goal decider that searches for paths to an adjacent
     * tile to a set of locations, and the results of such a search.
     */
    public static class MultipleDecider {

        private final GoalDecider gd;

        private final HashMap<Location, PathNode> results
            = new HashMap<Location, PathNode>();


        /**
         * Create a multiple decider.
         *
         * @param locs The list of <code>Location</code>s to search for
         *     paths to an adjacent location for.
         */
        public MultipleDecider(final List<Location> locs) {
            this.gd = new GoalDecider() {

                    private List<Location> done = new ArrayList<Location>();

                    public PathNode getGoal() { return null; }
                    public boolean hasSubGoals() { return true; }
                    public boolean check(Unit u, PathNode path) {
                        Tile tile = path.getTile();
                        if (tile == null) return false;
                        for (Location loc : locs) {
                            if (tile.isAdjacent(loc.getTile())) {
                                results.put(loc, path);
                                done.add(loc);
                            }
                        }
                        while (!done.isEmpty()) {
                            locs.remove(done.remove(0));
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
