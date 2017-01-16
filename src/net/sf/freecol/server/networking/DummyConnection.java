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

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.MessageHandler;

import org.w3c.dom.Element;


/**
 * A dummy connection, used for AI players.
 */
public final class DummyConnection extends Connection {

    /** The message handler to simulate using when receiving messages. */
    private MessageHandler outgoingMessageHandler;

    /** The other connection, to which outgoing requests are forwarded .*/
    private DummyConnection otherConnection;


    /**
     * Sets up a dummy connection using the specified {@link MessageHandler}s.
     *
     * @param name A name for this connection.
     * @param incomingMessageHandler The {@code MessageHandler}
     *     to call for each message received.
     */
    public DummyConnection(String name, MessageHandler incomingMessageHandler) {
        super(name);
        
        setMessageHandler(incomingMessageHandler);
    }


    /**
     * Sets the outgoing MessageHandler for this Connection.
     *
     * @param mh The new MessageHandler for this Connection.
     */
    private void setOutgoingMessageHandler(MessageHandler mh) {
        this.outgoingMessageHandler = mh;
    }

    /**
     * Gets the {@code DummyConnection} this object is connected to.
     *
     * @return The {@code DummyConnection} .
     */
    public DummyConnection getOtherConnection() {
        return otherConnection;
    }

    /**
     * Sets the other connection for this dummy connection.
     *
     * @param dc The {@code DummyConnection} to connect to.
     */
    public void setOtherConnection(DummyConnection dc) {
        this.otherConnection = dc;
        setOutgoingMessageHandler(dc.getMessageHandler());
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
    public boolean sendElement(Element element) {
        if (!isAlive()) return false;
        try {
            getOtherConnection().handleElement(element);
            log(element, true);
            return true;
        } catch (FreeColException e) {}
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean sendAndWaitElement(Element request) {
        try {
            log(request, true);
            Element reply = getOtherConnection().handleElement(request);
            log(reply, false);
            return true;
        } catch (FreeColException fce) {}
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
    public Element askElement(Element request) throws IOException {
        if (!isAlive()) return null;
        Element reply;
        try {
            log(request, true);
            reply = getOtherConnection().handleElement(request);
            log(reply, false);            
        } catch (FreeColException e) {
            reply = null;
        }
        return reply;
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
