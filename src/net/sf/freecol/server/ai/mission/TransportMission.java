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

package net.sf.freecol.server.ai.mission;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Locatable;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.pathfinding.CostDecider;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.util.LogBuilder;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.server.ai.AIColony;
import net.sf.freecol.server.ai.AIGoods;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.ai.Cargo;
import net.sf.freecol.server.ai.EuropeanAIPlayer;
import net.sf.freecol.server.ai.TransportableAIObject;


/**
 * Mission for transporting units and goods on a carrier.
 *
 * @see net.sf.freecol.common.model.Unit Unit
 */
public class TransportMission extends Mission {

    private static final Logger logger = Logger.getLogger(TransportMission.class.getName());

    private static final String tag = "AI transport";

    private static enum CargoResult {
        TCONTINUE,  // Cargo should continue
        TDONE,      // Cargo completed successfully
        TFAIL,      // Cargo failed badly
        TNEXT,      // Cargo changed to its next state
        TRETRY      // Cargo has blocked, retry
    }

    /**
     * Insist transport lists remain simple by imposing an upper bound
     * on the distinct destination locations to visit.
     */
    private static final int DESTINATION_UPPER_BOUND = 4;

    private static final int MINIMUM_GOLD_TO_STAY_IN_EUROPE = 600;

    /** A list of <code>Cargo</code>s to work on. */
    private final List<Cargo> cargoes = new ArrayList<>();

    /** The current target location to travel to. */
    private Location target;


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     */
    public TransportMission(AIMain aiMain, AIUnit aiUnit) {
        super(aiMain, aiUnit, aiUnit.getTrivialTarget());
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
    }


    /**
     * Disposes of this <code>Mission</code>.
     */
    @Override
    public void dispose() {
        logger.finest(tag + " disposing (" + clearCargoes() + "): " + this
            //+ "\n" + net.sf.freecol.common.debug.FreeColDebugger.stackTraceToString()
            );
        super.dispose();
    }


    // Simple internal utilities

    /**
     * Checks if the carrier using this mission is carrying the given
     * <code>TransportableAIObject</code>.
     *
     * @param t The <code>TransportableAIObject</code> to check.
     * @return True if the carrier is carrying the transportable.
     */
    private boolean isCarrying(TransportableAIObject t) {
        return t != null && t.getLocation() == getUnit();
    }

    /**
     * Is a transportable waiting for delivery on the cargoes list?
     *
     * @param t The <code>TransportableAIObject</code> to check.
     * @return True if the transportable is queued in this mission.
     */
    public boolean isTransporting(TransportableAIObject t) {
        return tFind(t) != null;
    }

    /**
     * Decide if this unit has a good chance of defeating another.
     * If there is cargo aboard, be more conservative.
     *
     * FIXME: magic numbers to the spec.
     *
     * @param other The other <code>Unit</code> to attack.
     * @return True if the attack should proceed.
     */
    private boolean shouldAttack(Unit other) {
        if (invalidAttackReason(getAIUnit(),
                                other.getOwner()) != null) return false;
        final Unit carrier = getUnit();
        final CombatModel cm = getGame().getCombatModel();
        double offence = cm.getOffencePower(carrier, other)
            * ((carrier.hasCargo()) ? 0.3 : 0.80);
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
        synchronized (cargoes) {
            return new ArrayList<>(cargoes);
        }
    }

    /**
     * Clears the cargoes list.
     *
     * @return The old cargoes list.
     */
    private List<Cargo> tClear() {
        List<Cargo> old = new ArrayList<>();
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
        List<Cargo> old = tCopy();
        synchronized (cargoes) {
            cargoes.clear();
            for (Cargo c : nxt) {
                if (c.isValid()) cargoes.add(c);
            }
            if (setSpace) tSpace();
        }
        tRetarget();
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
     * Find a <code>Cargo</code> with the given
     * <code>TransportableAIObject</code>.
     *
     * @param t The <code>TransportableAIObject</code> to look for.
     * @return The <code>Cargo</code> found, or null if none found.
     */
    private Cargo tFind(TransportableAIObject t) {
        synchronized (cargoes) {
            return find(cargoes, c -> c.getTransportable() == t);
        }
    }

    /**
     * Gets the first cargo.
     *
     * @return The first valid cargo, or null if none found.
     */
    private Cargo tFirst() {
        synchronized (cargoes) {
            return find(cargoes, Cargo::isValid);
        }
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
        boolean change = false;
        synchronized (cargoes) {
            change = cargoes.isEmpty() || index == 0;
            if (index >= 0) {
                cargoes.add(index, cargo); 
            } else {
                cargoes.add(cargo);
            }
            tSpace();
        }
        if (change) tRetarget();
        return true;
    }

    /**
     * Remove a cargo from the cargoes list.
     *
     * @param cargo The <code>Cargo</code> to remove.
     * @return True if the remove succeeded.
     */
    private boolean tRemove(Cargo cargo) {
        boolean result = false, change = false;
        final TransportableAIObject t = cargo.getTransportable();
        synchronized (cargoes) {
            for (int i = 0; i < cargoes.size(); i++) {
                if (cargoes.get(i).getTransportable() == t) {
                    cargoes.remove(i);
                    tSpace();
                    change = i == 0;
                    result = true;
                    break;
                }
            }
        }
        if (change) tRetarget();
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
            if (!cargo.isValid()) continue;
            holds += cargo.getNewSpace();
            cargo.setSpaceLeft(maxHolds - holds);
        }
    }

