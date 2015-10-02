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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import static net.sf.freecol.common.util.CollectionUtils.*;

import org.w3c.dom.Element;


/**
 * Contains goods and can be used by a {@link Location} to make certain
 * tasks easier.
 */
public class GoodsContainer extends FreeColGameObject implements Ownable {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(Location.class.getName());

    /** The size of a standard `hold' of data. */
    public static final int CARGO_SIZE = 100;

    /**
     * Value to use for apparent unlimited quantities of goods
     * (e.g. warehouse contents in Europe, amount of food a colony can
     * import).  Has to be signficantly bigger than any one unit could
     * expect to carry, but not so huge as to look silly in user
     * messages.
     */
    public static final int HUGE_CARGO_SIZE = 100 * CARGO_SIZE;

    /**
     * The list of Goods stored in this <code>GoodsContainer</code>.
     *
     * Always accessed synchronized (except I/O).
     */
    private final Map<GoodsType, Integer> storedGoods = new HashMap<>();

    /** 
     * The previous list of Goods stored in this
     * <code>GoodsContainer</code>.
     *
     * Always accessed synchronized and *after synchronized(storedGoods)*.
     * This is only touched rarely so the extra lock is tolerable.
     * (Not synchronized during I/O)
     */
    private final Map<GoodsType, Integer> oldStoredGoods = new HashMap<>();

    /** The location for this <code>GoodsContainer</code>. */
    private Location parent = null;


    /**
     * Creates an empty <code>GoodsContainer</code>.
     *
     * @param game The enclosing <code>Game</code>.
     * @param parent The <code>Location</code> this
     *     <code>GoodsContainer</code> contains goods for.
     */
    public GoodsContainer(Game game, Location parent) {
        super(game);

        this.parent = parent;
    }

    /**
     * Create a new <code>GoodsContainer</code>.
     *
     * @param game The enclosing <code>Game</code>.
     * @param id The object identifier.
     */
    public GoodsContainer(Game game, String id) {
        super(game, id);
    }

    /**
     * Create a new <code>GoodsContainer</code> from an
     * <code>Element</code>.
     *
     * @param game The enclosing <code>Game</code>.
     * @param parent The <code>Location</code> this
     *     <code>GoodsContainer</code> contains goods for.
     * @param e An XML-element that will be used to initialize
     *     this object.
     */
    public GoodsContainer(Game game, Location parent, Element e) {
        super(game, null);

        this.parent = parent;

        readFromXMLElement(e);
    }


    /**
     * Set the goods location.
     *
     * @param location The <code>Location</code> to set.
     */
    public void setLocation(Location location) {
        if (location == null) {
            throw new IllegalArgumentException("Null GoodsContainer Location.");
        }
        this.parent = location;
    }

    /**
     * Checks if the specified <code>Goods</code> is in this container.
     *
     * @param g The <code>Goods</code> to test the presence of.
     * @return True if there is enough of the specified goods present that it
     *     can be removed without error.
     */
    public boolean contains(Goods g) {
        return getGoodsCount(g.getType()) >= g.getAmount();
    }

    /**
     * Gets the amount of one type of goods in this container.
     *
     * @param type The <code>GoodsType</code> being looked for.
     * @return The amount of this type of goods in this container.
     */
    public int getGoodsCount(GoodsType type) {
        synchronized (storedGoods) {
            return (storedGoods.containsKey(type)) 
                ? storedGoods.get(type)
                : 0;
        }
    }

    /**
     * Gets the amount of one type of goods at the beginning of the turn.
     *
     * @param type The <code>GoodsType</code> being looked for.
     * @return The amount of this type of goods in this container at
     *     the beginning of the turn
     */
    public int getOldGoodsCount(GoodsType type) {
        synchronized (storedGoods) {
            synchronized (oldStoredGoods) {
                return (oldStoredGoods.containsKey(type))
                    ? oldStoredGoods.get(type)
                    : 0;
            }
        }
    }

