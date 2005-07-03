
package net.sf.freecol.server.ai.mission;

import net.sf.freecol.server.ai.*;
import net.sf.freecol.common.model.*;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.Connection;

import java.util.*;
import java.util.logging.Logger;
import java.io.IOException;

import org.w3c.dom.*;


/**
* A mission describes what a unit should do; attack, build colony, wander etc.
* Every {@link AIUnit} should have a mission. By extending this class,
* you create different missions.
*/
public abstract class Mission extends AIObject {
    private static final Logger logger = Logger.getLogger(Mission.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    protected int NO_PATH_TO_TARGET = -2,
                  NO_MORE_MOVES_LEFT = -1;

    private AIUnit aiUnit;


    /**
    * Creates a mission.
    * @param aiMain The main AI-object.
    */
    public Mission(AIMain aiMain) {
        this(aiMain, null);
    }
    

    /**
    * Creates a mission for the given <code>AIUnit</code>.
    *
    * @param aiMain The main AI-object.
    * @param aiUnit The <code>AIUnit</code> this mission
    *        is created for.
    */
    public Mission(AIMain aiMain, AIUnit aiUnit) {
        super(aiMain);
        this.aiUnit = aiUnit;
    }

    
    /**
    * Moves the unit owning this mission towards the given <code>Tile</code>.
    * This is done in a loop until the tile is reached, there are no moves left,
    * the path to the target cannot be found or that the next step is not a move.
    *
    * @param tile The <code>Tile</code> the unit should move towards.
    * @return The direction to take the final move (greater than or equal to zero),
    *         or {@link #NO_MORE_MOVES_LEFT} if there are no more moves left and
    *         {@link #NO_PATH_TO_TARGET} if there is no path to follow.
    *         If a direction is returned, it is guarantied that moving in that direction
    *         is not an {@link Unit#ILLEGAL_MOVE}.
    */
    protected int moveTowards(Connection connection, Tile tile) {
        Map map = getAIMain().getGame().getMap();
        PathNode pathNode = getUnit().findPath(tile);
        if (pathNode != null) {
            int direction = pathNode.getDirection();
            int turns = pathNode.getTurns();
            while (pathNode != null && pathNode.getTile() != tile && pathNode.getTurns() == turns && getUnit().getMoveType(direction) == Unit.MOVE) {
                move(connection, direction);
                pathNode = pathNode.next;
            }
            if (pathNode.getTile() == tile && pathNode.getTurns() == turns && getUnit().getMoveType(direction) != Unit.ILLEGAL_MOVE) {
                return pathNode.getDirection();
            }
            return NO_MORE_MOVES_LEFT;
        } else {
            return NO_PATH_TO_TARGET;
        }
    }
    
    
    /**
    * Moves the unit owning this mission using the given <code>pathNode</code>.
    * This is done in a loop until the end of the path is reached, the next step is not a move
    * or when there are no moves left.
    *
    * @param pathNode The first node of the path.
    * @return The direction to take the final move (greater than or equal to zero),
    *         or {@link #NO_MORE_MOVES_LEFT} if there are no more moves left.
    *         If a direction is returned, it is guarantied that moving in that direction
    *         is not an {@link Unit#ILLEGAL_MOVE}.
    */
    protected int moveTowards(Connection connection, PathNode pathNode) {
        int direction = pathNode.getDirection();
        int turns = pathNode.getTurns();
        while (pathNode != null && pathNode.getTurns() == turns && getUnit().getMoveType(direction) == Unit.MOVE) {
            move(connection, direction);
            pathNode = pathNode.next;
        }
        if (pathNode.getTurns() == turns && getUnit().getMoveType(direction) != Unit.ILLEGAL_MOVE) {
            return pathNode.getDirection();
        }
        return NO_MORE_MOVES_LEFT;
    }


    /**
    * Moves the unit owning this mission in the given direction.
    */
    protected void move(Connection connection, int direction) {
        Element moveElement = Message.createNewRootElement("move");
        moveElement.setAttribute("unit", getUnit().getID());
        moveElement.setAttribute("direction", Integer.toString(direction));

        try {
            connection.send(moveElement);
        } catch (IOException e) {
            logger.warning("Could not send \"move\"-message!");
        }
    }
    
    
    /**
    * Finds the best target to attack within the given range.
    *
    * @param maxTurns The maximum number of turns the unit is allowed
    *                 to spend in order to reach the target.
    * @return The path to the target or <code>null</code> if no target can
    *         be found.
    */
    protected PathNode findTarget(int maxTurns) {
        // This is just a temporary implementation; modify at will ;-)

        if (!getUnit().isOffensiveUnit()) {
            throw new IllegalStateException("A target can only be found for offensive units. You tried with: " + getUnit().getName());
        }

        PathNode bestTarget = null;

        Unit unit = getUnit();
        PathNode firstNode = new PathNode(unit.getTile(), 0, 0, -1, unit.getMovesLeft(), 0);

        // TODO: Choose a better datastructur:
        ArrayList openList = new ArrayList();
        ArrayList closedList = new ArrayList();

        openList.add(firstNode);

        while (openList.size() > 0) {
            // TODO: Better method for choosing the node with the lowest f:
            PathNode currentNode = (PathNode) openList.get(0);
            for (int i=1; i<openList.size(); i++) {
                if (currentNode.compareTo(openList.get(i)) < 0) {
                    currentNode = (PathNode) openList.get(i);
                }
            }

            // Reached the end
            if (currentNode.getTurns() > maxTurns) {
                break;
            }

            // Try every direction:
            int[] directions = getGame().getMap().getRandomDirectionArray();
            for (int j=0; j<8; j++) {
                int direction = directions[j];

                Tile newTile = getGame().getMap().getNeighbourOrNull(direction, currentNode.getTile());

                if (newTile == null) {
                    continue;
                }

                int cost = currentNode.getCost();
                int movesLeft = currentNode.getMovesLeft();
                int turns = currentNode.getTurns();

                if (newTile.isLand() && unit.isNaval()) {
                    if ((newTile.getSettlement() == null || newTile.getSettlement().getOwner() != unit.getOwner())) {
                        // Not allowed to move a naval unit on land:
                        continue;
                    } else {
                        // Entering a settlement costs all of the remaining moves for a naval unit:
                        cost += movesLeft;
                        movesLeft = 0;
                    }
                } else if ((!newTile.isLand() && !unit.isNaval())) {
                    // Not allowed to move a land unit on water:
                    continue;
                } else {
                    int mc = newTile.getMoveCost(currentNode.getTile());
                    if (mc - 2 <= movesLeft) {
                        // Normal move: Using -2 in order to make 1/3 and 2/3 move count as 3/3.
                        movesLeft -= mc;
                        if (movesLeft < 0) {
                            mc += movesLeft;
                            movesLeft = 0;
                        }
                        cost += mc;
                    } else if (movesLeft == unit.getInitialMovesLeft()) {
                        // Entering a terrain with a higher move cost, but no moves have been spent yet.
                        cost += movesLeft;
                        movesLeft = 0;
                    } else {
                        // This move takes an extra turn to complete:
                        turns++;
                        if (mc > unit.getInitialMovesLeft()) {
                            // Entering a terrain with a higher move cost than the initial moves:
                            cost += movesLeft + unit.getInitialMovesLeft();
                            movesLeft = 0;
                        } else {
                            // Normal move:
                            cost += movesLeft + mc;
                            movesLeft = unit.getInitialMovesLeft() - mc;
                        }
                    }
                }


                int f = cost; // + getDistance(newTile.getPosition(), end.getPosition());

                PathNode successor = null;
                // TODO: Better method for finding the node on the open list:
                int i;
                for (i=0; i<openList.size(); i++) {
                    if (((PathNode) openList.get(i)).getTile() == newTile) {
                        successor = (PathNode) openList.get(i);
                        break;
                    }
                }

                if (successor != null) {
                    if (successor.getF() <= f) {
                        continue;
                    } else {
                        openList.remove(i);
                    }
                } else {
                    // TODO: Better method for finding the node on the closed list:
                    for (i=0; i<closedList.size(); i++) {
                        if (((PathNode) closedList.get(i)).getTile() == newTile) {
                            successor = (PathNode) closedList.get(i);
                            break;
                        }
                    }
                    if (successor != null) {
                        if (successor.getF() <= f) {
                            continue;
                        } else {
                            closedList.remove(i);
                        }
                    }
                }

                successor = new PathNode(newTile, cost, f, direction, movesLeft, turns);
                successor.previous = currentNode;

                if ((newTile.isLand() && !unit.isNaval() || !newTile.isLand() && unit.isNaval()) &&
                        newTile.getDefendingUnit(unit) != null && newTile.getDefendingUnit(unit).getOwner() != unit.getOwner()) {

                    int tension = unit.getOwner().getTension(newTile.getDefendingUnit(unit).getOwner());
                    if (unit.getIndianSettlement() != null) {
                        tension += unit.getIndianSettlement().getAlarm(newTile.getDefendingUnit(unit).getOwner());
                    }
                    if (newTile.getDefendingUnit(unit).getType() == Unit.TREASURE_TRAIN) {
                        tension += Math.min(newTile.getDefendingUnit(unit).getTreasureAmount() / 10, 600);
                    }
                    if (newTile.getDefendingUnit(unit).getType() == Unit.ARTILLERY && newTile.getSettlement() == null) {
                        tension += 100 - newTile.getDefendingUnit(unit).getDefensePower(unit) * 2;
                    }
                    if (newTile.getDefendingUnit(unit).getType() == Unit.VETERAN_SOLDIER && !newTile.getDefendingUnit(unit).isArmed()) {
                        tension += 50 - newTile.getDefendingUnit(unit).getDefensePower(unit) * 2;
                    }
                    if (tension > Player.TENSION_CONTENT) {
                        if (bestTarget == null) {
                            bestTarget = successor;                           
                        } else if (bestTarget.getTurns() == successor.getTurns()) {
                            // TODO: Check if the new target is better than the previous:
                        }
                    }
                } else {
                    openList.add(successor);
                }
            }

            closedList.add(currentNode);

            // TODO: Better method for removing the node on the open list:
            for (int i=0; i<openList.size(); i++) {
                if (((PathNode) openList.get(i)) == currentNode) {
                    openList.remove(i);
                    break;
                }
            }
        }

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
    * Performs the mission. This method should be implemented by a subclass.
    * @param connection The <code>Connection</code> to the server.
    */
    public abstract void doMission(Connection connection);


    /**
    * Checks if this mission is still valid to perform.
    *
    * <BR><BR>
    *
    * A mission can be invalidated for a number of reasons. For example:
    * a seek-and-destroy mission can be invalidated in case the
    * relationship towards the targeted player improves.
    */
    public boolean isValid() {
        return true;
    }


    /**
    * Gets the unit this mission has been created for.
    */
    public Unit getUnit() {
        return aiUnit.getUnit();
    }


    /**
    * Gets the AI-unit this mission has been created for.
    */
    public AIUnit getAIUnit() {
        return aiUnit;
    }
    
    
    protected void setAIUnit(AIUnit aiUnit) {
        this.aiUnit = aiUnit;
    }
}
