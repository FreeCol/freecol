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

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.KeyStroke;

import net.sf.freecol.client.FreeColClient;


/**
 * An action for zooming in on the minimap.
 */
public class MiniMapZoomInAction extends MapboardAction {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(MiniMapZoomInAction.class.getName());

    public static final String id = "miniMapZoomInAction";


    /**
     * Creates a new <code>MiniMapZoomInAction</code>.
     * @param freeColClient The main controller object for the client.
     */
    MiniMapZoomInAction(FreeColClient freeColClient) {
        super(freeColClient, "unit.state.9", null, KeyEvent.VK_PLUS, KeyStroke.getKeyStroke('+', 0));
        addImageIcons("zoom_in");
    }
    
    
    /**
    * Returns the id of this <code>Option</code>.
    * @return "miniMapZoomInAction"
    */
    public String getId() {
        return id;
    }
    

    /**
     * Checks if this action should be enabled.
     * 
     * @return <code>true</code> if the minimap can be zoomed in.
     */
    protected boolean shouldBeEnabled() {
        MapControlsAction mca = (MapControlsAction) getFreeColClient().getActionManager().getFreeColAction(MapControlsAction.id);
        return super.shouldBeEnabled()
                && mca.getMapControls() != null
                && mca.getMapControls().canZoomIn();
    }  
    
    /**
     * Applies this action.
     * @param e The <code>ActionEvent</code>.
     */    
    public void actionPerformed(ActionEvent e) {
        MapControlsAction mca = (MapControlsAction) getFreeColClient().getActionManager().getFreeColAction(MapControlsAction.id);
        mca.getMapControls().zoomIn();
        update();
        getFreeColClient().getActionManager().getFreeColAction(MiniMapZoomOutAction.id).update();
    }
}
