/**
 *  Copyright (C) 2002-2007  The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */


package net.sf.freecol.common.option;

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.client.gui.i18n.Messages;


/**
 * Represents an option where the valid choice is an integer and the
 * choices are represented by strings. In general, these strings are
 * localized by looking up the key of the choice, which consists of
 * the id of the AbstractObject followed by a "." followed by the
 * value of the option string. The automatic localization can be
 * suppressed with the doNotLocalize parameter, however. There are two
 * reasons to do this: either the option strings should not be
 * localized at all (because they are language names, for example), or
 * the option strings have already been localized (because they do not
 * use the default keys, for example).
 */
public class SelectOption extends AbstractOption {
    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger(SelectOption.class.getName());



    protected int value;
    protected String[] options;

    
    /**
     * Creates a new <code>SelectOption</code>.
     *
     * @param id The identifier for this option. This is used when the object should be
     *           found in an {@link OptionGroup}.
     * @param options All possible values.
     * @param defaultOption The index of the default value.
     */
    public SelectOption(String id, String[] options, int defaultOption) {
        this(id, null, options, defaultOption, false);
    }
    
    /**
     * Creates a new <code>SelectOption</code>.
     *
     * @param id The identifier for this option. This is used when the object should be
     *           found in an {@link OptionGroup}.
     * @param optionGroup The OptionGroup this Option belongs to.
     * @param options All possible values.
     * @param defaultOption The index of the default value.
     */
    public SelectOption(String id, OptionGroup optionGroup, String[] options, int defaultOption) {
        this(id, optionGroup, options, defaultOption, false);
    }
    
    /**
     * Creates a new <code>SelectOption</code>.
     *
     * @param id The identifier for this option. This is used when the object should be
     *           found in an {@link OptionGroup}.
     * @param optionGroup The OptionGroup this Option belongs to.
     * @param options All possible values.
     * @param defaultOption The index of the default value.
     * @param doNotLocalize Suppress the default localization of options.
     */
    public SelectOption(String id, OptionGroup optionGroup, String[] options, int defaultOption, boolean doNotLocalize) {
        super(id, optionGroup);

        if (doNotLocalize) {
            this.options = options;
        } else {
            String[] localized = new String[options.length];
            for (int i = 0; i < options.length; i++) {
                localized[i] = Messages.message(getGroup() + id + "." + options[i]);
            }        
            this.options = localized;
        }
        
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
        final int oldValue = this.value;
        this.value = value;
        
        if (value != oldValue) {
            firePropertyChange("value", Integer.valueOf(oldValue), Integer.valueOf(value));
        }
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
    protected void setOptions(String[] options) {
        this.options = options;
    }

    /**
     * Gets a <code>String</code> representation of the
     * current value.
     * 
     * This method can be overwritten by subclasses to allow
     * a custom save value, since this method is used by
     * {@link #toXML(XMLStreamWriter)}.
     * 
     * @return The String value of the Integer.
     * @see #setValue(String)
     */
    protected String getStringValue() {
        return Integer.toString(value);
    }
    
    /**
     * Converts the given <code>String</code> to an Integer
     * and calls {@link #setValue(int)}. 
     * 
     * <br><br>
     * 
     * This method can be overwritten by subclasses to allow
     * a custom save value, since this method is used by
     * {@link #readFromXML(XMLStreamReader)}.
     * 
     * @param value The String value of the Integer.
     * @see #getStringValue()
     */
    protected void setValue(String value) {
        setValue(Integer.parseInt(value));
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

        out.writeAttribute("value", getStringValue());

        out.writeEndElement();
    }
    
    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        final int oldValue = this.value;
        
        setValue(in.getAttributeValue(null, "value"));
        in.nextTag();
        
        if (value != oldValue) {
            firePropertyChange("value", Integer.valueOf(oldValue), Integer.valueOf(value));
        }
    }


    /**
     * Gets the tag name of the root element representing this object.
     * @return "selectOption".
     */
    public static String getXMLElementTagName() {
        return "selectOption";
    }

}
