
package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Map.Position;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;



/**
* Represents a single tile on the <code>Map</code>.
*
* @see Map
*/
public final class Tile extends FreeColGameObject implements Location {
    private static final Logger logger = Logger.getLogger(Tile.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    // The type of a Tile can be one of the following.
    public static final int UNEXPLORED = 0,
                            PLAINS = 1,
                            GRASSLANDS = 2,
                            PRAIRIE = 3,
                            SAVANNAH = 4,
                            MARSH = 5,
                            SWAMP = 6,
                            DESERT = 7,
                            TUNDRA = 8,
                            ARCTIC = 9,
                            OCEAN = 10,
                            HIGH_SEAS = 11;

    // An addition onto the tile can be one of the following:
    public static final int ADD_NONE = 0,
                            ADD_RIVER_MINOR = 1,
                            ADD_RIVER_MAJOR = 2,
                            ADD_HILLS = 3,
                            ADD_MOUNTAINS = 4;

    // Indians' claims on the tile may be one of the following:
    public static final int CLAIM_NONE = 0,
                            CLAIM_VISITED = 1,
                            CLAIM_CLAIMED = 2;

    private boolean road,
                    plowed,
                    forested,
                    bonus,
                    lostCityRumour;

    private int     type;

    private int     addition_type;

    private int     x,
                    y;

    private int     indianClaim;

    /** The nation that consider this tile to be their land. */
    private int     nationOwner = Player.NO_NATION;

    /** A pointer to the settlement located on this tile or 'null' if there is no settlement on this tile. */
    private Settlement settlement;

    private UnitContainer unitContainer;


    /**
    * Indicates which colony or Indian settlement that owns this tile ('null' indicates no owner).
    * A colony owns the tile it is located on, and every tile with a worker on it.
    * Note that while units and settlements are owned by a player, a tile is owned by a settlement.
    */
    private Settlement owner;

    /**
    * Stores each player's image of this tile. Only initialized when needed.
    */
    private PlayerExploredTile[] playerExploredTiles = null;



    /**
    * Creates a new object with the type <code>UNEXPLORED</code>.
    *
    * @param game The <code>Game</code> this <code>Tile</code> belongs to.
    * @param locX The x-position of this tile on the map.
    * @param locY The y-position of this tile on the map.
    */
    public Tile(Game game, int locX, int locY) {
        this(game, UNEXPLORED, locX, locY);

        if (getGame().getViewOwner() == null) {
            playerExploredTiles = new PlayerExploredTile[Player.NUMBER_OF_NATIONS];
        }
    }


    /**
    * A constructor to use.
    *
    * @param game The <code>Game</code> this <code>Tile</code> belongs to.
    * @param type The type.
    * @param locX The x-position of this tile on the map.
    * @param locY The y-position of this tile on the map.
    */
    public Tile(Game game, int type, int locX, int locY) {
        super(game);

        unitContainer = new UnitContainer(game, this);
        this.type = type;
        this.addition_type = ADD_NONE;
        this.indianClaim = CLAIM_NONE;

        road = false;
        plowed = false;
        forested = false;
        bonus = false;
        lostCityRumour = false;

        x = locX;
        y = locY;

        owner = null;
        settlement = null;

        if (getGame().getViewOwner() == null) {
            playerExploredTiles = new PlayerExploredTile[Player.NUMBER_OF_NATIONS];
        }
    }


    /**
    * Initialize this object from an XML-representation of this object.
    *
    * @param element The DOM-element ("Document Object Model") made to represent this "Tile".
    */
    public Tile(Game game, Element element) {
        super(game, element);

        if (getGame().getViewOwner() == null) {
            playerExploredTiles = new PlayerExploredTile[Player.NUMBER_OF_NATIONS];
        }

        readFromXMLElement(element);
    }


    /**
    * Gets the name of this tile. The value of the name depends
    * on the {@link #getType type}, {@link #isForested forested}
    * and {@link #getAddition addition} of the tile.
    *
    * @return The name as a <code>String</code>.
    */
    public String getName() {
        if (getAddition() == ADD_MOUNTAINS) {
            return Messages.message("mountains");
        }
        else if (getAddition() == ADD_HILLS) {
            return Messages.message("hills");
        }
        else if ( 0 < getType()  &&  getType() < FreeCol.specification.numberOfTileTypes() ) {

            TileType  t = FreeCol.specification.tileType( type );
            return forested ? t.whenForested.name : t.name;
        }

        return "Unknown";
    }


    /**
    * Gets the distance in tiles between this <code>Tile</code>
    * and the specified one.
    *
    * @param tile The <code>Tile</code> to check the distance to.
    * @return Distance
    */
    public int getDistanceTo(Tile tile) {
        return getGame().getMap().getDistance(getPosition(), tile.getPosition());
    }


    public GoodsContainer getGoodsContainer() {
        return null;
    }


    /**
    * Calculates the value of a future colony at this tile.
    * @return The value of a future colony located on this tile.
    *         This value is used by the AI when deciding
    *         where to build a new colony.
    */
    public int getColonyValue() {
        if (!isLand()) {
            return 0;
        } else if (potential(Goods.FOOD) < 2) {
            return 0;
        } else if (getSettlement() != null) {
            return 0;
        } else {
            int value = potential(Goods.FOOD) * 3;

            boolean nearbyTileHasForest = false;
            boolean nearbyTileIsOcean = false;

            List v = getGame().getMap().getSurroundingTiles(this, 1);
            Iterator tileIterator = v.iterator();
            while (tileIterator.hasNext()) {
                Tile tile = (Tile) tileIterator.next();
                if (tile.getColony() != null) {
                    // can't build next to colony
                    return 0;
                } else if (tile.getSettlement() != null) {
                    // can build next to an indian settlement
                    value -= 10;
                } else {

                    if (tile.isLand()) {
                        for (int i = 0; i < Goods.NUMBER_OF_TYPES; i++) {
                            value += tile.potential(i);
                        }
                        if (tile.isForested()) {
                            nearbyTileHasForest = true;
                        }
                    } else {
                        nearbyTileIsOcean = true;
                        value += tile.potential(Goods.FISH);
                    }

                    if (tile.getNationOwner() != Player.NO_NATION) {
                        // tile is already owned by someone
                        if (Player.isEuropean(tile.getNationOwner())) {
                            // TODO: check whether it is owned by us
                            value -= 20;
                        } else {
                            value -= 5;
                        }
                    }
                }
            }

            if (hasBonus()) {
                value -= 10;
            }

            if (isForested()) {
                value -= 5;
            }

            if (!nearbyTileHasForest) {
                value -= 30;
            }
            if (!nearbyTileIsOcean) {
                // TODO: Uncomment when wagon train code has been written:
                //value -= 20;
                value = 0;
            }
            
            return Math.max(0, value);
        }
    }


    /**
    * Gets the <code>Unit</code> that is currently defending this <code>Tile</code>.
    * @param attacker The target that would be attacking this tile.
    * @return The <code>Unit</code> that has been choosen to defend this tile.
    */
    public Unit getDefendingUnit(Unit attacker) {
        Iterator unitIterator = getUnitIterator();

        Unit defender = null;
        if (unitIterator.hasNext()) {
            defender = (Unit) unitIterator.next();
        }

        while (unitIterator.hasNext()) {
            Unit nextUnit = (Unit) unitIterator.next();

            if (nextUnit.getDefensePower(attacker) > defender.getDefensePower(attacker)) {
                defender = nextUnit;
            }
        }

        if (settlement != null) {
            if (defender == null || defender.isColonist() && !defender.isArmed() && !defender.isMounted()) {
                return settlement.getDefendingUnit(attacker);
            }

            return defender;
        }

        return defender;
    }


    /**
    * Returns the cost of moving onto this tile from a given <code>Tile</code>.
    *
    * <br><br>
    *
    * This method does not take special unit behavior into account. Use
    * {@link Unit#getMoveCost} whenever it is possible.
    *
    * @param fromTile The <code>Tile</code> the moving {@link Unit} comes from.
    * @return The cost of moving the unit.
    * @see Unit#getMoveCost
    */
    public int getMoveCost(Tile fromTile) {

        if (hasRoad() && fromTile.hasRoad()) {
            return 1;
        }
        else if (hasRiver() && fromTile.hasRiver()) {
            return 1;
        }
        else if (getSettlement() != null) {
            return 3;
        }
        else if (getAddition() == ADD_MOUNTAINS) {
            return 9;
        }
        else if (getAddition() == ADD_HILLS) {
            return 6;
        }

        TileType  t = FreeCol.specification.tileType( type );
        return forested ? t.whenForested.basicMoveCost : t.basicMoveCost;
    }


    /**
    * Disposes all units on this <code>Tile</code>.
    */
    public void disposeAllUnits() {
        unitContainer.disposeAllUnits();
    }


    /**
    * Gets the first <code>Unit</code> on this tile.
    * @return The first <code>Unit</code> on this tile.
    */
    public Unit getFirstUnit() {
        return unitContainer.getFirstUnit();
    }


    /**
    * Gets the last <code>Unit</code> on this tile.
    * @return The last <code>Unit</code> on this tile.
    */
    public Unit getLastUnit() {
        return unitContainer.getLastUnit();
    }


    /**
    * Returns the total amount of Units at this Location.
    * This also includes units in a carrier
    *
    * @return The total amount of Units at this Location.
    */
    public int getTotalUnitCount() {
        return unitContainer.getTotalUnitCount();
    }


    /**
    * Checks if this <code>Tile</code> contains the specified
    * <code>Locatable</code>.
    *
    * @param locatable The <code>Locatable</code> to test the
    *        presence of.
    * @return <ul>
    *           <li><i>true</i>  if the specified <code>Locatable</code>
    *                            is on this <code>Tile</code> and
    *           <li><i>false</i> otherwise.
    *         </ul>
    */
    public boolean contains(Locatable locatable) {
        if (locatable instanceof Unit) {
            return unitContainer.contains((Unit) locatable);
        }

        logger.warning("Tile.contains(" + locatable + ") Not implemented yet!");

        return false;
    }


    /**
    * Gets the <code>Map</code> in which this <code>Tile</code> belongs.
    * @return The <code>Map</code>.
    */
    public Map getMap() {
        return getGame().getMap();
    }


    /**
     * Check if the tile has been explored.
     * @return true iff tile is known.
     */
    public boolean isExplored() {
        return getType() != UNEXPLORED;
    }


    /**
    * Returns 'true' if this Tile is a land Tile, 'false' otherwise.
    * @return 'true' if this Tile is a land Tile, 'false' otherwise.
    */
    public boolean isLand() {
        return (getType() != OCEAN)  &&  (getType() != HIGH_SEAS);
    }


    /**
    * Returns 'true' if this Tile has a road.
    * @return 'true' if this Tile has a road.
    */
    public boolean hasRoad() {
        return road || (getSettlement() != null);
    }


    public boolean hasRiver() {

        return ADD_RIVER_MINOR == addition_type
                ||  ADD_RIVER_MAJOR == addition_type;
    }


    /**
    * Returns 'true' if this Tile has been plowed.
    * @return 'true' if this Tile has been plowed.
    */
    public boolean isPlowed() {
        return plowed;
    }


    /**
    * Returns 'true' if this Tile is forested.
    * @return 'true' if this Tile is forested.
    */
    public boolean isForested() {
        return forested;
    }


    /**
    * Returns 'true' if this Tile has a bonus (an extra resource) on it.
    * @return 'true' if this Tile has a bonus (an extra resource) on it.
    */
    public boolean hasBonus() {
        return bonus;
    }


    /**
    * Returns the type of this Tile. Returns UNKNOWN if the type of this
    * Tile is unknown.
    *
    * @return The type of this Tile.
    */
    public int getType() {
        return type;
    }


    /**
    * The nation that consider this tile to be their property.
    * @return The nation or {@link Player#NO_NATION} is there is
    *         no nation owning this tile.
    */
    public int getNationOwner() {
        return nationOwner;
    }


    /**
    * Sets the nation that should consider this tile to be their property.
    * @see #getNationOwner
    */
    public void setNationOwner(int nationOwner) {
        this.nationOwner = nationOwner;
    }
    

    /**
    * Makes the given player take the ownership of this <code>Tile</code>.
    * The tension level is modified accordingly.
    */
    public void takeOwnership(Player player) {
        if (getNationOwner() != Player.NO_NATION
                && getNationOwner() != player.getNation()
                && !player.hasFather(FoundingFather.PETER_MINUIT)) {
            Player otherPlayer = getGame().getPlayer(getNationOwner());
            if (otherPlayer != null) {
                if (!otherPlayer.isEuropean()) {
                    otherPlayer.modifyTension(player, Player.TENSION_ADD_TAKE_LAND);
                }
            } else {
                logger.warning("Could not find player with nation: " + getNationOwner());
            }
        }
        setNationOwner(player.getNation());
    }


    /**
    * Returns the addition on this Tile.
    *
    * @return The addition on this Tile.
    */
    public int getAddition() {
        return addition_type;
    }


    /**
    * Sets the addition on this Tile.
    * @param addition The addition on this Tile.
    */
    public void setAddition(int addition) {
        if (addition != ADD_NONE) setForested(false);
        addition_type = addition;
    }


    /**
    * Returns the claim on this Tile.
    *
    * @return The claim on this Tile.
    */
    public int getClaim() {
        return indianClaim;
    }


    /**
    * Sets the claim on this Tile.
    * @param claim The claim on this Tile.
    */
    public void setClaim(int claim) {
        indianClaim = claim;
    }


    /**
    * Puts a <code>Settlement</code> on this <code>Tile</code>.
    * A <code>Tile</code> can only have one <code>Settlement</code>
    * located on it. The <code>Settlement</code> will also become
    * the owner of this <code>Tile</code>.
    *
    * @param s The <code>Settlement</code> that shall be located on
    *          this <code>Tile</code>.
    * @see #getSettlement
    */
    public void setSettlement(Settlement s) {
        settlement = s;
        owner = s;
    }


    /**
    * Gets the <code>Settlement</code> located on this <code>Tile</code>.
    *
    * @return The <code>Settlement</code> that is located on this <code>Tile</code>
    *         or <i>null</i> if no <code>Settlement</code> apply.
    * @see #setSettlement
    */
    public Settlement getSettlement() {
        return settlement;
    }


    /**
    * Gets the <code>Colony</code> located on this <code>Tile</code>.
    * Only a convenience method for {@link #getSettlement}.
    *
    * @return The <code>Colony</code> that is located on this <code>Tile</code>
    *         or <i>null</i> if no <code>Colony</code> apply.
    * @see #getSettlement
    */
    public Colony getColony() {

        if (settlement != null  &&  settlement instanceof Colony) {
            return ((Colony) settlement);
        }

        return null;
    }


    /**
    * Sets the owner of this tile. A <code>Settlement</code> become an
    * owner of a <code>Tile</code> when having workers placed on it.
    *
    * @param owner The Settlement that owns this tile.
    * @see #getOwner
    */
    public void setOwner(Settlement owner) {
        this.owner = owner;
    }


    /**
    * Gets the owner of this tile.
    *
    * @return The Settlement that owns this tile.
    * @see #setOwner
    */
    public Settlement getOwner() {
        return owner;
    }


    /**
    * Sets whether the tile is forested or not.
    * @param value New value for forested.
    */
    public void setForested(boolean value) {
        forested = value;
        bonus = false;
    }

    /**
    * Sets whether the tile is plowed or not.
    * @param value New value.
    */
    public void setPlowed(boolean value) {
        plowed = value;
    }

    /**
    * Sets whether the tile has a road or not.
    * @param value New value.
    */
    public void setRoad(boolean value) {
        road = value;
    }

    /**
    * Sets whether the tile has a bonus or not.
    * @param value New value for bonus
    */
    public void setBonus(boolean value) {
        bonus = value;
    }


    /**
    * Sets the type for this Tile.
    * @param t The new type for this Tile.
    */
    public void setType(int t) {
        if(t < UNEXPLORED || t > HIGH_SEAS)
            throw new IllegalStateException("Tile type must be valid");
        type = t;
        bonus = false;

        if (!isLand()) {
            settlement = null;
            road = false;
            plowed = false;
            forested = false;
            addition_type = ADD_NONE;
        }
    }


    /**
    * Returns the x-coordinate of this Tile.
    * @return The x-coordinate of this Tile.
    */
    public int getX() {
        return x;
    }


    /**
    * Returns the y-coordinate of this Tile.
    * @return The y-coordinate of this Tile.
    */
    public int getY() {
        return y;
    }


    /**
    * Gets the <code>Position</code> of this <code>Tile</code>.
    * @return The <code>Position</code> of this <code>Tile</code>.
    */
    public Position getPosition() {
        return new Position(x, y);
    }

    /**
     * Returns true if there is a lost city rumour on this tile.
     *
     * @return True or false.
     */
    public boolean hasLostCityRumour() {
        return lostCityRumour;
    }

    /**
     * Sets the lost city rumour for this tile.
     *
     * @param rumour If <code>true</code> then this <code>Tile</code>
     * 		will have a lost city rumour. The type of rumour will be
     * 		determined by the server.
     */
    public void setLostCityRumour(boolean rumour) {
        lostCityRumour = rumour;
    }

    
    /**
    * Check if the tile type is suitable for a <code>Settlement</code>, either by a
    * <code>Colony</code> or an <code>IndianSettlement</code>.
    * What are unsuitable tile types are Arctic or water tiles.
    *
    * @return true if tile suitable for settlement
    */
    public boolean isSettleable() {
        int type = getType();

        if (type == ARCTIC || type == OCEAN || type == HIGH_SEAS || getAddition() == ADD_MOUNTAINS) {
            return false;
        }

        return true;
    }


    /**
    * Check to see if this tile can be used to construct a new <code>Colony</code>.
    * If there is a colony here or in a tile next to this one, it is unsuitable
    * for colonization.
    *
    * @return true if tile is suitable for colonization, false otherwise
    */
    public boolean isColonizeable() {
        if (!isSettleable()) {
            return false;
        }

        if (settlement != null) {
            return false;
        }

        for (int direction = Map.N; direction <= Map.NW; direction++) {
            Tile otherTile = getMap().getNeighbourOrNull(direction, this);
            if (otherTile != null) {
                Settlement set = otherTile.getSettlement();
                if((set != null) && (set.getOwner().isEuropean())) {
                    return false;
                }
            }
        }

        return true;
    }


    /**
    * Gets a <code>Unit</code> that can become active. This is preferably
    * a <code>Unit</code> not currently preforming any work.
    *
    * @return A <code>Unit</code> with <code>movesLeft > 0</code> or
    *         <i>null</i> if no such <code>Unit</code> is located on this
    *         <code>Tile</code>.
    */
    public Unit getMovableUnit() {
        if (getFirstUnit() != null) {
            Iterator unitIterator = getUnitIterator();
            while (unitIterator.hasNext()) {
                Unit u = (Unit) unitIterator.next();

                Iterator childUnitIterator = u.getUnitIterator();
                while (childUnitIterator.hasNext()) {
                    Unit childUnit = (Unit) childUnitIterator.next();

                    if ((childUnit.getMovesLeft() > 0) && (childUnit.getState() == Unit.ACTIVE)) {
                        return childUnit;
                    }
                }

                if ((u.getMovesLeft() > 0) && (u.getState() == Unit.ACTIVE)) {
                    return u;
                }
            }
        } else {
            return null;
        }

        Iterator unitIterator = getUnitIterator();
        while (unitIterator.hasNext()) {
            Unit u = (Unit) unitIterator.next();

            Iterator childUnitIterator = u.getUnitIterator();
            while (childUnitIterator.hasNext()) {
                Unit childUnit = (Unit) childUnitIterator.next();

                if ((childUnit.getMovesLeft() > 0)) {
                    return childUnit;
                }
            }

            if (u.getMovesLeft() > 0) {
                return u;
            }
        }

        return null;
    }


    /**
    * Gets the <code>Tile</code> where this <code>Location</code> is located
    * or null if no <code>Tile</code> applies.
    *
    * @return This <code>Tile</code>.
    */
    public Tile getTile() {
        return this;
    }


    /**
    * Adds a <code>Locatable</code> to this Location.
    * @param locatable The <code>Locatable</code> to add to this Location.
    */
    public void add(Locatable locatable) {
        if (locatable instanceof Unit) {
            unitContainer.addUnit((Unit) locatable);
        } else {
            logger.warning("Tried to add an unrecognized 'Locatable' to a tile.");
        }
    }


    /**
    * Removes a <code>Locatable</code> from this Location.
    * @param locatable The <code>Locatable</code> to remove from this Location.
    */
    public void remove(Locatable locatable) {
        if (locatable instanceof Unit) {
            unitContainer.removeUnit((Unit) locatable);
        } else {
            logger.warning("Tried to remove an unrecognized 'Locatable' from a tile.");
        }
    }


    /**
    * Returns the amount of units at this <code>Location</code>.
    * @return The amount of units at this <code>Location</code>.
    */
    public int getUnitCount() {
        /*if (settlement != null) {
            return settlement.getUnitCount() + unitContainer.getUnitCount();
        }*/
        return unitContainer.getUnitCount();
    }


    /**
    * Gets an <code>Iterator</code> of every <code>Unit</code> directly located on this
    * <code>Tile</code>. This does not include <code>Unit</code>s located in a
    * <code>Settlement</code> or on another <code>Unit</code> on this <code>Tile</code>.
    *
    * @return The <code>Iterator</code>.
    */
    public Iterator getUnitIterator() {
        return unitContainer.getUnitIterator();
    }

    /**
    * Gets a clone of the UnitContainer's <code>units</code> array.
    *
    * @return The clone.
    */
    public ArrayList getUnitsClone() {
        return unitContainer.getUnitsClone();
    }

    /**
    * Checks wether or not the specified locatable may be added to this
    * <code>Location</code>.
    *
    * @param locatable The <code>Locatable</code> to test the addabillity of.
    * @return <i>true</i>.
    */
    public boolean canAdd(Locatable locatable) {
        return true;
    }

    /**
    * The potential of this tile to produce a certain type of goods.
    *
    * @param goods The type of goods to check the potential for.
    * @return The normal potential of this tile to produce that amount of goods.
    */
    public int potential(int goods) {
        return getTileTypePotential(getType(), goods, addition_type, hasBonus(), isForested(), isPlowed(), hasRoad());
    }
    
    
    /**
    * Gets the maximum potential for producing the given type of goods.
    * The maximum potential is the potential of a tile after the tile has
    * been plowed/built road on.
    *
    * @param goodsType The type of goods.
    * @return The maximum potential.
    */
    public int getMaximumPotential(int goodsType) {
        if (goodsType == Goods.FURS || goodsType == Goods.LUMBER || goodsType == Goods.ORE || goodsType == Goods.SILVER) {
            return getTileTypePotential(getType(), goodsType, addition_type, hasBonus(),
                                        isForested(), false, true);
        }

        return getTileTypePotential(getType(), goodsType, addition_type, hasBonus() && !isForested(),
                                    false, true, true);
    }

    
    /**
     * Checks wether this <code>Tile</code> can be plowed or not.
     * This method will return <code>false</code> if the tile
     * has already been plowed.
     *  
     * @return The result.
     * @see Unit#canPlow
     */
    public boolean canBePlowed() {
        return (!isPlowed() && isLand() 
                        && getAddition() != Tile.ADD_HILLS
                        && getAddition() != Tile.ADD_MOUNTAINS);
    }    

    
    /**
     * Checks wether this <code>Tile</code> can have a road or not.
     * This method will return <code>false</code> if a road has already
     * been built.
     * 
     * @return The result.
     */
    public boolean canGetRoad() {
        return isLand() && !hasRoad();
    }    
        
     
    /**
    * The potential of a given type of tile to produce a certain type of goods.
    *
    * @param tileType The type of tile
    * @param goods The type of goods to check the potential for.
    * @return The amount of goods.
    */
    public static int getTileTypePotential(int tileType, int goods, int addition_type, boolean bonus,
                                            boolean forested, boolean plowed, boolean road) {
    
        if (!Goods.isFarmedGoods(goods)) {
            return 0;
        }

        // Please someone tell me they want to put this data into a separate file... -sjm
        // Twelve tile types, sixteen goods types, and forested/unforested.
        int potentialtable[][][] = {
        // Food    Sugar  Tobac  Cotton Furs   Wood   Ore    Silver Horses Rum    Cigars Cloth  Coats  T.G.   Tools  Musket
            {{0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}}, // Unexp
            {{5,3}, {0,0}, {0,0}, {2,1}, {0,3}, {0,6}, {1,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}}, // Plains
            {{3,2}, {0,0}, {3,1}, {0,0}, {0,2}, {0,4}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}}, // Grasslands
            {{3,2}, {0,0}, {0,0}, {3,1}, {0,2}, {0,6}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}}, // Prairie
            {{4,3}, {3,1}, {0,0}, {0,0}, {0,2}, {0,4}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}}, // Savannah
            {{3,2}, {0,0}, {0,0}, {0,0}, {0,2}, {0,4}, {2,1}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}}, // Marsh
            {{3,2}, {2,1}, {2,1}, {0,0}, {0,1}, {0,4}, {2,1}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}}, // Swamp
            {{2,2}, {0,0}, {0,0}, {1,1}, {0,2}, {0,2}, {2,1}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}}, // Desert
            {{3,2}, {0,0}, {0,0}, {0,0}, {0,3}, {0,4}, {2,1}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}}, // Tundra
            {{0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}}, // Arctic
            {{4,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}}, // Ocean
            {{4,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}}, // High seas
            {{2,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {4,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}}, // Hills
            {{0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {3,0}, {1,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}}  // Mountains
        };

        int basepotential = 0;
        if (addition_type <= ADD_RIVER_MAJOR) {
            basepotential = potentialtable[tileType][goods][(forested ? 1 : 0)];
        } else if (addition_type == ADD_HILLS) {
            basepotential = potentialtable[12][goods][0];
        } else if (addition_type == ADD_MOUNTAINS) {
            basepotential = potentialtable[13][goods][0];
        }
        if (basepotential > 0) {
            if (plowed && (goods == Goods.FOOD || goods == Goods.SUGAR || goods == Goods.TOBACCO || goods == Goods.COTTON)) {
                basepotential++;
            } else if (road && (goods == Goods.FURS || goods == Goods.LUMBER || goods == Goods.ORE || goods == Goods.SILVER)) {
                basepotential++;
            }
        }

        if (bonus) {
            if (addition_type <= ADD_RIVER_MAJOR) {
                if (forested) {
                    if (tileType == GRASSLANDS || tileType == SAVANNAH) {
                        if (goods == Goods.LUMBER) {
                            basepotential += 6;
                        }
                    } else {
                        if (goods == Goods.FURS) {
                            basepotential += 6;
                        }
                    }
                } else if (tileType == PLAINS && goods == Goods.FOOD) {
                    basepotential += 4;
                } else if (tileType == SAVANNAH && goods == Goods.SUGAR) {
                    basepotential += 7;
                } else if (tileType == GRASSLANDS && goods == Goods.TOBACCO) {
                    basepotential += 6;
                } else if (tileType == PRAIRIE && goods == Goods.COTTON) {
                    basepotential += 6;
                } else if (tileType == OCEAN && goods == Goods.FOOD) {
                    basepotential += 5;
                }
            } else if (addition_type == ADD_HILLS && goods == Goods.ORE) {
                basepotential += 6;
            } else if (addition_type == ADD_MOUNTAINS && goods == Goods.SILVER) {
                basepotential += 2;
            }
        }

        return basepotential;
    }

    /**
    * The type of secondary good this tile produces best (used for Town Commons squares).
    * @return The type of secondary good best produced by this tile.
    */
    public int secondaryGoods() {
        if (isForested()) return Goods.FURS;
        if (getAddition() >= ADD_HILLS) return Goods.ORE;
        switch(getType()) {
            case PLAINS:
            case PRAIRIE:
            case DESERT:
                return Goods.COTTON;
            case SWAMP:
            case GRASSLANDS:
                return Goods.TOBACCO;
            case SAVANNAH:
                return Goods.SUGAR;
            case MARSH:
                return Goods.FURS;
            case TUNDRA:
            case ARCTIC:
            default:
                return Goods.ORE;
        }
    }

    /**
    * The defense/ambush bonus of this tile.
    * @return The defense modifier (in percent) of this tile.
    */
    public int defenseBonus () {

        if (addition_type == ADD_HILLS) {
            return 100;
        }
        else if (addition_type == ADD_MOUNTAINS) {
            return 150;
        }

        TileType  t = FreeCol.specification.tileType( type );
        return forested ? t.whenForested.defenceBonus : t.defenceBonus;
    }

    /**
    * Prepares this <code>Tile</code> for a new turn.
    */
    public void newTurn() {

    }


    /**
    * Make a XML-representation of this object.
    *
    * @param document The document to use when creating new componenets.
    * @return The DOM-element ("Document Object Model") made to represent this "Tile".
    */
    public Element toXMLElement(Player player, Document document, boolean showAll, boolean toSavedGame) {
        // TODO: This method always generates an XML with the latest version of this Tile,
        //       but this method is called for each Tile after loading a game when updating
        //       the map for each client so all players always end up receiving the very
        //       latest version of a Tile after loadGame (even if they didn't visit the Tile
        //       in ages).
        //       To solve this problem we could add an extra toXMLElement method with a parameter
        //       that indicates whether we should use the Tile data in the XML element or the
        //       ExploredTile data of 'player'. The toXMLElement method of IndianSettlement
        //       should also be expanded in the same way and the parameter should be passed on
        //       to that method. (Create new class PlayerExploredIndianSettlement?)
        if (toSavedGame && !showAll) {
            logger.warning("toSavedGame is true, but showAll is false");
        }

        Element tileElement = document.createElement(getXMLElementTagName());

        tileElement.setAttribute("ID", getID());
        tileElement.setAttribute("x", Integer.toString(x));
        tileElement.setAttribute("y", Integer.toString(y));
        tileElement.setAttribute("type", Integer.toString(type));
        if (addition_type != ADD_NONE) {
            tileElement.setAttribute("addition", Integer.toString(addition_type));
        }
        if (road) {
            tileElement.setAttribute("road", Boolean.toString(road));
        }
        if (plowed) {
            tileElement.setAttribute("plowed", Boolean.toString(plowed));
        }
        if (forested) {
            tileElement.setAttribute("forested", Boolean.toString(forested));
        }
        if (bonus) {
            tileElement.setAttribute("bonus", Boolean.toString(bonus));
        }

        if (lostCityRumour) {
            tileElement.setAttribute("lostCityRumour", Boolean.toString(lostCityRumour));
        }
        
        if (nationOwner != Player.NO_NATION && (showAll || player.canSee(this))) {
            tileElement.setAttribute("nationOwner", Integer.toString(nationOwner));
        }

        if ((showAll || player.canSee(this)) && (owner != null)) {
            tileElement.setAttribute("owner", owner.getID());
        }

        //if ((settlement != null) && (showAll || player.canSee(this))) {
        if (settlement != null) {
            tileElement.appendChild(settlement.toXMLElement(player, document, showAll, toSavedGame));
        }

        // Check if the player can see the tile: Do not show enemy units on a tile out-of-sight.
        if (showAll || (player.canSee(this) && (settlement == null || settlement.getOwner() == player))) {
            tileElement.appendChild(unitContainer.toXMLElement(player, document, showAll, toSavedGame));
        } else {
            UnitContainer emptyUnitContainer = new UnitContainer(getGame(), this);
            emptyUnitContainer.setFakeID(unitContainer.getID());
            tileElement.appendChild(emptyUnitContainer.toXMLElement(player, document, showAll, toSavedGame));
        }

        if (!showAll) {
            // We're sending the Tile from the server to the client and showAll is false.
            if (player != null) {
                if (playerExploredTiles[player.getNation()] != null) {
                    playerExploredTiles[player.getNation()].setAttributes(tileElement, player, document);
                }
            } else {
                logger.warning("player == null");
            }
        }

        if (toSavedGame) {
            for (int i=0; i<playerExploredTiles.length; i++) {
                if (playerExploredTiles[i] != null && playerExploredTiles[i].isExplored()) {
                    tileElement.appendChild(playerExploredTiles[i].toXMLElement(player, document, showAll, toSavedGame));
                }
            }
        }

        return tileElement;
    }


    /**
    * Initialize this object from an XML-representation of this object.
    * @param tileElement The DOM-element ("Document Object Model") made to represent this "Tile".
    */
    public void readFromXMLElement(Element tileElement) {
        setID(tileElement.getAttribute("ID"));

        x = Integer.parseInt(tileElement.getAttribute("x"));
        y = Integer.parseInt(tileElement.getAttribute("y"));
        type = Integer.parseInt(tileElement.getAttribute("type"));
        if (tileElement.hasAttribute("addition")) {
            addition_type = Integer.parseInt(tileElement.getAttribute("addition"));
        } else {
            addition_type = ADD_NONE;
        }
        if (tileElement.hasAttribute("road")) {
            road = Boolean.valueOf(tileElement.getAttribute("road")).booleanValue();
        } else {
            road = false;
        }
        if (tileElement.hasAttribute("plowed")) {
            plowed = Boolean.valueOf(tileElement.getAttribute("plowed")).booleanValue();
        } else {
            plowed = false;
        }
        if (tileElement.hasAttribute("forested")) {
            forested = Boolean.valueOf(tileElement.getAttribute("forested")).booleanValue();
        } else {
            forested = false;
        }
        if (tileElement.hasAttribute("bonus")) {
            bonus = Boolean.valueOf(tileElement.getAttribute("bonus")).booleanValue();
        } else {
            bonus = false;
        }

        if (tileElement.hasAttribute("lostCityRumour")) {
            lostCityRumour = Boolean.valueOf(tileElement.getAttribute("lostCityRumour")).booleanValue();
        } else {
            lostCityRumour = false;
        }

        if (tileElement.hasAttribute("nationOwner")) {
            nationOwner = Integer.parseInt(tileElement.getAttribute("nationOwner"));
        } else {
            nationOwner = Player.NO_NATION;
        }

        if (tileElement.hasAttribute("owner")) {
            owner = (Settlement) getGame().getFreeColGameObject(tileElement.getAttribute("owner"));
        }

        Element colonyElement = getChildElement(tileElement, Colony.getXMLElementTagName());
        if (colonyElement != null) {
            if (settlement != null && settlement instanceof Colony) {
                settlement.readFromXMLElement(colonyElement);
            } else {
                settlement = new Colony(getGame(), colonyElement);
            }
        }

        Element indianSettlementElement = getChildElement(tileElement, IndianSettlement.getXMLElementTagName());
        if (indianSettlementElement != null) {
            if (settlement != null && settlement instanceof IndianSettlement) {
                settlement.readFromXMLElement(indianSettlementElement);
            } else {
                settlement = new IndianSettlement(getGame(), indianSettlementElement);
            }
        }

        Element unitContainerElement = getChildElement(tileElement, UnitContainer.getXMLElementTagName());

        if (unitContainerElement == null) {
            throw new NullPointerException();
        }

        if (unitContainer != null) {
            unitContainer.readFromXMLElement(unitContainerElement);
        } else {
            unitContainer = new UnitContainer(getGame(), this, unitContainerElement);
        }

        // Only from a savegame:
        NodeList nl = tileElement.getElementsByTagName("playerExploredTile");
        for (int i=0; i<nl.getLength(); i++) {
            Node node = nl.item(i);
            if (!(node instanceof Element)) {
                continue;
            }
            Element petElement = (Element) node;
            if (Boolean.valueOf(petElement.getAttribute("explored")).booleanValue()) {
                if (playerExploredTiles[Integer.parseInt(petElement.getAttribute("nation"))] == null) {
                    PlayerExploredTile pet = new PlayerExploredTile(petElement);
                    playerExploredTiles[pet.getNation()] = pet;
                } else {
                    playerExploredTiles[Integer.parseInt(petElement.getAttribute("nation"))].readFromXMLElement(petElement);
                }
            }
        }
    }

    /**
    * Returns the tag name of the root element representing this object.
    *
    * @return "tile".
    */
    public static String getXMLElementTagName() {
        return "tile";
    }

    /**
     * Gets the <code>PlayerExploredTile</code> for the given
     * <code>Player</code>.
     *
     * @param player The <code>Player</code>.
     * @see PlayerExploredTile
     */
    private PlayerExploredTile getPlayerExploredTile(Player player) {
        if (playerExploredTiles == null) {
            return null;
        }
        return playerExploredTiles[player.getNation()];
    }

    /**
     * Creates a <code>PlayerExploredTile</code> for the given
     * <code>Player</code>.
     *
     * @param player The <code>Player</code>.
     * @see PlayerExploredTile
     */
    private void createPlayerExploredTile(Player player) {
        playerExploredTiles[player.getNation()] = new PlayerExploredTile(player.getNation());
    }

    /**
     * Updates the information about this <code>Tile</code> for the
     * given <code>Player</code>.
     *
     * @param player The <code>Player</code>.
     */
    public void updatePlayerExploredTile(Player player) {
        updatePlayerExploredTile(player.getNation());
    }

    /**
     * Updates the information about this <code>Tile</code> for the
     * given <code>Player</code>.
     *
     * @param nation The {@link Player#getNation nation} identifying the
     *      <code>Player</code>.
     */
    public void updatePlayerExploredTile(int nation) {
        if (playerExploredTiles == null || getGame().getViewOwner() != null) {
            return;
        }
        if (playerExploredTiles[nation] == null && !Player.isEuropean(nation)) {
            return;
        }
        if (playerExploredTiles[nation] == null) {
            logger.warning("'playerExploredTiles' for " + Player.getNationAsString(nation) + " is 'null'.");
            return;
        }

        playerExploredTiles[nation].setRoad(road);
        playerExploredTiles[nation].setPlowed(plowed);
        playerExploredTiles[nation].setForested(forested);
        playerExploredTiles[nation].setBonus(bonus);
        playerExploredTiles[nation].setLostCityRumour(lostCityRumour);
        
        if (getColony() != null) {
            playerExploredTiles[nation].setColonyUnitCount(getSettlement().getUnitCount());
            playerExploredTiles[nation].setColonyStockadeLevel(getColony().getBuilding(Building.STOCKADE).getLevel());
        } else if (getSettlement() != null) {
            playerExploredTiles[nation].setMissionary(((IndianSettlement) getSettlement()).getMissionary());

            /*
             * These attributes should not be updated by this method:
             * skill, highlyWantedGoods, wantedGoods1 and wantedGoods2
             */
        }
    }

    /**
     * Checks if this <code>Tile</code> has been explored by the given
     * <code>Player</code>.
     *
     * @param player The <code>Player</code>.
     * @return <code>true</code> if this <code>Tile</code> has been
     *      explored by the given <code>Player</code> and
     *      <code>false</code> otherwise.
     */
    public boolean isExploredBy(Player player) {
        if (player.isIndian()) {
            return true;
        }
        if (playerExploredTiles[player.getNation()] == null || !isExplored()) {
            return false;
        }

        return getPlayerExploredTile(player).isExplored();
    }

    /**
     * Sets this <code>Tile</code> to be explored by the given
     * <code>Player</code>.
     *
     * @param player The <code>Player</code>.
     * @param explored <code>true</code> if this <code>Tile</code>
     *      should be explored by the given <code>Player</code>
     *      and <code>false</code> otherwise.
     */    
    public void setExploredBy(Player player, boolean explored) {
        if (player.isIndian()) {
            return;
        }
        if (playerExploredTiles[player.getNation()] == null) {
            createPlayerExploredTile(player);
        }
        getPlayerExploredTile(player).setExplored(explored);
        updatePlayerExploredTile(player);
    }

    /**
     * Updates the skill available from the <code>IndianSettlement</code> located
     * on this <code>Tile</code>.
     * <p>
     * @param player The <code>Player</code> which should get the updated
     *      information.
     * @exception NullPointerException If there is no settlement on this
     *      <code>Tile</code>.
     * @exception ClassCastException If the <code>Settlement</code> on this
     *      <code>Tile</code> is not an <code>IndianSettlement</code>.
     * @see IndianSettlement
     */
    public void updateIndianSettlementSkill(Player player) {
        IndianSettlement is = (IndianSettlement) getSettlement();
        getPlayerExploredTile(player).setSkill(is.getLearnableSkill());
    }

    /**
     * Updates the information about the <code>IndianSettlement</code> located
     * on this <code>Tile</code>.
     * <p>
     * @param player The <code>Player</code> which should get the updated
     *      information.
     * @exception NullPointerException If there is no settlement on this
     *      <code>Tile</code>.
     * @exception ClassCastException If the <code>Settlement</code> on this
     *      <code>Tile</code> is not an <code>IndianSettlement</code>.
     * @see IndianSettlement
     */
    public void updateIndianSettlementInformation(Player player) {
        if (player.isIndian()) {
            return;
        }
        PlayerExploredTile playerExploredTile = getPlayerExploredTile(player);
        IndianSettlement is = (IndianSettlement) getSettlement();
        playerExploredTile.setSkill(is.getLearnableSkill());
        playerExploredTile.setHighlyWantedGoods(is.getHighlyWantedGoods());
        playerExploredTile.setWantedGoods1(is.getWantedGoods1());
        playerExploredTile.setWantedGoods2(is.getWantedGoods2());
    }


    /**
    * This class contains the data visible to a specific player.
    *
    * <br><br>
    *
    * Sometimes a tile contains information that should not be given to
    * a player. For instance; a settlement that was built after the player
    * last viewed the tile.
    *
    * <br><br>
    *
    * The <code>toXMLElement</code> of {@link Tile} uses {@link #setAttributes}
    * in this class to hide information that is not available.
    */
    public class PlayerExploredTile {

        private int nation;
        private boolean explored = false;

        // Tile data:
        private boolean road,
                        plowed,
                        forested,
                        bonus;

        // Colony data:
        private int     colonyUnitCount = 0,
                        colonyStockadeLevel;

        // IndianSettlement data:
        private int     skill = IndianSettlement.UNKNOWN,
                        highlyWantedGoods = IndianSettlement.UNKNOWN,
                        wantedGoods1 = IndianSettlement.UNKNOWN,
                        wantedGoods2 = IndianSettlement.UNKNOWN;
        private Unit    missionary = null;

        //private Settlement settlement;

        private boolean lostCityRumour;

        /**
        * Creates a new <code>PlayerExploredTile</code>.
        */
        public PlayerExploredTile(int nation) {
            this.nation = nation;
        }


        /**
        * Initialize this object from an XML-representation of this object.
        * @param element The DOM-element ("Document Object Model") made to represent this "PlayerExploredTile".
        */
        public PlayerExploredTile(Element element) {
            readFromXMLElement(element);
        }


        public void setColonyUnitCount(int colonyUnitCount) {
            this.colonyUnitCount = colonyUnitCount;
        }


        public void setColonyStockadeLevel(int colonyStockadeLevel) {
            this.colonyStockadeLevel = colonyStockadeLevel;
        }


        public void setRoad(boolean road) {
            this.road = road;
        }


        public void setPlowed(boolean plowed) {
            this.plowed = plowed;
        }


        public void setForested(boolean forested) {
            this.forested = forested;
        }


        public void setBonus(boolean bonus) {
            this.bonus = bonus;
        }

        public void setLostCityRumour(boolean lostCityRumour) {
            this.lostCityRumour = lostCityRumour;
        }

        public void setExplored(boolean explored) {
            this.explored = explored;
        }


        public void setSkill(int newSkill) {
            this.skill = newSkill;
        }


        public void setHighlyWantedGoods(int highlyWantedGoods) {
            this.highlyWantedGoods = highlyWantedGoods;
        }


        public void setWantedGoods1(int wantedGoods1) {
            this.wantedGoods1 = wantedGoods1;
        }


        public void setWantedGoods2(int wantedGoods1) {
            this.wantedGoods1 = wantedGoods1;
        }


        public void setMissionary(Unit missionary) {
            this.missionary = missionary;
        }


        /**
        * Returns <i>true</i> if the tile has been explored.
        */
        public boolean isExplored() {
            return explored;
        }


        /**
        * Returns the nation of this <code>PlayerExploredTile</code>.
        */
        public int getNation() {
            return nation;
        }


        /**
        * Hides the invisible features of the given <code>tileElement</code>.
        */
        public void setAttributes(Element tileElement, Player player, Document document) {
            tileElement.setAttribute("road", Boolean.toString(road));
            tileElement.setAttribute("plowed", Boolean.toString(plowed));
            tileElement.setAttribute("forested", Boolean.toString(forested));
            tileElement.setAttribute("bonus", Boolean.toString(bonus));
            tileElement.setAttribute("lostCityRumour", Boolean.toString(lostCityRumour));

            if (getColony() != null) {
                Element colonyElement = getChildElement(tileElement, Colony.getXMLElementTagName());

                if (!player.canSee(getTile())) {
                    if (colonyUnitCount != 0) {
                        if (colonyElement.hasAttribute("unitCount")) {
                            colonyElement.setAttribute("unitCount", Integer.toString(colonyUnitCount));
                        }

                        NodeList childNodes = colonyElement.getChildNodes();
                        for (int i=0; i<childNodes.getLength(); i++) {
                            Element childElement = (Element) childNodes.item(i);

                            if (childElement.getTagName().equals(Building.getXMLElementTagName())) {
                                Building b = (Building) getGame().getFreeColGameObject(childElement.getAttribute("ID"));

                                if (b.getType() == Building.STOCKADE) {
                                    childElement.setAttribute("level", Integer.toString(colonyStockadeLevel));
                                }
                            }
                        }
                    } else {    // Colony not discovered.
                        colonyElement.getParentNode().removeChild(colonyElement);
                    }
                }
            } else if ((getSettlement() != null) && (getSettlement() instanceof IndianSettlement)) {
                Element settlementElement = getChildElement(tileElement, IndianSettlement.getXMLElementTagName());

                settlementElement.setAttribute("learnableSkill", Integer.toString(skill));
                settlementElement.setAttribute("highlyWantedGoods", Integer.toString(highlyWantedGoods));
                settlementElement.setAttribute("wantedGoods1", Integer.toString(wantedGoods1));
                settlementElement.setAttribute("wantedGoods2", Integer.toString(wantedGoods2));

                Element missionaryElement = getChildElement(settlementElement, "missionary");
                if (missionaryElement != null) {
                    settlementElement.removeChild(missionaryElement);
                }
                if (missionary != null) {
                    missionaryElement = document.createElement("missionary");
                    missionaryElement.appendChild(missionary.toXMLElement(player, document, false, false));
                    settlementElement.appendChild(missionaryElement);
                }
            } else if (getSettlement() != null) {
                logger.warning("Unknown type of settlement: " + getSettlement());
            }
        }


        /**
        * Make a XML-representation of this object.
        *
        * @param document The document to use when creating new componenets.
        * @return The DOM-element ("Document Object Model") made to represent this "PlayerExploredTile".
        */
        public Element toXMLElement(Player player, Document document, boolean showAll, boolean toSavedGame) {
            Element tileElement = document.createElement("playerExploredTile");

            tileElement.setAttribute("explored", Boolean.toString(explored));
            tileElement.setAttribute("nation", Integer.toString(nation));

            tileElement.setAttribute("road", Boolean.toString(road));
            tileElement.setAttribute("plowed", Boolean.toString(plowed));
            tileElement.setAttribute("forested", Boolean.toString(forested));
            tileElement.setAttribute("bonus", Boolean.toString(bonus));
            tileElement.setAttribute("lostCityRumour", Boolean.toString(lostCityRumour));

            tileElement.setAttribute("colonyUnitCount", Integer.toString(colonyUnitCount));
            tileElement.setAttribute("colonyStockadeLevel", Integer.toString(colonyStockadeLevel));
            tileElement.setAttribute("learnableSkill", Integer.toString(skill));
            tileElement.setAttribute("highlyWantedGoods", Integer.toString(highlyWantedGoods));
            tileElement.setAttribute("wantedGoods1", Integer.toString(wantedGoods1));
            tileElement.setAttribute("wantedGoods2", Integer.toString(wantedGoods2));

            if (missionary != null) {
                Element missionaryElement = document.createElement("missionary");
                missionaryElement.appendChild(missionary.toXMLElement(player, document, false, false));
                tileElement.appendChild(missionaryElement);
            }

            return tileElement;
        }


        /**
        * Initialize this object from an XML-representation of this object.
        * @param tileElement The DOM-element ("Document Object Model") made to represent this "PlayerExploredTile".
        */
        public void readFromXMLElement(Element tileElement) {
            explored = Boolean.valueOf(tileElement.getAttribute("explored")).booleanValue();
            nation = Integer.parseInt(tileElement.getAttribute("nation"));

            road = Boolean.valueOf(tileElement.getAttribute("road")).booleanValue();
            plowed = Boolean.valueOf(tileElement.getAttribute("plowed")).booleanValue();
            forested = Boolean.valueOf(tileElement.getAttribute("forested")).booleanValue();
            bonus = Boolean.valueOf(tileElement.getAttribute("bonus")).booleanValue();
            lostCityRumour = Boolean.valueOf(tileElement.getAttribute("lostCityRumour")).booleanValue();

            colonyUnitCount = Integer.parseInt(tileElement.getAttribute("colonyUnitCount"));
            colonyStockadeLevel = Integer.parseInt(tileElement.getAttribute("colonyStockadeLevel"));

            if (tileElement.hasAttribute("learnableSkill")) {
                skill = Integer.parseInt(tileElement.getAttribute("learnableSkill"));
            } else { // Support for pre-0.1.0 protocols:
                skill = IndianSettlement.UNKNOWN;
            }

            if (tileElement.hasAttribute("highlyWantedGoods")) {
                highlyWantedGoods = Integer.parseInt(tileElement.getAttribute("highlyWantedGoods"));
                wantedGoods1 = Integer.parseInt(tileElement.getAttribute("wantedGoods1"));
                wantedGoods2 = Integer.parseInt(tileElement.getAttribute("wantedGoods2"));
            } else { // Support for pre-0.1.0 protocols:
                highlyWantedGoods = IndianSettlement.UNKNOWN;
                wantedGoods1 = IndianSettlement.UNKNOWN;
                wantedGoods2 = IndianSettlement.UNKNOWN;
            }

            Element missionaryElement = getChildElement(tileElement, "missionary");
            if (missionaryElement != null) {
                if (missionary == null) {
                    missionary = new Unit(getGame(), getChildElement(missionaryElement, Unit.getXMLElementTagName()));
                } else {
                    missionary.readFromXMLElement(getChildElement(missionaryElement, Unit.getXMLElementTagName()));
                }
            } else {
                missionary = null;
            }
        }


        /**
        * Returns the tag name of the root element representing this object.
        * @return "playerExploredTile".
        */
        /*public static String getXMLElementTagName() {
            return "playerExploredTile";
        }*/
    }
}
