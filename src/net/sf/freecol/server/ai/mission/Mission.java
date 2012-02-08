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

package net.sf.freecol.server.ai.mission;

import java.util.Random;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.server.ai.AIColony;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIObject;
import net.sf.freecol.server.ai.AIUnit;


/**
 * A mission describes what a unit should do; attack, build colony,
 * wander etc.  Every {@link AIUnit} should have a mission. By
 * extending this class, you create different missions.
 */
public abstract class Mission extends AIObject {

    private static final Logger logger = Logger.getLogger(Mission.class.getName());

    protected static final int MINIMUM_TRANSPORT_PRIORITY = 60,     // A transport can be used
                               NORMAL_TRANSPORT_PRIORITY = 100;     // Transport is required

    protected static final int NO_PATH_TO_TARGET = -2,
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
     * @exception NullPointerException if <code>aiUnit == null</code>.
     */
    public Mission(AIMain aiMain, AIUnit aiUnit) {
        super(aiMain);
        this.aiUnit = aiUnit;
    }

    /**
     * Disposes this mission by removing any references to it.
     */
    public void dispose() {
        // Nothing to do yet.
    }

    /**
     * Gets the unit this mission has been created for.
     * @return The <code>Unit</code>.
     */
    public Unit getUnit() {
        return aiUnit.getUnit();
    }

    /**
     * Gets the AI-unit this mission has been created for.
     * @return The <code>AIUnit</code>.
     */
    public AIUnit getAIUnit() {
        return aiUnit;
    }

    /**
     * Sets the AI-unit this mission has been created for.
     * @param aiUnit The <code>AIUnit</code>.
     */
    protected void setAIUnit(AIUnit aiUnit) {
        this.aiUnit = aiUnit;
    }

    /**
     * Convenience accessor for the unit/player PRNG.
     *
     * @return A <code>Random</code> to use.
     */
    public Random getAIRandom() {
        return aiUnit.getAIRandom();
    }

    /**
     * Moves the unit owning this mission towards the given
     * <code>Tile</code>.  This is done in a loop until the tile is
     * reached, there are no moves left, the path to the target cannot
     * be found or that the next step is not a move.
     *
     * @param tile The <code>Tile</code> the unit should move towards.
     * @return The direction to take the final move or null if the
     *     move can not be made.
     */
    protected Direction moveTowards(Tile tile) {
        PathNode pathNode = getUnit().findPath(tile);
        return (pathNode == null) ? null : moveTowards(pathNode);
    }

    /**
     * Moves the unit owning this mission using the given path.  This
     * is done in a loop until the end of the path is reached, the
     * next step is not a move or when there are no moves left.
     *
     * @param pathNode The first node of the path.
     * @return The direction to continue moving the path or null if the
     *     move can not be made.
     */
    protected Direction moveTowards(PathNode pathNode) {
        if (getUnit().getMovesLeft() <= 0) return null;

        while (pathNode.next != null && pathNode.getTurns() == 0) {
            if (!isValid()) return null;
            if (!getUnit().getMoveType(pathNode.getDirection()).isProgress()) {
                break;
            }
            if (!AIMessage.askMove(aiUnit, pathNode.getDirection())
                || getUnit() == null || getUnit().isDisposed()) {
                return null;
            }
            pathNode = pathNode.next;
        }
        return (pathNode.getTurns() == 0
                && getUnit().getMoveType(pathNode.getDirection()).isLegal())
            ? pathNode.getDirection()
            : null;
    }

    /**
     * Move a unit randomly.
     */
    protected void moveRandomly() {
        Unit unit = getUnit();
        Direction[] randomDirections
            = Direction.getRandomDirections("moveRandomly", getAIRandom());

        while (isValid() && unit.getMovesLeft() > 0) {
            Tile thisTile = getUnit().getTile();
            for (int j = 0; j < randomDirections.length; j++) {
                Direction direction = randomDirections[j];
                if (thisTile.getNeighbourOrNull(direction) != null
                    && unit.getMoveType(direction) == MoveType.MOVE) {
                    AIMessage.askMove(aiUnit, direction);
                    break;
                }
            }
            unit.setMovesLeft(0);
        }
    }


