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


import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;

/**
 * An action for prompting the user to save before quitting the game.
 */
public class SaveAndQuitAction extends FreeColAction {

    public static final String id = "saveAndQuitAction";


    /**
     * Creates a new <code>SaveAndQuitAction</code>.
     * 
     * @param freeColClient The main controller object for the client.
     */
    SaveAndQuitAction(FreeColClient freeColClient) {
        super(freeColClient, id);
    }

    /**
     * Applies this action.
     * 
     * @param e The <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent e) {
        Canvas canvas = getFreeColClient().getCanvas();
        if (canvas.showConfirmDialog("quitDialog.areYouSure.text", "ok", "cancel")) {
            if (canvas.showConfirmDialog("quitDialog.save.text", "yes", "no")) {
                if (!freeColClient.isMapEditor()) {
                    freeColClient.getInGameController().saveGame();
                } else {
                    freeColClient.getMapEditorController().saveGame();
                }
            }
            freeColClient.quit();
        }
    }
}

