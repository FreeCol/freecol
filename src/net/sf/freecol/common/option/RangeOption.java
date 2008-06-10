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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.client.gui.i18n.Messages;

/**
 * Represents an option where the valid choice is an integer and the choices are
 * represented by strings. In general, these strings are localized by looking up
 * the key of the choice, which consists of the id of the AbstractObject
 * followed by a "." followed by the value of the option string. RangeOption
 * differs from SelectOption, as the value being selected represents a numeric
 * measurement, defined by a bounded range of comparable values
 */
public class RangeOption extends AbstractOption {
    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger(RangeOption.class.getName());

    private int value;

    private boolean localizedLabels = false;

    private Map<String, Integer> rangeValues = new LinkedHashMap<String, Integer>();

    // TODO : remove this field and corresponding methods when all rangeOption
    // come from specification.xml
    String[] options;


    /**
     * Creates a new <code>RangeOption</code>.
     * 
     * @param in The <code>XMSStreamReader</code> to read the data from
     */
    public RangeOption(XMLStreamReader in) throws XMLStreamException {
        super(NO_ID);
        readFromXML(in);
    }

    // TODO : remove constructor when all rangeOption com from specification.xml
    /**
     * Creates a new <code>RangeOption</code>.
     * 
     * @deprecated
     * @param id The identifier for this option. This is used when the object
     *            should be found in an {@link OptionGroup}.
     * @param optionGroup The OptionGroup this Option belongs to.
     * @param options All possible values.
     * @param defaultOption The index of the default value.
     * @param doNotLocalize Suppress the default localization of options.
     */
    public RangeOption(String id, OptionGroup optionGroup, String[] options, int defaultOption, boolean doNotLocalize) {
        super(id, optionGroup);

        if (doNotLocalize) {
            this.options = options;
        } else {
            String[] localized = new String[options.length];
            for (int i = 0; i < options.length; i++) {
                localized[i] = Messages.message(getGroup() + "." + getId() + "." + options[i]);
            }
            this.options = localized;
        }

        this.value = defaultOption;
    }

    /**
     * Gets the current value of this <code>RangeOption</code>.
     * 
     * @deprecated
     * @return The value.
     */
    public String[] getOptions() {
        return options;
    }

    /**
     * Gets the current value of this <code>RangeOption</code>.
     * 
     * @return The value.
     */
    public int getValue() {
        return value;
    }

    /**
     * Gets the range values of this <code>RangeOption</code>.
     * 
     * @return The value.
     */
    public Map<String, Integer> getRangeValues() {
        return rangeValues;
    }

    /**
     * Sets the value of this <code>RangeOption</code>.
     * 
     * @param value The value to be set.
     */
    public void setValue(int value) {
        final int oldValue = this.value;
        this.value = value;

        if (value != oldValue && isDefined) {
            firePropertyChange("value", Integer.valueOf(oldValue), Integer.valueOf(value));
        }
        isDefined = true;
    }

    /**
     * Gets a <code>String</code> representation of the current value.
     * 
     * This method can be overwritten by subclasses to allow a custom save
     * value, since this method is used by {@link #toXML(XMLStreamWriter)}.
     * 
     * @return The String value of the Integer.
     * @see #setValue(String)
     */
    protected String getStringValue() {
        return Integer.toString(value);
    }

    /**
     * Converts the given <code>String</code> to an Integer and calls
     * {@link #setValue(int)}.
     * 
     * <br>
     * <br>
     * 
     * This method can be overwritten by subclasses to allow a custom save
     * value, since this method is used by {@link #readFromXML(XMLStreamReader)}.
     * 
     * @param value The String value of the Integer.
     * @see #getStringValue()
     */
    protected void setValue(String value) {
        setValue(Integer.parseInt(value));
    }

    /**
     * This method writes an XML-representation of this object to the given
     * stream.
     * 
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute("id", getId());
        out.writeAttribute("value", getStringValue());

        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * 
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        final String id = in.getAttributeValue(null, "id");
        final String defaultValue = in.getAttributeValue(null, "defaultValue");
        final String localizedLabels = in.getAttributeValue(null, "localizedLabels");
        final String value = in.getAttributeValue(null, "value");

        if (localizedLabels != null) {
            this.localizedLabels = localizedLabels.equals("true");
        }

        if (id == null && getId().equals("NO_ID")) {
            throw new XMLStreamException("invalid <" + getXMLElementTagName() + "> tag : no id attribute found.");
        }
        if (defaultValue == null && value == null) {
            throw new XMLStreamException("invalid <" + getXMLElementTagName()
                    + "> tag : no value nor default value found.");
        }

        if(getId() == NO_ID) {
            setId(id);
        }
        if (value != null) {
            setValue(Integer.parseInt(value));
            in.nextTag();
        } else {
            setValue(Integer.parseInt(defaultValue));
            while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
                if (in.getLocalName() == "range-value") {
                    String label = in.getAttributeValue(null, "label");
                    final String rangeValue = in.getAttributeValue(null, "value");
                    if (this.localizedLabels) {
                        label = Messages.message(label);
                    }
                    rangeValues.put(label, Integer.parseInt(rangeValue));
                } else {
                    throw new XMLStreamException("Unknow child \"" + in.getLocalName() + "\" in a \""
                            + getXMLElementTagName() + "\".");
                }
                in.nextTag();
            }

            options = rangeValues.keySet().toArray(new String[] {});
        }
    }

    /**
     * Gets the tag name of the root element representing this object.
     * 
     * @return "rangeOption".
     */
    public static String getXMLElementTagName() {
        return "range-option";
    }

}
