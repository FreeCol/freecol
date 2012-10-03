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
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Element;

/**
 * The <code>UnitLocation</code> is a place where a <code>Unit</code>
 * can be put. The UnitLocation can not store any other Locatables,
 * such as {@link Goods}, or {@link TileItem}s.
 *
 * @see Locatable
 */
public abstract class UnitLocation extends FreeColGameObject implements Location {

    private static final Logger logger = Logger.getLogger(UnitLocation.class.getName());

    public static enum NoAddReason {
        /**
         * No reason why Locatable can not be added.
         */
        NONE,
        /**
         * Unit is already in the location.
         */
        ALREADY_PRESENT,
        /**
         * Locatable can not be added because it has the wrong
         * type. E.g. a {@link Building} can not be added to a
         * {@link Unit}.
         */
        WRONG_TYPE,
        /**
         * Locatable can not be added because the Location is already
         * full.
         */
        CAPACITY_EXCEEDED,
        /**
         * Locatable can not be added because the Location is
         * occupied by objects belonging to another player.
         */
        OCCUPIED_BY_ENEMY,
        /**
         * Locatable can not be added because the Location belongs
         * to another player and does not admit foreign objects.
         */
        OWNED_BY_ENEMY,
        // Enums can not be extended, so ColonyTile-specific failure reasons
        // have to be here.
        /**
         * Claimed and in use by another of our colonies.
         */
        ANOTHER_COLONY,
        /**
         * Can not add to settlement center tile.
         */
        COLONY_CENTER,
        /**
         * Missing ability to work colony tile.
         * Currently only produceInWater, which is assumed by the error message
         */
        MISSING_ABILITY,
        /**
         * Missing skill to work colony tile.
         */
        MISSING_SKILL,
        /**
         * Either unclaimed or claimed but could be acquired.
         */
        CLAIM_REQUIRED,
    }

    /**
     * The Units present in this Location.
     */
    private final List<Unit> units = new ArrayList<Unit>();


    protected UnitLocation() {
        // empty constructor
    }

    /**
     * Creates a new <code>UnitLocation</code> instance.
     *
     * @param game a <code>Game</code> value
     */
    public UnitLocation(Game game) {
        super(game);
    }

    /**
     * Creates a new <code>UnitLocation</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param in a <code>XMLStreamReader</code> value
     * @exception XMLStreamException if an error occurs
     */
    public UnitLocation(Game game, XMLStreamReader in)
        throws XMLStreamException {
        super(game, in);
    }

    /**
     * Creates a new <code>UnitLocation</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param id a <code>String</code> value
     */
    public UnitLocation(Game game, String id) {
        super(game, id);
    }

    // Only Unit needs this
    public UnitLocation(Game game, Element e) {
        super(game, e);
    }

    /**
     * Removes all references to this object.
     *
     * @return A list of disposed objects.
     */
    public List<FreeColGameObject> disposeList() {
        List<FreeColGameObject> objects = new ArrayList<FreeColGameObject>();
        while (!units.isEmpty()) {
            objects.addAll(units.remove(0).disposeList());
        }
        objects.addAll(super.disposeList());
        return objects;
    }


    // Some useful utilities, marked final as they will work as long
    // as working implementations of getUnitList(), getUnitCount(),
    // and getUnitCapacity() are provided.

    /**
     * Is this unit location empty?
     *
     * @return a <code>boolean</code> value
     */
    public final boolean isEmpty() {
        return getUnitCount() == 0;
    }

    /**
     * Is this unit location full?
     *
     * @return True if this location is full.
     */
    public final boolean isFull() {
        return getUnitCount() >= getUnitCapacity();
    }

    /**
     * Gets the first Unit at this unit location.
     *
     * @return The first <code>Unit</code>.
     */
    public final Unit getFirstUnit() {
        if (isEmpty()) return null;
        List<Unit> units = getUnitList();
        return units.get(0);
    }

    /**
     * Gets the last Unit at this unit location.
     *
     * @return The last <code>Unit</code>.
     */
    public final Unit getLastUnit() {
        if (isEmpty()) return null;
        List<Unit> units = getUnitList();
        return units.get(units.size()-1);
    }


    // Interface Location

    // getId() inherited from FreeColGameObject

    /**
     * {@inheritDoc}
     */
    public Tile getTile() {
        return null; // Override this where it becomes meaningful.
    }

    /**
     * {@inheritDoc}
     */
    public StringTemplate getLocationName() {
        return StringTemplate.key(getId());
    }

    /**
     * {@inheritDoc}
     */
    public StringTemplate getLocationNameFor(Player player) {
        return getLocationName();
    }

