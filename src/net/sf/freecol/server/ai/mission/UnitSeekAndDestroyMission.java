/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.ai.MissionAIPlayer;


/**
 * Mission for attacking a specific target, be it a Unit or a Settlement.
 */
public final class UnitSeekAndDestroyMission extends Mission {

    private static final Logger logger = Logger.getLogger(UnitSeekAndDestroyMission.class.getName());

    public static final String TAG = "unitSeekAndDestroyMission";

    /** The tag for this mission. */
    private static final String tag = "AI seek+destroyer";

    /**
     * The object we are trying to destroy. This can be a
     * either {@code Settlement} or a {@code Unit}.
     */
    private Location target, transportTarget;


    /**
     * Creates a mission for the given {@code AIUnit}.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The {@code AIUnit} this mission is created for.
     * @param target The object we are trying to destroy.  This can be
     *     either a {@code Settlement} or a {@code Unit}.
     */
    public UnitSeekAndDestroyMission(AIMain aiMain, AIUnit aiUnit,
                                     Location target) {
        super(aiMain, aiUnit);

        setTarget(target);
    }

    /**
     * Creates a new {@code UnitSeekAndDestroyMission} from a reader.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The {@code AIUnit} this mission is created for.
     * @param xr The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public UnitSeekAndDestroyMission(AIMain aiMain, AIUnit aiUnit,
                                     FreeColXMLReader xr) throws XMLStreamException {
        super(aiMain, aiUnit);

        readFromXML(xr);
    }


    /**
     * Extract a valid target for this mission from a path.
     *
     * @param aiUnit The {@code AIUnit} to perform the mission.
     * @param path A {@code PathNode} to extract a target from.
     * @return A target for this mission, or null if none found.
     */
    public static Location extractTarget(AIUnit aiUnit, PathNode path) {
        final Location loc = (path == null) ? null
            : path.getLastNode().getLocation();
        Tile t;
        Unit u;
        return (loc == null || aiUnit == null || aiUnit.getUnit() == null) 
            ? null
            : (invalidSettlementReason(aiUnit, loc.getSettlement()) == null)
            ? loc.getSettlement()
            : ((t = loc.getTile()) != null
                && invalidTargetReason(aiUnit,
                    u = t.getDefendingUnit(aiUnit.getUnit())) == null)
            ? u
            : null;
    }

    /**
     * Scores a potential attack on a settlement.
     *
     * Do not cheat and look inside the settlement.
     * Just use visible facts about it.
     *
     * FIXME: if we are the REF and there is a significant Tory
     * population inside, assume traitors have briefed us.
     *
     * @param aiUnit The {@code AIUnit} to do the mission.
     * @param path The {@code PathNode} to take to the settlement.
     * @param settlement The {@code Settlement} to attack.
     * @return A score of the desirability of the mission.
     */
    private static int scoreSettlementPath(AIUnit aiUnit, PathNode path,
                                           Settlement settlement) {
        if (invalidSettlementReason(aiUnit, settlement) != null) {
            return Integer.MIN_VALUE;
        }
        final Unit unit = aiUnit.getUnit();
        final CombatModel combatModel = unit.getGame().getCombatModel();

        int value = 1020;
        value -= path.getTotalTurns() * 100;

        final double off = combatModel.getOffencePower(unit, settlement);
        value += (int)Math.round(off * 50);

        value = settlement.calculateSettlementValue(value, unit);

        return ((MissionAIPlayer)aiUnit.getAIOwner()).adjustMission(
            aiUnit, path, UnitSeekAndDestroyMission.class, value);
    }

