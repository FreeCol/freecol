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

package net.sf.freecol.server.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.MockPseudoRandom;


public class ServerPlayerTest extends FreeColTestCase {	

    private static final GoodsType cottonType
        = spec().getGoodsType("model.goods.cotton");
    private static final GoodsType foodType
        = spec().getPrimaryFoodType();
    private static final GoodsType musketsType
        = spec().getGoodsType("model.goods.muskets");
    private static final GoodsType silverType
        = spec().getGoodsType("model.goods.silver");

    private static final TileType plains
        = spec().getTileType("model.tile.plains");
    
    private static final UnitType colonistType
        = spec().getUnitType("model.unit.freeColonist");
    private static final UnitType wagonTrainType
        = spec().getUnitType("model.unit.wagonTrain");
    private static final UnitType caravelType
        = spec().getUnitType("model.unit.caravel");
    private static final UnitType galleonType
        = spec().getUnitType("model.unit.galleon");
    private static final UnitType privateerType
        = spec().getUnitType("model.unit.privateer");


    @Override
    public void tearDown() throws Exception {
        ServerTestHelper.stopServerGame();
        super.tearDown();
    }

    /**
     * If we wait a number of turns after selling, the market should
     * recover and finally settle back to the initial levels.  Also
     * test that selling reduces the price for other players.
     */
    public void testMarketRecovery() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        InGameController igc = ServerTestHelper.getInGameController();

        ServerPlayer french = (ServerPlayer) game.getPlayerByNationId("model.nation.french");
        ServerPlayer english = (ServerPlayer) game.getPlayerByNationId("model.nation.english");
        Market frenchMarket = french.getMarket();
        Market englishMarket = english.getMarket();
        int frenchGold = french.getGold();
        int silverPrice = spec().getInitialPrice(silverType);

        // Sell lightly in the English market to check that the good
        // is now considered "traded".
        Random random = new Random();
        int m = english.sell(null, silverType, 1);
        if (m > 0) english.propagateToEuropeanMarkets(silverType, m, random);
        assertTrue(englishMarket.hasBeenTraded(silverType));
        int englishAmount = englishMarket.getAmountInMarket(silverType);

        // Sell heavily in the French market, price should drop.
        m = french.sell(null, silverType, 200);
        if (m > 0) french.propagateToEuropeanMarkets(silverType, m, random);
        assertEquals(frenchGold + silverPrice * 200, french.getGold());
        assertTrue(frenchMarket.hasBeenTraded(silverType));
        assertTrue(frenchMarket.getSalePrice(silverType, 1) < silverPrice);

        // Price might have dropped in the English market too, but
        // not as much as for the French.
        assertTrue("English silver increases due to French sales",
            englishMarket.getAmountInMarket(silverType) > englishAmount);
        assertTrue("English silver price might drop due to French sales",
            englishMarket.getSalePrice(silverType, 1) <= silverPrice);
        assertTrue("English silver price should drop less than French",
            englishMarket.getSalePrice(silverType, 1)
            >= frenchMarket.getSalePrice(silverType, 1));

        // Pretend time is passing.
        // Have to advance time as yearly goods removal is initially low.
        game.setTurn(new Turn(200));
        List<Integer> setValues = new ArrayList<>();
        setValues.add(20);
        MockPseudoRandom mockRandom = new MockPseudoRandom(setValues, true);
        ServerTestHelper.setRandom(mockRandom);
        boolean frenchRecovered = false;
        boolean englishRecovered = false;
        for (int i = 0; i < 100; i++) {
            igc.yearlyGoodsAdjust((ServerPlayer) french);
            if (frenchMarket.getSalePrice(silverType, 1) >= silverPrice) {
                frenchRecovered = true;
            }
            igc.yearlyGoodsAdjust((ServerPlayer) english);
            if (englishMarket.getSalePrice(silverType, 1) >= silverPrice) {
                englishRecovered = true;
            }
        }

