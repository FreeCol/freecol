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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.pathfinding.CostDecider;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.common.model.pathfinding.GoalDeciders;
import net.sf.freecol.common.util.LogBuilder;
import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.RandomUtils.*;
// @compat 0.10.x
import net.sf.freecol.server.generator.TerrainGenerator;
// end @compat 0.10.x


/**
 * A rectangular isometric map.  The map is represented as a
 * two-dimensional array of tiles.  Off-map destinations, such as
 * {@link Europe}, can be reached via the {@link HighSeas}.
 *
 * In theory, a {@link Game} might contain several Map instances
 * connected by the HighSeas.
 */
public class Map extends FreeColGameObject implements Location {

    private static final Logger logger = Logger.getLogger(Map.class.getName());

    /**
     * Possible actions by the unit travelling along a path in consideration
     * of the next tile.
     */
    private static enum MoveStep { FAIL, BYLAND, BYWATER, EMBARK, DISEMBARK };

    /**
     * The number of tiles from the upper edge that are considered
     * polar by default.
     */
    public final static int POLAR_HEIGHT = 2;

    /**
     * The layers included in the map. The RIVERS layer includes all
     * natural tile improvements that are not resources. The NATIVES
     * layer includes Lost City Rumours as well as settlements.
     */
    public static enum Layer {
        NONE, LAND, TERRAIN, REGIONS, RIVERS, RESOURCES, NATIVES, ALL;
    };

    /** A position on the Map. */
    public static final class Position {
        
        /** The coordinates of the position. */
        public final int x, y;


        /**
         * Creates a new <code>Position</code> object with the given
         * coordinates.
         *
         * @param posX The x-coordinate for this position.
         * @param posY The y-coordinate for this position.
         */
        public Position(int posX, int posY) {
            x = posX;
            y = posY;
        }

        /**
         * Creates a new <code>Position</code> object with the coordinates
         * of a supplied tile.
         *
         * @param tile The <code>Tile</code> to extract coordinates from.
         */
        public Position(Tile tile) {
            this(tile.getX(), tile.getY());
        }

        /**
         * Creates a new <code>Position</code> from an existing one with
         * an optional step in a given direction.
         *
         * @param start The starting <code>Position</code>.
         * @param direction An optional <code>Direction</code> to step.
         */
        public Position(Position start, Direction direction) {
            Position step = (direction == null) ? start
                : direction.step(start.x, start.y);
            this.x = step.x;
            this.y = step.y;
        }


        /**
         * Gets the x-coordinate of this Position.
         *
         * @return The x-coordinate of this Position.
         */
        public int getX() {
            return x;
        }

        /**
         * Gets the y-coordinate of this Position.
         *
         * @return The y-coordinate of this Position.
         */
        public int getY() {
            return y;
        }

        /**
         * Checks whether a position is valid within a given map size.
         *
         * @param width The width of the map.
         * @param height The height of the map.
         * @return True if the given position is within the bounds of the map.
         */
        public boolean isValid(int width, int height) {
            return Map.isValid(x, y, width, height);
        }

        /**
         * Gets the distance in tiles between two map positions.
         * With an isometric map this is a non-trivial task.
         * The formula below has been developed largely through trial and
         * error.  It should cover all cases, but I wouldn't bet my
         * life on it.
         *
         * @param ax The x-coordinate of the first position.
         * @param ay The y-coordinate of the first position.
         * @param bx The x-coordinate of the second position.
         * @param by The y-coordinate of the second position.
         * @return The distance in tiles between the positions.
         */
        public static int getDistance(int ax, int ay, int bx, int by) {
            int r = (bx - ax) - (ay - by) / 2;

            if (by > ay && ay % 2 == 0 && by % 2 != 0) {
                r++;
            } else if (by < ay && ay % 2 != 0 && by % 2 == 0) {
                r--;
            }
            return Math.max(Math.abs(ay - by + r), Math.abs(r));
        }

        /**
         * Gets the distance in tiles between two map positions.
         * With an isometric map this is a non-trivial task.
         * The formula below has been developed largely through trial and
         * error.  It should cover all cases, but I wouldn't bet my
         * life on it.
         *
         * @param position The other <code>Position</code> to compare.
         * @return The distance in tiles to the other position.
         */
        public int getDistance(Position position) {
            return getDistance(getX(), getY(),
                               position.getX(), position.getY());
        }

        /**
         * Get the direction from this position to an adjacent position.
         *
         * @param other The adjacent <code>Position</code>.
         * @return The <code>Direction</code>, or null if not adjacent.
         */
        public Direction getDirection(Position other) {
            return find(Direction.values(),
                d -> new Position(this, d).equals(other), null);
        }

        // Override Object

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o instanceof Position) {
                Position p = (Position)o;
                return x == p.x && y == p.y;
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return x | (y << 16);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }


    /** The tiles that this map contains. */
    private Tile[][] tiles;

    /** The highest map layer included. */
    private Layer layer;

    /**
     * The latitude of the northern edge of the map. A negative value
     * indicates northern latitude, a positive value southern
     * latitude. Thus, -30 equals 30°N, and 40 equals 40°S.
     */
    private int minimumLatitude = -90;

    /**
     * The latitude of the southern edge of the map. A negative value
     * indicates northern latitude, a positive value southern
     * latitude. Thus, -30 equals 30°N, and 40 equals 40°S.
     */
    private int maximumLatitude = 90;

    /** Variable used to convert rows to latitude. */
    private float latitudePerRow;

    /** The regions on the map. */
    private final List<Region> regions = new ArrayList<>();

    /** The search tracing status.  Do not serialize. */
    private boolean traceSearch = false;


    /**
     * Create a new <code>Map</code> from a collection of tiles.
     *
     * @param game The enclosing <code>Game</code>.
     * @param width The map width.
     * @param height The map height.
     */
    public Map(Game game, int width, int height) {
        super(game);

        this.tiles = new Tile[width][height];
        setLayer(Layer.RESOURCES);
        calculateLatitudePerRow();
    }

    /**
     * Create a new <code>Map</code> from an <code>Element</code> in a
     * DOM-parsed XML-tree.
     *
     * @param game The enclosing <code>Game</code>.
     * @param xr The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     */
    public Map(Game game, FreeColXMLReader xr) throws XMLStreamException {
        super(game, null);

        readFromXML(xr);
    }

    /**
     * Creates a new <code>Map</code> with the given object
     * identifier.  The object should later be initialized by calling
     * either {@link #readFromXML(FreeColXMLReader)} or
     * {@link #readFromXMLElement(Element)}.
     *
     * @param game The enclosing <code>Game</code>.
     * @param id The object identifier.
     */
    public Map(Game game, String id) {
        super(game, id);
    }


    /**
     * Checks if an (x,y) coordinate tuple is within a map of
     * specified width and height.
     *
     * @param x The x-coordinate of the position.
     * @param y The y-coordinate of the position.
     * @param width The width of the map.
     * @param height The height of the map.
     * @return True if the given position is within the bounds of the map.
     */
    public static boolean isValid(int x, int y, int width, int height) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    /**
     * Checks whether a position is valid (within the map limits).
     *
     * @param x The X coordinate to check.
     * @param y The Y coordinate to check.
     * @return True if the coordinates are valid.
     */
    public boolean isValid(int x, int y) {
        return isValid(x, y, getWidth(), getHeight());
    }

    /**
     * Checks whether a position is valid (within the map limits).
     *
     * @param position The <code>Position</code> to check.
     * @return True if the position is valid.
     */
    public boolean isValid(Position position) {
        return isValid(position.getX(), position.getY());
    }

    /**
     * Gets the Tile at position (x, y).  'x' specifies a column and
     * 'y' specifies a row.  (0, 0) is the Tile at the top-left corner
     * of the Map.
     *
     * @param x The x-coordinate of the <code>Tile</code>.
     * @param y The y-coordinate of the <code>Tile</code>.
     * @return The <code>Tile</code> at (x, y), or null if the
     *     position is invalid.
     */
    public Tile getTile(int x, int y) {
        return (isValid(x, y)) ? tiles[x][y] : null;
    }

    /**
     * Gets the Tile at a requested position.
     *
     * @param p The <code>Position</code> to query.
     * @return The <code>Tile</code> at the given position.
     */
    public Tile getTile(Position p) {
        return getTile(p.getX(), p.getY());
    }

    /**
     * Sets the tile at the given coordinates.
     *
     * @param x The x-coordinate of the <code>Tile</code>.
     * @param y The y-coordinate of the <code>Tile</code>.
     * @param tile The <code>Tile</code> to set.
     */
    public void setTile(Tile tile, int x, int y) {
        tiles[x][y] = tile;
    }

    /**
     * Gets the width of this map.
     *
     * @return The width of this map.
     */
    public int getWidth() {
        return tiles.length;
    }

    /**
     * Gets the height of this map.
     *
     * @return The height of this map.
     */
    public int getHeight() {
        return tiles[0].length;
    }

    public final Layer getLayer() {
        return layer;
    }

    public final void setLayer(final Layer newLayer) {
        this.layer = newLayer;
    }

    /**
     * Gets the <code>MinimumLatitude</code> value.
     *
     * @return The minimum latitude of this map.
     */
    public final int getMinimumLatitude() {
        return minimumLatitude;
    }

