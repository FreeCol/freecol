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

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * A {@code GoodsLocation} is a place where {@link Unit}s and
 * {@link Goods} can be put.  It can not store any other Locatables,
 * such as {@link TileItem}s.
 *
 * @see Locatable
 */
public abstract class GoodsLocation extends UnitLocation {

    private static final Logger logger = Logger.getLogger(GoodsLocation.class.getName());

    /** The container for the goods. */
    private GoodsContainer goodsContainer = null;


    /**
     * Creates a new {@code GoodsLocation} instance.
     *
     * @param game The enclosing {@code Game}.
     */
    public GoodsLocation(Game game) {
        super(game);
    }

    /**
     * Creates a new {@code GoodsLocation} instance.
     *
     * @param game The enclosing {@code Game}.
     * @param id The object identifier.
     */
    public GoodsLocation(Game game, String id) {
        super(game, id);
    }


    // getGoodsContainer() is part of the Location interface.

    public final void setGoodsContainer(final GoodsContainer goodsContainer) {
        this.goodsContainer = goodsContainer;
    }

    /**
     * Adds some goods to this location.
     *
     * @param goods The {@code AbstractGoods} to add.
     * @return True if the goods were added.
     */
    public final boolean addGoods(AbstractGoods goods) {
        return addGoods(goods.getType(), goods.getAmount());
    }

    /**
     * Adds a list of goods to this location.
     *
     * @param goods The list of {@code AbstractGoods} to add.
     * @return True if the goods were all added.
     */
    public final boolean addGoods(List<AbstractGoods> goods) {
        return all(goods, ag -> addGoods(ag));
    }

    /**
     * Removes the some goods from this location.
     *
     * @param goods The {@code AbstractGoods} to remove.
     * @return The goods that was removed, which may be less than that
     *     requested, or null if none.
     */
    public final Goods removeGoods(AbstractGoods goods) {
        return removeGoods(goods.getType(), goods.getAmount());
    }

    /**
     * Removes all Goods of the given type from this location.
     *
     * @param type The {@code GoodsType} to remove.
     * @return The goods that was removed, or null if none.
     */
    public final Goods removeGoods(GoodsType type) {
        return removeGoods(type, getGoodsCount(type));
    }

    /**
     * Gets the amount of one type of goods at this location.
     *
     * @param type The {@code GoodsType} to look for.
     * @return The amount of goods.
     */
    public final int getGoodsCount(GoodsType type) {
        return (goodsContainer == null) ? 0
            : goodsContainer.getGoodsCount(type);
    }

    /**
     * Does this location contain enough goods to satisfy a list of
     * requirements?
     *
     * @param goods A list of {@code AbstractGoods} to check.
     * @return True if the goods are all present.
     */
    public final boolean containsGoods(List<AbstractGoods> goods) {
        return all(goods, ag -> ag.getAmount() <= getGoodsCount(ag.getType()));
    }

    /**
     * Remove all the goods.
     */
    public final void removeAll() {
        if (goodsContainer != null) {
            goodsContainer.removeAll();
            invalidateCache();
        }
    }
        
    /**
     * Gets a list of all the goods in this location.  Each list member is
     * limited to a maximum amount of CARGO_SIZE, thus there may be multiple
     * entries with the same goods type.
     *
     * @return A list of goods.
     */
    public List<Goods> getGoodsList() {
        return (goodsContainer == null) ? Collections.<Goods>emptyList()
            : goodsContainer.getGoodsList();
    }

    /**
     * Gets an list of all the goods in this location.  There is only
     * one {@code Goods} for each {@code GoodsType}, thus the
     * amount of goods may exceed CARGO_SIZE.
     *
     * @return A list of goods.
     */
    public List<Goods> getCompactGoodsList() {
        return (goodsContainer == null) ? Collections.<Goods>emptyList()
            : goodsContainer.getCompactGoodsList();
    }

