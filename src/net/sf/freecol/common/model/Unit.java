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
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.pathfinding.CostDecider;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.common.model.TradeRoute.Stop;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;
import net.sf.freecol.common.util.EmptyIterator;

import org.w3c.dom.Element;


/**
 * Represents all pieces that can be moved on the map-board. This includes:
 * colonists, ships, wagon trains e.t.c.
 *
 * <br>
 * <br>
 *
 * Every <code>Unit</code> is owned by a {@link Player} and has a
 * {@link Location}.
 */
public class Unit extends FreeColGameObject
    implements Consumer, Locatable, Location, Movable, Nameable, Ownable {

    private static final Logger logger = Logger.getLogger(Unit.class.getName());

    /** A comparator to order units by skill level. */
    private static Comparator<Unit> skillLevelComp
        = new Comparator<Unit>() {
            public int compare(Unit u1, Unit u2) {
                return u1.getSkillLevel() - u2.getSkillLevel();
            }
        };


    /**
     * XML tag name for equipment list.
     */
    private static final String EQUIPMENT_TAG = "equipment";

    public static final String CARGO_CHANGE = "CARGO_CHANGE";
    public static final String EQUIPMENT_CHANGE = "EQUIPMENT_CHANGE";

    /**
     * A state a Unit can have.
     */
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

    protected UnitType unitType;

    protected int movesLeft;

    protected UnitState state = UnitState.ACTIVE;

    protected Role role = Role.DEFAULT;

    /**
     * The number of turns until the work is finished, or '-1' if a
     * Unit can stay in its state forever.
     */
    protected int workLeft;

    protected int hitpoints; // For now; only used by ships when repairing.

    protected Player owner;

    protected String nationality = null;

    protected String ethnicity = null;

    protected List<Unit> units = Collections.emptyList();

    protected GoodsContainer goodsContainer;

    protected Location entryLocation;

    protected Location location;

    protected IndianSettlement indianSettlement = null; // only used by Brave and Convert

    protected Location destination = null;

    /** The trade route this unit has. */
    protected TradeRoute tradeRoute = null;

    /** Which stop in a trade route the unit is going to. */
    protected int currentStop = -1;

    /** To be used only for type == TREASURE_TRAIN */
    protected int treasureAmount;

    /**
     * What is being improved (to be used only for PIONEERs - where
     * they are working.
     */
    protected TileImprovement workImprovement;

    /** What type of goods this unit produces in its occupation. */
    protected GoodsType workType;

    /** What type of goods this unit last earned experience producing. */
    private GoodsType experienceType;

    protected int experience = 0;

    protected int turnsOfTraining = 0;

    /**
     * The attrition this unit has accumulated. At the moment, this
     * equals the number of turns it has spent in the open.
     */
    protected int attrition = 0;

    /** The individual name of this unit, not of the unit type. */
    protected String name = null;

    /**
     * The amount of goods carried by this unit. This variable is only used by
     * the clients. A negative value signals that the variable is not in use.
     *
     * @see #getVisibleGoodsCount()
     */
    protected int visibleGoodsCount;

    /** The student of this Unit, if it has one. */
    protected Unit student;

    /** The teacher of this Unit, if it has one. */
    protected Unit teacher;

    /** The equipment this Unit carries. */
    protected TypeCountMap<EquipmentType> equipment
        = new TypeCountMap<EquipmentType>();


    /**
     * Constructor for ServerUnit.
     */
    protected Unit() {
        // empty constructor
    }

    /**
     * Constructor for ServerUnit.
     *
     * @param game The <code>Game</code> in which this unit belongs.
     */
    protected Unit(Game game) {
        super(game);
    }

    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param game The <code>Game</code> in which this <code>Unit</code>
     *            belong.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     */
    public Unit(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);
        readFromXML(in);
    }

    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param game The <code>Game</code> in which this <code>Unit</code>
     *            belong.
     * @param e An XML-element that will be used to initialize this object.
     */
    public Unit(Game game, Element e) {
        super(game, e);
        readFromXMLElement(e);
    }

    /**
     * Initiates a new <code>Unit</code> with the given ID. The object should
     * later be initialized by calling either
     * {@link #readFromXML(XMLStreamReader)} or
     * {@link #readFromXMLElement(Element)}.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param id The unique identifier for this object.
     */
    public Unit(Game game, String id) {
        super(game, id);
    }

    /**
     * Returns <code>true</code> if the Unit can carry other Units.
     *
     * @return a <code>boolean</code> value
     */
    public boolean canCarryUnits() {
        return hasAbility(Ability.CARRY_UNITS);
    }

    /**
     * Is this unit able to carry a specified one?
     *
     * @param The potential cargo <code>Unit</code>.
     * @return True if this unit can carry the cargo unit.
     */
    public boolean canCarryUnit(Unit u) {
        return canCarryUnits() && getType().getSpace() >= u.getSpaceTaken();
    }

    /**
     * Returns <code>true</code> if the Unit can carry Goods.
     *
     * @return a <code>boolean</code> value
     */
    public boolean canCarryGoods() {
        return hasAbility(Ability.CARRY_GOODS);
    }

    /**
     * Returns a name for this unit, as a location.
     *
     * @return A name for this unit, as a location.
     */
    public StringTemplate getLocationName() {
        return StringTemplate.template("onBoard")
            .addStringTemplate("%unit%", getLabel());
    }

    /**
     * Returns the name for this unit, as a location, for a particular player.
     *
     * @param player The <code>Player</code> to prepare the name for.
     * @return A name for this unit, as a location.
     */
    public StringTemplate getLocationNameFor(Player player) {
        return getLocationName();
    }

    /**
     * Get the <code>UnitType</code> value.
     *
     * @return an <code>UnitType</code> value
     */
    public final UnitType getType() {
        return unitType;
    }

    /**
     * Returns the current amount of treasure in this unit. Should be type of
     * TREASURE_TRAIN.
     *
     * @return The amount of treasure.
     */
    public int getTreasureAmount() {
        if (canCarryTreasure()) {
            return treasureAmount;
        }
        throw new IllegalStateException("Unit can't carry treasure");
    }

    /**
     * The current amount of treasure in this unit. Should be type of
     * TREASURE_TRAIN.
     *
     * @param amt The amount of treasure
     */
    public void setTreasureAmount(int amt) {
        if (canCarryTreasure()) {
            this.treasureAmount = amt;
        } else {
            throw new IllegalStateException("Unit can't carry treasure");
        }
    }

    /**
     * Get the <code>Equipment</code> value.
     *
     * @return a <code>List<EquipmentType></code> value
     */
    public final TypeCountMap<EquipmentType> getEquipment() {
        return equipment;
    }

    /**
     * Set the <code>Equipment</code> value.
     *
     * @param newEquipment The new Equipment value.
     */
    public final void setEquipment(final TypeCountMap<EquipmentType> newEquipment) {
        this.equipment = newEquipment;
    }

    /**
     * Clears all <code>Equipment</code> held by this unit.
     */
    public void clearEquipment() {
        setEquipment(new TypeCountMap<EquipmentType>());
    }

    /**
     * Get the <code>TradeRoute</code> value.
     *
     * @return a <code>TradeRoute</code> value
     */
    public final TradeRoute getTradeRoute() {
        return tradeRoute;
    }

    /**
     * Set the <code>TradeRoute</code> value.
     *
     * @param newTradeRoute The new TradeRoute value.
     */
    public final void setTradeRoute(final TradeRoute newTradeRoute) {
        this.tradeRoute = newTradeRoute;
    }

    /**
     * Get the stop the unit is heading for or at.
     *
     * @return The target stop.
     */
    public Stop getStop() {
        return (validateCurrentStop() < 0) ? null
            : getTradeRoute().getStops().get(currentStop);
    }

    /**
     * Get the current stop.
     *
     * @return The current stop (an index in stops).
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
     * @return The current stop (an index in stops).
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
            return loc.getColony().isConnected();
        }
        // Otherwise, cash in if in Europe.
        return loc instanceof Europe
            || (loc instanceof Unit && ((Unit)loc).isInEurope());
    }

    /**
     * Return the fee that would have to be paid to transport this
     * treasure to Europe.
     *
     * @return an <code>int</code> value
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
     * @return <i>true</i> if this unit is a colonist and <i>false</i>
     *         otherwise.
     */
    public boolean isColonist() {
        return unitType.hasAbility("model.ability.foundColony")
            && owner.isEuropean();
    }

    /**
     * Checks if this is an offensive unit.  That is, one that can
     * attack other units.
     *
     * @return <code>true</code> if this is an offensive unit.
     */
    public boolean isOffensiveUnit() {
        return unitType.getOffence() > UnitType.DEFAULT_OFFENCE
            || isArmed() || isMounted();
    }

    /**
     * Gets the number of turns this unit has to train to educate a student.
     * This value is only meaningful for units that can be put in a school.
     *
     * @return The turns of training needed to teach its current type to a free
     *         colonist or to promote an indentured servant or a petty criminal.
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
     * @return The turns of training needed to teach its current type to a free
     *         colonist or to promote an indentured servant or a petty criminal.
     * @see #getTurnsOfTraining
     *
     * @param typeTeacher the unit type of the teacher
     * @param typeStudent the unit type of the student
     * @return an <code>int</code> value
     */
    public int getNeededTurnsOfTraining(UnitType typeTeacher, UnitType typeStudent) {
        UnitType teaching = getUnitTypeTeaching(typeTeacher, typeStudent);
        if (teaching != null) {
            return typeStudent.getEducationTurns(teaching);
        } else {
            throw new IllegalStateException("typeTeacher=" + typeTeacher + " typeStudent=" + typeStudent);
        }
    }

    /**
     * Gets the UnitType which a teacher is teaching to a student.
     * This value is only meaningful for teachers that can be put in a
     * school.
     *
     * @param typeTeacher the unit type of the teacher
     * @param typeStudent the unit type of the student
     * @return an <code>UnitType</code> value
     * @see #getTurnsOfTraining
     *
     */
    public static UnitType getUnitTypeTeaching(UnitType typeTeacher, UnitType typeStudent) {
        UnitType skillTaught = typeTeacher.getSkillTaught();
        if (typeStudent.canBeUpgraded(skillTaught, ChangeType.EDUCATION)) {
            return skillTaught;
        } else {
            return typeStudent.getEducationUnit(0);
        }
    }

    /**
     * Gets the skill level.
     *
     * @return The level of skill for this unit. A higher value signals a more
     *         advanced type of units.
     */
    public int getSkillLevel() {
        return getSkillLevel(unitType);
    }

    /**
     * Gets the skill level of the given type of <code>Unit</code>.
     *
     * @param unitType The type of <code>Unit</code>.
     * @return The level of skill for the given unit. A higher value signals a
     *         more advanced type of units.
     */
    public static int getSkillLevel(UnitType unitType) {
        if (unitType.hasSkill()) {
            return unitType.getSkill();
        }

        return 0;
    }

    /**
     * Returns a Comparator that compares the skill levels of given
     * units.
     *
     * @return skill Comparator
     */
    public static Comparator<Unit> getSkillLevelComparator() {
        return skillLevelComp;
    }

    /**
     * Gets the number of turns this unit has been training.
     *
     * @return The number of turns of training this <code>Unit</code> has
     *         given.
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
     *            <code>Unit</code> has given.
     * @see #getNeededTurnsOfTraining
     */
    public void setTurnsOfTraining(int turnsOfTraining) {
        this.turnsOfTraining = turnsOfTraining;
    }

    /**
     * Gets the experience of this <code>Unit</code> at its current
     * experienceType.
     *
     * @return The experience of this <code>Unit</code> at its current
     *         experienceType.
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
     *         at its current experienceType.
     * @see #modifyExperience
     */
    public void setExperience(int experience) {
        this.experience = Math.min(experience, getType().getMaximumExperience());
    }

    /**
     * Modifies the experience of this <code>Unit</code> at its current
     * experienceType.
     *
     * @param value The value by which to modify the experience of this
     *            <code>Unit</code>.
     * @see #getExperience
     */
    public void modifyExperience(int value) {
        experience += value;
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
     * Does this unit or its owner satisfy the ability set identified
     * by <code>id</code>.
     *
     * @param id The ability id to satisfy.
     * @return True if the ability is satisfied.
     */
    public boolean hasAbility(String id) {
        Set<Ability> result = new HashSet<Ability>();
        Turn turn = getGame().getTurn();
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
        if (getColony() != null) {
            result.addAll(getColony().getAbilitySet(id, unitType, turn));
        } else if (isInEurope()) {
            result.addAll(getOwner().getEurope().getAbilitySet(id, unitType, turn));
        }
        return FeatureContainer.hasAbility(result);
    }


    /**
     * Get a modifier that applies to this Unit.
     *
     * @param id a <code>String</code> value
     * @return a <code>Modifier</code> value
     */
    public Set<Modifier> getModifierSet(String id) {
        Set<Modifier> result = new HashSet<Modifier>();
        Turn turn = getGame().getTurn();
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
     * @param id a <code>String</code> value
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
     * Add the given Feature to the Features Map. If the Feature given
     * can not be combined with a Feature with the same ID already
     * present, the old Feature will be replaced.
     *
     * @param feature a <code>Feature</code> value
     */
    public void addFeature(Feature feature) {
        throw new UnsupportedOperationException("Can not add Feature to Unit directly!");
    }

    /**
     * Returns true if this unit can be a student.
     *
     * @param teacher the teacher which is trying to teach it
     * @return a <code>boolean</code> value
     */
    public boolean canBeStudent(Unit teacher) {
        return teacher != this && canBeStudent(unitType, teacher.unitType);
    }

    /**
     * Returns true if this type of unit can be a student.
     *
     * @param typeStudent the unit type of the student
     * @param typeTeacher the unit type of the teacher which is trying to teach it
     * @return a <code>boolean</code> value
     */
    public boolean canBeStudent(UnitType typeStudent, UnitType typeTeacher) {
        return getUnitTypeTeaching(typeTeacher, typeStudent) != null;
    }

    /**
     * Get the <code>Student</code> value.
     *
     * @return an <code>Unit</code> value
     */
    public final Unit getStudent() {
        return student;
    }

    /**
     * Set the <code>Student</code> value.
     *
     * @param newStudent The new Student value.
     */
    public final void setStudent(final Unit newStudent) {
        Unit oldStudent = this.student;
        if(oldStudent == newStudent){
            return;
        }

        if (newStudent == null) {
            this.student = null;
            if(oldStudent != null && oldStudent.getTeacher() == this){
                oldStudent.setTeacher(null);
            }
        } else if (newStudent.getColony() != null &&
                   newStudent.getColony() == getColony() &&
                   newStudent.canBeStudent(this)) {
            if(oldStudent != null && oldStudent.getTeacher() == this){
                oldStudent.setTeacher(null);
            }
            this.student = newStudent;
            newStudent.setTeacher(this);
        } else {
            throw new IllegalStateException("unit can not be student: " + newStudent);
        }
    }

    /**
     * Get the <code>Teacher</code> value.
     *
     * @return an <code>Unit</code> value
     */
    public final Unit getTeacher() {
        return teacher;
    }

    /**
     * Set the <code>Teacher</code> value.
     *
     * @param newTeacher The new Teacher value.
     */
    public final void setTeacher(final Unit newTeacher) {
        Unit oldTeacher = this.teacher;
        if(newTeacher == oldTeacher){
            return;
        }

        if (newTeacher == null) {
            this.teacher = null;
            if(oldTeacher != null && oldTeacher.getStudent() == this){
                oldTeacher.setStudent(null);
            }
        } else {
            UnitType skillTaught = newTeacher.getType().getSkillTaught();
            if (newTeacher.getColony() != null &&
                newTeacher.getColony() == getColony() &&
                getColony().canTrain(skillTaught)) {
                if(oldTeacher != null && oldTeacher.getStudent() == this){
                    oldTeacher.setStudent(null);
                }
                this.teacher = newTeacher;
                this.teacher.setStudent(this);
            } else {
                throw new IllegalStateException("unit can not be teacher: " + newTeacher);
            }
        }
    }

    /**
     * Gets a message to display if moving a unit would cause it to
     * abandon its participation in education (if any).
     *
     * @param checkStudent Should we check for student movements.
     * @return A message to display, or null if education is not an issue.
     */
    public StringTemplate getAbandonEducationMessage(boolean checkStudent) {
        if (!(getLocation() instanceof WorkLocation)) return null;
        boolean teacher = getStudent() != null;
        boolean student = checkStudent && getTeacher() != null;
        if (!teacher && !student) return null;

        Building school = (Building)((teacher) ? getLocation()
            : getTeacher().getLocation());
        String action = (teacher)
            ? Messages.message("abandonEducation.action.teaching")
            : Messages.message("abandonEducation.action.studying");
        return StringTemplate.template("abandonEducation.text")
                .addStringTemplate("%unit%", Messages.getLabel(this))
                .addName("%colony%", getColony().getName())
                .add("%building%", school.getNameKey())
                .addName("%action%", action);
    }

    /**
     * Gets the <code>Building</code> this unit is working in.
     * TODO: migrate usage of this to getWorkBuilding(), then delete this and rename the method below
     */
    public Building getWorkLocation() {
        if (getLocation() instanceof Building) {
            return ((Building) getLocation());
        }
        return null;
    }

    /**
     * Gets the <code>Location</code> this unit is working in.
     */
    public Location getWorkLocation2() {
        if (getLocation() instanceof Building) {
            return getLocation();
        }
        else if (getLocation() instanceof ColonyTile) {
            return getLocation();
        }
        return null;
    }

    /**
     * Gets the <code>Building</code> this unit is working in.
     */
    public Building getWorkBuilding() {
        if (getLocation() instanceof Building) {
            return ((Building) getLocation());
        }
        return null;
    }

    /**
     * Gets the <code>ColonyTile</code> this unit is working in.
     */
    public ColonyTile getWorkTile() {
        if (getLocation() instanceof ColonyTile) {
            return ((ColonyTile) getLocation());
        }
        return null;
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
     * Gets the type of goods this unit is producing in its current occupation.
     *
     * @return The type of goods this unit is producing.
     */
    public GoodsType getWorkType() {
        return workType;
    }

    /**
     * Sets the type of goods this unit is producing in its current occupation.
     *
     * @param type The type of goods to attempt to produce.
     */
    public void setWorkType(GoodsType type) {
        workType = type;
        if (type != null) {
            experienceType = type;
        }
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
     * Returns the destination of this unit.
     *
     * @return The destination of this unit.
     */
    public Location getDestination() {
        return destination;
    }

    /**
     * Sets the destination of this unit.
     *
     * @param newDestination The new destination of this unit.
     */
    public void setDestination(Location newDestination) {
        this.destination = newDestination;
    }

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
     * Finds a shortest path from the current <code>Tile</code> to the one
     * specified.  Only paths on water are allowed if <code>isNaval()</code>
     * and only paths on land if not.
     *
     * The <code>Tile</code> at the <code>end</code> will not be checked
     * against the legal moves of this <code>Unit</code>.
     *
     * @param end The <code>Tile</code> in which the path ends.
     * @return A <code>PathNode</code> for the first tile in the path.
     * @exception IllegalArgumentException if <code>end == null</code>
     */
    public PathNode findPath(Tile end) {
        if (getTile() == null) {
            logger.warning("getTile() == null for " + toString()
                + " at location: " + getLocation());
        }
        return this.findPath(getTile(), end);
    }

    /**
     * Finds a shortest path from the current <code>Tile</code> to the one
     * specified. Only paths on water are allowed if <code>isNaval()</code>
     * and only paths on land if not.
     *
     * @param start The <code>Tile</code> in which the path starts.
     * @param end The <code>Tile</code> in which the path ends.
     * @return A <code>PathNode</code> for the first tile in the path.
     */
    public PathNode findPath(Tile start, Tile end) {
        return this.findPath(start, end, null);
    }

    /**
     * Finds a shortest path from the current <code>Tile</code> to the one
     * specified. Only paths on water are allowed if <code>isNaval()</code>
     * and only paths on land if not.
     *
     * @param start The <code>Tile</code> in which the path starts.
     * @param end The <code>Tile</code> in which the path ends.
     * @param carrier An optional <code>Unit</code> to carry the unit.
     * @return A <code>PathNode</code> for the first tile in the path.
     */
    public PathNode findPath(Tile start, Tile end, Unit carrier) {
        return this.findPath(start, end, carrier, null);
    }

    /**
     * Finds a shortest path from the current <code>Tile</code> to the one
     * specified. Only paths on water are allowed if <code>isNaval()</code>
     * and only paths on land if not.
     *
     * @param start The <code>Tile</code> in which the path starts from.
     * @param end The <code>Tile</code> at the end of the path.
     * @param carrier An optional <code>Unit</code> to carry the unit.
     * @param costDecider An optional <code>CostDecider</code> for
     *        determining the movement costs (uses default cost deciders
     *        for the unit/s if not provided).
     * @return A <code>PathNode</code> for the first tile in the path.
     */
    public PathNode findPath(Tile start, Tile end, Unit carrier,
                             CostDecider costDecider) {
        Location dest = getDestination();
        setDestination(end);
        PathNode path = getGame().getMap().findPath(this, start, end,
                                                    carrier, costDecider);
        setDestination(dest);
        return path;
    }

    /**
     * Convenience wrapper to find a path to Europe for this unit.
     *
     * @return A path to Europe, or null if none found.
     */
    public PathNode findPathToEurope() {
        Location loc = getLocation();
        return (loc instanceof Tile) ? this.findPathToEurope((Tile)loc) : null;
    }

    /**
     * Convenience wrapper to find a path to Europe for this unit.
     *
     * @param start The <code>Tile</code> to start from.
     * @return A path to Europe, or null if none found.
     */
    public PathNode findPathToEurope(Tile start) {
        return getGame().getMap().findPathToEurope(this, start, null);
    }

    /**
     * Returns the number of turns this <code>Unit</code> will have to use in
     * order to reach the given <code>Tile</code>.
     *
     * @param start The <code>Tile</code> to start the search from.
     * @param end The <code>Tile</code> to be reached by this
     *            <code>Unit</code>.
     * @return The number of turns it will take to reach the <code>end</code>,
     *         or <code>INFINITY</code> if no path can be found.
     */
    public int getTurnsToReach(Tile start, Tile end) {
        if (start == end) return 0;

        PathNode p;
        if (isOnCarrier()) {
            Location dest = getDestination();
            setDestination(end);
            p = this.findPath(start, end, getCarrier());
            setDestination(dest);
        } else {
            p = this.findPath(start, end);
        }
        return (p != null) ? p.getTotalTurns() : INFINITY;
    }

    /**
     * Returns the number of turns this <code>Unit</code> will have to
     * use in order to reach the given <code>Location</code>.
     *
     * @param destination The destination for this unit.
     * @return The number of turns it will take to reach the destination,
     *         or <code>INFINITY</code> if no path can be found.
     */
    public int getTurnsToReach(Location destination) {
        if (destination == null) {
            logger.log(Level.WARNING, "destination == null", new Throwable());
        }

        Map map = getGame().getMap();
        boolean toEurope = destination instanceof Europe;
        Unit carrier = getCarrier();
        PathNode p;

        // Handle the special cases of travelling to and from Europe
        if (toEurope) {
            if (isInEurope()) {
                return 0;
            } else if (isNaval()) {
                p = this.findPathToEurope();
            } else if (carrier != null) {
                p = carrier.findPathToEurope();
            } else {
                return INFINITY;
            }
            return (p == null) ? INFINITY : p.getTotalTurns();
        }
        if (isInEurope()) {
            if (isNaval()) {
                p = this.findPath(getFullEntryLocation(),
                                  destination.getTile());
            } else {
                if (carrier == null) {
                    // Pick a carrier.  If none found the unit is stuck!
                    for (Unit u : getOwner().getUnits()) {
                        if (u.isNaval()) {
                            carrier = u;
                            break;
                        }
                    }
                    if (carrier == null) return INFINITY;
                }
                if (carrier.getFullEntryLocation().getTile()
                    == destination.getTile()) return carrier.getSailTurns();
                p = this.findPath(carrier.getFullEntryLocation(),
                                  destination.getTile(), carrier);
            }
            return (p == null) ? INFINITY
                : p.getTotalTurns() + carrier.getSailTurns();
        }
        if (isAtSea()) {
            if (isNaval()) {
                p = this.findPath(getFullEntryLocation(),
                                  destination.getTile());
                carrier = this;
            } else {
                if (carrier == null) return INFINITY;
                p = this.findPath(carrier.getFullEntryLocation(),
                                  destination.getTile(), carrier);
            }
            return (p == null) ? INFINITY : p.getTotalTurns()
                + carrier.getWorkLeft();
        }

        // Not in Europe, at sea, or going to Europe, so there must be
        // a well defined start and end tile.
        Tile start = (carrier == null) ? getTile() : carrier.getTile();
        return getTurnsToReach(start, destination.getTile());
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
                    if (t == startTile && excludeStart) return false;
                    Settlement settlement = t.getSettlement();
                    int value;
                    if (settlement != null
                        && settlement.getOwner() == player
                        && (!coastal || settlement.isConnected())
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
        if (isNaval()) {
            if (isInEurope() || isAtSea()) return null;
            ePath = findPathToEurope();
            eTurns = (ePath == null) ? -1
                : ePath.getTotalTurns() + getSailTurns();
        }
        PathNode sPath = findOurNearestSettlement(false, Integer.MAX_VALUE,
                                                  true);
        int sTurns = (sPath == null) ? -1 : sPath.getTotalTurns();
        return (sPath == null && ePath == null) ? null
            : (ePath == null || sTurns <= eTurns) ? sPath
            : ePath;
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
     * @param startTile The <code>Tile</code> to start the search from.
     * @param gd The object responsible for determining whether a
     *        given <code>PathNode</code> is a goal or not.
     * @param costDecider An optional <code>CostDecider</code>
     *        responsible for determining the path cost.
     * @param maxTurns The maximum number of turns the given
     *        <code>Unit</code> is allowed to move.
     * @param carrier The carrier the <code>unit</code> is currently
     *        onboard or <code>null</code> if the <code>unit</code> is
     *        either not onboard a carrier or should not use the
     *        carrier while finding the path.
     * @return The path to a goal determined by the given
     *        <code>GoalDecider</code>.
     */
    public PathNode search(Tile start, GoalDecider gd, CostDecider cd,
                           int maxTurns, Unit carrier) {
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
            .getMovement()) / this.getInitialMovesLeft();

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

        int cost = target.getType().getBasicMoveCost();
        if (target.isLand()) {
            TileItemContainer container = target.getTileItemContainer();
            if (container != null) {
                cost = container.getMoveCost(cost, from);
            }
        }

        if (isNaval() && from.isLand()
            && from.getSettlement() == null) {
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
        if (tile == null) {
            throw new IllegalStateException("getTile() == null, location is "
                                            + location);
        }

        Tile target = tile.getNeighbourOrNull(direction);
        return (target == null) ? MoveType.MOVE_ILLEGAL : getMoveType(target);
    }

    /**
     * Gets the type of a move that is made when moving from one tile
     * to another.
     *
     * @param target The target <code>Tile</code> of the move.
     * @return The move type.
     */
    public MoveType getMoveType(Tile target) {
        return getMoveType(getTile(), target, getMovesLeft());
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
        if (tile == null) {
            throw new IllegalStateException("Null tile");
        }
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
        if (tile == null) {
            throw new IllegalStateException("Null tile");
        }

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
        } else if (isUnderRepair()) {
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
            } else if (target.canMoveToEurope()) {
                return MoveType.MOVE_HIGH_SEAS;
            } else {
                return MoveType.MOVE;
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
        if (target == null) {
            return MoveType.MOVE_ILLEGAL;
        }

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
            for (Unit unit : target.getUnitList()) {
                if (unit.getSpaceLeft() >= getSpaceTaken()) {
                    return MoveType.EMBARK;
                }
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
                : (goodsContainer.getGoodsCount() > 0)
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
        return from.isLand() || getSpecification()
            .getBooleanOption("model.option.amphibiousMoves").getValue();
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
     * Returns the amount of moves this Unit has left.
     *
     * @return The amount of moves this Unit has left.
     */
    public int getMovesLeft() {
        return movesLeft;
    }

    /**
     * Sets the <code>movesLeft</code>.
     *
     * @param movesLeft The new amount of moves left this <code>Unit</code>
     *            should have. If <code>movesLeft < 0</code> then
     *            <code>movesLeft = 0</code>.
     */
    public void setMovesLeft(int movesLeft) {
        if (movesLeft < 0) {
            movesLeft = 0;
        }

        this.movesLeft = movesLeft;
    }

    /**
     * Gets the amount of space this <code>Unit</code> takes when put on a
     * carrier.
     *
     * @return The space this <code>Unit</code> takes.
     */
    public int getSpaceTaken() {
        int space = unitType.getSpaceTaken() + getGoodsCount();
        for (Unit u : units) space += u.getSpaceTaken();
        return space;
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
     * Sets the given state to all the units that si beeing carried.
     *
     * @param state The state.
     */
    public void setStateToAllChildren(UnitState state) {
        if (canCarryUnits()) {
            for (Unit u : getUnitList()) u.setState(state);
        }
    }

    /**
     * Set movesLeft to 0 if has some spent moves and it's in a colony
     *
     * @see #add(Locatable)
     * @see #remove(Locatable)
     */
    private void spendAllMoves() {
        if (getColony() != null && getMovesLeft() < getInitialMovesLeft())
            setMovesLeft(0);
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
            && !isUnderRepair()
            && !isAtSea()
            && !(isInEurope() && isOnCarrier())
            // this should never happen anyway, since these units
            // should have state IN_COLONY, but better safe than sorry
            && !(location instanceof WorkLocation);
    }

    /**
     * Finds the closest <code>Location</code> to this tile where
     * this ship can be repaired.
     *
     * @return The closest <code>Location</code> where a ship can be repaired.
     */
    public Location getRepairLocation() {
        Location closestLocation = null;
        int shortestDistance = INFINITY;
        Player player = getOwner();
        Tile tile = getTile();
        for (Colony colony : player.getColonies()) {
            if (colony == null || colony == tile.getColony()) continue;
            int distance;
            if (colony.hasAbility("model.ability.repairUnits")) {
                // Tile.getDistanceTo(Tile) doesn't care about
                // connectivity, so we need to check for an available
                // path to target colony instead
                PathNode pn = this.findPath(colony.getTile());
                if (pn != null
                    && (distance = pn.getTotalTurns()) < shortestDistance) {
                    closestLocation = colony;
                    shortestDistance = distance;
                }
            }
        }
        boolean connected = tile.isConnected()
            || (tile.getColony() != null && tile.getColony().isConnected());
        return (closestLocation != null) ? closestLocation.getTile()
            : (connected) ? player.getEurope()
            : null;
    }

    /**
     * Adds a locatable to this <code>Unit</code>.
     *
     * @param locatable The <code>Locatable</code> to add to this
     *            <code>Unit</code>.
     */
    public boolean add(Locatable locatable) {
        if (locatable instanceof Unit) {
            if (!canCarryUnits()) {
                throw new IllegalStateException("Can not carry units: "
                                                + this.toString());
            }
            Unit unit = (Unit) locatable;
            if (getSpaceLeft() < unit.getSpaceTaken()) {
                throw new IllegalStateException("Not enough space for "
                                                + unit.toString()
                                                + " on " + this.toString());
            }
            if (units.contains(locatable)) {
                logger.warning("Already on carrier: " + unit.toString());
                return true;
            }

            if (units.equals(Collections.emptyList())) {
                units = new ArrayList<Unit>();
            }
            spendAllMoves();
            unit.setState(UnitState.SENTRY);
            return units.add(unit);
        } else if (locatable instanceof Goods) {
            if (!canCarryGoods()) {
                throw new IllegalStateException("Can not carry goods: "
                                                + this.toString());
            }
            Goods goods = (Goods) locatable;
            if (getLoadableAmount(goods.getType()) < goods.getAmount()){
                throw new IllegalStateException("Not enough space for "
                                                + goods.toString()
                                                + " on " + this.toString());
            }
            spendAllMoves();
            return goodsContainer.addGoods(goods);
        } else {
            throw new IllegalStateException("Can not be added to unit: "
                                            + ((FreeColGameObject) locatable).toString());
        }
    }

    /**
     * Removes a <code>Locatable</code> from this <code>Unit</code>.
     *
     * @param locatable The <code>Locatable</code> to remove from this
     *            <code>Unit</code>.
     */
    public boolean remove(Locatable locatable) {
        if (locatable == null) {
            throw new IllegalArgumentException("Locatable must not be 'null'.");
        } else if (locatable instanceof Unit && canCarryUnits()) {
            spendAllMoves();
            return units.remove(locatable);
        } else if (locatable instanceof Goods && canCarryGoods()) {
            spendAllMoves();
            return goodsContainer.removeGoods((Goods) locatable) != null;
        } else {
            logger.warning("Tried to remove a 'Locatable' from a non-carrier unit.");
        }
        return false;
    }

    /**
     * Checks if this <code>Unit</code> contains the specified
     * <code>Locatable</code>.
     *
     * @param locatable The <code>Locatable</code> to test the presence of.
     * @return
     *            <ul>
     *            <li><i>true</i> if the specified <code>Locatable</code> is
     *            on this <code>Unit</code> and
     *            <li><i>false</i> otherwise.
     *            </ul>
     */
    public boolean contains(Locatable locatable) {
        if (locatable instanceof Unit && canCarryUnits()) {
            return units.contains(locatable);
        } else if (locatable instanceof Goods && canCarryGoods()) {
            return goodsContainer.contains((Goods) locatable);
        } else {
            return false;
        }
    }

    /**
     * Checks wether or not the specified locatable may be added to this
     * <code>Unit</code>. The locatable cannot be added is this
     * <code>Unit</code> if it is not a carrier or if there is no room left.
     *
     * @param locatable The <code>Locatable</code> to test the addabillity of.
     * @return The result.
     */
    public boolean canAdd(Locatable locatable) {
        if (locatable == this) {
            return false;
        } else if (locatable instanceof Unit && canCarryUnits()) {
            return getSpaceLeft() >= locatable.getSpaceTaken();
        } else if (locatable instanceof Goods) {
            Goods g = (Goods) locatable;
            return (getLoadableAmount(g.getType()) >= g.getAmount());
        } else {
            return false;
        }
    }

    /**
     * Returns the amount of a GoodsType that could be loaded.
     *
     * @param type a <code>GoodsType</code> value
     * @return an <code>int</code> value
     */
    public int getLoadableAmount(GoodsType type) {
        if (canCarryGoods()) {
            int result = getSpaceLeft() * GoodsContainer.CARGO_SIZE;
            int count = getGoodsContainer().getGoodsCount(type)
                % GoodsContainer.CARGO_SIZE;
            if (count > 0 && count < GoodsContainer.CARGO_SIZE) {
                result += GoodsContainer.CARGO_SIZE - count;
            }
            return result;
        } else {
            return 0;
        }
    }

    /**
     * Gets the amount of Units at this Location.
     *
     * @return The amount of Units at this Location.
     */
    public int getUnitCount() {
        return units.size();
    }

    /**
     * Gets the first <code>Unit</code> beeing carried by this
     * <code>Unit</code>.
     *
     * @return The <code>Unit</code>.
     */
    public Unit getFirstUnit() {
        if (units.isEmpty()) {
            return null;
        } else {
            return units.get(0);
        }
    }

    /**
     * Gets the last <code>Unit</code> beeing carried by this
     * <code>Unit</code>.
     *
     * @return The <code>Unit</code>.
     */
    public Unit getLastUnit() {
        if (units.isEmpty()) {
            return null;
        } else {
            return units.get(units.size() - 1);
        }
    }

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
            :  (!player.canSee(unitTile)) ? false
            : ((settlement = getSettlement()) != null
                && !player.owns(settlement)) ? false
            : (isOnCarrier() && !player.owns(getCarrier())) ? false
            : true;
    }

    /**
     * Gets a <code>Iterator</code> of every <code>Unit</code> directly
     * located on this <code>Location</code>.
     *
     * @return The <code>Iterator</code>.
     */
    public Iterator<Unit> getUnitIterator() {
        return new ArrayList<Unit>(units).iterator();
    }

    /**
     * Gets a list of the units in this carrier unit.
     *
     * @return The list of units in this carrier unit.
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
    public Iterator<Goods> getGoodsIterator() {
        if (canCarryGoods()) {
            return goodsContainer.getGoodsIterator();
        } else {
            return EmptyIterator.getInstance();
        }
    }

    /**
     * Returns a <code>List</code> containing the goods carried by this unit.
     * @return a <code>List</code> containing the goods carried by this unit.
     */
    public List<Goods> getGoodsList() {
        if (canCarryGoods()) {
            return goodsContainer.getGoods();
        } else {
            return Collections.emptyList();
        }
    }

    public GoodsContainer getGoodsContainer() {
        return goodsContainer;
    }

    /**
     * Sets the units location without updating any other variables
     *
     * @param newLocation The new Location
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
        Location oldLocation = location;

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
     * Sets the <code>IndianSettlement</code> that owns this unit.
     *
     * @param indianSettlement The <code>IndianSettlement</code> that should
     *            now be owning this <code>Unit</code>.
     */
    public void setIndianSettlement(IndianSettlement indianSettlement) {
        if (this.indianSettlement != null) {
            this.indianSettlement.removeOwnedUnit(this);
        }

        this.indianSettlement = indianSettlement;

        if (indianSettlement != null) {
            indianSettlement.addOwnedUnit(this);
        }
    }

    /**
     * Gets the <code>IndianSettlement</code> that owns this unit.
     *
     * @return The <code>IndianSettlement</code>.
     */
    public IndianSettlement getIndianSettlement() {
        return indianSettlement;
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
        for (Entry<String, Boolean> entry : equipmentType.getUnitAbilitiesRequired().entrySet()) {
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

    /**
     * Describe <code>getEquipmentCount</code> method here.
     *
     * @param equipmentType an <code>EquipmentType</code> value
     * @return an <code>int</code> value
     */
    public int getEquipmentCount(EquipmentType equipmentType) {
        return equipment.getCount(equipmentType);
    }

    /**
     * Checks if this <code>Unit</code> is located in Europe. That is; either
     * directly or onboard a carrier which is in Europe.
     *
     * @return The result.
     */
    public boolean isInEurope() {
        if (location instanceof Unit) {
            return ((Unit) location).isInEurope();
        } else {
            return getLocation() instanceof Europe;
        }
    }

    /**
     * Checks whether this <code>Unit</code> is at sea off the map, or
     * on board of a carrier that is.
     *
     * @return The result.
     */
    public boolean isAtSea() {
        if (location instanceof Unit) {
            return ((Unit) location).isAtSea();
        } else {
            return location instanceof HighSeas;
        }
    }

    /**
     * Checks if this <code>Unit</code> is able to carry {@link Locatable}s.
     *
     * @return 'true' if this unit can carry goods or other units,
     * 'false' otherwise.
     */
    public boolean isCarrier() {
        return unitType.canCarryGoods() ||
            unitType.canCarryUnits();
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
            || hasAbility(Ability.BORN_IN_COLONY)
            || hasAbility(Ability.BORN_IN_INDIAN_SETTLEMENT)
            || hasAbility("model.ability.foundColony")
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
     * Gets the owner of this Unit.
     *
     * @return The owner of this Unit.
     */
    public Player getOwner() {
        return owner;
    }

    /**
     * Get the name of the apparent owner of this Unit,
     * (like getOwner().getNationAsString() but handles pirates)
     *
     * @return The name of the owner of this Unit unless this is hidden.
     */
    public StringTemplate getApparentOwnerName() {
        return ((hasAbility(Ability.PIRACY))
                ? getGame().getUnknownEnemy()
                : owner).getNationName();
    }

    /**
     * Sets the owner of this Unit.
     *
     * @param owner The new owner of this Unit.
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
        owner.setUnit(this);
        if(getType() != null) {     // can be null if setOwner() is called from fixIntegrity()
            owner.modifyScore(getType().getScoreValue());
        }

        // for speed optimizations
        if(!isOnCarrier()) {
            getOwner().setExplored(this);
        }

        if (getGame().getFreeColGameObjectListener() != null) {
            getGame().getFreeColGameObjectListener().ownerChanged(this, oldOwner, owner);
        }
    }

    /**
     * Gets the nationality of this Unit.
     * Nationality represents a Unit's personal allegiance to a nation.
     * This may conflict with who currently issues orders to the Unit (the owner).
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
     * Ethnicity is inherited from the inhabitants of the place where the Unit was born.
     * Allows former converts to become native-looking colonists.
     *
     * @return The ethnicity of this Unit.
     */
    public String getEthnicity() {
        return ethnicity;
    }

    /**
     * Sets the ethnicity of this Unit.
     * Ethnicity is something units are born with. It cannot be subsequently changed.
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
            // FIXME: getNation() could fail, but getNationType() doesn't work as expected
            return getGame().getSpecification().getNation(ethnicity).getType().isIndian();
//          return getGame().getSpecification().getNationType(ethnicity).hasAbility("model.ability.native");
//          return getGame().getSpecification().getIndianNationTypes().contains(getNationType(ethnicity));
        } catch (Exception e) {
            return false;
        }
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
                owner.modifyScore(newUnitType.getScoreValue() - unitType.getScoreValue());
            }
            this.unitType = newUnitType;
            if (getMovesLeft() > getInitialMovesLeft()) {
                setMovesLeft(getInitialMovesLeft());
            }
            hitpoints = unitType.getHitPoints();
            if (getTeacher() != null && !canBeStudent(getTeacher())) {
                getTeacher().setStudent(null);
                setTeacher(null);
            }
        } else {
            // ColonialRegulars only available after independence is declared
            logger.warning("Units of type: " + newUnitType
                           + " are not available to " + owner.getPlayerType()
                           + " player " + owner.getName());
        }
    }


    // TODO: make these go away, if possible, private if not
    public boolean isArmed() {
        if(getOwner().isIndian()){
            return equipment.containsKey(getSpecification().getEquipmentType("model.equipment.indian.muskets"));
        }
        return equipment.containsKey(getSpecification().getEquipmentType("model.equipment.muskets"));
    }

    public boolean isMounted() {
        if(getOwner().isIndian()){
            return equipment.containsKey(getSpecification().getEquipmentType("model.equipment.indian.horses"));
        }
        return equipment.containsKey(getSpecification().getEquipmentType("model.equipment.horses"));
    }

    /**
     * Describe <code>getName</code> method here.
     *
     * @return a <code>String</code> value
     */
    public String getName() {
        return name;
    }

    /**
     * Set the <code>Name</code> value.
     *
     * @param newName The new Name value.
     */
    public void setName(String newName) {
        this.name = newName;
    }

    /**
     * Describe <code>getLabel</code> method here.
     *
     * @return a <code>StringTemplate</code> value
     */
    public StringTemplate getLabel() {
        StringTemplate result = StringTemplate.label(" ")
            .add(getType().getNameKey());
        if (name != null) {
            result.addName(name);
        }
        Role role = getRole();
        if (role != Role.DEFAULT) {
            result = StringTemplate.template("model.unit." + role.getId()
                                             + ".name")
                .addAmount("%number%", 1)
                .add("%unit%", getType().getNameKey());
        }
        return result;
    }


    /**
     * Return a description of the unit's equipment.
     *
     * @return a <code>String</code> value
     */
    public StringTemplate getEquipmentLabel() {
        if (equipment != null && !equipment.isEmpty()) {
            StringTemplate result = StringTemplate.label("/");
            for (java.util.Map.Entry<EquipmentType, Integer> entry : equipment.getValues().entrySet()) {
                EquipmentType type = entry.getKey();
                int amount = entry.getValue().intValue();
                if (type.getGoodsRequired().isEmpty()) {
                    result.addStringTemplate(StringTemplate.template("model.goods.goodsAmount")
                                             .add("%goods%", type.getNameKey())
                                             .addName("%amount%", Integer.toString(amount)));
                } else {
                    for (AbstractGoods goods : type.getGoodsRequired()) {
                        result.addStringTemplate(StringTemplate.template("model.goods.goodsAmount")
                                                 .add("%goods%", goods.getType().getNameKey())
                                                 .addName("%amount%", Integer.toString(amount * goods.getAmount())));
                    }
                }
            }
            return result;
        } else {
            return null;
        }
    }


    /**
     * Gets the amount of moves this unit has at the beginning of each turn.
     *
     * @return The amount of moves this unit has at the beginning of each turn.
     */
    public int getInitialMovesLeft() {
        return (int) FeatureContainer.applyModifierSet(unitType.getMovement(), getGame().getTurn(),
                                                       getModifierSet("model.modifier.movementBonus"));
    }

    /**
     * Sets the hitpoints for this unit.
     *
     * @param hitpoints The hitpoints this unit has. This is currently only used
     *            for damaged ships, but might get an extended use later.
     * @see UnitType#getHitPoints
     */
    public void setHitpoints(int hitpoints) {
        this.hitpoints = hitpoints;
        if (hitpoints >= unitType.getHitPoints()) {
            setState(UnitState.ACTIVE);
        }
    }

    /**
     * Returns the hitpoints.
     *
     * @return The hitpoints this unit has. This is currently only used for
     *         damaged ships, but might get an extended use later.
     * @see UnitType#getHitPoints
     */
    public int getHitpoints() {
        return hitpoints;
    }

    /**
     * Checks if this unit is under repair.
     *
     * @return <i>true</i> if under repair and <i>false</i> otherwise.
     */
    public boolean isUnderRepair() {
        return (hitpoints < unitType.getHitPoints());
    }

    /**
     * Checks if this unit is running a mission.
     *
     * @return True if this unit is running a mission.
     */
    public boolean isInMission() {
        return getRole() == Role.MISSIONARY
            && getTile() == null
            && !isOnCarrier();
    }

    public String getMovesAsString() {
        String moves = "";
        int quotient = getMovesLeft() / 3;
        int remainder = getMovesLeft() % 3;
        if (remainder == 0 || quotient > 0) {
            moves += Integer.toString(quotient);
        }

        if (remainder > 0) {
            if (quotient > 0) {
                moves += " ";
            }

            moves += "(" + Integer.toString(remainder) + "/3) ";
        }

        moves += "/" + Integer.toString(getInitialMovesLeft() / 3);
        return moves;
    }

    /**
     * Checks if this <code>Unit</code> is naval.
     *
     * @return <i>true</i> if this Unit is a naval Unit and <i>false</i>
     *         otherwise.
     */
    public boolean isNaval() {
        return getType().isNaval();
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
     * Gets the <code>Role</code> of this <code>Unit</code>.
     *
     * @return The <code>role</code> of this <code>Unit</code>.
     */
    public Role getRole() {
        return role;
    }

    /**
     * Determine role based on equipment.
     */
    protected void setRole() {
        Role oldRole = role;
        role = Role.DEFAULT;
        for (EquipmentType type : equipment.keySet()) {
            role = role.newRole(type.getRole());
        }
        if (getState() == UnitState.IMPROVING && role != Role.PIONEER) {
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
     * Checks if a <code>Unit</code> can get the given state set.
     *
     * @param s The new state for this Unit. Should be one of {UnitState.ACTIVE,
     *            FORTIFIED, ...}.
     * @return 'true' if the Unit's state can be changed to the new value,
     *         'false' otherwise.
     */
    public boolean checkSetState(UnitState s) {
        switch (s) {
        case ACTIVE:
        case SENTRY:
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
            return (getMovesLeft() > 0);
        default:
            logger.warning("Invalid unit state: " + s);
            return false;
        }
    }

    /**
     * Gets the number of turns this unit will need to sail to/from Europe.
     *
     * @return The number of turns to sail to/from Europe.
     */
    public int getSailTurns() {
        float base = getSpecification()
            .getIntegerOption("model.option.turnsToSail").getValue();
        return (int) getOwner().applyModifier(base,
                                              "model.modifier.sailHighSeas",
                                              unitType, getGame().getTurn());
    }


    /**
     * Sets a new state for this unit and initializes the amount of work the
     * unit has left.
     *
     * If the work needs turns to be completed (for instance when plowing), then
     * the moves the unit has still left will be used up. Some work (basically
     * building a road with a hardy pioneer) might actually be finished already
     * in this method-call, in which case the state is set back to UnitState.ACTIVE.
     *
     * @param s The new state for this Unit. Should be one of {UnitState.ACTIVE,
     *            UnitState.FORTIFIED, ...}.
     */
    public void setState(UnitState s) {
        if (state == s) {
            // No need to do anything when the state is unchanged
            return;
        } else if (!checkSetState(s)) {
            throw new IllegalStateException("Illegal UnitState transition: " + state + " -> " + s);
        } else {
            setStateUnchecked(s);
        }
    }

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
                    workImprovement.getTile().getTileItemContainer().removeTileItem(workImprovement);
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
     * Checks if this <code>Unit</code> can be moved to Europe.
     *
     * TODO: the new Carribean map has no south pole, and this allows
     * moving to Europe via the bottom edge of the map, which is
     * approximately the equator line.  Should we enforce moving to
     * Europe requires high seas, and no movement via north/south
     * poles?
     *
     * mpope 201103: Just leave it up to the map itself, tiles can
     * include a "moveToEurope" attribute to override the default
     * behaviour, which is to allow movement to Europe from tiles with
     * the moveToEurope ability or on the map borders.
     *
     * Now, IMHO on the Carribean map, settling on the land next to
     * the south border gives an unfair advantage and we *should* set
     * moveToEurope==false on the nearby sea tiles.
     *
     * @return <code>true</code> if this unit can move to Europe.
     */
    public boolean canMoveToEurope() {
        if (getLocation() instanceof Europe) return true;
        if (!getOwner().canMoveToEurope() || !isNaval()) return false;

        Tile tile = getTile();
        if (tile.canMoveToEurope()) return true;
        for (Tile t : tile.getSurroundingTiles(1)) {
            if (t.canMoveToEurope() && getMoveType(t).isLegal()) return true;
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
        return unitType.hasAbility("model.ability.foundColony")
            && getMovesLeft() > 0
            && getTile() != null;
    }

    /**
     * Returns the Tile where this Unit is located. Or null if its location is
     * Europe.
     *
     * @return The Tile where this Unit is located. Or null if its location is
     *         Europe.
     */
    public Tile getTile() {
        return (location != null) ? location.getTile() : null;
    }

    /**
     * Returns the amount of space left on this Unit.
     *
     * @return The amount of units/goods than can be moved onto this Unit.
     */
    public int getSpaceLeft() {
        int space = unitType.getSpace() - getGoodsCount();

        Iterator<Unit> unitIterator = getUnitIterator();
        while (unitIterator.hasNext()) {
            Unit u = unitIterator.next();
            space -= u.getSpaceTaken();
        }

        return space;
    }

    /**
     * Returns the amount of goods that is carried by this unit.
     *
     * @return The amount of goods carried by this <code>Unit</code>. This
     *         value might different from the one returned by
     *         {@link #getGoodsCount()} when the model is
     *         {@link Game#getViewOwner() owned by a client} and cargo hiding
     *         has been enabled.
     */
    public int getVisibleGoodsCount() {
        if (visibleGoodsCount >= 0) {
            return visibleGoodsCount;
        } else {
            return getGoodsCount();
        }
    }

    public int getGoodsCount() {
        return canCarryGoods() ? goodsContainer.getGoodsCount() : 0;
    }

    /**
     * Move the given unit to the front of this carrier (make sure it'll be the
     * first unit in this unit's unit list).
     *
     * @param u The unit to move to the front.
     */
    public void moveToFront(Unit u) {
        if (canCarryUnits() && units.remove(u)) {
            units.add(0, u);
        }
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
     * Sets the entry location in which this unit will be put when
     * returning from {@link Europe}.
     *
     * @param entryLocation The entry location.
     * @see #getEntryLocation
     */
    public void setEntryLocation(Location entryLocation) {
        this.entryLocation = entryLocation;
        if (entryLocation != null) {
            owner.setEntryLocation(entryLocation);
        }
    }

    /**
     * Gets the entry location for this unit to use when returning from
     * {@link Europe}.
     *
     * @return The entry location.
     */
    public Location getEntryLocation() {
        if (entryLocation == null) {
            entryLocation = owner.getEntryLocation();
        }
        return entryLocation;
    }

    /**
     * Gets the entry tile for this unit, or if null the default
     * entry location for the owning player.
     *
     * @return The entry tile.
     */
    public Tile getFullEntryLocation() {
        return (entryLocation != null) ? (Tile) entryLocation
            : (owner.getEntryLocation() == null) ? null
            : owner.getEntryLocation().getTile();
    }

    /**
     * Is the unit a beached ship?
     *
     * @param unit The <code>Unit</code> to test.
     * @return True if the unit is a beached ship.
     */
    public boolean isBeached() {
        return isNaval() && getTile() != null && getTile().isLand()
            && getSettlement() == null;
    }

    /**
     * Checks if this is an defensive unit. That is: a unit which can be used to
     * defend a <code>Settlement</code>.
     *
     * <br><br>
     *
     * Note! As this method is used by the AI it really means that the unit can
     * defend as is. To be specific an unarmed colonist is not defensive yet,
     * even if Paul Revere and stockpiled muskets are available. That check is
     * only performed on an actual attack.
     *
     * <br><br>
     *
     * A settlement is lost when there are no more defensive units.
     *
     * @return <code>true</code> if this is a defensive unit meaning it can be
     *         used to defend a <code>Colony</code>. This would normally mean
     *         that a defensive unit also will be
     *         {@link #isOffensiveUnit offensive}.
     */
    public boolean isDefensiveUnit() {
        return (unitType.getDefence() > UnitType.DEFAULT_DEFENCE || isArmed() || isMounted()) && !isNaval();
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
     * Checks if this unit is an undead.
     * @return return true if the unit is undead
     */
    public boolean isUndead() {
        return hasAbility("model.ability.undead");
    }

    /**
     * Returns true if this unit can carry treasure (like a treasure train)
     *
     * @return <code>true</code> if this <code>Unit</code> is capable of
     *         carrying treasure.
     */
    public boolean canCarryTreasure() {
        return unitType.hasAbility(Ability.CARRY_TREASURE);
    }

    /**
     * Returns true if this unit is a ship that can capture enemy goods.
     *
     * @return <code>true</code> if this <code>Unit</code> is capable of
     *         capturing goods.
     */
    public boolean canCaptureGoods() {
        return unitType.hasAbility(Ability.CAPTURE_GOODS);
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
     * Gets the Settlement this unit is in.
     *
     * @return The Settlement this unit is in, or null if none.
     */
    public Settlement getSettlement() {
        Location location = getLocation();
        return (location != null) ? location.getSettlement() : null;
    }

    /**
     * Gets the Colony this unit is in.
     *
     * @return The Colony this unit is in, or null if none.
     */
    public Colony getColony() {
        Location location = getLocation();
        return (location != null) ? location.getColony() : null;
    }

    /**
     * Given a type of goods to produce in the field and a tile,
     * returns the unit's potential to produce goods.
     *
     * @param goodsType The type of goods to be produced.
     * @param base an <code>int</code> value
     * @return The potential amount of goods to be farmed.
     */
    // TODO: do we need this?
    public int getProductionOf(GoodsType goodsType, int base) {
        if (base == 0) {
            return 0;
        } else {
            return Math.round(FeatureContainer.applyModifierSet(base, getGame().getTurn(),
                                                                getModifierSet(goodsType.getId())));
        }
    }

    /**
     * Removes all references to this object.
     *
     * @return A list of disposed objects.
     */
    public List<FreeColGameObject> disposeList() {
        List<FreeColGameObject> objects = new ArrayList<FreeColGameObject>();
        while (units.size() > 0) {
            objects.addAll(units.remove(0).disposeList());
        }

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

        setIndianSettlement(null);

        getOwner().invalidateCanSeeTiles();
        getOwner().removeUnit(this);

        if (unitType.canCarryGoods()) {
            objects.addAll(goodsContainer.disposeList());
        }

        objects.addAll(super.disposeList());
        return objects;
    }

    /**
     * Removes all references to this object.
     */
    public void dispose() {
        disposeList();
    }

    /**
     * Return how many turns left to be repaired
     *
     * @return turns to be repaired
     */
    public int getTurnsForRepair() {
        return unitType.getHitPoints() - getHitpoints();
    }

    /**
     * Gets the available equipment that can be equipped automatically
     * in case of an attack.
     *
     * @return The equipment that can be automatically equipped by
     *     this unit, or null if none.
     */
    public TypeCountMap<EquipmentType> getAutomaticEquipment(){
        // Paul Revere makes an unarmed colonist in a settlement pick up
        // a stock-piled musket if attacked, so the bonus should be applied
        // for unarmed colonists inside colonies where there are muskets
        // available. Indians can also pick up equipment.
        if(isArmed()){
            return null;
        }

        if(!getOwner().hasAbility("model.ability.automaticEquipment")){
            return null;
        }

        Settlement settlement = null;
        if (getLocation() instanceof WorkLocation) {
            settlement = getColony();
        }
        if (getLocation() instanceof IndianSettlement) {
            settlement = (Settlement) getLocation();
        }
        if(settlement == null){
            return null;
        }

        TypeCountMap<EquipmentType> equipmentList = null;

        // Check for necessary equipment in the settlement
        Set<Ability> autoDefence = new HashSet<Ability>();
        autoDefence.addAll(getOwner().getAbilitySet("model.ability.automaticEquipment"));

        for (EquipmentType equipment : getSpecification().getEquipmentTypeList()) {
            for (Ability ability : autoDefence) {
                if (!ability.appliesTo(equipment)){
                    continue;
                }
                if (!canBeEquippedWith(equipment)) {
                    continue;
                }

                boolean hasReqGoods = true;
                for(AbstractGoods goods : equipment.getGoodsRequired()){
                    if(settlement.getGoodsCount(goods.getType()) < goods.getAmount()){
                        hasReqGoods = false;
                        break;
                    }
                }
                if(hasReqGoods){
                    // lazy initialization, required
                    if(equipmentList == null){
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
     * Gets the probability that an attack by this unit will provoke a
     * native to convert.
     *
     * @return A probability of conversion.
     */
    public float getConvertProbability() {
        Specification spec = getSpecification();
        int opt = spec.getIntegerOption("model.option.nativeConvertProbability")
            .getValue();
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
        return 0.01f * getSpecification()
            .getIntegerOption("model.option.burnProbability").getValue();
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

    // Routines for message unpacking.

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
     * Get a settlement by id, validating as much as possible.
     * Designed for message unpacking where the id should not be trusted.
     *
     * @param settlementId The id of the <code>Settlement</code> to be found.
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
     * Get an adjacent Indian settlement by id, validating as much as
     * possible, including checking whether the nation involved has
     * been contacted.  Designed for message unpacking where the id
     * should not be trusted.
     *
     * @param id The id of the <code>IndianSettlement</code> to be found.
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
     * Returns a list of GoodsTypes this Consumer consumes.
     *
     * @return a <code>List</code> value
     */
    public List<AbstractGoods> getConsumedGoods() {
        return unitType.getConsumedGoods();
    }

    /**
     * Describe <code>getProductionInfo</code> method here.
     *
     * @return a <code>ProductionInfo</code> value
     */
    public ProductionInfo getProductionInfo(List<AbstractGoods> input) {
        ProductionInfo result = new ProductionInfo();
        result.setConsumption(getType().getConsumedGoods());
        result.setMaximumConsumption(getType().getConsumedGoods());
        return result;
    }

    /**
     * The priority of this Consumer. The higher the priority, the
     * earlier will the Consumer be allowed to consume the goods it
     * requires.
     *
     * @return an <code>int</code> value
     */
    public int getPriority() {
        return unitType.getPriority();
    }

    // Serialization

    private void unitsToXML(XMLStreamWriter out, Player player,
                            boolean showAll, boolean toSavedGame)
        throws XMLStreamException {
        if (!units.isEmpty()) {
            out.writeStartElement(UNITS_TAG_NAME);
            for (Unit unit : units) {
                unit.toXML(out, player, showAll, toSavedGame);
            }
            out.writeEndElement();
        }
    }

    /**
     * This method writes an XML-representation of this object to the given
     * stream.
     *
     * <br>
     * <br>
     *
     * Only attributes visible to the given <code>Player</code> will be added
     * to that representation if <code>showAll</code> is set to
     * <code>false</code>.
     *
     * @param out The target stream.
     * @param player The <code>Player</code> this XML-representation should be
     *            made for, or <code>null</code> if
     *            <code>showAll == true</code>.
     * @param showAll Only attributes visible to <code>player</code> will be
     *            added to the representation if <code>showAll</code> is set
     *            to <i>false</i>.
     * @param toSavedGame If <code>true</code> then information that is only
     *            needed when saving a game is added.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    protected void toXMLImpl(XMLStreamWriter out, Player player,
                             boolean showAll, boolean toSavedGame)
        throws XMLStreamException {
        boolean full = showAll || toSavedGame || player == getOwner();

        // Start element:
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute(ID_ATTRIBUTE, getId());
        if (name != null) {
            out.writeAttribute("name", name);
        }
        out.writeAttribute("unitType", unitType.getId());
        out.writeAttribute("movesLeft", Integer.toString(movesLeft));
        out.writeAttribute("state", state.toString());
        out.writeAttribute("role", role.toString());
        if (!full && hasAbility(Ability.PIRACY)) {
            // Pirates do not disclose national characteristics.
            out.writeAttribute("owner", getGame().getUnknownEnemy().getId());
        } else {
            out.writeAttribute("owner", getOwner().getId());
            if (isPerson()) {
                // Do not write out nationality and ethnicity for non-persons.
                out.writeAttribute("nationality",
                    (nationality != null) ? nationality
                    : getOwner().getNationID());
                out.writeAttribute("ethnicity",
                    (ethnicity != null) ? ethnicity
                    : getOwner().getNationID());
            }
        }
        out.writeAttribute("turnsOfTraining", Integer.toString(turnsOfTraining));
        if (workType != null) out.writeAttribute("workType", workType.getId());
        if (experienceType != null) out.writeAttribute("experienceType",
                                                       experienceType.getId());
        out.writeAttribute("experience", Integer.toString(experience));
        out.writeAttribute("treasureAmount", Integer.toString(treasureAmount));
        out.writeAttribute("hitpoints", Integer.toString(hitpoints));
        out.writeAttribute("attrition", Integer.toString(attrition));

        writeAttribute(out, "student", student);
        writeAttribute(out, "teacher", teacher);

        if (full) {
            writeAttribute(out, "indianSettlement", indianSettlement);
            out.writeAttribute("workLeft", Integer.toString(workLeft));
        } else {
            out.writeAttribute("workLeft", Integer.toString(-1));
        }

        if (entryLocation != null) {
            out.writeAttribute("entryLocation", entryLocation.getId());
        }

        if (location != null) {
            if (full || !(location instanceof Building
                          || location instanceof ColonyTile)) {
                out.writeAttribute("location", location.getId());
            } else {
                out.writeAttribute("location", getColony().getId());
            }
        }

        if (destination != null) {
            out.writeAttribute("destination", destination.getId());
        }
        if (tradeRoute != null) {
            out.writeAttribute("tradeRoute", tradeRoute.getId());
            out.writeAttribute("currentStop", String.valueOf(currentStop));
        }

        if (workImprovement != null) {
            workImprovement.toXML(out, player, showAll, toSavedGame);
        }

        // Do not show enemy units hidden in a carrier:
        if (full) {
            unitsToXML(out, player, showAll, toSavedGame);
            if (getType().canCarryGoods()) {
                goodsContainer.toXML(out, player, showAll, toSavedGame);
            }
        } else {
            if (getType().canCarryGoods()) {
                out.writeAttribute("visibleGoodsCount", Integer.toString(getGoodsCount()));
                goodsContainer.toXML(out, player, showAll, toSavedGame);
            }
        }

        if (!equipment.isEmpty()) {
            for (Entry<EquipmentType, Integer> entry : equipment.getValues().entrySet()) {
                out.writeStartElement(EQUIPMENT_TAG);
                out.writeAttribute(ID_ATTRIBUTE_TAG, entry.getKey().getId());
                out.writeAttribute("count", entry.getValue().toString());
                out.writeEndElement();
            }
        }

        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The input stream with the XML.
     * @throws javax.xml.stream.XMLStreamException is thrown if
     *     something goes wrong.
     */
    protected void readFromXMLImpl(XMLStreamReader in)
        throws XMLStreamException {
        Game game = getGame();
        setId(in.getAttributeValue(null, ID_ATTRIBUTE));
        setName(in.getAttributeValue(null, "name"));
        UnitType oldUnitType = unitType;
        unitType = getSpecification().getUnitType(in.getAttributeValue(null, "unitType"));

        movesLeft = Integer.parseInt(in.getAttributeValue(null, "movesLeft"));
        state = Enum.valueOf(UnitState.class, in.getAttributeValue(null, "state"));
        role = Enum.valueOf(Role.class, in.getAttributeValue(null, "role"));
        workLeft = Integer.parseInt(in.getAttributeValue(null, "workLeft"));
        attrition = getAttribute(in, "attrition", 0);

        owner = getFreeColGameObject(in, "owner", Player.class);

        nationality = in.getAttributeValue(null, "nationality");
        ethnicity = in.getAttributeValue(null, "ethnicity");

        if (oldUnitType == null) {
            owner.modifyScore(unitType.getScoreValue());
        } else {
            owner.modifyScore(unitType.getScoreValue() - oldUnitType.getScoreValue());
        }

        turnsOfTraining = Integer.parseInt(in.getAttributeValue(null, "turnsOfTraining"));
        hitpoints = Integer.parseInt(in.getAttributeValue(null, "hitpoints"));

        teacher = getFreeColGameObject(in, "teacher", Unit.class);

        student = getFreeColGameObject(in, "student", Unit.class);

        final String indianSettlementStr = in.getAttributeValue(null, "indianSettlement");
        if (indianSettlementStr != null) {
            indianSettlement = game.getFreeColGameObject(indianSettlementStr,
                                                         IndianSettlement.class);
            if (indianSettlement == null) {
                indianSettlement = new IndianSettlement(game, indianSettlementStr);
            }
        } else {
            setIndianSettlement(null);
        }

        treasureAmount = getAttribute(in, "treasureAmount", 0);

        destination = newLocation(in.getAttributeValue(null, "destination"));

        currentStop = -1;
        tradeRoute = null;
        final String tradeRouteStr = in.getAttributeValue(null, "tradeRoute");
        if (tradeRouteStr != null) {
            tradeRoute = game.getFreeColGameObject(tradeRouteStr,
                                                   TradeRoute.class);
            final String currentStopStr = in.getAttributeValue(null, "currentStop");
            if (currentStopStr != null) {
                currentStop = Integer.parseInt(currentStopStr);
            }
        }

        workType = getSpecification().getType(in, "workType", GoodsType.class, null);
        experienceType = getSpecification().getType(in, "experienceType", GoodsType.class, null);
        if (experienceType == null && workType != null) {
            experienceType = workType;
        }

        // @compat 0.9.x
        try {
            // this is likely to cause an exception, as the
            // specification might not define grain
            GoodsType grain = getSpecification().getGoodsType("model.goods.grain");
            GoodsType food = getSpecification().getPrimaryFoodType();
            if (food.equals(workType)) {
                workType = grain;
            }
            if (food.equals(experienceType)) {
                experienceType = grain;
            }
        } catch (Exception e) {
            logger.log(Level.FINEST, "Failed to update food to grain.", e);
        }
        // end compatibility code
        experience = getAttribute(in, "experience", 0);
        visibleGoodsCount = getAttribute(in, "visibleGoodsCount", -1);

        entryLocation = newLocation(in.getAttributeValue(null, "entryLocation"));

        location = newLocation(in.getAttributeValue(null, "location"));
        units.clear();
        if (goodsContainer != null) goodsContainer.removeAll();
        equipment.clear();
        setWorkImprovement(null);
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals(UNITS_TAG_NAME)) {
                units = new ArrayList<Unit>();
                while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
                    if (in.getLocalName().equals(Unit.getXMLElementTagName())) {
                        units.add(updateFreeColGameObject(in, Unit.class));
                    }
                }
            } else if (in.getLocalName().equals(GoodsContainer.getXMLElementTagName())) {
                goodsContainer = game.getFreeColGameObject(in.getAttributeValue(null, ID_ATTRIBUTE),
                    GoodsContainer.class);
                if (goodsContainer != null) {
                    goodsContainer.readFromXML(in);
                } else {
                    goodsContainer = new GoodsContainer(game, this, in);
                }
            } else if (in.getLocalName().equals(EQUIPMENT_TAG)) {
                String xLength = in.getAttributeValue(null, ARRAY_SIZE);
                if (xLength == null) {
                    String equipmentId = in.getAttributeValue(null, ID_ATTRIBUTE_TAG);
                    int count = Integer.parseInt(in.getAttributeValue(null, "count"));
                    equipment.incrementCount(getSpecification().getEquipmentType(equipmentId), count);
                } else { // @compat 0.9.x
                    int length = Integer.parseInt(xLength);
                    for (int index = 0; index < length; index++) {
                        String equipmentId = in.getAttributeValue(null, "x" + String.valueOf(index));
                        equipment.incrementCount(getSpecification().getEquipmentType(equipmentId), 1);
                    }
                } // end compatibility code
                in.nextTag();
            } else if (in.getLocalName().equals(TileImprovement.getXMLElementTagName())) {
                setWorkImprovement(updateFreeColGameObject(in, TileImprovement.class));
            } else {
                logger.warning("Found unknown child element '" + in.getLocalName() + "' of Unit " + getId() + ", skipping to next tag.");
                in.nextTag();
            }
        }

        // ensure all carriers have a goods container, just in case
        if (goodsContainer == null && getType().canCarryGoods()) {
            logger.warning("Carrier with ID " + getId() + " did not have a \"goodsContainer\"-tag.");
            goodsContainer = new GoodsContainer(game, this);
        }

        setRole();
        getOwner().setUnit(this);
        getOwner().invalidateCanSeeTiles();
    }

    /**
     * Partial writer for units, so that "remove" messages can be brief.
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
     * Partial reader for units, so that "remove" messages can be brief.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException If there are problems reading the stream.
     */
    @Override
    protected void readFromXMLPartialImpl(XMLStreamReader in)
        throws XMLStreamException {
        readFromXMLPartialByClass(in, getClass());
    }

    /**
     * Gets a string representation of this unit.
     *
     * @return A string representation of this <code>Unit</code>.
     */
    public String toString() {
        return "[" + getId() + " " + getType().getId()
            + ((getRole() == Role.DEFAULT) ? "" : "-" + getRole())
            + " " + owner.getNationID()
            + " " + getMovesAsString() + "]";
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "unit"
     */
    public static String getXMLElementTagName() {
        return "unit";
    }
}
