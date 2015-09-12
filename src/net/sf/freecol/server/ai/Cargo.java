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
import java.util.Locale;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Locatable;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.util.LogBuilder;


/**
 * An class describing the action needed to make progress in a
 * transportation action for a specific transportable.
 */
public class Cargo {

    private static final Logger logger = Logger.getLogger(Cargo.class.getName());

    /** Abandon cargo after three blockages. */
    private static final int MAX_TRY = 3;

    /** The actions to perform at the target. */
    public static enum CargoMode {
        LOAD,       // Go to target and load transportable
        UNLOAD,     // Go to target and unload transportable
        PICKUP,     // Go to drop node target, transportable unit to embark
        DROPOFF,    // Go to drop node target, transportable unit to disembark
        DUMP;       // Just dump this transportable at the next opportunity

        public boolean isCollection() {
            return this == LOAD || this == PICKUP;
        }
    }

    /**
     * Container for a plan for a transportable to get to TWAIT where
     * a carrier collects it from CWAIT (may equal TWAIT) and takes it
     * to CDST, whence the transportable goes to TDST (may equal
     * CDST).
     */
    public static class CargoPlan {

        /** The key locations along the path taken by cargo and carrier. */
        public Location twait, cwait, cdst, tdst;

        /** Expected total duration of this plan when it is initialized. */
        public int turns;

        /** Current state of the plan. */
        public CargoMode mode;

        /** Is the destination a fallback destination? */
        public boolean fallback;

        /**
         * Plan the transport of a transportable with a given carrier.
         *
         * @param t The <code>TransportableAIObject</code> to deliver.
         * @param carrier The carrier <code>Unit</code> to use.
         * @param destination The destination <code>Location</code> to take the
         *     transportable to, using the transport destination if null.
         * @param allowFallback Allow a fallback plan that does not complete
         *     the transport but does at least improve matters.
         * @return Null on success, an error message on failure.
         */
        public String initialize(TransportableAIObject t, Unit carrier,
                                 Location destination, boolean allowFallback) {
            // Do some sanity checking
            if (t.isDisposed()) {
                return "invalid-disposed";
            } else if (carrier == null) {
                return "invalid-null-carrier";
            } else if (destination == null
                && (destination = t.getTransportDestination()) == null) {
                return "invalid-null-destination";
            }
            this.tdst = Location.upLoc(destination);
            final Location src = t.getLocation();
            final boolean carrying = src == carrier;
            if (!carrying && src instanceof Unit) {
                // FIXME: drop this and allow multi-stage plans?
                return "invalid-collected-elsewhere";
            }

            // Get the path to the destination, possibly allowing fallback
            // to a destination that at least improves matters.
            PathNode deliver = t.getDeliveryPath(carrier, tdst);
            fallback = false;
            if (deliver == null && allowFallback) {
                deliver = t.getIntermediatePath(carrier, tdst);
                fallback = true;
            }
            if (deliver == null) {
                return "no-deliver " + t
                    + "/" + carrier.toShortString()
                    + " -> " + tdst.toShortString();
            }

            // Where is the transportable collected?  At the first
            // path node where it is on the carrier.
            PathNode pick = deliver.getCarrierMove();
            if (pick == null) return "invalid-transport-not-needed";
            // The pickup node determines the c/twait locations.
            if (carrying) {
                this.twait = this.cwait = null;
            } else {
                this.cwait = Location.upLoc(pick.getLocation());
                // If there is a previous non-carrier move on the delivery
                // path, that is where the transportable should wait.
                // This will be true for units moving directly from land
                // to a naval carrier, but usually false when collection
                // occurs in a colony (as for goods).
                PathNode prev = (pick.previous == null) ? pick : pick.previous;
                this.twait = Location.upLoc(prev.getLocation());
            }

            // Can the carrier reach the pickup point?  If already
            // carrying this is obviously moot.
            PathNode collect = null;
            if (!carrying
                && (collect = carrier.findPath(this.cwait)) == null) {
                return "no-collect " + t
                    + "/" + carrier.toShortString()
                    + " at " + this.cwait.toShortString();
            }
                
            // Where is the transportable dropped?  At the drop node,
            // or at its predecessor from the carrier point of view.
            PathNode drop = pick.getTransportDropNode();
            if (drop == null || drop.previous == null) {
                throw new IllegalStateException("Cargo failure " + t
                    + " " + deliver.fullPathToString()
                    + " " + pick.fullPathToString()
                    + " " + drop);
            }
            this.cdst = Location.upLoc(drop.previous.getLocation());

            // The transportable ends up at the end of the delivery path.
            this.tdst = Location.upLoc(deliver.getLastNode().getLocation());

            // Total turns is just that of the delivery path if the
            // transportable has been collected.  Otherwise, it is the
            // maximum of the turns for the transportable and carrier to
            // reach the collection point, plus the turns from there to
            // the destination.
            //
            // The mode depends whether the carrier and transportable
            // have the same terminal points.
            if (carrying) {
                this.turns = deliver.getTotalTurns();
                this.mode = (this.cdst instanceof Europe
                    || this.cdst == this.tdst) ? CargoMode.UNLOAD
                    : CargoMode.DROPOFF;
            } else {
                this.turns = Math.max(pick.getTurns(), collect.getTotalTurns())
                    + pick.getTotalTurns();
                this.mode = (this.cwait instanceof Europe
                    || this.cwait == this.twait) ? CargoMode.LOAD
                    : CargoMode.PICKUP;
            }
            return null;
        }
    }

