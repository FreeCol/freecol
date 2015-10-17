/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Direction;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.RandomChoice;
import static net.sf.freecol.common.util.RandomUtils.*;


/**
 * Represents a single tile on the <code>Map</code>.
 *
 * @see Map
 */
public final class Tile extends UnitLocation implements Named, Ownable {

    private static final Logger logger = Logger.getLogger(Tile.class.getName());

    /** Comparator to sort tiles by increasing distance from the edge. */
    public static final Comparator<Tile> edgeDistanceComparator
        = Comparator.comparingInt(Tile::getEdgeDistance);

    /**
     * Information that is internal to the native settlements, and only
     * updated on close contact.
     */
    private static class IndianSettlementInternals {

        /** The skill taught at the settlement. */
        public UnitType skill = null;

        /** The goods the settlement is interested in. */
        public GoodsType[] wantedGoods = null;


        /**
         * Update the internal information from a native settlement.
         *
         * @param indianSettlement The <code>IndianSettlement</code> to update.
         */
        public void update(IndianSettlement indianSettlement) {
            setValues(indianSettlement.getLearnableSkill(),
                      indianSettlement.getWantedGoods());
        }

        /**
         * Set the internal values.
         *
         * @param skill The skill taught.
         * @param wanted The wanted goods.
         */
        public void setValues(UnitType skill, GoodsType[] wanted) {
            this.skill = skill;
            if (wanted == null) {
                this.wantedGoods = null;
            } else {
                if (this.wantedGoods == null) {
                    this.wantedGoods
                        = new GoodsType[IndianSettlement.WANTED_GOODS_COUNT];
                }
                System.arraycopy(wanted, 0, this.wantedGoods, 0,
                    Math.min(wanted.length, this.wantedGoods.length));
            }
        }
    }

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
     * Warn about colonies that can not produce this amount of
     * a building material.
     */
    private static final int LOW_PRODUCTION_WARNING_VALUE = 4;

    /**
     * The maximum distance that will still be considered "near" when
     * determining the location name.
     *
     * @see #getLocationLabel
     */
    public static final int NEAR_RADIUS = 8;

    public static final int OVERLAY_ZINDEX = 100;
    public static final int FOREST_ZINDEX = 200;
    public static final int RESOURCE_ZINDEX = 400;
    public static final int RUMOUR_ZINDEX = 500;

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

    /** A map of cached tiles for each European player, null in clients. */
    private final java.util.Map<Player, Tile> cachedTiles;

    /**
     * A map of native settlement internals for each European player,
     * null in clients.
     */
    private final java.util.Map<Player, IndianSettlementInternals> playerIndianSettlements;

    /** A comparator to sort for best defence value. */
    private final Comparator<Tile> defenceValueComparator
        = new Comparator<Tile>() {
                public int compare(Tile t1, Tile t2) {
                    double f = t2.getDefenceValue() - t1.getDefenceValue();
                    return (f < 0.0) ? -1 : (f > 0.0) ? 1 : 0;
                }
            };


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
        this.x = locX;
        this.y = locY;
        this.owningSettlement = null;
        this.settlement = null;