    public final void setMinimumLatitude(final int newMinimumLatitude) {
        this.minimumLatitude = newMinimumLatitude;
        calculateLatitudePerRow();
    }

    /**
     * Gets the <code>MaximumLatitude</code> value.
     *
     * @return The maximum latitude of this map.
     */
    public final int getMaximumLatitude() {
        return maximumLatitude;
    }

    public final void setMaximumLatitude(final int newMaximumLatitude) {
        this.maximumLatitude = newMaximumLatitude;
        calculateLatitudePerRow();
    }

    /**
     * Gets the <code>LatitudePerRow</code> value.
     *
     * @return The latitude change between rows.
     */
    public final float getLatitudePerRow() {
        return latitudePerRow;
    }

    private final void calculateLatitudePerRow() {
        this.latitudePerRow = 1f * (maximumLatitude - minimumLatitude) /
            (getHeight() - 1);
    }

    /**
     * Gets the latitude of the given map row.
     *
     * @param row The row to check.
     * @return The row latitude.
     */
    public int getLatitude(int row) {
        return minimumLatitude + (int) (row * latitudePerRow);
    }

    /**
     * Gets the map row with the given latitude.
     *
     * @param latitude The latitude to find.
     * @return The row closest to the supplied latitude.
     */
    public int getRow(int latitude) {
        return (int) ((latitude - minimumLatitude) / latitudePerRow);
    }

    /**
     * Gets the regions in this map.
     *
     * @return All the regions in this map.
     */
    public Collection<Region> getRegions() {
        return regions;
    }

    /**
     * Get the fixed regions indexed by key.
     *
     * @return A map of the fixed regions.
     */
    public java.util.Map<String, Region> getFixedRegions() {
        HashMap<String, Region> result = new HashMap<>();
        for (Region r : getRegions()) {
            String n = r.getNameKey();
            if (r != null) result.put(n, r);
        }
        return result;
    }

    /**
     * Gets a <code>Region</code> by name key.
     *
     * @param key The name key to lookup the region with.
     * @return The region with the given name key, or null if not found.
     */
    public Region getRegionByKey(final String key) {
        return (key == null) ? null
            : find(getRegions(), r -> key.equals(r.getKey()));
    }

    /**
     * Gets a <code>Region</code> by name.
     *
     * @param name The region name.
     * @return The <code>Region</code> with the given name, or null if
     *     not found.
     */
    public Region getRegionByName(final String name) {
        return (name == null) ? null
            : find(getRegions(), r -> name.equals(r.getName()));
    }

    /**
     * Adds a region to this map.
     *
     * @param region The <code>Region</code> to add.
     */
    public void addRegion(final Region region) {
        regions.add(region);
    }


    /**
     * Are two locations non-null and either the same or at the same tile.
     * This routine is here because Location is an interface.
     *
     * @param l1 The first <code>Location</code>.
     * @param l2 The second <code>Location</code>.
     * @return True if the locations are the same or at the same tile.
     */
    public static final boolean isSameLocation(Location l1, Location l2) {
        return (l1 == null || l2 == null) ? false
            : (l1 == l2) ? true
            : (l1.getTile() == null) ? false
            : l1.getTile() == l2.getTile();
    }

    /**
     * Are two locations at least in the same contiguous land/sea-mass?
     * This routine is here because Location is an interface.
     *
     * @param l1 The first <code>Location</code>.
     * @param l2 The second <code>Location</code>.
     * @return True if the locations are the same or in the same land/sea-mass.
     */
    public static final boolean isSameContiguity(Location l1, Location l2) {
        return (l1 == null || l2 == null) ? false
            : (l1 == l2) ? true
            : (l1.getTile() == null || l2.getTile() == null) ? false
            : l1.getTile().isConnectedTo(l2.getTile());
    }            

    /**
     * Is a tile in the map in a polar region?
     *
     * @param tile The <code>Tile</code> to examine.
     * @return True if the tile is in a polar region.
     */
    public boolean isPolar(Tile tile) {
        return tile.getY() <= POLAR_HEIGHT
            || tile.getY() >= getHeight() - POLAR_HEIGHT - 1;
    }

    /**
     * Gets the direction a unit needs to move in
     * order to get from <code>t1</code> to <code>t2</code>
     *
     * @param t1 The tile to move from.
     * @param t2 The target tile if moving from <code>t1</code>
     *      in the direction returned by this method.
     * @return The direction you need to move from <code>t1</code>
     *      in order to reach <code>t2</code>, or null if the two
     *      specified tiles are not neighbours.
     */
    public Direction getDirection(Tile t1, Tile t2) {
        return (t1 == null || t2 == null) ? null
            : new Position(t1).getDirection(new Position(t2));
    }

    /**
     * Get the approximate direction from one tile to another.
     *
     * @param src The source <code>Tile</code>.
     * @param dst The destination <code>Tile</code>.
     * @return The approximate direction from source to direction, or null
     *     if source and destination are the same.
     */
    public static Direction getRoughDirection(Tile src, Tile dst) {
        int x = dst.getX() - src.getX();
        int y = dst.getY() - src.getY();
        if (x == 0 && y == 0) return null;
        double theta = Math.atan2(y, x) + Math.PI/2 + Math.PI/8;
        if (theta < 0) theta += 2 * Math.PI;
        return Direction.angleToDirection(theta);
    }

    /**
     * Gets the adjacent tile in a given direction from the given coordinates.
     *
     * @param x The x coordinate to work from.
     * @param y The y coordinate to work from.
     * @param direction The <code>Direction</code> to check.
     * @return The adjacent <code>Tile</code> in the specified
     *     direction, or null if invalid.
     */
    public Tile getAdjacentTile(int x, int y, Direction direction) {
        return getTile(direction.step(x, y));
    }

    /**
     * Gets the adjacent tile in a given direction from a given tile.
     *
     * @param tile The starting <code>Tile</code>.
     * @param direction The <code>Direction</code> to check.
     * @return The adjacent <code>Tile</code> in the specified
     *     direction, or null if invalid.
     */
    public Tile getAdjacentTile(Tile tile, Direction direction) {
        return getAdjacentTile(tile.getX(), tile.getY(), direction);
    }

    /**
     * Gets the distance between two tiles.
     *
     * @param t1 The first <code>Tile</code>.
     * @param t2 The second <code>Tile</code>.
     * @return The distance between the tiles.
     */
    public int getDistance(Tile t1, Tile t2) {
        return Position.getDistance(t1.getX(), t1.getY(),
                                    t2.getX(), t2.getY());
    }

    /**
     * Get the closest tile to a given one from a list of other tiles.
     *
     * @param tile The <code>Tile</code> to start from.
     * @param tiles The list of <code>Tile</code>s to check.
     * @return The closest tile found (may be null if the list is empty).
     */
    public Tile getClosestTile(Tile tile, Collection<Tile> tiles) {
        Tile result = null;
        int minimumDistance = Integer.MAX_VALUE;
        for (Tile t : tiles) {
            int distance = getDistance(t, tile);
            if (distance < minimumDistance) {
                minimumDistance = distance;
                result = t;
            }
        }
        return result;
    }

    /**
     * Select a random land tile on the map.
     *
     * @param random A <code>Random</code> number source.
     * @return A random land tile, or null if none found.
     */
    public Tile getRandomLandTile(Random random) {
        final int SLOSH = 10;
        int x = 0, y = 0, width = getWidth(), height = getHeight();
        if (width >= SLOSH) {
            width -= SLOSH;
            x += SLOSH/2;
        }
        if (height >= SLOSH) {
            height -= SLOSH;
            y += SLOSH/2;
        }
        x += randomInt(logger, "W", random, width);
        y += randomInt(logger, "H", random, height);
        for (Tile t : getCircleTiles(getTile(x, y), true, INFINITY)) {
            if (t.isLand()) return t;
        }
        return null;
    }


    // Path-finding/searching infrastructure and routines

    /**
     * Simple interface to supply a heuristic to the A* routine.
     */
    private interface SearchHeuristic {
        int getValue(Tile tile);
    }

    /**
     * Gets a search heuristic using the Manhatten distance to an end tile.
     *
     * @param endTile The <code>Tile</code> to aim for.
     * @return A new <code>SearchHeuristic</code> aiming for the end tile.
     */
    private SearchHeuristic getManhattenHeuristic(Tile endTile) {
        return (Tile tile) -> tile.getDistanceTo(endTile);
    }

