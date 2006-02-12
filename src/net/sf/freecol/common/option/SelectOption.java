
package net.sf.freecol.common.option;

import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
* Represents an option where the valid choice is an integer.
*/
public class SelectOption extends AbstractOption {
    private static Logger logger = Logger.getLogger(SelectOption.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


    private int value;
    private String[] options;

    /**
    * Creates a new <code>SelectOption</code>.
    *
    * @param id The identifier for this option. This is used when the object should be
    *           found in an {@link OptionGroup}.
    * @param name The name of the <code>Option</code>. This text is used for identifying
    *           the option for a user. Example: The text related to a checkbox.
    * @param shortDescription Should give a short description of the <code>SelectOption</code>.
    *           This might be used as a tooltip text.
    * @param options All possible values.
    * @param defaultOption The index of the default value.
    */
    public SelectOption(String id, String name, String shortDescription, String[] options, int defaultOption) {
        super(id, name, shortDescription);

        this.options = options;
        this.value = defaultOption;
    }

    /**
    * Gets the current value of this <code>SelectOption</code>.
    * @return The value.
    */
    public int getValue() {
        return value;
    }

    
    /**
    * Sets the value of this <code>SelectOption</code>.
    * @param value The value to be set.
    */
    public void setValue(int value) {
        this.value = value;
    }

    /**
    * Gets the current options of this <code>SelectOption</code>.
    * @return The options.
    */
    public String[] getOptions() {
        return options;
    }

    
    /**
    * Sets the options of this <code>SelectOption</code>.
    * @param options The options to be set.
    */
    public void setOptions(String[] options) {
        this.options = options;
    }


    /**
    * Makes an XML-representation of this object.
    *
    * @param document The document to use when creating new componenets.
    * @return The DOM-element ("Document Object Model") made to represent this "SelectOption".
    */
    public Element toXMLElement(Document document) {
        Element optionElement = document.createElement(getId());

        optionElement.setAttribute("value", Integer.toString(value));

        return optionElement;
    }


    /**
    * Initializes this object from an XML-representation of this object.
    * @param optionElement The DOM-element ("Document Object Model") made to represent this "SelectOption".
    */
    public void readFromXMLElement(Element optionElement) {
        value = Integer.parseInt(optionElement.getAttribute("value"));
    }


    /**
    * Gets the tag name of the root element representing this object.
    * @return "integerOption".
    */
    public static String getXMLElementTagName() {
        return "selectOption";
    }

}
