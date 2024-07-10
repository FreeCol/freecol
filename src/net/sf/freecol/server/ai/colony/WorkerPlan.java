package net.sf.freecol.server.ai.colony;

import java.util.List;

import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.UnitType;

public final class WorkerPlan {

    private final UnitType unitType;
    private final PlannedWorkLocation plannedWorkLocation;
    private final List<GoodsType> productionTypes;
    
    public WorkerPlan(UnitType unitType, PlannedWorkLocation plannedWorkLocation, List<GoodsType> productionTypes) {
        this.unitType = unitType;
        this.plannedWorkLocation = plannedWorkLocation;
        this.productionTypes = productionTypes;
    }
    
    public UnitType getUnitType() {
        return unitType;
    }
    
    public PlannedWorkLocation getPlannedWorkLocation() {
        return plannedWorkLocation;
    }
    
    public List<GoodsType> getProductionTypes() {
        return productionTypes;
    }
    
    @Override
    public String toString() {
        return plannedWorkLocation.toString()
                + ": " + Utils.compressIdForLogging(unitType.getId())
                + ": " + productionTypes.stream()
                    .map(gt -> {
                        final String tileProduction = plannedWorkLocation.getTile() == null ? "" : "=" + plannedWorkLocation.getTile().getMaximumPotential(gt, unitType);
                        return Utils.compressIdForLogging(gt.getId()) + tileProduction;
                    })
                    .reduce((a, b) -> a + "," + b)
                    .orElse("none");
    }
}
