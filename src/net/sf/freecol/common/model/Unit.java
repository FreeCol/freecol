
package net.sf.freecol.common.model;

import java.util.Vector;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.FreeColException;

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
public final class Unit extends FreeColGameObject implements Location, Locatable {
    public static final String  COPYRIGHT = "Copyright (C) 2003 The FreeCol Team";
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


    public static final int MOVE = 0,
                            MOVE_HIGH_SEAS = 1,
                            ATTACK = 2,
                            EMBARK = 3,
                            DISEMBARK = 4,
                            ILLEGAL_MOVE = 5;



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

    private int             workType; // What type of goods this unit produces in its occupation.

    /**
    * Initiate a new <code>Unit</code> of a specified type with the state set
    * to {@link #ACTIVE} if a carrier and {@link #SENTRY} otherwise. The
    * {@link Location} is set to <i>null</i>.
    *
    * @param The <code>Game</code> in which this <code>Unit</code> belong.
    * @param owner The Player owning the unit.
    * @param type The type of the unit.
    */
    /*public Unit(Game game, Player owner, int type, int movesLeft, int s) {
        this(game, null, owner, type, movesLeft, s);
    }*/

    public Unit(Game game, Player owner, int type) {
        this(game, null, owner, type, isCarrier(type)?ACTIVE:SENTRY);
    }

    /**
    * Initiate a new <code>Unit</code> with the specified parameters.
    *
    * @param The <code>Game</code> in which this <code>Unit</code> belong.
    * @param location The <code>Location/code> to place this <code>Unit</code> upon.
    * @param owner The <code>Player</code> owning this unit.
    * @param type The type of the unit.
    * @param s The initial state for this Unit (one of {@link #ACTIVE}, {@link #FORTIFY}...).
    */
    public Unit(Game game, Location location, Player owner, int type, int s) {
        super(game);

        unitContainer = new UnitContainer(game, this);
        goodsContainer = new GoodsContainer(game, this);

        setLocation(location);

        this.owner = owner;
        this.type = type;
        this.movesLeft = getInitialMovesLeft();

        this.armed = armed;
        this.mounted = mounted;
        this.missionary = missionary;
        this.numberOfTools = numberOfTools;

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
    }
    



    /**
    * The constructor to use.
    * @param location The Location the unit is created at.
    * @param owner The Player owning the unit.
    * @param type The type of the unit.
    * @param movesLeft The amount of moves this Unit has left.
    * @param s The initial state for this Unit (one of
    * {ACTIVE, FORTIFY, ...}).
    * @param id The unique identifier of this Unit for easy
    * referencing when communicating with the server.
    */
    /*public Unit(Game game, Location location, Player owner, int type, int movesLeft, int s, int id) {
        super(game);

        unitContainer = new UnitContainer(game, this);        

        this.location = location;
        location.add(this);
        this.owner = owner;
//        owner.addElement(this);
        this.type = type;
        this.movesLeft = movesLeft;
        //this.id = id;
        
        this.armed = armed;
        this.mounted = mounted;
        this.missionary = missionary;
        this.numberOfTools = numberOfTools;
        
        location = null;
        
        state = s;
        workLeft = -1;

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
    }*/




