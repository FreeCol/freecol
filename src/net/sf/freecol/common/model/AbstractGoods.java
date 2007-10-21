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

import org.w3c.dom.Element;

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
     * Describe <code>getID</code> method here.
     *
     * @return a <code>String</code> value
     */
    public String getId() {
        return type.getId();
    }

    /**
     * Get the <code>Type</code> value.
     *
     * @return a <code>GoodsType</code> value
     */
    public final GoodsType getType() {
        return type;
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
     * <br><br>
     * 
     * Only attributes visible to the given <code>Player</code> will 
     * be added to that representation if <code>showAll</code> is
     * set to <code>false</code>.
     *  
     * @param out The target stream.
     * @param player The <code>Player</code> this XML-representation 
     *      should be made for, or <code>null</code> if
     *      <code>showAll == true</code>.
     * @param showAll Only attributes visible to <code>player</code> 
     *      will be added to the representation if <code>showAll</code>
     *      is set to <i>false</i>.
     * @param toSavedGame If <code>true</code> then information that
     *      is only needed when saving a game is added.
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
    * @return "goods".
    */
    public static String getXMLElementTagName() {
        return "abstractGoods";
    }

}