    /**
     * Adds goods to this goods container.
     *
     * @param goods The <code>Goods</code> to add.
     * @return True if the addition succeeds.
     */
    public boolean addGoods(AbstractGoods goods) {
        return addGoods(goods.getType(), goods.getAmount());
    }

    /**
     * Adds goods by type and amount to this goods container.
     *
     * Note: negative amounts are allowed.
     *
     * @param type The <code>GoodsType</code> to add.
     * @param amount The amount of goods to add.
     * @return True if the addition succeeds.
     */
    public boolean addGoods(GoodsType type, int amount) {
        int oldAmount = getGoodsCount(type);
        int newAmount = oldAmount + amount;

        if (newAmount < 0) {
            throw new IllegalStateException("Operation would leave "
                + newAmount + " goods of type " + type
                + " in Location " + parent);
        } else if (newAmount == 0) {
            synchronized (storedGoods) {
                storedGoods.remove(type);
            }
        } else {
            synchronized (storedGoods) {
                storedGoods.put(type, newAmount);
            }
        }
        return true;
    }

    /**
     * Removes goods from this goods container.
     *
     * @param goods The <code>Goods</code> to remove from this container.
     * @return The <code>Goods</code> actually removed.
     */
    public Goods removeGoods(AbstractGoods goods) {
        return removeGoods(goods.getType(), goods.getAmount());
    }

    /**
     * Removes all goods of a given type from this goods container.
     *
     * @param type The <code>GoodsType</code> to remove.
     * @return The <code>Goods</code> actually removed.
     */
    public Goods removeGoods(GoodsType type) {
        return removeGoods(type, INFINITY);
    }

    /**
     * Removes goods by type and amount from this goods container.
     *
     * @param type The <code>GoodsType</code> to remove.
     * @param amount The amount of goods to remove.
     * @return The <code>Goods</code> actually removed, which may have a
     *     lower actual amount, or null if nothing removed.
     */
    public Goods removeGoods(GoodsType type, int amount) {
        int oldAmount = getGoodsCount(type);
        if (oldAmount <= 0) return null;

        int newAmount = oldAmount - amount;
        Goods removedGoods;
        if (newAmount > 0) {
            removedGoods = new Goods(getGame(), null, type, amount);
            synchronized (storedGoods) {
                storedGoods.put(type, newAmount);
            }
        } else {
            removedGoods = new Goods(getGame(), null, type, oldAmount);
            synchronized (storedGoods) {
                storedGoods.remove(type);
            }
        }
        return removedGoods;
    }

    /**
     * Set the amount of goods in this container.
     *
     * @param goodsType The <code>GoodsType</code> to set the amount of.
     * @param newAmount The new amount.
     */
    public void setAmount(GoodsType goodsType, int newAmount) {
        if (newAmount == 0) {
            synchronized (storedGoods) {
                storedGoods.remove(goodsType);
            }
        } else {
            synchronized (storedGoods) {
                storedGoods.put(goodsType, newAmount);
            }
        }
    }

    /**
     * Remove all goods.
     */
    public void removeAll() {
        synchronized (storedGoods) {
            storedGoods.clear();
        }
    }

    /**
     * Clear both containers.
     */
    private void clearContainers() {
        synchronized (storedGoods) {
            storedGoods.clear();
            synchronized (oldStoredGoods) {
                oldStoredGoods.clear();
            }
        }
    }

    /**
     * Removes all goods above given amount, provided that the goods
     * are storable and do not ignore warehouse limits.
     *
     * @param newAmount The threshold.
     */
    public void removeAbove(int newAmount) {
        synchronized (storedGoods) {
            if (newAmount <= 0) {
                storedGoods.clear();
                return;
            }
            for (GoodsType goodsType : storedGoods.keySet()) {
                if (goodsType.isStorable() && !goodsType.limitIgnored()
                    && storedGoods.get(goodsType) > newAmount) {
                    setAmount(goodsType, newAmount);
                }
            }
        }
    }