    protected void moveUnitToAmerica() {
        AIMessage.askMoveTo(aiUnit, getUnit().getOwner().getGame().getMap());
    }

    protected void moveUnitToEurope() {
        AIMessage.askMoveTo(aiUnit, getUnit().getOwner().getEurope());
    }


    /**
     * Move in a specified direction, but do not attack.
     * Always check the return from this in case the unit blundered into
     * a lost city and died.
     * The usual idiom is: "if (!moveButDontAttack(unit)) return;"
     *
     * @param direction The <code>Direction</code> to move.
     * @return True if the unit doing this mission is still valid/alive.
     */
    protected boolean moveButDontAttack(Direction direction) {
        final Unit unit = getUnit();
        if (direction != null
            && unit != null
            && unit.getMoveType(direction).isProgress()) {
            AIMessage.askMove(aiUnit, direction);
        }
        return getUnit() != null && !getUnit().isDisposed();
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
        if (!getUnit().isOffensiveUnit()) {
            throw new IllegalStateException("A target can only be found for offensive units. You tried with: "
                                            + getUnit().toString());
        }

        GoalDecider gd = new GoalDecider() {
            private PathNode bestTarget = null;
            private int higherTension = 0;

            public PathNode getGoal() {
                return bestTarget;
            }

            public boolean hasSubGoals() {
                return true;
            }

            public boolean check(Unit unit, PathNode pathNode) {
                CombatModel combatModel = getGame().getCombatModel();
                Tile newTile = pathNode.getTile();
                Unit defender = newTile.getDefendingUnit(unit);

                if( defender == null){
                    return false;
                }

                if( defender.getOwner() == unit.getOwner()){
                    return false;
                }

                if (newTile.isLand() && unit.isNaval() || !newTile.isLand() && !unit.isNaval()) {
                    return false;
                }

                int tension = unit.getOwner().getTension(defender.getOwner()).getValue();
                if (unit.getIndianSettlement() != null &&
                        unit.getIndianSettlement().hasContactedSettlement(defender.getOwner())) {
                    tension += unit.getIndianSettlement().getAlarm(defender.getOwner()).getValue();
                }
                if (defender.canCarryTreasure()) {
                    tension += Math.min(defender.getTreasureAmount() / 10, 600);
                }
                if (defender.getType().getDefence() > 0 &&
                        newTile.getSettlement() == null) {
                    tension += 100 - combatModel.getDefencePower(unit, defender) * 2;
                }
                if (defender.hasAbility(Ability.EXPERT_SOLDIER) &&
                        !defender.isArmed()) {
                    tension += 50 - combatModel.getDefencePower(unit, defender) * 2;
                }
                if (unit.hasAbility(Ability.PIRACY)){
                    tension += PrivateerMission.getModifierValueForTarget(combatModel, unit, defender);
                }
                // TODO-AI-CHEATING: REMOVE WHEN THE AI KNOWNS HOW TO HANDLE PEACE WITH THE INDIANS:
                if (unit.getOwner().isIndian()
                        && defender.getOwner().isAI()) {
                    tension -= 200;
                }
                // END: TODO-AI-CHEATING
                if (tension > Tension.Level.CONTENT.getLimit()) {
                    if (bestTarget == null) {
                        bestTarget = pathNode;
                        higherTension = tension;
                        return true;
                    } else if (bestTarget.getTurns() == pathNode.getTurns()
                            && tension > higherTension) {
                        bestTarget = pathNode;
                        higherTension = tension;
                        return true;
                    }
                }
                return false;
            }
        };
        return getUnit().search(getUnit().getTile(), gd,
                                CostDeciders.avoidIllegal(), maxTurns, null);
    }