    /** The AI object to be transported. */
    private TransportableAIObject transportable;

    /** The carrier that is providing transportation. */
    private Unit carrier;

    /** Counter for failed tries. */
    private int tries;

    /** Space left on the carrier. */
    private int spaceLeft;

    /** Wrap location for cargoes of the same destination. */
    private List<Cargo> wrapped;

    /** The plan to execute the transport. */
    private CargoPlan plan;


    /**
     * Create a new cargo.
     *
     * @param transportable The <code>TransportableAIObject</code>
     *     to transport.
     * @param carrier The carrier <code>Unit</code>.
     * @param plan The <code>CargoPlan</code> to perform.
     */
    private Cargo(TransportableAIObject transportable, Unit carrier,
                  CargoPlan plan) {
        this.transportable = transportable;
        this.carrier = carrier;
        this.tries = 0;
        this.spaceLeft = carrier.getSpaceLeft();
        this.wrapped = null;
        this.plan = plan;
    }

    /**
     * Create a new cargo from a stream.
     *
     * @param aiMain The <code>AIMain</code> root.
     * @param xr The input stream containing the XML.
     * @exception XMLStreamException if a problem was encountered
     *     during parsing.
     */
    public Cargo(AIMain aiMain, FreeColXMLReader xr) throws XMLStreamException {
        readFromXML(aiMain, xr);
    }


    /**
     * Initialize this cargo.
     *
     * @param destination The destination <code>Location</code> to take
     *     the transportable to, defaulting to the transport destination
     *     if null.
     * @param allowFallback Allow a fallback transport.
     * @return A reason the planning failed, null if it succeeded.
     */
    private String initialize(Location destination, boolean allowFallback) {
        return this.plan.initialize(this.transportable, this.carrier,
                                    destination, allowFallback);
    }

    /**
     * Update the current delivery at the current fallback tolerance.
     *
     * @return A reason the planning failed, null if it succeeded.
     */
    public String update() {
        return this.plan.initialize(this.transportable, this.carrier, null,
                                    this.plan.fallback);
    }

    /**
     * Make a new cargo with given transportable and carrier.
     *
     * @param t The <code>TransportableAIObject</code> to transport.
     * @param carrier The carrier <code>Unit</code> to perform the transport.
     * @return The new <code>Cargo</code>.
     * @exception FreeColException on failure of the planning stage.
     */
    public static Cargo newCargo(TransportableAIObject t, Unit carrier)
        throws FreeColException {
        return newCargo(t, carrier, t.getTransportDestination(), true);
    }

    /**
     * Make a new cargo with given transportable, carrier and explicit
     * destination and fallback state.
     *
     * @param t The <code>TransportableAIObject</code> to transport.
     * @param carrier The carrier <code>Unit</code> to perform the transport.
     * @param destination The destination <code>Location</code> for
     *     the transportable.
     * @param allowFallback Allow a fallback destination.
     * @return The new <code>Cargo</code>.
     * @exception FreeColException on failure of the planning stage.
     */
    public static Cargo newCargo(TransportableAIObject t, Unit carrier,
                                 Location destination, boolean allowFallback)
        throws FreeColException {
        Cargo cargo = new Cargo(t, carrier, new CargoPlan());
        String reason = cargo.plan.initialize(t, carrier, destination,
                                              allowFallback);
        if (reason != null) throw new FreeColException(reason);
        return cargo;
    }

