
package net.sf.freecol.client.gui.panel;


import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import net.sf.freecol.common.model.Market;
import net.sf.freecol.client.gui.Canvas;


/**
 * This label represents a cargo type on the European market.
 */
public final class MarketLabel extends JLabel implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static Logger logger = Logger.getLogger(MarketLabel.class.getName());

    private final int type;
    private final Market market;
    private final Canvas parent;

    /**
    * Initializes this JLabel with the given goods type.
    * @param goods The Goods type that this JLabel will visually represent.
    * @param parent The parent that knows more than we do.
    */
    public MarketLabel(int type, Market market, Canvas parent) {
        super(parent.getImageProvider().getGoodsImageIcon(type));
        
        this.type = type;
        
        if (market == null) {
            throw new NullPointerException();
        }

        this.market = market;
        this.parent = parent;
    }

    
    /**
    * Initializes this JLabel with the given goods type.
    * @param goods The Goods type that this JLabel will visually represent.
    * @param parent The parent that knows more than we do.
    */
    public MarketLabel(int type, Market market, Canvas parent, boolean isSmall) {
        this(type, market, parent);
        setSmall(true);
    }


    /**
    * Returns this MarketLabel's goods type.
    * @return This MarketLabel's goods type.
    */
    public int getType() {
        return type;
    }

    /**
    * Returns this MarketLabel's market.
    * @return This MarketLabel's market.
    */
    public Market getMarket() {
        return market;
    }

    /**
    * Makes a smaller version
    */
    public void setSmall(boolean isSmall) {
        if (isSmall) {
            ImageIcon imageIcon = (parent.getImageProvider().getGoodsImageIcon(type));
            setIcon(new ImageIcon(imageIcon.getImage().getScaledInstance(imageIcon.getIconWidth() / 2, imageIcon.getIconHeight() / 2, Image.SCALE_DEFAULT)));
        } else {
            setIcon(parent.getImageProvider().getGoodsImageIcon(type));
        }
    }


    /**
    * Paints this MarketLabel.
    * @param g The graphics context in which to do the painting.
    */
    public void paintComponent(Graphics g) {
        setEnabled(true);

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
                setIcon(parent.getImageProvider().getGoodsImageIcon(type));
                repaint(0, 0, getWidth(), getHeight());
                
                // TODO: Refresh the gold label when goods have prices.
                //goldLabel.repaint(0, 0, goldLabel.getWidth(), goldLabel.getHeight());
        }
        catch (NumberFormatException e) {
            logger.warning("Invalid action number");
        }
    }
}
