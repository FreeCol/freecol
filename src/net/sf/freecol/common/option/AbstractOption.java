
package net.sf.freecol.common.option;

import net.sf.freecol.client.gui.i18n.Messages;

import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;



/**
* The super class of all options.
*/
abstract public class AbstractOption implements Option {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final String NO_ID = "NO_ID";

    private static Logger logger = Logger.getLogger(AbstractOption.class.getName());

    private String id;
    private String name;
    private String shortDescription;


    /**
    * Creates a new <code>AbstractOption</code>.
    *
    * @param id The identifier for this option. This is used when the object should be
    *           found in an {@link OptionGroup}.
    * @param name The name of the <code>AbstractOption</code>. This text is used for identifying
    *           the option for a user. Example: The text related to a checkbox.
    * @param shortDescription Should give a short description of the <code>Option</code>.
    *           This might be used as a tooltip text.
    */
    public AbstractOption(String id, String name, String shortDescription) {
        this.id = id;
        this.name = name;
        this.shortDescription = shortDescription;
    }

    
    
    

    /**
    * Gives a short description of this <code>Option</code>.
    * Can for instance be used as a tooltip text.
    */
    public String getShortDescription() {
        return Messages.message(shortDescription);
    }


    /**
    * Returns a textual representation of this object.
    * @return The name of this <code>Option</code>.
    * @see #getName
    */
    public String toString() {
        return getName();
    }


    /**
    * Returns the id of this <code>Option</code>.
    * @return The unique identifier as provided in the constructor.
    */
    public String getId() {
        return id;
    }


    /**
    * Returns the name of this <code>Option</code>.
    * @return The name as provided in the constructor.
    */
    public String getName() {
        return Messages.message(name);
    }


    /**
    * Makes an XML-representation of this object.
    *
    * @param document The document to use when creating new componenets.
    * @return The DOM-element ("Document Object Model") made to represent this "Option".
    */
    public abstract Element toXMLElement(Document document);


    /**
    * Initializes this object from an XML-representation of this object.
    * @param element The DOM-element ("Document Object Model") made to represent this "Option".
    */
    public abstract void readFromXMLElement(Element element);


    /**
    * Gets the tag name of the root element representing this object.
    * @return "option".
    */
    public static String getXMLElementTagName() {
        return "option";
    }

}
