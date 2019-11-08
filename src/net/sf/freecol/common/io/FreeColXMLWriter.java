/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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
import java.nio.charset.StandardCharsets;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Player;
import static net.sf.freecol.common.util.StringUtils.*;
import net.sf.freecol.common.util.Utils;


/**
 * A wrapper for {@code XMLStreamWriter} and potentially an
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

    /** The scope of a FreeCol object write. */
    public static enum WriteScopeType {
        CLIENT,  // Only the client-visible information
        SERVER,  // Full server-visible information
        SAVE     // Absolutely everything needed to save the game state
    };

    public static class WriteScope {

        private final WriteScopeType scopeType;
        
        private final Player player;


        WriteScope(WriteScopeType scopeType, Player player) {
            this.scopeType = scopeType;
            this.player = player;
        }
        
        public static WriteScope toClient(Player player) {
            return new WriteScope(WriteScopeType.CLIENT, player);
        }            

        public static WriteScope toServer() {
            return new WriteScope(WriteScopeType.SERVER, null);
        }

        public static WriteScope toSave() {
            return new WriteScope(WriteScopeType.SAVE, null);
        }

        public boolean isValid() {
            return (this.scopeType == WriteScopeType.CLIENT)
                == (this.player != null);
        }

        public boolean validForSave() {
            return this.scopeType == WriteScopeType.SAVE;
        }

        public boolean validFor(Player player) {
            return this.scopeType != WriteScopeType.CLIENT
                || this.player == player;
        }

        public Player getClient() {
            return this.player;
        }

        public String toString() {
            String ret = this.scopeType.toString();
            if (this.scopeType == WriteScopeType.CLIENT) {
                ret += ":" + ((this.player == null) ? "INVALID"
                    : this.player.getId());
            }
            return ret;
        }
    }


    /** The internal XML writer to write XML to. */
    private final XMLStreamWriter xmlStreamWriter;

    /** An internal writer to accumulate XML into. */
    private final StringWriter stringWriter;

    /** An optional transformer to handle indentation. */
    private final Transformer transformer;

    /** The writer that receives the final output. */
    private final Writer outputWriter;

    /** A write scope to use for FreeCol object writes. */
    private WriteScope writeScope;


    /**
     * Creates a new {@code FreeColXMLWriter}.
     *
     * @param outputStream The {@code OutputStream} to create
     *     an {@code FreeColXMLWriter} for.
     * @param scope The {@code WriteScope} to use for FreeCol
     *     object writes.
     * @param indent If true, produce indented output if supported.
     * @exception IOException if there is a problem while creating the
     *     {@code FreeColXMLWriter}.
     */
    public FreeColXMLWriter(OutputStream outputStream, WriteScope scope,
                            boolean indent) throws IOException {
        this(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8),
             scope, indent);
    }

    /**
     * Creates a new {@code FreeColXMLWriter}.
     *
     * @param writer A {@code Writer} to create an 
     *     {@code FreeColXMLWriter} for.
     * @exception IOException if there is a problem while creating the
     *     {@code FreeColXMLWriter}.
     */
    public FreeColXMLWriter(Writer writer) throws IOException {
        this(writer, WriteScope.toSave());
    }

    /**
     * Creates a new {@code FreeColXMLWriter}.
     *
     * @param writer A {@code Writer} to create an
     *     {@code FreeColXMLWriter} for.
     * @param scope The {@code WriteScope} to use for FreeCol objects.
     * @exception IOException if there is a problem while creating the
     *     {@code FreeColXMLWriter}.
     */
    public FreeColXMLWriter(Writer writer,
                            WriteScope scope) throws IOException {
        this(writer, scope, false);
    }

    /**
     * Creates a new {@code FreeColXMLWriter}.
     *
     * @param writer A {@code Writer} to create an
     *     {@code FreeColXMLWriter} for.
     * @param scope The {@code WriteScope} to use for FreeCol objects.
     * @param indent If true, produce indented output if supported.
     * @exception IOException if there is a problem while creating the
     *     {@code FreeColXMLWriter}.
     */
    public FreeColXMLWriter(Writer writer, WriteScope scope,
                            boolean indent) throws IOException {
        this.outputWriter = writer;
        try {
            this.stringWriter = new StringWriter(1024);
            this.xmlStreamWriter = getFactory()
                .createXMLStreamWriter(this.stringWriter);
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
        this.transformer = (indent) ? Utils.makeTransformer(false, true)
            : null;
        this.writeScope = (scope == null) ? WriteScope.toSave() : scope;
    }


    /**
     * Get the {@code XMLOutputFactory} to create the output stream with.
     *
     * @return An {@code XMLOutputFactory}.
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
     * @param writeScope The new {@code WriteScope}.
     */     
    public void setWriteScope(WriteScope writeScope) {
        this.writeScope = writeScope;
    }

    /**
     * Replace the scope.
     *
     * @param newWriteScope The {@code WriteScope} to push.
     * @return The previous {@code WriteScope}.
     */
    public WriteScope replaceScope(WriteScope newWriteScope) {
        WriteScope ret = this.writeScope;
        this.writeScope = newWriteScope;
        return ret;
    }

    /**
     * Internal flush, returning what was written.
     *
     * @return The internal buffer containing the flushed data.
     * @exception XMLStreamException on stream error.
     */
    public StringBuffer flushBuffer() throws XMLStreamException {
        this.xmlStreamWriter.flush();

        StringBuffer sb = this.stringWriter.getBuffer();
        if (sb.length() > 0) {
            String str = sb.toString();
            if (this.transformer == null) {
                try {
                    this.outputWriter.write(str);
                } catch (IOException ioe) {
                    logger.log(Level.WARNING, "Flush-write fail:" + str, ioe);
                }
            } else {
                StreamSource source = new StreamSource(new StringReader(str));
                StreamResult result = new StreamResult(this.outputWriter);
                try {
                    this.transformer.transform(source, result);
                } catch (TransformerException te) {
                    logger.log(Level.WARNING, "Transform fail:" + str, te);
                }
            }

            try {
                this.outputWriter.flush();
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "Flush fail", ioe);
            }
        }
        return sb;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() throws XMLStreamException {
        // Clear the underlying buffer after flushing it
        flushBuffer().setLength(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        try {
            flush();
        } catch (XMLStreamException xse) {} // Ignore flush fail on close
        
        if (this.xmlStreamWriter != null) {
            try {
                this.xmlStreamWriter.close();
            } catch (XMLStreamException xse) {
                logger.log(Level.WARNING, "Error closing stream.", xse);
            }
            // TODO: Do not set this.xmlStreamWriter = null.  Need to build
            // robustness.
        }
    }


    /**
     * Write a boolean attribute to the stream.
     *
     * @param attributeName The attribute name.
     * @param value A boolean to write.
     * @exception XMLStreamException if a write error occurs.
     */
    public void writeAttribute(String attributeName, boolean value)
        throws XMLStreamException {
        xmlStreamWriter.writeAttribute(attributeName, String.valueOf(value));
    }

    /**
     * Write a float attribute to the stream.
     *
     * @param attributeName The attribute name.
     * @param value A float to write.
     * @exception XMLStreamException if a write error occurs.
     */
    public void writeAttribute(String attributeName, float value)
        throws XMLStreamException {
        xmlStreamWriter.writeAttribute(attributeName, String.valueOf(value));
    }

    /**
     * Write an integer attribute to the stream.
     *
     * @param attributeName The attribute name.
     * @param value An integer to write.
     * @exception XMLStreamException if a write error occurs.
     */
    public void writeAttribute(String attributeName, int value)
        throws XMLStreamException {
        xmlStreamWriter.writeAttribute(attributeName, String.valueOf(value));
    }

    /**
     * Write a long attribute to the stream.
     *
     * @param attributeName The attribute name.
     * @param value A long to write.
     * @exception XMLStreamException if a write error occurs.
     */
    public void writeAttribute(String attributeName, long value)
        throws XMLStreamException {
        xmlStreamWriter.writeAttribute(attributeName, String.valueOf(value));
    }

    /**
     * Write an enum attribute to the stream.
     *
     * @param attributeName The attribute name.
     * @param value The {@code Enum} to write.
     * @exception XMLStreamException if a write error occurs.
     */
    public void writeAttribute(String attributeName, Enum<?> value)
        throws XMLStreamException {
        xmlStreamWriter.writeAttribute(attributeName,
                                       downCase(value.toString()));
    }

    /**
     * Write an Object attribute to the stream.
     *
     * @param attributeName The attribute name.
     * @param value The {@code Object} to write.
     * @exception XMLStreamException if a write error occurs.
     */
    public void writeAttribute(String attributeName, Object value)
        throws XMLStreamException {
        xmlStreamWriter.writeAttribute(attributeName, String.valueOf(value));
    }

    /**
     * Write the identifier attribute of a non-null FreeColObject to the stream.
     *
     * @param attributeName The attribute name.
     * @param value The {@code FreeColObject} to write the identifier of.
     * @exception XMLStreamException if a write error occurs.
     */
    public void writeAttribute(String attributeName, FreeColObject value)
        throws XMLStreamException {
        if (value != null) {
            String id = value.getId();
            if (id != null) {
                xmlStreamWriter.writeAttribute(attributeName, value.getId());
            }
        }
    }

    /**
     * Write the identifier attribute of a non-null Location to the stream.
     *
     * @param attributeName The attribute name.
     * @param value The {@code Location} to write the identifier of.
     * @exception XMLStreamException if a write error occurs.
     */
    public void writeLocationAttribute(String attributeName, Location value)
        throws XMLStreamException {
        writeAttribute(attributeName, (FreeColObject)value);
    }

    /**
     * Writes an XML-representation of a collection object to the given stream.
     *
     * @param <T> The collection type.
     * @param tag The tag for the array.
     * @param members The members of the array.
     * @exception XMLStreamException if a problem was encountered
     *      while writing.
     */
    public <T extends FreeColObject> void writeToListElement(String tag,
        Collection<T> members) throws XMLStreamException {
        if (members.isEmpty()) return;
        
        writeStartElement(tag);

        writeAttribute(FreeColObject.ARRAY_SIZE_TAG, members.size());

        int i = 0;
        for (T t : members) {
            writeAttribute(FreeColObject.arrayKey(i), t);
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
    // present here except close and flush which are supplied above.

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
    public void setNamespaceContext(NamespaceContext context)
        throws XMLStreamException {
        xmlStreamWriter.setNamespaceContext(context);
    }

    @Override
    public void setPrefix(String prefix, String uri) throws XMLStreamException {
        xmlStreamWriter.setPrefix(prefix, uri);
    }

    @Override
    public void writeAttribute(String localName, String value)
        throws XMLStreamException {
        xmlStreamWriter.writeAttribute(localName, value);
    }

    @Override
    public void writeAttribute(String namespaceURI, String localName,
                               String value) throws XMLStreamException {
        xmlStreamWriter.writeAttribute(namespaceURI, localName, value);
    }

    @Override
    public void writeAttribute(String prefix, String namespaceURI,
                               String localName, String value)
        throws XMLStreamException {
        xmlStreamWriter.writeAttribute(prefix, namespaceURI, localName, value);
    }

    @Override
    public void writeCData(String data) throws XMLStreamException {
        xmlStreamWriter.writeCData(data);
    }

    @Override
    public void writeCharacters(char[] text, int start, int len)
        throws XMLStreamException {
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
    public void writeDefaultNamespace(String namespaceURI)
        throws XMLStreamException {
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
    public void writeEmptyElement(String namespaceURI, String localName)
        throws XMLStreamException {
        xmlStreamWriter.writeEmptyElement(namespaceURI, localName);
    }

    @Override
    public void writeEmptyElement(String prefix, String localName,
                                  String namespaceURI)
        throws XMLStreamException {
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
    public void writeNamespace(String prefix, String namespaceURI)
        throws XMLStreamException {
        xmlStreamWriter.writeNamespace(prefix, namespaceURI);
    }

    @Override
    public void writeProcessingInstruction(String target)
        throws XMLStreamException {
        xmlStreamWriter.writeProcessingInstruction(target);
    }

    @Override
    public void writeProcessingInstruction(String target, String data)
        throws XMLStreamException {
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
    public void writeStartDocument(String encoding, String version)
        throws XMLStreamException {
        xmlStreamWriter.writeStartDocument(encoding, version);
    }

    @Override
    public void writeStartElement(String localName) throws XMLStreamException {
        xmlStreamWriter.writeStartElement(localName);
    }

    @Override
    public void writeStartElement(String namespaceURI, String localName)
        throws XMLStreamException {
        xmlStreamWriter.writeStartElement(namespaceURI, localName);
    }

    @Override
    public void writeStartElement(String prefix, String localName,
                                  String namespaceURI)
        throws XMLStreamException {
        xmlStreamWriter.writeStartElement(prefix, localName, namespaceURI);
    }
}
