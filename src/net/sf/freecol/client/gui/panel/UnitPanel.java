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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.JPanel;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Unit;

public abstract class UnitPanel extends JPanel implements PropertyChangeListener {

    private static Logger logger = Logger.getLogger(UnitPanel.class.getName());

    private boolean editable;
    private PortPanel portPanel;

    /**
     * The location this UnitPanel visualizes.
     */
    private Location unitLocation;

    /**
     * Describe suffix here.
     */
    private String suffix;

    public UnitPanel(PortPanel portPanel, Location location, String suffix, boolean editable) {
        this.portPanel = portPanel;
        this.editable = editable;
        unitLocation = location;
        setName(suffix);
    }

    public boolean isEditable() {
        return editable;
    }

    public PortPanel getPortPanel() {
        return portPanel;
    }

    /**
     * Set the <code>Location</code> value.
     *
     * @param newLocation The new Location value.
     */
    public final void setUnitLocation(final Location newLocation) {
        removePropertyChangeListeners();
        unitLocation = newLocation;
        addPropertyChangeListeners();
        setName(suffix);
        update();
    }

    /**
     * Get the <code>Suffix</code> value.
     *
     * @return a <code>String</code> value
     */
    public final String getSuffix() {
        return suffix;
    }

    /**
     * Set the <code>Suffix</code> value.
     *
     * @param newSuffix The new Suffix value.
     */
    public final void setSuffix(final String newSuffix) {
        this.suffix = newSuffix;
    }

    public void initialize() {
        if (unitLocation != null) {
            addPropertyChangeListeners();
            update();
        }
    }

    public void update() {
        removeAll();

        for (Unit unit : unitLocation.getUnitList()) {
            if (displays(unit)) {

                UnitLabel unitLabel = new UnitLabel(portPanel.getFreeColClient(), unit, portPanel.getGUI());
                TradeRoute tradeRoute = unit.getTradeRoute();
                if (tradeRoute != null) {
                    unitLabel.setDescriptionLabel(Messages.message(Messages.getLabel(unit))
                                                  + " (" + tradeRoute.getName() + ")");
                }
                if (editable) {
                    unitLabel.setTransferHandler(portPanel.getTransferHandler());
                    unitLabel.addMouseListener(portPanel.getPressListener());
                }
                add(unitLabel);
            }
        }

        selectLabel();
        revalidate();
        repaint();
    }

    /**
     * Select a UnitLabel based on some criterion. By default, do nothing.
     *
     */
    private void selectLabel() {
        // do nothing
    }

    public void cleanup() {
        removePropertyChangeListeners();
    }

    protected void addPropertyChangeListeners() {
        // do nothing
    }

    protected void removePropertyChangeListeners() {
        // do nothing
    }

    public void propertyChange(PropertyChangeEvent event) {
        logger.finest(unitLocation.getLocationName()
                      + " - " + suffix
                      + " change " + event.getPropertyName()
                      + ": " + event.getOldValue()
                      + " -> " + event.getNewValue());
        update();
    }

    /**
     * Returns <code>true</code> if this panel could display the given
     * Unit.
     *
     * @param unit an <code>Unit</code> value
     * @return a <code>boolean</code> value
     */
    public abstract boolean displays(Unit unit);


}

