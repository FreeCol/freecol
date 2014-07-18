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

package net.sf.freecol.server.ai.mission;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsLocation;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Locatable;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.pathfinding.CostDecider;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.common.util.Utils;
import net.sf.freecol.server.ai.AIColony;
import net.sf.freecol.server.ai.AIGoods;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIObject;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.ai.EuropeanAIPlayer;
import net.sf.freecol.server.ai.GoodsWish;
import net.sf.freecol.server.ai.Transportable;
import net.sf.freecol.server.ai.Wish;
import net.sf.freecol.server.ai.WorkerWish;


/**
 * Mission for transporting units and goods on a carrier.
 *
 * @see net.sf.freecol.common.model.Unit Unit
 */
public class TransportMission extends Mission {

    private static final Logger logger = Logger.getLogger(TransportMission.class.getName());

    private static final String tag = "AI transport";

    /**
     * Insist transport lists remain simple by imposing an upper bound
     * on the distinct destination locations to visit.
     */
    private static final int DESTINATION_UPPER_BOUND = 3;

    /** Abandon cargo after three blockages. */
    private static final int MAX_TRY = 3;

    private static final int MINIMUM_GOLD_TO_STAY_IN_EUROPE = 600;

    private static enum CargoResult {
        TCONTINUE,  // Cargo should continue
        TDONE,      // Cargo completed successfully
        TFAIL,      // Cargo failed badly
        TNEXT,      // Cargo changed to its next state
        TRETRY      // Cargo has blocked, retry
    }

    /** The actions to perform at the target. */
    private static enum CargoMode {
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
     * An class describing the action needed to make progress in a
     * transportation action for a specific transportable.
     */
    public class Cargo implements Comparable<Cargo> {

        private Transportable transportable;
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
         * @param transportable The <code>Transportable</code> to cargo.
         * @param carrier The carrier <code>Unit</code>.
         */
        public Cargo(Transportable transportable, Unit carrier) {
            this(transportable, carrier, null);
        }

        /**
         * Creates the new dumping Cargo to a given location.
         *
         * @param transportable The <code>Transportable</code> to transport.
         * @param carrier The carrier <code>Unit</code>.
         * @param target The target <code>Location</code>.
         */
        public Cargo(Transportable transportable, Unit carrier,
                     Location target) {
            this(transportable, carrier, CargoMode.DUMP, target, -1, 0, -1);
        }

        /**
         * Creates the new Cargo with given mode and target.
         *
         * @param transportable The <code>Transportable</code> to transport.
         * @param carrier The carrier <code>Unit</code>.
         * @param mode The <code>CargoMode</code>.
         * @param target The target <code>Location</code>.
         * @param turns The turns required by the current path-estimate.
         * @param tries The tries taken to find a path.
         * @param spaceLeft The space left after this cargo action occurs.
         */
        public Cargo(Transportable transportable, Unit carrier, CargoMode mode,
                     Location target, int turns, int tries, int spaceLeft) {
            this.transportable = transportable;
            this.carrier = carrier;
            this.mode = mode;
            this.target = target;
            this.turns = turns;
            this.tries = tries;
            this.spaceLeft = spaceLeft;
            this.wrapped = null;
        }

