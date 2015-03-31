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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import java.util.Collection;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Player;


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
public class FreeColXMLWriter implements Closeable, XMLStreamWriter {

    private static final Logger logger = Logger.getLogger(FreeColXMLWriter.class.getName());

    /** Magic properties to indent files if supported. */
    private static final String[] indentProps = {
        OutputKeys.INDENT, "yes",
        "{http://xml.apache.org/xslt}indent-amount", "2"
    };

    /** The scope of a FreeCol object write. */
    public static enum WriteScope {
        CLIENT,  // Only the client-visible information
        SERVER,  // Full server-visible information
        SAVE;    // Absolutely everything needed to save the game state

        private Player player = null; // The player to write to.


        public static WriteScope toClient(Player player) {
            if (player == null) {
                throw new IllegalArgumentException("Null player.");
            }
            WriteScope ret = WriteScope.CLIENT;
            ret.player = player;
            return ret;
        }            

        public static WriteScope toServer() {
            return WriteScope.SERVER;
        }

        public static WriteScope toSave() {
            return WriteScope.SAVE;
        }

        public boolean isValid() {
            return (this == WriteScope.CLIENT) == (player != null);
        }

        public boolean validForSave() {
            return this == WriteScope.SAVE;
        }

        public boolean validFor(Player player) {
            return this != WriteScope.CLIENT || this.player == player;
        }

        public Player getClient() {
            return this.player;
        }
    }


    /** The stream to write to. */
    private final XMLStreamWriter xmlStreamWriter;

    /** Internal writer to accumulate into when pretty printing. */
    private StringWriter stringWriter = null;
    /** The final output writer to use when pretty printing. */
    private Writer outputWriter = null;

    /** A write scope to use for FreeCol object writes. */
    private WriteScope writeScope;


    /**
     * Creates a new <code>FreeColXMLWriter</code>.
     *
     * @param outputStream The <code>OutputStream</code> to create
     *     an <code>FreeColXMLWriter</code> for.
     * @param scope The <code>WriteScope</code> to use for FreeCol
     *     object writes.
     * @param indent If true, produce indented output if supported.
     * @exception IOException if thrown while creating the
     *     <code>XMLStreamWriter</code>.
     */
    public FreeColXMLWriter(OutputStream outputStream, WriteScope scope,
                            boolean indent) throws IOException {
        this(new OutputStreamWriter(outputStream, "UTF-8"), scope, indent);
    }

    /**
     * Creates a new <code>FreeColXMLWriter</code>.
     *
     * @param writer A <code>Writer</code> to create an
     *     <code>FreeColXMLWriter</code> for.
     * @param scope The <code>WriteScope</code> to use for FreeCol
     *     object writes.
     * @exception IOException if thrown while creating the
     *     <code>XMLStreamWriter</code>.
     */
    public FreeColXMLWriter(Writer writer, WriteScope scope) throws IOException {
        this(writer, scope, false);
    }

