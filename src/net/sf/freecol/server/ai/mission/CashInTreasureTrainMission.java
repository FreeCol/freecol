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

import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIUnit;

import org.w3c.dom.Element;


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

    /** A target to aim for, used for a TransportMission. */
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
        target = findTarget(aiUnit);
        logger.finest(tag + " starts at " + aiUnit.getUnit().getLocation()
            + " with target " + target + ": " + aiUnit.getUnit());
    }

    /**
     * Loads a mission from the given element.
     *
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an
     *      XML-representation of this object.
     */
    public CashInTreasureTrainMission(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
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
     * Gets the location we are aiming to cash in at.
     *
     * @return The location we are aiming to cash in at.
     */
    @Override
    public Location getTarget() {
        return target;
    }

    /**
     * Is a location a valid place to cash in this treasure train?
     *
     * @param aiUnit The treasure train <code>AIUnit</code>.
     * @param loc The <code>Location</code> to test.
     * @return True if the location is a valid cash in target.
     */
    public static boolean isTarget(AIUnit aiUnit, Location loc) {
        return (loc instanceof Europe
                && !((Europe)loc).isDisposed())
            || (loc instanceof Tile
                && aiUnit.getUnit().canCashInTreasureTrain((Tile)loc));
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
        final Unit unit = aiUnit.getUnit();
        final Tile tile = (path == null) ? unit.getTile()
            : path.getLastNode().getTile();
        return (isTarget(aiUnit, tile)) ? tile : null;
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
        int turns = (path == null) ? 1 : path.getTotalTurns() + 1;
        Location loc = extractTarget(aiUnit, path);
        return (isTarget(aiUnit, loc)) ? 1000 / turns
            : Integer.MIN_VALUE;
    }

    /**
     * Find a suitable cashin location for this unit.
     *
     * @param aiUnit The <code>AIUnit</code> to execute a cash in mission.
     * @return A <code>PathNode</code> to the target, or null if not found.
     */
    private static PathNode findTargetPath(AIUnit aiUnit) {
        Unit unit;
        if (aiUnit == null
            || (unit = aiUnit.getUnit()) == null || unit.isDisposed()) {
            return null;
        }
        // Not on the map?  Europe *must* be viable, so go there
        // (return null for now, path still impossible).
        final Player player = unit.getOwner();
        final Europe europe = player.getEurope();
        Tile startTile;
        if ((startTile = unit.getPathStartTile()) == null
            || player.getNumberOfSettlements() <= 0) return null;

        final Unit carrier = unit.getCarrier();
        final GoalDecider cashInDecider
            = getMissionGoalDecider(aiUnit, CashInTreasureTrainMission.class);
        PathNode path;

        // Find out how quickly the unit can get to Europe.
        final int europeTurns = (europe == null || carrier == null
            || (path = carrier.findPathToEurope(startTile)) == null) ? -1
            : path.getTotalTurns();

        // Find out how quickly the unit can get to a local cash-in site.
        path = unit.search(startTile, cashInDecider,
                           CostDeciders.avoidIllegal(), MAX_TURNS, carrier);
        final int localTurns = (path == null) ? -1 : path.getTotalTurns();

        // If there is a viable local target, go there first unless it is
        // quicker to just go straight to Europe.
        // Otherwise go to Europe if possible.
        if (localTurns >= 0
            && (europeTurns < 0 || localTurns < europeTurns)) return path;
        if (europeTurns >= 0) return null;

        // Finally search again for the nearest colony, this time with
        // relaxed cost decider that ignores blockages and no range
        // restriction.
        path = unit.search(startTile, cashInDecider,
                           CostDeciders.numberOfTiles(), INFINITY, carrier);
        if (path != null) return path;

        // Failed.  TODO: some sort of hack to build a colony nearby.
        logger.finest(tag + " out of targets: " + unit);
        return null;
    }

    /**
     * Finds a suitable cashin target for the supplied unit.
     * Falls back to Europe if the unit is not on the map.
     *
     * @param aiUnit The <code>AIUnit</code> to test.
     * @return A <code>PathNode</code> to the target, or null if none found.
     */
    public static Location findTarget(AIUnit aiUnit) {
        Location loc = extractTarget(aiUnit, findTargetPath(aiUnit));
        if (loc == null) loc = aiUnit.getUnit().getOwner().getEurope();
        return (isTarget(aiUnit, loc)) ? loc : null;
    }        


    // Fake Transportable interface

    /**
     * Gets the transport destination for units with this mission.
     *
     * @return The destination for this <code>Transportable</code>.
     */
    public Location getTransportDestination() {
        return (target instanceof Europe
            || (target instanceof Tile
                && shouldTakeTransportToTile((Tile)target))) ? target
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
     * Is it valid to for a unit to perform a CashInTreasureTrainMission.
     *
     * @param unit The <code>Unit</code> to check.
     * @return True if the task would be valid.
     */
    private static boolean isValid(Unit unit) {
        return unit.canCarryTreasure()
            && unit.getTreasureAmount() > 0;
    }

    /**
     * Is it valid to for an AI unit to perform a CashInTreasureTrainMission.
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @return True if the task would be valid.
     */
    public static boolean isValid(AIUnit aiUnit) {
        return Mission.isValid(aiUnit)
            && isValid(aiUnit.getUnit())
            && findTarget(aiUnit) != null;
    }

    /**
     * Is this mission still valid to perform.
     *
     * @return True if the task is still valid.
     */
    public boolean isValid() {
        return super.isValid()
            && isValid(getUnit())
            && isTarget(getAIUnit(), target);
    }

    /**
     * Performs this mission.
     *
     * @param connection The <code>Connection</code> to the server.
     */
    public void doMission(Connection connection) {
        final Unit unit = getUnit();
        if (unit == null || unit.isDisposed() || !isValid(unit)) {
            logger.warning(tag + " broken: " + unit);
            return;
        }

        // Validate target.
        final AIUnit aiUnit = getAIUnit();
        if (!isTarget(aiUnit, target)) {
            if ((target = findTarget(aiUnit)) == null) {
                logger.finest(tag + " could not find a target: " + unit);
                return;
            }
        }

        // Go there.
        if (travelToTarget(tag, target) != Unit.MoveType.MOVE) return;

        // Cash in now if:
        // - already in Europe
        // - or can never get there
        // - or there is no potential carrier to get the treasure to there
        // - or if the transport fee is not in effect.
        // Otherwise, it is better to send to Europe.
        final Player player = unit.getOwner();
        final Europe europe = player.getEurope();
        if (unit.canCashInTreasureTrain()) {
            if (unit.isInEurope()
                || europe == null
                || player.getCarriersForUnit(unit).isEmpty()
                || unit.getTransportFee() == 0) {
                if (AIMessage.askCashInTreasureTrain(aiUnit)) {
                    logger.finest(tag + " completed cash in at "
                        + unit.getLocation() + ": " + unit);
                }
            } else {
                target = europe;
                logger.finest(tag + " at " + unit.getLocation()
                    + " retargeting Europe: " + unit);
            }
        } else {
            logger.finest(tag + " waiting to cash in at "
                + unit.getLocation() + ": " + unit);
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
     * {@inherit-doc}
     */
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeAttributes(out);

        if (target != null) {
            out.writeAttribute("target", target.getId());
        }
    }

    /**
     * {@inherit-doc}
     */
    protected void readAttributes(XMLStreamReader in)
        throws XMLStreamException {
        super.readAttributes(in);
        
        FreeColGameObject fcgo = getGame()
            .getFreeColGameObjectSafely(in.getAttributeValue(null, "target"));
        target = (fcgo instanceof Europe || fcgo instanceof Tile)
            ? (Location)fcgo
            : null;
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