        public Transportable getTransportable() {
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
            switch (mode) {
            case UNLOAD: case DROPOFF: case DUMP: return false;
            default: break;
            }
            Location loc = transportable.getTransportLocatable().getLocation();
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
            switch (mode) {
            case LOAD: case PICKUP: return false;
            default: break;
            }
            Location loc = transportable.getTransportLocatable().getLocation();
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
         * Transportables can be `wrapped' if they have the same target
         * and advancing them reduces the space on the carrier.
         *
         * @param other The other <code>Transportable</code> to consider.
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
            dst = upLoc(dst);
            PathNode path, drop;

            if (transportable instanceof AIUnit) {
                final Unit unit = ((AIUnit)transportable).getUnit();
                if (unit.getLocation() == carrier) {
                    // Can the carrier deliver the unit to the target?
                    if ((path = unit.findPath(carrier.getLocation(), dst,
                                              carrier, null)) == null) {
                        return "no-deliver " + unit.toShortString()
                            + "/" + carrier.toShortString()
                            + " -> " + dst.toShortString();
                    }
                    // Drop node must exist, the unit is aboard
                    drop = path.getTransportDropNode();
                    if (upLoc(drop.getLocation()) instanceof Tile) {
                        this.mode = CargoMode.DROPOFF;
                        if (drop.previous == null) {
                            this.turns = 0;
                            this.target = drop.getLocation();
                        } else {
                            this.turns = drop.previous.getTotalTurns();
                            this.target = upLoc(drop.previous.getLocation());
                        }
                    } else {
                        this.mode = CargoMode.UNLOAD;
                        this.turns = drop.getTotalTurns();
                        this.target = upLoc(drop.getLocation());
                    }
                } else {
                    // Can the carrier get the unit to the target, and
                    // does the unit need the carrier at all?
                    if ((path = unit.findPath(unit.getLocation(), dst,
                                              carrier, null)) == null) {
                        return "no-collect+deliver " + unit.toShortString()
                            + "/" + carrier.toShortString()
                            + " -> " + dst.toShortString();
                    }
                    if ((drop = path.getCarrierMove()) == null) {
                        return "no-carrier-move for " + carrier.toShortString()
                            + " to collect " + unit.toShortString();
                    }
                    // TODO: proper rendezvous paths, unit needs
                    // to modify its target too!
                    if ((path = carrier.findPath(drop.getLocation())) == null) {
                        return "no-collect of " + unit.toShortString()
                            + " with " + carrier.toShortString()
                            + " -> " + drop.getLocation().toShortString();
                    }
                    if (upLoc(drop.getLocation()) instanceof Tile) {
                        this.mode = CargoMode.PICKUP;
                        this.turns = drop.getTotalTurns();
                        this.target = upLoc(drop.getLocation());
                    } else {
                        this.mode = CargoMode.LOAD;
                        this.turns = drop.getTotalTurns();
                        this.target = upLoc(drop.getLocation());
                    }
                }
                return null;

            } else if (transportable instanceof AIGoods) {
                final Goods goods = ((AIGoods)transportable).getGoods();
                if (goods.getLocation() == carrier) {
                    path = carrier.findPath(dst);
                    if (path == null) {
                        Tile dstTile = dst.getTile();
                        Tile srcTile = carrier.getTile();
                        // OK, this is expected if the carrier is a
                        // wagon and the destination is Europe or on
                        // another landmass, or if the carrier is a
                        // ship and the destination is inland.  Try to
                        // find an intermediate port.
                        if (carrier.isNaval()) {
                            if (dstTile != null) {
                                path = carrier.findIntermediatePort(dstTile);
                            }
                        } else {
                            if (dstTile == null
                                || dstTile.getContiguity() != dstTile.getContiguity()) {
                                path = carrier.findOurNearestPort();
                            }
                        }
                    }
                    if (path == null) {
                        return "no-deliver for " + carrier.toShortString()
                            + " -> " + dst.toShortString();
                    } else {
                        this.mode = CargoMode.UNLOAD;
                        this.turns = path.getLastNode().getTotalTurns();
                        this.target = upLoc(path.getLastNode().getLocation());
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
                    path = carrier.findPath(goods.getLocation());
                    if (path == null) {
                        return "no-collect for " + carrier.toShortString()
                            + " -> " + dst.toShortString();
                    } else {
                        this.mode = CargoMode.LOAD;
                        this.turns = path.getLastNode().getTotalTurns();
                        this.target = upLoc(path.getLastNode().getLocation());
                    }
                }
                return null;

            } else throw new IllegalStateException("Bogus transportable: "
                + transportable);
        }

        /**
         * Check the integrity of this carrier.
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
            
            String reason = invalidReason(aiCarrier, target);
            if (reason != null) return reason;

            Locatable l = transportable.getTransportLocatable();
            if (l == null) {
                return "null locatable: " + transportable.toString();
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


        // Implement Comparable<Cargo>

        /**
         * {@inheritDoc}
         */
        public int compareTo(Cargo other) {
            // Cargoes that reduce the carried amount should sort
            // before those that do not.
            return getNewSpace() - other.getNewSpace();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            LogBuilder lb = new LogBuilder(64);
            lb.add("[", transportable, " mode=", mode,
                ((mode.isCollection()) ? " from " : " to "), target,
                " ", turns, "/", tries, " space=", spaceLeft,
                ((wrapped == null) ? "" : " wrap"), "]");
            return lb.toString();
        }            
    };


    /** A list of <code>Cargo</code>s to work on. */
    private final List<Cargo> cargoes = new ArrayList<Cargo>();

    /** The current target location to travel to. */
    private Location target = null;


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     */
    public TransportMission(AIMain aiMain, AIUnit aiUnit) {
        super(aiMain, aiUnit);

        LogBuilder lb = new LogBuilder(logger, Level.FINEST);
        lb.add(tag);
        checkCargoes(lb);
        retarget();
        lb.add(" begins: ", toFullString());
        lb.flush();
        uninitialized = false;
    }

    /**
     * Creates a new <code>TransportMission</code> and reads the given
     * element.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     * @param xr The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public TransportMission(AIMain aiMain, AIUnit aiUnit,
                            FreeColXMLReader xr) throws XMLStreamException {
        super(aiMain, aiUnit);

        readFromXML(xr);
        uninitialized = getAIUnit() == null;
    }


    /**
     * Disposes of this <code>Mission</code>.
     */
    public void dispose() {
        logger.finest(tag + " disposing (" + clearCargoes() + "): " + this);
        super.dispose();
    }


    // Simple internal utilities

    /**
     * Gets the trivial target for the carrier unit.
     *
     * @return A path to the trivial target, or null if none found.
     */
    private PathNode getTrivialPath() {
        final Unit carrier = getUnit();
        return (carrier.isDisposed()) ? null
            : (carrier.isNaval()) ? carrier.findOurNearestPort()
            : carrier.findOurNearestSettlement();
    }

    /**
     * Checks if the carrier using this mission is carrying the given
     * <code>Transportable</code>.
     *
     * @param t The <code>Transportable</code> to check.
     * @return True if the carrier is carrying the transportable.
     */
    private boolean isCarrying(Transportable t) {
        return t != null
            && t.getTransportLocatable() != null
            && t.getTransportLocatable().getLocation() == getUnit();
    }

    /**
     * Is a transportable waiting for delivery on the cargoes list?
     *
     * @param t The <code>Transportable</code> to check.
     * @return True if the transportable is queued in this mission.
     */
    public boolean isTransporting(Transportable t) {
        return tFind(t) != null;
    }

    /**
     * Decide if this unit has a good chance of defeating another.
     * If there is cargo aboard, be more conservative.
     * TODO: magic numbers.
     *
     * @param other The other <code>Unit</code> to attack.
     * @return True if the attack should proceed.
     */
    private boolean shouldAttack(Unit other) {
        if (invalidAttackReason(getAIUnit(),
                                other.getOwner()) != null) return false;
        final Unit carrier = getUnit();
        final CombatModel cm = getGame().getCombatModel();
        float offence = cm.getOffencePower(carrier, other)
            * ((carrier.hasCargo()) ? 0.3f : 0.80f);
        return offence > cm.getOffencePower(other, carrier);
    }


    // Cargoes handling.
    // *Nothing* should touch "cargoes" but these.
    // It needs synchronization because of possible changes due to the
    // carrier being destroyed.

    /**
     * Gets the cargoes.
     *
     * @return A copy of the list of cargoes.
     */
    private List<Cargo> tCopy() {
        List<Cargo> nxt;
        synchronized (cargoes) {
            nxt = new ArrayList<Cargo>(cargoes);
        }
        return nxt;
    }

    /**
     * Clears the cargoes list.
     *
     * @return The old cargoes list.
     */
    private List<Cargo> tClear() {
        List<Cargo> old = new ArrayList<Cargo>();
        synchronized (cargoes) {
            old.addAll(cargoes);
            cargoes.clear();
        }
        return old;
    }

    /**
     * Sets the cargoes to a new list.
     *
     * @param nxt The new cargoes list.
     * @param setSpace If true, call tSpace to reset the space left values.
     * @return The old cargoes list.
     */
    private List<Cargo> tSet(List<Cargo> nxt, boolean setSpace) {
        List<Cargo> old = tClear();
        synchronized (cargoes) {
            for (Cargo c : nxt) {
                if (c.isValid()) cargoes.add(c);
            }
            if (setSpace) tSpace();
        }
        return old;
    }

    /**
     * Gets the size of the cargoes.
     *
     * @return The size of the cargoes.
     */
    private int tSize() {
        int size;
        synchronized (cargoes) {
            size = cargoes.size();
        }
        return size;
    }

    /**
     * Find a <code>Cargo</code> with the given <code>Transportable</code>.
     *
     * @param t The <code>Transportable</code> to look for.
     * @return The <code>Cargo</code> found, or null if none found.
     */
    private Cargo tFind(Transportable t) {
        Cargo result = null;
        synchronized (cargoes) {
            for (Cargo cargo : cargoes) {
                if (cargo.getTransportable() == t) {
                    result = cargo;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Gets the first cargo.
     *
     * @return The first cargo, or null if none found.
     */
    private Cargo tFirst() {
        Cargo cargo = null;
        synchronized (cargoes) {
            for (int i = 0; i < cargoes.size(); i++) {
                if (cargoes.get(i).isValid()) {
                    cargo = cargoes.get(i);
                    break;
                }
            }
        }
        return cargo;
    }

    /**
     * Adds a cargo to the cargoes list.
     *
     * @param cargo The <code>Cargo</code> to add.
     * @param index The position to add it.
     * @return True if the addition succeeded or the cargo was already present.
     */
    private boolean tAdd(Cargo cargo, int index) {
        if (!cargo.isValid()) return false;
        if (tFind(cargo.getTransportable()) != null) return true;
        synchronized (cargoes) {
            if (index >= 0) {
                cargoes.add(index, cargo); 
            } else {
                cargoes.add(cargo);
            }
            tSpace();
        }
        return true;
    }

    /**
     * Remove a cargo from the cargoes list.
     *
     * @param cargo The <code>Cargo</code> to remove.
     * @return True if the remove succeeded.
     */
    private boolean tRemove(Cargo cargo) {
        boolean result;
        synchronized (cargoes) {
            result = cargoes.remove(cargo);
            tSpace();
        }
        return result;
    }

    /**
     * Sets the spaceLeft fields in the cargoes.
     * To be called with synchronized (cargoes).
     */
    private void tSpace() {
        final Unit carrier = getUnit();
        final int maxHolds = carrier.getCargoCapacity();
        int holds = carrier.getCargoSpaceTaken();
        int i = 0;
        while (i < cargoes.size()) {
            Cargo cargo = cargoes.get(i);
            if (cargo.isValid()) {
                holds += cargo.getNewSpace();
                cargo.setSpaceLeft(maxHolds - holds);
                i++;
            } else {
                cargoes.remove(i);
            }
        }
    }

    // Medium-level cargo and target manipulation, should be kept
    // private to TransportMission.

    /**
     * Retarget the mission using the cargoes list.
     */
    private void retarget() {
        Cargo cargo = tFirst();
        PathNode path;
        if (cargo != null) {
            setTarget(cargo.getTarget());
            logger.finest(tag + " retargeted for cargo: "
                + upLoc(cargo.getTarget()).toShortString());
        } else if ((path = getTrivialPath()) != null) {
            Location loc = upLoc(path.getLastNode().getLocation());
            setTarget(loc);
            logger.finest(tag + " retargeted safe port: "
                + loc.toShortString());
        } else {
            setTarget(null);
            logger.finest(tag + " unable to retarget safe port: " + this);
        }
    }

    /**
     * Count distinct non-adjacent destinations in a list of cargoes.
     *
     * @return The number of distinct destinations.
     */
    private int destinationCount() {
        Location now = null;
        int ret = 0;
        for (Cargo cargo : tCopy()) {
            if (now == null) {
                now = cargo.getTarget();
            } else if (Map.isSameLocation(now, cargo.getTarget())) {
                ; // do nothing
            } else {
                ret++;
                now = cargo.getTarget();
            }
        }
        return ret;
    }                

    /**
     * How many more destinations are desirable on the current cargoes list?
     * Must be public!  This is checked in European AI player.
     *
     * @return The number of desired extra destinations.
     */
    public int destinationCapacity() {
        return DESTINATION_UPPER_BOUND - destinationCount();
    }

    /**
     * If this carrier is the current carrier of a transportable, drop it.
     *
     * @param t The <code>Transportable</code> to check.
     * @param reason A reason for changing the carrier.
     */
    private void dropTransportable(Transportable t, String reason) {
        AIUnit carrier = getAIUnit();
        if (t.getTransport() == carrier) t.setTransport(null, reason);
    }

    /**
     * If this carrier is the not the current carrier of a
     * transportable, make it so.
     *
     * @param t The <code>Transportable</code> to check.
     * @param reason A reason for changing the carrier.
     */
    private void takeTransportable(Transportable t, String reason) {
        AIUnit carrier = getAIUnit();
        if (t.getTransport() != carrier) t.setTransport(carrier, reason);
    }

    /**
     * Wrap up the compatible cargoes in a list.
     * O(N^2) alas.
     *
     * @return A wrapped list of cargoes.
     */
    private List<Cargo> wrapCargoes() {
        List<Cargo> ts = tCopy();
        String logMe = ":";
        for (Cargo t : ts) logMe += "\n" + t.toString();
        try {
            for (int i = 0; i < ts.size(); i++) {
                Cargo head = ts.get(i);
                while (i+1 < ts.size() && head.couldWrap(ts.get(i+1))) {
                    head.wrap(ts.remove(i+1));
                }
            }
        } catch (Exception e) {
            logMe = e.getMessage() + logMe;
            throw new IllegalStateException(logMe);
        }
        return ts;
    }

    /**
     * Unwrap a wrapped list of cargoes.
     *
     * @param ts The list of <code>Cargo</code>s to unwrap.
     * @return The unwrapped list of cargoes.
     */
    private List<Cargo> unwrapCargoes(List<Cargo> ts) {
        for (int i = 0; i < ts.size(); i++) {
            Cargo t = ts.get(i);
            if (t.hasWrapped()) {
                List<Cargo> tl = t.unwrap();
                ts.addAll(i+1, tl);
                i += tl.size();
            }
        }
        return ts;
    }

    /**
     * Clears all the cargoes.
     *
     * @return A message about the cargoes being cleared.
     */
    private String clearCargoes() {
        String log = "cargoes cleared: ";
        for (Cargo cargo : tClear()) {
            dropTransportable(cargo.getTransportable(), "cleared");
            log += " " + cargo;
        }
        return log;
    }

    /**
     * For a given transportable, work out where the carrier has to go to
     * advance the cargo (target), and what to do there (mode), allowing
     * a new <code>Cargo</code> to be defined.
     *
     * AIUnit cargo is harder than AIGoods, because AIUnits might have their
     * own inland paths, and thus we need to consider drop nodes.
     *
     * @param t The <code>Transportable</code> to consider.
     * @return A new <code>Cargo</code> defining the action to take
     *     with the <code>Transportable</code>, or null if impossible.
     */
    public Cargo makeCargo(Transportable t) {
        final Unit carrier = getUnit();
        String reason;
        Cargo cargo = null;
        if (t.getTransportDestination() == null) {
            reason = "null transport destination";
        } else if (!isCarrying(t) && !t.carriableBy(carrier)) {
            reason = "carrier " + carrier.toShortString() + " can not carry";
        } else {
            cargo = new Cargo(t, carrier);
            reason = cargo.setTarget();
        }
        if (reason == null) return cargo;
        logger.finest("Failed to make cargo (" + reason + "): " + t);
        return null;
    }

    /**
     * Add the given Cargo to the cargoes list.
     *
     * @param cargo The <code>Cargo</code> to add.
     * @param index The index of where to add the cargo.
     * @return True if the cargo was added.
     */
    private boolean addCargo(Cargo cargo, int index) {
        boolean result = tAdd(cargo, index);
        if (result) {
            takeTransportable(cargo.getTransportable(), "added");
            retarget();
        }

        if (result) {
            logger.finest(tag + " added " + cargo.toString()
                + " (at " + ((index < 0) ? "end" : Integer.toString(index))
                + "): " + toFullString());
        } else {
            logger.warning(tag + " add " + cargo.toString()
                + " (at " + ((index < 0) ? "end" : Integer.toString(index))
                + ") failed: " + toFullString());
        }
        return result;
    }

    /**
     * Removes the given Cargo from the cargoes list.
     *
     * @param cargo The <code>Cargo</code> to remove.
     * @param reason The reason for its removal (if null, do not log, it has
     *     already been mentioned).
     * @return True if the removal succeeded.
     */
    private boolean removeCargo(Cargo cargo, String reason) {
        boolean result = tRemove(cargo);
        if (result) {
            dropTransportable(cargo.getTransportable(), reason);
            retarget();
        }
        return result;
    }

    /**
     * Is there space available for a new cargo?
     *
     * @param cargo The <code>Cargo</code> to check.
     * @return True if there is space available for this cargo.
     */
    public boolean spaceAvailable(Cargo cargo) {
        return spaceAvailable(cargo.getTransportable());
    }

    /**
     * Is there space available for a new cargo?
     *
     * @param t The <code>Transportable</code> to check.
     * @return True if there is space available for this transportable.
     */
    public boolean spaceAvailable(Transportable t) {
        final List<Cargo> ts = tCopy();
        final int newSpace = t.getSpaceTaken();

        for (int i = ts.size()-1; i >= 0; i--) {
            if (ts.get(i).getSpaceLeft() < newSpace) return false;
        }
        return true;
    }

    /**
     * Incrementally queue a cargo to the cargoes list.  Try
     * place it with other cargoes with the same target, but do not
     * break the space restrictions.  If this does not work, it has
     * to go at the end.
     *
     * @param cargo The new <code>Cargo</code> to add.
     * @param requireMatch Fail if an existing destination is not matched.
     * @return True if the cargo was queued.
     */
    private boolean queueCargo(Cargo cargo, boolean requireMatch) {
        final Unit carrier = getUnit();
        final int maxHolds = carrier.getCargoCapacity();
        final List<Cargo> ts = tCopy();
        final int newSpace = cargo.getNewSpace();
        final Transportable t = cargo.getTransportable();

        // Match an existing target?
        int candidate = -1;
        outer: for (int i = 0; i < ts.size(); i++) {
            Cargo tr = ts.get(i);
            if (Map.isSameLocation(tr.getTarget(), cargo.getTarget())) {
                for (int j = i; j < ts.size(); j++) {
                    int holds = (j == 0) ? carrier.getCargoSpaceTaken()
                        : maxHolds - ts.get(j-1).getSpaceLeft();
                    holds += newSpace;
                    if (holds < 0 || holds > maxHolds) continue outer;
                }
                if (cargo.compareTo(tr) <= 0) {
                    candidate = i;
                    break;
                }
                candidate = i+1;
            } else if (candidate >= 0) break;
        }
        if (candidate < 0) {
            if (requireMatch) return false;
            candidate = ts.size();
        }
        return addCargo(cargo, candidate);
    }

    /**
     * Retarget an existing cargo.
     *
     * @param cargo The <code>Cargo</code> to retarget.
     * @return True if the retargeting succeeded.
     */
    public boolean requeueCargo(Cargo cargo) {
        Transportable t = cargo.getTransportable();
        Location dst = t.getTransportDestination();
        int result = 0;
        if (tRemove(cargo)) {
            result++;
            Cargo next = makeCargo(t);
            if (next == null) {
                dst = this.target;
                if (dst == null) {
                    PathNode path = getTrivialPath();
                    dst = path.getLastNode().getLocation();
                }
                if (dst != null) next = new Cargo(t, getUnit(), dst);
            }
            if (next != null) {
                cargo = next;
                result++;
                if (queueCargo(cargo, false)) {
                    takeTransportable(t, "retarget/queued");
                    result++;
                } else {
                    dropTransportable(t, "retarget/queuing failed");
                }
            }
            retarget();
        }

        switch (result) {
        case 0:
            logger.finest(tag + " retarget failed to remove " + cargo
                + ": " + toFullString());
            break;
        case 1:
            logger.finest(tag + " retarget failed to remake " + cargo
                + ": " + toFullString());
            break;
        case 2:
            logger.finest(tag + " retarget failed to requeue " + cargo
                + ": " + toFullString());
            break;
        case 3:
            logger.finest(tag + " retarget succeeded for " + cargo
                + " to " + dst.toShortString() + ": " + toFullString());
            break;
        }
        return result == 3;
    }

    /**
     * Find a good place to send a transportable currently on board a
     * carrier yet without a meaningful transport destination.
     *
     * @param t The <code>Transportable</code> to retarget.
     * @param lb A <code>LogBuilder</code> to log to.
     * @return True if the transportable should now have a valid
     *     transport destination.
     */
    private boolean retargetTransportable(Transportable t, LogBuilder lb) {
        final EuropeanAIPlayer euaip = getEuropeanAIPlayer();
        final AIUnit aiCarrier = getAIUnit();
        final Unit carrier = aiCarrier.getUnit();

        List<Location> locations = new ArrayList<Location>();
        for (Cargo c : tCopy()) {
            if (locations.contains(c.getTarget())) continue;
            locations.add(c.getTarget());
        }
            
        if (t instanceof AIUnit) {
            final AIUnit aiu = (AIUnit)t;
            final Unit u = aiu.getUnit();

            // Look for a scheduled location that wants this unit
            UnitType type = u.getType();
            Mission m = null;
            for (Location loc : locations) {
                for (WorkerWish ww : euaip.getWorkerWishesAt(loc, type)) {
                    if ((m = euaip.consumeWorkerWish(aiu, ww)) != null) {
                        aiu.changeMission(m, lb);lb.add(", ");
                        return true;
                    }
                }
            }
     
            // Try giving the unit a new mission (may well call
            // getBestWorkerWish at some point).
            if ((m = euaip.getSimpleMission(aiu)) != null) {
                aiu.changeMission(m, lb);lb.add(", ");
                return true;
            }

        } else if (t instanceof AIGoods) {
            final AIGoods aig = (AIGoods)t;

            Location dst = t.getTransportDestination();
            if (dst != null
                && carrier.getTurnsToReach(dst) != INFINITY) return true;

            // Look for a scheduled location that wants these goods
            GoodsType type = aig.getGoods().getType();
            for (Location loc : locations) {
                for (GoodsWish gw : euaip.getGoodsWishesAt(loc, type)) {
                    aig.setTransportDestination(loc);
                    int a = aig.getGoods().getAmount();
                    if (a >= gw.getGoodsAmount()) {
                        euaip.consumeGoodsWish(aig, gw);
                    } else {
                        gw.setGoodsAmount(gw.getGoodsAmount() - a);
                    }
                    lb.add(aig, " to ", loc, ", ");
                    return true;
                }
            }

            // Try another existing goods wish.
            GoodsWish gw = euaip.getBestGoodsWish(aiCarrier, type);
            if (gw != null) {
                aig.setTransportDestination(gw.getDestination());
                euaip.consumeGoodsWish(aig, gw);
                lb.add(aig, " to ", gw.getDestination(), ", ");
                return true;
            }

            // Look for a suitable colony to unload the goods.
            Location best = null;
            int bestValue = INFINITY;
            for (AIColony aic : euaip.getAIColonies()) {
                Colony colony = aic.getColony();
                if (colony.getImportAmount(aig.getGoodsType())
                    >= aig.getGoodsAmount()) {
                    int value = carrier.getTurnsToReach(colony);
                    if (bestValue > value) {
                        bestValue = value;
                        best = colony;
                    }
                }
            }
            Europe europe = getPlayer().getEurope();
            if (europe != null && getPlayer().canTrade(aig.getGoodsType())
                && carrier.getTurnsToReach(europe) < bestValue) {
                best = europe;
            }
            if (best != null) {
                aig.setTransportDestination(best);
                lb.add(aig, " to unload at ", best, ", ");
                return true;
            }
        }

        return false;
    }

    /**
     * Checks for invalid cargoes, and units and goods on board but
     * not in the cargoes list.  On exit from this routine, every
     * cargo on board should be on the cargoes list but the list is
     * not necessarily going to be in a sensible order.
     *
     * @param lb A <code>LogBuilder</code> to log to.
     */
    private void checkCargoes(LogBuilder lb) {
        final Unit carrier = getUnit();
        if (carrier.isAtSea()) return; // Let it emerge.

        final AIUnit aiCarrier = getAIUnit();
        final EuropeanAIPlayer euaip = getEuropeanAIPlayer();
        final Location here = carrier.getLocation();

        List<Unit> unitsPresent = carrier.getUnitList();
        List<Goods> goodsPresent = carrier.getCompactGoods();
        List<Transportable> drop = new ArrayList<Transportable>();
        List<Transportable> retry = new ArrayList<Transportable>();
        String reason;
        PathNode path;
        lb.add(" [check:");
        for (Cargo cargo : tCopy()) {
            Transportable t = cargo.getTransportable();
            if ((reason = cargo.check(aiCarrier)) != null) {
                // Just remove, it is invalid
                boolean result = removeCargo(cargo, reason);
                lb.add(" invalid(", reason, ")", cargo, "=", result);
            } else if (cargo.isCollectable()) {
                lb.add(" collect", cargo);
            } else if (cargo.isDeliverable()) {
                lb.add(" deliver", cargo);
            } else if ((path = carrier.findPath(cargo.getTarget())) == null
                && !cargo.retry()) {
                boolean result = removeCargo(cargo, "no path");
                lb.add(" drop(no-path)", cargo, "=", result);
                drop.add(t);
            } else if (carrier.hasTile()
                && (reason = cargo.setTarget()) != null) {
                boolean result = removeCargo(cargo, "fail(" + reason + ")");
                if (reason.startsWith("invalid") || !cargo.retry()) {
                    lb.add(" failed(", reason, ")", cargo, "=", result);
                } else {
                    lb.add(" retry(", reason, ")", cargo, "=", result);
                    retry.add(t);
                }
            } else {
                ; // Good
            }
            if (t instanceof AIUnit) {
                unitsPresent.remove(((AIUnit)t).getUnit());
            } else if (t instanceof AIGoods) {
                Goods goods = ((AIGoods)t).getGoods();
                if (goods != null) {
                    Iterator<Goods> gi = goodsPresent.iterator();
                    while (gi.hasNext()) {
                        Goods g = gi.next();
                        if (g.getType() == goods.getType()) {
                            gi.remove();
                            break;
                        }
                    }
                }
            }
        }

        // Retry anything found that was not on the cargoes list
        if (!unitsPresent.isEmpty()) {
            lb.add(", found unexpected units:");
            for (Unit u : unitsPresent) {
                AIUnit aiu = getAIMain().getAIUnit(u);
                if (aiu == null) throw new IllegalStateException("Bogus:" + u);
                retry.add(aiu);
                lb.add(" ", aiu.getUnit());
            }
        }
        if (!goodsPresent.isEmpty()) {
            lb.add(", found unexpected goods:");
            for (Goods g : goodsPresent) {
                AIGoods aig = new AIGoods(getAIMain(), carrier, g.getType(),
                                          g.getAmount(), null);
                retry.add(aig);
                lb.add(" ", aig);
            }
        }

        // Ask the parent player to retarget transportables on the retry list
        if (!retry.isEmpty()) {
            lb.mark();
            for (Transportable t : retry) {
                if (!retargetTransportable(t, lb)) drop.add(t);
            }
            if (lb.grew(", retarget: ")) lb.shrink(", ");
        }

        // Drop transportables on the drop list, or queue them to be dropped
        // at the next port
        if (!drop.isEmpty()) {
            path = getTrivialPath();
            Location end = (path == null) ? null
                : path.getLastNode().getLocation();
            boolean dropReady = path == null || carrier.isAtLocation(end);
            if (dropReady) {
                lb.add(", drop at ", upLoc(here), ":");
                while (!drop.isEmpty()) {
                    Transportable t = drop.remove(0);
                    boolean result = dumpTransportable(t, true);
                    lb.add(" ", t, ((result) ? "" : "(failed)"));
                }
            } else {
                lb.add(", will drop at ", upLoc(end));
                while (!drop.isEmpty()) {
                    Transportable t = drop.remove(0);
                    Cargo cargo = new Cargo(t, carrier, end);
                    boolean result = queueCargo(cargo, false);
                    lb.add(" ", t, ((result) ? "" : "(failed)"));
                }
            }
        }

        lb.add("]");
    }

    /**
     * Check a <code>Cargo</code> for continued validity and
     * whether action is needed at the current location.
     *
     * @param cargo The <code>Cargo</code> to check.
     * @return TCONTINUE if the <code>Cargo</code> should continue,
     *     TDONE if it has completed,
     *     TFAIL if it has failed,
     *     TNEXT if it has progressed to the next stage,
     *     TRETRY if a blockage has occurred and it should be retried,
     */
    private CargoResult tryCargo(Cargo cargo) {
        final Unit carrier = getUnit();
        final Location here = carrier.getLocation();
        final Transportable t = cargo.getTransportable();
        final Locatable l = t.getTransportLocatable();
        AIUnit aiu;
        AIGoods aig;
        String reason;

        switch (cargo.getMode()) {
        case LOAD:
            if (!Map.isSameLocation(here, l.getLocation())) {
                return CargoResult.TCONTINUE;
            }
            switch (carrier.getNoAddReason(l)) {
            case NONE: break;
            case CAPACITY_EXCEEDED: return CargoResult.TCONTINUE;
            default: return CargoResult.TRETRY;
            }

            if (!t.joinTransport(carrier, null)) {
                logger.warning(tag + " failed to load " + t
                    + " at " + here.toShortString() + ": " + this);
                return CargoResult.TFAIL;
            }
            logger.finest(tag + " loaded " + t
                + " at " + here.toShortString() + ": " + this);

            if ((reason = cargo.setTarget()) == null) {
                return CargoResult.TNEXT;
            }
            logger.finest(tag + " next fail(" + reason + ") " + t
                + " at " + here.toShortString() + ": " + this);
            return CargoResult.TFAIL;

        case UNLOAD:
            if (!Map.isSameLocation(here, cargo.getTarget())) {
                return CargoResult.TCONTINUE;
            }
            if (t.leaveTransport(null)) {
                logger.finest(tag + " completed (unload) of " + t
                    + " at " + here.toShortString() + ": " + this);
            } else {
                logger.warning(tag + " failed to unload " + t
                    + " at " + here.toShortString() + ": " + this);
                return CargoResult.TFAIL;
            }
            return CargoResult.TDONE;

        case PICKUP:
            if (!Map.isSameLocation(here, cargo.getTarget())) {
                return CargoResult.TCONTINUE;
            }
            if (isCarrying(t)) {
                logger.finest(tag + " picked up " + t
                    + " at " + here.toShortString() + ": " + this);
                if ((reason = cargo.setTarget()) == null) {
                    return CargoResult.TNEXT;
                }
                logger.finest(tag + " next fail(" + reason + ") " + t
                    + " at " + here.toShortString() + ": " + this);
                return CargoResult.TFAIL;
            }
            aiu = (AIUnit)t;
            if ((reason = aiu.getMission().invalidReason()) != null) {
                logger.warning(tag + " unit mission failed(" + reason
                    + ") for " + t + ": " + this);
                return CargoResult.TFAIL;
            }
            return CargoResult.TCONTINUE;

        case DROPOFF:
            if (!Map.isSameLocation(here, cargo.getTarget())) {
                return CargoResult.TCONTINUE;
            }
            if (!isCarrying(t)) {
                logger.finest(tag + " completed (dropoff) " + t
                    + " at " + carrier.getLocation().toShortString()
                    + ": " + this);
                return CargoResult.TDONE;
            }
            aiu = (AIUnit)t;
            if ((reason = aiu.getMission().invalidReason()) != null) {
                logger.warning(tag + " unit mission failed(" + reason
                    + ") for " + t + ": " + this);
                return CargoResult.TFAIL;
            }
            return CargoResult.TCONTINUE;

        case DUMP:
            return (dumpTransportable(t, false)) ? CargoResult.TDONE
                : CargoResult.TCONTINUE;
        }
        throw new IllegalStateException("Can not happen");
    }

    /**
     * Calculates a score for a proposed list of
     * <code>Cargo</code>s using the current unit.  Disallows
     * routes that would overfill the carrier.
     *
     * Useful for comparing proposed cargo delivery routes.  The
     * score is based primarily on the number of turns it takes, but
     * to break ties we also consider the hold*turn product to reduce
     * the risk of losses due to enemy action.
     *
     * @param initialLocation The initial <code>Location</code>.
     * @param order An ordering of <code>Cargo</code>s.
     * @return A score for the cargo ordering.
     */
    private float scoreCargoOrder(Location initialLocation, List<Cargo> order) {
        final Unit carrier = getUnit();
        final int maxHolds = carrier.getCargoCapacity();
        int holds = carrier.getCargoSpaceTaken();
        Location now = initialLocation;
        float totalHoldTurns = 0.0f;
        float totalTurns = 0.0f;
        float favourEarly = 1.0f;

        for (Cargo cargo : order) {
            int turns = carrier.getTurnsToReach(now, cargo.getTarget());
            totalTurns += turns; // Might be INFINITY!
            totalHoldTurns += holds * turns * favourEarly;
            holds += cargo.getNewSpace();
            if (holds < 0 || holds > maxHolds) return -1.0f;
            now = cargo.getTarget();
            favourEarly += 0.1f; // Slight preference for large loads first
        }
        return totalTurns + 0.001f * totalHoldTurns;
    }

    /**
     * Sets the current target.
     * Tries all permutations of cargoes and picks the fastest/safest one.
     *
     * Leaves the cargoes in the order they are expected to
     * execute, with valid spaceLeft values.
     *
     * @param lb A <code>LogBuilder</code> to log to.
     */
    private void optimizeCargoes(LogBuilder lb) {
        lb.add(", optimize");

        // We wrap/unwrap the list to minimize the number of nodes
        // that need consideration.
        List<Cargo> ts = wrapCargoes();
        List<Cargo> best = null;
        if (1 < ts.size() && ts.size() <= DESTINATION_UPPER_BOUND) {
            // Try all the permutations of visiting order for the
            // locations, and set the target to the first location
            // of the best scoring route.
            //
            // The target may get recomputed every time a cargo change
            // occurs, so there is no guarantee that the route chosen
            // here is actually executed.  This seems rather
            // inefficient, but we need to be adaptable.
            //
            final Location current = getUnit().getLocation();
            float bestValue = INFINITY;
            for (List<Cargo> tl : Utils.getPermutations(ts)) {
                float value = scoreCargoOrder(current, tl);
                if (value > 0.0f && bestValue > value) {
                    bestValue = value;
                    best = tl;
                }
            }
        }
        if (best != null) {
            tSet(unwrapCargoes(best), true);
        } else {
            tSet(unwrapCargoes(ts), false);
        }
        retarget();
        lb.add(" -> ", getTarget());
    }

    /**
     * What is the best transportable for a carrier to collect?
     *
     * @param carrier The carrier <code>Unit</code> to consider.
     * @return The best transportable, or null if none found.
     */
    private Transportable getBestTransportable(Unit carrier) {
        final EuropeanAIPlayer euaip = getEuropeanAIPlayer();
        final Location src = (carrier.isAtSea()) ? carrier.resolveDestination()
            : carrier.getLocation();
        Transportable best = null;
        float bestValue = 0.0f;
        for (Transportable t : euaip.getUrgentTransportables()) {
            if (t.isDisposed() || !t.carriableBy(carrier)) continue;
            Location loc = t.getTransportSource();
            int turns = carrier.getTurnsToReach(loc);
            if (turns != INFINITY) {
                float value = t.getTransportPriority() / (turns + 1);
                if (bestValue < value) {
                    bestValue = value;
                    best = t;
                }
            }
        }
        return best;
    }

    // End of cargoes handling.

    // Publically accessible routines to manipulate a Transportable.

    /**
     * Adds the given <code>Transportable</code> to the cargo
     * list.
     *
     * @param t The <code>Transportable</code> to add.
     * @param index The index of where to add the cargo.
     * @return True if the <code>Transportable</code> was added.
     */
    public boolean addTransportable(Transportable t, int index) {
        if (tFind(t) != null) return false;

        AIUnit oldCarrier = t.getTransport();
        if (oldCarrier.getMission() instanceof TransportMission) {
            ((TransportMission)oldCarrier.getMission())
                .removeTransportable(t);
        }

        Cargo cargo = makeCargo(t);
        return (cargo == null) ? false : addCargo(cargo, index);
    }

    /**
     * Removes the given <code>Transportable</code> from the cargo list.
     *
     * @param t The <code>Transportable</code> to remove.
     * @return True if the removal succeeded.
     */
    public boolean removeTransportable(Transportable t) {
        Cargo cargo = tFind(t);
        return (cargo == null) ? false : tRemove(cargo);
    }

    /**
     * Retargets a transportable that should already be on board the carrier.
     *
     * @param t The <code>Transportable</code> to retarget.
     * @return True if the retargeting succeeded.
     */
    public boolean requeueTransportable(Transportable t) {
        Cargo cargo = tFind(t);
        return (cargo == null) ? queueTransportable(t, false)
            : requeueCargo(cargo);
    }

    /**
     * Wrapper for queueCargo.
     * Public for the benefit of EuropeanAIPlayer.allocateTransportables
     * and CashInTreasureTrain.doMission.
     *
     * @param t The <code>Transportable</code> to add.
     * @param requireMatch Fail if an existing destination is not matched.
     * @return True if the transportable was queued.
     */
    public boolean queueTransportable(Transportable t, boolean requireMatch) {
        Cargo cargo = makeCargo(t);
        return (cargo == null) ? false : queueCargo(cargo, requireMatch);
    }

    /**
     * Gets rid of a transportable, possibly dumping it.
     *
     * @param t The <code>Transportable</code> to dump.
     * @param force If true, disband units or dump goods at sea.
     * @return True if the transportable was dumped.
     */
    private boolean dumpTransportable(Transportable t, boolean force) {
        if (t instanceof AIUnit) {
            AIUnit aiu = (AIUnit)t;
            return aiu.leaveTransport();
                
        } else if (t instanceof AIGoods) {
            final Unit carrier = getUnit();
            final AIUnit aiCarrier = getAIUnit();
            final Location here = carrier.getLocation();
            final Settlement settlement = carrier.getSettlement();

            AIGoods aig = (AIGoods)t;
            if (settlement != null) {
                if (aig.leaveTransport(null)) {
                    logger.finest(tag + " dumped " + aig
                        + " at " + settlement.getName() + ": " + this);
                } else {
                    logger.finest(tag + " dump " + aig
                        + " at " + settlement.getName() + " failed: " + this);
                }
            } else if (force) {
                logger.warning(tag + " forcing dump(goods) " + aig
                    + " at " + here.toShortString() + ": " + toFullString());
                return AIMessage.askUnloadCargo(aiCarrier, aig.getGoods());
            } else {
                logger.warning(tag + " dump ignored " + aig
                    + " at " + here.toShortString()
                    + ": " + toFullString());
                return false;
            }

        } else throw new RuntimeException("Bogus transportable: " + t);

        return true;
    }

    // End of public Transportable manipulations


    // Fake Transportable interface, noop as carriers are not transported.

    /**
     * {@inheritDoc}
     */
    @Override
    public Tile getTransportDestination() {
        return null; // The carrier unit is never transported!
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getTransportPriority() {
        return 0; // Override with no priority.
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
        this.target = target;
    }

    /**
     * {@inheritDoc}
     */
    public Location findTarget() {
        // A noop.  The target is defined by the cargoes.
        return null;
    }

    /**
     * Why would an TransportMission be invalid with the given unit?
     *
     * @param aiUnit The <code>AIUnit</code> to test.
     * @return A reason why the mission would be invalid with the unit,
     *     or null if none found.
     */
    private static String invalidMissionReason(AIUnit aiUnit) {
        String reason = invalidAIUnitReason(aiUnit);
        return (reason != null)
            ? reason
            : (!aiUnit.getUnit().isCarrier())
            ? "unit-not-a-carrier"
            : null;
    }

    /**
     * Why would this mission be invalid with a given cargo?
     * Checks the cargo locations.
     * A location becomes invalid if:
     *   - step is a null location
     *   - step is disposed
     *   - step is a captured settlement
     *
     * @param aiUnit The <code>AIUnit</code> to test.
     * @param cargo The <code>Cargo</code> to test.
     * @return A reason why the mission would be invalid,
     *     or null if none found.
     */
    private static String invalidCargoReason(AIUnit aiUnit, Cargo cargo) {
        final Unit carrier = aiUnit.getUnit();
        final Player owner = carrier.getOwner();
        final Transportable t = cargo.getTransportable();
        final Locatable l = t.getTransportLocatable();
        if (l == null) {
            return "null-transportable";
        } else if (l.getLocation() == carrier) {
            ; // OK so far
        } else if (l.getLocation() instanceof Unit) {
            return "transportable-on-other-carrier";
        } else {
            Location src = t.getTransportSource();
            if (src == null) {
                return "transportable-source-missing";
            } else if (((FreeColGameObject)src).isDisposed()) {
                return "transportable-source-disposed";
            } else if ((src instanceof Settlement)
                && ((Settlement)src).getOwner() != null
                && !owner.owns((Settlement)src)) {
                return "transportable-source-captured";
            }
        }
        Location dst = t.getTransportDestination();
        if (dst == null) {
            // Destination is null if we have arrived, but that should not
            // invalidate a transport mission as we still have to unload!
            if (!Map.isSameLocation(cargo.getTarget(),
                                    carrier.getLocation())) {
                return "transportable-destination-failure";
            }
        } else if (((FreeColGameObject)dst).isDisposed()) {
            return "transportable-destination-disposed";
        } else if (((dst instanceof Settlement)
                && ((Settlement)dst).getOwner() != null
                && !owner.owns((Settlement)dst))) {
            return "transportable-destination-captured";
        }
        return (t instanceof AIUnit) ? invalidAIUnitReason((AIUnit)t)
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
        return ((reason = invalidMissionReason(aiUnit)) != null)
            ? reason
            : (loc instanceof Europe || loc instanceof Colony)
            ? invalidTargetReason(loc, aiUnit.getUnit().getOwner())
            : (loc instanceof Tile)
            ? null
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

    /**
     * {@inheritDoc}
     */
    public String invalidReason() {
        final AIUnit aiUnit = getAIUnit();
        String reason = invalidReason(aiUnit, getTarget());
        Cargo cargo;
        return (reason != null) ? reason
            : ((cargo = tFirst()) == null) ? null
            : invalidCargoReason(aiUnit, cargo);
    }

    // Not a one-time mission, omit isOneTime().

    /**
     * {@inheritDoc}
     */
    public Mission doMission(LogBuilder lb) {
        lb.add(tag);
        checkCargoes(lb);

        String reason = invalidReason();
        if (reason != null) {
            lbBroken(lb, reason);
            retarget(); // Try to recover
            if ((reason = invalidReason()) != null) {
                checkCargoes(lb);
                optimizeCargoes(lb);
                if ((reason = invalidReason()) != null) {
                    lbBroken(lb, reason);
                    return null;
                }
            }
            lb.add(", recovered to ", getTarget());
        }

        final EuropeanAIPlayer euaip = getEuropeanAIPlayer();
        final AIUnit aiCarrier = getAIUnit();
        final Unit unit = getUnit();
        final CostDecider fallBackDecider
            = CostDeciders.avoidSettlementsAndBlockingUnits();
        CostDecider costDecider = CostDeciders.defaultCostDeciderFor(unit);
        for (;;) {
            Unit.MoveType mt = travelToTarget(target, costDecider, lb);
            switch (mt) {
            case MOVE_HIGH_SEAS: case MOVE_NO_MOVES: case MOVE_NO_REPAIR:
                return this;

            case MOVE_NO_TILE: // Can happen when another unit blocks a river
                moveRandomly(tag, null);
                unit.setMovesLeft(0);
                return this;

            case ATTACK_UNIT:
                Location blocker = resolveBlockage(aiCarrier, target);
                if (blocker instanceof Unit && shouldAttack((Unit)blocker)) {
                    AIMessage.askAttack(aiCarrier,
                        unit.getTile().getDirection(blocker.getTile()));
                    lbAttack(lb, blocker);
                    return this;
                }
                // Fall through
            case MOVE_NO_ATTACK_CIVILIAN:
                // TODO: See if the transportable can get around the
                // blockage using its own path finding.
                if (unit.getTile().isAdjacent(target.getTile())
                    || costDecider == fallBackDecider) {
                    moveRandomly(tag, null);
                    unit.setMovesLeft(0);
                    lbDodge(lb, unit);
                    return this;
                }
                costDecider = fallBackDecider; // Retry
                lb.add(", retry blockage at ", unit.getLocation());
                break;

            case MOVE:
                if (tSize() > 0) {
                    // Arrived at a target.  Deliver what can be
                    // delivered.  Check other deliveries, we might be
                    // in port so this is a good time to decide to
                    // fail to deliver something.
                    lb.add(", delivering:");
                    List<Cargo> cont = new ArrayList<Cargo>();
                    List<Cargo> curr = tClear();
                    for (Cargo cargo : curr) {
                        CargoResult result = (cargo.getMode().isCollection())
                            ? CargoResult.TCONTINUE
                            : tryCargo(cargo);
                        lb.add(" ", cargo, "=", result);
                        switch (result) {
                        case TCONTINUE:
                        case TRETRY: // will check again below
                            cont.add(cargo);
                            break;
                        case TDONE:
                            cargo.clear();
                            break;
                        case TFAIL: // failures will be retargeted below
                            break;
                        case TNEXT:
                            throw new IllegalStateException("Can not happen");
                        }
                    }
                    curr.clear();
                    // Rebuild the cargo list with the original members,
                    // less the transportables that were dropped.
                    tSet(cont, true);
                    checkCargoes(lb);
                    optimizeCargoes(lb); // This will retarget failures

                    // Now try again, this time collecting as well as
                    // delivering.
                    lb.add(", collecting:");
                    cont.clear();
                    List<Cargo> next = new ArrayList<Cargo>();
                    curr = tClear();
                    for (Cargo cargo : curr) {
                        CargoResult result = (cargo.getMode().isCollection())
                            ? tryCargo(cargo)
                            : CargoResult.TCONTINUE;
                        lb.add(" ", cargo, "=", result);
                        switch (result) {
                        case TCONTINUE:
                            cont.add(cargo);
                            break;
                        case TDONE:
                            cargo.clear();
                            break;
                        case TNEXT:
                            next.add(cargo);
                            break;
                        case TRETRY:
                            if (cargo.retry()) { // Can not reach the target.
                                next.add(cargo); // Try again next turn.
                                break;
                            }
                            // Fall through
                        case TFAIL:
                            break;
                        }
                    }
                    curr.clear();

                    // Rebuild the cargo list with the original members,
                    // less the transportables that were dropped.
                    tSet(cont, true);

                    // Add the new and blocked cargoes incrementally with
                    // the current arrangement, which is likely to put them
                    // at the end.
                    while (!next.isEmpty()) {
                        queueCargo(next.remove(0), false);
                    }
                }

                // See if the transportables need replenishing.
                // First try to collect more transportables at the current
                // location, then just add the best transportable for this
                // carrier.
                Location here = upLoc(unit.getLocation());
                List<Transportable> tl = euaip.getTransportablesAt(here);
                if (tl != null) {
                    for (Transportable t : tl) {
                        if (destinationCapacity() <= 0) break;
                        if (queueTransportable(t, true)) {
                            euaip.claimTransportable(t, here);
                        }
                    }
                }
                for (int n = destinationCapacity(); n > 0; n--) {
                    Transportable t = getBestTransportable(unit);
                    if (t == null) break;
                    if (queueTransportable(t, false)) {
                        euaip.claimTransportable(t);
                    }
                }

                retarget();
                if ((reason = invalidReason()) != null) {
                    lbBroken(lb, reason);
                    return null;
                } else if (unit.isAtLocation(target)) {
                    lb.add(", waiting at ", target, ".");
                    return this;
                }
                break;

            default:
                lbMove(lb, unit, mt);
                return this;
            }
        }
    }


    // Serialization

    private static final String CARGO_TAG = "cargo";
    private static final String CARRIER_TAG = "carrier";
    private static final String MODE_TAG = "mode";
    private static final String TARGET_TAG = "target";
    private static final String TURNS_TAG = "turns";
    private static final String TRIES_TAG = "tries";
    private static final String SPACELEFT_TAG = "space";
    // @compat 0.10.5
    private static final String OLD_TRANSPORTABLE_TAG = "transportable";
    // end @compat


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        if (target != null) {
            xw.writeLocationAttribute(TARGET_TAG, target);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        final AIUnit aiCarrier = getAIUnit();
        for (Cargo cargo : tCopy()) {
            // Sanity check first.  Another nation might have captured
            // or destroyed a target colony.
            String reason = cargo.check(aiCarrier);
            if (reason != null) {
                removeCargo(cargo, "serial-fail");
                continue;
            }
            // Do not bother writing cargoes that will be dumped.
            // On restore, checkCargoes will work out what to do with them.
            if (cargo.getMode() != CargoMode.DUMP) {
                xw.writeStartElement(CARGO_TAG);

                AIObject aio = (AIObject)cargo.getTransportable();
                xw.writeAttribute(ID_ATTRIBUTE_TAG, aio);

                xw.writeAttribute(CARRIER_TAG, cargo.getCarrier());

                xw.writeAttribute(MODE_TAG, cargo.getMode());
                
                if (cargo.getTarget() != null) {
                    xw.writeLocationAttribute(TARGET_TAG, cargo.getTarget());
                }

                xw.writeAttribute(TURNS_TAG, cargo.getTurns());

                xw.writeAttribute(TRIES_TAG, cargo.getTries());

                xw.writeAttribute(SPACELEFT_TAG, cargo.getSpaceLeft());

                xw.writeEndElement();
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
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        tClear();

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final Game game = getGame();
        final String tag = xr.getLocalName();

        if (CARGO_TAG.equals(tag)) {
            String tid = xr.readId();
            AIObject aio = null;
            if (tid != null) {
                if ((aio = getAIMain().getAIObject(tid)) == null) {
                    if (tid.startsWith(Unit.getXMLElementTagName())) {
                        aio = new AIUnit(getAIMain(), tid);
                    } else if (tid.startsWith(AIGoods.getXMLElementTagName())) {
                        aio = new AIGoods(getAIMain(), tid);
                    }
                }
            }
            if (aio == null) {
                throw new XMLStreamException("Transportable expected: " + tid);
            }

            Unit carrier = xr.getAttribute(game, CARRIER_TAG,
                                           Unit.class, (Unit)null);

            CargoMode mode = xr.getAttribute(MODE_TAG, 
                                             CargoMode.class, CargoMode.DUMP);
            
            Location target = xr.getLocationAttribute(game, TARGET_TAG, false);
            
            int turns = xr.getAttribute(TURNS_TAG, -1);
            
            int tries = xr.getAttribute(TRIES_TAG, 0);
            
            int spaceLeft = xr.getAttribute(SPACELEFT_TAG, -1);
            
            tAdd(new Cargo((Transportable)aio, carrier, mode,
                           target, turns, tries, spaceLeft), -1);
            xr.closeTag(CARGO_TAG);

        // @compat 0.10.5
        } else if (OLD_TRANSPORTABLE_TAG.equals(tag)) {
            // Ignore the old format, let checkCargoes sort it out
            xr.closeTag(OLD_TRANSPORTABLE_TAG);
        // end @compat

        } else {
            super.readChild(xr);
        }
    }

    /**
     * More verbose version of toString().
     *
     * @return A summary of this mission including its transportables.
     */
    public String toFullString() {
        LogBuilder lb = new LogBuilder(64);
        lb.add(this);
        for (Cargo cargo : tCopy()) lb.add("\n  ->", cargo);
        return lb.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "transportMission".
     */
    public static String getXMLElementTagName() {
        return "transportMission";
    }
}
