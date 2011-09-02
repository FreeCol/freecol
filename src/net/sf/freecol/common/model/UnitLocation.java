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
    public UnitLocation(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);
    }

    /**
     * Creates a new <code>UnitLocation</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param e an <code>Element</code> value
     */
    public UnitLocation(Game game, Element e) {
        super(game, e);
        readFromXMLElement(e);
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

    /**
     * Gets the maximum number of <code>Units</code> this Location
     * can hold.  To be overridden by subclasses.
     *
     * @return Integer.MAX_VALUE, denoting no effective limit.
     */
    public int getUnitCapacity() {
        return Integer.MAX_VALUE;
    }

    /**
     * Gets the current space taken by the units in this location.
     *
     * @return The sum of the space taken by the units in this location.
     */
    public int getSpaceTaken() {
        int space = 0;
        for (Unit u : units) space += u.getSpaceTaken();
        return space;
    }

    /**
     * Returns the name of this location.
     *
     * @return The name of this location.
     */
    public StringTemplate getLocationName() {
        return StringTemplate.key(getId());
    }

    /**
     * Returns the name of this location for a particular player.
     *
     * @param player The <code>Player</code> to return the name for.
     * @return The name of this location.
     */
    public StringTemplate getLocationNameFor(Player player) {
        return getLocationName();
    }

    /**
     * {@inheritDoc}
     */
    public boolean canAdd(Locatable locatable) {
        return getNoAddReason(locatable) == NoAddReason.NONE;
    }

    /**
     * Gets the reason why a given <code>Locatable</code> can not be
     * added to this Location.
     *
     * @param locatable The <code>Locatable</code> to test.
     * @return The reason why adding would fail.
     */
    public NoAddReason getNoAddReason(Locatable locatable) {
        Unit unit = (locatable instanceof Unit) ? (Unit) locatable : null;
        return (unit == null)
            ? NoAddReason.WRONG_TYPE
            : (units == null
                || unit.getSpaceTaken() + getSpaceTaken() > getUnitCapacity())
            ? NoAddReason.CAPACITY_EXCEEDED
            : (!isEmpty() && units.get(0).getOwner() != unit.getOwner())
            ? NoAddReason.OCCUPIED_BY_ENEMY
            // Always test this last before success (NoAddReason.NONE),
            // so that we can treat ALREADY_PRESENT as success in some
            // cases (e.g. if the unit changes type --- does it still
            // have a required skill?)
            : contains(unit)
            ? NoAddReason.ALREADY_PRESENT
            : NoAddReason.NONE;
    }

    /**
     * Adds a <code>Locatable</code> to this Location.
     *
     * @param locatable
     *            The <code>Locatable</code> to add to this Location.
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
     * Removes a <code>Locatable</code> from this Location.
     *
     * @param locatable
     *            The <code>Locatable</code> to remove from this Location.
     */
    public boolean remove(Locatable locatable) {
        if (locatable instanceof Unit) {
            return units.remove((Unit) locatable);
        } else {
            logger.warning("Tried to remove Locatable " + locatable
                           + " from UnitLocation with ID " + getId() + ".");
            return false;
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
        return units != null && units.contains(locatable);
    }

    /**
     * Returns <code>true</code> if this Location admits the given
     * <code>Ownable</code>. By default, this is the case if the
     * Location and the Ownable have the same owner, or if at least
     * one of the owners is <code>null</code>.
     *
     * @param ownable an <code>Ownable</code> value
     * @return a <code>boolean</code> value
     */
    /*
    public boolean admitsOwnable(Ownable ownable) {
        return (owner == null
                || ownable.getOwner() == null
                || owner == ownable.getOwner());
    }
    */

    /**
     * Returns the number of Units at this Location.
     *
     * @return The number of Units at this Location.
     */
    public int getUnitCount() {
        return units.size();
    }

    /**
     * Returns true if there are no Units present in this Location.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isEmpty() {
        return units.isEmpty();
    }

    /**
     * Is this unit location full?
     *
     * @return True if this location is full.
     */
    public boolean isFull() {
        return getUnitCount() >= getUnitCapacity();
    }

    /**
     * Gets the Units present at this Location.
     *
     * @return A copy of the list containing the Units present at this location.
     */
    public List<Unit> getUnitList() {
        return new ArrayList<Unit>(units);
    }

    /**
     * Gets a <code>Iterator</code> of every <code>Unit</code> directly
     * located on this <code>Location</code>.
     *
     * @return The <code>Iterator</code>.
     */
    public Iterator<Unit> getUnitIterator() {
        return getUnitList().iterator();
    }

    /**
     * Returns the <code>Tile</code> where this <code>Location</code>
     * is located, or <code>null</code> if it is not located on a Tile.
     *
     * @return a <code>Tile</code> value
     */
    public Tile getTile() {
        return null;
    }

    /**
     * Returns the <code>Colony</code> this <code>Location</code> is
     * located in, or <code>null</code> if it is not located in a colony.
     *
     * @return A <code>Colony</code>
     */
    public Colony getColony() {
        return null;
    }

    /**
     * Returns the <code>Settlement</code> this <code>Location</code>
     * is located in, or <code>null</code> if it is not located in any
     * settlement.
     *
     * @return a <code>Settlement</code> value
     */
    public Settlement getSettlement() {
        return null;
    }

    /**
     * Gets the <code>GoodsContainer</code> this <code>Location</code>
     * use for storing it's goods, or <code>null</code> if the
     * <code>Location</code> cannot store any goods.
     *
     * @return A <code>GoodsContainer</code> value
     */
    public GoodsContainer getGoodsContainer() {
        return null;
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

    /**
     * Dispose of this UnitLocation.
     */
    public void dispose() {
        disposeList();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        out.writeAttribute(ID_ATTRIBUTE, getId());
    }

    /**
     * {@inheritDoc}
     */
    protected void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        writeChildren(out, null, true, true);
    }

    /**
     * Serialize the children of this UnitLocation, i.e. the Units
     * themselves.
     *
     * @param out a <code>XMLStreamWriter</code> value
     * @param player a <code>Player</code> value
     * @param showAll a <code>boolean</code> value
     * @param toSavedGame a <code>boolean</code> value
     * @exception XMLStreamException if an error occurs
     */
    protected void writeChildren(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame)
        throws XMLStreamException {
        if (!isEmpty()) {
            for (Unit unit : getUnitList()) {
                unit.toXML(out, player, showAll, toSavedGame);
            }
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


    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        if (Unit.getXMLElementTagName().equals(in.getLocalName())) {
            Unit unit = updateFreeColGameObject(in, Unit.class);
            if (!units.contains(unit)) {
                units.add(unit);
            }
        } else {
            logger.warning("Found unknown child element '" + in.getLocalName() + "'.");
            in.nextTag();
        }
    }

}
