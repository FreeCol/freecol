/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.pathfinding.CostDecider;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.common.model.pathfinding.GoalDeciders;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIUnit;


/**
 * Mission for controlling a scout.
 */
public class ScoutingMission extends Mission {

    private static final Logger logger = Logger.getLogger(ScoutingMission.class.getName());

    /** The tag for this mission. */
    private static final String tag = "AI scout";

    /**
     * The target for this mission.  Either a tile with an LCR, a
     * native settlement to talk to the chief of, or a player colony
     * to retarget from.
     */
    private Location target;


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     * @param target The target <code>Location</code>.
     * @param lb A <code>LogBuilder</code> to log to.
     */
    public ScoutingMission(AIMain aiMain, AIUnit aiUnit, Location target,
                           LogBuilder lb) {
        super(aiMain, aiUnit, target, lb);
    }

    /**
     * Creates a new <code>ScoutingMission</code> and reads the given element.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     * @param xr The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public ScoutingMission(AIMain aiMain, AIUnit aiUnit,
                           FreeColXMLReader xr) throws XMLStreamException {
        super(aiMain, aiUnit);

        readFromXML(xr);
    }


    /**
     * Does a supplied unit have horses?
     *
     * @param aiUnit The scout <code>AIUnit</code> to check.
     * @return True if the scout has horses.
     */
    private static boolean canScoutNatives(AIUnit aiUnit) {
        return aiUnit.getUnit()
            .hasAbility(Ability.SPEAK_WITH_CHIEF);
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
        final Location loc = path.getLastNode().getLocation();
        return (loc == null) ? null
            : (invalidSettlementReason(aiUnit, loc.getSettlement()) == null)
            ? loc.getSettlement()
            : (invalidTileReason(aiUnit, loc.getTile()) == null)
            ? loc.getTile()
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
        Location loc = (path == null) ? null : extractTarget(aiUnit, path);
        return (loc instanceof Colony)
            ? 12 / (path.getTotalTurns() + 1)
            : (loc instanceof IndianSettlement)
            ? 2000 / (path.getTotalTurns() + 1)
            : (loc instanceof Tile)
            ? 1000 / (path.getTotalTurns() + 1)
            : Integer.MIN_VALUE;
    }

    /**
     * Gets a <code>GoalDecider</code> for finding the best colony
     * <code>Tile</code>, optionally falling back to the nearest colony.
     *
     * @param aiUnit The <code>AIUnit</code> that is searching.
     * @param deferOK Enable colony fallback.
     * @return A suitable <code>GoalDecider</code>.
     */
    private static GoalDecider getGoalDecider(final AIUnit aiUnit,
                                              boolean deferOK) {
        GoalDecider gd = new GoalDecider() {
                private PathNode bestPath = null;
                private int bestValue = Integer.MIN_VALUE;

                public PathNode getGoal() { return bestPath; }
                public boolean hasSubGoals() { return true; }
                public boolean check(Unit u, PathNode path) {
                    Location loc = extractTarget(aiUnit, path);
                    if (loc instanceof IndianSettlement
                        || loc instanceof Tile) {
                        int value = scorePath(aiUnit, path);
                        if (bestValue < value) {
                            bestValue = value;
                            bestPath = path;
                            return true;
                        }
                    }
                    return false;
                }
            };
        return (deferOK) ? GoalDeciders.getComposedGoalDecider(false, gd,
            GoalDeciders.getOurClosestSettlementGoalDecider())
            : gd;
    }

    /**
     * Finds a suitable scouting target for the supplied unit.
     *
     * @param aiUnit The <code>AIUnit</code> to execute this mission.
     * @param range An upper bound on the number of moves.
     * @param deferOK Enables deferring to a fallback colony.
     * @return A path to the new target, or null if none found.
     */
    public static PathNode findTargetPath(AIUnit aiUnit, int range,
                                          boolean deferOK) {
        if (invalidAIUnitReason(aiUnit) != null) return null;
        final Unit unit = aiUnit.getUnit();
        final Tile startTile = unit.getPathStartTile();
        if (startTile == null) return null;

        final Unit carrier = unit.getCarrier();
        final GoalDecider gd = getGoalDecider(aiUnit, deferOK);
        final CostDecider standardCd = CostDeciders.avoidIllegal();

        // Can the scout legally reach a valid target from where it
        // currently is?
        return unit.search(startTile, gd, standardCd, range, carrier);
    }

