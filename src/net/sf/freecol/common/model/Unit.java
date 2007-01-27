
package net.sf.freecol.common.model;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.util.EmptyIterator;

import org.w3c.dom.Element;


/**
* Represents all pieces that can be moved on the map-board.
* This includes: colonists, ships, wagon trains e.t.c.
*
* <br><br>
*
* Every <code>Unit</code> is owned by a {@link Player} and has a
* {@link Location}.
*/
public class Unit extends FreeColGameObject implements Location, Locatable, Ownable, Nameable {

    public static final  String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final  String  LICENSE   = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static final Logger logger = Logger.getLogger(Unit.class.getName());

    /** The type of a unit; used only for gameplaying purposes NOT painting purposes. */
    public static final int FREE_COLONIST = 0,
                            EXPERT_FARMER = 1,
                            EXPERT_FISHERMAN = 2,
                            EXPERT_FUR_TRAPPER = 3,
                            EXPERT_SILVER_MINER = 4,
                            EXPERT_LUMBER_JACK = 5,
                            EXPERT_ORE_MINER = 6,
                            MASTER_SUGAR_PLANTER = 7,
                            MASTER_COTTON_PLANTER = 8,
                            MASTER_TOBACCO_PLANTER = 9,
                            FIREBRAND_PREACHER = 10,
                            ELDER_STATESMAN = 11,
                            MASTER_CARPENTER = 12,
                            MASTER_DISTILLER = 13,
                            MASTER_WEAVER = 14,
                            MASTER_TOBACCONIST = 15,
                            MASTER_FUR_TRADER = 16,
                            MASTER_BLACKSMITH = 17,
                            MASTER_GUNSMITH = 18,
                            SEASONED_SCOUT = 19,
                            HARDY_PIONEER = 20,
                            VETERAN_SOLDIER = 21,
                            JESUIT_MISSIONARY = 22,
                            INDENTURED_SERVANT = 23,
                            PETTY_CRIMINAL = 24,
                            INDIAN_CONVERT = 25,
                            BRAVE = 26,
                            COLONIAL_REGULAR = 27,
                            KINGS_REGULAR = 28,
                            CARAVEL = 29,
                            FRIGATE = 30,
                            GALLEON = 31,
                            MAN_O_WAR = 32,
                            MERCHANTMAN = 33,
                            PRIVATEER = 34,
                            ARTILLERY = 35,
                            DAMAGED_ARTILLERY = 36,
                            TREASURE_TRAIN = 37,
                            WAGON_TRAIN = 38,
                            MILKMAID = 39,
                            REVENGER = 40,
                            FLYING_DUTCHMAN = 41,
                            UNDEAD = 42,
                            UNIT_COUNT = 43;

    /** A state a Unit can have. */
    public static final int ACTIVE = 0,
                            FORTIFIED = 1,
                            SENTRY = 2,
                            IN_COLONY = 3,
                            PLOW = 4,
                            BUILD_ROAD = 5,
                            TO_EUROPE = 6,
                            IN_EUROPE = 7,
                            TO_AMERICA = 8,
                            FORTIFYING = 9,
                            NUMBER_OF_STATES = 10;


    /**
     * A move type.
     * @see #getMoveType(int direction)
     */
    public static final int MOVE = 0,
                            MOVE_HIGH_SEAS = 1,
                            ATTACK = 2,
                            EMBARK = 3,
                            DISEMBARK = 4,
                            ENTER_INDIAN_VILLAGE_WITH_FREE_COLONIST = 5,
                            ENTER_INDIAN_VILLAGE_WITH_SCOUT = 6,
                            ENTER_INDIAN_VILLAGE_WITH_MISSIONARY = 7,
                            ENTER_FOREIGN_COLONY_WITH_SCOUT = 8,
                            ENTER_SETTLEMENT_WITH_CARRIER_AND_GOODS = 9,
                            EXPLORE_LOST_CITY_RUMOUR = 10,
                            ILLEGAL_MOVE = 11;

    public static final int ATTACK_GREAT_LOSS = -2,
                            ATTACK_LOSS = -1,
                            ATTACK_EVADES = 0,
                            ATTACK_WIN  = 1,
                            ATTACK_GREAT_WIN = 2,
                            ATTACK_DONE_SETTLEMENT = 3; // The last defender of the settlement has died.

    public static final int MUSKETS_TO_ARM_INDIAN = 25,
                            HORSES_TO_MOUNT_INDIAN = 25;


    private int             type;
    private  UnitType       unitType;
    private boolean         armed,
                            mounted,
                            missionary;
    private int             movesLeft;      // Always use getMovesLeft()
    private int             state;
    private int             workLeft;       // expressed in number of turns, '-1' if a Unit can stay in its state forever
    private int             numberOfTools;
    private int             hitpoints;      // For now; only used by ships when repairing.
    private Player          owner;
    private UnitContainer   unitContainer;
    private GoodsContainer  goodsContainer;
    private Location        entryLocation;
    private Location        location;
    private IndianSettlement indianSettlement = null; // only used by BRAVE.
    private Location        destination = null;
    
    // to be used only for type == TREASURE_TRAIN
    private int             treasureAmount;

    private int             workType; // What type of goods this unit produces in its occupation.
    private int             experience;

    private int             turnsOfTraining = 0;
    private int             trainingType = -1;

    /** The individual name of this unit, not of the unit type. */
    private String          name = null;

    /**
     * The amount of goods carried by this unit.
     * This variable is only used by the clients.
     * A negative value signals that the variable
     * is not in use.
     * 
     * @see #getVisibleGoodsCount()
     */
    private int             visibleGoodsCount;


    /**
    * This constructor should only be used by subclasses.
    */
    protected Unit() {

    }


    /**
    * Initiate a new <code>Unit</code> of a specified type with the state set
    * to {@link #ACTIVE} if a carrier and {@link #SENTRY} otherwise. The
    * {@link Location} is set to <i>null</i>.
    *
    * @param game The <code>Game</code> in which this <code>Unit</code> belong.
    * @param owner The Player owning the unit.
    * @param type The type of the unit.
    */
    public Unit(Game game, Player owner, int type) {
        this(game, null, owner, type, ACTIVE);//isCarrier(type)?ACTIVE:SENTRY);
    }


    /**
    * Initiate a new <code>Unit</code> with the specified parameters.
    *
    * @param game The <code>Game</code> in which this <code>Unit</code> belong.
    * @param location The <code>Location</code> to place this <code>Unit</code> upon.
    * @param owner The <code>Player</code> owning this unit.
    * @param type The type of the unit.
    * @param s The initial state for this Unit (one of {@link #ACTIVE}, {@link #FORTIFIED}...).
    */
    public Unit(Game game, Location location, Player owner, int type, int s) {
        this(game, location, owner, type, s,
                (type == VETERAN_SOLDIER),
                (type == SEASONED_SCOUT),
                (type == HARDY_PIONEER) ? 100 : 0,
                (type == JESUIT_MISSIONARY));
    }


