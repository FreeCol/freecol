/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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
import java.util.List;
import java.util.Map;

public class ProductionCache {

    private Colony colony;

    private TypeCountMap<GoodsType> netProduction = null;

    private Map<Object, ProductionInfo> productionAndConsumption = null;

    private boolean upToDate = false;


    public ProductionCache(Colony colony) {
        this.colony = colony;
    }


    /**
     * Returns a data structure containing all relevant information
     * about the production and consumption of the colony. This
     * includes the production of all colony tiles and buildings, as
     * well as the consumption of all units, buildings and build
     * queues. The method has no side-effects.
     *
     * @return a map using units, work locations and build queues as
     * keys, and <code>ProductionInfo</code> objects as values
     */
    private synchronized void update() {
        if (upToDate) return; // nothing to do
        productionAndConsumption = new HashMap<Object, ProductionInfo>();
        netProduction = new TypeCountMap<GoodsType>();
        ProductionMap production = new ProductionMap();
        for (ColonyTile colonyTile : colony.getColonyTiles()) {
            List<AbstractGoods> p = colonyTile.getProduction();
            if (!p.isEmpty()) {
                production.add(p);
                ProductionInfo info = new ProductionInfo();
                info.addProduction(p);
                productionAndConsumption.put(colonyTile, info);
                for (AbstractGoods goods : p) {
                    netProduction.incrementCount(goods.getType().getStoredAs(), goods.getAmount());
                }
            }
        }

        GoodsType bells = colony.getSpecification().getGoodsType("model.goods.bells");
        int unitsThatUseNoBells = colony.getSpecification().getInteger("model.option.unitsThatUseNoBells");
        int amount = Math.min(unitsThatUseNoBells, colony.getUnitCount());
        ProductionInfo bellsInfo = new ProductionInfo();
        bellsInfo.addProduction(new AbstractGoods(bells, amount));
        productionAndConsumption.put(this, bellsInfo);
        netProduction.incrementCount(bells, amount);

        for (Consumer consumer : colony.getConsumers()) {
            boolean surplusOnly = consumer.hasAbility("model.ability.consumeOnlySurplusProduction");
            List<AbstractGoods> goods = new ArrayList<AbstractGoods>();
            for (AbstractGoods g : consumer.getConsumedGoods()) {
                AbstractGoods surplus = new AbstractGoods(production.get(g.getType()));
                if (!surplusOnly) {
                    surplus.setAmount(surplus.getAmount() + getGoodsCount(g.getType()));
                }
                goods.add(surplus);
            }
            ProductionInfo info = null;
            if (consumer instanceof Building) {
                Building building = (Building) consumer;
                AbstractGoods output = null;
                if (building.getGoodsOutputType() != null) {
                    output = new AbstractGoods(production.get(building.getGoodsOutputType()));
                    output.setAmount(output.getAmount() + getGoodsCount(output.getType()));
                }
                info = building.getProductionInfo(output, goods);
            } else if (consumer instanceof Unit) {
                info = ((Unit) consumer).getProductionInfo(goods);
            } else if (consumer instanceof BuildQueue) {
                info = ((BuildQueue<?>) consumer).getProductionInfo(goods);
            }
            production.add(info.getProduction());
            production.remove(info.getConsumption());
            for (AbstractGoods g : info.getProduction()) {
                netProduction.incrementCount(g.getType().getStoredAs(), g.getAmount());
            }
            for (AbstractGoods g : info.getConsumption()) {
                netProduction.incrementCount(g.getType().getStoredAs(), -g.getAmount());
            }
            for (AbstractGoods g : info.getStorage()) {
                netProduction.incrementCount(g.getType().getStoredAs(), g.getAmount());
            }
            productionAndConsumption.put(consumer, info);
        }
        this.productionAndConsumption = productionAndConsumption;
        this.netProduction = netProduction;
        upToDate = true;
    }


    private int getGoodsCount(GoodsType type) {
        return colony.getGoodsCount(type);
    }


    public synchronized void invalidate() {
        upToDate = false;
    }


    public int getNetProductionOf(GoodsType type) {
        update();
        return netProduction.getCount(type);
    }

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
        TypeCountMap<GoodsType> result = new TypeCountMap<GoodsType>();
        result.putAll(netProduction);
        return result;
    }
}
