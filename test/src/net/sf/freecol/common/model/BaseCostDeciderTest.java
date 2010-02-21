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

import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.pathfinding.CostDecider;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.util.test.FreeColTestCase;

/**
 * Tests for the {@link BaseCostDecider} class.
 */
public class BaseCostDeciderTest extends FreeColTestCase {
    private UnitType pioneerType = spec().getUnitType("model.unit.hardyPioneer");
    private UnitType galleonType = spec().getUnitType("model.unit.galleon");
    private GoodsType tradeGoodsType = spec().getGoodsType("model.goods.tradeGoods");
    
    private Game game;

    @Override
    public void setUp() {
        game = getStandardGame();
    }

    @Override
    public void tearDown() {
        game = null;
    }

    /**
     * Checks that the decider returns the right cost for a plain to plain move.
     */
    public void testGetCostLandLand() {
        Map map = getTestMap(plainsType);
        game.setMap(map);
    	
        final CostDecider decider = CostDeciders.avoidSettlements();
        Tile start = game.getMap().getTile(5, 5);
        Unit unit = new Unit(game, start, game.getCurrentPlayer(), spec().getUnitType(
                "model.unit.hardyPioneer"),
                Unit.UnitState.ACTIVE);
        for (Map.Direction dir : Map.Direction.values()) {
            Tile end = game.getMap().getNeighbourOrNull(dir, start);
            assertNotNull(end);
            int cost = decider.getCost(unit, start, game.getMap().getTile(5, 6), 100, 0);
            assertEquals(plainsType.getBasicMoveCost(), cost);
        }
    }

    /**
     * Checks that {@link BaseCostDecider#getMovesLeft() } and {@link
     * BaseCostDecider#isNewTurn() } return the expected values after
     * a move.
     */
    public void testGetRemainingMovesAndNewTurn() {
        Map map = getTestMap(plainsType);
        game.setMap(map);
        
        final CostDecider decider = CostDeciders.avoidSettlements();
        Unit unit = new Unit(game, game.getMap().getTile(1, 1), game.getCurrentPlayer(),
                spec().getUnitType("model.unit.hardyPioneer"),
                UnitState.ACTIVE);
        int cost = decider.getCost(unit, game.getMap().getTile(1, 1), game.getMap().getTile(2, 2), 4,
                4);
        assertEquals(plainsType.getBasicMoveCost(), cost);
        assertEquals(4 - plainsType.getBasicMoveCost(), decider.getMovesLeft());
        assertFalse(decider.isNewTurn());
    }
    
    /**
     * Checks possible move of a land unit to an ocean tile
     * Verifies that is invalid
     */
    public void testInvalidMoveOfLandUnitToAnOceanTile() {
        // For this test we need a different map
        Map map = getCoastTestMap(plainsType);
        game.setMap(map);
        
        Tile unitTile = map.getTile(9, 9);
        assertTrue("Unit tile should be land",unitTile.isLand());
        Unit unit = new Unit(game, unitTile, game.getCurrentPlayer(), pioneerType, UnitState.ACTIVE);
        
        Tile seaTile = map.getTile(10, 9);
        assertFalse("Tile should be ocean",seaTile.isLand());
        
        // Execute
        CostDecider decider = CostDeciders.avoidSettlements();
        int cost = decider.getCost(unit, unitTile, seaTile, 4,4);
        assertTrue("Move should be invalid",cost == CostDecider.ILLEGAL_MOVE);
    }
    
    /**
     * Checks possible move of a naval unit to a land tile without settlement
     * Verifies that is invalid
     */
    public void testInvalidMoveOfNavalUnitToALandTile() {
        // For this test we need a different map
        Map map = getCoastTestMap(plainsType);
        game.setMap(map);
        
        Tile unitTile = map.getTile(10, 9);
        assertFalse("Unit tile should be ocean",unitTile.isLand());

        Unit unit = new Unit(game, unitTile, game.getCurrentPlayer(), galleonType, UnitState.ACTIVE);
        
        Tile landTile = map.getTile(9, 9);
        assertTrue("Tile should be land",landTile.isLand());        
        
        // Execute
        final CostDecider decider = CostDeciders.avoidSettlements();
        int cost = decider.getCost(unit, unitTile, landTile, 4,4);
        assertTrue("Move should be invalid",cost == CostDecider.ILLEGAL_MOVE);
    }
    
    /**
     * Checks possible move of a unit through a tile with a settlement
     * Verifies that is invalid
     */
    public void testInvalidMoveThroughTileWithSettlement() {
        Map map = getTestMap(plainsType);
        game.setMap(map);

        //Setup
        Tile settlementTile = map.getTile(2,1);
        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        settlementTile.setSettlement(builder.settlementTile(settlementTile).build());

        Tile unitTile = map.getTile(1, 1);
        Unit unit = new Unit(game, unitTile, game.getCurrentPlayer(), pioneerType, UnitState.ACTIVE);
        // unit is going somewhere else
        Tile unitDestination = map.getTile(3, 1);
        unit.setDestination(unitDestination);
        
        // Execute
        final CostDecider decider = CostDeciders.avoidSettlements();
        int cost = decider.getCost(unit, unitTile, settlementTile, 4,4);

        assertEquals("Move should be invalid", CostDecider.ILLEGAL_MOVE, cost);
    }
        
    /**
     * Checks possible move of a naval unit to a tile with a settlement
     */
    public void testNavalUnitMoveToTileWithSettlement() {
        // For this test we need a different map
        Map map = getCoastTestMap(plainsType);
        game.setMap(map);
        
        Tile unitTile = map.getTile(10, 9);
        assertFalse("Unit tile should be ocean",unitTile.isLand());

        Unit unit = new Unit(game, unitTile, game.getCurrentPlayer(), galleonType, UnitState.ACTIVE);
        
        Tile settlementTile = map.getTile(9, 9);
        assertTrue("Tile should be land",settlementTile.isLand());
        
        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        Settlement settlement = builder.settlementTile(settlementTile).build();

        // unit is trying go to settlement
        unit.setDestination(settlementTile);
        
        // Try to find a path
        assertNull("Move should be invalid, no contact or goods to trade",
                   map.findPath(unit, unitTile, settlementTile));

        // Add contact
        unit.getOwner().setContacted(settlement.getOwner());
        settlement.getOwner().setContacted(unit.getOwner());
        assertNull("Move should be invalid, no goods to trade",
                   map.findPath(unit, unitTile, settlementTile));

        // Add goods to trade
        Goods goods = new Goods(game, null, tradeGoodsType, 50);
        unit.add(goods);
        assertNotNull("Move should be valid, has contact and goods to trade",
                      map.findPath(unit, unitTile, settlementTile));

        // Set players at war
        Player indianPlayer = settlement.getOwner();
        indianPlayer.changeRelationWithPlayer(unit.getOwner(), Stance.WAR);
        assertNull("Move should be invalid, players at war",
                   map.findPath(unit, unitTile, settlementTile));
    }
}
