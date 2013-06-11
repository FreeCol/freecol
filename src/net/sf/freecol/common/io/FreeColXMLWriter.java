/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Location;


/**
 * A wrapper for <code>XMLStreamWriter</code> and potentially an
 * underlying stream.  Adds on many useful utilities for writing
 * XML and FreeCol values.
 *
 * Unlike FreeColXMLReader, do not try to close the underlying stream.
 * Sometimes items are saved with successive FreeColXMLWriters writing
 * to the same OutputStream.
 * 
 * Strange, there is no StreamWriterDelegate, so we are stuck with
 * all the delegation functions.
 */
public class FreeColXMLWriter implements XMLStreamWriter {

    private static final Logger logger = Logger.getLogger(FreeColXMLWriter.class.getName());

    private XMLStreamWriter xmlStreamWriter;


    /**
     * Creates a new <code>FreeColXMLWriter</code>.
     *
     * @param outputStream The <code>OutputStream</code> to create
     *     an <code>FreeColXMLWriter</code> for.
     * @exception IOException if thrown while creating the
     *     <code>XMLStreamWriter</code>.
     */
    public FreeColXMLWriter(OutputStream outputStream) throws IOException {
        try {
            XMLOutputFactory xof = XMLOutputFactory.newInstance();
            this.xmlStreamWriter = xof.createXMLStreamWriter(outputStream,
                                                             "UTF-8");
        } catch (XMLStreamException e) {
            throw new IOException(e.getCause());
        }
    }

    /**
     * Creates a new <code>FreeColXMLWriter</code>.
     *
     * @param writer A <code>Writer</code> to create
     *     an <code>FreeColXMLWriter</code> for.
     * @exception IOException if thrown while creating the
     *     <code>FreeColXMLWriter</code>.
     */
    public FreeColXMLWriter(Writer writer) throws IOException {
        try {
            XMLOutputFactory xof = XMLOutputFactory.newInstance();
            this.xmlStreamWriter = xof.createXMLStreamWriter(writer);
        } catch (XMLStreamException e) {
            throw new IOException(e.getCause());
        }
    }

    /**
     * Closes both the <code>XMLStreamWriter</code> and
     * the underlying stream if any.
     */
    public void close() {
        if (xmlStreamWriter != null) {
            try {
                xmlStreamWriter.close();
            } catch (XMLStreamException xse) {
                logger.log(Level.WARNING, "Error closing stream.", xse);
            }
        }
    }


    /**
     * Write a boolean attribute to the stream.
     *
     * @param attributeName The attribute name.
     * @param value A boolean to write.
     * @exception XMLStreamException if a write error occurs.
     */
    public void writeAttribute(String attributeName, boolean value) throws XMLStreamException {
        xmlStreamWriter.writeAttribute(attributeName, String.valueOf(value));
    }

    /**
     * Write a float attribute to the stream.
     *
     * @param attributeName The attribute name.
     * @param value A float to write.
     * @exception XMLStreamException if a write error occurs.
     */
    public void writeAttribute(String attributeName, float value) throws XMLStreamException {
        xmlStreamWriter.writeAttribute(attributeName, String.valueOf(value));
    }

    /**
     * Write an integer attribute to the stream.
     *
     * @param attributeName The attribute name.
     * @param value An integer to write.
     * @exception XMLStreamException if a write error occurs.
     */
    public void writeAttribute(String attributeName, int value) throws XMLStreamException {
        xmlStreamWriter.writeAttribute(attributeName, String.valueOf(value));
    }

    /**
     * Write an enum attribute to the stream.
     *
     * @param attributeName The attribute name.
     * @param value The <code>Enum</code> to write.
     * @exception XMLStreamException if a write error occurs.
     */
    public void writeAttribute(String attributeName, Enum<?> value) throws XMLStreamException {
        xmlStreamWriter.writeAttribute(attributeName,
            value.toString().toLowerCase(Locale.US));
    }

    /**
     * Write an Object attribute to the stream.
     *
     * @param attributeName The attribute name.
     * @param value The <code>Object</code> to write.
     * @exception XMLStreamException if a write error occurs.
     */
    public void writeAttribute(String attributeName, Object value) throws XMLStreamException {
        xmlStreamWriter.writeAttribute(attributeName, String.valueOf(value));
    }

    /**
     * Write the identifier attribute of a non-null FreeColObject to the stream.
     *
     * @param attributeName The attribute name.
     * @param value The <code>FreeColObject</code> to write the identifier of.
     * @exception XMLStreamException if a write error occurs.
     */
    public void writeAttribute(String attributeName, FreeColObject value) throws XMLStreamException {
        if (value != null) {
            xmlStreamWriter.writeAttribute(attributeName, value.getId());
        }
    }

