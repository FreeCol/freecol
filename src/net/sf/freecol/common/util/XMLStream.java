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

package net.sf.freecol.common.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Locale;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;


/**
 * A wrapper for <code>XMLStreamReader</code> and the underlying stream.
 *
 * The close method on <code>XMLStreamReader</code> doesn't close
 * the underlying stream. This class is a wrapper for the
 * <code>XMLStreamReader</code> and the underlying stream with
 * a {@link #close()} method which close them both.
 */
public class XMLStream implements Closeable {

    private static final Logger logger = Logger.getLogger(XMLStream.class.getName());

    private InputStream inputStream;
    private XMLStreamReader xmlStreamReader;


    /**
     * Creates a new <code>XMLStream</code>.
     *
     * @param inputStream The <code>InputStream</code> to create
     *     an <code>XMLStreamReader</code> for.
     * @throws IOException if thrown while creating the
     *     <code>XMLStreamReader</code>.
     */
    public XMLStream(InputStream inputStream) throws IOException {
        this.inputStream = inputStream;
        try {
            XMLInputFactory xif = XMLInputFactory.newInstance();
            this.xmlStreamReader = xif.createXMLStreamReader(inputStream,
                                                             "UTF-8");
        } catch (XMLStreamException e) {
            throw new IOException(e.getCause());
        }
    }

    /**
     * Creates a new <code>XMLStream</code>.
     *
     * @param reader A <code>Reader</code> to create
     *     an <code>XMLStreamReader</code> for.
     * @throws IOException if thrown while creating the
     *     <code>XMLStreamReader</code>.
     */
    public XMLStream(Reader reader) throws IOException {
        this.inputStream = null;
        try {
            XMLInputFactory xif = XMLInputFactory.newInstance();
            this.xmlStreamReader = xif.createXMLStreamReader(reader);
        } catch (XMLStreamException e) {
            throw new IOException(e.getCause());
        }
    }


    /**
     * Get the <code>XMLStreamReader</code>.
     *
     * @return The <code>XMLStreamReader</code> created using
     *      the underlying stream provided by the contructor
     *      on this object.
     */
    public XMLStreamReader getXMLStreamReader() {
        return xmlStreamReader;
    }

    /**
     * Closes both the <code>XMLStreamReader</code> and
     * the underlying stream.
     */
    public void close() {
        if (xmlStreamReader != null) {
            try {
                xmlStreamReader.close();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error closing XMLStreamReader", e);
            }
            xmlStreamReader = null;
        }

        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error closing InputStream", e);
            }
            inputStream = null;
        }
    }

    /**
     * Advance the underlying stream to the next tag.
     *
     * @return The next tag.
     * @exception XMLStreamException if there is a problem with the stream.
     */
    public int nextTag() throws XMLStreamException {
        return xmlStreamReader.nextTag();
    }

    /**
     * Get the tag name from the underlying stream.
     *
     * @return The current tag.
     */
    public String getTagName() {
        return xmlStreamReader.getLocalName();
    }

    /**
     * Get the tag type from the underlying stream.
     *
     * @return The current type.
     */
    public int getTagType() {
        return xmlStreamReader.getEventType();
    }


    /**
     * Is there an attribute present in the stream?
     *
     * @param attributeName An attribute name
     * @return True if the attribute is present.
     */
    public boolean hasAttribute(String attributeName) {
        return xmlStreamReader.getAttributeValue(null, attributeName) != null;
    }

    /**
     * Gets a boolean from an attribute in a stream.
     *
     * @param attributeName The attribute name.
     * @param defaultValue The default value.
     * @return The boolean attribute value, or the default value if none found.
     */
    public boolean getAttribute(String attributeName, boolean defaultValue) {
        final String attrib = xmlStreamReader.getAttributeValue(null,
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
        final String attrib = xmlStreamReader.getAttributeValue(null,
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
        final String attrib = xmlStreamReader.getAttributeValue(null,
                                                                attributeName);

        int result = defaultValue;
        if (attrib != null) {
            try {
                result = Integer.parseInt(attrib);
            } catch (NumberFormatException e) {
                logger.warning(attributeName + " is not an integer: " + attrib);
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
        final String attrib = xmlStreamReader.getAttributeValue(null,
                                                                attributeName);

        return (attrib == null) ? defaultValue
            : attrib;
    }

    /**
     * Gets an enum from an attribute in a stream.
     *
     * @param attributeName The attribute name.
     * @param returnType The type of the return value.
     * @param defaultValue The default value.
     * @return The enum attribute value, or the default value if none found.
     */
    public <T extends Enum<T>> T getAttribute(String attributeName,
                                              Class<T> returnType,
                                              T defaultValue) {
        final String attrib = xmlStreamReader.getAttributeValue(null,
                                                                attributeName);

        T result = defaultValue;
        if (attrib != null) {
            try {
                result = Enum.valueOf(returnType,
                                      attrib.toUpperCase(Locale.US));
            } catch (Exception e) {
                logger.warning(attributeName + " is not a "
                    + defaultValue.getClass().getName() + ": " + attrib);
            }
        }
        return result;
    }

    /**
     * Read the identifier attribute.
     *
     * @return The identifier attribute.
     */
    public String readId() {
        String id = getAttribute("ID", (String)null);
        if (id == null) id = getAttribute("id", (String)null);
        return id;
    }
}
