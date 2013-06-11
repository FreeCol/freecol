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

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIUnit;


/**
 * A mission for a Privateer unit.
 */
public class PrivateerMission extends Mission {

    private static final Logger logger = Logger.getLogger(PrivateerMission.class.getName());

    /** The tag for this mission. */
    private static String tag = "AI privateer";

    /**
     * The target for this mission.  Either a port location to drop off
     * plunder, or a unit to attack.
     */
    private Location target = null;


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     */
    public PrivateerMission(AIMain aiMain, AIUnit aiUnit) {
        this(aiMain, aiUnit, findTarget(aiUnit, 8, true));
    }

    /**
     * Creates a mission for the given <code>AIUnit</code>.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     * @param target The target <code>Location</code> for this mission.
     */
    public PrivateerMission(AIMain aiMain, AIUnit aiUnit, Location target) {
        super(aiMain, aiUnit);

        setTarget(target);
        Unit unit = aiUnit.getUnit();
        logger.finest(tag + " begins at " + unit.getLocation() + ": " + this);
        uninitialized = false;
    }

    /**
     * Creates a new <code>UnitWanderHostileMission</code> and reads
     * the given element.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     * @param xr The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public PrivateerMission(AIMain aiMain, AIUnit aiUnit,
                            FreeColXMLReader xr) throws XMLStreamException {
        super(aiMain, aiUnit);

        readFromXML(xr);
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
        final Player owner = unit.getOwner();
        final Location loc = path.getLastNode().getLocation();
        Settlement settlement = loc.getSettlement();
        Tile tile = loc.getTile();
        Unit other = (tile == null) ? null : tile.getDefendingUnit(unit);
        return (loc instanceof Europe) ? loc
            : (other != null
                && invalidUnitReason(aiUnit, other) == null) ? other
            : (settlement != null
                && invalidTargetReason(settlement, owner) == null) ? settlement
            : null;
    }

    /**
     * Score a potential attack on a unit.
     *
     * @param aiUnit The <code>AIUnit</code> that may attack.
     * @param defender The <code>Unit</code> to attack.
     * @return A score for the attack.
     */
    private static int scoreUnit(AIUnit aiUnit, Unit defender) {
        Unit attacker = aiUnit.getUnit();
        int value = 1000;
        // Pirates want cargo
        value += defender.getVisibleGoodsCount() * 200;
        // But they are wary of danger
        if (defender.isOffensiveUnit()) {
            value -= attacker.getGame().getCombatModel()
                .getDefencePower(attacker, defender) * 100;
        }
        return value;
    }

    /**
     * Evaluate a potential mission for a given unit and path.
     *
     * @param aiUnit The <code>AIUnit</code> to do the mission.
     * @param path A <code>PathNode</code> to take to the target.
     * @return A score for the proposed mission.
     */
    public static int scorePath(AIUnit aiUnit, PathNode path) {
        Location loc = extractTarget(aiUnit, path);
        if (loc instanceof Europe || loc instanceof Colony) {
            return 1000 / (path.getTotalTurns() + 1);
        } else if (loc instanceof Unit) {
            return scoreUnit(aiUnit, (Unit)loc) / (path.getTotalTurns() + 1);
        } else {
            return Integer.MIN_VALUE;
        }
    }

    /**
     * Gets a <code>GoalDecider</code> for this mission.
     *
     * @param aiUnit The <code>AIUnit</code> that is searching.
     * @param deferOK Enable colony fallback (not implemented).
     * @return A suitable <code>GoalDecider</code>.
     */
    private static GoalDecider getGoalDecider(final AIUnit aiUnit,
                                              boolean deferOK) {
        return new GoalDecider() {
            private PathNode bestPath = null;
            private int bestValue = 0;

            public PathNode getGoal() { return bestPath; }
            public boolean hasSubGoals() { return true; }
            public boolean check(Unit u, PathNode path) {
                int value = scorePath(aiUnit, path);
                if (bestValue < value) {
                    bestValue = value;
                    bestPath = path;
                    return true;
                }
                return false;
            }
        };
    }