    /**
     * Unified argument tests for full path searches, which then finds
     * the actual starting location for the path.  Deals with special
     * cases like starting on a carrier and/or high seas.
     *
     * @param unit The <code>Unit</code> to find the path for.
     * @param start The <code>Location</code> in which the path starts from.
     * @param carrier An optional naval carrier <code>Unit</code> to use.
     * @return The actual starting location.
     * @throws IllegalArgumentException If there are any argument problems.
     */
    private Location findRealStart(final Unit unit, final Location start,
                                   final Unit carrier) {
        // Unit checks.
        if (unit == null) {
            throw new IllegalArgumentException("Null unit.");
        } else if (carrier != null && !carrier.canCarryUnits()) {
            throw new IllegalArgumentException("Non-carrier carrier: "
                + carrier);
        } else if (carrier != null && !carrier.couldCarry(unit)) {
            throw new IllegalArgumentException("Carrier could not carry unit: "
                + carrier + "/" + unit);
        }

        Location entry;
        if (start == null) {
            throw new IllegalArgumentException("Null start: " + unit);
        } else if (start instanceof Unit) {
            Location unitLoc = ((Unit)start).getLocation();
            if (unitLoc == null) {
                throw new IllegalArgumentException("Null on-carrier start: "
                    + unit + "/" + start);
            } else if (unitLoc instanceof HighSeas) {
                if (carrier == null) {
                    throw new IllegalArgumentException("Null carrier when"
                        + " starting on high seas: " + unit);
                } else if (carrier != start) {
                    throw new IllegalArgumentException("Wrong carrier when"
                        + " starting on high seas: " + unit
                        + "/" + carrier + " != " + start);
                }
                entry = carrier.resolveDestination();
            } else {
                entry = unitLoc;
            }
            
        } else if (start instanceof HighSeas) {
            if (unit.isOnCarrier()) {
                entry = unit.getCarrier().resolveDestination();
            } else if (unit.isNaval()) {
                entry = unit.resolveDestination();
            } else {
                throw new IllegalArgumentException("No carrier when"
                    + " starting on high seas: " + unit
                    + "/" + unit.getLocation());
            }
        } else if (start instanceof Europe || start.getTile() != null) {
            entry = start; // OK
        } else {
            throw new IllegalArgumentException("Invalid start: " + start);
        }
        // Valid result, reduce to tile if possible.
        return (entry.getTile() != null) ? entry.getTile() : entry;
    }

    /**
     * Destination argument test for path searches.  Find the actual
     * destination of a path.
     *
     * @param end The candidate end <code>Location</code>.
     * @return The actual end location.
     * @throws IllegalArgumentException If there are any argument problems.
     */
    private Location findRealEnd(Location end) {
        if (end == null) {
            throw new IllegalArgumentException("Null end.");
        } else if (end instanceof Europe) {
            return end;
        } else if (end.getTile() != null) {
            return end.getTile();
        } else {
            throw new IllegalArgumentException("Invalid end: " + end);
        }
    }

    /**
     * Gets the best (closest) path location for this unit to reach a
     * given tile from off the map.
     *
     * @param unit The <code>Unit</code> to check.
     * @param tile The target <code>Tile</code>.
     * @param carrier An optional carrier <code>Unit</code>to use.
     * @param costDecider An optional <code>CostDecider</code> to use.
     * @return A path to the best entry location tile to arrive on the
     *     map at, or null if none found.
     */
    private PathNode getBestEntryPath(Unit unit, Tile tile, Unit carrier,
                                      CostDecider costDecider) {
        return searchMap(unit, tile, GoalDeciders.getHighSeasGoalDecider(),
            ((costDecider != null) ? costDecider
                : CostDeciders.avoidSettlementsAndBlockingUnits()),
            INFINITY, carrier, null, null);
    }

    /**
     * Gets the best (closest) entry location for this unit to reach a
     * given tile from off the map.
     *
     * @param unit The <code>Unit</code> to check.
     * @param tile The target <code>Tile</code>.
     * @param carrier An optional carrier <code>Unit</code>to use.
     * @param costDecider An optional <code>CostDecider</code> to use.
     * @return The best entry location tile to arrive on the map at, or null
     *     if none found.
     */
    public Tile getBestEntryTile(Unit unit, Tile tile, Unit carrier,
                                 CostDecider costDecider) {
        PathNode path = getBestEntryPath(unit, tile, carrier, costDecider);
        return (path == null) ? null : path.getLastNode().getTile();
    }

    /**
     * Find the quickest path for a unit (with optional carrier) from
     * a start tile to an end tile.
     *
     * @param unit The <code>Unit</code> to find the path for.
     * @param start The <code>Tile</code> in which the path starts from.
     * @param end The <code>Tile</code> at the end of the path.
     * @param carrier An optional naval carrier <code>Unit</code> to use.
     * @param costDecider An optional <code>CostDecider</code> for
     *     determining the movement costs (uses default cost deciders
     *     for the unit/s if not provided).
     * @param lb An optional <code>LogBuilder</code> to log to.
     * @return A path starting at the start tile and ending at the end
     *     tile, or null if none found.
     */
    private PathNode findMapPath(Unit unit, Tile start, Tile end, Unit carrier,
                                 CostDecider costDecider, LogBuilder lb) {
        final Unit offMapUnit = (carrier != null) ? carrier
            : (unit != null && unit.isNaval()) ? unit
            : null;
        final GoalDecider gd = GoalDeciders.getLocationGoalDecider(end);
        final SearchHeuristic sh = getManhattenHeuristic(end);
        Unit embarkTo;

        PathNode path;
        if (start.getContiguity() == end.getContiguity()) {
            // If the unit potentially could get to the destination
            // without a carrier, compare both with-carrier and
            // without-carrier paths.  The latter will usually be
            // faster, but not always, e.g. mounted units on a good
            // road system.
            path = searchMap(unit, start, gd, costDecider,
                             INFINITY, null, sh, lb);
            PathNode carrierPath = (carrier == null) ? null
                : searchMap(unit, start, gd, costDecider,
                            INFINITY, carrier, sh, lb);
            if (carrierPath != null
                && (path == null
                    || (path.getLastNode().getCost()
                        > carrierPath.getLastNode().getCost()))) {
                path = carrierPath;
            }

        } else if (offMapUnit != null) {
            // If there is an off-map unit then complex paths which
            // use settlements and inland lakes are possible, but hard
            // to capture with the contiguity test, so just allow the
            // search to proceed.
            path = searchMap(unit, start, gd, costDecider,
                             INFINITY, carrier, sh, lb);

        } else if (unit != null && unit.isOnCarrier()
            && !start.isLand() && end.isLand()
            && !start.getContiguityAdjacent(end.getContiguity()).isEmpty()) {
            // Special case where a land unit is trying to move off a
            // ship to adjacent land.
            path = searchMap(unit, start, gd, costDecider, INFINITY,
                             carrier, sh, lb);

        } else if (start.isLand() && !end.isLand()
            && end.getFirstUnit() != null
            && !end.getContiguityAdjacent(start.getContiguity()).isEmpty()
            && unit != null && unit.getOwner().owns(end.getFirstUnit())
            && (embarkTo = end.getCarrierForUnit(unit)) != null) {
            // Special case where a land unit is trying to move from
            // land to an adjacent ship.
            path = searchMap(unit, start,
                GoalDeciders.getAdjacentLocationGoalDecider(end), costDecider,
                INFINITY, null, null, lb);
            if (path != null) {
                PathNode last = path.getLastNode();
                last.next = new PathNode(embarkTo, 0, last.getTurns()+1, true,
                                         last, null);
            }
        } else { // Otherwise, there is a connectivity failure.
            path = null;
        }
        return path;
    }

    /**
     * Finish processing a path.
     *
     * @param path The <code>PathNode</code> to finish.
     * @param unit The <code>Unit</code> that is travelling along the path.
     * @param lb An optional <code>LogBuilder</code> to log to.
     */
    private void finishPath(PathNode path, Unit unit, LogBuilder lb) {
        if (path != null) {
            // Add the turns remaining on the high seas.
            final int initialTurns = (!unit.isAtSea()) ? 0
                : ((unit.isOnCarrier()) ? unit.getCarrier() : unit)
                .getWorkLeft();
            if (initialTurns != 0) path.addTurns(initialTurns);

            if (lb != null) {
                lb.add("\nSuccess\n", path.fullPathToString());
            }
        }
        if (lb != null) lb.log(logger, Level.INFO);
    }
        
