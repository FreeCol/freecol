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

package net.sf.freecol.common.model;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;

/**
 * Represents a certain amount of a GoodsType. It does not correspond
 * to actual cargo present in a Location. It is intended to represent
 * things such as the amount of Lumber necessary to build something,
 * or the amount of cargo to load at a certain Location.
 */
public class AbstractGoods extends FreeColObject {

    /**
     * Describe type here.
     */
    private GoodsType type;

    /**
     * Describe amount here.
     */
    private int amount;

    /**
     * Creates a new <code>AbstractGoods</code> instance.
     *
     */
    public AbstractGoods() {
        // empty constructor
    }

    /**
     * Creates a new <code>AbstractGoods</code> instance.
     *
     * @param type a <code>GoodsType</code> value
     * @param amount an <code>int</code> value
     */
    public AbstractGoods(GoodsType type, int amount) {
        setId(type.getId());
        this.type = type;
        this.amount = amount;
    }

    /**
     * Get the <code>Type</code> value.
     *
     * @return a <code>GoodsType</code> value
     */
    public final GoodsType getType() {
        return type;
    }

    public String getNameKey() {
        return getType().getNameKey();
    }

    /**
     * Set the <code>Type</code> value.
     *
     * @param newType The new Type value.
     */
    public final void setType(final GoodsType newType) {
        this.type = newType;
    }

    /**
     * Get the <code>Amount</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getAmount() {
        return amount;
    }

    /**
     * Set the <code>Amount</code> value.
     *
     * @param newAmount The new Amount value.
     */
    public final void setAmount(final int newAmount) {
        this.amount = newAmount;
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     * 
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */    
    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute("type", getId());
        out.writeAttribute("amount", Integer.toString(amount));
        out.writeEndElement();
    }
    
    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {        
        type = FreeCol.getSpecification().getGoodsType(in.getAttributeValue(null, "type"));
        amount = Integer.parseInt(in.getAttributeValue(null, "amount"));
        in.nextTag();
    }


    /**
    * Gets the tag name of the root element representing this object.
    * @return "abstractGoods".
    */
    public static String getXMLElementTagName() {
        return "abstractGoods";
    }

    public String toString() {
        return Integer.toString(amount) + " " + type.getId();
    }

}
