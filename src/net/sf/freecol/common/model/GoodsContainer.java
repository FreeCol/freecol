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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Ownable;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Element;


/**
 * Contains goods and can be used by a {@link Location} to make certain
 * tasks easier.
 */
public class GoodsContainer extends FreeColGameObject implements Ownable {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(Location.class.getName());

    public static final int CARGO_SIZE = 100;
    public static final String STORED_GOODS_TAG = "storedGoods";
    public static final String OLD_STORED_GOODS_TAG = "oldStoredGoods";

    /** The list of Goods stored in this <code>GoodsContainer</code>. */
    private Map<GoodsType, Integer> storedGoods = new HashMap<GoodsType, Integer>();

    /** The previous list of Goods stored in this <code>GoodsContainer</code>. */
    private Map<GoodsType, Integer> oldStoredGoods = new HashMap<GoodsType, Integer>();

    /** The owner of this <code>GoodsContainer</code>. */
    private final Location parent;

    /**
     * Creates an empty <code>GoodsContainer</code>.
     *
     * @param game The <code>Game</code> in which this <code>GoodsContainer</code> belong.
     * @param parent The <code>Location</code> this <code>GoodsContainer</code> will be containg goods for.
     */
    public GoodsContainer(Game game, Location parent) {
        super(game);

        if (parent == null) {
            throw new IllegalArgumentException("Location of GoodsContainer must not be null!");
        }

        this.parent = parent;
    }

    /**
     * Initiates a new <code>GoodsContainer</code> from an <code>Element</code>.
     *
     * @param game The <code>Game</code> in which this <code>GoodsContainer</code>
     *       belong.
     * @param parent The object using this <code>GoodsContainer</code>
     *       for storing it's goods.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public GoodsContainer(Game game, Location parent, XMLStreamReader in) throws XMLStreamException {
        super(game, in);

        if (parent == null) {
            throw new IllegalArgumentException("Location of GoodsContainer must not be null!");
        }

        this.parent = parent;
        readFromXML(in);
    }

    /**
     * Initiates a new <code>GoodsContainer</code> from an <code>Element</code>.
     *
     * @param game The <code>Game</code> in which this <code>GoodsContainer</code>
     *       belong.
     * @param parent The object using this <code>GoodsContainer</code>
     *       for storing it's goods.
     * @param e An XML-element that will be used to initialize
     *      this object.
     */
    public GoodsContainer(Game game, Location parent, Element e) {
        super(game, e);

        if (parent == null) {
            throw new IllegalArgumentException("Location of GoodsContainer must not be null!");
        }

        this.parent = parent;
        readFromXMLElement(e);
    }

    /**
     * Gets the owner of this <code>GoodsContainer</code>.
     *
     * @return The <code>Player</code> controlling this
     *         {@link Ownable}.
     */
    public Player getOwner() {
        return (parent instanceof Ownable) ? ((Ownable) parent).getOwner()
            : null;
    }

    /**
     * Sets the owner of this <code>Ownable</code>.
     *
     * @param p The <code>Player</code> that should take ownership
     *      of this {@link Ownable}.
     * @exception UnsupportedOperationException if not implemented.
     */
    public void setOwner(Player p) {
        throw new UnsupportedOperationException("Can not set GoodsContainer owner");
    }

    /**
     * Removes all references to this object.
     *
     * @return A list of disposed objects.
     */
    public List<FreeColGameObject> disposeList() {
        storedGoods.clear();

        List<FreeColGameObject> objects = new ArrayList<FreeColGameObject>();
        objects.addAll(super.disposeList());
        return objects;
    }

    /**
     * Dispose of this GoodsContainer.
     */
    public void dispose() {
        disposeList();
    }


    /**
     * Adds a <code>Goods</code> to this containter.
     * @param g The Goods to add to this container.
     */
    public boolean addGoods(AbstractGoods g) {
        return addGoods(g.getType(), g.getAmount());
    }

