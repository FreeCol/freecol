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
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.common.networking.NetworkConstants;
import net.sf.freecol.common.util.Utils;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIUnit;


/**
 * Mission for building a <code>Colony</code>.
 *
 * This mission can be used in two different ways:
 * <ul>
 * <li>Build a colony at a specific location.</li>
 * <li>Find a site for a colony and build it there.</li>
 * </ul>
 *
 * This mission will be aborted in the former case if the value gets
 * below a given threshold, while a colony will always get built (if
 * there is sufficient space on the map) in the latter case. Use the
 * appropriate constructor to get the desired behaviour.
 *
 * @see net.sf.freecol.common.model.Colony Colony
 */
public class BuildColonyMission extends Mission {

    private static final Logger logger = Logger.getLogger(BuildColonyMission.class.getName());

    private static final String tag = "AI colony builder";

    /** The maximum number of turns to travel to a building site. */
    private static final int MAX_TURNS = 5;

    /**
     * The target of this mission.  It can either be a Tile where a
     * Colony should be built or an existing connected Colony owned
     * by this player to go to before retargeting.
     */
    private Location target = null;

    /** The value of a target <code>Tile</code>. */
    private int colonyValue = -1;


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     * @param target The target for this mission.
     */
    public BuildColonyMission(AIMain aiMain, AIUnit aiUnit, Location target) {
        super(aiMain, aiUnit);

        setTarget(target);
        logger.finest(tag + " starts with target " + target
            + " and value " + colonyValue + ": " + this);
        uninitialized = false;
    }

    /**
     * Creates a new <code>BuildColonyMission</code> and reads the given
     * element.
     *
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public BuildColonyMission(AIMain aiMain, XMLStreamReader in)
        throws XMLStreamException {
        super(aiMain);

        readFromXML(in);
        uninitialized = getAIUnit() == null;
    }


    /**
     * Sets the target of this mission.
     *
     * @param target The new target.
     */
    private void setTarget(Location target) {
        this.target = target;
        this.colonyValue = (target instanceof Tile)
            ? getAIUnit().getUnit().getOwner().getColonyValue((Tile)target)
            : -1;
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
        Colony colony = tile.getColony();
        return (invalidReason(aiUnit, colony) == null) ? colony
            : (invalidReason(aiUnit, tile) == null) ? tile
            : null;
    }

    /**
     * Gets the value of a path to a colony building site.
     *
     * The value is proportional to the general desirability of the
     * site, and inversely proportional to the number of turns to get
     * there.
     *
     * @param aiUnit The <code>AIUnit</code> to build the colony.
     * @param path The <code>PathNode</code> to check.
     * @return A score for the target.
     */
    public static int scorePath(AIUnit aiUnit, PathNode path) {
        final Location loc = extractTarget(aiUnit, path);
        if (invalidReason(aiUnit, loc) != null) return Integer.MIN_VALUE;
        int turns = (path == null) ? 1 : (path.getTotalTurns() + 1);

        if (loc instanceof Colony) {
            return 1000 / turns;

        } else if (loc instanceof Tile) {
            final Tile tile = (Tile)loc;
            final Player player = aiUnit.getUnit().getOwner();
            switch (player.canClaimToFoundSettlementReason(tile)) {
            case NONE:
                break;
            case NATIVES:
                // Penalize value when the tile will need to be stolen
                int price = player.getLandPrice(tile);
                if (price > 0 && !player.checkGold(price)) turns *= 2;
                break;
            default:
                return Integer.MIN_VALUE;
            }
            return (int)(player.getColonyValue(tile) / turns);
        }

        return Integer.MIN_VALUE;
    }

