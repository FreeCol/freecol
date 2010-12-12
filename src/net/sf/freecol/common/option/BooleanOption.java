/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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
     * @param in The <code>XMLStreamReader</code> containing the data.
     * @exception XMLStreamException if an error occurs
     */
    public BooleanOption(XMLStreamReader in) throws XMLStreamException {
        super(NO_ID);
        readFromXML(in);
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
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute(ID_ATTRIBUTE_TAG, getId());
        out.writeAttribute(VALUE_TAG, Boolean.toString(value));

        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        final String id = in.getAttributeValue(null, ID_ATTRIBUTE_TAG);
        final String defaultValue = in.getAttributeValue(null, "defaultValue");
        final String value = in.getAttributeValue(null, VALUE_TAG);

        if (id == null && getId().equals("NO_ID")){
            throw new XMLStreamException("invalid <" + getXMLElementTagName() + "> tag : no id attribute found.");
        }
        if (defaultValue == null && value == null) {
            throw new XMLStreamException("invalid <" + getXMLElementTagName() + "> tag : no value nor default value found.");
        }

        if(getId() == NO_ID) {
            setId(id);
        }
        if(value != null) {
            setValue(Boolean.parseBoolean(value));
        } else {
            setValue(Boolean.parseBoolean(defaultValue));
        }
        in.nextTag();

    }


    /**
     * Gets the tag name of the root element representing this object.
     * @return "booleanOption".
     */
    public static String getXMLElementTagName() {
        return "booleanOption";
    }

}