    /**
     * Adds the given amount of the given type of goods.
     * @param type The type of goods to add.
     * @param amount The type of amount to add.
     */
    public boolean addGoods(GoodsType type, int amount) {
        int oldAmount = getGoodsCount(type);
        int newAmount = oldAmount + amount;

        if (newAmount < 0) {
            throw new IllegalStateException("Operation would leave "
                + newAmount + " goods of type "
                + type.getNameKey() + " in Location " + parent);
        } else if (newAmount == 0) {
            storedGoods.remove(type);
        } else {
            storedGoods.put(type, newAmount);
        }
        return true;
    }

    /**
     * Removes Goods from this containter.
     * @param g The Goods to remove from this container.
     */
    public Goods removeGoods(AbstractGoods g) {
        return removeGoods(g.getType(), g.getAmount());
    }

    public Goods removeGoods(GoodsType type) {
        return removeGoods(type, INFINITY);
    }

    /**
     * Removes the given amount of the given type of goods.
     *
     * @param type The type of goods to remove.
     * @param amount The type of amount to remove.
     * @return A Goods with the requested or available amount that has
     *      been removed, or null if none was removed.
     */
    public Goods removeGoods(GoodsType type, int amount) {
        int oldAmount = getGoodsCount(type);
        if (oldAmount <= 0) return null;
        int newAmount = oldAmount - amount;
        Goods removedGoods;
        if (newAmount > 0) {
            removedGoods = new Goods(getGame(), parent, type, amount);
            storedGoods.put(type, newAmount);
        } else {
            removedGoods = new Goods(getGame(), parent, type, oldAmount);
            storedGoods.remove(type);
        }
        return removedGoods;
    }

    /**
     * Removes all goods above given amount, provided that the goods
     * are storable and do not ignore warehouse limits.
     *
     * @param newAmount The treshold.
     */
    public void removeAbove(int newAmount) {
        for (GoodsType goodsType : storedGoods.keySet()) {
            if (goodsType.isStorable() && !goodsType.limitIgnored() &&
                storedGoods.get(goodsType) > newAmount) {
                setAmount(goodsType, newAmount);
            }
        }
    }

    /**
     * Set the amount of goods in this container.
     *
     * @param goodsType The <code>GoodsType</code> to set the amount of.
     * @param newAmount The new amount.
     */
    public void setAmount(GoodsType goodsType, int newAmount) {
        if (newAmount == 0) {
            storedGoods.remove(goodsType);
        } else {
            storedGoods.put(goodsType, newAmount);
        }
    }

    /**
     * Removes all goods.
     *
     */
    public void removeAll() {
        storedGoods.clear();
    }


