/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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


import org.w3c.dom.Element;


/**
 * The client connection to a server.
 */
public final class Client {
    private static final Logger logger = Logger.getLogger(Client.class.getName());

    /**
     * The <code>Connection</code> this <code>Client</code> uses when
     * communicating with the server.
     */
    private final Connection c;

    private String host;
    private int port;


    /**
     * Creates a new <code>Client</code>.
     *
     * @param host The host to connect to.
     * @param port The port to connect to.
     * @param handler The MessageHandler to use.
     * @param name The name for the connection.
     * @throws IOException If an exception is thrown while creating
     *         a new {@link Connection}.
     */
    public Client(String host, int port, MessageHandler handler, String name)
        throws IOException {
        this.host = host;
        this.port = port;
        c = new Connection(host, port, handler, name);
    }

    /**
     * Gets the host used by the connection.
     * Used in reconnect.
     *
     * @return The host.
     */
    public String getHost() {
        return host;
    }
    
    /**
     * Gets the port used by the connection.
     * Used in reconnect.
     *
     * @return The port.
     */
    public int getPort() {
        return port;
    }
    
    /**
     * Gets the <code>Connection</code> this <code>Client</code> uses when
     * communicating with the server.
     *
     * @return The {@link Connection}.
     */
    public Connection getConnection() {
        return c;
    }

    /**
     * Sets the <code>MessageHandler</code> for this <code>Client</code>.
     * The <code>MessageHandler</code> is the class responsible for receiving
     * and handling the network messages.
     *
     * @param mh The new <code>MessageHandler</code> for this client.
     */
    public void setMessageHandler(MessageHandler mh) {
        c.setMessageHandler(mh);
    }

    /**
     * Disconnects this client from the server.
     */
    public void disconnect() {
        c.close();
    }


    /**
     * Sends the specified message to the server.
     *
     * @param element The element (root element in a DOM-parsed XML tree) that
     *                holds all the information
     * @see #sendAndWait(Element)
     * @see #ask(Element)
     */
    public void send(Element element) {
        try {
            c.sendDumping(element);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not send: " + element, e);
        }
    }

    /**
     * Sends the specified message to the server and waits for the reply
     * to be returned before returning from this method.
     *
     * @param element The element (root element in a DOM-parsed XML tree) that
     *                holds all the information
     * @see #send(Element)
     * @see #ask(Element)
     */
    public void sendAndWait(Element element) {
        try {
            c.sendAndWait(element);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not sendAndWait: " + element, e);
        }
    }

    /**
     * Sends the specified message to the server and returns the reply.
     *
     * @param element The element (root element in a DOM-parsed XML tree)
     *       that holds all the information
     * @return The answer from the server or <code>null</code> if either
     *       an error occured or the server did not send a reply.
     * @see #sendAndWait
     * @see #send
     */
    public Element ask(Element element) {
        try {
            return c.askDumping(element);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not ask: " + element, e);
        }
        return null;
    }
}