    /**
    * Initialize this object from an XML-representation of this object.
    *
    * @param The <code>Game</code> in which this <code>Unit</code> belong.
    * @param element The DOM-element ("Document Object Model") made to represent this "Unit".
    */
    public Unit(Game game, Element element) {
        super(game, element);

        //unitContainer = new UnitContainer(game, this);
        readFromXMLElement(element);
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
     * Gets the type of goods this unit is producing in its current occupation.
     * @return The type of goods this unit would produce.
     */
    public int getWorkType() {
        if (getLocation() instanceof Building) {
	  // TODO: code me.
	  return 0;
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

        if (target == null || target.getType() == Tile.HIGH_SEAS) {
            return isNaval() ? MOVE_HIGH_SEAS : ILLEGAL_MOVE;
        }

        // Check for an 'attack' instead of 'move'.
        if ((target.getUnitCount() > 0) && (target.getDefendingUnit().getNation() != getNation())
            && ((target.isLand() && !isNaval()) || (isNaval() && !target.isLand()))) {
            return ATTACK;
        }

        // Check for disembark.
        if (isNaval() && target.isLand()) {
            if (target.getSettlement() != null && target.getSettlement().getOwner() == getOwner()) {
                return MOVE;
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
            throw new IllegalStateException("Illegal move requested!");
        }

        setState(ACTIVE);
        setStateToAllChildren(SENTRY);

        Tile newTile = getGame().getMap().getNeighbourOrNull(direction, getTile());

        if (newTile != null) {
            setLocation(newTile);
        } else {
            throw new IllegalStateException("Illegal move requested!");
        }

        movesLeft--;
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
        movesLeft--;
    }


    /**
    * Boards a carrier that is on the same tile.
    *
    * @param carrier The carrier this unit shall embark.
    * @exception IllegalStateException If the carrier is on another tile than this unit.
    */
    public void boardShip(Unit carrier) {
        if (getTile() == carrier.getTile()) {
            setLocation(carrier);
        } else {
            throw new IllegalStateException("It is not allowed to board a ship on another tile.");
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
        Location l = ((Unit) getLocation()).getLocation();

        if (l instanceof Europe) {
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
            // TODO: Check if there is space for a new Locatable.

            if (locatable instanceof Unit) {
                unitContainer.addUnit((Unit) locatable);
            } else if (locatable instanceof Goods) {
                goodsContainer.addGoods((Goods) locatable);
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
            return getSpaceLeft() >= locatable.getTakeSpace();
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
    * Gets the storage room of all Goods at this location.
    * @return The total storage room of all Goods at this location.
    */
    public int getGoodsSpace() {
        if (isCarrier()) {
	    int space = 0;
            Iterator goodsIterator = goodsContainer.getGoodsIterator();
	    while (goodsIterator.hasNext()) {
	      space += ((Goods) goodsIterator.next()).getTakeSpace();
	    }
	    return space;
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
        if (isCarrier()) {
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

        if (newLocation != null) {
            newLocation.add(this);
        }

        location = newLocation;
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
    }


    /**
    * Sets the armed attribute of this unit.
    * @param <i>true</i> if this unit should be armed and <i>false</i> otherwise.
    */
    public void setArmed(boolean b) {
        armed = b;
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
    */
    public void setMounted(boolean b) {
        mounted = b;
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
    * Sets how many tools this unit is carrying.
    * @param numberOfTools The number to set it to.
    */
    public void setNumberOfTools(int numberOfTools) {
        this.numberOfTools = numberOfTools;
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
        if ((type == CARAVEL) || (type == GALLEON) || (type == FRIGATE) || (type == MAN_O_WAR) || (type == MERCHANTMAN) || (type == PRIVATEER)) {
            return true;
        }
        else {
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
    * Returns the name of a unit type in a human readable format. The return
    * value can be used when communicating with the user.
    *
    * @return The given unit type as a String
    * @throws FreeColException
    */
    public static String getName(int someType) throws FreeColException {
        // TODO: Use i18n:

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
                return "Kings Regular";
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
                throw new FreeColException("Unit has an invalid type.");
        }
    }


    /**
    * Returns the name of this unit in a human readable format. The return
    * value can be used when communicating with the user.
    *
    * @return The type of this Unit as a String
    * @throws FreeColException
    */
    public String getName() throws FreeColException {
        return Unit.getName(getType());
    }


    /**
    * Gets the amount of moves this unit has at the beginning of each turn.
    * @return The amount of moves this unit has at the beginning of each turn.
    */
    public int getInitialMovesLeft() {
        if (isNaval()) {
            switch (getType()) {
                case CARAVEL:
                    return 4;
                case FRIGATE:
                    return 6;
                case GALLEON:
                    return 6;
                case MAN_O_WAR:
                    return 6;
                case MERCHANTMAN:
                    return 5;
                case PRIVATEER:
                    return 8;
                default:
                    logger.warning("Unit.getInitialMovesLeft(): Unit has invalid naval type.");
                    return 3;
            }
        } else {
            if (isMounted()) {
                return 4;
            } else if (isMissionary()) {
                return 2;
            } else if (getType() == WAGON_TRAIN) {
                return 2;
            } else {
                return 1;
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
        String result;
        try {
            result = getName();
        } catch (FreeColException e) {
            result = "<ERROR>";
            e.printStackTrace();
        }

        result += " " + movesLeft + "/" + getInitialMovesLeft();

        // TODO: add armed & mounted info.

        return result;
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
        // For now everything takes forever(=-1) turn to complete (build road, ...).
        // This should change according to the terrain etc. !!
        // TODO
        switch (s) {
            case ACTIVE:
                workLeft = -1;
                break;
            case SENTRY:
                workLeft = -1;
                break;
            case FORTIFY:
                workLeft = -1;
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
        }

        // Do the HARD work of moving this unit to europe:
	if (!(getLocation() instanceof Europe)) // Perpetual europedom bug fixed.. -sjm
          setEntryLocation(getLocation());
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
                if (!getTile().isLand()) {
                    return true;
                }
                else {
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
                        || (getTile().getType() == Tile.HIGH_SEAS)) {
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
        return getInitialSpaceLeft() - (getUnitCount() + getGoodsSpace());
    }


    /**
    * Returns the amount of units/cargo that this unit can carry.
    * @return The amount of units/cargo that this unit can carry.
    */
    private int getInitialSpaceLeft() {
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
                    case TO_EUROPE:     break;
                    case TO_AMERICA:    setLocation(getEntryLocation()); break;
                    case BUILD_ROAD:    getTile().setRoad(true); numberOfTools -= 20; break;
		    case PLOW:
		        if (getTile().isForested()) {
			    getTile().setForested(false);
			} else {
			    getTile().setPlowed(true);
			}
			numberOfTools -= 20;
			break;
                    default:            logger.warning("Unkown work completed. State: " + state);
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
    */
    public Location getEntryLocation() {
        if (entryLocation != null) {
            return entryLocation;
        } else {
            return getOwner().getEntryLocation();
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
    * Generates a random unit type. The unit type that is returned represents
    * the type of a unit that is recruitable in Europe.
    * @return A random unit type of a unit that is recruitable in Europe.
    */
    public static int generateRecruitable() {
        // There MUST be a better way to do this.
        int random = (int)(Math.random() * UNIT_COUNT);
        while (!Unit.isRecruitable(random)) {
            random = (int)(Math.random() * UNIT_COUNT);
        }
        return random;
    }


    /**
    * Returns the price of this unit in Europe.
    *
    * @return The price of this unit when trained in Europe. '-1' is returned in case the unit cannot be bought.
    */
    public int getPrice() {
        return getPrice(getType());
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
                return 500;
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
    * @return The current defensive power of this unit.
    */
    public int getDefensePower() {
        // Each unit in the game defends with power 5.
        // TODO: write this method
        return 5;
    }


    /**
    * Returns the current offensive power of this unit.
    * @return The current offensive power of this unit.
    */
    public int getOffensePower() {
        // TODO: write this method

        // For now we'll just return '1' for the units that can attack and '0' for the ones that can't attack.
        if (isCarrier()) {
            if ((getType() == FRIGATE) || (getType() == MAN_O_WAR) || (getType() == PRIVATEER)) {
                return 1;
            }
            else {
                return 0;
            }
        }
        else {
            if (isArmed() || isMounted()) {
                return 1;
            }
            else {
                return 0;
            }
        }
    }


    /**
    * Gets the Colony this unit is in
    * @return The Colony it's in, or null if it is not in a Colony
    */
    public Colony getColony() {
        if (!(location instanceof Colony)) {
            return null;
        } else {
            return (Colony) location;
        }
    }
    
    /**
    * Given a type of goods to produce in a building, returns the unit's potential to do so.
    * @param goods The type of goods to be produced.
    * @return The potential amount of goods to be manufactured.
    */
    public int getProducedAmount(int goods)
    {
        switch (type) {
            case MASTER_DISTILLER:
                if (goods == Goods.RUM) return 6;
                return 3;
            case MASTER_WEAVER:
                if (goods == Goods.CLOTH) return 6;
                return 3;
            case MASTER_TOBACCONIST:
                if (goods == Goods.CIGARS) return 6;
                return 3;
            case MASTER_FUR_TRADER:
                if (goods == Goods.COATS) return 6;
                return 3;
            case MASTER_BLACKSMITH:
                if (goods == Goods.TOOLS) return 6;
                return 3;
            case MASTER_GUNSMITH:
                if (goods == Goods.MUSKETS) return 6;
                return 3;
            case INDENTURED_SERVANT:
                return 2;
            case PETTY_CRIMINAL:
            case INDIAN_CONVERT:
                return 1;
            default: // Beats me who or what is working here, but he doesn't get a bonus.
                if (isColonist())
                    return 3; 
                else
                    return 0; // Can't work if you're not a colonist.
        }
    }

    /**
    * Given a type of goods to produce in the field and a tile, returns the unit's potential to produce goods.
    * @param goods The type of goods to be produced.
    * @param tile The tile which is being worked.
    * @return The potential amount of goods to be farmed.
    */
    public int getFarmedPotential(int goods, Tile tile)
    {
        int base = tile.potential(goods);
        switch (type) {
            case EXPERT_FARMER:
                if ((goods == Goods.FOOD) && ((tile.getType() != Tile.OCEAN) && (tile.getType() != Tile.HIGH_SEAS))) {
                    //TODO: Special tile stuff. He gets +6/+4 for wheat/deer tiles respectively...
                    base += 2;
                }
                break;
            case EXPERT_FISHERMAN:
                if ((goods == Goods.FOOD) && ((tile.getType() == Tile.OCEAN) || (tile.getType() == Tile.HIGH_SEAS))) {
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
                if ((goods == Goods.FOOD) || (goods == Goods.SUGAR) || (goods == Goods.COTTON) || (goods == Goods.TOBACCO) || (goods == Goods.FURS)) {
                    base += 1;
                }
                break;
            default: // Beats me who or what is working here, but he doesn't get a bonus.
                break;
        }
	return base;
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
    public Element toXMLElement(Player player, Document document) {
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
        
        if (entryLocation != null) {
            unitElement.setAttribute("entryLocation", entryLocation.getID());
        }

        if (location != null) {
            unitElement.setAttribute("location", location.getID());
        }

        // Do not show enemy units hidden in a carrier:
        if (isCarrier()) {
            if (getOwner().equals(player)) {
                unitElement.appendChild(unitContainer.toXMLElement(player, document));
                unitElement.appendChild(goodsContainer.toXMLElement(player, document));
            } else {
                UnitContainer emptyUnitContainer = new UnitContainer(getGame(), this);
                emptyUnitContainer.setID(unitContainer.getID());
                unitElement.appendChild(emptyUnitContainer.toXMLElement(player, document));
                GoodsContainer emptyGoodsContainer = new GoodsContainer(getGame(), this);
                emptyGoodsContainer.setID(unitContainer.getID());
                unitElement.appendChild(emptyGoodsContainer.toXMLElement(player, document));
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
        owner = getGame().getPlayerByID(unitElement.getAttribute("owner"));
        
        if (unitElement.hasAttribute("entryLocation")) {
            entryLocation = (Location) getGame().getFreeColGameObject(unitElement.getAttribute("entryLocation"));
        }

        if (unitElement.hasAttribute("location")) {
            location = (Location) getGame().getFreeColGameObject(unitElement.getAttribute("location"));
        }
        
        if (isCarrier()) {
            Element unitContainerElement = (Element) unitElement.getElementsByTagName(UnitContainer.getXMLElementTagName()).item(0);
            if (unitContainer != null) {
                unitContainer.readFromXMLElement(unitContainerElement);
            } else {
                unitContainer = new UnitContainer(getGame(), this, unitContainerElement);
            }
            Element goodsContainerElement = (Element) unitElement.getElementsByTagName(GoodsContainer.getXMLElementTagName()).item(0);
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
