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

import java.util.HashMap;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.NetworkConstants;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.ai.EuropeanAIPlayer;
import net.sf.freecol.server.ai.TileImprovementPlan;

import org.w3c.dom.Element;


/**
 * Mission for controlling a pioneer.
 *
 * @see net.sf.freecol.common.model.Unit.Role#PIONEER
 */
public class PioneeringMission extends Mission {

    private static final Logger logger = Logger.getLogger(PioneeringMission.class.getName());

    /**
     * Maximum number of turns to travel to make progress on
     * pioneering.  This is low-ish because it is usually more
     * efficient to ship the tools where they are needed and either
     * create a new pioneer on site or send a hardy pioneer on
     * horseback.  The AI is probably smart enough to do the former
     * already, and one day the latter.
     */
    private static final int MAX_TURNS = 10;

    /** The improvement this pioneer is to work on. */
    private TileImprovementPlan tileImprovementPlan = null;

    /** A colony to go to to equip if required. */
    private Colony colonyWithTools = null;


    /**
     * Creates a pioneering mission for the given <code>AIUnit</code>.
     * Note that PioneeringMission.isValid(aiUnit) should be called
     * before this, to guarantee that
     * findTileImprovementPlan/findColonyWithTools succeed.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission
     *        is created for.
     */
    public PioneeringMission(AIMain aiMain, AIUnit aiUnit) {
        super(aiMain, aiUnit);

        if (!hasTools()) colonyWithTools = findColonyWithTools(aiUnit);
        tileImprovementPlan = findTileImprovementPlan(aiUnit);
        tileImprovementPlan.setPioneer(aiUnit);
        logger.finest("AI pioneer starts with plan "
            + tileImprovementPlan + "/" + tileImprovementPlan.getTarget()
            + ": " + aiUnit.getUnit());
    }

    /**
     * Loads a mission from the given element.
     *
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an
     *      XML-representation of this object.
     */
    public PioneeringMission(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
    }

    /**
     * Creates a new <code>PioneeringMission</code> and reads the
     * given element.
     *
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public PioneeringMission(AIMain aiMain, XMLStreamReader in)
        throws XMLStreamException {
        super(aiMain);
        readFromXML(in);
    }

    /**
     * Gets the <code>TileImprovementPlan</code> for this mission.
     *
     * @return The <code>TileImprovementPlan</code>.
     */
    public TileImprovementPlan getTileImprovementPlan() {
        return tileImprovementPlan;
    }

    /**
     * Sets the <code>TileImprovementPlan</code> which should
     * be the next target.
     *
     * @param tip The <code>TileImprovementPlan</code>.
     */
    public void setTileImprovementPlan(TileImprovementPlan tip) {
        this.tileImprovementPlan = tip;
    }

    /**
     * Abandons the current plan if any.
     */
    private void abandonTileImprovementPlan() {
        if (tileImprovementPlan != null) {
            if (tileImprovementPlan.getPioneer() == getAIUnit()) {
                tileImprovementPlan.setPioneer(null);
            }
            tileImprovementPlan = null;
        }
    }

    /**
     * Disposes of this pioneering mission.
     */
    public void dispose() {
        abandonTileImprovementPlan();
        super.dispose();
    }


    /**
     * Does a supplied unit have tools?
     *
     * @param unit The pioneer <code>Unit</code> to check.
     * @return True if the pioneer has tools.
     */
    private static boolean hasTools(AIUnit aiUnit) {
        return aiUnit.getUnit().hasAbility("model.ability.improveTerrain");
    }

    /**
     * Does this pioneer have tools?
     *
     * @return True if the pioneer has tools.
     */
    private boolean hasTools() {
        return hasTools(getAIUnit());
    }

    /**
     * Checks if a colony can provide the tools required for a pioneer.
     *
     * @param aiUnit The <code>AIUnit</code> that needs tools.
     * @param colony The <code>Colony</code> to check.
     * @return True if the colony can provide tools.
     */
    private static boolean checkColonyForTools(AIUnit aiUnit, Colony colony) {
        return colony != null
            && !colony.isDisposed()
            && colony.getOwner() == aiUnit.getUnit().getOwner()
            && colony.canProvideEquipment(Unit.Role.PIONEER
                .getRoleEquipment(colony.getSpecification()));
    }


