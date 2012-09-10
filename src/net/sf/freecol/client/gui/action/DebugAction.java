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
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.common.debug.FreeColDebugger;

/**
 * Switch debug mode on.
 */
public class DebugAction extends FreeColAction {

    public static final String id = "debugAction";
    private final InGameController inGameController;

    /**
     * Creates a new <code>DebugAction</code>.
     *
     * @param freeColClient The main controller object for the client.
     * @param gui 
     */
    DebugAction(FreeColClient freeColClient, InGameController inGameController, GUI gui) {
        super(freeColClient, gui, id);
        this.inGameController = inGameController;
        setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.SHIFT_MASK | KeyEvent.CTRL_MASK));
    }

    /**
     * Checks if this action should be enabled.
     *
     * @return True if not already in debug mode.
     */
    @Override
    public boolean shouldBeEnabled() {
        return !FreeColDebugger.isInDebugMode();
    }

    /**
     * Applies this action.
     *
     * @param e The <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent e) {
        if (shouldBeEnabled()) {
            inGameController.setInDebugMode(true);
            freeColClient.getConnectController().reconnect();
        }
    }
}
