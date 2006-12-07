
package net.sf.freecol.common.networking;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
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

import org.w3c.dom.Element;





/**
* A network connection. Responsible for both sending and receiving
* network messages.
*
* @see #send(Element)
* @see #sendAndWait(Element)
* @see #ask(Element)
*/
public class Connection {
    private static final Logger logger = Logger.getLogger(Connection.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private final OutputStream out;
    private final InputStream in;
    private final Socket socket;
    private final Transformer xmlTransformer;
    private final ReceivingThread thread;
    private final XMLOutputFactory xof = XMLOutputFactory.newInstance(); 
    private MessageHandler messageHandler;
    
    private XMLStreamWriter xmlOut = null;
    private int currentQuestionID = -1;

    /**
    * Dead constructor, for DummyConnection purposes.
    */
    protected Connection() {
        out = null;
        in = null;
        socket = null;
        thread = null;
        xmlTransformer = null;
    }

    /**
    * Sets up a new socket with specified host and port and uses {@link #Connection(Socket, MessageHandler)}.
    *
    * @param host The host to connect to.
    * @param port The port to connect to.
    * @param messageHandler The MessageHandler to call for each message received.
    * @throws IOException
    */
    public Connection(String host, int port, MessageHandler messageHandler) throws IOException {
        this(new Socket(host, port), messageHandler);
    }


    /**
    * Creates a new <code>Connection</code> with the specified <code>Socket</code>
    * and {@link MessageHandler}.
    *
    * @param socket The socket to the client.
    * @param messageHandler The MessageHandler to call for each message received.
    * @throws IOException
    */
    public Connection(Socket socket, MessageHandler messageHandler) throws IOException {
        this.messageHandler = messageHandler;
        this.socket = socket;

        out = socket.getOutputStream();
        in = socket.getInputStream();
        
        Transformer myTransformer;
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            myTransformer = factory.newTransformer();
            myTransformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        } catch (TransformerException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.warning(sw.toString());

            myTransformer = null;
        }
        xmlTransformer = myTransformer;

        thread = new ReceivingThread(this, in);
        thread.start();
    }



