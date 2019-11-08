/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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


/**
 * This class contains the last sale a player has made, by Settlement
 * and GoodsType.
 */
public final class LastSale extends FreeColObject {

    public static final String TAG = "lastSale";

    /** When a sale was made. */
    private Turn when;

    /** The price per unit returned from the sale. */
    private int price;


    /**
     * Trivial constructor to allow creation with Game.newInstance.
     */
    public LastSale() {}

    /**
     * Make a new LastSale record.
     *
     * @param where The {@code Location} of the sale.
     * @param what The {@code GoodsType} sold.
     * @param when In which {@code Turn} the sale occurred.
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
     * @param when In which {@code Turn} the sale occurred.
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
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public LastSale(FreeColXMLReader xr) throws XMLStreamException {
        readFromXML(xr);
    }


    /**
     * Get the {@code Turn} when the sale was made.
     *
     * @return The {@code Turn} when the sale was made.
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
     * @param where The {@code Location} of the sale.
     * @param what The {@code GoodsType} sold.
     *
     * @return A key string.
     */
    public static String makeKey(Location where, GoodsType what) {
        return what.getId() + "-" + where.getId();
    }


    // Overide FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        LastSale o = copyInCast(other, LastSale.class);
        if (o == null || !super.copyIn(o)) return false;
        this.when = o.getWhen();
        this.price = o.getPrice();
        return true;
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
    public String getXMLTagName() { return TAG; }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(32);
        sb.append('[').append(getId())
            .append(" when=").append(when)
            .append(" price=").append(price)
            .append(']');
        return sb.toString();
    }
}
