/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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


/**
 * Represents an option where the valid choice is an integer and the choices are
 * represented by strings. In general, these strings are localized by looking up
 * the key of the choice, which consists of the id of the AbstractObject
 * followed by a "." followed by the value of the option string. The automatic
 * localization can be suppressed with the doNotLocalize parameter, however.
 * There are two reasons to do this: either the option strings should not be
 * localized at all (because they are language names, for example), or the
 * option strings have already been localized (because they do not use the
 * default keys, for example).
 */
public class SelectOption extends IntegerOption {

    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger(SelectOption.class.getName());

    protected boolean localizedLabels = false;

    private Map<Integer, String> itemValues = new LinkedHashMap<Integer, String>();


    /**
     * Creates a new <code>SelectOption</code>.
     *
     * @param in The <code>XMSStreamReader</code> to read the data from
     * @exception XMLStreamException if an error occurs
     */
    public SelectOption(XMLStreamReader in) throws XMLStreamException {
        super(NO_ID);
        readFromXML(in);
    }

    /**
     * Gets the range values of this <code>RangeOption</code>.
     *
     * @return The value.
     */
    public Map<Integer, String> getItemValues() {
        return itemValues;
    }


    /**
     * Whether the labels of this option need to be localized. This is
     * not the case when the labels are just numeric values.
     *
     * @return a <code>boolean</code> value
     */
    public boolean localizeLabels() {
        return localizedLabels;
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
        toXMLImpl(out, getXMLElementTagName());
    }

    protected void toXMLImpl(XMLStreamWriter out, String tag)
        throws XMLStreamException {
        // Start element:
        out.writeStartElement(tag);

        out.writeAttribute(ID_ATTRIBUTE_TAG, getId());
        out.writeAttribute(VALUE_TAG, getStringValue());
        out.writeAttribute("localizedLabels", Boolean.toString(localizedLabels));

        for (Map.Entry<Integer, String> entry : itemValues.entrySet()) {
            out.writeStartElement(getXMLItemElementTagName());
            out.writeAttribute(VALUE_TAG, Integer.toString(entry.getKey()));
            out.writeAttribute("label", entry.getValue());
            out.writeEndElement();
        }

        out.writeEndElement();
    }

    /**
     * {@inheritDoc}
     */
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);
        localizedLabels = getAttribute(in, "localizedLabels", true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        if (in.getLocalName().equals(getXMLItemElementTagName())) {
            String label = in.getAttributeValue(null, "label");
            final String itemValue = in.getAttributeValue(null, VALUE_TAG);
            itemValues.put(Integer.parseInt(itemValue), label);
            in.nextTag();
        } else {
            throw new XMLStreamException("Unknown child \""
                                         + in.getLocalName() + "\" in a \""
                                         + getXMLElementTagName() + "\". ");
        }
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "selectOption".
     */
    public static String getXMLElementTagName() {
        return "selectOption";
    }

    /**
     * Gets the tag name of the item element This method is not static
     * to ensure proper overriding in <code>readFromXMLImpl</code>.
     *
     * @return "selectValue".
     */
    public String getXMLItemElementTagName() {
        return "selectValue";
    }
}
