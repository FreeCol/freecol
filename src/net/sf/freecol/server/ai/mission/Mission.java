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
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Location;
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
import net.sf.freecol.server.ai.AIColony;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIObject;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.ai.EuropeanAIPlayer;


/**
 * A mission describes what a unit should do; attack, build colony,
 * wander etc.  Every {@link AIUnit} should have a mission. By
 * extending this class, you create different missions.
 */
public abstract class Mission extends AIObject {

    private static final Logger logger = Logger.getLogger(Mission.class.getName());

    /** A transport can be used.*/
    protected static final int MINIMUM_TRANSPORT_PRIORITY = 60;

    /** Transport is required. */
    protected static final int NORMAL_TRANSPORT_PRIORITY = 100;

    protected static final int NO_PATH_TO_TARGET = -2,
                               NO_MORE_MOVES_LEFT = -1;

    /** The unit to undertake the mission. */
    private AIUnit aiUnit;


    /**
     * Creates an uninitialized mission.
     *
     * @param aiMain The main AI-object.
     */
    public Mission(AIMain aiMain) {
        super(aiMain);

        this.aiUnit = null;
    }

    /**
     * Creates a mission for the given <code>AIUnit</code>.
     *
     * Note that missions are attached to their units, and thus do
     * not need AI ids, hence the plain superclass constructor.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
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
     * Gets the target of this mission, if any.
     * Subclasses should override this.
     *
     * @return The target of this mission or null if none.
     */
    public Location getTarget() {
        return null;
    }

    /**
     * Gets the unit this mission has been created for.
     *
     * @return The <code>Unit</code>.
     */
    public Unit getUnit() {
        return (aiUnit == null) ? null : aiUnit.getUnit();
    }

    /**
     * Gets the AI-unit this mission has been created for.
     *
     * @return The <code>AIUnit</code>.
     */
    public AIUnit getAIUnit() {
        return aiUnit;
    }

    /**
     * Sets the AI-unit this mission has been created for.
     *
     * @param aiUnit The <code>AIUnit</code>.
     */
    protected void setAIUnit(AIUnit aiUnit) {
        this.aiUnit = aiUnit;
    }

    /**
     * Convenience accessor for the owning AI player.
     *
     * @return The <code>AIPlayer</code>.
     */
    protected AIPlayer getAIPlayer() {
        return getAIMain().getAIPlayer(getUnit().getOwner());
    }

    /**
     * Convenience accessor for the owning European AI player.
     *
     * @return The <code>EuropeanAIPlayer</code>.
     */
    protected EuropeanAIPlayer getEuropeanAIPlayer() {
        Player player = getUnit().getOwner();
        if (!player.isEuropean()) {
            throw new IllegalArgumentException("Not a European player: "
                + player);
        }
        return (EuropeanAIPlayer)getAIMain().getAIPlayer(player);
    }

