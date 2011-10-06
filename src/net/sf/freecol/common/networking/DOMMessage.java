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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.freecol.FreeCol;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Class for parsing raw message data into an XML-tree and for creating new
 * XML-trees.
 */
public class DOMMessage {

    protected static final Logger logger = Logger.getLogger(DOMMessage.class.getName());

    private static final String FREECOL_PROTOCOL_VERSION = "0.1.6";

    private static final String INVALID_MESSAGE = "invalid";

    /** The actual Message data. */
    protected Document document;


    protected DOMMessage() {
        // empty constructor
    }

    /**
     * Constructs a new DOMMessage with data from the given
     * String.  The constructor to use if this is an INCOMING message.
     *
     * @param msg The raw message data.
     * @exception IOException should not be thrown.
     * @exception SAXException if thrown during parsing.
     */
    public DOMMessage(String msg) throws SAXException, IOException {
        this(new InputSource(new StringReader(msg)));
    }

    /**
     * Constructs a new DOMMessage with data from the given InputStream. The
     * constructor to use if this is an INCOMING message.
     *
     * @param inputStream The <code>InputStream</code> to get the XML-data
     *            from.
     * @exception IOException if thrown by the <code>InputStream</code>.
     * @exception SAXException if thrown during parsing.
     */
    public DOMMessage(InputStream inputStream)
        throws SAXException, IOException {
        this(new InputSource(inputStream));
    }

