
package net.sf.freecol.client.gui.panel;


import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.client.gui.Canvas;


/**
 * This label holds Unit data in addition to the JLabel data, which makes
 * it ideal to use for drag and drop purposes.
 */
public final class UnitLabel extends JLabel implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static Logger logger = Logger.getLogger(UnitLabel.class.getName());

    public static final int ARM = 0,
                            MOUNT = 1,
                            TOOLS = 2,
                            DRESS = 3;

    private final Unit unit;
    private final Canvas parent;
    private boolean selected;

    /**
    * Initializes this JLabel with the given unit data.
    * @param unit The Unit that this JLabel will visually represent.
    * @param parent The parent that knows more than we do.
    */
    public UnitLabel(Unit unit, Canvas parent) {
        super(parent.getImageProvider().getUnitImageIcon(parent.getImageProvider().getUnitGraphicsType(unit)));
        this.unit = unit;
        this.parent = parent;
        selected = false;
    }

    
    /**
    * Initializes this JLabel with the given unit data.
    * @param unit The Unit that this JLabel will visually represent.
    * @param parent The parent that knows more than we do.
    */
    public UnitLabel(Unit unit, Canvas parent, boolean isSmall) {
        this(unit, parent);
        setSmall(true);
    }


    /**
    * Returns this UnitLabel's unit data.
    * @return This UnitLabel's unit data.
    */
    public Unit getUnit() {
        return unit;
    }

    
    /**
    * Sets whether or not this unit should be selected.
    * @param b Whether or not this unit should be selected.
    */
    public void setSelected(boolean b) {
        selected = b;
    }


    /**
    * Makes a smaller version
    */
    public void setSmall(boolean isSmall) {
        if (isSmall) {
            ImageIcon imageIcon = (parent.getImageProvider().getUnitImageIcon(parent.getImageProvider().getUnitGraphicsType(unit)));
            setIcon(new ImageIcon(imageIcon.getImage().getScaledInstance(imageIcon.getIconWidth() / 2, imageIcon.getIconHeight() / 2, Image.SCALE_DEFAULT)));
        } else {
            setIcon(parent.getImageProvider().getUnitImageIcon(parent.getImageProvider().getUnitGraphicsType(unit)));
        }
    }


    /**
    * Paints this UnitLabel.
    * @param g The graphics context in which to do the painting.
    */
    public void paintComponent(Graphics g) {
        if (selected) {
            setEnabled(true);
        } else if (unit.isNaval()) {
            setEnabled(false);
        } else {
            setEnabled(true);
        }

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
            if (!unit.isCarrier()) {
                switch (Integer.valueOf(command).intValue()) {
                    case ARM:
                        //parent.trade(!unit.isArmed(), Goods.MUSKETS, unit, 50);
                        break;
                    case MOUNT:
                        //parent.trade(!unit.isMounted(), Goods.HORSES, unit, 50);
                        break;
                    case TOOLS:
                        //parent.trade(unit.getNumberOfTools() == 0, Goods.TOOLS, unit, 100);
                        break;
                    case DRESS:
                        //parent.dressAsMissionary(unit);
                        break;
                    default:
                        logger.warning("Invalid action");
                }
                setIcon(parent.getImageProvider().getUnitImageIcon(parent.getImageProvider().getUnitGraphicsType(unit)));
                repaint(0, 0, getWidth(), getHeight());
                
                // TODO: Refresh the gold label when goods have prices.
                //goldLabel.repaint(0, 0, goldLabel.getWidth(), goldLabel.getHeight());
            }
        }
        catch (NumberFormatException e) {
            logger.warning("Invalid action number");
        }
    }
}