    /**
     * Finds the closest colony within MAX_TURNS that can equip the unit.
     * Public for the test suite.
     *
     * @param aiUnit The <code>AIUnit</code> to equip.
     * @return The closest colony that can equip the unit.
     */
    public static Colony findColonyWithTools(final AIUnit aiUnit) {
        final Unit unit = aiUnit.getUnit();
        if (unit == null || unit.isDisposed()) return null;

        final Tile startTile = getPathStartTile(unit);
        if (startTile == null) return null;
        if (checkColonyForTools(aiUnit, startTile.getColony())) {
            return startTile.getColony();
        }

        final GoalDecider equipDecider = new GoalDecider() {
                private PathNode best = null;

                public PathNode getGoal() { return best; }
                public boolean hasSubGoals() { return false; }
                public boolean check(Unit u, PathNode path) {
                    Colony colony = path.getTile().getColony();
                    if (checkColonyForTools(aiUnit, colony)) {
                        best = path;
                        return true;
                    }
                    return false;
                }
            };
        PathNode path = unit.search(startTile, equipDecider,
            CostDeciders.avoidIllegal(), MAX_TURNS, 
            (unit.isOnCarrier()) ? ((Unit)unit.getLocation()) : null);
        return (path == null) ? null : path.getLastNode().getTile().getColony();
    }

    /**
     * Weeds out a broken or obsolete tile improvement plan.
     *
     * @param tip The <code>TileImprovementPlan</code> to test.
     * @param aiPlayer The <code>AIPlayer</code> that owns the plan.
     * @return True if the plan survives this check.
     */
    private static boolean validateTileImprovementPlan(TileImprovementPlan tip,
        EuropeanAIPlayer aiPlayer) {
        if (tip == null) return false;
        Tile target = tip.getTarget();
        if (target == null) {
            logger.warning("Removing targetless TileImprovementPlan");
            aiPlayer.removeTileImprovementPlan(tip);
            tip.dispose();
            return false;
        }
        if (target.hasImprovement(tip.getType())) {
            logger.finest("Removing obsolete TileImprovementPlan");
            aiPlayer.removeTileImprovementPlan(tip);
            tip.dispose();
            return false;
        }
        if (tip.getPioneer() != null
            && (tip.getPioneer().getUnit() == null
                || tip.getPioneer().getUnit().isDisposed())) {
            logger.warning("Clearing broken pioneer for TileImprovementPlan");
            tip.setPioneer(null);
        }
        return true;
    }

    /**
     * Checks that a tile improvement plan is valid.
     *
     * @param tip The <code>TileImprovementPlan</code> to check.
     * @return True if the plan is valid.
     */
    private boolean checkTileImprovementPlan(TileImprovementPlan tip) {
        return validateTileImprovementPlan(tip, getEuropeanAIPlayer());
    }

    /**
     * Finds the best tile improvement plan for a supplied AI unit.
     * Public for the test suite.
     *
     * @param aiUnit The <code>AIUnit</code> to find a plan for.
     * @return The best available tile improvement plan, or null if none found.
     */
    public static TileImprovementPlan findTileImprovementPlan(AIUnit aiUnit) {
        final Unit unit = aiUnit.getUnit();
        if (unit == null || unit.isDisposed()) return null;

        final Tile startTile = getPathStartTile(unit);
        if (startTile == null) return null;

        // Build the TileImprovementPlan map.
        final HashMap<Tile, TileImprovementPlan> tipMap
            = new HashMap<Tile, TileImprovementPlan>();
        final EuropeanAIPlayer aiPlayer
            = (EuropeanAIPlayer)aiUnit.getAIMain().getAIPlayer(unit.getOwner());
        for (TileImprovementPlan tip : aiPlayer.getTileImprovementPlans()) {
            if (!validateTileImprovementPlan(tip, aiPlayer)) continue;
            if (tip.getPioneer() == aiUnit) return tip;
            if (tip.getPioneer() != null) continue;
            if (startTile == tip.getTarget()) return tip;
            TileImprovementPlan other = tipMap.get(tip.getTarget());
            if (other == null || other.getValue() < tip.getValue()) {
                tipMap.put(tip.getTarget(), tip);
            }
        }

        // Find the best TileImprovementPlan.
        final GoalDecider tipDecider = new GoalDecider() {
                private PathNode best = null;
                private int bestValue = Integer.MIN_VALUE;

                public PathNode getGoal() { return best; }
                public boolean hasSubGoals() { return false; }
                public boolean check(Unit u, PathNode path) {
                    TileImprovementPlan tip = tipMap.get(path.getTile());
                    if (tip != null) {
                        int value = tip.getValue() - 5 * path.getTotalTurns();
                        if (value > bestValue) {
                            bestValue = value;
                            best = path;
                            return true;
                        }
                    }
                    return false;
                }
            };
        if (tipMap.get(startTile) != null) return tipMap.get(startTile);
        PathNode path = (tipMap.isEmpty()) ? null
            : unit.search(startTile, tipDecider,
                CostDeciders.avoidIllegal(), MAX_TURNS,
                (unit.isOnCarrier()) ? ((Unit)unit.getLocation()) : null);
        return (path == null) ? null : tipMap.get(path.getLastNode().getTile());
    }

