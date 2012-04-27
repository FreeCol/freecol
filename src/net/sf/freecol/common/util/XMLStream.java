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
import java.util.logging.Level;
import java.util.logging.Logger;

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
     *      a <code>XMLStreamReader</code> for.
     * @throws IOException if thrown while creating the
     *      <code>XMLStreamReader</code>.
     */
    public XMLStream(InputStream inputStream) throws IOException {
        this.inputStream = inputStream;
        this.xmlStreamReader = createXMLStreamReader(inputStream);
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
        try {
            xmlStreamReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            inputStream.close();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Close input stream fail", e);
        }
    }

    private XMLStreamReader createXMLStreamReader(InputStream inputStream)
        throws IOException{
        try {
            XMLInputFactory xif = XMLInputFactory.newInstance();
            return xif.createXMLStreamReader(inputStream, "UTF-8");
        } catch (XMLStreamException e) {
            throw new IOException("XMLStreamException: " + e.getMessage());
        } catch (NullPointerException e) {
            throw new NullPointerException("NullPointerException: "
                + e.getMessage());
        }
    }
}
