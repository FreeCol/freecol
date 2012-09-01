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
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.common.model.pathfinding.CostDecider;
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
    protected static final String TARGETNULL = "target-null";
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
     * @return A reason for the target to be invalid, or null if none found.
     */
    public static String invalidTargetReason(Location target) {
        return (target == null) ? Mission.TARGETNULL
            : (((FreeColGameObject)target).isDisposed()) ? "target-disposed"
            : null;
    }

    /**
     * Is a target a valid mission target?
     *
     * @param target The target <code>Location</code> to check.
     * @param player A <code>Player</code> that should own
     *     the target.
     * @return A reason for the target to be invalid, or null if none found.
     */
    public static String invalidTargetReason(Location target, Player owner) {
        String reason = invalidTargetReason(target);
        return (reason != null)
            ? reason
            : (target instanceof Ownable
                && owner != ((Ownable)target).getOwner())
            ? Mission.TARGETOWNERSHIP
            : null;
    }

    /**
     * Is another player a valid attack target?
     *
     * @param aiUnit The <code>AIUnit</code> that will attack.
     * @param other The <code>Player</code> to attack.
     * @return A reason why the attack would be invalid, or null if none found.
     */
    public static String invalidAttackReason(AIUnit aiUnit, Player other) {
        final Unit unit = aiUnit.getUnit();
        final Player player = unit.getOwner();
        return (player == other)
            ? Mission.TARGETOWNERSHIP
            : (player.isIndian()
                && player.getTension(other).getLevel()
                .compareTo(Tension.Level.CONTENT) <= 0)
            ? "target-native-tension-too-low"
            : (player.isEuropean()
                && !(player.getStance(other) == Stance.WAR
                    || (unit.hasAbility(Ability.PIRACY)
                        && player.getStance(other) != Stance.ALLIANCE)))
            ? "target-european-war-absent"
            : null;
    }

    /**
     * We have been blocked on the way to a target.  Attack the blockage,
     * or just try to avoid it?
     *
     * @param aiUnit The <code>AIUnit</code> that was blocked.
     * @param target The target <code>Location</code>.
     * @return The blockage to attack, or null if not.
     */
    public static Location resolveBlockage(AIUnit aiUnit, Location target) {
        final Unit unit = aiUnit.getUnit();
        PathNode path = unit.findPath(target.getTile());
        Direction d = null;
        if (path != null && path.next != null) {
            Tile tile = path.next.getTile();
            Settlement settlement = tile.getSettlement();
            Unit defender = tile.getDefendingUnit(unit);
            Location blocker = (settlement != null) ? settlement : unit;
            if (UnitSeekAndDestroyMission.invalidReason(aiUnit, blocker)
                == null) return blocker;
        }
        // Can not/decided not to attack.  Take one random step in
        // roughly the right direction (if known).
        return null;
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
     * Moves a unit randomly for the rest of its turn.
     *
     * @param logMe A string to log the random number generation with.
     */
    protected void moveRandomlyTurn(String logMe) {
        Direction direction = null;
        while ((direction = moveRandomly(logMe, direction)) != null);
        getUnit().setMovesLeft(0);
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
        final Unit carrier = getUnit();
        final Unit unit = aiUnit.getUnit();
        boolean result = (direction == null)
            ? AIMessage.askDisembark(aiUnit)
            : AIMessage.askMove(aiUnit, direction);
        Colony colony = unit.getColony();
        if (result) result = unit.getLocation() != carrier;
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
        final Unit carrier = getUnit();
        final Unit unit = aiUnit.getUnit();
        Colony colony = unit.getColony();
        boolean result = AIMessage.askEmbark(getAIUnit(), unit, direction);
        if (result) result = unit.getLocation() == carrier;
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
        if (carrier.getGoodsCount(type) < amount) return false;
        final AIUnit aiUnit = getAIUnit();
        Colony colony = carrier.getColony();
        int oldAmount = carrier.getGoodsCount(type);
        Goods goods = new Goods(carrier.getGame(), carrier, type, amount);
        boolean result;
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
        }
        if (result) {
            int newAmount = carrier.getGoodsCount(type);
            if (oldAmount - newAmount != amount) {
                logger.warning(carrier + " at " + carrier.getLocation()
                    + " only unloaded " + (oldAmount - newAmount)
                    + " " + type + " (" + amount + " expected)");
                // TODO: sort this out.
                // For now, do not tolerate partial unloads.
                result = false;
            }
        }   
        if (result && colony != null) {
            final AIColony aiColony = getAIMain().getAIColony(colony);
            if (aiColony != null) aiColony.completeWish(goods);
            colony.firePropertyChange(Colony.REARRANGE_WORKERS, true, false);
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
        Goods goods = aiGoods.getGoods();
        return goodsLeavesTransport(goods.getType(), goods.getAmount());
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
        int oldAmount = carrier.getGoodsCount(goodsType);
        boolean result;
        if (carrier.isInEurope()) {
            result = AIMessage.askBuyGoods(aiUnit, goodsType, goodsAmount);
        } else {
            result = AIMessage.askLoadCargo(aiUnit, goods);
        }
        if (result) {
            int newAmount = carrier.getGoodsCount(goodsType);
            if (newAmount - oldAmount != goodsAmount) {
                logger.warning(carrier + " at " + carrier.getLocation()
                    + " only loaded " + (newAmount - oldAmount)
                    + " " + goodsType
                    + " (" + goodsAmount + " expected)");
                goodsAmount = newAmount - oldAmount;
                // TODO: sort this out.  For now, tolerate partial loads.
                result = goodsAmount > 0;
            }
        }
        if (result) {
            Colony colony = carrier.getColony();
            if (colony != null) {
                getAIMain().getAIColony(colony).removeAIGoods(aiGoods);
            }
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
            : (type == MissionaryMission.class)
            ? MissionaryMission.scorePath(aiUnit, path)
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
                          CostDeciders.avoidIllegal(),
                          range, unit.getCarrier());
    }

    /**
     * Should the unit use transport to get to a specified tile?
     *
     * True if:
     * - The unit and tile are not null
     * - The unit is not there already
     * AND
     *   - there is no path OR the path uses an existing carrier
     *
     * @param tile The <code>Tile</code> to go to.
     * @return True if the unit should use transport.
     */
    protected boolean shouldTakeTransportToTile(Tile tile) {
        final Unit unit = getUnit();
        PathNode path;
        return tile != null
            && unit != null
            && unit.getTile() != tile
            && ((path = unit.findPath(unit.getLocation(), tile,
                                      unit.getCarrier(), null)) == null
                || path.usesCarrier());
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
     * - MOVE_NO_MOVES is underway but short of the target
     * - MOVE_NO_REPAIR if the unit died for whatever reason
     * - other legal results (e.g. ENTER_INDIAN_SETTLEMENT*) if that would
     *   occur if the unit proceeded.  Such moves require special handling
     *   and are not performed here, the calling mission code must
     *   handle them.
     *
     * Logging at `fine' on failures, `finest' on valid return values
     * other than normal path completion.
     *
     * @param logMe A prefix string for the log messages.
     * @param target The destination <code>Location</code>.
     * @param costDecider The <code>CostDecider</code> to use in any path
     *     finding.
     * @return The type of move the unit stopped at.
     */
    protected MoveType travelToTarget(String logMe, Location target,
                                      CostDecider costDecider) {
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

        // Sanitize the unit location and drop out the trivial cases.
        if (unit.isAtSea()) {
            logger.finest(logMe + " at sea: " + this);
            return MoveType.MOVE_NO_MOVES;
        } else if (unit.isInEurope()) {
            if (target instanceof Europe) {
                if (unit.getLocation() instanceof Unit) {
                    unitLeavesTransport(aiUnit, null);
                }
                return MoveType.MOVE;
            } else if (!unit.getOwner().canMoveToEurope()) {
                throw new IllegalStateException("Impossible move from Europe");
            }
        } else if (unit.getTile() == null) {
            throw new IllegalStateException("Null unit tile: " + unit);
        } else {
            if (unit.getTile() == targetTile) {
                if (unit.getLocation() instanceof Unit) {
                    unitLeavesTransport(aiUnit, null);
                }
                return MoveType.MOVE;
            } else if (target instanceof Europe) {
                if (!unit.getOwner().canMoveToEurope()) {
                    logger.fine(logMe + " impossible move to Europe"
                        + ": " + this);
                    return MoveType.MOVE_ILLEGAL;
                }
            }
        }

        final Map map = unit.getGame().getMap();
        if (target instanceof Europe) { // Going to Europe
            if (unit.getType().canMoveToHighSeas()) {
                if (unit.getTile().isDirectlyHighSeasConnected()) {
                    if (AIMessage.askMoveTo(aiUnit, target)) {
                        logger.finest(logMe + " sailed for " + target
                            + ": " + this);
                        return MoveType.MOVE_HIGH_SEAS;
                    } else {
                        logger.fine(logMe + " failed to sail for " + target
                            + ": " + this);
                        return MoveType.MOVE_ILLEGAL;
                    }
                }
                path = unit.findPath(unit.getLocation(), target,
                                     null, costDecider);
                if (path == null) {
                    logger.fine(logMe + " no path from " + unit.getLocation()
                        + " to " + target + ": " + this);
                    return MoveType.MOVE_ILLEGAL;
                }
            } else if (unit.isOnCarrier()) {
                inTransit = true;
            } else {
                needTransport = true;
            }
        } else if (unit.isInEurope()) { // Going to the map
            if (unit.getType().canMoveToHighSeas()) {
                if (AIMessage.askMoveTo(aiUnit, targetTile)) {
                    logger.finest(logMe + " sailed for " + target
                        + ": " + this);
                    return MoveType.MOVE_HIGH_SEAS;
                } else {
                    logger.fine(logMe + " failed to sail for " + target
                        + ": "+ this);
                    return MoveType.MOVE_ILLEGAL;
                }
            } else if (unit.isOnCarrier()) {
                inTransit = true;
            } else {
                needTransport = true;
            }
        } else { // Moving on the map
            if (unit.getType().canMoveToHighSeas()) {
                // If there is no path for a high seas capable unit, give up.
                path = unit.findPath(unit.getTile(), targetTile,
                                     null, costDecider);
                if (path == null) {
                    logger.fine(logMe + " no path from " + unit.getLocation()
                        + " to " + target + ": " + this);
                    return MoveType.MOVE_ILLEGAL;
                }
            } else if (unit.isOnCarrier()) {
                // Check if the carrier still has a useful path...
                path = unit.findPath(unit.getTile(), targetTile, 
                                     unit.getCarrier(), costDecider);
                if (path == null) {
                    logger.fine(logMe + " no transit from " + unit.getLocation()
                        + " to " + target + ": " + this);
                    return MoveType.MOVE_ILLEGAL;
                }
                // ...and whether the unit needs to stay on board.
                inTransit = path.isOnCarrier() && path.next.isOnCarrier();
            } else {
                // Not high seas capable.  If no path, it needs transport,
                // or is just blocked.
                path = unit.findPath(unit.getTile(), targetTile,
                                     null, costDecider);
                needTransport = path == null;
            }
        }
        if (inTransit) {
            logger.finest(logMe + " at " + unit.getLocation()
                + " in transit to " + target + ": " + this);
            return MoveType.MOVE_NO_MOVES;
        } else if (needTransport) {
            logger.finest(logMe + " at " + unit.getLocation()
                + " needs transport to " + target + ": " + this);
            return MoveType.MOVE_ILLEGAL;
        } else if (path == null) {
            throw new IllegalStateException("Path == null"); // Can not happen
        } else {
            Unit.MoveType mt = followPath(logMe, path);
            if (mt == MoveType.MOVE && unit.getLocation() instanceof Unit) {
                unitLeavesTransport(aiUnit, null);
            }
            return mt;
        }
    }

    /**
     * Follow a path to a target.
     *
     * Logging is fine on failures, finest on valid return values other
     * than normal path completion.
     *
     * @param logMe A prefix string for the log messages.
     * @param path The <code>PathNode</code> to follow.
     * @return The type of move the unit stopped at.
     */
    protected MoveType followPath(String logMe, PathNode path) {
        final Unit unit = getUnit();
        final AIUnit aiUnit = getAIUnit();
        final int NO_EUROPE = 0;
        final int USES_EUROPE = 1;
        final int BOTH_EUROPE = 2;

        for (; path != null; path = path.next) {
            int useEurope = 0;
            // Sanitize the unit state.
            if (unit.isDisposed()) {
                logger.fine(logMe + " died going to " + path.getLocation()
                    + ": " + this);
                return MoveType.MOVE_NO_REPAIR;
            } else if (unit.getMovesLeft() <= 0 || unit.isAtSea()) {
                logger.finest(logMe + " at " + unit.getLocation()
                    + " en route to " + path.getLastNode().getLocation()
                    + ": " + this);
                return MoveType.MOVE_NO_MOVES;
            } else if (unit.isInEurope()) {
                useEurope++;
            } else if (unit.getTile() == null) {
                logger.fine(logMe + " null location tile: " + this);
                return MoveType.MOVE_ILLEGAL;
            }
            // Sanitize the path node.
            if (path.getLocation() instanceof Europe) {
                useEurope++;
            } else if (path.getTile() == null) {
                logger.fine(logMe + " null path tile " + path.toString()
                    + ": " + this);
                return MoveType.MOVE_ILLEGAL;
            }

            // Post-sanitization there are only two valid cases,
            // not using Europe at all, and sailing to/from Europe.
            // Ignore the trivial path within Europe.
            // On success, continue or return.
            // On failure, fall through to the error report.
            switch (useEurope) {
            case NO_EUROPE:
                if (unit.getTile() == path.getTile()) {
                    if (unit.getLocation() instanceof Unit
                        && !path.isOnCarrier()) {
                        unitLeavesTransport(aiUnit, null);
                    }
                    continue;
                }
                MoveType mt = unit.getMoveType(path.getDirection());
                if (!mt.isProgress()) { // Special handling required.
                    logger.finest(logMe + " at " + unit.getTile()
                        + " has special move " + mt + " to " + path.getTile()
                        + ": " + this);
                    return mt;
                }

                if (AIMessage.askMove(aiUnit, path.getDirection())) continue;
                break;

            case USES_EUROPE:
                if (AIMessage.askMoveTo(aiUnit, path.getLocation())) {
                    logger.finest(logMe + " now on high seas: " + this);
                    return MoveType.MOVE_HIGH_SEAS;
                }
                break;

            case BOTH_EUROPE:
                continue; // Do nothing, on to next node.

            default:
                throw new IllegalStateException("Can not happen");
            }
            logger.finest(logMe + " at " + unit.getLocation()
                + " failed to move to " + path.getLocation()
                + ": " + this);
            return MoveType.MOVE_ILLEGAL;
        }
        return MoveType.MOVE; // Must have completed path normally, no log.
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
        String reason = invalidAIUnitReason(aiUnit);
        return (reason != null) ? reason : invalidTargetReason(loc);
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
            + "@" + Integer.toString(hashCode()) + "-" + aiUnit;
    }
}