    // Fake Transportable interface.

    /**
     * Gets the transport destination for units with this mission.
     *
     * @return The destination for this <code>Transportable</code>.
     */
    public Location getTransportDestination() {
        Tile target = (hasTools())
            ? ((!checkTileImprovementPlan(tileImprovementPlan)) ? null
                : tileImprovementPlan.getTarget())
            : ((!checkColonyForTools(getAIUnit(), colonyWithTools)) ? null
                : colonyWithTools.getTile());
        return (shouldTakeTransportToTile(target)) ? target : null;
    }

    // Mission interface

    /**
     * Checks if this mission is still valid to perform.
     *
     * @return True if this mission is still valid to perform.
     */
    public boolean isValid() {
        return super.isValid()
            && getUnit().isPerson()
            && checkTileImprovementPlan(tileImprovementPlan)
            && (hasTools() || checkColonyForTools(getAIUnit(), colonyWithTools));
    }

    /**
     * Checks if this mission is valid for the given unit.
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @return True if the AI unit can be assigned a PioneeringMission.
     */
    public static boolean isValid(AIUnit aiUnit) {
        return Mission.isValid(aiUnit)
            && aiUnit.getUnit().isPerson()
            && findTileImprovementPlan(aiUnit) != null
            && (hasTools(aiUnit) || findColonyWithTools(aiUnit) != null);
    }

