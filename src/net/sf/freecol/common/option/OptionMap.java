
package net.sf.freecol.common.option;

import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Element;


/**
* Used for grouping objects of {@link Option}.
*/
public abstract class OptionMap extends OptionGroup {
    private static Logger logger = Logger.getLogger(OptionMap.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


    private String xmlTagName;
    private HashMap<String, Option> values;
   


    /**
    * Creates a new <code>OptionMap</code>.
    *
    * @param xmlTagName The tag name that should be used for the parent XML-element
    *           returned by {@link Option#toXMLElement}.
    * @param name The name of the <code>Option</code>. This text is used for identifying
    *           the option for a user. Example: The text related to a checkbox.
    * @param shortDescription Should give a short description of the <code>OptionGroup</code>.
    *           This might be used as a tooltip text.
    */
    public OptionMap(String xmlTagName, String name, String shortDescription) {
        super(name, shortDescription);
        this.xmlTagName = xmlTagName;
        
        values = new HashMap<String, Option>();

        addDefaultOptions();
        addToMap(this);
    }

    /**
     * Creates an <code>OptionMap</code> from an XML representation.
     *
     * <br><br>
     *
     * @param in The XML stream to read the data from.
     * @param xmlTagName The tag name that should be used for the parent XML-element
     *           returned by {@link Option#toXMLElement}.
     * @param name The name of the <code>Option</code>. This text is used for identifying
     *           the option for a user. Example: The text related to a checkbox.
     * @param shortDescription Should give a short description of the <code>OptionGroup</code>.
     *           This might be used as a tooltip text.
     * @throws XMLStreamException if an error occured during parsing.          
     */
     public OptionMap(XMLStreamReader in, String xmlTagName, String name, String shortDescription) throws XMLStreamException {
         this(xmlTagName, name, shortDescription);
         readFromXML(in);
     }

    /**
    * Creates an <code>OptionMap</code> from an XML representation.
    *
    * <br><br>
    *
    * @param element The XML <code>Element</code> from which this object
    *                should be constructed.
    * @param xmlTagName The tag name that should be used for the parent XML-element
    *           returned by {@link Option#toXMLElement}.
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
    * @param id The ID.
    * @return The <code>Object</code> with the given ID.
    */
    public Option getObject(String id) {
        return values.get(id);
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
        Option o = values.get(id);
        if (o instanceof IntegerOption) {
            return ((IntegerOption) o).getValue();
        } else if (o instanceof SelectOption) {
            return ((SelectOption) o).getValue();
        } else {
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
    * 
    * @param og The <code>OptionGroup</code> to be added.
    */
    public void addToMap(OptionGroup og) {
        Iterator<Option> it = og.iterator();
        while (it.hasNext()) {
            Option option = it.next();
            if (option instanceof OptionGroup) {
                addToMap((OptionGroup) option);
            } else {
                values.put(option.getId(), option);
            }
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
    public void toXML(XMLStreamWriter out) throws XMLStreamException {
        // Start element:
        out.writeStartElement(xmlTagName);

        Iterator<Option> it = values.values().iterator();
        while (it.hasNext()) {
            (it.next()).toXML(out);
        }

        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        updateFromXML(in);
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    private void updateFromXML(XMLStreamReader in) throws XMLStreamException {
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals(OptionGroup.getXMLElementTagName())) {
                updateFromXML(in);
            } else {
                final String idStr = in.getAttributeValue(null, "id");
                if (idStr != null) {
                    // old protocols:
                    Option o = getObject(idStr);

                    if (o != null) {
                        o.readFromXML(in);
                    } else {
                        // Normal only if this option is from an old save game:
                        logger.info("Option \"" + idStr + "\" (" + in.getLocalName() + ") could not be found.");
                        
                        // Ignore the option:
                        final String ignoredTag = in.getLocalName();
                        while (in.nextTag() != XMLStreamConstants.END_ELEMENT
                                || !in.getLocalName().equals(ignoredTag));
                    }
                } else {
                    Option o = getObject(in.getLocalName());
                    if (o != null) {
                        o.readFromXML(in);
                    } else {
                        // Normal only if this option is from an old save game:
                        logger.info("Option \"" + in.getLocalName() + " not found.");

                        // Ignore the option:
                        final String ignoredTag = in.getLocalName();
                        while (in.nextTag() != XMLStreamConstants.END_ELEMENT
                                || !in.getLocalName().equals(ignoredTag));
                    }
                }
            }
        }
        // DONE BY while-loop: in.nextTag();
    }


    /**
    * Gets the tag name of the root element representing this object.
    * @exception UnsupportedOperationException at any time, since this
    *            class should get it's XML tag name in the
    *            {@link #OptionMap(String, String, String) constructor}
    */
    public static String getXMLElementTagName() {
        throw new UnsupportedOperationException();
    }

}
