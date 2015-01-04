/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.sf.freecol.common.debug.FreeColDebugger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
 * Class for parsing raw message data into an XML-tree and for creating new
 * XML-trees.
 */
public class Message {

    protected static final Logger logger = Logger.getLogger(Message.class.getName());

    private static final String FREECOL_PROTOCOL_VERSION = "0.1.6";

    private static final String INVALID_MESSAGE = "invalid";

    /** The actual Message data. */
    protected Document document;


    protected Message() {
        // empty constructor
    }

    /**
     * Constructs a new Message with data from the given String. The
     * constructor to use if this is an INCOMING message.
     * 
     * @param msg The raw message data.
     * @exception IOException should not be thrown.
     * @exception SAXException if thrown during parsing.
     */
    public Message(String msg) throws SAXException, IOException {
        this(new InputSource(new StringReader(msg)));
    }

    /**
     * Constructs a new Message with data from the given InputStream. The
     * constructor to use if this is an INCOMING message.
     * 
     * @param inputStream The <code>InputStream</code> to get the XML-data
     *            from.
     * @exception IOException if thrown by the <code>InputStream</code>.
     * @exception SAXException if thrown during parsing.
     */
    public Message(InputStream inputStream) throws SAXException, IOException {
        this(new InputSource(inputStream));
    }

    /**
     * Constructs a new Message with data from the given InputSource. The
     * constructor to use if this is an INCOMING message.
     * 
     * @param inputSource The <code>InputSource</code> to get the XML-data
     *            from.
     * @exception IOException if thrown by the <code>InputSource</code>.
     * @exception SAXException if thrown during parsing.
     */
    private Message(InputSource inputSource) throws SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document tempDocument = null;
        boolean dumpMsgOnError
            = FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.COMMS);
        if (dumpMsgOnError) {
            inputSource.setByteStream(new BufferedInputStream(inputSource.getByteStream()));

            inputSource.getByteStream().mark(1000000);
        }

        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            tempDocument = builder.parse(inputSource);
        } catch (ParserConfigurationException pce) {
            // Parser with specified options can't be built
            logger.log(Level.WARNING, "Parser error", pce);
        } catch (IOException|SAXException ex) {
            throw ex;
        } catch (ArrayIndexOutOfBoundsException e) {
            // Xerces throws ArrayIndexOutOfBoundsException when it barfs on
            // some FreeCol messages. I'd like to see the messages upon which
            // it barfs
            if (dumpMsgOnError) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                inputSource.getByteStream().reset();
                while (true) {
                    int i = inputSource.getByteStream().read();
                    if (-1 == i) {
                        break;
                    }
                    baos.write(i);
                }
                logger.severe(baos.toString("UTF-8"));
            }
            throw e;
        }
        document = tempDocument;
    }

    /**
     * Constructs a new Message with data from the given XML-document.
     * 
     * @param document The document representing an XML-message.
     */
    public Message(Document document) {
        this.document = document;
    }


    /**
     * Gets the <code>Document</code> holding the message data.
     * 
     * @return The <code>Document</code> holding the message data.
     */
    public Document getDocument() {
        return document;
    }

    /**
     * Gets the type of this Message.
     * 
     * @return The type of this Message.
     */
    public String getType() {
        return (document != null && document.getDocumentElement() != null)
            ? document.getDocumentElement().getTagName()
            : INVALID_MESSAGE;
    }

    /**
     * Checks if this message is of a given type.
     * 
     * @param type The type you wish to test against.
     * @return True if the type of this message equals the given type.
     */
    public boolean isType(String type) {
        return getType().equals(type);
    }

    /**
     * Gets an attribute from the root element.
     * 
     * @param key The key of the attribute.
     * @return The value of the attribute with the given key.
     */
    public String getAttribute(String key) {
        return document.getDocumentElement().getAttribute(key);
    }

    /**
     * Checks if an attribute is set on the root element.
     * 
     * @param attribute The attribute in which to verify the existence of.
     * @return <code>true</code> if the root element has the given attribute.
     */
    public boolean hasAttribute(String attribute) {
        return document.getDocumentElement().hasAttribute(attribute);
    }

    /**
     * Sets an attribute on the root element.
     * 
     * @param key The key of the attribute.
     * @param value The value of the attribute.
     */
    public void setAttribute(String key, String value) {
        document.getDocumentElement().setAttribute(key, value);
    }

    /**
     * Sets an attribute on the root element.
     * 
     * @param key The key of the attribute.
     * @param value The value of the attribute.
     */
    public void setAttribute(String key, int value) {
        setAttribute(key, Integer.toString(value));
    }

    /**
     * Dummy serialization stub.
     * Must be overridden by subclasses.
     *
     * @return Null.
     */
    public Element toXMLElement() {
        return null; // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return document.getDocumentElement().toString();
    }

    /**
     * Gets the current version of the FreeCol protocol.
     * 
     * @return The version of the FreeCol protocol.
     */
    public static String getFreeColProtocolVersion() {
        return FREECOL_PROTOCOL_VERSION;
    }
}
