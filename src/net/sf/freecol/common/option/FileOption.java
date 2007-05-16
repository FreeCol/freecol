
package net.sf.freecol.common.option;

import java.io.File;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


/**
 * Represents an option for specifying a <code>File</code>.
 */
public class FileOption extends AbstractOption {
    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger(FileOption.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


    private File value;

    

    /**
     * Creates a new <code>FileOption</code>.
     *
     * @param id The identifier for this option. This is used when the object should be
     *           found in an {@link OptionGroup}.
     * @param name The name of the <code>Option</code>. This text is used for identifying
     *           the option for a user. Example: The text related to a checkbox.
     * @param shortDescription Should give a short description of the <code>IntegerOption</code>.
     *           This might be used as a tooltip text.
     */
    public FileOption(String id, String name, String shortDescription) {
        this(id, name, shortDescription, null);
    }
    
    /**
     * Creates a new <code>FileOption</code>.
     *
     * @param id The identifier for this option. This is used when the object should be
     *           found in an {@link OptionGroup}.
     * @param name The name of the <code>Option</code>. This text is used for identifying
     *           the option for a user. Example: The text related to a checkbox.
     * @param shortDescription Should give a short description of the <code>IntegerOption</code>.
     *           This might be used as a tooltip text.
     * @param value The default value of this <code>FileOption</code>.
     */
    public FileOption(String id, String name, String shortDescription, File value) {
        super(id, name, shortDescription);
        this.value = value;
    }


    /**
     * Gets the current value of this <code>FileOption</code>.
     * @return The value using <code>null</code> for marking
     *      no value.
     */
    public File getValue() {
        return value;
    }

    
    /**
     * Sets the value of this <code>FileOption</code>.
     * @param value The value to be set.
     */
    public void setValue(File value) {
        final File oldValue = this.value;
        this.value = value;
        
        if (value != oldValue) {
            firePropertyChange("value", oldValue, value);
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
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        // Start element:
        out.writeStartElement(getId());

        if (value != null) {
            out.writeAttribute("value", value.getAbsolutePath());
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
        final File oldValue = this.value;
        
        if (in.getAttributeValue(null, "value") != null
                && !in.getAttributeValue(null, "value").equals("")){
            value = new File(in.getAttributeValue(null, "value"));
        }
        in.nextTag();
        
        if (value != oldValue) {
            firePropertyChange("value", oldValue, value);
        }
    }
}
