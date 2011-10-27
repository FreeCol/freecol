/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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


import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.Unit;


/**
 * An action for using the active unit to plow/clear a forest.
 */
public class ImprovementAction extends UnitAction {

    private TileImprovementType improvement;
    
    /**
     * Creates this action.
     * 
     * @param freeColClient The main controller object for the client.
     * @param improvement <code>TileImprovementType</code> ??
     */
    public ImprovementAction(FreeColClient freeColClient, TileImprovementType improvement) {
        super(freeColClient, improvement.getShortId() + "Action");
        this.improvement = improvement;
        addImageIcons(improvement.getShortId());
    }

    /**
     * Checks if this action should be enabled.
     * 
     * @return <code>false</code> if there is no active unit or if the unit
     *         cannot plow/clear forest.
     */
    @Override
    protected boolean shouldBeEnabled() {
        if (super.shouldBeEnabled()) {
            Unit selectedUnit = getFreeColClient().getMapViewer().getActiveUnit();
            Tile tile = selectedUnit.getTile();
            return selectedUnit.checkSetState(Unit.UnitState.IMPROVING)
                && improvement.isTileAllowed(tile)
                && improvement.isWorkerAllowed(selectedUnit);
        }
        return false;
    }

    /**
     * Applies this action.
     * 
     * @param e The <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent e) {
        getFreeColClient().getInGameController()
            .changeWorkImprovementType(getFreeColClient().getMapViewer().getActiveUnit(),
                                       improvement);
        getFreeColClient().getInGameController().nextActiveUnit();
    }
}