    /**
     * Scores a potential attack on a unit.
     *
     * @param aiUnit The {@code AIUnit} to do the mission.
     * @param path The {@code PathNode} to take to the settlement.
     * @param defender The {@code Unit} to attack.
     * @return A score of the desirability of the mission.
     */
    private static int scoreUnitPath(AIUnit aiUnit, PathNode path,
                                     Unit defender) {
        if (invalidMissionReason(aiUnit, defender) != null) {
            return Integer.MIN_VALUE;
        }
        final Unit unit = aiUnit.getUnit();
        final Tile tile = path.getLastNode().getTile();
        final int turns = path.getTotalTurns();
        final CombatModel combatModel = unit.getGame().getCombatModel();
        final double off = combatModel.getOffencePower(unit, defender);
        final double def = combatModel.getDefencePower(unit, defender);
        if (tile == null || off <= 0) return Integer.MIN_VALUE;

        int value = 1020 - turns * 100;
        value += 100 * (off - def);

        // Add a big bonus for treasure trains on the tile.
        // Do not cheat and look at the value.
        value += 1000 * count(tile.getUnits(),
            u -> u.canCarryTreasure() && u.getTreasureAmount() > 0);

        if (defender.isNaval()) {
            if (tile.isLand()) value += 500; // Easy win
        } else {
            if (defender.hasAbility(Ability.EXPERT_SOLDIER)
                && !defender.isArmed()) value += 100;
        }
        return ((MissionAIPlayer)aiUnit.getAIOwner()).adjustMission(
            aiUnit, path, UnitSeekAndDestroyMission.class, value);
    }

    /**
     * Evaluate a potential seek and destroy mission for a given unit
     * to a given tile.
     *
     * @param aiUnit The {@code AIUnit} to do the mission.
     * @param path A {@code PathNode} to take to the target.
     * @return A score for the proposed mission.
     */
    public static int scorePath(AIUnit aiUnit, PathNode path) {
        Location loc = extractTarget(aiUnit, path);
        return (loc instanceof Settlement)
            ? scoreSettlementPath(aiUnit, path, (Settlement)loc)
            : (loc instanceof Unit)
            ? scoreUnitPath(aiUnit, path, (Unit)loc)
            : Integer.MIN_VALUE;
    }

