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
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.pathfinding.CostDecider;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.common.model.pathfinding.GoalDeciders;
import net.sf.freecol.common.util.Utils;


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

    /**
     * The directions a Unit can move to. Includes deltas for moving
     * to adjacent squares, which are required due to the isometric
     * map. Starting north and going clockwise.
    */
    public static enum Direction {
        N  ( 0, -2,  0, -2),
        NE ( 1, -1,  0, -1),
        E  ( 1,  0,  1,  0),
        SE ( 1,  1,  0,  1),
        S  ( 0,  2,  0,  2),
        SW ( 0,  1, -1,  1),
        W  (-1,  0, -1,  0),
        NW ( 0, -1, -1, -1);

        public final static int NUMBER_OF_DIRECTIONS = values().length;

        public final static Direction[] longSides = new Direction[] {
            Direction.NE, Direction.SE, Direction.SW, Direction.NW
        };

        public static final Direction[] corners = new Direction[] {
            Direction.N, Direction.E, Direction.S, Direction.W
        };

        private int oddDX, oddDY, evenDX, evenDY;


        Direction(int oddDX, int oddDY, int evenDX, int evenDY) {
            this.oddDX = oddDX;
            this.oddDY = oddDY;
            this.evenDX = evenDX;
            this.evenDY = evenDY;
        }

        public int getOddDX() {
            return oddDX;
        }

        public int getOddDY() {
            return oddDY;
        }

        public int getEvenDX() {
            return evenDX;
        }

        public int getEvenDY() {
            return evenDY;
        }

        /**
         * Gets this direction rotated by n places.
         *
         * @param n The number of places to rotate
         *     (-#directions <= n <= #directions).
         * @return The rotated direction.
         */
        private Direction rotate(int n) {
            return values()[(ordinal() + n + NUMBER_OF_DIRECTIONS)
                            % NUMBER_OF_DIRECTIONS];
        }

        /**
         * Get the next direction after this one (clockwise).
         *
         * @return The next <code>Direction</code>.
         */
        public Direction getNextDirection() {
            return rotate(1);
        }

        /**
         * Get the previous direction after this one (anticlockwise).
         *
         * @return The previous <code>Direction</code>.
         */
        public Direction getPreviousDirection() {
            return rotate(-1);
        }

        /**
         * Gets the reverse direction of this one.
         *
         * @return The reverse <code>Direction</code>.
         */
        public Direction getReverseDirection() {
            return rotate(NUMBER_OF_DIRECTIONS/2);
        }

        public String getNameKey() {
            return "direction." + this.toString();
        }

        /**
         * Gets a random Direction.
         *
         * @param logMe A string to log with the random results.
         * @param random A <code>Random</code> number source.
         * @return A random <code>Direction</code> value.
         */
        public static Direction getRandomDirection(String logMe,
                                                   Random random) {
            return values()[Utils.randomInt(logger, logMe, random,
                                            NUMBER_OF_DIRECTIONS)];
        }

        /**
         * Creates an array of the eight directions in a random order.
         *
         * @param logMe A string to log with the random results.
         * @param random A <code>Random</code> number source.
         * @return An array of the <code>Direction</code>s in a random order.
         */
        public static Direction[] getRandomDirections(String logMe,
                                                      Random random) {
            int[] randoms = Utils.randomInts(logger, logMe, random,
                NUMBER_OF_DIRECTIONS, NUMBER_OF_DIRECTIONS);
            Direction[] directions = Direction.values();
            for (int i = 0; i < directions.length; i++) {
                if (randoms[i] != i) {
                    Direction temp = directions[randoms[i]];
                    directions[randoms[i]] = directions[i];
                    directions[i] = temp;
                }
            }
            return directions;
        }

        /**
         * Creates an array of the directions in an order that favours
         * a supplied one.  Entry 0 will be the supplied direction,
         * entry 1+2 will be those immediately to the left and right
         * of it (chosen randomly), and so on until the last entry
         * will be the complete reverse of the supplied direction.
         * 
         * Useful if we want to travel in a particular direction, but
         * if this fails to be able to try the closest other
         * directions to the original one in order.
         *
         * @param logMe A string to log with the random results.
         * @param random A <code>Random</code> number source.
         * @return An array of the <code>Direction</code>s favouring this one.
         */
        public Direction[] getClosestDirections(String logMe, Random random) {
            // Will need 3 bits of randomness --- 2 directions are known,
            // need one bit to randomize each remaining pair.
            final int nbits = (NUMBER_OF_DIRECTIONS - 2) / 2;
            final int r = Utils.randomInt(logger, logMe, random, 1 << nbits);

            Direction[] ret = new Direction[NUMBER_OF_DIRECTIONS];
            ret[0] = this;

            int step = 1, mask = 1;
            for (int i = 1; i < NUMBER_OF_DIRECTIONS - 1; i += 2) {
                Direction dr = this.rotate(step);
                Direction dl = this.rotate(NUMBER_OF_DIRECTIONS - step);
                ret[i] = ((r & mask) == 0) ? dr : dl;
                ret[i+1] = ((r & mask) == 0) ? dl : dr;
                step += 1;
                mask *= 2;
            }

            ret[NUMBER_OF_DIRECTIONS-1] = this.getReverseDirection();
            return ret;
        }
    }

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
         * Compares the other Position based on the coordinates.
         *
         * @param other The other object to compare with.
         * @return True iff the coordinates match.
         */
        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            } else if (other == null) {
                return false;
            } else if (!(other instanceof Position)) {
                return false;
            } else {
                return x == ((Position)other).x && y == ((Position)other).y;
            }
        }

        /**
         * Gets a hash code value.  The current implementation
         * (which may change at any time) works well as long as the
         * maximum coordinates fit in 16 bits.
         *
         * @return A hash code value for this object.
         */
        @Override
        public int hashCode() {
            return x | (y << 16);
        }

        /**
         * Gets the position adjacent to a given position, in a given
         * direction.
         *
         * @param direction The <code>Direction</code> to check.
         * @return The adjacent position.
         */
        public Position getAdjacent(Direction direction) {
            int x = this.x + (((this.y & 1) != 0) ? direction.getOddDX()
                : direction.getEvenDX());
            int y = this.y + (((this.y & 1) != 0) ? direction.getOddDY()
                : direction.getEvenDY());
            return new Position(x, y);
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
            int ay = getY();
            int by = position.getY();
            int r = position.getX() - getX() - (ay - by) / 2;

            if (by > ay && ay % 2 == 0 && by % 2 != 0) {
                r++;
            } else if (by < ay && ay % 2 != 0 && by % 2 == 0) {
                r--;
            }
            return Math.max(Math.abs(ay - by + r), Math.abs(r));
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

    /** The regions, indexed by object identifier. */
    private final java.util.Map<String, Region> regions
        = new HashMap<String, Region>();

    /** The search tracing status.  Do not serialize. */
    private boolean traceSearch = false;


    /**
     * Create a new <code>Map</code> from a collection of tiles.
     *
     * @param game The enclosing <code>Game</code>.
     * @param tiles The 2D array of tiles to contain.
     */
    public Map(Game game, Tile[][] tiles) {
        super(game);

        this.tiles = tiles;
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
        return (tiles == null) ? 0 : tiles.length;
    }

    /**
     * Gets the height of this map.
     *
     * @return The height of this map.
     */
    public int getHeight() {
        return (tiles == null) ? 0 : tiles[0].length;
    }

    /**
     * Gets the <code>Layer</code> value.
     *
     * @return The <code>Layer</code> value
     */
    public final Layer getLayer() {
        return layer;
    }

    /**
     * Sets the <code>Layer</code> value.
     *
     * @param newLayer The new Layer value.
     */
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

    /**
     * Sets the <code>MinimumLatitude</code> value.
     *
     * @param newMinimumLatitude The new minimum latitude value.
     */
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

    /**
     * Sets the <code>MaximumLatitude</code> value.
     *
     * @param newMaximumLatitude The new MaximumLatitude value.
     */
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

    /**
     * Calculates the <code>LatitudePerRow</code> value.
     */
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
        return regions.values();
    }

    /**
     * Gets a <code>Region</code> by name key.
     *
     * @param key The name key to lookup the region with.
     * @return The region with the given name key, or null if not found.
     */
    public Region getRegion(final String key) {
        return regions.get(key);
    }

    /**
     * Gets a <code>Region</code> by name.
     *
     * @param name The region name.
     * @return The <code>Region</code> with the given name, or null if
     *     not found.
     */
    public Region getRegionByName(final String name) {
        if (name == null) return null;
        for (Region region : regions.values()) {
            if (name.equals(region.getName())) {
                return region;
            }
        }
        return null;
    }

    /**
     * Puts a region into this map.
     *
     * @param region The <code>Region</code> to put.
     */
    public void putRegion(final Region region) {
        regions.put(region.getNameKey(), region);
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
            : l1.getTile().getContiguity() == l2.getTile().getContiguity();
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
        for (Direction d : Direction.values()) {
            if (t1.getNeighbourOrNull(d) == t2) return d;
        }
        return null;
    }

    /**
     * Gets the adjacent Tile in a given direction.
     *
     * @param x The x coordinate to work from.
     * @param y The y coordinate to work from.
     * @param direction The <code>Direction</code> to check.
     * @return The adjacent tile in the specified direction, or null
     *     if invalid.
     */
    public Tile getAdjacentTile(int x, int y, Direction direction) {
        x += ((y & 1) != 0) ? direction.getOddDX() : direction.getEvenDX();
        y += ((y & 1) != 0) ? direction.getOddDY() : direction.getEvenDY();
        return getTile(x, y);
    }


    // Path-finding/searching infrastructure and routines

    /**
     * Simple interface to supply a heuristic to the A* routine.
     */
    private interface SearchHeuristic {
        public int getValue(Tile tile);
    }

    /**
     * Gets a search heuristic using the Manhatten distance to an end tile.
     *
     * @param endTile The <code>Tile</code> to aim for.
     * @return A new <code>SearchHeuristic</code> aiming for the end tile.
     */
    private SearchHeuristic getManhattenHeuristic(final Tile endTile) {
        return new SearchHeuristic() {
            // Manhatten distance to the end tile.
            public int getValue(Tile tile) {
                return tile.getDistanceTo(endTile);
            }
        };
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
                    + unit + "/" + ((Unit)start));
            } else if (unitLoc instanceof HighSeas) {
                if (carrier == null) {
                    throw new IllegalArgumentException("Null carrier when starting on high seas: " + unit);
                } else if (carrier != (Unit)start) {
                    throw new IllegalArgumentException("Wrong carrier when starting on high seas: " + unit + "/" + carrier + " != " + ((Unit)start));
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
                throw new IllegalArgumentException("No carrier when starting on high seas: " + unit + "/" + unit.getLocation());
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
                         costDecider, INFINITY, carrier, null);
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
     * @return A path starting at the start tile and ending at the end
     *     tile, or null if none found.
     */
    private PathNode findMapPath(Unit unit, Tile start, Tile end, Unit carrier,
                                 CostDecider costDecider) {
        final Unit offMapUnit = (carrier != null) ? carrier : unit;
        final GoalDecider gd = GoalDeciders.getLocationGoalDecider(end);
        final SearchHeuristic sh = getManhattenHeuristic(end);

        PathNode path;
        if (start.getContiguity() == end.getContiguity()) {
            // If the unit potentially could get to the destination
            // without a carrier, compare both with-carrier and
            // without-carrier paths.  The latter will usually be
            // faster, but not always, e.g. mounted units on a good
            // road system.
            path = searchMap(unit, start, gd, costDecider,
                             INFINITY, null, sh);
            PathNode carrierPath = (carrier == null) ? null
                : searchMap(unit, start, gd, costDecider,
                            INFINITY, carrier, sh);
            if (carrierPath != null
                && (path == null
                    || (path.getLastNode().getCost()
                        > carrierPath.getLastNode().getCost()))) {
                path = carrierPath;
            }
        } else if (offMapUnit != null) {
            // If there is a water unit then complex paths which use
            // settlements and inland lakes are possible, but hard to
            // capture with the contiguity test, so just allow the
            // search to proceed.
            path = searchMap(unit, start, gd, costDecider,
                             INFINITY, carrier, sh);
        } else { // Otherwise, there is a connectivity failure.
            path = null;
        }
        return path;
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
     * @return A path starting at the start location and ending at the
     *     end location, or null if none found.
     * @throws IllegalArgumentException For many reasons, see
     *     {@link #findRealStart}.
     */
    public PathNode findPath(final Unit unit,
                             final Location start, final Location end,
                             final Unit carrier, CostDecider costDecider) {
        // Validate the arguments, reducing to either Europe or a Tile.
        final Location realStart = findRealStart(unit, start, carrier);
        final Location realEnd = findRealEnd(end);
        // Get the unit that will be used for off-map travel.
        final Unit offMapUnit = (carrier != null) ? carrier : unit;

        PathNode path = null;
        // There are four cases...
        if (realStart instanceof Europe && realEnd instanceof Europe) {
            // 0: Europe->Europe
            // Create a trivial path.
            path = new PathNode(realStart, unit.getMovesLeft(), 0,
                                false, null, null);

        } else if (realStart instanceof Europe && realEnd instanceof Tile) {
            // 1: Europe->Tile
            // Fail fast without an off map unit.
            if (offMapUnit == null
                || !offMapUnit.getType().canMoveToHighSeas()) return null;

            // Find the best place to enter the map from Europe.
            PathNode p = getBestEntryPath(unit, (Tile)realEnd, carrier,
                                          costDecider);
            if (p == null) return null;
            Tile tile = p.getLastNode().getTile();

            // Now search forward from there to get a path in the
            // right order (path costs are not symmetric).
            path = findMapPath(unit, tile, (Tile)realEnd, carrier, costDecider);
            if (path == null) {
                boolean old = traceSearch;
                setSearchTrace(true);
                path = findMapPath(unit, tile, (Tile)realEnd, carrier, costDecider);
                setSearchTrace(old);
                throw new IllegalStateException("FINDPATH-FAIL: " + unit
                    + "/" + carrier + " from " + tile + " to " + end
                    + "\n" + p.fullPathToString());
            }

            // At the front of the path insert a node for the starting
            // location in Europe, correcting for the turns to sail to
            // the entry location.
            path.addTurns(offMapUnit.getSailTurns());
            path.previous = new PathNode(realStart, unit.getMovesLeft(),
                                         0, carrier != null, null, path);
            path = path.previous;
            if (carrier != null && unit.getLocation() != carrier) {
                path.previous = new PathNode(realStart, unit.getMovesLeft(),
                                             0, false, null, path);
                path = path.previous;
            }

        } else if (realStart instanceof Tile && realEnd instanceof Europe) {
            // 2: Tile->Europe
            // Fail fast if Europe is unattainable.
            if (offMapUnit == null
                || !offMapUnit.getType().canMoveToHighSeas()) return null;
                
            // Search forwards to the high seas.
            path = searchMap(unit, (Tile)realStart,
                             GoalDeciders.getHighSeasGoalDecider(),
                             costDecider, INFINITY, carrier, null);
            if (path != null) {
                PathNode last = path.getLastNode();
                last.next = new PathNode(realEnd, unit.getInitialMovesLeft(),
                    last.getTurns() + offMapUnit.getSailTurns(),
                    last.isOnCarrier(), last, null);
            }

        } else if (realStart instanceof Tile && realEnd instanceof Tile) {
            // 3: Tile->Tile
            path = findMapPath(unit, (Tile)realStart, (Tile)realEnd, carrier,
                               costDecider);

        } else {
            throw new IllegalStateException("Can not happen: " + realStart
                                            + ", " + realEnd);
        }

        if (path != null) { // Add the turns remaining on the high seas.
            final int initialTurns = (!unit.isAtSea()) ? 0
                : ((unit.isOnCarrier()) ? unit.getCarrier() : unit).getWorkLeft();
            if (initialTurns != 0) path.addTurns(initialTurns);
        }
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
     * @return The path to a goal, or null if none can be found.
     * @throws IllegalArgumentException If the unit is null, or the
     *     start location does not make sense, or the carrier/unit
     *     combination is bogus.
     */
    public PathNode search(final Unit unit, Location start,
                           final GoalDecider goalDecider,
                           final CostDecider costDecider,
                           final int maxTurns, final Unit carrier) {
        final Location realStart = findRealStart(unit, start, carrier);
        
        PathNode path;
        if (realStart instanceof Europe) {
            // Fail fast if Europe is unattainable.
            Unit offMapUnit = (carrier != null) ? carrier : unit;
            if (offMapUnit == null
                || !offMapUnit.getType().canMoveToHighSeas()) return null;

            // This is suboptimal.  We do not know where to enter from
            // Europe, so start with the standard entry location, then
            // if we find a path, try to optimize it.  This will lose
            // if the initial search fails due to a turn limit.
            // TODO: something better.
            path = searchMap(unit, (Tile)offMapUnit.getEntryLocation(),
                             goalDecider, costDecider, maxTurns, carrier, null);
            if (path != null) {
                path = findPath(unit, realStart, path.getLastNode().getTile(),
                                carrier, costDecider);
            }
        } else {
            path = searchMap(unit, realStart.getTile(), goalDecider,
                             costDecider, maxTurns, carrier, null);
        }

        if (path != null) { // Add the turns remaining on the high seas.
            final int initialTurns = (!unit.isAtSea()) ? 0
                : ((unit.isOnCarrier()) ? unit.getCarrier() : unit).getWorkLeft();
            if (initialTurns != 0) path.addTurns(initialTurns);
        }
        return path;
    }

    /**
     * Sets the search tracing status.
     *
     * @param trace The new search tracing status.
     */
    public void setSearchTrace(boolean trace) {
        traceSearch = trace;
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
        private PathNode current;
        private Location dst;
        private int movesLeft;
        private int turns;
        private boolean onCarrier;
        private CostDecider decider;
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
                this.cost = PathNode.getCost(turns, movesLeft);
            }
            this.path = null;
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
         */
        public void resetPath() {
            this.path = new PathNode(dst, movesLeft, turns, onCarrier,
                                     current, null);
        }

        /**
         * Do not let the CostDecider (which may be conservative)
         * block a final destination.  This allows planning routines
         * to compute paths to tiles temporarily occupied by an enemy
         * unit, or for an empty ship to find a compound path to a
         * native settlement where the first step is to collect the
         * cargo it needs to make the final move legal.
         */
        public void recoverGoal() {
            Unit.MoveType mt;
            if (cost == CostDecider.ILLEGAL_MOVE
                && unit != null
                && current.getTile() != null
                && dst.getTile() != null) {
                // Pretend it finishes the move.
                movesLeft = unit.getInitialMovesLeft();
                turns++;
                cost = PathNode.getCost(turns, movesLeft);
                resetPath();
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
         * @param openList The list of available nodes.
         * @param openListQueue The queue of available nodes.
         * @param f The heuristic values for A*.
         * @param sh An optional <code>SearchHeuristic</code> to apply.
         */
        public void improve(HashMap<String, PathNode> openList,
                            PriorityQueue<PathNode> openListQueue,
                            HashMap<String, Integer> f,
                            SearchHeuristic sh) {
            PathNode best = openList.get(dst.getId());
            if (best != null) {
                openList.remove(dst.getId());
                openListQueue.remove(best);
            }
            int fcost = cost;
            if (sh != null && dst.getTile() != null) {
                fcost += sh.getValue(dst.getTile());
            }
            f.put(dst.getId(), new Integer(fcost));
            openList.put(dst.getId(), path);
            openListQueue.offer(path);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("[candidate unit=").append(unit)
                .append(" dst=").append((FreeColGameObject)dst)
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
     * Does this path include a non-carrier move within the last turn?
     *
     * @param p The <code>PathNode</code> to check.
     * @param turns Paths with fewer turns than this are previous turns.
     * @return True if there was a non-carrier move in the last turn.
     */
    private static boolean embarkedThisTurn(PathNode p, int turns) {
        for (; p != null; p = p.previous) {
            if (p.getTurns() < turns) return false;
            if (!p.isOnCarrier()) return true;
        }
        return false;
    }

    /**
     * Searches for a path to a goal determined by the given
     * <code>GoalDecider</code>.
     *
     * Using A* with a List (closedList) for marking the visited nodes
     * and using a PriorityQueue (openListQueue) for getting the next
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
     * @return A path to a goal determined by the given
     *     <code>GoalDecider</code>.
     */
    private PathNode searchMap(final Unit unit, final Tile start,
                               final GoalDecider goalDecider,
                               final CostDecider costDecider,
                               final int maxTurns, final Unit carrier,
                               final SearchHeuristic searchHeuristic) {
        final HashMap<String, PathNode> openList
            = new HashMap<String, PathNode>();
        final HashMap<String, PathNode> closedList
            = new HashMap<String, PathNode>();
        final HashMap<String, Integer> f
            = new HashMap<String, Integer>();
        final PriorityQueue<PathNode> openListQueue
            = new PriorityQueue<PathNode>(1024,
                new Comparator<PathNode>() {
                    public int compare(PathNode p1, PathNode p2) {
                        return (f.get(p1.getLocation().getId()).intValue()
                            - f.get(p2.getLocation().getId()).intValue());
                    }
                });
        final Europe europe = unit.getOwner().getEurope();
        final List<PathNode> tracing = (traceSearch)
            ? new ArrayList<PathNode>()
            : null;
        Unit offMapUnit = (carrier != null) ? carrier : unit;
        Unit currentUnit = (start.isLand())
            ? ((start.getSettlement() != null
                    && start.getSettlement().isConnectedPort()
                    && unit != null
                    && unit.getLocation() == carrier) ? carrier : unit)
            : offMapUnit;

        // Create the start node and put it on the open list.
        final PathNode firstNode = new PathNode(start,
            ((currentUnit != null) ? currentUnit.getMovesLeft() : -1),
            0, carrier != null && currentUnit == carrier, null, null);
        f.put(start.getId(),
            new Integer((searchHeuristic == null) ? 0
                : searchHeuristic.getValue(start)));
        openList.put(start.getId(), firstNode);
        openListQueue.offer(firstNode);

        while (!openList.isEmpty()) {
            // Choose the node with the lowest f.
            final PathNode currentNode = openListQueue.poll();
            final Location currentLocation = currentNode.getLocation();
            if (tracing != null) tracing.add(currentNode);
            openList.remove(currentLocation.getId());
            closedList.put(currentLocation.getId(), currentNode);

            // Reset current unit to that of this node.
            currentUnit = (currentNode.isOnCarrier()) ? carrier : unit;

            // Stop at simple success.
            if (goalDecider.check(currentUnit, currentNode)
                && !goalDecider.hasSubGoals()) {
                break;
            }

            // Ignore nodes over the turn limit.
            if (currentNode.getTurns() > maxTurns) continue;

            // Collect the parameters for the current node.
            final int currentMovesLeft = currentNode.getMovesLeft();
            final int currentTurns = currentNode.getTurns();
            final boolean currentOnCarrier = currentNode.isOnCarrier();

            final Tile currentTile = currentNode.getTile();
            if (currentTile == null) { // Must be in Europe.
                // TODO: Do not consider tiles `adjacent' to Europe, yet.
                // There may indeed be cases where going to Europe and
                // coming back on the other side of the map is faster.
                continue;
            }

            // Try the tiles in each direction
            for (Tile moveTile : currentTile.getSurroundingTiles(1)) {
                // If the new tile is the tile we just visited, skip it.
                if (currentNode.previous != null
                    && currentNode.previous.getTile() == moveTile) {
                    continue;
                }

                // Skip tiles already visited.
                if (closedList.containsKey(moveTile.getId())) continue;

                // Is this move possible?  Loop on failure.
                //
                // Allow some seemingly impossible moves if it is to the
                // final destination (see the comment to recoverMove).
                // 
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
                // The exception is the disembarkToGoal tests, which
                // handle the case where the carrier ends its turn one
                // short of a goal settlement, where we should
                // consider an immediate move into the settlement by
                // the passenger.
                //
                boolean isGoal = goalDecider.check(unit,
                    new PathNode(moveTile, currentMovesLeft, currentTurns,
                                 false, currentNode, null));
                Unit.MoveType umt = unit.getSimpleMoveType(currentTile,
                                                           moveTile);
                boolean unitMove = (isGoal) ? umt.isLegal() : umt.isProgress();
                boolean carrierMove = carrier != null
                    && carrier.isTileAccessible(moveTile);
                boolean embarked = embarkedThisTurn(currentNode, currentTurns);

                boolean disembarkToGoal = false;
                if (unitMove && carrierMove && currentOnCarrier
                    && !embarked && isGoal) {
                    // The unit has moves left, and is on a carrier
                    // next to the goal, which both can move to.
                    // Usually we will prefer to let the carrier
                    // finish the job, unless it can not do it this turn.
                    CostDecider cd = (costDecider != null) ? costDecider
                        : CostDeciders.defaultCostDeciderFor(carrier);
                    int cost = cd.getCost(carrier, currentNode.getLocation(),
                                          moveTile, currentMovesLeft);
                    if (cost == CostDecider.ILLEGAL_MOVE) {
                        carrierMove = false;
                    } else {
                        disembarkToGoal = cd.getNewTurns() > 0;
                    }
                }
                MoveStep step = (currentOnCarrier)
                    ? ((carrierMove && !disembarkToGoal) ? MoveStep.BYWATER
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
                    // If already embarked this turn the disembarking
                    // unit will have to wait another turn.
                    move = new MoveCandidate(unit, currentNode, moveTile,
                        0, ((embarked) ? currentTurns+1 : currentTurns), false,
                        ((costDecider != null) ? costDecider
                            : CostDeciders.defaultCostDeciderFor(unit)));
                    break;
                case FAIL: default: // Loop on failure.
                    move = null;
                    break;
                }

                if (move != null) {
                    move.resetPath();

                    // Special case when on the map.
                    if (isGoal) move.recoverGoal();

                    // Is this an improvement?  If not, ignore.
                    if (move.canImprove(openList.get(moveTile.getId()))) {
                        move.improve(openList, openListQueue, f,
                                     searchHeuristic);
                    }
                }
            }

            // Also try moving to Europe if it exists and the move is ok.
            if (europe != null
                && (currentNode.previous != null
                    && currentNode.previous.getLocation() != europe)
                && !closedList.containsKey(europe.getId())
                && currentUnit != null
                && currentUnit.getType().canMoveToHighSeas()
                && currentTile.isDirectlyHighSeasConnected()) {
                MoveCandidate move = new MoveCandidate(currentUnit,
                    currentNode, europe, currentMovesLeft, currentTurns,
                    currentOnCarrier,
                    ((costDecider != null) ? costDecider
                        : CostDeciders.defaultCostDeciderFor(currentUnit)));
                move.resetPath();
                if (move.canImprove(openList.get(europe.getId()))) {
                    move.improve(openList, openListQueue, f, null);
                }
            }
        }

        // Relink the path.  We omitted the .next link while constructing it.
        PathNode best = goalDecider.getGoal();
        if (best != null) {
            while (best.previous != null) {
                best.previous.next = best;
                best = best.previous;
            }
        }

        // Output the trace result.
        if (tracing != null) {
            String logMe = "Search trace(" + unit + ", " + start
                + ", " + ((carrier == null) ? "null" : carrier) + "):";
            for (PathNode p : tracing) logMe += "\n   " + p;
            if (best != null) logMe += "\n" + best.fullPathToString();
            logger.info(logMe);
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
     * Base class for internal iterators.
     */
    private abstract class MapIterator implements Iterator<Position> {

        /**
         * Gets the next position as a position rather as an object.
         *
         * @return The next <code>Position</code>.
         * @throws NoSuchElementException if the iterator is exhausted.
         */
        public abstract Position nextPosition() throws NoSuchElementException;

        /**
         * Gets the next position in the iteration.
         *
         * @return The next <code>Position</code> in the iteration.
         * @exception NoSuchElementException if the iterator is exhausted.
         */
        public Position next() {
            return nextPosition();
        }

        /**
         * Removes from the underlying collection the last element returned by
         * the iterator (optional operation).
         *
         * @exception UnsupportedOperationException no matter what.
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Makes an iterable version of a map iterator.
     *
     * @param m The <code>MapIterator</code> to make iterable.
     * @return A corresponding iterable.
     */
    private Iterable<Tile> makeMapIteratorIterable(final MapIterator m) {
        return new Iterable<Tile>() {
            public Iterator<Tile> iterator() {
                return new Iterator<Tile>() {
                    public boolean hasNext() { return m.hasNext(); }
                    public Tile next() { return getTile(m.next()); }
                    public void remove() { m.remove(); }
                };
            }
        };
    }

    /**
     * An iterator returning positions in a spiral starting at a given
     * center tile.  The center tile is never included in the
     * positions returned, and all returned positions are valid.
     */
    private final class CircleIterator extends MapIterator {

        /** The maximum radius. */
        private int radius;
        /** The current radius of the iteration. */
        private int currentRadius;
        /** The current index in the circle with the current radius: */
        private int n;
        /** The current position in the circle. */
        private Position nextPosition = null;


        /**
         * Create a new Circle Iterator.
         *
         * @param center The center <code>Position</code> of the circle.
         * @param isFilled True to get all of the positions within the circle.
         * @param radius The radius of the circle.
         */
        public CircleIterator(Position center, boolean isFilled, int radius) {
            if (center == null) {
                throw new IllegalArgumentException("center must not be null.");
            }
            this.radius = radius;
            n = 0;

            if (isFilled || radius == 1) {
                nextPosition = center.getAdjacent(Direction.NE);
                currentRadius = 1;
            } else {
                this.currentRadius = radius;
                nextPosition = center;
                for (int i = 1; i < radius; i++) {
                    nextPosition = nextPosition.getAdjacent(Direction.N);
                }
                nextPosition = nextPosition.getAdjacent(Direction.NE);
            }
            if (!isValid(nextPosition)) {
                determineNextPosition();
            }
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
        private void determineNextPosition() {
            boolean positionReturned = n != 0;
            do {
                n++;
                final int width = currentRadius * 2;
                if (n >= width * 4) {
                    currentRadius++;
                    if (currentRadius > radius) {
                        nextPosition = null;
                    } else if (!positionReturned) {
                        nextPosition = null;
                    } else {
                        n = 0;
                        positionReturned = false;
                        nextPosition = nextPosition.getAdjacent(Direction.NE);
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
                    nextPosition = nextPosition.getAdjacent(direction);
                }
            } while (nextPosition != null && !isValid(nextPosition));
        }

        /**
         * Check if the iterator has another position in it.
         *
         * @return True if there is another position.
         */
        public boolean hasNext() {
            return nextPosition != null;
        }

        /**
         * Gets the next position.
         *
         * @return The next valid position.
         */
        @Override
        public Position nextPosition() {
            if (nextPosition == null) return null;
            final Position p = nextPosition;
            determineNextPosition();
            return p;
        }
    }

    /**
     * Gets a circle iterator.
     *
     * @param center The center <code>Position</code> to iterate around.
     * @param isFilled True to get all of the positions in the circle.
     * @param radius The radius of circle.
     * @return The circle iterator.
     */
    public CircleIterator getCircleIterator(Position center, boolean isFilled,
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
    public Iterable<Tile> getCircleTiles(Tile center, boolean isFilled,
                                         int radius) {
        return makeMapIteratorIterable(getCircleIterator(center.getPosition(),
                                                         isFilled, radius));
    }

    /**
     * An iterator for the whole map.
     */
    private final class WholeMapIterator extends MapIterator {
       
        /**
         * The current coordinate position in the iteration.
         */
        private int x, y;


        /**
         * Default constructor.
         */
        public WholeMapIterator() {
            x = 0;
            y = 0;
        }

        /**
         * Checks if the iterator has another position in it.
         *
         * @return True of there is another position
         */
        public boolean hasNext() {
            return y < getHeight();
        }

        /**
         * Gets the next position in the iteration.
         *
         * @return The next <code>Position</code>.
         * @throws NoSuchElementException if the iterator is exhausted.
         */
        @Override
        public Position nextPosition() throws NoSuchElementException {
            if (!hasNext()) {
                throw new NoSuchElementException("WholeMapIterator exhausted");
            }
            Position newPosition = new Position(x, y);
            x++;
            if (x == getWidth()) {
                x = 0;
                y++;
            }
            return newPosition;
        }
    }

    /**
     * Gets an <code>Iterator</code> of every <code>Tile</code> on the map.
     *
     * @return The <code>Iterator</code>.
     */
    public MapIterator getWholeMapIterator() {
        return new WholeMapIterator();
    }

    /**
     * Gets an iterable for all the tiles in the map on using an
     * underlying WholeMapIterator.
     *
     * @return An <code>Iterable</code> for all tiles of the map.
     */
    public Iterable<Tile> getAllTiles() {
        return makeMapIteratorIterable(getWholeMapIterator());
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
     * Flood fills from a given <code>Position</code> p, based on
     * connectivity information encoded in boolmap
     *
     * @param boolmap The connectivity information for this floodfill.
     * @param p The starting <code>Position</code>.
     * @return A boolean[][] of the same size as boolmap, where "true"
     *      means the fill succeeded at that location.
     */
    public static boolean[][] floodFill(boolean[][] boolmap, Position p) {
        return floodFill(boolmap, p, Integer.MAX_VALUE);
    }

    /**
     * Flood fills from a given <code>Position</code> p, based on
     * connectivity information encoded in boolmap
     *
     * @param boolmap The connectivity information for this floodfill.
     * @param p The starting <code>Position</code>.
     * @param limit Limit to stop flood fill at.
     * @return A boolean[][] of the same size as boolmap, where "true"
     *      means the fill succeeded at that location.
     */
    public static boolean[][] floodFill(boolean[][] boolmap, Position p,
                                        int limit) {
        Queue<Position> q = new LinkedList<Position>();
        boolean[][] visited = new boolean[boolmap.length][boolmap[0].length];
        visited[p.getX()][p.getY()] = true;
        limit--;
        do {
            for (Direction direction : Direction.values()) {
                Position n = p.getAdjacent(direction);
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
                }
            }
        }

        // Flood fill each contiguous water region, setting the
        // contiguity number.
        int contig = 0;
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                Tile tile = getTile(x, y);
                if (waterMap[x][y]) {
                    if (tile.getContiguity() >= 0) continue;
                    
                    boolean[][] found = floodFill(waterMap,
                                                  new Position(x, y));
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
                    
                    boolean[][] found = floodFill(waterMap,
                                                  new Position(x, y));
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
        List<Tile> curr = new ArrayList<Tile>();
        List<Tile> next = new ArrayList<Tile>();
        int hsc = 0;
        for (Tile t : getAllTiles()) {
            t.setHighSeasCount(-1);
            if (!t.isLand()) {
                if ((t.getX() == 0 || t.getX() == getWidth()-1)
                    && t.getType().isHighSeasConnected()
                    && !t.getType().isDirectlyHighSeasConnected()
                    && t.getMoveToEurope() == null) {
                    t.setMoveToEurope(true);
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
                    Position p = position.getAdjacent(d);
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


    // Location interface.
    // getId() inherited.

    /**
     * Gets the location tile.  Obviously not applicable to a Map.
     *
     * @return Null.
     */
    public Tile getTile() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public StringTemplate getLocationName() {
        return StringTemplate.key("NewWorld");
    }

    /**
     * {@inheritDoc}
     */
    public StringTemplate getLocationNameFor(Player player) {
        String name = player.getNewLandName();
        return (name == null) ? getLocationName() : StringTemplate.name(name);
    }

    /**
     * {@inheritDoc}
     */
    public boolean add(Locatable locatable) {
        if (locatable instanceof Unit) {
            Unit unit = (Unit)locatable;
            unit.setLocation(unit.getEntryLocation());
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(Locatable locatable) {
        if (locatable instanceof Unit) {
            Tile tile = ((Unit)locatable).getTile();
            if (tile != null) return tile.remove(locatable);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(Locatable locatable) {
        return locatable instanceof Unit
            && locatable.getLocation() != null
            && locatable.getLocation().getTile() != null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean canAdd(Locatable locatable) {
        return locatable instanceof Unit;
    }

    /**
     * {@inheritDoc}
     */
    public int getUnitCount() {
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    public List<Unit> getUnitList() {
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Unit> getUnitIterator() {
        return getUnitList().iterator();
    }

    /**
     * {@inheritDoc}
     */
    public GoodsContainer getGoodsContainer() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Settlement getSettlement() {
        // Obviously irrelevant for a Map.
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Colony getColony() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public IndianSettlement getIndianSettlement() {
        return null;
    }


    // Serialization

    private static final String HEIGHT_TAG = "height";
    private static final String LAYER_TAG = "layer";
    private static final String MAXIMUM_LATITUDE_TAG = "maximumLatitude";
    private static final String MINIMUM_LATITUDE_TAG = "minimumLatitude";
    private static final String WIDTH_TAG = "width";
    // @compat 0.10.5, nasty I/O hack
    private boolean fixupHighSeas = false;
    // end @compat


    /**
     * {@inheritDoc}
     */
    protected void writeAttributes(FreeColXMLWriter xw, Player player,
                                   boolean showAll,
                                   boolean toSavedGame) throws XMLStreamException {
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
    protected void writeChildren(FreeColXMLWriter xw, Player player,
                                 boolean showAll,
                                 boolean toSavedGame) throws XMLStreamException {
        super.writeChildren(xw);

        for (Region region : getSortedCopy(regions.values())) region.toXML(xw);

        for (Tile tile: getAllTiles()) {
            if (showAll || toSavedGame
                || (player != null && player.hasExplored(tile))) {
                tile.toXML(xw, player, showAll, toSavedGame);

            } else {
                tile.toXMLMinimal(xw);
            }
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

            int height = xr.getAttribute(HEIGHT_TAG, -1);

            if (width > 0 && height > 0) {
                tiles = new Tile[width][height];
            }
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

        super.readChildren(xr);

        // @compat 0.10.5
        if (fixupHighSeas) resetHighSeasCount();
        // end @compat
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final Game game = getGame();
        final String tag = xr.getLocalName();

        if (Region.getXMLElementTagName().equals(tag)) {
            putRegion(xr.readFreeColGameObject(game, Region.class));

        } else if (Tile.getXMLElementTagName().equals(tag)) {
            Tile t = xr.readFreeColGameObject(game, Tile.class);
            if (t != null) {
                setTile(t, t.getX(), t.getY());

                // @compat 0.10.5
                if (t.getHighSeasCount() == Tile.FLAG_RECALCULATE) {
                    fixupHighSeas = true;
                }
                // end @compat
            }

        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
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
