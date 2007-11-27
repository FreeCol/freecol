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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.TransferHandler;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.WorkLocation;

/**
 * A DragListener should be attached to Swing components that have a
 * TransferHandler attached. The DragListener will make sure that the Swing
 * component to which it is attached is draggable (moveable to be precise).
 */
public final class DragListener extends MouseAdapter {




    private final FreeColPanel parentPanel;


    /**
     * The constructor to use.
     * 
     * @param parentPanel The layered pane that contains the components to which
     *            a DragListener might be attached.
     */
    public DragListener(FreeColPanel parentPanel) {
        this.parentPanel = parentPanel;
    }

    /**
     * Gets called when the mouse was pressed on a Swing component that has this
     * object as a MouseListener.
     * 
     * @param e The event that holds the information about the mouse click.
     */
    public void mousePressed(MouseEvent e) {
        JComponent comp = (JComponent) e.getSource();

        // Does not work on some platforms:
        // if (e.isPopupTrigger() && (comp instanceof UnitLabel)) {
        if ((e.getButton() == MouseEvent.BUTTON3 || e.isPopupTrigger())) {
            // Popup mustn't be shown when panel is not editable
            if (!parentPanel.isEditable()) return;
            
            if (comp instanceof UnitLabel) {
                UnitLabel unitLabel = (UnitLabel) comp;
                ImageLibrary imageLibrary = unitLabel.getCanvas().getGUI().getImageLibrary();
                Unit tempUnit = unitLabel.getUnit();

                JPopupMenu menu = new JPopupMenu("Unit");
                JMenuItem menuItem;
                boolean separatorNeeded = false;

                if (tempUnit.getLocation().getTile() != null && 
                    tempUnit.getLocation().getTile().getSettlement() != null) {
                    Colony colony = (Colony) tempUnit.getLocation().getColony();
                    
                    List<GoodsType> farmedGoods = FreeCol.getSpecification().getFarmedGoodsTypeList();
                    // Work in Field - automatically find the best location
                    for (GoodsType goodsType : farmedGoods) {
                        int maxpotential = colony.getVacantColonyTileProductionFor(tempUnit, goodsType);
                        if (maxpotential > 0) {
                            UnitType expert = FreeCol.getSpecification().getExpertForProducing(goodsType);
                            menuItem = new JMenuItem(Messages.message("beAExpert", "%expert%", expert.getName())
                                                     + " (" + maxpotential + " " + goodsType.getName() + ")",
                                                     imageLibrary.getScaledGoodsImageIcon(goodsType, 0.66f));
                            menuItem.setActionCommand(String.valueOf(UnitLabel.WORK_FARMING+goodsType.getIndex()));
                            menuItem.addActionListener(unitLabel);
                            menu.add(menuItem);
                        }
                    }
                    
                    // Work at Building - show both max potential and realistic projection
                    for (Building building : colony.getBuildings()) {
                        if (tempUnit.getWorkLocation() != building) { // Skip if currently working at this location
                            if (building.canAdd(tempUnit)) {
                                GoodsType goodsType = building.getGoodsOutputType();
                                String locName = building.getName();
                                menuItem = new JMenuItem(locName);
                                if (goodsType != null) {
                                    menuItem.setIcon(imageLibrary.getScaledGoodsImageIcon(goodsType, 0.66f));
                                    int addOutput = building.getAdditionalProductionNextTurn(tempUnit);
                                    locName += " (" + addOutput;
                                    int potential = building.getAdditionalProduction(tempUnit);
                                    if (addOutput < potential) {
                                        // Not reaching full potential, show full potential
                                        locName += "/" + potential;
                                    }
                                    locName +=  " " + goodsType.getName()+")";
                                    menuItem.setText(locName);
                                }
                                menuItem.setActionCommand(String.valueOf(UnitLabel.WORK_AT_BUILDING+building.getType().getIndex()));
                                menuItem.addActionListener(unitLabel);
                                menu.add(menuItem);
                            }
                        }
                    }
                    
                    separatorNeeded = true;
                }

                if (separatorNeeded) {
                    menu.addSeparator();
                    separatorNeeded = false;
                }

                if (tempUnit.getColony() != null) {
                    for (Unit teacher : tempUnit.getColony().getTeachers()) {
                        if (tempUnit.canBeStudent(teacher) &&
                                tempUnit.getLocation() instanceof WorkLocation &&
                                teacher.getStudent() != tempUnit) {
                            menuItem = new JMenuItem(Messages.message("assignToTeacher"),
                                                     imageLibrary.getScaledUnitImageIcon(teacher.getType(), 0.5f));
                            menuItem.setActionCommand("assign" + teacher.getId());
                            menuItem.addActionListener(unitLabel);
                            menu.add(menuItem);
                            separatorNeeded = true;
                        }
                    }
                }

                if (separatorNeeded) {
                    menu.addSeparator();
                    separatorNeeded = false;
                }

                if (tempUnit.getColony() != null && !(tempUnit.getLocation() instanceof WorkLocation)) {
                    menuItem = new JMenuItem(Messages.message("activateUnit"));
                    menuItem.setActionCommand(String.valueOf(UnitLabel.ACTIVATE_UNIT));
                    menuItem.addActionListener(unitLabel);
                    menuItem.setEnabled(true);
                    menu.add(menuItem);

                    menuItem = new JMenuItem(Messages.message("fortifyUnit"));
                    menuItem.setActionCommand(String.valueOf(UnitLabel.FORTIFY));
                    menuItem.addActionListener(unitLabel);
                    menuItem.setEnabled((tempUnit.getMovesLeft() > 0)
                            && !(tempUnit.getState() == Unit.FORTIFIED || tempUnit.getState() == Unit.FORTIFYING));
                    menu.add(menuItem);

                    menuItem = new JMenuItem(Messages.message("sentryUnit"));
                    menuItem.setActionCommand(String.valueOf(UnitLabel.SENTRY));
                    menuItem.addActionListener(unitLabel);
                    menuItem.setEnabled(tempUnit.getState() != Unit.SENTRY);
                    menu.add(menuItem);

                    menu.addSeparator();
                }


                if (tempUnit.isColonist()) {
                    if (tempUnit.canBeArmed()) {
                        if (tempUnit.isArmed()) {
                            menuItem = new JMenuItem(Messages.message("disarm"));
                        } else {
                            if (tempUnit.getTile() == null) { // -> in Europe
                                int price = tempUnit.getOwner().getMarket().getBidPrice(Goods.MUSKETS, 50);
                                menuItem = new JMenuItem(Messages.message("arm") + " (" +
                                                         Messages.message("goldAmount", "%amount%",
                                                                          String.valueOf(price)) + ")");
                            } else {
                                menuItem = new JMenuItem(Messages.message("arm"));
                            }
                        }
                        menuItem.setIcon(imageLibrary.getScaledGoodsImageIcon(Goods.MUSKETS, 0.66f));
                        menuItem.setActionCommand(String.valueOf(UnitLabel.ARM));
                        menuItem.addActionListener(unitLabel);
                        menu.add(menuItem);
                        separatorNeeded = true;
                    }

                    if (tempUnit.canBeMounted()) {
                        if (tempUnit.isMounted()) {
                            menuItem = new JMenuItem(Messages.message("removeHorses"));
                        } else {
                            if (tempUnit.getTile() == null) { // -> in Europe
                                int price = tempUnit.getOwner().getMarket().getBidPrice(Goods.HORSES, 50);
                                menuItem = new JMenuItem(Messages.message("mount") + " (" +
                                                         Messages.message("goldAmount", "%amount%",
                                                                          String.valueOf(price)) + ")");
                            } else {
                                menuItem = new JMenuItem(Messages.message("mount"));
                            }
                        }
                        menuItem.setIcon(imageLibrary.getScaledGoodsImageIcon(Goods.HORSES, 0.66f));
                        menuItem.setActionCommand(String.valueOf(UnitLabel.MOUNT));
                        menuItem.addActionListener(unitLabel);
                        menu.add(menuItem);
                        separatorNeeded = true;
                    }

                    if (tempUnit.canBeEquippedWithTools()) {
                        if (tempUnit.isPioneer()) {
                            menuItem = new JMenuItem(Messages.message("removeTools"));
                        } else {
                            if (tempUnit.getTile() == null) { // -> in Europe
                                int amount = 100;
                                int price = tempUnit.getOwner().getMarket().getBidPrice(Goods.TOOLS, amount);
                                if (price <= tempUnit.getOwner().getGold()) {
                                    menuItem = new JMenuItem(Messages.message("equipWithTools") + " (" +
                                                             Messages.message("goldAmount", "%amount%",
                                                                              String.valueOf(price)) + ")");
                                } else {
                                    while (price > tempUnit.getOwner().getGold()) {
                                        amount -= 20;
                                        price = tempUnit.getOwner().getMarket().getBidPrice(Goods.TOOLS, amount);
                                    }
                                    menuItem = new JMenuItem(Messages.message("equipWithToolsNumber", "%number%",
                                                                              String.valueOf(amount)) + " " +
                                                             Messages.message("goldAmount", "%amount%",
                                                                              String.valueOf(price)) + ")");

                                }
                            } else {
                                menuItem = new JMenuItem(Messages.message("equipWithTools"));
                            }
                        }
                        menuItem.setIcon(imageLibrary.getScaledGoodsImageIcon(Goods.TOOLS, 0.66f));
                        menuItem.setActionCommand(String.valueOf(UnitLabel.TOOLS));
                        menuItem.addActionListener(unitLabel);
                        menu.add(menuItem);
                        separatorNeeded = true;
                    }

                    if (tempUnit.canBeDressedAsMissionary()) {

                        if (tempUnit.isMissionary()) {
                            menuItem = new JMenuItem(Messages.message("cancelMissionaryStatus"));
                        } else {
                            menuItem = new JMenuItem(Messages.message("blessAsMissionaries"));
                        }
                        menuItem.setIcon(imageLibrary.getScaledGoodsImageIcon(Goods.CROSSES, 0.66f));
                        menuItem.setActionCommand(String.valueOf(UnitLabel.DRESS));
                        menuItem.addActionListener(unitLabel);
                        menu.add(menuItem);
                        separatorNeeded = true;
                    }

                    if (tempUnit.getLocation() instanceof WorkLocation) {
                        menuItem = new JMenuItem(Messages.message("leaveTown"));
                        menuItem.setActionCommand(String.valueOf(UnitLabel.LEAVE_TOWN));
                        menuItem.addActionListener(unitLabel);
                        menu.add(menuItem);
                        separatorNeeded = true;
                    }

                    if (separatorNeeded) {
                        menu.addSeparator();
                        separatorNeeded = false;
                    }

                    if (tempUnit.getUnitType().getClearSpeciality() != null) {
                        menuItem = new JMenuItem(Messages.message("clearSpeciality"));
                        menuItem.setActionCommand(String.valueOf(UnitLabel.CLEAR_SPECIALITY));
                        menuItem.addActionListener(unitLabel);
                        menu.add(menuItem);
                        menu.addSeparator();
                    }
                    
                }

                menuItem = new JMenuItem(Messages.message("menuBar.colopedia"));
                menuItem.setActionCommand(String.valueOf(UnitLabel.COLOPEDIA));
                menuItem.addActionListener(unitLabel);
                menu.add(menuItem);

                if (menu.getSubElements().length > 0) {
                    menu.show(comp, e.getX(), e.getY());
                }

            } else if (comp instanceof GoodsLabel) {
                GoodsLabel goodsLabel = (GoodsLabel) comp;
                goodsLabel.getCanvas().showColopediaPanel(ColopediaPanel.COLOPEDIA_GOODS,
                        goodsLabel.getGoods().getType().getIndex());
                /*
                 * if (parentPanel instanceof ColonyPanel) { Colony colony =
                 * ((ColonyPanel) parentPanel).getColony(); ((GoodsLabel)
                 * comp).getCanvas().showWarehouseDialog(colony);
                 * comp.repaint(); }
                 */
            } else if (comp instanceof MarketLabel) {
                if (parentPanel instanceof EuropePanel) {
                    ((EuropePanel) parentPanel).payArrears(((MarketLabel) comp).getType());
                }
            }
        } else {
            TransferHandler handler = comp.getTransferHandler();

            if (e.isShiftDown()) {
                if (comp instanceof GoodsLabel) {
                    ((GoodsLabel) comp).setPartialChosen(true);
                } else if (comp instanceof MarketLabel) {
                    ((MarketLabel) comp).setPartialChosen(true);
                }
            } else {
                if (comp instanceof GoodsLabel) {
                    ((GoodsLabel) comp).setPartialChosen(false);
                } else if (comp instanceof MarketLabel) {
                    ((MarketLabel) comp).setPartialChosen(false);
                    ((MarketLabel) comp).setAmount(100);
                }
            }

            if ((comp instanceof UnitLabel) && (((UnitLabel) comp).getUnit().isCarrier())) {
                if (parentPanel instanceof EuropePanel) {
                    Unit u = ((UnitLabel) comp).getUnit();
                    if (u.getState() != Unit.TO_AMERICA && u.getState() != Unit.TO_EUROPE) {
                        ((EuropePanel) parentPanel).setSelectedUnitLabel((UnitLabel) comp);
                    }
                } else if (parentPanel instanceof ColonyPanel) {
                    ((ColonyPanel) parentPanel).setSelectedUnitLabel((UnitLabel) comp);
                }
            }

            if (handler != null) {
                handler.exportAsDrag(comp, e, TransferHandler.COPY);
            }
        }
    }
}
