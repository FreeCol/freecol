/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

import java.awt.Point;
import java.awt.event.ActionEvent;


import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Tile;

/**
 * An action to make a unit go to a specific tile.
 */
public class GotoTileAction extends UnitAction {

    public static final String id = "gotoTileAction";
    
    /**
     * Creates this action.
     * 
     * @param freeColClient The main controller object for the client.
     */
    GotoTileAction(FreeColClient freeColClient) {
        super(freeColClient, id);
    }

    /**
     * Checks if this action should be enabled.
     * 
     * @return <code>false</code> if there is no active unit, and 
     *      <code>true</code> otherwise.
     */
    protected boolean shouldBeEnabled() {
        return super.shouldBeEnabled()
            && getFreeColClient().getGUI().getActiveUnit().getTile() != null;
    }

    /**
     * Applies this action.
     * @param e The <code>ActionEvent</code>.
     */    
    public void actionPerformed(ActionEvent e) {
        GUI gui = getFreeColClient().getCanvas().getGUI();

        //Action should be disabled if there is no active unit, but make sure
        if (gui.getActiveUnit() == null) {
            return;
        }
        
        //Enter "goto mode" if not already activated; otherwise cancel it 
        if (!gui.isGotoStarted()) {
            gui.startGoto();

            //Draw the path to the current mouse position, if the
            //mouse is over the screen; see also
            //CanvaseMouseMotionListener
            Point pt = getFreeColClient().getCanvas().getMousePosition();
            if (pt != null) {
                Map map = getFreeColClient().getGame().getMap();
                Map.Position p = gui.convertToMapCoordinates(pt.x, pt.y);

                if (p != null && map.isValid(p)) {
                    Tile tile = map.getTile(p);
                    if (tile != null) {
                        if (gui.getActiveUnit().getTile() != tile) {
                            PathNode dragPath = gui.getActiveUnit().findPath(tile);
                            gui.setGotoPath(dragPath);
                        }
                    }
                }
            }
        } else {
            gui.stopGoto();
        }


    }
    
}
