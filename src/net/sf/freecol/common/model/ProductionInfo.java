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
     * Possible actions when consumption fails.
     */
    public enum Failure {
        NOTHING, // do nothing
        WAIT,    // wait for more goods
        STARVE   // starve (units only)
    };

    /**
     * What to do if consumption fails.
     */
    private Failure failure = Failure.NOTHING;

    /**
     * The goods moved to storage.
     */
    private List<AbstractGoods> storage;

    /**
     * The maximum production possible given unlimited input.
     */
    private List<AbstractGoods> maximumProduction = new ArrayList<AbstractGoods>();

    /**
     * The actual production.
     */
    private List<AbstractGoods> production = new ArrayList<AbstractGoods>();

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

    /**
     * Get the <code>Failure</code> value.
     *
     * @return a <code>Failure</code> value
     */
    public final Failure getFailure() {
        return failure;
    }

    /**
     * Set the <code>Failure</code> value.
     *
     * @param newFailure The new Failure value.
     */
    public final void setFailure(final Failure newFailure) {
        this.failure = newFailure;
    }

}