    /**
     * Checks if any storable type of goods has reached the given amount.
     *
     * @param amount The amount to check.
     * @return True if any storable, capacity limited goods has reached the
     *     given amount.
     */
    public boolean hasReachedCapacity(int amount) {
        synchronized (storedGoods) {
            return any(storedGoods.keySet(), gt -> gt.isStorable()
                && !gt.limitIgnored() && storedGoods.get(gt) > amount);
        }
    }

    /**
     * Gets the amount of space that the goods in this container will consume.
     * Each occupied cargo slot contains an amount in [1, CARGO_SIZE].
     *
     * @return The amount of space taken by this containers goods.
     */
    public int getSpaceTaken() {
        synchronized (storedGoods) {
            return storedGoods.values().stream()
                .mapToInt(amount -> (amount % CARGO_SIZE == 0)
                    ? amount/CARGO_SIZE
                    : amount/CARGO_SIZE + 1).sum();
        }
    }

    /**
     * Gets an iterator over all holds of goods in this goods container.
     * Each <code>Goods</code> returned has a maximum amount of CARGO_SIZE.
     *
     * @return The <code>Iterator</code>.
     * @see #getCompactGoods
     */
    public Iterator<Goods> getGoodsIterator() {
        return getGoods().iterator();
    }

    /**
     * Gets a list containing all holds of goods in this goods container.
     * Each <code>Goods</code> returned has a maximum amount of CARGO_SIZE.
     *
     * @return A list of <code>Goods</code>.
     * @see #getGoodsIterator
     */
    public List<Goods> getGoods() {
        List<Goods> totalGoods = new ArrayList<>();
        synchronized (storedGoods) {
            for (GoodsType goodsType : storedGoods.keySet()) {
                int amount = storedGoods.get(goodsType);
                while (amount > 0) {
                    totalGoods.add(new Goods(getGame(), parent, goodsType,
                            ((amount >= CARGO_SIZE) ? CARGO_SIZE : amount)));
                    amount -= CARGO_SIZE;
                }
            }
        }
        return totalGoods;
    }

    /**
     * Gets a list of all goods in this goods container.
     * There is only one <code>Goods</code> for each distinct
     * <code>GoodsType</code>.
     *
     * @return A list of <code>Goods</code>.
     */
    public List<Goods> getCompactGoods() {
        List<Goods> totalGoods = new ArrayList<>();
        synchronized (storedGoods) {
            for (Entry<GoodsType, Integer> entry : storedGoods.entrySet()) {
                if (entry.getValue() > 0) {
                    totalGoods.add(new Goods(getGame(), parent, entry.getKey(),
                                             entry.getValue()));
                }
            }
        }
        return totalGoods;
    }

    /**
     * Save the current stored goods of this goods container in the old
     * stored goods.
     */
    public void saveState() {
        synchronized (storedGoods) {
            synchronized (oldStoredGoods) {
                oldStoredGoods.clear();
                oldStoredGoods.putAll(storedGoods);
            }
        }
    }

    /**
     * Restore the current stored goods of this goods container to the
     * old state.
     */
    public void restoreState() {
        synchronized (storedGoods) {
            synchronized (oldStoredGoods) {
                storedGoods.clear();
                storedGoods.putAll(oldStoredGoods);
            }
        }
    }

    /**
     * Has this goods containers contents changed from what was recorded
     * last time the state was saved?
     *
     * @return True if the contents have changed.
     */
    public boolean hasChanged() {
        return any(getSpecification().getGoodsTypeList(),
            gt -> getOldGoodsCount(gt) != getGoodsCount(gt));
    }

