/**
 *  Copyright (C) 2002-2017   The FreeCol Team
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
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent to the clients to signal a new turn.
 */
public class NewTurnMessage extends AttributeMessage {

    public static final String TAG = "newTurn";
    private static final String TURN_TAG = "turn";


    /**
     * Create a new {@code NewTurnMessage} with the
     * supplied message.
     *
     * @param turn The new {@code Turn}.
     */
    public NewTurnMessage(Turn turn) {
        super(TAG, TURN_TAG, String.valueOf(turn.getNumber()));
    }

    /**
     * Create a new {@code NewTurnMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     * @throws IllegalStateException if there is problem with the senderID.
     */
    public NewTurnMessage(Game game, Element element) {
        super(TAG, TURN_TAG, getStringAttribute(element, TURN_TAG));
    }


    // Public interface

    /**
     * Get the turn number.
     *
     * @return The turn number.
     */
    public int getTurnNumber() {
        return getIntegerAttribute(TURN_TAG, 0);
    }


    // No server handler method required.
    // This message is only sent to clients.
}
