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

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.panel.FreeColDialog;
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
     */
    NewEmptyMapAction(FreeColClient freeColClient) {
        super(freeColClient, id);
    }

    /**
     * Checks if this action should be enabled.
     * 
     * @return <code>true</code> if currently
     *      in map editor mode.
     * @see FreeColClient#isMapEditor()
     */
    protected boolean shouldBeEnabled() {
        return freeColClient.isMapEditor();
    }

    /**
     * Applies this action.
     * 
     * @param e The <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent e) {
        final Canvas canvas = getFreeColClient().getCanvas();
        final Game game = freeColClient.getGame();

        Dimension size = canvas.showFreeColDialog(FreeColDialog.createMapSizeDialog());
        if (size == null) {
            return;
        }
        //TODO: Use an interface
        final MapGenerator mapGenerator = (MapGenerator) freeColClient.getFreeColServer().getMapGenerator();
        mapGenerator.getTerrainGenerator().createMap(game, new boolean[size.width][size.height]);        
        
        freeColClient.getGUI().setFocus(1, 1);
        freeColClient.getActionManager().update();
        canvas.refresh();
    }
    
}
