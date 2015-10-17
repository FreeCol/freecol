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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Stance;
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


    private Map getSingleLandPathMap(Game game) {
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
    private Map getShortLongPathMap(Game game) {
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
        List<Tile> surroundingTiles = new ArrayList<>();
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
        surroundingTiles = new ArrayList<>();
        for (Tile t: map.getTile(0, 0).getSurroundingTiles(1))
            surroundingTiles.add(t);

        assertEquals(3, surroundingTiles.size());
        assertTrue(surroundingTiles.contains(map.getTile(0, 2)));
        assertTrue(surroundingTiles.contains(map.getTile(1, 0)));
        assertTrue(surroundingTiles.contains(map.getTile(0, 1)));

        // Check larger range
        surroundingTiles = new ArrayList<>();
        for (Tile t: map.getTile(4, 8).getSurroundingTiles(2))
            surroundingTiles.add(t);

        assertEquals(25 - 1, surroundingTiles.size());

        // Check that all tiles are returned
        surroundingTiles = new ArrayList<>();
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

    public void testGetAllTiles() {
        Game game = getStandardGame();
        final int xmax = 5;
        final int ymax = 6;
        Set<Tile> allTiles = new HashSet<Tile>();
        Map map = new Map(game, xmax, ymax);
        for (int x = 0; x < xmax; x++) {
            for (int y = 0; y < ymax; y++) {
                Tile tile = new Tile(game, plainsType, x, y);
                map.setTile(tile, x, y);
                allTiles.add(tile);
            }
        }
        
        int i = 0;
        for (Tile t : map.getAllTiles()) {
            i++;
            assertTrue(allTiles.remove(t));
        }
        assertTrue(allTiles.isEmpty());
        assertEquals(xmax * ymax, i);
    }

    public void testRandomDirection() {
        Game game = getStandardGame();
        MapBuilder builder = new MapBuilder(game);
        builder.setDimensions(10, 15).build();
        Direction[] dirs = Direction.getRandomDirections("testRandomDirection",
            null, new Random(1));
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
        Player dutchPlayer = game.getPlayerByNationId("model.nation.dutch");
        Tile unitTile = map.getTile(1, 11);
        Tile destinationTile = map.getTile(3,7);
        Unit colonist = new ServerUnit(game, unitTile, dutchPlayer,
                                       colonistType);
        colonist.setDestination(destinationTile);

        PathNode path = colonist.findPath(destinationTile);
        assertNull("No path should be available", path);
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
        Player frenchPlayer = game.getPlayerByNationId("model.nation.french");
        Tile settlementTile = map.getTile(2,10);
        FreeColTestUtils.getColonyBuilder().player(frenchPlayer)
            .colonyTile(settlementTile).build();
        assertTrue("French colony was not set properly on the map",
            settlementTile.hasSettlement());
        // set unit
        Player dutchPlayer = game.getPlayerByNationId("model.nation.dutch");
        Tile unitTile = map.getTile(1, 11);
        Tile destinationTile = map.getTile(3,7);
        Unit colonist = new ServerUnit(game, unitTile, dutchPlayer,
                                       colonistType);
        colonist.setDestination(destinationTile);

        PathNode path = colonist.findPath(destinationTile);
        assertNull("No path should be available", path);
    }

    public void testMoveThroughTileWithEnemyUnit() {
        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);

        //Setup
        Tile enemyUnitTile = map.getTile(2,1);
        Player frenchPlayer = game.getPlayerByNationId("model.nation.french");
        new ServerUnit(game, enemyUnitTile, frenchPlayer, pioneerType);

        Tile unitTile = map.getTile(1, 1);
        Tile otherTile = map.getTile(1, 2);
        Player dutchPlayer = game.getPlayerByNationId("model.nation.dutch");
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
        Player frenchPlayer = game.getPlayerByNationId("model.nation.french");
        new ServerUnit(game, unitObstructionTile, frenchPlayer, colonistType);

        // set unit
        Player dutchPlayer = game.getPlayerByNationId("model.nation.dutch");
        Tile unitTile = map.getTile(1, 11);
        Tile destinationTile = map.getTile(3,7);
        Unit colonist = new ServerUnit(game, unitTile, dutchPlayer,
                                       colonistType);
        colonist.setDestination(destinationTile);

        PathNode path = map.findPath(colonist, colonist.getTile(),
            destinationTile, null,
            CostDeciders.avoidSettlementsAndBlockingUnits(), null);
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
        Player dutchPlayer = game.getPlayerByNationId("model.nation.dutch");
        Tile unitTile = map.getTile(1, 11);
        Unit colonist = new ServerUnit(game, unitTile, dutchPlayer,
                                       colonistType);
        Tile destinationTile = map.getTile(3,7);
        colonist.setDestination(destinationTile);

        PathNode path = colonist.findPath(destinationTile);
        assertNotNull("A path should be available", path);
    }

    public void testSearchForColony() {
        Game game = getStandardGame();
        Map map = getCoastTestMap(plainsType, true);
        game.setMap(map);

        Player dutchPlayer = game.getPlayerByNationId("model.nation.dutch");
        Player frenchPlayer = game.getPlayerByNationId("model.nation.french");
        Tile unitTile = map.getTile(15, 5);
        Tile colonyTile = map.getTile(9, 9); // should be on coast
        Unit galleon = new ServerUnit(game, unitTile, dutchPlayer, galleonType);
        Unit artillery = new ServerUnit(game, galleon, dutchPlayer, artilleryType);
        FreeColTestUtils.getColonyBuilder().player(frenchPlayer)
            .colonyTile(colonyTile).build();
        assertTrue("French colony not on the map", colonyTile.hasSettlement());
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

        PathNode path = map.search(artillery, unitTile, gd,
                                   CostDeciders.avoidIllegal(),
                                   FreeColObject.INFINITY, galleon, null);
        assertTrue("Should find the French colony via a drop off",
                   path != null && path.getTransportDropNode() != null
                   && path.getLastNode().getTile() == colonyTile);

        // Add another colony
        Tile colonyTile2 = map.getTile(5, 5); // should score less
        FreeColTestUtils.getColonyBuilder().player(frenchPlayer)
            .colonyTile(colonyTile2).build();
        assertTrue("French colony not on the map",
                   colonyTile2.hasSettlement());
        path = map.search(artillery, unitTile, gd,
                          CostDeciders.avoidIllegal(),
                          FreeColObject.INFINITY, galleon, null);
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

    public void testFindPath() {
        Game game = getStandardGame();
        Map map = getCoastTestMap(plainsType, true);
        game.setMap(map);

        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Europe europe = dutch.getEurope();
        PathNode path;

        // Nearest port should be Europe
        Tile seaTile = map.getTile(13, 2);
        Unit galleon = new ServerUnit(game, seaTile, dutch,
                                      galleonType);
        path = galleon.findOurNearestPort();
        assertNotNull("Nearest port should exist.", path);
        assertEquals("Nearest port should be Europe.", europe,
                      path.getLastNode().getLocation());

        Tile settlementTile = map.getTile(9, 2);
        FreeColTestUtils.getColonyBuilder().player(dutch)
            .colonyTile(settlementTile).build();
        assertTrue("Dutch colony should be on the map.",
            settlementTile.hasSettlement());
        assertTrue("Dutch colony should be on the shore.",
            settlementTile.isShore());

        // Nearest port should now be the colony.
        path = galleon.findOurNearestPort();
        assertNotNull("Nearest port should exist.", path);
        assertEquals("Nearest port should be the colony.", settlementTile,
                     path.getLastNode().getTile());

        // Check colonist can find the trivial path
        Unit colonist = new ServerUnit(game, settlementTile, dutch,
                                       colonistType);
        path = map.findPath(colonist, settlementTile,
                            settlementTile, null, null, null);
        assertNotNull("Trivial path should exist.", path);
        assertNull("Trivial path should be trivial.", path.next);
        assertEquals("Trivial path should start at settlement.",
            settlementTile, path.getTile());

        // Check colonist can not find a path into the sea
        path = map.findPath(colonist, settlementTile, seaTile,
                            null, null, null);
        assertNull("Sea path should be illegal.", path);

        // Check that a naval unit can find that path.
        path = map.findPath(galleon, settlementTile, seaTile,
                            null, null, null);
        assertNotNull("Sea path should be legal for naval unit.", path);
        assertEquals("Sea path should start at settlement.", settlementTile,
            path.getTile());
        assertEquals("Sea path should end at sea tile.", seaTile,
            path.getLastNode().getTile());

        // Check giving the colonist access to a carrier makes the sea
        // path work.
        path = map.findPath(colonist, settlementTile, seaTile, galleon,
                            null, null);
        assertNotNull("Sea path should now be legal.", path);
        assertEquals("Sea path should start at settlement.", settlementTile,
            path.getTile());
        assertEquals("Sea path should end at sea tile.", seaTile,
            path.getLastNode().getTile());

        // Check the path still works if the colonist has to walk to
        // the carrier.
        Tile landTile = map.getTile(2, 2);
        path = map.findPath(colonist, landTile, seaTile, galleon, null, null);
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
        path = map.findPath(colonist, settlementTile, shoreTile,
                            galleon, null, null);
        assertNotNull("Shore path should be legal.", path);
        assertTrue("Shore path should have carrier moves.",
            path.usesCarrier());
        assertNotNull("Shore path should have drop node.",
            path.getCarrierMove().getTransportDropNode());

        // Check the colonist does not use the carrier if it does not help.
        Tile midTile = map.getTile(9, 4);
        path = map.findPath(colonist, map.getTile(2, 5), midTile,
                            galleon, null, null);
        assertNotNull("Middle path should be legal.", path);
        assertFalse("Middle path should not not use carrier.",
            path.usesCarrier());

        // Check path to Europe.
        path = map.findPath(colonist, settlementTile, europe,
                            galleon, null, null);
        assertNotNull("To-Europe path should be valid.", path);
        assertEquals("To-Europe path should end in Europe.", europe,
            path.getLastNode().getLocation());

        // Check path from Europe.
        path = map.findPath(colonist, europe, landTile, galleon, null, null);
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
        path = map.findPath(galleon, anotherSettlementTile, europe,
                            null, null, null);
        assertNotNull("From-lake-settlement path should be valid.", path);
        assertEquals("From-lake-settlement path should end in Europe.", europe,
            path.getLastNode().getLocation());

        // Put the unit on the carrier, put the carrier out to sea, and
        // find a path inland.
        colonist.setLocation(galleon);
        galleon.setLocation(seaTile);
        path = map.findPath(colonist, galleon, landTile, galleon, null, null);
        assertNotNull("From-galleon path should be valid.", path);
        assertEquals("From-galleon path should start at sea.", seaTile,
            path.getLocation());
        assertTrue("From-galleon path should start on carrier.",
            path.isOnCarrier());
        assertNotNull("From-galleon path should have a drop node.",
            path.getTransportDropNode());
    }

    public void testCopy() {
        Game game = getStandardGame();
        game.setMap(getTestMap());
        Map map = game.getMap();
        Colony colony = getStandardColony();
        Tile tile = colony.getTile();

        Map otherMap = map.copy(game, map.getClass());
        assertNotNull(otherMap);
        assertFalse(otherMap == map);
        assertEquals(otherMap.getId(), map.getId());
        Tile otherTile = otherMap.getTile(tile.getX(), tile.getY());
        assertNotNull(otherTile);
        assertFalse(otherTile == tile);
        assertEquals(otherTile.getId(), tile.getId());
        Colony otherColony = otherTile.getColony();
        assertNotNull(otherColony);
        assertFalse(otherColony == colony);
        assertEquals(otherColony.getId(), colony.getId());
    }
}
