/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

import net.sf.freecol.client.FreeColClient;


/**
 * Super class for all actions that should be disabled when the
 * mapboard is not selected.
 */
public abstract class MapboardAction extends FreeColAction {


    /**
     * Creates a new {@code MapboardAction}.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param id The action identifier.
     */
    protected MapboardAction(FreeColClient freeColClient, String id) {
        super(freeColClient, id);
    }


    // Override FreeColAction

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean shouldBeEnabled() { 
        return super.shouldBeEnabled()  
            && !getGUI().isShowingSubPanel()
            && (getGame() == null
                || freeColClient.isMapEditor()
                || freeColClient.currentPlayerIsMyPlayer());
    }
}
