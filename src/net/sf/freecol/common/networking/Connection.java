/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
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

/**
 * A network connection. Responsible for both sending and receiving network
 * messages.
 *
 * @see #send(Element)
 * @see #sendAndWait(Element)
 * @see #ask(Element)
 */
public class Connection {

    private static final Logger logger = Logger.getLogger(Connection.class.getName());

    private static final int TIMEOUT = 5000;

    private final OutputStream out;

    private final InputStream in;

    private final Socket socket;

    private final Transformer xmlTransformer;

    private final ReceivingThread thread;

    private final XMLOutputFactory xof = XMLOutputFactory.newInstance();

    private MessageHandler messageHandler;

    private XMLStreamWriter xmlOut = null;

    private int currentQuestionID = -1;

    private String name = null;


    /**
     * Trivial constructor for DummyConnection to use.
     */
    protected Connection(String name) {
        out = null;
        in = null;
        socket = null;
        thread = null;
        xmlTransformer = null;
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
        this.messageHandler = messageHandler;
        this.socket = socket;
        this.name = name;

        out = socket.getOutputStream();
        in = socket.getInputStream();

        Transformer myTransformer;
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            myTransformer = factory.newTransformer();
            myTransformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        } catch (TransformerException e) {
            logger.log(Level.WARNING, "Failed to install transformer!", e);
            myTransformer = null;
        }
        xmlTransformer = myTransformer;

