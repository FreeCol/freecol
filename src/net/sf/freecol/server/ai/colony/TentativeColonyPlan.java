package net.sf.freecol.server.ai.colony;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.FeatureContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.ProductionInfo;
import net.sf.freecol.common.model.ProductionType;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.production.BuildingProductionCalculator;
import net.sf.freecol.common.model.production.TileProductionCalculator;
import net.sf.freecol.common.model.production.TileProductionCalculator.MaximumPotentialProduction;
import net.sf.freecol.common.model.production.WorkerAssignment;

/**
 * A colony plan that can easily be mutated.
 * 
 * @see #toColonyPlan
 */
public class TentativeColonyPlan {
    
    private final Tile centerTile;
    private final Map<Tile, TentativeColonyTilePlan> tentativeColonyTilePlans = new IdentityHashMap<>();
    private final List<WorkerPlan> tentativeBuildingPlans = new ArrayList<>();
    private final Set<BuildableType> buildables = new HashSet<>();
    private final List<TileImprovementType> centerTileImprovements;

    
    /**
     * Creates a new colony plan that can easily be mutated.
     * 
     * @param centerTile The <code>Tile</code> where the colony is/will be located. 
     * @see #toColonyPlan
     */
    public TentativeColonyPlan(Tile centerTile) {
        this.centerTile = centerTile;
        
        for (Tile tile : getWorkTiles(centerTile)) {
            tentativeColonyTilePlans.put(tile, null);
        }
        
        final MaximumPotentialProduction maximumPotentialProduction = getMaximumUnattendedProduction(centerTile);
        this.centerTileImprovements = maximumPotentialProduction.getTileImprovementType(); 
    }
    
    
    /**
     * Gets the <code>Tile</code> where the colony is/will be located.
     */
    public Tile getCenterTile() {
        return centerTile;
    }
    
    /**
     * The tile improvements for the center colony tile.
     */
    public List<TileImprovementType> getCenterTileImprovements() {
        return centerTileImprovements;
    }
    
    public Set<BuildableType> getBuildables() {
        return buildables;
    }
    
    public void addBuildable(BuildableType buildableType) {
        buildables.add(buildableType);
    }
    
    public void removeColonyTilePlan(Tile tile) {
        Objects.requireNonNull(tile, "tile == null");
        tentativeColonyTilePlans.put(tile, null);
    }
    
    public void addOrReplace(TentativeColonyTilePlan tentativeColonyTilePlan) {
        tentativeColonyTilePlans.put(tentativeColonyTilePlan.getTile(), tentativeColonyTilePlan);
    }
    
    public TentativeColonyTilePlan get(Tile tile) {
        return tentativeColonyTilePlans.get(tile);
    }
    
    public void addBuildingWorkerPlan(WorkerPlan wp) {
        Objects.requireNonNull(wp.getPlannedWorkLocation().getBuildingType(), "buildingType");
        tentativeBuildingPlans.add(wp);
    }
    
    public void removeBuildingWorkerPlan(WorkerPlan wp) {
        Objects.requireNonNull(wp.getPlannedWorkLocation().getBuildingType(), "buildingType");
        tentativeBuildingPlans.remove(wp);
    }
    
    public List<WorkerPlan> getTentativeBuildingPlans(BuildingType bt) {
        return tentativeBuildingPlans.stream()
                .filter(wp -> wp.getPlannedWorkLocation().getBuildingType() == bt)
                .collect(Collectors.toList());
    }
    
    public List<WorkerPlan> getTentativeBuildingPlans() {
        return tentativeBuildingPlans;
    }
    
    public List<TentativeColonyTilePlan> getTentativeColonyTilePlans() {
        return tentativeColonyTilePlans.values().stream()
                .filter(tctp -> tctp != null)
                .collect(Collectors.toList());
    }
    
    public List<TentativeColonyTilePlan> findTentativeColonyTilePlanProducing(GoodsType goodsType) {
        return tentativeColonyTilePlans.values().stream()
                .filter(tctp -> tctp != null
                    && tctp.getProduction().stream().anyMatch(g -> g.getType().equals(goodsType))
                )
                .collect(Collectors.toList());
    }
    
