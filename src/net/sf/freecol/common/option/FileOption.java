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

package net.sf.freecol.common.option;

import java.io.File;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Specification;


/**
 * Represents an option for specifying a {@code File}.
 */
public class FileOption extends AbstractOption<File> {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(FileOption.class.getName());

    public static final String TAG = "fileOption";

    /** The value of this option. */
    private File value = null;

    /** Extra optional qualifier for what the file is to be used for. */
    private String type = null;


    /**
     * Creates a new {@code FileOption}.
     *
     * @param specification The {@code Specification} to refer to.
     */
    public FileOption(Specification specification) {
        super(specification);
    }


    /**
     * Trivial type accessor.
     *
     * @return The type.
     */
    public String getType() {
        return this.type;
    }

    /**
     * Trivial type mutator.
     *
     * @param type The new type.
     */
    public void setType(String type) {
        this.type = type;
    }


    // Interface Option

    /**
     * {@inheritDoc}
     */
    @Override
    public FileOption cloneOption() {
        FileOption result = new FileOption(getSpecification());
        result.setValues(this);
        result.setType(this.getType());
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

    private static final String TYPE_TAG = "type";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        if (value != null) {
            xw.writeAttribute(VALUE_TAG, value.getAbsolutePath());
        }

        if (type != null) {
            xw.writeAttribute(TYPE_TAG, type);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        type = xr.getAttribute(TYPE_TAG, (String)null);

        // @compat 0.11.6
        // Type attribute added
        if (type == null) {
            setType((value != null && value.getPath()
                    .endsWith("." + FreeCol.FREECOL_MAP_EXTENSION)) ? "map"
                : "save");
        }
        // end @compat 0.11.6
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return TAG; }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(16);
        sb.append('[').append(getId())
            .append(" value=").append((value == null) ? "null":value.getName())
            .append(" type=").append((type == null) ? "null" : type)
            .append(']');
        return sb.toString();
    }
}
