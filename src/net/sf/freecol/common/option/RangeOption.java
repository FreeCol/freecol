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

import java.util.Iterator;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


/**
 * Represents an option where the valid choice is an integer and the
 * choices are represented by strings. In general, these strings are
 * localized by looking up the key of the choice, which consists of
 * the id of the AbstractObject followed by a "." followed by the
 * value of the option string.
 *
 * RangeOption differs from SelectOption, as the value being selected
 * represents a numeric measurement, defined by a bounded range of
 * comparable values.  As the graphical component rendering a range
 * option only works with a conventional index, this implies to manage
 * a fixed rank for each possible values.
 */
public class RangeOption extends SelectOption {

    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger(RangeOption.class.getName());


    /**
     * Creates a new <code>RangeOption</code>.
     *
     * @param in The <code>XMSStreamReader</code> to read the data from
     * @exception XMLStreamException if an error occurs
     */
    public RangeOption(XMLStreamReader in) throws XMLStreamException {
        super(in);
    }

    /**
     * Gets the rank of the current selected value in the list of values of this
     * <code>RangeOption</code>.
     *
     * @return The value.
     */
    public int getValueRank() {
        int rank = 0;
        Iterator<Integer> iterator = getItemValues().keySet().iterator();
        while (iterator.hasNext() && iterator.next() != getValue()) {
            rank++;
        }
        return rank;
    }

    /**
     * Sets the value through the rank in the list of values of this
     * <code>RangeOption</code>.
     *
     * @param rank The rank of the value to be set.
     */
    public void setValueRank(int rank) {
        int curValue = UNDEFINED;
        Iterator<Integer> iterator = getItemValues().keySet().iterator();

        while (rank >= 0) {
            curValue = iterator.next();
            rank--;
        }

        setValue(curValue);
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
     * @return "rangeOption".
     */
    public static String getXMLElementTagName() {
        return "rangeOption";
    }

    /**
     * Gets the tag name of the item element
     *
     * @return "rangeValue".
     */
    public String getXMLItemElementTagName() {
        return "rangeValue";
    }
}
