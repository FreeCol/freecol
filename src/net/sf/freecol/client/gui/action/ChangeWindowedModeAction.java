/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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
 * An action for toggling between full-screen and windowed mode.
 */
public class ChangeWindowedModeAction extends SelectableAction {

    public static final String id = "changeWindowedModeAction";


    /**
     * Creates a new <code>ChangeWindowedModeAction</code>.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public ChangeWindowedModeAction(FreeColClient freeColClient) {
        super(freeColClient, id, null);
    }


    // Override SelectableAction

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean shouldBeSelected() {
        final GUI gui = getGUI();
        return super.shouldBeSelected()
            && !(gui == null || gui.isWindowed());
    }


    // Override FreeColAction

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean shouldBeEnabled() {
        final GUI gui = getGUI();
        return super.shouldBeEnabled()
            && !(gui == null || gui.isShowingSubPanel());
    }
    

    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        GUI gui = getGUI();
        if (gui == null) return;
        gui.changeWindowedMode();
    }
}
