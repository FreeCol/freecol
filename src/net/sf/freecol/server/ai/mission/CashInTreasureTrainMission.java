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
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
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

    /** Maximum number of turns to travel to a cash in location. */
    private static final int MAX_TURNS = 20;

    /** A target to aim for, used for a TransportMission. */
    private Location targetLoc = null;


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission
     *        is created for.
     */
    public CashInTreasureTrainMission(AIMain aiMain, AIUnit aiUnit) {
        super(aiMain, aiUnit);
        targetLoc = findTarget(aiUnit);
        logger.finest("AI treasure train starts with target " + targetLoc
            + ": " + aiUnit.getUnit());
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
     * Is a location a valid place to cash in this treasure train?
     *
     * @param loc The <code>Location</code> to test.
     * @param unit The treasure train <code>Unit</code>.
     * @return True if the location is a valid cash in target.
     */
    private static boolean checkTarget(Location loc, Unit unit) {
        return (loc instanceof Europe && !((Europe)loc).isDisposed())
            || (loc instanceof Tile && unit.canCashInTreasureTrain((Tile)loc));
    }

    /**
     * Find a suitable destination for this unit.
     *
     * @param aiUnit The <code>AIUnit</code> to execute a cash in mission.
     * @return A suitable target location or null if none found.
     */
    private static Location findTarget(AIUnit aiUnit) {
        final Unit unit = aiUnit.getUnit();
        if (unit == null || unit.isDisposed()) {
            logger.warning("AI treasure train broken: " + unit);
            return null;
        }

        final Player player = unit.getOwner();
        final Europe europe = player.getEurope();
        final Tile startTile = getPathStartTile(unit);
        // Not on the map?  Europe *must* be viable, so go there.
        // Nowhere to go to?  Go to Europe (even if it is null).
        if (startTile == null 
            || player.getNumberOfSettlements() <= 0) return europe;

        // Find out how quickly the unit can get to Europe.
        PathNode path;
        final Unit carrier = (unit.isOnCarrier()) ? ((Unit)unit.getLocation())
            : null;
        final GoalDecider cashInDecider = new GoalDecider() {
                private PathNode best = null;
                private int bestValue = INFINITY;
            
                public PathNode getGoal() { return best; }
                public boolean hasSubGoals() { return true; }
                public boolean check(Unit u, PathNode path) {
                    if (checkTarget(path.getTile(), unit)
                        && path.getTotalTurns() < bestValue) {
                        bestValue = path.getTotalTurns();
                        best = path;
                        return true;
                    }
                    return false;
                }
            };
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
        if (localTurns >= 0 && (europeTurns < 0 || localTurns < europeTurns)) {
            return path.getLastNode().getTile();
        }
        if (europeTurns >= 0) return europe;

        // Finally search again for the nearest colony, this time with
        // relaxed cost decider that ignores blockages and no range
        // restriction.
        path = unit.search(startTile, cashInDecider,
                           CostDeciders.numberOfTiles(), INFINITY, carrier);
        if (path != null) return path.getLastNode().getTile();

        // Failed.  TODO: some sort of hack to build a colony nearby.
        logger.finest("AI treasure train out of targets: " + unit);
        return null;
    }

    // Fake Transportable interface

    /**
     * Gets the transport destination for units with this mission.
     *
     * @return The destination for this <code>Transportable</code>.
     */
    public Location getTransportDestination() {
        final Unit unit = getUnit();
        return (checkTarget(targetLoc, unit)
            && (targetLoc instanceof Europe
                || (targetLoc instanceof Tile
                    && shouldTakeTransportToTile((Tile)targetLoc))))
            ? targetLoc : null;
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
     * @param aiUnit The <code>AIUnit</code> to check.
     * @return True if the task would be valid.
     */
    public static boolean isValid(AIUnit aiUnit) {
        return Mission.isValid(aiUnit)
            && aiUnit.getUnit().canCarryTreasure()
            && aiUnit.getUnit().getTreasureAmount() > 0
            && findTarget(aiUnit) != null;
    }

    /**
     * Is this mission still valid to perform.
     *
     * @return True if the task is still valid.
     */
    public boolean isValid() {
        return super.isValid()
            && getUnit().canCarryTreasure()
            && getUnit().getTreasureAmount() > 0
            && checkTarget(targetLoc, getUnit());
    }

    /**
     * Performs this mission.
     *
     * @param connection The <code>Connection</code> to the server.
     */
    public void doMission(Connection connection) {
        final Unit unit = getUnit();
        if (unit == null || unit.isDisposed()) {
            logger.warning("AI treasure train broken: " + unit);
            return;
        }

        // Validate target.
        AIUnit aiUnit = getAIUnit();
        if (!checkTarget(targetLoc, unit)) {
            if ((targetLoc = findTarget(aiUnit)) == null) return;
        }

        // Go to the target.
        if (travelToTarget("AI treasure train", targetLoc)
            != Unit.MoveType.MOVE) return;

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
                    logger.finest("AI treasure train completed cash in at "
                        + unit.getLocation() + ": " + unit);
                }
            } else {
                targetLoc = europe;
                logger.finest("AI treasure train at " + unit.getLocation()
                    + " retargeting Europe: " + unit);
            }
        } else {
            logger.finest("AI treasure train waiting to cash in at "
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
        out.writeAttribute("target", targetLoc.getId());
    }

    /**
     * {@inherit-doc}
     */
    protected void readAttributes(XMLStreamReader in)
        throws XMLStreamException {
        super.readAttributes(in);
        targetLoc = (Location) getGame()
            .getFreeColGameObject(in.getAttributeValue(null, "target"));
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
