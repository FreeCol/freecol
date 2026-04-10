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

package net.sf.freecol.common.model.production;

import static net.sf.freecol.common.util.CollectionUtils.concat;
import static net.sf.freecol.common.util.CollectionUtils.forEach;
import static net.sf.freecol.common.util.CollectionUtils.map;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.FeatureContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.ProductionCache;
import net.sf.freecol.common.model.ProductionInfo;
import net.sf.freecol.common.model.ProductionType;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.UnitType;

/**
 * Calculates the production for a tile.
 */
public class TileProductionCalculator {

    private Player owner;
    private int colonyProductionBonus;
    
    
    /**
     * Creates a calculator for the given owner and colony data.
     * 
     * @param owner The {@code Player} owning the building.
     * @param colonyProductionBonus The production bonus for the colony where the building
     *      is located.
     */
    public TileProductionCalculator(Player owner, int colonyProductionBonus) {
        this.owner = owner;
        this.colonyProductionBonus = colonyProductionBonus;
    }
    
    
    /**
     * Gets the basic production information for the colony tile,
     * ignoring any colony limits (which for now, should be
     * irrelevant).
     *
     * In the original game, the following special rules apply to
     * colony center tiles:
     * - All tile improvements contribute to the production of food
     * - Only natural tile improvements, such as rivers, contribute
     *   to the production of other types of goods.
     * - Artificial tile improvements, such as plowing, are ignored.
     *
     * @param tile The {@code Tile} where the production is happening.
     * @param turn The current game turn.
     * @param workerAssignment If any, the worker assign to this tile.
     * @param colonyCenterTile If true, then the tile will autoproduce.
     * @return The raw production of this colony tile.
     * @see ProductionCache#update
     */
    public ProductionInfo getBasicProductionInfo(Tile tile, Turn turn, WorkerAssignment workerAssignment, boolean colonyCenterTile) {
        return getBasicProductionInfo(tile, turn, workerAssignment, colonyCenterTile, List.of());
    }
    
    private ProductionInfo getBasicProductionInfo(Tile tile,
            Turn turn,
            WorkerAssignment workerAssignment,
            boolean colonyCenterTile,
            List<TileImprovementType> additionalTileImprovements) {
        ProductionInfo pi = new ProductionInfo();
        
        if (workerAssignment.getProductionType() == null) {
            /*
             *  XXX: It's silly that the production is calculated
             *       before the productionType is set.
             */
             
            return pi;
        }

        if (colonyCenterTile) {
            forEach(workerAssignment.getProductionType().getOutputs(), output -> {
                    int n = getCenterTileProduction(turn, tile, output.getType(), additionalTileImprovements);
                    if (n > 0) {
                        pi.addProduction(new AbstractGoods(output.getType(), n));
                    }
                });
        } else {
            forEach(map(workerAssignment.getProductionType().getOutputs(), AbstractGoods::getType),
                gt -> {
                    int n = getUnitProduction(turn, tile, workerAssignment, gt, additionalTileImprovements);
                    if (n > 0) {
                        pi.addProduction(new AbstractGoods(gt, n));
                    }
                });
        }
        return pi;
    }
    
