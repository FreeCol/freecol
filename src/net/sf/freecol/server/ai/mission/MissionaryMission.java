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

import java.util.ArrayList;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
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
 * Mission for sending a missionary to a native settlement.
 */
public class MissionaryMission extends Mission {

    private static final Logger logger = Logger.getLogger(MissionaryMission.class.getName());

    private static final String tag = "AI missionary";

    /**
     * Maximum number of turns to travel to a missionary target.
     */
    private static final int MAX_TURNS = 20;

    /**
     * The target to aim for, used for a TransportMission.
     * Either an IndianSettlement, or a backup Colony to head for before
     * retargeting.
     */
    private Location target = null;


    /**
     * Creates a missionary mission for the given <code>AIUnit</code>.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission
     *        is created for.
     */
    public MissionaryMission(AIMain aiMain, AIUnit aiUnit) {
        super(aiMain, aiUnit);
        target = findTarget(aiUnit, true);
        logger.finest(tag + " starts at " + aiUnit.getUnit().getLocation()
            + " with target " + target + ": " + this);
        uninitialized = false;
    }

    /**
     * Creates a new <code>MissionaryMission</code> and reads
     * the given element.
     *
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public MissionaryMission(AIMain aiMain, XMLStreamReader in)
        throws XMLStreamException {
        super(aiMain);

        readFromXML(in);
        uninitialized = getAIUnit() == null;
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
        final Unit unit = aiUnit.getUnit();
        final Location loc = path.getLastNode().getLocation();
        Settlement settlement = (loc == null) ? null : loc.getSettlement();
        return (settlement instanceof IndianSettlement
            && invalidIndianSettlementReason(aiUnit,
                (IndianSettlement)settlement) == null)
            ? (IndianSettlement)settlement
            : (settlement instanceof Colony
                && invalidColonyReason(aiUnit, (Colony)settlement) != null)
            ? (Colony)settlement
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
        Location loc = extractTarget(aiUnit, path);
        return (loc instanceof IndianSettlement)
            ? 1000 / (path.getTotalTurns() + 1)
            : Integer.MIN_VALUE;
    }

    /**
     * Makes a goal decider that checks for potential missions.
     *
     * @param aiUnit The <code>AIUnit</code> to find a mission with.
     * @param deferOK Keep track of the nearest of our colonies, to use
     *     as a fallback destination.
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
                int value = scorePath(aiUnit, path);
                if (bestValue < value) {
                    bestValue = value;
                    bestPath = path;
                    return true;
                }
                return false;
            }
        };
        return (deferOK) ? GoalDeciders.getComposedGoalDecider(gd,
            GoalDeciders.getOurClosestSettlementGoalDecider())
            : gd;
    }
            
    /**
     * Find a suitable mission location for this unit.
     *
     * @param aiUnit The <code>AIUnit</code> to execute a cash in mission.
     * @param deferOK If true, allow the search to return a nearby existing
     *     colony as a temporary target.     
     * @return A <code>PathNode</code> to the target, or null if not found.
     */
    private static PathNode findTargetPath(AIUnit aiUnit, boolean deferOK) {
        if (invalidAIUnitReason(aiUnit) != null) return null;
        final Unit unit = aiUnit.getUnit();
        final Tile startTile = unit.getPathStartTile();
        if (startTile == null) return null;

        PathNode path;
        final Unit carrier = unit.getCarrier();
        final GoalDecider gd = getGoalDecider(aiUnit, deferOK);
        final CostDecider standardCd
            = CostDeciders.avoidSettlementsAndBlockingUnits();
        final CostDecider relaxedCd = CostDeciders.numberOfTiles();

        // Is there a valid target available from the starting tile?
        path = unit.search(startTile, gd, standardCd, MAX_TURNS, carrier);
        if (path != null) return path;

        // One more try with a relaxed cost decider and no range limit.
        return unit.search(startTile, gd, relaxedCd, MAX_TURNS, carrier);
    }

    /**
     * Finds a suitable mission target for the supplied unit.
     * Falls back to the best colony if a path is not found.
     *
     * @param aiUnit The <code>AIUnit</code> to test.
     * @param deferOK Enables deferring to a fallback colony.
     * @return A new target for this mission.
     */
    public static Location findTarget(AIUnit aiUnit, boolean deferOK) {
        PathNode path = findTargetPath(aiUnit, deferOK);
        return (path != null) ? extractTarget(aiUnit, path)
            : (deferOK) ? getBestSettlement(aiUnit.getUnit().getOwner())
            : null;
    }

    /**
     * Prepare a unit for a Missionary mission.
     *
     * @param aiUnit The <code>AIUnit</code> to prepare.
     * @return A reason why the unit can not perform this mission, or null
     *     if none.
     */
    public static String prepare(AIUnit aiUnit) {
        String reason = invalidReason(aiUnit);
        if (reason == null) {
            final Unit unit = aiUnit.getUnit();
            if (!unit.hasAbility("model.ability.establishMission")
                && (((FreeColGameObject)unit.getLocation())
                    .hasAbility("model.ability.dressMissionary"))) {
                aiUnit.equipForRole(Unit.Role.MISSIONARY, false);
            }
            reason = (unit.hasAbility("model.ability.establishMission"))
                ? null
                : "unit-can-not-establish-mission";
        }
        return reason;
    }

