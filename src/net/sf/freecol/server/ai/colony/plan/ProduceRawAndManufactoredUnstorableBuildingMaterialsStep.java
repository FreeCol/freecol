/**
 *  Copyright (C) 2002-2024   The FreeCol Team
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

package net.sf.freecol.server.ai.colony.plan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.FeatureContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.ProductionInfo;
import net.sf.freecol.common.model.ProductionType;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.production.BuildingProductionCalculator;
import net.sf.freecol.common.model.production.WorkerAssignment;
import net.sf.freecol.server.ai.colony.PlannedWorkLocation;
import net.sf.freecol.server.ai.colony.TentativeColonyPlan;
import net.sf.freecol.server.ai.colony.TentativeColonyPlan.TentativeColonyTilePlan;
import net.sf.freecol.server.ai.colony.WorkerPlan;

/**
 * Produces unstorable building materials (hammers) along with the required raw materials.
 * 
 * Note that this step requires all raw materials (lumber) to be produced in the colony. If not,
 * then no additional buildings or worker plans are added.
 */
final class ProduceRawAndManufactoredUnstorableBuildingMaterialsStep {

    private ProduceRawAndManufactoredUnstorableBuildingMaterialsStep() {}
    
    /**
     * @ses {@link InitialSteps#produceRawAndManufactoredUnstorableBuildingMaterials(TentativeColonyPlan)}
     */
    static void execute(TentativeColonyPlan tcp) {
        final Specification s = tcp.getSpecification();
        
        final List<BuildingType> buildingTypes = allMaximumLevelBuildingHouses(s);
        if (buildingTypes.isEmpty()) {
            return;
        }
        
        final List<TentativeColonyTilePlan> rawBuildingMaterialsTilePlans = createRawBuildingMaterialsTilePlans(tcp);
        if (rawBuildingMaterialsTilePlans == null) {
            /* We are missing required raw materials. */
            return;
        }

        if (!isSufficientAmountOfFood(tcp, rawBuildingMaterialsTilePlans)) {
            /*
             * For now, just stop producing the raw materials.
             * 
             * Some cases would probably be better solved by:
             * 1. Produce food on just a single of the tiles if there are multiple raw building material bonus resources.
             * 2. Use a bonus resource for food production if the bonus resources have different goods types.
             */
            return;
        }
        
        /*
         * Both raw materials and building OK. Add them.
         */

        for (TentativeColonyTilePlan tctp : rawBuildingMaterialsTilePlans) {
            tcp.addOrReplace(tctp);
        }
        
        final List<AbstractGoods> tentativeProduction2 = tcp.determineTentativeProduction();
        for (BuildingType buildingType : buildingTypes) {
            tcp.addBuildable(buildingType);
            
            final GoodsType producedGoodsType = buildingType.getProducedGoodsType();
            final ProductionType productionType = ProductionType.getBestProductionType(producedGoodsType, buildingType.getAvailableProductionTypes(false));
            final UnitType unitType = s.getExpertForProducing(producedGoodsType);
            
            final int maximumWorkPlaces = buildingType.getWorkPlaces();

            final BuildingProductionCalculator bpc = new BuildingProductionCalculator(null, new FeatureContainer(), 2);
            final ProductionInfo pi = bpc.getAdjustedProductionInfo(buildingType, new Turn(0), List.of(new WorkerAssignment(unitType, productionType)), List.of(), List.of(), 10_000);
            
            final GoodsType primaryInputType = buildingType.getAvailableProductionTypes(false)
                    .stream()
                    .map(p -> p.getInputList())
                    .flatMap(Collection::stream)
                    .max((a, b) -> Integer.compare(a.getAmount(), b.getAmount()))
                    .map(AbstractGoods::getType)
                    .orElseThrow()
                    ;
            
            final int inputUsedPerWorker = pi.getMaximumConsumption().stream()
                    .filter(ag -> ag.getType() == primaryInputType)
                    .map(ag -> ag.getAmount())
                    .findFirst()
                    .orElse(0);

            final int maximumAvailableInput = tentativeProduction2.stream()
                    .filter(g -> g.getType().equals(primaryInputType))
                    .findFirst()
                    .orElseThrow()
                    .getAmount();
            
            int workPlacesUsed = 0;
            while (workPlacesUsed < maximumWorkPlaces) {
                final WorkerPlan workerPlan = new WorkerPlan(unitType, new PlannedWorkLocation(buildingType), List.of(producedGoodsType));
                tcp.addBuildingWorkerPlan(workerPlan);
                workPlacesUsed++;
                if (inputUsedPerWorker * (workPlacesUsed + 1) >= maximumAvailableInput) {
                    break;
                }
            }
        }
    }


