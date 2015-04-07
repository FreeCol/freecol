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

package net.sf.freecol.client.gui.panel;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JLabel;

import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Player;


/**
 * This label represents a cargo type on the European market.
 */
public final class MarketLabel extends AbstractGoodsLabel
    implements Draggable, PropertyChangeListener {

    /** The enclosing market. */
    private final Market market;


    /**
     * Initializes this JLabel with the given goods type.
     *
     * @param type The <code>GoodsType</code> to represent.
     * @param market The <code>Market</code> in which to trade the goods.
     */
    public MarketLabel(ImageLibrary lib, GoodsType type, Market market) {
        super(lib, new AbstractGoods(type, GoodsContainer.CARGO_SIZE));

        if (market == null) throw new IllegalArgumentException("Null market");
        this.market = market;
        update();
    }


    /**
     * Wrap the label with a border.
     *
     * @return This <code>MarketLabel</code>.
     */
    public MarketLabel addBorder() {
        setBorder(Utility.TOPCELLBORDER);
        setVerticalTextPosition(JLabel.BOTTOM);
        setHorizontalTextPosition(JLabel.CENTER);
        return this;
    }

    /**
     * Update this label.
     */
    public void update() {
        final GoodsType type = getType();
        final Player player = market.getOwner();
        String toolTipText = Messages.getName(type);
        if (player == null || player.canTrade(type)) {
            setEnabled(true);
        } else {
            toolTipText = Messages.message(type.getLabel());
            setEnabled(false);
        }
        if (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.MENUS)) {
            toolTipText += " " + market.getAmountInMarket(type);
        }
        setToolTipText(toolTipText);
        
        setText(market.getPaidForSale(type) + "/" + market.getCostToBuy(type));
    }

    /**
     * Get this MarketLabel's market.
     *
     * @return The enclosing <code>Market</code>.
     */
    public Market getMarket() {
        return market;
    }

    /**
     * Sets the amount of the goods wrapped by this Label to
     * GoodsContainer.CARGO_SIZE.
     */
    @Override
    public void setDefaultAmount() {
        setAmount(GoodsContainer.CARGO_SIZE);
    }


    // Implement Draggable

    /**
     * Is this label on a carrier?  No, it is in a market!
     *
     * @return False.
     */
    @Override
    public boolean isOnCarrier() {
        return false;
    }


    // Interface PropertyChangeListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void propertyChange(PropertyChangeEvent event) {
        update(); // Just update the text and tool tip.
    }
}
