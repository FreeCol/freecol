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
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIUnit;


/**
 * Mission for controlling a scout.
 *
 * @see net.sf.freecol.common.model.Unit.Role#SCOUT
 */
public class ScoutingMission extends Mission {

    private static final Logger logger = Logger.getLogger(ScoutingMission.class.getName());

    private static final String tag = "AI scout";

    /**
     * Maximum number of turns to travel to a scouting target.
     */
    private static final int MAX_TURNS = 20;

    /**
     * The target for this mission.  Either a tile with an LCR, a
     * native settlement to talk to the chief of, or a player colony
     * to retarget from.
     */
    private Location target = null;


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     */
    public ScoutingMission(AIMain aiMain, AIUnit aiUnit) {
        super(aiMain, aiUnit);

        target = findTarget(aiUnit);
        logger.finest(tag + " starts at " + aiUnit.getUnit().getLocation()
            + " with target " + target + ": " + this);
        uninitialized = false;
    }

    /**
     * Creates a new <code>ScoutingMission</code> and reads the given element.
     *
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public ScoutingMission(AIMain aiMain, XMLStreamReader in)
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
        final Location loc = (path == null) ? aiUnit.getUnit().getLocation()
            : path.getLastNode().getLocation();
        return (loc == null) ? null
            : (loc.getSettlement() instanceof IndianSettlement
                && invalidSettlementReason(aiUnit,
                    (IndianSettlement)loc.getSettlement()) == null)
            ? loc.getSettlement()
            : (invalidTileReason(aiUnit, loc.getTile()) == null) ? loc.getTile()
            : null;
    }

    /**
     * Evaluate a potential scouting mission for a given unit and
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
     * Finds a suitable scouting target for the supplied unit.
     *
     * @param aiUnit The <code>AIUnit</code> to test.
     * @return A <code>PathNode</code> to the target, or null if none found.
     */
    public static PathNode findTargetPath(final AIUnit aiUnit) {
        Unit unit;
        Tile startTile;
        if (aiUnit == null
            || (unit = aiUnit.getUnit()) == null || unit.isDisposed()
            || (startTile = unit.getPathStartTile()) == null) return null;

        final Unit carrier = unit.getCarrier();
        final GoalDecider scoutingDecider
            = getMissionGoalDecider(aiUnit, ScoutingMission.class);
        PathNode path;

        // Can the scout legally reach a valid target from where it
        // currently is?
        path = unit.search(startTile, scoutingDecider,
                           CostDeciders.avoidIllegal(), MAX_TURNS, carrier);
        if (path != null) return path;

        // If no target was found but there is a carrier, then give up
        // as we should have been able to get everywhere except
        // islands in lakes.
        if (carrier != null) return null;

        // Search again, purely on distance in tiles, which allows
        // water tiles and thus potentially finds targets that require
        // a carrier to reach.
        return unit.search(startTile, scoutingDecider,
                           CostDeciders.numberOfTiles(), MAX_TURNS, carrier);
    }

    /**
     * Finds a suitable scouting target for the supplied unit.
     * Falls back to the best settlement if the unit is not on the map.
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
        return (target == null
            || !shouldTakeTransportToTile(target.getTile())) ? null
            : target;
    }


    // Mission interface

    /**
     * Gets the target for this mission.
     *
     * @return The mission target.
     */
    public Location getTarget() {
        return target;
    }

    /**
     * Why would a ScoutingMission be invalid with the given unit.
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @return A reason why the mission would be invalid with the unit,
     *     or null if none found.
     */
    private static String invalidScoutingReason(AIUnit aiUnit) {
        final Unit unit = aiUnit.getUnit();
        return (unit.getRole() != Unit.Role.SCOUT) ? "unit-not-a-SCOUT"
            : null;
    }