    /**
     * Convenience accessor for the unit/player PRNG.
     *
     * @return A <code>Random</code> to use.
     */
    protected Random getAIRandom() {
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

    /**
     * Moves a unit to the new world.
     *
     * @return True if there was no c-s problem.
     */
    protected boolean moveUnitToAmerica() {
        return AIMessage.askMoveTo(aiUnit,
            getUnit().getOwner().getGame().getMap());
    }

    /**
     * Moves a unit to Europe.
     *
     * @return True if there was no c-s problem.
     */
    protected boolean moveUnitToEurope() {
        return AIMessage.askMoveTo(aiUnit, getUnit().getOwner().getEurope());
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

                if (defender.getOwner() == unit.getOwner()
                    || newTile.isLand() == unit.isNaval()) return false;

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
     * Finds the best existing settlement to use as a target.
     * Useful for missions where the unit might be in Europe, but should
     * go to a safe spot in the New World and proceed from there.
     *
     * @param player The <code>Player</code> that is searching.
     * @return A good settlement to restart a Mission from.
     */
    protected static Settlement getBestSettlement(Player player) {
        int bestValue = -1;
        Settlement best = null;
        for (Settlement settlement : player.getSettlements()) {
            int value = settlement.getUnitCount()
                + settlement.getTile().getUnitCount();
            if (settlement instanceof Colony) {
                Colony colony = (Colony)settlement;
                value += ((colony.isConnected()) ? 10 : 0) // Favour coastal
                    + colony.getAvailableWorkLocations().size();
            }
            if (value > bestValue) {
                bestValue = value;
                best = settlement;
            }
        }
        return (best == null) ? null : best;
    }

    /**
     * Unload a unit.
     * Fulfills a wish if possible.
     *
     * @param aiUnit The <code>AIUnit</code> to unload.
     * @return True if the unit is unloaded.
     */
    protected boolean unitLeavesShip(AIUnit aiUnit) {
        boolean result = AIMessage.askDisembark(aiUnit);
        Colony colony = aiUnit.getUnit().getColony();
        if (result && colony != null) {
            AIColony ac = getAIMain().getAIColony(colony);
            if (ac != null) ac.completeWish(aiUnit.getUnit());

            colony.firePropertyChange(Colony.REARRANGE_WORKERS, true, false);
        }
        return result;
    }

    /**
     * Unload some goods.
     * Fulfills a wish if possible.
     *
     * @param goods The <code>Goods</code> to unload.
     * @return True if the goods are unloaded.
     */
    protected boolean unloadCargoInColony(Goods goods) {
        boolean result = AIMessage.askUnloadCargo(aiUnit, goods);
        Colony colony = aiUnit.getUnit().getColony();
        AIColony ac;
        if (result && colony != null
            && (ac = getAIMain().getAIColony(colony)) != null) {
            ac.completeWish(goods);
        }
        return result;
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
     * Gets a standard mission-specific goal decider.
     *
     * @param aiUnit The <code>AIUnit</code> searching.
     * @param type The specific mission class.
     * @return A standard goal decider for the supplied mission type.
     */
    public static GoalDecider getMissionGoalDecider(final AIUnit aiUnit,
                                                    final Class type) {
        final AIPlayer aiPlayer = aiUnit.getAIMain()
            .getAIPlayer(aiUnit.getUnit().getOwner());

        return new GoalDecider() {
            private int bestValue = Integer.MIN_VALUE;
            private PathNode best = null;

            public PathNode getGoal() { return best; }
            public boolean hasSubGoals() { return true; }
            public boolean check(Unit u, PathNode path) {
                int value = aiPlayer.scoreMission(aiUnit, path, type);
                if (value > bestValue) {
                    bestValue = value;
                    best = path;
                    return true;
                }
                return false;
            }
        };
    }

    /**
     * Evaluates a proposed mission type for a unit.
     *
     * This works out the basic mission-specific score.  The final
     * result goes through further refinement in the fooAIPlayer
     * specialized scoreMission routines.
     *
     * TODO: see if we can remove the requirement this routine be
     * static (trouble is, we are trying to work out what sort of
     * mission to use, so there is not yet a mission object to call a
     * method of).  We could use introspection to call a static
     * scoreTarget routine if it exists...
     *
     * @param aiUnit The <code>AIUnit</code> to perform the mission.
     * @param path A <code>PathNode</code> to the target of this mission.
     * @param type The mission class.
     * @return A score representing the desirability of this mission.
     */
    public static int scorePath(AIUnit aiUnit, PathNode path, Class type) {
        return (type == BuildColonyMission.class)
            ? BuildColonyMission.scorePath(aiUnit, path)
            : (type == CashInTreasureTrainMission.class)
            ? CashInTreasureTrainMission.scorePath(aiUnit, path)
            : (type == DefendSettlementMission.class)
            ? DefendSettlementMission.scorePath(aiUnit, path)
            : (type == PioneeringMission.class)
            ? PioneeringMission.scorePath(aiUnit, path)
            : (type == ScoutingMission.class)
            ? ScoutingMission.scorePath(aiUnit, path)
            : (type == UnitSeekAndDestroyMission.class)
            ? UnitSeekAndDestroyMission.scorePath(aiUnit, path)
            : -1; // NYI
    }

    /**
     * Finds a suitable seek-and-destroy target path for an AI unit.
     *
     * @param aiUnit The <code>AIUnit</code> to find a target for.
     * @param range An upper bound on the number of moves.
     * @param type The mission class.
     * @return A path to the target, or null if none found.
     */
    public static PathNode findTargetPath(AIUnit aiUnit, int range, Class type) {
        Unit unit;
        Tile startTile;
        return (aiUnit == null
            || (unit = aiUnit.getUnit()) == null || unit.isDisposed() 
            || (startTile = unit.getPathStartTile()) == null)
            ? null
            : unit.search(startTile, getMissionGoalDecider(aiUnit, type),
                CostDeciders.avoidIllegal(), range, unit.getCarrier());
    }

    /**
     * Should the unit use transport to get to a specified tile?
     *
     * True if:
     * - The unit is not there already
     * AND
     *   - the unit already has transport, this will always be faster
     *     (TODO: actually, mounted units on good roads might be faster,
     *     check for this)
     *   - if not on the map
     *   - if on the map but can not find a path to the tile, unless
     *     adjacent to the destination which usually means the path
     *     finding failed due to a temporary blockage such as an enemy unit
     *   - if the path to the tile will take more than MAX_TURNS
     *
     * @param tile The <code>Tile</code> to go to.
     * @return True if the unit should use transport.
     */
    protected boolean shouldTakeTransportToTile(Tile tile) {
        final int MAX_TURNS = 5;
        final Unit unit = getUnit();
        PathNode path;
        return tile != null
            && unit != null
            && unit.getTile() != tile
            && (unit.isOnCarrier()
                || unit.getTile() == null
                || ((path = unit.findPath(tile)) == null
                    && !unit.getTile().isAdjacent(tile))
                || (path != null && path.getTotalTurns() >= MAX_TURNS));
    }

    /**
     * Tries to move this mission's unit to a target location.
     *
     * First check for units in transit, that is units on a carrier that
     * are going to but not yet in Europe, or going to but not yet at
     * a Tile and whose path still requires the carrier.  These need to
     * be handled by the carrier's TransportMission, not by the unit's
     * Mission.
     *
     * Similarly check for units not in transit but should be, that
     * is units not on a carrier but can not get to their target
     * without one.  These must just wait.
     *
     * If there is no impediment to the unit moving towards the target,
     * do so.  Return an indicative MoveType for the result of the travel.
     * - MOVE if the unit has arrived at the target.  The unit may not
     *   have moved, or have exhausted its moves.
     * - MOVE_NO_MOVES if out of moves short of the target
     * - MOVE_ILLEGAL if the unit is unable to proceed for now
     * - MOVE_NO_REPAIR if the unit died for whatever reason
     * - other results (e.g. ENTER_INDIAN_SETTLEMENT*) if that would
     *   occur if the unit proceeded.  Such moves require special handling
     *   and are not performed here, the calling mission code must
     *   handle them.
     *
     * @param logMe A prefix string for the log messages.
     * @param target The destination <code>Location</code>.
     * @return The type of move the unit stopped at.
     */
    protected MoveType travelToTarget(String logMe, Location target) {
        final Tile targetTile = target.getTile();
        if (!(target instanceof Europe) && targetTile == null) {
            throw new IllegalStateException("Target neither Europe nor Tile");
        }

        final Unit unit = getUnit();
        final Unit carrier = unit.getCarrier();
        PathNode path = null;
        boolean inTransit = false;
        boolean needTransport = false;
        if (target instanceof Europe) {
            if (unit.isInEurope()) return MoveType.MOVE;

            if (unit.isOnCarrier()) {
                inTransit = true;
            } else {
                needTransport = true;
            }
        } else {
            if (unit.getTile() == targetTile) return MoveType.MOVE;

            if (unit.isOnCarrier()) {
                if (carrier.getTile() == null) {
                    inTransit = true;
                } else {
                    path = unit.findPath(unit.getTile(), targetTile, carrier);
                    if (path == null) {
                        logger.finest(logMe 
                            + " can not get from " + unit.getTile()
                            + " to " + targetTile + ": " + unit);
                        return MoveType.MOVE_ILLEGAL;
                    } else {
                        inTransit = path.isOnCarrier();
                    }
                }
            } else {
                if (unit.isInEurope()) {
                    needTransport = true;
                } else if (unit.getTile() == null) { // Can not happen
                    throw new IllegalStateException("No tile or carrier: "
                        + unit);
                } else {
                    path = unit.findPath(targetTile);
                    if (path == null) needTransport = true;
                }
            }
        }
        if (inTransit) {
            logger.finest(logMe + " in transit to " + target + ": " + unit);
            return MoveType.MOVE_ILLEGAL;
        } else if (needTransport) {
            logger.finest(logMe + " at " + unit.getLocation()
                + " needs transport to " + target + ": " + unit);
            return MoveType.MOVE_ILLEGAL;
        }

        // This can not happen.
        if (path == null) throw new IllegalStateException("Path == null");

        // Follow the path towards the target.
        for (; path != null; path = path.next) {
            if (unit.getMovesLeft() <= 0) {
                logger.finest(logMe + " at " + unit.getTile()
                    + " en route to " + targetTile + ": " + unit);
                return MoveType.MOVE_NO_MOVES;
            }

            MoveType mt = unit.getMoveType(path.getDirection());
            if (!mt.isProgress()) return mt; // Special handling required

            if (!AIMessage.askMove(aiUnit, path.getDirection())) {
                logger.finest(logMe + " at " + unit.getTile()
                    + " failed to move: " + unit);
                return MoveType.MOVE_ILLEGAL;
            } else if (unit.isDisposed()) {
                logger.finest(logMe + " died en route to " + targetTile
                    + ": " + unit);
                return MoveType.MOVE_NO_REPAIR;
            }
        }
        return MoveType.MOVE; // Must have completed path
    }

    // Fake implementation of Transportable interface.
    // Missions are not actually Transportables but the units that are
    // performing a mission delegate to these routines.

    /**
     * Gets the transport destination of the unit associated with this
     * mission.
     * TODO: is this still needed?
     *
     * @return The destination of a required transport or
     *         <code>null</code> if no transport is needed.
     */
    public Location getTransportDestination() {
        final Unit unit = getUnit();
        final Unit carrier = unit.getCarrier();
        PathNode path;

        return (unit.getTile() == null)
            ? ((unit.isOnCarrier()) ? carrier : unit).getFullEntryLocation()
            : (!unit.isOnCarrier()) ? null
            : (carrier.getSettlement() != null) ? carrier.getTile()
            : ((path = unit.findOurNearestSettlement()) == null) ? null
            : path.getLastNode().getTile();
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
     * Checks if this mission is valid for the given unit.
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @return True if the unit can be usefully assigned this mission.
     */
    public static boolean isValid(AIUnit aiUnit) {
        return aiUnit != null
            && (aiUnit.getMission() == null || !aiUnit.getMission().isValid())
            && aiUnit.getUnit() != null
            && !aiUnit.getUnit().isDisposed()
            && !aiUnit.getUnit().isUnderRepair();
    }

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
     * Performs the mission.
     */
    public abstract void doMission();


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
