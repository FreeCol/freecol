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

package net.sf.freecol.common.option;

import java.io.File;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Specification;


/**
 * Represents an option for specifying a <code>File</code>.
 */
public class FileOption extends AbstractOption<File> {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(FileOption.class.getName());

    /** The value of this option. */
    private File value = null;


    /**
     * Creates a new <code>FileOption</code>.
     *
     * @param specification The <code>Specification</code> to refer to.
     */
    public FileOption(Specification specification) {
        super(specification);
    }


    // Interface Option

    /**
     * {@inheritDoc}
     */
    @Override
    public FileOption clone() {
        FileOption result = new FileOption(getSpecification());
        result.setValues(this);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File getValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(File value) {
        final File oldValue = this.value;
        this.value = value;

        if (value != oldValue) {
            firePropertyChange(VALUE_TAG, oldValue, value);
        }
        isDefined = true;
    }


    // Override AbstractOption

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNullValueOK() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setValue(String valueString, String defaultValueString) {
        if (valueString != null) {
            value = new File(valueString);
        } else if (defaultValueString != null) {
            value = new File(defaultValueString);
        } else {
            value = null;
        }
    }


    // Serialization


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        if (value != null) {
            xw.writeAttribute(VALUE_TAG, value.getAbsolutePath());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(16);
        sb.append("[").append(getId())
            .append(" value=").append((value == null) ? "null":value.getName())
            .append("]");
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "fileOption".
     */
    public static String getXMLElementTagName() {
        return "fileOption";
    }
}
