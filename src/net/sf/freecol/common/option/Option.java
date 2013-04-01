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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


/**
 * An option describes something which can be customized by the user.
 *
 * @see net.sf.freecol.common.model.GameOptions
 */
public interface Option<T> extends Cloneable {

    /**
     * {@inheritDoc}
     */
    public Option<T> clone() throws CloneNotSupportedException;

    /**
     * Gets the id of this option.
     *
     * @return The unique identifier as provided in the constructor.
     */
    public String getId();

    /**
     * Gets the value of this option.
     *
     * @return The value of this <code>Option</code>.
     */
    public T getValue();

    /**
     * Sets the value of this option.
     *
     * @param value The new value of this <code>Option</code>.
     */
    public void setValue(T value);

    /**
     * Initializes this object from an XML-representation of this object.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException if there are any problems reading the stream.
     */
    public void readFromXML(XMLStreamReader in) throws XMLStreamException;

    /**
     * Makes an XML-representation of this object.
     *
     * @param out The output stream.
     * @throws XMLStreamException if there are any problems writing the stream.
     */
    public void toXML(XMLStreamWriter out) throws XMLStreamException;

    /**
     * Gets a textual representation of this object.
     *
     * @return The name of this <code>Option</code>.
     */
    public String toString();
}