        // Prices should have recovered.
        assertTrue("French silver price should have recovered",
                   frenchRecovered);
        assertTrue("English silver price should have recovered",
                   englishRecovered);
    }

    public void testHasExploredTile() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        
        Map map = game.getMap();
        ServerPlayer dutch = (ServerPlayer) game.getPlayerByNationId("model.nation.dutch");
        ServerPlayer french = (ServerPlayer) game.getPlayerByNationId("model.nation.french");
        InGameController igc = ServerTestHelper.getInGameController();
        Tile tile0 = map.getTile(0, 0);
        Tile tile1 = map.getTile(6, 8);
        Tile tile2 = map.getTile(8, 6);
        assertFalse("Setup error, tile1 should not be explored by dutch player",dutch.hasExplored(tile1));
        assertFalse("Setup error, tile1 should not be explored by french player",french.hasExplored(tile1));
        assertFalse("Setup error, tile2 should not be explored by dutch player",dutch.hasExplored(tile2));
        assertFalse("Setup error, tile2 should not be explored by french player",french.hasExplored(tile2));

        igc.move(dutch, new ServerUnit(game, tile0, dutch, colonistType),
                 tile1);
        igc.move(french, new ServerUnit(game, tile0, french, colonistType),
                 tile2);
        assertTrue("Tile1 is explored", tile1.isExplored());
        assertTrue("Tile2 is explored", tile2.isExplored());
        assertTrue("Tile1 should be explored by dutch player",dutch.hasExplored(tile1));
        assertFalse("Tile1 should not be explored by french player",french.hasExplored(tile1));
        assertFalse("Tile2 should not be explored by dutch player",dutch.hasExplored(tile2));
        assertTrue("Tile2 should be explored by french player",french.hasExplored(tile2));
    }

    public void testLoadInColony() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        Map map = game.getMap();
        InGameController igc = ServerTestHelper.getInGameController();
        
        Colony colony = getStandardColony();
        colony.addGoods(cottonType, 200);
        ServerPlayer dutch
            = (ServerPlayer)game.getPlayerByNationId("model.nation.dutch");
        Unit wagonInColony
            = new ServerUnit(game, colony.getTile(), dutch, wagonTrainType);
        Unit wagonNotInColony
            = new ServerUnit(game, map.getTile(10, 10), dutch, wagonTrainType);

        // Fail to move to wagon not in colony
        igc.loadGoods(dutch, colony, cottonType, 50, wagonNotInColony);
        assertEquals(0, wagonNotInColony.getGoodsCount(cottonType));
        assertEquals(200, colony.getGoodsCount(cottonType));

        // Check colony to wagon
        igc.loadGoods(dutch, colony, cottonType, 10, wagonInColony);
        assertEquals(10, wagonInColony.getGoodsCount(cottonType));
        assertEquals(190, colony.getGoodsCount(cottonType));

        // Check wagon to colony
        igc.unloadGoods(dutch, cottonType, 5, wagonInColony);
        assertEquals(5, wagonInColony.getGoodsCount(cottonType));
        assertEquals(195, colony.getGoodsCount(cottonType));

        // Fail to load more than present
        igc.loadGoods(dutch, colony, cottonType, 200, wagonInColony);
        assertEquals(5, wagonInColony.getGoodsCount(cottonType));
        assertEquals(195, colony.getGoodsCount(cottonType));

        // Fill wagon
        igc.loadGoods(dutch, colony, cottonType, 195, wagonInColony);
        assertEquals(200, wagonInColony.getGoodsCount(cottonType));
        assertEquals(0, colony.getGoodsCount(cottonType));
        assertFalse(wagonInColony.hasSpaceLeft());

        // Fail to add more
        colony.addGoods(cottonType, 1);
        igc.loadGoods(dutch, colony, cottonType, 1, wagonInColony);
        assertEquals(200, wagonInColony.getGoodsCount(cottonType));
        assertEquals(1, colony.getGoodsCount(cottonType));
    }

    public void testLoadInEurope() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        Map map = game.getMap();
        InGameController igc = ServerTestHelper.getInGameController();

        ServerPlayer dutch
            = (ServerPlayer)game.getPlayerByNationId("model.nation.dutch");
        Europe europe = dutch.getEurope();
        Unit privateer1
            = new ServerUnit(game, europe, dutch, privateerType);
        Unit privateer2
            = new ServerUnit(game, map.getTile(1,1), dutch, privateerType);

        // Check Europe to privateer, should fail due to funds
        igc.loadGoods(dutch, europe, cottonType, 10, privateer1);
        assertEquals(0, privateer1.getGoodsCount(cottonType));

        // Add gold and succeed
        dutch.setGold(10000);
        igc.loadGoods(dutch, europe, cottonType, 10, privateer1);
        assertEquals(10, privateer1.getGoodsCount(cottonType));

        // Check Privateer to Europe
        igc.unloadGoods(dutch, cottonType, 5, privateer1);
        assertEquals(5, privateer1.getGoodsCount(cottonType));

        // Fail to load to privateer not in Europe
        igc.loadGoods(dutch, europe, cottonType, 10, privateer2);
        assertEquals(0, privateer2.getGoodsCount(cottonType));

        // Establish boycott and fail again
        dutch.getMarket().setArrears(cottonType, 1);
        igc.loadGoods(dutch, europe, cottonType, 10, privateer1);
        assertEquals(5, privateer1.getGoodsCount(cottonType));
    }

    public void testCheckGameOverNoUnits() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        ServerPlayer dutch = (ServerPlayer)game.getPlayerByNationId("model.nation.dutch");

        dutch.setGold(0);
        assertEquals("Should not have units", 0, dutch.getUnits().size());
        assertEquals("Should be game over due to no carrier", -1,
                     dutch.checkForDeath());
    }

    public void testCheckNoGameOverEnoughMoney() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        ServerPlayer dutch = (ServerPlayer)game.getPlayerByNationId("model.nation.dutch");

        dutch.setGold(10000);
        assertEquals("Should not be game, enough money", 0,
                     dutch.checkForDeath());
    }

    public void testCheckNoGameOverHasColonistInNewWorld() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        Map map = game.getMap();
        ServerPlayer dutch = (ServerPlayer)game.getPlayerByNationId("model.nation.dutch");
        dutch.setGold(0);

        new ServerUnit(game, map.getTile(4, 7), dutch, colonistType);
        assertEquals("Should not be game over, has units", 0,
                     dutch.checkForDeath());
    }

    public void testCheckGameOver1600Threshold() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        ServerPlayer dutch = (ServerPlayer)game.getPlayerByNationId("model.nation.dutch");
        dutch.setGold(0);

        new ServerUnit(game, dutch.getEurope(), dutch, galleonType);
        assertEquals("Should have 1 unit", 1, dutch.getUnits().size());
        assertEquals("Should not be game over, not 1600 yet, autorecruit", 1,
                     dutch.checkForDeath());

        new ServerUnit(game, dutch.getEurope(), dutch, colonistType);
        assertEquals("Should have 2 units", 2, dutch.getUnits().size());
        assertEquals("Should not be game over, not 1600 yet", 0,
                     dutch.checkForDeath());

        game.setTurn(new Turn(1600));
        assertEquals("Should be game over, no new world presence >= 1600", -1,
                     dutch.checkForDeath());
    }

    public void testCheckGameOverUnitsGoingToEurope() {
        Game game = ServerTestHelper.startServerGame(getTestMap(spec().getTileType("model.tile.highSeas")));
        Map map = game.getMap();
        InGameController igc = ServerTestHelper.getInGameController();
        ServerPlayer dutch = (ServerPlayer)game.getPlayerByNationId("model.nation.dutch");
        dutch.setGold(0);

        Unit galleon = new ServerUnit(game, map.getTile(6, 8), dutch,
                                      galleonType);
        Unit colonist = new ServerUnit(game, galleon, dutch, colonistType);
        assertTrue("Colonist should be aboard the galleon",
                   colonist.getLocation() == galleon);
        assertEquals("Galleon should have a colonist onboard",
                     1, galleon.getUnitCount());
        igc.moveTo(dutch, galleon, dutch.getEurope());

        assertEquals("Should not be game over, units between new world and europe", 0,
                     dutch.checkForDeath());

        game.setTurn(new Turn(1600));
        assertEquals("Should be game over, no new world presence >= 1600", -1,
                     dutch.checkForDeath());
    }

    public void testCheckGameOverUnitsGoingToNewWorld() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        Map map = game.getMap();
        InGameController igc = ServerTestHelper.getInGameController();
        ServerPlayer dutch = (ServerPlayer)game.getPlayerByNationId("model.nation.dutch");
        dutch.setGold(0);

        Unit galleon = new ServerUnit(game, dutch.getEurope(), dutch,
                                      galleonType);
        Unit colonist = new ServerUnit(game, galleon, dutch, colonistType);
        assertEquals("Colonist should be aboard the galleon", galleon,
                     colonist.getLocation());
        assertEquals("Galleon should have a colonist onboard", 1,
                     galleon.getUnitCount());
        igc.moveTo(dutch, galleon, map);

        assertEquals("Should not be game over, units between new world and europe", 0,
                     dutch.checkForDeath());

        game.setTurn(new Turn(1600));
        assertEquals("Should be game over, no new world presence >= 1600", -1,
                     dutch.checkForDeath());
    }

    public void testSellingMakesPricesFall() {
        Game g = ServerTestHelper.startServerGame(getTestMap());
        ServerPlayer p = (ServerPlayer)g.getPlayerByNationId("model.nation.dutch");

        Market dm = p.getMarket();
        int previousGold = p.getGold();
        int price = spec().getInitialPrice(silverType);
        p.sell(null, silverType, 1000);

        assertEquals(previousGold + price * 1000, p.getGold());
        assertTrue(dm.getSalePrice(silverType, 1) < price);
    }

    public void testBuyingMakesPricesRaise() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        ServerPlayer player = (ServerPlayer)game.getPlayerByNationId("model.nation.dutch");

        Market dm = player.getMarket();
        player.modifyGold(1000000);
        int price = dm.getCostToBuy(foodType);
        int n = player.buy(new GoodsContainer(game, player.getEurope()),
                                              foodType, 10000);
        assertEquals(1000000 - 10000 * price, player.getGold());
        assertTrue(dm.getBidPrice(foodType, 1) > price);
    }

    /**
     * Helper Method for finding out how much of a good to sell until
     * the price drops.
     */
    public int sellUntilPriceDrop(Game game, ServerPlayer player,
                                  GoodsType type) {
        int result = 0;
        Market market = player.getMarket();
        int price = market.getSalePrice(type, 1);
        if (price == 0) {
            throw new IllegalArgumentException("Price is already 0 for selling " + type);
        }

        while (price == market.getSalePrice(type, 1)) {
            player.sell(null, type, 10);
            result++;
        }
        return result;
    }

    /*
     * Helper method for finding out how much to buy of a good before
     * the prices rises.
     */
    public int buyUntilPriceRise(Game game, ServerPlayer player,
                                 GoodsType type) {
        Game g = ServerTestHelper.startServerGame(getTestMap());
        Random random = new Random();
        int result = 0;
        Market market = player.getMarket();
        int price = market.getBidPrice(type, 1);

        if (price == 20) {
            throw new IllegalArgumentException("Price is already 20 for buying " + type);
        }

        GoodsContainer container
            = new GoodsContainer(game, player.getEurope());
        while (price == market.getBidPrice(type, 1)) {
            player.buy(container, type, 10);
            result++;
        }
        return result;
    }

    /**
     * Assert that the dutch nation has more stable prices than the other
     * nations
     */
    public void testDutchMarket() {

        Game game = getStandardGame();
        ServerPlayer dutch = (ServerPlayer)game.getPlayerByNationId("model.nation.dutch");
        ServerPlayer french = (ServerPlayer)game.getPlayerByNationId("model.nation.french");
        assertEquals("model.nationType.trade", dutch.getNationType().getId());
        assertTrue(dutch.getNationType().hasModifier(Modifier.TRADE_BONUS));
        assertTrue(dutch.hasModifier(Modifier.TRADE_BONUS));

        {// Test that the dutch can sell more goods until the price drops
            int dutchSellAmount = sellUntilPriceDrop(game, dutch, silverType);

            Game g2 = getStandardGame();
            ServerPlayer french2 = (ServerPlayer)g2.getPlayerByNationId("model.nation.french");
            int frenchSellAmount = sellUntilPriceDrop(g2, french2, silverType);

            assertTrue(dutchSellAmount > frenchSellAmount);
        }
        {// Test that the dutch can buy more goods until the price rises
            dutch.modifyGold(10000);
            french.modifyGold(10000);
            int dutchBuyAmount = buyUntilPriceRise(getStandardGame(), dutch, musketsType);

            int frenchBuyAmount = buyUntilPriceRise(getStandardGame(), french, musketsType);

            assertTrue(dutchBuyAmount > frenchBuyAmount);
        }
    }
}
