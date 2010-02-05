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

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;

import javax.swing.KeyStroke;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;

/**
 * An action for prompting the user to save before quitting the game.
 */
public class SaveAndQuitAction extends MapboardAction {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(SaveAndQuitAction.class.getName());

    public static final String id = "SaveAndQuitAction";


    /**
     * Creates a new <code>SaveAndQuitAction</code>.
     * 
     * @param freeColClient The main controller object for the client.
     */
    SaveAndQuitAction(FreeColClient freeColClient) {
        super(freeColClient, "menuBar.game.quit", null,
              KeyStroke.getKeyStroke('Q', Toolkit.getDefaultToolkit() .getMenuShortcutKeyMask()));
        putValue(BUTTON_IMAGE, freeColClient.getImageLibrary()
                 .getUnitButtonImageIcon(ImageLibrary.UNIT_BUTTON_WAIT, 0));
        putValue(BUTTON_ROLLOVER_IMAGE, freeColClient.getImageLibrary()
                 .getUnitButtonImageIcon(ImageLibrary.UNIT_BUTTON_WAIT, 1));
        putValue(BUTTON_PRESSED_IMAGE, freeColClient.getImageLibrary()
                 .getUnitButtonImageIcon(ImageLibrary.UNIT_BUTTON_WAIT, 2));
        putValue(BUTTON_DISABLED_IMAGE, freeColClient.getImageLibrary()
                 .getUnitButtonImageIcon(ImageLibrary.UNIT_BUTTON_WAIT, 3));
    }

    /**
     * Returns the id of this <code>Option</code>.
     * 
     * @return "SaveAndQuitAction"
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

