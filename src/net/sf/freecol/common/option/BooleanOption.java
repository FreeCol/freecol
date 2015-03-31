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
import net.sf.freecol.common.model.Specification;


/**
 * Represents an option that can be either <i>true</i>
 * or <i>false</i>.
 */
public class BooleanOption extends AbstractOption<Boolean> {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(BooleanOption.class.getName());

    /** The value of this option. */
    private boolean value;


    /**
     * Creates a new <code>BooleanOption</code>.
     *
     * @param specification The <code>Specification</code> to refer to.
     */
    public BooleanOption(Specification specification) {
        super(specification);
    }

    /**
     * Creates a new <code>BooleanOption</code>.
     *
     * @param id The object identifier.
     * @param specification The <code>Specification</code> to refer to.
     */
    public BooleanOption(String id, Specification specification) {
        super(id, specification);
    }


    // Interface Option

    /**
     * {@inheritDoc}
     */
    @Override
    public BooleanOption clone() {
        BooleanOption result = new BooleanOption(getId(), getSpecification());
        result.setValues(this);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean getValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(Boolean value) {
        final boolean oldValue = this.value;
        this.value = value;

        if (value != oldValue && isDefined) {
            firePropertyChange(VALUE_TAG, oldValue, (boolean)value);
        }
        isDefined = true;
    }


    // Override AbstractOption

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setValue(String valueString, String defaultValueString) {
        setValue(Boolean.valueOf((valueString != null) ? valueString
                : defaultValueString));
    }


    // Serialization


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(VALUE_TAG, Boolean.toString(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(16);
        sb.append("[").append(getId())
            .append(" value=").append(value)
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
     * @return "booleanOption".
     */
    public static String getXMLElementTagName() {
        return "booleanOption";
    }
}
