/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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
import java.util.Comparator;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.pathfinding.CostDecider;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDecider;

/**
 * An isometric map. The map is represented as a collection of tiles.
 */
public class Map extends FreeColGameObject {

    private static final Logger logger = Logger.getLogger(Map.class.getName());

    public final static int POLAR_HEIGHT = 2;

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

        public final static Direction[] longSides = new Direction[] {
            Direction.NE, Direction.SE, Direction.SW, Direction.NW
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

        public Direction getNextDirection() {
            return values()[(ordinal() + 1) % 8];
        }

        public Direction getPreviousDirection() {
            return values()[(ordinal() + 7) % 8];
        }

        /**
         * Returns the reverse direction of the given direction.
         *
         * @return The reverse direction of the given direction.
         */
        public Direction getReverseDirection() {
            return values()[(ordinal() + 4) % 8];
        }

        /**
         * Returns a random Direction.
         *
         * @param random A <code>Random</code> number source.
         * @return a <code>Direction</code> value
         */
        public static Direction getRandomDirection(Random random) {
            return Direction.values()[random.nextInt(NUMBER_OF_DIRECTIONS)];
        }

        /**
         * Creates an array of the eight directions in a random order.
         *
         * @param random A <code>Random</code> number source.
         * @return The array.
         */
        public static Direction[] getRandomDirectionArray(Random random) {
            Direction[] directions = Direction.values();
            for (int i = 0; i < directions.length; i++) {
                int i2 = random.nextInt(NUMBER_OF_DIRECTIONS);
                if (i2 != i) {
                    Direction temp = directions[i2];
                    directions[i2] = directions[i];
                    directions[i] = temp;
                }
            }
            return directions;
        }
    }

    public static final int NUMBER_OF_DIRECTIONS = Direction.values().length;

    /** The infinity cost as used by {@link #findPath(Unit, Tile, Tile)}. */
    public static final int COST_INFINITY = Integer.MIN_VALUE;

    private Tile[][] tiles;

    private final java.util.Map<String, Region> regions = new HashMap<String, Region>();

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

    public Collection<Region> getRegions() {
        return regions.values();
    }

    /**
     * Returns the <code>Region</code> with the given ID.
     *
     * @param id a <code>String</code> value
     * @return a <code>Region</code> value
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
     * Finds a shortest path between the given tiles. The <code>Tile</code> at
     * the <code>end</code> will not be checked against the <code>unit</code>'s
     * legal moves.
     *
     * @param unit
     *            The <code>Unit</code> that should be used to determine
     *            whether or not a path is legal.
     * @param start
     *            The <code>Tile</code> in which the path starts from.
     * @param end
     *            The end of the path.
     * @return A <code>PathNode</code> for the first tile in the path. Calling
     *         {@link PathNode#getTile} on this object, will return the
     *         <code>Tile</code> right after the specified starting tile, and
     *         {@link PathNode#getDirection} will return the direction you need
     *         to take in order to reach that tile. This method returns
     *         <code>null</code> if no path is found.
     * @see #findPath(Tile, Tile, PathType)
     * @see Unit#findPath(Tile)
     * @exception IllegalArgumentException
     *                if either <code>unit</code>, <code>start</code> or
     *                <code>end</code> are <code>null</code>.
     */
    public PathNode findPath(Unit unit, Tile start, Tile end) {
        if (unit == null) {
            throw new IllegalArgumentException("Unit must not be 'null'.");
        }
        return findPath(unit, start, end, null, CostDeciders.defaultFor(unit));
    }

