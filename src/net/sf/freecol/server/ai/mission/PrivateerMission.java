/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIUnit;


/**
 * A mission for a Privateer unit.
 */
public class PrivateerMission extends Mission {

    private static final Logger logger = Logger.getLogger(PrivateerMission.class.getName());

    /** The tag for this mission. */
    private static final String tag = "AI privateer";

    /**
     * The target for this mission.
     * - A port location to drop off plunder
     * - A unit to attack
     * - An unexplored tile
     */
    private Location target;


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     * @param target The target <code>Location</code> for this mission.
     */
    public PrivateerMission(AIMain aiMain, AIUnit aiUnit, Location target) {
        super(aiMain, aiUnit, target);
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
    }


    /**
     * Hack to help REF planning.
     *
     * This should go away.  AI units should not exploit seeing the whole map.
     *
     * @return The distance to the target, or a large value on failure.
     */
    public int getDistanceToTarget() {
        return (target == null || target.getTile() == null
            || !getUnit().hasTile()) ? 1000000
            : getUnit().getTile().getDistanceTo(target.getTile());
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
        final Unit attacker = aiUnit.getUnit();
        int value = 100;
        // Pirates want cargo
        value += defender.getVisibleGoodsCount() * 200;
        // But they are wary of danger
        if (defender.isOffensiveUnit()) {
            CombatModel.CombatOdds co = attacker.getGame().getCombatModel()
                .calculateCombatOdds(attacker, defender);
            if (co != null) value += (co.win - 0.5) * 200;
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
        } else if (loc instanceof Tile) {
            return 50 / (path.getTotalTurns() + 1);
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
                private int bestValue = Integer.MIN_VALUE;

                @Override
                public PathNode getGoal() { return bestPath; }
                @Override
                public boolean hasSubGoals() { return true; }
                @Override
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
        final Location start = unit.getPathStartLocation();

        // Can the privateer legally reach a valid target from where
        // it currently is?
        return unit.search(start, getGoalDecider(aiUnit, deferOK),
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
            : (!unit.isEmpty()
                || !unit.getCompactGoods().isEmpty()) ? "unit-has-cargo"
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
        final Player player = aiUnit.getUnit().getOwner();
        final boolean pirate = aiUnit.getUnit().hasAbility(Ability.PIRACY);
        String reason;
        return (unit == null)
            ? Mission.TARGETINVALID
            : (!unit.isNaval())
            ? "privateer-ignores-land-unit"
            : (player.owns(unit))
            ? Mission.TARGETOWNERSHIP
            : ((reason = invalidAttackReason(aiUnit, unit.getOwner())) != null)
            ? reason
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
        final Player owner = aiUnit.getUnit().getOwner();
        String reason = invalidMissionReason(aiUnit);
        return (reason != null)
            ? reason
            : (aiUnit.getUnit().isInEurope())
            ? null
            : (loc == null)
            ? null
            : (loc instanceof Europe)
            ? invalidTargetReason(loc, owner)
            : (loc instanceof Settlement)
            ? invalidSettlementReason(aiUnit, (Settlement)loc)
            : (loc instanceof Unit)
            ? invalidUnitReason(aiUnit, (Unit)loc)
            : (loc instanceof Tile)
            ? ((((Tile)loc).isExploredBy(owner)) ? "tile-is-explored"
                : null)
            : Mission.TARGETINVALID;
    }


    // Implement Mission
    //   Inherit dispose, getBaseTransportPriority, isOneTime

    /**
     * {@inheritDoc}
     */
    @Override
    public Location getTransportDestination() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location getTarget() {
        return target;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTarget(Location target) {
        if (target == null
            || target instanceof Colony || target instanceof Europe
            || target instanceof Unit) {
            this.target = target;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location findTarget() {
        return findTarget(getAIUnit(), 8, true);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String invalidReason() {
        return invalidReason(getAIUnit(), getTarget());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mission doMission(LogBuilder lb) {
        lb.add(tag);
        final AIUnit aiUnit = getAIUnit();
        if (aiUnit.hasCargo()) { // Deliver the goods
            Mission m = getEuropeanAIPlayer().getTransportMission(aiUnit);
            return lbDone(lb, m != null, " transporting");
        }

        String reason = invalidReason();
        if (isTargetReason(reason)) {
            return retargetMission(reason, lb);
        } else if (reason != null) {
            return lbFail(lb, false, reason);
        }

        final Unit unit = getUnit();
        if (unit.isInEurope()) {
            Settlement settlement = getBestSettlement(unit.getOwner());
            Tile tile = (settlement != null) ? settlement.getTile()
                : unit.getFullEntryLocation();
            unit.setDestination(tile);
            aiUnit.moveToAmerica();
        }
        if (unit.isAtSea()) return lbAt(lb);

        Location newTarget = findTarget(aiUnit, 1, true);
        if (newTarget == null) {
            moveRandomlyTurn(tag);
            return lbAt(lb);
        }

        setTarget(newTarget);
        Unit.MoveType mt = travelToTarget(newTarget, null, lb);
        switch (mt) {
        case MOVE: // Arrived at intermediate safe location
            break;

        case MOVE_HIGH_SEAS: case MOVE_NO_MOVES:
        case MOVE_NO_REPAIR: case MOVE_ILLEGAL:
            return lbWait(lb);
            
        case MOVE_NO_TILE: // Can happen when another unit blocks a river
            moveRandomly(tag, null);
            return lbDodge(lb);

        case ATTACK_UNIT: // Arrived
            Direction direction = unit.getTile()
                .getDirection(getTarget().getTile());
            if (direction != null) {
                AIMessage.askAttack(aiUnit, direction);
                lbAttack(lb, getTarget());
            } else { // Found something else in the way!
                Location blocker = resolveBlockage(aiUnit, getTarget());
                if (blocker instanceof Unit
                    && scoreUnit(aiUnit, (Unit)blocker) > 0) {
                    AIMessage.askAttack(aiUnit,
                        unit.getTile().getDirection(blocker.getTile()));
                    lbAttack(lb, blocker);
                } else { // Might be dangerous, try to confuse them:-)
                    moveRandomlyTurn(tag);
                    lb.add(" avoiding ", blocker, ".");
                }
            }
            return this;

        case MOVE_NO_ACCESS_EMBARK: default:
            return lbMove(lb, mt);
        }

        return lbAt(lb);
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
    @Override
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