    /**
     * Gets a {@code GoalDecider} for finding the best colony
     * {@code Tile}, optionally falling back to the nearest colony.
     *
     * @param aiUnit The {@code AIUnit} that is searching.
     * @param deferOK Not used in this mission.
     * @return A suitable {@code GoalDecider}.
     */
    private static GoalDecider getGoalDecider(final AIUnit aiUnit,
        @SuppressWarnings("unused") boolean deferOK) {
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
     * Finds a suitable seek-and-destroy target path for an AI unit.
     *
     * @param aiUnit The {@code AIUnit} to find a target for.
     * @param range An upper bound on the number of moves.
     * @param deferOK Not implemented in this mission.
     * @return A path to the target, or null if none found.
     */
    private static PathNode findTargetPath(AIUnit aiUnit, int range,
        @SuppressWarnings("unused") boolean deferOK) {
        if (invalidAIUnitReason(aiUnit) != null) return null;
        final Unit unit = aiUnit.getUnit();
        final Location start = unit.getPathStartLocation();

        // Can the unit legally reach a valid target from where it
        // currently is?
        return unit.search(start, getGoalDecider(aiUnit, false),
            CostDeciders.avoidIllegal(), range, unit.getCarrier());
    }

    /**
     * Finds a suitable seek-and-destroy target for an AI unit.
     *
     * @param aiUnit The {@code AIUnit} to find a target for.
     * @param range An upper bound on the number of moves.
     * @param deferOK Not implemented in this mission.
     * @return A suitable target, or null if none found.
     */
    public static Location findMissionTarget(AIUnit aiUnit, int range,
                                             boolean deferOK) {
        PathNode path = findTargetPath(aiUnit, range, deferOK);
        return (path != null) ? extractTarget(aiUnit, path)
            : null;
    }

    /**
     * Why would a UnitSeekAndDestroyMission be invalid with the given unit.
     *
     * @param aiUnit The {@code AIUnit} to test.
     * @return A reason why the mission would be invalid, or null if
     *     none found.
     */
    private static String invalidUnitReason(AIUnit aiUnit) {
        String reason = invalidAIUnitReason(aiUnit);
        return (reason != null)
            ? reason
            : (!aiUnit.getUnit().isOffensiveUnit())
            ? Mission.UNITNOTOFFENSIVE
            : (aiUnit.getUnit().hasAbility(Ability.SPEAK_WITH_CHIEF))
            ? "scouts-should-not-seek-and-destroy"
            : null;
    }

    /**
     * Why would a UnitSeekAndDestroyMission be invalid with the given unit
     * and settlement.
     *
     * @param aiUnit The {@code AIUnit} to seek-and-destroy with.
     * @param settlement The {@code Settlement} to test.
     * @return A reason why the mission would be invalid, or null if
     *     none found.
     */
    private static String invalidSettlementReason(AIUnit aiUnit,
                                                  Settlement settlement) {
        String reason = invalidTargetReason(settlement);
        if (reason != null) return reason;
        final Unit unit = aiUnit.getUnit();
        return (unit.isNaval())
            ? "unit-is-naval"
            : (settlement.getOwner() == unit.getOwner())
            ? Mission.TARGETOWNERSHIP
            : invalidAttackReason(aiUnit, settlement.getOwner());
    }

    /**
     * Why would a UnitSeekAndDestroyMission be invalid with the given unit
     * and target unit.
     *
     * @param aiUnit The {@code AIUnit} to seek-and-destroy with.
     * @param unit The target {@code Unit} to test.
     * @return A reason why the mission would be invalid, or null if
     *     none found.
     */
    private static String invalidTargetReason(AIUnit aiUnit, Unit unit) {
        String reason = invalidTargetReason(unit);
        if (reason != null) return reason;
        final Tile tile = unit.getTile();
        return (tile == null)
            ? "target-not-on-map"
            : (aiUnit.getUnit().getOwner() == unit.getOwner())
            ? Mission.TARGETOWNERSHIP
            : (tile.hasSettlement())
            ? "target-in-settlement"
            : (!aiUnit.getUnit().isTileAccessible(tile))
            ? "target-incompatible"
            : invalidAttackReason(aiUnit, unit.getOwner());
    }

    /**
     * Why would this mission be invalid with the given AI unit?
     *
     * @param aiUnit The {@code AIUnit} to check.
     * @return A reason for mission invalidity, or null if none found.
     */
    public static String invalidMissionReason(AIUnit aiUnit) {
        return invalidUnitReason(aiUnit);
    }

    /**
     * Why would this mission be invalid with the given AI unit and location?
     *
     * @param aiUnit The {@code AIUnit} to check.
     * @param loc The {@code Location} to check.
     * @return A reason for invalidity, or null if none found.
     */
    public static String invalidMissionReason(AIUnit aiUnit, Location loc) {
        String reason = invalidMissionReason(aiUnit);
        return (reason != null)
            ? reason                
            : (loc instanceof Settlement)
            ? invalidSettlementReason(aiUnit, (Settlement)loc)
            : (loc instanceof Unit)
            ? invalidTargetReason(aiUnit, (Unit)loc)
            : Mission.TARGETINVALID;
    }

    
    // Implement Mission
    //   Inherit dispose, isOneTime

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBaseTransportPriority() {
        return NORMAL_TRANSPORT_PRIORITY - 5;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location getTransportDestination() {
        if (!isValid()) return null;
        Location loc = (transportTarget != null) ? transportTarget : target;
        return (getUnit().shouldTakeTransportTo(loc)) ? loc : null;
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
            || target instanceof Unit || target instanceof Settlement) {
            this.target = target;
            Unit unit = getUnit();
            transportTarget = null;
            if (unit.shouldTakeTransportTo(target)
                && (target instanceof Settlement)) {
                Settlement settlement = (Settlement)target;
                if (settlement.isConnectedPort()) {
                    transportTarget = settlement.getTile()
                        .getBestDisembarkTile(unit.getOwner());
                    logger.finest(tag + " chose dropoff " + transportTarget
                        + " for attack on "
                        + ((settlement.canBombardEnemyShip()) ? "hazardous"
                            : "normal")
                        + " settlement " + settlement.getName()
                        + ": " + this);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location findTarget() {
        return findMissionTarget(getAIUnit(), 4, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String invalidReason() {
        return invalidMissionReason(getAIUnit(), getTarget());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mission doMission(LogBuilder lb) {
        lb.add(tag);
        final AIUnit aiUnit = getAIUnit();
        String reason = invalidReason();
        if (isTargetReason(reason)) {
            Colony colony;
            Mission m;
            if (Mission.TARGETOWNERSHIP.equals(reason)
                && getTarget() instanceof Colony
                && (colony = (Colony)getTarget()) != null
                && getPlayer().owns(colony)
                && (m = getAIPlayer().getDefendSettlementMission(aiUnit,
                        colony)) != null) {
                return lbDone(lb, true, " capturing colony ", colony.getName());
            }            
            return retargetMission(reason, lb);
        } else if (reason != null) {
            return lbFail(lb, false, reason);
        }

        // Is there a target-of-opportunity?
        final Unit unit = getUnit();
        Location nearbyTarget = (unit.isOnCarrier()) ? null
            : findMissionTarget(aiUnit, 1, false);
        if (nearbyTarget != null) {
            if (getTarget() == null) {
                setTarget(nearbyTarget);
                return lbRetarget(lb);
            }
            if (nearbyTarget == getTarget()) {
                nearbyTarget = null;
            } else {
                Tile now = unit.getTile();
                Tile nearbyTile = nearbyTarget.getTile();
                Tile targetTile = getTarget().getTile();
                if (now != null && nearbyTile != null && targetTile != null
                    && (now.getDistanceTo(nearbyTile)
                        >= now.getDistanceTo(targetTile))) {
                    nearbyTarget = null;
                } else {
                    lb.add(", found target of opportunity ", nearbyTarget);
                }
            }
        }

        // Go to the target.
        Location currentTarget = (nearbyTarget != null) ? nearbyTarget
            : getTarget();
        // Note avoiding other targets by choice of cost decider.
        Unit.MoveType mt = travelToTarget(currentTarget,
            CostDeciders.avoidSettlementsAndBlockingUnits(), lb);
        switch (mt) {
        case MOVE_HIGH_SEAS: case MOVE_NO_MOVES: case MOVE_ILLEGAL:
            return lbWait(lb);

        case MOVE_NO_REPAIR:
            return lbFail(lb, false, AIUNITDIED);

        case MOVE_NO_ACCESS_EMBARK: case MOVE_NO_TILE:
            return this;

        case ATTACK_UNIT: case ATTACK_SETTLEMENT:
            Tile unitTile = unit.getTile();
            Settlement settlement = unitTile.getSettlement();
            if (settlement != null && settlement.getUnitCount() < 2) {
                // Do not risk attacking out of a settlement that
                // might collapse.  Defend instead.
                Mission m = getAIPlayer()
                    .getDefendSettlementMission(aiUnit, settlement);
                return lbDone(lb, m != null, " desperate defence of ",
                              settlement);
            }
            Direction d = unitTile.getDirection(currentTarget.getTile());
            if (d == null) {
                logger.warning("SDDM bogus " + mt + " with " + unit
                    + " from " + unitTile + " to " + currentTarget
                    + " at " + currentTarget.getTile());
                return lbWait(lb);
            }
            AIMessage.askAttack(aiUnit, d);
            return lbAttack(lb, currentTarget);

        default:
            return lbMove(lb, mt);
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

        target = xr.getLocationAttribute(getGame(), TARGET_TAG, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return TAG; }
}
