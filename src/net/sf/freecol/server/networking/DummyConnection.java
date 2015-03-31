/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

    private DummyConnection otherConnection;


    /**
     * Sets up a dummy connection using the specified {@link MessageHandler}s.
     *
     * @param incomingMessageHandler The <code>MessageHandler</code>
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
     * Sets the other connection for this dummy connection.
     *
     * @param dc The <code>DummyConnection</code> to connect to.
     */
    public void setConnection(DummyConnection dc) {
        this.otherConnection = dc;
        setOutgoingMessageHandler(dc.getMessageHandler());
    }

    /**
     * Gets the <code>DummyConnection</code> this object is connected to.
     *
     * @return The <code>DummyConnection</code> .
     */
    public DummyConnection getOtherConnection() {
        return otherConnection;
    }

    /**
     * Closes this connection.
     */
    @Override
    public void close() {
        // Do nothing.
    }

    /**
     * Sends the given message over this Connection.
     *
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information
     * @throws IOException If an error occur while sending the message.
     * @see #sendAndWait(Element)
     * @see #ask(Element)
     */
    @Override
    public void send(Element element) throws IOException {
        try {
            outgoingMessageHandler.handle(getOtherConnection(), element);
            log(element, true);
        } catch (FreeColException e) {
        }
    }

    /**
     * Sends the given message over this <code>Connection</code> and waits for
     * confirmation of receival before returning.
     *
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information
     * @throws IOException If an error occur while sending the message.
     * @see #send(Element)
     * @see #ask(Element)
     */
    @Override
    public void sendAndWait(Element element) throws IOException {
        send(element);
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
    public Element ask(Element request) throws IOException {
        Element reply;
        try {
            log(request, true);
            reply = outgoingMessageHandler.handle(getOtherConnection(),
                                                  request);
            log(reply, false);            
        } catch (FreeColException e) {
            reply = null;
        }
        return reply;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "[DummyConnection " + getName() + "]";
    }
}
