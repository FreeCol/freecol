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

import java.awt.Font;
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

import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.UnitLabel.UnitAction;
import net.sf.freecol.common.Specification;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
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
            if (parentPanel.isEditable()) {
                JPopupMenu menu = null;
                if (comp instanceof UnitLabel) {
                    menu = getUnitMenu((UnitLabel) comp);
                } else if (comp instanceof GoodsLabel) {
                    menu = getGoodsMenu((GoodsLabel) comp);
                } else if (comp instanceof MarketLabel &&
                           parentPanel instanceof EuropePanel) {
                    ((EuropePanel) parentPanel).payArrears(((MarketLabel) comp).getType());
                }
                if (menu != null) {
                    int elements = menu.getSubElements().length;
                    if (elements > 0) {
                        int lastIndex = menu.getComponentCount() - 1;
                        if (menu.getComponent(lastIndex) instanceof JPopupMenu.Separator) {
                            menu.remove(lastIndex);
                        }
                        menu.show(comp, e.getX(), e.getY());
                    }
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


    public JPopupMenu getUnitMenu(final UnitLabel unitLabel) {
        ImageLibrary imageLibrary = unitLabel.getCanvas().getGUI().getImageLibrary();
        final Unit tempUnit = unitLabel.getUnit();
        JPopupMenu menu = new JPopupMenu("Unit");
        ImageIcon unitIcon = imageLibrary.getUnitImageIcon(tempUnit);

        JMenuItem name = new JMenuItem(tempUnit.getName() + " (" +
                                       Messages.message("menuBar.colopedia") + ")", 
                                       imageLibrary.getScaledImageIcon(unitIcon, 0.66f));
        name.setActionCommand(UnitAction.COLOPEDIA.toString());
        name.addActionListener(unitLabel);
        menu.add(name);
        menu.addSeparator();

        if (tempUnit.isCarrier()) {
            if (addCarrierItems(unitLabel, menu)) {
                menu.addSeparator();
            }
        }                

        if (tempUnit.getLocation().getTile() != null && 
            tempUnit.getLocation().getTile().getColony() != null) {
            if (addWorkItems(unitLabel, menu)) {
                menu.addSeparator();
            }
            if (addEducationItems(unitLabel, menu)) {
                menu.addSeparator();
            }
            if (!(tempUnit.getLocation() instanceof WorkLocation)) {
                if (addCommandItems(unitLabel, menu)) {
                    menu.addSeparator();
                }
            }
        }

        if (tempUnit.hasAbility("model.ability.canBeEquipped")) {
            if (addEquipmentItems(unitLabel, menu)) {
                menu.addSeparator();
            }
        }

        return menu;
    }

    private boolean addCarrierItems(final UnitLabel unitLabel, final JPopupMenu menu) {
        final Unit tempUnit = unitLabel.getUnit();

        if (tempUnit.getSpaceLeft() < tempUnit.getType().getSpace()) {
            JMenuItem cargo = new JMenuItem(Messages.message("cargoOnCarrier"));
            menu.add(cargo);

            for (Unit passenger : tempUnit.getUnitList()) {
                JMenuItem menuItem = new JMenuItem("    " + passenger.getName());
                menuItem.setFont(menuItem.getFont().deriveFont(Font.ITALIC));
                menu.add(menuItem);
            }
            for (Goods goods : tempUnit.getGoodsList()) {
                JMenuItem menuItem = new JMenuItem("    " + goods.toString());
                menuItem.setFont(menuItem.getFont().deriveFont(Font.ITALIC));
                menu.add(menuItem);
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean addWorkItems(final UnitLabel unitLabel, final JPopupMenu menu) {

        final Unit tempUnit = unitLabel.getUnit();
        ImageLibrary imageLibrary = unitLabel.getCanvas().getGUI().getImageLibrary();
        Colony colony = tempUnit.getLocation().getColony();
        boolean separatorNeeded = false;

        List<GoodsType> farmedGoods = Specification.getSpecification().getFarmedGoodsTypeList();
        // Work in Field - automatically find the best location
        for (GoodsType goodsType : farmedGoods) {
            ColonyTile bestTile = colony.getVacantColonyTileFor(tempUnit, goodsType);
            if (bestTile != null) {
                int maxpotential = bestTile.getProductionOf(tempUnit, goodsType);
                UnitType expert = Specification.getSpecification().getExpertForProducing(goodsType);
                JMenuItem menuItem = new JMenuItem(Messages.message(goodsType.getId() + ".workAs",
                                                                    "%amount%",
                                                                    Integer.toString(maxpotential)),
                                                   imageLibrary.getScaledGoodsImageIcon(goodsType, 0.66f));
                menuItem.setActionCommand(UnitAction.WORK_TILE.toString() + ":" + goodsType.getId());
                menuItem.addActionListener(unitLabel);
                menu.add(menuItem);
                separatorNeeded = true;
            }
        }
                    
        // Work at Building - show both max potential and realistic projection
        for (Building building : colony.getBuildings()) {
            if (tempUnit.getWorkLocation() != building) { // Skip if currently working at this location
                if (building.canAdd(tempUnit)) {
                    GoodsType goodsType = building.getGoodsOutputType();
                    String locName = building.getName();
                    JMenuItem menuItem = new JMenuItem(locName);
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
                        if (addOutput == 0) {
                            menuItem.setForeground(FreeColPanel.LINK_COLOR);
                        }
                    }
                    menuItem.setActionCommand(UnitAction.WORK_BUILDING.toString() + ":" +
                                              building.getType().getId());
                    menuItem.addActionListener(unitLabel);
                    menu.add(menuItem);
                    separatorNeeded = true;
                }
            }
        }

        if (tempUnit.getWorkTile() != null) {
            JMenuItem menuItem = new JMenuItem(Messages.message("showProduction"));
            menuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        Canvas canvas = unitLabel.getCanvas();
                        canvas.showSubPanel(new ColonyTileProductionPanel(canvas, tempUnit.getWorkTile(), tempUnit.getWorkType()));
                    }
                });
            menu.add(menuItem);
            separatorNeeded = true;
        } else if (tempUnit.getWorkLocation() != null) {
            JMenuItem menuItem = new JMenuItem(Messages.message("showProductivity"));
            menuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        Canvas canvas = unitLabel.getCanvas();
                        canvas.showSubPanel(new BuildingProductionPanel(canvas, tempUnit));
                    }
                });
            menu.add(menuItem);
            separatorNeeded = true;
        }

        return separatorNeeded;
    }
    
    private boolean addEducationItems(final UnitLabel unitLabel, final JPopupMenu menu) {
        Unit tempUnit = unitLabel.getUnit();
        ImageLibrary imageLibrary = unitLabel.getCanvas().getGUI().getImageLibrary();
        boolean separatorNeeded = false;
        for (Unit teacher : tempUnit.getColony().getTeachers()) {
            if (tempUnit.canBeStudent(teacher) &&
                tempUnit.getLocation() instanceof WorkLocation &&
                teacher.getStudent() != tempUnit) {
                JMenuItem menuItem = new JMenuItem(Messages.message("assignToTeacher"),
                                                   imageLibrary.getScaledImageIcon(imageLibrary.getUnitImageIcon(teacher), 0.5f));
                menuItem.setActionCommand(UnitAction.ASSIGN.toString() + ":" + teacher.getId());
                menuItem.addActionListener(unitLabel);
                menu.add(menuItem);
                separatorNeeded = true;
            }
        }
        if (tempUnit.getTurnsOfTraining() > 0 && tempUnit.getStudent() != null) {
            JMenuItem teaching = new JMenuItem(Messages.message("menuBar.teacher") +
                                               ": " + tempUnit.getTurnsOfTraining() +
                                               "/" + tempUnit.getNeededTurnsOfTraining());
            teaching.setEnabled(false);
            menu.add(teaching);
            separatorNeeded = true;
        }
        int experience = Math.min(tempUnit.getExperience(), 200);
        if (experience > 0 && tempUnit.getWorkType() != null) {
            UnitType workType = Specification.getSpecification()
                .getExpertForProducing(tempUnit.getWorkType());
            if (tempUnit.getType().canBeUpgraded(workType, UnitType.UpgradeType.EXPERIENCE)) {
                JMenuItem experienceItem = new JMenuItem(Messages.message("menuBar.experience") +
                                                         ": " + experience + "/5000");
                experienceItem.setEnabled(false);
                menu.add(experienceItem);
                separatorNeeded = true;
            }
        }

        return separatorNeeded;
    }


    private boolean addCommandItems(final UnitLabel unitLabel, final JPopupMenu menu) {
        final Unit tempUnit = unitLabel.getUnit();
        JMenuItem menuItem = new JMenuItem(Messages.message("activateUnit"));
        menuItem.setActionCommand(UnitAction.ACTIVATE_UNIT.toString());
        menuItem.addActionListener(unitLabel);
        menuItem.setEnabled(true);
        menu.add(menuItem);

        menuItem = new JMenuItem(Messages.message("fortifyUnit"));
        menuItem.setActionCommand(UnitAction.FORTIFY.toString());
        menuItem.addActionListener(unitLabel);
        menuItem.setEnabled((tempUnit.getMovesLeft() > 0)
                            && !(tempUnit.getState() == UnitState.FORTIFIED ||
                                 tempUnit.getState() == UnitState.FORTIFYING));
        menu.add(menuItem);

        menuItem = new JMenuItem(Messages.message("sentryUnit"));
        menuItem.setActionCommand(UnitAction.SENTRY.toString());
        menuItem.addActionListener(unitLabel);
        menuItem.setEnabled(tempUnit.getState() != UnitState.SENTRY);
        menu.add(menuItem);

        if (tempUnit.canCarryTreasure() && !tempUnit.getColony().isLandLocked()) {
            menuItem = new JMenuItem(Messages.message("cashInTreasureTrain.order"));
            menuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        unitLabel.getCanvas().getClient().getInGameController()
                            .checkCashInTreasureTrain(tempUnit);
                    }
                });
            menu.add(menuItem);
        }
        return true;
    }


    private boolean addEquipmentItems(final UnitLabel unitLabel, final JPopupMenu menu) {
        final Unit tempUnit = unitLabel.getUnit();
        ImageLibrary imageLibrary = unitLabel.getCanvas().getGUI().getImageLibrary();
        boolean separatorNeeded = false;
        for (EquipmentType equipmentType : Specification.getSpecification().getEquipmentTypeList()) {
            int count = tempUnit.getEquipment().getCount(equipmentType);
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
                            unitLabel.getCanvas().getClient().getInGameController()
                                .equipUnit(tempUnit, type, -items);
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
                                unitLabel.getCanvas().getClient().getInGameController()
                                    .equipUnit(tempUnit, type, items);
                                unitLabel.updateIcon();
                            }
                        });
                    menu.add(newItem);
                }
            }
        }
        separatorNeeded = true;

        if (tempUnit.getLocation() instanceof WorkLocation
            && tempUnit.getColony().canReducePopulation()) {
            JMenuItem menuItem = new JMenuItem(Messages.message("leaveTown"));
            menuItem.setActionCommand(UnitAction.LEAVE_TOWN.toString());
            menuItem.addActionListener(unitLabel);
            menu.add(menuItem);
                
            separatorNeeded = true;
        }

        if (separatorNeeded) {
            menu.addSeparator();
            separatorNeeded = false;
        }

        if (tempUnit.getType().getDowngrade(UnitType.DowngradeType.CLEAR_SKILL) != null) {
            JMenuItem menuItem = new JMenuItem(Messages.message("clearSpeciality"));
            menuItem.setActionCommand(UnitAction.CLEAR_SPECIALITY.toString());
            menuItem.addActionListener(unitLabel);
            menu.add(menuItem);
            separatorNeeded = true;
        }
        return separatorNeeded;
    }


    public JPopupMenu getGoodsMenu(final GoodsLabel goodsLabel) {

        final Goods goods = goodsLabel.getGoods();
        final InGameController inGameController = goodsLabel.getCanvas().getClient().getInGameController();
        ImageLibrary imageLibrary = goodsLabel.getCanvas().getGUI().getImageLibrary();
        JPopupMenu menu = new JPopupMenu("Cargo");
        JMenuItem name = new JMenuItem(goods.getName() + " (" +
                                       Messages.message("menuBar.colopedia") + ")", 
                                       imageLibrary.getScaledGoodsImageIcon(goods.getType(), 0.66f));
        name.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    goodsLabel.getCanvas().showColopediaPanel(ColopediaPanel.PanelType.GOODS,
                                                              goods.getType());
                }
            });
        menu.add(name);
                
        JMenuItem unload = new JMenuItem(Messages.message("unload"));
        unload.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    inGameController.unloadCargo(goods);
                    if (parentPanel instanceof CargoPanel) {
                        CargoPanel cargoPanel = (CargoPanel) parentPanel;
                        cargoPanel.initialize();
                        /*
                        if (cargoPanel.getParentPanel() instanceof ColonyPanel) {
                            ((ColonyPanel) cargoPanel.getParentPanel()).updateWarehouse();
                        }
                        */
                    }
                    parentPanel.revalidate();
                }
            });
        menu.add(unload);
        JMenuItem dump = new JMenuItem(Messages.message("dumpCargo"));
        dump.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    inGameController.unloadCargo(goods, true);
                    if (parentPanel instanceof CargoPanel) {
                        ((CargoPanel) parentPanel).initialize();
                    }
                    parentPanel.revalidate();
                }
            });
        menu.add(dump);
                
        return menu;
    }
}