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

import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tension;
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
 * Mission for controlling a scout.
 *
 * @see net.sf.freecol.common.model.Unit.Role#SCOUT
 */
public class ScoutingMission extends Mission {

    private static final Logger logger = Logger.getLogger(ScoutingMission.class.getName());

    /**
     * The tile this scout is travelling to, either to investigate a LCR,
     * or talk to a chief.
     */
    private Tile target = null;


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     */
    public ScoutingMission(AIMain aiMain, AIUnit aiUnit) {
        super(aiMain, aiUnit);
        target = findTarget(aiUnit);
        logger.finest("AI scout starting at " + aiUnit.getUnit().getLocation()
            + " with target " + target + ": " + aiUnit.getUnit());
    }

    /**
     * Loads a mission from the given element.
     *
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an XML-representation
     *            of this object.
     */
    public ScoutingMission(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
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
    }

    /**
     * Is a tile a valid scouting target because of its native settlement.
     *
     * @param tile The <code>Tile</code> to test.
     * @param unit The <code>Unit</code> to test.
     * @return True if the tile is a valid scouting target.
     */
    private static boolean checkIndianSettlement(Tile tile, AIUnit aiUnit) {
        IndianSettlement settlement = tile.getIndianSettlement();
        if (settlement == null) return false;
        final Player owner = aiUnit.getUnit().getOwner();
        Tension tension = settlement.getAlarm(owner);
        return !settlement.hasSpokenToChief(owner)
            && (tension == null
                || tension.getValue() < Tension.Level.HATEFUL.getLimit());
    }

    /**
     * Is a tile a valid scouting target for a unit?
     *
     * @param tile The <code>Tile</code> to test.
     * @param unit The <code>Unit</code> to test.
     * @return True if the tile is a valid scouting target.
     */
    private static boolean checkTarget(Tile tile, AIUnit aiUnit) {
        return tile != null && tile.isLand()
            && (tile.hasLostCityRumour()
                || checkIndianSettlement(tile, aiUnit));
    }

    /**
     * Checks that a target tile is valid, including allowing for a
     * fallback colony target.
     *
     * @param tile The <code>Tile</code> to test.
     * @param unit The <code>Unit</code> to test.
     * @return True if the tile is a valid scouting target.
     */
    private boolean checkTargetAllowColony(Tile tile, AIUnit aiUnit) {
        return checkTarget(tile, aiUnit)
            || (tile != null && tile.getColony() != null
                && tile.getColony().getOwner() == aiUnit.getUnit().getOwner());
    }

    /**
     * Finds a suitable scouting target for the supplied unit.
     *
     * @param unit The <code>Unit</code> to test.
     * @return A <code>Tile</code> that is worth scouting.
     */
    public static Tile findTarget(final AIUnit aiUnit) {
        final Unit unit = aiUnit.getUnit();
        if (unit == null || unit.isDisposed()) {
            logger.warning("AI scout broken: " + unit);
            return null;
        }

        final Tile startTile = unit.getPathStartTile();
        if (startTile == null) {
            Settlement settlement = Mission.getBestSettlement(unit.getOwner());
            if (settlement != null) return settlement.getTile();
            logger.finest("AI scout settlement fallback failed: " + unit);
            return null;            
        }

        PathNode path;
        final Unit carrier = (unit.isOnCarrier()) ? ((Unit)unit.getLocation())
            : null;
        final GoalDecider scoutingDecider = new GoalDecider() {
                private PathNode best = null;
                private int bestValue = INFINITY;
                
                public PathNode getGoal() { return best; }
                public boolean hasSubGoals() { return true; }
                public boolean check(Unit u, PathNode path) {
                    if (checkTarget(path.getTile(), aiUnit)
                        && path.getTotalTurns() < bestValue) {
                        bestValue = path.getTotalTurns();
                        best = path;
                        return true;
                    }
                    return false;
                }
            };

        // Can the scout legally reach a valid target from where it
        // currently is?
        path = unit.search(startTile, scoutingDecider,
                           CostDeciders.avoidIllegal(), INFINITY, carrier);
        if (path != null) return path.getLastNode().getTile();

        // If no target was found but there is a carrier, then give up
        // as we should have been able to get everywhere except
        // islands in lakes.
        if (carrier != null) {
            logger.finest("AI scout (with carrier) out of targets: " + unit);
            return null;
        }

        // Search again, purely on distance in tiles, which allows
        // water tiles and thus potentially finds targets that require
        // a carrier to reach.
        path = unit.search(startTile, scoutingDecider,
                           CostDeciders.numberOfTiles(), INFINITY, carrier);
        if (path != null) return path.getLastNode().getTile();

        // Completely out of targets.
        logger.finest("AI scout out of targets: " + unit);
        return null;
    }

    // Fake Transportable interface

    /**
     * Gets the transport destination for units with this mission.
     *
     * @return The destination for this <code>Transportable</code>.
     */
    public Location getTransportDestination() {
        return (shouldTakeTransportToTile(target)) ? target : null;
    }

    // Mission interface

    /**
     * Checks if it is possible to assign a valid scouting mission to
     * a unit.
     *
     * @param aiUnit The <code>AIUnit</code> to be checked.
     * @return True if the unit could be a scout.
     */
    public static boolean isValid(AIUnit aiUnit) {
        return Mission.isValid(aiUnit)
            && aiUnit.getUnit().getRole() == Unit.Role.SCOUT
            && findTarget(aiUnit) != null;
    }

    /**
     * Checks if this mission is still valid.
     *
     * @return True if this mission is still valid.
     */
    public boolean isValid() {
        return super.isValid()
            && getUnit().getRole() == Unit.Role.SCOUT
            && checkTargetAllowColony(target, getAIUnit());
    }

    /**
     * Performs this mission.
     *
     * @param connection The <code>Connection</code> to the server.
     */
    public void doMission(Connection connection) {
        final Unit unit = getUnit();
        if (unit == null || unit.isDisposed()) return;

        // Check the target.
        final AIUnit aiUnit = getAIUnit();
        if (!checkTargetAllowColony(target, aiUnit)) {
            target = findTarget(aiUnit);
            if (target == null) {
                logger.finest("AI scout could not find a target: " + unit);
                return;
            }
        }

        Unit.MoveType mt = travelToTarget("AI scout", target);
        switch (mt) {
        case ATTACK_UNIT: case MOVE_NO_MOVES: case MOVE_NO_REPAIR:
        case MOVE_ILLEGAL:
            return;
        case MOVE:
            break;
        case ENTER_INDIAN_SETTLEMENT_WITH_SCOUT:
            Direction d = unit.getTile().getDirection(target);
            if (d == null) {
                throw new IllegalStateException("Unit not next to target "
                    + target + ": " + unit + "/" + unit.getLocation());
            }
            AIMessage.askScoutIndianSettlement(aiUnit, d);
            if (unit.isDisposed()) {
                logger.finest("AI scout died at target " + target
                    + ": " + unit);
                return;
            }
            break;
        default:
            logger.warning("AI scout unexpected move type " + mt
                + ": " + unit);
            return;
        }

        // Retarget when complete.
        Tile completed = target;
        target = findTarget(aiUnit);
        logger.finest("AI scout completed target " + completed
            + ", retargeting " + target + ": " + unit);
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
        target = (Tile) getGame()
            .getFreeColGameObjectSafely(in.getAttributeValue(null, "target"));
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