    /**
     * Reset this cargo to dump to the nearest available location.
     *
     * @return A reason for failing to reset, or null on succes.
     */
    public String dump() {
        if (!isCarried()) return "not-carried";
        PathNode path = carrier.getTrivialPath();
        if (path == null) return "no-trivial-path";
        String reason = initialize(path.getLastNode().getLocation(), false);
        if (reason != null) return reason;
        this.plan.mode = CargoMode.DUMP;
        return null;
    }

    /**
     * Get the transportable.
     *
     * @return The <code>TransportableAIObject</code> to transport.
     */
    public TransportableAIObject getTransportable() {
        return transportable;
    }

    public Unit getCarrier() {
        return carrier;
    }

    public int getTries() {
        return this.tries;
    }

    public int getSpaceLeft() {
        return spaceLeft;
    }

    public void setSpaceLeft(int spaceLeft) {
        this.spaceLeft = spaceLeft;
    }

    public boolean isValid() {
        return plan != null && plan.mode != null;
    }

    public CargoMode getMode() {
        return plan.mode;
    }

    public String getModeString() {
        CargoMode mode = getMode();
        return (mode == null) ? "null"
            : mode.toString().toLowerCase(Locale.US);
    }

    public int getTurns() {
        return plan.turns;
    }

    public boolean isFallback() {
        return plan.fallback;
    }

    public Location getTransportTarget() {
        return (getMode().isCollection()) ? plan.twait : plan.tdst;
    }

    public Location getCarrierTarget() {
        return (getMode().isCollection()) ? plan.cwait : plan.cdst;
    }

    public void clear() {
        this.transportable = null;
        this.carrier = null;
        this.plan.mode = null;
    }

    /**
     * Is the transportable on board the carrier?
     *
     * @return True if the transportable is being carried by the carrier.
     */
    public boolean isCarried() {
        return transportable != null
            && transportable.getLocation() == carrier;
    }
        
    /**
     * Is this cargo collectable?  That is, is it and the carrier
     * at their collection points, and in a collectable mode.
     *
     * @return True if the cargo can be collected.
     */
    public boolean isCollectable() {
        if (!getMode().isCollection() || isCarried()) return false;
        return Map.isSameLocation(plan.twait, transportable.getLocation())
            && Map.isSameLocation(plan.cwait, carrier.getLocation());
    }
            
    /**
     * Is this cargo deliverable?  That is, has it arrived at the target
     * on board the carrier in a deliverable mode.
     *
     * @return True if the cargo can be delivered to the target.
     */
    public boolean isDeliverable() {
        if (getMode().isCollection() || !isCarried()) return false;
        return Map.isSameLocation(plan.cdst, carrier.getLocation());
    }

    /**
     * Is this cargo delivered, or otherwise removed?  That is, is the
     * cargo not on board the carrier in a deliverable mode?
     *
     * @return True if the cargo has been delivered.
     */
    public boolean isDelivered() {
        return !getMode().isCollection() && !isCarried();
    }

    /**
     * Does this cargo have a potential delivery path?
     *
     * @return True if the carrier can deliver the cargo.
     */
    public boolean hasPath() {
        return carrier.findPath(getCarrierTarget()) != null;
    }

    /**
     * Get the movement direction to join the carrier.
     *
     * @return The <code>Direction</code> to join by.
     */
    public Direction getJoinDirection() {
        return (carrier.isInEurope() || plan.cwait == plan.twait) ? null
            : carrier.getGame().getMap().getDirection(plan.twait.getTile(),
                                                      plan.cwait.getTile());
    }

    /**
     * Get the movement direction to leave the carrier.
     *
     * @return The <code>Direction</code> to leave by.
     */
    public Direction getLeaveDirection() {
        if (!carrier.hasTile() || plan.cdst == plan.tdst) return null;
        TransportableAIObject t = getTransportable();
        PathNode path = t.getDeliveryPath(getCarrier(), plan.tdst);
        return (path == null || path.next == null) ? null
            : path.next.getDirection();
    }

    /**
     * How much space would be needed to add this transportable?
     *
     * @return The extra space required.
     */
    public int getNewSpace() {
        if (!isValid()) return 0;
        int ret = 0;
        ret += (getMode().isCollection()) ? getTransportable().getSpaceTaken()
            : -getTransportable().getSpaceTaken();
        if (hasWrapped()) {
            ret += wrapped.stream().mapToInt(c -> c.getNewSpace()).sum();
        }
        return ret;
    }