    public MaximumPotentialProduction getMaximumPotentialProduction(GoodsType goodsType, Tile tile, UnitType unitType) {
        final Specification spec = tile.getSpecification();
        final Turn turn = new Turn(1);
        final boolean colonyCenterTile = (unitType == null);
        final TileType originalTileType = tile.getType();
        final ProductionType originalProductionType = ProductionType.getBestProductionType(goodsType, originalTileType.getAvailableProductionTypes(colonyCenterTile));
        
        final ProductionInfo result = getBasicProductionInfo(tile, turn, new WorkerAssignment(unitType, originalProductionType), colonyCenterTile);
        MaximumPotentialProduction maximumPotentialProduction = new MaximumPotentialProduction(result, List.of());
        int currentMaximumPotentialProduction = productionOfGoodsType(goodsType, result);
        
        final List<TileImprovementType> tileTypeChanging = spec.getTileImprovementTypeList()
                .stream()
                .filter(tit -> !tit.isNatural() && tit.isTileTypeAllowed(originalTileType) && tit.isChangeType())
                .collect(Collectors.toList());
        
        for (TileImprovementType tit : getNormalImprovements(spec, originalTileType)) {
            if (!tile.isImprovementTypeAllowed(tit)) {
                continue;
            }
            final ProductionInfo newResult = getBasicProductionInfo(tile, turn, new WorkerAssignment(unitType, originalProductionType), colonyCenterTile, List.of(tit));
            final int amount = productionOfGoodsType(goodsType, newResult);
            if (amount > currentMaximumPotentialProduction) {
                currentMaximumPotentialProduction = amount;
                maximumPotentialProduction = new MaximumPotentialProduction(newResult, List.of(tit));
            }
        }
        
        for (TileImprovementType tileTypeChange : tileTypeChanging) {
            final TileType newTileType = tileTypeChange.getChange(originalTileType);
            final ProductionType newProductionType = ProductionType.getBestProductionType(goodsType, newTileType.getAvailableProductionTypes(colonyCenterTile));
            for (TileImprovementType tit : getNormalImprovements(spec, newTileType)) {
                final ProductionInfo newResult = getBasicProductionInfo(tile, turn, new WorkerAssignment(unitType, newProductionType), colonyCenterTile, List.of(tileTypeChange, tit));
                final int amount = productionOfGoodsType(goodsType, newResult);
                if (amount > currentMaximumPotentialProduction) {
                    currentMaximumPotentialProduction = amount;
                    maximumPotentialProduction = new MaximumPotentialProduction(newResult, List.of(tileTypeChange, tit));
                }
            }
            final ProductionInfo newResult = getBasicProductionInfo(tile, turn, new WorkerAssignment(unitType, newProductionType), colonyCenterTile, List.of(tileTypeChange));
            final int amount = productionOfGoodsType(goodsType, newResult);
            if (amount > currentMaximumPotentialProduction) {
                currentMaximumPotentialProduction = amount;
                maximumPotentialProduction = new MaximumPotentialProduction(newResult, List.of(tileTypeChange));
            }
        }
        
        return maximumPotentialProduction;
    }
    
    private static List<TileImprovementType> getNormalImprovements(Specification spec, TileType tileType) {
        return spec.getTileImprovementTypeList()
                .stream()
                .filter(tit -> !tit.isNatural() && tit.isTileTypeAllowed(tileType) && !tit.isChangeType())
                .collect(Collectors.toList());
    }

    private static int productionOfGoodsType(GoodsType goodsType, ProductionInfo newResult) {
        return newResult.getProduction().stream()
                .filter(a -> a.getType().equals(goodsType))
                .map(AbstractGoods::getAmount)
                .findFirst()
                .orElse(0);
    }
    
    /**
     * Gets the productivity of a unit working in this work location,
     * considering *only* the contribution of the unit, exclusive of
     * that of the work location.
     *
     * Used below, only public for the test suite.
     *
     * @param turn The current game turn.
     * @param tile The tile where the production is happening.
     * @param workerAssignment If any, the worker assigned to the {@code Tile}.
     * @param goodsType The {@code GoodsType} to check the production of.
     * @return The maximum return from this unit.
     */
    public int getUnitProduction(Turn turn, Tile tile, WorkerAssignment workerAssignment, GoodsType goodsType, List<TileImprovementType> additionalTileImprovements) {
        if (workerAssignment == null
                || workerAssignment.getProductionType().getOutputs().noneMatch(g -> goodsType.equals(g.getType()))
                || workerAssignment.getUnitType() == null) {
            return 0;
        }
        
        final TileType newTileType = getResultingTileType(tile.getType(), additionalTileImprovements);
        final int production = getBaseProduction(newTileType, workerAssignment.getProductionType(), goodsType, workerAssignment.getUnitType());
        return Math.max(0, (int) FeatureContainer.applyModifiers(
                production,
                turn,
                getProductionModifiers(turn, tile, goodsType, workerAssignment.getUnitType(), additionalTileImprovements)));
    }
    
    private int getCenterTileProduction(Turn turn, Tile tile, GoodsType goodsType, List<TileImprovementType> additionalTileImprovements) {
        final TileType newTileType = getResultingTileType(tile.getType(), additionalTileImprovements);
        final int production = newTileType.getBaseProduction(null, goodsType, null);
        return Math.max(0, (int) FeatureContainer.applyModifiers(
                production,
                turn,
                getCenterTileProductionModifiers(turn, tile, goodsType, additionalTileImprovements)));
    }
    
