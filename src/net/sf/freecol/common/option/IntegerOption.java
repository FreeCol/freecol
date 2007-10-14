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
* Represents an option where the valid choice is an integer.
*/
public class IntegerOption extends AbstractOption {
    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger(IntegerOption.class.getName());



    private int value;
    private int minimumValue;
    private int maximumValue;

    

    /**
    * Creates a new <code>IntegerOption</code>.
    *
    * @param id The identifier for this option. This is used when the object should be
    *           found in an {@link OptionGroup}.
    * @param optionGroup The OptionGroup this option belongs to. 
    * @param minimumValue The minimum allowed value.
    * @param maximumValue The maximum allowed value.
    * @param defaultValue The default value of this option.
    */
    public IntegerOption(String id, OptionGroup optionGroup, int minimumValue, int maximumValue, int defaultValue) {
        super(id, optionGroup);

        this.value = defaultValue;
        this.minimumValue = minimumValue;
        this.maximumValue = maximumValue;
    }


    
    /**
    * Returns the minimum allowed value.
    * @return The minimum value allowed by this option.
    */
    public int getMinimumValue() {
        return minimumValue;
    }
    
    
    /**
    * Returns the maximum allowed value.
    * @return The maximum value allowed by this option.
    */
    public int getMaximumValue() {
        return maximumValue;
    }


    /**
    * Gets the current value of this <code>IntegerOption</code>.
    * @return The value.
    */
    public int getValue() {
        return value;
    }

    
    /**
    * Sets the value of this <code>IntegerOption</code>.
    * @param value The value to be set.
    */
    public void setValue(int value) {
        final int oldValue = this.value;
        this.value = value;
        
        if (value != oldValue) {
            firePropertyChange("value", Integer.valueOf(oldValue), Integer.valueOf(value));
        }
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
        out.writeStartElement(getId());

        out.writeAttribute("value", Integer.toString(value));

        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        final int oldValue = this.value;
        
        value = Integer.parseInt(in.getAttributeValue(null, "value"));
        in.nextTag();
        
        if (value != oldValue) {
            firePropertyChange("value", Integer.valueOf(oldValue), Integer.valueOf(value));
        }
    }


    /**
    * Gets the tag name of the root element representing this object.
    * @return "integerOption".
    */
    public static String getXMLElementTagName() {
        return "integerOption";
    }

}
