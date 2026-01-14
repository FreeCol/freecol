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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.util.test.FreeColTestCase;

/**
 * Comprehensive test suite for MarketData logic, economic shifts, 
 * and model persistence.
 */
public class MarketDataTest extends FreeColTestCase {

    public void testInitialization() {
        Game game = getStandardGame();
        GoodsType rum = spec().getGoodsType("model.goods.rum");
        MarketData data = new MarketData(game, rum);
        
        assertEquals("Initial buy price mismatch", 
                     rum.getInitialBuyPrice(), data.getCostToBuy());
        assertEquals("Initial sell price mismatch", 
                     rum.getInitialSellPrice(), data.getPaidForSale());
        assertFalse("Traded status should be false by default", data.getTraded());
    }

    public void testPriceIncrease() {
        Game game = getStandardGame();
        GoodsType silver = spec().getGoodsType("model.goods.silver");
        MarketData data = new MarketData(game, silver);
        int initialBuyPrice = data.getCostToBuy();

        data.setAmountInMarket(Math.max(1, data.getAmountInMarket() / 10));
        data.update();

        assertTrue("Price should rise when supply is scarce", 
                   data.getCostToBuy() > initialBuyPrice);
    }

    public void testPriceDecrease() {
        Game game = getStandardGame();
        GoodsType silver = spec().getGoodsType("model.goods.silver");
        MarketData data = new MarketData(game, silver);
        int initialBuyPrice = data.getCostToBuy();

        // Flood the market
        data.setAmountInMarket(data.getAmountInMarket() * 5);
        data.update(); 

        assertTrue("Price should drop when supply increases", 
                   data.getCostToBuy() < initialBuyPrice);
    }

    public void testPriceChangePropertyEvent() {
        Game game = getStandardGame();
        GoodsType rum = spec().getGoodsType("model.goods.rum");
        MarketData data = new MarketData(game, rum);
        
        final List<PropertyChangeEvent> events = new ArrayList<>();
        data.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                events.add(evt);
            }
        });

        int initialAmount = data.getAmountInMarket();
        data.setAmountInMarket(initialAmount + 5000);
        data.price(); // Triggers event notification logic

        assertFalse("MarketData should fire PropertyChangeEvents upon price shifts", 
                    events.isEmpty());
    }

    public void testNonStorableGoodsNoPriceChange() {
        Game game = getStandardGame();
        GoodsType bells = spec().getGoodsType("model.goods.bells"); 
        MarketData data = new MarketData(game, bells);

        if (!bells.isStorable()) {
            data.setAmountInMarket(1);
            boolean changed = data.price();
            assertFalse("Non-storable goods should never trigger price changes", changed);
        }
    }

    public void testPriceClamping() {
        Game game = getStandardGame();
        GoodsType cigars = spec().getGoodsType("model.goods.cigars");
        MarketData data = new MarketData(game, cigars);

        int startPrice = data.getCostToBuy();
        int diff = cigars.getPriceDifference();

        data.setAmountInMarket(10000000); 
        data.price(); 

        assertEquals("Price fall should be limited by the goods priceDifference", 
                     startPrice - diff, data.getCostToBuy());
    }

    public void testCopyIn() {
        Game game = getStandardGame();
        GoodsType ore = spec().getGoodsType("model.goods.ore");

        MarketData original = new MarketData(game, ore);
        original.setArrears(123);
        original.setAmountInMarket(999);
        original.setTraded(true);

        Game game2 = new Game(spec()); 
        MarketData copy = new MarketData(game2, original.getId());
        
        assertTrue("copyIn should return true when IDs match", copy.copyIn(original));
        assertEquals("Arrears mismatch after copyIn", original.getArrears(), copy.getArrears());
        assertEquals("Market amount mismatch after copyIn", 
                     original.getAmountInMarket(), copy.getAmountInMarket());
        assertEquals("Traded status mismatch after copyIn", 
                     original.getTraded(), copy.getTraded());
    }

    public void testXMLRoundTrip() throws Exception {
        Game game = getStandardGame();
        GoodsType rum = spec().getGoodsType("model.goods.rum");
        MarketData original = new MarketData(game, rum);
        original.setArrears(42);
        original.setSales(100);
        original.setIncomeBeforeTaxes(500);
        original.setTraded(true);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(out, FreeColXMLWriter.WriteScope.toSave(), false)) {
            original.toXML(xw);
        }

        String id = original.getId();
        original.dispose(); 

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        MarketData loaded = new MarketData(game, id);
        try (FreeColXMLReader xr = new FreeColXMLReader(in)) {
            xr.nextTag();
            loaded.readFromXML(xr);
        }

        assertEquals("Arrears lost in XML", 42, loaded.getArrears());
        assertEquals("Sales lost in XML", 100, loaded.getSales());
        assertEquals("Income lost in XML", 500, loaded.getIncomeBeforeTaxes());
        assertTrue("Traded status lost in XML", loaded.getTraded());
    }
}
