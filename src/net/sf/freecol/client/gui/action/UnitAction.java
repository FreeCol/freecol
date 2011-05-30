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

import net.sf.freecol.client.FreeColClient;


/**
* Super class for all actions that should be disabled when no unit is selected.
*/
public abstract class UnitAction extends MapboardAction {

    /**
     * Creates a new <code>UnitAction</code>.
     * @param freeColClient The main controller object for the client
     * @param id The id of this action
     */
    protected UnitAction(FreeColClient freeColClient, String id) {
        super(freeColClient, id);
    }

    /**
     * Checks if this action should be enabled.
     *
     * @return <code>false</code> if there is no active unit.
     */
    @Override
    protected boolean shouldBeEnabled() {
        return super.shouldBeEnabled() && getFreeColClient().getGUI().getActiveUnit() != null;
    }

}
