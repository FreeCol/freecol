/**
 *  Copyright (C) 2002-2010  The FreeCol Team
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
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;

import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ViewMode;

/**
 * An action for chosing the next unit as the active unit.
 */
public class MoveNorthWestAction extends MapboardAction {

    public static final String id = "moveNorthWestAction";

    /**
     * Creates a new <code>MoveNorthWestAction</code>.
     * 
     * @param freeColClient The main controller object for the client.
     */
    MoveNorthWestAction(FreeColClient freeColClient) {
        super(freeColClient, "moveNorthWest", null, KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD7, 0));
    }

    /**
     * Returns the id of this <code>Option</code>.
     * 
     * @return "waitAction"
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
        switch(getFreeColClient().getGUI().getViewMode().getView()) {
        case ViewMode.MOVE_UNITS_MODE:
            getFreeColClient().getInGameController().moveActiveUnit(Direction.NW);
            break;
        case ViewMode.VIEW_TERRAIN_MODE:
            getFreeColClient().getGUI().moveTileCursor(Direction.NW);
            break;
        }
    }
}