    /**
     * {@inheritDoc}
     */
    public boolean add(Locatable locatable) {
        if (locatable instanceof Unit) {
            Unit unit = (Unit) locatable;
            if (contains(unit)) {
                return true;
            } else if (canAdd(unit)) {
                return units.add(unit);
            }
        } else if (locatable instanceof Goods) {
            // dumping goods is a valid action
            locatable.setLocation(null);
            logger.finest("Dumped " + locatable + " in UnitLocation with ID "
                          + getId());
            return true;
        } else {
            logger.warning("Tried to add Locatable " + locatable
                           + " to UnitLocation with ID " + getId() + ".");
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(Locatable locatable) {
        if (locatable instanceof Unit) {
            return units.remove((Unit)locatable);
        } else {
            logger.warning("Tried to remove non-Unit " + locatable
                           + " from UnitLocation: " + getId());
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(Locatable locatable) {
        return (locatable instanceof Unit) ? units.contains((Unit)locatable)
            : false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean canAdd(Locatable locatable) {
        return getNoAddReason(locatable) == NoAddReason.NONE;
    }

    /**
     * {@inheritDoc}
     */
    public int getUnitCount() {
        return units.size();
    }

    /**
     * {@inheritDoc}
     */
    public List<Unit> getUnitList() {
        return new ArrayList<Unit>(units);
    }

    /**
     * {@inheritDoc}
     *
     * Note: Marked final as this will always work if getUnitList() does.
     */
    public final Iterator<Unit> getUnitIterator() {
        return getUnitList().iterator();
    }

    /**
     * {@inheritDoc}
     */
    public GoodsContainer getGoodsContainer() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Settlement getSettlement() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Colony getColony() {
        return null;
    }


    // Overrideable routines to be implemented by UnitLocation subclasses.

    /**
     * Gets the current space taken by the units in this location.
     *
     * Note that Units are also unit locations, but their space taken is
     * derived from the spec, so this routine must be overrideable.
     *
     * @return The sum of the space taken by the units in this location.
     */
    public int getSpaceTaken() {
        int space = 0;
        for (Unit u : getUnitList()) space += u.getSpaceTaken();
        return space;
    }

    /**
     * Move the given unit to the front of the units list.
     *
     * @param u The <code>Unit</code> to move to the front.
     */
    public void moveToFront(Unit u) {
        if (units.remove(u)) units.add(0, u);
    }

    // @compat 0.10.5
    protected void clearUnitList() {
        units.clear();
    }

    /**
     * Gets the reason why a given <code>Locatable</code> can not be
     * added to this Location.
     *
     * Be careful to test for unit presence last before success
     * (NoAddReason.NONE) except perhaps for the capacity test, so
     * that we can treat ALREADY_PRESENT as success in some cases
     * (e.g. if the unit changes type --- does it still have a
     * required skill?)
     *
     * TODO: consider moving this up to Location.
     *
     * @param locatable The <code>Locatable</code> to test.
     * @return The reason why adding would fail.
     */
    public NoAddReason getNoAddReason(Locatable locatable) {
        Unit unit = (locatable instanceof Unit) ? (Unit)locatable : null;
        return (unit == null)
            ? NoAddReason.WRONG_TYPE
            : (units == null)
            ? NoAddReason.CAPACITY_EXCEEDED
            : (!isEmpty() && units.get(0).getOwner() != unit.getOwner())
            ? NoAddReason.OCCUPIED_BY_ENEMY
            : (contains(unit))
            ? NoAddReason.ALREADY_PRESENT
            : (unit.getSpaceTaken() + getSpaceTaken() > getUnitCapacity())
            ? NoAddReason.CAPACITY_EXCEEDED
            : NoAddReason.NONE;
    }

    /**
     * Gets the maximum number of <code>Units</code> this Location
     * can hold.  To be overridden by subclasses.
     *
     * @return Integer.MAX_VALUE, denoting no effective limit.
     */
    public int getUnitCapacity() {
        return Integer.MAX_VALUE;
    }


    // Serialization

    /**
     * {@inheritDoc}
     */
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        out.writeAttribute(ID_ATTRIBUTE, getId());
    }

    /**
     * Serialize the children of this UnitLocation, that is the units
     * themselves.
     *
     * @param out a <code>XMLStreamWriter</code> value
     * @param player a <code>Player</code> value
     * @param showAll a <code>boolean</code> value
     * @param toSavedGame a <code>boolean</code> value
     * @exception XMLStreamException if an error occurs
     */
    protected void writeChildren(XMLStreamWriter out, Player player,
                                 boolean showAll, boolean toSavedGame)
        throws XMLStreamException {
        for (Unit unit : units) {
            unit.toXML(out, player, showAll, toSavedGame);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void readChildren(XMLStreamReader in) throws XMLStreamException {
        units.clear();
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            readChild(in);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        if (Unit.getXMLElementTagName().equals(in.getLocalName())) {
            Unit unit = updateFreeColGameObject(in, Unit.class);
            if (!units.contains(unit)) units.add(unit);
        } else {
            logger.warning("Found unknown child element '" + in.getLocalName()
                + "' of UnitLocation " + getId() + ".");
            in.nextTag();
        }
    }
}
