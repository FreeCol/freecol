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

package net.sf.freecol.client.networking;

import java.io.IOException;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.MessageHandler;
import net.sf.freecol.common.networking.ServerAPI;


/**
 * Implementation of the ServerAPI for a player with a real connection
 * to the server.
 */
public class UserServerAPI extends ServerAPI {

    /** The connection used to communicate with the server. */
    private Connection connection = null;

    /** The last name used to login with. */
    private String name = null;

    /** The last host connected to. */
    private String host = null;

    /** The last port connected to. */
    private int port = -1;

    /** The last message handler specified. */
    private MessageHandler messageHandler = null;


    /**
     * Create the new user wrapper for the server API.
     */
    public UserServerAPI() {
        super();
    }

    /**
     * Name accessor.
     *
     * @return The connection name.
     */
    private synchronized String getName() {
        return this.name;
    }

    /**
     * Host accessor.
     *
     * @return The connection host.
     */
    private synchronized String getHost() {
        return this.host;
    }

    /**
     * Port accessor.
     *
     * @return The connection port.
     */
    private synchronized int getPort() {
        return this.port;
    }

    /**
     * Update the connection parameters so as to allow reconnection.
     *
     * @param name The connection name.
     * @param host The host connected to.
     * @param port The port connected to.
     */
    private synchronized void updateParameters(String name, String host, int port) {
        this.name = name;
        this.host = host;
        this.port = port;
    }        

    /**
     * A connection has been made, save it and its parameters.
     *
     * @param c The new {@code Connection}.
     */
    private synchronized void updateConnection(Connection c) {
        c.setMessageHandler(this.messageHandler);
        c.setWriteScope(FreeColXMLWriter.WriteScope.toServer());
        this.connection = c;
    }

    /**
     * Create a new connection.
     *
     * @param name The name to associate with the connection.
     * @param host The name of the host to connect to.
     * @param port The port to connect to.
     * @return The new <code>Connection</code>.
     * @exception IOException on failure to connect.
     */
    private static Connection newConnection(String name, String host, int port)
        throws IOException {
        int tries;
        if (port < 0) {
            port = FreeCol.getServerPort();
            tries = 10;
        } else {
            tries = 1;
        }
        Connection conn = null;
        for (int i = tries; i > 0; i--) {
            try {
                conn = new Connection(host, port, name);
                conn.startReceiving();
                break;
            } catch (IOException e) {
                if (i <= 1) throw e;
            }
        }
        return conn;
    }


    // Implement ServerAPI

    /**
     * {@inheritDoc}
     */
    public Connection connect(String name, String host, int port)
        throws IOException {
        Connection c = newConnection(name, host, port);
        if (c == null) return null;
        updateConnection(c);
        updateParameters(name, host, port);
        return c;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized boolean disconnect() {
        if (this.connection != null) {
            this.connection.disconnect();
            this.connection = null;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public Connection reconnect() throws IOException {
        Connection c = newConnection(getName(), getHost(), getPort());
        if (c != null) updateConnection(c);
        return c;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Connection getConnection() {
        return this.connection;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void setMessageHandler(MessageHandler mh) {
        super.setMessageHandler(mh);
        this.messageHandler = mh;
    }
}
