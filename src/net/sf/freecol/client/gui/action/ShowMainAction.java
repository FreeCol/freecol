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

/**
 * Returns to the <code>MainPanel</code>.
 * All in-game components are removed.
 *
 * @see net.sf.freecol.client.gui.panel.MainPanel
 */
public class ShowMainAction extends FreeColAction {

    public static final String id = "showMainAction";


    /**
     * Creates this action.
     *
     * @param freeColClient The main controller object for the client.
     * @param gui 
     */
    ShowMainAction(FreeColClient freeColClient, GUI gui) {
        super(freeColClient, gui, id);
    }

    /**
     * Applies this action.
     * @param e The <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent e) {
        if (!gui.showConfirmDialog("stopCurrentGame.text",
                                                              "stopCurrentGame.yes",
                                                              "stopCurrentGame.no")) {
            return;
        }
        getFreeColClient().getConnectController().quitGame(true);
        gui.removeInGameComponents();
        getFreeColClient().setMapEditor(false);
        getFreeColClient().setGame(null);
        gui.getCanvas().returnToTitle();
    }
}