    /**
    * Initiate a new <code>Unit</code> with the specified parameters.
    *
    * @param game The <code>Game</code> in which this <code>Unit</code> belong.
    * @param location The <code>Location</code> to place this <code>Unit</code> upon.
    * @param owner The <code>Player</code> owning this unit.
    * @param type The type of the unit.
    * @param s The initial state for this Unit (one of {@link #ACTIVE}, {@link #FORTIFIED}...).
    * @param armed Determines wether the unit should be armed or not.
    * @param mounted Determines wether the unit should be mounted or not.
    * @param numberOfTools The number of tools the unit will be carrying.
    * @param missionary Determines wether this unit should be dressed like a missionary or not.
    */
    public Unit(Game game, Location location, Player owner, int type, int s,
                boolean armed, boolean mounted, int numberOfTools, boolean missionary) {
        super(game);

        visibleGoodsCount = -1;
        unitContainer = new UnitContainer(game, this);
        goodsContainer = new GoodsContainer(game, this);

        this.owner = owner;
        this.type = type;
        unitType = FreeCol.specification.unitType( type );
        this.armed = armed;
        this.mounted = mounted;
        this.numberOfTools = numberOfTools;
        this.missionary = missionary;

        setLocation(location);

        state = s;
        workLeft = -1;
        workType = Goods.FOOD;
        experience = 0;

        this.movesLeft = getInitialMovesLeft();
        setHitpoints(getInitialHitpoints(getType()));       

        getOwner().invalidateCanSeeTiles();
    }


    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param game The <code>Game</code> in which this <code>Unit</code> belong.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public Unit(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);
        readFromXML(in);
    }
    
    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param game The <code>Game</code> in which this <code>Unit</code> belong.
     * @param e An XML-element that will be used to initialize
     *      this object.
     */
    public Unit(Game game, Element e) {
        super(game, e);
        readFromXMLElement(e);
    }

    /**
     * Initiates a new <code>Unit</code> 
     * with the given ID. The object should later be
     * initialized by calling either
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
     * @return A name for this unit, as a location.
     */
    public String getLocationName() {
        return Messages.message("onBoard", new String[][] {{"%unit%", getName()}});
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
     * Set the <code>UnitType</code> value.
     *
     * @param newUnitType The new UnitType value.
     */
    public final void setUnitType(final UnitType newUnitType) {
        this.unitType = newUnitType;
    }

    /**
    * Returns the current amount of treasure in this unit.
    * Should be type of TREASURE_TRAIN.
    * @return The amount of treasure.
    */
    public int getTreasureAmount() {

        if (getType() == TREASURE_TRAIN) {
            return treasureAmount;
        }

        throw new IllegalStateException();
    }


    /**
     * The current amount of treasure in this unit.
     * Should be type of TREASURE_TRAIN.
     * @param amt    The amount of treasure
     */
    public void setTreasureAmount(int amt) {
        if (getType() == TREASURE_TRAIN) {
            this.treasureAmount = amt;
        } else {
            throw new IllegalStateException();
        }
    }


    /**
    * Sells the given goods from this unit to the given settlement.
    * The owner of this unit gets the gold and the owner of
    * the settlement is charged for the deal.
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
         Value already tested. This test is needed because the opponent's
         amount of gold is hidden for the client:
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
            int value = ((IndianSettlement) settlement).getPrice(goods) / 1000;
            settlement.getOwner().modifyTension(getOwner(), -value);
        }
    }


    /**
    * Transfers the given goods from this unit to the given settlement.
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

        int type = goods.getType();
        int amount = goods.getAmount();

        goods.setLocation(settlement);
        setMovesLeft(0);

        if (settlement instanceof IndianSettlement) {
            int value = ((IndianSettlement) settlement).getPrice(goods) / 100;
            settlement.getOwner().modifyTension(getOwner(), -value);
        } else {
            addModelMessage(settlement.getOwner(), "model.unit.gift",
                            new String[][] {{"%player%", getOwner().getNationAsString()},
                                            {"%type%", Goods.getName(type)},
                                            {"%amount%", Integer.toString(amount)},
                                            {"%colony%", ((Colony) settlement).getName()}},
                            ModelMessage.GIFT_GOODS, new Goods(type));
        }
    }
 
    /**
     * Checks if the treasure train can be cashed in at it's
     * current <code>Location</code>.
     * 
     * @return <code>true</code> if the treasure train can be
     *      cashed in.
     * @exception IllegalStateException if this unit is not a treasure train.
     */
    public boolean canCashInTreasureTrain() {
        return canCashInTreasureTrain(getLocation());
    }
    
    /**
     * Checks if the treasure train can be cashed in at the
     * given <code>Location</code>.
     * 
     * @param location The <code>Location</code>.
     * @return <code>true</code> if the treasure train can be
     *      cashed in.
     * @exception IllegalStateException if this unit is not a treasure train.
     */
    public boolean canCashInTreasureTrain(Location location) {
        if (getType() != TREASURE_TRAIN) {
            throw new IllegalStateException("Not a treasure train");
        }
        return location instanceof Tile && location.getTile().getColony() != null
                || location instanceof Europe
                || (location instanceof Unit && ((Unit) location).getLocation() instanceof Europe);
    }
    

    /**
     * Transfers the gold carried by this unit to the {@link Player owner}.
     *
     * @exception IllegalStateException if this unit is not a treasure train.
     *                                  or if it cannot be cashed in at it's current
     *                                  location.
     */
    public void cashInTreasureTrain() {
        if (getType() != TREASURE_TRAIN) {
            throw new IllegalStateException("Not a treasure train");
        }

        if (canCashInTreasureTrain()) {
            boolean inEurope = (getLocation() instanceof Unit && ((Unit) getLocation()).getLocation() instanceof Europe);
            int cashInAmount = (getOwner().hasFather(FoundingFather.HERNAN_CORTES) || inEurope) ? getTreasureAmount() : getTreasureAmount() / 2; 
	    cashInAmount = cashInAmount * (100 - getOwner().getTax()) / 100;
            FreeColGameObject o = getOwner();
            if (inEurope) {
                o = getOwner().getEurope();
            }
            getOwner().modifyGold(cashInAmount);
            addModelMessage(o, "model.unit.cashInTreasureTrain",
                            new String[][] {{"%amount%", Integer.toString(getTreasureAmount())},
                                            {"%cashInAmount%", Integer.toString(cashInAmount)}},
                            ModelMessage.DEFAULT);
            dispose();
        } else {
            throw new IllegalStateException("Cannot cash in treasure train at the current location.");
        }
    }


    /**
    * Checks if this <code>Unit</code> is a colonist. A <code>Unit</code>
    * is a colonist if it can build a new <code>Colony</code>.
    *
    * @return <i>true</i> if this unit is a colonist and
    *         <i>false</i> otherwise.
    */
    public boolean isColonist() {

        return unitType.hasAbility( "found-colony" );
    }


    /**
    * Gets the number of turns this unit has to train to
    * become the current {@link #getTrainingType training type}.
    *
    * @return The turns of training needed to become the current
    *         training type, or <code>Integer.MAX_VALUE</code> if
    *         if no training type is specified.
    * @see #getTrainingType
    * @see #getTurnsOfTraining
    */
    public int getNeededTurnsOfTraining() {

        if (trainingType != -1) {
            return 2 + getSkillLevel(trainingType);
        }

        return Integer.MAX_VALUE;
    }


    /**
    * Gets the skill level.
    * @return The level of skill for this unit. A higher
    *       value signals a more advanced type of units.
    */
    public int getSkillLevel() {
        return getSkillLevel(getType());
    }


    /**
    * Gets the skill level of the given type of <code>Unit</code>.
    * 
    * @param unitTypeIndex The type of <code>Unit</code>.
    * @return The level of skill for the given unit. A higher
    *       value signals a more advanced type of units.
    */
    public static int getSkillLevel( int unitTypeIndex ) {

        UnitType  unitType = FreeCol.specification.unitType(unitTypeIndex);

        if ( unitType.hasSkill() ) {

            return unitType.skill;
        }

        throw new IllegalStateException();
    }


    /**
    * Gets the number of turns this unit has been training.
    *
    * @return The number of turns of training this
    *       <code>Unit</code> has received.
    * @see #setTurnsOfTraining
    * @see #getTrainingType
    * @see #getNeededTurnsOfTraining
    */
    public int getTurnsOfTraining() {
        return turnsOfTraining;
    }


    /**
    * Sets the number of turns this unit has been training.
    * @param turnsOfTraining The number of turns of training this
    *       <code>Unit</code> has received.
    * @see #getNeededTurnsOfTraining
    */
    public void setTurnsOfTraining(int turnsOfTraining) {
        this.turnsOfTraining = turnsOfTraining;
    }



    /**
    * Gets the unit type this <code>Unit</code> is training for.
    *
    * @return The type of <code>Unit</code> which this 
    *       <code>Unit</code> is currently working to become.
    * @see #getTurnsOfTraining
    * @see #setTrainingType
    */
    public int getTrainingType() {
        return trainingType;
    }


    /**
    * Sets the unit type this <code>Unit</code> is training for.
    * Use <code>-1</code> for no type at all.
    *
    * @param trainingType The type of <code>Unit</code> which this 
    *       <code>Unit</code> should currently working to become.
    * @see #getTurnsOfTraining
    * @see #getTrainingType
    */
    public void setTrainingType(int trainingType) {
        if (getType() == PETTY_CRIMINAL || getType() == INDENTURED_SERVANT) {
            this.trainingType = FREE_COLONIST;
        } else {
            this.trainingType = trainingType;
        }
    }

    /**
    * Gets the experience of this <code>Unit</code> at its current
    * workType.
    *
    * @return The experience of this <code>Unit</code> at its current
    * workType.
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
    * <code>Unit</code>.
    * @see #getExperience
    */
    public void modifyExperience(int value) {
        experience += value;
    }


     /**
     * Gets the type of goods this unit is producing in its current occupation.
     * @return The type of goods this unit would produce.
     */
    public int getWorkType() {
        if (getLocation() instanceof Building) {
          return ((Building) getLocation()).getGoodsOutputType();
        }
        return workType;
    }


     /**
     * Sets the type of goods this unit is producing in its current occupation.
     * @param type The type of goods to attempt to produce.
     */
    public void setWorkType(int type) {
        if (!Goods.isFarmedGoods(type)) {
            return;
        }
        if (workType != type) {
            experience = 0;
        }
        workType = type;
    }


    /**
    * Gets the type of goods this unit is an expert
    * at producing.
    *
    * @return The type of goods or <code>-1</code> if this unit is not an
    *         expert at producing any type of goods.
    * @see ColonyTile#getExpertForProducing
    */
    public int getExpertWorkType() {

        GoodsType  expertProduction = unitType.expertProduction;
        return (expertProduction != null) ? expertProduction.index : -1;
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
    * Finds a shortest path from the current <code>Tile</code>
    * to the one specified. Only paths on water are allowed if
    * <code>isNaval()</code> and only paths on land if not.
    *
    * <br><br>
    *
    * The <code>Tile</code> at the <code>end</code> will not be
    * checked against the legal moves of this <code>Unit</code>.
    *
    * @param end The <code>Tile</code> in which the path ends.
    * @return A <code>PathNode</code> for the first tile in the path. Calling
    *             {@link PathNode#getTile} on this object, will return the
    *             <code>Tile</code> right after the specified starting tile, and
    *             {@link PathNode#getDirection} will return the direction you
    *             need to take in order to reach that tile.
    *             This method returns <code>null</code> if no path is found.
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
    * Finds a shortest path from the current <code>Tile</code>
    * to the one specified. Only paths on water are allowed if
    * <code>isNaval()</code> and only paths on land if not.
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
    * Returns the number of turns this <code>Unit</code> will have to use
    * in order to reach the given <code>Tile</code>.
    *
    * @param end The <code>Tile</code> to be reached by this <code>Unit</code>.
    * @return The number of turns it will take to reach the <code>end</code>,
    *        or <code>Integer.MAX_VALUE</code> if no path can be found.
    */
    public int getTurnsToReach(Tile end) {
        return getTurnsToReach(getTile(), end);
    }
    
    
    /**
     * Returns the number of turns this <code>Unit</code> will have to use
     * in order to reach the given <code>Tile</code>.
     *
     * @param start The <code>Tile</code> to start the search from.
     * @param end The <code>Tile</code> to be reached by this <code>Unit</code>.
     * @return The number of turns it will take to reach the <code>end</code>,
     *        or <code>Integer.MAX_VALUE</code> if no path can be found.
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
    * Gets the cost of moving this <code>Unit</code> onto the given <code>Tile</code>.
    * A call to {@link #getMoveType(Tile)} will return <code>ILLEGAL_MOVE</code>, if {@link #getMoveCost}
    * returns a move cost larger than the {@link #getMovesLeft moves left}.
    *
    * @param target The <code>Tile</code> this <code>Unit</code> will move onto.
    * @return The cost of moving this unit onto the given <code>Tile</code>.
    * @see Tile#getMoveCost
    */
    public int getMoveCost(Tile target) {
        return getMoveCost(getTile(), target, getMovesLeft());
    }

    /**
    * Gets the cost of moving this <code>Unit</code> from the given <code>Tile</code>
    * onto the given <code>Tile</code>.
    * A call to {@link #getMoveType(Tile, Tile)} will return <code>ILLEGAL_MOVE</code>, if {@link #getMoveCost}
    * returns a move cost larger than the {@link #getMovesLeft moves left}.
    *
    * @param from The <code>Tile</code> this <code>Unit</code> will move from.
    * @param target The <code>Tile</code> this <code>Unit</code> will move onto.
    * @param ml The amount of moves this Unit has left.
    * @return The cost of moving this unit onto the given <code>Tile</code>.
    * @see Tile#getMoveCost
    */
    public int getMoveCost(Tile from, Tile target, int ml) {
        // Remember to also change map.findPath(...) if you change anything here.

        int cost = target.getMoveCost(from);
        
        // Using +2 in order to make 1/3 and 2/3 move count as 3/3, only when getMovesLeft > 0
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
     * @return <code>true</code> if this <code>Player</code> can trade
     *       with the given <code>Settlement</code>. The unit will for
     *       instance need to be a {@link #isCarrier() carrier} and have
     *       goods onboard.
     */
    public boolean canTradeWith(Settlement settlement) {
        return (isCarrier() && 
                goodsContainer.getGoodsCount() > 0 &&
                getOwner().getStance(settlement.getOwner()) != Player.WAR &&
                ((settlement instanceof IndianSettlement) ||
                 getOwner().hasFather(FoundingFather.JAN_DE_WITT)));
    }

    /**
     * Gets the type of a move made in a specified direction.
     *
     * @param direction The direction of the move.
     * @return The move type. Notice: <code>Unit.ILLEGAL_MOVE</code>
     *         when there are no moves left.
     */
    public int getMoveType(int direction) {
        if (getTile() == null) {
            throw new IllegalStateException("getTile() == null");
        }

        Tile target = getGame().getMap().getNeighbourOrNull(direction, getTile());

        return getMoveType(target);
    }

    /**
     * Gets the type of a move that is made when moving to the
     * specified <code>Tile</code> from the current one.
     *
     * @param target The target tile of the move
     * @return The move type. Notice: <code>Unit.ILLEGAL_MOVE</code>
     *         when there are no moves left.
     */
    public int getMoveType(Tile target) {
        return getMoveType(getTile(), target, getMovesLeft());
    }
    
    /**
     * Gets the type of a move that is made when moving to the
     * specified <code>Tile</code> from the specified <code>Tile</code>.
     *
     * @param target The origin tile of the move
     * @param target The target tile of the move
     * @param ml The amount of moves this Unit has left
     * @return The move type. Notice: <code>Unit.ILLEGAL_MOVE</code>
     *         when there are no moves left.
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
     * Gets the type of a move that is made when moving to the
     * specified <code>Tile</code> from the specified <code>Tile</code>.
     *
     * @param target The origin tile of the move
     * @param target The target tile of the move
     * @param ml The amount of moves this Unit has left
     * @return The move type. Notice: <code>Unit.ILLEGAL_MOVE</code>
     *         when there are no moves left.
     */
    private int getNavalMoveType(Tile from, Tile target, int ml) {
        if (target == null) { // TODO: do not allow "MOVE_HIGH_SEAS" north and south.
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
                    logger.fine("Trying to enter another player's settlement with " +
                                   getName());
                    return ILLEGAL_MOVE;
                }
            } else if (target.getDefendingUnit(this) != null &&
                       target.getDefendingUnit(this).getOwner() != getOwner()) {
                logger.fine("Trying to sail into tile occupied by enemy units with " +
                               getName());
                return ILLEGAL_MOVE;
            } else {
                // Check for disembark.
                Iterator unitIterator = getUnitIterator();

                while (unitIterator.hasNext()) {
                    Unit u = (Unit) unitIterator.next();
                    if (u.getMovesLeft() > 0) {
                        return DISEMBARK;
                    }
                }
                logger.fine("No units to disembark from " + getName());
                return ILLEGAL_MOVE;
            }
        } else if (target.getDefendingUnit(this) != null &&
                   target.getDefendingUnit(this).getOwner() != getOwner()) {
            // enemy units at sea
            if (isOffensiveUnit()) {
                return ATTACK;
            } else {
                return ILLEGAL_MOVE;
            }
        } else if (target.getType() == Tile.HIGH_SEAS) {
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
     * Gets the type of a move that is made when moving to the
     * specified <code>Tile</code> from the specified <code>Tile</code>.
     *
     * @param target The origin tile of the move
     * @param target The target tile of the move
     * @param ml The amount of moves this Unit has left
     * @return The move type. Notice: <code>Unit.ILLEGAL_MOVE</code>
     *         when there are no moves left.
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
                    logger.fine("Trying to disembark into foreign colony with " +
                            getName());
                    return ILLEGAL_MOVE;
                } else if (settlement instanceof IndianSettlement) {
                    if (isScout()) {
                        return ENTER_INDIAN_VILLAGE_WITH_SCOUT;
                    } else if (isMissionary()) {
                        return ENTER_INDIAN_VILLAGE_WITH_MISSIONARY;
                    } else if (isOffensiveUnit()) {
                        return ATTACK;                            
                    } else if (((getType() == FREE_COLONIST) ||
                            (getType() == INDENTURED_SERVANT))) {
                        return ENTER_INDIAN_VILLAGE_WITH_FREE_COLONIST;
                    } else {
                        logger.fine("Trying to enter Indian settlement with " +
                                getName());
                        return ILLEGAL_MOVE;
                    }
                } else if (settlement instanceof Colony) {
                    if (isScout()) {
                        return ENTER_FOREIGN_COLONY_WITH_SCOUT;
                    } else if (isOffensiveUnit()) {
                        return ATTACK;
                    } else {
                        logger.fine("Trying to enter foreign colony with " +
                                getName());
                        return ILLEGAL_MOVE;
                    }
                }
            } else if (target.getDefendingUnit(this) != null &&
                       target.getDefendingUnit(this).getOwner() != getOwner()) {
                if (from.isLand()) {
                    if (isOffensiveUnit()) {
                        return ATTACK;
                    } else {
                        logger.fine("Trying to attack with civilian " + getName());
                        return ILLEGAL_MOVE;
                    }
                } else {
                    logger.fine("Attempting marine assault with " + getName());
                    return ILLEGAL_MOVE;
                }
            } else if (target.getFirstUnit() != null && target.getFirstUnit().isNaval() &&
                       target.getFirstUnit().getOwner() != getOwner()) {
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
            if (target.getFirstUnit() == null || 
                target.getFirstUnit().getNation() != getNation()) {
                logger.fine("Trying to embark on tile occupied by foreign units with " +
                               getName());
                return ILLEGAL_MOVE;
            } else {
                Iterator unitIterator = target.getUnitIterator();
                        
                while (unitIterator.hasNext()) {
                    Unit u = (Unit) unitIterator.next();
                            
                    if (u.getSpaceLeft() >= getTakeSpace()) {
                        return EMBARK;
                    }
                }
                logger.fine("Trying to board full vessel with " +
                               getName());
                return ILLEGAL_MOVE;
            }
        }

        logger.info("Default illegal move for " + getName());
        return ILLEGAL_MOVE;
    }

    /**
    * Sets the <code>movesLeft</code>. 
    * 
    * @param movesLeft The new amount of moves left this
    *       <code>Unit</code> should have. If <code>movesLeft < 0</code>
    *       then <code>movesLeft = 0</code>.
    */
    public void setMovesLeft(int movesLeft) {
        if (movesLeft < 0) {
            movesLeft = 0;
        }

        this.movesLeft = movesLeft;
    }


    /**
    * Gets the amount of space this <code>Unit</code> takes when put on a carrier.
    *
    * @return The space this <code>Unit</code> takes.
    */
    public int getTakeSpace() {
        if (getType() == TREASURE_TRAIN) {
            return 6;
        } else if (isCarrier()) {
            return 100000; // Not possible to put on a carrier.
        } else {
            return 1;
        }
    }


    /**
    * Gets the line of sight of this <code>Unit</code>. That is the
    * distance this <code>Unit</code> can spot new tiles, enemy unit e.t.c.
    *
    * @return The line of sight of this <code>Unit</code>.
    */
    public int getLineOfSight() {
        int type = getType();

        if (type == REVENGER || type == FLYING_DUTCHMAN) {
            return 3;
        } else if (isScout() || type == FRIGATE || type == GALLEON || type == MAN_O_WAR || type == PRIVATEER) {
            return 2;
        } else if (getOwner().hasFather(FoundingFather.HERNANDO_DE_SOTO)) {
            return 2;
        } else {
            return 1;
        }
    }


    /**
    * Checks if this <code>Unit</code> is a scout.
    *
    * @return <i>true</i> if this <code>Unit</code> is a scout and
    *         <i>false</i> otherwise.
    */
    public boolean isScout() {

        return isMounted()  &&  ! isArmed();
    }

    /**
    * Moves this unit in the specified direction.
    *
    * @param direction The direction
    * @see #getMoveType(int)
    * @exception IllegalStateException If the move is illegal.
    */
    public void move(int direction) {
        int type = getMoveType(direction);

        // For debugging:
        switch (type) {
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
            throw new IllegalStateException("\nIllegal move requested: " + type
                                            + " while trying to move a " + getName() + " located at "
                                            + getTile().getPosition().toString() + ". Direction: "
                                            + direction + " Moves Left: " + getMovesLeft());
        }

        setState(ACTIVE);
        setStateToAllChildren(SENTRY);

        Tile newTile = getGame().getMap().getNeighbourOrNull(direction, getTile());

        int moveCost = getMoveCost(newTile);

        if (newTile != null) {
            setLocation(newTile);
            activeAdjacentSentryUnits(newTile);
        } else {
            throw new IllegalStateException("Illegal move requested!");
        }
        setMovesLeft(getMovesLeft() - moveCost);
    }

    /**
     * Active units with sentry state wich are adjacent to a specified tile
     *
     * @param tile The tile to iterate over adjacent tiles.
     */
    public void activeAdjacentSentryUnits(Tile tile) {
        Map map = getGame().getMap();
        Iterator it = map.getAdjacentIterator(tile.getPosition());
        while(it.hasNext()) {
            Iterator unitIt = map.getTile((Position) it.next()).getUnitIterator();
            while (unitIt.hasNext()) {
                Unit unit = (Unit) unitIt.next();
                if (unit.getState() == Unit.SENTRY) {
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
    *            NullPointerException If <code>unit == null</code>.
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
    * @exception IllegalStateException If the carrier is on another tile than this unit.
    */
    public void boardShip(Unit carrier) {
        if (isCarrier()) {
            throw new IllegalStateException("A carrier cannot board another carrier!");
        }

        if (getTile() == carrier.getTile() && carrier.getState() != TO_EUROPE && carrier.getState() != TO_AMERICA) {
            setLocation(carrier);
            setState(SENTRY);
        } else {
            throw new IllegalStateException("It is not allowed to board a ship on another tile.");
        }

        if (getTile() != null && getTile().getColony() != null && getTile().getColony().getUnitCount() <= 0) {
            getTile().getColony().dispose();
        }
    }


    /**
    * Leave the ship. This method should only be invoked if the ship is in a harbour.
    *
    * @exception IllegalStateException If not in harbour.
    * @exception ClassCastException If not this unit is located on a ship.
    */
    public void leaveShip() {
        Unit carrier = (Unit) getLocation();
        Location l = carrier.getLocation();

        if (l instanceof Europe && carrier.getState() != TO_EUROPE && carrier.getState() != TO_AMERICA) {
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
        if (l instanceof Europe && getState() != TO_EUROPE && getState() != TO_AMERICA) {
            return true;
        } else if (getTile() == null) {
            // this should not happen, but it does
            return false;
        } else if (getTile().getSettlement() != null && getTile().getSettlement() instanceof Colony) {
            return true;
        } else {
            return false;
        }
    }


    /**
    * Sets the given state to all the units that si beeing carried.
    * @param state The state.
    */
    public void setStateToAllChildren(int state) {
        if (isCarrier()) {
            Iterator i = getUnitIterator();
            while (i.hasNext()) {
                ((Unit) i.next()).setState(state);
            }
        }
    }
    
    /**
     * Set movesLeft to 0 if has some spent moves and it's in a colony
     * @see #add(Locatable)
     * @see #remove(Locatable)
     */
    private void spendAllMoves() {
        if (getTile() != null && getTile().getColony() != null &&
                getMovesLeft() < getInitialMovesLeft())
            setMovesLeft(0);
    }


    /**
    * Adds a locatable to this <code>Unit</code>.
    * @param locatable The <code>Locatable</code> to add to this <code>Unit</code>.
    */
    public void add(Locatable locatable) {
        if (isCarrier()) {
            if (locatable instanceof Unit) {
                if (getSpaceLeft() <= 0 || getType() == WAGON_TRAIN) {
                    throw new IllegalStateException();
                }

                unitContainer.addUnit((Unit) locatable);
                spendAllMoves();
            } else if (locatable instanceof Goods) {
                goodsContainer.addGoods((Goods) locatable);
                if (getSpaceLeft() < 0) {
                    throw new IllegalStateException("Not enough space for the given locatable!");
                }
                spendAllMoves();
            } else {
                logger.warning("Tried to add an unrecognized 'Locatable' to a unit.");
            }
        } else {
            logger.warning("Tried to add a 'Locatable' to a non-carrier unit.");
        }
    }


    /**
    * Removes a <code>Locatable</code> from this <code>Unit</code>.
    * @param locatable The <code>Locatable</code> to remove from this <code>Unit</code>.
    */
    public void remove(Locatable locatable) {
        if (isCarrier()) {
            if (locatable instanceof Unit) {
                unitContainer.removeUnit((Unit) locatable);
                spendAllMoves();
            } else if (locatable instanceof Goods) {
                goodsContainer.removeGoods((Goods) locatable);
                spendAllMoves();
            } else {
                logger.warning("Tried to remove an unrecognized 'Locatable' from a unit.");
            }
        } else {
            logger.warning("Tried to remove a 'Locatable' from a non-carrier unit.");
        }
    }


    /**
    * Checks if this <code>Unit</code> contains the specified
    * <code>Locatable</code>.
    *
    * @param locatable The <code>Locatable</code> to test the
    *        presence of.
    * @return <ul>
    *           <li><i>true</i>  if the specified <code>Locatable</code>
    *                            is on this <code>Unit</code> and
    *           <li><i>false</i> otherwise.
    *         </ul>
    */
    public boolean contains(Locatable locatable) {
        if (isCarrier()) {
            if (locatable instanceof Unit) {
                return unitContainer.contains((Unit) locatable);
            } else if (locatable instanceof Goods) {
                return goodsContainer.contains((Goods) locatable);
            } else {
                return false;
            }
        } else {
            logger.warning("Tried to remove a 'Locatable' from a non-carrier unit.");
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
        if (isCarrier()) {
            if ((getType() == WAGON_TRAIN || getType() == BRAVE) && locatable instanceof Unit) {
                return false;
            }

            if (locatable instanceof Unit) {
                return getSpaceLeft() >= locatable.getTakeSpace();
            } else if (locatable instanceof Goods) {
                Goods g = (Goods) locatable;
                return getSpaceLeft() > 0 || 
                       (getGoodsContainer().getGoodsCount(g.getType()) % 100 != 0
                        && getGoodsContainer().getGoodsCount(g.getType()) % 100 + g.getAmount() <= 100);
            } else { // Is there another class that implements Locatable ??
                logger.warning("Oops, not implemented...");
                return false;
            }
        } else {
            return false;
        }
    }


    /**
    * Gets the amount of Units at this Location.
    * @return The amount of Units at this Location.
    */
    public int getUnitCount() {

        return isCarrier() ? unitContainer.getUnitCount() : 0;
    }


    /**
    * Gets the first <code>Unit</code> beeing carried by this <code>Unit</code>.
    * @return The <code>Unit</code>.
    */
    public Unit getFirstUnit() {

        return isCarrier() ? unitContainer.getFirstUnit() : null;
    }


    /**
    * Checks if this unit is visible to the given player.
    * @param player The <code>Player</code>.
    * @return <code>true</code> if this <code>Unit</code> is
    *       visible to the given <code>Player</code>.
    */
    public boolean isVisibleTo(Player player) {
        if (player == getOwner()) {
            return true;
        }
        return getTile() != null && player.canSee(getTile()) 
                    && (getTile().getSettlement() == null 
                            || getTile().getSettlement().getOwner() == player
                            || (!getGameOptions().getBoolean(GameOptions.UNIT_HIDING)
                                && getLocation() instanceof Tile))
                    && (!(getLocation() instanceof Unit)
                            || ((Unit) getLocation()).getOwner() == player
                            || !getGameOptions().getBoolean(GameOptions.UNIT_HIDING));
    }


    /**
    * Gets the last <code>Unit</code> beeing carried by this <code>Unit</code>.
    * @return The <code>Unit</code>.
    */
    public Unit getLastUnit() {

        return isCarrier() ? unitContainer.getLastUnit() : null;
    }


    /**
    * Gets a <code>Iterator</code> of every <code>Unit</code> directly located on this
    * <code>Location</code>.
    *
    * @return The <code>Iterator</code>.
    */
    public Iterator getUnitIterator() {

        if ( isCarrier()  &&  getType() != WAGON_TRAIN ) {

            return unitContainer.getUnitIterator();
        }

        return EmptyIterator.SHARED_INSTANCE;
    }


    /**
    * Gets a <code>Iterator</code> of every <code>Unit</code> directly located on this
    * <code>Location</code>.
    *
    * @return The <code>Iterator</code>.
    */
    public Iterator getGoodsIterator() {
        if (isCarrier()) {
            return goodsContainer.getGoodsIterator();
        } else { // TODO: Make a better solution:
            return (new ArrayList()).iterator();
        }
    }


    public GoodsContainer getGoodsContainer() {
        return goodsContainer;
    }


    public UnitContainer getUnitContainer() {
        return unitContainer;
    }


    /**
    * Sets this <code>Unit</code> to work in the
    * specified <code>WorkLocation</code>.
    *
    * @param workLocation The place where this <code>Unit</code> shall
    *                     be out to work.
    * @exception IllegalStateException If the <code>workLocation</code> is
    *            on another {@link Tile} than this <code>Unit</code>.
    */
    public void work(WorkLocation workLocation) {
        if (workLocation.getTile() != getTile()) {
            throw new IllegalStateException("Can only set a 'Unit'  to a 'WorkLocation' that is on the same 'Tile'.");
        }

        if (armed) setArmed(false);
        if (mounted) setMounted(false);
        if (isPioneer()) setNumberOfTools(0);
        
        setState(Unit.IN_COLONY);

        setLocation(workLocation);
    }


    /**
    * Sets the location of this Unit.
    * @param newLocation The new Location of the Unit.
    */
    public void setLocation(Location newLocation) {
        if (location != null) {
            location.remove(this);
        }

        location = newLocation;

        if (location != null) {
            location.add(this);
        }

        // Check for adjacent units owned by a player that our owner has not met before:
        if (getGame().getMap() != null 
                && location != null
                && location instanceof Tile
                && !isNaval()) {
            Iterator tileIterator = getGame().getMap().getAdjacentIterator(getTile().getPosition());
            while (tileIterator.hasNext()) {
                Tile t = getGame().getMap().getTile((Position) tileIterator.next());

                if (t == null) {
                    continue;
                }

                if (getOwner() == null) {
                    logger.warning("owner == null");
                    throw new NullPointerException();
                }

                if (t.getSettlement() != null &&
                    !t.getSettlement().getOwner().hasContacted(getOwner().getNation())) {
                    t.getSettlement().getOwner().setContacted(getOwner(), true);
                    getOwner().setContacted(t.getSettlement().getOwner(), true);
                } else if (t.isLand()
                           && t.getFirstUnit() != null &&
                           !t.getFirstUnit().getOwner().hasContacted(getOwner().getNation())) {
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
    * @param indianSettlement The <code>IndianSettlement</code> that
    *       should now be owning this <code>Unit</code>.
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
    * @return The <code>IndianSettlement</code>.
    */
    public IndianSettlement getIndianSettlement() {
        return indianSettlement;
    }


    /**
    * Gets the location of this Unit.
    * @return The location of this Unit.
    */
    public Location getLocation() {
        return location;
    }


    /**
    * Puts this <code>Unit</code> outside the {@link Colony} by moving
    * it to the {@link Tile} below.
    */
    public void putOutsideColony() {
        if (getTile().getSettlement() == null) {
            throw new IllegalStateException();
        }
        
        if(getState() == Unit.IN_COLONY){
            setState(Unit.ACTIVE);
        }

        setLocation(getTile());
        setMovesLeft(0);

        if (getTile().getColony().getUnitCount() <= 0) {
            getTile().getColony().dispose();
        }
    }


    /**
     * Checks whether this unit can be equipped with goods at the
     * current location. This is the case if the unit is in a colony
     * in which the goods are present, or if it is in Europe and the
     * player can trade the goods and has enough gold to pay for them.
     *
     * @param type The type of goods.
     * @param amount The amount of goods.
     * @return whether this unit can be equipped with goods at the current location.
     */
    private boolean canBeEquipped(int type, int amount) {
        return ((getGoodsDumpLocation() != null &&
                 getGoodsDumpLocation().getGoodsCount(type) >= amount) ||
                ((location instanceof Europe ||
                  location instanceof Unit && ((Unit) location).getLocation() instanceof Europe) &&
                 getOwner().getGold() >= getGame().getMarket().getBidPrice(type, amount) &&
                 getOwner().canTrade(type)));
    }

    /**
    * Checks if this unit can be armed in the current location.
    * @return <code>true</code> if it can be armed at the current
    *       location.
    */
    public boolean canBeArmed() {
        return isArmed() || canBeEquipped(Goods.MUSKETS, 50);
    }


    /**
    * Checks if this unit can be mounted in the current location.
    * @return <code>true</code> if it can mount a horse at the current
    *       location.
    */
    public boolean canBeMounted() {
        return isMounted() || canBeEquipped(Goods.HORSES, 50);
    }


    /**
    * Checks if this unit can be equiped with tools in the current location.
    * @return <code>true</code> if it can be equipped with tools at the current
    *       location.
    */
    public boolean canBeEquippedWithTools() {
        return isPioneer() || canBeEquipped(Goods.TOOLS, 20);
    }


    /**
    * Checks if this unit can be dressed as a missionary at the current location.
    * @return <code>true</code> if it can be dressed as a missionary at the current
    *       location.
    */
    public boolean canBeDressedAsMissionary() {
        return isMissionary() || ((location instanceof Europe || location instanceof Unit && ((Unit) location).getLocation()
               instanceof Europe) || getTile() != null && getTile().getColony().getBuilding(Building.CHURCH).isBuilt());
    }


    /**
    * Sets the armed attribute of this unit.
    * @param b <i>true</i> if this unit should be armed and <i>false</i> otherwise.
    * @param isCombat Whether this is a result of combat. That is; do not pay
    *                 for the muskets.
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
                getGame().getMarket().buy(Goods.MUSKETS, 50, getOwner());
                armed = true;
            } else {
                logger.warning("Attempting to arm a soldier outside of a colony or Europe!");
            }
        } else if ((!b) && (armed)) {
            armed = false;

            if (getGoodsDumpLocation() != null) {
                getGoodsDumpLocation().addGoods(Goods.MUSKETS, 50);
            } else if (isInEurope()) {
                getGame().getMarket().sell(Goods.MUSKETS, 50, getOwner());
            } else {
                throw new IllegalStateException();
            }
        }
    }

    /**
    * Sets the armed attribute of this unit.
    * @param b <i>true</i> if this unit should be armed and <i>false</i> otherwise.
    */
    public void setArmed(boolean b) {
        setArmed(b, false);
    }

    /**
    * Checks if this <code>Unit</code> is currently armed.
    * @return <i>true</i> if this unit is armed and <i>false</i> otherwise.
    */
    public boolean isArmed() {
        return armed;
    }


    /**
    * Sets the mounted attribute of this unit.
    * @param b <i>true</i> if this unit should be mounted and <i>false</i> otherwise.
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
                getGame().getMarket().buy(Goods.HORSES, 50, getOwner());
                mounted = true;
            } else {
                logger.warning("Attempting to mount a colonist outside of a colony or Europe!");
            }
        } else if ((!b) && (mounted)) {
            mounted = false;

            if (getGoodsDumpLocation() != null) {
                getGoodsDumpLocation().addGoods(Goods.HORSES, 50);
            } else if (isInEurope()) {
                getGame().getMarket().sell(Goods.HORSES, 50, getOwner());
            }
        }
    }

    /**
    * Sets the mounted attribute of this unit.
    * @param b <i>true</i> if this unit should be mounted and <i>false</i> otherwise.
    */
    public void setMounted(boolean b) {
        setMounted(b, false);
    }


    /**
    * Checks if this <code>Unit</code> is currently mounted.
    * @return <i>true</i> if this unit is mounted and <i>false</i> otherwise.
    */
    public boolean isMounted() {
        return mounted;
    }

    
    /**
    * Checks if this <code>Unit</code> is located in Europe.
    * That is; either directly or onboard a carrier which is in Europe.
    * @return The result.
    */
    public boolean isInEurope() {
        return getTile() == null;
    }


    /**
    * Sets the unit to be a missionary.
    *
    * @param b <i>true</i> if the unit should be a missionary and <i>false</i> otherwise.
    */
    public void setMissionary(boolean b) {
        logger.finest(getID() + ": Entering method setMissionary with param " + b);
        setMovesLeft(0);

        if (b) {
            if (!isInEurope() && !getTile().getColony().getBuilding(Building.CHURCH).isBuilt()) {
                throw new IllegalStateException("Can only dress as a missionary when the unit is located in Europe or a Colony with a church.");
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
    * @return 'true' if this colonist is dressed as a missionary, 'false' otherwise.
    */
    public boolean isMissionary() {
        return missionary;
    }


    /**
    * Buys goods of a specified type and amount and adds it to this <code>Unit</code>.
    * Can only be used when the <code>Unit</code> is a carrier and is located in
    * {@link Europe}.
    *
    * @param type The type of goods to buy.
    * @param amount The amount of goods to buy.
    */
    public void buyGoods(int type, int amount) {
        if (!isCarrier() || !(getLocation() instanceof Europe && getState() != TO_EUROPE && getState() != TO_AMERICA)) {
            throw new IllegalStateException("Cannot buy goods when not a carrier or in Europe.");
        }

        try {
            getGame().getMarket().buy(type, amount, getOwner());
            goodsContainer.addGoods(type, amount);
        } catch(IllegalStateException ise) {
            this.addModelMessage(this, "notEnoughGold", null,
                                 ModelMessage.DEFAULT);
        }
    }


    /**
    * Sets how many tools this unit is carrying.
    * @param numberOfTools The number to set it to.
    */
    public void setNumberOfTools(int numberOfTools) {
        setMovesLeft(0);

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
        /*if (numberOfTools > 100) {
            logger.warning("Attempting to give a pioneer a number of greater than 100!");
        }*/
        if (numberOfTools > 100) {
            numberOfTools = 100;
        }

        if ((numberOfTools % 20) != 0) {
            //logger.warning("Attempting to give a pioneer a number of tools that is not a multiple of 20!");
            numberOfTools -= (numberOfTools % 20);
        }

        changeAmount = numberOfTools - this.numberOfTools;
        if (changeAmount > 0) {
            if (getGoodsDumpLocation() != null) {
               int actualAmount = getGoodsDumpLocation().getGoodsCount(Goods.TOOLS);
               if (actualAmount < changeAmount) changeAmount = actualAmount;
               if ((this.numberOfTools + changeAmount) % 20 > 0) changeAmount -= (this.numberOfTools + changeAmount)%20;
               if (changeAmount <= 0) return;
               getGoodsDumpLocation().removeGoods(Goods.TOOLS, changeAmount);
               this.numberOfTools = this.numberOfTools + changeAmount;
            } else if (isInEurope()) {
               int maximumAmount = ((getOwner().getGold()) / (getGame().getMarket().costToBuy(Goods.TOOLS)));
               if (maximumAmount < changeAmount) changeAmount = maximumAmount;
               if ((this.numberOfTools + changeAmount) % 20 > 0) changeAmount -= (this.numberOfTools + changeAmount)%20;
               if (changeAmount <= 0) return;
               getGame().getMarket().buy(Goods.TOOLS, changeAmount, getOwner());
               this.numberOfTools = this.numberOfTools + changeAmount;
            } else {
                logger.warning("Attempting to create a pioneer outside of a colony or Europe!");
            }
        } else if (changeAmount < 0) {
            this.numberOfTools = this.numberOfTools + changeAmount;

            if (getGoodsDumpLocation() != null) {
                getGoodsDumpLocation().addGoods(Goods.TOOLS, -changeAmount);
            } else if (isInEurope()) {
                getGame().getMarket().sell(Goods.TOOLS, -changeAmount, getOwner());
            }
        }
    }


    /**
    * Gets the number of tools this unit is carrying.
    * @return The number of tools.
    */
    public int getNumberOfTools() {
        return numberOfTools;
    }


    /**
    * Checks if this <code>Unit</code> is a pioneer.
    * @return <i>true</i> if it is a pioneer and <i>false</i> otherwise.
    */
    public boolean isPioneer() {

        return 0 < getNumberOfTools();
    }


    /**
    * Checks if this <code>Unit</code> is able to carry {@link Locatable}s.
    *
    * @return 'true' if this unit can carry other units, 'false'
    * otherwise.
    */
    public boolean isCarrier() {
        return isCarrier(getType());
    }


    /**
    * Checks if this <code>Unit</code> is able to carry {@link Locatable}s.
    *
    * @param type The type used when checking.
    * @return 'true' if the unit can carry other units, 'false'
    * otherwise.
    */
    public static boolean isCarrier(int type) {

        UnitType  unitType = FreeCol.specification.unitType( type );

        return unitType.hasAbility("naval") ||  unitType.hasAbility("carry-goods");
    }


    /**
    * Gets the owner of this Unit.
    * @return The owner of this Unit.
    */
    public Player getOwner() {
        return owner;
    }

    /**
    * Sets the owner of this Unit.
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
    * @return The type of the unit.
    */
    public int getType() {
        return type;
    }

    /**
    * Sets the type of the unit.
    * @param type The new type of the unit.
    */
    public void setType(int type) {
        this.type = type;
        unitType = FreeCol.specification.unitType(type);
    }

    /**
    * Checks if this unit is of a given type.
    *
    * @param type The type.
    * @return <code>true</code> if the unit was of the given type and <code>false</code> otherwise.
    */
    public boolean isType(int type) {
        return getType() == type;
    }


    /**
    * Returns the amount of moves this Unit has left.
    * @return The amount of moves this Unit has left. If the
    *         <code>unit.isUnderRepair()</code> then <code>0</code>
    *         is always returned.
    */
    public int getMovesLeft() {

        return ! isUnderRepair() ? movesLeft : 0;
    }


    /**
    * Skips this unit by setting the moves left to 0.
    */
    public void skip() {
        movesLeft = 0;
    }


    /**
    * Returns the name of a unit in a human readable format. The return
    * value can be used when communicating with the user.
    *
    * @return The given unit type as a String
    * @throws IllegalArgumentException
    */
    public String getName() {
        if (name != null) {
            return name;
        }
        String name = "";
        boolean addP = false;

        if (isPioneer() && getType() != HARDY_PIONEER) {
            name = Messages.message("model.unit.pioneer") + " (";
            addP = true;
        } else if (isArmed() && getType() != KINGS_REGULAR && getType() != COLONIAL_REGULAR && getType() != BRAVE && getType() != VETERAN_SOLDIER) {
            if (!isMounted()) {
                name = Messages.message("model.unit.soldier") + " (";
            } else {
                name = Messages.message("model.unit.dragoon") + " (";
            }
            addP = true;
        } else if (isMounted() && getType() != SEASONED_SCOUT && getType() != BRAVE) {
            name = Messages.message("model.unit.scout") +  " (";
            addP = true;
        } else if (isMissionary() && getType() != JESUIT_MISSIONARY) {
            name = Messages.message("model.unit.missionary") + " (";
            addP = true;
        }

        if (!isArmed() && !isMounted() && (getType() == KINGS_REGULAR || getType() == COLONIAL_REGULAR || getType() == VETERAN_SOLDIER)) {
            name = Messages.message("model.unit.unarmed") + " ";
        }

        if (getType() == BRAVE) {
            name = getOwner().getNationAsString() + " ";
            if (isArmed() && !isMounted()) {
                name += Messages.message("model.unit.armed") + " ";
            } else if (isMounted()) {
                name += Messages.message("model.unit.mounted") + " ";
            }            
        }

        name += getName(getType());

        if (isArmed() && isMounted()) {
            if (getType() == KINGS_REGULAR) {
                name = Messages.message("model.unit.kingsCavalry");
                addP = false;
            } else if (getType() == COLONIAL_REGULAR) {
                name = Messages.message("model.unit.colonialCavalry");
                addP = false;
            } else if (getType() == VETERAN_SOLDIER) {
                name = Messages.message("model.unit.veteranDragoon");
                addP = false;
            } else if (getType() == BRAVE) {
                name = getOwner().getNationAsString() + " " + Messages.message("model.unit.indianDragoon");
                addP = false;
            }
        }

        if (addP) {
            name += ")";
        }

        return name;
    }

    /**
     * Set the <code>Name</code> value.
     *
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
    */
    public static String getName(int someType) {

        return FreeCol.specification.unitType(someType).name;
    }


    /**
    * Returns the name of this unit in a human readable format. The return
    * value can be used when communicating with the user.
    *
    * @return The type of this Unit as a String
    * @throws FreeColException
    */
    /*public String getName() throws FreeColException {
        return Unit.getName(getType());
    }*/


    /**
    * Gets the amount of moves this unit has at the beginning of each turn.
    * @return The amount of moves this unit has at the beginning of each turn.
    */
    public int getInitialMovesLeft() {
        if (isNaval()) {
            int fMagellan = 0;
            if(owner.hasFather(FoundingFather.FERDINAND_MAGELLAN)) {
                fMagellan += 3;
            }

            switch (getType()) {
                case CARAVEL:
                    return 12+fMagellan;
                case FRIGATE:
                    return 18+fMagellan;
                case GALLEON:
                    return 18+fMagellan;
                case MAN_O_WAR:
                    return 18+fMagellan;
                case MERCHANTMAN:
                    return 15+fMagellan;
                case PRIVATEER:
                    return 24+fMagellan;
                case FLYING_DUTCHMAN:
                    return 36+fMagellan;
                default:
                    logger.warning("Unit.getInitialMovesLeft(): Unit has invalid naval type.");
                    return 9+fMagellan;
            }
        } else {
            if (getType() == REVENGER) {
                return 18;
            } else if (isMounted()) {
                return 12;
            } else if (isMissionary()) {
                return 6;
            } else if (getType() == WAGON_TRAIN) {
                return 6;
            } else {
                return 3;
            }
        }
    }


    /**
    * Gets the initial hitpoints for a given type of <code>Unit</code>.
    * For now this method only returns <code>5</code> that is used to
    * determine the number of rounds needed to repair a unit, but
    * later it can be used for indicating the health of a unit as well.
    *
    * <br><br>
    *
    * Larger values would indicate a longer repair-time.
    *
    * @param type The type of a <code>Unit</code>.
    * @return 6
    */
    public static int getInitialHitpoints(int type) {
        return 6;
    }


    /**
    * Sets the hitpoints for this unit.
    * 
    * @param hitpoints The hitpoints this unit has. This 
    *       is currently only used for damaged ships, but 
    *       might get an extended use later.
    * @see #getInitialHitpoints
    */
    public void setHitpoints(int hitpoints) {
        this.hitpoints = hitpoints;
        if (hitpoints >= getInitialHitpoints(getType()) && getState() == FORTIFIED) {
            setState(ACTIVE);
            addModelMessage(this, "model.unit.shipRepaired", 
                            new String [][] {{"%ship%", getName()},
                                             {"%repairLocation%", getLocation().getLocationName()}},
                            ModelMessage.DEFAULT, this);
        }
    }


    /**
    * Returns the hitpoints.
    * @return The hitpoints this unit has. This is currently only
    *       used for damaged ships, but might get an extended use
    *       later.
    * @see #getInitialHitpoints
    */
    public int getHitpoints() {
        return hitpoints;
    }


    /**
    * Checks if this unit is under repair.
    * @return <i>true</i> if under repair and <i>false</i> otherwise.
    */
    public boolean isUnderRepair() {
        return (hitpoints < getInitialHitpoints(getType()));
    }


    /**
    * Sends this <code>Unit</code> to the closest
    * <code>Location</code> it can get repaired.
    */
    public void sendToRepairLocation() {
        Location l = getOwner().getRepairLocation(this);
        setLocation(l);
        setState(ACTIVE);
        setMovesLeft(0);
    }


    /**
    * Gets the x-coordinate of this Unit (on the map).
    * @return The x-coordinate of this Unit (on the map).
    */
    public int getX() {

        return (getTile() != null) ? getTile().getX() : -1;
    }


    /**
    * Gets the y-coordinate of this Unit (on the map).
    * @return The y-coordinate of this Unit (on the map).
    */
    public int getY() {

        return (getTile() != null) ? getTile().getY() : -1;
    }


    /**
    * Returns a String representation of this Unit.
    * @return A String representation of this Unit.
    */
    public String toString() {
        return getName() + " " + getMovesAsString();
    }


    public String getMovesAsString() {
        String moves = "";
        if (getMovesLeft()%3 == 0 || getMovesLeft()/3 > 0) {
            moves += Integer.toString(getMovesLeft()/3);
        }

        if (getMovesLeft()%3 != 0) {
            if (getMovesLeft()/3 > 0) {
                moves += " ";
            }

            moves += "(" + Integer.toString(getMovesLeft() - (getMovesLeft()/3) * 3) + "/3) ";
        }

        moves += "/" + Integer.toString(getInitialMovesLeft()/3);
        return moves;
    }


    /**
    * Checks if this <code>Unit</code> is naval.
    * @return <i>true</i> if this Unit is a naval Unit and <i>false</i> otherwise.
    */
    public boolean isNaval() {

        return unitType.hasAbility( "naval" );
    }
    
    
    /**
    * Get occupation indicator
    * @return The occupation indicator string 
    */
    public String getOccupationIndicator(){
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
            case Unit.PLOW:
                occupationString = "P";
                break;
            case Unit.BUILD_ROAD:
                occupationString = "R";
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
    * @return The state of this <code>Unit</code>.
    */
    public int getState() {
        return state;
    }


    /**
    * Sets a new state for this Unit.
    * @param s The new state for this Unit. Should be one of
    * {ACTIVE, FORTIFIED, ...}.
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
            case BUILD_ROAD:
            case PLOW:
                switch(getTile().getType()) {
                    case Tile.DESERT:
                    case Tile.PLAINS:
                    case Tile.PRAIRIE:
                    case Tile.GRASSLANDS:
                    case Tile.SAVANNAH: workLeft = ((getTile().isForested()) ? 4 : 3); break;
                    case Tile.MARSH: workLeft = ((getTile().isForested()) ? 6 : 5); break;
                    case Tile.SWAMP: workLeft = 7; break;
                    case Tile.ARCTIC:
                    case Tile.TUNDRA: workLeft = 4; break;
                    default: workLeft = -1; break;
                }
                getTile().takeOwnership(getOwner());
                if (s == BUILD_ROAD) workLeft /= 2;
                movesLeft = 0;
            case TO_EUROPE:
                if (state == ACTIVE && !(location instanceof Europe)) {
                    workLeft = 3;
                } else if ((state == TO_AMERICA) && (location instanceof Europe)) {
                    // I think '4' was also used in the original game.
                    workLeft = 4 - workLeft;
                }
                if (getOwner().hasFather(FoundingFather.FERDINAND_MAGELLAN)) {
                    workLeft--;
                }
                break;
            case TO_AMERICA:
                if ((state == ACTIVE) && (location instanceof Europe)) {
                    workLeft = 3;
                } else if ((state == TO_EUROPE) && (location instanceof Europe)) {
                    // I think '4' was also used in the original game.
                    workLeft = 4 - workLeft;
                }
                if (getOwner().hasFather(FoundingFather.FERDINAND_MAGELLAN)) {
                    workLeft--;
                }
                break;
            default:
                workLeft = -1;
        }
        state = s;
    }

    /**
     * Checks if this <code>Unit</code> can be moved to Europe.
     * @return <code>true</code> if this unit is adjacent to
     *      a <code>Tile</code> of type {@link Tile#HIGH_SEAS}.
     */
    public boolean canMoveToEurope() {
        if (getLocation() instanceof Europe) {
            return true;
        }
        if (!getOwner().canMoveToEurope()) {
            return false;
        }        
        
        Vector surroundingTiles = getGame().getMap().getSurroundingTiles(getTile(), 1);
        if (surroundingTiles.size() != 8) {
            return true;
        } else {
            for (int i=0; i<surroundingTiles.size(); i++) {
                Tile tile = (Tile) surroundingTiles.get(i);
                if (tile == null || tile.getType() == Tile.HIGH_SEAS) {
                    return true;
                }
            }
        }        
        return false;
    }

    /**
    * Moves this unit to europe.
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

        setState(TO_EUROPE);
        setLocation(getOwner().getEurope());
    }


    /**
    * Moves this unit to america.
    * @exception IllegalStateException If the move is illegal.
    */
    public void moveToAmerica() {
        if (!(getLocation() instanceof Europe)) {
            throw new IllegalStateException("A unit can only be moved to america from europe.");
        }

        setState(TO_AMERICA);
    }


    /**
    * Checks wether this unit can plow the <code>Tile</code>
    * it is currently located on or not.
    * @return The result.
    */
    public boolean canPlow() {
        return getTile().canBePlowed() && getNumberOfTools() >= 20;
    }

    /**
    * Checks if a <code>Unit</code> can get the given state set.
    *
    * @param s The new state for this Unit. Should be one of
    * {ACTIVE, FORTIFIED, ...}.
    * @return 'true' if the Unit's state can be changed to the
    * new value, 'false' otherwise.
    */
    public boolean checkSetState(int s) {
        if (movesLeft <= 0 && (s == PLOW || s == BUILD_ROAD || s == FORTIFYING)) {
            return false;
        }
        switch (s) {
            case ACTIVE:
                return true;
            case PLOW:
                return canPlow();
            case BUILD_ROAD:
                if (getTile().hasRoad()) {
                    return false;
                }
                return (getNumberOfTools() >= 20);
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
    * @return <code>true</code> if this unit can build a colony on the tile where
    *         it is located and <code>false</code> otherwise.
    */
    public boolean canBuildColony() {
        return getOwner().canBuildColonies() 
                && isColonist() 
                && getMovesLeft() > 0
                && getTile() != null 
                && getTile().isColonizeable();                
    }


    /**
    * Makes this unit build the specified colony.
    * @param colony The colony this unit shall build.
    */
    public void buildColony(Colony colony) {
        if (!canBuildColony()) {
            throw new IllegalStateException();
        }

        getTile().setNationOwner(owner.getNation());
        getTile().setSettlement(colony);
        setLocation(colony);

        if (isArmed()) setArmed(false);
        if (isMounted()) setMounted(false);
        if (isPioneer()) setNumberOfTools(0);
    }


    /**
    * Returns the Tile where this Unit is located. Or null if
    * its location is Europe.
    *
    * @return The Tile where this Unit is located. Or null if
    * its location is Europe.
    */
    public Tile getTile() {

        return (location != null) ? location.getTile() : null;
    }


    /**
    * Returns the amount of space left on this Unit.
    * @return The amount of units/goods than can be moved onto this Unit.
    */
    public int getSpaceLeft() {
        int space = getInitialSpaceLeft() - getGoodsCount();
        
        Iterator unitIterator = getUnitIterator();
        while (unitIterator.hasNext()) {
            Unit u = (Unit) unitIterator.next();
            space -= u.getTakeSpace();
        }
        
        return space;
    }

    /**
     * Returns the amount of goods that is carried by this unit.
     *  
     * @return The amount of goods carried by this <code>Unit</code>.
     *      This value might different from the one returned by
     *      {@link #getGoodsCount()} when the model is
     *      {@link Game#getViewOwner() owned by a client} and cargo
     *      hiding has been enabled.
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
    * @return The amount of units/cargo that this unit can carry.
    */
    public int getInitialSpaceLeft() {
        switch (type) {
            case CARAVEL:
                return 2;
            case FRIGATE:
                // I've got an official reference sheet from Colonization (came with the game)
                // that says that the cargo space for a frigate is 2 but I'm almost 100% sure
                // that it was 4 in the Colonization version that I used to play.
                return 4;
            case GALLEON:
                return 6;
            case MAN_O_WAR:
                return 6;
            case MERCHANTMAN:
                return 4;
            case PRIVATEER:
                return 2;
            case WAGON_TRAIN:
                return 2;
            case BRAVE:
                return 1;
            case FLYING_DUTCHMAN:
                return 6;
            default:
                return 0;
        }
    }


    /**
    * Move the given unit to the front of this carrier (make sure
    * it'll be the first unit in this unit's unit list).
    *
    * @param u The unit to move to the front.
    */
    public void moveToFront(Unit u) {
            //TODO: Implement this feature.
        /*if (isCarrier() && removeUnit(u)) {
            units.add(0, u);
        }*/
    }


    /**
    * Get the number of turns of work left.
    * @return Work left
    */
    public int getWorkLeft() {
        return workLeft;
    }


    /**
    * This method does all the work.
    */
    public void doAssignedWork() {
        logger.finest("Entering method doAssignedWork.");
        if (workLeft > 0) {
            workLeft--;
            
            // Shorter travel time to America for the REF:
            if (state == TO_AMERICA && getOwner().isREF()) {
                workLeft = 0;
            }
            
            if (workLeft == 0) {
                workLeft = -1;

                int state = getState();

                switch (state) {
                    case TO_EUROPE:
                        addModelMessage(getOwner().getEurope(), "model.unit.arriveInEurope", null,
                                        ModelMessage.DEFAULT);
                        if (getType() == GALLEON) {
                            Iterator iter = getUnitIterator();
                            Unit u = null;
                            while (iter.hasNext() && (u = (Unit) iter.next()) != null && u.getType() != TREASURE_TRAIN);
                            if (u != null && u.getType() == TREASURE_TRAIN) {
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
                    case BUILD_ROAD:
                        getTile().setRoad(true);
                        expendTools(20);
                        setState(ACTIVE);
                        break;
                    case PLOW:
                        if (getTile().isForested()) {
                            // Give Lumber to adjacent colony
                            // Yes, the amount of lumber may exceed 100 units,
                            // but this was also true for the original game, IIRC.
                            int lumberAmount = getTile().potential(Goods.LUMBER) * 15 + 10;
                            if (getTile().getColony() != null && getTile().getColony().getOwner().equals(getOwner())) {
                                getTile().getColony().addGoods(Goods.LUMBER, lumberAmount);
                            } else {
                                Vector surroundingTiles = getTile().getMap().getSurroundingTiles(getTile(), 1);
                                Vector adjacentColonies = new Vector();
                                for (int i=0; i<surroundingTiles.size(); i++) {
                                    Tile t = (Tile) surroundingTiles.get(i);
                                    if (t.getColony() != null && t.getColony().getOwner().equals(getOwner())) {
                                        adjacentColonies.add(t.getColony());
                                    }
                                }
                                if (adjacentColonies.size() > 0) {
                                    int lumberPerCity = (lumberAmount / adjacentColonies.size());
                                    for (int i=0; i<adjacentColonies.size(); i++) {
                                        Colony c = (Colony) adjacentColonies.get(i);
                                        // Make sure the lumber lost is being added again to the first adjacent colony:
                                        if (i==0) {
                                            c.addGoods(Goods.LUMBER, lumberPerCity + (lumberAmount % adjacentColonies.size()));
                                        } else {
                                            c.addGoods(Goods.LUMBER, lumberPerCity);
                                        }
                                    }
                                }
                            }
                            getTile().setForested(false);
                            // TODO: check map generator options
                            logger.finest("Checking for bonus on cleared land");
                            if (getGame().getModelController().getRandom(getID() + "doAssignedWork:plow", 10) == 0) {
                                logger.finest("Received bonus on cleared land");
                                getTile().setBonus(true);
                            }
                        } else {
                            getTile().setPlowed(true);
                        }
                        expendTools(20);
                        setState(ACTIVE);
                        break;
                    default:
                        logger.warning("Unknown work completed. State: " + state);
                        setState(ACTIVE);
                }
            }
        }
    }

    /**
     * Reduces the number of tools and produces a warning if all tools
     * are used up.
     *
     * @param amount The number of tools to remove.
     */
    private void expendTools(int amount) {
        numberOfTools -= amount;
        if (numberOfTools == 0) {
            if (getType() == HARDY_PIONEER) {
                addModelMessage(this, "model.unit.noMoreToolsPioneer", null,
                                ModelMessage.WARNING, new Goods(Goods.TOOLS));
            } else {
                addModelMessage(this, "model.unit.noMoreTools",
                                new String [][] {{"%name%", getName()}},
                                ModelMessage.WARNING, new Goods(Goods.TOOLS));
            }
        }
    }


    /**
    * Sets the <code>Location</code> in which this unit will be put
    * when returning from {@link Europe}.
    *
    * @param entryLocation The <code>Location</code>.
    * @see #getEntryLocation
    */
    public void setEntryLocation(Location entryLocation) {
        this.entryLocation = entryLocation;
    }


    /**
    * Gets the <code>Location</code> in which this unit will be put
    * when returning from {@link Europe}. If this <code>Unit</code>
    * has not not been outside europe before, it will return the default
    * value from the {@link Player} owning this <code>Unit</code>.
    *
    * @return The <code>Location</code>.
    * @see Player#getEntryLocation
    * @see #getVacantEntryLocation
    */
    public Location getEntryLocation() {

        return (entryLocation != null) ? entryLocation : getOwner().getEntryLocation();
    }


    /**
    * Gets the <code>Location</code> in which this unit will be put
    * when returning from {@link Europe}. If this <code>Unit</code>
    * has not not been outside europe before, it will return the default
    * value from the {@link Player} owning this <code>Unit</code>.
    * If the tile is occupied by a enemy unit, then a nearby tile is choosen.
    *
    * <br><br>
    * <i>WARNING:</i> Only the server has the information to determine which
    *                 <code>Tile</code> is occupied. Use
    *                 {@link ModelController#setToVacantEntryLocation} instead.
    *
    * @return The <code>Location</code>.
    * @see #getEntryLocation
    */
    public Location getVacantEntryLocation() {
        Tile l = (Tile) getEntryLocation();

        if (l.getFirstUnit() != null && l.getFirstUnit().getOwner() != getOwner()) {
            int radius = 1;
            while (true) {
                Iterator i = getGame().getMap().getCircleIterator(l.getPosition(), false, radius);
                while (i.hasNext()) {
                    Tile l2 = getGame().getMap().getTile((Position) i.next());
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
    * Checks if the given unit can be recruited in <code>Europe</code>.
    * @param type The type of <code>Unit</code> to be tested.
    * @return <code>true</code> if the given type is the type of a 
    *       recruitable unit and <code>false</code> otherwise.
    */
    public static boolean isRecruitable(int type) {

        return FreeCol.specification.unitType(type).hasAbility( "recruitable" );
    }


    /**
    * Returns the price of this unit in Europe.
    *
    * @return The price of this unit when trained in Europe. '-1' is returned in case the unit cannot be bought.
    */
    public int getPrice() {

        if ( ARTILLERY == type ) {
            return getOwner().getEurope().getArtilleryPrice();
        }

        return getPrice(getType());
    }


    /**
    * Returns the price of a trained unit in Europe.
    *
    * @param type The type of unit of which you need the price.
    * @return The price of a trained unit in Europe. '-1' is returned in case the unit cannot be bought.
    */
    public static int getPrice(int type) {

        if ( ARTILLERY == type ) {

            throw new IllegalStateException();
        }

        UnitType  unitType = FreeCol.specification.unitType( type );

        if ( unitType.hasPrice() ) {

            return unitType.price;
        }

        return -1;
    }


    /**
    * Returns the current defensive power of this unit. The tile on which this
    * unit is located will be taken into account.
    * @param attacker The attacker of this unit.
    * @return The current defensive power of this unit.
    */
    public int getDefensePower(Unit attacker) {

        int base_power = unitType.defence;

        if (getOwner().hasFather(FoundingFather.PAUL_REVERE) && getTile() != null && getTile().getColony() != null) {
            if (isColonist() && base_power == 1 && (getLocation() instanceof ColonyTile || getLocation() instanceof Building)) {
                base_power = 2;
            }
        }

        if (isArmed()) {
            base_power++;
        }
        if (isMounted()) {
            if (!isArmed() && getType() != BRAVE) {
                base_power = 1;
            } else {
                base_power++;
            }
        }

        int modified_power = base_power;

        //TODO: <1 move point movement penalty

        if (isNaval()) {
            if (getGoodsCount() > 0) {
                modified_power -= ((base_power * getGoodsCount()) / 8); // -12.5% penalty for every unit of cargo.
            }
            if (getType() == PRIVATEER && getOwner().hasFather(FoundingFather.FRANCIS_DRAKE)) {
                modified_power += base_power / 2; // 50% bonus
            }
            return modified_power;
        }

        if (getState() == FORTIFIED) {
            modified_power += (base_power / 2); // 50% fortify bonus
        }

        if ((getTile() != null) && (getTile().getSettlement() != null) && (getTile().getSettlement() instanceof Colony) ) {
            Colony colony = ((Colony)getTile().getSettlement());
            switch(colony.getBuilding(Building.STOCKADE).getLevel()) {
                case Building.NOT_BUILT:
                default:
                    modified_power += (base_power / 2); // 50% colony bonus
                     break;
                case Building.HOUSE:
                    modified_power += base_power; // 100% stockade bonus
                    break;
                case Building.SHOP:
                    modified_power += ((base_power * 3) / 2); // 150% fort bonus
                    break;
                case Building.FACTORY:
                    modified_power += (base_power * 2); // 200% fortress bonus
                    break;
            }
        } else if (!(((attacker.getType() != BRAVE) && (getType() == KINGS_REGULAR)) ||  // TODO: check for REF artillery pieces
            ((attacker.getType() == BRAVE) && (getType() != KINGS_REGULAR))) &&
            (getTile() != null)) {
            // Terrain defensive bonus.
            modified_power += ((base_power * getTile().defenseBonus()) / 100);
        }

        // Indian settlement defensive bonus.
        if (getTile() != null && getTile().getSettlement() != null && getTile().getSettlement() instanceof IndianSettlement) {
            modified_power += (base_power / 2); // 50% bonus
        }

        if ((getType() == ARTILLERY) || (getType() == DAMAGED_ARTILLERY)) {
            if ((attacker.getType() == BRAVE) && (getTile().getSettlement() != null)) {
                modified_power += base_power; // 100% defense bonus against an Indian raid
            }
            if (((getTile().getSettlement()) == null) && (getState() != FORTIFIED)) {
                modified_power -= ((base_power * 3) / 4); // -75% Artillery in the Open penalty
            }
        }

        return modified_power;
    }


    /**
    * Checks if this is an offensive unit.
    * 
    * @return <code>true</code> if this is an offensive unit
    *       meaning it can attack other units.
    */
    public boolean isOffensiveUnit() {
        // TODO: Make this look prettier ;-)
        return (getOffensePower(this) > 0);
    }

    
    /**
     * Checks if this is an defensive unit.
     * That is: a unit which can be used to defend a <code>Settlement</code>.
     * @return <code>true</code> if this is a defensive unit
     *       meaning it can be used to defend a <code>Colony</code>.
     *       This would normally mean that a defensive unit also will
     *       be {@link #isOffensiveUnit offensive}.
     */
     public boolean isDefensiveUnit() {
         return isOffensiveUnit() && !isNaval();
     }
     

    /**
    * Returns the current offensive power of this unit.
    * @param target The target of the attack.
    * @return The current offensive power of this unit.
    */
    public int getOffensePower(Unit target) {

        int  base_power = unitType.offence;

        if (isArmed()) {
            if (base_power == 0) {
                base_power = 2;
            } else {
                base_power++;
            }
        }
        if (isMounted()) {
            if ((!isArmed()) && (getType() != BRAVE)) {
                base_power = 1;
            } else {
                base_power++;
            }
        }

        int modified_power = (base_power * 3) / 2; // 50% attack bonus

        //TODO: <1 move point movement penalty

        if (isNaval()) {
            if (getGoodsCount() > 0) {
                modified_power -= ((base_power * getGoodsCount()) / 8); // -12.5% penalty for every unit of cargo.
            }
            if (getType() == PRIVATEER && getOwner().hasFather(FoundingFather.FRANCIS_DRAKE)) {
                modified_power += base_power / 2; // 50% bonus
            }
            return modified_power;
        }

        if ((((getType() != BRAVE) && (target.getType() == KINGS_REGULAR)) ||  // TODO: check for REF artillery pieces
            ((getType() == BRAVE) && (target.getType() != KINGS_REGULAR))) &&
            (target.getTile() != null) &&
            (target.getTile().getSettlement() == null)) {
            // Ambush bonus.
            modified_power += ((base_power * target.getTile().defenseBonus()) / 100);
        }

        if (((getType() == KINGS_REGULAR)) && // TODO: check for REF artillery pieces
            (target.getTile() != null) &&
            (target.getTile().getSettlement() == null))
        {
            modified_power += (base_power / 2); // REF bombardment bonus
        }

        if ((getType() == ARTILLERY) || (getType() == DAMAGED_ARTILLERY)) {
          if ((target.getTile() != null) && (target.getTile().getSettlement()) == null) {
            modified_power -= ((base_power * 3) / 4); // -75% Artillery in the Open penalty
          }
        }
        return modified_power;
    }


    /**
     * Attack a unit with the given outcome.
     *
     * @param defender The <code>Unit</code> defending against attack.
     * @param result The result of the attack.
     * @param plunderGold The amount of gold to plunder in case of
     *        a successful attack on a <code>Settlement</code>.
     */
    public void attack(Unit defender, int result, int plunderGold) {
        if (defender == null) {
            throw new NullPointerException();
        }
        if (getOwner().getStance(defender.getOwner()) == Player.ALLIANCE) {
            throw new IllegalStateException("Cannot attack allied players.");
        }

        // make sure we are at war, unless one of both units is a privateer
        if (getOwner().isEuropean() && defender.getOwner().isEuropean() &&
            getType() != PRIVATEER && defender.getType() != PRIVATEER) {
            getOwner().setStance(defender.getOwner(), Player.WAR);
        }
        if (getType() == PRIVATEER) {
            defender.getOwner().setAttackedByPrivateers();
        }

        // Wake up if you're attacking something.
        // Before, a unit could stay fortified during execution of an
        // attack. - sjm
        state = ACTIVE;
        if (getType() != REVENGER) {
            movesLeft = 0;
        }

        Tile newTile = defender.getTile();
        adjustTension(defender);

        switch (result) {
        case ATTACK_EVADES:
            if (isNaval()) {
		// send message to both parties
                addModelMessage(this, "model.unit.shipEvaded",
                                new String [][] {{"%ship%", defender.getName()},
                                                 {"%nation%", defender.getOwner().getNationAsString()}},
                                ModelMessage.DEFAULT, this);
                addModelMessage(defender, "model.unit.shipEvaded",
                                new String [][] {{"%ship%", defender.getName()},
                                                 {"%nation%", defender.getOwner().getNationAsString()}},
                                ModelMessage.DEFAULT, this);
            } else {
                logger.warning("Non-naval unit evades!");
            }
            break;
        case ATTACK_LOSS:
            if (isNaval()) {
                shipDamaged();
            } else {
                demote(defender, false);
                if (defender.getOwner().hasFather(FoundingFather.GEORGE_WASHINGTON)) {
                    defender.promote();
                }
            }
            break;
        case ATTACK_GREAT_LOSS:
            if (isNaval()) {
                shipSunk();
            } else {
                demote(defender, true);
                defender.promote();
            }
            break;
        case ATTACK_DONE_SETTLEMENT:
            Settlement settlement = newTile.getSettlement();
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
                                new String [][] {{"%ship%", defender.getName()},
                                                 {"%nation%", defender.getOwner().getNationAsString()}},
                                ModelMessage.UNIT_DEMOTED);
            } else {
                if (getOwner().hasFather(FoundingFather.GEORGE_WASHINGTON)) {
                    promote();
                }
                defender.demote(this, false);
            }
            break;
        case ATTACK_GREAT_WIN:
            if (isNaval()) {
                captureGoods(defender);
                defender.shipSunk();
                addModelMessage(this, "model.unit.shipSunk",
                                new String [][] {{"%ship%", defender.getName()},
                                                 {"%nation%", defender.getOwner().getNationAsString()}},
                                ModelMessage.UNIT_DEMOTED);
            } else {
                promote();
                defender.demote(this, true);
            }
            break;
        default:
            logger.warning("Illegal result of attack!");
            throw new IllegalArgumentException("Illegal result of attack!");
        }
    }

    /**
     * Checks if this unit is an undead.
     */
    public boolean isUndead() {
        return (getType() == Unit.REVENGER 
               || getType() == Unit.FLYING_DUTCHMAN 
               || getType() == Unit.UNDEAD);    
    }
    
    /**
     * Sets the damage to this ship and sends it to its repair
     * location.
     */
    public void shipDamaged() {
        String nation = owner.getNationAsString();
        Location repairLocation = getOwner().getRepairLocation(this);
        String repairLocationName = Messages.message("menuBar.view.europe");
        if (repairLocation instanceof Colony) {
            repairLocationName = ((Colony) repairLocation).getName();
        }
        addModelMessage(this, "model.unit.shipDamaged",
                        new String [][] {{"%ship%", getName()},
                                         {"%repairLocation%", repairLocationName},
                                         {"%nation%", nation}},
                        ModelMessage.UNIT_DEMOTED);
        setHitpoints(1);
        getUnitContainer().disposeAllUnits();
        goodsContainer.removeAll();
        sendToRepairLocation();
    }

    /**
     * Sinks this ship.
     */
    private void shipSunk() {
        String nation = owner.getNationAsString();
        addModelMessage(this, "model.unit.shipSunk",
                        new String [][] {{"%ship%", getName()},
                                         {"%nation%", nation}},
                        ModelMessage.UNIT_LOST);
        dispose();
    }

    /**
     * Demotes this unit. A unit that can not be further demoted is
     * destroyed. The enemy may plunder horses and muskets.
     *
     * @param enemyUnit The unit we are fighting against.
     * @param greatDemote <code>true</code> indicates that
     *      muskets/horses should be taken by the <code>enemyUnit</code>.
     */
    public void demote(Unit enemyUnit, boolean greatDemote) {
        String oldName = getName();
        String messageID = "model.unit.unitDemoted";
        String nation = owner.getNationAsString();
        int type = ModelMessage.UNIT_DEMOTED;
        
        if (enemyUnit.isUndead()) {
            // this unit is captured, don't show old owner's messages to new owner
            Iterator i = getGame().getModelMessageIterator(getOwner());
            while (i.hasNext()) {
                ((ModelMessage) i.next()).setBeenDisplayed(true);
            }
            messageID = "model.unit.unitCaptured";
            type = ModelMessage.UNIT_LOST;
            setHitpoints(getInitialHitpoints(enemyUnit.getType()));
            setLocation(enemyUnit.getTile());
            setOwner(enemyUnit.getOwner());
            setType(UNDEAD);
        } else if (getType() == ARTILLERY) {
            messageID = "model.unit.artilleryDamaged";
            setType(DAMAGED_ARTILLERY);
        } else if (getType() == DAMAGED_ARTILLERY) {
            messageID = "model.unit.unitDestroyed";
            type = ModelMessage.UNIT_LOST;
            dispose();
        } else if (getType() == BRAVE ||
		   getType() == KINGS_REGULAR) {
            nation = "";
            messageID = "model.unit.unitSlaughtered";
            type = ModelMessage.UNIT_LOST;
            dispose();
        } else if (isMounted()) {
            if (isArmed()) {
                // dragoon
                setMounted(false, true);
                if (enemyUnit.getType() == BRAVE && greatDemote) {
                    addModelMessage(this, "model.unit.braveMounted",
                                    new String [][] {{"%nation%", enemyUnit.getOwner().getNationAsString()}},
                                    ModelMessage.FOREIGN_DIPLOMACY);
                    enemyUnit.setMounted(true, true);
                }
            } else {
                // scout
                messageID = "model.unit.unitSlaughtered";
                type = ModelMessage.UNIT_LOST;
                dispose();
            }
        } else if (isArmed()) {
            // soldier
            setArmed(false, true);
            if (enemyUnit.getType() == BRAVE && greatDemote) {
                addModelMessage(this, "model.unit.braveArmed",
                                new String [][] {{"%nation%", enemyUnit.getOwner().getNationAsString()}},
                                ModelMessage.FOREIGN_DIPLOMACY);
                enemyUnit.setArmed(true, true);
            }
        } else {
            // civilians, wagon trains and treasure trains
            if (enemyUnit.getOwner().isEuropean()) {
                // this unit is captured, don't show old owner's messages to new owner
                Iterator i = getGame().getModelMessageIterator(getOwner());
                while (i.hasNext()) {
                    ((ModelMessage) i.next()).setBeenDisplayed(true);
                }
                messageID = "model.unit.unitCaptured";
                type = ModelMessage.UNIT_LOST;
                setHitpoints(getInitialHitpoints(enemyUnit.getType()));
                setLocation(enemyUnit.getTile());
                setOwner(enemyUnit.getOwner());
            } else {
                messageID = "model.unit.unitSlaughtered";
                type = ModelMessage.UNIT_LOST;
                dispose();
            }
        }
        if (!isDisposed()
                && getMovesLeft() > getInitialMovesLeft()) {
            setMovesLeft(getInitialMovesLeft());
        }
        String newName = getName();
        addModelMessage(this, messageID,
                        new String [][] {{"%oldName%", oldName},
                                         {"%newName%", newName},
                                         {"%nation%", nation}},
                        type);
        
        if (getOwner() != enemyUnit.getOwner()) { 
            // this unit hasn't been captured by enemyUnit, show message to enemyUnit's owner
            addModelMessage(enemyUnit, messageID,
                            new String [][] {{"%oldName%", oldName},
                                             {"%newName%", newName},
                                             {"%nation%", nation}},
                            type);
        }
    }

    /**
     * Promotes this unit.
     */
    public void promote() {
        String oldName = getName();
        String nation = owner.getNationAsString();

        if (getType() == PETTY_CRIMINAL) {
            setType(INDENTURED_SERVANT);
        } else if (getType() == INDENTURED_SERVANT) {
            setType(FREE_COLONIST);
        } else if (getType() == FREE_COLONIST) {
            setType(VETERAN_SOLDIER);
        } else if (getType() == VETERAN_SOLDIER && getOwner().getRebellionState() >= Player.REBELLION_IN_WAR) {
            setType(COLONIAL_REGULAR);
        }

        String newName = getName();
        if (!newName.equals(oldName)) {
            addModelMessage(this, "model.unit.unitImproved",
                            new String[][] {{"%oldName%", oldName},
                                            {"%newName%", getName()},
                                            {"%nation%", nation}},
                            ModelMessage.UNIT_IMPROVED);
        }
    }

    /**
     * Adjusts the tension and alarm levels of the enemy unit's owner
     * according to the type of attack.
     *
     * @param enemyUnit The unit we are attacking.
     */
    public void adjustTension(Unit enemyUnit) {
        Player myPlayer = getOwner();
        Player enemy = enemyUnit.getOwner();
        myPlayer.modifyTension(enemy, -Tension.TENSION_ADD_MINOR);
        if (getIndianSettlement() != null) {
            getIndianSettlement().modifyAlarm(enemy, -Tension.TENSION_ADD_UNIT_DESTROYED/2);
        }

        // Increases the enemy's tension levels:
        if (enemy.isAI()) {
            Settlement settlement = enemyUnit.getTile().getSettlement();
            IndianSettlement homeTown = enemyUnit.getIndianSettlement();
            if (settlement != null) {
                // we are attacking a settlement
                if (settlement instanceof IndianSettlement &&
                    ((IndianSettlement) settlement).isCapital()) {
                    enemy.modifyTension(myPlayer, Tension.TENSION_ADD_MAJOR);
                } else {
                    enemy.modifyTension(myPlayer, Tension.TENSION_ADD_NORMAL);
                }
                if (homeTown != null) {
                    homeTown.modifyAlarm(myPlayer, Tension.TENSION_ADD_SETTLEMENT_ATTACKED);
                }
            } else {
                // we are attacking an enemy unit in the open
                enemy.modifyTension(myPlayer, Tension.TENSION_ADD_MINOR);
                if (homeTown != null) {
                    homeTown.modifyAlarm(myPlayer, Tension.TENSION_ADD_UNIT_DESTROYED);
                }
            }
        }

    }

    /**
     * Returns true if this unit is a ship that can capture enemy
     * goods. That is, a privateer, frigate or man-o-war.
     * @return <code>true</code> if this <code>Unit</code> is
     *      capable of capturing goods.
     */
    public boolean canCaptureGoods() {

        return unitType.hasAbility( "capture-goods" );
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
        Iterator iter = enemyUnit.getGoodsIterator();
        while(iter.hasNext() && getSpaceLeft() > 0) {
            // TODO: show CaptureGoodsDialog if there's not enough
            // room for everything.
            Goods g = ((Goods)iter.next());
                        
            // MESSY, but will mess up the iterator if we do this
            // besides, this gets cleared out later
            //enemy.getGoodsContainer().removeGoods(g);
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

        int randomTreasure = modelController.getRandom(getID() + "indianTreasureRandom" +
                                                       getID(), 11);
        Unit tTrain = modelController.createUnit(getID() + "indianTreasure" + getID(),
                                                 newTile, getOwner(), Unit.TREASURE_TRAIN);

        // Larger treasure if Hernan Cortes is present in the congress:
        int bonus = (getOwner().hasFather(FoundingFather.HERNAN_CORTES)) ? 2 : 1;

        // The number of Indian converts
        int converts = (4 - getOwner().getDifficulty());

        // Incan and Aztecs give more gold and converts
        if (enemy.getNation() == Player.INCA ||
            enemy.getNation() == Player.AZTEC) {
            tTrain.setTreasureAmount(randomTreasure * 500 * bonus + 10000);
            converts += 2;
        } else {
            tTrain.setTreasureAmount(randomTreasure * 50 * bonus + 300);
        }

        // capitals give more gold
        if (wasCapital) {
            tTrain.setTreasureAmount((tTrain.getTreasureAmount()*3)/2);
        }

        if (!getOwner().hasFather(FoundingFather.JUAN_DE_SEPULVEDA)) {
            converts = converts/2;
        }

        for (int i = 0; i < converts; i++) {
            Unit newUnit = modelController.createUnit(getID() + "indianConvert" + i,
                                                      newTile, getOwner(), Unit.INDIAN_CONVERT);
        }

        addModelMessage(this, "model.unit.indianTreasure",
                        new String[][] {{"%indian%", enemy.getNationAsString()},
                                        {"%amount%", Integer.toString(tTrain.getTreasureAmount())}},
                        ModelMessage.DEFAULT);
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
            addModelMessage(enemy, "model.unit.colonyCapturedBy",
                    new String[][] {{"%colony%", colony.getName()},
                                    {"%amount%", Integer.toString(plunderGold)},
                                    {"%player%", myPlayer.getNationAsString()}},
                    ModelMessage.DEFAULT);
            colony.damageAllShips();
            
            myPlayer.modifyGold(plunderGold);
            enemy.modifyGold(-plunderGold);

            colony.setOwner(myPlayer); // This also changes over all of the units...
            addModelMessage(colony, "model.unit.colonyCaptured",
                    new String[][] {{"%colony%", colony.getName()},
                                    {"%amount%", Integer.toString(plunderGold)}},
                    ModelMessage.DEFAULT);            
                        
            // Demote all soldiers and clear all orders:
            Iterator it = colony.getTile().getUnitIterator();
            while (it.hasNext()) {
                Unit u = (Unit) it.next();
                if (u.getType() == Unit.VETERAN_SOLDIER
                        || u.getType() == Unit.KINGS_REGULAR
                        || u.getType() == Unit.COLONIAL_REGULAR) {
                    u.setType(Unit.FREE_COLONIST);
                }
                u.setState(Unit.ACTIVE);
                if (isUndead()) {
                    u.setType(UNDEAD);
                }
            }
            
            if (isUndead()) {
                Iterator it2 = colony.getUnitIterator();
                while (it2.hasNext()) {
                    Unit u = (Unit) it2.next();
                    u.setType(UNDEAD);
                }
            }
            
            setLocation(colony.getTile());
        } else { // Indian:
            if (colony.getUnitCount() <= 1) {
                myPlayer.modifyGold(plunderGold);
                enemy.modifyGold(-plunderGold);
                addModelMessage(enemy, "model.unit.colonyBurning",
                                new String[][] {{"%colony%", colony.getName()},
                                                {"%amount%", Integer.toString(plunderGold)}},
                                ModelMessage.DEFAULT);
                colony.damageAllShips();
                colony.dispose();
            } else {
                Unit victim = colony.getRandomUnit();
                if (victim == null) {
                    return;
                }
                addModelMessage(victim, "model.unit.colonistSlaughtered",
                                new String[][] {{"%colony%", colony.getName()},
                                                {"%unit%", victim.getName()}},
                                ModelMessage.UNIT_LOST);
                victim.dispose();
            }
        }

    }


    /**
    * Gets the Colony this unit is in.
    * @return The Colony it's in, or null if it is not in a Colony
    */
    public Colony getColony() {
        return getTile().getColony();
    }


    /**
    * Clear the speciality of a <code>Unit</code>. That is, makes it a
    * <code>FREE_COLONIST</code>
    */
    public void clearSpeciality() {
        if (isColonist() && getType() != INDIAN_CONVERT && getType() != INDENTURED_SERVANT && getType() != PETTY_CRIMINAL) {
            setType(FREE_COLONIST);
        }
    }


    /**
    * Gets the Colony the goods of this unit would go to if it were to de-equip.
    * @return The Colony the goods would go to, or null if there is no appropriate Colony
    */
    public Colony getGoodsDumpLocation() {
        if ((location instanceof Colony)) {
            return ((Colony)location);
        } else if (location instanceof Building) {
            return (((Building)location).getColony());
        } else if (location instanceof ColonyTile) {
            return (((ColonyTile)location).getColony());
        } else if ((location instanceof Tile) && (((Tile)location).getSettlement() != null) && (((Tile)location).getSettlement() instanceof Colony)) {
            return (((Colony)(((Tile)location).getSettlement())));
        } else if (location instanceof Unit) {
            if ((((Unit)location).getLocation()) instanceof Colony) {
                return ((Colony)(((Unit)location).getLocation()));
            } else if (((((Unit)location).getLocation()) instanceof Tile) && (((Tile)(((Unit)location).getLocation())).getSettlement() != null) && (((Tile)(((Unit)location).getLocation())).getSettlement() instanceof Colony)) {
                return ((Colony)(((Tile)(((Unit)location).getLocation())).getSettlement()));
            }
        }
        return null;
    }


    /**
    * Given a type of goods to produce in a building, returns the unit's potential to do so.
    * @param goods The type of goods to be produced.
    * @return The potential amount of goods to be manufactured.
    */
    public int getProducedAmount(int goods) {
        int base = 0;

        base = getProductionUsing(getType(), goods, base);

        if (base == 0) {
            return 0;
        }

        //base += getTile().getColony().getProductionBonus();
        return Math.max(base, 1);
    }




    /**
    * Given a type of goods to produce in the field and a tile, returns the unit's potential to produce goods.
    * @param goods The type of goods to be produced.
    * @param tile The tile which is being worked.
    * @return The potential amount of goods to be farmed.
    */
    public int getFarmedPotential(int goods, Tile tile) {
        if (tile == null) {
            throw new NullPointerException();
        }

        int base = tile.potential(goods);
        base = getProductionUsing(getType(), goods, base, tile);

        if (getLocation() instanceof ColonyTile && !((ColonyTile) getLocation()).getWorkTile().isLand()
                && !((ColonyTile) getLocation()).getColony().getBuilding(Building.DOCK).isBuilt()) {
            base = 0;
        }

        if (base == 0) {
            return 0;
        }

        if (goods == Goods.FURS && getOwner().hasFather(FoundingFather.HENRY_HUDSON)) {
            base *= 2;
        }

        if (getTile() != null && getTile().getColony() != null) {
            base += getTile().getColony().getProductionBonus();
        }

        return Math.max(base, 1);
    }


    /**
    * Applies unit-type specific bonuses to a goods production in a
    * <code>Building</code>.
    *
    * @param unitType The {@link #getType type} of the unit.
    * @param goodsType The type of goods that is being produced.
    * @param base The production not including the unit-type specific bonuses.
    * @return The production.
    */
    public static int getProductionUsing(int unitType, int goodsType, int base) {
        if (Goods.isFarmedGoods(goodsType)) {
            throw new IllegalArgumentException("\"goodsType\" is not produced in buildings.");
        }

        switch (unitType) {
            case MASTER_DISTILLER:
                if (goodsType == Goods.RUM) {
                    base = 6;
                } else {
                    base = 3;
                }
                break;
            case MASTER_WEAVER:
                if (goodsType == Goods.CLOTH) {
                    base = 6;
                } else {
                    base = 3;
                }
                break;
            case MASTER_TOBACCONIST:
                if (goodsType == Goods.CIGARS) {
                    base = 6;
                } else {
                    base = 3;
                }
                break;
            case MASTER_FUR_TRADER:
                if (goodsType == Goods.COATS) {
                    base = 6;
                } else {
                    base = 3;
                }
                break;
            case MASTER_BLACKSMITH:
                if (goodsType == Goods.TOOLS) {
                    base = 6;
                } else {
                    base = 3;
                }
                break;
            case MASTER_GUNSMITH:
                if (goodsType == Goods.MUSKETS) {
                    base = 6;
                } else {
                    base = 3;
                }
                break;
            case ELDER_STATESMAN:
                if (goodsType == Goods.BELLS) {
                    base = 6;
                } else {
                    base = 3;
                }
                break;
            case FIREBRAND_PREACHER:
                if (goodsType == Goods.CROSSES) {
                    base = 6;
                } else {
                    base = 3;
                }
                break;
            case MASTER_CARPENTER:
                if (goodsType == Goods.HAMMERS) {
                    base = 6;
                } else {
                    base = 3;
                }
                break;
            case FREE_COLONIST:
                base = 3;
                break;
            case INDENTURED_SERVANT:
                base = 2;
                break;
            case PETTY_CRIMINAL:
            case INDIAN_CONVERT:
                base = 1;
                break;
            default: // Beats me who or what is working here, but he doesn't get a bonus.
                base = 3;
        }

        return base;
    }


    /**
    * Applies unit-type specific bonuses to a goods production on a
    * <code>Tile</code>.
    *
    * @param unitType The {@link #getType type} of the unit.
    * @param goodsType The type of goods that is being produced.
    * @param base The production not including the unit-type specific bonuses.
    * @param tile The <code>Tile</code> in which the given type of goods is being produced.
    * @return The production.
    */
    public static int getProductionUsing(int unitType, int goodsType, int base, Tile tile) {
        if (!Goods.isFarmedGoods(goodsType)) {
            throw new IllegalArgumentException("\"goodsType\" is produced in buildings and not on tiles.");
        }

        switch (unitType) {
            case EXPERT_FARMER:
                if ((goodsType == Goods.FOOD) && tile.isLand()) {
                    //TODO: Special tile stuff. He gets +6/+4 for wheat/deer tiles respectively...
                    base += 2;
                }
                break;
            case EXPERT_FISHERMAN:
                if ((goodsType == Goods.FOOD) && !tile.isLand()) {
                    //TODO: Special tile stuff. He gets +6 for a fishery tile.
                    base += 2;
                }
                break;
            case EXPERT_FUR_TRAPPER:
                if (goodsType == Goods.FURS) {
                    base *= 2;
                }
                break;
            case EXPERT_SILVER_MINER:
                if (goodsType == Goods.SILVER) {
                    //TODO: Mountain should be +1, not *2, but mountain didn't exist at type of writing.
                    base *= 2;
                }
                break;
            case EXPERT_LUMBER_JACK:
                if (goodsType == Goods.LUMBER) {
                    base *= 2;
                }
                break;
            case EXPERT_ORE_MINER:
                if (goodsType == Goods.ORE) {
                    base *= 2;
                }
                break;
            case MASTER_SUGAR_PLANTER:
                if (goodsType == Goods.SUGAR) {
                    base *= 2;
                }
                break;
            case MASTER_COTTON_PLANTER:
                if (goodsType == Goods.COTTON) {
                    base *= 2;
                }
                break;
            case MASTER_TOBACCO_PLANTER:
                if (goodsType == Goods.TOBACCO) {
                    base *= 2;
                }
                break;
            case INDIAN_CONVERT:
                if ((goodsType == Goods.FOOD
                        || goodsType == Goods.SUGAR
                        || goodsType == Goods.COTTON
                        || goodsType == Goods.TOBACCO
                        || goodsType == Goods.FURS
                        || goodsType == Goods.ORE
                        || goodsType == Goods.SILVER)
                        && base > 0) {
                    base += 1;
                }
                break;
            default: // Beats me who or what is working here, but he doesn't get a bonus.
                break;
        }

        return base;
    }


    public static int getNextHammers(int type) {

        UnitType  unitType = FreeCol.specification.unitType( type );

        if ( unitType.canBeBuilt() ) {

            return unitType.hammersRequired;
        }

        return -1;
    }

    public static int getNextTools(int type) {

        UnitType  unitType = FreeCol.specification.unitType( type );

        if ( unitType.canBeBuilt() ) {

            return unitType.toolsRequired;
        }

        return -1;
    }


    /**
    * Removes all references to this object.
    */
    public void dispose() {
        if (isCarrier()) {
            unitContainer.dispose();
            goodsContainer.dispose();
        }

        if (location != null) {
            location.remove(this);
        }

        setIndianSettlement(null);

        getOwner().invalidateCanSeeTiles();

        super.dispose();
    }


    /**
    * Prepares the <code>Unit</code> for a new turn.
    */
    public void newTurn() {
        if (isUninitialized()) {
            logger.warning("Calling newTurn for an uninitialized object: " + getID());
        }
        if (getType() == FREE_COLONIST && location instanceof ColonyTile) {
            logger.finest("About to call getRandom for experience");
            int random = getGame().getModelController().getRandom(getID() + "experience", 2000);
            if (random < Math.min(experience, 200)) {
                logger.finest("About to change type of unit.");
                String oldName = getName();
                setType(((ColonyTile) location).getExpertForProducing(workType));
                logger.finest("About to add model message.");
                addModelMessage(getTile().getColony(), "model.unit.experience",
                            new String[][] {{"%oldName%", oldName},
                                            {"%newName%", getName()},
                                            {"%nation%", owner.getNationAsString()}},
                            ModelMessage.UNIT_IMPROVED, this);
            }
        }
        logger.finest("About to change moves left.");
        movesLeft = getInitialMovesLeft();
        doAssignedWork();
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     * 
     * <br><br>
     * 
     * Only attributes visible to the given <code>Player</code> will 
     * be added to that representation if <code>showAll</code> is
     * set to <code>false</code>.
     *  
     * @param out The target stream.
     * @param player The <code>Player</code> this XML-representation 
     *      should be made for, or <code>null</code> if
     *      <code>showAll == true</code>.
     * @param showAll Only attributes visible to <code>player</code> 
     *      will be added to the representation if <code>showAll</code>
     *      is set to <i>false</i>.
     * @param toSavedGame If <code>true</code> then information that
     *      is only needed when saving a game is added.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame) throws XMLStreamException {
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
        String ownerID = (getOwner().equals(player) || getType() != PRIVATEER || showAll) ? owner.getID() : "unknown";
        out.writeAttribute("owner", ownerID);
        out.writeAttribute("turnsOfTraining", Integer.toString(turnsOfTraining));
        out.writeAttribute("trainingType", Integer.toString(trainingType));
        out.writeAttribute("workType", Integer.toString(workType));
        out.writeAttribute("experience", Integer.toString(experience));
        out.writeAttribute("treasureAmount", Integer.toString(treasureAmount));
        out.writeAttribute("hitpoints", Integer.toString(hitpoints));
        
        if (indianSettlement != null) {
            if (showAll || player == getOwner()) {
                out.writeAttribute("indianSettlement", indianSettlement.getID());
            }
        }

        if (entryLocation != null) {
            out.writeAttribute("entryLocation", entryLocation.getID());
        }

        if (location != null) {
            if (showAll || player == getOwner() || !(location instanceof Building || location instanceof ColonyTile)) {
                out.writeAttribute("location", location.getID());
            } else {
                out.writeAttribute("location", getTile().getColony().getID());
            }
        }
        
        if (destination != null) {
            out.writeAttribute("destination", destination.getID());
        }

        // Do not show enemy units hidden in a carrier:
        if (isCarrier()) {
            if (showAll || getOwner().equals(player) || !getGameOptions().getBoolean(GameOptions.UNIT_HIDING) && player.canSee(getTile())) {
                unitContainer.toXML(out, player, showAll, toSavedGame);
                goodsContainer.toXML(out, player, showAll, toSavedGame);
            } else {                
                out.writeAttribute("visibleGoodsCount", Integer.toString(getGoodsCount()));
                UnitContainer emptyUnitContainer = new UnitContainer(getGame(), this);
                emptyUnitContainer.setFakeID(unitContainer.getID());
                emptyUnitContainer.toXML(out, player, showAll, toSavedGame);
                GoodsContainer emptyGoodsContainer = new GoodsContainer(getGame(), this);
                emptyGoodsContainer.setFakeID(goodsContainer.getID());
                emptyGoodsContainer.toXML(out, player, showAll, toSavedGame);
            }
        }

        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setID(in.getAttributeValue(null, "ID"));

        setName(in.getAttributeValue(null, "name"));
        type = Integer.parseInt(in.getAttributeValue(null, "type"));
        unitType = FreeCol.specification.unitType( type );
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
        trainingType = Integer.parseInt(in.getAttributeValue(null, "trainingType"));
        hitpoints = Integer.parseInt(in.getAttributeValue(null, "hitpoints"));

        final String indianSettlementStr = in.getAttributeValue(null, "indianSettlement");
        if (indianSettlementStr != null) {
            indianSettlement = (IndianSettlement) getGame().getFreeColGameObject(indianSettlementStr);
            if (indianSettlement == null) {
                indianSettlement = new IndianSettlement(getGame(), indianSettlementStr);
            }
        } else {
            setIndianSettlement(null);
        }        

        final String treasureAmountStr = in.getAttributeValue(null, "treasureAmount");
        if (treasureAmountStr != null) {
            treasureAmount = Integer.parseInt(treasureAmountStr);
        } else {
            treasureAmount = 0;
        }

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
        
        final String workTypeStr = in.getAttributeValue(null, "workType");
        if (workTypeStr != null) {
            workType = Integer.parseInt(workTypeStr);
        }
        
        final String experienceStr = in.getAttributeValue(null, "experience");
        if (experienceStr != null) {
            experience = Integer.parseInt(experienceStr);
        }
        
        final String visibleGoodsCountStr = in.getAttributeValue(null, "visibleGoodsCount");
        if (visibleGoodsCountStr != null) {
            visibleGoodsCount = Integer.parseInt(visibleGoodsCountStr);
        } else {
            visibleGoodsCount = -1;
        }

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

        if (isCarrier()) {
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
            if (unitContainer == null) {
                logger.warning("Carrier did not have a \"unitContainer\"-tag.");
                unitContainer = new UnitContainer(getGame(), this);

            }
            if (goodsContainer == null) {
                logger.warning("Carrier did not have a \"goodsContainer\"-tag.");
                goodsContainer = new GoodsContainer(getGame(), this);

            }
        } else {
            in.nextTag();
        }
        
        getOwner().invalidateCanSeeTiles();        
    }


    /**
    * Gets the tag name of the root element representing this object.
    * @return "unit.
    */
    public static String getXMLElementTagName() {
        return "unit";
    }

}
