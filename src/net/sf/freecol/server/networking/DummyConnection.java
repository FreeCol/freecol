
package net.sf.freecol.server.networking;

import java.io.IOException;
import java.net.Socket;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.networking.*;

import org.w3c.dom.*;





/**
* A dummy connection, used for AI players.
*/
public final class DummyConnection extends Connection {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    /** The message handler to simulate using when receiving messages. */
    private MessageHandler outgoingMessageHandler;
    private DummyConnection otherConnection;
    
    
    /**
    * Sets up a dummy connection using the specified {@link MessageHandler}s.
    *
    * @param incomingMessageHandler The MessageHandler to call for each message received.
    * @throws IOException
    */
    public DummyConnection(MessageHandler incomingMessageHandler) {
        super();
        setMessageHandler(incomingMessageHandler);
    }

    
    /**
    * Sets up a dummy connection using the specified {@link MessageHandler}s.
    *
    * @param incomingMessageHandler The MessageHandler to call for each message received.
    * @throws IOException
    */
    /*public DummyConnection(MessageHandler incomingMessageHandler, MessageHandler outgoingMessageHandler) {
        super();
        setMessageHandler(incomingMessageHandler);
        this.outgoingMessageHandler = outgoingMessageHandler;
    }*/

    

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
            outgoingMessageHandler.handle(getOtherConnection(), element);
        } catch (FreeColException e) {
            //logger.warning("Can't ask AI player");
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
            theResult = outgoingMessageHandler.handle(getOtherConnection(), element);
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

        if (reply != null) {
            handleAndSendReply(reply);
        }
    }

    /**
    * Sets the outgoing MessageHandler for this Connection.
    * @param mh The new MessageHandler for this Connection.
    */
    private void setOutgoingMessageHandler(MessageHandler mh) {
        outgoingMessageHandler = mh;
    }

    
    /**
    * Sets the outgoing MessageHandler for this Connection.
    * @param c The connectio to get the messagehandler from.
    */
    public void setOutgoingMessageHandler(DummyConnection c) {
        otherConnection = c;
        setOutgoingMessageHandler(c.getMessageHandler());
    }
    
    
    /**
    * Gets the <code>DummyConnection</code> this object is
    * connected to.
    */
    public DummyConnection getOtherConnection() {
        return otherConnection;
    }


    /**
    * Gets the socket.
    * @return The <code>Socket</code> used while communicating with the other peer.
    */
    public Socket getSocket() {
        return null;
    }
}
