/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.PlayerType;

/**
 * An action for declaring independence.
 */
public class DeclareIndependenceAction extends MapboardAction {

    public static final String id = "declareIndependenceAction";


    /**
     * Creates a new <code>DeclareIndependenceAction</code>.
     *
     * @param freeColClient The main controller object for the client.
     * @param gui 
     */
    DeclareIndependenceAction(FreeColClient freeColClient, GUI gui) {
        super(freeColClient, gui, id);
    }

    /**
     * Checks if this action should be enabled.
     *
     * @return <code>true</code> if the player can declare independence.
     */
    @Override
    protected boolean shouldBeEnabled() {
        Player p = getFreeColClient().getMyPlayer();
        return super.shouldBeEnabled() && p != null && p.getPlayerType() == PlayerType.COLONIAL;
    }

    /**
     * Applies this action.
     *
     * @param e The <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent e) {
        getFreeColClient().getInGameController().declareIndependence();
        update();
    }
}
