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

import java.util.Collection;
import java.util.Comparator;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.GoodsType;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.Utils;


/**
 * Represents a certain amount of a GoodsType.  This does not
 * correspond to actual cargo present in a Location, but is intended
 * to represent things such as the amount of Lumber necessary to build
 * something, or the amount of cargo to load at a certain Location.
 */
public class AbstractGoods extends FreeColObject implements Named {

    /**
     * A comparator to sort by descending goods amount and then by a
     * predictable goods type order.
     */
    public static final Comparator<AbstractGoods> abstractGoodsComparator
        = new Comparator<AbstractGoods>() {
            @Override
            public int compare(AbstractGoods a1, AbstractGoods a2) {
                int cmp = a2.getAmount() - a1.getAmount();
                return (cmp != 0) ? cmp
                    : GoodsType.goodsTypeComparator.compare(a1.getType(),
                                                            a2.getType());
            }
        };

    /** The type of goods. */
    private GoodsType type;

    /** The amount of goods. */
    private int amount;


    /**
     * Empty constructor.
     */
    public AbstractGoods() {}

    /**
     * Creates a new <code>AbstractGoods</code> instance.
     *
     * @param type The <code>GoodsType</code> to create.
     * @param amount The amount of goods to create.
     */
    public AbstractGoods(GoodsType type, int amount) {
        setId(type.getId());
        this.type = type;
        this.amount = amount;
    }

    /**
     * Creates a new <code>AbstractGoods</code> instance.
     *
     * @param other Another <code>AbstractGoods</code> to copy.
     */
    public AbstractGoods(AbstractGoods other) {
        setId(other.type.getId());
        this.type = other.type;
        this.amount = other.amount;
    }


    /**
     * Get the goods type.
     *
     * @return The <code>GoodsType</code>.
     */
    public final GoodsType getType() {
        return type;
    }

    /**
     * Set the goods type.
     *
     * @param newType The new <code>GoodsType</code>.
     */
    public final void setType(final GoodsType newType) {
        this.type = newType;
    }

    /**
     * Get the goods amount.
     *
     * @return The goods amount.
     */
    public final int getAmount() {
        return amount;
    }

    /**
     * Set the goods amount.
     *
     * @param newAmount The new goods amount.
     */
    public final void setAmount(final int newAmount) {
        this.amount = newAmount;
    }

    /**
     * Get a label for these goods.
     *
     * @return The label for these goods.
     */
    public StringTemplate getLabel() {
        return getLabel(getType(), getAmount());
    }

    /**
     * Get a label for these goods.
     *
     * @param sellable Whether these goods can be sold.
     * @return A label for these goods.
     */
    public StringTemplate getLabel(boolean sellable) {
        return (sellable) ? getLabel()
            : StringTemplate.template("model.abstractGoods.boycotted")
                .addNamed("%goods%", getType())
                .addAmount("%amount%", getAmount());
    }

    /**
     * Get a label given a goods type and amount.
     *
     * @param type The <code>GoodsType</code> to display.
     * @param amount The amount of goods.
     * @return The goods label.
     */
    public static StringTemplate getLabel(GoodsType type, int amount) {
        return StringTemplate.template("model.abstractGoods.label")
            .addNamed("%goods%", type)
            .addAmount("%amount%", amount);
    }

    /**
     * Get a label given a key and amount.
     *
     * @param key A key for the goods to display.
     * @param amount The amount of goods.
     * @return The goods label.
     */
    public static StringTemplate getLabel(String key, int amount) {
        return StringTemplate.template("model.abstractGoods.label")
            .add("%goods%", key)
            .addAmount("%amount%", amount);
    }

    /**
     * Convenience lookup of the member of a collection of abstract goods that
     * matches a given goods type.
     *
     * @param type The <code>GoodsType</code> to look for.
     * @param goods The collection of <code>AbstractGoods</code> to look in.
     * @return The <code>AbstractGoods</code> found, or null if not.
     */
    public static AbstractGoods findByType(GoodsType type,
        Collection<? extends AbstractGoods> goods) {
        for (AbstractGoods ag : goods) if (ag.getType() == type) return ag;
        return null;
    }

    /**
     * Convenience lookup of the goods count in a collection of
     * abstract goods given a goods type.
     * 
     * @param type The <code>GoodsType</code> to look for.
     * @param goods The collection of <code>AbstractGoods</code> to look in.
     * @return The goods count found, or zero if not found.
     */
    public static int getCount(GoodsType type,
        Collection<? extends AbstractGoods> goods) {
        AbstractGoods ag = findByType(type, goods);
        return (ag == null) ? 0 : ag.getAmount();
    }

    /**
     * Does a goods collection contain an element with a given type?
     *
     * @param goods The <code>Goods<code> collection to search.
     * @param type The <code>GoodsType</code> to search for.
     * @return True if the goods type was found.
     */
    public static boolean containsType(GoodsType type,
        Collection<? extends AbstractGoods> goods) {
        return contains(goods, ag -> ag.getType() == type);
    }

    /**
     * Evaluate goods for trade purposes.
     *
     * @param player The <code>Player</code> to evaluate for.
     * @return A value for the goods.
     */
    public int evaluateFor(Player player) {
        final Market market = player.getMarket();
        return (market == null) ? getAmount() * 2 // FIXME: magic#
            : market.getSalePrice(getType(), getAmount());
    }


    // Interface Named

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNameKey() {
        return getType().getNameKey();
    }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof AbstractGoods) {
            AbstractGoods ag = (AbstractGoods)o;
            return type == ag.type && amount == ag.amount;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 31 * hash + Utils.hashCode(this.type);
        hash = 31 * hash + this.amount;
        return hash;
    }


    // Serialization

    private static final String AMOUNT_TAG = "amount";
    private static final String TYPE_TAG = "type";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(TYPE_TAG, type);

        xw.writeAttribute(AMOUNT_TAG, amount);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        final Specification spec = getSpecification();

        super.readAttributes(xr);

        type = xr.getType(spec, TYPE_TAG, GoodsType.class, (GoodsType)null);
        if (type == null) {
            throw new XMLStreamException("Null goods type.");
        } else {
            setId(type.getId());
        }

        amount = xr.getAttribute(AMOUNT_TAG, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return AbstractGoods.toString(this);
    }

    /**
     * Simple string version of some goods.
     *
     * @param ag The <code>AbstractGoods</code> to make a string from.
     * @return A string version of the goods.
     */     
    public static String toString(AbstractGoods ag) {
        return toString(ag.getType(), ag.getAmount());
    }

    /**
     * Simple string version of the component parts of some goods.
     *
     * @param goodsType The <code>GoodsType</code> to use.
     * @param amount The amount of goods.
     * @return A string version of the goods.
     */     
    public static String toString(GoodsType goodsType, int amount) {
        return amount + " "
            + ((goodsType == null) ? "(null)" : goodsType.getSuffix());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "abstractGoods".
     */
    public static String getXMLElementTagName() {
        return "abstractGoods";
    }
}