    public ColonyPlan toColonyPlan() {
        return toColonyPlan(tentativeColonyTilePlans, tentativeBuildingPlans);
    }
    
    public int foodConsumedByOneWorker() {
        final Specification s = getSpecification(); 
        return foodConsumedByOneWorker(s);
    }
    
    public static int foodConsumedByOneWorker(Specification s) {
        return s.getDefaultUnitType().getConsumptionOf(s.getPrimaryFoodType());
    }
    
    public List<Tile> prioritizeUnassignedTilesForFoodProduction() {
        return prioritizeTilesForFoodProduction(unassignedWorkTiles());
    }
    
    public List<Tile> prioritizeTilesForFoodProduction(Collection<Tile> tiles) {
        final int foodConsumedByOneWorker = foodConsumedByOneWorker();
        return tiles.stream()
                .filter(tile -> {
                    final AbstractGoods bestFoodProduction = tile.getMaximumPotentialFoodProductionWithExpert();
                    return bestFoodProduction != null && bestFoodProduction.getAmount() >= 2 * foodConsumedByOneWorker;
                })
                .sorted((a, b) -> Double.compare(scoreFoodProductionVsOtherGoods(b), scoreFoodProductionVsOtherGoods(a)))
                .collect(Collectors.toList());
    }
    
    private double scoreFoodProductionVsOtherGoods(Tile tile) {
        final AbstractGoods bestFoodProduction = tile.getMaximumPotentialFoodProductionWithExpert();
        if (bestFoodProduction == null || bestFoodProduction.getAmount() <= 0) {
            return -1D;
        }
        final Optional<AbstractGoods> bestAlternativeProduction = tile.getSortedPotential().stream()
            .filter(g -> {
                return !g.getType().isFoodType() && !g.getType().isRawMaterialForUnstorableBuildingMaterial();
            })
            .max((a, b) -> Integer.compare(a.getAmount(), b.getAmount()));
        if (bestAlternativeProduction.isEmpty()) {
            return bestFoodProduction.getAmount(); 
        }
        
        return bestFoodProduction.getAmount() / ((double) bestFoodProduction.getAmount() + bestAlternativeProduction.get().getAmount());
    }
    
    public Tile findBestUnassignedTileForProducing(GoodsType goodsType) {
        final List<Tile> candidateTiles = unassignedWorkTiles();
        return findBestTileForProducing(candidateTiles, goodsType);
    }
    
    public Tile findBestTileForProducing(GoodsType goodsType) {
        final Set<Tile> candidateTiles = tentativeColonyTilePlans.keySet();
        return findBestTileForProducing(candidateTiles, goodsType);
    }
    
    public List<Tile> unassignedWorkTiles() {
        return tentativeColonyTilePlans.entrySet()
                .stream()
                .filter(e -> e.getValue() == null)
                .map(Entry::getKey)
                .collect(Collectors.toList());
    }
    
    public Set<Tile> unassignedExcept(Collection<Tile> excludeTiles) {
        return unassignedWorkTiles().stream().filter(t -> !excludeTiles.contains(t)).collect(Collectors.toSet());
    }
    
