/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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
import net.sf.freecol.common.model.Locatable;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.common.model.pathfinding.CostDecider;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.common.util.LogBuilder;
import static net.sf.freecol.common.util.StringUtils.*;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIObject;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.ai.EuropeanAIPlayer;
import net.sf.freecol.server.ai.TransportableAIObject;


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
    private final AIUnit aiUnit;


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     *
     * Note that missions are attached to their units, and thus do
     * not need AI ids, hence the plain superclass constructor.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     */
    protected Mission(AIMain aiMain, AIUnit aiUnit) {
        super(aiMain);

        this.aiUnit = aiUnit;
        this.uninitialized = aiUnit == null;
    }

    /**
     * Creates a mission for the given <code>AIUnit</code> and target.
     *
     * Note that missions are attached to their units, and thus do
     * not need AI ids, hence the plain superclass constructor.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     * @param target The initial target <code>Location</code>.
     */
    protected Mission(AIMain aiMain, AIUnit aiUnit, Location target) {
        this(aiMain, aiUnit);

        setTarget(target);
        if (aiUnit != null && aiUnit.getMission() != this) {
            aiUnit.changeMission(this);
        }
    }


    /**
     * Gets the AI-unit this mission has been created for.
     *
     * @return The <code>AIUnit</code>.
     */
    public final AIUnit getAIUnit() {
        return aiUnit;
    }

    /**
     * Gets the unit this mission has been created for.
     *
     * @return The <code>Unit</code>.
     */
    public final Unit getUnit() {
        return (aiUnit == null) ? null : aiUnit.getUnit();
    }

    /**
     * Convenience accessor for the owning player.
     *
     * @return The <code>Player</code> that owns the mission unit.
     */
    protected final Player getPlayer() {
        return (getUnit() == null) ? null : getUnit().getOwner();
    }

    /**
     * Convenience accessor for the owning AI player.
     *
     * @return The <code>AIPlayer</code>.
     */
    protected final AIPlayer getAIPlayer() {
        return getAIMain().getAIPlayer(getUnit().getOwner());
    }

    /**
     * Convenience accessor for the owning European AI player.
     *
     * @return The <code>EuropeanAIPlayer</code>.
     */
    protected final EuropeanAIPlayer getEuropeanAIPlayer() {
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
    protected final Random getAIRandom() {
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
    public static boolean isTargetReason(String reason) {
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
            : (target instanceof Europe && !owner.owns((Europe)target))
            ? Mission.TARGETOWNERSHIP
            : (target instanceof Settlement && !owner.owns((Settlement)target))
            ? Mission.TARGETOWNERSHIP
            : null;
    }

    /**
     * Is there a reason to invalidate mission to move a transportable?
     *
     * @param t The <code>TransportableAIObject</code> to check.
     * @return A reason for the transport to be invalid, or null if
     *     none found.
     */
    public static String invalidTransportableReason(TransportableAIObject t) {
        if (t == null) return "null-transportable";
        final Locatable l = t.getTransportLocatable();
        if (l == null) return "null-locatable";

        final Unit carrier = (l.getLocation() instanceof Unit)
            ? (Unit)(l.getLocation()) : null;
        final AIUnit transport = t.getTransport();
        Player owner;
        Location loc;
        boolean checkSrc = transport == null;

        if (carrier != null && transport != null
            && carrier != transport.getUnit()) {
            return "transportable-on-other-carrier";
        }

        if (checkSrc) {
            Settlement s;
            if ((loc = t.getTransportSource()) == null) {
                return "transportable-source-missing-" + t;
            } else if (((FreeColGameObject)loc).isDisposed()) {
                return "transportable-source-disposed";
            } else if (loc instanceof Settlement && l instanceof Ownable
                && !((Ownable)l).getOwner().owns(s = (Settlement)loc)) {
                return "transportable-source-" + s.getName()
                    + "-captured-by-" + s.getOwner().getDebugName();
            }
        } else {
            loc = t.getTransportDestination();
            if (loc != null && ((FreeColGameObject)loc).isDisposed()) {
                return "transportable-destination-disposed";
            }
        }
        return null;
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


    // Mission logging support

    /**
     * State where a unit is.
     *
     * @param lb A <code>LogBuilder</code> to log to.
     * @return This <code>Mission</code>.
     */
    protected Mission lbAt(LogBuilder lb) {
        final Unit unit = getUnit();
        lb.add(", at ", Location.upLoc(unit.getLocation()));
        return this;
    }

    /**
     * State that the unit has made an attack.
     *
     * @param lb A <code>LogBuilder</code> to log to.
     * @param what What is being attacked (a <code>Unit</code> or
     *     <code>Settlement</code>).
     * @return This <code>Mission</code>.
     */
    protected Mission lbAttack(LogBuilder lb, Location what) {
        lb.add(", attacking ", what);
        return this;
    }

    /**
     * State that the unit is dodging.
     *
     * @param lb A <code>LogBuilder</code> to log to.
     * @return This <code>Mission</code>.
     */
    protected Mission lbDodge(LogBuilder lb) {
        final Unit unit = getUnit();
        lb.add(", dodging at ", unit.getLocation());
        unit.setMovesLeft(0);        
        return this;
    }

    /**
     * State that this mission has completed successfully.
     *
     * @param lb A <code>LogBuilder</code> to log to.
     * @param cont If true, the mission should continue, otherwise drop the
     *     mission.
     * @param reasons Reasons for the successful completion.
     * @return The current <code>Mission</code> of the unit, which may
     *     now be different from <code>this</code>.
     */
    protected Mission lbDone(LogBuilder lb, boolean cont, Object... reasons) {
        lb.add(", COMPLETED: ", reasons);
        return (cont) ? aiUnit.getMission() : lbDrop(lb);
    }

    /**
     * Drop the current mission.
     *
     * @param lb A <code>LogBuilder</code> to log to.
     * @param reasons Optional reasons to drop the mission.
     * @return Null, which is now the current <code>Mission</code> of
     *     this unit.
     */
    protected Mission lbDrop(LogBuilder lb, Object... reasons) {
        lb.add(", DROPPED", reasons);
        return (aiUnit == null) ? null : aiUnit.changeMission(null);
    }

    /**
     * The current mission has failed.
     *
     * @param lb A <code>LogBuilder</code> to log to.
     * @param cont If true, the mission should continue, otherwise drop the
     *     mission.
     * @param reasons Reasons for the successful completion.
     * @return The current <code>Mission</code> of the unit, which may
     *     not be different from <code>this</code>.
     */
    protected Mission lbFail(LogBuilder lb, boolean cont, Object... reasons) {
        lb.add(", FAILED: ", reasons);
        return (cont) ? aiUnit.getMission() : lbDrop(lb);
    }

    /**
     * State that a bad move has occurred.
     *
     * @param lb A <code>LogBuilder</code> to log to.
     * @param mt The bad <code>MoveType</code>.
     * @return This <code>Mission</code>.
     */     
    protected Mission lbMove(LogBuilder lb, Unit.MoveType mt) {
        lb.add(", bad move type at ", getUnit().getLocation(), ": ", mt);
        return this;
    }

    /**
     * State that the mission has been retargeted.
     *
     * @param lb A <code>LogBuilder</code> to log to.
     * @return This <code>Mission</code>.
     */
    protected Mission lbRetarget(LogBuilder lb) {
        lb.add(", retargeted ", getTarget());
        return this;
    }

    /**
     * State that the unit is waiting for something.
     *
     * @param lb A <code>LogBuilder</code> to log to.
     * @param reasons Reasons for the wait.
     * @return This <code>Mission</code>.
     */
    protected Mission lbWait(LogBuilder lb, Object... reasons) {
        lb.add(reasons);
        getUnit().setMovesLeft(0);
        return this;
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
            Location blocker = (settlement != null) ? settlement
                : tile.getDefendingUnit(unit);
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
            direction = Direction.getRandomDirection(logMe, logger, aiRandom);
        }

        Direction[] directions
            = direction.getClosestDirections(logMe, logger, aiRandom);
        for (Direction d : directions) {
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
     * - MOVE if the unit has arrived at the target, although it may
     *     have exhausted its moves
     * - MOVE_HIGH_SEAS if the unit has set sail to/from Europe
     * - MOVE_NO_MOVES is underway but ran out of moves
     * - MOVE_NO_ACCESS_EMBARK if progress depends on a carrier, either
     *     currently boarded or due to collect the unit
     * - MOVE_NO_REPAIR if the unit died for whatever reason
     * - MOVE_NO_TILE if there is no path (usually transitory on rivers)
     * - MOVE_ILLEGAL if there is an error or permanent restriction
     * - other legal results (e.g. ENTER_INDIAN_SETTLEMENT*) if that would
     *   occur if the unit proceeded.  Such moves require special handling
     *   and are not performed here, the calling mission code must
     *   handle them.
     *
     * @param target The destination <code>Location</code>.
     * @param costDecider The <code>CostDecider</code> to use in any path
     *     finding.
     * @param lb A <code>LogBuilder</code> to log to.
     * @return The type of move the unit stopped at.
     */
    protected MoveType travelToTarget(Location target, CostDecider costDecider,
                                      LogBuilder lb) {
        if (target == null) return MoveType.MOVE_ILLEGAL;
        final Tile targetTile = target.getTile();
        if (!(target instanceof Europe) && targetTile == null) {
            throw new IllegalStateException("Target neither Europe nor Tile");
        }
        final Unit unit = getUnit();
        final AIUnit aiUnit = getAIUnit();
        AIUnit aiCarrier = aiUnit.getTransport();
        final Map map = unit.getGame().getMap();
        PathNode path = null;
        boolean useTransport = false;
        target = Location.upLoc(target);

        // Consider where the unit is starting.
        if (unit.isAtSea()) {
            // Wait for carrier to arrive on the map or in Europe.
            lb.add(", at sea");
            return MoveType.MOVE_HIGH_SEAS;

        } else if (unit.isOnCarrier()) {
            // Transport mission will disembark the unit when it
            // arrives at the drop point.
            lb.add(", on carrier");
            return MoveType.MOVE_NO_ACCESS_EMBARK;

        } else if (unit.isAtLocation(target)) {
            // Arrived!
            return MoveType.MOVE;

        } else if (unit.isInEurope()) {
            // Leave, or require transport.
            if (!unit.getOwner().canMoveToEurope()) {
                lb.add(", impossible move from Europe");
                return MoveType.MOVE_ILLEGAL;
            }
            if (unit.getType().canMoveToHighSeas()) {
                unit.setDestination(target);
                if (AIMessage.askMoveTo(aiUnit, map)) {
                    lb.add(", sailed for ", target);
                    return MoveType.MOVE_HIGH_SEAS;
                } else {
                    lb.add(", failed to sail for ", target);
                    return MoveType.MOVE_ILLEGAL;
                }
            }
            useTransport = true;

        } else if (!unit.hasTile()) {
            // Fail!
            return MoveType.MOVE_ILLEGAL;

        } else {
            // On map.  Either find a path or decide to use transport.
            if (target instanceof Europe) {
                // Going to Europe.
                if (!unit.getOwner().canMoveToEurope()) {
                    lb.add(", impossible move to Europe");
                    return MoveType.MOVE_ILLEGAL;
                }
                if (!unit.getType().canMoveToHighSeas()
                    || aiCarrier != null) {
                    useTransport = true;
                } else {
                    path = unit.findPath(unit.getLocation(), target,
                                         null, costDecider);
                }
            } else if (aiCarrier != null) {
                // Transport already allocated.
                useTransport = true;

            } else if (!unit.getType().canMoveToHighSeas()
                && !Map.isSameContiguity(target, unit.getLocation())) {
                // Transport necessary.
                useTransport = true;

            } else {
                // Should not need transport within the same contiguity.
                path = unit.findPath(unit.getLocation(), target,
                                     null, costDecider);
            }
        }

        if (useTransport) {
            if (aiCarrier != null) {
                // A carrier has been assigned.  Try to go to the
                // collection point.
                Location pick;
                TransportMission tm;
                boolean waiting = false;
                PathNode ownPath;
                int pathTurns, ownTurns;

                if ((tm = aiCarrier.getMission(TransportMission.class)) == null) {
                    // Carrier has no transport mission?!?  Bogus.
                    lb.add(", had bogus carrier ", aiCarrier.getUnit());
                    logger.warning(unit + " has transport " + aiCarrier
                        + " without transport mission");
                    aiUnit.dropTransport();
                    aiCarrier = null;

                } else if ((pick = tm.getTransportTarget(aiUnit)) == null) {
                    // No collection point for this unit?  Bogus.
                    lb.add(", had bogus transport on ", aiCarrier.getUnit());
                    logger.warning(unit + " has transport " + aiCarrier
                        + " with transport mission but null transport target\n"
                        + tm.toFullString());
                    aiUnit.dropTransport();
                    aiCarrier = null;

                } else if (Map.isSameLocation(pick, unit.getLocation())) {
                    // Waiting for the carrier at the collection point.
                    waiting = true;

                } else if ((path = unit.findPath(unit.getLocation(), pick,
                                                 null, costDecider)) == null) {
                    // No path to the collection point.
                    lbAt(lb);
                    lb.add(", no path to meet ", aiCarrier.getUnit(),
                           " at ", pick);
                    path = unit.findPath(unit.getLocation(), target,
                                         null, costDecider);
                    if (path == null) {
                        // Unable to fall back to going direct.
                        // Return failure in the hope that it is a
                        // transient blockage.
                        return MoveType.MOVE_NO_TILE;
                    }
                    // Fall back to going direct to the target.
                    lb.add(", dropped carrier");
                    aiUnit.dropTransport();
                    aiCarrier = null;
                    useTransport = false;

                } else if ((ownPath = unit.findPath(unit.getLocation(), 
                            target, null, costDecider)) == null
                    || (ownTurns = ownPath.getTotalTurns())
                    > (pathTurns = path.getTotalTurns())) {
                    // Either there is no direct path to the target or
                    // a path exists but takes longer than using the
                    // carrier.  This confirms that it is not only
                    // possible to travel to the collection point, it
                    // is also the best plan.
                    MoveType ret = followMapPath(path.next, lb);
                    if (ret != MoveType.MOVE) return ret;
                    waiting = true; // Arrived for collection.

                } else {
                    // It is quicker to cancel the transport and go to
                    // the target directly.
                    lb.add(", dropping carrier", aiCarrier.getUnit(),
                        " as it is faster (", ownTurns, "<", pathTurns,
                        " without it");
                    aiUnit.dropTransport();
                    aiCarrier = null;
                    path = ownPath;
                    useTransport = false;
                }

                if (waiting) {
                    // If waiting for the carrier, signal that this
                    // unit can be reexamined if the carrier is still
                    // moving.
                    lbAt(lb);
                    lb.add(", wait for ", aiCarrier.getUnit());
                    return (aiCarrier.getUnit().getMovesLeft() > 0)
                        ? MoveType.MOVE_NO_ACCESS_EMBARK
                        : MoveType.MOVE_NO_MOVES;
                }
            }

            if (useTransport && aiCarrier == null) {
                // Still interested in transport but no carrier.
                lb.add(", needs transport to ", target);
                return MoveType.MOVE_NO_ACCESS_EMBARK;
            }
        }

        // Follow the path to the target.  If there is one.
        if (path == null) {
            lbAt(lb);
            lb.add(", no path to ", target);
            return MoveType.MOVE_NO_TILE;
        }
        if (path.next == null) {
            // This should not happen, the isAtLocation() test above
            // should have succeeded.
            throw new IllegalStateException("Trivial path found "
                + path.fullPathToString()
                + " from " + unit.getLocation() + " to target " + target
                + " result=" +  unit.isAtLocation(target));
        }            
        return followMapPath(path.next, lb);
    }

    /**
     * Follow a path that is on the map (except perhaps the last node)
     * and does not use a carrier.
     *
     * @param path The <code>PathNode</code> to follow.
     * @param lb A <code>LogBuilder</code> to log to.
     * @return The type of move the unit stopped at.
     */
    private MoveType followMapPath(PathNode path, LogBuilder lb) {
        final Unit unit = getUnit();
        final AIUnit aiUnit = getAIUnit();
        final Location target = path.getLastNode().getLocation();

        for (; path != null; path = path.next) {
            // Check for immediate failure
            if (unit.isDisposed()) {
                lb.add(", died going to ", Location.upLoc(path.getLocation()));
                return MoveType.MOVE_NO_REPAIR;
            } else if (unit.getMovesLeft() <= 0) {
                lbAt(lb);
                lb.add(", en route to ", Location.upLoc(target));
                return MoveType.MOVE_NO_MOVES;
            }

            if (path.getLocation() instanceof Europe) {
                if (AIMessage.askMoveTo(aiUnit, path.getLocation())) {
                    lb.add(", sailed to Europe");
                    return MoveType.MOVE_HIGH_SEAS;
                } else {
                    lb.add(", failed to sail for Europe");
                    return MoveType.MOVE_ILLEGAL;
                }
            }
            MoveType mt = unit.getMoveType(path.getDirection());
            if (mt == MoveType.MOVE_NO_MOVES) {
                unit.setMovesLeft(0);
                lbAt(lb);
                return MoveType.MOVE_NO_MOVES;
            }
            if (!mt.isProgress()) {
                return mt; // Special handling required, no log.
            }
            if (!aiUnit.move(path.getDirection())) {
                lbAt(lb);
                lb.add(", failed to move to ",
                    Location.upLoc(path.getLocation()));
                return MoveType.MOVE_ILLEGAL;
            }
        }
        return MoveType.MOVE; // Must have completed path normally, no log.
    }

    /**
     * Retarget a mission because of some problem.
     *
     * @param reason The reason for the retarget.
     * @param lb A <code>LogBuilder</code> to log to.
     * @return The current <code>Mission</code>, which has been set to
     *     null on failure to retarget.
     */
    public Mission retargetMission(String reason, LogBuilder lb) {
        lb.add(", failing(", reason, ")");

        Location newTarget = findTarget();
        if (newTarget != null) {
            setTarget(newTarget);
            return lbRetarget(lb);
        }
        lb.add(", retarget failed");
        return lbDrop(lb);
    }


    // Mission interface to be implemented/overridden by descendants.
    // TransportableAIObject delegates some functionality here when
    // a mission is available.
 
    /**
     * Disposes this mission by removing any references to it.
     */
    @Override
    public void dispose() {
        // Nothing to do yet.
    }

    /**
     * Get the base transport priority for the unit performing this mission.
     *
     * @return A base transport priority.
     */
    public int getBaseTransportPriority() {
        return 0;
    }
        
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
     * Should this mission be considered a mere fallback to be replaced
     * at will?
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
     * Performs the mission.
     *
     * @param lb A <code>LogBuilder</code> to log to.
     * @return The <code>Mission</code> to continue with, or null
     *     if the current mission has completed.
     */
    public abstract Mission doMission(LogBuilder lb);


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
        LogBuilder lb = new LogBuilder(64);
        lb.add(lastPart(getClass().getName(), "."), "@", hashCode(),
               "-", aiUnit.getUnit(), "->", getTarget());
        return lb.toString();
    }
}
