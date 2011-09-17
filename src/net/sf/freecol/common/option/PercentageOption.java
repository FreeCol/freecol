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
     * @exception XMLStreamException if an error occurs
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
        super.toXML(out, getXMLElementTagName());
    }

    /**
     * Write the attributes of this object to a stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing to
     *     the stream.
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeAttributes(out);

        out.writeAttribute(VALUE_TAG, Integer.toString(getValue()));
        out.writeAttribute("previewEnabled",
            Boolean.toString(isPreviewEnabled()));
    }

    /**
     * {@inheritDoc}
     */
    protected void readAttributes(XMLStreamReader in)
        throws XMLStreamException {
        super.readAttributes(in);
        final String previewEnabled = in.getAttributeValue(null, "previewEnabled");
        if (previewEnabled != null) {
            this.setPreviewEnabled(Boolean.parseBoolean(previewEnabled));
        }
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
