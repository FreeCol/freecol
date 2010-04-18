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
import java.util.logging.Logger;

import javax.swing.KeyStroke;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.model.Unit;

/**
 * An action for using the active unit to build a colony.
 */
public class BuildColonyAction extends MapboardAction {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(BuildColonyAction.class.getName());

    public static final String id = "buildColonyAction";


    /**
     * Creates this action.
     * 
     * @param freeColClient The main controller object for the client.
     */
    BuildColonyAction(FreeColClient freeColClient) {
        super(freeColClient, "unit.state.7", null, KeyStroke.getKeyStroke('B', 0));
        addImageIcons("build");
    }

    /**
     * Checks if this action should be enabled.
     * 
     * @return <code>false</code> if there is no active unit or the active
     *         unit cannot build a colony, and <code>true</code> otherwise.
     */
    protected boolean shouldBeEnabled() {
        if (!super.shouldBeEnabled()) {
            return false;
        }
        Unit selectedOne = getFreeColClient().getGUI().getActiveUnit();
        return selectedOne != null && selectedOne.getTile() != null
            && (selectedOne.canBuildColony()
                || (selectedOne.getTile().getColony() != null
                    // exclude artillery, ships, etc.
                    && selectedOne.getType().hasSkill()));
    }

    /**
     * Returns the id of this <code>Option</code>.
     * 
     * @return "buildColonyAction"
     */
    public String getId() {
        return id;
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
