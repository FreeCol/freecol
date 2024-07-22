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

package net.sf.freecol.server.ai.colony;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.FeatureContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.ProductionInfo;
import net.sf.freecol.common.model.ProductionType;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.production.BuildingProductionCalculator;
import net.sf.freecol.common.model.production.WorkerAssignment;
import net.sf.freecol.server.ai.colony.TentativeColonyPlan.TentativeColonyTilePlan;
import net.sf.freecol.server.ai.colony.TentativeColonyPlan.TentativeProduction;
import net.sf.freecol.server.ai.colony.plan.InitialSteps;

/**
 * The default {@code ColonyPlanner} for European players.
 */
public final class StandardColonyPlanner implements ColonyPlanner {

    public StandardColonyPlanner() {
        
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public ColonyPlan createPlan(Tile centerTile) {
        final TentativeColonyPlan tcp = new TentativeColonyPlan(centerTile);
        
        InitialSteps.produceGoodsTypesEnhancedByBonusResources(tcp);
        InitialSteps.produceFoodOnTilesWhereNoOtherPossibleOutputExists(tcp);
        InitialSteps.produceRawAndManufactoredUnstorableBuildingMaterials(tcp);

        stopWhenNoMoreFoodIsAvailable(tcp,
            /*
             *  If needed, this ensures sufficient food by reorganizing the workers
             *  assigned above.
             *  
             *  Every other step adds just a single colonist, at a time, before
             *  checking if more food need to be produced.
             */
            StandardColonyPlanner::produceFoodForConsumption,
            
            StandardColonyPlanner::produceLibertyBells,
            StandardColonyPlanner::produceRawMaterialForStorableBuildingMaterials, // ore.
            StandardColonyPlanner::produceStorableBuildingMaterials, // tools+muskets
            StandardColonyPlanner::produceRefinedGoods,
            StandardColonyPlanner::produceLibertyBells,
            
            StandardColonyPlanner::produceRawMaterialsToAvoidDeficits,
            StandardColonyPlanner::produceMoreOfExistingRefinedGoodsAlongWithRequiredRawMaterials,
            
            StandardColonyPlanner::produceRawMaterialsToAvoidDeficits,
            StandardColonyPlanner::produceMoreOfExistingRefinedGoodsAlongWithRequiredRawMaterials,
            
            StandardColonyPlanner::produceGoodsTypeForWeapons, // muskets

            StandardColonyPlanner::produceNewRawMaterialAndRefine,
            StandardColonyPlanner::produceRawMaterialsToAvoidDeficits,
            StandardColonyPlanner::produceMoreOfExistingRefinedGoodsAlongWithRequiredRawMaterials,

            StandardColonyPlanner::produceLibertyBells,
            
            StandardColonyPlanner::produceRawMaterialsJustForExportIfNoIndustry,
            StandardColonyPlanner::produceRawMaterialsJustForExportIfNoIndustry,
            StandardColonyPlanner::produceRawMaterialsJustForExportIfNoIndustry,
            
            StandardColonyPlanner::produceEducation,
            
            StandardColonyPlanner::produceFoodOnRemainingTiles,
            
            StandardColonyPlanner::produceImmigrationGoods // crosses
        );
        
        buildDocks(tcp);
        buildShipyard(tcp);
        buildExportBuildings(tcp); // custom house
        buildDefensiveBuildings(tcp);
        buildRequirementsForMilitaryLandUnitBuilding(tcp); // armory for artillery
        buildAutoProductionBuildings(tcp); // stables
        buildStorage(tcp);

        return tcp.toColonyPlan();
    }
    
    
    public void buildAutoProductionBuildings(TentativeColonyPlan tcp) {
        if (!tcp.canConstructBuildings()) {
            return;
        }
        
        final Specification s = tcp.getSpecification();
        final List<BuildingType> autoProducers = s.getTypesWithAbility(BuildingType.class, Ability.AUTO_PRODUCTION).stream()
                .filter(bt -> bt.getUpgradesTo() == null)
                .collect(Collectors.toList());
        final TentativeProduction production = tcp.determineTentativeProductionAndDeficits();
        
        final int MINIMUM_INPUT_REQUIRED = 8;
        for (BuildingType buildingType : autoProducers) {
            final boolean requiredInputProduced = buildingType.getAvailableProductionTypes(true).stream()
                    .filter(pt -> pt.getInputs().allMatch(ag -> production.getAmountProduced(ag.getType().getStoredAs()) > MINIMUM_INPUT_REQUIRED))
                    .findAny()
                    .isPresent();
            if (!requiredInputProduced) {
                continue;
            }
            
            tcp.addBuildable(buildingType);
        }
    }


    public void buildShipyard(TentativeColonyPlan tcp) {
        if (!tcp.canConstructBuildings()) {
            return;
        }
        
        final Tile centerTile = tcp.getCenterTile();
        if (!centerTile.isHighSeasConnected()) {
            return;
        }
        
        // TODO: Don't produce shipyard if artillery production already allowed.
        
        final Specification s = tcp.getSpecification();
        for (BuildingType bt : shipEnablingBuildings(s)) {
            tcp.addBuildable(bt);
            break;
        }
    }
    
    public void buildStorage(TentativeColonyPlan tcp) {
        final Specification s = tcp.getSpecification();
        final List<BuildingType> warehouseBuildingTypes = s.getBuildingTypeList()
                .stream()
                .filter(bt -> bt.hasModifier(Modifier.WAREHOUSE_STORAGE) && bt.getUpgradesTo() == null)
                .collect(Collectors.toList());
        warehouseBuildingTypes.stream().forEach(bt -> tcp.addBuildable(bt));
    }


    public void buildDocks(TentativeColonyPlan tcp) {
        final boolean needsDocks = tcp.getTentativeColonyTilePlans().stream()
            .filter(tp -> tp != null && tp.getTile() != null)
            .anyMatch(tp -> !tp.getTile().isLand());
        if (!needsDocks) {
            return;
        }
        final BuildingType docks = tcp.getSpecification().getBuildingTypeList()
                .stream()
                .filter(bt -> bt.hasAbility(Ability.PRODUCE_IN_WATER))
                .findFirst()
                .orElse(null);
        if (docks == null) {
            return;
        }
        tcp.addBuildable(docks);
    }

    public void buildRequirementsForMilitaryLandUnitBuilding(TentativeColonyPlan tcp) {
        if (!tcp.canConstructBuildings()) {
            return;
        }
        
        final Tile centerTile = tcp.getCenterTile();
        if (centerTile.isHighSeasConnected()) {
            // Shipyard already built.
            return;
        }
        
        final Specification s = tcp.getSpecification();
        final BuildingType bt = lowestLevelMilitaryLandUnitEnablingBuildings(s);
        tcp.addBuildable(bt);
    }


    public void buildDefensiveBuildings(TentativeColonyPlan tcp) {
        if (!tcp.canConstructBuildings()) {
            return;
        }
        
        final Specification s = tcp.getSpecification();
        final List<BuildingType> defensiveBuildings = s.getBuildingTypeList().stream()
                .filter(bt -> bt.isDefenceType())
                .filter(bt -> bt.getUpgradesTo() == null)
                .collect(Collectors.toList());
        
        for (BuildingType bt : defensiveBuildings) {
            tcp.addBuildable(bt);
        }
    }


    public void buildExportBuildings(TentativeColonyPlan tcp) {
        if (!tcp.canConstructBuildings()) {
            return;
        }
        
        final Specification s = tcp.getSpecification();
        
        final List<BuildingType> exportBuildings = s.getTypesWithAbility(BuildingType.class, Ability.EXPORT).stream()
                .filter(bt -> bt.getUpgradesTo() == null)
                .collect(Collectors.toList());
        for (BuildingType bt : exportBuildings) {
            tcp.addBuildable(bt);
            break;
        }
    }


    @SafeVarargs
    public static void stopWhenNoMoreFoodIsAvailable(TentativeColonyPlan tcp, Function<TentativeColonyPlan, Boolean>... functions) {
        for (Function<TentativeColonyPlan, Boolean> function : functions) {
            if (!function.apply(tcp)) {
                return;
            }
        }
    }
    
    public static boolean produceRawMaterialsToAvoidDeficits(TentativeColonyPlan tcp) {
        final TentativeProduction tentativeProduction = tcp.determineTentativeProductionAndDeficits();
        for (AbstractGoods deficitGoods : tentativeProduction.getDeficits()) {
            if (deficitGoods.getAmount() == 0) {
                continue;
            }
            
            final Tile bestTileToAvoidDeficit = tcp.findBestUnassignedTileForProducing(deficitGoods.getType());
            if (bestTileToAvoidDeficit == null) {
                continue;
            }
            tcp.addOrReplace(TentativeColonyTilePlan.expertProduction(bestTileToAvoidDeficit, deficitGoods.getType()));
            
            if (!produceFoodForConsumption(tcp)) {
                return false;
            }
        }
        return true;
    }
    
    public static boolean produceMoreOfExistingRefinedGoodsAlongWithRequiredRawMaterials(TentativeColonyPlan tcp) {
        final Map<BuildingType, List<WorkerPlan>> buildingWorkerPlansByType = tcp.getBuildingWorkerPlansByType();
        for (Entry<BuildingType, List<WorkerPlan>> building : buildingWorkerPlansByType.entrySet()) {
            if (building.getKey().getWorkPlaces() <= building.getValue().size()) {
                continue;
            }
            final List<ProductionType> productionTypes = building.getKey().getAvailableProductionTypes(false);
            if (productionTypes.isEmpty()) {
                continue;
            }
            final ProductionType pt = productionTypes.get(0);
            if (!pt.getInputs().allMatch(ag -> ag.getType().isRawMaterial() && !ag.getType().isRawBuildingMaterial() )) {
                continue;
            }
            for (AbstractGoods rawMaterial : pt.getInputList()) {
                final Tile bestTile = tcp.findBestUnassignedTileForProducing(rawMaterial.getType());
                if (bestTile == null) {
                    continue;
                }
                tcp.addOrReplace(TentativeColonyTilePlan.expertProduction(bestTile, rawMaterial.getType()));
                
                if (!produceRefinedGoods(tcp)) {
                    return false;
                }
            }
        }
        return true;
    }
    
    public static boolean produceFoodOnRemainingTiles(TentativeColonyPlan tcp) {
        final int consumedByOneWorker = tcp.foodConsumedByOneWorker();
        for (Tile workTile : tcp.unassignedWorkTiles()) {
            final AbstractGoods ag = workTile.getMaximumPotentialFoodProductionWithExpert();
            if (ag.getAmount() <= consumedByOneWorker) {
                continue;
            }
            tcp.addOrReplace(TentativeColonyTilePlan.expertProduction(workTile, ag.getType()));
        }
        return true;
    }
    
    public static boolean produceRefinedGoods(TentativeColonyPlan tcp) {
        if (!tcp.canConstructBuildings()) {
            return true;
        }
        final List<AbstractGoods> tentativeProduction = tcp.determineTentativeProduction();
        final List<AbstractGoods> existingRawGoods = tentativeProduction.stream()
                .filter(ag -> ag.getType().isRawMaterial()
                        //&& ag.getType().isNewWorldGoodsType()
                        && ag.getAmount() > 0
                        && !ag.getType().isRawBuildingMaterial()
                        && !ag.getType().isBuildingMaterial())
                .sorted((a, b) -> Integer.compare(b.getAmount(), a.getAmount()))
                .collect(Collectors.toList());
        
        for (AbstractGoods inputGoods : existingRawGoods) {
            if (!produceRefinedGoodsOf(tcp, inputGoods.getType())) {
                return false;
            }
        }
        
        return true;
    }
    
    private static boolean produceRefinedGoodsOf(TentativeColonyPlan tcp, GoodsType inputGoods) {
        List<AbstractGoods> tentativeProduction = tcp.determineTentativeProduction();

        final Specification s = tcp.getSpecification();
        final List<BuildingType> topLevelBuildings = s.getBuildingTypeList().stream()
                .filter(bt -> bt.getUpgradesTo() == null)
                .collect(Collectors.toList());
        
        for (BuildingType buildingType : topLevelBuildings) {
            final List<ProductionType> productionTypes = buildingType.getAvailableProductionTypes(false)
                    .stream()
                    .filter(pt -> pt.getInputs().anyMatch(ag -> ag.getType() == inputGoods))
                    .sorted((a, b) -> Integer.compare(b.getOutputs().collect(Collectors.summingInt(AbstractGoods::getAmount)),
                            a.getOutputs().collect(Collectors.summingInt(AbstractGoods::getAmount))))
                    .collect(Collectors.toList());
            
            if (productionTypes.isEmpty()) {
                continue;
            }
            
            final ProductionType productionType = productionTypes.get(0);
            final GoodsType producedGoodsType = productionType.getBestOutputType();
            final UnitType unitType = s.getExpertForProducing(producedGoodsType);
            
            final int maximumWorkPlaces = buildingType.getWorkPlaces();

            final BuildingProductionCalculator bpc = new BuildingProductionCalculator(null, new FeatureContainer(), 2);
            final ProductionInfo pi = bpc.getAdjustedProductionInfo(buildingType, new Turn(0), List.of(new WorkerAssignment(unitType, productionType)), List.of(), List.of(), 10_000);
            
            final int inputUsedPerWorker = pi.getMaximumConsumption().stream()
                    .filter(ag -> ag.getType() == inputGoods)
                    .map(ag -> ag.getAmount())
                    .findFirst()
                    .orElse(0);

            final int maximumAvailableInput = tentativeProduction.stream()
                    .filter(g -> g.getType().equals(inputGoods))
                    .findFirst()
                    .orElseThrow()
                    .getAmount();
            
            int workPlacesUsedBefore = tcp.getBuildingWorkerPlansByType().getOrDefault(buildingType, List.of()).size();
            int workPlacesUsedNow = 0;
            while (workPlacesUsedNow + workPlacesUsedBefore < maximumWorkPlaces && inputUsedPerWorker * (workPlacesUsedNow + 1) < maximumAvailableInput) {
                tcp.addBuildable(buildingType);
                final WorkerPlan workerPlan = new WorkerPlan(unitType, new PlannedWorkLocation(buildingType), List.of(producedGoodsType));
                tcp.addBuildingWorkerPlan(workerPlan);
                workPlacesUsedNow++;
                if (!produceFoodForConsumption(tcp)) {
                    return false;
                }
            }
        }
        return true;
    }
    
    public static boolean produceImmigrationGoods(TentativeColonyPlan tcp) {
        if (!tcp.canConstructBuildings()) {
            return true;
        }
        
        final Specification s = tcp.getSpecification();
        for (BuildingType bt : immigrationProductionBuildings(s)) {
            final List<WorkerPlan> currentWorkers = tcp.getTentativeBuildingPlans(bt);
            if (currentWorkers.size() >= bt.getWorkPlaces()) {
                continue;
            }
            
            tcp.addBuildable(bt);
            
            final GoodsType producedGoodsType = bt.getProducedGoodsType();
            final UnitType unitType = s.getExpertForProducing(producedGoodsType);
            
            for (int i=0; i<bt.getWorkPlaces() - currentWorkers.size(); i++) {
                final WorkerPlan workerPlan = new WorkerPlan(unitType, new PlannedWorkLocation(bt), List.of(producedGoodsType));
                tcp.addBuildingWorkerPlan(workerPlan);
            
                if (!produceFoodForConsumption(tcp)) {
                    return false;
                }
            }
        }
        return true;
    }
    
    public static boolean produceEducation(TentativeColonyPlan tcp) {
        if (!tcp.canConstructBuildings()) {
            return true;
        }
        
        int foodSurplus = tcp.determineFoodSurplus();
        if (foodSurplus < tcp.foodConsumedByOneWorker() * 6) {
            for (Tile foodTile : tcp.prioritizeUnassignedTilesForFoodProduction()) {
                if (foodTile == null) {
                    break;
                }
                final AbstractGoods foodGoods = foodTile.getMaximumPotentialFoodProductionWithExpert();
                if (foodGoods == null || foodGoods.getAmount() < 5) {
                    break;
                }
                tcp.addOrReplace(TentativeColonyTilePlan.expertProduction(foodTile, foodGoods.getType()));
                foodSurplus = tcp.determineFoodSurplus();
                if (foodSurplus >= tcp.foodConsumedByOneWorker() * 6) {
                    break;
                }
            }
        }
        final Specification s = tcp.getSpecification();
        
        final int teachers;
        final int convertExistingUnitsToStudents;
        if (foodSurplus >= tcp.foodConsumedByOneWorker() * 6) {
            teachers = 3;
            convertExistingUnitsToStudents = 0;
        } else if (foodSurplus >= tcp.foodConsumedByOneWorker() * 5) {
            teachers = 3;
            convertExistingUnitsToStudents = 1;
        } else if (foodSurplus >= tcp.foodConsumedByOneWorker() * 4) {
            teachers = 3;
            convertExistingUnitsToStudents = 2;
        } else if (foodSurplus >= tcp.foodConsumedByOneWorker() * 3) {
            teachers = 2;
            convertExistingUnitsToStudents = 1;
        } else if (foodSurplus >= tcp.foodConsumedByOneWorker() * 2) {
            teachers = 2;
            convertExistingUnitsToStudents = 2;
        } else {
            teachers = 1;
            convertExistingUnitsToStudents = 1;
        }
        final int newStudents = teachers - convertExistingUnitsToStudents;
        
        final List<BuildingType> teachingBuildings = s.getTypesWithAbility(BuildingType.class, Ability.TEACH);
        
        final BuildingType educationBuildingType = teachingBuildings.stream()
                .filter(bt -> (bt.getWorkPlaces() == teachers))
                .findAny()
                .orElse(null);
        
        if (educationBuildingType == null) {
            return true;
        }
        
        // TODO: Use a generalized type for the teacher.
        final UnitType defaultUnitType = s.getUnitTypeList().stream()
                .filter(ut -> ut.getSkill() <= educationBuildingType.getMaximumSkill() && educationBuildingType.canAdd(ut))
                .filter(ut -> ut.getSkillTaught() != null)
                .filter(ut -> ut.getPrice() > 0)
                .sorted(Comparator.<UnitType>comparingInt(ut -> {
                        final UnitType studentType = ut.getSkillTaught();
                        if (studentType == null) {
                            return 0;
                        }
                        return studentType.getSkill();
                    })
                    .thenComparingInt(ut -> {
                        final UnitType studentType = ut.getSkillTaught();
                        if (studentType == null) {
                            return 0;
                        }
                        return studentType.getPrice();
                    })
                    .reversed()
                )
                .findFirst()
                .orElse(null);
        
        if (defaultUnitType == null) {
            return true;
        }
        
        tcp.addBuildable(educationBuildingType);
        
        for (int i=0; i<teachers; i++) {
            tcp.addBuildingWorkerPlan(new WorkerPlan(defaultUnitType, new PlannedWorkLocation(educationBuildingType), List.of()));
        }
        
        int convertExistingUnitsToStudentsLeft = convertExistingUnitsToStudents;
        int newStudentsLeft = newStudents;
        int removeUnits = 0;

        // First check if an expert can be replaced by two students.
        if (convertExistingUnitsToStudentsLeft + newStudentsLeft >= 2 || newStudentsLeft >= 1) {
            for (Entry<BuildingType, List<WorkerPlan>> buildings : tcp.getBuildingWorkerPlansByType().entrySet()) {
                final BuildingType bt = buildings.getKey();
                if (bt == educationBuildingType) {
                    continue;
                }
                
                final List<WorkerPlan> workers = buildings.getValue();
                if (workers.isEmpty() || bt.getWorkPlaces() <= workers.size()) {
                    continue;
                }
                
                final WorkerPlan workerPlan = workers.stream()
                        .filter(wp -> wp.getUnitType() != s.getDefaultUnitType())
                        .findAny()
                        .orElse(null);
                
                if (workerPlan == null) {
                    continue;
                }
                
                final WorkerPlan newWorkerPlan1 = new WorkerPlan(s.getDefaultUnitType(), workerPlan.getPlannedWorkLocation(), workerPlan.getProductionTypes());
                final WorkerPlan newWorkerPlan2 = new WorkerPlan(s.getDefaultUnitType(), workerPlan.getPlannedWorkLocation(), workerPlan.getProductionTypes());
                
                tcp.removeBuildingWorkerPlan(workerPlan);
                tcp.addBuildingWorkerPlan(newWorkerPlan1);
                tcp.addBuildingWorkerPlan(newWorkerPlan2);
                
                if (convertExistingUnitsToStudentsLeft >= 1 && newStudentsLeft >= 1) {
                    convertExistingUnitsToStudentsLeft--;
                    newStudentsLeft--;
                } else if (convertExistingUnitsToStudentsLeft >= 2) {
                    convertExistingUnitsToStudentsLeft -= 2;
                    removeUnits++;
                } else if (newStudentsLeft >= 1) {
                    newStudentsLeft -= 2;
                    if (newStudentsLeft < 0) {
                        newStudentsLeft = 0;
                    }
                } else {
                    throw new IllegalArgumentException("All cases should be covered above: " + newStudentsLeft + ", " + convertExistingUnitsToStudentsLeft);
                }
                
                if (!(convertExistingUnitsToStudentsLeft + newStudentsLeft >= 2 || newStudentsLeft >= 1)) {
                    break;
                }
            }
        }

        final Set<BuildingType> libertyBellsProductionBuildings = libertyBellsProductionBuildings(s);
        
        final int maxTries = convertExistingUnitsToStudentsLeft + removeUnits;
        for (int i=0; i<maxTries && convertExistingUnitsToStudentsLeft + removeUnits > 0; i++) {
            // TODO: First factories without max consumption, then cross producers, then statesmen only as the last step.
            
            // Replace statesmen:
            for (Entry<BuildingType, List<WorkerPlan>> buildings : tcp.getBuildingWorkerPlansByType().entrySet()) {
                final BuildingType bt = buildings.getKey();
                if (bt == educationBuildingType) {
                    continue;
                }
                if (!libertyBellsProductionBuildings.contains(bt)) {
                    continue;
                }
                
                final List<WorkerPlan> workers = buildings.getValue();
                if (workers.isEmpty()) {
                    continue;
                }
                
                final WorkerPlan workerPlan = workers.stream()
                        .filter(wp -> wp.getUnitType() != s.getDefaultUnitType())
                        .findAny()
                        .orElse(null);
                
                if (workerPlan == null) {
                    continue;
                }
                
                final WorkerPlan newWorkerPlan1 = new WorkerPlan(s.getDefaultUnitType(), workerPlan.getPlannedWorkLocation(), workerPlan.getProductionTypes());
                tcp.removeBuildingWorkerPlan(workerPlan);
                if (removeUnits > 0) {
                    removeUnits--;
                } else {
                    tcp.addBuildingWorkerPlan(newWorkerPlan1);
                    convertExistingUnitsToStudentsLeft--;
                }
                
                if (convertExistingUnitsToStudentsLeft + removeUnits <= 0) {
                    break;
                }
            }
        }
        /*
         * TODO: Handle if we could not remove or replace sufficient units in the step above.
         *       However, it should not happen since there are always three statesmen added
         *       before this step with the current setup. Ideally, we do want this code to
         *       handle if that is no longer the case, however.
         */
        
        
        if (newStudentsLeft > 0) {
            for (Entry<BuildingType, List<WorkerPlan>> buildings : tcp.getBuildingWorkerPlansByType().entrySet()) {
                final BuildingType bt = buildings.getKey();
                if (bt == educationBuildingType) {
                    continue;
                }
                if (!libertyBellsProductionBuildings.contains(bt)) {
                    continue;
                }
                
                final List<WorkerPlan> workers = buildings.getValue();
                if (workers.size() >= bt.getWorkPlaces()) {
                    continue;
                }
                
                final GoodsType producedGoodsType = bt.getProducedGoodsType();
                final WorkerPlan newWorkerPlan = new WorkerPlan(s.getDefaultUnitType(), new PlannedWorkLocation(bt), List.of(producedGoodsType));
                tcp.addBuildingWorkerPlan(newWorkerPlan);
                newStudentsLeft--;
                
                if (newStudentsLeft <= 0) {
                    break;
                }
            }
        }
        
        // TODO: Prioritize increasing production, an especially crosses, before choosing whatever location:
        if (newStudentsLeft > 0) {
            for (Entry<BuildingType, List<WorkerPlan>> buildings : tcp.getBuildingWorkerPlansByType().entrySet()) {
                final BuildingType bt = buildings.getKey();
                if (bt == educationBuildingType) {
                    continue;
                }
                
                final List<WorkerPlan> workers = buildings.getValue();
                if (workers.size() >= bt.getWorkPlaces()) {
                    continue;
                }
                
                final GoodsType producedGoodsType = bt.getProducedGoodsType();
                final WorkerPlan newWorkerPlan = new WorkerPlan(s.getDefaultUnitType(), new PlannedWorkLocation(bt), List.of(producedGoodsType));
                tcp.addBuildingWorkerPlan(newWorkerPlan);
                newStudentsLeft--;
                
                if (newStudentsLeft <= 0) {
                    break;
                }
            }
        }
        
        return produceFoodForConsumption(tcp);
    }
    
    public static boolean produceStorableBuildingMaterials(TentativeColonyPlan tcp) {
        if (!tcp.canConstructBuildings()) {
            return true;
        }
        return internalProduceGenericStorableBuildingMaterials(tcp, false, 0.5D);
    }
    
    public static boolean produceRawMaterialsJustForExportIfNoIndustry(TentativeColonyPlan tcp) {
        if (tcp.canConstructBuildings()) {
            return true;
        }
        
        final Specification s = tcp.getSpecification();
        final List<AbstractGoods> tentativeProduction = tcp.determineTentativeProduction();
        final List<GoodsType> rawGoodsTypes = s.getGoodsTypeList()
                .stream()
                .filter(gt -> gt.isRawMaterial() && !gt.isRawBuildingMaterial())
                .collect(Collectors.toList());
        
        final List<AbstractGoods> leastProduced = rawGoodsTypes.stream()
            .map(gt -> {
                final int amount = tentativeProduction.stream()
                        .filter(ag -> ag.getType() == gt)
                        .map(AbstractGoods::getAmount)
                        .findAny()
                        .orElse(0);
                return new AbstractGoods(gt, amount);
            })
            .sorted(Comparator.comparingInt(AbstractGoods::getAmount))
            .collect(Collectors.toList());
        
        for (AbstractGoods ag : leastProduced) {
            final Tile tile = tcp.findBestUnassignedTileForProducing(ag.getType());
            if (tile == null) {
                continue;
            }
            if (tile.getMaximumPotential(ag.getType(), s.getExpertForProducing(ag.getType())) < 5) {
                continue;
            }
            
            tcp.addOrReplace(TentativeColonyTilePlan.expertProduction(tile, ag.getType()));
            
            return produceFoodForConsumption(tcp);
        }
        
        return true;
    }
    
    public static boolean produceNewRawMaterialAndRefine(TentativeColonyPlan tcp) {
        if (!tcp.canConstructBuildings()) {
            return true;
        }
        
        final Specification s = tcp.getSpecification();
        
        int foodSurplus = tcp.determineFoodSurplus();
        if (foodSurplus < tcp.foodConsumedByOneWorker() * 3) {
            for (Tile foodTile : tcp.prioritizeUnassignedTilesForFoodProduction()) {
                tcp.addOrReplace(TentativeColonyTilePlan.expertProduction(foodTile, foodTile.getBestFoodProduction().getType()));
                foodSurplus = tcp.determineFoodSurplus();
                if (foodSurplus >= tcp.foodConsumedByOneWorker() * 3) {
                    break;
                }
            }
        }
        if (foodSurplus < tcp.foodConsumedByOneWorker() * 3) {
            return true;
        }
        
        final List<Tile> unassignedWorkTiles = tcp.unassignedWorkTiles();
        final List<GoodsType> rawGoodsTypes = s.getGoodsTypeList()
                .stream()
                .filter(gt -> gt.isRawMaterial() && !gt.isRawBuildingMaterial())
                .collect(Collectors.toList());
        
        final Map<GoodsType, Integer> maxProduction = new HashMap<>();
        for (GoodsType gt : rawGoodsTypes) {
            int amount = 0;
            for (Tile tile : unassignedWorkTiles) {
                final int newAmount = tile.getMaximumPotential(gt, s.getExpertForProducing(gt));
                if (newAmount > 2) {
                    amount += newAmount;
                }
            }
            maxProduction.put(gt, amount);
        }
        
        final Set<GoodsType> alreadyProducing = tcp.determineRawMaterialTypesProduced();
        
        final Entry<GoodsType, Integer> bestRawMaterial = maxProduction.entrySet().stream()
                .filter(e -> !alreadyProducing.contains(e.getKey()))
                .max(Comparator.comparingInt(Entry::getValue))
                .orElse(null);
        if (bestRawMaterial == null) {
            return true;
        }
        if (bestRawMaterial.getValue() < 10) {
            return true;
        }
        
        final Tile raw1 = tcp.findBestUnassignedTileForProducing(bestRawMaterial.getKey());
        if (raw1 == null) {
            return true;
        }
        tcp.addOrReplace(TentativeColonyTilePlan.expertProduction(raw1, bestRawMaterial.getKey()));
        
        final Tile raw2 = tcp.findBestUnassignedTileForProducing(bestRawMaterial.getKey());
        if (raw2 == null) {
            return produceRefinedGoods(tcp);
        }
        tcp.addOrReplace(TentativeColonyTilePlan.expertProduction(raw2, bestRawMaterial.getKey()));
        
        return produceRefinedGoods(tcp);
    }
    
    public static boolean produceGoodsTypeForWeapons(TentativeColonyPlan tcp) {
        if (!tcp.canConstructBuildings()) {
            return true;
        }
        return internalProduceGenericStorableBuildingMaterials(tcp, true, 1.0D);
    }
    
    private static boolean internalProduceGenericStorableBuildingMaterials(TentativeColonyPlan tcp, boolean militaryEquipment, double minimumProductionRatio) {
        // Expert needed? Better building needed? An extra expert needed?
        
        final Specification s = tcp.getSpecification();
        final List<BuildingType> storableBuildablesProductionBuildings = storableBuildablesProductionBuildings(s); // produces tools / muskets
        for (BuildingType buildingType : storableBuildablesProductionBuildings) {
            if (buildingType.getWorkPlaces() <= 0) {
                continue;
            }
            
            final List<ProductionType> productionTypes = buildingType.getAvailableProductionTypes(false).stream()
                    .filter(productionType -> productionType.getOutputs().anyMatch(g -> g.getAmount() > 0))
                    .collect(Collectors.toList());
            
            if (productionTypes.isEmpty()) {
                continue;
            }
            final ProductionType productionType = productionTypes.get(0);
            final boolean producesMilitaryEquipment = productionType.getOutputs().anyMatch(ag -> ag.getType().getMilitary());
            if (!militaryEquipment && producesMilitaryEquipment
                    || militaryEquipment && !producesMilitaryEquipment) {
                continue;
            }
            
            for (int i=0; i<buildingType.getWorkPlaces(); i++) {
                /*
                 * TODO: Support autoproduction for buildings. We should call the calculator
                 *       with all workers at the same time, rather than one at the time.
                 */
                final List<AbstractGoods> tentativeProduction = tcp.determineTentativeProduction();

                final GoodsType producedGoodsType = buildingType.getProducedGoodsType();
                final UnitType unitType = s.getExpertForProducing(producedGoodsType);
                final WorkerPlan workerPlan = new WorkerPlan(unitType, new PlannedWorkLocation(buildingType), List.of(producedGoodsType));
                
                final BuildingProductionCalculator bpc = new BuildingProductionCalculator(null, new FeatureContainer(), 2);
                final ProductionInfo pi = bpc.getAdjustedProductionInfo(buildingType,
                        new Turn(0),
                        List.of(new WorkerAssignment(unitType, productionType)),
                        tentativeProduction,
                        tentativeProduction,
                        100000);
                
                // Demand at least half of the input for every new expert:
                if (pi.getProduction().stream().noneMatch(ag -> ag.getAmount() > 0
                        && pi.getMaximumProduction().stream().noneMatch(maxAg -> {
                            return ag.getType() == maxAg.getType() && ag.getAmount() < maxAg.getAmount() * minimumProductionRatio;
                        }))) {
                    break;
                }
                
                tcp.addBuildable(buildingType);
                tcp.addBuildingWorkerPlan(workerPlan);
               
                if (!produceFoodForConsumption(tcp)) {
                    return false;
                }
            }
        }


        return true;
    }

    public static boolean produceRawMaterialForStorableBuildingMaterials(TentativeColonyPlan tcp) {
        final Specification s = tcp.getSpecification();
        for (GoodsType goodsType : s.getRawMaterialsForStorableBuildingMaterials()) {
            while (true) {
                final Tile tile = tcp.findBestUnassignedTileForProducing(goodsType);
                if (tile == null) {
                    break;
                }
                final int goodsTypeAmount = tile.getMaximumPotential(goodsType, s.getExpertForProducing(goodsType));
                if (goodsTypeAmount <= 2) {
                    break;
                }
                
                final AbstractGoods foodProduction = tile.getMaximumPotentialFoodProductionWithExpert();
                final int foodAmount = (foodProduction != null) ? foodProduction.getAmount() : 0;
                if (foodAmount > goodsTypeAmount) {
                    break;
                }
                tcp.addOrReplace(TentativeColonyTilePlan.expertProduction(tile, goodsType));
                if (!produceFoodForConsumption(tcp)) {
                    return false;
                }
            }
        }
        return true;
    }
    
    public static boolean produceLibertyBells(TentativeColonyPlan tcp) {
        final Specification s = tcp.getSpecification();
        final Set<BuildingType> libertyBellsProductionBuildings = libertyBellsProductionBuildings(s);
        boolean canAddMoreWorkers = true; 
        for (BuildingType buildingType : libertyBellsProductionBuildings) {
            tcp.addBuildable(buildingType);
            if (buildingType.getWorkPlaces() <= 0 || !canAddMoreWorkers) {
                continue;
            }
            final GoodsType producedGoodsType = buildingType.getProducedGoodsType();
            final UnitType unitType = s.getExpertForProducing(producedGoodsType);
            final WorkerPlan workerPlan = new WorkerPlan(unitType, new PlannedWorkLocation(buildingType), List.of(producedGoodsType));
            tcp.addBuildingWorkerPlan(workerPlan);
            
            canAddMoreWorkers = produceFoodForConsumption(tcp);
        }
        
        for (GoodsType libertyGoodsType : s.getLibertyGoodsTypeList()) {
            final List<BuildingType> buildingsIncreasingLibertyProduction = s.getBuildingTypeList().stream()
                .filter(buildingType -> buildingType.getUpgradesTo() == null && buildingType.getModifiers(libertyGoodsType.getId()).count() > 0L)
                .collect(Collectors.toList());
            
            for (BuildingType buildingType : buildingsIncreasingLibertyProduction) {
                tcp.addBuildable(buildingType);
            }
        }
        
        return canAddMoreWorkers;
    }
    
    private static List<BuildingType> storableBuildablesProductionBuildings(Specification s) {
        return s.getBuildingTypeList().stream().filter(buildingType -> {
            final GoodsType producedGoodsType = buildingType.getProducedGoodsType();
            return producedGoodsType != null
                    && producedGoodsType.isBuildingMaterial()
                    && producedGoodsType.isStorable()
                    && buildingType.getUpgradesTo() == null; // Make the ideal plan based on the most upgraded building.
        }).collect(Collectors.toList());
    }

    private static Set<BuildingType> libertyBellsProductionBuildings(Specification s) {
        return s.getBuildingTypeList().stream().filter(buildingType -> {
            final GoodsType producedGoodsType = buildingType.getProducedGoodsType();
            return producedGoodsType != null
                    && producedGoodsType.isLibertyType()
                    && buildingType.getUpgradesTo() == null; // Make the ideal plan based on the most upgraded building.
        }).collect(Collectors.toSet());
    }
    
    private static Set<BuildingType> immigrationProductionBuildings(Specification s) {
        return s.getBuildingTypeList().stream().filter(buildingType -> {
            final GoodsType producedGoodsType = buildingType.getProducedGoodsType();
            return producedGoodsType != null
                    && producedGoodsType.isImmigrationType()
                    && buildingType.getUpgradesTo() == null; // Make the ideal plan based on the most upgraded building.
        }).collect(Collectors.toSet());
    }
    
    private static Set<BuildingType> shipEnablingBuildings(Specification s) {
        return s.getBuildingTypeList().stream().filter(buildingType -> {
            final boolean enablesBuildingShip = s.getBuildableUnitTypes().stream()
                    .filter(ut -> ut.isNaval()).anyMatch(ut -> buildingType.hasAbility(Ability.BUILD, ut));
            return enablesBuildingShip && buildingType.getUpgradesTo() == null; // Make the ideal plan based on the most upgraded building.
        }).collect(Collectors.toSet());
    }
    
    private static BuildingType lowestLevelMilitaryLandUnitEnablingBuildings(Specification s) {
        return s.getBuildingTypeList().stream().filter(buildingType -> {
            final boolean enablesBuildingArtillery = s.getBuildableUnitTypes().stream()
                    .filter(ut -> !ut.isNaval() && ut.hasAbility(Ability.BOMBARD)).anyMatch(ut -> buildingType.hasAbility(Ability.BUILD, ut));
            return enablesBuildingArtillery;
        }).sorted(Comparator.comparingInt(BuildingType::getLevel)).findAny().orElse(null);
    }

    public static boolean produceFoodForConsumption(TentativeColonyPlan tcp) {
        // TODO: Support generic consumables instead of just food.
        
        final int consumedByOneWorker = tcp.foodConsumedByOneWorker();
        
        int foodSurplus = tcp.determineFoodSurplus();
        if (foodSurplus >= consumedByOneWorker) {
            return true;
        }
        
        int index = 0;
        final List<Tile> foodProductionCandidates = tcp.prioritizeUnassignedTilesForFoodProduction();
        while (index < foodProductionCandidates.size() && foodSurplus < consumedByOneWorker) {
            final Tile tile = foodProductionCandidates.get(index);
            final AbstractGoods bestFoodProduction = tile.getMaximumPotentialFoodProductionWithExpert();

            final GoodsType foodType = bestFoodProduction.getType();
            final TentativeColonyTilePlan tentativeColonyTilePlan = TentativeColonyTilePlan.expertProduction(tile, foodType);
            tcp.addOrReplace(tentativeColonyTilePlan);
            
            foodSurplus = tcp.determineFoodSurplus();
            index++;
        }
        
        if (foodSurplus < 0) {
            return forceReallocateColonistsForFoodProduction(tcp);
        }
        
        return foodSurplus >= consumedByOneWorker;
    }
    
    private static boolean forceReallocateColonistsForFoodProduction(TentativeColonyPlan tcp) {
        /*
         * This should almost never happen. Yet the code is here to handle extreme cases like:
         * 1. No productive tiles (acrtic/desert).
         * 2. Almost all tiles are bonus resources not yielding any food by default.
         */
        
        final List<Tile> candidateTilesForProducingFood = tcp.getTentativeColonyTilePlans().stream()
                .filter(tctp -> tctp.getProduction().stream().noneMatch(g -> g.getType().isFoodType()))
                .map(TentativeColonyTilePlan::getTile)
                .collect(Collectors.toList());
        final List<Tile> sortedTilesForProducingFood = tcp.prioritizeTilesForFoodProduction(candidateTilesForProducingFood);
        int index = 0;
        int foodSurplus = tcp.determineFoodSurplus();
        while (foodSurplus < 0 && index < sortedTilesForProducingFood.size()) {
            final Tile tile = sortedTilesForProducingFood.get(index);
            final GoodsType goodsType = tile.getMaximumPotentialFoodProductionWithExpert().getType();
            final TentativeColonyTilePlan tentativeColonyTilePlan = TentativeColonyTilePlan.expertProduction(tile, goodsType);
            tcp.addOrReplace(tentativeColonyTilePlan);
            
            index++;
            foodSurplus = tcp.determineFoodSurplus();
        }
        return foodSurplus >= tcp.foodConsumedByOneWorker();
    }
}
