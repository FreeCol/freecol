/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.util.RandomChoice;
import net.sf.freecol.common.util.Utils;


/**
 * Represents a single tile on the <code>Map</code>.
 *
 * @see Map
 */
public final class Tile extends UnitLocation implements Named, Ownable {

    private static final Logger logger = Logger.getLogger(Tile.class.getName());

    /**
     * This must be distinct from ColonyTile/Building.UNIT_CHANGE or
     * the colony panel can get confused.
     */
    public static final String UNIT_CHANGE = "TILE_UNIT_CHANGE";

    /**
     * Flag to assign to the high seas count to flag that the high seas
     * connectivity needs recalculation after reading in the map.
     */
    public static final int FLAG_RECALCULATE = Integer.MAX_VALUE;

    /**
     * The maximum distance that will still be considered "near" when
     * determining the location name.
     *
     * @see #getLocationName
     */
    public static final int NEAR_RADIUS = 8;

    /**
     * The type of the tile.
     * Beware: this may appear to be null in the client when the tile is
     * unexplored.
     */
    private TileType type;

    /** The tile coordinates in the enclosing map. */
    private int x, y;

    /** The player that consider this tile to be their land. */
    private Player owner;

    /**
     * A pointer to the settlement located on this tile or null if
     * there is no settlement on this tile.
     */
    private Settlement settlement;

    /**
     * Indicates which settlement owns this tile (null indicates no
     * owner).  A colony owns the tile it is located on, and every
     * tile it has claimed by successfully moving a worker on to it.
     * Note that while units and settlements are owned by a player, a
     * tile is owned by a settlement.
     */
    private Settlement owningSettlement;

    /** Stores all Improvements and Resources (if any). */
    private TileItemContainer tileItemContainer;

    /** The region this tile is in. */
    private Region region;

    /** The number of tiles to traverse to get to the high seas. */
    private int highSeasCount = -1;

    /**
     * Does this tile have an explicit moveToEurope state.  If null,
     * just use the defaults (usually not, unless water and on map edge),
     * otherwise use the explicit value provided here.
     */
    private Boolean moveToEurope;

    /** The style of this Tile, as determined by adjacent tiles. */
    private int style;

    /**
     * An artificial contiguous-region number to identify connected
     * parts of the map.  That is, all land tiles with the same
     * contiguity number can be reached by a land unit on any of
     * those tiles in the absence of extra-geographic blockages like
     * settlements and other units.  Similarly for water tiles/naval
     * units.
     *
     * This is used to quickly scope out the sort of paths available
     * to a unit attempting to reach some destination.  It only needs
     * serialization from server to client, as it is set by the
     * TerrainGenerator on map import or creation.
     */
    private int contiguity = -1;

    /**
     * Stores each player's view of this tile.
     * Only initialized when needed, only relevant on the server.
     */
    private java.util.Map<Player, PlayerExploredTile> playerExploredTiles
        = null;


    /**
     * The main tile constructor.
     *
     * @param game The enclosing <code>Game</code>.
     * @param type The <code>TileType</code>.
     * @param locX The x-position of this tile on the map.
     * @param locY The y-position of this tile on the map.
     */
    public Tile(Game game, TileType type, int locX, int locY) {
        super(game);

        this.type = type;

        x = locX;
        y = locY;

        owningSettlement = null;
        settlement = null;

        if (!game.isViewShared()) {
            playerExploredTiles = new HashMap<Player, PlayerExploredTile>();
        }
    }

    /**
     * Create a new <code>Tile</code> with the given identifier.
     * The object should later be initialized by calling either
     * {@link #readFromXML(XMLStreamReader)}.
     *
     * @param game The enclosing <code>Game</code>.
     * @param id The object identifier.
     */
    public Tile(Game game, String id) {
        super(game, id);

        if (!game.isViewShared()) {
            playerExploredTiles = new HashMap<Player, PlayerExploredTile>();
        }
    }


    //
    // Basic accessors and mutators
    //

    /**
     * Gets the type of this Tile.
     *
     * @return The <code>TileType</code>.
     */
    public TileType getType() {
        return type;
    }

    /**
     * Sets the type for this Tile.
     *
     * @param t The new <code>TileType</code> for this <code>Tile</code>.
     */
    public void setType(TileType t) {
        if (t == null) {
            throw new IllegalArgumentException("Tile type must not be null");
        }
        type = t;
        if (tileItemContainer != null) {
            tileItemContainer.removeIncompatibleImprovements();
        }
        if (!isLand()) settlement = null;

        updateColonyTiles();
        updatePlayerExploredTiles();
    }

    /**
     * Check if the tile has been explored.
     *
     * @return True if this is an explored <code>Tile</code>.
     */
    public boolean isExplored() {
        return type != null;
    }

    /**
     * Is this a land tile?
     *
     * @return True if this a land <code>Tile</code>.
     */
    public boolean isLand() {
        return type != null && !type.isWater();
    }

    /**
     * Is this a forested tile?
     *
     * @return True if this is a forested <code>Tile</code>.
     */
    public boolean isForested() {
        return type != null && type.isForested();
    }

    /**
     * Gets the x-coordinate of this tile.
     *
     * @return The x-coordinate of this <code>Tile</code>.
     */
    public int getX() {
        return x;
    }

    /**
     * Gets the y-coordinate of this tile.
     *
     * @return The y-coordinate of this <code>Tile</code>.
     */
    public int getY() {
        return y;
    }

    /**
     * Gets the position of this tile.
     *
     * @return The <code>Position</code> of this <code>Tile</code>.
     */
    public Position getPosition() {
        return new Position(x, y);
    }

    /**
     * Get the map in which this tile belongs.
     *
     * @return The enclosing <code>Map</code>.
     */
    public Map getMap() {
        return getGame().getMap();
    }

    /**
     * Gets the settlement on this tile.
     *
     * @return The <code>Settlement</code> that is located on this
     *     <code>Tile</code>, or null if none is present.
     * @see #setSettlement
     */
    public Settlement getSettlement() {
        return settlement;
    }

    /**
     * Put a settlement onto this tile.  A tile can only have one
     * settlement located on it.  The settlement will also become the
     * owner of this tile.
     *
     * @param settlement A <code>Settlement</code> to put on this
     *     <code>Tile</code>.
     * @see #getSettlement
     */
    public void setSettlement(Settlement s) {
        settlement = s;
    }

    /**
     * Gets the owning settlement for this tile.
     *
     * @return The <code>Settlement</code> that owns this <code>Tile</code>.
     * @see #setOwner
     */
    public Settlement getOwningSettlement() {
        return owningSettlement;
    }

    /**
     * Sets the settlement that owns this tile.
     *
     * @param owner The <code>Settlement</code> to own this <code>Tile</code>.
     * @see #getOwner
     */
    public void setOwningSettlement(Settlement owner) {
        this.owningSettlement = owner;
    }

    /**
     * Gets this tiles <code>TileItemContainer</code>.
     *
     * @return The <code>TileItemContainer</code>.
     */
    public TileItemContainer getTileItemContainer() {
        return tileItemContainer;
    }

    /**
     * Sets the <code>TileItemContainer</code>.
     *
     * @param newTileItemContainer The new <code>TileItemContainer</code> value.
     */
    public void setTileItemContainer(TileItemContainer newTileItemContainer) {
        tileItemContainer = newTileItemContainer;
    }

