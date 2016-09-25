/**
 *  Copyright (C) 2002-2016   The FreeCol Team
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
import net.sf.freecol.common.util.Utils;


/**
 * A change in a tile type, including some bonus production when this occurs.
 */
public class TileTypeChange extends FreeColSpecObjectType {

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
     * Create a new tile type change.
     *
     * @param specification The {@code Specification} to refer to.
     */
    public TileTypeChange(Specification specification) {
        super(specification);
    }

    /**
     * Creates a new {@code TileTypeChange} instance.
     *
     * @param xr The {@code FreeColXMLReader} to read from.
     * @param specification The {@code Specification} to refer to.
     * @exception XMLStreamException if an error occurs
     */
    public TileTypeChange(FreeColXMLReader xr, Specification specification) throws XMLStreamException {
        this(specification);

        readFromXML(xr);
    }


    /**
     * Gets the original tile type.
     *
     * @return The original tile type.
     */
    public final TileType getFrom() {
        return from;
    }

    // @compat 0.10.4
    /**
     * Set the original tile type.
     *
     * @param from The new original tile type.
     */
    public final void setFrom(final TileType from) {
        this.from = from;
    }
    // end @compat 0.10.4

    /**
     * Gets the destination tile type.
     *
     * @return The destination tile type.
     */
    public final TileType getTo() {
        return to;
    }

    // @compat 0.10.4
    /**
     * Set the destination tile type.
     *
     * @param to The new destination tile type.
     */
    public final void setTo(final TileType to) {
        this.to = to;
    }
    // end @compat 0.10.4

    /**
     * Gets the production consequent to the type change.
     *
     * @return The consequent production.
     */
    public final AbstractGoods getProduction() {
        return production;
    }

    // @compat 0.10.4
    /**
     * Set the production consequent to the type change.
     *
     * @param production The new consequent production.
     */
    public final void setProduction(final AbstractGoods production) {
        this.production = production;
    }
    // end @compat 0.10.4


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
        hash = 37 * hash + Utils.hashCode(this.from);
        hash = 37 * hash + Utils.hashCode(this.to);
        return 37 * hash + Utils.hashCode(this.production);
    }


    // Serialization

    private static final String FROM_TAG = "from";
    private static final String GOODS_TYPE_TAG = "goods-type";
    private static final String PRODUCTION_TAG = "production";
    private static final String TO_TAG = "to";
    private static final String VALUE_TAG = "value";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        // No id, so no super.writeAttributes().

        xw.writeAttribute(FROM_TAG, this.from);

        xw.writeAttribute(TO_TAG, this.to);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        if (this.production != null) {
            xw.writeStartElement(PRODUCTION_TAG);

            xw.writeAttribute(GOODS_TYPE_TAG, this.production.getType());

            xw.writeAttribute(VALUE_TAG, this.production.getAmount());

            xw.writeEndElement();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        // No id, so no super.readAttributes().
        final Specification spec = getSpecification();
    
        from = xr.getType(spec, FROM_TAG, TileType.class, (TileType)null);

        to = xr.getType(spec, TO_TAG, TileType.class, (TileType)null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        final Specification spec = getSpecification();

        // Clear containers.
        if (xr.shouldClearContainers()) {
            this.production = null;
        }
    
        while (xr.moreTags()) {
            final String tag = xr.getLocalName();

            if (PRODUCTION_TAG.equals(tag)) {
                GoodsType type = xr.getType(spec, GOODS_TYPE_TAG,
                                            GoodsType.class, (GoodsType)null);

                int amount = xr.getAttribute(VALUE_TAG, 0);

                this.production = new AbstractGoods(type, amount);

                xr.closeTag(PRODUCTION_TAG);

            } else {
                throw new XMLStreamException("Bogus TileTypeChange tag: "
                    + tag);
            }
        }
    }

    /**
     * Delegate to getTagName.
     *
     * @return What getTagName does.
     */
    public String getXMLTagName() { return getTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "tile-type-change".
     */
    public static String getTagName() {
        return "tile-type-change";
    }
}
