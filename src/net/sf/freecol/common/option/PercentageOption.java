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

package net.sf.freecol.common.option;

import java.util.logging.Logger;

import net.sf.freecol.common.model.Specification;


/**
 * Represents an option where the result is a value between 0 and 100.
 */
public class PercentageOption extends IntegerOption {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(PercentageOption.class.getName());


    /**
     * Creates a new <code>PercentageOption</code>.
     *
     * @param specification The <code>Specification</code> to refer to.
     */
    public PercentageOption(Specification specification) {
        super(specification);

        setMinimumValue(0);
        setMaximumValue(100);
    }


    // Serialization

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(16);
        sb.append("[").append(getId())
            .append(" ").append(getValue()).append("%]");
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "percentageOption".
     */
    public static String getXMLElementTagName() {
        return "percentageOption";
    }
}
