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
import java.util.List;
import javax.swing.JPanel;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Unit;

public abstract class InPortPanel extends UnitPanel {

    public InPortPanel(PortPanel portPanel, String name, boolean editable) {
        super(portPanel, name, editable);
    }

    private void selectLabel() {
        // Keep the previous selected unit if possible, otherwise default
        // on the last carrier.
        Unit selectedUnit = getPortPanel().getSelectedUnit();
        UnitLabel lastCarrier = null;
        for (Component component : getComponents()) {
            if (component instanceof UnitLabel) {
                UnitLabel label = (UnitLabel) component;
                if (label.getUnit() == selectedUnit) {
                    getPortPanel().setSelectedUnitLabel(label);
                    return;
                } else if (label.getUnit().isCarrier()) {
                    lastCarrier = label;
                }
            }
        }
        if (lastCarrier != null) {
            getPortPanel().setSelectedUnitLabel(lastCarrier);
        }
        // No revalidate+repaint as this is done in setSelectedUnitLabel
    }

    @Override
    public String getUIClassID() {
        return "InPortPanelUI";
    }

}

