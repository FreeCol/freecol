/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;

import org.w3c.dom.Element;


/**
 * The <code>UnitLocation</code> is a place where a <code>Unit</code>
 * can be put.  The UnitLocation can not store any other Locatables,
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

    /** The Units present in this Location. */
    private final List<Unit> units = new ArrayList<Unit>();


    /**
     * Creates a new <code>UnitLocation</code> instance.
     *
     * @param game The enclosing <code>Game</code>.
     */
    public UnitLocation(Game game) {
        super(game);
    }

    /**
     * Creates a new <code>UnitLocation</code> instance.
     *
     * @param game The enclosing <code>Game</code>.
     * @param id The object identifier.
     */
    public UnitLocation(Game game, String id) {
        super(game, id);
    }

    /**
     * Creates a new <code>UnitLocation</code> instance.
     * Only Unit needs this.  TODO: make it go away, its a noop.
     *
     * @param game The enclosing <code>Game</code>.
     * @param e The <code>Element</code> to read from.
     */
    public UnitLocation(Game game, Element e) {
        super(game, null);
    }


    // Some useful utilities, marked final as they will work as long
    // as working implementations of getUnitList(), getUnitCount(),
    // getUnitCapacity() and getSettlement() are provided.

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

    /**
     * Gets the total amount of Units at this Location, including
     * units on a carrier.
     *
     * @return The total number of <code>Unit</code>s at this
     *     <code>Location</code>.
     */
    public int getTotalUnitCount() {
        int result = 0;
        for (Unit unit : getUnitList()) {
            result++;
            result += unit.getUnitCount();
        }
        return result;
    }

    /**
     * Checks if there is a useable carrier unit with a specified
     * minimum amount of space available in this location.
     *
     * @param space The amount of space to require.
     * @return True if there is a suitable unit present.
     * @see Unit#isCarrier
     */
    public boolean hasCarrierWithSpace(int space) {
        for (Unit u : getUnitList()) {
            if (u.isCarrier()
                && !u.isDamaged()
                && u.getSpaceLeft() >= space) return true;
        }
        return false;
    }


    // Override FreeColGameObject

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FreeColGameObject> disposeList() {
        List<FreeColGameObject> objects = new ArrayList<FreeColGameObject>();
        while (!units.isEmpty()) {
            objects.addAll(units.remove(0).disposeList());
        }
        objects.addAll(super.disposeList());
        return objects;
    }


    // Interface Location
    // Inheriting
    //   FreeColObject.getId()

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
            logger.finest("Dumped " + locatable + " in UnitLocation with id "
                          + getId());
            return true;
        } else {
            logger.warning("Tried to add Locatable " + locatable
                           + " to UnitLocation with id " + getId() + ".");
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
     */
    public final Iterator<Unit> getUnitIterator() {
        // Marked final as this will always work if getUnitList() does.
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
    public final Colony getColony() {
        // Final as this will always work if getSettlement() does.
        Settlement settlement = getSettlement();
        return (settlement instanceof Colony) ? (Colony)settlement : null;
    }

    /**
     * {@inheritDoc}
     */
    public final IndianSettlement getIndianSettlement() {
        // Final as this will always work if getSettlement() does.
        Settlement settlement = getSettlement();
        return (settlement instanceof IndianSettlement)
            ? (IndianSettlement)settlement : null;
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
    // end @compat

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
     * TODO: consider moving this up to Location?
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
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        for (Unit unit : units) {
            unit.toXML(xw);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        units.clear();

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final String tag = xr.getLocalName();

        if (Unit.getXMLElementTagName().equals(tag)) {
            units.add(xr.readFreeColGameObject(getGame(), Unit.class));

        } else {
            super.readChild(xr);
        }
    }
}
