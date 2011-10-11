/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Element;

/**
 * The <code>GoodsLocation</code> is a place where {@link Unit}s and
 * {@link Goods} can be put. The GoodsLocation can not store any other
 * Locatables, such as {@link TileItem}s.
 *
 * @see Locatable
 */
public abstract class GoodsLocation extends UnitLocation {

    private static final Logger logger = Logger.getLogger(GoodsLocation.class.getName());

    /**
     * Describe goodsContainer here.
     */
    private GoodsContainer goodsContainer;



    protected GoodsLocation() {
        // empty constructor
    }

    /**
     * Creates a new <code>GoodsLocation</code> instance.
     *
     * @param game a <code>Game</code> value
     */
    public GoodsLocation(Game game) {
        super(game);
    }

    /**
     * Creates a new <code>GoodsLocation</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param in a <code>XMLStreamReader</code> value
     * @exception XMLStreamException if an error occurs
     */
    public GoodsLocation(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);
    }

    /**
     * Creates a new <code>GoodsLocation</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param e an <code>Element</code> value
     */
    public GoodsLocation(Game game, Element e) {
        super(game, e);
        readFromXMLElement(e);
    }

    /**
     * Creates a new <code>GoodsLocation</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param id a <code>String</code> value
     */
    public GoodsLocation(Game game, String id) {
        super(game, id);
    }

    /**
     * Gets the maximum number of <code>Goods</code> this Location
     * can hold.
     *
     * @return the capacity for goods
     */
    public abstract int getGoodsCapacity();


    /**
     * Gets an <code>Iterator</code> of every <code>Goods</code> in this
     * <code>GoodsContainer</code>. Each <code>Goods</code> have a maximum
     * amount of GoodsContainer.CARGO_SIZE.
     *
     * @return The <code>Iterator</code>.
     */
    public Iterator<Goods> getGoodsIterator() {
        return goodsContainer.getGoodsIterator();
    }

    /**
     * Gets an <code>List</code> with every <code>Goods</code> in this
     * <code>Colony</code>. There is only one <code>Goods</code> for each
     * type of goods.
     *
     * @return The <code>Iterator</code>.
     */
    public List<Goods> getCompactGoods() {
        return goodsContainer.getCompactGoods();
    }

    /**
     * Gets the reason why a given <code>Locatable</code> can not be
     * added to this Location.
     *
     * @param locatable The <code>Locatable</code> to test.
     * @return The reason why adding would fail.
     */
    public NoAddReason getNoAddReason(Locatable locatable) {
        if (locatable instanceof Goods) {
            // WARNING: Goods can always be added to settlements. Any
            // excess Goods will be removed during end-of-turn
            // processing. If Units should inherit from GoodsLocation,
            // this needs to be changed.
            return NoAddReason.NONE;
        } else {
            return super.getNoAddReason(locatable);
        }
    }

    /**
     * Adds a <code>Locatable</code> to this Location.
     *
     * @param locatable
     *            The <code>Locatable</code> to add to this Location.
     */
    @Override
    public boolean add(Locatable locatable) {
        if (locatable instanceof Goods) {
            return addGoods((Goods) locatable);
        } else {
            return super.add(locatable);
        }
    }

    /**
     * Removes a <code>Locatable</code> from this Location.
     *
     * @param locatable
     *            The <code>Locatable</code> to remove from this Location.
     */
    public boolean remove(Locatable locatable) {
        if (locatable instanceof Goods) {
            return removeGoods((Goods) locatable) != null;
        } else {
            return super.remove(locatable);
        }
    }

    /**
     * Checks if this <code>Location</code> contains the specified
     * <code>Locatable</code>.
     *
     * @param locatable
     *            The <code>Locatable</code> to test the presence of.
     * @return
     *            <ul>
     *            <li><i>true</i> if the specified <code>Locatable</code> is
     *            on this <code>Location</code> and
     *            <li><i>false</i> otherwise.
     *            </ul>
     */
    public boolean contains(Locatable locatable) {
        if (locatable instanceof Goods) {
            return goodsContainer.contains((Goods) locatable);
        } else {
            return super.contains(locatable);
        }
    }

    /**
     * Gets the <code>GoodsContainer</code> this <code>Location</code>
     * use for storing it's goods, or <code>null</code> if the
     * <code>Location</code> cannot store any goods.
     *
     * @return A <code>GoodsContainer</code> value
     */
    public final GoodsContainer getGoodsContainer() {
        return goodsContainer;
    }

    /**
     * Set the <code>GoodsContainer</code> value.
     *
     * @param newGoodsContainer The new GoodsContainer value.
     */
    public final void setGoodsContainer(final GoodsContainer newGoodsContainer) {
        this.goodsContainer = newGoodsContainer;
    }

    /**
     * Gets the storage capacity of this settlement.
     *
     * @return The storage capacity of this settlement.
     * @see #getGoodsCapacity
     */
    public int getWarehouseCapacity() {
        return getGoodsCapacity();
    }

    /**
     * Removes a specified amount of a type of Goods from this Settlement.
     *
     * @param type The type of Goods to remove from this settlement.
     * @param amount The amount of Goods to remove from this settlement.
     */
    public Goods removeGoods(GoodsType type, int amount) {
        return goodsContainer.removeGoods(type, amount);
    }

    /**
     * Removes the given Goods from the Settlement.
     *
     * @param goods a <code>Goods</code> value
     */
    public Goods removeGoods(AbstractGoods goods) {
        return goodsContainer.removeGoods(goods);
    }

    /**
     * Removes all Goods of the given type from the Settlement.
     *
     * @param type a <code>GoodsType</code> value
     */
    public Goods removeGoods(GoodsType type) {
        return goodsContainer.removeGoods(type);
    }

    /**
     * Describe <code>addGoods</code> method here.
     *
     * @param type a <code>GoodsType</code> value
     * @param amount an <code>int</code> value
     */
    public boolean addGoods(GoodsType type, int amount) {
        return goodsContainer.addGoods(type, amount);
    }

    /**
     * Describe <code>addGoods</code> method here.
     *
     * @param goods an <code>AbstractGoods</code> value
     */
    public boolean addGoods(AbstractGoods goods) {
        return addGoods(goods.getType(), goods.getAmount());
    }

    /**
     * Gets the amount of one type of Goods at this Settlement.
     *
     * @param type The type of goods to look for.
     * @return The amount of this type of Goods at this Location.
     */
    public int getGoodsCount(GoodsType type) {
        return goodsContainer.getGoodsCount(type);
    }

    /**
     * Removes all references to this object.
     *
     * @return A list of disposed objects.
     */
    public List<FreeColGameObject> disposeList() {
        List<FreeColGameObject> objects = new ArrayList<FreeColGameObject>();
        if (goodsContainer != null) {
            objects.addAll(goodsContainer.disposeList());
            goodsContainer = null;
        }
        objects.addAll(super.disposeList());
        return objects;
    }

    /**
     * Dispose of this GoodsLocation.
     */
    public void dispose() {
        disposeList();
    }

    /**
     * {@inheritDoc}
     */
    protected void writeChildren(XMLStreamWriter out, Player player,
                                 boolean showAll, boolean toSavedGame)
        throws XMLStreamException {
        super.writeChildren(out, player, showAll, toSavedGame);
        goodsContainer.toXML(out, player, showAll, toSavedGame);
    }

    /**
     * {@inheritDoc}
     */
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        if (GoodsContainer.getXMLElementTagName().equals(in.getLocalName())) {
            goodsContainer = (GoodsContainer) getGame().getFreeColGameObject(in.getAttributeValue(null, ID_ATTRIBUTE));
            if (goodsContainer == null) {
                goodsContainer = new GoodsContainer(getGame(), this, in);
            } else {
                goodsContainer.readFromXML(in);
            }
        } else {
            super.readChild(in);
        }
    }

}
