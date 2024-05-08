/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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

import net.sf.freecol.common.ObjectWithId;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;


/**
 * An option describes something which can be customized by the user.
 */
public interface Option<T> extends Cloneable, ObjectWithId {

    /**
     * Clone this option.
     *
     * @return A clone of this option.
     * @exception CloneNotSupportedException if we can not clone.
     */
    Option<T> cloneOption() throws CloneNotSupportedException;

    /**
     * Gets the option group identifier for this option.
     *
     * @return The option group identifier.
     */
    String getGroup();

    /**
     * Set the option group for this option.
     *
     * @param group The identifier for the option group.
     */
    void setGroup(String group);

    /**
     * Gets the value of this option.
     *
     * @return The value of this {@code Option}.
     */
    T getValue();

    /**
     * Sets the value of this option.
     *
     * @param value The new value of this {@code Option}.
     */
    void setValue(T value);
    
    /**
     * Gets the key of an {@code BooleanOption} that must be true for this
     * option to be enabled.
     * 
     * @return The key of the {@code BooleanOption} that needs to be enabled.
     */
    String getEnabledBy();

    /**
     * Initializes this object from an XML-representation of this object.
     *
     * @param xr The input stream with the XML.
     * @throws XMLStreamException if there are any problems reading the stream.
     */
    void readFromXML(FreeColXMLReader xr) throws XMLStreamException;

    /**
     * Makes an XML-representation of this object.
     *
     * @param xw The {@code FreeColXMLWriter} to write to.
     * @throws XMLStreamException if there are any problems writing the stream.
     */
    void toXML(FreeColXMLWriter xw) throws XMLStreamException;

    /**
     * {@inheritDoc}
     */
    @Override
    String toString();
}
