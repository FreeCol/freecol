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


/**
 * A change in a tile type, including some bonus production when this occurs.
 */
public class TileTypeChange implements Comparable<TileTypeChange> {

    /** The original tile type. */
    private TileType from;

    /** The destination tile type. */
    private TileType to;

    /**
     * The goods produced by the tile type change, e.g. lumber when
     * clearing a forest.
     */
    private AbstractGoods production;


    /**
     * Gets the original tile type.
     *
     * @return The original tile type.
     */
    public final TileType getFrom() {
        return from;
    }

    /**
     * Set the original tile type.
     *
     * @param from The new original tile type.
     */
    public final void setFrom(final TileType from) {
        this.from = from;
    }

    /**
     * Gets the destination tile type.
     *
     * @return The destination tile type.
     */
    public final TileType getTo() {
        return to;
    }

    /**
     * Set the destination tile type.
     *
     * @param to The new destination tile type.
     */
    public final void setTo(final TileType to) {
        this.to = to;
    }

    /**
     * Gets the production consequent to the type change.
     *
     * @return The consequent production.
     */
    public final AbstractGoods getProduction() {
        return production;
    }

    /**
     * Set the production consequent to the type change.
     *
     * @param production The new consequent production.
     */
    public final void setProduction(final AbstractGoods production) {
        this.production = production;
    }


    // Interface Comparable<TileTypeChange>

    /**
     * Compares this object with the specified object.
     *
     * @param other The <code>TileTypeChange</code> to compare to.
     * @return A comparison result.
     */
    public int compareTo(TileTypeChange other) {
        int cmp;
        cmp = (from == null) ? ((other.from == null) ? 0 : -1)
            : (other.from == null) ? 1
            : from.getId().compareTo(other.from.getId());
        if (cmp != 0) return cmp;
        cmp = (to == null) ? ((other.to == null) ? 0 : -1)
            : (other.to == null) ? 1
            : to.getId().compareTo(other.to.getId());
        return cmp;
    }


    // Serialization
    private static final String FROM_TAG = "from";
    private static final String GOODS_TYPE_TAG = "goods-type";
    private static final String PRODUCTION_TAG = "production";
    private static final String TO_TAG = "to";
    private static final String VALUE_TAG = "value";


    /**
     * Makes an XML-representation of this object.
     *
     * @param out The output stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    public void toXML(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());

        FreeColObject.writeAttribute(out, FROM_TAG, from);

        FreeColObject.writeAttribute(out, TO_TAG, to);

        if (production != null) {
            out.writeStartElement(PRODUCTION_TAG);

            FreeColObject.writeAttribute(out, GOODS_TYPE_TAG,
                                         production.getType());

            FreeColObject.writeAttribute(out, VALUE_TAG,
                                         production.getAmount());

            out.writeEndElement();
        }

        out.writeEndElement();
    }

    /**
     * Reads this object from an XML stream.
     *
     * @param in The XML input stream.
     * @param spec The <code>Specification</code> to use.
     * @throws XMLStreamException if a problem was encountered
     *     during parsing.
     */
    public void readFromXML(XMLStreamReader in,
                            Specification spec) throws XMLStreamException {

        from = spec.getType(in, FROM_TAG, TileType.class, (TileType)null);

        to = spec.getType(in, TO_TAG, TileType.class, (TileType)null);

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            final String tag = in.getLocalName();

            if (PRODUCTION_TAG.equals(tag)) {
                GoodsType type = spec.getType(in, GOODS_TYPE_TAG,
                                              GoodsType.class, (GoodsType)null);

                int amount = FreeColObject.getAttribute(in, VALUE_TAG, 0);

                production = new AbstractGoods(type, amount);

                in.nextTag(); // close tag

            } else {
                throw new XMLStreamException("Unexpected TileTypeChange tag: "
                    + tag);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "change".
     */
    public static String getXMLElementTagName() {
        return "change";
    }
}
