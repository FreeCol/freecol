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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.pathfinding.CostDecider;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.common.model.pathfinding.GoalDeciders;
import net.sf.freecol.common.networking.NetworkConstants;
import net.sf.freecol.common.util.LogBuilder;
import static net.sf.freecol.common.util.RandomUtils.*;
import net.sf.freecol.server.ai.AIColony;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.ai.EuropeanAIPlayer;


/**
 * Mission for building a <code>Colony</code>.
 *
 * @see net.sf.freecol.common.model.Colony Colony
 */
public class BuildColonyMission extends Mission {

    private static final Logger logger = Logger.getLogger(BuildColonyMission.class.getName());

    /** The tag for this mission. */
    private static final String tag = "AI colony builder";

    /**
     * The target of this mission.  It can either be a Tile where a
     * Colony should be built, or an existing connected Colony owned
     * by this player to go to before retargeting.
     */
    private Location target;

    /** The value of a target <code>Tile</code>. */
    private int colonyValue;


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     * @param target The target <code>Location</code> for this mission.
     */
    public BuildColonyMission(AIMain aiMain, AIUnit aiUnit, Location target) {
        super(aiMain, aiUnit, target);
    }

    /**
     * Creates a new <code>BuildColonyMission</code> and reads the given
     * element.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     * @param xr The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public BuildColonyMission(AIMain aiMain, AIUnit aiUnit,
                              FreeColXMLReader xr) throws XMLStreamException {
        super(aiMain, aiUnit);

        readFromXML(xr);
    }


    /**
     * Get the colony value for a tile.
     *
     * @param tile The <code>Tile</code> to test.
     * @return The colony value for this player.
     */
    private int getColonyValue(Tile tile) {
        final Player owner = getAIUnit().getUnit().getOwner();
        return owner.getColonyValue(tile);
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
        Tile tile = loc.getTile();
        Colony colony = loc.getColony();
        return (invalidReason(aiUnit, tile) == null) ? tile
            : (invalidReason(aiUnit, colony) == null) ? colony
            : null;
    }

