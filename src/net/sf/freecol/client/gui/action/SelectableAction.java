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

import java.util.logging.Level;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.common.model.Player;


/**
 * An action for selecting one of several options.
 */
public abstract class SelectableAction extends MapboardAction {

    public static final String id = "selectableAction";

    private final String optionId;

    protected boolean selected = false;


    /**
     * Creates this action.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param id The object identifier.
     * @param optionId The identifier of a boolean client option.
     */
    protected SelectableAction(FreeColClient freeColClient,
                               String id, String optionId) {
        super(freeColClient, id);

        this.optionId = optionId;
        setSelected(shouldBeSelected());
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
     * Gets whether the action is selected.
     *
     * @return True if this action is selected.
     */
    public final boolean isSelected() {
        return selected;
    }

    /**
     * Sets whether the action is selected.
     *
     * @param b The new selection value.
     */
    public final void setSelected(boolean b) {
        this.selected = b;
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


    // Override FreeColAction

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean shouldBeEnabled() {
        final Player player = getFreeColClient().getMyPlayer();
        return super.shouldBeEnabled() && getFreeColClient().getGame() != null
            && player != null && player.getNewModelMessages().isEmpty();
    }

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
