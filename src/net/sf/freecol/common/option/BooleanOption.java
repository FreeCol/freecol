
package net.sf.freecol.common.option;

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


/**
* Represents an option that can be either <i>true</i>
* or <i>false</i>.
*/
public class BooleanOption extends AbstractOption {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger(BooleanOption.class.getName());


    private boolean value;


    /**
    * Creates a new <code>BooleanOption</code>.
    *
    * @param id The identifier for this option. This is used when the object should be
    *           found in an {@link OptionGroup}.
    * @param defaultValue The default value of this option.
    */
    public BooleanOption(String id, boolean defaultValue) {
        super(id);
        this.value = defaultValue;
    }

    /**
    * Creates a new <code>BooleanOption</code>.
    *
    * @param id The identifier for this option. This is used when the object should be
    *           found in an {@link OptionGroup}.
    * @param optionGroup the OptionGroup this option belongs to.
    * @param defaultValue The default value of this option.
    */
    public BooleanOption(String id, OptionGroup optionGroup, boolean defaultValue) {
        super(id, optionGroup);
        this.value = defaultValue;
    }



    /**
    * Gets the current value of this <code>BooleanOption</code>.
    * @return The value.
    */
    public boolean getValue() {
        return value;
    }
    
    
    /**
    * Sets the current value of this <code>BooleanOption</code>.
    * @param value The value.
    */
    public void setValue(boolean value) {
        final boolean oldValue = this.value;
        this.value = value;
        
        if (value != oldValue) {
            firePropertyChange("value", Boolean.valueOf(oldValue), Boolean.valueOf(value));
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

        out.writeAttribute("value", Boolean.toString(value));

        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        final boolean oldValue = value;
        value = Boolean.valueOf(in.getAttributeValue(null, "value")).booleanValue();
        in.nextTag();
        
        if (value != oldValue) {
            firePropertyChange("value", Boolean.valueOf(oldValue), Boolean.valueOf(value));
        }
    }


    /**
    * Gets the tag name of the root element representing this object.
    * @return "booleanOption".
    */
    public static String getXMLElementTagName() {
        return "booleanOption";
    }

}
