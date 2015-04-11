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

import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.model.pathfinding.CostDecider;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * Tests for the {@link BaseCostDecider} class.
 */
public class BaseCostDeciderTest extends FreeColTestCase {

    private static final GoodsType tradeGoodsType
        = spec().getGoodsType("model.goods.tradeGoods");

    private static final TileType plainsType
        = spec().getTileType("model.tile.plains");

    private static final UnitType colonistType
        = spec().getUnitType("model.unit.freeColonist");
    private static final UnitType galleonType
        = spec().getUnitType("model.unit.galleon");
    private static final UnitType pioneerType
        = spec().getUnitType("model.unit.hardyPioneer");
    
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
        Unit unit = new ServerUnit(game, start, game.getCurrentPlayer(),
                                   pioneerType);
        for (Direction dir : Direction.values()) {
            Tile end = start.getNeighbourOrNull(dir);
            assertNotNull(end);
            int cost = decider.getCost(unit, start, game.getMap().getTile(5, 6),
                                       100);
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
        Unit unit = new ServerUnit(game, game.getMap().getTile(1, 1),
                                   game.getCurrentPlayer(), pioneerType);
        int cost = decider.getCost(unit, game.getMap().getTile(1, 1),
                                   game.getMap().getTile(2, 2), 4);
        assertEquals(plainsType.getBasicMoveCost(), cost);
        assertEquals(4 - plainsType.getBasicMoveCost(), decider.getMovesLeft());
        assertEquals(0, decider.getNewTurns());
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
        Unit unit = new ServerUnit(game, unitTile, game.getCurrentPlayer(), pioneerType);
        
        Tile seaTile = map.getTile(10, 9);
        assertFalse("Tile should be ocean",seaTile.isLand());
        
        // Execute
        CostDecider decider = CostDeciders.avoidSettlements();
        int cost = decider.getCost(unit, unitTile, seaTile, 4);
        assertTrue("Move should be invalid", cost == CostDecider.ILLEGAL_MOVE);
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
        assertFalse("Unit tile should be ocean", unitTile.isLand());

        Unit unit = new ServerUnit(game, unitTile, game.getCurrentPlayer(),
                                   galleonType);
        
        Tile landTile = map.getTile(9, 9);
        assertTrue("Tile should be land", landTile.isLand());
        
        // Execute
        final CostDecider decider = CostDeciders.avoidSettlements();
        int cost = decider.getCost(unit, unitTile, landTile, 4);
        assertTrue("Move should be invalid", cost == CostDecider.ILLEGAL_MOVE);
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
        Settlement settlement = builder.settlementTile(settlementTile).build();
        settlementTile.setSettlement(settlement);
        settlementTile.changeOwningSettlement(settlement);

        Tile unitTile = map.getTile(1, 1);
        Unit unit = new ServerUnit(game, unitTile, game.getCurrentPlayer(), pioneerType);
        // unit is going somewhere else
        Tile unitDestination = map.getTile(3, 1);
        unit.setDestination(unitDestination);
        
        // Execute
        final CostDecider decider = CostDeciders.avoidSettlements();
        int cost = decider.getCost(unit, unitTile, settlementTile, 4);

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

        Unit galleon = new ServerUnit(game, unitTile, game.getCurrentPlayer(),
                                      galleonType);
        
        Tile settlementTile = map.getTile(9, 9);
        assertTrue("Tile should be land", settlementTile.isLand());
        
        FreeColTestCase.IndianSettlementBuilder builder
            = new FreeColTestCase.IndianSettlementBuilder(game);
        Settlement settlement = builder.settlementTile(settlementTile).build();

        // galleon is trying go to settlement
        galleon.setDestination(settlementTile);
        
        CostDecider base = CostDeciders.avoidIllegal();
        int cost;

        // Try to find a path
        cost = base.getCost(galleon, unitTile, settlementTile, 4);
        assertEquals("Move should be invalid, no contact or goods to trade",
                     CostDecider.ILLEGAL_MOVE, cost);

        // Add contact, but disallow empty traders
        Player.makeContact(galleon.getOwner(), settlement.getOwner());
        ((BooleanOption)spec().getOption(GameOptions.EMPTY_TRADERS))
            .setValue(Boolean.FALSE);
        cost = base.getCost(galleon, unitTile, settlementTile, 4);
        assertEquals("Move should be invalid, no goods to trade",
                     CostDecider.ILLEGAL_MOVE, cost);

        // Test empty traders
        ((BooleanOption)spec().getOption(GameOptions.EMPTY_TRADERS))
            .setValue(Boolean.TRUE);
        cost = base.getCost(galleon, unitTile, settlementTile, 4);
        assertTrue("Move should be valid, no goods to trade",
                   cost != CostDecider.ILLEGAL_MOVE);
        ((BooleanOption)spec().getOption(GameOptions.EMPTY_TRADERS))
            .setValue(Boolean.FALSE);

        // Add goods to trade
        Goods goods = new Goods(game, null, tradeGoodsType, 50);
        galleon.add(goods);
        cost = base.getCost(galleon, unitTile, settlementTile, 4);
        assertTrue("Move should be valid, has contact and goods to trade",
                   cost != CostDecider.ILLEGAL_MOVE);
        assertTrue("Move should consume whole turn",
                   base.getMovesLeft() == 0 && base.getNewTurns() == 0);

        // Try with colonist on galleon
        Unit colonist = new ServerUnit(game, galleon, game.getCurrentPlayer(),
                                       colonistType);
        cost = base.getCost(colonist, unitTile, settlementTile, 4);
        if (spec().getBoolean(GameOptions.AMPHIBIOUS_MOVES)) {
            assertFalse("Move valid, direct from carrier to settlement",
                        cost == CostDecider.ILLEGAL_MOVE);
        } else {
            assertTrue("Move invalid, direct from carrier to settlement",
                       cost == CostDecider.ILLEGAL_MOVE);
        }
        assertNotNull("Path should be valid from carrier to settlement",
                      map.findPath(colonist, unitTile, settlementTile,
                                   galleon, base, null));

        // Set players at war
        Player indianPlayer = settlement.getOwner();
        indianPlayer.setStance(galleon.getOwner(), Stance.WAR);
        galleon.getOwner().setStance(indianPlayer, Stance.WAR);
        cost = base.getCost(galleon, unitTile, settlementTile, 4);
        assertTrue("Move should be valid, war should not block gifts",
                   cost != CostDecider.ILLEGAL_MOVE);
    }
}
