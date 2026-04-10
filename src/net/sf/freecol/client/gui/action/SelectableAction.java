/**
 *  Copyright (C) 2002-2024   The FreeCol Team
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

import javax.swing.Action;

import net.sf.freecol.client.FreeColClient;


/**
 * An action for a boolean value.
 */
public abstract class SelectableAction extends MapboardAction {

    public static final String id = "selectableAction";


    /**
     * Creates this action.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param id The object identifier.
     */
    protected SelectableAction(FreeColClient freeColClient, String id) {
        super(freeColClient, id);
    }

    /**
     * Gets whether the action is selected.
     *
     * @return True if this action is selected.
     */
    public final boolean isSelected() {
        return Boolean.TRUE.equals(getValue(Action.SELECTED_KEY));
    }

    /**
     * Sets whether the action is selected.
     *
     * @param b The new selection value.
     */
    public final void setSelected(boolean b) {
        putValue(Action.SELECTED_KEY, b);
    }

    /**
     * Should this action be selected?
     *
     * Override this in subclasses.
     *
     * @return True of this action should be selected.
     */
    protected boolean shouldBeSelected() {
        return isSelected();
    }


    // Override FreeColAction

    /**
     * {@inheritDoc}
     */
    @Override
    public void update() {
        super.update();

        // Augment functionality to also update selection state.
        setSelected(shouldBeSelected());
    }
}
