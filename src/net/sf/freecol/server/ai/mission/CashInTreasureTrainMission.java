/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.pathfinding.CostDecider;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.common.model.pathfinding.GoalDeciders;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIUnit;


/**
 * Mission for cashing in a treasure train.
 * FIXME: acquire protection
 * FIXME: Better avoidance of enemy units
 */
public final class CashInTreasureTrainMission extends Mission {

    private static final Logger logger = Logger.getLogger(CashInTreasureTrainMission.class.getName());

    public static final String TAG = "cashInTreasureTrainMission";

    /** The tag for this mission. */
    private static final String tag = "AI treasureTrain";

    /**
     * The location to cash this treasure train in at, either a Colony
     * or Europe.
     */
    private Location target;


    /**
     * Creates a mission for the given {@code AIUnit}.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The {@code AIUnit} this mission is created for.
     * @param target The target {@code Location} for this mission.
     */
    public CashInTreasureTrainMission(AIMain aiMain, AIUnit aiUnit,
                                      Location target) {
        super(aiMain, aiUnit);

        setTarget(target);
    }

    /**
     * Creates a new {@code CashInTreasureTrainMission} from a reader.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The {@code AIUnit} this mission is created for.
     * @param xr The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public CashInTreasureTrainMission(AIMain aiMain, AIUnit aiUnit,
                                      FreeColXMLReader xr) throws XMLStreamException {
        super(aiMain, aiUnit);

        readFromXML(xr);
    }

    /**
     * Assign a carrier for this treasure.
     *
     * @param lb A {@code LogBuilder} to log to.
     * @return A suitable carrier {@code AIUnit}, to which this unit
     *     has been queued for transport.
     */
    private AIUnit assignCarrier(LogBuilder lb) {
        final AIUnit aiUnit = getAIUnit();
        final Unit unit = getUnit();
        final Player player = unit.getOwner();
        final Europe europe = player.getEurope();

        List<Unit> carriers = player.getCarriersForUnit(unit);
        if (carriers.isEmpty()) return null;

        // Pick the closest carrier and queue this unit.
        final Location here = unit.getLocation();
        final Comparator<Unit> comp = cachingIntComparator(u ->
            u.getTurnsToReach(here));
        Unit closest = minimize(carriers, comp);
        final AIMain aiMain = getAIMain();
        TransportMission tm;
        AIUnit aiCarrier;
        if (closest != null
            && (aiCarrier = aiMain.getAIUnit(closest)) != null
            && (tm = aiCarrier.getMission(TransportMission.class)) != null) {
            setTarget(europe);
            aiUnit.changeTransport(aiCarrier);
            if (tm.forceCollection(aiUnit, lb)) {
                lb.add(" forced collection on ", aiCarrier.getUnit());
                return aiCarrier;
            }
        }
        return null;
    }

    /**
     * Extract a valid target for this mission from a path.
     *
     * @param aiUnit A {@code AIUnit} to perform the mission.
     * @param path A {@code PathNode} to extract a target from,
     *     (uses the unit location if null).
     * @return A target for this mission, or null if none found.
     */
    public static Location extractTarget(AIUnit aiUnit, PathNode path) {
        if (path == null) return null;
        final Location loc = path.getLastNode().getLocation();
        Colony colony = loc.getColony();
        return (loc instanceof Europe
            && invalidMissionReason(aiUnit, loc) == null) ? loc
            : (colony != null
                && invalidMissionReason(aiUnit, colony) == null) ? colony
            : null;
    }

    /**
     * Evaluate a potential cashin mission for a given unit and
     * path.
     *
     * @param aiUnit The {@code AIUnit} to do the mission.
     * @param path A {@code PathNode} to take to the target.
     * @return A score for the proposed mission.
     */
    public static int scorePath(AIUnit aiUnit, PathNode path) {
        Location loc;
        if (path == null
            || (loc = extractTarget(aiUnit, path)) == null
            || (loc instanceof Colony
                && invalidFullColonyReason(aiUnit, loc.getColony()) != null))
            return Integer.MIN_VALUE;
        return aiUnit.getUnit().getTreasureAmount() / (path.getTotalTurns()+1);
    }

