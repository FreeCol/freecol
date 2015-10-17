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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.pathfinding.CostDecider;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.common.model.pathfinding.GoalDeciders;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;
import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.StringUtils.*;


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

    /**
     * A large number of turns, denoting pathfinding failure.  Do not use
     * INFINITY as further calculation might use this.
     */
    public static final int MANY_TURNS = 10000;

    public static final String CARGO_CHANGE = "CARGO_CHANGE";
    public static final String MOVE_CHANGE = "MOVE_CHANGE";
    public static final String ROLE_CHANGE = "ROLE_CHANGE";

    /**
     * A comparator to compare units by position, top to bottom,
     * left to right.
     */
    public static final Comparator<Unit> locComparator
        = Comparator.comparingInt(u -> Location.getRank(u));

    /** A comparator to compare units by type then role. */
    public static final Comparator<Unit> typeRoleComparator
        = Comparator.comparing(Unit::getType)
            .thenComparing(Comparator.comparing(Unit::getRole));
    
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
        // end @compat
        FORTIFYING,
        SKIPPED;

        /**
         * Get the stem key for this unit state.
         *
         * @return The stem key.
         */
        public String getKey() {
            return "unitState." + getEnumKey(this);
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
    protected Role role;

    /**
     * The amount of role-equipment this unit carries, subject to
     * role.getMaximumCount().  Currently zero or one except for pioneers.
     */
    protected int roleCount;

    /** The current unit location. */
    protected Location location;

    /** The last entry location used by this unit. */
    protected Location entryLocation;

    /** The number of moves this unit has left this turn. */
    protected int movesLeft;

    /** What type of goods this unit produces in its occupation. */
    protected GoodsType workType;

    /** What type of goods this unit last earned experience producing. */
    protected GoodsType experienceType;

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
    @Override
    public String getName() {
        return name;
    }

    /**
     * Set the individual name of this unit.
     *
     * @param newName The new name.
     */
    @Override
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
        return own.getNationLabel();
    }

    /** What type of unit label do we want? */
    public static enum UnitLabelType {
        PLAIN,      // Just the basics
        NATIONAL,   // Add the nation
        FULL        // Add the equipment and extras
    }

    /**
     * Get a plain string template for a unit.
     *
     * @return The <code>StringTemplate</code> to describe the given unit.
     */
    public StringTemplate getLabel() {
        return getLabel(UnitLabelType.PLAIN);
    }

    /**
     * Get a string template for a unit.
     *
     * The PLAIN template contains:
     * - The type of the unit
     * - A role if not the default
     * - The specific name of the unit if it has one
     * The NATIONAL template adds the nation
     * The FULL template adds equipment annotations
     *
     * @param ult The type of label to get.
     * @return The <code>StringTemplate</code> to describe the given unit.
     */
    public StringTemplate getLabel(UnitLabelType ult) {
        final UnitType type = getType();
        final Role role = getRole();
        final Player owner = getOwner();
        if (type == null || role == null || owner == null) {
            return null; // Probably disposed
        }

        switch (ult) {
        case PLAIN:
            return Messages.getUnitLabel(getName(), type.getId(), 1, null,
                                         role.getId(), null);

        case NATIONAL:
            if (role.getMaximumCount() > 1) {
                // If the amount of equipment can vary an equipment
                // label is required, so fall through into the FULL case.
            } else {
                return Messages.getUnitLabel(getName(), type.getId(), 1,
                                             owner.getNationId(), role.getId(),
                                             null);
            }
            // Fall through

        case FULL:
            StringTemplate extra = null;
            if (role.isDefaultRole()) {
                if (canCarryTreasure()) {
                    extra = StringTemplate.template("goldAmount")
                        .addAmount("%amount%", getTreasureAmount());
                } else {
                    boolean noEquipment = false;
                    // unequipped expert has no-equipment label
                    List<Role> expertRoles = type.getExpertRoles();
                    for (Role someRole : expertRoles) {
                        String key = someRole.getId() + ".noequipment";
                        if (Messages.containsKey(key)) {
                            extra = StringTemplate.key(key);
                            break;
                        }
                    }
                }
            } else {
                String equipmentKey = role.getId() + ".equipment";
                if (Messages.containsKey(equipmentKey)) {
                    // Currently only used for missionary which does not
                    // have equipment that directly corresponds to goods.
                    extra = AbstractGoods.getLabel(equipmentKey, 1);
                } else {
                    // Other roles can be characterized by their goods.
                    List<AbstractGoods> requiredGoods
                        = role.getRequiredGoods(getRoleCount());
                    boolean first = true;
                    extra = StringTemplate.label("");
                    for (AbstractGoods ag : requiredGoods) {
                        if (first) first = false; else extra.addName(" ");
                        extra.addStringTemplate(ag.getLabel());
                    }
                }
            }
            return Messages.getUnitLabel(getName(), type.getId(), 1,
                                         owner.getNationId(), role.getId(),
                                         extra);
        default: // Can not happen
            break;
        }
        return null;
    }

    /**
     * Get the basic i18n description for this unit.
     *
     * @return A <code>String</code> describing this unit.
     */
    public String getDescription() {
        return Messages.message(getLabel());
    }

    /**
     * Get the basic i18n description for this unit.
     *
     * @param ult The label type required.
     * @return A <code>String</code> describing this unit.
     */
    public String getDescription(UnitLabelType ult) {
        return Messages.message(getLabel(ult));
    }

    /**
     * Get a label for the chance of success in a potential combat.
     *
     * @param tile The <code>Tile</code> to attack into.
     * @return A suitable label.
     */
    public StringTemplate getCombatLabel(Tile tile) {
        final CombatModel.CombatOdds combatOdds = getGame().getCombatModel()
            .calculateCombatOdds(this, tile.getDefendingUnit(this));
        // If attacking a settlement, the true odds are never
        // known because units may be hidden within
        boolean unknown = combatOdds.win == CombatModel.CombatOdds.UNKNOWN_ODDS
            || tile.hasSettlement();
        return StringTemplate.template("model.unit.attackTileOdds")
            .addName("%chance%", (unknown) ? "??"
                : String.valueOf((int)(combatOdds.win * 100)));
    }
    
    /**
     * Get a destination label for this unit.
     *
     * @return A <code>StringTemplate</code> describing where this unit
     *     is going.
     */
    public StringTemplate getDestinationLabel() {
        // Create the right tag for the tagged "goingTo" message.
        String type = (isPerson()) ? "person"
            : (isNaval()) ? "ship"
            : "other";
        return getDestinationLabel(type, getDestination(), getOwner());
    }

    /**
     * Get a destination label for a given unit tag, destination and player.
     *
     * @param tag The unit tag for the "goingTo" message.
     * @param destination The destination <code>Location</code>.
     * @param player The <code>Player</code> viewpoint.
     * @return A <code>StringTemplate</code> describing the unit movement.
     */
    public static StringTemplate getDestinationLabel(String tag,
        Location destination, Player player) {
        return StringTemplate.template("model.unit.goingTo")
            .add("%type%", tag)
            .addStringTemplate("%location%",
                destination.getLocationLabelFor(player));
    }

    /**
     * Get a string template describing the repair state of this unit.
     *
     * @return A repair label.
     */
    public StringTemplate getRepairLabel() {
        return StringTemplate.template("model.unit.underRepair")
            .addAmount("%turns%", getTurnsForRepair());
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
     * -vis: Has visibility issues as the line of sight may change.
     *
     * @param unitType The new type of the unit.
     */
    public void setType(UnitType unitType) {
        this.unitType = unitType;
    }

    /**
     * Changes the type of the unit.
     *
     * -vis: Has visibility issues as the line of sight may change.
     *
     * @param unitType The new type of the unit.
     * @return True if the type change succeeds.
     */
    public boolean changeType(UnitType unitType) {
        if (!unitType.isAvailableTo(owner)) return false;

        setType(unitType);
        if (getMovesLeft() > getInitialMovesLeft()) {
            setMovesLeft(getInitialMovesLeft());
        }
        hitPoints = unitType.getHitPoints();
        if (getTeacher() != null && !canBeStudent(getTeacher())) {
            getTeacher().setStudent(null);
            setTeacher(null);
        }
        return true;
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
        return hasAbility(Ability.UNDEAD);
    }

    /**
     * Can this unit carry treasure (like a treasure train)?
     *
     * @return True if this <code>Unit</code> can carry treasure.
     */
    public boolean canCarryTreasure() {
        return hasAbility(Ability.CARRY_TREASURE);
    }

    /**
     * Can this unit capture enemy goods?
     *
     * @return True if this <code>Unit</code> is capable of capturing goods.
     */
    public boolean canCaptureGoods() {
        return hasAbility(Ability.CAPTURE_GOODS);
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
        return hasAbility(Ability.PERSON)
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
            // end @compat
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
        if (getState() == s) return false;
        switch (s) {
        case ACTIVE:
            return true;
        case FORTIFIED:
            return getState() == UnitState.FORTIFYING;
        case FORTIFYING:
            return getMovesLeft() > 0;
        case IMPROVING:
            return getMovesLeft() > 0
                && location instanceof Tile
                && getOwner().canAcquireForImprovement(location.getTile());
        case IN_COLONY:
            return !isNaval();
        case SENTRY:
            return true;
        case SKIPPED:
            return getState() == UnitState.ACTIVE;
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
        // FIXME: move to the server.
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
     * Change the owner of this unit.
     *
     * -vis: This routine calls setOwner() and thus has visibility
     * implications.  Ideally it should be in ServerUnit but we keep
     * it here for the benefit of the test suite.
     *
     * @param owner The new owner <code>Player</code>.
     */
    public void changeOwner(Player owner) {
        final Player oldOwner = this.owner;
        if (oldOwner == owner) return;

        if (oldOwner == null) {
            logger.warning("Unit " + getId()
                + " had no owner, when changing owner to " + owner.getId());
        }

        // This need to be set right away.
        setOwner(owner);

        // Clear trade route and goto orders if changing owner.
        if (getTradeRoute() != null) setTradeRoute(null);
        if (getDestination() != null) setDestination(null);

        // If its a carrier, we need to update the units it has loaded
        // before finishing with it
        for (Unit u : getUnitList()) u.changeOwner(owner);

        if (getTeacher() != null && !canBeStudent(getTeacher())) {
            getTeacher().setStudent(null);
            setTeacher(null);
        }

        if (oldOwner != null) oldOwner.removeUnit(this);
        if (owner != null) owner.addUnit(this);

        getGame().notifyOwnerChanged(this, oldOwner, owner);
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
     * Get the role count.
     *
     * @return The current role count.
     */    
    public int getRoleCount() {
        return roleCount;
    }

    /**
     * Set the role count.
     *
     * @param roleCount The new role count.
     */    
    public void setRoleCount(int roleCount) {
        this.roleCount = roleCount;
    }

    /**
     * Does this unit have the default role?
     *
     * @return True if the unit has the default <code>Role</code>.
     */
    public boolean hasDefaultRole() {
        return role.isDefaultRole();
    }

    /**
     * Get the last part of the role identifier, which is often used as
     * part of a message key.
     *
     * @return The role suffix.
     */
    public String getRoleSuffix() {
        return Role.getRoleSuffix(role.getId());
    }

    /**
     * Change the current role of this unit.
     *
     * @param role The new <code>Role</code>.
     * @param roleCount The new role count.
     */
    public void changeRole(Role role, int roleCount) {
        if (!role.isCompatibleWith(getRole())) {
            // Clear experience if changing to an incompatible role.
            setExperience(0);
        }
        setRole(role);
        setRoleCount((role.isDefaultRole()) ? 0 : roleCount);
    }

    /**
     * Change the current role count.  On zero, revert to default role.
     *
     * @param delta The change to apply to the role count.
     * @return True if the role count reached zero.
     */
    public boolean changeRoleCount(int delta) {
        this.roleCount = Math.max(0, this.roleCount + delta);
        if (this.roleCount != 0) return false;
        this.role = getSpecification().getDefaultRole();
        return true;
    }

    /**
     * Is a role available to this unit?
     *
     * @param role The <code>Role</code> to test.
     * @return True if the role is available to this unit.
     */
    public boolean roleIsAvailable(Role role) {
        return role.isAvailableTo(this);
    }

    /**
     * Filter a list of roles to return only those available to this unit.
     *
     * @param roles The list of <code>Role</code>s to filter, if null all
     *     available roles are used.
     * @return A list of available <code>Role</code>s.
     */
    public List<Role> getAvailableRoles(List<Role> roles) {
        if (roles == null) roles = getSpecification().getRoles();
        return roles.stream()
            .filter(r -> roleIsAvailable(r)).collect(Collectors.toList());
    }

    /**
     * Get a military role for this unit.
     *
     * @return A military <code>Role</code>, or null if none found.
     */
    public Role getMilitaryRole() {
        List<Role> roles
            = getAvailableRoles(getSpecification().getMilitaryRoles());
        return (roles.isEmpty()) ? null : roles.get(0);
    }

    /**
     * Get the change in goods required to change to a new role/count.
     *
     * @param role The new <code>Role</code> to change to.
     * @param roleCount The new role count.
     * @return A list of <code>AbstractGoods</code> defining the change
     *     in goods required.
     */
    public List<AbstractGoods> getGoodsDifference(Role role, int roleCount) {
        return Role.getGoodsDifference(getRole(), getRoleCount(),
                                       role, roleCount);
    }

    /**
     * Sets the units location without updating any other variables
     *
     * get/setLocation are in Locatable interface.
     *
     * -vis: This routine changes player visibility.
     *
     * @param newLocation The new <code>Location</code>.
     */
    public void setLocationNoUpdate(Location newLocation) {
        location = newLocation;
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
        return hasAbility(Ability.ESTABLISH_MISSION)
            && (getLocation() instanceof IndianSettlement
                // @compat 0.10.x
                // Remove this when PET missionary serialization is retired
                || getLocation() == null
                // end @compat 0.10.x
                );
    }

    /**
     * Checks whether this unit is working inside a colony.
     *
     * @return True if in colony.
     */
    public boolean isInColony() {
        return getLocation() instanceof WorkLocation;
    }

    /**
     * Is this unit on a tile?
     *
     * @return True if this unit is on a tile.
     */
    public boolean hasTile() {
        return getTile() != null;
    }


    /**
     * Gets the work location this unit is working in.
     *
     * @return The current <code>WorkLocation</code>, or null if none.
     */
    public WorkLocation getWorkLocation() {
        return (isInColony()) ? (WorkLocation)getLocation() : null;
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
    @Override
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
     * @param typeTeacher The teacher <code>UnitType</code>.
     * @param typeStudent the student <code>UnitType</code>.
     * @return The turns of training needed to teach its current type
     *     to a free colonist or to promote an indentured servant or a
     *     petty criminal.
     * @see #getTurnsOfTraining
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
     * @param newEthnicity The new ethnicity of this Unit.
     */
    public void setEthnicity(String newEthnicity) {
        this.ethnicity = newEthnicity;
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
    public TradeRouteStop getStop() {
        return (validateCurrentStop() < 0) ? null
            : getTradeRoute().getStops().get(currentStop);
    }

    /**
     * Get the stop the unit is heading for or at.
     *
     * @return The target <code>Stop</code>.
     */
    public List<TradeRouteStop> getCurrentStops() {
        if (validateCurrentStop() < 0) return null;
        List<TradeRouteStop> stops
            = new ArrayList<TradeRouteStop>(getTradeRoute().getStops());
        rotate(stops, currentStop);
        return stops;
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
            List<TradeRouteStop> stops = tradeRoute.getStops();
            if (stops == null || stops.isEmpty()) {
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
     * Convenience function to check if a unit is at a stop.
     *
     * @param stop The <code>TradeRouteStop</code> to check.
     * @return True if the unit is at the given stop.
     */
    public boolean atStop(TradeRouteStop stop) {
        return Map.isSameLocation(getLocation(), stop.getLocation());
    }

    /**
     * Get the current trade location.
     *
     * @return The <code>TradeLocation</code> for this unit.
     */
    public TradeLocation getTradeLocation() {
        Colony colony;
        IndianSettlement is;
        return ((colony = getColony()) != null) ? colony
            : ((is = getIndianSettlement()) != null) ? is
            : (isInEurope()) ? (TradeLocation)getOwner().getEurope()
            : null;
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


    // Combat routines

    /**
     * Gets a role that can be equipped automatically assumed
     * in case of an attack.
     *
     * Paul Revere makes an unarmed colonist in a settlement pick up a
     * stock-piled musket if attacked, so the bonus should be applied
     * for unarmed colonists inside colonies where there are muskets
     * available.  Natives can also auto-arm.
     *
     * @return A <code>Role</code> that can be automatically assumed
     *     by this unit, or null if none.
     */
    public Role getAutomaticRole() {
        if (!hasDefaultRole()) return null;
        Set<Ability> autoDefence = getAbilities(Ability.AUTOMATIC_EQUIPMENT);
        if (autoDefence.isEmpty()) return null;
        Settlement settlement = (isInColony()) ? getColony()
            : (getLocation() instanceof IndianSettlement)
            ? (Settlement)getLocation()
            : null;
        if (settlement == null) return null;

        final Specification spec = getSpecification();
        for (Ability ability : autoDefence) {
            for (Scope scope : ability.getScopes()) {
                Role role = spec.getRole(scope.getType());
                if (role != null
                    && settlement.containsGoods(getGoodsDifference(role, 1))) {
                    return role;
                }
            }
        }
        return null;
    }

    /**
     * After winning a battle, can this unit capture the loser's role
     * equipment?
     *
     * @param role The loser unit <code>Role</code>.
     * @return The <code>Role</code> available to this unit as a result
     *     of capturing the loser equipment.
     */
    public Role canCaptureEquipment(Role role) {
        if (!hasAbility(Ability.CAPTURE_EQUIPMENT)) return null;
        final Specification spec = getSpecification();
        final Role oldRole = getRole();
        for (Role r : getAvailableRoles(spec.getMilitaryRoles())) {
            for (Role.RoleChange rc : r.getRoleChanges()) {
                if (rc.getFrom(spec) == oldRole
                    && rc.getCapture(spec) == role) return r;
            }
        }
        return null;
    }

    /**
     * Does losing a piece of equipment mean the death of this unit?
     *
     * @return True if the unit is doomed.
     */
    public boolean losingEquipmentKillsUnit() {
        return hasAbility(Ability.DISPOSE_ON_ALL_EQUIPMENT_LOST)
            && getRole().getDowngrade() == null;
    }

    /**
     * Does losing equipment mean the demotion of this unit?
     *
     * @return True if the unit is to be demoted.
     */
    public boolean losingEquipmentDemotesUnit() {
        return hasAbility(Ability.DEMOTE_ON_ALL_EQUIPMENT_LOST)
            && getRole().getDowngrade() == null;
    }

    /**
     * Does the unit have arms?
     *
     * @return True if the unit has arms.
     */
    public boolean isArmed() {
        return hasAbility(Ability.ARMED);
    }

    /**
     * Does the unit have a mount?
     *
     * @return True if the unit have a mount.
     */
    public boolean isMounted() {
        return hasAbility(Ability.MOUNTED);
    }

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
            && !tile.hasSettlement();
    }

    /**
     * Checks if this is an defensive unit. That is: a unit which can
     * be used to defend a <code>Settlement</code>.
     *
     * Note! As this method is used by the AI it really means that the
     * unit can defend as is. To be specific an unarmed colonist is
     * not defensive yet, even if Paul Revere and stockpiled muskets
     * are available. That check is only performed on an actual
     * attack.
     *
     * A settlement is lost when there are no more defensive units.
     *
     * @return True if this is a defensive unit meaning it can be used
     *     to defend a <code>Colony</code>.  This would normally mean
     *     that a defensive unit also will be offensive.
     */
    public boolean isDefensiveUnit() {
        return (unitType.isDefensive() || getRole().isDefensive())
            && !isCarrier(); // Not wagons or ships
    }

    /**
     * Checks if this is an offensive unit.  That is, one that can
     * attack other units.
     *
     * @return True if this is an offensive unit.
     */
    public boolean isOffensiveUnit() {
        return unitType.isOffensive() || getRole().isOffensive();
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
    public static boolean betterDefender(Unit defender, double defenderPower,
                                         Unit other, double otherPower) {
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
     * this ship can be repaired, excluding the current colony.
     *
     * @return The closest <code>Location</code> where a ship can be
     *     repaired.
     */
    public Location getRepairLocation() {
        final Player player = getOwner();
        final Colony notHere = getTile().getColony();
        Location best = getClosestColony(player.getColonies().stream()
            .filter(c -> c != notHere && c.hasAbility(Ability.REPAIR_UNITS)));
        return (best != null) ? best : player.getEurope();
    }


    // Movement handling

    /**
     * A move type.
     *
     * @see Unit#getMoveType(Direction)
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
        MOVE_NO_ACCESS_MISSION_BAN("Attempt to use missionary at banned settlement"),
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
        private final String reason;

        /**
         * Does this move type imply progress towards a destination.
         */
        private final boolean progress;

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

        int cost = target.getType().getBasicMoveCost();
        if (target.isLand() && !isNaval()) {
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
                 || target.hasSettlement()) && ml != 0) {
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
        Tile target;
        return (!hasTile())
            ? MoveType.MOVE_NO_TILE
            : ((target = getTile().getNeighbourOrNull(direction)) == null)
            ? MoveType.MOVE_ILLEGAL
            : getMoveType(target);
    }

    /**
     * Gets the type of a move that is made when moving from one tile
     * to another.
     *
     * @param target The target <code>Tile</code> of the move.
     * @return The move type.
     */
    public MoveType getMoveType(Tile target) {
        return (!hasTile())
            ? MoveType.MOVE_NO_TILE
            : getMoveType(getTile(), target, getMovesLeft());
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
                break;
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
        return (!hasTile()) ? MoveType.MOVE_NO_TILE
            : getSimpleMoveType(getTile(), target);
    }

    /**
     * Gets the type of a move made in a specified direction,
     * without checking if the unit has moves left or logging errors.
     *
     * @param direction The direction of the move.
     * @return The move type.
     */
    public MoveType getSimpleMoveType(Direction direction) {
        Tile target;
        return (!hasTile())
            ? MoveType.MOVE_NO_TILE
            : ((target = getTile().getNeighbourOrNull(direction)) == null)
            ? MoveType.MOVE_ILLEGAL
            : getSimpleMoveType(getTile(), target);
    }

    /**
     * Gets the type of a move that is made when moving a naval unit
     * from one tile to another.
     *
     * @param from The origin <code>Tile<code> of the move.
     * @param target The target <code>Tile</code> of the move.
     * @return The move type.
     */
    private MoveType getNavalMoveType(@SuppressWarnings("unused") Tile from,
                                      Tile target) {
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
                if (settlement instanceof Colony
                    && hasAbility(Ability.NEGOTIATE)) {
                    return (allowMoveFrom(from))
                        ? MoveType.ENTER_FOREIGN_COLONY_WITH_SCOUT
                        : MoveType.MOVE_NO_ACCESS_WATER;
                } else if (settlement instanceof IndianSettlement
                    && hasAbility(Ability.SPEAK_WITH_CHIEF)) {
                    return (allowMoveFrom(from))
                        ? MoveType.ENTER_INDIAN_SETTLEMENT_WITH_SCOUT
                        : MoveType.MOVE_NO_ACCESS_WATER;
                } else if (isOffensiveUnit()) {
                    return (allowMoveFrom(from))
                        ? MoveType.ATTACK_SETTLEMENT
                        : MoveType.MOVE_NO_ATTACK_MARINE;
                } else if (hasAbility(Ability.ESTABLISH_MISSION)) {
                    return getMissionaryMoveType(from, settlement);
                } else {
                    return getLearnMoveType(from, settlement);
                }
            } else if (isOffensiveUnit()) {
                return (allowMoveFrom(from))
                    ? MoveType.ATTACK_SETTLEMENT
                    : MoveType.MOVE_NO_ATTACK_MARINE;
            } else {
                return MoveType.MOVE_NO_ACCESS_SETTLEMENT;
            }
        } else { // moving to sea, check for embarkation
            return (defender == null || !getOwner().owns(defender))
                ? MoveType.MOVE_NO_ACCESS_EMBARK
                : (any(target.getUnitList(), u -> u.canAdd(this)))
                ? MoveType.EMBARK
                : MoveType.MOVE_NO_ACCESS_FULL;
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
                : (!hasAbility(Ability.TRADE_WITH_FOREIGN_COLONIES))
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
                : (settlement.getOwner().missionsBanned(getOwner()))
                ? MoveType.MOVE_NO_ACCESS_MISSION_BAN
                : MoveType.ENTER_INDIAN_SETTLEMENT_WITH_MISSIONARY;
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
        return (isNaval())
            ? (!tile.isLand()
                || (tile.hasSettlement()
                    && getOwner().owns(tile.getSettlement())))
            : tile.isLand();
    }

    /**
     * Gets the amount of moves this unit has at the beginning of each turn.
     *
     * @return The amount of moves this unit has at the beginning of
     *     each turn.
     */
    @Override
    public int getInitialMovesLeft() {
        Turn turn = getGame().getTurn();
        return (int)applyModifiers(unitType.getMovement(), turn,
                                   Modifier.MOVEMENT_BONUS, unitType);
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
        if (quotient > 0 || remainder == 0) sb.append(quotient);
        if (remainder > 0) {
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
        float base = getSpecification().getInteger(GameOptions.TURNS_TO_SAIL);
        return (int)getOwner().applyModifiers(base, getGame().getTurn(),
                                              Modifier.SAIL_HIGH_SEAS,
                                              unitType);
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
        return (canMoveToHighSeas())
            ? true
            : (hasTile() && getMovesLeft() > 0)
            ? any(getTile().getSurroundingTiles(1, 1),
                Tile::isDirectlyHighSeasConnected)
            : false;
    }

    /**
     * Check if this unit can build a colony.  Does not consider whether
     * the tile where the unit is located is suitable,
     * @see Player#canClaimToFoundSettlement(Tile)
     *
     * @return <code>true</code> if this unit can build a colony.
     */
    public boolean canBuildColony() {
        final Specification spec = getSpecification();
        return hasTile() && unitType.canBuildColony() && getMovesLeft() > 0
            && (!getOwner().isRebel()
                || spec.getBoolean(GameOptions.FOUND_COLONY_DURING_REBELLION));
    }

    /**
     * Is this unit at a specified location?
     *
     * @param loc The <code>Location</code> to test.
     * @return True if the locations are the same, or on the same tile.
     */
    public boolean isAtLocation(Location loc) {
        Location ourLoc = getLocation(),
            otherLoc = (loc instanceof Unit) ? ((Unit)loc).getLocation() : loc;
        if (ourLoc instanceof Unit) ourLoc = ((Unit)ourLoc).getLocation();
        return Map.isSameLocation(ourLoc, otherLoc);
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
        TradeRouteStop stop = getStop();
        Location dst = (TradeRoute.isStopValid(this, stop))
            ? stop.getLocation()
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
            && !isInColony();
    }


    // Map support routines

    /**
     * Gets a suitable location to start path searches for a unit.
     *
     * Must handle all the cases where the unit is off the map, and
     * take account of the use of a carrier.
     *
     * @return A suitable starting location, or null if none found.
     */
    public Location getPathStartLocation() {
        final Unit carrier = getCarrier();
        Location ret = getTile();
        if (isOnCarrier()) {
            if (ret != null) {
                ; // OK
            } else if (carrier.getDestination() == null) {
                ret = null;
            } else if (carrier.getDestination() instanceof Map) {
                ret = carrier.getFullEntryLocation();
            } else if (carrier.getDestination() instanceof Settlement) {
                ret = carrier.getDestination();
            } else { // destination must be Europe
                ret = null;
            }
        } else if (isNaval()) {
            if (ret != null) {
                ; // OK
            } else if (getDestination() == null
                || getDestination() instanceof Map) {
                ret = getFullEntryLocation();
            } else if (getDestination() instanceof Settlement) {
                ret = getDestination();
            } else {
                ret = getFullEntryLocation();
            }
        }
        if (ret != null) return ret;

        // Must be a land unit not on the map.  May have a carrier.
        // Get our nearest settlement to Europe, fallback to any other.
        final Player owner = getOwner();
        int bestValue = INFINITY;
        for (Settlement s : owner.getSettlements()) {
            if (s.getTile().isHighSeasConnected()) {
                int value = s.getTile().getHighSeasCount();
                if (bestValue > value) {
                    bestValue = value;
                    ret = s;
                }
            } else if (bestValue == INFINITY) ret = s;
        }
        if (ret != null) return ret;

        // Owner has no settlements.  If it is the REF, start from a
        // rebel colony.  Prefer the closest port.
        if (owner.isREF()) {
            bestValue = INFINITY;
            for (Player p : owner.getRebels()) {
                for (Settlement s : p.getSettlements()) {
                    if (s.getTile().isHighSeasConnected()) {
                        int value = s.getTile().getHighSeasCount();
                        if (bestValue > value) {
                            bestValue = value;
                            ret = s;
                        }
                    } else if (bestValue == INFINITY) ret = s;
                }
            }
            if (ret != null) return ret;
        }

        // Desperately find the nearest land to the entry location.
        Location entry = getFullEntryLocation();
        if (entry != null && entry.getTile() != null) {
            for (Tile t : entry.getTile().getSurroundingTiles(INFINITY)) {
                if (t.isLand()) return t;
            }
        }

        return null; // Fail
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
     * Gets the trivial path for this unit.  That is, the path to the
     * nearest available safe settlement.
     *
     * @return A path to the trivial target, or null if none found.
     */
    public PathNode getTrivialPath() {
        if (isDisposed() || getLocation() == null) return null;
        if (!isNaval()) return findOurNearestSettlement();
        PathNode path = findOurNearestPort();
        if (path == null) {
            // This is unusual, but can happen when a ship is up a
            // river and foreign ship creates a blockage downstream.
            // If so, the rational thing to do is to go to a tile
            // where other units can pass and which has the best
            // connectivity to the high seas.
            Tile tile = getTile();
            if (tile != null && tile.isOnRiver()
                && tile.isHighSeasConnected()) {
                path = search(getLocation(), 
                    GoalDeciders.getCornerGoalDecider(),
                    CostDeciders.avoidSettlementsAndBlockingUnits(),
                    INFINITY, null);
                if (path == null && tile.isRiverCorner()) {
                    // Return trivial path if already present.
                    return new PathNode(tile, 0, 0, false, null, null);
                }
            }
        }
        return path;
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
                                           carrier, costDecider, null);
    }

    /**
     * Finds a quickest path to a neighbouring tile to a specified target
     * tile, optionally using a carrier and special purpose cost decider.
     *
     * @param start The <code>Location</code> to start at.
     * @param end The <code>Tile</code> to end at a neighbour of.
     * @param carrier An optional carrier <code>Unit</code> to carry the unit.
     * @param costDecider An optional <code>CostDecider</code> for
     *     determining the movement costs (uses default cost deciders
     *     for the unit/s if not provided).
     * @return A <code>PathNode</code>, or null if no path is found.
     */
    public PathNode findPathToNeighbour(Location start, Tile end, Unit carrier,
                                        CostDecider costDecider) {
        final Player owner = getOwner();
        int bestValue = INFINITY;
        PathNode best = null;
        for (Tile t : end.getSurroundingTiles(1)) {
            if (isTileAccessible(t)
                && (t.getFirstUnit() == null || owner.owns(t.getFirstUnit()))) {
                PathNode p = findPath(start, t, carrier, costDecider);
                if (p != null && bestValue > p.getTotalTurns()) {
                    bestValue = p.getTotalTurns();
                    best = p;
                }
            }
        }
        return best;
    }

    /**
     * Gets the number of turns required for this unit to reach a
     * destination location from its current position.  If the unit is
     * currently on a carrier, it will be used.
     *
     * @param end The destination <code>Location</code>.
     * @return The number of turns it will take to reach the destination,
     *         or <code>MANY_TURNS</code> if no path can be found.
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
     *         or <code>MANY_TURNS</code> if no path can be found.
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
     *         or <code>MANY_TURNS</code> if no path can be found.
     */
    public int getTurnsToReach(Location start, Location end, Unit carrier,
                               CostDecider costDecider) {
        PathNode path = findPath(start, end, carrier, costDecider);
        return (path == null) ? MANY_TURNS : path.getTotalTurns();
    }

    /**
     * Get the colony that can be reached by this unit in the least number
     * of turns.
     *
     * @param colonies A list of <code>Colony</code>s.
     * @return The nearest <code>Colony</code>, or null if none found.
     */
    public Colony getClosestColony(List<Colony> colonies) {
        return getClosestColony(colonies.stream());
    }
    
    /**
     * Get the colony that can be reached by this unit in the least number
     * of turns.
     *
     * @param colonies A stream of <code>Colony</code>s.
     * @return The nearest <code>Colony</code>, or null if none found.
     */
    public Colony getClosestColony(Stream<Colony> colonies) {
        ToIntFunction<Colony> closeness = c -> (c == null) ? MANY_TURNS-1
            : this.getTurnsToReach(c);
        return Stream.concat(Stream.of((Colony)null), colonies)
            .collect(Collectors.minBy(Comparator.comparingInt(closeness)))
            .orElse(null);
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
        if (player.getNumberOfSettlements() <= 0 || !hasTile()) return null;
        return findOurNearestSettlement(getTile(), excludeStart,
                                        range, coastal);
    }

    /**
     * Find a path for this unit to the nearest settlement with the
     * same owner that is reachable without a carrier.
     *
     * @param startTile The <code>Tile</code> to start searching from.
     * @param excludeStart If true, ignore any settlement the unit is
     *     currently in.
     * @param range An upper bound on the number of moves.
     * @param coastal If true, the settlement must have a path to Europe.
     * @return The nearest matching settlement if any, otherwise null.
     */
    public PathNode findOurNearestSettlement(final Tile startTile,
                                             final boolean excludeStart,
                                             int range, final boolean coastal) {
        final Player player = getOwner();
        if (startTile == null
            || player.getNumberOfSettlements() <= 0) return null;
        final GoalDecider gd = new GoalDecider() {

                private int bestValue = Integer.MAX_VALUE;
                private PathNode best = null;

                @Override
                public PathNode getGoal() { return best; }
                @Override
                public boolean hasSubGoals() { return true; }
                @Override
                public boolean check(Unit u, PathNode path) {
                    Tile t = path.getTile();
                    if (t == null
                        || (t == startTile && excludeStart)) return false;
                    Settlement settlement = t.getSettlement();
                    int value;
                    if (settlement != null
                        && player.owns(settlement)
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
     * Find a path to a settlement nearer to a destination.
     *
     * Used to find where to deliver goods to/from inland colonies,
     * or when blocked.
     *
     * @param dst The destination <code>Location</code>.
     * @return A path to the port, or null if none found.
     */
    public PathNode findIntermediatePort(Location dst) {
        final Settlement ignoreSrc = getSettlement();
        final Settlement ignoreDst = dst.getSettlement();
        final Tile srcTile = getTile();
        final Tile dstTile = dst.getTile();
        final int dstCont = (dstTile == null) ? -1 : dstTile.getContiguity();
        PathNode path, best = null;
        int value, bestValue = INFINITY;
        int type;

        if (isNaval()) {
            if (!srcTile.isHighSeasConnected()) {
                // On a lake!  FIXME: do better
                type = 0;
            } else if (dstTile == null) {
                // Carrier must be blocked from high seas
                type = 1;
            } else if (dstTile.isHighSeasConnected()) {
                // Carrier is blocked or destination is blocked.
                type = (getTile().isOnRiver()) ? 1 : 2;
            } else {
                // Destination must be blocked
                type = 2;
            }
        } else {
            if (dstTile == null || getTile().getContiguity() != dstCont) {
                // Ocean travel will be required
                // If already at port try to improve its connectivity,
                // otherwise go to a port.
                type = (srcTile.isHighSeasConnected()) ? 1 : 2;
            } else {
                // Pure land travel, just find a nearer settlement.
                type = 3;
            }
        }

        switch (type) {
        case 0:
            // No progress possible.
            break;
        case 1:
            // Starting on a river, probably blocked in there.
            // Find the settlement that most reduces the high seas count.
            best = search(getLocation(),
                          GoalDeciders.getReduceHighSeasCountGoalDecider(this),
                          null, INFINITY, null);
            break;
        case 2:
            // Ocean travel required, destination blocked.
            // Find the closest available connected port.
            for (Settlement s : getOwner().getSettlements()) {
                if (s != ignoreSrc && s != ignoreDst && s.isConnectedPort()
                    && (path = findPath(s)) != null) {
                    value = path.getTotalTurns()
                        + dstTile.getDistanceTo(s.getTile());
                    if (bestValue > value) {
                        bestValue = value;
                        best = path;
                    }
                }
            }
            break;
        case 3:
            // Land travel.  Find nearby settlement with correct contiguity.
            for (Settlement s : getOwner().getSettlements()) {
                if (s != ignoreSrc && s != ignoreDst
                    && s.getTile().getContiguity() == dstCont
                    && (path = findPath(s)) != null) {
                    value = path.getTotalTurns()
                        + dstTile.getDistanceTo(s.getTile());
                    if (bestValue > value) {
                        bestValue = value;
                        best = path;
                    }
                }
            }
        }
        return (best != null) ? best
            : findOurNearestSettlement(false, INFINITY, false);
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
        return (start == null) ? null
            : getGame().getMap().search(this, start, gd, cd, maxTurns,
                                        carrier, null);
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
        if (!isOffensiveUnit() || defender == null
            || !defender.hasTile()) return false;

        Tile tile = defender.getTile();
        return (isNaval())
            ? !tile.hasSettlement() && defender.isNaval()
            : !defender.isNaval() || defender.isBeached();
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

                @Override
                public PathNode getGoal() { return found; }
                @Override
                public boolean hasSubGoals() { return false; }
                @Override
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
     * Gets the line of sight of this <code>Unit</code>.  That is the
     * distance this <code>Unit</code> can sight new tiles.
     *
     * @return The line of sight of this <code>Unit</code>.
     */
    public int getLineOfSight() {
        final Turn turn = getGame().getTurn();
        Set<Modifier> result = new HashSet<>();
        result.addAll(this.getModifiers(Modifier.LINE_OF_SIGHT_BONUS,
                                        unitType, turn));
        if (hasTile() && getTile().isExplored()) {
            result.addAll(getTile().getType()
                .getModifiers(Modifier.LINE_OF_SIGHT_BONUS, unitType, turn));
        }
        float base = unitType.getLineOfSight();
        return (int)applyModifiers(base, turn, result);
    }


    // Goods handling

    /**
     * Get the goods carried by this unit.
     *
     * @return A list of <code>Goods</code>.
     */
    public List<Goods> getGoodsList() {
        return (getGoodsContainer() == null) ? Collections.<Goods>emptyList()
            : getGoodsContainer().getGoods();
    }

    /**
     * Get a compact version of the goods carried by this unit.
     *
     * @return A compact list of <code>Goods</code>.
     */
    public List<Goods> getCompactGoodsList() {
        return (getGoodsContainer() == null) ? Collections.<Goods>emptyList()
            : getGoodsContainer().getCompactGoods();
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
        return (canCarryUnits())
            ? getUnitList().stream().mapToInt(u -> u.getSpaceTaken()).sum()
            : 0;
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
     * Get a label for the current unit occupation.
     *
     * @param player The <code>Player</code> viewing the unit, as the
     *     owner has access to more information.
     * @param full If true, return a more detailed result.
     * @return A <code>StringTemplate</code> for the unit occupation.
     */
    public StringTemplate getOccupationLabel(Player player, boolean full) {
        final TradeRoute tradeRoute = getTradeRoute();
        StringTemplate ret;
        if (player != null && player.owns(this)) {
            if (isDamaged()) {
                if (full) {
                    ret = StringTemplate.label(":")
                        .add("model.unit.occupation.underRepair")
                        .addName(String.valueOf(getTurnsForRepair()));
                } else {
                    ret = StringTemplate.key("model.unit.occupation.underRepair");
                }
            } else if (tradeRoute != null) {
                if (full) {
                    ret = StringTemplate.label(":")
                        .add("model.unit.occupation.inTradeRoute")
                        .addName(tradeRoute.getName());
                } else {
                    ret = StringTemplate.key("model.unit.occupation.inTradeRoute");
                }
            } else if (getState() == UnitState.ACTIVE && getMovesLeft() == 0) {
                ret = StringTemplate.key("model.unit.occupation.activeNoMovesLeft");
            } else if (getState() == UnitState.IMPROVING
                && getWorkImprovement() != null) {
                if (full) {
                    ret = StringTemplate.label(":")
                        .add(getWorkImprovement().getType() + ".occupationString")
                        .addName(String.valueOf(getWorkTurnsLeft()));
                } else {
                    ret = StringTemplate.key(getWorkImprovement().getType() + ".occupationString");
                }
            } else if (getDestination() != null) {
                ret = StringTemplate.key("model.unit.occupation.goingSomewhere");
            } else {
                ret = StringTemplate.key("model.unit." + getState().getKey());
            }
        } else {
            if (isNaval()) {
                ret = StringTemplate.name(String.valueOf(getVisibleGoodsCount()));
            } else {
                ret = StringTemplate.key("model.unit.occupation.activeNoMovesLeft");
            }
        }
        return ret;
    }

    /**
     * Gets the probability that an attack by this unit will provoke a
     * native to convert.
     *
     * @return A probability of conversion.
     */
    public float getConvertProbability() {
        final Specification spec = getSpecification();
        int opt = spec.getInteger(GameOptions.NATIVE_CONVERT_PROBABILITY);
        return 0.01f * applyModifiers(opt, getGame().getTurn(),
                                      Modifier.NATIVE_CONVERT_BONUS);
    }

    /**
     * Gets the probability that an attack by this unit will provoke natives
     * to burn our missions.
     *
     * FIXME: enhance burn probability proportionally with tension
     *
     * @return A probability of burning missions.
     */
    public float getBurnProbability() {
        final Specification spec = getSpecification();
        return 0.01f * spec.getInteger(GameOptions.BURN_PROBABILITY);
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
            // unless the player has a suitable carrier and no free transport.
            return loc.getColony().isConnectedPort()
                && (getOwner().getCarriersForUnit(this).isEmpty()
                    || getTransportFee() == 0);
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
            float fee = (getSpecification()
                .getInteger(GameOptions.TREASURE_TRANSPORT_FEE)
                * getTreasureAmount()) / 100.0f;
            return (int)getOwner().applyModifiers(fee, getGame().getTurn(),
                Modifier.TREASURE_TRANSPORT_FEE, unitType);
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
     * Gets the appropriate trade bonuses due to a missionary unit.
     *
     * @param sense The sense to apply the modifiers.
     * @return The missionary trade bonuses.
     */
    public Set<Modifier> getMissionaryTradeModifiers(boolean sense) {
        HashSet<Modifier> result = new HashSet<>();
        for (Modifier m : getModifiers(Modifier.MISSIONARY_TRADE_BONUS)) {
            Modifier modifier = new Modifier(m);
            if (!sense) modifier.setValue(-m.getValue());
            result.add(modifier);
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

    /**
     * Gets the <code>ProductionInfo</code> for this unit.
     *
     * FIXME: the input parameter is ignored! Fix?
     *
     * @param input A list of input <code>AbstractGoods</code>.
     * @return The <code>ProductionInfo</code> for this unit.
     */
    public ProductionInfo getProductionInfo(List<AbstractGoods> input) {
        ProductionInfo result = new ProductionInfo();
        result.setConsumption(getType().getConsumedGoods());
        result.setMaximumConsumption(getType().getConsumedGoods());
        return result;
    }

    /**
     * Score this unit with its suitability for pioneering.
     *
     * A pioneer must be a colonst.  Favour:
     * - existing pioneers especially if on the map
     * - expert pioneer units
     * - then by skill but not other experts
     *
     * @return A pioneering score.
     */
    public int getPioneerScore() {
        int ht = (hasTile()) ? 100 : 0;
        return (getLocation() == null || !isColonist()) ? -1000
            : (hasAbility(Ability.IMPROVE_TERRAIN)) ? 900 + ht
            : (hasAbility(Ability.EXPERT_PIONEER)) ? 700
            : (!hasDefaultRole()) ? 0
            : (getSkillLevel() > 0) ? 0
            : 200 + getSkillLevel() * 50;
    }

    /**
     * Score this unit with its suitability for scouting.
     *
     * A scout must be a colonist.  Favour:
     * - existing scouts especially if on the map
     * - expert scouts
     * - lower skill level as scouting is a good career for crims and servants
     *   which might become seasoned scouts
     *
     * @return A scouting score.
     */
    public int getScoutScore() {
        int ht = (hasTile()) ? 100 : 0;
        return (getLocation() == null || !isColonist()) ? -1000
            : (hasAbility(Ability.SPEAK_WITH_CHIEF)) ? 900 + ht
            : (hasAbility(Ability.EXPERT_SCOUT)) ? 700
            : (!hasDefaultRole()) ? 0
            : (getSkillLevel() <= 0) ? -200 * getSkillLevel()
            : 0;
    }

    /**
     * Evaluate this unit for trade purposes.
     *
     * @param player The <code>Player</code> to evaluate for.
     * @return A value of this unit.
     */
    public int evaluateFor(Player player) {
        final Europe europe = player.getEurope();
        if (player.isAI() && player.getUnits().size() < 10) {
            return Integer.MIN_VALUE;
        }
        return (europe == null) ? 500 : europe.getUnitPrice(getType());
    }

    // @compat 0.11.0
    /**
     * Get modifiers required for combat.
     *
     * This can be replaced with just getModifiers() when accepted
     * specifications have all combat modifiers with correct index
     * values.
     */
    public Set<Modifier> getCombatModifiers(String id,
        FreeColGameObjectType fcgot, Turn turn) {
        final Player owner = getOwner();
        final UnitType unitType = getType();
        Set<Modifier> result = new HashSet<>();

        // UnitType modifiers always apply
        for (Modifier m : unitType.getModifiers(id, fcgot, turn)) {
            switch (m.getType()) {
            case ADDITIVE:
                m.setModifierIndex(Modifier.UNIT_ADDITIVE_COMBAT_INDEX);
                break;
            default:
                m.setModifierIndex(Modifier.UNIT_NORMAL_COMBAT_INDEX);
                break;
            }
            result.add(m);
        }

        // The player's modifiers may not all apply
        for (Modifier m : owner.getModifiers(id, fcgot, turn)) {
            m.setModifierIndex(Modifier.GENERAL_COMBAT_INDEX);
            result.add(m);
        }
        
        // Role modifiers apply
        for (Modifier m : role.getModifiers(id, fcgot, turn)) {
            m.setModifierIndex(Modifier.ROLE_COMBAT_INDEX);
            result.add(m);
        }

        return result;
    }
    // end @compat 0.11.0


    // Message unpacking support.

    /**
     * Gets the tile in a given direction.
     *
     * @param directionString The direction.
     * @return The <code>Tile</code> in the given direction.
     * @throws IllegalStateException if there is trouble.
     */
    public Tile getNeighbourTile(String directionString) {
        if (!hasTile()) {
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

        if (!hasTile()) {
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
    @Override
    public List<AbstractGoods> getConsumedGoods() {
        return unitType.getConsumedGoods();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPriority() {
        return unitType.getPriority();
    }


    // Interface Ownable

    /**
     * {@inheritDoc}
     */
    @Override
    public Player getOwner() {
        return owner;
    }

    /**
     * {@inheritDoc}
     *
     * -vis: This routine has visibility implications.
     */
    @Override
    public void setOwner(Player player) {
        this.owner = player;
    }


    // Interface Locatable
    //   getTile and getSpaceTaken are shared with Location below

    /**
     * Gets the location of this unit.
     *
     * @return The location of this <code>Unit</code>.
     */
    @Override
    public Location getLocation() {
        return location;
    }

    /**
     * Sets the location of this unit.
     *
     * -vis: This routine changes player visibility.
     * -til: While units do not contribute to tile appearance as such, if
     *     they move in/out of a colony the visible colony size changes.
     *
     * @param newLocation The new <code>Location</code>.
     * @return True if the location change succeeds.
     */
    @Override
    public boolean setLocation(Location newLocation) {
        // It is possible to add a unit to a non-specific location
        // within a colony by specifying the colony as the new
        // location.  Colony.joinColony handles this special case.
        if (newLocation instanceof Colony) {
            return ((Colony)newLocation).joinColony(this);
        }

        if (newLocation == location) return true;
        if (newLocation != null && !newLocation.canAdd(this)) {
            logger.warning("Can not add " + this + " to "
                + newLocation.getId());
            return false;
        }

        // If the unit either starts or ends this move in a colony
        // then teaching status can change.  However, if it moves
        // between locations within the same colony with the same
        // teaching ability, the teaching state should *not* change.
        // We have to handle this issue here in setLocation as this is
        // the only place that contains information about both
        // locations.
        Colony oldColony = (isInColony()) ? location.getColony() : null;
        Colony newColony = (newLocation instanceof WorkLocation)
            ? newLocation.getColony() : null;
        boolean withinColony = newColony != null && newColony == oldColony;
        boolean preserveEducation = withinColony
            && (((WorkLocation)location).canTeach()
                == ((WorkLocation)newLocation).canTeach());

        // First disable education that will fail due to the move.
        if (oldColony != null && !preserveEducation) {
            oldColony.updateEducation(this, false);
        }

        // Move out of the old location.
        if (location == null) {
            ; // do nothing
        } else if (!location.remove(this)) {//-vis
            // "Should not happen" (should always be able to remove)
            throw new RuntimeException("Failed to remove " + this
                + " from " + location.getId());
        }

        // Move in to the new location.
        if (newLocation == null) {
            setLocationNoUpdate(null);//-vis
        } else if (!newLocation.add(this)) {//-vis
            // "Should not happen" (canAdd was checked above)
            throw new RuntimeException("Failed to add "
                + this + " to " + newLocation.getId());
        }

        // See if education needs to be re-enabled.
        if (newColony != null && !preserveEducation) {
            newColony.updateEducation(this, true);
        }

        // Update population of any colonies involved.
        if (!withinColony) {
            if (oldColony != null) oldColony.updatePopulation();
            if (newColony != null) newColony.updatePopulation();
        }
        return true;
    }

    /**
     * Checks if this <code>Unit</code> is located in Europe.  That
     * is; either directly or onboard a carrier which is in Europe.
     *
     * @return True if in <code>Europe</code>.
     */
    @Override
    public boolean isInEurope() {
        return (location instanceof Unit) ? ((Unit)location).isInEurope()
            : getLocation() instanceof Europe;
    }


    // Interface Location (from GoodsLocation via UnitLocation)
    // Inherits
    //   FreeColObject.getId
    //   UnitLocation.getLocationLabelFor
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
    public StringTemplate getLocationLabel() {
        return StringTemplate.template("model.unit.onBoard")
            .addStringTemplate("%unit%", this.getLabel());
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
                // FIXME: there seems to be an inconsistency between
                // units moving from an adjacent tile onto a ship and
                // units boarding a ship in-colony.  The former does not
                // appear to come through here (which it probably should)
                // as the ship's moves do not get zeroed.
                spendAllMoves();
                unit.setState(UnitState.SENTRY);
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
                + locatable);
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
            if (super.remove(locatable)) {
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
                + locatable);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public Location up() {
        return (isInEurope()) ? getLocation().up()
            : (isInColony()) ? getColony()
            : (hasTile()) ? getTile().up()
            : this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRank() {
        return Location.getRank(getLocation());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toShortString() {
        StringBuilder sb = new StringBuilder(32);
        sb.append(getId()).append("-").append(getType().getSuffix());
        if (!hasDefaultRole()) {
            sb.append("-").append(getRoleSuffix());
            int count = getRoleCount();
            if (count > 1) sb.append(".").append(count);
        }
        return sb.toString();
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
                : (locatable.getSpaceTaken() > getSpaceLeft())
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
     *
     * -vis: This routine can change player visibility.
     */
    @Override
    public void disposeResources() {
        if (location != null) {
            location.remove(this);
            // Do not set location to null, units that are slaughtered in
            // battle need to remain valid during the animation.
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

        getOwner().removeUnit(this);

        super.disposeResources();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColGameObject getLinkTarget(Player player) {
        return (hasTile()) ? (FreeColGameObject)getTile().up()
            : player.getEurope();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int checkIntegrity(boolean fix) {
        int result = super.checkIntegrity(fix);
        if (this.role == null) {
            if (fix) {
                this.role = getSpecification().getDefaultRole();
                logger.warning("Fixed missing role for: " + getId());
                result = 0;
            } else {
                logger.warning("Missing role for: " + getId());
                result = -1;
            }
        }
        if (this.destination != null) {
            if (((FreeColGameObject)this.destination).isUninitialized()) {
                if (fix) {
                    this.destination = null;
                    logger.warning("Cleared uninitialized destination for: "
                        + getId());
                    result = Math.min(result, 0);
                } else {
                    logger.warning("Uninitialized destination for: "
                        + getId());
                    result = -1;
                }
            }
        }
        return result;
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Ability> getAbilities(String id, FreeColGameObjectType fcgot,
                                     Turn turn) {
        final Player owner = getOwner();
        final UnitType unitType = getType();
        Set<Ability> result = new HashSet<>();

        // UnitType abilities always apply.
        result.addAll(unitType.getAbilities(id));

        // Roles apply with qualification.
        result.addAll(role.getAbilities(id, fcgot, turn));

        // The player's abilities require more qualification.
        result.addAll(owner.getAbilities(id, fcgot, turn));

        // Location abilities may apply.
        // FIXME: extend this to all locations?  May simplify
        // code.  Units are also Locations however, which complicates
        // the issue as we do not want Units aboard other Units to share
        // the abilities of the carriers.
        if (getSettlement() != null) {
            result.addAll(getSettlement().getAbilities(id, unitType, turn));
        } else if (isInEurope()) {
            // @compat 0.10.x
            // It makes sense here to do:
            //   Europe europe = owner.getEurope();
            // However while there is fixup code in readChildren that calls
            // this routine we can not rely on owner.europe being initialized
            // yet.  Hence the following:
            Location loc = getLocation();
            Europe europe = (loc instanceof Europe) ? (Europe)loc
                : (loc instanceof Unit) ? (Europe)((Unit)loc).getLocation()
                : null;
            // end @compat 0.10.x
            result.addAll(europe.getAbilities(id, unitType, turn));
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Modifier> getModifiers(String id, FreeColGameObjectType fcgot,
                                      Turn turn) {
        final Player owner = getOwner();
        final UnitType unitType = getType();
        Set<Modifier> result = new HashSet<>();

        // UnitType modifiers always apply
        result.addAll(unitType.getModifiers(id, fcgot, turn));

        // The player's modifiers may not all apply
        result.addAll(owner.getModifiers(id, fcgot, turn));
        
        // Role modifiers apply
        result.addAll(role.getModifiers(id, fcgot, turn));

        return result;
    }


    // Serialization

    private static final String ATTRITION_TAG = "attrition";
    private static final String COUNT_TAG = "count";
    private static final String CURRENT_STOP_TAG = "currentStop";
    private static final String DESTINATION_TAG = "destination";
    private static final String ENTRY_LOCATION_TAG = "entryLocation";
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
    private static final String ROLE_COUNT_TAG = "roleCount";
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
    private static final String EQUIPMENT_TAG = "equipment";
    /** The equipment this Unit carries.  Now subsumed into roles. */
    private final TypeCountMap<EquipmentType> equipment
        = new TypeCountMap<>();
    // end @compat 0.10.x
    // @compat 0.11.3
    private static final String OLD_TILE_IMPROVEMENT_TAG = "tileimprovement";
    // end @compat 0.11.3


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        boolean full = xw.validFor(getOwner());

        if (name != null) xw.writeAttribute(NAME_TAG, name);

        xw.writeAttribute(UNIT_TYPE_TAG, unitType);

        xw.writeAttribute(MOVES_LEFT_TAG, movesLeft);

        xw.writeAttribute(STATE_TAG, state);

        xw.writeAttribute(ROLE_TAG, role);

        xw.writeAttribute(ROLE_COUNT_TAG, roleCount);

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

        if (location != null) {
            if (!full && isInColony()) {
                // Really special case.  This happens in attack
                // animations when a defender unit is invisible
                // working inside a colony and has to be specially
                // serialized to the client.
                xw.writeLocationAttribute(LOCATION_TAG, getColony());

            } else {
                xw.writeLocationAttribute(LOCATION_TAG, location);
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
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        if (xw.validFor(getOwner())) {

            // Do not show goods or units carried by this unit.
            super.writeChildren(xw);

            if (workImprovement != null) workImprovement.toXML(xw);

        } else if (getType().canCarryGoods()) {
            xw.writeAttribute(VISIBLE_GOODS_COUNT_TAG, getVisibleGoodsCount());
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

        Player oldOwner = owner;
        owner = xr.findFreeColGameObject(game, OWNER_TAG,
                                         Player.class, (Player)null, true);
        if (xr.shouldIntern()) game.checkOwners(this, oldOwner);

        UnitType oldUnitType = unitType;
        unitType = xr.getType(spec, UNIT_TYPE_TAG,
                              UnitType.class, (UnitType)null);

        state = xr.getAttribute(STATE_TAG, UnitState.class, UnitState.ACTIVE);

        role = xr.getRole(spec, ROLE_TAG, Role.class,
                          spec.getDefaultRole());
        // @compat 0.10.x
        // Fix roles
        if (owner.isIndian()) {
            if ("model.role.scout".equals(role.getId())) {
                role = spec.getRole("model.role.mountedBrave");
            } else if ("model.role.soldier".equals(role.getId())) {
                role = spec.getRole("model.role.armedBrave");
            } else if ("model.role.dragoon".equals(role.getId())) {
                role = spec.getRole("model.role.nativeDragoon");
            }
        } else if (owner.isREF()) {
            if ("model.role.soldier".equals(role.getId())
                && unitType.hasAbility(Ability.REF_UNIT)) {
                role = spec.getRole("model.role.infantry");
            } else if ("model.role.dragoon".equals(role.getId())
                && unitType.hasAbility(Ability.REF_UNIT)) {
                role = spec.getRole("model.role.cavalry");
            } else if ("model.role.infantry".equals(role.getId())
                && !unitType.hasAbility(Ability.REF_UNIT)) {
                role = spec.getRole("model.role.soldier");
            } else if ("model.role.cavalry".equals(role.getId())
                && !unitType.hasAbility(Ability.REF_UNIT)) {
                role = spec.getRole("model.role.dragoon");
            }
        } else {
            if ("model.role.infantry".equals(role.getId())) {
                role = spec.getRole("model.role.soldier");
            } else if ("model.role.cavalry".equals(role.getId())) {
                role = spec.getRole("model.role.dragoon");
            }
        }            
        // end @compat 0.10.x

        roleCount = xr.getAttribute(ROLE_COUNT_TAG,
            // @compat 0.10.x
            -1
            // Should be role.getMaximumCount()
            // end @compat 0.10.x
            );

        location = xr.getLocationAttribute(game, LOCATION_TAG, true);

        entryLocation = xr.getLocationAttribute(game, ENTRY_LOCATION_TAG,
                                                true);

        movesLeft = xr.getAttribute(MOVES_LEFT_TAG, 0);

        workLeft = xr.getAttribute(WORK_LEFT_TAG, 0);

        attrition = xr.getAttribute(ATTRITION_TAG, 0);

        nationality = xr.getAttribute(NATIONALITY_TAG, (String)null);

        ethnicity = xr.getAttribute(ETHNICITY_TAG, (String)null);

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

        destination = xr.getLocationAttribute(game, DESTINATION_TAG, true);

        tradeRoute = xr.findFreeColGameObject(game, TRADE_ROUTE_TAG,
            TradeRoute.class, (TradeRoute)null, false);

        currentStop = (tradeRoute == null) ? -1
            : xr.getAttribute(CURRENT_STOP_TAG, 0);

        experienceType = xr.getType(spec, EXPERIENCE_TYPE_TAG,
                                    GoodsType.class, (GoodsType)null);
        if (experienceType == null && workType != null) {
            experienceType = workType;
        }

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
        if (getGoodsContainer() != null) getGoodsContainer().removeAll();
        equipment.clear();
        workImprovement = null;

        super.readChildren(xr);

        // @compat 0.10.x
        if (roleCount < 0) {
            // If roleCount was not present, set it from equipment
            final Specification spec = getSpecification();
            Role role = spec.getDefaultRole();
            boolean horses = false, muskets = false;
            int count = 1;
            for (EquipmentType type : equipment.keySet()) {
                if ("model.equipment.horses".equals(type.getId())
                    || "model.equipment.indian.horses".equals(type.getId())) {
                    horses = true;
                } else if ("model.equipment.muskets".equals(type.getId())
                    || "model.equipment.indian.muskets".equals(type.getId())) {
                    muskets = true;
                } else {
                    role = type.getRole();
                    if ("model.equipment.tools".equals(type.getId())) {
                        count = equipment.getCount(type);
                    }
                }
            }
            if (horses && muskets) {
                if (owner.isIndian()) {
                    role = spec.getRole("model.role.nativeDragoon");
                } else if (owner.isREF() && hasAbility(Ability.REF_UNIT)) {
                    role = spec.getRole("model.role.cavalry");
                } else {
                    role = spec.getRole("model.role.dragoon");
                }
            } else if (horses) {
                if (owner.isIndian()) {
                    role = spec.getRole("model.role.mountedBrave");
                } else if (owner.isREF() && hasAbility(Ability.REF_UNIT)) {
                    logger.warning("Undefined role: REF Scout");
                } else {
                    role = spec.getRole("model.role.scout");
                }
            } else if (muskets) {
                if (owner.isIndian()) {
                    role = spec.getRole("model.role.armedBrave");
                } else if (owner.isREF() && hasAbility(Ability.REF_UNIT)) {
                    role = spec.getRole("model.role.infantry");
                } else {
                    role = spec.getRole("model.role.soldier");
                }
            }
            setRoleCount(Math.min(role.getMaximumCount(), count));
        } else {
            // If roleCount was present, we are now ignoring equipment.
            equipment.clear();
        }
        // end @compat 0.10.x

        // @compat 0.10.x
        // There was a bug in 0.10.x that did not clear tile
        // improvements after they were complete, leading to units
        // that still had a tile improvement after they had moved
        // away.  Consequently when reading such bogus improvements,
        // there is no guarantee that the tile is defined so
        // compatibility code in TileImprovement.readAttributes
        // tolerates null tile references.  These are obviously bogus,
        // so drop them.
        if (workImprovement != null && workImprovement.getTile() == null) {
            workImprovement = null;
        }
        // end @compat 0.10.x
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final Specification spec = getSpecification();
        final Game game = getGame();
        final String tag = xr.getLocalName();

        // @compat 0.10.x
        if (EQUIPMENT_TAG.equals(tag)) {
            equipment.incrementCount(spec.getEquipmentType(xr.readId()),
                                     xr.getAttribute(COUNT_TAG, 0));
            xr.closeTag(EQUIPMENT_TAG);
        // end @compat 0.10.x

        // @compat 0.10.5
        } else if (OLD_UNITS_TAG.equals(tag)) {
            while (xr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                super.readChild(xr);
            }
        // end @compat 0.10.5

        } else if (TileImprovement.getXMLElementTagName().equals(tag)
                   // @compat 0.11.3
                   || OLD_TILE_IMPROVEMENT_TAG.equals(tag)
                   // end @compat 0.11.3
                   ) {
            workImprovement = xr.readFreeColGameObject(game,
                                                       TileImprovement.class);

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
            sb.append(" ").append(lastPart(owner.getNationId(), "."))
                .append(" ").append(getType().getSuffix());
            if (!hasDefaultRole()) {
                sb.append("-").append(getRoleSuffix());
                int count = getRoleCount();
                if (count > 1) sb.append(".").append(count);
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
    @Override
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
