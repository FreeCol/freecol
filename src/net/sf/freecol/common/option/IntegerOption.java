
package net.sf.freecol.common.option;

import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
* Represents an option where the valid choice is an integer.
*/
public class IntegerOption extends AbstractOption {
    private static Logger logger = Logger.getLogger(IntegerOption.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


    private int value;
    private int defaultValue;
    private int minimumValue;
    private int maximumValue;

    

    /**
    * Creates a new <code>IntegerOption</code>.
    *
    * @param id The identifier for this option. This is used when the object should be
    *           found in an {@link OptionGroup}.
    * @param name The name of the <code>Option</code>. This text is used for identifying
    *           the option for a user. Example: The text related to a checkbox.
    * @param shortDescription Should give a short description of the <code>IntegerOption</code>.
    *           This might be used as a tooltip text.
    * @param minimumValue The minimum allowed value.
    * @param maximumValue The maximum allowed value.
    * @param defaultValue The default value of this option.
    */
    public IntegerOption(String id, String name, String shortDescription, int minimumValue, int maximumValue, int defaultValue) {
        super(id, name, shortDescription);

        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.minimumValue = minimumValue;
        this.maximumValue = maximumValue;
    }


    
    /**
    * Returns the minimum allowed value.
    */
    public int getMinimumValue() {
        return minimumValue;
    }
    
    
    /**
    * Returns the maximum allowed value.
    */
    public int getMaximumValue() {
        return maximumValue;
    }


    /**
    * Gets the current value of this <code>IntegerOption</code>.
    */
    public int getValue() {
        return value;
    }

    
    /**
    * Sets the value of this <code>IntegerOption</code>.
    */
    public void setValue(int value) {
        this.value = value;
    }


    /**
    * Makes an XML-representation of this object.
    *
    * @param document The document to use when creating new componenets.
    * @return The DOM-element ("Document Object Model") made to represent this "IntegerOption".
    */
    public Element toXMLElement(Document document) {
        Element optionElement = document.createElement(getId());

        optionElement.setAttribute("value", Integer.toString(value));

        return optionElement;
    }


    /**
    * Initializes this object from an XML-representation of this object.
    * @param optionElement The DOM-element ("Document Object Model") made to represent this "IntegerOption".
    */
    public void readFromXMLElement(Element optionElement) {
        value = Integer.parseInt(optionElement.getAttribute("value"));
    }


    /**
    * Gets the tag name of the root element representing this object.
    * @return "integerOption".
    */
    public static String getXMLElementTagName() {
        return "integerOption";
    }

}
