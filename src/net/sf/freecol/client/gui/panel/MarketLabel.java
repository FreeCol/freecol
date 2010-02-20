/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Player;


/**
 * This label represents a cargo type on the European market.
 */
public final class MarketLabel extends JLabel implements ActionListener {

    private static Logger logger = Logger.getLogger(MarketLabel.class.getName());

    private final GoodsType type;
    private int amount;
    private final Market market;
    private final Canvas parent;
    private boolean partialChosen;
    private boolean toEquip;

    /**
    * Initializes this JLabel with the given goods type.
    * @param type The Goods type that this JLabel will visually represent.
    * @param market The <code>Market</code> being used to buy
    *       and sell <code>Goods</code>.
    * @param parent The parent that knows more than we do.
    */
    public MarketLabel(GoodsType type, Market market, Canvas parent) {
        super(parent.getImageLibrary().getGoodsImageIcon(type));
        
        this.type = type;
        /*
        if (FreeCol.isInDebugMode()) {
            setToolTipText(type.getName() + " " + market.getMarketData(type).getAmountInMarket());
        } else {
            setToolTipText(type.getName());
        }
        */
        if (market == null) {
            throw new NullPointerException();
        }

        this.market = market;
        this.parent = parent;
        partialChosen = false;
        amount = 100;
    }


    /**
    * Initializes this JLabel with the given goods type.
    * @param type The Goods type that this JLabel will visually represent.
    * @param market The <code>Market</code> being used to buy
    *       and sell <code>Goods</code>.
    * @param parent The parent that knows more than we do.
    * @param isSmall The image will be smaller if set to <code>true</code>.
    */
    public MarketLabel(GoodsType type, Market market, Canvas parent, boolean isSmall) {
        this(type, market, parent);
        setSmall(true);
    }


    public boolean isPartialChosen() {
        return partialChosen;
    }
    
    
    public void setPartialChosen(boolean partialChosen) {
        this.partialChosen = partialChosen;
    }
    
    public boolean isToEquip() {
        return toEquip;
    }
    
    public void toEquip(boolean toEquip) {
        this.toEquip = toEquip;
    }


    /**
    * Returns this MarketLabel's goods type.
    * @return This MarketLabel's goods type.
    */
    public GoodsType getType() {
        return type;
    }

    /**
    * Returns this MarketLabel's goods amount.
    * @return This MarketLabel's goods amount.
    */
    public int getAmount() {
        return amount;
    }

    /**
    * Sets this MarketLabel's goods amount.
    * @param amount The amount of goods.
    */
    public void setAmount(int amount) {
        this.amount = amount;
    }
    

    /**
    * Returns this MarketLabel's market.
    * @return This MarketLabel's market.
    */
    public Market getMarket() {
        return market;
    }

    /**
    * Makes a smaller version.
    * @param isSmall The image will be smaller if set to <code>true</code>.
    */
    public void setSmall(boolean isSmall) {
        if (isSmall) {
            ImageIcon imageIcon = parent.getImageLibrary().getGoodsImageIcon(type);
            setIcon(new ImageIcon(imageIcon.getImage().getScaledInstance(imageIcon.getIconWidth() / 2, imageIcon.getIconHeight() / 2, Image.SCALE_DEFAULT)));
        } else {
            setIcon(parent.getImageLibrary().getGoodsImageIcon(type));
        }
    }


    /**
    * Paints this MarketLabel.
    * @param g The graphics context in which to do the painting.
    */
    public void paintComponent(Graphics g) {

        Player player = market.getGame().getViewOwner();
        String toolTipText = Messages.message(type.getNameKey());
        if (player == null || player.canTrade(type)) {
            setEnabled(true);
        } else {
            toolTipText = Messages.message(type.getLabel(false));
            setEnabled(false);
        }
        if (FreeCol.isInDebugMode()) {
            toolTipText += " " + market.getMarketData(type).getAmountInMarket();
        }
        setToolTipText(toolTipText);

        super.setText(Integer.toString(market.paidForSale(type)) + "/" + Integer.toString(market.costToBuy(type)));
        super.paintComponent(g);
    }

    /**
    * Analyzes an event and calls the right external methods to take
    * care of the user's request.
    * @param event The incoming action event
    */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        try {
                switch (Integer.valueOf(command).intValue()) {
                    default:
                        logger.warning("Invalid action");
                }
                setIcon(parent.getImageLibrary().getGoodsImageIcon(type));
                repaint(0, 0, getWidth(), getHeight());
                
                // TODO: Refresh the gold label when goods have prices.
                //goldLabel.repaint(0, 0, goldLabel.getWidth(), goldLabel.getHeight());
        }
        catch (NumberFormatException e) {
            logger.warning("Invalid action number");
        }
    }
}

