/**
 *  Copyright (C) 2002-2024   The FreeCol Team
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
import java.util.List;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.util.test.FreeColTestCase;


public class MarketTest extends FreeColTestCase {

    /**
     * Make sure that the initial prices are correctly taken from the
     * specification
     */
    public void testInitialMarket() {

        Game g = getStandardGame();

        Player p = g.getPlayerByNationId("model.nation.dutch");

        Market dm = p.getMarket();

        Specification s = spec();
        

        for (GoodsType good : s.getStorableGoodsTypeList()) {
            assertEquals(good.toString(), good.getInitialBuyPrice(), dm.getCostToBuy(good));
            assertEquals(good.toString(), good.getInitialSellPrice(), dm.getPaidForSale(good));
        }
    }

    public void testEuropeMarketPricing() {
        final Game g = getStandardGame();
        final Player p = g.getPlayerByNationId("model.nation.dutch");
        final Specification s = spec();
        final Europe eu = p.getEurope();

        for (GoodsType good : s.getGoodsTypeList()) {
            List<AbstractGoods> goods = new ArrayList<>();
            assertEquals(p.getMarket().getSalePrice(good, 1), eu.getOwner().getMarket().getSalePrice(good, 1));
            
            goods.add(new AbstractGoods(good, 1));
            int bidPrice = p.getMarket().getBidPrice(good, 1);
            int buyCost = p.getMarket().getCostToBuy(good);
            
            assertEquals(buyCost, bidPrice);
            try {
                int priceGoods = eu.priceGoods(goods);
                assertEquals(buyCost, priceGoods);
            } catch (FreeColException fce) {
                fail(fce.getMessage());
            }
        }
    }

    public void testPriceFluctuations() {
        Game g = getStandardGame();
        Player p = g.getPlayerByNationId("model.nation.dutch");
        Market dm = p.getMarket();
        GoodsType rum = spec().getGoodsType("model.goods.rum");

        int priceBefore = dm.getPaidForSale(rum);

        dm.addGoodsToMarket(rum, 5000);

        int priceAfter = dm.getPaidForSale(rum);
        assertTrue("Price of rum should decrease after selling 5000 units", priceAfter < priceBefore);

        dm.addGoodsToMarket(rum, 100000);
        assertTrue("Price should not drop below 1", dm.getPaidForSale(rum) >= 1);
    }

    public void testIncomeAndArrears() {
        Game g = getStandardGame();
        Player p = g.getPlayerByNationId("model.nation.dutch");
        Market dm = p.getMarket();
        GoodsType cigars = spec().getGoodsType("model.goods.cigars");

        dm.modifyIncomeBeforeTaxes(cigars, 1000);
        dm.modifyIncomeAfterTaxes(cigars, 850);
        assertEquals(1000, dm.getIncomeBeforeTaxes(cigars));
        assertEquals(850, dm.getIncomeAfterTaxes(cigars));

        assertEquals(0, dm.getArrears(cigars));
        dm.setArrears(cigars, 500);
        assertEquals("Player should owe 500 in arrears for cigars", 500, dm.getArrears(cigars));
    }

    public void testPriceIncrease() {
        Game g = getStandardGame();
        Market dm = g.getPlayerByNationId("model.nation.dutch").getMarket();
        GoodsType cloth = spec().getGoodsType("model.goods.cloth");

        int before = dm.getCostToBuy(cloth);

        dm.addGoodsToMarket(cloth, -5000);
        dm.update(cloth);

        int after = dm.getCostToBuy(cloth);
        assertTrue("Price should increase when supply is reduced", after >= before);
    }

    public void testPriceChangeTracking() {
        Game g = getStandardGame();
        Market dm = g.getPlayerByNationId("model.nation.dutch").getMarket();
        GoodsType tools = spec().getGoodsType("model.goods.tools");

        int oldPrice = dm.getCostToBuy(tools);

        dm.addGoodsToMarket(tools, 5000);
        dm.update(tools);

        int newPrice = dm.getCostToBuy(tools);

        assertEquals("Price change flag should match actual price change",
                oldPrice != newPrice, dm.hasPriceChanged(tools));

        dm.flushPriceChange(tools);
        assertFalse("Price change flag should clear after flush", dm.hasPriceChanged(tools));
    }

    public void testMarketUpdate() {
        Game g = getStandardGame();
        Player p = g.getPlayerByNationId("model.nation.dutch");
        Market dm = p.getMarket();
        GoodsType ore = spec().getGoodsType("model.goods.ore");

        int before = dm.getAmountInMarket(ore);

        dm.addGoodsToMarket(ore, 200);
        dm.update(ore);

        int after = dm.getAmountInMarket(ore);

        assertTrue("Market update should not reduce amount below minimum", after >= Market.MINIMUM_AMOUNT);
        assertTrue("Market amount should reflect added goods", after > before);
    }

    public void testCopyIn() {
        Game g = getStandardGame();
        Player p = g.getPlayerByNationId("model.nation.dutch");
        Market original = p.getMarket();
        GoodsType furs = spec().getGoodsType("model.goods.furs");

        original.setArrears(furs, 300);

        Market copy = new Market(g, p);
        copy.copyIn(original);

        assertEquals("Arrears should not be copied", 0, copy.getArrears(furs));
        assertEquals("Price should be copied", original.getCostToBuy(furs), copy.getCostToBuy(furs));
    }

    public void testMarketSerialization() throws Exception {
        Game game = getStandardGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Market market = dutch.getMarket();
        GoodsType silver = spec().getGoodsType("model.goods.silver");

        market.setArrears(silver, 500);
        market.addGoodsToMarket(silver, 1000);
        int expectedPrice = market.getCostToBuy(silver);
        String marketId = market.getId();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(out, FreeColXMLWriter.WriteScope.toSave(), false)) {
            xw.writeStartElement(market.getXMLTagName());
            market.writeAttributes(xw);
            market.writeChildren(xw);
            xw.writeEndElement();
        }

        market.dispose();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        Market market2;
        try (FreeColXMLReader xr = new FreeColXMLReader(in)) {
            xr.nextTag();
            market2 = new Market(game, marketId);
            market2.readFromXML(xr);
        }

        assertEquals("Arrears should be preserved", 500, market2.getArrears(silver));
        assertEquals("Price should be preserved", expectedPrice, market2.getCostToBuy(silver));
    }

    /**
     * Placeholder for transaction listener tests.
     */
    public void testTransactionListeners() {
        // TODO: Implement transaction listener tests
    }
}