    /**
     * Constructs a new DOMMessage with data from the given InputSource. The
     * constructor to use if this is an INCOMING message.
     *
     * @param inputSource The <code>InputSource</code> to get the XML-data
     *            from.
     * @exception IOException if thrown by the <code>InputSource</code>.
     * @exception SAXException if thrown during parsing.
     */
    private DOMMessage(InputSource inputSource)
        throws SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document tempDocument = null;
        boolean dumpMsgOnError = true; // FreeCol.isInDebugMode();
        if (dumpMsgOnError) {
            /*
             * inputSource.setByteStream( new
             * ReplayableInputStream(inputSource.getByteStream()) );
             *
             */
            inputSource.setByteStream(new BufferedInputStream(inputSource.getByteStream()));

            inputSource.getByteStream().mark(1000000);
        }

        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            tempDocument = builder.parse(inputSource);
        } catch (ParserConfigurationException pce) {
            // Parser with specified options can't be built
            logger.log(Level.WARNING, "Parser error", pce);
        } catch (SAXException se) {
            throw se;
        } catch (IOException ie) {
            throw ie;
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
                logger.severe(baos.toString());
            }
            throw e;
        }

        document = tempDocument;
    }

    /**
     * Constructs a new DOMMessage with data from the given XML-document.
     *
     * @param document The document representing an XML-message.
     */
    public DOMMessage(Document document) {
        this.document = document;
    }

    /**
     * Gets the current version of the FreeCol protocol.
     *
     * @return The version of the FreeCol protocol.
     */
    public static String getFreeColProtocolVersion() {
        return FREECOL_PROTOCOL_VERSION;
    }

    /**
     * Creates and returns a new XML-document.
     *
     * @return the new XML-document.
     */
    public static Document createNewDocument() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            return builder.newDocument();
        } catch (ParserConfigurationException pce) {
            // Parser with specified options can't be built
            pce.printStackTrace();
            return null;
        }
    }

    /**
     * Creates a new root element. This is done by creating a new document and
     * using this document to create the root element.
     *
     * @param tagName The tag name of the root element beeing created,
     * @return the new root element.
     */
    public static Element createNewRootElement(String tagName) {
        return createNewDocument().createElement(tagName);
    }

    /**
     * Collapses a list of elements into a "multiple" element
     * with the original elements added as child nodes.
     *
     * @param elements A list of <code>Element</code>s to collapse.
     * @return A new "multiple" element, or the singleton element of the list,
     *     or null if the list is empty.
     */
    public static Element collapseElements(List<Element> elements) {
        switch (elements.size()) {
        case 0:
            return null;
        case 1:
            return elements.get(0);
        default:
            break;
        }
        Element first = elements.remove(0);
        Document doc = first.getOwnerDocument();
        Element result = doc.createElement("multiple");
        result.appendChild(first);
        for (Element e : elements) {
            result.appendChild(doc.importNode(e, true));
        }
        return result;
    }

    /**
     * Creates an error message.
     *
     * @param messageID Identifies the "i18n"-keyname. Not specified in the
     *            message if <i>null</i>.
     * @param message The error in plain text. Not specified in the message if
     *            <i>null</i>.
     * @return The root <code>Element</code> of the error message.
     */
    public static Element createError(String messageID, String message) {
        Element errorElement = createNewRootElement("error");

        if (messageID != null && !messageID.equals("")) {
            errorElement.setAttribute("messageID", messageID);
        }

        if (message != null && !message.equals("")) {
            errorElement.setAttribute("message", message);
        }

        return errorElement;
    }

    /**
     * Creates an error message.
     *
     * @param out The output stream for the message.
     * @param messageID Identifies the "i18n"-keyname. Not specified in the
     *            message if <i>null</i>.
     * @param message The error in plain text. Not specified in the message if
     *            <i>null</i>.
     */
    public static void createError(XMLStreamWriter out, String messageID, String message) {
        try {
            out.writeStartElement("error");

            if (messageID != null && !messageID.equals("")) {
                out.writeAttribute("messageID", messageID);
            }

            if (message != null && !message.equals("")) {
                out.writeAttribute("message", message);
            }
            out.writeEndElement();
        } catch (XMLStreamException e) {
            logger.warning("Could not send error message.");
        }
    }

    /**
     * Creates an error message in response to bad client data.
     *
     * @param message The error in plain text.
     * @return The root <code>Element</code> of the error message.
     */
    public static Element clientError(String message) {
        if (FreeCol.isInDebugMode()) {
            try {
                throw new IllegalStateException(message);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Client error", e);
            }
        }
        Element errorElement = createNewRootElement("error");
        errorElement.setAttribute("messageID", "server.reject");
        errorElement.setAttribute("message", message);
        return errorElement;
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
     * Gets the type of this DOMMessage.
     *
     * @return The type of this DOMMessage.
     */
    public String getType() {

        if (document != null && document.getDocumentElement() != null) {

            return document.getDocumentElement().getTagName();
        }

        return INVALID_MESSAGE;
    }

    /**
     * Checks if this message is of a given type.
     *
     * @param type The type you wish to test against.
     * @return <code>true</code> if the type of this message equals the given
     *         type and <code>false</code> otherwise.
     */
    public boolean isType(String type) {

        return getType().equals(type);
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
        document.getDocumentElement().setAttribute(key, (new Integer(value)).toString());
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
     * Inserts <code>newRoot</code> as the new root element and appends the
     * old root element.
     *
     * @param newRoot The new root element.
     */
    public void insertAsRoot(Element newRoot) {
        Element oldRoot = document.getDocumentElement();

        if (oldRoot != null) {
            document.removeChild(oldRoot);
            newRoot.appendChild(oldRoot);
        }

        document.appendChild(newRoot);
    }

    /**
     * Convenience method: returns the first child element with the specified
     * tagname.
     *
     * @param element The <code>Element</code> to search for the child
     *            element.
     * @param tagName The tag name of the child element to be found.
     * @return The first child element with the given name.
     */
    public static Element getChildElement(Element element, String tagName) {
        NodeList n = element.getChildNodes();
        for (int i = 0; i < n.getLength(); i++) {
            if (n.item(i) instanceof Element && ((Element) n.item(i)).getTagName().equals(tagName)) {
                return (Element) n.item(i);
            }
        }

        return null;
    }

    /**
     * Convert an element to a string.
     *
     * @param element The <code>Element</code> to convert.
     * @return A string representation of an element.
     */
    public static String elementToString(Element element) {
        XMLInputFactory xif = XMLInputFactory.newInstance();
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer xt = factory.newTransformer();
            StringWriter sw = new StringWriter();
            xt.transform(new DOMSource(element), new StreamResult(sw));
            return sw.toString();
        } catch (TransformerException e) {
            logger.log(Level.WARNING, "TransformerException", e);
        }
        return null;
    }


    public Element toXMLElement() {
        // do nothing
        return null;
    }

    /**
     * Returns the <code>String</code> representation of the message. This is
     * what actually gets transmitted to the other peer.
     *
     * @return The <code>String</code> representation of the message.
     */
    @Override
    public String toString() {
        return document.getDocumentElement().toString();
    }
}
