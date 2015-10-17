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

import java.util.EnumMap;
import java.util.Map;

import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * This class handles zero or one rule for each number category.
 */
public class DefaultNumberRule extends Number {

    final Map<Category, Rule> rules = new EnumMap<>(Category.class);


    /**
     * Add a parsed rule for category.
     *
     * @param number a <code>Category</code> value
     * @param rule a <code>Rule</code> value
     */
    public void addRule(Category number, Rule rule) {
        rules.put(number, rule);
    }

    /**
     * Add an unparsed rule for category.
     *
     * @param number a <code>Category</code> value
     * @param input a <code>String</code> value
     */
    public void addRule(Category number, String input) {
        rules.put(number, new Rule(input));
    }

    /**
     * Return the number of rules added.
     *
     * @return an <code>int</code> value
     */
    public int countRules() {
        return rules.values().size();
    }

    /**
     * Return the rule for the given category.
     *
     * @param category a <code>Category</code> value
     * @return a <code>Rule</code> value
     */
    public Rule getRule(Category category) {
        return rules.get(category);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Category getCategory(double input) {
        return find(Category.values(),
            category -> rules.containsKey(category)
                && rules.get(category).matches(input), Category.other);
    }
}
