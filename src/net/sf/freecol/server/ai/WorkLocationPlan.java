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

package net.sf.freecol.server.ai;

import java.util.logging.Logger;

import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.WorkLocation;


/**
 * Objects of this class contains AI-information for a single
 * {@link net.sf.freecol.common.model.WorkLocation}.
 */
public final class WorkLocationPlan extends AIObject {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(WorkLocationPlan.class.getName());

    public static final String TAG = "workLocationPlan";

    /** The work location the plan is for. */
    private WorkLocation workLocation;

    /** The goods to produce. */
    private GoodsType goodsType;


    /**
     * Creates a new {@code WorkLocationPlan}.
     *
     * @param aiMain The main AI-object.
     * @param workLocation The {@code WorkLocation} to create
     *      a plan for.
     * @param goodsType The goodsType to be produced on the
     *      {@code workLocation} using this plan.
     */
    public WorkLocationPlan(AIMain aiMain, WorkLocation workLocation,
                            GoodsType goodsType) {
        super(aiMain);

        this.workLocation = workLocation;
        this.goodsType = goodsType;
        setInitialized();
    }


    /**
     * {@inheritDoc}
     */
    public void setInitialized() {
        this.initialized = getWorkLocation() != null && getGoodsType() != null;
    }

    /**
     * Gets the {@code WorkLocation} this
     * {@code WorkLocationPlan} controls.
     *
     * @return The {@code WorkLocation}.
     */
    public WorkLocation getWorkLocation() {
        return workLocation;
    }

    /**
     * Gets the type of goods which should be produced at the
     * {@code WorkLocation}.
     *
     * @return The type of goods.
     * @see net.sf.freecol.common.model.Goods
     * @see net.sf.freecol.common.model.WorkLocation
     */
    public GoodsType getGoodsType() {
        return goodsType;
    }

    /**
     * Sets the type of goods to be produced at the {@code WorkLocation}.
     *
     * @param goodsType The type of goods.
     * @see net.sf.freecol.common.model.Goods
     * @see net.sf.freecol.common.model.WorkLocation
     */
    public void setGoodsType(GoodsType goodsType) {
        this.goodsType = goodsType;
    }

    /**
     * Is this a food producing plan?
     *
     * @return True if this plan produces food.
     */
    public boolean isFoodPlan() {
        return goodsType.isFoodType();
    }


    // Serialization
    // WorkLocationPlans are not currently saved so this is a no-op.

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return TAG; }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(32);
        sb.append('[').append(getId())
            .append(' ').append(goodsType.getSuffix())
            .append(" at ").append(workLocation.getId())
            .append(']');
        return sb.toString();
    }
}