    /**
     * Finds a shortest path between the given tiles. The <code>Tile</code> at
     * the <code>end</code> will not be checked against the <code>unit</code>'s
     * legal moves.
     *
     * @param unit
     *            The <code>Unit</code> that should be used to determine
     *            whether or not a path is legal.
     * @param start
     *            The <code>Tile</code> in which the path starts from.
     * @param end
     *            The end of the path.
     * @param carrier
     *            A carrier that currently holds the <code>unit</code>, or
     *            <code>null</code> if the <code>unit</code> is not
     *            presently on a carrier.
     * @return A <code>PathNode</code> for the first tile in the path. Calling
     *         {@link PathNode#getTile} on this object, will return the
     *         <code>Tile</code> right after the specified starting tile, and
     *         {@link PathNode#getDirection} will return the direction you need
     *         to take in order to reach that tile. This method returns
     *         <code>null</code> if no path is found.
     * @see #findPath(Tile, Tile, PathType)
     * @see Unit#findPath(Tile)
     * @exception IllegalArgumentException
     *                if either <code>unit</code>, <code>start</code> or
     *                <code>end</code> are <code>null</code>.
     */
    public PathNode findPath(Unit unit, Tile start, Tile end, Unit carrier) {
        if (unit == null) {
            throw new IllegalArgumentException("Unit must not be 'null'.");
        }
        return findPath(unit, start, end, carrier,
                        CostDeciders.defaultFor(unit));
    }