    /**
     * Is this a valid scouting target because it is a suitable native
     * settlement.
     *
     * @param aiUnit The <code>AIUnit</code> to test.
     * @param is The <code>IndianSettlement</code> to test.
     * @return A reason why the mission would be invalid, or null if none found.
     */
    private static String invalidSettlementReason(AIUnit aiUnit,
                                                  IndianSettlement is) {
        final Unit unit = aiUnit.getUnit();
        final Player owner = unit.getOwner();
        Tension tension = is.getAlarm(owner);
        return (is.hasSpokenToChief(owner)) ? "settlement-contacted"
            : (tension != null && tension.getValue()
                >= Tension.Level.HATEFUL.getLimit()) ? "settlement-hateful"
            : null;
    }

    /**
     * Is this a valid scouting target because it is a suitable tile.
     *
     * @param aiUnit The <code>AIUnit</code> to test.
     * @param is The <code>IndianSettlement</code> to test.
     * @return A reason why the mission would be invalid, or null if none found.
     */
    private static String invalidTileReason(AIUnit aiUnit, Tile tile) {
        return (!tile.hasLostCityRumour()) ? "tile-empty"
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
     * @param aiUnit The <code>AIUnit</code> to check.
     * @return A reason for mission invalidity, or null if none found.
     */
    public static String invalidReason(AIUnit aiUnit) {
        String reason;
        return ((reason = Mission.invalidReason(aiUnit)) != null) ? reason
            : ((reason = invalidScoutingReason(aiUnit)) != null) ? reason
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
        return ((reason = invalidAIUnitReason(aiUnit)) != null) ? reason
            : ((reason = invalidScoutingReason(aiUnit)) != null) ? reason
            : (loc instanceof Tile)
            ? invalidTileReason(aiUnit, (Tile)loc)
            : (loc instanceof IndianSettlement)
            ? invalidSettlementReason(aiUnit, (IndianSettlement)loc)
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
            if ((target = findTarget(getAIUnit())) == null) {
                logger.finest(tag + " could not retarget: " + this);
                return;
            }
        } else if (reason != null) {
            logger.finest(tag + " broken(" + reason + "): " + this);
            return;
        }

        // Go to the target.
        final AIUnit aiUnit = getAIUnit();
        Unit.MoveType mt = travelToTarget(tag, target);
        switch (mt) {
        case ATTACK_UNIT: case MOVE_NO_MOVES: case MOVE_NO_REPAIR:
        case MOVE_ILLEGAL:
            return;
        case MOVE:
            break;
        case ENTER_INDIAN_SETTLEMENT_WITH_SCOUT:
            Direction d = unit.getTile().getDirection(target.getTile());
            if (d == null) {
                throw new IllegalStateException("Unit not next to target "
                    + target + ": " + unit + "/" + unit.getLocation());
            }
            AIMessage.askScoutIndianSettlement(aiUnit, d);
            if (unit.isDisposed()) {
                logger.finest(tag + " died at target " + target
                    + ": " + this);
                return;
            }
            break;
        default:
            logger.warning(tag + " unexpected move type " + mt + ": " + this);
            return;
        }

        // Retarget when complete, but do not retarget from one colony
        // to another (just drop equipment and invalidate the mission).
        final Player player = unit.getOwner();
        Location completed = target;
        target = findTarget(aiUnit);
        if (invalidTargetReason(completed, player) == null
            && invalidTargetReason(target, player) == null) {
            Colony colony = (Colony)completed;
            for (EquipmentType e : new ArrayList<EquipmentType>(unit
                    .getEquipment().keySet())) {
                int n = unit.getEquipmentCount(e);
                unit.changeEquipment(e, -n);
                colony.addEquipmentGoods(e, n); // TODO: check for overflow
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
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        toXML(out, getXMLElementTagName());
    }

    /**
     * {@inheritDoc}
     */
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeAttributes(out);
        if (target != null) {
            writeAttribute(out, "target", (FreeColGameObject)target);
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
     * @return "scoutingMission".
     */
    public static String getXMLElementTagName() {
        return "scoutingMission";
    }
}
