package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.model.TradeRoute.Stop;
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
public class Unit extends FreeColGameObject implements Abilities, Locatable, Location, Ownable, Nameable {

    public static final String COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    private static final Logger logger = Logger.getLogger(Unit.class.getName());

    /**
     * The type of a unit; used only for gameplaying purposes NOT painting
     * purposes.
     */
    public static final int FREE_COLONIST = 0, EXPERT_FARMER = 1, EXPERT_FISHERMAN = 2, EXPERT_FUR_TRAPPER = 3,
            EXPERT_SILVER_MINER = 4, EXPERT_LUMBER_JACK = 5, EXPERT_ORE_MINER = 6, MASTER_SUGAR_PLANTER = 7,
            MASTER_COTTON_PLANTER = 8, MASTER_TOBACCO_PLANTER = 9, FIREBRAND_PREACHER = 10, ELDER_STATESMAN = 11,
            MASTER_CARPENTER = 12, MASTER_DISTILLER = 13, MASTER_WEAVER = 14, MASTER_TOBACCONIST = 15,
            MASTER_FUR_TRADER = 16, MASTER_BLACKSMITH = 17, MASTER_GUNSMITH = 18, SEASONED_SCOUT = 19,
            HARDY_PIONEER = 20, VETERAN_SOLDIER = 21, JESUIT_MISSIONARY = 22, INDENTURED_SERVANT = 23,
            PETTY_CRIMINAL = 24, INDIAN_CONVERT = 25, BRAVE = 26, COLONIAL_REGULAR = 27, KINGS_REGULAR = 28,
            CARAVEL = 29, FRIGATE = 30, GALLEON = 31, MAN_O_WAR = 32, MERCHANTMAN = 33, PRIVATEER = 34, ARTILLERY = 35,
            DAMAGED_ARTILLERY = 36, TREASURE_TRAIN = 37, WAGON_TRAIN = 38, MILKMAID = 39, REVENGER = 40,
            FLYING_DUTCHMAN = 41, UNDEAD = 42, UNIT_COUNT = 43;

    /** A state a Unit can have. */
    public static final int ACTIVE = 0, FORTIFIED = 1, SENTRY = 2, IN_COLONY = 3, /*PLOW = 4, BUILD_ROAD = 5,*/
            IMPROVING = 4,      // All TileImprovements use state of IMPROVING
            TO_EUROPE = 6, IN_EUROPE = 7, TO_AMERICA = 8, FORTIFYING = 9, NUMBER_OF_STATES = 10;

    /**
     * A move type.
     * 
     * @see #getMoveType(int direction)
     */
    public static final int MOVE = 0, MOVE_HIGH_SEAS = 1, ATTACK = 2, EMBARK = 3, DISEMBARK = 4,
            ENTER_INDIAN_VILLAGE_WITH_FREE_COLONIST = 5, ENTER_INDIAN_VILLAGE_WITH_SCOUT = 6,
            ENTER_INDIAN_VILLAGE_WITH_MISSIONARY = 7, ENTER_FOREIGN_COLONY_WITH_SCOUT = 8,
            ENTER_SETTLEMENT_WITH_CARRIER_AND_GOODS = 9, EXPLORE_LOST_CITY_RUMOUR = 10, ILLEGAL_MOVE = 11;

    public static final int ATTACK_GREAT_LOSS = -2, ATTACK_LOSS = -1, ATTACK_EVADES = 0, ATTACK_WIN = 1,
            ATTACK_GREAT_WIN = 2, ATTACK_DONE_SETTLEMENT = 3; // The last defender of the settlement has died.

    public static final int MUSKETS_TO_ARM_INDIAN = 25, HORSES_TO_MOUNT_INDIAN = 25;

    private int type;

    private UnitType unitType;

    private boolean naval;

    private boolean armed, mounted, missionary;

    private int movesLeft; // Always use getMovesLeft()

    private int state;

    private int workLeft; // expressed in number of turns, '-1' if a Unit can

    // stay in its state forever
    private int numberOfTools;

    private int hitpoints; // For now; only used by ships when repairing.

    private Player owner;

    private UnitContainer unitContainer;

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

    /** The individual name of this unit, not of the unit type. */
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
     * Describe student here.
     */
    private Unit student;

    /**
     * Describe teacher here.
     */
    private Unit teacher;


