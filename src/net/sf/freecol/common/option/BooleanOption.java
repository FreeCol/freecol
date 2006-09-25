
package net.sf.freecol.common.option;

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
* Represents an option that can be either <i>true</i>
* or <i>false</i>.
*/
public class BooleanOption extends AbstractOption {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static Logger logger = Logger.getLogger(BooleanOption.class.getName());


    private boolean value;
    private boolean defaultValue;


    /**
    * Creates a new <code>BooleanOption</code>.
    *
    * @param id The identifier for this option. This is used when the object should be
    *           found in an {@link OptionGroup}.
    * @param name The name of the <code>Option</code>. This text is used for identifying
    *           the option for a user. Example: The text related to a checkbox.
    * @param shortDescription Should give a short description of the <code>BooleanOption</code>.
    *           This might be used as a tooltip text.
    * @param defaultValue The default value of this option.
    */
    public BooleanOption(String id, String name, String shortDescription, boolean defaultValue) {
        super(id, name, shortDescription);

        this.defaultValue = defaultValue;
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
        this.value = value;
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
        value = Boolean.valueOf(in.getAttributeValue(null, "value")).booleanValue();
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
