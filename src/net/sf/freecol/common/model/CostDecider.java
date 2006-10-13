
package net.sf.freecol.common.model;


/**
 * Determines the cost of a single move. Used by {@link Map#findPath(Unit, Tile, Tile) findPath} 
 * and {@link Map#search(Unit, Tile, GoalDecider, CostDecider, int, Unit) search}.
 */
public interface CostDecider {
    
    public static final  String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final  String  LICENSE   = "http://www.gnu.org/licenses/gpl.html";
    public static final  String  REVISION  = "$Revision$";

    public static final int ILLEGAL_MOVE = -1;
    
    /**
     * Determines the cost of a single move.
     * 
     * @param unit The <code>Unit</code> that will be used when
     *      determining the cost. This should be the same type
     *      of unit as the one following the path.
     * @param oldTile The <code>Tile</code> we are moving from.
     * @param newTile The <code>Tile</code> we are moving to.
     * @param movesLeft The remaining moves left. The
     *      <code>CostDecider</code> can use this information
     *      if needed.
     * @param turns The number of turns spent so far.
     * @return The cost of moving the given unit from the
     *      <code>oldTile</code> to the <code>newTile</code>.
     */
    public int getCost(Unit unit, Tile oldTile, Tile newTile, int movesLeft, int turns);   
    
    /**
     * Gets the number of moves left. This method should be
     * called after invoking {@link #getCost}.
     * 
     * @return The number og moves left.
     */
    public int getMovesLeft();

    /**
     * Checks if a new turn is needed in order to make the
     * move. This method should be called after invoking 
     * {@link #getCost}.
     * 
     * @return <code>true</code> if the move requires a
     *      new turn and <code>false</code> otherwise.
     */    
    public boolean isNewTurn();    
}
