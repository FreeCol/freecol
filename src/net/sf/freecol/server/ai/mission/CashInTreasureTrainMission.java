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

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.pathfinding.CostDecider;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.common.model.pathfinding.GoalDeciders;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIUnit;


/**
 * Mission for cashing in a treasure train.
 * TODO: Require protection
 * TODO: Better avoidance of enemy units
 */
public class CashInTreasureTrainMission extends Mission {

    private static final Logger logger = Logger.getLogger(CashInTreasureTrainMission.class.getName());

    /** The tag for this mission. */
    private static final String tag = "AI treasureTrain";

    /**
     * The location to cash this treasure train in at, either a Colony
     * or Europe.
     */
    private Location target = null;


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     * @param target The target <code>Location</code> for this mission.
     */
    public CashInTreasureTrainMission(AIMain aiMain, AIUnit aiUnit,
                                      Location target) {
        super(aiMain, aiUnit);

        setTarget(target);
        logger.finest(tag + " starts at " + aiUnit.getUnit().getLocation()
            + " with target " + target + ": " + this);
        uninitialized = false;
    }

    /**
     * Creates a new <code>CashInTreasureTrainMission</code> and reads
     * the given element.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
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
     * Extract a valid target for this mission from a path.
     *
     * @param aiUnit A <code>AIUnit</code> to perform the mission.
     * @param path A <code>PathNode</code> to extract a target from,
     *     (uses the unit location if null).
     * @return A target for this mission, or null if none found.
     */
    public static Location extractTarget(AIUnit aiUnit, PathNode path) {
        if (path == null) return null;
        final Location loc = path.getLastNode().getLocation();
        Colony colony = loc.getColony();
        return (loc instanceof Europe
            && invalidReason(aiUnit, loc) == null) ? loc
            : (colony != null
                && invalidReason(aiUnit, colony) == null) ? colony
            : null;
    }

