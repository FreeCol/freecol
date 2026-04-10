/**
 *  Copyright (C) 2002-2024   The FreeCol Team
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

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.option.GameOptions;


/**
 * An action to toggle the display of national borders.
 */
public class DisplayFogOfWarAction extends SelectableOptionAction {

    public static final String id = "displayFogOfWarAction";


    /**
     * Creates this action.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public DisplayFogOfWarAction(FreeColClient freeColClient) {
        super(freeColClient, id, ClientOptions.DISPLAY_FOG_OF_WAR);

        setSelected(shouldBeSelected());
    }

    
    @Override
    protected boolean shouldBeEnabled() {
        if (!super.shouldBeEnabled()) {
            return false;
        }
        
        /*
         * Disable the option to display the fog-of-war if the game is
         * without fog-of-war. 
         */
        return freeColClient.getGame() == null
                || freeColClient.getGame().getGameOptions() == null
                || freeColClient.getGame().getGameOptions().getBoolean(GameOptions.FOG_OF_WAR);
    }
}
