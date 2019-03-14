/**
 * Copyright (C) 2002-2019   The FreeCol Team
 *
 * This file is part of FreeCol.
 *
 * FreeCol is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * FreeCol is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.client.gui.label;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JLabel;

import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.panel.CargoPanel;
import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.debug.FreeColDebugger.DebugMode;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;


/**
 * This label represents a cargo type on the European market.
 */
public final class MarketLabel extends AbstractGoodsLabel
        implements CargoLabel, Draggable, PropertyChangeListener {

    /** The enclosing market. */
    private final Market market;


    /**
     * Initializes this FreeColLabel with the given goods type.
     *
     * @param lib The {@code ImageLibrary} to display with.
     * @param type The {@code GoodsType} to represent.
     * @param market The {@code Market} in which to trade the goods.
     */
    public MarketLabel(ImageLibrary lib, GoodsType type, Market market) {
        super(lib, new AbstractGoods(type, GoodsContainer.CARGO_SIZE));

        if (market == null) {
            throw new RuntimeException("Null market for " + this);
        }
        this.market = market;
        update();
    }


    /**
     * Update this label.
     */
    private void update() {
        final GoodsType type = getType();
        final Player player = market.getOwner();
        String toolTipText = Messages.getName(type);
        if (player == null || player.canTrade(type)) {
            setEnabled(true);
        } else {
            toolTipText = Messages.message(type.getLabel());
            setEnabled(false);
        }
        if (FreeColDebugger.isInDebugMode(DebugMode.MENUS)) {
            toolTipText += " " + market.getAmountInMarket(type);
        }
        setToolTipText(toolTipText);

        setText(market.getPaidForSale(type) + "/" + market.getCostToBuy(type));
    }


    /**
     * Wrap the label with a border.
     *
     * @return This {@code MarketLabel}.
     */
    public MarketLabel addBorder() {
        setBorder(Utility.TOPCELLBORDER);
        setVerticalTextPosition(JLabel.BOTTOM);
        setHorizontalTextPosition(JLabel.CENTER);
        return this;
    }


    /**
     * Get this MarketLabel's market.
     *
     * @return The enclosing {@code Market}.
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


    // Interface CargoLabel

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addCargo(Component comp, Unit carrier, CargoPanel cargoPanel) {
        MarketLabel label = (MarketLabel) comp;
        Player player = carrier.getOwner();
        if (!player.canTrade(label.getType())) {
            cargoPanel.igc().payArrears(label.getType());
            return false;
        }
        int loadable = carrier.getLoadableAmount(label.getType());
        if (loadable <= 0) return false;
        if (loadable > label.getAmount()) loadable = label.getAmount();
        cargoPanel.igc().buyGoods(label.getType(), loadable, carrier);
        cargoPanel.igc().nextModelMessage();
        cargoPanel.update();
        return true;
    }
}
