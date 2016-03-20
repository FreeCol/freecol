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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.common.FreeColException;

import org.w3c.dom.Element;


/**
 * The client connection to a server.  Extends the basic connection to
 * remember the host and port to allow reconnection.
 */
public final class ClientConnection extends Connection {

    private static final Logger logger = Logger.getLogger(ClientConnection.class.getName());

    /** The host to connect to. */
    private final String host;

    /** The port to connect to. */
    private final int port;


    /**
     * Creates a new <code>ClientConnection</code>.
     *
     * @param host The host to connect to.
     * @param port The port to connect to.
     * @param handler The <code>MessageHandler</code> to use.
     * @param name The name for the connection.
     * @exception IOException If an exception is thrown while creating
     *     a new {@link Connection}.
     */
    public ClientConnection(String host, int port, MessageHandler handler,
                            String name) throws IOException {
        super(host, port, handler, name);

        this.host = host;
        this.port = port;
    }


    /**
     * Gets the host used by the connection.
     *
     * Used in reconnect.
     *
     * @return The host.
     */
    public String getHost() {
        return this.host;
    }
    
    /**
     * Gets the port used by the connection.
     *
     * Used in reconnect.
     *
     * @return The port.
     */
    public int getPort() {
        return this.port;
    }
}