    /**
     * Fire property changes for all goods that have seen level changes
     * since the last saveState().
     *
     * @return True if something changed.
     */
    public boolean fireChanges() {
        boolean ret = false;
        for (GoodsType type : getSpecification().getGoodsTypeList()) {
            int oldCount = getOldGoodsCount(type);
            int newCount = getGoodsCount(type);
            if (oldCount != newCount) {
                firePropertyChange(type.getId(), oldCount, newCount);
                ret = true;
            }
        }
        return ret;
    }

    // Interface Ownable

    /**
     * {@inheritDoc}
     */
    @Override
    public Player getOwner() {
        return (parent instanceof Ownable) ? ((Ownable)parent).getOwner()
            : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOwner(Player p) {
        throw new UnsupportedOperationException("Can not set GoodsContainer owner");
    }

    // Override FreeColGameObject

    /**
     * {@inheritDoc}
     */
    @Override
    public void disposeResources() {
        clearContainers();
        super.disposeResources();
    }


    // Serialization

    public static final String AMOUNT_TAG = "amount";
    public static final String OLD_STORED_GOODS_TAG = "oldStoredGoods";
    public static final String STORED_GOODS_TAG = "storedGoods";
    public static final String TYPE_TAG = "type";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        if (xw.validFor(getOwner())) {

            writeStorage(xw, STORED_GOODS_TAG, storedGoods);

            writeStorage(xw, OLD_STORED_GOODS_TAG, oldStoredGoods);
        }
    }

    /**
     * Write a storage container to a stream.
     *
     * @param xw The <code>FreeColXMLWriter</code> to write to.
     * @param tag The element tag.
     * @param storage The storage container.
     * @exception XMLStreamException if there is a problem writing to
     *     the stream.
     */
    private void writeStorage(FreeColXMLWriter xw, String tag,
                              Map<GoodsType, Integer> storage) throws XMLStreamException {
        if (storage.isEmpty()) return;

        xw.writeStartElement(tag);

        for (GoodsType goodsType : getSortedCopy(storage.keySet())) {

            xw.writeStartElement(Goods.getXMLElementTagName());

            xw.writeAttribute(TYPE_TAG, goodsType);

            xw.writeAttribute(AMOUNT_TAG, storage.get(goodsType));

            xw.writeEndElement();
        }

        xw.writeEndElement();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        clearContainers();

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final String tag = xr.getLocalName();

        if (OLD_STORED_GOODS_TAG.equals(tag)) {
            readStorage(xr, oldStoredGoods);

        } else if (STORED_GOODS_TAG.equals(tag)) {
            readStorage(xr, storedGoods);

        } else {
            super.readChild(xr);
        }
    }

    /**
     * Read a storage container from a stream.
     *
     * @param xr The <code>FreeColXMLReader</code> to read from.
     * @param storage The storage container.
     * @exception XMLStreamException if there is a problem reading from
     *     the stream.
     */
    private void readStorage(FreeColXMLReader xr,
        Map<GoodsType, Integer> storage) throws XMLStreamException {
        final Specification spec = getGame().getSpecification();

        while (xr.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String tag = xr.getLocalName();

            if (Goods.getXMLElementTagName().equals(tag)) {
                GoodsType goodsType = xr.getType(spec, TYPE_TAG,
                    GoodsType.class, (GoodsType)null);

                int amount = xr.getAttribute(AMOUNT_TAG, 0);

                storage.put(goodsType, amount);

            } else {
                throw new XMLStreamException("Bogus GoodsContainer tag: "
                    + tag);
            }
            xr.closeTag(tag);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("[").append(getId()).append(" [");
        // Do not bother to synchronize containers for display
        for (Map.Entry<GoodsType, Integer> entry : storedGoods.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue())
                .append(", ");
        }
        sb.setLength(sb.length() - ", ".length());
        sb.append("][");
        for (Map.Entry<GoodsType, Integer> entry : oldStoredGoods.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue())
                .append(", ");
        }
        sb.setLength(sb.length() - ", ".length());
        sb.append("]]");
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "goodsContainer".
     */
    public static String getXMLElementTagName() {
        return "goodsContainer";
    }
}
