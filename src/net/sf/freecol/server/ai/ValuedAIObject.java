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

package net.sf.freecol.server.ai;

import javax.xml.stream.XMLStreamException;

import java.util.Comparator;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;


/**
 * Abstract class of AI object with a simple enclosed comparable
 * integer value.
 */
public abstract class ValuedAIObject extends AIObject {

    /** A comparator by ascending AI object value. */
    public static final Comparator<? super ValuedAIObject> ascendingValueComparator
        = Comparator.comparingInt(ValuedAIObject::getValue);
    
    /** A comparator by descending AI object value. */
    public static final Comparator<? super ValuedAIObject> descendingValueComparator
        = ascendingValueComparator.reversed();

    /** The value of this AIObject. */
    private int value;


    /**
     * Creates a new {@code ValuedAIObject} instance.
     *
     * @param aiMain an {@code AIMain} value
     */
    public ValuedAIObject(AIMain aiMain) {
        super(aiMain);
    }

    /**
     * Creates a new uninitialized {@code ValuedAIObject} instance.
     *
     * @param aiMain an {@code AIMain} value
     * @param id The object identifier.
     */
    public ValuedAIObject(AIMain aiMain, String id) {
        super(aiMain, id);

        this.value = 0;
    }

    /**
     * Creates a new {@code ValuedAIObject} from the given
     * XML-representation.
     *
     * @param aiMain The main AI-object.
     * @param xr The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public ValuedAIObject(AIMain aiMain,
                          FreeColXMLReader xr) throws XMLStreamException {
        super(aiMain, xr);
    }


    /**
     * Get the value.
     *
     * @return The value.
     */
    public final int getValue() {
        return value;
    }

    /**
     * Set the value.
     *
     * @param newValue The new value.
     */
    public final void setValue(final int newValue) {
        this.value = newValue;
    }


    // Serialization

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
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        setValue(xr.getAttribute(VALUE_TAG, -1));
    }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof ValuedAIObject) {
            ValuedAIObject other = (ValuedAIObject)o;
            return this.value == other.value
                && super.equals(other);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        return 37 * hash + this.value;
    }
}