    /**
     * Makes a goal decider that checks cash in sites.
     *
     * @param aiUnit The {@code AIUnit} to search with.
     * @param deferOK Keep track of the nearest colonies to use as a
     *     fallback destination.
     * @return A suitable {@code GoalDecider}.
     */
    private static GoalDecider getGoalDecider(final AIUnit aiUnit,
                                              final boolean deferOK) {
        GoalDecider gd = new GoalDecider() {
                private PathNode bestPath = null;
                private int bestValue = Integer.MIN_VALUE;
                
                @Override
                public PathNode getGoal() { return bestPath; }
                @Override
                public boolean hasSubGoals() { return true; }
                @Override
                public boolean check(Unit u, PathNode path) {
                    Location loc = extractTarget(aiUnit, path);
                    if ((loc instanceof Colony
                            && invalidFullColonyReason(aiUnit, (Colony)loc)
                            == null)
                        || loc instanceof Europe) {
                        int value = scorePath(aiUnit, path);
                        if (bestValue < value) {
                            bestValue = value;
                            bestPath = path;
                            return true;
                        }
                    }
                    return false;
                }
            };
        return (deferOK) ? GoalDeciders.getComposedGoalDecider(false, gd,
            GoalDeciders.getOurClosestSettlementGoalDecider())
            : gd;
    }

    /**
     * Find a suitable cash in location for this unit.
     *
     * @param aiUnit The {@code AIUnit} to execute this mission.
     * @param range The maximum number of moves to search.
     * @param deferOK Enables deferring to a fallback colony.
     * @return A {@code PathNode} to the target, or null if not found
     *     which includes the case when Europe should be preferred (because
     *     the unit can not get there by itself).
     */
    private static PathNode findTargetPath(AIUnit aiUnit, int range,
                                           boolean deferOK) {
        if (invalidAIUnitReason(aiUnit) != null) return null;
        final Unit unit = aiUnit.getUnit();
        final Location start = unit.getPathStartLocation();
        final Player player = unit.getOwner();
        final Europe europe = player.getEurope();
        final Unit carrier = unit.getCarrier();
        final CostDecider standardCd
            = CostDeciders.avoidSettlementsAndBlockingUnits();

        if (!player.hasSettlements() || start == null) {
            // No settlements or not on the map, so go straight to
            // Europe.  If Europe does not exist then this mission is
            // doomed.
            return (europe == null) ? null
                : unit.findPath(unit.getLocation(), europe, carrier,
                                standardCd, null);
        }

        // Can the unit get to a cash in site?
        return unit.search(start, getGoalDecider(aiUnit, deferOK),
                           standardCd, range, carrier);
    }

    /**
     * Finds a suitable cashin target for the supplied unit.
     *
     * @param aiUnit The {@code AIUnit} to test.
     * @param range The maximum number of moves to search.
     * @param deferOK Enables deferring to a fallback colony.
     * @return A {@code PathNode} to the target, or null if none found.
     */
    public static Location findMissionTarget(AIUnit aiUnit, int range, 
                                             boolean deferOK) {
        PathNode path = findTargetPath(aiUnit, range, deferOK);
        return (path != null) ? extractTarget(aiUnit, path)
            : Location.upLoc(findCircleTarget(aiUnit,
                    getGoalDecider(aiUnit, deferOK), range*3, deferOK));
    }

    /**
     * Why would this mission be invalid with the given unit?
     *
     * @param aiUnit The {@code AIUnit} to test.
     * @return A reason why the mission would be invalid with the unit,
     *     or null if none found.
     */
    private static String invalidUnitReason(AIUnit aiUnit) {
        String reason = invalidAIUnitReason(aiUnit);
        if (reason != null) return reason;
        final Unit unit = aiUnit.getUnit();
        return (!unit.canCarryTreasure()) ? "unit-cannot-carry-treasure"
            : (unit.getTreasureAmount() <= 0) ? "unit-treasure-nonpositive"
            : null;
    }

    /**
     * Why is this mission invalid with a given colony target, given that
     * intermediate colonies are excluded.
     *
     * @param aiUnit The {@code AIUnit} to check.
     * @param colony The potential target {@code Colony}.
     * @return A reason for mission invalidity, or null if none found.
     */
    private static String invalidFullColonyReason(AIUnit aiUnit,
                                                  Colony colony) {
        String reason = invalidTargetReason(colony, 
            aiUnit.getUnit().getOwner());
        return (reason != null)
            ? reason
            : (!aiUnit.getUnit().canCashInTreasureTrain(colony.getTile()))
            ? "cashin-impossible-at-location"
            : null;
    }

