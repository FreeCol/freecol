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


package net.sf.freecol.common.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Turn;

import org.w3c.dom.Element;


/**
 * This class contains the last sale a player has made, by Settlement
 * and GoodsType.
 */
public final class LastSale extends FreeColObject {

    private static String LAST_SALE_TAG = "lastSale";

    // When a sale was made.
    private Turn when;

    // The price per unit returned from the sale.
    private int price;


    /**
     * Empty constructor for Player.
     */
    public LastSale() {
    }

    /**
     * Read a new <code>LastSale</code> from an XML representation.
     *
     * @param game The <code>Game</code> this object belongs to.
     * @param e An XML-element that will be used to initialize this object.
     */
    public LastSale(Game game, Element e) {
        readFromXMLElement(e);
    }

    /**
     * Make a new LastSale record.
     *
     * @param where The <code>Location</code> of the sale.
     * @param what The <code>GoodsType</code> sold.
     * @param when In which <code>Turn</code> the sale occurred.
     * @param price The per-unit price of the sale.
     */
    public LastSale(Location where, GoodsType what,
                    Turn when, int price) {
        this(makeKey(where, what), when, price);
    }

    /**
     * Make a new LastSale record.
     *
     * @param id The identifier (encoding Settlement and GoodsType).
     * @param when In which <code>Turn</code> the sale occurred.
     * @param price The per-unit price of the sale.
     */
    public LastSale(String id, Turn when, int price) {
        setId(id);
        this.when = when;
        this.price = price;
    }

    /**
     * Get the <code>Turn</code> when the sale was made.
     *
     * @return The <code>Turn</code> when the sale was made.
     */
    public Turn getWhen() {
        return when;
    }

    /**
     * Get the price from the sale.
     *
     * @return The price from the sale.
     */
    public int getPrice() {
        return price;
    }

    /**
     * Make a String to be used as a key for looking up sales.
     *
     * @param where The <code>Location</code> of the sale.
     * @param what The <code>GoodsType</code> sold.
     *
     * @return A key string.
     */
    public static String makeKey(Location where, GoodsType what) {
        return where.getId() + "-" + what.getId();
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems with the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out)
        throws XMLStreamException {
        out.writeStartElement(LAST_SALE_TAG);
        writeAttributes(out);
        writeChildren(out);
        out.writeEndElement();
    }

    public void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);
        out.writeAttribute("when", Integer.toString(when.getNumber()));
        out.writeAttribute("price", Integer.toString(price));
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if there are any problems with the stream.
     */
    public void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);

        int w;
        try {
            w = Integer.parseInt(in.getAttributeValue(null, "when"));
        } catch (NumberFormatException e) {
            w = 0;
        }
        when = new Turn(w);
        try {
            price = Integer.parseInt(in.getAttributeValue(null, "price"));
        } catch (NumberFormatException e) {
            price = 0;
        }

        super.readChildren(in);
    }

    /**
     * Debug helper.
     */
    public String toString() {
        return getXMLElementTagName() + "-" + getId() + "-"
            + when.toString() + "-" + Integer.toString(price);
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "lastSale"
     */
    public static String getXMLElementTagName() {
        return LAST_SALE_TAG;
    }
}
