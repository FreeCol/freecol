/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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

import net.sf.freecol.common.model.Specification;

/**
 * Represents an option where the result is a value between 0 and 100.
 */
public class PercentageOption extends IntegerOption {

    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger(PercentageOption.class.getName());


    /**
     * Creates a new <code>PercentageOption</code>.
     *
     * @param specification The specification this option belongs
     *     to. May be null.
     */
    public PercentageOption(Specification specification) {
        super(specification);
    }

    /**
     * {@inheritDoc}
     */
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);
        setMinimumValue(0);
        setMaximumValue(100);
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
        super.toXMLImpl(out, getXMLElementTagName());
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
