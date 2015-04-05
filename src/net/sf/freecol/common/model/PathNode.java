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


package net.sf.freecol.common.model;

import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.util.LogBuilder;


/**
 * Represents a single <code>Location</code> in a path.
 *
 * You will most likely be using: {@link #next}, {@link #getDirection},
 * {@link #getTile} and {@link #getTotalTurns}, when
 * evaluating/following a path.
 */
public class PathNode {

    /** Weight turns >> moves. */
    private static final int TURN_FACTOR = 100;

    /** The location this node refers to.  Usually a Tile. */
    private final Location location;

    /**
     * The number of moves left at this node for the unit traversing
     * this path.
     */
    private int movesLeft;

    /**
     * The number of turns used to get to this node by the unit
     * traversing the path.
     */
    private int turns;

    /** Whether the unit traversing this path is on a carrier at this node. */
    private boolean onCarrier = false;

    /** The next node in the path. */
    public PathNode next = null;

    /** The previous node in the path. */
    public PathNode previous = null;


    /**
     * Creates a new <code>PathNode</code>.
     *
     * @param location The <code>Location</code> this
     *      <code>PathNode</code> represents in the path.
     * @param movesLeft The number of moves remaining at this point in
     *      the path.
     * @param turns The number of turns it takes to reach this
     *      <code>PathNode</code>'s <code>Tile</code> from the start
     *      of the path.
     * @param onCarrier Whether the path is still using a carrier.
     * @param previous The previous <code>PathNode</code> in the path.
     * @param next The next <code>PathNode</code> in the path.
     */
    public PathNode(Location location, int movesLeft, int turns,
                    boolean onCarrier, PathNode previous, PathNode next) {
        this.location = location;
        this.movesLeft = movesLeft;
        this.turns = turns;
        this.onCarrier = onCarrier;
        this.previous = previous;
        this.next = next;
    }


    /**
     * Gets the location of this path.
     *
     * @return The <code>Location</code>.
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Gets the <code>Tile</code> of this <code>PathNode</code>.
     *
     * @return The <code>Tile</code> this <code>PathNode</code>
     *     represents in the path, if any.
     */
    public Tile getTile() {
        return (location == null) ? null : location.getTile();
    }

    /**
     * Gets the number of moves remaining at this point in the path.
     *
     * @return The number of moves remaining. <code>-1</code> is
     *     returned if the number of moves left has not been calculated.
     */
    public int getMovesLeft() {
        return movesLeft;
    }

    /**
     * Sets the number of moves remaining at this point in the path.
     *
     * @param movesLeft The number of moves remaining.
     */
    public void setMovesLeft(int movesLeft) {
        this.movesLeft = movesLeft;
    }

    /**
     * Gets the number of turns it will take to reach this
     * <code>PathNode</code>'s <code>Tile</code> in the path.
     *
     * @return The number of turns, using zero for the first
     *     move. <code>-1</code> is returned if the number of turns
     *     has not been calculated.
     */
    public int getTurns() {
        return turns;
    }

    /**
     * Sets the number of turns it will take to reach this
     * <code>PathNode</code>'s <code>Tile</code> in the path.
     *
     * @param turns The new number of turns.
     */
    public void setTurns(int turns) {
        this.turns = turns;
    }

    /**
     * Adds turns to the turns on this path.
     *
     * @param turns The number of turns to add.
     */
    public void addTurns(int turns) {
        for (PathNode p = this; p != null; p = p.next) {
            p.setTurns(p.getTurns() + turns);
        }
    }

    /**
     * Checks if the unit using this path is still onboard its transport.
     *
     * @return <code>true</code> if the unit is still onboard a
     *     carrier when using this path.
     * @see #getTransportDropTurns
     */
    public boolean isOnCarrier() {
        return onCarrier;
    }

    /**
     * Sets if the unit using this path is still onboard its transport.
     *
     * @param onCarrier Should be set to <code>true</code> in order to
     *     indicate that the unit using this path is still onboard the
     *     carrier on this path node.
     * @see #getTransportDropTurns
     */
    public void setOnCarrier(boolean onCarrier) {
        this.onCarrier = onCarrier;
    }

    /**
     * Get the length of the path.
     *
     * @return The number of nodes in the path.
     */
    public int getLength() {
        int n = 0;
        for (PathNode temp = this; temp != null; temp = temp.next) n++;
        return n;
    }
        
    /**
     * Gets the direction to move in order to get to this path node.
     *
     * @return The direction to move on the map in order to get to the
     *     <code>Tile</code> returned by this <code>PathNode</code>'s
     *     {@link #getTile}, or null if there is no previous node or either
     *     this or the previous node location is not on the map.
     */
    public Direction getDirection() {
        if (previous == null
            || previous.getTile() == null
            || getTile() == null) return null;
        Tile prev = previous.getTile();
        return prev.getMap().getDirection(prev, getTile());
    }

