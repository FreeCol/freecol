/*
 *  Tile.java - A tile from the map.
 */

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.Vector;
import java.util.Iterator;
import java.util.logging.Logger;

import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.FreeColException;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;



/**
* Represents a single tile on the <code>Map</code>.
*
* @see Map
*/
public final class Tile extends FreeColGameObject implements Location {
    private static final Logger logger = Logger.getLogger(Tile.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003 The FreeCol Team";
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
                    bonus;

    private int     type;
    
    private int     addition_type;

    private int     x,
                    y;
                    
    private int     indianClaim;

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
    * Creates a new object with the type <code>UNEXPLORED</code>.
    *
    * @param game The <code>Game</code> this <code>Tile</code> belongs to.
    * @param locX The x-position of this tile on the map.
    * @param locY The y-position of this tile on the map.
    */
    public Tile(Game game, int locX, int locY) {
        this(game, UNEXPLORED, locX, locY);
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

        x = locX;
        y = locY;

        owner = null;
        settlement = null;
    }


    /**
    * Initialize this object from an XML-representation of this object.
    *
    * @param element The DOM-element ("Document Object Model") made to represent this "Tile".
    */
    public Tile(Game game, Element element) {
        super(game, element);
        readFromXMLElement(element);
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
            } else {
                return defender;
            }
        } else {
            if (defender != null) {
                return defender;
            } else {
                return null;
            }
        }
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
        } else {
            logger.warning("Tile.contains(" + locatable + ") Not implemented yet!");
        }

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
        if ((getType() == OCEAN) || (getType() == HIGH_SEAS)) {
            return false;
        } else {
            return true;
        }
    }

    
    /**
    * Returns 'true' if this Tile has a road.
    * @return 'true' if this Tile has a road.
    */
    public boolean hasRoad() {
        return road;
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
        if (settlement != null && settlement instanceof Colony) {
            return (Colony) settlement;
        } else {
            return null;
        }
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
    * @param Value new value for forested
    */
    public void setForested(boolean value) {
        forested = value;
    }

    /**
    * Sets whether the tile is plowed or not.
    * @param Value new value for forested
    */
    public void setPlowed(boolean value) {
        plowed = value;
    }
    
    /**
    * Sets whether the tile has a road or not.
    * @param Value new value for forested
    */
    public void setRoad(boolean value) {
        road = value;
    }
    
    /**
    * Sets whether the tile has a bonus or not.
    * @param Value new value for bonus
    */
    public void setBonus(boolean value) {
        bonus = value;
    }


    /**
    * Sets the type for this Tile.
    * @param t The new type for this Tile.
    */
    public void setType(int t) {
        // TODO: check if t is a valid type
        type = t;
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
        } else {
            return true;
        }
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
            if(getMap().getNeighbourOrNull(direction, this).getSettlement() != null) {
                return false;
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
    * @param locatable The code>Locatable</code> to add to this Location.
    */
    public void add(Locatable locatable) {
        if (locatable instanceof Unit) {
            unitContainer.addUnit((Unit) locatable);
        } else {
            logger.warning("Tried to add an unrecognized 'Locatable' to a tile.");
        }
    }


    /**
    * Removes a code>Locatable</code> from this Location.
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
        if (settlement != null) {
            return settlement.getUnitCount() + unitContainer.getUnitCount();
        }
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
    public int potential (int goods)
    {
      // Please someone tell me they want to put this data into a separate file... -sjm
      // Twelve tile types, sixteen goods types, and forested/unforested.
      int potentialtable[][][] =
      {
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
          basepotential = potentialtable[type][goods][(forested ? 1 : 0)];
      } else if (addition_type == ADD_HILLS) {
          basepotential = potentialtable[12][goods][0];
      } else if (addition_type == ADD_MOUNTAINS) {
          basepotential = potentialtable[13][goods][0];
      }
      if (basepotential > 0) {
        if (plowed) basepotential++;
        if (road) basepotential++;
      }
      
      return basepotential;
    }

    /**
    * The type of secondary good this tile produces best (used for Town Commons squares).
    * @return The type of secondary good best produced by this tile.
    */
    public int secondaryGoods()
    {
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
    public int defenseBonus ()
    {
        int defenseTable[][] =
        {
            { 0, 0}, // Unexp
            { 0,50}, // Plains
            { 0,50}, // Grasslands
            { 0,50}, // Prairie
            { 0,50}, // Savannah
            {25,50}, // Marsh
            { 0,75}, // Swamp
            { 0,50}, // Desert
            { 0,50}, // Tundra
            { 0, 0}, // Arctic
            { 0, 0}, // Ocean
            { 0, 0} // High seas
        };
        
        if (addition_type == ADD_HILLS) {
            return 100;
        } else if (addition_type == ADD_MOUNTAINS) {
            return 150;
        }
        return defenseTable[type][(forested ? 1 : 0)];
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
    public Element toXMLElement(Player player, Document document) {
        Element tileElement = document.createElement(getXMLElementTagName());

        tileElement.setAttribute("ID", getID());
        tileElement.setAttribute("x", Integer.toString(x));
        tileElement.setAttribute("y", Integer.toString(y));
        tileElement.setAttribute("type", Integer.toString(type));
        tileElement.setAttribute("addition", Integer.toString(addition_type));
        tileElement.setAttribute("road", Boolean.toString(road));
        tileElement.setAttribute("plowed", Boolean.toString(plowed));
        tileElement.setAttribute("forested", Boolean.toString(forested));
        tileElement.setAttribute("bonus", Boolean.toString(bonus));

        // Notice: Should be calculated by the other host instead of beeing transferred:
        //tileElement.setAttribute("owner", owner.getID());

        if (settlement != null) {
            tileElement.appendChild(settlement.toXMLElement(player, document));
        }

        // TODO: Check if there is a settlement on this tile: Do not show enemy units hidden in a settlement.
        //       Needs to store a "defendingUnit" if the unitContainer is hidden.

        // Check if the player can see the tile: Do not show enemy units on a tile out-of-sight.
        if (player.canSee(this)) {
            tileElement.appendChild(unitContainer.toXMLElement(player, document));
        } else {
            UnitContainer emptyUnitContainer = new UnitContainer(getGame(), this);
            emptyUnitContainer.setID(unitContainer.getID());
            tileElement.appendChild(emptyUnitContainer.toXMLElement(player, document));
        }

        return tileElement;
    }

    /**
    * Initialize this object from an XML-representation of this object.
    *
    * @param tileElement The DOM-element ("Document Object Model") made to represent this "Tile".
    */
    public void readFromXMLElement(Element tileElement) {
        setID(tileElement.getAttribute("ID"));

        x = Integer.parseInt(tileElement.getAttribute("x"));
        y = Integer.parseInt(tileElement.getAttribute("y"));
        type = Integer.parseInt(tileElement.getAttribute("type"));
        addition_type = Integer.parseInt(tileElement.getAttribute("addition"));
        road = Boolean.valueOf(tileElement.getAttribute("road")).booleanValue();
        plowed = Boolean.valueOf(tileElement.getAttribute("plowed")).booleanValue();
        forested = Boolean.valueOf(tileElement.getAttribute("forested")).booleanValue();
        bonus = Boolean.valueOf(tileElement.getAttribute("bonus")).booleanValue();

        // Notice: Should be calculated by the other host instead of beeing transferred:
        //tileElement.getAttribute("owner");

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
    }


    /**
    * Returns the tag name of the root element representing this object.
    *
    * @return the tag name.
    */
    public static String getXMLElementTagName() {
        return "tile";
    }
}
