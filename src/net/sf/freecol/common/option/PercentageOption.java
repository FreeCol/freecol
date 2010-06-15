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

/**
 * Represents an option where the result is a value between 0 and 100.
 */
public class PercentageOption extends IntegerOption {
    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger(PercentageOption.class.getName());


    /**
     * Creates a new <code>RangeOption</code>.
     * 
     * @param in The <code>XMSStreamReader</code> to read the data from
     */
    public PercentageOption(XMLStreamReader in) throws XMLStreamException {
        super(in);
        this.setMinimumValue(0);
        this.setMaximumValue(100);
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
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute(ID_ATTRIBUTE_TAG, getId());
        out.writeAttribute(VALUE_TAG, Integer.toString(getValue()));
        out.writeAttribute("previewEnabled", Boolean.toString(isPreviewEnabled()));

        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * 
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        final String id = in.getAttributeValue(null, ID_ATTRIBUTE_TAG);
        final String defaultValue = in.getAttributeValue(null, "defaultValue");
        final String value = in.getAttributeValue(null, VALUE_TAG);
        final String previewEnabled = in.getAttributeValue(null, "previewEnabled");

        if (id == null && getId().equals("NO_ID")) {
            throw new XMLStreamException("invalid <" + getXMLElementTagName() + "> tag : no id attribute found.");
        }
        if (defaultValue == null && value == null) {
            throw new XMLStreamException("invalid <" + getXMLElementTagName()
                    + "> tag : no value nor default value found.");
        }
        if (previewEnabled != null && !previewEnabled.equals("true") && !previewEnabled.equals("false")) {
            throw new XMLStreamException("invalid <" + getXMLElementTagName()
                    + "> tag : previewEnabled should be true or false.");
        }

        if (getId() == NO_ID) {
            setId(id);
        }
        if (value != null) {
            setValue(Integer.parseInt(value));
        } else {
            setValue(Integer.parseInt(defaultValue));
        }
        if(previewEnabled != null) {
            this.setPreviewEnabled(Boolean.parseBoolean(previewEnabled));
        }
        in.nextTag();
    }

    /**
     * Gets the tag name of the root element representing this object.
     * 
     * @return "percentageOption".
     */
    public static String getXMLElementTagName() {
        return "percentageOption";
    }
}
