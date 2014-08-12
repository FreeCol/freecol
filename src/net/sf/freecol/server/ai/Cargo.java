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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsLocation;
import net.sf.freecol.common.model.Locatable;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.pathfinding.CostDecider;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.ai.AIColony;
import net.sf.freecol.server.ai.AIGoods;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIObject;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.ai.Cargo;
import net.sf.freecol.server.ai.EuropeanAIPlayer;
import net.sf.freecol.server.ai.GoodsWish;
import net.sf.freecol.server.ai.TransportableAIObject;
import net.sf.freecol.server.ai.Wish;
import net.sf.freecol.server.ai.WorkerWish;


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

    private TransportableAIObject transportable;
    private Unit carrier;
    private CargoMode mode;
    private Location target;
    private int turns;
    private int tries;
    private int spaceLeft;
    private List<Cargo> wrapped;


    /**
     * Creates the new Cargo.
     *
     * Defaults to DUMP mode.  Call setTarget to do something useful.
     *
     * @param transportable The <code>TransportableAIObject</code>
     *     to cargo.
     * @param carrier The carrier <code>Unit</code>.
     */
    public Cargo(TransportableAIObject transportable, Unit carrier) {
        this(transportable, carrier, null);
    }

    /**
     * Creates the new dumping Cargo to a given location.
     *
     * @param transportable The <code>TransportableAIObject</code>
     *     to transport.
     * @param carrier The carrier <code>Unit</code>.
     * @param target The target <code>Location</code>.
     */
    public Cargo(TransportableAIObject transportable, Unit carrier,
        Location target) {
        this(transportable, carrier, CargoMode.DUMP, target, -1, 0, -1);
    }

    /**
     * Creates the new Cargo with given mode and target.
     *
     * @param transportable The <code>TransportableAIObject</code>
     *     to transport.
     * @param carrier The carrier <code>Unit</code>.
     * @param mode The <code>CargoMode</code>.
     * @param target The target <code>Location</code>.
     * @param turns The turns required by the current path-estimate.
     * @param tries The tries taken to find a path.
     * @param spaceLeft The space left after this cargo action occurs.
     */
    public Cargo(TransportableAIObject transportable, Unit carrier,
                 CargoMode mode, Location target,
                 int turns, int tries, int spaceLeft) {
        this.transportable = transportable;
        this.carrier = carrier;
        this.mode = mode;
        this.target = target;
        this.turns = turns;
        this.tries = tries;
        this.spaceLeft = spaceLeft;
        this.wrapped = null;
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

    public TransportableAIObject getTransportable() {
        return transportable;
    }

    public Unit getCarrier() {
        return carrier;
    }

    public CargoMode getMode() {
        return mode;
    }

    public Location getTarget() {
        return target;
    }

    public int getTurns() {
        return turns;
    }

    public int getTries() {
        return tries;
    }

    public int getSpaceLeft() {
        return spaceLeft;
    }

    public void setSpaceLeft(int spaceLeft) {
        this.spaceLeft = spaceLeft;
    }

    public void clear() {
        this.transportable = null;
        this.carrier = null;
        this.mode = null;
        this.target = null;
    }

    public boolean isValid() {
        return this.mode != null;
    }

    /**
     * Is this cargo collectable?  That is, is it and the carrier
     * at the target but not on board the carrier, and in a
     * deliverable mode.
     *
     * @return True if the cargo can be collected from the target.
     */
    public boolean isCollectable() {
        if (!mode.isCollection()) return false;
        Location loc = transportable.getLocation();
        return Map.isSameLocation(loc, target)
            && loc != carrier
            && Map.isSameLocation(carrier.getLocation(), target);
    }
            
    /**
     * Is this cargo deliverable?  That is, has it arrived at the target
     * on board the carrier in a deliverable mode.
     *
     * @return True if the cargo can be delivered to the target.
     */
    public boolean isDeliverable() {
        if (mode.isCollection()) return false;
        Location loc = transportable.getLocation();
        return loc == carrier
            && Map.isSameLocation(carrier.getLocation(), target);
    }

    /**
     * How much space would be needed to add this transportable?
     *
     * @return The extra space required.
     */
    public int getNewSpace() {
        if (!isValid()) return 0;
        int ret = 0;
        ret += (mode.isCollection()) ? getTransportable().getSpaceTaken()
            : -getTransportable().getSpaceTaken();
        if (hasWrapped()) {
            for (Cargo t : wrapped) ret += t.getNewSpace();
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
        return getTarget() == other.getTarget()
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
        if (wrapped == null) wrapped = new ArrayList<Cargo>();
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
     * TODO: be smarter.
     *
     * @return True if the <code>Cargo</code> should be retried.
     */
    public boolean retry() {
        return tries++ < MAX_TRY;
    }

    /**
     * Sets the target for this cargo, possibly also changing its mode.
     *
     * @return A reason the targeting failed, null if it succeeded.
     */
    public String setTarget() {
        if (!isValid()) return "invalid";
        if (transportable.isDisposed()) return "invalid-disposed";
        Location dst = transportable.getTransportDestination();
        if (dst == null) return "invalid-null-destination";
        dst = AIObject.upLoc(dst);
        PathNode deliveryPath = transportable.getDeliveryPath(carrier, dst);
        PathNode drop;

        if (transportable instanceof AIUnit) {
            final Unit unit = ((AIUnit)transportable).getUnit();
            if (unit.getLocation() == carrier) {
                // Can the carrier deliver the unit to the target?
                if (deliveryPath == null) {
                    return "no-deliver " + unit.toShortString()
                        + "/" + carrier.toShortString()
                        + " -> " + dst.toShortString();
                }
                // Drop node must exist, the unit is aboard
                drop = deliveryPath.getTransportDropNode();
                if (drop.getLocation().getColony() == null) {
                    this.mode = CargoMode.DROPOFF;
                    if (drop.previous == null) {
                        this.turns = 0;
                        this.target = drop.getLocation();
                    } else {
                        this.turns = drop.previous.getTotalTurns();
                        this.target = drop.previous.getLocation();
                    }
                } else {
                    this.mode = CargoMode.UNLOAD;
                    this.turns = drop.getTotalTurns();
                    this.target = drop.getLocation();
                }
            } else {
                // Can the carrier get the unit to the target, and
                // does the unit need the carrier at all?
                if (deliveryPath == null) {
                    return "no-collect+deliver " + unit.toShortString()
                        + "/" + carrier.toShortString()
                        + " -> " + dst.toShortString();
                }
                if ((drop = deliveryPath.getCarrierMove()) == null) {
                    return "no-carrier-move for " + carrier.toShortString()
                        + " to collect " + unit.toShortString();
                }
                // TODO: proper rendezvous paths, unit needs
                // to modify its target too!
                PathNode path = carrier.findPath(drop.getLocation());
                if (path == null) {
                    return "no-collect of " + unit.toShortString()
                        + " with " + carrier.toShortString()
                        + " -> " + drop.getLocation().toShortString();
                }
                if (drop.getLocation().getColony() == null) {
                    this.mode = CargoMode.PICKUP;
                    this.turns = drop.getTotalTurns();
                    this.target = drop.getLocation();
                } else {
                    this.mode = CargoMode.LOAD;
                    this.turns = drop.getTotalTurns();
                    this.target = drop.getLocation();
                }
            }
            this.target = AIObject.upLoc(this.target);
            return null;

        } else if (transportable instanceof AIGoods) {
            final Goods goods = ((AIGoods)transportable).getGoods();
            if (goods.getLocation() == carrier) {
                if (deliveryPath == null) {
                    Tile dstTile = dst.getTile();
                    Tile srcTile = carrier.getTile();
                    // OK, this is expected if the carrier is a
                    // wagon and the destination is Europe or on
                    // another landmass, or if the carrier is a
                    // ship and the destination is inland.  Try to
                    // find an intermediate port.
                    if (carrier.isNaval()) {
                        if (dstTile != null) {
                            deliveryPath = carrier.findIntermediatePort(dstTile);
                        }
                    } else {
                        if (dstTile == null
                            || dstTile.getContiguity() != dstTile.getContiguity()) {
                            deliveryPath = carrier.findOurNearestPort();
                        }
                    }
                }
                if (deliveryPath == null) {
                    return "no-deliver for " + carrier.toShortString()
                        + " -> " + dst.toShortString();
                } else {
                    this.mode = CargoMode.UNLOAD;
                    this.turns = deliveryPath.getLastNode().getTotalTurns();
                    this.target = deliveryPath.getLastNode().getLocation();
                }
            } else if (goods.getLocation() instanceof Unit) {
                return "goods-already-collected";
            } else {
                if (goods.getLocation() instanceof GoodsLocation) {
                    GoodsLocation gl = (GoodsLocation)goods.getLocation();
                    int present = gl.getGoodsCount(goods.getType());
                    if (present <= 0) { // Goods party can do this
                        ((AIGoods)transportable).dispose();
                        return "invalid-goods-gone-away";
                    }
                    if (goods.getAmount() != present) {
                        // Tolerate incorrect amount
                        goods.setAmount(Math.min(GoodsContainer.CARGO_SIZE,
                                present));
                    }
                }
                PathNode path = carrier.findPath(goods.getLocation());
                if (path == null) {
                    return "no-collect for " + carrier.toShortString()
                        + " -> " + dst.toShortString();
                } else {
                    this.mode = CargoMode.LOAD;
                    this.turns = path.getLastNode().getTotalTurns();
                    this.target = path.getLastNode().getLocation();
                }
            }
            this.target = AIObject.upLoc(this.target);
            return null;

        } else throw new IllegalStateException("Bogus transportable: "
            + transportable);
    }

    /**
     * Check the integrity of this carrier.
     *
     * @param aiCarrier The <code>AIUnit</code> version of the carrier.
     * @param tm A <code>TransportMission</code> to check with.
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
        if (tLoc instanceof Unit && (Unit)tLoc != carrier) {
            return "carrier usurped"; // On another carrier!
        }
        return null;
    }

    /**
     * Abbreviated string representation for this cargo.
     *
     * @return A short descriptive string.
     */
    public String toShortString() {
        LogBuilder lb = new LogBuilder(32);
        lb.add(mode.toString().toLowerCase(Locale.US), " ");
        if (transportable instanceof AIUnit) {
            lb.add(((AIUnit)transportable).getUnit());
        } else if (transportable instanceof AIGoods) {
            lb.add(((AIGoods)transportable).getGoods());
        }
        lb.add(" @ ", target);
        return lb.toString();
    }

    // Implement Comparable<Cargo>

    /**
     * {@inheritDoc}
     */
    public int compareTo(Cargo other) {
        // Cargoes that reduce the carried amount should sort
        // before those that do not.
        return other.getNewSpace() - getNewSpace();
    }

    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        LogBuilder lb = new LogBuilder(64);
        lb.add("[", transportable,
            " ", mode.toString().toLowerCase(Locale.US),
            ((mode.isCollection()) ? " from " : " to "), target,
            " ", turns, "/", tries, " space=", spaceLeft,
            ((wrapped == null) ? "" : " wrap"), "]");
        return lb.toString();
    }            


    // Serialization
    // Cargo is not yet an AIObject or FreeColObject, but that may happen.

    private static final String CARRIER_TAG = "carrier";
    private static final String MODE_TAG = "mode";
    private static final String TARGET_TAG = "target";
    private static final String TURNS_TAG = "turns";
    private static final String TRIES_TAG = "tries";
    private static final String SPACELEFT_TAG = "space";


    public void toXML(FreeColXMLWriter xw) throws XMLStreamException {
        xw.writeStartElement(getXMLElementTagName());

        xw.writeAttribute(FreeColObject.ID_ATTRIBUTE_TAG,
                          (AIObject)getTransportable());

        xw.writeAttribute(CARRIER_TAG, getCarrier());

        xw.writeAttribute(MODE_TAG, getMode());

        if (getTarget() != null) {
            xw.writeLocationAttribute(TARGET_TAG, getTarget());
        }

        xw.writeAttribute(TURNS_TAG, getTurns());

        xw.writeAttribute(TRIES_TAG, getTries());

        xw.writeAttribute(SPACELEFT_TAG, getSpaceLeft());

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

        this.mode = xr.getAttribute(MODE_TAG, 
                                    CargoMode.class, CargoMode.DUMP);
            
        this.target = xr.getLocationAttribute(game, TARGET_TAG, false);
            
        this.turns = xr.getAttribute(TURNS_TAG, -1);
            
        this.tries = xr.getAttribute(TRIES_TAG, 0);
        
        this.spaceLeft = xr.getAttribute(SPACELEFT_TAG, -1);
            
        this.wrapped = null;

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
