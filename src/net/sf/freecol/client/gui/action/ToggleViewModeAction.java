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
import java.awt.event.InputEvent;
import java.util.logging.Logger;

import javax.swing.KeyStroke;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ViewMode;

/**
 * An action for changing view mode between move units mode and view terrain
 * mode
 * 
 * @see ViewMode
 */
public class ToggleViewModeAction extends MapboardAction {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(ToggleViewModeAction.class.getName());




    public static final String id = "toggleViewModeAction";


    /**
     * Creates this action.
     * 
     * @param freeColClient The main controller object for the client.
     */
    ToggleViewModeAction(FreeColClient freeColClient) {
        super(freeColClient, "menuBar.view.toggle", null, KeyStroke.getKeyStroke('V', Toolkit.getDefaultToolkit()
                .getMenuShortcutKeyMask()
                | InputEvent.SHIFT_MASK));
    }

    /**
     * Returns the id of this <code>Option</code>.
     * 
     * @return "toggleViewModeAction"
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
        getFreeColClient().getGUI().getViewMode().toggleViewMode();
    }
}
