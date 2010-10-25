/**
 *  Copyright (C) 2002-2010  The FreeCol Team
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
import net.sf.freecol.common.model.Unit;


/**
* Super class for all actions that should be disabled when no unit is selected.
*/
public abstract class UnitAction extends MapboardAction {

    private Unit unit = null;

    /**
     * Creates a new <code>UnitAction</code>.
     * @param freeColClient The main controller object for the client
     * @param id The id of this action
     */
    protected UnitAction(FreeColClient freeColClient, String id) {
        this(freeColClient, id, null);
    }

    /**
     * Creates a new <code>UnitAction</code>.
     * @param freeColClient The main controller object for the client
     * @param id The id of this action
     * @param unit an <code>Unit</code> value
     */
    protected UnitAction(FreeColClient freeColClient, String id, Unit unit) {
        super(freeColClient, id);
        this.unit = unit;
    }

    /**
     * Returns the <code>Unit</code> this action refers to.
     *
     * @return an <code>Unit</code> value
     */
    public Unit getUnit() {
        return (unit == null)
            ? getFreeColClient().getGUI().getActiveUnit()
            : unit;
    }

    /**
     * Checks if this action should be enabled.
     *
     * @return <code>false</code> if there is no active unit.
     */
    protected boolean shouldBeEnabled() {
        return super.shouldBeEnabled() && getUnit() != null;
    }

}
