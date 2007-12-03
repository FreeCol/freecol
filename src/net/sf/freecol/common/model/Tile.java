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
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.Specification;
import net.sf.freecol.common.model.Map.CircleIterator;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.util.EmptyIterator;

import org.w3c.dom.Element;

/**
 * Represents a single tile on the <code>Map</code>.
 * 
 * @see Map
 */
public final class Tile extends FreeColGameObject implements Location, Named, Ownable {
    private static final Logger logger = Logger.getLogger(Tile.class.getName());

    // Indians' claims on the tile may be one of the following:
    public static final int CLAIM_NONE = 0, CLAIM_VISITED = 1, CLAIM_CLAIMED = 2;

    private TileType type;
    
    private boolean lostCityRumour;

    private int x, y;

    private int indianClaim;

    /** The player that consider this tile to be their land. */
    private Player owner;

    /**
     * A pointer to the settlement located on this tile or 'null' if there is no
     * settlement on this tile.
     */
    private Settlement settlement;

    /**
     * Stores all Improvements and Resources (if any)
     */
    private TileItemContainer tileItemContainer;
    
    private UnitContainer unitContainer;

    /** The number of adjacent land tiles, if this is an ocean tile */
    private int landCount = Integer.MIN_VALUE;

    /** The fish bonus of this tile, if it is an ocean tile */
    private int fishBonus = Integer.MIN_VALUE;

    /**
     * Indicates which colony or Indian settlement that owns this tile ('null'
     * indicates no owner). A colony owns the tile it is located on, and every
     * tile with a worker on it. Note that while units and settlements are owned
     * by a player, a tile is owned by a settlement.
     */
    private Settlement owningSettlement;

    /**
     * Stores each player's image of this tile. Only initialized when needed.
     */
    private PlayerExploredTile[] playerExploredTiles = null;

    /**
     * Describe region here.
     */
    private Region region;

    public static int NUMBER_OF_TYPES;

    private List<TileItem> tileItems;

