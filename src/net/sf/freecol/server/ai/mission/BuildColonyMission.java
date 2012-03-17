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
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.NetworkConstants;
import net.sf.freecol.common.util.Utils;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIUnit;

import org.w3c.dom.Element;


/**
 * Mission for building a <code>Colony</code>.
 *
 * This mission can be used in two different ways:
 * <ul>
 * <li>Build a colony at a specific location.</li>
 * <li>Find a site for a colony and build it there.</li>
 * </ul>
 *
 * This mission will be aborted in the former case if the value gets
 * below a given threshold, while a colony will always get built (if
 * there is sufficient space on the map) in the latter case. Use the
 * appropriate constructor to get the desired behaviour.
 *
 * @see net.sf.freecol.common.model.Colony Colony
 */
public class BuildColonyMission extends Mission {

    private static final Logger logger = Logger.getLogger(BuildColonyMission.class.getName());

    /** The maximum number of turns to travel to a building site. */
    private static final int MAX_TURNS = 5;

    /**
     * The <code>Tile</code> where the <code>Colony</code> should be built.
     * Alternately, this can be the site of an existing colony--- the unit
     * is to go there and then retarget a new site.
     *
     * This target must be non-null for this mission to remain valid.
     * doMission() maintains this as much as possible.
     */
    private Tile target = null;

    /** The value of the target <code>Tile</code>. */
    private int colonyValue = -1;


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     * @param target The target <code>Tile</code> to build a colony at.
     */
    public BuildColonyMission(AIMain aiMain, AIUnit aiUnit, Tile target) {
        super(aiMain, aiUnit);
        this.target = target;
        colonyValue = (target == null || target.getColony() != null) ? -1
            : aiUnit.getUnit().getOwner().getColonyValue(target);
        logger.finest("AI colony builder starts with target " + target
            + " and value " + colonyValue
            + ": " + aiUnit.getUnit());
    }

    /**
     * Creates a new <code>BuildColonyMission</code>.
     *
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an XML-representation
     *            of this object.
     */
    public BuildColonyMission(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
    }

    /**
     * Creates a new <code>BuildColonyMission</code> and reads the given
     * element.
     *
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public BuildColonyMission(AIMain aiMain, XMLStreamReader in)
        throws XMLStreamException {
        super(aiMain);
        readFromXML(in);
    }

    /**
     * Gets the target of this mission.
     *
     * @return The tile where a colony is to be built.
     */
    public Tile getTarget() {
        return target;
    }


    /**
     * Gets the value of a path to a colony building site.  The value is
     * proportional to the general desirability of the site, and inversely
     * proportional to the number of turns to get there.
     *
     * @param path The <code>PathNode</code> to check.
     * @param player The <code>Player</code> that will found the colony,
     *     needed to check that the site can be acquired.
     * @return A score for the building site.
     */
    private static float getSiteValue(PathNode path, Player player) {
        final Tile tile = path.getTile();
        if (tile == null || !tile.isLand()) return -1.0f;
        float turns = path.getTotalTurns() + 1.0f;

        // Can the player acquire the tile?
        switch (player.canClaimToFoundSettlementReason(tile)) {
        case NONE:
            break;
        case NATIVES:
            // Penalize value when the tile will need to be stolen
            int price = player.getLandPrice(tile);
            if (price > 0 && !player.checkGold(price)) turns *= 2.0f;
            break;
        default:
            return -1.0f;
        }

        return player.getColonyValue(tile) / turns;
    }

