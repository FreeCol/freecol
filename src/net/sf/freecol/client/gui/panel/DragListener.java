package net.sf.freecol.client.gui.panel;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.TransferHandler;

import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Unit;

/**
 * A DragListener should be attached to Swing components that have a
 * TransferHandler attached. The DragListener will make sure that the Swing
 * component to which it is attached is draggable (moveable to be precise).
 */
public final class DragListener extends MouseAdapter {

    public static final String COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    private static final int[] goodsTypes = { Goods.FOOD, Goods.SUGAR, Goods.TOBACCO, Goods.COTTON, 
                                              Goods.FURS, Goods.LUMBER, Goods.ORE, Goods.SILVER };

    private static final int[] workTypes = { UnitLabel.WORKTYPE_FOOD, UnitLabel.WORKTYPE_SUGAR,
                                             UnitLabel.WORKTYPE_TOBACCO, UnitLabel.WORKTYPE_COTTON, 
                                             UnitLabel.WORKTYPE_FURS, UnitLabel.WORKTYPE_LUMBER,
                                             UnitLabel.WORKTYPE_ORE, UnitLabel.WORKTYPE_SILVER };

    private static final String[] messages = { "beAFarmer", "beASugarPlanter",
                                               "beATobaccoPlanter", "beAcottonPlanter",
                                               "beAFurTrapper", "beALumberjack",
                                               "beAnOreMiner", "beASilverMiner"};

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
                ImageLibrary imageLibrary = (ImageLibrary) unitLabel.getCanvas().getImageProvider();
                Unit tempUnit = unitLabel.getUnit();

                JPopupMenu menu = new JPopupMenu("Unit");
                JMenuItem menuItem;

