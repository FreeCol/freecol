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

package net.sf.freecol.client.gui.action;

import java.awt.event.ActionEvent;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.common.model.Direction;


/**
 * An action for chosing the next unit as the active unit.
 */
public class MoveAction extends MapboardAction {

    public static final String id = "moveAction.";

    private final Direction direction;


    /**
     * Creates a new <code>MoveAction</code>.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param direction The <code>Direction</code> to move.
     */
    public MoveAction(FreeColClient freeColClient, Direction direction) {
        super(freeColClient, id + direction);

        this.direction = direction;
    }

    /**
     * Creates a new <code>MoveAction</code>.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param direction The <code>Direction</code> to move in.
     * @param secondary a <code>boolean</code> value
     */
    public MoveAction(FreeColClient freeColClient, Direction direction,
                      boolean secondary) {
        super(freeColClient, id + direction + ".secondary");

        this.direction = direction;
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) { 
        switch (getGUI().getViewMode()) {
        case GUI.MOVE_UNITS_MODE:
            igc().moveUnit(getGUI().getActiveUnit(), direction);
            break;
        case GUI.VIEW_TERRAIN_MODE:
            igc().moveTileCursor(direction);
            break;
        default:
            break;
        }
    }
}
