/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;

import org.w3c.dom.Element;


/**
 * This class contains the last sale a player has made, by Settlement
 * and GoodsType.
 */
public final class LastSale extends FreeColObject {

    /** When a sale was made. */
    private Turn when;

    /** The price per unit returned from the sale. */
    private int price;


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
     * @param id The object identifier.
     * @param when In which <code>Turn</code> the sale occurred.
     * @param price The per-unit price of the sale.
     */
    public LastSale(String id, Turn when, int price) {
        setId(id);
        this.when = when;
        this.price = price;
    }

    /**
     * Create a new last sale by reading a stream.
     *
     * @param xr The <code>FreeColXMLReader</code> to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public LastSale(FreeColXMLReader xr) throws XMLStreamException {
        readFromXML(xr);
    }

    /**
     * Create a new last sale by reading an element.
     *
     * @param element The <code>Element</code> to read from.
     */
    public LastSale(Element element) {
        readFromXMLElement(element);
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


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(FreeColObject other) {
        int cmp = 0;
        if (other instanceof LastSale) {
            LastSale ls = (LastSale)other;
            cmp = getWhen().getNumber() - ls.getWhen().getNumber();
        }
        if (cmp == 0) cmp = super.compareTo(other);
        return cmp;
    }


    // Serialization

    private static final String PRICE_TAG = "price";
    private static final String WHEN_TAG = "when";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(WHEN_TAG, when.getNumber());

        xw.writeAttribute(PRICE_TAG, price);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        when = new Turn(xr.getAttribute(WHEN_TAG, 0));

        price = xr.getAttribute(PRICE_TAG, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(32);
        sb.append("[").append(getId())
            .append(" when=").append(when)
            .append(" price=").append(price)
            .append("]");
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "lastSale"
     */
    public static String getXMLElementTagName() {
        return "lastSale";
    }
}
