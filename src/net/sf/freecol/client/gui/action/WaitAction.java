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

/**
 * An action for chosing the next unit as the active unit.
 */
public class WaitAction extends MapboardAction {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(WaitAction.class.getName());




    public static final String ID = "waitAction";


    /**
     * Creates this action.
     * 
     * @param freeColClient The main controller object for the client.
     */
    WaitAction(FreeColClient freeColClient) {
        super(freeColClient, "unit.state.0", null, KeyStroke.getKeyStroke('W', 0));
        putValue(BUTTON_IMAGE, freeColClient.getImageLibrary().getUnitButtonImageIcon(ImageLibrary.UNIT_BUTTON_WAIT, 0));
        putValue(BUTTON_ROLLOVER_IMAGE, freeColClient.getImageLibrary().getUnitButtonImageIcon(
                ImageLibrary.UNIT_BUTTON_WAIT, 1));
        putValue(BUTTON_PRESSED_IMAGE, freeColClient.getImageLibrary().getUnitButtonImageIcon(
                ImageLibrary.UNIT_BUTTON_WAIT, 2));
        putValue(BUTTON_DISABLED_IMAGE, freeColClient.getImageLibrary().getUnitButtonImageIcon(
                ImageLibrary.UNIT_BUTTON_WAIT, 3));
    }

    /**
     * Returns the id of this <code>Option</code>.
     * 
     * @return "waitAction"
     */
    public String getId() {
        return ID;
    }

    /**
     * Applies this action.
     * 
     * @param e The <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent e) {
        getFreeColClient().getInGameController().nextActiveUnit();
    }
}
