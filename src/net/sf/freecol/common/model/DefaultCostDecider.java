
package net.sf.freecol.common.model;


/**
 * Class for determining the cost of a single move.
 * 
 * <br /><br />
 * 
 * This {@link CostDecider} is used as a default by
 * {@link Map#findPath(Unit, Tile, Tile) findPath} and 
 * {@link Map#search(Unit, Tile, GoalDecider, CostDecider, int, Unit) search} 
 * if no other <code>CostDecider</code> has been specified.
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
        
        if (!newTile.isExplored()) {
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
            int mc = unit.getMoveCost(oldTile, newTile, ml);
            int moveType = unit.getMoveType(oldTile, newTile, ml);
            
            if (newTile.getSettlement() != null
                    && newTile.getSettlement().getOwner() != unit.getOwner()) {
                // A settlement is blocking the path:   
                switch (moveType) {
                    case Unit.ENTER_SETTLEMENT_WITH_CARRIER_AND_GOODS:
                    case Unit.ENTER_INDIAN_VILLAGE_WITH_FREE_COLONIST:
                    case Unit.ENTER_INDIAN_VILLAGE_WITH_MISSIONARY:
                    case Unit.ENTER_INDIAN_VILLAGE_WITH_SCOUT:
                    case Unit.ENTER_FOREIGN_COLONY_WITH_SCOUT:
                    case Unit.ATTACK:
                        if (unit.getDestination() == null ||
                                unit.getDestination().getTile() != newTile) {
                            movesLeft = 0;
                            return ml + unit.getInitialMovesLeft() * 5;
                        }
                        break;
                    case Unit.ILLEGAL_MOVE:
                        if (ml > 0) {
                            movesLeft = 0;
                            return ml + unit.getInitialMovesLeft() * 5;
                        }
                        break;
                }
            } else if (newTile.getDefendingUnit(unit) != null
                    && newTile.getDefendingUnit(unit).getOwner() != unit.getOwner()) {
                // A unit is blocking the path:                
                if (moveType != Unit.ATTACK || unit.getDestination() == null ||
                        unit.getDestination().getTile() != newTile)
                    mc += Math.max(0, 20 - turns * 4);
            } else if (newTile.isLand() && newTile.getFirstUnit() != null &&
                    newTile.getFirstUnit().isNaval() &&
                    newTile.getFirstUnit().getOwner() != unit.getOwner()) {
                // An enemy ship in land tile without a settlement is blocking the path:                
                mc += Math.max(0, 20 - turns * 4); 
            }

            if (mc <= ml) {
                movesLeft -= mc;
                return mc;
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