    /**
     * Does this cargo wrap others?
     *
     * @return True if wrapped transportables are present.
     */
    public boolean hasWrapped() {
        return wrapped != null;
    }

    /**
     * TransportableAIObjects can be `wrapped' if they have the
     * same target and advancing them reduces the space on the carrier.
     *
     * @param other The other <code>TransportableAIObject</code>
     *     to consider.
     * @return True if the transportables can be wrapped.
     */
    public boolean couldWrap(Cargo other) {
        return getCarrierTarget() == other.getCarrierTarget()
            && getNewSpace() < 0 && other.getNewSpace() < 0;
    }

    /**
     * Wrap a Cargo into this one.
     *
     * @param other The other <code>Cargo</code> to wrap.
     */
    public void wrap(Cargo other) {
        if (other == this) {
            throw new IllegalStateException("Autowrap at" + this);
        }
        if (wrapped == null) wrapped = new ArrayList<>();
        wrapped.add(other);
    }

    /**
     * Unwrap this cargo.
     *
     * @return The cargoes that were wrapped.
     */
    public List<Cargo> unwrap() {
        if (wrapped == null) {
            throw new IllegalStateException("Bogus unwrap " + this);
        }
        List<Cargo> result = wrapped;
        wrapped = null;
        return result;
    }
            
    /**
     * Should this <code>Cargo</code> be retried after encountering
     * a blockage?  For now, just tries three times.
     *
     * FIXME: be smarter.
     *
     * @return True if the <code>Cargo</code> should be retried.
     */
    public boolean retry() {
        return tries++ < MAX_TRY;
    }

    /**
     * Reset the tries counter.
     */
    public void resetTries() {
        this.tries = 0;
    }

    /**
     * Does this cargo involve trade with Europe in a given goods type?
     *
     * @param type The <code>GoodsType</code> to check.
     * @return True if this cargo is of the given type and to be
     *     collected or delivered to Europe.
     */
    public boolean isEuropeanTrade(GoodsType type) {
        return transportable instanceof AIGoods
            && ((AIGoods)transportable).getGoodsType() == type
            && getCarrierTarget() instanceof Europe;
    }

    /**
     * Check the integrity of this cargo.
     *
     * @param aiCarrier The <code>AIUnit</code> version of the carrier.
     * @return A reason for integrity failure, or null if none.
     */
    public String check(AIUnit aiCarrier) {
        if (transportable == null) {
            return "null transportable";
        } else if (transportable.isDisposed()) {
            return "disposed transportable";
        }
            
        Locatable l = transportable.getTransportLocatable();
        if (l == null) {
            return "null locatable: " + transportable;
        } else if (l instanceof FreeColGameObject
            && ((FreeColGameObject)l).isDisposed()) {
            return "locatable disposed";
        }
            
        Location tLoc = l.getLocation();
        if (tLoc instanceof Unit && tLoc != carrier) {
            return "carrier usurped"; // On another carrier!
        }
        return null;
    }

    /**
     * Can this cargo be queued at the given index in a list of cargoes?
     *
     * TODO: be smarter and break out of the loop if the cargo reaches
     * its delivery point.
     *
     * @param carrier The <code>Unit</code> to queue to.
     * @param index The queuing position to test.
     * @param cargoes A list of <code>Cargo</code>s.
     * @return True if there is space to add the cargo.
     */
    public boolean canQueueAt(Unit carrier, int index, List<Cargo> cargoes) {
        final int maxHolds = carrier.getCargoCapacity();
        final int newSpace = this.getNewSpace();
        Cargo tr = cargoes.get(index);
        for (int j = index; j < cargoes.size(); j++) {
            int holds = (j == 0) ? carrier.getCargoSpaceTaken()
                : maxHolds - cargoes.get(j-1).getSpaceLeft();
            holds += newSpace;
            if (holds < 0 || holds > maxHolds) return false;
        }
        return true;
    }

