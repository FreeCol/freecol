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
     * Describe failed here.
     */
    private boolean failed;

    /**
     * Describe maximumProduction here.
     */
    private List<AbstractGoods> maximumProduction = new ArrayList<AbstractGoods>();

    /**
     * Describe production here.
     */
    private List<AbstractGoods> production = new ArrayList<AbstractGoods>();

    /**
     * Describe consumption here.
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
     * Get the <code>Failed</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean failed() {
        return failed;
    }

    /**
     * Set the <code>Failed</code> value.
     *
     * @param newFailed The new Failed value.
     */
    public final void setFailed(final boolean newFailed) {
        this.failed = newFailed;
    }
}