/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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

package net.sf.freecol.common.model.production;

import static net.sf.freecol.common.util.CollectionUtils.concat;
import static net.sf.freecol.common.util.CollectionUtils.count;
import static net.sf.freecol.common.util.CollectionUtils.find;
import static net.sf.freecol.common.util.CollectionUtils.isNotNull;
import static net.sf.freecol.common.util.CollectionUtils.map;
import static net.sf.freecol.common.util.CollectionUtils.matchKey;
import static net.sf.freecol.common.util.CollectionUtils.sum;
import static net.sf.freecol.common.util.CollectionUtils.transform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.FeatureContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.ProductionCache;
import net.sf.freecol.common.model.ProductionInfo;
import net.sf.freecol.common.model.ProductionType;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.option.GameOptions;

/**
 * Calculates the production for a building of a given type.
 */
public class BuildingProductionCalculator {
    private final static double EPSILON = 0.0001;
    

    private Player owner;
    private FeatureContainer colonyFeatureContainer;
    private int colonyProductionBonus;
    
    
    /**
     * Creates a calculator for the given owner and colony data.
     * 
     * @param owner The {@code Player} owning the building.
     * @param colonyFeatureContainer The {@code FeatureContainer} for the colony where the
     *      building is located. This is used for applying bonus to the production.
     * @param colonyProductionBonus The production bonus for the colony where the building
     *      is located.
     */
    public BuildingProductionCalculator(Player owner, FeatureContainer colonyFeatureContainer, int colonyProductionBonus) {
        this.owner = owner;
        this.colonyFeatureContainer = colonyFeatureContainer;
        this.colonyProductionBonus = colonyProductionBonus;
    }
    
    
    /**
     * Gets the production information for a building taking account
     * of the available input and output goods.
     *
     * @param buildingType The type of building.
     * @param turn The current game turn.
     * @param workerAssignments A list of workers assigned to the building.
     * @param inputs The input goods available.
     * @param outputs The output goods already available in the colony,
     *     necessary in order to avoid excess production.
     * @param warehouseCapacity The storage capacity of the settlement
     *     producing the goods.
     * @return The production information.
     * @see ProductionCache#update
     */
    public ProductionInfo getAdjustedProductionInfo(
            BuildingType buildingType,
            Turn turn,
            List<WorkerAssignment> workerAssignments,
            List<AbstractGoods> inputs,
            List<AbstractGoods> outputs,
            int warehouseCapacity) {
        ProductionInfo result = new ProductionInfo();
        
        final List<AbstractGoods> buildingOutputs = getOutputs(buildingType, workerAssignments);
        if (buildingOutputs.isEmpty()) return result;
        
        final List<AbstractGoods> buildingInputs = getInputs(buildingType, workerAssignments);
        
        final Specification spec = buildingType.getSpecification();
        final boolean avoidOverflow = buildingType.hasAbility(Ability.AVOID_EXCESS_PRODUCTION);
        // Calculate two production ratios, the minimum (and actual)
        // possible multiplier between the nominal input and output
        // goods and the amount actually consumed and produced, and
        // the maximum possible ratio that would apply but for
        // circumstances such as limited input availability.
        double maximumRatio = 0.0, minimumRatio = Double.MAX_VALUE;

        // First, calculate the nominal production ratios.
        if (buildingType.hasAbility(Ability.AUTO_PRODUCTION)) {
            // Autoproducers are special
            for (AbstractGoods output : transform(buildingOutputs.stream(),
                                                  AbstractGoods::isPositive)) {
                final GoodsType goodsType = output.getType();
                //int available = colony.getGoodsCount(goodsType);
                int available = outputs.stream().filter(gt -> gt.getType().equals(goodsType)).findAny().map(AbstractGoods::getAmount).orElse(0);
                if (available >= warehouseCapacity) {
                    minimumRatio = maximumRatio = 0.0;
                } else {
                    int divisor = (int) buildingType.apply(0f, turn, Modifier.BREEDING_DIVISOR);
                    int factor = (int) buildingType.apply(0f, turn, Modifier.BREEDING_FACTOR);
                    int production = (available < goodsType.getBreedingNumber()
                        || divisor <= 0) ? 0
                        // Deliberate use of integer division
                        : ((available - 1) / divisor + 1) * factor;
                    double newRatio = (double)production / output.getAmount();
                    minimumRatio = Math.min(minimumRatio, newRatio);
                    maximumRatio = Math.max(maximumRatio, newRatio);
                }
            }
        } else {
            for (AbstractGoods output : buildingOutputs) {
                final GoodsType goodsType = output.getType();
                float production = determineProduction(buildingType, workerAssignments, turn, goodsType);

                // Beware!  If we ever unify this code with ColonyTile,
                // ColonyTiles have outputs with zero amount.
                double newRatio = production / output.getAmount();
                minimumRatio = Math.min(minimumRatio, newRatio);
                maximumRatio = Math.max(maximumRatio, newRatio);
            }
        }

        // Then reduce the minimum ratio if some input is in short supply.
        for (AbstractGoods input : buildingInputs) {
            long required = (long)Math.floor(input.getAmount() * minimumRatio);
            long available = getAvailable(input.getType(), inputs);
            // Do not allow auto-production to go negative.
            if (buildingType.hasAbility(Ability.AUTO_PRODUCTION)) available = Math.max(0, available);
            // Experts in factory level buildings may produce a
            // certain amount of goods even when no input is available.
            // Factories have the EXPERTS_USE_CONNECTIONS ability.
            long minimumGoodsInput;
            if (available < required
                && buildingType.hasAbility(Ability.EXPERTS_USE_CONNECTIONS)
                && spec.getBoolean(GameOptions.EXPERTS_HAVE_CONNECTIONS)
                && ((minimumGoodsInput = buildingType.getExpertConnectionProduction()
                        * count(workerAssignments.stream().map(WorkerAssignment::getUnitType),
                            matchKey(getExpertUnitType(buildingType))))
                    > available)) {
                available = minimumGoodsInput;
            }
            // Scale production by limitations on availability.
            if (available < required) {
                minimumRatio *= (double)available / required;
                //maximumRatio = Math.max(maximumRatio, minimumRatio);
            }
        }

        // Check whether there is space enough to store the goods
        // produced in order to avoid excess production.
        if (avoidOverflow) {
            for (AbstractGoods output : buildingOutputs) {
                double production = output.getAmount() * minimumRatio;
                if (production <= 0) continue;
                double headroom = (double)warehouseCapacity
                    - getAvailable(output.getType(), outputs);
                // Clamp production at warehouse capacity
                if (production > headroom) {
                    minimumRatio = Math.min(minimumRatio,
                        headroom / output.getAmount());
                }
                production = output.getAmount() * maximumRatio;
                if (production > headroom) {
                    maximumRatio = Math.min(maximumRatio, 
                        headroom / output.getAmount());
                }
            }
        }

        for (AbstractGoods input : buildingInputs) {
            GoodsType type = input.getType();
            // maximize consumption
            int consumption = (int)Math.floor(input.getAmount()
                * minimumRatio + EPSILON);
            int maximumConsumption = (int)Math.floor(input.getAmount()
                * maximumRatio);
            result.addConsumption(new AbstractGoods(type, consumption));
            if (consumption < maximumConsumption) {
                result.addMaximumConsumption(new AbstractGoods(type, maximumConsumption));
            }
        }
        for (AbstractGoods output : buildingOutputs) {
            GoodsType type = output.getType();
            // minimize production, but add a magic little something
            // to counter rounding errors
            int production = (int)Math.floor(output.getAmount() * minimumRatio
                + EPSILON);
            int maximumProduction = (int)Math.floor(output.getAmount()
                * maximumRatio);
            result.addProduction(new AbstractGoods(type, production));
            if (production < maximumProduction) {
                result.addMaximumProduction(new AbstractGoods(type, maximumProduction));
            }
        }
        return result;
    }
    
