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
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;

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
        GUI gui = getFreeColClient().getGUI();
        Unit unit = gui.getActiveUnit();

        // Action should be disabled if there is no active unit, but make sure
        if (unit != null) {
            // Enter "goto mode" if not already activated; otherwise cancel it
            if (gui.isGotoStarted()) {
                gui.stopGoto();
            } else {
                gui.startGoto();

                // Draw the path to the current mouse position, if the
                // mouse is over the screen; see also
                // CanvaseMouseMotionListener
                Point pt = getFreeColClient().getCanvas().getMousePosition();
                if (pt != null) {
                    Tile tile = gui.convertToMapTile(pt.x, pt.y);
                    if (tile != null && unit.getTile() != tile) {
                        PathNode dragPath = unit.findPath(tile);
                        gui.setGotoPath(dragPath);
                    }
                }
            }
        }
    }

}