    /**
     * Finds a suitable privateering target for the supplied unit.
     *
     * @param aiUnit The <code>AIUnit</code> to find a path for.
     * @param range The maximum number of turns to seek for a target.
     * @param deferOK Not implemented in this mission.
     * @return A path to the new target.
     */
    public static PathNode findTargetPath(AIUnit aiUnit, int range, 
                                          boolean deferOK) {
        if (invalidAIUnitReason(aiUnit) != null) return null;
        final Unit unit = aiUnit.getUnit();
        final Tile startTile = unit.getPathStartTile();
        if (startTile == null) return null;

        // Can the privateer legally reach a valid target from where
        // it currently is?
        return unit.search(startTile, getGoalDecider(aiUnit, deferOK),
                           CostDeciders.avoidIllegal(), range, null);
    }

    /**
     * Finds a suitable privateering target for the supplied unit.
     *
     * @param aiUnit The <code>AIUnit</code> to find a path for.
     * @param range The maximum number of turns to seek for a target.
     * @param deferOK Enables deferring to a fallback colony.
     * @return A <code>PathNode</code> to the target, or null if none found.
     */
    public static Location findTarget(AIUnit aiUnit, int range,
                                      boolean deferOK) {
        PathNode path = findTargetPath(aiUnit, range, deferOK);
        return (path != null) ? extractTarget(aiUnit, path)
            : null;
    }        


    // Fake Transportable interface

    /**
     * {@inheritDoc}
     */
    @Override
    public Location getTransportDestination() {
        return null;
    }


