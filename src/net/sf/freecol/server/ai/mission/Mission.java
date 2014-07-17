/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
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
import net.sf.freecol.common.model.UnitLocation;
import net.sf.freecol.common.model.pathfinding.CostDecider;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.common.util.Utils;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIObject;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.ai.EuropeanAIPlayer;
import net.sf.freecol.server.ai.Transportable;


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
            : (unit.isUninitialized()) ? "unit-uninitialized"
            : (unit.isDisposed()) ? "unit-disposed"
            : (unit.isDamaged()) ? "unit-under-repair"
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
     * @param owner A <code>Player</code> that should own
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
     * Finds a target for a unit without considering its movement
     * abilities.  This is used by missions when the current unit
     * can not find a target with the normal path finding routines,
     * and thus should consider targets that may require a carrier.
     *
     * @param aiUnit The <code>AIUnit</code> that is searching.
     * @param gd The <code>GoalDecider</code> that selects targets.
     * @param radius A maximum radius from the unit location to search within.
     * @param deferOK If true, fall back to the nearest port to Europe.
     * @return The best target <code>Tile</code> found, or null if none.
     */
    protected static Location findCircleTarget(final AIUnit aiUnit,
                                               final GoalDecider gd,
                                               final int radius,
                                               boolean deferOK) {
        final Unit unit = aiUnit.getUnit();
        final Tile start = unit.getTile();
        if (start == null) {
            if (!deferOK) return null;
            Settlement settlement = unit.getOwner().getClosestPortForEurope();
            return (settlement == null) ? null : settlement;
        }
        return unit.getGame().getMap().searchCircle(start, gd, radius);
    }

    /**
     * We have been blocked on the way to a target.  Is it valid to
     * attack the blockage, or should it just be avoided?
     *
     * @param aiUnit The <code>AIUnit</code> that was blocked.
     * @param target The target <code>Location</code>.
     * @return The blockage to attack, or null if not.
     */
    public static Location resolveBlockage(AIUnit aiUnit, Location target) {
        final Unit unit = aiUnit.getUnit();
        PathNode path = unit.findPath(target);
        Direction d = null;
        if (path != null && path.next != null) {
            Tile tile = path.next.getTile();
            Settlement settlement = tile.getSettlement();
            Unit defender = tile.getDefendingUnit(unit);
            Location blocker = (settlement != null) ? settlement : unit;
            if (UnitSeekAndDestroyMission.invalidReason(aiUnit, blocker)
                == null) return blocker;
        }
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
        if (unit.getMovesLeft() <= 0 || !unit.hasTile()) return null;
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
                && unit.getMoveType(d) == MoveType.MOVE
                && aiUnit.move(d)) return d;
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
     * Check if a unit can disembark to its transport destination.
     * This is useful because the normal path finding does not always
     * chose to use the transport destination tile, but it may have been
     * chosen carefully.
     *
     * @param tag A logging tag.
     * @return Positive if the unit disembarked, zero if no change occurs,
     *     negative if the disembark failed.
     */
    protected int checkDisembark(String tag) {
        final AIUnit aiUnit = getAIUnit();
        final Unit unit = getUnit();
        Location transportTarget;
        Tile tile;
        if (unit != null
            && unit.hasTile()
            && unit.isOnCarrier()
            && (transportTarget = getTransportDestination()) != null
            && (tile = transportTarget.getTile()) != null) {
            if (unit.getTile() == tile) {
                if (!aiUnit.leaveTransport(null)) {
                    logger.warning(tag + " at " + unit.getLocation()
                        + " failed simple disembark to " + transportTarget
                        + ": " + this);
                    return -1;
                }
                return 1;
            }
            Direction d = unit.getTile().getDirection(tile);
            if (d != null) {
                switch (unit.getMoveType(d)) {
                case MOVE:
                    if (!aiUnit.leaveTransport(d)) {
                        logger.warning(tag + " at " + unit.getLocation()
                            + " failed disembark " + d
                            + " to " + transportTarget + ": " + this);
                        return -1;
                    }
                    return 1;
                case ATTACK_UNIT:
                    Unit other = tile.getFirstUnit();
                    if (unit.getOwner().atWarWith(other.getOwner())) {
                        AIMessage.askAttack(aiUnit, d);
                    }
                    // Return failure as even if the attack succeeds the
                    // disembark has failed and the unit has consumed its
                    // moves so no further progress is possible.
                    return -1;
                default:
                    break;
                }
            }
        }
        return 0;
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
     * - MOVE_NO_TILE if there is no path (usually transitory on rivers)
     * - other legal results (e.g. ENTER_INDIAN_SETTLEMENT*) if that would
     *   occur if the unit proceeded.  Such moves require special handling
     *   and are not performed here, the calling mission code must
     *   handle them.
     *
     * Logging at `fine' on failures, `finest' on valid return values
     * other than normal path completion.
     *
     * @param target The destination <code>Location</code>.
     * @param costDecider The <code>CostDecider</code> to use in any path
     *     finding.
     * @param sb An optional <code>StringBuilder</code> to log to.
     * @return The type of move the unit stopped at.
     */
    protected MoveType travelToTarget(Location target, CostDecider costDecider,
                                      StringBuilder sb) {
        final Tile targetTile = target.getTile();
        if (!(target instanceof Europe) && targetTile == null) {
            throw new IllegalStateException("Target neither Europe nor Tile");
        }

        final Unit unit = getUnit();
        final AIUnit aiUnit = getAIUnit();
        final Unit carrier = unit.getCarrier();
        final Map map = unit.getGame().getMap();
        PathNode path = null;
        boolean inTransit = false;
        boolean needTransport = false;

        // Are we there yet?
        if (unit.isAtLocation(target)) {
            if (unit.isOnCarrier()) {
                if (!aiUnit.leaveTransport(null)) {
                    logSB(sb, ", at ", target,
                        " failed to disembark from ", carrier, ".");
                    return MoveType.MOVE_ILLEGAL;
                }
            }
            return MoveType.MOVE;
        }

        // Sanitize the unit location and drop out the trivial cases.
        if (unit.isAtSea()) {
            logSB(sb, ", at sea.");
            return MoveType.MOVE_NO_MOVES;
        } else if (unit.isInEurope()) {
            if (!unit.getOwner().canMoveToEurope()) {
                throw new IllegalStateException("Impossible move from Europe");
            }
            if (unit.getType().canMoveToHighSeas()) {
                unit.setDestination(target);
                if (AIMessage.askMoveTo(aiUnit, map)) {
                    logSB(sb, ", sailed for ", target, ".");
                    return MoveType.MOVE_HIGH_SEAS;
                } else {
                    logSB(sb, ", failed to sail for ", target, ".");
                    return MoveType.MOVE_ILLEGAL;
                }
            }
        } else if (!unit.hasTile()) {
            throw new IllegalStateException("Unit not on the map: " + unit);
        } else if (target instanceof Europe) {
            if (!unit.getOwner().canMoveToEurope()) {
                logSBfail(sb, "impossible move to Europe for ", unit);
                return MoveType.MOVE_ILLEGAL;
            }
            if (unit.getType().canMoveToHighSeas()
                && unit.getTile().isDirectlyHighSeasConnected()) {
                if (AIMessage.askMoveTo(aiUnit, target)) {
                    logSB(sb, ", sailed for ", target, ".");
                    return MoveType.MOVE_HIGH_SEAS;
                } else {
                    logSB(sb, ", failed to sail for ", target, ".");
                    return MoveType.MOVE_ILLEGAL;
                }
            }
        }

        path = unit.findPath(unit.getLocation(), target, carrier, costDecider);
        if (path == null) {
            if (unit.getType().canMoveToHighSeas() || unit.isOnCarrier()
                || unit.getOwner().isIndian()) {
                logSB(sb, ", no path from ", unit.getLocation(),
                    " to ", target, ".");
                return MoveType.MOVE_NO_TILE;
            }
            AIUnit newAICarrier = aiUnit.getTransport();
            if (newAICarrier == null) {
                logSB(sb, ", at ", unit.getLocation(),
                    " needs transport to ", target, ".");
                return MoveType.MOVE_ILLEGAL;
            }
            Unit newCarrier = newAICarrier.getUnit();
            path = unit.findPath(unit.getLocation(), target,
                                 newCarrier, costDecider);
            if (path == null) {
                logSB(sb, ", at ", unit.getLocation(),
                    " no path to ", target,
                    " with assigned carrier ", newCarrier, ".");
                return MoveType.MOVE_ILLEGAL;
            }
        }
        if (path.next == null) {
            throw new IllegalStateException("Trivial path found "
                + path.fullPathToString()
                + " from " + unit.getLocation() + " to target " + target
                + " result=" +  unit.isAtLocation(target));
        }            
        return followPath(path.next, sb);
    }

    /**
     * Follow a path to a target.
     *
     * Logging is fine on failures, finest on valid return values other
     * than normal path completion.
     *
     * @param path The <code>PathNode</code> to follow.
     * @param sb An optional <code>StringBuilder</code> to log to.
     * @return The type of move the unit stopped at.
     */
    protected MoveType followPath(PathNode path, StringBuilder sb) {
        final Unit unit = getUnit();
        final AIUnit aiUnit = getAIUnit();
        final Unit carrier = unit.getCarrier();
        final Location target = path.getLastNode().getLocation();
        final int NO_EUROPE = 0;
        final int USES_EUROPE = 1;
        final int BOTH_EUROPE = 2;

        for (; path != null; path = path.next) {
            int useEurope = 0;
            // Sanitize the unit state.
            if (unit.isDisposed()) {
                logSB(sb, ", died going to ", path.getLocation(), ".");
                return MoveType.MOVE_NO_REPAIR;
            } else if (unit.getMovesLeft() <= 0 || unit.isAtSea()) {
                logSB(sb, ", at ", unit.getLocation(),
                    " en route to ", target, ".");
                return MoveType.MOVE_NO_MOVES;
            } else if (unit.isInEurope()) {
                useEurope++;
            } else if (!unit.hasTile()) {
                logSB(sb, ", not on the map.");
                return MoveType.MOVE_ILLEGAL;
            }

            // Sanitize the path node.
            if (path.getLocation() instanceof Europe) {
                useEurope++;
            } else if (path.getTile() == null) {
                logSB(sb, ", null path tile.");
                return MoveType.MOVE_ILLEGAL;
            }

            // Handle embark/disembark.
            if (unit.isOnCarrier() && path.isOnCarrier()) {
                logSB(sb, ", on ", unit.getLocation(),
                    " in transit to ", target, ".");
                return MoveType.MOVE_NO_MOVES;

            } else if (unit.isOnCarrier() && !path.isOnCarrier()) {
                Direction d;
                if (unit.isAtLocation(path.getLocation())) {
                    d = null;
                } else if (unit.hasTile()
                    && unit.getTile().isAdjacent(path.getTile())) {
                    if (unit.getMovesLeft() <= 0) {
                        logSB(sb, ", on ", unit.getLocation(),
                            " waiting to disembark.");
                        return MoveType.MOVE_NO_MOVES;
                    }
                    d = unit.getTile().getDirection(path.getTile());
                } else {
                    logSB(sb, ", on ", unit.getLocation(),
                        " should be in range of ", path.getLocation(),
                        " to disembark.");
                    return MoveType.MOVE_ILLEGAL;
                }

                MoveType mt = unit.getMoveType(d);
                if (!mt.isProgress()) return mt; // Special handling required.

                if (aiUnit.leaveTransport(d)) continue;
                logSB(sb, ", on ", unit.getLocation(), 
                    " failed to disembark to ", path.getTile(), ".");
                return MoveType.MOVE_ILLEGAL;

            } else if (!unit.isOnCarrier() && path.isOnCarrier()) {
                final AIUnit newAICarrier = aiUnit.getTransport();
                if (newAICarrier == null) {
                    logSB(sb, " at " + unit.getLocation()
                        + " requires transport to " + target, ".");
                    return MoveType.MOVE_ILLEGAL;
                }
                final Unit newCarrier = newAICarrier.getUnit();
                if (!newCarrier.isAtLocation(path.getLocation())) {
                    logSB(sb, ", at ", unit.getLocation(),
                        " waiting for carrier ", newCarrier,
                        " to arrive and transport it to ", target, ".");
                    return MoveType.MOVE_ILLEGAL;
                }
                UnitLocation.NoAddReason reason
                    = newCarrier.getNoAddReason(unit);
                switch (reason) {
                case NONE:
                    break;
                case CAPACITY_EXCEEDED:
                    logSB(sb, ", at ", unit.getLocation(),
                        " waiting for space on ", newCarrier, ".");
                    return MoveType.MOVE_NO_MOVES;
                default:
                    logSB(sb, ", at ", unit.getLocation(),
                        " unexpected boarding problem ", reason, ".");
                    return MoveType.MOVE_ILLEGAL;
                }
                Direction d;
                if (unit.isAtLocation(path.getLocation())) {
                    d = null;
                } else if (unit.hasTile()
                    && unit.getTile().isAdjacent(newCarrier.getTile())) {
                    d = unit.getTile().getDirection(newCarrier.getTile());
                } else {
                    logSB(sb, ", at ", unit.getLocation(),
                        " should be in range of ", path.getLocation(),
                        " to embark to ", newCarrier, ".");
                    return MoveType.MOVE_ILLEGAL;
                }
                if (unit.getMovesLeft() <= 0) {
                    logSB(sb, ", waiting at ", unit.getLocation(),
                        " to embark on ", newCarrier, ".");
                    return MoveType.MOVE_NO_MOVES;
                }
                if (aiUnit.joinTransport(newCarrier, d)) continue;
                logSB(sb, ", at ", unit.getLocation(),
                    " failed to embark on ", newCarrier,
                    " at " + newCarrier.getLocation(), ".");
                return MoveType.MOVE_ILLEGAL;
            }

            // Post-sanitization there are only two valid cases,
            // not using Europe at all, and sailing to/from Europe.
            // Ignore the trivial path within Europe.
            // On success, continue or return.
            // On failure, fall through to the error report.
            switch (useEurope) {
            case NO_EUROPE:
                MoveType mt = unit.getMoveType(path.getDirection());
                if (mt == MoveType.MOVE_NO_MOVES) {
                    unit.setMovesLeft(0);
                    logSB(sb, ", reached ", unit.getLocation(), ".");
                    return MoveType.MOVE_NO_MOVES;
                }
                if (!mt.isProgress()) return mt; // Special handling required.
                if (aiUnit.move(path.getDirection())) continue;
                break;

            case USES_EUROPE:
                Location dst = (unit.isInEurope()) ? unit.getGame().getMap()
                    : path.getLocation();
                if (AIMessage.askMoveTo(aiUnit, dst)) {
                    logSB(sb, ", on the high seas.");
                    return MoveType.MOVE_HIGH_SEAS;
                }
                break;

            case BOTH_EUROPE:
                continue; // Do nothing, on to next node.

            default:
                throw new IllegalStateException("Can not happen");
            }
            logSB(sb, ", at ", unit.getLocation(),
                " failed to move to ", path.getLocation(), ".");
            return MoveType.MOVE_ILLEGAL;
        }
        return MoveType.MOVE; // Must have completed path normally, no log.
    }

    /**
     * If the unit in this mission is currently being transported, retarget
     * its transport mission as needed.
     *
     * @return True if the transport is retargeted.
     */
    public boolean retargetTransportable() {
        Unit u = getUnit();
        AIUnit aiUnit = getAIUnit();
        if (aiUnit == null) return false;
        AIUnit aiCarrier = (u.isOnCarrier())
            ? getAIMain().getAIUnit(u.getCarrier())
            : aiUnit.getTransport();
        if (aiCarrier == null) return false;
        Mission m = aiCarrier.getMission();
        if (!(m instanceof TransportMission)) return false;

        return ((TransportMission)m).requeueTransportable(aiUnit);
    }

    /**
     * Retarget a mission because of some problem.
     *
     * @param reason The reason for the retarget.
     * @param sb An optional <code>StringBuilder</code> to log to.
     * @return True if a non-null target was found.
     */
    public boolean retargetMission(String reason, StringBuilder sb) {
        final AIUnit aiu = getAIUnit();
        final Location loc = aiu.getTransportSource();
        String claim = (loc == null) ? "no-loc"
            : (!getPlayer().isEuropean()) ? "native"
            : (((EuropeanAIPlayer)getAIPlayer()).claimTransportable(aiu, loc))
            ? "cancelled"
            : "cancel-failed";
        Location newTarget = findTarget();
        if (newTarget == null) {
            setTarget(null);
            logSB(sb, ", retarget(", reason, ") failed.");
            return false;
        }
        setTarget(newTarget);
        logSB(sb, ", retargeted(", reason, ") to ", newTarget,
            " (transport ", claim, ")");
        return true;
    }

    // Fake partial implementation of Transportable interface.
    // Missions are not actually Transportables but the units that are
    // performing a mission often determine their destination and priority
    // from the mission undertaken.

    /**
     * Gets the destination of a required transport.
     *
     * Override this in the child mission classes if there is a useful
     * intermediate point to deliver the unit distinct from the
     * target.
     *
     * @return The mission target, or null if the mission is invalid,
     *     otherwise lacks a target (e.g. UnitWanderHostile), or the
     *     unit does not need transport.
     */
    public Location getTransportDestination() {
        Location loc;
        return (!isValid()) ? null
            : ((loc = getTarget()) == null) ? null
            : (!getUnit().shouldTakeTransportTo(loc)) ? null
            : loc;
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
     * Sets the target of this mission, if any.
     *
     * @param target The new target of this mission, or null if none.
     */
    public abstract void setTarget(Location target);

    /**
     * Finds a new target for this mission.
     *
     * @return A new target for this mission.
     */
    public abstract Location findTarget();

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
     *
     * @param sb An optional <code>StringBuilder</code> to log to.
     * @return The <code>Mission</code> to continue with, or null
     *     if the current mission has completed.
     * @return 
     */
    public abstract Mission doMission(StringBuilder sb);


    // Serialization

    /**
     * {@inheritDoc}
     */
    @Override
    public final void toXML(FreeColXMLWriter xw) throws XMLStreamException {
        if (isValid()) toXML(xw, getXMLTagName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        // This routine might look redundant, but if you let it
        // default out up the tree, you reach
        // FreeColObject.writeAttributes, which complains about
        // objects without an identifier.  Missions do not have
        // identifiers.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        // This routine might look redundant, but if you let it
        // default out up the tree, you reach
        // FreeColObject.readAttributes, which expects to find an id
        // attribute.  Missions do not have ids.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(24);
        sb.append(Utils.lastPart(getClass().getName(), "."))
            .append("@").append(hashCode())
            .append("-").append(aiUnit.getUnit().toShortString());
        Location target = getTarget();
        if (target != null) sb.append("->").append(target.toShortString());
        return sb.toString();
    }
}
