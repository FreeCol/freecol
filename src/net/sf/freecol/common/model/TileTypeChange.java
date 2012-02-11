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

package net.sf.freecol.common.model;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


public class TileTypeChange {

    /**
     * The original tile type.
     */
    private TileType from;

    /**
     * The destination tile type.
     */
    private TileType to;

    /**
     * The goods produced by the tile type change, e.g. lumber when
     * clearing a forest.
     */
    private AbstractGoods production;

    /**
     * Get the <code>From</code> value.
     *
     * @return a <code>TileType</code> value
     */
    public final TileType getFrom() {
        return from;
    }

    /**
     * Set the <code>From</code> value.
     *
     * @param newFrom The new From value.
     */
    public final void setFrom(final TileType newFrom) {
        this.from = newFrom;
    }

    /**
     * Get the <code>To</code> value.
     *
     * @return a <code>TileType</code> value
     */
    public final TileType getTo() {
        return to;
    }

    /**
     * Set the <code>To</code> value.
     *
     * @param newTo The new To value.
     */
    public final void setTo(final TileType newTo) {
        this.to = newTo;
    }

    /**
     * Get the <code>Production</code> value.
     *
     * @return an <code>AbstractGoods</code> value
     */
    public final AbstractGoods getProduction() {
        return production;
    }

    /**
     * Set the <code>Production</code> value.
     *
     * @param newProduction The new Production value.
     */
    public final void setProduction(final AbstractGoods newProduction) {
        this.production = newProduction;
    }


    /**
     * Makes an XML-representation of this object.
     *
     * @param out The output stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    public void toXML(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());
        out.writeAttribute("from", from.getId());
        out.writeAttribute("to", to.getId());

        if (production != null) {
            out.writeStartElement("production");
            out.writeAttribute("goods-type", production.getType().getId());
            out.writeAttribute("value", Integer.toString(production.getAmount()));
            out.writeEndElement();
        }
        out.writeEndElement();
    }

    /**
     * Reads this object from an XML stream.
     *
     * @param in The XML input stream.
     * @param specification a <code>Specification</code> value
     * @throws XMLStreamException if a problem was encountered
     *     during parsing.
     */
    protected void readFromXML(XMLStreamReader in, Specification specification)
        throws XMLStreamException {
        from = specification.getTileType(in.getAttributeValue(null, "from"));
        to = specification.getTileType(in.getAttributeValue(null, "to"));

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String childName = in.getLocalName();
            if ("production".equals(childName)) {
                GoodsType type = specification.getGoodsType(in.getAttributeValue(null, "goods-type"));
                int amount = Integer.parseInt(in.getAttributeValue(null, "value"));
                production = new AbstractGoods(type, amount);
                in.nextTag();
            }
        }
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "change".
     */
    public static String getXMLElementTagName() {
        return "change";
    }
}