    /**
     * Reset the carrier target after a change to the first cargo.
     */
    private void tRetarget() {
        Cargo c;
        synchronized (cargoes) {
            c = find(cargoes, Cargo::isValid);
        }
        setTarget(Location.upLoc((c == null) ? getAIUnit().getTrivialTarget()
                : c.getCarrierTarget()));
    }

    // Medium-level cargo and target manipulation, should be kept
    // private to TransportMission.

    /**
     * Count distinct non-adjacent destinations in a list of cargoes.
     *
     * @return The number of distinct destinations.
     */
    private int destinationCount() {
        Location now = null;
        int ret = 0;
        for (Cargo cargo : tCopy()) {
            if (!Map.isSameLocation(now, cargo.getCarrierTarget())) {
                ret++;
                now = cargo.getCarrierTarget();
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
     * @param t The <code>TransportableAIObject</code> to check.
     */
    private void dropTransportable(TransportableAIObject t) {
        AIUnit carrier = getAIUnit();
        if (t.getTransport() == carrier) t.setTransport(null);
    }

    /**
     * If this carrier is the not the current carrier of a
     * transportable, make it so.
     *
     * @param t The <code>TransportableAIObject</code> to check.
     */
    private void takeTransportable(TransportableAIObject t) {
        AIUnit carrier = getAIUnit();
        if (t.getTransport() != carrier) t.setTransport(carrier);
    }

    /**
     * Get the collection location for an uncollected transportable.
     *
     * Public so that mobile transportables (units) can move to the
     * collection point.
     *
     * @param t The <code>TransportableAIObject</code> to collect.
     * @return The collection <code>Location<code>, or null if not found.
     */
    public Location getTransportTarget(TransportableAIObject t) {
        if (isCarrying(t)) return null;
        Cargo cargo = tFind(t);
        return (cargo == null) ? null : cargo.getTransportTarget();
    }

    /**
     * Get the expected turns for an uncollected transport
     *
     * Public so that mobile transportables (units) can renege on
     * transport if they find themselves better able to get there
     * themselves.
     *
     * @param t The <code>TransportableAIObject</code> to collect.
     * @return The expected transport turns.
     */
    public int getTransportTurns(TransportableAIObject t) {
        if (isCarrying(t)) return INFINITY;
        Cargo cargo = tFind(t);
        return (cargo == null) ? INFINITY : cargo.getTurns();
    }

    /**
     * Wrap up the compatible cargoes in a list.
     * O(N^2) alas.
     *
     * @return A wrapped list of cargoes.
     */
    private List<Cargo> wrapCargoes() {
        List<Cargo> ts = tCopy();
        for (int i = 0; i < ts.size()-1; i++) {
            Cargo head = ts.get(i);
            while (i+1 < ts.size() && head.couldWrap(ts.get(i+1))) {
                head.wrap(ts.remove(i+1));
            }
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
            dropTransportable(cargo.getTransportable());
            log += " " + cargo;
        }
        tRetarget();
        return log;
    }

    /**
     * Is there nothing currently queued for this carrier?
     *
     * @return True if there is no work allocated to this carrier.
     */
    public boolean isEmpty() {
        return tSize() == 0;
    }

    /**
     * For a given transportable, work out where the carrier has to go to
     * advance the cargo (target), and what to do there (mode), allowing
     * a new <code>Cargo</code> to be defined.
     *
     * AIUnit cargo is harder than AIGoods, because AIUnits might have their
     * own inland paths, and thus we need to consider drop nodes.
     *
     * @param t The <code>TransportableAIObject</code> to consider.
     * @param lb A <code>LogBuilder</code> to log to.
     * @return A new <code>Cargo</code> defining the action to take
     *     with the <code>TransportableAIObject</code>, or null if impossible.
     */
    public Cargo makeCargo(TransportableAIObject t, LogBuilder lb) {
        final Unit carrier = getUnit();
        String reason;
        Cargo cargo = null;
        if (t.getTransportDestination() == null) {
            if (!isCarrying(t)) {
                reason = "null transport destination";
            } else {
                // Can happen with carriers with units transferred in
                // Spanish succession.
                PathNode path = carrier.getTrivialPath();
                if (path == null) {
                    reason = "null transport destination";
                } else {
                    try {
                        reason = null;
                        cargo = Cargo.newCargo(t, carrier,
                            path.getLastNode().getLocation(), true);
                    } catch (FreeColException fce) {
                        reason = fce.getMessage();
                        cargo = null;
                    }
                }
            }
        } else if (!isCarrying(t) && !t.carriableBy(carrier)) {
            reason = "carrier " + carrier.toShortString() + " can not carry";
        } else {
            try {
                reason = null;
                cargo = Cargo.newCargo(t, carrier);
            } catch (FreeColException fce) {
                reason = fce.getMessage();
                cargo = null;
            }
        }
        if (reason == null) {
            lb.add(", made ", cargo.toShortString());
            return cargo;
        } else {
            lb.add(", failed to make cargo for ", t, " (", reason, ")");
            return null;
        }
    }

    /**
     * Add the given Cargo to the cargoes list.
     *
     * @param cargo The <code>Cargo</code> to add.
     * @param index The index of where to add the cargo.
     * @param lb A <code>LogBuilder</code> to log to.
     * @return True if the cargo was added.
     */
    private boolean addCargo(Cargo cargo, int index, LogBuilder lb) {
        boolean result = tAdd(cargo, index);
        if (result) takeTransportable(cargo.getTransportable());

        if (result) {
            lb.add(", added ", cargo.toShortString(),
                   " at ", ((index < 0) ? "end" : Integer.toString(index)));
        } else {
            lb.add(", failed to add ", cargo.toShortString());
        }
        return result;
    }

    /**
     * Removes the given Cargo from the cargoes list.
     *
     * @param cargo The <code>Cargo</code> to remove.
     */
    private void removeCargo(Cargo cargo) {
        if (!tRemove(cargo)) {
            throw new RuntimeException("removeCargo " + cargo.toShortString());
        }
        dropTransportable(cargo.getTransportable());
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
     * @param t The <code>TransportableAIObject</code> to check.
     * @return True if there is space available for this transportable.
     */
    public boolean spaceAvailable(TransportableAIObject t) {
        final List<Cargo> ts = tCopy();
        final int newSpace = t.getSpaceTaken();

        for (int i = ts.size()-1; i >= 0; i--) {
            if (ts.get(i).getSpaceLeft() < newSpace) return false;
        }
        return true;
    }

   
    /**
     * Incrementally queue a cargo to the cargoes list.
     *
     * If the carrier is at the collection point favour immediate
     * collection.  Otherwise try to place it with other cargoes with
     * the same target, but do not break the space restrictions.  If
     * this does not work, it has to go at the end.
     *
     * @param cargo The new <code>Cargo</code> to add.
     * @param requireMatch Fail if an existing destination is not matched.
     * @param lb A <code>LogBuilder</code> to log to.
     * @return True if the cargo was queued.
     */
    private boolean queueCargo(Cargo cargo, boolean requireMatch,
                               LogBuilder lb) {
        final Unit carrier = getUnit();
        final int maxHolds = carrier.getCargoCapacity();
        final List<Cargo> ts = tCopy();
        final int newSpace = cargo.getNewSpace();
        int candidate = -1;

        if (ts.isEmpty() // Trivial case
            || (Map.isSameLocation(carrier.getLocation(), // Carrier here?
                                   cargo.getCarrierTarget())
                && cargo.canQueueAt(carrier, 0, ts))) {
            candidate = 0;
        }

        if (candidate < 0) { // Match an existing target?
            outer: for (int i = 0; i < ts.size(); i++) {
                Cargo tr = ts.get(i);
                if (Map.isSameLocation(tr.getCarrierTarget(),
                                       cargo.getCarrierTarget())) {
                    if (!cargo.canQueueAt(carrier, i, ts)) continue outer;
                    candidate = i;
                    break;
                }
            }
        }
        
        if (candidate < 0) { // Queue at end unless match required
            if (requireMatch) return false;
            candidate = ts.size();
        }
        return addCargo(cargo, candidate, lb);
    }

    /**
     * Dump a currently carried cargo.
     *
     * @param cargo The <code>Cargo</code> to dump.
     * @param lb A <code>LogBuilder</code> to log to.
     * @return True if the cargo is no longer on board and not on the
     *     transport list, or is on board but is scheduled to be dumped.
     */
    public boolean dumpCargo(Cargo cargo, LogBuilder lb) {
        TransportableAIObject t = cargo.getTransportable();
        if (isCarrying(t)) t.leaveTransport();
        if (!isCarrying(t) && tFind(t) != null) removeCargo(cargo);
        if (tFind(t) != null) {
            String reason = cargo.dump();
            if (reason != null) {
                lb.add(", dump failed(", reason, ")");
                return false;
            } else {
                lb.add(", dumping");
            }
        }
        return true;
    }

    /**
     * Requeue an existing cargo.  Typically done when the target changes.
     *
     * @param cargo The <code>Cargo</code> to requeue.
     * @param lb A <code>LogBuilder</code> to log to.
     * @return True if the queuing succeeded.
     */
    public boolean requeueCargo(Cargo cargo, LogBuilder lb) {
        final TransportableAIObject t = cargo.getTransportable();
        boolean ret = false;
        assert tFind(t) == cargo;
        String reason = cargo.update();
        if (reason != null) {
            lb.add(" requeue/update fail(", reason, ") ",
                   cargo.toShortString());
            dumpCargo(cargo, lb);
        } else if (!tRemove(cargo)) {
            lb.add(" requeue/remove fail ", cargo.toShortString());
        } else if (!queueCargo(cargo, false, lb)) {
            lb.add(" requeue/queue fail ", cargo.toShortString());
            dropTransportable(t);
        } else {
            lb.add(" requeued(", cargo.getTransportTarget(), ") ",
                   cargo.toShortString());
            takeTransportable(t);
            ret = true;
        }
        return ret;
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

        List<Unit> unitsPresent = carrier.getUnitList();
        List<Goods> goodsPresent = carrier.getCompactGoods();
        List<TransportableAIObject> todo = new ArrayList<>();
        List<TransportableAIObject> drop = new ArrayList<>();

        String reason;
        PathNode path;
        boolean dump;
        lb.add(" [check");
        for (Cargo cargo : tCopy()) {
            dump = false;
            TransportableAIObject t = cargo.getTransportable();
            reason = invalidReason(aiCarrier, cargo.getCarrierTarget());
            if (reason != null || (reason = cargo.check(aiCarrier)) != null) {
                // Just remove, it is invalid
                removeCargo(cargo);
                lb.add(", INVALID(", reason, ") ", cargo.toShortString());
            } else if (cargo.isDelivered()) {
                removeCargo(cargo);
                lb.add(", COMPLETED ", cargo.toShortString());
            } else if (!cargo.hasPath() && !cargo.retry()) {
                reason = " no-path";
                dump = true;
            } else if (carrier.hasTile() && (reason = cargo.update()) != null) {
                if (reason.startsWith("invalid")) {
                    removeCargo(cargo);
                    lb.add(", FAIL(", reason, ") ", cargo.toShortString());
                } else if (cargo.retry()) {
                    lb.add(", retry-", cargo.getTries(), "(", reason, ")");
                } else {
                    dump = true;
                }
            } else if (cargo.isCollectable()) {
                lb.add(", collect ", cargo.toShortString());
            } else if (cargo.isDeliverable()) {
                lb.add(", deliver ", cargo.toShortString());
            } else {
                lb.add(", ok ", cargo.toShortString()); // Good
                cargo.resetTries();
            }
            if (dump) {
                if (cargo.isCarried()) {
                    dumpCargo(cargo, lb); // FIXME: can fail
                } else {
                    removeCargo(cargo);
                    lb.add(", dropped(", reason, ") ", cargo.toShortString());
                }
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

        // Find anything that was not on the cargoes list
        if (!unitsPresent.isEmpty()) {
            lb.add(", found unexpected units");
            for (Unit u : unitsPresent) {
                AIUnit aiu = getAIMain().getAIUnit(u);
                if (aiu == null) throw new IllegalStateException("Bogus:" + u);
                todo.add(aiu);
            }
        }
        if (!goodsPresent.isEmpty()) {
            lb.add(", found unexpected goods");
            for (Goods g : goodsPresent) {
                AIGoods aig = new AIGoods(getAIMain(), carrier, g.getType(),
                                          g.getAmount(), null);
                todo.add(aig);
            }
        }

        // Try to queue the surprise transportables.
        while (!todo.isEmpty()) {
            TransportableAIObject t = todo.remove(0);
            if (!queueTransportable(t, false, lb)) drop.add(t);
        }

        // Drop transportables on the drop list, or queue them to be
        // dropped at the next port.
        if (!drop.isEmpty()) {
            path = carrier.getTrivialPath();
            Location end = (path == null) ? null
                : path.getLastNode().getLocation();
            
            while (!drop.isEmpty()) {
                TransportableAIObject t = drop.remove(0);
                if (t.leaveTransport()) {
                    lb.add(" ", t, " left");
                } else if (end != null) {
                    try {
                        Cargo cargo = Cargo.newCargo(t, carrier, end, false);
                        boolean result = queueCargo(cargo, false, lb);
                        lb.add(" to drop at ", Location.upLoc(end),
                            "=", result);
                    } catch (FreeColException fce) {
                        lb.add(" ", t, " drop-fail(", fce.getMessage(), ")");
                    }
                } else {
                    lb.add(" ", t, " stuck");
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
     * @param lb A <code>LogBuilder</code> to log to.
     * @return TCONTINUE if the <code>Cargo</code> should continue,
     *     TDONE if it has completed,
     *     TFAIL if it has failed,
     *     TNEXT if it has progressed to the next stage,
     *     TRETRY if a blockage has occurred and it should be retried,
     */
    private CargoResult tryCargo(Cargo cargo, LogBuilder lb) {
        final Unit carrier = getUnit();
        final Location here = carrier.getLocation();
        final TransportableAIObject t = cargo.getTransportable();
        final Locatable l = t.getTransportLocatable();
        if (l == null) {
            logger.warning("Null-locatable: " + cargo);
            return CargoResult.TDONE;
        }
        if (!Map.isSameLocation(here, cargo.getCarrierTarget())) {
            lb.add(", ", t, " unready");
            return CargoResult.TCONTINUE;
        }
        Direction d = null;
        Location tloc = here;

        switch (cargo.getMode()) {
        case PICKUP:
            if (!t.canMove()) {
                lb.add(", ", t, " out of moves");
                return CargoResult.TCONTINUE;
            }
            if ((d = cargo.getJoinDirection()) == null) {
                logger.warning("Null pickup direction"
                    + " for " + cargo.toShortString()
                    + " at " + t.getLocation().toString()
                    + " to " + carrier);
                return CargoResult.TFAIL;
            }                    
            tloc = tloc.getTile().getNeighbourOrNull(d.getReverseDirection());
            // Fall through
        case LOAD:
            if (!Map.isSameLocation(tloc, t.getLocation())) {
                lb.add(", ", t, " at ", t.getLocation(), " not ", tloc,
                    " # ", cargo.toShortString());
                return CargoResult.TCONTINUE;
            }
            switch (carrier.getNoAddReason(l)) {
            case NONE:
                if (!t.joinTransport(carrier, d)) {
                    lb.add(", ", t, " NO-JOIN");
                    return CargoResult.TFAIL;
                }
                break;
            case ALREADY_PRESENT:
                break;
            case CAPACITY_EXCEEDED:
                lb.add(", ", t, " NO-ROOM on ", carrier);
                return CargoResult.TFAIL;
            default:
                lb.add(", ", t, " retry-", carrier.getNoAddReason(l));
                return CargoResult.TRETRY;
            }
            
            String reason = cargo.update();
            if (reason != null) {
                lb.add(", ", t, " NO-UPDATE(", reason, ")");
                return CargoResult.TFAIL;
            }
            lb.add(", ", t, " collected");
            return CargoResult.TNEXT;

        case DROPOFF:
            if (!t.canMove()) {
                lb.add(", ", t, " about to leave");
                return CargoResult.TCONTINUE;
            }
            if ((d = cargo.getLeaveDirection()) == null) {
                Unit.MoveType mt = ((AIUnit)t).getUnit()
                    .getSimpleMoveType(t.getLocation().getTile(),
                                       cargo.getTransportTarget().getTile());
                switch (mt) {
                case ATTACK_UNIT: case MOVE_NO_ATTACK_CIVILIAN:
                    return CargoResult.TRETRY;
                default:
                    PathNode path = t.getDeliveryPath(carrier,
                        cargo.getTransportTarget());
                    logger.warning("Null direction"
                        + " for " + cargo.toShortString()
                        + " at " + t.getLocation().toShortString()
                        + "/" + carrier.getLocation().toShortString()
                        + " to " + cargo.getTransportTarget()
                        + " mov=" + mt
                        + " path=" + ((path == null) ? "null"
                            : path.fullPathToString()));
                    return CargoResult.TFAIL;
                }
            }
            // Fall through
        case UNLOAD:
            if (isCarrying(t) && !t.leaveTransport(d)) {
                //lb.add(", ", t, " NO-LEAVE");
                PathNode pn = t.getDeliveryPath(carrier, t.getTransportDestination());
                lb.add(", ", t, " NO-LEAVE(", here, "~", cargo.getLeaveDirection(), "~", t.getTransportDestination(), " ", ((pn == null) ? "no-path" : pn.fullPathToString()));
                return CargoResult.TRETRY;
            }
            lb.add(", ", t, " COMPLETED");
            break;

        case DUMP:
            if (!t.leaveTransport()) {
                lb.add(", ", t, " STUCK");
                return CargoResult.TCONTINUE;
            }
            lb.add(", ", t, " DUMPED at ", t.getLocation());
            break;
        }

        // Check for goods completing a wish
        Colony colony;
        AIColony aiColony;
        if ((colony = (t.getLocation() == null) ? null
                : t.getLocation().getColony()) != null
            && (aiColony = getAIMain().getAIColony(colony)) != null
            && aiColony.completeWish(t, lb)) {
            aiColony.requestRearrange();
        }
        return CargoResult.TDONE;
    }

    /**
     * Perform the transport load/unload operations on arrival at the
     * target for the top cargo.
     *
     * @param lb A <code>LogBuilder</code> to log to.
     */
    private void doTransport(LogBuilder lb) {
        final Unit unit = getUnit();
        if (tSize() > 0) {
            // Arrived at a target.  Deliver what can be delivered.
            // Check other deliveries, we might be in port so this is
            // a good time to decide to fail to deliver something.
            lbAt(lb);
            lb.add(", delivering");
            List<Cargo> cont = new ArrayList<>();
            List<Cargo> next = new ArrayList<>();
            List<Cargo> curr = tClear();
            for (Cargo cargo : curr) {
                CargoResult result = (cargo.getMode().isCollection())
                    ? CargoResult.TCONTINUE
                    : tryCargo(cargo, lb);
                switch (result) {
                case TCONTINUE:
                    cont.add(cargo);
                    break;
                case TRETRY: // will check again below
                    if (cargo.retry()) {
                        cont.add(cargo);
                        break;
                    }
                    // Fall through
                case TFAIL:
                    if (cargo.isCarried()) {
                        cargo.dump();
                        break;
                    }
                    // Fall through
                case TDONE:
                    dropTransportable(cargo.getTransportable());
                    cargo.clear();
                    break;
                case TNEXT: default:
                    throw new IllegalStateException("Can not happen");
                }
            }
            curr.clear();
            // Rebuild the cargo list with the original members,
            // less the transportables that were dropped.
            tSet(cont, true);

            // Now try again, this time collecting as well as
            // delivering.
            lb.add(", collecting");
            cont.clear();
            for (Cargo cargo : tClear()) {
                CargoResult result = (cargo.getMode().isCollection())
                    ? tryCargo(cargo, lb)
                    : CargoResult.TCONTINUE;
                switch (result) {
                case TCONTINUE:
                    cont.add(cargo);
                    break;
                case TNEXT:
                    cont.add(cargo);
                    break;
                case TRETRY:
                    if (cargo.retry()) { // Can not reach the target.
                        next.add(cargo); // Try again next turn.
                        break;
                    }
                    // Fall through
                case TFAIL: case TDONE:
                    dropTransportable(cargo.getTransportable());
                    cargo.clear();
                    break;
                default:
                    throw new IllegalStateException("Can not happen");
                }
            }

            // Rebuild the cargo list with the original members,
            // less the transportables that were dropped.
            tSet(cont, true);

            // Add the new and blocked cargoes incrementally with
            // the current arrangement, which is likely to put them
            // at the end.
            if (!next.isEmpty()) {
                lb.add(", requeue");
                for (Cargo c : next) queueCargo(c, false, lb);
            }

            // Delivering might have invalidated other cargo missions.
            // It may be rare, but there have been cases where a scout
            // has disembarked onto an LCR, invalidating the mission
            // of a scout further down the transport list.  Run check
            // cargoes again to handle this.  Then optimize.
            checkCargoes(lb);
            optimizeCargoes(lb);
        }

        // Replenish cargoes up to available destination capacity
        // and 50% above maximum cargoes (FIXME: longer?)
        final EuropeanAIPlayer euaip = getEuropeanAIPlayer();
        while (destinationCapacity() > 0
            && tSize() < unit.getCargoCapacity() * 3 / 2) {
            Cargo cargo = getBestCargo(unit);
            if (cargo == null) break;
            if (!queueCargo(cargo, false, lb)) break;
            euaip.claimTransportable(cargo.getTransportable());
        }
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
            int turns = carrier.getTurnsToReach(now, cargo.getCarrierTarget());
            totalTurns += turns; // Might be MANY_TURNS!
            totalHoldTurns += holds * turns * favourEarly;
            holds += cargo.getNewSpace();
            if (holds < 0 || holds > maxHolds) return -1.0f;
            now = cargo.getCarrierTarget();
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
        Location oldTarget = getTarget();

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
            for (List<Cargo> tl : getPermutations(ts)) {
                float value = scoreCargoOrder(current, tl);
                if (value > 0.0f && bestValue > value) {
                    bestValue = value;
                    best = tl;
                }
            }
        }
        if (best != null) {
            tSet(unwrapCargoes(best), true);
            if (oldTarget != getTarget()) lb.add("->", getTarget());
        } else {
            tSet(unwrapCargoes(ts), false);
        }
    }

    /**
     * What is the best transportable for a carrier to collect?
     *
     * @param carrier The carrier <code>Unit</code> to consider.
     * @return The best available new <code>Cargo</code>, or null if
     *     none found.
     */
    private Cargo getBestCargo(Unit carrier) {
        final EuropeanAIPlayer euaip = getEuropeanAIPlayer();
        Cargo bestDirect = null, bestFallback = null;
        float bestDirectValue = 0.0f, bestFallbackValue = 0.0f;
        for (TransportableAIObject t : euaip.getUrgentTransportables()) {
            if (t.isDisposed() || !t.carriableBy(carrier)) continue;
            Location loc = t.getTransportSource();
            Cargo cargo;
            try {
                cargo = Cargo.newCargo(t, carrier);
            } catch (FreeColException fce) {
                cargo = null;
            }
            if (cargo == null) continue;
            float value = t.getTransportPriority() / (cargo.getTurns() + 1.0f);
            if (cargo.isFallback()) {
                if (bestFallbackValue < value) {
                    bestFallbackValue = value;
                    bestFallback = cargo;
                }
            } else {
                if (bestDirectValue < value) {
                    bestDirectValue = value;
                    bestDirect = cargo;
                }
            }
        }
        return (bestDirect != null) ? bestDirect
            : (bestFallback != null) ? bestFallback
            : null;
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
     * @param cargo The <code>Cargo</code> to test.
     * @return A reason why the mission would be invalid,
     *     or null if none found.
     */
    private static String invalidCargoReason(Cargo cargo) {
        final TransportableAIObject t = cargo.getTransportable();
        String reason;
        return (t == null) ? "null-transportable"
            : ((reason = t.invalidReason()) != null) ? "cargo-" + reason
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
            : (loc instanceof Tile)
            ? null
            : (loc instanceof Europe || loc instanceof Colony)
            ? invalidTargetReason(loc, aiUnit.getUnit().getOwner())
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

    // End of cargoes handling.

    // Publically accessible routines to manipulate a TransportableAIObject.

    /**
     * Removes the given <code>TransportableAIObject</code> from the
     * cargo list.
     *
     * @param t The <code>TransportableAIObject</code> to remove.
     * @return True if the removal succeeded.
     */
    public boolean removeTransportable(TransportableAIObject t) {
        Cargo cargo = tFind(t);
        return (cargo == null) ? false : tRemove(cargo);
    }

    /**
     * Retargets a transportable that should already be on board the carrier.
     *
     * @param t The <code>TransportableAIObject</code> to retarget.
     * @param lb A <code>LogBuilder</code> to log to.
     * @return True if the retargeting succeeded.
     */
    public boolean requeueTransportable(TransportableAIObject t,
                                        LogBuilder lb) {
        Cargo cargo = tFind(t);
        return (cargo == null) ? queueTransportable(t, false, lb)
            : requeueCargo(cargo, lb);
    }

    /**
     * Wrapper for queueCargo.
     * Public for the benefit of EuropeanAIPlayer.allocateTransportables
     * and CashInTreasureTrain.doMission.
     *
     * @param t The <code>TransportableAIObject</code> to add.
     * @param requireMatch Fail if an existing destination is not matched.
     * @param lb A <code>LogBuilder</code> to log to.
     * @return True if the transportable was queued.
     */
    public boolean queueTransportable(TransportableAIObject t,
                                      boolean requireMatch, LogBuilder lb) {
        Cargo cargo = makeCargo(t, lb);
        return (cargo == null) ? false : queueCargo(cargo, requireMatch, lb);
    }

    /**
     * Dump a transportable.
     *
     * @param t The <code>TransportableAIObject</code> to dump.
     * @param lb A <code>LogBuilder</code> to log to.
     * @return True if the transportable is no longer on board, queued, or
     *     was reset to be dumped at the next stop.
     */
    public boolean dumpTransportable(TransportableAIObject t, LogBuilder lb) {
        if (t == null) return true;
        Cargo cargo = tFind(t);
        if (cargo == null) return true;
        if (!isCarrying(t)) {
            removeTransportable(t);
            return true;
        }
        return dumpCargo(cargo, lb);
    }

    /**
     * Drop all collections so that cargo is delivered only, then
     * collect this unit.  Useful for prioritizing treasure
     * collection.
     *
     * @param aiu The <code>AIUnit</code> to collect.
     * @param lb A <code>LogBuilder</code> to log to.
     * @return True if the unit was queued.
     */
    public boolean forceCollection(AIUnit aiu, LogBuilder lb) {
        for (Cargo c : tCopy()) {
            if (c.getMode().isCollection()) removeCargo(c);
        }            
        return queueTransportable(aiu, false, lb);
    }
    
    /**
     * Suppress European trade in a type of goods which is about to be
     * boycotted.
     *
     * @param type The <code>GoodsType</code> to suppress.
     * @param lb A <code>LogBuilder</code> to log to.
     */
    public void suppressEuropeanTrade(GoodsType type, LogBuilder lb) {
        for (Cargo c : tCopy()) {
            if (c.isEuropeanTrade(type)) removeCargo(c);
        }
    }


    // End of public TransportableAIObject manipulations

    // Implement Mission
    //   Inherit dispose, getBaseTransportPriority, isOneTime

    /**
     * {@inheritDoc}
     */
    @Override
    public Tile getTransportDestination() {
        return null; // Can not transport a carrier unit.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location getTarget() {
        return target;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTarget(Location target) {
        this.target = target;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location findTarget() {
        // A noop.  The target is defined by the cargoes.
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String invalidReason() {
        final AIUnit aiUnit = getAIUnit();
        String reason = invalidReason(aiUnit, getTarget());
        Cargo cargo;
        return (reason != null) ? reason
            : ((cargo = tFirst()) == null) ? null
            : invalidCargoReason(cargo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mission doMission(LogBuilder lb) {
        lb.add(tag);
        checkCargoes(lb);
        String reason = invalidReason();
        if (reason != null) return lbFail(lb, false, reason);

        final AIUnit aiCarrier = getAIUnit();
        final Unit unit = getUnit();
        final CostDecider fallBackDecider
            = CostDeciders.avoidSettlementsAndBlockingUnits();
        final EuropeanAIPlayer euaip = getEuropeanAIPlayer();
        CostDecider costDecider = CostDeciders.defaultCostDeciderFor(unit);
        for (;;) {
            Unit.MoveType mt = travelToTarget(target, costDecider, lb);
            switch (mt) {
            case MOVE: // Arrived at transport target
                doTransport(lb);
                if (isEmpty() && unit.isOffensiveUnit()) {
                    Mission m = euaip.getPrivateerMission(aiCarrier, null);
                    if (m != null) return lbDone(lb, false, "going pirate");
                }                    
                if ((reason = invalidReason()) != null) {
                    logger.warning(tag + " post-stop failure(" + reason
                        + "): " + this.toFullString());
                    return lbFail(lb, false, reason);
                }
                if (unit.isAtLocation(target)) {
                    return lbWait(lb, ", waiting at ", target);
                }
                break;

            case MOVE_HIGH_SEAS: case MOVE_NO_MOVES:
            case MOVE_NO_REPAIR: case MOVE_ILLEGAL:
                return lbWait(lb);

            case MOVE_NO_TILE: // Another unit is blocking a river?
                moveRandomly(tag, null);
                return lbDodge(lb);

            case ATTACK_UNIT:
                Location blocker = resolveBlockage(aiCarrier, target);
                if (blocker instanceof Unit && shouldAttack((Unit)blocker)) {
                    AIMessage.askAttack(aiCarrier,
                        unit.getTile().getDirection(blocker.getTile()));
                    return lbAttack(lb, blocker);
                }
                // Fall through
            case MOVE_NO_ATTACK_CIVILIAN:
                // FIXME: See if the transportable can get around the
                // blockage using its own path finding.
                if (unit.getTile().isAdjacent(target.getTile())
                    || costDecider == fallBackDecider) {
                    moveRandomly(tag, null);
                    return lbDodge(lb);
                }
                costDecider = fallBackDecider; // Retry
                lb.add(", retry blockage at ", unit.getLocation());
                break;

            case MOVE_NO_ACCESS_EMBARK: default:
                return lbMove(lb, mt);
            }
        }
    }


    // Serialization

    private static final String TARGET_TAG = "target";
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
            if (reason != null) continue;
            // Do not bother writing cargoes that will be dumped.
            // On restore, checkCargoes will work out what to do with them.
            if (cargo.getMode() == Cargo.CargoMode.DUMP) continue;
            cargo.toXML(xw);
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
        final String tag = xr.getLocalName();

        if (Cargo.getXMLElementTagName().equals(tag)) {
            tAdd(new Cargo(getAIMain(), xr), -1);

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
    @Override
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
