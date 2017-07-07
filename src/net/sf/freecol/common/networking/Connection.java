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
import java.net.MalformedURLException;
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
import net.sf.freecol.common.io.FreeColNetworkInputStream;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.util.DOMUtils;
import net.sf.freecol.common.util.Utils;

import org.xml.sax.SAXException;
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
    private static final char[] END_OF_STREAM_ARRAY = { END_OF_STREAM };
    public static final int BUFFER_SIZE = 1 << 14;
    
    public static final String NETWORK_REPLY_ID_TAG = "networkReplyId";
    public static final String QUESTION_TAG = "question";
    public static final String REPLY_TAG = "reply";
    public static final String SEND_SUFFIX = "-send";
    public static final String REPLY_SUFFIX = "-reply";

    private static final int TIMEOUT = 5000; // 5s

    /** The socket connected to the other end of the connection. */
    private Socket socket = null;

    /** The input stream to read from, derived from the socket. */
    private InputStream in;
    /** The wrapped version of the input stream. May go away. */
    private FreeColNetworkInputStream fcnis;
    /** A lock for the input side, including the socket. */
    private final Object inputLock = new Object();

    /** The transformer for output. */
    private final Transformer outTransformer;
    /** The output stream to write to, derived from the socket. */
    private OutputStream out;
    /** A lock for the output side. */
    private final Object outputLock = new Object();

    /** A stream wrapping of the input stream (currently transient). */
    private FreeColXMLReader xr;
    
    private ReceivingThread receivingThread;

    private DOMMessageHandler domMessageHandler;

    private String name;

    /** Main message writer. */
    private FreeColXMLWriter xw;

    /** A lock for the logging routines. */
    private final Object logLock = new Object();
    /** The transformer for logging, also used as a lock for logWriter. */
    private final Transformer logTransformer;
    /** The Writer to write logging messages to. */
    private final Writer logWriter;
    /** The FreeColXMLWriter to write logging messages to. */
    private final FreeColXMLWriter lw;
    

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
        this.fcnis = null;
        this.xr = null;
        this.receivingThread = null;
        this.domMessageHandler = null;
        this.xw = null;

        // Make a (pretty printing) transformer, but only make the log
        // writer in COMMS-debug mode.
        FreeColXMLWriter lw = null;
        if (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.COMMS)) {
            this.logWriter = Utils.getUTF8Writer(System.err);
            this.logTransformer = Utils.makeTransformer(true, true);
            try {
                lw = new FreeColXMLWriter(this.logWriter,
                    FreeColXMLWriter.WriteScope.toSave(), true);
            } catch (IOException ioe) {} // Ignore failure, just do not log
        } else {
            this.logWriter = null;
            this.logTransformer = null;
        }
        this.lw = lw;
    }

    /**
     * Creates a new {@code Connection} with the specified
     * {@code Socket} and {@link MessageHandler}.
     *
     * @param socket The socket to the client.
     * @param domMessageHandler The {@code DOMMessageHandler} to call
     *     for each message received.
     * @param name The connection name.
     * @exception IOException if streams can not be derived from the socket.
     */
    public Connection(Socket socket, DOMMessageHandler domMessageHandler,
                      String name) throws IOException {
        this(name);

        setSocket(socket);
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
        this.fcnis = new FreeColNetworkInputStream(this.in);
        this.receivingThread = new ReceivingThread(this, name);
        this.domMessageHandler = domMessageHandler;
        this.xw = new FreeColXMLWriter(this.out,
            FreeColXMLWriter.WriteScope.toSave(), false);

        this.receivingThread.start();
    }

    /**
     * Sets up a new socket with specified host and port and uses
     * {@link #Connection(Socket, MessageHandler, String)}.
     *
     * @param host The host to connect to.
     * @param port The port to connect to.
     * @param domMessageHandler The {@code DOMMessageHandler} to call
     *     for each message received.
     * @param name The name for the connection.
     * @exception IOException if the socket creation is problematic.
     */
    public Connection(String host, int port, DOMMessageHandler domMessageHandler,
                      String name) throws IOException {
        this(createSocket(host, port), domMessageHandler, name);
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
            if (this.xw != null) {
                this.xw.close();
                this.xw = null;
            }
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
            if (this.fcnis != null) {
                this.fcnis.reallyClose();
                this.fcnis = null;
            }
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
     * Gets the message handler for this connection.
     *
     * Work in progress: we are cutting over the implementers of
     * the DOMMessageHandler interface to also implement the
     * MessageHandler interface.  In due course this.domMessageHandler
     * will become a MessageHandler.
     *
     * @return The {@code MessageHandler} for this Connection.
     */
    private MessageHandler getMessageHandler() {
        return (this.domMessageHandler instanceof MessageHandler)
            ? (MessageHandler)this.domMessageHandler
            : null;
    }

    /**
     * Gets the message handler for this connection.
     *
     * @return The {@code DOMMessageHandler} for this Connection.
     */
    public DOMMessageHandler getDOMMessageHandler() {
        return this.domMessageHandler;
    }

    /**
     * Sets the message handler for this connection.
     *
     * @param mh The new {@code DOMMessageHandler} for this Connection.
     */
    public void setDOMMessageHandler(DOMMessageHandler mh) {
        this.domMessageHandler = mh;
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
        send(TrivialMessage.disconnectMessage);
    }

    /**
     * Send a reconnect message.
     */
    public void sendReconnect() {
        send(TrivialMessage.reconnectMessage);
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
        synchronized (this.logLock) {
            if (this.logWriter == null) return;
            StringWriter sw
                = elementToStringWriter(this.logTransformer, element);
            if (sw == null) return;
            StringBuffer sb = sw.getBuffer();
            try {
                sb.insert(0, "\n");
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
                String buf = sw.toString();
                this.out.write(buf.getBytes("UTF-8"));
                this.out.flush();
            }
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception ex) {
            throw new IOException("sendInternal fail", ex);
        }
        return true;
    }

    /**
     * Low level routine to sends a message and return the reply.
     *
     * @param element The element to wrap as a question for the peer.
     * @return The {@code Element} unwrapped from the reply from the peer.
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
        DOMMessage msg = (DOMMessage)nro.getResponse();
        Element response = (msg == null) ? null : msg.toXMLElement();
        log(response, false);
        return (response == null) ? null : (Element)response.getFirstChild();
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
    protected boolean sendElement(Element element) {
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
    protected boolean sendAndWaitElement(Element element) {
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
    protected Element askElement(Element element) throws IOException {
        Element reply = askInternal(element);
        logger.fine("Ask: " + element.getTagName()
            + ", reply: " + ((reply == null) ? "null" : reply.getTagName()));
        return reply;
    }

    /**
     * Handle a request.
     *
     * @param request The request {@code Element} to handle.
     * @return The reply from the message handler.
     * @exception FreeColException if there is a handler problem.
     */
    public Element handleElement(Element request) throws FreeColException {
        return (this.domMessageHandler == null) ? null
            : this.domMessageHandler.handle(this, request);
    }


    public <T extends DOMMessage> void sendAndWait(T message) throws IOException {
        sendAndWaitElement(message.toXMLElement());
    }

    public <T extends DOMMessage> Element ask(T message) throws IOException {
        try {
            return askElement(message.toXMLElement());
        } catch (Exception e) { // Catch undocumented exception in Java libs
            logger.log(Level.WARNING, "Unexpected exception", e);
        }
        return null;
    }

    /**
     * Sends the given message over this connection and waits for
     * confirmation of reception before returning.
     *
     * @param message The {@code Message} to send.
     * @return True if the message was sent or was null.
     */
    public boolean sendAndWaitMessage(Message message) {
        if (message == null) return true;
        boolean ret = sendAndWaitElement(((DOMMessage)message).toXMLElement());
        logger.fine("SendAndWait: " + message.getType());
        return ret;
    }

    // ReceivingThread input support, work in progress

    public FreeColXMLReader getFreeColXMLReader() {
        return this.xr;
    }

    public String startListen() throws XMLStreamException {
        this.fcnis.enable();
        try {
            this.fcnis.fill();
        } catch (IOException ioe) {
            throw new XMLStreamException("Fill failure: " + ioe.getMessage());
        }
        
        this.fcnis.mark(Connection.BUFFER_SIZE);
        try {
            this.xr = new FreeColXMLReader(this.fcnis); //.setTracing(true);
        } catch (Exception ex) {
            return DisconnectMessage.TAG;
        }
        this.xr.nextTag();
        return this.xr.getLocalName();
    }

    public int getReplyId() {
        return (this.xr == null) ? -1
            : this.xr.getAttribute(NETWORK_REPLY_ID_TAG, -1);
    }

    public void endListen() {
        if (this.fcnis != null) this.fcnis.close(); // Just clears the data
    }

    public DOMMessage domReader()
        throws IOException, SAXException {
        this.fcnis.enable();
        this.fcnis.reset();
        return new DOMMessage(this.fcnis);
    }

    // DOM handlers for ReceivingThread

    /**
     * Handle a query (has QUESTION_TAG), with given reply identifier,
     * and send a reply (has REPLY_TAG and the given reply identifier).
     * 
     * @param msg The query {@code DOMMessage}.
     * @param replyId The reply identifier.
     * @exception FreeColException if there is a handler problem.
     * @exception IOException if sending fails.
     */
    public void handleQuestion(DOMMessage msg, int replyId)
        throws FreeColException, IOException {
        Element element = msg.toXMLElement(), reply;
        element = (Element)element.getFirstChild();
        reply = handleElement(element);
        msg = new DOMMessage(REPLY_TAG,
            NETWORK_REPLY_ID_TAG, Integer.toString(replyId));
        if (reply != null) msg.add(reply);
        sendElement(msg.toXMLElement());
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
        Element reply = handleElement(msg.toXMLElement());
        if (reply != null) sendElement(reply);
    }


    // Low level Message routines
    
    // lowest level ask
    private Message askMessageInternal(Message message) throws IOException {
        final String tag = message.getType();
        final int replyId = this.receivingThread.getNextNetworkReplyId();

        if (Thread.currentThread() == this.receivingThread) {
            throw new IOException("wait(ReceivingThread) for: " + tag);
        }

        QuestionMessage qm = new QuestionMessage(replyId, message);
        NetworkReplyObject nro = this.receivingThread.waitForNetworkReply(replyId);
        log(qm, true);
        if (!sendMessageInternal(qm)) return null;

        // Wait for response
        Object response = nro.getResponse();
        if (!(response instanceof ReplyMessage)) {
            throw new IOException("Bad response to " + tag + ": " + response);
        }
        return ((ReplyMessage)response).getMessage();
    }

    /**
     * Send a message, and return the response.  Log both.
     *
     * @param message The {@code Message} to send.
     * @return The response.
     * @exception IOException on failure to send.
     */
    protected Message askMessage(Message message) throws IOException {
        if (message == null) return null;
        Message response = askMessageInternal(message);
        log(response, false);
        return response;
    }
    
    // lowest level send
    private boolean sendMessageInternal(Message message) throws IOException {
        final String tag = message.getType();
        try {
            synchronized (this.outputLock) {
                if (this.xw == null) return false;
                message.toXML(this.xw);
                this.xw.writeCharacters(END_OF_STREAM_ARRAY, 0,
                                        END_OF_STREAM_ARRAY.length);
                this.xw.flush();
            }
        } catch (Exception ex) {
            throw new IOException("sendMessageInternal fail: " + tag, ex);
        }
        return true;
    }

    /**
     * Send a message, do not consider a response.
     *
     * Public as this is called from ReceivingThread.
     *
     * @param message The {@code Message} to send.
     * @return True if the message was null or successfully sent.
     * @exception IOException on failure to send.
     */
    public boolean sendMessage(Message message) throws IOException {
        if (message == null) return true;
        sendMessageInternal(message);
        log(message, true);
        return true;
    }

    /**
     * Log a message.
     *
     * @param message The {@code Message} to log.
     * @param send True if this is a send, false if a reply.
     */
    protected void log(Message message, boolean send) {
        synchronized (this.logLock) {
            if (this.lw == null) return;
            try {
                this.lw.writeComment(this.name
                    + ((send) ? SEND_SUFFIX : REPLY_SUFFIX));
                if (message != null) message.toXML(this.lw);
                this.lw.writeCharacters(END_OF_STREAM_ARRAY, 0,
                                        END_OF_STREAM_ARRAY.length);
                this.lw.flush();
            } catch (XMLStreamException xse) {} // Ignore log failure
        }
    }
    

    // MessageHandler support

    /**
     * Handle a message using the MessageHandler.
     *
     * @param message The {@code Message} to handle.
     * @return The result of the handler.
     * @exception FreeColException if the message is corrupt.
     */
    public Message handle(Message message) throws FreeColException {
        Message ret = null;
        if (message != null) {
            final MessageHandler mh = getMessageHandler();
            if (mh == null) { // FIXME: Temporary hack
                throw new FreeColException(getName() + " has no handler for "
                    + message.getType()).preserveDebug();
            }
            ret = mh.handle(message);
        }
        return ret;
    }

    /**
     * Read a message using the MessageHandler.
     *
     * @return The {@code Message} found, if any.
     * @exception FreeColException there is a problem creating the message.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public Message reader()
        throws FreeColException, XMLStreamException {
        if (this.xr == null) return null;

        MessageHandler mh = getMessageHandler();
        if (mh == null) { // FIXME: Temporary fast fail
            throw new FreeColException("No handler at " + xr.getLocalName())
                .preserveDebug();
        }
        return mh.read(this);
    }


    // Client handling

    // Legacy DOM routine, to go away
    private boolean requestElement(Element request) {
        Element reply = null;
        try {
            reply = askElement(request);
        } catch (IOException ioe) {
            reply = new ErrorMessage("connection.io", ioe).toXMLElement();
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

    // In due course this is to replace request() but should handle
    // IOException in that form.
    private boolean requestMessage(Message message) throws IOException {
        final String outTag = message.getType();
        Message response = askMessage(message);
        if (response == null) return true;

        final String inTag = response.getType();
        try {
            Message reply = handle(response);
            assert reply == null; // Client message handlers all return null
            logger.finest("Client ask: " + outTag + " -> " + inTag);
            return true;
        } catch (FreeColException fce) {
            logger.log(Level.FINEST, "Client ask failure: " + outTag
                + " -> " + inTag, fce);
        }
        return false; // !ErrorMessage.TAG.equals(inTag);
    }

    /**
     * Client request.
     *
     * @param message A {@code Message} to process.
     * @return True if the message was sent, the reply handled, and the
     *     reply was not an error message.
     */
    public boolean request(Message message) {
        if (message == null) return true;

        // Try Message-based routine
        final String tag = message.getType();
        boolean ret = false;
        if (false) { // DISABLED FOR NOW
            try {
                ret = requestMessage(message);
            } catch (IOException ioe) {
                logger.log(Level.FINEST, this.name + " request("
                    + tag + ") fail", ioe);
                ret = false;
            }
        }
        
        // Temporary fall back to using DOMMessage
        if (!ret) {
            //logger.warning(this.name + " fallback-request(" +  tag + ") "
            //    + (message instanceof DOMMessage));
            if (message instanceof DOMMessage) {
                ret = requestElement(((DOMMessage)message).toXMLElement());
            }
        }

        if (ret) {
            logger.finest(this.name + " request( " + tag + ") ok");
        } else {
            logger.warning(this.name + " request( " + tag + ") failed");
        }            
        return ret;
    }
        
    /**
     * Client send.
     *
     * @param message A {@code DOMMessage} to send.
     * @return True if the message was sent.
     */
    public boolean send(Message message) {
        if (message == null) return true;
        final String tag = message.getType();
        try {
            if (sendMessage(message)) {
                logger.finest(this.name + " send(" + tag + ") ok");
                return true;
            }
            logger.warning(this.name + " send(" + tag + ") failed");
        } catch (IOException ioe) {
            logger.log(Level.WARNING, this.name + " send("
                + tag + ") exception", ioe);
        }
        return false;
    }
    

    // Implement Closeable

    /**
     * Close this connection.
     */
    public void close() {
        if (this.receivingThread != null) {
            this.receivingThread.askToStop("connection closing");
            this.receivingThread = null;
        }

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
