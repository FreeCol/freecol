/**
 *  Copyright (C) 2002-2012  The FreeCol Team
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
import java.util.Random;
import java.util.Set;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.pathfinding.CostDecider;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.FreeColTestUtils;


public class MapTest extends FreeColTestCase {

    private final TileType highSeasType
        = spec().getTileType("model.tile.highSeas");
    private final TileType lakeType
        = spec().getTileType("model.tile.lake");
    private final TileType oceanType
        = spec().getTileType("model.tile.ocean");
    private final TileType plainsType
        = spec().getTileType("model.tile.plains");

    private final UnitType artilleryType
        = spec().getUnitType("model.unit.artillery");
    private final UnitType colonistType
        = spec().getUnitType("model.unit.freeColonist");
    private final UnitType galleonType
        = spec().getUnitType("model.unit.galleon");
    private final UnitType pioneerType
        = spec().getUnitType("model.unit.hardyPioneer");


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
        List<Tile> surroundingTiles = new ArrayList<Tile>();
        for (Tile t: map.getTile(4,8).getSurroundingTiles(1))
            surroundingTiles.add(t);

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
        surroundingTiles = new ArrayList<Tile>();
        for (Tile t: map.getTile(0, 0).getSurroundingTiles(1))
            surroundingTiles.add(t);

        assertEquals(3, surroundingTiles.size());
        assertTrue(surroundingTiles.contains(map.getTile(0, 2)));
        assertTrue(surroundingTiles.contains(map.getTile(1, 0)));
        assertTrue(surroundingTiles.contains(map.getTile(0, 1)));

        // Check larger range
        surroundingTiles = new ArrayList<Tile>();
        for (Tile t: map.getTile(4, 8).getSurroundingTiles(2))
            surroundingTiles.add(t);

        assertEquals(25 - 1, surroundingTiles.size());

        // Check that all tiles are returned
        surroundingTiles = new ArrayList<Tile>();
        for (Tile t: map.getTile(4, 8).getSurroundingTiles(10))
            surroundingTiles.add(t);

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
                Tile tile = new Tile(game, plainsType, x, y);
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
        builder.setDimensions(10, 15).build();
        Direction[] dirs = Direction.getRandomDirections("testRandomDirection",
            new Random(1));
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
        FreeColTestCase.IndianSettlementBuilder builder
            = new FreeColTestCase.IndianSettlementBuilder(game);
        builder.settlementTile(settlementTile).build();

        // set unit
        Player dutchPlayer = game.getPlayer("model.nation.dutch");
        Tile unitTile = map.getTile(1, 11);
        Tile destinationTile = map.getTile(3,7);
        Unit colonist = new ServerUnit(game, unitTile, dutchPlayer,
                                       colonistType);
        colonist.setDestination(destinationTile);

        PathNode path = colonist.findPath(destinationTile);
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
            .colonyTile(settlementTile).build();
        assertTrue("French colony was not set properly on the map",
            settlementTile.getSettlement() != null);
        // set unit
        Player dutchPlayer = game.getPlayer("model.nation.dutch");
        Tile unitTile = map.getTile(1, 11);
        Tile destinationTile = map.getTile(3,7);
        Unit colonist = new ServerUnit(game, unitTile, dutchPlayer,
                                       colonistType);
        colonist.setDestination(destinationTile);

        PathNode path = colonist.findPath(destinationTile);
        assertNull("No path should be available",path);
    }

    public void testMoveThroughTileWithEnemyUnit() {
        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);

        //Setup
        Tile enemyUnitTile = map.getTile(2,1);
        Player frenchPlayer = game.getPlayer("model.nation.french");
        new ServerUnit(game, enemyUnitTile, frenchPlayer, pioneerType);

        Tile unitTile = map.getTile(1, 1);
        Tile otherTile = map.getTile(1, 2);
        Player dutchPlayer = game.getPlayer("model.nation.dutch");
        Unit unit = new ServerUnit(game, unitTile, dutchPlayer, pioneerType);
        // unit is going somewhere else
        Tile unitDestination = map.getTile(3, 1);
        unit.setDestination(unitDestination);

        // Execute
        CostDecider decider = CostDeciders.avoidSettlementsAndBlockingUnits();
        assertTrue("No blocking unit, should be legal",
                   decider.getCost(unit, unitTile, otherTile, 4)
                   != CostDecider.ILLEGAL_MOVE);
        assertTrue("Blocking unit, should be illegal",
                   decider.getCost(unit, unitTile, enemyUnitTile, 4)
                   == CostDecider.ILLEGAL_MOVE);
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
        new ServerUnit(game, unitObstructionTile, frenchPlayer, colonistType);

        // set unit
        Player dutchPlayer = game.getPlayer("model.nation.dutch");
        Tile unitTile = map.getTile(1, 11);
        Tile destinationTile = map.getTile(3,7);
        Unit colonist = new ServerUnit(game, unitTile, dutchPlayer,
                                       colonistType);
        colonist.setDestination(destinationTile);

        PathNode path = map.findPath(colonist, colonist.getTile(),
            destinationTile, null,
            CostDeciders.avoidSettlementsAndBlockingUnits());
        assertNull("No path should be available", path);
    }

    public void testShortestPathObstructed() {
        Game game = getStandardGame();
        Map map = getShortLongPathMap(getGame());
        game.setMap(map);

        // set obstructing indian camp
        Tile settlementTile = map.getTile(2, 10);
        FreeColTestCase.IndianSettlementBuilder builder
            = new FreeColTestCase.IndianSettlementBuilder(game);
        builder.settlementTile(settlementTile).build();

        // set unit
        Player dutchPlayer = game.getPlayer("model.nation.dutch");
        Tile unitTile = map.getTile(1, 11);
        Unit colonist = new ServerUnit(game, unitTile, dutchPlayer,
                                       colonistType);
        Tile destinationTile = map.getTile(3,7);
        colonist.setDestination(destinationTile);

        PathNode path = colonist.findPath(destinationTile);
        assertNotNull("A path should be available",path);
    }

    public void testSearchForColony() {
        Game game = getStandardGame();
        Map map = getCoastTestMap(plainsType, true);
        game.setMap(map);

        Player dutchPlayer = game.getPlayer("model.nation.dutch");
        Player frenchPlayer = game.getPlayer("model.nation.french");
        Tile unitTile = map.getTile(15, 5);
        Tile colonyTile = map.getTile(9, 9); // should be on coast
        Unit galleon = new ServerUnit(game, unitTile, dutchPlayer, galleonType);
        Unit artillery = new ServerUnit(game, galleon, dutchPlayer, artilleryType);
        FreeColTestUtils.getColonyBuilder().player(frenchPlayer)
            .colonyTile(colonyTile).build();
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
                    Settlement settlement = pathNode.getLocation().getSettlement();
                    if (settlement != null) {
                        int value = scoreTile(pathNode.getTile());
                        if (value > score) {
                            score = value;
                            found = pathNode;
                            return true;
                        }
                    }
                    return false;
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
        FreeColTestUtils.getColonyBuilder().player(frenchPlayer)
            .colonyTile(colonyTile2).build();
        assertTrue("French colony not on the map",
                   colonyTile2.getSettlement() != null);
        path = map.search(artillery, unitTile,
                          gd, CostDeciders.avoidIllegal(),
                          Integer.MAX_VALUE, galleon);
        assertTrue("Should still find the first French colony via a drop off",
                   path != null && path.getTransportDropNode() != null
                   && path.getLastNode().getTile() == colonyTile);
    }

    public void testLatitude() {
        Game game = getStandardGame();

        MapBuilder builder = new MapBuilder(game);
        Map map = builder.setDimensions(1, 181).build();

        assertEquals(181, map.getHeight());
        assertEquals(1f, map.getLatitudePerRow());
        assertEquals(-90, map.getLatitude(0));
        assertEquals(0, map.getRow(-90));
        assertEquals(0, map.getLatitude(90));
        assertEquals(90, map.getRow(0));
        assertEquals(90, map.getLatitude(180));
        assertEquals(180, map.getRow(90));

        builder = new MapBuilder(game);
        map = builder.setDimensions(1, 91).build();

        assertEquals(91, map.getHeight());
        assertEquals(2f, map.getLatitudePerRow());
        assertEquals(-90, map.getLatitude(0));
        assertEquals(0, map.getRow(-90));
        assertEquals(0, map.getLatitude(45));
        assertEquals(45, map.getRow(0));
        assertEquals(90, map.getLatitude(90));
        assertEquals(90, map.getRow(90));

        builder = new MapBuilder(game);
        map = builder.setDimensions(1, 91).build();
        map.setMinimumLatitude(0);

        assertEquals(91, map.getHeight());
        assertEquals(1f, map.getLatitudePerRow());
        assertEquals(0, map.getLatitude(0));
        assertEquals(0, map.getRow(0));
        assertEquals(45, map.getLatitude(45));
        assertEquals(45, map.getRow(45));
        assertEquals(90, map.getLatitude(90));
        assertEquals(90, map.getRow(90));
    }

    public void testFullPath() {
        Game game = getStandardGame();
        Map map = getCoastTestMap(plainsType, true);
        game.setMap(map);

        Player dutch = game.getPlayer("model.nation.dutch");
        Tile settlementTile = map.getTile(9, 2);
        FreeColTestUtils.getColonyBuilder().player(dutch)
            .colonyTile(settlementTile).build();
        assertTrue("Dutch colony should be on the map.",
            settlementTile.getSettlement() != null);
        assertTrue("Dutch colony should be on the shore.",
            settlementTile.isShore());

        // Check colonist can find the trivial path
        Unit colonist = new ServerUnit(game, settlementTile, dutch,
                                       colonistType);
        PathNode path = map.findFullPath(colonist, settlementTile,
                                         settlementTile, null, null);
        assertNotNull("Trivial path should exist.", path);
        assertNull("Trivial path should be trivial.", path.next);
        assertEquals("Trivial path should start at settlement.",
            settlementTile, path.getTile());

        // Check colonist can not find a path into the sea
        Tile seaTile = map.getTile(13, 2);
        path = map.findFullPath(colonist, settlementTile, seaTile,
                                null, null);
        assertNull("Sea path should be illegal.", path);

        // Check that a naval unit can find that path.
        Unit galleon = new ServerUnit(game, settlementTile, dutch,
                                      galleonType);
        path = map.findFullPath(galleon, settlementTile, seaTile,
                                null, null);
        assertNotNull("Sea path should be legal for naval unit.", path);
        assertEquals("Sea path should start at settlement.", settlementTile,
            path.getTile());
        assertEquals("Sea path should end at sea tile.", seaTile,
            path.getLastNode().getTile());

        // Check giving the colonist access to a carrier makes the sea
        // path work.
        path = map.findFullPath(colonist, settlementTile, seaTile,
                                galleon, null);
        assertNotNull("Sea path should now be legal.", path);
        assertEquals("Sea path should start at settlement.", settlementTile,
            path.getTile());
        assertEquals("Sea path should end at sea tile.", seaTile,
            path.getLastNode().getTile());

        // Check the path still works if the colonist has to walk to
        // the carrier.
        Tile landTile = map.getTile(2, 2);
        path = map.findFullPath(colonist, landTile, seaTile,
                                galleon, null);
        assertNotNull("Sea path should still be legal.", path);
        assertEquals("Sea path should start at land tile.", landTile,
            path.getTile());
        assertEquals("Sea path should end at sea tile.", seaTile,
            path.getLastNode().getTile());
        while (!path.isOnCarrier()) path = path.next;
        assertEquals("Sea path should include pickup at settlement.",
            settlementTile, path.getTile());

        // Check the colonist uses the carrier if it is quicker than walking.
        Tile shoreTile = map.getTile(9, 13);
        assertTrue("Shore tile should be on the shore.", shoreTile.isShore());
        path = map.findFullPath(colonist, settlementTile, shoreTile,
                                galleon, null);
        assertNotNull("Shore path should be legal.", path);
        assertTrue("Shore path should have carrier moves.",
            path.usesCarrier());
        assertNotNull("Shore path should have drop node.",
            path.getCarrierMove().getTransportDropNode());

        // Check the colonist does not use the carrier if it does not help.
        Tile midTile = map.getTile(9, 4);
        path = map.findFullPath(colonist, map.getTile(2, 6), midTile,
                                galleon, null);
        assertNotNull("Middle path should be legal.", path);
        assertFalse("Middle path should not not use carrier.",
            path.usesCarrier());

        // Check path to Europe.
        Europe europe = dutch.getEurope();
        path = map.findFullPath(colonist, settlementTile, europe,
                                galleon, null);
        assertNotNull("To-Europe path should be valid.", path);
        assertEquals("To-Europe path should end in Europe.", europe,
            path.getLastNode().getLocation());

        // Check path from Europe.
        path = map.findFullPath(colonist, europe, landTile,
                                galleon, null);
        assertNotNull("From-Europe path should be valid.", path);
        assertEquals("From-Europe path should start in Europe.", europe,
            path.getLocation());

        // Add another settlement on a lake connected to the existing
        // settlement and see if a ship at the new settlement can find
        // a path to Europe.
        Tile lakeTile = map.getTile(8, 2);
        lakeTile.setType(lakeType);
        Tile anotherSettlementTile = map.getTile(7, 2);
        FreeColTestUtils.getColonyBuilder().player(dutch)
            .colonyTile(anotherSettlementTile).build();
        path = map.findFullPath(galleon, anotherSettlementTile, europe,
                                null, null);
        assertNotNull("From-lake-settlement path should be valid.", path);
        assertEquals("From-lake-settlement path should end in Europe.", europe,
            path.getLastNode().getLocation());

        // Put the unit on the carrier, put the carrier out to sea, and
        // find a path inland.
        colonist.setLocation(galleon);
        galleon.setLocation(seaTile);
        path = map.findFullPath(colonist, galleon, landTile, galleon, null);
        assertNotNull("From-galleon path should be valid.", path);
        assertEquals("From-galleon path should start at sea.", seaTile,
            path.getLocation());
        assertTrue("From-galleon path should start on carrier.",
            path.isOnCarrier());
        assertNotNull("From-galleon path should have a drop node.",
            path.getTransportDropNode());
    }
}
