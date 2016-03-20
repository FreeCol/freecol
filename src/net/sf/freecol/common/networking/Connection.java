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

package net.sf.freecol.common.networking;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.util.Utils;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;


/**
 * A network connection.
 * Responsible for both sending and receiving network messages.
 *
 * @see #send(Element)
 * @see #sendAndWait(Element)
 * @see #ask(Element)
 */
public class Connection implements Closeable {

    private static final Logger logger = Logger.getLogger(Connection.class.getName());

    public static final byte END_OF_STREAM = '\n';
    
    public static final String DISCONNECT_TAG = "disconnect";
    public static final String NETWORK_REPLY_ID_TAG = "networkReplyId";
    public static final String QUESTION_TAG = "question";
    public static final String RECONNECT_TAG = "reconnect";
    public static final String REPLY_TAG = "reply";
    public static final String SEND_SUFFIX = "-send\n";
    public static final String REPLY_SUFFIX = "-reply\n";

    private static final int TIMEOUT = 5000; // 5s

    private InputStream in;

    private Socket socket;

    /** The output stream to write to. */
    private OutputStream out;
    /** A lock to protect the output stream. */
    private Object outLock = new Object();

    private final Transformer xmlTransformer;

    private ReceivingThread receivingThread;

    private MessageHandler messageHandler;

    private String name;

    // Logging variables.
    private final StreamResult logResult;
    private final Writer logWriter;


