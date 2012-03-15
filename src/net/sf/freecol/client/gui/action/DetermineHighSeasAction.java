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

import java.awt.event.ActionEvent;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.panel.Parameters;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.server.generator.TerrainGenerator;

/**
 * An action for determining the high seas tiles.
 */
public class DetermineHighSeasAction extends FreeColAction {

    public static final String id = "determineHighSeasAction";


    /**
     * Creates a new <code>DetermineHighSeasAction</code>.
     *
     * @param freeColClient The main controller object for the client.
     * @param gui 
     */
    DetermineHighSeasAction(FreeColClient freeColClient, GUI gui) {
        super(freeColClient, gui, id);
    }

    /**
     * Checks if this action should be enabled.
     *
     * @return <code>false</code> if there is no active map.
     */
    @Override
    protected boolean shouldBeEnabled() {
        return super.shouldBeEnabled()
            && freeColClient.isMapEditor()
            && freeColClient.getGame() != null
            && freeColClient.getGame().getMap() != null;
    }

    /**
     * Applies this action.
     *
     * @param e The <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent e) {
        final Game game = freeColClient.getGame();
        final Map map = game.getMap();
        
        Parameters p = gui.showParametersDialog();

        if (p != null) {
            TerrainGenerator.determineHighSeas(map, p.distToLandFromHighSeas, p.maxDistanceToEdge);
        }
    }

 

}