    /**
     * Evaluate a potential cashin mission for a given unit and
     * path.
     *
     * @param aiUnit The <code>AIUnit</code> to do the mission.
     * @param path A <code>PathNode</code> to take to the target.
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
     * @param aiUnit The <code>AIUnit</code> to search with.
     * @param deferOK Keep track of the nearest colonies to use as a
     *     fallback destination.
     * @return A suitable <code>GoalDecider</code>.
     */
    private static GoalDecider getGoalDecider(final AIUnit aiUnit,
                                              final boolean deferOK) {
        GoalDecider gd = new GoalDecider() {
            private PathNode bestPath = null;
            private int bestValue = 0;

            public PathNode getGoal() { return bestPath; }
            public boolean hasSubGoals() { return true; }
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
        return (deferOK) ? GoalDeciders.getComposedGoalDecider(gd,
            GoalDeciders.getOurClosestSettlementGoalDecider())
            : gd;
    }

    /**
     * Find a suitable cash in location for this unit.
     *
     * @param aiUnit The <code>AIUnit</code> to execute this mission.
     * @param range The maximum number of moves to search.
     * @param deferOK Enables deferring to a fallback colony.
     * @return A <code>PathNode</code> to the target, or null if not found
     *     which includes the case when Europe should be preferred (because
     *     the unit can not get there by itself).
     */
    private static PathNode findTargetPath(AIUnit aiUnit, int range,
                                           boolean deferOK) {
        if (invalidAIUnitReason(aiUnit) != null) return null;
        final Unit unit = aiUnit.getUnit();
        final Tile startTile = unit.getPathStartTile();
        if (startTile == null) return null;
        
        // Not on the map?  Europe *must* be viable, so go there
        // (return null for now, path still impossible).
        PathNode path;
        final Player player = unit.getOwner();
        final Europe europe = player.getEurope();
        final Unit carrier = unit.getCarrier();
        final CostDecider standardCd
            = CostDeciders.avoidSettlementsAndBlockingUnits();

        if (player.getNumberOfSettlements() <= 0) {
            // No settlements, so go straight to Europe.  If Europe does
            // not exist then this mission is doomed.
            return (europe == null) ? null
                : unit.findPath(startTile, europe, carrier, standardCd);
        }

        // Can the unit get to a cash in site?
        final GoalDecider gd = getGoalDecider(aiUnit, deferOK);
        return unit.search(startTile, gd, standardCd, range, carrier);
    }

    /**
     * Finds a suitable cashin target for the supplied unit.
     *
     * @param aiUnit The <code>AIUnit</code> to test.
     * @param range The maximum number of moves to search.
     * @param deferOK Enables deferring to a fallback colony.
     * @return A <code>PathNode</code> to the target, or null if none found.
     */
    public static Location findTarget(AIUnit aiUnit, int range, 
                                      boolean deferOK) {
        final Player player = aiUnit.getUnit().getOwner();
        PathNode path = findTargetPath(aiUnit, range, deferOK);
        return (path != null) ? extractTarget(aiUnit, path)
            : upLoc(findCircleTarget(aiUnit, getGoalDecider(aiUnit, deferOK),
                                     range*3, deferOK));
    }


    // Fake Transportable interface

    /**
     * {@inheritDoc}
     */
    @Override
    public Location getTransportDestination() {
        return (getUnit().shouldTakeTransportTo(getTarget())) ? getTarget()
            : null;
    }

    /**
     * {@inheritDoc}
     */
    public int getTransportPriority() {
        return (getTransportDestination() == null) ? 0
            : getUnit().getTreasureAmount();
    }


    // Mission interface

    /**
     * {@inheritDoc}
     */
    public Location getTarget() {
        return target;
    }

    /**
     * {@inheritDoc}
     */
    public void setTarget(Location target) {
        if (target == null
            || target instanceof Europe || target instanceof Colony) {
            boolean retarget = this.target != null && this.target != target;
            this.target = target;
            if (retarget) retargetTransportable();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Location findTarget() {
        return findTarget(getAIUnit(), 20, true);
    }

    /**
     * Why would this mission be invalid with the given unit?
     *
     * @param aiUnit The <code>AIUnit</code> to test.
     * @return A reason why the mission would be invalid with the unit,
     *     or null if none found.
     */
    private static String invalidMissionReason(AIUnit aiUnit) {
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
     * @param aiUnit The <code>AIUnit</code> to check.
     * @param colony The potential target <code>Colony</code>.
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
     * @param aiUnit The <code>AIUnit</code> to check.
     * @param colony The potential target <code>Colony</code>.
     * @return A reason for mission invalidity, or null if none found.
     */
    private static String invalidColonyReason(AIUnit aiUnit, Colony colony) {
        return invalidTargetReason(colony, aiUnit.getUnit().getOwner());
    }

    /**
     * Why is this mission invalid with a given Europe target?
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @param europe The potential target <code>Europe</code>.
     * @return A reason for mission invalidity, or null if none found.
     */
    private static String invalidEuropeReason(AIUnit aiUnit, Europe europe) {
        return invalidTargetReason(europe, aiUnit.getUnit().getOwner());
    }

    /**
     * Why would this mission be invalid with the given AI unit and location?
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @param loc The <code>Location</code> to check.
     * @return A reason for invalidity, or null if none found.
     */
    public static String invalidReason(AIUnit aiUnit, Location loc) {
        String reason = invalidMissionReason(aiUnit);
        return (reason != null)
            ? reason
            : (loc instanceof Colony)
            ? invalidColonyReason(aiUnit, (Colony)loc)
            : (loc instanceof Europe)
            ? invalidEuropeReason(aiUnit, (Europe)loc)
            : Mission.TARGETINVALID;
    }

    /**
     * Why would this mission be invalid with the given AI unit?
     *
     * @param aiUnit The <code>AIUnit</code> to test.
     * @return A reason for invalidity, or null if none found.
     */
    public static String invalidReason(AIUnit aiUnit) {
        return invalidMissionReason(aiUnit);
    }

    /**
     * {@inheritDoc}
     */
    public String invalidReason() {
        return invalidReason(getAIUnit(), target);
    }

    // Not a one-time mission, omit isOneTime().
    
    /**
     * {@inheritDoc}
     */
    public void doMission() {
        final Unit unit = getUnit();
        String reason = invalidReason();
        if (isTargetReason(reason)) {
            if (!retargetMission(tag, reason)) return;
        } else if (reason != null) {
            logger.finest(tag + " broken(" + reason + "): " + this);
            return;
        }

        // Go to the target.
        if (travelToTarget(tag, getTarget(),
                CostDeciders.avoidSettlementsAndBlockingUnits())
            != Unit.MoveType.MOVE) return;

        // Cash in now if:
        // - already in Europe
        // - or can never get there
        // - or there is no potential carrier to get the treasure to there
        // - or if the transport fee is not in effect.
        // Otherwise, it is better to send to Europe.
        final AIUnit aiUnit = getAIUnit();
        final Player player = unit.getOwner();
        final Europe europe = player.getEurope();
        if (unit.canCashInTreasureTrain()) {
            if (unit.isInEurope()
                || europe == null
                || unit.getTransportFee() == 0
                || player.getCarriersForUnit(unit).isEmpty()) {
                if (AIMessage.askCashInTreasureTrain(aiUnit)) {
                    logger.finest(tag + " completed cash in at "
                        + upLoc(unit.getLocation()) + ": " + this);
                } else {
                    logger.warning(tag + " failed to cash in at "
                        + upLoc(unit.getLocation()) + ": " + this);
                }
            } else {
                setTarget(europe);
                logger.finest(tag + " at " + upLoc(unit.getLocation())
                    + " retargeting Europe: " + this);
            }
        } else {
            retargetMission(tag, "arrived at "
                + unit.getLocation().getColony().getName());
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
        
        target = xr.findLocationAttribute(getGame(), TARGET_TAG);
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "cashInTreasureTrainMission".
     */
    public static String getXMLElementTagName() {
        return "cashInTreasureTrainMission";
    }
}
