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

package net.sf.freecol.server.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Locatable;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitLocation;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.ai.AIObject;
import net.sf.freecol.server.ai.goal.Goal;
import net.sf.freecol.server.ai.mission.BuildColonyMission;
import net.sf.freecol.server.ai.mission.CashInTreasureTrainMission;
import net.sf.freecol.server.ai.mission.DefendSettlementMission;
import net.sf.freecol.server.ai.mission.IdleAtSettlementMission;
import net.sf.freecol.server.ai.mission.IndianBringGiftMission;
import net.sf.freecol.server.ai.mission.IndianDemandMission;
import net.sf.freecol.server.ai.mission.Mission;
import net.sf.freecol.server.ai.mission.MissionaryMission;
import net.sf.freecol.server.ai.mission.PioneeringMission;
import net.sf.freecol.server.ai.mission.PrivateerMission;
import net.sf.freecol.server.ai.mission.ScoutingMission;
import net.sf.freecol.server.ai.mission.TransportMission;
import net.sf.freecol.server.ai.mission.UnitSeekAndDestroyMission;
import net.sf.freecol.server.ai.mission.UnitWanderHostileMission;
import net.sf.freecol.server.ai.mission.UnitWanderMission;
import net.sf.freecol.server.ai.mission.WishRealizationMission;
import net.sf.freecol.server.ai.mission.WorkInsideColonyMission;

import org.w3c.dom.Element;


/**
 * Objects of this class contains AI-information for a single {@link Unit}.
 *
 * The method {@link #doMission(LogBuilder)} is called once each turn,
 * by {@link AIPlayer#startWorking()}, to perform the assigned
 * <code>Mission</code>.  Most of the methods in this class just
 * delegates the call to that mission.
 *
 * @see Mission
 */
public class AIUnit extends TransportableAIObject {

    private static final Logger logger = Logger.getLogger(AIUnit.class.getName());

    /** The Unit this AIObject contains AI-information for. */
    private Unit unit;

    /** The mission to which this AI unit has been assigned. */
    private Mission mission;

    /** The goal this AIUnit belongs to, if one has been assigned. */
    private Goal goal = null;

    /** The dynamic part of the transport priority. */
    private int dynamicPriority;


    /**
     * Creates a new uninitialized <code>AIUnit</code>.
     *
     * @param aiMain The main AI-object.
     * @param id The object identifier.
     */
    public AIUnit(AIMain aiMain, String id) {
        super(aiMain, id);

        this.unit = null;
        this.mission = null;
        this.goal = null;
        this.dynamicPriority = 0;
    }

    /**
     * Creates a new <code>AIUnit</code>.
     *
     * @param aiMain The main AI-object.
     * @param unit The unit to make an {@link AIObject} for.
     */
    public AIUnit(AIMain aiMain, Unit unit) {
        this(aiMain, unit.getId());

        this.unit = unit;

        uninitialized = unit == null;
    }

    /**
     * Creates a new <code>AIUnit</code> from the given
     * XML-representation.
     *
     * @param aiMain The main AI-object.
     * @param element The root element for the XML-representation
     *       of a <code>Wish</code>.
     */
    public AIUnit(AIMain aiMain, Element element) {
        super(aiMain, element);

        uninitialized = getUnit() == null;
    }

    /**
     * Creates a new <code>AIUnit</code> from the given
     * XML-representation.
     *
     * @param aiMain The main AI-object.
     * @param xr The input stream containing the XML.
     * @exception XMLStreamException if a problem was encountered
     *     during parsing.
     */
    public AIUnit(AIMain aiMain,
                  FreeColXMLReader xr) throws XMLStreamException {
        super(aiMain, xr);

        uninitialized = getUnit() == null;
    }


    /**
     * Gets the <code>Unit</code> this <code>AIUnit</code> controls.
     *
     * @return The <code>Unit</code>.
     */
    public final Unit getUnit() {
        return unit;
    }

