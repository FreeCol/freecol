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

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;

import javax.swing.AbstractButton;
import javax.swing.KeyStroke;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.panel.MapControls;

/**
 * An action for displaying the map controls.
 * 
 * @see MapControls
 */
public class MapControlsAction extends SelectableAction {

    public static final String id = "mapControlsAction";

    private MapControls mapControls;


    /**
     * Creates this action.
     * 
     * @param freeColClient The main controller object for the client.
     */
    MapControlsAction(FreeColClient freeColClient) {
        super(freeColClient, id);
        setSelected(freeColClient.getClientOptions().getBoolean(ClientOptions.DISPLAY_MAP_CONTROLS));
    }

    /**
     * Updates the "enabled"-status and calls {@link #showMapControls(boolean)}.
     */
    public void update() {
        super.update();

        showMapControls(enabled && isSelected());
    }

    /**
     * Returns The MapControls object.
     * 
     * @return The MapControls object.
     */
    public MapControls getMapControls() {
        return mapControls;
    }

    /**
     * Applies this action.
     * 
     * @param e The <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent e) {
        selected = ((AbstractButton) e.getSource()).isSelected();
        showMapControls(enabled && selected);
    }

    private void showMapControls(boolean value) {
        if (value && getFreeColClient().getGUI().isInGame()) {
            if (mapControls == null) {
                mapControls = new MapControls(getFreeColClient());
            }
            mapControls.update();
        }
        if (mapControls != null) {
            if (value) {
                if (!mapControls.isShowing()) {
                    mapControls.addToComponent(getFreeColClient().getCanvas());
                }
                mapControls.update();
            } else {
                if (mapControls.isShowing()) {
                    mapControls.removeFromComponent(getFreeColClient().getCanvas());
                }
            }
        }
    }
}