        if (game.isInServer()) {
            this.cachedTiles = new HashMap<>();
            this.playerIndianSettlements = new HashMap<>();
        } else {
            this.cachedTiles = null;
            this.playerIndianSettlements = null;
        }
    }

    /**
     * Create a new <code>Tile</code> with the given identifier.
     * The object should later be initialized by calling either
     * {@link #readFromXML(FreeColXMLReader)}.
     *
     * @param game The enclosing <code>Game</code>.
     * @param id The object identifier.
     */
    public Tile(Game game, String id) {
        super(game, id);

        if (game.isInServer()) {
            this.cachedTiles = new HashMap<>();
            this.playerIndianSettlements = new HashMap<>();
        } else {
            this.cachedTiles = null;
            this.playerIndianSettlements = null;
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
     * -til: Changes appearance.
     *
     * @param t The new <code>TileType</code> for this <code>Tile</code>.
     */
    public void setType(TileType t) {
        type = t;
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
    @Override
    public Settlement getSettlement() {
        return settlement;
    }

    /**
     * Put a settlement onto this tile.  A tile can only have one
     * settlement located on it.  The settlement will also become the
     * owner of this tile.
     *
     * -til: Changes appearance.
     *
     * @param settlement A <code>Settlement</code> to put on this
     *     <code>Tile</code>.
     * @see #getSettlement
     */
    public void setSettlement(Settlement settlement) {
        this.settlement = settlement;
    }

    /**
     * Does this tile have a settlement.
     *
     * @return True if there is a settlement present.
     */
    public boolean hasSettlement() {
        return settlement != null;
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
     * -til: Changes appearance.
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
     * -til: Changes appearance.
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
     * Is this a land tile on the sea coast (lakes do not count).
     *
     * @return True if this is a coastland tile.
     */
    public boolean isCoastland() {
        return isLand() && getHighSeasCount() > 0;
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
        return (moveToEurope != null) ? moveToEurope
            : (type == null) ? false
            : type.isDirectlyHighSeasConnected();
    }

    /**
     * Is this tile on a river corner?
     *
     * @return True if this is a river corner.
     */
    public boolean isRiverCorner() {
        List<Tile> tiles = getSurroundingTiles(0, 1).stream()
            .filter(Tile::isOnRiver).collect(Collectors.toList());
        switch (tiles.size()) {
        case 0: case 1:
            return false;
        case 2:
            return tiles.get(0).isAdjacent(tiles.get(1));
        case 3:
            return tiles.get(0).isAdjacent(tiles.get(1))
                || tiles.get(1).isAdjacent(tiles.get(2))
                || tiles.get(2).isAdjacent(tiles.get(0));
        default:
            break;
        }
        return true;
    }

    /**
     * Get the minimum distance in tiles from this tile to the map edge.
     *
     * @return The distance to the edge.
     */
    private int getEdgeDistance() {
        final Map map = getMap();
        final int x = getX(), y = getY();
        return Math.min(Math.min(x, map.getWidth() - x),
                        Math.min(y, map.getHeight() - y));
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
     * -til: Changes appearance.
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
     * Get the adjacent tiles that have a given contiguity.
     *
     * @param contiguity The contiguity to search for.
     * @return A set of <code>Tile</code>s with the required contiguity.
     */
    public Set<Tile> getContiguityAdjacent(int contiguity) {
        Set<Tile> ret = new HashSet<>();
        for (Tile t : getSurroundingTiles(1)) {
            if (t.getContiguity() == contiguity) ret.add(t);
        }
        return ret;
    }

    /**
     * Is this tile on or adjacent to a navigable river but not the ocean.
     *
     * @return True if on a navigable river.
     */
    public boolean isOnRiver() {
        final TileType greatRiver
            = getSpecification().getTileType("model.tile.greatRiver");
        final TileType ocean
            = getSpecification().getTileType("model.tile.ocean");
        boolean ret = getType() == greatRiver;
        for (Tile t : getSurroundingTiles(1)) {
            if (t.getType() == ocean) return false;
            ret |= t.getType() == greatRiver;
        }
        return ret;
    }

    /**
     * Quick test whether this tile is trivially blocked to moves from
     * a unit.  This is a simplification, use getMoveType().isProgress()
     * for the full details.
     *
     * @param unit The <code>Unit</code> to test.
     * @return True if the unit can not move to this tile.
     */
    public boolean isBlocked(Unit unit) {
        Player owner = unit.getOwner();

        Unit u = getFirstUnit();
        if (u != null && !owner.owns(u)) return true; // Blocked by unit

        if (isLand()) {
            Settlement s = getSettlement();
            if (unit.isNaval()) {
                return s == null || !owner.owns(s); // Land, not our settlement
            } else {
                return s != null && !owner.owns(s); // Not our settlement
            }
        } else {
            return !unit.isNaval(); // Can not swim
        }
    }
       
    /**
     * Gets the <code>IndianSettlementInternals</code> for the given player.
     *
     * @param player The <code>Player</code> to query.
     * @return The <code>IndianSettlementInternals</code> for the given player,
     *     or null if none present.
     */
    private IndianSettlementInternals getPlayerIndianSettlement(Player player) {
        return (playerIndianSettlements == null) ? null
            : playerIndianSettlements.get(player);
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
        return (tileItemContainer == null)
            ? Collections.<TileImprovement>emptyList()
            : tileItemContainer.getImprovements();
    }

    /**
     * Gets a list of completed <code>TileImprovements</code>.
     *
     * @return A list of all completed <code>TileImprovements</code>.
     */
    public List<TileImprovement> getCompletedTileImprovements() {
        return (tileItemContainer == null)
            ? Collections.<TileImprovement>emptyList()
            : tileItemContainer.getCompletedImprovements();
    }

    /**
     * Does this tile contain a completed improvement of the given type?
     *
     * @param type The <code>TileImprovementType</code> to look for.
     * @return True if there is a completed improvement present.
     */
    public boolean hasTileImprovement(TileImprovementType type) {
        return (type.isChangeType()) ? type.changeContainsTarget(getType())
            : (tileItemContainer == null) ? false
            : tileItemContainer.hasImprovement(type);
    }

    /**
     * Gets the TileImprovement of a given type, or null if there is no match.
     *
     * @param type The <code>TileImprovementType</code> to look for.
     * @return The <code>TileImprovement</code> of the requested type found,
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
     * -til: Changes appearance.
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
        return added == item;
    }

    /**
     * Removes a tile item from this tile.
     *
     * -til: Changes appearance.
     *
     * @param item The <code>TileItem</code> to remove.
     * @return The item removed, or null on failure.
     */
    private <T extends TileItem> T removeTileItem(T item) {
        if (item == null || tileItemContainer == null) return null;
        return tileItemContainer.removeTileItem(item);
    }

    /**
     * Adds a lost city rumour to this tile.
     *
     * -til: Changes appearance.
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
     * -til: Changes appearance.
     *
     * @return The removed <code>LostCityRumour</code>.
     */
    public LostCityRumour removeLostCityRumour() {
        return removeTileItem(getLostCityRumour());
    }

    /**
     * Adds a new river to this tile.
     *
     * -til: Changes appearance.
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
     * -til: Changes appearance.
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
     * -til: Changes appearance.
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
     * -til: Changes appearance.
     *
     * @return The removed road.
     */
    public TileImprovement removeRoad() {
        TileImprovement road = getRoad();
        if (road == null) return null;
        road.updateRoadConnections(false);
        return removeTileItem(road);
    }

    /**
     * Gets the resource on this tile.
     *
     * @return A <code>Resource</code>, or null if none present.
     */
    public Resource getResource() {
        return (tileItemContainer == null) ? null
            : tileItemContainer.getResource();
    }

    /**
     * Adds a resource to this tile.
     *
     * -til: Changes appearance.
     *
     * @param resource The <code>Resource</code> to add.
     */
    public void addResource(Resource resource) {
        addTileItem(resource);
    }

    /**
     * Removes a resource from this tile.
     *
     * -til: Changes appearance.
     *
     * @return The removed <code>Resource</code>.
     */
    public Resource removeResource() {
        Resource resource = getResource();
        if (resource == null) return null;
        return removeTileItem(resource);
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
            : (getTileImprovement(workType) != null) ? -1
            // Return the basic work turns + additional work turns
            : getType().getBasicWorkTurns() + workType.getAddWorkTurns();
    }

    /**
     * Check if a given improvement type is valid for this tile.
     *
     * @param type The <code>TileImprovementType</code> to check.
     * @return True if this tile can be improved with the improvement type.
     */
    public boolean isImprovementTypeAllowed(TileImprovementType type) {
        TileImprovement ti;
        return type != null
            && type.isTileTypeAllowed(getType())
            && ((ti = getTileImprovement(type)) == null || !ti.isComplete());
    }
        
    /**
     * Check if a given improvement is valid for this tile.
     *
     * @param tip The <code>TileImprovement</code> to check.
     * @return True if this tile can be improved with the improvement.
     */
    public boolean isImprovementAllowed(TileImprovement tip) {
        final TileImprovementType type = tip.getType();
        if (!isImprovementTypeAllowed(type)) return false;
        TileImprovementType req = type.getRequiredImprovementType();
        if (req != null && getTileImprovement(req) == null) return false;
        TileImprovement ti = getTileImprovement(type);
        return ti == null || !ti.isComplete();
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
        List<RandomChoice<Disaster>> disasters = new ArrayList<>();
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
        StringTemplate label = StringTemplate.key(type);
        if (tileItemContainer != null) {
            List<Named> keys = tileItemContainer.getTileItems().stream()
                .filter(TileItem::isComplete).collect(Collectors.toList());
            if (!keys.isEmpty()) {
                label = StringTemplate.label("/").addNamed(type);
                for (Named key : keys) label.addNamed(key);
            }
        }
        return label;
    }
    /**
     * Get a simple label for this tile, with just its coordinates.
     *
     * @return A simple <code>StringTemplate</code> label.
     */
    public StringTemplate getSimpleLabel() {
        return StringTemplate.template("model.tile.simpleLabel")
            .addAmount("%x%", getX())
            .addAmount("%y%", getY());
    }

    /**
     * Get a detailed label for this tile.
     *
     * @return A suitable <code>StringTemplate</code>.
     */
    public StringTemplate getDetailedLocationLabel() {
        Settlement nearSettlement = null;
        for (Tile tile : getSurroundingTiles(NEAR_RADIUS)) {
            nearSettlement = tile.getSettlement();
            if (nearSettlement != null && nearSettlement.getName() != null) {
                Direction d = Map.getRoughDirection(tile, this);
                StringTemplate t = StringTemplate
                    .template("model.tile.nameLocation");
                if (d == null) {
                    t.addName("%location%", nearSettlement.getName());
                } else {
                    t.addStringTemplate("%location%",
                        getNearLocationLabel(d, nearSettlement.getLocationLabel()));
                }
                if (type == null) {
                    t.add("%name%", "unexplored");
                } else {
                    t.addNamed("%name%", type);
                }
                return t;
            }
        }
        return (region != null && region.getName() != null)
            ? StringTemplate.template("model.tile.nameLocation")
                .addNamed("%name%", type)
                .addNamed("%location%", region)
            : getSimpleLabel();
    }

    /**
     * Get a detailed label for this tile for a given player.
     *
     * @param player The <code>Player</code> to produce a label for.
     * @return A suitable <code>StringTemplate</code>.
     */
    public StringTemplate getDetailedLocationLabelFor(Player player) {
        Settlement nearSettlement = null;
        for (Tile tile : getSurroundingTiles(NEAR_RADIUS)) {
            nearSettlement = tile.getSettlement();
            if (nearSettlement != null
                && nearSettlement.hasContacted(player)) {
                Direction d = Map.getRoughDirection(tile, this);
                StringTemplate t = StringTemplate
                    .template("model.tile.nameLocation")
                        .addStringTemplate("%location%", (d == null)
                            ? nearSettlement.getLocationLabelFor(player)
                            : getNearLocationLabel(d,
                                nearSettlement.getLocationLabelFor(player)));
                if (type == null) {
                    t.add("%name%", "unexplored");
                } else {
                    t.addNamed("%name%", type);
                }
                return t;
            }
        }
        return (region != null && region.getName() != null)
            ? StringTemplate.template("model.tile.nameLocation")
                .addNamed("%name%", type)
                .addStringTemplate("%location%", region.getLabel())
            : getSimpleLabel();
    }

    /**
     * Get a label for this tile assuming it is a colony tile of
     * a given colony.
     *
     * @param colony The <code>Colony</code> assumed to own this tile.
     * @return A suitable <code>StringTemplate</code>, or null if this
     *     tile is not close enough to the colony to be a colony tile.
     */
    public StringTemplate getColonyTileLocationLabel(Colony colony) {
        Tile ct = colony.getTile();
        StringTemplate t = StringTemplate.template("model.tile.nameLocation");
        if (ct == this) {
            t.addStringTemplate("%location%",
                StringTemplate.key("colonyCenter"));
        } else {
            Direction d = getMap().getDirection(ct, this);
            if (d == null) return null;
            t.addNamed("%location%", d);
        }
        if (type == null) {
            t.add("%name%", "unexplored");
        } else {
            t.addNamed("%name%", type);
        }
        return t;
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
        return getMap().getDistance(this, tile);
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
        return getMap().getAdjacentTile(x, y, direction);
    }

    /**
     * Determines whether this tile is adjacent to the specified tile.
     *
     * @param tile A potentially adjacent <code>Tile</code>.
     * @return True if the <code>Tile</code> is adjacent to this
     *     <code>Tile</code>.
     */
    public boolean isAdjacent(Tile tile) {
        return (tile == null) ? false
            : any(getSurroundingTiles(1, 1), t -> t == tile);
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
     * Is this tile land locked?
     *
     * @return True if land locked.
     */
    public boolean isLandLocked() {
        return (!isLand()) ? false
            : all(getSurroundingTiles(1, 1), Tile::isLand);
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
        return any(getSurroundingTiles(1, 1), t -> t.isLand() != this.isLand());
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
        List<Tile> result = new ArrayList<>();
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
        return any(getSurroundingTiles(1, 1), t -> !t.isExplored());
    }

    /**
     * Get the number of tiles adjacent to this one that are of the same
     * land/water type such as to be nominally accessible to a unit.
     *
     * @return The number of adjacent available tiles.
     */
    public int getAvailableAdjacentCount() {
        int n = 0;
        for (Tile t : getSurroundingTiles(1)) if (t.isLand() == isLand()) n++;
        return n;
    }

    /**
     * Get the adjacent colonies.
     *
     * @return A list of adjacent <code>Colony</code>s.
     */
    public List<Colony> getAdjacentColonies() {
        List<Colony> result = new ArrayList<>();
        for (Tile t : getSurroundingTiles(1)) {
            Colony c = t.getColony();
            if (c != null) result.add(c);
        }
        return result;
    }

    /**
     * Finds the nearest settlement to this tile.
     *
     * @param owner If non-null, the settlement should be owned by this player.
     * @param radius The maximum radius of the search.
     * @param same If true, require the settlement to be on the same land mass.
     * @return The nearest settlement, or null if none.
     */
    public Settlement getNearestSettlement(Player owner, int radius,
                                           boolean same) {
        if (radius <= 0) radius = INFINITY;
        Map map = getGame().getMap();
        for (Tile t : map.getCircleTiles(this, true, radius)) {
            if (t == this
                || (same && !isConnectedTo(t))) continue;
            Settlement settlement = t.getSettlement();
            if (settlement != null
                && (owner == null || owner.owns(settlement))) {
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
        if ((getFirstUnit() == null || getFirstUnit().getOwner() == player)
            && (!hasSettlement() || getSettlement().getOwner() == player)) {
            return this;
        }

        for (int r = 1; true; r++) {
            List<Tile> tiles = getSurroundingTiles(r, r);
            if (tiles.isEmpty()) return null;
            if (random != null) {
                randomShuffle(logger, "Safe tile", tiles, random);
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

    /**
     * Get the defence value for this tile type.
     *
     * @return The defence value.
     */
    public double getDefenceValue() {
        final TileType type = getType();
        return (type == null) ? 0.0
            : applyModifiers(1.0f, null, type.getDefenceModifiers());
    }

    /**
     * Get the defence bonus as a percent.
     *
     * @return The percentage defence bonus.
     */
    public int getDefenceBonusPercentage() {
        return (int)getType().applyModifiers(100f, getGame().getTurn(),
                                             Modifier.DEFENCE)
            - 100;
    }

    /**
     * Get a list of surrounding land tiles, sorted with the most
     * defensible first.  Useful when planning an attack.
     *
     * @param player A <code>Player</code> to use to check for
     *     tile access.
     * @return A list of land <code>Tile</code>s.
     */
    public List<Tile> getSafestSurroundingLandTiles(Player player) {
        return getSurroundingTiles(0, 1).stream()
            .filter(t -> t.isLand()
                && (!t.hasSettlement() || player.owns(t.getSettlement())))
            .sorted(defenceValueComparator).collect(Collectors.toList());
    }
                    
    /**
     * Get the adjacent land tile with the best defence bonus.
     * Useful for incoming attackers as a disembark site.
     *
     * @param player A <code>Player</code> to use to check for
     *     tile access.
     * @return The most defensible adjacent land <code>Tile</code>.
     */
    public Tile getBestDisembarkTile(Player player) {
        return find(getSafestSurroundingLandTiles(player),
            Tile::isHighSeasConnected);
    }

    /**
     * Is this tile dangerous for a naval unit to enter?
     * That is, is there an adjacent settlement that is likely to bombard it.
     *
     * @param ship The naval <code>Unit</code> to check.
     * @return True if moving the ship to this tile exposes it to attack.
     */
    public boolean isDangerousToShip(Unit ship) {
        final Player player = ship.getOwner();
        return any(getSurroundingTiles(0, 1).stream()
            .filter(Tile::hasSettlement),
            t -> {
                Settlement settlement = t.getSettlement();
                return !player.owns(settlement)
                    && settlement.canBombardEnemyShip()
                    && (player.atWarWith(settlement.getOwner())
                        || ship.hasAbility(Ability.PIRACY));
            });
    }

    /**
     * Get any safe sites for a naval transport unit to stop at to disembark
     * a unit to this tile.  To be safe, the tile must be adjacent to this
     * one but not adjacent to a dangerous settlement.
     *
     * @param unit The transport <code>Unit</code> that needs a anchoring site.
     * @return A list of suitable <code>Tile</code>s.
     */
    public List<Tile> getSafeAnchoringTiles(Unit unit) {
        return getSurroundingTiles(0, 1).stream()
            .filter(t -> !t.isLand() && t.isHighSeasConnected()
                && !t.isDangerousToShip(unit))
            .collect(Collectors.toList());
    }
                

    //
    // Type and Ownership
    //

    /**
     * Changes the type of this tile.
     * The map generator et al should just use setType(), whereas this
     * routine should be called for the special case of a change of an
     * existing tile type (e.g. pioneer clearing forest).
     *
     * -til: Changes appearance.
     *
     * @param type The new <code>TileType</code>.
     */
    public void changeType(TileType type) {
        setType(type);

        if (tileItemContainer != null) {
            tileItemContainer.removeIncompatibleImprovements();
        }
        if (!isLand()) settlement = null;

        updateColonyTiles();
    }

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
     * -til: Changes appearance.
     *
     * @param settlement The new owning <code>Settlement</code> for
     *     this <code>Tile</code>.
     */
    public void changeOwningSettlement(Settlement settlement) {
        if (owningSettlement != null) {
            owningSettlement.removeTile(this);
        }
        setOwningSettlement(settlement);//-til
        if (settlement != null) {
            settlement.addTile(this);
        }
    }

    /**
     * Change the tile ownership.  Also change the owning settlement
     * as the two are commonly related.
     *
     * -til: Changes appearance.
     *
     * @param player The <code>Player</code> to own the tile.
     * @param settlement The <code>Settlement</code> to own the
     *     <code>Tile</code>.
     */
    public void changeOwnership(Player player, Settlement settlement) {
        setOwner(player);//-til
        changeOwningSettlement(settlement);//-til
    }

    /**
     * A colony is proposed to be built on this tile.  Collect
     * warnings if this has disadvantages.
     *
     * @param unit The <code>Unit</code> which is to build the colony.
     * @return A <code>StringTemplate</code> containing the warnings,
     *      or null if none.
     */
    public StringTemplate getBuildColonyWarnings(Unit unit) {
        final Specification spec = getSpecification();
        final Player owner = unit.getOwner();
        boolean landLocked = true;
        boolean ownedByEuropeans = false;
        boolean ownedBySelf = false;
        boolean ownedByIndians = false;

        java.util.Map<GoodsType, Integer> goodsMap = new HashMap<>();
        for (GoodsType goodsType : spec.getGoodsTypeList()) {
            if (goodsType.isBuildingMaterial()) {
                while (goodsType.isRefined()) {
                    goodsType = goodsType.getInputType();
                }
            } else if (!goodsType.isFoodType()) {
                continue;
            }
            for (ProductionType productionType : getType()
                     .getAvailableProductionTypes(false)) {
                int potential = (productionType.getOutput(goodsType) == null)
                    ? 0 : getPotentialProduction(goodsType, null);
                Integer oldPotential = goodsMap.get(goodsType);
                if (oldPotential == null || potential > oldPotential) {
                    goodsMap.put(goodsType, potential);
                }
            }
        }

        for (Tile t : getSurroundingTiles(1)) {
            if (!t.isLand()) landLocked = false;
            for (Entry<GoodsType, Integer> entry : goodsMap.entrySet()) {
                entry.setValue(entry.getValue()
                    + t.getPotentialProduction(entry.getKey(),
                        spec.getDefaultUnitType(owner)));
            }
            Player tileOwner = t.getOwner();
            if (owner == tileOwner) {
                if (t.getOwningSettlement() != null) {
                    // we are using newTile
                    ownedBySelf = true;
                } else {
                    for (Tile ownTile : t.getSurroundingTiles(1)) {
                        Colony colony = ownTile.getColony();
                        if (colony != null && colony.getOwner() == owner) {
                            // newTile can be used from an own colony
                            ownedBySelf = true;
                            break;
                        }
                    }
                }
            } else if (tileOwner != null && tileOwner.isEuropean()) {
                ownedByEuropeans = true;
            } else if (tileOwner != null) {
                ownedByIndians = true;
            }
        }

        StringTemplate ret = StringTemplate.label("\n");
        if (landLocked) {
            ret.add("warning.landLocked");
        }
        int food = goodsMap.entrySet().stream()
            .filter(e -> e.getKey().isFoodType())
            .mapToInt(e -> e.getValue()).sum();
        if (food < 8) {
            ret.add("warning.noFood");
        }
        for (Entry<GoodsType, Integer> entry : goodsMap.entrySet()) {
            if (!entry.getKey().isFoodType()
                && entry.getValue() < LOW_PRODUCTION_WARNING_VALUE) {
                ret.addStringTemplate(StringTemplate
                    .template("warning.noBuildingMaterials")
                    .addNamed("%goods%", entry.getKey()));
            }
        }

        if (ownedBySelf) {
            ret.add("warning.ownLand");
        }
        if (ownedByEuropeans) {
            ret.add("warning.europeanLand");
        }
        if (ownedByIndians) {
            ret.add("warning.nativeLand");
        }

        return ret;
    }

    //
    // Production
    //

    /**
     * Can this tile produce a given goods type?  To produce goods
     * either the tile type must have a suitable production type, or
     * the tile item container contains suitable resource.
     *
     * @param goodsType The <code>GoodsType</code> to produce.
     * @param unitType An optional <code>UnitType</code> to use.
     * @return True if the tile can produce the goods.
     */
    public boolean canProduce(GoodsType goodsType, UnitType unitType) {
        return (type != null && type.canProduce(goodsType, unitType))
            || (tileItemContainer != null
                && tileItemContainer.canProduce(goodsType, unitType));
    }

    /**
     * Get the base production exclusive of any bonuses.
     *
     * @param productionType An optional <code>ProductionType</code> to use,
     *     if null the best available one is used.
     * @param goodsType The <code>GoodsType</code> to produce.
     * @param unitType An optional <code>UnitType</code> to use.
     * @return The base production due to tile type and resources.
     */
    public int getBaseProduction(ProductionType productionType,
                                 GoodsType goodsType, UnitType unitType) {
        if (type == null || goodsType == null
            || !goodsType.isFarmed()) return 0;
        int amount = type.getBaseProduction(productionType, goodsType,
                                            unitType);
        return (amount < 0) ? 0 : amount;
    }

    /**
     * Get the potential production of this tile for a given goods type
     * and optional worker type.
     *
     * @param goodsType The <code>GoodsType</code> to check the
     *     potential for.
     * @param unitType An optional <code>UnitType</code> to do the work.
     * @return The potential production of this <code>Tile</code> to
     *     produce the given <code>GoodsType</code>.
     */
    public int getPotentialProduction(GoodsType goodsType,
                                      UnitType unitType) {
        if (!canProduce(goodsType, unitType)) return 0;

        int amount = getBaseProduction(null, goodsType, unitType);
        amount = (int)applyModifiers(amount, getGame().getTurn(),
            getProductionModifiers(goodsType, unitType));
        return (amount < 0) ? 0 : amount;
    }

    /**
     * Get the production modifiers for this tile.
     *
     * @param goodsType The <code>GoodsType</code> to produce.
     * @param unitType An optional <code>UnitType</code> to do the work.
     * @return A list of production <code>Modifier</code>s.
     */
    public List<Modifier> getProductionModifiers(GoodsType goodsType,
                                                 UnitType unitType) {
        if (!canProduce(goodsType, unitType)) {
            return Collections.<Modifier>emptyList();
        }

        List<Modifier> result = new ArrayList<>();
        if (tileItemContainer != null) {
            result.addAll(tileItemContainer
                .getProductionModifiers(goodsType, unitType));
        }
        return result;
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
        // there are options to change TileType using an improvement,
        // consider that too.
        final Specification spec = getSpecification();
        List<TileType> tileTypes = new ArrayList<>();
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
            float potential = tileType.getPotentialProduction(goodsType, unitType);
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
     * Sort possible goods types according to potential.
     *
     * @param unitType The <code>UnitType</code> to do the work.
     * @param owner the <code>Player</code> owning the unit.
     * @return A list of goods, highest potential production first.
     */
    public List<AbstractGoods> getSortedPotential(UnitType unitType,
                                                  Player owner) {
        final Specification spec = getSpecification();
        List<AbstractGoods> goodsTypeList = new ArrayList<>();
        if (getType() != null) {
            // It is necessary to consider all farmed goods, since the
            // tile might have a resource that produces goods not
            // produced by the tile type.
            for (GoodsType goodsType : spec.getFarmedGoodsTypeList()) {
                int potential = getPotentialProduction(goodsType, unitType);
                if (potential > 0) {
                    goodsTypeList.add(new AbstractGoods(goodsType, potential));
                }
            }
            Collections.sort(goodsTypeList,
                (owner == null || owner.getMarket() == null)
                ? AbstractGoods.abstractGoodsComparator
                : owner.getMarket().getSalePriceComparator());
        }
        return goodsTypeList;
    }


    //
    // Colony and cached Tile maintenance
    //

    /**
     * Update player explored tiles after a change to this tile.
     */
    private void updateColonyTiles() {
        for (Player player : getGame().getLiveEuropeanPlayers(null)) {
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
     * Get a players view of this tile.
     *
     * @param player The <code>Player</code> who owns the view.
     * @return The view of this <code>Tile</code>.
     */
    public Tile getCachedTile(Player player) {
        return (cachedTiles == null) ? null
            : (player.isEuropean()) ? cachedTiles.get(player)
            : this;
    }

    /**
     * Set a players view of this tile.
     *
     * @param player The <code>Player</code> who owns the view.
     * @param tile The view of the <code>Tile</code> (either this
     *     tile, or an uninterned copy of it).
     */
    public void setCachedTile(Player player, Tile tile) {
        if (cachedTiles == null || !player.isEuropean()) return;
        cachedTiles.put(player, tile);
    }

    /**
     * Set the players view of this tile to the tile itself if
     * the player can see it.  Useful when the cache needs to be cleared
     * forcibly such as when a native settlement is removed.
     */
    public void seeTile() {
        for (Player p : getGame().getLiveEuropeanPlayers(null)) {
            if (p.canSee(this)) seeTile(p);
        }
    }

    /**
     * Set a players view of this tile to the tile itself.
     *
     * @param player The <code>Player</code> who owns the view.
     */
    public void seeTile(Player player) {
        setCachedTile(player, this);
    }

    /**
     * A change is about to occur on this tile.  Cache it if unseen.
     */
    public void cacheUnseen() {
        cacheUnseen(null, null);
    }

    /**
     * Get a copy of this tile suitable for caching (lacking units).
     *
     * @return An uninterned copy of this <code>Tile</code>.
     */
    public Tile getTileToCache() {
        Tile tile = this.copy(getGame(), Tile.class);
        tile.clearUnitList();
        // Set the unit count for a copied colony
        Colony colony = getColony();
        if (colony != null) {
            tile.getColony().setDisplayUnitCount(colony.getUnitCount());
        }
        return tile;
    }


    /**
     * A change is about to occur on this tile.  Cache it if unseen.
     *
     * @param player A <code>Player</code> that currently may not be able
     *     to see the tile, but will as a result of the change, and so
     *     should not cache it.
     */
    public void cacheUnseen(Player player) {
        cacheUnseen(player, null);
    }

    /**
     * A change may have occured on this tile.  Establish caches where
     * needed.  Use the copied tile if supplied (which should have
     * been created previously with {@link #getTileToCache},
     *
     * @param copied An optional <code>Tile</code> to cache.
     */
    public void cacheUnseen(Tile copied) {
        cacheUnseen(null, copied);
    }

    /**
     * A change may have occured on this tile.  Establish caches where
     * needed.  Use the copied tile if supplied (which should have
     * been created previously with {@link #getTileToCache},
     *
     * @param player A <code>Player</code> that currently may not be able
     *     to see the tile, but will as a result of the change, and so
     *     should not cache it.
     * @param copied An optional <code>Tile</code> to cache.
     */
    public void cacheUnseen(Player player, Tile copied) {
        if (cachedTiles == null) return;
        for (Player p : getGame().getLiveEuropeanPlayers(player)) {
            if (!p.canSee(this) && getCachedTile(p) == this) {
                if (copied == null) copied = getTileToCache();
                setCachedTile(p, copied);
            }
        }
    }

    /**
     * Updates the information about the native settlement on this
     * <code>Tile</code> for the given <code>Player</code>.
     *
     * @param player The <code>Player</code>.
     */
    public void updateIndianSettlement(Player player) {
        if (playerIndianSettlements == null || !player.isEuropean()) return;
        IndianSettlementInternals isi = getPlayerIndianSettlement(player);
        IndianSettlement is = getIndianSettlement();
        if (is == null) {
            if (isi != null) removeIndianSettlementInternals(player);
        } else {
            if (isi == null) {
                isi = new IndianSettlementInternals();
                playerIndianSettlements.put(player, isi);
            }
            isi.update(is);
        }
    }

    public void removeIndianSettlementInternals(Player player) {
        if (playerIndianSettlements == null) return;
        playerIndianSettlements.remove(player);
    }

    public UnitType getLearnableSkill(Player player) {
        IndianSettlementInternals isi = getPlayerIndianSettlement(player);
        return (isi == null) ? null : isi.skill;
    }

    public GoodsType[] getWantedGoods(Player player) {
        IndianSettlementInternals isi = getPlayerIndianSettlement(player);
        return (isi == null) ? null : isi.wantedGoods;
    }

    /**
     * Set native settlement information.  Do not check the current
     * map state as we might leak destruction information.
     *
     * @param player The <code>Player</code> to pet belonged to.
     * @param skill The skill taught by the settlement.
     * @param wanted The goods wanted by the settlement.
     */
    public void setIndianSettlementInternals(Player player, UnitType skill,
                                             GoodsType[] wanted) {
        IndianSettlementInternals isi = getPlayerIndianSettlement(player);
        if (isi == null) {
            isi = new IndianSettlementInternals();
            playerIndianSettlements.put(player, isi);
        }
        isi.setValues(skill, wanted);
    }

    /**
     * Checks if this <code>Tile</code> has been explored by the given
     * <code>Player</code>.
     *
     * If we are in the server, then the presence of a cached tile
     * determines whether exploration has happened.  In the client
     * there are no cached tiles, but if the tile is explored the
     * server will have updated the client with the tile type (checked
     * by isExplored()).
     *
     * @param player The <code>Player</code>.
     * @return True if this <code>Tile</code> has been explored
     *     by the given <code>Player</code>.
     */
    public boolean isExploredBy(Player player) {
        return (!player.isEuropean()) ? true
            : (!isExplored()) ? false
            : (cachedTiles == null) ? true
            : getCachedTile(player) != null;
    }

    /**
     * Explore/unexplore a tile for a player.
     *
     * @param player The <code>Player</code> that is exploring.
     * @param reveal The exploration state.
     */
    public void setExplored(Player player, boolean reveal) {
        if (cachedTiles == null || !player.isEuropean()) return;
        if (reveal) {
            seeTile(player);
        } else {
            cachedTiles.remove(player);
        }
    }


    //
    // Unit manipulation
    //

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
        double defenderPower = -1.0, power;

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
            && hasSettlement()) {
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
                    //defenderPower = power;
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
        return (owner != null && unit != null && unit.getOwner() != owner
            && unit.getOwner().atWarWith(owner))
            ? find(getUnitList(), Unit::isOffensiveUnit)
            : null;
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
    @Override
    public Tile getTile() {
        return this;
    }

    /**
     * Get a label for a nearby location.
     *
     * @param direction The <code>Direction</code> from this tile to the
     *     nearby location.
     * @param location A <code>StringTemplate</code> describing the location.
     * @return A <code>StringTemplate</code> stating that the location
     *     is nearby.
     */
    public StringTemplate getNearLocationLabel(Direction direction,
                                               StringTemplate location) {
        return StringTemplate.template("model.tile.nearLocation")
            .addNamed("%direction%", direction)
            .addStringTemplate("%location%", location);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public StringTemplate getLocationLabel() {
        return (settlement != null) ? settlement.getLocationLabel()
            : getDetailedLocationLabel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StringTemplate getLocationLabelFor(Player player) {
        return (settlement != null) ? settlement.getLocationLabelFor(player)
            : getDetailedLocationLabelFor(player);
    }

    /**
     * {@inheritDoc}
     *
     * -til: Changes appearance with TileItems.
     */
    @Override
    public boolean add(Locatable locatable) {
        if (locatable instanceof TileItem) {
            return addTileItem((TileItem) locatable);//-til

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
     *
     * -til: Changes appearance with TileItems.
     */
    @Override
    public boolean remove(Locatable locatable) {
        if (locatable instanceof TileItem) {
            return removeTileItem((TileItem)locatable)
                == locatable;//-til

        } else {
            return super.remove(locatable);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
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

    /**
     * {@inheritDoc}
     */
    @Override
    public Location up() {
        return (hasSettlement()) ? getSettlement()
            : this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRank() {
        return getX() + getY() * getMap().getWidth();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toShortString() {
        StringBuilder sb = new StringBuilder(16);
        TileType type = getType();
        sb.append(getX()).append(",").append(getY())
            .append("-").append((type == null) ? "?" : type.getSuffix());
        return sb.toString();
    }


    // Interface Named

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNameKey() {
        if (getGame().isInClient()) {
            return (isExplored()) ? getType().getNameKey() : "unexplored";
        } else {
            Player player = getGame().getCurrentPlayer();
            if (player != null) {
                return (getCachedTile(player) == null) ? "unexplored"
                    : getType().getNameKey();
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
    @Override
    public Player getOwner() {
        return owner;
    }

    /**
     * {@inheritDoc}
     *
     * -til: Changes appearance.
     */
    @Override
    public void setOwner(Player owner) {
        this.owner = owner;
    }


    // Override FreeColGameObject

    /**
     * {@inheritDoc}
     */
    @Override
    public void disposeResources() {
        if (settlement != null) {
            settlement.disposeResources();
            settlement = null;
        }
        if (tileItemContainer != null) {
            tileItemContainer.disposeResources();
            tileItemContainer = null;
        }
        super.disposeResources();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColGameObject getLinkTarget(Player player) {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int checkIntegrity(boolean fix) {
        int result = super.checkIntegrity(fix);
        Settlement settlement = getSettlement();
        if (settlement != null) {
            result = Math.min(result, settlement.checkIntegrity(fix));
        }
        if (tileItemContainer != null) {
            result = Math.min(result, tileItemContainer.checkIntegrity(fix));
        }
        return result;
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Ability> getAbilities(String id,
                                     FreeColGameObjectType fcgot,
                                     Turn turn) {
        // Delegate to type
        return getType().getAbilities(id, fcgot, turn);
    }


    // Serialization

    private static final String CACHED_TILE_TAG = "cachedTile";
    private static final String CONNECTED_TAG = "connected";
    private static final String CONTIGUITY_TAG = "contiguity";
    private static final String COPIED_TAG = "copied";
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
    // @compat 0.11.3
    public static final String OLD_TILE_ITEM_CONTAINER_TAG = "tileitemcontainer";
    // end @compat 0.11.3
    

    /**
     * {@inheritDoc}
     */
    @Override
    public void toXML(FreeColXMLWriter xw, String tag) throws XMLStreamException {
        // Special override of tile output serialization that handles
        // the tile caching.
        // 1. If not writing to a player, just write this tile.
        // 2. If there is no cached tile then it is unexplored, so
        //    write the minimal tile (id, x, y).
        // 3. Otherwise write the cached tile, which will either be a
        //    copy or <code>this</code>.
        Player player = xw.getClientPlayer();
        Tile tile = (player == null) ? this : getCachedTile(player);

        if (tile == null) {
            xw.writeStartElement(tag);

            xw.writeAttribute(ID_ATTRIBUTE_TAG, getId());

            xw.writeAttribute(X_TAG, this.x);

            xw.writeAttribute(Y_TAG, this.y);

            xw.writeEndElement();
        } else {
            tile.internalToXML(xw, tag);
        }
    }

    /**
     * Fundamental (post-cache) version of toXML.
     *
     * @param xw The <code>FreeColXMLWriter</code> to write to.
     * @param tag The tag to use.
     * @exception XMLStreamException if there are any problems writing
     *     to the stream.
     */
    public void internalToXML(FreeColXMLWriter xw, String tag) throws XMLStreamException {
        xw.writeStartElement(tag);

        writeAttributes(xw);

        writeChildren(xw);

        xw.writeEndElement();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(X_TAG, this.x);

        xw.writeAttribute(Y_TAG, this.y);

        xw.writeAttribute(TYPE_TAG, type);

        if (owner != null) {
            xw.writeAttribute(OWNER_TAG, owner);
        }
        
        if (owningSettlement != null) {
            if (owningSettlement.isDisposed()
                || owningSettlement.getId() == null) {
                // Owning settlement is a special case because it is a
                // reference to something outside this tile.  If the
                // tile being written here is a cached copy, and the
                // owning settlement referred to therein has really
                // been destroyed, then we risk corrupting or at least
                // confusing the client by referring to the disposed
                // settlement.  So clear out such cases.  This is an
                // information leak, but a better option than the
                // crashes caused by the alternative.
                this.owningSettlement = null;
            } else {
                xw.writeAttribute(OWNING_SETTLEMENT_TAG, owningSettlement);
            }
        }
        
        xw.writeAttribute(STYLE_TAG, style);
        
        if (region != null) {
            xw.writeAttribute(REGION_TAG, region);
        }
        
        if (moveToEurope != null) {
            xw.writeAttribute(MOVE_TO_EUROPE_TAG,
                              moveToEurope.booleanValue());
        }

        if (highSeasCount >= 0) {
            xw.writeAttribute(CONNECTED_TAG, highSeasCount);
        }
       
        if (contiguity >= 0) {
            xw.writeAttribute(CONTIGUITY_TAG, contiguity);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        // Show tile contents (e.g. enemy units) if not scoped to a
        // player that can not see the tile, and there is no blocking
        // enemy settlement.
        Player player = xw.getClientPlayer();
        if ((player == null || player.canSee(this)) 
            && (settlement == null
                || xw.validFor(settlement.getOwner()))) {
            super.writeChildren(xw);
        }

        if (settlement != null) settlement.toXML(xw);

        if (tileItemContainer != null) tileItemContainer.toXML(xw);

        // Save the cached tiles to saved games.
        if (xw.validForSave() && cachedTiles != null) {
            for (Player p : getGame().getLiveEuropeanPlayers(null)) {
                Tile t = getCachedTile(p);
                if (t == null) continue;

                if (t == this && getIndianSettlement() != null) {
                    // Always save client view of native settlements
                    // because of the hidden information.
                    t = getTileToCache();
                    t.setIndianSettlementInternals(p, getLearnableSkill(p),
                                                   getWantedGoods(p));
                }

                xw.writeStartElement(CACHED_TILE_TAG);

                xw.writeAttribute(PLAYER_TAG, p);

                xw.writeAttribute(COPIED_TAG, t != this);

                if (t != this) {
                    // Only write copied tiles, with limited scope.
                    FreeColXMLWriter.WriteScope scope = xw.getWriteScope();
                    xw.setWriteScope(FreeColXMLWriter.WriteScope.toClient(p));
                    // Do not call toXML!  It will look for a cached tile,
                    // inside t which is already a cached copy!
                    try {
                        t.internalToXML(xw, getXMLElementTagName());
                    } finally {
                        xw.setWriteScope(scope);
                    }
                }

                xw.writeEndElement();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        final Specification spec = getSpecification();
        final Game game = getGame();

        x = xr.getAttribute(X_TAG, 0);

        y = xr.getAttribute(Y_TAG, 0);

        type = xr.getType(spec, TYPE_TAG, TileType.class, (TileType)null);
        if (type == null) { // Unexplored tile.
            style = 0;
            highSeasCount = -1;
            owner = null;
            region = null;
            moveToEurope = null;
            contiguity = -1;
            return;
        }

        style = xr.getAttribute(STYLE_TAG, 0);

        String str = xr.getAttribute(CONNECTED_TAG, (String)null);
        if (str == null || str.isEmpty()) {
            highSeasCount = -1;
            // @compat 0.10.5
            // High seas should have connected==0.  If it does not, this
            // is probably an old save file, so flag a recalculation.
            String typeStr = xr.getAttribute(TYPE_TAG, (String)null);
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

        owner = xr.findFreeColGameObject(game, OWNER_TAG,
                                         Player.class, (Player)null, false);

        region = xr.findFreeColGameObject(game, REGION_TAG,
                                          Region.class, (Region)null, false);
        
        moveToEurope = (xr.hasAttribute(MOVE_TO_EUROPE_TAG))
            ? xr.getAttribute(MOVE_TO_EUROPE_TAG, false)
            : null;

        contiguity = xr.getAttribute(CONTIGUITY_TAG, -1);

        // Tiles are added to the settlement owned tiles list in Map.
        // Doing it here can cause cache weirdness.
        Location loc = xr.getLocationAttribute(game, OWNING_SETTLEMENT_TAG,
                                               true);
        owningSettlement = (loc instanceof Settlement) ? (Settlement)loc
            : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        settlement = null;

        super.readChildren(xr);

        // @compat 0.10.1
        // Old settlements were prone to not owning their tiles.
        if (getSettlement() != null) {
            Settlement settlement = getSettlement();
            Player owner = settlement.getOwner();
            if (owner != null && getOwner() != owner) {
                this.owner = owner;
            }
            if (this.owningSettlement != settlement) {
                this.owningSettlement = settlement;
            }
        }
        // end @compat 0.10.1
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final Specification spec = getSpecification();
        final Game game = getGame();
        final String tag = xr.getLocalName();

        if (cachedTiles != null && CACHED_TILE_TAG.equals(tag)) {
            Player player = xr.findFreeColGameObject(game, PLAYER_TAG, 
                Player.class, (Player)null, true);

            boolean copied = xr.getAttribute(COPIED_TAG, false);
            if (copied) {
                FreeColXMLReader.ReadScope scope = xr.getReadScope();
                xr.setReadScope(FreeColXMLReader.ReadScope.NOINTERN);
                xr.nextTag();
                xr.expectTag(Tile.getXMLElementTagName());
                Tile tile = xr.readFreeColGameObject(game, Tile.class);

                // Temporary workaround for BR#2618 on input
                Colony colony = tile.getColony();
                if (colony != null && colony.getDisplayUnitCount() <= 0) {
                    logger.warning("Copied colony " + colony.getId()
                        + " display unit count set to 1 from corrupt: "
                        + colony.getDisplayUnitCount());
                    colony.setDisplayUnitCount(1);
                }
                // end workaround

                setCachedTile(player, tile);
                xr.setReadScope(scope);

                IndianSettlement is = tile.getIndianSettlement();
                if (is == null) {
                    removeIndianSettlementInternals(player);
                } else {
                    setIndianSettlementInternals(player,
                        is.getLearnableSkill(), is.getWantedGoods());
                }

            } else {
                setCachedTile(player, this);
            }
            xr.closeTag(CACHED_TILE_TAG);

        // @compat 0.10.1
        } else if (OLD_UNITS_TAG.equals(tag)) {
            while (xr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                super.readChild(xr);
            }
        // end @compat

        } else if (Colony.getXMLElementTagName().equals(tag)) {
            settlement = xr.readFreeColGameObject(game, Colony.class);

        } else if (IndianSettlement.getXMLElementTagName().equals(tag)) {
            settlement = xr.readFreeColGameObject(game, IndianSettlement.class);

        // @compat 0.10.7
        } else if (PlayerExploredTile.getXMLElementTagName().equals(tag)) {
            // Only from a saved game.
            Player player = xr.findFreeColGameObject(game, PLAYER_TAG,
                Player.class, (Player)null, true);

            if (xr.hasAttribute(IndianSettlement.LEARNABLE_SKILL_TAG)
                || xr.hasAttribute(IndianSettlement.WANTED_GOODS_TAG + "0")) {
                UnitType skill = xr.getType(spec,
                    IndianSettlement.LEARNABLE_SKILL_TAG,
                    UnitType.class, (UnitType)null);
                GoodsType[] wanted
                    = new GoodsType[IndianSettlement.WANTED_GOODS_COUNT];
                for (int i = 0; i < wanted.length; i++) {
                    wanted[i] = xr.getType(spec,
                        IndianSettlement.WANTED_GOODS_TAG + i,
                        GoodsType.class, (GoodsType)null);
                }
                setIndianSettlementInternals(player, skill, wanted);
            }

            PlayerExploredTile pet = xr.readFreeColGameObject(game,
                PlayerExploredTile.class);
            pet.fixCache();
        // end @compat 0.10.7

        } else if (TileItemContainer.getXMLElementTagName().equals(tag)
                   // @compat 0.11.3
                   || OLD_TILE_ITEM_CONTAINER_TAG.equals(tag)
                   // end @compat 0.11.3
                   ) {
            tileItemContainer = xr.readFreeColGameObject(game,
                TileItemContainer.class);

        } else {
            super.readChild(xr);
        }

        // @compat 0.10.x
        // Fix bug where missionary locations get cleared.
        // FIXME: Remove this when PETs have been revised to not store
        // the actual unit.
        if (settlement instanceof IndianSettlement) {
            Unit missionary = ((IndianSettlement)settlement).getMissionary();
            if (missionary != null) missionary.setLocationNoUpdate(settlement);
        }
        // end @compat 0.10.x
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append("[").append(getId())
            .append(" ").append((type == null) ? "unknown" : type.getSuffix())
            .append(" ").append(x).append(",").append(y)
            .append((!hasSettlement()) ? "" : " " + getSettlement().getName())
            .append("]");
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "tile".
     */
    public static String getXMLElementTagName() {
        return "tile";
    }
}
