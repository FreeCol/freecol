
package net.sf.freecol.client.networking;

import net.sf.freecol.common.networking.*;
import java.io.IOException;
import java.util.logging.Logger;

import org.w3c.dom.Element;


/**
* The client that can connect to a server.
*/
public final class Client {
    private static final Logger logger = Logger.getLogger(Client.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


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
    * @throws IOException If an exception is thrown while creating
    *         a new {@link Connection}.
    */
    public Client(String host, int port, MessageHandler handler) throws IOException {
        this.host = host;
        this.port = port;
        c = new Connection(host, port, handler);
    }



    public String getHost() {
        return host;
    }
    
    
    public int getPort() {
        return port;
    }
    
    
    /**
    * Sends the specified message to the server.
    *
    * @param element The element (root element in a DOM-parsed XML tree) that
    *                holds all the information
    * @see #sendAndWait
    * @see #ask
    */
    public void send(Element element) {
        try {
            c.send(element);
        } catch (IOException e) {
            logger.warning("Could not send the specified message: " + element);
        }
    }


    /**
    * Sends the specified message to the server and waits for the reply
    * to be returned before returning from this method.
    *
    * @param element The element (root element in a DOM-parsed XML tree) that
    *                holds all the information
    * @see #send
    * @see #ask
    */
    public void sendAndWait(Element element) {
        try {
            c.sendAndWait(element);
        } catch (IOException e) {
            logger.warning("Could not send the specified message: " + element);
        }
    }


    /**
    * Sends the specified message to the server and returns the reply.
    *
    * @param element The element (root element in a DOM-parsed XML tree) that
    *                holds all the information
    * @see #sendAndWait
    * @see #send
    */
    public Element ask(Element element) {
        try {
            return c.ask(element);
        } catch (IOException e) {
            logger.warning("Could not send the specified message: " + element);
        }

        return null;
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
    * Disconnects this client from the server.
    */
    public void disconnect() {
        try {
            c.close();
        } catch (IOException e) {
            logger.warning("Exception while closing connection: " + e);
        }
    }


    /**
    * Sets the <code>MessageHandler</code> for this <code>Client</code>.
    * The <code>MessageHandler</code> is the class responsible for receiving
    * and handling the network messages.
    *
    * @param The new <code>MessageHandler</code> for this <code>Client</code>.
    */
    public void setMessageHandler(MessageHandler mh) {
        c.setMessageHandler(mh);
    }
}