    // Fake Transportable interface

    /**
     * Gets the transport destination for units with this mission.
     *
     * @return The destination for this <code>Transportable</code>.
     */
    @Override
    public Location getTransportDestination() {
        return (target == null
            || !shouldTakeTransportToTile(target.getTile())) ? null
            : target;
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
     * @param aiUnit The <code>AIUnit</code> to check.
     * @return A reason to not perform the mission, or null if none.
     */
    private static String invalidMissionReason(AIUnit aiUnit) {
        String reason = invalidAIUnitReason(aiUnit);
        if (reason != null) return reason;
        final Unit unit = aiUnit.getUnit();
        return (!unit.isPerson()) ? Mission.UNITNOTAPERSON
            : (unit.getSkillLevel() >= -1
                && !unit.hasAbility(Ability.EXPERT_MISSIONARY))
            ? "unit-is-not-subskilled-or-expertMissionary"
            : (unit.isInEurope() || unit.isAtSea()) 
            ? ((unit.getOwner().getNumberOfSettlements() <= 0)
                ? "unit-off-map-but-missing-initial-settlement"
                : null)
            : (unit.isInMission()) ? "unit-is-already-at-mission"
            : null;
    }

    /**
     * Why would a MissionaryMission be invalid with the given Colony?
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @param colony The <code>Colony</code> to check.
     * @return A reason to not perform the mission, or null if none.
     */
    private static String invalidColonyReason(AIUnit aiUnit, Colony colony) {
        return invalidTargetReason(colony, aiUnit.getUnit().getOwner());
    }

    /**
     * Why would a MissionaryMission be invalid with the given
     * IndianSettlement?
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @param indianSettlement The <code>IndianSettlement</code> to check.
     * @return A reason to not perform the mission, or null if none.
     */
    private static String invalidIndianSettlementReason(AIUnit aiUnit,
                                                        IndianSettlement is) {
        String reason = invalidTargetReason(is);
        if (reason != null) return reason;
        final Player owner = aiUnit.getUnit().getOwner();
        return (!owner.hasContacted(is.getOwner()))
            ? "target-is-uncontacted"
            : (is.getOwner().atWarWith(owner))
            ? "target-at-war"
            : (is.getMissionary() != null
                && is.getMissionary().getOwner() == owner)
            ? "target-has-our-mission"
            : null;
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
            : (loc instanceof IndianSettlement)
            ? invalidIndianSettlementReason(aiUnit, (IndianSettlement)loc)
            : (loc instanceof Colony)
            ? invalidColonyReason(aiUnit, (Colony)loc)
            : Mission.TARGETINVALID;
    }

    // Not a one-time mission, omit isOneTime().
    
    /**
     * Performs this mission.
     */
    public void doMission() {
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
        final AIUnit aiUnit = getAIUnit();
        final Unit unit = getUnit();

        // Go to the target.
        Unit.MoveType mt = travelToTarget(tag, target,
            CostDeciders.avoidSettlementsAndBlockingUnits());
        switch (mt) {
        case MOVE_NO_MOVES: case MOVE_NO_REPAIR: case MOVE_ILLEGAL:
            break;
        case MOVE:
            // Reached an intermediate colony.  Retarget, but do not
            // accept fallback targets.
            Location completed = target;
            target = findTarget(aiUnit, false);
            logger.finest(tag + " reached colony target " + completed
                + ", retargeting " + target + ": " + this);
            break;
        case ENTER_INDIAN_SETTLEMENT_WITH_MISSIONARY:
            Direction d = unit.getTile().getDirection(target.getTile());
            if (d == null) {
                throw new IllegalStateException("Unit not next to target "
                    + target + ": " + unit + "/" + unit.getLocation());
            }
            IndianSettlement is = (IndianSettlement)target;
            AIMessage.askEstablishMission(aiUnit, d,
                                          is.getMissionary() != null);
            if (unit.isDisposed()) {
                logger.finest(tag + " died at target " + target + ": " + this);
            } else if (is.getMissionary() == unit && unit.isInMission()) {
                logger.finest(tag + " completed at " + target + ": " + this);
                target = null;
            } else {
                logger.warning(tag + " unexpected failure at " + target
                    + ": " + this);
            }
            break;
        default:
            logger.warning(tag + " unexpected move type (" + mt
                + ") at " + unit.getLocation() + ": " + this);
            break;
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
        target = (str == null) ? null : getGame().getFreeColLocation(str);
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "missionaryMission".
     */
    public static String getXMLElementTagName() {
        return "missionaryMission";
    }
}