    /**
     * Gets the unit type that is the expert for this work location
     * using its first output for which an expert type can be found.
     *
     * @return The expert {@code UnitType} or null if none found.
     */
    public UnitType getExpertUnitType(BuildingType buildingType) {
        final Specification spec = buildingType.getSpecification();
        ProductionType pt = getBestProductionType(buildingType);
        return (pt == null) ? null
            : find(map(pt.getOutputs(),
                       ag -> spec.getExpertForProducing(ag.getType())),
                   isNotNull());
    }
    
    private ProductionType getBestProductionType(BuildingType buildingType) {
        return ProductionType.getBestProductionType(null, buildingType.getAvailableProductionTypes(false));
    }
    
    private List<AbstractGoods> getOutputs(BuildingType buildingType, List<WorkerAssignment> workerAssignments) {
        final List<AbstractGoods> unattendedOutputs = buildingType.getAvailableProductionTypes(true).stream()
                .map(pt -> pt.getOutputList())
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        
        final List<AbstractGoods> workerOutputs = workerAssignments.stream()
                //.map(wa -> wa.getProductionType().getOutputList())
                .map(WorkerAssignment::getProductionType)
                /*
                 * XXX: This code is needed when a production type have yet to be
                 *      chosen for the worker. But why are we calling this method
                 *      in that case?
                 */
                .filter(Objects::nonNull)
                .map(pt -> pt.getOutputList())
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        
        final List<AbstractGoods> allOutputs = new ArrayList<>(unattendedOutputs);
        allOutputs.addAll(workerOutputs);
        
        final Map<GoodsType, Integer> amounts = new HashMap<>();
        for (AbstractGoods ag : allOutputs) {
            if (amounts.get(ag.getType()) == null) {
                amounts.put(ag.getType(), 0);
            }
            amounts.put(ag.getType(), amounts.get(ag.getType()) + ag.getAmount());
        }
        
        return amounts.entrySet().stream().map(e -> new AbstractGoods(e.getKey(), e.getValue())).collect(Collectors.toList());
    }
    
