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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.CombatModel;
import static net.sf.freecol.common.model.Constants.*;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.pathfinding.CostDecider;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.common.model.pathfinding.GoalDeciders;
import net.sf.freecol.common.model.UnitTypeChange;
import net.sf.freecol.common.option.GameOptions;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.LogBuilder;
import static net.sf.freecol.common.util.StringUtils.*;


/**
 * Represents all pieces that can be moved on the map-board. This includes:
 * colonists, ships, wagon trains e.t.c.
 *
 * Every {@code Unit} is owned by a {@link Player} and has a
 * {@link Location}.
 */
public class Unit extends GoodsLocation
    implements Consumer, Locatable, Movable, Nameable, Ownable {

    private static final Logger logger = Logger.getLogger(Unit.class.getName());

    private static class ClosestSettlementGoalDecider implements GoalDecider {

        /** A tile to exclude. */
        private Tile exclude;

        /** Require a connected port. */
        private boolean coastal;

        /** Best value so far. */
        private int bestValue;

        /** Best path so far. */
        private PathNode best;


        /**
         * Build a new goal decider to find the closest path to a settlement
         * owned by the player controlling the searching unit.
         *
         * @param exclude An optional tile to exclude.
         * @param coastal If true, a connected port is required.
         */
        public ClosestSettlementGoalDecider(Tile exclude, boolean coastal) {
            this.exclude = exclude;
            this.coastal = coastal;
            this.bestValue = Integer.MAX_VALUE;
            this.best = null;
        }

        // Implement GoalDecider

        /**
         * {@inheritDoc}
         */
        public PathNode getGoal() {
            return this.best;
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasSubGoals() {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        public boolean check(Unit u, PathNode path) {
            Tile t = path.getTile();
            if (t == null || t == this.exclude) return false;
            Settlement settlement = t.getSettlement();
            int value;
            if (settlement != null
                && u.getOwner().owns(settlement)
                && (!this.coastal || settlement.isConnectedPort())
                && (value = path.getTotalTurns()) < bestValue) {
                bestValue = value;
                best = path;
                return true;
            }
            return false;
        }
    };

    /** Class index for units. */
    private static final int UNIT_CLASS_INDEX = 40;

    public static final String TAG = "unit";

    /**
     * A large number of turns, denoting pathfinding failure.  Do not use
     * INFINITY as further calculation might use this.
     */
    public static final int MANY_TURNS = 10000;

    public static final String CARGO_CHANGE = "CARGO_CHANGE";
    public static final String MOVE_CHANGE = "MOVE_CHANGE";
    public static final String ROLE_CHANGE = "ROLE_CHANGE";

    /** Compare units by location. */
    public static final Comparator<Unit> locComparator
        = Comparator.comparingInt(u -> Location.rankOf(u));

    /**
     * A comparator to compare units by type, then role index, then
     * the FCO order.
     */
    public static final Comparator<Unit> typeRoleComparator
        = Comparator.comparing(Unit::getType)
            .thenComparingInt(u -> u.getRole().getRoleIndex())
            .thenComparing(FreeColObject.fcoComparator);

    /** A comparator to compare units by increasing skill level. */
    public static final Comparator<Unit> increasingSkillComparator
        = Comparator.comparingInt(Unit::getSkillLevel);
    /** A comparator to compare units by decreasing skill level. */
    public static final Comparator<Unit> decreasingSkillComparator
        = increasingSkillComparator.reversed();

    /**
     * Comparator to rank settlements by accessibility by sea to Europe.
     */
    private static final Comparator<Settlement> settlementStartComparator
        = cachingIntComparator(s ->
            (s == null || !s.getTile().isHighSeasConnected()) ? INFINITY
                : s.getTile().getHighSeasCount());

    /** Useful predicate for finding sentried land units. */
    public static final Predicate<Unit> sentryPred = u ->
        !u.isNaval() && u.getState() == UnitState.SENTRY;

    /** A state a Unit can have. */
    public static enum UnitState {
        ACTIVE,
        FORTIFIED,
        SENTRY,
        IN_COLONY,
        IMPROVING,
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

    /** Internal state for findIntermediatePort. */
    private static enum PortMode {
        LAKE,
        NO_HIGH_SEAS,
        BLOCKED,
        LAND
    };


    /** The individual name of this unit, not of the unit type. */
    protected String name = null;

    /** The owner player. */
    protected Player owner;

    /** The unit type. */
    protected UnitType type;

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
     * @param game The enclosing {@code Game}.
     */
    protected Unit(Game game) {
        super(game);

        initialize();
    }

    /**
     * Creates a new {@code Unit} with the given
     * identifier.  The object should later be initialized by calling
     * {@link #readFromXML(FreeColXMLReader)}.
     *
     * @param game The enclosing {@code Game}.
     * @param id The object identifier.
     */
    public Unit(Game game, String id) {
        super(game, id);

        initialize();
    }


    /**
     * Initialize the nationality and ethnicity.
     */
    private final void initialize() {
        Player owner = getOwner();
        if (owner != null && isPerson()) {
            setNationality(owner.getNationId());
            setEthnicity(owner.getNationId());
        }
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
     * @return The name of the apparent owner of this {@code Unit}.
     */
    public StringTemplate getApparentOwnerName() {
        Player own = (isOwnerHidden()) ? getGame().getUnknownEnemy()
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
     * @return The {@code StringTemplate} to describe the given unit.
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
     * @return The {@code StringTemplate} to describe the given unit.
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
                    extra = AbstractGoods.getAbstractLabel(equipmentKey, 1);
                } else {
                    // Other roles can be characterized by their goods.
                    List<AbstractGoods> requiredGoods
                        = role.getRequiredGoodsList(getRoleCount());
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
     * @return A {@code String} describing this unit.
     */
    public String getDescription() {
        return Messages.message(getLabel());
    }

    /**
     * Get the basic i18n description for this unit.
     *
     * @param ult The label type required.
     * @return A {@code String} describing this unit.
     */
    public String getDescription(UnitLabelType ult) {
        return Messages.message(getLabel(ult));
    }

    /**
     * Get a label for the chance of success in a potential combat.
     *
     * @param tile The {@code Tile} to attack into.
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
     * @return A {@code StringTemplate} describing where this unit
     *     is going.
     */
    public StringTemplate getDestinationLabel() {
        // Create the right tag for the tagged "goingTo" message.
        String type = (isPerson()) ? "person"
            : (isNaval()) ? "ship"
            : "other";
        return getUnitDestinationLabel(type, getDestination(), getOwner());
    }

    /**
     * Get a destination label for a given unit tag, destination and player.
     *
     * @param tag The unit tag for the "goingTo" message.
     * @param destination The destination {@code Location}.
     * @param player The {@code Player} viewpoint.
     * @return A {@code StringTemplate} describing the unit movement.
     */
    public static StringTemplate getUnitDestinationLabel(String tag,
        Location destination, Player player) {
        return StringTemplate.template("model.unit.goingTo")
            .addTagged("%type%", tag)
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
     * Get the {@code UnitType} value.
     *
     * @return The current {@code UnitType}.
     */
    public final UnitType getType() {
        return this.type;
    }

    /**
     * Sets the type of the unit.
     *
     * -vis: Has visibility issues as the line of sight may change.
     *
     * @param type The new type of the unit.
     */
    public void setType(UnitType type) {
        this.type = type;
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
        this.hitPoints = unitType.getHitPoints();
        if (getTeacher() != null && !canBeStudent(getTeacher())) {
            getTeacher().setStudent(null);
            setTeacher(null);
        }
        return true;
    }

    /**
     * Score this unit.
     *
     * Just delegates to unit type for now.
     *
     * @return The score for this unit.
     */
    public int getScoreValue() {
        return (this.type == null) ? 0 : this.type.getScoreValue();
    }

    /**
     * Checks if this {@code Unit} is naval.
     *
     * @return True if this is a naval {@code Unit}.
     */
    public boolean isNaval() {
        return (this.type == null) ? false : this.type.isNaval();
    }

    /**
     * Is this a unit that hides its ownership?
     *
     * @return True if the owner should be hidden from clients.
     */
    public boolean isOwnerHidden() {
        return (this.type == null) ? false
            : this.type.hasAbility(Ability.PIRACY);
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
     * @return True if this {@code Unit} can carry treasure.
     */
    public boolean canCarryTreasure() {
        return hasAbility(Ability.CARRY_TREASURE);
    }

    /**
     * Can this unit capture enemy goods?
     *
     * @return True if this {@code Unit} is capable of capturing goods.
     */
    public boolean canCaptureGoods() {
        return hasAbility(Ability.CAPTURE_GOODS);
    }

    /**
     * Checks if this is a trading {@code Unit}, meaning that it
     * can trade with settlements.
     *
     * @return True if this is a trading unit.
     */
    public boolean isTradingUnit() {
        return canCarryGoods() && owner.isEuropean();
    }

    /**
     * Checks if this {@code Unit} is a `colonist'.  A unit is a
     * colonist if it is European and can build a new {@code Colony}.
     *
     * @return True if this unit is a colonist.
     */
    public boolean isColonist() {
        return this.type.hasAbility(Ability.FOUND_COLONY)
            && owner.hasAbility(Ability.FOUNDS_COLONIES);
    }

    /**
     * Checks if this {@code Unit} is able to carry {@link Locatable}s.
     *
     * @return True if this unit can carry goods or other units.
     */
    public boolean isCarrier() {
        return this.type.canCarryGoods() || this.type.canCarryUnits();
    }

    /**
     * Checks if this unit is a person, that is not a ship or wagon.
     *
     * @return True if this unit is a person.
     */
    public boolean isPerson() {
        return (this.type == null) ? false
            : this.type.hasAbility(Ability.PERSON);
    }

    /**
     * Gets the state of this {@code Unit}.
     *
     * @return The state of this {@code Unit}.
     */
    public UnitState getState() {
        return state;
    }

    /**
     * Checks if a {@code Unit} can get the given state set.
     *
     * @param s The new state for this Unit.  Should be one of
     *     {UnitState.ACTIVE, FORTIFIED, ...}.
     * @return True if the {@code Unit} state can be changed to
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
            return getMovesLeft() > 0 && isOnTile()
                && getOwner().canAcquireForImprovement(getLocation().getTile());
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
     * @param s The new {@code UnitState}.
     */
    protected void setStateUnchecked(UnitState s) {
        // FIXME: move to the server.
        // Cleanup the old UnitState, for example destroy the
        // TileImprovment being built by a pioneer.
        switch (state) {
        case IMPROVING:
            if (workImprovement != null && getWorkLeft() > 0) {
                // Remove the tile improvement if it is incomplete
                // and no one else is working on it
                Tile tile;
                if (!workImprovement.isComplete()
                    && (tile = workImprovement.getTile()) != null
                    && tile.getTileItemContainer() != null
                    && none(tile.getUnits(), u ->
                        u != this && u.getState() == UnitState.IMPROVING
                             && u.getWorkImprovement() == workImprovement)) {
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
        case ACTIVE: case FORTIFYING: case SENTRY:
            setWorkLeft(-1);
            break;
        case FORTIFIED:
            setWorkLeft(-1);
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
     * @param state The {@code UnitState} to set..
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
     * @param owner The new owner {@code Player}.
     */
    public void changeOwner(Player owner) {
        final Player oldOwner = this.owner;
        if (oldOwner == owner) return;

        if (oldOwner == null) {
            logger.warning("Unit " + getId()
                + " had no owner, when changing owner to " + owner.getId());
        }

        // Notify the AI before actually changing the owner so the AI
        // can find the owning AIPlayer
        getGame().notifyOwnerChanged(this, oldOwner, owner);

        // This need to be set right away.
        setOwner(owner);

        // If its a carrier, we need to update the units it has loaded
        for (Unit u : getUnitList()) u.changeOwner(owner);

        // Clear education, trade route/orders and home settlement
        if (getTeacher() != null && !canBeStudent(getTeacher())) {
            getTeacher().setStudent(null);
            setTeacher(null);
        }
        if (getTradeRoute() != null) setTradeRoute(null);
        if (getDestination() != null) setDestination(null);
        changeHomeIndianSettlement(null);

        // Update owner unit lists
        if (oldOwner != null) oldOwner.removeUnit(this);
        if (owner != null) owner.addUnit(this);
    }

    /**
     * Gets the unit role.
     *
     * @return The {@code Role} of this {@code Unit}.
     */
    public Role getRole() {
        return role;
    }

    /**
     * Sets the {@code Role} of this {@code Unit}.
     *
     * @param role The new {@code Role}.
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
     * @return True if the unit has the default {@code Role}.
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
        return Role.getRoleIdSuffix(role.getId());
    }

    /**
     * Change the current role of this unit.
     *
     * @param role The new {@code Role}.
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
     * @param role The {@code Role} to test.
     * @return True if the role is available to this unit.
     */
    public boolean roleIsAvailable(Role role) {
        return role.isAvailableTo(this);
    }

    /**
     * Filter a list of roles to return only those available to this unit.
     *
     * @param roles The list of {@code Role}s to filter, if null all
     *     available roles are used.
     * @return A list of available {@code Role}s.
     */
    public List<Role> getAvailableRolesList(List<Role> roles) {
        if (roles == null) roles = getSpecification().getRolesList();
        return transform(roles, r -> roleIsAvailable(r));
    }

    /**
     * Filter a list of roles to return only those available to this unit,
     * returning a stream.
     *
     * @param roles The list of {@code Role}s to filter, if null all
     *     available roles are used.
     * @return A stream of available {@code Role}s.
     */
    public Stream<Role> getAvailableRoles(List<Role> roles) {
        return getAvailableRolesList(roles).stream();
    }

    /**
     * Get a military role for this unit.
     *
     * @return A military {@code Role}, or null if none found.
     */
    public Role getMilitaryRole() {
        return first(transform(getSpecification().getMilitaryRoles(),
                               r -> roleIsAvailable(r)));
    }

    /**
     * Get the change in goods required to change to a new role/count.
     *
     * @param role The new {@code Role} to change to.
     * @param roleCount The new role count.
     * @return A list of {@code AbstractGoods} defining the change
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
     * @param newLocation The new {@code Location}.
     */
    public void setLocationNoUpdate(Location newLocation) {
        this.location = newLocation;
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
     * Is this unit on a tile?
     *
     * @return True if the unit is on a tile.
     */
    public boolean isOnTile() {
        return getLocation() instanceof Tile;
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
     * Checks whether this {@code Unit} is at sea off the map, or
     * on board of a carrier that is.
     *
     * @return True if at sea.
     */
    public boolean isAtSea() {
        return (isOnCarrier()) ? getCarrier().isAtSea()
            : getLocation() instanceof HighSeas;
    }

    /**
     * Checks if this unit is running a mission.
     *
     * @return True if this unit is running a mission.
     */
    public boolean isInMission() {
        return hasAbility(Ability.ESTABLISH_MISSION)
            && getLocation() instanceof IndianSettlement;
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
     * @return The current {@code WorkLocation}, or null if none.
     */
    public WorkLocation getWorkLocation() {
        return (isInColony()) ? (WorkLocation)getLocation() : null;
    }

    /**
     * Get the work tile this unit is working in, if any.
     *
     * @return The current work {@code Tile}, if any.
     */
    public Tile getWorkTile() {
        return (getLocation() instanceof WorkLocation)
            ? ((WorkLocation)getLocation()).getWorkTile()
            : null;
    }

    /**
     * Gets the entry location for this unit to use when returning from
     * {@link Europe}.
     *
     * @return The entry {@code Location}.
     */
    public Location getEntryLocation() {
        return this.entryLocation;
    }

    /**
     * Sets the entry location in which this unit will be put when
     * returning from {@link Europe}.
     *
     * @param entryLocation The new entry {@code Location}.
     * @see #getEntryLocation
     */
    public void setEntryLocation(Location entryLocation) {
        this.entryLocation = entryLocation;
        if (this.entryLocation != null) {
            Tile tile = this.entryLocation.getTile();
            if (tile != null) owner.setEntryTile(tile);
        }
    }

    /**
     * Gets the entry tile for this unit, or if null the default
     * entry location for the owning player.
     *
     * @return The entry {@code Tile}.
     */
    public Tile getFullEntryLocation() {
        return (this.entryLocation != null) ? this.entryLocation.getTile()
            : owner.getEntryTile();
    }

    /**
     * Get the moves left this turn.
     *
     * @return The number of moves this {@code Unit} has left.
     */
    @Override
    public int getMovesLeft() {
        return movesLeft;
    }

    /**
     * Get a carried unit by identifier.
     *
     * @param id The identifier of the carried unit.
     * @return The {@code Unit} found, or null if not present.
     */
    public Unit getCarriedUnitById(String id) {
        if (id == null) return null;
        for (Unit u : getUnitList()) {
            if (id.equals(u.getId())) return u;
        }
        return null;
    }

    /**
     * Sets the moves left this turn.
     *
     * @param moves The new amount of moves left this {@code Unit}
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
     * @param type The {@code GoodsType} to produce.
     */
    public void setWorkType(GoodsType type) {
        this.workType = type;
    }

    /**
     * Change the type of goods this unit is producing in its current
     * occupation.  Updates the work location production and the unit
     * experience type if necessary.
     *
     * @param type The {@code GoodsType} to produce.
     */
    public void changeWorkType(GoodsType type) {
        setWorkType(type);
        WorkLocation wl = getWorkLocation();
        if (wl != null) wl.updateProductionType();
    }

    /**
     * Gets the type of goods this unit has accrued experience producing.
     *
     * @return The {@code GoodsType} this unit would produce.
     */
    public GoodsType getExperienceType() {
        return experienceType;
    }

    /**
     * Sets the type of goods this unit has accrued experience producing.
     *
     * @param type The {@code GoodsType} this unit would produce.
     */
    public void changeExperienceType(GoodsType type) {
        if (experienceType != type) {
            experience = 0;
            experienceType = type;
        }
    }

    /**
     * Gets the experience of this {@code Unit} at its current
     * experienceType.
     *
     * @return The experience of this {@code Unit} at its current
     *     experienceType.
     * @see #modifyExperience
     */
    public int getExperience() {
        return experience;
    }

    /**
     * Sets the experience of this {@code Unit} at its current
     * experienceType.
     *
     * @param experience The new experience of this {@code Unit}
     *     at its current experienceType.
     * @see #modifyExperience
     */
    public void setExperience(int experience) {
        this.experience = Math.min(experience,
                                   getType().getMaximumExperience());
    }

    /**
     * Modifies the experience of this {@code Unit} at its current
     * experienceType.
     *
     * @param value The value by which to modify the experience of this
     *     {@code Unit}.
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
                && this.type.hasAbility(Ability.EXPERT_PIONEER))
            ? (getWorkLeft() + 1) / 2
            : getWorkLeft();
    }

    /**
     * Gets the TileImprovement that this pioneer is contributing to.
     *
     * @return The {@code TileImprovement} the pioneer is working on.
     */
    public TileImprovement getWorkImprovement() {
        return workImprovement;
    }

    /**
     * Sets the TileImprovement that this pioneer is contributing to.
     *
     * @param imp The new {@code TileImprovement} the pioneer is to
     *     work on.
     */
    public void setWorkImprovement(TileImprovement imp) {
        workImprovement = imp;
    }

    /**
     * Get the unit being taught.
     *
     * @return A student {@code Unit} if any.
     */
    public final Unit getStudent() {
        return student;
    }

    /**
     * Set the student unit.
     *
     * @param newStudent The new student {@code Unit}.
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
     * @return A teacher {@code Unit}.
     */
    public final Unit getTeacher() {
        return teacher;
    }

    /**
     * Set the teacher for this unit.
     *
     * @param newTeacher The new teacher {@code Unit}.
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
     * @return The number of turns of training this {@code Unit} has
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
     *     {@code Unit} has given.
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
            result = getSpecification()
                .getNeededTurnsOfTraining(getType(), student.getType());
            if (getColony() != null) {
                result -= getColony().getProductionBonus();
            }
        }
        return result;
    }

    /**
     * Get a unit change for this unit.
     *
     * @param change The identifier for the required change type.
     * @return The {@code UnitChange} found, or null if the
     *     change is impossible.
     */
    public UnitTypeChange getUnitChange(String change) {
        return getUnitChange(change, null);
    }

    /**
     * Get a unit change for this unit.
     *
     * @param change The identifier for the required change type.
     * @param toType A {@code UnitType} to change to.
     * @return The {@code UnitChange} found, or null if the
     *     change is impossible.
     */
    public UnitTypeChange getUnitChange(String change, UnitType toType) {
        UnitChangeType uct = getSpecification().getUnitChangeType(change);
        if (uct != null && uct.getOwnerChange()) {
            throw new RuntimeException("2-arg getUnitChange of " + this
                + " change=" + change + " which changes owner");
        }
        return getUnitChange(change, toType, getOwner());
    }

    /**
     * Get a unit change for this unit, including the ownership check.
     *
     * @param change The identifier for the required change type.
     * @param toType A {@code UnitType} to change to.
     * @param player The expected {@code Player} that will own the unit.
     * @return The {@code UnitChange} found, or null if the
     *     change is impossible.
     */
    public UnitTypeChange getUnitChange(String change, UnitType toType,
                                        Player player) {
        if (player == null) {
            throw new RuntimeException("getUnitChange null player: " + change);
        }
        UnitChangeType uct = getSpecification().getUnitChangeType(change);
        if (uct != null && uct.getOwnerChange() != (player != getOwner())) {
            throw new RuntimeException("getUnitChange of " + this
                + " change=" + change
                + " getOwnerChange=" + uct.getOwnerChange()
                + " != player-change=" + (player != getOwner())
                + " player=" + player.getSuffix()
                + " owner=" + getOwner().getSuffix());
        }
        UnitTypeChange uc = (uct == null || !uct.appliesTo(this)) ? null
            : uct.getUnitChange(getType(), toType);
        return (uc == null || !uc.isAvailableTo(player)) ? null : uc;
    }

    /**
     * Get the skill another unit type can teach this unit.
     *
     * Public for the test suite.
     *
     * @param teacherType The {@code UnitType} to teach this unit.
     * @return The {@code UnitType} (skill) this unit can learn.
     */
    public UnitType getTeachingType(UnitType teacherType) {
        UnitType ret = (getSpecification()
            .getUnitChangeType(UnitChangeType.EDUCATION).appliesTo(this))
            ? getType().getTeachingType(teacherType)
            : null;
        return (ret == null || !ret.isAvailableTo(getOwner())) ? null : ret;
    }

    /**
     * Get the skill another unit can teach this unit.
     *
     * @param teacher The {@code Unit} to teach this unit.
     * @return The {@code UnitType} (skill) this unit can learn.
     */
    public UnitType getTeachingType(Unit teacher) {
        return getTeachingType(teacher.getType());
    }

    /**
     * Can this unit be a student of a teacher unit?
     *
     * @param teacher The teacher {@code Unit} which is trying to
     *     teach it.
     * @return True if the unit can be taught by the teacher.
     */
    public boolean canBeStudent(Unit teacher) {
        return teacher != null && teacher != this
            && getTeachingType(teacher) != null;
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
    public final String getNationality() {
        return this.nationality;
    }

    /**
     * Sets the nationality of this Unit.  A unit will change
     * nationality when it switches owners willingly.  Currently only
     * Converts do this, but it opens the possibility of
     * naturalisation.
     *
     * @param newNationality The new nationality of this Unit.
     */
    public final void setNationality(String newNationality) {
        this.nationality = newNationality;
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
    public final String getEthnicity() {
        return this.ethnicity;
    }

    /**
     * Sets the ethnicity of this Unit.
     *
     * @param newEthnicity The new ethnicity of this Unit.
     */
    public final void setEthnicity(String newEthnicity) {
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
     * Gets the {@code IndianSettlement} home for this unit.
     *
     * @return The home {@code IndianSettlement} of this unit.
     */
    public IndianSettlement getHomeIndianSettlement() {
        return indianSettlement;
    }

    /**
     * Sets the home {@code IndianSettlement} for this unit.
     *
     * @param indianSettlement The {@code IndianSettlement} that this unit
     *     considers to be its home.
     */
    public void setHomeIndianSettlement(IndianSettlement indianSettlement) {
        this.indianSettlement = indianSettlement;
    }

    /**
     * Changes the home {@code IndianSettlement} for this unit.
     *
     * @param indianSettlement The {@code IndianSettlement} that should
     *     now own this {@code Unit} and be considered this unit's home.
     * @return The old {@code IndianSettlement}.
     */
    public IndianSettlement changeHomeIndianSettlement(IndianSettlement indianSettlement) {
        if (this.indianSettlement != null) {
            this.indianSettlement.removeOwnedUnit(this);
        }

        IndianSettlement ret = this.indianSettlement;
        this.indianSettlement = indianSettlement;

        if (indianSettlement != null) {
            indianSettlement.addOwnedUnit(this);
        }
        return ret;
    }

    /**
     * Gets the unit hit points.
     *
     * This is currently only used for damaged ships, but might get an
     * extended use later.
     *
     * @return The hit points this {@code Unit} has.
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
        return hitPoints < this.type.getHitPoints();
    }

    /**
     * Get how many turns left to be repaired
     *
     * @return The number of turns left to be repaired.
     */
    public int getTurnsForRepair() {
        return this.type.getHitPoints() - getHitPoints();
    }

    /**
     * Get the destination of this unit.
     *
     * @return The destination {@code Location} of this {@code Unit}.
     */
    public Location getDestination() {
        return destination;
    }

    /**
     * Sets the destination of this unit.
     *
     * @param newDestination The new destination {@code Location}.
     */
    public void setDestination(Location newDestination) {
        this.destination = newDestination;
    }

    /**
     * Get the unit trade route, if any.
     *
     * @return The {@code TradeRoute}, or null if none.
     */
    public final TradeRoute getTradeRoute() {
        return tradeRoute;
    }

    /**
     * Set the unit trade route.
     *
     * @param newTradeRoute The new {@code TradeRoute} value.
     */
    public final void setTradeRoute(final TradeRoute newTradeRoute) {
        this.tradeRoute = newTradeRoute;
    }

    /**
     * Get the stop the unit is heading for or at.
     *
     * @return The target {@code TradeRouteStop}.
     */
    public TradeRouteStop getStop() {
        return (validateCurrentStop() < 0) ? null
            : getTradeRoute().getStop(currentStop);
    }

    /**
     * Get the stop the unit is heading for or at.
     *
     * @return The target {@code TradeRouteStop}.
     */
    public List<TradeRouteStop> getCurrentStops() {
        if (validateCurrentStop() < 0) return null;
        List<TradeRouteStop> stops = getTradeRoute().getStopList();
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
    private int validateCurrentStop() {
        if (tradeRoute == null) {
            currentStop = -1;
        } else {
            int stopCount = tradeRoute.getStopCount();
            if (stopCount <= 0) {
                currentStop = -1;
            } else if (currentStop < 0 || currentStop >= stopCount) {
                // The current stop can become out of range if the trade
                // route is modified.
                currentStop = 0;
            }
        }
        return currentStop;
    }

    /**
     * Convenience function to check if a unit is at a stop.
     *
     * @param stop The {@code TradeRouteStop} to check.
     * @return True if the unit is at the given stop.
     */
    public boolean atStop(TradeRouteStop stop) {
        return Map.isSameLocation(getLocation(), stop.getLocation());
    }

    /**
     * Get the current trade location.
     *
     * @return The {@code TradeLocation} for this unit.
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
        return this.treasureAmount;
    }

    /**
     * Set the amount of treasure in this unit.
     *
     * @param amount The new amount of treasure.
     */
    public void setTreasureAmount(int amount) {
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
     * @return The visible amount of goods carried by this {@code Unit}.
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
     * @return A {@code Role} that can be automatically assumed
     *     by this unit, or null if none.
     */
    public Role getAutomaticRole() {
        if (!hasDefaultRole()) return null;
        Settlement settlement = (isInColony()) ? getColony()
            : (getLocation() instanceof IndianSettlement)
            ? (Settlement)getLocation()
            : null;
        if (settlement == null) return null;

        final Specification spec = getSpecification();
        return find(transform(flatten(getAbilities(Ability.AUTOMATIC_EQUIPMENT),
                                      Ability::getScopes),
                              alwaysTrue(), s -> spec.getRole(s.getType())),
                    r -> r != null
                        && settlement.containsGoods(getGoodsDifference(r, 1)));
    }

    /**
     * After winning a battle, can this unit capture the loser's role
     * equipment?
     *
     * @param role The loser unit {@code Role}.
     * @return The {@code Role} available to this unit as a result
     *     of capturing the loser equipment.
     */
    public Role canCaptureEquipment(Role role) {
        if (!hasAbility(Ability.CAPTURE_EQUIPMENT)) return null;
        final Specification spec = getSpecification();
        final Role oldRole = getRole();
        return find(getAvailableRoles(spec.getMilitaryRolesList()),
            r -> any(r.getRoleChanges(), rc ->
                rc.getFrom(spec) == oldRole && rc.getCapture(spec) == role));
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
     * @param tile The {@code Tile} to check.
     * @return True if the unit is a beached ship.
     */
    public boolean isBeached(Tile tile) {
        return tile != null && tile.isLand() && !tile.hasSettlement()
            && isNaval();
    }

    /**
     * Checks if this is an defensive unit. That is: a unit which can
     * be used to defend a {@code Settlement}.
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
     *     to defend a {@code Colony}.  This would normally mean
     *     that a defensive unit also will be offensive.
     */
    public boolean isDefensiveUnit() {
        return (this.type.isDefensive() || getRole().isDefensive())
            && !isCarrier(); // Not wagons or ships
    }

    /**
     * Checks if this is an offensive unit.  That is, one that can
     * attack other units.
     *
     * @return True if this is an offensive unit.
     */
    public boolean isOffensiveUnit() {
        return this.type.isOffensive() || getRole().isOffensive();
    }

    /**
     * Can this unit ambush another?
     *
     * @param defender The defending {@code Unit}.
     * @return True if an ambush attack is possible.
     */
    public boolean canAmbush(Unit defender) {
        return isOnTile() && getSettlement() == null
            && defender.isOnTile() && defender.getSettlement() == null
            && defender.getState() != UnitState.FORTIFIED
            && (hasAbility(Ability.AMBUSH_BONUS)
                || defender.hasAbility(Ability.AMBUSH_PENALTY))
            && (getTile().hasAbility(Ability.AMBUSH_TERRAIN)
                || defender.getTile().hasAbility(Ability.AMBUSH_TERRAIN));
    }
            
    /**
     * Is an alternate unit a better defender than the current choice.
     * Prefer if there is no current defender, or if the alternate
     * unit is better armed, or provides greater defensive power and
     * does not replace a defensive unit defender with a non-defensive
     * unit.
     *
     * @param defender The current defender {@code Unit}.
     * @param defenderPower Its defence power.
     * @param other An alternate {@code Unit}.
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
     * Finds the closest {@code Location} to this tile where
     * this ship can be repaired, excluding the current colony.
     *
     * @return The closest {@code Location} where a ship can be
     *     repaired.
     */
    public Location getRepairLocation() {
        final Player player = getOwner();
        final Colony notHere = getTile().getColony();
        final Predicate<Colony> repairPred = c ->
            c != notHere && c.hasAbility(Ability.REPAIR_UNITS);
        Location loc = getClosestColony(transform(player.getColonies(), repairPred));
        return (loc != null) ? loc : player.getEurope();
    }

    /**
     * Damage this unit (which should be a ship).
     *
     * @param repair A {@code Location} to send the ship to for repair.
     */
    public void damageShip(Location repair) {
        setHitPoints(1);
        setDestination(null);
        setLocation(repair);//-vis(player)
        setState(Unit.UnitState.ACTIVE);
        setMovesLeft(0);
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
     * Gets the cost of moving this {@code Unit} onto the given
     * {@code Tile}. A call to {@link #getMoveType(Tile)} will return
     * {@code MOVE_NO_MOVES}, if  returns a move cost
     * larger than the {@link #getMovesLeft moves left}.
     *
     * @param target The {@code Tile} this {@code Unit} will move
     *            onto.
     * @return The cost of moving this unit onto the given {@code Tile}.
     */
    public int getMoveCost(Tile target) {
        return getMoveCost(getTile(), target, getMovesLeft());
    }

    /**
     * Gets the cost of moving this {@code Unit} from the given
     * {@code Tile} onto the given {@code Tile}. A call to
     * {@link #getMoveType(Tile, Tile, int)} will return
     * {@code MOVE_NO_MOVES}, if {@link #getMoveCost} returns a move cost
     * larger than the {@link #getMovesLeft} moves left.
     *
     * @param from The {@code Tile} this {@code Unit} will
     *     move from.
     * @param target The {@code Tile} this {@code Unit} will
     *     move onto.
     * @param ml The amount of moves this Unit has left.
     * @return The cost of moving this unit onto the given {@code Tile}.
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
     * @param direction The {@code Direction} of the move.
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
     * @param target The target {@code Tile} of the move.
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
     * @param from The origin {@code Tile} of the move.
     * @param target The target {@code Tile} of the move.
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
     * @param from The origin {@code Tile} of the move.
     * @param target The target {@code Tile} of the move.
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
     * @param target The target {@code Tile} of the move.
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
     * @param from The origin {@code Tile} of the move.
     * @param target The target {@code Tile} of the move.
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
     * @param from The origin {@code Tile} of the move.
     * @param target The target {@code Tile} of the move.
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
                    // Natives do not explore rumours
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
                : (any(target.getUnits(), u -> u.canAdd(this)))
                ? MoveType.EMBARK
                : MoveType.MOVE_NO_ACCESS_FULL;
        }
    }

    /**
     * Get the {@code MoveType} when moving a trading unit to a
     * settlement.
     *
     * @param settlement The {@code Settlement} to move to.
     * @return The appropriate {@code MoveType}.
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
            return (!((IndianSettlement)settlement).allowContact(this))
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
     * Get the {@code MoveType} when moving a colonist to a settlement.
     *
     * @param from The {@code Tile} to move from.
     * @param settlement The {@code Settlement} to move to.
     * @return The appropriate {@code MoveType}.
     */
    private MoveType getLearnMoveType(Tile from, Settlement settlement) {
        if (settlement instanceof Colony) {
            return MoveType.MOVE_NO_ACCESS_SETTLEMENT;
        } else if (settlement instanceof IndianSettlement) {
            return (!allowContact(settlement))
                ? MoveType.MOVE_NO_ACCESS_CONTACT
                : (!allowMoveFrom(from))
                ? MoveType.MOVE_NO_ACCESS_WATER
                : (getUnitChange(UnitChangeType.NATIVES) == null)
                ? MoveType.MOVE_NO_ACCESS_SKILL
                : MoveType.ENTER_INDIAN_SETTLEMENT_WITH_FREE_COLONIST;
        } else {
            return MoveType.MOVE_ILLEGAL; // should not happen
        }
    }

    /**
     * Get the {@code MoveType} when moving a missionary to a settlement.
     *
     * @param from The {@code Tile} to move from.
     * @param settlement The {@code Settlement} to move to.
     * @return The appropriate {@code MoveType}.
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
     * @param from The {@code Tile} to consider.
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
     * @param settlement The {@code Settlement} to consider.
     * @return True if the contact is allowed.
     */
    private boolean allowContact(Settlement settlement) {
        return getOwner().hasContacted(settlement.getOwner());
    }

    /**
     * Does a basic check whether a unit can ever expect to move to a tile.
     *
     * @param tile The code {@code Tile} to check.
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
        return (int)apply(this.type.getMovement(), turn, Modifier.MOVEMENT_BONUS, this.type);
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
            sb.append('(').append(remainder).append("/3) ");
        }
        sb.append('/').append(getInitialMovesLeft() / 3);
        return sb.toString();
    }

    /**
     * Gets the number of turns this unit will need to sail to/from Europe.
     *
     * @return The number of turns to sail to/from Europe.
     */
    public int getSailTurns() {
        float base = getSpecification().getInteger(GameOptions.TURNS_TO_SAIL);
        return (int)getOwner().apply(base, getGame().getTurn(),
                                     Modifier.SAIL_HIGH_SEAS, this.type);
    }

    /**
     * Checks if this {@code Unit} can be moved to the high seas
     * from its current location.
     *
     * @return True if this unit can move immediately to the high seas.
     */
    public boolean canMoveToHighSeas() {
        final Predicate<Tile> highSeasMovePred = t ->
            t.isDirectlyHighSeasConnected() // Quick filter before full check
                && getMoveType(t) == MoveType.MOVE_HIGH_SEAS;
        return (isAtSea()) ? true
            : (isInEurope()) ? getType().canMoveToHighSeas()
            : (hasTile()) ? (getType().canMoveToHighSeas()
                && getOwner().canMoveToEurope()
                && (getTile().isDirectlyHighSeasConnected()
                    || any(getTile().getSurroundingTiles(1, 1),
                           highSeasMovePred)))
            : false;
    }

    /**
     * Check if this unit can build a colony.  Does not consider whether
     * the tile where the unit is located is suitable,
     * @see Player#canClaimToFoundSettlement(Tile)
     *
     * @return {@code true} if this unit can build a colony.
     */
    public boolean canBuildColony() {
        final Specification spec = getSpecification();
        return hasTile() && this.type.canBuildColony() && getMovesLeft() > 0
            && (!getOwner().isRebel()
                || spec.getBoolean(GameOptions.FOUND_COLONY_DURING_REBELLION));
    }

    /**
     * Is this unit at a specified location?
     *
     * @param loc The {@code Location} to test.
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
     * @param tile The target {@code Tile}.
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
        if (!isAtSea()) throw new RuntimeException("Not at sea: " + this);
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
     * Is this unit ready to operate a trade route?
     *
     * @return True if the unit is ready to trade.
     */
    public boolean isReadyToTrade() {
        return !isDisposed()
            && !isDamaged()
            && !isAtSea()
            && !isOnCarrier()
            && !isInColony()
            && getTradeRoute() != null
            && getState() != Unit.UnitState.FORTIFYING
            && getState() != Unit.UnitState.SKIPPED
            && getMovesLeft() > 0;
    }

    /**
     * Basic checks for whether a unit is usable ATM.
     *
     * @return True if the unit might be useful at present.
     */
    private boolean readyAndAble() {
        return !isDisposed()
            && !isDamaged()
            && !isAtSea()
            && !isOnCarrier()
            && !isInColony()
            && getState() == UnitState.ACTIVE
            && getMovesLeft() > 0;
    }

    /**
     * Is this unit a suitable `next active unit', that is, the unit
     * needs to be currently movable by the player.
     *
     * Used as a predicate in Player.nextActiveUnitIterator.
     *
     * @return True if this unit could still be moved by the player.
     */
    public boolean couldMove() {
        return readyAndAble()
            && getDestination() == null
            && getTradeRoute() == null;
    }

    /**
     * Is this unit a suitable `going-to unit', that is, the unit
     * needs have a valid destination and be able to progress towards it.
     *
     * Used as a predicate in Player.nextGoingToUnitIterator.
     *
     * @return True if this unit can go to its destination.
     */
    public boolean goingToDestination() {
        return readyAndAble()
            && getTradeRoute() == null
            && getDestination() != null;
    }

    /**
     * Is this unit available to move along a trade route?
     *
     * Used as a predicate in Player.nextTradeRouteUnitIterator.
     *
     * @return True if this unit can follow a trade route.
     */
    public boolean followingTradeRoute() {
        return readyAndAble()
            && getTradeRoute() != null;
            // Trade route code might set destination
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
        Settlement sett = minimize(owner.getSettlements(),
                                   settlementStartComparator);
        if (sett == null) sett = first(owner.getSettlements());
        if (sett != null) return sett;

        // Owner has no settlements.  If it is the REF, start from a
        // rebel colony.  Prefer the closest port.
        if (owner.isREF()) {
            return minimize(flatten(owner.getRebels(), Player::getSettlements),
                            settlementStartComparator);
        }

        // Desperately find the nearest land to the entry location.
        Location loc = getFullEntryLocation();
        return (loc == null || loc.getTile() == null) ? null
            : find(loc.getTile().getSurroundingTiles(1, INFINITY),
                   Tile::isLand);
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
     * @param loc The {@code Location} to go to.
     * @return True if the unit should use transport.
     */
    public boolean shouldTakeTransportTo(Location loc) {
        PathNode path;
        return loc != null
            && !isNaval()
            && !isAtLocation(loc)
            && ((path = this.findPath(getLocation(), loc,
                                      getCarrier())) == null
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
     * Get a comparator to rank locations by proximity to a start location,
     * using this unit+optional carrier and cost decider.
     *
     * @param start The starting {@code Location}.
     * @param carrier An optional carrier {@code Unit}.
     * @param costDecider An option {@code CostDecider}.
     * @return A suitable {@code Comparator}.
     */
    private Comparator<Tile> getPathComparator(final Location start,
                                               final Unit carrier,
                                               final CostDecider costDecider) {
        return cachingIntComparator((Tile t) -> {
                PathNode p = this.findPath(start, t, carrier,
                                           costDecider, null);
                return (p == null) ? INFINITY : p.getTotalTurns();
            });
    }

    /**
     * Finds the fastest path from the current location to the
     * specified one.  No carrier is provided, and the default cost
     * decider for this unit is used.
     *
     * @param end The {@code Location} in which the path ends.
     * @return A {@code PathNode} from the current location to the
     *     end location, or null if none found.
     */
    public PathNode findPath(Location end) {
        return this.findPath(getLocation(), end);
    }

    /**
     * Finds the fastest path from a given location to a specified
     * one.  No carrier is provided, and the default cost decider for
     * this unit is used.
     *
     * @param start The {@code Location} at which the path starts.
     * @param end The {@code Location} in which the path ends.
     * @return A {@code PathNode} from the current location to the
     *     end location, or null if none found.
     */
    public PathNode findPath(Location start, Location end) {
        return this.findPath(start, end, null);
    }

    /**
     * Finds the fastest path from a given location to a specified
     * one, with an optional carrier.  The default cost decider for
     * the relevant unit is used.
     *
     * @param start The {@code Location} at which the path starts.
     * @param end The {@code Location} in which the path ends.
     * @param carrier An optional carrier {@code Unit} to use.
     * @return A {@code PathNode} from the current location to the
     *     end location, or null if none found.
     */
    public PathNode findPath(Location start, Location end, Unit carrier) {
        return this.findPath(start, end, carrier, null, null);
    }

    /**
     * Finds a quickest path between specified locations, optionally
     * using a carrier and special purpose cost decider.
     *
     * @param start The {@code Location} to start at.
     * @param end The {@code Location} to end at.
     * @param carrier An optional carrier {@code Unit} to carry the unit.
     * @param costDecider An optional {@code CostDecider} for
     *     determining the movement costs (uses default cost deciders
     *     for the unit/s if not provided).
     * @param lb An optional {@code LogBuilder} to log the path to.
     * @return A {@code PathNode}, or null if no path is found.
     * @exception IllegalArgumentException if the destination is null,
     *     (FIXME) this is a temporary debugging measure.
     */
    public PathNode findPath(Location start, Location end, Unit carrier,
                             CostDecider costDecider, LogBuilder lb) {
        if (end == null) {
            throw new IllegalArgumentException("findPath to null for " + this
                + " from " + start + " on " + carrier);
        }
        return getGame().getMap().findPath(this, realStart(start, carrier),
                                           end, carrier, costDecider, lb);
    }

    /**
     * Unified argument tests for full path searches, which then finds
     * the actual starting location for the path.  Deals with special
     * cases like starting on a carrier and/or high seas.
     *
     * @param start The {@code Location} in which the path starts from.
     * @param carrier An optional naval carrier {@code Unit} to use.
     * @return The actual starting location.
     * @throws IllegalArgumentException If there are any argument problems.
     */
    private Location realStart(final Location start, final Unit carrier) {
        if (carrier != null && !carrier.canCarryUnits()) {
            throw new IllegalArgumentException("Non-carrier carrier: "
                + carrier);
        } else if (carrier != null && !carrier.couldCarry(this)) {
            throw new IllegalArgumentException("Carrier could not carry unit: "
                + carrier + "/" + this);
        }

        Location entry;
        if (start == null) {
            throw new IllegalArgumentException("Null start: " + this);
        } else if (start instanceof Unit) {
            Location unitLoc = ((Unit)start).getLocation();
            if (unitLoc == null) {
                throw new IllegalArgumentException("Null on-carrier start: "
                    + this + "/" + start);
            } else if (unitLoc instanceof HighSeas) {
                if (carrier == null) {
                    throw new IllegalArgumentException("Null carrier when"
                        + " starting on high seas: " + this);
                } else if (carrier != start) {
                    throw new IllegalArgumentException("Wrong carrier when"
                        + " starting on high seas: " + this
                        + "/" + carrier + " != " + start);
                }
                entry = carrier.resolveDestination();
            } else {
                entry = unitLoc;
            }
            
        } else if (start instanceof HighSeas) {
            if (isOnCarrier()) {
                entry = getCarrier().resolveDestination();
            } else if (isNaval()) {
                entry = resolveDestination();
            } else {
                throw new IllegalArgumentException("No carrier when"
                    + " starting on high seas: " + this
                    + "/" + getLocation());
            }
        } else if (start instanceof Europe || start.getTile() != null) {
            entry = start; // OK
        } else {
            throw new IllegalArgumentException("Invalid start: " + start);
        }
        // Valid result, reduce to tile if possible.
        return (entry.getTile() != null) ? entry.getTile() : entry;
    }

    /**
     * Convenience wrapper for the
     * {@link net.sf.freecol.common.model.Map#search} function.
     *
     * @param start The {@code Location} to start the search from.
     * @param gd The object responsible for determining whether a
     *     given {@code PathNode} is a goal or not.
     * @param cd An optional {@code CostDecider}
     *     responsible for determining the path cost.
     * @param maxTurns The maximum number of turns the given
     *     {@code Unit} is allowed to move. This is the
     *     maximum search range for a goal.
     * @param carrier An optional naval carrier {@code Unit} to use.
     * @return The path to a goal, or null if none can be found.
     */
    public PathNode search(Location start, GoalDecider gd,
                           CostDecider cd, int maxTurns, Unit carrier) {
        return (start == null) ? null
            : getGame().getMap().search(this, realStart(start, carrier),
                                        gd, cd, maxTurns, carrier, null);
    }

    /**
     * Finds a quickest path to a neighbouring tile to a specified target
     * tile, optionally using a carrier and special purpose cost decider.
     *
     * @param start The {@code Location} to start at.
     * @param end The {@code Tile} to end at a neighbour of.
     * @param carrier An optional carrier {@code Unit} to carry the unit.
     * @param costDecider An optional {@code CostDecider} for
     *     determining the movement costs (uses default cost deciders
     *     for the unit/s if not provided).
     * @return A {@code PathNode}, or null if no path is found.
     */
    public PathNode findPathToNeighbour(Location start, Tile end, Unit carrier,
                                        CostDecider costDecider) {
        final Player owner = getOwner();
        final Predicate<Tile> endPred = t ->
            (isTileAccessible(t)
                && (t.getFirstUnit() == null || owner.owns(t.getFirstUnit())));

        Tile best = minimize(end.getSurroundingTiles(1, 1), endPred,
            getPathComparator(start, carrier, costDecider));
        return (best == null) ? null
            : this.findPath(start, best, carrier, costDecider, null);
    }

    /**
     * Gets the number of turns required for this unit to reach a
     * destination location from its current position.  If the unit is
     * currently on a carrier, it will be used.
     *
     * @param end The destination {@code Location}.
     * @return The number of turns it will take to reach the destination,
     *         or {@code MANY_TURNS} if no path can be found.
     */
    public int getTurnsToReach(Location end) {
        return getTurnsToReach(getLocation(), end);
    }

    /**
     * Gets the number of turns required for this unit to reach a
     * destination location from a starting location.  If the unit is
     * currently on a carrier, it will be used.
     *
     * @param start The {@code Location} to start the search from.
     * @param end The destination {@code Location}.
     * @return The number of turns it will take to reach the {@code end},
     *         or {@code MANY_TURNS} if no path can be found.
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
     * @param start The {@code Location} to start the search from.
     * @param end The destination {@code Location}.
     * @param carrier An optional carrier {@code Unit} to use.
     * @param costDecider An optional {@code CostDecider} to
     *     score the path with.
     * @return The number of turns it will take to reach the {@code end},
     *         or {@code MANY_TURNS} if no path can be found.
     */
    public int getTurnsToReach(Location start, Location end, Unit carrier,
                               CostDecider costDecider) {
        PathNode path = this.findPath(start, end, carrier, costDecider, null);
        return (path == null) ? MANY_TURNS : path.getTotalTurns();
    }

    /**
     * Get the colony that can be reached by this unit in the least number
     * of turns.
     *
     * @param colonies A list of {@code Colony}s.
     * @return The nearest {@code Colony}, or null if none found.
     */
    public Colony getClosestColony(List<Colony> colonies) {
        return getClosestColony(colonies.stream());
    }

    /**
     * Get the colony that can be reached by this unit in the least number
     * of turns.
     *
     * @param colonies A stream of {@code Colony}s.
     * @return The nearest {@code Colony}, or null if none found.
     */
    public Colony getClosestColony(Stream<Colony> colonies) {
        final Comparator<Colony> comp = cachingIntComparator(col ->
            (col == null) ? MANY_TURNS-1 : this.getTurnsToReach(col));
        return minimize(concat(Stream.of((Colony)null), colonies), comp);
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
        if (!player.hasSettlements() || !hasTile()) return null;
        return findOurNearestSettlement(getTile(), excludeStart,
                                        range, coastal);
    }

    /**
     * Find a path for this unit to the nearest settlement with the
     * same owner that is reachable without a carrier.
     *
     * @param startTile The {@code Tile} to start searching from.
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
        if (startTile == null || !player.hasSettlements()) return null;

        GoalDecider gd = new ClosestSettlementGoalDecider((excludeStart) ? startTile : null, coastal);
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
            ePath = (europe == null) ? null : this.findPath(europe);
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
     * @param dst The destination {@code Location}.
     * @return A path to the port, or null if none found.
     */
    public PathNode findIntermediatePort(Location dst) {
        final Settlement ignoreSrc = getSettlement();
        final Settlement ignoreDst = dst.getSettlement();
        final Tile srcTile = getTile();
        final Tile dstTile = dst.getTile();
        final int dstCont = (dstTile == null) ? -1 : dstTile.getContiguity();
        final Comparator<Settlement> settlementComparator
            = cachingIntComparator(s -> {
                    PathNode p = this.findPath(s);
                    return (p == null) ? INFINITY
                    : p.getTotalTurns() + dstTile.getDistanceTo(s.getTile());
                });

        PortMode type;
        if (isNaval()) {
            if (!srcTile.isHighSeasConnected()) {
                // On a lake!  FIXME: do better
                type = PortMode.LAKE;
            } else if (dstTile == null) {
                // Carrier must be blocked from high seas
                type = PortMode.NO_HIGH_SEAS;
            } else if (dstTile.isHighSeasConnected()) {
                // Carrier is blocked or destination is blocked.
                type = (getTile().isOnRiver()) ? PortMode.NO_HIGH_SEAS
                    : PortMode.BLOCKED;
            } else {
                // Destination must be blocked
                type = PortMode.BLOCKED;
            }
        } else {
            if (dstTile == null || getTile().getContiguity() != dstCont) {
                // Ocean travel will be required
                // If already at port try to improve its connectivity,
                // otherwise go to a port.
                type = (srcTile.isHighSeasConnected()) ? PortMode.NO_HIGH_SEAS
                    : PortMode.BLOCKED;
            } else {
                // Pure land travel, just find a nearer settlement.
                type = PortMode.LAND;
            }
        }

        PathNode path = null;
        Settlement sett;
        switch (type) {
        case LAKE:
            // No progress possible.
            break;
        case NO_HIGH_SEAS:
            // Starting on a river, probably blocked in there.
            // Find the settlement that most reduces the high seas count.
            path = search(getLocation(),
                          GoalDeciders.getReduceHighSeasCountGoalDecider(this),
                          null, INFINITY, null);
            break;
        case BLOCKED:
            // Ocean travel required, destination blocked.
            // Find the closest available connected port.
            final Predicate<Settlement> portPredicate = s ->
                s != ignoreSrc && s != ignoreDst;
            sett = minimize(getOwner().getConnectedPortList(), portPredicate,
                            settlementComparator);
            path = (sett == null) ? null : this.findPath(sett);
            break;
        case LAND:
            // Land travel.  Find nearby settlement with correct contiguity.
            final Predicate<Settlement> contiguityPred = s ->
                s != ignoreSrc && s != ignoreDst
                    && s.getTile().getContiguity() == dstCont;
            sett = minimize(getOwner().getSettlements(), contiguityPred,
                            settlementComparator);
            path = (sett == null) ? null : this.findPath(sett);
            break;
        }
        return (path != null) ? path
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
     * Can this unit attack a specified defender?
     *
     * A naval unit can never attack a land unit or settlement,
     * but a land unit *can* attack a naval unit if it is beached.
     * Otherwise naval units can only fight at sea, land units
     * only on land.
     *
     * @param defender The defending {@code Unit}.
     * @return True if this unit can attack.
     */
    public boolean canAttack(Unit defender) {
        if (defender == null || !defender.hasTile()
            || !isOffensiveUnit()) return false;

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
                    final Predicate<Unit> attackerPred = u -> {
                        PathNode p;
                        return (u.canAttack(unit)
                            && cm.calculateCombatOdds(u, unit).win >= threat
                            && (p = u.findPath(start)) != null
                            && p.getTotalTurns() < range);
                    };
                    if (any(transform(tile.getUnits(), attackerPred))) {
                        found = path;
                        return true;
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
     * Gets the line of sight of this {@code Unit}.  That is the
     * distance this {@code Unit} can sight new tiles.
     *
     * @return The line of sight of this {@code Unit}.
     */
    public int getLineOfSight() {
        final Turn turn = getGame().getTurn();
        return (int)applyModifiers(this.type.getLineOfSight(), turn,
            Stream.concat(this.getModifiers(Modifier.LINE_OF_SIGHT_BONUS,
                                            this.type, turn),
                ((hasTile() && getTile().isExplored())
                    ? getTile().getType().getModifiers(Modifier.LINE_OF_SIGHT_BONUS, this.type, turn)
                    : Stream.<Modifier>empty())));
    }

    /**
     * Get the tiles visible to this unit.
     *
     * @return A set of visible {@code Tile}s.
     */
    public Set<Tile> getVisibleTileSet() {
        final Tile tile = getTile();
        return (tile == null) ? Collections.<Tile>emptySet()
            : new HashSet<Tile>(tile.getSurroundingTiles(0, getLineOfSight()));
    }


    // Goods handling

    /**
     * Get the goods carried by this unit.
     *
     * @param compact If true create a compact list.
     * @return A list of {@code Goods}.
     */
    private List<Goods> getGoodsInternal(boolean compact) {
        GoodsContainer gc = getGoodsContainer();
        if (gc == null) return Collections.<Goods>emptyList();
        List<Goods> goods = (compact) ? gc.getCompactGoodsList()
            : gc.getGoodsList();
        for (Goods g : goods) g.setLocation(this);
        return goods;
    }

    /**
     * Get the goods carried by this unit.
     *
     * @return A list of {@code Goods}.
     */
    @Override
    public List<Goods> getGoodsList() {
        return getGoodsInternal(false);
    }

    /**
     * Get a compact version of the goods carried by this unit.
     *
     * @return A compact list of {@code Goods}.
     */
    @Override
    public List<Goods> getCompactGoodsList() {
        return getGoodsInternal(true);
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
     * @param u The potential cargo {@code Unit}.
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
     * @param g The potential cargo {@code Goods}.
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
        return this.type.getSpace();
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
        return (canCarryUnits()) ? sum(getUnits(), Unit::getSpaceTaken)
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
     * @param type The {@code GoodsType} to examine.
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
     * @param player The {@code Player} viewing the unit, as the
     *     owner has access to more information.
     * @param full If true, return a more detailed result.
     * @return A {@code StringTemplate} for the unit occupation.
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
            } else if (getState() == UnitState.ACTIVE && getMovesLeft() == 0
                       && !isInEurope()) {
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
        return 0.01f * apply(opt, getGame().getTurn(),
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
     * Checks if the treasure train can be cashed in at it's current
     * {@code Location}.
     *
     * @return {@code true} if the treasure train can be cashed in.
     * @exception IllegalStateException if this unit is not a treasure train.
     */
    public boolean canCashInTreasureTrain() {
        return canCashInTreasureTrain(getLocation());
    }

    /**
     * Checks if the treasure train can be cashed in at the given
     * {@code Location}.
     *
     * @param loc The {@code Location}.
     * @return {@code true} if the treasure train can be cashed in.
     * @exception IllegalStateException if this unit is not a treasure train.
     */
    public boolean canCashInTreasureTrain(Location loc) {
        if (!canCarryTreasure()) {
            throw new RuntimeException("Can't carry treasure: " + this);
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
            return (int)getOwner().apply(fee, getGame().getTurn(),
                Modifier.TREASURE_TRANSPORT_FEE, this.type);
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
        return getUnitSkillLevel(this.type);
    }

    /**
     * Gets the skill level of the given type of {@code Unit}.
     *
     * @param unitType The type of {@code Unit}.
     * @return The level of skill for the given unit.  A higher value
     *     signals a more advanced type of units.
     */
    public static int getUnitSkillLevel(UnitType unitType) {
        return (unitType.hasSkill()) ? unitType.getSkill() : 0;
    }

    /**
     * Gets the appropriate trade bonuses due to a missionary unit.
     *
     * @param sense The sense to apply the modifiers.
     * @return The missionary trade bonuses.
     */
    public Set<Modifier> getMissionaryTradeModifiers(boolean sense) {
        final Function<Modifier, Modifier> mapper = m -> {
            Modifier mod = Modifier.makeModifier(m);
            if (!sense) mod.setValue(-m.getValue());
            return mod;
        };
        return transform(getModifiers(Modifier.MISSIONARY_TRADE_BONUS),
                         m -> m.getValue() != 0, mapper, Collectors.toSet());
    }

    /**
     * Adds a feature to the Unit.  This method always throws an
     * {@code UnsupportedOperationException}, since features can
     * not be added to Units directly.
     *
     * @param feature The {@code Feature} to add.
     */
    public void addFeature(Feature feature) {
        throw new UnsupportedOperationException("Can not add Feature to Unit directly!");
    }

    /**
     * Gets the {@code ProductionInfo} for this unit.
     *
     * FIXME: the input parameter is ignored! Fix?
     *
     * @param input A list of input {@code AbstractGoods}.
     * @return The {@code ProductionInfo} for this unit.
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
     * @param player The {@code Player} to evaluate for.
     * @return A value of this unit.
     */
    public int evaluateFor(Player player) {
        final Europe europe = player.getEurope();
        return (europe == null) ? 500 : europe.getUnitPrice(getType());
    }

    // @compat 0.11.0
    /**
     * Get modifiers required for combat.
     *
     * This can be replaced with just getModifiers() when accepted
     * specifications have all combat modifiers with correct index
     * values.
     *
     * @param id The identifier to get combat modifiers for.
     * @param fcgot An optional {@code FreeColSpecObjectType} the
     *     modifier applies to.
     * @param turn An optional applicable {@code Turn}.
     * @return The set of {@code Modifier}s found.
     */
    public Set<Modifier> getCombatModifiers(String id,
        FreeColSpecObjectType fcgot, Turn turn) {
        final Player owner = getOwner();
        final UnitType unitType = getType();
        Set<Modifier> result = new HashSet<>();

        // UnitType modifiers always apply
        result.addAll(transform(unitType.getModifiers(id, fcgot, turn),
                alwaysTrue(),
                m -> m.setModifierIndex((m.getType() == Modifier.ModifierType.ADDITIVE)
                    ? Modifier.UNIT_ADDITIVE_COMBAT_INDEX
                    : Modifier.UNIT_NORMAL_COMBAT_INDEX)));

        // The player's modifiers may not all apply
        result.addAll(transform(owner.getModifiers(id, fcgot, turn),
                alwaysTrue(),
                m -> m.setModifierIndex(Modifier.GENERAL_COMBAT_INDEX)));

        // Role modifiers apply
        result.addAll(transform(role.getModifiers(id, fcgot, turn),
                alwaysTrue(),
                m -> m.setModifierIndex(Modifier.ROLE_COMBAT_INDEX)));

        return result;
    }
    // end @compat 0.11.0

    /**
     * Is this unit a person that is making a given goods type, but not
     * an expert at it.
     *
     * @param work The {@code GoodsType} to check.
     * @return True if this unit is a non-expert worker.
     */
    private boolean nonExpertWorker(GoodsType work) {
        return isPerson() && getWorkType() == work
            && getType().getExpertProduction() != work;
    }

    /**
     * Try to swap this unit if it is an expert for another that is
     * doing its job.
     *
     * @param others A list of other {@code Unit}s to test against.
     * @return The unit that was replaced by this expert, or null if none.
     */
    public Unit trySwapExpert(List<Unit> others) {
        final GoodsType work = getType().getExpertProduction();
        if (work == null) return null;
        final Unit other = find(others, u -> u.nonExpertWorker(work));
        if (other != null) swapWork(other);
        return other;
    }

    /**
     * Swap work with another unit.
     *
     * @param other The other {@code Unit}.
     */
    public void swapWork(Unit other) {
        final Colony colony = getColony();
        final Role oldRole = getRole();
        final int oldRoleCount = getRoleCount();
        final GoodsType work = getType().getExpertProduction();
        final GoodsType oldWork = getWorkType();
        Location l1 = getLocation();
        Location l2 = other.getLocation();
        other.setLocation(colony.getTile());
        setLocation(l2);
        changeWorkType(work);
        other.setLocation(l1);
        if (oldWork != null) other.changeWorkType(oldWork);
        Role tmpRole = other.getRole();
        int tmpRoleCount = other.getRoleCount();
        other.changeRole(oldRole, oldRoleCount);
        changeRole(tmpRole, tmpRoleCount);
    }

        
    // Message unpacking support.

    /**
     * Gets the tile in a given direction.
     *
     * @param directionString The direction.
     * @return The {@code Tile} in the given direction.
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
     * @param <T> The {@link Settlement} type.
     * @param settlementId The identifier of the {@code Settlement} to find.
     * @param returnClass The expected returned settlement class.
     * @return The settlement corresponding to the settlementId argument.
     */
    public <T extends Settlement> T getAdjacentSettlement(String settlementId,
                                                          Class<T> returnClass) {
        Game game = getOwner().getGame();

        T ret = game.getFreeColGameObject(settlementId, returnClass);
        if (ret == null) {
            throw new IllegalStateException("Not a settlement: "
                + settlementId);
        } else if (ret.getTile() == null) {
            throw new IllegalStateException("Settlement is not on the map: "
                + settlementId);
        } else if (!getOwner().hasContacted(ret.getOwner())) {
            throw new IllegalStateException("Player " + getOwner().getId()
                + " has not contacted: " + ret.getOwner().getId());
        }

        if (!hasTile()) {
            throw new IllegalStateException("Unit is not on the map: "
                + getId());
        } else if (getTile().getDistanceTo(ret.getTile()) > 1) {
            throw new IllegalStateException("Unit " + getId()
                + " is not adjacent to settlement: " + settlementId);
        } else if (getOwner() == ret.getOwner()) {
            throw new IllegalStateException("Unit: " + getId()
                + " and settlement: " + settlementId
                + " are both owned by player: " + getOwner().getId());
        }

        return ret;
    }

    /**
     * Copy the unit, reduce visibility into any carrier and reference
     * to a settlement.
     *
     * This is used when unit information is attached to an animation.
     * The normal scope rules are inadequate there as the unit *must* be
     * visible, but would normally be invisible if in a settlement or
     * on a carrier.
     *
     * @param tile The {@code Tile} the unit appears at.
     * @param player The {@code Player} the copy is for.
     * @return This {@code Unit} with reduced visibility.
     */
    public Unit reduceVisibility(Tile tile, Player player) {
        final Game game = getGame();
        Unit ret = this.copy(game, player);
        if (isOnCarrier()) {
            Unit carrier = getCarrier().copy(game, player);
            carrier.removeAll();
            carrier.add(ret);
            carrier.setLocationNoUpdate(tile);
            ret.setLocationNoUpdate(carrier);
        } else {
            ret.setLocationNoUpdate(tile);
            ret.setWorkType(null);
            ret.setState(Unit.UnitState.ACTIVE);
        }
        return ret;
    }


    // Interface Consumer

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AbstractGoods> getConsumedGoods() {
        return this.type.getConsumedGoods();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPriority() {
        return this.type.getPriority();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<Modifier> getConsumptionModifiers(String id) {
        return getModifiers(id);
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
     * @return The location of this {@code Unit}.
     */
    @Override
    public Location getLocation() {
        return this.location;
    }

    /**
     * Sets the location of this unit.
     *
     * -vis: This routine changes player visibility.
     * -til: While units do not contribute to tile appearance as such, if
     *     they move in/out of a colony the visible colony size changes.
     *
     * @param newLocation The new {@code Location}.
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

        if (newLocation == this.location) return true;
        if (newLocation != null && !newLocation.canAdd(this)) {
            logger.warning("Can not add " + this + " to " + newLocation);
            return false;
        }

        // If the unit either starts or ends this move in a colony
        // then teaching status can change.  However, if it moves
        // between locations within the same colony with the same
        // teaching ability, the teaching state should *not* change.
        // We have to handle this issue here in setLocation as this is
        // the only place that contains information about both
        // locations.
        Colony oldColony = (isInColony()) ? this.location.getColony() : null;
        Colony newColony = (newLocation instanceof WorkLocation)
            ? newLocation.getColony() : null;
        boolean withinColony = newColony != null && newColony == oldColony;
        boolean preserveEducation = withinColony
            && (((WorkLocation)this.location).canTeach()
                == ((WorkLocation)newLocation).canTeach());

        // First disable education that will fail due to the move.
        if (oldColony != null && !preserveEducation) {
            oldColony.updateEducation(this, false);
        }

        // Move out of the old location.
        if (this.location == null) {
            ; // do nothing
        } else if (!this.location.remove(this)) {//-vis
            // "Should not happen" (should always be able to remove)
            throw new RuntimeException("Failed to remove " + this
                + " from " + this.location.getId());
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
     * Checks if this {@code Unit} is located in Europe.  That
     * is; either directly or onboard a carrier which is in Europe.
     *
     * @return True if in {@code Europe}.
     */
    @Override
    public boolean isInEurope() {
        return (isOnCarrier()) ? getCarrier().isInEurope()
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
        return (getLocation() != null) ? getLocation().getTile() : null;
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
            throw new RuntimeException("Locatable must not be null: " + this);
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
        Location loc = getLocation();
        return (loc != null) ? loc.getSettlement() : null;
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
        return Location.rankOf(getLocation());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toShortString() {
        StringBuilder sb = new StringBuilder(32);
        sb.append(getId()).append('-').append(getType().getSuffix());
        if (!hasDefaultRole()) {
            sb.append('-').append(getRoleSuffix());
            int count = getRoleCount();
            if (count > 1) sb.append('.').append(count);
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
        return this.type.getSpaceTaken();
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
    public void invalidateCache() {}

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
        Location loc = getLocation();
        if (loc != null) {
            loc.remove(this);
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

        changeHomeIndianSettlement(null);

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
    public IntegrityType checkIntegrity(boolean fix, LogBuilder lb) {
        IntegrityType result = super.checkIntegrity(fix, lb);
        if (this.role == null) {
            if (fix) {
                this.role = getSpecification().getDefaultRole();
                lb.add("\n  Missing role set to default for: ", getId());
                result = result.fix();
            } else {
                lb.add("\n  Missing role for: ", getId());
                result = result.fail();
            }
        }
        if (this.destination != null) {
            if (!((FreeColGameObject)this.destination).isInitialized()) {
                if (fix) {
                    this.destination = null;
                    lb.add("\n  Uninitialized destination cleared for: ",
                        getId());
                    result = result.fix();
                } else {
                    lb.add("\n  Uninitialized destination for: ", getId());
                    result = result.fail();
                }
            }
        }
        if (this.state == UnitState.IMPROVING
            && this.workImprovement == null) {
            // This can happen as a result of trying to read an invalid
            // improvement.
            if (fix) {
                this.state = UnitState.ACTIVE;
                lb.add("\n  Improving unit without improvement made active: ",
                    getId());
                result = result.fix();
            } else {
                lb.add("\n  Improving unit without improvement: ", getId());
                result = result.fail();
            }
        }
        return result;
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream <Ability> getAbilities(String id, FreeColSpecObjectType fcgot,
                                         Turn turn) {
        final Player owner = getOwner();
        final UnitType unitType = getType();

        return concat(
            // UnitType abilities always apply.
            unitType.getAbilities(id),

            // Roles apply with qualification.
            role.getAbilities(id, fcgot, turn),

            // The player's abilities require more qualification.
            owner.getAbilities(id, fcgot, turn),

            // Location abilities may apply.
            getLocationAbilities(id, turn));
    }

    /**
     * Get abilities specific to this location.
     *
     * This is here just to simplify getAbilities().  Perhaps one day
     * it could be wrapped back in, but there is unresolved complexity.
     *
     * FIXME: extend this to all locations?  May simplify code.  Units
     * are also Locations however, which complicates the issue as we
     * do not want Units aboard other Units to share the abilities of
     * the carriers.
     *
     * @param id The identifier to check.
     * @param turn The turn that applies.
     * @return A stream of {@code Ability}s found.
     */
    private Stream<Ability> getLocationAbilities(String id, Turn turn) {
        final UnitType unitType = getType();
        final Settlement settlement = getSettlement();
        if (settlement != null) {
            return settlement.getAbilities(id, unitType, turn);
        }
        if (isInEurope()) {
            Europe europe = owner.getEurope();
            if (europe != null) return europe.getAbilities(id, getType(), turn);
        }
        return Stream.<Ability>empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<Modifier> getModifiers(String id, FreeColSpecObjectType fcgot,
                                         Turn turn) {
        final Player owner = getOwner();
        final UnitType unitType = getType();

        return concat(// UnitType modifiers always apply.
                      unitType.getModifiers(id, fcgot, turn),
                      // The player's modifiers apply.
                      owner.getModifiers(id, fcgot, turn),
                      // Role modifiers apply.
                      role.getModifiers(id, fcgot, turn));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getClassIndex () {
        return UNIT_CLASS_INDEX;
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        Unit o = copyInCast(other, Unit.class);
        if (o == null || !super.copyIn(o)) return false;
        final Game game = getGame();
        this.name = o.getName();
        this.owner = game.updateRef(o.getOwner());
        this.type = o.getType();
        this.state = o.getState();
        this.role = o.getRole();
        this.roleCount = o.getRoleCount();
        this.location = game.updateLocationRef(o.getLocation());
        this.entryLocation = game.updateLocationRef(o.getEntryLocation());
        this.movesLeft = o.getMovesLeft();
        this.workType = o.getWorkType();
        this.experienceType = o.getExperienceType();
        this.experience = o.getExperience();
        this.workLeft = o.getWorkLeft();
        // Allow creation, might be first sight
        this.workImprovement = game.update(o.getWorkImprovement(), true);
        this.student = game.updateRef(o.getStudent());
        this.teacher = game.updateRef(o.getTeacher());
        this.turnsOfTraining = o.getTurnsOfTraining();
        this.nationality = o.getNationality();
        this.ethnicity = o.getEthnicity();
        this.indianSettlement = game.updateRef(o.getIndianSettlement());
        this.hitPoints = o.getHitPoints();
        this.destination = game.updateLocationRef(o.getDestination());
        this.tradeRoute = game.updateRef(o.getTradeRoute());
        this.currentStop = o.getCurrentStop();
        this.treasureAmount = o.getTreasureAmount();
        this.attrition = o.getAttrition();
        this.visibleGoodsCount = o.getVisibleGoodsCount();

        this.owner.addUnit(this);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColObject getDisplayObject() {
        return getType();
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
    // @compat 0.11.0
    private static final String OLD_EQUIPMENT_TAG = "equipment";
    // end @compat 0.11.0
    // @compat 0.11.3
    private static final String OLD_TILE_IMPROVEMENT_TAG = "tileimprovement";
    // end @compat 0.11.3


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        if (name != null) xw.writeAttribute(NAME_TAG, name);

        xw.writeAttribute(UNIT_TYPE_TAG, this.type);

        xw.writeAttribute(MOVES_LEFT_TAG, movesLeft);

        xw.writeAttribute(STATE_TAG, state);

        xw.writeAttribute(ROLE_TAG, role);

        xw.writeAttribute(ROLE_COUNT_TAG, roleCount);

        if (!xw.validFor(getOwner()) && isOwnerHidden()) {
            // Pirates do not disclose national characteristics.
            xw.writeAttribute(OWNER_TAG, getGame().getUnknownEnemy());

        } else {
            xw.writeAttribute(OWNER_TAG, getOwner());

            if (nationality != null) {
                xw.writeAttribute(NATIONALITY_TAG, nationality);
            }

            if (ethnicity != null) {
                xw.writeAttribute(ETHNICITY_TAG, ethnicity);
            }
        }

        if (location != null) {
            xw.writeLocationAttribute(LOCATION_TAG, location);
        }

        xw.writeAttribute(TREASURE_AMOUNT_TAG, treasureAmount);

        if (xw.validFor(getOwner())) {
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

        } else {
            if (getType().canCarryGoods()) {
                xw.writeAttribute(VISIBLE_GOODS_COUNT_TAG, getVisibleGoodsCount());
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
        final WorkLocation oldWorkLocation = getWorkLocation();

        name = xr.getAttribute(NAME_TAG, (String)null);

        Player oldOwner = owner;
        owner = xr.findFreeColGameObject(game, OWNER_TAG,
                                         Player.class, (Player)null, true);
        if (xr.shouldIntern()) game.checkOwners(this, oldOwner);

        this.type = xr.getType(spec, UNIT_TYPE_TAG,
                               UnitType.class, (UnitType)null);

        state = xr.getAttribute(STATE_TAG, UnitState.class, UnitState.ACTIVE);

        role = xr.getType(spec, ROLE_TAG, Role.class, spec.getDefaultRole());

        roleCount = xr.getAttribute(ROLE_COUNT_TAG, role.getMaximumCount());

        setLocationNoUpdate(xr.getLocationAttribute(game, LOCATION_TAG, true));

        entryLocation = xr.getLocationAttribute(game, ENTRY_LOCATION_TAG,
                                                true);

        movesLeft = xr.getAttribute(MOVES_LEFT_TAG, 0);

        workLeft = xr.getAttribute(WORK_LEFT_TAG, 0);

        attrition = xr.getAttribute(ATTRITION_TAG, 0);

        nationality = xr.getAttribute(NATIONALITY_TAG, (String)null);

        ethnicity = xr.getAttribute(ETHNICITY_TAG, (String)null);

        turnsOfTraining = xr.getAttribute(TURNS_OF_TRAINING_TAG, 0);

        hitPoints = xr.getAttribute(HIT_POINTS_TAG, -1);

        teacher = xr.makeFreeColObject(game, TEACHER_TAG, Unit.class, false);

        student = xr.makeFreeColObject(game, STUDENT_TAG, Unit.class, false);

        setHomeIndianSettlement(xr.makeFreeColObject(game,
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

        setWorkType(xr.getType(spec, WORK_TYPE_TAG, GoodsType.class, null));

        // Fix changes to production
        WorkLocation wl = getWorkLocation();
        if (wl != null && wl != oldWorkLocation) wl.updateProductionType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        if (getGoodsContainer() != null) getGoodsContainer().removeAll();
        workImprovement = null;

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final Game game = getGame();
        final String tag = xr.getLocalName();

        // @compat 0.11.0
        if (OLD_EQUIPMENT_TAG.equals(tag)) {
            xr.swallowTag(OLD_EQUIPMENT_TAG);
        // end @compat 0.11.0

        } else if (TileImprovement.TAG.equals(tag)
                   // @compat 0.11.3
                   || OLD_TILE_IMPROVEMENT_TAG.equals(tag)
                   // end @compat 0.11.3
                   ) {
            workImprovement = xr.readFreeColObject(game, TileImprovement.class);

        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return TAG; }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toString("");
    }

    /**
     * Gets a string representation of this unit.
     *
     * @param prefix A prefix (e.g. "AIUnit")
     * @return A string representation of this {@code Unit}.
     */
    public String toString(String prefix) {
        StringBuilder sb = new StringBuilder(64);
        sb.append('[').append(prefix).append(getId());
        if (!isInitialized()) {
            sb.append(" uninitialized");
        } else if (isDisposed()) {
            sb.append(" disposed");
        } else {
            sb.append(' ').append(lastPart(owner.getNationId(), "."))
                .append(' ').append(getType().getSuffix());
            if (!hasDefaultRole()) {
                sb.append('-').append(getRoleSuffix());
                int count = getRoleCount();
                if (count > 1) sb.append('.').append(count);
            }
            sb.append(' ').append(getMovesAsString());
        }
        sb.append(']');
        return sb.toString();
    }
}
