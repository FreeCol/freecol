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
import net.sf.freecol.client.ClientOptions;

import net.sf.freecol.client.FreeColClient;


/**
 * An action for enable/disable fog of war in on the minimap.
 */
public class MiniMapToggleFogOfWarAction extends MapboardAction {

    public static final String id = "miniMapToggleFogOfWarAction";


    /**
     * Creates a new <code>MiniMapToggleFogAction</code>.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public MiniMapToggleFogOfWarAction(FreeColClient freeColClient) {
        super(freeColClient, id);
        
        addImageIcons("toggle_fog_of_war");
    }

    /**
     * Creates a new <code>MiniMapToggleFogAction</code>.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param b a <code>boolean</code> value
     */
    public MiniMapToggleFogOfWarAction(FreeColClient freeColClient, boolean b) {
        super(freeColClient, id + ".secondary");

        addImageIcons("toggle_fog_of_war");
    }
    
    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        getGUI().miniMapToggleFogOfWarControls();

        if (freeColClient.getClientOptions().getBoolean(ClientOptions.MINIMAP_TOGGLE_FOG_OF_WAR)) {
            addImageIcons("toggle_fog_of_war");
        } else {
            addImageIcons("toggle_fog_of_war_no");
        }
    }
}

