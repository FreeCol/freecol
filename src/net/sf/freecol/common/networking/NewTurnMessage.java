/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.model.ServerPlayer;


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
     * Create a new {@code NewTurnMessage} from a stream.
     *
     * @param game The {@code Game} to read within.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     */
    public NewTurnMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, TURN_TAG);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public MessagePriority getPriority() {
        return Message.MessagePriority.NORMAL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void aiHandler(FreeColServer freeColServer, AIPlayer aiPlayer) {
        // Ignored
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clientHandler(FreeColClient freeColClient) {
        final int turn = getTurnNumber();

        if (turn < 0) {
            logger.warning("Invalid turn for newTurn: "
                + Integer.toString(turn) );
            return;
        }

        igc(freeColClient).newTurnHandler(turn);
        clientGeneric(freeColClient);
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
}
