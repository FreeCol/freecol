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

import java.util.List;
import javax.swing.JPanel;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Unit;

public abstract class InPortPanel extends JPanel {

    private boolean editable;
    private PortPanel portPanel;

    public InPortPanel(PortPanel portPanel, boolean editable) {
        this.portPanel = portPanel;
        this.editable = editable;
    }

    public void initialize(List<Unit> units) {
        removeAll();

        UnitLabel lastCarrier = null;
        UnitLabel prevCarrier = null;
        for (Unit unit : units) {
            if (accepts(unit)) {

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

                lastCarrier = unitLabel;
                if (portPanel.getSelectedUnit() == unit) prevCarrier = unitLabel;
            }
        }

        // Keep the previous selected unit if possible, otherwise default
        // on the last carrier.
        portPanel.setSelectedUnitLabel((prevCarrier != null) ? prevCarrier
                                       : (lastCarrier != null) ? lastCarrier
                                       : null);
        // No revalidate+repaint as this is done in setSelectedUnitLabel
    }

    /**
     * Returns <code>true</code> if this panel accepts the given Unit.
     *
     * @param unit an <code>Unit</code> value
     * @return a <code>boolean</code> value
     */
    public abstract boolean accepts(Unit unit);


    @Override
    public String getUIClassID() {
        return "InPortPanelUI";
    }

}

