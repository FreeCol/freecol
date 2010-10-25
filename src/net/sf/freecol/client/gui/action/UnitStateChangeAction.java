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

import java.awt.event.ActionEvent;


import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.model.Unit.UnitState;

/**
 * An action to change the state of a unit.
 */
public class UnitStateChangeAction extends UnitAction {

    public static final String id = "stateChangeAction";

    private UnitState state;

    /**
     * Creates this action.
     * @param freeColClient The main controller object for the client.
     */
    public UnitStateChangeAction(FreeColClient freeColClient, UnitState unitState) {
        super(freeColClient, unitState.getId() + "Action");
        this.state = unitState;
        addImageIcons(unitState.getId());
    }

    /**
     * Applies this action.
     * @param actionEvent The <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent actionEvent) {
        getFreeColClient().getInGameController().changeState(getUnit(), state);
    }

}