    private static boolean isSufficientAmountOfFood(TentativeColonyPlan tcp, List<TentativeColonyTilePlan> rawBuildingMaterialsTilePlans) {
        final int surplusFood = tcp.determinePossibleVacantTileFoodSurplusAfterAddingOrReplacing(rawBuildingMaterialsTilePlans);
        final int foodConsumedByOneWorker = tcp.foodConsumedByOneWorker();
        final int MINIMUM_WORKERS_IN_BUILDERS_HOUSE = 1;
        final int minimumNeededFood = foodConsumedByOneWorker * MINIMUM_WORKERS_IN_BUILDERS_HOUSE;
        
        return (surplusFood >= minimumNeededFood);
    }


    private static List<TentativeColonyTilePlan> createRawBuildingMaterialsTilePlans(TentativeColonyPlan tcp) {
        final Specification s = tcp.getSpecification();
        
        final List<GoodsType> rawMaterialForUnstorableBuildingMaterials = s.getRawMaterialsForUnstorableBuildingMaterials();
        final List<AbstractGoods> tentativeProduction = tcp.determineTentativeProduction();
        final List<GoodsType> requiredRawMaterialForUnstorableBuildingMaterials = rawMaterialForUnstorableBuildingMaterials.stream()
                .filter(goodsType -> tentativeProduction.stream().noneMatch(g -> g.getType().equals(goodsType) && g.getAmount() > 0))
                .collect(Collectors.toList());
        
        final List<TentativeColonyTilePlan> rawBuildingMaterialsTilePlans = findWorkerPlansForMissingRawMaterials(tcp,
                rawMaterialForUnstorableBuildingMaterials, requiredRawMaterialForUnstorableBuildingMaterials);
        
        if (rawBuildingMaterialsTilePlans.size() != requiredRawMaterialForUnstorableBuildingMaterials.size()) {
            /* We are missing required raw materials. */
            return null;
        }
        return rawBuildingMaterialsTilePlans;
    }
    
    private static List<TentativeColonyTilePlan> findWorkerPlansForMissingRawMaterials(TentativeColonyPlan tcp,
            List<GoodsType> rawMaterialForUnstorableBuildingMaterials,
            List<GoodsType> requiredRawMaterialForUnstorableBuildingMaterials) {
        
        final List<TentativeColonyTilePlan> rawBuildingMaterialsTilePlans = new ArrayList<>();
        final Set<Tile> usedTiles = new HashSet<>();
        for (GoodsType goodsType : requiredRawMaterialForUnstorableBuildingMaterials) {
            Tile chosenTile = tcp.findBestTileForProducing(tcp.unassignedExcept(usedTiles), goodsType);
            if (chosenTile == null) {
                /* 
                 * Prioritize having raw material (lumber) for unstorable building material (hammers)
                 * above using a bonus resource.
                 */
                
                // Reserved tiles:
                final Set<Tile> ignoreTilesWithRequiredResources = new HashSet<>(usedTiles);
                for (GoodsType requiredGoodsType : rawMaterialForUnstorableBuildingMaterials) {
                    tcp.findTentativeColonyTilePlanProducing(requiredGoodsType).stream()
                        .findFirst()
                        .ifPresent(keep -> ignoreTilesWithRequiredResources.add(keep.getTile()));
                }
                
                chosenTile = tcp.findBestTileForProducing(tcp.workTilesExcept(ignoreTilesWithRequiredResources), goodsType);
            }
            if (chosenTile == null) {
                return List.of();
            }
            
            final TentativeColonyTilePlan tentativeColonyTilePlan = TentativeColonyTilePlan.expertProduction(chosenTile, goodsType);
            rawBuildingMaterialsTilePlans.add(tentativeColonyTilePlan);
            usedTiles.add(chosenTile);
        }
        return rawBuildingMaterialsTilePlans;
    }
    
    private static List<BuildingType> allMaximumLevelBuildingHouses(final Specification s) {
        return s.getBuildingTypeList().stream().filter(buildingType -> {
            final GoodsType producedGoodsType = buildingType.getProducedGoodsType();
            return producedGoodsType != null
                    && producedGoodsType.isBuildingMaterial()
                    && !producedGoodsType.isStorable()
                    && buildingType.getUpgradesTo() == null;
        }).collect(Collectors.toList());
    }
}