    /**
     * Find the quickest path for a unit (with optional carrier) from
     * a start location to an end location.
     *
     * @param unit The <code>Unit</code> to find the path for.
     * @param start The <code>Location</code> in which the path starts from.
     * @param end The <code>Location</code> at the end of the path.
     * @param carrier An optional naval carrier <code>Unit</code> to use.
     * @param costDecider An optional <code>CostDecider</code> for
     *     determining the movement costs (uses default cost deciders
     *     for the unit/s if not provided).
     * @param lb An optional <code>LogBuilder</code> to log to.
     * @return A path starting at the start location and ending at the
     *     end location, or null if none found.
     * @throws IllegalArgumentException For many reasons, see
     *     {@link #findRealStart}.
     */
    public PathNode findPath(final Unit unit,
                             final Location start, final Location end,
                             final Unit carrier, CostDecider costDecider,
                             LogBuilder lb) {
        if (traceSearch) lb = new LogBuilder(1024);

        // Validate the arguments, reducing to either Europe or a Tile.
        final Location realStart = findRealStart(unit, start, carrier);
        final Location realEnd;
        try {
            realEnd = findRealEnd(end);
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException("Path fail: " + unit
                + " from " + start + " to " + end + " with " + carrier, iae);
        }
        // Get the unit that will be used for off-map travel.
        final Unit offMapUnit = (carrier != null) ? carrier : unit;

        PathNode p, path;
        Tile tile;
        if (realEnd instanceof Tile && !((Tile)realEnd).isExplored()) {
            // Do not allow finding a path into unexplored territory,
            // as we do not have the terrain type and thus can not
            // calculate costs.
            path = null;

        } else if (realStart instanceof Europe && realEnd instanceof Europe) {
            // 0: Europe->Europe: Create a trivial path.
            path = new PathNode(realStart, unit.getMovesLeft(), 0,
                                false, null, null);

        } else if (realStart instanceof Europe && realEnd instanceof Tile) {
            // 1: Europe->Tile
            // Fail fast without an off map unit.
            if (offMapUnit == null
                || !offMapUnit.getType().canMoveToHighSeas()) {
                path = null;

            // Find the best place to enter the map from Europe
            } else if ((p = getBestEntryPath(unit, (Tile)realEnd, carrier,
                                             costDecider)) == null) {
                path = null;

            // Now search forward from there to get a path in the
            // right order (path costs are not symmetric).  There are
            // "expected" failures when rivers block due to foreign
            // ship movement.  There are also other failures which we
            // would like to log.  Try to filter out the first case.
            } else if ((path = findMapPath(unit,
                        (tile = p.getLastNode().getTile()), (Tile)realEnd,
                        carrier, costDecider, lb)) == null) {
                if (!((Tile)realEnd).isOnRiver()) {
                    LogBuilder l2 = new LogBuilder(512);
                    l2.add("Fail in findPath(", unit, ", ", tile,
                        ", ", realEnd, ", ", carrier, ")\n");
                    l2.addStackTrace();
                    l2.add(p.fullPathToString());
                    findMapPath(unit, tile, (Tile)realEnd,
                                carrier, costDecider, l2);
                    l2.log(logger, Level.WARNING);
                }
                path = null;

            // At the front of the path insert a node for the starting
            // location in Europe, correcting for the turns to sail to
            // the entry location.
            } else {
                path.addTurns(offMapUnit.getSailTurns());
                path.previous = new PathNode(realStart, unit.getMovesLeft(),
                                             0, carrier != null, null, path);
                path = path.previous;
                if (carrier != null && unit.getLocation() != carrier) {
                    path.previous = new PathNode(realStart, unit.getMovesLeft(),
                                                 0, false, null, path);
                    path = path.previous;
                }
            }

        } else if (realStart instanceof Tile && realEnd instanceof Europe) {
            // 2: Tile->Europe
            // Fail fast if Europe is unattainable.
            if (offMapUnit == null
                || !offMapUnit.getType().canMoveToHighSeas()) {
                path = null;
                
            // Search forwards to the high seas.
            } else if ((p = searchMap(unit, (Tile)realStart,
                        GoalDeciders.getHighSeasGoalDecider(),
                        costDecider, INFINITY, carrier, null, lb)) == null) {
                path = null;

            } else {
                PathNode last = p.getLastNode();
                last.next = new PathNode(realEnd, unit.getInitialMovesLeft(),
                    last.getTurns() + offMapUnit.getSailTurns(),
                    last.isOnCarrier(), last, null);
                path = p;
            }

        } else if (realStart instanceof Tile && realEnd instanceof Tile) {
            // 3: Tile->Tile
            path = findMapPath(unit, (Tile)realStart, (Tile)realEnd, carrier,
                               costDecider, lb);

        } else {
            throw new IllegalStateException("Can not happen: " + realStart
                                            + ", " + realEnd);
        }

        finishPath(path, unit, lb);
        return path;
    }

    /**
     * Searches for a goal.
     * Assumes units in Europe return to their current entry location,
     * which is not optimal most of the time.
     * Returns the full path including the start and end locations.
     *
     * @param unit The <code>Unit</code> to find a path for.
     * @param start The <code>Location</code> to start the search from.
     * @param goalDecider The object responsible for determining whether a
     *     given <code>PathNode</code> is a goal or not.
     * @param costDecider An optional <code>CostDecider</code>
     *     responsible for determining the path cost.
     * @param maxTurns The maximum number of turns the given
     *     <code>Unit</code> is allowed to move.  This is the
     *     maximum search range for a goal.
     * @param carrier An optional naval carrier <code>Unit</code> to use.
     * @param lb An optional <code>LogBuilder</code> to log to.
     * @return The path to a goal, or null if none can be found.
     * @throws IllegalArgumentException If the unit is null, or the
     *     start location does not make sense, or the carrier/unit
     *     combination is bogus.
     */
    public PathNode search(final Unit unit, Location start,
                           final GoalDecider goalDecider,
                           final CostDecider costDecider,
                           final int maxTurns, final Unit carrier,
                           LogBuilder lb) {
        if (traceSearch) lb = new LogBuilder(1024);

        final Location realStart = findRealStart(unit, start, carrier);
        final Unit offMapUnit = (carrier != null) ? carrier : unit;
        
        PathNode p, path;
        if (realStart instanceof Europe) {
            // Fail fast if Europe is unattainable.
            if (offMapUnit == null
                || !offMapUnit.getType().canMoveToHighSeas()) {
                path = null;

            // This is suboptimal.  We do not know where to enter from
            // Europe, so start with the standard entry location...
            } else if ((p = searchMap(unit,
                        (Tile)offMapUnit.getEntryLocation(),
                        goalDecider, costDecider, maxTurns, carrier,
                        null, lb)) == null) {
                path = null;

            // ...then if we find a path, try to optimize it.  This
            // will lose if the initial search fails due to a turn limit.
            // FIXME: do something better.
            } else {
                path = findPath(unit, realStart, p.getLastNode().getTile(),
                                carrier, costDecider, lb);
            }

        } else {
            path = searchMap(unit, realStart.getTile(), goalDecider,
                             costDecider, maxTurns, carrier, null, lb);
        }

        finishPath(path, unit, lb);
        return path;
    }

    /**
     * Gets the search tracing status.
     *
     * @return The search tracing status.
     */
    public boolean getSearchTrace() {
        return traceSearch;
    }

    /**
     * Sets the search tracing status.
     *
     * @param trace The new search tracing status.
     * @return The original search tracing status.
     */
    public boolean setSearchTrace(boolean trace) {
        boolean ret = traceSearch;
        traceSearch = trace;
        return ret;
    }

    /**
     * Was a carrier used previously on a path?
     *
     * Beware!  This is special case code for partially constructed
     * paths that do not yet have valid .next links, so we can not use the
     * generic PathNode routines.
     *
     * @param path The path the search.
     * @return True if the path includes a previous on-carrier node.
     */
    private boolean usedCarrier(PathNode path) {
        while (path != null) {
            if (path.isOnCarrier()) return true;
            path = path.previous;
        }
        return false;
    }

    /**
     * Internal class for evaluating a candidate move.
     */
    private class MoveCandidate {

        private Unit unit;
        private final PathNode current;
        private final Location dst;
        private int movesLeft;
        private int turns;
        private final boolean onCarrier;
        private final CostDecider decider;
        private int cost;
        private PathNode path;


        /**
         * Creates a new move candidate where a cost decider will be used
         * to work out the new moves and turns left.
         *
         * @param unit The <code>Unit</code> to move.
         * @param current The current position on the path.
         * @param dst The <code>Location</code> to move to.
         * @param movesLeft The initial number of moves left.
         * @param turns The initial number of turns.
         * @param onCarrier Will the new move be on a carrier.
         * @param decider The <code>CostDecider</code> to use.
         */
        public MoveCandidate(Unit unit, PathNode current, Location dst,
                             int movesLeft, int turns, boolean onCarrier,
                             CostDecider decider) {
            this.unit = unit;
            this.current = current;
            this.dst = dst;
            this.movesLeft = movesLeft;
            this.turns = turns;
            this.onCarrier = onCarrier;
            this.decider = decider;
            this.cost = decider.getCost(unit, current.getLocation(),
                                        dst, movesLeft);
            if (this.cost != CostDecider.ILLEGAL_MOVE) {
                this.turns += decider.getNewTurns();
                this.movesLeft = decider.getMovesLeft();
                this.cost = PathNode.getCost(this.turns, this.movesLeft);
            }
            this.path = null;
        }

        /**
         * Get the cost.
         *
         * @return The current move cost.
         */
        public int getCost() {
            return this.cost;
        }

        /**
         * Handles the change of unit as a result of an embark.
         */
        public void embarkUnit(Unit unit) {
            this.unit = unit;
            this.movesLeft = unit.getInitialMovesLeft();
            this.cost = PathNode.getCost(turns, movesLeft);
        }

        /**
         * Resets the path.  Required after the parameters change.
         *
         * @param goal True if this is a goal node.
         */
        public void resetPath(boolean goal) {
            path = new PathNode(dst, movesLeft, turns, onCarrier,
                                current, null);
            if (goal) {
                // Do not let the CostDecider (which may be
                // conservative) block a final destination.  This
                // allows planning routines to compute paths to tiles
                // temporarily occupied by an enemy unit, or for an
                // empty ship to find a compound path to a native
                // settlement where the first step is to collect the
                // cargo it needs to make the final move legal.
                if (cost == CostDecider.ILLEGAL_MOVE
                    && unit != null
                    && current.getTile() != null
                    && dst.getTile() != null) {
                    // Pretend it finishes the move.
                    movesLeft = unit.getInitialMovesLeft();
                    turns++;
                    path = new PathNode(dst, movesLeft, turns, onCarrier,
                                        current, null);
                }

                // Add an extra step to disembark from a carrier at a
                // settlement.  If this is omitted, then a path that
                // disembarks a unit from its carrier on an adjacent
                // tile looks unfairly expensive.
                Settlement s;
                if (unit != null && path.isOnCarrier()
                    && (s = path.getLocation().getSettlement()) != null
                    && unit.getOwner().owns(s)) {
                    movesLeft = 0;
                    if (path.embarkedThisTurn(turns)) turns++;
                    path = new PathNode(s.getTile(), 0, turns, false,
                                        path, null);
                }
                cost = PathNode.getCost(turns, movesLeft);
            }
        }

