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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.util.Introspector;

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

    /** The actual message data. */
    protected Document document;


    /**
     * Constructs a new DOMMessage with data from the given XML-document.
     *
     * @param document The document representing an XML-message.
     */
    public DOMMessage(Document document) {
        this.document = document;
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
        boolean dumpMsgOnError = true;
        if (dumpMsgOnError) {
            inputSource.setByteStream(new BufferedInputStream(inputSource.getByteStream()));

            inputSource.getByteStream().mark(1000000);
        }
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            tempDocument = builder.parse(inputSource);
        } catch (ParserConfigurationException pce) {
            logger.log(Level.WARNING, "Parser configuration", pce);
        } catch (IOException ex) {
            //} catch (IOException|SAXException ex) {
            throw ex;
        } catch (Exception ex) {
            // Xerces throws ArrayIndexOutOfBoundsException when it barfs on
            // some FreeCol messages. I'd like to see the messages upon which
            // it barfs.
            // Its also throwing SAXParseException in BR#2925
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
                logger.log(Level.SEVERE, baos.toString("UTF-8"), ex);
            } else {
                logger.log(Level.WARNING, "Parse error", ex);
            }
            throw ex;
        }
        this.document = tempDocument;
    }

    /**
     * Create a DOMMessage with given tag and attributes.
     *
     * @param tag The main tag.
     * @param attributes Attribute,value pairs.
     */
    public DOMMessage(String tag, String... attributes) {
        this.document = createNewDocument();
        Element root = this.document.createElement(tag);
        this.document.appendChild(root);
        String[] all = attributes;
        for (int i = 0; i < all.length; i += 2) {
            root.setAttribute(all[i], all[i+1]);
        }
    }


    /**
     * Create a DOMMessage from an element.
     *
     * FIXME: make this into a constructor?
     *
     * @param game The <code>Game</code> to create the message in.
     * @param element The <code>Element</code> to create the message from.
     * @return The message created, or null on failure.
     */
    public static DOMMessage createMessage(Game game, Element element) {
        if (element == null) return null;
        String tag = element.getTagName();
        tag = "net.sf.freecol.common.networking."
            + tag.substring(0, 1).toUpperCase() + tag.substring(1)
            + "Message";
        Class[] types = { Game.class, Element.class };
        Object[] params = { game, element };
        DOMMessage message;
        try {
            message = (DOMMessage)Introspector.instantiate(tag, types, params);
        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "Instantiation fail", e);
            message = null;
        }
        return message;
    }

    /**
     * Gets the <code>Document</code> holding the message data.
     *
     * @return The <code>Document</code> holding the message data.
     */
    public Document getDocument() {
        return document;
    }

    private Element getElement() {
        return this.document.getDocumentElement();
    }
    
    /**
     * Gets the type of this DOMMessage.
     *
     * @return The type of this DOMMessage.
     */
    public String getType() {
        return (document != null && getElement() != null)
            ? getElement().getTagName()
            : INVALID_MESSAGE;
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
     * Gets an attribute from the root element.
     *
     * @param key The key of the attribute.
     * @return The value of the attribute with the given key.
     */
    public String getAttribute(String key) {
        return getElement().getAttribute(key);
    }

    /**
     * Sets an attribute on the root element.
     *
     * @param key The key of the attribute.
     * @param value The value of the attribute.
     */
    public void setAttribute(String key, String value) {
        getElement().setAttribute(key, value);
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
     * Checks if an attribute is set on the root element.
     *
     * @param attribute The attribute in which to verify the existence of.
     * @return <code>true</code> if the root element has the given attribute.
     */
    public boolean hasAttribute(String attribute) {
        return getElement().hasAttribute(attribute);
    }

    public void add(FreeColObject fco) {
        getElement().appendChild(fco.toXMLElement(document));
    }
    public void add(FreeColObject fco, Player player) {
        getElement().appendChild(fco.toXMLElement(document, player));
    }
    public void add(DOMMessage msg) {
        getElement().appendChild(msg.toXMLElement());
    }
    public void add(String tag, String... attributes) {
        Element e = document.createElement(tag);
        getElement().appendChild(e);
        String[] all = attributes;
        for (int i = 0; i < all.length; i += 2) {
            e.setAttribute(all[i], all[i+1]);
        }
    }        
        
    /**
     * Inserts an element as a new root element to the existing
     * element of this message.
     *
     * @param newRoot The new root <code>Element</code>.
     */
    public void insertAsRoot(Element newRoot) {
        Element oldRoot = getElement();
        if (oldRoot != null) {
            document.removeChild(oldRoot);
            newRoot.appendChild(oldRoot);
        }

        document.appendChild(newRoot);
    }

    /**
     * Dummy serialization stub.
     * Must be overridden by subclasses.
     *
     * @return The document element.
     */
    public Element toXMLElement() {
        return getElement();
    }


    // Collection of static methods.
    // Much of the Element manipulation needs to go away.

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
            DocumentBuilderFactory factory
                = DocumentBuilderFactory.newInstance();
            return factory.newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException pce) {
            // Parser with specified options can't be built
            logger.log(Level.WARNING, "Parser failure", pce);
        }
        return null;
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
     * Creates an error message in response to bad client data.
     *
     * @param message The error in plain text.
     * @return The root <code>Element</code> of the error message.
     */
    public static Element clientError(String message) {
        logger.warning(message);
        if (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.COMMS)) {
            Thread.dumpStack();
        }
        return new ErrorMessage("server.reject", message).toXMLElement();
    }

    /**
     * Convenience method to find the first child element with the
     * specified tagname.
     *
     * @param element The <code>Element</code> to search for the child
     *     element in.
     * @param tagName The tag name of the child element to be found.
     * @return The first child element with the given name.
     */
    public static Element getChildElement(Element element, String tagName) {
        NodeList n = element.getChildNodes();
        for (int i = 0; i < n.getLength(); i++) {
            if (n.item(i) instanceof Element
                && ((Element)n.item(i)).getTagName().equals(tagName)) {
                return (Element)n.item(i);
            }
        }
        return null;
    }

    /**
     * Convert an element to a string.
     *
     * @param element The <code>Element</code> to convert.
     * @return The <code>String</code> representation of an element.
     */
    public static String elementToString(Element element) {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer xt = factory.newTransformer();
            StringWriter sw = new StringWriter();
            xt.transform(new DOMSource(element), new StreamResult(sw));
            String result = sw.toString();

            // Drop the <?xml...?> part if present to keep logging concise.
            if (result.startsWith("<?xml")) {
                final String xmlEnd = "?>";
                int index = result.indexOf(xmlEnd);
                if (index > 0) {
                    result = result.substring(index + xmlEnd.length());
                }
            }
            return result;
        } catch (TransformerException e) {
            logger.log(Level.WARNING, "TransformerException", e);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return document.getDocumentElement().toString();
    }
}
