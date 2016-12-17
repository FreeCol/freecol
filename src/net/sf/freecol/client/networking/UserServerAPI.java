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
    private Connection connection;

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
     *
     * @param gui The {@code GUI} to use for user interaction.
     */
    public UserServerAPI(GUI gui) {
        super();

        this.gui = gui;
    }


    /**
     * Just forget about the client.
     *
     * Only call this if we are sure it is dead.
     */
    public void reset() {
        this.connection = null;
    }

    /**
     * Sets the message handler for the connection.
     *
     * @param mh The new {@code MessageHandler}.
     */
    public void setMessageHandler(MessageHandler mh) {
        if (this.connection != null) {
            this.connection.setMessageHandler(mh);
        }
        this.messageHandler = mh;
    }


    // Implement ServerAPI

    /**
     * {@inheritDoc}
     */
    public Connection connect(String name, String host, int port,
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
                this.connection = new Connection(host, port, messageHandler, name);
                if (this.connection != null) {
                    // Connected, save the connection information
                    this.name = name;
                    this.host = host;
                    this.port = port;
                    this.messageHandler = messageHandler;
                    break;
                }
            } catch (IOException e) {
                if (i <= 1) throw e;
            }
        }
        return this.connection;
    }

    /**
     * {@inheritDoc}
     */
    public boolean disconnect() {
        if (this.connection != null) {
            this.connection.disconnect();
            this.connection.close();
            reset();
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public Connection reconnect() throws IOException {
        return connect(this.name, this.host, this.port, this.messageHandler);
    }

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
        return this.connection;
    }
}