    /**
     * Initiate a new <code>Unit</code> of a specified type with the state set
     * to {@link #ACTIVE} if a carrier and {@link #SENTRY} otherwise. The
     * {@link Location} is set to <i>null</i>.
     * 
     * @param game The <code>Game</code> in which this <code>Unit</code>
     *            belong.
     * @param owner The Player owning the unit.
     * @param type The type of the unit.
     */
    public Unit(Game game, Player owner, UnitType type) {
        this(game, null, owner, type, ACTIVE);
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
     * @param s The initial state for this Unit (one of {@link #ACTIVE},
     *            {@link #FORTIFIED}...).
     */
    public Unit(Game game, Location location, Player owner, UnitType type, int s) {
        this(game, location, owner, type, s,
                type.hasAbility("model.ability.expertSoldier"),
                type.hasAbility("model.ability.expertScout"),
                type.hasAbility("model.ability.expertPioneer") ? 100 : 0,
                type.hasAbility("model.ability.expertMissionary"));
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
     * @param s The initial state for this Unit (one of {@link #ACTIVE},
     *            {@link #FORTIFIED}...).
     * @param armed Determines wether the unit should be armed or not.
     * @param mounted Determines wether the unit should be mounted or not.
     * @param numberOfTools The number of tools the unit will be carrying.
     * @param missionary Determines wether this unit should be dressed like a
     *            missionary or not.
     */
    public Unit(Game game, Location location, Player owner, UnitType type, int s, boolean armed, boolean mounted,
            int numberOfTools, boolean missionary) {
        super(game);

        visibleGoodsCount = -1;
        unitContainer = new UnitContainer(game, this);
        goodsContainer = new GoodsContainer(game, this);

        this.owner = owner;
        this.type = type.getIndex();
        unitType = type;
        naval = unitType.hasAbility("model.ability.navalUnit");
        this.armed = armed;
        this.mounted = mounted;
        this.numberOfTools = numberOfTools;
        this.missionary = missionary;

        setLocation(location);

        state = s;
        workLeft = -1;
        workType = Goods.FOOD;

        this.movesLeft = getInitialMovesLeft();
        hitpoints = getInitialHitpoints(getUnitType());

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
    public final UnitType getUnitType() {
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
        getOwner().modifySales(goods.getType().getIndex(), goods.getAmount());
        getOwner().modifyIncomeBeforeTaxes(goods.getType().getIndex(), gold);
        getOwner().modifyIncomeAfterTaxes(goods.getType().getIndex(), gold);

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
            addModelMessage(settlement, "model.unit.gift", new String[][] {
                    { "%player%", getOwner().getNationAsString() }, { "%type%", goods.getName() },
                    { "%amount%", Integer.toString(amount) }, { "%colony%", ((Colony) settlement).getName() } },
                    ModelMessage.GIFT_GOODS, new Goods(goods.getType()));
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
                int transportFee = getTreasureAmount() / 2;
                Modifier modifier = getOwner().getModifier("model.modifier.treasureTransportFee");
                if (modifier != null) {
                    return (int) modifier.applyTo(transportFee);
                }
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
            FreeColGameObject o = getOwner();
            if (isInEurope()) {
                o = getOwner().getEurope();
            }
            getOwner().modifyGold(cashInAmount);
            addModelMessage(o, "model.unit.cashInTreasureTrain", new String[][] {
                    { "%amount%", Integer.toString(getTreasureAmount()) },
                    { "%cashInAmount%", Integer.toString(cashInAmount) } }, ModelMessage.DEFAULT);
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
            return getNeededTurnsOfTraining(getUnitType(), student.getUnitType());
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
     * Gets the UnitType which is teaching a teacher to a student
     * This value is only meaningful for teachers that can be put in a school.
     * 
     * @return The turns of training needed to teach its current type to a free
     *         colonist or to promote an indentured servant or a petty criminal.
     * @see #getTurnsOfTraining
     *
     * @param typeTeacher the unit type of the teacher
     * @param typeStudent the unit type of the student
     * @return an <code>UnitType</code> value
     */
    public static UnitType getUnitTypeTeaching(UnitType typeTeacher, UnitType typeStudent) {
        if (typeStudent.canBeTaught(typeTeacher)) {
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
        return getSkillLevel(getUnitType());
    }

    /**
     * Gets the skill level of the given type of <code>Unit</code>.
     * 
     * @param unitTypeIndex The type of <code>Unit</code>.
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
     * Returns true if the Unit has the ability identified by
     * <code>id</code.
     *
     * @param id a <code>String</code> value
     * @return a <code>boolean</code> value
     */
    public boolean hasAbility(String id) {
        // TODO: implement role and unit abilities
        return unitType.hasAbility(id);
    }

    /**
     * Sets the ability identified by <code>id</code.
     *
     * @param id a <code>String</code> value
     * @param newValue a <code>boolean</code> value
     */
    public void setAbility(String id, boolean newValue) {
        // TODO: implement unit abilities
    }



    /**
     * Returns true if this unit can be a student.
     *
     * @param teacher the teacher which is trying to teach it
     * @return a <code>boolean</code> value
     */
    public boolean canBeStudent(Unit teacher) {
        return canBeStudent(getUnitType(), teacher.getUnitType());
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
     * @returns The <code>TileImprovement</code>
     */
    public TileImprovement getWorkImprovement() {
        return workImprovement;
    }

    /**
     * Sets the TileImprovement that this pioneer is contributing to.
     * @returns The <code>TileImprovement</code>
     */
    public void setWorkImprovement(TileImprovement imp) {
        workImprovement = imp;
    }

    /**
     * Gets the type of goods this unit is an expert at producing.
     * 
     * @return The type of goods or <code>-1</code> if this unit is not an
     *         expert at producing any type of goods.
     * @see ColonyTile#getExpertForProducing
     */
    public GoodsType getExpertWorkType() {
        return unitType.getExpertProduction();
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
     * @see Map#findPath(Tile, Tile, int)
     * @see Map#findPath(Unit, Tile , Tile)
     * @exception NullPointerException if <code>end == null</code>
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
                p = getGame().getMap().findPath((Tile) getOwner().getEntryLocation(), destination.getTile(), Map.BOTH_LAND_AND_SEA);
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
        return (hasAbility("model.ability.carryGoods") &&
                goodsContainer.getGoodsCount() > 0 &&
                getOwner().getStance(settlement.getOwner()) != Player.WAR &&
                ((settlement instanceof IndianSettlement) ||
                 getOwner().hasAbility("model.ability.tradeWithForeignColonies")));
    }

    /**
     * Gets the type of a move made in a specified direction.
     * 
     * @param direction The direction of the move.
     * @return The move type. Notice: <code>Unit.ILLEGAL_MOVE</code> when
     *         there are no moves left.
     */
    public int getMoveType(int direction) {
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
    public int getMoveType(Tile target) {
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
    public int getMoveType(Tile from, Tile target, int ml) {
        if (from == null) {
            throw new IllegalStateException("from == null");
        } else if (isUnderRepair()) {
            return ILLEGAL_MOVE;
        } else if (ml <= 0) {
            return ILLEGAL_MOVE;
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
     * @return The move type. Notice: <code>Unit.ILLEGAL_MOVE</code> when
     *         there are no moves left.
     */
    private int getNavalMoveType(Tile from, Tile target, int ml) {
        if (target == null) { // TODO: do not allow "MOVE_HIGH_SEAS" north and
            // south.
            if (getOwner().canMoveToEurope()) {
                return MOVE_HIGH_SEAS;
            } else {
                return ILLEGAL_MOVE;
            }
        } else if (target.isLand()) {
            Settlement settlement = target.getSettlement();
            if (settlement != null) {
                if (settlement.getOwner() == getOwner()) {
                    return MOVE;
                } else if (canTradeWith(settlement)) {
                    return ENTER_SETTLEMENT_WITH_CARRIER_AND_GOODS;
                } else {
                    logger.fine("Trying to enter another player's settlement with " + getName());
                    return ILLEGAL_MOVE;
                }
            } else if (target.getDefendingUnit(this) != null && target.getDefendingUnit(this).getOwner() != getOwner()) {
                logger.fine("Trying to sail into tile occupied by enemy units with " + getName());
                return ILLEGAL_MOVE;
            } else {
                // Check for disembark.
                Iterator<Unit> unitIterator = getUnitIterator();

                while (unitIterator.hasNext()) {
                    Unit u = unitIterator.next();
                    if (u.getMovesLeft() > 0) {
                        return DISEMBARK;
                    }
                }
                logger.fine("No units to disembark from " + getName());
                return ILLEGAL_MOVE;
            }
        } else if (target.getDefendingUnit(this) != null && target.getDefendingUnit(this).getOwner() != getOwner()) {
            // enemy units at sea
            if (isOffensiveUnit()) {
                return ATTACK;
            } else {
                return ILLEGAL_MOVE;
            }
        } else if (target.getType().canSailToEurope()) {
            if (getOwner().canMoveToEurope()) {
                return MOVE_HIGH_SEAS;
            } else {
                return MOVE;
            }
        } else {
            // this must be ocean
            return MOVE;
        }
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
    private int getLandMoveType(Tile from, Tile target, int ml) {
        if (target == null) {
            // only naval units are allowed to do this
            logger.fine("Trying to enter null tile with land unit " + getName());
            return ILLEGAL_MOVE;
        }

        if (target.isLand()) {
            Settlement settlement = target.getSettlement();
            if (settlement != null) {
                if (settlement.getOwner() == getOwner()) {
                    // our colony
                    return MOVE;
                } else if (canTradeWith(settlement)) {
                    return ENTER_SETTLEMENT_WITH_CARRIER_AND_GOODS;
                } else if (!from.isLand()) {
                    /*
                     * An enemy should be able to attack a colony on a 1x1
                     * island. Therefor: All attacks on colonies from ships
                     * are allowed. This behavior should be discussed on the
                     * developer's mailing list before being changed.
                     */
                    if (isOffensiveUnit()) {
                        return ATTACK;
                    } else {
                        return ILLEGAL_MOVE;
                    }
                } else if (settlement instanceof IndianSettlement) {
                    IndianSettlement indian = (IndianSettlement) settlement;
                    if (isScout()) {
                        return ENTER_INDIAN_VILLAGE_WITH_SCOUT;
                    } else if (isMissionary()) {
                        return ENTER_INDIAN_VILLAGE_WITH_MISSIONARY;
                    } else if (isOffensiveUnit()) {
                        return ATTACK;
                    } else if (indian.getLearnableSkill() != null || !indian.hasBeenVisited()) {
                        return ENTER_INDIAN_VILLAGE_WITH_FREE_COLONIST;
                    } else {
                        return ILLEGAL_MOVE;
                    }
                } else if (settlement instanceof Colony) {
                    if (isScout()) {
                        return ENTER_FOREIGN_COLONY_WITH_SCOUT;
                    } else if (isOffensiveUnit()) {
                        return ATTACK;
                    } else {
                        logger.fine("Trying to enter foreign colony with " + getName());
                        return ILLEGAL_MOVE;
                    }
                }
            } else if (target.getDefendingUnit(this) != null && target.getDefendingUnit(this).getOwner() != getOwner()) {
                if (from.isLand()) {
                    if (isOffensiveUnit()) {
                        return ATTACK;
                    } else {
                        logger.fine("Trying to attack with civilian " + getName());
                        return ILLEGAL_MOVE;
                    }
                } else {
                    /*
                     * Note that attacking a settlement from a ship is allowed.
                     */
                    logger.fine("Attempting marine assault with " + getName());
                    return ILLEGAL_MOVE;
                }
            } else if (target.getFirstUnit() != null && target.getFirstUnit().isNaval()
                    && target.getFirstUnit().getOwner() != getOwner()) {
                // An enemy ship in land tile without a settlement
                return ILLEGAL_MOVE;
            } else if (getMoveCost(from, target, ml) > ml) {
                return ILLEGAL_MOVE;
            } else if (target.hasLostCityRumour()) {
                return EXPLORE_LOST_CITY_RUMOUR;
            } else {
                return MOVE;
            }
        } else {
            // check for embarkation
            if (target.getFirstUnit() == null || target.getFirstUnit().getNation() != getNation()) {
                logger.fine("Trying to embark on tile occupied by foreign units with " + getName());
                return ILLEGAL_MOVE;
            } else {
                for (Unit u : target.getUnitList()) {
                    if (u.getSpaceLeft() >= getTakeSpace()) {
                        return EMBARK;
                    }
                }
                logger.fine("Trying to board full vessel with " + getName());
                return ILLEGAL_MOVE;
            }
        }

        logger.info("Default illegal move for " + getName());
        return ILLEGAL_MOVE;
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
    public int getTakeSpace() {
        return unitType.getSpaceTaken();
    }

    /**
     * Gets the line of sight of this <code>Unit</code>. That is the distance
     * this <code>Unit</code> can spot new tiles, enemy unit e.t.c.
     * 
     * @return The line of sight of this <code>Unit</code>.
     */
    public int getLineOfSight() {
        int line = unitType.getLineOfSight();
        if (isScout()) {
            line = 2;
        }
        if (!isNaval() && getOwner().hasFather(FreeCol.getSpecification().getFoundingFather("model.foundingFather.hernandoDeSoto"))) {
            line++;
        }
        return line;
    }

    /**
     * Checks if this <code>Unit</code> is a scout.
     * 
     * @return <i>true</i> if this <code>Unit</code> is a scout and <i>false</i>
     *         otherwise.
     */
    public boolean isScout() {

        return isMounted() && !isArmed();
    }

    /**
     * Moves this unit in the specified direction.
     * 
     * @param direction The direction
     * @see #getMoveType(int)
     * @exception IllegalStateException If the move is illegal.
     */
    public void move(int direction) {
        int moveType = getMoveType(direction);

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
     * Move to a given tile. Unlike {@link #move(int)} no validation is done, so
     * this method may be called from the opponentMove-handler.
     * 
     * @param newTile The new tile.
     */
    public void moveToTile(Tile newTile) {
        if (newTile != null) {
            setState(ACTIVE);
            setStateToAllChildren(SENTRY);
            int moveCost = getMoveCost(newTile);
            setLocation(newTile);
            activeAdjacentSentryUnits(newTile);
            setMovesLeft(getMovesLeft() - moveCost);

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
                if (unit.getState() == Unit.SENTRY && unit.getOwner() != getOwner()) {
                    unit.setState(Unit.ACTIVE);
                }
            }
        }
    }

    /**
     * Embarks this unit onto the specified unit.
     * 
     * @param unit The unit to embark onto.
     * @exception IllegalStateException If the embark is illegal.
     *                NullPointerException If <code>unit == null</code>.
     */
    public void embark(Unit unit) {
        if (unit == null) {
            throw new NullPointerException();
        }

        if (getMoveType(unit.getTile()) != EMBARK) {
            throw new IllegalStateException("Illegal disembark requested!");
        }

        setLocation(unit);
        setMovesLeft(getMovesLeft() - 3);
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
            setState(SENTRY);
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

        setState(ACTIVE);
    }

    /**
     * Checks wether or not the unit can unload the cargo
     * 
     * @return The result.
     */
    public boolean canUnload() {
        Location l = getLocation();
        if (isInEurope()) {
            return true;
        } else if (getTile() == null) {
            // this should not happen, but it does
            return false;
        } else if (getColony() != null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Sets the given state to all the units that si beeing carried.
     * 
     * @param state The state.
     */
    public void setStateToAllChildren(int state) {
        if (hasAbility("model.ability.carryUnits")) {
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
        if (locatable instanceof Unit && hasAbility("model.ability.carryUnits")) {
            if (getSpaceLeft() <= 0) {
                throw new IllegalStateException();
            }

            unitContainer.addUnit((Unit) locatable);
            spendAllMoves();
        } else if (locatable instanceof Goods && hasAbility("model.ability.carryGoods")) {
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
        if (locatable instanceof Unit && hasAbility("model.ability.carryUnits")) {
            unitContainer.removeUnit((Unit) locatable);
            spendAllMoves();
        } else if (locatable instanceof Goods && hasAbility("model.ability.carryGoods")) {
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
        if (locatable instanceof Unit && hasAbility("model.ability.carryUnits")) {
            return unitContainer.contains((Unit) locatable);
        } else if (locatable instanceof Goods && hasAbility("model.ability.carryGoods")) {
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
        if (locatable instanceof Unit && !((Unit) locatable).isCarrier() &&
                hasAbility("model.ability.carryUnits")) {
            return getSpaceLeft() >= locatable.getTakeSpace();
        } else if (locatable instanceof Goods && hasAbility("model.ability.carryGoods")) {
            Goods g = (Goods) locatable;
            return getSpaceLeft() > 0
                || (getGoodsContainer().getGoodsCount(g.getType()) % 100 != 0 && getGoodsContainer()
                    .getGoodsCount(g.getType())
                    % 100 + g.getAmount() <= 100);
        } else {
            return false;
        }
    }

    /**
     * Gets the amount of Units at this Location.
     * 
     * @return The amount of Units at this Location.
     */
    public int getUnitCount() {
        return hasAbility("model.ability.carryUnits") ? unitContainer.getUnitCount() : 0;
    }

    /**
     * Gets the first <code>Unit</code> beeing carried by this
     * <code>Unit</code>.
     * 
     * @return The <code>Unit</code>.
     */
    public Unit getFirstUnit() {
        return hasAbility("model.ability.carryUnits") ? unitContainer.getFirstUnit() : null;
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
     * Gets the last <code>Unit</code> beeing carried by this
     * <code>Unit</code>.
     * 
     * @return The <code>Unit</code>.
     */
    public Unit getLastUnit() {
        return hasAbility("model.ability.carryUnits") ? unitContainer.getLastUnit() : null;
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

    public List<Unit> getUnitList() {
        if (hasAbility("model.ability.carryUnits")) {
            return unitContainer.getUnitsClone();
        } else {
            return new ArrayList<Unit>();
        }
    }

    /**
     * Gets a <code>Iterator</code> of every <code>Unit</code> directly
     * located on this <code>Location</code>.
     * 
     * @return The <code>Iterator</code>.
     */
    public Iterator<Goods> getGoodsIterator() {
        if (hasAbility("model.ability.carryGoods")) {
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
        if (hasAbility("model.ability.carryGoods")) {
            return goodsContainer.getGoods();
        } else {
            return Collections.emptyList();
        }
    }

    public GoodsContainer getGoodsContainer() {
        return goodsContainer;
    }

    public UnitContainer getUnitContainer() {
        return unitContainer;
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

        if (armed)
            setArmed(false);
        if (mounted)
            setMounted(false);
        if (isPioneer())
            setNumberOfTools(0);

        setState(Unit.IN_COLONY);

        setLocation(workLocation);
    }

    /**
     * Sets this <code>Unit</code> to work at this <code>TileImprovement</code>
     * @param impType The <code>TileImprovement</code> that this Unit will work at.
     * @exception IllegalStateException If the <code>TileImprovement</code> is on
     *                another {@link Tile} than this <code>Unit</code> or is not
     *                a valid pioneer.
     */
    public void work(TileImprovement improvement) {
        if (improvement.getTile() != getTile()) {
            throw new IllegalStateException("Can only set a 'Unit' to work at a 'TileImprovement' that is on the same 'Tile'.");
        }
        if (!isPioneer()) {
            throw new IllegalStateException("Only 'Pioneers' can perform TileImprovement.");
        }

        setWorkImprovement(improvement);
        setState(Unit.IMPROVING);
        // No need to set Location, stay at the tile it is on.
    }

    /**
     * Sets the location of this Unit.
     * 
     * @param newLocation The new Location of the Unit.
     */
    public void setLocation(Location newLocation) {
        if (location != newLocation) {
            experience = 0;
        }
        
        Colony oldColony = this.getColony();
        
        if (location != null) {
            location.remove(this);
        }

        location = newLocation;

        if (location != null) {
            location.add(this);
        }

        Colony newColony = this.getColony();
        
        // Reset training when changing/leaving colony
        if (!Utils.equals(oldColony, newColony)){
            setTurnsOfTraining(0);
        }

        if (newLocation instanceof WorkLocation) {
            if (teacher == null) {
                for (Unit teacher : getColony().getTeachers()) {
                    if (teacher.getStudent() == null && canBeStudent(teacher)) {
                        teacher.setStudent(this);
                        this.setTeacher(teacher);
                        break;
                    }
                }
            }
        } else {
            if (teacher != null) {
                teacher.setStudent(null);
                teacher = null;
            }
            if (student != null) {
                student.setTeacher(null);
                student = null;
            }
        }
        
        // Check for adjacent units owned by a player that our owner has not met
        // before:
        if (getGame().getMap() != null && location != null && location instanceof Tile && !isNaval()) {
            Iterator<Position> tileIterator = getGame().getMap().getAdjacentIterator(getTile().getPosition());
            while (tileIterator.hasNext()) {
                Tile t = getGame().getMap().getTile(tileIterator.next());

                if (t == null) {
                    continue;
                }

                if (getOwner() == null) {
                    logger.warning("owner == null");
                    throw new NullPointerException();
                }

                if (t.getSettlement() != null && !t.getSettlement().getOwner().hasContacted(getOwner().getNation())) {
                    t.getSettlement().getOwner().setContacted(getOwner(), true);
                    getOwner().setContacted(t.getSettlement().getOwner(), true);
                } else if (t.isLand() && t.getFirstUnit() != null
                        && !t.getFirstUnit().getOwner().hasContacted(getOwner().getNation())) {
                    t.getFirstUnit().getOwner().setContacted(getOwner(), true);
                    getOwner().setContacted(t.getFirstUnit().getOwner(), true);
                }
            }
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

        if (getState() == Unit.IN_COLONY) {
            setState(Unit.ACTIVE);
        }

        setLocation(getTile());
    }

    /**
     * Checks whether this unit can be equipped with goods at the current
     * location. This is the case if the unit is in a colony in which the goods
     * are present, or if it is in Europe and the player can trade the goods and
     * has enough gold to pay for them.
     * 
     * @param equipType The type of goods.
     * @param amount The amount of goods.
     * @return whether this unit can be equipped with goods at the current
     *         location.
     */
    private boolean canBeEquipped(GoodsType equipType, int amount) {
        return ((getGoodsDumpLocation() != null && getGoodsDumpLocation().getGoodsCount(equipType) >= amount) || ((location instanceof Europe || location instanceof Unit
                && ((Unit) location).getLocation() instanceof Europe)
                && getOwner().getGold() >= getOwner().getMarket().getBidPrice(equipType, amount) && getOwner()
                .canTrade(equipType)));
    }

    /**
     * Checks if this unit can be armed in the current location.
     * 
     * @return <code>true</code> if it can be armed at the current location.
     */
    public boolean canBeArmed() {
        return isArmed() || canBeEquipped(Goods.MUSKETS, 50);
    }

    /**
     * Checks if this unit can be mounted in the current location.
     * 
     * @return <code>true</code> if it can mount a horse at the current
     *         location.
     */
    public boolean canBeMounted() {
        return isMounted() || canBeEquipped(Goods.HORSES, 50);
    }

    /**
     * Checks if this unit can be equiped with tools in the current location.
     * 
     * @return <code>true</code> if it can be equipped with tools at the
     *         current location.
     */
    public boolean canBeEquippedWithTools() {
        return isPioneer() || canBeEquipped(Goods.TOOLS, 20);
    }

    /**
     * Checks if this unit can be dressed as a missionary at the current
     * location.
     * 
     * @return <code>true</code> if it can be dressed as a missionary at the
     *         current location.
     */
    public boolean canBeDressedAsMissionary() {
        return isMissionary()
                || ((location instanceof Europe || location instanceof Unit
                        && ((Unit) location).getLocation() instanceof Europe) 
                        || (getColony() != null && getColony().hasAbility("model.ability.dressMissionary")));
    }

    /**
     * Sets the armed attribute of this unit.
     * 
     * @param b <i>true</i> if this unit should be armed and <i>false</i>
     *            otherwise.
     * @param isCombat Whether this is a result of combat. That is; do not pay
     *            for the muskets.
     * 
     */
    public void setArmed(boolean b, boolean isCombat) {
        if (isCombat) {
            armed = b; // No questions asked.
            return;
        }

        setMovesLeft(0);

        if (b) {
            if (isPioneer()) {
                setNumberOfTools(0);
            }

            if (isMissionary()) {
                setMissionary(false);
            }
        }

        if ((b) && (!armed)) {
            if (getGoodsDumpLocation() != null) {
                if (getGoodsDumpLocation().getGoodsCount(Goods.MUSKETS) < 50) {
                    return;
                }

                getGoodsDumpLocation().removeGoods(Goods.MUSKETS, 50);
                armed = true;
            } else if (isInEurope()) {
                getOwner().getMarket().buy(Goods.MUSKETS, 50, getOwner());
                armed = true;
            } else {
                logger.warning("Attempting to arm a soldier outside of a colony or Europe!");
            }
        } else if ((!b) && (armed)) {
            armed = false;

            if (getGoodsDumpLocation() != null) {
                getGoodsDumpLocation().addGoods(Goods.MUSKETS, 50);
            } else if (isInEurope()) {
                getOwner().getMarket().sell(Goods.MUSKETS, 50, getOwner());
            } else {
                throw new IllegalStateException();
            }
        }
    }

    /**
     * Sets the armed attribute of this unit.
     * 
     * @param b <i>true</i> if this unit should be armed and <i>false</i>
     *            otherwise.
     */
    public void setArmed(boolean b) {
        setArmed(b, false);
    }

    /**
     * Checks if this <code>Unit</code> is currently armed.
     * 
     * @return <i>true</i> if this unit is armed and <i>false</i> otherwise.
     */
    public boolean isArmed() {
        return armed;
    }

    /**
     * Sets the mounted attribute of this unit.
     * 
     * @param b <i>true</i> if this unit should be mounted and <i>false</i>
     *            otherwise.
     * @param isCombat Whether this is a result of combat.
     */
    public void setMounted(boolean b, boolean isCombat) {
        if (isCombat) {
            mounted = b; // No questions asked.
            return;
        }

        setMovesLeft(0);

        if (b) {
            if (isPioneer()) {
                setNumberOfTools(0);
            }

            if (isMissionary()) {
                setMissionary(false);
            }
        }

        if ((b) && (!mounted)) {
            if (getGoodsDumpLocation() != null) {
                if (getGoodsDumpLocation().getGoodsCount(Goods.HORSES) < 50) {
                    throw new IllegalStateException();
                }

                getGoodsDumpLocation().removeGoods(Goods.HORSES, 50);
                mounted = true;
            } else if (isInEurope()) {
                getOwner().getMarket().buy(Goods.HORSES, 50, getOwner());
                mounted = true;
            } else {
                logger.warning("Attempting to mount a colonist outside of a colony or Europe!");
            }
        } else if ((!b) && (mounted)) {
            mounted = false;

            if (getGoodsDumpLocation() != null) {
                getGoodsDumpLocation().addGoods(Goods.HORSES, 50);
            } else if (isInEurope()) {
                getOwner().getMarket().sell(Goods.HORSES, 50, getOwner());
            }
        }
    }

    /**
     * Sets the mounted attribute of this unit.
     * 
     * @param b <i>true</i> if this unit should be mounted and <i>false</i>
     *            otherwise.
     */
    public void setMounted(boolean b) {
        setMounted(b, false);
    }

    /**
     * Checks if this <code>Unit</code> is currently mounted.
     * 
     * @return <i>true</i> if this unit is mounted and <i>false</i> otherwise.
     */
    public boolean isMounted() {
        return mounted;
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
            return getLocation() instanceof Europe && getState() != TO_EUROPE && getState() != TO_AMERICA;
        }
    }

    /**
     * Sets the unit to be a missionary.
     * 
     * @param b <i>true</i> if the unit should be a missionary and <i>false</i>
     *            otherwise.
     */
    public void setMissionary(boolean b) {
        logger.finest(getID() + ": Entering method setMissionary with param " + b);
        setMovesLeft(0);

        if (b) {
            if (!isInEurope() && !getColony().hasAbility("model.ability.dressMissionary")) {
                throw new IllegalStateException(
                        "Can only dress as a missionary when the unit is located in Europe or a Colony with a church.");
            } else {
                if (isPioneer()) {
                    setNumberOfTools(0);
                }

                if (isArmed()) {
                    setArmed(false);
                }

                if (isMounted()) {
                    setMounted(false);
                }
            }
        }

        missionary = b;
    }

    /**
     * Checks if this <code>Unit</code> is a missionary.
     * 
     * @return 'true' if this colonist is dressed as a missionary, 'false'
     *         otherwise.
     */
    public boolean isMissionary() {
        return missionary || (hasAbility("model.ability.expertMissionary") && !isArmed() && !isMounted() && !isPioneer());
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
        if (!hasAbility("model.ability.carryGoods") || !isInEurope()) {
            throw new IllegalStateException("Cannot buy goods when not a carrier or in Europe.");
        }

        try {
            getOwner().getMarket().buy(goodsType, amount, getOwner());
            goodsContainer.addGoods(goodsType, amount);
        } catch (IllegalStateException ise) {
            this.addModelMessage(this, "notEnoughGold", null, ModelMessage.DEFAULT);
        }
    }

    /**
     * Sets how many tools this unit is carrying.
     * 
     * @param numberOfTools The number to set it to.
     */
    public void setNumberOfTools(int numberOfTools) {
        setMovesLeft(0);
        setState(ACTIVE);

        if (numberOfTools >= 20) {
            if (isMounted()) {
                setMounted(false);
            }

            if (isArmed()) {
                setArmed(false);
            }

            if (isMissionary()) {
                setMissionary(false);
            }
        }

        int changeAmount = 0;
        /*
         * if (numberOfTools > 100) { logger.warning("Attempting to give a
         * pioneer a number of greater than 100!"); }
         */
        if (numberOfTools > 100) {
            numberOfTools = 100;
        }

        if ((numberOfTools % 20) != 0) {
            // logger.warning("Attempting to give a pioneer a number of tools
            // that is not a multiple of 20!");
            numberOfTools -= (numberOfTools % 20);
        }

        changeAmount = numberOfTools - this.numberOfTools;
        if (changeAmount > 0) {
            if (getGoodsDumpLocation() != null) {
                int actualAmount = getGoodsDumpLocation().getGoodsCount(Goods.TOOLS);
                if (actualAmount < changeAmount)
                    changeAmount = actualAmount;
                if ((this.numberOfTools + changeAmount) % 20 > 0)
                    changeAmount -= (this.numberOfTools + changeAmount) % 20;
                if (changeAmount <= 0)
                    return;
                getGoodsDumpLocation().removeGoods(Goods.TOOLS, changeAmount);
                this.numberOfTools = this.numberOfTools + changeAmount;
            } else if (isInEurope()) {
                int maximumAmount = ((getOwner().getGold()) / (getOwner().getMarket().costToBuy(Goods.TOOLS)));
                if (maximumAmount < changeAmount)
                    changeAmount = maximumAmount;
                if ((this.numberOfTools + changeAmount) % 20 > 0)
                    changeAmount -= (this.numberOfTools + changeAmount) % 20;
                if (changeAmount <= 0)
                    return;
                getOwner().getMarket().buy(Goods.TOOLS, changeAmount, getOwner());
                this.numberOfTools = this.numberOfTools + changeAmount;
            } else {
                logger.warning("Attempting to create a pioneer outside of a colony or Europe!");
            }
        } else if (changeAmount < 0) {
            this.numberOfTools = this.numberOfTools + changeAmount;

            if (getGoodsDumpLocation() != null) {
                getGoodsDumpLocation().addGoods(Goods.TOOLS, -changeAmount);
            } else if (isInEurope()) {
                getOwner().getMarket().sell(Goods.TOOLS, -changeAmount, getOwner());
            }
        }
    }

    /**
     * Gets the number of tools this unit is carrying.
     * 
     * @return The number of tools.
     */
    public int getNumberOfTools() {
        return numberOfTools;
    }

    /**
     * Checks if this <code>Unit</code> is a pioneer.
     * 
     * @return <i>true</i> if it is a pioneer and <i>false</i> otherwise.
     */
    public boolean isPioneer() {

        return 0 < getNumberOfTools();
    }

    /**
     * Checks if this <code>Unit</code> is able to carry {@link Locatable}s.
     * 
     * @return 'true' if this unit can carry other units, 'false' otherwise.
     */
    public boolean isCarrier() {
        return isCarrier(getUnitType());
    }

    /**
     * Checks if this <code>Unit</code> is able to carry {@link Locatable}s.
     * 
     * @param type The type used when checking.
     * @return 'true' if the unit can carry other units, 'false' otherwise.
     */
    public static boolean isCarrier(UnitType unitType) {
        return unitType.hasAbility("model.ability.carryGoods") ||
            unitType.hasAbility("model.ability.carryUnits");
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

        this.owner = owner;

        oldOwner.invalidateCanSeeTiles();
        getOwner().setExplored(this);

        if (getGame().getFreeColGameObjectListener() != null) {
            getGame().getFreeColGameObjectListener().ownerChanged(this, oldOwner, owner);
        }
    }

    /**
     * Gets the nation the unit is serving. One of {DUTCH , ENGLISH, FRENCH,
     * SPANISH}.
     * 
     * @return The nation the unit is serving.
     */
    public int getNation() {
        return owner.getNation();
    }

    /**
     * Gets the type of the unit.
     * 
     * @return The type of the unit.
     */
    public int getType() {
        return type;
    }

    /**
     * Sets the type of the unit.
     * 
     * @param type The new type of the unit.
     */
    public void setType(int type) {
        setType(FreeCol.getSpecification().getUnitType(type));
    }

    /**
     * Sets the type of the unit.
     * 
     * @param type The new type of the unit.
     */
    public void setType(UnitType unitType) {
        // TODO: check for requirements (like for colonialRegular)
        this.unitType = unitType;
        type = unitType.getIndex();
        naval = unitType.hasAbility("model.ability.naval");
    }

    /**
     * Checks if this unit is of a given type.
     * 
     * @param t The type.
     * @return <code>true</code> if the unit was of the given type and
     *         <code>false</code> otherwise.
     */
    public boolean isType(int t) {
        return type == t;
    }

    /**
     * Returns the amount of moves this Unit has left.
     * 
     * @return The amount of moves this Unit has left. If the
     *         <code>unit.isUnderRepair()</code> then <code>0</code> is
     *         always returned.
     */
    public int getMovesLeft() {

        return !isUnderRepair() ? movesLeft : 0;
    }

    /**
     * Skips this unit by setting the moves left to 0.
     */
    public void skip() {
        movesLeft = 0;
    }

    /**
     * Returns the name of a unit in a human readable format. The return value
     * can be used when communicating with the user.
     * 
     * @return The given unit type as a String
     * @throws IllegalArgumentException
     */
    public String getName() {
        if (name != null) {
            return name;
        } else if (isArmed() && isMounted()) {
            switch (getType()) {
            case KINGS_REGULAR:
                return Messages.message("model.unit.kingsCavalry");
            case COLONIAL_REGULAR:
                return Messages.message("model.unit.colonialCavalry");
            case VETERAN_SOLDIER:
                return Messages.message("model.unit.veteranDragoon");
            case BRAVE:
                return Messages.message("model.unit.indianDragoon");
            default:
                return (Messages.message("model.unit.dragoon") + " (" + getUnitType().getName() + ")");
            }
        } else if (isArmed()) {
            switch (getType()) {
            case KINGS_REGULAR:
            case COLONIAL_REGULAR:
            case VETERAN_SOLDIER:
                return getUnitType().getName();
            case BRAVE:
                return Messages.message("model.unit.armedBrave");
            default:
                return (Messages.message("model.unit.soldier") + " (" + getUnitType().getName() + ")");
            }
        } else if (isMounted()) {
            if (hasAbility("model.ability.expertScout")) {
                return getUnitType().getName();
            } else if (getType() == BRAVE) {
                return Messages.message("model.unit.mountedBrave");
            } else {
                return (Messages.message("model.unit.scout") + " (" + getUnitType().getName() + ")");
            }
        } else if (isMissionary()) {
            if (hasAbility("model.ability.expertMissionary")) {
                return getUnitType().getName();
            } else {
                return (Messages.message("model.unit.missionary") + " (" + getUnitType().getName() + ")");
            }
        } else if (canCarryTreasure()) {
            return getUnitType().getName() + " (" + getTreasureAmount() + " " + Messages.message("gold") + ")";
        } else if (isPioneer()) {
            if (hasAbility("model.ability.expertPioneer")) {
                return getUnitType().getName();
            } else {
                return (Messages.message("model.unit.pioneer") + " (" + getUnitType().getName() + ")");
            }
        } else {
            return getUnitType().getName();
        }

    }

    /**
     * Set the <code>Name</code> value.
     * TODO: Should we still allow this?
     * @param newName The new Name value.
     */
    public void setName(String newName) {
        if (name != null && name.equals("")) {
            this.name = null;
        } else {
            this.name = newName;
        }
    }

    /**
     * Returns the name of a unit type in a human readable format. The return
     * value can be used when communicating with the user.
     * 
     * @param someType The type of <code>Unit</code>.
     * @return The given unit type as a String
     * @throws IllegalArgumentException
     *//*
    public static String getName(UnitType someType) {

        return Messages.message(someType.getName());
    }
*/
    public int getScoreValue() {
        return getScoreValue(type, location);
    }

    public static int getScoreValue(int someType, Location someLocation) {
        int value = 0;
        switch(someType) {
        case WAGON_TRAIN:
        case DAMAGED_ARTILLERY:
            return 1;
        case ARTILLERY:
            return 2;
        case CARAVEL:
            return 3;
        case MERCHANTMAN:
        case PRIVATEER:
            return 4;
        case GALLEON:
            return 5;
        case FRIGATE:
            return 6;
        case MAN_O_WAR:
            return 8;
        default:
            // TODO: restore
            //value = getSkillLevel(someType) + 3;
            value = 3;
            if (someLocation != null && someLocation instanceof WorkLocation) {
                value *= 2;
            }
        }
        return value;
    }


    /**
     * Gets the amount of moves this unit has at the beginning of each turn.
     * 
     * @return The amount of moves this unit has at the beginning of each turn.
     */
    public int getInitialMovesLeft() {
        if (isMounted()) {
            return 12;
        } else if (isMissionary()) {
            return 6;
        } else if (isNaval() && owner.hasFather(FreeCol.getSpecification().getFoundingFather("model.foundingFather.ferdinandMagellan"))) {
            return unitType.getMovement() + 3;
        } else {
            return unitType.getMovement();
        }
    }
    

    /**
     * Gets the initial hitpoints for a given type of <code>Unit</code>. For
     * now this method only returns <code>6</code> that is used to determine
     * the number of rounds needed to repair a unit, but later it can be used
     * for indicating the health of a unit as well.
     * 
     * <br>
     * <br>
     * 
     * Larger values would indicate a longer repair-time.
     * 
     * @param type The type of a <code>Unit</code>.
     * @return The initial hitpoints
     */
    public static int getInitialHitpoints(UnitType type) {
        return type.getHitPoints();
    }

    /**
     * Sets the hitpoints for this unit.
     * 
     * @param hitpoints The hitpoints this unit has. This is currently only used
     *            for damaged ships, but might get an extended use later.
     * @see #getInitialHitpoints
     */
    public void setHitpoints(int hitpoints) {
        this.hitpoints = hitpoints;
        if (hitpoints >= getInitialHitpoints(getUnitType())) {
            setState(ACTIVE);
        }
    }

    /**
     * Returns the hitpoints.
     * 
     * @return The hitpoints this unit has. This is currently only used for
     *         damaged ships, but might get an extended use later.
     * @see #getInitialHitpoints
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
        return (hitpoints < getInitialHitpoints(getUnitType()));
    }

    /**
     * Sends this <code>Unit</code> to the closest <code>Location</code> it
     * can get repaired.
     */
    public void sendToRepairLocation() {
        Location l = getOwner().getRepairLocation(this);
        setLocation(l);
        setState(ACTIVE);
        setMovesLeft(0);
    }

    /**
     * Gets the x-coordinate of this Unit (on the map).
     * 
     * @return The x-coordinate of this Unit (on the map).
     */
    public int getX() {

        return (getTile() != null) ? getTile().getX() : -1;
    }

    /**
     * Gets the y-coordinate of this Unit (on the map).
     * 
     * @return The y-coordinate of this Unit (on the map).
     */
    public int getY() {

        return (getTile() != null) ? getTile().getY() : -1;
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
        String occupationString;

        switch (getState()) {
        case Unit.ACTIVE:
            occupationString = getMovesLeft() > 0 ? "-" : "0";
            break;
        case Unit.FORTIFIED:
            occupationString = "F";
            break;
        case Unit.FORTIFYING:
            occupationString = "F";
            break;
        case Unit.SENTRY:
            occupationString = "S";
            break;
        case Unit.IN_COLONY:
            occupationString = "B";
            break;
        case Unit.IMPROVING:
            occupationString = workImprovement.getOccupationString();
            break;
        case Unit.TO_AMERICA:
        case Unit.TO_EUROPE:
            occupationString = "G";
            break;
        default:
            occupationString = "?";
            logger.warning("Unit has an invalid occpuation: " + getState());
        }
        if (getDestination() != null) {
            occupationString = "G";
        }

        return occupationString;
    }

    /**
     * Gets the state of this <code>Unit</code>.
     * 
     * @return The state of this <code>Unit</code>.
     */
    public int getState() {
        return state;
    }

    /**
     * Sets a new state for this unit and initializes the amount of work the
     * unit has left.
     * 
     * If the work needs turns to be completed (for instance when plowing), then
     * the moves the unit has still left will be used up. Some work (basically
     * building a road with a hardy pioneer) might actually be finished already
     * in this method-call, in which case the state is set back to ACTIVE.
     * 
     * @param s The new state for this Unit. Should be one of {ACTIVE,
     *            FORTIFIED, ...}.
     */
    public void setState(int s) {
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
            getTile().takeOwnership(getOwner());
            workLeft = getWorkImprovement().getTurnsToComplete();
            state = s;
            doAssignedWork();
            return;
        case TO_EUROPE:
            if (state == ACTIVE && !(location instanceof Europe)) {
                workLeft = 3;
            } else if ((state == TO_AMERICA) && (location instanceof Europe)) {
                // I think '4' was also used in the original game.
                workLeft = 4 - workLeft;
            }
            if (getOwner().hasFather(FreeCol.getSpecification().getFoundingFather("model.foundingFather.ferdinandMagellan"))) {
                workLeft--;
            }
            movesLeft = 0;
            break;
        case TO_AMERICA:
            if ((state == ACTIVE) && (location instanceof Europe)) {
                workLeft = 3;
            } else if ((state == TO_EUROPE) && (location instanceof Europe)) {
                // I think '4' was also used in the original game.
                workLeft = 4 - workLeft;
            }
            if (getOwner().hasFather(FreeCol.getSpecification().getFoundingFather("model.foundingFather.ferdinandMagellan"))) {
                workLeft--;
            }
            movesLeft = 0;
            break;
        default:
            workLeft = -1;
        }
        state = s; // PLOW and BUILD_ROAD returned already
    }

    /**
     * Checks if this <code>Unit</code> can be moved to Europe.
     * 
     * @return <code>true</code> if this unit is adjacent to a
     *         <code>Tile</code> of type {@link Tile#HIGH_SEAS}.
     */
    public boolean canMoveToEurope() {
        if (getLocation() instanceof Europe) {
            return true;
        }
        if (!getOwner().canMoveToEurope()) {
            return false;
        }

        Vector<Tile> surroundingTiles = getGame().getMap().getSurroundingTiles(getTile(), 1);
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
     * Moves this unit to europe.
     * 
     * @exception IllegalStateException If the move is illegal.
     */
    public void moveToEurope() {
        // Check if this move is illegal or not:
        if (!canMoveToEurope()) {
            throw new IllegalStateException(
                    "It is not allowed to move units to europe from the tile where this unit is located.");
        } else if (getLocation() instanceof Tile) {
            // Don't set entry location if location isn't Tile
            setEntryLocation(getLocation());
        }

        setState(TO_EUROPE);
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

        setState(TO_AMERICA);

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
     * Checks wether this unit can plow the <code>Tile</code> it is currently
     * located on or not.
     * 
     * @return The result.
     *//*
    public boolean canPlow() {
        return getTile().canBePlowed() && getNumberOfTools() >= 20;
    }
*/
    /**
     * Checks if a <code>Unit</code> can get the given state set.
     * 
     * @param s The new state for this Unit. Should be one of {ACTIVE,
     *            FORTIFIED, ...}.
     * @return 'true' if the Unit's state can be changed to the new value,
     *         'false' otherwise.
     */
    public boolean checkSetState(int s) {
        if (movesLeft <= 0 && (/*s == PLOW || s == BUILD_ROAD || */s == IMPROVING || s == FORTIFYING)) {
            return false;
        }
        switch (s) {
        case ACTIVE:
            return true;
        case IMPROVING:     // Check should be done before
            return true;
/*
        case PLOW:
            return canPlow();
        case BUILD_ROAD:
            if (getTile().hasRoad()) {
                return false;
            }
            return (getNumberOfTools() >= 20);
*/
        case IN_COLONY:
            return !isNaval();
        case FORTIFIED:
            return getState() == FORTIFYING;
        case FORTIFYING:
            return (getMovesLeft() > 0);
        case SENTRY:
            return true;
        case TO_EUROPE:
            if (!isNaval()) {
                return false;
            }
            return ((location instanceof Europe) && (getState() == TO_AMERICA))
                    || (getEntryLocation() == getLocation());
        case TO_AMERICA:
            return (location instanceof Europe && isNaval());
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
        return getOwner().canBuildColonies() && hasAbility("model.ability.foundColony")
                && getMovesLeft() > 0 && getTile() != null && getTile().isColonizeable();
    }

    /**
     * Makes this unit build the specified colony.
     * 
     * @param colony The colony this unit shall build.
     */
    public void buildColony(Colony colony) {
        if (!canBuildColony()) {
            throw new IllegalStateException();
        }
        if (!getTile().getPosition().equals(colony.getTile().getPosition())) {
            throw new IllegalStateException("A Unit can only build a colony if on the same tile as the colony");
        }

        getTile().setNationOwner(owner.getNation());
        getTile().setSettlement(colony);
        setLocation(colony);

        if (isArmed())
            setArmed(false);
        if (isMounted())
            setMounted(false);
        if (isPioneer())
            setNumberOfTools(0);
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
        int space = getInitialSpaceLeft() - getGoodsCount();

        Iterator<Unit> unitIterator = getUnitIterator();
        while (unitIterator.hasNext()) {
            Unit u = unitIterator.next();
            space -= u.getTakeSpace();
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
        return goodsContainer.getGoodsCount();
    }

    /**
     * Returns the amount of units/cargo that this unit can carry.
     * 
     * @return The amount of units/cargo that this unit can carry.
     */
    public int getInitialSpaceLeft() {
        return getInitialSpaceLeft(getUnitType());
    }

    /**
     * Returns the amount of units/cargo that this unit can carry.
     * 
     * @param type The type of unit.
     * @return The amount of units/cargo that this unit can carry.
     */
    public static int getInitialSpaceLeft(UnitType type) {
        return type.getSpace();
    }

    /**
     * Move the given unit to the front of this carrier (make sure it'll be the
     * first unit in this unit's unit list).
     * 
     * @param u The unit to move to the front.
     */
    public void moveToFront(Unit u) {
        if (hasAbility("model.ability.carryUnits") && unitContainer.removeUnit(u)) {
            unitContainer.addUnit(0, u);
        }
    }

    /**
     * Get the number of turns of work left.
     * 
     * @return Work left
     */
    public int getWorkLeft() {
        return workLeft;
    }

    /**
     * The status of units that are currently working (for instance on building
     * a road, or fortifying themselves) is updated in this method.
     */
    public void doAssignedWork() {
        logger.finest("Entering method doAssignedWork.");
        if (workLeft > 0) {
            if (state == IMPROVING) {
                // Has the improvement been completed already? Do nothing.
                if (getWorkImprovement().isComplete()) {
                    setState(ACTIVE);
                    return;
                }
                // Otherwise do work
                getWorkImprovement().doWork(getUnitType().hasAbility("model.ability.expertPioneer") ? 2 : 1);
                workLeft = getWorkImprovement().getTurnsToComplete();
            } else {
                workLeft--;
            }

            // Shorter travel time to America for the REF:
            if (state == TO_AMERICA && getOwner().isREF()) {
                workLeft = 0;
            }

            if (workLeft == 0) {
                workLeft = -1;

                int state = getState();

                switch (state) {
                case TO_EUROPE:
                    addModelMessage(getOwner().getEurope(),
                                    "model.unit.arriveInEurope",
                                    new String[][] {
                                        {"%europe%", getOwner().getEurope().getName()}},
                                    ModelMessage.DEFAULT, this);
                    Iterator<Unit> iter = getUnitIterator();
                    while (iter.hasNext()) {
                        Unit u = iter.next();
                        if (u.canCarryTreasure()) {
                            u.cashInTreasureTrain();
                        }
                    }
                    setState(ACTIVE);
                    break;
                case TO_AMERICA:
                    getGame().getModelController().setToVacantEntryLocation(this);
                    setState(ACTIVE);
                    break;
                case FORTIFYING:
                    setState(FORTIFIED);
                    break;
                case IMPROVING:
                    // Spend Goods - Quick fix, replace later
                    if (getWorkImprovement().getExpendedGoodsType() == Goods.TOOLS) {
                        expendTools(getWorkImprovement().getExpendedAmount());
                    }
                    // Deliver Goods if any
                    GoodsType deliverType = getWorkImprovement().getDeliverGoodsType();
                    if (deliverType != null) {
                        int deliverAmount = getTile().potential(deliverType) * getWorkImprovement().getDeliverAmount();
                        if (getUnitType().hasAbility("model.ability.expertPioneer")) {
                            deliverAmount *= 2;
                        }
                        if (getColony() != null && getColony().getOwner().equals(getOwner())) {
                            getColony().addGoods(deliverType, deliverAmount);
                        } else {
                            Vector<Tile> surroundingTiles = getTile().getMap().getSurroundingTiles(getTile(), 1);
                            Vector<Settlement> adjacentColonies = new Vector<Settlement>();
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
                    setState(ACTIVE);
                    setMovesLeft(0);
                    break;
                default:
                    logger.warning("Unknown work completed. State: " + state);
                    setState(ACTIVE);
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
    private void expendTools(int amount) {
        numberOfTools -= amount;
        if (numberOfTools == 0) {
            if (hasAbility("model.ability.expertPioneer")) {
                addModelMessage(this, "model.unit.noMoreToolsPioneer",
                                new String[][] {
                                    {"%unit%", getName()}},
                                ModelMessage.WARNING, this);
            } else {
                addModelMessage(this, "model.unit.noMoreTools",
                                new String[][] {
                                    { "%unit%", getName() } },
                                ModelMessage.WARNING, this);
            }
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
     * Returns the current defensive power of this unit. The tile on which this
     * unit is located will be taken into account.
     * 
     * @param attacker The attacker of this unit.
     * @return The current defensive power of this unit.
     */
    public float getDefensePower(Unit attacker) {
        return getDefensePower(attacker, this);
    }

    /**
     * Checks if this is an offensive unit.
     * 
     * @return <code>true</code> if this is an offensive unit meaning it can
     *         attack other units.
     */
    public boolean isOffensiveUnit() {
        return getUnitType().getOffence() > 0 || isArmed() || isMounted();
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
        return (getUnitType().getDefence() > 1 || isArmed() || isMounted()) && !isNaval();
    }

    /**
     * Returns the current offensive power of this unit.
     * 
     * @param target The target of the attack.
     * @return The current offensive power of this unit.
     */
    public float getOffensePower(Unit target) {
        return getOffensePower(this, target);
    }

    /**
     * Return the offensive power of the attacker versus the defender as an int.
     * 
     * @param attacker an <code>Unit</code> value
     * @param defender an <code>Unit</code> value
     * @return an <code>int</code> value
     */
    public static float getOffensePower(Unit attacker, Unit defender) {
        ArrayList<Modifier> modifiers = getOffensiveModifiers(attacker, defender);
        return modifiers.get(modifiers.size() - 1).getValue();
    }

    /**
     * Return a list of all offensive modifiers that apply to the attacker
     * versus the defender.
     * 
     * @param attacker an <code>Unit</code> value
     * @param defender an <code>Unit</code> value
     * @return an <code>ArrayList</code> of Modifiers
     */
    public static ArrayList<Modifier> getOffensiveModifiers(Unit attacker, Unit defender) {
        ArrayList<Modifier> result = new ArrayList<Modifier>();

        float addend, percentage;
        float totalAddend = attacker.getUnitType().getOffence();
        float totalPercentage = 100;

        result.add(new Modifier("modifiers.baseOffense", totalAddend, Modifier.ADDITIVE));

        if (attacker.isNaval()) {
            int goodsCount = attacker.getGoodsCount();
            if (goodsCount > 0) {
                // -12.5% penalty for every unit of cargo.
                // TODO: shouldn't this be -cargo/capacity?
                percentage = -12.5f * goodsCount;
                result.add(new Modifier("modifiers.cargoPenalty", percentage, Modifier.PERCENTAGE));
                totalPercentage += percentage;
            }
            if (attacker.hasAbility("model.ability.piracy") && 
                attacker.getOwner().hasFather(FreeCol.getSpecification().getFoundingFather("model.foundingFather.francisDrake"))) {
                // Drake grants 50% attack bonus
                percentage = 50;
                result.add(new Modifier("modifiers.drake", percentage, Modifier.PERCENTAGE));
                totalPercentage += percentage;
            }
        } else {

            if (attacker.isArmed()) {
                if (totalAddend == 0) {
                    // civilian
                    addend = 2;
                } else {
                    // brave or REF
                    addend = 1;
                }
                result.add(new Modifier("modifiers.armed", addend, Modifier.ADDITIVE));
                totalAddend += addend;
            }

            if (attacker.isMounted()) {
                addend = 1;
                result.add(new Modifier("modifiers.mounted", addend, Modifier.ADDITIVE));
                totalAddend += addend;
            }

            // 50% veteran bonus
            if (attacker.hasAbility("model.ability.expertSoldier")) {
                percentage = 50;
                result.add(new Modifier("modifiers.veteranBonus", percentage, Modifier.PERCENTAGE));
                totalPercentage += percentage;
            }

            // 50% attack bonus
            percentage = 50;
            result.add(new Modifier("modifiers.attackBonus", percentage, Modifier.PERCENTAGE));
            totalPercentage += percentage;

            // movement penalty
            int movesLeft = attacker.getMovesLeft();
            if (movesLeft == 1) {
                percentage = -66;
                result.add(new Modifier("modifiers.movementPenalty", percentage, Modifier.PERCENTAGE));
                totalPercentage += percentage;
            } else if (movesLeft == 2) {
                percentage = -33;
                result.add(new Modifier("modifiers.movementPenalty", percentage, Modifier.PERCENTAGE));
                totalPercentage += percentage;
            }

            // In the open
            if (defender != null && defender.getTile() != null && defender.getTile().getSettlement() == null) {

                /**
                 * Ambush bonus in the open = defender's defense bonus, if
                 * defender is REF, or attacker is indian.
                 */
                if (attacker.getOwner().isIndian() || defender.getOwner().isREF()) {
                    percentage = defender.getTile().defenseBonus();
                    result.add(new Modifier("modifiers.ambushBonus", percentage, Modifier.PERCENTAGE));
                    totalPercentage += percentage;
                }

                // 75% Artillery in the open penalty
                // TODO: is it right? or should it be another ability?
                if (attacker.hasAbility("model.ability.bombard")) {
                    percentage = -75;
                    result.add(new Modifier("modifiers.artilleryPenalty", percentage, Modifier.PERCENTAGE));
                    totalPercentage += percentage;
                }
            }

            // Attacking a settlement
            if (defender != null && defender.getTile() != null && defender.getTile().getSettlement() != null) {
                // REF bombardment bonus
                if (attacker.getOwner().isREF()) {
                    percentage = 50;
                    result.add(new Modifier("modifiers.REFbonus", percentage, Modifier.PERCENTAGE));
                    totalPercentage += percentage;
                }
            }
        }

        float offensivePower = (totalAddend * totalPercentage) / 100;
        result.add(new Modifier("modifiers.finalResult", offensivePower, Modifier.ADDITIVE));
        return result;
    }

    /**
     * Return the defensive power of the defender versus the attacker as an int.
     * 
     * @param attacker an <code>Unit</code> value
     * @param defender an <code>Unit</code> value
     * @return an <code>int</code> value
     */
    public static float getDefensePower(Unit attacker, Unit defender) {
        ArrayList<Modifier> modifiers = getDefensiveModifiers(attacker, defender);
        return modifiers.get(modifiers.size() - 1).getValue();
    }

    /**
     * Return a list of all defensive modifiers that apply to the defender
     * versus the attacker.
     * 
     * @param attacker an <code>Unit</code> value
     * @param defender an <code>Unit</code> value
     * @return an <code>ArrayList</code> of Modifiers
     */
    public static ArrayList<Modifier> getDefensiveModifiers(Unit attacker, Unit defender) {

        ArrayList<Modifier> result = new ArrayList<Modifier>();
        if (defender == null) {
            return result;
        }

        float addend, percentage;
        float totalAddend = defender.getUnitType().getDefence();
        float totalPercentage = 100;

        result.add(new Modifier("modifiers.baseDefense", totalAddend, Modifier.ADDITIVE));

        if (defender.isNaval()) {
            int goodsCount = defender.getVisibleGoodsCount();
            if (goodsCount > 0) {
                // -12.5% penalty for every unit of cargo.
                // TODO: shouldn't this be -cargo/capacity?
                percentage =  -12.5f * goodsCount;
                result.add(new Modifier("modifiers.cargoPenalty", percentage, Modifier.PERCENTAGE));
                totalPercentage += percentage;
            }
            if (defender.hasAbility("model.ability.piracy") && 
                defender.getOwner().hasFather(FreeCol.getSpecification().getFoundingFather("model.foundingFather.francisDrake"))) {
                // Drake grants 50% power bonus (in colonization gives for attack and defense)
                percentage = 50;
                result.add(new Modifier("modifiers.drake", percentage, Modifier.PERCENTAGE));
                totalPercentage += percentage;
            }
        } else {
            // Paul Revere makes an unarmed colonist in a settlement pick up
            // a stock-piled musket if attacked, so the bonus should be applied
            // for unarmed colonists inside colonies where there are muskets
            // available.
            if (defender.isArmed()) {
                addend = 1;
                result.add(new Modifier("modifiers.armed", addend, Modifier.ADDITIVE));
                totalAddend += addend;
            } else if (defender.getOwner().hasAbility("model.ability.automaticDefense") && defender.isColonist()
                    && defender.getLocation() instanceof WorkLocation) {
                Colony colony = ((WorkLocation) defender.getLocation()).getColony();
                if (colony.getGoodsCount(Goods.MUSKETS) >= 50) {
                    addend = 1;
                    result.add(new Modifier("modifiers.paulRevere", addend, Modifier.ADDITIVE));
                    totalAddend += addend;
                }
            }

            if (defender.isMounted()) {
                addend = 1;
                result.add(new Modifier("modifiers.mounted", addend, Modifier.ADDITIVE));
                totalAddend += addend;
            }

            // 50% veteran bonus
            if (defender.hasAbility("model.ability.expertSoldier")) {
                percentage = 50;
                result.add(new Modifier("modifiers.veteranBonus", percentage, Modifier.PERCENTAGE));
                totalPercentage += percentage;
            }

            // 50% fortify bonus
            if (defender.getState() == Unit.FORTIFIED) {
                percentage = 50;
                result.add(new Modifier("modifiers.fortified", percentage, Modifier.PERCENTAGE));
                totalPercentage += percentage;
            }

            if (defender.getTile() != null && defender.getTile().getSettlement() != null) {
                Modifier settlementModifier = getSettlementModifier(attacker, defender.getTile().getSettlement());
                result.add(settlementModifier);
                totalPercentage += settlementModifier.getValue();
                // TODO: is it right? or should it be another ability?
                if (defender.hasAbility("model.ability.bombard") && attacker.getOwner().isIndian()) {
                    // 100% defense bonus against an Indian raid
                    percentage = 100;
                    result.add(new Modifier("modifiers.artilleryAgainstRaid", percentage, Modifier.PERCENTAGE));
                    totalPercentage += percentage;
                }
            } else if (defender.getTile() != null) {
                // In the open
                if (!attacker.getOwner().isIndian() && !defender.getOwner().isREF()) {
                    // Terrain defensive bonus.
                    percentage = defender.getTile().defenseBonus();
                    result.add(new Modifier("modifiers.terrainBonus", percentage, Modifier.PERCENTAGE));
                    totalPercentage += percentage;
                }
                // TODO: is it right? or should it be another ability?
                if (defender.hasAbility("model.ability.bombard") && defender.getState() != Unit.FORTIFIED) {
                    // -75% Artillery in the Open penalty
                    percentage = -75;
                    result.add(new Modifier("modifiers.artilleryPenalty", percentage, Modifier.PERCENTAGE));
                    totalPercentage += percentage;
                }
            }

        }
        float defensivePower = (totalAddend * totalPercentage) / 100;
        result.add(new Modifier("modifiers.finalResult", defensivePower, Modifier.ADDITIVE));
        return result;
    }

    /**
     * Return the defensive modifier that applies to defenders in the given
     * settlement versus the attacker.
     * 
     * @param attacker an <code>Unit</code> value
     * @param settlement a <code>Settlement</code> value
     * @return a <code>Modifier</code>
     */
    public static Modifier getSettlementModifier(Unit attacker, Settlement settlement) {

        if (settlement instanceof Colony) {
            // Colony defensive bonus.
            Colony colony = (Colony) settlement;
            Building stockade = colony.getStockade();
            if (!stockade.isBuilt()) {
                // 50% colony bonus
                return new Modifier("modifiers.inColony", 50, Modifier.PERCENTAGE);
            } else {
                String modifier = stockade.getType().getID();
                modifier = "modifiers." + modifier.substring(modifier.lastIndexOf(".") + 1);
                return new Modifier(modifier, colony.getDefenseBonus(), Modifier.PERCENTAGE);
            }
        } else if (settlement instanceof IndianSettlement) {
            // Indian settlement defensive bonus.
            return new Modifier("modifiers.inSettlement", 50, Modifier.PERCENTAGE);
        } else {
            return new Modifier(null, 0, Modifier.PERCENTAGE);
        }
    }

    /**
     * Attack a unit with the given outcome.
     * 
     * @param defender The <code>Unit</code> defending against attack.
     * @param result The result of the attack.
     * @param plunderGold The amount of gold to plunder in case of a successful
     *            attack on a <code>Settlement</code>.
     */
    public void attack(Unit defender, int result, int plunderGold) {
        if (defender == null) {
            throw new NullPointerException("No defender specified!");
        }
        Player enemy = defender.getOwner();
        if (getOwner().getStance(enemy) == Player.ALLIANCE) {
            throw new IllegalStateException("Cannot attack allied players.");
        }

        // make sure we are at war, unless one of both units is a privateer
        //getOwner().isEuropean() && enemy.isEuropean() &&
        if (!hasAbility("model.ability.piracy") && !defender.hasAbility("model.ability.piracy")) {
            getOwner().setStance(enemy, Player.WAR);
        } else if (hasAbility("model.ability.piracy")) {
            enemy.setAttackedByPrivateers();
        }

        // Wake up if you're attacking something.
        // Before, a unit could stay fortified during execution of an
        // attack. - sjm
        state = ACTIVE;
        if (hasAbility("model.ability.multipleAttacks")) {
            movesLeft = 0;
        }

        Tile newTile = defender.getTile();
        adjustTension(defender);
        Settlement settlement = newTile.getSettlement();

        switch (result) {
        case ATTACK_EVADES:
            if (isNaval()) {
                // send message to both parties
                addModelMessage(this, "model.unit.shipEvaded",
                                new String[][] {
                                    { "%unit%", defender.getName() },
                                    { "%nation%", enemy.getNationAsString() } },
                                ModelMessage.DEFAULT, this);
                addModelMessage(defender, "model.unit.shipEvaded",
                                new String[][] {
                                    { "%unit%", defender.getName() },
                                    { "%nation%", enemy.getNationAsString() }
                                }, ModelMessage.DEFAULT, this);
            } else {
                logger.warning("Non-naval unit evades!");
            }
            break;
        case ATTACK_LOSS:
            if (isNaval()) {
                shipDamaged();
                addModelMessage(defender, "model.unit.enemyShipDamaged",
                                new String[][] {
                                    { "%unit%", getName() },
                                    { "%nation%", getOwner().getNationAsString() } }, 
                                ModelMessage.UNIT_DEMOTED);
            } else {
                demote(defender, false);
                if (enemy.hasAbility("model.ability.automaticPromotion")) {
                    defender.promote();
                }
            }
            break;
        case ATTACK_GREAT_LOSS:
            if (isNaval()) {
                shipSunk();
                addModelMessage(defender, "model.unit.shipSunk",
                                new String[][] {
                                    { "%unit%", getName() },
                                    { "%nation%", getOwner().getNationAsString() } }, 
                                ModelMessage.UNIT_DEMOTED);
            } else {
                demote(defender, false);
                defender.promote();
            }
            break;
        case ATTACK_DONE_SETTLEMENT:
            if (settlement instanceof IndianSettlement) {
                defender.dispose();
                destroySettlement((IndianSettlement) settlement);
            } else if (settlement instanceof Colony) {
                captureColony((Colony) settlement, plunderGold);
            } else {
                throw new IllegalStateException("Unknown type of settlement.");
            }
            break;
        case ATTACK_WIN:
            if (isNaval()) {
                captureGoods(defender);
                defender.shipDamaged();
                addModelMessage(this, "model.unit.enemyShipDamaged",
                                new String[][] {
                                    { "%unit%", defender.getName() },
                                    { "%nation%", enemy.getNationAsString() } }, 
                                ModelMessage.UNIT_DEMOTED);
            } else if (hasAbility("model.ability.pillageUnprotectedColony") && !defender.isDefensiveUnit() &&
                    defender.getColony() != null && !defender.getColony().hasStockade()) {
                pillageColony(defender.getColony());
            } else {
                if (getOwner().hasAbility("model.ability.automaticPromotion")) {
                    promote();
                }
                if (!defender.isNaval()) {
                    defender.demote(this, false);
                    if (settlement instanceof IndianSettlement) {
                        getConvert((IndianSettlement) settlement);
                    }
                }
            }
            break;
        case ATTACK_GREAT_WIN:
            if (isNaval()) {
                captureGoods(defender);
                defender.shipSunk();
                addModelMessage(this, "model.unit.shipSunk",
                                new String[][] {
                                    { "%unit%", defender.getName() },
                                    { "%nation%", enemy.getNationAsString() } },
                                ModelMessage.UNIT_DEMOTED);
            } else {
                promote();
                if (!defender.isNaval()) {
                    defender.demote(this, true);
                    if (settlement instanceof IndianSettlement) {
                        getConvert((IndianSettlement) settlement);
                    }
                }
            }
            break;
        default:
            logger.warning("Illegal result of attack!");
            throw new IllegalArgumentException("Illegal result of attack!");
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
     * Sets the damage to this ship and sends it to its repair location.
     */
    public void shipDamaged() {
        shipDamaged(null);
    }

    /**
     * Sets the damage to this ship and sends it to its repair location.
     * 
     * @param colony The colony that opened fire on this unit.
     */
    public void shipDamaged(Colony colony) {
        String nation = owner.getNationAsString();
        Location repairLocation = getOwner().getRepairLocation(this);
        if (repairLocation == null) {
            // This fixes a problem with enemy ships without a known repair
            // location.
            dispose();
            return;
        }
        String repairLocationName = repairLocation.getLocationName();
        /*
         * if (repairLocation instanceof Colony) { repairLocationName =
         * ((Colony) repairLocation).getName(); }
         */
        if (colony != null) {
            addModelMessage(this, "model.unit.shipDamagedByBombardment", 
                            new String[][] {
                                { "%colony%", colony.getName() },
                                { "%unit%", getName() },
                                { "%repairLocation%", repairLocationName },
                                { "%nation%", nation } },
                            ModelMessage.UNIT_DEMOTED);
        } else {
            addModelMessage(this, "model.unit.shipDamaged",
                            new String[][] {
                                { "%unit%", getName() },
                                { "%repairLocation%", repairLocationName },
                                { "%nation%", nation } },
                            ModelMessage.UNIT_DEMOTED);
        }
        setHitpoints(1);
        getUnitContainer().disposeAllUnits();
        goodsContainer.removeAll();
        sendToRepairLocation();
    }

    /**
     * Sinks this ship.
     */
    public void shipSunk() {
        shipSunk(null);
    }

    /**
     * Sinks this ship.
     * 
     * @param colony The colony that opened fire on this unit.
     */
    public void shipSunk(Colony colony) {
        String nation = owner.getNationAsString();
        if (colony != null) {
            addModelMessage(this, "model.unit.shipSunkByBombardment", new String[][] {
                    { "%colony%", colony.getName() }, { "%unit%", getName() },
                    { "%nation%", nation } }, ModelMessage.UNIT_LOST);
        } else {
            addModelMessage(this, "model.unit.shipSunk", new String[][] { { "%unit%", getName() },
                    { "%nation%", nation } }, ModelMessage.UNIT_LOST);
        }
        dispose();
    }

    /**
     * Demotes this unit. A unit that can not be further demoted is destroyed.
     * The enemy may plunder horses and muskets.
     * 
     * @param enemyUnit The unit we are fighting against.
     * @param canStole <code>true</code> indicates that muskets/horses
     *            should be taken by the <code>enemyUnit</code>.
     */
    public void demote(Unit enemyUnit, boolean canStole) {
        String oldName = getName();
        String messageID = "model.unit.unitDemoted";
        String nation = owner.getNationAsString();
        int messageType = ModelMessage.UNIT_DEMOTED;

        if (enemyUnit.isUndead()) {
            // this unit is captured, don't show old owner's messages to new
            // owner
            for (ModelMessage message : getOwner().getModelMessages()) {
                message.setBeenDisplayed(true);
            }
            messageID = "model.unit.unitCaptured";
            messageType = ModelMessage.UNIT_LOST;
            setHitpoints(getInitialHitpoints(enemyUnit.getUnitType()));
            setLocation(enemyUnit.getTile());
            setOwner(enemyUnit.getOwner());
            setType(UNDEAD);
        } else if (getType() == ARTILLERY) {
            messageID = "model.unit.artilleryDamaged";
            setType(DAMAGED_ARTILLERY);
        } else if (getType() == DAMAGED_ARTILLERY) {
            messageID = "model.unit.unitDestroyed";
            messageType = ModelMessage.UNIT_LOST;
            dispose();
        } else if (getType() == BRAVE || getType() == KINGS_REGULAR) {
            messageID = "model.unit.unitSlaughtered";
            messageType = ModelMessage.UNIT_LOST;
            dispose();
        } else if (isMounted()) {
            if (enemyUnit.getType() == BRAVE && !enemyUnit.isMounted() && canStole) {
                addModelMessage(this, "model.unit.braveMounted", new String[][] { { "%nation%",
                        enemyUnit.getOwner().getNationAsString() } }, ModelMessage.FOREIGN_DIPLOMACY);
                enemyUnit.setMounted(true, true);
            }
            if (isArmed()) { // dragoon
                setMounted(false, true);
            } else { // scout
                messageID = "model.unit.unitSlaughtered";
                messageType = ModelMessage.UNIT_LOST;
                dispose();
            }
        } else if (isArmed()) {
            // soldier
            setArmed(false, true);
            if (enemyUnit.getType() == BRAVE && !enemyUnit.isArmed() && canStole) {
                addModelMessage(this, "model.unit.braveArmed", new String[][] { { "%nation%",
                        enemyUnit.getOwner().getNationAsString() } }, ModelMessage.FOREIGN_DIPLOMACY);
                enemyUnit.setArmed(true, true);
            }
        } else {
            // civilians, wagon trains and treasure trains
            if (enemyUnit.getOwner().isEuropean()) {
                // this unit is captured, don't show old owner's messages to new
                // owner
                for (ModelMessage message : getOwner().getModelMessages()) {
                    message.setBeenDisplayed(true);
                }
                if (getType() == VETERAN_SOLDIER) {
                    clearSpeciality();
                    messageID = "model.unit.veteranUnitCaptured";
                } else {
                    messageID = "model.unit.unitCaptured";
                }
                messageType = ModelMessage.UNIT_LOST;
                setHitpoints(getInitialHitpoints(enemyUnit.getUnitType()));
                setLocation(enemyUnit.getTile());
                setOwner(enemyUnit.getOwner());
            } else {
                messageID = "model.unit.unitSlaughtered";
                messageType = ModelMessage.UNIT_LOST;
                dispose();
            }
        }
        if (!isDisposed() && getMovesLeft() > getInitialMovesLeft()) {
            setMovesLeft(getInitialMovesLeft());
        }
        String newName = getName();
        FreeColGameObject source = this;
        if (getColony() != null) {
            source = getColony();
        }
        addModelMessage(source, messageID, new String[][] {
            { "%oldName%", oldName },
            { "%unit%", newName },
            { "%nation%", nation },
            { "%enemyUnit%", enemyUnit.getName() },
            { "%enemyNation%", enemyUnit.getOwner().getNationAsString() }
        }, messageType, this);

        if (getOwner() != enemyUnit.getOwner()) {
            // this unit hasn't been captured by enemyUnit, show message to
            // enemyUnit's owner
            source = enemyUnit;
            if (enemyUnit.getColony() != null) {
                source = enemyUnit.getColony();
            }
            addModelMessage(source, messageID, new String[][] {
                { "%oldName%", oldName },
                { "%unit%", newName },
                { "%enemyUnit%", enemyUnit.getName() },
                { "%nation%", nation },
                { "%enemyNation%", enemyUnit.getOwner().getNationAsString() }
            }, messageType, this);
        }
    }

    /**
     * Train the current unit in the job of its teacher.
     * 
     */
    public void train() {
        String oldName = getName();
        UnitType learning = getUnitTypeTeaching(getTeacher().getUnitType(), unitType);

        if (learning != null) {
            setType(learning);
        }

        String newName = getName();
        if (!newName.equals(oldName)) {
            Colony colony = getTile().getColony();
            addModelMessage(colony, "model.unit.unitEducated",
                            new String[][] {
                                { "%oldName%", oldName },
                                { "%unit%", newName }, 
                                { "%colony%", colony.getName() } },
                            ModelMessage.UNIT_IMPROVED, this);
        }
    }

    /**
     * Promotes this unit.
     */
    public void promote() {
        String oldName = getName();
        String nation = owner.getNationAsString();
        UnitType newType = getUnitType().getPromotion();
        
        if (newType != null) {
            setType(newType);
        }

        String newName = getName();
        if (!newName.equals(oldName)) {
            addModelMessage(this, "model.unit.unitPromoted",
                            new String[][] {
                                { "%oldName%", oldName },
                                { "%unit%", getName() },
                                { "%nation%", nation } },
                            ModelMessage.UNIT_IMPROVED);
        }
    }

    /**
     * Adjusts the tension and alarm levels of the enemy unit's owner according
     * to the type of attack.
     * 
     * @param enemyUnit The unit we are attacking.
     */
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
     * Returns true if this unit is a ship that can capture enemy goods. That
     * is, a privateer, frigate or man-o-war.
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
     * Destroys an Indian settlement.
     * 
     * @param settlement The Indian settlement to destroy.
     */
    public void destroySettlement(IndianSettlement settlement) {
        Player enemy = settlement.getOwner();
        boolean wasCapital = settlement.isCapital();
        Tile newTile = settlement.getTile();
        ModelController modelController = getGame().getModelController();
        settlement.dispose();

        enemy.modifyTension(getOwner(), Tension.TENSION_ADD_MAJOR);

        List<UnitType> treasureUnitTypes = FreeCol.getSpecification().getUnitTypesWithAbility("model.ability.carryTreasure");
        if (treasureUnitTypes.size() > 0) {
            int randomTreasure = modelController.getRandom(getID() + "indianTreasureRandom" + getID(), 11);
            int random = modelController.getRandom(getID() + "newUnitForTreasure" + getID(), treasureUnitTypes.size());
            Unit tTrain = modelController.createUnit(getID() + "indianTreasure" + getID(), newTile, getOwner(),
                    treasureUnitTypes.get(random));

            // Larger treasure if Hernan Cortes is present in the congress:
            Modifier modifier = getOwner().getModifier("model.modifier.nativeTreasureModifier");
            if (modifier != null) {
                randomTreasure = (int) modifier.applyTo(randomTreasure);
            }

            // Incan and Aztecs give more gold
            // TODO: make this part of the NationType
            if (enemy.getNation() == Player.INCA || enemy.getNation() == Player.AZTEC) {
                tTrain.setTreasureAmount(randomTreasure * 500 + 10000);
            } else {
                tTrain.setTreasureAmount(randomTreasure * 50  + 300);
            }

            // capitals give more gold
            if (wasCapital) {
                tTrain.setTreasureAmount((tTrain.getTreasureAmount() * 3) / 2);
            }

            addModelMessage(this, "model.unit.indianTreasure", new String[][] { { "%indian%", enemy.getNationAsString() },
                    { "%amount%", Integer.toString(tTrain.getTreasureAmount()) } }, ModelMessage.DEFAULT);
        }
        setLocation(newTile);
    }

    /**
     * Captures an enemy colony and plunders gold.
     * 
     * @param colony The enemy colony to capture.
     * @param plunderGold The amount of gold to plunder.
     */
    public void captureColony(Colony colony, int plunderGold) {
        Player enemy = colony.getOwner();
        Player myPlayer = getOwner();
        enemy.modifyTension(getOwner(), Tension.TENSION_ADD_MAJOR);

        if (myPlayer.isEuropean()) {
            addModelMessage(enemy, "model.unit.colonyCapturedBy", new String[][] { { "%colony%", colony.getName() },
                    { "%amount%", Integer.toString(plunderGold) }, { "%player%", myPlayer.getNationAsString() } },
                    ModelMessage.DEFAULT);
            colony.damageAllShips();

            myPlayer.modifyGold(plunderGold);
            enemy.modifyGold(-plunderGold);

            colony.setOwner(myPlayer); // This also changes over all of the
            // units...
            addModelMessage(colony, "model.unit.colonyCaptured", new String[][] { { "%colony%", colony.getName() },
                    { "%amount%", Integer.toString(plunderGold) } }, ModelMessage.DEFAULT);

            // Demote all soldiers and clear all orders:
            Iterator<Unit> it = colony.getTile().getUnitIterator();
            while (it.hasNext()) {
                Unit u = it.next();
                if (u.getType() == Unit.VETERAN_SOLDIER || u.getType() == Unit.KINGS_REGULAR
                        || u.getType() == Unit.COLONIAL_REGULAR) {
                    u.clearSpeciality();
                }
                u.setState(Unit.ACTIVE);
                if (isUndead()) {
                    u.setType(UNDEAD);
                }
            }

            if (isUndead()) {
                Iterator<Unit> it2 = colony.getUnitIterator();
                while (it2.hasNext()) {
                    Unit u = it2.next();
                    u.setType(UNDEAD);
                }
            }

            setLocation(colony.getTile());
        } else { // Indian:
            if (colony.getUnitCount() <= 1) {
                myPlayer.modifyGold(plunderGold);
                enemy.modifyGold(-plunderGold);
                addModelMessage(enemy, "model.unit.colonyBurning",
                        new String[][] { { "%colony%", colony.getName() },
                        { "%amount%", Integer.toString(plunderGold) },
                        { "%nation%", myPlayer.getNationAsString() },
                        { "%unit%", getName() } }, ModelMessage.DEFAULT);
                colony.damageAllShips();
                colony.dispose();
            } else {
                Unit victim = colony.getRandomUnit();
                if (victim == null) {
                    return;
                }
                addModelMessage(colony, "model.unit.colonistSlaughtered",
                                new String[][] {
                                    { "%colony%", colony.getName() },
                                    { "%unit%", victim.getName() },
                                    { "%nation%", myPlayer.getNationAsString() },
                                    { "%enemyUnit%", getName() } }, ModelMessage.UNIT_LOST);
                victim.dispose();
            }
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
     * Clear the speciality of a <code>Unit</code> changing the UnitType
     * to the UnitType specified for clearing
     */
    public void clearSpeciality() {
        UnitType newType = unitType.getClearSpeciality();
        if (newType != null) {
            setType(newType);
        }
    }

    /**
     * Gets the Colony the goods of this unit would go to if it were to
     * de-equip.
     * 
     * @return The Colony the goods would go to, or null if there is no
     *         appropriate Colony
     */
    public Colony getGoodsDumpLocation() {
        if ((location instanceof Colony)) {
            return ((Colony) location);
        } else if (location instanceof Building) {
            return (((Building) location).getColony());
        } else if (location instanceof ColonyTile) {
            return (((ColonyTile) location).getColony());
        } else if ((location instanceof Tile) && (((Tile) location).getSettlement() != null)
                && (((Tile) location).getSettlement() instanceof Colony)) {
            return (((Colony) (((Tile) location).getSettlement())));
        } else if (location instanceof Unit) {
            if ((((Unit) location).getLocation()) instanceof Colony) {
                return ((Colony) (((Unit) location).getLocation()));
            } else if (((((Unit) location).getLocation()) instanceof Tile)
                    && (((Tile) (((Unit) location).getLocation())).getSettlement() != null)
                    && (((Tile) (((Unit) location).getLocation())).getSettlement() instanceof Colony)) {
                return ((Colony) (((Tile) (((Unit) location).getLocation())).getSettlement()));
            }
        }
        return null;
    }

    /**
     * Given a type of goods to produce in the field and a tile, returns the
     * unit's potential to produce goods.
     * 
     * @param goodsType The type of goods to be produced.
     * @param tile The tile which is being worked.
     * @return The potential amount of goods to be farmed.
     */
    public int getProductionOf(GoodsType goodsType, int base) {
        if (base == 0) {
            return 0;
        }
        
        base = getUnitType().getProductionFor(goodsType, base);

        if (goodsType == Goods.FURS && 
            getOwner().hasFather(FreeCol.getSpecification().getFoundingFather("model.foundingFather.henryHudson"))) {
            base *= 2;
        }

        return base;
    }

    public static int getNextHammers(int index) {
        return getNextHammers(FreeCol.getSpecification().getUnitType(index));
    }

    public static int getNextHammers(UnitType type) {

        if (type.canBeBuilt()) {

            return type.getHammersRequired();
        }

        return -1;
    }

    public static int getNextTools(int index) {
        return getNextTools(FreeCol.getSpecification().getUnitType(index));
    }

    public static int getNextTools(UnitType type) {

        if (type.canBeBuilt()) {

            return type.getToolsRequired();
        }

        return -1;
    }

    /**
     * Removes all references to this object.
     */
    public void dispose() {
        if (hasAbility("model.ability.carryUnits")) {
            unitContainer.dispose();
        }

        if (hasAbility("model.ability.carryGoods")) {
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
        if (learnType != null && unitType.canLearnFromExperience(learnType)) {
            logger.finest("About to call getRandom for experience");
            int random = getGame().getModelController().getRandom(getID() + "experience", 5000);
            if (random < Math.min(experience, 200)) {
                logger.finest("About to change type of unit due to experience.");
                String oldName = getName();
                setType(learnType);
                addModelMessage(getColony(), "model.unit.experience",
                                new String[][] {
                                    { "%oldName%", oldName },
                                    { "%unit%", getName() },
                                    { "%colony%", getColony().getName() } },
                                ModelMessage.UNIT_IMPROVED, this);
            }
        }
    }

    /**
     * Prepares the <code>Unit</code> for a new turn.
     */
    public void newTurn() {
        if (isUninitialized()) {
            logger.warning("Calling newTurn for an uninitialized object: " + getID());
        }
        checkExperiencePromotion();
        movesLeft = getInitialMovesLeft();
        doAssignedWork();
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

        out.writeAttribute("ID", getID());
        if (name != null) {
            out.writeAttribute("name", name);
        }
        out.writeAttribute("type", Integer.toString(type));
        out.writeAttribute("armed", Boolean.toString(armed));
        out.writeAttribute("mounted", Boolean.toString(mounted));
        out.writeAttribute("missionary", Boolean.toString(missionary));
        out.writeAttribute("movesLeft", Integer.toString(movesLeft));
        out.writeAttribute("state", Integer.toString(state));
        out.writeAttribute("workLeft", Integer.toString(workLeft));
        out.writeAttribute("numberOfTools", Integer.toString(numberOfTools));
        String ownerID = null;
        if (getOwner().equals(player) || !hasAbility("model.ability.piracy") || showAll) {
            ownerID = owner.getID();
        } else {
            ownerID = "unknown";
        }
        out.writeAttribute("owner", ownerID);
        out.writeAttribute("turnsOfTraining", Integer.toString(turnsOfTraining));
        out.writeAttribute("workType", Integer.toString(workType.getIndex()));
        out.writeAttribute("experience", Integer.toString(experience));
        out.writeAttribute("treasureAmount", Integer.toString(treasureAmount));
        out.writeAttribute("hitpoints", Integer.toString(hitpoints));

        if (student != null) {
            out.writeAttribute("student", student.getID());
        }

        if (teacher != null) {
            out.writeAttribute("teacher", teacher.getID());
        }

        if (indianSettlement != null) {
            if (getGame().isClientTrusted() || showAll || player == getOwner()) {
                out.writeAttribute("indianSettlement", indianSettlement.getID());
            }
        }

        if (entryLocation != null) {
            out.writeAttribute("entryLocation", entryLocation.getID());
        }

        if (location != null) {
            if (getGame().isClientTrusted() || showAll || player == getOwner()
                    || !(location instanceof Building || location instanceof ColonyTile)) {
                out.writeAttribute("location", location.getID());
            } else {
                out.writeAttribute("location", getColony().getID());
            }
        }

        if (destination != null) {
            out.writeAttribute("destination", destination.getID());
        }
        if (tradeRoute != null) {
            out.writeAttribute("tradeRoute", tradeRoute.getID());
            out.writeAttribute("currentStop", String.valueOf(currentStop));
        }

        // Do not show enemy units hidden in a carrier:
        if (getGame().isClientTrusted() || showAll || getOwner().equals(player)
                || !getGameOptions().getBoolean(GameOptions.UNIT_HIDING) && player.canSee(getTile())) {
            if (hasAbility("model.ability.carryUnits")) {
                unitContainer.toXML(out, player, showAll, toSavedGame);
            }
            if (hasAbility("model.ability.carryGoods")) {
                goodsContainer.toXML(out, player, showAll, toSavedGame);
            }
        } else {
            if (hasAbility("model.ability.carryGoods")) {
                out.writeAttribute("visibleGoodsCount", Integer.toString(getGoodsCount()));
                GoodsContainer emptyGoodsContainer = new GoodsContainer(getGame(), this);
                emptyGoodsContainer.setFakeID(goodsContainer.getID());
                emptyGoodsContainer.toXML(out, player, showAll, toSavedGame);
            }
            if (hasAbility("model.ability.carryUnits")) {
                UnitContainer emptyUnitContainer = new UnitContainer(getGame(), this);
                emptyUnitContainer.setFakeID(unitContainer.getID());
                emptyUnitContainer.toXML(out, player, showAll, toSavedGame);
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
        setID(in.getAttributeValue(null, "ID"));
        setName(in.getAttributeValue(null, "name"));
        type = Integer.parseInt(in.getAttributeValue(null, "type"));
        unitType = FreeCol.getSpecification().getUnitType(type);
        naval = unitType.hasAbility("model.ability.navalUnit");
        armed = Boolean.valueOf(in.getAttributeValue(null, "armed")).booleanValue();
        mounted = Boolean.valueOf(in.getAttributeValue(null, "mounted")).booleanValue();
        missionary = Boolean.valueOf(in.getAttributeValue(null, "missionary")).booleanValue();
        movesLeft = Integer.parseInt(in.getAttributeValue(null, "movesLeft"));
        state = Integer.parseInt(in.getAttributeValue(null, "state"));
        workLeft = Integer.parseInt(in.getAttributeValue(null, "workLeft"));
        numberOfTools = Integer.parseInt(in.getAttributeValue(null, "numberOfTools"));

        String ownerID = in.getAttributeValue(null, "owner");
        if (ownerID.equals("unknown")) {
            owner = Game.unknownEnemy;
        } else {
            owner = (Player) getGame().getFreeColGameObject(ownerID);
            if (owner == null) {
                owner = new Player(getGame(), in.getAttributeValue(null, "owner"));
            }
        }

        turnsOfTraining = Integer.parseInt(in.getAttributeValue(null, "turnsOfTraining"));
        hitpoints = Integer.parseInt(in.getAttributeValue(null, "hitpoints"));

        final String teacherString = in.getAttributeValue(null, "teacher");
        if (teacherString != null) {
            teacher = (Unit) getGame().getFreeColGameObject(teacherString);
            if (teacher == null) {
                teacher = new Unit(getGame(), teacherString);
            }
        }

        final String studentString = in.getAttributeValue(null, "student");
        if (studentString != null) {
            student = (Unit) getGame().getFreeColGameObject(studentString);
            if (student == null) {
                student = new Unit(getGame(), studentString);
            }
        }

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
                if (destinationStr.startsWith(Tile.getXMLElementTagName())) {
                    destination = new Tile(getGame(), destinationStr);
                } else if (destinationStr.startsWith(Colony.getXMLElementTagName())) {
                    destination = new Colony(getGame(), destinationStr);
                } else if (destinationStr.startsWith(IndianSettlement.getXMLElementTagName())) {
                    destination = new IndianSettlement(getGame(), destinationStr);
                } else if (destinationStr.startsWith(Europe.getXMLElementTagName())) {
                    destination = new Europe(getGame(), destinationStr);
                } else if (destinationStr.startsWith(ColonyTile.getXMLElementTagName())) {
                    destination = new ColonyTile(getGame(), destinationStr);
                } else if (destinationStr.startsWith(Building.getXMLElementTagName())) {
                    destination = new Building(getGame(), destinationStr);
                } else if (destinationStr.startsWith(Unit.getXMLElementTagName())) {
                    destination = new Unit(getGame(), destinationStr);
                } else {
                    logger.warning("Unknown type of Location.");
                    destination = new Tile(getGame(), destinationStr);
                }
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

        final String workTypeStr = in.getAttributeValue(null, "workType");
        if (workTypeStr != null) {
            workType = FreeCol.getSpecification().getGoodsType(Integer.parseInt(workTypeStr));
        }

        final String experienceStr = in.getAttributeValue(null, "experience");
        if (experienceStr != null) {
            experience = Integer.parseInt(experienceStr);
        }

        visibleGoodsCount = getAttribute(in, "visibleGoodsCount", -1);

        final String entryLocationStr = in.getAttributeValue(null, "entryLocation");
        if (entryLocationStr != null) {
            entryLocation = (Location) getGame().getFreeColGameObject(entryLocationStr);
            if (entryLocation == null) {
                if (entryLocationStr.startsWith(Tile.getXMLElementTagName())) {
                    entryLocation = new Tile(getGame(), entryLocationStr);
                } else if (entryLocationStr.startsWith(Colony.getXMLElementTagName())) {
                    entryLocation = new Colony(getGame(), entryLocationStr);
                } else if (entryLocationStr.startsWith(IndianSettlement.getXMLElementTagName())) {
                    entryLocation = new IndianSettlement(getGame(), entryLocationStr);
                } else if (entryLocationStr.startsWith(Europe.getXMLElementTagName())) {
                    entryLocation = new Europe(getGame(), entryLocationStr);
                } else if (entryLocationStr.startsWith(ColonyTile.getXMLElementTagName())) {
                    entryLocation = new ColonyTile(getGame(), entryLocationStr);
                } else if (entryLocationStr.startsWith(Building.getXMLElementTagName())) {
                    entryLocation = new Building(getGame(), entryLocationStr);
                } else if (entryLocationStr.startsWith(Unit.getXMLElementTagName())) {
                    entryLocation = new Unit(getGame(), entryLocationStr);
                } else {
                    logger.warning("Unknown type of Location (2).");
                    entryLocation = new Tile(getGame(), entryLocationStr);
                }
            }
        }

        final String locationStr = in.getAttributeValue(null, "location");
        if (locationStr != null) {
            // TODO: Fix properly bug #1755566 and remove this
            if (location != null) {
                location.remove(this);
            }
            location = (Location) getGame().getFreeColGameObject(locationStr);
            if (location == null) {
                if (locationStr.startsWith(Tile.getXMLElementTagName())) {
                    location = new Tile(getGame(), locationStr);
                } else if (locationStr.startsWith(Colony.getXMLElementTagName())) {
                    location = new Colony(getGame(), locationStr);
                } else if (locationStr.startsWith(IndianSettlement.getXMLElementTagName())) {
                    location = new IndianSettlement(getGame(), locationStr);
                } else if (locationStr.startsWith(Europe.getXMLElementTagName())) {
                    location = new Europe(getGame(), locationStr);
                } else if (locationStr.startsWith(ColonyTile.getXMLElementTagName())) {
                    location = new ColonyTile(getGame(), locationStr);
                } else if (locationStr.startsWith(Building.getXMLElementTagName())) {
                    location = new Building(getGame(), locationStr);
                } else if (locationStr.startsWith(Unit.getXMLElementTagName())) {
                    location = new Unit(getGame(), locationStr);
                } else {
                    logger.warning("Unknown type of Location (3): " + locationStr);
                    location = new Tile(getGame(), locationStr);
                }
            }
        }

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals(UnitContainer.getXMLElementTagName())) {
                unitContainer = (UnitContainer) getGame().getFreeColGameObject(in.getAttributeValue(null, "ID"));
                if (unitContainer != null) {
                    unitContainer.readFromXML(in);
                } else {
                    unitContainer = new UnitContainer(getGame(), this, in);
                }
            } else if (in.getLocalName().equals(GoodsContainer.getXMLElementTagName())) {
                goodsContainer = (GoodsContainer) getGame().getFreeColGameObject(in.getAttributeValue(null, "ID"));
                if (goodsContainer != null) {
                    goodsContainer.readFromXML(in);
                } else {
                    goodsContainer = new GoodsContainer(getGame(), this, in);
                }

            }
        }
        
        if (unitContainer == null && hasAbility("model.ability.carryUnits")) {
            logger.warning("Carrier did not have a \"unitContainer\"-tag.");
            unitContainer = new UnitContainer(getGame(), this);

        }
        if (goodsContainer == null && hasAbility("model.ability.carryGoods")) {
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
        return getInitialHitpoints(getUnitType()) - getHitpoints();
    }
    
    /**
     * Return the type of the image which will be used to draw the path
     *
     * @returns a <code>String</code> to form the resource key
     */
    public String getPathTypeImage() {
        if (isMounted()) {
            return "horse";
        } else {
            return unitType.getPathImage();
        }
    }

    /**
     * Damage a building or a ship or steal some goods or gold. It's called
     * from attack when an indian attacks a colony and lose the combat with
     * ATTACK_LOSS as result
     *
     * @param colony The attacked colony
     */
    private void pillageColony(Colony colony) {
        ArrayList<Building> buildingList = new ArrayList<Building>();
        ArrayList<Unit> shipList = new ArrayList<Unit>();
        ArrayList<Goods> goodsList = colony.getGoodsContainer().getCompactGoods();
        
        Iterator<Building> itB = colony.getBuildingIterator();
        while (itB.hasNext()) {
            Building building = itB.next();
            if (building.canBeDamaged()) {
                buildingList.add(building);
            }
        }
        
        List<Unit> unitList = colony.getTile().getUnitList();
        for (Unit unit : unitList) {
            if (unit.isNaval()) {
                shipList.add(unit);
            }
        }
        
        String nation = getOwner().getNationAsString();
        String unitName = getName();
        String colonyName = colony.getName();
        
        int random = getGame().getModelController().getRandom(getID() + "pillageColony",
                buildingList.size() + goodsList.size() + shipList.size() + 1);
        if (random < buildingList.size()) {
            Building building = buildingList.get(random);
            colony.addModelMessage(colony, "model.unit.buildingDamaged",
                    new String[][] {
                        {"%building%", building.getName()}, {"%colony%", colonyName},
                        {"%enemyNation%", nation}, {"%enemyUnit%", unitName}},
                    ModelMessage.DEFAULT, colony);
            building.damage();
        } else if (random < buildingList.size() + goodsList.size()) {
            Goods goods = goodsList.get(random - buildingList.size());
            goods.setAmount(Math.min(goods.getAmount() / 2, 50));
            colony.removeGoods(goods);
            if (getSpaceLeft() > 0) add(goods);
            colony.addModelMessage(colony, "model.unit.goodsStolen",
                    new String[][] {
                        {"%amount%", String.valueOf(goods.getAmount())},
                        {"%goods%", goods.getName()}, {"%colony%", colonyName},
                        {"%enemyNation%", nation}, {"%enemyUnit%", unitName}},
                    ModelMessage.DEFAULT, goods);
        } else if (random < buildingList.size() + goodsList.size() + shipList.size()) {
            Unit ship = shipList.get(random - buildingList.size() - goodsList.size());
            ship.shipDamaged();
        } else { // steal gold
            int gold = colony.getOwner().getGold() / 10;
            colony.getOwner().modifyGold(-gold);
            getOwner().modifyGold(gold);
            colony.addModelMessage(colony, "model.unit.indianPlunder",
                    new String[][] {
                        {"%amount%", String.valueOf(gold)}, {"%colony%", colonyName},
                        {"%enemyNation%", nation}, {"%enemyUnit%", unitName}},
                    ModelMessage.DEFAULT, colony);
        }
    }

    /**
     * Check whether some indian converts due to the attack or they burn all missions
     *
     * @param indianSettlement The attacked indian settlement
     */
    private void getConvert(IndianSettlement indianSettlement) {
        ModelController modelController = getGame().getModelController();
        int random = modelController.getRandom(getID() + "getConvert", 100);
        int convertProbability = (5 - getOwner().getDifficulty()) * 10; // 50% - 10%
        Modifier modifier = getOwner().getModifier("model.ability.nativeConvertBonus");
        if (modifier != null) {
            convertProbability += modifier.getValue();
        }
        // TODO: it should be bigger when tension is high
        int burnProbability = (1 + getOwner().getDifficulty()) * 2; // 2% - 10%
        
        if (random < convertProbability) {
            Unit missionary = indianSettlement.getMissionary();
            if (missionary != null && missionary.getOwner() == getOwner() &&
                    getGame().getViewOwner() == null && indianSettlement.getUnitCount() > 1) {
                List<UnitType> converts = FreeCol.getSpecification().getUnitTypesWithAbility("model.ability.convert");
                if (converts.size() > 0) {
                    indianSettlement.getFirstUnit().dispose();
                    random = modelController.getRandom(getID() + "getConvertType", converts.size());
                    modelController.createUnit(getID() + "indianConvert", getLocation(),
                            getOwner(), converts.get(random));
                }
            }
        } else if (random >= 100 - burnProbability) {
            boolean burn = false;
            List<Settlement> settlements = indianSettlement.getOwner().getSettlements();
            for (Settlement settlement : settlements) {
                IndianSettlement indian = (IndianSettlement) settlement;
                Unit missionary = indian.getMissionary();
                if (missionary != null && missionary.getOwner() == getOwner()) {
                    burn = true;
                    indian.setMissionary(null);
                }
            }
            if (burn) {
                addModelMessage(this, "model.unit.burnMissions", new String[][] {
                    {"%nation%", getOwner().getNationAsString()},
                    {"%enemyNation%", indianSettlement.getOwner().getNationAsString()}},
                    ModelMessage.DEFAULT, indianSettlement);
            }
        }
    }
}
