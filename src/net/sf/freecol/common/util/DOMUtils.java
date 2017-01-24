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

package net.sf.freecol.common.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.io.FreeColXMLWriter.WriteScope;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.networking.AttributeMessage;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.networking.MessageHandler;
import net.sf.freecol.common.networking.MultipleMessage;
import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.StringUtils.*;
import net.sf.freecol.common.util.Introspector;
    
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
 * DOM-specific utilities.  Corral them here in preparation for getting
 * rid of them entirely.
 */
public class DOMUtils {

    protected static final Logger logger = Logger.getLogger(DOMUtils.class.getName());
    
    private static DocumentBuilder builder = null, parser = null;
    static {
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException pce) {
            // Never seen this in practice.  Apparently thrown if a
            // parser with specified options can not be built.
            logger.log(Level.WARNING, "Parser failure", pce);
        }
    };

    /**
     * Make a new reader for an element.
     *
     * @param element The {@code Element} to read.
     * @param intern If true make an interning reader.
     * @return A new {@code FreeColXMLReader} to read from.
     * @exception IOException if the reader can not be created,
     *     and miscellaneous run time exceptions for problems with the
     *     transformer mechanisms.
     */
    private static FreeColXMLReader makeElementReader(Element element,
                                                      boolean intern)
        throws IOException {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer xmlTransformer = factory.newTransformer();
            StringWriter stringWriter = new StringWriter();
            xmlTransformer.transform(new DOMSource(element),
                                     new StreamResult(stringWriter));
            String xml = stringWriter.toString();
            StringReader sr = new StringReader(xml);
            FreeColXMLReader xr = new FreeColXMLReader(sr);
            xr.setReadScope((intern)
                ? FreeColXMLReader.ReadScope.NORMAL
                : FreeColXMLReader.ReadScope.NOINTERN);
            return xr;
        } catch (TransformerException ex) {
            throw new RuntimeException("Reader creation failure", ex);
        }
    }

    /**
     * Get a FreeColObject class corresponding to the element tag.
     *
     * @param e The {@code Element} to query.
     * @return The class, or null if none found.
     */
    private static <T extends FreeColObject> Class<T> getElementClass(Element e) {
        return FreeColObject.getFreeColObjectClass(e.getTagName());
    }

    /**
     * Convenience method to find the first child element with the
     * specified tagname.
     *
     * @param element The {@code Element} to search for the child
     *     element in.
     * @param index The index of the child element.
     * @return The child element with the given index, or null if none
     *     present.
     */
    private static Element getChildElement(Element element, int index) {
        if (element == null) return null;
        NodeList nl = element.getChildNodes();
        return (nl == null || index >= nl.getLength()
            || !(nl.item(index) instanceof Element)) ? null
            : (Element)nl.item(index);
    }

    /**
     * Get a list of the child elements.
     *
     * @param element The parent {@code Element} to query.
     * @return A list of child {@code Element}s.
     */
    private static List<Element> getChildElementList(Element element) {
        List<Element> ret = new ArrayList<>();
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            if (nl.item(i) instanceof Element) ret.add((Element)nl.item(i));
        }
        return ret;
    }


    // Public interface
    
    /**
     * Creates and returns a new XML-document.
     *
     * @return the new XML-document.
     */
    public static Document createNewDocument() {
        synchronized (builder) {
            return builder.newDocument();
        }
    }

    /**
     * Create a new document with a root element with a given tag name.
     *
     * @param tag The root element name.
     * @return The new document.
     */
    public static Document createDocument(String tag) {
        Document document = createNewDocument();
        Element root = document.createElement(tag);
        document.appendChild(root);
        return document;
    }

    /**
     * Create a new document with a root element with a given tag name
     * and attributes.
     *
     * @param tag The root element name.
     * @param map A {@code Map} of attributes.
     * @return The new document.
     */
    public static Document createDocument(String tag, Map<String,String> map) {
        Document document = createDocument(tag);
        Element root = document.getDocumentElement();
        forEachMapEntry(map, e -> root.setAttribute(e.getKey(), e.getValue()));
        return document;
    }
    
    /**
     * Read a Document from an input source.
     * 
     * @param inputSource An {@code InputSource} to read from.
     * @return The resulting {@code Document}.
     * @exception IOException if thrown by the input stream.
     * @exception SAXException if thrown during parsing.
     */
    public static Document readDocument(InputSource inputSource)
        throws SAXException, IOException {
        Document tempDocument = null;
        boolean dumpMsgOnError = true;
        if (dumpMsgOnError) {
            inputSource.setByteStream(new BufferedInputStream(inputSource.getByteStream()));

            inputSource.getByteStream().mark(1000000);
        }
        try {
            synchronized (parser) {
                tempDocument = parser.parse(inputSource);
            }
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
        return tempDocument;
    }

    /**
     * Create a new document with a root element with a given tag name.
     *
     * @param tag The root element name.
     * @return The root element of the document.
     */
    public static Element createElement(String tag) {
        return createDocument(tag).getDocumentElement();
    }

    /**
     * Create a new document with a root element with a given tag name
     * and attributes.
     *
     * @param tag The root element name.
     * @param map A {@code Map} of attributes.
     * @return The root element of the document.
     */
    public static Element createElement(String tag, Map<String,String> map) {
        return createDocument(tag, map).getDocumentElement();
    }

    /**
     * Gets the type of an element.
     *
     * @param element The {@code Element} to query.
     * @return The type of the element.
     */
    public static String getType(Element element) {
        return (element == null) ? null : element.getTagName();
    }

    /**
     * Create a DOMMessage from an element.
     *
     * @param game The {@code Game} to create the message in.
     * @param element The {@code Element} to create the message from.
     * @return The message created, or null on failure.
     */
    public static DOMMessage createMessage(Game game, Element element) {
        if (element == null) return null;
        String tag = element.getTagName();
        tag = "net.sf.freecol.common.networking." + capitalize(tag) + "Message";
        DOMMessage message;
        Class<?> tagClass = Introspector.getClassByName(tag);
        if (tagClass == null) {
            message = new AttributeMessage(tag, getAttributeMap(element));
        } else {
            Class[] types = { Game.class, Element.class };
            Object[] params = { game, element };
            try {
                message = (DOMMessage)Introspector.instantiate(tag,
                    types, params);
            } catch (Introspector.IntrospectorException ex) {
                logger.log(Level.WARNING, "Instantiation fail for:" + tag, ex);
                message = null;
            }
        }            
        return message;
    }

    /**
     * Collapses a list of elements into a "multiple" element
     * with the original elements added as child nodes.
     *
     * @param elements A list of {@code Element}s to collapse.
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
        return new MultipleMessage(elements).toXMLElement();
    }

    /**
     * Handle a list of messages.
     *
     * @param mh The {@code MessageHandler} to handle the messages.
     * @param connection The {@code Connection} the messages arrived on.
     * @param elements The list of {@code Element}s to process.
     * @return An {@code Element} containing the response/s.
     */
    public static final Element handleList(MessageHandler mh,
        Connection connection, List<Element> elements) {
        List<Element> results = new ArrayList<>();
        int i = 0;
        for (Element e : elements) {
            final String tag = e.getTagName();
            try {
                Element reply = mh.handle(connection, e);
                if (reply != null) results.add(reply);
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Crash in multiple " + i
                    + ", tag " + tag + ", continuing.", ex);
            }
            i++;
        }
        return collapseElements(results);
    }    
    
    /**
     * Extract a child FreeColObject from an element given the
     * expected class.
     *
     * @param <T> The actual return type.
     * @param game The {@code Game} to instantiate within.
     * @param element The parent {@code Element}.
     * @param index The index of the child element.
     * @param intern Whether to intern the object found.
     * @param returnClass The expected class of the child.
     * @return A new instance of the return class, or null on error.
     */
    public static <T extends FreeColObject> T getChild(Game game,
        Element element, int index, boolean intern, Class<T> returnClass) {
        Element e = getChildElement(element, index);
        return (e == null) ? null : readElement(game, e, intern, returnClass);
    }

    /**
     * Extract a child FreeColObject from an element given its class.
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
        return getChild(game, element, index, true, returnClass);
    }

    /**
     * Extract a child FreeColObject from an element without knowing the class.
     *
     * @param <T> The actual return type.
     * @param game The {@code Game} to instantiate within.
     * @param element The parent {@code Element}.
     * @param index The index of the child element.
     * @param returnClass The expected class of the child.
     * @return A new instance of the return class, or null on error.
     */
    public static <T extends FreeColObject> T getChild(Game game,
        Element element, int index) {
        Element e = getChildElement(element, index);
        return (e == null) ? null : readElement(game, e, true);
    }

    /**
     * Extract all child FreeColObjects from an element given their
     * common class.
     *
     * @param <T> The actual list member return type.
     * @param game The {@code Game} to instantiate within.
     * @param element The parent {@code Element}.
     * @param returnClass The expected class of the child.
     * @return A list of new instances of the return class.
     */
    public static <T extends FreeColObject> List<T> getChildren(Game game,
        Element element, Class<T> returnClass) {
        List<T> ret = new ArrayList<>();
        for (Element e : getChildElementList(element)) {
            T t = readElement(game, e, true, returnClass);
            if (t != null) ret.add(t);
        }
        return ret;
    }

    /**
     * Extract all child FreeColObjects from an element without
     * knowing their class.
     *
     * @param <T> The actual list member return type.
     * @param game The {@code Game} to instantiate within.
     * @param element The parent {@code Element}.
     * @return A list of new instances of the return class.
     */
    public static List<FreeColObject> getChildren(Game game, Element element) {
        List<FreeColObject> ret = new ArrayList<>();
        for (Element e : getChildElementList(element)) {
            FreeColObject fco = readElement(game, e, true);
            if (fco != null) ret.add(fco);
        }
        return ret;
    }
    
    /**
     * Convenience method to map a function over the children of an Element.
     *
     * @param <T> The actual list member return type.
     * @param element The {@code Element} to extract children from.
     * @param mapper A mapper function.
     * @return A list of results of the mapping.
     */
    public static <T> List<T> mapChildren(Element element,
        Function<? super Element, ? extends T> mapper) {
        List<T> ret = new ArrayList<>();
        for (Element e : getChildElementList(element)) {
            T x = mapper.apply(e);
            if (x != null) ret.add(x);
        }
        return ret;
    }

    /**
     * Utility to extract the attributes of an element into a map.
     *
     * @param element The {@code Element} to extract from.
     * @return A {@code Map} of the attributes.
     */
    public static Map<String,String> getAttributeMap(Element element) {
        Map<String, String> map = new HashMap<>();
        NamedNodeMap nnm = element.getAttributes();
        final int n = nnm.getLength();
        for (int i = 0; i < n; i++) {
            Node node = nnm.item(i);
            map.put(node.getNodeName(), node.getNodeValue());
        }
        return map;
    }
    
    /**
     * Get an array of string attributes from an element.
     *
     * @param element The {@code Element} to query.
     * @return A list of the attributes found.
     */
    public static List<String> getArrayAttributes(Element element) {
        List<String> result = new ArrayList<>();
        String key;
        int i = 0;
        for (;;) {
            key = FreeColObject.arrayKey(i);
            if (!element.hasAttribute(key)) break;
            result.add(element.getAttribute(key));
            i++;
        }
        return result;
    }

    /**
     * Read a new FreeCol object from an element given its class.
     *
     * @param <T> The actual return type.
     * @param game The {@code Game} to check for existing objects.
     * @param element The {@code Element} to read from.
     * @param intern Whether to intern the instantiated object.
     * @param returnClass The expected return class.
     * @return The object found or instantiated, or null on error.
     */
    public static <T extends FreeColObject> T readElement(Game game,
        Element element, boolean intern, Class<T> returnClass) {
        if (element == null) return null;
        T ret = null;
        try (
             FreeColXMLReader xr = makeElementReader(element, intern);
        ) {
            xr.nextTag();
            //xr.setTracing(true);
            ret = xr.readFreeColObject(game, returnClass);
        } catch (XMLStreamException|IOException ex) {
            throw new RuntimeException("readElement fail", ex);
        }
        return ret;
    }

    /**
     * Read a new FreeCol object from an element without knowing its class.
     *
     * @param <T> The actual return type.
     * @param game The {@code Game} to check for existing objects.
     * @param element The {@code Element} to read from.
     * @param intern Whether to intern the instantiated object.
     * @return The object found or instantiated, or null on error.
     */
    public static <T extends FreeColObject> T readElement(Game game,
        Element element, boolean intern) {
        if (element == null) return null;
        final Class<T> c = getElementClass(element);
        return (c == null) ? null : readElement(game, element, intern, c);
    }


    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * Only attributes visible to the given {@code Player} will
     * be added to that representation if {@code showAll} is
     * set to {@code false}.
     *
     * @param fco The {@code FreeColObject} to write.
     * @param document The {@code Document}.
     * @param player The {@code Player} to send to, or to server if null.
     * @return An XML-representation of this object.
     */
    public static Element toXMLElement(FreeColObject fco, Document document,
                                       Player player) {
        return toXMLElement(fco, document, ((player == null)
                ? WriteScope.toServer()
                : WriteScope.toClient(player)), null);
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * Only attributes visible to the given {@code Player} will
     * be added to that representation if {@code showAll} is
     * set to {@code false}.
     *
     * @param fco The {@code FreeColObject} to write.
     * @param document The {@code Document}.
     * @param writeScope The {@code WriteScope} to apply.
     * @return An XML-representation of this object.
     */
    public static Element toXMLElement(FreeColObject fco, Document document,
                                       WriteScope writeScope) {
        if (!writeScope.isValid()) {
            throw new IllegalStateException("Invalid write scope: "
                + writeScope);
        }
        return toXMLElement(fco, document, writeScope, null);
    }

    /**
     * This method writes a partial XML-representation of this object to
     * an element using only the mandatory and specified fields.
     *
     * @param fco The {@code FreeColObject} to write.
     * @param document The {@code Document}.
     * @param fields The fields to write.
     * @return An XML-representation of this object.
     */
    public static Element toXMLElementPartial(FreeColObject fco,
                                              Document document,
                                              String... fields) {
        return toXMLElement(fco, document, WriteScope.toServer(), fields);
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * Only attributes visible to the given {@code Player} will
     * be added to that representation if {@code showAll} is
     * set to {@code false}.
     *
     * @param fco The {@code FreeColObject} to write.
     * @param document The {@code Document}.
     * @param writeScope The {@code WriteScope} to apply.
     * @param fields An array of field names, which if non-null
     *               indicates this should be a partial write.
     * @return An XML-representation of this object.
     */
    private static Element toXMLElement(FreeColObject fco, Document document,
                                        WriteScope writeScope, String[] fields) {
        StringWriter sw = new StringWriter();
        FreeColXMLWriter xw = null;
        try {
            xw = new FreeColXMLWriter(sw, writeScope);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Error creating FreeColXMLWriter,", ioe);
            return null;
        }

        try {
            if (fields == null) {
                try {
                    fco.toXML(xw);
                } catch (XMLStreamException xse) {
                    throw new RuntimeException("toXML failed on " + fco, xse);
                }
            } else {
                try {
                    fco.toXMLPartial(xw, fields);
                } catch (XMLStreamException xse) {
                    throw new RuntimeException("toXML[" + join(",", fields)
                        + "] failed on " + fco, xse);
                }
            }
            DocumentBuilderFactory factory
                = DocumentBuilderFactory.newInstance();
            Document tempDocument = null;
            DocumentBuilder builder = factory.newDocumentBuilder();
            tempDocument = builder.parse(new InputSource(new StringReader(sw.toString())));
            return (Element)document.importNode(tempDocument.getDocumentElement(), true);
        } catch (IOException|ParserConfigurationException|SAXException ex) {
            throw new RuntimeException("Parse fail at " + fco, ex);
        } finally {
            xw.close();
        }
    }

    /**
     * Convert an element to a string.
     *
     * @param element The {@code Element} to convert.
     * @return The {@code String} representation of an element.
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

    // Duplicates from DOMMessage

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
        if (element != null && element.hasAttribute(tag)) {
            String str = element.getAttribute(tag);
            try {
                return Boolean.parseBoolean(str);
            } catch (NumberFormatException e) {}
        }
        return defaultValue;
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
        if (element != null && element.hasAttribute(tag)) {
            String str = element.getAttribute(tag);
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException e) {}
        }
        return defaultValue;
    }

    /**
     * Get a string attribute value from an element.
     *
     * @param element The {@code Element} to query.
     * @param tag The attribute name.
     * @return The string value found, or the default value on error.
     */
    public static String getStringAttribute(Element element, String tag) {
        return (element != null && element.hasAttribute(tag))
            ? element.getAttribute(tag)
            : (String)null;
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
        String value = getStringAttribute(element, tag);
        if (value != null) {
            try {
                return Enum.valueOf(returnClass, value.toUpperCase(Locale.US));
            } catch (Exception ex) {}
        }
        return defaultValue;
    }
}