    // Mission interface

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
            || target instanceof Colony || target instanceof Europe
            || target instanceof Unit) {
            boolean retarget = this.target != null && this.target != target;
            this.target = target;
            if (retarget) retargetTransportable();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Location findTarget() {
        return findTarget(getAIUnit(), 8, true);
    }
    
    /**
     * Why would a PrivateeringMission be invalid with the given unit.
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @return A reason why the mission would be invalid with the unit,
     *     or null if none found.
     */
    private static String invalidMissionReason(AIUnit aiUnit) {
        String reason = invalidAIUnitReason(aiUnit);
        if (reason != null) return reason;
        final Unit unit = aiUnit.getUnit();
        return (!unit.isCarrier()) ? "unit-not-a-carrier"
            : (!unit.isOffensiveUnit()) ? Mission.UNITNOTOFFENSIVE
            : (!unit.hasAbility(Ability.PIRACY)) ? "unit-not-a-pirate"
            : null;
    }

    /**
     * Is this a valid target because it is one of our colonies.
     *
     * @param aiUnit The <code>AIUnit</code> to test.
     * @param settlement The <code>Settlement</code> to test.
     * @return A reason why the mission would be invalid, or null if
     *     none found.
     */
    private static String invalidSettlementReason(AIUnit aiUnit,
                                                  Settlement settlement) {
        return (settlement instanceof Colony)
            ? invalidTargetReason(settlement, aiUnit.getUnit().getOwner())
            : Mission.TARGETINVALID;
    }

    /**
     * Is this a valid target because it is a hostile unit.
     *
     * @param aiUnit The <code>AIUnit</code> to test.
     * @param unit The <code>Unit</code> to test.
     * @return A reason why the mission would be invalid, or null if
     *     none found.
     */
    private static String invalidUnitReason(AIUnit aiUnit, Unit unit) {
        Player player = aiUnit.getUnit().getOwner();
        Player other = unit.getOwner();
        return (unit == null)
            ? Mission.TARGETINVALID
            : (!unit.isNaval())
            ? "privateer-ignores-land-unit"
            : (player == other)
            ? Mission.TARGETOWNERSHIP
            : (player.getStance(other) == Stance.ALLIANCE)
            ? "privateer-avoids-ally"
            : (scoreUnit(aiUnit, unit) <= 0)
            ? "privateer-avoids-trouble"
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
     * Why would this mission be invalid with the given AI unit?
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @param loc The <code>Location</code> to check.
     * @return A reason for mission invalidity, or null if none found.
     */
    public static String invalidReason(AIUnit aiUnit, Location loc) {
        String reason = invalidMissionReason(aiUnit);
        return (reason != null)
            ? reason
            : (aiUnit.getUnit().isInEurope())
            ? null
            : (loc == null)
            ? null
            : (loc instanceof Europe)
            ? invalidTargetReason(loc, aiUnit.getUnit().getOwner())
            : (loc instanceof Settlement)
            ? invalidSettlementReason(aiUnit, (Settlement)loc)
            : (loc instanceof Unit)
            ? invalidUnitReason(aiUnit, (Unit)loc)
            : Mission.TARGETINVALID;
    }

    /**
     * {@inheritDoc}
     */
    public String invalidReason() {
        return invalidReason(getAIUnit(), getTarget());
    }

    // Not a one-time mission, omit isOneTime().

    /**
     * {@inheritDoc}
     */
    public void doMission() {
        final AIMain aiMain = getAIMain();
        final AIUnit aiUnit = getAIUnit();
        if (aiUnit.hasCargo()) { // Deliver the goods
            aiUnit.setMission(new TransportMission(aiMain, aiUnit));
            aiUnit.getMission().doMission();
            return;
        }

        String reason = invalidReason();
        if (isTargetReason(reason)) {
            if (!retargetMission(tag, reason)) return;
        } else if (reason != null) {
            logger.finest(tag + " broken(" + reason + "): " + this);
            return;
        }
        final Unit unit = getUnit();
        if (unit.isAtSea()) return;

        if (unit.isInEurope()) {
            Settlement settlement = getBestSettlement(unit.getOwner());
            Tile tile = (settlement != null) ? settlement.getTile()
                : unit.getFullEntryLocation();
            unit.setDestination(tile);
            aiUnit.moveToAmerica();
            return;
        }

        Location newTarget = findTarget(aiUnit, 1, true);
        if (newTarget == null) {
            moveRandomlyTurn(tag);
            return;
        }

        setTarget(newTarget);
        Unit.MoveType mt = travelToTarget(tag, getTarget(), null);
        switch (mt) {
        case MOVE: case MOVE_HIGH_SEAS: case MOVE_NO_MOVES:
        case MOVE_NO_REPAIR:
            return;
        case MOVE_ILLEGAL: // Can happen when another unit blocks a river
            logger.finest(tag + " hit unexpected blockage: " + this);
            moveRandomly(tag, null);
            unit.setMovesLeft(0);
            return;
        case ATTACK_UNIT:
            Direction direction = unit.getTile()
                .getDirection(getTarget().getTile());
            if (direction != null) {
                logger.finest(tag + " completed hunt for target " + getTarget()
                    + ", attacking: " + this);
                AIMessage.askAttack(aiUnit, direction);
            } else { // Found something else in the way!
                Location blocker = resolveBlockage(aiUnit, getTarget());
                if (blocker instanceof Unit
                    && scoreUnit(aiUnit, (Unit)blocker) > 0) {
                    logger.finest(tag + " bumped into " + blocker
                        + ", attacking: " + this);
                    AIMessage.askAttack(aiUnit,
                        unit.getTile().getDirection(blocker.getTile()));
                } else { // Might be dangerous, try to confuse them:-)
                    logger.finest(tag + " bumped into " + blocker
                        + ", avoiding: " + this);
                    moveRandomlyTurn(tag);
                }
            }
            break;
        default:
            logger.warning(tag + " unexpected hunt move " + mt + ": " + this);
            break;
        }
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

        target = xr.findLocationAttribute(getGame(), TARGET_TAG);
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "privateerMission"
     */
    public static String getXMLElementTagName() {
        return "privateerMission";
    }
}
