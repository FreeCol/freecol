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

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

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

    private static final String tag = "AI treasureTrain";

    /** Maximum number of turns to travel to a cash in location. */
    private static final int MAX_TURNS = 20;

    /**
     * The location to cash this treasure train in at, either a Colony
     * or Europe.
     */
    private Location target = null;


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission
     *        is created for.
     */
    public CashInTreasureTrainMission(AIMain aiMain, AIUnit aiUnit) {
        super(aiMain, aiUnit);
        target = findTarget(aiUnit, true);
        logger.finest(tag + " starts at " + aiUnit.getUnit().getLocation()
            + " with target " + target + ": " + this);
        uninitialized = false;
    }

    /**
     * Creates a new <code>CashInTreasureTrainMission</code> and reads
     * the given element.
     *
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public CashInTreasureTrainMission(AIMain aiMain, XMLStreamReader in)
        throws XMLStreamException {
        super(aiMain);

        readFromXML(in);
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
            || !(invalidFullColonyReason(aiUnit, loc.getColony()) == null
                || loc instanceof Europe))
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
     * @param aiUnit The <code>AIUnit</code> to execute a cash in mission.
     * @param deferOK Enables deferring to a fallback colony.
     * @return A <code>PathNode</code> to the target, or null if not found
     *     which includes the case when Europe should be preferred (because
     *     the unit can not get there by itself).
     */
    private static PathNode findTargetPath(AIUnit aiUnit, boolean deferOK) {
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
        final CostDecider relaxedCd = CostDeciders.numberOfTiles();

        if (player.getNumberOfSettlements() <= 0) {
            // No settlements, so go straight to Europe.  If Europe does
            // not exist then this mission is doomed.  If a carrier is
            // present use it, otherwise relax the cost decider and at least
            // start working on a path.
            return (europe == null)
                ? null // Nowhere suitable at all!
                : (carrier != null)
                ? unit.findFullPath(startTile, europe, carrier, standardCd)
                : unit.findFullPath(startTile, europe, null, relaxedCd);
        }

        // Can the unit get to a cash in site?
        final GoalDecider gd = getGoalDecider(aiUnit, deferOK);
        path = unit.searchFullPath(startTile, gd, standardCd,
                                   MAX_TURNS, carrier);
        if (path != null) return path;

        // One more try with a relaxed cost decider and no range limit.
        path = unit.searchFullPath(startTile, gd, relaxedCd,
                                   INFINITY, carrier);
        if (path == null) {
            throw new IllegalStateException("Should have found a path!");
        }
        return path;
    }

    /**
     * Finds a suitable cashin target for the supplied unit.
     *
     * @param aiUnit The <code>AIUnit</code> to test.
     * @param deferOK Enables deferring to a fallback colony.
     * @return A <code>PathNode</code> to the target, or null if none found.
     */
    public static Location findTarget(AIUnit aiUnit, boolean deferOK) {
        final Player player = aiUnit.getUnit().getOwner();
        PathNode path = findTargetPath(aiUnit, deferOK);
        return (path != null) ? extractTarget(aiUnit, path)
            : null;
    }


    // Fake Transportable interface

    /**
     * Gets the transport destination for units with this mission.
     *
     * @return The destination for this <code>Transportable</code>.
     */
    @Override
    public Location getTransportDestination() {
        return (target instanceof Europe
            || (target instanceof Colony
                && shouldTakeTransportToTile(target.getTile()))) ? target
            : null;
    }

    /**
     * Gets the priority of getting the unit to the transport
     * destination.
     *
     * @return The priority.
     */
    public int getTransportPriority() {
        return (getTransportDestination() == null) ? 0
            : getUnit().getTreasureAmount();
    }


    // Mission interface

    /**
     * Gets the location we are aiming to cash in at.
     *
     * @return The location we are aiming to cash in at.
     */
    public Location getTarget() {
        return target;
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
     * Why is this mission invalid?
     *
     * @return A reason for mission invalidity, or null if none found.
     */
    public String invalidReason() {
        return invalidReason(getAIUnit(), target);
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

    // Not a one-time mission, omit isOneTime().
    
    /**
     * Performs this mission.
     */
    public void doMission() {
        final Unit unit = getUnit();
        String reason = invalidReason();
        if (isTargetReason(reason)) {
            if ((target = findTarget(getAIUnit(), true)) == null) {
                logger.finest(tag + " could not retarget: " + this);
                return;
            }
        } else if (reason != null) {
            logger.finest(tag + " broken(" + reason + "): " + this);
            return;
        }

        // Go to the target.
        if (travelToTarget(tag, target) != Unit.MoveType.MOVE) return;

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
                || Player.getCarriersForUnit(unit).isEmpty()
                || unit.getTransportFee() == 0) {
                if (AIMessage.askCashInTreasureTrain(aiUnit)) {
                    logger.finest(tag + " completed cash in at "
                        + unit.getLocation() + ": " + this);
                }
            } else {
                target = europe;
                logger.finest(tag + " at " + unit.getLocation()
                    + " retargeting Europe: " + this);
            }
        } else {
            Location newTarget = findTarget(aiUnit, false);
            logger.finest(tag + " arrived at " + target.getColony().getName()
                + ", retargeting " + newTarget + ": " + this);
            target = newTarget;
        }
    }


    // Serialization

    /**
     * Writes all of the <code>AIObject</code>s and other AI-related
     * information to an XML-stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        if (isValid()) {
            toXML(out, getXMLElementTagName());
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeAttributes(out);

        if (target != null) {
            out.writeAttribute("target", target.getId());
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void readAttributes(XMLStreamReader in)
        throws XMLStreamException {
        super.readAttributes(in);
        
        String str = in.getAttributeValue(null, "target");
        target = getGame().getFreeColLocation(str);
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "cashInTreasureTrainMission".
     */
    public static String getXMLElementTagName() {
        return "cashInTreasureTrainMission";
    }
}
