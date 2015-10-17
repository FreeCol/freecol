/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * The <code>ProductionCache</code> is contains all relevant
 * information about the production and consumption of the
 * colony. This includes the production of all colony tiles and
 * buildings, as well as the consumption of all units, buildings and
 * build queues.
 *
 */
public class ProductionCache {

    /**
     * The colony whose production is being cached. The goods stored
     * in the colony may need to be considered in order to prevent
     * excess production.
     */
    private final Colony colony;

    /** A map of net production by goods type. */
    private final TypeCountMap<GoodsType> netProduction
        = new TypeCountMap<>();

    /** A map of production info for various producers and consumers. */
    private final Map<Object, ProductionInfo> productionAndConsumption
        = new HashMap<>();

    /** A set of the goods used by the colony. */
    private final Set<GoodsType> goodsUsed = new HashSet<>();

    /**
     * Flag to indicate whether the cache is up to date, or not and
     * needs {@link #update} to be called.
     */
    private boolean upToDate = false;


    /**
     * Creates a new <code>ProductionCache</code> instance.
     *
     * @param colony a <code>Colony</code> value
     */
    public ProductionCache(Colony colony) {
        this.colony = colony;
    }


    /**
     * Updates all data structures.  The method has no side effects.
     *
     * For now, there is a hard assumption that ColonyTiles do not
     * consume but Buildings do.  One day we may want to generalize
     * this, which will require processing the goods types in an order
     * sorted by the requirement dependencies.  But not yet.  This
     * assumption is made explicit by getting the ProductionInfo from
     * ColonyTiles with the simple getBasicProductionInfo() routine,
     * but from Buildings with getAdjustedProductionInfo() which takes
     * account of the input and output goods levels.
     *
     * FIXME: Ideally these should be unified into a
     * WorkLocation.getProductionInfo with the Building-form
     * arguments.
     */
    private synchronized void update() {
        if (upToDate) return; // nothing to do
        final Specification spec = colony.getSpecification();
        final GoodsType bells = spec.getGoodsType("model.goods.bells");

        productionAndConsumption.clear();
        netProduction.clear();
        goodsUsed.clear();
        ProductionMap production = new ProductionMap();

        for (ColonyTile colonyTile : colony.getColonyTiles()) {
            ProductionInfo info = colonyTile.getBasicProductionInfo();
            production.add(info.getProduction());
            productionAndConsumption.put(colonyTile, info);
            for (AbstractGoods goods : info.getProduction()) {
                goodsUsed.add(goods.getType());
                netProduction.incrementCount(goods.getType().getStoredAs(),
                                             goods.getAmount());
            }
        }

        // Add bell production to compensate for the units-that-use-no-bells
        // as this is not handled by the unit conumption.
        int unitsThatUseNoBells
            = spec.getInteger(GameOptions.UNITS_THAT_USE_NO_BELLS);
        int amount = Math.min(unitsThatUseNoBells, colony.getUnitCount());
        ProductionInfo bellsInfo = new ProductionInfo();
        bellsInfo.addProduction(new AbstractGoods(bells, amount));
        productionAndConsumption.put(this, bellsInfo);
        netProduction.incrementCount(bells, amount);

        List<AbstractGoods> goods = new ArrayList<>();
        for (Consumer consumer : colony.getConsumers()) {
            Set<Modifier> modifiers = consumer
                .getModifiers(Modifier.CONSUME_ONLY_SURPLUS_PRODUCTION);
            goods.clear();
            for (AbstractGoods g : consumer.getConsumedGoods()) {
                goodsUsed.add(g.getType());
                AbstractGoods surplus
                    = new AbstractGoods(production.get(g.getType()));
                if (modifiers.isEmpty()) {
                    surplus.setAmount(surplus.getAmount()
                        + getGoodsCount(g.getType()));
                } else {
                    surplus.setAmount((int)FeatureContainer
                        .applyModifiers(surplus.getAmount(), null, modifiers));
                }
                goods.add(surplus);
            }
            ProductionInfo info = null;
            if (consumer instanceof Building) {
                Building building = (Building)consumer;
                List<AbstractGoods> outputs = new ArrayList<>();
                for (AbstractGoods output : building.getOutputs()) {
                    GoodsType outputType = output.getType();
                    goodsUsed.add(outputType);
                    AbstractGoods newOutput
                        = new AbstractGoods(production.get(outputType));
                    newOutput.setAmount(newOutput.getAmount()
                        + getGoodsCount(outputType));
                    outputs.add(newOutput);
                }
                info = building.getAdjustedProductionInfo(goods, outputs);
            } else if (consumer instanceof Unit) {
                info = ((Unit)consumer).getProductionInfo(goods);
            } else if (consumer instanceof BuildQueue) {
                info = ((BuildQueue<?>)consumer).getProductionInfo(goods);
            }
            if (info != null) {
                production.add(info.getProduction());
                production.remove(info.getConsumption());
                for (AbstractGoods g : info.getProduction()) {
                    netProduction.incrementCount(g.getType().getStoredAs(),
                                                 g.getAmount());
                }
                for (AbstractGoods g : info.getConsumption()) {
                    netProduction.incrementCount(g.getType().getStoredAs(),
                                                 -g.getAmount());
                }
                productionAndConsumption.put(consumer, info);
            }
        }
        upToDate = true;
    }


