/**
 *  Copyright (C) 2002-2015  The FreeCol Team
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

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.common.model.pathfinding.GoalDeciders;
import net.sf.freecol.server.model.ServerUnit;

import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.FreeColTestUtils;



public class PathfindingTest extends FreeColTestCase {

    private final TileType plainsType
        = spec().getTileType("model.tile.plains");

    private final UnitType colonistType
        = spec().getUnitType("model.unit.freeColonist");



    public void testComposedGoalDeciders() {
        final Game game = getStandardGame();
        final Map map = getCoastTestMap(plainsType, true);
        game.setMap(map);
        final Player dutch = game.getPlayerByNationId("model.nation.dutch");
        PathNode path;
        GoalDecider gd;

        Tile colonyTile = map.getTile(9, 2);
        Colony colony = FreeColTestUtils.getColonyBuilder().player(dutch)
            .colonyTile(colonyTile).build();
        assertTrue(colonyTile.isShore());

        Tile unitTile = map.getTile(9, 3);
        Unit unit = new ServerUnit(game, unitTile, dutch, colonistType);

        Tile nativeTile = map.getTile(9, 4);
        IndianSettlement is = new FreeColTestCase.IndianSettlementBuilder(game)
            .settlementTile(nativeTile).build();
        assertTrue(nativeTile.isShore());
            
        GoalDecider settlementGD = new GoalDecider() {
                private PathNode goal = null;
                public PathNode getGoal() { return goal; }
                public boolean hasSubGoals() { return true; }
                public boolean check(Unit u, PathNode path) {
                    Tile tile = path.getTile();
                    if (tile.hasSettlement()) {
                        goal = path;
                        return true;
                    }
                    return false;
                }
            };

        GoalDecider colonyGD = new GoalDecider() {
                private PathNode goal = null;
                public PathNode getGoal() { return goal; }
                public boolean hasSubGoals() { return true; }
                public boolean check(Unit u, PathNode path) {
                    Tile tile = path.getTile();
                    if (tile.getColony() != null) {
                        goal = path;
                        return true;
                    }
                    return false;
                }
            };

        GoalDecider nativeGD = new GoalDecider() {
                private PathNode goal = null;
                public PathNode getGoal() { return goal; }
                public boolean hasSubGoals() { return true; }
                public boolean check(Unit u, PathNode path) {
                    Tile tile = path.getTile();
                    if (tile.getIndianSettlement() != null) {
                        goal = path;
                        return true;
                    }
                    return false;
                }
            };

        GoalDecider ownedGD = new GoalDecider() {
                private PathNode goal = null;
                public PathNode getGoal() { return goal; }
                public boolean hasSubGoals() { return true; }
                public boolean check(Unit u, PathNode path) {
                    Tile tile = path.getTile();
                    if (tile.getOwner() == dutch) {
                        goal = path;
                        return true;
                    }
                    return false;
                }
            };

        gd = GoalDeciders.getComposedGoalDecider(true, ownedGD, settlementGD);
        path = unit.search(unitTile, gd, null, 1, null);
        assertNotNull(path);
        assertEquals("Composed-AND GoalDecider should find colony", colonyTile,
                     path.getLastNode().getTile());
        gd = GoalDeciders.getComposedGoalDecider(true, settlementGD, ownedGD);
        path = unit.search(unitTile, gd, null, 1, null);
        assertNotNull(path);
        assertEquals("Composed-AND GoalDecider should still find colony", colonyTile,
                     path.getLastNode().getTile());

        gd = GoalDeciders.getComposedGoalDecider(false, nativeGD, colonyGD);
        path = unit.search(unitTile, gd, null, 1, null);
        assertNotNull(path);
        assertEquals("Composed-OR GoalDecider should find natives", nativeTile,
                     path.getLastNode().getTile());
        gd = GoalDeciders.getComposedGoalDecider(false, nativeGD, colonyGD);
        path = unit.search(unitTile, gd, null, 1, null);
        assertNotNull(path);
        assertEquals("Composed-OR GoalDecider should find colony", colonyTile,
                     path.getLastNode().getTile());
    }
}