    /**
     * Get the tile region.
     *
     * @return The tile <code>Region</code>.
     */
    public Region getRegion() {
        return region;
    }

    /**
     * Set the tile region.
     *
     * @param newRegion The new <code>Region</code> value.
     */
    public void setRegion(final Region newRegion) {
        this.region = newRegion;
    }

    /**
     * Get the discoverable region of this tile.
     *
     * @return Any discoverable <code>Region</code>.
     */
    public Region getDiscoverableRegion() {
        return (region == null) ? null : region.getDiscoverableRegion();
    }

    /**
     * Gets whether this tile is connected to the high seas.
     *
     * @return True if this <code>Tile</code> is connected to the high seas.
     */
    public boolean isHighSeasConnected() {
        return highSeasCount >= 0;
    }

    /**
     * Gets the high seas count.
     *
     * @return The high seas count value.
     */
    public int getHighSeasCount() {
        return this.highSeasCount;
    }

    /**
     * Set the high seas count.
     *
     * @param count The new high seas count value.
     */
    public void setHighSeasCount(final int count) {
        this.highSeasCount = count;
    }

    /**
     * Get the move-to-Europe state of the tile.
     *
     * @return The move-to-Europe state of the <code>Tile</code>.
     */
    public Boolean getMoveToEurope() {
        return moveToEurope;
    }

    /**
     * Set the move-to-Europe state of the tile.
     *
     * @param moveToEurope The new move-to-Europe state for the
     *     <code>Tile</code>.
     */
    public void setMoveToEurope(Boolean moveToEurope) {
        this.moveToEurope = moveToEurope;
    }

    /**
     * Can a unit move to the high seas from this tile?
     *
     * @return True if a unit can move to high seas from this tile.
     */
    public boolean isDirectlyHighSeasConnected() {
        return (moveToEurope != null) ? moveToEurope.booleanValue()
            : (type == null) ? false
            : type.isDirectlyHighSeasConnected();
    }

    /**
     * Get the style value.
     *
     * @return The <code>Tile</code> style.
     */
    public int getStyle() {
        return style;
    }

    /**
     * Set the tile style.
     *
     * @param newStyle The new style value.
     */
    public void setStyle(final int newStyle) {
        this.style = newStyle;
    }

    /**
     * Get the contiguity identifier for this tile.
     *
     * @return A contiguity number.
     */
    public int getContiguity() {
        return contiguity;
    }

    /**
     * Sets the contiguity identifier for this tile.
     *
     * @param contiguity A contiguity number.
     */
    public void setContiguity(int contiguity) {
        this.contiguity = contiguity;
    }

    /**
     * Is this tile connected to another across the same contiguous piece
     * of land or water?
     *
     * @param other The other <code>Tile</code> to check.
     * @return True if the <code>Tile</code>s are connected.
     */
    public boolean isConnectedTo(Tile other) {
        return getContiguity() == other.getContiguity();
    }

    /**
     * Gets the <code>PlayerExploredTile</code> for the given player.
     *
     * @param player The <code>Player</code> to query.
     * @return The <code>PlayerExploredTile</code> for the given player,
     *     or null if the <code>Tile</code> has not been explored.
     * @see PlayerExploredTile
     */
    public PlayerExploredTile getPlayerExploredTile(Player player) {
        return (playerExploredTiles == null) ? null
            : playerExploredTiles.get(player);
    }

    /**
     * Get or create the <code>PlayerExploredTile</code> for the given player
     * if on the server.
     *
     * @param player The <code>Player</code> to query.
     * @return The <code>PlayerExploredTile</code> for the given player.
     * @see PlayerExploredTile
     */
    private PlayerExploredTile requirePlayerExploredTile(Player player) {
        if (playerExploredTiles == null) return null;
        PlayerExploredTile pet = playerExploredTiles.get(player);
        if (pet == null) {
            pet = new PlayerExploredTile(getGame(), player, this);
            playerExploredTiles.put(player, pet);
        }
        return pet;
    }


    //
    // Tile Item (LCR, Resource, TileImprovement) handling
    //

    /**
     * Gets a list of <code>TileImprovements</code>.
     *
     * @return A list of all the <code>TileImprovements</code>.
     */
    public List<TileImprovement> getTileImprovements() {
        if (tileItemContainer == null) return Collections.emptyList();
        return tileItemContainer.getImprovements();
    }

    /**
     * Gets a list of completed <code>TileImprovements</code>.
     *
     * @return A list of all completed <code>TileImprovements</code>.
     */
    public List<TileImprovement> getCompletedTileImprovements() {
        if (tileItemContainer == null) return Collections.emptyList();
        return tileItemContainer.getCompletedImprovements();
    }

    /**
     * Does this tile contain a completed improvement of the given type?
     *
     * @param type The <code>TileImprovementType</code> to look for.
     * @param True if there is a completed improvement present.
     */
    public boolean hasTileImprovement(TileImprovementType type) {
        return (tileItemContainer == null) ? false
            : tileItemContainer.hasImprovement(type);
    }

    /**
     * Gets the TileImprovement of a given type, or null if there is no match.
     *
     * @param type The <code>TileImprovementType</code> to look for.
     * @param The <code>TileImprovement</code> of the requested type found,
     *     or null if none.
     */
    public TileImprovement getTileImprovement(TileImprovementType type) {
        return (tileItemContainer == null) ? null
            : tileItemContainer.getImprovement(type);
    }

    /**
     * Does this tile have a LCR?
     *
     * @return True if this <code>Tile</code> has a
     *     <code>LostCityRumour</code> on it.
     */
    public boolean hasLostCityRumour() {
        return tileItemContainer != null
            && tileItemContainer.getLostCityRumour() != null;
    }

    /**
     * Gets a lost city rumour on this tile.
     *
     * @return The <code>LostCityRumour</code> on this
     *     <code>Tile</code>, or null if none found.
     */
    public LostCityRumour getLostCityRumour() {
        return (tileItemContainer == null) ? null
            : tileItemContainer.getLostCityRumour();
    }

    /**
     * Does this tile have a resource?
     *
     * @return True if this is a resource <code>Tile</code>.
     */
    public boolean hasResource() {
        return tileItemContainer != null
            && tileItemContainer.getResource() != null;
    }

    /**
     * Does this tile have a river?
     *
     * @return True if this is a river <code>Tile</code>.
     */
    public boolean hasRiver() {
        return tileItemContainer != null
            && tileItemContainer.getRiver() != null;
    }

    /**
     * Gets the river on this tile.
     *
     * @return A river <code>TileImprovement</code>, or null if none present.
     */
    public TileImprovement getRiver() {
        return (tileItemContainer == null) ? null
            : tileItemContainer.getRiver();
    }

    /**
     * Gets the style of a river improvement on this tile.
     *
     * @return The river <code>TileImprovementStyle</code>.
     */
    public TileImprovementStyle getRiverStyle() {
        TileImprovement river;
        return (tileItemContainer == null) ? null
            : ((river = tileItemContainer.getRiver()) == null) ? null
            : river.getStyle();
    }

    /**
     * Does this tile have a road?
     *
     * @return True if this <code>Tile</code> has a road.
     */
    public boolean hasRoad() {
        return tileItemContainer != null
            && tileItemContainer.getRoad() != null;
    }

    /**
     * Gets the road on this tile.
     *
     * @return A road <code>TileImprovement</code>, or null if none present.
     */
    public TileImprovement getRoad() {
        return (tileItemContainer == null) ? null : tileItemContainer.getRoad();
    }

