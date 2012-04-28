/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.border.TitledBorder;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Unit;


/**
 * A panel that holds units and goods that represent Units and cargo
 * that are on board the currently selected ship.
 */
public class CargoPanel extends FreeColPanel
    implements PropertyChangeListener {

    private static Logger logger = Logger.getLogger(CargoPanel.class.getName());

    /**
     * The carrier that contains cargo.
     */
    private Unit carrier;

    private final DefaultTransferHandler defaultTransferHandler;

    private final MouseListener pressListener;

    private final TitledBorder border;


    /**
     * Creates this CargoPanel.
     *
     * @param parent The parent Canvas that holds this CargoPanel
     * @param withTitle boolean
     */
    public CargoPanel(FreeColClient freeColClient, GUI gui, boolean withTitle) {
        super(freeColClient, gui);

        carrier = null;
        defaultTransferHandler = new DefaultTransferHandler(freeColClient, gui, this);
        pressListener = new DragListener(getFreeColClient(), gui, this);

        if (withTitle) {
            border = BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                                                      Messages.message("cargoOnCarrier"));
        } else {
            border = null;
        }

        setBorder(border);
        initialize();
    }



    /**
     * Whether this panel is active.
     *
     * @return boolean <b>true</b> == active
     */
    public boolean isActive() {
        return carrier != null;
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
        removePropertyChangeListeners();
        this.carrier = newCarrier;
        addPropertyChangeListeners();
        update();
    }

    /**
     * Initialize this CargoPanel.
     */
    public void initialize() {
        update();
    }

    /**
     * Clean up this CargoPanel.
     */
    public void cleanup() {
        removePropertyChangeListeners();
    }

    /**
     * Update this CargoPanel.
     */
    public void update() {
        removeAll();

        if (carrier != null) {

            Iterator<Unit> unitIterator = carrier.getUnitIterator();
            while (unitIterator.hasNext()) {
                Unit unit = unitIterator.next();

                UnitLabel label = new UnitLabel(getFreeColClient(), unit, getGUI());
                if (isEditable()) {
                    label.setTransferHandler(defaultTransferHandler);
                    label.addMouseListener(pressListener);
                }

                add(label);
            }

            Iterator<Goods> goodsIterator = carrier.getGoodsIterator();
            while (goodsIterator.hasNext()) {
                Goods g = goodsIterator.next();

                GoodsLabel label = new GoodsLabel(g, getGUI());
                if (isEditable()) {
                    label.setTransferHandler(defaultTransferHandler);
                    label.addMouseListener(pressListener);
                }

                add(label);
            }
        }
        updateTitle();
        revalidate();
        repaint();
    }

    /**
     * Update the title of this CargoPanel.
     */
    private void updateTitle() {
        // sanitation
        if (border == null) {
            return;
        }

        if (carrier == null) {
            border.setTitle(Messages.message("cargoOnCarrier"));
        } else {
            int spaceLeft = carrier.getSpaceLeft();
            StringTemplate t = StringTemplate.template("cargoOnCarrierLong")
                .addStringTemplate("%name%", Messages.getLabel(carrier))
                .addAmount("%space%", spaceLeft);
            border.setTitle(Messages.message(t));
        }
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
        }

        if (editState) {
            if (comp instanceof UnitLabel) {
                Unit unit = ((UnitLabel) comp).getUnit();
                if (carrier.canAdd(unit)) {
                    Container oldParent = comp.getParent();
                    if (getController().boardShip(unit, carrier)) {
                        ((UnitLabel) comp).setSmall(false);
                        if (oldParent != null) oldParent.remove(comp);
                        update();
                        return comp;
                    }
                }
            } else if (comp instanceof GoodsLabel) {
                Goods goods = ((GoodsLabel) comp).getGoods();
                int loadableAmount = carrier.getLoadableAmount(goods.getType());
                if (loadableAmount == 0) {
                    return null;
                } else if (loadableAmount > goods.getAmount()) {
                    loadableAmount = goods.getAmount();
                }
                Goods goodsToAdd = new Goods(goods.getGame(), goods.getLocation(),
                                             goods.getType(), loadableAmount);
                goods.setAmount(goods.getAmount() - loadableAmount);
                getController().loadCargo(goodsToAdd, carrier);
                update();
                return comp;
            } else if (comp instanceof MarketLabel) {
                MarketLabel label = (MarketLabel) comp;
                Player player = carrier.getOwner();
                if (player.canTrade(label.getType())) {
                    getController().buyGoods(label.getType(), label.getAmount(), carrier);
                    getController().nextModelMessage();
                    update();
                    return comp;
                } else {
                    getController().payArrears(label.getType());
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
        if (comp instanceof UnitLabel) {
            Unit unit = ((UnitLabel) comp).getUnit();
            getController().leaveShip(unit);
            update();
        } else if (comp instanceof GoodsLabel) {
            Goods g = ((GoodsLabel) comp).getGoods();
            getController().unloadCargo(g, false);
            update();
        }
    }

    public void addPropertyChangeListeners() {
        if (carrier != null) {
            carrier.addPropertyChangeListener(Unit.CARGO_CHANGE, this);
            carrier.getGoodsContainer().addPropertyChangeListener(this);
        }
    }

    public void removePropertyChangeListeners() {
        if (carrier != null) {
            carrier.removePropertyChangeListener(Unit.CARGO_CHANGE, this);
            carrier.getGoodsContainer().removePropertyChangeListener(this);
        }
    }

    public void propertyChange(PropertyChangeEvent event) {
        logger.finest("CargoPanel change " + event.getPropertyName()
                      + ": " + event.getOldValue()
                      + " -> " + event.getNewValue());
        update();
    }

    @Override
    public String getUIClassID() {
        return "CargoPanelUI";
    }
}
