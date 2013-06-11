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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.pathfinding.CostDecider;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.common.model.TradeRoute.Stop;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;
import net.sf.freecol.common.util.EmptyIterator;
import net.sf.freecol.common.util.Utils;

import org.w3c.dom.Element;


/**
 * Represents all pieces that can be moved on the map-board. This includes:
 * colonists, ships, wagon trains e.t.c.
 *
 * Every <code>Unit</code> is owned by a {@link Player} and has a
 * {@link Location}.
 */
public class Unit extends GoodsLocation
    implements Consumer, Locatable, Movable, Nameable, Ownable {

    private static final Logger logger = Logger.getLogger(Unit.class.getName());

    private static final EquipmentType horsesEq[] = { null, null };
    private static final EquipmentType musketsEq[] = { null, null };

    /** A comparator to order units by skill level. */
    private static Comparator<Unit> skillLevelComp
        = new Comparator<Unit>() {
            public int compare(Unit u1, Unit u2) {
                return u1.getSkillLevel() - u2.getSkillLevel();
            }
        };


    public static final String CARGO_CHANGE = "CARGO_CHANGE";
    public static final String EQUIPMENT_CHANGE = "EQUIPMENT_CHANGE";

    /** A state a Unit can have. */
    public static enum UnitState {
        ACTIVE,
        FORTIFIED,
        SENTRY,
        IN_COLONY,
        IMPROVING,
        // @compat 0.10.0
        TO_EUROPE,
        TO_AMERICA,
        // end compatibility code
        FORTIFYING,
        SKIPPED
    }

    /**
     * The roles a Unit can have. TODO: make this configurable by
     * adding roles to the specification instead of using an enum. New
     * roles, such as mounted pioneers, have already been suggested.
     */
    public static enum Role {
        DEFAULT, PIONEER, MISSIONARY, SOLDIER, SCOUT, DRAGOON;

        // Equipment types needed for certain roles.
        private static final HashMap<Role, List<EquipmentType>> roleEquipment
            = new HashMap<Role, List<EquipmentType>>();

        /**
         * Initializes roleEquipment.  How about that.
         */
        private void initializeRoleEquipment(Specification spec) {
            if (!roleEquipment.isEmpty()) return;
            UnitType defaultUnit = spec.getDefaultUnitType();
            for (EquipmentType e : spec.getEquipmentTypeList()) {
                Role r = e.getRole();
                if (r != null) {
                    List<EquipmentType> eq = roleEquipment.get(r);
                    if (eq == null) {
                        eq = new ArrayList<EquipmentType>();
                        roleEquipment.put(r, eq);
                    }
                    eq.add(e);
                }
            }
            // TODO: Not quite completely generic yet.  There are more
            // equipment types that are compatible with the dragoon role.
            // The spec expresses this with <compatible-equipment> but
            // it does not express that while muskets and horses are compatible
            // for a soldier, they are not for a scout.
            for (EquipmentType e : spec.getEquipmentTypeList()) {
                if (!e.isMilitaryEquipment()) continue;
                List<EquipmentType> eq = roleEquipment.get(Role.DRAGOON);
                if (eq == null) {
                    eq = new ArrayList<EquipmentType>();
                    roleEquipment.put(Role.DRAGOON, eq);
                }
                if (!eq.contains(e)) eq.add(e);
            }
            // Make sure there is an empty list at least for each role.
            for (Role r : Role.values()) {
                List<EquipmentType> e = roleEquipment.get(r);
                if (e == null) {
                    e = new ArrayList<EquipmentType>();
                    roleEquipment.put(r, e);
                }
            }
        }

        /**
         * Gets the equipment required for this role.
         * TODO: passing the spec in is a wart.
         *
         * @param spec The <code>Specification</code> to extract requirements
         *     from.
         * @return A list of required <code>EquipmentType</code>s.
         */
        public List<EquipmentType> getRoleEquipment(Specification spec) {
            initializeRoleEquipment(spec);
            return new ArrayList<EquipmentType>(roleEquipment.get(this));
        }

        public boolean isCompatibleWith(Role oldRole) {
            return (this == oldRole) ||
                (this == SOLDIER && oldRole == DRAGOON) ||
                (this == DRAGOON && oldRole == SOLDIER);
        }

        public Role newRole(Role role) {
            if (this == SOLDIER && role == SCOUT) {
                return DRAGOON;
            } else if (this == SCOUT && role == SOLDIER) {
                return DRAGOON;
            } else {
                return role;
            }
        }

        public String getId() {
            return toString().toLowerCase(Locale.US);
        }
    }

    /** The individual name of this unit, not of the unit type. */
    protected String name = null;

    /** The owner player. */
    protected Player owner;

    /** The unit type. */
    protected UnitType unitType;

    /** Current unit state. */
    protected UnitState state = UnitState.ACTIVE;

    /** Current unit role. */
    protected Role role = Role.DEFAULT;

    /** The current unit location. */
    protected Location location;

    /** The last entry location used by this unit. */
    protected Location entryLocation;

    /** The number of moves this unit has left this turn. */
    protected int movesLeft;

    /** What type of goods this unit produces in its occupation. */
    protected GoodsType workType;

    /** What type of goods this unit last earned experience producing. */
    private GoodsType experienceType;

    /** The mount of experience a unit has earned. */
    protected int experience = 0;

    /**
     * The number of turns until the work is finished (e.g. sailing,
     * improving), or '-1' if a Unit can stay in its state forever.
     */
    protected int workLeft;

    /**
     * What is being improved (to be used only for PIONEERs - where
     * they are working.
     */
    protected TileImprovement workImprovement;

    /** The student of this Unit, if it has one. */
    protected Unit student;

    /** The teacher of this Unit, if it has one. */
    protected Unit teacher;

    /** Number of turns of training needed by this unit. */
    protected int turnsOfTraining = 0;

    /** The original nationality. */
    protected String nationality = null;

    /** The original ethnicity. */
    protected String ethnicity = null;

    /** The home settlement of a native unit. */
    protected IndianSettlement indianSettlement = null;

    /** For now; only used by ships when repairing. */
    protected int hitPoints;

    /** A destination for go-to moves. */
    protected Location destination = null;

    /** The trade route this unit has. */
    protected TradeRoute tradeRoute = null;

    /** Which stop in a trade route the unit is going to. */
    protected int currentStop = -1;

    /** To be used only for type == TREASURE_TRAIN */
    protected int treasureAmount;

    /**
     * The attrition this unit has accumulated.  At the moment, this
     * equals the number of turns it has spent in the open.
     */
    protected int attrition = 0;

    /**
     * The amount of goods carried by this unit.  This variable is
     * only used by the clients.  A negative value signals that the
     * variable is not in use.
     *
     * @see #getVisibleGoodsCount()
     */
    protected int visibleGoodsCount;

    /** The equipment this Unit carries. */
    protected final TypeCountMap<EquipmentType> equipment
        = new TypeCountMap<EquipmentType>();


    /**
     * Constructor for ServerUnit.
     *
     * @param game The enclosing <code>Game</code>.
     */
    protected Unit(Game game) {
        super(game);
    }

    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param game The enclosing <code>Game</code>.
     * @param e An XML-element that will be used to initialize this object.
     */
    public Unit(Game game, Element e) {
        super(game, e);

        readFromXMLElement(e);
    }

    /**
     * Creates a new <code>Unit</code> with the given
     * identifier.  The object should later be initialized by calling
     * {@link #readFromXML(FreeColXMLReader)}.
     *
     * @param game The enclosing <code>Game</code>.
     * @param id The object identifier.
     */
    public Unit(Game game, String id) {
        super(game, id);
    }


    /**
     * Get the individual name of this unit.
     *
     * @return The individual name.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the individual name of this unit.
     *
     * @param newName The new name.
     */
    public void setName(String newName) {
        this.name = newName;
    }

    /**
     * Get the name of the apparent owner of this Unit,
     * (like getOwner().getNationAsString() but handles pirates).
     *
     * @return The name of the apparent owner of this <code>Unit</code>.
     */
    public StringTemplate getApparentOwnerName() {
        Player own = (hasAbility(Ability.PIRACY)) ? getGame().getUnknownEnemy()
            : owner;
        return own.getNationName();
    }

    /**
     * Get a description of a unit by name, type, role and number.
     *
     * @param name The unit name, if any.
     * @param type The <code>UnitType</code>.
     * @param role The <code>Role</code>.
     * @param number A unit count.
     * @return A <code>StringTemplate</code> describing a <code>Unit</code>.
     */
    public static StringTemplate getLabel(String name, UnitType type,
                                          Role role, int number) {
        StringTemplate result = StringTemplate.label(" ")
            .add(type.getNameKey());
        if (name != null) {
            result.addName(name);
        }
        if (role != Role.DEFAULT) {
            result = StringTemplate.template("model.unit." + role.getId()
                                             + ".name")
                .addAmount("%number%", number)
                .add("%unit%", type.getNameKey());
        }
        return result;
    }

    /**
     * Get a description of this unit.
     *
     * @return A <code>StringTemplate</code> describing this <code>Unit</code>.
     */
    public StringTemplate getLabel() {
        return getLabel(name, getType(), getRole(), 1);
    }

    /**
     * Get the <code>UnitType</code> value.
     *
     * @return The current <code>UnitType</code>.
     */
    public final UnitType getType() {
        return unitType;
    }

    /**
     * Sets the type of the unit.
     *
     * @param newUnitType The new type of the unit.
     */
    public void setType(UnitType newUnitType) {
        if (newUnitType.isAvailableTo(owner)) {
            if (unitType == null) {
                owner.modifyScore(newUnitType.getScoreValue());
            } else {
                owner.modifyScore(newUnitType.getScoreValue()
                    - unitType.getScoreValue());
            }
            this.unitType = newUnitType;
            if (getMovesLeft() > getInitialMovesLeft()) {
                setMovesLeft(getInitialMovesLeft());
            }
            hitPoints = unitType.getHitPoints();
            if (getTeacher() != null && !canBeStudent(getTeacher())) {
                getTeacher().setStudent(null);
                setTeacher(null);
            }
        } else {
            // ColonialRegulars for example are only available after
            // independence is declared.
            logger.warning("Units of type: " + newUnitType
                           + " are not available to " + owner.getPlayerType()
                           + " player " + owner.getName());
        }
    }

    /**
     * Checks if this <code>Unit</code> is naval.
     *
     * @return True if this is a naval <code>Unit</code>.
     */
    public boolean isNaval() {
        return (unitType == null) ? false : unitType.isNaval();
    }

    /**
     * Checks if this unit is an undead.
     *
     * @return True if the unit is undead.
     */
    public boolean isUndead() {
        return hasAbility("model.ability.undead");
    }

    /**
     * Can this unit carry treasure (like a treasure train)?
     *
     * @return True if this <code>Unit</code> can carry treasure.
     */
    public boolean canCarryTreasure() {
        return getType().hasAbility(Ability.CARRY_TREASURE);
    }

    /**
     * Can this unit capture enemy goods?
     *
     * @return True if this <code>Unit</code> is capable of capturing goods.
     */
    public boolean canCaptureGoods() {
        return unitType.hasAbility(Ability.CAPTURE_GOODS);
    }

    /**
     * Checks if this is a trading <code>Unit</code>, meaning that it
     * can trade with settlements.
     *
     * @return True if this is a trading unit.
     */
    public boolean isTradingUnit() {
        return canCarryGoods() && owner.isEuropean();
    }

    /**
     * Checks if this <code>Unit</code> is a `colonist'.  A unit is a
     * colonist if it is European and can build a new <code>Colony</code>.
     *
     * @return True if this unit is a colonist.
     */
    public boolean isColonist() {
        return unitType.hasAbility(Ability.FOUND_COLONY)
            && owner.hasAbility(Ability.FOUNDS_COLONIES);
    }

    /**
     * Checks if this <code>Unit</code> is able to carry {@link Locatable}s.
     *
     * @return True if this unit can carry goods or other units.
     */
    public boolean isCarrier() {
        return unitType.canCarryGoods() || unitType.canCarryUnits();
    }

    /**
     * Checks if this unit is a person, that is not a ship or wagon.
     * Surprisingly difficult without explicit enumeration because
     * model.ability.person only arrived in 0.10.1.
     *
     * @return True if this unit is a person.
     */
    public boolean isPerson() {
        return hasAbility("model.ability.person")
            // @compat 0.10.0
            || unitType.hasAbility(Ability.BORN_IN_COLONY)
            || unitType.hasAbility(Ability.BORN_IN_INDIAN_SETTLEMENT)
            || unitType.hasAbility(Ability.FOUND_COLONY)
            // Nick also had:
            //     && (!hasAbility("model.ability.carryGoods")
            //         && !hasAbility("model.ability.carryUnits")
            //         && !hasAbility("model.ability.carryTreasure")
            //         && !hasAbility("model.ability.bombard"))
            // ...but that should be unnecessary.
            // end compatibility code
            ;
    }

    /**
     * Gets the state of this <code>Unit</code>.
     *
     * @return The state of this <code>Unit</code>.
     */
    public UnitState getState() {
        return state;
    }

    /**
     * Checks if a <code>Unit</code> can get the given state set.
     *
     * @param s The new state for this Unit.  Should be one of
     *     {UnitState.ACTIVE, FORTIFIED, ...}.
     * @return True if the <code>Unit</code> state can be changed to
     *     the new value.
     */
    public boolean checkSetState(UnitState s) {
        switch (s) {
        case ACTIVE: case SENTRY:
            return true;
        case IN_COLONY:
            return !isNaval();
        case FORTIFIED:
            return getState() == UnitState.FORTIFYING;
        case IMPROVING:
            return location instanceof Tile
                && getOwner().canAcquireForImprovement(location.getTile());
        case SKIPPED:
            if (getState() == UnitState.ACTIVE) return true;
            // Fall through
        case FORTIFYING:
            return getMovesLeft() > 0;
        default:
            logger.warning("Invalid unit state: " + s);
            return false;
        }
    }

    /**
     * Sets a new state for this unit and initializes the amount of
     * work the unit has left.
     *
     * If the work needs turns to be completed (for instance when
     * plowing), then the moves the unit has still left will be used
     * up. Some work (basically building a road with a hardy pioneer)
     * might actually be finished already in this method-call, in
     * which case the state is set back to UnitState.ACTIVE.
     *
     * @param s The new state for this Unit.  Should be one of
     *     {UnitState.ACTIVE, UnitState.FORTIFIED, ...}.
     */
    public void setState(UnitState s) {
        if (state == s) {
            // No need to do anything when the state is unchanged
            return;
        } else if (!checkSetState(s)) {
            throw new IllegalStateException("Illegal UnitState transition: "
                + state + " -> " + s);
        } else {
            setStateUnchecked(s);
        }
    }

    /**
     * Actually set the unit state.
     *
     * @param s The new <code>UnitState</code>.
     */
    protected void setStateUnchecked(UnitState s) {
        // TODO: move to the server.
        // Cleanup the old UnitState, for example destroy the
        // TileImprovment being built by a pioneer.
        switch (state) {
        case IMPROVING:
            if (workImprovement != null && getWorkLeft() > 0) {
                if (!workImprovement.isComplete()
                    && workImprovement.getTile() != null
                    && workImprovement.getTile().getTileItemContainer() != null) {
                    workImprovement.getTile().getTileItemContainer()
                        .removeTileItem(workImprovement);
                }
                setWorkImprovement(null);
            }
            break;
        default:
            // do nothing
            break;
        }

        // Now initiate the new UnitState
        switch (s) {
        case ACTIVE:
            setWorkLeft(-1);
            break;
        case SENTRY:
            setWorkLeft(-1);
            break;
        case FORTIFIED:
            setWorkLeft(-1);
            movesLeft = 0;
            break;
        case FORTIFYING:
            setWorkLeft(1);
            movesLeft = 0;
            break;
        case IMPROVING:
            if (workImprovement == null) {
                setWorkLeft(-1);
            } else {
                setWorkLeft(workImprovement.getTurnsToComplete()
                    + ((getMovesLeft() > 0) ? 0 : 1));
            }
            movesLeft = 0;
            break;
        case SKIPPED: // do nothing
            break;
        default:
            setWorkLeft(-1);
        }
        state = s;
    }

    /**
     * Sets the given state to all the units that are carried.
     *
     * @param state The <code>UnitState</code> to set..
     */
    public void setStateToAllChildren(UnitState state) {
        if (canCarryUnits()) {
            for (Unit u : getUnitList()) u.setState(state);
        }
    }

    /**
     * Gets the unit role.
     *
     * @return The <code>Role</code> of this <code>Unit</code>.
     */
    public Role getRole() {
        return role;
    }
 
    /**
     * Sets the <code>Role</code> of this <code>Unit</code>.
     *
     * @param role The new <code>Role</code>.
     */
    public void setRole(Role role) {
        this.role = role;
    }

    /**
     * Set the unit role based on its equipment.
     */
    protected void setRole() {
        Role oldRole = role;
        role = Role.DEFAULT;
        for (EquipmentType type : equipment.keySet()) {
            role = role.newRole(type.getRole());
        }
        if (getState() == UnitState.IMPROVING
            && !hasAbility("model.ability.improveTerrain")) {
            setStateUnchecked(UnitState.ACTIVE);
            setMovesLeft(0);
        }

        // Check for role change for reseting the experience.
        // Soldier and Dragoon are compatible, no loss of experience.
        if (!role.isCompatibleWith(oldRole)) {
            experience = 0;
        }
    }

    /**
     * Gets the location of this Unit.
     *
     * @return The location of this Unit.
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Sets the units location without updating any other variables
     *
     * @param newLocation The new <code>Location</code>.
     */
    public void setLocationNoUpdate(Location newLocation) {
        location = newLocation;
    }

    /**
     * Sets the location of this Unit.
     *
     * @param newLocation The new <code>Location</code>.
     */
    public void setLocation(Location newLocation) {
        // If either the add or remove involves a colony, call the
        // colony-specific routine...
        Colony oldColony = (location instanceof WorkLocation)
            ? this.getColony() : null;
        Colony newColony = (newLocation instanceof WorkLocation)
            ? newLocation.getColony() : null;
        // However if the unit is moving within the same colony,
        // do not call the colony-specific routines.
        if (oldColony == newColony) oldColony = newColony = null;

        boolean result = true;
        if (location != null) {
            result = (oldColony != null)
                ? oldColony.removeUnit(this)
                : location.remove(this);
        }
        /*if (!result) return false;*/

        location = newLocation;
        // Explore the new location now to prevent dealing with tiles
        // with null (unexplored) type.
        getOwner().setExplored(this);

        // It is possible to add a unit to a non-specific location
        // within a colony by specifying the colony as the new
        // location.
        if (newLocation instanceof Colony) {
            newColony = (Colony) newLocation;
            location = newLocation = newColony.getWorkLocationFor(this);
        }

        if (newLocation != null) {
            result = (newColony != null)
                ? newColony.addUnit(this, (WorkLocation) newLocation)
                : newLocation.add(this);
        }

        return /*result*/;
    }

    /**
     * Verifies if the unit is aboard a carrier
     *
     * @return True if the unit is aboard a carrier.
     */
    public boolean isOnCarrier() {
        return getLocation() instanceof Unit;
    }

    /**
     * Gets the carrier this unit is aboard if any.
     *
     * @return The carrier this unit is aboard, or null if none.
     */
    public Unit getCarrier() {
        return (isOnCarrier()) ? ((Unit)getLocation()) : null;
    }

    /**
     * Checks if this <code>Unit</code> is located in Europe.  That
     * is; either directly or onboard a carrier which is in Europe.
     *
     * @return True if in <code>Europe</code>.
     */
    public boolean isInEurope() {
        return (location instanceof Unit) ? ((Unit)location).isInEurope()
            : getLocation() instanceof Europe;
    }

    /**
     * Checks whether this <code>Unit</code> is at sea off the map, or
     * on board of a carrier that is.
     *
     * @return True if at sea.
     */
    public boolean isAtSea() {
        return (location instanceof Unit) ? ((Unit)location).isAtSea()
            : location instanceof HighSeas;
    }

    /**
     * Checks if this unit is running a mission.
     *
     * @return True if this unit is running a mission.
     */
    public boolean isInMission() {
        return hasAbility("model.ability.missionary")
            && (getLocation() instanceof IndianSettlement
                // TODO: remove this when PET missionary serialization is fixed
                || getLocation() == null);
    }

    /**
     * Gets the work location this unit is working in.
     *
     * @return The current <code>WorkLocation</code>, or null if none.
     */
    public WorkLocation getWorkLocation() {
        if (getLocation() instanceof WorkLocation) {
            return (WorkLocation) getLocation();
        }
        return null;
    }

    /**
     * Gets the <code>Building</code> this unit is working in.
     *
     * @return The current <code>Building</code>, or null if none.
     */
    public Building getWorkBuilding() {
        if (getLocation() instanceof Building) {
            return ((Building) getLocation());
        }
        return null;
    }

    /**
     * Gets the <code>ColonyTile</code> this unit is working in.
     *
     * @return The current <code>ColonyTile</code>, or null if none.
     */
    public ColonyTile getWorkTile() {
        if (getLocation() instanceof ColonyTile) {
            return ((ColonyTile) getLocation());
        }
        return null;
    }

    /**
     * Gets the entry location for this unit to use when returning from
     * {@link Europe}.
     *
     * @return The entry <code>Location</code>.
     */
    public Location getEntryLocation() {
        if (entryLocation == null) {
            entryLocation = owner.getEntryLocation();
        }
        return entryLocation;
    }

    /**
     * Sets the entry location in which this unit will be put when
     * returning from {@link Europe}.
     *
     * @param entryLocation The new entry <code>Location</code>.
     * @see #getEntryLocation
     */
    public void setEntryLocation(Location entryLocation) {
        this.entryLocation = entryLocation;
        if (entryLocation != null) {
            owner.setEntryLocation(entryLocation);
        }
    }

    /**
     * Gets the entry tile for this unit, or if null the default
     * entry location for the owning player.
     *
     * @return The entry <code>Tile</code>.
     */
    public Tile getFullEntryLocation() {
        return (entryLocation != null) ? (Tile) entryLocation
            : (owner.getEntryLocation() == null) ? null
            : owner.getEntryLocation().getTile();
    }

    /**
     * Get the moves left this turn.
     *
     * @return The number of moves this <code>Unit</code> has left.
     */
    public int getMovesLeft() {
        return movesLeft;
    }

    /**
     * Sets the moves left this turn.
     *
     * @param moves The new amount of moves left this <code>Unit</code>
     *     should have.
     */
    public void setMovesLeft(int moves) {
        this.movesLeft = (moves < 0) ? 0 : moves;
    }

    /**
     * Gets the type of goods this unit is producing in its current occupation.
     *
     * @return The type of goods this unit is producing.
     */
    public GoodsType getWorkType() {
        return workType;
    }

    /**
     * Set the type of goods this unit is producing in its current
     * occupation.
     *
     * @param type The <code>GoodsType</code> to produce.
     */
    public void setWorkType(GoodsType type) {
        this.workType = type;
    }

    /**
     * Change the type of goods this unit is producing in its current
     * occupation.  Updates the work location production and the unit
     * experience type if necessary.
     *
     * @param type The <code>GoodsType</code> to produce.
     */
    public void changeWorkType(GoodsType type) {
        setWorkType(type);
        if (type != null) experienceType = type;
        WorkLocation wl = getWorkLocation();
        if (wl != null) wl.updateProductionType();
    }

    /**
     * Gets the type of goods this unit has accrued experience producing.
     *
     * @return The type of goods this unit would produce.
     */
    public GoodsType getExperienceType() {
        return experienceType;
    }

    /**
     * Gets the experience of this <code>Unit</code> at its current
     * experienceType.
     *
     * @return The experience of this <code>Unit</code> at its current
     *     experienceType.
     * @see #modifyExperience
     */
    public int getExperience() {
        return experience;
    }

    /**
     * Sets the experience of this <code>Unit</code> at its current
     * experienceType.
     *
     * @param experience The new experience of this <code>Unit</code>
     *     at its current experienceType.
     * @see #modifyExperience
     */
    public void setExperience(int experience) {
        this.experience = Math.min(experience,
                                   getType().getMaximumExperience());
    }

    /**
     * Modifies the experience of this <code>Unit</code> at its current
     * experienceType.
     *
     * @param value The value by which to modify the experience of this
     *     <code>Unit</code>.
     * @see #getExperience
     */
    public void modifyExperience(int value) {
        experience += value;
    }

    /**
     * Gets the amount of work left.
     *
     * @return The amount of work left.
     */
    public int getWorkLeft() {
        return workLeft;
    }

    /**
     * Sets the amount of work left.
     *
     * @param workLeft The new amount of work left.
     */
    public void setWorkLeft(int workLeft) {
        this.workLeft = workLeft;
    }

    /**
     * Get the number of turns of work left.
     *
     * @return The number of turns of work left.
     */
    public int getWorkTurnsLeft() {
        return (state == UnitState.IMPROVING
                && unitType.hasAbility(Ability.EXPERT_PIONEER))
            ? (getWorkLeft() + 1) / 2
            : getWorkLeft();
    }

    /**
     * Gets the TileImprovement that this pioneer is contributing to.
     *
     * @return The <code>TileImprovement</code> the pioneer is working on.
     */
    public TileImprovement getWorkImprovement() {
        return workImprovement;
    }

    /**
     * Sets the TileImprovement that this pioneer is contributing to.
     *
     * @param imp The new <code>TileImprovement</code> the pioneer is to
     *     work on.
     */
    public void setWorkImprovement(TileImprovement imp) {
        workImprovement = imp;
    }

    /**
     * Get the unit being taught.
     *
     * @return A student <code>Unit</code> if any.
     */
    public final Unit getStudent() {
        return student;
    }

    /**
     * Set the student unit.
     *
     * @param newStudent The new student <code>Unit</code>.
     */
    public final void setStudent(final Unit newStudent) {
        Unit oldStudent = this.student;
        if (oldStudent == newStudent) return;

        if (newStudent == null) {
            this.student = null;
            if (oldStudent != null && oldStudent.getTeacher() == this) {
                oldStudent.setTeacher(null);
            }
        } else if (newStudent.getColony() != null
            && newStudent.getColony() == getColony()
            && newStudent.canBeStudent(this)) {
            if (oldStudent != null && oldStudent.getTeacher() == this) {
                oldStudent.setTeacher(null);
            }
            this.student = newStudent;
            newStudent.setTeacher(this);
        } else {
            throw new IllegalStateException("Unit can not be student: "
                + newStudent);
        }
    }

    /**
     * Get the unit teaching this one.
     *
     * @return A teacher <code>Unit</code>.
     */
    public final Unit getTeacher() {
        return teacher;
    }

    /**
     * Set the teacher for this unit.
     *
     * @param newTeacher The new teacher <code>Unit</code>.
     */
    public final void setTeacher(final Unit newTeacher) {
        Unit oldTeacher = this.teacher;
        if (newTeacher == oldTeacher) return;

        if (newTeacher == null) {
            this.teacher = null;
            if (oldTeacher != null && oldTeacher.getStudent() == this) {
                oldTeacher.setStudent(null);
            }
        } else {
            UnitType skillTaught = newTeacher.getType().getSkillTaught();
            if (newTeacher.getColony() != null
                && newTeacher.getColony() == getColony()
                && getColony().canTrain(skillTaught)) {
                if (oldTeacher != null && oldTeacher.getStudent() == this) {
                    oldTeacher.setStudent(null);
                }
                this.teacher = newTeacher;
                this.teacher.setStudent(this);
            } else {
                throw new IllegalStateException("Unit can not be teacher: "
                    + newTeacher);
            }
        }
    }

    /**
     * Gets the number of turns this unit has been training.
     *
     * @return The number of turns of training this <code>Unit</code> has
     *     given.
     * @see #setTurnsOfTraining
     * @see #getNeededTurnsOfTraining
     */
    public int getTurnsOfTraining() {
        return turnsOfTraining;
    }

    /**
     * Sets the number of turns this unit has been training.
     *
     * @param turnsOfTraining The number of turns of training this
     *     <code>Unit</code> has given.
     * @see #getNeededTurnsOfTraining
     */
    public void setTurnsOfTraining(int turnsOfTraining) {
        this.turnsOfTraining = turnsOfTraining;
    }

    /**
     * Gets the number of turns this unit has to train to educate a student.
     * This value is only meaningful for units that can be put in a school.
     *
     * @return The turns of training needed to teach its current type
     *     to a free colonist or to promote an indentured servant or a
     *     petty criminal.
     * @see #getTurnsOfTraining
     */
    public int getNeededTurnsOfTraining() {
        // number of turns is 4/6/8 for skill 1/2/3
        int result = 0;
        if (student != null) {
            result = getNeededTurnsOfTraining(unitType, student.unitType);
            if (getColony() != null) {
                result -= getColony().getProductionBonus();
            }
        }
        return result;
    }

    /**
     * Gets the number of turns this unit has to train to educate a student.
     * This value is only meaningful for units that can be put in a school.
     *
     * @return The turns of training needed to teach its current type
     *     to a free colonist or to promote an indentured servant or a
     *     petty criminal.
     * @see #getTurnsOfTraining
     *
     * @param typeTeacher The teacher <code>UnitType</code>.
     * @param typeStudent the student <code>UnitType</code>.
     * @return The number of turns.
     */
    public int getNeededTurnsOfTraining(UnitType typeTeacher, 
                                        UnitType typeStudent) {
        UnitType teaching = getUnitTypeTeaching(typeTeacher, typeStudent);
        if (teaching != null) {
            return typeStudent.getEducationTurns(teaching);
        } else {
            throw new IllegalStateException("typeTeacher=" + typeTeacher
                + " typeStudent=" + typeStudent);
        }
    }

    /**
     * Gets the UnitType which a teacher is teaching to a student.
     * This value is only meaningful for teachers that can be put in a
     * school.
     *
     * @param typeTeacher The teacher <code>UnitType</code>.
     * @param typeStudent The student <code>UnitType</code>.
     * @return The <code>UnitType</code> taught.
     * @see #getTurnsOfTraining
     *
     */
    public static UnitType getUnitTypeTeaching(UnitType typeTeacher,
                                               UnitType typeStudent) {
        UnitType skillTaught = typeTeacher.getSkillTaught();
        if (typeStudent.canBeUpgraded(skillTaught, ChangeType.EDUCATION)) {
            return skillTaught;
        } else {
            return typeStudent.getEducationUnit(0);
        }
    }

    /**
     * Can this unit be a student?
     *
     * @param teacher The teacher <code>Unit</code> which is trying to
     *     teach it.
     * @return True if the unit can be taught by the teacher.
     */
    public boolean canBeStudent(Unit teacher) {
        return teacher != this && canBeStudent(unitType, teacher.unitType);
    }

    /**
     * Can a unit be a student?
     *
     * @param typeStudent The student <code>UnitType</code>.
     * @param typeTeacher The teacher <code>UnitType</code>.
     * @return True if the student can be taught by the teacher.
     */
    public boolean canBeStudent(UnitType typeStudent, UnitType typeTeacher) {
        return getUnitTypeTeaching(typeTeacher, typeStudent) != null;
    }

    /**
     * Gets the nationality of this Unit.
     *
     * Nationality represents a Unit's personal allegiance to a
     * nation.  This may conflict with who currently issues orders to
     * the Unit (the owner).
     *
     * @return The nationality of this Unit.
     */
    public String getNationality() {
        return nationality;
    }

    /**
     * Sets the nationality of this Unit.  A unit will change
     * nationality when it switches owners willingly.  Currently only
     * Converts do this, but it opens the possibility of
     * naturalisation.
     *
     * @param newNationality The new nationality of this Unit.
     */
    public void setNationality(String newNationality) {
        if (isPerson()) {
            nationality = newNationality;
        } else {
            throw new UnsupportedOperationException("Can not set the nationality of a Unit which is not a person!");
        }
    }

    /**
     * Gets the ethnicity of this Unit.
     *
     * Ethnicity is inherited from the inhabitants of the place where
     * the Unit was born.  Allows former converts to become
     * native-looking colonists.
     *
     * @return The ethnicity of this Unit.
     */
    public String getEthnicity() {
        return ethnicity;
    }

    /**
     * Sets the ethnicity of this Unit.
     *
     * Ethnicity is something units are born with.  It cannot be
     * subsequently changed.
     *
     * @param newEthnicity The new ethnicity of this Unit.
     */
    public void setEthnicity(String newEthnicity) {
        throw new UnsupportedOperationException("Can not change a Unit's ethnicity!");
    }

    /**
     * Identifies whether this unit came from a native tribe.
     *
     * @return Whether this unit looks native or not.
     */
    public boolean hasNativeEthnicity() {
        try {
            // FIXME: getNation() could fail, but getNationType()
            // doesn't work as expected
            return getGame().getSpecification().getNation(ethnicity)
                .getType().isIndian();
            // return getGame().getSpecification().getNationType(ethnicity).hasAbility("model.ability.native");
            // return getGame().getSpecification().getIndianNationTypes().contains(getNationType(ethnicity));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets the <code>IndianSettlement</code> home for this unit.
     *
     * @return The home <code>IndianSettlement</code> of this unit.
     */
    public IndianSettlement getHomeIndianSettlement() {
        return indianSettlement;
    }

    /**
     * Sets the home <code>IndianSettlement</code> for this unit.
     *
     * @param indianSettlement The <code>IndianSettlement</code> that should
     *     now own this <code>Unit</code>.
     */
    public void setHomeIndianSettlement(IndianSettlement indianSettlement) {
        if (this.indianSettlement != null) {
            this.indianSettlement.removeOwnedUnit(this);
        }

        this.indianSettlement = indianSettlement;

        if (indianSettlement != null) {
            indianSettlement.addOwnedUnit(this);
        }
    }

    /**
     * Gets the unit hit points.
     *
     * This is currently only used for damaged ships, but might get an
     * extended use later.
     *
     * @return The hit points this <code>Unit</code> has.
     * @see UnitType#getHitPoints
     */
    public int getHitPoints() {
        return hitPoints;
    }

    /**
     * Sets the hit points for this unit.
     *
     * @param hitPoints The new hit points for this unit.
     */
    public void setHitPoints(int hitPoints) {
        this.hitPoints = hitPoints;
    }

    /**
     * Checks if this unit is under repair.
     *
     * @return True if under repair.
     */
    public boolean isDamaged() {
        return hitPoints < unitType.getHitPoints();
    }

    /**
     * Get how many turns left to be repaired
     *
     * @return The number of turns left to be repaired.
     */
    public int getTurnsForRepair() {
        return unitType.getHitPoints() - getHitPoints();
    }

    /**
     * Get the destination of this unit.
     *
     * @return The destination <code>Location</code> of this <code>Unit</code>.
     */
    public Location getDestination() {
        return destination;
    }

    /**
     * Sets the destination of this unit.
     *
     * @param newDestination The new destination <code>Location</code>.
     */
    public void setDestination(Location newDestination) {
        this.destination = newDestination;
    }

    /**
     * Get the unit trade route, if any.
     *
     * @return The <code>TradeRoute</code>, or null if none.
     */
    public final TradeRoute getTradeRoute() {
        return tradeRoute;
    }

    /**
     * Set the unit trade route.
     *
     * @param newTradeRoute The new <code>TradeRoute</code> value.
     */
    public final void setTradeRoute(final TradeRoute newTradeRoute) {
        this.tradeRoute = newTradeRoute;
    }

    /**
     * Get the stop the unit is heading for or at.
     *
     * @return The target <code>Stop</code>.
     */
    public Stop getStop() {
        return (validateCurrentStop() < 0) ? null
            : getTradeRoute().getStops().get(currentStop);
    }

    /**
     * Get the current trade route stop.
     *
     * @return The current stop index.
     */
    public int getCurrentStop() {
        return currentStop;
    }

    /**
     * Set the current stop.
     *
     * @param currentStop A new value for the currentStop.
     */
    public void setCurrentStop(int currentStop) {
        this.currentStop = currentStop;
    }

    /**
     * Validate and return the current stop.
     *
     * @return The current stop index, or negative on failure.
     */
    public int validateCurrentStop() {
        if (tradeRoute == null) {
            currentStop = -1;
        } else {
            List<Stop> stops = tradeRoute.getStops();
            if (stops == null || stops.size() == 0) {
                currentStop = -1;
            } else {
                if (currentStop < 0 || currentStop >= stops.size()) {
                    // The current stop can become out of range if the trade
                    // route is modified.
                    currentStop = 0;
                }
            }
        }
        return currentStop;
    }

    /**
     * Get the current amount of treasure in this unit.
     *
     * @return The amount of treasure.
     * @exception IllegalStateException if this is not a treasure
     *     carrying unit.
     */
    public int getTreasureAmount() {
        if (!canCarryTreasure()) {
            throw new IllegalStateException("Unit can not carry treasure");
        }
        return treasureAmount;
    }

    /**
     * Set the amount of treasure in this unit.
     *
     * @param amount The new amount of treasure.
     */
    public void setTreasureAmount(int amount) {
        if (!canCarryTreasure()) {
            throw new IllegalStateException("Unit can not carry treasure");
        }
        this.treasureAmount = amount;
    }

    /**
     * Gets the attrition of this unit.
     *
     * @return The attrition of this unit.
     */
    public int getAttrition() {
        return attrition;
    }

    /**
     * Sets the attrition of this unit.
     *
     * @param attrition The new attrition of this unit.
     */
    public void setAttrition(int attrition) {
        this.attrition = attrition;
    }

    /**
     * Get the visible amount of goods that is carried by this unit.
     *
     * @return The visible amount of goods carried by this <code>Unit</code>.
     */
    public int getVisibleGoodsCount() {
        return (visibleGoodsCount >= 0) ? visibleGoodsCount
            : getGoodsSpaceTaken();
    }

    /**
     * Get the <code>Equipment</code> value.
     *
     * @return A counted map of the <code>EquipmentType</code>s
     *     carried by this unit.
     */
    public final TypeCountMap<EquipmentType> getEquipment() {
        return equipment;
    }

    /**
     * Clears all <code>Equipment</code> held by this unit.
     */
    public void clearEquipment() {
        equipment.clear();
    }

    /**
     * Get the amount of an equipment type this unit has.
     *
     * @param equipmentType The <code>EquipmentType</code> to check.
     * @return The amount of equipment.
     */
    public int getEquipmentCount(EquipmentType equipmentType) {
        return equipment.getCount(equipmentType);
    }



    // More complex equipment manipulation.

    /**
     * Does the unit have arms?
     *
     * @return True if the unit has arms.
     */
    public boolean isArmed() {
        if (musketsEq[0] == null) {
            Specification spec = getSpecification();
            musketsEq[0] = spec.getEquipmentType("model.equipment.muskets");
            musketsEq[1] = spec.getEquipmentType("model.equipment.indian.muskets");
        }
        for (EquipmentType et : musketsEq) {
            if (getEquipmentCount(et) > 0) return true;
        }
        return false;
    }

    /**
     * Does the unit have a mount?
     *
     * @return True if the unit have a mount.
     */
    public boolean isMounted() {
        if (horsesEq[0] == null) {
            Specification spec = getSpecification();
            horsesEq[0] = spec.getEquipmentType("model.equipment.horses");
            horsesEq[1] = spec.getEquipmentType("model.equipment.indian.horses");
        }
        for (EquipmentType et : horsesEq) {
            if (getEquipmentCount(et) > 0) return true;
        }
        return false;
    }

    /**
     * Get a description of the unit's equipment.
     *
     * @return A <code>StringTemplate</code> summarizing the equipment.
     */
    public StringTemplate getEquipmentLabel() {
        if (equipment.isEmpty()) return null;
        StringTemplate result = StringTemplate.label("/");
        for (java.util.Map.Entry<EquipmentType, Integer> entry
                 : equipment.getValues().entrySet()) {
            EquipmentType type = entry.getKey();
            int amount = entry.getValue().intValue();
            if (!type.needsGoodsToBuild()) {
                result.addStringTemplate(StringTemplate.template("model.goods.goodsAmount")
                    .add("%goods%", type.getNameKey())
                    .addName("%amount%", Integer.toString(amount)));
            } else {
                for (AbstractGoods goods : type.getRequiredGoods()) {
                    result.addStringTemplate(StringTemplate.template("model.goods.goodsAmount")
                        .add("%goods%", goods.getType().getNameKey())
                        .addName("%amount%", Integer.toString(amount * goods.getAmount())));
                }
            }
        }
        return result;
    }

    /**
     * After winning a battle, can this unit the loser equipment?
     *
     * @param equip The <code>EquipmentType</code> to consider.
     * @param loser The loser <code>Unit</code>.
     * @return The <code>EquipmentType</code> to capture, which may
     *     differ from the equip parameter due to transformations such
     *     as to the native versions of horses and muskets.
     *     Or return null if capture is not possible.
     */
    public EquipmentType canCaptureEquipment(EquipmentType equip, Unit loser) {
        if (hasAbility("model.ability.captureEquipment")) {
            if (getOwner().isIndian() != loser.getOwner().isIndian()) {
                equip = equip.getCaptureEquipment(getOwner().isIndian());
            }
            return (canBeEquippedWith(equip)) ? equip : null;
        }
        return null;
    }

    /**
     * Gets the available equipment that can be equipped automatically
     * in case of an attack.
     *
     * @return The equipment that can be automatically equipped by
     *     this unit, or null if none.
     */
    public TypeCountMap<EquipmentType> getAutomaticEquipment() {
        // Paul Revere makes an unarmed colonist in a settlement pick up
        // a stock-piled musket if attacked, so the bonus should be applied
        // for unarmed colonists inside colonies where there are muskets
        // available. Indians can also pick up equipment.
        if (isArmed()) return null;

        if (!getOwner().hasAbility("model.ability.automaticEquipment")) {
            return null;
        }

        Settlement settlement = null;
        if (getLocation() instanceof WorkLocation) {
            settlement = getColony();
        }
        if (getLocation() instanceof IndianSettlement) {
            settlement = (Settlement) getLocation();
        }
        if (settlement == null) return null;

        TypeCountMap<EquipmentType> equipmentList = null;
        // Check for necessary equipment in the settlement
        Set<Ability> autoDefence = new HashSet<Ability>();
        autoDefence.addAll(getOwner()
            .getAbilitySet("model.ability.automaticEquipment"));

        for (EquipmentType equipment : getSpecification().getEquipmentTypeList()) {
            for (Ability ability : autoDefence) {
                if (!ability.appliesTo(equipment)) continue;

                if (!canBeEquippedWith(equipment)) continue;

                boolean hasReqGoods = true;
                for (AbstractGoods ag : equipment.getRequiredGoods()) {
                    if (settlement.getGoodsCount(ag.getType()) < ag.getAmount()){
                        hasReqGoods = false;
                        break;
                    }
                }
                if (hasReqGoods) {
                    // lazy initialization, required
                    if (equipmentList == null) {
                        equipmentList = new TypeCountMap<EquipmentType>();
                    }
                    equipmentList.incrementCount(equipment, 1);
                }
            }
        }
        return equipmentList;
    }

    /**
     * Does losing a piece of equipment mean the death of this unit?
     *
     * @param lose The <code>EquipmentType</code> to lose.
     * @return True if the unit is doomed.
     */
    public boolean losingEquipmentKillsUnit(EquipmentType lose) {
        if (hasAbility("model.ability.disposeOnAllEquipLost")) {
            for (EquipmentType equip : getEquipment().keySet()) {
                if (equip != lose) return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Does losing a piece of equipment mean the demotion of this unit?
     *
     * @param lose The <code>EquipmentType</code> to lose.
     * @return True if the unit is to be demoted.
     */
    public boolean losingEquipmentDemotesUnit(EquipmentType lose) {
        if (hasAbility("model.ability.demoteOnAllEquipLost")) {
            for (EquipmentType equip : getEquipment().keySet()) {
                if (equip != lose) return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Gets the best combat equipment type that this unit has.
     *
     * @param equipment The equipment to look through, such as returned by
     *     @see Unit#getEquipment() and/or @see Unit#getAutomaticEquipment().
     * @return The equipment type to lose, or null if none.
     */
    public EquipmentType getBestCombatEquipmentType(TypeCountMap<EquipmentType> equipment) {
        EquipmentType lose = null;
        if (equipment != null) {
            int priority = -1;
            for (EquipmentType equipmentType : equipment.keySet()) {
                if (equipmentType.getCombatLossPriority() > priority) {
                    lose = equipmentType;
                    priority = equipmentType.getCombatLossPriority();
                }
            }
        }
        return lose;
    }

    /**
     * Checks whether this unit can be equipped with the given
     * <code>EquipmentType</code> at the current
     * <code>Location</code>. This is the case if all requirements of
     * the EquipmentType are met.
     *
     * @param equipmentType an <code>EquipmentType</code> value
     * @return whether this unit can be equipped with the given
     *         <code>EquipmentType</code> at the current location.
     */
    public boolean canBeEquippedWith(EquipmentType equipmentType) {
        for (Entry<String, Boolean> entry : equipmentType.getRequiredAbilities().entrySet()) {
            if (hasAbility(entry.getKey()) != entry.getValue()) {
                return false;
            }
        }
        if (equipment.getCount(equipmentType) >= equipmentType.getMaximumCount()) {
            return false;
        }
        return true;
    }

    /**
     * Changes the equipment a unit has and returns a list of equipment
     * it still has but needs to drop due to the changed equipment being
     * incompatible.
     *
     * @param type The <code>EquipmentType</code> to change.
     * @param amount The amount to change by (may be negative).
     * @return A list of equipment types that the unit must now drop.
     */
    public List<EquipmentType> changeEquipment(EquipmentType type, int amount) {
        List<EquipmentType> result = new ArrayList<EquipmentType>();
        equipment.incrementCount(type, amount);
        if (amount > 0) {
            for (EquipmentType oldType
                     : new HashSet<EquipmentType>(equipment.keySet())) {
                if (!oldType.isCompatibleWith(type)) {
                    result.add(oldType);
                }
            }
        }
        setRole();
        return result;
    }


    // Combat routines

    /**
     * Is the unit a beached ship?
     *
     * @return True if the unit is a beached ship.
     */
    public boolean isBeached() {
        return isBeached(getTile());
    }

    /**
     * Would this unit be beached if it was on a particular tile?
     *
     * @param tile The <code>Tile</code> to check.
     * @return True if the unit is a beached ship.
     */
    public boolean isBeached(Tile tile) {
        return isNaval() && tile != null && tile.isLand()
            && tile.getSettlement() == null;
    }

    /**
     * Checks if this is an defensive unit. That is: a unit which can be used to
     * defend a <code>Settlement</code>.
     *
     * Note! As this method is used by the AI it really means that the unit can
     * defend as is. To be specific an unarmed colonist is not defensive yet,
     * even if Paul Revere and stockpiled muskets are available. That check is
     * only performed on an actual attack.
     *
     * A settlement is lost when there are no more defensive units.
     *
     * @return True if this is a defensive unit meaning it can be used
     *     to defend a <code>Colony</code>.  This would normally mean
     *     that a defensive unit also will be offensive.
     */
    public boolean isDefensiveUnit() {
        return (unitType.isDefensive() || isArmed() || isMounted())
            && !isNaval();
    }

    /**
     * Checks if this is an offensive unit.  That is, one that can
     * attack other units.
     *
     * @return True if this is an offensive unit.
     */
    public boolean isOffensiveUnit() {
        return unitType.isOffensive() || isArmed() || isMounted();
    }

    /**
     * Is an alternate unit a better defender than the current choice.
     * Prefer if there is no current defender, or if the alternate
     * unit is better armed, or provides greater defensive power and
     * does not replace a defensive unit defender with a non-defensive
     * unit.
     *
     * @param defender The current defender <code>Unit</code>.
     * @param defenderPower Its defence power.
     * @param other An alternate <code>Unit</code>.
     * @param otherPower Its defence power.
     * @return True if the other unit should be preferred.
     */
    public static boolean betterDefender(Unit defender, float defenderPower,
                                         Unit other, float otherPower) {
        if (defender == null) {
            return true;
        } else if (defender.isPerson() && other.isPerson()
            && !defender.isArmed() && other.isArmed()) {
            return true;
        } else if (defender.isPerson() && other.isPerson()
            && defender.isArmed() && !other.isArmed()) {
            return false;
        } else if (!defender.isDefensiveUnit() && other.isDefensiveUnit()) {
            return true;
        } else if (defender.isDefensiveUnit() && !other.isDefensiveUnit()) {
            return false;
        } else {
            return defenderPower < otherPower;
        }
    }

    /**
     * Finds the closest <code>Location</code> to this tile where
     * this ship can be repaired.
     *
     * @return The closest <code>Location</code> where a ship can be repaired.
     */
    public Location getRepairLocation() {
        final Player player = getOwner();
        final Tile tile = getTile();
        Location bestLocation = null;
        int bestTurns = INFINITY;
        for (Colony colony : player.getColonies()) {
            int turns;
            if (colony != null && colony != tile.getColony()
                && colony.hasAbility("model.ability.repairUnits")
                && (turns = getTurnsToReach(colony)) >= 0
                && turns < bestTurns) {
                // Tile.getDistanceTo(Tile) doesn't care about
                // connectivity, so we need to check for an available
                // path to target colony instead
                bestTurns = turns;
                bestLocation = colony;
            }
        }
        if (bestLocation == null) bestLocation = player.getEurope();
        return bestLocation;
    }


    // Movement handling

    /**
     * A move type.
     *
     * @see Unit#getMoveType(Map.Direction)
     */
    public static enum MoveType {
        MOVE(null, true),
        MOVE_HIGH_SEAS(null, true),
        EXPLORE_LOST_CITY_RUMOUR(null, true),
        ATTACK_UNIT(null, false),
        ATTACK_SETTLEMENT(null, false),
        EMBARK(null, false),
        ENTER_INDIAN_SETTLEMENT_WITH_FREE_COLONIST(null, false),
        ENTER_INDIAN_SETTLEMENT_WITH_SCOUT(null, false),
        ENTER_INDIAN_SETTLEMENT_WITH_MISSIONARY(null, false),
        ENTER_FOREIGN_COLONY_WITH_SCOUT(null, false),
        ENTER_SETTLEMENT_WITH_CARRIER_AND_GOODS(null, false),
        MOVE_NO_MOVES("Attempt to move without moves left"),
        MOVE_NO_ACCESS_LAND("Attempt to move a naval unit onto land"),
        MOVE_NO_ACCESS_BEACHED("Attempt to move onto foreign beached ship"),
        MOVE_NO_ACCESS_EMBARK("Attempt to embark onto absent or foreign carrier"),
        MOVE_NO_ACCESS_FULL("Attempt to embark onto full carrier"),
        MOVE_NO_ACCESS_GOODS("Attempt to trade without goods"),
        MOVE_NO_ACCESS_CONTACT("Attempt to interact with natives before contact"),
        MOVE_NO_ACCESS_SETTLEMENT("Attempt to move into foreign settlement"),
        MOVE_NO_ACCESS_SKILL("Attempt to learn skill with incapable unit"),
        MOVE_NO_ACCESS_TRADE("Attempt to trade without authority"),
        MOVE_NO_ACCESS_WAR("Attempt to trade while at war"),
        MOVE_NO_ACCESS_WATER("Attempt to move into a settlement by water"),
        MOVE_NO_ATTACK_CIVILIAN("Attempt to attack with civilian unit"),
        MOVE_NO_ATTACK_MARINE("Attempt to attack from on board ship"),
        MOVE_NO_EUROPE("Attempt to move to Europe by incapable unit"),
        MOVE_NO_REPAIR("Attempt to move a unit that is under repair"),
        MOVE_NO_TILE("Attempt to move when not on a tile"),
        MOVE_ILLEGAL("Unspecified illegal move");

        /**
         * The reason why this move type is illegal.
         */
        private String reason;

        /**
         * Does this move type imply progress towards a destination.
         */
        private boolean progress;

        MoveType(String reason) {
            this.reason = reason;
            this.progress = false;
        }

        MoveType(String reason, boolean progress) {
            this.reason = reason;
            this.progress = progress;
        }

        public boolean isLegal() {
            return this.reason == null;
        }

        public String whyIllegal() {
            return (reason == null) ? "(none)" : reason;
        }

        public boolean isProgress() {
            return progress;
        }

        public boolean isAttack() {
            return this == ATTACK_UNIT || this == ATTACK_SETTLEMENT;
        }
    }

    /**
     * Gets the cost of moving this <code>Unit</code> onto the given
     * <code>Tile</code>. A call to {@link #getMoveType(Tile)} will return
     * <code>MOVE_NO_MOVES</code>, if {@link #getMoveCost} returns a move cost
     * larger than the {@link #getMovesLeft moves left}.
     *
     * @param target The <code>Tile</code> this <code>Unit</code> will move
     *            onto.
     * @return The cost of moving this unit onto the given <code>Tile</code>.
     */
    public int getMoveCost(Tile target) {
        return getMoveCost(getTile(), target, getMovesLeft());
    }

    /**
     * Gets the cost of moving this <code>Unit</code> from the given
     * <code>Tile</code> onto the given <code>Tile</code>. A call to
     * {@link #getMoveType(Tile, Tile, int)} will return
     * <code>MOVE_NO_MOVES</code>, if {@link #getMoveCost} returns a move cost
     * larger than the {@link #getMovesLeft moves left}.
     *
     * @param from The <code>Tile</code> this <code>Unit</code> will move
     *            from.
     * @param target The <code>Tile</code> this <code>Unit</code> will move
     *            onto.
     * @param ml The amount of moves this Unit has left.
     * @return The cost of moving this unit onto the given <code>Tile</code>.
     */
    public int getMoveCost(Tile from, Tile target, int ml) {
        // Remember to also change map.findPath(...) if you change anything
        // here.

        // TODO: also pass direction, so that we can check for rivers
        // more easily

        int cost = target.getType().getBasicMoveCost();
        if (target.isLand()) {
            TileItemContainer container = target.getTileItemContainer();
            if (container != null) {
                cost = container.getMoveCost(from, target, cost);
            }
        }

        if (isBeached(from)) {
            // Ship on land due to it was in a colony which was abandoned
            cost = ml;
        } else if (cost > ml) {
            // Using +2 in order to make 1/3 and 2/3 move count as
            // 3/3, only when getMovesLeft > 0
            if ((ml + 2 >= getInitialMovesLeft() || cost <= ml + 2
                 || target.getSettlement()!=null) && ml != 0) {
                cost = ml;
            }
        }
        return cost;
    }

    /**
     * Gets the type of a move made in a specified direction.
     *
     * @param direction The <code>Direction</code> of the move.
     * @return The move type.
     */
    public MoveType getMoveType(Direction direction) {
        Tile tile = getTile();
        if (tile == null) return MoveType.MOVE_NO_TILE;
        Tile target = tile.getNeighbourOrNull(direction);
        if (target == null) return MoveType.MOVE_ILLEGAL;
        return getMoveType(target);
    }

    /**
     * Gets the type of a move that is made when moving from one tile
     * to another.
     *
     * @param target The target <code>Tile</code> of the move.
     * @return The move type.
     */
    public MoveType getMoveType(Tile target) {
        Tile tile = getTile();
        if (tile == null) return MoveType.MOVE_NO_TILE;
        return getMoveType(tile, target, getMovesLeft());
    }

    /**
     * Gets the type of a move that is made when moving from one tile
     * to another.
     *
     * @param from The origin <code>Tile</code> of the move.
     * @param target The target <code>Tile</code> of the move.
     * @param ml The amount of moves this unit has left.
     * @return The move type.
     */
    public MoveType getMoveType(Tile from, Tile target, int ml) {
        MoveType move = getSimpleMoveType(from, target);
        if (move.isLegal()) {
            switch (move) {
            case ATTACK_UNIT: case ATTACK_SETTLEMENT:
                // Needs only a single movement point, regardless of
                // terrain, but suffers penalty.
                if (ml <= 0) {
                    move = MoveType.MOVE_NO_MOVES;
                }
                break;
            default:
                if (ml <= 0
                    || (from != null && getMoveCost(from, target, ml) > ml)) {
                    move = MoveType.MOVE_NO_MOVES;
                }
            }
        }
        return move;
    }

    /**
     * Gets the type of a move that is made when moving from one tile
     * to another, without checking if the unit has moves left or
     * logging errors.
     *
     * @param from The origin <code>Tile</code> of the move.
     * @param target The target <code>Tile</code> of the move.
     * @return The move type, which will be one of the extended illegal move
     *         types on failure.
     */
    public MoveType getSimpleMoveType(Tile from, Tile target) {
        return (isNaval()) ? getNavalMoveType(from, target)
            : getLandMoveType(from, target);
    }

    /**
     * Gets the type of a move that is made when moving from one tile
     * to another, without checking if the unit has moves left or
     * logging errors.
     *
     * @param target The target <code>Tile</code> of the move.
     * @return The move type, which will be one of the extended illegal move
     *         types on failure.
     */
    public MoveType getSimpleMoveType(Tile target) {
        Tile tile = getTile();
        if (tile == null) return MoveType.MOVE_NO_TILE;
        return getSimpleMoveType(tile, target);
    }

    /**
     * Gets the type of a move made in a specified direction,
     * without checking if the unit has moves left or logging errors.
     *
     * @param direction The direction of the move.
     * @return The move type.
     */
    public MoveType getSimpleMoveType(Direction direction) {
        Tile tile = getTile();
        if (tile == null) return MoveType.MOVE_NO_TILE;
        Tile target = tile.getNeighbourOrNull(direction);
        return getSimpleMoveType(tile, target);
    }

    /**
     * Gets the type of a move that is made when moving a naval unit
     * from one tile to another.
     *
     * @param from The origin <code>Tile<code> of the move.
     * @param target The target <code>Tile</code> of the move.
     * @return The move type.
     */
    private MoveType getNavalMoveType(Tile from, Tile target) {
        if (target == null) {
            return (getOwner().canMoveToEurope()) ? MoveType.MOVE_HIGH_SEAS
                : MoveType.MOVE_NO_EUROPE;
        } else if (isDamaged()) {
            return MoveType.MOVE_NO_REPAIR;
        }

        if (target.isLand()) {
            Settlement settlement = target.getSettlement();
            if (settlement == null) {
                return MoveType.MOVE_NO_ACCESS_LAND;
            } else if (settlement.getOwner() == getOwner()) {
                return MoveType.MOVE;
            } else if (isTradingUnit()) {
                return getTradeMoveType(settlement);
            } else {
                return MoveType.MOVE_NO_ACCESS_SETTLEMENT;
            }
        } else { // target at sea
            Unit defender = target.getFirstUnit();
            if (defender != null && !getOwner().owns(defender)) {
                return (isOffensiveUnit())
                    ? MoveType.ATTACK_UNIT
                    : MoveType.MOVE_NO_ATTACK_CIVILIAN;
            } else {
                return (target.isDirectlyHighSeasConnected())
                    ? MoveType.MOVE_HIGH_SEAS
                    : MoveType.MOVE;
            }
        }
    }

    /**
     * Gets the type of a move that is made when moving a land unit to
     * from one tile to another.
     *
     * @param from The origin <code>Tile</code> of the move.
     * @param target The target <code>Tile</code> of the move.
     * @return The move type.
     */
    private MoveType getLandMoveType(Tile from, Tile target) {
        if (target == null) return MoveType.MOVE_ILLEGAL;

        Player owner = getOwner();
        Unit defender = target.getFirstUnit();

        if (target.isLand()) {
            Settlement settlement = target.getSettlement();
            if (settlement == null) {
                if (defender != null && owner != defender.getOwner()) {
                    if (defender.isNaval()) {
                        return MoveType.ATTACK_UNIT;
                    } else if (!isOffensiveUnit()) {
                        return MoveType.MOVE_NO_ATTACK_CIVILIAN;
                    } else {
                        return (allowMoveFrom(from))
                            ? MoveType.ATTACK_UNIT
                            : MoveType.MOVE_NO_ATTACK_MARINE;
                    }
                } else if (target.hasLostCityRumour() && owner.isEuropean()) {
                    // Natives do not explore rumours, see:
                    // server/control/InGameInputHandler.java:move()
                    return MoveType.EXPLORE_LOST_CITY_RUMOUR;
                } else {
                    return MoveType.MOVE;
                }
            } else if (owner == settlement.getOwner()) {
                return MoveType.MOVE;
            } else if (isTradingUnit()) {
                return getTradeMoveType(settlement);
            } else if (isColonist()) {
                switch (getRole()) {
                case DEFAULT: case PIONEER:
                    return getLearnMoveType(from, settlement);
                case MISSIONARY:
                    return getMissionaryMoveType(from, settlement);
                case SCOUT:
                    return getScoutMoveType(from, settlement);
                case SOLDIER: case DRAGOON:
                    return (allowMoveFrom(from))
                        ? MoveType.ATTACK_SETTLEMENT
                        : MoveType.MOVE_NO_ATTACK_MARINE;
                }
                return MoveType.MOVE_ILLEGAL; // should not happen
            } else if (isOffensiveUnit()) {
                return (allowMoveFrom(from))
                    ? MoveType.ATTACK_SETTLEMENT
                    : MoveType.MOVE_NO_ATTACK_MARINE;
            } else {
                return MoveType.MOVE_NO_ACCESS_SETTLEMENT;
            }
        } else { // moving to sea, check for embarkation
            if (defender == null || !getOwner().owns(defender)) {
                return MoveType.MOVE_NO_ACCESS_EMBARK;
            }
            for (Unit u : target.getUnitList()) {
                if (u.canAdd(this)) return MoveType.EMBARK;
            }
            return MoveType.MOVE_NO_ACCESS_FULL;
        }
    }

    /**
     * Get the <code>MoveType</code> when moving a trading unit to a
     * settlement.
     *
     * @param settlement The <code>Settlement</code> to move to.
     * @return The appropriate <code>MoveType</code>.
     */
    private MoveType getTradeMoveType(Settlement settlement) {
        if (settlement instanceof Colony) {
            return (getOwner().atWarWith(settlement.getOwner()))
                ? MoveType.MOVE_NO_ACCESS_WAR
                : (!hasAbility("model.ability.tradeWithForeignColonies"))
                ? MoveType.MOVE_NO_ACCESS_TRADE
                : MoveType.ENTER_SETTLEMENT_WITH_CARRIER_AND_GOODS;
        } else if (settlement instanceof IndianSettlement) {
            // Do not block for war, bringing gifts is allowed
            return (!allowContact(settlement))
                ? MoveType.MOVE_NO_ACCESS_CONTACT
                : (hasGoodsCargo() || getSpecification()
                    .getBoolean(GameOptions.EMPTY_TRADERS))
                ? MoveType.ENTER_SETTLEMENT_WITH_CARRIER_AND_GOODS
                : MoveType.MOVE_NO_ACCESS_GOODS;
        } else {
            return MoveType.MOVE_ILLEGAL; // should not happen
        }
    }

    /**
     * Get the <code>MoveType</code> when moving a colonist to a settlement.
     *
     * @param from The <code>Tile</code> to move from.
     * @param settlement The <code>Settlement</code> to move to.
     * @return The appropriate <code>MoveType</code>.
     */
    private MoveType getLearnMoveType(Tile from, Settlement settlement) {
        if (settlement instanceof Colony) {
            return MoveType.MOVE_NO_ACCESS_SETTLEMENT;
        } else if (settlement instanceof IndianSettlement) {
            UnitType scoutSkill = getSpecification()
                .getUnitType("model.unit.seasonedScout");
            return (!allowContact(settlement))
                ? MoveType.MOVE_NO_ACCESS_CONTACT
                : (!allowMoveFrom(from))
                ? MoveType.MOVE_NO_ACCESS_WATER
                : (!getType().canBeUpgraded(null, ChangeType.NATIVES))
                ? MoveType.MOVE_NO_ACCESS_SKILL
                : MoveType.ENTER_INDIAN_SETTLEMENT_WITH_FREE_COLONIST;
        } else {
            return MoveType.MOVE_ILLEGAL; // should not happen
        }
    }

    /**
     * Get the <code>MoveType</code> when moving a missionary to a settlement.
     *
     * @param from The <code>Tile</code> to move from.
     * @param settlement The <code>Settlement</code> to move to.
     * @return The appropriate <code>MoveType</code>.
     */
    private MoveType getMissionaryMoveType(Tile from, Settlement settlement) {
        if (settlement instanceof Colony) {
            return MoveType.MOVE_NO_ACCESS_SETTLEMENT;
        } else if (settlement instanceof IndianSettlement) {
            return (!allowContact(settlement))
                ? MoveType.MOVE_NO_ACCESS_CONTACT
                : (!allowMoveFrom(from))
                ? MoveType.MOVE_NO_ACCESS_WATER
                : MoveType.ENTER_INDIAN_SETTLEMENT_WITH_MISSIONARY;
        } else {
            return MoveType.MOVE_ILLEGAL; // should not happen
        }
    }

    /**
     * Get the <code>MoveType</code> when moving a scout to a settlement.
     *
     * @param from The <code>Tile</code> to move from.
     * @param settlement The <code>Settlement</code> to move to.
     * @return The appropriate <code>MoveType</code>.
     */
    private MoveType getScoutMoveType(Tile from, Settlement settlement) {
        if (settlement instanceof Colony) {
            // No allowMoveFrom check for Colonies
            return MoveType.ENTER_FOREIGN_COLONY_WITH_SCOUT;
        } else if (settlement instanceof IndianSettlement) {
            return (!allowMoveFrom(from))
                ? MoveType.MOVE_NO_ACCESS_WATER
                : MoveType.ENTER_INDIAN_SETTLEMENT_WITH_SCOUT;
        } else {
            return MoveType.MOVE_ILLEGAL; // should not happen
        }
    }

    /**
     * Is this unit allowed to move from a source tile?
     * Implements the restrictions on moving from water.
     *
     * @param from The <code>Tile</code> to consider.
     * @return True if the move is allowed.
     */
    private boolean allowMoveFrom(Tile from) {
        return from.isLand()
            || (!getOwner().isREF()
                && getSpecification().getBoolean(GameOptions.AMPHIBIOUS_MOVES));
    }

    /**
     * Is this unit allowed to contact a settlement?
     *
     * @param settlement The <code>Settlement</code> to consider.
     * @return True if the contact is allowed.
     */
    private boolean allowContact(Settlement settlement) {
        return getOwner().hasContacted(settlement.getOwner());
    }

    /**
     * Does a basic check whether a unit can ever expect to move to a tile.
     *
     * @param tile The code <code>Tile</code> to check.
     * @return True if some sort of legal move to the tile exists, including
     *     special cases where there is an interaction but the unit does not
     *     actually move, such as trade.
     */
    public boolean isTileAccessible(Tile tile) {
        return (isNaval()) ? !tile.isLand() || (tile.getSettlement() != null
            && getOwner() == tile.getSettlement().getOwner())
            : tile.isLand();
    }

    /**
     * Gets the amount of moves this unit has at the beginning of each turn.
     *
     * @return The amount of moves this unit has at the beginning of
     *     each turn.
     */
    public int getInitialMovesLeft() {
        return (int)FeatureContainer.applyModifierSet(unitType.getMovement(),
            getGame().getTurn(),
            getModifierSet("model.modifier.movementBonus"));
    }

    /**
     * Make a label showing the unit moves left.
     *
     * @return A movement label.
     */
    public String getMovesAsString() {
        StringBuilder sb = new StringBuilder(16);
        int quotient = getMovesLeft() / 3;
        int remainder = getMovesLeft() % 3;
        if (remainder == 0 || quotient > 0) {
            sb.append(quotient);
        }
        if (remainder > 0) {
            if (sb.length() > 0) sb.append(" ");
            sb.append("(").append(remainder).append("/3) ");
        }

        sb.append("/").append(getInitialMovesLeft() / 3);
        return sb.toString();
    }

    /**
     * Gets the number of turns this unit will need to sail to/from Europe.
     *
     * @return The number of turns to sail to/from Europe.
     */
    public int getSailTurns() {
        float base = getSpecification().getInteger("model.option.turnsToSail");
        return (int)getOwner().applyModifier(base,
                                             "model.modifier.sailHighSeas",
                                             unitType, getGame().getTurn());
    }

    /**
     * Checks if this <code>Unit</code> can be moved to the high seas
     * from its current location.
     *
     * @return True if this unit can move immediately to the high seas.
     */
    public boolean canMoveToHighSeas() {
        if (isInEurope() || isAtSea()) return true;
        if (!getOwner().canMoveToEurope()
            || !getType().canMoveToHighSeas()) return false;
        return getTile().isDirectlyHighSeasConnected();
    }

    /**
     * Does this unit have a valid move to the high seas this turn.
     *
     * @return True if the unit can either move immediately to the high
     *      seas or can make a move to a neighbouring high seas tile.
     */
    public boolean hasHighSeasMove() {
        if (canMoveToHighSeas()) return true;
        Tile tile = getTile();
        if (tile != null && getMovesLeft() > 0) {
            for (Tile t : tile.getSurroundingTiles(1)) {
                if (t.isDirectlyHighSeasConnected()
                    && getMoveType(t).isLegal()) return true;
            }
        }
        return false;
    }

    /**
     * Check if this unit can build a colony.  Does not consider whether
     * the tile where the unit is located is suitable,
     * @see Player#canClaimToFoundSettlement(Tile)
     *
     * @return <code>true</code> if this unit can build a colony.
     */
    public boolean canBuildColony() {
        return unitType.canBuildColony() && getMovesLeft() > 0
            && getTile() != null;
    }

    /**
     * Is this unit at a specified location?
     *
     * @param loc The <code>Location</code> to test.
     * @return True if the locations are the same, or on the same tile.
     */
    public boolean isAtLocation(Location loc) {
        Location ourLoc = getLocation();
        if (ourLoc instanceof Unit) ourLoc = ((Unit)ourLoc).getLocation();
        return Map.isSameLocation(ourLoc, loc);
    }

    /**
     * Gets the best (closest) entry location for this unit to reach a
     * given tile.
     *
     * @param tile The target <code>Tile</code>.
     * @return The best entry location tile to arrive on the map at, or null
     *     if none found.
     */
    public Tile getBestEntryTile(Tile tile) {
        return getGame().getMap().getBestEntryTile(this, tile, null, null);
    }

    /**
     * Resolves a destination for a unit on the high seas.
     * That is, the location where the unit will appear when it leaves
     * the high seas, which will either be Europe or a tile.
     *
     * @return The location the unit should appear next after leaving
     *      the high seas.
     */
    public Location resolveDestination() {
        if (!isAtSea()) throw new IllegalArgumentException("Not at sea.");
        Stop stop = getStop();
        Location dst = (TradeRoute.isStopValid(this, stop)) ? stop.getLocation()
            : getDestination();
        Tile best;
        return (dst == null) ? getFullEntryLocation()
            : (dst instanceof Europe) ? dst
            : (dst.getTile() != null
                && (best = getBestEntryTile(dst.getTile())) != null) ? best
            : getFullEntryLocation();
    }

    /**
     * Set movesLeft to 0 if has some spent moves and it's in a colony
     *
     * @see #add(Locatable)
     * @see #remove(Locatable)
     */
    private void spendAllMoves() {
        if (getColony() != null && getMovesLeft() < getInitialMovesLeft()) {
            setMovesLeft(0);
        }
    }

    /**
     * Is this unit a suitable `next active unit', that is, the unit
     * needs to be currently movable by the player.
     *
     * @return True if this unit could still be moved by the player.
     */
    public boolean couldMove() {
        return !isDisposed()
            && getState() == UnitState.ACTIVE
            && getMovesLeft() > 0
            && destination == null // Can not reach next tile
            && tradeRoute == null
            && !isDamaged()
            && !isAtSea()
            && !isOnCarrier()
            // this should never happen anyway, since these units
            // should have state IN_COLONY, but better safe than sorry
            && !(location instanceof WorkLocation);
    }


    // Map searching support routines

    /**
     * Gets a suitable tile to start path searches from for a unit.
     *
     * Must handle all the cases where the unit is off the map, and
     * take account of the use of a carrier.
     *
     * If the unit is in or heading to Europe, return null because there
     * is no good way to tell where the unit will reappear on the map,
     * which is in question anyway if not on a carrier.
     *
     * @return A suitable starting tile, or null if none found.
     */
    public Tile getPathStartTile() {
        if (isOnCarrier()) {
            final Unit carrier = getCarrier();
            return (carrier.getTile() != null)
                ? carrier.getTile()
                : (carrier.getDestination() instanceof Map)
                ? carrier.getFullEntryLocation()
                : (carrier.getDestination() instanceof Settlement)
                ? ((Settlement)carrier.getDestination()).getTile()
                //: (carrier.getDestination() instanceof Europe)
                //? null
                : null;
        }
        return getTile(); // Or null if heading to or in Europe.
    }

    /**
     * Should the unit use transport to get to a specified tile?
     *
     * True if:
     * - The location is not null
     * - The unit is not naval
     * - The unit is not there already
     * AND
     *   - there is no path OR the path uses an existing carrier
     *
     * @param loc The <code>Location</code> to go to.
     * @return True if the unit should use transport.
     */
    public boolean shouldTakeTransportTo(Location loc) {
        PathNode path;
        return loc != null
            && !isNaval()
            && !isAtLocation(loc)
            && ((path = findPath(getLocation(), loc,
                                 getCarrier(), null)) == null
                || path.usesCarrier());
    }

    /**
     * Finds the fastest path from the current location to the
     * specified one.  No carrier is provided, and the default cost
     * decider for this unit is used.
     *
     * @param end The <code>Location</code> in which the path ends.
     * @return A <code>PathNode</code> from the current location to the
     *     end location, or null if none found.
     */
    public PathNode findPath(Location end) {
        return findPath(getLocation(), end, null, null);
    }

    /**
     * Finds a quickest path between specified locations, optionally
     * using a carrier and special purpose cost decider.
     *
     * @param start The <code>Location</code> to start at.
     * @param end The <code>Location</code> to end at.
     * @param carrier An optional carrier <code>Unit</code> to carry the unit.
     * @param costDecider An optional <code>CostDecider</code> for
     *     determining the movement costs (uses default cost deciders
     *     for the unit/s if not provided).
     * @return A <code>PathNode</code>, or null if no path is found.
     */
    public PathNode findPath(Location start, Location end, Unit carrier,
                             CostDecider costDecider) {
        return getGame().getMap().findPath(this, start, end,
                                           carrier, costDecider);
    }

    /**
     * Gets the number of turns required for this unit to reach a
     * destination location from its current position.  If the unit is
     * currently on a carrier, it will be used.
     *
     * @param end The destination <code>Location</code>.
     * @return The number of turns it will take to reach the destination,
     *         or <code>INFINITY</code> if no path can be found.
     */
    public int getTurnsToReach(Location end) {
        return getTurnsToReach(getLocation(), end);
    }

    /**
     * Gets the number of turns required for this unit to reach a
     * destination location from a starting location.  If the unit is
     * currently on a carrier, it will be used.
     *
     * @param start The <code>Location</code> to start the search from.
     * @param end The destination <code>Location</code>.
     * @return The number of turns it will take to reach the <code>end</code>,
     *         or <code>INFINITY</code> if no path can be found.
     */
    public int getTurnsToReach(Location start, Location end) {
        return getTurnsToReach(start, end, getCarrier(),
            CostDeciders.avoidSettlementsAndBlockingUnits());
    }

    /**
     * Gets the number of turns required for this unit to reach a
     * destination location from a starting location, using an optional
     * carrier and cost decider.
     *
     * @param start The <code>Location</code> to start the search from.
     * @param end The destination <code>Location</code>.
     * @param carrier An optional carrier <code>Unit</code> to use.
     * @param costDecider An optional <code>CostDecider</code> to
     *     score the path with.
     * @return The number of turns it will take to reach the <code>end</code>,
     *         or <code>INFINITY</code> if no path can be found.
     */
    public int getTurnsToReach(Location start, Location end, Unit carrier,
                               CostDecider costDecider) {
        PathNode path = findPath(start, end, carrier, costDecider);
        return (path == null) ? INFINITY : path.getTotalTurns();
    }

    /**
     * Find a path for this unit to the nearest settlement with the
     * same owner that is reachable without a carrier.
     *
     * @param excludeStart If true, ignore any settlement the unit is
     *     currently in.
     * @param range An upper bound on the number of moves.
     * @param coastal If true, the settlement must have a path to Europe.
     * @return The nearest matching settlement if any, otherwise null.
     */
    public PathNode findOurNearestSettlement(final boolean excludeStart,
                                             int range, final boolean coastal) {
        final Player player = getOwner();
        if (player.getNumberOfSettlements() <= 0
            || getTile() == null) return null;

        final Tile startTile = getTile();
        final GoalDecider gd = new GoalDecider() {
                private int bestValue = Integer.MAX_VALUE;
                private PathNode best = null;

                public PathNode getGoal() { return best; }
                public boolean hasSubGoals() { return true; }
                public boolean check(Unit u, PathNode path) {
                    Tile t = path.getTile();
                    if (t == null
                        || (t == startTile && excludeStart)) return false;
                    Settlement settlement = t.getSettlement();
                    int value;
                    if (settlement != null
                        && settlement.getOwner() == player
                        && (!coastal || settlement.isConnectedPort())
                        && (value = path.getTotalTurns()) < bestValue) {
                        bestValue = value;
                        best = path;
                        return true;
                    }
                    return false;
                }
            };
        return search(startTile, gd, CostDeciders.avoidIllegal(), range, null);
    }

    /**
     * Find a path for this unit to the nearest settlement with the
     * same owner that is reachable without a carrier.
     *
     * @return A path to the nearest settlement if any, otherwise null.
     */
    public PathNode findOurNearestSettlement() {
        return findOurNearestSettlement(false, Integer.MAX_VALUE, false);
    }

    /**
     * Find a path for this unit to the nearest settlement with the
     * same owner that is reachable without a carrier and is connected to
     * Europe by sea, or Europe if it is closer.
     *
     * @return A path to the nearest settlement if any, otherwise null
     *     (for now including if in Europe or at sea).
     */
    public PathNode findOurNearestPort() {
        PathNode ePath = null;
        int eTurns = -1;
        Europe europe = getOwner().getEurope();
        if (getType().canMoveToHighSeas()) {
            ePath = (europe == null) ? null : findPath(europe);
            eTurns = (ePath == null) ? -1 : ePath.getTotalTurns();
        }
        PathNode sPath = findOurNearestSettlement(false, INFINITY, true);
        int sTurns = (sPath == null) ? -1 : sPath.getTotalTurns();
        return (ePath == null) ? sPath
            : (sPath == null) ? ePath
            : (sTurns <= eTurns) ? sPath : ePath;
    }

    /**
     * Find a path to a port nearest to a destination.
     * Used by ships to find where to deliver goods to inland colonies.
     *
     * The absolute best port is not necessarily found as that depends
     * on the movement characteristics of the unit that makes the
     * final delivery, which is as yet unknown.  We just pick the
     * closest in basic tile count.
     *
     * @param dst The destination <code>Tile</code>.
     * @return A path to the port, or null if none found.
     */
    public PathNode findIntermediatePort(Tile dst) {
        int dstCont = dst.getContiguity();
        Settlement best = null;
        int bestValue = INFINITY;
        int value;
        for (Settlement s : getOwner().getSettlements()) {
            if (s.getTile().getContiguity() == dstCont
                && s.isConnectedPort()
                && bestValue > (value = dst.getDistanceTo(s.getTile()))) {
                bestValue = value;
                best = s;
            }
        }
        return (best == null) ? null : findPath(best.getTile());
    }
 
    /**
     * Find a path for this unit to the nearest settlement with the
     * same owner that is reachable without a carrier, excepting any
     * on the current tile.
     *
     * @return The nearest settlement if any, otherwise null.
     */
    public PathNode findOurNearestOtherSettlement() {
        return findOurNearestSettlement(true, Integer.MAX_VALUE, false);
    }

    /**
     * Convenience wrapper for the
     * {@link net.sf.freecol.common.model.Map#search} function.
     *
     * @param start The <code>Location</code> to start the search from.
     * @param gd The object responsible for determining whether a
     *     given <code>PathNode</code> is a goal or not.
     * @param cd An optional <code>CostDecider</code>
     *     responsible for determining the path cost.
     * @param maxTurns The maximum number of turns the given
     *     <code>Unit</code> is allowed to move. This is the
     *     maximum search range for a goal.
     * @param carrier An optional naval carrier <code>Unit</code> to use.
     * @return The path to a goal, or null if none can be found.
     */
    public PathNode search(Location start, GoalDecider gd,
                           CostDecider cd, int maxTurns, Unit carrier) {
        return getGame().getMap().search(this, start, gd, cd, maxTurns,
                                         carrier);
    }

    /**
     * Can this unit attack a specified defender?
     *
     * A naval unit can never attack a land unit or settlement,
     * but a land unit *can* attack a naval unit if it is beached.
     * Otherwise naval units can only fight at sea, land units
     * only on land.
     *
     * @param defender The defending <code>Unit</code>.
     * @return True if this unit can attack.
     */
    public boolean canAttack(Unit defender) {
        if (!isOffensiveUnit()
            || defender == null
            || defender.getTile() == null) return false;
        Tile tile = defender.getTile();

        return (isNaval())
            ? (tile.getSettlement() == null && defender.isNaval())
            : (!defender.isNaval() || defender.isBeached());
    }

    /**
     * Searches for a unit that is a credible threatening unit to this
     * unit within a range.
     *
     * @param range The number of turns to search for a threat in.
     * @param threat The maximum tolerable probability of a potentially
     *            threatening unit defeating this unit in combat.
     * @return A path to the threat, or null if not found.
     */
    public PathNode searchForDanger(final int range, final float threat) {
        final CombatModel cm = getGame().getCombatModel();
        final Tile start = getTile();
        final GoalDecider threatDecider = new GoalDecider() {
                private PathNode found = null;

                public PathNode getGoal() { return found; }
                public boolean hasSubGoals() { return false; }
                public boolean check(Unit unit, PathNode path) {
                    Tile tile = path.getTile();
                    if (tile == null) return false;
                    Unit first = tile.getFirstUnit();
                    if (first == null
                        || !getOwner().atWarWith(first.getOwner())) {
                        return false;
                    }
                    for (Unit u : tile.getUnitList()) {
                        PathNode reverse;
                        if (u.canAttack(unit)
                            && cm.calculateCombatOdds(u, unit).win >= threat
                            && (reverse = u.findPath(start)) != null
                            && reverse.getTotalTurns() < range) {
                            found = path;
                            return true;
                        }
                    }
                    return false;
                }
            };
        // The range to search will depend on the speed of the other
        // unit.  We can not know what it will be in advance, and it
        // might be significantly faster than this unit.  We do not
        // want to just use an unbounded search range because this
        // routine must be quick (especially when the supplied range
        // is low).  So use the heuristic of increasing the range by
        // the ratio of the fastest appropriate (land/naval) unit type
        // speed over the unit speed.
        int reverseRange = range * (((isNaval())
                ? getSpecification().getFastestNavalUnitType()
                : getSpecification().getFastestLandUnitType())
            .getMovement()) / this.getType().getMovement();

        return (start == null) ? null
            : search(start, threatDecider, CostDeciders.avoidIllegal(),
                     reverseRange, getCarrier());
    }

    /**
     * Checks if there is a credible threatening unit to this unit
     * within a range of moves.
     *
     * @param range The number of turns to search for a threat within.
     * @param threat The maximum tolerable probability of a potentially
     *            threatening unit defeating this unit in combat.
     * @return True if a threat was found.
     */
    public boolean isInDanger(int range, float threat) {
        return searchForDanger(range, threat) != null;
    }

    /**
     * Gets the line of sight of this <code>Unit</code>. That is the distance
     * this <code>Unit</code> can spot new tiles, enemy unit e.t.c.
     *
     * @return The line of sight of this <code>Unit</code>.
     */
    public int getLineOfSight() {
        Turn turn = getGame().getTurn();
        Set<Modifier> modifierSet = new HashSet<Modifier>();
        modifierSet.addAll(getModifierSet("model.modifier.lineOfSightBonus"));
        if (getTile() != null && getTile().getType() != null) {
            modifierSet.addAll(getTile().getType()
                .getModifierSet("model.modifier.lineOfSightBonus",
                                unitType, turn));
        }
        return (int)FeatureContainer
            .applyModifierSet((float)unitType.getLineOfSight(),
                              turn, modifierSet);
    }


    // Goods handling

    /**
     * Get the goods carried by this unit.
     *
     * @return A list of <code>Goods</code>.
     */
    public List<Goods> getGoodsList() {
        if (getGoodsContainer() == null) return Collections.emptyList();
        return getGoodsContainer().getGoods();
    }

    /**
     * Get a compact version of the goods carried by this unit.
     *
     * @return A compact list of <code>Goods</code>.
     */
    public List<Goods> getCompactGoodsList() {
        if (getGoodsContainer() == null) return Collections.emptyList();
        return getGoodsContainer().getCompactGoods();
    }

    /**
     * Can this unit carry other units?
     *
     * @return True if the unit can carry units.
     */
    public boolean canCarryUnits() {
        return hasAbility(Ability.CARRY_UNITS);
    }

    /**
     * Could this unit carry a specified one?
     * This ignores the current load.
     *
     * @param u The potential cargo <code>Unit</code>.
     * @return True if this unit can carry the cargo.
     */
    public boolean couldCarry(Unit u) {
        return canCarryUnits()
            && getCargoCapacity() >= u.getSpaceTaken();
    }

    /**
     * Can this unit carry goods.
     *
     * @return True if the unit can carry goods.
     */
    public boolean canCarryGoods() {
        return hasAbility(Ability.CARRY_GOODS);
    }

    /**
     * Could this unit carry some specified goods?
     * This ignores the current load.
     *
     * @param g The potential cargo <code>Goods</code>.
     * @return True if this unit can carry the cargo.
     */
    public boolean couldCarry(Goods g) {
        return canCarryGoods()
            && getCargoCapacity() >= g.getSpaceTaken();
    }

    /**
     * Gets the number of free cargo slots left on this unit.
     *
     * @return The number of free cargo slots on this unit.
     */
    public int getSpaceLeft() {
        return getCargoCapacity() - getCargoSpaceTaken();
    }

    /**
     * Is there free space left on this unit?
     *
     * @return True if there is free space.
     */
    public boolean hasSpaceLeft() {
        return getSpaceLeft() > 0;
    }

    /**
     * Gets the total space this unit has to carry cargo.
     *
     * @return The total space.
     */
    public int getCargoCapacity() {
        return unitType.getSpace();
    }

    /**
     * Gets the space occupied by goods in this unit.  Must defend
     * against goods container being null as this can be called in the
     * client on foreign units, which will not have goods containers.
     *
     * @return The number cargo slots occupied by goods.
     */
    public int getGoodsSpaceTaken() {
        if (!canCarryGoods()) return 0;
        GoodsContainer gc = getGoodsContainer();
        return (gc == null) ? 0 : gc.getSpaceTaken();
    }

    /**
     * Gets the space occupied by units in this unit.
     *
     * @return The number of cargo slots occupied by units.
     */
    public int getUnitSpaceTaken() {
        int space = 0;
        if (canCarryUnits()) {
            for (Unit u : getUnitList()) space += u.getSpaceTaken();
        }
        return space;
    }

    /**
     * Gets the space occupied by cargo in this unit (both goods and units).
     *
     * @return The number of occupied cargo slots.
     */
    public int getCargoSpaceTaken() {
        return getGoodsSpaceTaken() + getUnitSpaceTaken();
    }

    /**
     * Is this unit carrying any goods cargo?
     *
     * @return True if the unit is carrying any goods cargo.
     */
    public boolean hasGoodsCargo() {
        return getGoodsSpaceTaken() > 0;
    }

    /**
     * Is this unit carrying any cargo (goods or unit).
     *
     * @return True if the unit is carrying any cargo.
     */
    public boolean hasCargo() {
        return getCargoSpaceTaken() > 0;
    }

    /**
     * Gets the extra amount of a specified type of goods that could
     * be loaded onto this unit.  Includes empty cargo slots and any
     * spare space in a slot partially filled with the specified
     * goods.
     *
     * @param type The <code>GoodsType</code> to examine.
     * @return The amount of goods that could be loaded onto this unit.
     */
    public int getLoadableAmount(GoodsType type) {
        if (!canCarryGoods()) return 0;
        int result = getSpaceLeft() * GoodsContainer.CARGO_SIZE;
        int count = getGoodsCount(type) % GoodsContainer.CARGO_SIZE;
        if (count != 0) result += GoodsContainer.CARGO_SIZE - count;
        return result;
    }


    // Miscellaneous more complex functionality

    /**
     * Checks if this unit is visible to the given player.
     *
     * @param player The <code>Player</code>.
     * @return <code>true</code> if this <code>Unit</code> is visible to the
     *         given <code>Player</code>.
     */
    public boolean isVisibleTo(Player player) {
        Tile unitTile;
        Settlement settlement;

        return (player == getOwner()) ? true
            : ((unitTile = getTile()) == null) ? false
            : (!player.canSee(unitTile)) ? false
            : ((settlement = getSettlement()) != null
                && !player.owns(settlement)) ? false
            : (isOnCarrier() && !player.owns(getCarrier())) ? false
            : true;
    }

    /**
     * Gets a key for the unit occupation.
     *
     * @param owner True if the key should be for the owner of the unit.
     * @return A message key.
     */
    public String getOccupationKey(boolean owner) {
        return (owner)
            ? ((isDamaged())
                ? "model.unit.occupation.underRepair"
                : (getTradeRoute() != null)
                ? "model.unit.occupation.inTradeRoute"
                : (getDestination() != null)
                ? "model.unit.occupation.goingSomewhere"
                : (getState() == Unit.UnitState.IMPROVING
                    && getWorkImprovement() != null)
                ? (getWorkImprovement().getType().getId() + ".occupationString")
                : (getState() == Unit.UnitState.ACTIVE && getMovesLeft() <= 0)
                ? "model.unit.occupation.activeNoMovesLeft"
                : ("model.unit.occupation."
                    + getState().toString().toLowerCase(Locale.US)))
            : (isNaval())
            ? Integer.toString(getVisibleGoodsCount())
            : "model.unit.occupation.activeNoMovesLeft";
    }

    /**
     * Gets a message to display if moving a unit would cause it to
     * abandon its participation in education (if any).
     *
     * @param leavingColony Should we check for student movements.
     * @return A message to display, or null if education is not an issue.
     */
    public StringTemplate getAbandonEducationMessage(boolean leavingColony) {
        if (!(getLocation() instanceof WorkLocation)) return null;
        boolean teacher = getStudent() != null;
        // if leaving the colony, the student loses learning spot, so
        // check with player
        boolean student = leavingColony && getTeacher() != null;
        if (!teacher && !student) return null;

        Building school = (Building)((teacher) ? getLocation()
            : getTeacher().getLocation());
 
        return (leavingColony)
            ? StringTemplate.template("abandonEducation.text")
            .addStringTemplate("%unit%", Messages.getLabel(this))
            .addName("%colony%", getColony().getName())
            .add("%building%", school.getNameKey())
            .addName("%action%", (teacher)
                ? Messages.message("abandonEducation.action.teaching")
                : Messages.message("abandonEducation.action.studying"))
            : (teacher)
            ? StringTemplate.template("abandonTeaching.text")
            .addStringTemplate("%unit%", Messages.getLabel(this))
            .add("%building%", school.getNameKey())
            : null;
    }

    /**
     * Gets the probability that an attack by this unit will provoke a
     * native to convert.
     *
     * @return A probability of conversion.
     */
    public float getConvertProbability() {
        final Specification spec = getSpecification();
        int opt = spec.getInteger("model.option.nativeConvertProbability");
        return 0.01f * FeatureContainer.applyModifierSet(opt,
            getGame().getTurn(),
            getModifierSet("model.modifier.nativeConvertBonus"));
    }

    /**
     * Gets the probability that an attack by this unit will provoke natives
     * to burn our missions.
     *
     * @return A probability of burning missions.
     */
    public float getBurnProbability() {
        // TODO: enhance burn probability proportionally with tension
        final Specification spec = getSpecification();
        return 0.01f * spec.getInteger("model.option.burnProbability");
    }

    /**
     * Get a type change for this unit.
     *
     * @param change The <code>ChangeType</code> to consider.
     * @param owner The <code>Player</code> to own this unit after a
     *    change of type CAPTURE or UNDEAD.
     * @return The resulting unit type or null if there is no change suitable.
     */
    public UnitType getTypeChange(ChangeType change, Player owner) {
        return getType().getTargetType(change, owner);
    }

    /**
     * Checks if the treasure train can be cashed in at it's current
     * <code>Location</code>.
     *
     * @return <code>true</code> if the treasure train can be cashed in.
     * @exception IllegalStateException if this unit is not a treasure train.
     */
    public boolean canCashInTreasureTrain() {
        return canCashInTreasureTrain(getLocation());
    }

    /**
     * Checks if the treasure train can be cashed in at the given
     * <code>Location</code>.
     *
     * @param loc The <code>Location</code>.
     * @return <code>true</code> if the treasure train can be cashed in.
     * @exception IllegalStateException if this unit is not a treasure train.
     */
    public boolean canCashInTreasureTrain(Location loc) {
        if (!canCarryTreasure()) {
            throw new IllegalStateException("Can't carry treasure");
        }
        if (loc == null) return false;

        if (getOwner().getEurope() == null) {
            // Any colony will do once independent, as the treasure stays
            // in the New World.
            return loc.getColony() != null;
        }
        if (loc.getColony() != null) {
            // Cash in if at a colony which has connectivity to Europe
            return loc.getColony().isConnectedPort();
        }
        // Otherwise, cash in if in Europe.
        return loc instanceof Europe
            || (loc instanceof Unit && ((Unit)loc).isInEurope());
    }

    /**
     * Get the fee that would have to be paid to transport this
     * treasure to Europe.
     *
     * @return The fee required for transport.
     */
    public int getTransportFee() {
        if (!isInEurope() && getOwner().getEurope() != null) {
            float fee = (getSpecification().getInteger("model.option.treasureTransportFee")
                         * getTreasureAmount()) / 100;
            return (int) getOwner().applyModifier(fee,
                "model.modifier.treasureTransportFee",
                unitType, getGame().getTurn());
        }
        return 0;
    }

    /**
     * Gets the skill level.
     *
     * @return The level of skill for this unit.  A higher value
     *     signals a more advanced type of units.
     */
    public int getSkillLevel() {
        return getSkillLevel(unitType);
    }

    /**
     * Gets the skill level of the given type of <code>Unit</code>.
     *
     * @param unitType The type of <code>Unit</code>.
     * @return The level of skill for the given unit.  A higher value
     *     signals a more advanced type of units.
     */
    public static int getSkillLevel(UnitType unitType) {
        return (unitType.hasSkill()) ? unitType.getSkill() : 0;
    }

    /**
     * Get a Comparator that compares the skill levels of given units.
     *
     * @return skill Comparator
     */
    public static Comparator<Unit> getSkillLevelComparator() {
        return skillLevelComp;
    }

    /**
     * Does this unit or its owner satisfy the ability set identified
     * by the object identifier.
     *
     * @param id The id of the ability to test.
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     ability applies to.
     * @param turn An optional applicable <code>Turn</code>.
     * @return True if the ability is present.
     */
    public boolean hasAbility(String id, FreeColGameObjectType fcgot,
                              Turn turn) {
        if (turn == null) turn = getGame().getTurn();
        Set<Ability> result = new HashSet<Ability>();
        // UnitType abilities always apply
        result.addAll(unitType.getAbilitySet(id));
        // The player's abilities require more qualification.
        result.addAll(getOwner().getAbilitySet(id, unitType, turn));
        // EquipmentType abilities always apply.
        for (EquipmentType equipmentType : equipment.keySet()) {
            result.addAll(equipmentType.getAbilitySet(id));
            // Player abilities may also apply to equipment (e.g. missionary).
            result.addAll(getOwner().getAbilitySet(id, equipmentType, turn));
        }
        // Location abilities may apply.
        // TODO: extend this to all locations? May simplify
        // code. Units are also Locations, however, which complicates
        // the issue. We do not want Units aboard other Units to share
        // the abilities of the carriers.
        if (getSettlement() != null) {
            result.addAll(getSettlement().getAbilitySet(id, unitType, turn));
        } else if (isInEurope()) {
            result.addAll(getOwner().getEurope().getAbilitySet(id, unitType, turn));
        }
        return FeatureContainer.hasAbility(result);
    }

    /**
     * Get the modifiers that apply to this Unit.
     *
     * @param id The id of the modifier to test.
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     modifier applies to.
     * @param turn An optional applicable <code>Turn</code>.
     * @return A set of modifiers.
     */
    public Set<Modifier> getModifierSet(String id, FreeColGameObjectType fcgot,
                                        Turn turn) {
        if (turn == null) turn = getGame().getTurn();
        Set<Modifier> result = new HashSet<Modifier>();
        // UnitType modifiers always apply
        result.addAll(unitType.getModifierSet(id));
        // the player's modifiers may not apply
        result.addAll(getOwner().getModifierSet(id, unitType, turn));
        // EquipmentType modifiers always apply
        for (EquipmentType equipmentType : equipment.keySet()) {
            result.addAll(equipmentType.getModifierSet(id));
            // player modifiers may also apply to equipment (unused)
            result.addAll(getOwner().getModifierSet(id, equipmentType, turn));
        }
        return result;
    }

    /**
     * Get a modifier that applies to the given Ownable. This is used
     * for the offenceAgainst and defenceAgainst modifiers.
     *
     * @param id The object identifier to test.
     * @param ownable a <code>Ownable</code> value
     * @return a <code>Modifier</code> value
     */
    public Set<Modifier> getModifierSet(String id, Ownable ownable) {
        Set<Modifier> result = new HashSet<Modifier>();
        Turn turn = getGame().getTurn();
        NationType nationType = ownable.getOwner().getNationType();
        result.addAll(unitType.getModifierSet(id, nationType, turn));
        result.addAll(getOwner().getModifierSet(id, nationType, turn));
        for (EquipmentType equipmentType : equipment.keySet()) {
            result.addAll(equipmentType.getModifierSet(id, nationType, turn));
        }
        return result;
    }

    /**
     * Adds a feature to the Unit.  This method always throws an
     * <code>UnsupportedOperationException</code>, since features can
     * not be added to Units directly.
     *
     * @param feature The <code>Feature</code> to add.
     */
    public void addFeature(Feature feature) {
        throw new UnsupportedOperationException("Can not add Feature to Unit directly!");
    }


    // Message unpacking support.

    /**
     * Gets the tile in a given direction.
     *
     * @param directionString The direction.
     * @return The <code>Tile</code> in the given direction.
     * @throws IllegalStateException if there is trouble.
     */
    public Tile getNeighbourTile(String directionString) {
        if (getTile() == null) {
            throw new IllegalStateException("Unit is not on the map: "
                + getId());
        }

        Direction direction = Enum.valueOf(Direction.class, directionString);
        Tile tile = getTile().getNeighbourOrNull(direction);
        if (tile == null) {
            throw new IllegalStateException("Could not find tile"
                + " in direction: " + direction + " from unit: " + getId());
        }
        return tile;
    }

    /**
     * Get a settlement by identifier, validating as much as possible.
     * Designed for message unpacking where the identifier should not
     * be trusted.
     *
     * @param settlementId The identifier of the
     *     <code>Settlement</code> to be found.
     * @return The settlement corresponding to the settlementId argument.
     * @throws IllegalStateException on failure to validate the settlementId
     *     in any way.
     */
    public Settlement getAdjacentSettlementSafely(String settlementId)
        throws IllegalStateException {
        Game game = getOwner().getGame();

        Settlement settlement = game.getFreeColGameObject(settlementId,
                                                          Settlement.class);
        if (settlement == null) {
            throw new IllegalStateException("Not a settlement: "
                + settlementId);
        } else if (settlement.getTile() == null) {
            throw new IllegalStateException("Settlement is not on the map: "
                + settlementId);
        }

        if (getTile() == null) {
            throw new IllegalStateException("Unit is not on the map: "
                + getId());
        } else if (getTile().getDistanceTo(settlement.getTile()) > 1) {
            throw new IllegalStateException("Unit " + getId()
                + " is not adjacent to settlement: " + settlementId);
        } else if (getOwner() == settlement.getOwner()) {
            throw new IllegalStateException("Unit: " + getId()
                + " and settlement: " + settlementId
                + " are both owned by player: " + getOwner().getId());
        }

        return settlement;
    }

    /**
     * Get an adjacent Indian settlement by identifier, validating as
     * much as possible, including checking whether the nation
     * involved has been contacted.  Designed for message unpacking
     * where the identifier should not be trusted.
     *
     * @param id The identifier of the <code>IndianSettlement</code>
     *     to be found.
     * @return The settlement corresponding to the settlementId argument.
     * @throws IllegalStateException on failure to validate the settlementId
     *     in any way.
     */
    public IndianSettlement getAdjacentIndianSettlementSafely(String id)
        throws IllegalStateException {
        Settlement settlement = getAdjacentSettlementSafely(id);
        if (!(settlement instanceof IndianSettlement)) {
            throw new IllegalStateException("Not an indianSettlement: " + id);
        } else if (!getOwner().hasContacted(settlement.getOwner())) {
            throw new IllegalStateException("Player has not contacted the "
                + settlement.getOwner().getNation());
        }

        return (IndianSettlement)settlement;
    }


    // Interface Consumer

    /**
     * {@inheritDoc}
     */
    public List<AbstractGoods> getConsumedGoods() {
        return unitType.getConsumedGoods();
    }

    /**
     * {@inheritDoc}
     */
    public ProductionInfo getProductionInfo(List<AbstractGoods> input) {
        ProductionInfo result = new ProductionInfo();
        result.setConsumption(getType().getConsumedGoods());
        result.setMaximumConsumption(getType().getConsumedGoods());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public int getPriority() {
        return unitType.getPriority();
    }


    // Interface Ownable

    /**
     * {@inheritDoc}
     */
    public Player getOwner() {
        return owner;
    }

    /**
     * {@inheritDoc}
     */
    public void setOwner(Player owner) {
        Player oldOwner = this.owner;

        // safeguard
        if (oldOwner == owner) {
            return;
        } else if (oldOwner == null) {
            logger.warning("Unit " + getId() + " had no previous owner, when changing owner to " + owner.getId());
        }

        // Clear trade route and goto orders if changing owner.
        if (getTradeRoute() != null) {
            setTradeRoute(null);
        }
        if (getDestination() != null) {
            setDestination(null);
        }

        // This need to be set right away
        this.owner = owner;
        // If its a carrier, we need to update the units it has loaded
        //before finishing with it
        for (Unit unit : getUnitList()) {
            unit.setOwner(owner);
        }

        if(oldOwner != null) {
            oldOwner.removeUnit(this);
            oldOwner.modifyScore(-getType().getScoreValue());
            // for speed optimizations
            if(!isOnCarrier()){
                oldOwner.invalidateCanSeeTiles();
            }
        }
        owner.addUnit(this);
        if (getType() != null) { // can be null when fixing integrity
            owner.modifyScore(getType().getScoreValue());
        }

        // for speed optimizations
        if(!isOnCarrier()) {
            getOwner().setExplored(this);
        }

        getGame().notifyOwnerChanged(this, oldOwner, owner);
    }

    // Interface Location (from GoodsLocation via UnitLocation)
    // Inherits
    //   FreeColObject.getId
    //   UnitLocation.getLocationNameFor
    //   UnitLocation.contains
    //   UnitLocation.canAdd
    //   UnitLocation.getUnitCount
    //   UnitLocation.getUnitList
    //   GoodsLocation.getGoodsContainer

    /**
     * {@inheritDoc}
     */
    @Override
    public Tile getTile() {
        return (location != null) ? location.getTile() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StringTemplate getLocationName() {
        return StringTemplate.template("onBoard")
            .addStringTemplate("%unit%", getLabel());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean add(Locatable locatable) {
        if (!canAdd(locatable)) {
            return false;
        } else if (locatable instanceof Unit) {
            Unit unit = (Unit)locatable;
            if (super.add(locatable)) {
                // TODO: there seems to be an inconsistency between
                // units moving from an adjacent tile onto a ship and
                // units boarding a ship in-colony.  The former does not
                // appear to come through here (which it probably should)
                // as the ship's moves do not get zeroed.
                spendAllMoves();
                ((Unit)locatable).setState(UnitState.SENTRY);
                return true;
            }
        } else if (locatable instanceof Goods) {
            Goods goods = (Goods)locatable;
            if (super.addGoods(goods)) {
                spendAllMoves();
                return true;
            }
        } else {
            throw new IllegalStateException("Can not be added to unit: "
                + ((FreeColGameObject)locatable).toString());
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(Locatable locatable) {
        if (locatable == null) {
            throw new IllegalArgumentException("Locatable must not be 'null'.");
        } else if (locatable instanceof Unit && canCarryUnits()) {
            if (super.remove((Unit)locatable)) {
                spendAllMoves();
                return true;
            }
        } else if (locatable instanceof Goods && canCarryGoods()) {
            if (super.removeGoods((Goods)locatable) != null) {
                spendAllMoves();
                return true;
            }
        } else {
            logger.warning("Tried to remove from unit: "
                + ((FreeColGameObject)locatable));
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Settlement getSettlement() {
        Location location = getLocation();
        return (location != null) ? location.getSettlement() : null;
    }


    // UnitLocation
    // Inherits
    //   UnitLocation.getSpaceTaken
    //   UnitLocation.moveToFront
    //   UnitLocation.clearUnitList

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSpaceTaken() {
        // We do not have to consider what this unit is carrying
        // because carriers can not be put onto carriers.  Yet.
        return unitType.getSpaceTaken();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NoAddReason getNoAddReason(Locatable locatable) {
        if (locatable == this) {
            return NoAddReason.ALREADY_PRESENT;
        } else if (locatable instanceof Unit) {
            return (!canCarryUnits())
                ? NoAddReason.WRONG_TYPE
                : (((Unit)locatable).getSpaceTaken() > getSpaceLeft())
                ? NoAddReason.CAPACITY_EXCEEDED
                : super.getNoAddReason(locatable);
        } else if (locatable instanceof Goods) {
            Goods goods = (Goods)locatable;
            return (!canCarryGoods())
                ? NoAddReason.WRONG_TYPE
                : (goods.getAmount() > getLoadableAmount(goods.getType()))
                ? NoAddReason.CAPACITY_EXCEEDED
                : NoAddReason.NONE;
            // Do not call super.getNoAddReason for goods because
            // the capacity test in GoodsLocation.getNoAddReason does not
            // account for packing and is thus too conservative.
        }
        return super.getNoAddReason(locatable);
    }


    // GoodsLocation
    // Inherits
    //   GoodsLocation.addGoods
    //   GoodsLocation.removeGoods

    /**
     * {@inheritDoc}
     */
    @Override
    public int getGoodsCapacity() {
        return getCargoCapacity();
    }


    // Override FreeColGameObject

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FreeColGameObject> disposeList() {
        List<FreeColGameObject> objects = new ArrayList<FreeColGameObject>();

        if (location != null) {
            location.remove(this);
        }

        if (teacher != null) {
            teacher.setStudent(null);
            teacher = null;
        }

        if (student != null) {
            student.setTeacher(null);
            student = null;
        }

        setHomeIndianSettlement(null);

        getOwner().invalidateCanSeeTiles();
        getOwner().removeUnit(this);

        objects.addAll(super.disposeList());
        return objects;
    }


    // Serialization

    private static final String ATTRITION_TAG = "attrition";
    private static final String COUNT_TAG = "count";
    private static final String CURRENT_STOP_TAG = "currentStop";
    private static final String DESTINATION_TAG = "destination";
    private static final String ENTRY_LOCATION_TAG = "entryLocation";
    private static final String EQUIPMENT_TAG = "equipment";
    private static final String ETHNICITY_TAG = "ethnicity";
    private static final String EXPERIENCE_TAG = "experience";
    private static final String EXPERIENCE_TYPE_TAG = "experienceType";
    private static final String HIT_POINTS_TAG = "hitPoints";
    private static final String INDIAN_SETTLEMENT_TAG = "indianSettlement";
    private static final String LOCATION_TAG = "location";
    private static final String MOVES_LEFT_TAG = "movesLeft";
    private static final String NAME_TAG = "name";
    private static final String NATIONALITY_TAG = "nationality";
    private static final String OWNER_TAG = "owner";
    private static final String ROLE_TAG = "role";
    private static final String STATE_TAG = "state";
    private static final String STUDENT_TAG = "student";
    private static final String TRADE_ROUTE_TAG = "tradeRoute";
    private static final String TEACHER_TAG = "teacher";
    private static final String TREASURE_AMOUNT_TAG = "treasureAmount";
    private static final String TURNS_OF_TRAINING_TAG = "turnsOfTraining";
    private static final String UNIT_TYPE_TAG = "unitType";
    private static final String VISIBLE_GOODS_COUNT_TAG = "visibleGoodsCount";
    private static final String WORK_LEFT_TAG = "workLeft";
    private static final String WORK_TYPE_TAG = "workType";
    // @compat 0.10.5
    private static final String OLD_UNITS_TAG = "units";
    // end @compat
    // @compat 0.10.7
    private static final String OLD_HIT_POINTS_TAG = "hitpoints";
    // end @compat


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw, Player player,
                                   boolean showAll,
                                   boolean toSavedGame) throws XMLStreamException {
        super.writeAttributes(xw);

        boolean full = showAll || toSavedGame || player == getOwner();

        if (name != null) xw.writeAttribute(NAME_TAG, name);

        xw.writeAttribute(UNIT_TYPE_TAG, unitType);

        xw.writeAttribute(MOVES_LEFT_TAG, movesLeft);

        xw.writeAttribute(STATE_TAG, state);

        xw.writeAttribute(ROLE_TAG, role);

        if (location != null) {
            if (full || !(location instanceof Building
                    || location instanceof ColonyTile)) {
                xw.writeLocationAttribute(LOCATION_TAG, location);

            } else {
                xw.writeLocationAttribute(LOCATION_TAG, getColony());
            }
        }

        if (!full && hasAbility(Ability.PIRACY)) {
            // Pirates do not disclose national characteristics.
            xw.writeAttribute(OWNER_TAG, getGame().getUnknownEnemy());

        } else {
            xw.writeAttribute(OWNER_TAG, getOwner());

            if (isPerson()) {
                // Do not write out nationality and ethnicity for non-persons.
                xw.writeAttribute(NATIONALITY_TAG, (nationality != null)
                    ? nationality
                    : getOwner().getNationId());

                xw.writeAttribute(ETHNICITY_TAG, (ethnicity != null)
                    ? ethnicity
                    : getOwner().getNationId());
            }
        }

        xw.writeAttribute(TREASURE_AMOUNT_TAG, treasureAmount);

        if (full) {
            if (entryLocation != null) {
                xw.writeLocationAttribute(ENTRY_LOCATION_TAG, entryLocation);
            }

            xw.writeAttribute(TURNS_OF_TRAINING_TAG, turnsOfTraining);

            if (workType != null) xw.writeAttribute(WORK_TYPE_TAG, workType);
            
            if (experienceType != null) {
                xw.writeAttribute(EXPERIENCE_TYPE_TAG, experienceType);
            }

            xw.writeAttribute(EXPERIENCE_TAG, experience);

            xw.writeAttribute(INDIAN_SETTLEMENT_TAG, indianSettlement);

            xw.writeAttribute(WORK_LEFT_TAG, workLeft);

            xw.writeAttribute(HIT_POINTS_TAG, hitPoints);
            
            xw.writeAttribute(ATTRITION_TAG, attrition);
            
            if (student != null) xw.writeAttribute(STUDENT_TAG, student);
            
            if (teacher != null) xw.writeAttribute(TEACHER_TAG, teacher);

            if (destination != null) {
                xw.writeLocationAttribute(DESTINATION_TAG, destination);
            }

            if (tradeRoute != null) {
                xw.writeAttribute(TRADE_ROUTE_TAG, tradeRoute);

                xw.writeAttribute(CURRENT_STOP_TAG, currentStop);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw, Player player,
                                 boolean showAll,
                                 boolean toSavedGame) throws XMLStreamException {
        boolean full = showAll || toSavedGame || player == getOwner();

        // Do not show enemy units hidden in a carrier:
        if (full) {
            super.writeChildren(xw, player, showAll, toSavedGame);

        } else if (getType().canCarryGoods()) {
            xw.writeAttribute(VISIBLE_GOODS_COUNT_TAG, getVisibleGoodsCount());
        }

        if (workImprovement != null) {
            workImprovement.toXML(xw, player, showAll, toSavedGame);
        }

        for (EquipmentType et : getSortedCopy(equipment.keySet())) {
            xw.writeStartElement(EQUIPMENT_TAG);
            
            xw.writeAttribute(ID_ATTRIBUTE_TAG, et);

            xw.writeAttribute(COUNT_TAG, equipment.getCount(et));

            xw.writeEndElement();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        final Specification spec = getSpecification();
        final Game game = getGame();

        name = xr.getAttribute(NAME_TAG, (String)null);

        owner = xr.makeFreeColGameObject(game, OWNER_TAG, Player.class, true);

        UnitType oldUnitType = unitType;
        unitType = xr.getType(spec, UNIT_TYPE_TAG,
                              UnitType.class, (UnitType)null);

        state = xr.getAttribute(STATE_TAG, UnitState.class, UnitState.ACTIVE);

        role = xr.getAttribute(ROLE_TAG, Role.class, Role.DEFAULT);

        location = xr.makeLocationAttribute(game, LOCATION_TAG);

        entryLocation = xr.makeLocationAttribute(game, ENTRY_LOCATION_TAG);

        movesLeft = xr.getAttribute(MOVES_LEFT_TAG, 0);

        workLeft = xr.getAttribute(WORK_LEFT_TAG, 0);

        attrition = xr.getAttribute(ATTRITION_TAG, 0);

        nationality = xr.getAttribute(NATIONALITY_TAG, (String)null);

        ethnicity = xr.getAttribute(ETHNICITY_TAG, (String)null);

        // TODO: does this make sense?
        if (oldUnitType == null) {
            owner.modifyScore(unitType.getScoreValue());
        } else {
            owner.modifyScore(unitType.getScoreValue()
                - oldUnitType.getScoreValue());
        }

        turnsOfTraining = xr.getAttribute(TURNS_OF_TRAINING_TAG, 0);

        hitPoints = xr.getAttribute(HIT_POINTS_TAG, -1);
        // @compat 0.10.7
        if (hitPoints < 0) hitPoints = xr.getAttribute(OLD_HIT_POINTS_TAG, -1);
        // end @compat

        teacher = xr.makeFreeColGameObject(game, TEACHER_TAG,
                                           Unit.class, false);

        student = xr.makeFreeColGameObject(game, STUDENT_TAG,
                                           Unit.class, false);

        setHomeIndianSettlement(xr.makeFreeColGameObject(game,
                INDIAN_SETTLEMENT_TAG, IndianSettlement.class, false));

        treasureAmount = xr.getAttribute(TREASURE_AMOUNT_TAG, 0);

        destination = xr.makeLocationAttribute(game, DESTINATION_TAG);

        tradeRoute = xr.findFreeColGameObject(game, TRADE_ROUTE_TAG,
            TradeRoute.class, (TradeRoute)null, false);

        currentStop = (tradeRoute == null) ? -1
            : xr.getAttribute(CURRENT_STOP_TAG, 0);

        experienceType = xr.getType(spec, EXPERIENCE_TYPE_TAG,
                                    GoodsType.class, (GoodsType)null);
        if (experienceType == null && workType != null) {
            experienceType = workType;
        }

        // @compat 0.9.x
        try {
            // this is likely to cause an exception, as the
            // specification might not define grain
            GoodsType grain = spec.getGoodsType("model.goods.grain");
            GoodsType food = spec.getPrimaryFoodType();
            if (food.equals(workType)) workType = grain;
            if (food.equals(experienceType)) experienceType = grain;
        } catch (Exception e) {
            logger.log(Level.FINEST, "Failed to update food to grain.", e);
        }
        // end @compat

        experience = xr.getAttribute(EXPERIENCE_TAG, 0);

        visibleGoodsCount = xr.getAttribute(VISIBLE_GOODS_COUNT_TAG, -1);

        // Make sure you do this after experience and location stuff.
        changeWorkType(xr.getType(spec, WORK_TYPE_TAG, GoodsType.class, null));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        clearUnitList();
        if (getGoodsContainer() != null) getGoodsContainer().removeAll();
        clearEquipment();
        setWorkImprovement(null);

        super.readChildren(xr);

        setRole();
        getOwner().addUnit(this);
        getOwner().invalidateCanSeeTiles();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final Specification spec = getSpecification();
        final Game game = getGame();
        final String tag = xr.getLocalName();

        if (EQUIPMENT_TAG.equals(tag)) {
            // @compat 0.9.x
            int length = xr.getAttribute(ARRAY_SIZE_TAG, 0);
            if (length > 0) {
                for (int index = 0; index < length; index++) {
                    EquipmentType et = xr.getType(spec, "x" + index,
                        EquipmentType.class, (EquipmentType)null);
                    if (et != null) {
                        equipment.incrementCount(et, 1);
                    }
                }
            // end @compat
            } else {
                equipment.incrementCount(spec.getEquipmentType(xr.readId()),
                                         xr.getAttribute(COUNT_TAG, 0));
            }
            xr.closeTag(EQUIPMENT_TAG);

        // @compat 0.10.5
        } else if (OLD_UNITS_TAG.equals(tag)) {
            while (xr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                super.readChild(xr);
            }
        // end @compat

        } else if (TileImprovement.getXMLElementTagName().equals(tag)) {
            setWorkImprovement(xr.readFreeColGameObject(game,
                    TileImprovement.class));

        } else {
            super.readChild(xr);
        }
    }

    /**
     * Gets a string representation of this unit.
     *
     * @param prefix A prefix (e.g. "AIUnit")
     * @return A string representation of this <code>Unit</code>.
     */
    public String toString(String prefix) {
        StringBuilder sb = new StringBuilder(64);
        sb.append("[").append(prefix).append(getId());
        if (isUninitialized()) {
            sb.append(" uninitialized");
        } else if (isDisposed()) {
            sb.append(" disposed");
        } else {
            sb.append(" ").append(Utils.lastPart(owner.getNationId(), "."))
                .append(" ").append(Utils.lastPart(getType().getId(), "."));
            if (getRole() != Role.DEFAULT) {
                sb.append("-").append(getRole());
            }
            sb.append(" ").append(getMovesAsString());
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toString("");
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "unit"
     */
    public static String getXMLElementTagName() {
        return "unit";
    }
}
