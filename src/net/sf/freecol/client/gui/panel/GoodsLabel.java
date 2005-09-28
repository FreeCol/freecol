
package net.sf.freecol.client.gui.panel;


import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;


/**
 * This label holds Goods data in addition to the JLabel data, which makes
 * it ideal to use for drag and drop purposes.
 */
public final class GoodsLabel extends JLabel implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static Logger logger = Logger.getLogger(GoodsLabel.class.getName());

    private final Goods goods;
    private final Canvas parent;
    private boolean selected;
    private boolean partialChosen;

    /**
    * Initializes this JLabel with the given goods data.
    * @param goods The Goods that this JLabel will visually represent.
    * @param parent The parent that knows more than we do.
    */
    public GoodsLabel(Goods goods, Canvas parent) {
        super(parent.getImageProvider().getGoodsImageIcon(goods.getType()));
        this.goods = goods;
        setToolTipText(goods.getName());
        this.parent = parent;
        selected = false;
        partialChosen = false;
    }

    
    /**
    * Initializes this JLabel with the given goods data.
    * @param goods The Goods that this JLabel will visually represent.
    * @param parent The parent that knows more than we do.
    */
    public GoodsLabel(Goods goods, Canvas parent, boolean isSmall) {
        this(goods, parent);
        setSmall(true);
    }


    public boolean isPartialChosen() {
        return partialChosen;
    }
    
    
    public void setPartialChosen(boolean partialChosen) {
        this.partialChosen = partialChosen;
    }


    /**
    * Returns this GoodsLabel's goods data.
    * @return This GoodsLabel's goods data.
    */
    public Goods getGoods() {
        return goods;
    }

    
    /**
    * Sets whether or not this goods should be selected.
    * @param b Whether or not this goods should be selected.
    */
    public void setSelected(boolean b) {
        selected = b;
    }


    /**
    * Makes a smaller version
    */
    public void setSmall(boolean isSmall) {
        if (isSmall) {
            ImageIcon imageIcon = (parent.getImageProvider().getGoodsImageIcon(goods.getType()));
            setIcon(new ImageIcon(imageIcon.getImage().getScaledInstance(imageIcon.getIconWidth() / 2, imageIcon.getIconHeight() / 2, Image.SCALE_DEFAULT)));
        } else {
            setIcon(parent.getImageProvider().getGoodsImageIcon(goods.getType()));
        }
    }


    /**
    * Paints this GoodsLabel.
    * @param g The graphics context in which to do the painting.
    */
    public void paintComponent(Graphics g) {
        if (!getToolTipText().equals(goods.getName())) {
            setToolTipText(goods.getName());
        }

        if (goods.getType() != Goods.FOOD && goods.getLocation() instanceof Colony 
                && ((Colony) goods.getLocation()).getWarehouseCapacity() < goods.getAmount()) {
            setForeground(Color.RED);
        } else {
            setForeground(Color.BLACK);
        }

        super.setText(String.valueOf(goods.getAmount()));
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
                setIcon(parent.getImageProvider().getGoodsImageIcon(goods.getType()));
                repaint(0, 0, getWidth(), getHeight());
                
                // TODO: Refresh the gold label when goods have prices.
                //goldLabel.repaint(0, 0, goldLabel.getWidth(), goldLabel.getHeight());
        }
        catch (NumberFormatException e) {
            logger.warning("Invalid action number");
        }
    }
}
