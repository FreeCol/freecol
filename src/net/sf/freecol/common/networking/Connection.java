/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.freecol.FreeCol;

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
public class Connection {

    private static final Logger logger = Logger.getLogger(Connection.class.getName());

    private static final int TIMEOUT = 5000;

    private final XMLOutputFactory xof = XMLOutputFactory.newInstance();

    private InputStream in;

    private Socket socket;

    private OutputStream out;

    private Transformer xmlTransformer;

    private ReceivingThread thread;

    private MessageHandler messageHandler;

    private String name;

    protected static boolean dump
        = FreeCol.getDebugLevel() >= FreeCol.DEBUG_FULL_COMMS;


    /**
     * Trivial constructor.
     *
     * @param name The name of the connection.
     */
    protected Connection(String name) {
        this.in = null;
        this.socket = null;
        this.out = null;
        this.xmlTransformer = null;
        this.thread = null;
        this.messageHandler = null;
        this.name = name;
    }

    /**
     * Sets up a new socket with specified host and port and uses
     * {@link #Connection(Socket, MessageHandler, String)}.
     *
     * @param host The host to connect to.
     * @param port The port to connect to.
     * @param messageHandler The MessageHandler to call for each message
     *            received.
     * @throws IOException
     */
    public Connection(String host, int port, MessageHandler messageHandler,
                      String name) throws IOException {
        this(createSocket(host, port), messageHandler, name);
    }

