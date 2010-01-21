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

package net.sf.freecol.server.model;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.Specification;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.control.Controller;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.control.PreGameController;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.MockMapGenerator;


public class ServerPlayerTest extends FreeColTestCase {	
    BuildingType schoolHouseType = spec().getBuildingType("model.building.Schoolhouse");

    GoodsType cottonType = FreeCol.getSpecification().getGoodsType("model.goods.cotton");

    TileType plains = spec().getTileType("model.tile.plains");
    
    UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
    UnitType hardyPioneerType = spec().getUnitType("model.unit.hardyPioneer");
    UnitType treasureTrainType = spec().getUnitType("model.unit.treasureTrain");
    UnitType wagonTrainType = spec().getUnitType("model.unit.wagonTrain");
    UnitType caravelType = spec().getUnitType("model.unit.caravel");
    UnitType galleonType = spec().getUnitType("model.unit.galleon");
    UnitType privateerType = spec().getUnitType("model.unit.privateer");

    FreeColServer server = null;
	
    public void tearDown() throws Exception {
        if(server != null){
            // must make sure that the server is stopped
            ServerTestHelper.stopServer(server);
            FreeCol.setInDebugMode(false);
            FreeColTestCase.setGame(null);
            server = null;
        }
        super.tearDown();
    }

    /**
     * If we wait a number of turns after selling, the market should
     * recover and finally settle back to the initial levels.  Also
     * test that selling reduces the price for other players.
     */
    public void testMarketRecovery() {
        if (server == null) {
            server = ServerTestHelper.startServer(false, true);
        }

        Controller c = server.getController();
        PreGameController pgc = (PreGameController)c;

        try {
            pgc.startGame();
        } catch (FreeColException e) {
            fail("Failed to start game");
        }

        Game game = server.getGame();
        FreeColTestCase.setGame(game);

        Player french = game.getPlayer("model.nation.french");
        Player english = game.getPlayer("model.nation.english");
        Market frenchMarket = french.getMarket();
        Market englishMarket = english.getMarket();
        int frenchGold = french.getGold();
        int englishGold = english.getGold();
        Specification s = spec();
        GoodsType silver = s.getGoodsType("model.goods.silver");
        int silverPrice = silver.getInitialSellPrice();

        // Sell lightly in the English market to check that the good
        // is now considered "traded".
        englishMarket.sell(silver, 1, english);
        assertTrue(englishMarket.hasBeenTraded(silver));

        // Sell heavily in the French market, price should drop.
        frenchMarket.sell(silver, 200, french);
        assertEquals(frenchGold + silverPrice * 200, french.getGold());
        assertTrue(frenchMarket.hasBeenTraded(silver));
        assertTrue(frenchMarket.getSalePrice(silver, 1) < silverPrice);

        // Price should have dropped in the English market too, but
        // not as much as for the French.
        // assertTrue(englishMarket.getSalePrice(silver, 1) < silverPrice);
        // assertTrue(englishMarket.getSalePrice(silver, 1) >= frenchMarket.getSalePrice(silver, 1));
        // This has never worked while the test was done client side,
        // and had the comment: ``This does not work without real
        // ModelControllers''.  TODO: Revisit when the client-server
        // conversion of sales is complete.

        // Pretend time is passing.
        // Have to advance time as yearly goods removal is initially low.
        InGameController igc = server.getInGameController();
        game.getTurn().setNumber(200);
        for (int i = 0; i < 100; i++) {
            igc.yearlyGoodsRemoval((ServerPlayer) french);
            igc.yearlyGoodsRemoval((ServerPlayer) english);
        }

        // Price should have recovered
        int newPrice;
        newPrice = frenchMarket.getSalePrice(silver, 1);
        assertTrue("French silver price " + newPrice
                   + " should have recovered to " + silverPrice,
                   newPrice >= silverPrice);
        newPrice = englishMarket.getSalePrice(silver, 1);
        assertTrue("English silver price " + newPrice
                   + " should have recovered to " + silverPrice,
                   newPrice >= silverPrice);
    }