    /**
     * Get the base production exclusive of any bonuses.
     *
     * @param tileType The tile type where the production is happening.
     * @param productionType An optional {@code ProductionType} to use,
     *     if null the best available one is used.
     * @param goodsType The {@code GoodsType} to produce.
     * @param unitType An optional {@code UnitType} to use.
     * @return The base production due to tile type and resources.
     */
    private int getBaseProduction(TileType tileType, ProductionType productionType,
                                 GoodsType goodsType, UnitType unitType) {
        if (tileType == null || goodsType == null || !goodsType.isFarmed()) {
            return 0;
        }
        final int amount = tileType.getBaseProduction(productionType, goodsType, unitType);
        return (amount < 0) ? 0 : amount;
    }
    
    private TileType getResultingTileType(TileType originalTileType, List<TileImprovementType> additionalTileImprovements) {
        final List<TileImprovementType> tileTypeChange = additionalTileImprovements.stream().filter(tit -> tit.isChangeType()).collect(Collectors.toList());
        if (tileTypeChange.size() > 1) {
            throw new IllegalArgumentException("Only the last tile type change should be included in additionalTileImprovements");
        }
        if (tileTypeChange.isEmpty()) {
            return originalTileType;
        }
        return tileTypeChange.get(0).getChange(originalTileType);
    }
    
    /**
     * Gets the production modifiers for the given type of goods and
     * unit type.
     *
     * @param goodsType The {@code GoodsType} to produce.
     * @param unitType The optional {@code UnitType} to produce them.
     * @return A stream of the applicable modifiers.
     */
    public Stream<Modifier> getProductionModifiers(Turn turn, Tile tile, GoodsType goodsType, UnitType unitType, List<TileImprovementType> additionalTileImprovements) {
        final TileType newTileType = getResultingTileType(tile.getType(), additionalTileImprovements);
        final boolean tileTypeChanged = (newTileType != tile.getType());
        
        final Stream<Modifier> productionModifiers;
        if (!tileTypeChanged) {
            productionModifiers = tile.getProductionModifiers(goodsType, unitType);
        } else if (tile.getTileItemContainer() != null) {
            productionModifiers = tile.getTileItemContainer().getProductionModifiersWithoutResource(goodsType, unitType);
        } else {
            productionModifiers = Stream.of();
        }
        
        if (tileTypeChanged && !newTileType.canProduce(goodsType, unitType)
                || !tile.canProduce(goodsType, unitType)) {
            return Stream.<Modifier>empty();
        }
        
        Stream<Modifier> additionalProductionModifiers = additionalTileImprovements.stream()
                .filter(tit -> !tit.isChangeType())
                .flatMap(tit -> tit.getProductionModifiers(goodsType, unitType, newTileType, 1));
        
        return concat(productionModifiers,
                additionalProductionModifiers,
                (unitType != null) ? unitType.getModifiers(goodsType.getId(), newTileType, turn) : Stream.of(),
                (owner != null && unitType != null) ? owner.getModifiers(goodsType.getId(), unitType, turn) : Stream.of(),
                //(owner != null && unitType == null) ? owner.getModifiers(goodsType.getId(), tile.getType(), turn) : Stream.of()
                ProductionUtils.getRebelProductionModifiersForTile(((!tileTypeChanged) ? tile : null), colonyProductionBonus, goodsType, unitType));
    }

    /**
     * Gets the production modifiers for the given type of goods on
     * the colony center tile.
     *
     * @param goodsType The {@code GoodsType} to produce.
     * @param unitType The optional {@code UnitType} to produce them.
     * @return A stream of the applicable modifiers.
     */
    public Stream<Modifier> getCenterTileProductionModifiers(Turn turn, Tile tile, GoodsType goodsType, List<TileImprovementType> additionalTileImprovements) {        
        return getProductionModifiers(turn, tile, goodsType, null, additionalTileImprovements);
    }
    
    public static class MaximumPotentialProduction {
        private ProductionInfo productionInfo;
        private List<TileImprovementType> tileImprovementType;
        
        public MaximumPotentialProduction(ProductionInfo productionInfo, List<TileImprovementType> tileImprovementType) {
            this.productionInfo = productionInfo;
            this.tileImprovementType = tileImprovementType;
        }
        
        public ProductionInfo getProductionInfo() {
            return productionInfo;
        }
        
        public List<TileImprovementType> getTileImprovementType() {
            return tileImprovementType;
        }
        
        public int getAmount(GoodsType goodsType) {
            return productionOfGoodsType(goodsType, productionInfo);
        }
    }
}
