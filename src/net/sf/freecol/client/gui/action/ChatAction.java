/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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
 * An action for initiating chatting.
 *
 * @see net.sf.freecol.client.gui.panel.MapControls
 */
public class ChatAction extends FreeColAction {

    public static final String id = "chatAction";


    /**
     * Creates a new <code>ChatAction</code>.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public ChatAction(FreeColClient freeColClient) {
        super(freeColClient, id);
    }


    /**
     * Checks if this action should be enabled.
     *
     * @return True if the mapboard is selected.
     */
    @Override
    protected boolean shouldBeEnabled() {
        return super.shouldBeEnabled()
            && !freeColClient.isSinglePlayer()
            && (!getGUI().isShowingSubPanel() || getGame() != null
                && !freeColClient.currentPlayerIsMyPlayer());
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {
        getGUI().showChatPanel();
    }
}