    /**
     * Performs this mission.
     *
     * - Gets tools if needed.
     * - Makes sure we have a valid plan.
     * - Get to the target.
     * - Claim it if necessary.
     * - Make the improvement.
     *
     * @param connection The <code>Connection</code> to the server.
     */
    public void doMission(Connection connection) {
        final Unit unit = getUnit();
        if (unit.getTile() == null) {
            logger.finest("AI pioneer waiting to go to"
                + getTransportDestination() + ": " + unit);
            return;
        }

        if (!hasTools()) { // Try to equip.
            if (colonyWithTools != null
                && !checkColonyForTools(getAIUnit(), colonyWithTools)) {
                colonyWithTools = null;
            }
            if (colonyWithTools == null) { // Find a new colony.
                colonyWithTools = findColonyWithTools(getAIUnit());
            }
            if (colonyWithTools == null) {
                abandonTileImprovementPlan();
                logger.finest("AI pioneer can not find equipment: " + unit);
                return;
            }

            // Go there, equip unit.
            if (travelToTarget("AI pioneer", colonyWithTools.getTile())
                != Unit.MoveType.MOVE) return;
            getAIUnit().equipForRole(Unit.Role.PIONEER, false);
            if (!hasTools()) {
                abandonTileImprovementPlan();
                logger.finest("AI pioneer reached " + colonyWithTools.getName()
                    + " but could not equip: " + unit);
                return;
            }
            logger.finest("AI pioneer reached " + colonyWithTools.getName()
                + " and equips: " + unit);
            colonyWithTools = null;
        }

        // Check the plan still makes sense.
        final Player player = unit.getOwner();
        final EuropeanAIPlayer aiPlayer = getEuropeanAIPlayer();
        if (tileImprovementPlan != null
            && !validateTileImprovementPlan(tileImprovementPlan, aiPlayer)) {
            tileImprovementPlan = null;
        }
        if (tileImprovementPlan == null) { // Find a new plan.
            AIUnit aiu = getAIUnit();
            tileImprovementPlan = findTileImprovementPlan(aiu);
            if (tileImprovementPlan == null) {
                logger.finest("AI pioneer could not find an improvement: "
                    + unit);
                return;
            }
            tileImprovementPlan.setPioneer(aiu);
        }
    
        // Go to target and take control of the land before proceeding
        // to build.
        Tile target = tileImprovementPlan.getTarget();
        if (travelToTarget("AI pioneer", target) != Unit.MoveType.MOVE) return;
        if (!player.owns(target)) {
            // TODO: Better choice whether to pay or steal.
            // Currently always pay if we can, steal if we can not.
            boolean fail = false;
            int price = player.getLandPrice(target);
            if (price < 0) {
                fail = true;
            } else {
                if (price > 0 && !player.checkGold(price)) {
                    price = NetworkConstants.STEAL_LAND;
                }
                if (!AIMessage.askClaimLand(aiPlayer.getConnection(), target,
                                            null, price)
                    || !player.owns(target)) { // Failed to take ownership
                    fail = true;
                }
            }
            if (fail) {
                aiPlayer.removeTileImprovementPlan(tileImprovementPlan);
                tileImprovementPlan.dispose();
                tileImprovementPlan = null;
                logger.finest("AI pioneer can not claim land at " + target
                    + ": " + unit);
                return;
            }
        }

        if (unit.getState() == UnitState.IMPROVING) {
            unit.setMovesLeft(0);
            logger.finest("AI pioneer improving "
                + tileImprovementPlan.getType() + ": " + unit);
        } else if (unit.checkSetState(UnitState.IMPROVING)) {
            // Ask to create the TileImprovement
            if (AIMessage.askChangeWorkImprovementType(getAIUnit(),
                    tileImprovementPlan.getType())) {
                logger.finest("AI pioneer began improvement "
                    + tileImprovementPlan.getType()
                    + " at target " + target
                    + ": " + unit);
            } else {
                aiPlayer.removeTileImprovementPlan(tileImprovementPlan);
                tileImprovementPlan.dispose();
                tileImprovementPlan = null;
                logger.finest("AI pioneer failed to improve " + target
                    + ": " + unit);
            }
        } else { // Probably just out of moves.
            logger.finest("AI pioneer waiting to improve at " + target.getId()
                + ": " + unit);
        }
    }

    /**
     * Gets debugging information about this mission.
     * This string is a short representation of this
     * object's state.
     *
     * @return The <code>String</code>:
     *      <ul>
     *          <li>"(x, y) P" (for plowing)</li>
     *          <li>"(x, y) R" (for building road)</li>
     *          <li>"(x, y) Getting tools: (x, y)"</li>
     *      </ul>
     */
    public String getDebuggingInfo() {
        if (hasTools()) {
            if (tileImprovementPlan == null) return "No target";
            final String action = tileImprovementPlan.getType().getNameKey();
            return tileImprovementPlan.getTarget().getPosition().toString()
                + " " + action;
        } else {
            if (colonyWithTools == null) return "No target";
            return "Getting tools from " + colonyWithTools.getName();
        }
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
        writeAttribute(out, "tileImprovementPlan", tileImprovementPlan);
    }

    /**
     * {@inherit-doc}
     */
    protected void readAttributes(XMLStreamReader in)
        throws XMLStreamException {
        super.readAttributes(in);

        final String tileImprovementPlanStr
            = in.getAttributeValue(null, "tileImprovementPlan");
        if (tileImprovementPlanStr != null) {
            tileImprovementPlan = (TileImprovementPlan)
                getAIMain().getAIObject(tileImprovementPlanStr);
            if (tileImprovementPlan == null) {
                tileImprovementPlan = new TileImprovementPlan(getAIMain(),
                    tileImprovementPlanStr);
            }
        } else {
            tileImprovementPlan = null;
        }
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "pioneeringMission".
     */
    public static String getXMLElementTagName() {
        return "pioneeringMission";
    }
}
