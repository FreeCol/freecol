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

package net.sf.freecol.server.ai;

import java.util.Comparator;


/**
 * A special comparator to sort the production cache with.
 */
public class CacheEntryComparator implements Comparator<ProductionCache.Entry> {

    public int compareProduction(ProductionCache.Entry entry1, ProductionCache.Entry entry2) {
        return entry2.getProduction() - entry1.getProduction();
    }

    @Override
    public int compare(ProductionCache.Entry entry1, ProductionCache.Entry entry2) {

        int production = compareProduction(entry1, entry2);
        if (production != 0) {
            return production;
        } else if (entry1.isExpert()) {
            if (entry2.isExpert()) {
                return 0;
            } else {
                return -1;
            }
        } else if (entry2.isExpert()) {
            return 1;
        } else if (entry1.unitUpgradesToExpert()) {
            if (entry2.unitUpgradesToExpert()) {
                // both can be upgraded to expert: compare experience
                return entry2.getUnit().getExperience()
                    - entry1.getUnit().getExperience();
            } else {
                return -1;
            }
        } else if (entry2.unitUpgradesToExpert()) {
            return 1;
        } else if (entry1.unitUpgrades()) {
            if (entry2.unitUpgrades()) {
                // both can be upgraded: compare wasted experience
                return entry1.getUnit().getExperience()
                    - entry2.getUnit().getExperience();
            } else {
                return -1;
            }
        } else if (entry2.unitUpgrades()) {
            return 1;
        } else if (entry1.isOtherExpert()) {
            if (entry2.isOtherExpert()) {
                return 0;
            } else {
                // prefer non-experts
                return 1;
            }
        } else if (entry2.isOtherExpert()) {
            return -1;
        } else {
            return 0;
        }
    }
}

