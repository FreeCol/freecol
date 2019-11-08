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

import java.util.Collection;
import java.util.Comparator;
import java.util.function.Predicate;

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

    /** Compare the amount of abstract goods. */
    public static final Comparator<AbstractGoods> ascendingAmountComparator
        = Comparator.comparingInt(AbstractGoods::getAmount)
            .thenComparing(AbstractGoods::getType,
                           GoodsType.goodsTypeComparator);

    /**
     * A comparator to sort by descending goods amount and then by a
     * predictable goods type order.
     */
    public static final Comparator<AbstractGoods> descendingAmountComparator
        = Comparator.comparingInt(AbstractGoods::getAmount).reversed()
            .thenComparing(AbstractGoods::getType,
                           GoodsType.goodsTypeComparator);

    /** The type of goods. */
    protected GoodsType type;

    /** The amount of goods. */
    protected int amount;


    /**
     * Empty constructor.
     */
    public AbstractGoods() {}

    /**
     * Creates a new {@code AbstractGoods} instance.
     *
     * @param type The {@code GoodsType} to create.
     * @param amount The amount of goods to create.
     */
    public AbstractGoods(GoodsType type, int amount) {
        setId(type.getId());
        this.type = type;
        this.amount = amount;
    }

    /**
     * Creates a new {@code AbstractGoods} instance.
     *
     * @param other Another {@code AbstractGoods} to copy.
     */
    public AbstractGoods(AbstractGoods other) {
        setId(other.type.getId());
        this.type = other.type;
        this.amount = other.amount;
    }


    /**
     * Get the goods type.
     *
     * @return The {@code GoodsType}.
     */
    public final GoodsType getType() {
        return type;
    }

    /**
     * Set the goods type.
     *
     * @param newType The new {@code GoodsType}.
     */
    public final void setType(final GoodsType newType) {
        this.type = newType;
    }

    /**
     * Is the goods type a food type?
     *
     * @return True if this is food.
     */
    public final boolean isFoodType() {
        return getType().isFoodType();
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
     * Is the amount positive?
     *
     * @return True if the amount is greater than zero.
     */
    public final boolean isPositive() {
        return getAmount() > 0;
    }

    /**
     * Are these goods storable.
     *
     * @return True if the goods are storable.
     */
    public boolean isStorable() {
        return getType().isStorable();
    }

    /**
     * Get a label for these goods.
     *
     * @return The label for these goods.
     */
    public StringTemplate getLabel() {
        return StringTemplate.template("model.abstractGoods.label")
            .addNamed("%goods%", getType())
            .addAmount("%amount%", getAmount());
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
     * Get a label given a key and amount.
     *
     * Used for arbitrary objects, such as the missionary's bible.
     *
     * @param key A key for the goods to display.
     * @param amount The amount of goods.
     * @return The goods label.
     */
    public static StringTemplate getAbstractLabel(String key, int amount) {
        return StringTemplate.template("model.abstractGoods.label")
            .add("%goods%", key)
            .addAmount("%amount%", amount);
    }

    /**
     * Convenience lookup of the goods count in a collection of
     * abstract goods given a goods type.
     * 
     * @param type The {@code GoodsType} to look for.
     * @param goods The collection of {@code AbstractGoods} to look in.
     * @return The goods count found, or zero if not found.
     */
    public static int getCount(GoodsType type,
        Collection<? extends AbstractGoods> goods) {
        AbstractGoods ag = find(goods, matches(type));
        return (ag == null) ? 0 : ag.getAmount();
    }

    /**
     * Evaluate goods for trade purposes.
     *
     * @param player The {@code Player} to evaluate for.
     * @return A value for the goods.
     */
    public int evaluateFor(Player player) {
        final Market market = player.getMarket();
        return (market == null) ? getAmount() * 2 // FIXME: magic#
            : market.getSalePrice(getType(), getAmount());
    }

    /**
     * A predicate maker to match by type.
     *
     * @param key The key of type {@link GoodsType}
     * @return A suitable {@code Predicate}.
     */
    public static final Predicate<? super AbstractGoods> matches(final GoodsType key) {
        return matchKey(key, AbstractGoods::getType);
    }
    

    // Interface Named

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNameKey() {
        return getType().getNameKey();
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() {
        return null; // AbstractGoods are never serialized directly
    }


    // Overide FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        AbstractGoods o = copyInCast(other, AbstractGoods.class);
        if (o == null || !super.copyIn(o)) return false;
        this.type = o.getType();
        this.amount = o.getAmount();
        return true;
    }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof AbstractGoods) {
            AbstractGoods other = (AbstractGoods)o;
            return this.type == other.type && this.amount == other.amount
                && super.equals(other);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return AbstractGoods.toFullString(getType(), getAmount());
    }

    /**
     * Simple string version of the component parts of some goods.
     *
     * @param goodsType The {@code GoodsType} to use.
     * @param amount The amount of goods.
     * @return A string version of the goods.
     */     
    public static String toFullString(GoodsType goodsType, int amount) {
        return amount + " "
            + ((goodsType == null) ? "(null)" : goodsType.getSuffix());
    }
}
