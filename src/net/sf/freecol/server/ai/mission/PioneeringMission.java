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
import net.sf.freecol.common.model.FreeColGameObject;
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
import net.sf.freecol.server.ai.AIColony;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.ai.EuropeanAIPlayer;
import net.sf.freecol.server.ai.TileImprovementPlan;


/**
 * Mission for controlling a pioneer.
 *
 * @see net.sf.freecol.common.model.Unit.Role#PIONEER
 */
public class PioneeringMission extends Mission {

    private static final Logger logger = Logger.getLogger(PioneeringMission.class.getName());

    private static final String tag = "AI pioneer";

    /**
     * Maximum number of turns to travel to make progress on
     * pioneering.  This is low-ish because it is usually more
     * efficient to ship the tools where they are needed and either
     * create a new pioneer on site or send a hardy pioneer on
     * horseback.  The AI is probably smart enough to do the former
     * already, and one day the latter.
     */
    private static final int MAX_TURNS = 10;

    /**
     * Default distance in turns to a threatening unit.
     */
    private static final int DEFAULT_THREAT_TURNS = 3;

    /** The improvement this pioneer is to work on. */
    private TileImprovementPlan tileImprovementPlan = null;

    /**
     * The target location to go to:
     *   - a tile where the tileImprovement is
     *   - a colony to go to to equip
     *   - just an initial colony to retarget from
     */
    private Location target = null;


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

