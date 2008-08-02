/**
 *  Copyright (C) 2002-2008  The FreeCol Team
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

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.Specification;
import net.sf.freecol.util.test.FreeColTestCase;

/**
 * Tests for the {@link DefaultCostDecider} class.
 */
public class DefaultCostDeciderTest extends FreeColTestCase {

    private TileType plains;
    private Game game;

    @Override
    public void setUp() {
        game = getStandardGame();
        Specification s = FreeCol.getSpecification();
        plains = s.getTileType("model.tile.plains");
        Map map = getTestMap(plains);
        game.setMap(map);
    }

    @Override
    public void tearDown() {
        game = null;
        plains = null;
    }

    /**
     * Checks that the decider returns the right cost for a plain to plain move.
     */
    public void testGetCostLandLand() {
        DefaultCostDecider decider = new DefaultCostDecider();
        Tile start = game.getMap().getTile(5, 5);
        Unit unit = new Unit(game, start, game.getCurrentPlayer(), spec().getUnitType(
                "model.unit.hardyPioneer"),
                Unit.UnitState.ACTIVE);
        for (Map.Direction dir : Map.Direction.values()) {
            Tile end = game.getMap().getNeighbourOrNull(dir, start);
            assertNotNull(end);
            int cost = decider.getCost(unit, start, game.getMap().getTile(5, 6), 100, 0);
            assertEquals(plains.getBasicMoveCost(), cost);
        }
    }

    /**
     * Checks that {@link  DefaultCostDecider#getMovesLeft() } and
     * {@link  DefaultCostDecider#isNewTurn() } return the expected values after a move.
     */
    public void testGetRemainingMovesAndNewTurn() {
        DefaultCostDecider decider = new DefaultCostDecider();
        Unit unit = new Unit(game, game.getMap().getTile(1, 1), game.getCurrentPlayer(),
                spec().getUnitType("model.unit.hardyPioneer"),
                Unit.UnitState.ACTIVE);
        int cost = decider.getCost(unit, game.getMap().getTile(1, 1), game.getMap().getTile(2, 2), 4,
                4);
        assertEquals(plains.getBasicMoveCost(), cost);
        assertEquals(4 - plains.getBasicMoveCost(), decider.getMovesLeft());
        assertFalse(decider.isNewTurn());
    }
}
