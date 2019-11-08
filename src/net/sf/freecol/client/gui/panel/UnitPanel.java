/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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
import java.awt.LayoutManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;

import net.sf.freecol.client.gui.label.UnitLabel;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Unit;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * This class provides common functionality for sub-panels of a
 * PortPanel that display UnitLabels.
 */
public abstract class UnitPanel extends MigPanel
    implements PropertyChangeListener {

    private static final Logger logger = Logger.getLogger(UnitPanel.class.getName());

    /** The panel containing the units to display. */
    private PortPanel portPanel;

    /** Whether this panel is editable. */
    private final boolean editable;


    /**
     * Create a unit panel.
     *
     * @param uiClassId An optional L+F class to render this component.
     * @param layout The {@code LayoutManager} to use.
     * @param portPanel A {@code PortPanel} to supply units.
     * @param name An optional name for the panel.
     * @param editable True if the panel can be edited.
     */
    protected UnitPanel(String uiClassId, LayoutManager layout,
                        PortPanel portPanel, String name, boolean editable) {
        super(uiClassId, layout);

        if (portPanel == null) {
            throw new RuntimeException("Null port panel for: " + this);
        }
        this.portPanel = portPanel;
        this.editable = editable;
        setName(name);
    }


    /**
     * Initialize this unit panel.
     */
    protected void initialize() {
        cleanup();
        addPropertyChangeListeners();
        update();
        Unit active = portPanel.getGUI().getActiveUnit();
        if (active != null && active.isCarrier()) setSelectedUnit(active);
    }

    /**
     * Clean up this unit panel.
     */
    protected void cleanup() {
        removePropertyChangeListeners();
    }

    /**
     * Add any property change listeners.
     */
    protected void addPropertyChangeListeners() {}

    /**
     * Remove any property change listeners.
     */
    protected void removePropertyChangeListeners() {}

    /**
     * Update this unit panel.
     */
    protected void update() {
        removeAll();

        if (portPanel != null) {
            for (Unit unit : transform(portPanel.getUnitList(),
                                       u -> accepts(u))) {
                UnitLabel unitLabel
                    = new UnitLabel(portPanel.getFreeColClient(), unit);
                TradeRoute tradeRoute = unit.getTradeRoute();
                if (tradeRoute != null) {
                    unitLabel.setDescriptionLabel(unit
                        .getDescription(Unit.UnitLabelType.NATIONAL)
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
     * Get the port panel that supplies units to this panel.
     *
     * @return The {@code PortPanel}.
     */
    public PortPanel getPortPanel() {
        return portPanel;
    }

    /**
     * Is this panel editable?
     *
     * @return True if the panel is editable.
     */
    public boolean isEditable() {
        return editable;
    }

    /**
     * Can this panel accepts the given Unit.
     *
     * @param unit The {@code Unit} to check.
     * @return True if the unit can be added.
     */
    public abstract boolean accepts(Unit unit);

    /**
     * Select a UnitLabel based on some criterion.
     */
    public abstract void selectLabel();

    /**
     * Select a given unit.
     *
     * @param unit The {@code Unit} to select.
     * @return True if the selection succeeds.
     */
    protected boolean setSelectedUnit(Unit unit) {
        for (Component component : getComponents()) {
            if (component instanceof UnitLabel) {
                UnitLabel label = (UnitLabel)component;
                if (label.getUnit() == unit) {
                    getPortPanel().setSelectedUnitLabel(label);
                    return true;
                }
            }
        }
        return false;
    }
        

    // Interface PropertyChangeListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void propertyChange(PropertyChangeEvent event) {
        logger.finest(getName() + " change " + event.getPropertyName()
                      + ": " + event.getOldValue()
                      + " -> " + event.getNewValue());
        update();
    }


    // Override Component

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeNotify() {
        super.removeNotify();

        removePropertyChangeListeners();
        portPanel = null;
    }
}