    /**
     * Adds a tile item to this tile.
     *
     * @param item The <code>TileItem</code> to add.
     * @return True if the item was added.
     */
    private boolean addTileItem(TileItem item) {
        if (item == null) return false;
        if (tileItemContainer == null) {
            tileItemContainer = new TileItemContainer(getGame(), this);
        }
        TileItem added = tileItemContainer.addTileItem(item);
        updatePlayerExploredTiles();
        return added == item;
    }

    /**
     * Removes a tile item from this tile.
     *
     * @param item The <code>TileItem</code> to remove.
     * @return The item removed, or null on failure.
     */
    private <T extends TileItem> T removeTileItem(T item) {
        if (item == null || tileItemContainer == null) return null;
        T result = tileItemContainer.removeTileItem(item);
        updatePlayerExploredTiles();
        return result;
    }

    /**
     * Adds a lost city rumour to this tile.
     *
     * @param rumour The <code>LostCityRumour</code> to add.
     */
    public void addLostCityRumour(LostCityRumour rumour) {
        addTileItem(rumour);
    }

    /**
     * Removes the lost city rumour from this <code>Tile</code> if there
     * is one.
     *
     * @return The removed <code>LostCityRumour</code>.
     */
    public LostCityRumour removeLostCityRumour() {
        return removeTileItem(getLostCityRumour());
    }

    /**
     * Adds a new river to this tile.
     *
     * @param magnitude The magnitude of the river to be created
     * @param conns The encoded river size/connections.
     * @return The new river added, or null on failure.
     */
    public TileImprovement addRiver(int magnitude, String conns) {
        if (magnitude == TileImprovement.NO_RIVER) return null;
        TileImprovementType riverType = getSpecification()
            .getTileImprovementType("model.improvement.river");
        TileImprovement river = new TileImprovement(getGame(), this, riverType);
        river.setTurnsToComplete(0);
        river.setMagnitude(magnitude);
        if (!addTileItem(river)) return null;
        river.updateRiverConnections(conns);
        return river;
    }

    /**
     * Removes a river from this tile.
     *
     * @return The removed river.
     */
    public TileImprovement removeRiver() {
        TileImprovement river = getRiver();
        if (river == null) return null;
        TileImprovement result = removeTileItem(river);
        if (result == river) river.updateRiverConnections(null);
        return result;
    }

    /**
     * Adds a road to this tile.  It is not complete.
     *
     * @return The new road added, or the existing one.
     */
    public TileImprovement addRoad() {
        TileImprovementType roadType = getSpecification()
            .getTileImprovementType("model.improvement.road");
        TileImprovement road = new TileImprovement(getGame(), this, roadType);
        road.setMagnitude(1);
        return (addTileItem(road)) ? road : null;
    }

    /**
     * Removes a road from this tile.
     *
     * @return The removed road.
     */
    public TileImprovement removeRoad() {
        TileImprovement road = getRoad();
        if (road == null) return null;
        TileImprovement result = removeTileItem(road);
        if (result == road) road.updateRoadConnections(false);
        return result;
    }

    /**
     * Adds a resource to this tile.
     *
     * @param resource The <code>Resource</code> to add.
     */
    public void addResource(Resource resource) {
        addTileItem(resource);
    }

    /**
     * This method is called only when a new turn is beginning.  It
     * will reduce the quantity of the bonus <code>Resource</code>
     * that is on the tile, if any and if applicable.
     *
     * @param goodsType The <code>GoodsType</code> the goods type to expend.
     * @param unitType The <code>UnitType</code> doing the production.
     * @return The <code>Resource</code> if it is exhausted by this
     *     call (so it can be used in a message), otherwise null.
     * @see ResourceType
     */
    public Resource expendResource(GoodsType goodsType, UnitType unitType,
                                   Settlement settlement) {
        if (hasResource()
            && tileItemContainer.getResource().getQuantity() != -1) {
            Resource resource = tileItemContainer.getResource();
            // Potential of this Tile and Improvements
            // TODO: review
            int potential = getTileTypePotential(getType(), goodsType, unitType,
                                                 tileItemContainer);
            for (TileItem item : tileItemContainer.getTileItems()) {
                if (item instanceof TileImprovement) {
                    potential = item.applyBonus(goodsType, unitType, potential);
                }
            }

            if (resource.useQuantity(goodsType, unitType, potential) == 0) {
                tileItemContainer.removeTileItem(resource);
                updatePlayerExploredTiles();
                return resource;
            }
        }
        return null;
    }

    /**
     * Get the number of turns it takes for a non-expert pioneer to build
     * the given <code>TileImprovementType</code>. 
     * It will check if it is valid for this <code>TileType</code>.
     *
     * @param workType The <code>TileImprovementType</code> to check.
     * @return The number of turns it should take a non-expert pioneer
     *     to finish the work.
     */
    public int getWorkAmount(TileImprovementType workType) {
        return (workType == null) ? -1
            : (!workType.isTileAllowed(this)) ? -1
            // Return the basic work turns + additional work turns
            : getType().getBasicWorkTurns() + workType.getAddWorkTurns();
    }

    /**
     * Gets a weighted list of natural disasters than can strike
     * this tile.  This list comprises all natural disasters that can
     * strike a tile of this type or a completed tile improvement
     * present.
     *
     * @return A weighted list of <code>Disaster</code>s.
     */
    public List<RandomChoice<Disaster>> getDisasters() {
        List<RandomChoice<Disaster>> disasters
            = new ArrayList<RandomChoice<Disaster>>();
        disasters.addAll(type.getDisasters());
        for (TileImprovement ti : getCompletedTileImprovements()) {
            disasters.addAll(ti.getType().getDisasters());
        }
        return disasters;
    }


    //
    // Naming
    //

    /**
     * Gets a description of the <code>Tile</code>, with the name of
     * the tile and any improvements on it (road/plow/etc) from
     * <code>TileItemContainer</code>.
     *
     * @return The description label for this <code>Tile</code>.
     */
    public StringTemplate getLabel() {
        StringTemplate label = StringTemplate.key(type.getNameKey());
        if (tileItemContainer != null) {
            List<String> keys = new ArrayList<String>();
            for (TileItem item : tileItemContainer.getTileItems()) {
                if (item instanceof Resource) {
                    keys.add(((Resource) item).getType().getNameKey());
                } else if (item instanceof TileImprovement
                           && ((TileImprovement) item).isComplete()) {
                    keys.add(((TileImprovement) item).getType().getNameKey());
                }
            }
            if (!keys.isEmpty()) {
                label = StringTemplate.label("/")
                    .add(type.getNameKey());
                for (String key : keys) {
                    label.add(key);
                }
            }
        }
        return label;
    }


    //
    // Map / geographic routines
    //

    /**
     * Gets the distance in tiles between this tile and the specified
     * one.
     *
     * @param tile The <code>Tile</code> to check the distance to.
     * @return The distance.
     */
    public int getDistanceTo(Tile tile) {
        return getPosition().getDistance(tile.getPosition());
    }

    /**
     * Gets the direction to a neighbouring tile from this one.
     *
     * @param tile The other <code>Tile</code>.
     * @return The direction to the other <code>Tile</code>, or null
     *     if the other tile is not a neighbour.
     */
    public Direction getDirection(Tile tile) {
        return getMap().getDirection(this, tile);
    }

