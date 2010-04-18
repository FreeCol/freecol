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
import net.sf.freecol.common.model.Unit.UnitState;

/**
 * An action for skipping the active unit.
 */
public class SkipUnitAction extends MapboardAction {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(SkipUnitAction.class.getName());

    public static final String id = "skipUnitAction";


    /**
     * Creates this action.
     * 
     * @param freeColClient The main controller object for the client.
     */
    SkipUnitAction(FreeColClient freeColClient) {
        super(freeColClient, "unit.state.1", null, KeyStroke.getKeyStroke(' ', 0));
        addImageIcons("done");
    }

    /**
     * Checks if this action should be enabled.
     * 
     * @return <code>false</code> if there is no active unit.
     */
    protected boolean shouldBeEnabled() {
        return super.shouldBeEnabled() && getFreeColClient().getGUI().getActiveUnit() != null;
    }

    /**
     * Returns the id of this <code>Option</code>.
     * 
     * @return "skipUnitAction"
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
        //getFreeColClient().getInGameController().skipActiveUnit();
        getFreeColClient().getInGameController().changeState(getFreeColClient().getGUI().getActiveUnit(), 
                                                             UnitState.SKIPPED);
    }
}
