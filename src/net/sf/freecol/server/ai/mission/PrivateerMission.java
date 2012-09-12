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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.MoveType;
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
     * @param aiUnit The <code>AIUnit</code> this mission
     *        is created for.
     */
    public PrivateerMission(AIMain aiMain, AIUnit aiUnit) {
        super(aiMain, aiUnit);

        Unit unit = aiUnit.getUnit();
        logger.finest(tag + " begins at " + unit.getLocation() + ": " + this);
        uninitialized = false;
    }

    /**
     * Creates a new <code>UnitWanderHostileMission</code> and reads
     * the given element.
     *
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public PrivateerMission(AIMain aiMain, XMLStreamReader in)
        throws XMLStreamException {
        super(aiMain);

        readFromXML(in);
        uninitialized = getAIUnit() == null;
    }


    /**
     * Sets a new mission target.
     *
     * @param target The new target <code>Location</code>.
     */
    public void setTarget(Location target) {
        removeTransportable("retargeted");
        this.target = target;
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
        final Location loc = path.getLastNode().getLocation();
        Settlement settlement = loc.getSettlement();
        Tile tile = loc.getTile();
        Unit other = (tile == null) ? null : tile.getDefendingUnit(unit);
        return (aiUnit.hasCargo())
            ? ((loc instanceof Europe) ? loc
                : (settlement instanceof Colony) ? settlement
                : null)
            : ((other != null) ? other : null);
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
        value += defender.getGoodsSpaceTaken() * 200;
        value += defender.getUnitSpaceTaken() * 100;
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
     * Gets the transport destination for units with this mission.
     *
     * @return Always null, we never transport carrier units.
     */
    @Override
    public Location getTransportDestination() {
        return null;
    }


    // Mission interface

    /**
     * Gets the target for this mission.
     *
     * @return The target for this mission.
     */
    public Location getTarget() {
        return target;
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
     * @param settlement The <code>Settlement</code> to test.
     * @return A reason why the mission would be invalid, or null if
     *     none found.
     */
    private static String invalidUnitReason(AIUnit aiUnit, Unit unit) {
        Player player = aiUnit.getUnit().getOwner();
        Player other = unit.getOwner();
        return (unit == null)
            ? Mission.TARGETINVALID
            : (player == other)
            ? Mission.TARGETOWNERSHIP
            : (player.getStance(other) == Stance.ALLIANCE)
            ? "privateer-avoids-ally"
            : (scoreUnit(aiUnit, unit) <= 0)
            ? "privateer-avoids-trouble"
            : null;
    }

    /**
     * Why is this mission invalid?
     *
     * @return A reason for the mission invalidity, or null if still valid.
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
            : (aiUnit.hasCargo() && loc instanceof Europe)
            ? invalidTargetReason(loc, aiUnit.getUnit().getOwner())
            : (aiUnit.hasCargo() && loc instanceof Settlement)
            ? invalidSettlementReason(aiUnit, (Settlement)loc)
            : (!aiUnit.hasCargo() && loc == null)
            ? null
            : (!aiUnit.hasCargo() && loc instanceof Unit)
            ? invalidUnitReason(aiUnit, (Unit)loc)
            : Mission.TARGETINVALID;
    }

    // Not a one-time mission, omit isOneTime().

    /**
     * Performs the mission. This is done by searching for hostile units
     * that are located within one tile and attacking them. If no such units
     * are found, then wander in a random direction.
     */
    public void doMission() {
        final AIUnit aiUnit = getAIUnit();
        String reason = invalidReason();
        if (isTargetReason(reason)) {
            target = null; // Handled below
        } else if (reason != null) {
            logger.finest(tag + " broken(" + reason + "): " + this);
            return;
        }

        final Unit unit = getUnit();
        if (unit.isAtSea()) return;

        Direction direction;
        Location newTarget;
        if (aiUnit.hasCargo()) { // Deliver the goods
            if (isTargetReason(reason)) {
                if ((newTarget = findTarget(aiUnit, 8, true)) == null) {
                    logger.finest(tag + " could not retarget: " + this);
                    return;
                }
                setTarget(newTarget);
            }
            Unit.MoveType mt = travelToTarget(tag, getTarget(),
                CostDeciders.avoidSettlementsAndBlockingUnits());
            switch (mt) {
            case MOVE_NO_MOVES: case MOVE_HIGH_SEAS:
                return;
            case MOVE:
                for (Goods g : unit.getGoodsList()) {
                    if (unit.isInEurope()) {
                        goodsLeavesTransport(g.getType(), g.getAmount());
                    } else {
                        Colony colony = unit.getTile().getColony();
                        unloadCargoInColony(g);
                    }
                }

                for (Unit u : unit.getUnitList()) {
                    unitLeavesTransport(getAIMain().getAIUnit(u), null);
                }

                logger.finest(tag + " completed goods delivery"
                    + " at " + unit.getLocation() + ": " + this);
                setTarget(findTarget(aiUnit, 1, false));
                break;
            default:
                logger.warning(tag + " unexpected delivery move " + mt
                    + ": " + this);
                break;
            }
        } else if (unit.isInEurope()) {
            Settlement settlement = getBestSettlement(unit.getOwner());
            Tile tile = (settlement != null) ? settlement.getTile()
                : unit.getFullEntryLocation();
            unit.setDestination(tile);
            aiUnit.moveToAmerica();
        } else if ((newTarget = findTarget(aiUnit, 1, true)) == null) {
            moveRandomlyTurn(tag);
        } else {
            setTarget(newTarget);
            Unit.MoveType mt = travelToTarget(tag, getTarget(), null);
            switch (mt) {
            case MOVE_NO_MOVES:
                return;
            case MOVE_ILLEGAL: // Can happen when another unit blocks a river
                logger.finest(tag + " hit unexpected blockage: " + this);
                moveRandomly(tag, null);
                unit.setMovesLeft(0);
                return;
            case ATTACK_UNIT:
                direction = unit.getTile().getDirection(getTarget().getTile());
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
                logger.warning(tag + " unexpected hunt move " + mt
                    + ": " + this);
                break;
            }
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
     * Returns the tag name of the root element representing this object.
     *
     * @return "privateerMission"
     */
    public static String getXMLElementTagName() {
        return "privateerMission";
    }
}
