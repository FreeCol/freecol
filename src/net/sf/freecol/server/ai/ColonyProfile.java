/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

import java.util.ArrayList;
import java.util.List;

import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.GoodsType;

public class ColonyProfile {

    /**
     * Describe type here.
     */
    private ProfileType type;

    /**
     * Describe preferredProduction here.
     */
    private List<GoodsType> preferredProduction;

    public static enum ProfileType {
        OUTPOST, SMALL, MEDIUM, LARGE, CAPITAL
    };


    public ColonyProfile() {
        this(ProfileType.MEDIUM, null);
    }

    public ColonyProfile(ProfileType type, List<GoodsType> production) {
        this.type = type;
        if (production == null) {
            preferredProduction = new ArrayList<GoodsType>();
            for (GoodsType goodsType : Specification.getSpecification().getGoodsTypeList()) {
                if (goodsType.isFoodType() || goodsType.isLibertyType()) {
                    preferredProduction.add(goodsType);
                }
            }
        } else {
            preferredProduction = production;
        }
    }

    /**
     * Get the <code>Type</code> value.
     *
     * @return a <code>ProfileType</code> value
     */
    public final ProfileType getType() {
        return type;
    }

    /**
     * Set the <code>Type</code> value.
     *
     * @param newType The new Type value.
     */
    public final void setType(final ProfileType newType) {
        this.type = newType;
    }

    /**
     * Get the <code>PreferredProduction</code> value.
     *
     * @return a <code>List<GoodsType></code> value
     */
    public final List<GoodsType> getPreferredProduction() {
        return preferredProduction;
    }

    /**
     * Set the <code>PreferredProduction</code> value.
     *
     * @param newPreferredProduction The new PreferredProduction value.
     */
    public final void setPreferredProduction(final List<GoodsType> newPreferredProduction) {
        this.preferredProduction = newPreferredProduction;
    }

}