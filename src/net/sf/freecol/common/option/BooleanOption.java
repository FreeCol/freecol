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

    private boolean value;

    /**
     * Creates a new <code>BooleanOption</code>.
     *
     * @param id The identifier for this option. This is used when the object
     *            should be found in an {@link OptionGroup}.
     */
    public BooleanOption(String id) {
        super(id);
    }

    /**
     * Creates a new <code>BooleanOption</code>.
     *
     * @param specification The specification this option belongs
     *     to. May be null.
     */
    public BooleanOption(Specification specification) {
        super(specification);
    }

    public BooleanOption clone() {
        BooleanOption result = new BooleanOption(getId());
        result.setValues(this);
        return result;
    }

    /**
     * Gets the current value of this <code>BooleanOption</code>.
     * @return The value.
     */
    public Boolean getValue() {
        return value;
    }


    /**
     * Sets the current value of this <code>BooleanOption</code>.
     * @param value The value.
     */
    public void setValue(Boolean value) {
        final boolean oldValue = this.value;
        this.value = value;

        if (value != oldValue && isDefined) {
            firePropertyChange(VALUE_TAG, Boolean.valueOf(oldValue), Boolean.valueOf(value));
        }
        isDefined = true;
    }

    /**
     * Sets the value of this Option from the given string
     * representation. Both parameters must not be null at the same
     * time.
     *
     * @param valueString the string representation of the value of
     * this Option
     * @param defaultValueString the string representation of the
     * default value of this Option
     */
    protected void setValue(String valueString, String defaultValueString) {
        setValue(Boolean.parseBoolean((valueString != null) ? valueString : defaultValueString));
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
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
