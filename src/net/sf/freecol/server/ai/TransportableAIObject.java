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

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Locatable;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.ai.mission.TransportMission;

import org.w3c.dom.Element;


/**
 * A single item in a carrier's transport list.  Any {@link Locatable}
 * which should be able to be transported by a carrier using the
 * {@link net.sf.freecol.server.ai.mission.TransportMission}, 
 * should extend this class.
 *
 * @see net.sf.freecol.server.ai.mission.TransportMission
 */
public abstract class TransportableAIObject extends ValuedAIObject {

    /**
     * The priority for a goods that are hitting the warehouse limit.
     */
    public static final int IMPORTANT_DELIVERY = 110;

    /**
     * The priority for goods that provide at least a full cargo load.
     */
    public static final int FULL_DELIVERY = 100;

    /**
     * The priority of tools intended for a Colony with none stored
     * at the present (and with no special needs).
     */
    public static final int TOOLS_FOR_COLONY_PRIORITY = 10;

    /**
     * The extra priority value added to the base value of
     * {@link #TOOLS_FOR_COLONY_PRIORITY}
     * for each ColonyTile needing a terrain improvement.
     */
    public static final int TOOLS_FOR_IMPROVEMENT = 10;

    /**
     * The extra priority value added to the base value of
     * {@link #TOOLS_FOR_COLONY_PRIORITY}
     * if a Pioneer is lacking tools
     */
    public static final int TOOLS_FOR_PIONEER = 90;

    /**
     * The extra priority value added to the base value of
     * {@link #TOOLS_FOR_COLONY_PRIORITY}
     * if a building is lacking tools. The number of tools
     * is also added to the total amount.
     */
    public static final int TOOLS_FOR_BUILDING = 100;

    /**
     * The <code>AIUnit</code> which has been allocated to transport
     * this object.
     */
    private AIUnit transport;


    /**
     * Create a new uninitialized transportable AI object.
     *
     * @param aiMain an <code>AIMain</code> value
     * @param id The object identifier.
     */
    public TransportableAIObject(AIMain aiMain, String id) {
        super(aiMain, id);

        this.transport = null;
    }

    /**
     * Creates a new transportable AI object from the given
     * XML-representation.
     *
     * @param aiMain The main AI-object.
     * @param element The root element for the XML-representation 
     *     of the object.
     */
    public TransportableAIObject(AIMain aiMain, Element element) {
        super(aiMain, element);
    }
    
    /**
     * Creates a new transportable AI object from the given
     * XML-representation.
     *
     * @param aiMain The main AI-object.
     * @param xr The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public TransportableAIObject(AIMain aiMain,
                                 FreeColXMLReader xr) throws XMLStreamException {
        super(aiMain, xr);
    }


    // Fundamental access

    /**
     * Gets the priority of transporting this object to its destination.
     *
     * @return The priority of the transport.
     */
    public int getTransportPriority() {
        return getValue();
    }

    /**
     * Sets the priority of getting this object to its destination.
     *
     * @param transportPriority The priority.
     */
    public void setTransportPriority(int transportPriority) {
        setValue(transportPriority);
    }

    /**
     * Increases the transport priority.
     */
    public final void incrementTransportPriority() {
        setValue(getValue() + 1);
    }

    /**
     * Gets the carrier responsible for transporting this object.
     *
     * @return The <code>AIUnit</code> which will transport this object.
     */
    public final AIUnit getTransport() {
        return this.transport;
    }

    /**
     * Sets the carrier responsible for transporting this object.
     *
     * @param transport The new carrier <code>AIUnit</code>.
     */
    public final void setTransport(AIUnit transport) {
        this.transport = transport;
    }


    // Public routines

    /**
     * Gets the number of cargo slots taken by this transportable.
     *
     * @return The number of cargo slots taken.
     */
    public int getSpaceTaken() {
        Locatable l = getTransportLocatable();
        return (l == null) ? 0 : l.getSpaceTaken();
    }

    /**
     * Drop the current transport, keeping the transport mission consistent.
     *
     * Public so AIPlayer.removeAIUnit can drop its responsibilities.
     *
     * @return True if the unit has no allocated transport.
     */
    public boolean dropTransport() {
        AIUnit transport = getTransport();
        if (transport != null) {
            if (!transport.isDisposed()
                && getLocation() != transport.getUnit()) {
                TransportMission tm
                    = transport.getMission(TransportMission.class);
                if (tm != null) tm.removeTransportable(this);
            }
            setTransport(null);
        }
        return getTransport() == null;
    }

