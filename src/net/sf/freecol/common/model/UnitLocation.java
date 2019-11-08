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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.StringUtils.*;


/**
 * The {@code UnitLocation} is a place where a {@code Unit}
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
         * Missing ability to work colony tile or building.
         * Currently only produceInWater, which is assumed by the error message
         */
        MISSING_ABILITY,
        /**
         * The unit has no skill.
         */
        MISSING_SKILL,
        /**
         * The unit does not have the minimum skill required.
         */
        MINIMUM_SKILL,
        /**
         * The unit exceeds the maximum skill of this type.
         */
        MAXIMUM_SKILL,
        /**
         * Either unclaimed or claimed but could be acquired.
         */
        CLAIM_REQUIRED;

        /**
         * Get a message key describing this reason.
         *
         * @return A message key.
         */
        private String getKey() {
            return "noAddReason." + getEnumKey(this);
        }

        /**
         * Get the description key.
         *
         * @return The description key.
         */
        public String getDescriptionKey() {
            return Messages.descriptionKey("model." + getKey());
        }
    }

    /** The Units present in this Location. */
    private final List<Unit> units = new ArrayList<>();


    /**
     * Creates a new {@code UnitLocation} instance.
     *
     * @param game The enclosing {@code Game}.
     */
    public UnitLocation(Game game) {
        super(game);
    }

    /**
     * Creates a new {@code UnitLocation} instance.
     *
     * @param game The enclosing {@code Game}.
     * @param id The object identifier.
     */
    public UnitLocation(Game game, String id) {
        super(game, id);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void intern() {
        super.intern();
        for (Unit u : getUnitList()) u.intern();
    }
    
    /**
     * Internal addition of a unit to this location.
     *
     * @param u The {@code Unit} to add.
     */
    private void addUnit(Unit u) {
        if (u == null) return;
        if (u.getLocation() != this) {
            logger.warning("Fixing bogus unit location for " + u.getId()
                + ", expected " + this
                + " but found " + u.getLocation());
            u.setLocationNoUpdate(this);
        }
        synchronized (this.units) {
            this.units.add(u);
        }
    }

    // Some useful utilities, marked final as they will work as long
    // as working implementations of getUnits/List(), getUnitCount(),
    // getUnitCapacity() and getSettlement() are provided.

    /**
     * Is this unit location empty?
     *
     * @return a {@code boolean} value
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
     * @return The first {@code Unit}.
     */
    public final Unit getFirstUnit() {
        return first(getUnits());
    }

    /**
     * Gets the last Unit at this unit location.
     *
     * @return The last {@code Unit}.
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
     * @return The total number of {@code Unit}s at this
     *     {@code Location}.
     */
    public int getTotalUnitCount() {
        return sum(getUnits(), u -> 1 + u.getUnitCount());
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
        return any(getUnits(), u -> u.isCarrier() && !u.isDamaged()
            && u.getSpaceLeft() >= space);
    }

    /**
     * Gets a list of all naval units here.
     *
     * @return A list of naval {@code Unit}s present.
     */
    public List<Unit> getNavalUnits() {
        return transform(getUnits(), Unit::isNaval);
    }

    /**
     * Gets a carrier for the supplied unit, if one exists.
     *
     * @param unit The {@code Unit} to carry.
     * @return A suitable carrier or null if none found.
     */
    public Unit getCarrierForUnit(Unit unit) {
        return find(getUnits(), u -> u.couldCarry(unit));
    }


    // Override FreeColGameObject

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<FreeColGameObject> getDisposables() {
        synchronized (this.units) {
            return concat(flatten(this.units, Unit::getDisposables),
                          super.getDisposables());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disposeResources() {
        synchronized (this.units) {
            this.units.clear();
        }
        super.disposeResources();
    }


    // Interface Location
    // Inheriting
    //   FreeColObject.getId()
    // Does not implement getRank()

    /**
     * {@inheritDoc}
     */
    @Override
    public Tile getTile() {
        return null; // Override this where it becomes meaningful.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StringTemplate getLocationLabel() {
        return StringTemplate.key(getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StringTemplate getLocationLabelFor(Player player) {
        return getLocationLabel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean add(Locatable locatable) {
        if (locatable instanceof Unit) {
            Unit unit = (Unit) locatable;
            if (contains(unit)) {
                return true;
            } else if (canAdd(unit)) {
                synchronized (this.units) {
                    if (!this.units.add(unit)) return false;
                }
                unit.setLocationNoUpdate(this);
                return true;
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
    @Override
    public boolean remove(Locatable locatable) {
        if (locatable instanceof Unit) {
            Unit unit = (Unit)locatable;
            synchronized (this.units) {
                if (!this.units.remove(unit)) return false;
            }
            unit.setLocationNoUpdate(null);
            return true;
        } else {
            logger.warning("Tried to remove non-Unit " + locatable
                           + " from UnitLocation: " + getId());
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(Locatable locatable) {
        if (!(locatable instanceof Unit)) return false;
        synchronized (this.units) {
            return this.units.contains(locatable);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canAdd(Locatable locatable) {
        return getNoAddReason(locatable) == NoAddReason.NONE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getUnitCount() {
        synchronized (this.units) {
            return this.units.size();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<Unit> getUnits() {
        synchronized (this.units) {
            return getUnitList().stream();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Unit> getUnitList() {
        synchronized (this.units) {
            return (this.units.isEmpty()) ? Collections.<Unit>emptyList()
                : new ArrayList<>(this.units);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GoodsContainer getGoodsContainer() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Settlement getSettlement() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Colony getColony() {
        // Final as this will always work if getSettlement() does.
        Settlement settlement = getSettlement();
        return (settlement instanceof Colony) ? (Colony)settlement : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IndianSettlement getIndianSettlement() {
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
        return sum(getUnits(), Unit::getSpaceTaken);
    }

    /**
     * Move the given unit to the front of the units list.
     *
     * @param u The {@code Unit} to move to the front.
     */
    public void moveToFront(Unit u) {
        synchronized (this.units) {
            if (this.units.remove(u)) this.units.add(0, u);
        }
    }

    /**
     * Clear the units from this container.
     */
    protected void clearUnitList() {
        synchronized (this.units) {
            this.units.clear();
        }
    }

    /**
     * Gets the reason why a given {@code Locatable} can not be
     * added to this Location.
     *
     * Be careful to test for unit presence last before success
     * (NoAddReason.NONE) except perhaps for the capacity test, so
     * that we can treat ALREADY_PRESENT as success in some cases
     * (e.g. if the unit changes type --- does it still have a
     * required skill?)
     *
     * FIXME: consider moving this up to Location?
     *
     * @param locatable The {@code Locatable} to test.
     * @return The reason why adding would fail.
     */
    public NoAddReason getNoAddReason(Locatable locatable) {
        Unit unit = (locatable instanceof Unit) ? (Unit)locatable : null;
        return (unit == null)
            ? NoAddReason.WRONG_TYPE
            : (!isEmpty() && getFirstUnit().getOwner() != unit.getOwner())
            ? NoAddReason.OCCUPIED_BY_ENEMY
            : (contains(unit))
            ? NoAddReason.ALREADY_PRESENT
            : (unit.getSpaceTaken() + getSpaceTaken() > getUnitCapacity())
            ? NoAddReason.CAPACITY_EXCEEDED
            : NoAddReason.NONE;
    }

    /**
     * Gets the maximum number of {@code Units} this Location
     * can hold.  To be overridden by subclasses.
     *
     * @return Integer.MAX_VALUE, denoting no effective limit.
     */
    public int getUnitCapacity() {
        return Integer.MAX_VALUE;
    }

    /**
     * What would the price be for a collection of goods at this location?
     *
     * @param goods The list of {@code AbstractGoods} to check for.
     * @return The net cost of acquiring the goods at this location.
     * @exception FreeColException if the goods are unavailable for any reason.
     */
    public int priceGoods(List<AbstractGoods> goods) throws FreeColException {
        if (goods.isEmpty()) return 0;
        throw new FreeColException("Not available: " + goods.get(0));
    }

    /**
     * Equip a unit for a role using resources at this location.
     *
     * @param unit The {@code Unit} to equip.
     * @param role The {@code Role} to build for.
     * @param roleCount The role count.
     * @return True if the equipping succeeded.
     */
    public boolean equipForRole(Unit unit, Role role, int roleCount) {
        return false;
    }


    // Overide FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        UnitLocation o = copyInCast(other, UnitLocation.class);
        if (o == null || !super.copyIn(o)) return false;
        final Game game = getGame();
        clearUnitList();
        for (Unit u : o.getUnitList()) { // Allow creation, unit may be new
            synchronized (this.units) {
                Unit nu = game.update(u, true);
                this.units.add(nu);
                nu.setLocationNoUpdate(this);
            }
        }
        return true;
    }


    // Serialization

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        synchronized (this.units) {
            // FIXME: Do *not* use getUnits/List here, because Colony lies!
            for (Unit unit : this.units) {
                if (unit.getLocation() != this) {
                    logger.warning("UnitLocation contains unit " + unit
                        + " with bogus location " + unit.getLocation()
                        + ", fixing.");
                    unit.setLocationNoUpdate(this);
                }
                unit.toXML(xw);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        clearUnitList();

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final String tag = xr.getLocalName();

        if (Unit.TAG.equals(tag)) {
            addUnit(xr.readFreeColObject(getGame(), Unit.class));

        } else {
            super.readChild(xr);
        }
    }

    // getXMLTagName left to subclasses
}
