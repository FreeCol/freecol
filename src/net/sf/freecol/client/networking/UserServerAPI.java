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

package net.sf.freecol.client.networking;

import java.io.IOException;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.networking.ClientConnection;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.MessageHandler;
import net.sf.freecol.common.networking.ServerAPI;

import org.w3c.dom.Element;


/**
 * Implementation of the ServerAPI for a player with attached GUI and
 * real connection to the server.
 */
public class UserServerAPI extends ServerAPI {

    /** The GUI to use for error and client processing. */
    private final GUI gui;

    /** The connection used to communicate with the server. */
    private ClientConnection clientConnection;


    /**
     * Create the new user wrapper for the server API.
     *
     * @param gui The <code>GUI</code> to use for user interaction.
     */
    public UserServerAPI(GUI gui) {
        super();

        this.gui = gui;
    }


    // Client manipulation

    /**
     * Connects a client to host:port (or more).
     *
     * @param threadName The name for the thread.
     * @param host The name of the machine running the
     *     <code>FreeColServer</code>.
     * @param port The port to use when connecting to the host.
     * @param messageHandler The <code>MessageHandler</code> to use on this
     *     connection.
     * @return True if the connection succeeded.
     * @exception IOException on connection failure.
     */
    public boolean connect(String threadName, String host, int port,
                           MessageHandler messageHandler) 
        throws IOException {
        int tries;
        if (port < 0) {
            port = FreeCol.getServerPort();
            tries = 10;
        } else {
            tries = 1;
        }
        for (int i = tries; i > 0; i--) {
            try {
                this.clientConnection = new ClientConnection(host, port,
                    messageHandler, threadName);
                if (this.clientConnection != null) break;
            } catch (IOException e) {
                if (i <= 1) throw e;
            }
        }
        return this.clientConnection != null;
    }

    /**
     * Disconnect the client connection..
     *
     * @exception IOException on failure to send the required messages.
     */
    public void disconnect() throws IOException {
        if (this.clientConnection != null) {
            this.clientConnection.disconnect();
            reset();
        }
    }

    /**
     * Get the host we are connected to.
     *
     * @return The current host, or null if none.
     */
    public String getHost() {
        return (this.clientConnection == null) ? null
            : this.clientConnection.getHost();
    }

    /**
     * Get the port we are connected to.
     *
     * @return The current port, or negative if none.
     */     
    public int getPort() {
        return (this.clientConnection == null) ? -1
            : this.clientConnection.getPort();
    }

    /**
     * Just forget about the client.
     *
     * Only call this if we are sure it is dead.
     */
    public void reset() {
        this.clientConnection = null;
    }

    /**
     * Sets the message handler for the connection.
     *
     * @param mh The new <code>MessageHandler</code>.
     */
    public void setMessageHandler(MessageHandler mh) {
        if (this.clientConnection != null) {
            this.clientConnection.setMessageHandler(mh);
        }
    }


    // Implement ServerAPI
    
    /**
     * {@inheritDoc}
     */
    protected void doClientProcessingFor(Element reply) {
        String sound = reply.getAttribute("sound");
        if (sound != null && !sound.isEmpty()) {
            this.gui.playSound(sound);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Connection getConnection() {
        return this.clientConnection;
    }
}
