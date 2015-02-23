/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.MarketData;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.TransactionListener;


/**
 * Helper container to remember the Market state prior to some
 * change, and fire off any consequent property changes.
 */
public class MarketWas {

    private static final Logger logger = Logger.getLogger(MarketWas.class.getName());

    private final Market market;
    private final int tax;
    private final Map<GoodsType, Integer> costToBuy = new HashMap<>();
    private final Map<GoodsType, Integer> paidForSale = new HashMap<>();


    /**
     * Make a new MarketWas instance for the given player.
     *
     * @param player The <code>Player</code> to find a market for.
     */
    public MarketWas(Player player) {
        this.market = player.getMarket();
        this.tax = player.getTax();
        for (MarketData md : this.market.getMarketData()) {
            this.costToBuy.put(md.getGoodsType(), md.getCostToBuy());
            this.paidForSale.put(md.getGoodsType(), md.getPaidForSale());
        }
    }


    /**
     * Fire any property changes resulting from actions in Market.
     *
     * @param req A list of <code>AbstractGoods</code> that changed hands.
     */
    public void fireChanges(List<AbstractGoods> req) {
        for (AbstractGoods ag : req) fireChanges(ag.getType(), ag.getAmount());
    }

    /**
     * Fire any property changes resulting from actions in Market.
     *
     * @param type A <code>GoodsType</code> that changed hands.
     * @param amount The amount of goods that changed hands.
     */
    public void fireChanges(GoodsType type, int amount) {
        for (TransactionListener l : this.market.getTransactionListener()) {
            if (amount > 0) {
                int buy = (this.costToBuy.containsKey(type))
                    ? this.costToBuy.get(type)
                    : this.market.getCostToBuy(type);
                l.logPurchase(type, amount, buy);
            } else if (amount < 0) {
                int sell = (this.paidForSale.containsKey(type))
                    ? this.paidForSale.get(type)
                    : this.market.getPaidForSale(type);
                l.logSale(type, -amount, sell, this.tax);
            }
        }
    }
}