    /**
     * Finds a suitable scouting target for the supplied unit.
     * Falls back to the best settlement if a path is not found.
     *
     * @param aiUnit The <code>AIUnit</code> to test.
     * @param range An upper bound on the number of moves.
     * @param deferOK Enables deferring to a fallback colony.
     * @return A <code>PathNode</code> to the target, or null if none found.
     */
    public static Location findTarget(AIUnit aiUnit, int range,
                                      boolean deferOK) {
        PathNode path = findTargetPath(aiUnit, range, deferOK);
        return (path != null) ? extractTarget(aiUnit, path)
            : upLoc(findCircleTarget(aiUnit, getGoalDecider(aiUnit, deferOK),
                                     range*3, deferOK));
    }

    /**
     * Prepare a unit for this mission.  Allow even experts to proceed
     * even if not mounted.
     *
     * @param aiUnit The <code>AIUnit</code> to prepare.
     * @return A reason why the unit can not perform this mission, or null
     *     if none.
     */
    public static String prepare(AIUnit aiUnit) {
        String reason = invalidReason(aiUnit);
        return (reason != null) ? reason
            : (canScoutNatives(aiUnit)
                || aiUnit.equipForRole("model.role.scout")
                || aiUnit.getUnit().hasAbility(Ability.EXPERT_SCOUT)) ? null
            : "unit-unprepared-to-SCOUT";
    }

    /**
     * Why would this mission be invalid with the given unit.
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @return A reason why the mission would be invalid with the unit,
     *     or null if none found.
     */
    private static String invalidMissionReason(AIUnit aiUnit) {
        String reason = invalidAIUnitReason(aiUnit);
        return (reason != null) ? reason
            : (!canScoutNatives(aiUnit)) ? "unit-not-a-SCOUT"
            : null;
    }

    /**
     * Why is this mission invalid with a given colony target?
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @param colony The potential target <code>Colony</code>.
     * @return A reason for mission invalidity, or null if none found.
     */
    private static String invalidColonyReason(AIUnit aiUnit, Colony colony) {
        return invalidTargetReason(colony, aiUnit.getUnit().getOwner());
    }

    /**
     * Why is this mission invalid with a given native settlement target?
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @param is The potential target <code>IndianSettlement</code>.
     * @return A reason for mission invalidity, or null if none found.
     */
    private static String invalidIndianSettlementReason(AIUnit aiUnit,
                                                        IndianSettlement is) {
        final Unit unit = aiUnit.getUnit();
        final Player owner = unit.getOwner();
        Tension tension;
        String reason = invalidTargetReason(is);
        return (reason != null) ? reason
            : (is.hasScouted(owner))
            ? "settlement-scouted"
            : ((tension = is.getAlarm(owner)) != null
                && tension.getValue() >= Tension.Level.HATEFUL.getLimit())
            ? "settlement-hateful"
            : null;
    }

    /**
     * Is this a valid scouting target because it is a suitable native
     * settlement or an intermediate colony.
     *
     * @param aiUnit The <code>AIUnit</code> to test.
     * @param settlement The <code>Settlement</code> to test.
     * @return A reason why the mission would be invalid, or null if none found.
     */
    private static String invalidSettlementReason(AIUnit aiUnit,
                                                  Settlement settlement) {
        return (settlement instanceof Colony)
            ? invalidColonyReason(aiUnit, (Colony)settlement)
            : (settlement instanceof IndianSettlement)
            ? invalidIndianSettlementReason(aiUnit, (IndianSettlement)settlement)
            : Mission.TARGETINVALID;
    }

    /**
     * Is this a valid scouting target because it is a suitable tile.
     *
     * @param aiUnit The <code>AIUnit</code> to test.
     * @param tile The <code>Tile</code> to test.
     * @return A reason why the mission would be invalid, or null if none found.
     */
    private static String invalidTileReason(AIUnit aiUnit, Tile tile) {
        return (tile == null) ? "tile-null"
            : (!tile.hasLostCityRumour()) ? "tile-lacks-rumour"
            : null;
    }

