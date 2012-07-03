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
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.common.util.Utils;
import net.sf.freecol.server.ai.AIColony;
import net.sf.freecol.server.ai.AIGoods;
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

    // Common mission invalidity reasons.
    protected static final String AIUNITNULL = "aiUnit-null";
    protected static final String TARGETINVALID = "target-invalid";
    protected static final String TARGETOWNERSHIP = "target-ownership";
    protected static final String TARGETNOTFOUND = "target-not-found";
    protected static final String UNITNOTAPERSON = "unit-not-a-person";
    protected static final String UNITNOTOFFENSIVE = "unit-not-offensive";
    protected static final String UNITNOTONMAP = "unit-not-on-map";

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
     * Subclasses should override as needed.
     */
    public void dispose() {
        // Nothing to do yet.
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
     * Gets the unit this mission has been created for.
     *
     * @return The <code>Unit</code>.
     */
    public Unit getUnit() {
        return (aiUnit == null) ? null : aiUnit.getUnit();
    }

    /**
     * Convenience accessor for the owning player.
     *
     * @return The <code>Player</code> that owns the mission unit.
     */
    protected Player getPlayer() {
        return (getUnit() == null) ? null : getUnit().getOwner();
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
     * Is this mission valid?
     *
     * @return True if the mission is valid.
     */
    public final boolean isValid() {
        return invalidReason() == null;
    }

    /**
     * Is an invalidity reason due to a target failure?
     *
     * @return True if the reason starts with "target-".
     */
    public boolean isTargetReason(String reason) {
        return reason != null && reason.startsWith("target-");
    }

    /**
     * Is a unit able to perform a mission of a particular type?
     *
     * @param unit The <code>Unit</code> to check.
     * @return A reason for mission invalidity, or null if none found.
     */
    public static String invalidUnitReason(Unit unit) {
        return (unit == null) ? "unit-null"
            : (unit.isDisposed()) ? "unit-disposed"
            : (unit.isUnderRepair()) ? "unit-under-repair"
            : null;
    }

    /**
     * Is an AI unit able to perform a mission of a particular type?
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @return A reason for mission invalidity, or null if none found.
     */
    public static String invalidAIUnitReason(AIUnit aiUnit) {
        String reason;
        return (aiUnit == null) ? AIUNITNULL
            : ((reason = invalidUnitReason(aiUnit.getUnit())) != null) ? reason
            : null;
    }

    /**
     * Is an AI unable to perform a new mission because it already has
     * a valid, non-onetime mission?
     *
     * @return "mission-exists" if a valid mission is found, or null
     *     if none found.
     */
    public static String invalidNewMissionReason(AIUnit aiUnit) {
        return (aiUnit == null) ? AIUNITNULL
            : (aiUnit.hasMission()
                && !aiUnit.getMission().isOneTime()
                && aiUnit.getMission().isValid()) ? "mission-exists"
            : null;
    }

    /**
     * Is a target a valid mission target?
     *
     * @param target The target <code>Location</code> to check.
     * @param player An optional <code>Player</code> that should own
     *     the target.
     * @return A reason for the target to be invalid, or null if none found.
     */
    public static String invalidTargetReason(Location target, Player owner) {
        return (target == null) ? "target-null"
            : (((FreeColGameObject)target).isDisposed()) ? "target-disposed"
            : (owner == null) ? null
            : (target instanceof Ownable
                && owner != ((Ownable)target).getOwner())
            ? Mission.TARGETOWNERSHIP
            : null;
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
     * Moves a unit one step randomly.
     *
     * @param logMe A string to log the random number generation with.
     * @param direction An optional preferred <code>Direction</code>.
     * @return The direction of the move, or null if no move was made.
     */
    protected Direction moveRandomly(String logMe, Direction direction) {
        final Unit unit = getUnit();
        if (unit.getMovesLeft() <= 0) return null;
        if (logMe == null) logMe = "moveRandomly";

        Random aiRandom = getAIRandom();
        if (direction == null) {
            direction = Direction.getRandomDirection(logMe, aiRandom);
        }

        Direction[] directions
            = direction.getClosestDirections(logMe, aiRandom);
        for (int j = 0; j < directions.length; j++) {
            Direction d = directions[j];
            Tile moveTo = unit.getTile().getNeighbourOrNull(d);
            if (moveTo != null
                && unit.getMoveType(d) == MoveType.MOVE) {
                return (AIMessage.askMove(aiUnit, d)
                    && unit.getTile() == moveTo) ? d
                    : null; // Failed!
            }
        }
        return null; // Stuck!
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
                value += ((colony.isConnectedPort()) ? 10 : 0) // Favour coastal
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
     * A unit leaves a ship.
     * Fulfills a wish if possible.
     *
     * @param aiUnit The <code>AIUnit</code> to unload.
     * @param direction The <code>Direction</code> to move, if any.
     * @return True if the unit is unloaded.
     */
    protected boolean unitLeavesTransport(AIUnit aiUnit, Direction direction) {
        final Unit unit = aiUnit.getUnit();
        boolean result = (direction == null)
            ? AIMessage.askDisembark(aiUnit)
            : AIMessage.askMove(aiUnit, direction);
        Colony colony = unit.getColony();
        if (result && colony != null) {
            AIColony ac = getAIMain().getAIColony(colony);
            if (ac != null) ac.completeWish(unit);

            colony.firePropertyChange(Colony.REARRANGE_WORKERS, true, false);
        }
        return result;
    }

    /**
     * A unit joins a ship.
     *
     * @param aiUnit The <code>AIUnit</code> to load.
     * @param direction The <code>Direction</code> to move, if any.
     * @return True if the unit is loaded.
     */
    protected boolean unitJoinsTransport(AIUnit aiUnit, Direction direction) {
        final Unit unit = aiUnit.getUnit();
        Colony colony = unit.getColony();
        boolean result = (direction == null)
            ? AIMessage.askEmbark(getAIUnit(), unit, direction)
            : AIMessage.askMove(aiUnit, direction);
        if (result && colony != null) {
            colony.firePropertyChange(Colony.REARRANGE_WORKERS, true, false);
        }
        return result;
    }

    /**
     * Goods leaves a ship.
     *
     * @param type The <code>GoodsType</code> to unload.
     * @param amount The amount of goods to unload.
     * @return True if the unload succeeds.
     */
    protected boolean goodsLeavesTransport(GoodsType type, int amount) {
        final Unit carrier = getUnit();
        if (carrier.getGoodsContainer().getGoodsCount(type) < amount) {
            return false;
        }
        final AIUnit aiUnit = getAIUnit();
        boolean result;
        Goods goods = new Goods(carrier.getGame(), carrier, type, amount);
        if (carrier.isInEurope()) {
            if (carrier.getOwner().canTrade(type)) {
                result = AIMessage.askSellGoods(aiUnit, goods);
                logger.finest("Sell " + goods + " in Europe "
                    + ((result) ? "succeeds" : "fails")
                    + ": " + this);
            } else { // dump
                result = AIMessage.askUnloadCargo(aiUnit, goods);
            }
        } else {
            result = AIMessage.askUnloadCargo(aiUnit, goods);
            if (result) {
                final Colony colony = carrier.getTile().getColony();
                final AIColony aiColony = getAIMain().getAIColony(colony);
                if (aiColony != null) aiColony.completeWish(goods);
                colony.firePropertyChange(Colony.REARRANGE_WORKERS, true, false);
            }
        }
        return result;
    }

    /**
     * Goods leaves a ship.
     * Completes a wish if possible.
     *
     * @param aiGoods The <code>AIGoods</code> to unload.
     * @return True if the unload succeeds.
     */
    protected boolean goodsLeavesTransport(AIGoods aiGoods) {
        final Unit carrier = getUnit();
        Goods goods = aiGoods.getGoods();
        boolean result = goodsLeavesTransport(goods.getType(),
                                              goods.getAmount());
        if (result) {
            Colony colony = carrier.getColony();
            AIColony ac;
            if (colony != null
                && (ac = getAIMain().getAIColony(colony)) != null) {
                ac.completeWish(goods);
                colony.firePropertyChange(Colony.REARRANGE_WORKERS, true, false);
            }
        }
        return result;
    }

    /**
     * Goods joins a ship.
     *
     * @param aiGoods The <code>AIGoods</code> to load.
     * @return True if the load succeeds.
     */
    protected boolean goodsJoinsTransport(AIGoods aiGoods) {
        final Unit carrier = getUnit();
        final AIUnit aiUnit = getAIUnit();
        final Goods goods = aiGoods.getGoods();
        GoodsType goodsType = goods.getType();
        int goodsAmount = goods.getAmount();
        boolean result;
        if (carrier.isInEurope()) {
            result = AIMessage.askBuyGoods(aiUnit, goodsType, goodsAmount);
        } else {
            result = AIMessage.askLoadCargo(aiUnit, goods);
            Colony colony = carrier.getColony();
            if (colony != null) {
                getAIMain().getAIColony(colony).removeAIGoods(aiGoods);
            }
        }
        if (result) {
            aiGoods.setGoods(new Goods(getGame(), carrier,
                                       goodsType, goodsAmount));
        }
        return result;
    }

    // Deprecated, going away.
    protected void unitLeavesShip(AIUnit aiUnit) {
        unitLeavesTransport(aiUnit, null);
    }

    // Deprecated, going away.
    protected boolean sellCargoInEurope(Goods goods) {
        return goodsLeavesTransport(goods.getType(), goods.getAmount());
    }

    // Deprecated, going away.
    protected boolean unloadCargoInColony(Goods goods) {
        return goodsLeavesTransport(goods.getType(), goods.getAmount());
    }

    // Deprecated, going away.
    protected boolean moveUnitToEurope() {
        return getAIUnit().moveToEurope();
    }

    // Deprecated, going away.
    protected boolean moveUnitToAmerica() {
        return getAIUnit().moveToAmerica();
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
            private int bestValue = -1;
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
     * - MOVE_HIGH_SEAS if the unit has successfully set sail.
     * - MOVE_ILLEGAL if the unit is unable to proceed for now
     * - MOVE_NO_MOVES if out of moves short of the target
     * - MOVE_NO_REPAIR if the unit died for whatever reason
     * - other legal results (e.g. ENTER_INDIAN_SETTLEMENT*) if that would
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
        final AIUnit aiUnit = getAIUnit();
        final Unit carrier = unit.getCarrier();
        PathNode path = null;
        boolean inTransit = false;
        boolean needTransport = false;
        if (target instanceof Europe) {
            if (!unit.getOwner().canMoveToEurope()) {
                return MoveType.MOVE_ILLEGAL;
            }
            if (unit.isInEurope()) return MoveType.MOVE;

            if (unit.isNaval()) {
                if (unit.isAtSea()) {
                    logger.finest(logMe + " at sea: " + this);
                    return MoveType.MOVE_ILLEGAL;
                }
                if (unit.getTile().canMoveToHighSeas()) {
                    if (aiUnit.moveToEurope()) {
                        logger.finest(logMe + " sailed for Europe: " + this);
                        return MoveType.MOVE_HIGH_SEAS;
                    } else {
                        logger.finest(logMe + " failed to sail for Europe: "
                            + this);
                        return MoveType.MOVE_ILLEGAL;
                    }
                }
                path = unit.findPathToEurope();
                if (path == null) {
                    logger.finest(logMe
                        + " can not get from " + unit.getTile()
                        + " to Europe: " + this);
                    return MoveType.MOVE_ILLEGAL;
                }
            } else if (unit.isOnCarrier()) {
                inTransit = true;
            } else {
                needTransport = true;
            }
        } else {
            if (unit.getTile() == targetTile) return MoveType.MOVE;

            if (unit.isNaval()) {
                if (unit.isAtSea()) {
                    logger.finest(logMe + " at sea: " + this);
                    return MoveType.MOVE_ILLEGAL;
                } else if (unit.isInEurope()) {
                    if (aiUnit.moveToAmerica()) {
                        logger.finest(logMe + " sailed for the New World: "
                            + this);
                        return MoveType.MOVE_HIGH_SEAS;
                    } else {
                        logger.finest(logMe + " in Europe failed to sail: "
                            + this);
                        return MoveType.MOVE_ILLEGAL;
                    }
                } else {
                    path = unit.findPath(unit.getTile(), targetTile, null,
                        CostDeciders.avoidSettlementsAndBlockingUnits());
                    if (path == null) {
                        logger.finest(logMe
                            + " can not sail from " + unit.getTile()
                            + " to " + targetTile + ": " + this);
                        return MoveType.MOVE_ILLEGAL;
                    }
                }
            } else if (unit.isOnCarrier()) {
                if (unit.getTile() == null) {
                    inTransit = true;
                } else {
                    path = unit.findPath(unit.getTile(), targetTile, carrier,
                        CostDeciders.avoidSettlementsAndBlockingUnits());
                    if (path == null) {
                        logger.finest(logMe 
                            + " can not get from " + unit.getTile()
                            + " to " + targetTile + ": " + this);
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
                    path = unit.findPath(unit.getTile(), targetTile, null,
                        CostDeciders.avoidSettlementsAndBlockingUnits());
                    if (path == null) needTransport = true;
                }
            }
        }
        if (inTransit) {
            logger.finest(logMe + " in transit to " + target + ": " + this);
            return MoveType.MOVE_ILLEGAL;
        } else if (needTransport) {
            logger.finest(logMe + " at " + unit.getLocation()
                + " needs transport to " + target + ": " + this);
            return MoveType.MOVE_ILLEGAL;
        }

        // This can not happen.
        if (path == null) throw new IllegalStateException("Path == null");
        return followPath(logMe, path, target instanceof Europe);
    }

    /**
     * Follow a path to a target.
     *
     * @param logMe A prefix string for the log messages.
     * @param path The <code>PathNode</code> to follow.
     * @param europe The ultimate target is Europe, move there if possible.
     * @return The type of move the unit stopped at.
     */
    protected MoveType followPath(String logMe, PathNode path,
                                  boolean europe) {
        final Unit unit = getUnit();
        for (; path != null; path = path.next) {
            if (unit.getMovesLeft() <= 0) {
                logger.finest(logMe + " at " + unit.getTile()
                    + " en route to " + path.getLastNode().getTile()
                    + ": " + this);
                return MoveType.MOVE_NO_MOVES;
            }

            MoveType mt = unit.getMoveType(path.getDirection());
            if (!mt.isProgress()) return mt; // Special handling required

            if (!AIMessage.askMove(getAIUnit(), path.getDirection())) {
                logger.finest(logMe + " at " + unit.getTile()
                    + " failed to move: " + this);
                return MoveType.MOVE_ILLEGAL;
            } else if (unit.isDisposed()) {
                logger.finest(logMe + " died en route to " + path.getTile()
                    + ": " + this);
                return MoveType.MOVE_NO_REPAIR;
            }
        }
        if (europe && unit.getTile().canMoveToHighSeas()) {
            if (!AIMessage.askMoveTo(getAIUnit(),
                    unit.getOwner().getEurope())) {
                logger.finest(logMe + " at " + unit.getTile()
                    + " failed to sail for Europe: " + this);
                return MoveType.MOVE_ILLEGAL;
            }
            logger.finest(logMe + " sailed for Europe: " + this);
            return MoveType.MOVE_HIGH_SEAS;
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
     * Gets the target of this mission, if any.
     *
     * @return The target of this mission, or null if none.
     */
    public abstract Location getTarget();

    /**
     * Why is this mission invalid?
     *
     * Mission subclasses must implement this routine, which probably
     * should start by checking invalidAIUnitReason.
     * 
     * A mission can be invalid for a number of subclass-specific
     * reasons.  For example: a seek-and-destroy mission could be
     * invalid because of a improved stance towards the targeted
     * player.
     *
     * @return A reason for mission invalidity, or null if none found.
     */
    public abstract String invalidReason();

    /**
     * Is an AI unit able to perform a different mission?
     *
     * AIPlayers will call FooMission.invalidReason(aiUnit) to
     * determine whether it is valid to assign some unit to a
     * FooMission, so `interesting' Mission subclasses with complex
     * validity requirements must implement a routine with this
     * signature.  Conversely, simple Missions that are always possible
     * need not.
     *
     * Implementations should usually start by calling this routine
     * (i.e. Mission.invalidReason(AIUnit)).
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @return A reason for mission invalidity, or null if none found.
     */
    public static String invalidReason(AIUnit aiUnit) {
        return invalidAIUnitReason(aiUnit);
    }

    /**
     * Is an AI unit able to perform a mission with a specified target?
     *
     * Specific Missions can be invalid for target-related reasons.
     * Such Missions need to implement a routine with this signature,
     * as it will be called by the GoalDeciders in map path find/searches
     * to choose a Mission target.
     *
     * Implementations should usually start by calling either
     * invalidAIUnitReason() or this routine if the target checking is
     * trivial.
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @param loc The target <code>Location</code> to check.
     * @return A reason for mission invalidity, or null if none found.
     */
    public static String invalidReason(AIUnit aiUnit, Location loc) {
        String reason;
        return ((reason = invalidAIUnitReason(aiUnit)) != null) ? reason
            : invalidTargetReason(loc, null);
    }

    /**
     * Should this mission only be carried out once?
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
     * {@inheritDoc}
     */
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        out.writeAttribute("unit", getUnit().getId());
    }

    /**
     * {@inheritDoc}
     */
    protected void readAttributes(XMLStreamReader in)
        throws XMLStreamException {
        String unit = in.getAttributeValue(null, "unit");
        setAIUnit((AIUnit)getAIMain().getAIObject(unit));
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return Utils.lastPart(getClass().getName(), ".")
            + "@" + Integer.toString(hashCode())
            + ((aiUnit == null) ? "-aiUnit-null"
                : (getUnit() == null) ? "-unit-null"
                : getUnit().toString());
    }
}
