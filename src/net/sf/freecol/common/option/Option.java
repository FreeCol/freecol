
package net.sf.freecol.common.option;

import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;



/**
* The super class of all options.
*/
abstract public class Option {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final String NO_ID = "NO_ID";

    private static Logger logger = Logger.getLogger(Option.class.getName());

    private String id;
    private String name;
    private String shortDescription;
    

    /**
    * Creates a new <code>Option</code>.
    * @param id The identifier for this option. This is used when the object should be
    *           found in an {@link OptionGroup}.
    * @param name The name of the <code>Option</code>. This text is used for identifying
    *           the option for a user. Example: The text related to a checkbox.
    * @param shortDescription Should give a short description of the <code>Option</code>.
    *           This might be used as a tooltip text.
    */
    public Option(String id, String name, String shortDescription) {
        this.id = id;
        this.name = name;
        this.shortDescription = shortDescription;
    }


    /**
    * Creates an <code>Option</code> from an XML representation.
    *
    * <br><br>
    *
    * This constructor should be used by subclasses that which to provide
    * this functionality.
    *
    * @param element The XML <code>Element</code> from which this object
    *                should be constructed.
    */
    public Option(Element element) {

    }


    /**
    * Returns a textual representation of this object.
    * @return The name of this <code>Option</code>.
    * @see #getName
    */
    public String toString() {
        return name;
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
        return name;
    }


    /**
    * Writes the attributes and add the child nodes for this <code>Option</code>.
    * Should be called by subclasses from their own <code>writeAttributes</code>
    * in order to keep the superclass' values.
    *
    * @param element The XML <code>Element</code> in which the attributes and
    *                child nodes should be added.
    */
    protected void writeAttributes(Element element) {
        element.setAttribute("id", id);
        element.setAttribute("name", name);
        element.setAttribute("shortDescription", shortDescription);
    }


    /**
    * Reads the attributes and child nodes from the given <code>Element</code>
    * and uses this information to update this object.
    * Should be called by subclasses for their own <code>readAttributes</code>
    * in order to read the superclass' values.
    *
    * @param element The XML <code>Element</code> in which the attributes and
    *                child nodes should be read from.
    */
    protected void readAttributes(Element element) {
        id = element.getAttribute("id");
        name = element.getAttribute("name");
        shortDescription = element.getAttribute("shortDescription");
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