    /*
     * Tests worker allocation regarding building tasks
     */
    public void testCashInTreasure() {
        if (server == null) {
            server = ServerTestHelper.startServer(false, true);
        }
        Map map = getCoastTestMap(plains, true);
        server.setMapGenerator(new MockMapGenerator(map));
        Controller c = server.getController();
        PreGameController pgc = (PreGameController)c;
        try {
            pgc.startGame();
        } catch (FreeColException e) {
            fail("Failed to start game");
        }
        Game game = server.getGame();
        FreeColTestCase.setGame(game);
     
        ServerPlayer dutch = (ServerPlayer) game.getPlayer("model.nation.dutch");
        Tile tile = map.getTile(10, 4);
        Unit ship = new Unit(game, tile, dutch, galleonType, UnitState.ACTIVE, galleonType.getDefaultEquipment());
        Unit treasure = new Unit(game, tile, dutch, treasureTrainType, UnitState.ACTIVE, treasureTrainType.getDefaultEquipment());
        assertTrue(treasure.canCarryTreasure());
        treasure.setTreasureAmount(100);
        
        assertFalse(treasure.canCashInTreasureTrain()); // from a tile
        treasure.setLocation(ship);
        assertFalse(treasure.canCashInTreasureTrain()); // from a ship
        ship.setLocation(dutch.getEurope());    
        assertTrue(treasure.canCashInTreasureTrain()); // from a ship in Europe
        int fee = treasure.getTransportFee();
        assertEquals(0, fee);
        int oldGold = dutch.getGold();
        dutch.cashInTreasureTrain(treasure);
        assertEquals(100, dutch.getGold() - oldGold);

        // Succeed from a port with a connection to Europe
        Colony port = getStandardColony(1, 9, 4);
        assertFalse(port.isLandLocked());
        assertTrue(port.isConnected());
        treasure.setLocation(port);
        assertTrue(treasure.canCashInTreasureTrain());

        // Fail from a landlocked colony
        Colony inland = getStandardColony(1, 7, 7);
        assertTrue(inland.isLandLocked());
        assertFalse(inland.isConnected());
        treasure.setLocation(inland);
        assertFalse(treasure.canCashInTreasureTrain());

        // Fail from a colony with a port but no connection to Europe
        map.getTile(5, 5).setType(FreeCol.getSpecification().getTileType("model.tile.lake"));
        Colony lake = getStandardColony(1, 4, 5);
        assertFalse(lake.isLandLocked());
        assertFalse(lake.isConnected());
        treasure.setLocation(lake);
        assertFalse(treasure.canCashInTreasureTrain());
    }
	
    public void testHasExploredTile() {
        if (server == null) {
            server = ServerTestHelper.startServer(false, true);
        }
        Map map = getTestMap(plains);
        server.setMapGenerator(new MockMapGenerator(getTestMap()));
        PreGameController pgc = (PreGameController) server.getController();
        try {
            pgc.startGame();
        } catch (FreeColException e) {
            fail("Failed to start game");
        }
        Game game = server.getGame();
        FreeColTestCase.setGame(game);

        ServerPlayer dutch = (ServerPlayer) game.getPlayer("model.nation.dutch");
        ServerPlayer french = (ServerPlayer) game.getPlayer("model.nation.french");
        Tile tile1 = map.getTile(6, 8);
        Tile tile2 = map.getTile(8, 6);
        assertFalse("Setup error, tile1 should not be explored by dutch player",dutch.hasExplored(tile1));
        assertFalse("Setup error, tile1 should not be explored by french player",french.hasExplored(tile1));
        assertFalse("Setup error, tile2 should not be explored by dutch player",dutch.hasExplored(tile2));
        assertFalse("Setup error, tile2 should not be explored by french player",french.hasExplored(tile2));

        new Unit(game, tile1, dutch, colonistType, UnitState.SENTRY);
        new Unit(game, tile2, french, colonistType, UnitState.SENTRY);
        assertTrue("Tile1 should be explored by dutch player",dutch.hasExplored(tile1));
        assertFalse("Tile1 should not be explored by french player",french.hasExplored(tile1));
        assertFalse("Tile2 should not be explored by dutch player",dutch.hasExplored(tile2));
        assertTrue("Tile2 should be explored by french player",french.hasExplored(tile2));
    }