    /**
     * Makes a goal decider that checks colony sites.
     *
     * @param deferOK Keep track of the nearest of our colonies, to use
     *     as a fallback destination.
     * @return A suitable <code>GoalDecider</code>.
     */
    private static GoalDecider getColonyDecider(final boolean deferOK) {
        return new GoalDecider() {
            private PathNode best = null;
            private float bestValue = 0.0f;
            private PathNode backup = null;
            private int backupTurns = INFINITY;

            public PathNode getGoal() { return (best != null) ? best : backup; }
            public boolean hasSubGoals() { return true; }
            public boolean check(Unit u, PathNode path) {
                Colony colony = path.getTile().getColony();
                if (colony != null) {
                    if (deferOK
                        && colony.getOwner() == u.getOwner()
                        && colony.isConnected()
                        && path.getTotalTurns() < backupTurns) {
                        backupTurns = path.getTotalTurns();
                        backup = path;
                    }
                    return false;
                }
                float value = getSiteValue(path, u.getOwner());
                if (value > bestValue) {
                    bestValue = value;
                    best = path;
                    return true;
                }
                return false;
            }
        };
    }
            
    /**
     * Finds a site for a new colony.  Favour closer sites.
     *
     * @param unit The <code>Unit</code> to find a colony site with.
     * @param deferOK If true, allow the search to return a nearby existing
     *     colony as a temporary target.     
     * @return A suitable tile for a new colony, or backup target.
     */
    public static Tile findTargetTile(AIUnit aiUnit, boolean deferOK) {
        final Unit unit = aiUnit.getUnit();
        if (unit == null || unit.isDisposed()) {
            logger.warning("AI colony builder broken: " + unit);
            return null;
        }

        final Tile startTile = unit.getPathStartTile();
        if (startTile == null) {
            Settlement settlement = Mission.getBestSettlement(unit.getOwner());
            if (settlement != null) return settlement.getTile();
            logger.finest("AI colony builder settlement fallback failed: "
                + unit);
            return null;
        }

        PathNode path;
        final Unit carrier = (unit.isOnCarrier()) ? ((Unit)unit.getLocation())
            : null;
        final GoalDecider colonyDecider = getColonyDecider(deferOK);

        // Try for something sensible nearby.
        path = unit.search(startTile, colonyDecider,
                           CostDeciders.avoidIllegal(), MAX_TURNS, carrier);
        if (path != null) return path.getLastNode().getTile();

        // Retry, but increase the range.
        path = unit.search(startTile, colonyDecider,
                           CostDeciders.avoidIllegal(), MAX_TURNS*3, carrier);
        if (path != null) return path.getLastNode().getTile();

        // One more try with a relaxed cost decider and no range limit.
        path = unit.search(startTile, colonyDecider,
                           CostDeciders.numberOfTiles(), INFINITY, carrier);
        if (path != null) return path.getLastNode().getTile();
        
        // Enough.
        logger.finest("AI colony builder out of targets: " + unit);
        return null;
    }

    // Fake Transportable interface

    /**
     * Gets the transport destination for the unit with this mission.
     *
     * @return The destination for this <code>Transportable</code>.
     */
    public Location getTransportDestination() {
        return (shouldTakeTransportToTile(target)) ? target : null;
    }

    // Mission interface

    /**
     * Checks if this mission is valid for the given unit.
     *
     * @param aiUnit The <code>AIUnit</code> to test.
     * @return True if the unit can be usefully assigned this mission.
     */
    public static boolean isValid(AIUnit aiUnit) {
        return Mission.isValid(aiUnit)
            && aiUnit.getUnit().hasAbility("model.ability.foundColony");
    }

    /**
     * Checks if this mission is still valid to perform.
     *
     * @return True if this mission is still valid to perform.
     */
    public boolean isValid() {
        return super.isValid()
            && getUnit().hasAbility("model.ability.foundColony")
            && target != null;
    }

