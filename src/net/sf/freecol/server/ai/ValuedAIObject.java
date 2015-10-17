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

package net.sf.freecol.server.ai;

import javax.xml.stream.XMLStreamException;

import java.util.Comparator;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.FreeColObject;

import org.w3c.dom.Element;


/**
 * Abstract class of AI object with a simple enclosed comparable
 * integer value.
 */
public abstract class ValuedAIObject extends AIObject {

    /** A comparator by descending AI object value. */
    public static final Comparator<ValuedAIObject> valuedComparator
        = Comparator.comparingInt(ValuedAIObject::getValue).reversed();

    /** The value of this AIObject. */
    private int value;


    /**
     * Creates a new <code>ValuedAIObject</code> instance.
     *
     * @param aiMain an <code>AIMain</code> value
     */
    public ValuedAIObject(AIMain aiMain) {
        super(aiMain);
    }

    /**
     * Creates a new uninitialized <code>ValuedAIObject</code> instance.
     *
     * @param aiMain an <code>AIMain</code> value
     * @param id The object identifier.
     */
    public ValuedAIObject(AIMain aiMain, String id) {
        super(aiMain, id);

        this.value = 0;
    }

    /**
     * Creates a new <code>ValuedAIObject</code> from the given
     * XML-representation.
     *
     * @param aiMain The main AI-object.
     * @param element The root element for the XML-representation 
     *     of the object.
     */
    public ValuedAIObject(AIMain aiMain, Element element) {
        super(aiMain, element);
    }
    
    /**
     * Creates a new <code>ValuedAIObject</code> from the given
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
     * Get the <code>Value</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getValue() {
        return value;
    }

    /**
     * Set the <code>Value</code> value.
     *
     * @param newValue The new Value value.
     */
    public final void setValue(final int newValue) {
        this.value = newValue;
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(FreeColObject other) {
        int cmp = 0;
        if (other instanceof ValuedAIObject) {
            ValuedAIObject vao = (ValuedAIObject)other;
            cmp = vao.value - this.value;
        }
        if (cmp == 0) cmp = super.compareTo(other);
        return cmp;
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
}
