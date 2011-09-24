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


import java.beans.PropertyChangeListener;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * An option describes something which can be customized by the user.
 *
 * @see net.sf.freecol.common.model.GameOptions
 */
public interface Option<T> {

    /**
    * Returns a textual representation of this object.
    * @return The name of this <code>Option</code>.
    */
    public String toString() ;


    /**
    * Returns the id of this <code>Option</code>.
    * @return The unique identifier as provided in the constructor.
    */
    public String getId();

    /**
     * Returns the value of this Option.
     *
     * @return the value of this Option
     */
    public T getValue();

    /**
    * Makes an XML-representation of this object.
    *
    * @param document The document to use when creating new componenets.
    * @return The DOM-element ("Document Object Model") made to represent this "Option".
    */
    public Element toXMLElement(Document document);


    /**
    * Initializes this object from an XML-representation of this object.
    * @param element The DOM-element ("Document Object Model") made to represent this "Option".
    */
    public void readFromXMLElement(Element element);

    /**
     * Initializes this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    public void readFromXML(XMLStreamReader in) throws XMLStreamException;

    /**
     * Makes an XML-representation of this object.
     *
     * @param out The output stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    public void toXML(XMLStreamWriter out) throws XMLStreamException;
}
