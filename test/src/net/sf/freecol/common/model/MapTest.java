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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.pathfinding.CostDecider;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.util.test.MockModelController;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.FreeColTestUtils;


public class MapTest extends FreeColTestCase {
    TileType oceanType = spec().getTileType("model.tile.ocean");
    TileType plainsType = spec().getTileType("model.tile.plains");

    UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
    
    private Map getSingleLandPathMap(Game game){
        MapBuilder builder = new MapBuilder(game);
        builder.setBaseTileType(oceanType);
        // Land Stripe
        builder.setTile(1,11,plainsType);
        builder.setTile(2,10,plainsType);
        builder.setTile(2,9,plainsType); 
        builder.setTile(3,8,plainsType);
        builder.setTile(3,7,plainsType);
       
        return builder.build();
    }
    
    // (1,5)*
    //          *            
    //      *        *     * F(3,7)
    //                  * C(3,8)
    //      *        *
    //            
    //      *   *
    //      
    //      *S(1,11)
    //
    private Map getShortLongPathMap(Game game){
        TileType oceanType = spec().getTileType("model.tile.ocean");
        TileType plainsType = spec().getTileType("model.tile.plains");
        
        MapBuilder builder = new MapBuilder(game);
        builder.setBaseTileType(oceanType);
        //Start
        builder.setTile(1,11,plainsType);
        //Short path
        builder.setTile(2,10,plainsType);
        builder.setTile(2,9,plainsType);
        //Longer path
        builder.setTile(1,9,plainsType);
        builder.setTile(1,7,plainsType);
        builder.setTile(1,5,plainsType);
        builder.setTile(2,6,plainsType);
        builder.setTile(2,7,plainsType);
        // Common
        builder.setTile(3,8,plainsType);
        // Finish
        builder.setTile(3,7,plainsType);
       
        return builder.build();
    }

    public void testMapGameInt() throws FreeColException {
        int expectedWidth = 20;
        int expectedHeigth = 15;
        
        Game game = getStandardGame();
        MapBuilder builder = new MapBuilder(game);
        Map map = builder.setDimensions(expectedWidth, expectedHeigth).build();

        assertEquals(expectedWidth, map.getWidth());
        assertEquals(expectedHeigth, map.getHeight());
    }

    public void testGetSurroundingTiles() {
        Game game = getStandardGame();

        MapBuilder builder = new MapBuilder(game);
        Map map = builder.setDimensions(10, 15).build();
        game.setMap(map);

        // Check in the middle
        List<Tile> surroundingTiles = map.getTile(4,8).getSurroundingTiles(1);

        assertEquals(8, surroundingTiles.size());
        assertTrue(surroundingTiles.contains(map.getTile(4, 6)));
        assertTrue(surroundingTiles.contains(map.getTile(4, 10)));
        assertTrue(surroundingTiles.contains(map.getTile(3, 8)));
        assertTrue(surroundingTiles.contains(map.getTile(5, 8)));
        assertTrue(surroundingTiles.contains(map.getTile(3, 7)));
        assertTrue(surroundingTiles.contains(map.getTile(4, 7)));
        assertTrue(surroundingTiles.contains(map.getTile(3, 9)));
        assertTrue(surroundingTiles.contains(map.getTile(4, 9)));

        // Check on sides
        surroundingTiles = map.getTile(0, 0).getSurroundingTiles(1);

        assertEquals(3, surroundingTiles.size());
        assertTrue(surroundingTiles.contains(map.getTile(0, 2)));
        assertTrue(surroundingTiles.contains(map.getTile(1, 0)));
        assertTrue(surroundingTiles.contains(map.getTile(0, 1)));

        // Check larger range
        surroundingTiles = map.getTile(4,8).getSurroundingTiles(2);

        assertEquals(25 - 1, surroundingTiles.size());

        // Check that all tiles are returned
        surroundingTiles = map.getTile(4,8).getSurroundingTiles(10);
        assertEquals(150 - 1, surroundingTiles.size());
    }

