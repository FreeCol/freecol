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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.Specification;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Map.PathType;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.TradeRoute.Stop;
import net.sf.freecol.common.model.UnitType.DowngradeType;
import net.sf.freecol.common.model.UnitType.UpgradeType;
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

    private static final Logger logger = Logger.getLogger(Unit.class.getName());

    /**
     * XML tag name for equipment list.
     */
    private static final String EQUIPMENT_TAG = "equipment";

    private static final String UNITS_TAG_NAME = "units";

    /**
     * The number of turns required to sail between Europe and the New
     * World. TODO: In the future, this should be replaced by an
     * advanced sailing model taking prevailing winds into account.
     */
    public static final int TURNS_TO_SAIL = Specification.getSpecification().getIntegerOption(
            "model.option.turnsToSail").getValue();

    /**
     * A state a Unit can have.
     */
    public static enum UnitState { ACTIVE, FORTIFIED, SENTRY, IN_COLONY, IMPROVING,
            TO_EUROPE, IN_EUROPE, TO_AMERICA, FORTIFYING, SKIPPED }

    /** The roles a Unit can have. */
    public static enum Role {
        DEFAULT, PIONEER, MISSIONARY, SOLDIER, SCOUT, DRAGOON;
    
        public String getId() {
            return toString().toLowerCase();
        }
    }

    /**
     * A move type.
     * 
     * @see Unit#getMoveType(Map#Direction)
     */
    public static enum MoveType { MOVE, MOVE_HIGH_SEAS, ATTACK, EMBARK, DISEMBARK,
            ENTER_INDIAN_VILLAGE_WITH_FREE_COLONIST, ENTER_INDIAN_VILLAGE_WITH_SCOUT,
            ENTER_INDIAN_VILLAGE_WITH_MISSIONARY, ENTER_FOREIGN_COLONY_WITH_SCOUT,
            ENTER_SETTLEMENT_WITH_CARRIER_AND_GOODS, EXPLORE_LOST_CITY_RUMOUR, ILLEGAL_MOVE }

    public static final int MUSKETS_TO_ARM_INDIAN = 25, HORSES_TO_MOUNT_INDIAN = 25;

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
     * Describes if the unit has been moved onto high sea but not to europe.
     * This happens if the user moves a ship onto high sea but doesn't want to
     * send it to europe and selects to keep the ship in this waters.
     * 
     */
    private boolean alreadyOnHighSea = false;

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
    private List<EquipmentType> equipment = new ArrayList<EquipmentType>();


    /**
     * Initiate a new <code>Unit</code> of a specified type with the state set
     * to {@link #UnitState#ACTIVE} if a carrier and {@link #UnitState#SENTRY} otherwise. The
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
     * @param state The initial state for this Unit (one of {@link #UnitState.ACTIVE},
     *            {@link #FORTIFIED}...).
     * @param initialEquipment The list of initial EquimentTypes
     */
    public Unit(Game game, Location location, Player owner, UnitType type, UnitState state, 
                EquipmentType... initialEquipment) {
        super(game);

        visibleGoodsCount = -1;

        if (type.canCarryGoods()) {
            goodsContainer = new GoodsContainer(game, this);
        }

        this.owner = owner;
        unitType = type;
        naval = unitType.hasAbility("model.ability.navalUnit");
        setLocation(location);

        this.state = state;
        workLeft = -1;
        workType = Goods.BELLS;

        this.movesLeft = getInitialMovesLeft();
        hitpoints = unitType.getHitPoints();

        for (EquipmentType equipmentType : initialEquipment) {
            equipment.add(equipmentType);
        }
        setRole();

        getOwner().setUnit(this);
        getOwner().invalidateCanSeeTiles();
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
        getOwner().setUnit(this);
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
        getOwner().setUnit(this);
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
    public String getLocationName() {
        return Messages.message("onBoard", "%unit%", getName());
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
    public final List<EquipmentType> getEquipment() {
        return equipment;
    }

    /**
     * Set the <code>Equipment</code> value.
     *
     * @param newEquipment The new Equipment value.
     */
    public final void setEquipment(final List<EquipmentType> newEquipment) {
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
     * Get the current stop.
     * 
     * @return the current stop.
     */
    public Stop getCurrentStop() {
        ArrayList<Stop> stops = getTradeRoute().getStops();
        if (currentStop < 0 || currentStop >= stops.size()) {
            // currentStop can be out of range if trade route is modified
            currentStop = 0;
        }
        return stops.get(currentStop);
    }

    /**
     * Set current stop to next stop and return it
     * 
     * @return the next stop.
     */
    public Stop nextStop() {
        ArrayList<Stop> stops = getTradeRoute().getStops();
        if (stops.size() == 0) {
            currentStop = -1;
            setDestination(null);
            return null;
        }
        
        int oldStop = currentStop;
        Stop stop;
        do {
            currentStop++;
            if (currentStop >= stops.size()) {
                currentStop = 0;
            }
            stop = stops.get(currentStop);
        } while (!shouldGoToStop(stop) && currentStop != oldStop);
        
        setDestination(stop.getLocation());
        // if there is no valid stop, keep in current stop waiting to load
        return stop;
    }
    
    /**
     * Returns true if the Unit should proceed to the next stop of its
     * TradeRoute.
     *
     * @param stop a <code>Stop</code> value
     * @return a <code>boolean</code> value
     */
    public boolean shouldGoToStop(Stop stop) {
        ArrayList<GoodsType> goodsTypes = stop.getCargo();
        for(Goods goods : getGoodsList()) {
            boolean unload = true;
            for (int index = 0; index < goodsTypes.size(); index++) {
                if (goods.getType() == goodsTypes.get(index)) {
                    goodsTypes.remove(index);
                    unload = false;
                    break;
                }
            }
            if (unload) { // There is goods to unload
                return true;
            }
        }
        // All loaded goods are in cargo list
        // go to stop only if there is something to load
        return getSpaceLeft() > 0 && goodsTypes.size() > 0;
    }

    /**
     * Sells the given goods from this unit to the given settlement. The owner
     * of this unit gets the gold and the owner of the settlement is charged for
     * the deal.
     * 
     * @param settlement The <code>Settlement</code> to trade with.
     * @param goods The <code>Goods</code> to be traded.
     * @param gold The money to be given for the goods.
     */
    // TODO: shouldn't this be somewhere else?
    public void trade(Settlement settlement, Goods goods, int gold) {
        if (getTile().getDistanceTo(settlement.getTile()) > 1) {
            logger.warning("Unit not adjacent to settlement!");
            throw new IllegalStateException("Unit not adjacent to settlement!");
        }
        if (goods.getLocation() != this) {
            logger.warning("Goods not onboard this unit!");
            throw new IllegalStateException("Goods not onboard this unit!");
        }
        if (getMovesLeft() <= 0) {
            logger.warning("No more moves!");
            throw new IllegalStateException("No more moves left!");
        }

        goods.setLocation(settlement);

        /*
         * Value already tested. This test is needed because the opponent's
         * amount of gold is hidden for the client:
         */
        if (settlement.getOwner().getGold() - gold >= 0) {
            settlement.getOwner().modifyGold(-gold);
        }

        setMovesLeft(0);
        getOwner().modifyGold(gold);
        getOwner().modifySales(goods.getType(), goods.getAmount());
        getOwner().modifyIncomeBeforeTaxes(goods.getType(), gold);
        getOwner().modifyIncomeAfterTaxes(goods.getType(), gold);

        if (settlement instanceof IndianSettlement) {
            IndianSettlement nativeSettlement = ((IndianSettlement) settlement);
            int value = nativeSettlement.getPrice(goods) / 1000;
            nativeSettlement.modifyAlarm(getOwner(), -value*2);
        }
    }

    /**
     * Sells the given goods from the given settlement to this unit. The owner
     * of the settlement gets the gold and the owner of the unit is charged for
     * the deal.
     * 
     * @param settlement The <code>IndianSettlement</code> to trade with.
     * @param goods The <code>Goods</code> to be traded.
     * @param gold The money to be given for the goods.
     */
    public void buy(IndianSettlement settlement, Goods goods, int gold) {
        if (getTile().getDistanceTo(settlement.getTile()) > 1) {
            logger.warning("Unit not adjacent to settlement!");
            throw new IllegalStateException("Unit not adjacent to settlement!");
        }
        if (goods.getLocation() != settlement) {
            logger.warning("Goods not in the settlement!");
            throw new IllegalStateException("Goods not in the settlement!");
        }

        goods.setLocation(this);

        settlement.getOwner().modifyGold(gold);
        getOwner().modifyGold(-gold);

        settlement.modifyAlarm(getOwner(), -gold / 50);
    }

    /**
     * Transfers the given goods from this unit to the given settlement.
     * 
     * @param settlement The <code>Settlement</code> to deliver a gift to.
     * @param goods The <code>Goods</code> to be delivered as a gift.
     */
    public void deliverGift(Settlement settlement, Goods goods) {
        if (getTile().getDistanceTo(settlement.getTile()) > 1) {
            logger.warning("Unit not adjacent to settlement!");
            throw new IllegalStateException("Unit not adjacent to settlement!");
        }
        if (goods.getLocation() != this) {
            logger.warning("Goods not onboard this unit!");
            throw new IllegalStateException("Goods not onboard this unit!");
        }
        if (getMovesLeft() <= 0) {
            logger.warning("No more moves left!");
            throw new IllegalStateException("No more moves left!");
        }

        int amount = goods.getAmount();

        goods.setLocation(settlement);
        setMovesLeft(0);

        if (settlement instanceof IndianSettlement) {
            int value = ((IndianSettlement) settlement).getPrice(goods) / 100;
            ((IndianSettlement)settlement).modifyAlarm(getOwner(), -value*2);
        } else {
            addModelMessage(settlement, ModelMessage.MessageType.GIFT_GOODS, goods.getType(),
                            "model.unit.gift",
                            "%player%", getOwner().getNationAsString(),
                            "%type%", goods.getName(),
                            "%amount%", Integer.toString(amount),
                            "%colony%", ((Colony) settlement).getName());
        }
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
        if (getOwner().getEurope() != null) {
            return (loc.getColony() != null && !loc.getColony().isLandLocked()) || loc instanceof Europe
                || (loc instanceof Unit && ((Unit) loc).getLocation() instanceof Europe);
        } else {
            return loc.getColony() != null;
        }
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
                float transportFee = getTreasureAmount() / 2;
                return (int) getOwner().getFeatureContainer()
                    .applyModifier(getTreasureAmount() / 2f,
                                   "model.modifier.treasureTransportFee",
                                   unitType, getGame().getTurn());
            }
        }
        return 0;
    }

    /**
     * Transfers the gold carried by this unit to the {@link Player owner}.
     * 
     * @exception IllegalStateException if this unit is not a treasure train. or
     *                if it cannot be cashed in at it's current location.
     */
    public void cashInTreasureTrain() {
        if (!canCarryTreasure()) {
            throw new IllegalStateException("Can't carry a treasure");
        }

        if (canCashInTreasureTrain()) {
            int cashInAmount = getTreasureAmount() - getTransportFee();
            cashInAmount = cashInAmount * (100 - getOwner().getTax()) / 100;
            FreeColGameObject owner = getOwner();
            if (isInEurope()) {
                owner = getOwner().getEurope();
            }
            getOwner().modifyGold(cashInAmount);
            addModelMessage(owner, ModelMessage.MessageType.DEFAULT,
                            "model.unit.cashInTreasureTrain",
                            "%amount%", Integer.toString(getTreasureAmount()),
                            "%cashInAmount%", Integer.toString(cashInAmount));
            dispose();
        } else {
            throw new IllegalStateException("Cannot cash in treasure train at the current location.");
        }
    }

    /**
     * Checks if this <code>Unit</code> is a colonist. A <code>Unit</code>
     * is a colonist if it can build a new <code>Colony</code>.
     * 
     * @return <i>true</i> if this unit is a colonist and <i>false</i>
     *         otherwise.
     */
    public boolean isColonist() {
        return unitType.hasAbility("model.ability.foundColony");
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
        if (student != null) {
            return getNeededTurnsOfTraining(unitType, student.unitType);
        } else {
            throw new IllegalStateException();
        }
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
    public static int getNeededTurnsOfTraining(UnitType typeTeacher, UnitType typeStudent) {
        UnitType teaching = getUnitTypeTeaching(typeTeacher, typeStudent);
        if (teaching != null) {
            return typeStudent.getEducationTurns(teaching);
        } else {
            throw new IllegalStateException();
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
        if (typeStudent.canBeUpgraded(typeTeacher, UpgradeType.EDUCATION)) {
            return typeTeacher;
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
        for (EquipmentType equipmentType : equipment) {
            result.addAll(equipmentType.getFeatureContainer().getAbilitySet(id));
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
        for (EquipmentType equipmentType : equipment) {
            result.addAll(equipmentType.getFeatureContainer().getModifierSet(id));
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
    public static boolean canBeStudent(UnitType typeStudent, UnitType typeTeacher) {
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
        if (newStudent == null) {
            this.student = null;
        } else if (newStudent.getColony() != null &&
                   newStudent.getColony() == getColony() &&
                   newStudent.canBeStudent(this)) {
            this.student = newStudent;
        } else {
            throw new IllegalStateException("unit can not be student: " + newStudent.getName());
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
        if (newTeacher == null) {
            this.teacher = null;
        } else if (newTeacher.getColony() != null &&
                   newTeacher.getColony() == getColony() &&
                   getColony().canTrain(newTeacher)) {
            this.teacher = newTeacher;
        } else {
            throw new IllegalStateException("unit can not be teacher: " + newTeacher.getName());
        }
    }

    /**
     * Gets the <code>Building</code> this unit is working in.
     */
    public Building getWorkLocation() {
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
        if (!type.isFarmed()) {
            return;
        }
        if (workType != type) {
            experience = 0;
        }
        workType = type;
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
            logger.warning("getTile() == null for " + getName() + " at location: " + getLocation());
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
     *         or <code>Integer.MAX_VALUE</code> if no path can be found.
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
     *         or <code>Integer.MAX_VALUE</code> if no path can be found.
     */
    public int getTurnsToReach(Tile start, Tile end) {

        if (start == end) {
            return 0;
        }

        if (getLocation() instanceof Unit) {
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

        return Integer.MAX_VALUE;
    }
    
    /**
     * Returns the number of turns this <code>Unit</code> will have to use in
     * order to reach the given <code>Location</code>.
     * 
     * @param destination The destination for this unit.
     * @return The number of turns it will take to reach the <code>destination</code>,
     *         or <code>Integer.MAX_VALUE</code> if no path can be found.
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
            if (getLocation() instanceof Unit) {
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
                return Integer.MAX_VALUE;
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
     * <code>ILLEGAL_MOVE</code>, if {@link #getMoveCost} returns a move cost
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
     * <code>ILLEGAL_MOVE</code>, if {@link #getMoveCost} returns a move cost
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

        // Using +2 in order to make 1/3 and 2/3 move count as 3/3, only when
        // getMovesLeft > 0
        if (cost > ml) {
            if ((ml + 2 >= getInitialMovesLeft() || cost <= ml + 2) && ml != 0) {
                return ml;
            }

            return cost;
        } else if (isNaval() && from.isLand() && from.getSettlement() == null) {
            // Ship on land due to it was in a colony which was abandoned
            return ml;
        } else {
            return cost;
        }
    }

    /**
     * Returns true if this unit can enter a settlement in order to trade.
     * 
     * @param settlement The settlement to enter.
     * @return <code>true</code> if this <code>Player</code> can trade with
     *         the given <code>Settlement</code>. The unit will for instance
     *         need to be a {@link #isCarrier() carrier} and have goods onboard.
     */
    public boolean canTradeWith(Settlement settlement) {
        return (canCarryGoods() &&
                goodsContainer.getGoodsCount() > 0 &&
                getOwner().getStance(settlement.getOwner()) != Stance.WAR &&
                ((settlement instanceof IndianSettlement) ||
                 hasAbility("model.ability.tradeWithForeignColonies")));
    }

    /**
     * Gets the type of a move made in a specified direction.
     * 
     * @param direction The direction of the move.
     * @return The move type. Notice: <code>Unit.ILLEGAL_MOVE</code> when
     *         there are no moves left.
     */
    public MoveType getMoveType(Direction direction) {
        if (getTile() == null) {
            throw new IllegalStateException("getTile() == null");
        }

        Tile target = getGame().getMap().getNeighbourOrNull(direction, getTile());

        return getMoveType(target);
    }

    /**
     * Gets the type of a move that is made when moving to the specified
     * <code>Tile</code> from the current one.
     * 
     * @param target The target tile of the move
     * @return The move type. Notice: <code>Unit.ILLEGAL_MOVE</code> when
     *         there are no moves left.
     */
    public MoveType getMoveType(Tile target) {
        return getMoveType(getTile(), target, getMovesLeft());
    }

    /**
     * Gets the type of a move that is made when moving to the specified
     * <code>Tile</code> from the specified <code>Tile</code>.
     * 
     * @param from The origin tile of the move
     * @param target The target tile of the move
     * @param ml The amount of moves this Unit has left
     * @return The move type. Notice: <code>Unit.ILLEGAL_MOVE</code> when
     *         there are no moves left.
     */
    public MoveType getMoveType(Tile from, Tile target, int ml) {
        if (from == null) {
            throw new IllegalStateException("from == null");
        } else if (isUnderRepair()) {
            return MoveType.ILLEGAL_MOVE;
        } else if (ml <= 0) {
            return MoveType.ILLEGAL_MOVE;
        } else {
            if (isNaval()) {
                return getNavalMoveType(from, target, ml);
            }
            return getLandMoveType(from, target, ml);
        }
    }

    /**
     * Gets the type of a move that is made when moving to the specified
     * <code>Tile</code> from the specified <code>Tile</code>.
     * 
     * @param from The origin tile of the move
     * @param target The target tile of the move
     * @param ml The amount of moves this Unit has left
     * @return The move type. Notice: <code>Unit.MoveType.ILLEGAL_MOVE</code> when
     *         there are no moves left.
     */
    private MoveType getNavalMoveType(Tile from, Tile target, int ml) {
        if (target == null) { // TODO: do not allow "MOVE_HIGH_SEAS" north and
            // south.
            if (getOwner().canMoveToEurope()) {
                return MoveType.MOVE_HIGH_SEAS;
            } else {
                return MoveType.ILLEGAL_MOVE;
            }
        } else if (target.isLand()) {
            Settlement settlement = target.getSettlement();
            if (settlement != null) {
                if (settlement.getOwner() == getOwner()) {
                    return MoveType.MOVE;
                } else if (canTradeWith(settlement)) {
                    return MoveType.ENTER_SETTLEMENT_WITH_CARRIER_AND_GOODS;
                } else {
                    logger.fine("Trying to enter another player's settlement with " + getName());
                    return MoveType.ILLEGAL_MOVE;
                }
            } else if (target.getDefendingUnit(this) != null && target.getDefendingUnit(this).getOwner() != getOwner()) {
                logger.fine("Trying to sail into tile occupied by enemy units with " + getName());
                return MoveType.ILLEGAL_MOVE;
            } else {
                // Check for disembark.
                Iterator<Unit> unitIterator = getUnitIterator();

                while (unitIterator.hasNext()) {
                    Unit u = unitIterator.next();
                    if (u.getMovesLeft() > 0) {
                        return MoveType.DISEMBARK;
                    }
                }
                logger.fine("No units to disembark from " + getName());
                return MoveType.ILLEGAL_MOVE;
            }
        } else if (target.getDefendingUnit(this) != null && target.getDefendingUnit(this).getOwner() != getOwner()) {
            // enemy units at sea
            if (isOffensiveUnit()) {
                return MoveType.ATTACK;
            } else {
                return MoveType.ILLEGAL_MOVE;
            }
        } else if (target.getType().canSailToEurope()) {
            if (getOwner().canMoveToEurope()) {
                return MoveType.MOVE_HIGH_SEAS;
            } else {
                return MoveType.MOVE;
            }
        } else {
            // this must be ocean
            return MoveType.MOVE;
        }
    }

    /**
     * Gets the type of a move that is made when moving to the specified
     * <code>Tile</code> from the specified <code>Tile</code>.
     * 
     * @param from The origin tile of the move
     * @param target The target tile of the move
     * @param ml The amount of moves this Unit has left
     * @return The move type. Notice: <code>Unit.MoveType.ILLEGAL_MOVE</code> when
     *         there are no moves left.
     */
    private MoveType getLandMoveType(Tile from, Tile target, int ml) {
        if (target == null) {
            // only naval units are allowed to do this
            logger.fine("Trying to enter null tile with land unit " + getName());
            return MoveType.ILLEGAL_MOVE;
        }

        if (target.isLand()) {
            Settlement settlement = target.getSettlement();
            if (settlement != null) {
                if (settlement.getOwner() == getOwner()) {
                    // our colony
                    return MoveType.MOVE;
                } else if (canTradeWith(settlement)) {
                    return MoveType.ENTER_SETTLEMENT_WITH_CARRIER_AND_GOODS;
                } else if (settlement instanceof IndianSettlement) {
                    IndianSettlement indian = (IndianSettlement) settlement;
                    if (hasAbility("model.ability.scoutIndianSettlement")) {
                        return MoveType.ENTER_INDIAN_VILLAGE_WITH_SCOUT;
                    } else if (hasAbility("model.ability.missionary")) {
                        return MoveType.ENTER_INDIAN_VILLAGE_WITH_MISSIONARY;
                    } else if (isOffensiveUnit()) {
                        return MoveType.ATTACK;
                    } else if (indian.getLearnableSkill() != null || !indian.hasBeenVisited()) {
                        return MoveType.ENTER_INDIAN_VILLAGE_WITH_FREE_COLONIST;
                    } else {
                        return MoveType.ILLEGAL_MOVE;
                    }
                } else if (settlement instanceof Colony) {
                    if (hasAbility("model.ability.scoutForeignColony")) {
                        return MoveType.ENTER_FOREIGN_COLONY_WITH_SCOUT;
                    } else if (isOffensiveUnit()) {
                        return MoveType.ATTACK;
                    } else {
                        logger.fine("Trying to enter foreign colony with " + getName());
                        return MoveType.ILLEGAL_MOVE;
                    }
                }
            } else if (target.getDefendingUnit(this) != null && target.getDefendingUnit(this).getOwner() != getOwner()) {
                if (from.isLand()) {
                    if (isOffensiveUnit()) {
                        return MoveType.ATTACK;
                    } else {
                        logger.fine("Trying to attack with civilian " + getName());
                        return MoveType.ILLEGAL_MOVE;
                    }
                } else {
                    /*
                     * Note that attacking a settlement from a ship is allowed.
                     */
                    logger.fine("Attempting marine assault with " + getName());
                    return MoveType.ILLEGAL_MOVE;
                }
            } else if (target.getFirstUnit() != null && target.getFirstUnit().isNaval()
                       && target.getFirstUnit().getOwner() != getOwner()) {
                // An enemy ship in land tile without a settlement
                return MoveType.ILLEGAL_MOVE;
            } else if (getMoveCost(from, target, ml) > ml) {
                return MoveType.ILLEGAL_MOVE;
            } else if (target.hasLostCityRumour()) {
                return MoveType.EXPLORE_LOST_CITY_RUMOUR;
            } else {
                return MoveType.MOVE;
            }
        } else {
            // check for embarkation
            if (target.getFirstUnit() == null || target.getFirstUnit().getOwner() != getOwner()) {
                logger.fine("Trying to embark on tile occupied by foreign units with " + getName());
                return MoveType.ILLEGAL_MOVE;
            } else {
                for (Unit u : target.getUnitList()) {
                    if (u.getSpaceLeft() >= getSpaceTaken()) {
                        return MoveType.EMBARK;
                    }
                }
                logger.fine("Trying to board full vessel with " + getName());
                return MoveType.ILLEGAL_MOVE;
            }
        }

        logger.info("Default illegal move for " + getName());
        return MoveType.ILLEGAL_MOVE;
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
     * Moves this unit in the specified direction.
     * 
     * @param direction The direction
     * @see #getMoveType(Direction)
     * @exception IllegalStateException If the move is illegal.
     */
    public void move(Direction direction) {
        MoveType moveType = getMoveType(direction);

        // For debugging:
        switch (moveType) {
        case MOVE:
        case MOVE_HIGH_SEAS:
        case EXPLORE_LOST_CITY_RUMOUR:
            break;
        case ATTACK:
        case EMBARK:
        case ENTER_INDIAN_VILLAGE_WITH_FREE_COLONIST:
        case ENTER_INDIAN_VILLAGE_WITH_SCOUT:
        case ENTER_INDIAN_VILLAGE_WITH_MISSIONARY:
        case ENTER_FOREIGN_COLONY_WITH_SCOUT:
        case ENTER_SETTLEMENT_WITH_CARRIER_AND_GOODS:
        case ILLEGAL_MOVE:
        default:
            throw new IllegalStateException("\nIllegal move requested: " + moveType + " while trying to move a "
                                            + getName() + " located at " + getTile().getPosition().toString() + ". Direction: " + direction
                                            + " Moves Left: " + getMovesLeft());
        }

        moveToTile(getGame().getMap().getNeighbourOrNull(direction, getTile()));
    }

    /**
     * Move to a given tile. Unlike {@link #move(Direction)} no validation is done, so
     * this method may be called from the opponentMove-handler.
     * 
     * @param newTile The new tile.
     */
    public void moveToTile(Tile newTile) {
        if (newTile != null) {
            setState(UnitState.ACTIVE);
            setStateToAllChildren(UnitState.SENTRY);
            int moveCost = getMoveCost(newTile);
            setMovesLeft(getMovesLeft() - moveCost);
            setLocation(newTile);
            activeAdjacentSentryUnits(newTile);

            // Clear the alreadyOnHighSea flag if we move onto a non-highsea
            // tile.
            if (newTile.getType().canSailToEurope()) {
                setAlreadyOnHighSea(true);
            } else {
                setAlreadyOnHighSea(false);
            }

        } else {
            throw new IllegalStateException("Illegal move requested - no target tile!");
        }
    }

    /**
     * Active units with sentry state wich are adjacent to a specified tile
     * 
     * @param tile The tile to iterate over adjacent tiles.
     */
    public void activeAdjacentSentryUnits(Tile tile) {
        Map map = getGame().getMap();
        Iterator<Position> it = map.getAdjacentIterator(tile.getPosition());
        while (it.hasNext()) {
            Iterator<Unit> unitIt = map.getTile(it.next()).getUnitIterator();
            while (unitIt.hasNext()) {
                Unit unit = unitIt.next();
                if (unit.getState() == UnitState.SENTRY && unit.getOwner() != getOwner()) {
                    unit.setState(UnitState.ACTIVE);
                }
            }
        }
    }

    /**
     * Embarks this unit onto the specified unit.
     * 
     * @param unit The unit to embark onto.
     * @exception IllegalStateException If the embark is illegal.
     */
    public void embark(Unit unit) {
        if (unit == null) {
            throw new IllegalArgumentException("Target of embarkation must not be 'null'.");
        } else if (getMoveType(unit.getTile()) != MoveType.EMBARK) {
            throw new IllegalStateException("Illegal disembark requested!");
        } else {
            setLocation(unit);
            setMovesLeft(getMovesLeft() - 3);
        }
    }

    /**
     * Boards a carrier that is on the same tile.
     * 
     * @param carrier The carrier this unit shall embark.
     * @exception IllegalStateException If the carrier is on another tile than
     *                this unit.
     */
    public void boardShip(Unit carrier) {
        if (isCarrier()) {
            throw new IllegalStateException("A carrier cannot board another carrier!");
        }

        if (getTile() == carrier.getTile() && isInEurope() == carrier.isInEurope()) {
            setLocation(carrier);
            setState(UnitState.SENTRY);
        } else {
            throw new IllegalStateException("It is not allowed to board a ship on another tile.");
        }
    }

    /**
     * Leave the ship. This method should only be invoked if the ship is in a
     * harbour.
     * 
     * @exception IllegalStateException If not in harbour.
     * @exception ClassCastException If not this unit is located on a ship.
     */
    public void leaveShip() {
        Unit carrier = (Unit) getLocation();
        Location l = carrier.getLocation();

        if (carrier.isInEurope()) {
            setLocation(l);
        } else if (getTile().getSettlement() != null) {
            setLocation(getTile());
        } else {
            throw new IllegalStateException("A unit may only leave a ship while in a harbour.");
        }

        setState(UnitState.ACTIVE);
    }

    /**
     * Sets the given state to all the units that si beeing carried.
     * 
     * @param state The state.
     */
    public void setStateToAllChildren(UnitState state) {
        if (canCarryUnits()) {
            for (Unit u : getUnitList())
                u.setState(state);
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
            if (getSpaceLeft() <= 0) {
                throw new IllegalStateException();
            }
            if (units.equals(Collections.emptyList())) {
                units = new ArrayList<Unit>();
            }
            units.add((Unit) locatable);
            spendAllMoves();
        } else if (locatable instanceof Goods && canCarryGoods()) {
            goodsContainer.addGoods((Goods) locatable);
            if (getSpaceLeft() < 0) {
                throw new IllegalStateException("Not enough space for the given locatable!");
            }
            spendAllMoves();
        } else {
            logger.warning("Tried to add a 'Locatable' to a non-carrier unit.");
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
            units.remove((Unit) locatable);
            spendAllMoves();
        } else if (locatable instanceof Goods && canCarryGoods()) {
            goodsContainer.removeGoods((Goods) locatable);
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
            return units.contains((Unit) locatable);
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
        if (player == getOwner()) {
            return true;
        }
        return getTile() != null
            && player.canSee(getTile())
            && (getTile().getSettlement() == null || getTile().getSettlement().getOwner() == player || (!getGameOptions()
                                                                                                        .getBoolean(GameOptions.UNIT_HIDING) && getLocation() instanceof Tile))
            && (!(getLocation() instanceof Unit) || ((Unit) getLocation()).getOwner() == player || !getGameOptions()
                .getBoolean(GameOptions.UNIT_HIDING));
    }

    /**
     * Gets a <code>Iterator</code> of every <code>Unit</code> directly
     * located on this <code>Location</code>.
     * 
     * @return The <code>Iterator</code>.
     */
    public Iterator<Unit> getUnitIterator() {
        return units.iterator();
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
     * @exception IllegalStateException If the <code>workLocation</code> is on
     *                another {@link Tile} than this <code>Unit</code>.
     */
    public void work(WorkLocation workLocation) {
        if (workLocation.getTile() != getTile()) {
            throw new IllegalStateException("Can only set a 'Unit'  to a 'WorkLocation' that is on the same 'Tile'.");
        }
        setState(UnitState.IN_COLONY);
        setLocation(workLocation);
    }

    /**
     * Sets this <code>Unit</code> to work at this <code>TileImprovement</code>
     * @param improvement a <code>TileImprovement</code> value
     * @exception IllegalStateException If the <code>TileImprovement</code> is on
     *                another {@link Tile} than this <code>Unit</code> or is not
     *                a valid pioneer.
     */
    public void work(TileImprovement improvement) {
    	
    	if (!hasAbility("model.ability.improveTerrain")) {
            throw new IllegalStateException("Only 'Pioneers' can perform TileImprovement.");
        }
    	
    	if (!canPerformImprovement(improvement.getType())){
            throw new IllegalArgumentException("Cannot perform this improvement for this tile");
    	}
    	
    	if (improvement == null){
            throw new IllegalArgumentException("Cannot perform this improvement for this tile");
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
        
        if (location != null) {
            if (location instanceof ColonyTile){
                // the unit is leaving a colony tile reset the
                // ownership of the tile, so that others may claim it
                ((ColonyTile)location).getWorkTile().setOwner(null);
            }
            location.remove(this);
        }

        location = newLocation;

        if (newLocation != null) {
            newLocation.add(this);
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

        // Check for adjacent units owned by a player that our owner has not met
        // before:
        if (getGame().getMap() != null && location != null && location instanceof Tile && !isNaval()) {
            contactAdjacent(getTile());
        }
        if (!getOwner().isIndian()) {
            getOwner().setExplored(this);
        }
    }

    /**
     * Contact Players with Units or Settlements on surrounding Tiles.
     *
     * @param tile a <code>Tile</code> value
     */
    public void contactAdjacent(Tile tile) {
        Iterator<Position> tileIterator = getGame().getMap().getAdjacentIterator(tile.getPosition());
        while (tileIterator.hasNext()) {
            Tile t = getGame().getMap().getTile(tileIterator.next());

            if (t == null) {
                continue;
            }
            Settlement settlement = t.getSettlement();
            if (settlement != null) {
                if (!settlement.getOwner().hasContacted(getOwner())) {
                    settlement.getOwner().setContacted(getOwner(), true);
                    getOwner().setContacted(settlement.getOwner(), true);
                }
                if (settlement instanceof IndianSettlement) {
                    ((IndianSettlement) settlement).getAlarm().put(getOwner(), new Tension(0));
                }
            } else if (t.isLand() && t.getFirstUnit() != null
                       && !t.getFirstUnit().getOwner().hasContacted(getOwner())) {
                t.getFirstUnit().getOwner().setContacted(getOwner(), true);
                getOwner().setContacted(t.getFirstUnit().getOwner(), true);
            }
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
        int count = 1;
        for (EquipmentType oldType : equipment) {
            if (oldType == equipmentType) {
                count++;
            }
        }
        if (count > equipmentType.getMaximumCount()) {
            return false;
        }
        return true;
    }

    public void equipWith(EquipmentType equipmentType) {
        equipWith(equipmentType, false);
    }

    /**
     * Equip this unit with the given EquipmentType, provided that all
     * requirements are met, and that the EquipmentType can be built
     * at this location or is present as a result of combat.
     *
     * @param equipmentType an <code>EquipmentType</code> value
     * @param asResultOfCombat a <code>boolean</code> value
     * @see #canBeEquippedWith
     */
    public void equipWith(EquipmentType equipmentType, boolean asResultOfCombat) {
        if (equipmentType == null) {
            throw new IllegalArgumentException("EquipmentType is 'null'.");
        }
        if (!canBeEquippedWith(equipmentType)) {
            logger.fine("Unable to equip unit " + getId() + " with " + equipmentType.getName());
            return;
        }
        if (!(asResultOfCombat || 
              (getColony() != null && getColony().canBuildEquipment(equipmentType)) ||
              (isInEurope() && getOwner().getEurope().canBuildEquipment(equipmentType)))) {
            logger.fine("Unable to build equipment " + equipmentType.getName());
            return;
        }
        Iterator<EquipmentType> equipmentIterator = equipment.iterator();
        while (equipmentIterator.hasNext()) {
            EquipmentType oldEquipment = equipmentIterator.next();
            if (!oldEquipment.isCompatibleWith(equipmentType)) {
                dumpEquipment(oldEquipment, asResultOfCombat);
                equipmentIterator.remove();
            }
        }
        if (!asResultOfCombat) {
            if (getColony() != null) {
                for (AbstractGoods goods : equipmentType.getGoodsRequired()) {
                    getColony().removeGoods(goods);
                }
            } else if (isInEurope()) {
                for (AbstractGoods goods : equipmentType.getGoodsRequired()) {
                    getOwner().getMarket().buy(goods.getType(), goods.getAmount(), getOwner());
                }
            }
        }
        equipment.add(equipmentType);
        setRole();
    }

    public void removeEquipment(EquipmentType equipmentType) {
        removeEquipment(equipmentType, false);
    }

    public void removeEquipment(EquipmentType equipmentType, boolean asResultOfCombat) {
        dumpEquipment(equipmentType, asResultOfCombat);
        equipment.remove(equipmentType);
        setRole();
    }

    public void removeAllEquipment(boolean asResultOfCombat) {
        for (EquipmentType equipmentType : equipment) {
            dumpEquipment(equipmentType, asResultOfCombat);
        }
        equipment.clear();
        role = Role.DEFAULT;
    }

    private void dumpEquipment(EquipmentType equipmentType, boolean asResultOfCombat) {
        if (!asResultOfCombat) {
            // the equipment is returned to storage in the form of goods
            if (getColony() != null) {
                for (AbstractGoods goods : equipmentType.getGoodsRequired()) {
                    getColony().addGoods(goods);
                }
            } else if (isInEurope()) {
                for (AbstractGoods goods : equipmentType.getGoodsRequired()) {
                    getOwner().getMarket().sell(goods.getType(), goods.getAmount(), getOwner());
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
        return Collections.frequency(equipment, equipmentType);
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
            return getLocation() instanceof Europe && 
                getState() != UnitState.TO_EUROPE &&
                getState() != UnitState.TO_AMERICA;
        }
    }

    /**
     * Buys goods of a specified type and amount and adds it to this
     * <code>Unit</code>. Can only be used when the <code>Unit</code> is a
     * carrier and is located in {@link Europe}.
     * 
     * @param goodsType The type of goods to buy.
     * @param amount The amount of goods to buy.
     */
    public void buyGoods(GoodsType goodsType, int amount) {
        if (!canCarryGoods() || !isInEurope()) {
            throw new IllegalStateException("Cannot buy goods when not a carrier or in Europe.");
        }

        try {
            getOwner().getMarket().buy(goodsType, amount, getOwner());
            goodsContainer.addGoods(goodsType, amount);
        } catch (IllegalStateException ise) {
            this.addModelMessage(this, ModelMessage.MessageType.DEFAULT, "notEnoughGold");
        }
    }

    /**
     * Checks if this <code>Unit</code> is able to carry {@link Locatable}s.
     * 
     * @return 'true' if this unit can carry other units, 'false' otherwise.
     */
    public boolean isCarrier() {
        return isCarrier(unitType);
    }

    /**
     * Checks if this <code>Unit</code> is able to carry {@link Locatable}s.
     * 
     * @param unitType The type used when checking.
     * @return 'true' if the unit can carry other units, 'false' otherwise.
     */
    public static boolean isCarrier(UnitType unitType) {
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
     * Sets the owner of this Unit.
     * 
     * @param owner The new owner of this Unit.
     */
    public void setOwner(Player owner) {
        Player oldOwner = this.owner;
        oldOwner.removeUnit(this);
        owner.setUnit(this);

        this.owner = owner;

        oldOwner.invalidateCanSeeTiles();
        getOwner().setExplored(this);

        if (getGame().getFreeColGameObjectListener() != null) {
            getGame().getFreeColGameObjectListener().ownerChanged(this, oldOwner, owner);
        }
    }

    /**
     * Sets the type of the unit.
     * 
     * @param unitType The new type of the unit.
     */
    public void setType(UnitType unitType) {
        if (unitType.isAvailableTo(owner)) {
            this.unitType = unitType;
            naval = unitType.hasAbility("model.ability.navalUnit");
            if (getMovesLeft() > getInitialMovesLeft()) {
                setMovesLeft(getInitialMovesLeft());
            }
            hitpoints = unitType.getHitPoints();
        } else {
            // ColonialRegulars only available after independence is declared
            logger.warning(unitType.getName() + " is not available to " + owner.getPlayerType() +
                           " player " + owner.getName());
        }

    }

    /**
     * Returns the amount of moves this Unit has left.
     * 
     * @return The amount of moves this Unit has left. If the
     *         <code>unit.isUnderRepair()</code> then <code>0</code> is
     *         always returned.
     */
    public int getMovesLeft() {
        return movesLeft;
    }

    // TODO: make these go away, if possible, private if not
    public boolean isArmed() {
        return equipment.contains(FreeCol.getSpecification().getEquipmentType("model.equipment.muskets"));
    }

    public boolean isMounted() {
        return equipment.contains(FreeCol.getSpecification().getEquipmentType("model.equipment.horses"));
    }

    /**
     * Returns the name of a unit in a human readable format. The return value
     * can be used when communicating with the user.
     * 
     * @return The given unit type as a String
     */
    public String getName() {
        if (name != null) {
            return name;
        } else if (canCarryTreasure()) {
            return Messages.message(getType().getId() + ".gold", "%gold%",
                                    String.valueOf(getTreasureAmount()));
        } else {
            return getName(getType(), getRole());
        }
    }

    /**
     * Returns the name of a unit in a human readable format. The return value
     * can be used when communicating with the user.
     * 
     * @param someType an <code>UnitType</code> value
     * @param someRole a <code>Role</code> value
     * @return The given unit type as a String
     */
    public static String getName(UnitType someType, Role someRole) {
        String key = someRole.toString().toLowerCase();
        if (someRole == Role.DEFAULT) {
            key = "name";
        }
        String messageID = someType.getId() +  "." + key;
        if (Messages.containsKey(messageID)) {
            return Messages.message(messageID);
        } else {
            return Messages.message("model.unit." + key + ".name", "%unit%", someType.getName());
        }
    }

    /**
     * Set the <code>Name</code> value.
     *
     * @param newName The new Name value.
     */
    public void setName(String newName) {
        if (name != null && newName.equals("")) {
            this.name = null;
        } else {
            this.name = newName;
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
     * Sends this <code>Unit</code> to the closest <code>Location</code> it
     * can get repaired.
     */
    public void sendToRepairLocation() {
        Location l = getOwner().getRepairLocation(this);
        setLocation(l);
        setState(UnitState.ACTIVE);
        setMovesLeft(0);
    }

    /**
     * Returns a String representation of this Unit.
     * 
     * @return A String representation of this Unit.
     */
    public String toString() {
        return getName() + " " + getMovesAsString();
    }

    public String getMovesAsString() {
        String moves = "";
        if (getMovesLeft() % 3 == 0 || getMovesLeft() / 3 > 0) {
            moves += Integer.toString(getMovesLeft() / 3);
        }

        if (getMovesLeft() % 3 != 0) {
            if (getMovesLeft() / 3 > 0) {
                moves += " ";
            }

            moves += "(" + Integer.toString(getMovesLeft() - (getMovesLeft() / 3) * 3) + "/3) ";
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
     * Get occupation indicator
     * 
     * @return The occupation indicator string
     */
    public String getOccupationIndicator() {

        if (getDestination() != null) {
        	if(getTradeRoute() != null)
        		return Messages.message("model.unit.occupation.inTradeRoute");
        	else
        		return Messages.message("model.unit.occupation.goingSomewhere");
        } else if (state == UnitState.IMPROVING && workImprovement != null) {
            return workImprovement.getOccupationString();
        } else if (state == UnitState.ACTIVE && getMovesLeft() == 0) {
        	if(isUnderRepair())
        		return Messages.message("model.unit.occupation.underRepair");
        	else
        		return Messages.message("model.unit.occupation.activeNoMovesLeft");
        } else {
            return Messages.message("model.unit.occupation." + state.toString().toLowerCase());
        }
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
        role = Role.DEFAULT;
        for (EquipmentType type : equipment) {
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
     *            FORTIFIED, ...}.
     */
    public void setState(UnitState s) {
        if (!checkSetState(s)) {
            throw new IllegalStateException("Illegal state: " + s);
        }

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
            getTile().takeOwnership(getOwner(), null);
            workLeft = -1;
            if (workImprovement != null) {
                workLeft = workImprovement.getTurnsToComplete();
            }
            state = s;
            doAssignedWork();
            return;
        case TO_EUROPE:
            if (state == UnitState.ACTIVE && !(location instanceof Europe)) {
                workLeft = TURNS_TO_SAIL;
            } else if ((state == UnitState.TO_AMERICA) && (location instanceof Europe)) {
                workLeft = TURNS_TO_SAIL + 1 - workLeft;
            }
            workLeft = (int) getOwner().getFeatureContainer()
                .applyModifier(workLeft,"model.modifier.sailHighSeas", unitType, getGame().getTurn());
            movesLeft = 0;
            break;
        case TO_AMERICA:
            if ((state == UnitState.ACTIVE) && (location instanceof Europe)) {
                workLeft = TURNS_TO_SAIL;
            } else if ((state == UnitState.TO_EUROPE) && (location instanceof Europe)) {
                workLeft = TURNS_TO_SAIL + 1 - workLeft;
            }
            workLeft = (int) getOwner().getFeatureContainer()
                .applyModifier(workLeft,"model.modifier.sailHighSeas", unitType, getGame().getTurn());
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

        List<Tile> surroundingTiles = getGame().getMap().getSurroundingTiles(getTile(), 1);
        if (surroundingTiles.size() != 8) {
            return true;
        } else {
            for (int i = 0; i < surroundingTiles.size(); i++) {
                Tile tile = surroundingTiles.get(i);
                if (tile == null || tile.getType().canSailToEurope()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Moves this unit to Europe.
     * 
     * @exception IllegalStateException If the move is illegal.
     */
    public void moveToEurope() {
        // Check if this move is illegal or not:
        if (!canMoveToEurope()) {
            throw new IllegalStateException("It is not allowed to move units to europe from the tile where this unit is located.");
        } else if (getLocation() instanceof Tile) {
            // Don't set entry location if location isn't Tile
            setEntryLocation(getLocation());
        }

        setState(UnitState.TO_EUROPE);
        setLocation(getOwner().getEurope());

        // Clear the alreadyOnHighSea flag:
        alreadyOnHighSea = false;
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

        // Clear the alreadyOnHighSea flag:
        alreadyOnHighSea = false;
    }

    /**
     * Checks whether this unit can create or contribute to a TileImprovement of this Type here
     * @return The result.
     */
    public boolean canPerformImprovement(TileImprovementType impType) {
        // Is this a valid ImprovementType?
        if (impType == null || impType.isNatural()) {
            return false;
        }
        // Check if improvement can be performed on this TileType
        if (!impType.isTileTypeAllowed(getTile().getType())) {
            return false;
        }
        // TODO: This does not check if the tile (not TileType accepts the improvement)
        
        // Check if there is an existing Improvement of this type
        TileImprovement improvement = getTile().findTileImprovementType(impType);
        if (improvement == null) {
            // No improvement found, check if worker can do it
            return impType.isWorkerAllowed(this);
        } else {
            // Has improvement, check if worker can contribute to it
            return improvement.isWorkerAllowed(this);
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
            return true;
        case IN_COLONY:
            return !isNaval();
        case FORTIFIED:
            return getState() == UnitState.FORTIFYING;
        case IMPROVING:
        case FORTIFYING:
        case SKIPPED:
            return (getMovesLeft() > 0);
        case SENTRY:
            return true;
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
     * Makes this unit build the specified colony.
     * 
     * @param colony The colony this unit shall build.
     */
    public void buildColony(Colony colony) {
        if (!canBuildColony()) {
            throw new IllegalStateException("Unit " + getName() + " can not build colony on " + getTile().getName() + "!");
        }
        if (!getTile().getPosition().equals(colony.getTile().getPosition())) {
            throw new IllegalStateException("A Unit can only build a colony if on the same tile as the colony");
        }

        getTile().setOwner(owner);
        getTile().setSettlement(colony);
        setLocation(colony);
        setMovesLeft(0);
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
                	// trade unit arrives in Europe
                	if(this.getTradeRoute() != null){
                		setMovesLeft(0);
                		setState(UnitState.ACTIVE);
                		return;
                	}
                	
                    addModelMessage(getOwner().getEurope(), ModelMessage.MessageType.DEFAULT, this,
                                    "model.unit.arriveInEurope",
                                    "%europe%", getOwner().getEurope().getName());
                    Iterator<Unit> iter = getUnitIterator();
                    while (iter.hasNext()) {
                        Unit u = iter.next();
                        if (u.canCarryTreasure()) {
                            u.cashInTreasureTrain();
                        }
                    }
                    setState(UnitState.ACTIVE);
                    break;
                case TO_AMERICA:
                    getGame().getModelController().setToVacantEntryLocation(this);
                    setState(UnitState.ACTIVE);
                    break;
                case FORTIFYING:
                    setState(UnitState.FORTIFIED);
                    break;
                case IMPROVING:
                    expendEquipment(getWorkImprovement().getExpendedEquipmentType(), 
                                    getWorkImprovement().getExpendedAmount());
                    // Deliver Goods if any
                    GoodsType deliverType = getWorkImprovement().getDeliverGoodsType();
                    if (deliverType != null) {
                        int deliverAmount = getTile().potential(deliverType) * getWorkImprovement().getDeliverAmount();
                        if (unitType.hasAbility("model.ability.expertPioneer")) {
                            deliverAmount *= 2;
                        }
                        if (getColony() != null && getColony().getOwner().equals(getOwner())) {
                            getColony().addGoods(deliverType, deliverAmount);
                        } else {
                            List<Tile> surroundingTiles = getTile().getMap().getSurroundingTiles(getTile(), 1);
                            List<Settlement> adjacentColonies = new ArrayList<Settlement>();
                            for (int i = 0; i < surroundingTiles.size(); i++) {
                                Tile t = surroundingTiles.get(i);
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
                    // Perform TileType change if any
                    TileType changeType = getWorkImprovement().getChange(getTile().getType());
                    if (changeType != null) {
                        getTile().setType(changeType);
                    }
                    // Finish up
                    setState(UnitState.ACTIVE);
                    setMovesLeft(0);
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
        for (int count = 0; count < amount; count++) {
            equipment.remove(type);
        }
        setRole();
        // TODO: make this more generic
        EquipmentType tools = FreeCol.getSpecification().getEquipmentType("model.equipment.tools");
        if (!equipment.contains(tools)) {
            addModelMessage(this, ModelMessage.MessageType.WARNING, this,
                            Messages.getKey(getId() + ".noMoreTools", 
                                            "model.unit.noMoreTools"),
                            "%unit%", getName());
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
     * Checks if this is an offensive unit.
     * 
     * @return <code>true</code> if this is an offensive unit meaning it can
     *         attack other units.
     */
    public boolean isOffensiveUnit() {
        return unitType.getOffence() > 0 || isArmed() || isMounted();
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
        return (unitType.getDefence() > 1 || isArmed() || isMounted()) && !isNaval();
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
        String oldName = getName();
        UnitType learning = getUnitTypeTeaching(getTeacher().unitType, unitType);

        if (learning != null) {
            setType(learning);
        }

        String newName = getName();
        if (!newName.equals(oldName)) {
            Colony colony = getTile().getColony();
            addModelMessage(colony, ModelMessage.MessageType.UNIT_IMPROVED, this,
                            "model.unit.unitEducated",
                            "%oldName%", oldName,
                            "%unit%", newName,
                            "%colony%", colony.getName());
        }
    }

    /**
     * Adjusts the tension and alarm levels of the enemy unit's owner according
     * to the type of attack.
     * 
     * @param enemyUnit The unit we are attacking.
     */
    // TODO: move to AI code
    public void adjustTension(Unit enemyUnit) {
        Player myPlayer = getOwner();
        Player enemy = enemyUnit.getOwner();
        myPlayer.modifyTension(enemy, -Tension.TENSION_ADD_MINOR);
        if (getIndianSettlement() != null) {
            getIndianSettlement().modifyAlarm(enemy, -Tension.TENSION_ADD_UNIT_DESTROYED / 2);
        }

        // Increases the enemy's tension levels:
        if (enemy.isAI()) {
            Settlement settlement = enemyUnit.getTile().getSettlement();
            if (settlement != null) {
                // we are attacking an indian settlement - let propagation take care of the effects on the tribe
                if (settlement instanceof IndianSettlement) {
                    IndianSettlement indianSettlement = (IndianSettlement) settlement;
                    if (indianSettlement.isCapital()){
                        indianSettlement.modifyAlarm(myPlayer, Tension.TENSION_ADD_CAPITAL_ATTACKED);
                    } else {
                        indianSettlement.modifyAlarm(myPlayer, Tension.TENSION_ADD_SETTLEMENT_ATTACKED);
                    }
                } else { // we are attacking an european settlement
                    enemy.modifyTension(myPlayer, Tension.TENSION_ADD_NORMAL);
                }
            } else {
                // we are attacking an enemy unit in the open
                // only one effect - at the home town if there's one or directly to the enemy nation
                IndianSettlement homeTown = enemyUnit.getIndianSettlement();
                if (homeTown != null) {
                    homeTown.modifyAlarm(myPlayer, Tension.TENSION_ADD_UNIT_DESTROYED);
                } else {
                    enemy.modifyTension(myPlayer, Tension.TENSION_ADD_MINOR);
                }
            }
        }
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
     * Captures the goods on board of the enemy unit.
     * 
     * @param enemyUnit The unit we are attacking.
     */
    public void captureGoods(Unit enemyUnit) {
        if (!canCaptureGoods()) {
            return;
        }
        // can capture goods; regardless attacking/defending
        Iterator<Goods> iter = enemyUnit.getGoodsIterator();
        while (iter.hasNext() && getSpaceLeft() > 0) {
            // TODO: show CaptureGoodsDialog if there's not enough
            // room for everything.
            Goods g = iter.next();

            // MESSY, but will mess up the iterator if we do this
            // besides, this gets cleared out later
            // enemy.getGoodsContainer().removeGoods(g);
            getGoodsContainer().addGoods(g);
        }
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
     * Clear the speciality of a <code>Unit</code> changing the
     * UnitType to the UnitType specified for clearing.
     */
    public void clearSpeciality() {
        UnitType newType = unitType.getDowngrade(DowngradeType.CLEAR_SKILL);
        if (newType != null) {
            setType(newType);
        }
    }

    /** 
     * Given a type of goods to produce in the field and a tile,
     * returns the unit's potential to produce goods.
     * 
     * @param goodsType The type of goods to be produced.
     * @param base an <code>int</code> value
     * @return The potential amount of goods to be farmed.
     */
    public int getProductionOf(GoodsType goodsType, int base) {
        if (base == 0) {
            return 0;
        } else {
            return Math.round(FeatureContainer.applyModifierSet(base, getGame().getTurn(),
                                                                getModifierSet(goodsType.getId())));
        }
    }

    /**
     * Disposes all Units aboard this one.
     *
     */
    public void disposeAllUnits() {
        // Copy the list first, as the Unit will try to remove itself
        // from its location.
        for (Unit unit : new ArrayList<Unit>(units)) {
            unit.dispose();
        }
    }

    /**
     * Removes all references to this object.
     */
    public void dispose() {
        disposeAllUnits();
        if (unitType.canCarryGoods()) {
            goodsContainer.dispose();
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

        super.dispose();
    }

    /**
     * Checks if a colonist can get promoted by experience.
     */
    private void checkExperiencePromotion() {
        GoodsType goodsType = getWorkType();
        if (goodsType == null) {
            return;
        }
        
        UnitType learnType = FreeCol.getSpecification().getExpertForProducing(goodsType);
        if (learnType != null && unitType.canBeUpgraded(learnType, UpgradeType.EXPERIENCE)) {
            logger.finest("About to call getRandom for experience");
            int random = getGame().getModelController().getRandom(getId() + "experience", 5000);
            if (random < Math.min(experience, 200)) {
                logger.finest("About to change type of unit due to experience.");
                String oldName = getName();
                setType(learnType);
                addModelMessage(getColony(), ModelMessage.MessageType.UNIT_IMPROVED, this,
                                "model.unit.experience",
                                "%oldName%", oldName,
                                "%unit%", getName(),
                                "%colony%", getColony().getName());
            }
        }
    }

    /**
     * Prepares the <code>Unit</code> for a new turn.
     */
    public void newTurn() {
        if (isUninitialized()) {
            logger.warning("Calling newTurn for an uninitialized object: " + getId());
            return;
        }
        checkExperiencePromotion();
        if (isUnderRepair()) {
            movesLeft = 0;
        } else {
            movesLeft = getInitialMovesLeft();
        }
        doAssignedWork();
        if (getState() == UnitState.SKIPPED) {
            setState(UnitState.ACTIVE);
        }
        if (location instanceof Tile && ((Tile) location).getSettlement() == null) {
            attrition++;
        } else {
            attrition = 0;
        }
        if (attrition > getType().getMaximumAttrition()) {
            addModelMessage(this, ModelMessage.MessageType.UNIT_LOST, getType(),
                            "model.unit.attrition", "%unit%", getName());
            dispose();
        }
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
        out.writeAttribute("workLeft", Integer.toString(workLeft));
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
        if (getGame().isClientTrusted() || showAll || getOwner().equals(player)
            || !getGameOptions().getBoolean(GameOptions.UNIT_HIDING) && player.canSee(getTile())) {
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
            out.writeStartElement(EQUIPMENT_TAG);
            out.writeAttribute(ARRAY_SIZE, Integer.toString(equipment.size()));
            int index = 0;
            for (EquipmentType type : equipment) {
                out.writeAttribute("x" + Integer.toString(index), type.getId());
                index++;
            }
            out.writeEndElement();
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
        unitType = FreeCol.getSpecification().getUnitType(in.getAttributeValue(null, "unitType"));
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

        workType = FreeCol.getSpecification().getType(in, "workType", GoodsType.class, null);
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
        }

        equipment.clear();

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
                int length = Integer.parseInt(in.getAttributeValue(null, ARRAY_SIZE));
                for (int index = 0; index < length; index++) {
                    String equipmentId = in.getAttributeValue(null, "x" + String.valueOf(index));
                    equipment.add(FreeCol.getSpecification().getEquipmentType(equipmentId));
                }
                in.nextTag();
            } else if (in.getLocalName().equals(TileImprovement.getXMLElementTagName())) {
                workImprovement = updateFreeColGameObject(in, TileImprovement.class);
            }
        }
        
        if (goodsContainer == null && canCarryGoods()) {
            logger.warning("Carrier did not have a \"goodsContainer\"-tag.");
            goodsContainer = new GoodsContainer(getGame(), this);
        }

        getOwner().invalidateCanSeeTiles();
    }

    /**
     * Gets the tag name of the root element representing this object.
     * 
     * @return "unit.
     */
    public static String getXMLElementTagName() {
        return "unit";
    }

    private Location newLocation(Game game, String locationString) {
        Location loc = null;
        if (locationString.startsWith(Tile.getXMLElementTagName())) {
            loc = new Tile(game, locationString);
        } else if (locationString.startsWith(Colony.getXMLElementTagName())) {
            loc = new Colony(game, locationString);
        } else if (locationString.startsWith(IndianSettlement.getXMLElementTagName())) {
            loc = new IndianSettlement(game, locationString);
        } else if (locationString.startsWith(Europe.getXMLElementTagName())) {
            loc = new Europe(game, locationString);
        } else if (locationString.startsWith(ColonyTile.getXMLElementTagName())) {
            loc = new ColonyTile(game, locationString);
        } else if (locationString.startsWith(Building.getXMLElementTagName())) {
            loc = new Building(game, locationString);
        } else if (locationString.startsWith(Unit.getXMLElementTagName())) {
            loc = new Unit(game, locationString);
        } else {
            logger.warning("Unknown type of Location: " + locationString);
            loc = new Tile(game, locationString);
        }
        return loc;
    }

    /**
     * Returns true if this unit has already been moved onto high seas but not
     * to europe.
     * 
     * @return true if the unit has already been moved onto high seas
     */
    public boolean isAlreadyOnHighSea() {
        return alreadyOnHighSea;
    }

    /**
     * Tells unit that it has just entered the high seas but instead of going to
     * europe, it stays on the current side of the atlantic.
     * 
     * @param alreadyOnHighSea
     */
    public void setAlreadyOnHighSea(boolean alreadyOnHighSea) {
        this.alreadyOnHighSea = alreadyOnHighSea;
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
     * Return the type of the image which will be used to draw the path
     *
     * @return a <code>String</code> to form the resource key
     */
    public String getPathTypeImage() {
        if (isMounted()) {
            return "horse";
        } else {
            return unitType.getPathImage();
        }
    }

}
