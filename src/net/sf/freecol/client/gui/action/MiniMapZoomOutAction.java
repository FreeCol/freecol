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

import java.awt.event.ActionEvent;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;


/**
 * An action for zooming out on the minimap.
 */
public class MiniMapZoomOutAction extends MapboardAction {

    public static final String id = "miniMapZoomOutAction";


    /**
     * Creates a new <code>MiniMapZoomOutAction</code>.
     * @param freeColClient The main controller object for the client.
     * @param b 
     */
    MiniMapZoomOutAction(FreeColClient freeColClient, GUI gui, boolean b) {
        super(freeColClient, gui, id);
        addImageIcons("zoom_out");
    }

    /**
     * Creates a new <code>MiniMapZoomOutAction</code>.
     * @param freeColClient The main controller object for the client.
     * @param gui a <code>boolean</code> value
     */
    MiniMapZoomOutAction(FreeColClient freeColClient, GUI gui) {
        super(freeColClient, gui, id + ".secondary");
        addImageIcons("zoom_out");
    }

    /**
     * Checks if this action should be enabled.
     *
     * @return <code>true</code> if the minimap can be zoomed out.
     */
    @Override
    protected boolean shouldBeEnabled() {
        return super.shouldBeEnabled()
            && gui.canZoomOutMapControls();
    }

    /**
     * Applies this action.
     * @param e The <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent e) {
        gui.zoomOutMapControls();
        update();
        getFreeColClient().getActionManager().getFreeColAction(MiniMapZoomInAction.id).update();
        getFreeColClient().getActionManager().getFreeColAction(MiniMapZoomInAction.id + ".secondary").update();
    }
}
