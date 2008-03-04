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

import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseListener;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.border.TitledBorder;
import javax.swing.JPanel;

import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;

/**
 * A panel that holds units and goods that represent Units and cargo
 * that are on board the currently selected ship.
 */
public class CargoPanel extends FreeColPanel {

    /**
     * The carrier that contains cargo.
     */
    private Unit carrier;

    private final DefaultTransferHandler defaultTransferHandler;

    private final MouseListener pressListener;

    private final TitledBorder border;

    /**
     * Describe editable here.
     */
    private boolean editable = true;

    /**
     * Describe parentPanel here.
     */
    private JPanel parentPanel;

    /**
     * Creates this CargoPanel.
     * 
     * @param parent The parent Canvas that holds this CargoPanel.
     */
    public CargoPanel(Canvas parent, boolean withTitle) {
        super(parent);

        defaultTransferHandler = new DefaultTransferHandler(parent, this);
        pressListener = new DragListener(this);

        if (withTitle) {
            border = BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                                                      Messages.message("cargoOnCarrier"));
        } else {
            border = null;
        }

        setBorder(border);
        initialize();
    }

    @Override
    public String getUIClassID() {
        return "CargoPanelUI";
    }

    /**
     * Get the <code>ParentPanel</code> value.
     *
     * @return a <code>JPanel</code> value
     */
    public final JPanel getParentPanel() {
        return parentPanel;
    }

    /**
     * Set the <code>ParentPanel</code> value.
     *
     * @param newParentPanel The new ParentPanel value.
     */
    public final void setParentPanel(final JPanel newParentPanel) {
        this.parentPanel = newParentPanel;
    }

    /**
     * Get the <code>Carrier</code> value.
     *
     * @return an <code>Unit</code> value
     */
    public Unit getCarrier() {
        return carrier;
    }

    /**
     * Set the <code>Carrier</code> value.
     *
     * @param newCarrier The new Carrier value.
     */
    public void setCarrier(final Unit newCarrier) {
        this.carrier = newCarrier;
        initialize();
    }

    /**
     * Get the <code>Editable</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isEditable() {
        return editable;
    }

    /**
     * Set the <code>Editable</code> value.
     *
     * @param newEditable The new Editable value.
     */
    public void setEditable(final boolean newEditable) {
        this.editable = newEditable;
    }

    public void initialize() {
        removeAll();

        if (carrier != null) {

            Iterator<Unit> unitIterator = carrier.getUnitIterator();
            while (unitIterator.hasNext()) {
                Unit unit = unitIterator.next();

                UnitLabel label = new UnitLabel(unit, getCanvas());
                if (isEditable()) {
                    label.setTransferHandler(defaultTransferHandler);
                    label.addMouseListener(pressListener);
                }

                add(label);
            }

            Iterator<Goods> goodsIterator = carrier.getGoodsIterator();
            while (goodsIterator.hasNext()) {
                Goods g = goodsIterator.next();

                GoodsLabel label = new GoodsLabel(g, getCanvas());
                if (isEditable()) {
                    label.setTransferHandler(defaultTransferHandler);
                    label.addMouseListener(pressListener);
                }

                add(label);
            }
        }

        updateTitle();
    }

    private void updateTitle() {
        if (border != null) {
            if (carrier == null) {
                border.setTitle(Messages.message("cargoOnCarrier"));
            } else {
                border.setTitle(Messages.message("cargoOnCarrierLong", 
                                                 "%name%", carrier.getName(),
                                                 "%space%", String.valueOf(carrier.getSpaceLeft())));
            }
        }
    }

    public boolean isActive() {
        return (carrier != null);
    }

    /**
     * Adds a component to this CargoPanel and makes sure that the unit or
     * good that the component represents gets modified so that it is on
     * board the currently selected ship.
     * 
     * @param comp The component to add to this CargoPanel.
     * @param editState Must be set to 'true' if the state of the component
     *            that is added (which should be a dropped component
     *            representing a Unit or good) should be changed so that the
     *            underlying unit or goods are on board the currently
     *            selected ship.
     * @return The component argument.
     */
    public Component add(Component comp, boolean editState) {
        if (carrier == null) {
            return null;
        } else if (editState) {
            InGameController inGameController = getCanvas().getClient().getInGameController();
            if (comp instanceof UnitLabel) {
                Container oldParent = comp.getParent();
                Unit unit = ((UnitLabel) comp).getUnit();
                if (carrier.canAdd(unit)) {
                    ((UnitLabel) comp).setSmall(false);
                    if (inGameController.boardShip(unit, carrier)) {
                        if (oldParent != null) {
                            oldParent.remove(comp);
                            oldParent.repaint();
                        }
                        add(comp);
                        updateTitle();
                        return comp;
                    }
                }
            } else if (comp instanceof GoodsLabel) {
                Goods goods = ((GoodsLabel) comp).getGoods();

                int spaceLeft = carrier.getSpaceLeft();
                int loadableAmount = carrier.getLoadableAmount(goods.getType());
                if (loadableAmount == 0) {
                    return null;
                } else if (loadableAmount > goods.getAmount()) {
                    loadableAmount = goods.getAmount();
                }
                Goods goodsToAdd = new Goods(goods.getGame(), goods.getLocation(), goods.getType(), loadableAmount);
                goods.setAmount(goods.getAmount() - loadableAmount);
                inGameController.loadCargo(goodsToAdd, carrier);
                initialize();
                Container oldParent = comp.getParent();
                if (oldParent instanceof ColonyPanel.WarehousePanel) {
                    ((ColonyPanel.WarehousePanel) oldParent).initialize();
                } else {
                    oldParent.remove(comp);
                }
                return comp;
            } else if (comp instanceof MarketLabel) {
                MarketLabel label = (MarketLabel) comp;
                Player player = carrier.getOwner();
                if (player.canTrade(label.getType())) {
                    inGameController.buyGoods(label.getType(), label.getAmount(), carrier);
                    inGameController.nextModelMessage();
                    initialize();
                    updateTitle();
                    return comp;
                } else {
                    inGameController.payArrears(label.getType());
                    return null;
                }
            } else {
                return null;
            }
        } else {
            super.add(comp);
        }

        return null;
    }


    @Override
    public void remove(Component comp) {
        InGameController inGameController = getCanvas().getClient().getInGameController();
        if (comp instanceof UnitLabel) {
            Unit unit = ((UnitLabel) comp).getUnit();
            inGameController.leaveShip(unit);
            updateTitle();
            super.remove(comp);
        } else if (comp instanceof GoodsLabel) {
            Goods g = ((GoodsLabel) comp).getGoods();
            inGameController.unloadCargo(g);
            updateTitle();
            super.remove(comp);
        }
    }

}

