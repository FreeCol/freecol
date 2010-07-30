/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Colony.ColonyChangeEvent;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.model.TradeRoute.Stop;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;
import net.sf.freecol.common.util.EmptyIterator;
import net.sf.freecol.common.util.Utils;

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
public class Unit extends FreeColGameObject implements Locatable, Location, Ownable, Nameable {
    private static Comparator<Unit> skillLevelComp  = null;
    
    private static final Logger logger = Logger.getLogger(Unit.class.getName());

    /**
     * XML tag name for equipment list.
     */
    private static final String EQUIPMENT_TAG = "equipment";

    private static final String UNITS_TAG_NAME = "units";

    public static final String CARGO_CHANGE = "CARGO_CHANGE";
    public static final String EQUIPMENT_CHANGE = "EQUIPMENT_CHANGE";
    
    /**
     * A state a Unit can have.
     */
    public static enum UnitState { ACTIVE, FORTIFIED, SENTRY, IN_COLONY, IMPROVING,
            TO_EUROPE, TO_AMERICA, FORTIFYING, SKIPPED }

    /** The roles a Unit can have. */
    public static enum Role {
        DEFAULT, PIONEER, MISSIONARY, SOLDIER, SCOUT, DRAGOON;
    
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
        ATTACK(null, false),
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
    }

    private UnitType unitType;

    private boolean naval;

    private int movesLeft;

    private UnitState state = UnitState.ACTIVE;

    private Role role = Role.DEFAULT;

    /** 
     * The number of turns until the work is finished, or '-1' if a
     * Unit can stay in its state forever.
     */
    private int workLeft;

    private int hitpoints; // For now; only used by ships when repairing.

    private Player owner;

    private List<Unit> units = Collections.emptyList();

    private GoodsContainer goodsContainer;

    private Location entryLocation;

    private Location location;

    private IndianSettlement indianSettlement = null; // only used by BRAVE.

    private Location destination = null;

    private TradeRoute tradeRoute = null; // only used by carriers

    // Unit is going towards current stop's location
    private int currentStop = -1;

    // to be used only for type == TREASURE_TRAIN
    private int treasureAmount;

    // to be used only for PIONEERs - where they are working
    private TileImprovement workImprovement;

    // What type of goods this unit produces in its occupation.
    private GoodsType workType;

    private int experience = 0;

    private int turnsOfTraining = 0;

    /**
     * The attrition this unit has accumulated. At the moment, this
     * equals the number of turns it has spent in the open.
     */
    private int attrition = 0;

    /**
     * The individual name of this unit, not of the unit type.
     */
    private String name = null;

    /**
     * The amount of goods carried by this unit. This variable is only used by
     * the clients. A negative value signals that the variable is not in use.
     * 
     * @see #getVisibleGoodsCount()
     */
    private int visibleGoodsCount;

    /**
     * The student of this Unit, if it has one.
     */
    private Unit student;

    /**
     * The teacher of this Unit, if it has one.
     */
    private Unit teacher;

    /**
     * The equipment this Unit carries.
     */
    private TypeCountMap<EquipmentType> equipment = new TypeCountMap<EquipmentType>();


    /**
     * Initiate a new <code>Unit</code> of a specified type with the state set
     * to {@link UnitState#ACTIVE} if a carrier and {@link UnitState#SENTRY} otherwise. The
     * {@link Location} is set to <i>null</i>.
     * 
     * @param game The <code>Game</code> in which this <code>Unit</code>
     *            belong.
     * @param owner The Player owning the unit.
     * @param type The type of the unit.
     */
    public Unit(Game game, Player owner, UnitType type) {
        this(game, null, owner, type, UnitState.ACTIVE);
    }

    /**
     * Initiate a new <code>Unit</code> with the specified parameters.
     * 
     * @param game The <code>Game</code> in which this <code>Unit</code>
     *            belong.
     * @param location The <code>Location</code> to place this
     *            <code>Unit</code> upon.
     * @param owner The <code>Player</code> owning this unit.
     * @param type The type of the unit.
     * @param state The initial state for this Unit (one of {@link UnitState#ACTIVE},
     *            {@link UnitState#FORTIFIED}...).
     */
    public Unit(Game game, Location location, Player owner, UnitType type, UnitState state) {
        this(game, location, owner, type, state, type.getDefaultEquipment());
    }

    /**
     * Initiate a new <code>Unit</code> with the specified parameters.
     * 
     * @param game The <code>Game</code> in which this <code>Unit</code>
     *            belong.
     * @param location The <code>Location</code> to place this
     *            <code>Unit</code> upon.
     * @param owner The <code>Player</code> owning this unit.
     * @param type The type of the unit.
     * @param state The initial state for this Unit (one of {@link UnitState#ACTIVE},
     *            {@link UnitState#FORTIFIED}...).
     * @param initialEquipment The list of initial EquimentTypes
     */
    public Unit(Game game, Location location, Player owner, UnitType type, UnitState state, 
                EquipmentType... initialEquipment) {
        super(game);

        visibleGoodsCount = -1;

        if (type.canCarryGoods()) {
            goodsContainer = new GoodsContainer(game, this);
        }

        UnitType newType = type.getUnitTypeChange(ChangeType.CREATION, owner);
        if (newType == null) {
            unitType = type;
        } else {
            unitType = newType;
        }
        this.owner = owner;
        owner.getNationID();
        naval = unitType.hasAbility("model.ability.navalUnit");
        setLocation(location);

        workLeft = -1;
        workType = getSpecification().getGoodsFood().get(0);

        this.movesLeft = getInitialMovesLeft();
        hitpoints = unitType.getHitPoints();

        for (EquipmentType equipmentType : initialEquipment) {
            if (EquipmentType.NO_EQUIPMENT.equals(equipmentType)) {
                equipment.clear();
                break;
            } else {
                equipment.incrementCount(equipmentType, 1);
            }
        }
        setRole();
        setStateUnchecked(state);

        getOwner().setUnit(this);
        getOwner().invalidateCanSeeTiles();
        getOwner().modifyScore(type.getScoreValue());
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
        return hasAbility("model.ability.carryUnits");
    }

    /**
     * Returns <code>true</code> if the Unit can carry Goods.
     *
     * @return a <code>boolean</code> value
     */
    public boolean canCarryGoods() {
        return hasAbility("model.ability.carryGoods");
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
        if (newTradeRoute != null) {
            ArrayList<Stop> stops = newTradeRoute.getStops();
            if (stops.size() > 0) {
                setDestination(newTradeRoute.getStops().get(0).getLocation());
                currentStop = 0;
            }
        }
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
            ArrayList<Stop> stops = tradeRoute.getStops();
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
            || (loc instanceof Unit && ((Unit) loc).getLocation() instanceof Europe);
    }

    /**
     * Return the fee that would have to be paid to transport this
     * treasure to Europe.
     *
     * @return an <code>int</code> value
     */
    public int getTransportFee() {
        if (canCashInTreasureTrain()) {
            if (!isInEurope() && getOwner().getEurope() != null) {
                return (int) getOwner().getFeatureContainer()
                    .applyModifier(getTreasureAmount() / 2f,
                                   "model.modifier.treasureTransportFee",
                                   unitType, getGame().getTurn());
            }
        }
        return 0;
    }

