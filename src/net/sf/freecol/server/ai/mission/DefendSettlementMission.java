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

import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIUnit;


/**
 * Mission for defending a <code>Settlement</code>.
 */
public class DefendSettlementMission extends Mission {

    private static final Logger logger = Logger.getLogger(DefendSettlementMission.class.getName());

    /** The tag for this mission. */
    private String tag = "AI defender";

    /** Maximum number of turns to travel to the settlement. */
    private static int MAX_TURNS = 20;

    /** The settlement to be protected. */
    private Location target = null;


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
        super(aiMain, aiUnit);

        setTarget(settlement);
        logger.finest(tag + " started with " + target + ": " + this);
        uninitialized = false;
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
        uninitialized = getAIUnit() == null;
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
        GoalDecider gd = new GoalDecider() {
                private PathNode bestPath = null;
                private int bestValue = Integer.MIN_VALUE;
                
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
        return gd;
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
        final Tile startTile = unit.getPathStartTile();
        if (startTile == null) return null;

        return unit.search(startTile, getGoalDecider(aiUnit),
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
        return (path != null) ? extractTarget(aiUnit, path)
            : null;
    }


    // Fake Transportable interface

    /**
     * {@inheritDoc}
     */
    @Override
    public Location getTransportDestination() {
        return (getUnit().shouldTakeTransportTo(getTarget())) ? getTarget()
            : null;
    }

    /**
     * {@inheritDoc}
     */
    public int getTransportPriority() {
        return (getTransportDestination() == null) ? 0
            : NORMAL_TRANSPORT_PRIORITY + 5;
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
        if (target == null || target instanceof Settlement) {
            boolean retarget = this.target != null && this.target != target;
            this.target = target;
            if (retarget) retargetTransportable();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Location findTarget() {
        return findTarget(getAIUnit(), 4, true);
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
        return (unit.getGame().getCombatModel()
            .getDefencePower(null, unit) <= 0.0f) ? "unit-not-defender"
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
        String reason = invalidReason();
        if (isTargetReason(reason)) {
            if (!retargetMission(tag, reason)) return;
        } else if (reason != null) {
            logger.finest(tag + " broken(" + reason + "): " + this);
            return;
        }

        // Go to the target!
        if (travelToTarget(tag, getTarget(),
                CostDeciders.avoidSettlementsAndBlockingUnits())
            != Unit.MoveType.MOVE) return;

        // Check if the mission should change?
        // Change to supporting the settlement if the size is marginal.
        final AIMain aiMain = getAIMain();
        final AIUnit aiUnit = getAIUnit();
        final Unit unit = getUnit();
        Mission m = null;
        if (getTarget() instanceof Colony) {
            Colony colony = (Colony)getTarget();
            if (unit.getLocation() instanceof WorkLocation
                || (unit.isPerson() && colony.getUnitCount() <= 1)) {
                m = new WorkInsideColonyMission(aiMain, aiUnit,
                    aiMain.getAIColony(colony));
                aiUnit.setMission(m);
                m.doMission();
                return; // No log, setMission logs this mission going away.
            }
        }

        // Anything more to do?
        if (unit.getState() == UnitState.FORTIFIED
            || unit.getState() == UnitState.FORTIFYING) {
            return; // No log, these happen indefinitely.
        }

        // Check if the settlement is badly defended.  If so, try to fortify.
        Settlement settlement = (Settlement)getTarget();
        int defenderCount = 0, fortifiedCount = 0;
        List<Unit> units = settlement.getUnitList();
        units.addAll(settlement.getTile().getUnitList());
        for (Unit u : units) {
            AIUnit aiu = getAIMain().getAIUnit(u);
            if (invalidMissionReason(aiu) == null) {
                defenderCount++;
                if (u.getState() == UnitState.FORTIFIED) fortifiedCount++;
            }
        }
        if (defenderCount <= 2 || fortifiedCount <= 1) {
            String logMe;
            if (!unit.checkSetState(UnitState.FORTIFYING)) {
                logMe = " waiting to fortify at ";
            } else if (AIMessage.askChangeState(aiUnit, UnitState.FORTIFYING)
                && unit.getState() == UnitState.FORTIFYING) {
                logMe = " completed (fortifying) at ";
            } else {
                logMe = " fortify failed at ";
            }
            logger.finest(tag + logMe + settlement.getName() + ": " + this);
            return;
        }

        // The target is well enough defended.  See if the unit
        // should attack a nearby hostile unit.  Remember to prevent a
        // sole unit attacking because if it loses, the settlement
        // will collapse (and the combat model does not understand that).
        if (!unit.isOffensiveUnit()) return;
        final CombatModel cm = unit.getGame().getCombatModel();
        Unit bestTarget = null;
        float bestDifference = Float.MIN_VALUE;
        Direction bestDirection = null;
        for (Direction d : Direction.getRandomDirections("defendSettlements",
                                                         getAIRandom())) {
            Tile t = unit.getTile().getNeighbourOrNull(d);
            if (t == null) continue;
            Unit defender = t.getFirstUnit();
            if (defender != null
                && defender.getOwner().atWarWith(unit.getOwner())
                && unit.getMoveType(d).isAttack()) {
                Unit enemyUnit = t.getDefendingUnit(unit);
                float enemyAttack = cm.getOffencePower(enemyUnit, unit);
                float weAttack = cm.getOffencePower(unit, enemyUnit);
                float enemyDefend = cm.getDefencePower(unit, enemyUnit);
                float weDefend = cm.getDefencePower(enemyUnit, unit);
                float difference = weAttack / (weAttack + enemyDefend)
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
            logger.finest(tag + " attacking " + bestTarget
                + " from " + ((Settlement)getTarget()).getName()
                + ": " + this);
            AIMessage.askAttack(getAIUnit(), bestDirection);
        }
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
