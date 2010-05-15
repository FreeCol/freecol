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


import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;

/**
 * An action for selecting one of several options.
 */
public abstract class SelectableAction extends MapboardAction {

    public static final String id = "selectableAction";

    protected boolean selected = false;

    /**
     * Creates this action.
     * @param freeColClient The main controller object for the client
     * @param id a <code>String</code> value
     */
    protected SelectableAction(FreeColClient freeColClient, String id) {
        super(freeColClient, id);
    }

    /**
     * Updates the "enabled"-status 
     */
    public void update() {
        super.update();
        
        final Game game = getFreeColClient().getGame();
        final Player player = getFreeColClient().getMyPlayer();
        if (game != null && player != null && !player.getNewModelMessages().isEmpty()) {
            enabled = false;
        }
    }

    /**
     * Checks if the map controls is selcted.
     * @return <code>true</code> if the map controls is selected.
     */
    public boolean isSelected() {
        return selected;
    }
    
    public void setSelected(boolean b) {
        this.selected = b;
    }
}
