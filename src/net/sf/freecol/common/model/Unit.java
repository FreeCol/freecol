
package net.sf.freecol.common.model;

import java.util.Vector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.model.Colony;

import org.w3c.dom.Element;
import org.w3c.dom.Document;


/**
* Represents all pieces that can be moved on the map-board.
* This includes: colonists, ships, wagon trains e.t.c.
*
* <br><br>
*
* Every <code>Unit</code> is owned by a {@link Player} and has a
* {@link Location}.
*/
public class Unit extends FreeColGameObject implements Location, Locatable {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static final Logger logger = Logger.getLogger(Unit.class.getName());

    // The type of a unit; used only for gameplaying purposes NOT painting purposes.
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
                            UNIT_COUNT = 40;

    // The states a Unit can have.
    public static final int ACTIVE = 0,
                            FORTIFY = 1,
                            SENTRY = 2,
                            IN_COLONY = 3,
                            PLOW = 4,
                            BUILD_ROAD = 5,
                            TO_EUROPE = 6,
                            IN_EUROPE = 7,
                            TO_AMERICA = 8;


    // The move types
    public static final int MOVE = 0,
                            MOVE_HIGH_SEAS = 1,
                            ATTACK = 2,
                            EMBARK = 3,
                            DISEMBARK = 4,
                            ENTER_INDIAN_VILLAGE_WITH_FREE_COLONIST = 5,
                            ENTER_INDIAN_VILLAGE_WITH_SCOUT = 6,
                            ENTER_INDIAN_VILLAGE_WITH_MISSIONARY = 7,
                            ENTER_FOREIGN_COLONY_WITH_SCOUT = 8,
                            ILLEGAL_MOVE = 9;

    public static final int ATTACK_GREAT_LOSS = -2,
                            ATTACK_LOSS = -1,
                            ATTACK_EVADES = 0,
                            ATTACK_WIN  = 1,
                            ATTACK_GREAT_WIN = 2,
                            ATTACK_DONE_SETTLEMENT = 3; // The last defender of the settlement has died.


    private int             type;
    private boolean         armed,
                            mounted,
                            missionary;
    private int             movesLeft;
    private int             state;
    private int             workLeft; // expressed in number of turns, '-1' if a Unit can stay in its state forever
    private int             numberOfTools;
    private Player          owner;
    private UnitContainer   unitContainer;
    private GoodsContainer  goodsContainer;
    private Location        entryLocation;
    private Location        location;

    // to be used only for type == TREASURE_TRAIN
    private int             treasureAmount;

    private int             workType; // What type of goods this unit produces in its occupation.

    private int             turnsOfTraining = 0;
    private int             trainingType = -1;



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
    * @param The <code>Game</code> in which this <code>Unit</code> belong.
    * @param owner The Player owning the unit.
    * @param type The type of the unit.
    */
    public Unit(Game game, Player owner, int type) {
        this(game, null, owner, type, isCarrier(type)?ACTIVE:SENTRY);
    }


    /**
    * Initiate a new <code>Unit</code> with the specified parameters.
    *
    * @param game The <code>Game</code> in which this <code>Unit</code> belong.
    * @param location The <code>Location/code> to place this <code>Unit</code> upon.
    * @param owner The <code>Player</code> owning this unit.
    * @param type The type of the unit.
    * @param s The initial state for this Unit (one of {@link #ACTIVE}, {@link #FORTIFY}...).
    */
    public Unit(Game game, Location location, Player owner, int type, int s) {
        super(game);

        unitContainer = new UnitContainer(game, this);
        goodsContainer = new GoodsContainer(game, this);

        this.owner = owner;
        this.type = type;
        this.movesLeft = getInitialMovesLeft();

        setLocation(location);

        state = s;
        workLeft = -1;
        workType = Goods.FOOD;

        if (type == VETERAN_SOLDIER) {
            armed = true;
        } else {
            armed = false;
        }

        if (type == SEASONED_SCOUT) {
            mounted = true;
        } else {
            mounted = false;
        }

        if (type == HARDY_PIONEER) {
            numberOfTools = 100;
        } else {
            numberOfTools = 0;
        }

        if (type == JESUIT_MISSIONARY) {
            missionary = true;
        } else {
            missionary = false;
        }
    }


    /**
    * Initialize this object from an XML-representation of this object.
    *
    * @param The <code>Game</code> in which this <code>Unit</code> belong.
    * @param element The DOM-element ("Document Object Model") made to represent this "Unit".
    */
    public Unit(Game game, Element element) {
        super(game, element);
        readFromXMLElement(element);
    }



    /**
    * Returns the current amount of treasure in this unit.
    * Should be type of TREASURE_TRAIN.
    * @return The amount of treasure.
    */
    public int getTreasureAmount() {
        if (getType() == TREASURE_TRAIN) {
            return treasureAmount;
        } else {
            throw new IllegalStateException();
        }
    }


    /**
     * the current amount of treasure in this unit.
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

        if (location instanceof Tile && getTile().getColony() != null ||
                (getLocation() instanceof Unit && ((Unit) getLocation()).getLocation() instanceof Europe)) {
            boolean inEurope = (getLocation() instanceof Unit && ((Unit) getLocation()).getLocation() instanceof Europe);
            int cashInAmount = (getOwner().hasFather(FoundingFather.HERNAN_CORTES) || inEurope) ? getTreasureAmount() : getTreasureAmount() / 2; // TODO: Use tax
            FreeColGameObject o = getOwner();
            if (inEurope) {
                o = getOwner().getEurope();
            }
            getOwner().modifyGold(cashInAmount);
            addModelMessage(o, "model.unit.cashInTreasureTrain", new String[][] {{"%amount%", Integer.toString(getTreasureAmount())}, {"%cashInAmount%", Integer.toString(cashInAmount)}});
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
        if (isNaval() || getType() == INDIAN_CONVERT || getType() == ARTILLERY || getType() == DAMAGED_ARTILLERY || getType() == WAGON_TRAIN || getType() == TREASURE_TRAIN) {
            return false;
        } else {
            return true;
        }
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
        } else {
            return Integer.MAX_VALUE;
        }
    }


    /**
    * Gets the skill level.
    */
    public static int getSkillLevel(int unitType) {
        switch (unitType) {
            case FREE_COLONIST:
                return 0;
            case EXPERT_FISHERMAN:
            case EXPERT_FARMER:
            case EXPERT_FUR_TRAPPER:
            case EXPERT_SILVER_MINER:
            case EXPERT_LUMBER_JACK:
            case EXPERT_ORE_MINER:
            case MASTER_CARPENTER:
            case SEASONED_SCOUT:
            case HARDY_PIONEER:
                return 1;
            case MASTER_SUGAR_PLANTER:
            case MASTER_COTTON_PLANTER:
            case MASTER_TOBACCO_PLANTER:
            case MASTER_GUNSMITH:
            case MASTER_FUR_TRADER:
            case MASTER_BLACKSMITH:
            case MASTER_DISTILLER:
            case MASTER_WEAVER:
            case MASTER_TOBACCONIST:
            case VETERAN_SOLDIER:
                return 2;
            case FIREBRAND_PREACHER:
            case ELDER_STATESMAN:
            case JESUIT_MISSIONARY:
                return 3;
            default:
                throw new IllegalStateException();
        }
    }


    /**
    * Gets the number of turns this unit has been training.
    *
    * @see #setTurnsOfTraining
    * @see #getTrainingType
    * @see #getNeededTurnsOfTraining
    */
    public int getTurnsOfTraining() {
        return turnsOfTraining;
    }