        setTarget(findTarget(aiUnit));
        logger.finest(tag + " starts with target " + target
            + ": " + aiUnit.getUnit());
        uninitialized = false;
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
        uninitialized = getAIUnit() == null;
    }

    /**
     * Get the best improvement associated with a tile.
     *
     * @return The <code>TileImprovementPlan</code>, or null if not found.
     */
    private TileImprovementPlan getBestPlan(Tile tile) {
        return getEuropeanAIPlayer().getBestPlan(tile);
    }

    /**
     * Get the best improvement associated with a tile.
     *
     * @return The <code>TileImprovementPlan</code>, or null if not found.
     */
    private static TileImprovementPlan getBestPlan(AIUnit aiUnit, Tile tile) {
        return ((EuropeanAIPlayer)aiUnit.getAIOwner()).getBestPlan(tile);
    }

    /**
     * Gets the target for this mission, either the colony to go to,
     * or the tile that needs improvement.
     *
     * @return The target for this mission.
     */
    public Location getTarget() {
        return target;
    }

    /**
     * Sets the target for this mission, and the tile improvement plan
     * as required.
     *
     * @return The target for this mission.
     */
    public void setTarget(Location target) {
        this.target = target;
        setTileImprovementPlan((target instanceof Tile)
            ? getBestPlan((Tile)target)
            : null);
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
        TileImprovementPlan old = tileImprovementPlan;
        this.tileImprovementPlan = tip;
        AIUnit aiUnit = getAIUnit();
        if (old != tileImprovementPlan) {
            if (old != null && old.getPioneer() == aiUnit) {
                old.setPioneer(null);
            }
            if (tileImprovementPlan != null) {
                tileImprovementPlan.setPioneer(aiUnit);
            }
        }
    }

    /**
     * Abandons the current plan if any.
     */
    private void abandonTileImprovementPlan() {
        if (tileImprovementPlan != null) setTileImprovementPlan(null);
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
     * Checks if a target is one of our colonies.
     *
     * @param aiUnit The <code>AIUnit</code> that needs tools.
     * @param target The target to check.
     * @return True if the colony can provide tools.
     */
    private static boolean isOurColonyTarget(AIUnit aiUnit, Location target) {
        if (!(target instanceof Colony)) return false;
        Colony colony = (Colony)target;
        return colony != null && !colony.isDisposed()
            && aiUnit.getUnit().getOwner().owns(colony);
    }

    /**
     * Checks if a target is a colony that can provide the tools
     * required for a pioneer.
     *
     * @param aiUnit The <code>AIUnit</code> that needs tools.
     * @param target The target to check.
     * @return True if the colony can provide tools.
     */
    private static boolean isColonyTarget(AIUnit aiUnit, Location target) {
        if (!isOurColonyTarget(aiUnit, target)) return false;
        Colony colony = (Colony)target;
        return colony.canProvideEquipment(Unit.Role.PIONEER
            .getRoleEquipment(colony.getSpecification()));
    }

    /**
     * Checks if a target is a tile with improvements required.
     *
     * @param aiUnit The <code>AIUnit</code> to improve with.
     * @param target The target to check.
     * @return True if the tile needs improvement.
     */
    private static boolean isTipTarget(AIUnit aiUnit, Location target) {
        return target instanceof Tile
            && getBestPlan(aiUnit, (Tile)target) != null;
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
        final Tile tile = (path == null) ? aiUnit.getUnit().getTile()
            : path.getLastNode().getTile();
        TileImprovementPlan tip;
        return (tile == null) ? null
            : (hasTools(aiUnit)) ? ((isTipTarget(aiUnit, tile)) ? tile : null)
            : (isColonyTarget(aiUnit, tile.getColony())) ? tile.getColony()
            : null;
    }

    /**
     * Evaluate a potential pioneering mission for a given unit and
     * path.
     *
     * @param aiUnit The <code>AIUnit</code> to do the mission.
     * @param path A <code>PathNode</code> to take to the target.
     * @return A score for the proposed mission.
     */
    public static int scorePath(AIUnit aiUnit, PathNode path) {
        int turns = (path == null) ? 1 : path.getTotalTurns() + 1;
        Location loc = extractTarget(aiUnit, path);
        TileImprovementPlan tip;
        return (isTipTarget(aiUnit, loc))
            ? 100 * getBestPlan(aiUnit, (Tile)loc).getValue() / turns
            : Integer.MIN_VALUE;
    }

    /**
     * Finds the closest colony within MAX_TURNS that can equip the unit.
     *
     * @param aiUnit The <code>AIUnit</code> to equip.
     * @return A path to the closest suitable colony.
     */
    private static PathNode findColonyPath(final AIUnit aiUnit) {
        Unit unit = aiUnit.getUnit();
        Tile startTile = unit.getPathStartTile();

        if (isColonyTarget(aiUnit, startTile.getColony())) return null;

        final GoalDecider equipDecider = new GoalDecider() {
                private PathNode best = null;
                private int bestValue = INFINITY;

                public PathNode getGoal() { return best; }
                public boolean hasSubGoals() { return true; }
                public boolean check(Unit u, PathNode path) {
                    if (isColonyTarget(aiUnit, path.getTile().getColony())
                        && path.getTotalTurns() < bestValue) {
                        bestValue = path.getTotalTurns();
                        best = path;
                        return true;
                    }
                    return false;
                }
            };
        return unit.search(startTile, equipDecider,
                           CostDeciders.avoidIllegal(), MAX_TURNS,
                           unit.getCarrier());
    }

    /**
     * Finds the best tile improvement plan for a supplied AI unit.
     * Public for the test suite.
     *
     * @param aiUnit The <code>AIUnit</code> to find a plan for.
     * @return A path to the best location to improve, or null if the
     *     unit is not on the map or the unit location is a suitable target.
     */
    private static PathNode findTipPath(AIUnit aiUnit) {
        final Unit unit = aiUnit.getUnit();
        final Tile startTile = unit.getPathStartTile();
        final GoalDecider pioneeringDecider
            = getMissionGoalDecider(aiUnit, PioneeringMission.class);

        return (startTile == null || isTipTarget(aiUnit, startTile)) ? null
            : unit.search(startTile, pioneeringDecider,
                          CostDeciders.avoidIllegal(), MAX_TURNS,
                          unit.getCarrier());
    }

    /**
     * Finds a suitable pioneering target for the supplied unit.
     *
     * @param aiUnit The <code>AIUnit</code> to test.
     * @return A <code>PathNode</code> to the target, or null if none found.
     */
    public static PathNode findTargetPath(final AIUnit aiUnit) {
        TileImprovementPlan tip;
        Unit unit;
        Tile startTile;
        return (aiUnit == null
            || (unit = aiUnit.getUnit()) == null || unit.isDisposed()
            || (startTile = unit.getPathStartTile()) == null) ? null
            : (hasTools(aiUnit)) ? findTipPath(aiUnit)
            : findColonyPath(aiUnit);
    }

    /**
     * Gets the Colony that most needs a pioneer.
     *
     * @param aiUnit The pioneer <code>AIUnit</code>.
     * @return The colony with the most outstanding tile improvement plans.
     */
    private static Colony getBestPioneeringColony(AIUnit aiUnit) {
        EuropeanAIPlayer aiPlayer = (EuropeanAIPlayer)aiUnit.getAIOwner();
        AIColony best = null;
        int bestValue = -1;
        for (AIColony aic : aiPlayer.getAIColonies()) {
            int value = aic.getTileImprovementPlans().size();
            if (value > bestValue) {
                bestValue = value;
                best = aic;
            }
        }
        return (best == null) ? null : best.getColony();
    }

    /**
     * Finds a suitable pioneering target for the supplied unit.
     * Falls back to the best settlement if the unit is not on the map.
     *
     * @param aiUnit The <code>AIUnit</code> to test.
     * @return A target for this mission.
     */
    public static Location findTarget(AIUnit aiUnit) {
        Location loc = extractTarget(aiUnit, findTargetPath(aiUnit));
        return (loc != null) ? loc
            : (!hasTools(aiUnit)) ? null
            : (aiUnit.getUnit().isInEurope()) ? getBestPioneeringColony(aiUnit)
            : null;
    }

    
    // Fake Transportable interface.

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
     * Checks if this mission is still valid to perform.
     *
     * @return True if this mission is still valid to perform.
     */
    public boolean isValid() {
        return super.isValid()
            && getUnit().isPerson()
            && getTarget() != null;
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
            && findTarget(aiUnit) != null;
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
        if (unit == null || unit.isDisposed()) {
            logger.finest(tag + " broken: " + unit);
            return;
        } else if (!unit.isPerson()) {
            logger.finest(tag + " not a person: " + unit);
            return;
        }

        final AIUnit aiUnit = getAIUnit();
        final Player player = unit.getOwner();
        final EuropeanAIPlayer aiPlayer = getEuropeanAIPlayer();
        PathNode path;
        Tile tile;
        String where;

        // Get tools first.
        while (!hasTools()) {
            if (!isOurColonyTarget(aiUnit, target)) {
                setTarget(extractTarget(aiUnit, findColonyPath(aiUnit)));
                logger.finest(tag + " retargeting for tools " + target
                    + ": " + unit);
                if (!isOurColonyTarget(aiUnit, target)) return;
            }

            // Go there and clear target on arrival.
            if (travelToTarget(tag, target) != Unit.MoveType.MOVE) return;
            where = ((Colony)target).getName();
            setTarget(null);

            // Equip
            if (aiUnit.equipForRole(Unit.Role.PIONEER, false)
                && hasTools()) {
                logger.finest(tag + " reached " + where
                    + " and equips: " + unit);
            } else {
                logger.finest(tag + " reached " + where
                    + " but fails to equip: " + unit);
            }
        }

        // Going to an intermediate colony?
        if (isOurColonyTarget(aiUnit, target)) {
            if (travelToTarget(tag, target) != Unit.MoveType.MOVE) return;
            where = ((Colony)target).getName();
            setTarget(null);
            logger.finest(tag + " reached intermediate colony " + where
                + ": " + unit);
        }

        // Now insist on a tip-target.
        if (tileImprovementPlan != null
            && !aiPlayer.validateTileImprovementPlan(tileImprovementPlan)) {
            setTarget(null);
        }
        if (tileImprovementPlan == null) {
            setTarget(extractTarget(aiUnit, findTipPath(aiUnit)));
            if (tileImprovementPlan == null) {
                logger.finest(tag + " at " + unit.getLocation() 
                    + " could not find improvement: " + unit);
                return;
            }
            logger.finest(tag + " retargeting " + tileImprovementPlan
                + ": " + unit);
        }

        // Go there.
        if (travelToTarget(tag, target) != Unit.MoveType.MOVE) return;

        // Take control of the land before proceeding to improve.
        tile = target.getTile();
        if (!player.owns(tile)) {
            // TODO: Better choice whether to pay or steal.
            // Currently always pay if we can, steal if we can not.
            boolean fail = false;
            int price = player.getLandPrice(tile);
            if (price < 0) {
                fail = true;
            } else {
                if (price > 0 && !player.checkGold(price)) {
                    price = NetworkConstants.STEAL_LAND;
                }
                if (!AIMessage.askClaimLand(tile, aiUnit, price)
                    || !player.owns(tile)) { // Failed to take ownership
                    fail = true;
                }
            }
            if (fail) {
                aiPlayer.removeTileImprovementPlan(tileImprovementPlan);
                tileImprovementPlan.dispose();
                setTarget(null);
                logger.finest(tag + " can not claim land at " + tile
                    + ": " + unit);
                return;
            }
        }

        // Check for threats
        int turnsNeeded = DEFAULT_THREAT_TURNS;
        if (unit.getWorkImprovement() != null) {
            turnsNeeded = Math.min(turnsNeeded, unit.getWorkLeft());
        }
        if (unit.isInDanger(turnsNeeded, 0.25f)) {
            PathNode safe = unit.findOurNearestSettlement(false, 1);
            if (safe != null) {
                travelToTarget(tag + " (evading)",
                               safe.getLastNode().getTile());
            }
            return;
        }

        // Work on the improvement
        if (unit.getState() == UnitState.IMPROVING) {
            unit.setMovesLeft(0);
            logger.finest(tag + " improving "
                + tileImprovementPlan.getType() + ": " + unit);
        } else if (unit.checkSetState(UnitState.IMPROVING)) {
            if (AIMessage.askChangeWorkImprovementType(aiUnit,
                    tileImprovementPlan.getType())) {
                logger.finest(tag + " began improvement "
                    + tileImprovementPlan.getType()
                    + " at target " + tile + ": " + unit);
            } else {
                setTarget(null);
                aiPlayer.removeTileImprovementPlan(tileImprovementPlan);
                tileImprovementPlan.dispose();
                logger.finest(tag + " failed to improve " + tile + ": " + unit);
            }
        } else { // Probably just out of moves.
            logger.finest(tag + " waiting to improve at " + tile + ": " + unit);
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
            if (target == null) return "No target";
            return "Getting tools from " + target;
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

        if (target != null) {
            writeAttribute(out, "target", (FreeColGameObject)target);
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
        if (fcgo instanceof Colony || fcgo instanceof Tile) {
            target = (Location)fcgo; // Do not use setTarget in serialization
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
