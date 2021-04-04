/**
 *  Copyright (C) 2002-2021   The FreeCol Team
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
import net.sf.freecol.common.model.Specification;
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
            List<AbstractGoods> goods = new ArrayList<AbstractGoods>();
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

    /**
     * Serialization and deserialization?
     */
    public void testSerialization() {
        //fail();
    }

    /**
     * Do the transaction listeners work?
     */
    public void testTransactionListeners() {
        //fail("Not yet implemented");
    }
}