    /**
     * Why would this mission be invalid with the given AI unit?
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @return A reason for mission invalidity, or null if none found.
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
            : (loc instanceof Settlement)
            ? invalidSettlementReason(aiUnit, (Settlement)loc)
            : (loc instanceof Tile)
            ? invalidTileReason(aiUnit, (Tile)loc)
            : Mission.TARGETINVALID;
    }


    // Implement Mission
    //   Inherit dispose, getTransportDestination, isOneTime

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBaseTransportPriority() {
        return NORMAL_TRANSPORT_PRIORITY;
    }

    /**
     * {@inheritDoc}
     */
    public Location getTarget() {
        return target;
    }

    /**
     * {@inheritDoc}
     */
    public void setTarget(Location target) {
        if (target == null
            || target instanceof Settlement || target instanceof Tile) {
            boolean retarget = this.target != null && this.target != target;
            this.target = target;
            if (retarget) retargetTransportable();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Location findTarget() {
        return findTarget(getAIUnit(), 20, true);
    }

    /**
     * {@inheritDoc}
     */
    public String invalidReason() {
        return invalidReason(getAIUnit(), getTarget());
    }

    /**
     * {@inheritDoc}
     */
    public Mission doMission(LogBuilder lb) {
        lb.add(tag);
        String reason = invalidReason();
        if (isTargetReason(reason)) {
            if (!retargetMission(reason, lb)) return null;
        } else if (reason != null) {
            lbBroken(lb, reason);
            return null;
        }

        // Go to the target.
        final AIUnit aiUnit = getAIUnit();
        final Unit unit = getUnit();
        Direction d;
        Unit.MoveType mt = travelToTarget(getTarget(),
            CostDeciders.avoidSettlementsAndBlockingUnits(), lb);
        switch (mt) {
        case MOVE_ILLEGAL:
        case MOVE_NO_MOVES: case MOVE_NO_REPAIR: case MOVE_NO_TILE:
            return this;

        case MOVE: // Arrived at a colony
            break;

        case ATTACK_UNIT:
            // Could be adjacent to the destination but it is
            // temporarily blocked by another unit.  Make a random
            // (directed if possible) move and try again.
            moveRandomly(tag, unit.getTile()
                .getDirection(getTarget().getTile()));
            lbDodge(lb, unit);
            return this;

        case ENTER_INDIAN_SETTLEMENT_WITH_SCOUT:
            d = unit.getTile().getDirection(getTarget().getTile());
            assert d != null;
            if (AIMessage.askScoutSpeakToChief(aiUnit, d)) {
                lbDone(lb, "speak-with-chief at ", getTarget());
            } else {
                lbFail(lb, " unexpected failure to speak at ", getTarget());
            }
            break;

        case EXPLORE_LOST_CITY_RUMOUR:
            d = unit.getTile().getDirection(getTarget().getTile());
            assert d != null;
            if (AIMessage.askMove(aiUnit, d)) {
                lbDone(lb, tag, "explore at ", getTarget());
            } else {
                lbFail(lb, tag, "unexpected failure at ", getTarget());
            }
            break;

        default:
            lbMove(lb, unit, mt);
            return this;
        }
        if (unit.isDisposed()) {
            lb.add(", died at target ", getTarget(), ".");
            return null;
        }

        // Retarget on failure or complete, but do not retarget from
        // one colony to another, just drop equipment and invalidate
        // the mission.
        Location completed = getTarget();
        Location newTarget = findTarget(aiUnit, 20, false);
        if (completed instanceof Colony
            && (newTarget == null || newTarget == completed)) {
            if (canScoutNatives(aiUnit)) {
                aiUnit.equipForRole(Specification.DEFAULT_ROLE_ID);
            }
            lbFail(lb, " arrived at ", completed, " but found no targets");
            return null;
        }
        if (newTarget == null) {
            lb.add(" at ", completed, " but found no targets");
            return null;
        }

        setTarget(newTarget);
        lb.add(" retargeting ", newTarget);
        return this;
    }


    // Serialization

    private static final String TARGET_TAG = "target";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        if (target != null) {
            xw.writeAttribute(TARGET_TAG, target.getId());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        target = xr.getLocationAttribute(getGame(), TARGET_TAG, false);
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "scoutingMission".
     */
    public static String getXMLElementTagName() {
        return "scoutingMission";
    }
}
