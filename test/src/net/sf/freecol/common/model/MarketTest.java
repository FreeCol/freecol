package net.sf.freecol.common.model;

import net.sf.freecol.common.model.Specification;
import net.sf.freecol.util.test.FreeColTestCase;

public class MarketTest extends FreeColTestCase {

    public void testSellingMakesPricesFall() {
        Game g = getStandardGame();

        Player p = g.getPlayer("model.nation.dutch");

        Market dm = p.getMarket();

        Specification s = spec();

        GoodsType silver = s.getGoodsType("model.goods.silver");

        int previousGold = p.getGold();

        int price = silver.getInitialSellPrice();

        dm.sell(silver, 1000, p);

        assertEquals(previousGold + price * 1000, p.getGold());

        assertTrue(dm.getSalePrice(silver, 1) < price);
    }

    public void testBuyingMakesPricesRaise() {
        Game g = getStandardGame();

        Player p = g.getPlayer("model.nation.dutch");

        Market dm = p.getMarket();

        Specification s = spec();

        GoodsType food = s.getGoodsType("model.goods.food");

        p.modifyGold(1000000);

        int price = food.getInitialBuyPrice();

        dm.buy(food, 10000, p);

        assertEquals(1000000 - 10000 * price, p.getGold());

        assertTrue(dm.getBidPrice(food, 1) > price);
    }

    /**
     * Helper Method for finding out how much of a good to sell until the price drops.
     */
    public int sellUntilPriceDrop(Game game, Player player, GoodsType type){

        int result = 0;
                
        Market market = player.getMarket();

        int price = market.getSalePrice(type, 1);
                
        if (price == 0)
            throw new IllegalArgumentException("Price is already 0 for selling " + type);
                
        while (price == market.getSalePrice(type, 1)){
            market.sell(type, 10, player);
            result++;
        }
        return result;
    }
        
    /*
     * Helper method for finding out how much to buy of a good before the prices
     * rises.
     */
    public int buyUntilPriceRise(Game game, Player player, GoodsType type) {

        int result = 0;

        Market market = player.getMarket();

        int price = market.getBidPrice(type, 1);

        if (price == 20)
            throw new IllegalArgumentException("Price is already 20 for buying " + type);

        while (price == market.getBidPrice(type, 1)) {
            market.buy(type, 10, player);
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
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        assertEquals("model.nationType.trade", dutch.getNationType().getId());
        assertFalse(dutch.getNationType().getFeatureContainer()
                    .getModifierSet("model.modifier.tradeBonus").isEmpty());
        assertFalse(dutch.getFeatureContainer().getModifierSet("model.modifier.tradeBonus").isEmpty());

        {// Test that the dutch can sell more goods until the price drops
            GoodsType silver = spec().getGoodsType("model.goods.silver");
            int dutchSellAmount = sellUntilPriceDrop(game, dutch, silver);

            Game g2 = getStandardGame();
            int frenchSellAmount = sellUntilPriceDrop(g2, g2
                                                      .getPlayer("model.nation.french"), silver);

            assertTrue(dutchSellAmount > frenchSellAmount);
        }
        {// Test that the dutch can buy more goods until the price rises
            GoodsType muskets = spec().getGoodsType("model.goods.muskets");
            dutch.modifyGold(10000);
            french.modifyGold(10000);
            int dutchBuyAmount = buyUntilPriceRise(getStandardGame(), dutch, muskets);

            int frenchBuyAmount = buyUntilPriceRise(getStandardGame(), french, muskets);

            assertTrue(dutchBuyAmount > frenchBuyAmount);
        }
    }

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
                assertEquals(good.toString(), good.getInitialBuyPrice(), dm.costToBuy(good));
                assertEquals(good.toString(), good.getInitialSellPrice(), dm.paidForSale(good));
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
