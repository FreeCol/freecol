package net.sf.freecol.common.model.pathfinding;

import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;

/**
 * Cost deciders to be used while finding paths.
 */
public final class CostDeciders {

    private static final CostDecider BASE_COST_DECIDER = new BaseCostDecider();
    private static final CostDecider AVOID_BLOCKING_UNITS_COST_DECIDER = new AvoidBlockingUnitsCostDecider();
    
    /**
     * A <code>CostDecider</code> only considering
     * the number of tiles visited when determining the
     * cost.
     */
    private static final CostDecider tileCostDecider = new CostDecider() {
        public int getCost(Unit unit, Tile oldTile, Tile newTile, int movesLeft, int turns) {
            if (unit.isNaval() 
                    && newTile.isLand()
                    && (newTile.getSettlement() == null
                        || newTile.getSettlement().getOwner().equals(unit.getOwner()))) {
                return ILLEGAL_MOVE;
            } else if (!unit.isNaval() && !newTile.isLand()){
                return ILLEGAL_MOVE;
            } else {
                return 1;
            }
        }
        public int getMovesLeft() {
            return 0;
        }
        public boolean isNewTurn() {
            return false;
        }
    };

    /**
     * Selects a default <code>CostDecider</code> for the given units.
     * 
     * @param unit The unit to create a <code>CostDecider</code> for.
     * @return A <code>CostDecider</code> depending on the owner of the
     *      unit (ai/human) and if the unit can attack other units.
     */
    public static CostDecider defaultFor(final Unit unit) {
        if (!unit.getOwner().isAI()) {
            return avoidSettlements();
        }
        return unit.isOffensiveUnit() ? BASE_COST_DECIDER : AVOID_BLOCKING_UNITS_COST_DECIDER;
    }

    
    /**
     * A <code>CostDecider</code> returning only
     * the cost of moving across the terrain (no additional
     * cost for blocking enemy units etc). Paths containing
     * an enemy settlement are considered illegal.
     * 
     * @return The <code>CostDecider</code>.
     */
    public static CostDecider avoidSettlements() {
        return BASE_COST_DECIDER;
    }
    
    /**
     * A <code>CostDecider</code> for avoiding using tiles
     * which have blocking enemy units on them. Paths containing
     * an enemy settlement are considered illegal, and so are
     * paths where the next move has an enemy unit on it.
     * 
     * @return The <code>CostDecider</code>.
     */
    public static CostDecider avoidSettlementsAndBlockingUnits() {
        return AVOID_BLOCKING_UNITS_COST_DECIDER;
    }
    
    /**
     * A <code>CostDecider</code> only considering
     * the number of tiles visited when determining the
     * cost.
     * 
     * @return The <code>CostDecider</code>.
     */
    public static CostDecider numberOfTiles() {
        return tileCostDecider;
    }
}