    /**
     * Trivial constructor.
     *
     * @param name The name of the connection.
     */
    protected Connection(String name) {
        this.in = null;
        this.socket = null;
        this.out = null;
        this.xmlTransformer = Utils.makeTransformer(false, false);
        this.receivingThread = null;
        this.messageHandler = null;
        this.name = name;
        if (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.COMMS)) {
            this.logWriter = new BufferedWriter(new OutputStreamWriter(System.err));
            this.logResult = new StreamResult(this.logWriter);
        } else {
            this.logWriter = null;
            this.logResult = null;
        }
    }

    /**
     * Creates a new <code>Connection</code> with the specified
     * <code>Socket</code> and {@link MessageHandler}.
     *
     * @param socket The socket to the client.
     * @param messageHandler The MessageHandler to call for each message
     *     received.
     * @param name The connection name.
     * @exception IOException if streams can not be derived from the socket.
     */
    public Connection(Socket socket, MessageHandler messageHandler,
                      String name) throws IOException {
        this(name);

        this.socket = socket;
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
        this.receivingThread = new ReceivingThread(this, this.in, name);
        this.messageHandler = messageHandler;
        this.name = name;

        this.receivingThread.start();
    }

    /**
     * Sets up a new socket with specified host and port and uses
     * {@link #Connection(Socket, MessageHandler, String)}.
     *
     * @param host The host to connect to.
     * @param port The port to connect to.
     * @param messageHandler The MessageHandler to call for each message
     *     received.
     * @param name The name for the connection.
     * @exception IOException if the socket creation is problematic.
     */
    public Connection(String host, int port, MessageHandler messageHandler,
                      String name) throws IOException {
        this(createSocket(host, port), messageHandler, name);
    }


    /**
     * Creates a socket to communication with a given host, port pair.
     *
     * @param host The host to connect to.
     * @param port The port to connect to.
     * @return A new socket.
     * @exception IOException on failure to create/connect the socket.
     */
    private static Socket createSocket(String host, int port)
        throws IOException {
        Socket socket = new Socket();
        SocketAddress addr = new InetSocketAddress(host, port);
        socket.connect(addr, TIMEOUT);
        return socket;
    }

    /**
     * Close and clear the socket.
     */
    private void closeSocket() {
        if (this.socket != null) {
            try {
                this.socket.close();
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "Error closing socket", ioe);
            } finally {
                this.socket = null;
            }
        }
    }
    
    /**
     * Close and clear the output stream.
     */
    private void closeOutputStream() {
        IOException ioe = null;
        synchronized (this.outLock) {
            if (this.out == null) return;
            try {
                this.out.close();
            } catch (IOException e) {
                ioe = e;
            } finally {
                this.out = null;
            }
        }
        if (ioe != null) {
            logger.log(Level.WARNING, "Error closing output", ioe);
        }
    }

    /**
     * Close and clear the input stream.
     */
    private void closeInputStream() {
        if (this.in != null) {
            try {
                this.in.close();
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "Error closing input", ioe);
            } finally {
                this.in = null;
            }
        }
    }

    /**
     * Is this connection alive?
     *
     * @return True if the connection is alive.
     */
    public boolean isAlive() {
        return this.socket != null;
    }

    /**
     * Gets the socket.
     *
     * @return The <code>Socket</code> used while communicating with
     *     the other peer.
     */
    public Socket getSocket() {
        return this.socket;
    }

    /**
     * Sets the MessageHandler for this Connection.
     *
     * @param mh The new MessageHandler for this Connection.
     */
    public void setMessageHandler(MessageHandler mh) {
        this.messageHandler = mh;
    }

    /**
     * Gets the MessageHandler for this Connection.
     *
     * @return The MessageHandler for this Connection.
     */
    public MessageHandler getMessageHandler() {
        return this.messageHandler;
    }

    /**
     * Gets the connection name.
     *
     * @return The connection name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Close this connection.
     */
    @Override
    public void close() {
        try {
            disconnect();
        } catch (IOException ioe) {
            // Failure is expected if the other end has closed already
            logger.log(Level.FINE, "Disconnect failed for " + this.name, ioe);
        } finally {
            reallyClose();
        }
    }

    /**
     * Really closes this connection.
     */
    public void reallyClose() {
        if (this.receivingThread != null) this.receivingThread.askToStop();

        closeOutputStream();
        closeInputStream();
        closeSocket();
        
        logger.fine("Connection really closed for " + this.name);
    }

    /**
     * Log transfer of a DOMSource.
     *
     * @param element The <code>Element</code> to log.
     * @param send True if sending (else replying).
     */
    protected void log(Element element, boolean send) {
        if (this.logResult == null) return;
        try {
            this.logWriter.write(name, 0, name.length());
            if (send) {
                this.logWriter.write(SEND_SUFFIX, 0, SEND_SUFFIX.length());
            } else {
                this.logWriter.write(REPLY_SUFFIX, 0, REPLY_SUFFIX.length());
            }
            this.xmlTransformer.transform(new DOMSource(element), this.logResult);
            this.logWriter.write('\n');
            this.logWriter.flush();
        } catch (IOException|TransformerException e) {
            ; // Ignore logging failure
        }
    }

    /**
     * Low level routine to send a message over this Connection.
     *
     * @param element The <code>Element</code> (root element in a
     *     DOM-parsed XML tree) that holds all the information
     * @exception IOException If an error occur while sending the message.
     */
    private void sendInternal(Element element) throws IOException {
        TransformerException te = null;
        synchronized (this.outLock) {
            if (this.out == null) return;
            DOMSource source = new DOMSource(element);
            try {
                xmlTransformer.transform(source, new StreamResult(this.out));
            } catch (TransformerException e) {
                te = e;
            }
            this.out.write(END_OF_STREAM);
            this.out.flush();
        }
        log(element, true);
        if (te != null) logger.log(Level.WARNING, "Failed to transform", te);
    }

    /**
     * Low level routine to sends a message and return the reply.
     *
     * @param element The question for the other peer.
     * @return The reply from the other peer.
     * @exception IOException if an error occur while sending the message.
     * @see #sendInternal(Element)
     */
    private Element askInternal(Element element) throws IOException {
        if (element == null) return null;
        final String tag = element.getTagName();
        int networkReplyId = this.receivingThread.getNextNetworkReplyId();

        if (Thread.currentThread() == this.receivingThread) {
            throw new IOException("wait(ReceivingThread) for: " + tag);
        }

        Element question = element.getOwnerDocument()
            .createElement(QUESTION_TAG);
        question.setAttribute(NETWORK_REPLY_ID_TAG,
                              Integer.toString(networkReplyId));
        question.appendChild(element);

        NetworkReplyObject nro
            = this.receivingThread.waitForNetworkReply(networkReplyId);
        sendInternal(question);

        // Wait for response
        DOMMessage response = (DOMMessage)nro.getResponse();
        Element reply = (response == null) ? null : response.toXMLElement();
        log(reply, false);

        return (reply == null) ? null : (Element)reply.getFirstChild();
    }


    // Wrappers, to be promoted soon
    public void send(DOMMessage message) throws IOException {
        send(message.toXMLElement());
    }
    public void sendAndWait(DOMMessage message) throws IOException {
        sendAndWait(message.toXMLElement());
    }
    public Element ask(DOMMessage message) throws IOException {
        return ask(message.toXMLElement());
    }

    
    /**
     * Main public routine to send a message over this connection.
     *
     * @param element The <code>Element</code> (root element in a
     *     DOM-parsed XML tree) that holds all the information
     * @exception IOException If an error occur while sending the message.
     * @see #sendAndWait(Element)
     * @see #ask(Element)
     */
    public void send(Element element) throws IOException {
        sendInternal(element);
        logger.fine("Send: " + element.getTagName());
    }

    /**
     * Sends the given message over this connection and waits for
     * confirmation of reception before returning.
     *
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information
     * @exception IOException If an error occur while sending the message.
     * @see #send(Element)
     * @see #ask(Element)
     */
    public void sendAndWait(Element element) throws IOException {
        askInternal(element);
        logger.fine("SendAndWait: " + element.getTagName());
    }

    /**
     * Sends a message to the other peer and returns the reply.
     *
     * @param element The question for the peer.
     * @return The reply from the peer.
     * @exception IOException if an error occur while sending the message.
     * @see #send(Element)
     * @see #sendAndWait(Element)
     */
    public Element ask(Element element) throws IOException {
        Element reply = askInternal(element);
        logger.fine("Ask: " + element.getTagName()
            + ", reply: " + ((reply == null) ? "null" : reply.getTagName()));
        return reply;
    }

    /**
     * Sends a message to the peer and returns a message in reply.
     *
     * @param game The <code>Game</code> to create the reply message in.
     * @param message The <code>DOMMessage</code> to send.
     * @return A <code>DOMMessage</code> created from the reply.
     */
    public DOMMessage ask(Game game, DOMMessage message) {
        Element reply;
        try {
            reply = ask(message);
        } catch (IOException e) {
            return new ErrorMessage("reject", e.getMessage());
        }
        return DOMMessage.createMessage(game, reply);
    }

    /**
     * Sends a message to the peer and returns a message of requested type
     * or error in reply.
     *
     * @param game The <code>Game</code> to create the reply message in.
     * @param message The <code>DOMMessage</code> to send.
     * @param replyTag The requested tag of the reply.
     * @return A <code>DOMMessage</code> in reply, either of the requested
     *     type or error.
     */
    public DOMMessage ask(Game game, DOMMessage message, String replyTag) {
        DOMMessage reply = ask(game, message);
        return (reply != null && (reply.isType(replyTag)
                || reply.isType(ErrorMessage.TAG))) ? reply
            : new ErrorMessage("reject", "Request: "
                + ((message == null) ? "null" : message.getType())
                + ", Reply: " + ((reply == null) ? "null" : reply.getType())
                + ", Expected: " + ((replyTag == null) ? "null" : replyTag));
    }

    /**
     * Handle a query (has QUESTION_TAG), with given reply identifier,
     * and send a reply (has REPLY_TAG and the given reply identifier).
     * 
     * @param msg The query <code>DOMMessage</code>.
     * @param replyId The reply identifier.
     * @exception FreeColException if there is a handler problem.
     * @exception IOException if sending fails.
     */
    public void handleQuery(DOMMessage msg, int replyId)
        throws FreeColException, IOException {
        Element element = msg.toXMLElement(), reply;
        element = (Element)element.getFirstChild();
        reply = handle(element);
        msg = new DOMMessage(REPLY_TAG,
            NETWORK_REPLY_ID_TAG, Integer.toString(replyId));
        if (reply != null) msg.add(reply);
        send(msg);
    }

    /**
     * Handle an ordinary message, and if the response is non-null send it.
     * 
     * @param msg The <code>DOMMessage</code> to handle.
     * @exception FreeColException if there is a handler problem.
     * @exception IOException if sending fails.
     */
    public void handleUpdate(DOMMessage msg)
        throws FreeColException, IOException {
        Element element = msg.toXMLElement();
        Element reply = handle(element);
        if (reply != null) send(reply);
    }

    /**
     * Handle a request.
     *
     * @param request The request <code>Element</code> to handle.
     * @return The reply from the message handler.
     * @exception FreeColException if there is trouble with the response.
     */
    public Element handle(Element request) throws FreeColException {
        return this.messageHandler.handle(this, request);
    }


    /**
     * Send a disconnect message.
     *
     * @exception IOException if failed to send the message.
     */
    public void disconnect() throws IOException {
        this.send(new DOMMessage(DISCONNECT_TAG));
    }

    /**
     * Send a reconnect message, ignoring (but logging) I/O errors.
     */
    public void reconnect() {
        try {
            this.send(new DOMMessage(RECONNECT_TAG));
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Reconnect failed for " + this.name,
                ioe);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(32);
        sb.append("[Connection ").append(this.name);
        if (this.socket != null) {
            sb.append(" (").append(this.socket.getInetAddress())
                .append(":").append(this.socket.getPort()).append(")");
        }
        sb.append("]");
        return sb.toString();
    }
}
