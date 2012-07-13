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
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Unit;


/**
 * This class provides common functionality for sub-panels of a
 * PortPanel that display UnitLabels.
 */
public abstract class UnitPanel extends JPanel implements PropertyChangeListener {

    private static Logger logger = Logger.getLogger(UnitPanel.class.getName());

    private boolean editable;
    private PortPanel portPanel;

    public UnitPanel(PortPanel portPanel, String name, boolean editable) {
        this.portPanel = portPanel;
        this.editable = editable;
        setName(name);
    }

    public boolean isEditable() {
        return editable;
    }

    public PortPanel getPortPanel() {
        return portPanel;
    }

    public void initialize() {
        addPropertyChangeListeners();
        update();
    }

    public void update() {
        removeAll();

        for (Unit unit : portPanel.getUnitList()) {
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
    public void selectLabel() {
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
        logger.finest(getName() + " change " + event.getPropertyName()
                      + ": " + event.getOldValue()
                      + " -> " + event.getNewValue());
        update();
    }

    /**
     * Returns <code>true</code> if this panel accepts the given Unit.
     *
     * @param unit an <code>Unit</code> value
     * @return a <code>boolean</code> value
     */
    public abstract boolean accepts(Unit unit);


}

