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
    public static final String COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    /** The message handler to simulate using when receiving messages. */
    private MessageHandler outgoingMessageHandler;

    private DummyConnection otherConnection;

    /**
     * Name for dummy connection, makes it possible to identify a specific
     * instance.
     */
    private final String _name;


    /**
     * Sets up a dummy connection using the specified {@link MessageHandler}s.
     * 
     * @param name The name that identifies the connection.
     * @param incomingMessageHandler The MessageHandler to call for each message
     *            received.
     */
    public DummyConnection(String name, MessageHandler incomingMessageHandler) {
        super();
        _name = name;
        setMessageHandler(incomingMessageHandler);
    }

    /**
     * Closes this connection.
     * 
     * @throws IOException Will not be thrown by a <code>DummyConnection</code>,
     *             but added because of the superclass' specification.
     */
    public void close() throws IOException {
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
    public void send(Element element) throws IOException {
        try {
            outgoingMessageHandler.handle(getOtherConnection(), element);
        } catch (FreeColException e) {
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
        }

        return theResult;
    }

    /**
     * Sends the given message over this <code>Connection</code> and waits for
     * confirmation of receiveval before returning.
     * 
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information
     * @throws IOException If an error occur while sending the message.
     * @see #send(Element)
     * @see #ask(Element)
     */
    public void sendAndWait(Element element) throws IOException {
        ask(element);
    }

    /**
     * Sets the outgoing MessageHandler for this Connection.
     * 
     * @param mh The new MessageHandler for this Connection.
     */
    private void setOutgoingMessageHandler(MessageHandler mh) {
        outgoingMessageHandler = mh;
    }

    /**
     * Sets the outgoing MessageHandler for this Connection.
     * 
     * @param c The connectio to get the messagehandler from.
     */
    public void setOutgoingMessageHandler(DummyConnection c) {
        otherConnection = c;
        setOutgoingMessageHandler(c.getMessageHandler());
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
     * Return a human-readable string with the dummy connection name.
     * 
     * @return string for debugging.
     */
    public String toString() {
        return "DummyConnection[" + _name + "]";
    }
}
