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


/**
 * An action for toggling between full-screen and windowed mode.
 */
public class ChangeWindowedModeAction extends SelectableAction {

    public static final String id = "changeWindowedModeAction";


    /**
     * Creates a new <code>ChangeWindowedModeAction</code>.
     *
     * @param freeColClient The main controller object for the client.
     */
    ChangeWindowedModeAction(FreeColClient freeColClient) {
        super(freeColClient, id, "NO_ID");
    }

    /**
     * Updates the "enabled"-status
     */
    @Override
    public void update() {
        selected = !getFreeColClient().isWindowed();
    }

    /**
     * {@inheritDoc}
     */
    public boolean shouldBeSelected() {
        return !getFreeColClient().isWindowed();
    }

    /**
     * Applies this action.
     *
     * @param e The <code>ActionEvent</code>.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        getFreeColClient().changeWindowedMode(!getFreeColClient().isWindowed());
    }
}
