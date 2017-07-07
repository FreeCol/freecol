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

package net.sf.freecol.server.networking;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.DOMMessageHandler;

import org.w3c.dom.Element;


/**
 * A dummy connection, used for AI players.
 */
public final class DummyConnection extends Connection {

    private static final Logger logger = Logger.getLogger(DummyConnection.class.getName());

    /** The other connection, to which outgoing requests are forwarded .*/
    private DummyConnection otherConnection;


    /**
     * Sets up a dummy connection using the specified message handler.
     *
     * @param name A name for this connection.
     * @param incomingMessageHandler The {@code DOMMessageHandler}
     *     to call for each message received.
     */
    public DummyConnection(String name,
                           DOMMessageHandler incomingMessageHandler) {
        super(name);
        
        setDOMMessageHandler(incomingMessageHandler);
    }


    /**
     * Gets the {@code DummyConnection} this object is connected to.
     *
     * @return The {@code DummyConnection} .
     */
    public DummyConnection getOtherConnection() {
        return this.otherConnection;
    }

    /**
     * Sets the other connection for this dummy connection.
     *
     * @param dc The {@code DummyConnection} to connect to.
     */
    public void setOtherConnection(DummyConnection dc) {
        this.otherConnection = dc;
    }


    // Override Connection

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAlive() {
        return this.otherConnection != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        this.otherConnection = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean sendElement(Element element) {
        return sendAndWaitElement(element);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean sendAndWaitElement(Element request) {
        DummyConnection other = getOtherConnection();
        if (other == null) return false;
        if (request == null) return true;
        final String tag = request.getTagName();
        try {
            log(request, true);
            Element reply = other.handleElement(request);
            log(reply, false);
            return true;
        } catch (FreeColException fce) {
            logger.log(Level.WARNING, "Dummy send-handler fail: " + tag, fce);
        }
        return false;
    }

    /**
     * Sends a message to the other peer and returns the reply.
     *
     * @param request The question for the other peer.
     * @return The reply from the other peer.
     * @throws IOException If an error occur while sending the message.
     * @see #send
     * @see #sendAndWait
     */
    @Override
    protected Element askElement(Element request) throws IOException {
        if (!isAlive() || request == null) return null;
        final String tag = request.getTagName();
        Element reply;
        try {
            log(request, true);
            reply = getOtherConnection().handleElement(request);
            log(reply, false);            
        } catch (FreeColException fce) {
            logger.log(Level.WARNING, "Dummy-ask handler fail: " + tag, fce);
            reply = null;
        }
        return reply;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean sendMessage(Message message) throws IOException {
        DummyConnection other = getOtherConnection();
        if (other == null) return false;
        if (message == null) return true;
        log(message, true);
        final String tag = message.getType();
        Message reply;
        try {
            reply = other.handle(message);
            assert reply == null;
        } catch (FreeColException fce) {
            logger.log(Level.WARNING, "Dummy sendMessage fail: " + tag, fce);
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Message askMessage(Message message) throws IOException {
        DummyConnection other = getOtherConnection();
        if (other == null) return null;
        if (message == null) return null;
        log(message, true);
        final String tag = message.getType();
        Message response;
        try {
            response = other.handle(message);
        } catch (FreeColException fce) {
            logger.log(Level.WARNING, "Dummy askMessage fail: " + tag, fce);
            return null;
        }
        log(response, false);
        return response;
    }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "[DummyConnection " + getName() + "]";
    }
}
