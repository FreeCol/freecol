
package net.sf.freecol.common.option;

import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
* Used for grouping objects of {@link Option}.
*/
public abstract class OptionMap extends OptionGroup {
    private static Logger logger = Logger.getLogger(OptionMap.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


    private String xmlTagName;
    private HashMap values;
   


    /**
    * Creates a new <code>OptionMap</code>.
    *
    * @param xmlTagName The tag name that should be used for the parent XML-element
    *           returned by {@link #toXMLElement}.
    * @param name The name of the <code>Option</code>. This text is used for identifying
    *           the option for a user. Example: The text related to a checkbox.
    * @param shortDescription Should give a short description of the <code>OptionGroup</code>.
    *           This might be used as a tooltip text.
    */
    public OptionMap(String xmlTagName, String name, String shortDescription) {
        super(name, shortDescription);
        this.xmlTagName = xmlTagName;
        
        values = new HashMap();

        addDefaultOptions();
        addToMap(this);
    }


    /**
    * Creates an <code>OptionMap</code> from an XML representation.
    *
    * <br><br>
    *
    * @param element The XML <code>Element</code> from which this object
    *                should be constructed.
    * @param xmlTagName The tag name that should be used for the parent XML-element
    *           returned by {@link #toXMLElement}.
    * @param name The name of the <code>Option</code>. This text is used for identifying
    *           the option for a user. Example: The text related to a checkbox.
    * @param shortDescription Should give a short description of the <code>OptionGroup</code>.
    *           This might be used as a tooltip text.
    */
    public OptionMap(Element element, String xmlTagName, String name, String shortDescription) {
        this(xmlTagName, name, shortDescription);
        readFromXMLElement(element);
    }



    /**
    * Adds the default options to this <code>OptionMap</code>.
    * Needs to be implemented by subclasses.
    */
    protected abstract void addDefaultOptions();


    /**
    * Gets the object identified by the given <code>id</code>.
    */
    public Option getObject(String id) {
        return (Option) values.get(id);
    }


    /**
    * Gets the integer value of an option.
    *
    * @param id The id of the option.
    * @return The value.
    * @exception IllegalArgumentException If there is no integer
    *            value associated with the specified option.
    * @exception NullPointerException if the given <code>Option</code> does not exist.
    */
    public int getInteger(String id) {
        try {
            return ((IntegerOption) values.get(id)).getValue();
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("No integer value associated with the specified option.");
        }
    }


    /**
    * Gets the boolean value of an option.
    *
    * @param id The id of the option.
    * @return The value.
    * @exception IllegalArgumentException If there is no boolean
    *            value associated with the specified option.
    * @exception NullPointerException if the given <code>Option</code> does not exist.
    */
    public boolean getBoolean(String id) {
        try {
            return ((BooleanOption) values.get(id)).getValue();
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("No boolean value associated with the specified option.");
        }
    }


    /**
    * Adds the <code>Option</code>s from the given <code>OptionGroup</code>
    * to the <code>Map</code>. This is done recursively if the specified
    * group has any sub-groups.
    */
    public void addToMap(OptionGroup og) {
        Iterator it = og.iterator();
        while (it.hasNext()) {
            Option option = (Option) it.next();
            if (option instanceof OptionGroup) {
                addToMap((OptionGroup) option);
            } else {
                values.put(option.getId(), option);
            }
        }
    }


    /**
    * Makes an XML-representation of this object.
    *
    * @param document The document to use when creating new componenets.
    * @return The DOM-element ("Document Object Model") made to represent this "GameOptions".
    */
    public Element toXMLElement(Document document) {
        Element gameOptionsElement = document.createElement(xmlTagName);

        Iterator it = values.values().iterator();
        while (it.hasNext()) {
            gameOptionsElement.appendChild(((Option) it.next()).toXMLElement(document));
        }

        return gameOptionsElement;
    }


    /**
    * Initializes this object from an XML-representation of this object.
    * @param gameOptionsElement The DOM-element ("Document Object Model")
    *        made to represent this "GameOptions".
    */
    public void readFromXMLElement(Element gameOptionsElement) {
        updateFromElement(gameOptionsElement);
    }


    /**
    * Updates this <code>OptionMap</code> from the given XML-representation.
    * @param element The base <code>Element</code> of this object's 
    *                XML-representation.
    */
    private void updateFromElement(Element element) {
        NodeList nl = element.getChildNodes();
        for (int i=0; i<nl.getLength(); i++) {
            Node n = nl.item(i);
            if (!(n instanceof Element)) {
                continue;
            }
            Element optionElement = (Element) n;

            if (optionElement.getTagName().equals(OptionGroup.getXMLElementTagName())) {
                updateFromElement(optionElement);
            } else {
                if (optionElement.hasAttribute("id")) {
                    // old protocols:
                    Option o = getObject(optionElement.getAttribute("id"));

                    if (o != null) {
                        o.readFromXMLElement(optionElement);
                    } else {
                        // Normal only if this option is from an old save game:
                        logger.info("Option \"" + optionElement.getAttribute("id") + "\" (" + optionElement.getTagName() + ") could not be found.");
                    }
                } else {
                    Option o = getObject(optionElement.getTagName());
                    if (o != null) {
                        o.readFromXMLElement(optionElement);
                    } else {
                        // Normal only if this option is from an old save game:
                        logger.info("Option \"" + optionElement.getTagName() + " not found.");
                    }
                }
            }
        }
    }


    /**
    * Gets the tag name of the root element representing this object.
    * @exception UnsupportedOperationException at any time, since this
    *            class should get it's XML tag name in the
    *            {@link #OptionMap constructor}
    */
    public static String getXMLElementTagName() {
        throw new UnsupportedOperationException();
    }

}
