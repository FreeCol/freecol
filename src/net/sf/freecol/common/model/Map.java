package net.sf.freecol.common.model;

import java.util.*;
import java.util.logging.Logger;
import javax.xml.stream.*;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.PseudoRandom;
import org.w3c.dom.Element;

/**
 * An isometric map. The map is represented as a collection of tiles.
 */
public class Map extends FreeColGameObject {
    
    public static final String COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String REVISION = "$Revision$";

    private static final Logger logger = Logger.getLogger(Map.class.getName());

    /** The possible sizes for a Map. */
    public static final int SMALL = 0, MEDIUM = 1, LARGE = 2, HUGE = 3,
            CUSTOM = 4;

    /** The directions a Unit can move to. */
    public static final int N = 0, NE = 1, E = 2, SE = 3, S = 4, SW = 5, W = 6,
            NW = 7, NUMBER_OF_DIRECTIONS = 8;

    /** The infinity cost as used by {@link #findPath(Unit, Tile, Tile)}. */
    public static final int COST_INFINITY = Integer.MAX_VALUE - 100000000;

    /** Constant used for given options in {@link #findPath(Unit, Tile, Tile)}. */
    public static final int BOTH_LAND_AND_SEA = 0, ONLY_LAND = 1, ONLY_SEA = 2;

    // Deltas for moving to adjacent squares. Different due to the
    // isometric map. Starting north and going clockwise.
    private static final int[] ODD_DX = { 0, 1, 1, 1, 0, 0, -1, 0 };
    private static final int[] ODD_DY = { -2, -1, 0, 1, 2, 1, 0, -1 };
    private static final int[] EVEN_DX = { 0, 0, 1, 0, 0, -1, -1, -1 };
    private static final int[] EVEN_DY = { -2, -1, 0, 1, 2, 1, 0, -1 };

    /**
     * This Vector contains a set of other Vectors that can be considered
     * columns. Those columns contain a set of Tiles.
     */
    private Vector<Vector<Tile>> columns = null;

    private final DefaultCostDecider defaultCostDecider = new DefaultCostDecider();
    private int width;
    private int height;
    

    /**
     * Create a new <code>Map</code> of a specified size.
     * 
     * @param game
     *            The <code>Game</code> this map belongs to.
     * @param size
     *            The size of the map to construct, should be one of {SMALL,
     *            MEDIUM, LARGE, HUGE}.
     * @exception FreeColException
     *                If thrown during the creation of the map.
     */
    public Map(Game game, int size) throws FreeColException {
        super(game);

        createColumns(size);
        initSize();
    }

    /**
     * Create a new <code>Map</code> from a collection of tiles.
     * 
     * @param game
     *            The <code>Game</code> this map belongs to.
     * @param columns
     *            This <code>Vector</code> contains the rows, that contains
     *            the tiles.
     */

    public Map(Game game, Vector<Vector<Tile>> columns) {
        super(game);

        this.columns = columns;
        initSize();
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
        initSize();
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
        initSize();        
    }

    private void initSize() {
      width = columns.size();
      height = ((Vector) columns.get(0)).size();;
    }

    /**
     * Returns the opposite direction of the given direction.
     * 
     * @param direction
     *            The direction
     * @return The oppositre direction of the given one.
     * @deprecated use the static method {@link #getReverseDirection(int)}
     */
    public int getOppositeDirection(int direction) {
        return (direction + 4 < 8) ? direction + 4 : direction - 4;
    }

