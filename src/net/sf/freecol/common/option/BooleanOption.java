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

package net.sf.freecol.common.option;

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Specification;


/**
 * Represents an option that can be either <i>true</i>
 * or <i>false</i>.
 */
public class BooleanOption extends AbstractOption<Boolean> {

    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger(BooleanOption.class.getName());

    /** The value of this option. */
    private boolean value;


    /**
     * Creates a new <code>BooleanOption</code>.
     *
     * @param id The identifier for this option.  This is used when
     *     the object should be found in an {@link OptionGroup}.
     */
    public BooleanOption(String id) {
        super(id);
    }

    /**
     * Creates a new <code>BooleanOption</code>.
     *
     * @param specification The enclosing <code>Specification</code>.
     */
    public BooleanOption(Specification specification) {
        super(specification);
    }


    // Interface Option

    /**
     * {@inheritDoc}
     */
    public BooleanOption clone() {
        BooleanOption result = new BooleanOption(getId());
        result.setValues(this);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Boolean getValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(Boolean value) {
        final boolean oldValue = this.value;
        this.value = value;

        if (value != oldValue && isDefined) {
            firePropertyChange(VALUE_TAG, Boolean.valueOf(oldValue),
                Boolean.valueOf(value));
        }
        isDefined = true;
    }


    // Override AbstractOption

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setValue(String valueString, String defaultValueString) {
        setValue(Boolean.parseBoolean((valueString != null) ? valueString
                : defaultValueString));
    }


    // Serialization

    /**
     * {@inheritDoc}
     */
    @Override
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);

        out.writeAttribute(VALUE_TAG, Boolean.toString(value));
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "booleanOption".
     */
    public static String getXMLElementTagName() {
        return "booleanOption";
    }
}
