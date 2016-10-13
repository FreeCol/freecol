/**
 *  Copyright (C) 2002-2016   The FreeCol Team
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

package net.sf.freecol.common.networking;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent to signal connection disconnection.
 */
public class DisconnectMessage extends AttributeMessage {

    public static final String TAG = "disconnect";
    private static final String REASON_TAG = "reason";


    /**
     * Create a new {@code DisconnectMessage}.
     *
     * @param reason The new AI state.
     */
    public DisconnectMessage(String reason) {
        super(TAG, REASON_TAG, reason);
    }

    /**
     * Create a new {@code DisconnectMessage} from a supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public DisconnectMessage(Game game, Element element) {
        super(TAG, REASON_TAG, getStringAttribute(element, REASON_TAG));
    }


    // Public interface

    /**
     * Get the disconnection reason.
     *
     * @return The reason.
     */
    public String getReason() {
        return getAttribute(REASON_TAG);
    }

    // No single handle() method, disconnection response varies.
}