    public void testEmbark() {
        if (server == null) {
            server = ServerTestHelper.startServer(false, true);
        }
        Map map = getCoastTestMap(plains);
        server.setMapGenerator(new MockMapGenerator(getTestMap()));
        PreGameController pgc = (PreGameController) server.getController();
        try {
            pgc.startGame();
        } catch (FreeColException e) {
            fail("Failed to start game");
        }
        Game game = server.getGame();
        FreeColTestCase.setGame(game);

        //Game game = getStandardGame();
        //Map map = getTestMap();
        //Tile tile = map.getTile(6, 8);
        //game.setMap(map);

        InGameController igc = (InGameController) server.getController();
        Tile landTile = map.getTile(9, 9);
        Tile seaTile = map.getTile(10, 9);
        ServerPlayer dutch = (ServerPlayer) game.getPlayer("model.nation.dutch");
        Unit colonist = new Unit(game, landTile, dutch, colonistType, UnitState.ACTIVE);
        Unit galleon = new Unit(game, seaTile, dutch, galleonType, UnitState.ACTIVE);
        Unit caravel = new Unit(game, seaTile, dutch, caravelType, UnitState.ACTIVE);
        caravel.getType().setSpaceTaken(2);
        Unit wagon = new Unit(game, landTile, dutch, wagonTrainType, UnitState.ACTIVE);

        // can not put ship on carrier
        assertFalse(igc.embarkUnit(dutch, caravel, galleon));

        // can not put wagon on galleon at its normal size
        wagon.getType().setSpaceTaken(12);
        assertFalse(igc.embarkUnit(dutch, wagon, galleon));

        // but we can if it is made smaller
        wagon.getType().setSpaceTaken(2);
        assertTrue(igc.embarkUnit(dutch, wagon, galleon));
        assertEquals(UnitState.SENTRY, wagon.getState());

        // can put colonist on carrier
        assertTrue(igc.embarkUnit(dutch, colonist, caravel));
        assertEquals(UnitState.SENTRY, colonist.getState());
    }

    public void testLoadInColony() {
        if (server == null) {
            server = ServerTestHelper.startServer(false, true);
        }
        Map map = getCoastTestMap(plains);
        server.setMapGenerator(new MockMapGenerator(getTestMap()));
        PreGameController pgc = (PreGameController) server.getController();
        try {
            pgc.startGame();
        } catch (FreeColException e) {
            fail("Failed to start game");
        }
        Game game = server.getGame();
        FreeColTestCase.setGame(game);

        Colony colony = getStandardColony();
        ServerPlayer dutch = (ServerPlayer) game.getPlayer("model.nation.dutch");
        Unit wagonInColony = new Unit(game, colony.getTile(), dutch,
                                      wagonTrainType, UnitState.ACTIVE);
        Unit wagonNotInColony = new Unit(game, map.getTile(10, 10), dutch,
                                         wagonTrainType, UnitState.ACTIVE);
        InGameController igc = (InGameController) server.getController();
        Goods cotton = new Goods(game, null, cottonType, 75);

        // Check if location null
        assertEquals(null, cotton.getTile());

        // Check that it does not work if current Location == null
        try {
            igc.moveGoods(cotton, wagonInColony);
            fail();
        } catch (IllegalStateException e) {
        }
        try {
            igc.moveGoods(cotton, wagonNotInColony);
            fail();
        } catch (IllegalStateException e) {
        }

        // Check wagon to colony
        cotton.setLocation(wagonInColony);
        igc.moveGoods(cotton, colony);
        assertEquals(cotton.getLocation(), colony);
        assertEquals(75, colony.getGoodsCount(cottonType));

        // Check from colony to wagon train
        igc.moveGoods(cotton, wagonInColony);
        assertEquals(wagonInColony, cotton.getLocation());
        assertEquals(0, colony.getGoodsCount(cottonType));

        // Check failure units not co-located
        try {
            igc.moveGoods(cotton, wagonNotInColony);
            fail();
        } catch (IllegalStateException e) {
        }

        // Check failure to non-GoodsContainer (Tile)
        try {
            igc.moveGoods(cotton, map.getTile(9, 10));
            fail();
        } catch (IllegalStateException e) {
        }

        // Check from unit to unit
        wagonInColony.setLocation(wagonNotInColony.getTile());
        igc.moveGoods(cotton, wagonNotInColony);
        assertEquals(wagonNotInColony, cotton.getLocation());
    }

