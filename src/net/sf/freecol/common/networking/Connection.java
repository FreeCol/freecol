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

package net.sf.freecol.common.networking;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.util.DOMUtils;
import net.sf.freecol.common.util.Utils;

import org.w3c.dom.Element;


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

    public static final char END_OF_STREAM = '\n';
    public static final int BUFFER_SIZE = 1 << 14;
    
    public static final String NETWORK_REPLY_ID_TAG = "networkReplyId";
    public static final String QUESTION_TAG = "question";
    public static final String REPLY_TAG = "reply";
    public static final String SEND_SUFFIX = "-send\n";
    public static final String REPLY_SUFFIX = "-reply\n";

    private static final int TIMEOUT = 5000; // 5s

    /** The socket connected to the other end of the connection. */
    private Socket socket = null;

    /** The input stream to read from, derived from the socket. */
    private InputStream in;
    /** A lock for the input side, including the socket. */
    private final Object inputLock = new Object();

    /** The transformer for output. */
    private final Transformer outTransformer;
    /** The output stream to write to, derived from the socket. */
    private OutputStream out;
    /** A lock for the output side. */
    private final Object outputLock = new Object();

    private ReceivingThread receivingThread;

    private MessageHandler messageHandler;

    private String name;

    /** The transformer for logging, also used as a lock for logWriter. */
    private final Transformer logTransformer;
    /** The Writer to write logging messages to. */
    private final Writer logWriter;


    /**
     * Trivial constructor.
     *
     * @param name The name of the connection.
     */
    protected Connection(String name) {
        this.name = name;
        this.outTransformer = Utils.makeTransformer(false, false);

        setSocket(null);
        this.in = null;
        this.out = null;
        this.receivingThread = null;
        this.messageHandler = null;

        // Make a (pretty printing) transformer, but only make the log
        // writer in COMMS-debug mode.
        if (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.COMMS)) {
            this.logWriter = Utils.getUTF8Writer(System.err);
            this.logTransformer = Utils.makeTransformer(true, true);
        } else {
            this.logWriter = null;
            this.logTransformer = null;
        }
    }

    /**
     * Creates a new {@code Connection} with the specified
     * {@code Socket} and {@link MessageHandler}.
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

        setSocket(socket);
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
        this.receivingThread = new ReceivingThread(this, this.in, name);
        this.messageHandler = messageHandler;

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
     * Get the socket.
     *
     * @return The current {@code Socket}.
     */
    public Socket getSocket() {
        synchronized (this.inputLock) {
            return this.socket;
        }
    }

    /**
     * Set the socket.
     *
     * @param socket The new {@code Socket}.
     */
    private void setSocket(Socket socket) {
        synchronized (this.inputLock) {
            this.socket = socket;
        }
    }

    /**
     * Close and clear the socket.
     */
    private void closeSocket() {
        synchronized (this.inputLock) {
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
    }

    /**
     * Close and clear the output stream.
     */
    private void closeOutputStream() {
        synchronized (this.outputLock) {
            if (this.out == null) return;
            try {
                this.out.close();
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "Error closing output", ioe);
            } finally {
                this.out = null;
            }
        }
    }

    /**
     * Close and clear the input stream.
     */
    private void closeInputStream() {
        synchronized (this.inputLock) {
            if (this.in == null) return;
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
        return getSocket() != null;
    }

    /**
     * Get the host address of this connection.
     *
     * @return The host address, or an empty string on error.
     */
    public String getHostAddress() {
        Socket socket = getSocket();
        return (socket == null) ? ""
            : socket.getInetAddress().getHostAddress();
    }

    /**
     * Get the port for this connection.
     *
     * @return The port number, or negative on error.
     */
    public int getPort() {
        Socket socket = getSocket();
        return (socket == null) ? -1 : socket.getPort();
    }

    /**
     * Get the printable description of the socket.
     *
     * @return *host-address*:*port-number* or an empty string on error.
     */
    public String getSocketName() {
        return (isAlive()) ? getHostAddress() + ":" + getPort() : "";
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
     * Sets the MessageHandler for this Connection.
     *
     * @param mh The new MessageHandler for this Connection.
     */
    public void setMessageHandler(MessageHandler mh) {
        this.messageHandler = mh;
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
     * Signal that this connection is disconnecting.
     */
    public void sendDisconnect() {
        send(TrivialMessage.DISCONNECT_MESSAGE);
    }

    /**
     * Send a reconnect message.
     */
    public void sendReconnect() {
        send(TrivialMessage.RECONNECT_MESSAGE);
    }

    /**
     * Disconnect this connection.
     */
    public void disconnect() {
        sendDisconnect();
        close();
    }

    // Element processing.  To go away
    
    /**
     * Write an element into a string writer.
     *
     * @param transformer A {@code Transformer} to convert the
     *     element with.
     * @param element The {@code Element} to write.
     * @return A new {@code StringWriter} containing the element, or
     *     null if the element could not be transformed.
     */
    private StringWriter elementToStringWriter(Transformer transformer,
                                               Element element) {
        StringWriter sw = new StringWriter(BUFFER_SIZE);
        DOMSource source = new DOMSource(element);
        try {
            transformer.transform(source, new StreamResult(sw));
            sw.append(END_OF_STREAM);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Failed to transform element", ex);
            sw = null;
        }
        return sw;
    }

    /**
     * Log transfer of a DOMSource.
     *
     * FIXME: Convert to not use Element.
     *
     * @param element An {@code Element} to log.
     * @param send True if sending (else replying).
     */
    protected void log(Element element, boolean send) {
        if (this.logWriter == null) return;
        synchronized (this.logWriter) {
            StringWriter sw
                = elementToStringWriter(this.logTransformer, element);
            if (sw == null) return;
            StringBuffer sb = sw.getBuffer();
            try {
                sb.insert(0, (send) ? SEND_SUFFIX : REPLY_SUFFIX);
                sb.insert(0, name);
                this.logWriter.write(sb.toString());
                this.logWriter.flush();
            } catch (IOException ioe) {
                ; // Ignore logging failure
            }
        }
    }
    
    /**
     * Low level routine to send a message over this Connection.
     *
     * @param element The {@code Element} (root element in a
     *     DOM-parsed XML tree) that holds all the information
     * @return True if the message was sent.
     * @exception IOException If an error occur while sending the message.
     */
    private boolean sendInternal(Element element) throws IOException {
        try {
            synchronized (this.outputLock) {
                if (this.out == null) return false;
                StringWriter sw
                    = elementToStringWriter(this.outTransformer, element);
                if (sw == null) return false;
                this.out.write(sw.toString().getBytes("UTF-8"));
                this.out.flush();
            }
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception ex) {
            throw new IOException("sendInternal internal fail", ex);
        }
        return true;
    }

    /**
     * Low level routine to sends a message and return the reply.
     *
     * @param element The question for the other peer.
     * @return The reply {@code DOMMessage} from the other peer.
     * @exception IOException if an error occur while sending the message.
     * @see #sendInternal(Element)
     */
    private DOMMessage askInternal(Element element) throws IOException {
        if (element == null) return null;
        final String tag = element.getTagName();
        int networkReplyId = this.receivingThread.getNextNetworkReplyId();

        if (Thread.currentThread() == this.receivingThread) {
            throw new IOException("wait(ReceivingThread) for: " + tag);
        }

        Element question = DOMUtils.createElement(QUESTION_TAG);
        question.setAttribute(NETWORK_REPLY_ID_TAG,
                              Integer.toString(networkReplyId));
        question.appendChild(question.getOwnerDocument()
                                     .importNode(element, true));

        NetworkReplyObject nro
            = this.receivingThread.waitForNetworkReply(networkReplyId);
        if (!sendInternal(question)) return null;
        log(question, true);

        // Wait for response
        DOMMessage response = (DOMMessage)nro.getResponse();
        log((response == null) ? null : response.toXMLElement(), false);
        return response;
    }
    
    /**
     * Main public routine to send a message over this connection.
     *
     * @param element The {@code Element} (root element in a
     *     DOM-parsed XML tree) that holds all the information
     * @return True if the message was sent or was null.
     * @see #sendAndWait(Element)
     * @see #ask(Element)
     */
    public boolean sendElement(Element element) {
        if (element == null) return true;
        final String tag = element.getTagName();
        try {
            sendInternal(element);
            log(element, true);
            logger.fine("Send: " + tag);
            return true;
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Send fail: " + tag, ioe);
        }
        return false;
    }

    /**
     * Sends the given message over this connection and waits for
     * confirmation of reception before returning.
     *
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information
     * @return True if the message was sent or was null.
     * @see #send(Element)
     * @see #ask(Element)
     */
    public boolean sendAndWaitElement(Element element) {
        if (element == null) return true;
        final String tag = element.getTagName();
        try {
            askInternal(element);
            logger.fine("SendAndWait: " + tag);
            return true;
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "SendAndWait fail: " + tag, ioe);
        }
        return false;
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
    public Element askElement(Element element) throws IOException {
        DOMMessage reply = askInternal(element);
        logger.fine("Ask: " + element.getTagName()
            + ", reply: " + ((reply == null) ? "null" : reply.getType()));
        return (reply == null) ? null
            : (Element)reply.toXMLElement().getFirstChild();
    }

    /**
     * Handle a request.
     *
     * @param request The request {@code Element} to handle.
     * @return The reply from the message handler.
     * @exception FreeColException if there is a handler problem.
     */
    public Element handleElement(Element request) throws FreeColException {
        return (this.messageHandler == null) ? null
            : this.messageHandler.handle(this, request);
    }


    public <T extends DOMMessage> void sendAndWait(T message) throws IOException {
        sendAndWaitElement(message.toXMLElement());
    }
    public <T extends DOMMessage> Element ask(T message) throws IOException {
        return askElement(message.toXMLElement());
    }

    /**
     * Sends a message to the peer and returns a message in reply.
     *
     * @param game The {@code Game} to create the reply message in.
     * @param message The {@code DOMMessage} to send.
     * @return A {@code DOMMessage} created from the reply.
     */
    public <T extends DOMMessage> DOMMessage ask(Game game, T message) {
        Element reply;
        try {
            reply = ask(message);
        } catch (IOException e) {
            return new ErrorMessage(StringTemplate
                .template("connection.io")
                .addName("%extra%", e.getMessage()));
        }
        return DOMUtils.createMessage(game, reply);
    }

    /**
     * Handle a query (has QUESTION_TAG), with given reply identifier,
     * and send a reply (has REPLY_TAG and the given reply identifier).
     * 
     * @param msg The query {@code DOMMessage}.
     * @param replyId The reply identifier.
     * @exception FreeColException if there is a handler problem.
     * @exception IOException if sending fails.
     */
    public void handleQuery(DOMMessage msg, int replyId)
        throws FreeColException, IOException {
        Element element = msg.toXMLElement(), reply;
        element = (Element)element.getFirstChild();
        reply = handleElement(element);
        msg = new DOMMessage(REPLY_TAG,
            NETWORK_REPLY_ID_TAG, Integer.toString(replyId));
        if (reply != null) msg.add(reply);
        send(msg);
    }

    /**
     * Handle an ordinary message, and if the response is non-null send it.
     * 
     * @param msg The {@code DOMMessage} to handle.
     * @exception FreeColException if there is a handler problem.
     * @exception IOException if sending fails.
     */
    public void handleUpdate(DOMMessage msg)
        throws FreeColException, IOException {
        Element reply = handle(msg);
        if (reply != null) sendElement(reply);
    }

    /**
     * Handle a message.
     *
     * @param message The {@code DOMMessage} to handle.
     * @return The response from the handler.
     * @exception FreeColException if there is a handler problem.
     */
    public Element handle(DOMMessage message) throws FreeColException {
        Element element = message.toXMLElement();
        return handleElement(element);
    }


    // Client handling

    /**
     * Client request.
     *
     * @param T The message type.
     * @param game The enclosing {@code Game}.
     * @param message A {@code DOMMessage} to process.
     * @return True if the message was sent, the reply handled, and the
     *     reply was not an error message.
     */
    public <T extends DOMMessage> boolean request(Game game, T message) {
        // Better if we could do this, but it fails for now.
        //
        // DOMMessage reply = ask(game, message);
        // if (reply == null) return true;
        // final String tag = reply.getType();

        Element reply = null;
        try {
            reply = ask(message);
        } catch (IOException ioe) {
            reply = new ErrorMessage(StringTemplate
                .template("connection.io")
                .addName("%extra%", ioe.getMessage())).toXMLElement();
        }
        if (reply == null) return true;
        final String tag = reply.getTagName();
        
        try {
            Element e = handleElement(reply);
            assert e == null; // client handlers now all return null
        } catch (FreeColException fce) {
            logger.log(Level.WARNING, "Failed to handle: " + tag, fce);
            return false;
        }
        return !ErrorMessage.TAG.equals(tag);
    }
        
    /**
     * Client send.
     *
     * @param T The message type.
     * @param message A {@code DOMMessage} to send.
     * @return True if the message was sent.
     */
    public <T extends DOMMessage> boolean send(T message) {
        return sendElement(message.toXMLElement());
    }
    

    // Implement Closeable

    /**
     * Close this connection.
     */
    public void close() {
        if (this.receivingThread != null) this.receivingThread.askToStop();

        closeOutputStream();
        closeInputStream();
        closeSocket();
        
        logger.fine("Connection closed for " + this.name);
    }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(32);
        sb.append("[Connection ").append(this.name).append(" (")
            .append(getSocketName()).append(")]");
        return sb.toString();
    }
}