        /**
         * Does this move candidate improve on a specified move.
         *
         * @param best The <code>PathNode</code> to compare against.
         */
        public boolean canImprove(PathNode best) {
            return cost != CostDecider.ILLEGAL_MOVE
                && (best == null || cost < best.getCost()
                    || (cost == best.getCost()
                        && best.getLength() < path.getLength()));
        }

        /**
         * Replace a given path with that of this candidate move.
         *
         * @param openMap The list of available nodes.
         * @param openMapQueue The queue of available nodes.
         * @param f The heuristic values for A*.
         * @param sh An optional <code>SearchHeuristic</code> to apply.
         */
        public void improve(HashMap<String, PathNode> openMap,
                            PriorityQueue<PathNode> openMapQueue,
                            HashMap<String, Integer> f,
                            SearchHeuristic sh) {
            PathNode best = openMap.get(dst.getId());
            if (best != null) {
                openMap.remove(dst.getId());
                openMapQueue.remove(best);
            }
            int fcost = cost;
            if (sh != null && dst.getTile() != null) {
                fcost += sh.getValue(dst.getTile());
            }
            f.put(dst.getId(), fcost);
            openMap.put(dst.getId(), path);
            openMapQueue.offer(path);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("[candidate unit=").append(unit)
                .append(" dst=").append(dst)
                .append(" movesLeft=").append(movesLeft)
                .append(" turns=").append(turns)
                .append(" onCarrier=").append(onCarrier)
                .append(" decider=").append(decider)
                .append(" cost=").append(cost)
                .append("]");
            return sb.toString();
        }
    };

    /**
     * Searches for a path to a goal determined by the given
     * <code>GoalDecider</code>.
     *
     * Using A* with a List (closedMap) for marking the visited nodes
     * and using a PriorityQueue (openMapQueue) for getting the next
     * edge with the least cost.  This implementation could be
     * improved by having the visited attribute stored on each Tile in
     * order to avoid both of the HashMaps currently being used to
     * serve this purpose.
     *
     * If the SearchHeuristic is not supplied, then the algorithm
     * degrades gracefully to Dijkstra's algorithm.
     *
     * The data structure for the open list is a combined structure: using a
     * HashMap for membership tests and a PriorityQueue for getting the node
     * with the minimal f (cost+heuristics). This gives O(1) on membership
     * test and O(log N) for remove-best and insertions.
     *
     * @param unit The <code>Unit</code> to find a path for.
     * @param start The <code>Tile</code> to start the search from.
     * @param goalDecider The object responsible for determining whether a
     *     given <code>PathNode</code> is a goal or not.
     * @param costDecider An optional <code>CostDecider</code>
     *     responsible for determining the path cost.
     * @param maxTurns The maximum number of turns the given
     *     <code>Unit</code> is allowed to move. This is the
     *     maximum search range for a goal.
     * @param carrier An optional naval carrier <code>Unit</code> to use.
     * @param searchHeuristic An optional <code>SearchHeuristic</code>.
     * @param lb An optional <code>LogBuilder</code> to log to.
     * @return A path to a goal determined by the given
     *     <code>GoalDecider</code>.
     */
    private PathNode searchMap(final Unit unit, final Tile start,
                               final GoalDecider goalDecider,
                               final CostDecider costDecider,
                               final int maxTurns, final Unit carrier,
                               final SearchHeuristic searchHeuristic,
                               final LogBuilder lb) {
        final HashMap<String, PathNode> openMap = new HashMap<>();
        final HashMap<String, PathNode> closedMap = new HashMap<>();
        final HashMap<String, Integer> f = new HashMap<>();
        final PriorityQueue<PathNode> openMapQueue = new PriorityQueue<>(1024,
                new Comparator<PathNode>() {
                    @Override
                    public int compare(PathNode p1, PathNode p2) {
                        return (f.get(p1.getLocation().getId())
                            - f.get(p2.getLocation().getId()));
                    }
                });
        final Europe europe = (unit == null) ? null
            : unit.getOwner().getEurope();
        final Unit offMapUnit = (carrier != null) ? carrier : unit;
        Unit currentUnit = (start.isLand())
            ? ((start.hasSettlement()
                    && start.getSettlement().isConnectedPort()
                    && unit != null
                    && unit.getLocation() == carrier) ? carrier : unit)
            : offMapUnit;
        if (lb != null) lb.add("Search trace(unit=", unit,
            ", from=", start,
            ", max=", ((maxTurns == INFINITY)?"-":Integer.toString(maxTurns)),
            ", carrier=", carrier, ")");

        // Create the start node and put it on the open list.
        final PathNode firstNode = new PathNode(start,
            ((currentUnit != null) ? currentUnit.getMovesLeft() : -1),
            0, carrier != null && currentUnit == carrier, null, null);
        f.put(start.getId(), (searchHeuristic == null) ? 0
                : searchHeuristic.getValue(start));
        openMap.put(start.getId(), firstNode);
        openMapQueue.offer(firstNode);

        PathNode best = null;
        int bestScore = INFINITY;
        while (!openMap.isEmpty()) {
            // Choose the node with the lowest f.
            final PathNode currentNode = openMapQueue.poll();
            final Location currentLocation = currentNode.getLocation();
            openMap.remove(currentLocation.getId());
            if (lb != null) lb.add("\n  ", currentNode);

            // Reset current unit to that of this node.
            currentUnit = (currentNode.isOnCarrier()) ? carrier : unit;

            // Check for success.
            if (goalDecider.check(currentUnit, currentNode)) {
                if (lb != null) lb.add(" ***goal(",
                    currentNode.getCost(), ")***");
                best = goalDecider.getGoal();
                bestScore = best.getCost();
                if (!goalDecider.hasSubGoals()) break;
                continue;
            }

            // Skip nodes that can not beat the current best path.
            if (bestScore < currentNode.getCost()) {
                closedMap.put(currentLocation.getId(), currentNode);
                if (lb != null) lb.add(" ...goal cost wins(",
                    bestScore, " < ", currentNode.getCost(), ")...");
                continue;
            }

            // Ignore nodes over the turn limit.
            if (currentNode.getTurns() > maxTurns) {
                if (lb != null) lb.add("...out-of-range");
                continue;
            }

            // Valid candidate for the closed list.
            closedMap.put(currentLocation.getId(), currentNode);
            if (lb != null) lb.add("...close");

            // Collect the parameters for the current node.
            final int currentMovesLeft = currentNode.getMovesLeft();
            final int currentTurns = currentNode.getTurns();
            final boolean currentOnCarrier = currentNode.isOnCarrier();

            final Tile currentTile = currentNode.getTile();
            if (currentTile == null) { // Must be in Europe.
                // FIXME: Do not consider tiles "adjacent" to Europe, yet.
                // There may indeed be cases where going to Europe and
                // coming back on the other side of the map is faster.
                if (lb != null) lb.add("...skip Europe");
                continue;
            }

            // Try the tiles in each direction
            PathNode closed;
            for (Tile moveTile : currentTile.getSurroundingTiles(1)) {
                // If the new tile is the tile we just visited, skip it.
                if (lb != null) lb.add("\n    ", moveTile);
                if (currentNode.previous != null
                    && currentNode.previous.getTile() == moveTile) {
                    if (lb != null) lb.add(" prev");
                    continue;
                }

                // Skip neighbouring tiles already too expensive.
                int cc;
                if ((closed = closedMap.get(moveTile.getId())) != null
                    && (cc = closed.getCost()) <= currentNode.getCost()) {
                    if (lb != null) lb.add(" ", cc);
                    continue;
                }

                // Is this move to the goal?  Use fake high cost so
                // this does not become cached inside the goal decider
                // as the preferred path.
                boolean isGoal = goalDecider.check(unit,
                    new PathNode(moveTile, 0, INFINITY/2, false,
                        currentNode, null));
                if (isGoal && lb != null) lb.add(" *goal*");

                // Is this move possible for the base unit?
                // Allow some seemingly impossible moves if it is to
                // the goal (see the comment to recoverMove).
                Unit.MoveType umt = unit.getSimpleMoveType(currentTile,
                                                           moveTile);
                boolean carrierMove = carrier != null
                    && carrier.isTileAccessible(moveTile);
                boolean unitMove = umt.isProgress();
                if (isGoal) {
                    if (!unitMove) {
                        switch (umt) {
                        case ATTACK_UNIT:
                        case ATTACK_SETTLEMENT:
                        case ENTER_FOREIGN_COLONY_WITH_SCOUT:
                        case ENTER_INDIAN_SETTLEMENT_WITH_SCOUT:
                        case ENTER_INDIAN_SETTLEMENT_WITH_FREE_COLONIST:
                        case ENTER_INDIAN_SETTLEMENT_WITH_MISSIONARY:
                        case ENTER_SETTLEMENT_WITH_CARRIER_AND_GOODS:
                            // Can not move to the tile, but there is
                            // a valid interaction with the unit or
                            // settlement that is there.
                            unitMove = true;
                            break;
                        case MOVE_NO_ATTACK_MARINE:
                        case MOVE_NO_ATTACK_CIVILIAN:
                            if (moveTile.hasSettlement()) break;
                            // There is a unit in the way.  Unless this
                            // unit can arrive there this turn, assume the
                            // condition is transient as long as the tile
                            // is not in a constrained position such as a
                            // small island or river.
                            unitMove = currentNode.getTurns() > 0
                                && moveTile.getAvailableAdjacentCount() >= 3;
                            break;
                        case MOVE_NO_ACCESS_WATER:
                            // The unit can not disembark directly to the
                            // goal along this path, but the goal is still
                            // available by other paths.
                            if (lb != null) lb.add(" !disembark");
                            continue;
                        default:
                            break;
                        }
                        if (!unitMove && unit == currentUnit) {
                            // This search can never succeed if the unit
                            // can not reach the goal, except if there is
                            // a carrier involved that might still succeed.
                            if (lb != null) lb.add(" fail-at-GOAL(", umt, ")");
                            continue;
                        }
                    }
                    // Special case where the carrier is adjacent to
                    // an accessible goal settlement but out of moves,
                    // in which case we let the unit finish the job
                    // if it can move.
                    if (unitMove && carrierMove && currentOnCarrier) {
                        carrierMove = currentNode.getMovesLeft() > 0
                            || currentNode.embarkedThisTurn(currentTurns);
                    }
                }
                if (lb != null) lb.add(" ", umt, "/",
                    ((unitMove) ? "U" : ""), ((carrierMove) ? "C" : ""));

                // Check for a carrier change at the new tile,
                // creating a MoveCandidate for each case.
                //
                // Do *not* allow units to re-embark on the carrier.
                // Note that embarking can actually increase the moves
                // left because the carrier might be not have spent
                // any moves yet that turn.
                //
                // Note that we always favour using the carrier if
                // both carrier and non-carrier moves are possible,
                // which can only be true moving into a settlement.
                // Usually when moving into a settlement it will be
                // useful to dock the carrier so it can collect new
                // cargo.  OTOH if the carrier is just passing through
                // the right thing is to keep the passenger on board.
                // However, see the goal settlement exception above.
                MoveStep step = (currentOnCarrier)
                    ? ((carrierMove) ? MoveStep.BYWATER
                        : (unitMove) ? MoveStep.DISEMBARK
                        : MoveStep.FAIL)
                    : ((carrierMove && !usedCarrier(currentNode))
                        ? MoveStep.EMBARK
                        : (unitMove || isGoal) ? ((unit.isNaval())
                            ? MoveStep.BYWATER
                            : MoveStep.BYLAND)
                        : MoveStep.FAIL);
                MoveCandidate move;
                switch (step) {
                case BYLAND:
                    move = new MoveCandidate(unit, currentNode, moveTile, 
                        currentMovesLeft, currentTurns, false,
                        ((costDecider != null) ? costDecider
                            : CostDeciders.defaultCostDeciderFor(unit)));
                    break;
                case BYWATER:
                    move = new MoveCandidate(offMapUnit, currentNode, moveTile,
                        currentMovesLeft, currentTurns, currentOnCarrier,
                        ((costDecider != null) ? costDecider
                            : CostDeciders.defaultCostDeciderFor(offMapUnit)));
                    break;
                case EMBARK:
                    move = new MoveCandidate(unit, currentNode, moveTile,
                        currentMovesLeft, currentTurns, true,
                        ((costDecider != null) ? costDecider
                            : CostDeciders.defaultCostDeciderFor(unit)));
                    move.embarkUnit(carrier);
                    break;
                case DISEMBARK:
                    move = new MoveCandidate(unit, currentNode, moveTile,
                        0, currentTurns, false,
                        ((costDecider != null) ? costDecider
                            : CostDeciders.defaultCostDeciderFor(unit)));
                    break;
                case FAIL: default: // Loop on failure.
                    move = null;
                    break;
                }

                String stepLog;
                if (move == null) {
                    stepLog = "!";
                } else {
                    move.resetPath(isGoal);
                    // Tighten the bounds on a previously seen case if possible
                    if (closed != null) {
                        if (move.canImprove(closed)) {
                            closedMap.remove(moveTile.getId());
                            move.improve(openMap, openMapQueue, f,
                                         searchHeuristic);
                            stepLog = "^" + Integer.toString(move.getCost());
                        } else {
                            stepLog = ".";
                        }
                    } else if (move.canImprove(openMap.get(moveTile.getId()))){
                        move.improve(openMap, openMapQueue, f,
                                     searchHeuristic);
                        stepLog = "+" + Integer.toString(move.getCost());
                    } else {
                        stepLog = "-";
                    }
                }
                if (lb != null) lb.add(" ", step, stepLog);
            }
        }

        // Relink the path.  We omitted the .next link while constructing it.
        best = goalDecider.getGoal();
        if (best != null) {
            while (best.previous != null) {
                best.previous.next = best;
                best = best.previous;
            }
        }
        return best;
    }