    /**
     * Gets the node where the unit using this path should leave its
     * transport.
     *
     * @return The node where the unit leaves the carrier.
     */
    public PathNode getTransportDropNode() {
        PathNode p = this;
        while (p.next != null && p.isOnCarrier()) p = p.next;
        return p;
    }

    /**
     * Gets the first node of this path.
     *
     * @return The first <code>PathNode</code>.
     */
    public PathNode getFirstNode() {
        PathNode path;
        for (path = this; path.previous != null; path = path.previous);
        return path;
    }

    /**
     * Gets the last node of this path.
     *
     * @return The last <code>PathNode</code>.
     */
    public PathNode getLastNode() {
        PathNode path;
        for (path = this; path.next != null; path = path.next);
        return path;
    }

    /**
     * Gets the number of turns it will take to move the entire path,
     * from the starting <code>PathNode</code> until the end.
     *
     * @return The number of turns, using zero for the first move.
     */
    public int getTotalTurns() {
        PathNode path = getLastNode();
        int n = path.getTurns();
        if (path.getMovesLeft() == 0) n++;
        return n;
    }

    /**
     * Gets the number of turns it takes to reach the
     * {@link #getTransportDropNode transport node}.
     *
     * @return The number of turns in takes to get to the node where
     *     the unit using this path should leave it's transport.
     */
    public int getTransportDropTurns() {
        return getTransportDropNode().getTurns();
    }

    /**
     * Standard function to get the cost of moving to a <code>PathNode</code>.
     *
     * Static version provided for path calculation comparisons.  Some
     * care is taken to avoid overflow as test paths with infinite
     * moves are created in the path planning process.
     *
     * @param turns The number of turns taken.
     * @param movesLeft The number of moves left for the moving unit.
     * @return The cost of moving to a <code>PathNode</code>.
     */
    public static int getCost(int turns, int movesLeft) {
        return (turns >= FreeColObject.INFINITY / (TURN_FACTOR + 1))
            ? FreeColObject.INFINITY
            : TURN_FACTOR * turns + (TURN_FACTOR - movesLeft);
    }
        
    /**
     * Gets the cost of moving to this <code>PathNode</code>.
     *
     * @return The cost of moving to this <code>PathNode</code>.
     */
    public int getCost() {
        return getCost(turns, movesLeft);
    }

    /**
     * Gets the next carrier move on this path.
     *
     * @return The first node along the path which is a carrier move, or null
     *     if the path does not use a carrier.
     */
    public PathNode getCarrierMove() {
        for (PathNode p = this; p != null; p = p.next) {
            if (p.isOnCarrier()) return p;
        }
        return null;
    }

    /**
     * Does this path us a carrier at any point?
     *
     * @return True if there is an onCarrier move in this path.
     */
    public boolean usesCarrier() {
        return getFirstNode().getCarrierMove() != null;
    }

    /**
     * Does this path include a non-carrier move within a given turn?
     *
     * @param turns Paths with fewer turns than this are previous turns.
     * @return True if there was a non-carrier move in the last turn.
     */
    public boolean embarkedThisTurn(int turns) {
        for (PathNode p = this; p != null; p = p.previous) {
            if (p.getTurns() < turns) return false;
            if (!p.isOnCarrier()) return true;
        }
        return false;
    }

    /**
     * Convert this path to a delivery path for some goods, where
     * every node is marked as on carrier, except for a duplicate of
     * the last node.
     */
    public void convertToGoodsDeliveryPath() {
        PathNode p;
        for (p = this; p.next != null; p = p.next) {
            p.onCarrier = true;
        }
        p.onCarrier = true;
        ensureDisembark();
    }

    /**
     * Ensure the last node of this path is no longer on the carrier.
     */
    public void ensureDisembark() {
        PathNode p = this.getLastNode();
        if (p.isOnCarrier()) {
            p.next = new PathNode(p.location, p.movesLeft, p.turns, false,
                                  p, null);
        }
    }
           
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        LogBuilder lb = new LogBuilder(256);
        lb.add("PathNode loc=", location, " movesLeft=", movesLeft,
            " turns=", turns, " onCarrier=", onCarrier,
            " direction=", getDirection(), " cost=", getCost());
        return lb.toString();
    }

    /**
     * Another debug helper.
     *
     * @return A string describing the whole path.
     */
    public String fullPathToString() {
        LogBuilder lb = new LogBuilder(500);
        PathNode p;
        for (p = this; p != null; p = p.next) lb.add(p, "\n");
        return lb.toString();
    }
}
