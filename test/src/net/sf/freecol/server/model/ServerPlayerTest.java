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
import net.sf.freecol.common.model.Specification;
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

    GoodsType cottonType = FreeCol.getSpecification().getGoodsType("model.goods.cotton");

    TileType plains = spec().getTileType("model.tile.plains");
    
    UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
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

    private Game startSPT() {
        if (server == null) {
            server = ServerTestHelper.startServer(false, true);
        }
        server.setMapGenerator(new MockMapGenerator(getTestMap()));
        PreGameController pgc = (PreGameController) server.getController();
        try {
            pgc.startGame();
        } catch (FreeColException e) {
            fail("Failed to start game");
        }
        Game game = server.getGame();
        FreeColTestCase.setGame(game);
        return game;
    }

    /**
     * If we wait a number of turns after selling, the market should
     * recover and finally settle back to the initial levels.  Also
     * test that selling reduces the price for other players.
     */
    public void testMarketRecovery() {
        Game game = startSPT();

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

    public void testHasExploredTile() {
        // we need to update the reference
        Game game = startSPT();
        Map map = game.getMap();
        
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

    public void testLoadInColony() {
        Game game = startSPT();
        Map map = game.getMap();
        
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
        Game game = startSPT();

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
        igc.moveToEurope(dutch, privateer1);
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
        igc.moveToEurope(dutch, privateer1);
        try {
            igc.moveGoods(cotton, privateer2);
            fail();
        } catch (IllegalStateException e) {
        }

        // While source in Europe, target moving from America
        privateer1.setLocation(europe);
        igc.moveToEurope(dutch, privateer2);

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
        igc.moveToEurope(dutch, privateer1);
        try {
            igc.moveGoods(cotton, privateer2);
            fail();
        } catch (IllegalStateException e) {
        }
    }

    public void testCheckGameOverNoUnits() {
        Game game = startSPT();

        ServerPlayer dutch = (ServerPlayer) game.getPlayer("model.nation.dutch");

        assertTrue("Should be game over due to no units",
                   dutch.checkForDeath());
    }

    public void testCheckNoGameOverEnoughMoney() {
        Game game = startSPT();

        ServerPlayer dutch = (ServerPlayer) game.getPlayer("model.nation.dutch");

        dutch.modifyGold(10000);
        assertFalse("Should not be game, enough money",
                    dutch.checkForDeath());
    }

    public void testCheckNoGameOverHasColonistInNewWorld() {
        Game game = startSPT();
        Map map = getTestMap();
        game.setMap(map);

        ServerPlayer dutch = (ServerPlayer) game.getPlayer("model.nation.dutch");

        new Unit(game, map.getTile(4, 7), dutch, colonistType, UnitState.ACTIVE);

        assertFalse("Should not be game over, has units",
                    dutch.checkForDeath());
    }

    public void testCheckGameOver1600Threshold() {
        Game game = startSPT();
        Map map = getTestMap();
        game.setMap(map);

        ServerPlayer dutch = (ServerPlayer) game.getPlayer("model.nation.dutch");

        UnitType galleon = spec().getUnitType("model.unit.galleon");
        new Unit(game, dutch.getEurope(), dutch, colonistType, UnitState.SENTRY);
        new Unit(game, dutch.getEurope(), dutch, galleon, UnitState.SENTRY);
        assertFalse("Should not be game over, not 1600 yet",
                    dutch.checkForDeath());

        game.setTurn(new Turn(1600));
        assertTrue("Should be game over, no new world presence after 1600",
                   dutch.checkForDeath());
    }

    public void testCheckGameOverUnitsGoingToEurope() {
        Game game = startSPT();
        Map map = getTestMap(spec().getTileType("model.tile.highSeas"));
        game.setMap(map);

        ServerPlayer dutch = (ServerPlayer) game.getPlayer("model.nation.dutch");

        UnitType galleonType = spec().getUnitType("model.unit.galleon");
        Unit galleon = new Unit(game,map.getTile(6, 8) , dutch, galleonType, UnitState.ACTIVE);
        Unit colonist = new Unit(game, galleon, dutch, colonistType, UnitState.SENTRY);
        assertTrue("Colonist should be aboard the galleon",colonist.getLocation() == galleon);
        assertEquals("Galleon should have a colonist onboard",1,galleon.getUnitCount());
        InGameController igc = (InGameController) server.getController();
        igc.moveToEurope(dutch, galleon);

        assertFalse("Should not be game over, units between new world and europe",
                    dutch.checkForDeath());

        game.setTurn(new Turn(1600));
        assertTrue("Should be game over, no new world presence after 1600",
                   dutch.checkForDeath());
    }

    public void testCheckGameOverUnitsGoingToNewWorld() {
        Game game = startSPT();
        Map map = getTestMap();
        game.setMap(map);

        ServerPlayer dutch = (ServerPlayer) game.getPlayer("model.nation.dutch");

        Unit galleon = new Unit(game,dutch.getEurope() , dutch, galleonType, UnitState.ACTIVE);
        Unit colonist = new Unit(game, galleon, dutch, colonistType, UnitState.SENTRY);
        assertTrue("Colonist should be aboard the galleon",colonist.getLocation() == galleon);
        assertEquals("Galleon should have a colonist onboard",1,galleon.getUnitCount());
        galleon.moveToAmerica();

        assertFalse("Should not be game over, units between new world and europe",
                    dutch.checkForDeath());

        game.setTurn(new Turn(1600));
        assertTrue("Should be game over, no new world presence after 1600",
                   dutch.checkForDeath());
    }

}