    /**
     * Searches for a tile within a radius of a starting tile.
     *
     * Does not use a unit, and thus does not consider movement validity.
     *
     * @param start The starting <code>Tile</code>.
     * @param goalDecider A <code>GoalDecider</code> that chooses the goal,
     *     which must be capable of tolerating a null unit.
     * @param radius The maximum radius of tiles to search from the start.
     * @return The goal tile as determined by the, or null if none found.
     */
    public Tile searchCircle(final Tile start, final GoalDecider goalDecider,
                             final int radius) {
        if (start == null || goalDecider == null || radius <= 0) return null;

        for (Tile t : getCircleTiles(start, true, radius)) {
            PathNode path = new PathNode(t, 0, start.getDistanceTo(t), false,
                                         null, null);
            if (goalDecider.check(null, path)
                && !goalDecider.hasSubGoals())
                break;
        }
        
        PathNode best = goalDecider.getGoal();
        return (best == null) ? null : best.getTile();
    }

    
    // Support for various kinds of map iteration.

    /**
     * An iterator returning positions in a spiral starting at a given
     * center tile.  The center tile is never included in the returned
     * tiles, and all returned tiles are valid.
     */
    private final class CircleIterator implements Iterator<Tile> {

        /** The maximum radius. */
        private final int radius;
        /** The current radius of the iteration. */
        private int currentRadius;
        /** The current index in the circle with the current radius: */
        private int n;
        /** The current position in the circle. */
        private int x, y;


        /**
         * Create a new Circle Iterator.
         *
         * @param center The center <code>Tile</code> of the circle.
         * @param isFilled True to get all of the positions within the circle.
         * @param radius The radius of the circle.
         */
        public CircleIterator(Tile center, boolean isFilled, int radius) {
            if (center == null) {
                throw new IllegalArgumentException("center must not be null.");
            }
            this.radius = radius;
            n = 0;

            Position step;
            if (isFilled || radius == 1) {
                step = Direction.NE.step(center.getX(), center.getY());
                x = step.x;
                y = step.y;
                currentRadius = 1;
            } else {
                this.currentRadius = radius;
                x = center.getX();
                y = center.getY();
                for (int i = 1; i < radius; i++) {
                    step = Direction.N.step(x, y);
                    x = step.x;
                    y = step.y;
                }
                step = Direction.NE.step(x, y);
                x = step.x;
                y = step.y;
            }
            if (!isValid(x, y)) nextTile();
        }

        /**
         * Gets the current radius of the circle.
         *
         * @return The distance from the center tile this
         *     <code>CircleIterator</code> was initialized with.
         */
        public int getCurrentRadius() {
            return currentRadius;
        }