    /**
     * Checks if this unit has been assigned a mission.
     *
     * @return True if this unit has a mission.
     */
    public final boolean hasMission() {
        return mission != null;
    }

    /**
     * Gets the mission this unit has been assigned.
     *
     * @return The <code>Mission</code>.
     */
    public final Mission getMission() {
        return mission;
    }

    /**
     * Assigns a mission to unit. 
     *
     * @param mission The new <code>Mission</code>.
     */
    public final void setMission(Mission mission) {
        this.mission = mission;
    }

    /**
     * Gets the goal of this AI unit.
     *
     * @return The goal of this AI unit.
     */
    public final Goal getGoal() {
        return goal;
    }

    /**
     * Sets the goal of this AI unit.
     *
     * @param goal The new <code>Goal</code>.
     */
    public final void setGoal(Goal goal) {
        this.goal = goal;
    }


    // Internal

    /**
     * If this unit has a transport, retarget.
     */
    private void retargetTransport() {
        AIUnit transport = getTransport();
        if (transport != null) {
            Mission m = transport.getMission();
            if (m instanceof TransportMission) {
                ((TransportMission)m).requeueTransportable(this);
            }
        }
    }


    // Public interface

    /**
     * Is this AI unit carrying any cargo (units or goods).
     *
     * @return True if the unit has cargo aboard.
     */
    public boolean hasCargo() {
        return (unit == null) ? false : unit.hasCargo();
    }

    /**
     * Gets the PRNG to use with this unit.
     *
     * @return A <code>Random</code> instance.
     */
    public Random getAIRandom() {
        return getAIMain().getAIPlayer(unit.getOwner()).getAIRandom();
    }

    /**
     * Gets the AIPlayer that owns this AIUnit.
     *
     * @return The owning AIPlayer.
     */
    public AIPlayer getAIOwner() {
        return (unit == null) ? null
            : getAIMain().getAIPlayer(unit.getOwner());
    }

    /**
     * Does this unit have a particular class of mission?
     *
     * @param returnClass The <code>Class</code> of mission to check.
     * @return True if the mission is of the given class.
     */
    public <T extends Mission> boolean hasMission(Class<T> returnClass) {
        return getMission(returnClass) != null;
    }

    /**
     * Get the unit mission if it is of a given class.
     *
     * @param returnClass The <code>Class</code> of the mission.
     * @return The <code>Mission</code>, or null if it is not of the
     *     given class.
     */
    public <T extends Mission> T getMission(Class<T> returnClass) {
        Mission m = getMission();
        try {
            return returnClass.cast(m);
        } catch (ClassCastException cce) {
            return null;
        }
    }

    /**
     * Change the mission of a unit.
     * The dynamic priority is reset.
     *
     * @param mission The new <code>Mission</code>.
     * @param lb A <code>LogBuilder</code> to log to.
     */
    public void changeMission(Mission mission, LogBuilder lb) {
        if (this.mission == mission) return;
        Location oldTarget;

        if (this.mission == null) {
            lb.add(unit, " replaced null with ", mission);
            oldTarget = null;
        } else {
            lb.add(unit, " replaced ", this.mission, " with ", mission);
            oldTarget = this.mission.getTarget();
            this.mission.dispose();
        }
       
        setMission(mission);
        this.dynamicPriority = 0;

        final AIUnit transport = getTransport();
        final TransportMission tm = (transport == null) ? null
            : transport.getMission(TransportMission.class);
        boolean clear;
        if (transport == null) {
            clear = true;
        } else {
            if (tm == null) {
                clear = true;
            } else {
                if (mission == null) {
                    clear = true;
                } else if (oldTarget == mission.getTarget()) {
                    clear = false;
                    lb.add(" (transport preserved)");
                } else if (tm.requeueTransportable(this)) {
                    clear = false;
                    lb.add(" (transport requeued)");
                } else {
                    clear = true;
                }
                if (clear) {
                    tm.removeTransportable(this);
                    lb.add(" (transport dequeued)");
                }
            }
            if (clear) setTransport(null, "(mission-change)");
        }
        if (clear && unit.isOnCarrier()) {
            if (leaveTransport()) {
                lb.add("(disembarked)");
            } else {
                lb.add("(stuck on", unit.getLocation(), ")");
            }
        }
    }