    /**
     * Checks if this is a trading <code>Unit</code>, meaning that it
     * can trade with settlements, and that it has something to trade.
     *
     * @return True if this is a trading unit.
     */
    public boolean isTradingUnit() {
        return canCarryGoods() && goodsContainer.getGoodsCount() > 0
            && owner.isEuropean();
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
    
    public static Comparator<Unit> getSkillLevelComparator(){
        if(skillLevelComp != null){
            return skillLevelComp;
        }
        
        // Create comparator to sort units by skill level
        // Prefer unit with less qualifications
        skillLevelComp = new Comparator<Unit>(){
            public int compare(Unit u1,Unit u2){
                if(u1.getSkillLevel() < u2.getSkillLevel()){
                    return -1;
                }
                if(u1.getSkillLevel() > u2.getSkillLevel()){
                    return 1;
                }
                return 0;
            }
        };
        
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
     * Gets the experience of this <code>Unit</code> at its current workType.
     * 
     * @return The experience of this <code>Unit</code> at its current
     *         workType.
     * @see #modifyExperience
     */
    public int getExperience() {
        return experience;
    }

    /**
     * Modifies the experience of this <code>Unit</code> at its current
     * workType.
     * 
     * @param value The value by which to modify the experience of this
     *            <code>Unit</code>.
     * @see #getExperience
     */
    public void modifyExperience(int value) {
        experience += value;
    }

    /**
     * Returns true if the Unit, or its owner has the ability
     * identified by <code>id</code>.
     *
     * @param id a <code>String</code> value
     * @return a <code>boolean</code> value
     */
    public boolean hasAbility(String id) {
        Set<Ability> result = new HashSet<Ability>();
        // UnitType abilities always apply
        result.addAll(unitType.getFeatureContainer().getAbilitySet(id));
        // the player's abilities may not apply
        result.addAll(getOwner().getFeatureContainer()
                      .getAbilitySet(id, unitType, getGame().getTurn()));
        // EquipmentType abilities always apply
        for (EquipmentType equipmentType : equipment.keySet()) {
            result.addAll(equipmentType.getFeatureContainer().getAbilitySet(id));
            // player abilities may also apply to equipment (missionary) 
            result.addAll(getOwner().getFeatureContainer()
                          .getAbilitySet(id, equipmentType, getGame().getTurn()));
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
        // UnitType modifiers always apply
        result.addAll(unitType.getFeatureContainer().getModifierSet(id));
        // the player's modifiers may not apply
        result.addAll(getOwner().getFeatureContainer()
                      .getModifierSet(id, unitType, getGame().getTurn()));
        // EquipmentType modifiers always apply
        for (EquipmentType equipmentType : equipment.keySet()) {
            result.addAll(equipmentType.getFeatureContainer().getModifierSet(id));
            // player modifiers may also apply to equipment (unused) 
            result.addAll(getOwner().getFeatureContainer()
                          .getModifierSet(id, equipmentType, getGame().getTurn()));
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
        return canBeStudent(unitType, teacher.unitType);
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
        return workType;
    }

    /**
     * Gets the type of goods this unit is producing in its current occupation.
     * 
     * @return The type of goods this unit would produce.
     */
    public GoodsType getWorkType() {
        if (getLocation() instanceof Building) {
            return ((Building) getLocation()).getGoodsOutputType();
        }
        return workType;
    }

    /**
     * Sets the type of goods this unit is producing in its current occupation.
     * 
     * @param type The type of goods to attempt to produce.
     */
    public void setWorkType(GoodsType type) {
        if (type == null) {
            throw new IllegalStateException("GoodsType must not be 'null'.");
        } else if (workType != type) {
            logger.finest("resetting experience for " + this);
            experience = 0;
            if (type.isFarmed()) {
                GoodsType oldWorkType = workType;
                workType = type;
                if (getLocation() instanceof ColonyTile) {
                    ColonyTile colonyTile = (ColonyTile) getLocation();
                    if (oldWorkType != null) {
                        colonyTile.firePropertyChange(oldWorkType.getId(), 
                                                      colonyTile.getProductionOf(this, oldWorkType), null);
                    }
                    colonyTile.firePropertyChange(type.getId(), 
                                                  null, colonyTile.getProductionOf(this, type));
                }
            }
        }
    }

    /**
     * Gets the TileImprovement that this pioneer is contributing to.
     * @return The <code>TileImprovement</code>
     */
    public TileImprovement getWorkImprovement() {
        return workImprovement;
    }

    /**
     * Sets the TileImprovement that this pioneer is contributing to.
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
     * Finds a shortest path from the current <code>Tile</code> to the one
     * specified. Only paths on water are allowed if <code>isNaval()</code>
     * and only paths on land if not.
     * 
     * <br>
     * <br>
     * 
     * The <code>Tile</code> at the <code>end</code> will not be checked
     * against the legal moves of this <code>Unit</code>.
     * 
     * @param end The <code>Tile</code> in which the path ends.
     * @return A <code>PathNode</code> for the first tile in the path. Calling
     *         {@link PathNode#getTile} on this object, will return the
     *         <code>Tile</code> right after the specified starting tile, and
     *         {@link PathNode#getDirection} will return the direction you need
     *         to take in order to reach that tile. This method returns
     *         <code>null</code> if no path is found.
     * @see Map#findPath(Tile, Tile, PathType)
     * @see Map#findPath(Unit, Tile , Tile)
     * @exception IllegalArgumentException if <code>end == null</code>
     */
    public PathNode findPath(Tile end) {
        if (getTile() == null) {
            logger.warning("getTile() == null for " + toString() + " at location: " + getLocation());
        }
        return findPath(getTile(), end);
    }

    /**
     * Finds a shortest path from the current <code>Tile</code> to the one
     * specified. Only paths on water are allowed if <code>isNaval()</code>
     * and only paths on land if not.
     * 
     * @param start The <code>Tile</code> in which the path starts.
     * @param end The <code>Tile</code> in which the path ends.
     * @return A <code>PathNode</code> for the first tile in the path.
     * @see #findPath(Tile)
     */
    public PathNode findPath(Tile start, Tile end) {
        Location dest = getDestination();
        setDestination(end);
        PathNode path = getGame().getMap().findPath(this, start, end);
        setDestination(dest);
        return path;
    }

    /**
     * Returns the number of turns this <code>Unit</code> will have to use in
     * order to reach the given <code>Tile</code>.
     * 
     * @param end The <code>Tile</code> to be reached by this
     *            <code>Unit</code>.
     * @return The number of turns it will take to reach the <code>end</code>,
     *         or <code>INFINITY</code> if no path can be found.
     */
    public int getTurnsToReach(Tile end) {
        return getTurnsToReach(getTile(), end);
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

        if (start == end) {
            return 0;
        }

        if (isOnCarrier()) {
            Location dest = getDestination();
            setDestination(end);
            PathNode p = getGame().getMap().findPath(this, start, end, (Unit) getLocation());
            setDestination(dest);
            if (p != null) {
                return p.getTotalTurns();
            }
        }
        PathNode p = findPath(start, end);
        if (p != null) {
            return p.getTotalTurns();
        }

        return INFINITY;
    }
    
    /**
     * Returns the number of turns this <code>Unit</code> will have to use in
     * order to reach the given <code>Location</code>.
     * 
     * @param destination The destination for this unit.
     * @return The number of turns it will take to reach the <code>destination</code>,
     *         or <code>INFINITY</code> if no path can be found.
     */
    public int getTurnsToReach(Location destination) {
        if (destination == null) {
            logger.log(Level.WARNING, "destination == null", new Throwable());
        }
        
        if (getTile() == null) {
            if (destination.getTile() == null) {
                return 0;
            }
            final PathNode p;
            if (isOnCarrier()) {
                final Unit carrier = (Unit) getLocation();
                p = getGame().getMap().findPath(this, (Tile) carrier.getEntryLocation(), destination.getTile(), carrier);
            } else {
                // TODO: Use a standard carrier with four move points as a the unit's carrier:
                p = getGame().getMap().findPath((Tile) getOwner().getEntryLocation(), destination.getTile(), 
                                                Map.PathType.BOTH_LAND_AND_SEA);
            }
            if (p != null) {
                return p.getTotalTurns();
            } else {
                return INFINITY;
            }
        }
        
        if (destination.getTile() == null) {
            // TODO: Find a path (and determine distance) to Europe:
            return 10;
        }
        
        return getTurnsToReach(destination.getTile());
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
     * @see Tile#getMoveCost
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
     * @see Tile#getMoveCost
     */
    public int getMoveCost(Tile from, Tile target, int ml) {
        // Remember to also change map.findPath(...) if you change anything
        // here.

        int cost = target.getMoveCost(from);

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
            throw new IllegalStateException("getTile() == null");
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
        return getMoveType(from, target, ml, false);
    }

    /**
     * Gets the type of a move that is made when moving from one tile
     * to another.
     * 
     * @param from The origin <code>Tile</code> of the move.
     * @param target The target <code>Tile</code> of the move.
     * @param ml The amount of moves this unit has left.
     * @param ignoreEnemyUnits Should be <code>true</code> if enemy
     *      units should be ignored when determining the move type.
     * @return The move type.
     */
    public MoveType getMoveType(Tile from, Tile target, int ml, boolean ignoreEnemyUnits) {
        MoveType move = getSimpleMoveType(from, target, ignoreEnemyUnits);

        if (move.isLegal()) {
            switch(move) {
                //case DISEMBARK:
                // doesn't really move and may ignore movement points
                // break;
            case ATTACK:
                // needs only a single movement point, regardless of
                // terrain, but suffers penalty
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
     * @param ignoreEnemyUnits Should be <code>true</code> if enemy
     *      units should be ignored when determining the move type.
     * @return The move type, which will be one of the extended illegal move
     *         types on failure.
     */
    public MoveType getSimpleMoveType(Tile from, Tile target, boolean ignoreEnemyUnits) {
        return (isNaval())
            ? getNavalMoveType(from, target, ignoreEnemyUnits)
            : getLandMoveType(from, target, ignoreEnemyUnits);
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
        return getSimpleMoveType(tile, target, false);
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
        return getSimpleMoveType(tile, target, false);
    }

    /**
     * Gets the type of a move that is made when moving a naval unit
     * from one tile to another.
     *
     * @param from The origin <code>Tile<code> of the move.
     * @param target The target <code>Tile</code> of the move.
     * @param ignoreEnemyUnits Should be <code>true</code> if enemy
     *      units should be ignored when determining the move type.
     * @return The move type.
     */
    private MoveType getNavalMoveType(Tile from, Tile target,
                                      boolean ignoreEnemyUnits) {
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
            if (defender != null && !ignoreEnemyUnits
                && defender.getOwner() != getOwner()) {
                return (isOffensiveUnit()) ? MoveType.ATTACK
                    : MoveType.MOVE_NO_ATTACK_CIVILIAN;
            } else if (target.canMoveToEurope() && getOwner().canMoveToEurope()) {
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
     * @param ignoreEnemyUnits Should be <code>true</code> if enemy
     *      units should be ignored when determining the move type.
     * @return The move type.
     */
    private MoveType getLandMoveType(Tile from, Tile target,
                                     boolean ignoreEnemyUnits) {
        if (target == null) {
            return MoveType.MOVE_ILLEGAL;
        }

        Player owner = getOwner();
        Unit defender = target.getFirstUnit();

        if (target.isLand()) {
            Settlement settlement = target.getSettlement();
            if (settlement == null) {
                if (defender != null && owner != defender.getOwner()
                    && !ignoreEnemyUnits) {
                    if (defender.isNaval()) {
                        return MoveType.MOVE_NO_ACCESS_BEACHED;
                    } else if (!isOffensiveUnit()) {
                        return MoveType.MOVE_NO_ATTACK_CIVILIAN;
                    } else {
                        return (allowMoveFrom(from))
                            ? MoveType.ATTACK
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
                        ? MoveType.ATTACK
                        : MoveType.MOVE_NO_ATTACK_MARINE;
                }
                return MoveType.MOVE_ILLEGAL; // should not happen
            } else if (isOffensiveUnit()) {
                return MoveType.ATTACK;
            } else {
                return MoveType.MOVE_NO_ACCESS_SETTLEMENT;
            }
        } else { // moving to sea, check for embarkation
            if (defender == null || defender.getOwner() != getOwner()) {
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
        if (getOwner().atWarWith(settlement.getOwner())) {
            return MoveType.MOVE_NO_ACCESS_WAR;
        } else if (settlement instanceof Colony) {
            return (hasAbility("model.ability.tradeWithForeignColonies"))
                ? MoveType.ENTER_SETTLEMENT_WITH_CARRIER_AND_GOODS
                : MoveType.MOVE_NO_ACCESS_TRADE;
        } else if (settlement instanceof IndianSettlement) {
            return (allowContact(settlement))
                ? MoveType.ENTER_SETTLEMENT_WITH_CARRIER_AND_GOODS
                : MoveType.MOVE_NO_ACCESS_CONTACT;
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
            if (getType().canBeUpgraded(scoutSkill, ChangeType.NATIVES)) {
                return (allowMoveFrom(from))
                    ? MoveType.ENTER_INDIAN_SETTLEMENT_WITH_FREE_COLONIST
                    : MoveType.MOVE_NO_ACCESS_WATER;
            } else {
                return MoveType.MOVE_NO_ACCESS_SKILL;
            }
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
            if (allowContact(settlement)) {
                return (allowMoveFrom(from))
                    ? MoveType.ENTER_INDIAN_SETTLEMENT_WITH_MISSIONARY
                    : MoveType.MOVE_NO_ACCESS_WATER;
            } else {
                return MoveType.MOVE_NO_ACCESS_CONTACT;
            }
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
            return MoveType.ENTER_FOREIGN_COLONY_WITH_SCOUT;
        } else if (settlement instanceof IndianSettlement) {
            return (allowMoveFrom(from))
                ? MoveType.ENTER_INDIAN_SETTLEMENT_WITH_SCOUT
                : MoveType.MOVE_NO_ACCESS_WATER;
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
        return from.isLand();
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
        return unitType.getSpaceTaken();
    }

    /**
     * Gets the line of sight of this <code>Unit</code>. That is the distance
     * this <code>Unit</code> can spot new tiles, enemy unit e.t.c.
     * 
     * @return The line of sight of this <code>Unit</code>.
     */
    public int getLineOfSight() {
        float line = unitType.getLineOfSight();
        Set<Modifier> modifierSet = getModifierSet("model.modifier.lineOfSightBonus");
        if (getTile() != null && getTile().getType() != null) {
            modifierSet.addAll(getTile().getType().getFeatureContainer()
                               .getModifierSet("model.modifier.lineOfSightBonus",
                                               unitType, getGame().getTurn()));
        }
        return (int) FeatureContainer.applyModifierSet(line, getGame().getTurn(), modifierSet);
    }

    /**
     * Verifies if the unit is aboard a carrier
     */
    public boolean isOnCarrier(){
        return(this.getLocation() instanceof Unit);
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
     * Adds a locatable to this <code>Unit</code>.
     * 
     * @param locatable The <code>Locatable</code> to add to this
     *            <code>Unit</code>.
     */
    public void add(Locatable locatable) {
        if (locatable instanceof Unit && canCarryUnits()) {
            Unit unit = (Unit) locatable;
            if (getSpaceLeft() < unit.getSpaceTaken()) {
                throw new IllegalStateException("Not enough space for " + unit.toString()
                                                + " left on " + toString());
            }
            if (units.contains(locatable)) {
                logger.warning("Tried to add a 'Locatable' already in the carrier.");
                return;
            }
            
            if (units.equals(Collections.emptyList())) {
                units = new ArrayList<Unit>();
            } 
            units.add(unit);
            unit.setState(UnitState.SENTRY);
            firePropertyChange(CARGO_CHANGE, null, locatable);
            spendAllMoves();
        } else if (locatable instanceof Goods && canCarryGoods()) {
            Goods goods = (Goods) locatable;
            if (getLoadableAmount(goods.getType()) < goods.getAmount()){
                throw new IllegalStateException("Not enough space for " + goods.toString()
                                                + " left on " + toString());
            }
            goodsContainer.addGoods(goods);
            firePropertyChange(CARGO_CHANGE, null, locatable);
            spendAllMoves();
        } else {
            throw new IllegalStateException("Tried to add a 'Locatable' to a non-carrier unit.");
        }
    }

    /**
     * Removes a <code>Locatable</code> from this <code>Unit</code>.
     * 
     * @param locatable The <code>Locatable</code> to remove from this
     *            <code>Unit</code>.
     */
    public void remove(Locatable locatable) {
        if (locatable == null) {
            throw new IllegalArgumentException("Locatable must not be 'null'.");
        } else if (locatable instanceof Unit && canCarryUnits()) {
            units.remove(locatable);
            firePropertyChange(CARGO_CHANGE, locatable, null);
            spendAllMoves();
        } else if (locatable instanceof Goods && canCarryGoods()) {
            goodsContainer.removeGoods((Goods) locatable);
            firePropertyChange(CARGO_CHANGE, locatable, null);
            spendAllMoves();
        } else {
            logger.warning("Tried to remove a 'Locatable' from a non-carrier unit.");
        }
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
            int result = getSpaceLeft() * 100;
            int count = getGoodsContainer().getGoodsCount(type) % 100;
            if (count > 0 && count < 100) {
                result += (100 - count);
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
        if(player == getOwner()){
            return true;
        }
        
        Tile unitTile = getTile();
        if(unitTile == null){
            return false;
        }
        
        if(!player.canSee(unitTile)){
            return false;
        }
        
        Settlement settlement = unitTile.getSettlement();
        if(settlement != null && settlement.getOwner() != player){
            return false;
        }

        if(isOnCarrier() && ((Unit) getLocation()).getOwner() != player){
            return false;
        }
        
        return true;
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

    public List<Unit> getUnitList() {
        return units;
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
     * Sets this <code>Unit</code> to work in the specified
     * <code>WorkLocation</code>.
     * 
     * @param workLocation The place where this <code>Unit</code> shall be out
     *            to work.
     * @exception IllegalStateException If the <code>workLocation</code> is in
     *                another {@link Colony} than this <code>Unit</code>.
     * Deprecated: should move to the server.  WorkMessage is implemented
     *     but there are still lots of uses, mostly in the AI.
     */
    public void work(WorkLocation workLocation) {
        // Colonies will be the same if the unit is either on the tile
        // of the colony or already working in one of the colonies
        // WorkLocations.
        if (workLocation.getColony() != this.getColony()) {
            throw new IllegalStateException("Can only set a 'Unit'  to a 'WorkLocation' that is in the same 'Colony'.");
        }
        if (workLocation.getTile().getOwner() != getOwner()) {
            throw new IllegalStateException("Can only set a 'Unit' to a 'WorkLocation' owned by the same player.");
        }
        setState(UnitState.IN_COLONY);
        setLocation(workLocation);
    }

    /**
     * Sets this unit to work at this TileImprovement.
     *
     * @param improvement The <code>TileImprovement</code> to work on.
     * @exception IllegalStateException If the
     *     <code>TileImprovement</code> is on another {@link Tile}
     *     than this <code>Unit</code> or is not a valid pioneer.
     */
    public void work(TileImprovement improvement) {
        if (!hasAbility("model.ability.improveTerrain")) {
            throw new IllegalStateException("Only 'Pioneers' can perform TileImprovement.");
        } else if (improvement == null) {
            // TODO: Check whether and why improvement can be null
            // here - possible bug in caller?
            throw new IllegalArgumentException("Improvement must not be 'null'.");
        } else {
            TileImprovementType impType = improvement.getType();
            // Is this a valid ImprovementType?
            if (impType == null) {
                throw new IllegalArgumentException("ImprovementType must not be 'null'.");
            } else if (impType.isNatural()) {
                throw new IllegalArgumentException("ImprovementType must not be natural.");
            } else if (!impType.isTileTypeAllowed(getTile().getType())) {
                // Check if improvement can be performed on this TileType
                throw new IllegalArgumentException(impType + " not allowed on "
                                                   + getTile().getType());
            } else {
                // TODO: This does not check if the tile (not TileType
                // accepts the improvement).  Check if there is an
                // existing Improvement of this type
                TileImprovement oldImprovement = getTile().findTileImprovementType(impType);
                if (oldImprovement == null) {
                    // No improvement found, check if worker can do it
                    if (!impType.isWorkerAllowed(this)) {
                        throw new IllegalArgumentException(toString() + " not allowed to perform "
                                                           + improvement.toString());
                    }
                } else {
                    // Has improvement, check if worker can contribute to it
                    if (!oldImprovement.isWorkerAllowed(this)) {
                        throw new IllegalArgumentException(toString() + " not allowed to perform "
                                                           + improvement.toString());
                    }
                }
            }
        }
        
        setWorkImprovement(improvement);
        setState(UnitState.IMPROVING);
        // No need to set Location, stay at the tile it is on.
    }
    
    /**
     * Sets the units location without updating any other variables
     * @param newLocation The new Location
     */
    public void setLocationNoUpdate(Location newLocation) {
        location = newLocation;
    }

    /**
     * Sets the location of this Unit.
     * 
     * @param newLocation The new Location of the Unit.
     */
    public void setLocation(Location newLocation) {

        Colony oldColony = this.getColony();
        Location oldLocation = location;
        
        if (location != null) {
            location.remove(this);
        }
        location = newLocation;
        if (newLocation != null) {
            newLocation.add(this);
        }

        // Units in WorkLocations get counted twice
        if (oldLocation instanceof WorkLocation) {
            if (!(newLocation instanceof WorkLocation)) {
                getOwner().modifyScore(-getType().getScoreValue());
                if (oldColony != null) {
                    // this should always be the case, except possibly for unit tests
                    oldColony.updatePopulation(-1);
                    setState(UnitState.ACTIVE);
                }
            }
        } else if (newLocation instanceof WorkLocation) {
            // entering colony
            UnitType newType = unitType.getUnitTypeChange(ChangeType.ENTER_COLONY, owner);
            if (newType == null) {
                getOwner().modifyScore(getType().getScoreValue());
            } else {
                Colony colony = newLocation.getColony();
                String oldName = unitType.getId() + ".name";
                getOwner().modifyScore(-getType().getScoreValue());
                setType(newType);
                getOwner().modifyScore(getType().getScoreValue() * 2);
                String newName = newType.getId() + ".name";
                colony.firePropertyChange(ColonyChangeEvent.UNIT_TYPE_CHANGE.toString(),
                                          oldName, newName);
            }
            newLocation.getColony().updatePopulation(1);
            if (getState() != UnitState.IN_COLONY) {
                logger.warning("Adding unit " + getId() + " with state==" + getState()
                               + " (should be IN_COLONY) to WorkLocation in "
                               + newLocation.getColony().getName() + ". Fixing: ");
                setState(UnitState.IN_COLONY);
            }
        }
                
        // Reset training when changing/leaving colony
        if (!Utils.equals(oldColony, getColony())){
            setTurnsOfTraining(0);
        }

        if (student != null &&
            !(newLocation instanceof Building &&
              ((Building) newLocation).getType().hasAbility("model.ability.teach"))) {
            // teacher has left school
            student.setTeacher(null);
            student = null;
        }

        if (newLocation instanceof WorkLocation) {
            removeAllEquipment(false);
        } else if (teacher != null) {
            teacher.setStudent(null);
            teacher = null;
        }

        if (!getOwner().isIndian()) {
            getOwner().setExplored(this);
        }
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
     * Puts this <code>Unit</code> outside the {@link Colony} by moving it to
     * the {@link Tile} below.
     */
    public void putOutsideColony() {
        if (getTile().getSettlement() == null) {
            throw new IllegalStateException();
        }

        if (getState() == UnitState.IN_COLONY) {
            setState(UnitState.ACTIVE);
        }

        setLocation(getTile());
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
        if (!equipmentType.getLocationAbilitiesRequired().isEmpty()) {
            if (isInEurope()) {
                return true;
            } else {
                Colony colony = getColony();
                if (colony == null) {
                    return false;
                } else {
                    for (Entry<String, Boolean> entry : equipmentType.getLocationAbilitiesRequired().entrySet()) {
                        if (colony.getFeatureContainer().hasAbility(entry.getKey()) != entry.getValue()) {
                            return false;
                        }
                    }
                }
            }
        }
        if (equipment.getCount(equipmentType) >= equipmentType.getMaximumCount()) {
            return false;
        }
        return true;
    }

    /**
     * Describe <code>equipWith</code> method here.
     *
     * @param equipmentType an <code>EquipmentType</code> value
     */
    public void equipWith(EquipmentType equipmentType) {
        equipWith(equipmentType, 1, false);
    }

    /**
     * Describe <code>equipWith</code> method here.
     *
     * @param equipmentType an <code>EquipmentType</code> value
     * @param amount an <code>int</code> value
     */
    public void equipWith(EquipmentType equipmentType, int amount) {
        equipWith(equipmentType, amount, false);
    }

    /**
     * Describe <code>equipWith</code> method here.
     *
     * @param equipmentType an <code>EquipmentType</code> value
     * @param asResultOfCombat a <code>boolean</code> value
     */
    public void equipWith(EquipmentType equipmentType, boolean asResultOfCombat) {
        equipWith(equipmentType, 1, asResultOfCombat);
    }

    /**
     * Equip this unit with the given EquipmentType, provided that all
     * requirements are met, and that the EquipmentType can be built
     * at this location or is present as a result of combat.
     *
     * @param equipmentType an <code>EquipmentType</code> value
     * @param amount an <code>int</code> value
     * @param asResultOfCombat a <code>boolean</code> value
     * @see #canBeEquippedWith
     */
    public void equipWith(EquipmentType equipmentType, int amount, boolean asResultOfCombat) {
        if (equipmentType == null) {
            throw new IllegalArgumentException("EquipmentType is 'null'.");
        } else if (amount < 1) {
            throw new IllegalArgumentException("Amount must be a positive integer.");
        }
        if (!canBeEquippedWith(equipmentType)) {
            logger.fine("Unable to equip unit " + getId() + " with " + equipmentType);
            return;
        }
        if (!(asResultOfCombat || 
              (getColony() != null && getColony().canBuildEquipment(equipmentType)) ||
              (isInEurope() && getOwner().getEurope().canBuildEquipment(equipmentType)) ||
              (getIndianSettlement() != null))) {
            logger.fine("Unable to build equipment " + equipmentType);
            return;
        }
        if (!asResultOfCombat) {
            setMovesLeft(0);
            if (getColony() != null) {
                for (AbstractGoods goods : equipmentType.getGoodsRequired()) {
                    int requiredAmount = amount * goods.getAmount();
                    if(getColony().getGoodsCount(goods.getType()) < requiredAmount){
                        throw new IllegalStateException("Not enough goods to equip");
                    }
                    getColony().removeGoods(goods.getType(), requiredAmount);
                }
            } else if (isInEurope()) {
                for (AbstractGoods goods : equipmentType.getGoodsRequired()) {
                    int requiredAmount = amount * goods.getAmount();
                    getOwner().getMarket().buy(goods.getType(), requiredAmount, getOwner());
                }
            } else if(getIndianSettlement() != null) {
                for (AbstractGoods goods : equipmentType.getGoodsRequired()) {                    
                    int requiredAmount = amount * goods.getAmount();
                    if(getIndianSettlement().getGoodsCount(goods.getType()) < requiredAmount){
                        throw new IllegalStateException("Not enough goods to equip");
                    }
                    getIndianSettlement().removeGoods(goods.getType(), requiredAmount);
                }
            }
        }
        equipment.incrementCount(equipmentType, amount);
        Set<EquipmentType> equipmentTypes = equipment.keySet();
        // We are changing the set, so we need to create a copy for iteration, to avoid
        //a ConcurrentModificationException being thrown
        Set<EquipmentType> eqLst = new HashSet<EquipmentType>(equipmentTypes);
        for (EquipmentType oldEquipment : eqLst) {
            if (!oldEquipment.isCompatibleWith(equipmentType)) {
                dumpEquipment(oldEquipment, equipment.getCount(oldEquipment), asResultOfCombat);
                equipmentTypes.remove(oldEquipment);
            }
        }
        setRole();
        firePropertyChange(Unit.EQUIPMENT_CHANGE, null, null);
    }
    
    public void removeEquipment(EquipmentType equipmentType) {
        int amount = getEquipmentCount(equipmentType);
        
        removeEquipment(equipmentType, amount, false);
    }

    /**
     * Describe <code>removeEquipment</code> method here.
     *
     * @param equipmentType an <code>EquipmentType</code> value
     * @param amount an <code>int</code> value
     */
    public void removeEquipment(EquipmentType equipmentType, int amount) {
        removeEquipment(equipmentType, amount, false);
    }

    /**
     * Describe <code>removeEquipment</code> method here.
     *
     * @param equipmentType an <code>EquipmentType</code> value
     * @param amount an <code>int</code> value
     * @param asResultOfCombat a <code>boolean</code> value
     */
    public void removeEquipment(EquipmentType equipmentType, int amount, boolean asResultOfCombat) {
        dumpEquipment(equipmentType, amount, asResultOfCombat);
        equipment.incrementCount(equipmentType, -amount);
        if (asResultOfCombat) {
            // loss of horses reduces movement
            setMovesLeft(Math.min(movesLeft, getInitialMovesLeft()));
        } else {
            setMovesLeft(0);
        }
        setRole();
        firePropertyChange(Unit.EQUIPMENT_CHANGE, null, null);
    }

    public void removeAllEquipment(boolean asResultOfCombat) {
        for (EquipmentType equipmentType : equipment.keySet()) {
            dumpEquipment(equipmentType, equipment.getCount(equipmentType), asResultOfCombat);
        }
        equipment.clear();
        setMovesLeft(0);
        setRole();
        firePropertyChange(Unit.EQUIPMENT_CHANGE, null, null);
    }

    private void dumpEquipment(EquipmentType equipmentType, int amount, boolean asResultOfCombat) {
        if (!asResultOfCombat) {
            // the equipment is returned to storage in the form of goods
            if (getColony() != null) {
                for (AbstractGoods goods : equipmentType.getGoodsRequired()) {
                    getColony().addGoods(goods.getType(), amount * goods.getAmount());
                }
            } else if (isInEurope()) {
                for (AbstractGoods goods : equipmentType.getGoodsRequired()) {
                    getOwner().getMarket().sell(goods.getType(), amount * goods.getAmount(), getOwner());
                }
            }
        }
        // else in case of a lost battle, the equipment is just destroyed
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
     * Switches equipment between colonists
     */
    public void switchEquipmentWith(Unit unit){
        if(!isColonist() || !unit.isColonist()){
            throw new IllegalArgumentException("Both units need to be colonists to switch equipment");
        }
        
        if(getTile() != unit.getTile()){
            throw new IllegalStateException("Units can only switch equipment in the same location");
        }
        
        if(getTile().getSettlement() == null){
            throw new IllegalStateException("Units can only switch equipment in a settlement");
        }
        
        List<EquipmentType> equipList = new ArrayList<EquipmentType>(getEquipment().keySet());
        List<EquipmentType> otherEquipList = new ArrayList<EquipmentType>(unit.getEquipment().keySet());
        removeAllEquipment(false);
        unit.removeAllEquipment(false);
        for(EquipmentType equip : otherEquipList){
            equipWith(equip);
        }
        for(EquipmentType equip : equipList){
            unit.equipWith(equip);
        }
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
        }
        
        if(!(getLocation() instanceof Europe)){
            return false;
        }
        
        if(isBetweenEuropeAndNewWorld()){
            return false;
        }
        return true;
    }
    
    /**
     * Checks if this <code>Unit</code> is either a carrier or on one, bound to/from Europe
     * 
     * @return The result.
     */
    public boolean isBetweenEuropeAndNewWorld() {
        if (location instanceof Unit) {
            return ((Unit) location).isBetweenEuropeAndNewWorld();
        }
        
        if(!(getLocation() instanceof Europe)){
            return false;
        }
        
        if(getState() == UnitState.TO_EUROPE 
                || getState() == UnitState.TO_AMERICA){
            return true;
        }
        return false;
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
        return (hasAbility("model.ability.piracy"))
            ? StringTemplate.name(Player.UNKNOWN_ENEMY)
            : owner.getNationName();
    }

    /**
     * Sets the owner of this Unit.
     * 
     * @param owner The new owner of this Unit.
     */
    public void setOwner(Player owner) {
        Player oldOwner = this.owner;
        
        // safeguard
        if(oldOwner == owner){
            return;
        }
        
        if(oldOwner == null){
            logger.warning("Unit " + getId() + " had no previous owner");
        }

        // This need to be set right away
        this.owner = owner;
        // If its a carrier, we need to update the units it has loaded 
        //before finishing with it
        for (Unit unit : getUnitList()) {
            unit.setOwner(owner);
        }
                
        if(oldOwner != null){
            oldOwner.removeUnit(this);
            oldOwner.modifyScore(-getType().getScoreValue());
            // for speed optimizations
            if(!isOnCarrier()){
                oldOwner.invalidateCanSeeTiles();
            }
        }
        owner.setUnit(this);
        owner.modifyScore(getType().getScoreValue());

        // for speed optimizations
        if(!isOnCarrier()){
            getOwner().setExplored(this);
        }

        if (getGame().getFreeColGameObjectListener() != null) {
            getGame().getFreeColGameObjectListener().ownerChanged(this, oldOwner, owner);
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
            naval = unitType.hasAbility("model.ability.navalUnit");
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
     * Returns a String representation of this Unit.
     * 
     * @return A String representation of this Unit.
     */
    public String toString() {
        return getId() + " [" + getType().getId() + " " + getMovesAsString() +"] "
            + owner.getNationID() + " (" + getRole() + ")";
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
        return naval;
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
    private void setRole() {
        Role oldRole = role;
        role = Role.DEFAULT;
        for (EquipmentType type : equipment.keySet()) {
            switch (type.getRole()) {
            case SOLDIER:
                if (role == Role.SCOUT) {
                    role = Role.DRAGOON;
                } else {
                    role = Role.SOLDIER;
                }
                break;
            case SCOUT:
                if (role == Role.SOLDIER) {
                    role = Role.DRAGOON;
                } else {
                    role = Role.SCOUT;
                }
                break;
            default:
                role = type.getRole();
            }
        }
        if (getState() == UnitState.IMPROVING && role != Role.PIONEER) {
            setStateUnchecked(UnitState.ACTIVE);
            setMovesLeft(0);
        }
        
        //Check for role change for reseting the experience
        // Soldier and Dragoon are compatible, no loss of experience
        boolean keepExperience = (role == oldRole) || 
                                 (role == Role.SOLDIER && oldRole == Role.DRAGOON) ||
                                 (role == Role.DRAGOON && oldRole == Role.SOLDIER);
        
        if(!keepExperience){
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
            if (location instanceof Tile
                && location.getTile().claimable(getOwner())) {
                return getMovesLeft() > 0;
            }
            return false;
        case FORTIFYING:
        case SKIPPED:
            return (getMovesLeft() > 0);
        case TO_EUROPE:
            return isNaval() &&
                ((location instanceof Europe) && (getState() == UnitState.TO_AMERICA)) ||
                (getEntryLocation() == getLocation());
        case TO_AMERICA:
            return (location instanceof Europe && isNaval() && !isUnderRepair());
        default:
            logger.warning("Invalid unit state: " + s);
            return false;
        }
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

    private void setStateUnchecked(UnitState s) {
        // Cleanup the old UnitState, for example destroy the
        // TileImprovment being built by a pioneer.
        switch (state) {
        case IMPROVING: 
            if (workLeft > 0) {
                if (!workImprovement.isComplete()) {
                    workImprovement.getTile().getTileItemContainer().removeTileItem(workImprovement);
                }
                workImprovement = null;
            }
            break;
        default:
            // do nothing
            break;
        }

        // Now initiate the new UnitState
        switch (s) {
        case ACTIVE:
            workLeft = -1;
            break;
        case SENTRY:
            workLeft = -1;
            break;
        case FORTIFIED:
            workLeft = -1;
            movesLeft = 0;
            break;
        case FORTIFYING:
            movesLeft = 0;
            workLeft = 1;
            break;
        case IMPROVING:
            movesLeft = 0;
            workLeft = -1;
            if (workImprovement != null) {
                workLeft = workImprovement.getTurnsToComplete();
            }
            state = s;
            doAssignedWork();
            return;
        case TO_EUROPE:
            workLeft = getSpecification().getIntegerOption("model.option.turnsToSail").getValue();
            if (state == UnitState.TO_AMERICA) {
                workLeft += 1 - workLeft;
            }
            workLeft = (int) getOwner().getFeatureContainer().applyModifier(workLeft,
                "model.modifier.sailHighSeas", unitType, getGame().getTurn());
            movesLeft = 0;
            break;
        case TO_AMERICA:
            workLeft = getSpecification().getIntegerOption("model.option.turnsToSail").getValue();
            if (state == UnitState.TO_EUROPE) {
                workLeft += 1 - workLeft;
            }
            workLeft = (int) getOwner().getFeatureContainer().applyModifier(workLeft,
                "model.modifier.sailHighSeas", unitType, getGame().getTurn());
            movesLeft = 0;
            break;
        case SKIPPED:
            // do nothing
            break;
        default:
            workLeft = -1;
        }
        state = s;
    }

    /**
     * Checks if this <code>Unit</code> can be moved to Europe.
     * 
     * @return <code>true</code> if this unit can move to Europe.
     */
    public boolean canMoveToEurope() {
        if (getLocation() instanceof Europe) {
            return true;
        }
        if (!getOwner().canMoveToEurope()) {
            return false;
        }


        List<Tile> surroundingTiles = new ArrayList<Tile>();
        for (Tile t: getTile().getSurroundingTiles(1))
            surroundingTiles.add(t);

        if (surroundingTiles.size() != 8) {
            // TODO: the new carribean map has no south pole, and this allows moving to europe
            // via the bottom edge of the map, which is approximately the equator line. 
            // Should we enforce moving to europe requires high seas, and no movement via north/south poles?
            return true;
        } else {
            for (Tile tile: getTile().getSurroundingTiles(1)) {
                if (tile == null || tile.canMoveToEurope()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Moves this unit to america.
     * 
     * @exception IllegalStateException If the move is illegal.
     */
    public void moveToAmerica() {
        if (!(getLocation() instanceof Europe)) {
            throw new IllegalStateException("A unit can only be moved to america from europe.");
        }

        setState(UnitState.TO_AMERICA);

        logger.info(toString() + " moving to America");
        
    }

    /**
     * Check if this unit can build a colony on the tile where it is located.
     * 
     * @return <code>true</code> if this unit can build a colony on the tile
     *         where it is located and <code>false</code> otherwise.
     */
    public boolean canBuildColony() {
        return (unitType.hasAbility("model.ability.foundColony") &&
                getMovesLeft() > 0 && 
                getTile() != null && 
                getTile().isColonizeable());
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
     * Get the number of turns of work left.
     * 
     * Caution: This does not equal the internal amount of work left as expert bonuses 
     * are included in the returned number. 
     * 
     * @return number of turns of work left.
     */
    public int getWorkLeft() {
        if (state == UnitState.IMPROVING && unitType.hasAbility("model.ability.expertPioneer")){
            return workLeft / 2;
        }
        
        return workLeft;
    }

    /**
     * The status of units that are currently working (for instance on building
     * a road, or fortifying themselves) is updated in this method.
     */
    public void doAssignedWork() {
        logger.finest("Entering method doAssignedWork.");
        if (workLeft > 0) {
            if (state == UnitState.IMPROVING) {
                // Has the improvement been completed already? Do nothing.
                if (getWorkImprovement().isComplete()) {
                    setState(UnitState.ACTIVE);
                    return;
                }

                // Otherwise do work
                int amountOfWork = unitType.hasAbility("model.ability.expertPioneer") ? 2 : 1;
                
                workLeft = getWorkImprovement().doWork(amountOfWork);
                
                // Make sure that a hardy pioneer will finish if the workLeft is 
                // less than he can do in a turn 
                if (0 < workLeft && workLeft < amountOfWork){
                    workLeft = getWorkImprovement().doWork(workLeft);
                }
            } else {
                workLeft--;
            }

            // Shorter travel time to America for the REF:
            if (state == UnitState.TO_AMERICA && getOwner().isREF()) {
                workLeft = 0;
            }

            if (workLeft == 0) {
                workLeft = -1;

                UnitState state = getState();

                switch (state) {
                case TO_EUROPE:
                    logger.info(toString() + " arrives in Europe");
                    // trade unit arrives in Europe
                    if (this.getTradeRoute() != null){
                        setMovesLeft(0);
                        setState(UnitState.ACTIVE);
                        return;
                    }
                        
                    getOwner().addModelMessage(new ModelMessage(ModelMessage.MessageType.DEFAULT,
                                                     "model.unit.arriveInEurope", getOwner().getEurope(), this)
                                    .add("%europe%", getOwner().getEurope().getNameKey()));
                    setState(UnitState.ACTIVE);
                    break;
                case TO_AMERICA:
                    logger.info(toString() + " arrives in America");
                    getGame().getModelController().setToVacantEntryLocation(this);
                    setState(UnitState.ACTIVE);
                    break;
                case FORTIFYING:
                    setState(UnitState.FORTIFIED);
                    break;
                case IMPROVING:
                    // Deliver Goods if any
                    GoodsType deliverType = getWorkImprovement().getDeliverGoodsType();
                    if (deliverType != null) {
                        int deliverAmount = getTile().potential(deliverType, getType())
                            * getWorkImprovement().getDeliverAmount();
                        if (unitType.hasAbility("model.ability.expertPioneer")) {
                            deliverAmount *= 2;
                        }
                        if (getColony() != null && getColony().getOwner().equals(getOwner())) {
                            getColony().addGoods(deliverType, deliverAmount);
                        } else {
                            List<Settlement> adjacentColonies = new ArrayList<Settlement>();
                            for (Tile t: getTile().getSurroundingTiles(1)) {
                                if (t.getColony() != null && t.getColony().getOwner().equals(getOwner())) {
                                    adjacentColonies.add(t.getColony());
                                }
                            }
                            if (adjacentColonies.size() > 0) {
                                int deliverPerCity = (deliverAmount / adjacentColonies.size());
                                for (int i = 0; i < adjacentColonies.size(); i++) {
                                    Colony c = (Colony) adjacentColonies.get(i);
                                    // Make sure the lumber lost is being added
                                    // again to the first adjacent colony:
                                    if (i == 0) {
                                        c.addGoods(deliverType, deliverPerCity
                                                   + (deliverAmount % adjacentColonies.size()));
                                    } else {
                                        c.addGoods(deliverType, deliverPerCity);
                                    }
                                }
                            }
                        }
                    }
                    // Finish up
                    TileImprovement improvement = getWorkImprovement();
                    for (Unit unit : getTile().getUnitList()) {
                        if (unit.getState() == UnitState.IMPROVING
                            && unit.getWorkImprovement().getType() == improvement.getType()) {
                            unit.expendEquipment(improvement.getExpendedEquipmentType(), 
                                                 improvement.getExpendedAmount());
                            unit.workLeft = -1;
                            unit.setWorkImprovement(null);
                            unit.setState(UnitState.ACTIVE);
                            // this only works for the current unit,
                            // all others will already have spent
                            // their moves, or will reset their moves
                            unit.setMovesLeft(0);
                        }
                    }
                    // This should be run at the end, so that the info sent to the other players
                    //is up to date.
                    getGame().getModelController().tileImprovementFinished(this, improvement);
                    
                    break;
                default:
                    logger.warning("Unknown work completed. State: " + state);
                    setState(UnitState.ACTIVE);
                }
            }
        }
    }

    /**
     * Reduces the number of tools and produces a warning if all tools are used
     * up.
     * 
     * @param amount The number of tools to remove.
     */
    private void expendEquipment(EquipmentType type, int amount) {
        equipment.incrementCount(type, -amount);
        setRole();
        // TODO: make this more generic
        EquipmentType tools = getSpecification().getEquipmentType("model.equipment.tools");
        if (!equipment.containsKey(tools)) {
            String messageId = (getType().getDefaultEquipmentType() == type)
                ? getType() + ".noMoreTools" : "model.unit.noMoreTools";
            getOwner().addModelMessage(new ModelMessage(ModelMessage.MessageType.WARNING,
                                                        messageId, this)
                            .addStringTemplate("%unit%", getLabel())
                            .addStringTemplate("%location%", getLocation().getLocationNameFor(getOwner())));
            firePropertyChange(Unit.EQUIPMENT_CHANGE, null, null);
        }
    }

    /**
     * Sets the <code>Location</code> in which this unit will be put when
     * returning from {@link Europe}.
     * 
     * @param entryLocation The <code>Location</code>.
     * @see #getEntryLocation
     */
    public void setEntryLocation(Location entryLocation) {
        this.entryLocation = entryLocation;
    }

    /**
     * Gets the <code>Location</code> in which this unit will be put when
     * returning from {@link Europe}. If this <code>Unit</code> has not not
     * been outside europe before, it will return the default value from the
     * {@link Player} owning this <code>Unit</code>.
     * 
     * @return The <code>Location</code>.
     * @see Player#getEntryLocation
     * @see #getVacantEntryLocation
     */
    public Location getEntryLocation() {
        return (entryLocation != null) ? entryLocation : getOwner().getEntryLocation();
    }

    /**
     * Gets the <code>Location</code> in which this unit will be put when
     * returning from {@link Europe}. If this <code>Unit</code> has not not
     * been outside europe before, it will return the default value from the
     * {@link Player} owning this <code>Unit</code>. If the tile is occupied
     * by a enemy unit, then a nearby tile is choosen.
     * 
     * <br>
     * <br>
     * <i>WARNING:</i> Only the server has the information to determine which
     * <code>Tile</code> is occupied. Use
     * {@link ModelController#setToVacantEntryLocation} instead.
     * 
     * @return The <code>Location</code>.
     * @see #getEntryLocation
     */
    // TODO: shouldn't this be somewhere else?
    public Location getVacantEntryLocation() {
        Tile l = (Tile) getEntryLocation();

        if (l.getFirstUnit() != null && l.getFirstUnit().getOwner() != getOwner()) {
            int radius = 1;
            while (true) {
                Iterator<Position> i = getGame().getMap().getCircleIterator(l.getPosition(), false, radius);
                while (i.hasNext()) {
                    Tile l2 = getGame().getMap().getTile(i.next());
                    if (l2.getFirstUnit() == null || l2.getFirstUnit().getOwner() == getOwner()) {
                        return l2;
                    }
                }

                radius++;
            }
        }

        return l;
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
     * Checks if this unit is an undead.
     * @return return true if the unit is undead
     */
    public boolean isUndead() {
        return hasAbility("model.ability.undead");
    }

    /**
     * Train the current unit in the job of its teacher.
     * 
     */
    public void train() {
        StringTemplate oldName = getLabel();
        UnitType skillTaught = getTeacher().getType().getSkillTaught();
        UnitType learning = getUnitTypeTeaching(skillTaught, unitType);

        if (learning != null) {
            setType(learning);
        }

        StringTemplate newName = getLabel();
        if (!newName.equals(oldName)) {
            Colony colony = getTile().getColony();
            getOwner().addModelMessage(new ModelMessage(ModelMessage.MessageType.UNIT_IMPROVED,
                                                        "model.unit.unitEducated", colony, this)
                            .addStringTemplate("%oldName%", oldName)
                            .addStringTemplate("%unit%", newName)
                            .addName("%colony%", colony.getName()));
        }
        this.setTurnsOfTraining(0);
        //setState(UnitState.ACTIVE);
        setMovesLeft(0);
    }

    /**
     * Returns true if this unit can carry treasure (like a treasure train)
     * 
     * @return <code>true</code> if this <code>Unit</code> is capable of
     *         carrying treasure.
     */
    public boolean canCarryTreasure() {
        return unitType.hasAbility("model.ability.carryTreasure");
    }

    /**
     * Returns true if this unit is a ship that can capture enemy goods.
     * 
     * @return <code>true</code> if this <code>Unit</code> is capable of
     *         capturing goods.
     */
    public boolean canCaptureGoods() {
        return unitType.hasAbility("model.ability.captureGoods");
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
     * Gets the Colony this unit is in.
     * 
     * @return The Colony it's in, or null if it is not in a Colony
     */
    public Colony getColony() {
        Location location = getLocation();
        return (location != null ? location.getColony() : null);
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
     * Checks if a colonist can get promoted by experience.
     */
    private void checkExperiencePromotion() {
        GoodsType produce = getWorkType();
        
        if(produce == null){
            return;
        }
        
        UnitType learnType = getSpecification().getExpertForProducing(produce);
        if (learnType == null || 
            learnType == unitType ||
            !unitType.canBeUpgraded(learnType, ChangeType.EXPERIENCE)) {
                return;
        }
        
        int random = getGame().getModelController().getRandom(getId() + "experience", 5000);
        if (random >= Math.min(experience, 200)) {
            return;
        }

        logger.finest("About to change type of unit due to experience.");
        StringTemplate oldName = getLabel();
        setType(learnType);
        getOwner().addModelMessage(new ModelMessage(ModelMessage.MessageType.UNIT_IMPROVED,
                                                    "model.unit.experience", getColony(), this)
                        .addStringTemplate("%oldName%", oldName)
                        .addStringTemplate("%unit%", getLabel())
                        .addName("%colony%", getColony().getName()));
    }

    /**
     * Prepares the <code>Unit</code> for a new turn.
     */
    public void newTurn() {
        if (isUninitialized()) {
            logger.warning("Calling newTurn for an uninitialized object: " + getId());
            return;
        }
        if (location instanceof ColonyTile) {
            checkExperiencePromotion();
        } 
        if (location instanceof Tile && ((Tile) location).getSettlement() == null) {
            attrition++;
            if (attrition > getType().getMaximumAttrition()) {
                getOwner().addModelMessage(new ModelMessage(ModelMessage.MessageType.UNIT_LOST,
                                                            "model.unit.attrition", this)
                                .addStringTemplate("%unit%", getLabel()));
                dispose();
            }
        } else {
            attrition = 0;
        }
        if (isUnderRepair()) {
            movesLeft = 0;
        } else {
            movesLeft = getInitialMovesLeft();
        }
        doAssignedWork();
        if (getState() == UnitState.SKIPPED) {
            setState(UnitState.ACTIVE);
        }

    }

    private Location newLocation(Game game, String locationString) {
        String XMLElementTag = locationString.substring(0, locationString.indexOf(':'));
        if (XMLElementTag.equals(Tile.getXMLElementTagName())) {
            return new Tile(game, locationString);
        } else if (XMLElementTag.equals(ColonyTile.getXMLElementTagName())) {
            return new ColonyTile(game, locationString);
        } else if (XMLElementTag.equals(Colony.getXMLElementTagName())) {
            return new Colony(game, locationString);
        } else if (XMLElementTag.equals(IndianSettlement.getXMLElementTagName())) {
            return new IndianSettlement(game, locationString);
        } else if (XMLElementTag.equals(Europe.getXMLElementTagName())) {
            return new Europe(game, locationString);
        } else if (XMLElementTag.equals(Building.getXMLElementTagName())) {
            return new Building(game, locationString);
        } else if (XMLElementTag.equals(Unit.getXMLElementTagName())) {
            return new Unit(game, locationString);
        } else {
            logger.warning("Unknown type of Location: " + locationString);
            return new Tile(game, locationString);
        }
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
        Set<Ability> autoDefence = getOwner().getFeatureContainer().getAbilitySet("model.ability.automaticEquipment");

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
        return getType().getUnitTypeChange(change, owner);
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


    private void unitsToXML(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame)
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
    protected void toXMLImpl(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame)
        throws XMLStreamException {
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
        String ownerID = null;
        if (getOwner().equals(player) || !hasAbility("model.ability.piracy") || showAll) {
            ownerID = owner.getId();
        } else {
            ownerID = Player.UNKNOWN_ENEMY;
        }
        out.writeAttribute("owner", ownerID);
        out.writeAttribute("turnsOfTraining", Integer.toString(turnsOfTraining));
        out.writeAttribute("workType", workType.getId());
        out.writeAttribute("experience", Integer.toString(experience));
        out.writeAttribute("treasureAmount", Integer.toString(treasureAmount));
        out.writeAttribute("hitpoints", Integer.toString(hitpoints));
        out.writeAttribute("attrition", Integer.toString(attrition));
        
        writeAttribute(out, "student", student);
        writeAttribute(out, "teacher", teacher);

        if (getGame().isClientTrusted() || showAll || player == getOwner()) {
            writeAttribute(out, "indianSettlement", indianSettlement);
            out.writeAttribute("workLeft", Integer.toString(workLeft));
        } else {
            out.writeAttribute("workLeft", Integer.toString(-1));
        }

        if (entryLocation != null) {
            out.writeAttribute("entryLocation", entryLocation.getId());
        }

        if (location != null) {
            if (getGame().isClientTrusted() || showAll || player == getOwner()
                || !(location instanceof Building || location instanceof ColonyTile)) {
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
        
        writeFreeColGameObject(workImprovement, out, player, showAll, toSavedGame);

        // Do not show enemy units hidden in a carrier:
        if (getGame().isClientTrusted() || showAll || getOwner().equals(player)) {
            unitsToXML(out, player, showAll, toSavedGame);
            if (canCarryGoods()) {
                goodsContainer.toXML(out, player, showAll, toSavedGame);
            }
        } else {
            if (canCarryGoods()) {
                out.writeAttribute("visibleGoodsCount", Integer.toString(getGoodsCount()));
                GoodsContainer emptyGoodsContainer = new GoodsContainer(getGame(), this);
                emptyGoodsContainer.setFakeID(goodsContainer.getId());
                emptyGoodsContainer.toXML(out, player, showAll, toSavedGame);
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
     * @throws javax.xml.stream.XMLStreamException is thrown if something goes wrong.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setId(in.getAttributeValue(null, ID_ATTRIBUTE));
        setName(in.getAttributeValue(null, "name"));
        UnitType oldUnitType = unitType;
        unitType = getSpecification().getUnitType(in.getAttributeValue(null, "unitType"));

        naval = unitType.hasAbility("model.ability.navalUnit");
        movesLeft = Integer.parseInt(in.getAttributeValue(null, "movesLeft"));
        state = Enum.valueOf(UnitState.class, in.getAttributeValue(null, "state"));
        role = Enum.valueOf(Role.class, in.getAttributeValue(null, "role"));
        workLeft = Integer.parseInt(in.getAttributeValue(null, "workLeft"));
        attrition = getAttribute(in, "attrition", 0);

        String ownerID = in.getAttributeValue(null, "owner");
        if (ownerID.equals(Player.UNKNOWN_ENEMY)) {
            owner = getGame().getUnknownEnemy();
        } else {
            owner = (Player) getGame().getFreeColGameObject(ownerID);
            if (owner == null) {
                owner = new Player(getGame(), in.getAttributeValue(null, "owner"));
            }
        }

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
            indianSettlement = (IndianSettlement) getGame().getFreeColGameObject(indianSettlementStr);
            if (indianSettlement == null) {
                indianSettlement = new IndianSettlement(getGame(), indianSettlementStr);
            }
        } else {
            setIndianSettlement(null);
        }

        treasureAmount = getAttribute(in, "treasureAmount", 0);

        final String destinationStr = in.getAttributeValue(null, "destination");
        if (destinationStr != null) {
            destination = (Location) getGame().getFreeColGameObject(destinationStr);
            if (destination == null) {
                destination = newLocation(getGame(), destinationStr);
            }
        } else {
            destination = null;
        }

        currentStop = -1;
        tradeRoute = null;
        final String tradeRouteStr = in.getAttributeValue(null, "tradeRoute");
        if (tradeRouteStr != null) {
            tradeRoute = (TradeRoute) getGame().getFreeColGameObject(tradeRouteStr);
            final String currentStopStr = in.getAttributeValue(null, "currentStop");
            if (currentStopStr != null) {
                currentStop = Integer.parseInt(currentStopStr);
            }
        }

        workType = getSpecification().getType(in, "workType", GoodsType.class, null);
        experience = getAttribute(in, "experience", 0);
        visibleGoodsCount = getAttribute(in, "visibleGoodsCount", -1);

        final String entryLocationStr = in.getAttributeValue(null, "entryLocation");
        if (entryLocationStr != null) {
            entryLocation = (Location) getGame().getFreeColGameObject(entryLocationStr);
            if (entryLocation == null) {
                entryLocation = newLocation(getGame(), entryLocationStr);
            }
        }

        final String locationStr = in.getAttributeValue(null, "location");
        if (locationStr != null) {
            location = (Location) getGame().getFreeColGameObject(locationStr);
            if (location == null) {
                location = newLocation(getGame(), locationStr);
            }
            //TODO: added to fix bug in pre-rev.4883 savegames. Might eventually
            //be removed later.
            //Savegame sanitation: A WorkLocation is always inside a colony, so
            //a unit located there should always have state "IN_COLONY".
            //If not, parts of the code may consider the unit to be outside
            //the colony, leading to errors.
            if ((location instanceof WorkLocation) && state!=UnitState.IN_COLONY) {
                logger.warning("Found "+getId()+" with state=="+state+" on WorkLocation in "+location.getColony().getName()+". Fixing: ");
                state=UnitState.IN_COLONY;
            }
        }

        units.clear();
        if (goodsContainer != null) goodsContainer.removeAll();
        equipment.clear();
        workImprovement = null;
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals(UNITS_TAG_NAME)) {
                units = new ArrayList<Unit>();
                while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
                    if (in.getLocalName().equals(Unit.getXMLElementTagName())) {
                        units.add(updateFreeColGameObject(in, Unit.class));
                    }
                }
            } else if (in.getLocalName().equals(GoodsContainer.getXMLElementTagName())) {
                goodsContainer = (GoodsContainer) getGame().getFreeColGameObject(in.getAttributeValue(null, ID_ATTRIBUTE));
                if (goodsContainer != null) {
                    goodsContainer.readFromXML(in);
                } else {
                    goodsContainer = new GoodsContainer(getGame(), this, in);
                }
            } else if (in.getLocalName().equals(EQUIPMENT_TAG)) {
                String xLength = in.getAttributeValue(null, ARRAY_SIZE);
                if (xLength == null) {
                    String equipmentId = in.getAttributeValue(null, ID_ATTRIBUTE_TAG);
                    int count = Integer.parseInt(in.getAttributeValue(null, "count"));
                    equipment.incrementCount(getSpecification().getEquipmentType(equipmentId), count);
                } else {
                    // TODO: remove support for old format
                    int length = Integer.parseInt(xLength);
                    for (int index = 0; index < length; index++) {
                        String equipmentId = in.getAttributeValue(null, "x" + String.valueOf(index));
                        equipment.incrementCount(getSpecification().getEquipmentType(equipmentId), 1);
                    }
                }
                in.nextTag();
            } else if (in.getLocalName().equals(TileImprovement.getXMLElementTagName())) {
                workImprovement = updateFreeColGameObject(in, TileImprovement.class);
            }
        }
        
        if (goodsContainer == null && canCarryGoods()) {
            logger.warning("Carrier with ID " + getId() + " did not have a \"goodsContainer\"-tag.");
            goodsContainer = new GoodsContainer(getGame(), this);
        }

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
     * Gets the tag name of the root element representing this object.
     *
     * @return "unit"
     */
    public static String getXMLElementTagName() {
        return "unit";
    }
}
