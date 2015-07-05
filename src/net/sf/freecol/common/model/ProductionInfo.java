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
import java.util.List;


/**
 * Container for information about production in a colony.
 */
public class ProductionInfo {

    /** The maximum production possible given unlimited input. */
    private List<AbstractGoods> maximumProduction = new ArrayList<>();

    /** The actual production. */
    private List<AbstractGoods> production = new ArrayList<>();

    /** The maximum consumption possible given unlimited input. */
    private List<AbstractGoods> maximumConsumption = new ArrayList<>();

    /** The actual consumption. */
    private List<AbstractGoods> consumption = new ArrayList<>();


    public final List<AbstractGoods> getConsumption() {
        return consumption;
    }

    public final void setConsumption(final List<AbstractGoods> newConsumption) {
        this.consumption = newConsumption;
    }

    public void addConsumption(AbstractGoods goods) {
        consumption.add(goods);
    }

    public final List<AbstractGoods> getProduction() {
        return production;
    }

    public final void setProduction(final List<AbstractGoods> newProduction) {
        this.production = newProduction;
    }

    public void addProduction(AbstractGoods goods) {
        production.add(goods);
    }

    public void addProduction(List<AbstractGoods> goods) {
        production.addAll(goods);
    }

    public final List<AbstractGoods> getMaximumProduction() {
        return maximumProduction;
    }

    public final void setMaximumProduction(final List<AbstractGoods> newMaximumProduction) {
        this.maximumProduction = newMaximumProduction;
    }

    public void addMaximumProduction(AbstractGoods goods) {
        maximumProduction.add(goods);
    }

    /**
     * Get a list of the goods that are in production deficit, that is,
     * those which are produced at less than their maximum possible rate.
     *
     * @return A list of <code>AbstractGoods</code>.
     */
    public List<AbstractGoods> getProductionDeficit() {
        if (this.maximumProduction.isEmpty()) {
            return WorkLocation.EMPTY_LIST;
        }
        List<AbstractGoods> result = new ArrayList<>();
        for (AbstractGoods ag : this.production) {
            AbstractGoods agMax = AbstractGoods.findByType(ag.getType(),
                this.maximumProduction);
            if (agMax == null) continue;
            int amount = agMax.getAmount() - ag.getAmount();
            if (amount != 0) {
                result.add(new AbstractGoods(ag.getType(), amount));
            }
        }
        return result;
    }

    /**
     * Get a list of the goods that are in consumption deficit, that is,
     * those which are consumed at less than their maximum possible rate.
     *
     * @return A list of <code>AbstractGoods</code>.
     */
    public List<AbstractGoods> getConsumptionDeficit() {
        if (this.maximumConsumption.isEmpty()) {
            return WorkLocation.EMPTY_LIST;
        }
        List<AbstractGoods> result = new ArrayList<>();
        for (AbstractGoods ag : this.consumption) {
            AbstractGoods agMax = AbstractGoods.findByType(ag.getType(),
                this.maximumConsumption);
            if (agMax == null) continue;
            int amount = agMax.getAmount() - ag.getAmount();
            if (amount != 0) {
                result.add(new AbstractGoods(ag.getType(), amount));
            }
        }
        return result;
    }

    /**
     * Does production equal maximum production?
     *
     * @return True if at maximum production.
     */
    public boolean hasMaximumProduction() {
        if (maximumProduction.isEmpty()) return true;

        for (int index = 0; index < production.size(); index++) {
            if (maximumProduction.size() < index) return true;

            if (maximumProduction.get(index).getAmount()
                > production.get(index).getAmount()) return false;
        }
        return true;
    }

    public final List<AbstractGoods> getMaximumConsumption() {
        return maximumConsumption;
    }

    public final void setMaximumConsumption(final List<AbstractGoods> newMaximumConsumption) {
        this.maximumConsumption = newMaximumConsumption;
    }

    public void addMaximumConsumption(AbstractGoods goods) {
        maximumConsumption.add(goods);
    }

    private void append(StringBuilder result, String key,
                        List<AbstractGoods> list) {
        if (list.isEmpty()) return;

        result.append(key).append(": ");
        for (AbstractGoods goods : list) {
            result.append(goods);
            if (goods.getType().getStoredAs() != goods.getType()) {
                result.append(" [")
                    .append(goods.getType().getStoredAs().getId())
                    .append("]");
            }
            result.append(", ");
        }
        int length = result.length();
        result.replace(length - 2, length, "\n");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        append(result, "Production", production);
        append(result, "Consumption", consumption);
        append(result, "Maximum Production", maximumProduction);
        append(result, "Maximum Consumption", maximumConsumption);
        return result.toString();
    }
}