    /**
     * Returns the number of goods of the given type stored in the
     * colony.
     *
     * @param type a <code>GoodsType</code> value
     * @return an <code>int</code> value
     */
    private int getGoodsCount(GoodsType type) {
        return colony.getGoodsCount(type);
    }

    /**
     * Invalidates the production cache. This method needs to be
     * called whenever global production modifiers change. This might
     * be the case when a new {@link FoundingFather} is added, or when
     * the colony's production bonus changes.
     *
     */
    public synchronized void invalidate() {
        upToDate = false;
    }

    /**
     * Invalidates the production cache if it produces or consumes the
     * given GoodsType. This method needs to be called whenever goods
     * are added to or removed from the colony.
     *
     * @param goodsType a <code>GoodsType</code> value
     */
    public synchronized void invalidate(GoodsType goodsType) {
        if (goodsUsed.contains(goodsType)) {
            upToDate = false;
        }
    }

    /**
     * Does this production cache contain production of a goods type?
     *
     * @param goodsType The <code>GoodsType</code> to check.
     * @return True if there is a production entry for the given type.
     */
    public boolean isProducing(GoodsType goodsType) {
        update();
        return any(productionAndConsumption.values(),
            pi -> AbstractGoods.containsType(goodsType, pi.getProduction()));
    }

    /**
     * Does this production cache contain consumption of a goods type?
     *
     * @param goodsType The <code>GoodsType</code> to check.
     * @return True if there is a consumption entry for the given type.
     */
    public boolean isConsuming(GoodsType goodsType) {
        update();
        return any(productionAndConsumption.values(),
            pi -> AbstractGoods.containsType(goodsType, pi.getConsumption()));
    }
    
    /**
     * Returns the net production, that is the total production minus
     * the total consumption, of the given GoodsType.
     *
     * @param type a <code>GoodsType</code> value
     * @return an <code>int</code> value
     */
    public int getNetProductionOf(GoodsType type) {
        update();
        return netProduction.getCount(type);
    }

    /**
     * Gets the <code>ProductionInfo</code> for the given
     * {@link WorkLocation} or {@link Consumer}.
     *
     * @param object an <code>Object</code> value
     * @return a <code>ProductionInfo</code> value
     */
    public ProductionInfo getProductionInfo(Object object) {
        update();
        return productionAndConsumption.get(object);
    }

    /**
     * Gets a copy of the current production state.
     *
     * @return A copy of the current production state.
     */
    public TypeCountMap<GoodsType> getProductionMap() {
        update();
        TypeCountMap<GoodsType> result = new TypeCountMap<>();
        result.putAll(netProduction);
        return result;
    }
}
