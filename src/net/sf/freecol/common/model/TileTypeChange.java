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

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.util.Utils;


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
     * {@inheritDoc}
     */
    @Override
    public int compareTo(TileTypeChange other) {
        int cmp;
        cmp = (from == null) ? ((other.from == null) ? 0 : -1)
            : (other.from == null) ? 1
            : FreeColObject.compareIds(from, other.from);
        if (cmp != 0) return cmp;
        return (to == null) ? ((other.to == null) ? 0 : -1)
            : (other.to == null) ? 1
            : FreeColObject.compareIds(to, other.to);
    }

    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof TileTypeChange) {
            return this.compareTo((TileTypeChange)other) == 0;
        }
        return super.equals(other);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 37 * hash + Utils.hashCode(from);
        hash = 37 * hash + Utils.hashCode(to);
        return 37 * hash + Utils.hashCode(production);
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
     * @param xw The <code>FreeColXMLWriter</code> to write to.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    public void toXML(FreeColXMLWriter xw) throws XMLStreamException {
        xw.writeStartElement(getXMLElementTagName());

        xw.writeAttribute(FROM_TAG, from);

        xw.writeAttribute(TO_TAG, to);

        if (production != null) {
            xw.writeStartElement(PRODUCTION_TAG);

            xw.writeAttribute(GOODS_TYPE_TAG, production.getType());

            xw.writeAttribute(VALUE_TAG, production.getAmount());

            xw.writeEndElement();
        }

        xw.writeEndElement();
    }

    /**
     * Reads this object from an XML stream.
     *
     * @param xr The XML input stream.
     * @param spec The <code>Specification</code> to use.
     * @throws XMLStreamException if a problem was encountered
     *     during parsing.
     */
    public void readFromXML(FreeColXMLReader xr,
                            Specification spec) throws XMLStreamException {
        from = xr.getType(spec, FROM_TAG, TileType.class, (TileType)null);

        to = xr.getType(spec, TO_TAG, TileType.class, (TileType)null);

        while (xr.nextTag() != XMLStreamConstants.END_ELEMENT) {
            final String tag = xr.getLocalName();

            if (PRODUCTION_TAG.equals(tag)) {
                GoodsType type = xr.getType(spec, GOODS_TYPE_TAG,
                                            GoodsType.class, (GoodsType)null);

                int amount = xr.getAttribute(VALUE_TAG, 0);

                production = new AbstractGoods(type, amount);

                xr.closeTag(PRODUCTION_TAG);

            } else {
                throw new XMLStreamException("Bogus TileTypeChange tag: "
                    + tag);
            }
        }
    }

    /**
     * Delegate to getXMLElementTagName.
     *
     * @return What getXMLElementTagName does.
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