    /**
     * Find the nearest reachable settlement to a unit (owned by the
     * same player) excepting the any on the current tile.
     *
     * @param unit The <code>Unit</code> to check.
     * @return The nearest settlement if any, otherwise null.
     */
    protected PathNode findNearestOtherSettlement(Unit unit) {
        Player player = unit.getOwner();
        PathNode nearest = null;
        int dist = Integer.MAX_VALUE;
        for (Settlement settlement : player.getSettlements()) {
            if (settlement.getTile() == unit.getTile()) return null;
            PathNode path = unit.findPath(settlement.getTile());
            if (path != null) {
                int d = path.getTotalTurns();
                if (d < dist) {
                    nearest = path;
                    dist = d;
                }
            }
        }
        return nearest;
    }

    /**
     * Unload a unit.
     *
     * @param aiUnit The <code>AIUnit</code> to unload.
     * @return True if the unit is unloaded.
     */
    protected boolean unitLeavesShip(AIUnit aiUnit) {
        Colony colony = aiUnit.getUnit().getColony();
        if (colony != null) {
            AIColony ac = getAIMain().getAIColony(colony);
            if (ac != null) ac.completeWish(aiUnit.getUnit());

            colony.firePropertyChange(Colony.REARRANGE_WORKERS, true, false);
        }
        return AIMessage.askDisembark(aiUnit);
    }

    /**
     * Unload some goods in a colony.
     *
     * @param goods The <code>Goods</code> to unload.
     * @return True if the goods are unloaded.
     */
    protected boolean unloadCargoInColony(Goods goods) {
        Colony colony = aiUnit.getUnit().getColony();
        AIColony ac;
        if (colony != null
            && (ac = getAIMain().getAIColony(colony)) != null) {
            ac.completeWish(goods);
        }
        return AIMessage.askUnloadCargo(aiUnit, goods);
    }

    /**
     * Sell some goods in Europe.
     *
     * @param goods The <code>Goods</code> to sell.
     * @return True if the goods are sold.
     */
    protected boolean sellCargoInEurope(Goods goods) {
        // CHEAT: Remove when the AI is good enough
        Player p = getUnit().getOwner();
        if (p.isAI() && getAIMain().getFreeColServer().isSingleplayer()) {
            // Double the income by adding this bonus:
            p.modifyGold(p.getMarket().getSalePrice(goods));
        }
        return AIMessage.askSellGoods(aiUnit, goods);
    }

    /**
     * Should the unit use transport to get to a specified tile?
     *
     * True if:
     * - The unit is not there already
     * AND
     * - the unit is already has transport, this will always be faster
     *   (TODO: actually, mounted units on good roads might be faster,
     *   check for this)
     * - if not on the map
     * - if on the map but can not find a path to the tile, unless
     *   adjacent to the destination which usually means the path
     *   finding failed due to a temporary blockage such as an enemty unit
     *
     * @param tile The <code>Tile</code> to go to.
     * @return True if the unit should use transport.
     */
    protected boolean shouldTakeTransportToTile(Tile tile) {
        final Unit unit = getUnit();
        return tile != null
            && unit.getTile() != tile
            && (unit.isOnCarrier()
                || unit.getTile() == null
                || (unit.findPath(tile) == null
                    && !unit.getTile().isAdjacent(tile)));
    }

    /**
     * Gets a suitable tile to start path searches from for a unit.
     *
     * Must handle all the cases where the unit is off the map, and
     * take account of the use of a carrier.
     *
     * If the unit is in or heading to Europe, return null because there
     * is no good way to tell where the unit will reappear on the map,
     * which is in question anyway if not on a carrier.
     *
     * @param unit The <code>Unit</code> to check.
     * @return A suitable starting tile, or null if none found.
     */
    protected static Tile getPathStartTile(Unit unit) {
        Location loc;
        if (unit.isOnCarrier()) {
            final Unit carrier = (Unit)unit.getLocation();
            if (carrier.getTile() != null) {
                return carrier.getTile();
            } else if (carrier.getDestination() instanceof Map) {
                return carrier.getFullEntryLocation();
            } else if (carrier.getDestination() instanceof Colony) {
                return ((Colony)carrier.getDestination()).getTile();
            }
        } else if (unit.getTile() != null) {
            return unit.getTile();
        }
        // Must be heading to or in Europe.
        return null;
    }

