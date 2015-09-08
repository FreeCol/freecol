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

import java.util.logging.Logger;


/**
 * Helper container to remember the Europe state prior to some
 * change, and fire off any consequent property changes.
 */
public class EuropeWas {

    private static final Logger logger = Logger.getLogger(EuropeWas.class.getName());

    private final Europe europe;
    private final int unitCount;


    public EuropeWas(Europe europe) {
        this.europe = europe;
        this.unitCount = europe.getUnitCount();
    }

    /**
     * Gets a unit added to Europe since this EuropeWas was sampled.
     *
     * Simply makes sure there is at least one new unit, then picks the one
     * with the highest numeric id.
     *
     * @return A new unit.
     */
    public Unit getNewUnit() {
        if (europe.getUnitCount() < unitCount+1) return null;
        Unit newUnit = null;
        int idMax = 0;
        final String unitPrefix = Unit.getXMLElementTagName() + ":";
        for (Unit u : europe.getUnitList()) {
            String uid = u.getId();
            if (!uid.startsWith(unitPrefix)) continue;
            try {
                int id = Integer.parseInt(uid.substring(unitPrefix.length()));
                if (idMax < id) {
                    idMax = id;
                    newUnit = u;
                }
            } catch (NumberFormatException nfe) {}
        }
        return newUnit;        
    }

    /**
     * Fire any property changes resulting from actions in Europe.
     *
     * @return True if something changed.
     */
    public boolean fireChanges() {
        int newUnitCount = europe.getUnitCount();

        if (newUnitCount != unitCount) {
            europe.firePropertyChange(Europe.UNIT_CHANGE,
                                      unitCount, newUnitCount);
            return true;
        }
        return false;
    }
}