        thread = new ReceivingThread(this, in, name);
        thread.start();
    }

    private static Socket createSocket(String host, int port) throws IOException {
        Socket socket = new Socket();
        SocketAddress addr = new InetSocketAddress(host, port);
        socket.connect(addr, TIMEOUT);

        return socket;
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
        Element disconnectElement = DOMMessage.createNewRootElement("disconnect");
        send(disconnectElement);

        reallyClose();
    }

    /**
     * Closes this connection.
     *
     * @throws IOException
     */
    public void reallyClose() throws IOException {
        if (thread != null) {
            thread.askToStop();
        }

        if (out != null) {
            out.close();
        }

        if (socket != null) {
            socket.close();
        }

        if (in != null) {
            in.close();
        }

        logger.info("Connection closed.");
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
        // Note - waits for question but does not install a new value.
        // Must hold out for entire call.
        synchronized (out) {
            while (currentQuestionID != -1) {
                try {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("Waiting to send element " + element.getTagName() + "...");
                    }
                    out.wait();
                } catch (InterruptedException e) {
                }
            }
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Sending element " + element.getTagName() + "...");
            }
            try {
                xmlTransformer.transform(new DOMSource(element), new StreamResult(out));
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to transform and send element!", e);
            }

            out.write('\n');
            out.flush();

            // Just in case others are waiting
            out.notifyAll();
        }
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

        Element questionElement = element.getOwnerDocument().createElement("question");
        questionElement.setAttribute("networkReplyId", Integer.toString(networkReplyId));
        questionElement.appendChild(element);

        if (Thread.currentThread() == thread) {
            logger.warning("Attempt to 'wait()' the ReceivingThread for sending " + element.getTagName());
            throw new IOException("Attempt to 'wait()' the ReceivingThread.");
        } else {
            NetworkReplyObject nro = thread.waitForNetworkReply(networkReplyId);
            send(questionElement);
            DOMMessage response = (DOMMessage) nro.getResponse();
            if (response == null) return null;
            Element rootElement = response.getDocument().getDocumentElement();
            return (Element) rootElement.getFirstChild();
        }
    }

    /**
     * Starts a session for asking a question using streaming. There is also a
     * simpler method for sending data using {@link #ask(Element) XML Elements}
     * that can be used when streaming is not required (that is: when the
     * messages to be transmitted are small).
     *
     * <br>
     * <br>
     *
     * <b>Example:</b>
     *
     * <PRE>
     *
     * try { XMLStreamWriter out = ask(); // Write XML here XMLStreamReader in =
     * connection.getReply(); // Read XML here connection.endTransmission(in); }
     * catch (IOException e) { logger.warning("Could not send XML."); }
     *
     * </PRE>
     *
     * @return The <code>XMLStreamWriter</code> for sending the question. The
     *         method {@link #getReply()} should be called when the message has
     *         been written and the reply is required.
     * @throws IOException if thrown by the underlying network stream.
     * @see #getReply()
     * @see #endTransmission(XMLStreamReader)
     */
    public XMLStreamWriter ask() throws IOException {
        waitForAndSetNewQuestionId();
        try {
            xmlOut = xof.createXMLStreamWriter(out);
            xmlOut.writeStartElement("question");
            xmlOut.writeAttribute("networkReplyId", Integer.toString(currentQuestionID));
            return xmlOut;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to ask question (" + currentQuestionID + ")", e);
            releaseQuestionId();
            throw new IOException(e.toString());
        }
    }

    /**
     * Release a previously obtained question id. This is absolutely necessary
     * as other questions will be blocked as long as the old id is in place.
     *
     * @see #waitForAndSetNewQuestionId()
     */
    private void releaseQuestionId() {
        synchronized (out) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(toString() + " released question id " + currentQuestionID);
            }
            currentQuestionID = -1;
            out.notifyAll();
        }
    }

    /**
     * Wait until the previous question has been released, then install a new
     * question id. The caller is then free to send.
     */
    private void waitForAndSetNewQuestionId() {
        synchronized (out) {
            while (currentQuestionID != -1) {
                try {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine(toString() + " waiting for question id...");
                    }
                    out.wait();
                } catch (InterruptedException e) {
                    logger.log(Level.WARNING, "Interrupted waiting for question id!", e);
                }
            }
            currentQuestionID = thread.getNextNetworkReplyId();
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(toString() + " installed new question id " + currentQuestionID);
            }
        }
    }

    /**
     * Starts a session for sending a message using streaming. There is also a
     * simpler method for sending data using {@link #send(Element) XML Elements}
     * that can be used when streaming is not required (that is: when the
     * messages to be transmitted are small).
     *
     * <br>
     * <br>
     *
     * <b>Example:</b>
     *
     * <PRE>
     *
     * try { XMLStreamWriter out = send(); // Write XML here
     * connection.endTransmission(in); } catch (IOException e) {
     * logger.warning("Could not send XML."); }
     *
     * </PRE>
     *
     * @return The <code>XMLStreamWriter</code> for sending the question. The
     *         method {@link #endTransmission(XMLStreamReader)} should be called
     *         when the message has been written.
     * @throws IOException if thrown by the underlying network stream.
     * @see #getReply()
     * @see #endTransmission(XMLStreamReader)
     */
    public XMLStreamWriter send() throws IOException {
        waitForAndSetNewQuestionId();
        try {
            xmlOut = xof.createXMLStreamWriter(out);
            return xmlOut;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to send message", e);
            releaseQuestionId();
            throw new IOException(e.toString());
        }
    }

    /**
     * Gets the reply being received after sending a question.
     *
     * @return An <code>XMLStreamReader</code> for reading the incoming data.
     * @throws IOException if thrown by the underlying network stream.
     * @see #ask()
     */
    public XMLStreamReader getReply() throws IOException {
        try {
            NetworkReplyObject nro = thread.waitForStreamedNetworkReply(currentQuestionID);
            xmlOut.writeEndElement();
            xmlOut.writeCharacters("\n");
            xmlOut.flush();
            xmlOut.close();
            xmlOut = null;

            XMLStreamReader in = (XMLStreamReader) nro.getResponse();
            in.nextTag();

            return in;
        } catch (Exception e) {
            logger.log(Level.WARNING, toString() + " failed to get reply (" + currentQuestionID + ")", e);
            throw new IOException(e.toString());
        }
    }

    /**
     * Ends the transmission of a message or a ask/get-reply session.
     *
     * @throws IOException if thrown by the underlying network stream.
     * @see #ask()
     * @see #send()
     */
    public void endTransmission(XMLStreamReader in) throws IOException {
        try {
            if (in != null) {
                while (in.hasNext()) {
                    in.next();
                }
                thread.unlock();
                in.close();
            } else {
                xmlOut.writeCharacters("\n");
                xmlOut.flush();
                xmlOut.close();
                xmlOut = null;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, toString() + " failed to end transmission", e);
            throw new IOException(e.toString());
        } finally {
            // Unless the question id is released, can we ever recover?
            releaseQuestionId();
        }
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
     * Handles a message using the registered <code>MessageHandler</code>.
     *
     * @param in The stream containing the message.
     */
    public void handleAndSendReply(final BufferedInputStream in) {
        try {
            in.mark(200);
            final XMLInputFactory xif = XMLInputFactory.newInstance();
            final XMLStreamReader xmlIn = xif.createXMLStreamReader(in);
            xmlIn.nextTag();

            final String networkReplyId = xmlIn.getAttributeValue(null, "networkReplyId");

            final boolean question = xmlIn.getLocalName().equals("question");
            boolean messagedConsumed = false;
            if (messageHandler instanceof StreamedMessageHandler) {
                StreamedMessageHandler smh = (StreamedMessageHandler) messageHandler;
                if (question) {
                    xmlIn.nextTag();
                }
                if (smh.accepts(xmlIn.getLocalName())) {
                    XMLStreamWriter xmlOut = null;
                    if (question) {
                        xmlOut = send();
                        xmlOut.writeStartElement("reply");
                        xmlOut.writeAttribute("networkReplyId", networkReplyId);
                    }
                    smh.handle(this, xmlIn, xmlOut);
                    if (question) {
                        xmlOut.writeEndElement();
                        endTransmission(null);
                    }
                    thread.unlock();
                    messagedConsumed = true;
                }
            }
            if (!messagedConsumed) {
                xmlIn.close();
                in.reset();
                final DOMMessage msg = new DOMMessage(in);

                final Connection connection = this;
                Thread t = new Thread(msg.getType()) {
                    @Override
                    public void run() {
                        try {
                            Element element = msg.getDocument().getDocumentElement();

                            if (question) {
                                Element reply = messageHandler.handle(connection, (Element) element.getFirstChild());

                                if (reply == null) {
                                    reply = DOMMessage.createNewRootElement("reply");
                                    reply.setAttribute("networkReplyId", networkReplyId);
                                    logger.finest("reply == null");
                                } else {
                                    Element replyHeader = reply.getOwnerDocument().createElement("reply");
                                    replyHeader.setAttribute("networkReplyId", networkReplyId);
                                    replyHeader.appendChild(reply);
                                    reply = replyHeader;
                                }

                                connection.send(reply);
                            } else {
                                Element reply = messageHandler.handle(connection, element);

                                if (reply != null) {
                                    connection.send(reply);
                                }
                            }
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "Message handler failed!", e);
                            logger.warning(msg.getDocument().getDocumentElement().toString());
                        }
                    }
                };
                t.setName(name + "MessageHandler:" + t.getName());
                t.start();
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to handle and send reply", e);
        }
    }

    /**
     * Handles a message using the registered <code>MessageHandler</code>.
     *
     * @param element The message as a DOM-parsed XML-tree.
     */
    /*
     * public void handleAndSendReply(Element element) { try { if
     * (element.getTagName().equals("question")) { String networkReplyId =
     * element.getAttribute("networkReplyId");
     *
     * Element reply = messageHandler.handle(this, (Element)
     * element.getFirstChild());
     *
     * if (reply == null) { reply = Message.createNewRootElement("reply");
     * reply.setAttribute("networkReplyId", networkReplyId); logger.info("reply ==
     * null"); } else { Element replyHeader =
     * reply.getOwnerDocument().createElement("reply");
     * replyHeader.setAttribute("networkReplyId", networkReplyId);
     * replyHeader.appendChild(reply); reply = replyHeader; }
     *
     * send(reply); } else { Element reply = messageHandler.handle(this,
     * element);
     *
     * if (reply != null) { send(reply); } } } catch (FreeColException e) {
     * StringWriter sw = new StringWriter(); e.printStackTrace(new
     * PrintWriter(sw)); logger.warning(sw.toString()); } catch (IOException e) {
     * StringWriter sw = new StringWriter(); e.printStackTrace(new
     * PrintWriter(sw)); logger.warning(sw.toString()); } }
     */

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
     * Dumping version of ask().
     * Dumps to System.err with a faked-XML prefix so the whole line can
     * be fed to an XML-pretty printer if required.
     *
     * @param request The <code>Element</code> to send.
     * @return The reply element.
     * @exception Throws IOException if ask() fails.
     */
    public Element askDumping(Element request) throws IOException {
        boolean dump = FreeCol.getDebugLevel() >= FreeCol.DEBUG_FULL_COMMS;
        if (dump) {
            try {
                System.err.println("<" + getName() + "-request>"
                    + DOMMessage.elementToString(request) + "\n");
            } catch (Exception e) {}
        }

        Element reply;
        if (dump) {
            try {
                reply = ask(request);
                try {
                    System.err.println("<" + getName() + "-reply>"
                        + ((reply == null) ? ""
                            : DOMMessage.elementToString(reply))
                        + "\n");
                } catch (Exception x) {}
            } catch (IOException e) {
                try {
                    System.err.println("<" + getName() + "-reply><exception "
                        + e.getMessage() + "\n");
                } catch (Exception x) {}
                throw e;
            }
        } else {
            reply = ask(request);
        }

        return reply;
    }

    /**
     * Override the default and return socket details.
     *
     * @return human-readable description of connection.
     */
    @Override
    public String toString() {
        return "Connection[" + getSocket() + "]";
    }
}
