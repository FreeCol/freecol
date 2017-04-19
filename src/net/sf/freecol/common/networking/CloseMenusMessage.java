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

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;

import org.w3c.dom.Element;


/**
 * A trivial message sent to clients to signal that menus should be closed.
 */
public class CloseMenusMessage extends TrivialMessage {

    public static final String TAG = "closeMenus";
    

    /**
     * Create a new {@code CloseMenusMessage}.
     */
    public CloseMenusMessage() {
        super(TAG);
    }

    /**
     * Create a new {@code CloseMenusMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     * @exception FreeColException if the internal message can not be read.
     */
    public CloseMenusMessage(Game game, FreeColXMLReader xr)
        throws FreeColException, XMLStreamException {
        super(TAG, game, xr);
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
        // This is a trivial handler to allow the server to signal to
        // the client that an offer that caused a popup (for example,
        // a native demand or diplomacy proposal) has not been
        // answered quickly enough and that the offering player has
        // assumed this player has refused-by-inaction, and therefore,
        // the popup needs to be closed.
        invokeAndWait(freeColClient, () -> igc(freeColClient).closeMenus());
    }
}
