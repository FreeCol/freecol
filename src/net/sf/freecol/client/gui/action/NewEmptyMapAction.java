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

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Tile;


/**
 * Creates a new empty map.
 */
public class NewEmptyMapAction extends MapboardAction {

    public static final String id = "newEmptyMapAction";


    /**
     * Creates this action
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public NewEmptyMapAction(FreeColClient freeColClient) {
        super(freeColClient, id);
    }


    // Override FreeColAction

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean shouldBeEnabled() {
        return freeColClient.isMapEditor();
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        Dimension size = getGUI().showMapSizeDialog();
        if (size == null) return;
        final Game game = getGame();
        Map map = getFreeColClient().getFreeColServer()
            .createEmptyMap(game, size.width, size.height);
        game.setMap(map);
        Tile tile = map.getTile(size.width/2, size.height/2);
        getGUI().setFocus(tile);
        getGUI().updateMenuBar();
        getGUI().refresh();
    }
}
