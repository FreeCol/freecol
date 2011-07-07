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

package net.sf.freecol.common.option;

import java.io.File;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


/**
 * Represents an option for specifying a <code>File</code>.
 */
public class FileOption extends AbstractOption<File> {

    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger(FileOption.class.getName());

    private File value = null;


    /**
     * Creates a new <code>IntegerOption</code>.
     *
     * @param in The <code>XMLStreamReader</code> containing the data.
     * @exception XMLStreamException if an error occurs
     */
    public FileOption(XMLStreamReader in) throws XMLStreamException {
        super(NO_ID);
        readFromXML(in);
    }

    /**
     * Gets the current value of this <code>FileOption</code>.
     *
     * @return The value using <code>null</code> for marking no value.
     */
    public File getValue() {
        return value;
    }

    /**
     * Sets the value of this <code>FileOption</code>.
     *
     * @param value The value to be set.
     */
    public void setValue(File value) {
        final File oldValue = this.value;
        this.value = value;

        if (value != oldValue) {
            firePropertyChange(VALUE_TAG, oldValue, value);
        }
        isDefined = true;
    }


    /**
     * This method writes an XML-representation of this object to the given
     * stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    /**
     * Write the attributes of this object to a stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing to
     *     the stream.
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeAttributes(out);

        if (value != null) {
            out.writeAttribute(VALUE_TAG, value.getAbsolutePath());
        }
    }

    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in)
        throws XMLStreamException {
        final String id = in.getAttributeValue(null, ID_ATTRIBUTE_TAG);
        if (id == null && getId().equals(NO_ID)){
            throw new XMLStreamException("invalid <" + getXMLElementTagName() + "> tag : no id attribute found.");
        }

        if(getId() == NO_ID) {
            setId(id);
        }
        if (in.getAttributeValue(null, VALUE_TAG) != null && !in.getAttributeValue(null, VALUE_TAG).equals("")) {
            setValue(new File(in.getAttributeValue(null, VALUE_TAG)));
        }
        in.nextTag();
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "fileOption".
     */
    public static String getXMLElementTagName() {
        return "fileOption";
    }
}
