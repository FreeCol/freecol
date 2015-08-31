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
 * An action for scaling a map. This action is a part of the map editor.
 */
public class ScaleMapAction extends FreeColAction {

    public static final String id = "scaleMapAction";


    /**
     * Creates a new <code>ScaleMapAction</code>.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public ScaleMapAction(FreeColClient freeColClient) {
        super(freeColClient, id);
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
         * FIXME: Find a better method for choosing a group of
         * adjacent tiles.  This group can then be merged into a
         * common tile by using the average value (for example: are
         * there a majority of ocean tiles?).
         */

        final Game game = getGame();
        final Map oldMap = game.getMap();

        final int oldWidth = oldMap.getWidth();
        final int oldHeight = oldMap.getHeight();

        Map map = new Map(game, width, height);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                final int oldX = (x * oldWidth) / width;
                final int oldY = (y * oldHeight) / height;
                // FIXME: This tile should be based on the average as
                // mentioned at the top of this method.
                Tile importTile = oldMap.getTile(oldX, oldY);
                Tile t = new Tile(game, importTile.getType(), x, y);
                if (importTile.getMoveToEurope() != null) {
                    t.setMoveToEurope(importTile.getMoveToEurope());
                }
                if (t.getTileItemContainer() != null) {
                    t.getTileItemContainer().copyFrom(importTile.getTileItemContainer());
                }
                map.setTile(t, x, y);
            }
        }
        game.setMap(map);

        /* Commented because it doesn't appear to do anything valuable
        // Update river directions
        for (Tile t : map.getAllTiles()) {
            t.getTileItemContainer().updateRiver();
        }*/

        getGUI().setSelectedTile(map.getTile(0, 0));
        getGUI().refresh();
    }


    // Override FreeColAction

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean shouldBeEnabled() {
        return super.shouldBeEnabled()
            && freeColClient.isMapEditor()
            && getGame() != null
            && getGame().getMap() != null;
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        Dimension ms = getGUI().showScaleMapSizeDialog();
        if (ms != null) {
            scaleMapTo(ms.width, ms.height);
        }
    }
}