    /**
     * Get the neighbouring tile in the given direction.
     *
     * @param direction The <code>Direction</code> to check in.
     * @return The neighbouring <code>Tile</code> in the given
     *     <code>Direction</code>, or null if none present.
     */
    public Tile getNeighbourOrNull(Direction direction) {
        Position position = getPosition();
        return (!getMap().isValid(position)) ? null
            : getMap().getTile(position.getAdjacent(direction));
    }

    /**
     * Determines whether this tile is adjacent to the specified tile.
     *
     * @param tile A potentially adjacent <code>Tile</code>.
     * @return True if the <code>Tile</code> is adjacent to this
     *     <code>Tile</code>.
     */
    public boolean isAdjacent(Tile tile) {
        return (tile == null) ? false : this.getDistanceTo(tile) == 1;
    }

    /**
     * Gets the adjacent Tile in a given direction.
     *
     * @param direction The <code>Direction</code> to check.
     * @return The adjacent <code>Tile</code> in the specified
     *     direction, or null if invalid.
     */
    public Tile getAdjacentTile(Direction direction) {
        return getMap().getAdjacentTile(getX(), getY(), direction);
    }

    /**
     * Is this tile in the polar regions?
     *
     * @return True if the <code>Tile</code> is polar.
     */
    public boolean isPolar() {
        return getMap().isPolar(this);
    }

    /**
     * Is this a shoreline tile?
     *
     * The tile can be water or land, and the water can be ocean,
     * river or an inland lake.  If this is true for a land tile with
     * a colony, the colony can build docks.
     *
     * @return True if this <code>Tile</code> is on the shore.
     */
    public boolean isShore() {
        for (Tile t : getSurroundingTiles(1)) {
            if (t.isLand() != this.isLand()) return true;
        }
        return false;
    }


    /**
     * Gets all the tiles surrounding a tile within the given range.
     * The center tile is not included.
     *
     * @param range How far away do we need to go starting from this.
     * @return The tiles surrounding this <code>Tile</code>.
     */
    public Iterable<Tile> getSurroundingTiles(int range) {
        return getMap().getCircleTiles(this, true, range);
    }

    /**
     * Gets all the tiles surrounding this tile within the given
     * inclusive upper and lower bounds.
     *
     * getSurroundingTiles(r) is equivalent to getSurroundingTiles(1, r),
     * thus this tile is included if rangeMin is zero.
     *
     * @param rangeMin The inclusive minimum distance from this
     *     <code>Tile</code>.
     * @param rangeMax The inclusive maximum distance from this
     *     <code>Tile</code>.
     * @return A list of the tiles surrounding this <code>Tile</code>.
     */
    public List<Tile> getSurroundingTiles(int rangeMin, int rangeMax) {
        List<Tile> result = new ArrayList<Tile>();
        if (rangeMin > rangeMax || rangeMin < 0) return result;

        if (rangeMin == 0) result.add(this);

        if (rangeMax > 0) {
            for (Tile t : getSurroundingTiles(rangeMax)) {
                // add all tiles up to rangeMax
                result.add(t);
            }
        }
        if (rangeMin > 1) {
            for (Tile t : getSurroundingTiles(rangeMin - 1)) {
                // remove the tiles closer than rangeMin
                result.remove(t);
            }
        }
        return result;
    }

    /**
     * Determine whether this tile has adjacent tiles that are unexplored.
     *
     * @return True if at least one neighbouring <code>Tile</code>s is
     *     unexplored.
     */
    public boolean hasUnexploredAdjacent() {
        for (Tile t : getSurroundingTiles(1)) {
            if (!t.isExplored()) return true;
        }
        return false;
    }

    /**
     * Finds the nearest settlement to this tile.
     *
     * @param owner If non-null, the settlement should be owned by this player.
     * @param radius The maximum radius of the search.
     * @return The nearest settlement, or null if none.
     */
    public Settlement getNearestSettlement(Player owner, int radius) {
        if (radius <= 0) radius = INFINITY;
        Map map = getGame().getMap();
        for (Tile t : map.getCircleTiles(this, true, radius)) {
            if (t == this) continue;
            Settlement settlement = t.getSettlement();
            if (settlement != null
                && (owner == null || settlement.getOwner() == owner)) {
                return settlement;
            }
        }
        return null;
    }

    /**
     * Finds a safe tile to put a unit on, near to this one.
     * Useful on return from Europe.
     *
     * @param player The owner of the unit to place (may be null).
     * @param random An optional pseudo-random number source.
     * @return A vacant <code>Tile</code> near this one.
     */
    public Tile getSafeTile(Player player, Random random) {
        if ((getFirstUnit() == null
                || getFirstUnit().getOwner() == player)
            && (getSettlement() == null
                || getSettlement().getOwner() == player)) {
            return this;
        }

        for (int r = 1; true; r++) {
            List<Tile> tiles = getSurroundingTiles(r, r);
            if (random != null) {
                Utils.randomShuffle(logger, "Safe tile", tiles, random);
            }
            for (Tile t : tiles) {
                if ((t.getFirstUnit() == null
                        || t.getFirstUnit().getOwner() == player)
                    && (t.getSettlement() == null
                        || t.getSettlement().getOwner() == player)) {
                    return t;
                }
            }
        }
    }


    //
    // Ownership
    //

    /**
     * Is this tile under active use?
     *
     * @return True if a <code>Colony</code> is using this <code>Tile</code>.
     */
    public boolean isInUse() {
        return getOwningSettlement() instanceof Colony
            && ((Colony)getOwningSettlement()).isTileInUse(this);
    }

    /**
     * Changes the owning settlement for this tile.
     *
     * @param settlement The new owning <code>Settlement</code> for
     *     this <code>Tile</code>.
     */
    public void changeOwningSettlement(Settlement settlement) {
        if (owningSettlement != null) {
            owningSettlement.removeTile(this);
        }
        setOwningSettlement(settlement);
        if (settlement != null) {
            settlement.addTile(this);
        }
    }

    /**
     * Change the tile ownership.  Also change the owning settlement
     * as the two are commonly related.
     *
     * @param player The <code>Player</code> to own the tile.
     * @param settlement The <code>Settlement</code> to own the
     *     <code>Tile</code>.
     */
    public void changeOwnership(Player player, Settlement settlement) {
        Player old = getOwner();
        setOwner(player);
        changeOwningSettlement(settlement);
        updatePlayerExploredTiles(old);
    }


    //
    // Production
    //

    /**
     * The potential of this tile to produce a certain type of goods.
     *
     * @param goodsType The <code>GoodsType</code> to check the potential for.
     * @param unitType A <code>UnitType</code> to do the work.
     * @return The normal potential of this <code>Tile</code> to
     *     produce the given <code>GoodsType</code>.
     */
    public int potential(GoodsType goodsType, UnitType unitType) {
        return getTileTypePotential(getType(), goodsType, unitType,
                                    getTileItemContainer());
    }

