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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Specification;


/**
 * Represents an option where the valid choice is an integer.
 */
public class IntegerOption extends AbstractOption<Integer> {

    private static final Logger logger = Logger.getLogger(IntegerOption.class.getName());

    /** The value of this option. */
    private int value;

    /** A upper bound on the value of this option. */
    private int maximumValue = Integer.MAX_VALUE;

    /** A lower bound on the value of this option. */
    private int minimumValue = Integer.MIN_VALUE;


    /**
     * Creates a new <code>IntegerOption</code>.
     *
     * @param specification The <code>Specification</code> to refer to.
     */
    public IntegerOption(Specification specification) {
        super(specification);
    }

    /**
     * Creates a new <code>IntegerOption</code>.
     *
     * @param id The object identifier.
     * @param specification The <code>Specification</code> to refer to.
     */
    public IntegerOption(String id, Specification specification) {
        super(id, specification);
    }


    /**
     * Get the minimum allowed value.
     *
     * @return The minimum value allowed by this option.
     */
    public int getMinimumValue() {
        return minimumValue;
    }

    /**
     * Set the minimum allowed value.
     *
     * @param minimumValue The new minimum value.
     */
    public void setMinimumValue(int minimumValue) {
        this.minimumValue = minimumValue;
    }

    /**
     * Get the maximum allowed value.
     *
     * @return The maximum value allowed by this option.
     */
    public int getMaximumValue() {
        return maximumValue;
    }

    /**
     * Set the maximum allowed value.
     *
     * @param maximumValue The new maximum value.
     */
    public void setMaximumValue(int maximumValue) {
        this.maximumValue = maximumValue;
    }

    /**
     * Limit a value with respect to the limits of this option.
     *
     * @param value The value to limit.
     * @return The value limited by the option limits.
     */
    public int limitValue(int value) {
        return Math.min(Math.max(value, this.minimumValue), this.maximumValue);
    }


    // Interface Option

    /**
     * {@inheritDoc}
     */
    @Override
    public IntegerOption clone() {
        IntegerOption result = new IntegerOption(getId(), getSpecification());
        result.setValues(this);
        result.minimumValue = minimumValue;
        result.maximumValue = maximumValue;
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(Integer value) {
        final int oldValue = this.value;
        this.value = limitValue(value);

        if (value != oldValue && isDefined) {
            firePropertyChange(VALUE_TAG, oldValue, (int)value);
        }
        isDefined = true;
    }

    // Override AbstractOption

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setValue(String valueString, String defaultValueString) {
        String str = (valueString != null) ? valueString : defaultValueString;
        try {
            setValue(Integer.valueOf(str));
        } catch (NumberFormatException nfe) {
            logger.log(Level.WARNING, "IntegerOption fail: " + str, nfe);
        }
    }


    // Serialization

    private static final String MAXIMUM_VALUE_TAG = "maximumValue";
    private static final String MINIMUM_VALUE_TAG = "minimumValue";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(VALUE_TAG, value);

        if (maximumValue < Integer.MAX_VALUE) {
            xw.writeAttribute(MAXIMUM_VALUE_TAG, maximumValue);
        }

        if (minimumValue > Integer.MIN_VALUE) {
            xw.writeAttribute(MINIMUM_VALUE_TAG, minimumValue);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        maximumValue = xr.getAttribute(MAXIMUM_VALUE_TAG, Integer.MAX_VALUE);

        minimumValue = xr.getAttribute(MINIMUM_VALUE_TAG, Integer.MIN_VALUE);

        this.value = limitValue(this.value);
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
     * @return "integerOption".
     */
    public static String getXMLElementTagName() {
        return "integerOption";
    }
}
