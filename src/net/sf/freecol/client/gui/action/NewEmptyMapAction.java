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

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import net.sf.freecol.client.FreeColClient;
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
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public NewEmptyMapAction(FreeColClient freeColClient) {
        super(freeColClient, id);
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
        Dimension size = getGUI().showMapSizeDialog();
        if (size == null) return;
        MapGenerator mapGenerator = getFreeColClient().getFreeColServer()
            .getMapGenerator();
        mapGenerator.createEmptyMap(getGame(),
            new boolean[size.width][size.height]);
        getGUI().setFocus(getGame().getMap().getTile(1,1));
        getFreeColClient().updateActions();
        getGUI().refresh();
    }
}
