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
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Locatable;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.server.ai.goal.Goal;
import net.sf.freecol.server.ai.mission.BuildColonyMission;
import net.sf.freecol.server.ai.mission.CashInTreasureTrainMission;
import net.sf.freecol.server.ai.mission.DefendSettlementMission;
import net.sf.freecol.server.ai.mission.IdleAtSettlementMission;
import net.sf.freecol.server.ai.mission.IndianBringGiftMission;
import net.sf.freecol.server.ai.mission.IndianDemandMission;
import net.sf.freecol.server.ai.mission.Mission;
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
 * <br>
 * <br>
 *
 * The method {@link #doMission(Connection)} is called once each turn, by
 * {@link AIPlayer#startWorking()}, to perform the assigned
 * <code>Mission</code>. Most of the methods in this class just delegates the
 * call to that mission.
 *
 * @see Mission
 */
public class AIUnit extends AIObject implements Transportable {

    private static final Logger logger = Logger.getLogger(AIUnit.class.getName());

    /**
     * The Unit this AIObject contains AI-information for.
     */
    private Unit unit;

    /**
     * The mission to which this AI unit has been assigned.
     */
    private Mission mission;

    /**
     * The goal this AIUnit belongs to, if one has been assigned.
     */
    private Goal goal = null;

    /**
     * The dynamic part of the transport priority.
     */
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

        unit = null;
        mission = null;
        goal = null;
        dynamicPriority = 0;
        transport = null;
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
        mission = new UnitWanderHostileMission(aiMain, this);
        uninitialized = getUnit() == null;
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
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public AIUnit(AIMain aiMain, XMLStreamReader in)
        throws XMLStreamException {
        super(aiMain, in);

        uninitialized = getUnit() == null;
    }


    /**
     * Disposes this object and any attached mission.
     */
    public void dispose() {
        abortMission("disposed");
        setTransport(null);
        super.dispose();
    }

    /**
     * Gets this AI object's identifier.
     *
     * @return The id of the unit.
     */
    public String getId() {
        if (unit == null) {
            logger.warning("Uninitialized AI unit");
            return null;
        }
        return unit.getId();
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
        return getAIMain().getAIPlayer(unit.getOwner());
    }

    /**
     * Convenience accessor for the owning player connection.
     *
     * @return The connection.
     */
    public Connection getConnection() {
        return getAIOwner().getConnection();
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
     * Checks if this unit has been assigned a mission.
     *
     * @return <code>true</code> if this unit has a mission.
     */
    public boolean hasMission() {
        return mission != null;
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
            logger.finest("Aborting old " + why
                + " " + mission.getClass().getName()
                + " for unit " + getUnit());
            mission.dispose();
            this.mission = null;
        }
    }

    /**
     * Assigns a mission to unit. The dynamic priority is reset.
     * Do not call setMission(null), use abortMission above.
     *
     * @param mission The new <code>Mission</code>.
     */
    public void setMission(Mission mission) {
        final Mission oldMission = this.mission;
        if (oldMission != null) {
            logger.finest("Replacing old " + oldMission.getClass().getName()
                + " with " + mission.getClass().getName()
                + " for unit " + getUnit());
            oldMission.dispose();
        }
        this.mission = mission;
        this.dynamicPriority = 0;
    }

    /**
     * Performs the mission this unit has been assigned.
     *
     * @param connection The <code>Connection</code> to use when communicating
     *            with the server.
     */
    public void doMission(Connection connection) {
        if (mission != null && mission.isValid()) {
            mission.doMission(connection);
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
        Europe europe = (loc instanceof Europe) ? (Europe)loc : null;
        Settlement settlement = loc.getSettlement();
        if (settlement == null && europe == null) return false;

        eq: for (EquipmentType e : r.getRoleEquipment(spec)) {
            if (!unit.canBeEquippedWith(e)) {
                // Weed out native/colonial-specific equipment types.
                continue;
            }
            // Check that this should succeed before querying server.
            if (europe != null) {
                for (AbstractGoods ag : e.getGoodsRequired()) {
                    if (player.getMarket().getArrears(ag.getType()) > 0) {
                        continue eq; // Boycott prevents purchase.
                    }
                    int cost = player.getMarket().getBidPrice(ag.getType(),
                                                              ag.getAmount());
                    if (!player.checkGold(cost)) {
                        if (cheat) {
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
     * Checks the integrity of this AIUnit.
     *
     * @return True if the unit is intact.
     */
    public boolean checkIntegrity() {
        return super.checkIntegrity()
            && unit != null && !unit.isDisposed();
    }


    // Transportable interface

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
     * This can either be the target {@link
     * net.sf.freecol.common.model.Tile} of the transport or the
     * target for the entire <code>Transportable</code>'s mission. The
     * target for the tansport is determined by {@link
     * TransportMission} in the latter case.
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
     *
     */
    public void setTransport(AIUnit transport) {
        if (this.transport == transport) return;
        AIUnit oldTransport = this.transport;
        this.transport = transport;

        if (oldTransport != null) {
            // Remove from old carrier:
            if (oldTransport.getMission() != null && oldTransport.getMission() instanceof TransportMission) {
                TransportMission tm = (TransportMission) oldTransport.getMission();
                if (tm.isOnTransportList(this)) {
                    tm.removeFromTransportList(this);
                }
            }
        }

        if (transport != null && transport.getMission() instanceof TransportMission
                && !((TransportMission) transport.getMission()).isOnTransportList(this)) {
            // Add to new carrier:
            ((TransportMission) transport.getMission()).addToTransportList(this);
        }
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

   
    // Serialization

    /**
     * Writes this object to an XML stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute(ID_ATTRIBUTE, getId());

        if (transport != null) {
            if (transport.getUnit() == null) {
                logger.warning("transport.getUnit() == null");
            } else if (getAIMain().getAIObject(transport.getId()) == null) {
                logger.warning("broken reference to transport");
            } else if (transport.getMission() != null
                && transport.getMission() instanceof TransportMission
                && !((TransportMission) transport.getMission()).isOnTransportList(this)) {
                logger.warning("We should not be on the transport list.");
            } else {
                out.writeAttribute("transport", transport.getUnit().getId());
            }
        }

        if (mission != null) {
            if (mission.isValid()) {
                mission.toXML(out);
            } else {
                logger.warning("AI unit with invalid mission " + mission
                    + ": " + this);
            }
        }

        out.writeEndElement();
    }

    /**
     * Reads information for this object from an XML stream.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException if there are any problems reading from the
     *             stream.
     */
    protected void readFromXMLImpl(XMLStreamReader in)
        throws XMLStreamException {
        final AIMain aiMain = getAIMain();
        String str, tag;

        str = in.getAttributeValue(null, ID_ATTRIBUTE);
        unit = (Unit)aiMain.getFreeColGameObject(str);
        if (unit == null) logger.warning("Could not find unit: " + str);

        str = in.getAttributeValue(null, "transport");
        if (str != null) {
            if ((transport = (AIUnit)aiMain.getAIObject(str)) == null) {
                transport = new AIUnit(aiMain, str);
            }
        } else {
            transport = null;
        }

        if (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            tag = in.getLocalName();
            if (tag.equals(BuildColonyMission.getXMLElementTagName())) {
                mission = new BuildColonyMission(aiMain, in);
            } else if (tag.equals(CashInTreasureTrainMission.getXMLElementTagName())) {
                mission = new CashInTreasureTrainMission(aiMain, in);
            } else if (tag.equals(DefendSettlementMission.getXMLElementTagName())) {
                mission = new DefendSettlementMission(aiMain, in);
            } else if (tag.equals(IdleAtSettlementMission.getXMLElementTagName())
                // @compat 0.10.5
                || tag.equals("idleAtColonyMission")
                // @end compatibility code
                ) {
                mission = new IdleAtSettlementMission(aiMain, in);
            } else if (tag.equals(IndianBringGiftMission.getXMLElementTagName())) {
                mission = new IndianBringGiftMission(aiMain, in);
            } else if (tag.equals(IndianDemandMission.getXMLElementTagName())) {
                mission = new IndianDemandMission(aiMain, in);
            } else if (tag.equals(PioneeringMission.getXMLElementTagName())
                // @compat 0.10.3
                || tag.equals("tileImprovementPlanMission")) {
                // @end compatibility code                
                mission = new PioneeringMission(aiMain, in);
            } else if (tag.equals(PrivateerMission.getXMLElementTagName())) {
                mission = new PrivateerMission(aiMain, in);
            } else if (tag.equals(ScoutingMission.getXMLElementTagName())) {
                mission = new ScoutingMission(aiMain, in);
            } else if (tag.equals(TransportMission.getXMLElementTagName())) {
                mission = new TransportMission(aiMain, in);
            } else if (tag.equals(UnitSeekAndDestroyMission.getXMLElementTagName())) {
                mission = new UnitSeekAndDestroyMission(aiMain, in);
            } else if (tag.equals(UnitWanderHostileMission.getXMLElementTagName())) {
                mission = new UnitWanderHostileMission(aiMain, in);
            } else if (tag.equals(UnitWanderMission.getXMLElementTagName())) {
                mission = new UnitWanderMission(aiMain, in);
            } else if (tag.equals(WishRealizationMission.getXMLElementTagName())) {
                mission = new WishRealizationMission(aiMain, in);
            } else if (tag.equals(WorkInsideColonyMission.getXMLElementTagName())) {
                mission = new WorkInsideColonyMission(aiMain, in);
            } else {
                mission = null;
                logger.warning("Could not find mission-class for: " + tag);
            }

            in.nextTag();
        }

        tag = in.getLocalName();
        if (!tag.equals(getXMLElementTagName())) {
            logger.warning("Expected end " + getXMLElementTagName()
                + " tag, received: " + tag);
        }
    }

    /**
     * {@inherit-doc}
     */
    @Override
    public String toString() {
        return "[AIUnit " + unit + " ]";
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "aiUnit"
     */
    public static String getXMLElementTagName() {
        return "aiUnit";
    }
}
