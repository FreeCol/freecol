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
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.ImageLibrary;

/**
 * An action for chosing the next unit as the active unit.
 */
public class ZoomInAction extends FreeColAction {
    private static final Logger logger = Logger.getLogger(ZoomInAction.class.getName());




    public static final String id = "zoomInAction";


    /**
     * Creates a new <code>ZoomInAction</code>.
     * 
     * @param freeColClient The main controller object for the client.
     */
    ZoomInAction(FreeColClient freeColClient) {
        super(freeColClient, "menuBar.view.zoomIn", null, KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0));
    }

    /**
     * Checks if this action should be enabled.
     * 
     * @return <code>true</code> if the mapboard is selected
     *      and can be zoomed onto.
     */
    protected boolean shouldBeEnabled() {
        if (!super.shouldBeEnabled()) {
            return false;
        } 
        
        Canvas canvas = getFreeColClient().getCanvas();
        
        if (canvas == null || !canvas.isMapboardActionsEnabled())
        	return false;
        
        float oldScaling = getFreeColClient().getGUI().getImageLibrary().getScalingFactor();

        return oldScaling < 1.0;
    }

    /**
     * Returns the id of this <code>Option</code>.
     * 
     * @return "zoomInAction"
     */
    public String getId() {
        return id;
    }

    /**
     * Applies this action.
     * 
     * @param e The <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent e) {
        float oldScaling = getFreeColClient().getGUI().getImageLibrary().getScalingFactor();
        float newScaling = oldScaling + 1/4f;
        ImageLibrary im;
        if (newScaling >= 1f) {
            newScaling = 1f;
            im = getFreeColClient().getImageLibrary();
        } else {
            try {
                im = getFreeColClient().getImageLibrary().getScaledImageLibrary(newScaling);
            } catch(Exception ex) {
                logger.warning("Failed to retrieve scaled image library.");
                im = getFreeColClient().getImageLibrary();
            }
        }
        getFreeColClient().getGUI().setImageLibrary(im);
        getFreeColClient().getGUI().forceReposition();
        getFreeColClient().getCanvas().refresh();

        update();
        freeColClient.getActionManager().getFreeColAction(ZoomOutAction.id).update();
    }
}
