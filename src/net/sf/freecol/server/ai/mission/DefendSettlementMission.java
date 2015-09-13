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

import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIUnit;


/**
 * Mission for defending a <code>Settlement</code>.
 */
public class DefendSettlementMission extends Mission {

    private static final Logger logger = Logger.getLogger(DefendSettlementMission.class.getName());

    /** The tag for this mission. */
    private final String tag = "AI defender";

    /** Maximum number of turns to travel to the settlement. */
    private static final int MAX_TURNS = 20;

    /** The settlement to be protected. */
    private Location target;


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission
     *        is created for.
     * @param settlement The <code>Settlement</code> to defend.
     */
    public DefendSettlementMission(AIMain aiMain, AIUnit aiUnit,
                                   Settlement settlement) {
        super(aiMain, aiUnit, settlement);
    }

    /**
     * Creates a new <code>DefendSettlementMission</code> and reads
     * the given element.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     * @param xr The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public DefendSettlementMission(AIMain aiMain, AIUnit aiUnit,
                                   FreeColXMLReader xr) throws XMLStreamException {
        super(aiMain, aiUnit);

        readFromXML(xr);
    }


    /**
     * Extract a valid target for this mission from a path.
     *
     * @param aiUnit The <code>AIUnit</code> to perform the mission.
     * @param path A <code>PathNode</code> to extract a target from.
     * @return A target for a <code>DefendSettlementMission</code>, or null
     *     if none found.
     */
    public static Location extractTarget(AIUnit aiUnit, PathNode path) {
        if (path == null) return null;
        final Location loc = path.getLastNode().getLocation();
        Settlement settlement = loc.getSettlement();
        return (invalidReason(aiUnit, settlement) == null) ? settlement
            : null;
    }

    /**
     * Evaluate allocating a unit to the defence of a settlement.
     *
     * @param aiUnit The <code>AIUnit</code> that is to defend.
     * @param path A <code>PathNode</code> to take to the settlement.
     * @return A value for such a mission.
     */
    public static int scorePath(AIUnit aiUnit, PathNode path) {
        final Location loc = extractTarget(aiUnit, path);
        return (loc instanceof Settlement)
            ? aiUnit.getAIOwner().adjustMission(aiUnit, path,
                DefendSettlementMission.class,
                (int)(1000 * ((Settlement)loc).getDefenceRatio()
                    / (path.getTotalTurns() + 1)))
            : Integer.MIN_VALUE;
    }

    /**
     * Gets a <code>GoalDecider</code> for finding the best colony
     * <code>Tile</code>, optionally falling back to the nearest colony.
     *
     * @param aiUnit The <code>AIUnit</code> that is searching.
     * @return A suitable <code>GoalDecider</code>.
     */
    private static GoalDecider getGoalDecider(final AIUnit aiUnit) {
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
     * Finds a path to the best nearby settlement to defend.
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
        final Location start = unit.getPathStartLocation();

        return unit.search(start, getGoalDecider(aiUnit),
                           CostDeciders.avoidSettlementsAndBlockingUnits(),
                           range, unit.getCarrier());
    }

    /**
     * Finds a path to the best nearby settlement to defend.
     *
     * @param aiUnit The <code>AIUnit</code> that is searching.
     * @param range An upper bound on the number of moves.
     * @param deferOK Enables deferral (not implemented in this mission).
     * @return A suitable target, or null if none found.
     */
    public static Location findTarget(AIUnit aiUnit, int range,
                                      boolean deferOK) {
        PathNode path = findTargetPath(aiUnit, range, deferOK);
        return (path != null) ? extractTarget(aiUnit, path) : null;
    }

    /**
     * Why would a DefendSettlementMission be invalid with the given unit?
     *
     * @param aiUnit The <code>AIUnit</code> to test.
     * @return A reason why the mission would be invalid with the unit,
     *     or null if none found.
     */
    private static String invalidMissionReason(AIUnit aiUnit) {
        String reason = invalidAIUnitReason(aiUnit);
        if (reason != null) return reason;
        final Unit unit = aiUnit.getUnit();
        final CombatModel cm = unit.getGame().getCombatModel();
        return (cm.getDefencePower(null, unit) <= 0.0f) ? "unit-not-defender"
            : null;
    }
    
    /**
     * Why is this mission invalid with a given settlement target?
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @param settlement The potential target <code>Settlement</code>.
     * @return A reason for mission invalidity, or null if none found.
     */
    private static String invalidSettlementReason(AIUnit aiUnit,
                                                  Settlement settlement) {
        return invalidTargetReason(settlement, aiUnit.getUnit().getOwner());
    }

    /**
     * Why would this mission be invalid with the given AI unit?
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @return A reason for mission invalidity, or null if none found.
     */
    public static String invalidReason(AIUnit aiUnit) {
        String reason = invalidMissionReason(aiUnit);
        return (reason != null)
            ? reason
            : (aiUnit.getUnit().getOwner().getNumberOfSettlements() <= 0)
            ? Mission.TARGETNOTFOUND
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
        String reason = invalidMissionReason(aiUnit);
        return (reason != null)
            ? reason
            : (loc instanceof Settlement)
            ? invalidSettlementReason(aiUnit, (Settlement)loc)
            : Mission.TARGETINVALID;
    }


