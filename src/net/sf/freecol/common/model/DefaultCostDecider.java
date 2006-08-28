
package net.sf.freecol.common.model;


/**
 * Class for determining the cost of a single move.
 * 
 * <br /><br />
 * 
 * This {@link CostDecider} is used as a default by
 * {@link Map#findPath} and {@link Map#search} if no
 * other <code>CostDecider</code> has been specified.
 */
public class DefaultCostDecider implements CostDecider {
    
    public static final  String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final  String  LICENSE   = "http://www.gnu.org/licenses/gpl.html";
    public static final  String  REVISION  = "$Revision$";

    private int movesLeft;
    private boolean newTurn;
    
    /**
     * Determines the cost of a single move.
     * 
     * @param unit The <code>Unit</code> making the move.
     * @param oldTile The <code>Tile</code> we are moving from.
     * @param newTile The <code>Tile</code> we are moving to.
     * @param ml The remaining moves left.
     * @return The cost of moving the given unit from the
     *      <code>oldTile</code> to the <code>newTile</code>.
     */    
    public int getCost(Unit unit, Tile oldTile, Tile newTile, int ml, int turns) {
        movesLeft = ml;
        newTurn = false;
        
        if (newTile.getType() == Tile.UNEXPLORED) {
            // Not allowed to use an unexplored tile for a path:
            return ILLEGAL_MOVE;
        } else if (newTile.isLand() && unit.isNaval() && (newTile.getSettlement() == null
                || newTile.getSettlement().getOwner() != unit.getOwner())) {
            // Not allowed to move a naval unit on land:
            return ILLEGAL_MOVE;
        } else if (!newTile.isLand() && !unit.isNaval()) {
            // Not allowed to move a land unit on water:
            return ILLEGAL_MOVE;
        } else {
            int mc = newTile.getMoveCost(oldTile);
            
            if (newTile.getDefendingUnit(unit) != null
                    && newTile.getDefendingUnit(unit).getOwner() != unit.getOwner()) {
                // A unit is blocking the path:                
                mc += Math.max(0, 20 - turns * 4); 
            }

            if (newTile.getSettlement() != null
                    && newTile.getSettlement().getOwner() != unit.getOwner()) {
                // A settlement is blocking the path:
                return ILLEGAL_MOVE;
            } else if (mc - 2 <= movesLeft && getMovesLeft() != 0) {
                // Normal move: Using -2 in order to make 1/3 and 2/3 move count as 3/3.
                if (mc <= movesLeft) {
                    if ((unit.isNaval() || unit.getType() == Unit.WAGON_TRAIN) &&
                            newTile.getSettlement() != null) {
                        // A ship or wagon entering in a colony
                        movesLeft = 0;
                        return ml;
                    } else if (unit.isNaval() && oldTile.isLand() && oldTile.getSettlement() == null) {
                        // Ship on land due to it was in a colony which was abandoned
                        movesLeft = 0;
                        return ml;
                    } else {
                        movesLeft -= mc;
                        return mc;
                    }
                } else {
                    movesLeft = 0;
                    return ml;
                }
            } else if (movesLeft + 2 >= unit.getInitialMovesLeft()) {
                movesLeft = 0;
                return ml;
            } else {
                // This move takes an extra turn to complete:
                mc = getCost(unit, oldTile, newTile, unit.getInitialMovesLeft(), turns+1);
                newTurn = true;
                return ml + mc;
            }
        }
    }
    
    /**
     * Gets the number of moves left. This method should be
     * called after invoking {@link #getCost}.
     * 
     * @return The number og moves left.
     */
    public int getMovesLeft() {
        return movesLeft;
    }
    
    /**
     * Checks if a new turn is needed in order to make the
     * move. This method should be called after invoking 
     * {@link #getCost}.
     * 
     * @return <code>true</code> if the move requires a
     *      new turn and <code>false</code> otherwise.
     */      
    public boolean isNewTurn() {
        return newTurn;
    }
}    
