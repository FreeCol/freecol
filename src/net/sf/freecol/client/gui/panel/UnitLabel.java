
package net.sf.freecol.client.gui.panel;


import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.client.gui.Canvas;

import net.sf.freecol.client.control.InGameController;

/**
 * This label holds Unit data in addition to the JLabel data, which makes
 * it ideal to use for drag and drop purposes.
 */
public final class UnitLabel extends JLabel implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static Logger logger = Logger.getLogger(UnitLabel.class.getName());

    // The space between the top of this 'UnitLabel' and the top of the production images.
    //private static final int STP = 10;
    private static final int STP = 0;

    public static final int ARM = 0,
                            MOUNT = 1,
                            TOOLS = 2,
                            DRESS = 3,
                            WORKTYPE_FOOD = 4,
                            WORKTYPE_SUGAR = 5,
                            WORKTYPE_TOBACCO = 6,
                            WORKTYPE_COTTON = 7,
                            WORKTYPE_FURS = 8,
                            WORKTYPE_LUMBER = 9,
                            WORKTYPE_ORE = 10,
                            WORKTYPE_SILVER = 11,
                            CLEAR_SPECIALITY = 12;

    private final Unit unit;
    private final Canvas parent;
    private boolean selected;

    private InGameController inGameController;

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

        setSmall(false);

        this.inGameController = parent.getClient().getInGameController();
    }

    
    /**
    * Initializes this JLabel with the given unit data.
    * @param unit The Unit that this JLabel will visually represent.
    * @param parent The parent that knows more than we do.
    */
    public UnitLabel(Unit unit, Canvas parent, boolean isSmall) {
        this(unit, parent);
        setSmall(isSmall);
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
            setPreferredSize(null);
            ImageIcon imageIcon = (parent.getImageProvider().getUnitImageIcon(parent.getImageProvider().getUnitGraphicsType(unit)));
            setIcon(new ImageIcon(imageIcon.getImage().getScaledInstance(imageIcon.getIconWidth() / 2, imageIcon.getIconHeight() / 2, Image.SCALE_DEFAULT)));
        } else {
            if (unit.getLocation() instanceof ColonyTile) {
                setPreferredSize(new java.awt.Dimension(parent.getImageProvider().getTerrainImageWidth(0)*3/4, parent.getImageProvider().getUnitImageHeight(parent.getImageProvider().getUnitGraphicsType(unit))));
            } else {
                setPreferredSize(null);
            }

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
        } else if (unit.isCarrier()) {
            setEnabled(false);
        } else {
            setEnabled(true);
        }

        super.paintComponent(g);


        if (unit.getLocation() instanceof ColonyTile) {
            ImageIcon goodsIcon;

            if (unit.getWorkType() == Goods.FOOD && unit.getLocation() instanceof ColonyTile
                    && !((ColonyTile) unit.getLocation()).getWorkTile().isLand()) {
                goodsIcon = parent.getImageProvider().getGoodsImageIcon(Goods.FISH);
            } else {
                goodsIcon = parent.getImageProvider().getGoodsImageIcon(unit.getWorkType());
            }

            int production = unit.getFarmedPotential(unit.getWorkType(), ((ColonyTile) unit.getLocation()).getWorkTile());
            
            if (production > 0) {
                int p = getWidth() / production;
                if (p-goodsIcon.getIconWidth() < 0) {
                    p = (getWidth() - goodsIcon.getIconWidth()) / production;
                }

                if (production > 6) { // TODO: Or the user chooses it:
                    goodsIcon.paintIcon(this, g, getWidth()/2 - goodsIcon.getIconWidth()/2, STP);
                    BufferedImage stringImage = parent.getGUI().createStringImage((Graphics2D) g, Integer.toString(production), Color.WHITE, goodsIcon.getIconWidth()*2, 12);
                    g.drawImage(stringImage, getWidth()/2-stringImage.getWidth()/2, goodsIcon.getIconHeight()/2 - stringImage.getHeight()/2+STP, null);
                } else {
                    for (int i=0; i<production; i++) {
                        goodsIcon.paintIcon(this, g, i*p, STP);
                    }
                }

            } else {
                goodsIcon.paintIcon(this, g, getWidth()/2 - goodsIcon.getIconWidth()/2, STP);
                BufferedImage stringImage = parent.getGUI().createStringImage((Graphics2D) g, "0", Color.WHITE, goodsIcon.getIconWidth()*2, 12);
                g.drawImage(stringImage, getWidth()/2-stringImage.getWidth()/2, goodsIcon.getIconHeight()/2 - stringImage.getHeight()/2+STP, null);
            }
        }

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
                        inGameController.equipUnit(unit, Goods.MUSKETS, ((unit.isArmed()) ? 0 : 50));
                        break;
                    case MOUNT:
                        inGameController.equipUnit(unit, Goods.HORSES, ((unit.isMounted()) ? 0 : 50));
                        break;
                    case TOOLS:
                        inGameController.equipUnit(unit, Goods.TOOLS, ((unit.isPioneer()) ? 0 : 100));
                        break;
                    case DRESS:
                        inGameController.equipUnit(unit, Goods.CROSSES, ((unit.isMissionary()) ? 0 : 1));
                        break;
                    case WORKTYPE_FOOD:
                        inGameController.changeWorkType(unit, Goods.FOOD);
                        break;
                    case WORKTYPE_SUGAR:
                        inGameController.changeWorkType(unit, Goods.SUGAR);
                        break;
                    case WORKTYPE_TOBACCO:
                        inGameController.changeWorkType(unit, Goods.TOBACCO);
                        break;
                    case WORKTYPE_COTTON:
                        inGameController.changeWorkType(unit, Goods.COTTON);
                        break;
                    case WORKTYPE_FURS:
                        inGameController.changeWorkType(unit, Goods.FURS);
                        break;
                    case WORKTYPE_LUMBER:
                        inGameController.changeWorkType(unit, Goods.LUMBER);
                        break;
                    case WORKTYPE_ORE:
                        inGameController.changeWorkType(unit, Goods.ORE);
                        break;
                    case WORKTYPE_SILVER:
                        inGameController.changeWorkType(unit, Goods.SILVER);
                        break;
                    case CLEAR_SPECIALITY:
                        inGameController.clearSpeciality(unit);
                    default:
                        logger.warning("Invalid action");
                }
                setIcon(parent.getImageProvider().getUnitImageIcon(parent.getImageProvider().getUnitGraphicsType(unit)));

                Component uc = getParent();
                while (uc != null) {
                    if (uc instanceof ColonyPanel) {
                        if (unit.getTile() != null && unit.getTile().getColony() == null) {
                            parent.remove(uc);
                            parent.showMapControls();
                        } else {
                            ((ColonyPanel) uc).reinitialize();
                        }

                        break;
                    } else if (uc instanceof EuropePanel) {
                        //((EuropePanel) uc).reinitialize();
                        EuropePanel ep = (EuropePanel) uc;
                        ep.updateGoldLabel();
                        break;
                    }

                    uc = uc.getParent();
                }

                //repaint(0, 0, getWidth(), getHeight());
                //uc.refresh();
            }
        }
        catch (NumberFormatException e) {
            logger.warning("Invalid action number");
        }
    }
}