    /**
     * Creates a new <code>FreeColXMLWriter</code>.
     *
     * @param scope The <code>WriteScope</code> to use for FreeCol
     *     object writes.
     * @param indent If true, produce indented output if supported.
     * @exception IOException if thrown while creating the
     *     <code>FreeColXMLWriter</code>.
     */
    private FreeColXMLWriter(Writer writer, WriteScope scope,
                             boolean indent) throws IOException {
        try {
            if (indent) {
                this.outputWriter = writer;
                this.stringWriter = new StringWriter(1024);
                this.xmlStreamWriter = getFactory()
                    .createXMLStreamWriter(this.stringWriter);
            } else {
                this.xmlStreamWriter = getFactory()
                    .createXMLStreamWriter(writer);
            }
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
        this.writeScope = scope;
    }


    /**
     * Get the <code>XMLOutputFactory</code> to create the output stream with.
     *
     * @return An <code>XMLOutputFactory</code>.
     */
    private XMLOutputFactory getFactory() {
        return XMLOutputFactory.newInstance();
    }

    /**
     * Get the write scope prevailing on this stream.
     *
     * @return The write scope.
     */     
    public WriteScope getWriteScope() {
        return this.writeScope;
    }

    /**
     * Set the write scope prevailing on this stream.
     *
     * @param writeScope The new <code>WriteScope</code>.
     */     
    public void setWriteScope(WriteScope writeScope) {
        this.writeScope = writeScope;
    }

    /**
     * Closes both the <code>XMLStreamWriter</code> and
     * the underlying stream if any.
     *
     * Implement interface Closeable.
     */
    @Override
    public void close() {
        if (xmlStreamWriter != null) {
            try {
                xmlStreamWriter.close();
            } catch (XMLStreamException xse) {
                logger.log(Level.WARNING, "Error closing stream.", xse);
            }
        }

        if (this.outputWriter != null) {
            TransformerFactory factory;
            Transformer transformer;
            StreamSource source;
            StreamResult result;
            try {
                source = new StreamSource(new StringReader(this.stringWriter
                                                               .toString()));
                result = new StreamResult(this.outputWriter);
                factory = TransformerFactory.newInstance();
                transformer = factory.newTransformer();
                for (int i = 0; i < indentProps.length; i += 2) {
                    transformer.setOutputProperty(indentProps[i],
                                                  indentProps[i+1]);
                }
                transformer.transform(source, result);
            } catch (TransformerException te) {
                logger.log(Level.WARNING, "Transformer fail", te);
            }
            try {
                this.outputWriter.flush();
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "Flush fail", ioe);
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
     * Write a long attribute to the stream.
     *
     * @param attributeName The attribute name.
     * @param value A long to write.
     * @exception XMLStreamException if a write error occurs.
     */
    public void writeAttribute(String attributeName, long value) throws XMLStreamException {
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

    // Delegations to the WriteScope.

    public Player getClientPlayer() {
        return writeScope.getClient();
    }

    //public boolean isValid() {
    //    return (this == WriteScope.CLIENT) == (player != null);
    //}

    public boolean validForSave() {
        return writeScope.validForSave();
    }

    public boolean validFor(Player player) {
        return writeScope.validFor(player);
    }

    // Simple delegations to the XMLStreamWriter.  All should be
    // present here except close which is supplied above.

    @Override
    public void flush() throws XMLStreamException {
        xmlStreamWriter.flush();
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return xmlStreamWriter.getNamespaceContext();
    }

    @Override
    public String getPrefix(String uri) throws XMLStreamException {
        return xmlStreamWriter.getPrefix(uri);
    }

    @Override
    public Object getProperty(String name) {
        return xmlStreamWriter.getProperty(name);
    }

    @Override
    public void setDefaultNamespace(String uri) throws XMLStreamException {
        xmlStreamWriter.setDefaultNamespace(uri);
    }

    @Override
    public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
        xmlStreamWriter.setNamespaceContext(context);
    }

    @Override
    public void setPrefix(String prefix, String uri) throws XMLStreamException {
        xmlStreamWriter.setPrefix(prefix, uri);
    }

    @Override
    public void writeAttribute(String localName, String value) throws XMLStreamException {
        xmlStreamWriter.writeAttribute(localName, value);
    }

    @Override
    public void writeAttribute(String namespaceURI, String localName, String value) throws XMLStreamException {
        xmlStreamWriter.writeAttribute(namespaceURI, localName, value);
    }

    @Override
    public void writeAttribute(String prefix, String namespaceURI, String localName, String value) throws XMLStreamException {
        xmlStreamWriter.writeAttribute(prefix, namespaceURI, localName, value);
    }

    @Override
    public void writeCData(String data) throws XMLStreamException {
        xmlStreamWriter.writeCData(data);
    }

    @Override
    public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
        xmlStreamWriter.writeCharacters(text, start, len);
    }

    @Override
    public void writeCharacters(String text) throws XMLStreamException {
        xmlStreamWriter.writeCharacters(text);
    }

    @Override
    public void writeComment(String data) throws XMLStreamException {
        xmlStreamWriter.writeComment(data);
    }

    @Override
    public void writeDefaultNamespace(String namespaceURI) throws XMLStreamException {
        xmlStreamWriter.writeDefaultNamespace(namespaceURI);
    }

    @Override
    public void writeDTD(String dtd) throws XMLStreamException {
        xmlStreamWriter.writeDTD(dtd);
    }

    @Override
    public void writeEmptyElement(String localName) throws XMLStreamException {
        xmlStreamWriter.writeEmptyElement(localName);
    }

    @Override
    public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
        xmlStreamWriter.writeEmptyElement(namespaceURI, localName);
    }

    @Override
    public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        xmlStreamWriter.writeEmptyElement(prefix, localName, namespaceURI);
    }

    @Override
    public void writeEndDocument() throws XMLStreamException {
        xmlStreamWriter.writeEndDocument();
    }

    @Override
    public void writeEndElement() throws XMLStreamException {
        xmlStreamWriter.writeEndElement();
    }

    @Override
    public void writeEntityRef(String name) throws XMLStreamException {
        xmlStreamWriter.writeEntityRef(name);
    }

    @Override
    public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
        xmlStreamWriter.writeNamespace(prefix, namespaceURI);
    }

    @Override
    public void writeProcessingInstruction(String target) throws XMLStreamException {
        xmlStreamWriter.writeProcessingInstruction(target);
    }

    @Override
    public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
        xmlStreamWriter.writeProcessingInstruction(target, data);
    }

    @Override
    public void writeStartDocument() throws XMLStreamException {
        xmlStreamWriter.writeStartDocument();
    }

    @Override
    public void writeStartDocument(String version) throws XMLStreamException {
        xmlStreamWriter.writeStartDocument(version);
    }

    @Override
    public void writeStartDocument(String encoding, String version) throws XMLStreamException {
        xmlStreamWriter.writeStartDocument(encoding, version);
    }

    @Override
    public void writeStartElement(String localName) throws XMLStreamException {
        xmlStreamWriter.writeStartElement(localName);
    }

    @Override
    public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
        xmlStreamWriter.writeStartElement(namespaceURI, localName);
    }

    @Override
    public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        xmlStreamWriter.writeStartElement(prefix, localName, namespaceURI);
    }
}
