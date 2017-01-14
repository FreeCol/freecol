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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.io.FreeColXMLWriter.WriteScope;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.networking.DOMMessage;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.DOMUtils;
import static net.sf.freecol.common.util.StringUtils.*;
import net.sf.freecol.common.util.Introspector;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;
    
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
 * Class for parsing raw message data into an XML-tree and for creating new
 * XML-trees.
 */
public class DOMMessage {

    protected static final Logger logger = Logger.getLogger(DOMMessage.class.getName());

    private static final String INVALID_MESSAGE = "invalid";

    /** The actual message data. */
    protected Document document;

    /**
     * Internal root constructor.
     *
     * @param tag The main tag.
     */
    private DOMMessage(String tag) {
        this.document = DOMUtils.createDocument(tag);
    }
        
    /**
     * Constructs a new DOMMessage with data from the given InputStream. The
     * constructor to use if this is an INCOMING message.
     *
     * @param inputStream The {@code InputStream} to get the XML-data from.
     * @exception IOException if thrown by the {@code InputStream}.
     * @exception SAXException if thrown during parsing.
     */
    public DOMMessage(InputStream inputStream)
        throws SAXException, IOException {
        this.document = DOMUtils.readDocument(new InputSource(inputStream));
    }

    /**
     * Create a DOMMessage with given tag and attributes.
     *
     * @param tag The main tag.
     * @param attributes Attribute,value pairs.
     */
    public DOMMessage(String tag, String... attributes) {
        this(tag);
        
        String[] all = attributes;
        for (int i = 0; i < all.length; i += 2) {
            if (all[i+1] != null) this.setAttribute(all[i], all[i+1]);
        }
    }
    
    /**
     * Create a DOMMessage with given tag and attributes.
     *
     * @param tag The main tag.
     * @param map A {@code NamedNodeMap} of attributes.
     */
    private DOMMessage(String tag, NamedNodeMap map) {
        this(tag);

        final int n = map.getLength();
        for (int i = 0; i < n; i++) {
            Node node = map.item(i);
            this.setAttribute(node.getNodeName(), node.getNodeValue());
        }
    }

    /**
     * Gets the root element of the document.
     *
     * @return The root element.
     */
    private Element getElement() {
        return this.document.getDocumentElement();
    }

    /**
     * Gets the type of this DOMMessage.
     *
     * @return The type of this DOMMessage.
     */
    public String getType() {
        return (this.document != null && getElement() != null)
            ? DOMUtils.getType(getElement())
            : INVALID_MESSAGE;
    }

    /**
     * Checks if this message is of a given type.
     *
     * @param type The type you wish to test against.
     * @return {@code true} if the type of this message equals the given
     *         type and {@code false} otherwise.
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
     * Set a list of attributes as an array.
     *
     * @param attributes The list of attribute strings.
     * @return This message.
     */
    public DOMMessage setArrayAttributes(List<String> attributes) {
        if (!attributes.isEmpty()) {
            int i = 0;
            for (String a : attributes) {
                setAttribute(FreeColObject.arrayKey(i), a);
                i++;
            }
        }
        return this;
    }                

    /**
     * Set a array of attributes.
     *
     * @param attributes The array of attribute strings.
     * @return This message.
     */
    public DOMMessage setArrayAttributes(String[] attributes) {
        if (attributes != null) {
            for (int i = 0; i < attributes.length; i++) {
                setAttribute(FreeColObject.arrayKey(i), attributes[i]);
            }
        }
        return this;
    }                
    
    /**
     * Set all the attributes in a map.
     *
     * @param attributes The map of attribute strings.
     * @return This message.
     */
    public DOMMessage setAttributes(Map<String, String> attributes) {
        forEachMapEntry(attributes,
                        e -> setAttribute(e.getKey(), e.getValue()));
        return this;
    }

    /**
     * Checks if an attribute is set on the root element.
     *
     * @param attribute The attribute in which to verify the existence of.
     * @return {@code true} if the root element has the given attribute.
     */
    public boolean hasAttribute(String attribute) {
        return getElement().hasAttribute(attribute);
    }

