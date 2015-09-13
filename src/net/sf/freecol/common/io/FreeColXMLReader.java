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

package net.sf.freecol.common.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.util.StreamReaderDelegate;

import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColGameObjectType;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.server.ai.AIObject;
import net.sf.freecol.server.ai.AIMain;


/**
 * A wrapper for <code>XMLStreamReader</code> and potentially an
 * underlying stream.  Adds on many useful utilities for reading
 * XML and FreeCol values.
 */
public class FreeColXMLReader extends StreamReaderDelegate
    implements Closeable {

    private static final Logger logger = Logger.getLogger(FreeColXMLReader.class.getName());

    public static enum ReadScope {
        SERVER,     // Loading the game in the server
        NORMAL,     // Normal interning read
        NOINTERN,   // Do not intern any object that are read
    }

    /** The stream to read from. */
    private InputStream inputStream = null;

    /** The read scope to apply. */
    private ReadScope readScope;

    /** A cache of uninterned objects. */
    private Map<String, FreeColObject> uninterned = null;


    /**
     * Creates a new <code>FreeColXMLReader</code>.
     *
     * @param inputStream The <code>InputStream</code> to create
     *     an <code>FreeColXMLReader</code> for.
     * @exception IOException if thrown while creating the
     *     <code>XMLStreamReader</code>.
     */
    public FreeColXMLReader(InputStream inputStream) throws IOException {
        super();

        try {
            XMLInputFactory xif = XMLInputFactory.newInstance();
            setParent(xif.createXMLStreamReader(inputStream, "UTF-8"));
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
        this.inputStream = inputStream;
        this.readScope = ReadScope.NORMAL;
    }

    /**
     * Creates a new <code>FreeColXMLReader</code>.
     *
     * @param reader A <code>Reader</code> to create
     *     an <code>FreeColXMLReader</code> for.
     * @exception IOException if thrown while creating the
     *     <code>FreeColXMLReader</code>.
     */
    public FreeColXMLReader(Reader reader) throws IOException {
        super();

        try {
            XMLInputFactory xif = XMLInputFactory.newInstance();
            setParent(xif.createXMLStreamReader(reader));
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
        this.inputStream = null;
        this.readScope = ReadScope.NORMAL;
    }


    /**
     * Should reads from this stream intern their objects into the
     * enclosing game?
     *
     * @return True if this is an interning stream.
     */
    public boolean shouldIntern() {
        return this.readScope != ReadScope.NOINTERN;
    }

    /**
     * Get the read scope.
     *
     * @return The <code>ReadScope</code>.
     */
    public ReadScope getReadScope() {
        return this.readScope;
    }

    /**
     * Set the read scope.
     *
     * @param readScope The new <code>ReadScope</code>.
     */
    public void setReadScope(ReadScope readScope) {
        this.readScope = readScope;
        this.uninterned = (shouldIntern()) ? null
            : new HashMap<String, FreeColObject>();
    }

    /**
     * Look up an identifier in an enclosing game.  If not interning
     * prefer an non-interned result.
     *
     * @param game The <code>Game</code> to consult.
     * @param id The object identifier.
     * @return The <code>FreeColObject</code> found, or null if none.
     */
    private FreeColObject lookup(Game game, String id) {
        FreeColObject fco = (shouldIntern()) ? null : uninterned.get(id);
        return (fco != null) ? fco
            : game.getFreeColGameObject(id);
    }

    /**
     * Closes both the <code>XMLStreamReader</code> and
     * the underlying stream if any.
     *
     * Implements interface Closeable.
     */
    @Override
    public void close() {
        try {
            super.close();
        } catch (XMLStreamException xse) {
            logger.log(Level.WARNING, "Error closing stream.", xse);
        }

        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "Error closing stream.", ioe);
            }
            inputStream = null;
        }
    }

    // @compat 0.10.x
    /**
     * Reads the identifier attribute.
     *
     * Normally a simple getAttribute() would be sufficient, but
     * while we are allowing both the obsolete ID_ATTRIBUTE and the correct
     * ID_ATTRIBUTE_TAG, this routine is useful.
     *
     * When 0.10.x is obsolete, remove this routine and replace its
     * uses with just getAttribute(in, ID_ATTRIBUTE_TAG, (String)null)
     * or equivalent.
     *
     * @return The identifier found, or null if none present.
     */
    public String readId() {
        String id = getAttribute(FreeColObject.ID_ATTRIBUTE_TAG, (String)null);
        if (id == null) {
            id = getAttribute(FreeColObject.ID_ATTRIBUTE, (String)null);
        }
        return id;
    }
    // end @compat 0.10.x

    /**
     * Is the stream at the given tag?
     *
     * @param tag The tag to test.
     * @return True if at the given tag.
     */
    public boolean atTag(String tag) {
        return getLocalName().equals(tag);
    }

    /**
     * Expect a particular tag.
     *
     * @param tag The expected tag name.
     * @exception XMLStreamException if the expected tag is not found.
     */
    public void expectTag(String tag) throws XMLStreamException {
        final String endTag = getLocalName();
        if (!endTag.equals(tag)) {
            throw new XMLStreamException("Parse error, " + tag
                + " expected, not: " + endTag);
        }
    }

    /**
     * Close the current tag, checking that it did indeed close correctly.
     *
     * @param tag The expected tag name.
     * @exception XMLStreamException if a closing tag is not found.
     */
    public void closeTag(String tag) throws XMLStreamException {
        if (nextTag() != XMLStreamConstants.END_ELEMENT) {
            throw new XMLStreamException("Parse error, END_ELEMENT expected,"
                + " not: " + getLocalName());
        }
        expectTag(tag);
    }

    /**
     * Extract the current tag and its attributes from an input stream.
     * Useful for error messages.
     *
     * @return A simple display of the stream state.
     */
    public String currentTag() {
        StringBuilder sb = new StringBuilder(getLocalName());
        sb.append(", attributes:");
        int n = getAttributeCount();
        for (int i = 0; i < n; i++) {
            sb.append(" ").append(getAttributeLocalName(i))
                .append("=\"").append(getAttributeValue(i)).append("\"");
        }
        return sb.toString();
    }

    /**
     * Is there an attribute present in the stream?
     *
     * @param attributeName An attribute name
     * @return True if the attribute is present.
     */
    public boolean hasAttribute(String attributeName) {
        return getParent().getAttributeValue(null, attributeName) != null;
    }

    /**
     * Gets a boolean from an attribute in a stream.
     *
     * @param attributeName The attribute name.
     * @param defaultValue The default value.
     * @return The boolean attribute value, or the default value if none found.
     */
    public boolean getAttribute(String attributeName, boolean defaultValue) {
        final String attrib = getParent().getAttributeValue(null,
                                                            attributeName);
        return (attrib == null) ? defaultValue
            : Boolean.parseBoolean(attrib);
    }

    /**
     * Gets a float from an attribute in a stream.
     *
     * @param attributeName The attribute name.
     * @param defaultValue The default value.
     * @return The float attribute value, or the default value if none found.
     */
    public float getAttribute(String attributeName, float defaultValue) {
        final String attrib = getParent().getAttributeValue(null,
                                                            attributeName);
        float result = defaultValue;
        if (attrib != null) {
            try {
                result = Float.parseFloat(attrib);
            } catch (NumberFormatException e) {
                logger.warning(attributeName + " is not a float: " + attrib);
            }
        }
        return result;
    }

    /**
     * Gets an int from an attribute in a stream.
     *
     * @param attributeName The attribute name.
     * @param defaultValue The default value.
     * @return The int attribute value, or the default value if none found.
     */
    public int getAttribute(String attributeName, int defaultValue) {
        final String attrib = getParent().getAttributeValue(null,
                                                            attributeName);
        int result = defaultValue;
        if (attrib != null) {
            try {
                result = Integer.decode(attrib);
            } catch (NumberFormatException e) {
                logger.warning(attributeName + " is not an integer: " + attrib);
            }
        }
        return result;
    }

    /**
     * Gets a long from an attribute in a stream.
     *
     * @param attributeName The attribute name.
     * @param defaultValue The default value.
     * @return The long attribute value, or the default value if none found.
     */
    public long getAttribute(String attributeName, long defaultValue) {
        final String attrib = getParent().getAttributeValue(null,
                                                            attributeName);
        long result = defaultValue;
        if (attrib != null) {
            try {
                result = Long.decode(attrib);
            } catch (NumberFormatException e) {
                logger.warning(attributeName + " is not a long: " + attrib);
            }
        }
        return result;
    }

    /**
     * Gets a string from an attribute in a stream.
     *
     * @param attributeName The attribute name.
     * @param defaultValue The default value.
     * @return The string attribute value, or the default value if none found.
     */
    public String getAttribute(String attributeName, String defaultValue) {
        final String attrib = getParent().getAttributeValue(null,
                                                            attributeName);
        return (attrib == null) ? defaultValue
            : attrib;
    }

    /**
     * Gets an enum from an attribute in a stream.
     *
     * @param attributeName The attribute name.
     * @param returnClass The class of the return value.
     * @param defaultValue The default value.
     * @return The enum attribute value, or the default value if none found.
     */
    public <T extends Enum<T>> T getAttribute(String attributeName,
                                              Class<T> returnClass,
                                              T defaultValue) {
        final String attrib = getParent().getAttributeValue(null,
                                                            attributeName);
        T result = defaultValue;
        if (attrib != null) {
            try {
                result = Enum.valueOf(returnClass,
                                      attrib.toUpperCase(Locale.US));
            } catch (Exception e) {
                logger.warning(attributeName + " is not a "
                    + defaultValue.getClass().getName() + ": " + attrib);
            }
        }
        return result;
    }

    /**
     * Gets a FreeCol object from an attribute in a stream.
     *
     * @param game The <code>Game</code> to look in.
     * @param attributeName The attribute name.
     * @param returnClass The <code>FreeColObject</code> type to expect.
     * @param defaultValue The default value.
     * @return The <code>FreeColObject</code> found, or the default
     *     value if not.
     * @exception XMLStreamException if the wrong class was passed.
     */
    public <T extends FreeColObject> T getAttribute(Game game,
        String attributeName, Class<T> returnClass,
        T defaultValue) throws XMLStreamException {

        final String attrib =
        // @compat 0.10.7
            (FreeColObject.ID_ATTRIBUTE_TAG.equals(attributeName)) ? readId() :
        // end @compat
            getAttribute(attributeName, (String)null);

        if (attrib == null) return defaultValue;
        FreeColObject fco = lookup(game, attrib);
        try {
            return returnClass.cast(fco);
        } catch (ClassCastException cce) {
            throw new XMLStreamException(cce);
        }
    }

    /**
     * Get a FreeCol AI object from an attribute in a stream.
     *
     * @param aiMain The <code>AIMain</code> that contains the object.
     * @param attributeName The attribute name.
     * @param returnClass The <code>AIObject</code> type to expect.
     * @param defaultValue The default value.
     * @return The <code>AIObject</code> found, or the default value if not.
     */
    public <T extends AIObject> T getAttribute(AIMain aiMain,
        String attributeName, Class<T> returnClass, T defaultValue) {
        final String attrib =
        // @compat 0.10.7
            (FreeColObject.ID_ATTRIBUTE_TAG.equals(attributeName)) ? readId() :
        // end @compat
            getAttribute(attributeName, (String)null);

        return (attrib == null) ? defaultValue
            : aiMain.getAIObject(attrib, returnClass);
    }

    /**
     * Find a new location from a stream attribute.  This is necessary
     * because <code>Location</code> is an interface.
     *
     * @param game The <code>Game</code> to look in.
     * @param attributeName The attribute to check.
     * @param make If true, try to make the location if it is not found.
     * @return The <code>Location</code> found.
     */
    public Location getLocationAttribute(Game game, String attributeName,
        boolean make) throws XMLStreamException {

        if (attributeName == null) return null;

        final String attrib =
        // @compat 0.10.7
            (FreeColObject.ID_ATTRIBUTE_TAG.equals(attributeName)) ? readId() :
        // end @compat
            getAttribute(attributeName, (String)null);

        if (attrib != null) {
            FreeColObject fco = lookup(game, attrib);
            if (fco == null && make) {
                Class<? extends FreeColGameObject> c
                    = game.getLocationClass(attrib);
                if (c != null) {
                    fco = makeFreeColGameObject(game, attributeName, c,
                        getReadScope() == ReadScope.SERVER);
                }
            }
            if (fco instanceof Location) return (Location)fco;
                logger.warning("Not a location: " + attrib);
        }
        return null;
    }

    /**
     * Reads an XML-representation of a list of some general type.
     *
     * @param tag The tag for the list <code>Element</code>.
     * @param type The type of the items to be added.  This type
     *     needs to have a constructor accepting a single <code>String</code>.
     * @return The list.
     * @exception XMLStreamException if a problem was encountered
     *     during parsing.
     */
    public <T> List<T> readList(String tag, Class<T> type)
        throws XMLStreamException {

        expectTag(tag);

        final int length = getAttribute(FreeColObject.ARRAY_SIZE_TAG, -1);
        if (length < 0) return Collections.<T>emptyList();

        List<T> list = new ArrayList<>(length);
        for (int x = 0; x < length; x++) {
            try {
                final String value = getAttribute("x" + x, (String)null);
                T object = null;
                if (value != null) {
                    Constructor<T> c = type.getConstructor(type);
                    object = c.newInstance(new Object[] {value});
                }
                list.add(object);
            } catch (IllegalAccessException|InstantiationException
                |InvocationTargetException|NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        closeTag(tag);
        return list;
    }

    /**
     * Reads an XML-representation of a list of
     * <code>FreeColGameObjectType</code>s.
     *
     * @param tag The tag for the list <code>Element</code>.
     * @param spec The <code>Specification</code> to find items in.
     * @param type The type of the items to be added.  The type must exist
     *     in the supplied specification.
     * @return The list.
     * @exception XMLStreamException if a problem was encountered
     *     during parsing.
     */
    public <T extends FreeColGameObjectType> List<T> readList(Specification spec,
        String tag, Class<T> type) throws XMLStreamException {

        expectTag(tag);

        final int length = getAttribute(FreeColObject.ARRAY_SIZE_TAG, -1);
        if (length < 0) return Collections.<T>emptyList();

        List<T> list = new ArrayList<>(length);
        for (int x = 0; x < length; x++) {
            T value = getType(spec, "x" + x, type, (T)null); 
            if (value == null) logger.warning("Null list value(" + x + ")");
            list.add(value);
        }

        closeTag(tag);
        return list;
    }

    /**
     * Find a <code>FreeColGameObject</code> of a given class
     * from a stream attribute.
     *
     * Use this routine when the object is optionally already be
     * present in the game.
     *
     * @param game The <code>Game<code> to look in.
     * @param attributeName The attribute name.
     * @param returnClass The class to expect.
     * @param defaultValue A default value to return if not found.
     * @param required If true a null result should throw an exception.
     * @return The <code>FreeColGameObject</code> found, or the default
     *     value if not found.
     * @exception XMLStreamException if the attribute is missing.
     */
    public <T extends FreeColGameObject> T findFreeColGameObject(Game game,
        String attributeName, Class<T> returnClass, T defaultValue,
        boolean required) throws XMLStreamException {

        T ret = getAttribute(game, attributeName, returnClass, (T)null);
        if (ret == (T)null) {
            if (required) {
                throw new XMLStreamException("Missing " + attributeName
                    + " for " + returnClass.getName() + ": " + currentTag());
            } else {
                ret = defaultValue;
            }
        }
        return ret;
    }

    /**
     * Either get an existing <code>FreeColGameObject</code> from a stream
     * attribute or create it if it does not exist.
     *
     * Use this routine when the object may not necessarily already be
     * present in the game, but is expected to be defined eventually.
     *
     * @param game The <code>Game</code> to look in.
     * @param attributeName The required attribute name.
     * @param returnClass The class of object.
     * @param required If true a null result should throw an exception.
     * @return The <code>FreeColGameObject</code> found or made, or null
     *     if the attribute was not present.
     */
    public <T extends FreeColGameObject> T makeFreeColGameObject(Game game,
        String attributeName, Class<T> returnClass,
        boolean required) throws XMLStreamException {
        final String id =
            // @compat 0.10.7
            (FreeColObject.ID_ATTRIBUTE_TAG.equals(attributeName)) ? readId() :
            // end @compat
            getAttribute(attributeName, (String)null);

        if (id == null) {
            if (required) {
                throw new XMLStreamException("Missing " + attributeName
                    + " for " + returnClass.getName() + ": " + currentTag());
            }
        } else {
            FreeColObject fco = lookup(game, id);
            if (fco == null) {
                try {
                    T ret = game.newInstance(returnClass,
                        getReadScope() == ReadScope.SERVER);
                    if (shouldIntern()) {
                        ret.internId(id);
                    } else {
                        uninterned.put(id, ret);
                    }
                    return ret;
                } catch (IOException e) {
                    if (required) {
                        throw new XMLStreamException(e);
                    } else {
                        logger.log(Level.WARNING, "Failed to create FCGO: "
                            + id, e);
                    }
                }
            } else {
                try {
                    return returnClass.cast(fco);
                } catch (ClassCastException cce) {
                    throw new XMLStreamException(cce);
                }
            }
        }
        return null;
    }

    /**
     * Do a normal interning read of a <code>FreeColGameObject</code>.
     *
     * @param game The <code>Game</code> to look in.
     * @param returnClass The class to expect.
     * @return The <code>FreeColGameObject</code> found, or null there
     *     was no ID_ATTRIBUTE_TAG present.
     * @exception XMLStreamException if there is problem reading the stream.
     */
    private <T extends FreeColGameObject> T internedRead(Game game,
        Class<T> returnClass) throws XMLStreamException {

        T ret = makeFreeColGameObject(game, FreeColObject.ID_ATTRIBUTE_TAG, 
                                      returnClass, false);
        if (ret != null) ret.readFromXML(this);
        return ret;
    }

    /**
     * Do a special non-interning read of a <code>FreeColObject</code>.
     *
     * @param game The <code>Game</code> to look in.
     * @param returnClass The class to expect.
     * @return The <code>FreeColObject</code> found, or null there
     *     was no ID_ATTRIBUTE_TAG present.
     * @exception XMLStreamException if there is problem reading the stream.
     */
    private <T extends FreeColObject> T uninternedRead(Game game,
        Class<T> returnClass) throws XMLStreamException {

        T ret;
        try {
            ret = game.newInstance(returnClass, false);
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
        String id = readId();
        if (id == null) {
            throw new XMLStreamException("Object identifier not found.");
        }
        uninterned.put(id, ret);
        ret.readFromXML(this);
        return ret;
    }

    /**
     * Reads a <code>FreeColGameObject</code> from a stream.
     * Expects the object to be identified by the standard ID_ATTRIBUTE_TAG.
     *
     * Use this routine when the object may or may not have been
     * referenced and created-by-id in this game, but this is the
     * point where it is authoritatively defined.
     *
     * @param game The <code>Game</code> to look in.
     * @param returnClass The class to expect.
     * @return The <code>FreeColGameObject</code> found, or null there
     *     was no ID_ATTRIBUTE_TAG present.
     * @exception XMLStreamException if there is problem reading the stream.
     */
    public <T extends FreeColGameObject> T readFreeColGameObject(Game game,
        Class<T> returnClass) throws XMLStreamException {
        return (shouldIntern())
            ? internedRead(game, returnClass)
            : uninternedRead(game, returnClass);
    }

    /**
     * Find a FreeCol AI object from an attribute in a stream.
     *
     * @param aiMain The <code>AIMain</code> that contains the object.
     * @param attributeName The attribute name.
     * @param returnClass The <code>AIObject</code> type to expect.
     * @param defaultValue The default value.
     * @param required If true a null result should throw an exception.
     * @exception XMLStreamException if there is problem reading the stream.
     * @return The <code>AIObject</code> found, or the default value if not.
     */
    public <T extends AIObject> T findAIObject(AIMain aiMain,
        String attributeName, Class<T> returnClass, T defaultValue,
        boolean required) throws XMLStreamException {

        T ret = getAttribute(aiMain, attributeName, returnClass, (T)null);
        if (ret == (T)null) {
            if (required) {
                throw new XMLStreamException("Missing " + attributeName
                    + " for " + returnClass.getName() + ": " + currentTag());
            } else {
                ret = defaultValue;
            }
        }
        return ret;
    }

    /**
     * Either get an existing <code>AIObject</code> from a stream
     * attribute or create it if it does not exist.
     *
     * Use this routine when the object may not necessarily already be
     * present in the game, but is expected to be defined eventually.
     * @param aiMain The <code>AIMain</code> that contains the object.
     * @param attributeName The attribute name.
     * @param returnClass The <code>AIObject</code> type to expect.
     * @param defaultValue The default value.
     * @exception XMLStreamException if there is problem reading the stream.
     * @return The <code>AIObject</code> found, or the default value if not.
     */
    public <T extends AIObject> T makeAIObject(AIMain aiMain,
        String attributeName, Class<T> returnClass, T defaultValue,
        boolean required) throws XMLStreamException {

        final String id =
            // @compat 0.10.7
            (FreeColObject.ID_ATTRIBUTE_TAG.equals(attributeName)) ? readId() :
            // end @compat
            getAttribute(attributeName, (String)null);

        T ret = null;
        if (id == null) {
            if (required) {
                throw new XMLStreamException("Missing " + attributeName
                    + " for " + returnClass.getName() + ": " + currentTag());
            }
        } else {
            ret = aiMain.getAIObject(id, returnClass);
            if (ret == null) {
                try {
                    Constructor<T> c = returnClass.getConstructor(AIMain.class,
                                                                  String.class);
                    ret = returnClass.cast(c.newInstance(aiMain, id));
                    if (required && ret == null) {
                        throw new XMLStreamException("Constructed null "
                            + returnClass.getName() + " for " + id
                            + ": " + currentTag());
                    }
                } catch (NoSuchMethodException | SecurityException 
                        | InstantiationException | IllegalAccessException 
                        | IllegalArgumentException | InvocationTargetException 
                        | XMLStreamException e) {
                    if (required) {
                        throw new XMLStreamException(e);
                    } else {
                        logger.log(Level.WARNING, "Failed to create AIObject: "
                                   + id, e);
                    }
                }
            }
        }
        return ret;
    }

    /**
     * Should the game object type being read clear its containers before
     * reading the child elements?
     *
     * Usually true, but not if the type is extending another one.
     *
     * @return True if the containers should be cleared.
     */
    public boolean shouldClearContainers() {
        return !hasAttribute(FreeColGameObjectType.EXTENDS_TAG)
            && !hasAttribute(FreeColGameObjectType.PRESERVE_TAG);
    }

    /**
     * Get a FreeColGameObjectType by identifier from a stream from a
     * specification.
     *
     * @param spec The <code>Specification</code> to look in.
     * @param attributeName the name of the attribute identifying the
     *     <code>FreeColGameObjectType</code>.
     * @param returnClass The expected class of the return value.
     * @param defaultValue A default value to return if the attributeName 
     *     attribute is not present.
     * @return The <code>FreeColGameObjectType</code> found, or the
     *     <code>defaultValue</code>.
     */
    public <T extends FreeColGameObjectType> T getType(Specification spec,
        String attributeName, Class<T> returnClass, T defaultValue) {

        final String attrib =
        // @compat 0.10.7
            (FreeColObject.ID_ATTRIBUTE_TAG.equals(attributeName)) ? readId() :
        // end @compat
            getAttribute(attributeName, (String)null);

        return (attrib == null) ? defaultValue
            : spec.getType(attrib, returnClass);
    }

    // @compat 0.10.7
    public <T extends FreeColGameObjectType> T getRole(Specification spec,
        String attributeName, Class<T> returnClass, T defaultValue) {

        String attrib =
            (FreeColObject.ID_ATTRIBUTE_TAG.equals(attributeName)) ? readId() :
            getAttribute(attributeName, (String)null);

        if (attrib == null) {
            return defaultValue;
        }
        attrib = Role.fixRoleId(attrib);
        return spec.getType(attrib, returnClass);
    }
    // end @compat

    /**
     * Copy a FreeColObject by serializing it and reading back the result
     * with a non-interning stream.
     *
     * @param game The <code>Game</code> to look in.
     * @param returnClass The class to expect.
     * @return The copied <code>FreeColObject</code> found, or null there
     *     was no ID_ATTRIBUTE_TAG present.
     * @exception XMLStreamException if there is problem reading the stream.
     */
    public <T extends FreeColObject> T copy(Game game, Class<T> returnClass)
        throws XMLStreamException {

        setReadScope(ReadScope.NOINTERN);
        nextTag();
        return uninternedRead(game, returnClass);
    }
}