    private List<AbstractGoods> getInputs(BuildingType buildingType, List<WorkerAssignment> workerAssignments) {
        final List<AbstractGoods> unattendedInputs = buildingType.getAvailableProductionTypes(true).stream()
                .map(pt -> pt.getInputList())
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        final List<AbstractGoods> workerInputs = workerAssignments.stream()
                //.map(wa -> wa.getProductionType().getInputList())
                .map(WorkerAssignment::getProductionType)
                /*
                 * XXX: This code is needed when a production type have yet to be
                 *      chosen for the worker. But why are we calling this method
                 *      in that case?
                 */
                .filter(Objects::nonNull)
                .map(pt -> pt.getInputList())
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        
        final List<AbstractGoods> allInputs = new ArrayList<>(unattendedInputs);
        allInputs.addAll(workerInputs);
        
        final Map<GoodsType, Integer> amounts = new HashMap<>();
        for (AbstractGoods ag : allInputs) {
            if (amounts.get(ag.getType()) == null) {
                amounts.put(ag.getType(), 0);
            }
            amounts.put(ag.getType(), amounts.get(ag.getType()) + ag.getAmount());
        }
        
        return amounts.entrySet().stream().map(e -> new AbstractGoods(e.getKey(), e.getValue())).collect(Collectors.toList());
    }

    private int determineProduction(BuildingType buildingType, List<WorkerAssignment> workerAssignments, final Turn turn, final GoodsType goodsType) {
        float production = sum(workerAssignments,
                               wa -> getUnitProduction(turn, buildingType, wa, goodsType));
        // Unattended production always applies for buildings!
        production += getBaseProduction(buildingType, null, goodsType, null);
        production = FeatureContainer.applyModifiers(production, turn,
                                    getProductionModifiers(turn, buildingType, goodsType, null));
        return (int) Math.floor(production);
    }
    
    /**
     * Convenience function to extract a goods amount from a list of
     * available goods.
     *
     * @param type The {@code GoodsType} to extract the amount for.
     * @param available The list of available goods to query.
     * @return The goods amount, or zero if none found.
     */
    private int getAvailable(GoodsType type, List<AbstractGoods> available) {
        return AbstractGoods.getCount(type, available);
    }
    
    /**
     * Gets the productivity of a unit working in this work location,
     * considering *only* the contribution of the unit, exclusive of
     * that of the work location.
     *
     * @param turn The current game turn.
     * @param buildingType The type of building.
     * @param workerAssignment The worker assigned to the building.
     * @param goodsType The {@code GoodsType} to check the production of.
     * @return The maximum return from this unit.
     */
    private int getUnitProduction(Turn turn, BuildingType buildingType, WorkerAssignment workerAssignment, GoodsType goodsType) {
        if (workerAssignment == null || workerAssignment.getProductionType().getOutputs().noneMatch(g -> goodsType.equals(g.getType()))) {
            return 0;
        }
        
        return Math.max(0,
                        (int) FeatureContainer.applyModifiers(getBaseProduction(buildingType, workerAssignment.getProductionType(), goodsType, workerAssignment.getUnitType()),
                                            turn,
                                            getProductionModifiers(turn, buildingType, goodsType, workerAssignment.getUnitType())));
    }
    
    private int getBaseProduction(BuildingType buildingType, ProductionType productionType, GoodsType goodsType, UnitType unitType) {
        return (buildingType == null) ? 0 : buildingType.getBaseProduction(productionType, goodsType, unitType);
    }
    
    private Stream<Modifier> getProductionModifiers(Turn turn, BuildingType buildingType, GoodsType goodsType,
            UnitType unitType) {
        final String id = (goodsType == null) ? null : goodsType.getId();
        
        return (unitType != null)
            // With a unit, unit specific bonuses apply
            ? concat(buildingType.getModifiers(id, unitType, turn),
                    ProductionUtils.getRebelProductionModifiersForBuilding(buildingType, colonyProductionBonus, goodsType, unitType),
                    buildingType.getCompetenceModifiers(id, unitType, turn),
                    (owner == null) ? null : owner.getModifiers(id, unitType, turn))
            // With no unit, only the building-specific bonuses 
            : concat(colonyFeatureContainer.getModifiers(id, buildingType, turn), // XXX: Can we simplify this?
                    (owner == null) ? null : owner.getModifiers(id, buildingType, turn));
    }
}
