
package net.sf.freecol.common.model;


/**
* Represents a single <code>Tile</code> in a path.
*
* <br><br>
*
* You will most likely be using: {@link #next}, {@link #getDirection},
* {@link #getTile} and {@link #getTotalTurns}, when evaluating/following a path.
*/
public class PathNode implements Comparable<PathNode> {

    public static final  String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final  String  LICENSE   = "http://www.gnu.org/licenses/gpl.html";
    public static final  String  REVISION  = "$Revision$";

    private Tile tile;
    private int cost;
    
    /** 
    * This is <code>cost + heuristics</code>. The latter one is an estimate
    * for the cost from this <code>tile</code> and to the goal.
    */
    private int f;

    private int direction;
    private int movesLeft;
    private int turns;
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
    * @param tile The <code>Tile</code> this <code>PathNode</code> represents in the path.
    * @param cost The cost of moving to this <code>PathNode</code>'s <code>Tile</code>,
    *             given in {@link Unit#getMovesLeft move points}.
    * @param f    This is <code>cost + heuristics</code>. The latter one is an estimate
    *             for the cost from this <code>tile</code> and to the goal.
    * @param direction The direction to move on the map in order to get to the next
    *             <code>Tile</code> in the path.
    * @param movesLeft The number of moves remaining at this point in the path.
    * @param turns The number of turns it takes to reach this <code>PathNode</code>'s
    *             <code>Tile</code> from the start of the path.
    */
    public PathNode(Tile tile, int cost, int f, int direction, int movesLeft, int turns) {
        this.tile = tile;
        this.cost = cost;
        this.f = f;
        this.direction = direction;
        this.movesLeft = movesLeft;
        this.turns = turns;
    }


    /**
    * Returns the cost of moving to this <code>PathNode</code>'s tile.
    *
    * @return The cost of moving to this <code>PathNode</code>'s <code>Tile</code>,
    *         given in {@link Unit#getMovesLeft move points}.
    */
    public int getCost() {
        return cost;
    }


    /**
    * Gets the <code>Tile</code> of this <code>PathNode</code>.
    *
    * <br><br>
    *
    * That is; the <code>Tile</code> you reach if you move in the direction
    * given by {@link #getDirection} from the previous tile. Explained by code:
    * <br><br>
    * <code>map.getNeighbourOrNull(getDirection(), previous.getTile()) == getTile()</code>
    *
    * @return The <code>Tile</code> this <code>PathNode</code> represents in the path.
    */
    public Tile getTile() {
        return tile;
    }

    
    /**
    * Checks if the unit using this path is still onboard
    * it's transport.
    *
    * @return <code>true</code> if the unit is still onboard
    *       a carrier when using this path.
    * @see #getTransportDropTurns
    */
    public boolean isOnCarrier() {
        return onCarrier;
    }
    

    /**
    * Sets if the unit using this path is still onboard
    * it's transport.
    *
    * @param onCarrier Should be set to <code>true</code>
    *       in order to indicate that the unit using this
    *       path is still onboard the carrier on this
    *       path node.
    * @see #getTransportDropTurns
    */
    public void setOnCarrier(boolean onCarrier) {
        this.onCarrier = onCarrier;
    }


    /**
    * Returns the number of turns it takes to reach the 
    * {@link #getTransportDropNode transport node}.
    * 
    * @return The number of turns in takes to get to the node 
    *       where the unit using this path should leave it's 
    *       transport.
    */
    public int getTransportDropTurns() {
        PathNode temp = this;
        while (temp.next != null && temp.isOnCarrier()) {
            temp = temp.next;
        }
        return temp.getTurns();
    }
    

    /**
    * Returns the node where the unit using this path should
    * leave it's transport.
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
    * Returns the last node of this path.
    * @return The last <code>PathNode</code>.
    */
    public PathNode getLastNode() {
        PathNode temp = this;
        while (temp.next != null) {
            temp = temp.next;
        }
        return temp;
    }


    /**
    * Returns the estimated cost of the path at this stage.
    *
    * @return The <code>cost + heuristics</code>. The latter one is an estimate
    *         for the cost from this <code>tile</code> and to the goal.
    */
    public int getF() {
        return f;
    }


    /**
    * Returns the direction to move in order to get closer towards the goal.
    *
    * @return The direction to move on the map in order to get to the
    *         <code>Tile</code> returned by this <code>PathNode</code>'s
    *         {@link #getTile} in the path.
    */
    public int getDirection() {
        return direction;
    }


    /**
    * Returns the number of turns it will take to reach this <code>PathNode</code>'s
    * <code>Tile</code> in the path.
    *
    * @return The number of turns, using zero for the first move. <code>-1</code> is
    *         returned if the number of turns has not been calculated.
    */
    public int getTurns() {
        return turns;
    }


    /**
    * Returns the number of turns it will take to move the entire path,
    * from the starting <code>PathNode</code> until the end.
    *
    * @return The number of turns, using zero for the first move.
    */
    public int getTotalTurns() {
        PathNode temp = this;
        while (temp.next != null) {
            temp = temp.next;
        }
        return temp.getTurns();
    }


    /**
    * Returns the number of moves remaining at this point in the path.
    * @return The number of moves remaining. <code>-1</code> is
    *         returned if the number of moves left has not been calculated.
    */
    public int getMovesLeft() {
        return movesLeft;
    }

    
    /**
    * Sets the number of moves remaining at this point in the path.
    * @param movesLeft The number of moves remaining.
    */
    public void setMovesLeft(int movesLeft) {
        this.movesLeft = movesLeft;
    }


    /**
    * Compares this <code>PathNode</code>'s {@link #getF f} with the
    * <code>f</code> of the given object.
    *
    * <br><br>
    *
    * Note: this class has a natural ordering that is inconsistent with equals.
    *
    * @param o the object to be compared.
    * @return a negative integer, zero or a positive integer as this object is
    *         less than, equal to, or greater than the specified object.
    * @exception ClassCastException if the given object is not a <code>PathNode</code>.
    */
    public int compareTo(PathNode o) {
        return o.getF()-f;
    }
    
    /**
     * Checks if this <code>PathNode</code> is equal to another object.
     * 
     * @param o The <code>Object</code> to compare with.
     * @return <code>true</code> if the given object is a <code>PathNode</code>
     *      with the same {@link #getTile() tile} as this one.
     */
    public boolean equals(Object o) {
        if (!(o instanceof PathNode)) {
            return false;
        } else {
            return tile.getID().equals(((PathNode) o).getTile().getID()); 
        }
    }
    
    /**
     * Returns the hashCode of this object.
     */
    public int hashCode() {
        return tile.getX() * 10000 + tile.getY();
    }
}
