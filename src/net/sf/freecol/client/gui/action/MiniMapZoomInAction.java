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

import java.awt.event.ActionEvent;

import net.sf.freecol.client.FreeColClient;


/**
 * An action for zooming in on the minimap.
 */
public class MiniMapZoomInAction extends MapboardAction {

    public static final String id = "miniMapZoomInAction";


    /**
     * Creates a new <code>MiniMapZoomInAction</code>.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public MiniMapZoomInAction(FreeColClient freeColClient) {
        super(freeColClient, id);

        addImageIcons("zoom_in");
    }

    /**
     * Creates a new <code>MiniMapZoomInAction</code>.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param b a <code>boolean</code> value
     */
    public MiniMapZoomInAction(FreeColClient freeColClient, boolean b) {
        super(freeColClient, id + ".secondary");
 
        addImageIcons("zoom_in");
    }


    // Override FreeColAction

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean shouldBeEnabled() {
        return super.shouldBeEnabled() && getGUI().canZoomInMapControls();
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        getGUI().zoomInMapControls();
        update();
        getActionManager().getFreeColAction(MiniMapZoomOutAction.id).update();
        getActionManager().getFreeColAction(MiniMapZoomOutAction.id + ".secondary").update();
    }
}
