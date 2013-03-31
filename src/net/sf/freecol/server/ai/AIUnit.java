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

package net.sf.freecol.server.ai;

import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.EquipmentType;
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
import net.sf.freecol.common.model.Unit.Role;
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
 * The method {@link #doMission()} is called once each turn, by
 * {@link AIPlayer#startWorking()}, to perform the assigned
 * <code>Mission</code>.  Most of the methods in this class just
 * delegates the call to that mission.
 *
 * @see Mission
 */
public class AIUnit extends AIObject implements Transportable {

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
     * The <code>AIUnit</code> which has this <code>Transportable</code> in
     * its transport list.
     */
    private AIUnit transport;


    /**
     * Creates a new uninitialized <code>AIUnit</code>.
     *
     * @param aiMain The main AI-object.
     * @param id The identifier for the uninitialized unit.
     */
    public AIUnit(AIMain aiMain, String id) {
        super(aiMain, id);

        this.unit = null;
        this.mission = null;
        this.goal = null;
        this.dynamicPriority = 0;
        this.transport = null;
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
     * @param in The input stream containing the XML.
     * @exception XMLStreamException if a problem was encountered
     *     during parsing.
     */
    public AIUnit(AIMain aiMain, XMLStreamReader in) throws XMLStreamException {
        super(aiMain, in);

        uninitialized = getUnit() == null;
    }


    /**
     * Gets the <code>Unit</code> this <code>AIUnit</code> controls.
     *
     * @return The <code>Unit</code>.
     */
    public Unit getUnit() {
        return unit;
    }

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
     * Checks if this unit has been assigned a mission.
     *
     * @return True if this unit has a mission.
     */
    public boolean hasMission() {
        return mission != null;
    }

    /**
     * Gets the mission this unit has been assigned.
     *
     * @return The <code>Mission</code>.
     */
    public Mission getMission() {
        return mission;
    }

    /**
     * Assigns a mission to unit. The dynamic priority is reset.
     * Do not call setMission(null), use abortMission above.
     *
     * @param mission The new <code>Mission</code>.
     */
    public void setMission(Mission mission) {
        if (this.mission == mission) {
            return;
        } else if (this.mission == null) {
            if (!mission.isOneTime()) {
                logger.fine("Replacing null mission with " + mission);
            }
        } else {
            abortMission("replaced");
        }
        this.mission = mission;
        this.dynamicPriority = 0;
    }

    /**
     * Aborts a mission.  Always use this instead of setMission(null),
     * and provide a useful reason so that AI mission thrashing can be
     * tracked down.
     *
     * @param why A string describing why the mission is to be aborted
     *     (e.g. "invalid").
     */
    public void abortMission(String why) {
        if (mission != null) {
            if (!mission.isOneTime()) {
                logger.fine("Mission-ABORT(" + why + "): " + mission);
            }
            removeTransport(why);
            mission.dispose();
            mission = null;
            this.dynamicPriority = 0;
        }
    }

    /**
     * Gets the goal of this AI unit.
     *
     * @return The goal of this AI unit.
     */
    public Goal getGoal() {
        return goal;
    }

    /**
     * Sets the goal of this AI unit.
     *
     * @param goal The new <code>Goal</code>.
     */
    public void setGoal(Goal goal) {
        this.goal = goal;
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
     * If this unit is scheduled for transport, deschedule.
     *
     * @param reason A reason why the unit is to be removed.
     */
    private void removeTransport(String reason) {
        AIUnit transport = getTransport();
        if (transport != null) {
            Mission m = transport.getMission();
            if (m instanceof TransportMission) {
                ((TransportMission)m).removeTransportable(this, reason);
            }
        }
        setTransport(null, reason);
    }

    /**
     * If this unit has a transport, retarget.
     */
    private void retargetTransport() {
        AIUnit transport = getTransport();
        if (transport != null) {
            Mission m = transport.getMission();
            if (m instanceof TransportMission) {
                ((TransportMission)m).retargetTransportable(this);
            }
        }
    }

    /**
     * Performs the mission this unit has been assigned.
     */
    public void doMission() {
        if (mission != null && mission.isValid()) {
            mission.doMission();
        }
    }

    /**
     * Equips this AI unit for a particular role.
     *
     * The unit must be at a location where the required goods are available
     * (possibly requiring a purchase, which may fail due to lack of gold
     * or boycotts in effect).
     *
     * When multiple equipment types are needed, try them all --- so for
     * example, if a request is made to equip a unit in Europe as a dragoon
     * but muskets are boycotted, it may still acquire the horses and end
     * up as a scout.
     *
     * TODO: remove cheat.
     *
     * @param r The <code>Role</code> to adopt.
     * @param cheat Cheat goods purchase in Europe (but *not* boycotts).
     * @return True if the role change was successful.
     */
    public boolean equipForRole(Role r, boolean cheat) {
        final Specification spec = getSpecification();
        final Player player = unit.getOwner();
        Location loc = unit.getLocation();
        Europe europe = (unit.isInEurope()) ? player.getEurope() : null;
        Settlement settlement;
        if (loc == null
            || ((settlement = loc.getSettlement()) == null
                && europe == null)) return false;

        eq: for (EquipmentType e : r.getRoleEquipment(spec)) {
            if (!unit.canBeEquippedWith(e)) {
                // Weed out native/colonial-specific equipment types.
                continue;
            }
            // Check that this should succeed before querying server.
            if (europe != null) {
                for (AbstractGoods ag : e.getRequiredGoods()) {
                    if (player.getMarket().getArrears(ag.getType()) > 0) {
                        continue eq; // Boycott prevents purchase.
                    }
                    int cost = player.getMarket().getBidPrice(ag.getType(),
                                                              ag.getAmount());
                    if (!player.checkGold(cost)) {
                        if (cheat) {
                            player.logCheat("minted " + cost
                                + " gold to equip " + unit + " for " + r);
                            player.modifyGold(cost);
                        } else {
                            continue eq;
                        }
                    }
                }
            } else {
                if (!settlement.canBuildEquipment(e)) continue eq;
            }
            // Should now only fail due to comms lossage.
            AIMessage.askEquipUnit(this, e, 1);
        }
        return unit.getRole() == r;
    }

    /**
     * Moves this AI unit.
     *
     * @param direction The <code>Direction</code> to move.
     * @return True if the move succeeded.
     */
    public boolean move(Direction direction) {
        Tile start = unit.getTile();
        return AIMessage.askMove(this, direction)
            && unit.getTile() != start;
    }

    /**
     * Takes this unit one step along a path.
     *
     * @param path The path to follow.
     * @return True if the step succeeds.
     */
    public boolean stepPath(PathNode path) {
        return (unit.isOnCarrier() && !path.isOnCarrier())
            ? leaveTransport(path.getDirection())
            : move(path.getDirection());
    }


    // Interface Transportable

    /**
     * Gets the number of cargo slots taken by this AI unit.
     *
     * @return The number of cargo slots taken.
     */
    public int getSpaceTaken() {
        return (getUnit() == null) ? 0 : getUnit().getSpaceTaken();
    }

    /**
     * Returns the source for this <code>Transportable</code>. This is
     * normally the location of the {@link #getTransportLocatable locatable}.
     *
     * @return The source for this <code>Transportable</code>.
     */
    public Location getTransportSource() {
        return (getUnit() == null || getUnit().isDisposed()) ? null
            : getUnit().getLocation();
    }

    /**
     * Returns the destination for this <code>Transportable</code>.
     * This can either be the target
     * {@link net.sf.freecol.common.model.Tile} of the transport or
     * the target for the entire <code>Transportable</code>'s
     * mission.  The target for the transport is determined by
     * {@link TransportMission} in the latter case.
     *
     * @return The destination for this <code>Transportable</code>.
     */
    public Location getTransportDestination() {
        return (getUnit() == null || getUnit().isDisposed() || !hasMission())
            ? null
            : mission.getTransportDestination();
    }

    /**
     * Gets the priority of transporting this <code>Transportable</code> to
     * it's destination.
     *
     * @return The priority of the transport.
     */
    public int getTransportPriority() {
        if (hasMission()) {
            return mission.getTransportPriority() + dynamicPriority;
        } else {
            return 0;
        }
    }

    /**
     * Sets the priority of getting the goods to the {@link
     * #getTransportDestination}.
     *
     * @param transportPriority The priority.
     */
    public void setTransportPriority(int transportPriority) {
        if (hasMission()) {
            dynamicPriority = transportPriority;
        }
    }

    /**
     * Increases the transport priority of this <code>Transportable</code>.
     * This method gets called every turn the <code>Transportable</code> have
     * not been put on a carrier's transport list.
     */
    public void increaseTransportPriority() {
        if (hasMission()) {
            ++dynamicPriority;
        }
    }

    /**
     * Gets the <code>Locatable</code> which should be transported.
     *
     * @return The <code>Locatable</code>.
     */
    public Locatable getTransportLocatable() {
        return unit;
    }

    /**
     * Gets the carrier responsible for transporting this
     * <code>Transportable</code>.
     *
     * @return The <code>AIUnit</code> which has this
     *         <code>Transportable</code> in it's transport list. This
     *         <code>Transportable</code> has not been scheduled for transport
     *         if this value is <code>null</code>.
     *
     */
    public AIUnit getTransport() {
        return transport;
    }

    /**
     * Sets the carrier responsible for transporting this
     * <code>Transportable</code>.
     *
     * @param transport The <code>AIUnit</code> which has this
     *            <code>Transportable</code> in it's transport list. This
     *            <code>Transportable</code> has not been scheduled for
     *            transport if this value is <code>null</code>.
     * @param reason A reason for changing the transport.
     */
    public void setTransport(AIUnit transport, String reason) {
        if (this.transport != transport) {
            logger.finest("setTransport " + this + " on " + transport
                + ": " + reason);
        }
        this.transport = transport;
    }

    /**
     * Aborts the given <code>Wish</code>.
     *
     * @param w The <code>Wish</code> to be aborted.
     */
    public void abortWish(Wish w) {
        if (mission instanceof WishRealizationMission) {
            // TODO: should we use setMission and dispose the mission as well?
            mission = null;
            dynamicPriority = 0;
        }
        if (w.getTransportable() == this) {
            w.dispose();
        }
    }

    /**
     * An AI unit leaves a ship.
     * Fulfills a wish if possible.
     *
     * @param direction The <code>Direction</code> to move, if any.
     * @return True if the unit is unloaded.
     */
    public boolean leaveTransport(Direction direction) {
        if (!unit.isOnCarrier()) return false;
        final Unit carrier = unit.getCarrier();
        boolean result = (direction != null) ? move(direction)
            : AIMessage.askDisembark(this)
            && unit.getLocation() == carrier.getTile();

        if (result) {
            Colony colony = unit.getColony();
            if (colony != null) {
                colony.firePropertyChange(Colony.REARRANGE_WORKERS,
                                          true, false);
            }
            removeTransport("disembarked");
        }
        return result;
    }

    /**
     * An AI unit joins a ship.
     *
     * @param carrier The carrier <code>Unit</code> to join.
     * @param direction The <code>Direction</code> to move, if any.
     * @return True if the unit is loaded.
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

            AIPlayer owner = getAIOwner();
            if (owner instanceof EuropeanAIPlayer) {
                if (!((EuropeanAIPlayer)owner).claimTransportable(this, old)) {
                    logger.warning("Could not claim transportable from " + old
                        + ": " + this);
                }
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public boolean carriableBy(Unit carrier) {
        return carrier.couldCarry(unit);
    }


    // Override AIObject

    /**
     * Disposes this object and any attached mission.
     */
    @Override
    public void dispose() {
        AIPlayer aiOwner = getAIOwner();
        if (aiOwner != null) aiOwner.removeAIUnit(this);
        abortMission("AIUnit-disposed");
        setTransport(null, "disposing");
        super.dispose();
    }

    /**
     * Checks the integrity of this AIUnit.
     *
     * @return True if the unit is intact.
     */
    @Override
    public boolean checkIntegrity() {
        return super.checkIntegrity()
            && unit != null && !unit.isDisposed();
    }


    // Serialization

    private static final String TRANSPORT_TAG = "transport";
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
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);

        if (transport != null) {
            Unit u = transport.getUnit();
            if (u == null) {
                logger.warning("transport.getUnit() == null");
            } else if (transport.isDisposed()) {
                logger.warning("broken reference to transport");
            } else {
                writeAttribute(out, TRANSPORT_TAG, u);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        super.writeChildren(out);

        if (mission != null && !mission.isOneTime()) {
            String reason = mission.invalidReason();
            if (reason != null) {
                abortMission(reason);
            } else {
                mission.toXML(out);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);

        final AIMain aiMain = getAIMain();

        unit = getAttribute(in, ID_ATTRIBUTE_TAG, aiMain.getGame(),
                            Unit.class, (Unit)null);
        if (unit == null) {
            throw new IllegalStateException("Not a Unit: " + currentTag(in));
        }
        
        if (hasAttribute(in, TRANSPORT_TAG)) {
            transport = getAttribute(in, TRANSPORT_TAG,
                                     AIUnit.class, (AIUnit)null);
            if (transport == null) {
                transport = new AIUnit(aiMain,
                    getAttribute(in, TRANSPORT_TAG, (String)null));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(XMLStreamReader in) throws XMLStreamException {
        super.readChildren(in);

        if (getUnit() != null) uninitialized = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        final AIMain aiMain = getAIMain();
        final String tag = in.getLocalName();

        mission = null;
        if (BuildColonyMission.getXMLElementTagName().equals(tag)) {
            mission = new BuildColonyMission(aiMain, this, in);

        } else if (CashInTreasureTrainMission.getXMLElementTagName().equals(tag)) {
            mission = new CashInTreasureTrainMission(aiMain, this, in);

        } else if (DefendSettlementMission.getXMLElementTagName().equals(tag)) {
            mission = new DefendSettlementMission(aiMain, this, in);

        } else if (IdleAtSettlementMission.getXMLElementTagName().equals(tag)
            // @compat 0.10.5
            || IDLE_AT_COLONY_MISSION_TAG.equals(tag)
            // end @compat
                   ) {
            mission = new IdleAtSettlementMission(aiMain, this, in);

        } else if (IndianBringGiftMission.getXMLElementTagName().equals(tag)) {
            mission = new IndianBringGiftMission(aiMain, this, in);

        } else if (IndianDemandMission.getXMLElementTagName().equals(tag)) {
            mission = new IndianDemandMission(aiMain, this, in);

        } else if (MissionaryMission.getXMLElementTagName().equals(tag)) {
            mission = new MissionaryMission(aiMain, this, in);

        } else if (PioneeringMission.getXMLElementTagName().equals(tag)
            // @compat 0.10.3
            || TILE_IMPROVEMENT_PLAN_MISSION_TAG.equals(tag)
            // end @compat
                   ) {
            mission = new PioneeringMission(aiMain, this, in);

        } else if (PrivateerMission.getXMLElementTagName().equals(tag)) {
            mission = new PrivateerMission(aiMain, this, in);

        } else if (ScoutingMission.getXMLElementTagName().equals(tag)) {
            mission = new ScoutingMission(aiMain, this, in);

        } else if (TransportMission.getXMLElementTagName().equals(tag)) {
            mission = new TransportMission(aiMain, this, in);

        } else if (UnitSeekAndDestroyMission.getXMLElementTagName().equals(tag)) {
            mission = new UnitSeekAndDestroyMission(aiMain, this, in);

        } else if (UnitWanderHostileMission.getXMLElementTagName().equals(tag)) {
            mission = new UnitWanderHostileMission(aiMain, this, in);

        } else if (UnitWanderMission.getXMLElementTagName().equals(tag)) {
            mission = new UnitWanderMission(aiMain, this, in);

        } else if (WishRealizationMission.getXMLElementTagName().equals(tag)) {
            mission = new WishRealizationMission(aiMain, this, in);

        } else if (WorkInsideColonyMission.getXMLElementTagName().equals(tag)) {
            mission = new WorkInsideColonyMission(aiMain, this, in);

        } else {
            super.readChild(in);
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
     * Gets the tag name of the root element representing this object.
     *
     * @return "aiUnit"
     */
    public static String getXMLElementTagName() {
        return "aiUnit";
    }
}
