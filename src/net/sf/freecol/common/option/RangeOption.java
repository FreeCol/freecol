/**
 *  Copyright (C) 2002-2021   The FreeCol Team
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

package net.sf.freecol.common.option;

import java.util.Iterator;
import java.util.logging.Logger;

import static net.sf.freecol.common.model.Constants.*;
import net.sf.freecol.common.model.Specification;


/**
 * Represents an option where the valid choice is an integer and the
 * choices are represented by strings. In general, these strings are
 * localized by looking up the key of the choice, which consists of
 * the identifier of the AbstractObject followed by a "." followed by
 * the value of the option string.
 *
 * RangeOption differs from SelectOption, as the value being selected
 * represents a numeric measurement, defined by a bounded range of
 * comparable values.  As the graphical component rendering a range
 * option only works with a conventional index, this implies to manage
 * a fixed rank for each possible values.
 */
public class RangeOption extends SelectOption {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(RangeOption.class.getName());

    public static final String TAG = "rangeOption";
    
    /**
     * Creates a new {@code RangeOption}.
     *
     * @param specification The {@code Specification} to refer to.
     */
    public RangeOption(Specification specification) {
        super(specification);
    }

    /**
     * Creates a new {@code RangeOption}.
     *
     * @param id The object identifier.
     * @param specification The {@code Specification} to refer to.
     */
    public RangeOption(String id, Specification specification) {
        super(id, specification);
    }

    /**
     * Gets the rank of the current selected value in the list of
     * values of this {@code RangeOption}.
     *
     * @return The value.
     */
    public int getValueRank() {
        int rank = 0;
        for (Integer i : getItemValues().keySet()) {
            if (i.compareTo(getValue()) == 0) return rank;
            rank++;
        }
        return 0; // Actually invalid
    }

    /**
     * Sets the value through the rank in the list of values of this
     * {@code RangeOption}.
     *
     * @param rank The rank of the value to be set.
     */
    public void setValueRank(int rank) {
        int curValue = UNDEFINED;
        Iterator<Integer> iterator = getItemValues().keySet().iterator();

        while (rank >= 0) {
            if (!iterator.hasNext()) break;
            curValue = iterator.next();
            rank--;
        }

        setValue(curValue);
    }

    /**
     * Gets the tag name of the contained object.
     *
     * @return "rangeValue".
     */
    @Override
    public String getXMLItemElementTagName() {
        return "rangeValue";
    }


    // Serialization

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return TAG; }
}
