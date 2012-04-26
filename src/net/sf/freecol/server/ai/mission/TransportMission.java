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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Locatable;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.server.ai.AIColony;
import net.sf.freecol.server.ai.AIGoods;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIObject;
import net.sf.freecol.server.ai.AIPlayer;
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

    private static final String ELEMENT_TRANSPORTABLE = "transportable";

    private static final int MINIMUM_GOLD_TO_STAY_IN_EUROPE = 600;

    private final List<Transportable> transportList
        = new ArrayList<Transportable>();

    class Destination {
        private boolean atDestination;
        private boolean moveToEurope;
        private PathNode path;

        /*
         * Returns an "Already at destination"
         */
        public Destination() {
            this.atDestination = true;
            this.moveToEurope = false;
            this.path = null;
        }

        public Destination(boolean moveToEurope, PathNode path) {
            this.atDestination = false;
            this.moveToEurope = moveToEurope;
            this.path = path;
        }

        public boolean moveToEurope() {
            return moveToEurope;
        }
        
        public PathNode getPath() {
            return path;
        }

        public boolean isAtDestination() {
            return atDestination;
        }
    }


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     */
    public TransportMission(AIMain aiMain, AIUnit aiUnit) {
        super(aiMain, aiUnit);

        if (!getUnit().isCarrier()) {
            throw new IllegalArgumentException("Carrier required: " + aiUnit);
        }
        uninitialized = false;
    }

    /**
     * Creates a new <code>TransportMission</code> and reads the given
     * element.
     *
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public TransportMission(AIMain aiMain, XMLStreamReader in)
        throws XMLStreamException {
        super(aiMain);

        readFromXML(in);
        uninitialized = getAIUnit() == null;
    }


    /**
     * Disposes this <code>Mission</code>.
     */
    public void dispose() {
        // a new list must be created as the first one may be changed
        //elsewhere in between loop calls
        List<Transportable> cargoList = new ArrayList<Transportable>();
        List<Transportable> scheduledCargoList = new ArrayList<Transportable>();

        Iterator<Transportable> ti = transportList.iterator();
        while (ti.hasNext()) {
            Transportable t = ti.next();
            // the cargo is on board, add to list to be disposed of
            if (isCarrying(t)) {
                cargoList.add(t);
            } else {
                // the cargo was scheduled to be transported
                // cancel order
                scheduledCargoList.add(t);
            }
        }

        for (Transportable t : cargoList) ((AIObject) t).dispose();
        for (Transportable t : scheduledCargoList) t.setTransport(null);
        super.dispose();
    }

    /**
     * Adds every <code>Goods</code> and <code>Unit</code> onboard the
     * carrier to the transport list.
     *
     * @see Goods
     * @see Unit
     */
    private void updateTransportList() {
        Unit carrier = getUnit();
        Player owner = carrier.getOwner();

        // Try to add units that happen to be on board.
        for (Unit u : carrier.getUnitList()) {
            AIUnit aiUnit = getAIMain().getAIUnit(u);
            if (aiUnit == null) {
                logger.warning("Could not find AI unit for: " + u);
                continue;
            }
            if (aiUnit.getTransportDestination() != null) {
                addToTransportList(aiUnit);
            } else if (carrier.isInEurope()
                || (carrier.getTile() != null && carrier.getTile().isLand())) {
                // Unit has no destination, drop it off if on land.
                unitLeavesShip(aiUnit);
            }
        }

        // Remove items that should no longer be transported:
        // - next step is a null location
        // - next step is disposed
        // - next step is a captured settlement
        List<Transportable> ts = new ArrayList<Transportable>();
        for (Transportable t : new ArrayList<Transportable>(transportList)) {
            if (ts.contains(t) || isCarrying(t)) {
                Location dst = t.getTransportDestination();
                if (dst == null
                    || ((FreeColGameObject)dst).isDisposed()
                    || ((dst instanceof Settlement)
                        && ((Settlement)dst).getOwner() != null
                        && !owner.owns((Settlement)dst))) {
                    removeFromTransportList(t);
                }
            } else {
                Location src = t.getTransportSource();
                if (src == null
                    || ((FreeColGameObject)src).isDisposed()
                    || ((src instanceof Settlement)
                        && ((Settlement)src).getOwner() != null
                        && !owner.owns((Settlement)src))) { 
                    removeFromTransportList(t);
                }
            }
            ts.add(t);
        }
    }

    /**
     * Checks if the carrier using this mission is carrying the given
     * <code>Transportable</code>.
     *
     * @param t The <code>Transportable</code>.
     * @return <code>true</code> if the given <code>Transportable</code> is
     *         {@link Unit#getLocation located} in the carrier.
     */
    private boolean isCarrying(Transportable t) {
        // TODO: Proper code for checking if the goods is onboard the carrier.
        return t != null
            && t.getTransportLocatable() != null
            && t.getTransportLocatable().getLocation() == getUnit();
    }

    /**
     * Checks if the given <code>Transportable</code> is on the transport
     * list.
     *
     * @param newTransportable The <code>Transportable</code> to be checked
     * @return <code>true</code> if the given <code>Transportable</code> was
     *         on the transport list, and <code>false</code> otherwise.
     */
    public boolean isOnTransportList(Transportable newTransportable) {
        for (int i = 0; i < transportList.size(); i++) {
            if (transportList.get(i) == newTransportable) {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes the given <code>Transportable</code> from the transport list.
     * This method calls {@link Transportable#setTransport(AIUnit)}.
     *
     * @param transportable The <code>Transportable</code>.
     */
    public void removeFromTransportList(Transportable transportable) {
        transportList.remove(transportable);
        if (transportable.getTransport() == getAIUnit()) {
            transportable.setTransport(null);
        }
    }

    /**
     * Adds the given <code>Transportable</code> to the transport list. The
     * method returns immediately if the {@link Transportable} has already be
     * added.
     *
     * <br>
     * <br>
     *
     * Both the source and destination {@link Location} for the
     * <code>Transportable</code> is entered into the transport list if the
     * <code>Transportable</code> is not already loaded onto the transport. If
     * the <code>Transportable</code> is onboard the transport, then only the
     * destination is put on the transport list.
     *
     * @param newTransportable The <code>Transportable</code>.
     */
    public void addToTransportList(Transportable newTransportable) {
        Unit carrier = getUnit();
        if (newTransportable.getTransportLocatable() instanceof Unit
            && ((Unit) newTransportable.getTransportLocatable()).isCarrier()) {
            throw new IllegalArgumentException("Can not add a carrier to a transport list.");
        }
        Location newSource = newTransportable.getTransportSource();
        Location newDestination = newTransportable.getTransportDestination();

        if (newDestination == null) {
            logger.warning("No destination for: "
                + newTransportable.toString());
            return;
        }

        if (newSource == null && !isCarrying(newTransportable)) {
            logger.warning("No source for: " + newTransportable.toString());
            return;
        }

        if (isOnTransportList(newTransportable)) return;

        int bestSourceIndex = -1;
        if (!isCarrying(newTransportable)) {
            int distToSource;
            if (carrier.getLocation().getTile() == newSource.getTile()) {
                distToSource = 0;
            } else {
                Tile t = (carrier.getTile() != null) ? carrier.getTile()
                    : carrier.getFullEntryLocation();
                distToSource = getDistanceTo(newTransportable, t, true);
                // Sanitation
                // Carrier cant reach source
                if(distToSource == Map.COST_INFINITY){
                    return;
                }
            }
            bestSourceIndex = 0;
            int bestSourceDistance = distToSource;
            for (int i = 1; i < transportList.size() && bestSourceDistance > 0; i++) {
                Transportable t1 = transportList.get(i - 1);
                if (t1.getTransportSource() != null && t1.getTransportSource().getTile() == newSource.getTile()
                    || t1.getTransportDestination() != null
                    && t1.getTransportDestination().getTile() == newSource.getTile()) {
                    bestSourceIndex = i;
                    bestSourceDistance = 0;
                }

            }

            for (int i = 1; i < transportList.size() && bestSourceDistance > 0; i++) {
                Transportable t1 = transportList.get(i - 1);

                if (isCarrying(t1)){
                    int distToDestination = getDistanceTo(newTransportable, t1.getTransportDestination(), true);
                    if(distToDestination == Map.COST_INFINITY){
                        continue;
                    }
                    if(distToDestination <= bestSourceDistance) {
                        bestSourceIndex = i;
                        bestSourceDistance = distToDestination;
                    }
                } else{
                    distToSource = getDistanceTo(newTransportable, t1.getTransportSource(), true);
                    if(distToSource == Map.COST_INFINITY){
                        continue;
                    }
                    if (distToSource <= bestSourceDistance) {
                        bestSourceIndex = i;
                        bestSourceDistance = distToSource;
                    }
                }
            }
            transportList.add(bestSourceIndex, newTransportable);
        }

        int bestDestinationIndex = bestSourceIndex + 1;
        int bestDestinationDistance = Integer.MAX_VALUE;
        if (bestSourceIndex == -1) {
            bestDestinationIndex = 0;
            if (carrier.getTile() == newSource.getTile()) {
                bestDestinationDistance = 0;
            } else {
                int distToCarrier = getDistanceTo(newTransportable, carrier.getTile(), false);
                if(distToCarrier != Map.COST_INFINITY){
                    bestDestinationDistance = distToCarrier;
                }
            }
        }
        for (int i = Math.max(bestSourceIndex, 1); i < transportList.size() && bestDestinationDistance > 0; i++) {
            Transportable t1 = transportList.get(i - 1);
            if ((t1.getTransportSource() != null
                    && t1.getTransportSource().getTile()
                    == newDestination.getTile())
                || (t1.getTransportDestination() != null
                    && t1.getTransportDestination().getTile()
                    == newDestination.getTile())) {
                bestDestinationIndex = i;
                bestDestinationDistance = 0;
            }
        }
        for (int i = Math.max(bestSourceIndex, 1); i < transportList.size() && bestDestinationDistance > 0; i++) {
            Transportable t1 = transportList.get(i - 1);
            if (isCarrying(t1)){
                int distToDestination = getDistanceTo(newTransportable, t1.getTransportDestination(), false);
                if(distToDestination == Map.COST_INFINITY){
                    continue;
                }
                if(distToDestination <= bestDestinationDistance) {
                    bestDestinationIndex = i;
                    bestDestinationDistance = distToDestination;
                }
            } else{
                int distToSource = getDistanceTo(newTransportable, t1.getTransportSource(), false);
                if(distToSource == Map.COST_INFINITY){
                    continue;
                }
                if(distToSource <= bestDestinationDistance) {
                    bestDestinationIndex = i;
                    bestDestinationDistance =  distToSource;
                }
            }
        }
        transportList.add(bestDestinationIndex, newTransportable);

        if (newTransportable.getTransport() != getAIUnit()) {
            newTransportable.setTransport(getAIUnit());
        }
    }

    /**
     * Gets the distance to the given <code>Transportable</code>.
     *
     * @param start The <code>Location</code> to check the distance from.
     *            <code>Europe</code> is used instead of this location if
     *            <code>start.getTile() == null</code>.
     * @param source Sets wether the <code>Transportable</code>'s
     *            {@link Transportable#getTransportSource source} or
     *            {@link Transportable#getTransportDestination destination}
     *            should be used.
     * @return The distance from the given <code>Location</code> to the source
     *         or destination of the given <code>Transportable</code>.
     */
    private int getDistanceTo(Transportable t, Location start, boolean source) {
        // TODO: This is too expensive - find another method:
        PathNode path = getTransportPath(t, start, source);
        return (path == null) ? Map.COST_INFINITY : path.getTotalTurns();
    }

    private boolean canAttackEnemyShips() {
        final Unit carrier = getUnit();
        return carrier.getTile() != null
            && carrier.isNaval()
            && carrier.isOffensiveUnit();
    }

    private boolean hasCargo() {
        final Unit carrier = getUnit();
        return (carrier.getGoodsCount() + carrier.getUnitCount()) > 0;
    }

    /**
     * Attack blocking ships.
     *
     * @return True if this ship is still capable of its mission.
     */
    private boolean attackIfEnemyShipIsBlocking(Direction direction) {
        final Unit carrier = getUnit();
        if (canAttackEnemyShips()
            && carrier.getMoveType(direction) == MoveType.ATTACK_UNIT) {
            final Tile newTile = carrier.getTile().getNeighbourOrNull(direction);
            final Unit defender = newTile.getDefendingUnit(carrier);
            if (canAttackPlayer(defender.getOwner())) {
                AIMessage.askAttack(getAIUnit(), direction);
            }
        }
        return isValid();
    }

    /**
     * Attack suitable enemy ships.
     *
     * @return True if this ship is still capable of its mission.
     */
    private boolean attackEnemyShips() {
        if (!canAttackEnemyShips()) {
            return true;
        }
        final Unit carrier = getUnit();
        if (hasCargo()) {
            // Do not search for a target if we have cargo onboard.
            return true;
        }
        final PathNode pathToTarget = findNavalTarget(0);
        if (pathToTarget != null) {
            final Direction direction = moveTowards(pathToTarget);
            if (direction != null
                && carrier.getMoveType(direction) == MoveType.ATTACK_UNIT) {
                AIMessage.askAttack(getAIUnit(), direction);
            }
        }
        return isValid();
    }

    private boolean canAttackPlayer(Player target) {
        return getUnit().getOwner().atWarWith(target)
            || getUnit().hasAbility(Ability.PIRACY);
    }

    /**
     * Finds the best target to attack within the given range.
     *
     * @param maxTurns The maximum number of turns the unit is allowed
     *      to spend in order to reach the target.
     * @return The path to the target or <code>null</code> if no target
     *      can be found.
     */
    protected PathNode findNavalTarget(final int maxTurns) {
        if (!getUnit().isOffensiveUnit()) {
            throw new IllegalStateException("A target can only be found for offensive units. You tried with: "
                                            + getUnit().toString());
        }
        if (!getUnit().isNaval()) {
            throw new IllegalStateException("A target can only be found for naval units. You tried with: "
                                            + getUnit().toString());
        }

        final GoalDecider gd = new GoalDecider() {
                private PathNode bestTarget = null;
                private int bestValue = 0;

                public PathNode getGoal() {
                    return bestTarget;
                }

                public boolean hasSubGoals() {
                    return true;
                }

                public boolean check(final Unit unit, final PathNode pathNode) {
                    final Tile newTile = pathNode.getTile();
                    final Unit defender = newTile.getDefendingUnit(unit);
                    if (newTile.isLand()
                        || defender == null
                        || defender.getOwner() == unit.getOwner()) {
                        return false;
                    }
                    if (!canAttackPlayer(defender.getOwner())) {
                        return false;
                    }
                    final int value = 1 + defender.getUnitCount() + defender.getGoodsCount();
                    if (value > bestValue) {
                        bestTarget = pathNode;
                        bestValue = value;
                    }
                    return true;
                }
            };
        return getUnit().search(getUnit().getTile(), gd,
                                CostDeciders.avoidSettlements(),
                                maxTurns, null);
    }

    /**
     * Performs the mission.
     */
    public void doMission() {
        logger.finest("Doing transport mission for unit " + getUnit()
            + "(" + getUnit().getId() + ")");
        if (transportList == null || transportList.size() <= 0) {
            updateTransportList();
        }

        Unit carrier = getUnit();
        if (carrier.getMovesLeft() == 0) return;
        if (carrier.isAtSea()) return; // Going to/from Europe, do nothing
        if (carrier.isInEurope()) { // Actually in Europe
            inEurope();
            return;
        }

        if (!attackEnemyShips()) return;
        restockCargoAtDestination();
        if (!attackEnemyShips()) return;

        boolean transportListChanged = false;
        boolean moreWork = true;
        for (int i = 0; i < transportList.size() && moreWork || i == 0; i++) {
            if (carrier.getMovesLeft() == 0) return;

            moreWork = false;
            if (transportListChanged) {
                i = 0;
                transportListChanged = false;
            }

            // Special case, already on a tile which gives direct
            // access to Europe path will be null.
            Destination destination = getNextDestination();
            boolean canMoveToEurope = destination != null
                && destination.moveToEurope()
                && carrier.canMoveToEurope();

            if (destination == null
                || (destination.getPath() == null && !canMoveToEurope)) {
                String carrierLoc = "";
                if (carrier.getLocation() instanceof Europe) {
                    carrierLoc = "Europe";
                } else {
                    Tile carrierTile = carrier.getTile();
                    carrierLoc = carrierTile.toString();
                    if (carrierTile.getColony() != null) {
                        carrierLoc += " (" + carrierTile.getColony().getName()
                            + ")";
                    }
                }

                logger.info("Could not get a next move for unit " + carrier
                    + "(" + carrier.getId()
                    + "), staying put at " + carrierLoc);
                //carrier.setMovesLeft(0);
                return;
            }

            if (destination.isAtDestination()) {
                transportListChanged = restockCargoAtDestination();
                continue;
            }

            // Already on a tile which gives direct access to Europe,
            // just make the move
            if (canMoveToEurope) {
                moveUnitToEurope();
                return;
            }

            // Move towards the next target:
            PathNode path = destination.getPath();
            boolean moveToEurope = destination.moveToEurope();
            Direction r = moveTowards(path);
            if (r != null && carrier.getMoveType(r).isProgress()) {
                if (carrier.getMoveType(r) == MoveType.MOVE_HIGH_SEAS
                    && moveToEurope) {
                    moveUnitToEurope();
                } else {
                    if (!moveButDontAttack(r)) return;
                }

                if (!(carrier.getLocation() instanceof Europe)) {
                    moreWork = true;
                }
            }
            if (r != null) {
                if (!attackIfEnemyShipIsBlocking(r)) return;
            }
            transportListChanged = restockCargoAtDestination();
            if (!attackEnemyShips()) return;
        }
    }

    /**
     * Works out the next destination the carrier should go to to
     * make progress with its transport list.
     *
     * @return A new <code>Destination</code>, which may be null if none
     *     is available.
     */
    public Destination getNextDestination() {
        final Unit carrier = getUnit();
        if (transportList.isEmpty() && !hasCargo()) {
            logger.finest("Next destination for " + carrier + ": default");
            return getDefaultDestination();
        }

        // Cache unavailable destinations to avoid unnecessary path finding.
        List<Location> unavailable = new ArrayList<Location>();
        for (Transportable t : transportList) {
            Location dst = (isCarrying(t))
                ? t.getTransportDestination()
                : t.getTransportLocatable().getLocation();

            // Check if we already found this destination to be inaccessible.
            if (dst == null || unavailable.contains(dst)) continue;
            
            PathNode path;
            if (dst.getTile() == null) {
                if (dst instanceof Europe
                    && (path = carrier.findPathToEurope()) != null) {
                    logger.finest("Next destination for " + carrier
                        + ": " + dst
                        + " (" + ((isCarrying(t)) ? "transport" : "collect") 
                        + " " + t + ")");
                    return new Destination(true, path);
                }
            } else {
                if (dst.getTile() == carrier.getTile()) {
                    logger.finest("Next destination for " + carrier
                        + ": already at " + dst
                        + " (for " + t + ")");
                    return new Destination(); // Already at dst!
                }
                if ((path = getTransportPath(t)) != null) {
                    logger.finest("Next destination for " + carrier
                        + ": " + dst
                        + " (" + ((isCarrying(t)) ? "transport" : "collect") 
                        + " " + t + ")");
                    return new Destination(false, path);
                }
            }
            unavailable.add(dst);
        }
        logger.finest("Next destination for " + carrier + ": none found");
        return null;
    }

    /**
     * Gets the current default destination for the unit of this mission.
     *
     * @return The default <code>Destination</code> for the unit.
     */
    Destination getDefaultDestination() {
        Unit unit = getUnit();
        PathNode path = null;

        // If in Europe, stay in Europe
        if (unit.getLocation() instanceof Europe) {
            return new Destination();
        }

        // Otherwise should be on the map
        if (unit.getTile() == null) {
            throw new IllegalStateException("Unit not on the map: "
                                            + unit.getId());
        }

        // Already at a settlement
        if (unit.getSettlement() != null) {
            return new Destination();
        }

        // Try nearest colony
        if ((path = unit.findOurNearestOtherSettlement()) != null) {
            return new Destination(false, path);
        }

        // Try Europe
        if (unit.isNaval() && unit.getOwner().canMoveToEurope()) {
            if (unit.canMoveToEurope()) {
                return new Destination(true, null);
            }
            if ((path = unit.findPathToEurope()) != null) {
                return new Destination(true, path);
            }
        }

        // Can fail intermittantly.  For example: up river and blocked in.
        logger.warning("Could not get default destination for " + unit);
        return null;
    }

    /**
     * Buys cargo (units and goods) when the carrier is in <code>Europe</code>.
     *
     * <br>
     * <br>
     *
     * <b>Warning:</b> This method can only be called when the carrier is
     * located in {@link Europe}.
     */
    private void buyCargo() {
        if (!getUnit().isInEurope()) {
            throw new IllegalStateException("Carrier not in Europe");
        }

        /*
         * Quick fix for forcing the AI to build more colonies. This fix should
         * be removed after a proper implementation has been created.
         */
        final EuropeanAIPlayer aiPlayer = getEuropeanAIPlayer();
        final AIUnit aiUnit = getAIUnit();
        if (aiPlayer.hasFewColonies()) {
            // since we are in Europe, use the carrier entry point to
            // search for a good settlement spot.
            Unit carrier = getUnit();
            AIUnit newUnit;            
            for (int space = getAvailableSpace(); space > 0;
                 space -= newUnit.getUnit().getSpaceTaken()) {
                newUnit = getCheapestUnitInEurope();
                if (newUnit == null
                    || !BuildColonyMission.isValid(newUnit)) break;
                addToTransportList(newUnit);
                Location buildTarget = BuildColonyMission.findTarget(aiUnit, false);
                if (buildTarget == null) break;
                // send the colonist to build the new colony
                newUnit.setMission(new BuildColonyMission(getAIMain(),
                                   newUnit, buildTarget));
            }
        }

        /*
         * Add colonies containing wishes with the same destination as an item
         * in the transport list to the "aiColonies"-list:
         */
        List<AIColony> aiColonies = new ArrayList<AIColony>();
        for (int i = 0; i < transportList.size(); i++) {
            Transportable t = transportList.get(i);
            if (t.getTransportDestination() != null
                && t.getTransportDestination().getTile() != null
                && t.getTransportDestination().getTile().getColony() != null
                && t.getTransportDestination().getTile().getColony().getOwner() == getUnit().getOwner()) {
                AIColony ac = getAIMain().getAIColony(t.getTransportDestination().getTile().getColony());
                aiColonies.add(ac);
            }
        }

        /*
         * Add the colony containing the wish with the highest value to the
         * "aiColonies"-list:
         */
        EuropeanAIPlayer player = (EuropeanAIPlayer) getAIMain().getAIPlayer(getUnit().getOwner());
        for (Wish w : player.getWishes()) {
            if (w.getTransportable() != null) continue;
            if (w instanceof WorkerWish && w.getDestination() instanceof Colony) {
                WorkerWish ww = (WorkerWish) w;
                Colony c = (Colony) ww.getDestination();
                AIColony ac = getAIMain().getAIColony(c);
                if (!aiColonies.contains(ac)) {
                    aiColonies.add(ac);
                }
            } else if (w instanceof GoodsWish && w.getDestination() instanceof Colony) {
                GoodsWish gw = (GoodsWish) w;
                Colony c = (Colony) gw.getDestination();
                AIColony ac = getAIMain().getAIColony(c);
                if (!aiColonies.contains(ac)) {
                    aiColonies.add(ac);
                }
            } else {
                logger.warning("Unknown type of wish: " + w);
            }
        }
        for (int i = 0; i < aiColonies.size(); i++) {
            AIColony ac = aiColonies.get(i);
            // Assuming that all colonists which can be bought in Europe take
            // the same space: TODO: fix this
            int space = getAvailableSpace(getUnit().getType(), getUnit().getOwner().getEurope(), ac.getColony());
            for (Wish w : ac.getWishes()) {
                if (space <= 0) break;
                if (w.getTransportable() != null) continue;
                if (w instanceof WorkerWish) {
                    WorkerWish ww = (WorkerWish) w;
                    AIUnit newUnit = getUnitInEurope(ww.getUnitType());
                    if (newUnit != null) {
                        newUnit.setMission(new WishRealizationMission(getAIMain(), newUnit, ww));
                        ww.setTransportable(newUnit);
                        addToTransportList(newUnit);
                        space--;
                    }
                } else if (w instanceof GoodsWish) {
                    GoodsWish gw = (GoodsWish) w;
                    AIGoods ag = buyGoodsInEurope(gw.getGoodsType(),
                                                  GoodsContainer.CARGO_SIZE, gw.getDestination());
                    if (ag != null) {
                        gw.setTransportable(ag);
                        addToTransportList(ag);
                        space--;
                    }
                } else {
                    logger.warning("Unknown type of wish: " + w);
                }
            }
        }

        // Fill the transport with cheap colonists:
        int space = getAvailableSpace();
        while (space > 0) {
            AIUnit newUnit = getCheapestUnitInEurope();
            if (newUnit != null) {
                addToTransportList(newUnit);
                space--;
            } else {
                break;
            }
        }
    }

    /**
     * Buys the given cargo.
     *
     * <br>
     * <br>
     *
     * <b>Warning:</b> This method can only be called when the carrier is
     * located in {@link Europe}.
     *
     * @param type The type of goods to buy.
     * @param amount The amount of goods to buy.
     * @param destination The <code>Location</code> to which the goods should
     *            be transported.
     * @return The goods.
     */
    public AIGoods buyGoodsInEurope(GoodsType type, int amount, Location destination) {
        AIPlayer aiPlayer = getAIMain().getAIPlayer(getUnit().getOwner());
        Player player = aiPlayer.getPlayer();
        Market market = player.getMarket();

        if (!player.checkGold(market.getBidPrice(type, amount))) return null;

        boolean success = AIMessage.askBuyGoods(getAIUnit(), type, amount);
        return (success)
            ? new AIGoods(getAIMain(), getUnit(), type, amount, destination)
            : null;
    }

    /**
     * Returns the given type of <code>Unit</code>.
     *
     * <br>
     * <br>
     *
     * <b>Warning:</b> This method can only be called when the carrier is
     * located in {@link Europe}.
     *
     * <br>
     * <br>
     *
     * This sequence is used when trying to get the unit: <br>
     * <br>
     * <ol>
     * <li>Getting the unit from the docks.
     * <li>Recruiting the unit.
     * <li>Training the unit.
     * </ol>
     *
     * @param unitType The type of {@link Unit} to be found/recruited/trained.
     * @return The <code>AIUnit</code>.
     */
    private AIUnit getUnitInEurope(UnitType unitType) {
        EuropeanAIPlayer aiPlayer = (EuropeanAIPlayer) getAIMain().getAIPlayer(getUnit().getOwner());
        Player player = aiPlayer.getPlayer();
        Europe europe = player.getEurope();

        if (!getUnit().isInEurope()) {
            throw new IllegalStateException("Carrier not in Europe");
        }

        // Check if the given type of unit appear on the docks:
        Iterator<Unit> ui = europe.getUnitIterator();
        while (ui.hasNext()) {
            Unit u = ui.next();
            if (unitType == null || unitType == u.getType()) {
                return getAIMain().getAIUnit(u);
            }
        }

        int price = -1;
        if (unitType.hasPrice() && europe.getUnitPrice(unitType) >= 0) {
            price = europe.getUnitPrice(unitType);
        }

        // Try recruiting the unit, unless it would be cheaper to train.
        final String selectAbility = "model.ability.selectRecruit";
        if (player.checkGold(player.getRecruitPrice())
            && price > player.getRecruitPrice()
            && player.hasAbility(selectAbility)) {
            for (int i = 0; i < Europe.RECRUIT_COUNT; i++) {
                if (europe.getRecruitable(i) == unitType) {
                    return aiPlayer.recruitAIUnitInEurope(i);
                }
            }
        }

        // Try training the unit:
        if (price > 0 && player.checkGold(price)) {
            return aiPlayer.trainAIUnitInEurope(unitType);
        }

        return null;
    }

    /**
     * Returns the cheapest unit which can be bought in <code>Europe</code>.
     *
     * @return The <code>AIUnit</code>.
     */
    private AIUnit getCheapestUnitInEurope() {
        EuropeanAIPlayer aiPlayer = (EuropeanAIPlayer) getAIMain().getAIPlayer(getUnit().getOwner());
        Player player = aiPlayer.getPlayer();
        Europe europe = player.getEurope();

        if (!getUnit().isInEurope()) {
            throw new IllegalStateException("Carrier not in Europe");
        }
        if (!player.canRecruitUnits()) {
            return null;
        }

        // Check if there are any units on the docks:
        Iterator<Unit> ui = europe.getUnitIterator();
        while (ui.hasNext()) {
            Unit u = ui.next();
            if (!u.isCarrier() && getAIMain().getAIUnit(u).getTransport() == null) {
                return getAIMain().getAIUnit(u);
            }
        }

        int priceTrained = 0;
        UnitType cheapestTrained = null;
        List<UnitType> unitTypes = getSpecification().getUnitTypesTrainedInEurope();
        for (UnitType unitType : unitTypes) {
            int price = europe.getUnitPrice(unitType);
            if (cheapestTrained == null || price < priceTrained) {
            	cheapestTrained = unitType;
            	priceTrained = price;
            }
        }
        // Recruit a random unit.
        if (player.checkGold(player.getRecruitPrice())
            && cheapestTrained != null
            && player.getRecruitPrice() < priceTrained) {
            // TODO: Take the best unit (Seasoned scout, pioneer, soldier etc)
            return aiPlayer.recruitAIUnitInEurope(-1);
        }

        // Try training the unit:
        if (cheapestTrained != null && player.checkGold(priceTrained)) {
            return aiPlayer.trainAIUnitInEurope(cheapestTrained);
        }

        return null;
    }

    /**
     * Returns the path the carrier should use to get/drop the given
     * <code>Transportable</code>.
     *
     * @param transportable The <code>Transportable</code>.
     * @return The path.
     */
    public PathNode getTransportPath(Transportable transportable) {
        return getTransportPath(transportable, getUnit().getTile(),
            !isCarrying(transportable));
    }

    /**
     * Returns the path the carrier should use to get/drop the given
     * <code>Transportable</code>.
     *
     * @param transportable The <code>Transportable</code>.
     * @param start The <code>Tile</code> to search from. If
     *            <code>start == null</code> or
     *            <code>start.getTile() == null</code> then the carrier's
     *            {@link Unit#getEntryLocation entry location} is used instead.
     * @param collect True if the transportable must be collected.
     * @return The path.
     */
    private PathNode getTransportPath(Transportable transportable,
                                      Location start, boolean collect) {
        Unit carrier = getUnit();
        if (start == null || start.getTile() == null) {
            start = carrier.getFullEntryLocation();
        }

        Locatable locatable = transportable.getTransportLocatable();
        Location destination = (collect) ? locatable.getLocation()
            : transportable.getTransportDestination();
        if (destination == null) return null;
        if (destination.getTile() == null) {
            return (destination instanceof Europe) 
                ? findPathToEurope(start.getTile())
                : null;
        }

        PathNode path;
        if (locatable instanceof Unit && isCarrying(transportable)) {
            path = ((Unit)locatable).findPath(start.getTile(),
                                              destination.getTile(), carrier);
            if (path == null || path.getTransportDropNode().previous == null) {
                path = null;
            } else {
                path.getTransportDropNode().previous.next = null;
            }
        } else {
            path = carrier.findPath(start.getTile(), destination.getTile());
        }
        return path;
    }

    /**
     * Returns the available space for the given <code>Transportable</code>.
     *
     * @param t The <code>Transportable</code>
     * @return The space available for <code>Transportable</code>s with the
     *         same source and
     *         {@link Transportable#getTransportDestination destination}.
     */
    public int getAvailableSpace(Transportable t) {
        UnitType type = (t.getTransportLocatable() instanceof Unit)
            ? ((Unit)t.getTransportLocatable()).getType()
            : null;
        return getAvailableSpace(type, t.getTransportSource(),
                                 t.getTransportDestination());
    }

    /**
     * Returns the available space for the given type of <code>Unit</code> at
     * the given <code>Location</code>.
     *
     * @param unitType The type of {@link Unit} or <code>null</code> for
     *            {@link Goods}
     * @param source The source for the unit. This is where the unit is
     *            presently located.
     * @param destination The destination for the unit.
     * @return The space available
     */
    public int getAvailableSpace(UnitType unitType, Location source,
                                 Location destination) {
        // TODO: Implement this method properly:
        return Math.max(0, getUnit().getSpaceLeft() - transportList.size());
    }

    /**
     * Returns the available space for any type of unit going to any type of
     * location.
     *
     * @return The space available
     */
    public int getAvailableSpace() {
        // TODO: Implement this method properly:
        return Math.max(0, getUnit().getSpaceLeft() - transportList.size());
    }

    /**
     * Loads and unloads any <code>Transportable</code>.
     *
     * @return <code>true</code> if something has been loaded/unloaded and
     *         <code>false</code>otherwise.
     */
    private boolean restockCargoAtDestination() {
        return unloadCargoAtDestination() || loadCargoAtDestination();
    }

    /**
     * Unloads any <code>Transportable</code>s which have reached their
     * destination.
     *
     * @return <code>true</code> if something has been unloaded and
     *         <code>false</code>otherwise.
     */
    private boolean unloadCargoAtDestination() {
        Map map = getGame().getMap();
        Unit carrier = getUnit();
        boolean transportListChanged = false;

        // Sanitation
        if (carrier.isAtSea()) return false;

        // Make a copy for iteration, the main list may change inside the loop
        for (Transportable t : new ArrayList<Transportable>(transportList)) {
            if (!isCarrying(t)) continue; // To pickup, ignore

            if (t instanceof AIUnit) {
                AIUnit au = (AIUnit) t;
                Unit u = au.getUnit();
                Mission mission = au.getMission();
                String reason = null;
                boolean unload = false;
                Tile destTile;

                if (mission == null || !mission.isValid()) {
                    // Get rid of the unit ASAP if it has no mission
                    unload = true;
                    reason = "No valid mission";

                } else if (au.getTransportDestination() == null) {
                    // Get rid of the unit ASAP if it has no destination
                    unload = true;
                    reason = "No destination";

                } else if (au.getTransportDestination() instanceof Europe) {
                    if (carrier.isInEurope()) {
                        // Unload at destination of Europe
                        unload = true;
                        reason = "Arrived in Europe";
                    }

                } else if ((destTile = au.getTransportDestination().getTile())
                           != null) {
                    PathNode p;
                    if (carrier.getTile() == null) {
                        ;// Get back on the map
                    } else if (destTile == carrier.getTile()) {
                        // Unload at destination tile
                        unload = true;
                        reason = "Arrived at " + destTile;
                    } else if ((p = u.findPath(carrier.getTile(), destTile,
                                               carrier)) != null) {
                        final PathNode dropNode = p.getTransportDropNode();
                        int d;
                        if (dropNode != null && dropNode.getTile() != null
                            && (d = dropNode.getTile().getDistanceTo(carrier.getTile())) != Map.COST_INFINITY
                            && d <= 1) {
                            // Next to the drop node, proceed with mission
                            mission.doMission();
                            reason = "Next to drop node " + dropNode.getTile();
                        }
                    } else {
                        // Destination has become unreachable
                        unload = true;
                        reason = "No path to destination: " + destTile;
                    }
                }

                // If unloading, do not drop transported unit into the sea
                if (unload && (carrier.getSettlement() != null
                               || carrier.isInEurope())) {
                    unitLeavesShip(au);
                }
                // If unload or doMission succeeded, update the transportables
                if (u.getLocation() != carrier) {
                    removeFromTransportList(au);
                    while (transportList.remove(au)); // Make sure its gone!
                    transportListChanged = true;
                }
                if (reason != null) {
                    logger.finest("Unloading(" + reason + "," + unload
                                  + "): " + u
                                  + " from: " + carrier
                                  + " -> " + (u.getLocation() != carrier));
                }

            } else if (t instanceof AIGoods) {
                AIGoods ag = (AIGoods) t;
                String locStr = (carrier.isInEurope()) ? "Europe"
                    : (carrier.getLocation().getSettlement() != null)
                    ? carrier.getLocation().getSettlement().getName()
                    : carrier.getLocation().toString();
                if (ag.getTransportDestination() == null ||
                    (ag.getTransportDestination() != null
                     && ag.getTransportDestination().getTile() == carrier.getLocation().getTile())) {
                    logger.finest(carrier + "("
                                  + carrier.getId() + ") unloading " + ag + " at " + locStr);
                    if (carrier.isInEurope()) {
                        boolean success = sellCargoInEurope(ag.getGoods());
                        if(success){
                            removeFromTransportList(ag);
                            ag.dispose();
                            transportListChanged = true;
                        }
                    } else {
                        boolean success = unloadCargoInColony(ag.getGoods());
                        if (success) {
                            removeFromTransportList(ag);
                            ag.dispose();
                            transportListChanged = true;
                        }
                    }
                }
            } else {
                logger.warning("Unknown Transportable.");
            }
            // Kick the colony if the unit or goods available changes.
            Colony colony = carrier.getColony();
            if (colony != null) {
                colony.firePropertyChange(Colony.REARRANGE_WORKERS, true, false);
            }
        }

        return transportListChanged;
    }

    /**
     * Loads any <code>Transportable</code>s being in range of the carrier.
     *
     * @return <code>true</code> if something has been unloaded and
     *         <code>false</code>otherwise.
     */
    private boolean loadCargoAtDestination() {
        Unit carrier = getUnit();
        if (carrier.isAtSea()) return false;

        // TODO: Add code for rendez-vous.

        boolean transportListChanged = false;
        Iterator<Transportable> tli = transportList.iterator();
        while (tli.hasNext()) {
            if (carrier.getSpaceLeft() == 0) break;
            Transportable t = tli.next();
            if (isCarrying(t)) continue; // To deliver, ignore.

            if (t instanceof AIUnit) {
                AIUnit au = (AIUnit) t;
                Unit u = au.getUnit();
                if (u.isAtSea()) {
                    continue;
                } else if (carrier.getTile() == u.getTile()) {
                    if (carrier.getTile() != null
                        || (carrier.isInEurope() && u.isInEurope())) {
                        // Drop the transportable from the transport
                        // list without checking if embark succeeds or
                        // not--- if it succeeds all is well, if it
                        // fails it is likely to fail again for the
                        // same unknown reason so we might as well
                        // give up on it and do something else with
                        // the carrier.  Similarly also for the goods
                        // loads below.
                        if (u.getLocation() instanceof WorkLocation
                            && ((WorkLocation)u.getLocation()).getColony().getUnitCount() <= 1) {
                            ; // Do not load sole units in colonies.
                            // TODO: do this better.
                        } else {
                            AIMessage.askEmbark(getAIUnit(), u, null);
                        }
                        tli.remove();
                        transportListChanged = true;
                    } else {
                        throw new IllegalStateException("Bogus"
                            + " carrier at: " + carrier.getLocation()
                            + " unit at: " + u.getLocation());
                    }
                }
            } else if (t instanceof AIGoods) {
                AIGoods ag = (AIGoods) t;
                if (carrier.getTile() == ag.getGoods().getTile()) {
                    if (carrier.getTile() != null) {
                        AIMessage.askLoadCargo(getAIUnit(), ag.getGoods());
                        tli.remove();
                        transportListChanged = true;
                    } else if (carrier.isInEurope()) {
                        GoodsType goodsType = ag.getGoods().getType();
                        int goodsAmount = ag.getGoods().getAmount();
                        if (AIMessage.askBuyGoods(getAIUnit(), goodsType,
                                                  goodsAmount)) {
                            ag.setGoods(new Goods(getGame(), carrier,
                                                  goodsType, goodsAmount));
                        }
                        tli.remove();
                        transportListChanged = true;
                    } else {
                        throw new IllegalStateException("Bogus carrier at: "
                            + carrier.getLocation());
                    }
                }
            } else {
                logger.warning("Unknown Transportable: " + t);
            }
            // Kick the colony if the unit or goods available changes.
            Colony colony = carrier.getColony();
            if (colony != null) {
                colony.firePropertyChange(Colony.REARRANGE_WORKERS, true, false);
            }
        }

        return transportListChanged;
    }

    /**
     * Checks if this mission is valid for the given unit.
     *
     * @param aiUnit The unit.
     * @return <code>true</code> if this mission is valid to perform
     *         and <code>false</code> otherwise.
     */
    public static boolean isValid(AIUnit aiUnit) {
        return Mission.isValid(aiUnit)
            && aiUnit.getUnit().isCarrier();
    }

    /**
     * Checks if this mission is still valid to perform.
     *
     * @return <code>true</code>
     */
    public boolean isValid() {
        if (!super.isValid()) return false;
        updateTransportList();
        return !transportList.isEmpty();
    }

    /**
     * Returns the destination of a required transport.
     *
     * @return <code>null</code>
     */
    public Tile getTransportDestination() {
        return null;
    }

    /**
     * Returns the priority of getting the unit to the transport destination.
     *
     * @return 0.
     */
    public int getTransportPriority() {
        return 0;
    }

    /**
     * Unit is in Europe, unload cargo on board, buy required goods
     * and board unit.
     */
    private void inEurope() {
        restockCargoAtDestination();
        buyCargo();
        restockCargoAtDestination();

        // Move back to America:
        Unit carrier = getUnit();
        if (!carrier.getOwner().checkGold(MINIMUM_GOLD_TO_STAY_IN_EUROPE)
            || transportList.size() > 0) {
            moveUnitToAmerica();
        }
    }

    /**
     * Finds the best path to <code>Europe</code>.
     *
     * @param start The starting <code>Tile</code>.
     * @return The path to the target or <code>null</code> if no target can be
     *         found.
     * @see Europe
     */
    protected PathNode findPathToEurope(Tile start) {
        return getUnit().findPathToEurope(start);
    }

    /**
     * Gives the number of naval units assigned with a Transport Mission
     */
    public static int getPlayerNavalTransportMissionCount(AIPlayer aiPlayer, Unit unitExcluded){
        Player player = aiPlayer.getPlayer();
        int units = 0;

        for(Unit unit : player.getUnits()){
            if(unit == unitExcluded){
                continue;
            }
            if(!unit.isNaval()){
                continue;
            }
            AIUnit aiUnit = aiPlayer.getAIMain().getAIUnit(unit);
            if(aiUnit.getMission() instanceof TransportMission){
                units++;
            }
        }
        return units;
    }


    // Serialization

    /**
     * Writes all of the <code>AIObject</code>s and other AI-related
     * information to an XML-stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        toXML(out, getXMLElementTagName());
    }

    /**
     * {@inherit-doc}
     */
    protected void writeChildren(XMLStreamWriter out)
        throws XMLStreamException {
        for (Transportable t : transportList) {
            out.writeStartElement(ELEMENT_TRANSPORTABLE);
            out.writeAttribute(ID_ATTRIBUTE, ((AIObject)t).getId());
            out.writeEndElement();
        }
    }

    /**
     * {@inherit-doc}
     */
    protected void readChildren(XMLStreamReader in)
        throws XMLStreamException {
        transportList.clear();

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String tag = in.getLocalName();
            if (tag.equals(ELEMENT_TRANSPORTABLE)) {
                String tid = in.getAttributeValue(null, ID_ATTRIBUTE);
                AIObject ao = getAIMain().getAIObject(tid);
                if (ao == null) {
                    if (tid.startsWith(Unit.getXMLElementTagName())) {
                        ao = new AIUnit(getAIMain(), tid);
                    } else {
                        ao = new AIGoods(getAIMain(), tid);
                    }
                }
                if (ao instanceof Transportable) {
                    transportList.add((Transportable) ao);
                } else {
                    logger.warning("Transportable expected: " + tid);
                }
                in.nextTag(); // Consume closing tag
            } else {
                logger.warning("Unknown TransportMission tag: " + tag);
            }
        }
    }

    /**
     * Creates a <code>String</code> representation of this mission to
     * be used for debugging purposes.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append("\nTransport list:\n");
        List<Transportable> ts = new LinkedList<Transportable>();
        for(Transportable t : transportList) {
            Locatable l = t.getTransportLocatable();
            sb.append(l.toString());
            sb.append(" (");
            Location target;
            if (ts.contains(t) || isCarrying(t)) {
                sb.append("to ");
                target = t.getTransportDestination();
            } else {
                sb.append("from ");
                target = t.getTransportSource();
            }
            if (target instanceof Europe) {
                sb.append("Europe");
            } else if (target == null) {
                sb.append("null");
            } else {
                sb.append(target.toString());
            }
            sb.append(")");
            sb.append("\n");
            ts.add(t);
        }
        return sb.toString();
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "transportMission".
     */
    public static String getXMLElementTagName() {
        return "transportMission";
    }
}
