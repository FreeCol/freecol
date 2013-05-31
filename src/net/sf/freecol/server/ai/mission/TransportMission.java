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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Locatable;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.pathfinding.CostDecider;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.util.Utils;
import net.sf.freecol.server.ai.AIGoods;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIObject;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.ai.EuropeanAIPlayer;
import net.sf.freecol.server.ai.Transportable;


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

        /**
         * How much space would be needed to add this transportable?
         *
         * @return The extra space required.
         */
        public int getNewSpace() {
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
         * Compares this cargo to another.  Cargoes that reduce the
         * cargo amount should sort before those that do not.
         *
         * @param other The other <code>Cargo</code> to compare to.
         * @return A comparison result.
         */
        public int compareTo(Cargo other) {
            return getNewSpace() - other.getNewSpace();
        }

        /**
         * Sets the target for this cargo, possibly also changing its mode.
         *
         * @return A reason the targeting failed, null if it succeeded.
         */
        public String setTarget() {
            Location dst = transportable.getTransportDestination();
            if (dst == null) return "no-destination";
            dst = upLoc(dst);
            PathNode path, drop;

            if (transportable instanceof AIUnit) {
                final Unit unit = ((AIUnit)transportable).getUnit();
                if (unit.getLocation() == carrier) {
                    // Can the carrier deliver the unit to the target?
                    if ((path = unit.findPath(carrier.getLocation(), dst,
                                              carrier, null)) == null) {
                        return "no-deliver " + unit + "/" + carrier
                            + " -> " + dst;
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
                        return "no-collect " + unit + "/" + carrier
                            + " -> " + dst;
                    }
                    if ((drop = path.getCarrierMove()) == null) {
                        return "carrier not needed for " + unit.toString();
                    }
                    // TODO: proper rendezvous paths, unit needs
                    // to modify its target too!
                    if ((path = carrier.findPath(drop.getLocation())) == null) {
                        return "carrier can not reach collection point "
                            + carrier + " -> "
                            + ((FreeColGameObject)drop.getLocation());
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
                        return "no-deliver for " + carrier + " -> " + dst;
                    } else {
                        this.mode = CargoMode.UNLOAD;
                        this.turns = path.getLastNode().getTotalTurns();
                        this.target = upLoc(path.getLastNode().getLocation());
                    }
                } else {
                    path = carrier.findPath(goods.getLocation());
                    if (path == null) {
                        return "no-collect for " + carrier + " -> " + dst;
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
            String reason = invalidReason(aiCarrier, target);
            if (reason != null) return reason;

            Locatable l = transportable.getTransportLocatable();
            if (l == null) return "null locatable: " + transportable.toString();

            if (l instanceof FreeColGameObject
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
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(64);
            sb.append("[").append(transportable.toString())
                .append(" mode=").append(mode)
                .append((mode.isCollection()) ? " from " : " to ").append(target)
                .append(" ").append(turns).append("/").append(tries)
                .append(" space=").append(spaceLeft)
                .append((wrapped == null) ? "" : " wrap")
                .append("]");
            return sb.toString();
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

        checkCargoes(false);
        retarget();
        logger.finest(tag + " begins: " + toFullString());
        uninitialized = false;
    }

    /**
     * Creates a new <code>TransportMission</code> and reads the given
     * element.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public TransportMission(AIMain aiMain, AIUnit aiUnit, XMLStreamReader in)
        throws XMLStreamException {
        super(aiMain, aiUnit);

        readFromXML(in);
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
        return (carrier.isNaval()) ? carrier.findOurNearestPort()
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
     * @return The old cargoes list.
     */
    private List<Cargo> tSet(List<Cargo> nxt) {
        List<Cargo> old = tClear();
        synchronized (cargoes) {
            cargoes.addAll(nxt);
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
        Cargo cargo;
        synchronized (cargoes) {
            cargo = (cargoes.isEmpty()) ? null : cargoes.get(0);
        }
        return cargo;
    }

    /**
     * Adds a cargo to the cargoes list.
     *
     * @param cargo The <code>Cargo</code> to add.
     * @param index The position to add it.
     * @return True if the addition succeeded.
     */
    private boolean tAdd(Cargo cargo, int index) {
        if (tFind(cargo.getTransportable()) != null) return false;
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
        for (Cargo cargo : cargoes) {
            holds += cargo.getNewSpace();
            cargo.setSpaceLeft(maxHolds - holds);
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
        Location loc = (cargo != null) ? cargo.getTarget()
            : ((path = getTrivialPath()) == null) ? null
            : upLoc(path.getLastNode().getLocation());
        setTarget(loc);
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
            reason = "carrier " + carrier + " can not carry";
        } else {
            cargo = new Cargo(t, carrier);
            reason = cargo.setTarget();
        }
        if (reason == null) return cargo;
        logger.finest("Failed to remake cargo (" + reason + "): " + t);
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
            logger.finest(tag + " add " + cargo.toString()
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

        if (result) {
            logger.finest(tag + " removed " + cargo.toString()
                + " (" + reason + "): " + toFullString());
        } else {
            logger.finest(tag + " remove " + cargo.toString()
                + " failed: " + toFullString());
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
        final List<Cargo> ts = tCopy();
        final int newSpace = cargo.getTransportable().getSpaceTaken();

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
            if (!ts.isEmpty() && !isCarrying(t)) {
                int holds = maxHolds - ts.get(ts.size()-1).getSpaceLeft()
                    + newSpace;
                if (holds < 0 || holds > maxHolds) return false;
            }
        }
        return addCargo(cargo, candidate);
    }

    /**
     * Retarget an existing cargo.
     *
     * @param cargo The <code>Cargo</code> to retarget.
     * @return True if the retargeting succeeded.
     */
    private boolean retargetCargo(Cargo cargo) {
        Transportable t = cargo.getTransportable();
        Location dst = t.getTransportDestination();
        int result = 0;
        if (tRemove(cargo)) {
            result++;
            Cargo next = makeCargo(t);
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
        }
        retarget();

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
                + " to " + dst + ": " + toFullString());
            break;
        }
        return result == 3;
    }

    /**
     * Checks for invalid cargoes, and units and goods on board but
     * not in the cargoes list.  On exit from this routine, every
     * cargo on board should be on the cargoes list but the list is
     * not necessarily going to be in a sensible order.
     *
     * @param complain Complain if unexpected units are found.
     */
    private void checkCargoes(boolean complain) {
        final Unit carrier = getUnit();
        if (carrier.isAtSea()) return; // Let it emerge.

        final AIUnit aiCarrier = getAIUnit();
        List<Unit> unitsPresent = new ArrayList<Unit>(carrier.getUnitList());
        List<Goods> goodsPresent
            = new ArrayList<Goods>(carrier.getGoodsContainer().getCompactGoods());
        String reason;
        // Check all cargoes are valid.
        // Collect any non-cargo units and goods.
        for (Cargo cargo : tCopy()) {
            if ((reason = cargo.check(aiCarrier)) != null) {
                removeCargo(cargo, reason);
                continue;
            }
            PathNode path = carrier.findPath(cargo.getTarget());
            if (path == null && !cargo.retry()) {
                removeCargo(cargo, "no path " + cargo.getTarget());
                continue;
            }
            // Redo the cargo in case the pickup/drop node needs
            // updating in response to changes in moves left or the
            // map situation.
            Transportable t = cargo.getTransportable();
            if (carrier.getTile() != null) {
                if ((reason = cargo.setTarget()) != null && !cargo.retry()) {
                    removeCargo(cargo, "can not progress (" + reason
                        + ") to " + t.getTransportDestination());
                    continue;
                }
            }
            
            if (t instanceof AIUnit) {
                unitsPresent.remove(((AIUnit)t).getUnit());

            } else if (t instanceof AIGoods) {
                Goods goods = ((AIGoods)t).getGoods();
                int i = 0;
                while (i < goodsPresent.size()) {
                    Goods g = goodsPresent.get(i);
                    if (goods.getType() == g.getType()) {
                        // TODO: handle size?
                        goodsPresent.remove(i);
                    } else {
                        i++;
                    }
                }

            } else throw new IllegalStateException("Bogus transportable: "+t);
        }

        // Ask the parent player to retarget transportables that are not
        // on the list.
        EuropeanAIPlayer euaip = getEuropeanAIPlayer();
        List<Transportable> drop = new ArrayList<Transportable>();
        for (Unit u : unitsPresent) {
            AIUnit aiu = getAIMain().getAIUnit(u);
            if (aiu == null) throw new IllegalStateException("Bogus:" + u);
            if (complain) {
                logger.warning(tag + " found unexpected unit " + aiu
                    + " aboard: " + toFullString());
            }
            if (euaip.retargetCargo(aiu, aiCarrier, tCopy())) {
                Cargo cargo = makeCargo(aiu);
                if (cargo == null) {
                    logger.warning("COULD NOT REMAKE CARGO: " + aiu);
                    drop.add(aiu);
                } else if (!queueCargo(cargo, false)) {
                    logger.warning("COULD NOT QUEUE CARGO: " + cargo);
                    drop.add(aiu);
                }
            }
        }
        for (Goods g : goodsPresent) {
            AIGoods aig = new AIGoods(getAIMain(), carrier, g.getType(),
                g.getAmount(), null);
            if (complain) {
                logger.warning(tag + " found unexpected goods " + aig
                    + " aboard: " + toFullString());
            }
            if (!euaip.retargetCargo(aig, aiCarrier, tCopy())
                || !queueTransportable(aig, false)) {
                drop.add(aig);
            }
        }

        if (!drop.isEmpty()) {
            PathNode path = getTrivialPath();
            Location end = (path == null) ? null
                : path.getLastNode().getLocation();
            if (path == null || carrier.isAtLocation(end)) {
                while (!drop.isEmpty()) {
                    Transportable t = drop.remove(0);
                    dumpTransportable(t, true);
                }
            } else {
                String log = tag + " will dump ";
                for (Transportable t : drop) log += " " + t;
                logger.warning(log + " at " + ((FreeColGameObject)end)
                    + ": " + toFullString());
                while (!drop.isEmpty()) {
                    Transportable t = drop.remove(0);
                    Cargo cargo = new Cargo(t, carrier, end);
                    queueCargo(cargo, false);
                }
            }
        }
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
                    + " at " + here + ": " + this);
                return CargoResult.TFAIL;
            }
            logger.finest(tag + " loaded " + t
                + " at " + here + ": " + this);

            if ((reason = cargo.setTarget()) == null) {
                return CargoResult.TNEXT;
            }
            logger.finest(tag + " next fail(" + reason + ") " + t
                + " at " + here + ": " + this);
            return CargoResult.TFAIL;

        case UNLOAD:
            if (!Map.isSameLocation(here, cargo.getTarget())) {
                return CargoResult.TCONTINUE;
            }
            if (t.leaveTransport(null)) {
                logger.finest(tag + " completed (unload) of " + t
                    + " at " + here + ": " + this);
            } else {
                logger.warning(tag + " failed to unload " + t
                    + " at " + here + ": " + this);
                return CargoResult.TFAIL;
            }
            return CargoResult.TDONE;

        case PICKUP:
            if (!Map.isSameLocation(here, cargo.getTarget())) {
                return CargoResult.TCONTINUE;
            }
            if (isCarrying(t)) {
                logger.finest(tag + " picked up " + t
                    + " at " + here + ": " + this);
                if ((reason = cargo.setTarget()) == null) {
                    return CargoResult.TNEXT;
                }
                logger.finest(tag + " next fail(" + reason + ") " + t
                    + " at " + here + ": " + this);
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
                    + " at " + carrier.getLocation() + ": " + this);
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
     */
    private void optimizeCargoes() {
        checkCargoes(true);
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
            tSet(unwrapCargoes(best));
            tSpace();
        } else {
            tSet(unwrapCargoes(ts));
        }
        retarget();
        if (best != null) {
            logger.finest(tag + " post-optimize " + getTarget()
                + ": " + toFullString());
        }
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
    private boolean addTransportable(Transportable t, int index) {
        if (tFind(t) != null) return false;

        AIUnit oldCarrier = t.getTransport();
        if (oldCarrier.getMission() instanceof TransportMission) {
            ((TransportMission)oldCarrier.getMission())
                .removeTransportable(t, "transferring to " + getUnit());
        }

        Cargo cargo = makeCargo(t);
        return (cargo == null) ? false : addCargo(cargo, index);
    }

    /**
     * Removes the given <code>Transportable</code> from the cargo list.
     *
     * @param t The <code>Transportable</code> to remove.
     * @param reason The reason for its removal (if null, do not log, it has
     *     already been mentioned).
     * @return True if the removal succeeded.
     */
    public boolean removeTransportable(Transportable t, String reason) {
        Cargo cargo = tFind(t);
        return (cargo == null) ? false : removeCargo(cargo, reason);
    }

    /**
     * Retargets a transportable that should already be on being transported.
     *
     * @param t The <code>Transportable</code> to retarget.
     * @return True if the retargeting succeeded.
     */
    public boolean retargetTransportable(Transportable t) {
        Cargo cargo = tFind(t);
        return (cargo == null) ? false : retargetCargo(cargo);
    }

    /**
     * Wrapper for queueCargo.
     * Public for the benefit of EuropeanAIPlayer.allocateTransportables.
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
        final Unit carrier = getUnit();
        final AIUnit aiCarrier = getAIUnit();
        final Location here = carrier.getLocation();
        Locatable l = t.getTransportLocatable();
        boolean canLeave = carrier.isInEurope()
            || carrier.getLocation().getSettlement() != null;

        if (t instanceof AIUnit) {
            AIUnit aiu = (AIUnit)t;
            Direction direction = null;
            if (!canLeave) {
                for (Tile tile : carrier.getTile().getSurroundingTiles(1)) {
                    if (tile.isLand()
                        && aiu.getUnit().getMoveType(tile).isProgress()) {
                        direction = carrier.getTile().getDirection(tile);
                        canLeave = true;
                        break;
                    }
                }
            }
            if (canLeave && aiu.leaveTransport(direction)) {
                logger.finest(tag + " dumped " + aiu
                    + " at " + here + ": " + toFullString());
            } else {
                if (!force) {
                    logger.warning(tag + " failed to dump " + aiu
                        + " at " + here + ": " + toFullString());
                    return false;
                }
                logger.warning(tag + " forcing dump(disband) " + aiu
                    + " at " + here + ": " + toFullString());
                return AIMessage.askDisband(aiu);
            }
                
        } else if (t instanceof AIGoods) {
            AIGoods aig = (AIGoods)t;
            if (canLeave && aig.leaveTransport(null)) {
                logger.finest(tag + " dumped " + aig
                    + " at " + here + ": " + toFullString());
            } else {
                if (!force) {
                    logger.warning(tag + " failed to dump " + aig
                        + " at " + here + ": " + toFullString());
                    return false;
                }
                logger.warning(tag + " forcing dump(goods) " + aig
                    + " at " + here + ": " + toFullString());
                return AIMessage.askUnloadCargo(aiCarrier, aig.getGoods());
            }

        } else throw new IllegalStateException("Bogus transportable: " + t);

        return true;
    }

    // End of public Transportable manipulations


    // Fake Transportable interface

    /**
     * Returns the destination of a required transport.
     *
     * @return Always null, we never transport carrier units.
     */
    @Override
    public Tile getTransportDestination() {
        return null;
    }

    /**
     * Returns the priority of getting the unit to the transport destination.
     *
     * @return 0, the carrier is never transported.
     */
    @Override
    public int getTransportPriority() {
        return 0;
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
     * Why would this mission be invalid with a given transportable?
     * Checks the transportable locations.
     * A location becomes invalid if:
     *   - step is a null location
     *   - step is disposed
     *   - step is a captured settlement
     *
     * @param aiUnit The <code>AIUnit</code> to test.
     * @param t The <code>Transportable</code> to test.
     * @return A reason why the mission would be invalid,
     *     or null if none found.
     */
    private static String invalidTransportableReason(AIUnit aiUnit, 
                                                     Transportable t) {
        final Unit carrier = aiUnit.getUnit();
        final Player owner = carrier.getOwner();
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
            return "transportable-destination-missing";
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
            : invalidTransportableReason(aiUnit, cargo.getTransportable());
    }

    // Not a one-time mission, omit isOneTime().

    /**
     * {@inheritDoc}
     */
    public void doMission() {
        checkCargoes(true);
        String reason = invalidReason();
        if (reason != null) {
            retarget(); // Try to recover
            if ((reason = invalidReason()) != null) {
                optimizeCargoes();
                if ((reason = invalidReason()) != null) {
                    logger.finest(tag + " broken(" + reason + "): "
                        + toFullString());
                    return;
                }
            }
        }

        final AIUnit aiCarrier = getAIUnit();
        final Unit carrier = getUnit();
        final EuropeanAIPlayer euaip = getEuropeanAIPlayer();
        final CostDecider fallBackDecider
            = CostDeciders.avoidSettlementsAndBlockingUnits();
        CostDecider costDecider = CostDeciders.defaultCostDeciderFor(carrier);
        for (;;) {
            logger.info(tag + " travelling: " + toFullString());
            Unit.MoveType mt = travelToTarget(tag, target, costDecider);
            switch (mt) {
            case MOVE_NO_MOVES: case MOVE_HIGH_SEAS:
                return;
            case ATTACK_UNIT:
                Location blocker = resolveBlockage(aiCarrier, target);
                if (blocker instanceof Unit && shouldAttack((Unit)blocker)) {
                    logger.finest(tag + " attacking " + blocker
                        + ": " + this);
                    AIMessage.askAttack(aiCarrier,
                        carrier.getTile().getDirection(blocker.getTile()));
                    break;
                }
                // Fall through
            case MOVE_NO_ATTACK_CIVILIAN:
                // TODO: See if the transportable can get around the
                // blockage using its own path finding.
                if (carrier.getTile().isAdjacent(target.getTile())
                    || costDecider == fallBackDecider) {
                    moveRandomly(tag, null);
                    carrier.setMovesLeft(0);
                    logger.finest(tag + " blocked at " + carrier.getLocation()
                        + ", moving randomly: " + this);
                    return;
                }
                logger.finest(tag + " blocked at " + carrier.getLocation()
                    + ", retrying: " + this);
                costDecider = fallBackDecider; // Retry
                break;
            case MOVE:
                if (tSize() > 0) {
                    // Arrived at a target.  Deliver what can be
                    // delivered.  Check other deliveries, we might be
                    // in port so this is a good time to decide to
                    // fail to deliver something.
                    String logMe = tag + " delivery-pass:";
                    List<Cargo> cont = new ArrayList<Cargo>();
                    for (Cargo cargo : tCopy()) {
                        CargoResult result = (cargo.getMode().isCollection())
                            ? CargoResult.TCONTINUE
                            : tryCargo(cargo);
                        logMe += "\n    " + cargo.toString() + " = " + result;
                        switch (result) {
                        case TCONTINUE:
                        case TRETRY: // will check again below
                            cont.add(cargo);
                            break;
                        case TDONE:
                        case TFAIL: // failures will be retargeted below
                            break;
                        case TNEXT:
                            throw new IllegalStateException("Can not happen");
                        }
                    }
                    logger.finest(logMe);
                    // Rebuild the cargo list with the original members,
                    // less the transportables that were dropped.
                    tSet(cont);
                    tSpace();
                    optimizeCargoes(); // This will retarget failures

                    // Now try again, this time collecting as well as
                    // delivering.
                    logMe = tag + " collection-pass:";
                    cont.clear();
                    List<Cargo> next = new ArrayList<Cargo>();
                    for (Cargo cargo : tCopy()) {
                        CargoResult result = (cargo.getMode().isCollection())
                            ? tryCargo(cargo)
                            : CargoResult.TCONTINUE;
                        logMe += "\n    " + cargo.toString() + " = " + result;
                        switch (result) {
                        case TCONTINUE:
                            cont.add(cargo);
                            break;
                        case TDONE:
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
                    logger.finest(logMe);

                    // Rebuild the cargo list with the original members,
                    // less the transportables that were dropped.
                    tSet(cont);
                    tSpace();

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
                if (destinationCapacity() > 0) {
                    Location here = upLoc(carrier.getLocation());
                    List<Transportable> tl = euaip.getTransportablesAt(here);
                    if (tl != null) {
                        for (Transportable t : tl) {
                            if (queueTransportable(t, true)) {
                                euaip.claimTransportable(t, here);
                            }
                        }
                    }
                }
                for (int n = destinationCapacity(); n > 0; n--) {
                    Transportable t = euaip.getBestTransportable(carrier);
                    if (t == null) break;
                    if (queueTransportable(t, false)) {
                        euaip.claimTransportable(t);
                    }
                }

                retarget();
                if (carrier.isAtLocation(target)) {
                    logger.finest(tag + " waiting at " + target
                        + ": " + toFullString());
                    return;
                }
                break;
            default:
                logger.warning(tag + " unexpected move " + mt
                    + ": " + toFullString());
                return;
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
    public void toXML(XMLStreamWriter out) throws XMLStreamException {
        if (isValid()) {
            toXML(out, getXMLElementTagName());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);

        if (target != null) {
            writeLocationAttribute(out, TARGET_TAG, target);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        final AIUnit aiCarrier = getAIUnit();
        for (Cargo cargo : tCopy()) {
            // Sanity check first.  Another nation might have captured
            // or destroyed a target colony.
            String reason = cargo.check(aiCarrier);
            if (reason != null) {
                removeCargo(cargo, reason);
                continue;
            }
            // Do not bother writing cargoes that will be dumped.
            // On restore, checkCargoes will work out what to do with them.
            if (cargo.getMode() != CargoMode.DUMP) {
                out.writeStartElement(CARGO_TAG);

                AIObject aio = (AIObject)cargo.getTransportable();
                writeAttribute(out, ID_ATTRIBUTE_TAG, aio);

                writeAttribute(out, CARRIER_TAG, cargo.getCarrier());

                writeAttribute(out, MODE_TAG, cargo.getMode());
                
                if (cargo.getTarget() != null) {
                    writeLocationAttribute(out, TARGET_TAG, cargo.getTarget());
                }

                writeAttribute(out, TURNS_TAG, cargo.getTurns());

                writeAttribute(out, TRIES_TAG, cargo.getTries());

                writeAttribute(out, SPACELEFT_TAG, cargo.getSpaceLeft());

                out.writeEndElement();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);

        target = findLocationAttribute(in, TARGET_TAG, getGame());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(XMLStreamReader in) throws XMLStreamException {
        // Clear containers
        tClear();

        super.readChildren(in);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        final Game game = getGame();
        final String tag = in.getLocalName();

        if (CARGO_TAG.equals(tag)) {
            String tid = readId(in);
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

            Unit carrier = getAttribute(in, CARRIER_TAG, getGame(),
                                        Unit.class, (Unit)null);

            CargoMode mode = getAttribute(in, MODE_TAG, 
                                          CargoMode.class, CargoMode.DUMP);
            
            Location target = findLocationAttribute(in, TARGET_TAG, game);
            
            int turns = getAttribute(in, TURNS_TAG, -1);
            
            int tries = getAttribute(in, TRIES_TAG, 0);
            
            int spaceLeft = getAttribute(in, SPACELEFT_TAG, -1);
            
            tAdd(new Cargo((Transportable)aio, carrier, mode,
                           target, turns, tries, spaceLeft), -1);
            closeTag(in, CARGO_TAG);

        // @compat 0.10.5
        } else if (OLD_TRANSPORTABLE_TAG.equals(tag)) {
            // Ignore the old format, let checkCargoes sort it out
            closeTag(in, OLD_TRANSPORTABLE_TAG);
        // end @compat

        } else {
            super.readChild(in);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(" -> ").append(target);
        return sb.toString();
    }

    /**
     * More verbose version of toString().
     *
     * @return A summary of this mission including its transportables.
     */
    public String toFullString() {
        StringBuilder sb = new StringBuilder(this.toString());
        for (Cargo cargo : tCopy()) {
            sb.append("\n  ->");
            sb.append(cargo.toString());
        }
        return sb.toString();
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "transportMission".
     */
    public static String getXMLElementTagName() {
        return "transportMission";
    }
}