    public Set<Tile> workTilesExcept(Set<Tile> ignoreTilesWithRequiredResources) {
        return tentativeColonyTilePlans.keySet().stream()
                .filter(t -> !ignoreTilesWithRequiredResources.contains(t))
                .collect(Collectors.toSet());
    }
    
    
    public Tile findBestTileForProducing(Collection<Tile> candidateTiles, GoodsType goodsType) {
        if (candidateTiles.isEmpty()) {
            return null;
        }
        final Specification s = centerTile.getSpecification();
        
        Tile bestTile = null;
        int bestAmount = 0;
        for (Tile t : candidateTiles) {
            final int newAmount = t.getMaximumPotential(goodsType, s.getExpertForProducing(goodsType));
            if (newAmount > bestAmount) {
                bestTile = t;
                bestAmount = newAmount;
            }
        }
        
        return bestTile;
    }

    
    private ColonyPlan toColonyPlan(final Map<Tile, TentativeColonyTilePlan> tentativeColonyTilePlans, final List<WorkerPlan> tentativeBuildingPlans) {
        final List<TileImprovementPlan> tileImprovementPlans = tentativeColonyTilePlans.values()
            .stream()
            .filter(tp -> tp != null)
            .map(tp -> {
                return tp.getTileImprovements().stream().map(ti -> new TileImprovementPlan(ti, tp.getTile())).collect(Collectors.toList());
            })
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        
        centerTileImprovements.stream()
                .map(ti -> new TileImprovementPlan(ti, centerTile))
                .forEach(tip -> tileImprovementPlans.add(tip));
        
        final List<WorkerPlan> workerPlans = new ArrayList<>();
        
        tentativeColonyTilePlans.values()
            .stream()
            .filter(Objects::nonNull)
            .map(TentativeColonyTilePlan::getWorkerPlan)
            .forEach(wp -> {
                workerPlans.add(wp);
            });
        workerPlans.addAll(tentativeBuildingPlans);
        final ColonyPlan colonyPlan = new ColonyPlan(buildables, tileImprovementPlans, workerPlans);
        return colonyPlan;
    }
    
    public Specification getSpecification() {
        return centerTile.getSpecification();
    }
    
    
    public static class TentativeProduction {
        private Map<GoodsType, Integer> production;
        private Map<GoodsType, Integer> deficits;
        
        public TentativeProduction(Map<GoodsType, Integer> production, Map<GoodsType, Integer> deficits) {
            this.production = production;
            this.deficits = deficits;
        }
        