    /**
     * Performs the mission this unit has been assigned.
     *
     * @param lb A <code>LogBuilder</code> to log to.
     */
    public Mission doMission(LogBuilder lb) {
        return (mission != null) ? mission.doMission(lb) : null;
    }

    /**
     * Moves a unit to the new world.
     *
     * @return True if there was no c-s problem.
     */
    public boolean moveToAmerica() {
        return AIMessage.askMoveTo(this, unit.getOwner().getGame().getMap());
    }

    /**
     * Moves a unit to Europe.
     *
     * @return True if there was no c-s problem.
     */
    public boolean moveToEurope() {
        return AIMessage.askMoveTo(this, unit.getOwner().getEurope());
    }

    /**
     * Moves this AI unit.
     *
     * @param direction The <code>Direction</code> to move.
     * @return True if the move succeeded.
     */
    public boolean move(Direction direction) {
        Tile start = unit.getTile();
        return unit.getMoveType(direction).isProgress()
            && AIMessage.askMove(this, direction)
            && unit.getTile() != start;
    }

    /**
     * Equips this AI unit for a particular role.
     *
     * The unit must be at a location where the required goods are available
     * (possibly requiring a purchase, which may fail due to lack of gold
     * or boycotts in effect).
     *
     * @param roleId The role identifier.
     * @return True if the role change was successful.
     */
    public boolean equipForRole(String roleId) {
        final Specification spec = getSpecification();
        Role r = spec.getRole(roleId);
        final Player player = unit.getOwner();
        Location loc = upLoc(unit.getLocation());
        if (!(loc instanceof UnitLocation)) return false;
        int count = r.getMaximumCount();
        if (count > 0) {
            for (; count > 0; count--) {
                List<AbstractGoods> req = unit.getGoodsDifference(r, count);
                int price = ((UnitLocation)loc).priceGoods(req);
                if (price < 0) continue;
                if (player.checkGold(price)) break;
            }
            if (count <= 0) return false;
        }
        return AIMessage.askEquipForRole(this, r, count)
            && unit.getRole() == r && unit.getRoleCount() == count;
    }


    // Implement TransportableAIObject

    /**
     * {@inheritDoc}
     */
    public Locatable getTransportLocatable() {
        return unit;
    }

    /**
     * {@inheritDoc}
     */
    public Location getTransportSource() {
        return (getUnit() == null || getUnit().isDisposed()) ? null
            : getUnit().getLocation();
    }

    /**
     * {@inheritDoc}
     */
    public Location getTransportDestination() {
        return (getUnit() == null || getUnit().isDisposed() || !hasMission())
            ? null
            : mission.getTransportDestination();
    }

    /**
     * {@inheritDoc}
     */
    public void setTransportDestination(Location location) {
        throw new RuntimeException("Can not set transport destination for AIUnit");
    }

    /**
     * {@inheritDoc}
     */
    public boolean carriableBy(Unit carrier) {
        return carrier.couldCarry(unit);
    }

