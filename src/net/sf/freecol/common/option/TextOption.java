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

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.util.Utils;
import net.sf.freecol.common.model.Specification;


/**
 * Represents an option that can be an arbitrary string.
 */
public class TextOption extends AbstractOption<String> {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(TextOption.class.getName());

    /** The value of this option. */
    private String value;


    /**
     * Creates a new <code>TextOption</code>.
     *
     * @param specification The <code>Specification</code> to refer to.
     */
    public TextOption(Specification specification) {
        super(specification);
    }

    /**
     * Creates a new <code>TextOption</code>.
     *
     * @param id The object identifier.
     * @param specification The <code>Specification</code> to refer to.
     */
    public TextOption(String id, Specification specification) {
        super(id, specification);
    }


    // Interface Option.

    /**
     * {@inheritDoc}
     */
    @Override
    public TextOption clone() {
        TextOption result = new TextOption(getId(), getSpecification());
        result.setValues(this);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(String value) {
        final String oldValue = this.value;
        this.value = value;

        if (isDefined && !Utils.equals(value, oldValue)) {
            firePropertyChange(VALUE_TAG, oldValue, value);
        }
        isDefined = true;
    }


    // Override AbstractOption

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setValue(String valueString, String defaultValueString) {
        setValue((valueString != null) ? valueString : defaultValueString);
    }


    // Serialization

    private static final String CHOICE_TAG = "choice";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(VALUE_TAG, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(32);
        sb.append("[").append(getId())
            .append(" value=").append(value);
        sb.append("]");
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
     * @return "textOption".
     */
    public static String getXMLElementTagName() {
        return "textOption";
    }
}