    /**
     * Change the allocated transport for this transportable to a different
     * carrier unit.
     *
     * FIXME: partial attempt to maintain consistency of any carrier
     * TransportMission lists, and disembark from the old carrier if
     * possible.
     *
     * @param aiCarrier The new carrier <code>AIUnit</code>.
     * @return True if the transport was changed, false if the transportable
     *     was unable to disembark from the old carrier or unable to be
     *     added to the new carrier transport list.
     */
    public boolean changeTransport(AIUnit aiCarrier) {
        // Get off any current carrier unless it is the new one.
        Location now;
        Locatable l = getTransportLocatable();
        if (l != null && (now = l.getLocation()) instanceof Unit
            && !(aiCarrier != null && aiCarrier.getUnit() == now)) {
            if (!leaveTransport()) return false;
        }

        AIUnit old = getTransport();
        if (old != null) {
            if (old == aiCarrier) return true;

            TransportMission tm = old.getMission(TransportMission.class);
            if (tm != null) tm.removeTransportable(this);
        }
        setTransport(null);
        if (aiCarrier != null) {
            //TransportMission tm = aiCarrier.getMission(TransportMission.class);
            //if (tm != null) {
            //    if (!tm.requeueTransportable(this)) return false;
            //}
            setTransport(aiCarrier);
        }
        return true;
    }

    /**
     * Get the transportables location if any.
     *
     * @return The transportable <code>Location</code>.
     */
    public Location getLocation() {
        Locatable l = getTransportLocatable();
        return (l == null) ? null : l.getLocation();
    }


    // TransportableAIObject abstract routines

    /**
     * Gets the underlying locatable object which should be transported.
     *
     * @return The <code>Locatable</code>.
     */
    public abstract Locatable getTransportLocatable();

    /**
     * Get the source location for this transportable AI object.
     * This is normally the location of the
     * {@link #getTransportLocatable locatable}.
     *
     * @return The source <code>Location</code>.
     */
    public abstract Location getTransportSource();

    /**
     * Get the destination location for this transportable AI object.
     * This can be the target {@link net.sf.freecol.common.model.Tile}
     * of the transport or the target of the mission.
     *
     * @return The destination <code>Location</code>.
     */
    public abstract Location getTransportDestination();

    /**
     * Set the destination location for this transportable AI object.
     *
     * @param destination The destination <code>Location</code>.
     */
    public abstract void setTransportDestination(Location destination);

    /**
     * Get the path to deliver this transportable to its destination
     * with a given carrier.
     *
     * @param carrier The carrier <code>Unit</code> to use.
     * @param dst The destination <code>Location</code>, defaulting to the
     *     transport destination if null.
     * @return A path, or null if none found.
     */
    public abstract PathNode getDeliveryPath(Unit carrier, Location dst);

    /**
     * Get the path to make progress with this transport, for the
     * carrier to either collect or deliver the transportable, albeit
     * it need only improve the current situation rather than complete
     * the delivery to the destination.
     *
     * @param carrier The carrier <code>Unit</code> to use.
     * @param dst The destination <code>Location</code>, defaulting to the
     *     transport destination if null.
     * @return A path, or null if none found.
     */
    public abstract PathNode getIntermediatePath(Unit carrier, Location dst);

    /**
     * Can this transportable be carried by a given carrier unit?
     *
     * @param carrier The potential carrier <code>Unit</code>.
     * @return True if the unit can carry this transportable.
     */
    public abstract boolean carriableBy(Unit carrier);

    /**
     * This transportable can move now.  Useful for units that may or
     * may not have enough moves left to join or leave a carrier.
     *
     * @return True if the transportable can move.
     */
    public abstract boolean canMove();

    /**
     * This object leaves its current carrier unit by the most
     * suitable means.
     *
     * @return True if the object successfully left the carrier.
     */
    public abstract boolean leaveTransport();

    /**
     * This object leaves its current carrier unit.
     *
     * @param direction The <code>Direction</code> to leave in, null
     *     to leave in place.
     * @return True if the object successfully left the carrier.
     */
    public abstract boolean leaveTransport(Direction direction);

    /**
     * This object joins a carrier unit.
     *
     * @param carrier The carrier <code>Unit</code> to join.
     * @param direction The <code>Direction</code> to move, null to join
     *     a carrier in the same location.
     * @return True if the object has joined tha carrier.
     */
    public abstract boolean joinTransport(Unit carrier, Direction direction);

    /**
     * Is there a reason to invalidate transporting this object?
     *
     * @return A reason to abort transport, or null if none found.
     */
    public abstract String invalidReason();


    // Serialization

    private static final String TRANSPORT_TAG = "transport";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        if (transport != null) {
            Unit u = transport.getUnit();
            if (u != null && !u.isDisposed()) {
                xw.writeAttribute(TRANSPORT_TAG, u);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        final AIMain aiMain = getAIMain();

        transport = (xr.hasAttribute(TRANSPORT_TAG))
            ? xr.makeAIObject(aiMain, TRANSPORT_TAG,
                              AIUnit.class, (AIUnit)null, true)
            : null;
    }
}
