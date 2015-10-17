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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import static net.sf.freecol.common.util.CollectionUtils.*;

import org.w3c.dom.Element;


/**
 * A <code>GoodsLocation</code> is a place where {@link Unit}s and
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
     * Creates a new <code>GoodsLocation</code> instance.
     *
     * @param game The enclosing <code>Game</code>.
     */
    public GoodsLocation(Game game) {
        super(game);
    }

    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param game The enclosing <code>Game</code>.
     * @param e An XML-element that will be used to initialize this object.
     */
    // Only Unit needs this
    public GoodsLocation(Game game, Element e) {
        super(game, e);
    }

    /**
     * Creates a new <code>GoodsLocation</code> instance.
     *
     * @param game The enclosing <code>Game</code>.
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
     * @param goods The <code>AbstractGoods</code> to add.
     * @return True if the goods were added.
     */
    public final boolean addGoods(AbstractGoods goods) {
        return addGoods(goods.getType(), goods.getAmount());
    }

    /**
     * Adds a list of goods to this location.
     *
     * @param goods The list of <code>AbstractGoods</code> to add.
     * @return True if the goods were all added.
     */
    public final boolean addGoods(List<AbstractGoods> goods) {
        for (AbstractGoods ag : goods) if (!addGoods(ag)) return false;
        return true;
    }

    /**
     * Removes the some goods from this location.
     *
     * @param goods The <code>AbstractGoods</code> to remove.
     * @return The goods that was removed, which may be less than that
     *     requested, or null if none.
     */
    public final Goods removeGoods(AbstractGoods goods) {
        return removeGoods(goods.getType(), goods.getAmount());
    }

    /**
     * Removes all Goods of the given type from this location.
     *
     * @param type The <code>GoodsType</code> to remove.
     * @return The goods that was removed, or null if none.
     */
    public final Goods removeGoods(GoodsType type) {
        return removeGoods(type, getGoodsCount(type));
    }

    /**
     * Gets the amount of one type of goods at this location.
     *
     * @param type The <code>GoodsType</code> to look for.
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
     * @param goods A list of <code>AbstractGoods</code> to check.
     * @return True if the goods are all present.
     */
    public final boolean containsGoods(List<AbstractGoods> goods) {
        return all(goods, ag -> ag.getAmount() <= getGoodsCount(ag.getType()));
    }

    /**
     * Gets an iterator for every <code>Goods</code> in this location.
     * Each <code>Goods</code> have a maximum amount of CARGO_SIZE.
     *
     * @return The <code>Iterator</code>.
     */
    public final Iterator<Goods> getGoodsIterator() {
        return (goodsContainer == null) ? null
            : goodsContainer.getGoodsIterator();
    }

    /**
     * Gets a list of all the goods in this location.  Each list member is
     * limited to a maximum amount of CARGO_SIZE, thus there may be multiple
     * entries with the same goods type.
     *
     * @return A list of goods.
     */
    public final List<Goods> getGoods() {
        return (goodsContainer == null) ? Collections.<Goods>emptyList()
            : goodsContainer.getGoods();
    }

    /**
     * Gets an list of all the goods in this location.  There is only
     * one <code>Goods</code> for each <code>GoodsType</code>, thus the
     * amount of goods may exceed CARGO_SIZE.
     *
     * @return A list of goods.
     */
    public final List<Goods> getCompactGoods() {
        return (goodsContainer == null) ? Collections.<Goods>emptyList()
            : goodsContainer.getCompactGoods();
    }


    // Interface Location (from UnitLocation)
    // Inheriting
    //    FreeColObject.getId()
    //    UnitLocation.getTile()
    //    UnitLocation.getLocationLabel
    //    UnitLocation.getLocationLabelFor
    //    UnitLocation.canAdd
    //    UnitLocation.getUnitCount
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
     * Gets the maximum number of <code>Goods</code> this Location
     * can hold.
     *
     * @return The capacity for goods
     */
    public abstract int getGoodsCapacity();

    /**
     * Adds a specified amount of a type of goods to this location.
     *
     * @param type The <code>GoodsType</code> to add.
     * @param amount The amount of goods to add.
     * @return True if the goods were added.
     */
    public boolean addGoods(GoodsType type, int amount) {
        if (goodsContainer == null) {
            goodsContainer = new GoodsContainer(getGame(), this);
        }
        return goodsContainer.addGoods(type, amount);
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
        return (goodsContainer == null) ? null
            : goodsContainer.removeGoods(type, amount);
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FreeColGameObject> getDisposeList() {
        List<FreeColGameObject> objects = new ArrayList<>();
        if (goodsContainer != null) {
            objects.addAll(goodsContainer.getDisposeList());
        }
        objects.addAll(super.getDisposeList());
        return objects;
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

        if (GoodsContainer.getXMLElementTagName().equals(tag)) {
            goodsContainer = xr.readFreeColGameObject(getGame(),
                                                      GoodsContainer.class);
            goodsContainer.setLocation(this);

        } else {
            super.readChild(xr);
        }
    }
}
