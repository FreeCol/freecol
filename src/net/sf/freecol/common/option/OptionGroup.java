
package net.sf.freecol.common.option;

import java.util.logging.Logger;
import java.util.*;

import org.w3c.dom.*;


/**
* Used for grouping objects of {@link Option}s.
*/
public class OptionGroup extends Option {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static Logger logger = Logger.getLogger(OptionGroup.class.getName());

    private List options;


    /**
    * Creates a new <code>OptionGroup</code>.
    *
    * @param name The name of the <code>Option</code>. This text is used for identifying
    *           the option for a user. Example: The text related to a checkbox.
    * @param shortDescription Should give a short description of the <code>OptionGroup</code>.
    *           This might be used as a tooltip text.
    */
    public OptionGroup(String name, String shortDescription) {
        super(NO_ID, name, shortDescription);
        options = new ArrayList();
    }


    /**
    * Creates an <code>OptionGroup</code> from an XML representation.
    *
    * <br><br>
    *
    * @param element The XML <code>Element</code> from which this object
    *                should be constructed.
    */
    public OptionGroup(Element element) {
        super(element);
        options = new ArrayList();
        readFromXMLElement(element);
    }



    /**
    * Adds the given <code>Option</code>.
    * @param The <code>Option</code> that should be added to this
    *        <code>OptionGroup</code>.
    */
    public void add(Option option) {
        options.add(option);
    }

    
    /**
    * Removes all of the <code>Option</code>s from this <code>OptionGroup</code>.
    */
    public void removeAll() {
        options.clear();
    }


    /**
    * Removes the given <code>Option</code> from this <code>OptionGroup</code>.
    * @param option The <code>Option</code> to be removed.
    * @return The <code>Option</code> if it has been found and removed from either
    *         this <code>OptionGroup</code> or any of this <code>OptionGroup</code>'s
    *         sub-<code>OptionGroup</code>s. <code>null</code> is returned if the 
    *         object is not found.
    */
    public Option remove(Option option) {
        int index = options.indexOf(option);
        if (index >= 0) {
            return (Option) options.remove(index);
        } else {
            Iterator optionIterator = options.iterator();
            while (optionIterator.hasNext()) {
                Option o = (Option) optionIterator.next();
                if (o instanceof OptionGroup) {
                    if (((OptionGroup) o).remove(option) != null) {
                        return option;
                    }
                }
            }
        }
        
        return null;
    }


    /**
    * Returns an <code>Iterator</code> for the <code>Option</code>s.
    */
    public Iterator iterator() {
        return options.iterator();
    }


    /**
    * Writes the attributes and add the child nodes for this <code>OptionGroup</code>.
    * Should be called by subclasses from their own <code>writeAttributes</code>
    * in order to keep the superclass' values.
    *
    * @param element The XML <code>Element</code> in which the attributes and
    *                child nodes should be added.
    */
    protected void writeAttributes(Element optionGroupElement) {
        super.writeAttributes(optionGroupElement);

        Document document = optionGroupElement.getOwnerDocument();

        Iterator oi = options.iterator();
        while (oi.hasNext()) {
            optionGroupElement.appendChild(((Option) oi.next()).toXMLElement(document));
        }
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
    protected void readAttributes(Element optionGroupElement) {
        super.readAttributes(optionGroupElement);

        NodeList nl = optionGroupElement.getChildNodes();
        for (int i=0; i<nl.getLength(); i++) {
            Element optionElement = (Element) nl.item(i);
            if (optionElement.getTagName().equals(OptionGroup.getXMLElementTagName())) {
                options.add(new OptionGroup(optionElement));
            } else if (optionElement.getTagName().equals(BooleanOption.getXMLElementTagName())) {
                options.add(new BooleanOption(optionElement));
            } else if (optionElement.getTagName().equals(IntegerOption.getXMLElementTagName())) {
                options.add(new IntegerOption(optionElement));
            } else {
                logger.warning("Unrecognized option.");
            }
        }
    }


    /**
    * Makes an XML-representation of this object.
    *
    * @param document The document to use when creating new componenets.
    * @return The DOM-element ("Document Object Model") made to represent this "OptionGroup".
    */
    public Element toXMLElement(Document document) {
        Element optionGroupElement = document.createElement(getXMLElementTagName());

        writeAttributes(optionGroupElement);

        return optionGroupElement;
    }


    /**
    * Initializes this object from an XML-representation of this object.
    * @param optionGroupElement The DOM-element ("Document Object Model") made to represent this "OptionGroup".
    */
    public void readFromXMLElement(Element optionGroupElement) {
        options.clear();
        readAttributes(optionGroupElement);
    }


    /**
    * Gets the tag name of the root element representing this object.
    * @return "optionGroup".
    */
    public static String getXMLElementTagName() {
        return "optionGroup";
    }

}
