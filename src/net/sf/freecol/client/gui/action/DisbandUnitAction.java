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
 * An action for disbanding the active unit.
 */
public class DisbandUnitAction extends MapboardAction {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(DisbandUnitAction.class.getName());




    public static final String ID = "disbandUnitAction";


    /**
     * Creates a new <code>DisbandUnitAction</code>.
     * 
     * @param freeColClient The main controller object for the client.
     */
    DisbandUnitAction(FreeColClient freeColClient) {
        super(freeColClient, "unit.state.8", null, KeyStroke.getKeyStroke('D', 0));
        putValue(BUTTON_IMAGE, freeColClient.getImageLibrary().getUnitButtonImageIcon(ImageLibrary.UNIT_BUTTON_DISBAND,
                0));
        putValue(BUTTON_ROLLOVER_IMAGE, freeColClient.getImageLibrary().getUnitButtonImageIcon(
                ImageLibrary.UNIT_BUTTON_DISBAND, 1));
        putValue(BUTTON_PRESSED_IMAGE, freeColClient.getImageLibrary().getUnitButtonImageIcon(
                ImageLibrary.UNIT_BUTTON_DISBAND, 2));
        putValue(BUTTON_DISABLED_IMAGE, freeColClient.getImageLibrary().getUnitButtonImageIcon(
                ImageLibrary.UNIT_BUTTON_DISBAND, 3));
    }

    /**
     * Checks if this action should be enabled.
     * 
     * @return <code>true</code> if there is an active unit.
     */
    protected boolean shouldBeEnabled() {
        return super.shouldBeEnabled() && getFreeColClient().getGUI().getActiveUnit() != null;
    }

    /**
     * Returns the id of this <code>Option</code>.
     * 
     * @return "disbandUnitAction"
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
        getFreeColClient().getInGameController().disbandActiveUnit();
    }
}