    /**
     * Makes a goal decider that checks colony sites.
     *
     * @param aiUnit The <code>AIUnit</code> to find a colony site with.
     * @param deferOK Keep track of the nearest of our colonies, to use
     *     as a fallback destination.
     * @return A suitable <code>GoalDecider</code>.
     */
    private static GoalDecider getColonyDecider(final AIUnit aiUnit,
                                                final boolean deferOK) {
        return new GoalDecider() {
            private PathNode best = null;
            private int bestValue = 0;
            private PathNode backup = null;
            private int backupValue = 0;

            public PathNode getGoal() { return (best != null) ? best : backup; }
            public boolean hasSubGoals() { return true; }
            public boolean check(Unit u, PathNode path) {
                int value = scorePath(aiUnit, path);
                Colony colony = path.getTile().getColony();
                if (colony != null && invalidReason(aiUnit, colony) == null) {
                    if (deferOK && value > backupValue) {
                        backupValue = value;
                        backup = path;
                        return true;
                    }
                    return false;
                }
                if (value > bestValue) {
                    bestValue = value;
                    best = path;
                    return true;
                }
                return false;
            }
        };
    }
            
    /**
     * Finds a site for a new colony.  Favour closer sites.
     *
     * @param aiUnit The <code>AIUnit</code> to find a colony site with.
     * @param deferOK If true, allow the search to return a nearby existing
     *     colony as a temporary target.     
     * @return A path to the new colony or backup.
     */
    public static PathNode findTargetPath(AIUnit aiUnit, boolean deferOK) {
        Unit unit;
        Tile startTile;
        if (aiUnit == null
            || (unit = aiUnit.getUnit()) == null || unit.isDisposed()
            || (startTile = unit.getPathStartTile()) == null) return null;

        PathNode path;
        final Unit carrier = unit.getCarrier();
        final GoalDecider colonyDecider = getColonyDecider(aiUnit, deferOK);

        // Try for something sensible nearby.
        path = unit.search(startTile, colonyDecider,
                           CostDeciders.avoidIllegal(), MAX_TURNS, carrier);
        if (path != null) return path;

        // Retry, but increase the range.
        path = unit.search(startTile, colonyDecider,
                           CostDeciders.avoidIllegal(), MAX_TURNS*3, carrier);
        if (path != null) return path;

        // One more try with a relaxed cost decider and no range limit.
        return unit.search(startTile, colonyDecider,
                           CostDeciders.numberOfTiles(), INFINITY, carrier);
    }

    /**
     * Finds a site for a new colony or a backup colony to go to.
     *
     * @param aiUnit The <code>AIUnit</code> to find a colony site with.
     * @param deferOK If true, allow the search to return a nearby existing
     *     colony as a temporary target.     
     * @return A new target for this mission.
     */
    public static Location findTarget(AIUnit aiUnit, boolean deferOK) {
        PathNode path = findTargetPath(aiUnit, deferOK);
        return (path == null) ? getBestSettlement(aiUnit.getUnit().getOwner())
            : extractTarget(aiUnit, path);
    }

       
    // Fake Transportable interface

    /**
     * Gets the transport destination for the unit with this mission.
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
     * Gets the target of this mission.
     *
     * @return The tile where a colony is to be built.
     */
    public Location getTarget() {
        return target;
    }

    /**
     * Why would a BuildColonyMission be invalid with the given unit?
     *
     * @param aiUnit The <code>AIUnit</code> to test.
     * @return A reason why the mission would be invalid with the unit,
     *     or null if none found.
     */
    private static String invalidBuildReason(AIUnit aiUnit) {
        return (!aiUnit.getUnit().hasAbility("model.ability.foundColony"))
            ? "unit-not-a-colony-founder"
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
            : ((reason = invalidBuildReason(aiUnit)) != null) ? reason
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
            : ((reason = invalidBuildReason(aiUnit)) != null) ? reason
            : (loc instanceof Colony)
            ? (((reason = invalidTargetReason(loc, aiUnit.getUnit().getOwner()))
                    != null) ? reason : null)
            : (loc instanceof Tile)
            ? (((reason = invalidTargetReason(loc, null)) != null) ? reason
                : (!((Tile)loc).isLand()) ? "target-not-land"
                : (((Tile)loc).getColony() != null) ? "target-has-colony"
                : null)
            : Mission.TARGETINVALID;
    }

    // Not a one-time mission, omit isOneTime().