    /**
     * Finds a shortest path between the given tiles. The <code>Tile</code> at
     * the <code>end</code> will not be checked against validity (neither the
     * <code>options</code> nor allowed movement by the <code>unit</code>.
     *
     * @param unit an <code>Unit</code> value
     * @param start
     *            The <code>Tile</code> in which the path starts from.
     * @param end
     *            The end of the path.
     * @param carrier
     *            A carrier that currently holds the <code>unit</code>, or
     *            <code>null</code> if the <code>unit</code> is not
     *            presently on a carrier.
     * @param costDecider
     *            The object responsible for determining the cost.
     * @return A <code>PathNode</code> for the first tile in the path. Calling
     *         {@link PathNode#getTile} on this object, will return the
     *         <code>Tile</code> right after the specified starting tile, and
     *         {@link PathNode#getDirection} will return the direction you need
     *         to take in order to reach that tile. This method returns
     *         <code>null</code> if no path is found.
     * @exception IllegalArgumentException
     *                if either <code>start</code> or <code>end</code> are
     *                <code>null</code>.
     * @exception IllegalArgumentException
     *                if either <code>start</code> or <code>end</code> are
     *                <code>null</code>.
     */
    public PathNode findPath(final Unit unit, final Tile start, final Tile end,
                             final Unit carrier,
                             final CostDecider costDecider) {
        /*
         * Using A* with the Manhatten distance as the heuristics.
         *
         * The data structure for the open list is a combined structure: using a
         * HashMap for membership tests and a PriorityQueue for getting the node
         * with the minimal f (cost+heuristics). This gives O(1) on membership
         * test and O(log N) for remove-best and insertions.
         *
         * The data structure for the closed list is simply a HashMap.
         */

        if (start == null) {
            throw new IllegalArgumentException("Argument 'start' must not be 'null'.");
        }
        if (end == null) {
            throw new IllegalArgumentException("Argument 'end' must not be 'null'.");
        }
        if (start.equals(end)) {
            throw new IllegalArgumentException("start == end");
        }
        if (unit == null) {
            throw new IllegalArgumentException("Argument 'unit' must not be 'null'.");
        }

        // What unit starts the path?
        Unit currentUnit = (carrier != null) ? carrier : unit;

        final PathNode firstNode;
        if (currentUnit != null) {
            firstNode = new PathNode(start, 0,
                                     start.getDistanceTo(end),
                                     Direction.N, currentUnit.getMovesLeft(), 0);
            firstNode.setOnCarrier(carrier != null);
        } else {
            firstNode = new PathNode(start, 0, start.getDistanceTo(end),
                                     Direction.N, -1, -1);
        }

        final HashMap<String, PathNode> openList
            = new HashMap<String, PathNode>();
        final HashMap<String, PathNode> closedList
            = new HashMap<String, PathNode>();
        final PriorityQueue<PathNode> openListQueue
            = new PriorityQueue<PathNode>(1024,
                new Comparator<PathNode>() {
                    public int compare(PathNode o, PathNode p) {
                        return o.getF() - p.getF();
                    }
                });

        openList.put(firstNode.getTile().getId(), firstNode);
        openListQueue.offer(firstNode);

        while (!openList.isEmpty()) {
            // Choose the node with the lowest f.
            PathNode currentNode = openListQueue.poll();
            final Tile currentTile = currentNode.getTile();
            openList.remove(currentTile.getId());
            closedList.put(currentTile.getId(), currentNode);

            // Found the goal?
            if (currentTile == end) {
                while (currentNode.previous != null) {
                    currentNode.previous.next = currentNode;
                    currentNode = currentNode.previous;
                }
                return currentNode.next;
            }

            // Reset current unit to that of this node.
            currentUnit = (currentNode.isOnCarrier()) ? carrier : unit;

            // Only check further along a path (i.e. ignore initial
            // node) if it is possible to transit *through* it
            // (isProgress()).
            if (currentNode.previous != null) {
                Tile previousTile = currentNode.previous.getTile();
                if (!currentUnit.getSimpleMoveType(previousTile, currentTile,
                                                   false).isProgress()) {
                    continue;
                }
            }

            // Try the tiles in each direction
            for (Direction direction : Direction.values()) {
                final Tile newTile = currentTile.getNeighbourOrNull(direction);
                if (newTile == null) continue;

                // If the new tile is the tile we just visited, skip
                // it. We can use == because PathNode.getTile() and
                // getNeighborOrNull both return references to the
                // actual Tile in tiles[][].
                if (currentNode.previous != null
                    && currentNode.previous.getTile() == newTile) {
                    continue;
                }
                if (closedList.containsKey(newTile.getId())) {
                    continue;
                }

                // Collect the parameters for the current node.
                int cost = currentNode.getCost();
                int movesLeft = currentNode.getMovesLeft();
                int turns = currentNode.getTurns();
                boolean onCarrier = currentNode.isOnCarrier();
                Unit moveUnit;

                // Check for disembarkation on new tile, setting
                // moveUnit to the unit that would actually move.
                if (carrier != null
                    && onCarrier
                    && newTile.isLand()
                    && (newTile.getSettlement() == null
                        || newTile.getSettlement().getOwner() == currentUnit.getOwner())) {
                    moveUnit = unit;
                    movesLeft = unit.getInitialMovesLeft();
                } else {
                    moveUnit = (onCarrier) ? carrier : unit;
                }

                // Update parameters for the new tile.
                int extraCost = costDecider.getCost(moveUnit,
                                                    currentTile, newTile,
                                                    movesLeft, turns);
                if (extraCost == CostDecider.ILLEGAL_MOVE) {
                    // Do not let the CostDecider (which may be
                    // conservative) block the final destination if it
                    // is still a legal move.
                    if (newTile == end
                        && moveUnit.getSimpleMoveType(currentTile, newTile,
                                                      false).isLegal()) {
                        cost += moveUnit.getInitialMovesLeft();
                        movesLeft = 0;
                    } else {
                        continue;
                    }
                } else {
                    cost += extraCost;
                    movesLeft = costDecider.getMovesLeft();
                    if (costDecider.isNewTurn()) turns++;
                }

                // Is this an improvement?  If not, ignore.
                final int f = cost + newTile.getDistanceTo(end);
                PathNode successor = openList.get(newTile.getId());
                if (successor != null) {
                    if (successor.getF() <= f) {
                        continue;
                    }
                    openList.remove(successor.getTile().getId());
                    openListQueue.remove(successor);
                }

                // Queue new node with updated parameters.
                successor = new PathNode(newTile, cost, f, direction,
                                         movesLeft, turns);
                successor.previous = currentNode;
                successor.setOnCarrier(carrier != null && moveUnit == carrier);
                openList.put(newTile.getId(), successor);
                openListQueue.offer(successor);
            }
        }

        return null;
    }