    // Fake implementation of Transportable interface.
    // Missions are not actually Transportables but the units that are
    // performing a mission delegate to these routines.

    /**
     * Gets the transport destination of the unit associated with this
     * mission.
     *
     * @return The destination of a required transport or
     *         <code>null</code> if no transport is needed.
     */
    public Location getTransportDestination() {
        final Unit unit = getUnit();
        if (unit.getTile() == null) {
            return ((unit.isOnCarrier()) ? ((Unit)unit.getLocation()) : unit)
                .getFullEntryLocation();
        }
        if (!unit.isOnCarrier()) return null;
        final Unit carrier = (Unit)unit.getLocation();
        if (carrier.getSettlement() != null) return carrier.getTile();

        // Find the closest friendly Settlement:
        GoalDecider gd = new GoalDecider() {
            private PathNode bestTarget = null;

            public PathNode getGoal() {
                return bestTarget;
            }

            public boolean hasSubGoals() {
                return false;
            }

            public boolean check(Unit unit, PathNode pathNode) {
                Tile newTile = pathNode.getTile();
                boolean hasOurSettlement = (newTile.getSettlement() != null)
                    && newTile.getSettlement().getOwner() == unit.getOwner();
                if (hasOurSettlement) bestTarget = pathNode;
                return hasOurSettlement;
            }
        };
        PathNode path = carrier.search(carrier.getTile(), gd,
            CostDeciders.avoidSettlementsAndBlockingUnits(),
            Integer.MAX_VALUE, null);
        return (path == null) ? null : path.getLastNode().getTile();
    }

    /**
     * Gets the priority of getting the unit to the transport
     * destination.
     *
     * @return The priority.
     */
    public int getTransportPriority() {
        return (getTransportDestination() != null)
            ? NORMAL_TRANSPORT_PRIORITY
            : 0;
    }


    // Mission interface to be implemented/overridden by descendants.

    /**
     * Checks if this mission is still valid to perform.  At this
     * level, if the unit was killed then the mission becomes invalid.
     *
     * A mission can be invalidated for a number of subclass-specific
     * reasons.  For example: a seek-and-destroy mission could be
     * invalidated when the relationship towards the targeted player
     * improves.
     *
     * @return True if the unit is still intact to perform its mission.
     */
    public boolean isValid() {
        return getUnit() != null && !getUnit().isDisposed();
    }

    /**
     * Checks if this mission is valid for the given unit.
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @return True if the unit can be usefully assigned this mission.
     */
    public static boolean isValid(AIUnit aiUnit) {
        return aiUnit != null
            && aiUnit.getMission() == null
            && aiUnit.getUnit() != null
            && !aiUnit.getUnit().isDisposed()
            && !aiUnit.getUnit().isUnderRepair();
    }

    /**
     * Checks if this Mission should only be carried out once.
     *
     * Missions are not one-time by default, true one-time missions
     * must override this routine.
     *
     * @return False.
     */
    public boolean isOneTime() {
        return false;
    }

    /**
     * Performs the mission. Must be implemented by the subclasses.
     *
     * @param connection The <code>Connection</code> to the server.
     */
    public abstract void doMission(Connection connection);

    /**
     * Gets debugging information about this mission.  This string is
     * a short representation of this object's state.
     *
     * @return "".
     */
    public String getDebuggingInfo() {
        return "";
    }


    // Serialization

    /**
     * {@inherit-doc}
     */
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        out.writeAttribute("unit", getUnit().getId());
    }

    /**
     * {@inherit-doc}
     */
    protected void readAttributes(XMLStreamReader in)
        throws XMLStreamException {
        String unit = in.getAttributeValue(null, "unit");
        setAIUnit((AIUnit)getAIMain().getAIObject(unit));
    }
}
