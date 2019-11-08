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

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.Game;


/**
 * Read and handle incoming messages.
 */
public interface MessageHandler {
    
    /**
     * Handle an incoming message.
     *
     * @param connection The {@code Connection} the message arrived on.
     * @param message The {@code Message} to handle.
     * @return A reply message, if any.
     * @exception FreeColException if the message is malformed.
     */
    public Message handle(Connection connection, Message message)
        throws FreeColException;

    /**
     * Read an incoming Message.
     *
     * @param connection The {@code Connection} to read from.
     * @return The {@code Message} found, or null if none.
     * @exception FreeColException if the message can not be instantiated.
     * @exception XMLStreamException if there is a problem reading the
     *     message.
     */
    public Message read(Connection connection)
        throws FreeColException, XMLStreamException;
}