    /**
     * Creates a new <code>Connection</code> with the specified
     * <code>Socket</code> and {@link MessageHandler}.
     *
     * @param socket The socket to the client.
     * @param messageHandler The MessageHandler to call for each message
     *            received.
     * @throws IOException
     */
    public Connection(Socket socket, MessageHandler messageHandler,
                      String name) throws IOException {
        this(name);

        this.in = socket.getInputStream();
        this.socket = socket;
        this.out = socket.getOutputStream();
        Transformer myTransformer = null;
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            myTransformer = factory.newTransformer();
            myTransformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,
                                            "yes");
        } catch (TransformerException e) {
            logger.log(Level.WARNING, "Failed to install transformer!", e);
        }
        this.xmlTransformer = myTransformer;
        this.thread = new ReceivingThread(this, in, name);
        this.messageHandler = messageHandler;
        this.name = name;

        thread.start();
    }

    /**
     * Creates a socket to communication with a given host, port pair.
     *
     * @param host The host to connect to.
     * @param port The port to connect to.
     * @return A new socket.
     */
    private static Socket createSocket(String host, int port)
        throws IOException {
        Socket socket = new Socket();
        SocketAddress addr = new InetSocketAddress(host, port);
        socket.connect(addr, TIMEOUT);

        return socket;
    }

    /**
     * Gets the socket.
     *
     * @return The <code>Socket</code> used while communicating with the other
     *         peer.
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * Sets the MessageHandler for this Connection.
     *
     * @param mh The new MessageHandler for this Connection.
     */
    public void setMessageHandler(MessageHandler mh) {
        messageHandler = mh;
    }

    /**
     * Gets the MessageHandler for this Connection.
     *
     * @return The MessageHandler for this Connection.
     */
    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    /**
     * Gets the connection name.
     *
     * @return The connection name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sends a "disconnect"-message and closes this connection.
     *
     * @throws IOException
     */
    public void close() throws IOException {
        Element disconnect = DOMMessage.createNewRootElement("disconnect");
        sendDumping(disconnect);
        reallyClose();
    }

    /**
     * Really closes this connection.
     *
     * @throws IOException
     */
    public void reallyClose() throws IOException {
        if (thread != null) thread.askToStop();

        if (out != null) out.close();

        if (socket != null) socket.close();

        if (in != null) in.close();
        logger.fine("Connection really closed.");
    }

    /**
     * Fundamental routine to send a message over this Connection.
     *
     * @param element The <code>Element</code> (root element in a
     *     DOM-parsed XML tree) that holds all the information
     * @param logOK Log the send if true.
     * @throws IOException If an error occur while sending the message.
     */
    private void send(Element element, boolean logOK) throws IOException {
        synchronized (out) {
            try {
                xmlTransformer.transform(new DOMSource(element),
                                         new StreamResult(out));
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to transform and send!", e);
            }

            out.write('\n');
            out.flush();
            out.notifyAll(); // Just in case others are waiting
        }
        if (logOK) logger.fine("Send: " + element.getTagName());
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
        send(element, logger.isLoggable(Level.FINE));
    }

    /**
     * Dumping wrapper for send().
     *
     * @param element The element (root element in a DOM-parsed XML tree) that
     *            holds all the information
     * @throws IOException If an error occur while sending the message.
     * @see #sendAndWait(Element)
     * @see #ask(Element)
     */
    public void sendDumping(Element element) throws IOException {
        if (dump) {
            String x = getName() + "-send";
            try {
                System.err.println("<" + x + ">"
                    + DOMMessage.elementToString(element)
                    + "</" + x + ">\n");
            } catch (Exception e) {}
        }
        send(element);
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
        askDumping(element);
    }

    /**
     * Sends a message to the other peer and returns the reply.
     *
     * @param element The question for the other peer.
     * @return The reply from the other peer.
     * @throws IOException If an error occur while sending the message.
     * @see #send(Element)
     * @see #sendAndWait(Element)
     */
    public Element ask(Element element) throws IOException {
        int networkReplyId = thread.getNextNetworkReplyId();
        String tag = element.getTagName();

        if (Thread.currentThread() == thread) {
            throw new IOException("wait(ReceivingThread) for: " + tag);
        }

        Element question = element.getOwnerDocument()
            .createElement("question");
        question.setAttribute("networkReplyId",
                              Integer.toString(networkReplyId));
        question.appendChild(element);

        NetworkReplyObject nro = thread.waitForNetworkReply(networkReplyId);
        send(question, false);
        DOMMessage response = (DOMMessage)nro.getResponse();
        Element reply = (response == null) ? null
            : response.getDocument().getDocumentElement();

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Ask(" + networkReplyId + "): " + tag + ", reply: "
                + ((reply == null) ? "null" : reply.getTagName()));
        }
        return (reply == null) ? null : (Element)reply.getFirstChild();
    }

    /**
     * Dumping wrapper for ask().
     * Dumps to System.err with a faked-XML prefix so the whole line can
     * be fed to an XML-pretty printer if required.
     *
     * @param request The <code>Element</code> to send.
     * @return The reply element.
     * @exception Throws IOException if ask() fails.
     */
    public Element askDumping(Element request) throws IOException {
        if (dump) {
            String x = getName() + "-request";
            try {
                System.err.println("<" + x + ">"
                    + DOMMessage.elementToString(request)
                    + "</" + x + ">\n");
            } catch (Exception e) {}
        }

        Element reply;
        if (dump) {
            try {
                reply = ask(request);
                try {
                    String x = getName() + "-reply";
                    System.err.println("<" + x + ">"
                        + DOMMessage.elementToString(reply)
                        + "</" + x + ">\n");
                } catch (Exception x) {}
            } catch (IOException e) {
                try {
                    System.err.println("<" + getName() + "-exception e=\""
                        + e.getMessage() + "\" />\n");
                } catch (Exception x) {}
                throw e;
            }
        } else {
            reply = ask(request);
        }

        return reply;
    }

    /**
     * Handles a message using the registered <code>MessageHandler</code>.
     *
     * @param in The stream containing the message.
     */
    public void handleAndSendReply(final BufferedInputStream in) 
        throws IOException {

        // Peek at the reply id and tag.
        in.mark(200);
        final XMLInputFactory xif = XMLInputFactory.newInstance();

        final String networkReplyId;
        final boolean question;
        try {
            final XMLStreamReader xmlIn = xif.createXMLStreamReader(in);
            xmlIn.nextTag();
            networkReplyId = xmlIn.getAttributeValue(null, "networkReplyId");
            question = xmlIn.getLocalName().equals("question");
            xmlIn.close();
        } catch (XMLStreamException xme) {
            logger.log(Level.WARNING, "XML stream failure", xme);
            return;
        } 

        // Reset and build a message.
        final DOMMessage msg;
        in.reset();
        try {
            msg = new DOMMessage(in);
        } catch (SAXException e) {
            logger.log(Level.WARNING, "Unable to read message.", e);
            return;
        }

        // Process the message in its own thread.
        final Connection conn = this;
        Thread t = new Thread(msg.getType()) {
                @Override
                public void run() {
                    Element reply, element = msg.getDocument()
                        .getDocumentElement();
                    try {
                        if (question) {
                            reply = messageHandler.handle(conn,
                                (Element)element.getFirstChild());
                            if (reply == null) {
                                reply = DOMMessage.createNewRootElement("reply");
                                reply.setAttribute("networkReplyId",
                                    networkReplyId);
                            } else {
                                Element header = reply.getOwnerDocument()
                                    .createElement("reply");
                                header.setAttribute("networkReplyId",
                                    networkReplyId);
                                header.appendChild(reply);
                                reply = header;
                            }
                        } else {
                            reply = messageHandler.handle(conn, element);
                        }
                        if (reply != null) conn.sendDumping(reply);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Handler failed: "
                            + element.toString(), e);
                    }
                }
            };
        t.setName(name + "-MessageHandler-" + t.getName());
        t.start();
    }

    /**
     * Override the default and return socket details.
     *
     * @return human-readable description of connection.
     */
    @Override
    public String toString() {
        return "[Connection " + name + " (" + socket + ") ]";
    }
}