    /**
     * Finds a path to a goal determined by the given <code>GoalDecider</code>.
     *
     * <br />
     * <br />
     *
     * A <code>GoalDecider</code> is typically defined inline to serve a
     * specific need.
     *
     * @param unit
     *            The <code>Unit</code> to find the path for.
     * @param gd
     *            The object responsible for determining whether a given
     *            <code>PathNode</code> is a goal or not.
     * @param maxTurns
     *            The maximum number of turns the given <code>Unit</code> is
     *            allowed to move. This is the maximum search range for a goal.
     * @return The path to a goal determined by the given
     *         <code>GoalDecider</code>.
     */
    public PathNode search(Unit unit, GoalDecider gd, int maxTurns) {
        return search(unit, unit.getTile(), gd, CostDeciders.defaultFor(unit), maxTurns);
    }

    /**
     * Finds a path to a goal determined by the given <code>GoalDecider</code>.
     *
     * <br />
     * <br />
     *
     * A <code>GoalDecider</code> is typically defined inline to serve a
     * specific need.
     *
     * @param unit
     *            The <code>Unit</code> to find the path for.
     * @param startTile
     *            The <code>Tile</code> to start the search from.
     * @param gd
     *            The object responsible for determining whether a given
     *            <code>PathNode</code> is a goal or not.
     * @param maxTurns
     *            The maximum number of turns the given <code>Unit</code> is
     *            allowed to move. This is the maximum search range for a goal.
     * @return The path to a goal determined by the given
     *         <code>GoalDecider</code>.
     */
    public PathNode search(Unit unit, Tile startTile, GoalDecider gd,
            int maxTurns) {
        return search(unit, startTile, gd, CostDeciders.defaultFor(unit), maxTurns);
    }

    /**
     * Finds a path to a goal determined by the given <code>GoalDecider</code>.
     *
     * <br />
     * <br />
     *
     * A <code>GoalDecider</code> is typically defined inline to serve a
     * specific need.
     *
     * @param unit
     *            The <code>Unit</code> to find the path for.
     * @param gd
     *            The object responsible for determining wether a given
     *            <code>PathNode</code> is a goal or not.
     * @param maxTurns
     *            The maximum number of turns the given <code>Unit</code> is
     *            allowed to move. This is the maximum search range for a goal.
     * @param carrier
     *            The carrier the <code>unit</code> is currently onboard or
     *            <code>null</code> if the <code>unit</code> is either not
     *            onboard a carrier or should not use the carrier while finding
     *            the path.
     * @return The path to a goal determined by the given
     *         <code>GoalDecider</code>.
     */
    public PathNode search(Unit unit, GoalDecider gd,
            int maxTurns, Unit carrier) {
        return search(unit, unit.getTile(), gd, CostDeciders.defaultFor(unit), maxTurns, carrier);
    }

    /**
     * Finds a path to a goal determined by the given <code>GoalDecider</code>.
     *
     * <br />
     * <br />
     *
     * A <code>GoalDecider</code> is typically defined inline to serve a
     * specific need.
     *
     * @param startTile
     *            The <code>Tile</code> to start the search from.
     * @param gd
     *            The object responsible for determining whether a given
     *            <code>PathNode</code> is a goal or not.
     * @param costDecider
     *            The object responsible for determining the cost.
     * @param maxTurns
     *            The maximum number of turns the given <code>Unit</code> is
     *            allowed to move. This is the maximum search range for a goal.
     * @return The path to a goal determined by the given
     *         <code>GoalDecider</code>.
     */
    public PathNode search(Tile startTile, GoalDecider gd,
            CostDecider costDecider, int maxTurns) {
        return search(null, startTile, gd, costDecider, maxTurns);
    }

    /**
     * Finds a path to a goal determined by the given <code>GoalDecider</code>.
     *
     * <br />
     * <br />
     *
     * A <code>GoalDecider</code> is typically defined inline to serve a
     * specific need.
     *
     * @param unit
     *            The <code>Unit</code> to find the path for.
     * @param startTile
     *            The <code>Tile</code> to start the search from.
     * @param gd
     *            The object responsible for determining whether a given
     *            <code>PathNode</code> is a goal or not.
     * @param costDecider
     *            The object responsible for determining the cost.
     * @param maxTurns
     *            The maximum number of turns the given <code>Unit</code> is
     *            allowed to move. This is the maximum search range for a goal.
     * @return The path to a goal determined by the given
     *         <code>GoalDecider</code>.
     */
    public PathNode search(Unit unit, Tile startTile, GoalDecider gd,
            CostDecider costDecider, int maxTurns) {
        return search(unit, startTile, gd, costDecider, maxTurns, null);
    }

