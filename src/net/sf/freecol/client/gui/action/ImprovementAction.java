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

package net.sf.freecol.client.gui.action;

import java.awt.event.ActionEvent;

import javax.swing.KeyStroke;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;

/**
 * An action for using the active unit to plow/clear a forest.
 */
public class ImprovementAction extends UnitAction {

    public ImprovementActionType iaType;
    
    int actionID;

    /**
     * Creates this action.
     * 
     * @param freeColClient The main controller object for the client.
     */
    public ImprovementAction(FreeColClient freeColClient, ImprovementActionType iaType) {
        super(freeColClient, iaType.getId());
        putValue(NAME, iaType.getNames().get(0));
        setAccelerator(KeyStroke.getKeyStroke(iaType.getAccelerator(), 0));
        this.iaType = iaType;
        actionID = -1;
        updateValues(0);
    }

    /**
     * Updates this action to one of the possible actions for this ImprovementAction.
     * 
     * @param newActionID The new action.
     */
    private void updateValues(int newActionID) {
        if (actionID == newActionID) {
            return;
        }
        actionID = newActionID;
        addImageIcons(iaType.getImageIDs().get(actionID));
        putValue(NAME, Messages.message(iaType.getNames().get(actionID)));
    }

    /**
     * Updates the "enabled"-status with the value returned by
     * {@link #shouldBeEnabled} and updates the name of the action.
     */
    public void update() {
        super.update();

        GUI gui = getFreeColClient().getGUI();
        if (gui != null) {
            Unit selectedOne = getFreeColClient().getGUI().getActiveUnit();
            if (enabled && selectedOne != null && selectedOne.getTile() != null) {
                Tile tile = selectedOne.getTile();
                int newActionID = 0;
                for (TileImprovementType impType : iaType.getImpTypes()) {
                	// Make sure that the tile accepts this improvement 
                	// and that the worker can do the improvement
                	if (!impType.isTileAllowed(tile) || !impType.isWorkerAllowed(selectedOne)) {
                        continue;
                    }
                    newActionID = iaType.getImpTypes().indexOf(impType);
                    break;
                }
                updateValues(newActionID);
            } else {
                updateValues(0);
            }
        }
    }

    /**
     * Checks if this action should be enabled.
     * 
     * @return <code>false</code> if there is no active unit or if the unit
     *         cannot plow/clear forest.
     */
    protected boolean shouldBeEnabled() {
        if (!super.shouldBeEnabled()) {
            return false;
        }

        GUI gui = getFreeColClient().getGUI();
        if (gui == null)
            return false;

        Unit selectedOne = getFreeColClient().getGUI().getActiveUnit();
        if (selectedOne == null || !selectedOne.checkSetState(UnitState.IMPROVING))
            return false;

        Tile tile = selectedOne.getTile();
        if (tile == null)
             return false;
        
        // Check if there is an ImprovementType that can be performed by this unit on this tile
        for (TileImprovementType impType : iaType.getImpTypes()) {
        	// Make sure that the tile accepts this improvement 
        	// and that the worker can do the improvement
            if (!impType.isTileAllowed(tile) || !impType.isWorkerAllowed(selectedOne)) {
                continue;
            }
            return true;
        }
        // Since nothing suitable was found, disable this ImprovementAction.
        return false;
    }

    /**
     * Applies this action.
     * 
     * @param e The <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent e) {
        getFreeColClient().getInGameController().changeWorkImprovementType(getFreeColClient().getGUI().getActiveUnit(),
                                                                iaType.getImpTypes().get(actionID));
        getFreeColClient().getInGameController().nextActiveUnit();
    }
}
