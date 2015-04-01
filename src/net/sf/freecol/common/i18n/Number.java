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

package net.sf.freecol.common.i18n;

import java.util.logging.Logger;


/**
 * Classes implementing this interface can determine the category and
 * the index of a double based on the number rules for a language.
 *
 * See the
 * <a href="http://cldr.unicode.org/index/cldr-spec/plural-rules">
 * Common Locale Data Repository</a>.
 */
public abstract class Number implements Selector {

    public enum Category { zero, one, two, few, many, other };

    private static final Logger logger = Logger.getLogger(Number.class.getName());

    /**
     * Return the category the selector value belongs to.
     *
     * @param selector a <code>double</code> value
     * @return a <code>Category</code> value
     */
    public abstract Category getCategory(double selector);

    /**
     * {@inheritDoc}
     */
    @Override
    public String getKey(String selector, String template) {
        try {
            return getKey(Double.parseDouble(selector));
        } catch(NumberFormatException e) {
            logger.warning("Syntax error in string template '" + template + "'");
            return Category.other.toString();
        }
    }

    /**
     * Return the key of the rule this selector matches. The key is the
     * string representation of the Category.
     *
     * @param selector a <code>double</code> value
     * @return a <code>String</code> value
     */
    public String getKey(double selector) {
        return getCategory(selector).toString();
    }
}