    public void testGetReverseDirection() {
        assertEquals(Direction.S, Direction.N.getReverseDirection());
        assertEquals(Direction.N, Direction.S.getReverseDirection());
        assertEquals(Direction.E, Direction.W.getReverseDirection());
        assertEquals(Direction.W, Direction.E.getReverseDirection());
        assertEquals(Direction.NE, Direction.SW.getReverseDirection());
        assertEquals(Direction.NW, Direction.SE.getReverseDirection());
        assertEquals(Direction.SW, Direction.NE.getReverseDirection());
        assertEquals(Direction.SE, Direction.NW.getReverseDirection());
    }

    public void testGetWholeMapIterator() {

        Game game = getStandardGame();

        Tile[][] tiles = new Tile[5][6];

        Set<Position> positions = new HashSet<Position>();
        Set<Tile> allTiles = new HashSet<Tile>();
        
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 6; y++) {
                Tile tile = new Tile(game, spec().getTileType("model.tile.plains"), x, y);
                tiles[x][y] = tile;
                allTiles.add(tile);
                positions.add(new Position(x, y));
            }
        }

        Map map = new Map(game, tiles);

        Iterator<Position> wholeMapIterator = map.getWholeMapIterator();
        for (int i = 0; i < 30; i++) {
            assertTrue(wholeMapIterator.hasNext());
            assertTrue(positions.remove(wholeMapIterator.next()));
        }
        assertEquals(0, positions.size());
        assertFalse(wholeMapIterator.hasNext());

        // Check for-Iterator
        for (Tile t : map.getAllTiles()){
            assertTrue(allTiles.remove(t));
        }
        assertEquals(0, positions.size());
    }

    public void testGetAdjacent() {
        Game game = getStandardGame();

        MapBuilder builder = new MapBuilder(game);
        Map map = builder.setDimensions(10, 15).build();

        { // Even case
            Iterator<Position> i = map.getAdjacentIterator(map.getTile(4, 8).getPosition());

            List<Position> shouldBe = new ArrayList<Position>();
            shouldBe.add(new Position(4, 6));
            shouldBe.add(new Position(4, 10));
            shouldBe.add(new Position(3, 8));
            shouldBe.add(new Position(5, 8));

            shouldBe.add(new Position(4, 7));
            shouldBe.add(new Position(4, 9));
            shouldBe.add(new Position(3, 7));
            shouldBe.add(new Position(3, 9));

            for (int j = 0; j < 8; j++) {
                assertTrue(i.hasNext());
                Position p = i.next();
                assertTrue("" + p.getX() + ", " + p.getY(), shouldBe.contains(p));
            }
            assertFalse(i.hasNext());
        }
        { // Even case 2

            Iterator<Position> i = map.getAdjacentIterator(map.getTile(5, 8).getPosition());

            List<Position> shouldBe = new ArrayList<Position>();
            shouldBe.add(new Position(5, 6));
            shouldBe.add(new Position(5, 10));
            shouldBe.add(new Position(4, 8));
            shouldBe.add(new Position(6, 8));

            shouldBe.add(new Position(4, 7));
            shouldBe.add(new Position(5, 7));
            shouldBe.add(new Position(4, 9));
            shouldBe.add(new Position(5, 9));

            for (int j = 0; j < 8; j++) {
                assertTrue(i.hasNext());
                Position p = i.next();
                assertTrue("" + p.getX() + ", " + p.getY(), shouldBe.contains(p));
            }
            assertFalse(i.hasNext());
        }
        { // Odd case

            Iterator<Position> i = map.getAdjacentIterator(map.getTile(4, 7).getPosition());

            List<Position> shouldBe = new ArrayList<Position>();
            shouldBe.add(new Position(4, 5));
            shouldBe.add(new Position(4, 9));
            shouldBe.add(new Position(3, 7));
            shouldBe.add(new Position(5, 7));

            shouldBe.add(new Position(4, 6));
            shouldBe.add(new Position(5, 6));
            shouldBe.add(new Position(4, 8));
            shouldBe.add(new Position(5, 8));

            for (int j = 0; j < 8; j++) {
                assertTrue(i.hasNext());
                Position p = i.next();
                assertTrue("" + p.getX() + ", " + p.getY(), shouldBe.contains(p));
            }
            assertFalse(i.hasNext());
        }
    }

    public void testRandomDirection() {
        Game game = getStandardGame();
        MapBuilder builder = new MapBuilder(game);
        Map map = builder.setDimensions(10, 15).build();
        Direction[] dirs = Direction.getRandomDirectionArray(((MockModelController) game.getModelController()).getPseudoRandom());
        assertNotNull(dirs);
    }
    
    /**
     * Tests path discoverability in a map with only one path available
     * That path is obstructed by a settlement, so is invalid
     */
    public void testNoPathAvailableDueToCampInTheWay() {
        Game game = getStandardGame();
        Map map = getSingleLandPathMap(game);
        game.setMap(map);
        
        // set obstructing indian camp
        Tile settlementTile = map.getTile(2,10);
        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        builder.settlementTile(settlementTile).build();

        // set unit
        Player dutchPlayer = game.getPlayer("model.nation.dutch");
        Tile unitTile = map.getTile(1, 11);
        Tile destinationTile = map.getTile(3,7);
        Unit colonist = new Unit(game, unitTile, dutchPlayer, colonistType, UnitState.ACTIVE);
        colonist.setDestination(destinationTile);
        
        PathNode path = map.findPath(colonist, colonist.getTile(), destinationTile);
        assertNull("No path should be available",path);
    }
    
    /**
     * Tests path discoverability in a map with only one path available
     * That path is obstructed by a settlement, so is invalid
     */
    public void testNoPathAvailableDueToColonyInTheWay() {
        Game game = getStandardGame();
        Map map = getSingleLandPathMap(game);
        game.setMap(map);
        
        // set obstructing french colony
        Player frenchPlayer = game.getPlayer("model.nation.french");
        Tile settlementTile = map.getTile(2,10);
        FreeColTestUtils.getColonyBuilder().player(frenchPlayer)
                                                                 .colonyTile(settlementTile)
                                                                 .build();
        assertTrue("French colony was not set properly on the map",settlementTile.getSettlement() != null);
        // set unit
        Player dutchPlayer = game.getPlayer("model.nation.dutch");
        Tile unitTile = map.getTile(1, 11);
        Tile destinationTile = map.getTile(3,7);
        Unit colonist = new Unit(game, unitTile, dutchPlayer, colonistType, UnitState.ACTIVE);
        colonist.setDestination(destinationTile);
        
        PathNode path = map.findPath(colonist, colonist.getTile(), destinationTile);
        assertNull("No path should be available",path);
    }
    
    public void testMoveThroughTileWithEnemyUnit() {
        final UnitType pioneerType = spec().getUnitType("model.unit.hardyPioneer");
        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);
        
        //Setup
        Tile enemyUnitTile = map.getTile(2,1);
        Player frenchPlayer = game.getPlayer("model.nation.french");
        new Unit(game, enemyUnitTile, frenchPlayer, pioneerType, UnitState.ACTIVE);
        
        Tile unitTile = map.getTile(1, 1);
        Player dutchPlayer = game.getPlayer("model.nation.dutch");
        Unit unit = new Unit(game, unitTile, dutchPlayer, pioneerType, UnitState.ACTIVE);
        // unit is going somewhere else
        Tile unitDestination = map.getTile(3, 1);
        unit.setDestination(unitDestination);
        
        // Execute
        final CostDecider decider = CostDeciders.avoidSettlementsAndBlockingUnits();
        final int cost = decider.getCost(unit, unitTile, enemyUnitTile, 4, 0);
        assertTrue("Move should be invalid", cost == CostDecider.ILLEGAL_MOVE);
        final int cost2 = decider.getCost(unit, unitTile, enemyUnitTile, 4, 1);
        assertTrue("Move should be valid", cost2 != CostDecider.ILLEGAL_MOVE);
    }

    /**
     * Tests path discoverability in a map with only one path available
     * That path is obstructed by a settlement, so is invalid
     */
    public void testNoPathAvailableDueToUnitInTheWay() {
        Game game = getStandardGame();
        Map map = getSingleLandPathMap(game);
        game.setMap(map);
        
        // set obstructing unit
        Tile unitObstructionTile = map.getTile(2,10);
        Player frenchPlayer = game.getPlayer("model.nation.french");
        new Unit(game, unitObstructionTile, frenchPlayer, colonistType, UnitState.ACTIVE);
        
        // set unit
        Player dutchPlayer = game.getPlayer("model.nation.dutch");
        Tile unitTile = map.getTile(1, 11);
        Tile destinationTile = map.getTile(3,7);
        Unit colonist = new Unit(game, unitTile, dutchPlayer, colonistType, UnitState.ACTIVE);
        colonist.setDestination(destinationTile);
        
        PathNode path = map.findPath(colonist, colonist.getTile(), destinationTile, CostDeciders.avoidSettlementsAndBlockingUnits());
        assertNull("No path should be available",path);
    }
    
    public void testShortestPathObstructed() {        
        Game game = getStandardGame();
        Map map = getShortLongPathMap(getGame());
        game.setMap(map);

        // set obstructing indian camp
        Tile settlementTile = map.getTile(2, 10);
        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        builder.settlementTile(settlementTile).build();

        // set unit
        Player dutchPlayer = game.getPlayer("model.nation.dutch");
        Tile unitTile = map.getTile(1, 11);
        Unit colonist = new Unit(game, unitTile, dutchPlayer, colonistType, UnitState.ACTIVE);
        Tile destinationTile = map.getTile(3,7);
        colonist.setDestination(destinationTile);
        
        PathNode path = map.findPath(colonist, colonist.getTile(), destinationTile);
        assertNotNull("A path should be available",path);
    }

    public void testSearchForColony() {
        Game game = getStandardGame();
        Map map = getCoastTestMap(spec().getTileType("model.tile.plains"), true);
        game.setMap(map);

        Player dutchPlayer = game.getPlayer("model.nation.dutch");
        Player frenchPlayer = game.getPlayer("model.nation.french");
        UnitType artilleryType = spec().getUnitType("model.unit.artillery");
        UnitType galleonType = spec().getUnitType("model.unit.galleon");
        Tile unitTile = map.getTile(15, 5);
        Tile colonyTile = map.getTile(9, 9); // should be on coast
        Unit galleon = new Unit(game, unitTile, dutchPlayer, galleonType,
                                UnitState.ACTIVE);
        Unit artillery = new Unit(game, galleon, dutchPlayer, artilleryType, UnitState.ACTIVE);
        FreeColTestUtils.getColonyBuilder()
            .player(frenchPlayer)
            .colonyTile(colonyTile)
            .build();
        assertTrue("French colony not on the map",
                   colonyTile.getSettlement() != null);
        dutchPlayer.setStance(frenchPlayer, Stance.WAR);
        frenchPlayer.setStance(dutchPlayer, Stance.WAR);

        // Test a GoalDecider with subgoals.
        // The scoring function is deliberately simple.
        GoalDecider gd = new GoalDecider() {
                private PathNode found = null;
                private int score = -1;

                private int scoreTile(Tile tile) {
                    return tile.getX() + tile.getY();
                }

                public PathNode getGoal() {
                    return found;
                }

                public boolean hasSubGoals() {
                    return true;
                }

                public boolean check(Unit u, PathNode pathNode) {
                    Tile newTile = pathNode.getTile();
                    boolean result = newTile.getSettlement() != null;
                    if (result) {
                        if (scoreTile(newTile) > score) {
                            score = scoreTile(newTile);
                            found = pathNode;
                        }
                    }
                    return result;
                }
            };

        PathNode path = map.search(artillery, unitTile,
                                   gd, CostDeciders.avoidIllegal(),
                                   Integer.MAX_VALUE, galleon);
        assertTrue("Should find the French colony via a drop off",
                   path != null && path.getTransportDropNode() != null
                   && path.getLastNode().getTile() == colonyTile);

        // Add another colony
        Tile colonyTile2 = map.getTile(5, 5); // should score less
        FreeColTestUtils.getColonyBuilder()
            .player(frenchPlayer)
            .colonyTile(colonyTile2)
            .build();
        assertTrue("French colony not on the map",
                   colonyTile2.getSettlement() != null);
        path = map.search(artillery, unitTile,
                          gd, CostDeciders.avoidIllegal(),
                          Integer.MAX_VALUE, galleon);
        assertTrue("Should still find the first French colony via a drop off",
                   path != null && path.getTransportDropNode() != null
                   && path.getLastNode().getTile() == colonyTile);
    }
}
