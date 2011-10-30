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
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.common.model.Unit;

/**
 * An action for using the active unit to build a colony.
 */
public class BuildColonyAction extends UnitAction {

    public static final String id = "buildColonyAction";


    /**
     * Creates this action.
     *
     * @param freeColClient The main controller object for the client.
     * @param gui 
     */
    BuildColonyAction(FreeColClient freeColClient, GUI gui) {
        super(freeColClient, gui, id);
        addImageIcons("build");
    }

    /**
     * Checks if this action should be enabled.
     *
     * @return <code>false</code> if there is no active unit or the active
     *         unit cannot build a colony, and <code>true</code> otherwise.
     */
    @Override
    protected boolean shouldBeEnabled() {
        if (!super.shouldBeEnabled()) {
            return false;
        }
        Unit selectedOne = getFreeColClient().getMapViewer().getActiveUnit();
        return selectedOne != null && selectedOne.getTile() != null
            && (selectedOne.canBuildColony()
                || (selectedOne.getTile().getColony() != null
                    // exclude artillery, ships, etc.
                    && selectedOne.getType().hasSkill()));
    }

    /**
     * Applies this action.
     *
     * @param e The <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent e) {
        getFreeColClient().getInGameController().buildColony();
    }
}