    /**
     * Gets the value of a path to a colony building site.
     *
     * @param aiUnit The <code>AIUnit</code> to build the colony.
     * @param path The <code>PathNode</code> to check.
     * @return A score for the target.
     */
    public static float scorePath(AIUnit aiUnit, PathNode path) {
        Location loc;
        if (path == null
            || !((loc = extractTarget(aiUnit, path)) instanceof Tile)) 
            return Integer.MIN_VALUE;

        final Tile tile = (Tile)loc;
        final Player player = aiUnit.getUnit().getOwner();
        float turns = path.getTotalTurns() + 1.0f;
        return player.getColonyValue(tile) / turns;
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
                private float bestValue = 0f;

                @Override
                public PathNode getGoal() { return bestPath; }
                @Override
                public boolean hasSubGoals() { return true; }
                @Override
                public boolean check(Unit u, PathNode path) {
                    Location loc = extractTarget(aiUnit, path);
                    if (loc instanceof Tile) {
                        float value = scorePath(aiUnit, path);
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
     * Finds a site for a new colony.  Favour closer sites.
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
        final Unit carrier = unit.getCarrier();
        final GoalDecider gd = getGoalDecider(aiUnit, deferOK);
        final CostDecider standardCd
            = CostDeciders.avoidSettlementsAndBlockingUnits();

        // Try for something sensible nearby.
        return unit.search(start, gd, standardCd, range, carrier);
    }

    /**
     * Finds a site for a new colony or a backup colony to go to.
     *
     * @param aiUnit The <code>AIUnit</code> to find a colony site with.
     * @param range An upper bound on the number of moves.
     * @param deferOK Enables deferring to a fallback colony.
     * @return A new target for this mission.
     */
    public static Location findTarget(AIUnit aiUnit, int range,
                                      boolean deferOK) {
        PathNode path = findTargetPath(aiUnit, range, deferOK);
        return (path != null) ? extractTarget(aiUnit, path)
            : Location.upLoc(findCircleTarget(aiUnit,
                    getGoalDecider(aiUnit, deferOK), range*3, deferOK));
    }

    /**
     * Why would this mission be invalid with the given unit?
     *
     * @param aiUnit The <code>AIUnit</code> to test.
     * @return A reason why the mission would be invalid with the unit,
     *     or null if none found.
     */
    private static String invalidMissionReason(AIUnit aiUnit) {
        String reason = invalidAIUnitReason(aiUnit);
        return (reason != null)
            ? reason
            : (!aiUnit.getUnit().getOwner().canBuildColonies())
            ? "player-not-a-colony-founder"
            : (!aiUnit.getUnit().getType().canBuildColony())
            ? "unit-not-a-colony-founder"
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
     * Why is this mission invalid with a given tile target?
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @param tile The potential target <code>Tile</code>.
     * @return A reason for mission invalidity, or null if none found.
     */
    private static String invalidTileReason(AIUnit aiUnit, Tile tile) {
        Player owner = aiUnit.getUnit().getOwner();
        Player.NoClaimReason reason
            = owner.canClaimToFoundSettlementReason(tile);
        switch (reason) {
        case NONE: case NATIVES:
            return null;
        default:
            break;
        }
        return "target-" + reason;
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
        return (reason != null) ? reason
            : (loc instanceof Colony)
            ? invalidColonyReason(aiUnit, (Colony)loc)
            : (loc instanceof Tile) 
            ? invalidTileReason(aiUnit, (Tile)loc)
            : Mission.TARGETINVALID;
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

      
    // Implement Mission
    //   Inherit dispose, getTransportDestination, isOneTime

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBaseTransportPriority() {
        return NORMAL_TRANSPORT_PRIORITY + 10;
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
            || target instanceof Colony || target instanceof Tile) {
            this.target = target;
            this.colonyValue = (target instanceof Tile)
                ? getColonyValue((Tile)target)
                : -1;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location findTarget() {
        return findTarget(getAIUnit(), 5, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String invalidReason() {
        return invalidReason(getAIUnit(), target);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mission doMission(LogBuilder lb) {
        lb.add(tag);
        final AIMain aiMain = getAIMain();
        final AIUnit aiUnit = getAIUnit();
        final Unit unit = getUnit();
        final Player player = unit.getOwner();
        final EuropeanAIPlayer euaip = getEuropeanAIPlayer();

        boolean retarget = false;
        String reason = invalidReason();
        if (isTargetReason(reason)) {
            // Allow target invalidation by another builder succeeding, and
            // continue on to the target as an intermediate colony.
            Colony c;
            if (target instanceof Tile
                && (c = target.getColony()) != null
                && player.owns(c)) {
                // Favour improving colony center.
                Mission m = euaip.getPioneeringMission(aiUnit, c.getTile());
                if (m != null) return lbDrop(lb, ", improving with ", m);
                // Just go to the colony.
                setTarget(c);
                return lbRetarget(lb);
            }
        } else if (reason != null) {
            return lbFail(lb, false, reason);
        } else { // Target valid, but has it devalued?
            if (target instanceof Tile) {
                int newValue = getColonyValue((Tile)target);
                if (newValue < colonyValue) {
                    reason = "target tile " + target.toShortString()
                        + " value " + colonyValue + " -> " + newValue;
                    retarget = true;
                }
            }
        }
        if (retarget) return retargetMission(reason, lb);

        for (;;) {
            // Go there.
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

            lbAt(lb);
            if (getTarget() instanceof Colony) {
                // If arrived at the target colony it is time to retarget
                // another building site, unless the existing one is small
                // or nothing is found.
                Colony colony = (Colony)getTarget();

                // Improve colony center?
                Mission m
                    = euaip.getPioneeringMission(aiUnit, colony.getTile());
                if (m != null) return lbDrop(lb, ", improving with ", m);

                // Colony too small?
                if (colony.getUnitCount() <= 1) {
                    setTarget(colony);
                    return lbDrop(lb, ", join small colony");
                }

                // Find a real tile target?
                Location newTarget;
                if ((newTarget = findTarget(aiUnit, 5, false)) != null) {
                    setTarget(newTarget);
                    return lbRetarget(lb);
                }

                // Go to the nearest smaller colony?
                Colony best = null;
                int bestValue = INFINITY;
                for (Colony c : player.getColonies()) {
                    if (c == colony) continue;
                    if (c.getUnitCount() < colony.getUnitCount()) continue;
                    PathNode path = unit.findPath(c);
                    if (path != null && path.getTotalTurns() < bestValue) {
                        bestValue = path.getTotalTurns();
                        best = c;
                    }
                }
                if (best != null) {
                    lb.add(", going to smaller ", best.getUnitCount(), "<",
                        colony.getUnitCount(), " colony");
                    setTarget(best);
                    return lbRetarget(lb);
                }

                // Just join up then.
                return lbDrop(lb, ", joining");
            }

            if (!(getTarget() instanceof Tile)) {
                return lbFail(lb, false, "bogus target ", getTarget());
            }
            Tile tile = (Tile)getTarget();
            // Check that the unit has moves left, which are required
            // for building.
            if (unit.getMovesLeft() <= 0) {
                return lbWait(lb, ", waiting to build at ", tile);
            }

            if (tile.getOwner() == null) {
                ; // All is well
            } else if (player.owns(tile)) { // Already ours, clear users
                Colony colony = (Colony)tile.getOwningSettlement();
                if (colony != null) {
                    logger.warning("Building on colony tile: " + tile);
                    return lbFail(lb, false, "building on colony tile ", tile);
                }
            } else {
                // Not our tile, so claim it first.  Fail if someone
                // has claimed the tile and will not sell.  Otherwise
                // try to buy it or steal it.
                int price = player.getLandPrice(tile);
                boolean fail = price < 0;
                if (price > 0 && !player.checkGold(price)) {
                    if (randomInt(logger, "Land gold?", getAIRandom(), 4) == 0) {
                        lb.add(", ");
                        euaip.cheatGold(price, lb);
                        lb.add(" to buy ", tile);
                    }
                }
                if (price >= 0) {
                    fail = !AIMessage.askClaimLand(tile, aiUnit,
                        ((price == 0) ? 0 : (player.checkGold(price)) ? price
                            : NetworkConstants.STEAL_LAND))
                        || !player.owns(tile);
                }
                if (fail) return retargetMission("tile-claim-at-" + tile, lb);
                lb.add(", claimed colony tile");
            }

            // Log the colony values so we can improve things
            if (logger.isLoggable(Level.FINE)) {
                LogBuilder l2 = new LogBuilder(64);
                l2.add(tag, " score-at-foundation ", tile, ":");
                for (Double d : player.getAllColonyValues(tile)) {
                    l2.add(" ", d);
                }
                l2.log(logger, Level.FINE);
            }
            
            // Clear to build the colony.
            if (AIMessage.askBuildColony(aiUnit, Player.ASSIGN_SETTLEMENT_NAME)
                && tile.getColony() != null) {
                Colony colony = tile.getColony();
                AIColony aiColony = aiMain.getAIColony(colony);
                aiColony.requestRearrange();
                Mission m = euaip.getWorkInsideColonyMission(aiUnit, aiColony);
                return lbDone(lb, m != null, colony);
            }
            return lbFail(lb, false, "build at ", tile);
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

            if (colonyValue > 0) {
                xw.writeAttribute(VALUE_TAG, colonyValue);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        target = xr.getLocationAttribute(getGame(), TARGET_TAG, false);

        colonyValue = xr.getAttribute(VALUE_TAG, -1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "buildColonyMission".
     */
    public static String getXMLElementTagName() {
        return "buildColonyMission";
    }
}