    /**
     * A constructor to use.
     * 
     * @param game The <code>Game</code> this <code>Tile</code> belongs to.
     * @param type The type.
     * @param locX The x-position of this tile on the map.
     * @param locY The y-position of this tile on the map.
     */
    public Tile(Game game, TileType type, int locX, int locY) {
        super(game);

        this.type = type;
        this.indianClaim = CLAIM_NONE;

        unitContainer = new UnitContainer(game, this);
        tileItemContainer = new TileItemContainer(game, this);
        lostCityRumour = false;

        x = locX;
        y = locY;

        owningSettlement = null;
        settlement = null;

        if (!isViewShared()) {
            playerExploredTiles = new PlayerExploredTile[Player.NUMBER_OF_PLAYERS];
        }
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * 
     * @param game The <code>Game</code> this <code>Tile</code> should be
     *            created in.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     */
    public Tile(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);

        if (!isViewShared()) {
            playerExploredTiles = new PlayerExploredTile[Player.NUMBER_OF_PLAYERS];
        }

        readFromXML(in);
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * 
     * @param game The <code>Game</code> this <code>Tile</code> should be
     *            created in.
     * @param e An XML-element that will be used to initialize this object.
     */
    public Tile(Game game, Element e) {
        super(game, e);

        if (!isViewShared()) {
            playerExploredTiles = new PlayerExploredTile[Player.NUMBER_OF_PLAYERS];
        }

        readFromXMLElement(e);
    }

    /**
     * Initiates a new <code>Tile</code> with the given ID. The object should
     * later be initialized by calling either
     * {@link #readFromXML(XMLStreamReader)} or
     * {@link #readFromXMLElement(Element)}.
     * 
     * @param game The <code>Game</code> in which this object belong.
     * @param id The unique identifier for this object.
     */
    public Tile(Game game, String id) {
        super(game, id);

        if (!isViewShared()) {
            playerExploredTiles = new PlayerExploredTile[Player.NUMBER_OF_PLAYERS];
        }
    }

    // ------------------------------------------------------------ static methods

    /**
     * Initializes the important Types for quick reference - performed by Specification.java
     * @param numberOfTypes Initializer for NUMBER_OF_TYPES
     */
    public static void initialize(int numberOfTypes) {
        NUMBER_OF_TYPES = numberOfTypes;
    }

    public boolean isViewShared() {
        return (getGame().getViewOwner() != null);
    }

    /**
     * Get the <code>Region</code> value.
     *
     * @return a <code>Region</code> value
     */
    public Region getRegion() {
        return region;
    }

    /**
     * Set the <code>Region</code> value.
     *
     * @param newRegion The new Region value.
     */
    public void setRegion(final Region newRegion) {
        this.region = newRegion;
    }

    /**
     * Gets the name of this tile, or shows "unexplored" if not explored by player.
     * 
     * @return The name as a <code>String</code>.
     */
    public String getName() {
        if (isViewShared()) {
            if (isExplored()) {
                return getType().getName();
            } else {
                return Messages.message("unexplored");
            }
        } else {
            Player player = getGame().getCurrentPlayer();
            if (player != null) {
                if (playerExploredTiles[player.getIndex()] != null) {
                    PlayerExploredTile pet = playerExploredTiles[player.getIndex()];
                    if (pet.explored) {
                        return getType().getName();
                    }
                }
                return Messages.message("unexplored");
            } else {
                logger.warning("player == null");
                return null;
            }
        }
    }

    /**
     * Returns a description of the <code>Tile</code>, with the name of the tile
     * and any improvements on it (road/plow/etc) from <code>TileItemContainer</code>.
     * @return The description label for this tile
     */
    public String getLabel() {
        return getName() + tileItemContainer.getLabel();
    }
    
    /**
     * Returns the name of this location.
     * 
     * @return The name of this location.
     */
    public String getLocationName() {
        if (settlement == null) {
            Settlement nearSettlement = null;
            int radius = 8; // more than 8 tiles away is no longer "near"
            CircleIterator mapIterator = getMap().getCircleIterator(getPosition(), true, radius);
            while (mapIterator.hasNext()) {
                nearSettlement = getMap().getTile(mapIterator.nextPosition()).getSettlement();
                if (nearSettlement != null && nearSettlement instanceof Colony) {
                    String name = ((Colony) nearSettlement).getName();
                    return getName() + " ("
                        + Messages.message("nearLocation","%location%", name) + ")";
                }
            }
            return getName();
        } else {
            return settlement.getLocationName();
        }
    }

    /**
     * Gets the distance in tiles between this <code>Tile</code> and the
     * specified one.
     * 
     * @param tile The <code>Tile</code> to check the distance to.
     * @return Distance
     */
    public int getDistanceTo(Tile tile) {
        return getGame().getMap().getDistance(getPosition(), tile.getPosition());
    }

    /**
     * Returns null.
     *
     * @return null
     */
    public GoodsContainer getGoodsContainer() {
        return null;
    }

    /**
     * Returns the <code>TileItemContainer</code>.
     *
     * @return a <code>TileItemContainer</code> value
     */
    public TileItemContainer getTileItemContainer() {
        return tileItemContainer;
    }

    /**
     * Returns a List of <code>TileImprovements</code>.
     *
     * @returns a List of <code>TileImprovements</code>
     */
    public List<TileImprovement> getTileImprovements() {
        return tileItemContainer.getImprovements();
    }

    /**
     * Returns a List of completed <code>TileImprovements</code>.
     *
     * @returns a List of <code>TileImprovements</code>
     */
    public List<TileImprovement> getCompletedTileImprovements() {
        List<TileImprovement> result = new ArrayList<TileImprovement>();
        for (TileImprovement improvement : tileItemContainer.getImprovements()) {
            if (improvement.getTurnsToComplete() == 0) {
                result.add(improvement);
            }
        }
        return result;
    }



    /**
     * Gets the total value of all treasure trains on this <code>Tile</code>.
     * 
     * @return The total value of all treasure trains on this <code>Tile</code>
     *         or <code>0</code> if there are no treasure trains at all.
     */
    public int getUnitTreasureAmount() {
        int amount = 0;

        Iterator<Unit> ui = getUnitIterator();
        while (ui.hasNext()) {
            Unit u = ui.next();
            if (u.canCarryTreasure()) {
                amount += u.getTreasureAmount();
            }
        }

        return amount;
    }

    /**
     * Returns the treasure train carrying the largest treasure located on this
     * <code>Tile</code>.
     * 
     * @return The best treasure train or <code>null</code> if no treasure
     *         train is located on this <code>Tile</code>.
     */
    public Unit getBestTreasureTrain() {
        Unit bestTreasureTrain = null;

        Iterator<Unit> ui = getUnitIterator();
        while (ui.hasNext()) {
            Unit u = ui.next();
            if (u.canCarryTreasure()
                && (bestTreasureTrain == null || bestTreasureTrain.getTreasureAmount() < u.getTreasureAmount())) {
                bestTreasureTrain = u;
            }
        }

        return bestTreasureTrain;
    }

    /**
     * Calculates the value of a future colony at this tile.
     * 
     * @return The value of a future colony located on this tile. This value is
     *         used by the AI when deciding where to build a new colony.
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

            for (Tile tile : getGame().getMap().getSurroundingTiles(this, 1)) {
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
                        value += tile.potential(Goods.FOOD);
                    }
                    if (tile.hasResource()) {
                        value += 20;
                    }

                    if (tile.getOwner() != null &&
                        tile.getOwner() != getGame().getCurrentPlayer()) {
                        // tile is already owned by someone (and not by us!)
                        if (tile.getOwner().isEuropean()) {
                            value -= 20;
                        } else {
                            value -= 5;
                        }
                    }
                }
            }

            if (hasResource()) {
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
                // value -= 20;
                value = 0;
            } else {
                // TODO: Remove when wagon train code has been written. START
                final GoalDecider gd = new GoalDecider() {
                    private PathNode goal = null;

                    public PathNode getGoal() {
                        return goal;
                    }

                    public boolean hasSubGoals() {
                        return false;
                    }

                    public boolean check(Unit u, PathNode pathNode) {
                        Map map = getGame().getMap();

                        if (pathNode.getTile().getType().canSailToEurope()) {
                            goal = pathNode;
                            return true;
                        }
                        if (map.isAdjacentToMapEdge(pathNode.getTile())) {
                            goal = pathNode;
                            return true;
                        }
                        return false;
                    }
                };
                final CostDecider cd = new CostDecider() {
                    public int getCost(Unit unit, Tile oldTile, Tile newTile, int movesLeft, int turns) {
                        if (newTile.isLand()) {
                            return ILLEGAL_MOVE;
                        } else {
                            return 1;
                        }
                    }
                    public int getMovesLeft() {
                        return 0;
                    }
                    public boolean isNewTurn() {
                        return false;
                    }
                };
                final PathNode n = getMap().search(this, gd, cd, Integer.MAX_VALUE);
                if (n == null) {
                    value = 0;
                }
                // END-TODO
            }

            return Math.max(0, value);
        }
    }

    /**
     * Gets the <code>Unit</code> that is currently defending this
     * <code>Tile</code>.
     * 
     * @param attacker The target that would be attacking this tile.
     * @return The <code>Unit</code> that has been choosen to defend this
     *         tile.
     */
    public Unit getDefendingUnit(Unit attacker) {
        Unit defender = null;
        float defensePower = -1.0f;
        for(Unit nextUnit : unitContainer.getUnitsClone()) {
            float tmpPower = nextUnit.getDefensePower(attacker);
            if (this.isLand() != nextUnit.isNaval()
                && (tmpPower > defensePower)) {
                defender = nextUnit;
                defensePower = tmpPower;
            }
        }

        if (getSettlement() != null) {
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
     * <br>
     * <br>
     * 
     * This method does not take special unit behavior into account. Use
     * {@link Unit#getMoveCost} whenever it is possible.
     * 
     * @param fromTile The <code>Tile</code> the moving {@link Unit} comes
     *            from.
     * @return The cost of moving the unit.
     * @see Unit#getMoveCost
     */
    public int getMoveCost(Tile fromTile) {
        return tileItemContainer.getMoveCost(getType().getBasicMoveCost(), fromTile);
    }

    /**
     * Disposes all units on this <code>Tile</code>.
     */
    public void disposeAllUnits() {
        unitContainer.disposeAllUnits();
        updatePlayerExploredTiles();
    }

    /**
     * Gets the first <code>Unit</code> on this tile.
     * 
     * @return The first <code>Unit</code> on this tile.
     */
    public Unit getFirstUnit() {
        return unitContainer.getFirstUnit();
    }

    /**
     * Gets the last <code>Unit</code> on this tile.
     * 
     * @return The last <code>Unit</code> on this tile.
     */
    public Unit getLastUnit() {
        return unitContainer.getLastUnit();
    }

    /**
     * Returns the total amount of Units at this Location. This also includes
     * units in a carrier
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
     * @param locatable The <code>Locatable</code> to test the presence of.
     * @return
     *            <ul>
     *            <li><i>true</i> if the specified <code>Locatable</code> is
     *            on this <code>Tile</code> and
     *            <li><i>false</i> otherwise.
     *            </ul>
     */
    public boolean contains(Locatable locatable) {
        if (locatable instanceof Unit) {
            return unitContainer.contains((Unit) locatable);
        } else if (locatable instanceof TileItem) {
            return tileItemContainer.contains((TileItem) locatable);
        }

        logger.warning("Tile.contains(" + locatable + ") Not implemented yet!");

        return false;
    }

    /**
     * Gets the <code>Map</code> in which this <code>Tile</code> belongs.
     * 
     * @return The <code>Map</code>.
     */
    public Map getMap() {
        return getGame().getMap();
    }

    /**
     * Check if the tile has been explored.
     * 
     * @return true iff tile is known.
     */
    public boolean isExplored() {
        return type != null;
    }

    /**
     * Returns 'true' if this Tile is a land Tile, 'false' otherwise.
     * 
     * @return 'true' if this Tile is a land Tile, 'false' otherwise.
     */
    public boolean isLand() {
        return type != null && !type.isWater();
    }

    /**
     * Returns 'true' if this Tile is forested.
     * 
     * @return 'true' if this Tile is forested.
     */
    public boolean isForested() {
        return type != null && type.isForested();
    }

    /**
     * Returns 'true' if this Tile has a road.
     * 
     * @return 'true' if this Tile has a road.
     */
    public boolean hasRoad() {
        return getTileItemContainer().hasRoad() || (getSettlement() != null);
    }

    public TileImprovement getRoad() {
        return getTileItemContainer().getRoad();
    }

    public boolean hasRiver() {
        return getTileItemContainer().hasRiver();
    }

    /**
     * Returns 'true' if this Tile has been plowed.
     * 
     * @return 'true' if this Tile has been plowed.
     *
     * TODO: remove as soon as alternative is clear
     * 
     * @deprecated Plowing is an specification dependent improvement type and should not be needed.
     */
    public boolean isPlowed() {
        return hasImprovement(FreeCol.getSpecification().getTileImprovementType("model.improvement.Plow"));
    }
    
    /**
     * Returns 'true' if this Tile has a resource on it.
     * 
     * @return 'true' if this Tile has a resource on it.
     */
    public boolean hasResource() {
        return getTileItemContainer().hasResource();
    }

    /**
     * Returns the type of this Tile. Returns UNKNOWN if the type of this Tile
     * is unknown.
     * 
     * @return The type of this Tile.
     */
    public TileType getType() {
        return type;
    }

    /**
     * Returns the TileType of this Tile.
     *//*
         public TileType getTileType() {
         return FreeCol.getSpecification().tileType(type);
         }
       */
    /**
     * The nation that consider this tile to be their property.
     * 
     * @return The player owning this tile.
     */
    public Player getOwner() {
        return owner;
    }

    /**
     * Sets the nation that should consider this tile to be their property.
     * 
     * @param owner The player, new owner of this tile.
     * @see #getOwner
     */
    public void setOwner(Player owner) {
        this.owner = owner;
        updatePlayerExploredTiles();
    }

    /**
     * Makes the given player take the ownership of this <code>Tile</code>.
     * The tension level is modified accordingly.
     * 
     * @param player The <code>Player</code>.
     */
    public void takeOwnership(Player player) {
        if (player.getLandPrice(this) > 0) {
            Player otherPlayer = getOwner();
            if (otherPlayer != null) {
                if (!otherPlayer.isEuropean()) {
                    otherPlayer.modifyTension(player, Tension.TENSION_ADD_LAND_TAKEN);
                }
            } else {
                logger.warning("Could not find player with nation: " + getOwner());
            }
        }
        setOwner(player);
        updatePlayerExploredTiles();
    }

    /**
     * Returns the river on this <code>Tile</code> if any
     * @return River <code>TileImprovement</code>
     */
    public TileImprovement getRiver() {
        return tileItemContainer.getRiver();
    }

    /**
     * Returns the style of a river <code>TileImprovement</code> on this <code>Tile</code>.
     * 
     * @return an <code>int</code> value
     */
    public int getRiverStyle() {
        return tileItemContainer.getRiverStyle();
    }

    /**
     * Adds a river to this tile.
     * 
     * @param magnitude The magnitude of the river at this point
     */
    public void addRiver(int magnitude) {
        tileItemContainer.addRiver(magnitude);
    }

    public void setRiverMagnitude(int magnitude) {
        tileItemContainer.setRiverMagnitude(magnitude);
    }

    public void updateRiver() {
        tileItemContainer.updateRiver();
    }

    /**
     * Return the number of land tiles adjacent to this one.
     * 
     * @return an <code>int</code> value
     */
    public int getLandCount() {
        if (landCount < 0) {
            landCount = 0;
            Iterator<Position> tileIterator = getMap().getAdjacentIterator(getPosition());
            while (tileIterator.hasNext()) {
                if (getMap().getTile(tileIterator.next()).isLand()) {
                    landCount++;
                }
            }
        }
        return landCount;
    }

    /**
     * Return the fish bonus of this tile. The fish bonus is zero if
     * this is a land tile. Otherwise it depends on the number of
     * adjacent land tiles and the rivers on these tiles (if any).
     * 
     * @return an <code>int</code> value
     */
    public int getFishBonus() {
        if (fishBonus < 0) {
            fishBonus = 0;
            if (!isLand()) {
                Iterator<Position> tileIterator = getMap().getAdjacentIterator(getPosition());
                while (tileIterator.hasNext()) {
                    Tile t = getMap().getTile(tileIterator.next());
                    if (t.isLand()) {
                        fishBonus++;
                    }
                    if (t.hasRiver()) {
                        fishBonus += t.getRiver().getMagnitude();
                    }
                }
            }
        }
        return fishBonus;
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
     * 
     * @param claim The claim on this Tile.
     */
    public void setClaim(int claim) {
        indianClaim = claim;
        updatePlayerExploredTiles();
    }

    /**
     * Puts a <code>Settlement</code> on this <code>Tile</code>. A
     * <code>Tile</code> can only have one <code>Settlement</code> located
     * on it. The <code>Settlement</code> will also become the owner of this
     * <code>Tile</code>.
     * 
     * @param s The <code>Settlement</code> that shall be located on this
     *            <code>Tile</code>.
     * @see #getSettlement
     */
    public void setSettlement(Settlement s) {
        settlement = s;
        owningSettlement = s;
        setLostCityRumour(false);
        updatePlayerExploredTiles();
    }

    /**
     * Gets the <code>Settlement</code> located on this <code>Tile</code>.
     * 
     * @return The <code>Settlement</code> that is located on this
     *         <code>Tile</code> or <i>null</i> if no <code>Settlement</code>
     *         apply.
     * @see #setSettlement
     */
    public Settlement getSettlement() {
        return settlement;
    }

    /**
     * Gets the <code>Colony</code> located on this <code>Tile</code>. Only
     * a convenience method for {@link #getSettlement} that makes sure that 
     * the settlement is a colony.
     * 
     * @return The <code>Colony</code> that is located on this
     *         <code>Tile</code> or <i>null</i> if no <code>Colony</code>
     *         apply.
     * @see #getSettlement
     */
    public Colony getColony() {

        if (settlement != null && settlement instanceof Colony) {
            return ((Colony) settlement);
        }

        return null;
    }

    /**
     * Sets the owner of this tile. A <code>Settlement</code> become an owner
     * of a <code>Tile</code> when having workers placed on it.
     * 
     * @param owner The Settlement that owns this tile.
     * @see #getOwner
     */
    public void setOwningSettlement(Settlement owner) {
        this.owningSettlement = owner;
        updatePlayerExploredTiles();
    }

    /**
     * Gets the owner of this tile.
     * 
     * @return The Settlement that owns this tile.
     * @see #setOwner
     */
    public Settlement getOwningSettlement() {
        return owningSettlement;
    }

    /**
     * Sets the <code>Resource</code> for this <code>Tile</code>
     */
    public void setResource(ResourceType r) {
        if (r == null) {
            return;
        }
        Resource resource = new Resource(getGame(), this, r);
        tileItemContainer.addTileItem(resource);
        updatePlayerExploredTiles();
    }
    
    /**
     * Sets the type for this Tile.
     * 
     * @param t The new TileType for this Tile.
     */
    public void setType(TileType t) {
        if (t == null) {
            throw new IllegalStateException("Tile type must be valid");
        }
        type = t;
        getTileItemContainer().clear();
        if (!isLand()) {
            settlement = null;
        }
        updatePlayerExploredTiles();
    }

    /**
     * Returns the x-coordinate of this Tile.
     * 
     * @return The x-coordinate of this Tile.
     */
    public int getX() {
        return x;
    }

    /**
     * Returns the y-coordinate of this Tile.
     * 
     * @return The y-coordinate of this Tile.
     */
    public int getY() {
        return y;
    }

    /**
     * Gets the <code>Position</code> of this <code>Tile</code>.
     * 
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
     * @param rumour If <code>true</code> then this <code>Tile</code> will
     *            have a lost city rumour. The type of rumour will be determined
     *            by the server.
     */
    public void setLostCityRumour(boolean rumour) {
        lostCityRumour = rumour;
        
        if (!isLand() && rumour) {
            logger.warning("Setting lost city rumour to Ocean.");
            // Get the first land type from TileTypeList
            for (TileType t : FreeCol.getSpecification().getTileTypeList()) {
                if (!t.isWater()) {
                    setType(t);
                    break;
                }
            }
        }
        
        updatePlayerExploredTiles();
    }

    /**
     * Check if the tile type is suitable for a <code>Settlement</code>,
     * either by a <code>Colony</code> or an <code>IndianSettlement</code>.
     * 
     * @return true if tile suitable for settlement
     */
    public boolean isSettleable() {
        return getType().canSettle();
    }

    /**
     * Check to see if this tile can be used to construct a new
     * <code>Colony</code>. If there is a colony here or in a tile next to
     * this one, it is unsuitable for colonization.
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
                if ((set != null) && (set.getOwner().isEuropean())) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Gets a <code>Unit</code> that can become active. This is preferably a
     * <code>Unit</code> not currently preforming any work.
     * 
     * @return A <code>Unit</code> with <code>movesLeft > 0</code> or
     *         <i>null</i> if no such <code>Unit</code> is located on this
     *         <code>Tile</code>.
     */
    public Unit getMovableUnit() {
        if (getFirstUnit() != null) {
            Iterator<Unit> unitIterator = getUnitIterator();
            while (unitIterator.hasNext()) {
                Unit u = unitIterator.next();

                Iterator<Unit> childUnitIterator = u.getUnitIterator();
                while (childUnitIterator.hasNext()) {
                    Unit childUnit = childUnitIterator.next();

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

        Iterator<Unit> unitIterator = getUnitIterator();
        while (unitIterator.hasNext()) {
            Unit u = unitIterator.next();

            Iterator<Unit> childUnitIterator = u.getUnitIterator();
            while (childUnitIterator.hasNext()) {
                Unit childUnit = childUnitIterator.next();

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
     * Gets the <code>Tile</code> where this <code>Location</code> is
     * located or null if no <code>Tile</code> applies.
     * 
     * @return This <code>Tile</code>.
     */
    public Tile getTile() {
        return this;
    }

    /**
     * Adds a <code>Locatable</code> to this Location.
     * 
     * @param locatable The <code>Locatable</code> to add to this Location.
     */
    public void add(Locatable locatable) {
        if (locatable instanceof Unit) {
            unitContainer.addUnit((Unit) locatable);
        } else if (locatable instanceof TileItem) {
            tileItemContainer.addTileItem((TileItem) locatable);
        } else {
            logger.warning("Tried to add an unrecognized 'Locatable' to a tile.");
        }
        updatePlayerExploredTiles();
    }

    /**
     * Removes a <code>Locatable</code> from this Location.
     * 
     * @param locatable The <code>Locatable</code> to remove from this
     *            Location.
     */
    public void remove(Locatable locatable) {
        if (locatable instanceof Unit) {
            unitContainer.removeUnit((Unit) locatable);
        } else if (locatable instanceof TileItem) {
            tileItemContainer.addTileItem((TileItem) locatable);
        } else {
            logger.warning("Tried to remove an unrecognized 'Locatable' from a tile.");
        }
        updatePlayerExploredTiles();
    }

    /**
     * Returns the amount of units at this <code>Location</code>.
     * 
     * @return The amount of units at this <code>Location</code>.
     */
    public int getUnitCount() {
        return unitContainer.getUnitCount();
    }

    /**
     * Gets a
     * <code>List/code> of every <code>Unit</code> directly located on this
     * <code>Tile</code>. This does not include <code>Unit</code>s located in a
     * <code>Settlement</code> or on another <code>Unit</code> on this <code>Tile</code>.
     *
     * @return The <code>List</code>.
     */
    public List<Unit> getUnitList() {
        return unitContainer.getUnitsClone();
    }
    
    /**
     * Gets an <code>Iterator</code> of every <code>Unit</code> directly
     * located on this <code>Tile</code>. This does not include
     * <code>Unit</code>s located in a <code>Settlement</code> or on
     * another <code>Unit</code> on this <code>Tile</code>.
     * 
     * @return The <code>Iterator</code>.
     */
    public Iterator<Unit> getUnitIterator() {
        return getUnitList().iterator();
    }

    /**
     * Gets a <code>List/code> of every <code>TileItem</code> located on this <code>Tile</code>.
     *
     * @return The <code>List</code>.
     */
    public List<TileItem> getTileItemList() {
        return tileItems;
    }

    /**
     * Gets an <code>Iterator</code> of every <code>TileItem</code>
     * located on this <code>Tile</code>.
     * 
     * @return The <code>Iterator</code>.
     */
    public Iterator<TileItem> getTileItemIterator() {
        if (tileItems == null) {
            return EmptyIterator.getInstance();
        } else {
            return tileItems.iterator();
        }
    }

    /**
     * Checks whether or not the specified locatable may be added to this
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
     * @param goodsType The type of goods to check the potential for.
     * @return The normal potential of this tile to produce that amount of
     *         goods.
     */
    public int potential(GoodsType goodsType) {
        return getTileTypePotential(getType(), goodsType, getTileItemContainer(), getFishBonus());
    }

    /**
     * The potential of this tile to produce a certain type of goods.
     * 
     * @param goodsIndex The index of the goods to check the potential for.
     * @return The normal potential of this tile to produce that amount of
     *         goods.
     */
    public int potential(int goodsIndex) {
        return potential(FreeCol.getSpecification().getGoodsType(goodsIndex));
    }

    /**
     * Gets the maximum potential for producing the given type of goods. The
     * maximum potential is the potential of a tile after the tile has been
     * plowed/built road on.
     * 
     * @param goodsType The type of goods.
     * @return The maximum potential.
     */
    public int getMaximumPotential(GoodsType goodsType) {
        // If we consider maximum potential to the effect of having
        // all possible improvements done, iterate through the
        // improvements and get the bonuses of all related ones.  If
        // there are options to change tiletype using an improvement,
        // consider that too.

        // Assortment of valid TileTypes and their respective
        // TileItemContainers, including this one
        List<TileType> tileTypes = new ArrayList<TileType>();
        List<TileItemContainer> tiContainers = new ArrayList<TileItemContainer>();
        List<TileImprovementType> tiList = FreeCol.getSpecification().getTileImprovementTypeList();

        tileTypes.add(getType());
        // Get a clone of the tileitemcontainer for calculation
        tiContainers.add(getTileItemContainer().clone());

        // Add to the list the various possible tile type changes
        for (TileImprovementType impType : tiList) {
            if (impType.getChange(getType()) != null) {
                // There is an option to change TileType
                tileTypes.add(impType.getChange(getType()));
                // Clone container with the natural improvements of this tile (without resource)
                tiContainers.add(getTileItemContainer().clone(false, true));
            }
        }

        int maxProduction = 0;
        // For each combination, fill the tiContainers with anything that would increase production of oodsType
        for (int i = 0; i < tiContainers.size() ; i++) {
            TileType t = tileTypes.get(i);
            TileItemContainer tic = tiContainers.get(i);
            for (TileImprovementType impType : tiList) {
                if (impType.isNatural() || !impType.isTileTypeAllowed(t)) {
                    continue;
                }
                if (tic.findTileImprovementType(impType) != null) {
                    continue;
                }
                if (impType.getBonus(goodsType) > 0) {
                    tic.addTileItem(new TileImprovement(getGame(), this, impType));
                }
            }
            maxProduction = Math.max(getTileTypePotential(t, goodsType, tic, getFishBonus()), maxProduction);
        }
        return maxProduction;
    }

    /**
     * Checks whether this <code>Tile</code> can have a road or not. This
     * method will return <code>false</code> if a road has already been built.
     * 
     * @return The result.
     */
    public boolean canGetRoad() {
        return isLand() && !tileItemContainer.hasRoad();
    }

    /**
     * Finds the TileImprovement of a given Type, or null if there is no match.
     */
    public TileImprovement findTileImprovementType(TileImprovementType type) {
        return tileItemContainer.findTileImprovementType(type);
    }
    
    /**
     * Will check whether this tile has a completed improvement of the given
     * type.
     * 
     * Useful for checking whether the tile for instance has a road or is
     * plowed.
     * 
     * @param type
     *            The type to check for.
     * @return Whether the tile has the improvement and the improvement is
     *         completed.
     */
    public boolean hasImprovement(TileImprovementType type) {
        return tileItemContainer.hasImprovement(type);
    }
    
    /**
     * Calculates the potential of a certain <code>GoodsType</code>.
     * 
     * @param tileType
     *            The <code>TileType</code>.
     * @param goodsType
     *            The <code>GoodsType</code> to check the potential for.
     * @param tiContainer
     *            The <code>TileItemContainer</code> with any TileItems to
     *            give bonuses.
     * @param fishBonus
     *            The Bonus Fish to be considered if valid
     * @return The amount of goods.
     */
    public static int getTileTypePotential(TileType tileType, GoodsType goodsType, 
                                           TileItemContainer tiContainer, int fishBonus) {
        if (tileType == null || goodsType == null || !goodsType.isFarmed()) {
            return 0;
        }
        // Get tile potential + bonus if any
        int potential = tileType.getPotential(goodsType);
        if (tileType.isWater() && goodsType == Goods.FISH) {
            potential += fishBonus;
        }
        if (tiContainer != null) {
            potential = tiContainer.getTotalBonusPotential(goodsType, potential);
        }
        return potential;
    }

    /**
     * Finds the top three outputs based on TileType, TileItemContainer and FishBonus if any
     * @param tileType The <code>TileType</code>
     * @param tiContainer The <code>TileItemContainer</code>
     * @param fishBonus The Bonus Fish to be considered if valid
     * @return The sorted top three of the outputs.
     */
    public static GoodsType[] getSortedGoodsTop(TileType tileType, TileItemContainer tiContainer, int fishBonus) {
        GoodsType[] top = new GoodsType[3];
        int[] val = new int[3];
        List<GoodsType> goodsTypeList = FreeCol.getSpecification().getGoodsTypeList();
        for (GoodsType g : goodsTypeList) {
            int potential = getTileTypePotential(tileType, g, tiContainer, fishBonus);
            // Higher than the lowest saved value (which is 0 by default)
            if (potential > val[2]) {
                // Find highest spot to put this item
                for (int i = 0; i < 3; i++) {
                    if (potential > val[i]) {
                        // Shift and move down
                        for (int j = 2; j > i; j--) {
                            top[j] = top[j-1];
                            val[j] = val[j-1];
                        }
                        top[i] = g;
                        val[i] = potential;
                        break;
                    }
                }
            }
        }
        return top;
    }

    public List<GoodsType> getSortedGoodsList(final TileType tileType,
                                              final TileItemContainer tiContainer,
                                              final int fishBonus) {
        List<GoodsType> goodsTypeList = FreeCol.getSpecification().getGoodsTypeList();
        Collections.sort(goodsTypeList, new Comparator<GoodsType>() {
                public int compare(GoodsType o, GoodsType p) {
                    return getTileTypePotential(tileType, p, tiContainer, fishBonus) - 
                        getTileTypePotential(tileType, o, tiContainer, fishBonus);
                }
            });
        return goodsTypeList;
    }

    /**
     * The type of primary good (food) this tile produces best (used for Town Commons
     * squares).
     * 
     * @return The type of primary good best produced by this tile.
     * 
     * @TODO: This might fail if the tile produces more other stuff than food.
     * 
     */
    public GoodsType primaryGoods() {
        if (type == null) {
            return null;
        }
        
        GoodsType[] top = getSortedGoodsTop(type, tileItemContainer, getFishBonus());
        for (GoodsType g : top) {
            if (g != null && g.isFoodType()) {
                return g;
            }
        }
        return null;
    }

    /**
     * The type of secondary good (non-food) this tile produces best (used for Town Commons
     * squares).
     * 
     * @return The type of secondary good best produced by this tile (or null if none found).
     */
    public GoodsType secondaryGoods() {
        if (type != null) {
            return type.getSecondaryGoods();
        } else {
            return null;
        }
    }

    /**
     * The defense/ambush bonus of this tile.
     * <p>
     * Note that the defense bonus is relative to the unit base strength,
     * not to the cumulative strength.
     * 
     * @return The defense modifier (in percent) of this tile.
     */
    public int defenseBonus() {
        if (type == null || type.getDefenceBonus() == null) {
            return 0;
        }
        return (int) type.getDefenceBonus().getValue();
    }

    public Modifier getDefenceBonus() {
        return type.getDefenceBonus();
    }

    /**
     * This method is called only when a new turn is beginning. It will reduce the quantity of
     * the bonus <code>Resource</code> that is on the tile, if any and if applicable.
     * @see ResourceType
     * @see ColonyTile#newTurn
     */
    public void expendResource(GoodsType goodsType, Settlement settlement) {
        if (hasResource() && tileItemContainer.getResource().getQuantity() != -1) {
            Resource resource = tileItemContainer.getResource();
            // Potential of this Tile and Improvements
            int potential = getTileTypePotential(getType(), goodsType, null, getFishBonus())
                + tileItemContainer.getImprovementBonusPotential(goodsType);
            if (resource.useQuantity(goodsType, potential) == 0) {
                addModelMessage(this, "model.tile.resourceExhausted", 
                                new String[][] { 
                                    { "%resource%", resource.getName() },
                                    { "%colony%", ((Colony) settlement).getName() } },
                                ModelMessage.WARNING);
            }
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

        if (toSavedGame && !showAll) {
            logger.warning("toSavedGame is true, but showAll is false");
        }

        PlayerExploredTile pet = null;
        if (!(showAll)) {
            // We're sending the Tile from the server to the client and showAll
            // is false.
            if (player != null) {
                if (playerExploredTiles[player.getIndex()] != null) {
                    pet = playerExploredTiles[player.getIndex()];
                }
            } else {
                logger.warning("player == null");
            }
        }

        out.writeAttribute("ID", getId());
        out.writeAttribute("x", Integer.toString(x));
        out.writeAttribute("y", Integer.toString(y));
        if (type != null) {
            out.writeAttribute("type", getType().getId());
        }

        if (region != null) {
            out.writeAttribute("region", region.getId());
        }

        boolean lostCity = (pet == null) ? lostCityRumour : pet.hasLostCityRumour();
        out.writeAttribute("lostCityRumour", Boolean.toString(lostCity));

        if (owner != null) {
            if (getGame().isClientTrusted() || showAll || player.canSee(this)) {
                out.writeAttribute("owner", owner.getId());
            } else if (pet != null) {
                if (pet.getOwner() != null) {
                    out.writeAttribute("owner", pet.getOwner().getId());
                }
            }
        }

        if ((getGame().isClientTrusted() || showAll || player.canSee(this)) && (owningSettlement != null)) {
            out.writeAttribute("owningSettlement", owningSettlement.getId());
        }

        if (settlement != null) {
            if (pet == null || getGame().isClientTrusted() || showAll || settlement.getOwner() == player) {
                settlement.toXML(out, player, showAll, toSavedGame);
            } else {
                if (getColony() != null) {
                    if (!player.canSee(getTile())) {
                        if (pet.getColonyUnitCount() != 0) {
                            out.writeStartElement(Colony.getXMLElementTagName());
                            out.writeAttribute("ID", getColony().getId());
                            out.writeAttribute("name", getColony().getName());
                            out.writeAttribute("owner", getColony().getOwner().getId());
                            out.writeAttribute("tile", getId());
                            out.writeAttribute("unitCount", Integer.toString(pet.getColonyUnitCount()));

                            Building stockade = getColony().getStockade();
                            if (stockade != null) {
                                stockade.toXML(out);
                            }

                            GoodsContainer emptyGoodsContainer = new GoodsContainer(getGame(), getColony());
                            emptyGoodsContainer.setFakeID(getColony().getGoodsContainer().getId());
                            emptyGoodsContainer.toXML(out, player, showAll, toSavedGame);

                            out.writeEndElement();
                        } // Else: Colony not discovered.
                    } else {
                        settlement.toXML(out, player, showAll, toSavedGame);
                    }
                } else if (getSettlement() instanceof IndianSettlement) {
                    final IndianSettlement is = (IndianSettlement) getSettlement();

                    out.writeStartElement(IndianSettlement.getXMLElementTagName());
                    out.writeAttribute("ID", getSettlement().getId());
                    out.writeAttribute("tile", getId());
                    out.writeAttribute("owner", getSettlement().getOwner().getId());
                    out.writeAttribute("tribe", Integer.toString(is.getTribe()));
                    out.writeAttribute("kind", Integer.toString(is.getKind()));
                    out.writeAttribute("isCapital", Boolean.toString(is.isCapital()));
                    if (pet.getSkill() != null) {
                        out.writeAttribute("learnableSkill", Integer.toString(pet.getSkill().getIndex()));
                    }
                    if (pet.getHighlyWantedGoods() != null) {
                        out.writeAttribute("highlyWantedGoods", pet.getHighlyWantedGoods().getId());
                        out.writeAttribute("wantedGoods1", pet.getWantedGoods1().getId());
                        out.writeAttribute("wantedGoods2", pet.getWantedGoods2().getId());
                    }
                    out.writeAttribute("hasBeenVisited", Boolean.toString(pet.hasBeenVisited()));

                    int[] tensionArray = new int[Player.NUMBER_OF_PLAYERS];
                    for (int i = 0; i < tensionArray.length; i++) {
                        tensionArray[i] = is.getAlarm(i).getValue();
                    }
                    toArrayElement("alarm", tensionArray, out);

                    if (pet.getMissionary() != null) {
                        out.writeStartElement("missionary");
                        pet.getMissionary().toXML(out, player, false, false);
                        out.writeEndElement();
                    }

                    UnitContainer emptyUnitContainer = new UnitContainer(getGame(), getSettlement());
                    emptyUnitContainer.setFakeID(is.getUnitContainer().getId());
                    emptyUnitContainer.toXML(out, player, showAll, toSavedGame);

                    GoodsContainer emptyGoodsContainer = new GoodsContainer(getGame(), is);
                    emptyGoodsContainer.setFakeID(is.getGoodsContainer().getId());
                    emptyGoodsContainer.toXML(out, player, showAll, toSavedGame);

                    out.writeEndElement();
                } else {
                    logger.warning("Unknown type of settlement: " + getSettlement());
                }
            }
        }

        // Check if the player can see the tile:
        // Do not show enemy units or any tileitems on a tile out-of-sight.
        if (getGame().isClientTrusted() || showAll
            || (player.canSee(this) && (settlement == null || settlement.getOwner() == player))
            || !getGameOptions().getBoolean(GameOptions.UNIT_HIDING) && player.canSee(this)) {
            unitContainer.toXML(out, player, showAll, toSavedGame);
            tileItemContainer.toXML(out, player, showAll, toSavedGame);
        } else {
            UnitContainer emptyUnitContainer = new UnitContainer(getGame(), this);
            emptyUnitContainer.setFakeID(unitContainer.getId());
            emptyUnitContainer.toXML(out, player, showAll, toSavedGame);
            TileItemContainer emptyTileItemContainer = new TileItemContainer(getGame(), this);
            emptyTileItemContainer.setFakeID(tileItemContainer.getId());
            emptyTileItemContainer.toXML(out, player, showAll, toSavedGame);
        }

        if (toSavedGame) {
            for (int i = 0; i < playerExploredTiles.length; i++) {
                if (playerExploredTiles[i] != null && playerExploredTiles[i].isExplored()) {
                    playerExploredTiles[i].toXML(out, player, showAll, toSavedGame);
                }
            }
        }

        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * 
     * @param in The input stream with the XML.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setId(in.getAttributeValue(null, "ID"));

        x = Integer.parseInt(in.getAttributeValue(null, "x"));
        y = Integer.parseInt(in.getAttributeValue(null, "y"));
        String typeStr = in.getAttributeValue(null, "type");
        if (typeStr != null) {
            type = FreeCol.getSpecification().getTileType(typeStr);
        }

        String regionString = in.getAttributeValue(null, "region");
        if (regionString != null) {
            region = getGame().getMap().getRegion(regionString);
        }

        final String lostCityRumourStr = in.getAttributeValue(null, "lostCityRumour");
        if (lostCityRumourStr != null) {
            lostCityRumour = Boolean.valueOf(lostCityRumourStr).booleanValue();
        } else {
            lostCityRumour = false;
        }

        final String ownerStr = in.getAttributeValue(null, "owner");
        if (ownerStr != null) {
            owner = (Player) getGame().getFreeColGameObject(ownerStr);
        } else {
            owner = null;
        }

        final String owningSettlementStr = in.getAttributeValue(null, "owningSettlement");
        if (owningSettlementStr != null) {
            owningSettlement = (Settlement) getGame().getFreeColGameObject(owningSettlementStr);
            if (owningSettlement == null) {
                if (owningSettlementStr.startsWith(IndianSettlement.getXMLElementTagName())) {
                    owningSettlement = new IndianSettlement(getGame(), owningSettlementStr);
                } else if (owningSettlementStr.startsWith(Colony.getXMLElementTagName())) {
                    owningSettlement = new Colony(getGame(), owningSettlementStr);
                } else {
                    logger.warning("Unknown type of Settlement.");
                }
            }
        } else {
            owningSettlement = null;
        }

        boolean settlementSent = false;
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals(Colony.getXMLElementTagName())) {
                settlement = (Settlement) getGame().getFreeColGameObject(in.getAttributeValue(null, "ID"));
                if (settlement != null) {
                    settlement.readFromXML(in);
                } else {
                    settlement = new Colony(getGame(), in);
                }
                settlementSent = true;
            } else if (in.getLocalName().equals(IndianSettlement.getXMLElementTagName())) {
                settlement = (Settlement) getGame().getFreeColGameObject(in.getAttributeValue(null, "ID"));
                if (settlement != null) {
                    settlement.readFromXML(in);
                } else {
                    settlement = new IndianSettlement(getGame(), in);
                }
                settlementSent = true;
            } else if (in.getLocalName().equals(UnitContainer.getXMLElementTagName())) {
                unitContainer = (UnitContainer) getGame().getFreeColGameObject(in.getAttributeValue(null, "ID"));
                if (unitContainer != null) {
                    unitContainer.readFromXML(in);
                } else {
                    unitContainer = new UnitContainer(getGame(), this, in);
                }
            } else if (in.getLocalName().equals(TileItemContainer.getXMLElementTagName())) {
                tileItemContainer = (TileItemContainer) getGame().getFreeColGameObject(in.getAttributeValue(null, "ID"));
                if (tileItemContainer != null) {
                    tileItemContainer.readFromXML(in);
                } else {
                    tileItemContainer = new TileItemContainer(getGame(), this, in);
                }
            } else if (in.getLocalName().equals("playerExploredTile")) {
                // Only from a savegame:
                if (playerExploredTiles[Integer.parseInt(in.getAttributeValue(null, "nation"))] == null) {
                    PlayerExploredTile pet = new PlayerExploredTile(in);
                    playerExploredTiles[pet.getNation()] = pet;
                } else {
                    playerExploredTiles[Integer.parseInt(in.getAttributeValue(null, "nation"))].readFromXML(in);
                }
            }
        }
        if (!settlementSent && settlement != null) {
            settlement.dispose();
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
        return playerExploredTiles[player.getIndex()];
    }

    /**
     * Creates a <code>PlayerExploredTile</code> for the given
     * <code>Player</code>.
     * 
     * @param player The <code>Player</code>.
     * @see PlayerExploredTile
     */
    private void createPlayerExploredTile(Player player) {
        playerExploredTiles[player.getIndex()] = new PlayerExploredTile(player.getIndex(), getTileItemContainer());
    }

    /**
     * Updates the information about this <code>Tile</code> for the given
     * <code>Player</code>.
     * 
     * @param player The <code>Player</code>.
     */
    public void updatePlayerExploredTile(Player player) {
        int nation = player.getIndex();
        if (playerExploredTiles == null || getGame().getViewOwner() != null) {
            return;
        }
        if (playerExploredTiles[nation] == null && !player.isEuropean()) {
            return;
        }
        if (playerExploredTiles[nation] == null) {
            logger.warning("'playerExploredTiles' for " + player.getName() + " is 'null'.");
            throw new IllegalStateException("'playerExploredTiles' for " + player.getName()
                                            + " (index " + player.getIndex() + ") " 
                                            + " is 'null'. " + player.canSee(this) + ", "
                                            + isExploredBy(player) + " ::: " + getPosition());
        }

        playerExploredTiles[nation].getTileItemInfo(tileItemContainer);

        playerExploredTiles[nation].setLostCityRumour(lostCityRumour);
        playerExploredTiles[nation].setOwner(owner);
        playerExploredTiles[nation].setRegion(region);

        if (getColony() != null) {
            playerExploredTiles[nation].setColonyUnitCount(getSettlement().getUnitCount());
            
            // TODO stockade may now be null, but is 0 the right way to set this?
            // This might as well be a mistake in the spec.
            Building stockade = getColony().getStockade();
            if (stockade != null){
            	playerExploredTiles[nation].setColonyStockadeLevel(stockade.getType().getIndex());
            } else {
            	playerExploredTiles[nation].setColonyStockadeLevel(0);
            }
        } else if (getSettlement() != null) {
            playerExploredTiles[nation].setMissionary(((IndianSettlement) getSettlement()).getMissionary());

            /*
             * These attributes should not be updated by this method: skill,
             * highlyWantedGoods, wantedGoods1 and wantedGoods2
             */
        } else {
            playerExploredTiles[nation].setColonyUnitCount(0);
        }
    }

    /**
     * Updates the <code>PlayerExploredTile</code> for each player. This
     * update will only be performed if the player
     * {@link Player#canSee(Tile) can see} this <code>Tile</code>.
     */
    public void updatePlayerExploredTiles() {
        if (playerExploredTiles == null || getGame().getViewOwner() != null) {
            return;
        }
        Iterator<Player> it = getGame().getPlayerIterator();
        while (it.hasNext()) {
            Player p = it.next();
            if (playerExploredTiles[p.getIndex()] == null && !p.isEuropean()) {
                continue;
            }
            if (p.canSee(this)) {
                updatePlayerExploredTile(p);
            }
        }
    }

    /**
     * Updates the information about this <code>Tile</code> for the given
     * <code>Player</code>.
     * 
     * @param nation The {@link Player#getNation nation} identifying the
     *            <code>Player</code>.
     */
    public void updatePlayerExploredTile(int nation) {
        updatePlayerExploredTile(getGame().getPlayer(nation));
    }

    /**
     * Checks if this <code>Tile</code> has been explored by the given
     * <code>Player</code>.
     * 
     * @param player The <code>Player</code>.
     * @return <code>true</code> if this <code>Tile</code> has been explored
     *         by the given <code>Player</code> and <code>false</code>
     *         otherwise.
     */
    public boolean isExploredBy(Player player) {
        if (player.isIndian()) {
            return true;
        }
        if (playerExploredTiles[player.getIndex()] == null || !isExplored()) {
            return false;
        }

        return getPlayerExploredTile(player).isExplored();
    }

    /**
     * Sets this <code>Tile</code> to be explored by the given
     * <code>Player</code>.
     * 
     * @param player The <code>Player</code>.
     * @param explored <code>true</code> if this <code>Tile</code> should be
     *            explored by the given <code>Player</code> and
     *            <code>false</code> otherwise.
     */
    public void setExploredBy(Player player, boolean explored) {
        if (player.isIndian()) {
            return;
        }
        if (playerExploredTiles[player.getIndex()] == null) {
            createPlayerExploredTile(player);
        }
        getPlayerExploredTile(player).setExplored(explored);
        updatePlayerExploredTile(player);
    }

    /**
     * Updates the skill available from the <code>IndianSettlement</code>
     * located on this <code>Tile</code>.
     * <p>
     * 
     * @param player The <code>Player</code> which should get the updated
     *            information.
     * @exception NullPointerException If there is no settlement on this
     *                <code>Tile</code>.
     * @exception ClassCastException If the <code>Settlement</code> on this
     *                <code>Tile</code> is not an
     *                <code>IndianSettlement</code>.
     * @see IndianSettlement
     */
    public void updateIndianSettlementSkill(Player player) {
        IndianSettlement is = (IndianSettlement) getSettlement();
        PlayerExploredTile pet = getPlayerExploredTile(player);
        pet.setSkill(is.getLearnableSkill());
        pet.setVisited();
    }

    /**
     * Updates the information about the <code>IndianSettlement</code> located
     * on this <code>Tile</code>.
     * <p>
     * 
     * @param player The <code>Player</code> which should get the updated
     *            information.
     * @exception NullPointerException If there is no settlement on this
     *                <code>Tile</code>.
     * @exception ClassCastException If the <code>Settlement</code> on this
     *                <code>Tile</code> is not an
     *                <code>IndianSettlement</code>.
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
        playerExploredTile.setVisited();
    }

    /**
     * This class contains the data visible to a specific player.
     * 
     * <br>
     * <br>
     * 
     * Sometimes a tile contains information that should not be given to a
     * player. For instance; a settlement that was built after the player last
     * viewed the tile.
     * 
     * <br>
     * <br>
     * 
     * The <code>toXMLElement</code> of {@link Tile} uses information from
     * this class to hide information that is not available.
     */
    public class PlayerExploredTile {

        private int nation;

        private boolean explored = false;

        private Player owner;

        private Region region;

        // All known TileItems
        private Resource resource;
        private List<TileImprovement> improvements;
        private TileImprovement road;
        private TileImprovement river;

        // Colony data:
        private int colonyUnitCount = 0, colonyStockadeLevel;

        // IndianSettlement data:
        private UnitType skill = null;
        private GoodsType highlyWantedGoods = null, wantedGoods1 = null, wantedGoods2 = null;
        private boolean settlementVisited = false;

        private Unit missionary = null;

        // private Settlement settlement;

        private boolean lostCityRumour;


        /**
         * Creates a new <code>PlayerExploredTile</code>.
         * 
         * @param nation The nation.
         */
        public PlayerExploredTile(int nation, TileItemContainer tic) {
            this.nation = nation;
            getTileItemInfo(tic);
        }

        /**
         * Initialize this object from an XML-representation of this object.
         * 
         * @param in The XML stream to read the data from.
         * @throws XMLStreamException if an error occured during parsing.
         */
        public PlayerExploredTile(XMLStreamReader in) throws XMLStreamException {
            readFromXML(in);
        }

        /**
         * Copies given TileItemContainer
         * @param tic The <code>TileItemContainer</code> to copy from
         */
        public void getTileItemInfo(TileItemContainer tic) {
            resource = tic.getResource();
            improvements = tic.getImprovements();
            road = tic.getRoad();
            river = tic.getRiver();
        }

        public void setColonyUnitCount(int colonyUnitCount) {
            this.colonyUnitCount = colonyUnitCount;
        }

        public int getColonyUnitCount() {
            return colonyUnitCount;
        }

        public void setColonyStockadeLevel(int colonyStockadeLevel) {
            this.colonyStockadeLevel = colonyStockadeLevel;
        }

        public int getColonyStockadeLevel() {
            return colonyStockadeLevel;
        }

        public boolean hasRoad() {
            return (road != null);
        }

        public TileImprovement getRoad() {
            return road;
        }

        public boolean hasRiver() {
            return (river != null);
        }

        public TileImprovement getRiver() {
            return river;
        }

        public void setLostCityRumour(boolean lostCityRumour) {
            this.lostCityRumour = lostCityRumour;
        }

        public boolean hasLostCityRumour() {
            return lostCityRumour;
        }

        public void setExplored(boolean explored) {
            this.explored = explored;
        }

        public void setSkill(UnitType newSkill) {
            this.skill = newSkill;
        }

        public UnitType getSkill() {
            return skill;
        }

        public void setOwner(Player owner) {
            this.owner = owner;
        }

        public Player getOwner() {
            return owner;
        }

        public void setHighlyWantedGoods(GoodsType highlyWantedGoods) {
            this.highlyWantedGoods = highlyWantedGoods;
        }

        public GoodsType getHighlyWantedGoods() {
            return highlyWantedGoods;
        }

        public void setWantedGoods1(GoodsType wantedGoods1) {
            this.wantedGoods1 = wantedGoods1;
        }

        public GoodsType getWantedGoods1() {
            return wantedGoods1;
        }

        public void setWantedGoods2(GoodsType wantedGoods2) {
            this.wantedGoods2 = wantedGoods2;
        }

        public GoodsType getWantedGoods2() {
            return wantedGoods2;
        }

        public void setMissionary(Unit missionary) {
            this.missionary = missionary;
        }

        public Unit getMissionary() {
            return missionary;
        }

        private void setVisited() {
            settlementVisited = true;
        }

        private boolean hasBeenVisited() {
            return settlementVisited;
        }

        public Region getRegion() {
            return region;
        }

        private void setRegion(Region region) {
            this.region = region;
        }

        /**
         * Checks if this <code>Tile</code> has been explored.
         * 
         * @return <i>true</i> if the tile has been explored.
         */
        public boolean isExplored() {
            return explored;
        }

        /**
         * Gets the nation owning this object.
         * 
         * @return The nation of this <code>PlayerExploredTile</code>.
         */
        public int getNation() {
            return nation;
        }

        /**
         * This method writes an XML-representation of this object to the given
         * stream.
         * 
         * <br>
         * <br>
         * 
         * Only attributes visible to the given <code>Player</code> will be
         * added to that representation if <code>showAll</code> is set to
         * <code>false</code>.
         * 
         * @param out The target stream.
         * @param player The <code>Player</code> this XML-representation
         *            should be made for, or <code>null</code> if
         *            <code>showAll == true</code>.
         * @param showAll Only attributes visible to <code>player</code> will
         *            be added to the representation if <code>showAll</code>
         *            is set to <i>false</i>.
         * @param toSavedGame If <code>true</code> then information that is
         *            only needed when saving a game is added.
         * @throws XMLStreamException if there are any problems writing to the
         *             stream.
         */
        public void toXML(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame)
            throws XMLStreamException {
            // Start element:
            out.writeStartElement("playerExploredTile");

            out.writeAttribute("nation", Integer.toString(nation));

            if (!explored) {
                out.writeAttribute("explored", Boolean.toString(explored));
            }
            if (Tile.this.hasLostCityRumour()) {
                out.writeAttribute("lostCityRumour", Boolean.toString(lostCityRumour));
            }
            if (Tile.this.getOwner() != owner && owner != null) {
                out.writeAttribute("owner", owner.getId());
            }
            if (colonyUnitCount != 0) {
                out.writeAttribute("colonyUnitCount", Integer.toString(colonyUnitCount));
                out.writeAttribute("colonyStockadeLevel", Integer.toString(colonyStockadeLevel));
            }
            if (skill != null) {
                out.writeAttribute("learnableSkill", Integer.toString(skill.getIndex()));
            }
            out.writeAttribute("settlementVisited", Boolean.toString(settlementVisited));
            if (highlyWantedGoods != null) {
                out.writeAttribute("highlyWantedGoods", Integer.toString(highlyWantedGoods.getIndex()));
                out.writeAttribute("wantedGoods1", Integer.toString(wantedGoods1.getIndex()));
                out.writeAttribute("wantedGoods2", Integer.toString(wantedGoods2.getIndex()));
            }
            if (missionary != null) {
                out.writeStartElement("missionary");
                missionary.toXML(out, player, showAll, toSavedGame);
                out.writeEndElement();
            }
            if (hasResource()) {
                resource.toXML(out, player, showAll, toSavedGame);
            }
            if (region != null) {
                out.writeAttribute("region", region.getId());
            }
            for (TileImprovement t : improvements) { 
                t.toXML(out, player, showAll, toSavedGame);
            }

            out.writeEndElement();
        }

        /**
         * Initialize this object from an XML-representation of this object.
         * 
         * @param in The input stream with the XML.
         * @throws XMLStreamException if an error occured during parsing.
         */
        public void readFromXML(XMLStreamReader in) throws XMLStreamException {
            nation = Integer.parseInt(in.getAttributeValue(null, "nation"));

            final String exploredStr = in.getAttributeValue(null, "explored");
            if (exploredStr != null) {
                explored = Boolean.valueOf(exploredStr).booleanValue();
            } else {
                explored = true;
            }

            final String lostCityRumourStr = in.getAttributeValue(null, "lostCityRumour");
            if (lostCityRumourStr != null) {
                lostCityRumour = Boolean.valueOf(lostCityRumourStr).booleanValue();
            } else {
                lostCityRumour = Tile.this.hasLostCityRumour();
            }

            final String ownerStr = in.getAttributeValue(null, "owner");
            if (ownerStr != null) {
                owner = (Player) getGame().getFreeColGameObject(ownerStr);
            } else {
                owner = Tile.this.getOwner();
            }

            final String colonyUnitCountStr = in.getAttributeValue(null, "colonyUnitCount");
            if (colonyUnitCountStr != null) {
                colonyUnitCount = Integer.parseInt(colonyUnitCountStr);
                colonyStockadeLevel = Integer.parseInt(in.getAttributeValue(null, "colonyStockadeLevel"));
            } else {
                colonyUnitCount = 0;
            }

            Specification spec = FreeCol.getSpecification();
            final String learnableSkillStr = in.getAttributeValue(null, "learnableSkill");
            if (learnableSkillStr != null) {
                skill = spec.getUnitType(Integer.parseInt(learnableSkillStr));
            } else {
                skill = null;
            }
            settlementVisited = Boolean.valueOf(in.getAttributeValue(null, "settlementVisited")).booleanValue();

            final String highlyWantedGoodsStr = in.getAttributeValue(null, "highlyWantedGoods");
            if (highlyWantedGoodsStr != null) {
                highlyWantedGoods = spec.getGoodsType(Integer.parseInt(highlyWantedGoodsStr));
                wantedGoods1 = spec.getGoodsType(Integer.parseInt(in.getAttributeValue(null, "wantedGoods1")));
                wantedGoods2 = spec.getGoodsType(Integer.parseInt(in.getAttributeValue(null, "wantedGoods2")));
            } else {
                highlyWantedGoods = null;
                wantedGoods1 = null;
                wantedGoods2 = null;
            }

            String regionString = in.getAttributeValue(null, "region");
            if (regionString != null) {
                region = getGame().getMap().getRegion(regionString);
            }

            missionary = null;
            TileItemContainer tileItemContainer = new TileItemContainer(getGame(), Tile.this);
            while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
                if (in.getLocalName().equals("missionary")) {
                    in.nextTag();
                    missionary = (Unit) getGame().getFreeColGameObject(in.getAttributeValue(null, "ID"));
                    if (missionary == null) {
                        missionary = new Unit(getGame(), in);
                    } else {
                        missionary.readFromXML(in);
                    }
                } else if (in.getLocalName().equals(Resource.getXMLElementTagName())) {
                    Resource resource = (Resource) getGame().getFreeColGameObject(in.getAttributeValue(null, "ID"));
                    if (resource != null) {
                        resource.readFromXML(in);
                    } else {
                        resource = new Resource(getGame(), in);
                    }
                    tileItemContainer.addTileItem(resource);
                } else if (in.getLocalName().equals(TileImprovement.getXMLElementTagName())) {
                    TileImprovement ti = (TileImprovement) getGame().getFreeColGameObject(in.getAttributeValue(null, "ID"));
                    if (ti != null) {
                        ti.readFromXML(in);
                    } else {
                        ti = new TileImprovement(getGame(), in);
                    }
                    tileItemContainer.addTileItem(ti);
                }
            }
            getTileItemInfo(tileItemContainer);
        }

        /**
         * Returns the tag name of the root element representing this object.
         * 
         * @return "playerExploredTile".
         */
        /*
         * public static String getXMLElementTagName() { return
         * "playerExploredTile"; }
         */
    }

    /**
     * Returns the number of turns it takes for a non-expert pioneer to perform
     * the given <code>TileImprovementType</code>. It will check if it is valid
     * for this <code>TileType</code>.
     * 
     * @param workType The <code>TileImprovementType</code>
     * 
     * @return The number of turns it should take a non-expert pioneer to finish
     *         the work.
     */
    public int getWorkAmount(TileImprovementType workType) {
        if (workType == null) {
            return -1;
        }
        if (!workType.isTileAllowed(this)) {
            return -1;
        }
        // Return the basic work turns + additional work turns
        return (getType().getBasicWorkTurns() + workType.getAddWorkTurns());
    }

    /**
     * Returns the unit who is occupying the tile
     * @return the unit who is occupying the tile
     * @see #isOccupied()
     */
    public Unit getOccupyingUnit() {
        Unit unit = getFirstUnit();
        Player owner = null;
        if (owningSettlement != null) {
            owner = owningSettlement.getOwner();
        }
        if (owner != null && unit != null && unit.getOwner() != owner
            && owner.getStance(unit.getOwner()) != Player.ALLIANCE) {
            for(Unit enemyUnit : getUnitList()) {
                if (enemyUnit.isOffensiveUnit() && enemyUnit.getState() == Unit.FORTIFIED) {
                    return enemyUnit;
                }
            }
        }
        return null;
    }

    /**
     * Checks whether there is a fortified enemy unit in the tile.
     * Units can't produce in occupied tiles
     * @return <code>true</code> if an fortified enemy unit is in the tile
     */
    public boolean isOccupied() {
        return getOccupyingUnit() != null;
    }
}