    /**
     * Finds a path to a goal determined by the given <code>GoalDecider</code>.
     *
     * <br />
     * <br />
     *
     * A <code>GoalDecider</code> is typically defined inline to serve a
     * specific need.
     *
     * @param unit an <code>Unit</code> value
     * @param startTile
     *            The <code>Tile</code> to start the search from.
     * @param gd
     *            The object responsible for determining whether a given
     *            <code>PathNode</code> is a goal or not.
     * @param maxTurns
     *            The maximum number of turns the given <code>Unit</code> is
     *            allowed to move. This is the maximum search range for a goal.
     * @param carrier
     *            The carrier the <code>unit</code> is currently onboard or
     *            <code>null</code> if the <code>unit</code> is either not
     *            onboard a carrier or should not use the carrier while finding
     *            the path.
     * @return The path to a goal determined by the given
     *         <code>GoalDecider</code>.
     */
    public PathNode search(final Unit unit, final Tile startTile,
            final GoalDecider gd, final int maxTurns,
            final Unit carrier) {
        return search(unit, startTile, gd, CostDeciders.defaultFor(unit), maxTurns, carrier);
    }

    /**
     * Finds a path to a goal determined by the given <code>GoalDecider</code>.
     *
     * <br />
     * <br />
     *
     * A <code>GoalDecider</code> is typically defined inline to serve a
     * specific need.
     *
     * @param unit an <code>Unit</code> value
     * @param startTile
     *            The <code>Tile</code> to start the search from.
     * @param gd
     *            The object responsible for determining whether a given
     *            <code>PathNode</code> is a goal or not.
     * @param costDecider
     *            The object responsible for determining the cost.
     * @param maxTurns
     *            The maximum number of turns the given <code>Unit</code> is
     *            allowed to move. This is the maximum search range for a goal.
     * @param carrier
     *            The carrier the <code>unit</code> is currently onboard or
     *            <code>null</code> if the <code>unit</code> is either not
     *            onboard a carrier or should not use the carrier while finding
     *            the path.
     * @return The path to a goal determined by the given
     *         <code>GoalDecider</code>.
     */
    public PathNode search(final Unit unit, final Tile startTile,
            final GoalDecider gd, final CostDecider costDecider,
            final int maxTurns, final Unit carrier) {
        /*
         * Using Dijkstra's algorithm with a closedList for marking the visited
         * nodes and using a PriorityQueue for getting the next edge with the
         * least cost. This implementation could be improved by having the
         * visited attribute stored on each Tile in order to avoid both of the
         * HashMaps currently being used to serve this purpose.
         */

        if (startTile == null) {
            throw new IllegalArgumentException("startTile must not be 'null'.");
        }

        // What unit starts the path?
        Unit currentUnit = (carrier != null) ? carrier : unit;

        final HashMap<String, PathNode> openList
            = new HashMap<String, PathNode>();
        final HashMap<String, PathNode> closedList
            = new HashMap<String, PathNode>();
        final PriorityQueue<PathNode> openListQueue
            = new PriorityQueue<PathNode>(1024,
                new Comparator<PathNode>() {
                    public int compare(PathNode o, PathNode p) {
                        return o.getCost() - p.getCost();
                    }
                });
        final PathNode firstNode
            = new PathNode(startTile, 0, 0, Direction.N,
                           (currentUnit != null) ? currentUnit.getMovesLeft() : -1,
                           0);
        firstNode.setOnCarrier(carrier != null);
        openList.put(startTile.getId(), firstNode);
        openListQueue.offer(firstNode);

        while (!openList.isEmpty()) {
            // Choose the node with the lowest cost.
            final PathNode currentNode = openListQueue.poll();
            final Tile currentTile = currentNode.getTile();
            openList.remove(currentTile.getId());
            closedList.put(currentTile.getId(), currentNode);

            // Reset current unit to that of this node.
            currentUnit = (currentNode.isOnCarrier()) ? carrier : unit;

            // Check for simple success.
            if (gd.check(currentUnit, currentNode) && !gd.hasSubGoals()) {
                break;
            }

            // Stop if reached the turn limit.
            if (currentNode.getTurns() > maxTurns) {
                break;
            }

            // Only check further along a path (i.e. ignore initial
            // node) if it is possible to transit *through* it
            // (isProgress()).
            if (currentUnit != null
                && currentNode.previous != null) {
                Tile previousTile = currentNode.previous.getTile();
                if (!currentUnit.getSimpleMoveType(previousTile, currentTile,
                                                   false).isProgress()) {
                    continue;
                }
            }

            // Try the tiles in each direction
            for (Direction direction : Direction.values()) {
                final Tile newTile = currentTile.getNeighbourOrNull(direction);
                if (newTile == null) {
                    continue;
                }

                // If the new tile is the tile we just visited, skip
                // it. We can use == because PathNode.getTile() and
                // getNeighborOrNull both return references to the
                // actual Tile in tiles[][].
                if (currentNode.previous != null
                    && currentNode.previous.getTile() == newTile) {
                    continue;
                }
                if (closedList.containsKey(newTile.getId())) {
                    continue;
                }

                // Collect the parameters for the current node.
                int cost = currentNode.getCost();
                int movesLeft = currentNode.getMovesLeft();
                int turns = currentNode.getTurns();
                boolean onCarrier = currentNode.isOnCarrier();
                Unit moveUnit;

                // Check for disembarkation on new tile, setting
                // moveUnit to the unit that would actually move.
                if (carrier != null
                    && onCarrier
                    && newTile.isLand()
                    && (newTile.getSettlement() == null
                        || newTile.getSettlement().getOwner() == currentUnit.getOwner())) {
                    moveUnit = unit;
                    movesLeft = moveUnit.getInitialMovesLeft();
                } else {
                    moveUnit = (onCarrier) ? carrier : unit;
                }

                // Update parameters for the new tile.
                int extraCost = costDecider.getCost(moveUnit,
                        currentTile, newTile, movesLeft, turns);
                if (extraCost == CostDecider.ILLEGAL_MOVE) continue;
                cost += extraCost;
                movesLeft = costDecider.getMovesLeft();
                if (costDecider.isNewTurn()) {
                    turns++;
                }

                // Is this an improvement?  If not, ignore.
                PathNode successor = openList.get(newTile.getId());
                if (successor != null) {
                    if (successor.getCost() <= cost) {
                        continue;
                    }
                    openList.remove(successor.getTile().getId());
                    openListQueue.remove(successor);
                }

                // Queue new node with updated parameters.
                successor = new PathNode(newTile, cost, cost, direction,
                                         movesLeft, turns);
                successor.previous = currentNode;
                successor.setOnCarrier(carrier != null && moveUnit == carrier);
                openList.put(newTile.getId(), successor);
                openListQueue.offer(successor);
            }
        }

        PathNode bestTarget = gd.getGoal();
        if (bestTarget != null) {
            while (bestTarget.previous != null) {
                bestTarget.previous.next = bestTarget;
                bestTarget = bestTarget.previous;
            }
            return bestTarget.next;
        }
        return null;
    }