    /**
     * {@inheritDoc}
     */
    public boolean leaveTransport() {
        final Unit unit = getUnit();
        if (!unit.isOnCarrier()) return true; // Harmless error

        // Just leave at once if in Europe
        if (unit.isInEurope()) return leaveTransport(null);

        // Otherwise if not on the map, do nothing.
        final Tile tile = unit.getTile();
        if (tile == null) return false;

        // Try to go to the target location.
        final Mission mission = getMission();
        final Location target = (mission == null || !mission.isValid()) ? null
            : mission.getTarget();
        Direction direction;
        if (target != null) {
            if (Map.isSameLocation(target, tile)) return leaveTransport(null);
            if (target.getTile() != null
                && (direction = tile.getDirection(target.getTile())) != null) {
                return leaveTransport(direction);
            }
            PathNode path = unit.findPath(target); // Not using carrier!
            if (path != null
                && (direction = tile.getDirection(path.next.getTile())) != null) {
                return leaveTransport(direction);
            }
        }

        // Just get off here if possible.
        if (tile.isLand()) return leaveTransport(null);

        // Collect neighbouring land tiles, get off if one has our settlement
        List<Tile> tiles = new ArrayList<Tile>();
        for (Tile t : tile.getSurroundingTiles(1)) {
            if (!t.isBlocked(unit)) {
                if (t.getSettlement() != null) {
                    return leaveTransport(tile.getDirection(t));
                }
                tiles.add(t);
            }
        }

        // No adjacent unblocked tile, fail.
        if (tiles.isEmpty()) return false;

        // Pick the available tile with the shortest path to one of our
        // settlements, or the tile with the highest defence value.
        final Player player = unit.getOwner();
        final Map map = getGame().getMap();
        Tile safe = tiles.get(0);
        Tile best = null;
        int bestTurns = Integer.MAX_VALUE;
        Settlement settlement = null;
        for (Tile t : tiles) {
            if (settlement == null
                || !map.isSameContiguity(t, settlement.getTile())) {
                settlement = t.getNearestSettlement(player, 10, true);
            }
            if (settlement != null) {
                int turns = unit.getTurnsToReach(t, settlement);
                if (bestTurns > turns) {
                    bestTurns = turns;
                    best = t;
                }
            }
            if (safe.getDefenceValue() < t.getDefenceValue()) {
                safe = t;
            }
        }
        return leaveTransport(tile.getDirection((best != null) ? best : safe));
    }               
        
