
package net.sf.freecol.client.gui.panel;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.TransferHandler;

import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.ColonyTile;

/**
* A DragListener should be attached to Swing components that have a
* TransferHandler attached. The DragListener will make sure that the
* Swing component to which it is attached is draggable (moveable to be
* precise).
*/
public final class DragListener extends MouseAdapter {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    private static Logger logger = Logger.getLogger(DragListener.class.getName());
    
    private final JLayeredPane parentPanel;

    /**
    * The constructor to use.
    * @param parentPanel The layered pane that contains the components to which a
    * DragListener might be attached.
    */
    public DragListener(JLayeredPane parentPanel) {
        this.parentPanel = parentPanel;
    }
    
    /**
    * Gets called when the mouse was pressed on a Swing component that has this
    * object as a MouseListener.
    * @param e The event that holds the information about the mouse click.
    */
    public void mousePressed(MouseEvent e) {
        JComponent comp = (JComponent)e.getSource();

        //Does not work on some platforms:
        //if (e.isPopupTrigger() && (comp instanceof UnitLabel)) {
        if (e.getButton() == MouseEvent.BUTTON3 && (comp instanceof UnitLabel)) {
            UnitLabel unitLabel = (UnitLabel)comp;
            Unit tempUnit = unitLabel.getUnit();

            if (tempUnit.isColonist()) {
                JPopupMenu menu = new JPopupMenu("Unit");
                JMenuItem menuItem;

                if (tempUnit.getLocation() instanceof ColonyTile) {
                    ColonyTile colonyTile = (ColonyTile) tempUnit.getLocation();
                    menuItem = new JMenuItem("Be a Farmer (" + tempUnit.getFarmedPotential(Goods.FOOD, colonyTile.getWorkTile()) + "/"
                            + colonyTile.getColony().getVacantColonyTileProductionFor(tempUnit, Goods.FOOD) + ")");
                    menuItem.setActionCommand(String.valueOf(UnitLabel.WORKTYPE_FOOD));
                    menuItem.addActionListener(unitLabel);
                    menu.add(menuItem);
                    menuItem = new JMenuItem("Be a Sugar Planter (" + tempUnit.getFarmedPotential(Goods.SUGAR, colonyTile.getWorkTile()) + "/"
                            + colonyTile.getColony().getVacantColonyTileProductionFor(tempUnit, Goods.SUGAR) + ")");
                    menuItem.setActionCommand(String.valueOf(UnitLabel.WORKTYPE_SUGAR));
                    menuItem.addActionListener(unitLabel);
                    menu.add(menuItem);
                    menuItem = new JMenuItem("Be a Tobacco Planter (" + tempUnit.getFarmedPotential(Goods.TOBACCO, colonyTile.getWorkTile()) + "/"
                            + colonyTile.getColony().getVacantColonyTileProductionFor(tempUnit, Goods.TOBACCO) + ")");
                    menuItem.setActionCommand(String.valueOf(UnitLabel.WORKTYPE_TOBACCO));
                    menuItem.addActionListener(unitLabel);
                    menu.add(menuItem);
                    menuItem = new JMenuItem("Be a Cotton Planter (" + tempUnit.getFarmedPotential(Goods.COTTON, colonyTile.getWorkTile()) + "/"
                            + colonyTile.getColony().getVacantColonyTileProductionFor(tempUnit, Goods.COTTON) + ")");
                    menuItem.setActionCommand(String.valueOf(UnitLabel.WORKTYPE_COTTON));
                    menuItem.addActionListener(unitLabel);
                    menu.add(menuItem);
                    menuItem = new JMenuItem("Be a Fur Trapper (" + tempUnit.getFarmedPotential(Goods.FURS, colonyTile.getWorkTile()) + "/"
                            + colonyTile.getColony().getVacantColonyTileProductionFor(tempUnit, Goods.FURS) + ")");
                    menuItem.setActionCommand(String.valueOf(UnitLabel.WORKTYPE_FURS));
                    menuItem.addActionListener(unitLabel);
                    menu.add(menuItem);
                    menuItem = new JMenuItem("Be a Lumberjack (" + tempUnit.getFarmedPotential(Goods.LUMBER, colonyTile.getWorkTile()) + "/"
                            + colonyTile.getColony().getVacantColonyTileProductionFor(tempUnit, Goods.LUMBER) + ")");
                    menuItem.setActionCommand(String.valueOf(UnitLabel.WORKTYPE_LUMBER));
                    menuItem.addActionListener(unitLabel);
                    menu.add(menuItem);
                    menuItem = new JMenuItem("Be an Ore Miner (" + tempUnit.getFarmedPotential(Goods.ORE, colonyTile.getWorkTile()) + "/"
                            + colonyTile.getColony().getVacantColonyTileProductionFor(tempUnit, Goods.ORE) + ")");
                    menuItem.setActionCommand(String.valueOf(UnitLabel.WORKTYPE_ORE));
                    menuItem.addActionListener(unitLabel);
                    menu.add(menuItem);
                    menuItem = new JMenuItem("Be a Silver Miner (" + tempUnit.getFarmedPotential(Goods.SILVER, colonyTile.getWorkTile()) + "/"
                            + colonyTile.getColony().getVacantColonyTileProductionFor(tempUnit, Goods.SILVER) + ")");
                    menuItem.setActionCommand(String.valueOf(UnitLabel.WORKTYPE_SILVER));
                    menuItem.addActionListener(unitLabel);
                    menu.add(menuItem);

                    menu.addSeparator();
                }


                if (!tempUnit.isPioneer() && !tempUnit.isMissionary() && tempUnit.canArm()) {
                    if (tempUnit.isArmed()) {
                        menuItem = new JMenuItem("Disarm");
                    } else {
                        if (tempUnit.getLocation() instanceof Europe) {
                            menuItem = new JMenuItem("Arm (" + tempUnit.getGame().getMarket().getBidPrice(Goods.MUSKETS, 50) + " gold)");
                        } else {
                            menuItem = new JMenuItem("Arm");
                        }
                    }
                    menuItem.setActionCommand(String.valueOf(UnitLabel.ARM));
                    menuItem.addActionListener(unitLabel);
                    menu.add(menuItem);
                }

                if (!tempUnit.isPioneer() && !tempUnit.isMissionary() && tempUnit.canMount()) {
                    if (tempUnit.isMounted()) {
                        menuItem = new JMenuItem("Remove Horses");
                    } else {
                        if (tempUnit.getLocation() instanceof Europe) {
                            menuItem = new JMenuItem("Mount (" + tempUnit.getGame().getMarket().getBidPrice(Goods.HORSES, 50) + " gold)");
                        } else {
                            menuItem = new JMenuItem("Mount");
                        }
                    }
                    menuItem.setActionCommand(String.valueOf(UnitLabel.MOUNT));
                    menuItem.addActionListener(unitLabel);
                    menu.add(menuItem);
                }

                if (!tempUnit.isArmed() && !tempUnit.isMounted() && !tempUnit.isMissionary() && tempUnit.canEquipWithTools()) {
                    if (tempUnit.isPioneer()) {
                        menuItem = new JMenuItem("Remove Tools");
                    } else {
                        if (tempUnit.getLocation() instanceof Europe) {
                            int amount = 100;
                            int price = tempUnit.getGame().getMarket().getBidPrice(Goods.TOOLS, amount);
                            if (price <= tempUnit.getOwner().getGold()) {
                                menuItem = new JMenuItem("Equip with Tools (" + price + " gold)");
                            } else {
                                while (price > tempUnit.getOwner().getGold()) {
                                    amount -= 20;
                                    price = tempUnit.getGame().getMarket().getBidPrice(Goods.TOOLS, amount);
                                }
                                menuItem = new JMenuItem("Equip with " + amount + " Tools (" + price + " gold)");

                            }
                        } else {
                            menuItem = new JMenuItem("Equip with Tools");
                        }
                    }
                    menuItem.setActionCommand(String.valueOf(UnitLabel.TOOLS));
                    menuItem.addActionListener(unitLabel);
                    menu.add(menuItem);
                }

                if (!tempUnit.isArmed() && !tempUnit.isMounted() && !tempUnit.isPioneer() && tempUnit.canDressAsMissionary()) {

                    if (tempUnit.isMissionary()) {
                        menuItem = new JMenuItem("Take Off Silly Clothes");
                    } else {
                        menuItem = new JMenuItem("Dress as Missionaries");
                    }
                    menuItem.setActionCommand(String.valueOf(UnitLabel.DRESS));
                    menuItem.addActionListener(unitLabel);
                    menu.add(menuItem);
                }

                if (tempUnit.getType() != Unit.INDIAN_CONVERT && tempUnit.getType() != Unit.PETTY_CRIMINAL &&
                        tempUnit.getType() != Unit.INDENTURED_SERVANT && tempUnit.getType() != Unit.FREE_COLONIST) {

                    if (menu.getSubElements().length > 0) {
                        menu.addSeparator();
                    }

                    menuItem = new JMenuItem("Clear speciality");
                    menuItem.setActionCommand(String.valueOf(UnitLabel.CLEAR_SPECIALITY));
                    menuItem.addActionListener(unitLabel);
                    menu.add(menuItem);
                }

                if (menu.getSubElements().length > 0) {
                    menu.show(comp, e.getX(), e.getY());
                }
            }
        } else {
            TransferHandler handler = comp.getTransferHandler();

            if (e.isShiftDown()) {
                if (comp instanceof GoodsLabel) {
                    ((GoodsLabel) comp).getGoods().setAmount(-1);
                } else if (comp instanceof MarketLabel) {
                    ((MarketLabel) comp).setAmount(-1);
                }
            } else {
                // We can have less than 100 of the goods so we must not do setAmount(100).
                //if (comp instanceof GoodsLabel) {
                    //((GoodsLabel) comp).getGoods().setAmount(100);
                //} else
                
                if (comp instanceof MarketLabel) {
                    ((MarketLabel) comp).setAmount(100);
                }
            }

            if ((comp instanceof UnitLabel) && (((UnitLabel)comp).getUnit().isCarrier())) {
                if (parentPanel instanceof EuropePanel) {
                    Unit u = ((UnitLabel) comp).getUnit();
                    if (u.getState() != Unit.TO_AMERICA && u.getState() != Unit.TO_EUROPE) {
                        ((EuropePanel) parentPanel).setSelectedUnitLabel((UnitLabel)comp);
                    }
                } else if (parentPanel instanceof ColonyPanel) {
                    ((ColonyPanel) parentPanel).setSelectedUnitLabel((UnitLabel)comp);
                }
            }
            
            handler.exportAsDrag(comp, e, TransferHandler.COPY);
        }
    }
}