    /**
    * Sets the number of turns this unit has been training.
    * @see #getNeededTurnsOfTraining
    */
    public void setTurnsOfTraining(int turnsOfTraining) {
        this.turnsOfTraining = turnsOfTraining;
    }



    /**
    * Gets the unit type this <code>Unit</code> is training for.
    *
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
     * Gets the type of goods this unit is producing in its current occupation.
     * @return The type of goods this unit would produce.
     */
    public int getWorkType() {
        if (getLocation() instanceof Building) {
          return ((Building) getLocation()).getGoodsOutputType();
        } else {
          return workType;
        }
    }

     /**
     * Sets the type of goods this unit is producing in its current occupation.
     * @param type The type of goods to attempt to produce.
     */
    public void setWorkType(int type) {
        workType = type;
    }


    /**
     * Gets the type of a move made in a specified direction.
     *
     * @param direction The direction of the move.
     * @return The move type. Notice: <code>Unit.ILLEGAL_MOVE</code>
     *         when there are no moves left.
     */
    public int getMoveType(int direction) {
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

        if (getMovesLeft() <= 0) {
            return ILLEGAL_MOVE;
        }

        if (target == null) { // TODO: do not allow "MOVE_HIGH_SEAS" north and south.
            return isNaval() ? MOVE_HIGH_SEAS : ILLEGAL_MOVE;
        }

        // Check for disembark.
        if (isNaval() && target.isLand()) {
            if (target.getSettlement() != null && target.getSettlement().getOwner() == getOwner()) {
                return MOVE;
            } else if (target.getDefendingUnit(this) != null && target.getDefendingUnit(this).getOwner() != getOwner()) {
                return ILLEGAL_MOVE;
            }

            Iterator unitIterator = getUnitIterator();

            while (unitIterator.hasNext()) {
                Unit u = (Unit) unitIterator.next();
                if (u.getMovesLeft() > 0) {
                    return DISEMBARK;
                }
            }

            return ILLEGAL_MOVE;
        }

        if (target.getMoveCost(getTile()) > getMovesLeft() + 1 && getMovesLeft() < getInitialMovesLeft()) {
            return ILLEGAL_MOVE;
        }


        // Check for an 'attack' instead of 'move'.
        if (target.getSettlement() != null && target.getSettlement().getOwner() != getOwner()
                || (target.getUnitCount() > 0) && (target.getDefendingUnit(this) != null) &&
                (target.getDefendingUnit(this).getNation() != getNation())
                && ((target.isLand() && !isNaval()) || (isNaval() && !target.isLand()))) {

            if (isScout() && target.getSettlement() != null) {
                if (target.getSettlement() instanceof IndianSettlement) {
                    return ENTER_INDIAN_VILLAGE_WITH_SCOUT;
                } else if (target.getSettlement().getOwner() != getOwner()) {
                    return ENTER_FOREIGN_COLONY_WITH_SCOUT;
                }
            } else if (isOffensiveUnit()) {
                return ATTACK;
            } else {
                // Check for entering indian village.
                if ((target.getSettlement() != null)
                        && (target.getSettlement() instanceof IndianSettlement)
                        && (getNumberOfTools() == 0)
                        /* TODO: CHECK IF YOU'RE ALLOWED IN THE VILLAGE (=no war with indians) */) {
                    if (isMounted()) {
                        return ENTER_INDIAN_VILLAGE_WITH_SCOUT;
                    } else if (isMissionary()) {
                        return ENTER_INDIAN_VILLAGE_WITH_MISSIONARY;
                    } else if ((getType() == FREE_COLONIST) || (getType() == INDENTURED_SERVANT)) {
                        return ENTER_INDIAN_VILLAGE_WITH_FREE_COLONIST;
                    } else {
                        return ILLEGAL_MOVE;
                    }
                } else {
                    return ILLEGAL_MOVE;
                }
            }
        }

        if (target.getType() == Tile.HIGH_SEAS) {
            return isNaval() ? MOVE_HIGH_SEAS : ILLEGAL_MOVE;
        }

        // Check for an embark:
        if (!isNaval() && !target.isLand()) {
            //boolean foundOneCarrier = false;

            if (target.getFirstUnit() == null || target.getFirstUnit().getNation() != getNation()) {
                return ILLEGAL_MOVE;
            }

            Iterator unitIterator = target.getUnitIterator();

            while (unitIterator.hasNext()) {
                Unit u = (Unit) unitIterator.next();

                if (u.getSpaceLeft() >= getTakeSpace()) {
                    return EMBARK;
                }
            }

            return ILLEGAL_MOVE;
        }

        return MOVE;
    }


