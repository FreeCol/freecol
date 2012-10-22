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
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.pathfinding.CostDecider;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.common.model.pathfinding.GoalDeciders;
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
    private Location target = null;


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     * @param loc The target <code>Location</code>.
     */
    public ScoutingMission(AIMain aiMain, AIUnit aiUnit, Location loc) {
        super(aiMain, aiUnit);

        setTarget(loc);
        logger.finest(tag + " starts at " + aiUnit.getUnit().getLocation()
            + " with target " + target + ": " + this);
        uninitialized = false;
    }

    /**
     * Creates a new <code>ScoutingMission</code> and reads the given element.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public ScoutingMission(AIMain aiMain, AIUnit aiUnit, XMLStreamReader in)
        throws XMLStreamException {
        super(aiMain, aiUnit);

        readFromXML(in);
        uninitialized = getAIUnit() == null;
    }


    /**
     * Does a supplied unit have horses?
     *
     * @param aiUnit The scout <code>AIUnit</code> to check.
     * @return True if the scout has horses.
     */
    private static boolean hasHorses(AIUnit aiUnit) {
        return aiUnit.getUnit()
            .hasAbility("model.ability.scoutIndianSettlement");
    }

    /**
     * Does this scout have horses?
     *
     * @return True if the scout has horses.
     */
    private boolean hasHorses() {
        return hasHorses(getAIUnit());
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
        Location loc;
        if (path == null
            || (loc = extractTarget(aiUnit, path)) == null
            || !(loc instanceof IndianSettlement || loc instanceof Tile))
            return Integer.MIN_VALUE;
        return 1000 / (path.getTotalTurns() + 1);
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
                private int bestValue = 0;

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
        return (deferOK) ? GoalDeciders.getComposedGoalDecider(gd,
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
     * Prepare a unit for this mission.
     *
     * @param aiUnit The <code>AIUnit</code> to prepare.
     * @return A reason why the unit can not perform this mission, or null
     *     if none.
     */
    public static String prepare(AIUnit aiUnit) {
        String reason = invalidReason(aiUnit);
        if (reason != null) return reason;
        if (!hasHorses(aiUnit)) aiUnit.equipForRole(Unit.Role.SCOUT, false);
        return (hasHorses(aiUnit)
            || aiUnit.getUnit().hasAbility(Ability.EXPERT_SCOUT)) ? null
            : "unit-not-a-SCOUT";
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
        boolean retarget = this.target != null && this.target != target;
        this.target = target;
        if (retarget) retargetTransportable();
    }

    /**
     * {@inheritDoc}
     */
    public Location findTarget() {
        return findTarget(getAIUnit(), 20, true);
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
            : (!hasHorses(aiUnit)) ? "unit-not-a-SCOUT"
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
            : (is.hasSpokenToChief(owner))
            ? "settlement-contacted"
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

        // Go to the target.
        final AIUnit aiUnit = getAIUnit();
        final Unit unit = getUnit();
        Direction d;
        Unit.MoveType mt = travelToTarget(tag, getTarget(),
            CostDeciders.avoidSettlementsAndBlockingUnits());
        switch (mt) {
        case MOVE_ILLEGAL: case MOVE_NO_MOVES: case MOVE_NO_REPAIR:
            return;
        case ATTACK_UNIT:
            // Could be adjacent to the destination but it is
            // temporarily blocked by another unit.  Make a random
            // (directed if possible) move and try again.
            moveRandomly(tag, unit.getTile()
                .getDirection(getTarget().getTile()));
            return;
        case MOVE:
            break;
        case ENTER_INDIAN_SETTLEMENT_WITH_SCOUT:
            if ((d = unit.getTile()
                    .getDirection(getTarget().getTile())) == null) {
                throw new IllegalStateException("Unit not next to target "
                    + getTarget() + ": " + unit + "/" + unit.getLocation());
            }
            if (!AIMessage.askScoutIndianSettlement(aiUnit, d)) {
                logger.warning(tag + " unexpected failure at " + getTarget()
                    + ": " + this);
            }
            break;
        case EXPLORE_LOST_CITY_RUMOUR:
            if ((d = unit.getTile()
                    .getDirection(getTarget().getTile())) == null) {
                throw new IllegalStateException("Unit not next to target "
                    + getTarget() + ": " + unit + "/" + unit.getLocation());
            }
            if (!AIMessage.askMove(aiUnit, d)) {
                logger.warning(tag + " unexpected failure at " + getTarget()
                    + ": " + this);
            }
            break;
        default:
            logger.warning(tag + " unexpected move type " + mt + ": " + this);
            return;
        }
        if (unit.isDisposed()) {
            logger.finest(tag + " died at target " + getTarget() + ": " + this);
            return;
        }

        // Retarget on failure or complete, but do not retarget from
        // one colony to another, just drop equipment and invalidate
        // the mission.
        Location completed = getTarget();
        setTarget(findTarget(aiUnit, 20, false));
        if (completed instanceof Colony) {
            if (getTarget() == null) {
                for (EquipmentType e : new ArrayList<EquipmentType>(unit
                        .getEquipment().keySet())) {
                    AIMessage.askEquipUnit(aiUnit, e, -unit.getEquipmentCount(e));
                }
            }
            logger.finest(tag + " arrived at " + ((Colony)completed).getName()
                + ", retargeting " + getTarget() + ": " + this);
        } else {
            logger.finest(tag + " completed target " + completed
                + ", retargeting " + getTarget() + ": " + this);
        }
    }


    // Serialization

    /**
     * {@inheritDoc}
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        if (isValid()) {
            toXML(out, getXMLElementTagName());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
    protected void readAttributes(XMLStreamReader in)
        throws XMLStreamException {
        super.readAttributes(in);

        String str = in.getAttributeValue(null, "target");
        target = (str == null) ? null : getGame().getFreeColLocation(str);
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "scoutingMission".
     */
    public static String getXMLElementTagName() {
        return "scoutingMission";
    }
}
