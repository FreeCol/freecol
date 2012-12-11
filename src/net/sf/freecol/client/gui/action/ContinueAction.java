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
import java.io.File;


import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.ConnectController;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;

/**
 *  Action to load and start the most recent save game of the client.
 */
public class ContinueAction extends FreeColAction {

    public static final String id = "continueAction";
    private final InGameController inGameController;
    private ConnectController connectController;


    /**
     * Creates a new <code>ContinueAction</code>.
     *
     * @param freeColClient The main controller object for the client.
     * @param gui 
     */
    ContinueAction(FreeColClient freeColClient, InGameController inGameController, ConnectController connectController, GUI gui) {
        super(freeColClient, gui, id);
        this.inGameController = inGameController;
        this.connectController = connectController;
        
        // interim solution to be replaced! redirect to identical NAME text
        putValue(NAME, Messages.message("victory.continue"));
        putValue(SHORT_DESCRIPTION, null);
    }

    /**
     * Applies this action.
     * Starts the most recent save game if it is defined in the client.
     *
     * @param e The <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent e) {
       File lastSave = inGameController.getLastSaveGameFile();
       if (lastSave != null) {
           connectController.loadGame(lastSave);
       }
    }
}