    /**
     * Gets the maximum potential for producing the given type of
     * goods.  The maximum potential is the potential of a tile after
     * the tile has been plowed/built road on.
     *
     * @param goodsType The <code>GoodsType</code> to check.
     * @param unitType A <code>UnitType</code> to do the work.
     * @return The maximum potential.
     */
    public int getMaximumPotential(GoodsType goodsType, UnitType unitType) {
        // If we consider maximum potential to the effect of having
        // all possible improvements done, iterate through the
        // improvements and get the bonuses of all related ones.  If
        // there are options to change tiletype using an improvement,
        // consider that too.
        final Specification spec = getSpecification();
        List<TileType> tileTypes = new ArrayList<TileType>();
        tileTypes.add(type);

        // Add to the list the various possible tile type changes
        for (TileImprovementType impType : spec.getTileImprovementTypeList()) {
            if (impType.getChange(type) != null) {
                // There is an option to change TileType
                tileTypes.add(impType.getChange(type));
            }
        }

        int maxProduction = 0;
        for (TileType tileType : tileTypes) {
            float potential = tileType.getProductionOf(goodsType, unitType);
            if (tileType == type && hasResource()) {
                for (TileItem item : tileItemContainer.getTileItems()) {
                    if (item instanceof Resource) {
                        potential = item.applyBonus(goodsType, unitType,
                                                    (int)potential);
                    }
                }
            }
            for (TileImprovementType ti : spec.getTileImprovementTypeList()) {
                if (ti.isNatural() || !ti.isTileTypeAllowed(tileType)) continue;
                if (ti.getBonus(goodsType) > 0) {
                    potential = ti.getProductionModifier(goodsType)
                        .applyTo(potential);
                }
            }
            maxProduction = Math.max((int)potential, maxProduction);
        }
        return maxProduction;
    }

    /**
     * Get the production modifiers for this tile.
     *
     * @param goodsType The <code>GoodsType</code> to produce.
     * @param unitType A <code>UnitType</code> to do the work.
     * @return A list of production <code>Modifier</code>s.
     */
    public List<Modifier> getProductionModifiers(GoodsType goodsType,
                                                 UnitType unitType) {
        List<Modifier> result = new ArrayList<Modifier>();
        result.addAll(type.getProductionBonus(goodsType));
        if (tileItemContainer != null) {
            result.addAll(tileItemContainer.getProductionModifiers(goodsType,
                                                                   unitType));
        }
        return result;
    }

    /**
     * Calculates the potential of a certain <code>GoodsType</code>.
     *
     * @param tileType A type of tile to produce on.
     * @param goodsType The <code>GoodsType</code> to produce.
     * @param unitType A <code>UnitType</code> to do the work.
     * @param tileItemContainer A <code>TileItemContainer</code> with any
     *     <code>TileItem<code> to give bonuses.
     * @return The amount of goods.
     */
    public static int getTileTypePotential(TileType tileType,
                                           GoodsType goodsType,
                                           UnitType unitType,
                                           TileItemContainer tileItemContainer){
        if (tileType == null || goodsType == null
            || !goodsType.isFarmed()) return 0;
        // Get tile potential + bonus if any
        int potential = tileType.getProductionOf(goodsType, unitType);
        if (tileItemContainer != null) {
            potential = tileItemContainer.getTotalBonusPotential(goodsType,
                unitType, potential, false);
        }
        return potential;
    }

    /**
     * Sort possible goods types according to potential.
     *
     * @return A list of goods, highest potential production first.
     */
    public List<AbstractGoods> getSortedPotential() {
        return getSortedPotential(null, null);
    }

    /**
     * Sort possible goods types according to potential.
     *
     * @param unit the <code>Unit</code> to work on this <code>Tile</code>
     * @return A list of goods, highest potential production first.
     */
    public List<AbstractGoods> getSortedPotential(Unit unit) {
        return getSortedPotential(unit.getType(), unit.getOwner());
    }

    /**
     * Get a comparator to order a list of goods by potential sale value
     * in a market.
     *
     * @param market The <code>Market</code> to evaluate against.
     * @return A comparator.
     */
    public Comparator<AbstractGoods> getMarketGoodsComparator(final Market market) {
        return new Comparator<AbstractGoods>() {
            public int compare(AbstractGoods o, AbstractGoods p) {
                return market.getSalePrice(p.getType(), p.getAmount())
                    - market.getSalePrice(o.getType(), o.getAmount());
            }
        };
    }

    /**
     * Sort possible goods types according to potential.
     *
     * @param unitType The <code>UnitType</code> to do the work.
     * @param owner the <code>Player</code> owning the unit.
     * @return A list of goods, highest potential production first.
     */
    public List<AbstractGoods> getSortedPotential(UnitType unitType,
                                                  Player owner) {
        final Specification spec = getSpecification();
        List<AbstractGoods> goodsTypeList = new ArrayList<AbstractGoods>();
        if (getType() != null) {
            // It is necessary to consider all farmed goods, since the
            // tile might have a resource that produces goods not
            // produced by the tile type.
            for (GoodsType goodsType : spec.getFarmedGoodsTypeList()) {
                int potential = potential(goodsType, unitType);
                if (potential > 0) {
                    goodsTypeList.add(new AbstractGoods(goodsType, potential));
                }
            }
            Collections.sort(goodsTypeList,
                (owner == null || owner.getMarket() == null)
                ? AbstractGoods.goodsAmountComparator
                : getMarketGoodsComparator(owner.getMarket()));
        }
        return goodsTypeList;
    }


    //
    // ColonyTile and PlayerExploredTile maintenance
    //

    /**
     * Update player explored tiles after a change to this tile.
     */
    private void updateColonyTiles() {
        for (Player player : getGame().getLiveEuropeanPlayers()) {
            for (Colony colony : player.getColonies()) {
                for (ColonyTile colonyTile : colony.getColonyTiles()) {
                    if (colonyTile.getWorkTile() == this) {
                        colonyTile.updateProductionType();
                    }
                }
            }
        }
    }

    /**
     * Updates the <code>PlayerExploredTile</code> for each player.
     * This update will only be performed if the player
     * {@link Player#canSee(Tile) can see} this <code>Tile</code>.
     */
    public void updatePlayerExploredTiles() {
        updatePlayerExploredTiles(null);
    }

    /**
     * Updates the <code>PlayerExploredTile</code> for each player.
     * This update will only be performed if the player
     * {@link Player#canSee(Tile) can see} this <code>Tile</code>.
     *
     * @param oldPlayer The optional <code>Player</code> that formerly
     *     had visibility of this <code>Tile</code> and should see the change.
     */
    public void updatePlayerExploredTiles(Player oldPlayer) {
        if (playerExploredTiles == null) return;
        for (Player player : getGame().getLiveEuropeanPlayers()) {
            if (player == oldPlayer || player.canSee(this)) {
                updatePlayerExploredTile(player, false);
            }
        }
    }

    /**
     * Updates the information about this <code>Tile</code> for the given
     * <code>Player</code>.
     *
     * @param player The <code>Player</code>.
     * @param full If true, also update any hidden information specific to a
     *     settlement present on the <code>Tile</code>.
     */
    public void updatePlayerExploredTile(Player player, boolean full) {
        if (playerExploredTiles == null || !player.isEuropean()) return;
        PlayerExploredTile pet = requirePlayerExploredTile(player);
        pet.update(full);
    }

    /**
     * Checks if this <code>Tile</code> has been explored by the given
     * <code>Player</code>.
     *
     * @param player The <code>Player</code>.
     * @return True if this <code>Tile</code> has been explored
     *     by the given <code>Player</code>.
     */
    public boolean isExploredBy(Player player) {
        if (!player.isEuropean()) return true;
        if (!isExplored()) return false;
        return getPlayerExploredTile(player) != null;
    }

