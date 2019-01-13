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
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message that contains a fountainOfYouth string.
 */
public class FountainOfYouthMessage extends AttributeMessage {

    public static final String TAG = "fountainOfYouth";
    private static final String MIGRANTS_TAG = "migrants";


    /**
     * Create a new {@code FountainOfYouthMessage}.
     *
     * @param migrants The number of new migrants on the docks.
     */
    public FountainOfYouthMessage(int migrants) {
        super(TAG, MIGRANTS_TAG, String.valueOf(migrants));
    }

    /**
     * Create a new {@code FountainOfYouthMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     */
    public FountainOfYouthMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, MIGRANTS_TAG);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public MessagePriority getPriority() {
        return Message.MessagePriority.LATE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void aiHandler(FreeColServer freeColServer, AIPlayer aiPlayer) {
        final int n = getMigrants();

        aiPlayer.fountainOfYouthHandler(n);
    }

    /**
     * {@inheritDoc}
     */
    public void clientHandler(FreeColClient freeColClient) {
        final int n = getMigrants();

        if (n <= 0) {
            logger.warning("Invalid migrants attribute: " + n);
            return;
        }

        igc(freeColClient).fountainOfYouthHandler(n);
        clientGeneric(freeColClient);
    }


    // Public interface

    /**
     * Get the number of migrants.
     *
     * @return The number of migrants.
     */
    public int getMigrants() {
        return getIntegerAttribute(MIGRANTS_TAG, -1);
    }
}