    /**
    * Sets the <code>movesLeft</code>. If <code>movesLeft < 0</code>
    * then <code>movesLeft = 0</code>.
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

        if (isScout() || type == FRIGATE || type == GALLEON || type == MAN_O_WAR || type == PRIVATEER) {
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
        if (isMounted() && !isArmed()) {
            return true;
        } else {
            return false;
        }
    }


    /**
    * Moves this unit in the specified direction.
    *
    * @see #getMoveType
    * @exception IllegalStateException If the move is illegal.
    */
    public void move(int direction) {
        int type = getMoveType(direction);

        if (type != MOVE && type != DISEMBARK && type != MOVE_HIGH_SEAS) {
            throw new IllegalStateException("Illegal move requested: " + type);
        }

        setState(ACTIVE);
        setStateToAllChildren(SENTRY);

        Tile newTile = getGame().getMap().getNeighbourOrNull(direction, getTile());
        Tile oldTile = getTile();

        if (newTile != null) {
            setLocation(newTile);
        } else {
            throw new IllegalStateException("Illegal move requested!");
        }

        if (isNaval() && newTile.getSettlement() != null) {
            setMovesLeft(0);
        } else {
            setMovesLeft(getMovesLeft() - newTile.getMoveCost(oldTile));
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
    * @param unit The unit who is going to leave the ship where it is located.
    * @param carrier The carrier.
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
    * Adds a locatable to this <code>Unit</code>.
    * @param u The Unit to add to this Unit.
    */
    public void add(Locatable locatable) {
        if (isCarrier()) {
            if (locatable instanceof Unit) {
                if (getSpaceLeft() <= 0 || getType() == WAGON_TRAIN) {
                    throw new IllegalStateException();
                }

                unitContainer.addUnit((Unit) locatable);
            } else if (locatable instanceof Goods) {
                goodsContainer.addGoods((Goods) locatable);
                if (getSpaceLeft() < 0) {
                    throw new IllegalStateException();
                }
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
            } else if (locatable instanceof Goods) {
                goodsContainer.removeGoods((Goods) locatable);
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
            if (getType() == WAGON_TRAIN && locatable instanceof Unit) {
                return false;
            }

            if(locatable instanceof Unit) {
                return getSpaceLeft() >= locatable.getTakeSpace();
            }
            else if(locatable instanceof Goods) {
                Goods g = (Goods) locatable;
                int goodsAlreadyLoaded = getGoodsContainer().getGoodsCount(g.getType());
                return (((goodsAlreadyLoaded + g.getAmount()) <= 100) && (goodsAlreadyLoaded != 0)) || (getSpaceLeft() >= 1);
            }
            else { // Is there another class that implements Locatable ??
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
        if (isCarrier()) {
            return unitContainer.getUnitCount();
        } else {
            return 0;
        }
    }


    /**
    * Gets the first <code>Unit</code> beeing carried by this <code>Unit</code>.
    * @return The <code>Unit</code>.
    */
    public Unit getFirstUnit() {
        if (isCarrier()) {
            return unitContainer.getFirstUnit();
        } else {
            return null;
        }
    }


    /**
    * Checks if this unit is visible to the given player.
    */
    public boolean isVisibleTo(Player player) {
        return (getTile() != null && player.canSee(getTile()) && (getTile().getSettlement() == null
                || getTile().getSettlement().getOwner() == player));
    }


    /**
    * Gets the last <code>Unit</code> beeing carried by this <code>Unit</code>.
    * @return The <code>Unit</code>.
    */
    public Unit getLastUnit() {
        if (isCarrier()) {
            return unitContainer.getLastUnit();
        } else {
            return null;
        }
    }


    /**
    * Gets a <code>Iterator</code> of every <code>Unit</code> directly located on this
    * <code>Location</code>.
    *
    * @return The <code>Iterator</code>.
    */
    public Iterator getUnitIterator() {
        if (isCarrier() && getType() != WAGON_TRAIN) {
            return unitContainer.getUnitIterator();
        } else { // TODO: Make a better solution:
            return (new ArrayList()).iterator();
        }
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
        if (getGame().getMap() != null && location != null && location instanceof Tile && !isNaval()) {
            Iterator tileIterator = getGame().getMap().getAdjacentIterator(getTile().getPosition());
            while (tileIterator.hasNext()) {
                Tile t = getGame().getMap().getTile((Position) tileIterator.next());

                if (t == null) {
                    continue;
                }

                if (getOwner() == null) {
                    throw new NullPointerException();
                }

                if (t.getSettlement() != null && !t.getSettlement().getOwner().hasContacted(getOwner().getNation())) {
                    t.getSettlement().getOwner().setContacted(getOwner(), true);
                    getOwner().setContacted(t.getSettlement().getOwner(), true);
                } else if (t.isLand() && t.getFirstUnit() != null && !t.getFirstUnit().getOwner().hasContacted(getOwner().getNation())) {
                    t.getFirstUnit().getOwner().setContacted(getOwner(), true);
                    getOwner().setContacted(t.getFirstUnit().getOwner(), true);
                }
            }
        }

        getOwner().setExplored(this);
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

        setLocation(getTile());
        setMovesLeft(0);

        if (getTile().getColony().getUnitCount() <= 0) {
            getTile().getColony().dispose();
        }
    }


    /**
    * Checks if this unit can be armed in the current location.
    */
    public boolean canArm() {
        return isArmed() || getGoodsDumpLocation() != null && getGoodsDumpLocation().getGoodsCount(Goods.MUSKETS) >= 50 ||
               (location instanceof Europe || location instanceof Unit && ((Unit) location).getLocation() instanceof Europe) &&
               getOwner().getGold() >= getGame().getMarket().getBidPrice(Goods.MUSKETS, 50);
    }


    /**
    * Checks if this unit can be mounted in the current location.
    */
    public boolean canMount() {
        return isMounted() || getGoodsDumpLocation() != null && getGoodsDumpLocation().getGoodsCount(Goods.HORSES) >= 50 ||
               (location instanceof Europe || location instanceof Unit && ((Unit) location).getLocation() instanceof Europe)
               && getOwner().getGold() >= getGame().getMarket().getBidPrice(Goods.HORSES, 50);
    }


    /**
    * Checks if this unit can be equiped with tools in the current location.
    */
    public boolean canEquipWithTools() {
        return isPioneer() || getGoodsDumpLocation() != null && getGoodsDumpLocation().getGoodsCount(Goods.TOOLS) >= 20 ||
               (location instanceof Europe || location instanceof Unit && ((Unit) location).getLocation() instanceof Europe)
               && getOwner().getGold() >= getGame().getMarket().getBidPrice(Goods.TOOLS, 20);
    }


    /**
    * Checks if this unit can be dressed as a missionary at the current location.
    */
    public boolean canDressAsMissionary() {
        return isMissionary() || ((location instanceof Europe || location instanceof Unit && ((Unit) location).getLocation()
               instanceof Europe) || getTile() != null && getTile().getColony().getBuilding(Building.CHURCH).isBuilt());
    }


    /**
    * Sets the armed attribute of this unit.
    * @param b <i>true</i> if this unit should be armed and <i>false</i> otherwise.
    * @param isCombat Whether this is a result of combat.
    */
    public void setArmed(boolean b, boolean isCombat) {
        setMovesLeft(0);

        if (isCombat) {
            armed = b; // No questions asked.
            return;
        }

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
            } else if (location instanceof Europe) {
                getGame().getMarket().buy(Goods.MUSKETS, 50, getOwner());
                armed = true;
            } else {
                logger.warning("Attempting to arm a soldier outside of a colony or Europe!");
            }
        } else if ((!b) && (armed)) {
            armed = false;

            if (getGoodsDumpLocation() != null) {
                getGoodsDumpLocation().addGoods(Goods.MUSKETS, 50);
            } else if (location instanceof Europe) {
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
    * @param <i>true</i> if this unit should be mounted and <i>false</i> otherwise.
    * @param isCombat Whether this is a result of combat.
    */
    public void setMounted(boolean b, boolean isCombat) {
        setMovesLeft(0);

        if (isCombat) {
            mounted = b; // No questions asked.
            return;
        }

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
            } else if (location instanceof Europe) {
                getGame().getMarket().buy(Goods.HORSES, 50, getOwner());
                mounted = true;
            } else {
                logger.warning("Attempting to mount a colonist outside of a colony or Europe!");
            }
        } else if ((!b) && (mounted)) {
            mounted = false;

            if (getGoodsDumpLocation() != null) {
                getGoodsDumpLocation().addGoods(Goods.HORSES, 50);
            } else if (location instanceof Europe) {
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
    * Sets the unit to be a missionary.
    *
    * @param b <i>true</i> if the unit should be a missionary and <i>false</i> otherwise.
    */
    public void setMissionary(boolean b) {
        setMovesLeft(0);

        if (!b && (getLocation() instanceof Europe || (getTile() != null && getTile().getColony() != null))) {
            missionary = b;
        } else if (!(getLocation() instanceof Europe) && !getTile().getColony().getBuilding(Building.CHURCH).isBuilt()) {
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

            missionary = b;
        }
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
    * @param The amount of goods to buy.
    */
    public void buyGoods(int type, int amount) {
        if (!isCarrier() || !(getLocation() instanceof Europe && getState() != TO_EUROPE && getState() != TO_AMERICA)) {
            throw new IllegalStateException("Cannot buy goods when not a carrier or in Europe.");
        }

        try {
            getGame().getMarket().buy(type, amount, getOwner());
            goodsContainer.addGoods(type, amount);
        } catch(IllegalStateException ise) {
            this.addModelMessage(this, "notEnoughGold", null);
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
            } else if (location instanceof Europe) {
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
            } else if (location instanceof Europe) {
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
        if (getNumberOfTools() > 0) {
            return true;
        } else {
            return false;
        }
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
        if ((type == CARAVEL) || (type == GALLEON) || (type == FRIGATE) || (type == MAN_O_WAR) || (type == MERCHANTMAN) || (type == PRIVATEER) || (type == WAGON_TRAIN)) {
            return true;
        } else {
            return false;
        }
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
    * @param type The new owner of this Unit.
    */
    public void setOwner(Player owner) {
        this.owner = owner;
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
    * @return The amount of moves this Unit has left.
    */
    public int getMovesLeft() {
        return movesLeft;
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
        // TODO: Use i18n:

        String name = "";
        boolean addP = false;

        if (isPioneer() && getType() != HARDY_PIONEER) {
            name = "Pioneer (";
            addP = true;
        } else if (isArmed() && getType() != KINGS_REGULAR && getType() != COLONIAL_REGULAR && getType() != BRAVE && getType() != VETERAN_SOLDIER) {
            if (!isMounted()) {
                name = "Soldier (";
            } else {
                name = "Dragoon (";
            }
            addP = true;
        } else if (isMounted() && getType() != SEASONED_SCOUT && getType() != BRAVE) {
            name = "Scout (";
            addP = true;
        }

        if (!isArmed() && (getType() == KINGS_REGULAR || getType() == COLONIAL_REGULAR || getType() == VETERAN_SOLDIER)) {
            name = "Unarmed ";
        }

        if (getType() == BRAVE) {
            if (isArmed() && !isMounted()) {
                name = "Armed ";
            } else if (isMounted()) {
                name = "Mounted ";
            }
        }

        name += getName(getType());

        if (isArmed() && isMounted()) {
            if (getType() == KINGS_REGULAR) {
                name = "King's cavalry";
                addP = false;
            } else if (getType() == COLONIAL_REGULAR) {
                name = "Colonial cavalry";
                addP = false;
            } else if (getType() == VETERAN_SOLDIER) {
                name = "Veteran Dragoon";
                addP = false;
            } else if (getType() == BRAVE) {
                name = "Indian Dragoon";
                addP = false;
            }
        }

        if (addP) {
            name += ")";
        }

        return name;
    }


    /**
    * Returns the name of a unit type in a human readable format. The return
    * value can be used when communicating with the user.
    *
    * @return The given unit type as a String
    * @throws IllegalArgumentException
    */
    public static String getName(int someType) {
        // TODO: USE i18n !

        switch (someType) {
            case FREE_COLONIST:
                return "Free Colonist";
            case EXPERT_FARMER:
                return "Expert Farmer";
            case EXPERT_FISHERMAN:
                return "Expert Fisherman";
            case EXPERT_FUR_TRAPPER:
                return "Expert FurTrapper";
            case EXPERT_SILVER_MINER:
                return "Expert SilverMiner";
            case EXPERT_LUMBER_JACK:
                return "Expert LumberJack";
            case EXPERT_ORE_MINER:
                return "Expert Ore Miner";
            case MASTER_SUGAR_PLANTER:
                return "Master Sugar Planter";
            case MASTER_COTTON_PLANTER:
                return "Master Cotton Planter";
            case MASTER_TOBACCO_PLANTER:
                return "Master Tobacco Planter";
            case FIREBRAND_PREACHER:
                return "Firebrand Preacher";
            case ELDER_STATESMAN:
                return "Elder Statesman";
            case MASTER_CARPENTER:
                return "Master Carpenter";
            case MASTER_DISTILLER:
                return "Master Distiller";
            case MASTER_WEAVER:
                return "Master Weaver";
            case MASTER_TOBACCONIST:
                return "Master Tobacconist";
            case MASTER_FUR_TRADER:
                return "Master Fur Trader";
            case MASTER_BLACKSMITH:
                return "Master Blacksmith";
            case MASTER_GUNSMITH:
                return "Master Gunsmith";
            case SEASONED_SCOUT:
                return "Seasoned Scout";
            case HARDY_PIONEER:
                return "Hardy Pioneer";
            case VETERAN_SOLDIER:
                return "Veteran Soldier";
            case JESUIT_MISSIONARY:
                return "Jesuit Missionary";
            case INDENTURED_SERVANT:
                return "Indentured Servant";
            case PETTY_CRIMINAL:
                return "Petty Criminal";
            case INDIAN_CONVERT:
                return "Indian Convert";
            case BRAVE:
                return "Brave";
            case COLONIAL_REGULAR:
                return "Colonial Regular";
            case KINGS_REGULAR:
                return "King's Regular";
            case CARAVEL:
                return "Caravel";
            case FRIGATE:
                return "Frigate";
            case GALLEON:
                return "Galleon";
            case MAN_O_WAR:
                return "Man of War";
            case MERCHANTMAN:
                return "Merchantman";
            case PRIVATEER:
                return "Privateer";
            case ARTILLERY:
                return "Artillery";
            case DAMAGED_ARTILLERY:
                return "Damaged Artillery";
            case TREASURE_TRAIN:
                return "Treasure Train";
            case WAGON_TRAIN:
                return "Wagon Train";
            case MILKMAID:
                return "Milkmaid";
            default:
                throw new IllegalArgumentException("Unit has an invalid type.");
        }
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
                default:
                    logger.warning("Unit.getInitialMovesLeft(): Unit has invalid naval type.");
                    return 9+fMagellan;
            }
        } else {
            if (isMounted()) {
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
    * Gets the x-coordinate of this Unit (on the map).
    * @return The x-coordinate of this Unit (on the map).
    */
    public int getX() {
        if (getTile() != null) {
            return getTile().getX();
        } else {
            return -1;
        }
    }


    /**
    * Gets the y-coordinate of this Unit (on the map).
    * @return The y-coordinate of this Unit (on the map).
    */
    public int getY() {
        if (getTile() != null) {
            return getTile().getY();
        } else {
            return -1;
        }
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
        if (movesLeft%3 == 0 || movesLeft/3 > 0) {
            moves += Integer.toString(movesLeft/3);
        }

        if (movesLeft%3 != 0) {
            if (movesLeft/3 > 0) {
                moves += " ";
            }

            moves += "(" + Integer.toString(movesLeft - (movesLeft/3) * 3) + "/3) ";
        }

        moves += "/" + Integer.toString(getInitialMovesLeft()/3);
        return moves;
    }


    /**
    * Checks if this <code>Unit</code> is naval.
    * @return <i>true</i> if this Unit is a naval Unit and <i>false</i> otherwise.
    */
    public boolean isNaval() {
        if ((getType() == CARAVEL) || (getType() == GALLEON) || (getType() == FRIGATE) || (getType() == MAN_O_WAR) || (getType() == MERCHANTMAN) || (getType() == PRIVATEER)) {
            return true;
        }
        else {
            return false;
        }
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
            throw new IllegalStateException();
        }

        switch (s) {
            case ACTIVE:
                workLeft = -1;
                break;
            case SENTRY:
                workLeft = -1;
                // Make sure we don't lose our movepoints if we're on a ship -sjm
                if (location instanceof Tile) {
                    movesLeft = 0;
                }
                break;
            case FORTIFY:
                workLeft = -1;
                movesLeft = 0;
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
                if (s == BUILD_ROAD) workLeft /= 2;
                movesLeft = 0;
            case TO_EUROPE:
                if ((state == ACTIVE) && (!(location instanceof Europe))) {
                    workLeft = 3;
                } else if ((state == TO_AMERICA) && (location instanceof Europe)) {
                    // I think '4' was also used in the original game.
                    workLeft = 4 - workLeft;
                }
                break;
            case TO_AMERICA:
                if ((state == ACTIVE) && (location instanceof Europe)) {
                    workLeft = 3;
                } else if ((state == TO_EUROPE) && (location instanceof Europe)) {
                    // I think '4' was also used in the original game.
                    workLeft = 4 - workLeft;
                }
                break;
            default:
                workLeft = -1;
        }
        state = s;
    }


    /**
    * Moves this unit to europe.
    * @exception IllegalStateException If the move is illegal.
    */
    public void moveToEurope() {

        // Check if this move is illegal or not:
        if (!(getLocation() instanceof Europe)) {
            boolean ok = false;

            Vector surroundingTiles = getGame().getMap().getSurroundingTiles(getTile(), 1);
            if (surroundingTiles.size() != 8) {
                ok = true;
            } else {
                for (int i=0; i<surroundingTiles.size(); i++) {
                    Tile tile = (Tile) surroundingTiles.get(i);
                    if (tile == null || tile.getType() == Tile.HIGH_SEAS) {
                        ok = true;
                        break;
                    }
                }
            }

            if (!ok) {
                throw new IllegalStateException("It is not allowed to move units to europe from the tile where this unit is located.");
            }

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


    /*
    * Checks if a <code>Unit</code> can get the given state set.
    *
    * @param s The new state for this Unit. Should be one of
    * {ACTIVE, FORTIFIED, ...}.
    * @return 'true' if the Unit's state can be changed to the
    * new value, 'false' otherwise.
    */
    public boolean checkSetState(int s) {
        switch (s) {
            case ACTIVE:
                return true;
            case PLOW:
                if (!((getTile().isForested()) || !(getTile().isPlowed()))) return false;
            case BUILD_ROAD:
                if ((s == BUILD_ROAD) && (getTile().hasRoad())) return false;
                if (getNumberOfTools() < 20) return false;
            case IN_COLONY:
                if (isNaval()) {
                    return false;
                }
            case FORTIFY:
                if (getMovesLeft() > 0) {
                    return true;
                }
                else {
                    return false;
                }
            case SENTRY:
                if (getTile() == null) {
                    return true;
                } else if (!getTile().isLand()) {
                    return true;
                } else {
                    if (getMovesLeft() > 0) {
                        return true;
                    }
                    else {
                        return false;
                    }
                }
            case TO_EUROPE:
                if (!isNaval()) {
                    return false;
                }
                if (((location instanceof Europe) && (getState() == TO_AMERICA))
                        || (getEntryLocation() == getLocation())) {
                        //|| (getTile().getType() == Tile.HIGH_SEAS)) {
                    return true;
                }
                return false;
            case TO_AMERICA:
                return location instanceof Europe && isNaval();
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
        if (getTile() == null || !getTile().isColonizeable() || !isColonist()) {
            return false;
        } else {
            return true;
        }
    }


    /**
    * Makes this unit build the specified colony.
    * @param colony The colony this unit shall build.
    */
    public void buildColony(Colony colony) {
        if (!canBuildColony()) {
            throw new IllegalStateException();
        }

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
        return location.getTile();
    }


    /**
    * Returns the amount of space left on this Unit.
    * @return The amount of units/goods than can be moved onto this Unit.
    */
    public int getSpaceLeft() {
        return getInitialSpaceLeft() - (getUnitCount() + getGoodsCount());
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
    * This method does all the work :-)
    */
    public void doAssignedWork() {
        if (workLeft > 0) {
            workLeft--;
            if (workLeft == 0) {
                workLeft = -1;

                int state = getState();

                switch (state) {
                    case TO_EUROPE:
                        addModelMessage(getOwner().getEurope(), "model.unit.arriveInEurope", null);
                        if (getType() == GALLEON) {
                            Iterator iter = getUnitIterator();
                            Unit u = (Unit) iter.next();
                            while (iter.hasNext() && u.getType() != TREASURE_TRAIN) {
                                u = (Unit) iter.next();
                            }
                            if (u != null && u.getType() == TREASURE_TRAIN) {
                                u.cashInTreasureTrain();
                            }
                        }
                        break;
                    case TO_AMERICA:
                        getGame().getModelController().setToVacantEntryLocation(this);
                        break;
                    case BUILD_ROAD:
                        getTile().setRoad(true);
                        numberOfTools -= 20;
                        break;
                    case PLOW:
                        if (getTile().isForested()) {
                            getTile().setForested(false);
                            // Give Lumber to adjacent colony
                            if (getTile().getColony() != null &&
                                getTile().getColony().getOwner().equals(getOwner())) {
                                getTile().getColony().addGoods(Goods.LUMBER, 100);
                            }
                            else {
                                Vector surroundingTiles = getTile().getMap().getSurroundingTiles(getTile(), 1);
                                for (int i=0; i<surroundingTiles.size(); i++) {
                                    Tile t = (Tile) surroundingTiles.get(i);
                                    if (t.getColony() != null &&
                                        t.getColony().getOwner().equals(getOwner())) {
                                        t.getColony().addGoods(Goods.LUMBER, 100);
                                        break;
                                    }
                                }
                            }
                        } else {
                            getTile().setPlowed(true);
                        }
                        numberOfTools -= 20;
                        break;
                    default:
                        logger.warning("Unknown work completed. State: " + state);
                }

                setState(ACTIVE);
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
        if (entryLocation != null) {
            return entryLocation;
        } else {
            return getOwner().getEntryLocation();
        }
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
    *                 {@link ModelController#getVacantEntryLocation] instead.
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
        } else {
            return l;
        }
    }


    /**
    * Returns 'true' if the given type is the type of a recruitable unit, 'false' otherwise.
    * @return 'true' if the given type is the type of a recruitable unit, 'false' otherwise.
    */
    public static boolean isRecruitable(int type) {
        switch (type) {
            case FREE_COLONIST:
            case INDENTURED_SERVANT:
            case PETTY_CRIMINAL:
            case EXPERT_ORE_MINER:
            case EXPERT_LUMBER_JACK:
            case MASTER_GUNSMITH:
            case EXPERT_SILVER_MINER:
            case MASTER_FUR_TRADER:
            case MASTER_CARPENTER:
            case EXPERT_FISHERMAN:
            case MASTER_BLACKSMITH:
            case EXPERT_FARMER:
            case MASTER_DISTILLER:
            case HARDY_PIONEER:
            case MASTER_TOBACCONIST:
            case MASTER_WEAVER:
            case JESUIT_MISSIONARY:
            case FIREBRAND_PREACHER:
            case ELDER_STATESMAN:
            case VETERAN_SOLDIER:
                return true;
            default:
                return false;
        }
    }


    /**
    * Returns the price of this unit in Europe.
    *
    * @return The price of this unit when trained in Europe. '-1' is returned in case the unit cannot be bought.
    */
    public int getPrice() {
        if (getType() == ARTILLERY) {
            return getOwner().getEurope().getArtilleryPrice();
        } else {
            return getPrice(getType());
        }
    }


    /**
    * Returns the price of a trained unit in Europe.
    *
    * @param type The type of unit of which you need the price.
    * @return The price of a trained unit in Europe. '-1' is returned in case the unit cannot be bought.
    */
    public static int getPrice(int type) {
        switch (type) {
            case EXPERT_ORE_MINER:
                return 600;
            case EXPERT_LUMBER_JACK:
                return 700;
            case MASTER_GUNSMITH:
                return 850;
            case EXPERT_SILVER_MINER:
                return 900;
            case MASTER_FUR_TRADER:
                return 950;
            case MASTER_CARPENTER:
            case EXPERT_FISHERMAN:
                return 1000;
            case MASTER_BLACKSMITH:
                return 1050;
            case EXPERT_FARMER:
            case MASTER_DISTILLER:
                return 1100;
            case HARDY_PIONEER:
            case MASTER_TOBACCONIST:
                return 1200;
            case MASTER_WEAVER:
                return 1300;
            case JESUIT_MISSIONARY:
                return 1400;
            case FIREBRAND_PREACHER:
                return 1500;
            case ELDER_STATESMAN:
                return 1900;
            case VETERAN_SOLDIER:
                return 2000;
            case ARTILLERY:
                throw new IllegalStateException();
            case CARAVEL:
                return 1000;
            case MERCHANTMAN:
                return 2000;
            case GALLEON:
                return 3000;
            case PRIVATEER:
                return 2000;
            case FRIGATE:
                return 5000;
            default:
                return -1;
        }
    }


    /**
    * Returns the current defensive power of this unit. The tile on which this
    * unit is located will be taken into account.
    * @param attacker The attacker of this unit.
    * @return The current defensive power of this unit.
    */
    public int getDefensePower(Unit attacker) {
        int base_power = 1;
        switch(getType()) {
            case TREASURE_TRAIN:
                base_power = 0;
                break;
            case BRAVE:
                base_power = 1;
                break;
            case VETERAN_SOLDIER:
                base_power = 2;
                break;
            case COLONIAL_REGULAR:
                base_power = 3;
                break;
            case KINGS_REGULAR:
                base_power = 4;
                break;
            case CARAVEL:
                base_power = 2;
                break;
            case MERCHANTMAN:
                base_power = 6;
                break;
            case GALLEON:
                base_power = 10;
                break;
            case PRIVATEER:
                base_power = 8;
                break;
            case FRIGATE:
                base_power = 16;
                break;
            case MAN_O_WAR:
                base_power = 24;
                break;
            case DAMAGED_ARTILLERY:
                base_power = 3;
                break;
            case ARTILLERY:
                base_power = 5;
                break;
            default:
                base_power = 1;
                break;
        }

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
            return modified_power;
        }

        if (getState() == FORTIFY) {
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
            if (((getTile().getSettlement()) == null) && (getState() != FORTIFY)) {
                modified_power -= ((base_power * 3) / 4); // -75% Artillery in the Open penalty
            }
        }

        return modified_power;
    }


    /**
    * Checks if this is an offensive unit.
    * That is a unit capable of attacking another unit.
    */
    public boolean isOffensiveUnit() {
        // TODO: Make this look prettier ;-)
        return (getOffensePower(this) > 0);
    }


    /**
    * Returns the current offensive power of this unit.
    * @param target The target of the attack.
    * @return The current offensive power of this unit.
    */
    public int getOffensePower(Unit target) {
        int base_power = 1;
        switch(getType()) {
            case BRAVE:
                base_power = 1;
                break;
            case VETERAN_SOLDIER:
                if (isArmed()) {
                    base_power = 2;
                } else {
                    base_power = 0;
                }
                break;
            case COLONIAL_REGULAR:
                base_power = 3;
                break;
            case KINGS_REGULAR:
                base_power = 4;
                break;
            case PRIVATEER:
                base_power = 8;
                break;
            case FRIGATE:
                base_power = 16;
                break;
            case MAN_O_WAR:
                base_power = 24;
                break;
            case DAMAGED_ARTILLERY:
                base_power = 5;
                break;
            case ARTILLERY:
                base_power = 7;
                break;
            default:
                base_power = 0;
                break;
        }

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
                modified_power += (base_power * 3) / 2;
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
    *        a successful attack on a <code>Settlement</cdoe>.
    */
    public void attack(Unit defender, int result, int plunderGold) {
        if (defender == null) {
            throw new NullPointerException();
        }

        movesLeft = 0;

        Tile newTile = defender.getTile();

        if (result == ATTACK_EVADES) {
            if (!isNaval()) {
                logger.warning("Non-naval unit evades!");
            }
        } else if (result == ATTACK_LOSS || result == ATTACK_GREAT_LOSS) {
            if (getType() == BRAVE) {
                dispose();
            } else if (isNaval()) {
                if (result == ATTACK_LOSS) {
                    // TODO: Add damage. For now just move to Europe:
                    moveToEurope();
                } else { // ATTACK_GREAT_LOSS:
                    dispose();
                }
            } else if (!(isArmed()) && !(getType() == ARTILLERY) && !(getType() == DAMAGED_ARTILLERY)) {
                dispose(); // Only scouts should ever reach this point. Nobody else should be able to attack.
            } else {
                if (isMounted()) {
                    setMounted(false, true);
                    if (defender.getType() == BRAVE && result == ATTACK_GREAT_LOSS) {
                        defender.setMounted(true, true);
                    }
                } else if (getType() == ARTILLERY) {
                    setType(DAMAGED_ARTILLERY);
                } else if ((getType() == KINGS_REGULAR) || (getType() == DAMAGED_ARTILLERY)) {
                    dispose();
                } else {
                    setArmed(false, true);
                    if (defender.getType() == BRAVE && result == ATTACK_GREAT_LOSS) {
                        defender.setArmed(true, true);
                    }
                }
            }

            if (defender.getOwner().hasFather(FoundingFather.GEORGE_WASHINGTON) || result == ATTACK_GREAT_LOSS) {
                String oldName = defender.getName();

                if (defender.getType() == PETTY_CRIMINAL) {
                    defender.setType(INDENTURED_SERVANT);
                } else if (defender.getType() == INDENTURED_SERVANT) {
                    defender.setType(FREE_COLONIST);
                } else if (getType() == FREE_COLONIST) {
                    defender.setType(VETERAN_SOLDIER);
                } else if (defender.getType() == VETERAN_SOLDIER && defender.getOwner().getRebellionState() > 1) {
                    defender.setType(COLONIAL_REGULAR);
                }

                if (!defender.getName().equals(oldName)) {
                    addModelMessage(defender, "model.unit.unitImproved", new String[][] {{"%oldName%", oldName}, {"%newName%", defender.getName()}});
                }
            }
        } else if (result == ATTACK_WIN || result == ATTACK_GREAT_WIN || result == ATTACK_DONE_SETTLEMENT) {
            getOwner().modifyTension(defender.getOwner(), -Player.TENSION_ADD_MINOR);

            // Increases the defender's tension levels:
            if (defender.getOwner().isAI()) {
                if (defender.getTile().getSettlement() != null) {
                    if (defender.getTile().getSettlement() instanceof IndianSettlement && ((IndianSettlement) defender.getTile().getSettlement()).isCapital()) {
                        defender.getOwner().modifyTension(getOwner(), Player.TENSION_ADD_MAJOR);
                    } else {
                        defender.getOwner().modifyTension(getOwner(), Player.TENSION_ADD_NORMAL);
                    }
                } else {
                    defender.getOwner().modifyTension(getOwner(), Player.TENSION_ADD_MINOR);
                }
            }


            if (defender.getType() == BRAVE) {
                defender.dispose();

                if (newTile.getSettlement() != null) {
                    if (result == ATTACK_DONE_SETTLEMENT) { // (defender==BRAVE): Can only be an indian settlmenet:
                        Player settlementOwner = newTile.getSettlement().getOwner();
                        boolean wasCapital = ((IndianSettlement)newTile.getSettlement()).isCapital();
                        newTile.getSettlement().dispose();

                        settlementOwner.modifyTension(getOwner(), Player.TENSION_ADD_MAJOR);

                        int randomTreasure = getGame().getModelController().getRandom(getID()+"indianTreasureRandom"+getID(), 11);

                        Unit tTrain = getGame().getModelController().createUnit(
                                getID()+"indianTreasure"+getID(), newTile, getOwner(), Unit.TREASURE_TRAIN);

                        // Incan and Aztecs give more gold
                        if (settlementOwner.getNation() == Player.INCA || settlementOwner.getNation() == Player.AZTEC) {
                            tTrain.setTreasureAmount(randomTreasure * 500 + 10000);
                        } else {
                            tTrain.setTreasureAmount(randomTreasure * 50 + 300);
                        }

                        // capitals give more gold
                        if (wasCapital) {
                            tTrain.setTreasureAmount((tTrain.getTreasureAmount()*3)/2);
                        }

                        addModelMessage(this, "model.unit.indianTreasure", new String[][] {{"%indian%", settlementOwner.getNationAsString()}, {"%amount%", Integer.toString(tTrain.getTreasureAmount())}});
                        setLocation(newTile);
                    }
                }
            } else if (isNaval()) {
                if (type==PRIVATEER || type==FRIGATE || type==MAN_O_WAR) { // can capture goods; regardless attacking/defending
                    Iterator iter = defender.getGoodsIterator();
                    if(iter.hasNext()) {
                        //TODO: show CaptureGoodsDialog
                    }
                }

                if (result == ATTACK_WIN) {
                    // TODO: Add damage. For now just move to Europe:
                    defender.moveToEurope();
                } else { // ATTACK_GREAT_WIN:
                    defender.dispose();
                }
            } else if (!defender.isArmed() && defender.getType() != ARTILLERY && defender.getType() != DAMAGED_ARTILLERY) {
                if (defender.isMounted()) {
                    defender.dispose(); // Scouts die if they lose.
                } else {
                    Colony targetcolony = null;
                    boolean captureColony = ((result == ATTACK_DONE_SETTLEMENT) && (newTile.getSettlement() instanceof Colony));
                    if (captureColony) {

                        defender.getOwner().modifyTension(getOwner(), Player.TENSION_ADD_MAJOR);

                        if (getOwner().isEuropean()) {
                            getOwner().modifyGold(plunderGold);

                            try {
                                newTile.getSettlement().getOwner().modifyGold(-plunderGold);
                            } catch (IllegalArgumentException e) {}

                            targetcolony = (Colony)(newTile.getSettlement());
                            targetcolony.setOwner(getOwner()); // This also changes over all of the units...
                            setLocation(newTile);
                            addModelMessage(this, "model.unit.colonyCaptured", new String[][] {{"%colony%", newTile.getColony().getName()}, {"%amount%", Integer.toString(plunderGold)}});
                        } else { // Indian:
                            if (newTile.getSettlement() instanceof Colony && newTile.getColony().getUnitCount() <= 1) {
                                getOwner().modifyGold(plunderGold);
                                newTile.getSettlement().getOwner().modifyGold(-plunderGold);
                                addModelMessage(newTile.getSettlement().getOwner(), "model.unit.colonyBurning", new String[][] {{"%colony%", newTile.getColony().getName()}, {"%amount%", Integer.toString(plunderGold)}});
                                newTile.getSettlement().dispose();
                            } else {
                                addModelMessage(newTile.getSettlement(), "model.unit.colonistSlaughtered", new String[][] {{"%colony%", newTile.getColony().getName()}, {"%unit%", newTile.getColony().getRandomUnit().getName()}});
                                newTile.getColony().getRandomUnit().dispose();
                            }
                        }
                    } else {
                        if (getOwner().isEuropean()) {
                            defender.setLocation(getTile());
                            defender.setOwner(getOwner());
                        } else {
                            defender.dispose();
                        }
                    }
                }
            } else {
                if (defender.isMounted()) {
                    defender.setMounted(false, true);
                    if (getType() == BRAVE && result == ATTACK_GREAT_WIN) {
                        setMounted(true, true);
                    }
                } else if ((defender.getType() == ARTILLERY)) {
                    defender.setType(DAMAGED_ARTILLERY);
                } else if ((defender.getType() == KINGS_REGULAR) || (defender.getType() == DAMAGED_ARTILLERY)) {
                    defender.dispose();
                } else {
                    defender.setArmed(false, true);
                    if (getType() == BRAVE && result == ATTACK_GREAT_WIN) {
                        setArmed(true, true);
                    }
                }
            }

            if (getOwner().hasFather(FoundingFather.GEORGE_WASHINGTON) || result == ATTACK_GREAT_WIN) {
                String oldName = getName();

                if (getType() == PETTY_CRIMINAL) {
                    setType(INDENTURED_SERVANT);
                } else if (getType() == INDENTURED_SERVANT) {
                    setType(FREE_COLONIST);
                } else if (getType() == FREE_COLONIST) {
                    setType(VETERAN_SOLDIER);
                } else if (getType() == VETERAN_SOLDIER && getOwner().getRebellionState() > 1) {
                    setType(COLONIAL_REGULAR);
                }

                if (!getName().equals(oldName)) {
                    addModelMessage(this, "model.unit.unitImproved", new String[][] {{"%oldName%", oldName}, {"%newName%", getName()}});
                }
            }
        } else {
            logger.warning("Illegal result of attack!");
            throw new IllegalArgumentException("Illegal result of attack!");
        }
    }


    /**
    * Gets the Colony this unit is in
    * @return The Colony it's in, or null if it is not in a Colony
    */
    public Colony getColony() {
        if (location == null) {
            return null;
        }

        if (!(location instanceof Colony)) {
            return null;
        } else {
            return (Colony) location;
        }
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

        switch (type) {
            case MASTER_DISTILLER:
                if (goods == Goods.RUM) {
                    base = 6;
                } else {
                    return 3;
                }
                break;
            case MASTER_WEAVER:
                if (goods == Goods.CLOTH) {
                    base = 6;
                } else {
                    base = 3;
                }
                break;
            case MASTER_TOBACCONIST:
                if (goods == Goods.CIGARS) {
                    base = 6;
                } else {
                    base = 3;
                }
                break;
            case MASTER_FUR_TRADER:
                if (goods == Goods.COATS) {
                    base = 6;
                } else {
                    base = 3;
                }
                break;
            case MASTER_BLACKSMITH:
                if (goods == Goods.TOOLS) {
                    base = 6;
                } else {
                    base = 3;
                }
                break;
            case MASTER_GUNSMITH:
                if (goods == Goods.MUSKETS) {
                    base = 6;
                } else {
                    base = 3;
                }
                break;
            case ELDER_STATESMAN:
                if (goods == Goods.BELLS) {
                    base = 6;
                } else {
                    base = 3;
                }
                break;
            case FIREBRAND_PREACHER:
                if (goods == Goods.CROSSES) {
                    base = 6;
                } else {
                    base = 3;
                }
                break;
            case MASTER_CARPENTER:
                if (goods == Goods.HAMMERS) {
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
                if (isColonist()) {
                    base = 3;
                } else {
                    base = 0; // Can't work if you're not a colonist.
                }
        }

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
        switch (type) {
            case EXPERT_FARMER:
                if ((goods == Goods.FOOD) && tile.isLand()) {
                    //TODO: Special tile stuff. He gets +6/+4 for wheat/deer tiles respectively...
                    base += 2;
                }
                break;
            case EXPERT_FISHERMAN:
                if ((goods == Goods.FOOD) && !tile.isLand()) {
                    //TODO: Special tile stuff. He gets +6 for a fishery tile.
                    base += 2;
                }
                break;
            case EXPERT_FUR_TRAPPER:
                if (goods == Goods.FURS) {
                    base *= 2;
                }
                break;
            case EXPERT_SILVER_MINER:
                if (goods == Goods.SILVER) {
                    //TODO: Mountain should be +1, not *2, but mountain didn't exist at type of writing.
                    base *= 2;
                }
                break;
            case EXPERT_LUMBER_JACK:
                if (goods == Goods.LUMBER) {
                    base *= 2;
                }
                break;
            case EXPERT_ORE_MINER:
                if (goods == Goods.ORE) {
                    base *= 2;
                }
                break;
            case MASTER_SUGAR_PLANTER:
                if (goods == Goods.SUGAR) {
                    base *= 2;
                }
                break;
            case MASTER_COTTON_PLANTER:
                if (goods == Goods.COTTON) {
                    base *= 2;
                }
                break;
            case MASTER_TOBACCO_PLANTER:
                if (goods == Goods.TOBACCO) {
                    base *= 2;
                }
                break;
            case INDIAN_CONVERT:
                if ((goods == Goods.FOOD) || (goods == Goods.SUGAR) || (goods == Goods.COTTON) || (goods == Goods.TOBACCO) || (goods == Goods.FURS) || (goods == Goods.ORE || (goods == Goods.SILVER))) {
                    base += 1;
                }
                break;
            default: // Beats me who or what is working here, but he doesn't get a bonus.
                break;
        }

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

        base += getTile().getColony().getProductionBonus();
        return Math.max(base, 1);
    }


    public static int getNextHammers(int type) {
        if (type == WAGON_TRAIN) {
            return 40;
        } else if (type == ARTILLERY) {
            return 192;
        } else if (type == CARAVEL) {
            return 128;
        } else if (type == MERCHANTMAN) {
            return 192;
        } else if (type == GALLEON) {
            return 320;
        } else if (type == PRIVATEER) {
            return 256;
        } else if (type == FRIGATE) {
            return 512;
        } else {
            return -1;
        }
    }

    public static int getNextTools(int type) {
        if (type == WAGON_TRAIN) {
            return 0;
        } else if (type == ARTILLERY) {
            return 40;
        } else if (type == CARAVEL) {
            return 40;
        } else if (type == MERCHANTMAN) {
            return 80;
        } else if (type == GALLEON) {
            return 100;
        } else if (type == PRIVATEER) {
            return 120;
        } else if (type == FRIGATE) {
            return 200;
        } else {
            return -1;
        }
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

        super.dispose();
    }


    /**
    * Prepares the <code>Unit</code> for a new turn.
    */
    public void newTurn() {
        movesLeft = getInitialMovesLeft();
        doAssignedWork();
    }


    /**
    * Make a XML-representation of this object.
    *
    * @param document The document to use when creating new componenets.
    * @return The DOM-element ("Document Object Model") made to represent this "Unit".
    */
    public Element toXMLElement(Player player, Document document, boolean showAll, boolean toSavedGame) {
        Element unitElement = document.createElement(getXMLElementTagName());

        unitElement.setAttribute("ID", getID());
        unitElement.setAttribute("type", Integer.toString(type));
        unitElement.setAttribute("armed", Boolean.toString(armed));
        unitElement.setAttribute("mounted", Boolean.toString(mounted));
        unitElement.setAttribute("missionary", Boolean.toString(missionary));
        unitElement.setAttribute("movesLeft", Integer.toString(movesLeft));
        unitElement.setAttribute("state", Integer.toString(state));
        unitElement.setAttribute("workLeft", Integer.toString(workLeft));
        unitElement.setAttribute("numberOfTools", Integer.toString(numberOfTools));
        unitElement.setAttribute("owner", owner.getID());
        unitElement.setAttribute("turnsOfTraining", Integer.toString(turnsOfTraining));
        unitElement.setAttribute("trainingType", Integer.toString(trainingType));
        unitElement.setAttribute("workType", Integer.toString(workType));
        unitElement.setAttribute("treasureAmount", Integer.toString(treasureAmount));

        if (entryLocation != null) {
            unitElement.setAttribute("entryLocation", entryLocation.getID());
        }

        if (location != null) {
            if (showAll || player == getOwner() || !(location instanceof Building || location instanceof ColonyTile)) {
                unitElement.setAttribute("location", location.getID());
            } else {
                unitElement.setAttribute("location", getTile().getColony().getID());
            }
        }

        // Do not show enemy units hidden in a carrier:
        if (isCarrier()) {
            if (showAll || getOwner().equals(player)) {
                unitElement.appendChild(unitContainer.toXMLElement(player, document, showAll, toSavedGame));
                unitElement.appendChild(goodsContainer.toXMLElement(player, document, showAll, toSavedGame));
            } else {
                UnitContainer emptyUnitContainer = new UnitContainer(getGame(), this);
                emptyUnitContainer.setID(unitContainer.getID());
                unitElement.appendChild(emptyUnitContainer.toXMLElement(player, document, showAll, toSavedGame));
                GoodsContainer emptyGoodsContainer = new GoodsContainer(getGame(), this);
                emptyGoodsContainer.setID(unitContainer.getID());
                unitElement.appendChild(emptyGoodsContainer.toXMLElement(player, document, showAll, toSavedGame));
            }
        }

        return unitElement;
    }


    /**
    * Initialize this object from an XML-representation of this object.
    *
    * @param unitElement The DOM-element ("Document Object Model") made to represent this "Unit".
    */
    public void readFromXMLElement(Element unitElement) {
        setID(unitElement.getAttribute("ID"));

        type = Integer.parseInt(unitElement.getAttribute("type"));
        armed = Boolean.valueOf(unitElement.getAttribute("armed")).booleanValue();
        mounted = Boolean.valueOf(unitElement.getAttribute("mounted")).booleanValue();
        missionary = Boolean.valueOf(unitElement.getAttribute("missionary")).booleanValue();
        movesLeft = Integer.parseInt(unitElement.getAttribute("movesLeft"));
        state = Integer.parseInt(unitElement.getAttribute("state"));
        workLeft = Integer.parseInt(unitElement.getAttribute("workLeft"));
        numberOfTools = Integer.parseInt(unitElement.getAttribute("numberOfTools"));
        owner = (Player) getGame().getFreeColGameObject(unitElement.getAttribute("owner"));
        turnsOfTraining = Integer.parseInt(unitElement.getAttribute("turnsOfTraining"));
        trainingType = Integer.parseInt(unitElement.getAttribute("trainingType"));

        if (unitElement.hasAttribute("treasureAmount")) {
            treasureAmount = Integer.parseInt(unitElement.getAttribute("treasureAmount"));
        } else {
            treasureAmount = 0;
        }

        if (unitElement.hasAttribute("workType")) {
            workType = Integer.parseInt(unitElement.getAttribute("workType"));
        }

        if (owner == null) {
            logger.warning("VERY BAD: Can't find player with ID " + unitElement.getAttribute("owner") + "!");
        }

        if (unitElement.hasAttribute("entryLocation")) {
            entryLocation = (Location) getGame().getFreeColGameObject(unitElement.getAttribute("entryLocation"));
        }

        if (unitElement.hasAttribute("location")) {
            location = (Location) getGame().getFreeColGameObject(unitElement.getAttribute("location"));

            /*
             In this case, 'location == null' can only occur if the location specified in the
             XML-Element does not exists:
            */
            if (location == null) {
                throw new NullPointerException();
            }
        }

        if (isCarrier()) {
            Element unitContainerElement = getChildElement(unitElement, UnitContainer.getXMLElementTagName());
            if (unitContainer != null) {
                unitContainer.readFromXMLElement(unitContainerElement);
            } else {
                unitContainer = new UnitContainer(getGame(), this, unitContainerElement);
            }

            Element goodsContainerElement = getChildElement(unitElement, GoodsContainer.getXMLElementTagName());
            if (goodsContainer != null) {
                goodsContainer.readFromXMLElement(goodsContainerElement);
            } else {
                goodsContainer = new GoodsContainer(getGame(), this, goodsContainerElement);
            }
        }
    }


    /**
    * Gets the tag name of the root element representing this object.
    * @return "unit.
    */
    public static String getXMLElementTagName() {
        return "unit";
    }

}
