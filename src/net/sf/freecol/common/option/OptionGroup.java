
package net.sf.freecol.common.option;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


/**
* Used for grouping objects of {@link Option}s.
*/
public class OptionGroup extends AbstractOption {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
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
    * Adds the given <code>Option</code>.
    * @param option The <code>Option</code> that should be
    *               added to this <code>OptionGroup</code>.
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
    * Returns an <code>Iterator</code> for the <code>Option</code>s.
    * @return The <code>Iterator</code>.
    */
    public Iterator iterator() {
        return options.iterator();
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
        out.writeStartElement(getXMLElementTagName());

        Iterator oi = options.iterator();
        while (oi.hasNext()) {
            ((Option) oi.next()).toXML(out);
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
        throw new UnsupportedOperationException();
    }


    /**
    * Gets the tag name of the root element representing this object.
    * @return "optionGroup".
    */
    public static String getXMLElementTagName() {
        return "optionGroup";
    }

}
