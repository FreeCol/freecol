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

import java.awt.event.ActionEvent;


import net.sf.freecol.client.FreeColClient;


/**
 * An action for zooming out on the minimap.
 */
public class MiniMapZoomOutAction extends MapboardAction {

    public static final String id = "miniMapZoomOutAction";


    /**
     * Creates a new <code>MiniMapZoomOutAction</code>.
     * @param freeColClient The main controller object for the client.
     */
    MiniMapZoomOutAction(FreeColClient freeColClient) {
        super(freeColClient, id);
        addImageIcons("zoom_out");
    }

    /**
     * Creates a new <code>MiniMapZoomOutAction</code>.
     * @param freeColClient The main controller object for the client.
     * @param secondary a <code>boolean</code> value
     */
    MiniMapZoomOutAction(FreeColClient freeColClient, boolean secondary) {
        super(freeColClient, id + ".secondary");
        addImageIcons("zoom_out");
    }

    /**
     * Checks if this action should be enabled.
     *
     * @return <code>true</code> if the minimap can be zoomed out.
     */
    protected boolean shouldBeEnabled() {
        MapControlsAction mca = (MapControlsAction) getFreeColClient().getActionManager()
            .getFreeColAction(MapControlsAction.id);
        return super.shouldBeEnabled()
            && mca.getMapControls() != null
            && mca.getMapControls().canZoomOut();
    }

    /**
     * Applies this action.
     * @param e The <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent e) {
        MapControlsAction mca = (MapControlsAction) getFreeColClient().getActionManager()
            .getFreeColAction(MapControlsAction.id);
        mca.getMapControls().zoomOut();
        update();
        getFreeColClient().getActionManager().getFreeColAction(MiniMapZoomInAction.id).update();
        getFreeColClient().getActionManager().getFreeColAction(MiniMapZoomInAction.id + ".secondary").update();
    }
}