    /**
     * Finds the best path to <code>Europe</code>.
     *
     * @param unit
     *            The <code>Unit</code> that should be used to determine
     *            whether or not a path is legal.
     * @param start
     *            The starting <code>Tile</code>.
     * @return The path to the target or <code>null</code> if no target can be
     *         found.
     * @see Europe
     */
    public PathNode findPathToEurope(Unit unit, Tile start) {
        return findPathToEurope(unit, start, CostDeciders.defaultFor(unit));
    }

    /**
     * Finds the best path to <code>Europe</code>.
     *
     * @param unit
     *            The <code>Unit</code> that should be used to determine
     *            whether or not a path is legal.
     * @param start
     *            The starting <code>Tile</code>.
     * @return The path to the target or <code>null</code> if no target can be
     *         found.
     * @see Europe
     */
    public PathNode findPathToEurope(Unit unit, Tile start, CostDecider costDecider) {
        GoalDecider gd = new GoalDecider() {
            private PathNode goal = null;

            public PathNode getGoal() {
                return goal;
            }

            public boolean hasSubGoals() {
                return false;
            }

            public boolean check(Unit u, PathNode pathNode) {

                if (pathNode.getTile().canMoveToEurope()) {
                    goal = pathNode;
                    return true;
                }

                //TODO: This may make invalid assumptions about map topology!
                //Solution: Add booleans, defining which edges are considered
                //  connected to europe
                //or make sure during map generation that high seas tiles
                //  exist in all sensible spots, then remove this check.
                if (pathNode.getTile().isAdjacentToVerticalMapEdge()) {
                    goal = pathNode;
                    return true;
                }
                return false;
            }
        };
        return search(unit, start, gd, costDecider, INFINITY);
    }

