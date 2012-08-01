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

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIUnit;


/**
 * Mission for sending a missionary to a settlement.
 */
public class MissionaryMission extends Mission {

    private static final Logger logger = Logger.getLogger(MissionaryMission.class.getName());

    private static final String tag = "AI missionary";

    /**
     * Maximum number of turns to travel to a missionary target.
     */
    private static final int MAX_TURNS = 16;

    /** A target to aim for, used for a TransportMission. */
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
        target = findTarget(aiUnit);
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
        Settlement settlement;
        return (loc == null) ? null
            : ((settlement = loc.getSettlement()) != null)
            ? ((invalidSettlementReason(aiUnit, settlement) == null)
                ? settlement : null)
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
        Location target;
        return (path == null
            || (target = extractTarget(aiUnit, path)) == null
            || target instanceof Colony) ? Integer.MIN_VALUE
            : 1000 / (path.getTotalTurns() + 1);
    }

    /**
     * Find a suitable mission location for this unit.
     *
     * @param aiUnit The <code>AIUnit</code> to execute a cash in mission.
     * @return A <code>PathNode</code> to the target, or null if not found.
     */
    private static PathNode findTargetPath(AIUnit aiUnit) {
        Unit unit;
        Tile startTile;
        if (Mission.invalidAIUnitReason(aiUnit) != null
            || (startTile = (unit = aiUnit.getUnit()).getTile()) == null)
            return null;

        final Unit carrier = unit.getCarrier();
        final GoalDecider decider
            = getMissionGoalDecider(aiUnit, MissionaryMission.class);
        PathNode path;

        // Is there a valid target available from the starting tile?
        path = unit.search(startTile, decider,
                           CostDeciders.avoidIllegal(), MAX_TURNS, carrier);
        if (path != null) return path;

        // If no target was found but there is a carrier, then give up
        // as we should have been able to get everywhere except
        // islands in lakes.
        if (carrier != null) return null;

        // Search again, purely on distance in tiles, which allows
        // water tiles and thus potentially finds targets that require
        // a carrier to reach.
        return unit.search(startTile, decider,
                           CostDeciders.numberOfTiles(), MAX_TURNS, carrier);
    }

    /**
     * Finds a suitable mission target for the supplied unit.
     * Falls back to the best settlement if a path is not found.
     *
     * TODO: improve the fallback to a real target when we understand
     * searching from off-map.
     *
     * @param aiUnit The <code>AIUnit</code> to test.
     * @return A <code>PathNode</code> to the target, or null if none found.
     */
    public static Location findTarget(AIUnit aiUnit) {
        PathNode path = findTargetPath(aiUnit);
        return (path == null) ? getBestSettlement(aiUnit.getUnit().getOwner())
            : extractTarget(aiUnit, path);
    }


    // Fake Transportable interface

    /**
     * Gets the transport destination for units with this mission.
     *
     * @return The destination for this <code>Transportable</code>.
     */
    @Override
    public Location getTransportDestination() {
        return (target != null && shouldTakeTransportToTile(target.getTile()))
            ? target
            : null;
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
     * Why would a MissionaryMission be invalid with the given unit?
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @return A reason to not perform the mission, or null if none.
     */
    private static String invalidMissionReason(AIUnit aiUnit) {
        final Unit unit = aiUnit.getUnit();
        return (!unit.isPerson()) ? Mission.UNITNOTAPERSON
            : (unit.getSkillLevel() >= 0
                && !unit.hasAbility("model.ability.expertMissionary"))
            ? "unit-is-not-subskilled-or-expertMissionary"
            : (unit.isInEurope() || unit.isAtSea()) ? null
            : (unit.getTile() == null) ? "unit-is-already-at-mission"
            : null;
    }

    /**
     * Why would a MissionaryMission be invalid with the given location?
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @param settlement The <code>Settlement</code> to check.
     * @return A reason to not perform the mission, or null if none.
     */
    private static String invalidSettlementReason(AIUnit aiUnit,
                                                  Settlement settlement) {
        final Unit unit = aiUnit.getUnit();
        final Player owner = unit.getOwner();
        return (settlement == null || settlement.isDisposed())
            ? Mission.TARGETINVALID
            : (settlement instanceof Colony)
            ? ((settlement.getOwner() == owner) ? null
                : Mission.TARGETOWNERSHIP)
            : (settlement instanceof IndianSettlement)
            ? ((settlement.getOwner().atWarWith(owner)) ? "target-at-war"
                : (((IndianSettlement)settlement).getMissionary() != null
                    && ((IndianSettlement)settlement).getMissionary().getOwner()
                    == owner)
                ? "target-has-our-mission"
                : null)
            : Mission.TARGETINVALID;
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
        String reason;
        return ((reason = Mission.invalidReason(aiUnit)) != null) ? reason
            : ((reason = invalidMissionReason(aiUnit)) != null) ? reason
            : null;
    }

    /**
     * Why would this mission be invalid with the given AI unit and location?
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @param loc The <code>Location</code> to check.
     * @return A reason for invalidity, or null if none found.
     */
    public static String invalidReason(AIUnit aiUnit, Location loc) {
        String reason;
        return ((reason = invalidAIUnitReason(aiUnit)) != null
            || (reason = invalidMissionReason(aiUnit)) != null)
            ? reason
            : (loc instanceof Settlement)
            ? invalidSettlementReason(aiUnit, (Settlement)loc)
            : Mission.TARGETINVALID;
    }

    // Not a one-time mission, omit isOneTime().
    
    /**
     * Performs this mission.
     */
    public void doMission() {
        final AIUnit aiUnit = getAIUnit();
        final Unit unit = getUnit();
        String reason = invalidReason();
        if (isTargetReason(reason)) {
            if ((target = findTarget(aiUnit)) == null) {
                logger.finest(tag + " could not retarget: " + this);
                return;
            }
        } else if (reason != null) {
            logger.finest(tag + " broken(" + reason + "): " + this);
            return;
        }

        // Go to the target.
        Unit.MoveType mt = travelToTarget(tag, target);
        switch (mt) {
        case MOVE_NO_MOVES:
            return;
        case MOVE:
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
                return;
            }
            if (is.getMissionary() == unit) {
                logger.finest(tag + " completed at " + target + ": " + this);
                target = null;
                return;
            }
            logger.finest(tag + " unexpected failure at " + target
                + ": " + this);
            break;
        default:
            logger.warning(tag + " unexpected move type (" + mt
                + ") at " + unit.getLocation() + ": " + this);
            return;
        }

        // Retarget on failure or complete at colony, but do not
        // retarget from one colony to another, just drop equipment
        // and invalidate the mission.
        Location completed = target;
        target = findTarget(aiUnit);
        if (completed instanceof Colony && target instanceof Colony) {
           Colony colony = (Colony)completed;
            for (EquipmentType e : new ArrayList<EquipmentType>(unit
                    .getEquipment().keySet())) {
                AIMessage.askEquipUnit(aiUnit, e, -unit.getEquipmentCount(e));
            }
            target = null;
        }
        logger.finest(tag + " completed target " + completed
            + ", retargeting " + target + ": " + this);
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