    /**
     * Performs this mission.
     *
     * @param connection The <code>Connection</code> to the server.
     */
    public void doMission(Connection connection) {
        // Check the target
        final Unit unit = getUnit();
        final Player player = unit.getOwner();
        final AIUnit aiUnit = getAIUnit();
        int value;
        if (target == null
            || (value = player.getColonyValue(target)) < colonyValue
            || (target.getColony() != null
                && target.getColony().getOwner() != player)) {
            if ((target = findTargetTile(aiUnit, true)) == null) return;
            colonyValue = player.getColonyValue(target);
        }

        if (travelToTarget("AI colony builder", target)
            != Unit.MoveType.MOVE) return;

        // If arrived at one of our colonies it is time to either
        // retarget or just join the current colony.  Only continue
        // looking for a building site if `few colonies', but insist on
        // finding a building site this time.
        if (target.getColony() != null) {
            String name = unit.getTile().getColony().getName();
            if (getEuropeanAIPlayer().hasFewColonies()) {
                target = findTargetTile(aiUnit, false);
                logger.finest("AI colony builder arrived at " + name
                    + ", retargeting " + target
                    + ": " + unit);
            } else {
                logger.finest("AI colony builder gives up and joins " + name
                    + ": " + unit);
                target = null;
            }
            return;
        }

        // Arrived at the target (non-colony) tile.
        if (target.getOwner() == null) {
            ; // All is well
        } else if (player.owns(target)) { // Already ours, clear users
            Colony colony = (Colony) target.getOwningSettlement();
            if (colony != null
                && colony.getColonyTile(target) != null) {
                colony.getColonyTile(target).relocateWorkers();
            }
        } else {
            // Not our tile, so claim it first.  Fail if someone has
            // claimed the tile and will not sell.  Otherwise try to
            // buy it (TODO: remove cheat) or steal it.
            int price = player.getLandPrice(target);
            boolean fail = price < 0;
            if (price > 0 && !player.checkGold(price)) {
                if (Utils.randomInt(logger, "Cheat gold",
                                    getAIRandom(), 4) == 0) {
                    // CHEAT: provide the gold needed 
                    player.modifyGold(price);
                }
            }
            if (price >= 0) {
                fail = !AIMessage.askClaimLand(target, aiUnit,
                    ((price == 0) ? 0 : (player.checkGold(price)) ? price
                        : NetworkConstants.STEAL_LAND))
                    || !player.owns(target);
            }
            if (fail) {
                logger.finest("AI colony builder failed to claim land at "
                    + unit.getTile() + ": " + unit);
                target = null;
                return;
            }
        }

        // Check that the unit has moves left, which are required for building.
        if (unit.getMovesLeft() <= 0) {
            logger.finest("AI colony builder waiting to build at " + target
                + ": " + unit);
            return;
        }
            
        // Clear to build the colony.
        if (AIMessage.askBuildColony(aiUnit, Player.ASSIGN_SETTLEMENT_NAME)
            && target.getColony() != null) {
            logger.finest("AI colony builder completed "
                + target.getColony().getName() + ": " + unit);
            aiUnit.setMission(new WorkInsideColonyMission(getAIMain(), aiUnit,
                    getAIMain().getAIColony(target.getColony())));
        } else {
            logger.warning("AI colony builder failed to build at " + target
                + ": " + unit);
            target = null;
        }
    }

    /**
     * Gets debugging information about this mission. This string is a short
     * representation of this object's state.
     *
     * @return The <code>String</code>: "(x, y) z" or "(x, y) z!"
     *         where <code>x</code> and <code>y</code> is the
     *         coordinates of the target tile for this mission, and
     *         <code>z</code> is the value of building the colony. The
     *         exclamation mark is added if the unit should continue
     *         searching for a colony site if the targeted site is
     *         lost.
     */
    public String getDebuggingInfo() {
        final String targetName = (target != null) ? target.toString()
            : "unassigned";
        return targetName + " " + colonyValue;
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
        writeAttribute(out, "target", target);
    }

    /**
     * Reads all the <code>AIObject</code>s and other AI-related information
     * from XML data.
     *
     * @param in The input stream with the XML.
     */
    protected void readAttributes(XMLStreamReader in)
        throws XMLStreamException {
        super.readAttributes(in);
        target = (Tile)getGame()
            .getFreeColGameObjectSafely(in.getAttributeValue(null, "target"));
        final Unit unit = getUnit();
        colonyValue = (unit == null) ? -1
            : unit.getOwner().getColonyValue(target);
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "buildColonyMission".
     */
    public static String getXMLElementTagName() {
        return "buildColonyMission";
    }
}
