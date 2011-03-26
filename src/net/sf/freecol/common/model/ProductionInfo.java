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
import java.util.List;


public class ProductionInfo {

    /**
     * The goods moved to storage.
     */
    private List<AbstractGoods> storage = new ArrayList<AbstractGoods>();

    /**
     * The maximum production possible given unlimited input.
     */
    private List<AbstractGoods> maximumProduction = new ArrayList<AbstractGoods>();

    /**
     * The actual production.
     */
    private List<AbstractGoods> production = new ArrayList<AbstractGoods>();

    /**
     * The maximum consumption possible given unlimited input.
     */
    private List<AbstractGoods> maximumConsumption = new ArrayList<AbstractGoods>();

    /**
     * The actual consumption.
     */
    private List<AbstractGoods> consumption = new ArrayList<AbstractGoods>();


    /**
     * Get the <code>Consumption</code> value.
     *
     * @return a <code>List<AbstractGoods></code> value
     */
    public final List<AbstractGoods> getConsumption() {
        return consumption;
    }

    /**
     * Set the <code>Consumption</code> value.
     *
     * @param newConsumption The new Consumption value.
     */
    public final void setConsumption(final List<AbstractGoods> newConsumption) {
        this.consumption = newConsumption;
    }

    /**
     * Describe <code>addConsumption</code> method here.
     *
     * @param goods an <code>AbstractGoods</code> value
     */
    public void addConsumption(AbstractGoods goods) {
        consumption.add(goods);
    }

    /**
     * Get the <code>Production</code> value.
     *
     * @return a <code>List<AbstractGoods></code> value
     */
    public final List<AbstractGoods> getProduction() {
        return production;
    }

    /**
     * Set the <code>Production</code> value.
     *
     * @param newProduction The new Production value.
     */
    public final void setProduction(final List<AbstractGoods> newProduction) {
        this.production = newProduction;
    }

    /**
     * Describe <code>addProduction</code> method here.
     *
     * @param goods an <code>AbstractGoods</code> value
     */
    public void addProduction(AbstractGoods goods) {
        production.add(goods);
    }

    /**
     * Describe <code>addProduction</code> method here.
     *
     * @param goods an <code>AbstractGoods</code> value
     */
    public void addProduction(List<AbstractGoods> goods) {
        production.addAll(goods);
    }

    /**
     * Get the <code>MaximumProduction</code> value.
     *
     * @return a <code>List<AbstractGoods></code> value
     */
    public final List<AbstractGoods> getMaximumProduction() {
        return maximumProduction;
    }

    /**
     * Set the <code>MaximumProduction</code> value.
     *
     * @param newMaximumProduction The new MaximumProduction value.
     */
    public final void setMaximumProduction(final List<AbstractGoods> newMaximumProduction) {
        this.maximumProduction = newMaximumProduction;
    }

    /**
     * Describe <code>addMaximumProduction</code> method here.
     *
     * @param goods an <code>AbstractGoods</code> value
     */
    public void addMaximumProduction(AbstractGoods goods) {
        maximumProduction.add(goods);
    }

    /**
     * Returns true if production equals maximum production.
     *
     * @return a <code>boolean</code> value
     */
    public boolean hasMaximumProduction() {
        for (int index = 0; index < production.size(); index++) {
            if (maximumProduction.size() < index) {
                return true;
            } else if (maximumProduction.get(index).getAmount() > production.get(index).getAmount()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get the <code>MaximumConsumption</code> value.
     *
     * @return a <code>List<AbstractGoods></code> value
     */
    public final List<AbstractGoods> getMaximumConsumption() {
        return maximumConsumption;
    }

    /**
     * Set the <code>MaximumConsumption</code> value.
     *
     * @param newMaximumConsumption The new MaximumConsumption value.
     */
    public final void setMaximumConsumption(final List<AbstractGoods> newMaximumConsumption) {
        this.maximumConsumption = newMaximumConsumption;
    }

    /**
     * Describe <code>addMaximumConsumption</code> method here.
     *
     * @param goods an <code>AbstractGoods</code> value
     */
    public void addMaximumConsumption(AbstractGoods goods) {
        maximumConsumption.add(goods);
    }

    /**
     * Get the <code>Storage</code> value.
     *
     * @return a <code>List<AbstractGoods></code> value
     */
    public final List<AbstractGoods> getStorage() {
        return storage;
    }

    /**
     * Set the <code>Storage</code> value.
     *
     * @param newStorage The new Storage value.
     */
    public final void setStorage(final List<AbstractGoods> newStorage) {
        this.storage = newStorage;
    }

    /**
     * Describe <code>addStorage</code> method here.
     *
     * @param goods an <code>AbstractGoods</code> value
     */
    public void addStorage(AbstractGoods goods) {
        storage.add(goods);
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        append(result, "Storage", storage);
        append(result, "Production", production);
        append(result, "Consumption", consumption);
        append(result, "Maximum Production", maximumProduction);
        append(result, "Maximum Consumption", maximumConsumption);
        return result.toString();
    }


    private void append(StringBuilder result, String key, List<AbstractGoods> list) {
        if (!list.isEmpty()) {
            result.append(key + ": ");
            for (AbstractGoods goods : list) {
                result.append(goods.toString());
                if (goods.getType().getStoredAs() != goods.getType()) {
                    result.append(" [" + goods.getType().getStoredAs().getId() + "]");
                }
                result.append(", ");
            }
            int length = result.length();
            result.replace(length - 2, length, "\n");
        }
    }

}