    /**
    * Sends a "disconnect"-message and closes this connection.
    *
    * @throws IOException
    */
    public void close() throws IOException {
        Element disconnectElement = Message.createNewRootElement("disconnect");
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
    *                holds all the information
    * @throws IOException If an error occur while sending the message.
    * @see #sendAndWait(Element)
    * @see #ask(Element)
    */
    public void send(Element element) throws IOException {      
        synchronized (out) {
            while (currentQuestionID != -1) {
                try {
                    out.wait();
                } catch (InterruptedException e) {}
            }

            try {
                xmlTransformer.transform(new DOMSource(element), new StreamResult(out));
            } catch (TransformerException e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                logger.warning(sw.toString());
            }

            out.write('\n');
            out.flush();           
        }        
    }


    /**
    * On IBM's JDK version 1.4.2 the Element.toString method doesn't give you
    * a normal XML string, which is fine because the Sun API docs never said that
    * it should. On Sun's JDK version 1.5 this is also the case. So now instead
    * of calling Element.toString you should use this method.
    *
    * @param element The Element to convert.
    * @return The string representation of the given Element without the xml version tag.
    */
    String convertElementToString(Element element) {
        synchronized (out) { // Also a lock for: xmlTransformer
            String xml;
            try {
                StringWriter stringWriter = new StringWriter();
                xmlTransformer.transform(new DOMSource(element), new StreamResult(stringWriter));
                xml = stringWriter.toString();
            } catch (TransformerException e) {
                xml = e.getMessage();
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                logger.warning(sw.toString());
            }
            return xml;
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
            logger.warning("Attempt to 'wait()' the ReceivingThread.");
            throw new IOException("Attempt to 'wait()' the ReceivingThread.");
        } else {
            NetworkReplyObject nro = thread.waitForNetworkReply(networkReplyId);
            send(questionElement);
            return (Element) ((Message) nro.getResponse()).getDocument().getDocumentElement().getFirstChild();
        }
    }
    
    /**
     * Starts a session for asking a question using
     * streaming. There is also a simpler method for
     * sending data using 
     * {@link #ask(Element) XML Elements} that can be
     * used when streaming is not required (that is:
     * when the messages to be transmitted are small).
     *
     * <br><br>
     * 
     * <b>Example:</b>
     * <PRE>
     * try {
     *     XMLStreamWriter out = ask();
     *     // Write XML here
     *     XMLStreamReader in = connection.getReply();
     *     // Read XML here
     *     connection.endTransmission(in);     
     * } catch (IOException e) {
     *     logger.warning("Could not send XML.");
     * }
     * </PRE>
     *    
     * @return The <code>XMLStreamWriter</code> for sending
     *      the question. The method {@link #getReply()}
     *      should be called when the message has been
     *      written and the reply is required.
     * @throws IOException if thrown by the underlying
     *      network stream.
     * @see #getReply()
     * @see #endTransmission(XMLStreamReader)
     */
    public XMLStreamWriter ask() throws IOException {
        synchronized (out) {
            while (currentQuestionID != -1) {
                try {
                    out.wait();
                } catch (InterruptedException e) {}
            }
            currentQuestionID = thread.getNextNetworkReplyId();
        }
        try {
            xmlOut = xof.createXMLStreamWriter(out);
        } catch (XMLStreamException e) {
            throw new IOException(e.toString());
        }
        try {
            xmlOut.writeStartElement("question");
            xmlOut.writeAttribute("networkReplyId", Integer.toString(currentQuestionID));
            return xmlOut;
        } catch (XMLStreamException e) {
            currentQuestionID = -1;
            throw new IOException(e.toString());
        }
    }
    
    /**
     * Starts a session for sending a message using
     * streaming. There is also a simpler method for
     * sending data using 
     * {@link #send(Element) XML Elements} that can be
     * used when streaming is not required (that is:
     * when the messages to be transmitted are small).
     *
     * <br><br>
     * 
     * <b>Example:</b>
     * <PRE>
     * try {
     *     XMLStreamWriter out = send();
     *     // Write XML here
     *     connection.endTransmission(in);     
     * } catch (IOException e) {
     *     logger.warning("Could not send XML.");
     * }
     * </PRE>
     *    
     * @return The <code>XMLStreamWriter</code> for sending
     *      the question. The method 
     *      {@link #endTransmission(XMLStreamReader)}
     *      should be called when the message has been
     *      written.
     * @throws IOException if thrown by the underlying
     *      network stream.
     * @see #getReply()
     * @see #endTransmission(XMLStreamReader)
     */
    public XMLStreamWriter send() throws IOException {
        synchronized (out) {
            while (currentQuestionID != -1) {
                try {
                    out.wait();
                } catch (InterruptedException e) {}
            }
            currentQuestionID = thread.getNextNetworkReplyId();
        }
        try {       
            xmlOut = xof.createXMLStreamWriter(out);
        } catch (XMLStreamException e) {
            throw new IOException(e.toString());
        }
        return xmlOut;
    }
    
    /**
     * Gets the reply being received after sending a question.
     * @return An <code>XMLStreamReader</code> for reading the
     *      incoming data.
     * @throws IOException if thrown by the underlying
     *      network stream.
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
        } catch (XMLStreamException e) {
            throw new IOException(e.toString());
        }
    }   
    
    /**
     * Ends the transmission of a message or a ask/get-reply
     * session.
     *  
     * @return An <code>XMLStreamReader</code> for reading the
     *      incoming data.
     * @throws IOException if thrown by the underlying
     *      network stream.
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
            
            synchronized (out) {
                currentQuestionID = -1;
                out.notifyAll();
            }
        } catch (XMLStreamException e) {
            IOException ie = new IOException(e.toString());
            ie.initCause(e);
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.warning(sw.toString());
            throw ie;
        }
    }

    /**
    * Sends the given message over this <code>Connection</code>
    * and waits for confirmation of receiveval before returning.
    *
    * @param element The element (root element in a DOM-parsed XML tree) that
    *                holds all the information
    * @throws IOException If an error occur while sending the message.
    * @see #send(Element)
    * @see #ask(Element)
    */
    public void sendAndWait(Element element) throws IOException {
        Element reply = ask(element);

        /*if (reply != null) {
            handleAndSendReply(reply);
        }*/
    }


    /**
    * Sets the MessageHandler for this Connection.
    * @param mh The new MessageHandler for this Connection.
    */
    public void setMessageHandler(MessageHandler mh) {
        messageHandler = mh;
    }

    /**
    * Gets the MessageHandler for this Connection.
    * @return The MessageHandler for this Connection.
    */
    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    /*public void handleAndSendReply(Message message) {
        handleAndSendReply(message.getDocument().getDocumentElement());
    }*/

    /**
     * Handles a message using the registered <code>MessageHandler</code>.
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
                final Message msg = new Message(in);
                
                final Connection connection = this;
                Thread t = new Thread() {
                    public void run() {
                        try {                      
                            Element element = msg.getDocument().getDocumentElement();
                            
                            if (question) {
                                Element reply = messageHandler.handle(connection, (Element) element.getFirstChild());
                                
                                if (reply == null) {
                                    reply = Message.createNewRootElement("reply");
                                    reply.setAttribute("networkReplyId", networkReplyId);
                                    logger.info("reply == null");
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
                            StringWriter sw = new StringWriter();
                            e.printStackTrace(new PrintWriter(sw));
                            logger.warning(sw.toString());
                        }
                    }
                };
                t.setName("MessageHandler:" + t.getName());
                t.start();
            }
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.warning(sw.toString());
        }
    }

    /**
    * Handles a message using the registered <code>MessageHandler</code>.
    * @param element The message as a DOM-parsed XML-tree.
    */
    /*public void handleAndSendReply(Element element) {
        try {
            if (element.getTagName().equals("question")) {
                String networkReplyId = element.getAttribute("networkReplyId");

                Element reply = messageHandler.handle(this, (Element) element.getFirstChild());

                if (reply == null) {
                    reply = Message.createNewRootElement("reply");
                    reply.setAttribute("networkReplyId", networkReplyId);
                    logger.info("reply == null");
                } else {
                    Element replyHeader = reply.getOwnerDocument().createElement("reply");
                    replyHeader.setAttribute("networkReplyId", networkReplyId);
                    replyHeader.appendChild(reply);
                    reply = replyHeader;
                }

                send(reply);
            } else {
                Element reply = messageHandler.handle(this, element);

                if (reply != null) {
                    send(reply);
                }
            }
        } catch (FreeColException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.warning(sw.toString());
        } catch (IOException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.warning(sw.toString());
        }
    }*/


    /**
    * Gets the socket.
    * @return The <code>Socket</code> used while communicating with the other peer.
    */
    public Socket getSocket() {
        return socket;
    }    
}
