/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

import net.sf.freecol.common.model.Unit;


/**
 * This class displays the carriers present in a port.
 *
 * @see PortPanel
 */
public abstract class InPortPanel extends UnitPanel {

    /**
     * Create an InPortPanel.
     *
     * @param portPanel The <code>PortPanel</code> to enclose.
     * @param name An optional name for the panel.
     * @param editable Is this panel editable?
     */
    public InPortPanel(PortPanel portPanel, String name, boolean editable) {
        super(portPanel, name, editable);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void selectLabel() {
        // Keep the previous selected unit if possible, otherwise default
        // on the last carrier.
        PortPanel portPanel = getPortPanel();
        if (portPanel == null) return;
        Unit selectedUnit = portPanel.getSelectedUnit();
        UnitLabel lastCarrier = null;
        for (Component component : getComponents()) {
            if (component instanceof UnitLabel) {
                UnitLabel label = (UnitLabel)component;
                Unit unit = label.getUnit();
                if (unit == selectedUnit) {
                    portPanel.setSelectedUnitLabel(label);
                    return;
                }
                if (unit.isCarrier() && unit.getTradeRoute() == null) {
                    lastCarrier = label;
                }
            }
        }
        if (lastCarrier != null) {
            portPanel.setSelectedUnitLabel(lastCarrier);
        }
        // No revalidate+repaint as this is done in setSelectedUnitLabel
    }


    // Override JLabel

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUIClassID() {
        return "InPortPanelUI";
    }
}
