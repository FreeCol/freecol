/*
 *  DummyConnection.java - A connection between server and an AI player.
 *
 *  Copyright (C) 2002  The FreeCol Team
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Library General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package net.sf.freecol.server.networking;

import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import java.util.logging.Logger;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.networking.*;

import org.w3c.dom.*;





/**
* A dummy connection, used for AI players.
*/
public final class DummyConnection extends Connection {

    /** The message handler to simulate using when receiving messages. */
    private MessageHandler outgoingMessageHandler;
    
    /**
    * Sets up a dummy connection using the specified {@link MessageHandler}s.
    *
    * @param incomingMessageHandler The MessageHandler to call for each message received.
    * @throws IOException
    */
    public DummyConnection(MessageHandler incomingMessageHandler, MessageHandler outgoingMessageHandler) {
        super();
        setMessageHandler(incomingMessageHandler);
        this.outgoingMessageHandler = outgoingMessageHandler;
    }

    /**
    * Closes this connection.
    *
    * @throws IOException
    */
    public void close() throws IOException {
        // Do nothing.
    }


    /**
    * Sends the given message over this Connection.
    *
    * @param element The element (root element in a DOM-parsed XML tree) that
    *                holds all the information
    * @throws IOException If an error occur while sending the message.
    * @see #sendAndWait
    * @see #ask
    */
    public void send(Element element) throws IOException {
        try {
            outgoingMessageHandler.handle(this, element);
        } catch (FreeColException e) {
            //logger.warning("Can't send to AI player");
        }
    }


    /**
    * Sends a message to the other peer and returns the reply.
    *
    * @param element The question for the other peer.
    * @return The reply from the other peer.
    * @throws IOException If an error occur while sending the message.
    * @see #send
    * @see #sendAndWait
    */
    public Element ask(Element element) throws IOException {
        Element theResult = null;
        try {
            theResult = outgoingMessageHandler.handle(this, element);
        } catch (FreeColException e) {
            //logger.warning("Can't ask AI player");
        }
        return theResult;
    }


    /**
    * Sends the given message over this <code>Connection</code>
    * and waits for confirmation of receiveval before returning.
    *
    * @param element The element (root element in a DOM-parsed XML tree) that
    *                holds all the information
    * @throws IOException If an error occur while sending the message.
    * @see #send
    * @see #ask
    */
    public void sendAndWait(Element element) throws IOException {
        Element reply = ask(element);

        handleAndSendReply(reply);
    }

    /**
    * Sets the outgoing MessageHandler for this Connection.
    * @param The new MessageHandler for this Connection.
    */
    public void setOutgoingMessageHandler(MessageHandler mh) {
        outgoingMessageHandler = mh;
    }

    /**
    * Gets the socket.
    * @return The <code>Socket</code> used while communicating with the other peer.
    */
    public Socket getSocket() {
        return null;
    }
}
