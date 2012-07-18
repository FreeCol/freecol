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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.pathfinding.CostDecider;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
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
        NONE, LAND, TERRAIN, RIVERS, RESOURCES, NATIVES, ALL;
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
         * @param n The number of places to rotate.
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
         * Returns the reverse direction of the given direction.
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
         * Useful if we to step in a particular direction, but if this
         * fails to be able to try the closest other directions to the
         * original one in order.
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

    /**
     * The infinite cost used by
     * {@link #findPath(Unit, Tile, Tile, Unit, CostDecider)}.
     */
    public static final int COST_INFINITY = Integer.MIN_VALUE;

    private Tile[][] tiles;

    /**
     * The highest map layer included.
     */
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

    /**
     * Variable used to convert rows to latitude.
     */
    private float latitudePerRow;

    /**
     * The regions, indexed by id.
     */
    private final java.util.Map<String, Region> regions
        = new HashMap<String, Region>();

    /**
     * The search tracing status.
     */
    private boolean traceSearch = false;


    /**
     * Create a new <code>Map</code> from a collection of tiles.
     *
     * @param game
     *            The <code>Game</code> this map belongs to.
     * @param tiles
     *            The 2D array of tiles.
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
     * @param game
     *            The <code>Game</code> this map belongs to.
     * @param in
     *            The input stream containing the XML.
     * @throws XMLStreamException
     *             if a problem was encountered during parsing.
     */
    public Map(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);
        readFromXML(in);
    }

    /**
     * Initiates a new <code>Map</code> with the given ID. The object should
     * later be initialized by calling either
     * {@link #readFromXML(XMLStreamReader)} or
     * {@link #readFromXMLElement(Element)}.
     *
     * @param game
     *            The <code>Game</code> in which this object belong.
     * @param id
     *            The unique identifier for this object.
     */
    public Map(Game game, String id) {
        super(game, id);
    }

    /**
     * Get the <code>Layer</code> value.
     *
     * @return a <code>Layer</code> value
     */
    public final Layer getLayer() {
        return layer;
    }

    /**
     * Set the <code>Layer</code> value.
     *
     * @param newLayer The new Layer value.
     */
    public final void setLayer(final Layer newLayer) {
        this.layer = newLayer;
    }

    /**
     * Get the <code>MinimumLatitude</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getMinimumLatitude() {
        return minimumLatitude;
    }

    /**
     * Set the <code>MinimumLatitude</code> value.
     *
     * @param newMinimumLatitude The new MinimumLatitude value.
     */
    public final void setMinimumLatitude(final int newMinimumLatitude) {
        this.minimumLatitude = newMinimumLatitude;
        calculateLatitudePerRow();
    }

    /**
     * Get the <code>MaximumLatitude</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getMaximumLatitude() {
        return maximumLatitude;
    }

    /**
     * Set the <code>MaximumLatitude</code> value.
     *
     * @param newMaximumLatitude The new MaximumLatitude value.
     */
    public final void setMaximumLatitude(final int newMaximumLatitude) {
        this.maximumLatitude = newMaximumLatitude;
        calculateLatitudePerRow();
    }

    /**
     * Get the <code>LatitudePerRow</code> value.
     *
     * @return a <code>float</code> value
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
     * Get the latitude of the given map row.
     *
     * @param row an <code>int</code> value
     * @return an <code>int</code> value
     */
    public int getLatitude(int row) {
        return minimumLatitude + (int) (row * latitudePerRow);
    }

    /**
     * Get the map row with the given latitude.
     *
     * @param latitude an <code>int</code> value
     * @return an <code>int</code> value
     */
    public int getRow(int latitude) {
        return (int) ((latitude - minimumLatitude) / latitudePerRow);
    }

    /**
     * Returns a Collection containing all map regions.
     *
     * @return a Collection containing all map regions
     */
    public Collection<Region> getRegions() {
        return regions.values();
    }

    /**
     * Returns the <code>Region</code> with the given key (the nameKey).
     *
     * @param key The key to lookup the region with.
     * @return The region with the given name key.
     */
    public Region getRegion(final String id) {
        return regions.get(id);
    }

    /**
     * Returns the <code>Region</code> with the given name.
     *
     * @param id a <code>String</code> value
     * @return a <code>Region</code> value
     */
    public Region getRegionByName(final String id) {
        for (Region region : regions.values()) {
            if (id.equals(region.getName())) {
                return region;
            }
        }
        return null;
    }

    /**
     * Describe <code>setRegion</code> method here.
     *
     * @param region a <code>Region</code> value
     */
    public void setRegion(final Region region) {
        regions.put(region.getNameKey(), region);
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
     * Returns the Tile at a requested position.
     *
     * @param p
     *            The position.
     * @return The Tile at the given position.
     */
    public Tile getTile(Position p) {
        return getTile(p.getX(), p.getY());
    }

    /**
     * Returns the Tile at position (x, y). 'x' specifies a column and 'y'
     * specifies a row. (0, 0) is the Tile at the top-left corner of the Map.
     *
     * @param x
     *            The x-coordinate of the <code>Tile</code>.
     * @param y
     *            The y-coordinate of the <code>Tile</code>.
     * @return The Tile at position (x, y) or <code>null</code> if the
     *         position is invalid.
     */
    public Tile getTile(int x, int y) {
        if (isValid(x, y)) {
            return tiles[x][y];
        } else {
            return null;
        }
    }

    /**
     * Sets the given tile the the given coordinates.
     *
     * @param x
     *            The x-coordinate of the <code>Tile</code>.
     * @param y
     *            The y-coordinate of the <code>Tile</code>.
     * @param tile
     *            The <code>Tile</code>.
     */
    public void setTile(Tile tile, int x, int y) {
        tiles[x][y] = tile;
    }

    /**
     * Returns the width of this Map.
     *
     * @return The width of this Map.
     */
    public int getWidth() {
        if (tiles == null) {
            return 0;
        } else {
            return tiles.length;
        }
    }

    /**
     * Returns the height of this Map.
     *
     * @return The height of this Map.
     */
    public int getHeight() {
        if (tiles == null) {
            return 0;
        } else {
            return tiles[0].length;
        }
    }

    /**
     * Returns the direction a unit needs to move in
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
            if (t1.getNeighbourOrNull(d) == t2) {
                return d;
            }
        }
        return null;
    }

    /**
     * Gets the list of tiles that might be claimable by a settlement.
     * We can not do a simple iteration of the rings because this
     * allows settlements to claim tiles across unclaimable gaps
     * (e.g. Aztecs owning tiles on nearby islands).  So we have to
     * only allow tiles that are adjacent to a known connected tile.
     *
     * @param player The <code>Player</code> that intends to found a settlement.
     * @param centerTile The intended settlement center <code>Tile</code>.
     * @param radius The radius of the settlement.
     * @return A list of potentially claimable tiles.
     */
    public List<Tile> getClaimableTiles(Player player, Tile centerTile,
                                        int radius) {
        List<Tile> tiles = new ArrayList<Tile>();
        List<Tile> layer = new ArrayList<Tile>();
        if (player.canClaimToFoundSettlement(centerTile)) {
            layer.add(centerTile);
            for (int r = 1; r <= radius; r++) {
                List<Tile> lastLayer = new ArrayList<Tile>(layer);
                tiles.addAll(layer);
                layer.clear();
                for (Tile have : lastLayer) {
                    for (Tile next : have.getSurroundingTiles(1)) {
                        if (!tiles.contains(next)
                            && player.canClaimForSettlement(next)) {
                            layer.add(next);
                        }
                    }
                }
            }
            tiles.addAll(layer);
        }
        return tiles;
    }


    /**
     * Simple interface to supply a heuristic to the A* routine.
     */
    private interface SearchHeuristic {
        public int getValue(Tile tile);
    }

    /**
     * Finds a shortest path between the given tiles.  The tile at the
     * end will not be checked for validity.
     *
     * @param unit The <code>Unit</code> to find the path for.
     * @param start The <code>Tile</code> in which the path starts from.
     * @param end The <code>Tile</code> at the end of the path.
     * @param carrier An optional naval carrier <code>Unit</code> to use.
     * @param costDecider An optional <code>CostDecider</code> for
     *     determining the movement costs (uses default cost deciders
     *     for the unit/s if not provided).
     * @return A <code>PathNode</code> for the path found, or null if
     *     none found.
     * @throws IllegalArgumentException If <code>start</code> equals end.
     */
    public PathNode findPath(final Unit unit, final Tile start,
                             final Tile end, final Unit carrier,
                             CostDecider costDecider) {
        if (start == end) {
            throw new IllegalArgumentException("start == end");
        }

        PathNode path = findFullPath(unit, start, end, carrier, costDecider);
        return (path == null) ? null : path.next;
    }

    /**
     * Finds the best path to <code>Europe</code>.
     *
     * @param unit The <code>Unit</code> that should be used to determine
     *     whether or not a path is legal.
     * @param start The starting <code>Tile</code>.
     * @param costDecider An optional <code>CostDecider</code>
     *     responsible for determining the path cost.
     * @return The path to Europe, or null if not found.
     * @throws IllegalArgumentException If <code>start</code> or
     *     <code>unit</code> are null, or the unit is not able to move
     *     to Europe.
     */
    public PathNode findPathToEurope(Unit unit, Tile start,
                                     CostDecider costDecider) {
        Europe europe = null;
        if (unit == null) {
            throw new IllegalArgumentException("Null unit.");
        } else if (start == null) {
            throw new IllegalArgumentException("Null start.");
        } else if ((europe = unit.getOwner().getEurope()) == null) {
            throw new IllegalArgumentException("Null Europe.");
        } else if (!unit.getType().canMoveToHighSeas()) {
            throw new IllegalArgumentException("Unit can not move to Europe: "
                + unit);
        }

        PathNode path = findFullPath(unit, start, europe, null, costDecider);
        return (path == null) ? null : path.next;
    }

    /**
     * Finds the best path to <code>Europe</code> independently of any unit.
     * This method is intended to be executed by the server code,
     * with complete knowledge of the map
     *
     * @param start The starting <code>Tile</code>.
     * @return The path to the target or null if no target can be found.
     * @throws IllegalArgumentException If <code>start</code> is null.
     * @see Europe
    public PathNode findPathToEurope(Tile start) {
        if (start == null) {
            throw new IllegalArgumentException("Null start.");
        }

        PathNode path = search(null, start,
            getLocationGoalDecider(unit.getOwner().getEurope()),
            INFINITY, null, null);
        return (path == null) ? null : path.next;
    }
     */

    /**
     * Searches for a goal determined by the given <code>GoalDecider</code>.
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
     * @return The path to a goal or null if none can be found.
     * @throws IllegalArgumentException If <code>start</code> or
     *     <code>unit</code> are null.
     */
    public PathNode search(final Unit unit, final Tile start,
                           final GoalDecider goalDecider,
                           final CostDecider costDecider,
                           final int maxTurns, final Unit carrier) {
        if (unit == null) {
            throw new IllegalArgumentException("Null unit.");
        } else if (start == null) {
            throw new IllegalArgumentException("Null start.");
        } else if (carrier != null) {
            if (!carrier.isNaval()) {
                throw new IllegalArgumentException("Non-naval carrier: "
                    + carrier);
            } else if (unit.isNaval()) {
                throw new IllegalArgumentException("Carrier for naval unit: "
                    + unit);
            }
        }
        PathNode path = search(unit, start, goalDecider, costDecider,
                               maxTurns, carrier, null);
        return (path == null) ? null : path.next;
    }

    /**
     * Builds a simple goal decider to find a single target location.
     *
     * @param target The target <code>Location</code>.
     * @return A simple <code>GoalDecider</code> that only succeeds for the
     *     target location.
     */
    private GoalDecider getLocationGoalDecider(final Location target) {
        return new GoalDecider() {
            private PathNode best = null;

            public PathNode getGoal() { return best; }
            public boolean hasSubGoals() { return false; }
            public boolean check(Unit u, PathNode path) {
                if (path.getLocation() == target) {
                    if (best == null || path.getCost() < best.getCost()) {
                        best = path;
                    }
                    return true;
                }
                return false;
            }
        };
    }

    /**
     * Builds a simple goal decider to find a path to the high seas.
     *
     * @return A simple <code>GoalDecider</code> that finds high seas tiles.
     */
    private GoalDecider getHighSeasGoalDecider() {
        return new GoalDecider() {
            private PathNode best = null;

            public PathNode getGoal() { return best; }
            public boolean hasSubGoals() { return false; }
            public boolean check(Unit u, PathNode path) {
                if (path.getTile() != null
                    && path.getTile().isDirectlyHighSeasConnected()) {
                    if (best == null || path.getCost() < best.getCost()) {
                        best = path;
                    }
                    return true;
                }
                return false;
            }
        };
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
     * Version of findPath that includes the start tile and generalized
     * start and end locations.
     *
     * @param unit The <code>Unit</code> to find the path for.
     * @param start The <code>Location</code> in which the path starts from.
     * @param end The <code>Location</code> at the end of the path.
     * @param carrier An optional naval carrier <code>Unit</code> to use.
     * @param costDecider An optional <code>CostDecider</code> for
     *     determining the movement costs (uses default cost deciders
     *     for the unit/s if not provided).
     * @return A <code>PathNode</code> starting at the start location and
     *     ending at the end location, or null if no path is found.
     * @throws IllegalArgumentException If the unit is null, or
     *     the start and end locations are not Europe or yield Tiles,
     *     or the carrier/unit combination is bogus.
     */
    public PathNode findFullPath(final Unit unit,
                                 final Location start, final Location end,
                                 final Unit carrier, CostDecider costDecider) {
        if (unit == null) {
            throw new IllegalArgumentException("Null unit.");
        } else if (start == null) {
            throw new IllegalArgumentException("Null start.");
        } else if (!(start instanceof Europe || start.getTile() != null)) {
            throw new IllegalArgumentException("Invalid start: " + start);
        } else if (end == null) {
            throw new IllegalArgumentException("Null end.");
        } else if (!(end instanceof Europe || end.getTile() != null)) {
            throw new IllegalArgumentException("Invalid end: " + end);
        } else if (carrier != null) {
            if (!carrier.isNaval()) {
                throw new IllegalArgumentException("Non-naval carrier: "
                    + carrier);
            } else if (unit.isNaval()) {
                throw new IllegalArgumentException("Carrier for naval unit: "
                    + unit);
            }
        }

        Unit waterUnit = (carrier != null) ? carrier : unit;
        PathNode path;
        if (start instanceof Europe) {
            if (end instanceof Europe) { // The trivial(Europe) path.
                return new PathNode(start, unit.getMovesLeft(), 0,
                                    false, null, null);

            } else { // Start in Europe, end on a Tile
                // Fail fast without water unit.
                if (!waterUnit.getType().canMoveToHighSeas()) return null;

                // Search backwards from target to get best entry location.
                path = search(unit, end.getTile(), getHighSeasGoalDecider(),
                              costDecider, INFINITY, carrier, null);
                if (path == null) return null;

                // Now search forward from there to get a path in the right
                // order (the existing one might not be optimal if reversed!)
                Tile entry = path.getLastNode().getTile();
                path = search(unit, entry,
                              getLocationGoalDecider(end.getTile()),
                              costDecider, INFINITY, carrier,
                              getManhattenHeuristic(entry));

                // At the front of the path insert a node for the
                // starting location in Europe, correcting for the turns
                // to sail to the entry location.
                for (PathNode p = path; p != null; p = p.next) {
                    p.setTurns(p.getTurns() + waterUnit.getSailTurns());
                }
                return path.previous = new PathNode(start, unit.getMovesLeft(),
                                                    0, carrier != null,
                                                    null, path);
            }
        } else { // start has Tile
            if (end instanceof Europe) {
                // Fail fast if Europe is unattainable.
                if (!waterUnit.getType().canMoveToHighSeas()) return null;
                
                // Search forwards to the high seas.
                path = search(unit, start.getTile(), getHighSeasGoalDecider(),
                              costDecider, INFINITY, carrier, null);
                if (path == null) return null;

                // Add a final node for Europe and return the path.
                PathNode seas = path.getLastNode();
                seas.next = new PathNode(end, unit.getInitialMovesLeft(),
                    seas.getTurns() + waterUnit.getSailTurns(),
                    seas.isOnCarrier(), seas, null);
                return path;

            } else { // start and end are Tiles
                final Tile startTile = start.getTile();
                final Tile endTile = end.getTile();
                final GoalDecider gd = getLocationGoalDecider(endTile);
                final SearchHeuristic sh = getManhattenHeuristic(endTile);
                if (startTile.getContiguity() == endTile.getContiguity()) {
                    // If the unit potentially could get to the
                    // destination without a carrier, compare both
                    // with-carrier and without-carrier paths.  The
                    // latter will usually be faster, but not always,
                    // e.g. mounted units on a good road system.
                    PathNode walkPath = search(unit, startTile, gd,
                        costDecider, INFINITY, null, sh);
                    if (carrier == null) return walkPath;
                    PathNode carrierPath = search(unit, startTile, gd,
                                                  costDecider, INFINITY,
                                                  carrier, sh);
                    return (carrierPath == null) ? walkPath
                        : (walkPath == null) ? carrierPath
                        : (walkPath.getLastNode().getCost()
                            <= carrierPath.getLastNode().getCost())
                        ? walkPath : carrierPath;
                } else if (waterUnit != null) {
                    // If there is a water unit then complex paths which
                    // use settlements and inland lakes are possible, but
                    // hard to capture with the contiguity test, so just
                    // allow the search to proceed.
                    return search(unit, startTile, gd, costDecider, INFINITY,
                                  carrier, sh);
                } else { // Otherwise, there is a connectivity failure.
                    return null;
                }
            }
        }
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
     * @return The path to a goal or null if none can be found.
     * @throws IllegalArgumentException if <code>unit</code> or
     *      <code>start</code> is null or neither Europe or yields a tile,
     *      or the carrier/unit combination is bogus.
     */
    public PathNode searchFullPath(final Unit unit, final Location start,
                                   final GoalDecider goalDecider,
                                   final CostDecider costDecider,
                                   final int maxTurns, final Unit carrier) {
        if (unit == null) {
            throw new IllegalArgumentException("Null unit.");
        } else if (start == null) {
            throw new IllegalArgumentException("Null start.");
        } else if (carrier != null) {
            if (!carrier.isNaval()) {
                throw new IllegalArgumentException("Non-naval carrier: "
                    + carrier);
            } else if (unit.isNaval()) {
                throw new IllegalArgumentException("Carrier for naval unit: "
                    + unit);
            }
        } else if (start instanceof Europe) {
            Unit waterUnit = (carrier != null) ? carrier : unit;
            // Fail fast if Europe is unattainable.
            if (!waterUnit.getType().canMoveToHighSeas()) return null;

            PathNode path = search(unit, (Tile)waterUnit.getEntryLocation(),
                                   goalDecider, costDecider,
                                   maxTurns, carrier, null);
            if (path == null) return null;
            for (PathNode p = path; p != null; p = p.next) {
                p.setTurns(p.getTurns() + waterUnit.getSailTurns());
            }
            return path.previous = new PathNode(start, waterUnit.getMovesLeft(),
                                                0, carrier != null, null, path);
        } else if (start.getTile() == null) {
            throw new IllegalArgumentException("Invalid start: " + start);
        }
        return search(unit, start.getTile(), goalDecider, costDecider,
                      maxTurns, carrier, null);
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
        private boolean costed;
        

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
            this.cost = 0;
            this.costed = false;
        }

        /**
         * Creates a new move candidate where the moves and turns left
         * are specified.
         *
         * @param unit The <code>Unit</code> to move.
         * @param current The current position on the path.
         * @param dst The <code>Location</code> to move to.
         * @param movesLeft The new number of moves left.
         * @param turns The new number of turns.
         * @param onCarrier Will the new move be on a carrier.
         */
        public MoveCandidate(Unit unit, PathNode current, Location dst,
                             int movesLeft, int turns, boolean onCarrier) {
            this.unit = unit;
            this.current = current;
            this.dst = dst;
            this.movesLeft = movesLeft;
            this.turns = turns;
            this.onCarrier = onCarrier;
            this.cost = PathNode.getCost(turns, movesLeft);
            this.costed = true;
        }

        /**
         * Gets the cost of this candidate move.
         *
         * @return The move cost.
         */
        public int getCost() {
            if (!costed) {
                Location src = current.getLocation();
                cost = decider.getCost(unit, src, dst, movesLeft);
                if (cost != CostDecider.ILLEGAL_MOVE) {
                    turns += decider.getNewTurns();
                    movesLeft = decider.getMovesLeft();
                    cost = PathNode.getCost(turns, movesLeft);
                }
                costed = true;
            }
            return cost;
        }

        /**
         * Do not let the CostDecider (which may be conservative)
         * block a final destination if it is still a legal move.
         *
         * @param goalDecider A <code>GoalDecider</code> to check the
         *     goal with.
         */
        public void recoverGoal(GoalDecider goalDecider) {
            if (getCost() == CostDecider.ILLEGAL_MOVE
                && unit != null
                && current.getTile() != null
                && dst.getTile() != null
                && (unit.getSimpleMoveType(current.getTile(), dst.getTile())
                    .isLegal())
                && goalDecider != null
                && goalDecider.check(unit,
                    new PathNode(dst, movesLeft, turns, onCarrier, 
                        current, null))) {
                // Pretend it finishes the move.
                movesLeft = unit.getInitialMovesLeft();
                turns++;
                cost = PathNode.getCost(turns, movesLeft);
            }
        }

        /**
         * Does this move candidate improve on a specified move.
         *
         * @param best The <code>PathNode</code> to compare against.
         */
        public boolean canImprove(PathNode best) {
            return getCost() != CostDecider.ILLEGAL_MOVE
                && (best == null || getCost() < best.getCost());
        }

        /**
         * Replace a given path with this candidate move.
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
            PathNode path = openList.get(dst.getId());
            if (path != null) {
                openList.remove(dst.getId());
                openListQueue.remove(path);
            }
            path = new PathNode(dst, movesLeft, turns, onCarrier,
                                current, null);
            int cost = getCost();
            if (sh != null && dst.getTile() != null) {
                cost += sh.getValue(dst.getTile());
            }
            f.put(dst.getId(), new Integer(cost));
            openList.put(dst.getId(), path);
            openListQueue.offer(path);
        }
    };

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
     * @param unit The <code>Unit</code> to find a path for, which may be null!
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
     * @return The path to a goal determined by the given
     *     <code>GoalDecider</code>.
     */
    private PathNode search(final Unit unit, final Tile start,
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
        final Europe europe = (unit == null) ? null
            : unit.getOwner().getEurope();
        final List<Location> tracing = (traceSearch) ? new ArrayList<Location>()
            : null;
        Unit waterUnit = (carrier != null) ? carrier : unit;
        Unit currentUnit = (start.isLand())
            ? ((start.getSettlement() != null
                    && unit != null
                    && unit.getLocation() == carrier) ? carrier : unit)
            : waterUnit;

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
            if (tracing != null) tracing.add(currentLocation);
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
                // Usually when moving into a settlement it will
                // be useful to dock the carrier so it can collect new
                // cargo.  OTOH if the carrier is just passing through
                // the right thing is to keep the passenger on board.
                // Still there is a suboptimality if the carrier ends
                // its turn one short of a settlement where there is
                // no useful cargo but we fail to consider an
                // immediate move into the settlement by the
                // passenger.  The trouble with looking for such
                // special cases is that we do not know yet whether
                // that settlement is worth disembarking the unit to.
                // There have been nasty bugs where passengers would
                // disembark too early, so for now we live with the
                // suboptimality.
                //
                boolean unitMove = unit == null
                    || unit.isTileAccessible(moveTile);
                boolean carrierMove = carrier != null
                    && carrier.isTileAccessible(moveTile);
                MoveStep step = (currentOnCarrier)
                    ? ((carrierMove) ? MoveStep.BYWATER
                        : (unitMove) ? MoveStep.DISEMBARK
                        : MoveStep.FAIL)
                    : ((carrierMove
                            && !usedCarrier(currentNode)) ? MoveStep.EMBARK
                        : (unitMove) ? ((unit.isNaval()) ? MoveStep.BYWATER
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
                    move = new MoveCandidate(waterUnit, currentNode, moveTile,
                        currentMovesLeft, currentTurns, currentOnCarrier,
                        ((costDecider != null) ? costDecider
                            : CostDeciders.defaultCostDeciderFor(waterUnit)));
                    break;
                case EMBARK:
                    move = new MoveCandidate(carrier, currentNode, moveTile,
                        carrier.getInitialMovesLeft(), currentTurns, true);
                    break;
                case DISEMBARK:
                    // Check if embarked this turn.
                    // Consume an extra move if so.
                    int moveTurns = currentTurns + 1;
                    for (PathNode p = currentNode; p != null; p = p.previous) {
                        if (!p.isOnCarrier()) {
                            if (p.getTurns() == currentTurns) moveTurns++;
                            break;
                        }
                    }                                
                    move = new MoveCandidate(unit, currentNode, moveTile,
                        unit.getInitialMovesLeft(), moveTurns, false);
                    break;
                case FAIL: default: // Loop on failure.
                    move = null;
                    break;
                }

                if (move != null) {
                    // Special case when on the map.
                    move.recoverGoal(goalDecider);

                    // Is this an improvement?  If not, ignore.
                    if (move.canImprove(openList.get(moveTile.getId()))) {
                        move.improve(openList, openListQueue, f,
                                     searchHeuristic);
                    }
                }
            }

            // Also try moving to Europe, if it exists and the move is ok.
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
            for (Location t : tracing) logMe += " " + t;
            logMe += "\n";
            if (best != null) logMe += best.fullPathToString() + "\n";
            logger.info(logMe);
        }

        return best;
    }

    /**
     * Searches for land within the given radius.
     *
     * @param x
     *            X-component of the position to search from.
     * @param y
     *            Y-component of the position to search from.
     * @param distance
     *            The radius that should be searched for land, given in number
     *            of {@link Tile tiles}.
     * @return <code>true</code> if there is {@link Tile#isLand land} within
     *         the given radius and <code>false</code> otherwise.
     */
    public boolean isLandWithinDistance(int x, int y, int distance) {
        Iterator<Position> i = getCircleIterator(new Position(x, y), true,
                distance);
        while (i.hasNext()) {
            if (getTile(i.next()).isLand()) {
                return true;
            }
        }

        return false;
    }


    /**
     * Gets an <code>Iterator</code> of every <code>Tile</code> on the map.
     *
     * @return the <code>Iterator</code>.
     */
    public WholeMapIterator getWholeMapIterator() {
        return new WholeMapIterator();
    }


    /**
     * Get an adjacent iterator.
     *
     * @param centerPosition
     *            The center position to iterate around
     * @return Iterator
     */
    public Iterator<Position> getAdjacentIterator(Position centerPosition) {
        return new AdjacentIterator(centerPosition);
    }

    /**
     * Get a border adjacent iterator.
     *
     * @param centerPosition
     *            The center position to iterate around
     * @return Iterator
     */
    public Iterator<Position> getBorderAdjacentIterator(Position centerPosition) {
        return new BorderAdjacentIterator(centerPosition);
    }

    /**
     * Get a flood fill iterator.
     *
     * @param centerPosition
     *            The center position to iterate around
     * @return Iterator
     */
    public Iterator<Position> getFloodFillIterator(Position centerPosition) {
        return new CircleIterator(centerPosition, true, INFINITY);
    }

    /**
     * Get a circle iterator.
     *
     * @param center
     *            The center position to iterate around
     * @param isFilled
     *            True to get all of the positions in the circle
     * @param radius
     *            Radius of circle
     * @return Iterator
     */
    public CircleIterator getCircleIterator(Position center, boolean isFilled,
            int radius) {
        return new CircleIterator(center, isFilled, radius);
    }

    /**
     * Checks whether a position is valid (within the map limits).
     *
     * @param position
     *            The position
     * @return True if it is valid
     */
    public boolean isValid(Position position) {
        return isValid(position.x, position.y, getWidth(), getHeight());
    }

    /**
     * Checks whether a position is valid (within the map limits).
     *
     * @param x
     *            X coordinate
     * @param y
     *            Y coordinate
     * @return True if it is valid
     */
    public boolean isValid(int x, int y) {
        return isValid(x, y, getWidth(), getHeight());
    }

    /**
     * Checks whether a position is valid.
     *
     * @param position The <code>Position</code> to check.
     * @param width The width of the map.
     * @param height The height of the map.
     * @return True if the given position is within the bounds of the map.
     */
    public static boolean isValid(Position position, int width, int height) {
        return isValid(position.x, position.y, width, height);
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
     * Represents a position on the Map.
     */
    public static final class Position {
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
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }

        /**
         * Gets the position adjacent to a given position, in a given
         * direction.
         *
         * @param direction The direction (N, NE, E, etc.)
         * @return Adjacent position
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
         * @param position The <code>Position</code> to compare.
         * @return The distance to the other position.
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
    }

    /**
     * Base class for internal iterators.
     */
    private abstract class MapIterator implements Iterator<Position> {

        /**
         * Get the next position as a position rather as an object.
         *
         * @return position.
         * @throws NoSuchElementException
         *             if iterator is exhausted.
         */
        public abstract Position nextPosition() throws NoSuchElementException;

        /**
         * Returns the next element in the iteration.
         *
         * @return the next element in the iteration.
         * @exception NoSuchElementException
         *                iteration has no more elements.
         */
        public Position next() {
            return nextPosition();
        }

        /**
         * Removes from the underlying collection the last element returned by
         * the iterator (optional operation).
         *
         * @exception UnsupportedOperationException
         *                no matter what.
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public final class WholeMapIterator extends MapIterator {
        private int x;
        private int y;

        /**
         * Default constructor.
         */
        public WholeMapIterator() {
            x = 0;
            y = 0;
        }

        /**
         * Determine if the iterator has another position in it.
         *
         * @return True of there is another position
         */
        public boolean hasNext() {
            return y < getHeight();
        }

        /**
         * Obtain the next position to iterate over.
         *
         * @return Next position
         * @throws java.util.NoSuchElementException
         *             if last position already returned
         */
        @Override
        public Position nextPosition() throws NoSuchElementException {
            if (y < getHeight()) {
                Position newPosition = new Position(x, y);
                x++;
                if (x == getWidth()) {
                    x = 0;
                    y++;
                }
                return newPosition;
            }
            throw new NoSuchElementException("Iterator exhausted");
        }
    }

    private final class AdjacentIterator extends MapIterator {
        // The starting tile position
        private Position basePosition;
        // Index into the list of adjacent tiles
        private int x = 0;

        /**
         * The constructor to use.
         *
         * @param basePosition
         *            The position around which to iterate
         */
        public AdjacentIterator(Position basePosition) {
            this.basePosition = basePosition;
        }

        /**
         * Determine if the iterator has another position in it.
         *
         * @return True of there is another position
         */
        public boolean hasNext() {
            for (int i = x; i < Direction.NUMBER_OF_DIRECTIONS; i++) {
                Position newPosition = basePosition.getAdjacent(Direction.values()[i]);
                if (isValid(newPosition))
                    return true;
            }
            return false;
        }

        /**
         * Obtain the next position to iterate over.
         *
         * @return Next position
         * @throws NoSuchElementException
         *             if last position already returned
         */
        @Override
        public Position nextPosition() throws NoSuchElementException {
            for (int i = x; i < Direction.NUMBER_OF_DIRECTIONS; i++) {
                Position newPosition = basePosition.getAdjacent(Direction.values()[i]);
                if (isValid(newPosition)) {
                    x = i + 1;
                    return newPosition;
                }
            }
            throw new NoSuchElementException("Iterator exhausted");
        }
    }

    /**
     * An interator returning positions in a spiral starting at a given center
     * tile. The center tile is never included in the positions returned, and
     * all returned positions are valid.
     *
     * @see Map.Position
     */
    public final class CircleIterator extends MapIterator {
        private int radius;
        private int currentRadius;
        private Position nextPosition = null;
        // The current position in the circle with the current radius:
        private int n;

        /**
         * The constructor to use.
         *
         * @param center
         *            The center of the circle
         * @param isFilled
         *            True to get all of the positions within the circle
         * @param radius
         *            The radius of the circle
         */
        public CircleIterator(Position center, boolean isFilled, int radius) {
            this.radius = radius;

            if (center == null) {
                throw new IllegalArgumentException("center must not be null.");
            }

            n = 0;

            if (isFilled || radius == 1) {
                nextPosition = center.getAdjacent(Direction.NE);
                currentRadius = 1;
            } else {
                currentRadius = radius;
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
         * Returns the current radius of the circle.
         *
         * @return The distance from the center tile this
         *         <code>CircleIterator</code> was initialized with.
         */
        public int getCurrentRadius() {
            return currentRadius;
        }

        /**
         * Finds the next position.
         */
        private void determineNextPosition() {
            boolean positionReturned = (n != 0);
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
         * Determine if the iterator has another position in it.
         *
         * @return <code>true</code> of there is another position and
         *         <code>false</code> otherwise.
         */
        public boolean hasNext() {
            return nextPosition != null;
        }

        /**
         * Obtains the next position.
         *
         * @return The next position. This position is guaranteed to be
         *         {@link Map#isValid(net.sf.freecol.common.model.Map.Position) valid}.
         */
        @Override
        public Position nextPosition() {
            if (nextPosition != null) {
                final Position p = nextPosition;
                determineNextPosition();
                return p;
            } else {
                return null;
            }
        }
    }

    /**
     * Make the map usable as a parameter in the for-loop.
     *
     * Returns all Tiles based on the order of the WholeMapIterator.
     *
     * @return An Iterable that can be used to get an iterator for all tiles of the map.
     */
    public Iterable<Tile> getAllTiles() {
        return new Iterable<Tile>(){
            public Iterator<Tile> iterator(){
                final WholeMapIterator m = getWholeMapIterator();

                return new Iterator<Tile>(){
                    public boolean hasNext() {
                        return m.hasNext();
                    }

                    public Tile next() {
                        return getTile(m.next());
                    }

                    public void remove() {
                        m.remove();
                    }
                };
            }
        };
    }

    private final class BorderAdjacentIterator extends MapIterator {
        // The starting tile position
        private Position basePosition;
        // Index into the list of adjacent tiles
        private int index;

        /**
         * The constructor to use.
         *
         * @param basePosition
         *            The position around which to iterate
         */
        public BorderAdjacentIterator(Position basePosition) {
            this.basePosition = basePosition;
            index = 1;
        }

        /**
         * Determine if the iterator has another position in it.
         *
         * @return True of there is another position
         */
        public boolean hasNext() {
            for (int i = index; i < Direction.NUMBER_OF_DIRECTIONS; i += 2) {
                Position newPosition = basePosition.getAdjacent(Direction.values()[i]);
                if (isValid(newPosition))
                    return true;
            }
            return false;
        }

        /**
         * Obtain the next position to iterate over.
         *
         * @return Next position
         * @throws NoSuchElementException
         *             if last position already returned
         */
        @Override
        public Position nextPosition() throws NoSuchElementException {
            for (int i = index; i < Direction.NUMBER_OF_DIRECTIONS; i += 2) {
                Position newPosition = basePosition.getAdjacent(Direction.values()[i]);
                if (isValid(newPosition)) {
                    index = i + 2;
                    return newPosition;
                }
            }
            throw new NoSuchElementException("Iterator exhausted");
        }
    }

    /**
     * Are two locations non-null and either the same or at the same tile.
     * This routine is here because Location is an interface.
     *
     * @param l1 The first <code>Location</code>.
     * @param l2 The second <code>Location</code>.
     * @return True if this location is the same or at the same tile.
     */
    public static final boolean isSameLocation(Location l1, Location l2) {
        return (l1 == null || l2 == null) ? false
            : (l1 == l2) ? true
            : (l1.getTile() == null) ? false
            : l1.getTile() == l2.getTile();
    }


    // Location interface

    /**
     * Returns <code>null</code>.
     *
     * @return <code>null</code>
     */
    public Tile getTile() {
        return null;
    }

    /**
     * Returns the name of this location.
     *
     * @return The name of this location.
     */
    public StringTemplate getLocationName() {
        return StringTemplate.key("NewWorld");
    }

    /**
     * Returns the name of this location for a particular player.
     *
     * @param player The <code>Player</code> to return the name for.
     * @return The name of this location.
     */
    public StringTemplate getLocationNameFor(Player player) {
        String name = player.getNewLandName();
        if (name == null) {
            return getLocationName();
        } else {
            return StringTemplate.name(name);
        }
    }

    /**
     * Adds a <code>Locatable</code> to this Location. It the given
     * Locatable is a Unit, its location is set to its entry location,
     * otherwise nothing happens.
     *
     * @param locatable
     *            The <code>Locatable</code> to add to this Location.
     */
    public boolean add(Locatable locatable) {
        if (locatable instanceof Unit) {
            Unit unit = (Unit) locatable;
            unit.setLocation(unit.getEntryLocation());
            return true;
        }
        return false;
    }

    /**
     * Removes a <code>Locatable</code> from this Location.
     *
     * @param locatable
     *            The <code>Locatable</code> to remove from this Location.
     */
    public boolean remove(Locatable locatable) {
        if (locatable instanceof Unit) {
            Tile tile = ((Unit) locatable).getTile();
            if (tile != null) {
                return tile.remove(locatable);
            }
        }
        return false;
    }

    /**
     * Checks if this <code>Location</code> contains the specified
     * <code>Locatable</code>.
     *
     * @param locatable
     *            The <code>Locatable</code> to test the presence of.
     * @return
     *            <ul>
     *            <li><i>true</i> if the specified <code>Locatable</code> is
     *            on this <code>Location</code> and
     *            <li><i>false</i> otherwise.
     *            </ul>
     */
    public boolean contains(Locatable locatable) {
        if (locatable.getLocation() == null) {
            return false;
        } else if (locatable.getLocation().getTile() == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Checks whether or not the specified locatable may be added to this
     * <code>Location</code>.
     *
     * @param locatable
     *            The <code>Locatable</code> to add.
     * @return The result.
     */
    public boolean canAdd(Locatable locatable) {
        return (locatable instanceof Unit);
    }

    /**
     * Returns <code>-1</code>
     *
     * @return <code>-1</code>
     */
    public int getUnitCount() {
        return -1;
    }

    /**
     * Returns an empty list.
     *
     * @return an empty list
     */
    public List<Unit> getUnitList() {
        return Collections.emptyList();
    }

    /**
     * Returns an <code>Iterator</code> for an empty list.
     *
     * @return The <code>Iterator</code>.
     */
    public Iterator<Unit> getUnitIterator() {
        return getUnitList().iterator();
    }

    /**
     * Gets the <code>GoodsContainer</code> this <code>Location</code> use
     * for storing it's goods.
     *
     * @return The <code>GoodsContainer</code> or <code>null</code> if the
     *         <code>Location</code> cannot store any goods.
     */
    public GoodsContainer getGoodsContainer() {
        return null;
    }

    /**
     * Returns <code>null</code>.
     *
     * @return <code>null</code>
     */
    public Settlement getSettlement() {
        return null;
    }

    /**
     * Returns <code>null</code>.
     *
     * @return <code>null</code>
     */
    public Colony getColony() {
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
        Queue<Position>q = new LinkedList<Position>();
        boolean[][] visited = new boolean[boolmap.length][boolmap[0].length];
        visited[p.getX()][p.getY()] = true;
        limit--;
        do {
            for (Direction direction : Direction.values()) {
                Position n = p.getAdjacent(direction);
                if (Map.isValid(n, boolmap.length, boolmap[0].length)
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

    // Serialization

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
     * @param out
     *            The target stream.
     * @param player
     *            The <code>Player</code> this XML-representation should be
     *            made for, or <code>null</code> if
     *            <code>showAll == true</code>.
     * @param showAll
     *            Only attributes visible to <code>player</code> will be added
     *            to the representation if <code>showAll</code> is set to
     *            <i>false</i>.
     * @param toSavedGame
     *            If <code>true</code> then information that is only needed
     *            when saving a game is added.
     * @throws XMLStreamException
     *             if there are any problems writing to the stream.
     */
    @Override
    protected void toXMLImpl(XMLStreamWriter out, Player player,
                             boolean showAll, boolean toSavedGame)
        throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());
        writeAttributes(out, player, showAll, toSavedGame);
        writeChildren(out, player, showAll, toSavedGame);
        out.writeEndElement();
    }

    /**
     * {@inheritDoc}
     */
    protected void writeAttributes(XMLStreamWriter out, Player player,
                                   boolean showAll, boolean toSavedGame)
        throws XMLStreamException {
        out.writeAttribute(ID_ATTRIBUTE, getId());
        out.writeAttribute("width", Integer.toString(getWidth()));
        out.writeAttribute("height", Integer.toString(getHeight()));
        out.writeAttribute("layer", layer.toString());
        out.writeAttribute("minimumLatitude", Integer.toString(minimumLatitude));
        out.writeAttribute("maximumLatitude", Integer.toString(maximumLatitude));
    }

    /**
     * {@inheritDoc}
     */
    protected void writeChildren(XMLStreamWriter out, Player player,
                                 boolean showAll, boolean toSavedGame)
        throws XMLStreamException {
        for (Region region : regions.values()) {
            region.toXML(out);
        }

        for (Tile tile: getAllTiles()) {
            if (showAll || toSavedGame || player.hasExplored(tile)) {
                tile.toXML(out, player, showAll, toSavedGame);
            } else {
                tile.toXMLMinimal(out);
            }
        }

    }

    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The input stream with the XML.
     */
    @Override
    protected void readFromXMLImpl(XMLStreamReader in)
            throws XMLStreamException {
        boolean fixupHighSeas = false; // @compat 0.10.5
        setId(in.getAttributeValue(null, ID_ATTRIBUTE));
        setLayer(Layer.valueOf(getAttribute(in, "layer", "ALL")));

        if (tiles == null) {
            int width = Integer.parseInt(in.getAttributeValue(null, "width"));
            int height = Integer.parseInt(in.getAttributeValue(null, "height"));

            tiles = new Tile[width][height];
        }

        minimumLatitude = getAttribute(in, "minimumLatitude", -90);
        maximumLatitude = getAttribute(in, "maximumLatitude", 90);
        calculateLatitudePerRow();

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals(Tile.getXMLElementTagName())) {
                Tile t = updateFreeColGameObject(in, Tile.class);
                setTile(t, t.getX(), t.getY());
                // @compat 0.10.5
                if (t.getHighSeasCount() == Tile.FLAG_RECALCULATE) {
                    fixupHighSeas = true;
                }
                // @end compatibility code
            } else if (in.getLocalName().equals(Region.getXMLElementTagName())) {
                setRegion(updateFreeColGameObject(in, Region.class));
            } else {
                logger.warning("Unknown tag: " + in.getLocalName() + " loading map");
                in.nextTag();
            }
        }

        if (fixupHighSeas) resetHighSeasCount(); // @compat 0.10.5
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "map".
     */
    public static String getXMLElementTagName() {
        return "map";
    }
}