    /**
     * Abbreviated string representation for this cargo.
     *
     * @return A short descriptive string.
     */
    public String toShortString() {
        LogBuilder lb = new LogBuilder(32);
        lb.add(getModeString(), " ", transportable);
        Location lt = getTransportTarget();
        lb.add(" @ ", ((lt == null) ? "null" : lt.toShortString()));
        Location ct = getCarrierTarget();
        if (ct != lt) lb.add("/", ct.toShortString());
        return lb.toString();
    }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        LogBuilder lb = new LogBuilder(64);
        lb.add("[", transportable,
            " ", getModeString(),
            " ", getTurns(), "/", tries, " space=", spaceLeft,
            ((wrapped == null) ? "" : " wrap"));
        if (plan.twait != null && plan.cwait != null) {
            lb.add(" ", plan.twait.toShortString(),
                "/", plan.cwait.toShortString());
        }
        if (plan.cdst != null && plan.tdst != null) {
            lb.add("->", plan.cdst.toShortString(),
                "/", plan.tdst.toShortString());
        }
        lb.add(" ", plan.fallback, "]");
        return lb.toString();
    }            


    // Serialization
    // Cargo is not yet an AIObject or FreeColObject, but that may happen.

    private static final String CDST_TAG = "cdst";
    private static final String CWAIT_TAG = "cwait";
    private static final String CARRIER_TAG = "carrier";
    private static final String FALLBACK_TAG = "fallback";
    private static final String MODE_TAG = "mode";
    private static final String SPACELEFT_TAG = "space";
    private static final String TDST_TAG = "tdst";
    private static final String TRIES_TAG = "tries";
    private static final String TURNS_TAG = "turns";
    private static final String TWAIT_TAG = "twait";
    // Used to use TARGET_TAG = "target"


    public void toXML(FreeColXMLWriter xw) throws XMLStreamException {
        xw.writeStartElement(getXMLElementTagName());

        xw.writeAttribute(FreeColObject.ID_ATTRIBUTE_TAG,
                          (AIObject)getTransportable());

        xw.writeAttribute(CARRIER_TAG, getCarrier());

        xw.writeAttribute(TRIES_TAG, getTries());

        xw.writeAttribute(SPACELEFT_TAG, getSpaceLeft());

        if (plan.twait != null) {
            xw.writeLocationAttribute(TWAIT_TAG, plan.twait);
        }

        if (plan.cwait != null) {
            xw.writeLocationAttribute(CWAIT_TAG, plan.cwait);
        }

        if (plan.cdst != null) {
            xw.writeLocationAttribute(CDST_TAG, plan.cdst);
        }

        if (plan.tdst != null) {
            xw.writeLocationAttribute(TDST_TAG, plan.tdst);
        }

        xw.writeAttribute(TURNS_TAG, plan.turns);

        xw.writeAttribute(MODE_TAG, plan.mode);

        xw.writeAttribute(FALLBACK_TAG, plan.fallback);

        xw.writeEndElement();
    }

    public void readFromXML(AIMain aiMain, FreeColXMLReader xr) throws XMLStreamException {
        final Game game = aiMain.getGame();

        String tid = xr.readId();
        TransportableAIObject tao = null;
        if (tid != null) {
            AIObject aio = aiMain.getAIObject(tid);
            if (aio == null) {
                if (tid.startsWith(Unit.getXMLElementTagName())) {
                    tao = new AIUnit(aiMain, tid);
                } else if (tid.startsWith(AIGoods.getXMLElementTagName())) {
                    tao = new AIGoods(aiMain, tid);
                }
            } else {
                tao = (TransportableAIObject)aio;
            }
        }
        if (tao == null) {
            throw new XMLStreamException("Transportable expected: " + tid);
        }
        this.transportable = tao;

        this.carrier = xr.getAttribute(game, CARRIER_TAG,
                                       Unit.class, (Unit)null);

        this.tries = xr.getAttribute(TRIES_TAG, 0);
        
        this.spaceLeft = xr.getAttribute(SPACELEFT_TAG, -1);
            
        this.wrapped = null;

        this.plan = new CargoPlan();

        this.plan.twait = xr.getLocationAttribute(game, TWAIT_TAG, false);

        this.plan.cwait = xr.getLocationAttribute(game, CWAIT_TAG, false);

        this.plan.cdst = xr.getLocationAttribute(game, CDST_TAG, false);

        this.plan.tdst = xr.getLocationAttribute(game, TDST_TAG, false);
            
        this.plan.turns = xr.getAttribute(TURNS_TAG, -1);

        this.plan.mode = xr.getAttribute(MODE_TAG, 
                                         CargoMode.class, (CargoMode)null);

        this.plan.fallback = xr.getAttribute(FALLBACK_TAG, false);

        xr.closeTag(getXMLElementTagName());
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "cargo"
     */
    public static String getXMLElementTagName() {
        return "cargo";
    }
}