    /**
     * Sets this <code>Tile</code> to be explored by the given
     * <code>Player</code>.
     *
     * @param player The <code>Player</code>.
     * @param explored True if this <code>Tile</code> should be
     *     explored by the given <code>Player</code>.
     */
    public void setExploredBy(Player player, boolean explored) {
        if (!player.isEuropean()) return;
        if (explored) {
            updatePlayerExploredTile(player, false);
        } else {
            if (playerExploredTiles != null) {
                playerExploredTiles.remove(player);
            }
        }
    }

    /**
     * This is a hack.  When a missionary is removed, its player
     * disposes of it.  However they can still exist in the PETs.
     * Ideally players that can not see the change should still see
     * the old missionary, but referring to a disposed unit is a Bad
     * Thing.  For now, we clean up the PET-missionaries but do not
     * explicitly update the rest of the PET.  This needs to go away
     * at next save-break when we properly virtualize the settlements.
     *
     * @param old The old missionary <code>Unit</code> to fix.
     */
    public void fixMissionary(Unit old) {
        for (PlayerExploredTile pet : playerExploredTiles.values()) {
            if (pet.getMissionary() == old) pet.setMissionary(null);
        }
    }


    //
    // Unit manipulation
    //

    /**
     * Gets a unit that can become active.  This is preferably a unit
     * not currently performing any work.
     *
     * @return A <code>Unit</code> with moves left, or null if none found.
     */
    public Unit getMovableUnit() {
        if (getFirstUnit() != null) {
            Iterator<Unit> unitIterator = getUnitIterator();
            while (unitIterator.hasNext()) {
                Unit u = unitIterator.next();

                Iterator<Unit> childUnitIterator = u.getUnitIterator();
                while (childUnitIterator.hasNext()) {
                    Unit childUnit = childUnitIterator.next();

                    if (childUnit.getMovesLeft() > 0
                        && childUnit.getState() == Unit.UnitState.ACTIVE) {
                        return childUnit;
                    }
                }

                if (u.getMovesLeft() > 0
                    && u.getState() == Unit.UnitState.ACTIVE) {
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
     * Gets the unit that is currently defending this tile.
     * <p>If this tile has a settlement, the units inside the
     * settlement are also considered as potential defenders.
     * <p>As this method is quite expensive, it should not be used to
     * test for the presence of enemy units.
     *
     * @param attacker The <code>Unit</code> that would be attacking
     *     this <code>Tile</code>.
     * @return The <code>Unit</code> that has been chosen to defend this
     *     <code>Tile</code>.
     */
    public Unit getDefendingUnit(Unit attacker) {
        CombatModel cm = getGame().getCombatModel();
        Unit defender = null;
        float defenderPower = -1.0f;
        float power;

        // Check the units on the tile...
        for (Unit u : getUnitList()) {
            if (isLand() != u.isNaval()) {
                // On land, ships are normally docked in port and
                // cannot defend.  Except if beached (see below).
                // On ocean tiles, land units behave as ship cargo and
                // can not defend.
                power = cm.getDefencePower(attacker, u);
                if (Unit.betterDefender(defender, defenderPower, u, power)) {
                    defender = u;
                    defenderPower = power;
                }
            }
        }

        // ...then a settlement defender if any...
        if ((defender == null || !defender.isDefensiveUnit())
            && getSettlement() != null) {
            Unit u = null;
            try {
                // HACK: The AI is prone to removing all units in a
                // settlement which causes Colony.getDefendingUnit()
                // to throw.
                u = settlement.getDefendingUnit(attacker);
            } catch (IllegalStateException e) {
                logger.log(Level.WARNING, "Empty settlement: "
                    + settlement.getName(), e);
            }
            // This routine can be called on the client for the pre-combat
            // popup where enemy settlement defenders are not visible,
            // thus u == null is valid.
            if (u != null) {
                power = cm.getDefencePower(attacker, u);
                if (Unit.betterDefender(defender, defenderPower, u, power)) {
                    defender = u;
                    defenderPower = power;
                }
            }
        }

        // ...finally, if we have failed to find a valid defender
        // for a land tile, allow a beached naval unit to defend (and
        // lose) as a last resort.
        if (defender == null && isLand()) defender = getFirstUnit();

        return defender;
    }

    /**
     * Gets the unit that is occupying the tile.
     *
     * @return The <code>Unit</code> that is occupying this <code>Tile</code>.
     * @see #isOccupied()
     */
    public Unit getOccupyingUnit() {
        Unit unit = getFirstUnit();
        Player owner = null;
        if (getOwningSettlement() != null) {
            owner = getOwningSettlement().getOwner();
        }
        if (owner != null && unit != null && unit.getOwner() != owner
            && unit.getOwner().atWarWith(owner)) {
            for (Unit enemyUnit : getUnitList()) {
                if (enemyUnit.isOffensiveUnit()) return enemyUnit;
            }
        }
        return null;
    }

    /**
     * Checks whether there is an enemy unit occupying this tile.
     * Units can not produce in occupied tiles.
     *
     * @return True if an enemy unit is occupying this <code>Tile</code>.
     */
    public boolean isOccupied() {
        return getOccupyingUnit() != null;
    }


    //
    // Scratch tile routines for the AI
    //

    /**
     * Creates a temporary copy of this tile for planning purposes.
     * The copy is identical except:
     *   - it is not present on the map
     *   - it has no owner, owning settlement or settlement
     * The latter is not a problem as the main use of this routine
     * is by Colony.getScratchColony() which needs to change these fields
     * anyway.
     * Note that the following fields are shared--- do not mutate them!
     *   + The tile item container.
     *   + The player explored tiles.
     * Colony.getCorrespondingWorkLocation() depends on the tics being shared.
     *
     * @return A scratch version of this <code>Tile</code>.
     */
    public Tile getScratchTile() {
        Game game = getGame();
        Tile scratch = new Tile(game, type, x, y);
        scratch.owner = null;
        scratch.settlement = null;
        scratch.owningSettlement = null;
        if (tileItemContainer == null) {
            tileItemContainer = new TileItemContainer(getGame(), this);
        }
        scratch.tileItemContainer = tileItemContainer;
        scratch.playerExploredTiles = playerExploredTiles;
        scratch.region = region;
        scratch.highSeasCount = highSeasCount;
        scratch.moveToEurope = moveToEurope;
        scratch.style = style;
        return scratch;
    }

    /**
     * Special handling on dispose to avoid mutating the shared fields.
     */
    public void disposeScratchTile() {
        tileItemContainer = null;
        playerExploredTiles = null;
        dispose();
    }


    // Interface Location
    //   getSettlement and getColony are simple accessors of Tile
    // Inherits
    //   FreeColObject.getId
    //   UnitLocation.getUnitCount
    //   UnitLocation.getUnitList
    //   UnitLocation.getGoodsContainer

    /**
     * {@inheritDoc}
     */
    public Tile getTile() {
        return this;
    }
    
    /**
     * {@inheritDoc}
     */
    public StringTemplate getLocationName() {
        if (settlement == null) {
            Settlement nearSettlement = null;
            for (Tile tile: getSurroundingTiles(NEAR_RADIUS)) {
                nearSettlement = tile.getSettlement();
                if (nearSettlement != null) {
                    int x = getX() - tile.getX();
                    int y = getY() - tile.getY();
                    double theta = Math.atan2(y, x) + Math.PI/2 + Math.PI/8;
                    if (theta < 0) {
                        theta += 2*Math.PI;
                    }
                    Direction direction = Direction.values()[(int)Math.floor(theta / (Math.PI/4))];

                    return StringTemplate.template("nameLocation")
                        .add("%name%", type.getNameKey())
                        .addStringTemplate("%location%", StringTemplate.template("nearLocation")
                            .add("%direction%", "direction." + direction.toString())
                            .addName("%location%", nearSettlement.getName()));
                }
            }
            if (region != null && region.getName() != null) {
                return StringTemplate.template("nameLocation")
                    .add("%name%", type.getNameKey())
                    .add("%location%", region.getNameKey());
            } else {
                return StringTemplate.key(type.getNameKey());
            }
        } else {
            return settlement.getLocationName();
        }
    }

    /**
     * {@inheritDoc}
     */
    public StringTemplate getLocationNameFor(Player player) {
        return (settlement == null) ? getLocationName()
            : settlement.getLocationNameFor(player);
    }

    /**
     * {@inheritDoc}
     */
    public boolean add(Locatable locatable) {
        if (locatable instanceof TileItem) {
            return addTileItem((TileItem) locatable);

        } else if (locatable instanceof Unit) {
            if (super.add(locatable)) {
                ((Unit)locatable).setState(Unit.UnitState.ACTIVE);
                return true;
            }
            return false;

        } else {
            return super.add(locatable);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(Locatable locatable) {
        if (locatable instanceof TileItem) {
            return removeTileItem((TileItem)locatable) == (TileItem)locatable;

        } else {
            return super.remove(locatable);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(Locatable locatable) {
        if (locatable instanceof TileItem) {
            return tileItemContainer != null
                && tileItemContainer.contains((TileItem) locatable);
        } else {
            return super.contains(locatable);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean canAdd(Locatable locatable) {
        if (locatable instanceof Unit) {
            return ((Unit)locatable).isTileAccessible(this);
        } else if (locatable instanceof TileImprovement) {
            return ((TileImprovement)locatable).getType()
                .isTileTypeAllowed(getType());
        } else {
            return false;
        }
    }


    // Interface Named

    /**
     * {@inheritDoc}
     */
    public String getNameKey() {
        if (getGame().isViewShared()) {
            if (isExplored()) {
                return getType().getNameKey();
            } else {
                return "unexplored";
            }
        } else {
            Player player = getGame().getCurrentPlayer();
            if (player != null) {
                PlayerExploredTile pet = getPlayerExploredTile(player);
                return (pet != null) ? getType().getNameKey() : "unexplored";
            } else {
                logger.warning("player == null");
                return "";
            }
        }
    }


    // Interface Ownable

    /**
     * {@inheritDoc}
     */
    public Player getOwner() {
        return owner;
    }

    /**
     * {@inheritDoc}
     */
    public void setOwner(Player owner) {
        this.owner = owner;
    }


    // Override FreeColGameObject

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        if (settlement != null) {
            settlement.dispose();
            settlement = null;
        }
        if (tileItemContainer != null) {
            tileItemContainer.dispose();
            tileItemContainer = null;
        }
        super.dispose();
    }

    //
    // Miscellaneous low level
    //

    /**
     * Check for any tile integrity problems.
     *
     * @param fix Fix problems if possible.
     * @return Negative if there are problems remaining, zero if
     *     problems were fixed, positive if no problems found at all.
     */
    public int checkIntegrity(boolean fix) {
        int result = 1;
        for (TileImprovement ti : getTileImprovements()) {
            result = Math.min(result, ti.checkIntegrity(fix));
        }
        if (type == null) result = -1;
        return result;
    }


    // Serialization

    private static final String CONNECTED_TAG = "connected";
    private static final String CONTIGUITY_TAG = "contiguity";
    private static final String MOVE_TO_EUROPE_TAG = "moveToEurope";
    private static final String OWNER_TAG = "owner";
    private static final String OWNING_SETTLEMENT_TAG = "owningSettlement";
    private static final String PLAYER_TAG = "player";
    private static final String REGION_TAG = "region";
    private static final String STYLE_TAG = "style";
    private static final String TYPE_TAG = "type";
    private static final String X_TAG = "x";
    private static final String Y_TAG = "y";
    // @compat 0.10.1
    public static final String OLD_UNITS_TAG = "units";
    // end @compat


    /**
     * Write a minimal version of this tile.  This is a special case
     * used when the player has not explored the tile yet.
     *
     * @param out The target stream.
     * @exception XMLStreamException if there is problem writing to the stream.
     */
    public void toXMLMinimal(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());

        super.writeAttributes(out);

        writeAttribute(out, X_TAG, this.x);

        writeAttribute(out, Y_TAG, this.y);

        writeAttribute(out, STYLE_TAG, style);

        if (moveToEurope != null) {
            writeAttribute(out, MOVE_TO_EUROPE_TAG,
                           moveToEurope.booleanValue());
        }

        out.writeEndElement();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void toXMLImpl(XMLStreamWriter out, Player player,
                             boolean showAll,
                             boolean toSavedGame) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName(), player, showAll, toSavedGame);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out, Player player,
                                   boolean showAll,
                                   boolean toSavedGame) throws XMLStreamException {
        PlayerExploredTile pet;

        super.writeAttributes(out);

        writeAttribute(out, X_TAG, this.x);

        writeAttribute(out, Y_TAG, this.y);

        writeAttribute(out, STYLE_TAG, style);

        writeAttribute(out, TYPE_TAG, type);

        writeAttribute(out, REGION_TAG, region);

        if (moveToEurope != null) {
            writeAttribute(out, MOVE_TO_EUROPE_TAG,
                           moveToEurope.booleanValue());
        }

        // Compatibility note: this used to be a boolean
        if (highSeasCount >= 0) {
            writeAttribute(out, CONNECTED_TAG, highSeasCount);
        }

        if (contiguity >= 0) {
            writeAttribute(out, CONTIGUITY_TAG, contiguity);
        }

        if (showAll || toSavedGame || player.canSee(this)) {
            if (owner != null) {
                writeAttribute(out, OWNER_TAG, owner);
            }
            if (owningSettlement != null) {
                writeAttribute(out, OWNING_SETTLEMENT_TAG, owningSettlement);
            }

        } else if ((pet = getPlayerExploredTile(player)) != null) {
            if (pet.getOwner() != null) {
                writeAttribute(out, OWNER_TAG, pet.getOwner());
            }

            if (pet.getOwningSettlement() != null) {
                writeAttribute(out, OWNING_SETTLEMENT_TAG,
                    pet.getOwningSettlement());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(XMLStreamWriter out, Player player,
                                 boolean showAll,
                                 boolean toSavedGame) throws XMLStreamException {
        PlayerExploredTile pet;

        if (showAll || toSavedGame || player.canSee(this)) {
            if (settlement != null) {
                settlement.toXML(out, player, showAll, toSavedGame);
            }
            // Show enemy units if there is no enemy settlement.
            if ((showAll || toSavedGame || settlement == null
                    || settlement.getOwner() == player)
                && !isEmpty()) {
                super.writeChildren(out, player, showAll, toSavedGame);
            }

        } else if ((pet = getPlayerExploredTile(player)) != null) {
            // Only display the settlement if we know it owns the tile
            // and we have a useful level of information about it.
            // This is a compromise, but something more precise is too
            // complex for the present.
            if (settlement != null
                && settlement == pet.getOwningSettlement()
                && settlement.getOwner() == pet.getOwner()
                && !(settlement instanceof Colony
                    && pet.getColonyUnitCount() <= 0)) {
                settlement.toXML(out, player, showAll, toSavedGame);
            }
        }

        if (tileItemContainer != null) {
            tileItemContainer.toXML(out, player, showAll, toSavedGame);
        }

        // Save the pets to saved games.
        if (toSavedGame && playerExploredTiles != null) {
            for (Entry<Player, PlayerExploredTile> entry
                     : playerExploredTiles.entrySet()) {
                entry.getValue().toXML(out, entry.getKey(),
                    showAll, toSavedGame);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        final Specification spec = getSpecification();

        super.readAttributes(in);

        x = getAttribute(in, X_TAG, 0);

        y = getAttribute(in, Y_TAG, 0);

        style = getAttribute(in, STYLE_TAG, 0);

        type = spec.getType(in, TYPE_TAG, TileType.class, (TileType)null);

        style = getAttribute(in, STYLE_TAG, 0);

        String typeStr = getAttribute(in, TYPE_TAG, (String)null);
        type = (typeStr == null) ? null : spec.getTileType(typeStr);
        // TODO: This is better, use when dropping the @compat 0.10.5 below
        //   type = spec.getType(in, TYPE_TAG, TileType.class, (TileType)null);

        String str = getAttribute(in, CONNECTED_TAG, (String)null);
        if (str == null || "".equals(str)) {
            highSeasCount = -1;
            // @compat 0.10.5
            // High seas should have connected==0.  If it does not, this
            // is probably an old save file, so flag a recalculation.
            if ("model.tile.highSeas".equals(typeStr)) {
                highSeasCount = Tile.FLAG_RECALCULATE;
            }
            // @end compatibility code
        } else {
            try {
                highSeasCount = Integer.parseInt(str);
            } catch (NumberFormatException nfe) {
                highSeasCount = -1;
                // @compat 0.10.5
                // < 0.10.6 used to have a simple boolean connected
                // attribute, but it is now highSeasCount, the number of
                // tiles to get to a tile where a unit can move
                // directly to the high seas.
                highSeasCount = Tile.FLAG_RECALCULATE;
                // @end compatibility code
            }
        }

        owner = makeFreeColGameObject(in, OWNER_TAG, Player.class);

        region = makeFreeColGameObject(in, REGION_TAG, Region.class);

        moveToEurope = (hasAttribute(in, MOVE_TO_EUROPE_TAG))
            ? new Boolean(getAttribute(in, MOVE_TO_EUROPE_TAG, false))
            : null;

        contiguity = getAttribute(in, CONTIGUITY_TAG, -1);

        Location loc = makeLocationAttribute(in, OWNING_SETTLEMENT_TAG,
                                             getGame());
        if (loc == null || loc instanceof Settlement) {
            changeOwningSettlement((Settlement)loc);
        } else {
            logger.warning("Settlement expected: " + currentTag(in));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(XMLStreamReader in) throws XMLStreamException {
        // Clear containers.
        Settlement oldSettlement = settlement;
        Player oldSettlementOwner = (settlement == null) ? null
            : settlement.getOwner();
        settlement = null;

        super.readChildren(in);

        // Player settlement list is not passed in player updates
        // so do it here.  TODO: something better.
        Player settlementOwner = (settlement == null) ? null
            : settlement.getOwner();
        if (settlement == null && oldSettlement != null) {
            // Settlement disappeared
            logger.info("Settlement " + oldSettlement.getName() + " removed.");
            oldSettlementOwner.removeSettlement(oldSettlement);
        } else if (settlement != null && oldSettlement == null) {
            // Settlement appeared
            settlementOwner.addSettlement(settlement);
            owner = settlementOwner;
        } else if (settlementOwner != oldSettlementOwner) {
            // Settlement changed owner
            logger.info("Settlement " + oldSettlement.getName() + " captured"
                + " from " + oldSettlement.getOwner()
                + " to " + settlementOwner);
            settlement.setOwner(settlementOwner);
            oldSettlementOwner.removeSettlement(oldSettlement);
            settlementOwner.addSettlement(settlement);
            owner = settlementOwner;
        }

        if (getColony() != null && getColony().isTileInUse(this)) {
            getColony().invalidateCache();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        final String tag = in.getLocalName();

        // @compat 0.10.1
        if (OLD_UNITS_TAG.equals(tag)) {
            while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
                super.readChild(in);
            }
        // end @compat

        } else if (Colony.getXMLElementTagName().equals(tag)) {
            settlement = readFreeColGameObject(in, Colony.class);

        } else if (IndianSettlement.getXMLElementTagName().equals(tag)) {
            settlement = readFreeColGameObject(in, IndianSettlement.class);

        } else if (PlayerExploredTile.getXMLElementTagName().equals(tag)) {
            // Only from a saved game.
            Player player = requireFreeColGameObject(in, PLAYER_TAG,
                                                     Player.class);
            PlayerExploredTile pet = readFreeColGameObject(in,
                PlayerExploredTile.class);
            playerExploredTiles.put(player, pet);

        } else if (TileItemContainer.getXMLElementTagName().equals(tag)) {
            tileItemContainer = readFreeColGameObject(in, TileItemContainer.class);

        } else {
            super.readChild(in);
        }

        // Fix bug where missionary locations get cleared.
        // TODO: Remove this when PETs have been revised to not store
        // the actual unit.
        if (settlement instanceof IndianSettlement) {
            Unit missionary = ((IndianSettlement)settlement).getMissionary();
            if (missionary != null) missionary.setLocationNoUpdate(settlement);
        }
    }

    /**
     * Fixes visible pets where there is a settlement present but the
     * tile is not owned correctly as ownership was not implemented in
     * 0.9.x.
     * Need to do this after reading the game so that canSee() is valid.
     * TODO: remove when 0.9.x is not supported.
     */
    public void fixup09x() {
        if (playerExploredTiles == null) return;
        for (Entry<Player, PlayerExploredTile> e
                 : playerExploredTiles.entrySet()) {
            Player p = e.getKey();
            PlayerExploredTile pet = e.getValue();
            if (settlement != null) {
                if (pet.getOwner() == null
                    || pet.getOwningSettlement() == null) {
                    if (p.canSee(this)) {
                        // Correct with an ordinary update
                        pet.update(false);
                    } else if (settlement instanceof Colony) {
                        if (pet.getColonyUnitCount() > 0) {
                            // Have seen the colony, update the ownership
                            // and the stockade level but not the unit count
                            // as that is the one that was seen.
                            pet.setOwner(settlement.getOwner());
                            pet.setOwningSettlement(settlement);
                            pet.setColonyStockadeKey(((Colony) settlement)
                                .getStockadeKey());
                        }
                    } else if (settlement instanceof IndianSettlement) {
                        // Unclear what has been seen, update just the ownership
                        pet.setOwner(settlement.getOwner());
                        pet.setOwningSettlement(settlement);
                    }
                }
            } else {
                if (pet.getOwningSettlement() != null
                    && pet.getOwner() == null) {
                    pet.setOwner(pet.getOwningSettlement().getOwner());
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append("[").append(getId())
            .append(" ").append((type == null) ? "unknown"
                : Utils.lastPart(type.getId(), "."))
            .append(" ").append(x).append(",").append(y)
            .append((getSettlement() == null) ? ""
                : " " + getSettlement().getName())
            .append("]");
        return sb.toString();
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "tile".
     */
    public static String getXMLElementTagName() {
        return "tile";
    }
}
