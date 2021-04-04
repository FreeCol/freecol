/**
 *  Copyright (C) 2002-2021   The FreeCol Team
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
import net.sf.freecol.common.model.Player;


/**
 * An action for retiring from the game.
 */
public class RetireAction extends FreeColAction {

    public static final String id = "retireAction";


    /**
     * Creates a new {@code RetireAction}.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public RetireAction(FreeColClient freeColClient) {
        super(freeColClient, id);
    }


    // Override FreeColAction

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean shouldBeEnabled() {
        Player player = freeColClient.getMyPlayer();
        return player != null
            && player.getPlayerType() != Player.PlayerType.RETIRED
            && super.shouldBeEnabled();
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        freeColClient.retire();
    }
}