    /**
     * Finds a shortest path between the given tiles. The <code>Tile</code> at
     * the <code>end</code> will not be checked against the
     * <code>options</code>.
     * 
     * <br>
     * <br>
     * 
     * <i>Important: This method will not include colonies in a possible path,
     * and {@link PathNode#getTurns}, {@link PathNode#getTotalTurns} and
     * {@link PathNode#getMovesLeft} will all return <code>-1</code> for the
     * generated {@link PathNode}s.
     * 
     * <br>
     * <br>
     * 
     * Use {@link #findPath(Unit, Tile, Tile)} whenever possible.</i>
     * 
     * @param start
     *            The <code>Tile</code> in which the path starts from.
     * @param end
     *            The end of the path.
     * @param type
     *            One of: {@link #BOTH_LAND_AND_SEA},
     *            {@link #BOTH_LAND_AND_SEA}, {@link #ONLY_LAND} and
     *            {@link #ONLY_SEA}.
     * @return A <code>PathNode</code> for the first tile in the path. Calling
     *         {@link PathNode#getTile} on this object, will return the
     *         <code>Tile</code> right after the specified starting tile, and
     *         {@link PathNode#getDirection} will return the direction you need
     *         to take in order to reach that tile. This method returns
     *         <code>null</code> if no path is found.
     * @see #findPath(Unit, Tile, Tile)
     * @see Unit#findPath(Tile)
     * @exception NullPointerException
     *                if either <code>start</code> or <code>end</code> are
     *                <code>null</code>.
     */
    public PathNode findPath(Tile start, Tile end, int type) {
        return findPath(null, start, end, type);
    }

    /**
     * Finds a shortest path between the given tiles. The <code>Tile</code> at
     * the <code>end</code> will not be checked against the <code>unit</code>'s
     * legal moves.
     * 
     * @param unit
     *            The <code>Unit</code> that should be used to determine
     *            wether or not a path is legal.
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
     * @see #findPath(Tile, Tile, int)
     * @see Unit#findPath(Tile)
     * @exception NullPointerException
     *                if either <code>unit</code>, <code>start</code> or
     *                <code>end</code> are <code>null</code>.
     */
    public PathNode findPath(Unit unit, Tile start, Tile end) {
        if (unit == null) {
            throw new NullPointerException();
        }
        return findPath(unit, start, end, -1, null);
    }

    /**
     * Finds a shortest path between the given tiles. The <code>Tile</code> at
     * the <code>end</code> will not be checked against the <code>unit</code>'s
     * legal moves.
     * 
     * @param unit
     *            The <code>Unit</code> that should be used to determine
     *            wether or not a path is legal.
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
     * @see #findPath(Tile, Tile, int)
     * @see Unit#findPath(Tile)
     * @exception NullPointerException
     *                if either <code>unit</code>, <code>start</code> or
     *                <code>end</code> are <code>null</code>.
     */
    public PathNode findPath(Unit unit, Tile start, Tile end, Unit carrier) {
        if (unit == null) {
            throw new NullPointerException();
        }
        return findPath(unit, start, end, -1, carrier);
    }

    /**
     * Finds a shortest path between the given tiles. The <code>Tile</code> at
     * the <code>end</code> will not be checked against validity (neither the
     * <code>options</code> nor allowed movement by the <code>unit</code>.
     * 
     * @param unit
     *            The <code>Unit</code> that should be used to determine
     *            wether or not a path is legal. The <code>options</code> are
     *            used instead if <code>unit == null</code>.
     * @param start
     *            The <code>Tile</code> in which the path starts from.
     * @param end
     *            The end of the path.
     * @param type
     *            One of: {@link #BOTH_LAND_AND_SEA},
     *            {@link #BOTH_LAND_AND_SEA}, {@link #ONLY_LAND} and
     *            {@link #ONLY_SEA}. This argument if ignored if
     *            <code>unit != null</code>.
     * @return A <code>PathNode</code> for the first tile in the path. Calling
     *         {@link PathNode#getTile} on this object, will return the
     *         <code>Tile</code> right after the specified starting tile, and
     *         {@link PathNode#getDirection} will return the direction you need
     *         to take in order to reach that tile. This method returns
     *         <code>null</code> if no path is found.
     * @exception NullPointerException
     *                if either <code>start</code> or <code>end</code> are
     *                <code>null</code>.
     * @exception IllegalArgumentException
     *                if <code>start == end</code>.
     */
    private PathNode findPath(Unit unit, Tile start, Tile end, int type) {
        return findPath(unit, start, end, type, null);
    }

