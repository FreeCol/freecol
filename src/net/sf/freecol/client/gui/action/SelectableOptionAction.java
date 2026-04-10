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

import java.awt.event.ActionEvent;
import java.util.logging.Level;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;


/**
 * An action for selecting one of several options.
 */
public abstract class SelectableOptionAction extends SelectableAction {

    public static final String id = "selectableOptionAction";

    private final String optionId;


    /**
     * Creates this action.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param id The object identifier.
     * @param optionId The identifier of a boolean client option.
     */
    protected SelectableOptionAction(FreeColClient freeColClient,
                               String id, String optionId) {
        super(freeColClient, id);

        this.optionId = optionId;
    }


    /**
     * Get the value of the underlying option.
     *
     * @return The option value.
     */
    public final boolean getOption() {
        ClientOptions co = freeColClient.getClientOptions();
        if (co != null && optionId != null) {
            try {
                return co.getBoolean(optionId);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failure with option: " + optionId, e);
            }
        }
        return false;
    }

    /**
     * Set the option value.
     *
     * @param value The new boolean value.
     */
    public final void setOption(boolean value) {
        ClientOptions co = freeColClient.getClientOptions();
        if (co != null && optionId != null) co.setBoolean(optionId, value);
    }

    /**
     * Should this action be selected?
     *
     * Override this in subclasses.
     *
     * @return True of this action should be selected.
     */
    protected boolean shouldBeSelected() {
        return getOption();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        setOption(isSelected());
        getGUI().refresh();
    }
}