        /**
         * Finds the next position.
         */
        private void nextTile() {
            boolean started = n != 0;
            do {
                n++;
                final int width = currentRadius * 2;
                if (n >= width * 4) {
                    currentRadius++;
                    if (currentRadius > radius) {
                        x = y = UNDEFINED;
                        break;
                    } else if (!started) {
                        x = y = UNDEFINED;
                        break;
                    } else {
                        n = 0;
                        started = false;
                        Position step = Direction.NE.step(x, y);
                        x = step.x;
                        y = step.y;
                    }
                } else {
                    int i = n / width;
                    Direction direction;
                    switch (i) {
                    case 0:
                        direction = Direction.SE;
                        break;
                    case 1:
                        direction = Direction.SW;
                        break;
                    case 2:
                        direction = Direction.NW;
                        break;
                    case 3:
                        direction = Direction.NE;
                        break;
                    default:
                        throw new IllegalStateException("i=" + i + ", n=" + n
                                                        + ", width=" + width);
                    }
                    Position step = direction.step(x, y);
                    x = step.x;
                    y = step.y;
                }
            } while (!isValid(x, y));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            return x != UNDEFINED && y != UNDEFINED;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Tile next() throws NoSuchElementException {
            if (!hasNext()) {
                throw new NoSuchElementException("CircleIterator exhausted");
            }
            Tile result = getTile(x, y);
            nextTile();
            return result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Gets a circle iterator.
     *
     * @param center The center <code>Tile</code> to iterate around.
     * @param isFilled True to get all of the positions in the circle.
     * @param radius The radius of circle.
     * @return The circle iterator.
     */
    public Iterator<Tile> getCircleIterator(Tile center, boolean isFilled,
                                            int radius) {
        return new CircleIterator(center, isFilled, radius);
    }

    /**
     * Gets an iterable for all the tiles in a circle using an
     * underlying CircleIterator.
     *
     * @param center The center <code>Tile</code> to iterate around.
     * @param isFilled True to get all of the positions in the circle.
     * @param radius The radius of circle.
     * @return An <code>Iterable</code> for a circle of tiles.
     */
    public Iterable<Tile> getCircleTiles(final Tile center,
                                         final boolean isFilled,
                                         final int radius) {
        return new Iterable<Tile>() {
            @Override
            public Iterator<Tile> iterator() {
                return getCircleIterator(center, isFilled, radius);
            }
        };
    }

    /**
     * An iterator for the whole map.
     */
    private class WholeMapIterator implements Iterator<Tile> {
       
        /** The current coordinate position in the iteration. */
        private int x, y;


        /**
         * Default constructor.
         */
        public WholeMapIterator() {
            x = y = 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            return y < getHeight();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Tile next() throws NoSuchElementException {
            if (!hasNext()) {
                throw new NoSuchElementException("WholeMapIterator exhausted");
            }
            Tile result = getTile(x, y);
            x++;
            if (x >= getWidth()) {
                x = 0;
                y++;
            }
            return result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Gets an <code>Iterator</code> of every <code>Tile</code> on the map.
     *
     * @return An <code>Iterator</code> for the whole map.
     */
    public Iterator<Tile> getWholeMapIterator() {
        return new WholeMapIterator();
    }

    /**
     * Gets an iterable for all the tiles in the map using an
     * underlying WholeMapIterator.
     *
     * @return An <code>Iterable</code> for all tiles of the map.
     */
    public Iterable<Tile> getAllTiles() {
        return new Iterable<Tile>() {
            @Override
            public Iterator<Tile> iterator() {
                return new WholeMapIterator();
            }
        };
    }


    // Useful customers for tile iteration.

    /**
     * Searches for land within the given radius.
     *
     * @param x X-component of the position to search from.
     * @param y Y-component of the position to search from.
     * @param distance The radius in tiles that should be searched for land.
     * @return The first land tile found within the radius, or null if none
     *     found.
     */
    public Tile getLandWithinDistance(int x, int y, int distance) {
        for (Tile t : getCircleTiles(getTile(x, y), true, distance)) {
            if (t.isLand()) return t;
        }
        return null;
    }

    /**
     * Iterates through a rectangular subpart of the Map.
     * Intentionally avoids calling methods doing redundant checks,
     * which would slow down map display.
     * 
     * @param x X-component of the position of first tile.
     * @param y Y-component of the position of first tile.
     * @param w Width of the rectangle.
     * @param h Height of the rectangle.
     * @param consumer Provides a function to call for each tile.
     */
    public void forSubMap(int x, int y, int w, int h,
                          java.util.function.Consumer<Tile> consumer) {
        if (x < 0) {
            w += x;
            x = 0;
        }
        if (y < 0) {
            h += y;
            y = 0;
        }
        if (w <= 0 || h <= 0)
            return;
        int width = getWidth();
        int height = getHeight();
        if (x > width || y > height)
            return;
        if (x+w > width)
            w = width - x;
        if (y+h > height)
            h = height - y;
        for (int yi = y; yi < y+h; ++yi)
            for (int xi = x; xi < x+w; ++xi)
                consumer.accept(tiles[xi][yi]);
    }

    /**
     * Flood fills from a given <code>Position</code> p, based on
     * connectivity information encoded in boolmap
     *
     * @param boolmap The connectivity information for this floodfill.
     * @param x The starting x coordinate.
     * @param y The starting y coordinate.
     * @return A boolean[][] of the same size as boolmap, where "true"
     *      means the fill succeeded at that location.
     */
    public static boolean[][] floodFill(boolean[][] boolmap, int x, int y) {
        return floodFill(boolmap, x, y, Integer.MAX_VALUE);
    }

    /**
     * Flood fills from a given <code>Position</code> p, based on
     * connectivity information encoded in boolmap
     *
     * @param boolmap The connectivity information for this floodfill.
     * @param x The starting x coordinate.
     * @param y The starting y coordinate.
     * @param limit Limit to stop flood fill at.
     * @return A boolean[][] of the same size as boolmap, where "true"
     *      means the fill succeeded at that location.
     */
    public static boolean[][] floodFill(boolean[][] boolmap, int x, int y,
                                        int limit) {
        Position p = new Position(x, y);
        Queue<Position> q = new LinkedList<>();
        boolean[][] visited = new boolean[boolmap.length][boolmap[0].length];
        visited[p.getX()][p.getY()] = true;
        limit--;
        do {
            for (Direction direction : Direction.values()) {
                Position n = new Position(p, direction);
                if (n.isValid(boolmap.length, boolmap[0].length)
                    && boolmap[n.getX()][n.getY()]
                    && !visited[n.getX()][n.getY()] && limit > 0) {
                    visited[n.getX()][n.getY()] = true;
                    limit--;
                    q.add(n);
                }
            }

            p = q.poll();
        } while (p != null && limit > 0);
        return visited;
    }

    /**
     * Sets the contiguity identifier for all tiles.
     */
    public void resetContiguity() {
        // Create the water map.  It is an error for any tile not to
        // have a region at this point.
        boolean[][] waterMap = new boolean[getWidth()][getHeight()];
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                if (isValid(x, y)) {
                    waterMap[x][y] = !getTile(x,y).isLand();
                    Tile tile = getTile(x, y);
                    tile.setContiguity(-1);
                }
            }
        }

        // Flood fill each contiguous water region, setting the
        // contiguity number.
        int contig = 0;
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                if (waterMap[x][y]) {
                    Tile tile = getTile(x, y);
                    if (tile.getContiguity() >= 0) continue;
                    
                    boolean[][] found = floodFill(waterMap, x, y);
                    for (int yy = 0; yy < getHeight(); yy++) {
                        for (int xx = 0; xx < getWidth(); xx++) {
                            if (found[xx][yy]) {
                                Tile t = getTile(xx, yy);
                                if (t.getContiguity() < 0) {
                                    t.setContiguity(contig);
                                }
                            }
                        }
                    }
                    contig++;
                }
            }
        }

        // Complement the waterMap, it is now the land map.
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                if (isValid(x, y)) waterMap[x][y] = !waterMap[x][y];
            }
        }

