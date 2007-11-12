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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.border.TitledBorder;
import javax.swing.BorderFactory;
import javax.swing.JPanel;

import net.sf.freecol.client.FreeColClient;
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
public final class CargoPanel extends FreeColPanel {

    /**
     * The carrier that contains cargo.
     */
    private Unit carrier;

    private List<LocationChangeListener> locationChangeListeners = new ArrayList<LocationChangeListener>();

    private final DefaultTransferHandler defaultTransferHandler;

    private final MouseListener pressListener;

    private final Canvas parent;

    private final FreeColClient freeColClient;

    private InGameController inGameController;

    private final TitledBorder border;

    /**
     * Describe editable here.
     */
    private boolean editable = true;

    /**
     * Creates this CargoPanel.
     * 
     * @param colonyPanel The panel that holds this CargoPanel.
     */
    public CargoPanel(Canvas parent, FreeColClient freeColClient) {
        this.parent = parent;
        this.freeColClient = freeColClient;
        this.inGameController = freeColClient.getInGameController();

        defaultTransferHandler = new DefaultTransferHandler(parent, this);
        pressListener = new DragListener(this);

        border = BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                                                  Messages.message("cargoOnCarrier"));

        setBorder(border);
        initialize();
    }

    @Override
    public String getUIClassID() {
        return "CargoPanelUI";
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

                UnitLabel label = new UnitLabel(unit, parent);
                if (isEditable()) {
                    label.setTransferHandler(defaultTransferHandler);
                    label.addMouseListener(pressListener);
                }

                add(label);
            }

            Iterator<Goods> goodsIterator = carrier.getGoodsIterator();
            while (goodsIterator.hasNext()) {
                Goods g = goodsIterator.next();

                GoodsLabel label = new GoodsLabel(g, parent);
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
        if (carrier == null) {
            border.setTitle(Messages.message("cargoOnCarrier"));
        } else {
            border.setTitle(Messages.message("cargoOnCarrierLong", 
                                             "%name%", carrier.getName(),
                                             "%space%", String.valueOf(carrier.getSpaceLeft())));
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
            if (comp instanceof UnitLabel) {
                Container oldParent = comp.getParent();
                Unit unit = ((UnitLabel) comp).getUnit();
                if (carrier.canAdd(unit)) {
                    ((UnitLabel) comp).setSmall(false);
                    if (inGameController.boardShip(unit, carrier)) {
                        for (LocationChangeListener listener : locationChangeListeners) {
                            listener.locationChanged((UnitLabel) comp, oldParent, this);
                        }
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
                Container oldParent = comp.getParent();
                Goods g = ((GoodsLabel) comp).getGoods();
                
                int newAmount = Math.min(g.getAmount(), carrier.getLoadableAmount(g.getType()));
                if (newAmount == 0) {
                    return null;
                }

                Goods goodsToAdd = new Goods(freeColClient.getGame(), g.getLocation(), g.getType(), newAmount);
                g.setAmount(g.getAmount() - newAmount);

                ((GoodsLabel) comp).setSmall(false);
                inGameController.loadCargo(goodsToAdd, carrier);

                for (LocationChangeListener listener : locationChangeListeners) {
                    listener.locationChanged((GoodsLabel) comp, oldParent, this);
                }

                add(comp);
                updateTitle();
                return comp;
            } else if (comp instanceof MarketLabel) {
                MarketLabel label = (MarketLabel) comp;
                Player player = freeColClient.getMyPlayer();
                if (player.canTrade(label.getType())) {
                    inGameController.buyGoods(label.getType(), label.getAmount(), carrier);
                    inGameController.nextModelMessage();
                    add(comp);
                    updateTitle();
                    return comp;
                } else {
                    inGameController.payArrears(label.getType());
                    return null;
                }
            } else {
                //logger.warning("An invalid component got dropped on this CargoPanel.");
                return null;
            }
        }

        return null;
    }


    @Override
    public void remove(Component comp) {
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

    /**
     * Adds a loading listener for notification of any loading
     *
     * @param listener the listener
     */
    public void addLocationChangeListener(LocationChangeListener listener) {
        locationChangeListeners.add(listener);
    }

    /**
     * Removes a loading listener
     *
     * @param listener the listener
     */
    public void removeLocationChangeListener(LocationChangeListener listener) {
        locationChangeListeners.remove(listener);
    }

    /**
     * Returns an array of all the LocationChangeListener added to this Market.
     *
     * @return all of the LocationChangeListener added or an empty array if no
     * listeners have been added
     */
    public LocationChangeListener[] getLocationChangeListeners() {
        return locationChangeListeners.toArray(new LocationChangeListener[0]);
    }
}

