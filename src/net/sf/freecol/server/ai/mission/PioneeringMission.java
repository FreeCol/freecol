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
        logger.finest(tag + " starts with target " + target + ": " + this);
        uninitialized = false;
    }

    /**
     * Creates a pioneering mission for the given <code>AIUnit</code>.
     * Note that PioneeringMission.isValid(aiUnit) should be called
     * before this, to guarantee that
     * findTileImprovementPlan/findColonyWithTools succeed.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission
     *        is created for.
     * @param loc The target <code>Location</code>.
     */
    public PioneeringMission(AIMain aiMain, AIUnit aiUnit, Location loc) {
        super(aiMain, aiUnit);

        setTarget(loc);
        logger.finest(tag + " starts with target " + target + ": " + this);
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
     * Get the best improvement associated with a tile for a given unit.
     * Take care to first check if the unit has a plan already, if so,
     * return that.
     *
     * @return The <code>TileImprovementPlan</code>, or null if not found.
     */
    private static TileImprovementPlan getBestPlan(AIUnit aiUnit, Tile tile) {
        return ((EuropeanAIPlayer)aiUnit.getAIOwner()).getBestPlan(tile);
    }

    /**
     * Sets the target for this mission, and the tile improvement plan
     * as required.
     *
     * @param target The new target for this mission.
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
     * @param aiUnit The pioneer <code>AIUnit</code> to check.
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
            : (Mission.invalidAIUnitReason(aiUnit) != null) ? null
            : (hasTools(aiUnit))
            ? ((invalidPioneeringTileReason(aiUnit, tile) == null)
                ? tile : null)
            : ((invalidPioneeringColonyReason(aiUnit, tile.getColony()) == null)
                ? tile.getColony() : null);
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
        return (loc instanceof Colony) ? (100 / turns)
            : (loc instanceof Tile) ? (100 * getBestPlan(aiUnit, (Tile)loc)
                .getValue() / turns)
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

        Colony colony = startTile.getColony();
        if (colony != null
            && invalidPioneeringColonyReason(aiUnit, colony) == null) {
            return null;
        }

        final GoalDecider equipDecider = new GoalDecider() {
                private PathNode best = null;
                private int bestValue = INFINITY;

                public PathNode getGoal() { return best; }
                public boolean hasSubGoals() { return true; }
                public boolean check(Unit u, PathNode path) {
                    Colony colony = path.getTile().getColony();
                    if (invalidPioneeringColonyReason(aiUnit, colony) == null
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

        return (startTile == null
            || invalidPioneeringTileReason(aiUnit, startTile) == null) ? null
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
     * Gets the target for this mission, either the colony to go to,
     * or the tile that needs improvement.
     *
     * @return The target for this mission.
     */
    public Location getTarget() {
        return target;
    }

    /**
     * Why would a PioneeringMission be invalid with the given unit.
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @return A reason why the mission would be invalid with the unit,
     *     or null if none found.
     */
    private static String invalidPioneeringReason(AIUnit aiUnit) {
        return (!aiUnit.getUnit().isPerson()) ? Mission.UNITNOTAPERSON
            : null;
    }

    /**
     * Why would a PioneeringMission be invalid with the given unit and colony.
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @param colony The <code>Colony</code> to check.
     * @return A reason why the mission would be invalid, or null if
     *     none found.
     */
    private static String invalidPioneeringColonyReason(AIUnit aiUnit,
                                                        Colony colony) {
        String reason;
        return ((reason = invalidTargetReason(colony,
                    aiUnit.getUnit().getOwner())) != null) ? reason
            : (aiUnit.getUnit().getTile() == null
                || aiUnit.getUnit().isOnCarrier()) ? null 
            : (!colony.canProvideEquipment(Unit.Role.PIONEER
                    .getRoleEquipment(colony.getSpecification()))
                && !hasTools(aiUnit)) ? "colony-can-not-provide-equipment"
            : null;
    }

    /**
     * Gets the existing tile improvement plan for a unit and tile.
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @param tile The <code>Tile</code> to check.
     * @return The associated <code>TileImprovementPlan</code>.
     */
    private static TileImprovementPlan getPlan(AIUnit aiUnit, Tile tile) {
        if (aiUnit.getMission() instanceof PioneeringMission) {
            PioneeringMission pm = (PioneeringMission)aiUnit.getMission();
            if (pm.getTileImprovementPlan() != null
                && pm.getTileImprovementPlan().getTarget() == tile) {
                return pm.getTileImprovementPlan();
            }
        }
        return null;
    }

    /**
     * Why would a PioneeringMission be invalid with the given unit and tile.
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @param tile The <code>Tile</code> to check.
     * @return A reason why the mission would be invalid, or null if none found.
     */
    private static String invalidPioneeringTileReason(AIUnit aiUnit,
                                                      Tile tile) {
        return (tile == null) ? Mission.TARGETINVALID
            : (!hasTools(aiUnit)) ? "unit-needs-tools"
            : (getPlan(aiUnit, tile) == null
                && getBestPlan(aiUnit, tile) == null) ? "tile-has-no-plan"
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
            : ((reason = invalidPioneeringReason(aiUnit)) != null) ? reason
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
            : ((reason = invalidPioneeringReason(aiUnit)) != null) ? reason
            : (loc instanceof Colony)
            ? invalidPioneeringColonyReason(aiUnit, (Colony)loc)
            : (loc instanceof Tile)
            ? invalidPioneeringTileReason(aiUnit, (Tile)loc)
            : Mission.TARGETINVALID;
    }

    // Not a one-time mission, omit isOneTime().

    /**
     * Performs this mission.
     *
     * - Gets tools if needed.
     * - Makes sure we have a valid plan.
     * - Get to the target.
     * - Claim it if necessary.
     * - Make the improvement.
     */
    public void doMission() {
        final Unit unit = getUnit();
        String reason = invalidReason();
        if (isTargetReason(reason)) {
            ; // handled below
        } else if (reason != null) {
            logger.finest(tag + " broken(" + reason + "): " + this);
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
            if (invalidTargetReason(target, player) != null) {
                setTarget(extractTarget(aiUnit, findColonyPath(aiUnit)));
                if (invalidTargetReason(target, player) != null) {
                    logger.finest(tag + " unable to retarget: " + this);
                    return;
                }
                logger.finest(tag + " retargeting for tools " + target
                    + ": " + this);
            }

            // Go there and clear target on arrival.
            if (travelToTarget(tag, target) != Unit.MoveType.MOVE) return;
            where = ((Colony)target).getName();
            setTarget(null);

            // Equip
            if (aiUnit.equipForRole(Unit.Role.PIONEER, false)
                && hasTools()) {
                logger.finest(tag + " reached " + where
                    + " and equips: " + this);
            } else {
                logger.finest(tag + " reached " + where
                    + " but fails to equip: " + this);
            }
        }

        // Going to an intermediate colony?
        if (target instanceof Colony
            && invalidTargetReason(target, player) == null) {
            if (travelToTarget(tag, target) != Unit.MoveType.MOVE) return;
            where = ((Colony)target).getName();
            setTarget(null);
            logger.finest(tag + " reached intermediate colony " + where
                + ": " + this);
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
                    + " could not find improvement: " + this);
                return;
            }
            logger.finest(tag + " retargeting " + tileImprovementPlan
                + ": " + this);
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
                    + ": " + this);
                return;
            }
        }

        // Check for threats
        int turnsNeeded = DEFAULT_THREAT_TURNS;
        if (unit.getWorkImprovement() != null) {
            turnsNeeded = Math.min(turnsNeeded, unit.getWorkLeft());
        }
        if (unit.isInDanger(turnsNeeded, 0.25f)) {
            PathNode safe = unit.findOurNearestSettlement(false, 1, false);
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
                + tileImprovementPlan.getType() + ": " + this);
        } else if (unit.checkSetState(UnitState.IMPROVING)) {
            if (AIMessage.askChangeWorkImprovementType(aiUnit,
                    tileImprovementPlan.getType())) {
                logger.finest(tag + " began improvement "
                    + tileImprovementPlan.getType()
                    + " at target " + tile + ": " + this);
            } else {
                setTarget(null);
                aiPlayer.removeTileImprovementPlan(tileImprovementPlan);
                tileImprovementPlan.dispose();
                logger.finest(tag + " failed to improve " + tile + ": " + this);
            }
        } else { // Probably just out of moves.
            logger.finest(tag + " waiting to improve at " + tile + ": " + this);
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

        // Do not use setTarget in serialization
        String str = in.getAttributeValue(null, "target");
        target = getGame().getFreeColLocation(str);
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
