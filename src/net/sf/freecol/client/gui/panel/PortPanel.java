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
import java.awt.LayoutManager;
import java.awt.event.MouseListener;

import javax.swing.TransferHandler;

import java.util.List;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.model.Unit;


/**
 * This class provides common functionality for panels that display
 * ports, such as the ColonyPanel and the EuropePanel.  This includes
 * an InPortPanel for displaying the carriers in port, and a
 * CargoPanel for displaying the cargo aboard that carrier.
 */
public abstract class PortPanel extends FreeColPanel {

    protected CargoPanel cargoPanel;
    protected InPortPanel inPortPanel;
    protected UnitLabel selectedUnitLabel;
    protected DefaultTransferHandler defaultTransferHandler;
    protected MouseListener pressListener;


    /**
     * Create a new port panel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param layout The <code>LayoutManager</code> to be used.
     */
    public PortPanel(FreeColClient freeColClient, LayoutManager layout) {
        super(freeColClient, layout);

        this.selectedUnitLabel = null;
    }


    /**
     * Get the cargo panel.
     *
     * @return The cargo panel.
     */
    public final CargoPanel getCargoPanel() {
        return cargoPanel;
    }

    /**
     * Get the currently select unit.
     *
     * @return The currently select unit.
     */
    public Unit getSelectedUnit() {
        return (selectedUnitLabel == null) ? null
            : selectedUnitLabel.getUnit();
    }

    /**
     * Select a given unit.
     *
     * @param unit The <code>Unit</code> to select.
     * @return True if the selection succeeds.
     */
    public boolean setSelectedUnit(Unit unit) {
        for (Component component : getComponents()) {
            if (component instanceof UnitLabel) {
                UnitLabel label = (UnitLabel)component;
                if (label.getUnit() == unit) {
                    setSelectedUnitLabel(label);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get the currently select unit label.
     *
     * @return The currently select unit label.
     */
    public UnitLabel getSelectedUnitLabel() {
        return selectedUnitLabel;
    }

    /**
     * Set the selected unit label.
     *
     * @param label The unit label to select.
     */
    public void setSelectedUnitLabel(UnitLabel label) {
        selectedUnitLabel = label;
    }

    /**
     * Get the press listener.  Associated UnitPanels often add this
     * mouse listener to their contained UnitLabels.
     *
     * @return The press listener.
     */
    public MouseListener getPressListener() {
        return pressListener;
    }

    /**
     * Get the units present in this port.
     *
     * @return A list of <code>Unit</code>s.
     */
    public abstract List<Unit> getUnitList();


    // Override JComponent

    /**
     * {@inheritDoc}
     */
    @Override
    public TransferHandler getTransferHandler() {
        return defaultTransferHandler;
    }


    // Override Component
    /**
     * {@inheritDoc}
     */
    @Override
    public void removeNotify() {
        super.removeNotify();

        removeAll();
        cargoPanel = null;
        inPortPanel = null;
        defaultTransferHandler = null;
        pressListener = null;
        selectedUnitLabel = null;
    }
}