                if (tempUnit.getColony() != null) {
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


                if ((tempUnit.isColonist() || tempUnit.getType() == Unit.INDIAN_CONVERT)
                        && tempUnit.getLocation() instanceof ColonyTile) {
                    ColonyTile colonyTile = (ColonyTile) tempUnit.getLocation();
                    for (int index = 0; index < goodsTypes.length; index++) {
                        int potential = tempUnit.getFarmedPotential(goodsTypes[index], 
                                                                    colonyTile.getWorkTile());
                        if (potential > 0) {
                            int maxPotential = colonyTile.getColony()
                                .getVacantColonyTileProductionFor(tempUnit, goodsTypes[index]);
                            menuItem = new JMenuItem(Messages.message(messages[index]) +
                                                     " (" + potential + "/" + maxPotential + ")",
                                                     imageLibrary.getScaledGoodsImageIcon(goodsTypes[index], 0.66f));
                            menuItem.setActionCommand(String.valueOf(workTypes[index]));
                            menuItem.addActionListener(unitLabel);
                            menu.add(menuItem);
                        }
                    }
                    menu.addSeparator();
                }

                if (tempUnit.getLocation().getTile() != null && 
                    tempUnit.getLocation().getTile().getSettlement() != null) {
                    Colony colony = (Colony) tempUnit.getLocation().getColony();
                    Iterator<Building> buildingIterator = colony.getBuildingIterator();
                    while (buildingIterator.hasNext()) {
                        Building building = buildingIterator.next();
                        if (building.isBuilt() && building.canAdd(tempUnit)) {
                            String locName = building.getName();
                            int goodsType = building.getGoodsOutputType();
                            menuItem = new JMenuItem(locName);
                            if (goodsType > -1) {
                                menuItem.setIcon(imageLibrary.getScaledGoodsImageIcon(goodsType, 0.66f));
                            }
                            menuItem.setActionCommand(String.valueOf(UnitLabel.WORK_AT_SOMEWHERE+building.getType()));
                            menuItem.addActionListener(unitLabel);
                            menu.add(menuItem);
                        }
                    }
                    menu.addSeparator();
                }

                if (tempUnit.isColonist()) {
                    if (tempUnit.canBeArmed()) {
                        if (tempUnit.isArmed()) {
                            menuItem = new JMenuItem(Messages.message("disarm"), unitLabel.getCanvas()
                                    .getImageProvider().getGoodsImageIcon(ImageLibrary.GOODS_MUSKETS));
                        } else {
                            if (tempUnit.getTile() == null) { // -> in Europe
                                menuItem = new JMenuItem(Messages.message("arm") + " ("
                                        + tempUnit.getOwner().getMarket().getBidPrice(Goods.MUSKETS, 50) + " gold)",
                                        imageLibrary.getGoodsImageIcon(
                                                ImageLibrary.GOODS_MUSKETS));
                            } else {
                                menuItem = new JMenuItem(Messages.message("arm"), unitLabel.getCanvas()
                                        .getImageProvider().getGoodsImageIcon(ImageLibrary.GOODS_MUSKETS));
                            }
                        }
                        menuItem.setActionCommand(String.valueOf(UnitLabel.ARM));
                        menuItem.addActionListener(unitLabel);
                        menu.add(menuItem);
                    }

                    if (tempUnit.canBeMounted()) {
                        if (tempUnit.isMounted()) {
                            menuItem = new JMenuItem(Messages.message("removeHorses"), unitLabel.getCanvas()
                                    .getImageProvider().getGoodsImageIcon(ImageLibrary.GOODS_HORSES));
                        } else {
                            if (tempUnit.getTile() == null) { // -> in Europe
                                menuItem = new JMenuItem(Messages.message("mount") + " ("
                                        + tempUnit.getOwner().getMarket().getBidPrice(Goods.HORSES, 50) + " gold)",
                                        imageLibrary.getGoodsImageIcon(
                                                ImageLibrary.GOODS_HORSES));
                            } else {
                                menuItem = new JMenuItem(Messages.message("mount"), unitLabel.getCanvas()
                                        .getImageProvider().getGoodsImageIcon(ImageLibrary.GOODS_HORSES));
                            }
                        }
                        menuItem.setActionCommand(String.valueOf(UnitLabel.MOUNT));
                        menuItem.addActionListener(unitLabel);
                        menu.add(menuItem);
                    }

                    if (tempUnit.canBeEquippedWithTools()) {
                        if (tempUnit.isPioneer()) {
                            menuItem = new JMenuItem(Messages.message("removeTools"), unitLabel.getCanvas()
                                    .getImageProvider().getGoodsImageIcon(ImageLibrary.GOODS_TOOLS));
                        } else {
                            if (tempUnit.getTile() == null) { // -> in Europe
                                int amount = 100;
                                int price = tempUnit.getOwner().getMarket().getBidPrice(Goods.TOOLS, amount);
                                if (price <= tempUnit.getOwner().getGold()) {
                                    menuItem = new JMenuItem(Messages.message("equipWithTools") + " (" + price
                                            + " gold)", imageLibrary.getGoodsImageIcon(
                                            ImageLibrary.GOODS_TOOLS));
                                } else {
                                    while (price > tempUnit.getOwner().getGold()) {
                                        amount -= 20;
                                        price = tempUnit.getOwner().getMarket().getBidPrice(Goods.TOOLS, amount);
                                    }
                                    menuItem = new JMenuItem(Messages.message("equipWith") + ' ' + amount + " "
                                            + Messages.message("model.goods.Tools") + " (" + price + " "
                                            + Messages.message("gold") + ")", imageLibrary
                                            .getGoodsImageIcon(ImageLibrary.GOODS_TOOLS));

                                }
                            } else {
                                menuItem = new JMenuItem(Messages.message("equipWithTools"), unitLabel.getCanvas()
                                        .getImageProvider().getGoodsImageIcon(ImageLibrary.GOODS_TOOLS));
                            }
                        }
                        menuItem.setActionCommand(String.valueOf(UnitLabel.TOOLS));
                        menuItem.addActionListener(unitLabel);
                        menu.add(menuItem);
                    }

                    if (tempUnit.canBeDressedAsMissionary()) {

                        if (tempUnit.isMissionary()) {
                            menuItem = new JMenuItem(Messages.message("cancelMissionaryStatus"), unitLabel.getCanvas()
                                    .getImageProvider().getGoodsImageIcon(ImageLibrary.GOODS_CROSSES));
                        } else {
                            menuItem = new JMenuItem(Messages.message("blessAsMissionaries"), unitLabel.getCanvas()
                                    .getImageProvider().getGoodsImageIcon(ImageLibrary.GOODS_CROSSES));
                        }
                        menuItem.setActionCommand(String.valueOf(UnitLabel.DRESS));
                        menuItem.addActionListener(unitLabel);
                        menu.add(menuItem);
                    }

                    if (tempUnit.getType() != Unit.INDIAN_CONVERT && tempUnit.getType() != Unit.PETTY_CRIMINAL
                            && tempUnit.getType() != Unit.INDENTURED_SERVANT
                            && tempUnit.getType() != Unit.FREE_COLONIST) {

                        if (menu.getSubElements().length > 0) {
                            menu.addSeparator();
                        }

                        menuItem = new JMenuItem(Messages.message("clearSpeciality"));
                        menuItem.setActionCommand(String.valueOf(UnitLabel.CLEAR_SPECIALITY));
                        menuItem.addActionListener(unitLabel);
                        menu.add(menuItem);
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
                        goodsLabel.getGoods().getType());
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
