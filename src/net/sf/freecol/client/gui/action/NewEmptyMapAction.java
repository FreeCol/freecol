/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.server.generator.MapGenerator;


/**
 * Creates a new empty map.
 */
public class NewEmptyMapAction extends MapboardAction {

    public static final String id = "newEmptyMapAction";


    /**
     * Creates this action
     *
     * @param freeColClient The main controller object for the client.
     * @param gui 
     */
    NewEmptyMapAction(FreeColClient freeColClient, GUI gui) {
        super(freeColClient, gui, id);
    }

    /**
     * Checks if this action should be enabled.
     *
     * @return <code>true</code> if currently
     *      in map editor mode.
     * @see FreeColClient#isMapEditor()
     */
    @Override
    protected boolean shouldBeEnabled() {
        return freeColClient.isMapEditor();
    }

    /**
     * Applies this action.
     *
     * @param e The <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent e) {
        final Game game = freeColClient.getGame();

        Dimension size = gui.showMapSizeDialog();
        if (size == null) {
            return;
        }
        MapGenerator mapGenerator = freeColClient.getFreeColServer().getMapGenerator();
        mapGenerator.createEmptyMap(game, new boolean[size.width][size.height]);

        gui.setFocus(game.getMap().getTile(1,1));
        freeColClient.updateActions();
        gui.refresh();
    }

}
