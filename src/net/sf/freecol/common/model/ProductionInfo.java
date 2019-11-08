/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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
import java.util.function.Function;

import static net.sf.freecol.common.util.CollectionUtils.*;


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
     * @return A list of {@code AbstractGoods}.
     */
    public List<AbstractGoods> getProductionDeficit() {
        final Function<AbstractGoods, AbstractGoods> mapper = ag -> {
            AbstractGoods agMax = find(this.maximumProduction,
                                       AbstractGoods.matches(ag.getType()));
            int amount = (agMax == null) ? 0
                : agMax.getAmount() - ag.getAmount();
            return (amount <= 0) ? null
                : new AbstractGoods(ag.getType(), amount);
        };            
        return (this.maximumProduction.isEmpty()) ? WorkLocation.EMPTY_LIST
            : transform(this.production, alwaysTrue(), mapper,
                        toListNoNulls());
    }

    /**
     * Get a list of the goods that are in consumption deficit, that is,
     * those which are consumed at less than their maximum possible rate.
     *
     * @return A list of {@code AbstractGoods}.
     */
    public List<AbstractGoods> getConsumptionDeficit() {
        final Function<AbstractGoods, AbstractGoods> mapper = ag -> {
            AbstractGoods agMax = find(this.maximumConsumption,
                                       AbstractGoods.matches(ag.getType()));
            int amount = (agMax == null) ? 0
                : agMax.getAmount() - ag.getAmount();
            return (amount == 0) ? null
                : new AbstractGoods(ag.getType(), amount);
        };
        return (this.maximumConsumption.isEmpty()) ? WorkLocation.EMPTY_LIST
            : transform(this.consumption, alwaysTrue(), mapper,
                        toListNoNulls());
    }

    /**
     * Does production equal maximum production?
     *
     * @return True if at maximum production.
     */
    public boolean atMaximumProduction() {
        if (maximumProduction.isEmpty()) return true;

        for (AbstractGoods ag : production) {
            AbstractGoods agMax = find(this.maximumConsumption,
                                       AbstractGoods.matches(ag.getType()));
            if (agMax != null && agMax.getAmount() > ag.getAmount())
                return false;
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
                    .append(']');
            }
            result.append(", ");
        }
        int length = result.length();
        result.replace(length - 2, length, "\n");
    }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        append(result, "Production", production);
        append(result, " Consumption", consumption);
        append(result, " Maximum Production", maximumProduction);
        append(result, " Maximum Consumption", maximumConsumption);
        return result.toString();
    }
}
