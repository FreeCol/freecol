/*
 *  Tile.java - A tile from the map.
 */

package net.sf.freecol.common.model;

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

    private boolean road,
                    plowed,
                    forested,
                    bonus;

    private int     type;

    private int     x,
                    y;

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
    * @return The <code>Unit</code> that has been choosen to defend this tile.
    */
    public Unit getDefendingUnit() {
        Iterator unitIterator = getUnitIterator();

        Unit defender;
        if (unitIterator.hasNext()) {
            defender = (Unit) unitIterator.next();
        } else {
            return null;
        }

        while (unitIterator.hasNext()) {
            Unit nextUnit = (Unit) unitIterator.next();

            if (nextUnit.getDefensePower() > defender.getDefensePower()) {
                defender = nextUnit;
            }
        }

        return defender;
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

        if (type == ARCTIC || type == OCEAN || type == HIGH_SEAS) {
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
    * @return The code>Tile</code> where this <code>Location</code> is
    *         located. Or null if no code>Tile</code> applies.
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
        tileElement.setAttribute("road", Boolean.toString(road));
        tileElement.setAttribute("plowed", Boolean.toString(plowed));
        tileElement.setAttribute("forested", Boolean.toString(forested));
        tileElement.setAttribute("bonus", Boolean.toString(bonus));

        // Notice: Should be calculated by the other host instead of beeing transferred:
        //tileElement.setAttribute("owner", owner.getID());

        if (settlement != null) {
            tileElement.appendChild(settlement.toXMLElement(player, document));
        }

        // Check if there is a settlement on this tile: Do not show enemy units hidden in a settlement:
        // Check if the player can see the tile: Do not show enemy units on a tile out-of-sight.
        if ((settlement == null || settlement.getOwner().equals(player)) && player.canSee(this)) {
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
        road = Boolean.valueOf(tileElement.getAttribute("road")).booleanValue();
        plowed = Boolean.valueOf(tileElement.getAttribute("plowed")).booleanValue();
        forested = Boolean.valueOf(tileElement.getAttribute("forested")).booleanValue();
        bonus = Boolean.valueOf(tileElement.getAttribute("bonus")).booleanValue();

        // Notice: Should be calculated by the other host instead of beeing transferred:
        //tileElement.getAttribute("owner");

        NodeList cnl = tileElement.getElementsByTagName(Colony.getXMLElementTagName());
        if (cnl.getLength() > 0) {
            Element colonyElement = (Element) cnl.item(0);
            if (settlement != null && settlement instanceof Colony) {
                settlement.readFromXMLElement(colonyElement);
            } else {
                settlement = new Colony(getGame(), colonyElement);
            }
        }
        
        NodeList inl = tileElement.getElementsByTagName(IndianSettlement.getXMLElementTagName());
        if (inl.getLength() > 0) {
            Element indianSettlementElement = (Element) inl.item(0);
            if (settlement != null && settlement instanceof IndianSettlement) {
                settlement.readFromXMLElement(indianSettlementElement);
            } else {
                settlement = new IndianSettlement(getGame(), indianSettlementElement);
            }
        }

        Element unitContainerElement = (Element) tileElement.getElementsByTagName(UnitContainer.getXMLElementTagName()).item(0);
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
