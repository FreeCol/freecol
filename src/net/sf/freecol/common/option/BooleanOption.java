
package net.sf.freecol.common.option;

import java.util.logging.Logger;
import java.util.*;

import org.w3c.dom.*;


/**
* Represents an option that can be either <i>true</i>
* or <i>false</i>.
*/
public class BooleanOption extends Option {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
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
    * Creates an <code>BooleanOption</code> from an XML representation.
    *
    * <br><br>
    *
    * @param element The XML <code>Element</code> from which this object
    *                should be constructed.
    */
    public BooleanOption(Element element) {
        super(element);
        readFromXMLElement(element);
    }


    /**
    * Gets the current value of this <code>BooleanOption</code>.
    */
    public boolean getValue() {
        return value;
    }


    /**
    * Makes an XML-representation of this object.
    *
    * @param document The document to use when creating new componenets.
    * @return The DOM-element ("Document Object Model") made to represent this "BooleanOption".
    */
    public Element toXMLElement(Document document) {
        Element optionElement = document.createElement(getXMLElementTagName());

        super.writeAttributes(optionElement);

        optionElement.setAttribute("value", Boolean.toString(value));
        optionElement.setAttribute("defaultValue", Boolean.toString(defaultValue));

        return optionElement;
    }


    /**
    * Initializes this object from an XML-representation of this object.
    * @param optionElement The DOM-element ("Document Object Model") made to represent this "BooleanOption".
    */
    public void readFromXMLElement(Element optionElement) {
        super.readAttributes(optionElement);

        value = Boolean.valueOf(optionElement.getAttribute("value")).booleanValue();
        defaultValue = Boolean.valueOf(optionElement.getAttribute("defaultValue")).booleanValue();
    }


    /**
    * Gets the tag name of the root element representing this object.
    * @return "booleanOption".
    */
    public static String getXMLElementTagName() {
        return "booleanOption";
    }

}