    /**
     * Write the identifier attribute of a non-null Location to the stream.
     *
     * @param attributeName The attribute name.
     * @param value The <code>Location</code> to write the identifier of.
     * @exception XMLStreamException if a write error occurs.
     */
    public void writeLocationAttribute(String attributeName, Location value) throws XMLStreamException {
        writeAttribute(attributeName, (FreeColObject)value);
    }

    /**
     * Writes an XML-representation of a collection object to the given stream.
     *
     * @param tag The tag for the array <code>Element</code>.
     * @param members The members of the array.
     * @exception XMLStreamException if a problem was encountered
     *      while writing.
     */
    public <T extends FreeColObject> void writeToListElement(String tag,
        Collection<T> members) throws XMLStreamException {
        writeStartElement(tag);

        writeAttribute(FreeColObject.ARRAY_SIZE_TAG, members.size());

        int i = 0;
        for (T t : members) {
            writeAttribute("x" + i, t);
            i++;
        }

        writeEndElement();
    }


    // Simple delegations.  All except close (supplied above).

    public void flush() throws XMLStreamException {
        xmlStreamWriter.flush();
    }

    public NamespaceContext getNamespaceContext() {
        return xmlStreamWriter.getNamespaceContext();
    }

    public String getPrefix(String uri) throws XMLStreamException {
        return xmlStreamWriter.getPrefix(uri);
    }

    public Object getProperty(String name) {
        return xmlStreamWriter.getProperty(name);
    }

    public void setDefaultNamespace(String uri) throws XMLStreamException {
        xmlStreamWriter.setDefaultNamespace(uri);
    }

    public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
        xmlStreamWriter.setNamespaceContext(context);
    }

    public void setPrefix(String prefix, String uri) throws XMLStreamException {
        xmlStreamWriter.setPrefix(prefix, uri);
    }

    public void writeAttribute(String localName, String value) throws XMLStreamException {
        xmlStreamWriter.writeAttribute(localName, value);
    }

    public void writeAttribute(String namespaceURI, String localName, String value) throws XMLStreamException {
        xmlStreamWriter.writeAttribute(namespaceURI, localName, value);
    }

    public void writeAttribute(String prefix, String namespaceURI, String localName, String value) throws XMLStreamException {
        xmlStreamWriter.writeAttribute(prefix, namespaceURI, localName, value);
    }

    public void writeCData(String data) throws XMLStreamException {
        xmlStreamWriter.writeCData(data);
    }

    public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
        xmlStreamWriter.writeCharacters(text, start, len);
    }

    public void writeCharacters(String text) throws XMLStreamException {
        xmlStreamWriter.writeCharacters(text);
    }

    public void writeComment(String data) throws XMLStreamException {
        xmlStreamWriter.writeComment(data);
    }

    public void writeDefaultNamespace(String namespaceURI) throws XMLStreamException {
        xmlStreamWriter.writeDefaultNamespace(namespaceURI);
    }

    public void writeDTD(String dtd) throws XMLStreamException {
        xmlStreamWriter.writeDTD(dtd);
    }

    public void writeEmptyElement(String localName) throws XMLStreamException {
        xmlStreamWriter.writeEmptyElement(localName);
    }

    public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
        xmlStreamWriter.writeEmptyElement(namespaceURI, localName);
    }

    public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        xmlStreamWriter.writeEmptyElement(prefix, localName, namespaceURI);
    }

    public void writeEndDocument() throws XMLStreamException {
        xmlStreamWriter.writeEndDocument();
    }

    public void writeEndElement() throws XMLStreamException {
        xmlStreamWriter.writeEndElement();
    }

    public void writeEntityRef(String name) throws XMLStreamException {
        xmlStreamWriter.writeEntityRef(name);
    }

    public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
        xmlStreamWriter.writeNamespace(prefix, namespaceURI);
    }

    public void writeProcessingInstruction(String target) throws XMLStreamException {
        xmlStreamWriter.writeProcessingInstruction(target);
    }

    public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
        xmlStreamWriter.writeProcessingInstruction(target, data);
    }

    public void writeStartDocument() throws XMLStreamException {
        xmlStreamWriter.writeStartDocument();
    }

    public void writeStartDocument(String version) throws XMLStreamException {
        xmlStreamWriter.writeStartDocument(version);
    }

    public void writeStartDocument(String encoding, String version) throws XMLStreamException {
        xmlStreamWriter.writeStartDocument(encoding, version);
    }

    public void writeStartElement(String localName) throws XMLStreamException {
        xmlStreamWriter.writeStartElement(localName);
    }

    public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
        xmlStreamWriter.writeStartElement(namespaceURI, localName);
    }

    public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        xmlStreamWriter.writeStartElement(prefix, localName, namespaceURI);
    }
}