    // Implement Mission
    //   Inherit dispose, getTransportDestination, isOneTime

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBaseTransportPriority() {
        return (getTransportDestination() == null) ? 0
            : NORMAL_TRANSPORT_PRIORITY + 5;
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
        if (target == null || target instanceof Settlement) {
            this.target = target;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location findTarget() {
        return findTarget(getAIUnit(), 4, true);
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
        String reason = invalidReason();
        if (isTargetReason(reason)) {
            return retargetMission(reason, lb);
        } else if (reason != null) {
            return lbFail(lb, false, reason);
        }

        // Go to the target!
        final Unit unit = getUnit();
        Unit.MoveType mt = travelToTarget(getTarget(),
            CostDeciders.avoidSettlementsAndBlockingUnits(), lb);
        switch (mt) {
        case MOVE: // Arrived
            break;

        case MOVE_HIGH_SEAS: case MOVE_NO_MOVES:
        case MOVE_NO_REPAIR: case MOVE_ILLEGAL:
            return lbWait(lb);

        case MOVE_NO_ACCESS_EMBARK: case MOVE_NO_TILE:
            return this;

        default:
            return lbMove(lb, mt);
        }

        // Check if the mission should change?
        // Change to supporting the settlement if the size is marginal.
        final AIMain aiMain = getAIMain();
        final AIUnit aiUnit = getAIUnit();
        Mission m = null;
        if (getTarget() instanceof Colony) {
            Colony colony = (Colony)getTarget();
            if (unit.isInColony()
                || (unit.isPerson() && colony.getUnitCount() < 1)) {
                m = getEuropeanAIPlayer().getWorkInsideColonyMission(aiUnit,
                    aiMain.getAIColony(colony));
                return lbDone(lb, m != null, "bolster ", colony);
            }
        }

        // Anything more to do?
        switch (unit.getState()) {
        case FORTIFIED:  return lbWait(lb, ", fortified");
        case FORTIFYING: return lbWait(lb, ", fortifying");
        default: break;
        }

        // Check if the settlement is badly defended.  If so, try to fortify.
        Settlement settlement = (Settlement)getTarget();
        int defenderCount = 0, fortifyCount = 0;
        List<Unit> units = settlement.getUnitList();
        units.addAll(settlement.getTile().getUnitList());
        for (Unit u : units) {
            AIUnit aiu = getAIMain().getAIUnit(u);
            if (invalidMissionReason(aiu) == null) {
                defenderCount++;
                switch (u.getState()) {
                case FORTIFIED: case FORTIFYING: fortifyCount++; break;
                default: break;
                }
            }
        }
        if (defenderCount <= 2 || fortifyCount <= 1) {
            return (!unit.checkSetState(UnitState.FORTIFYING))
                ? lbWait(lb, ", waiting to fortify at ", settlement)
                : (AIMessage.askChangeState(aiUnit, UnitState.FORTIFYING)
                    && unit.getState() == UnitState.FORTIFYING)
                ? lbWait(lb, ", now fortifying at ", settlement)
                : lbFail(lb, false, ", fortify failed at ", settlement);
        }

        // The target is well enough defended.  See if the unit
        // should attack a nearby hostile unit.  Remember to prevent a
        // sole unit attacking because if it loses, the settlement
        // will collapse (and the combat model does not handle that).
        if (!unit.isOffensiveUnit()) {
            return lbFail(lb, false, "not-offensive-unit");
        }

        final CombatModel cm = unit.getGame().getCombatModel();
        Unit bestTarget = null;
        double bestDifference = Double.MIN_VALUE;
        Direction bestDirection = null;
        for (Direction d : Direction.getRandomDirections("defendSettlements",
                logger, getAIRandom())) {
            Tile t = unit.getTile().getNeighbourOrNull(d);
            if (t == null) continue;
            Unit defender = t.getFirstUnit();
            if (defender != null
                && defender.getOwner().atWarWith(unit.getOwner())
                && unit.getMoveType(d).isAttack()) {
                Unit enemyUnit = t.getDefendingUnit(unit);
                double enemyAttack = cm.getOffencePower(enemyUnit, unit);
                double weAttack = cm.getOffencePower(unit, enemyUnit);
                double enemyDefend = cm.getDefencePower(unit, enemyUnit);
                double weDefend = cm.getDefencePower(enemyUnit, unit);
                double difference = weAttack / (weAttack + enemyDefend)
                    - enemyAttack / (enemyAttack + weDefend);
                if (difference > bestDifference) {
                    if (difference > 0 || weAttack > enemyDefend) {
                        bestDifference = difference;
                        bestTarget = enemyUnit;
                        bestDirection = d;
                    }
                }
            }
        }

        // Attack if a target is available.
        if (bestTarget != null) {
            AIMessage.askAttack(getAIUnit(), bestDirection);
            return lbAttack(lb, bestTarget);
        }

        return lbWait(lb, ", alert at ", getTarget());
    }
    

    // Serialization

    private static final String SETTLEMENT_TAG = "settlement";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        if (target != null) {
            xw.writeAttribute(SETTLEMENT_TAG, target.getId());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        target = xr.getAttribute(getGame(), SETTLEMENT_TAG,
                                 Settlement.class, (Settlement)null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "defendSettlementMission".
     */
    public static String getXMLElementTagName() {
        return "defendSettlementMission";
    }
}
