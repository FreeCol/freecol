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


package net.sf.freecol.common.model;

import  net.sf.freecol.common.model.Location;
import  net.sf.freecol.common.model.Map.Direction;
import  net.sf.freecol.common.model.Tile;


/**
 * Represents a single <code>Location</code> in a path.
 *
 * You will most likely be using: {@link #next}, {@link #getDirection},
 * {@link #getTile} and {@link #getTotalTurns}, when
 * evaluating/following a path.
 */
public class PathNode {

    /**
     * The location this node refers to.  Usually a Tile.
     */
    private Location location;

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

    /**
     * Whether the unit traversing this path is on a carrier at this node.
     */
    private boolean onCarrier = false;

    /**
     * The next node in the path.
     */
    public PathNode next = null;

    /**
     * The previous node in the path.
     */
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
     * @return The node where the unit leaves it's carrier.
     */
    public PathNode getTransportDropNode() {
        PathNode temp = this;
        while (temp.next != null && temp.isOnCarrier()) {
            temp = temp.next;
        }
        return temp;
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
        return getLastNode().getTurns();
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
     * Static version provided for path calculation comparisons.
     *
     * @param turns The number of turns taken.
     * @param movesLeft The number of moves left for the moving unit.
     * @return The cost of moving to a <code>PathNode</code>.
     */
    public static int getCost(int turns, int movesLeft) {
        return 100 * turns + (100 - movesLeft);
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
        for (PathNode path = this; path != null; path = path.next) {
            if (path.isOnCarrier()) return path;
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
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("PathNode loc=");
        sb.append(((FreeColGameObject)location).toString())
            .append(" movesLeft=").append(movesLeft)
            .append(" turns=").append(turns)
            .append(" onCarrier=").append(onCarrier)
            .append(" direction=").append(getDirection())
            .append(" cost=").append(getCost());
        return sb.toString();
    }

    /**
     * Another debug helper.
     *
     * @return A string describing the whole path.
     */
    public String fullPathToString() {
        StringBuilder sb = new StringBuilder(500);
        PathNode p;
        for (p = this; p != null; p = p.next) {
            sb.append(p.toString()).append("\n");
        }
        return sb.toString();
    }
}