        // Flood fill again for each contiguous land region.
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                if (waterMap[x][y]) {
                    Tile tile = getTile(x, y);
                    if (tile.getContiguity() >= 0) continue;
                    
                    boolean[][] found = floodFill(waterMap, x, y);
                    for (int yy = 0; yy < getHeight(); yy++) {
                        for (int xx = 0; xx < getWidth(); xx++) {
                            if (found[xx][yy]) {
                                Tile t = getTile(xx, yy);
                                if (t.getContiguity() < 0) {
                                    t.setContiguity(contig);
                                }
                            }
                        }
                    }
                    contig++;
                }
            }
        }
    }        

    /**
     * Places the "high seas"-tiles on the border of this map.
     *
     * All other tiles previously of type High Seas will be set to Ocean.
     *
     * @param distToLandFromHighSeas The distance between the land
     *     and the high seas (given in tiles).
     * @param maxDistanceToEdge The maximum distance a high sea tile
     *     can have from the edge of the map.
     */
    public void resetHighSeas(int distToLandFromHighSeas,
                              int maxDistanceToEdge) {
        final Specification spec = getSpecification();
        final TileType ocean = spec.getTileType("model.tile.ocean");
        final TileType highSeas = spec.getTileType("model.tile.highSeas");
        if (highSeas == null) {
            throw new RuntimeException("HighSeas TileType must exist");
        }
        if (ocean == null) {
            throw new RuntimeException("Ocean TileType must exist");
        }
        if (distToLandFromHighSeas < 0) {
            throw new RuntimeException("Land<->HighSeas distance can not be negative");
        }
        if (maxDistanceToEdge < 0) {
            throw new RuntimeException("Distance to edge can not be negative");
        }

        // Reset all highSeas tiles to the default ocean type.
        for (Tile t : getAllTiles()) {
            if (t.getType() == highSeas) t.setType(ocean);
        }

        final int width = getWidth(), height = getHeight();
        Tile t, seaL = null, seaR = null;
        int totalL = 0, totalR = 0, distanceL = -1, distanceR = -1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < maxDistanceToEdge && x < width
                     && isValid(x, y)
                     && (t = getTile(x, y)).getType() == ocean; x++) {
                Tile other = getLandWithinDistance(x, y,
                                                   distToLandFromHighSeas);
                if (other == null) {
                    t.setType(highSeas);
                    totalL++;
                } else {
                    int distance = t.getDistanceTo(other);
                    if (distanceL < distance) {
                        distanceL = distance;
                        seaL = t;
                    }
                }
            }
            for (int x = 0; x < maxDistanceToEdge && x < width
                     && isValid(width-1-x, y)
                     && (t = getTile(width-1-x, y)).getType() == ocean; x++) {
                Tile other = getLandWithinDistance(width-1-x, y,
                                                   distToLandFromHighSeas);
                if (other == null) {
                    t.setType(highSeas);
                    totalR++;
                } else {
                    int distance = t.getDistanceTo(other);
                    if (distanceR < distance) {
                        distanceR = distance;
                        seaR = t;
                    }
                }
            }
        }
        if (totalL <= 0 && seaL != null) {
            seaL.setType(highSeas);
            totalL++;
        }
        if (totalR <= 0 && seaR != null) {
            seaR.setType(highSeas);
            totalR++;
        }
        if (totalL <= 0 || totalR <= 0) {
            logger.warning("No high seas on "
                + ((totalL <= 0 && totalR <= 0) ? "either"
                    : (totalL <= 0) ? "left"
                    : (totalR <= 0) ? "right"
                    : "BOGUS") + " side of the map."
                + "  This can cause failures on small test maps.");
        }
    }

    /**
     * Sets the high seas count for all tiles connected to the high seas.
     * Any ocean tiles on the map vertical edges that do not have an
     * explicit false moveToEurope attribute are given a true one.
     *
     * Set all high seas counts negative, then start with a count of
     * zero for tiles with the moveToEurope attribute or of a type
     * with that ability.  Iterate outward by neighbouring tile,
     * incrementing the count on each pass, stopping at land.  Thus,
     * only the coastal land tiles will have a non-negative high seas
     * count.  This significantly speeds up the colony site evaluator,
     * as it does not have to try to find a path to Europe for each
     * tile.
     */
    public void resetHighSeasCount() {
        List<Tile> curr = new ArrayList<>();
        List<Tile> next = new ArrayList<>();
        int hsc = 0;
        for (Tile t : getAllTiles()) {
            t.setHighSeasCount(-1);
            if (!t.isLand()) {
                if ((t.getX() == 0 || t.getX() == getWidth()-1)
                    && t.getType() != null
                    && t.getType().isHighSeasConnected()
                    && !t.getType().isDirectlyHighSeasConnected()
                    && t.getMoveToEurope() == null) {
                    t.setMoveToEurope(Boolean.TRUE);
                }
                if (t.isDirectlyHighSeasConnected()) {
                    t.setHighSeasCount(hsc);
                    next.add(t);
                }
            }
        }
        while (!next.isEmpty()) {
            hsc++;
            curr.addAll(next);
            next.clear();
            while (!curr.isEmpty()) {
                Tile tile = curr.remove(0);
                // Deliberately using low level access to neighbours
                // rather than Tile.getSurroundingTiles() because that
                // relies on the map being attached to the game, which
                // is not necessarily true in the test suite.
                Position position = new Position(tile.getX(), tile.getY());
                for (Direction d : Direction.values()) {
                    Position p = new Position(position, d);
                    if (isValid(p)) {
                        Tile t = getTile(p);
                        if (t.getHighSeasCount() < 0) {
                            t.setHighSeasCount(hsc);
                            if (!t.isLand()) next.add(t);
                        }
                    }
                }
            }
        }
    }

    /**
     * Reset layer to reflect what is actually there.
     */
    public void resetLayers() {
        boolean regions = false,
            rivers = false,
            lostCityRumours = false,
            resources = false,
            nativeSettlements = false;
        for (Tile t : getAllTiles()) {
            regions |= t.getRegion() != null;
            rivers |= t.hasRiver();
            lostCityRumours |= t.hasLostCityRumour();
            resources |= t.hasResource();
            nativeSettlements |= t.getSettlement() instanceof IndianSettlement;
        }
        setLayer((rivers && lostCityRumours && resources && nativeSettlements)
            ? Layer.ALL
            : (nativeSettlements || lostCityRumours) ? Layer.NATIVES
            : (resources) ? Layer.RESOURCES
            : (rivers) ? Layer.RIVERS
            : (regions) ? Layer.REGIONS
            : Layer.TERRAIN);
    }

    /**
     * Fix the region parent/child relationships.
     */
    public void fixupRegions() {
        for (Region r : regions) {
            if (r.isPacific()) continue;
            Region p = r.getParent();
            // Mountains and Rivers were setting their parent to the
            // discoverable land region they are created within.  Move them
            // up to being children of the geographic region.
            if (r.getDiscoverable() && p != null && p.getDiscoverable()) {
                p = p.getParent();
                r.setParent(p);
            }
            if (p != null && !p.getChildren().contains(r)) p.addChild(r);
        }
    }


    // Interface Location
    // getId() inherited.

    /**
     * Gets the location tile.  Obviously not applicable to a Map.
     *
     * @return Null.
     */
    @Override
    public Tile getTile() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StringTemplate getLocationLabel() {
        return StringTemplate.key("newWorld");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StringTemplate getLocationLabelFor(Player player) {
        String name = player.getNewLandName();
        return (name == null) ? getLocationLabel() : StringTemplate.name(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean add(Locatable locatable) {
        // Used to add units to their entry location.  Dropped as this
        // is handled explicitly in the server.
        if (locatable instanceof Unit) {
            throw new RuntimeException("Disabled Map.add(Unit)");
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(Locatable locatable) {
        if (locatable instanceof Unit) {
            Tile tile = locatable.getTile();
            if (tile != null) return tile.remove(locatable);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(Locatable locatable) {
        return locatable instanceof Unit
            && locatable.getLocation() != null
            && locatable.getLocation().getTile() != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canAdd(Locatable locatable) {
        return locatable instanceof Unit;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getUnitCount() {
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Unit> getUnitList() {
        return Collections.<Unit>emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Unit> getUnitIterator() {
        return getUnitList().iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GoodsContainer getGoodsContainer() {
        return null; // Obviously irrelevant for a Map.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Settlement getSettlement() {
        return null; // Obviously irrelevant for a Map.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Colony getColony() {
        return null; // Obviously irrelevant for a Map.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IndianSettlement getIndianSettlement() {
        return null; // Obviously irrelevant for a Map.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location up() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRank() {
        return Location.LOCATION_RANK_NOWHERE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toShortString() {
        return "Map";
    }


    // Override FreeColGameObject

    /**
     * {@inheritDoc}
     */
    @Override
    public int checkIntegrity(boolean fix) {
        int result = super.checkIntegrity(fix);
        for (Tile t : getAllTiles()) {
            result = Math.min(result, t.checkIntegrity(fix));
        }
        return result;
    }


    // Serialization

    private static final String HEIGHT_TAG = "height";
    private static final String LAYER_TAG = "layer";
    private static final String MAXIMUM_LATITUDE_TAG = "maximumLatitude";
    private static final String MINIMUM_LATITUDE_TAG = "minimumLatitude";
    private static final String WIDTH_TAG = "width";
    // @compat 0.10.x, region remediation
    private final List<Tile> missingRegions = new ArrayList<>();
    // end @compat
    // @compat 0.10.5, nasty I/O hack
    private boolean fixupHighSeas = false;
    // end @compat


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(WIDTH_TAG, getWidth());

        xw.writeAttribute(HEIGHT_TAG, getHeight());

        xw.writeAttribute(LAYER_TAG, layer);

        xw.writeAttribute(MINIMUM_LATITUDE_TAG, minimumLatitude);

        xw.writeAttribute(MAXIMUM_LATITUDE_TAG, maximumLatitude);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        for (Region region : getSortedCopy(regions)) {
            region.toXML(xw);
        }

        for (Tile tile: getAllTiles()) {
            tile.toXML(xw);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        setLayer(xr.getAttribute(LAYER_TAG, Layer.class, Layer.ALL));

        if (tiles == null) {
            int width = xr.getAttribute(WIDTH_TAG, -1);
            if (width < 0) {
                throw new XMLStreamException("Bogus width: " + width);
            }
               
            int height = xr.getAttribute(HEIGHT_TAG, -1);
            if (height < 0) {
                throw new XMLStreamException("Bogus height: " + height);
            }

            tiles = new Tile[width][height];
        }

        minimumLatitude = xr.getAttribute(MINIMUM_LATITUDE_TAG, -90);

        maximumLatitude = xr.getAttribute(MAXIMUM_LATITUDE_TAG, 90);

        calculateLatitudePerRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // The tiles structure is large, and individually
        // overwriteable, so we do not clear it unlike most other containers.

        // @compat 0.10.5
        fixupHighSeas = false;
        // end @compat

        // @compat 0.10.x
        missingRegions.clear();
        // end @compat

        super.readChildren(xr);

        // @compat 0.10.x
        if (getGame().isInServer() && !missingRegions.isEmpty()) {
            TerrainGenerator.makeLakes(this, missingRegions);
        }
        // end @compat

        // @compat 0.10.5
        if (fixupHighSeas) resetHighSeasCount();
        // end @compat

        // Fix up settlement tile ownership in one hit here, avoiding
        // complications with cached tiles within the Tile serialization.
        for (Tile t : getAllTiles()) {
            Settlement s = t.getOwningSettlement();
            if (s != null) s.addTile(t);
        }

        // @compat 0.11.3
        // Maps with incorrect parent/child chains were occurring.
        fixupRegions();
        // end @compat 0.11.3
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final Game game = getGame();
        final String tag = xr.getLocalName();

        if (Region.getXMLElementTagName().equals(tag)) {
            addRegion(xr.readFreeColGameObject(game, Region.class));

        } else if (Tile.getXMLElementTagName().equals(tag)) {
            Tile t = xr.readFreeColGameObject(game, Tile.class);
            setTile(t, t.getX(), t.getY());

            // @compat 0.10.x
            if (t.getType() != null
                && "model.tile.lake".equals(t.getType().getId())
                && t.getRegion() == null) missingRegions.add(t);
            // end @compat

            // @compat 0.10.5
            if (t.getHighSeasCount() == Tile.FLAG_RECALCULATE) {
                fixupHighSeas = true;
            }
            // end @compat

        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "map".
     */
    public static String getXMLElementTagName() {
        return "map";
    }
}
