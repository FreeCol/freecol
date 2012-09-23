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
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Tile;

/**
 * An action for scaling a map. This action is a part of the map editor.
 */
public class ScaleMapAction extends FreeColAction {

    public static final String id = "scaleMapAction";


    /**
     * Creates a new <code>ScaleMapAction</code>.
     *
     * @param freeColClient The main controller object for the client.
     * @param gui 
     */
    ScaleMapAction(FreeColClient freeColClient, GUI gui) {
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
        Dimension ms = gui.showScaleMapSizeDialog();
        if (ms != null) {
            scaleMapTo(ms.width, ms.height);
        }
    }


    /**
     * Scales the current map into the specified size. The current
     * map is given by freeColClient.getGame().getMap().
     *
     * @param width The width of the resulting map.
     * @param height The height of the resulting map.
     */
    private void scaleMapTo(final int width, final int height) {
        /*
         * This implementation uses a simple linear scaling, and
         * the isometric shape is not taken into account.
         *
         * TODO: Find a better method for choosing a group of
         *       adjacent tiles. This group can then be merged into
         *       a common tile by using the average value (for
         *       example: are there a majority of ocean tiles?).
         */

        final Game game = freeColClient.getGame();
        final Map oldMap = game.getMap();

        final int oldWidth = oldMap.getWidth();
        final int oldHeight = oldMap.getHeight();

        Tile[][] tiles = new Tile[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                final int oldX = (x * oldWidth) / width;
                final int oldY = (y * oldHeight) / height;
                /*
                 * TODO: This tile should be based on the average as
                 *       mentioned at the top of this method.
                 */
                Tile importTile = oldMap.getTile(oldX, oldY);
                Tile t = new Tile(game, importTile.getType(), x, y);
                if (importTile.getMoveToEurope() != null) {
                    t.setMoveToEurope(importTile.getMoveToEurope());
                }
                if (t.getTileItemContainer() != null) {
                    t.getTileItemContainer().copyFrom(importTile.getTileItemContainer());
                }
                tiles[x][y] = t;
            }
        }

        Map map = new Map(game, tiles);
        game.setMap(map);

        /* Commented because it doesn't appear to do anything valuable
        // Update river directions
        for (Tile t : map.getAllTiles()) {
            t.getTileItemContainer().updateRiver();
        }*/

        gui.setSelectedTile(map.getTile(0, 0), false);
        gui.refresh();
    }
}
