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

import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ViewMode;

/**
 * An action for chosing the next unit as the active unit.
 */
public class MoveAction extends MapboardAction {

    public static final String id = "moveAction.";

    private Direction direction;

    /**
     * Creates a new <code>MoveAction</code>.
     *
     * @param freeColClient The main controller object for the client.
     * @param direction a <code>Direction</code> value
     */
    MoveAction(FreeColClient freeColClient, Direction direction) {
        super(freeColClient, id + direction);
        this.direction = direction;
    }

    /**
     * Creates a new <code>MoveAction</code>.
     *
     * @param freeColClient The main controller object for the client.
     * @param direction a <code>Direction</code> value
     * @param secondary a <code>boolean</code> value
     */
    MoveAction(FreeColClient freeColClient, Direction direction, boolean secondary) {
        super(freeColClient, id + direction + ".secondary");
        this.direction = direction;
    }

    /**
     * Applies this action.
     *
     * @param e The <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent e) {
        switch(getFreeColClient().getGUI().getViewMode().getView()) {
        case ViewMode.MOVE_UNITS_MODE:
            getFreeColClient().getInGameController().moveActiveUnit(direction);
            break;
        case ViewMode.VIEW_TERRAIN_MODE:
            getFreeColClient().getGUI().moveTileCursor(direction);
            break;
        }
    }
}