    public DOMMessage add(Element e) {
        if (e != null) getElement().appendChild(this.document.importNode(e, true));
        return this;
    }
    public DOMMessage add(FreeColObject fco) {
        if (fco != null) add(DOMUtils.toXMLElement(fco, this.document, (Player)null));
        return this;
    }
    public DOMMessage add(FreeColObject fco, Player player) {
        if (fco != null) add(DOMUtils.toXMLElement(fco, this.document, player));
        return this;
    }
    public DOMMessage add(List<? extends FreeColObject> fcos) {
        if (fcos != null) for (FreeColObject fco : fcos) add(fco);
        return this;
    }
    public DOMMessage add(DOMMessage msg) {
        if (msg != null) add(msg.toXMLElement());
        return this;
    }
    public DOMMessage addMessages(List<DOMMessage> msgs) {
        if (msgs != null) for (DOMMessage msg : msgs) add(msg);
        return this;
    }

    public void clearChildren() {
        Element element = getElement();
        NodeList nl = element.getChildNodes();
        for (int i = nl.getLength() - 1; i >= 0; i--) {
            element.removeChild(nl.item(i));
        }
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

    /**
     * Attach a copy of this message to a document.
     *
     * @param doc The {@code Document} to attach to.
     * @return The attached {@code Element}.
     */
    public Element attachToDocument(Document doc) {
        return (Element)doc.adoptNode(this.toXMLElement());
    }
    
    /**
     * Server-side handler for this message.
     *
     * @param freeColServer The {@code FreeColServer} handling the request.
     * @param serverPlayer The {@code ServerPlayer} that sent the request.
     * @return A {@code ChangeSet} defining the response.
     */
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        return serverPlayer.clientError("Invalid message type: " + getType());
    }


    // Useful utilities for subclass *class*(Game,Element) constructors
    // Temporarily duplicated from DOMUtils
    
    /**
     * Get a boolean attribute value from an element.
     *
     * @param element The {@code Element} to query.
     * @param tag The attribute name.
     * @param defaultValue A default value to return on failure.
     * @return The boolean value found, or the default value on error.
     */
    public static boolean getBooleanAttribute(Element element, String tag,
                                              boolean defaultValue) {
        return DOMUtils.getBooleanAttribute(element, tag, defaultValue);
    }

    /**
     * Get an integer attribute value from an element.
     *
     * @param element The {@code Element} to query.
     * @param tag The attribute name.
     * @param defaultValue A default value to return on failure.
     * @return The integer value found, or the default value on error.
     */
    public static int getIntegerAttribute(Element element, String tag,
                                          int defaultValue) {
        return DOMUtils.getIntegerAttribute(element, tag, defaultValue);
    }

    /**
     * Get a string attribute value from an element.
     *
     * @param element The {@code Element} to query.
     * @param tag The attribute name.
     * @return The string value found, or the default value on error.
     */
    public static String getStringAttribute(Element element, String tag) {
        return DOMUtils.getStringAttribute(element, tag);
    }

    /**
     * Get an enum attribute value from an element.
     *
     * @param <T> The actual enum return type.
     * @param element The {@code Element} to query.
     * @param tag The attribute name.
     * @param returnClass The class of the return value.
     * @param defaultValue A default value to return on failure.
     * @return The string value found, or the default value on error.
     */
    public static <T extends Enum<T>> T getEnumAttribute(Element element,
        String tag, Class<T> returnClass, T defaultValue) {
        return DOMUtils.getEnumAttribute(element, tag, returnClass, defaultValue);
    }

    /**
     * Convenience method to extract a child FreeColGameObject from an element.
     *
     * @param <T> The actual return type.
     * @param game The {@code Game} to instantiate within.
     * @param element The parent {@code Element}.
     * @param index The index of the child element.
     * @param intern Whether to intern the object found.
     * @param returnClass The expected class of the child.
     * @return A new instance of the return class, or null on error.
     */
    public static <T extends FreeColGameObject> T getChild(Game game,
            Element element, int index, boolean intern, Class<T> returnClass) {
        return DOMUtils.getChild(game, element, index, intern, returnClass);
    }

    /**
     * Convenience method to extract a child FreeColObject from an element.
     *
     * @param <T> The actual return type.
     * @param game The {@code Game} to instantiate within.
     * @param element The parent {@code Element}.
     * @param index The index of the child element.
     * @param returnClass The expected class of the child.
     * @return A new instance of the return class, or null on error.
     */
    public static <T extends FreeColObject> T getChild(Game game,
        Element element, int index, Class<T> returnClass) {
        return DOMUtils.getChild(game, element, index, returnClass);
    }
    
    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getElement().toString();
    }
}