    /**
     * Move goods from one location to another.
     *
     * @param src The source {@code GoodsLocation}.
     * @param goodsType The {@code GoodsType} to move.
     * @param amount The amount of goods to move.
     * @param dst The new {@code GoodsLocation}.
     */
    public static void moveGoods(GoodsLocation src, GoodsType goodsType,
                                 int amount, GoodsLocation dst) {
        GoodsContainer srcC = (src == null) ? null : src.getGoodsContainer();
        GoodsContainer dstC = (dst == null) ? null : dst.getGoodsContainer();
        GoodsContainer.moveGoods(srcC, goodsType, amount, dstC);
    }

    
    // Interface Location (from UnitLocation)
    // Inheriting
    //    FreeColObject.getId()
    //    UnitLocation.getTile()
    //    UnitLocation.getLocationLabel
    //    UnitLocation.getLocationLabelFor
    //    UnitLocation.canAdd
    //    UnitLocation.getUnitCount
    //    UnitLocation.getUnits
    //    UnitLocation.getUnitList
    //    UnitLocation.getSettlement
    // Does not implement getRank

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean add(Locatable locatable) {
        return (locatable instanceof Goods)
            ? addGoods((Goods)locatable)
            : super.add(locatable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(Locatable locatable) {
        return (locatable instanceof Goods) 
            ? removeGoods((Goods)locatable) != null
            : super.remove(locatable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(Locatable locatable) {
        return (locatable instanceof Goods)
            ? goodsContainer.contains((Goods)locatable)
            : super.contains(locatable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final GoodsContainer getGoodsContainer() {
        // Marked final, as this is where the goods container is.
        return goodsContainer;
    }

    // Interface UnitLocation
    // Inheriting
    //   UnitLocation.getSpaceTaken
    //   UnitLocation.moveToFront
    //   UnitLocation.clearUnitList,
    //   UnitLocation.getUnitCapacity
    //   UnitLocation.priceGoods
    //   UnitLocation.equipForRole

    /**
     * {@inheritDoc}
     */
    @Override
    public NoAddReason getNoAddReason(Locatable locatable) {
        if (locatable instanceof Goods) {
            Goods goods = (Goods)locatable;
            if (goods.getSpaceTaken() + ((goodsContainer == null) ? 0
                    : goodsContainer.getSpaceTaken())
                > getGoodsCapacity()) return NoAddReason.CAPACITY_EXCEEDED;
            return NoAddReason.NONE;
        }
        return super.getNoAddReason(locatable);
    }


    // GoodsLocation routines to be implemented/overridden by subclasses

    /**
     * Invalidate any cache dependent on the goods levels.
     */
    public abstract void invalidateCache();
    
    /**
     * Gets the maximum number of {@code Goods} this Location
     * can hold.
     *
     * @return The capacity for goods
     */
    public abstract int getGoodsCapacity();

    /**
     * Adds a specified amount of a type of goods to this location.
     *
     * @param type The {@code GoodsType} to add.
     * @param amount The amount of goods to add.
     * @return True if the goods were added.
     */
    public boolean addGoods(GoodsType type, int amount) {
        if (goodsContainer == null) {
            goodsContainer = new GoodsContainer(getGame(), this);
        }
        boolean ret = goodsContainer.addGoods(type, amount);
        invalidateCache();
        return ret;
    }

    /**
     * Removes a specified amount of a type of Goods from this location.
     *
     * @param type The type of goods to remove.
     * @param amount The amount of goods to remove.
     * @return The goods that was removed, which may be less than that
     *     requested, or null if none.
     */
    public Goods removeGoods(GoodsType type, int amount) {
        if (goodsContainer == null) return null;
        Goods ret = goodsContainer.removeGoods(type, amount);
        invalidateCache();
        return ret;
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<FreeColGameObject> getDisposables() {
        Stream<FreeColGameObject> up = super.getDisposables();
        return (this.goodsContainer == null) ? up
            : concat(this.goodsContainer.getDisposables(), up);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        GoodsLocation o = copyInCast(other, GoodsLocation.class);
        if (o == null || !super.copyIn(o)) return false;
        this.goodsContainer = getGame().update(o.getGoodsContainer(), true);
        return true;
    }


    // Serialization


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        if (goodsContainer != null) goodsContainer.toXML(xw);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        if (goodsContainer != null) goodsContainer.removeAll();

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final String tag = xr.getLocalName();

        if (GoodsContainer.TAG.equals(tag)) {
            goodsContainer = xr.readFreeColObject(getGame(), GoodsContainer.class);
            goodsContainer.setLocation(this);

        } else {
            super.readChild(xr);
        }
    }

    // getXMLTagName left to subclasses
}
