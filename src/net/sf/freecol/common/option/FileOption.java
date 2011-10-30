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

import net.sf.freecol.common.model.Specification;

/**
 * Represents an option for specifying a <code>File</code>.
 */
public class FileOption extends AbstractOption<File> {

    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger(FileOption.class.getName());

    private File value = null;


    /**
     * Creates a new <code>FileOption</code>.
     *
     * @param specification The specification this option belongs
     *     to. May be null.
     */
    public FileOption(Specification specification) {
        super(specification);
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

    protected void setValue(String valueString, String defaultValueString) {
        if (valueString != null) {
            value = new File(valueString);
        } else if (defaultValueString != null) {
            value = new File(defaultValueString);
        } else {
            value = null;
        }
    }

    /**
     * Returns whether <code>null</code> is an acceptable value for
     * this Option. This method always returns <code>true</code>.
     *
     * @return true
     */
    public boolean isNullValueOK() {
        return true;
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
     * Gets the tag name of the root element representing this object.
     *
     * @return "fileOption".
     */
    public static String getXMLElementTagName() {
        return "fileOption";
    }
}
