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
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Specification;


/**
 * Represents an option where the valid choice is an integer.
 */
public class IntegerOption extends AbstractOption<Integer> {

    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger(IntegerOption.class.getName());

    private int value;
    private int minimumValue = Integer.MIN_VALUE;
    private int maximumValue = Integer.MAX_VALUE;

    /**
     * Creates a new <code>IntegerOption</code>.
     *
     * @param id The identifier for this option. This is used when the object
     *            should be found in an {@link OptionGroup}.
     */
    public IntegerOption(String id) {
        super(id);
    }

    /**
     * Creates a new <code>IntegerOption</code>.
     *
     * @param specification The specification this option belongs
     *     to. May be null.
     */
    public IntegerOption(Specification specification) {
        super(specification);
    }

    public IntegerOption clone() {
        IntegerOption result = new IntegerOption(getId());
        result.value = value;
        result.minimumValue = minimumValue;
        result.maximumValue = maximumValue;
        result.isDefined = true;
        return result;
    }


    /**
    * Returns the minimum allowed value.
    * @return The minimum value allowed by this option.
    */
    public int getMinimumValue() {
        return minimumValue;
    }

    /**
     * Sets the minimum allowed value.
     * @param minimumValue The minimum value to set
     */
    public void setMinimumValue(int minimumValue) {
        this.minimumValue = minimumValue;
    }

    /**
    * Returns the maximum allowed value.
    * @return The maximum value allowed by this option.
    */
    public int getMaximumValue() {
        return maximumValue;
    }


    /**
     * Sets the maximum allowed value.
     * @param maximumValue the maximum value to set
     */
    public void setMaximumValue(int maximumValue) {
        this.maximumValue = maximumValue;
    }

    /**
    * Gets the current value of this <code>IntegerOption</code>.
    * @return The value.
    */
    public Integer getValue() {
        return value;
    }


    /**
    * Sets the value of this <code>IntegerOption</code>.
    * @param value The value to be set.
    */
    public void setValue(Integer value) {
        final int oldValue = this.value;
        this.value = value;

        if (value != oldValue && isDefined) {
            firePropertyChange(VALUE_TAG, Integer.valueOf(oldValue), Integer.valueOf(value));
        }
        isDefined = true;
    }


    /**
     * Gets a <code>String</code> representation of the current value.
     *
     * This method can be overwritten by subclasses to allow a custom save
     * value, since this method is used by {@link #toXML(XMLStreamWriter)}.
     *
     * @return The String value of the Integer.
     * @see #setValue(String)
     */
    protected String getStringValue() {
        return Integer.toString(value);
    }

    /**
     * Converts the given <code>String</code> to an Integer and calls
     * {@link #setValue(Integer)}.
     *
     * <br>
     * <br>
     *
     * This method can be overwritten by subclasses to allow a custom save
     * value, since this method is used by {@link #readFromXML(XMLStreamReader)}.
     *
     * @param value The String value of the Integer.
     * @see #getStringValue()
     */
    protected void setValue(String value) {
        setValue(Integer.parseInt(value));
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
    @Override
    protected void setValue(String valueString, String defaultValueString) {
        setValue(Integer.parseInt((valueString != null) ? valueString : defaultValueString));
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
        toXMLImpl(out, getXMLElementTagName());
    }

    protected void toXMLImpl(XMLStreamWriter out, String tag) throws XMLStreamException {
        out.writeStartElement(tag);

        out.writeAttribute(ID_ATTRIBUTE_TAG, getId());
        out.writeAttribute(VALUE_TAG, Integer.toString(value));
        if (minimumValue > Integer.MIN_VALUE) {
            out.writeAttribute("minimumValue", Integer.toString(minimumValue));
        }
        if (maximumValue < Integer.MAX_VALUE) {
            out.writeAttribute("maximumValue", Integer.toString(maximumValue));
        }
        out.writeEndElement();
    }

    /**
     * {@inheritDoc}
     */
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);
        minimumValue = getAttribute(in, "minimumValue", Integer.MIN_VALUE);
        maximumValue = getAttribute(in, "maximumValue", Integer.MAX_VALUE);
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "integerOption".
     */
    public static String getXMLElementTagName() {
        return "integerOption";
    }
}
