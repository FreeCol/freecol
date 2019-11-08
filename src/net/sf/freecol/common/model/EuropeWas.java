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

import java.util.logging.Logger;

import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * Helper container to remember the Europe state prior to some
 * change, and fire off any consequent property changes.
 */
public class EuropeWas {

    private static final Logger logger = Logger.getLogger(EuropeWas.class.getName());

    /** The Europe to remember. */
    private final Europe europe;

    /** The initial number of units in the Europe. */
    private final int unitCount;


    /**
     * Create a new wrapper to remember the state of Europe.
     *
     * @param europe The {@code Europe} to remember.
     */
    public EuropeWas(Europe europe) {
        this.europe = europe;
        this.unitCount = europe.getUnitCount();
    }


    /**
     * Gets a unit added to Europe since this EuropeWas was sampled.
     *
     * As long there is at least one new unit, pick the one with the
     * highest numeric id.
     *
     * @return The newest {@code Unit} or null if none has been added.
     */
    public Unit getNewUnit() {
        return (europe.getUnitCount() <= this.unitCount) ? null
            : maximize(europe.getUnits(),
                       cachingIntComparator(Unit::getIdNumber));
    }

    /**
     * Fire any property changes resulting from actions in Europe.
     *
     * @return True if something changed.
     */
    public boolean fireChanges() {
        final int newCount = europe.getUnitCount();
        if (newCount != this.unitCount) {
            europe.firePropertyChange(Europe.UNIT_CHANGE, unitCount, newCount);
            return true;
        }
        return false;
    }
}
