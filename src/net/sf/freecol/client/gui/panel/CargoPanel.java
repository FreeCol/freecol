/**
 *  Copyright (C) 2002-2024   The FreeCol Team
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;

import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.label.CargoLabel;
import net.sf.freecol.client.gui.label.GoodsLabel;
import net.sf.freecol.client.gui.label.UnitLabel;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Unit;

/**
 * A panel that holds units and goods that represent Units and cargo
 * that are on board the currently selected ship.
 */
public class CargoPanel extends FreeColPanel
        implements DropTarget, PropertyChangeListener {

    private static final Logger logger = Logger.getLogger(CargoPanel.class.getName());

    /** The carrier that contains cargo. */
    private Unit carrier;

    private DefaultTransferHandler defaultTransferHandler = null;
    private boolean withStyling; 

    /**
     * Creates this CargoPanel.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param withTitle     Should the panel have a title?
     */
    public CargoPanel(FreeColClient freeColClient, boolean withTitle, boolean withStyling) {
        super(freeColClient, "CargoPanelUI",
                new MigLayout("wrap 6, gap 0 0, insets 0", "[79!, center][79!, center][79!, center][79!, center][79!, center][79!, center]", "[100!]"));

        this.carrier = null;
        this.defaultTransferHandler = new DefaultTransferHandler(getFreeColClient(), this);
        this.withStyling = withStyling;

        if (withTitle) {
            setBorder(Utility.localizedBorder("cargoOnCarrier"));
        } else {
            setBorder(null);
        }
    }

    /**
     * Initialize this CargoPanel.
     */
    public void initialize() {
        addPropertyChangeListeners();
        update();
    }

    /**
     * Clean up this CargoPanel.
     */
    public void cleanup() {
        removePropertyChangeListeners();
    }

    protected void addPropertyChangeListeners() {
        if (carrier != null) {
            carrier.addPropertyChangeListener(Unit.CARGO_CHANGE, this);
            carrier.getGoodsContainer().addPropertyChangeListener(this);
        }
    }

    protected void removePropertyChangeListeners() {
        if (carrier != null) {
            carrier.removePropertyChangeListener(Unit.CARGO_CHANGE, this);
            carrier.getGoodsContainer().removePropertyChangeListener(this);
        }
    }

    /**
     * Update this CargoPanel.
     */
    public void update() {
        removeAll();

        if (carrier != null) {
            final FreeColClient fcc = getFreeColClient();
            DragListener dl = new DragListener(fcc, this);
            for (Unit unit : carrier.getUnitList()) {
                UnitLabel label = new UnitLabel(fcc, unit);
                if (isEditable()) {
                    label.setTransferHandler(defaultTransferHandler);
                    label.addMouseListener(dl);
                }
                add(label);
            }

            for (Goods g : carrier.getGoodsList()) {
                GoodsLabel label = new GoodsLabel(fcc, g);
                if (withStyling) {
                    label.setHorizontalTextPosition(JLabel.CENTER);
                    label.setVerticalTextPosition(JLabel.BOTTOM);
                    label.setForeground(Color.WHITE);
                }
                if (isEditable()) {
                    label.setTransferHandler(defaultTransferHandler);
                    label.addMouseListener(dl);
                }
                add(label);
            }
        }
        updateTitle();
        revalidate();
        repaint();
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
     * Get the carrier unit.
     *
     * @return The carrier {@code Unit}.
     */
    public Unit getCarrier() {
        return carrier;
    }

    /**
     * Set the carrier unit.
     *
     * @param newCarrier The new carrier {@code Unit}.
     */
    public void setCarrier(final Unit newCarrier) {
        if (newCarrier != carrier) {
            cleanup();
            this.carrier = newCarrier;
            initialize();
        }
    }

    /**
     * Update the title of this CargoPanel.
     */
    private void updateTitle() {
        if (getBorder() == null) {
            return;
        }
        Utility.localizeBorder(this, (carrier == null)
                ? StringTemplate.key("cargoOnCarrier")
                : StringTemplate.template("cargoPanel.cargoAndSpace")
                        .addStringTemplate("%name%",
                                carrier.getLabel(Unit.UnitLabelType.NATIONAL))
                        .addAmount("%space%", carrier.getSpaceLeft()));
    }

    // Interface DropTarget

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean accepts(Goods goods) {
        return carrier != null && carrier.canAdd(goods);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean accepts(Unit unit) {
        return carrier != null && carrier.canAdd(unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component add(Component comp, boolean editState) {
        if (carrier != null && comp instanceof CargoLabel && editState
                && ((CargoLabel) comp).addCargo(comp, carrier, this)) {
            return comp;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int suggested(GoodsType type) {
        return carrier.getLoadableAmount(type);
    }

    // Interface PropertyChangeListener

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        logger.finest("CargoPanel change " + event.getPropertyName()
                + ": " + event.getOldValue()
                + " -> " + event.getNewValue());
        update();
    }

    // Override Container

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(Component comp) {
        if (comp instanceof CargoLabel) {
            ((CargoLabel) comp).removeCargo(comp, this);
        }
    }

    // Override Component

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeNotify() {
        super.removeNotify();

        removeAll();
        removePropertyChangeListeners();

        defaultTransferHandler = null;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (!withStyling) {
            return;
        }
        
        final Dimension size = getSize();
        
        final int cargoHoldsPerRow = 6;
        final BufferedImage available = getImageLibrary().getScaledCargoHold(true);
        final BufferedImage unavailable = getImageLibrary().getScaledCargoHold(false);
        
        final int totalAvailableHolds = (carrier != null) ? carrier.getCargoCapacity() : 0;
        final int rows = totalAvailableHolds / cargoHoldsPerRow + ((totalAvailableHolds % cargoHoldsPerRow > 0) ? 1 : 0);

        if (rows == 0) {
            int x = 0;
            while (x < size.width) {
                g.drawImage(unavailable, x, 0, null);
                x += unavailable.getWidth();
            }
            g.drawImage(unavailable, x, 0, null);
            return;
        }
        
        int y = 0;
        for (int row=0; row<rows; row++) {
            final int availableHolds = (row < rows - 1) ? cargoHoldsPerRow : totalAvailableHolds - cargoHoldsPerRow * row;
            int x = 0;
            for (int i=0; i<availableHolds; i++) {
                g.drawImage(available, x, y, null);
                x += available.getWidth();
            }
            while (x < size.width) {
                g.drawImage(unavailable, x, y, null);
                x += unavailable.getWidth();
            }
            g.drawImage(unavailable, x, y, null);
            y += available.getHeight();
        }        
    }
}
