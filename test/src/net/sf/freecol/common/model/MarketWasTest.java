/**
 *  Copyright (C) 2002-2024  The FreeCol Team
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

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import net.sf.freecol.util.test.FreeColTestCase;

/**
 * Regression tests for the MarketWas utility class.
 */
public class MarketWasTest extends FreeColTestCase {

    private Player getHumanPlayer(Game game) {
        return game.getPlayers(p -> true).findFirst().orElse(null);
    }

    public void testFireChangesOnPriceShift() {
        Game game = getStandardGame();
        Player player = getHumanPlayer(game);
        Market market = player.getMarket();
        GoodsType rum = spec().getGoodsType("model.goods.rum");
        MarketData md = market.getMarketData(rum);

        md.setAmountInMarket(10);
        md.update();
        int priceHigh = market.getPaidForSale(rum);

        MarketWas marketWas = new MarketWas(player);

        final List<String> eventsFound = new ArrayList<>();
        PropertyChangeListener diagnosticListener = evt -> eventsFound.add(evt.getPropertyName());
        md.addPropertyChangeListener(diagnosticListener);
        market.addPropertyChangeListener(diagnosticListener);

        md.setAmountInMarket(100000);
        md.update();
        int priceLow = market.getPaidForSale(rum);

        assertTrue("Test setup failed: Price must move for MarketWas to fire. High: "
            + priceHigh + " Low: " + priceLow, priceHigh != priceLow);

        marketWas.add(new AbstractGoods(rum, 100));
        boolean result = marketWas.fireChanges();

        assertTrue("fireChanges() should return true when changes are detected", result);
        assertFalse("A PropertyChangeEvent should have been captured in the listener", 
            eventsFound.isEmpty());
    }

    public void testTaxPropagation() {
        Game game = getStandardGame();
        Player player = getHumanPlayer(game);
        player.setTax(15);

        Market market = player.getMarket();
        GoodsType cigars = spec().getGoodsType("model.goods.cigars");

        MockTransactionListener listener = new MockTransactionListener();
        market.addTransactionListener(listener);

        MarketWas marketWas = new MarketWas(player);
        market.getMarketData(cigars).setAmountInMarket(100);
        market.getMarketData(cigars).update();

        marketWas.add(new AbstractGoods(cigars, 50));
        marketWas.fireChanges();

        assertEquals("Tax should propagate from player to listener",
            player.getTax(), listener.lastTax);
        assertEquals("Correct goods type should be logged", cigars, listener.lastType);
    }

    public void testAddAllTransactions() {
        Game game = getStandardGame();
        Player player = getHumanPlayer(game);
        Market market = player.getMarket();

        GoodsType rum = spec().getGoodsType("model.goods.rum");
        GoodsType silver = spec().getGoodsType("model.goods.silver");

        MockTransactionListener listener = new MockTransactionListener();
        market.addTransactionListener(listener);

        MarketWas marketWas = new MarketWas(player);

        market.getMarketData(rum).setAmountInMarket(5000);
        market.getMarketData(silver).setAmountInMarket(10);
        market.getMarketData(rum).update();
        market.getMarketData(silver).update();

        List<AbstractGoods> batch = new ArrayList<>();
        batch.add(new AbstractGoods(rum, 30));     // Sale
        batch.add(new AbstractGoods(silver, -20)); // Purchase

        marketWas.addAll(batch);
        boolean fired = marketWas.fireChanges();

        assertTrue("fireChanges should return true when transactions exist", fired);
        assertEquals("Should log exactly one sale", 1, listener.saleCount);
        assertEquals("Should log exactly one purchase", 1, listener.purchaseCount);
    }

    public void testTransactionLogging() {
        Game game = getStandardGame();
        Player player = getHumanPlayer(game);
        Market market = player.getMarket();
        GoodsType silver = spec().getGoodsType("model.goods.silver");

        MockTransactionListener listener = new MockTransactionListener();
        market.addTransactionListener(listener);

        MarketWas marketWas = new MarketWas(player);
        market.getMarketData(silver).setAmountInMarket(10);
        market.getMarketData(silver).update();

        marketWas.add(new AbstractGoods(silver, -50));
        marketWas.fireChanges();

        assertEquals("Should have logged one purchase", 1, listener.purchaseCount);
        assertEquals("Should have logged Silver as the last type", silver, listener.lastType);
    }

    public void testMultipleTransactions() {
        Game game = getStandardGame();
        Player player = getHumanPlayer(game);
        Market market = player.getMarket();
        GoodsType rum = spec().getGoodsType("model.goods.rum");
        GoodsType silver = spec().getGoodsType("model.goods.silver");

        MockTransactionListener listener = new MockTransactionListener();
        market.addTransactionListener(listener);

        MarketWas marketWas = new MarketWas(player);
        market.getMarketData(rum).setAmountInMarket(1000);
        market.getMarketData(silver).setAmountInMarket(10);
        market.getMarketData(rum).update();
        market.getMarketData(silver).update();

        marketWas.add(new AbstractGoods(rum, 30));
        marketWas.add(new AbstractGoods(silver, -20));
        marketWas.fireChanges();

        assertEquals("Strict check: Expecting exactly 1 sale", 1, listener.saleCount);
        assertEquals("Strict check: Expecting exactly 1 purchase", 1, listener.purchaseCount);
    }

    public void testNoTransactions() {
        Game game = getStandardGame();
        Player player = getHumanPlayer(game);
        MarketWas marketWas = new MarketWas(player);
        assertFalse("fireChanges should be false if no transactions were recorded", 
            marketWas.fireChanges());
    }

    /**
     * Mock Listener to capture transaction data for verification.
     */
    private static class MockTransactionListener implements TransactionListener {
        public int purchaseCount = 0;
        public int saleCount = 0;
        public int lastTax = -1;
        public GoodsType lastType;

        @Override
        public void logPurchase(GoodsType t, int a, int p) {
            purchaseCount++;
            lastType = t;
        }

        @Override
        public void logSale(GoodsType t, int a, int p, int tax) {
            saleCount++;
            lastType = t;
            lastTax = tax;
        }
    }
}