    /**
     * Performs this mission.
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

        // Check the target
        final AIMain aiMain = getAIMain();
        final Player player = unit.getOwner();
        final AIUnit aiUnit = getAIUnit();
        Location newTarget;
        int value;
        if (reason != null
            || (target instanceof Tile
                && (value = player.getColonyValue((Tile)target)) < colonyValue)) {
            if ((newTarget = findTarget(aiUnit, true)) == null) {
                setTarget(null);
                logger.finest(tag + " unable to retarget: " + this);
                return;
            }
            setTarget(newTarget);
        }

        // Go there.
        if (travelToTarget(tag, target) != Unit.MoveType.MOVE) return;

        // If arrived at the target colony it is time to either
        // retarget and insist on finding a building site.  On failure,
        // try working in the colony for the present.
        if (target instanceof Colony) {
            String name = ((Colony)target).getName();
            PathNode path = findTargetPath(aiUnit, false);
            if (path != null
                && (newTarget = extractTarget(aiUnit, path)) != null) {
                setTarget(newTarget);
                logger.finest(tag + " arrived at " + name
                    + ", retargeting " + target + ": " + this);
            } else {
                logger.finest(tag + " gives up at " + name + ": " + this);
                aiUnit.setMission(new WorkInsideColonyMission(aiMain, aiUnit,
                        aiMain.getAIColony((Colony)target)));
            }
            return;
        }

        // Arrived at the target (non-colony) tile.
        if (!(target instanceof Tile)) {
            throw new IllegalStateException("Not a building-site tile: "
                + target);
        }
        Tile tile = (Tile)target;
        if (tile.getOwner() == null) {
            ; // All is well
        } else if (player.owns(tile)) { // Already ours, clear users
            Colony colony = (Colony)tile.getOwningSettlement();
            if (colony != null
                && colony.getColonyTile(tile) != null) {
                colony.getColonyTile(tile).relocateWorkers();
            }
        } else {
            // Not our tile, so claim it first.  Fail if someone has
            // claimed the tile and will not sell.  Otherwise try to
            // buy it or steal it.
            int price = player.getLandPrice(tile);
            boolean fail = price < 0;
            if (price > 0 && !player.checkGold(price)) {
                if (Utils.randomInt(logger, "Cheat gold",
                                    getAIRandom(), 4) == 0) {
                    // CHEAT: provide the gold needed
                    player.modifyGold(price);
                }
            }
            if (price >= 0) {
                fail = !AIMessage.askClaimLand(tile, aiUnit,
                    ((price == 0) ? 0 : (player.checkGold(price)) ? price
                        : NetworkConstants.STEAL_LAND))
                    || !player.owns(tile);
            }
            if (fail) {
                logger.finest(tag + " failed to claim land at " + tile
                    + ": " + this);
                setTarget(null);
                return;
            }
        }

        // Check that the unit has moves left, which are required for building.
        if (unit.getMovesLeft() <= 0) {
            logger.finest(tag + " waiting to build at " + tile + ": " + this);
            return;
        }
            
        // Clear to build the colony.
        if (AIMessage.askBuildColony(aiUnit, Player.ASSIGN_SETTLEMENT_NAME)
            && tile.getColony() != null) {
            Colony colony = tile.getColony();
            logger.finest(tag + " completed " + colony.getName()
                + ": " + this);
            aiUnit.setMission(new WorkInsideColonyMission(aiMain, aiUnit,
                    aiMain.getAIColony(colony)));
        } else {
            setTarget(null);
            logger.warning(tag + " failed to build at " + tile + ": " + this);
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
        if (isValid()) {
            toXML(out, getXMLElementTagName());
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeAttributes(out);

        if (target != null) {
            writeAttribute(out, "target", (FreeColGameObject)target);

            if (colonyValue > 0) {
                out.writeAttribute("value", Integer.toString(colonyValue));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void readAttributes(XMLStreamReader in)
        throws XMLStreamException {
        super.readAttributes(in);

        String str = in.getAttributeValue(null, "target");
        target = getGame().getFreeColLocation(str);

        colonyValue = getAttribute(in, "value", -1);
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "buildColonyMission".
     */
    public static String getXMLElementTagName() {
        return "buildColonyMission";
    }
}
