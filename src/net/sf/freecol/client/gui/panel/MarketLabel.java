/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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


package net.sf.freecol.client.gui.panel;


import java.awt.Graphics;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Player;


/**
 * This label represents a cargo type on the European market.
 */
public final class MarketLabel extends AbstractGoodsLabel {

    private static Logger logger = Logger.getLogger(MarketLabel.class.getName());

    private final Market market;

    /**
     * Initializes this JLabel with the given goods type.
     * @param type The Goods type that this JLabel will visually represent.
     * @param market The <code>Market</code> being used to buy
     *       and sell <code>Goods</code>.
     * @param parent The parent that knows more than we do.
     */
    public MarketLabel(GoodsType type, Market market, Canvas parent) {
        super(new AbstractGoods(type, GoodsContainer.CARGO_SIZE), parent);
        if (market == null) {
            throw new NullPointerException();
        }

        this.market = market;
    }

    /**
     * Returns this MarketLabel's market.
     * @return This MarketLabel's market.
     */
    public Market getMarket() {
        return market;
    }

    /**
     * Sets this MarketLabel's goods amount.
     * @param amount The amount of goods.
     */
    public void setAmount(int amount) {
        getGoods().setAmount(amount);
    }

    /**
     * Paints this MarketLabel.
     * @param g The graphics context in which to do the painting.
     */
    @Override
    public void paintComponent(Graphics g) {

        Player player = market.getGame().getViewOwner();
        String toolTipText = Messages.message(getType().getNameKey());
        if (player == null || player.canTrade(getType())) {
            setEnabled(true);
        } else {
            toolTipText = Messages.message(getType().getLabel(false));
            setEnabled(false);
        }
        if (FreeCol.isInDebugMode()) {
            toolTipText += " " + market.getAmountInMarket(getType());
        }
        setToolTipText(toolTipText);

        super.setText(Integer.toString(market.getPaidForSale(getType()))
                      + "/" + Integer.toString(market.getCostToBuy(getType())));
        super.paintComponent(g);
    }

}

