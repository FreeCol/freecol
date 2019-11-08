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

package net.sf.freecol.client.control;

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.FreeColClientHolder;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.MessageHandler;


/**
 * Message handler implementation for the client side.
 */
public final class ClientInputHandler extends FreeColClientHolder
    implements MessageHandler {

    private static final Logger logger = Logger.getLogger(ClientInputHandler.class.getName());

    /**
     * The constructor to use.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public ClientInputHandler(FreeColClient freeColClient) {
        super(freeColClient);
    }

    
    // Implement MessageHandler

    /**
     * {@inheritDoc}
     */
    public Message handle(@SuppressWarnings("unused") Connection connection,
                          Message message) throws FreeColException {
        message.clientHandler(getFreeColClient());
        return null; 
    }

    /**
     * {@inheritDoc}
     */
    public Message read(Connection connection)
        throws FreeColException, XMLStreamException {
        return Message.read(getGame(), connection.getFreeColXMLReader());
    }
}