    /**
     * Checks if any storable type of goods has reached the given
     * amount.
     *
     * @param amount The amount.
     * @return <code>true</code> if any type of goods,
     * except for <code>Goods.FOOD</code>, has reached
     * the given amount.
     */
    public boolean hasReachedCapacity(int amount) {
        for (GoodsType goodsType : storedGoods.keySet()) {
            if (goodsType.isStorable() && !goodsType.limitIgnored() &&
                storedGoods.get(goodsType) > amount) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the specified <code>Goods</code> is in this container.
     *
     * @param g The <code>Goods</code> to test the presence of.
     * @return The result.
     */
    public boolean contains(Goods g) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the amount of one type of goods in this container.
     *
     * @param type The <code>GoodsType</code> being looked for.
     * @return The amount of this type of goods in this container.
     */
    public int getGoodsCount(GoodsType type) {
        return (storedGoods.containsKey(type)) 
            ? storedGoods.get(type).intValue()
            : 0;
    }

    /**
     * Gets the amount of one type of goods at the beginning of the turn.
     *
     * @param type The <code>GoodsType</code> being looked for.
     * @return The amount of this type of goods in this container at
     *     the beginning of the turn
     */
    public int getOldGoodsCount(GoodsType type) {
        return (oldStoredGoods.containsKey(type))
            ? oldStoredGoods.get(type).intValue()
            : 0;
    }

    /**
     * Gets the amount of space that the goods in this container will consume.
     * Each occupied cargo slot contains an amount in [1, CARGO_SIZE].
     *
     * @return The amount of space taken by this containers goods.
     */
    public int getSpaceTaken() {
        int count = 0;
        for (Integer amount : storedGoods.values()) {
            if (amount % CARGO_SIZE == 0) {
                count += amount/CARGO_SIZE;
            } else {
                count += amount/CARGO_SIZE + 1;
            }
        }
        return count;
    }


    /**
     * Gets an <code>Iterator</code> of every <code>Goods</code> in this
     * <code>GoodsContainer</code>. Each <code>Goods</code> have a maximum
     * amount of CARGO_SIZE.
     *
     * @return The <code>Iterator</code>.
     * @see #getCompactGoods
     */
    public Iterator<Goods> getGoodsIterator() {
        return getGoods().iterator();
    }

    /**
     * Returns an <code>ArrayList</code> containing all
     * <code>Goods</code> in this <code>GoodsContainer</code>. Each
     * <code>Goods</code> has a maximum amount of CARGO_SIZE.
     *
     * @return The <code>ArrayList</code>.
     * @see #getGoodsIterator
     */
    public List<Goods> getGoods() {
        ArrayList<Goods> totalGoods = new ArrayList<Goods>();

        for (GoodsType goodsType : storedGoods.keySet()) {
            int amount = storedGoods.get(goodsType).intValue();
            while (amount > 0) {
                totalGoods.add(new Goods(getGame(), parent, goodsType, (amount >= CARGO_SIZE ? CARGO_SIZE : amount)));
                amount -= CARGO_SIZE;
            }
        }

        return totalGoods;
    }


    /**
     * Gets an <code>Iterator</code> of every <code>Goods</code> in this
     * <code>GoodsContainer</code>. There is only one <code>Goods</code>
     * for each type of goods.
     *
     * @return The <code>Iterator</code>.
     * @see #getGoodsIterator
     */
    public List<Goods> getCompactGoods() {
        ArrayList<Goods> totalGoods = new ArrayList<Goods>();

        for (Entry<GoodsType, Integer> entry : storedGoods.entrySet()) {
            if (entry.getValue() > 0) {
                totalGoods.add(new Goods(getGame(), parent, entry.getKey(), entry.getValue()));
            }
        }

        return totalGoods;
    }

    /**
     * Gets an <code>Iterator</code> of every <code>Goods</code> in this
     * <code>GoodsContainer</code>. There is only one <code>Goods</code>
     * for each type of goods.
     *
     * @return The <code>Iterator</code>.
     * @see #getGoodsIterator
     */
    public List<Goods> getFullGoods() {
        ArrayList<Goods> totalGoods = new ArrayList<Goods>();

        for (GoodsType goodsType : storedGoods.keySet()) {
            totalGoods.add(new Goods(getGame(), parent, goodsType, storedGoods.get(goodsType)));
        }

        return totalGoods;
    }

    /**
     * Prepares this <code>GoodsContainer</code> for a new turn.
     */
    public void saveState() {
        oldStoredGoods.clear();
        for (Map.Entry<GoodsType, Integer> entry : storedGoods.entrySet()) {
            oldStoredGoods.put(entry.getKey(), new Integer(entry.getValue().intValue()));
        }
    }

    /**
     * Has this goods containers contents changed from what was recorded
     * last time the state was saved?
     *
     * @return True if the contents have changed.
     */
    public boolean hasChanged() {
        for (GoodsType type : getSpecification().getGoodsTypeList()) {
            int oldCount = getOldGoodsCount(type);
            int newCount = getGoodsCount(type);
            if (oldCount != newCount) return true;
        }
        return false;
    }

    /**
     * Fire property changes for all goods that have seen level changes
     * since the last saveState().
     */
    public void fireChanges() {
        for (GoodsType type : getSpecification().getGoodsTypeList()) {
            int oldCount = getOldGoodsCount(type);
            int newCount = getGoodsCount(type);
            if (oldCount != newCount) {
                firePropertyChange(type.getId(), oldCount, newCount);
            }
        }
        oldStoredGoods.clear();
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * <br><br>
     *
     * Only attributes visible to the given <code>Player</code> will
     * be added to that representation if <code>showAll</code> is
     * set to <code>false</code>.
     *
     * @param out The target stream.
     * @param player The <code>Player</code> this XML-representation
     *      should be made for, or <code>null</code> if
     *      <code>showAll == true</code>.
     * @param showAll Only attributes visible to <code>player</code>
     *      will be added to the representation if <code>showAll</code>
     *      is set to <i>false</i>.
     * @param toSavedGame If <code>true</code> then information that
     *      is only needed when saving a game is added.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out, Player player,
                             boolean showAll, boolean toSavedGame)
        throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute(ID_ATTRIBUTE_TAG, getId());
        if (showAll || toSavedGame || player == getOwner()) {
            writeStorage(out, STORED_GOODS_TAG, storedGoods);
            writeStorage(out, OLD_STORED_GOODS_TAG, oldStoredGoods);
        }
        out.writeEndElement();
    }

    private void writeStorage(XMLStreamWriter out, String tag, Map<GoodsType, Integer> storage)
        throws XMLStreamException {
        if (!storage.isEmpty()) {
            out.writeStartElement(tag);
            for (Map.Entry<GoodsType, Integer> entry : storage.entrySet()) {
                out.writeStartElement(Goods.getXMLElementTagName());
                out.writeAttribute("type", entry.getKey().getId());
                out.writeAttribute("amount", entry.getValue().toString());
                out.writeEndElement();
            }
            out.writeEndElement();
        }
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     */
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        setId(readId(in));
        storedGoods.clear();
        oldStoredGoods.clear();
    }

    @Override
    protected void readChildren(XMLStreamReader in) throws XMLStreamException {
        Map<GoodsType, Integer> storage;
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals(STORED_GOODS_TAG)) {
                storage = storedGoods;
            } else if (in.getLocalName().equals(OLD_STORED_GOODS_TAG)) {
                storage = oldStoredGoods;
            } else {
                continue;
            }
            while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
                if (in.getLocalName().equals(Goods.getXMLElementTagName())) {
                    GoodsType goodsType = getGame().getSpecification()
                        .getGoodsType(in.getAttributeValue(null, "type"));
                    Integer amount = new Integer(in.getAttributeValue(null, "amount"));
                    storage.put(goodsType, amount);
                }
                in.nextTag();
            }
        }
    }


    /**
     * Partial writer, so that "remove" messages can be brief.
     *
     * @param out The target stream.
     * @param fields The fields to write.
     * @throws XMLStreamException If there are problems writing the stream.
     */
    @Override
    protected void toXMLPartialImpl(XMLStreamWriter out, String[] fields)
        throws XMLStreamException {
        toXMLPartialByClass(out, getClass(), fields);
    }

    /**
     * Partial reader, so that "remove" messages can be brief.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException If there are problems reading the stream.
     */
    @Override
    public void readFromXMLPartialImpl(XMLStreamReader in)
        throws XMLStreamException {
        readFromXMLPartialByClass(in, getClass());
    }

    /**
     * Creates a <code>String</code> representation of this
     * <code>GoodsContainer</code>.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer(200);
        sb.append("GoodsContainer with: ");
        for (Map.Entry<GoodsType, Integer> entry : storedGoods.entrySet()) {
            sb.append(entry.getKey() + "=" + entry.getValue() + ", ");
        }
        sb.setLength(sb.length() - 2);
        return sb.toString();
    }

    /**
     * Gets the tag name of the root element representing this object.
     * @return "goodsContainer".
     */
    public static String getXMLElementTagName() {
        return "goodsContainer";
    }
}
