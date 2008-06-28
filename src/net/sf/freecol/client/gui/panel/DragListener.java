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

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.TransferHandler;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.resources.ResourceManager;

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
                final UnitLabel unitLabel = (UnitLabel) comp;
                ImageLibrary imageLibrary = unitLabel.getCanvas().getGUI().getImageLibrary();
                final Unit tempUnit = unitLabel.getUnit();
                final InGameController inGameController = unitLabel.getCanvas().getClient().getInGameController();
                JPopupMenu menu = new JPopupMenu("Unit");
                ImageIcon unitIcon = imageLibrary.getUnitImageIcon(tempUnit);

                JMenuItem name = new JMenuItem(tempUnit.getName() + " (" +
                                               Messages.message("menuBar.colopedia") + ")", 
                                               imageLibrary.getScaledImageIcon(unitIcon, 0.66f));
                name.setActionCommand(String.valueOf(UnitLabel.COLOPEDIA));
                name.addActionListener(unitLabel);
                menu.add(name);

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
                                                     imageLibrary.getScaledImageIcon(imageLibrary.getUnitImageIcon(teacher), 0.5f));
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
                            && !(tempUnit.getState() == UnitState.FORTIFIED ||
                                 tempUnit.getState() == UnitState.FORTIFYING));
                    menu.add(menuItem);

                    menuItem = new JMenuItem(Messages.message("sentryUnit"));
                    menuItem.setActionCommand(String.valueOf(UnitLabel.SENTRY));
                    menuItem.addActionListener(unitLabel);
                    menuItem.setEnabled(tempUnit.getState() != UnitState.SENTRY);
                    menu.add(menuItem);

                    menu.addSeparator();
                }


                if (tempUnit.hasAbility("model.ability.canBeEquipped")) {
                    for (EquipmentType equipmentType : FreeCol.getSpecification().getEquipmentTypeList()) {
                        int count = 0;
                        for (EquipmentType oldEquipment : tempUnit.getEquipment()) {
                            if (equipmentType == oldEquipment) {
                                count++;
                            }
                        }
                        if (count > 0) {
                            // "remove current equipment" action
                            JMenuItem newItem = new JMenuItem(Messages.message(equipmentType.getId() + ".remove"));
                            if (!equipmentType.getGoodsRequired().isEmpty()) {
                                GoodsType goodsType = equipmentType.getGoodsRequired().get(0).getType();
                                newItem.setIcon(imageLibrary.getScaledGoodsImageIcon(goodsType, 0.66f));
                            }
                            final int items = count;
                            final EquipmentType type = equipmentType; 
                            newItem.addActionListener(new ActionListener() {
                                    public void actionPerformed(ActionEvent e) {
                                        inGameController.equipUnit(tempUnit, type, -items);
                                        unitLabel.updateIcon();
                                    }
                                });
                            menu.add(newItem);
                        }
                        if (tempUnit.canBeEquippedWith(equipmentType)) {
                            // "add new equipment" action
                            JMenuItem newItem = null;
                            count = equipmentType.getMaximumCount() - count;
                            if (equipmentType.getGoodsRequired().isEmpty()) {
                                newItem = new JMenuItem();
                                newItem.setText(Messages.message(equipmentType.getId() + ".add"));
                            } else if (tempUnit.isInEurope() &&
                                       tempUnit.getOwner().getEurope().canBuildEquipment(equipmentType)) {
                                int price = 0;
                                newItem = new JMenuItem();
                                for (AbstractGoods goodsRequired : equipmentType.getGoodsRequired()) {
                                    price += tempUnit.getOwner().getMarket().getBidPrice(goodsRequired.getType(),
                                                                                         goodsRequired.getAmount());
                                    newItem.setIcon(imageLibrary.getScaledGoodsImageIcon(goodsRequired.getType(), 0.66f));
                                }
                                while (count * price > tempUnit.getOwner().getGold()) {
                                    count--;
                                }
                                newItem.setText(Messages.message(equipmentType.getId() + ".add") + " (" +
                                                Messages.message("goldAmount", "%amount%", 
                                                                 String.valueOf(count * price)) +
                                                ")");
                            } else if (tempUnit.getColony() != null &&
                                       tempUnit.getColony().canBuildEquipment(equipmentType)) {
                                newItem = new JMenuItem();
                                for (AbstractGoods goodsRequired : equipmentType.getGoodsRequired()) {
                                    int present = tempUnit.getColony().getGoodsCount(goodsRequired.getType()) /
                                        goodsRequired.getAmount();
                                    if (present < count) {
                                        count = present;
                                    }
                                    newItem.setIcon(imageLibrary.getScaledGoodsImageIcon(goodsRequired.getType(), 0.66f));
                                }
                                newItem.setText(Messages.message(equipmentType.getId() + ".add"));
                            }
                            if (newItem != null) {
                                final int items = count;
                                final EquipmentType type = equipmentType; 
                                newItem.addActionListener(new ActionListener() {
                                        public void actionPerformed(ActionEvent e) {
                                            inGameController.equipUnit(tempUnit, type, items);
                                            unitLabel.updateIcon();
                                        }
                                    });
                                menu.add(newItem);
                            }
                        }
                    }
                    separatorNeeded = true;

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

                    if (tempUnit.getType().getDowngrade(UnitType.DowngradeType.CLEAR_SKILL) != null) {
                        menuItem = new JMenuItem(Messages.message("clearSpeciality"));
                        menuItem.setActionCommand(String.valueOf(UnitLabel.CLEAR_SPECIALITY));
                        menuItem.addActionListener(unitLabel);
                        menu.add(menuItem);
                        menu.addSeparator();
                    }
                    
                }

                /*
                Image colopediaIcon = ResourceManager.getImage("Colopedia.closedSection.image");
                menuItem = new JMenuItem(Messages.message("menuBar.colopedia"), 
                        imageLibrary.getScaledImageIcon(colopediaIcon, 0.66f));
                menuItem.setActionCommand(String.valueOf(UnitLabel.COLOPEDIA));
                menuItem.addActionListener(unitLabel);
                menu.add(menuItem);
                */

                int experience = Math.min(tempUnit.getExperience(), 200);
                if (tempUnit.getTurnsOfTraining() > 0 || experience > 0) {
                    menu.addSeparator();
                }
                if (tempUnit.getTurnsOfTraining() > 0) {
                    JMenuItem teaching = new JMenuItem(Messages.message("menuBar.teacher") +
                                                       ": " + tempUnit.getTurnsOfTraining() +
                                                       "/" + tempUnit.getNeededTurnsOfTraining());
                    teaching.setEnabled(false);
                    menu.add(teaching);
                }
                if (experience > 0) {
                    JMenuItem experienceItem = new JMenuItem(Messages.message("menuBar.experience") +
                                                             ": " + experience + "/200");
                    experienceItem.setEnabled(false);
                    menu.add(experienceItem);
                }

                if (menu.getSubElements().length > 0) {
                    menu.show(comp, e.getX(), e.getY());
                }

            } else if (comp instanceof GoodsLabel) {
                final GoodsLabel goodsLabel = (GoodsLabel) comp;
                final InGameController inGameController = goodsLabel.getCanvas().getClient().getInGameController();
                ImageLibrary imageLibrary = goodsLabel.getCanvas().getGUI().getImageLibrary();
                JPopupMenu menu = new JPopupMenu("Cargo");
                ImageIcon goodsIcon = imageLibrary.getScaledGoodsImageIcon(goodsLabel.getGoods().getType(), 0.66f);
                JMenuItem name = new JMenuItem(goodsLabel.getGoods().getName(), goodsIcon);
                name.setEnabled(false);
                menu.add(name);
                
                JMenuItem unload = new JMenuItem(Messages.message("unload"));
                unload.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            inGameController.unloadCargo(goodsLabel.getGoods());
                            if (parentPanel instanceof CargoPanel) {
                                CargoPanel cargoPanel = (CargoPanel) parentPanel;
                                cargoPanel.initialize();
                                if (cargoPanel.getParentPanel() instanceof ColonyPanel) {
                                    ((ColonyPanel) cargoPanel.getParentPanel()).updateWarehouse();
                                }
                            }
                            parentPanel.revalidate();
                        }
                    });
                menu.add(unload);
                JMenuItem dump = new JMenuItem(Messages.message("dumpCargo"));
                dump.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            inGameController.unloadCargo(goodsLabel.getGoods(), true);
                            if (parentPanel instanceof CargoPanel) {
                                ((CargoPanel) parentPanel).initialize();
                            }
                            parentPanel.revalidate();
                        }
                    });
                menu.add(dump);
                
                menu.addSeparator();
                
                Image colopediaIcon = ResourceManager.getImage("Colopedia.closedSection.image");
                JMenuItem colopedia = new JMenuItem(Messages.message("menuBar.colopedia"), 
                        imageLibrary.getScaledImageIcon(colopediaIcon, 0.66f));
                colopedia.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            goodsLabel.getCanvas().showColopediaPanel(ColopediaPanel.PanelType.GOODS,
                                                                      goodsLabel.getGoods().getType());
                        }
                    });
                menu.add(colopedia);
                /*
                 * if (parentPanel instanceof ColonyPanel) { Colony colony =
                 * ((ColonyPanel) parentPanel).getColony(); ((GoodsLabel)
                 * comp).getCanvas().showWarehouseDialog(colony);
                 * comp.repaint(); }
                 */
                if (menu.getSubElements().length > 0) {
                    menu.show(comp, e.getX(), e.getY());
                }

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
                    if (u.getState() != UnitState.TO_AMERICA && u.getState() != UnitState.TO_EUROPE) {
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
