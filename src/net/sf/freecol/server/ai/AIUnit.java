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

package net.sf.freecol.server.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Locatable;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitLocation;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.util.LogBuilder;
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

    /** The goal.  Currently unused. */
    private Goal goal = null;


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
        this.mission = null;
        this.goal = null;

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
        return this.unit;
    }

    /**
     * Checks if this unit has been assigned a mission.
     *
     * @return True if this unit has a mission.
     */
    public final boolean hasMission() {
        return this.mission != null;
    }

    /**
     * Gets the mission this unit has been assigned.
     *
     * @return The <code>Mission</code>.
     */
    public final Mission getMission() {
        return this.mission;
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
        return this.goal;
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
     * Request a rearrangement of any colony at the current location.
     */
    private void requestLocalRearrange() {
        Location loc;
        Colony colony;
        AIColony aiColony;
        if (unit != null
            && (loc = unit.getLocation()) != null
            && (colony = loc.getColony()) != null
            && (aiColony = getAIMain().getAIColony(colony)) != null) {
            aiColony.requestRearrange();
        }
    }

    /**
     * Take the current carrier as transport, keeping the transport mission/s
     * consistent.
     */
    private void takeTransport() {
        Unit carrier = getUnit().getCarrier();
        AIUnit aiCarrier = (carrier == null) ? null
            : getAIMain().getAIUnit(carrier);
        AIUnit transport = getTransport();
        if (transport != aiCarrier) {
            if (transport != null) {
                logger.warning("Taking different transport: " + aiCarrier);
                dropTransport();
            }
            setTransport(aiCarrier);
        }
    }


    // Public routines

    /**
     * Gets the AIPlayer that owns this AIUnit.
     *
     * @return The owning AIPlayer.
     */
    public AIPlayer getAIOwner() {
        return (unit == null) ? null
            : (unit.getOwner() == null) ? null
            : getAIMain().getAIPlayer(unit.getOwner());
    }

    /**
     * Gets the PRNG to use with this unit.
     *
     * @return A <code>Random</code> instance.
     */
    public Random getAIRandom() {
        return (unit == null) ? null : getAIOwner().getAIRandom();
    }

    /**
     * Get a trivial target, usually a safe nearby settlement or Europe.
     *
     * @return A trivial target, or null if none found.
     */
    public Location getTrivialTarget() {
        PathNode path = unit.getTrivialPath();
        return (path == null) ? null
            : Location.upLoc(path.getLastNode().getLocation());
    }

    /**
     * Is this AI unit carrying any cargo (units or goods).
     *
     * @return True if the unit has cargo aboard.
     */
    public final boolean hasCargo() {
        return (unit == null) ? false : unit.hasCargo();
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
        try {
            return returnClass.cast(this.mission);
        } catch (ClassCastException cce) {
            return null;
        }
    }

    /**
     * Performs the mission this unit has been assigned.
     *
     * Do *not* check mission validity.  The mission itself does that,
     * and has special case error handling.
     *
     * @param lb A <code>LogBuilder</code> to log to.
     */
    public void doMission(LogBuilder lb) {
        if (this.mission != null) this.mission.doMission(lb);
    }

    /**
     * Change the mission of a unit.
     *
     * @param mission The new <code>Mission</code>.
     * @return The new current <code>Mission</code>.
     */
    public Mission changeMission(Mission mission) {
        if (this.mission == mission) return this.mission;

        if (this.mission != null) {
            this.mission.dispose();
            this.mission = null;
        }
       
        setMission(mission);
        if (mission != null) {
            setTransportPriority(mission.getBaseTransportPriority());
        }
        return this.mission;
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
     * @param role The <code>Role</code> to equip for identifier.
     * @return True if the role change was successful.
     */
    public boolean equipForRole(Role role) {
        final Specification spec = getSpecification();
        final Player player = unit.getOwner();
        Location loc = Location.upLoc(unit.getLocation());
        if (!(loc instanceof UnitLocation)) return false;
        int count = role.getMaximumCount();
        if (count > 0) {
            for (; count > 0; count--) {
                List<AbstractGoods> req = unit.getGoodsDifference(role, count);
                int price = ((UnitLocation)loc).priceGoods(req);
                if (price < 0) continue;
                if (player.checkGold(price)) break;
            }
            if (count <= 0) return false;
        }
        return AIMessage.askEquipForRole(this, role, count)
            && unit.getRole() == role && unit.getRoleCount() == count;
    }

        
    // Implement TransportableAIObject

    /**
     * {@inheritDoc}
     */
    @Override
    public int getTransportPriority() {
        return (hasMission()) ? super.getTransportPriority() : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Locatable getTransportLocatable() {
        return unit;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location getTransportSource() {
        return (getUnit() == null || getUnit().isDisposed()) ? null
            : getUnit().getLocation();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location getTransportDestination() {
        return (getUnit() == null || getUnit().isDisposed() || !hasMission())
            ? null
            : mission.getTransportDestination();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PathNode getDeliveryPath(Unit carrier, Location dst) {
        if (dst == null) {
            dst = getTransportDestination();
            if (dst == null) return null;
        }
        dst = Location.upLoc(dst);

        PathNode path;
        if (unit.getLocation() == carrier) {
            path = unit.findPath(carrier.getLocation(), dst, carrier, null);
            if (path == null && dst.getTile() != null) {
                path = unit.findPathToNeighbour(carrier.getLocation(),
                    dst.getTile(), carrier, null);
            }
        } else if (unit.getLocation() instanceof Unit) {
            return null;
        } else {
            path = unit.findPath(unit.getLocation(), dst, carrier, null);
            if (path == null && dst.getTile() != null) {
                path = unit.findPathToNeighbour(unit.getLocation(),
                    dst.getTile(), carrier, null);
            }
        }
        if (path != null) path.ensureDisembark();
        return path;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PathNode getIntermediatePath(Unit carrier, Location dst) {
        return null; // NYI
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTransportDestination(Location destination) {
        throw new RuntimeException("AI unit transport destination set by mission.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean carriableBy(Unit carrier) {
        return carrier.couldCarry(getUnit());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canMove() {
        return getUnit().getMovesLeft() > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
                try {
                    return leaveTransport(direction);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Leave transport crash for "
                        + this + "/" + unit.getMovesLeft(), e);
                }
            }
        }

        // Just get off here if possible.
        if (tile.isLand()) return leaveTransport(null);

        // Collect neighbouring land tiles, get off if one has our settlement
        List<Tile> tiles = new ArrayList<>();
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
        Tile safe = tiles.get(0);
        Tile best = null;
        int bestTurns = Unit.MANY_TURNS;
        Settlement settlement = null;
        for (Tile t : tiles) {
            if (settlement == null || t.isConnectedTo(settlement.getTile())) {
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
    @Override
    public boolean leaveTransport(Direction direction) {
        if (!unit.isOnCarrier()) return false;
        final Unit carrier = unit.getCarrier();
        boolean result = (direction == null)
            ? (AIMessage.askDisembark(this)
                && unit.getLocation() == carrier.getLocation())
            : move(direction);

        if (result) {
            requestLocalRearrange();
            dropTransport();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean joinTransport(Unit carrier, Direction direction) {
        AIUnit aiCarrier = getAIMain().getAIUnit(carrier);
        if (aiCarrier == null) return false;
        boolean result = AIMessage.askEmbark(aiCarrier, unit, direction)
            && unit.getLocation() == carrier;

        if (result) {
            requestLocalRearrange();
            takeTransport();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
        dropTransport();
        AIPlayer aiOwner = getAIOwner();
        if (aiOwner != null) {
            aiOwner.removeAIUnit(this);
        } else {
            // This happens with missionaries, and legitimately when a unit
            // changes owner.  FIXME: cleanup.
            logger.warning("Disposing of " + getId() + " but owner is null!");
        }
        if (mission != null) {
            this.mission.dispose();
            this.mission = null;
        }
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
        if (unit.isUninitialized()) {
            xr.nextTag(); // Move off the opening <AIUnit> tag
            throw new XMLStreamException("AIUnit for uninitialized Unit: "
                + unit.getId());
        }
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
    @Override
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
