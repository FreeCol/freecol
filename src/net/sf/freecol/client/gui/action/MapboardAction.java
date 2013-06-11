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

import java.util.logging.Logger;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;


/**
 * Super class for all actions that should be disabled when the
 * mapboard is not selected.
 */
public abstract class MapboardAction extends FreeColAction {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(MapboardAction.class.getName());


    /**
     * Creates a new <code>MapboardAction</code>.
     * @param freeColClient The main controller object for the client
     * @param id The object identifier for this action.
     */
    protected MapboardAction(FreeColClient freeColClient, GUI gui, String id) {
        super(freeColClient, gui, id);
    }

    /**
     * Checks if this action should be enabled.
     * 
     * @return <code>true</code> if the mapboard is selected.
     */
    @Override
    protected boolean shouldBeEnabled() { 
        return super.shouldBeEnabled()  
            && gui.isMapboardActionsEnabled()
            && (getFreeColClient().getGame() == null
                || getFreeColClient().currentPlayerIsMyPlayer());
    }
}
