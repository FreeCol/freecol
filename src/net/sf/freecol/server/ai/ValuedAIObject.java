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


package net.sf.freecol.server.ai;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class ValuedAIObject extends AIObject implements Comparable<ValuedAIObject> {

    /**
     * Describe value here.
     */
    private int value;

    public ValuedAIObject(AIMain aiMain) {
        super(aiMain);
    }

    public ValuedAIObject(AIMain aiMain, String id) {
        super(aiMain, id);
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


    public final int compareTo(ValuedAIObject other) {
        return other.value - this.value;
    }

    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        // do nothing
    }

}