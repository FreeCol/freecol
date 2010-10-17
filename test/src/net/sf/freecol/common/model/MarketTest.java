package net.sf.freecol.common.model;

import net.sf.freecol.common.model.Specification;
import net.sf.freecol.util.test.FreeColTestCase;

public class MarketTest extends FreeColTestCase {

    /**
     * Make sure that the initial prices are correctly taken from the
     * specification
     */
    public void testInitialMarket() {

        Game g = getStandardGame();

        Player p = g.getPlayer("model.nation.dutch");

        Market dm = p.getMarket();

        Specification s = spec();

        for (GoodsType good : s.getGoodsTypeList()) {
            if (good.isStorable()) {
                assertEquals(good.toString(), good.getInitialBuyPrice(), dm.getCostToBuy(good));
                assertEquals(good.toString(), good.getInitialSellPrice(), dm.getPaidForSale(good));
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
