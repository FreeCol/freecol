/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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
     * Creates a new {@code ScaleMapAction}.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
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
        final Game game = getGame();
        game.changeMap(game.getMap().scale(width, height));
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