    public void testLoadInEurope() {
        if (server == null) {
            server = ServerTestHelper.startServer(false, true);
        }
        Map map = getCoastTestMap(plains);
        server.setMapGenerator(new MockMapGenerator(getTestMap()));
        PreGameController pgc = (PreGameController) server.getController();
        try {
            pgc.startGame();
        } catch (FreeColException e) {
            fail("Failed to start game");
        }
        Game game = server.getGame();
        FreeColTestCase.setGame(game);

        ServerPlayer dutch = (ServerPlayer) game.getPlayer("model.nation.dutch");
        Goods cotton = new Goods(game, null, cottonType, 75);
        Europe europe = dutch.getEurope();
        Unit privateer1 = new Unit(game, europe, dutch,
                                   privateerType, UnitState.ACTIVE);
        Unit privateer2 = new Unit(game, europe, dutch,
                                   privateerType, UnitState.ACTIVE);
        InGameController igc = (InGameController) server.getController();

        // While source in Europe, target in Europe
        cotton.setLocation(privateer1);
        igc.moveGoods(cotton, privateer2);
        assertEquals(privateer2, cotton.getLocation());

        // Can not unload directly to Europe
        try {
            igc.moveGoods(cotton, europe);
            fail();
        } catch (IllegalStateException e) {
        }

        // While source moving from America, target in Europe
        cotton.setLocation(privateer1);
        assertEquals(europe, privateer1.getLocation());
        privateer1.moveToAmerica();
        try {
            igc.moveGoods(cotton, privateer2);
            fail();
        } catch (IllegalStateException e) {
        }

        // While source moving to America, target in Europe
        cotton.setLocation(privateer1);
        privateer1.moveToEurope();
        try {
            igc.moveGoods(cotton, privateer2);
            fail();
        } catch (IllegalStateException e) {
        }

        // While source in Europe, target moving to America
        privateer1.setLocation(europe);
        privateer2.moveToAmerica();
        cotton.setLocation(privateer1);
        try {
            igc.moveGoods(cotton, privateer2);
            fail();
        } catch (IllegalStateException e) {
        }

        // While source moving to America, target moving to America
        cotton.setLocation(privateer1);
        privateer1.moveToAmerica();
        try {
            igc.moveGoods(cotton, privateer2);
            fail();
        } catch (IllegalStateException e) {
        }

        // While source moving from America, target moving to America
        cotton.setLocation(privateer1);
        privateer1.moveToEurope();
        try {
            igc.moveGoods(cotton, privateer2);
            fail();
        } catch (IllegalStateException e) {
        }

        // While source in Europe, target moving from America
        privateer1.setLocation(europe);
        privateer2.moveToEurope();

        cotton.setLocation(privateer1);
        try {
            igc.moveGoods(cotton, privateer2);
            fail();
        } catch (IllegalStateException e) {
        }

        // While source moving to America, target moving from America
        cotton.setLocation(privateer1);
        privateer1.moveToAmerica();
        try {
            igc.moveGoods(cotton, privateer2);
            fail();
        } catch (IllegalStateException e) {
        }

        // While source moving from America, target moving from America
        cotton.setLocation(privateer1);
        privateer1.moveToEurope();
        try {
            igc.moveGoods(cotton, privateer2);
            fail();
        } catch (IllegalStateException e) {
        }
    }

    public void testClearSpecialty() {
        if (server == null) {
            server = ServerTestHelper.startServer(false, true);
        }
        Map map = getTestMap(plains);
        server.setMapGenerator(new MockMapGenerator(getTestMap()));
        PreGameController pgc = (PreGameController) server.getController();
        try {
            pgc.startGame();
        } catch (FreeColException e) {
            fail("Failed to start game");
        }
        Game game = server.getGame();
        FreeColTestCase.setGame(game);

        ServerPlayer dutch = (ServerPlayer) game.getPlayer("model.nation.dutch");
        Unit unit = new Unit(game, map.getTile(5, 8), dutch, hardyPioneerType,
                             UnitState.ACTIVE);
        assertTrue("Unit should be a hardy pioneer",
                   unit.getType() == hardyPioneerType);
        InGameController igc = (InGameController) server.getController();

        // Basic function
        igc.clearSpeciality(unit, dutch);
        assertFalse("Unit was not cleared of its specialty",
                    unit.getType() == hardyPioneerType);

        // Can not clear speciality while teaching
        Colony colony = getStandardColony();
        Building school = new Building(game, colony, schoolHouseType);
        colony.addBuilding(school);
        Unit teacher = new Unit(game, school, colony.getOwner(),
                                hardyPioneerType, UnitState.ACTIVE);
        assertTrue("Unit should be a hardy pioneer",
                   teacher.getType() == hardyPioneerType);
        try {
            igc.clearSpeciality(teacher, dutch);
            fail("Unit specialty cannot be cleared, a IllegalStateException should have been raised");
        } catch (IllegalStateException e) {
        }
    }

}