    /**
     * Why is this mission invalid with a given colony target, given that
     * intermediate colonies are included.
     *
     * @param aiUnit The {@code AIUnit} to check.
     * @param colony The potential target {@code Colony}.
     * @return A reason for mission invalidity, or null if none found.
     */
    private static String invalidColonyReason(AIUnit aiUnit, Colony colony) {
        return invalidTargetReason(colony, aiUnit.getUnit().getOwner());
    }

    /**
     * Why is this mission invalid with a given Europe target?
     *
     * @param aiUnit The {@code AIUnit} to check.
     * @param europe The potential target {@code Europe}.
     * @return A reason for mission invalidity, or null if none found.
     */
    private static String invalidEuropeReason(AIUnit aiUnit, Europe europe) {
        return invalidTargetReason(europe, aiUnit.getUnit().getOwner());
    }

    /**
     * Why would this mission be invalid with the given AI unit?
     *
     * @param aiUnit The {@code AIUnit} to test.
     * @return A reason for invalidity, or null if none found.
     */
    public static String invalidMissionReason(AIUnit aiUnit) {
        return invalidUnitReason(aiUnit);
    }

    /**
     * Why would this mission be invalid with the given AI unit and location?
     *
     * @param aiUnit The {@code AIUnit} to check.
     * @param loc The {@code Location} to check.
     * @return A reason for invalidity, or null if none found.
     */
    public static String invalidMissionReason(AIUnit aiUnit, Location loc) {
        String reason = invalidMissionReason(aiUnit);
        return (reason != null)
            ? reason
            : (loc instanceof Colony)
            ? invalidColonyReason(aiUnit, (Colony)loc)
            : (loc instanceof Europe)
            ? invalidEuropeReason(aiUnit, (Europe)loc)
            : Mission.TARGETINVALID;
    }


    // Implement Mission
    //   Inherit dispose, getTransportDestination, isOneTime

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBaseTransportPriority() {
        return getUnit().getTreasureAmount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location getTarget() {
        return target;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTarget(Location target) {
        if (target == null
            || target instanceof Europe || target instanceof Colony) {
            this.target = target;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location findTarget() {
        return findMissionTarget(getAIUnit(), 20, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String invalidReason() {
        return invalidMissionReason(getAIUnit(), target);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mission doMission(LogBuilder lb) {
        lb.add(tag);
        String reason = invalidReason();
        if (isTargetReason(reason)) {
            return retargetMission(reason, lb);
        } else if (reason != null) {
            return lbFail(lb, false, reason);
        }

        for (;;) {
            // Go to the target.
            final Unit unit = getUnit();
            Unit.MoveType mt = travelToTarget(getTarget(),
                CostDeciders.avoidSettlementsAndBlockingUnits(), lb);
            switch (mt) {
            case MOVE: // Arrived
                break;

            case MOVE_HIGH_SEAS: case MOVE_NO_MOVES: case MOVE_ILLEGAL:
                return lbWait(lb);

            case MOVE_NO_REPAIR:
                return lbFail(lb, false, AIUNITDIED);

            case MOVE_NO_ACCESS_EMBARK: case MOVE_NO_TILE:
                return this;

            default:
                return lbMove(lb, mt);
            }

            // Cash in now if:
            // - already in Europe
            // - or can never get there
            // - it is free to transport the treasure
            // - or there is no potential carrier to get the treasure to there
            // Otherwise, it is better to send to Europe.
            lbAt(lb);
            final AIUnit aiUnit = getAIUnit();
            final Europe europe = getUnit().getOwner().getEurope();
            if (unit.canCashInTreasureTrain()) {
                AIUnit aiCarrier = null;
                boolean cashin = europe == null || unit.isInEurope()
                    || unit.getTransportFee() == 0;
                if (!cashin && aiUnit.getTransport() == null) {
                    cashin = assignCarrier(lb) == null;
                }
                if (cashin) return (AIMessage.askCashInTreasureTrain(aiUnit))
                                ? lbDone(lb, false, "cashed in")
                                : lbFail(lb, false, "cashin");
            }
            return retargetMission("transport expected", lb);
        }
    }


    // Serialization

    private static final String TARGET_TAG = "target";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        if (target != null) {
            xw.writeAttribute(TARGET_TAG, target.getId());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);
        
        target = xr.getLocationAttribute(getGame(), TARGET_TAG, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return TAG; }
}