    /**
     * Finds the best path to <code>Europe</code> independently of any unit.
     * This method is meant to be executed by the server/AI code, with complete knowledge of the map
     *
     * @param start
     *            The starting <code>Tile</code>.
     * @return The path to the target or <code>null</code> if no target can be
     *         found.
     * @see Europe
     */
    public PathNode findPathToEurope(Tile start) {
        final GoalDecider gd = new GoalDecider() {
            private PathNode goal = null;

            public PathNode getGoal() {
                return goal;
            }

            public boolean hasSubGoals() {
                return false;
            }

            public boolean check(Unit u, PathNode pathNode) {
                Tile t = pathNode.getTile();
                if (t.canMoveToEurope()) {
                    goal = pathNode;
                    return true;
                }

                //TODO: This may make invalid assumptions about map topology!
                //Solution: Add booleans, defining which edges are considered
                //  connected to europe
                //or make sure during map generation that high seas tiles
                //  exist in all sensible spots, then remove this check.
                if (t.isAdjacentToVerticalMapEdge()) {
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
        return search(start, gd, cd, INFINITY);
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
     * @param position The position
     * @param width The width of the map.
     * @param height The height of the map.
     * @return <code>true</code> if the given position is
     *        within the bounds of the map and <code>false</code> otherwise
     */
    public static boolean isValid(Position position, int width, int height) {
        return isValid(position.x, position.y, width, height);
    }

    /**
     * Checks if the given position is valid.
     *
     * @param x The x-coordinate of the position.
     * @param y The y-coordinate of the position.
     * @param width The width of the map.
     * @param height The height of the map.
     * @return <code>true</code> if the given position is
     *        within the bounds of the map and <code>false</code> otherwise
     */
    public static boolean isValid(int x, int y, int width, int height) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    /**
     * Select a random land position on the map.
     *
     * <b>Warning:</b> This method should not be used by any model
     * object unless we have completed restructuring the model
     * (making all model changes at the server). The reason is
     * the use of random numbers in this method.
     *
     * @param random A <code>Random</code> number source.
     * @return Position selected
     */
    public Position getRandomLandPosition(Random random) {
        int x = (getWidth() > 10) ? random.nextInt(getWidth() - 10) + 5
            : random.nextInt(getWidth());
        int y = (getHeight() > 10) ? random.nextInt(getHeight() - 10) + 5
            : random.nextInt(getHeight());
        Position centerPosition = new Position(x, y);
        Iterator<Position> it = getFloodFillIterator(centerPosition);
        while (it.hasNext()) {
            Position p = it.next();
            if (getTile(p).isLand()) {
                return p;
            }
        }
        return null;
    }

    /**
     * Represents a position on the Map.
     */
    public static final class Position {
        public final int x, y;

        /**
         * Creates a new object with the given position.
         *
         * @param posX
         *            The x-coordinate for this position.
         * @param posY
         *            The y-coordinate for this position.
         */
        public Position(int posX, int posY) {
            x = posX;
            y = posY;
        }

        /**
         * Returns the x-coordinate of this Position.
         *
         * @return The x-coordinate of this Position.
         */
        public int getX() {
            return x;
        }

        /**
         * Returns the y-coordinate of this Position.
         *
         * @return The y-coordinate of this Position.
         */
        public int getY() {
            return y;
        }

        /**
         * Compares the other Position based on the coordinates.
         *
         * @param other the reference object with which to compare.
         * @return true iff the coordinates match.
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
                return x == ((Position) other).x && y == ((Position) other).y;
            }
        }

        /**
         * Returns a hash code value. The current implementation (which may
         * change at any time) works well as long as the maximum coordinates fit
         * in 16 bits.
         *
         * @return a hash code value for this object.
         */
        @Override
        public int hashCode() {
            return x | (y << 16);
        }

        /**
         * Returns a string representation of the object.
         *
         * @return a string representation of the object.
         */
        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }

        /**
         * Gets the position adjacent to a given position, in a given
         * direction.
         *
         * @param position The position
         * @param direction The direction (N, NE, E, etc.)
         * @return Adjacent position
         */
         public Position getAdjacent(Direction direction) {
             int x = this.x + ((this.y & 1) != 0 ?
                                   direction.getOddDX() : direction.getEvenDX());
             int y = this.y + ((this.y & 1) != 0 ?
                                   direction.getOddDY() : direction.getEvenDY());
             return new Position(x, y);
    }

        /**
         * Gets the distance in tiles between two map positions. With an isometric
         * map this is a non-trivial task. The formula below has been developed
         * largely through trial and error. It should cover all cases, but I
         * wouldn't bet my life on it.
         *
         * @param position
         *            The second position.
         * @return Distance
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

        protected Direction[] directions = Direction.values();

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
            for (int i = x; i < 8; i++) {
                Position newPosition = basePosition.getAdjacent(directions[i]);
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
            for (int i = x; i < 8; i++) {
                Position newPosition = basePosition.getAdjacent(directions[i]);
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
                throw new IllegalArgumentException("center must not be 'null'.");
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
            for (int i = index; i < 8; i += 2) {
                Position newPosition = basePosition.getAdjacent(directions[i]);
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
            for (int i = index; i < 8; i += 2) {
                Position newPosition = basePosition.getAdjacent(directions[i]);
                if (isValid(newPosition)) {
                    index = i + 2;
                    return newPosition;
                }
            }
            throw new NoSuchElementException("Iterator exhausted");
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
            boolean showAll, boolean toSavedGame) throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute("ID", getId());
        out.writeAttribute("width", Integer.toString(getWidth()));
        out.writeAttribute("height", Integer.toString(getHeight()));

        for (Region region : regions.values()) {
            region.toXML(out);
        }

        for (Tile tile: getAllTiles()) {
            if (showAll || player.hasExplored(tile)) {
                tile.toXML(out, player, showAll, toSavedGame);
            } else {
                tile.toXMLMinimal(out);
            }
        }

        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in
     *            The input stream with the XML.
     */
    @Override
    protected void readFromXMLImpl(XMLStreamReader in)
            throws XMLStreamException {
        setId(in.getAttributeValue(null, "ID"));

        if (tiles == null) {
            int width = Integer.parseInt(in.getAttributeValue(null, "width"));
            int height = Integer.parseInt(in.getAttributeValue(null, "height"));

            tiles = new Tile[width][height];
        }

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals(Tile.getXMLElementTagName())) {
                Tile t = updateFreeColGameObject(in, Tile.class);
                setTile(t, t.getX(), t.getY());
            } else if (in.getLocalName().equals(Region.getXMLElementTagName())) {
                setRegion(updateFreeColGameObject(in, Region.class));
            } else {
                logger.warning("Unknown tag: " + in.getLocalName() + " loading map");
                in.nextTag();
            }
        }
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return the tag name.
     */
    public static String getXMLElementTagName() {
        return "map";
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
}