    /**
     * Finds a shortest path between the given tiles. The <code>Tile</code> at
     * the <code>end</code> will not be checked against validity (neither the
     * <code>options</code> nor allowed movement by the <code>unit</code>.
     * 
     * @param unit
     *            The <code>Unit</code> that should be used to determine
     *            wether or not a path is legal. The <code>options</code> are
     *            used instead if <code>unit == null</code>.
     * @param start
     *            The <code>Tile</code> in which the path starts from.
     * @param end
     *            The end of the path.
     * @param type
     *            One of: {@link #BOTH_LAND_AND_SEA},
     *            {@link #BOTH_LAND_AND_SEA}, {@link #ONLY_LAND} and
     *            {@link #ONLY_SEA}. This argument if ignored if
     *            <code>unit != null</code>.
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
     * @exception NullPointerException
     *                if either <code>start</code> or <code>end</code> are
     *                <code>null</code>.
     * @exception IllegalArgumentException
     *                if <code>start == end</code>.
     */
    private PathNode findPath(Unit unit, Tile start, final Tile end, int type,
            Unit carrier) {
        /*
         * Using A* with the Manhatten distace as the heuristics.
         * 
         * The datastructure for the open list is a combined structure: using a
         * HashMap for membership tests and a PriorityQueue for getting the node
         * with the minimal f (cost+heuristics). This gives O(1) on membership
         * test and O(log N) for remove-best and insertions.
         * 
         * The datastructure for the closed list is simply a HashMap.
         */

        if (start == end) {
            throw new IllegalArgumentException("start == end");
        }
        if (start == null) {
            throw new NullPointerException("start == null");
        }
        if (end == null) {
            throw new NullPointerException("end == null");
        }
        if (carrier != null && unit == null) {
            throw new NullPointerException("unit == null");
        }

        final Unit theUnit = unit;
        if (carrier != null) {
            unit = carrier;
        }

        PathNode firstNode;
        if (unit != null) {
            firstNode = new PathNode(start, 0, getDistance(start.getPosition(),
                    end.getPosition()), -1, unit.getMovesLeft(), 0);
            firstNode.setOnCarrier(carrier != null);
        } else {
            firstNode = new PathNode(start, 0, getDistance(start.getPosition(),
                    end.getPosition()), -1, -1, -1);
        }

        final HashMap<String, PathNode> openList = new HashMap<String, PathNode>();
        final PriorityQueue<PathNode> openListQueue = new PriorityQueue<PathNode>(
                1024, new Comparator<PathNode>() {
                    public int compare(PathNode o, PathNode p) {
                        int i = o.getF() - p.getF();
                        if (i != 0) {
                            return i;
                        } else {
                            i = o.getTile().getX() - p.getTile().getX();
                            if (i != 0) {
                                return i;
                            } else {
                                return o.getTile().getY() - p.getTile().getY();
                            }
                        }
                    }
                });
        final HashMap<String, PathNode> closedList = new HashMap<String, PathNode>();

        openList.put(firstNode.getTile().getID(), firstNode);
        openListQueue.offer(firstNode);

        while (openList.size() > 0) {
            // Choosing the node with the lowest f:
            PathNode currentNode = openListQueue.peek();

            // Found our goal:
            if (currentNode.getTile() == end) {
                while (currentNode.previous != null) {
                    currentNode.previous.next = currentNode;
                    currentNode = currentNode.previous;
                }
                return currentNode.next;
            }

            if (currentNode.isOnCarrier()) {
                unit = carrier;
            } else {
                unit = theUnit;
            }

            // Try every direction:
            for (int direction = 0; direction < 8; direction++) {
                Tile newTile = getNeighbourOrNull(direction, currentNode
                        .getTile());

                if (newTile == null) {
                    continue;
                }

                int cost = currentNode.getCost();
                int movesLeft = currentNode.getMovesLeft();
                int turns = currentNode.getTurns();

                if (unit != null) {
                    int extraCost = defaultCostDecider.getCost(unit,
                            currentNode.getTile(), newTile, movesLeft, turns);
                    if (extraCost == CostDecider.ILLEGAL_MOVE && newTile != end) {
                        continue;
                    }
                    if (extraCost == CostDecider.ILLEGAL_MOVE) {
                        if (newTile == end) {
                            cost += unit.getInitialMovesLeft();
                            movesLeft = 0;
                        }
                    } else {
                        cost += extraCost;
                        movesLeft = defaultCostDecider.getMovesLeft();
                        if (defaultCostDecider.isNewTurn()) {
                            turns++;
                        }
                    }
                } else {
                    if ((type == ONLY_SEA && newTile.isLand() || type == ONLY_LAND
                            && !newTile.isLand())
                            && newTile != end) {
                        continue;
                    } else {
                        cost += newTile.getMoveCost(currentNode.getTile());
                    }
                }

                // Disembark from 'carrier':
                if (carrier != null
                        && newTile.isLand()
                        && unit.isNaval()
                        && (newTile.getSettlement() == null || newTile
                                .getSettlement().getOwner() == unit.getOwner())
                        && newTile != end) {
                    int mc = newTile.getMoveCost(currentNode.getTile());
                    if (theUnit.getInitialMovesLeft() < carrier
                            .getInitialMovesLeft()) {
                        mc *= (carrier.getInitialMovesLeft() - theUnit
                                .getInitialMovesLeft());
                    }
                    cost = mc;
                    movesLeft = Math.max(0, theUnit.getInitialMovesLeft() - mc);
                }

                int f = cost
                        + getDistance(newTile.getPosition(), end.getPosition());

                // Finding the node on the open list:
                PathNode successor = openList.get(newTile.getID());

                if (successor != null) {
                    if (successor.getF() <= f) {
                        continue;
                    } else {
                        openList.remove(successor.getTile().getID());
                        openListQueue.remove(successor);
                    }
                } else {
                    // Finding the node on the closed list.
                    successor = closedList.get(newTile.getID());
                    if (successor != null) {
                        if (successor.getF() <= f) {
                            continue;
                        } else {
                            closedList.remove(newTile.getID());
                        }
                    }
                }

                successor = new PathNode(newTile, cost, f, direction,
                        movesLeft, turns);
                successor.previous = currentNode;
                successor.setOnCarrier(currentNode.isOnCarrier());

                // Disembark from 'carrier':
                if (carrier != null
                        && newTile.isLand()
                        && unit == carrier
                        && (newTile.getSettlement() == null || newTile
                                .getSettlement().getOwner() != unit.getOwner())
                        && newTile != end) {
                    successor.setOnCarrier(false);
                }

                // Adding the new node to the open list:
                openList.put(successor.getTile().getID(), successor);
                openListQueue.offer(successor);
            }

            closedList.put(currentNode.getTile().getID(), currentNode);

            // Removing the current node from the open list:
            openList.remove(currentNode.getTile().getID());
            openListQueue.remove(currentNode);
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
     *            The object responsible for determining wether a given
     *            <code>PathNode</code> is a goal or not.
     * @param maxTurns
     *            The maximum number of turns the given <code>Unit</code> is
     *            allowed to move. This is the maximum search range for a goal.
     * @return The path to a goal determined by the given
     *         <code>GoalDecider</code>.
     */
    public PathNode search(Unit unit, GoalDecider gd, int maxTurns) {
        return search(unit, unit.getTile(), gd, defaultCostDecider, maxTurns);
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
     *            The object responsible for determining wether a given
     *            <code>PathNode</code> is a goal or not.
     * @param maxTurns
     *            The maximum number of turns the given <code>Unit</code> is
     *            allowed to move. This is the maximum search range for a goal.
     * @return The path to a goal determined by the given
     *         <code>GoalDecider</code>.
     */
    public PathNode search(Unit unit, Tile startTile, GoalDecider gd,
            int maxTurns) {
        return search(unit, startTile, gd, defaultCostDecider, maxTurns);
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
     *            The object responsible for determining wether a given
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
     *            The object responsible for determining wether a given
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
     * @param unit
     *            The <code>Unit</code> to find the path for.
     * @param startTile
     *            The <code>Tile</code> to start the search from.
     * @param gd
     *            The object responsible for determining wether a given
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
    public PathNode search(Unit unit, Tile startTile, GoalDecider gd,
            CostDecider costDecider, int maxTurns, Unit carrier) {
        /*
         * Using Dijkstra's algorithm with a closedList for marking the visited
         * nodes and using a PriorityQueue for getting the next edge with the
         * least cost. This implementation could be improved by having the
         * visited attribute stored on each Tile in order to avoid both of the
         * HashMaps currently being used to serve this purpose.
         */

        if (startTile == null) {
            throw new NullPointerException("startTile == null");
        }

        Unit theUnit = unit;
        if (carrier != null) {
            unit = carrier;
        }
        int ml = (unit != null) ? unit.getMovesLeft() : -1;
        PathNode firstNode = new PathNode(startTile, 0, 0, -1, ml, 0);
        firstNode.setOnCarrier(carrier != null);

        final HashMap<String, PathNode> openList = new HashMap<String, PathNode>();
        final PriorityQueue<PathNode> openListQueue = new PriorityQueue<PathNode>(
                1024, new Comparator<PathNode>() {
                    public int compare(PathNode o, PathNode p) {
                        int i = o.getCost() - p.getCost();
                        if (i != 0) {
                            return i;
                        } else {
                            i = o.getTile().getX() - p.getTile().getX();
                            if (i != 0) {
                                return i;
                            } else {
                                return o.getTile().getY() - p.getTile().getY();
                            }
                        }
                    }
                });
        final HashMap<String, PathNode> closedList = new HashMap<String, PathNode>();

        openList.put(startTile.getID(), firstNode);
        openListQueue.offer(firstNode);

        while (openList.size() > 0) {
            // Choosing the node with the lowest cost:
            PathNode currentNode = openListQueue.poll();
            openList.remove(currentNode.getTile().getID());
            closedList.put(currentNode.getTile().getID(), currentNode);

            // Reached the end
            if (currentNode.getTurns() > maxTurns) {
                break;
            }

            if (currentNode.isOnCarrier()) {
                unit = carrier;
            } else {
                unit = theUnit;
            }

            if (gd.check(unit, currentNode) && !gd.hasSubGoals()) {
                PathNode bestTarget = gd.getGoal();
                if (bestTarget != null) {
                    while (bestTarget.previous != null) {
                        bestTarget.previous.next = bestTarget;
                        bestTarget = bestTarget.previous;
                    }
                    return bestTarget.next;
                } else {
                    logger.warning("The returned goal is null.");
                    return null;
                }
            }

            // Try every direction:
            int[] directions = getDirectionArray();
            for (int j = 0; j < directions.length; j++) {
                int direction = directions[j];

                Tile newTile = getNeighbourOrNull(direction, currentNode
                        .getTile());

                if (newTile == null) {
                    continue;
                }

                int cost = currentNode.getCost();
                int movesLeft = currentNode.getMovesLeft();
                int turns = currentNode.getTurns();

                if (currentNode.isOnCarrier() && newTile.isLand()) {
                    unit = theUnit;
                    movesLeft = unit.getInitialMovesLeft();
                }

                int extraCost = costDecider.getCost(unit,
                        currentNode.getTile(), newTile, movesLeft, turns);
                if (extraCost == CostDecider.ILLEGAL_MOVE) {
                    continue;
                }
                if (carrier != null && unit == theUnit) {
                    cost += extraCost
                            * (1 + (carrier.getInitialMovesLeft() / ((double) theUnit
                                    .getInitialMovesLeft())));
                } else {
                    cost += extraCost;
                }
                movesLeft = costDecider.getMovesLeft();
                if (costDecider.isNewTurn()) {
                    turns++;
                }

                // Finding the node on the open list:
                PathNode successor = closedList.get(newTile.getID());
                if (successor != null) {
                    if (successor.getCost() <= cost) {
                        continue;
                    } else {
                        logger.warning("This should not happen. :-(");
                    }
                } else {
                    successor = openList.get(newTile.getID());
                    if (successor != null) {
                        if (successor.getCost() <= cost) {
                            continue;
                        } else {
                            openList.remove(successor.getTile().getID());
                            openListQueue.remove(successor);
                        }
                    }
                    successor = new PathNode(newTile, cost, cost, direction,
                            movesLeft, turns);
                    successor.previous = currentNode;
                    successor.setOnCarrier(currentNode.isOnCarrier()
                            && unit != theUnit);
                    openList.put(successor.getTile().getID(), successor);
                    openListQueue.offer(successor);
                }
            }
        }

        PathNode bestTarget = gd.getGoal();
        if (bestTarget != null) {
            while (bestTarget.previous != null) {
                bestTarget.previous.next = bestTarget;
                bestTarget = bestTarget.previous;
            }
            return bestTarget.next;
        } else {
            return null;
        }
    }

    /**
     * Gets the default <code>CostDecider</code>.
     * 
     * @return The default <code>CostDecider</code>. This is currently a
     *         singleton instance of {@link DefaultCostDecider}.
     */
    public CostDecider getDefaultCostDecider() {
        return defaultCostDecider;
    }

    /**
     * Checks if the given <code>Tile</code> is adjacent to the edge of the
     * map.
     * 
     * @param tile
     *            The <code>Tile</code> to be checked.
     * @return <code>true</code> if the given tile is at the edge of the map.
     */
    public boolean isAdjacentToMapEdge(Tile tile) {
        for (int direction = 0; direction < Map.NUMBER_OF_DIRECTIONS; direction++) {
            if (getNeighbourOrNull(direction, tile) == null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds the best path to <code>Europe</code>.
     * 
     * @param unit
     *            The <code>Unit</code> that should be used to determine
     *            wether or not a path is legal.
     * @param start
     *            The starting <code>Tile</code>.
     * @return The path to the target or <code>null</code> if no target can be
     *         found.
     * @see Europe
     */
    public PathNode findPathToEurope(Unit unit, Tile start) {
        GoalDecider gd = new GoalDecider() {
            private PathNode goal = null;

            public PathNode getGoal() {
                return goal;
            }

            public boolean hasSubGoals() {
                return false;
            }

            public boolean check(Unit u, PathNode pathNode) {
                Map map = u.getGame().getMap();

                if (pathNode.getTile().getType() == Tile.HIGH_SEAS) {
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
        return search(unit, start, gd, defaultCostDecider, Integer.MAX_VALUE);
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
     * Creates the columns contains the rows that contains the tiles.
     * 
     * @exception FreeColException
     *                If the given size is invalid.
     */
    private void createColumns(int size) throws FreeColException {
        int width, height;

        switch (size) {
        case SMALL:
            width = 30;
            height = 64;
            break;
        case MEDIUM:
            width = 60;
            height = 128;
            break;
        case LARGE:
            width = 120;
            height = 256;
            break;
        case HUGE:
            width = 240;
            height = 512;
            break;
        default:
            throw new FreeColException("Invalid map-size: " + size + ".");
        }

        createColumns(width, height);
    }

    /**
     * Creates the columns contains the rows that contains the tiles.
     */
    private void createColumns(int width, int height) {
        columns = new Vector<Vector<Tile>>(width);
        for (int i = 0; i < width; i++) {
            Vector<Tile> v = new Vector<Tile>(height);
            for (int j = 0; j < height; j++) {
                Tile t = new Tile(getGame(), i, j);
                v.add(t);
            }
            columns.add(v);
        }
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
        if ((x >= 0) && (x < getWidth()) && (y >= 0) && (y < getHeight())) {
            return columns.get(x).get(y);
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
        columns.get(x).set(y, tile);
    }

    /**
     * Returns the width of this Map.
     * 
     * @return The width of this Map.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the height of this Map.
     * 
     * @return The height of this Map.
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns the neighbouring Tile of the given Tile in the given direction.
     * 
     * @param direction
     *            The direction in which the neighbour is located given t.
     * @param t
     *            The Tile to get a neighbour of.
     * @return The neighbouring Tile of the given Tile in the given direction.
     */
    public Tile getNeighbourOrNull(int direction, Tile t) {
        return getNeighbourOrNull(direction, t.getX(), t.getY());
    }

    /**
     * Returns the neighbouring Tile of the given Tile in the given direction.
     * 
     * @param direction
     *            The direction in which the neighbour is located given the base
     *            tile.
     * @param x
     *            The base tile X coordinate.
     * @param y
     *            The base tile Y coordinate.
     * @return The neighbouring Tile of the given coordinate in the given
     *         direction or null if invalid.
     */
    public Tile getNeighbourOrNull(int direction, int x, int y) {
        if (isValid(x, y)) {
            Position pos = getAdjacent(new Position(x, y), direction);
            return getTileOrNull(pos.getX(), pos.getY());
        } else {
            return null;
        }
    }

    /**
     * Get a tile or null if it does not exist.
     * 
     * @param x
     *            The x position.
     * @param y
     *            The y position.
     * @return tile if position is valid, null otherwise.
     */
    public Tile getTileOrNull(int x, int y) {
        return isValid(x, y) ? getTile(x, y) : null;
    }

    /**
     * Get a tile or null if it does not exist.
     * 
     * @param position
     *            The position of the tile.
     * @return tile if position is valid, null otherwise.
     */
    public Tile getTileOrNull(Position position) {
        return isValid(position) ? getTile(position) : null;
    }

    /**
     * Returns all the tiles surrounding the given tile within the given range.
     * 
     * @param t
     *            The tile that lies on the center of the tiles to return.
     * @param range
     *            How far away do we need to go starting from the center tile.
     * @return The tiles surrounding the given tile.
     */
    public Vector<Tile> getSurroundingTiles(Tile t, int range) {
        Vector<Tile> result = new Vector<Tile>();
        Position tilePosition = new Position(t.getX(), t.getY());
        Iterator<Position> i = (range == 1) ? getAdjacentIterator(tilePosition)
                : getCircleIterator(tilePosition, true, range);

        while (i.hasNext()) {
            Position p = i.next();
            if (!p.equals(tilePosition)) {
                result.add(getTile(p));
            }
        }

        return result;
    }

    /**
     * Creates an array of the eight directions in a random order.
     * 
     * @return The array.
     */
    public int[] getRandomDirectionArray() {
        PseudoRandom random = getGame().getModelController().getPseudoRandom();

        int[] directions = getDirectionArray();
        for (int i = 0; i < directions.length; i++) {
            int i2 = random.nextInt(directions.length);
            if (i2 != i) {
                int temp = directions[i2];
                directions[i2] = directions[i];
                directions[i] = temp;
            }
        }

        return directions;
    }

    /**
     * Get an array of the eight directions in deterministic order.
     * 
     * @return array with directions.
     */
    private int[] getDirectionArray() {
        int[] directions = new int[8];
        for (int i = 0; i < directions.length; i++) {
            directions[i] = i;
        }
        return directions;
    }

    /**
     * Returns the reverse direction of the given direction.
     * 
     * @param direction
     *            The direction to get the reverse of.
     * @return The reverse direction of the given direction.
     */
    public static int getReverseDirection(int direction) {
        switch (direction) {
        case N:
            return S;
        case NE:
            return SW;
        case E:
            return W;
        case SE:
            return NW;
        case S:
            return N;
        case SW:
            return NE;
        case W:
            return E;
        case NW:
            return SE;
        default:
            throw new IllegalArgumentException("Invalid direction received.");
        }
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
     * Get the position adjacent to a given position, in a given direction.
     * 
     * @param position
     *            The position
     * @param direction
     *            The direction (N, NE, E, etc.)
     * @return Adjacent position
     */
    public Position getAdjacent(Position position, int direction) {
        int x = position.getX()
                + ((position.getY() & 1) != 0 ? ODD_DX[direction]
                        : EVEN_DX[direction]);
        int y = position.getY()
                + ((position.getY() & 1) != 0 ? ODD_DY[direction]
                        : EVEN_DY[direction]);
        return new Position(x, y);
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
        return new CircleIterator(centerPosition, true, Integer.MAX_VALUE);
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
        return isValid(position.getX(), position.getY());
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
        return ((x >= 0) && (x < getWidth()) && (y >= 0) && (y < getHeight()));
    }

    /**
     * Gets the distance in tiles between two map positions. With an isometric
     * map this is a non-trivial task. The formula below has been developed
     * largely through trial and error. It should cover all cases, but I
     * wouldn't bet my life on it.
     * 
     * @param position1
     *            The first position.
     * @param position2
     *            The second position.
     * @return Distance
     */
    public int getDistance(Position position1, Position position2) {
        return getDistance(position1.getX(), position1.getY(),
                position2.getX(), position2.getY());
    }

    /**
     * Gets the distance in tiles between two map positions.
     * 
     * @param ax
     *            Position A x-coordinate
     * @param ay
     *            Position A y-coordinate
     * @param bx
     *            Position B x-coordinate
     * @param by
     *            Position B y-coordinate
     * @return Distance
     */
    public int getDistance(int ax, int ay, int bx, int by) {
        int r = bx - ax - (ay - by) / 2;

        if (by > ay && ay % 2 == 0 && by % 2 != 0) {
            r++;
        } else if (by < ay && ay % 2 != 0 && by % 2 == 0) {
            r--;
        }

        return Math.max(Math.abs(ay - by + r), Math.abs(r));
    }

    /**
     * Represents a position on the Map.
     */
    public static final class Position {
        private final int x, y;

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
            if (this == other)
                return true;
            if (other == null)
                return false;
            if (!(other instanceof Position))
                return false;
            return x == ((Position) other).x && y == ((Position) other).y;
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

    private final class WholeMapIterator extends MapIterator {
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
                Position newPosition = getAdjacent(basePosition, i);
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
                Position newPosition = getAdjacent(basePosition, i);
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
                throw new NullPointerException("center == null");
            }

            n = 0;

            if (isFilled || radius == 1) {
                nextPosition = getAdjacent(center, NE);
                currentRadius = 1;
            } else {
                currentRadius = radius;
                nextPosition = center;
                for (int i = 1; i < radius; i++) {
                    nextPosition = getAdjacent(nextPosition, N);
                }
                nextPosition = getAdjacent(nextPosition, NE);
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
                        nextPosition = getAdjacent(nextPosition, NE);
                    }
                } else {
                    int i = n / width;
                    int direction;
                    switch (i) {
                    case 0:
                        direction = SE;
                        break;
                    case 1:
                        direction = SW;
                        break;
                    case 2:
                        direction = NW;
                        break;
                    case 3:
                        direction = NE;
                        break;
                    default:
                        throw new IllegalStateException("i=" + i + ", n=" + n
                                + ", width=" + width);
                    }
                    nextPosition = getAdjacent(nextPosition, direction);
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
         * @return The next position. This position is guaratied to be
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
                Position newPosition = getAdjacent(basePosition, i);
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
                Position newPosition = getAdjacent(basePosition, i);
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

        out.writeAttribute("ID", getID());
        out.writeAttribute("width", Integer.toString(getWidth()));
        out.writeAttribute("height", Integer.toString(getHeight()));

        Iterator<Position> tileIterator = getWholeMapIterator();
        while (tileIterator.hasNext()) {
            Tile tile = getTile(tileIterator.next());

            if (showAll || player.hasExplored(tile)) {
                tile.toXML(out, player, showAll, toSavedGame);
            } else {
                Tile hiddenTile = new Tile(getGame(), tile.getX(), tile.getY());
                hiddenTile.setFakeID(tile.getID());
                hiddenTile.toXML(out, player, showAll, toSavedGame);
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
        setID(in.getAttributeValue(null, "ID"));

        if (columns == null) {
            int width = Integer.parseInt(in.getAttributeValue(null, "width"));
            int height = Integer.parseInt(in.getAttributeValue(null, "height"));

            // createColumns(width, height);
            columns = new Vector<Vector<Tile>>(width);
            for (int i = 0; i < width; i++) {
                Vector<Tile> v = new Vector<Tile>(height);
                for (int j = 0; j < height; j++) {
                    v.add(null);
                }
                columns.add(v);
            }
        }

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals(Tile.getXMLElementTagName())) {
                Tile t = (Tile) getGame().getFreeColGameObject(
                        in.getAttributeValue(null, "ID"));
                int x = Integer.parseInt(in.getAttributeValue(null, "x"));
                int y = Integer.parseInt(in.getAttributeValue(null, "y"));

                if (t != null) {
                    t.readFromXML(in);
                } else {
                    t = new Tile(getGame(), in);
                }
                setTile(t, x, y);
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