    /**
     * {@inheritDoc}
     */
    public boolean leaveTransport(Direction direction) {
        if (!unit.isOnCarrier()) return false;
        final Unit carrier = unit.getCarrier();
        boolean result = (direction == null)
            ? (AIMessage.askDisembark(this)
                && unit.getLocation() == carrier.getLocation())
            : move(direction);
        if (result) {
            Colony colony = unit.getColony();
            if (colony != null) {
                colony.firePropertyChange(Colony.REARRANGE_WORKERS,
                                          true, false);
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public boolean joinTransport(Unit carrier, Direction direction) {
        AIUnit aiCarrier = getAIMain().getAIUnit(carrier);
        if (aiCarrier == null) return false;
        Location old = upLoc(unit.getLocation());
        boolean result = AIMessage.askEmbark(aiCarrier, unit, direction)
            && unit.getLocation() == carrier;

        if (result) {
            Colony colony = unit.getColony();
            if (colony != null) {
                colony.firePropertyChange(Colony.REARRANGE_WORKERS,
                                          true, false);
            }
            retargetTransport();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public String invalidReason() {
        String reason = Mission.invalidTransportableReason(this);
        return (reason != null) ? reason
            : (hasMission()) ? getMission().invalidReason()
            : null;
    }


    // Override AIObject

    /**
     * Disposes this object and any attached mission.
     */
    @Override
    public void dispose() {
        AIPlayer aiOwner = getAIOwner();
        if (aiOwner != null) {
            aiOwner.removeAIUnit(this);
        } else {
            // This happens with missionaries, and legitimately when a unit
            // changes owner.  TODO: cleanup.
            logger.warning("Disposing of " + getId() + " but owner is null!");
        }
        setMission(null);
        super.dispose();
    }

    /**
     * Checks the integrity of this AIUnit.
     *
     * @param fix Fix problems if possible.
     * @return Negative if there are problems remaining, zero if
     *     problems were fixed, positive if no problems found at all.
     */
    @Override
    public int checkIntegrity(boolean fix) {
        int result = super.checkIntegrity(fix);
        if (unit == null || unit.isDisposed()) {
            result = -1;
        }
        return result;
    }


    // Serialization

    // @compat 0.10.3
    private static final String TILE_IMPROVEMENT_PLAN_MISSION_TAG = "tileImprovementPlanMission";
    // end @compat
    // @compat 0.10.5
    private static final String IDLE_AT_COLONY_MISSION_TAG = "idleAtColonyMission";
    // end @compat


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        if (mission != null && !mission.isOneTime() && mission.isValid()) {
            mission.toXML(xw);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        final AIMain aiMain = getAIMain();

        unit = xr.findFreeColGameObject(aiMain.getGame(), ID_ATTRIBUTE_TAG,
                                        Unit.class, (Unit)null, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        super.readChildren(xr);

        if (getUnit() != null) uninitialized = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final AIMain aiMain = getAIMain();
        final String tag = xr.getLocalName();

        mission = null;
        if (BuildColonyMission.getXMLElementTagName().equals(tag)) {
            mission = new BuildColonyMission(aiMain, this, xr);

        } else if (CashInTreasureTrainMission.getXMLElementTagName().equals(tag)) {
            mission = new CashInTreasureTrainMission(aiMain, this, xr);

        } else if (DefendSettlementMission.getXMLElementTagName().equals(tag)) {
            mission = new DefendSettlementMission(aiMain, this, xr);

        } else if (IdleAtSettlementMission.getXMLElementTagName().equals(tag)
            // @compat 0.10.5
            || IDLE_AT_COLONY_MISSION_TAG.equals(tag)
            // end @compat
                   ) {
            mission = new IdleAtSettlementMission(aiMain, this, xr);

        } else if (IndianBringGiftMission.getXMLElementTagName().equals(tag)) {
            mission = new IndianBringGiftMission(aiMain, this, xr);

        } else if (IndianDemandMission.getXMLElementTagName().equals(tag)) {
            mission = new IndianDemandMission(aiMain, this, xr);

        } else if (MissionaryMission.getXMLElementTagName().equals(tag)) {
            mission = new MissionaryMission(aiMain, this, xr);

        } else if (PioneeringMission.getXMLElementTagName().equals(tag)
            // @compat 0.10.3
            || TILE_IMPROVEMENT_PLAN_MISSION_TAG.equals(tag)
            // end @compat
                   ) {
            mission = new PioneeringMission(aiMain, this, xr);

        } else if (PrivateerMission.getXMLElementTagName().equals(tag)) {
            mission = new PrivateerMission(aiMain, this, xr);

        } else if (ScoutingMission.getXMLElementTagName().equals(tag)) {
            mission = new ScoutingMission(aiMain, this, xr);

        } else if (TransportMission.getXMLElementTagName().equals(tag)) {
            mission = new TransportMission(aiMain, this, xr);

        } else if (UnitSeekAndDestroyMission.getXMLElementTagName().equals(tag)) {
            mission = new UnitSeekAndDestroyMission(aiMain, this, xr);

        } else if (UnitWanderHostileMission.getXMLElementTagName().equals(tag)) {
            mission = new UnitWanderHostileMission(aiMain, this, xr);

        } else if (UnitWanderMission.getXMLElementTagName().equals(tag)) {
            mission = new UnitWanderMission(aiMain, this, xr);

        } else if (WishRealizationMission.getXMLElementTagName().equals(tag)) {
            mission = new WishRealizationMission(aiMain, this, xr);

        } else if (WorkInsideColonyMission.getXMLElementTagName().equals(tag)) {
            mission = new WorkInsideColonyMission(aiMain, this, xr);

        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return (unit == null) ? "AIUnit-null" : unit.toString("AIUnit ");
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "aiUnit"
     */
    public static String getXMLElementTagName() {
        return "aiUnit";
    }
}