        public List<AbstractGoods> getProduction() {
            return production.entrySet()
                    .stream()
                    .map(e -> new AbstractGoods(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
        }
        
        public List<AbstractGoods> getDeficits() {
            return deficits.entrySet()
                    .stream()
                    .map(e -> new AbstractGoods(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
        }
        
        public int getAmountProduced(GoodsType gt) {
            return production.getOrDefault(gt, 0);
        }
    }

    public List<AbstractGoods> determineTentativeProduction() {
        return determineTentativeProductionAndDeficits().getProduction();
    }
    
    public TentativeProduction determineTentativeProductionAndDeficits() {
        return determineTentativeProductionAndDeficits(getSpecification(),
                centerTile,
                tentativeColonyTilePlans,
                getBuildingWorkerPlansByType(),
                getBuildingTypesToBuild());
    }
    
    public static TentativeProduction determineTentativeProductionAndDeficits(Specification s,
            Tile centerTile,
            Map<Tile, TentativeColonyTilePlan> tentativeColonyTilePlans,
            Map<BuildingType, List<WorkerPlan>> buildingWorkerPlansByType,
            List<BuildingType> buildingTypesToBuild) {
        final Map<GoodsType, Integer> deficits = new HashMap<>();
        final Map<GoodsType, Integer> production = new HashMap<>();
        production.put(s.getPrimaryFoodType(), 0);        

        for (TentativeColonyTilePlan p : tentativeColonyTilePlans.values()) {
            if (p == null) {
                continue;
            }
            for (AbstractGoods g : p.getProduction()) {
                final Integer oldAmount = production.get(g.getType().getStoredAs());
                final int amount = ((oldAmount != null) ? oldAmount : 0) + g.getAmount();
                production.put(g.getType().getStoredAs(), amount);
            }
            production.put(s.getPrimaryFoodType(), production.get(s.getPrimaryFoodType()) - foodConsumedByOneWorker(s));
        }
        
        final Map<BuildingType, List<WorkerPlan>> buildings = buildingWorkerPlansByType;
        final FeatureContainer colonyFeatureContainer = new FeatureContainer();
        for (BuildingType bt : buildingTypesToBuild) {
            colonyFeatureContainer.addFeatures(bt);
        }
        
        
        
        final List<Entry<BuildingType, List<WorkerPlan>>> buildingsList = new ArrayList<>(buildings.entrySet());
        Collections.sort(buildingsList, (a, b) -> {
            return Integer.compare(b.getKey().getPriority(), a.getKey().getPriority());
        });
        
        for (Entry<BuildingType, List<WorkerPlan>> building : buildingsList) {
            if (building.getKey() == null || building.getValue().isEmpty()) {
                continue;
            }
            final BuildingProductionCalculator bpc = new BuildingProductionCalculator(null, colonyFeatureContainer, 2);
            final List<WorkerAssignment> workerAssignments = building.getValue().stream()
                    .map(wp -> {
                        final List<ProductionType> productionTypes = building.getKey().getAvailableProductionTypes(false).stream()
                                .filter(productionType -> productionType.getOutputs().anyMatch(g -> g.getAmount() > 0))
                                .collect(Collectors.toList());
                        production.put(s.getPrimaryFoodType(), production.get(s.getPrimaryFoodType()) - foodConsumedByOneWorker(s));
                        if (productionTypes.isEmpty()) {
                            return null;
                        }
                        return new WorkerAssignment(wp.getUnitType(), productionTypes.get(0));
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            final List<AbstractGoods> inputAndOutput = production.entrySet()
                    .stream()
                    .map(e -> new AbstractGoods(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
            final ProductionInfo pi = bpc.getAdjustedProductionInfo(building.getKey(), new Turn(0), workerAssignments, inputAndOutput, inputAndOutput, 10000);
            for (AbstractGoods ag : pi.getConsumption()) {
                final Integer oldAmount = production.get(ag.getType());
                final int amount = ((oldAmount != null) ? oldAmount : 0) - ag.getAmount();
                production.put(ag.getType(), amount);
                
                final AbstractGoods maxConsumption = pi.getMaximumConsumption()
                        .stream()
                        .filter(maxConsumeAg -> maxConsumeAg.getType() == ag.getType())
                        .findFirst()
                        .orElse(null);
                if (maxConsumption != null) {
                    final int deficitAmount = maxConsumption.getAmount() - ag.getAmount();
                    deficits.put(ag.getType(), deficitAmount);
                }
            }
            for (AbstractGoods ag : pi.getProduction()) {
                final Integer oldAmount = production.get(ag.getType().getStoredAs());
                final int amount = ((oldAmount != null) ? oldAmount : 0) + ag.getAmount();
                production.put(ag.getType().getStoredAs(), amount);
            }
        }
       
        for (AbstractGoods g : getAutoProduction(centerTile)) {
            final Integer oldAmount = production.get(g.getType().getStoredAs());
            final int amount = ((oldAmount != null) ? oldAmount : 0) + g.getAmount();
            production.put(g.getType().getStoredAs(), amount);
        }
        
        return new TentativeProduction(production, deficits);
    }

    private static MaximumPotentialProduction getMaximumUnattendedProduction(Tile centerTile) {
        final AbstractGoods foodType = centerTile.getMaximumPotentialUnattendedFoodProduction();
        final TileProductionCalculator tpc = new TileProductionCalculator(null, 2);
        final MaximumPotentialProduction maximumPotentialProduction = tpc.getMaximumPotentialProduction(foodType.getType(), centerTile, null);
        return maximumPotentialProduction;
    }

    public Map<BuildingType, List<WorkerPlan>> getBuildingWorkerPlansByType() {
        final Map<BuildingType, List<WorkerPlan>> buildings = tentativeBuildingPlans.stream()
                .collect(Collectors.groupingBy(wp -> wp.getPlannedWorkLocation().getBuildingType()));
        return buildings;
    }
    
    public boolean canConstructBuildings() {
        return determineTentativeProduction().stream()
            .filter(ag -> ag.getType().isBuildingMaterial() && !ag.getType().isStorable())
            .anyMatch(ag -> ag.getAmount() > 0);
    }


    private List<BuildingType> getBuildingTypesToBuild() {
        return buildables.stream().filter(b -> (b instanceof BuildingType)).map(b -> (BuildingType) b).collect(Collectors.toList());
    }
    
    public int determineFoodSurplus() {
        return determineTentativeProduction().stream()
            .filter(g -> g.getType().isFoodType())
            .findFirst()
            .get()
            .getAmount();
    }
    
    public int determinePossibleVacantTileFoodSurplusAfterAddingOrReplacing(Collection<TentativeColonyTilePlan> plansToAdd) {
        final Set<Tile> takenTiles = plansToAdd.stream()
                .map(TentativeColonyTilePlan::getTile)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        final Map<Tile, TentativeColonyTilePlan> newColonyTilePlans = new IdentityHashMap<>();
        getTentativeColonyTilePlans().stream()
                .filter(tctp -> !takenTiles.contains(tctp.getTile()))
                .forEach(tctp -> newColonyTilePlans.put(tctp.getTile(), tctp));
        
        for (TentativeColonyTilePlan planToAdd : plansToAdd) {
            newColonyTilePlans.put(planToAdd.getTile(), planToAdd);
        }
        
        for (Tile extraFoodTile : prioritizeTilesForFoodProduction(unassignedExcept(takenTiles))) {
            final AbstractGoods foodGoods = extraFoodTile.getBestFoodProduction();
            newColonyTilePlans.put(extraFoodTile, TentativeColonyTilePlan.expertProduction(extraFoodTile, foodGoods.getType()));
        }
        
        final TentativeProduction production = determineTentativeProductionAndDeficits(getSpecification(),
                centerTile,
                newColonyTilePlans,
                getBuildingWorkerPlansByType(),
                getBuildingTypesToBuild());
        
        return production.getAmountProduced(getSpecification().getPrimaryFoodType());
    }
    
    private static List<Tile> getWorkTiles(Tile centerTile) {
        return centerTile.getSurroundingTiles(1, 1);
    }
    
    private static List<AbstractGoods> getAutoProduction(Tile centerTile) {
        return getMaximumUnattendedProduction(centerTile).getProductionInfo().getProduction();
    }
    
    public Set<GoodsType> determineRawMaterialTypesProduced() {
        return tentativeColonyTilePlans.values().stream()
                .filter(Objects::nonNull)
                .map(TentativeColonyTilePlan::getProduction)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .map(AbstractGoods::getType)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
    
    public static final class TentativeColonyTilePlan {
        
        private Tile tile;
        private WorkerPlan workerPlan;
        private List<TileImprovementType> tileImprovements;
        private List<AbstractGoods> production;
        
        private TentativeColonyTilePlan(Tile tile, WorkerPlan workerPlan) {
            this.tile = tile;
            this.workerPlan = workerPlan;
            
            final GoodsType goodsType = Objects.requireNonNull(workerPlan.getProductionTypes().get(0), "goodsType");
            final UnitType unitType = Objects.requireNonNull(workerPlan.getUnitType(), "unitType");
            
            this.tileImprovements = determineTileImprovements(tile, goodsType, unitType);
            this.production = List.of(new AbstractGoods(goodsType, tile.getMaximumPotential(goodsType, unitType)));
        }
        
        public Tile getTile() {
            return tile;
        }
        
        public WorkerPlan getWorkerPlan() {
            return workerPlan;
        }
        
        public List<AbstractGoods> getProduction() {
            return production;
        }
        
        public List<TileImprovementType> getTileImprovements() {
            return tileImprovements;
        }

        private List<TileImprovementType> determineTileImprovements(Tile tile, final GoodsType goodsType, final UnitType unitType) {
            /*
             * TODO: Use the same logic as tile.getMaximumPotential
             */
            final TileProductionCalculator tpc = new TileProductionCalculator(null, 2);
            final MaximumPotentialProduction maximumPotentialProduction = tpc.getMaximumPotentialProduction(goodsType, tile, unitType);
            return maximumPotentialProduction.getTileImprovementType();
        }
        
        
        public static TentativeColonyTilePlan expertProduction(Tile tile, GoodsType goodsType) {
            final Specification s = tile.getSpecification();
            final UnitType expertUnitType = s.getExpertForProducing(goodsType);
            final WorkerPlan wp = new WorkerPlan(expertUnitType, new PlannedWorkLocation(tile), List.of(goodsType));
            return new TentativeColonyTilePlan(tile, wp);
        }
        
    }
}
