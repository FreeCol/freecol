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

/**
 * An action for chosing the next unit as the active unit.
 */
public class MoveWestAction extends MapboardAction {

    public static final String id = "moveWestAction";

    /**
     * Creates a new <code>MoveWestAction</code>.
     * 
     * @param freeColClient The main controller object for the client.
     */
    MoveWestAction(FreeColClient freeColClient) {
        super(freeColClient, "moveWest", null, KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD4, 0));
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
        getFreeColClient().getInGameController().moveActiveUnit(Direction.W);
    }
}
